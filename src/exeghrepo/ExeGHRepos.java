package exeghrepo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

public class ExeGHRepos {
	GitTools gt = new GitTools();
	GradleTools gr;
	private String org="";
	private String assignment="";
	private String tag = "";
	private String runLog = "";
	private String date = "";
	private String path = "";
	private String excludeRepo = "";
	private String localSHA = "";
	private String repoPath = "";
	private boolean checkout = false;
	private boolean incremental = false;
	private boolean gradleDebug = false;
	private boolean debug = false;
	private boolean noRestore = false;
	private boolean force = false;
	private boolean list = false;
	private boolean noClone = false;
	private int testIndex = -1;
	private ArrayList<String> deleteList;
	private ArrayList<FileCopyInfo> copyList;
	private ArrayList<String> postCommands;
	private ArrayList<ExeTest> testList;
	private ExeTest currTest;
	private HashMap<String,HashMap<String,String>> testResults;
	private ArrayList<String> repoURLs;
	private String[] gradleCmd;
    /**
     * Gets the operating system.
     *
     * @return the operating system
     */
    private boolean isWindows() {
    	return System.getProperty("os.name").contains("Win");
    }

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
			case "-d": date = getNextArg(args,i++); break;
			case "-x" : excludeRepo = getNextArg(args,i++); break;
			case "-i": incremental = true; break;
			case "-gd": gradleDebug = true; break;
			case "-D": debug = true; break;
			case "-nR": noRestore = true; break;
			case "-f": force = true; break;
			case "-L": list = true; break;
			case "-nC": noClone = true; break;
			default : {
						printArgsError("Unexpected argument: "+args[i]+". Expected valid switch",args);
						printUsage();
						System.exit(1);
					  }
			}
		}
		if ("".equals(path)) path = System.getProperty("user.dir");
	}
	
	private void processDelete(ArrayList<String> deleteArray) {
		for (String deleteRE : deleteArray) {
			if (!"".equals(deleteRE)) {
				if (deleteList == null) 
					deleteList = new ArrayList<>();
				deleteList.add(deleteRE);
			}
		}
	}
	
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

	private void processPostCommands(ArrayList<String> values) {
		String command = "";
		int i = 0;
		do {
			command += values.get(i);
			i++;
			if (i < values.size()) command += ":";
		} while (i < values.size());
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
		ArrayList<String> values = new ArrayList<String>(Arrays.asList(tokens));
		String option = values.remove(0);		
		if (values.isEmpty()) {
			System.out.println("-I- Option "+option+" not initialized correctly in .CONFIG file. Skipping");
			return;
		}
		switch (option) {
		case "ORG": org = values.get(0); return;
		case "ASSIGNMENT": assignment = values.get(0); return;
		case "DELETE": processDelete(new ArrayList<String>(values)); return;
		case "COPY": processCopy(new ArrayList<String>(values)); return;
		case "POSTCMD": processPostCommands(values); return;
		case "TEST": processTest(values.get(0)); return;
		default:updateCurrTestOption(option, values.get(0)); return;
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
		if (!testResults.containsKey(repo)) return false;
		HashMap<String,String> repoTestResults = testResults.get(repo);
		for (ExeTest test : testList) {
			if (!repoTestResults.get(test.getTestName()).equals("PASSED")) return true;
		}
		return false;
	}

	private String getRemoteHeadSHA(String url) {
		String[] cmd = {"git","ls-remote",url,"HEAD"};
		String SHA = null;
		ProcessResults results = executeProcess(cmd,null,false,0);
		if (results.getStatus() == 0)
			SHA = results.getOutput().get(0).replaceAll("\\s+HEAD", "");
		return SHA;
	}
	 
	private String getLocalHeadSHA() {
		String[] cmd = {"git","rev-parse","HEAD"};
		String SHA = null;
		ProcessResults results = executeProcess(cmd,new File(repoPath),false,0);
		if (results.getStatus() == 0)
			SHA = results.getOutput().get(0).replaceAll("\\s+HEAD", "");
		return SHA;
	}
	
	private String getSHAbyDate() {
		String[] cmd = {"git","log","-1","--until=\'"+date+"\'","--format=format:%H"};
		String SHA = null;
		ProcessResults results = executeProcess(cmd,new File(repoPath),false,0);
		if (results.getStatus() == 0)
			SHA = results.getOutput().get(0).replaceAll("\\s+.*", "");
		return SHA;
	}
	
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
	
	ArrayList<String> extractProcessOutput(BufferedReader br) throws Exception {
		String line;
		ArrayList<String> output = new ArrayList<>();
		while ((line = br.readLine())!=null) {
			output.add(line);
		}
		return output;
	}
	 
	private ProcessResults executeProcess(String[] cmd, File directory, boolean errorSrc, int timeout) {
		BufferedReader br;
		ProcessResults procResults = new ProcessResults();
		ProcessBuilder pb = new ProcessBuilder(cmd);
		if (directory != null) pb.directory(directory);
		try {
			Process proc = pb.start();
			if (errorSrc)
				br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			else
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return procResults;
	}
	
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
			ProcessResults results = executeProcess(cmd,new File(path),true,0);
			results.printOutput();
			status = results.getStatus();
			System.out.println("Attempt "+attempt+ " to clone "+repo+" completed with status = "+status);

		} while (status != 0 && attempt <= 5);
		return (status == 0);
	}
	
	private void resetRepoTestHistory(String repo) {
		if (testResults.containsKey(repo))
			for (ExeTest test : testList) {
				testResults.get(repo).put(test.getTestName(), "-");
			}
	}
	
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
	
	private void checkoutByDate() {
		localSHA = getLocalHeadSHA();
		String SHAbyDate = getSHAbyDate();
		checkout = false;
		
		if ((SHAbyDate == null) || (!localSHA.equals(SHAbyDate))) {
			String[] cmd = {"git","checkout",SHAbyDate};
			ProcessResults results = executeProcess(cmd,new File(repoPath),true,0);
			results.printOutput();
			if (results.getStatus() == 0) {
				checkout = !noRestore;
			}
		}
	}
	
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
	
	private void copyFilesInList() throws Exception {
		System.out.println("-I- Copying specified files");
		for (FileCopyInfo copyInfo : copyList) {
			ArrayList<Path> copySourcePaths = new ArrayList<>();
			if (!checkSourceFiles(copyInfo.getSource(),copySourcePaths)) {
				throw new Exception();
			}
			Path destPath = Paths.get(repoPath,copyInfo.getDest());
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
			
			for (Path sourcePath : copySourcePaths) {
				File sourceFile = sourcePath.toFile();
				if (!sourceFile.isDirectory()) {
					Path copyPath = Paths.get(destPath.toString(),sourceFile.getName());
					System.out.println("-I- Copying "+sourcePath.toString()+" to "+copyPath.toString());
					try {
						Files.copy(sourcePath, copyPath,StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						System.out.println("-E- Exception occurred while copying "+sourcePath.toString()+" to "+copyPath.toString());
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
	
	private void executeTest(ExeTest test) {
		String[] cmd = {gradleCmd[0],gradleCmd[1],"gradle","clean"};
		System.out.println("-I- Executing Gradle clean\n");
		ProcessResults results = executeProcess(cmd,new File(repoPath),false,0);
		results.printOutput();
		if ("test".equals(test.getTestMode())) {
			String[] testCmd = {gradleCmd[0],gradleCmd[1],"gradle","test","--tests",test.getTestName()};
			System.out.println("-I- Executing Gradle test: "+test.getTestName());
			results = executeTimeoutProcess(testCmd, test.getTestName(), new File(repoPath),test.getTimeout());
			results.printOutput();
		} else {
			String[] runCmd = {gradleCmd[0],gradleCmd[1],"gradle","run"};
			System.out.println("-I- Executing Gradle run: "+test.getTestName());
			results = executeTimeoutProcess(runCmd, test.getTestName(), new File(repoPath),test.getTimeout());
			results.printOutput();			
		}
	}

	private void executeRepo(String url) {
		String repo = url.replaceAll(".git$", "").replaceAll("^.*\\/", "");
		repoPath = path + "/" + repo;
		System.out.println("-I- Executing Repo: "+repo);
		boolean status = true;
		if (repoRequiresCloning(url,repo)) { 
			if (!cloneRepo(url,repo)) {
				resetRepoTestHistory(repo);
				return;
			}
		}
		if (!"".equals(date)) checkoutByDate();
		try {
			if (deleteList !=null) deleteFilesInList();
			if (copyList != null)  copyFilesInList();
			for (ExeTest test : testList) {
				if (!"vim".equals(test.getTestMode())) {
					setupTest(test);
					executeTest(test);
				} else {
					
				}
			}
		} catch (Exception e) {
			return;
		}
		
	}
	
	private void setGradleCommand() {
		if (isWindows()) 
			gradleCmd = new String[] {"cmd","/c"};
		else 
			gradleCmd = new String[] {"/bin/sh","-c"};
			
			
	}
	
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
