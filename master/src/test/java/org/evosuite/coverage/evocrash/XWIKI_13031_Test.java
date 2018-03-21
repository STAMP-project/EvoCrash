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

import org.evosuite.EvoSuite;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestChromosome;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

//import org.crash.client.log.LogParser;
//import org.crash.master.EvoSuite;


@NotThreadSafe
public class XWIKI_13031_Test {

	@Test
	public void XWIKI_13031_frameLevel_3(){
		int frameLevel =3;
		String ExceptionType = "java.lang.ClassCastException";
		String user_dir = System.getProperty("user.dir");
		Path binpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash" );
		String bin_path = binpath.toString();

		Path logpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash", "XWIKI-13031" , "XWIKI-13031.log");
		String logPath = logpath.toString();
		String dependencies = "";
		File depFolder = new File(bin_path,"XWIKI-7.4");
		File[] listOfFilesInSourceFolder = depFolder.listFiles();
		for(int i = 0; i < listOfFilesInSourceFolder.length; i++){
			if(listOfFilesInSourceFolder[i].getName().charAt(0) !='.') {
				Path depPath = Paths.get(depFolder.getAbsolutePath(), listOfFilesInSourceFolder[i].getName());
				String dependency = depPath.toString();

				dependencies += (dependency+":");
			}
		}
		dependencies = dependencies.substring(0, dependencies.length() - 1);

		String targetClass = LogParser.getTargetClass(logPath, 1);

		Path testpath = Paths.get(user_dir, "GGA-tests");
		String test_path = testpath.toString();

		String[] command = {
				"-generateTests",
				"-Dcriterion=CRASH",
				"-Dsandbox=FALSE",
				"-Dtest_dir="+ test_path,
				"-Drandom_tests=0",
				"-Dminimize=TRUE",
				"-Dheadless_chicken_test=FALSE",
				"-Dpopulation=80",
				"-Dp_functional_mocking=0.8",
				"-Dfunctional_mocking_percent=0.5",
				"-Dsearch_budget=1800",
				"-Dtarget_frame="+frameLevel,
				"-Dvirtual_fs=FALSE",
				"-Dreplace_calls=FALSE",
				"-Dreset_static_fields=FALSE",
				"-Dno_runtime_dependency=TRUE",
				"-Dvirtual_net=FALSE",
				"-Dtarget_exception_crash="+ExceptionType,
				"-DEXP="+ logPath.toString(),
				"-projectCP",
				dependencies,
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
