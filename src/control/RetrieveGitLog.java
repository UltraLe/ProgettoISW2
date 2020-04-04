package control;

import java.awt.List;
import java.io.BufferedReader;
import java.io.FileWriter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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
	private static final String GIT_TKN = "9d0e5f2bee915688cb35faf5ba30dfb62b0b0c42";
	
	public static final String CSV_FILENAME ="commitsPerMonth.csv";
	public static final String LOG_FILE = "log.txt";
	
	//GITHUB REST API to retrieve the commit with given (%s to specify later on) ticket ID
	//sorted by committer date (from latest to earlier)
	public static final String GIT_API_URL = "https://api.github.com/search/commits?q=repo:apache/"+GIT_PROJ_NAME+"+%s+sort:committer-date";
	   
	private static final Logger LOGGER = Logger.getLogger(RetrieveGitLog.class.getName());
	
	private RetrieveGitLog() {
		throw new IllegalStateException("Utility class");
		}
	
	//method that has to take a map of string and integer
	//and write on a csv file how the integer corresponding to each string
	private static void writeCSVfile(Map<String, Integer> commitsMap) throws IOException{
		
		try (FileWriter csvWriter = new FileWriter(CSV_FILENAME)){
			
			csvWriter.append("Date");
			csvWriter.append(",");
			csvWriter.append("Commits");
			csvWriter.append("\n");
			
			for (Map.Entry<String, Integer> entry : commitsMap.entrySet()) {
				csvWriter.append(entry.getKey());
				csvWriter.append(",");
				csvWriter.append(String.valueOf(entry.getValue()));
		        csvWriter.append("\n");
			}

		}
		
		
	}
	
	//given a list of (JIRA) tickets, this method will return
	private static void gitLog(List ticketsID) throws IOException, InterruptedException {
		
		HttpURLConnection con = null;
		StringBuilder response = new StringBuilder();
		String nextUrl;
		Map<String, Integer> commitsMap = new HashMap<>();
		
		int total = 1;
		for(String ticketID : ticketsID.getItems()) {
			
			nextUrl = String.format(GIT_API_URL, ticketID);
			//HTTP GET request
			URL url = new URL(nextUrl);
			
			if(total%29 == 0) {
				//are permitted 30 search queries each 60 seconds
				//sleeping more than needed to make sure that 
				//timer has been reset
				LOGGER.log(Level.INFO,"Read {} tokens", total);
				Thread.sleep(70000);
			}
			
			con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
			//adding token to avoid rate limitation
			//this token can be public because it has only read permission
			con.setRequestProperty("Authorization", "token "+GIT_TKN);
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
				//now take the first item which is the latest..
				String date = (((items.getJSONObject(0)).getJSONObject("commit")).getJSONObject("committer")).getString("date").substring(0, 10);
				
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
