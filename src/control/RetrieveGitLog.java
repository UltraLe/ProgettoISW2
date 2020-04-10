package control;

import java.awt.List;
import java.io.BufferedReader;
import java.io.FileWriter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public class RetrieveGitLog {
	
	//Project name used on GitHub
	public static final String GIT_PROJ_NAME = "incubator-daffodil";
	
	//this token can be public because it has only read permission
	//but it has to be obscured due to github policies
	private static final String GIT_TKN = "e 6 e 9 8 0 3 6 a 3 6 c 5 f 6 9 5 0 5 6 b c b 6 6 f 5 5 5 d e 6 6 c 1 0 c f 3 d";
	
	public static final String CSV_FILENAME ="commitsPerMonth.csv";
	public static final String LOG_FILE = "log.txt";
	
	//GITHUB REST API to retrieve the commit with given (%s to specify later on) ticket ID
	//sorted by committer date (from latest to earlier)
	public static final String GIT_API_URL = "https://api.github.com/search/commits?q=repo:apache/"+GIT_PROJ_NAME+"+\"%s\"+sort:committer-date";
	   
	private static final Logger LOGGER = Logger.getLogger(RetrieveGitLog.class.getName());
	
	private RetrieveGitLog() {
		throw new IllegalStateException("Utility class");
		}
	
	private static String extractTkn(String tkn) {
		
		StringBuilder clearTkn = new StringBuilder();
		String[] t = tkn.split(" ");
		
		for (String pieceTkn : t) {
			clearTkn.append(pieceTkn);
		}
		
		return clearTkn.toString();
	}
	
	private static Date addMonth(Date date, int i) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, i);
        return cal.getTime();
    }
	
	//method that has to take a map of string and integer
	//and write on a csv file how the integer corresponding to each string
	private static void writeCSVfile(Map<Date, Integer> commitsMap) throws IOException{
		
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
					LOGGER.log(Level.SEVERE,"Something went wrong in date comparison");
				}
				//add 1 month to first date
				currentDate = addMonth(currentDate, 1);
			}
			
			//add 1 month to first date
			currentDate = addMonth(currentDate, 1);
		}
		
		sortedMap.putAll(tempMap);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
		
		try (FileWriter csvWriter = new FileWriter(CSV_FILENAME)){
			
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
	
	//given a list of (JIRA) tickets, this method will return
	private static void gitLog(List ticketsID) throws IOException, InterruptedException, ParseException {
		
		HttpURLConnection con = null;
		StringBuilder response = new StringBuilder();
		String nextUrl;
		
		Map<Date, Integer> commitsMap = new HashMap<>();
		int ticketsNum = ticketsID.getItemCount();
		
		String gitTkn = extractTkn(GIT_TKN);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
		
		int total = 1;
		for(String ticketID : ticketsID.getItems()) {
			
			nextUrl = String.format(GIT_API_URL, ticketID);
			//HTTP GET request
			URL url = new URL(nextUrl);
			
			if(total%29 == 0) {
				//are permitted 30 search queries each 60 seconds
				//sleeping more than needed to make sure that 
				//timer has been reset
				LOGGER.log(Level.INFO,"Tokens read: {0}",String.valueOf(total));
				//26 tickets per minute are searched
				LOGGER.log(Level.INFO,"Minutes left: {0}",String.valueOf((ticketsNum-total)/25));
				writeCSVfile(commitsMap);
				Thread.sleep(70000);
			}
			
			con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
			//adding token to avoid rate limitation
			//this token can be public because it has only read permission
			con.setRequestProperty("Authorization", "token "+gitTkn);
			con.setRequestMethod("GET");
			
			try(BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			
				//reading response
				String inputLine;
				
				while((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				
				JSONObject jsonResult = new JSONObject(response.toString());
				
				//if i get NO results from the query, skip the current ticket ID
				if(jsonResult.getInt("total_count") == 0) {
					total++;
					response = new StringBuilder();
					continue;
				}
				
				//otherwise...
				JSONArray items = jsonResult.getJSONArray("items");
				String dateString = (((items.getJSONObject(0)).getJSONObject("commit")).getJSONObject("committer")).getString("date").substring(0, 7);
				
				Date date = dateFormat.parse(dateString);
				
				if(commitsMap.get(date) == null) {
					commitsMap.put(date, 1);
				}else {
					commitsMap.put(date, commitsMap.get(date)+1);
				}
				
				total++;
				response = new StringBuilder();
		
			}finally{
				con.disconnect();
			}
		
		}
		
		//writing into csv file
		writeCSVfile(commitsMap);
		
	}
	   
	public static void main(String[] args) throws IOException{
		
		try {
			//setting up the logger
			Handler fileHandler = new FileHandler(LOG_FILE);
			LOGGER.addHandler(fileHandler);
			List tickets = RetrieveTicketsID.retriveTicket();
			RetrieveGitLog.gitLog(tickets);
		}catch(Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
		
	}
	
	

}
