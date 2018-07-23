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


//import org.crash.client.CrashProperties;
import org.evosuite.coverage.exception.ExceptionCoverageHelper;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.utils.LoggingUtils;


public class CrashCoverageHelper extends ExceptionCoverageHelper {

	static Throwable targetException = CrashProperties.TARGET_EXCEPTION;
	static StackTraceElement [] targetStackTrace = targetException.getStackTrace();


	public static Throwable getThrownException (ExecutionResult result, int exceptionPosition){
		Throwable t = result.getExceptionThrownAtPosition(exceptionPosition);
		return t;
	}

	protected transient static BytecodeInstruction goalInstruction;

	public static double compareTraces (TestChromosome individual, ExecutionResult result, Integer i) {
		double overallFitness = 0.0;
		double dist = 0;

		Throwable thrownException = result.getExceptionThrownAtPosition(i);
		StackTraceElement [] thrownStackTrace = thrownException.getStackTrace();
		dist = stackTracesFastDistance(targetStackTrace, thrownStackTrace);

		overallFitness = dist;
		return normalize(overallFitness);
	} 

//*******************************************************************************************************
// Trace distance calculators: 	

	public static double stackTracesFastDistance (StackTraceElement [] targetTrace, StackTraceElement [] actualTrace){
		double stackDistance = 0;

		int position = -1;
		// iterating through the target stack frames
		for (int i = 0 ; i < targetTrace.length; i++){
			// if the target frame is using reflection objects, we jump and skip the evaluation!
			if (targetTrace[i].getClassName().contains("reflect") && targetTrace[i].getClassName().contains("invoke")){
				continue;
			} 
			StackTraceElement targetElement = targetTrace[i];
			double minDistance = 1 ;
			// iterating through the generated trace frames
			for (int j = position+1 ; j < actualTrace.length; j++){
				double dist = 1;
				StackTraceElement actualElement = actualTrace[j];
				if (actualElement.getClassName().contains("evosuite")){
					dist = 0;
					continue;
				} else
					dist = stackElementsDistance(targetElement, actualElement) ;
				if (dist < minDistance){
					minDistance = dist;
					position = j;
				}
			
			}
			
			stackDistance += minDistance ;
		}
		
		return normalize(stackDistance);
	}


	public static double  stackElementsDistance (StackTraceElement ste1, StackTraceElement ste2){
		double elemnetDistance = 0;
		if (!ste1.getClassName().equals(ste2.getClassName())){
			elemnetDistance++;
			elemnetDistance++;
			elemnetDistance++;
		} else {
			//if we added the class name as the constructor call
			if(ste1.getMethodName().contains(".")){
				if (!ste2.getMethodName().equals("<init>")) {
					LoggingUtils.getEvoLogger().info("Method names were not equal......................");
					LoggingUtils.getEvoLogger().info("method1: " + ste1.getMethodName() + "method2: " + ste2.getMethodName()+ "\n");
					elemnetDistance++;
					elemnetDistance++;
				}
			}
			else if (!ste1.getMethodName().equals(ste2.getMethodName())) {
				elemnetDistance++;
				elemnetDistance++;
			}  else 
				elemnetDistance = normalize(Math.abs(ste1.getLineNumber() - ste2.getLineNumber()));
		}


		return normalize(elemnetDistance);
	}


	/**
	 * Normalize a value using Andrea's normalization function
	 * 
	 * @param value
	 *            a double.
	 * @return a double.
	 * @throws java.lang.IllegalArgumentException
	 *             if any.
	 */
	public static double normalize(double value) throws IllegalArgumentException {
		if (value < 0d) {
			throw new IllegalArgumentException("Values to normalize cannot be negative");
		}
		if (Double.isInfinite(value)) {
			return 1.0;
		}
		return value / (1.0 + value);
	}

}