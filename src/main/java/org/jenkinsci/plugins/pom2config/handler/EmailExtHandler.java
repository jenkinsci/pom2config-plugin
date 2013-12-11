package org.jenkinsci.plugins.pom2config.handler;

import hudson.Extension;
import hudson.PluginWrapper;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.plugins.emailext.ExtendedEmailPublisher;

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
public class EmailExtHandler extends Pom2ConfigHandler {

	public EmailExtHandler() {
		super(new EmailExtHandlerSpec());
	}



	public static class EmailExtHandlerSpec extends Pom2ConfigHandlerSpec {

		private static final Logger LOG = Logger.getLogger(EmailExtHandler.class.getName());

		private final String developerEmail = "Developer Email Addresses";

		@Override
		public String getName() {
			return "Email-ext";
		}

		@Override
		public boolean isLoaded() {
			PluginWrapper plugin = Hudson.getInstance().pluginManager
					.getPlugin("email-ext");
			if (plugin != null && plugin.isActive()) {
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		public boolean isActivatedInJob(AbstractProject<?, ?> project) {
			ExtendedEmailPublisher publisher = project.getPublishersList().get(ExtendedEmailPublisher.class);
			if (publisher == null) {
				return false;
			} else {
				return true;
			}

		}

		public String getProjectRecipients(AbstractProject<?, ?> project) throws IOException {
			String recipients = "";

			ExtendedEmailPublisher publisher = project.getPublishersList().get(ExtendedEmailPublisher.class);
			recipients = publisher.recipientList;
			if (recipients.isEmpty()) {
				recipients = "No email recipients set.";
			}
			return recipients;
		}

		public void replaceEmailAddresses(AbstractProject<?, ?> project,
				String newAddresses) throws IOException {
			String addresses = newAddresses.trim().replace(" ", ",");
			ExtendedEmailPublisher publisher = project.getPublishersList().get(
					ExtendedEmailPublisher.class);
			publisher.recipientList = addresses;
			project.save();
		}

		@Override
		public List<DataSet> parsePom(AbstractProject<?, ?> project,
				Document doc) throws IOException {
			DataSet emailAddresses;
			List<String> oldAdresses = new ArrayList<String>();
			if (isActivatedInJob(project)) {

				oldAdresses.add(getProjectRecipients(project));

				emailAddresses = new DataSet(this.developerEmail, true,
						oldAdresses, retrieveDetailsFromPom(doc,
								"//developers/developer/email/text()"));

			} else {
				oldAdresses.add("not activated in job!");
				emailAddresses = new DataSet(this.developerEmail, false,
						oldAdresses, "not activated in job!");

			}
			this.pomValues.add(emailAddresses);

			return this.pomValues;
		}

		@Override
		public String setDetails(AbstractProject<?, ?> project,
				JSONObject formData) {

			final String newEmail = formData.getJSONObject(this.developerEmail)
					.getString("pomEntry").trim();
			if (!newEmail.isEmpty()) {
				try {
					replaceEmailAddresses(project, newEmail);
				} catch (IOException e) {
					LOG.finest("Unable to change Email Addresses"
							+ e.getMessage());
					return "Email Addresses not replaced";
				}
				return "Email Addresses replaced";
			} else {
				return "Email Addresses not replaced";
			}
		}

	}
}
