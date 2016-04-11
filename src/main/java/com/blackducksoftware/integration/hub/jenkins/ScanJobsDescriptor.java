package com.blackducksoftware.integration.hub.jenkins;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

@Extension
public class ScanJobsDescriptor extends Descriptor<ScanJobs> {

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
	 */
	public FormValidation doCheckScanTarget(@QueryParameter("scanTarget") final String scanTarget)
			throws IOException, ServletException {
		if (StringUtils.isBlank(scanTarget)) {
			return FormValidation.warningWithMarkup(Messages
					.HubBuildScan_getWorkspaceWillBeScanned());
		}

		return FormValidation.ok();
	}

}
