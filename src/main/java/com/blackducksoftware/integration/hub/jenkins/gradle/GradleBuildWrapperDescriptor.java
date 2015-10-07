package com.blackducksoftware.integration.hub.jenkins.gradle;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.AbstractProject;
import jenkins.model.Jenkins;

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
        return Messages.HubMavenWrapper_getDisplayName();
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
}
