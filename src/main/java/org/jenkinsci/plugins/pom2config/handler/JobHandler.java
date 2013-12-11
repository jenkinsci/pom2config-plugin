package org.jenkinsci.plugins.pom2config.handler;

import hudson.Extension;
import hudson.model.AbstractProject;

import java.io.IOException;
import java.util.ArrayList;
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
public class JobHandler extends Pom2ConfigHandler {

	public JobHandler() {
		super(new JobHandlerSpec());
	}

	public static class JobHandlerSpec extends Pom2ConfigHandlerSpec {

		private static final Logger LOG = Logger.getLogger(JobHandler.class.getName());

		private final String descLabel = "Project Description";

		@Override
		public String getName() {
			return "Values which every Job has";
		}

		@Override
		public boolean isLoaded() {
			return true;
		}
		
		@Override
		public boolean isActivatedInJob(AbstractProject<?, ?> project) {
			return true;
		}

		public List<DataSet> parsePom(AbstractProject<?, ?> project, Document doc) throws IOException {
			List<String> oldDescription = new ArrayList<String>();
			oldDescription.add(project.getDescription());

			DataSet description = new DataSet(descLabel, true, oldDescription,
					retrieveDetailsFromPom(doc, "//description/text()"));
			this.pomValues.add(description);

			return this.pomValues;
		}

		@Override
		public String setDetails(AbstractProject<?, ?> project, JSONObject formData) {

			final String newDescription = formData.getJSONObject(descLabel).getString("pomEntry").trim();

			if (!newDescription.isEmpty()) {

				try {
					project.setDescription(formData.getJSONObject(descLabel)
							.getString("pomEntry").trim());
				} catch (IOException ex) {
					LOG.finest("Unable to change project description."
							+ ex.getMessage());
					return "Description not replaced";
				}
				return "Description replaced";
			} else {
				return "Description not replaced";
			}

		}
	}
}
