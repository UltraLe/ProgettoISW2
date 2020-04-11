package control;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Buggy {
	
	private static final Logger LOGGER = Logger.getLogger(Buggy.class.getName());
	public static final String LOG_FILE = "buggyLog.txt";
	
	private int maxVersionIndex;
	private String projName;
	
	public Buggy(String projName) {
		this.projName = projName;
		//setting up logger
		Handler fileHandler;
		try {
			fileHandler = new FileHandler(LOG_FILE);
			LOGGER.addHandler(fileHandler);
		} catch (SecurityException | IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}	
	}
	
	private void getMaxVersionIndex() {
		
		try {
			GetReleaseInfo.getIndexOfVersions(this.projName);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
	}
}
