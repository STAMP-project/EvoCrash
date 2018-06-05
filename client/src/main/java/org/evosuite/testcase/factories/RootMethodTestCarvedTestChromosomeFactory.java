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

package org.evosuite.testcase.factories;

import org.evosuite.Properties;
import org.evosuite.coverage.evocrash.CrashCoverageTestFitness;
import org.evosuite.coverage.evocrash.CrashProperties;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.rmi.ClientServices;
import org.evosuite.rmi.service.ClientNodeLocal;
import org.evosuite.runtime.System;
import org.evosuite.setup.TestCluster;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testcarver.extraction.CarvingManager;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.evosuite.utils.generic.GenericConstructor;
import org.evosuite.utils.generic.GenericMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

//import org.crash.client.CrashProperties;
//import org.crash.client.crashcoverage.CrashCoverageTestFitness;


public class RootMethodTestCarvedTestChromosomeFactory extends AllMethodsTestChromosomeFactory{

	/** Constant <code>logger</code> */
	protected static final Logger logger = LoggerFactory.getLogger(RootMethodTestCarvedTestChromosomeFactory.class);

	private static final long serialVersionUID = 1153654174584764835L;

	// List of useful carved test cases
	private final List<TestCase> junitTests = new ArrayList<TestCase>();

	/** Public and protected methods of the target class */
	private static List<GenericAccessibleObject<?>> allMethods = new LinkedList<GenericAccessibleObject<?>>();

	private static int totalNumberOfTestsCarved = 0;
	private static double carvedCoverage = 0.0;

	/** Public parent methods of the target call */
	private static Set<GenericAccessibleObject<?>> publicParentCalls = new HashSet<GenericAccessibleObject<?>>();
	private static Set<GenericAccessibleObject<?>> attemptedPublicParents = new HashSet<GenericAccessibleObject<?>>();

	/**
	 * Create a list of all methods
	 */
	public RootMethodTestCarvedTestChromosomeFactory() {
		//super();
		allMethods.clear();
		allMethods.addAll(TestCluster.getInstance().getTestCalls());
		Randomness.shuffle(allMethods);
		reset();
	}

	/**
	 * Create a random individual. It will contain at least one call to "Method Under Test"
	 * (i.e., the method involved in the target crash)
	 * @param size
	 */
	private TestCase getRandomTestCase(int size) {
		boolean tracerEnabled = ExecutionTracer.isEnabled();
		if (tracerEnabled)
			ExecutionTracer.disable();

		CrashProperties.getInstance();
		// Counts the number of injected target calls in the created test.
		int target_counter = 0;
		int max_rounds = 0;
		TestCase test = null;
		// Loop until the created method has at least one target call or it reaches the maximum number of rounds.
		while (target_counter < 1 && (max_rounds < CrashProperties.MAX_INIT_ROUNDS)) {
			max_rounds++;
			test = getNewTestCase();
			// Choose a random length in 0 - size
			double length = Randomness.nextInt(size);
			while (length < 2)
				length = Randomness.nextInt(size);

				double prob = 1/length;
				boolean isIncluded = false;
			while (test.size() < length) {

				// If all public parents have been attempted,
				// reset the set of parents to start over injecting them.
				if (publicParentCalls.size() == 0) {
					reset();
				}

				GenericAccessibleObject<?> call;
				boolean injecting = false;
				if (Randomness.nextDouble() <= prob) {
					call = Randomness.choice(publicParentCalls);
					publicParentCalls.remove(call);
					attemptedPublicParents.add(call);
					injecting = true;
				}else {
					call = Randomness.choice(allMethods);
				}

				try {
					TestFactory testFactory = TestFactory.getInstance();
					if (call.isMethod()) {
						testFactory.addMethod(test, (GenericMethod) call, test.size(), 0);
					} else if (call.isConstructor()) {
						testFactory.addConstructor(test, (GenericConstructor) call,
								test.size(), 0);
					}

					//at this point, if injecting, then we successfully injected a target call.
					if (injecting){
						isIncluded = true;
						target_counter++;
						prob = 1/length;
					}
//					else {
//						assert (false) : "Found test call that is neither method nor constructor";
//					}
				} catch (ConstructionFailedException e) {
					if (injecting)
						prob = 1/(length-test.size()+1);
				}
			}

		} // a test case is created which has at least 1 target call.

		if (target_counter < 1 && max_rounds >= CrashProperties.MAX_INIT_ROUNDS){
			LoggingUtils.getEvoLogger().error("Guided initialization failed. Please revise the target class and method!");
			System.exit(0);
		}

		if (logger.isDebugEnabled())
			logger.debug("Randomized test case:" + test.toCode());

		if (tracerEnabled)
			ExecutionTracer.enable();

		return test;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Generate a random chromosome
	 */
	@Override
	public TestChromosome getChromosome() {
        LoggingUtils.getEvoLogger().info("NEW");
		CarvingManager manager = CarvingManager.getInstance();
		final Class<?> targetClass = Properties.getTargetClassAndDontInitialise();
		List<TestCase> tests = manager.getTestsForClass(targetClass);
		junitTests.addAll(tests);

		if (junitTests.size() > 0) {
			totalNumberOfTestsCarved = junitTests.size();
			LoggingUtils.getEvoLogger().info("* Using {} carved tests from existing JUnit tests for seeding", junitTests.size());

			// Printing out all of the carved test cases code
			for (TestCase test : junitTests) {
				LoggingUtils.getEvoLogger().info("Carved Test: {}", test.toCode());
			}
		}

		ClientNodeLocal client = ClientServices.getInstance().getClientNode();
		client.trackOutputVariable(RuntimeVariable.CarvedTests, totalNumberOfTestsCarved);
		client.trackOutputVariable(RuntimeVariable.CarvedCoverage,carvedCoverage);
		final int N_mutations = Properties.SEED_MUTATIONS;
		final double P_clone = Properties.SEED_CLONE;
		double r = Randomness.nextDouble();

		if (r >= P_clone || junitTests.isEmpty()) {
			TestChromosome c = new TestChromosome();
			c.setTestCase(getRandomTestCase(Properties.CHROMOSOME_LENGTH));
			return c;
		}


		logger.info("Cloning user test");
		TestCase test = Randomness.choice(junitTests);
		TestChromosome chromosome = new TestChromosome();
		chromosome.setTestCase(test.clone());
		if (N_mutations > 0) {
			int numMutations = Randomness.nextInt(N_mutations);
			logger.debug("Mutations: " + numMutations);
			// doing the mutations on the cloned test case
			for (int i = 0; i < numMutations; i++) {
				chromosome.mutate();
			}
		}

		return chromosome;

	}

	/**
	 * Provided so that subtypes of this factory type can modify the returned
	 * TestCase
	 *
	 * @return a {@link TestCase} object.
	 */
	protected TestCase getNewTestCase() {
		return new DefaultTestCase();
	}


	/**
	 * Resets the sets of public parent calls and the attempted ones.
	 */
	public void reset(){
		fillPublicCalls();
		attemptedPublicParents.clear();

	}

	/**
	 * Fills the public parent calls.
	 * Used when resetting the sets of calls. 
	 */
	private void fillPublicCalls(){
		Iterator <String> iterateParents = CrashCoverageTestFitness.publicCalls.iterator();
		
		// Fill up the set of parent calls by assessing the method names
		while (iterateParents.hasNext()) {
			String nextCall = iterateParents.next();
			for (int i=0; i<allMethods.size(); i++) {
				if (allMethods.get(i).getName().equals(nextCall)) {
					publicParentCalls.add(allMethods.get(i));
				}
			}
		}
	}
	
	
}