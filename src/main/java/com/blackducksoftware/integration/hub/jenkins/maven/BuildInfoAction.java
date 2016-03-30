package com.blackducksoftware.integration.hub.jenkins.maven;

import hudson.model.Action;

import com.blackducksoftware.integration.build.BuildInfo;

public class BuildInfoAction implements Action {

    private BuildInfo buildInfo;

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Temp build info Action";
    }

    @Override
    public String getUrlName() {
        return null;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public void setBuildInfo(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

}
