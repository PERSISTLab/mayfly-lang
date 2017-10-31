package edu.clemson.mayfly.compile;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.jgrapht.Graphs;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import edu.clemson.mayfly.ast.Argument;
import edu.clemson.mayfly.ast.Constraint;
import edu.clemson.mayfly.ast.Edge;
import edu.clemson.mayfly.ast.Flow;
import edu.clemson.mayfly.ast.FlowPart;
import edu.clemson.mayfly.ast.Program;
import edu.clemson.mayfly.ast.TaskDefinition;
import edu.clemson.mayfly.ast.TimeValue;
import edu.clemson.mayfly.parser.MayflyLexer;
import edu.clemson.mayfly.parser.parser;

public class MayflyCompiler {

	public MayflyCompiler() {}
	
	public String compile(HashMap<String, String> args) throws Exception {
		
		// Parse input file, convert to Program
		Program parsed = parse(args.get("filename"));
		// Extract the sub graphs from Program, calculate priority execution list
		Vector<TaskGraph> task_graphs = extractTaskGraphs(parsed);
		
		// Validate the task graphs structure
		Vector<String> errors = ProgramValidator.validateTaskGraph(task_graphs);
		if(errors.size() > 0) {
			System.err.println("There were structure errors....");
			for(String err : errors) {
				System.err.println("ERROR: "+ err);
			}
			throw new Exception();
		}
		
		// Use the task graphs to generate Embedded-C code with Handlebars
		String final_code = generateCode(task_graphs, parsed, args);
		return final_code;
	}
	
	private Program parse(String filename) throws Exception {
		parser p = new parser(new MayflyLexer(new FileReader(filename)));
		Program result = (Program) p.parse().value;
		return result;
	}
	
	private Vector<TaskGraph> extractTaskGraphs(Program prog) throws Exception {
		Vector<TaskGraph> task_graphs = new Vector<TaskGraph>(); 
		Vector<DirectedMultigraph<TaskDefinition, Edge>> graph_obj_task_path = new Vector<DirectedMultigraph<TaskDefinition, Edge>>();
		
		// Validate the parsed graph
		Vector<String> errors = ProgramValidator.validateProgram(prog);
		if(errors.size() > 0) {
			System.err.println("There were compilation errors....");
			for(String err : errors) {
				System.err.println("ERROR: "+ err);
			}
			throw new Exception();
		}
		
		// Generate mega graph
		SimpleGraph<TaskDefinition, Edge> mega_graph = new SimpleGraph<TaskDefinition, Edge>(Edge.class);
		
		// Extract flow information (like predicates) into edges
		for (Flow f: prog.getFlows()) {
			Vector<FlowPart> tasks_names = f.getFlowTasks();
			FlowPart lastFlow = tasks_names.get(0);
			for(int i = 1;i<tasks_names.size();i++) {
				FlowPart next_flow = tasks_names.get(i);
				int predicate_arg_count = lastFlow.getPredicate().size();
				if(predicate_arg_count > 0) {
					Edge e = prog.getEdge(lastFlow.getName()+"->"+next_flow.getName());
					TaskDefinition source = prog.getTaskDefinition(lastFlow.getName());
					int targetIndex = 0;
					for (Object s : lastFlow.getPredicate()) {
						if(!s.equals("_")) {
							break;
						}
						targetIndex++;
					}
					// Already validated this
					Integer targ = (Integer)lastFlow.getPredicate().get(targetIndex);
					e.setPredicate(source.getOutput().get(targetIndex).getName(), targ);
				}
				lastFlow = next_flow;
				
			}
		} 
		
		// Vertices
		Vector<TaskDefinition> tasks = prog.getTaskDefinitions();
		for (TaskDefinition t : tasks) {
			mega_graph.addVertex(t);
		}
		
		// Add all the edges + constraints into the structure
		Vector<Edge> edges = prog.getEdges();
		for (Edge ed : edges) {
			TaskDefinition sourceVertex = prog.getTaskDefinition(ed.getSource());
			TaskDefinition targetVertex = prog.getTaskDefinition(ed.getDest());
			mega_graph.addEdge(sourceVertex, targetVertex, ed);
		}

		// Put vertices / tasks in correct subgraphs, remove from mega graph
		// Generate task graphs from mega graph (directed multi-graphs)
		// Use simple flood fill coloring algorithm
		while(mega_graph.vertexSet().size() > 0) {
			DirectedMultigraph<TaskDefinition, Edge> dgi = new DirectedMultigraph<TaskDefinition, Edge>(Edge.class);
			// Continually color and remove the sub-graphs 
			BreadthFirstIterator<TaskDefinition, Edge> bfs = new BreadthFirstIterator<TaskDefinition, Edge>(
					mega_graph, 
					(TaskDefinition)mega_graph.vertexSet().toArray()[0]
					);
			
			while(bfs.hasNext()) {
				TaskDefinition td = bfs.next();
				dgi.addVertex(td);
				mega_graph.removeVertex(td);
				
			}
			
			// Add new graph to graph set
			graph_obj_task_path.add(dgi);
		}
		
		// Now add the edges back in, these must be defined for every edge by programmer
		for (Edge e: edges) {
			TaskDefinition dest = prog.getTaskDefinition(e.getDest());
			TaskDefinition source = prog.getTaskDefinition(e.getSource());
			DirectedMultigraph<TaskDefinition, Edge> dgv = getGraphByTask(prog, graph_obj_task_path, e.getSource());
			if(dgv == null) {
				throw new Exception("FATAL ERROR: Task \""+e.getSource()+" \"does not exist in Sub graph");
			}
			dgv.addEdge(source, dest, e);
		}	
		
		// Generate priority list for each task graph, closeness to a sink has highest priority
		// Also more validation
		for(DirectedMultigraph<TaskDefinition, Edge> dgv: graph_obj_task_path) {
			// Get sinks, sources, reverse topologically sorted task list, and priority
			TaskGraph tg = new TaskGraph(dgv, prog.getFlows());
			task_graphs.add(tg);
		}	
		
		// Now handle MISD constraint placement
		// It needs to be a generation constraint for a task, not on the edge where defined by programmer
		/*for(DirectedMultigraph<TaskDefinition, Edge> dgv: graph_obj_task_path) {
			for(Edge e: dgv.edgeSet()) {
				Constraint misdConstraint = e.constraintOnEdge("misd");
				if(misdConstraint != null) {
					TaskDefinition tdgen = prog.getTaskDefinition(e.getSource());
					tdgen.addGenerationConstraint(misdConstraint);
					// Remove misd from this edge
					e.removeConstraint("misd");
				}
				
			}
		}*/

		return task_graphs;
	}

