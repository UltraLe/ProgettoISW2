package control;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class Buggy {
	
	private static final Logger LOGGER = Logger.getLogger(Buggy.class.getName());
	public static final String LOG_FILE = "buggyLog.txt";
	
	private HashMap<Integer, List<Object>> indexVersionDate;
	private int maxVersionIndex;
	private String projName;
	
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
		this.indexVersionDate = GetReleaseInfo.getIndexOfVersions(this.projName);
		this.maxVersionIndex = GetReleaseInfo.lastIndexOfVersionAalyzable;
	}
	
	//The first field to check is list[2], if not null, 
	//then AV was provided, else, calculate AV using Proportion
	private List<Integer> getAffectedVersion(String ticket) throws JSONException, IOException {
		String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"+this.projName+
						"%22AND%22issueType%22=%22Bug%22AND%22id%22=%22"+ticket+"%22";
        JSONObject json = GetReleaseInfo.readJsonFromUrl(url);
        //test on this:
        //https://issues.apache.org/jira/rest/api/2/search?jql=project=%22DAFFODIL%22AND%22issueType%22=%22Bug%22AND%22id%22=%22DAFFODIL-2302%22
     
        //if the ticket has the AV, i'm done.
        
        //otherwise extract OV from first date.
        //Extract FV
        //calculate predicted IV = ...
        //calculate AV =[IV, FV) by using indexVersionDate
        //done.

	}
	
	//from a ' bug fixed' tickect, given the time when it was created,
	//the "opening time", and extract the opening version from this time
	//using the hash map indexVersionDate
	private int getOpeningVersion(LocalDateTime tktCreationTime) {
		return 0;
	}
	
	private int getAffectedVersions(JSONObject jiraInfo) {
		//could be integrated in getAffectedVersion
		return 0;
	}
	
	
	public static void main(String[] argv) throws JSONException, IOException {
		
		List tickets = (List) RetrieveTicketsID.retriveTicket(Buggy.projName);
		
		//test first
		
		
	}
	
}
