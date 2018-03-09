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

import java.util.*;

//import org.crash.client.CrashProperties;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.branch.BranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.exception.ExceptionCoverageHelper;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.utils.LoggingUtils;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Fitness function for a single test
 *
 */
public class CrashCoverageTestFitness extends TestFitnessFunction  {

	private static final long serialVersionUID = 5063274349559091258L;
	private static Logger logger = LoggerFactory.getLogger(CrashCoverageTestFitness.class);
	// a Singleton class which saves all of the coverage histories.
	Throwable targetException;
	public static StackTraceElement [] targetStackTrace;

	public static Set<String> publicCalls = new HashSet <String> ();

	/** Target method */
	private static String className;
	private static String methodName;
	private static int lineNumber;

	protected transient static BytecodeInstruction goalInstruction;

	/**
	 * Constructor - fitness is specific to a method
	 * @param methodIdentifier
	 * @param exceptionClass
	 * @throws IllegalArgumentException
	 */
	public CrashCoverageTestFitness (Throwable targetExcep){
		this.targetException = targetExcep;
		this.targetStackTrace = targetExcep.getStackTrace();
		System.out.println(this.targetException);
		this.publicCalls = getPublicCalls();
	}


	public String getKey(){
		int len = targetStackTrace.length;
		//Using the CUT name and top method caller!
		return targetStackTrace[len-1].getClassName() + "_" + targetStackTrace[len-1].getMethodName();
	}


