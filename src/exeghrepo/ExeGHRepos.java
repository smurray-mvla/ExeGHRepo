package exeghrepo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

// TODO: Auto-generated Javadoc
/**
 * The Class ExeGHRepos.
 */
public class ExeGHRepos {
	
	/** The gt. */
	GitTools gt = null;
	
	/** The gr. */
	GradleTools gr;
	
	/** The org. */
	private String org="";
	
	/** The assignment. */
	private String assignment="";
	
	/** The tag. */
	private String tag = "";
	
	/** The run log. */
	private String runLog = "";
	
	/** The date. */
	private String date = "";
	
	/** The path. */
	private String path = "";
	
	/** The exclude repo. */
	private String excludeRepo = "";
	
	/** The local SHA. */
	private String localSHA = "";
	
	/** The repo path. */
	private String repoPath = "";
	
	/** The curr repo. */
	private String currRepo = null;
	
	private String branch = "";
	private String currBranch = "";
	
	/** The checkout. */
	private boolean checkout = false;
	
	/** The incremental. */
	private boolean incremental = false;
	
	/** The gradle debug. */
	private boolean gradleDebug = false;
	
	/** The debug. */
	private boolean debug = false;
	
	/** The no restore. */
	private boolean noRestore = false;
	
	/** The force. */
	private boolean force = false;
	
	/** The list. */
	private boolean list = false;
	
	/** The no clone. */
	private boolean noClone = false;
	
	/** The test index. */
	private int testIndex = -1;
	
	/** The delete list. */
	private ArrayList<String> deleteList;
	
	/** The copy list. */
	private ArrayList<FileCopyInfo> copyList;
	
	/** The post commands. */
	private ArrayList<String> postCommands;
	
	/** The test list. */
	private ArrayList<ExeTest> testList;
	
	/** The curr test. */
	private ExeTest currTest;
	
	/** The test results. */
	private HashMap<String,HashMap<String,String>> testResults;
	
	/** The detail test order map. */
	private HashMap<String,ArrayList<String>> detailTestOrderMap;
	
	/** The detailed test results. */
	private HashMap<String,HashMap<String,ArrayList<String>>> detailedTestResults;
	
	/** The repo UR ls. */
	private ArrayList<String> repoURLs;
	
	/** The gradle cmd. */
	private String[] gradleCmd;
    /**
     * Gets the operating system.
     *
     * @return the operating system
     */
    private boolean isWindows() {
    	return System.getProperty("os.name").contains("Win");
    }

	/**
	 * Gets the matching paths.
	 *
	 * @param srcPath the src path
	 * @param matchPattern the match pattern
	 * @param files the files
	 * @return the matching paths
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void getMatchingPaths(Path srcPath, String matchPattern, ArrayList<Path> files) throws IOException {
		final ArrayList<Path> filePaths = new ArrayList<>();
		final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/"+matchPattern);
		Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (matcher.matches(file)) {
					filePaths.add(file);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		filePaths.forEach(filePath -> files.add(filePath));
	}
	
	/**
	 * Gets the UR ls.
	 *
	 * @param organization the organization
	 * @param assignment the assignment
	 * @param tag the tag
	 * @return the UR ls
	 */
	private void getURLs (String organization, String assignment, String tag) {
		gt = new GitTools(organization);
		repoURLs = gt.getRepoURLs(organization,assignment,tag);
		if (!"".equals(excludeRepo)) {
			for (int i = repoURLs.size()-1; i >=0; i--) {
				if (repoURLs.get(i).matches("^.*"+excludeRepo+".*")) {
					System.out.println("-I- Excluding Repo URL: "+repoURLs.get(i));
					repoURLs.remove(i);
				}
			}
		}
		repoURLs.sort(Comparator.naturalOrder());
		if (list) printURLs();
	}
	
	/**
	 * Prints the usage.
	 */
	private void printUsage() {
		System.out.println("Usage message needs to be added here!!!");
	}
	
	/**
	 * Prints the args error.
	 *
	 * @param message the message
	 * @param args the args
	 */
	private void printArgsError(String message,String[] args) {
		System.out.println("-E-: "+message);
	}
	
	/**
	 * Gets the next arg.
	 *
	 * @param args the args
	 * @param i the i
	 * @return the next arg
	 */
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
	
	/**
	 * Prints the args error and exit.
	 *
	 * @param args the args
	 * @param i the i
	 */
	private void printArgsErrorAndExit(String[] args, int i) {
		printArgsError("-I- Unexpected command line argument: "+args[i]+".\n"+
	                   "    Expected a valid switch",args);
		printUsage();
		System.exit(1);
		
	}
	
