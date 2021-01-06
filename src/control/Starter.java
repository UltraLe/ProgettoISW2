package control;

import entity.Constants;

public class Starter {
	
	private Starter() {
		throw new IllegalStateException("Utility class");
	}
	
	public static void main(String[] args) {
		
		
		//project DAFFODIL
		Constants.setJiraProjName(Constants.DAFFODIL_PRJ);
		Constants.setGitProjName(Constants.DAFFODIL_PRJ_GIT);
		GitInteractor.getBugsPerMonth();
		
		
		
		//project BOOKKEEPER
		Constants.setJiraProjName(Constants.BOOKKEEPER_PRJ);
		Constants.setGitProjName(Constants.BOOKKEEPER_PRJ_GIT);
		Buggy.getBuggyFiles();
		GitFilesAttributesFinder.getFinalTable();
		
		
		
		//project SYNCOPE
		Constants.setJiraProjName(Constants.SYNCOPE_PRJ);
		Constants.setGitProjName(Constants.SYNCOPE_PRJ_GIT);
		Buggy.getBuggyFiles();
		GitFilesAttributesFinder.getFinalTable();
		
		
		
		//evaluating results
		// *	*	*	*	*	*	*
		//The dataset (.csv) created with the previous method calls MUST BE converted in a .arff file
		//removing the column that specifies the filenames and the number of bugs.
		// *	*	*	*	*	*	*


		DatasetAnalyzer da = new DatasetAnalyzer("finalTableBOOKKEEPER.arff", Constants.BOOKKEEPER_PRJ);
		da.startAnalysis();
		
		da = new DatasetAnalyzer("finalTableSYNCOPE.arff", Constants.SYNCOPE_PRJ);
		da.startAnalysis();
		
		
		//Used to generate file for De Angelis part of project
		Constants.setJiraProjName(Constants.BOOKKEEPER_PRJ);
		Constants.setGitProjName(Constants.BOOKKEEPER_PRJ_GIT);
		GitFilesAttributesFinder g = new GitFilesAttributesFinder(Constants.getJiraProjName());
		g.getLastReleaseMetrics();
		
		Constants.setJiraProjName(Constants.SYNCOPE_PRJ);
		Constants.setGitProjName(Constants.SYNCOPE_PRJ_GIT);
		g = new GitFilesAttributesFinder(Constants.getJiraProjName());
		g.getLastReleaseMetrics();
		
		
	}

}
