package org.evosuite.coverage.evocrash;

import java.util.ArrayList;
import java.io.Serializable;

public class CrashCoveragesHistory implements Serializable  {

	private static CrashCoveragesHistory history = null;
	private static ArrayList<String> lineCoverages = new ArrayList<String>();
	protected CrashCoveragesHistory() {}
	
	public static CrashCoveragesHistory getInstance() {
		if(history == null) {
	    	  history = new CrashCoveragesHistory();
	    }
	      return history;
	}
	
	public static ArrayList<String> getCoverages(){
		return lineCoverages;
	}
	
	public static void addCoverage(String st) {
		lineCoverages.add(st);
	}
	
	public static  int getSize() {
		return lineCoverages.size();
	}
	
	
}
