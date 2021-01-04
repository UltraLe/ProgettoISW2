package control;

import entity.Constants;

public class Starter {
	
	private Starter() {
		throw new IllegalStateException("Utility class");
	}
	
	public static void main(String[] args) {
		
		
		//project DAFFODIL
		Constants.setJiraProjName("DAFFODIL");
		Constants.setGitProjName("incubator-daffodil");
		GitInteractor.getBugsPerMonth();
		
		//project BOOKKEEPER
		Constants.setJiraProjName("BOOKKEEPER");
		Constants.setGitProjName("bookkeeper");
		Buggy.getBuggyFiles();
		GitFilesAttributesFinder.getFinalTable();
		
		
		//project SYNCOPE
		Constants.setJiraProjName("SYNCOPE");
		Constants.setGitProjName("syncope");
		Buggy.getBuggyFiles();
		GitFilesAttributesFinder.getFinalTable();
		
		//evaluating results
		DatasetAnalyzer da = new DatasetAnalyzer("finalTableBOOKKEEPER.arff", "BOOKKEEPER");
		da.startAnalysis();
		
		da = new DatasetAnalyzer("finalTableSYNCOPE.arff", "SYNCOPE");
		da.startAnalysis();
		
		
		//Used to generate file for De Angelis part of project
		Constants.setJiraProjName("BOOKKEEPER");
		Constants.setGitProjName("bookkeeper");
		GitFilesAttributesFinder g = new GitFilesAttributesFinder(Constants.getJiraProjName());
		g.getLastReleaseMetrics();
		
		Constants.setJiraProjName("SYNCOPE");
		Constants.setGitProjName("syncope");
		g = new GitFilesAttributesFinder(Constants.getJiraProjName());
		g.getLastReleaseMetrics();
		
		
	}

}
