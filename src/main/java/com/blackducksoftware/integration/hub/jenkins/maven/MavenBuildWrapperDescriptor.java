package com.blackducksoftware.integration.hub.jenkins.maven;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.AutoCompletionCandidates;
import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.jenkins.BDBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.jenkins.Messages;

@Extension(optional = true)
public class MavenBuildWrapperDescriptor extends BDBuildWrapperDescriptor {

    /**
     * In order to load the persisted global configuration, you have to call
     * load() in the constructor.
     */
    public MavenBuildWrapperDescriptor() {
        super(MavenBuildWrapper.class);
        load();
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> aClass) {
        if (super.isApplicable(aClass)) {
            Plugin requiredPlugin = Jenkins.getInstance().getPlugin("maven-plugin");
            if (requiredPlugin != null && requiredPlugin.getWrapper() != null) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * This human readable name is used in the configuration screen.
     */
    @Override
    public String getDisplayName() {
        return Messages.HubMavenWrapper_getDisplayName();
    }

    /*
     * (non-JSDoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MavenBuildWrapperDescriptor [hubServerInfo=" + getHubServerInfo() + "]";
    }

    /**
     * Fills the drop down list of possible Version phases
     *
     * @return
     */
    public ListBoxModel doFillMavenHubVersionPhaseItems() {
        return doFillHubWrapperVersionPhaseItems();
    }

    /**
     * Fills the drop down list of possible Version distribution types
     *
     * @return
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
