package edu.clemson.mayfly.ast;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Program {
	
    /**
     * The supported flow policies (the same as global policies), and the supported edge constraints
     * are initialized here. 
     */
	private static final Map<String, Integer> SUPPORTED_FLOW_POLICIES;
	private static final Map<String, Integer> SUPPORTED_EDGE_CONSTRAINTS;
    static {
    	Map<String, Integer> spl = new HashMap<String, Integer>();
    	spl.put("scheduling_method", 1);
    	spl.put("priority", 1);
        SUPPORTED_FLOW_POLICIES = Collections.unmodifiableMap(spl);

    	Map<String, Integer> ec = new HashMap<String, Integer>();
    	ec.put("expires", 1);
    	ec.put("misd", 1);
    	ec.put("collect", 1);
    	SUPPORTED_EDGE_CONSTRAINTS = Collections.unmodifiableMap(ec);
    }
    
    public static int getPolicyArguments(String key) {
    	return SUPPORTED_FLOW_POLICIES.get(key);
    }
    
    public static int getEdgeConstraintArguments(String key) {
    	return SUPPORTED_EDGE_CONSTRAINTS.get(key);
    }
    
    public static boolean isPolicySupported(String name) {
    	return SUPPORTED_FLOW_POLICIES.containsKey(name);
    }
    
    public static boolean isEdgeConstraintSupported(String name) {
    	return SUPPORTED_EDGE_CONSTRAINTS.containsKey(name);
    }
	
	private Vector<Constraint> policies;
	private Vector<TaskDefinition> task_defs;
	private HashMap<String, TaskDefinition> task_def_lookup;
	private Vector<Flow> flow_list;
	private Vector<Edge> constraint_list;
	private  HashMap<String, Edge> edge_lookup;
	private Vector<String> error_list;
	
	public Program(Vector<Constraint> policies, Vector<TaskDefinition> task_defs, Vector<Flow> flow_list, Vector<Edge> new_constraint_list) {
		this.task_defs = task_defs;
		this.flow_list = flow_list;
		this.policies = policies;
		this.edge_lookup = new HashMap<String, Edge>();
		error_list = new Vector<String>();
		
		// Generate edges from the flow list with empty constraints
		for(Flow fl : flow_list) {
			Vector<FlowPart> flowparts =  fl.getFlowTasks();
			for(int i=1;i<flowparts.size();i++) {
				Edge e = new Edge(fl.getLine(), flowparts.elementAt(i-1).getName(), flowparts.elementAt(i).getName(), new Vector<Constraint>());
				edge_lookup.put(e.getSource()+"->"+e.getDest(), e);
			}
		}
		
		// Add in constraints
		for(Edge edge: new_constraint_list) {
			Edge existing_edge = edge_lookup.get(edge.getSource()+"->"+edge.getDest());
			if(existing_edge == null) {
				if(edge.getDepends().size() > 0) {
					System.err.println("ERROR: Constraint references undefined edge between "+edge.getSource()+" and "+edge.getDest()+" on line "+edge.getDepends().get(0).getLine());
				} else {
					System.err.println("ERROR: Constraint references undefined edge between "+edge.getSource()+" and "+edge.getDest());
				}
				
			}
			existing_edge.addConstraints(edge.getDepends());
			edge_lookup.put(edge.getSource()+"->"+edge.getDest(), existing_edge);
		}
				
		// Get list of edges from lookup
		this.constraint_list = new Vector<Edge>(edge_lookup.values());
		
		// Parse Tasks to make a lookup
		this.task_def_lookup = new HashMap<String, TaskDefinition>();
		for(TaskDefinition task: task_defs) {
			task_def_lookup.put(task.getName(), task);
		}
		
		System.out.println();
	}

	public Vector<TaskDefinition> getTaskDefinitions() {
		return task_defs;
	}
	
	public boolean isTaskDefined(String name) {
		return task_def_lookup.containsKey(name);
	}
	
	public TaskDefinition getTaskDefinition(String name) {
		return task_def_lookup.get(name);
	}

	public Vector<Flow> getFlows() {
		return flow_list;
	}

	public Vector<Edge> getEdges() {
		return constraint_list;
	}
	
	public boolean isEdgeDefined(String name) {
		return edge_lookup.containsKey(name);
	}
	
	public Edge getEdge(String name) {
		return edge_lookup.get(name);
	}
	
	public Vector<Constraint> getPolicies() {
		return policies;
	}

	
	public void addError(String s) {
		if (!error_list.contains(s)) {
			error_list.add(s);	
		}
	}
	
	public boolean checkForErrors() {
		if (error_list.size() > 0) {
			for (int i = 0; i < error_list.size(); ++i) {
				System.err.println(error_list.elementAt(i));
			}
			System.err.println("Compilation Terminated.");
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return
				"== Policies==\n"+
				policies.toString()+"\n\n"+
				"== TaskDefs ==\n"+
				task_defs.toString()+"\n\n"+
				"== Flows ==\n"+
				flow_list.toString()+"\n\n"+
				"== Edge Constraints ==\n"+
				constraint_list.toString()+"\n\n";
				
	}
	
}
