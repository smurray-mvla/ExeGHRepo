package exeghrepo;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * The Class TimeoutProcess.
 */
public class TimeoutProcess implements Runnable {
	
	/** The exe task. */
	ExeGHRepos exeTask;
	
	/** The cmd. */
	String[] cmd;
	
	/** The test name. */
	String testName;
	
	/** The directory. */
	File directory;
	
	/** The path. */
	String path;
	
	/** The terminate. */
	boolean terminate = false;
	
	/** The results. */
	ProcessResults results;
	
	/** The log. */
	File log;
	
	/**
	 * Instantiates a new timeout process.
	 *
	 * @param exeTask the exe task
	 * @param cmd the cmd
	 * @param testName the test name
	 * @param directory the directory
	 * @param results the results
	 */
	public TimeoutProcess(ExeGHRepos exeTask, String[] cmd, String testName, File directory, ProcessResults results) {
		this.exeTask = exeTask;
		this.cmd = cmd;
		this.testName = testName;
		this.directory = directory;
		path = directory.getPath();
		this.results = results;
		log = new File(path+"/"+testName+".log");
		if (log.exists()) log.delete();
	}
	
	/**
	 * Sets the terminate.
	 */
	public void setTerminate() {
		terminate = true;
	}

	/**
	 * Run.
	 */
	public void run() {
		InputStreamReader isr=null;
		String output="";
		char[] cbuf = new char[4096];
		int bufSize;
		ProcessBuilder pb = new ProcessBuilder(cmd);
		Map<String,String> env = pb.environment();
		if (!env.containsKey("JAVA_HOME")) env.put("JAVA_HOME",System.getProperty("java.home"));
		pb.redirectErrorStream(true);
		long nextCheckTime = System.currentTimeMillis()+250;
		if (directory != null) pb.directory(directory);
		Process proc = null;
		try {
			proc = pb.start();
			isr = new InputStreamReader(proc.getInputStream());
			while (proc.isAlive()) {
				while (isr.ready()) {
					bufSize = isr.read(cbuf,0,4096);
					output+=new String(cbuf,0,bufSize);
				}
				if (System.currentTimeMillis()>nextCheckTime) {
					if (terminate) {
						proc.destroyForcibly();
						while (isr.ready()) {
							bufSize = isr.read(cbuf,0,4096);
							output+=new String(cbuf,0,bufSize);
						}
						isr.close();
						results.setOutput(processOutputString(output));
						results.setStatus(-2);
						return;
					} else 
						nextCheckTime+=250;
				}
			}	
			while (isr.ready()) {
				bufSize = isr.read(cbuf,0,4096);
				output+=new String(cbuf,0,bufSize);
			}
			results.setOutput(processOutputString(output));
		} catch (Exception e) {
			System.out.println("-E- Exception occured while running test");
			e.printStackTrace();
			results.setStatus(-1);
			results.setOutput(null);
		}
		results.setStatus(proc.exitValue());
	}
	
	/**
	 * Process output string.
	 *
	 * @param output the output
	 * @return the array list
	 */
	private ArrayList<String> processOutputString(String output) {
		ArrayList<String> aryOutput = new ArrayList<>();
		String[] lines = output.split("\\R");
		for (String line : lines) {
			aryOutput.add(line);
		}
		return aryOutput;		
	}
	
 }
