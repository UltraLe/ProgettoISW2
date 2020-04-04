package control;

import java.awt.List;
import java.io.BufferedReader;
import java.io.FileWriter;
//TODO
//import java.io.Console; <- use this to log messages
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class RetrieveGitLog {
	
	//Project name used on GitHub
	public static final String GIT_PROJ_NAME = "incubator-daffodil";
	
	//this token can be public because it has only read permission
	private static final String GIT_TKN = "7bf3a5c0e57a961e4e1303055e5871cc648b3767";
	
	public static final String CSV_FILENAME ="commitsPerMonth.csv";
	
	//GITHUB REST API to retrieve the commit with given (%s to specify later on) ticket ID
	//sorted by committer date (from latest to earlier)
	public static final String GIT_API_URL = "https://api.github.com/search/commits?q=repo:apache/"+GIT_PROJ_NAME+"+%s+sort:committer-date";
	   
	private RetrieveGitLog() {
		throw new IllegalStateException("Utility class");
		}
	
	//method that has to take a map of string and integer
	//and write on a csv file how the integer corresponding to each string
	private static void writeCSVfile(Map<String, Integer> commitsMap) throws IOException{
		
		FileWriter csvWriter = new FileWriter(CSV_FILENAME);
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

		csvWriter.flush();
		csvWriter.close();
		
	}
	
	//given a list of (JIRA) tickets, this method will return
	private static void gitLog(List ticketsID) throws IOException, MalformedURLException, IOException, InterruptedException {
		
		HttpURLConnection con = null;
		BufferedReader in = null;
		StringBuffer response = new StringBuffer();
		String nextUrl;
		Map<String, Integer> commitsMap = new HashMap<String, Integer>();
		
		try {
			int total = 1;
			for(String ticketID : ticketsID.getItems()) {
				
				if(total%29 == 0) {
					//are permitted 30 search queries each 60 seconds
					//sleeping more than needed to make sure that 
					//timer has been reset
					Thread.sleep(70000);
				}
				
				nextUrl = String.format(GIT_API_URL, ticketID);
				//HTTP GET request
				URL url = new URL(nextUrl);
				con = (HttpURLConnection) url.openConnection();
				
				con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
				
				//adding token to avoid rate limitation
				//this token can be public because it has only read permission
				con.setRequestProperty("Authorization", "token "+GIT_TKN);
				
				con.setRequestMethod("GET");
				
				//reading response
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				
				while((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				
				JSONObject jsonResult = new JSONObject(response.toString());
				
				//if i get NO results from the query, skip the current ticket ID
				if(jsonResult.getInt("total_count") == 0) {
					total++;
					response = new StringBuffer();
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
				response = new StringBuffer();
			}
		
		}finally{
			in.close();
			con.disconnect();
		}
		
		//writing into csv file
		writeCSVfile(commitsMap);
		
		
	}
	   
	public static void main(String[] args){
		
		try {
			List tickets = RetrieveTicketsID.retriveTicket();
			RetrieveGitLog.gitLog(tickets);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	

}
