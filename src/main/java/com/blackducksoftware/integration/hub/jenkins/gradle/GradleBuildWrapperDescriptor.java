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
