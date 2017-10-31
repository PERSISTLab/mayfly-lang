package edu.clemson.mayfly.ast;

import java.util.Vector;

public class FlowPart {
	private String name; 
	private Vector<Object> predicate;
	public FlowPart(String name, Vector<Object> predicate) {
		this.name = name;
		this.predicate = predicate;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Vector<Object> getPredicate() {
		return predicate;
	}

	public void setPredicate(Vector<Object> predicate) {
		this.predicate = predicate;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "( "+name+" : "+predicate+" )";
	}
}
