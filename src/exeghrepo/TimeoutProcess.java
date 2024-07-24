package exeghrepo;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class TimeoutProcess implements Runnable {
	ExeGHRepos exeTask;
	String[] cmd;
	String testName;
	File directory;
	String path;
	boolean terminate = false;
	ProcessResults results;
	File log;
	
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
	
	public void setTerminate() {
		terminate = true;
	}

	public void run() {
		InputStreamReader isr=null;
		String output="";
		char[] cbuf = new char[4096];
		int bufSize;
		ProcessBuilder pb = new ProcessBuilder(cmd);
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
	
	private ArrayList<String> processOutputString(String output) {
		ArrayList<String> aryOutput = new ArrayList<>();
		String[] lines = output.split("\\R");
		for (String line : lines) {
			aryOutput.add(line);
		}
		return aryOutput;		
	}
	
 }
