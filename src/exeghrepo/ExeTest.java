package exeghrepo;

// TODO: Auto-generated Javadoc
/**
 * The Class ExeTest.
 */
public class ExeTest {
	
	/** The test name. */
	String testName;
	
	/** The test mode. */
	String testMode;
	
	/** The source path. */
	String sourcePath;
	
	/** The lib. */
	String lib;
	
	/** The cmp. */
	String cmp;
	
	/** The vim. */
	String vim;
	
	/** The user. */
	boolean user;
	
	/** The timeout. */
	int    timeout;
	
	/**  command line arguments - if test mode is run. */
	String args;
	
	/**
	 * Instantiates a new exe test.
	 *
	 * @param testName the test name
	 */
	public ExeTest(String testName) {
		this.testName = testName;
		testMode = "";
		sourcePath = "";
		lib = "";
		cmp = "";
		vim = "";
		args = "";
		user = false;
		timeout = 20;
	}

	/**
	 * Gets the test mode.
	 *
	 * @return the test mode
	 */
	public String getTestMode() {
		return testMode;
	}

	/**
	 * Sets the test mode.
	 *
	 * @param testMode the new test mode
	 */
	public void setTestMode(String testMode) {
		this.testMode = testMode;
	}

	/**
	 * Gets the source path.
	 *
	 * @return the source path
	 */
	public String getSourcePath() {
		return sourcePath;
	}

	/**
	 * Sets the source path.
	 *
	 * @param sourcePath the new source path
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	/**
	 * Gets the lib.
	 *
	 * @return the lib
	 */
	public String getLib() {
		return lib;
	}

	/**
	 * Sets the lib.
	 *
	 * @param lib the new lib
	 */
	public void setLib(String lib) {
		this.lib = lib;
	}

	/**
	 * Gets the cmp.
	 *
	 * @return the cmp
	 */
	public String getCmp() {
		return cmp;
	}

	/**
	 * Sets the cmp.
	 *
	 * @param cmp the new cmp
	 */
	public void setCmp(String cmp) {
		this.cmp = cmp;
	}

	/**
	 * Gets the vim.
	 *
	 * @return the vim
	 */
	public String getVim() {
		return vim;
	}

	/**
	 * Sets the vim.
	 *
	 * @param vim the new vim
	 */
	public void setVim(String vim) {
		this.vim = vim;
	}

	/**
	 * Checks if is user.
	 *
	 * @return true, if is user
	 */
	public boolean isUser() {
		return user;
	}

	/**
	 * Sets the user.
	 *
	 * @param user the new user
	 */
	public void setUser(boolean user) {
		this.user = user;
	}

	/**
	 * Gets the timeout.
	 *
	 * @return the timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Sets the timeout.
	 *
	 * @param timeout the new timeout
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Gets the test name.
	 *
	 * @return the test name
	 */
	public String getTestName() {
		return testName;
	}

	/**
	 * Gets the args.
	 *
	 * @return the args
	 */
	public String getArgs() {
		return args;
	}

	/**
	 * Sets the args.
	 *
	 * @param args the new args
	 */
	public void setArgs(String args) {
		this.args = args;
	}
	
	
	
}
