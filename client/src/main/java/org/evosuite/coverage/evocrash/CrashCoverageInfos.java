package org.evosuite.coverage.evocrash;

public  class CrashCoverageInfos {
	private double FitnessFunctionMin = 1000;
	private double LineCoverageMin = 1000;
	private double ExceptionHappenedMin = 1000;
	private double StackTraceSimilarityMin = 1000;
	
	private long currentTime;
	private long startTime;
	private long FFTime ; 
	private long LCTime ; 
	private long EHTime ; 
	private long SSTime ;


	private long FFTries;
	private long LCTries;
	private long EHTries;
	private long SSTries;

	private int numberOfTries =0;


	public static CrashCoverageInfos instance = null;

	public CrashCoverageInfos() {
	}

	private void CrashCoverageInfos() {}
	
	public static CrashCoverageInfos getInstance() {
		if(instance==null){
		       instance = new CrashCoverageInfos();
		      }
		      return instance;
	}

	public double getFitnessFunctionMin() {
		return FitnessFunctionMin;
	}

	public void setFitnessFunctionMin(double fitnessFunctionMin) {
		FitnessFunctionMin = fitnessFunctionMin;
	}

	public double getLineCoverageMin() {
		return LineCoverageMin;
	}

	public void setLineCoverageMin(double lineCoverageMin) {
		LineCoverageMin = lineCoverageMin;
	}

	public double getStackTraceSimilarityMin() {
		return StackTraceSimilarityMin;
	}

	public void setStackTraceSimilarityMin(double stackTraceSimilarityMin) {
		StackTraceSimilarityMin = stackTraceSimilarityMin;
	}

	public double getExceptionHappenedMin() {
		return ExceptionHappenedMin;
	}

	public void setExceptionHappenedMin(double exceptionHappenedMin) {
		ExceptionHappenedMin = exceptionHappenedMin;
	}

	public long getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(long currentTime) {
		this.currentTime = currentTime;
	}

	public long getEHTime() {
		return EHTime;
	}

	public void setEHTime(long eHTime) {
		EHTime = eHTime;
	}

	public long getLCTime() {
		return LCTime;
	}

	public void setLCTime(long lCTime) {
		LCTime = lCTime;
	}

	public long getFFTime() {
		return FFTime;
	}

	public void setFFTime(long fFTime) {
		FFTime = fFTime;
	}

	public long getSSTime() {
		return SSTime;
	}

	public void setSSTime(long sSTime) {
		SSTime = sSTime;
	}

	public void numberOfTriesPP(){
		this.numberOfTries++;
	}

	public long getnumberOfTries(){return numberOfTries;}

	public long getFFTries() {
		return FFTries;
	}
	public void setFFTries() {
		this.FFTries = this.numberOfTries;
	}

	public long getLCTries() {
		return LCTries;
	}
	public void setLCTries() {
		this.LCTries = this.numberOfTries;
	}

	public long getEHTries() {
		return EHTries;
	}
	public void setEHTries() {
		this.EHTries = this.numberOfTries;
	}

	public long getSSTries() {
		return SSTries;
	}
	public void setSSTries() {
		this.SSTries = this.numberOfTries;
	}

	public void setStartTime(long startTime){this.startTime = startTime;}
	public long getStartTime(){return this.startTime;}




}
