package com.blackducksoftware.integration.hub.jenkins.maven;

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

// TODO Uncomment extension to add the maven wrapper back in
// @Extension(optional = true)
public class MavenBuildWrapperDescriptor extends BDBuildWrapperDescriptor {

    public MavenBuildWrapperDescriptor() {
        super(MavenBuildWrapper.class);
        load();
    }

    @Override
    public boolean isApplicable(final AbstractProject<?, ?> aClass) {
        if (super.isApplicable(aClass)) {
            final Plugin requiredPlugin = Jenkins.getInstance().getPlugin("maven-plugin");
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
        return Messages.HubMavenWrapper_getDisplayName();
    }

    @Override
    public String toString() {
        return "MavenBuildWrapperDescriptor [hubServerInfo=" + getHubServerInfo() + "]";
    }

    /**
     * Fills the drop down list of possible Version phases
     *
     */
    public ListBoxModel doFillMavenHubVersionPhaseItems() {
        return doFillHubWrapperVersionPhaseItems();
    }

    /**
     * Fills the drop down list of possible Version distribution types
     *
     */
    public ListBoxModel doFillMavenHubVersionDistItems() {
        return doFillHubWrapperVersionDistItems();
    }

    public AutoCompletionCandidates doAutoCompleteMavenHubProjectName(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName)
            throws IOException,
            ServletException {
        return doAutoCompleteHubWrapperProjectName(mavenHubProjectName);
    }

    public FormValidation doCheckMavenHubProjectName(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName,
            @QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion) throws IOException, ServletException {
        return doCheckHubWrapperProjectName(mavenHubProjectName, mavenHubProjectVersion);
    }

    public FormValidation doCheckMavenHubProjectVersion(@QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion,
            @QueryParameter("mavenHubProjectName") final String mavenHubProjectName) throws IOException, ServletException {
        return doCheckHubWrapperProjectVersion(mavenHubProjectVersion, mavenHubProjectName);
    }

    public FormValidation doCreateMavenHubProject(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName,
            @QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion,
            @QueryParameter("mavenHubVersionPhase") final String mavenHubVersionPhase,
            @QueryParameter("mavenHubVersionDist") final String mavenHubVersionDist) {
        return doCreateHubWrapperProject(mavenHubProjectName, mavenHubProjectVersion, mavenHubVersionPhase, mavenHubVersionDist);
    }

}
