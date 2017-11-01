Mayfly Compiler 
======
<img src="https://raw.githubusercontent.com/PERSISTLab/mayfly-lang/master/media/logo.png" width="128" style="float: left;"> This repository hosts the Mayfly language specification and compiler acompanying the paper titled [*Timely Execution on Intermittently Powered Batteryless Sensors*](http://josiahhester.com/cv/files/mayflysensys2017.pdf) in the 15th ACM Conference on Embedded Networked Sensor Systems [(SenSys 2017)](http://sensys.acm.org/2017/).

This project is part of an ongoing collaboration between Clemson University and Northwestern University, originally initiated at Clemson under Prof. Jacob Sorber, with Josiah Hester.

## Setup
First ensure JAVA, and ANT are installed.

To build and upload firmware to WISP, or Flicker devices (or other MSP430 based device), refer to those projects: just make sure to update the MSPGCC_ROOT variable in the Makefile.

Only MSP430 devices with FRAM are supported at this time as compiler output.

## Usage

`ant generate`

Takes the parser grammar (CUP) and lexer rules (JFlex) and generates the actual parser and lexer for Mayfly.

`ant compile`

Compiles the Mayfly compiler.

`ant dist`

Makes a fully encapsulated executable JAR file of the compiler for distribution and usage.

	$>	java -jar dist/mayfly-launch.jar
	This is the Mayfly compiler, version 1.0.0, built for MSP430 FRAM devices.
	Check the repository for updates: https://github.com/PERSISTLab/mayfly-lang
	Author: Josiah Hester <josiah@northwestern.edu>
	Usage: MayflyCompiler_v1.0.0 [options]
	  Options:
	    --autogen, -a     Autogenerate code from mayfly graph file
	                      Default: false
	    --filename, -f    Filename of Mayfly code
	    --help, -h
	                      Default: false
	    --mcu, -m         MSP430 target processor
	                      Default: msp430fr5969
	    --output, -o      Name and path of output file to write to
	                      Default: gen/main.c
	    --scheduler, -s   Scheduler type [simple, random]
	                      Default: simple


This takes a mayfly program and backing C-source code and generates an embedded-C program for MSP430 FRAM devices. First the Mayfly compiler is compiled (if necesary), then the Mayfly program source is parsed, and the Mayfly scheduler is generated for the specific tasks named by the end user. The code leverages the libraries in `lib` to work on WISP devices with an off-chip RTC. The generated code can be compiled by `msp430-elf-gcc` and then run on the WISP platform.

This tool also embeds a graph of the specified Mayfly program in ASCII, in the generated source code. For example for a very simple Mayfly program that samples the accelerometer, with no edge dependancies:

	// Define Nodes -- infer inputs and outputs that are omitted.
	sample_xl ( ) => (uint16_t x, uint16_t y, uint16_t z);
	wait (uint16_t x, uint16_t y, uint16_t z) => ( );
	// Flow
	sample_xl -> wait {
		
	}
	
	//Edge Dependencies
	sample_xl => wait {
	}

This graph is generated.

	/* ----------------- Task Graphs ---------------- */
	// Task Graph format
	//	+----------------+
	//	|  NODE_ID:NAME  |  [CONSTRAINTS]
	//	|    [inputs]    | --------------->
	//	+----------------+
	/* ---------------------------------------------- */
	/*
	+------------------------------------------+       +-----------------------------------------+
	|               0:sample_xl                |       |                 1:wait                  |
	|                  in:[]                   |       | in:[uint16_t x, uint16_t y, uint16_t z] |
	| out:[uint16_t x, uint16_t y, uint16_t z] |  []   |                 out:[]                  |
	|                  gen:[]                  | ----> |                 gen:[]                  |
	+------------------------------------------+       +-----------------------------------------+
	
	 */

## Simplified Language Specification
Task definitions, data flows, edge constraints are required. 

Optional global policy information. 

- **expires**(time) : The amount of time data can sit on an edge.

- **misd**(time) : The amount of time before more data is useful.

- **collect**(quantity,duration) : Gather a set of data coming out of the task, over a duration.


## Contributors

- Siara Fabra and Chris Datko contributed early on to the Mayfly language for an REU in the summer of 2014.
- [Kevin Storer](https://kevinstorer.com/) contributed to the language design, and designed and conducted the user studies in 2016.
- [Prof. Josiah Hester](http://josiahhester.com/cv/) guided the early effort and continued on after summer as a graduate student in the PERSIST Lab, implementing the compiler and supporting systems. Prof. Hester continues this work at Northwestern University.
- [Prof. Jacob Sorber](https://people.cs.clemson.edu/~jsorber/) oversaw, funded, and originated the project.

## Funding 

This research is based upon work supported by the National Science Foundation under grants [CNS-1453607](https://nsf.gov/awardsearch/showAward?AWD_ID=1453607) and [CCF-1539536](https://www.nsf.gov/awardsearch/showAward?AWD_ID=1539536) Any opinions, findings, and conclusions or recommendations expressed in this material are those of the authors and do not necessarily reflect the views of the National Science Foundation

## License

	MIT License
	Copyright 2017 Josiah Hester, Kevin Storer, Jacob Sorber

	Permission is hereby granted, free of charge, to any person obtaining a copy of this 
	software and associated documentation files (the "Software"), to deal in the Software 
	without restriction, including without limitation the rights to use, copy, modify, merge, 
	publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons 
	to whom the Software is furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all copies or 
	substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
	INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
	PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
	FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
	OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
	DEALINGS IN THE SOFTWARE.