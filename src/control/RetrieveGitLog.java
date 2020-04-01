package control;

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
	//GIT REST API to retrieve the informations off all commits
	public static final String GIT_API_URL = "https://api.github.com/repos/apache/"+GIT_PROJ_NAME+"/commits?page=";
	//110 is the last page
	
	//ticket template
	private static final String TICKET_ID_TEMPLATE = "DAFFODIL-";
	   
	private RetrieveGitLog() {
		throw new IllegalStateException("Utility class");
		}
	
	private static void gitLog() throws IOException, MalformedURLException, IOException {
		
		HttpURLConnection con = null;
		BufferedReader in = null;
		StringBuffer response = new StringBuffer();
		String nextUrl;
		
		Map<String, String> commitsMap = new HashMap<String, String>();
		
		int page = 1;
		
		try {
		
			JSONArray jsonPartialCommits;
			
			do {
				
				nextUrl = GIT_API_URL.concat(String.valueOf(page));
				
				//System.out.println(nextUrl);
				
				//HTTP GET request
				URL url = new URL(nextUrl);
				con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				
				//reading response
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				
				while((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				
				jsonPartialCommits = new JSONArray(response.toString());
				
				for(int i = 0; i < jsonPartialCommits.length(); ++i) {
					
					JSONObject jsonSingleCommit = jsonPartialCommits.getJSONObject(i);
					String commitMessage = (jsonSingleCommit.getJSONObject("commit")).getString("message");				
					String messageDate = ((jsonSingleCommit.getJSONObject("commit")).getJSONObject("committer")).getString("date");
					
					//verifying that message contains Ticket ID
					int startIndx = commitMessage.indexOf(TICKET_ID_TEMPLATE);
					if(startIndx != -1) {
						//if it does then get the ticked ID
						//and put all in the commit hash map,
						//this will make easier the search operation
						//of the last commit of a ticket ID
						StringBuffer ticketID = new StringBuffer();
						String[] charsOfTicketId = commitMessage.substring(startIndx+TICKET_ID_TEMPLATE.length()).split("");
						
						for(int j = 0; j < charsOfTicketId.length; ++j) {
							if(charsOfTicketId[j].matches("[0-9]+")) {
								ticketID.append(charsOfTicketId[j]);
							}
						}
						
						commitsMap.put(TICKET_ID_TEMPLATE.concat(ticketID.toString()), messageDate);
					}
				}
				
				page++;
				
				//TODO FIX RATE LIMITING REQUESTS
				//fot now, just test with 3 pages
			}while(jsonPartialCommits.length()>0 || page < 3);
		
		}finally{
			in.close();
			con.disconnect();
		}
		
		//printing results
		for (Map.Entry<String, String> entry : commitsMap.entrySet()) {
	        System.out.println(entry.getKey() + ":" + entry.getValue());
		}
		
	}
	   
	public static void main(String[] args) throws Exception{
		
		RetrieveGitLog.gitLog();

		/*
		String test = "ciaociao sadh k DAFFODIL-123dsf";
		
		String[] n = test.substring(test.indexOf("DAFFODIL-")).split("");
		StringBuffer f = new StringBuffer();
		for(int i = 0; i < n.length; ++i) {
			if(n[i].matches("[0-9]+")) {
				f.append(n[i]);
			}
		}
		System.out.println("DAFFODIL-"+ f.toString());
		*/
	}
	
	

}
