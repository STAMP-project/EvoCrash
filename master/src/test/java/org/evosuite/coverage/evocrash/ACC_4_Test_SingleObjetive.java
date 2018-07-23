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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import org.evosuite.EvoSuite;
//import org.crash.client.log.LogParser;
//import org.crash.master.EvoSuite;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.LoggingUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


@NotThreadSafe
@Ignore
public class ACC_4_Test_SingleObjetive {
	
	@Test
	public void testACC_4_frameLevel_1(){
		
		String user_dir = System.getProperty("user.dir");
		
		Path binpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash" );
		String bin_path = binpath.toString();
		
		Path logpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash", "ACC-4" , "ACC-4.log");
		String logPath = logpath.toString();
		
		Path dep_1 = Paths.get(bin_path, "ACC-2.0", "commons-collections-2.0.jar");
		String dependency_1 = dep_1.toString();
		
		String targetClass = LogParser.getTargetClass(logPath, 3);
		
		Path testpath = Paths.get(user_dir, "GGA-tests");
		String test_path = testpath.toString();
		
		String[] command = {
				"-generateTests",
				"-Dcriterion=CRASH",
				"-Dsandbox=TRUE",
				"-Dtest_dir="+ test_path,
				"-Dminimize=TRUE",
				"-Dalgorithm=PICKYMONOTONICGA",
				"-Dstrategy=ONEBRANCH",
				"-Dtest_factory=ROOT_PUBLIC_METHOD",
				"-Doutput_variables=TARGET_CLASS,algorithm,criterion,Total_Goals,Covered_Goals,Generations,Total_Time,Size,Result_Size,Length,Result_Length",
				"-Dreport_dir=spreadsheet",
				"-Dheadless_chicken_test=FALSE",
				"-Dpopulation=80",
				"-Dstopping_condition=MAXFITNESSEVALUATIONS",
				"-Dsearch_budget=62328",
				"-Dtarget_frame=3",
				"-Drandom_tests=0",
				"-Dvirtual_fs=TRUE",
				"-Dreplace_calls=FALSE",
				"-Dlog_goals=TRUE",
				"-Dreset_static_fields=FALSE",
				"-DEXP="+ logPath,
				"-Dvirtual_fs=TRUE",
                "-Duse_separate_classloader=FALSE",
                "-Dvirtual_net=FALSE",
                "-Dreplace_calls=FALSE",
				"-projectCP",
				dependency_1,
				"-class", 
				targetClass
				};


		
		EvoSuite evosuite = new EvoSuite();
		Object result = evosuite.parseCommandLine(command);
		List<List<TestGenerationResult>> results = (List<List<TestGenerationResult>>)result;
		GeneticAlgorithm<?> ga = getGAFromResult(results);
		if (ga == null){
			// ga is null when during bootstrapping the ideal test is found!
			Assert.assertTrue(true);
		}
		else{
			TestChromosome best = (TestChromosome) ga.getBestIndividual();
			Assert.assertEquals(0.0, best.getFitness(), 0);
		}
	
	}


	@SuppressWarnings("unchecked")
	protected GeneticAlgorithm<?> getGAFromResult(Object result) {
		assert(result instanceof List);
		List<List<TestGenerationResult>> results = (List<List<TestGenerationResult>>)result;
		assert(results.size() == 1);
		return results.get(0).get(0).getGeneticAlgorithm();
	}
	
}
