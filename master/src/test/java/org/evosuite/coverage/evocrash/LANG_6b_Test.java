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

import com.google.gson.*;
import org.evosuite.EvoSuite;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestChromosome;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

//import org.crash.client.log.LogParser;
//import org.crash.master.EvoSuite;


@NotThreadSafe
public class LANG_6b_Test {

    @Test
    public void lang_6b_frameLevel_1(){
        boolean accessed = false;


        int targetFrame = 3;
        String user_dir = System.getProperty("user.dir");

        Path binpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash","Lang-bins","LANG-6b");
        String bin_path = binpath.toString();

        Path logpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash", "LANG-6b" , "LANG-6b.log");
        String logPath = logpath.toString();

        Path accessedClasses = Paths.get(user_dir,"accessed_classes.json");
        String accessedClassesPath = accessedClasses.toString();

        File depFolder = new File(bin_path);
        File[] listOfFilesInSourceFolder = depFolder.listFiles();
        String dependencies = "";
        List<String> testJars = new LinkedList<String>();
        for(int i = 0; i < listOfFilesInSourceFolder.length; i++) {
            if (listOfFilesInSourceFolder[i].getName().charAt(0) != '.') {
                Path depPath = Paths.get(depFolder.getAbsolutePath(), listOfFilesInSourceFolder[i].getName());
                String dependency = depPath.toString();
                if (listOfFilesInSourceFolder[i].getName().contains("-tests")){ testJars.add(dependency); }
                dependencies += (dependency + ":");
            }
        }

        String targetClass = LogParser.getTargetClass(logPath, targetFrame);

        Path testpath = Paths.get(user_dir, "GGA-tests");
        String test_path = testpath.toString();

        String jUnits = "";

        if (accessed){
        // get all of the test cases for generating accessed_classes.json file
        for (String testJarAddr : testJars){
            try {
                JarInputStream jarFile = new JarInputStream(new FileInputStream(testJarAddr));
                JarEntry fileInJar;
                while (true){
                    fileInJar = jarFile.getNextJarEntry();
                    if (fileInJar == null)
                        break;
                    if (fileInJar.getName().endsWith(".class")){
                        String className = fileInJar.getName().replaceAll("/","\\.");
                        String testClassPath = className.substring(0,className.lastIndexOf("."));
                        jUnits += (testClassPath + ":");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(jUnits);
        }

        }else {
            // get the test cases from accessed_classes.json to use them for test case seeding or model seeding.
            try {
                JsonObject root = new JsonParser().parse(new FileReader(accessedClassesPath)).getAsJsonObject();
                if (root.has(targetClass)) {
                    JsonArray ja = root.get(targetClass).getAsJsonArray();
                    for (int i = 0; i < ja.size(); i++){
                        jUnits += (ja.get(i).toString().substring(1,ja.get(i).toString().lastIndexOf('.')) + ":");}
                } else {
                    System.out.println("No Junits for class: " + targetClass);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println(jUnits);
        }







        String[] command = {
                "-generateTests",
                "-Dcriterion=CRASH",
                "-Dsandbox=FALSE",
                "-Dtest_dir="+ test_path,
                "-Drandom_tests=3",
                "-Dminimize=TRUE",
                "-Dheadless_chicken_test=FALSE",
                "-Dpopulation=100",
                "-Dsearch_budget=1800",
                "-Dtarget_frame="+targetFrame,
                "-Drandom_tests=0",
                "-Dvirtual_fs=TRUE",
                "-Dreplace_calls=FALSE",
                "-Dreport_dir=spreadsheets",
                "-Dlog_goals=TRUE",
                "-Dmax_recursion=30",
                "-Dcarve_model=TRUE",
                "-Dmodel_path=/Users/pooria/Desktop/CallSequencePoolJson",
//                "-Djunit="+jUnits,
//                "-Dmodel_sut=TRUE",
//                "-Dcollect_accessed_classes_in_tests=FALSE",
//                "-Dcarve_object_pool=TRUE",
                "-Dp_object_pool=0.5",
//                "-Dseed_clone=0",
                //"-Dwrite_cfg=true",
//                "-Dseed_mutations=0",
//                "-Dselected_junit=" + jUnits,
                "-Dtarget_exception_crash=java.lang.NullPointerException",
                "-DEXP="+ logPath,
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