package control;
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
import entity.EvaluationException;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.bayes.NaiveBayes;
import weka.filters.Filter;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.filters.supervised.instance.SMOTE;

// 						!	!	!
//JVM agruments: -Djava.util.Arrays.useLegacyMergeSort=true

public class DatasetAnalyzer{
	
	private String datasetName;
	private String projName;
	
	private static final String TRAINING_FILENAME = "tempTraining.arff";
	private static final String TESTING_FILENAME = "tempTesting.arff";
	private static final String NONE = "None";
	private static final String OVERSAMPLING = "Oversampling";
	private static final String UNDERSAMPLING = "Undersampling";
	private static final String SMOTE = "SMOTE";
	private static final boolean SAVE_ONLY_LAST_MODEL = true;
	
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

	//this method will implement the walk forward validation method,
	//by creating 2 temporary files, that refers to testing and training dataset
	//This method will return true if all the indexes have been analyzed
	private boolean walkForward(int releaseIndx) throws IOException {
		
		int testIndx = -1;
		boolean stop = false;
		
		StringBuilder notData = new StringBuilder();
		StringBuilder training = new StringBuilder();
		StringBuilder testing = new StringBuilder();
		
		int currIndx = 1;
		boolean choosen = false;
		
		//if temporary files were not deleted, they will be overwritten	
		try (BufferedReader reader = new BufferedReader(new FileReader(this.datasetName))){
			String line;
			StringBuilder completeLine = new StringBuilder();
			while((line = reader.readLine()) != null) {
				completeLine.append(line);
				completeLine.append("\n");
				
				if(completeLine.toString().contains("@")) {
					notData.append(completeLine.toString());
				}else if(completeLine.toString().length() < 2) {
					//if newlines are read
					notData.append(completeLine.toString());
				}else {
					//all the other lines starts with a number (the index release number)
					currIndx = Integer.valueOf(completeLine.toString().split(",")[0]);
					
					//when the index becomes grater than the release (training) index
					//the testing index has to be assigned
					if(!choosen && currIndx > releaseIndx) {
						testIndx = currIndx;
						choosen = true;
					}
					
					if(currIndx <= releaseIndx) {
						training.append(completeLine.toString());
					}else if(currIndx == testIndx) {
						testing.append(completeLine.toString());
					}else {
						//the index is greater than the test index
						break;
					}
				}
				completeLine = new StringBuilder();
			}
			
			//if null was read, this is the last phase of walkForward
			if(line == null) {
				stop = true;
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
		
		return stop;
	}
	
	private int countAttrValues(Instances instances, int classIndx, String value) {
		
		int nums = 0;
		
		for(Instance in : instances) {
			if(value.equals(in.stringValue(classIndx))){
				nums++;
			}
		}
		
		return nums;
	}
	
	private void evaluateModel(Classifier classifier, Instances training, Instances testing,
												int trainingRelease, String featureSelection, 
												String balancing, String classifierName) throws EvaluationException {
		
		Evaluation eval;
		try {
			eval = new Evaluation(testing);
			eval.evaluateModel(classifier, testing);
		} catch (Exception e) {
			throw new EvaluationException(e.getMessage());
		}
		
		
		ClassifierAnalysis ca = new ClassifierAnalysis(this.projName, classifierName);
		
		
		//settin feature selection and balancing methods
		ca.setFeatureSelection(featureSelection);
		ca.setBalancing(balancing);
		
		//storing results
		ca.setNumTrainingReleases(trainingRelease);
		ca.setNumTrainingInstances(training.numInstances());
		ca.setNumTestingInstances(testing.numInstances());
		
		//counting classes defective in training
		int buggyIndx = training.numAttributes()-1;
		int numYesTr = countAttrValues(training, buggyIndx, "Yes");
		int numNoTr = countAttrValues(training, buggyIndx, "No");
		
		ca.setNumNotDefectiveTraining(numNoTr);
		ca.setNumDefectiveTraining(numYesTr);
		
		//counting classes defective in testing
		int numYesTe = countAttrValues(testing, buggyIndx, "Yes");
		int numNoTe = countAttrValues(testing, buggyIndx, "No");
		
		ca.setNumDefectiveTesting(numYesTe);
		ca.setNumNotDefectiveTesting(numNoTe);
		
		//setting up accuracy metrics
		double precision = eval.precision(1);
		double kappa = eval.kappa();
		double roc = eval.areaUnderROC(1);
		double recall = eval.recall(1);
		
		ca.setPrecision(precision);
		ca.setKappa(kappa);
		ca.setRocArea(roc);
		ca.setRecall(recall);
		
		//setting TP, FP, TN FN
		int tp = (int) eval.numTruePositives(1);
		int fp = (int) eval.numFalsePositives(1);
		int tn = (int) eval.numTrueNegatives(1);
		int fn = (int) eval.numFalseNegatives(1);
		
		ca.setTruePositive(tp);
		ca.setFalsePositive(fp);
		ca.setTrueNegative(tn);
		ca.setFalseNegative(fn);
		
		//if the # of buggy classes in testing or training = 0, 
		//the evaluation of the classifier would be meaningless
		//so the ca is not added to the final result
		if(numYesTr != 0 && numYesTe != 0) {
			classifierAnalysis.add(ca);
		}
		
	}
	
	private FilteredClassifier configureBalancing(Instances training, String balancing, int buggyIndx) throws EvaluationException {
		
		FilteredClassifier fc = new FilteredClassifier();
		
		if(balancing.equals(OVERSAMPLING)){
			
			int numYesTr = countAttrValues(training, buggyIndx, "Yes");
			int numNoTr = countAttrValues(training, buggyIndx, "No");
			if(numYesTr+numNoTr == 0) {
				throw new EvaluationException("NumYesTr = NumNoTr = 0");
			}
			double doublePercOfMajClass = (double)numNoTr/(numYesTr+numNoTr);
			
			Resample resample = new Resample();
			try {
				resample.setInputFormat(training);
				String[] opts = new String[]{ "-B", "1.0","-Z", String.valueOf(2*doublePercOfMajClass)};
				resample.setOptions(opts);
			} catch (Exception e) {
				throw new EvaluationException(e.getMessage());
			}
			
			fc.setFilter(resample);	
			
		}else if(balancing.equals(UNDERSAMPLING)) {
			
			SpreadSubsample  spreadSubsample = new SpreadSubsample();
			try {
				String[] opts = new String[]{ "-M", "1.0"};
				spreadSubsample.setOptions(opts);
			}catch(Exception e) {
				throw new EvaluationException(e.getMessage());
			}
			fc.setFilter(spreadSubsample);
			
		}else if(balancing.equals(SMOTE)) {
			SMOTE smote = new SMOTE();
			try {
				smote.setInputFormat(training);
			} catch (Exception e) {
				throw new EvaluationException(e.getMessage());
			}
			fc.setFilter(smote);
			
		}else {
			Constants.LOGGER.log(Level.SEVERE, "Unsupported type of balancing selected");
		}

		return fc;
		
	}
	
	private void calssifierEvaluation(boolean featureSelection, String balancing) throws IOException, EvaluationException {
		
		boolean notEnded;
		int indx;
		String fs = "None";
		//iterate over calssifier, make a list of classifier
		RandomForest randomForest = new RandomForest();
		NaiveBayes nativeBayes = new NaiveBayes();
		IBk ibk = new IBk();
		FilteredClassifier fc = null;
		
		List<Classifier> classifiers = new ArrayList<>();
		
		classifiers.add(randomForest);
		classifiers.add(nativeBayes);
		classifiers.add(ibk);
		
		String indexStr;
		
		
		for(Classifier classifier : classifiers) {
			
			try {
			
			notEnded = true;
			indx = 1;
			while(notEnded) {
				notEnded = !walkForward(indx);
				
				
				indexStr = "-"+indx+"-";
				if(SAVE_ONLY_LAST_MODEL) {
					indexStr = "-";
				}
				
				DataSource source1 = new DataSource(TRAINING_FILENAME);
				Instances training = source1.getDataSet();
				DataSource source2 = new DataSource(TESTING_FILENAME);
				Instances testing = source2.getDataSet();
				
				//removing releaseIndx attribute
				testing.deleteAttributeAt(0);
				training.deleteAttributeAt(0);
				
				//setting class to analyze
				training.setClassIndex(training.numAttributes()-1);
				testing.setClassIndex(testing.numAttributes()-1);
				
				if(featureSelection) {
					//create AttributeSelection object
					AttributeSelection filter = new AttributeSelection();
					//create evaluator and search algorithm objects
					CfsSubsetEval eval = new CfsSubsetEval();
					GreedyStepwise search = new GreedyStepwise();
					//set the algorithm to search backward
					search.setSearchBackwards(true);
					//set the filter to use the evaluator and search algorithm
					filter.setEvaluator(eval);
					filter.setSearch(search);
					//specify the dataset
					filter.setInputFormat(training);
					//apply
					training = Filter.useFilter(training, filter);
					testing = Filter.useFilter(testing, filter);
					fs = "Yes";
				}

				if(!balancing.equals(NONE)) {
					int buggyIndx = training.numAttributes()-1;
					fc = configureBalancing(training, balancing, buggyIndx);
					fc.setClassifier(classifier);
					fc.buildClassifier(training);
					evaluateModel(fc, training, testing, indx, fs, balancing, classifier.getClass().getSimpleName());
					
					//saving the model
					weka.core.SerializationHelper.write(Constants.MODELS_DIR+this.projName+indexStr+classifier.getClass().getSimpleName()+
											"-FS-"+fs+"-SAM-"+balancing+".model", fc);
				}else {
					classifier.buildClassifier(training);
					evaluateModel(classifier, training, testing, indx, fs, balancing, classifier.getClass().getSimpleName());
					
					//saving the model
					weka.core.SerializationHelper.write(Constants.MODELS_DIR+this.projName+indexStr+classifier.getClass().getSimpleName()+
														"-FS-"+fs+"-SAM-"+balancing+".model", classifier);
				}
				
				indx++;
			}
			
			}catch(Exception e) {
				throw new EvaluationException(e.getMessage());
			}
		}
		
		Constants.LOGGER.log(Level.INFO, "Written results for 'only classifiers'");
	}
	
	public void startAnalysis(){
		
		boolean[] fs = {true, false};
		String[] samplingMethods = {NONE, OVERSAMPLING, UNDERSAMPLING, SMOTE};
		
		for(boolean b : fs) {
			for(String sampling : samplingMethods) {
				
				try {
					this.calssifierEvaluation(b, sampling);
				} catch (IOException | EvaluationException e) {
					//some releases could not have enough difective classes (AVRO in C Language) 
					Constants.LOGGER.log(Level.SEVERE, e.getMessage());
					continue;
				}
				Constants.LOGGER.log(Level.INFO, "Analysis with FS,Sampling = "+b+","+sampling+" done");
			}
			
			//Storing results
			try {
				CsvFileWriter.writeWekaResults(classifierAnalysis, this.projName);
			} catch (IOException e) {
				Constants.LOGGER.log(Level.SEVERE, e.getMessage());
			}
		}
	}
}
