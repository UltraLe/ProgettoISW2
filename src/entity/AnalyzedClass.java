package entity;

import java.util.ArrayList;
import java.util.List;

public class AnalyzedClass {
	
	//the name associated to the class to be analyzed
	private String name;
	
	//The following attributes refers to the metrics that
	//i want to analyze in a project. They are hash map
	//having key = index (project version) and value = Metric Value
	private List<Integer> buggy;
	
	public AnalyzedClass(String name) {
		this.name = name;
		this.buggy = new ArrayList<>();
	}

	public String getName() {
		return this.name;
	}
	
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
