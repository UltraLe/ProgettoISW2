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
		Constants.setProgLangExt(Constants.JAVA_LANG);
		GitInteractor.getBugsPerMonth();
		
		
		
		//project BOOKKEEPER
		Constants.setJiraProjName(Constants.BOOKKEEPER_PRJ);
		Constants.setGitProjName(Constants.BOOKKEEPER_PRJ_GIT);
		Constants.setProgLangExt(Constants.JAVA_LANG);
		Buggy.getBuggyFiles();
		GitFilesAttributesFinder.getFinalTable();
		
		
		
		//project SYNCOPE
		Constants.setJiraProjName(Constants.SYNCOPE_PRJ);
		Constants.setGitProjName(Constants.SYNCOPE_PRJ_GIT);
		Constants.setProgLangExt(Constants.JAVA_LANG);
		Buggy.getBuggyFiles();
		GitFilesAttributesFinder.getFinalTable();
		
		
		
		//project FALCON
		Constants.setJiraProjName(Constants.FALCON_PRJ);
		Constants.setGitProjName(Constants.FALCON_PRJ_GIT);
		Constants.setProgLangExt(Constants.JAVA_LANG);
		Buggy.getBuggyFiles();
		GitFilesAttributesFinder.getFinalTable();
		
		
		
		//project NON-JAVA (python) AVRO
		Constants.setJiraProjName(Constants.AVRO_PRJ);
		Constants.setGitProjName(Constants.AVRO_PRJ_GIT);
		Constants.setProgLangExt(Constants.C_LANG);
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
		
		
		da = new DatasetAnalyzer("finalTableAVRO.arff", Constants.AVRO_PRJ);
		da.startAnalysis();
		
		da = new DatasetAnalyzer("finalTableFALCON.arff", Constants.FALCON_PRJ);
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
