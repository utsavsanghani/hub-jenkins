/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.jenkins.maven;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.exception.BDCIScopeException;
import com.blackducksoftware.integration.hub.jenkins.BDCommonDescriptorUtil;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.helper.PluginHelper;
import com.blackducksoftware.integration.hub.maven.Scope;

import hudson.maven.MavenReporterDescriptor;
import hudson.model.AutoCompletionCandidates;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

// TODO Uncomment extension to add the maven reporter back in
// @Extension(optional = true)
// This indicates to Jenkins that this is an implementation of an extension
// point.
public class HubMavenReporterDescriptor extends MavenReporterDescriptor {

	public HubMavenReporterDescriptor() {
		super(HubMavenReporter.class);
		load();
	}

	@Override
	public String getDisplayName() {
		return Messages.HubMavenWrapper_getDisplayName();
	}

	public String getPluginVersion() {
		return PluginHelper.getPluginVersion();
	}

	public HubServerInfo getHubServerInfo() {
		return HubServerInfoSingleton.getInstance().getServerInfo();
	}

	/**
	 * Fills the drop down list of possible Version phases
	 *
	 */
	public ListBoxModel doFillMavenHubVersionPhaseItems() {
		return BDCommonDescriptorUtil.doFillHubVersionPhaseItems();
	}

	/**
	 * Fills the drop down list of possible Version distribution types
	 *
	 */
	public ListBoxModel doFillMavenHubVersionDistItems() {
		return BDCommonDescriptorUtil.doFillHubVersionDistItems();
	}

	public AutoCompletionCandidates doAutoCompleteMavenHubProjectName(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName)
			throws IOException, ServletException {
		return BDCommonDescriptorUtil.doAutoCompleteHubProjectName(getHubServerInfo(), mavenHubProjectName);
	}

	/**
	 * Performs on-the-fly validation of the form field 'mavenHubProjectName'.
	 * Checks to see if there is already a project in the Hub with this name.
	 *
	 */
	public FormValidation doCheckMavenHubProjectName(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName,
			@QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion) throws IOException, ServletException {
		return BDCommonDescriptorUtil.doCheckHubProjectName(getHubServerInfo(), mavenHubProjectName, mavenHubProjectVersion);
	}

	/**
	 * Performs on-the-fly validation of the form field 'mavenHubProjectVersion'. Checks to see if there is already a
	 * project
	 * in the Hub with this name.
	 *
	 */
	public FormValidation doCheckMavenHubProjectVersion(@QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion,
			@QueryParameter("mavenHubProjectName") final String mavenHubProjectName) throws IOException, ServletException {
		return BDCommonDescriptorUtil.doCheckHubProjectVersion(getHubServerInfo(), mavenHubProjectVersion,
				mavenHubProjectName);
	}

	/**
	 * Creates the Hub project AND/OR version
	 *
	 *
	 */
	public FormValidation doCreateMavenHubProject(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName,
			@QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion,
			@QueryParameter("mavenHubVersionPhase") final String mavenHubVersionPhase,
			@QueryParameter("mavenHubVersionDist") final String mavenHubVersionDist) {
		return BDCommonDescriptorUtil.doCreateHubProject(getHubServerInfo(), mavenHubProjectName,
				mavenHubProjectVersion, mavenHubVersionPhase, mavenHubVersionDist);
	}

	/**
	 * Performs on-the-fly validation of the form field 'userScopesToInclude'.
	 *
	 */
	public FormValidation doCheckUserScopesToInclude(@QueryParameter final String value)
			throws IOException, ServletException {
		if (StringUtils.isBlank(value)) {
			return FormValidation.error(Messages
					.HubMavenWrapper_getPleaseIncludeAScope());
		}
		try {
			Scope.getScopeListFromString(value);
		} catch (final BDCIScopeException e) {
			final String scope = e.getMessage().substring(e.getMessage().indexOf(":") + 1).trim();
			return FormValidation.error(Messages.HubMavenWrapper_getIncludedInvalidScope_0_(scope));
		}
		return FormValidation.ok();
	}

}
