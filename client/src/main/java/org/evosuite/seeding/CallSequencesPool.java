package org.evosuite.seeding;


import java.io.Serializable;
import java.util.*;
public class CallSequencesPool implements Serializable {
    protected Map<String, Set<List<MethodCalls>>> pool = new HashMap<String, Set<List<MethodCalls>>>();

    public void addSequence(String clazz, List<MethodCalls> sequences) {
        if (!pool.containsKey(clazz))
            pool.put(clazz, new HashSet<List<MethodCalls>>());

        pool.get(clazz).add(sequences);
    }

    public void reWritePool(Map<String, Set<List<MethodCalls>>> poolFromFile){
        pool = new HashMap<String, Set<List<MethodCalls>>>(poolFromFile);
    }


}
