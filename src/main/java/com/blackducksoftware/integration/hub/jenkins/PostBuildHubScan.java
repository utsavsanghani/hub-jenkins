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

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCanonicalPath;
import com.blackducksoftware.integration.hub.jenkins.remote.GetSystemProperty;
import com.blackducksoftware.integration.hub.jenkins.scan.HubCommonScanStep;
import com.blackducksoftware.integration.hub.logging.IntLogger;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

public class PostBuildHubScan extends Recorder {
	private final ScanJobs[] scans;
	private final String hubProjectName;
	private final String hubVersionPhase;
	private final String hubVersionDist;
	private String hubProjectVersion;
	private final String scanMemory;
	private final boolean shouldGenerateHubReport;
	private String bomUpdateMaxiumWaitTime;
	private final boolean dryRun;
	private Boolean verbose;

	// Old variable, renaming to hubProjectVersion
	// need to keep this around for now for migration purposes
	private String hubProjectRelease;

	// Hub Jenkins 1.4.1, renaming this variable to bomUpdateMaxiumWaitTime
	// need to keep this around for now for migration purposes
	private String reportMaxiumWaitTime;

	@DataBoundConstructor
	public PostBuildHubScan(final ScanJobs[] scans, final String hubProjectName,
			final String hubProjectVersion, final String hubVersionPhase, final String hubVersionDist,
			final String scanMemory, final boolean shouldGenerateHubReport, final String bomUpdateMaxiumWaitTime,
			final boolean dryRun) {
		this.scans = scans;
		this.hubProjectName = hubProjectName;
		this.hubProjectVersion = hubProjectVersion;
		this.hubVersionPhase = hubVersionPhase;
		this.hubVersionDist = hubVersionDist;
		this.scanMemory = scanMemory;
		this.shouldGenerateHubReport = shouldGenerateHubReport;
		this.bomUpdateMaxiumWaitTime = bomUpdateMaxiumWaitTime;
		this.dryRun = dryRun;
	}

	public void setverbose(final boolean verbose) {
		this.verbose = verbose;
	}

