package entity;

import java.util.TreeMap;

public class AnalyzedClass {
	
	//the name associated to the class to be analyzed
	private String name;
	
	//The following attributes refers to the metrics that
	//i want to analyze in a project. They are hash map
	//haveing key = index (project version) and value = Metric Value
	private TreeMap<Integer, String> buggy;
	
	public AnalyzedClass(String name) {
		this.name = name;
		this.buggy = new TreeMap<>();
	}

	public String getName() {
		return this.name;
	}
	
	public TreeMap<Integer, String> getBuggy(){
		return this.buggy;
	}
	
	public void addBuggy(Integer key, String value) {
		this.buggy.put(key, value);
	}
}
