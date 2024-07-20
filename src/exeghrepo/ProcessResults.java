package exeghrepo;

import java.util.ArrayList;

public class ProcessResults {
	ArrayList<String> output;
	int status;

	public ProcessResults() {
		output = new ArrayList<String>();
		status = -1;
	}

	public ArrayList<String> getOutput() {
		return output;
	}

	public void setOutput(ArrayList<String> output) {
		this.output = output;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	public void printOutput() {
		for (String line : output) {
			System.out.println(line);
		}
	}
}
