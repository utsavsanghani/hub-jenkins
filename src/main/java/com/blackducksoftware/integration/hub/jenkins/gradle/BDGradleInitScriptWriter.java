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
package com.blackducksoftware.integration.hub.jenkins.gradle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.blackducksoftware.integration.build.BuildInfo;
import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.google.common.base.Charsets;
import com.google.gson.Gson;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.remoting.Which;

/**
 * Class to generate a Gradle initialization script
 *
 */
public class BDGradleInitScriptWriter {
	private final AbstractBuild<?, ?> build;

	private transient IntLogger buildLogger;

	/**
	 * The gradle initialization script constructor.
	 *
	 */
	public BDGradleInitScriptWriter(final AbstractBuild<?, ?> build, final IntLogger buildLogger) {
		this.build = build;
		this.buildLogger = buildLogger;
	}

	/**
	 * Generate the init script from a template
	 *
	 */
	public String generateInitScript() throws URISyntaxException, IOException, InterruptedException {
		final StringBuilder initScript = new StringBuilder();
		final InputStream templateStream = getClass().getResourceAsStream("/bdInitScript.gradle");
		final String templateAsString = IOUtils.toString(templateStream, Charsets.UTF_8.name());

		final Node buildOn = build.getBuiltOn();
		if (buildOn == null) {
			buildLogger.error("Node build on: null");
		} else {
			FilePath remoteDependencyDir = new FilePath(buildOn.getRootPath(), "cache");
			remoteDependencyDir = new FilePath(remoteDependencyDir, "hub-jenkins");
			removeSnapshots(remoteDependencyDir);

			final File gradleExtractorJar = Which.jarFile(getClass().getResource("/bdInitScript.gradle"));
			final File buildInfoJar = Which.jarFile(BuildInfo.class);
			final File gsonJar = Which.jarFile(Gson.class);

			copyDependenciesToRemote(remoteDependencyDir, gradleExtractorJar);
			copyDependenciesToRemote(remoteDependencyDir, buildInfoJar);
			copyDependenciesToRemote(remoteDependencyDir, gsonJar);

			String absoluteDependencyDirPath = remoteDependencyDir.getRemote();
			absoluteDependencyDirPath = absoluteDependencyDirPath.replace("\\", "/");
			final String str = templateAsString.replace("${pluginLibDir}", absoluteDependencyDirPath);
			initScript.append(str);
		}
		return initScript.toString();
	}

	public void copyDependenciesToRemote(final FilePath remoteDir, final File localDependencyFile) throws IOException, InterruptedException {

		if (!remoteDir.exists()) {
			remoteDir.mkdirs();
		}

		final FilePath remoteDependencyFilePath = new FilePath(remoteDir, localDependencyFile.getName());
		if (!remoteDependencyFilePath.exists()) {
			final FilePath localDependencyFilePath = new FilePath(localDependencyFile);
			localDependencyFilePath.copyTo(remoteDependencyFilePath);
		} else {
			if (remoteDependencyFilePath.getName().contains("SNAPSHOT")) {
				// Update Snapshot versions
				remoteDependencyFilePath.delete();
				final FilePath localDependencyFilePath = new FilePath(localDependencyFile);
				localDependencyFilePath.copyTo(remoteDependencyFilePath);
			}
		}

	}

	/**
	 * Force update of snap shots
	 */
	private void removeSnapshots(final FilePath remoteDir) throws IOException, InterruptedException {
		if (remoteDir == null) {
			return;
		}
		final List<FilePath> remoteFiles = remoteDir.list();
		if (remoteFiles == null || remoteFiles.size() == 0) {
			return;
		}
		for (final FilePath file : remoteDir.list()) {
			// SWe force SNAPSHOT updates by removing them before we do anything
			// else
			if (file.getName().contains("2014") || file.getName().contains("2015") || file.getName().contains("SNAPSHOT")) {
				file.delete();
			}
		}
	}

}
