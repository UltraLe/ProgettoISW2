package control;

import java.util.List;
import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Constants;

public class GitInteractor {
	
	private static String gitTkn;
	
	private GitInteractor() {
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
	
	
	
	//given a list of (JIRA) tickets, this method will return
	public static Object gitInfo(List<String> ticketsID, String info) throws IOException, InterruptedException, ParseException {
		
		HttpURLConnection con = null;
		StringBuilder response = new StringBuilder();
		String nextUrl;
		
		Map<Date, Integer> commitsMap = new HashMap<>();
		int ticketsNum = ticketsID.size();
		
		gitTkn = extractTkn(Constants.GIT_TKN);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
		
		List<List<String>> classesName = new ArrayList<>();
		
		JSONObject jsonResult = null;
		
		String ticketID;
		int total = 1;
		int retried = 0;
		boolean retrying = false;
		boolean found = false;
		
		for(int i = 0; i < ticketsID.size(); ++i) {
			
				do {
					ticketID = ticketsID.get(i);
					
					if(retrying) {
						ticketID = ticketID.substring(Constants.JIRA_PROJ_NAME.length());
						//ticketID will be like '-1234'
					}
					
					nextUrl = String.format(Constants.SEARCHTKT_LASTCOMMIT_URL, ticketID);
					
					//HTTP GET request
					URL url = new URL(nextUrl);
					
					if(total%29 == 0) {
						//are permitted 30 search queries each 60 seconds
						//sleeping more than needed to make sure that 
						//timer has been reset
						Constants.LOGGER.log(Level.INFO,"Tokens read: {0}",String.valueOf(total));
						//26 tickets per minute are searched
						Constants.LOGGER.log(Level.INFO,"Minutes left: {0}",String.valueOf((ticketsNum-(total-retried))/25));
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
					}finally{
						con.disconnect();
					}
					
					jsonResult = new JSONObject(response.toString());
					
					//if i get NO results from the query, skip the current ticket ID
					if((jsonResult.getInt("total_count") == 0) && (!Constants.TKT_SEARCH_ACCURATE && !retrying)) {
							
						//if we want to retry finding the ticket
						//using only the ID
						total++;
						retried++;
						retrying = true;
						response = new StringBuilder();
						found = false;
						
					}else if(jsonResult.getInt("total_count") == 0) {
						retrying = false;
						classesName.add(new ArrayList<>());
						total++;
						response = new StringBuilder();
						found = false;
					}else {
						found = true;
					}
					
				}while(retrying);
				
				if(!found) {
					continue;
				}
				retrying = false;
				
				//the response
				JSONArray items = jsonResult.getJSONArray("items");
				
				//Here the method can retrieve the last commit date
				//associated to the ticked id, or the commit ID
				//that will we used in another method (that does not use search limited
				//github rest api) to get all the files edited in that commit.	
				if(info.compareTo(Constants.COMMIT_CLASS_NAME) == 0) {
					
					String commitSha = (items.getJSONObject(0)).getString("sha");
					classesName.add(commitClasses(commitSha, "modified"));
					total++;
					response = new StringBuilder();
					
				}else {
				
					//otherwise, get the date from the last commit associated to the ticket id
					String dateString = (((items.getJSONObject(0)).getJSONObject("commit")).getJSONObject("committer")).getString("date").substring(0, 7);
					
					Date date = dateFormat.parse(dateString);
					
					if(commitsMap.get(date) == null) {
						commitsMap.put(date, 1);
					}else {
						commitsMap.put(date, commitsMap.get(date)+1);
					}
					
					total++;
					response = new StringBuilder();
				}		
		}
		
		if(info.compareTo(Constants.COMMIT_CLASS_NAME) == 0) {
			return classesName;
		}
		
		//otherwise
		//writing into csv file
		CsvFileWriter.monthCommitsCSV(commitsMap);
		return null;
		
	}
	
	//get classes of a commit with a given status,
	//if status = null, take all
	private static List<String> commitClasses(String commitSha, String status) throws JSONException, IOException{
		
		List<String> classes = new ArrayList<>();
		String stringUrl = String.format(Constants.COMMITINFO_URL, commitSha);
		HttpURLConnection con = null;
		URL url = new URL(stringUrl);
		
		StringBuilder response = new StringBuilder();
		JSONObject jsonResult;
		//use authenticated requests
		con = (HttpURLConnection) url.openConnection();
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
			
			jsonResult = new JSONObject(response.toString());
			
		}finally {
			con.disconnect();
		}
		
		JSONArray files = jsonResult.getJSONArray("files");
		
		for(int i = 0; i < files.length(); ++i) {
			
			//if status is null add the classes
			if(status == null) {
				classes.add(files.getJSONObject(i).getString("filename"));
				continue;
			}
			//otherwise add them only if status matches the requested one
			if(status.compareTo(files.getJSONObject(i).getString("status")) == 0) {
				classes.add(files.getJSONObject(i).getString("filename"));
			}
			
		}
		
		return classes;
	}
	   
	public static void main(String[] args) throws IOException{
		
		try {
			//setting up the logger
			Handler fileHandler = new FileHandler(Constants.LOG_FILE);
			Constants.LOGGER.addHandler(fileHandler);
			List<String> tickets = RetrieveTicketsID.retriveTicket(Constants.JIRA_PROJ_NAME);
			GitInteractor.gitInfo(tickets, Constants.DATE);
		}catch(Exception e) {
			Constants.LOGGER.log(Level.SEVERE, e.getMessage());
		}
		
	}
	
	

}
