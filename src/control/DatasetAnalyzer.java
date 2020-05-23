package control;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import entity.ClassifierAnalysis;
import entity.Constants;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.bayes.NaiveBayes;
import weka.filters.supervised.instance.Resample;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.evaluation.*;
import weka.classifiers.lazy.IBk;

public class DatasetAnalyzer{
	
	private String datasetName;
	private String projName;
	
	private static final String TRAINING_FILENAME = "tempTraining.arff";
	private static final String TESTING_FILENAME = "tempTesting.arff";
	
	//This list will contain all the results of each run of
	//the WalkFarkward validation method
	private List<ClassifierAnalysis> classifierAnalysis;
	
	public DatasetAnalyzer(String datasetName, String projName) {
		this.datasetName = datasetName;
		this.projName = projName;
		this.classifierAnalysis = new ArrayList<>();
		
		//setting up logger
		Handler fileHandler;
		try {
			fileHandler = new FileHandler(Constants.LOG_FILE);
			Constants.LOGGER.addHandler(fileHandler);
		} catch (SecurityException | IOException e) {
			Constants.LOGGER.log(Level.SEVERE, e.getMessage());
		}
	}

	//this method will implement the ealk farward validation method,
	//by creating 2 temporary files, that refers to testing and training dataset
	//This method will return true if all the indexes have been analyzed
	private boolean walkForward(int releaseIndx) throws IOException {
		
		int testIndx = releaseIndx+1;
		boolean allDone = false;
		
		StringBuilder notData = new StringBuilder();
		StringBuilder training = new StringBuilder();
		StringBuilder testing = new StringBuilder();
		
		int currIndx;
		
		//if temporary files were not deleted, they will be overwritten	
		try (BufferedReader reader = new BufferedReader(new FileReader(this.datasetName))){
			String line;
			while((line = reader.readLine()) != null) {
				if(line.contains("@")) {
					notData.append(line);
				}else if(line.length() < 2) {
					//if newlines are read
					notData.append(line);
				}else {
					//all the other lines starts with a number (the index release number)
					currIndx = Integer.valueOf(line.split(",")[0]);
					
					if(currIndx <= releaseIndx) {
						training.append(line);
					}else if(currIndx == testIndx) {
						testing.append(line);
					}else {
						//the index is greater than the test index
						break;
					}
				}
			}
			
			//if null was read, this is the last phase of walkForward
			if(line == null) {
				allDone = true;
			}
					
		}
		
		//writing training dataset
		try (FileWriter writer = new FileWriter(TRAINING_FILENAME)){
			writer.append(notData.toString());
			writer.append(training.toString());
		}
		
		//writing testing dataset
		try (FileWriter writer = new FileWriter(TESTING_FILENAME)){
			writer.append(notData.toString());
			writer.append(testing.toString());
		}
		
		System.out.println("walkForward done for release index: "+releaseIndx+", done: "+allDone);
		
		return allDone;
	}
	
	public void setFeatureSelection() {}
	
	public void setSamplingMethod() {}
	
	private void onlyClassifier() throws Exception {
		
		boolean notEnded = true;
		int indx = 1;
		//iterate over calssifier, make a list of classifier
		RandomForest randomForest = new RandomForest();
		NaiveBayes nativeBayes = new NaiveBayes();
		IBk ibk = new IBk();
		
		List<Classifier> classifiers = new ArrayList<>();
		classifiers.add(randomForest);
		classifiers.add(nativeBayes);
		classifiers.add(ibk);
		
		for(Classifier classifier : classifiers) {
			while(notEnded) {
				notEnded = walkForward(indx);
				
				
				DataSource source1 = new DataSource(TRAINING_FILENAME);
				Instances training = source1.getDataSet();
				DataSource source2 = new DataSource(TESTING_FILENAME);
				Instances testing = source2.getDataSet();
				
				//removing releaseIndx attribute
				testing.deleteAttributeAt(0);
				training.deleteAttributeAt(0);
				
				classifier.buildClassifier(training);
				Evaluation eval = new Evaluation(testing);
				eval.evaluateModel(classifier, testing);
				
				ClassifierAnalysis ca = new ClassifierAnalysis(this.projName, classifier.toString());
				
				//storing results
				ca.setNumTrainingReleases(indx);
				ca.setNumTrainingInstances(training.numInstances());
				ca.setNumTestingInstances(testing.numInstances());
				
				//counting classes defective in training
				int buggyIndx = training.numAttributes()-1;
				int numYes = 0;
				int numNo = 0;
				
				for(Instance instance : training) {
					String value = instance.stringValue(buggyIndx);
					if(value.equals("Yes")){
						numYes++;
					}else {
						numNo++;
					}
				}
				
				ca.setNumNotDefectiveTraining(numNo);
				ca.setNumDefectiveTraining(numYes);
				
				numYes = 0;
				numNo = 0;
				for(Instance instance : testing) {
					String value = instance.stringValue(buggyIndx);
					if(value.equals("Yes")){
						numYes++;
					}else {
						numNo++;
					}
				}
				
				ca.setNumDefectiveTesting(numYes);
				ca.setNumNotDefectiveTesting(numNo);
				
				
				
				indx++;
			}
		}
	}
	
