package org.jenkinsci.plugins.pom2config;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;
import hudson.model.AbstractProject;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Extends project actions for all jobs.
 *
 * @author Kathi Stutz
 */
@Extension
public class ActionFactory extends TransientProjectActionFactory {

    /**
     * {@inheritDoc} Adds the Pom2Config to every job
     */
    @Override
    public Collection<? extends Action> createFor(@SuppressWarnings("rawtypes") AbstractProject target) {
        final ArrayList<Action> actions = new ArrayList<Action>();
        final Pom2Config newAction = new Pom2Config(target);
        actions.add(newAction);

        return actions;
    }
}
