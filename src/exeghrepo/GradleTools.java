package exeghrepo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

// TODO: Auto-generated Javadoc
/**
 * The Class GradleTools.
 */
public class GradleTools {
	
	/** The template. */
	private ArrayList<String> template = null;
	
	/**
	 * Instantiates a new gradle tools.
	 *
	 * @param path the path
	 * @throws Exception the exception
	 */
	public GradleTools(String path) throws Exception {
		template = readGradleTemplate(path);
	}
	
	/**
	 * Read gradle template.
	 *
	 * @param path the path
	 * @return the array list
	 * @throws Exception the exception
	 */
	private ArrayList<String> readGradleTemplate(String path) throws Exception {
		Path gradleTemplatePath = Paths.get(path,"gradle.template");
		File templateFile = gradleTemplatePath.toFile();
		String line;
		if (templateFile.exists()) {
			template = new ArrayList<>();
			try {
				BufferedReader br = new BufferedReader(new FileReader(templateFile));
				while ((line = br.readLine()) != null) {
					template.add(line);
				}
				br.close();
				if (template.isEmpty()) template = null;
			} catch (IOException e) {
				System.out.println("-E- Fatal Error while attempting to read gradle.template");
				throw new Exception();
			}
		}
		return template;
	}
	
	/**
	 * Write build gradle file.
	 *
	 * @param path the path
	 * @param command the command
	 * @param testSrcDir the test src dir
	 * @param testName the test name
	 * @param testLib the test lib
	 * @throws Exception the exception
	 */
	public void writeBuildGradleFile(String path,String command, String testSrcDir, String testName, String testLib)
				throws Exception {
		Path buildGradlePath = Paths.get(path,"build.gradle");
		File buildGradleFile = buildGradlePath.toFile();
		String editLine;
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(buildGradleFile));
			for (String line : template) {
				if (line.contains("MAIN_CLASS")) {
					if ("run".equals(command)) {
						editLine = "mainClassName = \'";
						if (!"".equals(testSrcDir)) editLine += testSrcDir + ".";
						editLine += testName + "\'";
						bw.write(editLine+"\n");
					}
				} else if (line.contains("IMP_FILES")) {
					if (!"".equals(testLib)) {
						editLine = "\timplementation files(\'"+testLib+"\')";
						bw.write(editLine+"\n");
						editLine = "\ttestImplementation files(\'"+testLib+"\')";
						bw.write(editLine+"\n");	
					}
					
				} else if (line.contains("SRC_DIR")) {
					if (!"".equals(testSrcDir)) {
						editLine = line.replaceAll("SRC_DIR", "src/"+testSrcDir+"\",\"src");
					} else {
						editLine = line.replaceAll("SRC_DIR", "src");
					}
					bw.write(editLine+"\n");
				} else 
					bw.write(line+"\n");
			}
			bw.flush();
			bw.close();
			System.out.println("-I- Created build.gradle for test "+testName+" in "+path);

		} catch (IOException e) {
			System.out.println("-E- Unable to create build.gradle file for test "+testName+" in "+path);
			throw new Exception();
		}
	}
}
