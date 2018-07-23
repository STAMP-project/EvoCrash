/**
 * Copyright (C) 2017 Mozhan Soltani, Annibale Panichella, and Arie van Deursen
 *
 * This file is part of EvoCrash.
 *
 * EvoCrash is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoCrash is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */

package org.evosuite.coverage.evocrash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

//import org.crash.client.CrashProperties;
import org.evosuite.utils.LoggingUtils;

public class LogParser {
	
	/**
	 * Returns the target class based on a given target frame level.
	 * 
	 * @param logPath
	 * @param lineNumber
	 * @return
	 */
	public static String getTargetClass(String logPath, int lineNumber){
		String targetClass = null;

		try {
			CrashProperties.getInstance();
			// Opening the log file
			File file = new File(logPath);
			BufferedReader br = new BufferedReader(new FileReader(file));
			
			// Skipping the first line in the crash stack trace
			br.readLine();
			//going to parse the stack frames
			String otherLine;
			int frame_counter = 1;

			while (frame_counter <= lineNumber) {
				otherLine = br.readLine();
				if (frame_counter == lineNumber){
					if (otherLine != null){
						StringTokenizer space_tokenizer = new StringTokenizer(otherLine, " ");
						space_tokenizer.nextToken();
						String theLine = space_tokenizer.nextToken();
						StringTokenizer dot_tokenizer = new StringTokenizer(theLine, ".");
						String tempClass = getClassFromLine(theLine, dot_tokenizer);
						if (tempClass.contains("$")) {
							StringTokenizer dollarTokenizer = new StringTokenizer(tempClass, "$");
							targetClass = dollarTokenizer.nextToken();
						} else {
							targetClass = tempClass;
						}
					}
				}
				frame_counter++;
			}
			  
			LoggingUtils.getEvoLogger().info("* The target class is:" + targetClass);
			   
			//Closing the file...  
			br.close();
		} catch (Exception e){
			LoggingUtils.getEvoLogger().error("* LogParser: Failed to parse the log file to get the target class!");
		}
		
		return targetClass;
	}

	public static int getNumberOfFrames(String logPath){
		int counter = -1;
		try {
			// Opening the log file
			File file = new File(logPath);
			BufferedReader br = new BufferedReader(new FileReader(file));
			// Skipping the first line in the crash stack trace
			while(br.readLine() != null)
				counter++;
			br.close();
			return counter;
			//going to parse the stack frames
			} catch (Exception e){
			LoggingUtils.getEvoLogger().error("* LogParser: Failed to parse the log file to get the target class!");
		}finally {
				return counter;
		}
	}

//*******************************************************************************
// This block prepares the stack trace to be used by the fitness function
	
	// Reads crash stack trace, and returns the target exception.
	public static Throwable getTargetException (String filePath) {
		return parseLog();
	}
	
	// Parses the crash file and returns the target exception.
	private static Throwable parseLog() {
		ArrayList <StackTraceElement> stack = new ArrayList<> ();

		try {
			CrashProperties.getInstance();
			// Open the log file
			File file = new File(CrashProperties.EXP);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String firstLine = br.readLine();
			    
			// going to parse the exception line
			if (firstLine != null) {
			  	CrashProperties.TARGET_EXCEPTION_CRASH = parseFirstLine(firstLine);
			  	LoggingUtils.getEvoLogger().info("* Target exception was set to: " + CrashProperties.TARGET_EXCEPTION_CRASH);
			}
			    
			//going to parse the stack frames
			String otherLine;
			int frame_counter = 1;
			while ((otherLine = br.readLine()) != null && frame_counter <= CrashProperties.TARGET_FRAME) {
			   	stack.add(parseNormalStackLine(otherLine));
			   	frame_counter++;
			}
			  
//			LoggingUtils.getEvoLogger().info("* Passed the target level " + (frame_counter-1) + ", so stopping the parser!");
			    
			//Closing the file...  
			br.close();
		} catch (Exception e){
			LoggingUtils.getEvoLogger().error("* LogParser: Failed to parse the log file!");
		}
		
		//Converting the arraylist to the array of stack elements
		StackTraceElement [] stackArray = new StackTraceElement [stack.size()];
		stackArray = stack.toArray(stackArray);
				
		//Creating the target exception
		Throwable targetException = new Exception();
		targetException.setStackTrace(stackArray);
		
		return targetException;

	}
	
