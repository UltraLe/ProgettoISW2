package control;

import entity.Constants;

public class Starter {
	
	private Starter() {
		throw new IllegalStateException("Utility class");
	}
	
	public static void main(String[] args) {
		
		
		//project DAFFODIL
		Constants.jiraProjName = "DAFFODIL";
		Constants.gitProjName = "incubator-daffodil";
		GitInteractor.getLastCommits();
		
		//project BOOKKEEPER
		Constants.jiraProjName = "BOOKKEEPER";
		Constants.gitProjName = "bookkeeper";
		Buggy.getBuggyFiles();
		GitFilesAttributesFinder.getFinalTable();
		
		
		//project SYNCOPE
		Constants.jiraProjName = "SYNCOPE";
		Constants.gitProjName = "syncope";
		Buggy.getBuggyFiles();
		GitFilesAttributesFinder.getFinalTable();
	}

}
