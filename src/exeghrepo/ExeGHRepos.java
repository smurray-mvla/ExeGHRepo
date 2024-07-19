package exeghrepo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class ExeGHRepos {
	GitTools gt = new GitTools();
	private String org="";
	private String assignment="";
	private String tag = "";
	private String runLog = "";
	private String date = "";
	private String path = "";
	private String excludeRepo = "";
	private boolean incremental = false;
	private boolean gradleDebug = false;
	private boolean debug = false;
	private boolean noRestore = false;
	private boolean force = false;
	private boolean list = false;
	private boolean noClone = false;
	private int testIndex = -1;
	private ArrayList<String> deleteList;
	private ArrayList<String> copyList;
	private ArrayList<String> postCommands;
	private ArrayList<ExeTest> testList;
	private ExeTest currTest;
	private HashMap<String,HashMap<String,String>> testResults;
	private ArrayList<String> repoURLs;
	
	private void getURLs (String organization, String assignment, String tag) {
		 repoURLs = gt.getRepoURLs(organization,assignment,tag);
		 repoURLs.sort(Comparator.naturalOrder());
		 if (list) printURLs();
	}
	
	private void printUsage() {
		System.out.println("Usage message needs to be added here!!!");
	}
	
	private void printArgsError(String message,String[] args) {
		System.out.println("-E-: "+message);
	}
	

	private String getNextArg(String[] args, int i) {
		if ((i+1) == args.length) {
			printArgsError("Switch "+args[i]+" requires a string argument - none supplied",args);
			printUsage();
			System.exit(2);
		} 
		String nextArg = args[i+1];
		if (nextArg.matches("\\-.*")) {
			printArgsError("Switch "+args[i]+" requires a string argument, but found a switch: "+nextArg,args);
			printUsage();
			System.exit(3);
		}
		return (nextArg);
	}
	
	private void processArgs(String[] args) {
		System.out.println("-I-: Command Line Arguments:");
		System.out.println("     "+String.join(" ", args));
		
		for (int i = 0; i < args.length; i++) {
			// all switches begin with a "-"; otherwise printHelp and abort
			if (!args[i].matches("\\-.*")) {
				printArgsError("Unexpected argument: "+args[i]+". Expected valid switch",args);
				printUsage();
				System.exit(1);
			}
			switch (args[i]) {
			case "-o": org = getNextArg(args,i++); break;
			case "-a": assignment = getNextArg(args,i++); break;
			case "-p": path = getNextArg(args,i++); break;
			case "-l": runLog = getNextArg(args,i++); break;
			case "-t": tag = getNextArg(args,i++); break;
			case "-dl": date = getNextArg(args,i++); break;
			case "-exc" : excludeRepo = getNextArg(args,i++); break;
			case "-i": incremental = true; break;
			case "-gd": gradleDebug = true; break;
			case "-d": debug = true; break;
			case "-noRestore": noRestore = true; break;
			case "-f": force = true; break;
			case "-L": list = true; break;
			case "-noClone": noClone = true; break;
			}
		}
		if ("".equals(path)) path = System.getProperty("user.dir");
		System.out.println("path = "+ path);
	}
	
	private void processDelete(String deleteRE) {
		if (!"".equals(deleteRE)) {
			if (deleteList == null) 
				deleteList = new ArrayList<>();
			deleteList.add(deleteRE);
		}
	}
	
	private void processCopy(String copyRE) {
		if (!"".equals(copyRE)) {
			if (copyList == null) 
				copyList = new ArrayList<>();
			copyList.add(copyRE);
		}
	}
	
	private void processTest(String test) {
		if (!"".equals(test)) {
			if (testList == null) 
				testList = new ArrayList<>();
			currTest = new ExeTest(test);
			testList.add(currTest);
		} else {
			System.out.println("Problem with config file - no test name supplied");
			System.exit(4);
		}
	}

	private void processPostCommands(String[] tokens) {
		String command = "";
		int i = 1;
		do {
			command += tokens[i];
			i++;
			if (i < tokens.length) command += ":";
		} while (i < tokens.length);
		if (postCommands == null)
			postCommands = new ArrayList<>();
		postCommands.add(command);
	}

	private void updateCurrTestOption(String option, String value) {
		if (value == null) return;
		if ("EXE".equals(option)) currTest.setTestMode(value);
		else if ("USER".equals(option)) currTest.setUser(true);
		else if ("VIM".equals(option)) currTest.setVim(value);
		else if ("SRC".equals(option)) currTest.setSourcePath(value);
		else if ("LIB".equals(option)) currTest.setLib(value);
		else if ("CMP".equals(option)) currTest.setCmp(value);
		else if ("TIMEOUT".equals(option)) {
			try {
				int timeout = Integer.parseInt(value);
				currTest.setTimeout(timeout);
			} catch (Exception e) {
				System.out.println("-I- Detected non-integer timeout value in config file. Using default timeout value");
			}
		}
	}
	
		
	private void processConfigOptions(String[] tokens) {
		String option = tokens[0];
		String value = null;
		if (tokens.length >= 2) value = tokens[1];
		switch (option) {
		case "ORG": org = value; return;
		case "ASSIGNMENT": assignment = value; return;
		case "DELETE": processDelete(value); return;
		case "COPY": processCopy(value); return;
		case "POSTCMD": processPostCommands(tokens); return;
		case "TEST": processTest(value); return;
		default:updateCurrTestOption(option, value); return;
		}

	}
	
	private void updateRunLog() {
		if ("".equals(runLog)) 
			runLog = assignment+".summary.csv";
//		if (!"".equals(path))
				runLog = path + "/"+runLog;
	}
	
	private void readConfigFile() {
		String cfgFilename = ".CONFIG";
//		if (!"".equals(path)) 
			cfgFilename = path + "/" + cfgFilename;
		File cfg = new File(cfgFilename);
		String line = null;
		if (cfg.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(cfg))) {
				while ((line = br.readLine())!=null) {
					if (line.matches("^\\s*$") || line.matches("#.*$"))
						continue;
					String[] tokens = line.split(":");
					processConfigOptions(tokens);
				}
				br.close();
			} catch (Exception e)
			{
				System.out.println("-E- Exception occured when trying to read config file: "+cfg.getPath());
				e.printStackTrace();
			}
		} else {
			System.out.println("-E- "+cfgFilename+" does not exist in current directory - aborting...");
			System.exit(5);
		}
	}
	
	private boolean consistentHeader(String[] tokens) {
		if (tokens.length - 1 != testList.size())
			return false;
		int index = 1;
		for (ExeTest test : testList) {
			if (!test.getTestName().equals(tokens[index++]))
				return false;
		}
		return true;
	}
	
	private void printTestResults() {
		ArrayList<String> repoList = new ArrayList<>();
		System.out.print("Repository");
		for (ExeTest test : testList) {
			System.out.print(","+test.getTestName());
		}
		System.out.println();
		for (String url : repoURLs) { 
			String repo = url.replaceAll("\\.git","").replaceAll("^.*\\/", "");
			if (testResults.containsKey(repo)) {
				System.out.print(repo);
				for (ExeTest test : testList) {
					System.out.print(","+testResults.get(repo).get(test.getTestName()));
				}
				System.out.println();		
			}
		}
	}
	
	private void processHistoryFile() {
		updateRunLog();
		File historyFile = new File(runLog);
		if (historyFile.exists()) {
			boolean header = true;
			String line = null;
			testResults = new HashMap<>();
			try (BufferedReader br = new BufferedReader(new FileReader(historyFile))) {
				while ((line = br.readLine())!=null) {
					String[] tokens = line.split(",");
					if (header) {
						if (!consistentHeader(tokens)) break;
					} else {
						String repo = tokens[0];
						HashMap<String,String> repoTestResults = new HashMap<>();
						int index = 1;
						for (ExeTest test : testList ) {
							repoTestResults.put(test.getTestName(), tokens[index++]);
						}
						testResults.put(repo, repoTestResults);
					}
					header = false;
				}
				br.close();
			} catch (Exception e)
			{
				System.out.println("-E- Exception occured when trying to read history file: " + historyFile.getName());
				e.printStackTrace();
			}
			if (debug) printTestResults();
		}
	}
	
	private void printURLs() {
		for (String url : repoURLs) {
			System.out.println(url);
		}
	}

	private boolean isFailingClone(String repo) {
		HashMap<String,String> repoTestResults = testResults.get(repo);
		for (ExeTest test : testList) {
			if (!repoTestResults.get(test.getTestName()).equals("PASSED")) return true;
		}
		return false;
	}
	
	private String getRemoteHeadSHA(String url) {
		StringBuffer SHA = new StringBuffer();
		ProcessBuilder pb = new ProcessBuilder("git","ls-remote",url,"HEAD");
		InputStreamReader isr;
		int c;
		try {
			Process gitRemoteSHA = pb.start();
			isr = new InputStreamReader(gitRemoteSHA.getInputStream());
			while ((c=isr.read())>0) {
				SHA.append((char) c);
			}
			isr.close();

		} catch (Exception e) {
			System.out.println("Exception when trying to get the SHA of the remote HEAD");
			e.printStackTrace();
		}
		return SHA.toString().replaceAll("\\s+HEAD", "");
	}
	 
	private String getLocalHeadSHA(String repoPath) {
		StringBuffer SHA = new StringBuffer();
		ProcessBuilder pb = new ProcessBuilder("git","rev-parse","HEAD");
		pb.directory(new File(repoPath));
		InputStreamReader isr;
		int c;
		try {
			Process gitRemoteSHA = pb.start();
			isr = new InputStreamReader(gitRemoteSHA.getInputStream());
			while ((c=isr.read())>0) {
				SHA.append((char) c);
			}
			isr.close();

		} catch (Exception e) {
			System.out.println("Exception when trying to get the SHA of the local HEAD");
			e.printStackTrace();
		}
		return SHA.toString().replaceAll("\\s+HEAD", "");
	}
	
	private boolean repoRequiresCloning(String url, String repo) {
		String remoteSHA = getRemoteHeadSHA(url);
		System.out.println("Remote SHA = "+remoteSHA);
		String repoPath = path +"/"+repo;
		String localSHA = getLocalHeadSHA(repoPath);
		System.out.println("Local SHA = "+localSHA);
		return true;
	}
	
	private void deleteRepo(File file) {
		File[] list = file.listFiles();
		if (list != null) {
			for (File subFile : list) {
				deleteRepo(subFile);
			}
		}
		boolean status = file.delete();
		if (!status)
			System.out.println("Could not delete: "+file.getPath());
	}
	
	 
	private boolean cloneRepo(String url, String repo) {
		String repoPath =  path + "/" + repo;
		File repoDir = new File(repoPath);
		int status=-1;
		int attempt = 0;
		if (repoDir.exists()) {
			System.out.println("-I- "+repoPath + " already exists; deleting before attempting to clone");
			deleteRepo(repoDir);
		}
		ProcessBuilder pb = new ProcessBuilder("git", "clone",url);
		InputStreamReader isr;
		int c;
		pb.directory(new File(path));
		try {
			do {
				attempt++;
				System.out.println("-I- Executing Command: git clone "+url);
				Process clone = pb.start();
				isr = new InputStreamReader(clone.getErrorStream());
				while ((c=isr.read())>=0) {
					System.out.print((char) c);
					System.out.flush();
				}
				isr.close();
				status = clone.exitValue();
			
				System.out.println("Attempt "+attempt+ " to clone "+repo+" completed with status = "+status);

			} while (status != 0 && attempt <= 5);
		} catch (Exception e) {
			System.out.println("Attempt to clone failed");
			e.printStackTrace();
		}
		return (status == 0);
	}
	
	private void testRepo(String url) {
		String repo = url.replaceAll(".git$", "").replaceAll("^.*\\/", "");
		if (repoRequiresCloning(url,repo)) 
			cloneRepo(url,repo);
		
	}
	
	private void executeFlow() {
		getURLs(org,assignment,tag);
		processHistoryFile();
		for (String url : repoURLs) {
			testRepo(url);
		}
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ExeGHRepos jExe = new ExeGHRepos();
		jExe.processArgs(args);
		jExe.readConfigFile();
		jExe.executeFlow();
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public String getAssignment() {
		return assignment;
	}

	public void setAssignment(String assignment) {
		this.assignment = assignment;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public ArrayList<String> getRepoURL() {
		return repoURLs;
	}


}
