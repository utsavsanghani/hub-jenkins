package com.blackducksoftware.integration.hub.jenkins.failure;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

@Extension(ordinal = 1)
public class HubFailureConditionStepDescriptor extends BuildStepDescriptor<Publisher> {

	public HubFailureConditionStepDescriptor() {
		super(HubFailureConditionStep.class);
	}

	public HubSupportHelper getCheckedHubSupportHelper() {
		final HubSupportHelper hubSupport = new HubSupportHelper();
		final HubServerInfo serverInfo = HubServerInfoSingleton.getInstance().getServerInfo();
		try {
			final HubIntRestService service = BuildHelper.getRestService(null, serverInfo.getServerUrl(),
					serverInfo.getUsername(),
					serverInfo.getPassword(),
					serverInfo.getTimeout());
			hubSupport.checkHubSupport(service, null);
		} catch (final Exception e) {
			return null;
		}
		return hubSupport;
	}

	@Override
	public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
		final HubSupportHelper hubSupport = getCheckedHubSupportHelper();
		if (hubSupport != null && hubSupport.isPolicyApiSupport()) {
			return true;
		}

		return false;
	}

	@Override
	public String getDisplayName() {
		return Messages.HubFailureCondition_getDisplayName();
	}

	public FormValidation doCheckFailBuildForPolicyViolations(@QueryParameter("failBuildForPolicyViolations") final boolean failBuildForPolicyViolations)
			throws IOException, ServletException {
		if (failBuildForPolicyViolations) {
			final HubSupportHelper hubSupport = getCheckedHubSupportHelper();

			if (hubSupport != null && !hubSupport.isPolicyApiSupport()) {
				return FormValidation.error(Messages.HubFailureCondition_getPoliciesNotSupported());
			}
		}
		return FormValidation.ok();
	}

}
