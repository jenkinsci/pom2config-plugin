
package org.jenkinsci.plugins.pom2config;

import java.util.List;

/**
 * @author Kathi Stutz, Michael Klein
 */
public class DataSet {
    private final String name;
    private boolean isActive;
    private final List<String> oldEntrys;
    private final String newEntry;

    public DataSet(String name, boolean isActiv, List<String> oldEntrys, String newEntry) {
        this.name = name;
        this.isActive = isActiv;
        this.oldEntrys = oldEntrys;
        this.newEntry = newEntry;
    }

    public String getName() {
        return name;
    }

    public List<String> getOldEntrys() {
        return oldEntrys;
    }

    public String getNewEntry() {
        return newEntry;
    }

	public boolean getIsActive() {
		return isActive;
	}
}