	private String generateCode(Vector<TaskGraph> task_graphs, Program prog, HashMap<String, String> args) throws Exception {
		MustacheFactory mf = new MayflyMustacheFactory();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		/* Init the necessary templates */
		
		// Standard and developer made includes
		Mustache includes_mustache = mf.compile("templates/includes.hbs");
		// Data structures
		Mustache tasks_mustache = mf.compile("templates/tasks.hbs");
		Mustache task_data_mustache = mf.compile("templates/task_data.hbs");
		// Helper functions for scheduler
		Mustache helpers_mustache = mf.compile("templates/helpers.hbs");
		// Scheduler
		Mustache scheduler_mustache = mf.compile("templates/scheduler/"+args.get("scheduler")+".hbs");
		// Main
		Mustache main_mustache = mf.compile("templates/mayfly.hbs");

		/* ------------------- Includes ----------------- */
		HashMap<String, String> include_info = new HashMap<String, String>();
		include_info.put("target", args.get("mcu"));
		includes_mustache.execute(pw, include_info).flush();
		String includes = sw.toString();
		sw.getBuffer().setLength(0);
		
		
		/* ---------------------------------------------- */
		/* ---------------- Task Graphs ----------------- */
		/* ---------------- User Tasks ------------------ */
		/* ---------------------------------------------- */
		String usertasks = "";
		for(TaskGraph tg: task_graphs) {
			tasks_mustache.execute(pw, tg).flush();
			Vector<TaskDefinition> tgtasks = (Vector<TaskDefinition>)tg.getSorted().clone();
			Collections.reverse(tgtasks);
			for(TaskDefinition td : tgtasks) {
				if(args.get("autogen").equals("false")) {
					usertasks += "extern ";
				}
				usertasks += "void "+td.getName() +"(";
				Vector<Argument> ins = td.getInput();
				for(int i = 0;i<ins.size();i++) {
					if(i != 0) {
						usertasks += ", ";
					}
					Argument a = ins.get(i);
					usertasks += a.getType() + (a.isArray() ? "" : " *") + " "+a.getName()+ (a.isArray() ? "[] " : "");
				}
				Vector<Argument> outs = td.getOutput();
				if(outs.size() > 0 && td.getInput().size() > 0) usertasks += ", ";
				for(int i = 0;i<outs.size();i++) {
					if(i != 0) {
						usertasks += ", ";
					}
					usertasks += outs.get(i).getType() + " * "+outs.get(i).getName();
				}
				if(args.get("autogen").equals("false")) {
					usertasks+=");\n";
				} else {
					usertasks+=") {\n/* Blank task function*/\n#ifdef DEBUG\n       EIF_PRINTF(\"%u:"+td.getName()+"\\r\\n\","+td.getNodeId()+");\n       __delay_cycles(1000);\n#endif\n}\n";
				}
				
			}
		}
		String tasks = sw.toString();
		sw.getBuffer().setLength(0);
		
		
		/* ---------------------------------------------- */
		/* ------------- Task Data Structs -------------- */
		/* ---------------------------------------------- */
		Vector<MustacheTaskOutputDataObject> task_name_data = new Vector<MustacheTaskOutputDataObject>();
		for(TaskGraph tg: task_graphs) {
			DirectedMultigraph<TaskDefinition, Edge> graph_structure = tg.getGraph();
			// Generate edge data structure definitions and instantiation
			for(TaskDefinition td: graph_structure.vertexSet()) {
				Set<Edge> outgoing_edges = graph_structure.outgoingEdgesOf(td);
				MustacheTaskOutputDataObject mtdo = new MustacheTaskOutputDataObject(td, outgoing_edges);
				task_data_mustache.execute(pw, mtdo).flush();
				task_name_data.add(mtdo);
			}
		}
		String task_data = sw.toString();
		sw.getBuffer().setLength(0);
		
		
		/* ---------------------------------------------- */
		/* -------------- Edge Constraints -------------- */
		/* -------------- Helpers, Part. 1 -------------- */
		/* ---------------------------------------------- */
		Vector<MustacheConstraintHelperDataObject> helper_data_tasks = new Vector<MustacheConstraintHelperDataObject>();
		for(TaskGraph tg: task_graphs) {
			DirectedMultigraph<TaskDefinition, Edge> graph_structure = tg.getGraph();
		
			// For every task get the incominng and outgoing edges constraints
			for(TaskDefinition td: graph_structure.vertexSet()) {
				Set<Edge> incoming = graph_structure.incomingEdgesOf(td);
				Set<Edge> outgoing = graph_structure.outgoingEdgesOf(td);
				
				MustacheConstraintHelperDataObject mt = new MustacheConstraintHelperDataObject(td, incoming, outgoing);
				
				// Get expires, collect, and predicate on incoming edges
				for (Edge edge : incoming) {
					mt.addConstraints(edge);
				}
				
				// Get misd on outgoing edges
				for (Edge edge : outgoing) {
					mt.addConstraints(edge);
				}
				helper_data_tasks.add(mt);
			}
		}
		
		/* ---------------------------------------------- */
		/* -------------- Helpers, Part. 2 -------------- */
		/* ---------------------------------------------- */
		HashMap<String, Object> helper_obj = 
				new HashMap<String, Object>(); 
		helper_obj.put("task_constraints", helper_data_tasks);
		helpers_mustache.execute(pw, helper_obj).flush();
		String helpers = sw.toString();
		sw.getBuffer().setLength(0);

		
		/* ---------------------------------------------- */
		/* ----------- Main Program & Scheduler ----------*/
		/* ---------------------------------------------- */
		int num_tasks = 0;
		Vector<MustacheTaskSchedulerDataObject> mustache_task_graph_list = new 
					Vector<MustacheTaskSchedulerDataObject> ();
		for(TaskGraph tg: task_graphs) {
			num_tasks += tg.getSorted().size();
			Vector<MustacheTaskDefHandlerDataObject> task_list = new 
					Vector<MustacheTaskDefHandlerDataObject> ();
			for(TaskDefinition td: tg.getSorted()) {
				MustacheTaskDefHandlerDataObject mtdf = new MustacheTaskDefHandlerDataObject(prog, td, tg.getGraph().incomingEdgesOf(td), tg.getGraph().outgoingEdgesOf(td));
					task_list.add(mtdf);
			}
			MustacheTaskSchedulerDataObject mtsched = new MustacheTaskSchedulerDataObject(tg.name_caps(), tg.name(), task_list);
			mustache_task_graph_list.add(mtsched);
		}
		HashMap<String, Vector<MustacheTaskSchedulerDataObject>> mustache_bull_crap = 
				new HashMap<String, Vector<MustacheTaskSchedulerDataObject>>(1); 
		mustache_bull_crap.put("task_graph", mustache_task_graph_list);
		scheduler_mustache.execute(pw, mustache_bull_crap ).flush();
		String scheduler = sw.toString();
		sw.getBuffer().setLength(0);
		
		/* Fill in the final object and put code in string */
		HashMap<String, Object> mustache_prog_data = new HashMap<String, Object>();
		mustache_prog_data.put("includes", includes);
		mustache_prog_data.put("tasks", tasks);
		mustache_prog_data.put("task_data", task_data);
		mustache_prog_data.put("helpers", helpers);
		mustache_prog_data.put("num_tasks", num_tasks+"");
		mustache_prog_data.put("scheduler", scheduler);
		mustache_prog_data.put("usertasks", usertasks);
		mustache_prog_data.put("task_name_data", task_name_data);
		main_mustache.execute(pw, mustache_prog_data).flush();
		return sw.toString();
	}
	