	@Override
	public double getFitness(TestChromosome individual, ExecutionResult result) {

		if(CrashCoverageInfos.getInstance().getnumberOfTries() > Properties.SEARCH_BUDGET){
			long startTime = CrashCoverageInfos.getInstance().getStartTime();
			long endTime = System.currentTimeMillis();
			long totalTime = (endTime - startTime)/1000;
			LoggingUtils.getEvoLogger().info(">>>>>>>>>>>>>>>>>>>>>>>>>>>GGA was done in " + totalTime+ "!<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			long FFTimePassed = CrashCoverageInfos.getInstance().getFFTime() - CrashCoverageInfos.getInstance().getCurrentTime();
			LoggingUtils.getEvoLogger().info("* Fitness Function/Time passed/Number of tries: "+CrashCoverageInfos.getInstance().getFitnessFunctionMin()+"/"+FFTimePassed+"/"+CrashCoverageInfos.getInstance().getFFTries());
			long LCTimePassed = CrashCoverageInfos.getInstance().getLCTime() - CrashCoverageInfos.getInstance().getCurrentTime();
			LoggingUtils.getEvoLogger().info("* LineCoverage Fitness/Time passed/Number of tries: "+CrashCoverageInfos.getInstance().getLineCoverageMin()+"/"+LCTimePassed+"/"+CrashCoverageInfos.getInstance().getLCTries());
			long EHTimePassed = CrashCoverageInfos.getInstance().getEHTime() - CrashCoverageInfos.getInstance().getCurrentTime();
			LoggingUtils.getEvoLogger().info("* Exception Happened/Time passed/Number of tries: "+CrashCoverageInfos.getInstance().getExceptionHappenedMin()+"/"+EHTimePassed+"/"+CrashCoverageInfos.getInstance().getEHTries());
			long SSTimePassed = CrashCoverageInfos.getInstance().getSSTime() - CrashCoverageInfos.getInstance().getCurrentTime();
			LoggingUtils.getEvoLogger().info("* Stack Trace Similarity/Time passed/Number of tries: "+CrashCoverageInfos.getInstance().getStackTraceSimilarityMin()+"/"+SSTimePassed+"/"+CrashCoverageInfos.getInstance().getSSTries());
			LoggingUtils.getEvoLogger().info("* number of total tries: "+ CrashCoverageInfos.getInstance().getnumberOfTries());
			LoggingUtils.getEvoLogger().info("* Computation finished");
//			notifySearchFinished();
		}
		double currentFitnessValue = 0;
		double lineFitness = 1;
		double hasDeclaredException = 1;
		double stackFitness = 1;

		int targetLength = targetStackTrace.length;
		className = targetStackTrace[targetLength-1].getClassName();
		lineNumber = targetStackTrace[targetLength-1].getLineNumber();
		String uncompleteMethodName = targetStackTrace[targetLength-1].getMethodName();
		methodName = this.derivingMethodFromBytecode(className, uncompleteMethodName, lineNumber);

		// 1) first let's see whether we reach the line where the exception should be thrown
		if (lineNumber == (-1)) {
			lineFitness = 0.0;
		} 
		else {
			lineFitness = getLineFitness(individual, result, lineNumber);
			//LoggingUtils.getEvoLogger().info("lineFitness: " + lineFitness);
		}

		// 2) If we reached the line, let's see whether the exception is thrown
		if(lineFitness == 0.0){
			if (result.getPositionsWhereExceptionsWereThrown().size()>0){

				for (Integer i : result.getPositionsWhereExceptionsWereThrown()) {

					hasDeclaredException = 1;
					stackFitness = 1;

					//ignore uninteresting exceptions
					if(CrashCoverageHelper.shouldSkip(result,i)){
						continue;
					}
					// 2.1) Let's check whether we obtain the target exception
					String thrownExceptionName = ExceptionCoverageHelper.getExceptionClass(result, i).getName();
					if (thrownExceptionName.equals(CrashProperties.TARGET_EXCEPTION_CRASH)){
						hasDeclaredException = 0;
						// 2.1.1) Calculate the trace distance
						double tempFitness = CrashCoverageHelper.compareTraces(individual , result, i);
						if (tempFitness == 0.0){
							stackFitness = 0.0;
							break;
						}else if (tempFitness < stackFitness) {
							stackFitness = tempFitness;
						}
					}

				}

			}
		}

		if (CrashCoverageInfos.getInstance().getLineCoverageMin() > lineFitness) {
			CrashCoverageInfos.getInstance().setLineCoverageMin(lineFitness);
			CrashCoverageInfos.getInstance().setLCTime(System.currentTimeMillis());
			CrashCoverageInfos.getInstance().setLCTries();
			LoggingUtils.getEvoLogger().info("*************** New lineFitness: " + lineFitness);
		}

		if (CrashCoverageInfos.getInstance().getStackTraceSimilarityMin() > stackFitness) {
			CrashCoverageInfos.getInstance().setStackTraceSimilarityMin(stackFitness);
			CrashCoverageInfos.getInstance().setSSTime(System.currentTimeMillis());
			CrashCoverageInfos.getInstance().setSSTries();
		}

		if (CrashCoverageInfos.getInstance().getExceptionHappenedMin() > hasDeclaredException) {
			CrashCoverageInfos.getInstance().setExceptionHappenedMin(hasDeclaredException);
			CrashCoverageInfos.getInstance().setEHTime(System.currentTimeMillis());
			CrashCoverageInfos.getInstance().setEHTries();
		}

		currentFitnessValue =  3 * lineFitness  + 2 * hasDeclaredException + stackFitness;

		if (CrashCoverageInfos.getInstance().getFitnessFunctionMin() > currentFitnessValue) {
			CrashCoverageInfos.getInstance().setFitnessFunctionMin(currentFitnessValue);
			CrashCoverageInfos.getInstance().setFFTime(System.currentTimeMillis());
			CrashCoverageInfos.getInstance().setFFTries();
			LoggingUtils.getEvoLogger().info("*************** New currentFitness: " + currentFitnessValue );
		}

		logger.debug((3*lineFitness)+" "+(2*hasDeclaredException)+" "+stackFitness);

		individual.setFitness(this, currentFitnessValue);
		updateIndividual(this, individual, currentFitnessValue);
		//LoggingUtils.getEvoLogger().info("currentFitness: " + currentFitnessValue + "\n");
		return currentFitnessValue;

	} // end of getFitness!


	@Override
	public int compareTo(TestFitnessFunction other) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((targetException == null) ? 0 : targetException.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CrashCoverageTestFitness other = (CrashCoverageTestFitness) obj;
		if (targetException == null) {
			if (other.targetException != null)
				return false;
		} else if (!targetException.equals(other.targetException))
			return false;
		return true;
	}


	@Override
	public String getTargetClass() {

		return Properties.getInitializedTargetClass().getCanonicalName();
	}


	@Override
	public String getTargetMethod() {
		//Assuming it is already specified in Properties!
		return Properties.TARGET_METHOD;
	}


	/**
	 * Borrowed from line test fitness
	 * @param individual
	 * @param result
	 * @return
	 */
	public static double getLineFitness(TestChromosome individual, ExecutionResult result , int line) {
		List<BranchCoverageTestFitness> branchFitnesses = new ArrayList<>();
		branchFitnesses = setupDependencies(className, methodName, lineNumber);
		double fitness = 1.0;
		int lineNumber = line;
		if (result.getTrace().getCoveredLines().contains(lineNumber)) {
			fitness = 0.0;
		} else {
			double r = Double.MAX_VALUE;
			// Find minimum distance to satisfying any of the control dependencies
			for (BranchCoverageTestFitness branchFitness : branchFitnesses) {
				// let's calculate the branch distance
				ControlFlowDistance distance = branchFitness.getBranchGoal().getDistance(result);
				double newFitness = distance.getResultingBranchFitness();

				if (newFitness == 0.0) {
					// If the control dependency was covered, then likely
					// an exception happened before the line was reached
					newFitness = 1.0;
				} else {
					newFitness = normalize(newFitness);
				}
				if (newFitness < r)
					r = newFitness;
			}

			fitness = r;
		}
		return fitness;
	}


	public static List<BranchCoverageTestFitness> setupDependencies(String classNamn , String methodNamn, int lineNummer ) {
		goalInstruction = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getFirstInstructionAtLineNumber(classNamn, methodNamn, lineNummer);
		List<BranchCoverageTestFitness> branchFitnesses = new ArrayList<>();
		if(goalInstruction == null){
			return branchFitnesses;
		}	

		Set<ControlDependency> cds = goalInstruction.getControlDependencies();

		for (ControlDependency cd : cds) {
			BranchCoverageTestFitness fitness = BranchCoverageFactory.createBranchCoverageTestFitness(cd);

			branchFitnesses.add(fitness);
		}

		if (goalInstruction.isRootBranchDependent())
			branchFitnesses.add(BranchCoverageFactory.createRootBranchTestFitness(goalInstruction));

		if (cds.isEmpty() && !goalInstruction.isRootBranchDependent())
			throw new IllegalStateException(
					"expect control dependencies to be empty only for root dependent instructions: "
					);

		if (branchFitnesses.isEmpty())
			throw new IllegalStateException(
					"an instruction is at least on the root branch of it's method");

		branchFitnesses.sort((a,b) -> a.compareTo(b));

		return branchFitnesses;
	}


	private String derivingMethodFromBytecode(String className, String methodName, int lineNumber){
		List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(className);
		if (instructions != null) {
			for (BytecodeInstruction ins : instructions) {
				if(ins != null) {
					if (ins.getLineNumber() == lineNumber){
						String bytecodeMethodName = ins.getMethodName();
						//						if (bytecodeMethodName.contains(methodName))
						return bytecodeMethodName;
					}
				} else {
					LoggingUtils.getEvoLogger().error("CrashCoverageTestfitness.derivingMethodFromBytecode: instruction for this line number " + lineNumber+" was null!");
				}
			}
		} else {
			LoggingUtils.getEvoLogger().error("CrashCoverageTestfitness.derivingMethodFromBytecode: instruction for this class " + className +" was null!");
		}
		return null;
	}


	//*******************************************************************************
	//public calls retriever:
	/**
	 * 
	 * @return a set of public/protected calls -- either the target call or its parents.
	 * 
	 */
	private static Set<String> getPublicCalls() {
		int stacklength = targetStackTrace.length;
		String targetClass = targetStackTrace[stacklength-1].getClassName();
		String targetMethod = targetStackTrace[stacklength-1].getMethodName();
		int targetLine = targetStackTrace[stacklength-1].getLineNumber();
		List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);
		BytecodeInstruction targetInstruction = getTargetInstruction(instructions, targetLine);
		
		if (targetInstruction.getActualCFG().isPublicMethod() ||
				isProtectedMethod(targetInstruction.getActualCFG())){
			LoggingUtils.getEvoLogger().info("* EvoCrash: The target call is either public or protected!");
			// If the public call is to a constructor, just add the class name
			// because this is how it will be retrieved in RootMethodTestChromosomeFactory!
			if(targetInstruction.getActualCFG().getName().contains("<init>")){
				publicCalls.add(targetClass);
			} else {
				publicCalls.add(cleanMethodName(targetInstruction.getMethodName()));
			}

		} // So the target call is private
		else {
			LoggingUtils.getEvoLogger().info("* EvoCrash: The target call '{}' is private!", targetInstruction.getMethodName());
			LoggingUtils.getEvoLogger().info("* EvoCrash: Looking for public callers", targetInstruction.getMethodName());
			searchForNonPrivateMethods(targetInstruction, targetClass);
		}


		LoggingUtils.getEvoLogger().info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> The retrived call(s) to inject in the tests are:");
		Iterator<String> iterateParents = CrashCoverageTestFitness.publicCalls.iterator();

		// Fill up the set of parent calls by assessing the method names
		while (iterateParents.hasNext()) {
			String nextCall = iterateParents.next();
			LoggingUtils.getEvoLogger().info(">>>>>> " + nextCall);
		}

		return publicCalls;
	}

