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
import org.evosuite.utils.LoggingUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import sun.rmi.runtime.Log;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

//import org.crash.client.log.LogParser;
//import org.crash.master.EvoSuite;


@NotThreadSafe
@Ignore
public class LANG_6b_Test {

    @Test
    public void lang_6b_frameLevel_1(){
        boolean accessed =
                false;
        boolean modeling = true;


        int targetFrame = 2 ;
        String user_dir = System.getProperty("user.dir");

//        Path binpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash","MATH-bins","MATH-4b");
        Path binpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash","XWIKI-9.5");
//        Path binpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash","fake");
        String bin_path = binpath.toString();

//        Path logpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash", "MATH-4b" , "MATH-4b.log");
        Path logpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash", "XWIKI-14475" , "XWIKI-14475.log");
//        Path logpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash", "fakeLog" , "fake.log");pwd
        String logPath = logpath.toString();

        Path accessedOutput = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash","accessed_classes.json");
        String accessed_output_file = accessedOutput.toString();

        Path CallSequencesOutput = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash","CallSequencePoolJson.json");
        String call_sequences_file = CallSequencesOutput.toString();


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
        System.out.println("target is: "+targetClass);

        Path testpath = Paths.get(user_dir, "GGA-tests");
        String test_path = testpath.toString();

        String jUnits = "";

        String staticAnalysisTarget="";
        int numberOfFrames = LogParser.getNumberOfFrames(logPath);
        for(int i=1;i<=numberOfFrames;i++)
            staticAnalysisTarget += (LogParser.getTargetClass(logPath,i) + ":");
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
        }

        }else if(modeling){
            String allTestsJson = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage","evocrash","XWIKI-14475.json").toString();
            try {
                JsonObject root = new JsonParser().parse(new FileReader(allTestsJson)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    JsonArray testsArray = entry.getValue().getAsJsonArray();
                    for (int i=0;i <testsArray.size();i++){
                        String existingTestPath = testsArray.get(i).getAsString().substring(0,testsArray.get(i).toString().lastIndexOf('.')-1);
                        if (!jUnits.contains(existingTestPath)) {
//                            System.out.println(existingTestPath);
                            jUnits += (existingTestPath+":");
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }else {
            // get the test cases from accessed_classes.json to use them for test case seeding or model seeding.
            try {
                JsonObject root = new JsonParser().parse(new FileReader(accessedClassesPath)).getAsJsonObject();
                if (root.has(targetClass)) {
                    System.out.println(":(");
                    JsonArray ja = root.get(targetClass).getAsJsonArray();
                    for (int i = 0; i < ja.size(); i++){
                        jUnits += (ja.get(i).toString().substring(1,ja.get(i).toString().lastIndexOf('.')) + ":");}
                } else {
                    System.out.println("No Junits for class: " + targetClass);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
//            jUnits="org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormat_ParserTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormatTest:org.apache.commons.lang3.time.FastDateFormat_PrinterTest:org.apache.commons.lang3.time.FastDateFormat_PrinterTest:org.apache.commons.lang3.time.FastDateFormat_PrinterTest:org.apache.commons.lang3.time.FastDateFormat_PrinterTest:org.apache.commons.lang3.time.FastDateFormat_PrinterTest:org.apache.commons.lang3.time.FastDateFormat_PrinterTest:org.apache.commons.lang3.time.FastDateFormat_PrinterTest:org.apache.commons.lang3.time.FastDateFormat_PrinterTest:org.apache.commons.lang3.time.FastDateFormat_PrinterTest:org.apache.commons.lang3.time.FastDateFormat_PrinterTest";
            System.out.println("JJ"+jUnits);
        }









        String[] command = {


                "-generateTests",
                "-Dcriterion=CRASH",
                "-Dsandbox=TRUE",
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
                "-Dmodel_path=/Users/pooria/Desktop/CallSequencePoolJson",
                "-Daccessed_classes_output_path="+accessed_output_file,
                "-Dcall_Sequences_output_path="+call_sequences_file,
                "-Dcarve_model=FALSE",
                "-Djunit="+jUnits,
                "-Dselected_junit=" + jUnits,
                "-Dseed_mutations=0",
                "-Dp_object_pool=0",
                "-Dcarve_object_pool=TRUE",
                "-Dcollect_accessed_classes_in_tests=FALSE",
                "-Dmodel_sut=TRUE",
                "-Dproject_keyword=math",
                "-Dtarget_exception_crash=java.lang.NullPointerException",
                "-DEXP="+ logPath,
                "-projectCP",
                dependencies,
                "-class",
                targetClass,
                "-DCP_static_analysis="+staticAnalysisTarget,


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