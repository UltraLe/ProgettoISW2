package control;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.AnalyzedFile;
import entity.Constants;

public class GitFilesAttributesFinder {

	//the key of this map will be the index of a release,
	//the value, a list of all the files contained in that release
	private TreeMap<Integer, List<AnalyzedFile>> allReleasesFiles = new TreeMap<>();
	
	//key =releaseIndex, value = releasedate
	private TreeMap<Integer, LocalDate> indexDate;
	
	//max release index to work with
	private int maxReleaseIndex;
	
	//the files of the release i am currently working with
	private HashMap<String, AnalyzedFile> filesOfARelease;
	
	
	private String projName;
	
	private GitFilesAttributesFinder(String projName) {
		this.projName = projName;
		//setting up logger
		Handler fileHandler;
		try {
			fileHandler = new FileHandler(Constants.LOG_FILE);
			Constants.LOGGER.addHandler(fileHandler);

			Map<String, List<Object>> releaseIndexDate = GetReleaseInfo.getIndexOfVersions(this.projName);
			this.maxReleaseIndex = GetReleaseInfo.lastIndexOfVersionAalyzable;
			
			//creating a tree map that will be useful to retrieve the index
			//corresponding to the date of the opening version of a ticket
			this.indexDate = new TreeMap<>();
			for(Map.Entry<String, List<Object>> entry : releaseIndexDate.entrySet()) {
				this.indexDate.put((Integer)entry.getValue().get(0), ((LocalDateTime)entry.getValue().get(1)).toLocalDate());
			}

		} catch (SecurityException | IOException e) {
			Constants.LOGGER.log(Level.SEVERE, e.getMessage());
		}
		
	}
	
	
	private List<LocalDate> getReleaseDateInterval(int releaseIndx){
		
		List<LocalDate> sinceUntil = new ArrayList<>();
		
		//if the release number is 1, since is the birth date of the project
		//but i do not need to extract it, i can just set an ''old'' date
		if(releaseIndx == 1) {
			sinceUntil.add(LocalDate.of(1971, Month.JANUARY, 1));
		}else {
			//otherwise the release 'i' has started when the release 'i-1'
			sinceUntil.add(indexDate.get(releaseIndx-1));
		}
		
		sinceUntil.add(indexDate.get(releaseIndx));
		
		return sinceUntil;
		
	}
	
	//method that return a the analyzed file if it has been found 
	private AnalyzedFile findFileInRelease(String filename, int release, HashMap<String, AnalyzedFile> filesOfCommit) {
		
		AnalyzedFile af;
		
		//i have to look in filesOfARelease and filesOfCommit
		if(!filesOfCommit.isEmpty() && filesOfCommit.containsKey(filename)) {
				return filesOfCommit.get(filename);
		}
		
		if(!filesOfARelease.isEmpty() && filesOfARelease.containsKey(filename)) {
			return filesOfARelease.get(filename);
		}
		
		//otherwise the file is new
		af = new AnalyzedFile(filename);
		af.setReleaseIndex(release);
		
		return af;
	}
	
	
	private Map<String, AnalyzedFile> getFileAttributePerCommit(JSONObject commit, int release) {
		
		HashMap<String, AnalyzedFile> filesOfCommit = new HashMap<>();
		
		JSONArray files = commit.getJSONArray("files");
		
		for(int i = 0; i < files.length(); ++i) {
			
			JSONObject file = files.getJSONObject(i);
			
			String fileName = file.getString("filename");
			
			//a new class has to be created IF and ONLY IF
			//the file is found for the first time...
			AnalyzedFile currentFile = findFileInRelease(fileName, release, filesOfCommit);
			//incrementing its number of revisions
			currentFile.incrementNumRevisions();
			
			//now calculating file's attributes
			
			//extraction commit date
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			String dateString = commit.getJSONObject(Constants.COMMIT).getJSONObject(Constants.COMMITTER).getString(Constants.GIT_DATE).substring(0, 10);
	        LocalDate commitDate = LocalDate.parse( dateString, formatter);
			
	        //updating LOC if needed (if a commit of the same file is found, and it
	        //has been done later than the previous found (if any))
			if(currentFile.shouldUpdateSize(commitDate)) {
				try {
					int loc = GitInteractor.getFileLOC(file.getString("raw_url"));
					
					System.out.println("LOC: "+loc+" file: "+fileName);
					
					currentFile.updateSize(loc);
					
				} catch (JSONException | IOException e) {
					Constants.LOGGER.log(Level.SEVERE, e.getMessage());
				}
			}
			
			//LOC touched
			int additions = file.getInt("additions");
			int deletions = file.getInt("deletions");
			int changes = file.getInt("changes");
			currentFile.incrementLocTouched(additions+deletions+changes);
			
			//LOC added
			currentFile.incrementLocAdded(additions);
			
			//adding authors of the file
			String author = commit.getJSONObject(Constants.COMMIT).getJSONObject(Constants.COMMITTER).getString("name");
			currentFile.addAuthor(author);
			
			//churn
			currentFile.updateChurn(additions-deletions);
			
			//chgset size
			currentFile.incrementChgSetSize(files.length());
			
			//last but not least, add the AnalyzedClass onto the Hash Map
			//if the element war already in here, it has to be overwritten
			filesOfCommit.put(fileName, currentFile);
		}
		
		return filesOfCommit;
		
	}
	