	/**
	 * It retrieves all public methods that directly or indirectly call the target instruction to cover
	 * @param targetInstruction target instruction to cover (line coverage)
	 * @param targetClass name of the CUT
	 */
	private static void searchForNonPrivateMethods(BytecodeInstruction targetInstruction, String targetClass){
		List<BytecodeInstruction> instructions = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getInstructionsIn(targetClass);

		LinkedList<String> methods =  new LinkedList<String>(); // linked list to store all methods to analyze
		Set<String> visitedMethods = new HashSet<String>();    // set to keep track of already visited methods
		HashMap<BytecodeInstruction, ArrayList<String>> methodsInCUT = new HashMap<>(); // all of the methods in Class Under Test!
		targetInstruction.getActualCFG().getMethodName();     // let's take the name of the method containing the instruction to cover
		methods.add(targetInstruction.getActualCFG().getMethodName()); // this is the first non-public method to visit
		// let's prepare the hashMap containing methods and the methods which is called by them!
		for (BytecodeInstruction instruct : instructions) {
			if(!methodsInCUT.containsKey(instruct)) {
				ArrayList<String> calledMethods =  new ArrayList<>();
				for (BytecodeInstruction method_call : instruct.getRawCFG().determineMethodCalls()){
				calledMethods.add(method_call.getCalledMethod());
				}
				methodsInCUT.put(instruct, calledMethods);
			}
		}
		// until there are non-public methods to visit
		while (methods.size()>0){
			String target_method = methods.removeFirst(); // get the name of one of the private methods to analyze
			if (visitedMethods.contains(target_method)) // if it has been already visited, we skip it to avoid infinite loop
				continue;
			else
				visitedMethods.add(target_method);
			
			for( BytecodeInstruction key : methodsInCUT.keySet()) {
				ArrayList<String> list = methodsInCUT.get(key);
				for (String invokedMethod : list) {
					if (invokedMethod.equals(target_method)) {
						// we know that key is parent.
						// now, we want to know if key is private or not!
						if(key.getActualCFG().isPublicMethod() || isProtectedMethod(key.getActualCFG())){
							// this parent is public or protected.
							if(key.getMethodName().contains("<init>")){
								LoggingUtils.getEvoLogger().info("* EvoCrash: The target call is made to a protected constructor!");
							    	publicCalls.add(key.getMethodName());
							} else {
							    	LoggingUtils.getEvoLogger().info("* EvoCrash: The target call is made to a protected method!");
							    	publicCalls.add(cleanMethodName(key.getMethodName()));
							    	}
						}else {
							//this parent is private
							methods.addLast(key.getMethodName());
							
						}
					}
				}
			}
			
		} // while
		
		LoggingUtils.getEvoLogger().info("CrashCoverageTestFitness: public calls size after search: " + publicCalls.size());
	}


	private static BytecodeInstruction getTargetInstruction (List<BytecodeInstruction> instructions , int targetLine) {
		BytecodeInstruction targetInstruction = null;
		// Looking for the instructions of the target line...
		for (BytecodeInstruction ins : instructions) {
			// and if the instruction for the target line number
			if (ins.getLineNumber() == targetLine){
				targetInstruction = ins;
			}
		}

		return targetInstruction;
	}


	private static String cleanMethodName(String method){
		String newMethodName = method.substring(0, method.indexOf('('));
		return newMethodName;
	}
	
	private static boolean isProtectedMethod(ActualControlFlowGraph acfg){
		return (acfg.getMethodAccess() & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED;
	}


}
