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
package com.blackducksoftware.integration.hub.jenkins;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import hudson.model.AbstractProject;
import hudson.security.ACL;

public class HubServerInfo {

	private String serverUrl;

	private String hubCredentialsId;

	private UsernamePasswordCredentialsImpl credential;

	private int timeout;

	public HubServerInfo() {
	}

	public HubServerInfo(final String serverUrl, final String hubCredentialsId, final int timeout) {
		this.serverUrl = serverUrl;
		this.hubCredentialsId = hubCredentialsId;
		this.timeout = timeout;
	}

	public static int getDefaultTimeout() {
		return 120;
	}

	public int getTimeout() {
		if (timeout == 0) {
			return getDefaultTimeout();
		}
		return timeout;
	}

	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(final String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String getCredentialsId() {
		return hubCredentialsId;
	}

	public void setCredentialsId(final String hubCredentialsId) {
		this.hubCredentialsId = hubCredentialsId;
	}

	public boolean isPluginConfigured() {
		return StringUtils.isNotBlank(getServerUrl()) && StringUtils.isNotBlank(getCredentialsId());
	}

	public String getUsername() {
		final UsernamePasswordCredentialsImpl creds = getCredential();
		if (creds == null) {
			return null;
		} else {
			return creds.getUsername();
		}
	}

	public String getPassword() {
		final UsernamePasswordCredentialsImpl creds = getCredential();
		if (creds == null) {
			return null;
		} else {
			return creds.getPassword().getPlainText();
		}

	}

	public UsernamePasswordCredentialsImpl getCredential() {
		// Only need to look up the credential when you first run a build or if the credential that the user wants to
		// use has changed.
		if (credential == null || !credential.getId().equals(hubCredentialsId)) {
			final AbstractProject<?, ?> project = null;
			final List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
					project, ACL.SYSTEM,
					Collections.<DomainRequirement> emptyList());
			final IdMatcher matcher = new IdMatcher(hubCredentialsId);
			for (final StandardCredentials c : credentials) {
				if (matcher.matches(c) && c instanceof UsernamePasswordCredentialsImpl) {
					credential = (UsernamePasswordCredentialsImpl) c;
				}
			}
		}
		return credential;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("HubServerInfo [serverUrl=");
		builder.append(serverUrl);
		builder.append(", hubCredentialsId=");
		builder.append(hubCredentialsId);
		builder.append(", credential=");
		builder.append(credential);
		builder.append(", timeout=");
		builder.append(timeout);
		builder.append("]");
		return builder.toString();
	}

}
