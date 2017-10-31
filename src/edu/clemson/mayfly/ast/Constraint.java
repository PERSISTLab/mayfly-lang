package edu.clemson.mayfly.ast;


import java.util.Vector;

public class Constraint
{
	private String constraint_name;
	private Vector<Object> arg_list;
	private int line;
	public Constraint(int line, String constraint_name, Vector<Object> arg_list) {
		this.line = line;
		this.constraint_name = constraint_name;
		this.arg_list = arg_list;
		
	}
	
	public int getLine() {
		return line;
	}
	
	public String getName()
	{
		return constraint_name;
	}
	public Vector<Object> getArguments()
	{
		return arg_list;
	}
	
	public String toString() {
		return constraint_name+"("+arg_list.toString()+")";
	}

}
