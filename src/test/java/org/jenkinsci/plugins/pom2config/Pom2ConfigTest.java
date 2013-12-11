package org.jenkinsci.plugins.pom2config;


import hudson.model.FreeStyleProject;
import hudson.model.Hudson;

import java.util.logging.Level;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * @author kstutz, miklein
 */
public class Pom2ConfigTest extends HudsonTestCase {

    @LocalData
    public void testReplacingDescription() throws Exception {
    	java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
    	final String newDescription = "Brave new description";
        final FreeStyleProject project =  (FreeStyleProject) hudson.getItem("TestJob1");
        project.scheduleBuild2(0);

        checkIfJobsAreLoaded();

        final WebClient webClient =  new WebClient();

        final HtmlPage indexPage = webClient.goTo(project.getUrl() + "pom2config");
        final HtmlElement radioButton = indexPage.getElementsByName("fromWhere").get(0);
        final HtmlPage chooseDetailsPage = submit(radioButton.getEnclosingFormOrDie(), "SubmitButton");

        WebAssert.assertTextPresent(chooseDetailsPage, "Config entry\tOld description");
        WebAssert.assertTextPresent(chooseDetailsPage, newDescription);

        webClient.closeAllWindows();
    }

    private void checkIfJobsAreLoaded() {
        assertNotNull("job missing.. @LocalData problem?", Hudson.getInstance()
                .getItem("TestJob1"));
    }

}
