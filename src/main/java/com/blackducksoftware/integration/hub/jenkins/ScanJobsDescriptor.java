package com.blackducksoftware.integration.hub.jenkins;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;

@Extension
public class ScanJobsDescriptor extends Descriptor<ScanJobs> {

    /**
     * In order to load the persisted global configuration, you have to call
     * load() in the constructor.
     */
    public ScanJobsDescriptor() {
        super(ScanJobs.class);
        load();
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    /**
     * Performs on-the-fly validation of the form field 'scanTarget'.
     *
     * @param value
     *            This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckScanTarget(@QueryParameter("scanTarget") String scanTarget)
            throws IOException, ServletException {
        if (scanTarget.length() == 0) {
            return FormValidation.warningWithMarkup(Messages
                    .HubBuildScan_getWorkspaceWillBeScanned());
        }

        return FormValidation.ok();
    }

}
