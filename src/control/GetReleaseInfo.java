package control;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.json.JSONException;
import org.json.JSONObject;

import entity.Constants;

import org.json.JSONArray;


public class GetReleaseInfo {
	
	public static Map<LocalDateTime, String> releaseNames;
	public static Map<LocalDateTime, String> releaseID;
	public static List<LocalDateTime> releases;
	public static int lastIndexOfVersionAalyzable = 0;
	
	private GetReleaseInfo() {
		throw new IllegalStateException("Utility class");
	}
	
	public static int getLastIndexOfVersionAalyzable() {
		return lastIndexOfVersionAalyzable;
	}

	//the returned Has ha Key = version name and
	//value is a List where:
	//List[0] = version index
	//List[1] = version date
	public static Map<String, List<Object>> getIndexOfVersions(String projName) throws IOException, JSONException {
		   
		   //Fills the arraylist with releases dates and orders them
		   //Ignores releases with missing dates
		   releases = new ArrayList<>();
		         Integer i;
		         String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
		         JSONObject json = readJsonFromUrl(url);
		         JSONArray versions = json.getJSONArray("versions");
		         releaseNames = new HashMap<>();
		         releaseID = new HashMap<>();
		         for (i = 0; i < versions.length(); i++ ) {
		            String name = "";
		            String id = "";
		            if(versions.getJSONObject(i).has("releaseDate")) {
		               if (versions.getJSONObject(i).has("name"))
		                  name = versions.getJSONObject(i).get("name").toString();
		               if (versions.getJSONObject(i).has("id"))
		                  id = versions.getJSONObject(i).get("id").toString();
		               addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
		                          name,id);
		            }
		         }
		         // order releases by date
		         Collections.sort(releases, (x, y) -> x.compareTo(y));
		         
		         //i need to calculate the 'half project life'
		         LocalDateTime first = releases.get(0);
		         LocalDateTime last = releases.get(releases.size()-1);
		        
		         LocalDateTime halfDate = first.plusMonths((last.getMonth().minus(last.getMonthValue()).getValue())/2);
		         halfDate = halfDate.plusDays((last.getDayOfMonth()-first.getDayOfMonth())/2);
		         halfDate = halfDate.plusYears((last.getYear()-first.getYear())/2);
		         
		         HashMap<String, List<Object>> indexesOfVersions = new HashMap<>();
		         for ( i = 0; i < releases.size(); i++) {
		               Integer index = i + 1;
		               List<Object> versionAndDate = new ArrayList<>();
		               versionAndDate.add(index);
		               versionAndDate.add(releases.get(i));
		               indexesOfVersions.put(releaseNames.get(releases.get(i)), versionAndDate);
		          
		               //if current version is 
		               if(releases.get(i).compareTo(halfDate) <= 0 && lastIndexOfVersionAalyzable <= index) {
		            	   lastIndexOfVersionAalyzable = index;
		               }
		         }
		         
		        return indexesOfVersions;
		   }
 
	
	   public static void addRelease(String strDate, String name, String id) {
		      LocalDate date = LocalDate.parse(strDate);
		      LocalDateTime dateTime = date.atStartOfDay();
		      if (!releases.contains(dateTime))
		         releases.add(dateTime);
		      releaseNames.put(dateTime, name);
		      releaseID.put(dateTime, id);
		   }


	   public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		   
	      InputStream is = new URL(url).openStream();
	      try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName(StandardCharsets.UTF_8.displayName())));){
	    	  
	         String jsonText = readAll(rd);
	         return new JSONObject(jsonText);
	       } finally {
	         is.close();
	       }
	   }
	   
	   private static String readAll(Reader rd) throws IOException {
		      StringBuilder sb = new StringBuilder();
		      int cp;
		      while ((cp = rd.read()) != -1) {
		         sb.append((char) cp);
		      }
		      return sb.toString();
	  }
	   
	   
	   public static void main(String[] args) throws JSONException, IOException {
		   
		   HashMap<String,List<Object>> map = (HashMap<String, List<Object>>) getIndexOfVersions(Constants.JIRA_PROJ_NAME);
		   
		   for(Map.Entry<String, List<Object>> m : map.entrySet()) {
			   System.out.println("version name: "+m.getKey()+" version index: "+m.getValue().get(0)+" version date: "+m.getValue().get(1).toString());
		   }
		   
		   System.out.println("Last index to analyze: "+GetReleaseInfo.lastIndexOfVersionAalyzable);
		   
	   }
	   
   
}