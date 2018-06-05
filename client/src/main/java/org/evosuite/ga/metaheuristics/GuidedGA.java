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

package org.evosuite.ga.metaheuristics;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.evosuite.coverage.evocrash.CrashCoverageInfos;
import org.evosuite.seeding.CallSequencesPoolManager;
import org.evosuite.testcarver.extraction.CarvingManager;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.statements.ConstructorStatement;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.coverage.evocrash.CrashCoverageTestFitness;
import org.evosuite.Properties;
import org.evosuite.TimeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;


public class GuidedGA <T extends Chromosome> extends MonotonicGA <T> {

	public GuidedGA(ChromosomeFactory<T> factory) {
		super(factory);
	}

	private static final long serialVersionUID = -7127020173037680696L;
	
	private final Logger logger = LoggerFactory.getLogger(GuidedGA.class);
	

	
	@Override
	public void generateSolution() {

		if(Properties.ACCESED_CLASSES){
			CarvingManager manager = CarvingManager.getInstance();
			for(Class<?> targetClass : manager.getClassesWithTests()) {
				List<TestCase> tests = manager.getTestsForClass(targetClass);
				for(TestCase test : tests) {
					LoggingUtils.getEvoLogger().info("~~~~~~~~~~~~~~~~~~~~~~~~");
					LoggingUtils.getEvoLogger().info("The test is: ");
					LoggingUtils.getEvoLogger().info(test.toCode());
					Set<Class<?>> accessed = test.getAccessedClasses();
					LoggingUtils.getEvoLogger().info("Accessed classes:");
					for (Class<?> c: accessed){
						LoggingUtils.getEvoLogger().info(c.getName());
					}
				}
			}

			System.exit(0);
		}

		if (Properties.MODEL_SUT) {
			// carving tests and analyse the source code statically before starting search
			CallSequencesPoolManager callseqsManager = CallSequencesPoolManager.getInstance();
			callseqsManager.report();
			LoggingUtils.getEvoLogger().info("saving pool");
			callseqsManager.savePool();
			LoggingUtils.getEvoLogger().info("reading pool");
			callseqsManager.readPoolFromTheFile("CallSequencePoolJson.txt");
			LoggingUtils.getEvoLogger().info("reporting again");
			callseqsManager.report();
		}else{


		CrashCoverageInfos crashInfos = CrashCoverageInfos.getInstance();
		long startTime = System.currentTimeMillis();
		crashInfos.setStartTime(startTime);

		if (Properties.ENABLE_SECONDARY_OBJECTIVE_AFTER > 0 || Properties.ENABLE_SECONDARY_OBJECTIVE_STARVATION) {
			disableFirstSecondaryCriterion();
		}
		if (population.isEmpty()) {
			initializePopulation();
			assert !population.isEmpty() : "Could not create any test";
		}


		logger.debug("Starting evolution");
		int starvationCounter = 0;
		double bestFitness = Double.MAX_VALUE;
		double lastBestFitness = Double.MAX_VALUE;
		if (getFitnessFunction().isMaximizationFunction()) {
			bestFitness = 0.0;
			lastBestFitness = 0.0;
		}
		double bestFFinInitialization = getBestFitness();
		crashInfos.setPreviousFitnessFunction(bestFFinInitialization);
		LoggingUtils.getEvoLogger().info("@@ff:(" + bestFFinInitialization + ";" + crashInfos.getnumberOfTries() + ";" + crashInfos.getFFDuration() + ")");
		while (!isFinished()) {
			logger.info("Population size before: " + population.size());
			// related to Properties.ENABLE_SECONDARY_OBJECTIVE_AFTER;
			// check the budget progress and activate a secondary criterion
			// according to the property value.

			{
				double bestFitnessBeforeEvolution = getBestFitness();

				evolve();
				sortPopulation();
				double bestFitnessAfterEvolution = getBestFitness();
				//LoggingUtils.getEvoLogger().info("Fitness>>> "+bestFitnessAfterEvolution);

				if (getFitnessFunction().isMaximizationFunction())
					assert (bestFitnessAfterEvolution >= (bestFitnessBeforeEvolution
							- DELTA)) : "best fitness before evolve()/sortPopulation() was: " + bestFitnessBeforeEvolution
							+ ", now best fitness is " + bestFitnessAfterEvolution;
				else
					assert (bestFitnessAfterEvolution <= (bestFitnessBeforeEvolution
							+ DELTA)) : "best fitness before evolve()/sortPopulation() was: " + bestFitnessBeforeEvolution
							+ ", now best fitness is " + bestFitnessAfterEvolution;
			}

			{
				double bestFitnessBeforeLocalSearch = getBestFitness();
				applyLocalSearch();
				double bestFitnessAfterLocalSearch = getBestFitness();

				if (getFitnessFunction().isMaximizationFunction())
					assert (bestFitnessAfterLocalSearch >= (bestFitnessBeforeLocalSearch
							- DELTA)) : "best fitness before applyLocalSearch() was: " + bestFitnessBeforeLocalSearch
							+ ", now best fitness is " + bestFitnessAfterLocalSearch;
				else
					assert (bestFitnessAfterLocalSearch <= (bestFitnessBeforeLocalSearch
							+ DELTA)) : "best fitness before applyLocalSearch() was: " + bestFitnessBeforeLocalSearch
							+ ", now best fitness is " + bestFitnessAfterLocalSearch;
			}

			/*
			 * TODO: before explanation: due to static state handling, LS can
			 * worse individuals. so, need to re-sort.
			 * 
			 * now: the system tests that were failing have no static state...
			 * so re-sorting does just hide the problem away, and reduce
			 * performance (likely significantly). it is definitively a bug
			 * somewhere...
			 */
			// sortPopulation();

			double newFitness = getBestFitness();
			if (crashInfos.getPreviousFitnessFunction() != newFitness) {
				crashInfos.setPreviousFitnessFunction(newFitness);
				LoggingUtils.getEvoLogger().info("@@ff:(" + newFitness + ";" + crashInfos.getnumberOfTries() + ";" + crashInfos.getFFDuration() + ")");
			}
			if (getFitnessFunction().isMaximizationFunction())
				assert (newFitness >= (bestFitness - DELTA)) : "best fitness was: " + bestFitness
						+ ", now best fitness is " + newFitness;
			else
				assert (newFitness <= (bestFitness + DELTA)) : "best fitness was: " + bestFitness
						+ ", now best fitness is " + newFitness;
			bestFitness = newFitness;

			if (Double.compare(bestFitness, lastBestFitness) == 0) {
				starvationCounter++;
			} else {
				logger.info("reset starvationCounter after " + starvationCounter + " iterations");
				starvationCounter = 0;
				lastBestFitness = bestFitness;

			}

			updateSecondaryCriterion(starvationCounter);

			logger.info("Current iteration: " + currentIteration);
			this.notifyIteration();

			logger.info("Population size: " + population.size());
			logger.info("Best individual has fitness: " + population.get(0).getFitness());
			logger.info("Worst individual has fitness: " + population.get(population.size() - 1).getFitness());
//			LoggingUtils.getEvoLogger().error("Best individual has fitness: " + population.get(0).getFitness());
//			LoggingUtils.getEvoLogger().error("Worst individual has fitness: " + population.get(population.size() - 1).getFitness() + "\n\n");
		}

		long endTime = System.currentTimeMillis();
		long totalTime = (endTime - startTime) / 1000;
///		if(population.get(0).getFitness() == 0.0) {
		T best = population.get(0);
		ExecutionResult results = ((TestChromosome) best).getLastExecutionResult();
		int index = results.getFirstPositionOfThrownException();
		Throwable thrownException = results.getExceptionThrownAtPosition(index);
		LoggingUtils.getEvoLogger().info("* EvoCrash: the generated stack trace: \n");
		for (int i = 0; i < thrownException.getStackTrace().length; i++) {
			LoggingUtils.getEvoLogger().info("" + thrownException.getStackTrace()[i]);
		}
		TestChromosome tc = (TestChromosome) best;
		LoggingUtils.getEvoLogger().error("\n* EvoCrash: the generated test case: \n" + tc.getTestCase().toCode());
//	    }

		LoggingUtils.getEvoLogger().info(">>>>>>>>>>>>>>>>>>>>>>>>>>>GGA was done in " + totalTime + "!<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		// archive
		TimeController.execute(this::updateBestIndividualFromArchive, "update from archive", 5_000);
		long FFTimePassed = crashInfos.getFFTime() - crashInfos.getStartTime();
		LoggingUtils.getEvoLogger().info("* Fitness Function/Time passed/Number of tries: " + crashInfos.getFitnessFunctionMin() + "/" + FFTimePassed + "/" + crashInfos.getFFTries());
		long LCTimePassed = crashInfos.getLCTime() - crashInfos.getStartTime();
		LoggingUtils.getEvoLogger().info("* LineCoverage Fitness/Time passed/Number of tries: " + crashInfos.getLineCoverageMin() + "/" + LCTimePassed + "/" + crashInfos.getLCTries());
		long EHTimePassed = crashInfos.getEHTime() - crashInfos.getStartTime();
		LoggingUtils.getEvoLogger().info("* Exception Happened/Time passed/Number of tries: " + crashInfos.getExceptionHappenedMin() + "/" + EHTimePassed + "/" + crashInfos.getEHTries());
		long SSTimePassed = crashInfos.getSSTime() - crashInfos.getStartTime();
		LoggingUtils.getEvoLogger().info("* Stack Trace Similarity/Time passed/Number of tries: " + crashInfos.getStackTraceSimilarityMin() + "/" + SSTimePassed + "/" + crashInfos.getSSTries());
		LoggingUtils.getEvoLogger().info("* number of total tries: " + crashInfos.getnumberOfTries());
		notifySearchFinished();
	}
	}
	
	
	
	
	
	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	protected void evolve() {
		List<T> newGeneration = new ArrayList<T>();
//		LoggingUtils.getEvoLogger().error("* EvoCrash: Evolving individuals...");
		// Elitism
		logger.debug("Elitism");
		newGeneration.addAll(elitism());
		while (!isNextPopulationFull(newGeneration) && !isFinished()) {
			logger.debug("Generating offspring");
			T parent1 = selectionFunction.select(population);
			T parent2;
			if (Properties.HEADLESS_CHICKEN_TEST)
				parent2 = newRandomIndividual(); // crossover with new random
													// individual
			else
				parent2 = selectionFunction.select(population); // crossover
																// with existing
			T offspring1 = (T) parent1.clone();
			T offspring2 = (T) parent2.clone();
			// Crossover
			if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
				try{
					crossoverFunction.crossOver(offspring1, offspring2);
				}catch (ConstructionFailedException e){
//					LoggingUtils.getEvoLogger().error("construction failed when doing crossover!");
					continue;
				}catch (Exception e) {
//					LoggingUtils.getEvoLogger().error("something went wrong in crossover in EvoSuite. we move on!");
				}
				
			}
			// Check the inclusion of a target call.
			if (!includesPublicCall(offspring1)) {
				offspring1 = (T) parent1.clone();
			} else if(!includesPublicCall(offspring2)) {
				offspring2 = (T) parent2.clone();
			}
			try {
				mutateOffspring(offspring1);
				mutateOffspring(offspring2);
			}catch (Exception e) {
				LoggingUtils.getEvoLogger().error("something went wrong in mutation in EvoSuite. We just move on! :D");
			}
			// The two offspring replace the parents if and only if one of
			// the offspring is not worse than the best parent.
			for (FitnessFunction<T> fitnessFunction : fitnessFunctions) {
				fitnessFunction.getFitness(offspring1);
				notifyEvaluation(offspring1);
				fitnessFunction.getFitness(offspring2);
				notifyEvaluation(offspring2);
			}
			

			if (keepOffspring(parent1, parent2, offspring1, offspring2)) {
				logger.debug("Keeping offspring");

				// Reject offspring straight away if it's too long
				int rejected = 0;
				if (isTooLong(offspring1) || offspring1.size() == 0) {
					rejected++;
				} else {
					// if(Properties.ADAPTIVE_LOCAL_SEARCH ==
					// AdaptiveLocalSearchTarget.ALL)
					// applyAdaptiveLocalSearch(offspring1);
					newGeneration.add(offspring1);
				}

				if (isTooLong(offspring2) || offspring2.size() == 0) {
					rejected++;
				} else {
					// if(Properties.ADAPTIVE_LOCAL_SEARCH ==
					// AdaptiveLocalSearchTarget.ALL)
					// applyAdaptiveLocalSearch(offspring2);
					newGeneration.add(offspring2);
				}

				if (rejected == 1)
					newGeneration.add(Randomness.choice(parent1, parent2));
				else if (rejected == 2) {
					newGeneration.add(parent1);
					newGeneration.add(parent2);
				}
			} else {
				logger.debug("Keeping parents");
				newGeneration.add(parent1);
				newGeneration.add(parent2);
			}
			

		}
		population = newGeneration;
		// archive
		updateFitnessFunctionsAndValues();

		currentIteration++;
	}
	
	
	private void mutateOffspring (T offspring) {
		boolean permission = false;
		while (!permission) {
			
			// Mutation
			notifyMutation(offspring);
			try{
				offspring.mutate();
			}catch(Exception e){
				LoggingUtils.getEvoLogger().error("Mutation failed!");
			}
			if (offspring.isChanged()) {
				offspring.updateAge(currentIteration);
			}
			try {
				permission = includesPublicCall(offspring);
			}catch(Exception e){
				LoggingUtils.getEvoLogger().error("* EvoCrash: Something went wrong when checking the target call after mutation! \n ");
			}

		}

	}

	
	/**
	 * Checks whether the target method statement is included in the individual test case.
	 * This is used in population initialization, crossover, and mutation.
	 * 
	 * @param individual
	 * @return
	 */
	private boolean includesPublicCall (T individual) {
		boolean isIdeal = false;
		
		Iterator <String> publicCallsIterator = CrashCoverageTestFitness.publicCalls.iterator();
		TestChromosome candidateChrom = (TestChromosome) individual;
		TestCase candidateTC = candidateChrom.getTestCase();
		if (candidateTC.size() == 0){
			isIdeal = true;
			return isIdeal;
		}
		while (!isIdeal && publicCallsIterator.hasNext()){
			String name = publicCallsIterator.next();
			for ( int ind= 0 ; ind < candidateTC.size() ;ind++) {
				Statement currentStatement = candidateTC.getStatement(ind);
				if (currentStatement instanceof MethodStatement) {
					MethodStatement candidateMethod = (MethodStatement) candidateTC.getStatement(ind);
					if (candidateMethod.getMethodName().equalsIgnoreCase(name)) {
						isIdeal = true;
						break;
					}
				} else if (currentStatement instanceof ConstructorStatement){
					// If the target call is to a constructor, we pass the class name as the public call in CrashCoverageTestFitness.
					// Thus, we only check if the name has "." to see if the public call is to a constructor, and if so,
					// The constructor statement should be ideal most likely! TODO: verify this!
					ConstructorStatement candidateConstructor = (ConstructorStatement) candidateTC.getStatement(ind);
					if(name.contains(".")){
						isIdeal = true;
						break;
					}
				}else {
//					LoggingUtils.getEvoLogger().debug("The method is neither a constructor nor method call!");
				}
				
			}
		}
		return isIdeal;
	}
	
	
	private T newRandomIndividual() {
		//FIXME: this is used for headless-chicken-test. Is a check on being ideal required?
		T randomChromosome = chromosomeFactory.getChromosome();
		for (FitnessFunction<?> fitnessFunction : this.fitnessFunctions) {
			randomChromosome.addFitness(fitnessFunction);
		}
		return randomChromosome;
	}

