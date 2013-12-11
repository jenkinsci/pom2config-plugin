package org.jenkinsci.plugins.pom2config;

import hudson.ExtensionPoint;
import hudson.model.AbstractProject;

import java.io.IOException;
import java.util.List;

import net.sf.json.JSONObject;

import org.w3c.dom.Document;

/**
 * @author Michael Klein
 */
public interface Handler extends ExtensionPoint, Comparable<Handler>{

	/**
	 * Name of the plugin
	 * @return plugin name
	 */
	public String getName();

	/**
	 * List of found values in the pom
	 * @return found pom values
	 */
	public List<DataSet> getPomValues();

	/**
	 * Is the plugin loaded in Jenkins
	 * @return true if it is or no if not
	 */
	public boolean isLoaded();

	/**
	 * Parses the pom and looks for the searched values
	 * @param project
	 * @param doc the pom
	 * @return A List of {@link DataSet} for the found values
	 * @throws IOException
	 */
	public abstract List<DataSet> parsePom(AbstractProject<?,?> project, Document doc) throws IOException;

	/**
	 * Sets the selected values from the pom in the job configuration
	 * @param project
	 * @param formData
	 * @return A message if setting the values was successful or not
	 */
	public abstract String setDetails(AbstractProject<?,?> project, JSONObject formData);

}
