package exeghrepo;

import java.util.ArrayList;

// TODO: Auto-generated Javadoc
/**
 * The Class ExeTestResults.
 */
public class ExeTestResults {
	
	/** The exe GH repo. */
	ExeGHRepos exeGHRepo;
	
	/** The exe status. */
	int exeStatus;
	
	/** The test run. */
	boolean testRun;
	
	/** The comp java. */
	boolean compJava = false;
	
	/** The comp junit. */
	boolean compJunit = false;
	
	/** The exe test. */
	boolean exeTest = false;
	
	/** The passed test. */
	int passedTest = 0;
	
	/** The failed test. */
	int failedTest = 0;
	
	/** The timed out. */
	boolean timedOut = false;
	
	/** The sub test name. */
	String subTestName = "";
	
	/**
	 * Instantiates a new exe test results.
	 *
	 * @param exeGHRepo the exe GH repo
	 * @param testRun the test run
	 * @param exeStatus the exe status
	 */
	public ExeTestResults(ExeGHRepos exeGHRepo, boolean testRun, int exeStatus) {
		this.exeGHRepo = exeGHRepo;
		this.exeStatus = exeStatus;
		this.testRun = testRun;
	}
	
	/**
	 * Process test results.
	 *
	 * @param testName the test name
	 * @param resultsLog the results log
	 * @return true, if successful
	 */
	boolean processTestResults(String testName, ArrayList<String> resultsLog) {
		boolean isGrader =  testName.contains("Grader");
		boolean isTest = testName.endsWith("Test");
		for (String line: resultsLog) {
			if (line.matches("^.*> Task\\s+:compileJava\\s*$")) compJava = true;
			if (line.matches("^.*> Task\\s+:compileTestJava\\s*$")) compJunit = true;
			if (line.matches("^.*> Task\\s+:test\\s*$")) exeTest = true;
			if ((line.matches("^.*"+testName+" > (\\S*)\\s?.*PASSED.*$") ||
				 (isGrader && line.matches("^.*Test.*PASSED.*")))) {
				subTestName = (!isGrader && isTest) ? line.replaceAll(".*> (\\S+)\\(.*","$1") : testName;
				exeGHRepo.recordDetailedTestResults(testName,subTestName,passedTest+failedTest,"PASSED");
				passedTest++;			}
			if (line.matches("^.*"+testName+" > .*Timed Out FAILED.*$")) timedOut = true;
			else if ((line.matches("^.*"+testName+" > (\\S*)\\s?.*FAILED.*$") ||
					 (isGrader && line.matches("^.*Test.*FAILED.*")))) {
				subTestName = (!isGrader) ? line.replaceAll(".*> (\\S+)\\(.*","$1") : testName;
				if (!subTestName.startsWith("test"))
					System.out.println("-I- Non-test related failure: "+line);
				else { 
					exeGHRepo.recordDetailedTestResults(testName,subTestName,passedTest+failedTest,"FAILED");
					failedTest++;
				}
			}
		}
		
		if (exeStatus != 0) System.out.println("-E- Gradle execution returned status: "+exeStatus);
		if (!compJava) System.out.println("-E- Gradle did not compile the Java source code");
		else if (!testRun && !compJunit) System.out.println("-E- Gradle did not compile the JUnit source code");
		else if (!testRun && !exeTest) System.out.println("-E- Gradle did not compile the JUnit source code");
		else if (failedTest>0) System.out.println("-E- There were "+failedTest+" test failures");
		else if (passedTest == 0) System.out.println("-E- There were no tests run --> failing");
		else if (timedOut) System.out.println("-E- Gradle Execution Timed Out");
		
		return (passedTest>0) && compJava && (testRun || (!testRun && compJunit && exeTest)) && (failedTest==0) 
				&& !timedOut; 		
	}
}
