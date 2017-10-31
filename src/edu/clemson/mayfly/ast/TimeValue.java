package edu.clemson.mayfly.ast;

/**
 * An argument for a function, contains a name and a type
 **/
public class TimeValue {
    private Integer value_s;
    private String unit;
    private Integer value_orig;
    
    final static public int M_SEC = 1;
    final static public int M_MIN = 60;
    final static public int M_HR = 60;
   
    /**
     * Constructor
     * @param value the timevalue string.
     **/
    public TimeValue(Object val) throws Exception{
    	int numval = 0;
    	String textUnit = "s";
    	if(val instanceof String) {
    		String value = (String) val;
    		//split value
    		String addspace = "";
    		boolean found = false;
    		for (int i=0; i < value.length(); i++)
    		{
    			if (!found && Character.isLetter(value.charAt(i)))
    			{
    				found = true;
    				addspace = addspace + " ";
    			}
    			addspace = addspace + value.charAt(i);
    		}
    		String[] tokens = addspace.split(" ");
    		if (tokens.length > 2) throw new Exception("Invalid time value "+value);

    		numval = Integer.parseInt(tokens[0]);
    		textUnit="";
    		if (tokens.length == 2)
    		{
    			textUnit = tokens[1];
    		} else {
    			textUnit = "S";
    		}

    		if (numval < 0) throw new Exception ("Error Negative Time value \""+tokens[0]+"\"");
    	}
    	this.unit = textUnit;
    	this.value_s = -1;;
    	if (this.unit.equalsIgnoreCase("S")) this.value_s = numval;
    	if (this.unit.equalsIgnoreCase("MIN")) this.value_s = numval * M_MIN;
    	if (this.unit.equalsIgnoreCase("HR")) this.value_s = numval * M_MIN * M_HR;
    	
    	if (value_s < 0) throw new Exception ("Unsupported time unit \""+textUnit+"\"");
    	
    	this.value_orig  = numval;
    }    
    
    public Integer getMSValue() {
        return this.value_s;
    }
    
    public Integer getOriginalValue() {
        return this.value_orig;
    }
    
    /**
     * Get the name
     * @return The name of the argument
     **/
    String getUnits() {
        return this.unit;
    }
    
    /**
     * Get the string representation of this argument
     * @return A string representing this argument
     **/
    public String toString() {
        return "time("+this.value_s + " ms = "+this.value_orig +" "+this.unit+")";
    }
}
