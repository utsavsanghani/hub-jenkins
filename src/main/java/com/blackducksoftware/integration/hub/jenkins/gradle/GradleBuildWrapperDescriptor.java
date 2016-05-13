/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.jenkins.gradle;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.jenkins.BDBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.jenkins.Messages;

import hudson.Plugin;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

// TODO Uncomment extension to add the gradle wrapper back in
// @Extension(optional = true)
public class GradleBuildWrapperDescriptor extends BDBuildWrapperDescriptor {

    public GradleBuildWrapperDescriptor() {
        super(GradleBuildWrapper.class);
        load();
    }

    @Override
    public boolean isApplicable(final AbstractProject<?, ?> aClass) {
        if (super.isApplicable(aClass)) {
            final Plugin requiredPlugin = Jenkins.getInstance().getPlugin("gradle");
            if (requiredPlugin != null && requiredPlugin.getWrapper() != null) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public String getDisplayName() {
        return Messages.HubGradleWrapper_getDisplayName();
    }

    @Override
    public String toString() {
        return "GradleBuildWrapperDescriptor [hubServerInfo=" + getHubServerInfo() + "]";
    }

    /**
     * Fills the drop down list of possible Version phases
     *
     */
    public ListBoxModel doFillGradleHubVersionPhaseItems() {
        return doFillHubWrapperVersionPhaseItems();
    }

    /**
     * Fills the drop down list of possible Version distribution types
     *
     */
    public ListBoxModel doFillGradleHubVersionDistItems() {
        return doFillHubWrapperVersionDistItems();
    }

    public AutoCompletionCandidates doAutoCompleteGradleHubProjectName(@QueryParameter("gradleHubProjectName") final String gradleHubProjectName)
            throws IOException,
            ServletException {
        return doAutoCompleteHubWrapperProjectName(gradleHubProjectName);
    }

    public FormValidation doCheckGradleHubProjectName(@QueryParameter("gradleHubProjectName") final String gradleHubProjectName,
            @QueryParameter("gradleHubProjectVersion") final String gradleHubProjectVersion) throws IOException, ServletException {
        return doCheckHubWrapperProjectName(gradleHubProjectName, gradleHubProjectVersion);
    }

    public FormValidation doCheckGradleHubProjectVersion(@QueryParameter("gradleHubProjectVersion") final String gradleHubProjectVersion,
            @QueryParameter("gradleHubProjectName") final String gradleHubProjectName) throws IOException, ServletException {
        return doCheckHubWrapperProjectVersion(gradleHubProjectVersion, gradleHubProjectName);
    }

    public FormValidation doCreateGradleHubProject(@QueryParameter("gradleHubProjectName") final String gradleHubProjectName,
            @QueryParameter("gradleHubProjectVersion") final String gradleHubProjectVersion,
            @QueryParameter("gradleHubVersionPhase") final String gradleHubVersionPhase,
            @QueryParameter("gradleHubVersionDist") final String gradleHubVersionDist) {
        return doCreateHubWrapperProject(gradleHubProjectName, gradleHubProjectVersion, gradleHubVersionPhase, gradleHubVersionDist);
    }

}
