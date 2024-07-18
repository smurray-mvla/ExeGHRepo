package exeghrepo;

public class ExeTest {
	String testName;
	String testMode;
	String sourcePath;
	String lib;
	String cmp;
	String vim;
	boolean user;
	int    timeout;
	
	public ExeTest(String testName) {
		this.testName = testName;
		testMode = "";
		sourcePath = "";
		lib = "";
		cmp = "";
		vim = "";
		user = false;
		timeout = 20;
	}

	public String getTestMode() {
		return testMode;
	}

	public void setTestMode(String testMode) {
		this.testMode = testMode;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getLib() {
		return lib;
	}

	public void setLib(String lib) {
		this.lib = lib;
	}

	public String getCmp() {
		return cmp;
	}

	public void setCmp(String cmp) {
		this.cmp = cmp;
	}

	public String getVim() {
		return vim;
	}

	public void setVim(String vim) {
		this.vim = vim;
	}

	public boolean isUser() {
		return user;
	}

	public void setUser(boolean user) {
		this.user = user;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getTestName() {
		return testName;
	}
	
	
	
}