	/**
	 * Process args.
	 *
	 * @param args the args
	 */
	private void processArgs(String[] args) {
		System.out.println("-I-: Command Line Arguments:");
		System.out.println("     "+String.join(" ", args));
		
		for (int i = 0; i < args.length; i++) {
			// all switches begin with a "-"; otherwise printHelp and abort
			if (!args[i].matches("\\-.*")) printArgsErrorAndExit(args,i);
			switch (args[i]) {
			case "-o": org = getNextArg(args,i++); break;
			case "-a": assignment = getNextArg(args,i++); break;
			case "-p": path = getNextArg(args,i++); break;
			case "-l": runLog = getNextArg(args,i++); break;
			case "-t": tag = getNextArg(args,i++); break;
			case "-d": date = getNextArg(args,i++); break;
			case "-b": branch = getNextArg(args,i++); break;
			case "-x" : excludeRepo = getNextArg(args,i++); break;
			case "-i": incremental = true; break;
			case "-gd": gradleDebug = true; break;
			case "-D": debug = true; break;
			case "-nR": noRestore = true; break;
			case "-f": force = true; break;
			case "-L": list = true; break;
			case "-nC": noClone = true; break;
			default : printArgsErrorAndExit(args,i);
			}
		}
		if ("".equals(path)) path = System.getProperty("user.dir");
	}
	
	/**
	 * Process delete.
	 *
	 * @param deleteArray the delete array
	 */
	private void processDelete(ArrayList<String> deleteArray) {
		for (String deleteRE : deleteArray) {
			if (!"".equals(deleteRE)) {
				if (deleteList == null) 
					deleteList = new ArrayList<>();
				deleteList.add(deleteRE);
			}
		}
	}
	
	/**
	 * Process copy.
	 *
	 * @param copyArray the copy array
	 */
	private void processCopy(ArrayList<String> copyArray) {
		if ("".equals(copyArray.get(0))) {
			System.out.println("-I- COPY command in .CONFIG, but no file specified to copy. Skipping.");
			return;
		}
		if (copyList == null) 
			copyList = new ArrayList<>();
		if (copyArray.size() > 1)
			copyList.add(new FileCopyInfo(copyArray.get(0),copyArray.get(1)));
		else
			copyList.add(new FileCopyInfo(copyArray.get(0),""));
			
	}
	
	/**
	 * Process test.
	 *
	 * @param test the test
	 */
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

