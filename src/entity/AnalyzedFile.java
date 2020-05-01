package entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AnalyzedFile {
	
	//the name associated to the class to be analyzed
	private String name;
	//the release associated to the file
	private int releaseIndex;
	
	//the file's lines of code, referring to the last commit
	//of the file in that release
	//update this  attribute only if a newest commit of the
	//file is found.
	private int sizeLOC = 0;
	private LocalDate lastCommitDate = null;
	private LocalDate firstCommitDate = null;
	
	//sum over revisions of LOC added+deleted+modified
	private int locTouched = 0;
	
	//sum of loc added over revision,
	private int locAdded = 0;	
	//maximum over revision of loc added
	private int maxLocAdded = 0;
	
	//the number of revisions
	private int numRevisions = 0;
	
	//this will contain the name of people that 
	//worked on this file during a release
	private List<String> authors;
	
	//sum over revision of added - deleted lines of the file
	private int churn = 0;
	private int maxChurn = 0;
	
	//age in weeks
	private int age = 0;
	
	//number of file committed together with C
	//(incremental)
	private int chgSetSize = 0;
	private int maxChgSetSize = 0;
	
	
	//The following attributes refers to the metrics that
	//i want to analyze in a project. They are hash map
	//having key = index (project version) and value = Metric Value
	private List<Integer> buggy;
	
	public AnalyzedFile(String name) {
		this.name = name;
		this.buggy = new ArrayList<>();
		this.authors = new ArrayList<>();
	}
	
	//methods for sizeLOC
	
	public boolean shouldUpdateSize(LocalDate commitDate) {
		if(this.lastCommitDate == null) {
			this.lastCommitDate = commitDate;
			this.firstCommitDate = commitDate;	
			return true;
		}
		
		//every time that a commit is found, last and first
		//commit date for the file is updated
		if(commitDate.compareTo(this.firstCommitDate) < 0) {
			this.firstCommitDate = commitDate;
		}
		
		if(commitDate.compareTo(this.lastCommitDate) > 0) {
			return true;
		}
		
		return false;
	}
	
	public void updateSize(int newSize) {
		this.sizeLOC = newSize;
	}
	
	public int getSizeLoc() {
		return this.sizeLOC;
	}
	
	//methods for locTouched
	
	public void incrementLocTouched(int increment) {
		this.locTouched += increment;
	}
	
	public int getLocTouched() {
		return this.locTouched;
	}
	
	//methods for locAdded & max locAdded & avgLocAdded
	
	public void incrementLocAdded(int increment) {
		this.locAdded += increment;
		this.updateMaxLocAdded(increment);
	}
	
	public int getLocAdded() {
		return this.locAdded;
	}
	
	public int getMaxLocAdded() {
		return this.maxLocAdded;
	}
	
	public double getAvarageLocAdded() {
		return (double)this.locAdded/this.numRevisions;
	}
	
	private void updateMaxLocAdded(int la) {
		if(la > this.maxLocAdded) {
			this.maxLocAdded = la;
		}
	}
	
	//methods for numRevisions
	
	public void incrementNumRevisions() {
		this.numRevisions++;
	}
	
	public int getNumRevisions() {
		return this.numRevisions;
	}
	
	//methods for numAuthors
	
	public void addAuthor(String name) {
		if(!this.authors.contains(name)) {
			this.authors.add(name);
		}	
	}
	
	public int numAuthors() {
		return this.authors.size();
	}
	
	//methods for churn & maxChurn & avgChurn
	
	public void updateChurn(int churn) {
		this.churn += churn;
		this.updateMaxChurn(churn);
	}
	
	public int getChurn() {
		return this.churn;
	}
	
	public double getAvgChurn() {
		return (double)this.churn/this.numRevisions;
	}
	
	public int getMaxChurn() {
		return this.maxChurn;
	}
	
	private void updateMaxChurn(int ch) {
		if(ch > this.maxChurn) {
			this.maxChurn = ch;
		}
	}
	
	//methods for change set size & max & avg
	
	public void incrementChgSetSize(int chgSet) {
		this.chgSetSize += chgSet;
		this.updateMaxChgSetSize(chgSet);
	}
	
	private void updateMaxChgSetSize(int c) {
		if(c > this.maxChgSetSize) {
			this.maxChgSetSize = c;
		}
	}
	
	public int getChgSetSize() {
		return this.chgSetSize;
	}
	
	public int getMaxChgSetSize() {
		return this.maxChgSetSize;
	}
	
	public double getAvgChgSetSize() {
		return (double)this.chgSetSize/this.numRevisions;
	}
	
	//methods for age & waighted age
	
	public void setAgeWeeks(int age) {
		this.age = age;
	}
	
	public int getAgeWeeks() {
		return this.age;
	}
	
	public int getWeightedAge() {
		return this.age*this.locTouched;
	}
	
	//methods for file name and file release index
	
	public void setReleaseIndex(int releaseIndex) {
		this.releaseIndex = releaseIndex;
	}
	
	public int getReleaseIndex(int releaseIndex) {
		return this.releaseIndex;
	}

	public String getName() {
		return this.name;
	}
	
	//methods for buggyness
	
	public List<Integer> getBuggy(){
		return this.buggy;
	}
	
	public void addBuggy(List<Integer> versionIndx) {
		if(versionIndx == null) {
			return;
		}
		
		for(Integer indx: versionIndx) {
			if(!this.buggy.contains(indx)) {
				this.buggy.add(indx);
			}
		}	
	}
	
}
