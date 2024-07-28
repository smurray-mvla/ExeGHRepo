package exeghrepo;

import java.util.ArrayList;

public class ExeTestResults {
	ExeGHRepos exeGHRepo;
	int exeStatus;
	boolean testRun;
	boolean compJava = false;
	boolean compJunit = false;
	boolean exeTest = false;
	int passedTest = 0;
	int failedTest = 0;
	boolean timedOut = false;
	String subTestName = "";
	
	public ExeTestResults(ExeGHRepos exeGHRepo, boolean testRun, int exeStatus) {
		this.exeGHRepo = exeGHRepo;
		this.exeStatus = exeStatus;
		this.testRun = testRun;
	}
	
	boolean processTestResults(String testName, ArrayList<String> resultsLog) {
		boolean grader =  testName.contains("Grader");
		for (String line: resultsLog) {
			if (line.matches("^.*> Task\\s+:compileJava\\s*$")) compJava = true;
			if (line.matches("^.*> Task\\s+:compileTestJava\\s*$")) compJunit = true;
			if (line.matches("^.*> Task\\s+:test\\s*$")) exeTest = true;
			if ((line.matches("^.*"+testName+" > (\\S*)\\s?.*PASSED.*$") ||
				 (grader && line.matches("^.*Test.*PASSED.*")))) {
				subTestName = (!grader) ? line.replaceAll(".*> (\\S+)\\(.*","$1") : testName;
				exeGHRepo.recordDetailedTestResults(testName,subTestName,passedTest+failedTest,"PASSED");
				passedTest++;			}
			if (line.matches("^.*"+testName+" > .*Timed Out FAILED.*$")) timedOut = true;
			else if ((line.matches("^.*"+testName+" > (\\S*)\\s?.*FAILED.*$") ||
					 (grader && line.matches("^.*Test.*FAILED.*")))) {
				subTestName = (!grader) ? line.replaceAll(".*> (\\S+)\\(.*","$1") : testName;
				exeGHRepo.recordDetailedTestResults(testName,subTestName,passedTest+failedTest,"FAILED");
				failedTest++;
			}
		}
		
		if (exeStatus != 0) System.out.println("-E- Gradle execution returned status: "+exeStatus);
		if (!compJava) System.out.println("-E- Gradle did not compile the Java source code");
		else if (!testRun && !compJunit) System.out.println("-E- Gradle did not compile the JUnit source code");
		else if (!testRun && !exeTest) System.out.println("-E- Gradle did not compile the JUnit source code");
		else if (failedTest>0) System.out.println("-E- There were "+failedTest+" test failures");
		else if (passedTest == 0) System.out.println("-E- There were no tests run --> failing");
		else if (timedOut) System.out.println("-E- Gradle Execution Timed Out");
		
		return (passedTest>0) && compJava && (!testRun && compJunit && exeTest) && (failedTest==0) && !timedOut; 		
	}
}