	private void classifierAndFeaSel() {}
	
	private void classifierAndSampling() {}
	
	private void classifierFeaSelSampl() {}
	
	public void startAnalysis() throws Exception {
		
		this.onlyClassifier();
		Constants.LOGGER.log(Level.INFO, "Analysis with only calssifier done");
		this.classifierAndFeaSel();
		Constants.LOGGER.log(Level.INFO, "Analysis with calssifier and deature selection done");
		this.classifierAndSampling();
		Constants.LOGGER.log(Level.INFO, "Analysis with calssifier and balancing done");
		this.classifierFeaSelSampl();
		Constants.LOGGER.log(Level.INFO, "Analysis with classifier, feature selection and balancing done");
		
	}
	
	
	public static void main(String args[]) throws Exception{
		
		/*
		 * do this thing in starter
		DatasetAnalyzer da = new DatasetAnalyzer("BOOKKEEPER","finalTableBOOKKEEPER.arff");
		da.startAnalysis();
		*/
		
		//load datasets
		
				//ORDINATED HANDOFF (but implement walk forward...)
		
				DataSource source1 = new DataSource("/home/ezio/Scrivania/ISW2/datasets/trS.arff");
				Instances training = source1.getDataSet();
				DataSource source2 = new DataSource("/home/ezio/Scrivania/ISW2/datasets/teS.arff");
				Instances testing = source2.getDataSet();
				
				int numAttr = testing.numAttributes();
				System.out.println("Num attr: "+numAttr);
				//with numBugs it is all 1.
				//removing numBugs...
				//and thing goes worst... OK.
				testing.deleteAttributeAt(numAttr-1);
				training.deleteAttributeAt(numAttr-1);
				numAttr = testing.numAttributes();
				
				//OK it works
				for(Instance instance : testing) {
					String value = instance.stringValue(numAttr-1);
					System.out.println(value);
				}
				
				testing.deleteAttributeAt(0);
				training.deleteAttributeAt(0);
				
				numAttr = training.numAttributes();
				
				training.setClassIndex(numAttr-1);
				testing.setClassIndex(numAttr-1);

				RandomForest classifier = new RandomForest();
				//NaiveBayes classifier = new NaiveBayes();
				//Native bayes sucks

				
				classifier.buildClassifier(training);

				Evaluation eval = new Evaluation(testing);	
				
				

				eval.evaluateModel(classifier, testing);
				//MUST BE 0, if 1, SHITTY results... why ??
				System.out.println("AUC = "+eval.areaUnderROC(1));
				System.out.println("kappa = "+eval.kappa());
				System.out.println("precision = "+eval.precision(1));
				System.out.println("recall = "+eval.recall(1));
				
				/*
				 * TODO
				 * 1. classIndex in eval
				 * 2. does setClassIndex do what i think ?
				 * 3. are there other measures in eval ?
				 * 4. implement walk farward.
				 * 5. apply to different classifier (which ones ?)
				 * 6. register results.
				 * 7. do in such a way to exclude some attributes... -> FEATURE SELECTION.
				 *   (maybe it is better to wait...)
				 */
			
				
	}
}
