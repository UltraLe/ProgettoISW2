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
	
	private static String gitTkn = null;
	
	private GitInteractor() {
		throw new IllegalStateException("Utility class");
	}
	
	public static void extractTkn() {
		
		StringBuilder clearTkn = new StringBuilder();
		String[] t = Constants.GIT_TKN.split(" ");
		
		for (String pieceTkn : t) {
			clearTkn.append(pieceTkn);
		}
		
		gitTkn = clearTkn.toString();
	}
	
	private static void limitRequest(int total, int ticketsNum, HttpURLConnection con) 
									throws IOException, InterruptedException {
		if(total%29 == 0) {
			//are permitted 30 search queries each 60 seconds
			//sleeping more than needed to make sure that 
			//timer has been reset
			Constants.LOGGER.log(Level.INFO,"Tokens read: {0}",String.valueOf(total));
			//26 tickets per minute are searched
			if(Constants.TKT_SEARCH_FAST) {
				Constants.LOGGER.log(Level.INFO,"Minutes left: {0}",String.valueOf((ticketsNum-total)/25));
			}
			//if fast ticket search is not enabled, minutes left
			//are not predictable
			
			Thread.sleep(70000);
		}
		
		con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
		//adding token to avoid rate limitation
		//this token can be public because it has only read permission
		con.setRequestProperty(Constants.AUTORIZATION, Constants.TOKEN+gitTkn);
		con.setRequestMethod("GET");
		
	}
	
	private static void readResponse(HttpURLConnection con, StringBuilder response) throws IOException {
		
		try(BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			//reading response
			String inputLine;
			while((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		}finally{
			con.disconnect();
		}
	}
	
	//function that retrieve GIT commits given (JIRA) ticket information, json format (jsonResult)
	//and retries to perform the search if.... see the comment below
	private static List<Object> ticketInfoJson(String ticketID, boolean retrying, int total, int ticketsNum, int retried,
										List<List<String>> classesName) throws IOException, InterruptedException {
		
		String nextUrl;
		HttpURLConnection con = null;
		StringBuilder response = new StringBuilder();
		boolean found = false;
		JSONObject jsonResult;
		
		List<Object> objToReturn = new ArrayList<>();
		
		do {
		
			if(retrying) {
				ticketID = ticketID.substring(Constants.getJiraProjName().length());
				//ticketID will be like '-1234'
			}
			
			nextUrl = String.format(Constants.getSearchTktLastCommitUrl(), ticketID);
			
			//HTTP GET request
			URL url = new URL(nextUrl);
			con = (HttpURLConnection) url.openConnection();
			//method that limits the number of requests per seconds
			limitRequest(total, ticketsNum, con);
			total++;
			readResponse(con, response);
			
			jsonResult = new JSONObject(response.toString());
			
			//if i get NO results from the query, skip the current ticket ID
			if((jsonResult.getInt("total_count") == 0) && (!Constants.TKT_SEARCH_FAST && !retrying)) {
				
				//if we want to retry finding the ticket
				//using only the ID
				retried++;
				retrying = true;
				response = new StringBuilder();
				found = false;
				
			}else if(jsonResult.getInt("total_count") == 0) {
				retrying = false;
				classesName.add(new ArrayList<>());
				response = new StringBuilder();
				found = false;
			}else {
				retrying = false;
				found = true;
			}		
			
		}while(retrying);
		
		objToReturn.add(found);
		objToReturn.add(total);
		objToReturn.add(jsonResult);
		
		return objToReturn;
	}
	
	
	//given a list of (JIRA) tickets, this method will look for commit(s) in Json format,
	//that matches the information passed as parameter.
	//Eg. if info is COMMINTS_MONTH, there will be searched all the commits that matches
	//    the specification as defined in the Deriverable 1 
	public static Object getGitInfo(List<String> ticketsID, String info) throws IOException, InterruptedException, ParseException {
		
		Map<Date, Integer> commitsMap = new HashMap<>();
		int ticketsNum = ticketsID.size();
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
		
		List<List<String>> classesName = new ArrayList<>();
		
		List<Object> tktInfoResult;
		JSONObject jsonResult = null;
		
		String ticketID;
		int total = 1;
		int retried = 0;
		boolean retrying = false;
		boolean found = false;
		
		for(int i = 0; i < ticketsID.size(); ++i) {
			
				ticketID = ticketsID.get(i);
				tktInfoResult = ticketInfoJson(ticketID, retrying, total, ticketsNum, retried, classesName);
				found = (boolean) tktInfoResult.get(0);
				total = (int) tktInfoResult.get(1);
				jsonResult = (JSONObject) tktInfoResult.get(2);
				
				if(!found) {
					continue;
				}
				//the response
				@SuppressWarnings("null")
				JSONArray items = jsonResult.getJSONArray("items");
				
				//Here the method can retrieve the last commit date
				//associated to the ticked ID
				// OR the commit ID
				//that will we use in another method (that does not use search limited
				//github rest api) to get all the files edited in that commit.	
				if(info.compareTo(Constants.COMMIT_CLASS_NAME) == 0) {
					
					String commitSha = (items.getJSONObject(0)).getString("sha");
					classesName.add(getClassOfCommit(commitSha, "modified"));
					
				}else {
				
					//otherwise, get the date from the last commit associated to the ticket id
					String dateString = (((items.getJSONObject(0)).getJSONObject("commit")).getJSONObject("committer")).getString("date").substring(0, 7);
					
					Date date = dateFormat.parse(dateString);
					
					if(commitsMap.get(date) == null) {
						commitsMap.put(date, 1);
					}else {
						commitsMap.put(date, commitsMap.get(date)+1);
					}
				}		
		}
		
		if(info.compareTo(Constants.COMMIT_CLASS_NAME) == 0) {
			return classesName;
		}
		
		//otherwise
		//writing into csv file
		CsvFileWriter.bugsPerMonthCSV(commitsMap, Constants.getJiraProjName());
		return null;
		
	}
	
	
	//method that given 2 dates and a page, returns a JSONArray which contains
	//all the commits in that time interval
	public static JSONArray getCommitsInTimeInterval(String since, String until, int page) throws IOException {
		
		String parameterFormat = "?since=%s&until=%s&page=%d";
		String parameter = String.format(parameterFormat, since, until, page);
		String stringUrl = Constants.getCommitInfoUrl().substring(0, Constants.getCommitInfoUrl().length()-3).concat(parameter);
		
		HttpURLConnection con = null;
		URL url = new URL(stringUrl);
		
		StringBuilder response = new StringBuilder();

		//use authenticated requests
		con = (HttpURLConnection) url.openConnection();
		//adding token to avoid rate limitation
		//this token can be public because it has only read permission
		con.setRequestProperty(Constants.AUTORIZATION, Constants.TOKEN+gitTkn);
		con.setRequestMethod("GET");
		
		readResponse(con, response);

		return new JSONArray(response.toString());
		
	}
	
	//retrieve LOC of a given file raw url
	public static int getFileLOC(String rawUrl) throws IOException {
		
		int loc = 0;
		
		HttpURLConnection con = null;
		URL url = new URL(rawUrl);

		//use authenticated requests
		con = (HttpURLConnection) url.openConnection();
		//adding token to avoid rate limitation
		//this token can be public because it has only read permission
		con.setRequestProperty(Constants.AUTORIZATION, Constants.TOKEN+gitTkn);
		con.setRequestMethod("GET");
		String line;
		try(BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			while((line = in.readLine()) != null) {
				if(line.length() > 2) {
				loc++;
				}
			}
		}finally{
			con.disconnect();
		}
		
		return loc;
	}
	
	
	//get commit haveing a given sha
	public static JSONObject getGitCommit(String sha) throws IOException {
		
		
		String stringUrl = String.format(Constants.getCommitInfoUrl(), sha);
		HttpURLConnection con = null;
		URL url = new URL(stringUrl);
		
		StringBuilder response = new StringBuilder();
		//use authenticated requests
		con = (HttpURLConnection) url.openConnection();
		//adding token to avoid rate limitation
		//this token can be public because it has only read permission
		con.setRequestProperty(Constants.AUTORIZATION, Constants.TOKEN+gitTkn);
		con.setRequestMethod("GET");
		
		readResponse(con, response);
		
		return  new JSONObject(response.toString());
	}
	
	
	//get classes of a commit with a given status,
	//if status = null, take all
	private static List<String> getClassOfCommit(String commitSha, String status) throws JSONException, IOException{
		
		JSONObject jsonResult = getGitCommit(commitSha);
		JSONArray files = jsonResult.getJSONArray("files");
		
		List<String> classes = new ArrayList<>();
		for(int i = 0; i < files.length(); ++i) {
			
			String fileName = files.getJSONObject(i).getString("filename");
			
			//If the file is not a source code artifact, skip it
			if(!fileName.contains(Constants.PROG_LANG_EXT)) {
				continue;
			}
			
			//otherwise add them only if status matches the requested one
			if(status.compareTo(files.getJSONObject(i).getString("status")) == 0 || status.compareTo("any") == 0) {
				classes.add(fileName);
			}
			
		}
		
		return classes;
	}
	   
	public static void getBugsPerMonth(){
		
		try {
			//setting up the logger
			Handler fileHandler = new FileHandler(Constants.LOG_FILE);
			Constants.LOGGER.addHandler(fileHandler);
			List<String> tickets = RetrieveTicketsID.retriveTicket(Constants.getJiraProjName());
			GitInteractor.extractTkn();
			GitInteractor.getGitInfo(tickets, Constants.DATE);
		}catch(Exception e) {
			Constants.LOGGER.log(Level.SEVERE, e.getMessage());
		}
		
	}
	
	

}