	private void getFileAttributesPerRelease(int release) throws IOException {
		
		List<LocalDate> siceUntil = getReleaseDateInterval(release);
		String iso8601 = "T12:00:00Z";
		//github api wants date in ISO 8601 format
		String since = siceUntil.get(0).toString().concat(iso8601);
		String until = siceUntil.get(1).toString().concat(iso8601);
		
		filesOfARelease = new HashMap<>();
		
		int page = 1;
		int commitsNum;
		do {
			//calling the method that will return the commits in a given time interval
			//at a given page
			JSONArray commits = GitInteractor.getCommitsInTimeInterval(since, until, page);
			commitsNum = commits.length();
			
			//here the main body, for each commit we need to extract the files associated
			//to it and calculate the attributes...
			for(int i = 0; i < commits.length(); ++i) {
				
				//retrieving all the commit informations needed (from its sha)
				//(where there will be all the files)
				JSONObject commit = GitInteractor.getGitCommit(commits.getJSONObject(i).getString("sha"));
				filesOfARelease.putAll(getFileAttributePerCommit(commit, release));
			}
			
			//the requested url will return 29 commits per page, if
			//less than 29 commits are returned there will not be
			//a next page to look for
			page++;
		//}while(commitsNum >= 29);
		}while(false);
		//TODO remove after testing
		
		//super magic
		List<AnalyzedFile> listFilesOfARelease = new ArrayList<>();
		for(Map.Entry<String, AnalyzedFile> entry : filesOfARelease.entrySet()) {
			listFilesOfARelease.add(entry.getValue());
		}
		
		allReleasesFiles.put(release, listFilesOfARelease);
	}
	
	public void start() throws IOException {
		
		GitInteractor.extractTkn();
		//for each analyzable release, call getFileAttributePerRelease
		this.maxReleaseIndex = 2;
		for(int i = 1 ; i < this.maxReleaseIndex; ++i) {
			
			try {
				getFileAttributesPerRelease(i);
			} catch (IOException e) {
				Constants.LOGGER.log(Level.SEVERE, e.getMessage());
			}
			
			Constants.LOGGER.log(Level.INFO, "Collected Files Attributes of release(indx) {0}", i);
		}
		
		//TODO age calculator algorithm 
		
		CsvFileWriter.writeFilesAttributes(allReleasesFiles, Constants.JIRA_PROJ_NAME);
	}
	
	
	public static void main(String[] args) {
		
		GitFilesAttributesFinder g = new GitFilesAttributesFinder(Constants.JIRA_PROJ_NAME);
		
		try {
			g.start();
		} catch (IOException e) {
			Constants.LOGGER.log(Level.SEVERE, e.getMessage());
		}
	}
	
	
	
}
