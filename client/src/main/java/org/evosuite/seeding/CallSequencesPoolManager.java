package org.evosuite.seeding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.evosuite.testcarver.extraction.CarvingManager;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.generic.GenericClass;
import java.lang.reflect.Type;
import java.io.*;
import java.util.*;

public class CallSequencesPoolManager extends CallSequencesPool {
    private static CallSequencesPoolManager instance = null;
    private CallSequencesPoolManager() {
        initialisePool();
    }


    public static CallSequencesPoolManager getInstance() {
        if(instance == null)
            instance = new CallSequencesPoolManager();
        return instance;
    }

    private void initialisePool() {
        if(Properties.SELECTED_JUNIT.length()>0) {
            // Using carved tests for initialising the pool
            CarvingManager manager = CarvingManager.getInstance();
            for (Class<?> targetClass : manager.getClassesWithTests()) {
                List<TestCase> tests = manager.getTestsForClass(targetClass);
                GenericClass cut = new GenericClass(targetClass);
                for (TestCase test : tests) {
                    Map<String, List<MethodCalls>> temp = new HashMap<String, List<MethodCalls>>();
                    for (int i = 0; i < test.size(); i++) {
                        Statement statement = test.getStatement(i);
                        if (statement.getClass().getSimpleName().equals("MethodStatement")) {
                            String keyName = statement.getAccessibleObject().getOwnerClass().getClassName();
                            for (VariableReference var : statement.getUniqueVariableReferences()) {
                                if (var.getType().getTypeName().equals(statement.getAccessibleObject().getOwnerClass().getClassName())) {
                                    keyName = keyName + "-" + var.getName();
                                    break;
                                }
                            }
                            if (!temp.containsKey(keyName)) {
                                temp.put(keyName, new ArrayList<MethodCalls>());
                            }
                            temp.get(keyName).add(new MethodCalls(statement));
                        }else if(statement.getClass().getSimpleName().equals("ConstructorStatement")){
                            String keyName = statement.getAccessibleObject().getOwnerClass().getClassName();
                            for (VariableReference var : statement.getUniqueVariableReferences()) {
                                if (var.getType().getTypeName().equals(statement.getAccessibleObject().getOwnerClass().getClassName())) {
                                    keyName = keyName + "-" + var.getName();
                                    break;
                                }
                            }
                            if (!temp.containsKey(keyName)) {
                                temp.put(keyName, new ArrayList<MethodCalls>());
                            }
                            temp.get(keyName).add(new MethodCalls(statement));
                        }
                    }
                    for (Map.Entry<String, List<MethodCalls>> entry : temp.entrySet()) {
                        String key = entry.getKey();
                        List<MethodCalls> callSeqs = entry.getValue();
                        if (key.indexOf('-') != -1) {
                            this.addSequence(key.substring(0, key.indexOf('-')), callSeqs);
                        } else {
                            LoggingUtils.getEvoLogger().error("key " + key + " does not have - char!!!");
                        }
                    }
                }
            }
        }



//         using source code for initialising the pool
        GraphPool graphPool = GraphPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT());
        Set<String> clazzes = graphPool.getAvailableGraphsClassname();
        for(String clazz: clazzes){
            LoggingUtils.getEvoLogger().info("Reading Call Sequences from class "+clazz);
            Map<String, RawControlFlowGraph> methodsGraphs = graphPool.getRawCFGs(clazz);
            for(Map.Entry<String,RawControlFlowGraph> entry : methodsGraphs.entrySet()){
                Map<String, List<MethodCalls>> temp = new HashMap<String, List<MethodCalls>>();
                String methodname = entry.getKey();
                LoggingUtils.getEvoLogger().info("Reading Call Sequences from method "+methodname);
                RawControlFlowGraph cfg = entry.getValue();
                List<BytecodeInstruction> bcList = cfg.determineMethodCalls();
                for (BytecodeInstruction bc : bcList){
                    String keyName = bc.getCalledMethodsClass();
                    if (!keyName.equals(clazz)){
                    if (!temp.containsKey(keyName)){
                        temp.put(keyName,new ArrayList<MethodCalls>());
                    }
                    temp.get(keyName).add(new MethodCalls(bc));}else{
                        LoggingUtils.getEvoLogger().info("Same class: "+clazz+ "For method name -> "+bc.getCalledMethod());
                    }
                }
                for (Map.Entry<String, List<MethodCalls>> tempEntry : temp.entrySet()) {
                    String key = tempEntry.getKey();
                    List<MethodCalls> callSeqs = tempEntry.getValue();
                    this.addSequence(key, callSeqs);
                }
            }
        }


    }


    public void report(){
        for (Map.Entry<String, Set<List<MethodCalls>>> entry : this.pool.entrySet()) {
            String key = entry.getKey();
            Set<List<MethodCalls>> callSeqsSet = entry.getValue();
            LoggingUtils.getEvoLogger().info("The Class is: "+key);
            for (List<MethodCalls> callSeqs:callSeqsSet){
                for(MethodCalls call: callSeqs){
                    LoggingUtils.getEvoLogger().info(call.getMethodName());
                    for (String t: call.getParams()){
                        LoggingUtils.getEvoLogger().info(t);
                    }
                    LoggingUtils.getEvoLogger().info("----");
                }
                LoggingUtils.getEvoLogger().info("~~~~~~~~~");
            }

        }
    }


    public void savePool(){
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(this.pool,pool.getClass());

        try (PrintWriter out = new PrintWriter(Properties.CALL_SEQUENCES_OUTPUT_PATH)) {
            out.println(json);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void readPoolFromTheFile(String fileName){
        try {
            String json = readFile(fileName);
            Gson gson = new GsonBuilder().create();
            Type listType = new TypeToken<HashMap<String, Set<List<MethodCalls>>>>(){}.getType();
            this.reWritePool(gson.fromJson(json,listType));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private String readFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }
}
