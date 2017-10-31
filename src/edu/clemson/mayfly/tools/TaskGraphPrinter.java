package edu.clemson.mayfly.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import edu.clemson.mayfly.ast.Edge;
import edu.clemson.mayfly.ast.TaskDefinition;
import edu.clemson.mayfly.compile.TaskGraph;

public class TaskGraphPrinter {
	
	public static String renderTaskGraphASCII(TaskGraph tg) {
		String retval = "";
		String dotgraph = "digraph GRAPH_0 {\n"+
				"edge [ arrowhead=open ];\n"+
				"graph [ rankdir=LR ];\n"+
				"node [fontsize=11,fillcolor=white,style=filled,shape=box];\n";
		
		// Vertex naming
		HashMap<String, String> node_map = new HashMap<String, String>();
		for(TaskDefinition td: tg.getGraph().vertexSet()) {
			node_map.put(td.getName(), td.getNodeId()+":"+td.getName() +"\\nin:"+td.getInput() +"\\nout:"+td.getOutput()+"\\ngen:"+td.getGenerationConstraints());
		}
		
		// Connect
		for(Edge td: tg.getGraph().edgeSet()) {
			dotgraph += "\""+node_map.get(td.getSource()) + "\" -> \""+ node_map.get(td.getDest()) + "\"[label=\""+td.getDepends()+"\"]\n";
		}
		dotgraph += "}";
		
		// Use graph-easy to write an ascii representation
		try {  
			Process p = new ProcessBuilder("/usr/local/bin/graph-easy", "--as=ascii").redirectErrorStream(true).start();
	        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
	        writer.write(dotgraph);
	        writer.flush();
	        writer.close();
			StringBuilder inputStringBuilder = new StringBuilder();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = bufferedReader.readLine();
			while(line != null){
				inputStringBuilder.append(line);inputStringBuilder.append('\n');
				line = bufferedReader.readLine();
			}
			p.waitFor();
			retval = inputStringBuilder.toString();

		} catch(Exception e) {
			e.printStackTrace();
		}
		return retval;
	}
}