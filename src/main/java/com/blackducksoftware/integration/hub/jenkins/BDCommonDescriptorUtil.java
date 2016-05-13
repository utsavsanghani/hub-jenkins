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
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.VersionDoesNotExistException;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;
import com.blackducksoftware.integration.hub.project.api.ProjectItem;
import com.blackducksoftware.integration.hub.version.api.DistributionEnum;
import com.blackducksoftware.integration.hub.version.api.PhaseEnum;
import com.blackducksoftware.integration.hub.version.api.ReleaseItem;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class BDCommonDescriptorUtil {

	/**
	 * Fills the Credential drop down list in the global config
	 *
	 */
	public static ListBoxModel doFillCredentialsIdItems() {

		ListBoxModel boxModel = null;
		final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		boolean changed = false;
		try {
			if (PostBuildScanDescriptor.class.getClassLoader() != originalClassLoader) {
				changed = true;
				Thread.currentThread().setContextClassLoader(PostBuildScanDescriptor.class.getClassLoader());
			}
			// Code copied from
			// https://github.com/jenkinsci/git-plugin/blob/f6d42c4e7edb102d3330af5ca66a7f5809d1a48e/src/main/java/hudson/plugins/git/UserRemoteConfig.java
			final CredentialsMatcher credentialsMatcher = CredentialsMatchers
					.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
			final AbstractProject<?, ?> project = null; // Dont want to limit
			// the search to a particular project for the drop
			// down menu
			boxModel = new StandardListBoxModel().withEmptySelection().withMatching(credentialsMatcher,
					CredentialsProvider.lookupCredentials(StandardCredentials.class, project, ACL.SYSTEM,
							Collections.<DomainRequirement> emptyList()));
		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}
		return boxModel;
	}

	/**
	 * Fills the drop down list of possible Version phases
	 *
	 */
	public static ListBoxModel doFillHubVersionPhaseItems() {
		final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		final boolean changed = false;
		final ListBoxModel items = new ListBoxModel();
		try {
			// should get this list from the Hub server, ticket HUB-1610
			for (final PhaseEnum phase : PhaseEnum.values()) {
				if (phase != PhaseEnum.UNKNOWNPHASE) {
					items.add(phase.getDisplayValue(), phase.name());
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}
		return items;
	}

	/**
	 * Fills the drop down list of possible Version distribution types
	 *
	 */
	public static ListBoxModel doFillHubVersionDistItems() {
		final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		final boolean changed = false;
		final ListBoxModel items = new ListBoxModel();
		try {
			// should get this list from the Hub server, ticket HUB-1610
			for (final DistributionEnum distribution : DistributionEnum.values()) {
				if (distribution != DistributionEnum.UNKNOWNDISTRIBUTION) {
					items.add(distribution.getDisplayValue(), distribution.name());
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}
		return items;
	}


	public static AutoCompletionCandidates doAutoCompleteHubProjectName(final HubServerInfo serverInfo,
			final String hubProjectName)
					throws IOException, ServletException {
		final AutoCompletionCandidates potentialMatches = new AutoCompletionCandidates();
		if (StringUtils.isNotBlank(serverInfo.getServerUrl())
				&& StringUtils.isNotBlank(serverInfo.getCredentialsId())) {
			final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
			final boolean changed = false;
			try {
				if (hubProjectName.contains("$")) {
					return potentialMatches;
				}

				final HubIntRestService service = BuildHelper.getRestService(serverInfo.getServerUrl(),
						serverInfo.getUsername(), serverInfo.getPassword(), serverInfo.getTimeout());

				final List<ProjectItem> suggestions = service.getProjectMatches(hubProjectName);

				if (!suggestions.isEmpty()) {
					for (final ProjectItem projectSuggestion : suggestions) {
						potentialMatches.add(projectSuggestion.getName());
					}
				}
			} catch (final Exception e) {
				// do nothing for exception, there is nowhere in the UI to
				// display this error
			} finally {
				if (changed) {
					Thread.currentThread().setContextClassLoader(originalClassLoader);
				}
			}

		}
		return potentialMatches;
	}

	public static FormValidation doCheckHubProjectName(final HubServerInfo serverInfo, final String hubProjectName,
			final String hubProjectVersion) throws IOException, ServletException {
		// Query for the project version so hopefully the check methods run for
		// both fields
		// when the User changes the Name of the project
		if (StringUtils.isNotBlank(hubProjectName)) {
			final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
			final boolean changed = false;
			try {
				if (StringUtils.isBlank(serverInfo.getServerUrl())) {
					return FormValidation.error(Messages.HubBuildScan_getPleaseSetServerUrl());
				}
				if (StringUtils.isBlank(serverInfo.getCredentialsId())) {
					return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
				}
				if (hubProjectName.contains("$")) {
					return FormValidation.warning(Messages.HubBuildScan_getProjectNameContainsVariable());
				}

				final HubIntRestService service = BuildHelper.getRestService(serverInfo.getServerUrl(),
						serverInfo.getUsername(), serverInfo.getPassword(), serverInfo.getTimeout());

				service.getProjectByName(hubProjectName);
				return FormValidation
						.ok(Messages.HubBuildScan_getProjectExistsIn_0_(serverInfo.getServerUrl()));
			} catch (final ProjectDoesNotExistException e) {
				return FormValidation
						.error(Messages.HubBuildScan_getProjectNonExistingIn_0_(serverInfo.getServerUrl()));
			} catch (final BDRestException e) {
				String message;
				if (e.getCause() != null) {
					message = e.getCause().toString();
					if (message.contains("(407)")) {
						return FormValidation.error(e, message);
					}
				}
				return FormValidation.error(e, e.getMessage());
			} catch (final Exception e) {
				String message;
				if (e.getCause() != null && e.getCause().getCause() != null) {
					message = e.getCause().getCause().toString();
				} else if (e.getCause() != null) {
					message = e.getCause().toString();
				} else {
					message = e.toString();
				}
				if (message.toLowerCase().contains("service unavailable")) {
					message = Messages.HubBuildScan_getCanNotReachThisServer_0_(serverInfo.getServerUrl());
				} else if (message.toLowerCase().contains("precondition failed")) {
					message = message + ", Check your configuration.";
				}
				return FormValidation.error(e, message);
			} finally {
				if (changed) {
					Thread.currentThread().setContextClassLoader(originalClassLoader);
				}
			}
		} else {
			if (StringUtils.isNotBlank(hubProjectVersion)) {
				return FormValidation.error(Messages.HubBuildScan_getProvideProjectName());
			}
		}
		return FormValidation.ok();
	}

	public static FormValidation doCheckHubProjectVersion(final HubServerInfo serverInfo,
			final String hubProjectVersion, final String hubProjectName) throws IOException, ServletException {
		if (StringUtils.isNotBlank(hubProjectVersion)) {

			final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
			final boolean changed = false;
			try {
				if (StringUtils.isBlank(serverInfo.getServerUrl())) {
					return FormValidation.error(Messages.HubBuildScan_getPleaseSetServerUrl());
				}
				if (StringUtils.isBlank(serverInfo.getCredentialsId())) {
					return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
				}
				if (StringUtils.isBlank(hubProjectName)) {
					// Error will be displayed for the project name field
					return FormValidation.ok();
				}
				if (hubProjectVersion.contains("$")) {
					return FormValidation.warning(Messages.HubBuildScan_getProjectVersionContainsVariable());
				}
				if (hubProjectName.contains("$")) {
					// Warning will be displayed for the project name field
					return FormValidation.ok();
				}

				final HubIntRestService service = BuildHelper.getRestService(serverInfo.getServerUrl(),
						serverInfo.getUsername(), serverInfo.getPassword(), serverInfo.getTimeout());

				ProjectItem project = null;
				try {
					project = service.getProjectByName(hubProjectName);
				} catch (final Exception e) {
					// This error will already show up for the project name
					// field
					return FormValidation.ok();
				}
				final List<ReleaseItem> releases = service.getVersionsForProject(project);

				final StringBuilder projectVersions = new StringBuilder();
				for (final ReleaseItem release : releases) {
					if (release.getVersionName().equals(hubProjectVersion)) {
						return FormValidation.ok(Messages.HubBuildScan_getVersionExistsIn_0_(project.getName()));
					} else {
						if (projectVersions.length() > 0) {
							projectVersions.append(", " + release.getVersionName());
						} else {
							projectVersions.append(release.getVersionName());
						}
					}
				}
				return FormValidation.error(Messages.HubBuildScan_getVersionNonExistingIn_0_(project.getName(),
						projectVersions.toString()));
			} catch (final BDRestException e) {
				String message;
				if (e.getCause() != null) {
					message = e.getCause().toString();
					if (message.contains("(407)")) {
						return FormValidation.error(e, message);
					}
				}
				return FormValidation.error(e, e.getMessage());
			} catch (final Exception e) {
				String message;
				if (e.getCause() != null && e.getCause().getCause() != null) {
					message = e.getCause().getCause().toString();
				} else if (e.getCause() != null) {
					message = e.getCause().toString();
				} else {
					message = e.toString();
				}
				if (message.toLowerCase().contains("service unavailable")) {
					message = Messages.HubBuildScan_getCanNotReachThisServer_0_(serverInfo.getServerUrl());
				} else if (message.toLowerCase().contains("precondition failed")) {
					message = message + ", Check your configuration.";
				}
				return FormValidation.error(e, message);
			} finally {
				if (changed) {
					Thread.currentThread().setContextClassLoader(originalClassLoader);
				}
			}
		} else {
			if (StringUtils.isNotBlank(hubProjectName)) {
				return FormValidation.error(Messages.HubBuildScan_getProvideProjectVersion());
			}
		}
		return FormValidation.ok();
	}


	public static FormValidation doCreateHubProject(final HubServerInfo serverInfo, final String hubProjectName,
			final String hubProjectVersion, final String hubVersionPhase, final String hubVersionDist) {
		final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		final boolean changed = false;
		try {

			if (StringUtils.isBlank(hubProjectName)) {
				return FormValidation.error(Messages.HubBuildScan_getProvideProjectName());
			}
			if (StringUtils.isBlank(hubProjectVersion)) {
				return FormValidation.error(Messages.HubBuildScan_getProvideProjectVersion());
			}
			if (hubProjectName.contains("$")) {
				return FormValidation.warning(Messages.HubBuildScan_getProjectNameContainsVariable());
			}
			if (hubProjectVersion.contains("$")) {
				return FormValidation.warning(Messages.HubBuildScan_getProjectVersionContainsVariable());
			}
			if (StringUtils.isBlank(hubVersionPhase)) {
				return FormValidation.error(Messages.HubBuildScan_getProvideVersionPhase());
			}
			if (StringUtils.isBlank(hubVersionDist)) {
				return FormValidation.error(Messages.HubBuildScan_getProvideVersionDist());
			}

			String credentialUserName = null;
			String credentialPassword = null;

			final UsernamePasswordCredentialsImpl credential = serverInfo.getCredential();
			if (credential == null) {
				return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
			}
			credentialUserName = credential.getUsername();
			credentialPassword = credential.getPassword().getPlainText();

			final HubIntRestService service = BuildHelper.getRestService(serverInfo.getServerUrl(), credentialUserName,
					credentialPassword, serverInfo.getTimeout());

			Boolean projectCreated = false;

			ProjectItem project = null;
			try {
				project = service.getProjectByName(hubProjectName);
			} catch (final ProjectDoesNotExistException e) {
				final String projectUrl = service.createHubProject(hubProjectName);
				project = service.getProject(projectUrl);
				projectCreated = true;
			}

			try {
				service.getVersion(project, hubProjectVersion);
				return FormValidation.warning(Messages.HubBuildScan_getProjectAndVersionExist());
			} catch (final VersionDoesNotExistException e) {
				service.createHubVersion(project, hubProjectVersion, hubVersionPhase, hubVersionDist);
				if (projectCreated) {
					return FormValidation.ok(Messages.HubBuildScan_getProjectAndVersionCreated());
				} else {
					return FormValidation.ok(Messages.HubBuildScan_getVersionCreated());
				}
			}
		} catch (final BDRestException e) {
			if (e.getResource().getResponse().getStatus().getCode() == 412) {
				return FormValidation.error(e, Messages.HubBuildScan_getProjectVersionCreationProblem());
			} else if (e.getResource().getResponse().getStatus().getCode() == 401) {
				// If User is Not Authorized, 401 error, an exception should be
				// thrown by the ClientResource
				return FormValidation.error(e,
						Messages.HubBuildScan_getCredentialsInValidFor_0_(serverInfo.getServerUrl()));
			} else if (e.getResource().getResponse().getStatus().getCode() == 407) {
				return FormValidation.error(e, Messages
						.HubBuildScan_getErrorConnectingTo_0_(e.getResource().getResponse().getStatus().getCode()));
			} else {
				return FormValidation.error(e, Messages
						.HubBuildScan_getErrorConnectingTo_0_(e.getResource().getResponse().getStatus().getCode()));
			}
		} catch (final Exception e) {
			String message;
			if (e.getCause() != null && e.getCause().getCause() != null) {
				message = e.getCause().getCause().toString();
			} else if (e.getCause() != null) {
				message = e.getCause().toString();
			} else {
				message = e.toString();
			}
			if (message.toLowerCase().contains("service unavailable")) {
				message = Messages.HubBuildScan_getCanNotReachThisServer_0_(serverInfo.getServerUrl());
			} else if (message.toLowerCase().contains("precondition failed")) {
				message = message + ", Check your configuration.";
			}
			return FormValidation.error(e, message);
		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}

	}
}