	/**
	 * Process post commands.
	 *
	 * @param command the command
	 */
	private void processPostCommands(String command) {
		String[] tokens = command.split("\\s+");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].matches("^~.*")) {
				tokens[i] = Paths.get(System.getProperty("user.home"),tokens[i].replaceAll("^~[^A-Za-z]*","")).toString();
			}
		}

		if (postCommands == null)
			postCommands = new ArrayList<>();
		postCommands.add(String.join(" ", tokens));
	}

	/**
	 * Update curr test option.
	 *
	 * @param option the option
	 * @param value the value
	 */
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
				System.out.println("-I- Non-integer timeout value ("+value+") in config file.\n"+
			                       "    Using default timeout value");
			}
		}
	}
	
	/**
	 * Process config options.
	 *
	 * @param tokens the tokens
	 */
	private void processConfigOptions(String[] tokens) {
		ArrayList<String> values = new ArrayList<String>(Arrays.asList(tokens));
		String option = values.remove(0);		
		if (values.isEmpty()) {
			if ((option.equals("ORG") || option.equals("TEST") || option.equals("ASSIGNMENT")) ) 
				System.out.println("-I- Option "+option+" not initialized correctly in .CONFIG file. Skipping");
			return;
		}
		switch (option) {
		case "ORG": org = values.get(0); return;
		case "ASSIGNMENT": assignment = values.get(0); return;
		case "DELETE": processDelete(new ArrayList<String>(values)); return;
		case "COPY": processCopy(new ArrayList<String>(values)); return;
		case "POSTCMD": processPostCommands(values.get(0)); return;
		case "TEST": processTest(values.get(0)); return;
		default:updateCurrTestOption(option, values.get(0)); return;
		}
	}

	
	
	/**
	 * Update run log.
	 */
	private void updateRunLog() {
		if ("".equals(runLog)) 
			runLog = assignment+".summary.csv";
		runLog = path + "/"+runLog;
	}
	
	/**
	 * Read config file.
	 */
	private void readConfigFile() {
		String cfgFilename = ".CONFIG";
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
	
	/**
	 * Consistent header.
	 *
	 * @param tokens the tokens
	 * @return true, if successful
	 */
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
	
	/**
	 * Prints the test results.
	 */
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
	
	/**
	 * Process history file.
	 */
	private void processHistoryFile() {
		updateRunLog();
		testResults = new HashMap<>();
		File historyFile = new File(runLog);
		if (historyFile.exists()) {
			boolean header = true;
			String line = null;
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
			} catch (Exception e) {
				System.out.println("-E- Exception occured when trying to read history file: " + historyFile.getName());
				e.printStackTrace();
			}
			if (debug) printTestResults();
		}
	}
	
	/**
	 * Prints the UR ls.
	 */
	private void printURLs() {
		for (String url : repoURLs) {
			System.out.println(url);
		}
	}

	/**
	 * Checks if is failing clone.
	 *
	 * @param repo the repo
	 * @return true, if is failing clone
	 */
	private boolean isFailingClone(String repo) {
		if (!testResults.containsKey(repo)) return false;
		HashMap<String,String> repoTestResults = testResults.get(repo);
		for (ExeTest test : testList) {
			if (!repoTestResults.get(test.getTestName()).equals("PASSED")) return true;
		}
		return false;
	}

	/**
	 * Gets the remote head SHA.
	 *
	 * @param url the url
	 * @return the remote head SHA
	 */
	private String getRemoteHeadSHA(String url) {
		String[] cmd = {"git","ls-remote",url,"HEAD"};
		String SHA = null;
		ProcessResults results = executeProcess(cmd,null,0);
		if (results.getStatus() == 0)
			SHA = results.getOutput().get(0).replaceAll("\\s+HEAD", "");
		return SHA;
	}
	 
	/**
	 * Gets the local head SHA.
	 *
	 * @return the local head SHA
	 */
	private String getLocalHeadSHA() {
		String[] cmd = {"git","rev-parse","HEAD"};
		String SHA = null;
		ProcessResults results = executeProcess(cmd,new File(repoPath),0);
		if (results.getStatus() == 0)
			SHA = results.getOutput().get(0).replaceAll("\\s+HEAD", "");
		return SHA;
	}
	
	/**
	 * Gets the SH aby date.
	 *
	 * @return the SH aby date
	 */
	private String getSHAbyDate() {
		String[] cmd = {"git","log","-1","--until=\'"+date+"\'","--format=format:%H"};
		String SHA = null;
		ProcessResults results = executeProcess(cmd,new File(repoPath),0);
		if (results.getStatus() == 0)
			SHA = results.getOutput().get(0).replaceAll("\\s+.*", "");
		return SHA;
	}
	
	/**
	 * Delete dir.
	 *
	 * @param file the file
	 * @return true, if successful
	 */
	private boolean deleteDir(File file) {
		boolean status = true;
		File[] list = file.listFiles();
		if (list != null) {
			for (File subFile : list) {
				status = status && deleteDir(subFile);
			}
		}
		if (!file.delete()) {
			System.out.println("Could not delete: "+file.getPath());
			status = false;
		}
		return status;
	}
	
	/**
	 * Extract process output.
	 *
	 * @param br the br
	 * @return the array list
	 * @throws Exception the exception
	 */
	ArrayList<String> extractProcessOutput(BufferedReader br) throws Exception {
		String line;
		ArrayList<String> output = new ArrayList<>();
		while ((line = br.readLine())!=null) {
			output.add(line);
		}
		return output;
	}
	 
	/**
	 * Execute process.
	 *
	 * @param cmd the cmd
	 * @param directory the directory
	 * @param timeout the timeout
	 * @return the process results
	 */
	private ProcessResults executeProcess(String[] cmd, File directory, int timeout) {
		BufferedReader br;
		ProcessResults procResults = new ProcessResults();
		ProcessBuilder pb = new ProcessBuilder(cmd);
		if (directory != null) pb.directory(directory);
		pb.redirectErrorStream(true);
		try {
			Process proc = pb.start();
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			proc.waitFor();
			procResults.setOutput(extractProcessOutput(br));
			br.close();
			procResults.setStatus(proc.exitValue());
		} catch (Exception e) {
			System.out.println("Exception executing cmd");
			procResults.setOutput(null);
			procResults.setStatus(-1);
		}
		return procResults;
	}
	
	/**
	 * Execute process post cmd.
	 *
	 * @param cmd the cmd
	 * @param directory the directory
	 * @return the process results
	 */
	private ProcessResults executeProcessPostCmd(String[] cmd, File directory) {
		InputStreamReader isr;
		ProcessResults procResults = new ProcessResults();
		ProcessBuilder pb = new ProcessBuilder(cmd);
		Map<String,String> env = pb.environment();
		if (!env.containsKey("JAVA_HOME")) env.put("JAVA_HOME",System.getProperty("java.home"));
		if (directory != null) pb.directory(directory);
		pb.redirectErrorStream(true);
		try {
			Process proc = pb.start();
			isr = new InputStreamReader(proc.getInputStream());
			int c;
			while (proc.isAlive()) {
				if ((c=isr.read())!=-1) {
					System.out.print((char) c);				
				}
			}
			while ((c=isr.read())!=-1) {
				System.out.print((char) c);
			}
			
			isr.close();
			procResults.setStatus(proc.exitValue());
		} catch (Exception e) {
			System.out.println("Exception executing cmd");
			e.printStackTrace();
			procResults.setOutput(null);
			procResults.setStatus(-1);
		}
		procResults.setOutput(null);
		return procResults;
	}
	
	/**
	 * Execute timeout process.
	 *
	 * @param cmd the cmd
	 * @param testName the test name
	 * @param directory the directory
	 * @param timeout the timeout
	 * @return the process results
	 */
	private ProcessResults executeTimeoutProcess(String[] cmd, String testName, File directory, long timeout) {
		ProcessResults procResults = new ProcessResults();
		TimeoutProcess timeoutProcess = new TimeoutProcess(this,cmd, testName, directory, procResults);
		Thread thread = new Thread(timeoutProcess);
		thread.start();
		long endTime = System.currentTimeMillis()+timeout*1000;
		boolean timedOut = false;
		while (thread.isAlive()) {
			if (System.currentTimeMillis() > endTime) {
				timedOut = true;
				break;
			}
		}
		if (timedOut) {
			timeoutProcess.setTerminate();
			procResults.setStatus(-2);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return procResults;
	}
	
	/**
	 * Clone repo.
	 *
	 * @param url the url
	 * @param repo the repo
	 * @return true, if successful
	 */
	private boolean cloneRepo(String url, String repo) {
		File repoDir = new File(repoPath);
		int status=-1;
		int attempt = 0;
		System.out.println("-I- Attemping to clone repo: "+repo);

		if (repoDir.exists()) {
			System.out.println("-I- "+repoPath + " already exists; deleting existing directory");
			if (!deleteDir(repoDir)) return false;
		}
	
		do {
			attempt++;
			System.out.println("-I- Executing Command: git clone "+url);
			String[] cmd = {"git","clone",url};
			ProcessResults results = executeProcess(cmd,new File(path),0);
			results.printOutput();
			status = results.getStatus();
			System.out.println("Attempt "+attempt+ " to clone "+repo+" completed with status = "+status);

		} while (status != 0 && attempt <= 5);
		return (status == 0);
	}
	
	/**
	 * Reset repo test history.
	 *
	 * @param repo the repo
	 */
	private void resetRepoTestHistory(String repo) {
		if (!testResults.containsKey(repo)) {
			testResults.put(repo, new HashMap<String,String>());
		}
		for (ExeTest test : testList) {
			testResults.get(repo).put(test.getTestName(), "-");
		}
	}
	
	/**
	 * Repo requires cloning.
	 *
	 * @param url the url
	 * @param repo the repo
	 * @return true, if successful
	 */
	private boolean repoRequiresCloning(String url, String repo) {
		File clone = new File(repoPath);
		if (!clone.exists()) return true;
		if (noClone) return false;
		if (force || !"".equals(date)) return true;
		String remoteSHA = getRemoteHeadSHA(url);
		String localSHA = getLocalHeadSHA();
		if (remoteSHA.equals(localSHA)) 
			return (incremental && isFailingClone(repo));
		return true;
	}
	
	/**
	 * Checkout by date.
	 *
	 * @return true, if successful
	 */
	private boolean checkoutByDate() {
		localSHA = getLocalHeadSHA();
		String SHAbyDate = getSHAbyDate();
		checkout = false;
		
		if ((SHAbyDate == null) || (!localSHA.equals(SHAbyDate))) {
			String[] cmd = {"git","checkout",SHAbyDate};
			ProcessResults results = executeProcess(cmd,new File(repoPath),0);
			results.printOutput();
			if (results.getStatus() == 0) {
				checkout = !noRestore;
			}
		}
		return checkout;
	}
	
	/**
	 * Delete files in list.
	 *
	 * @throws Exception the exception
	 */
	private void deleteFilesInList() throws Exception {
		System.out.println("-I- Deleting specified files");
		ArrayList<Path> deletePaths = new ArrayList<>();
		for (String deleteFile : deleteList) {
			if (!deleteFile.contains("*"))
				deletePaths.add(Paths.get(repoPath+"/"+deleteFile));
			else try {
				getMatchingPaths(Paths.get(repoPath),deleteFile,deletePaths);
				
			} catch (IOException e) {
				System.out.println("-E- getMatchingPaths threw an exception while trying to find "+deleteFile);
				throw new IOException();
			}
		}
		
		for (Path path : deletePaths) {
			System.out.println("-I- Deleting "+path.toString());
			File f = path.toFile();
			if (f.exists()) {
				if (f.isDirectory()) {
					if (!deleteDir(f)) throw new Exception();
				} else {
					if (!f.delete()) {
						System.out.println("-E- Attempt to delete file "+path + " failed");
						throw new Exception();
					}
				}
			}
		}	
	}

	/**
	 * Check source files.
	 *
	 * @param copySource the copy source
	 * @param copySourcePaths the copy source paths
	 * @return true, if successful
	 */
	private boolean checkSourceFiles(String copySource, ArrayList<Path> copySourcePaths) {
		if (!copySource.contains("*")) {
			Path source = Paths.get(path+"/"+copySource);
			File sf = source.toFile();
			if (!sf.exists()) {
				System.out.println("-E- Source file "+copySource+" does not exist in "+path);
				return false;
			}
			copySourcePaths.add(Paths.get(path+"/"+copySource));
		} else {
			try {
				String rootDir = (new File(path)).getName();
				getMatchingPaths(Paths.get(path),rootDir+"/"+ copySource,copySourcePaths);
			} catch (IOException e) {
				System.out.println("-E- getMatchingPaths threw an exception while trying to find "+ copySource);
				return false;
			} 
		}
		return true;
	}
	
	/**
	 * Copy directory.
	 *
	 * @param srcLoc the src loc
	 * @param destLoc the dest loc
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void copyDirectory(String srcLoc, String destLoc) throws IOException {
		Files.walk(Paths.get(srcLoc))
		.forEach(source -> {
			Path destination = Paths.get(destLoc,source.toString().substring(srcLoc.length()));
			try {
				Files.copy(source, destination);
			} catch (IOException e) {
				System.out.println("-E- Error copying "+source.toString()+" to "+destination.toString());
			}
		});
	}

	/**
	 * Check copy source dest paths.
	 *
	 * @param copyInfo the copy info
	 * @param copySourcePaths the copy source paths
	 * @param destPath the dest path
	 * @throws Exception the exception
	 */
	private void checkCopySourceDestPaths(FileCopyInfo copyInfo, ArrayList<Path> copySourcePaths,Path destPath) 
	             throws Exception {
		if (!checkSourceFiles(copyInfo.getSource(),copySourcePaths)) {
			throw new Exception();
		}
		File destDir = destPath.toFile();
		if (destDir.exists() && !destDir.isDirectory()) {
				System.out.println("-E- Destination of COPY command must a directory, but is not");
				throw new Exception();
		} else if (!destDir.exists()) {
			if (!destDir.mkdir()) {
				System.out.println("-E- Unable to create destination directory "+destDir.getName());
				throw new Exception();
			}
		}		
	}
	
	/**
	 * Copy files in list.
	 *
	 * @throws Exception the exception
	 */
	private void copyFilesInList() throws Exception {
		System.out.println("-I- Copying specified files");
		for (FileCopyInfo copyInfo : copyList) {
			ArrayList<Path> copySourcePaths = new ArrayList<>();
			Path destPath = Paths.get(repoPath,copyInfo.getDest());
			checkCopySourceDestPaths(copyInfo, copySourcePaths,destPath);
			
			for (Path sourcePath : copySourcePaths) {
				File sourceFile = sourcePath.toFile();
				if (!sourceFile.isDirectory()) {
					Path copyPath = Paths.get(destPath.toString(),sourceFile.getName());
					System.out.println("-I- Copying "+sourcePath.toString()+" to "+copyPath.toString());
					try {
						Files.copy(sourcePath, copyPath,StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						System.out.println("-E- Exception occurred while copying "+sourcePath.toString()+" to "
					                       +copyPath.toString());
						throw new Exception();
					}
				} else {
					System.out.println("-I- Copying "+sourcePath.toString()+" to "+destPath.toString());
					try {
						copyDirectory(sourcePath.toString(),destPath.toString()+"/"+sourceFile.getName());
					} catch (IOException e) {
						throw new Exception();
					}
				}
			}	
		}
	}
	
	/**
	 * Sets the up test.
	 *
	 * @param test the new up test
	 * @throws Exception the exception
	 */
	private void setupTest(ExeTest test) throws Exception {
		gr.writeBuildGradleFile(repoPath, test.getTestMode(), test.getSourcePath(), 
                test.getTestName(), test.getLib());
		Path destPath = Paths.get(repoPath,"src",test.getSourcePath(),test.getTestName()+".java");
		if (!test.isUser() && "test".equals(test.getTestMode())) {
			Path srcPath = Paths.get(path,test.getTestName()+".java");
			System.out.println("-I- Copy test "+test.getTestName()+" to "+destPath.toString());
			Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
		}
		File testFile = destPath.toFile();
		if (!testFile.exists()) {
			System.out.println("-E- Test "+test.getTestName()+" does not exist in source directory");
			throw new Exception();
		}
	}
	
	/**
	 * Execute test.
	 *
	 * @param test the test
	 */
	private void executeTest(ExeTest test) {
		String[] cmd = {gradleCmd[0],gradleCmd[1],gradleCmd[2]+" clean"};
		System.out.println("-I- Executing Gradle clean\n");
		ProcessResults results = executeProcess(cmd,new File(repoPath),0);
		results.printOutput();
		if ("test".equals(test.getTestMode())) {
			String[] testCmd = {gradleCmd[0],gradleCmd[1],gradleCmd[2]+" test --tests "+ test.getTestName()};
			System.out.println("-I- Executing Gradle test: "+test.getTestName());
			results = executeTimeoutProcess(testCmd, test.getTestName(), new File(repoPath),test.getTimeout());
			results.printOutput();
		} else {
			String[] runCmd = {gradleCmd[0],gradleCmd[1],gradleCmd[2]+" run"};
			System.out.println("-I- Executing Gradle run: "+test.getTestName());
			results = executeTimeoutProcess(runCmd, test.getTestName(), new File(repoPath),test.getTimeout());
			results.printOutput();			
		}
		updateTestResultOutput(test,results);
		if (results.getStatus()==-2) results.getOutput().add(test.getTestName()+" > Timed Out FAILED");
		writeLogFile(test.getTestName(),results.getOutput());
		updateTestResultsHash(test, results);
	}
	
	/**
	 * Record detailed test results.
	 *
	 * @param testName the test name
	 * @param subTestName the sub test name
	 * @param index the index
	 * @param result the result
	 */
	void recordDetailedTestResults(String testName, String subTestName, int index, String result) {
		if (detailTestOrderMap == null) {
			detailTestOrderMap = new HashMap<String, ArrayList<String>>();
			detailedTestResults = new HashMap<String,HashMap<String, ArrayList<String>>>();
		}
		if (index == 0) {
			if (!detailedTestResults.containsKey(currRepo)) 
				detailedTestResults.put(currRepo, new HashMap<String, ArrayList<String>>());
			if (!detailedTestResults.get(currRepo).containsKey(testName)) 
				detailedTestResults.get(currRepo).put(testName,new ArrayList<String>() );
			if (!detailTestOrderMap.containsKey(testName))
				detailTestOrderMap.put(testName, new ArrayList<String>());
		}
		if (detailTestOrderMap.get(testName).size() == index) 
			detailTestOrderMap.get(testName).add(subTestName);
		if (detailTestOrderMap.get(testName).get(index).equals(subTestName)) {
			detailedTestResults.get(currRepo).get(testName).add(subTestName+":"+result);
		} else {
			System.out.println("-E- Detected mismatch in test execution for JUnit Test "+testName);
			System.out.println("    Expected subtest "+detailTestOrderMap.get(currRepo).get(index));
			System.out.println("    Actual subtest "+subTestName);
		}
	}
	
	/**
	 * Decode test results.
	 *
	 * @param test the test
	 * @param results the results
	 * @return true, if successful
	 */
	private boolean decodeTestResults(ExeTest test, ProcessResults results) {
		String testName = test.getTestName();
		ArrayList<String> resultsLog = results.getOutput();
		int exeStatus = results.getStatus();
		boolean testRun = test.getTestMode().equals("run");
		ExeTestResults exeTestResults = new ExeTestResults(this,testRun,exeStatus);
		return exeTestResults.processTestResults(testName,resultsLog);
	}
	
	/**
	 * Update test results hash.
	 *
	 * @param test the test
	 * @param results the results
	 */
	private void updateTestResultsHash(ExeTest test, ProcessResults results) {
		boolean exeResult = decodeTestResults(test,results);
		if (!testResults.containsKey(currRepo))
			testResults.put(currRepo, new HashMap<String,String>());
		testResults.get(currRepo).put(test.getTestName(), (exeResult)?"PASSED":"FAILED");
	}
	
	/**
	 * Write log file.
	 *
	 * @param testName the test name
	 * @param outputLines the output lines
	 */
	private void writeLogFile(String testName, ArrayList<String> outputLines) {
		File log = new File(repoPath +"/"+testName+".log");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(log));
			for (String line : outputLines) {
				bw.write(line);
				bw.write("\n");
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Read file into array list.
	 *
	 * @param file the file
	 * @return the array list
	 */
	private ArrayList<String> readFileIntoArrayList(File file) {
		ArrayList<String> data = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine())!=null) {
				data.add(line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	/**
	 * Compare expect and actual outputs.
	 *
	 * @param expect the expect
	 * @param actual the actual
	 * @param test the test
	 * @param output the output
	 */
	private void compareExpectAndActualOutputs(File expect, File actual, ExeTest test, ArrayList<String> output) {
		ArrayList<String> expData = readFileIntoArrayList(expect);
		ArrayList<String> actData = readFileIntoArrayList(actual);
		if (expData.size() != actData.size()) {
			output.add(test.getTestName()+" > "+test.getCmp() + " Size Check FAILED");
			output.add("  E:   "+expData.size()+" lines, A:   "+actData.size()+" lines.");
			return;
		}
		for (int i = 0; i < expData.size(); i ++) {
			if (!expData.get(i).equals(actData.get(i))) {
				output.add(test.getTestName()+" > "+test.getCmp() + " Mismatch FAILED");
				output.add("   E: "+expData.get(i));
				output.add("   A: "+actData.get(i));
				return;
			}
		}
		output.add(test.getTestName()+" > PASSED");
	}
	
	/**
	 * Update test result output.
	 *
	 * @param test the test
	 * @param results the results
	 */
	private void updateTestResultOutput(ExeTest test, ProcessResults results) {
		if ("run".equals(test.getTestMode())) {
			if ("".equals(test.getCmp())) {
				results.getOutput().add(test.getTestName()+" > PASSED");
				return;
			}
			File expFile = new File(path + "/"+test.getCmp().replaceAll(".*/", ""));
			if (!expFile.exists()) { 
				results.getOutput().add(test.getTestName()+" > Failed");
				results.getOutput().add("   Expect file "+expFile.getName()+" does not exist in "+path);
				return;					
			}
			File actFile = new File(repoPath + "/"+test.getCmp());
			if (!actFile.exists()) { 
				results.getOutput().add(test.getTestName()+" > Failed");
				results.getOutput().add("   Test did not create output file: "+test.getCmp());
				return;					
			}
			compareExpectAndActualOutputs(expFile, actFile, test, results.getOutput());
		} 
	}
	
	/**
	 * Clean up repo.
	 */
	private void cleanUpRepo() {
		File del = new File(repoPath+"/build.gradle");
		if (del.exists()) {
			if (!del.delete()) {
			System.out.println("-I- Unable to remove file: "+del.getPath());
			}
		}
		del = new File(repoPath+"/build");
		if (!deleteDir(del)) {
			System.out.println("-I- Unable to remove directory: "+del.getPath());
		}
		del = new File(repoPath+"/.gradle");
		if (!deleteDir(del)) {
			System.out.println("-I- Unable to remove directory: "+del.getPath());
		}
		if (!noRestore) {
			File repoDir = new File(repoPath);
			System.out.println("-I- Executing git restore on "+repoDir.getName());
			String[] cmd = {"git","restore","."};
			ProcessResults results = executeProcess(cmd,repoDir,0);
			if (results.getStatus() != 0) {
				System.out.println("-I- git restore failed for repo: "+repoDir.getName());
			}
		}
	}
	
	/**
	 * Execute VIM.
	 *
	 * @param test the test
	 */
	private void executeVIM(ExeTest test) {
		System.out.println("-I- Execute VIM not implemented yet");
		return;
	}
	
	private ArrayList<String> getBranchList() {
		String[] cmd = {"git","branch"};
		ProcessResults results = executeProcess(cmd,new File(repoPath),0);
		return results.getOutput();
	}
	
	private boolean switchToBranch() {
		ArrayList<String> branches = getBranchList();
		return true;
	}
	
	private boolean processRepo(String url,String repo) {
		boolean status = true;
		if (repoRequiresCloning(url,currRepo)) {
			status = cloneRepo(url,repo);
			if (!status) {
				resetRepoTestHistory(repo);
				return status;
			}
		}
		if (!"".equals(branch)) status = switchToBranch();
		if (status && !"".equals(date)) status = checkoutByDate();
		return status;
	}

	/**
	 * Execute repo.
	 *
	 * @param url the url
	 */
	private void executeRepo(String url) {
		currRepo = url.replaceAll(".git$", "").replaceAll("^.*\\/", "");
		repoPath = path + "/" + currRepo;
		System.out.println("-I- Executing Repo: "+currRepo);
		if (!processRepo(url,currRepo)) return;
		try {
			if (deleteList !=null) deleteFilesInList();
			if (copyList != null)  copyFilesInList();
		} catch (Exception e) {
			return;
		}
		for (ExeTest test : testList) {
			try {
				if (!"vim".equals(test.getTestMode())) {
					setupTest(test);
					executeTest(test);
				} else {
					executeVIM(test);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		cleanUpRepo();
	}
	
	/**
	 * Sets the gradle command.
	 */
	private void setGradleCommand() {
		if (isWindows()) 
			gradleCmd = new String[] {"cmd","/c","gradle"};
		else 
			gradleCmd = new String[] {"/bin/sh","-c",System.getProperty("user.home")+"/bin/gradle"};
	}
	
	/**
	 * Execute flow.
	 */
	private void executeFlow() {
		setGradleCommand();
		getURLs(org,assignment,tag);
		processHistoryFile();
		try {
			gr = new GradleTools(path);
		} catch (Exception e) {
			System.out.println("-E- Exception occured while reading gradle.template. Aborting");
			System.exit(6);
		}
		for (String url : repoURLs) {
			executeRepo(url);
		}
		writeHistoryFile();
		writeDetailedTestResults();
	}

	/**
	 * Write detailed header.
	 *
	 * @param bw the bw
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void writeDetailedHeader(BufferedWriter bw) throws IOException {
		bw.write("Repository");
		for (ExeTest test : testList ) {
			String testName = test.getTestName();
			for (String subTestName : detailTestOrderMap.get(testName)) {
				bw.write(","+testName);
				if (!testName.equals(subTestName)) bw.write(":"+subTestName);
			}
		}
		bw.write("\n");		
	}

	/**
	 * Write detailed repo results.
	 *
	 * @param bw the bw
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void writeDetailedRepoResults(BufferedWriter bw) throws IOException {
		bw.write(currRepo);
		boolean repoExists = detailedTestResults.containsKey(currRepo);
		for (ExeTest test : testList ) {
			String testName = test.getTestName();
			for (String subTestName : detailTestOrderMap.get(testName)) {
				int index = 0;
				if (repoExists) {
					if (detailedTestResults.get(currRepo).containsKey(testName)) {
						if (detailedTestResults.get(currRepo).get(testName).get(index).matches(subTestName+":.*")) {
							String result = detailedTestResults.get(currRepo).get(testName).get(index)
									                           .replaceAll(".*:(.*)","$1");
							bw.write(","+result);
						} else {
							bw.write(",-");						
						}
					} else {
						bw.write(",-");
					}
				} else {
					bw.write(",-");
				}
				index++;
			}

		}
		bw.write("\n");		
	}
	
	/**
	 * Write detailed test results.
	 */
	private void writeDetailedTestResults() {
		File detailFile = new File(path+"/testResults.csv");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(detailFile));
			writeDetailedHeader(bw);
			for (String url : repoURLs) {
				currRepo = url.replaceAll(".git$", "").replaceAll("^.*\\/", "");
				writeDetailedRepoResults(bw);
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Write history file.
	 */
	private void writeHistoryFile() {
		File logFile = new File(runLog);
		try {
			if (logFile.exists()) {
				Files.move(Paths.get(runLog), Paths.get(runLog+".save"), StandardCopyOption.REPLACE_EXISTING);
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(logFile));
			bw.write("Repository");
			for (ExeTest test : testList ) {
				bw.write(","+test.getTestName());
			}
			bw.write("\n");
			
			for (String repo : new TreeSet<String>(testResults.keySet())) {
				bw.write(repo);
				for (ExeTest test : testList ) {
					if (testResults.containsKey(repo)) {
						if (testResults.get(repo).containsKey(test.getTestName())) {
							bw.write(","+testResults.get(repo).get(test.getTestName()));
						} else bw.write(",-");
					} else bw.write(",-");
				}
				bw.write("\n");
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Execute post commands.
	 */
	private void executePostCommands() {
		if (postCommands == null) return;
		for (String line: postCommands) {
			String[] command = line.split("\\s+");
			System.out.println("-I- Executing PostCommand: "+line);
			ProcessResults results = executeProcessPostCmd(command,new File(path));
		}
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ExeGHRepos jExe = new ExeGHRepos();
		jExe.processArgs(args);
		jExe.readConfigFile();
		jExe.executeFlow();
		jExe.executePostCommands();
	}

	/**
	 * Gets the org.
	 *
	 * @return the org
	 */
	public String getOrg() {
		return org;
	} 

	/**
	 * Sets the org.
	 *
	 * @param org the new org
	 */
	public void setOrg(String org) {
		this.org = org;
	}

	/**
	 * Gets the assignment.
	 *
	 * @return the assignment
	 */
	public String getAssignment() {
		return assignment;
	}

	/**
	 * Sets the assignment.
	 *
	 * @param assignment the new assignment
	 */
	public void setAssignment(String assignment) {
		this.assignment = assignment;
	}

	/**
	 * Gets the tag.
	 *
	 * @return the tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Sets the tag.
	 *
	 * @param tag the new tag
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * Gets the repo URL.
	 *
	 * @return the repo URL
	 */
	public ArrayList<String> getRepoURL() {
		return repoURLs;
	}
}
