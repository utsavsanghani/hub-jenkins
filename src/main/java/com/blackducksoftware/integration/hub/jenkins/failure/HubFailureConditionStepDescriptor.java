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
