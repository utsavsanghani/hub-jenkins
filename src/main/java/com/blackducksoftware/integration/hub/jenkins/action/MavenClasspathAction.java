/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.jenkins.action;

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