	public boolean isVerbose() {
		if (verbose == null) {
			verbose = true;
		}
		return verbose;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public boolean getShouldGenerateHubReport() {
		return shouldGenerateHubReport;
	}

	public String getScanMemory() {
		return scanMemory;
	}

	public String getBomUpdateMaxiumWaitTime() {
		if (bomUpdateMaxiumWaitTime == null && reportMaxiumWaitTime != null) {
			bomUpdateMaxiumWaitTime = reportMaxiumWaitTime;
		}
		return bomUpdateMaxiumWaitTime;
	}

	public String getHubProjectVersion() {
		if (hubProjectVersion == null && hubProjectRelease != null) {
			hubProjectVersion = hubProjectRelease;
		}
		return hubProjectVersion;
	}

	public String getHubProjectName() {
		return hubProjectName;
	}

	public String getHubVersionPhase() {
		return hubVersionPhase;
	}

	public String getHubVersionDist() {
		return hubVersionDist;
	}

	public ScanJobs[] getScans() {
		return scans;
	}

	// http://javadoc.jenkins-ci.org/hudson/tasks/Recorder.html
	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public PostBuildScanDescriptor getDescriptor() {
		return (PostBuildScanDescriptor) super.getDescriptor();
	}

	public HubServerInfo getHubServerInfo() {
		return HubServerInfoSingleton.getInstance().getServerInfo();
	}

	/**
	 * Overrides the Recorder perform method. This is the method that gets
	 * called by Jenkins to run as a Post Build Action
	 *
	 */
	@Override
	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
			throws InterruptedException, IOException {
		final HubJenkinsLogger logger = new HubJenkinsLogger(listener);

		try {
			final HubCommonScanStep scanStep = new HubCommonScanStep(getScans(), getHubProjectName(),
					getHubProjectVersion(), getHubVersionPhase(), getHubVersionDist(), getScanMemory(),
					getShouldGenerateHubReport(), getBomUpdateMaxiumWaitTime(), isDryRun(), isVerbose());
			final EnvVars envVars = build.getEnvironment(listener);

			final JDK jdk = determineJava(logger, build, envVars);
			final FilePath javaHome = new FilePath(build.getBuiltOn().getChannel(), jdk.getHome());
			scanStep.runScan(build, build.getBuiltOn(), envVars, getWorkingDirectory(logger, build), logger,
					launcher, listener,
					build.getFullDisplayName(), String.valueOf(build.getNumber()), javaHome);
		} catch (final Exception e) {
			logger.error(e);
		}
		return true;
	}

	public FilePath getWorkingDirectory(final IntLogger logger, final AbstractBuild<?, ?> build)
			throws InterruptedException {
		String workingDirectory = "";
		try {
			if (build.getWorkspace() == null) {
				// might be using custom workspace
				workingDirectory = build.getProject().getCustomWorkspace();
			} else {
				workingDirectory = build.getWorkspace().getRemote();
			}

			workingDirectory = build.getBuiltOn().getChannel().call(new GetCanonicalPath(new File(workingDirectory)));
		} catch (final IOException e) {
			logger.error("Problem getting the working directory on this node. Error : " + e.getMessage(), e);
		}
		logger.info("Node workspace " + workingDirectory);
		return new FilePath(build.getBuiltOn().getChannel(), workingDirectory);
	}

	/**
	 * Sets the Java Home that is to be used for running the Shell script
	 *
	 */
	private JDK determineJava(final HubJenkinsLogger logger, final AbstractBuild<?, ?> build, final EnvVars envVars)
			throws IOException, InterruptedException, HubConfigurationException {
		JDK javaHomeTemp = null;

		if (StringUtils.isEmpty(build.getBuiltOn().getNodeName())) {
			logger.info("Getting Jdk on master  : " + build.getBuiltOn().getNodeName());
			// Empty node name indicates master
			final String byteCodeVersion = System.getProperty("java.class.version");
			final Double majorVersion = Double.valueOf(byteCodeVersion);
			if (majorVersion >= 51.0) {
				// Java 7 bytecode
				final String javaHome = System.getProperty("java.home");
				javaHomeTemp = new JDK("Java running master agent", javaHome);
			} else {
				javaHomeTemp = build.getProject().getJDK();
			}
		} else {
			logger.info("Getting Jdk on node  : " + build.getBuiltOn().getNodeName());

			final String byteCodeVersion = build.getBuiltOn().getChannel()
					.call(new GetSystemProperty("java.class.version"));
			final Double majorVersion = Double.valueOf(byteCodeVersion);
			if (majorVersion >= 51.0) {
				// Java 7 bytecode
				final String javaHome = build.getBuiltOn().getChannel().call(new GetSystemProperty("java.home"));
				javaHomeTemp = new JDK("Java running slave agent", javaHome);
			} else {
				javaHomeTemp = build.getProject().getJDK().forNode(build.getBuiltOn(), logger.getJenkinsListener());
			}
		}
		if (javaHomeTemp != null && javaHomeTemp.getHome() != null) {
			logger.info("JDK home : " + javaHomeTemp.getHome());
		}

		if (javaHomeTemp == null || StringUtils.isEmpty(javaHomeTemp.getHome())) {
			logger.info("Could not find the specified Java installation, checking the JAVA_HOME variable.");
			if (envVars.get("JAVA_HOME") == null || envVars.get("JAVA_HOME") == "") {
				throw new HubConfigurationException("Need to define a JAVA_HOME or select an installed JDK.");
			}
			// In case the user did not select a java installation, set to the
			// environment variable JAVA_HOME
			javaHomeTemp = new JDK("Default Java", envVars.get("JAVA_HOME"));
		}
		final FilePath javaHome = new FilePath(build.getBuiltOn().getChannel(), javaHomeTemp.getHome());
		if (!javaHome.exists()) {
			throw new HubConfigurationException(
					"Could not find the specified Java installation at: " + javaHome.getRemote());
		}

		return javaHomeTemp;
	}
}