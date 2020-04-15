package control;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.AnalyzedClass;
import entity.Constants;

public class Buggy {
	
	private static final Logger LOGGER = Logger.getLogger(Buggy.class.getName());
	public static final String LOG_FILE = "buggyLog.txt";
	
	//the returned Has ha Key = release name and
	//value is a List where:
	//List[0] = release index
	//List[1] = release date
	private HashMap<String, List<Object>> releaseIndexDate;
	private TreeMap<Integer, LocalDate> indexDate;
	private int maxReleaseIndex;
	private String projName;
	
	private double P;
	
	public Buggy(String projName) {
		this.projName = projName;
		//setting up logger
		Handler fileHandler;
		try {
			fileHandler = new FileHandler(LOG_FILE);
			LOGGER.addHandler(fileHandler);
			this.setup();
		} catch (SecurityException | IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}	
	}
	
	private void setup() throws JSONException, IOException {
		
		this.releaseIndexDate = GetReleaseInfo.getIndexOfVersions(this.projName);
		this.maxReleaseIndex = GetReleaseInfo.lastIndexOfVersionAalyzable;
		
		//creating a tree map that will be useful to retrieve the index
		//corresponding to the date of the opening version of a ticket
		this.indexDate = new TreeMap<>();
		for(Map.Entry<String, List<Object>> entry : this.releaseIndexDate.entrySet()) {
			this.indexDate.put((Integer)entry.getValue().get(0), ((LocalDateTime)entry.getValue().get(1)).toLocalDate());
		}
		
		this.proportionMovingWindow();
		System.out.println("Finisched Moving Window, p: "+this.P);
		LOGGER.log(Level.INFO, "Moving Window (proportion) P value: {0}",String.valueOf(this.P));
	} 
	
	private int retrieveOpeningVersion(String OVdate) {
		
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate openingVersionDate = LocalDate.parse(OVdate, formatter);
        
        int indexOV = 1;
        for(Map.Entry<Integer, LocalDate> entry : indexDate.entrySet()) {
        	
        	if(openingVersionDate.compareTo(entry.getValue()) > 0) {
        		//if i am here i found the corresponding index
        		continue;
        	}
        	indexOV = entry.getKey();
        	break;
        }
        
        return indexOV;
	}
	
	//this function is called when the class is created and 
	//is used to calculate the parameter P.
	private void proportionMovingWindow() throws JSONException, IOException {
		
		int startAt = 0;
		int maxResults = 300;
		int total = 0;
		int count = 0;
		int ticketWithAV = 0;
		double partialP = 0;
		double percentage = 0.1;
		
		do {
			//given the project, i have to extract all the bug fixed
			String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"+this.projName
					+"%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22Resolved%22OR%22status%22=%22Closed%22)AND%22resolution%22=%22Fixed%22&startAt="+
					startAt+"&maxResults="+maxResults;
			JSONObject json = GetReleaseInfo.readJsonFromUrl(url);
			JSONArray tickets = json.getJSONArray("issues");
			
			//counting total tickets to work with
			total = json.getInt("total");
			//now for each ticket i have to calculate P = (FV-IV)/(FV-OV)
			int FV, IV, OV;
			
			for(int i = 0; i < tickets.length(); ++i) {
				//if the ticket does not have AV, skip,
				JSONArray affectedVersions = ((tickets.getJSONObject(i)).getJSONObject("fields")).getJSONArray("versions"); 

				if(affectedVersions.length() > 0) {
					
					List<Integer> indexesAVs = new ArrayList<Integer>();
		        	for(int j = 0; j < affectedVersions.length(); ++j) {
		        		//this is the name of the version
		        		String versName = affectedVersions.getJSONObject(j).getString("name");
		        		if(releaseIndexDate.get(versName) != null) {
		        			indexesAVs.add((Integer)releaseIndexDate.get(versName).get(0));
			        	}else {
			        		continue;
			        	}
		        		
		        	}
		        	
		        	//it may happen that the fix version has not been released yet
		        	if(!indexesAVs.isEmpty()) {
		        		Collections.sort(indexesAVs);
			        	IV = indexesAVs.get(0);
		        	}else {
		        		continue;
		        	}
		        	
		        	//now extracting FV
		        	JSONArray fixVersion = ((((tickets.getJSONObject(i)).getJSONObject("fields")).getJSONArray("fixVersions")));
		        	//we may not have specified av
		        	if(fixVersion.length() > 0) {
		        		//the version may not have been released yet
		        		if(releaseIndexDate.get(fixVersion.getJSONObject(0).getString("name")) != null) {
		        			FV = (Integer) releaseIndexDate.get(fixVersion.getJSONObject(0).getString("name")).get(0);
		        		}else {
		        			continue;
		        		}
		        	}else {
		        		continue;
		        	}
		            
		            //and extracting OV
		        	String OVdate = (((tickets.getJSONObject(i)).getJSONObject("fields")).getString("created")).substring(0, 10);
		            OV = retrieveOpeningVersion(OVdate);
		            //it may happen that FV = OV, in this case
		            //we ignore the result
		            if(FV == OV) {
		            	continue;
		            }
		            partialP = partialP + (FV-IV)/(FV-OV);
		            ticketWithAV++;
		        	
				}
				
			}
			
			count += tickets.length();
			
			//reading next 1000 tickets, if any
			startAt += maxResults;
			maxResults += maxResults;
		}while(ticketWithAV < total*percentage && count < total);
		
		this.P = partialP/total;
	}
	
