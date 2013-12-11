package org.jenkinsci.plugins.pom2config.handler.scm;

import hudson.model.AbstractProject;
import hudson.plugins.git.GitSCM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * @author Kathi Stutz, Michael Klein
 */
public class ScmGit {

	/**
	 * Finds the Git locations for a project.
	 *
	 * @return List of found Git paths
	 */
	protected List<String> getGitPaths(AbstractProject<?, ?> project) {
		final List<String> gitPaths = new ArrayList<String>();
		final GitSCM git = (GitSCM) project.getScm();
		StringBuilder sb = new StringBuilder();
		int i = 0;

		for (RemoteConfig repo : git.getRepositories()) {
			for (URIish uriIsh : repo.getURIs()) {
				if (i >= 1) {
					sb.append(", ");
				}
				sb.append(uriIsh.toString());
				i++;
			}
		}
		gitPaths.add(sb.toString());
		return gitPaths;
	}

	/**
	 * Replaces the Git URL in the job configuration with the new one from the
	 * pom
	 *
	 * @param project
	 * @param oldScmUrl
	 *            , the old Git URL from the job configuration
	 * @param newScmUrl
	 *            . the new Git URL from the pom
	 * @return
	 * @throws IOException
	 */
	protected String replaceGitUrl(AbstractProject<?, ?> project,
			String oldScmUrl, String newScmUrl) throws IOException {
		GitSCM newSCM = null;

// Since the mayor refactoring of version 2.0 of the git plugin I haven't found a way to simple replace
// the URL but keep the other properties of the repo.

//		SCM scm = project.getScm();
//		if (scm instanceof GitSCM) {
//			GitSCM gitSCM = (GitSCM) scm;
//			final List<UserRemoteConfig> oldRemoteConfigs = new ArrayList<UserRemoteConfig>(
//					gitSCM.getUserRemoteConfigs());
//
//			if(oldRemoteConfigs.size() < 1){
//
//				for (UserRemoteConfig config : oldRemoteConfigs) {
//					if (config.getUrl().trim().equals(oldScmUrl)) {
//					//	oldRemoteConfigs.remove(config);
//						gitSCM.getUserRemoteConfigs().remove(config);
//						break;
//					}
//				}
//
////				final List<UserRemoteConfig> newRemoteConfigList = new ArrayList<UserRemoteConfig>();
////				newRemoteConfigList.addAll(oldRemoteConfigs);
//
//				// Reflection to determine if git plugin version < 2.0 or >= 2.0
//				// is loaded in Jenkins
//				// I dont like reflections but I see no other way at then
//				// moment, because of the constructor changes since version 2.0
//				// Other suggestions are welcome!
//				Class<?> cl = null;
//				Constructor<?> cons = null;
//				boolean returnValue;
//				try {
//					cl = Class.forName("hudson.plugins.git.UserRemoteConfig");
//				} catch (ClassNotFoundException e) {
//					e.printStackTrace();
//					return "Fatal Error Git calss not found!";
//				}
//
//				// Try git plugin version < 2.0
//				try {
//					cons = cl.getConstructor(String.class, String.class, String.class);
//					gitSCM.getUserRemoteConfigs().add((UserRemoteConfig) cons.newInstance(newScmUrl, null, null));
//
//				} catch (NoSuchMethodException e) {
//					// Try git plugin >= 2.0
//					try {
//						cons = cl.getConstructor(String.class, String.class, String.class, String.class);
//						gitSCM.getUserRemoteConfigs().add((UserRemoteConfig) cons.newInstance(newScmUrl, null, null, null));
//					} catch (NoSuchMethodException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					} catch (Exception e1) {
//						// something is wrong
//						e1.printStackTrace();
//					}
//
//				} catch (Exception e) {
//					// something is wrong
//					e.printStackTrace();
//				}
//
//				// newSCM = new GitSCM(gitSCM.getScmName(), newRemoteConfigList,
//				// gitSCM.getBranches(), gitSCM.getUserMergeOptions(),
//				// gitSCM.getDoGenerate(), gitSCM.getSubmoduleCfg(),
//				// gitSCM.getClean(), gitSCM.getWipeOutWorkspace(),
//				// gitSCM.getBuildChooser(), gitSCM.getBrowser(),
//				// gitSCM.getGitTool(), gitSCM.getAuthorOrCommitter(),
//				// gitSCM.getRelativeTargetDir(), gitSCM.getReference(),
//				// gitSCM.getExcludedRegions(), gitSCM.getExcludedUsers(),
//				// gitSCM.getLocalBranch(), gitSCM.getDisableSubmodules(),
//				// gitSCM.getRecursiveSubmodules(), gitSCM.getPruneBranches(),
//				// gitSCM.getRemotePoll(), gitSCM.getGitConfigName(),
//				// gitSCM.getGitConfigEmail(), gitSCM.getSkipTag(),
//				// gitSCM.getIncludedRegions(), gitSCM.isIgnoreNotifyCommit(),
//				// gitSCM.getUseShallowClone());
//
//				// newRemoteConfigList.add(new UserRemoteConfig(newScmUrl, null,
//				// null));
//				// newRemoteConfigList.addAll(oldRemoteConfigs);
//			}
//
//			//newSCM = new GitSCM(newScmUrl);
//		} else {
//			System.out.println("make new GitSCM!!!!!!!!!!!!1");
//			newSCM = new GitSCM(newScmUrl);
//			project.setScm(newSCM);
//		}
		newSCM = new GitSCM(newScmUrl);
		project.setScm(newSCM);
		project.save();
		return "Git Url replaced";
	}
}
