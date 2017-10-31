package edu.clemson.mayfly.ast;

import java.util.Objects;

/**
 * An argument for a function, contains a name and a type
 **/
public class Argument {
    private String type;
    private String name;
    private boolean array = false;
    public boolean last = false;
    private int numitems = 2;
    
    /**
     * Constructor
     * @param argumentType The type of this argument
     * @param argumentName The name of this argument
     **/
    public Argument(String argumentType, String argumentName) {
	    this.type = argumentType;
	    this.name = argumentName;
	    array = false;
    }
    
    public Argument(String argumentType, String argumentName, boolean isArray, int num) {
	    this.type = argumentType;
	    this.name = argumentName;
	    this.array = isArray;
	    this.numitems = num;
    }
    
    /**
     * Get the type
     * @return The type of the argument
     **/
    public String getType() {
        return this.type;
    }
    
    /**
     * Get the name
     * @return The name of the argument
     **/
    public String getName() {
        return this.name;
    }
    
    /**
     * Get the string representation of this argument
     * @return A string representing this argument
     **/
    public String toString() {
        //return this.name + ":" + this.type;
    	return this.type + " " + this.name;
    }
    
    public boolean isArray() {
    	return array;
    }
    
    public int getNumitems() {
    	return numitems;
    }
    
    @Override
    public boolean equals(Object obj) {
    	if(obj instanceof Argument) {
    		Argument a = (Argument) obj;
    		if(a.getType().equals(this.getType()) && 
    				a.getName().equals(this.getName())) return true;
    		else return false;
    	} else return false;
    }
  
    @Override
    public int hashCode() {
    	return Objects.hash(this.getName(), this.getType());
    }
    
}
