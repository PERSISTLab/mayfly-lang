package edu.clemson.mayfly.compile;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import org.jgrapht.Graphs;

import edu.clemson.mayfly.ast.Argument;
import edu.clemson.mayfly.ast.Constraint;
import edu.clemson.mayfly.ast.Edge;
import edu.clemson.mayfly.ast.Flow;
import edu.clemson.mayfly.ast.FlowPart;
import edu.clemson.mayfly.ast.Program;
import edu.clemson.mayfly.ast.TaskDefinition;

import java.util.List;
import java.util.Set;

public class ProgramValidator {
	
	private static final List<String> SUPPORTED_PRIMITIVE_TYPES;
	private static final String[] supported_types_arr = {
			"uint8_t", 
			"uint16_t", 
			"uint32_t",
			"uint64_t", 
			"int8_t", 
			"int16_t",
			"int32_t", 
			"int64_t", 
			"char",
			"int",
			"long",
			"long long",
			"float",
			"double"
			};
	static {
		SUPPORTED_PRIMITIVE_TYPES = Collections.unmodifiableList(Arrays.asList(supported_types_arr));
	}
	
	private static Vector<String> validateTasks(Program prog) {
		Vector<String> errors = new Vector<String>();
		
		//1- Make sure types of arguments in inputs and outputs are valid primitives 
		//2- Make sure names of inputs and output sare unique per task
		//3- Make sure outputs do not have arrays 
		Vector<TaskDefinition> task_defs = prog.getTaskDefinitions();
		for(TaskDefinition t: task_defs) {
			/*for(Argument a: t.getInput()) {
				if(SUPPORTED_PRIMITIVE_TYPES.contains(a.getType()) == false) {
					errors.add("Line "+(t.getLine()+1)+": Argument type \""+a.getType()+"\" not supported.");
				}
			}
			
			for(Argument a: t.getOutput()) {
				if(SUPPORTED_PRIMITIVE_TYPES.contains(a.getType()) == false) {
					errors.add("Line "+(t.getLine()+1)+": Argument type \""+a.getType()+"\" not supported.");
				}
			} */
			Set<Argument> allargs = new HashSet<Argument>();
			Vector<String> dupargs = new Vector<String>();
			for (Argument each: t.getInput()) {
				if(allargs.add(each) == false) {
					dupargs.add(each.getName());
				}
			}
			for (Argument each: t.getOutput()) {
				if(allargs.add(each) == false) {
					dupargs.add(each.getName());
				}
				// Default size is 2 for buffers
				if(each.getNumitems() > 2 || each.getType().contains("[")) {
					errors.add("Line "+(t.getLine()+1)+": Task \""+t.getName()+"\" output cannot have arrays.");
				}
			}
			if(dupargs.size() > 0) {
				errors.add("Line "+(t.getLine()+1)+": Task \""+t.getName()+"\" has duplicate arguments, "+dupargs);
			}
		}
		
		
		return errors;
	}
	
