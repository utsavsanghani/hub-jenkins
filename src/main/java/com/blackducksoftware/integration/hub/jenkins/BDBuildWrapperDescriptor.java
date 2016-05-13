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
package com.blackducksoftware.integration.hub.jenkins;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.exception.BDCIScopeException;
import com.blackducksoftware.integration.hub.jenkins.gradle.GradleBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.jenkins.helper.PluginHelper;
import com.blackducksoftware.integration.hub.jenkins.maven.MavenBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.maven.Scope;

import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class BDBuildWrapperDescriptor extends BuildWrapperDescriptor implements Serializable {
	public BDBuildWrapperDescriptor() {
		super(BDBuildWrapper.class);
		load();
	}

	public BDBuildWrapperDescriptor(final Class<? extends BuildWrapper> subClass) {
		super(subClass);
		load();
	}

	public HubServerInfo getHubServerInfo() {
		return HubServerInfoSingleton.getInstance().getServerInfo();
	}

	/**
	 * Fills the drop down list of possible Version phases
	 *
	 */
	public ListBoxModel doFillHubWrapperVersionPhaseItems() {
		return BDCommonDescriptorUtil.doFillHubVersionPhaseItems();
	}

	/**
	 * Fills the drop down list of possible Version distribution types
	 *
	 */
	public ListBoxModel doFillHubWrapperVersionDistItems() {
		return BDCommonDescriptorUtil.doFillHubVersionDistItems();
	}

	public AutoCompletionCandidates doAutoCompleteHubWrapperProjectName(
			@QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName)
					throws IOException, ServletException {
		return BDCommonDescriptorUtil.doAutoCompleteHubProjectName(getHubServerInfo(), hubWrapperProjectName);
	}

	/**
	 * Performs on-the-fly validation of the form field 'hubWrapperProjectName'.
	 * Checks to see if there is already a project in the Hub with this name.
	 *
	 */
	public FormValidation doCheckHubWrapperProjectName(
			@QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName,
			@QueryParameter("hubWrapperProjectVersion") final String hubWrapperProjectVersion)
					throws IOException, ServletException {
		return BDCommonDescriptorUtil.doCheckHubProjectName(getHubServerInfo(), hubWrapperProjectName,
				hubWrapperProjectVersion);
	}

	/**
	 * Performs on-the-fly validation of the form field
	 * 'hubWrapperProjectVersion'. Checks to see if there is already a project
	 * in the Hub with this name.
	 *
	 */
	public FormValidation doCheckHubWrapperProjectVersion(
			@QueryParameter("hubWrapperProjectVersion") final String hubWrapperProjectVersion,
			@QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName)
					throws IOException, ServletException {
		return BDCommonDescriptorUtil.doCheckHubProjectVersion(getHubServerInfo(), hubWrapperProjectVersion,
				hubWrapperProjectName);
	}

	/**
	 * Creates the Hub project AND/OR version
	 *
	 */
	public FormValidation doCreateHubWrapperProject(
			@QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName,
			@QueryParameter("hubWrapperProjectVersion") final String hubWrapperProjectVersion,
			@QueryParameter("hubWrapperVersionPhase") final String hubWrapperVersionPhase,
			@QueryParameter("hubWrapperVersionDist") final String hubWrapperVersionDist) {
		save();
		return BDCommonDescriptorUtil.doCreateHubProject(getHubServerInfo(), hubWrapperProjectName,
				hubWrapperProjectVersion, hubWrapperVersionPhase, hubWrapperVersionDist);
	}

	/**
	 * Performs on-the-fly validation of the form field 'userScopesToInclude'.
	 *
	 */
	public FormValidation doCheckUserScopesToInclude(@QueryParameter final String value)
			throws IOException, ServletException {
		if (this instanceof MavenBuildWrapperDescriptor) {
			if (StringUtils.isBlank(value)) {
				return FormValidation.error(Messages.HubMavenWrapper_getPleaseIncludeAScope());
			}
			try {
				Scope.getScopeListFromString(value);
			} catch (final BDCIScopeException e) {
				final String scope = e.getMessage().substring(e.getMessage().indexOf(":") + 1).trim();
				return FormValidation.error(Messages.HubMavenWrapper_getIncludedInvalidScope_0_(scope));
			}
		} else if (this instanceof GradleBuildWrapperDescriptor) {
			if (StringUtils.isBlank(value)) {
				return FormValidation.error(Messages.HubGradleWrapper_getPleaseIncludeAConfiguration());
			}
		}
		return FormValidation.ok();
	}

	@Override
	public boolean isApplicable(final AbstractProject<?, ?> aClass) {
		return aClass.getClass().isAssignableFrom(FreeStyleProject.class);
	}

	@Override
	public String getDisplayName() {
		return "";
	}

	public String getPluginVersion() {
		return PluginHelper.getPluginVersion();
	}

}
