package org.jenkinsci.plugins.pom2config;

import hudson.model.AbstractProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.json.JSONObject;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * @author Michael Klein
 */
public class Pom2ConfigHandler implements Handler{

	 public static abstract class Pom2ConfigHandlerSpec {

		 public abstract String getName();

		 protected List<DataSet> pomValues = new ArrayList<DataSet>();


		 public List<DataSet> getPomValues() {
				return this.pomValues;
			}

			/**
			 * Clears the current parsed pom values
			 */
			public void clearPomValues(){
				this.pomValues.clear();
			}

			/**
			 * Is the plugin activated in the job configuration
			 * @param project
			 * @return true if it is or no if not
			 */
			public abstract boolean isActivatedInJob(AbstractProject<?, ?> project);

			public abstract boolean isLoaded();

			public abstract List<DataSet> parsePom(AbstractProject<?,?> project, Document doc) throws IOException;

			public abstract String setDetails(AbstractProject<?,?> project, JSONObject formData);


		    protected String retrieveDetailsFromPom(Document doc, String path) {
		        final XPath xpath = XPathFactory.newInstance().newXPath();
		        final StringBuilder builder = new StringBuilder();
		        NodeList nodes;
		        try {
		            XPathExpression expr = xpath.compile(path);
		            nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		            for (int i = 0; i < nodes.getLength(); i++) {
		                builder.append(nodes.item(i).getNodeValue());
		                builder.append(" ");
		            }
		        } catch (XPathExpressionException ex) {}
		        return builder.toString();
		    }

	 }


	 private Pom2ConfigHandlerSpec spec;

		public Pom2ConfigHandler(Pom2ConfigHandlerSpec spec){
			this.spec = spec;
		}

		  public String getName() {
		        return spec.getName();
		    }

		  public List<DataSet> getPomValues() {
				return spec.getPomValues();
			}


	    public List<DataSet> parsePom(AbstractProject<?,?> project, Document doc) throws IOException{
	    	spec.clearPomValues();
	    	return spec.parsePom(project, doc);
	    }


		public String setDetails(AbstractProject<?, ?> project,
				JSONObject formData) {
			return spec.setDetails(project, formData);
		}

		public int compareTo(Handler handler) {
			return getName().compareToIgnoreCase(handler.getName());
		}


		public boolean isLoaded() {
			return spec.isLoaded();
		}

}
