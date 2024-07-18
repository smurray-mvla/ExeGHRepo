package exeghrepo;

import java.io.IOException;
import java.util.ArrayList;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;

public class GitTools {
	GitHub gh = null;
	GHOrganization ghOrg = null;
	
	public GitTools() {
		try {
			gh = new GitHubBuilder().fromPropertyFile().build();
			ghOrg = gh.getOrganization("ADEN-LAHS");
		} catch (IOException e) {
			System.out.println("something bad happened");
			e.printStackTrace();
		}
	}
	
	
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
