package exeghrepo;

import java.io.IOException;
import java.util.ArrayList;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;

// TODO: Auto-generated Javadoc
/**
 * The Class GitTools.
 */
public class GitTools {
	
	/** The gh. */
	GitHub gh = null;
	
	/** The gh org. */
	GHOrganization ghOrg = null;
	
	/**
	 * Instantiates a new git tools.
	 *
	 * @param org the org
	 */
	public GitTools(String org) {
		try {
			gh = new GitHubBuilder().fromPropertyFile().build();
			ghOrg = gh.getOrganization(org);
		} catch (IOException e) {
			System.out.println("something bad happened");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Gets the repo URLs.
	 *
	 * @param organization the organization
	 * @param assignment the assignment
	 * @param tag the tag
	 * @return the repo UR ls
	 */
	ArrayList<String> getRepoURLs(String organization,String assignment, String tag) {
		ArrayList<String> urlList = new ArrayList<>();
		String match = organization + "/" + assignment;
		if (!"".equals(tag))
			match = match + "-"+ tag;
		match += ".*";
		System.out.println("-I- Getting all repos that match "+match);
		PagedIterable<GHRepository> repos = ghOrg.listRepositories();
		for (GHRepository repo : repos) {
			if (repo.getFullName().matches(match))
				urlList.add(repo.getSshUrl());
		}
		return urlList;
	}
	
	
	
}
