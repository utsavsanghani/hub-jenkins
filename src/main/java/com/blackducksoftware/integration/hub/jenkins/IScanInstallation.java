package com.blackducksoftware.integration.hub.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolProperty;
import hudson.tools.ToolInstallation;

import java.io.IOException;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class IScanInstallation extends ToolInstallation implements NodeSpecific<IScanInstallation>, EnvironmentSpecific<IScanInstallation> {

    @DataBoundConstructor
    public IScanInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    public IScanInstallation forEnvironment(EnvVars environment) {
        return new IScanInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    public IScanInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new IScanInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Extension
    public static final class Descriptor extends ToolDescriptor<IScanInstallation> {

        public Descriptor() {
            setInstallations();
            load();
        }

        @Override
        public String getDisplayName() {
            return "iScan";
        }

        @Override
        public IScanInstallation newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return (IScanInstallation) super.newInstance(req, formData.getJSONObject("IScanInstallation"));
        }
    }
}
