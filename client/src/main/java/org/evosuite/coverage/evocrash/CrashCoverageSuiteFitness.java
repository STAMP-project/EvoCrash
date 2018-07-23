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

import java.util.List;
import java.util.Map;

//import org.crash.client.CrashProperties;
//import org.crash.client.log.LogParser;
//import org.evosuite.Properties;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.evosuite.utils.LoggingUtils;

public class CrashCoverageSuiteFitness extends TestSuiteFitnessFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2859854608051164110L;

//	Throwable tarException = Properties.TARGET_EXCEPTION;
	Throwable tarException = CrashProperties.TARGET_EXCEPTION;
	public static double totalFitnessValue;
	
	@Override
	public double getFitness(
	        AbstractTestSuiteChromosome<? extends ExecutableChromosome> suite) {
		
		double goalcoverage = 0.0;
//		if(Properties.TEST_ARCHIVE) {
//			// If we are using the archive, then fitness is by definition 0
//			// as all assertions already covered are in the archive
//			goalcoverage = CrashCoverageFactory.getGoals().size();
//			suite.setFitness(this,  0.0);
//			suite.setCoverage(this, goalcoverage);
//			return 0.0;
//		}
		
		List<ExecutionResult> results = runTestSuite(suite);
		
		
		if (results == null) {
			throw new IllegalArgumentException();
		} else {
			
			totalFitnessValue = calculateFitness(suite, results);
			goalcoverage = calculateGoalCoverage(suite);
			System.err.println("Total fitness and goal coverage: " + totalFitnessValue + " :--: " + goalcoverage);
			System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
			suite.setFitness(this, totalFitnessValue);
			suite.setCoverage(this, goalcoverage);
	
		}
		
		updateIndividual(this, suite, totalFitnessValue);

		return totalFitnessValue;
	}
	
	/**
	 * Calculates the overall fitness of the suite.
	 * @param suite
	 * @return
	 */
	private static double calculateFitness (AbstractTestSuiteChromosome<? extends ExecutableChromosome>  suite, List<ExecutionResult> result ){
		
		double fitnessValue = 0.0;
		for (int i=0 ; i<suite.size(); i++) {
			fitnessValue = fitnessValue + suite.getTestChromosome(i).getFitness();
			LoggingUtils.getEvoLogger().info("CrashCoverageSuiteFitness: Suite Fitness is:  >>>> "+ fitnessValue);
		}
		return fitnessValue;
	}

	/**
	 * This method computes the percentage of crashes (goals) covered by the generated suite
	 * @param suite
	 * @return percentage of covered crashes
	 */
	private static double calculateGoalCoverage (AbstractTestSuiteChromosome<? extends ExecutableChromosome>  suite) {

		double covereg_goals = 0;
		
		Map<String, CrashCoverageTestFitness> crashGoals = CrashCoverageFactory.getGoals();
		double total_goals = CrashCoverageFactory.getGoals().keySet().size();
		
		for (String crash : crashGoals.keySet()){
			CrashCoverageTestFitness crashGoal = crashGoals.get(crash);
			
			double minFitnessValue = Double.MAX_VALUE;
			
			for (int i=0 ; i<suite.size(); i++) {
				minFitnessValue = Math.min(minFitnessValue , crashGoal.getFitness((TestChromosome) suite.getTestChromosome(i)));
			}
			
			if (minFitnessValue == 0)
				covereg_goals++;
		}
		
		return (covereg_goals/total_goals);
	}
}