	private static final double DELTA = 0.000000001;    // it seems there is some
														// rounding error in LS,
														// but hard to debug :(
	private double getBestFitness() {
		T bestIndividual = getBestIndividual();
		for (FitnessFunction<T> ff : fitnessFunctions) {
			ff.getFitness(bestIndividual);
		}
		return bestIndividual.getFitness();
	}	
	
	
	@Override
	public void initializePopulation() {
		notifySearchStarted();
		currentIteration = 0;

		// Set up initial population
		generateInitialPopulation(Properties.POPULATION);
		logger.debug("Calculating fitness of initial population");
		calculateFitnessAndSortPopulation();

		this.notifyIteration();
	}

	/**
	 *  Randomly, generates an initial (hopefully ideal) population!
	 *  
	 */
	protected void generateInitialPopulation(int population_size) {
		// This is not needed anymore since the method call(s) are injected in the chromosomes already.
		//generateIdealPopulation(population_size - population.size());
		
		// First generation is populated with chromosomes in which the target call is injected.
		generatePopulation(population_size - population.size());
		
	}
	
	/**
	 * Generates a population of individuals.
	 * 
	 * Please note! since the target call(s) are injected, the check for ideal(s) is lifted here.
	 * 
	 * @param population_size
	 *       		 a int.
	 */
	protected void generatePopulation(int population_size) {
		logger.debug("Creating random population");
		for (int i = 0; i < population_size; i++) {
			T individual;
				individual = chromosomeFactory.getChromosome();
			for (FitnessFunction<?> fitnessFunction : this.fitnessFunctions) {
				individual.addFitness(fitnessFunction);
			}

			population.add(individual);
			
			if (isFinished())
				break;
		}
		
		logger.debug("Created " + population.size() + " individuals");
	}
	
	
}