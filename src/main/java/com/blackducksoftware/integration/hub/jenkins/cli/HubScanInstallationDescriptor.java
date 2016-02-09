package com.blackducksoftware.integration.hub.jenkins.cli;

import hudson.model.Descriptor;
import hudson.tools.ToolDescriptor;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

// @Extension
public class HubScanInstallationDescriptor extends ToolDescriptor<HubScanInstallation> {

    // private ScanInstallation[] installations = new ScanInstallation[0];

    public HubScanInstallationDescriptor() {
        // load();
    }

    @Override
    public void setInstallations(HubScanInstallation... installations) {
        // this.installations = installations;
        // save();
    }

    @Override
    public HubScanInstallation[] getInstallations() {
        // return installations;
        return null;
    }

    @Override
    public String getDisplayName() {
        // return "BlackDuck Scan";
        return null;
    }

    @Override
    public HubScanInstallation newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        // return (ScanInstallation) super.newInstance(req, formData.getJSONObject("IScanInstallation"));
        return null;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws Descriptor.FormException {
        // save();
        // return super.configure(req, formData);
        return false;
    }
}