	//This method return the AV of a ticket if provided or by using
	//proportion method, or null if the ticket was created in the second half life
	//of the project.
	private List<Integer> getAffectedVersion(String ticket) throws JSONException, IOException {
		
		String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"+this.projName+
						"%22AND%22issueType%22=%22Bug%22AND%22id%22=%22"+ticket+"%22";
        JSONObject json = GetReleaseInfo.readJsonFromUrl(url);
        List<Integer> indexesAVs = new ArrayList<Integer>();  
        
        //date be like yyyy-mm-dd
        String OVdate = ((((json.getJSONArray("issues")).getJSONObject(0)).getJSONObject("fields")).getString("created")).substring(0, 10);
        int indexOV = retrieveOpeningVersion(OVdate);
        
        if(indexOV > this.maxReleaseIndex) {
        	return null;
        }
        
        JSONArray affectedVersions = (((json.getJSONArray("issues")).getJSONObject(0)).getJSONObject("fields")).getJSONArray("versions");  
    
        if(affectedVersions.length() > 0) {
        	for(int i = 0; i < affectedVersions.length(); ++i) {
        		//this is the name of the version
        		String versName = affectedVersions.getJSONObject(i).getString("name");
        		indexesAVs.add((Integer)releaseIndexDate.get(versName).get(0));
        	
        	}
        	return indexesAVs;
        }
        
        //otherwise extract OV from first date and extract FV
        //calculate predicted IV=FV-(FV-OV)*P, and return AV = [IV, FV)
        String fixVersion = (((((json.getJSONArray("issues")).getJSONObject(0)).getJSONObject("fields")).getJSONArray("fixVersions")).getJSONObject(0)).getString("name");
        //it may happen that the fix version refers to a release that has not been released yet
        //in this case ignore the ticket. This should not happen because
        //it will be considered the first half life of the project.	
        if(releaseIndexDate.get(fixVersion) == null) {
        	return null;
        }
        int indexFV = (Integer) releaseIndexDate.get(fixVersion).get(0);
        
        int predictedIV = (int)(indexFV-(indexFV-indexOV)*P);
        //AV = [predictedIV, FV)
        for(int i = predictedIV; i < indexFV; ++i) {
        	indexesAVs.add(i);
        }
        
        return indexesAVs;
	}
	
	//this method check if the ticket is 'analyzable':
    //the opening version has to be less then the maxReleaseIndex, which refers
    //to the maximum release index that can be analyze
	private List<String> getAnalyzableTickets(List<String> tickets) throws JSONException, IOException{

		List<String> analyzableTickets = new ArrayList<String>();
		
		for(String ticket: tickets) {
			
			String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"+this.projName+
					"%22AND%22issueType%22=%22Bug%22AND%22id%22=%22"+ticket+"%22";
			JSONObject json = GetReleaseInfo.readJsonFromUrl(url);
	
			//date be like yyyy-mm-dd
			String OVdate = ((((json.getJSONArray("issues")).getJSONObject(0)).getJSONObject("fields")).getString("created")).substring(0, 10);
			int indexOV = retrieveOpeningVersion(OVdate);
	
			if(indexOV > this.maxReleaseIndex) {
				continue;
			}
			
			analyzableTickets.add(ticket);
		}
		
		return analyzableTickets;
		
	}
	
	private void writeBuggyClasses(List<AnalyzedClass> classes) {
		
		
		
	}
	
	//thin method will find the buggy classes and for each class of the given project
	//it will be maintained the version in which is is buggy. 
	//The results will be written on a CSV file.
	public void getBuggyClasses() throws JSONException, IOException {
		
		List<AnalyzedClass> classes = new ArrayList<>();
		List<String> analyzableTickets = this.getAnalyzableTickets(RetrieveTicketsID.retriveTicket(this.projName));
		
		//retrieve the classes that has been modified by this ticket
		//edit getGitInfo in order to return something...
		List<List<String>> classesName = RetrieveGitLog.getGitInfo(analyzableTickets, Constants.COMMIT);
		
		List<List<Integer>> affectedVersions = new ArrayList<>();
		//retrieving the affected version of all tickets
		for(String tkt : analyzableTickets) {
			List<Integer> AVs = this.getAffectedVersion(tkt);
			affectedVersions.add(AVs);
		}
		
		//TODO check if classes name size = analyzableTickets and they refers to the same tickets
		
		//building classes
		for(int i = 0; i < classesName.size(); ++i) {
			
			//if the commit associated to the ticket id
			//was not found, just skip it
			if(classesName.get(i) == null) {
				continue;
			}
			
			
			for(int j = 0; j < classesName.get(i).size(); ++j) {
				//add to analyzed classes
				AnalyzedClass newClass = new AnalyzedClass(classesName.get(i).get(j));
				newClass.addBuggy(affectedVersions.get(i));
			}
		}
		
		writeBuggyClasses(classes);
	}
	
}
