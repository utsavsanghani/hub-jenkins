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
package com.blackducksoftware.integration.hub.jenkins.helper;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.log.IntLogger;

import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import jenkins.model.Jenkins;

public class BuildHelper {

	public static boolean isSuccess(final AbstractBuild<?, ?> build) {
		return build.getResult() == Result.SUCCESS;
	}

	public static boolean isOngoing(final AbstractBuild<?, ?> build) {
		return build.getResult() == null;
	}

	public static HubIntRestService getRestService(final String serverUrl, final String username, final String password,
			final int hubTimeout) throws BDJenkinsHubPluginException, HubIntegrationException, URISyntaxException,
			MalformedURLException, BDRestException {

		final HubIntRestService service = new HubIntRestService(
				getRestConnection(null, serverUrl, username, password, hubTimeout));

		return service;
	}

	public static HubIntRestService getRestService(final IntLogger logger, final String serverUrl,
			final String username, final String password, final int hubTimeout) throws BDJenkinsHubPluginException,
			HubIntegrationException, URISyntaxException, MalformedURLException, BDRestException {

		final HubIntRestService service = new HubIntRestService(
				getRestConnection(logger, serverUrl, username, password, hubTimeout));

		return service;
	}

	public static RestConnection getRestConnection(final IntLogger logger, final String serverUrl,
			final String username, final String password, final int hubTimeout) throws BDJenkinsHubPluginException,
			HubIntegrationException, URISyntaxException, MalformedURLException, BDRestException {

		final RestConnection restConnection = new RestConnection(serverUrl);
		restConnection.setLogger(logger);
		restConnection.setTimeout(hubTimeout);

		final Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			final ProxyConfiguration proxyConfig = jenkins.proxy;
			if (proxyConfig != null) {

				final URL actualUrl = new URL(serverUrl);

				final Proxy proxy = ProxyConfiguration.createProxy(actualUrl.getHost(), proxyConfig.name,
						proxyConfig.port, proxyConfig.noProxyHost);

				if (proxy.address() != null) {
					final InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
					if (StringUtils.isNotBlank(proxyAddress.getHostName()) && proxyAddress.getPort() != 0) {
						if (StringUtils.isNotBlank(jenkins.proxy.getUserName())
								&& StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
							restConnection.setProxyProperties(proxyAddress.getHostName(), proxyAddress.getPort(), null,
									jenkins.proxy.getUserName(), jenkins.proxy.getPassword());
						} else {
							restConnection.setProxyProperties(proxyAddress.getHostName(), proxyAddress.getPort(), null,
									null, null);
						}
						if (logger != null) {
							logger.debug("Using proxy: '" + proxyAddress.getHostName() + "' at Port: '"
									+ proxyAddress.getPort() + "'");
						}
					}
				}
			}
		}
		if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
			restConnection.setCookies(username, password);
		}
		return restConnection;
	}

	public static String handleVariableReplacement(final Map<String, String> variables, final String value)
			throws BDJenkinsHubPluginException {
		if (value != null) {

			final String newValue = Util.replaceMacro(value, variables);

			if (newValue.contains("$")) {
				throw new BDJenkinsHubPluginException("Variable was not properly replaced. Value : " + value
						+ ", Result : " + newValue + ". Make sure the variable has been properly defined.");
			}
			return newValue;
		} else {
			return null;
		}
	}

}