	private static String parseFirstLine(String firstLine) {
		StringTokenizer st = new StringTokenizer(firstLine, ":");
		return st.nextToken();
	}
	
	// Parses a frame level and returns an stack element
	private static StackTraceElement parseNormalStackLine(String otherLine) {
		StringTokenizer st = new StringTokenizer(otherLine, " ");
		st.nextToken();
		String secondPiece = st.nextToken();
		StringTokenizer secondTokenizer = new StringTokenizer(secondPiece, ".");
		
		String clazz = getClassFromLine(otherLine , secondTokenizer);
		String method = getMethodFromLine(otherLine, secondTokenizer);
		String lineNum = getLineFromLine(otherLine, secondTokenizer);
		
		StackTraceElement stackElement = new StackTraceElement(clazz, method, null, Integer.parseInt(lineNum));
		return stackElement;
	}
	
	
	/**
	 * Returns the target class from the input line
	 * @param line
	 * @param secondTokenizer
	 * @return
	 */
	private static String getClassFromLine(String line, StringTokenizer secondTokenizer){
		String className = "";
		int tokens = secondTokenizer.countTokens();
		for (int index = 1 ; index <= tokens; index++) {
			if (index < tokens-1) {
				if (index == tokens-2) {
					className += secondTokenizer.nextToken();
				}else {
					className += secondTokenizer.nextToken() + ".";
				}
			}
		}
		return className;
	}
	
	
	/**
	 * Returns the target method from the input line
	 * @param line
	 * @param secondTokenizer
	 * @return
	 */
	private static String getMethodFromLine(String line, StringTokenizer secondTokenizer){
		String methodName = "";
		int tokens = secondTokenizer.countTokens();
		for (int index = 1 ; index <= tokens; index++) {
			if (index == tokens-1){
				String methodPiece = secondTokenizer.nextToken();
				if (methodPiece.contains("<init>")){
					StringTokenizer space_Tokenizer = new StringTokenizer(line, " ");
					space_Tokenizer.nextToken();
					String theLine = space_Tokenizer.nextToken();
					StringTokenizer dot_Tokenizer = new StringTokenizer(theLine, ".");
					methodName = getClassFromLine(theLine, dot_Tokenizer);
				}else {
					methodName = methodPiece.substring(0, methodPiece.indexOf('('));
				}	
			}
		}
		return methodName;
	}
	
	
	/**
	 * Returns the target line from the input line
	 * @param line
	 * @param secondTokenizer
	 * @return
	 */
	private static String getLineFromLine(String line, StringTokenizer secondTokenizer){
		String lineNumber = "";
		int tokens = secondTokenizer.countTokens();
		for (int index = 1 ; index <= tokens; index++) {
			if (index == tokens){
				String linePiece = secondTokenizer.nextToken();
				lineNumber = linePiece.substring(linePiece.indexOf(":")+1, linePiece.indexOf(")"));
			}
		}
		return lineNumber;
	}

//*******************************************************************************
//old routine - not used now - was written for cases where the source is unknown.
	
	// If the frame contains "Unknown" source, this parser is used.
	private static StackTraceElement parseUnknownSourceLine(String otherLine) {
		StringTokenizer st = new StringTokenizer(otherLine, " ");
		st.nextToken();
		String secondPiece = st.nextToken();
		StringTokenizer secondTokenizer = new StringTokenizer(secondPiece, ".");
		int tokens = secondTokenizer.countTokens();
		String clazz = "";
		String method = "";
		//String lineNum = ""; Not needed anymore!
		for (int index = 1 ; index <= tokens; index++) {
			if (index < tokens) {
				if (index == tokens-1) {
					clazz += secondTokenizer.nextToken();
				}else {
					clazz += secondTokenizer.nextToken() + ".";
				}
			}else if (index == tokens){
				String methodPiece = secondTokenizer.nextToken();
				method = methodPiece.substring(0, methodPiece.indexOf('('));
				// -1 is going to be assigned as the line number!
			}

		}
		StackTraceElement stackElement = new StackTraceElement(clazz, method, null, (-1));
		return stackElement;
	}
	
	

}