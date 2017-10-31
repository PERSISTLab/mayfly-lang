package edu.clemson.mayfly.ast;


import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

public class TaskDefinition {
	private static int id_counter = 0;
	private int node_id;
	private String name;
	private Vector<Argument> input;
	private Vector<Argument> output;
	private Vector<Constraint> generation;
	private int line;
	private HashMap<String, Constraint> generation_constraints;
	public TaskDefinition(int line, String name, Vector<Argument> input, Vector<Argument> output, Vector<Constraint> generation) {
		this.line = line;
		this.name = name;
		this.input = input;
		this.output = output;
		this.generation = generation;
		node_id = id_counter++;
		generation_constraints = new HashMap<String, Constraint>();
		for (Constraint constraint : generation) {
			this.addGenerationConstraint(constraint);
		}
		
	}
	
	public Collection<Constraint> getGenerationConstraints() {
		return generation_constraints.values();
	}
	public Constraint generationConstraintUsed(String name) {
		return generation_constraints.get(name);
	}
	
	public void removeGenerationConstraint(String name) {
		generation_constraints.remove(name);
	}
	
	public void addGenerationConstraint(Constraint c) {
		// Remove any old constraints
		// Look at old constraints, and decide if we replace or not
		// This is because multiple outgoing edges of this task can have different generation constraints
		if(generation_constraints.containsKey(c.getName())) {
			Constraint oldc = generation_constraints.get(c.getName());
			if(c.getName().equals("misd")) {
				try {
					TimeValue tv_oldc = new TimeValue(oldc.getArguments().get(0));
					TimeValue tv_newc = new TimeValue(c.getArguments().get(0));
					if(tv_newc.getMSValue() > tv_oldc.getMSValue()) {
						removeGenerationConstraint(c.getName());
						generation_constraints.put(c.getName(), c);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			generation_constraints.put(c.getName(), c);
		}
		
	}
	
	public int getLine() {
		return line;
	}
	public String getName() {
		return name;
	}
	
	public Vector<Argument> getInput() {
		return input;
	}
	
	public Vector<Argument> getOutput() {
		return output;
	}
	
	// Checks to see if argument vector v1 matches argument vector v2
	protected static boolean isArgMatch(Vector<Argument> v1, Vector<Argument> v2) {
		if (v1.size() != v2.size())
			return false;

		for (int i = 0; i < v1.size(); i++) {
			if (!(v1.get(i)).getType().equals((v2.get(i)).getType())) {
				return false;
			}
		}
		return true;
	}
	
	// Checks to see if input matches input of node n
	public boolean isInMatch(TaskDefinition n) {
		return isArgMatch(getInput(), n.getInput());
	}
	
	// Checks to see if output matches output of node n
	public boolean isOutMatch(TaskDefinition n) {
		return isArgMatch(getOutput(), n.getOutput());
	}
	
	// Checks to see if input of node matches output of another node
	public boolean isInMatchOut(TaskDefinition n) {
		return isArgMatch(getInput(), n.getOutput());
	}
	
	// Checks to see if node is equal to object o
	public boolean equals(Object o) {
		if (o instanceof TaskDefinition) {
			TaskDefinition n = (TaskDefinition) o;
			return n.getName().equals(getName()) && n.isInMatch(this)
					&& n.isOutMatch(this);
		}
		return false;
	}
	
	// Displays node name, inputs, and outputs in form of a string
	public String toString() {
			//return "Name = "+name+"\n"+"Inputs = "+input.toString()+"\n"+"Outputs = "+output.toString()+"\n";
		return name;
	}

	public int getNodeId() {
		return node_id;
	}
}
