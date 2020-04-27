package control;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import entity.AnalyzedFile;
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
	
	private static String whichFilename(String name, String projName, String ext) {
		
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

}
