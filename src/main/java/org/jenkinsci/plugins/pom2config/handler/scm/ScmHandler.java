package org.jenkinsci.plugins.pom2config.handler.scm;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.plugins.git.GitSCM;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.ModuleLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.jenkinsci.plugins.pom2config.DataSet;
import org.jenkinsci.plugins.pom2config.Pom2ConfigHandler;
import org.w3c.dom.Document;

/**
 * @author Kathi Stutz, Michael Klein
 */
@Extension
public class ScmHandler extends Pom2ConfigHandler {

	public ScmHandler() {
		super(new ScmHandlerSpec());
	}

	public static class ScmHandlerSpec extends Pom2ConfigHandlerSpec {

		private static final Logger LOG = Logger.getLogger(ScmHandler.class.getName());

		private final String scmLabel = "SCM URLs";

		private ScmGit gitHandler = null;

		@Override
		public String getName() {
			return "SCM";
		}

		@Override
		public boolean isLoaded() {
			return true;
		}

		@Override
		public boolean isActivatedInJob(AbstractProject<?, ?> project) {
			if (project.getScm() instanceof NullSCM) {
				return false;
			} else {
				return true;
			}
		}

		/**
		 * Checks if the git plugin is loaded in Jenkins
		 *
		 * @return true if loaded, false when not
		 */
		private boolean isGitAvailable() {
			try {
				new GitSCM("http:/test/foo.git");
				if (gitHandler == null) {
					gitHandler = new ScmGit();
				}
				return true;
			} catch (Throwable t) {
				return false;
			}
		}

		/**
		 * Finds the SCM locations for a project.
		 *
		 * @return List of found SCM paths
		 */
		protected List<String> getSCMPaths(AbstractProject<?, ?> project) {
			final List<String> scmPaths = new ArrayList<String>();
			final SCM scm = project.getScm();

			if (scm instanceof SubversionSCM) {
				final SubversionSCM svn = (SubversionSCM) scm;
				for (ModuleLocation location : svn.getLocations()) {
					scmPaths.add(location.remote);
				}
				return scmPaths;
			} else if (isGitAvailable() && scm instanceof GitSCM) {
				return gitHandler.getGitPaths(project);
			}
			scmPaths.add("");
			return scmPaths;
		}

		/**
		 * Replaces the SCM URL in the job configuration with the new one from
		 * the pom
		 *
		 * @param project
		 * @param oldScmUrl
		 *            , the old SCM URL from the job configuration
		 * @param newScmUrl
		 *            . the new SCM URL from the pom
		 * @throws IOException
		 */
		protected String replaceScmUrl(AbstractProject<?, ?> project, String oldScmUrl, String newScmUrl) {
			final String[] scmParts = newScmUrl.split(":");
			String scmUrl = "";
			String prefix = "";
			StringBuilder scmUrlBuilder = new StringBuilder();
			String message = "Unable to replace SCM URL";

			if (!"scm".equals(scmParts[0].trim())) {
				LOG.finest("No SCM address");
				return "Unable to replace SCM URL - No valid SCM address!";
			}
			if ("git".equals(scmParts[1]) && !isGitAvailable()) {
				return "Unable to replace Git URL - Git Plugin is not activated in the job!";
			}

			for (int i = 2; i < scmParts.length; i++) {
				scmUrlBuilder.append(prefix);
				prefix = ":";
				scmUrlBuilder.append(scmParts[i].trim());
			}
			scmUrl = scmUrlBuilder.toString();

			if ("git".equals(scmParts[1])) {
				try {
					message = gitHandler.replaceGitUrl(project, oldScmUrl, scmUrl);
				} catch (IOException e) {
					LOG.finest("Unable to replace Git URL!" + e.getMessage());
				}
			} else if ("svn".equals(scmParts[1])) {
				try {
					message = replaceSvnUrl(project, oldScmUrl, scmUrl);
				} catch (IOException e) {
					LOG.finest("Unable to replace SVN URL!" + e.getMessage());
				}
			}
			return message;
		}

		/**
		 * Replaces the SVN URL in the job configuration with the new one from
		 * the pom
		 *
		 * @param project
		 * @param oldScmUrl
		 *            , the old SVN URL from the job configuration
		 * @param newScmUrl
		 *            . the new SVN URL from the pom
		 * @throws IOException
		 */
		private String replaceSvnUrl(AbstractProject<?, ?> project, String oldScmUrl, String newScmUrl) throws IOException {
			final SubversionSCM newSCM;
			final SCM scm = project.getScm();
			if (scm instanceof SubversionSCM) {
				final SubversionSCM svnSCM = (SubversionSCM) scm;
				final List<ModuleLocation> oldLocations = new ArrayList<ModuleLocation>(
						Arrays.asList(svnSCM.getProjectLocations(project)));

				for (ModuleLocation location : oldLocations) {
					if (location.remote.trim().equals(oldScmUrl)) {
						oldLocations.remove(location);
						break;
					}
				}

				final List<ModuleLocation> locationList = new ArrayList<ModuleLocation>();
				final ModuleLocation location = new ModuleLocation(newScmUrl, null);
				locationList.add(location);
				locationList.addAll(oldLocations);

				newSCM = new SubversionSCM(locationList,
						svnSCM.getWorkspaceUpdater(), svnSCM.getBrowser(),
						svnSCM.getExcludedRegions(), svnSCM.getExcludedUsers(),
						svnSCM.getExcludedRevprop(),
						svnSCM.getExcludedCommitMessages(),
						svnSCM.getIncludedRegions(),
						svnSCM.isIgnoreDirPropChanges(),
						svnSCM.isFilterChangelog());
			} else {
				newSCM = new SubversionSCM(newScmUrl);
			}
			project.setScm(newSCM);
			project.save();
			return "SVN Url replaced";
		}

		@Override
		public List<DataSet> parsePom(AbstractProject<?, ?> project, Document doc) throws IOException {
			DataSet scmUrls;
			scmUrls = new DataSet(scmLabel, isActivatedInJob(project),
					getSCMPaths(project), retrieveDetailsFromPom(doc,
							"//scm/connection/text()"));
			this.pomValues.add(scmUrls);

			return this.pomValues;
		}

		@Override
		public String setDetails(AbstractProject<?, ?> project, JSONObject formData) {

			final String newScm = formData.getJSONObject(scmLabel)
					.getString("pomEntry").trim();
			final String oldScm = formData.getJSONObject(scmLabel)
					.getString("configEntry").trim();
			if (!newScm.isEmpty()) {
				return replaceScmUrl(project, oldScm, newScm);
			} else {
				return "SCM Url not replaced!";
			}
		}

	}

}