	private static Vector<String> validateFlows(Program prog) {
		Vector<String> errors = new Vector<String>();
		Vector<Flow> flows = prog.getFlows();
		for (Flow f: flows) {
			String lastFlow = "";
			Vector<FlowPart> tasks_names = f.getFlowTasks();
			if(tasks_names.size() < 2) errors.add("Line "+(f.getLine()+1)+": Flow must be greater than one.");
			for (FlowPart s: tasks_names) {
				// Make sure flows don't reference undefined tasks
				if(prog.isTaskDefined(s.getName()) == false) {
					errors.add("Line "+(f.getLine()+1)+": Task \""+s.getName()+"\" referenced by flow \""+f+"\" does not exist.");
				} else {

					// Make sure flow predicates are the same length as the task outputs
					int output_arg_count = prog.getTaskDefinition(s.getName()).getOutput().size();
					
					int predicate_arg_count = s.getPredicate().size();
					if(s.getPredicate().size() != 0 && 
							predicate_arg_count != output_arg_count ) {
						errors.add("Line "+(f.getLine()+1)+": Flow \""+s.getName()+"\" predicate signature does not match output signature. Output has "+output_arg_count+" arguments. Flow has "+predicate_arg_count+" arguments.");
					}
					
					for(Object o: s.getPredicate()) {
						if(!o.equals("_")) {
							if(!(o instanceof Integer)) {
								errors.add("Line "+(f.getLine()+1)+": Flow \""+s.getName()+"\" must have integers for the predicate value.");
							}
						}
					}
					
					// Also check for flows that reference undefined edges
					if(lastFlow != "") {
						// Make sure each edge listed in flow is defined
						//String flow_str = lastFlow+"->"+s.getName();
						//if(prog.isEdgeDefined(flow_str) == false) {
						//	errors.add("Line "+(f.getLine()+1)+": Flow \""+flow_str+"\" has undefined edge constraints.");
						//}

					}
					lastFlow = s.getName();
				}
			}
			
			// Make sure flows have policies that are supported and match arguments
			// 
			Vector<Constraint> constr = f.getConstraints();
			for(Constraint c: constr) {
				Vector<Object> args = c.getArguments();
				String nm = c.getName();
				if(Program.isPolicySupported(nm) == false) {
					errors.add("Line "+(c.getLine()+1)+": Flow policy \""+nm+"\" referenced by flow \""+f+"\" is not supported.");
				} else {
					if(Program.getPolicyArguments(nm) != args.size()) {
						errors.add("Line "+(c.getLine()+1)+": Number of arguments for flow policy \""+nm+"\" should be "+Program.getPolicyArguments(nm));
					}
				}
			}
		}
		return errors;
	}
	
	
	private static Vector<String> validateEdgeConstraints(Program prog) {
		Vector<String> errors = new Vector<String>();
		
		Vector<Edge> edges = prog.getEdges();
		for (Edge e: edges) {
			String edgeName = e.getSource()+"->"+e.getDest();
			// Make sure that edges reference defined tasks
			if(prog.isTaskDefined(e.getDest()) == false) {
				errors.add("Line "+(e.getLine()+1)+": Task \""+e.getDest()+"\" on edge \""+e.getSource()+" => "+e.getDest()+"\" has not been defined.");
			}
			if(prog.isTaskDefined(e.getSource()) == false) {
				errors.add("Line "+(e.getLine()+1)+": Task \""+e.getSource()+"\" on edge \""+e.getSource()+" => "+e.getDest()+"\" has not been defined.");
			}
			
			// Make sure edge constraints are supported
			// Make sure edge constraints follow rules
			Vector<Constraint> depends = e.getDepends();
			for(Constraint c: depends) {
				if(Program.isEdgeConstraintSupported(c.getName()) == false) {
					errors.add("Line "+(c.getLine()+1)+": Edge constraint \""+c.getName()+"\" on edge \""+edgeName+"\" not supported.");
				} else {
					// If supported make sure they have correct number of arguments
					if(Program.getEdgeConstraintArguments(c.getName()) != c.getArguments().size()) {
						errors.add("Line "+(c.getLine()+1)+": Edge constraint \""+c.getName()+"\" on edge \""+edgeName+"\" has "+Program.getEdgeConstraintArguments(c.getName())+" argument(s) not "+c.getArguments().size()+".");
					}
				}
				// 1- Check that collect buffer size is power of 2
				// 2- Check that buffer size is same size as array on edge of task inputs this is defined on i.e. like below
				// task () => (uint8_t arr[8]);
				// task  => task1 {collect(8);}
				if(c.getName().equals("collect")) {
					// 1
					int buf_size = (int)(c.getArguments().get(0)); 
					if((buf_size & (buf_size - 1)) != 0) {
						errors.add("Line "+(c.getLine()+1)+": Edge constraint \"collect\" on edge \""+edgeName+"\" has invalid buffer size of "+buf_size+". Buffer size must be power of two .");
					}
					
					// 2 
					// Dest task input must match size of collectbuffer
					TaskDefinition desttask = prog.getTaskDefinition(e.getDest());
					for(Argument a: desttask.getInput()) {
						if(a.getNumitems() != buf_size) {
							errors.add("Line "+(c.getLine()+1)+": Edge constraint \"collect\" on edge \""+edgeName+"\" buffer size of "+buf_size+" does not match expected buffer size on task input of "+a.getNumitems()+"."); 
						}
					}
				}
			
			}
		}
		return errors;
	}
	
	public static Vector<String> validateProgram(Program prog) {
		Vector<String> errors = new Vector<String>();
		
		// Check that global policies are supported and have right format 
		Vector<Constraint> policies = prog.getPolicies();
		for(Constraint c: policies) {
			if(Program.isPolicySupported(c.getName()) == false) {
				errors.add("Line "+(c.getLine()+1)+": Global policy \""+c.getName()+"\" is not supported.");
			} else {
				if(Program.getPolicyArguments(c.getName()) != c.getArguments().size()) {
					errors.add("Line "+(c.getLine()+1)+": Global policy \""+c.getName()+"\" has "+Program.getPolicyArguments(c.getName())+" argument(s) not "+c.getArguments().size()+".");
				}
			}
		}
		
		// Check tasks, flows, and edges
		errors.addAll(validateTasks(prog));
		errors.addAll(validateFlows(prog));
		errors.addAll(validateEdgeConstraints(prog));
		
		return errors;
	}

	public static Vector<String> validateTaskGraph(Vector<TaskGraph> task_graphs) {
		// TODO validate multi source, multi sink task graphs
		// Make sure that inputs of the task match incoming edges data
		// This takes care of outputs as well
		// Check counts, types, NOT order, of input and output through graph
		Vector<String> errors = new Vector<String>();
		for( TaskGraph tg: task_graphs) {
			for(TaskDefinition td: tg.getSorted()) {
				Set<Edge> incoming_edges = tg.getGraph().incomingEdgesOf(td);
				Set<Edge> outgoing_edges = tg.getGraph().outgoingEdgesOf(td);
				// Cannot have more than 8 outgoing edges
				if(outgoing_edges.size() > 8) {
					errors.add("Line "+(td.getLine()+1)+": Task \""+td.getName()+"\" cannot have more than eight (8) outgoing edges, remove links.");
				}
				Iterator<Edge> itr = incoming_edges.iterator();
				Vector<Argument> task_inputs = td.getInput();
				while(itr.hasNext()) {
					TaskDefinition source_task = tg.getGraph().getEdgeSource(itr.next());
					Vector<Argument> incoming_data = source_task.getOutput();
					if(task_inputs.containsAll(incoming_data) == false) {
						errors.add("Line "+(td.getLine()+1)+": Input signature of \""+td.getName()+"\" does not match name, type, or count, of incoming edges, missing " + incoming_data);
					}
					
				}
				// Special case: if an incomming edge is a predicate, then this does not 
				// satisfy input match to output, as predicate is not guaranteed to always be originator / source
				
			}
		}
		return errors;
	}
}
