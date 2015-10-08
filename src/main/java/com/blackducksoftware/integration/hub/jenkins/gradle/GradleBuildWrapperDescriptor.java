package com.blackducksoftware.integration.hub.jenkins.gradle;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.AutoCompletionCandidates;
import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.jenkins.BDBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.jenkins.Messages;

@Extension(optional = true)
public class GradleBuildWrapperDescriptor extends BDBuildWrapperDescriptor {

    /**
     * In order to load the persisted global configuration, you have to call
     * load() in the constructor.
     */
    public GradleBuildWrapperDescriptor() {
        super(GradleBuildWrapper.class);
        load();
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> aClass) {
        if (super.isApplicable(aClass)) {
            Plugin requiredPlugin = Jenkins.getInstance().getPlugin("gradle");
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
        return Messages.HubGradleWrapper_getDisplayName();
    }

    /*
     * (non-JSDoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "GradleBuildWrapperDescriptor [hubServerInfo=" + getHubServerInfo() + "]";
    }

    /**
     * Fills the drop down list of possible Version phases
     *
     * @return
     */
    public ListBoxModel doFillGradleHubVersionPhaseItems() {
        return doFillHubWrapperVersionPhaseItems();
    }

    /**
     * Fills the drop down list of possible Version distribution types
     *
     * @return
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

    public FormValidation doCheckGradleSameAsPostBuildScan(@QueryParameter("gradleSameAsPostBuildScan") final Boolean gradleSameAsPostBuildScan,
            @AncestorInPath AbstractProject project) throws IOException, ServletException {
        return doCheckSameAsPostBuildScan(gradleSameAsPostBuildScan, project);
    }
}
