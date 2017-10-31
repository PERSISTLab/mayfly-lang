package edu.clemson.mayfly.ast;


import java.util.HashMap;
import java.util.Vector;

import org.jgrapht.graph.DefaultEdge;

public class Edge extends DefaultEdge
{
	private String source;
	private String destination;
	private Vector<Constraint> constraints;
	private int line;
	private boolean array;
	private int expires;
	private HashMap<String, Constraint> constraint_lookup;
	public Edge(int line, String node1, String node2, Vector<Constraint> depends) {
		this.line = line;
		source = node1;
		destination = node2;
		this.constraints = depends;
		array = false;
		constraint_lookup = new HashMap<String, Constraint>();
		for(Constraint c : depends) {
			constraint_lookup.put(c.getName(), c);
			if(c.getName().equals("collect")) { 
				array = true;
			}
			
		}
	}
	
	public Constraint constraintOnEdge(String name) {
		return constraint_lookup.get(name);
	}
	

	public void removeConstraint(String name) {
		constraint_lookup.remove(name);
		for(Constraint c : constraints) {
			if(c.getName().equals(name)) { 
				constraints.remove(c);
				break;
			}
		}
	}
	
	public void addConstraints(Vector<Constraint> cs) {
		for(Constraint c : cs) {
			addConstraint(c);
		}
	}
	
	public void addConstraint(Constraint c) {
		// Remove any old constraints
		removeConstraint(c.getName());
		constraints.add(c);
		constraint_lookup.put(c.getName(), c);
	}
	
	public void setPredicate(String target, int value) {
		// Add to the constraints
		Vector<Object> predicate_args = new Vector<Object>();
		predicate_args.addElement(target);
		predicate_args.addElement(value);
		Constraint c = new Constraint(line, "predicate", predicate_args);
		constraints.add(c);
	}
	
	public boolean isArray() {
		return array;
	}
	
	public int getLine() {
		return line;
	}
	
	public String getSource()
	{
		return source;
	}
	
	public String getSourceCaps() {
		return source.toUpperCase();
	}
	
	public int getExpires() {
		return expires;
	}
	
	public String getDest()
	{
		return destination;
	}
	
	public String getDestCaps()
	{
		return destination.toUpperCase();
	}
	
	
	public Vector<Constraint> getDepends()
	{
		return constraints;
	}
	
	public String toString()
	{
		return "Source: "+source+" Destination: "+destination+" Constraints: "+constraints.toString();
	}
	
}
