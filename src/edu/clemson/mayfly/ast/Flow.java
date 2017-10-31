package edu.clemson.mayfly.ast;

import java.util.HashMap;
import java.util.Vector;

public class Flow {

	private Vector<FlowPart> flow_list;
	private Vector<Constraint> constraints;
	private HashMap<String, Vector<Object>> constraint_lookup;
	private int line;
	public Flow(int line, Vector<FlowPart> flows, Vector<Constraint> depends) {
		this.line = line;
		this.flow_list = flows;
		this.constraints = depends;
		
		constraint_lookup = new HashMap<String, Vector<Object>>();
		for(Constraint c: constraints) {
			constraint_lookup.put(c.getName(), c.getArguments());
		}
		
	}
	
	public Vector<Object> getArgumentsFor(String name) {
		return constraint_lookup.get(name);
	}
	
	public int getLine() {
		return line;
	}
	
	public Vector<Constraint> getConstraints() {
		return constraints;
	}

	public Vector<FlowPart> getFlowTasks() {
		return flow_list;
	}
	
	public boolean isTaskInFlow(String name) {
		for(FlowPart fp: flow_list) {
			if(fp.getName().equals(name)) return true;
		}
		return false;
		
	}
	
	@Override
	public String toString() {
		String ret = "";
		for(FlowPart f: flow_list) {
			ret += f.getName()+f.getPredicate()+"->";
		}
		return ret.substring(0, ret.length()-4);
	}

}
