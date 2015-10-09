package com.blackducksoftware.integration.hub.jenkins.maven;

import hudson.model.Action;

import com.blackducksoftware.integration.build.BuildInfo;

public class BuildInfoAction implements Action {

    private BuildInfo buildInfo;

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "Temp build info Action";
    }

    public String getUrlName() {
        return null;
    }

    /**
     * @return the buildInfo
     */
    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    /**
     * @param buildInfo
     *            the buildInfo to set
     */
    public void setBuildInfo(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

}