	/**
	 * Helper methods
	 */
	private DirectedMultigraph<TaskDefinition, Edge> getGraphByTask(Program prog, Vector<DirectedMultigraph<TaskDefinition, Edge>> task_graphs, String task) {
		for (int i = 0; i < task_graphs.size(); i++) {
			if(task_graphs.get(i).containsVertex(prog.getTaskDefinition(task)) == true) {
				return task_graphs.get(i);
			}
		}
		return null;
	}
}

/**
 * Mustache template filler objects
 * @author josiah
 *
 */
class MayflyMustacheFactory extends DefaultMustacheFactory {
	@Override
	public void encode(String value, Writer writer) {
		try {
			writer.write(value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class MustacheTaskArgsDataObject {
	String name;
	String source;
	String dest;
	String type;
	String last;
	int numItems;
	boolean is_dupe = false;
	Vector<TaskDefinition> dupes; 
	public MustacheTaskArgsDataObject(String name,
			String source,
			String dest,
			String type,
			String last, 
			int numItems) {
		this.name = name;
		this.source = source;
		this.dest = dest;
		this.type = type;
		this.last = last;
		// This is because the OUTPUT of this template must be a power of 2 
		this.numItems = Math.min(numItems, 2);
		
	}
}

class MustacheEdgeAvailableObject {
	String source;
	String destCaps;
	boolean lastEdge;
	public MustacheEdgeAvailableObject(	String source, String destCaps, boolean lastEdge) {
		this.source = source;
		this.destCaps = destCaps;
		this.lastEdge = lastEdge;
	}
}

class MustacheTaskDefHandlerDataObject {
	Vector<Edge> incoming_edges;
	Vector<Edge> outgoing_edges;
	Vector<MustacheEdgeAvailableObject> out_edge_data;
	Vector<MustacheEdgeAvailableObject> in_edge_data;
	Vector<MustacheTaskArgsDataObject> task_input;
	Vector<MustacheTaskArgsDataObject> task_output;
	String task_signature_filled;
	boolean has_outgoing = false;
	boolean has_incoming = false;
	int task_id;
	String task_name;
	boolean hasOutputs = false;
	boolean hasInputs = false;
	private Program prog;
	boolean isArray = false;
	// Outgoing
	int buffer_size = 2;
	int buffer_num_max = 1; 
	int buffer_size_mask = 1;
	// Incoming
	int incoming_buffer_size = 2;
	int incoming_buffer_num_max = 1; 
	int incoming_buffer_size_mask = 1;
	public MustacheTaskDefHandlerDataObject(Program prog, TaskDefinition td, Set<Edge> in_edges, Set<Edge> out_edges) {
		// Init
		task_id = td.getNodeId();
		task_name = td.getName();
		task_input = new Vector<MustacheTaskArgsDataObject>();
		task_output = new Vector<MustacheTaskArgsDataObject>();
		out_edge_data = new Vector<MustacheEdgeAvailableObject>();
		in_edge_data = new Vector<MustacheEdgeAvailableObject>();
		this.incoming_edges = new Vector<Edge>(in_edges);
		this.outgoing_edges = new Vector<Edge>(out_edges);
		this.prog = prog;
		
		Iterator<Edge> itr = in_edges.iterator();		
		while(itr.hasNext()) {
			Edge e = itr.next();
			// Get incoming collect edges
			for(Constraint c: e.getDepends()) {
				if(c.getName().equals("collect")) {
					int new_buf_size = (Integer)c.getArguments().get(0);
					if(new_buf_size > buffer_size) {
						incoming_buffer_size = incoming_buffer_num_max = new_buf_size;
						incoming_buffer_size_mask = incoming_buffer_size-1;
					}
				}
			}
			in_edge_data.add(new MustacheEdgeAvailableObject(e.getSource(), e.getDestCaps(), false));
		}
		if(in_edge_data.size() > 0) in_edge_data.lastElement().lastEdge = true;
		// Get collect edges to determine size of buffer for collect
		for(Edge e: outgoing_edges) {
			for(Constraint c: e.getDepends()) {
				if(c.getName().equals("collect")) {
					int new_buf_size = (Integer)c.getArguments().get(0);
					if(new_buf_size > buffer_size) {
						buffer_size = buffer_num_max = new_buf_size;
						buffer_size_mask = buffer_size-1;
					}
				}
			}
			out_edge_data.add(new MustacheEdgeAvailableObject(e.getSource(), e.getDestCaps(), false));
		}
		if(out_edge_data.size() > 0) out_edge_data.lastElement().lastEdge = true;
		
		// Input and output fields for task
		// Get the arguments from all incoming edges (the data)
		// Need to put this in the task function, then clear them
		for(Argument a: td.getInput()) {
			Vector<TaskDefinition> source_tds = getTaskDefinitionFromIncomingEdgeData(a);
			MustacheTaskArgsDataObject cc = new MustacheTaskArgsDataObject(a.getName(), source_tds.get(0).getName(), td.getName(), a.getType(), "", a.getNumitems());
			task_input.add(cc);
		}
		if(task_input.size() > 0) {
			task_input.lastElement().last = "true";
			hasInputs = true;
		}
		
		// Get the outgoing edges and the dest nodes inputs to know what to set
		// Only put the first incoming edge values that match the output, then put the rest in a dupe list
		
		for(Argument a: td.getOutput()) {
			// Get the tasks that need this output (multicast).
			// If there are none, this is an exception (which should have been caught in validation)
			Vector<TaskDefinition> dest_td_list = getTaskDefinitionListFromOutgoingEdgeData(a);
			TaskDefinition dest_td_main = dest_td_list.get(0);
			// Construct function call argument for task output (will be modified by task function call)
			MustacheTaskArgsDataObject cc = new MustacheTaskArgsDataObject(a.getName(), td.getName(), dest_td_main.getName(), a.getType(), "", a.getNumitems());
			task_output.add(cc);
		}
		if(task_output.size() > 0) {
			task_output.lastElement().last = "true";
			hasOutputs = true;
		}
		
		if(this.outgoing_edges.size() > 0) {
			has_outgoing = true;
		}
		
		if(this.incoming_edges.size() > 0) {
			has_incoming = true;
		}
		System.out.println();
	}

	private Vector<TaskDefinition> getTaskDefinitionListFromOutgoingEdgeData(Argument output_arg) {
		Vector<TaskDefinition> tasks_with_matching_args = new Vector<TaskDefinition>();
		Iterator<Edge> itr = outgoing_edges.iterator();		
		while(itr.hasNext()) {
			Edge e = itr.next();
			TaskDefinition dest = prog.getTaskDefinition(e.getDest());
			Vector<Argument> args = dest.getInput();
			for(Argument a: args) {
				if(a.equals(output_arg)) tasks_with_matching_args.add(dest);
			}
		}
		
		return tasks_with_matching_args;
	}
	
	private Vector<TaskDefinition> getTaskDefinitionFromIncomingEdgeData(Argument input_arg) {
		Vector<TaskDefinition> tds = new Vector<TaskDefinition>();
		Iterator<Edge> itr = incoming_edges.iterator();		
		while(itr.hasNext()) {
			Edge e = itr.next();
			TaskDefinition src = prog.getTaskDefinition(e.getSource());
			Vector<Argument> args = src.getOutput();
			for(Argument a: args) {
				// Input arguments to a sink MUST be unique to that sink 
				if(a.equals(input_arg)) tds.addElement(src);
			}
		}
		return tds;
	}
}


class MustacheTaskSchedulerDataObject {
	String name_caps;
	String name;
	Vector<MustacheTaskDefHandlerDataObject> task_list;
	public MustacheTaskSchedulerDataObject(	String name_caps, String name, 
			Vector<MustacheTaskDefHandlerDataObject> task_list) {
		this.name = name;
		this.name_caps = name_caps;
		this.task_list = task_list;
		
	}
}

class MustacheConstraintHelperDataObject {
	int node_id;
	String name;
	String name_caps;
	Set<Edge> incoming_edges;
	Vector<MustacheEdgeAvailableObject> outgoing_edges;
	Set<MustacheConstraintsDataObject> constraints;
	boolean has_outgoing = false;
	boolean has_incoming = false;
	int buffer_read_index=0;
	int buffer_size_mask = 1;
	
	int incoming_buffer_read_index=0;
	int incoming_buffer_size_mask = 1;
	public MustacheConstraintHelperDataObject(TaskDefinition td, Set<Edge> incoming_e, Set<Edge> outgoing_e) {
		node_id = td.getNodeId();
		name = td.getName();
		name_caps = name.toUpperCase();
		constraints = new HashSet<MustacheConstraintsDataObject>();
		incoming_edges = incoming_e;
		outgoing_edges = new Vector<MustacheEdgeAvailableObject>(outgoing_e.size());
		for(Edge e: outgoing_e) {
			outgoing_edges.add(new MustacheEdgeAvailableObject(e.getSource(), e.getDestCaps(), false));
		}
		if(outgoing_edges.size() > 0) {
			outgoing_edges.lastElement().lastEdge = true;
			has_outgoing = true;
		}
		
		if(incoming_e.size() > 0) has_incoming = true;
				
	}
	
	public void addConstraints(Edge e) {
		for(Constraint c: e.getDepends()) {
			MustacheConstraintsDataObject cc = new MustacheConstraintsDataObject();
			cc.source = e.getSource();
			cc.dest = e.getDest();
			cc.collectsize = 2;
			cc.collectsizeminusone = 1;
			switch(c.getName()) {
			case "misd":
				// Only apply MISD if outgoing edge
				if(e.getDest().equals(name)) break;
				
				cc.isMisd = true;
				try {
					TimeValue tvmisd = new TimeValue(c.getArguments().get(0));
					cc.misd = tvmisd.getMSValue();
				} catch (Exception exc) {
					exc.printStackTrace();
				}
				break;
			case "expires":
				// Only apply expires if incoming edge
				if(e.getSource().equals(name)) break;
				TimeValue expire_time = null;
				try {
					expire_time = new TimeValue(c.getArguments().get(0));
				} catch (Exception exc) {
					exc.printStackTrace();
				}
				cc.expires = expire_time.getMSValue();
				cc.isExpires = true;
				break;
			case "collect":
				cc.isCollect = true;
				cc.collectsize = (Integer) c.getArguments().get(0);
				cc.collectsizeminusone = cc.collectsize -1;
				// If outgoing edge
				if(e.getSource().equals(name)) {
					buffer_read_index = cc.collectsize - 1;
					buffer_size_mask = cc.collectsize - 1;
				} else {
					incoming_buffer_read_index= cc.collectsize - 1;
					incoming_buffer_size_mask = cc.collectsize - 1;
				}
				break;
			case "predicate":
				// Only apply predicate if incoming edge
				if(e.getSource().equals(name)) break;
				cc.isPredicate = true;
				cc.output_name = (String)c.getArguments().get(0);
				cc.predicate_val = (Integer)c.getArguments().get(1);
				break;
			}
			constraints.add(cc);
		}
	}
}

class MustacheConstraintsDataObject {
	boolean isExpires;
	boolean isCollect;
	boolean isMisd;
	boolean isPredicate;
	int expires;
	String source;
	String dest;
	String task_name;
	int predicate_val;
	String output_name; 
	int misd;
	int collectsize;
	int collectsizeminusone;
	boolean last = false;
}

class AvailableDataRegisters {
	String name_caps;
	String bit_field;
	public AvailableDataRegisters(String name_caps, String bit_field) {
		this.name_caps = name_caps;
		this.bit_field = bit_field;
	}
}

class MustacheTaskOutputDataObject {
	Vector<Argument> task_output;
	Set<Edge> outgoing_edges ;
	String name;
	int node_id;
	int numitems;
	int buffer_size_mask = 1;
	boolean last = false;
	Set<AvailableDataRegisters> available_data_registers;

	public MustacheTaskOutputDataObject(TaskDefinition td, Set<Edge> outgoing_edges ) {
		this.task_output = new Vector<Argument>();
		this.name = td.getName();
		node_id = td.getNodeId();
		numitems = 2;
		this.outgoing_edges  = outgoing_edges;
		
		// Buf size determined by the largest collect constraint
		for(Edge e: outgoing_edges) {
			for(Constraint c: e.getDepends()) {
				if(c.getName().equals("collect")) {
					numitems = Math.max(numitems, (Integer) c.getArguments().get(0));
					buffer_size_mask = numitems-1;
				}
			}
		}
		
		for (Argument arg : td.getOutput()) {
			task_output.add(new Argument(arg.getType(), arg.getName(), true, numitems));
		}
		
		// Get the available data registers
		available_data_registers  = new HashSet<AvailableDataRegisters>(this.outgoing_edges.size());
		int ndx=0;
		for(Edge e : this.outgoing_edges) {
			
			available_data_registers.add(new AvailableDataRegisters(e.getDest().toUpperCase(), String.format("%8s", Integer.toBinaryString(1 << ndx)).replace(' ', '0')));
			ndx++;
		}
	}
}

class MustacheEdgeDataObject {
	Vector<Argument> task_input;
	Vector<MustacheConstraintsDataObject> constraints;
	Vector<Constraint> raw_edge_constraints;
	String source;
	String dest;
	int numItems;
	boolean isArrayData;
	
	public MustacheEdgeDataObject(Vector<Argument> task_input, String source, String dest, Vector<Constraint> edge_constraints) {
		this.task_input = task_input;
		if( task_input.size() > 0) {
			Argument lastel = (Argument) task_input.lastElement();
			lastel.last = true;
		}
		this.source = source;
		this.dest = dest;
		this.raw_edge_constraints = edge_constraints;
		constraints = new Vector<MustacheConstraintsDataObject>();
		numItems = 2;
		isArrayData = false;

		for(Constraint c: edge_constraints) {
			MustacheConstraintsDataObject cc = new MustacheConstraintsDataObject();
			cc.dest = dest;
			cc.source = source;
			switch(c.getName()) {
			case "expires":
				TimeValue expire_time = null;
				try {
					expire_time = new TimeValue(c.getArguments().get(0));
				} catch (Exception e) {
					e.printStackTrace();
				}
				cc.expires = expire_time.getMSValue();
				cc.isExpires = true;
				break;
			case "collect":
				isArrayData = true;
				numItems = (Integer) c.getArguments().get(0);
				TimeValue tv = null;
				try {
					tv = new TimeValue(c.getArguments().get(1));
				} catch (Exception e) {
					e.printStackTrace();
				}
				cc.expires = tv.getMSValue();
				cc.isCollect = true;
				break;
			case "predicate":
				cc.isPredicate = true;
				cc.output_name = (String)c.getArguments().get(0);
				cc.predicate_val = (Integer)c.getArguments().get(1);
				break;
			}
			constraints.add(cc);
		}
		if( constraints.size() > 0) {
			MustacheConstraintsDataObject lastel = (MustacheConstraintsDataObject) constraints.lastElement();
			lastel.last = true;
		}
	}
}
