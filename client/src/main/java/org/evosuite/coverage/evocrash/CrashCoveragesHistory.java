package org.evosuite.coverage.evocrash;

import java.util.ArrayList;
import java.io.Serializable;

public class CrashCoveragesHistory implements Serializable  {

    private static CrashCoveragesHistory history = null;
    private static ArrayList<String> lineCoverages = new ArrayList<String>();
    private CrashCoveragesHistory() {}

    public static CrashCoveragesHistory getInstance() {
        if(history == null) {
            history = new CrashCoveragesHistory();
        }
        return history;
    }

    public ArrayList<String> getCoverages(){
        return lineCoverages;
    }

    public void addCoverage(String st) {
        lineCoverages.add(st);
    }

    public  int getSize() {
        return lineCoverages.size();
    }


}