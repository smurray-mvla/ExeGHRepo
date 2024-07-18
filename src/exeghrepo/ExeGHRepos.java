package exeghrepo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class ExeGHRepos {
	GitTools gt = new GitTools();
	private String org="";
	private String assignment="";
	private String tag = "";
	private String runLog = "";
	private String date = "";
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
	
	private ArrayList<String> getURLs (String organization, String assignment, String tag) {
		return (gt.getRepoURLs(organization,assignment,tag));
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
			case "-l": runLog = getNextArg(args,i++); break;
			case "-t": tag = getNextArg(args,i++); break;
			case "-dl": date = getNextArg(args,i++); break;
			case "-i": incremental = !incremental; break;
			case "-gd": gradleDebug = !gradleDebug; break;
			case "-d": debug = !debug; break;
			case "-noRestore": noRestore = !noRestore; break;
			case "-f": force = !force; break;
			case "-L": list = !list; break;
			case "-noClone": noClone = !noClone; break;
			}
		}
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
	
	private void readConfigFile() {
		String cfgFilename = (debug) ? "debug/.CONFIG" : ".CONFIG";
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
			} catch (Exception e)
			{
				System.out.println("-E- Exception occured when trying to read config file: "+cfg.getName());
				e.printStackTrace();
			}
		} else {
			System.out.println("-E- "+cfgFilename+" does not exist in current directory - aborting...");
			System.exit(5);
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ExeGHRepos jExe = new ExeGHRepos();
		if (args.length > 0) 
			jExe.processArgs(args);
		jExe.readConfigFile();
		ArrayList<String> repoURL = jExe.getURLs(jExe.getOrg(),jExe.getAssignment(),jExe.getTag());
		for (String url : repoURL) {
			System.out.println(url);
		}
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

}
