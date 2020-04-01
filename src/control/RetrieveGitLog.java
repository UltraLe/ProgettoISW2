package control;

import java.awt.List;
import java.io.BufferedReader;
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
	
	//GITHUB REST API to retrieve the commit with given (%s to specify later on) ticket ID
	//sorted by committer date (from latest to earlier)
	public static final String GIT_API_URL = "https://api.github.com/search/commits?q=repo:apache/"+GIT_PROJ_NAME+"+%s+sort:committer-date";
	   
	private RetrieveGitLog() {
		throw new IllegalStateException("Utility class");
		}
	
	//method that has to take a map of string and integer
	//and write on a csv file how the integer corresponding to each string
	//TODO
	private static void writeCSVfile(Map<String, Integer> commitsMap) {
		
	}
	
	//given a list of (JIRA) tickets, this method will return
	private static void gitLog(List ticketsID) throws IOException, MalformedURLException, IOException {
		
		HttpURLConnection con = null;
		BufferedReader in = null;
		StringBuffer response = new StringBuffer();
		String nextUrl;
		
		Map<String, Integer> commitsMap = new HashMap<String, Integer>();
		
		try {
			
			for(String ticketID : ticketsID.getItems()) {
				
				nextUrl = String.format(GIT_API_URL, ticketID);
				
				//System.out.println(nextUrl);
				
				//HTTP GET request
				//TODO add Accept thing...
				URL url = new URL(nextUrl);
				con = (HttpURLConnection) url.openConnection();
				
				con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
				
				con.setRequestMethod("GET");
				
				System.out.println("HERE");
				
				//reading response
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				
				while((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				
				JSONObject jsonResult = new JSONObject(response.toString());
				
				//if i get NO results from the query, skip the current ticket ID
				if(jsonResult.getInt("total_count") == 0) {
					continue;
				}
				
				//otherwise...
				JSONArray items = jsonResult.getJSONArray("items");
				
				//now take the first item which is the latest..
				String date = (((items.getJSONObject(0)).getJSONObject("commit")).getJSONObject("committer")).getString("date");
				
				if(commitsMap.get(date) == null) {
					commitsMap.put(date, 1);
				}else {
					commitsMap.put(date, commitsMap.get(date)+1);
				}
				
			}
		
		}finally{
			//in.close();
			con.disconnect();
		}
		
		//printing results
		for (Map.Entry<String, Integer> entry : commitsMap.entrySet()) {
	        System.out.println(entry.getKey() + ":" + entry.getValue());
		}
		
		//writing into csv file
		writeCSVfile(commitsMap);
		
		
	}
	   
	public static void main(String[] args){
		
		List l = new List();
		l.add("DAFFODIL-1034");
		
		try {
			RetrieveGitLog.gitLog(l);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	

}
