package control;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;

import entity.AnalyzedFile;
import entity.ClassifierAnalysis;
import entity.Constants;

public class CsvFileWriter {
	
	private CsvFileWriter() {
		throw new IllegalStateException("Utility class");
	}
	
	private static Date addMonth(Date date, int i) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, i);
        return cal.getTime();
    }
	
	public static void writeFilesAttributes(SortedMap<Integer, HashMap<String, AnalyzedFile>> allReleasesFiles, String projName) throws IOException {
		
		String filename = whichFilename(Constants.FINAL_TABLE,projName,Constants.CSV_EXT);
		
		try (FileWriter csvWriter = new FileWriter(filename)){
			
			csvWriter.append("Release Index");
			csvWriter.append(",");
			csvWriter.append("File");
			csvWriter.append(",");
			csvWriter.append("LOC");
			csvWriter.append(",");
			csvWriter.append("LOC Thouched");
			csvWriter.append(",");
			csvWriter.append("LOC Added");
			csvWriter.append(",");
			csvWriter.append("MAX LOC Added");
			csvWriter.append(",");
			csvWriter.append("AVERAGE LOC Added");
			csvWriter.append(",");
			csvWriter.append("Num Revisions");
			csvWriter.append(",");
			csvWriter.append("Num Authors");
			csvWriter.append(",");
			csvWriter.append("Churn");
			csvWriter.append(",");
			csvWriter.append("MAX Churn");
			csvWriter.append(",");
			csvWriter.append("AVERAGE Churn");
			csvWriter.append(",");
			csvWriter.append("ChgSet Size");
			csvWriter.append(",");
			csvWriter.append("MAX ChgSetSize");
			csvWriter.append(",");
			csvWriter.append("AVERAGE ChgSetSize");
			csvWriter.append(",");
			csvWriter.append("Buggy");
			csvWriter.append(",");
			csvWriter.append("Num Bugs");
			csvWriter.append("\n");
			
			//missing, buggy, age, weighted age, add num bugs per file
			
			for (Map.Entry<Integer, HashMap<String, AnalyzedFile>> entry : allReleasesFiles.entrySet()) {
				
				for(Map.Entry<String, AnalyzedFile> innerEntry : entry.getValue().entrySet()) {

						AnalyzedFile af = innerEntry.getValue();
						
						csvWriter.append(String.valueOf(entry.getKey()));
						csvWriter.append(",");
						csvWriter.append(af.getName());
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getSizeLoc()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getLocTouched()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getLocAdded()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getMaxLocAdded()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getAvarageLocAdded()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getNumRevisions()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.numAuthors()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getChurn()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getMaxChurn()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getAvgChurn()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getChgSetSize()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getMaxChgSetSize()));
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getAvgChgSetSize()));
						csvWriter.append(",");
						csvWriter.append(af.getBugginess());
						csvWriter.append(",");
						csvWriter.append(String.valueOf(af.getnumBugs()));
				        csvWriter.append("\n");
					
				}
			}

		}
		
	}
	
	//method that has to take a map of string and integer
	//and write on a csv file how the integer corresponding to each string
	public static void monthCommitsCSV(Map<Date, Integer> commitsMap, String projName) throws IOException{
		
		//in order to order the map by date an hash tree is used
		TreeMap<Date, Integer> sortedMap = new TreeMap<>(commitsMap);
		
		//add zeros for missing month
		Date currentDate = sortedMap.firstKey();
		Map<Date, Integer> tempMap = new HashMap<>();
		
		for (Map.Entry<Date, Integer> entry : sortedMap.entrySet()) {
			
			//if the current date is missing, then we need to add it
			while(true) {
				if(currentDate.compareTo(entry.getKey()) == 0) {
					break;
				}else if(currentDate.compareTo(entry.getKey()) < 0){
					//if i am here the current month is missing, so i have to add it
					tempMap.put(currentDate, 0);
				}else {
					Constants.LOGGER.log(Level.SEVERE,"Something went wrong in date comparison");
				}
				//add 1 month to first date
				currentDate = addMonth(currentDate, 1);
			}
			
			//add 1 month to first date
			currentDate = addMonth(currentDate, 1);
		}
		
		sortedMap.putAll(tempMap);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
		
		String filename = whichFilename(Constants.COMMINTS_MONTH,projName,Constants.CSV_EXT);
		
		try (FileWriter csvWriter = new FileWriter(filename)){
			
			csvWriter.append("Date");
			csvWriter.append(",");
			csvWriter.append("Commits");
			csvWriter.append("\n");
			
			for (Map.Entry<Date, Integer> entry : sortedMap.entrySet()) {
				csvWriter.append(dateFormat.format(entry.getKey()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getValue()));
		        csvWriter.append("\n");
			}

		}
	}
	
	public static String whichFilename(String name, String projName, String ext) {
		
		String filename;
		
		if(Constants.TKT_SEARCH_FAST) {
			filename = name+projName+Constants.FAST+ext;
		}else {
			filename = name+projName+ext;
		}
		
		return filename;
	}
	
	public static void writeBuggyClasses(List<AnalyzedFile> classes, String projName) throws IOException {
		
		String filename = whichFilename(Constants.BUGGY_FILENAME,projName,Constants.CSV_EXT);
	
		
		try (FileWriter csvWriter = new FileWriter(filename)){
					
			csvWriter.append("Class");
			csvWriter.append(",");
			csvWriter.append("Buggy In Version");
			csvWriter.append("\n");
					
			for (AnalyzedFile entry : classes) {
				for(Integer av : entry.getBuggy()) {
					
					csvWriter.append(entry.getName());
					csvWriter.append(",");
					csvWriter.append(String.valueOf(av));
			        csvWriter.append("\n");
				}
			}
		}
		
	}
	
	public static void writeWekaResults(List<ClassifierAnalysis> classifierAnalysis, String projName) throws IOException {
		
		String filename = whichFilename(Constants.WEKA_RESULTS,projName,Constants.CSV_EXT);
		
		try (FileWriter csvWriter = new FileWriter(filename)){
			
			csvWriter.append("Num Training Releases");
			csvWriter.append(",");
			csvWriter.append("% Training");
			csvWriter.append(",");
			csvWriter.append("% Defective In Training");
			csvWriter.append(",");
			csvWriter.append("% Defective In Testing");
			csvWriter.append(",");
			csvWriter.append("Classifier");
			csvWriter.append(",");
			csvWriter.append("Balancing");
			csvWriter.append(",");
			csvWriter.append("Feature Selection");
			csvWriter.append(",");
			csvWriter.append("TP");
			csvWriter.append(",");
			csvWriter.append("FP");
			csvWriter.append(",");
			csvWriter.append("TN");
			csvWriter.append(",");
			csvWriter.append("FN");
			csvWriter.append(",");
			csvWriter.append("Precision");
			csvWriter.append(",");
			csvWriter.append("Recall");
			csvWriter.append(",");
			csvWriter.append("ROC Area");
			csvWriter.append(",");
			csvWriter.append("Kappa");
			csvWriter.append("\n");
					
			for (ClassifierAnalysis entry : classifierAnalysis) {
				csvWriter.append(String.valueOf(entry.getNumTrainingReleases()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getPercentageTraining()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getPercDefectiveInTraining()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getPercDefectiveInTesting()));
				csvWriter.append(",");
				csvWriter.append(entry.getClassifierName());
				csvWriter.append(",");
				csvWriter.append(entry.getBalancing());
				csvWriter.append(",");
				csvWriter.append(entry.getFeatureSelection());
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getTruePositive()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getFalsePositive()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getTrueNegative()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getFalseNegative()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getPrecision()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getRecall()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getRocArea()));
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getKappa()));
		        csvWriter.append("\n");

			}
		}
		
	}

}
