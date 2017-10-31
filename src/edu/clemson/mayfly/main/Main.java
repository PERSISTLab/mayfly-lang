package edu.clemson.mayfly.main;

import java.io.FileWriter;
import java.util.HashMap;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import edu.clemson.mayfly.ast.Program;
import edu.clemson.mayfly.compile.MayflyCompiler;
/**
 * 
 * @author Josiah Hester <josiah@northwestern.edu>
 *
 */
public class Main {
    @Parameter(names={"--filename", "-f"}, required = false, description = "Filename of Mayfly code")
    String filename;
    @Parameter(names={"--mcu", "-m"}, required = false, description = "MSP430 target processor")
    String mcu = "msp430fr5969";
    @Parameter(names={"--scheduler", "-s"}, required = false, description = "Scheduler type [simple, random]")
    String scheduler = "simple";
    @Parameter(names={"--autogen", "-a"}, required = false, description = "Autogenerate code from mayfly graph file")
    String autogen = "false";
    @Parameter(names={"--output", "-o"}, required = false, description = "Name and path of output file to write to")
    String outputfile = "gen/main.c";
    @Parameter(names={"--help", "-h"}, help = true)
    private boolean help = false;
    
	public static void main(String argv[]) {    
	
		Main main = new Main();
		JCommander jc = new JCommander(main, argv);
		jc.setProgramName("MayflyCompiler_v1.0.0");
	
		if(argv.length == 0) {
			System.err.println("This is the Mayfly compiler, version 1.0.0, built for MSP430 FRAM devices.");
			System.err.println("Check the repository for updates: https://github.com/PERSISTLab/mayfly-lang");
			System.err.println("Author: Josiah Hester <josiah@northwestern.edu>");
			jc.usage();
			return;
		}	
		if (main.help) {
			System.err.println("This is the Mayfly compiler, version 1.0.0, built for MSP430 FRAM devices.");
			System.err.println("Check the repository for updates: https://github.com/PERSISTLab/mayfly-lang");
			System.err.println("Author: Josiah Hester <josiah@northwestern.edu>");
			jc.usage();
			return;
		}

		if(main.filename == null) {
			System.err.println("Incorrect usage...");
			jc.usage();
			return;		
		}
		
		if(!main.scheduler.equals("simple") && !main.scheduler.equals("random")) {
			System.err.println("Scheduler type \""+main.scheduler+" \"not supported...");
			jc.usage();
			return;	
		}
		
		MayflyCompiler mc = new MayflyCompiler();
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("filename", main.filename);
		args.put("mcu", main.mcu);
		args.put("scheduler", main.scheduler);
		args.put("autogen", main.autogen);
		/* Start the parser */
		try {
			String code = mc.compile(args);

			FileWriter fw = new FileWriter(main.outputfile);
			fw.write(code);
			fw.flush();
			fw.close();

			System.out.println(code);
		} catch (Exception e) {
			/* do cleanup here -- possibly rethrow e */
			e.printStackTrace();
		}
	}
}
