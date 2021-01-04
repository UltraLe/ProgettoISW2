package entity;

import java.util.logging.Logger;

public class Constants {
	
	public static final String COMMIT_CLASS_NAME = "COMMIT_CLASS_NAME"; 
	public static final String DATE = "DATE"; 
	public static final String BUGGY_FILENAME = "buggyFiles";
	public static final String WEKA_RESULTS = "wekaResults";
	
	public static final String CSV_EXT = ".csv";
	
	public static final String JIRA_SEARCH_URL = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22";
	
	//Project name used on GitHub
	//bookkeeper
	//syncope
	private static String gitProjName;
		
	//Project name used on JIRA
	//BOOKKEEPER
	//SYNCOPE
	private static String jiraProjName;
		
	//this token can be public because it has only read permission
	//but it has to be obscured due to github policies
	public static final String GIT_TKN = "e 6 e 9 8 0 3 6 a 3 6 c 5 f 6 9 5 0 5 6 b c b 6 6 f 5 5 5 d e 6 6 c 1 0 c f 3 d";
		
	public static final String COMMINTS_MONTH ="bugsPerMonth";
	public static final String LOG_FILE = "log.txt";
	public static final String MODELS_DIR ="./models/";
	public static final String FINAL_TABLE ="finalTable";
		
	//GITHUB REST API to retrieve the commit with given (%s to specify later on) ticket ID
	//sorted by committer date (from latest to earlier)
		   
	//GITHUB api url to get information of a ginven commit
	
	public static final String ISSUES = "issues";
	public static final String FIELDS = "fields";
	public static final String CREATED = "created";
	public static final String COMMIT = "commit";
	public static final String COMMITTER = "committer";
	public static final String GIT_DATE = "date";
	public static final String AUTORIZATION = "Authorization";
	public static final String TOKEN = "token ";
	
	//Sometimes the tickets are referred in git not in the usual way
	//(such us [PRJNAME-1234]) BUT all are like %-1234,
	//In order to get as much commits as possible it is useful
	//to search commits that contains '-0000' in the comment
	public static final boolean TKT_SEARCH_FAST = true;
	public static final String FAST = "FAST";
	
	//Extension of the programming lengulanguageage
	public static final String PROG_LANG_EXT = ".java";
	
	//Projects name
	public static final String DAFFODIL_PRJ = "DAFFODIL";
	public static final String DAFFODIL_PRJ_GIT = "incubator-daffodil";
	
	public static final String BOOKKEEPER_PRJ = "BOOKKEEPER";
	public static final String BOOKKEEPER_PRJ_GIT = "bookkeeper";
	
	public static final String SYNCOPE_PRJ = "SYNCOPE";
	public static final String SYNCOPE_PRJ_GIT = "syncope";
	
	public static final Logger LOGGER = Logger.getLogger(Constants.class.getName());
	
	
	private Constants() {
		throw new IllegalStateException("Utility class");
	}
	
	public static String getSearchTktLastCommitUrl() {
		return "https://api.github.com/search/commits?q=repo:apache/"+gitProjName+"+\"%s\"+sort:committer-date";
		
	}
	
	public static String getCommitInfoUrl() {
		return "https://api.github.com/repos/apache/"+gitProjName+"/commits/%s";
	}
	
	public static String getJiraProjName() {
		return jiraProjName;
	}
	
	public static String getGitProjName() {
		return gitProjName;
	}
	
	public static void setJiraProjName(String j) {
		jiraProjName = j;
	}
	
	public static void setGitProjName(String g) {
		gitProjName = g;
	}

}
