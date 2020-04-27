package control;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

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
	
	private void getFileAttributesPerRelease(int release) {
		
		List<LocalDate> siceUntil = getReleaseDateInterval(release);
		
		//github api wants date in ISO format
		String since = siceUntil.get(0).format(DateTimeFormatter.ISO_INSTANT);
		String until = siceUntil.get(1).format(DateTimeFormatter.ISO_INSTANT);
		
		//super magic
		
		
	}
	
	public void start() {
		
		//for each analyzable release, call getFileAttributePerRelease
		for(int i = 1 ; i < this.maxReleaseIndex; ++i) {
			getFileAttributesPerRelease(i);
		}
		
	}
	
	
	
}
