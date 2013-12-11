package org.jenkinsci.plugins.pom2config;

import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Functions;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.Hudson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * @author Kathi Stutz, Michael Klein
 */
public class Pom2Config implements Action {

    private static final Logger LOG = Logger.getLogger(Pom2Config.class.getName());

    /** The project. */
    private final transient AbstractProject<?, ?> project;

    private List<Handler> pom2ConfigHandler = new ArrayList<Handler>();
    private List<String> messages = new ArrayList<String>();
    private List<DataSet> configDetails = new ArrayList<DataSet>();
    private List<Handler> notLoadedHandler = new ArrayList<Handler>();


	public Pom2Config(AbstractProject<?, ?> project) {
		super();
		this.project = project;
	}


	/**
	 * Gets all available Pom2ConfigHandler
	 * @return Loaded Pom2ConfigHandler
	 */
	public List<Handler> getHandler() {
    	ExtensionList<Handler> extensionList = Hudson.getInstance().getExtensionList(Handler.class);
    	List<Handler> handlerList = new ArrayList<Handler>();
    	for (Handler handler: extensionList) {
    		if (handler.isLoaded()) {
    			handlerList.add(handler);
    			LOG.info("Loaded: " + handler.getClass());
    		} else {
    			notLoadedHandler.add(handler);
    			LOG.info("NOT Loaded: " + handler.getClass());
    		}
    	}
    	Collections.sort(handlerList);
    	this.pom2ConfigHandler = handlerList;

    	return this.pom2ConfigHandler;
    }


	/**
	 * Checks if there is a pom in the job workspace
	 * @return true if there is or no if not
	 */
	public boolean isPomInWorkspace() {
        FilePath workspace = project.getWorkspace();

        if (project.getLastBuild() != null && project.getLastBuild().getWorkspace() != null) {
            workspace = project.getWorkspace();
        }

        if (workspace != null) {
        	  FilePath pomPath;
              if (project instanceof MavenModuleSet) {
            	  pomPath = new FilePath(workspace, ((MavenModuleSet) project).getRootPOM(null));
              }else{
            	  pomPath = workspace.child("pom.xml");
              }
            try {
                if (pomPath.exists()){
                    return true;
                }
            } catch (IOException ex) {
                return false;
            } catch (InterruptedException ex) {
                return false;
            }
        }
        return false;
    }


	/**
	 * Retrieves and parses the selected pom
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
    public final void doGetPom(StaplerRequest req, StaplerResponse rsp) throws IOException {
        final String notRetrieved = "Unable to retrieve pom file.";
        final String notParsed = "Unable to parse pom file.";
        final Writer writer = rsp.getCompressedWriter(req);
        pom2ConfigHandler.clear();
        notLoadedHandler.clear();
        getHandler();
        String pomAsString = "";

        try {
            pomAsString = retrievePom(req.getSubmittedForm().getJSONObject("fromWhere"));
        } catch (IOException ioe) {
            writeErrorMessage(notRetrieved, writer);
        } catch (InterruptedException ie) {
            writeErrorMessage(notRetrieved, writer);
        } catch (ServletException se) {
            writeErrorMessage(notRetrieved, writer);
        }

        if (!pomAsString.isEmpty()) {
            try {
                parsePom(pomAsString);
            } catch (ParserConfigurationException e) {
                writeErrorMessage(notParsed, writer);
            } catch (SAXException e) {
                writeErrorMessage(notParsed, writer);
            } catch (IOException e) {
                writeErrorMessage(notParsed, writer);
            }
            rsp.sendRedirect("chooseDetails");
        } else {
            writeErrorMessage(notRetrieved, writer);
        }
    }

    public void writeErrorMessage(String message, Writer writer) throws IOException {
        try {
            writer.append(message + "\n");
        } finally {
            writer.close();
        }
    }


    /**
     * Retrieves the selected pom
     * @param formData
     * @return
     * @throws ServletException
     * @throws IOException
     * @throws InterruptedException
     */
    private String retrievePom(JSONObject formData) throws ServletException, IOException, InterruptedException {
        String pomAsString = "";
        final String fromWhere = formData.getString("value");

        if ("useExisting".equals(fromWhere)) {
        	FilePath workspace = project.getWorkspace();
            if (project.getLastBuild() != null && project.getLastBuild().getWorkspace() != null) {
                workspace = project.getLastBuild().getWorkspace();
            }
            if (workspace != null) {
                FilePath pomPath = workspace.child("pom.xml");
                if (pomPath.exists()){
                    pomAsString = pomPath.readToString();
                }
            }
        } else if ("fromUrl".equals(fromWhere)){
            final URL pomURL = new URL(formData.getString("location"));
            BufferedReader in = new BufferedReader(
                new InputStreamReader(pomURL.openStream()));

            StringBuilder builder = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine);
            }
            in.close();
            pomAsString = builder.toString();
        } else {
            final FileItem fileItem = Stapler.getCurrentRequest().getFileItem(formData.getString("file"));
            pomAsString = fileItem.getString();
        }

        return pomAsString;
    }


    /**
     * Parse the pom
     * @param xml
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private void parsePom(String xml) throws ParserConfigurationException, SAXException, IOException {
        configDetails.clear();
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));
        final Document doc = db.parse(is);

        for (Handler handler : this.pom2ConfigHandler) {
        	handler.parsePom(project, doc);
        }

    }


    /**
     * Set chosen values in the job configuration
     * @param req
     * @param rsp
     * @throws IOException
     * @throws URISyntaxException
     * @throws ServletException
     */
    public final void doSetDetails(StaplerRequest req, StaplerResponse rsp) throws IOException, URISyntaxException, ServletException{
		List<DataSet> values;
		JSONObject formData = null;

		formData = req.getSubmittedForm();
		LOG.finest(formData.toString(2));
		for (Handler handler : pom2ConfigHandler) {
			values = handler.getPomValues();
			for (DataSet dataSet : values) {
				if (formData.containsKey(dataSet.getName())) {
					messages.add(handler.setDetails(project, formData));
				}

			}

		}
		rsp.forward(this, "showOutcome", req);
	}


    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        return "Pom2Config";
    }

    /**
     * {@inheritDoc}
     */
    public String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/pom2config/icons/find-replace-32x32.png";
    }

    /**
     * {@inheritDoc}
     */
    public String getUrlName() {
        return "pom2config";
    }


	public List<Handler> getNotLoadedHandler() {
		return notLoadedHandler;
	}


	public List<Handler> getPom2ConfigHandler() {
		return pom2ConfigHandler;
	}


	public List<String> getMessages() {
		return messages;
	}
}
