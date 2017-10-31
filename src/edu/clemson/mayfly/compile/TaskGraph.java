package edu.clemson.mayfly.compile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

import edu.clemson.mayfly.ast.Edge;
import edu.clemson.mayfly.ast.Flow;
import edu.clemson.mayfly.ast.FlowPart;
import edu.clemson.mayfly.ast.TaskDefinition;
import edu.clemson.mayfly.tools.TaskGraphPrinter;

public class TaskGraph {
	private DirectedMultigraph<TaskDefinition, Edge> graph;
	private TaskDefinition source; // Only one source task allowed right now 

	private Vector<TaskDefinition> sinks;
	private Vector<TaskDefinition> sorted;
	private Vector<Flow> all_flows;
	private int priority;
	
	/* Start fields for the template */
	public String name_caps() {
		return source.getName().toUpperCase()+"_TO_"+sinks.get(0).getName().toUpperCase();
	}
	
	public String name() {
		return source.getName()+"_to_"+sinks.get(0).getName();
	}
	
	public int num_tasks() {
		return sorted.size();
	}
	
	public String exec_list() { 
		List<Integer> t = new ArrayList<Integer>();
		for(TaskDefinition td: sorted) {
			t.add(td.getNodeId());
		}
		return t.toString().replace('[', ' ').replace(']', ' '); 
	}
	
	public String graph_render() {		
		return TaskGraphPrinter.renderTaskGraphASCII(this);
	}
	/* End fields for the template */

	public TaskGraph(DirectedMultigraph<TaskDefinition, Edge> graph, Vector<Flow> all_flows) {
		this.graph = graph;
		this.all_flows = all_flows;
		this.sinks = new Vector<TaskDefinition>();
		this.sorted = new Vector<TaskDefinition>();
		this.priority = 100;
		
		// Process graph for useful info to put in the template
		getSinksAndSource();	
		sortTasks();
		determineGraphPriority();
	}
	
	private void getSinksAndSource() {
		Iterator<TaskDefinition> itr = graph.vertexSet().iterator();
		while(itr.hasNext()) {
			TaskDefinition nxt = itr.next();
			// Source
			if(graph.inDegreeOf(nxt) == 0) {
				source = nxt;
			}
			
			// Sink
			if(graph.outDegreeOf(nxt) == 0) {
				sinks.add(nxt);
			}
		}
		
		// TODO: Move this error handline to validation, this is ugly
		if(source == null) {
			System.err.println("ERROR: No task defined is a source.");
			System.exit(1);
		}
		
		if(sinks.size() == 0) {
			System.err.println("ERROR: No task is a sink.");
			System.exit(1);
		}
	}
	
	/**
	 * After this, the tasks will be in priority order in "sorted"
	 * Generates priorities by topological sort from the source, and then reversing
	 */
	private void sortTasks() {
		BreadthFirstIterator<TaskDefinition, Edge> orderIterator = 
				new BreadthFirstIterator<TaskDefinition, Edge>(graph);
		while (orderIterator.hasNext()) {
			sorted.add(orderIterator.next());
		}
		Collections.reverse(sorted);
	}
	
	/**
	 * Goes through the flows and determines priority,
	 * by taking the highest priority defined by a flow as the priority
	 * of the entire flow
	 */
	private void determineGraphPriority() {
		HashSet<Flow> task_flows = new HashSet<Flow>();
		for(Flow f:all_flows) {
			for(TaskDefinition t: sorted) {
				if(f.isTaskInFlow(t.getName())) {
					task_flows.add(f);
					// Default priority is 100, 1 is highest priority, 2 is lower priority, etc
					if(f.getArgumentsFor("priority") != null) {
						Object s = f.getArgumentsFor("priority").get(0);
						int new_priority = 100;
						if (s instanceof Integer) {
							new_priority = (Integer)s;
						}
						if(new_priority < priority) priority = new_priority;
					}
				}
			}
		}
	}
	
	private void parsePredicates() {

	}
	
	public DirectedMultigraph<TaskDefinition, Edge> getGraph() {
		return graph;
	}

	public Vector<TaskDefinition> getSorted() {
		return sorted;
	}

	public int getPriority() {
		return priority;
	}

	public TaskDefinition getSource() {
		return source;
	}

	public Vector<TaskDefinition> getSinks() {
		return sinks;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "{Tasks: "+sorted.toString()+", "+
				"Priority: "+priority+", " +
		"};";
	}

}
