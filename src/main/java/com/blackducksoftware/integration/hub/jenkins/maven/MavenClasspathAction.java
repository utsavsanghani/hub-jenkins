package com.blackducksoftware.integration.hub.jenkins.maven;

import hudson.model.Action;

public class MavenClasspathAction implements Action {

    private String buildId;

    private String workingDirectory;

    private String mavenClasspathExtension;

    private Boolean isRecorder30 = false;

    private Boolean isRecorder31 = false;

    private String separator;

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Temp BlackDuck maven Action";
    }

    @Override
    public String getUrlName() {
        return null;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getMavenClasspathExtension() {
        return mavenClasspathExtension;
    }

    public void setMavenClasspathExtension(String mavenClasspathExtension) {
        this.mavenClasspathExtension = mavenClasspathExtension;
    }

    public Boolean getIsRecorder30() {
        return isRecorder30;
    }

    public void setIsRecorder30(Boolean isRecorder30) {
        this.isRecorder30 = isRecorder30;
    }

    public Boolean getIsRecorder31() {
        return isRecorder31;
    }

    public void setIsRecorder31(Boolean isRecorder31) {
        this.isRecorder31 = isRecorder31;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

}
