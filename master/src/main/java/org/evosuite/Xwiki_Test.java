package org.evosuite;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.evosuite.coverage.evocrash.LogParser;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestChromosome;
import org.junit.Test;


public class Xwiki_Test {


    private static int frameLevel;
    private static String ExceptionType = "java.lang.IllegalArgumentException";
    private static String issueNumber;
    private static String softwareVersion;
    private static Path logPath;
    private static String dependencies = "";
    private static Path testPath;
    private static Path binpath;
    private static int executionId;


    private static String budget;
    private static String p_functional_mocking;
    private static String functional_mocking_percent;
    private static String p_reflection_on_private;
    private static String reflection_start_percent;
    private static String population;
    private static String targetClass;


    @Test
    public void run(){
        String user_dir = System.getProperty("user.dir");
        testPath = Paths.get(user_dir, "GGA-tests");
        binpath = Paths.get(user_dir, "src", "test", "java", "org", "evosuite", "coverage", "evocrash");

        String bin_path = binpath.toString();
        int lineNumber = 2;
        logPath = Paths.get(user_dir,"src", "test", "java", "org", "evosuite", "coverage", "evocrash", "XWIKI-13196", "XWIKI-13196.log");


        String dependencies = "";
        File depFolder = new File(bin_path, "XWIKI-platform-7.4.2");
        System.out.println(depFolder.toString());
        //System.out.println(depFolder);
        //System.out.println(ExceptionType);
        File[] listOfFilesInSourceFolder = depFolder.listFiles();
        for (int i = 0; i < listOfFilesInSourceFolder.length; i++) {
            if (listOfFilesInSourceFolder[i].getName().charAt(0) != '.') {
                Path depPath = Paths.get(depFolder.getAbsolutePath(), listOfFilesInSourceFolder[i].getName());
                String dependency = depPath.toString();

                dependencies += (dependency + ":");
            }
        }
        dependencies = dependencies.substring(0, dependencies.length() - 1);
        String targetClass = LogParser.getTargetClass(logPath.toString(), lineNumber);
        //prepare variables:
//        JSONObject array = new JSONObject(args[0]);
//        preloadVars(array);
        // prepare command for evoCrash
        String[] command = {
                "-generateTests",
                "-Dcriterion=CRASH",
                "-Dsandbox=TRUE",
                "-Dtest_dir=" + testPath.toString(),
                "-Drandom_tests=0",
                "-Dp_functional_mocking=" + 0.8,
                "-Dfunctional_mocking_percent=" + 0.5,
                "-Dp_reflection_on_private=" + 0,
                "-Dreflection_start_percent=" + 0,
                "-Dminimize=TRUE",
                "-Dheadless_chicken_test=FALSE",
                "-Dpopulation=" + 80,
                "-Dsearch_budget=" + 18000,
                "-Dstopping_condition=MAXFITNESSEVALUATIONS",
                "-Dglobal_timeout=" + (5 * 60 * 60),
                "-Dtarget_frame=" + lineNumber,
                "-Dvirtual_fs=TRUE",
                "-Duse_separate_classloader=FALSE",
                "-Dreplace_calls=TRUE",
                "-Dmax_recursion=50",
                // "-Dtools_jar_location=/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/lib",
                "-Dreset_static_fields=FALSE",
                "-Dvirtual_net=FALSE",
                "-Dtarget_exception_crash=" + ExceptionType,
                "-DEXP=" + logPath.toString(),
                "-projectCP",
                dependencies,
                "-class",
                targetClass
        };

        EvoSuite evosuite = new EvoSuite();

        try {
            Object result = evosuite.parseCommandLine(command);
            List<List<TestGenerationResult>> results = (List<List<TestGenerationResult>>) result;
            GeneticAlgorithm<?> ga = getGAFromResult(results);

            if (ga == null) {
                // ga is null when during bootstrapping the ideal test is found!
                //Assert.assertTrue(true);
                System.exit(0);
            } else {
                TestChromosome best = (TestChromosome) ga.getBestIndividual();
                System.exit(0);
                //Assert.assertEquals(0.0, best.getFitness(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(0);
        }

    }

//
//    public static void preloadVars(JSONObject array) throws IOException {
//        executionId = array.getInt("execution_idx");
//        budget = array.getString("search_budget");
//        p_functional_mocking = array.getString("p_functional_mocking");
//        functional_mocking_percent = array.getString("functional_mocking_percent");
//        p_reflection_on_private = array.getString("p_reflection_on_private");
//        reflection_start_percent = array.getString("reflection_start_percent");
//        population = array.getString("population");
//        frameLevel = array.getInt("frame");
//        softwareVersion = array.getString("version");
//        issueNumber = array.getString("case");
//
//        String user_dir = System.getProperty("user.dir");
//        binpath = Paths.get(user_dir,"..","resources", "targetedSoftware", array.getString("application").toUpperCase()+"-bins");
//        String bin_path = binpath.toString();
//
//        logPath = Paths.get(user_dir,"..","resources", "logs" ,array.getString("application").toUpperCase(), issueNumber , issueNumber+".log");
//        //finding exception type:
////		ExceptionType = array.getString("getExceptionName");
//
//        BufferedReader br =  new BufferedReader(new FileReader(logPath.toString()));
//        String firstLine = br.readLine();
//        String[] parts = firstLine.split(":");
//        ExceptionType = parts[0];
//        br.close();
//
//
//        // get all of the dependencies
//        File depFolder = new File(bin_path,array.getString("application").toUpperCase()+"-"+softwareVersion);
//        System.out.println(depFolder);
//        System.out.println(ExceptionType);
//        File[] listOfFilesInSourceFolder = depFolder.listFiles();
//        for(int i = 0; i < listOfFilesInSourceFolder.length; i++){
//            if(listOfFilesInSourceFolder[i].getName().charAt(0) !='.') {
//                Path depPath = Paths.get(depFolder.getAbsolutePath(), listOfFilesInSourceFolder[i].getName());
//                String dependency = depPath.toString();
//
//                dependencies += (dependency+":");
//            }
//        }
//        dependencies = dependencies.substring(0, dependencies.length() - 1);
//
//
//        //set the place which generated test should be saved.
//        testPath = Paths.get(user_dir,"..", "GGA-tests",issueNumber,"frame-"+frameLevel,"R"+executionId+"_PM"+p_functional_mocking+"_Mperc"+functional_mocking_percent+"_SB"+budget+"_POP"+population);
//        //testPath = Paths.get(user_dir, "GGA-tests");
//
//        //set the target class
//        targetClass = LogParser.getTargetClass(logPath.toString(), frameLevel);
//    }
//

    @SuppressWarnings("unchecked")
    protected GeneticAlgorithm<?> getGAFromResult(Object result) {
        assert (result instanceof List);
        List<List<TestGenerationResult>> results = (List<List<TestGenerationResult>>) result;
        if (results.size() == 1) {
            return results.get(0).get(0).getGeneticAlgorithm();
        } else {
            return null;
        }
    }
}
