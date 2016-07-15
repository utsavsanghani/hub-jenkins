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
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.builder.HubScanJobConfigBuilder;
import com.blackducksoftware.integration.hub.builder.ValidationResults;
import com.blackducksoftware.integration.hub.capabilities.HubCapabilitiesEnum;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.hub.exception.VersionDoesNotExistException;
import com.blackducksoftware.integration.hub.jenkins.action.BomUpToDateAction;
import com.blackducksoftware.integration.hub.jenkins.action.HubReportAction;
import com.blackducksoftware.integration.hub.jenkins.action.HubScanFinishedAction;
import com.blackducksoftware.integration.hub.jenkins.bom.RemoteBomGenerator;
import com.blackducksoftware.integration.hub.jenkins.cli.DummyToolInstallation;
import com.blackducksoftware.integration.hub.jenkins.cli.DummyToolInstaller;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubScanToolMissingException;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;
import com.blackducksoftware.integration.hub.jenkins.remote.CLIRemoteInstall;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCLI;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCLIExists;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCLIProvidedJRE;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCanonicalPath;
import com.blackducksoftware.integration.hub.jenkins.remote.GetHostName;
import com.blackducksoftware.integration.hub.jenkins.remote.GetHostNameFromNetworkInterfaces;
import com.blackducksoftware.integration.hub.jenkins.remote.GetIsOsWindows;
import com.blackducksoftware.integration.hub.jenkins.remote.GetOneJarFile;
import com.blackducksoftware.integration.hub.jenkins.remote.GetSystemProperty;
import com.blackducksoftware.integration.hub.jenkins.scan.JenkinsScanExecutor;
import com.blackducksoftware.integration.hub.job.HubScanJobConfig;
import com.blackducksoftware.integration.hub.job.HubScanJobFieldEnum;
import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.blackducksoftware.integration.hub.project.api.ProjectItem;
import com.blackducksoftware.integration.hub.report.api.HubReportGenerationInfo;
import com.blackducksoftware.integration.hub.version.api.ReleaseItem;
import com.blackducksoftware.integration.phone.home.api.PhoneHomeInfo;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;

public class PostBuildHubScan extends Recorder {
	private final ScanJobs[] scans;
	private final String hubProjectName;
	private final String hubVersionPhase;
	private final String hubVersionDist;
	private String hubProjectVersion;
	private final String scanMemory;
	private final boolean shouldGenerateHubReport;
	private String bomUpdateMaxiumWaitTime;
	private transient Result result;
	private Boolean verbose;
        private Calendar lastPhoneHome;

	// Old variable, renaming to hubProjectVersion
	// need to keep this around for now for migration purposes
	private String hubProjectRelease;

	// Hub Jenkins 1.4.1, renaming this variable to bomUpdateMaxiumWaitTime
	// need to keep this around for now for migration purposes
	private String reportMaxiumWaitTime;

	@DataBoundConstructor
	public PostBuildHubScan(final ScanJobs[] scans, final boolean sameAsBuildWrapper, final String hubProjectName,
			final String hubProjectVersion, final String hubVersionPhase, final String hubVersionDist,
			final String scanMemory, final boolean shouldGenerateHubReport, final String bomUpdateMaxiumWaitTime) {
		this.scans = scans;
		// 2016-06-27 ekerwin: we need to look in to whether we can remove the
		// boolean sameAsBuildWrapper from the constructor
		// this might break existing configurations and we removed the build
		// wrappers since we don't use recorders any more
		// this.sameAsBuildWrapper = sameAsBuildWrapper;
		this.hubProjectName = hubProjectName;
		this.hubProjectVersion = hubProjectVersion;
		this.hubVersionPhase = hubVersionPhase;
		this.hubVersionDist = hubVersionDist;
		this.scanMemory = scanMemory;
		this.shouldGenerateHubReport = shouldGenerateHubReport;
		this.bomUpdateMaxiumWaitTime = bomUpdateMaxiumWaitTime;
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

	public boolean getShouldGenerateHubReport() {
		return shouldGenerateHubReport;
	}

	public Result getResult() {
		return result;
	}

	private void setResult(final Result result) {
		this.result = result;
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

		final EnvVars variables = build.getEnvironment(listener);
		logger.setLogLevel(variables);
                
                

//@nrowles
//                bdPhoneHome();
////                String blackDuckName = "Hub";
////                String thirdPartyName = "Jenkins";
////                String blackDuckVersion = getHubProjectVersion();
////                String thirdPartyVersion = Jenkins.getVersion().toString();
////                PhoneHomeInfo info = new PhoneHomeInfo(blackDuckName, blackDuckVersion, thirdPartyName, thirdPartyVersion);
////                info.phoneHome();
//                PhoneHomeInfo dummyInfo = new PhoneHomeInfo("blackDuckName", "blackDuckVersion", "thirdPartyName", "thirdPartyVersion");
//                
//                dummyInfo.phoneHome();
                
		setResult(build.getResult());
		if (BuildHelper.isSuccess(build)) {
			try {
				logger.alwaysLog("Starting BlackDuck Scans...");

				final String localHostName = getLocalHostName(logger, build);

				if (validateGlobalConfiguration()) {
					final String workingDirectory = getWorkingDirectory(logger, build);

					final List<String> scanTargetPaths = getScanTargets(logger, build, variables, workingDirectory);

					String projectName = null;
					String projectVersion = null;

					if (StringUtils.isNotBlank(getHubProjectName()) && StringUtils.isNotBlank(getHubProjectVersion())) {
						projectName = BuildHelper.handleVariableReplacement(variables, getHubProjectName());
						projectVersion = BuildHelper.handleVariableReplacement(variables, getHubProjectVersion());

					}
					final HubScanJobConfigBuilder hubScanJobConfigBuilder = new HubScanJobConfigBuilder(true);
					hubScanJobConfigBuilder.setProjectName(projectName);
					hubScanJobConfigBuilder.setVersion(projectVersion);
					hubScanJobConfigBuilder.setPhase(getHubVersionPhase());
					hubScanJobConfigBuilder.setDistribution(getHubVersionDist());
					hubScanJobConfigBuilder.setWorkingDirectory(workingDirectory);
					hubScanJobConfigBuilder.setShouldGenerateRiskReport(getShouldGenerateHubReport());
					if (getBomUpdateMaxiumWaitTime() == null) {
						// If a User migrates from version 1.4.0 to here and
						// does not re-save the configuration the wait time will
						// be null
						hubScanJobConfigBuilder.setMaxWaitTimeForBomUpdate(
								HubScanJobConfigBuilder.DEFAULT_BOM_UPDATE_WAIT_TIME_IN_MINUTES);
					} else {
						hubScanJobConfigBuilder.setMaxWaitTimeForBomUpdate(getBomUpdateMaxiumWaitTime());
					}
					hubScanJobConfigBuilder.setScanMemory(getScanMemory());
					hubScanJobConfigBuilder.addAllScanTargetPaths(scanTargetPaths);
					hubScanJobConfigBuilder.disableScanTargetPathExistenceCheck();

					final ValidationResults<HubScanJobFieldEnum, HubScanJobConfig> builderResults = hubScanJobConfigBuilder
							.build();
					final HubScanJobConfig jobConfig = builderResults.getConstructedObject();

					printConfiguration(build, listener, logger, jobConfig);

					final DummyToolInstaller dummyInstaller = new DummyToolInstaller();
					final String toolsDirectory = dummyInstaller
							.getToolDir(new DummyToolInstallation(), build.getBuiltOn()).getRemote();

					final String scanExec = getScanCLI(logger, build.getBuiltOn(), toolsDirectory, localHostName,
							variables);

					final String jrePath = getJavaExec(logger, build, toolsDirectory);

					final String oneJarPath = getOneJarFile(build.getBuiltOn(), toolsDirectory);

					final HubIntRestService service = BuildHelper.getRestService(logger,
							getHubServerInfo().getServerUrl(), getHubServerInfo().getUsername(),
							getHubServerInfo().getPassword(), getHubServerInfo().getTimeout());
					ProjectItem project = null;
					ReleaseItem version = null;
					if (StringUtils.isNotBlank(projectName) && StringUtils.isNotBlank(projectVersion)) {
						project = ensureProjectExists(service, logger, projectName);
						version = ensureVersionExists(service, logger, projectVersion, project);
						logger.debug("Found Project : " + projectName);
						logger.debug("Found Version : " + projectVersion);
					}
					final HubSupportHelper hubSupport = new HubSupportHelper();
					hubSupport.checkHubSupport(service, logger);
                                        
                                        //@nrowles
                                        String hubVersion = hubSupport.getHubVersion(service);
                                        bdPhoneHome(hubVersion);
                                        
					final JenkinsScanExecutor scan = new JenkinsScanExecutor(getHubServerInfo(),
							jobConfig.getScanTargetPaths(), build.getNumber(), hubSupport, build, launcher, logger);

					final DateTime beforeScanTime = new DateTime();
					runScan(service, build, scan, logger, scanExec, jrePath, oneJarPath, jobConfig);
					final DateTime afterScanTime = new DateTime();

					final BomUpToDateAction bomUpdatedAction = new BomUpToDateAction();
					if (getResult().equals(Result.SUCCESS) && getShouldGenerateHubReport()) {

						final HubReportGenerationInfo reportGenInfo = new HubReportGenerationInfo();
						reportGenInfo.setService(service);
						reportGenInfo.setHostname(localHostName);
						reportGenInfo.setProject(project);
						reportGenInfo.setVersion(version);
						reportGenInfo.setScanTargets(jobConfig.getScanTargetPaths());

						reportGenInfo.setMaximumWaitTime(jobConfig.getMaxWaitTimeForBomUpdateInMilliseconds());

						reportGenInfo.setBeforeScanTime(beforeScanTime);
						reportGenInfo.setAfterScanTime(afterScanTime);

						reportGenInfo.setScanStatusDirectory(scan.getScanStatusDirectoryPath());

						generateHubReport(build, logger, reportGenInfo, getHubServerInfo(), hubSupport,
								bomUpdatedAction);
					} else {
						bomUpdatedAction.setHasBomBeenUdpated(false);
						bomUpdatedAction.setAfterScanTime(afterScanTime);
						bomUpdatedAction.setBeforeScanTime(beforeScanTime);
						bomUpdatedAction.setLocalHostName(localHostName);
						bomUpdatedAction.setMaxWaitTime(jobConfig.getMaxWaitTimeForBomUpdateInMilliseconds());
						bomUpdatedAction.setScanStatusDirectory(scan.getScanStatusDirectoryPath());
						bomUpdatedAction.setScanTargets(jobConfig.getScanTargetPaths());
					}
					if (version != null && hubSupport.hasCapability(HubCapabilitiesEnum.POLICY_API)) {
						bomUpdatedAction.setPolicyStatusUrl(version.getLink(ReleaseItem.POLICY_STATUS_LINK));
					}

					build.addAction(bomUpdatedAction);
				}
			} catch (final BDJenkinsHubPluginException e) {
				logger.error(e.getMessage(), e);
				setResult(Result.UNSTABLE);
			} catch (final HubIntegrationException e) {
				logger.error(e.getMessage(), e);
				setResult(Result.UNSTABLE);
			} catch (final Exception e) {
				String message;
				if (e.getMessage() != null && e.getMessage().contains("Project could not be found")) {
					message = e.getMessage();
				} else {

					if (e.getCause() != null && e.getCause().getCause() != null) {
						message = e.getCause().getCause().toString();
					} else if (e.getCause() != null) {
						message = e.getCause().toString();
					} else {
						message = e.toString();
					}
					if (message.toLowerCase().contains("service unavailable")) {
						message = Messages.HubBuildScan_getCanNotReachThisServer_0_(getHubServerInfo().getServerUrl());
					} else if (message.toLowerCase().contains("precondition failed")) {
						message = message + ", Check your configuration.";
					}
				}
				logger.error(message, e);
				setResult(Result.UNSTABLE);
			} finally {
				// Add this action to the Build so we know if the scan ran
				// before the Failure Conditions
				build.addAction(new HubScanFinishedAction());
			}
		} else {
			logger.alwaysLog("Build was not successful. Will not run Black Duck Scans.");
		}
		logger.alwaysLog("Finished running Black Duck Scans.");
		build.setResult(getResult());
		return true;
	}

	public String getLocalHostName(final IntLogger logger, final AbstractBuild<?, ?> build)
			throws InterruptedException {
		String localHostName = "";
		try {
			localHostName = build.getBuiltOn().getChannel().call(new GetHostName());
		} catch (final IOException e) {
			// ignore the error, try to get the host name from the network
			// interfaces
		}
		if (StringUtils.isBlank(localHostName)) {
			try {
				localHostName = build.getBuiltOn().getChannel().call(new GetHostNameFromNetworkInterfaces());
			} catch (final IOException e) {
				logger.error("Problem getting the Local Host name : " + e.getMessage(), e);
			}
		}
		logger.info("Hub Plugin running on machine : " + localHostName);
		return localHostName;
	}

	public String getWorkingDirectory(final IntLogger logger, final AbstractBuild<?, ?> build)
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
		return workingDirectory;
	}

	public List<String> getScanTargets(final IntLogger logger, final AbstractBuild<?, ?> build, final EnvVars variables,
			final String workingDirectory) throws BDJenkinsHubPluginException, InterruptedException {
		final List<String> scanTargetPaths = new ArrayList<String>();
		final ScanJobs[] scans = getScans();
		if (scans == null || scans.length == 0) {
			scanTargetPaths.add(workingDirectory);
		} else {
			for (final ScanJobs scanJob : scans) {
				if (StringUtils.isEmpty(scanJob.getScanTarget())) {
					scanTargetPaths.add(workingDirectory);
				} else {
					String target = BuildHelper.handleVariableReplacement(variables, scanJob.getScanTarget().trim());
					// make sure the target provided doesn't already begin with
					// a slash or end in a slash
					// removes the slash if the target begins or ends with one
					final File targetFile = new File(workingDirectory, target);

					try {
						target = build.getBuiltOn().getChannel().call(new GetCanonicalPath(targetFile));
					} catch (final IOException e) {
						logger.error("Problem getting the real path of the target : " + target
								+ " on this node. Error : " + e.getMessage(), e);
					}
					scanTargetPaths.add(target);
				}
			}
		}
		return scanTargetPaths;
	}

	private void generateHubReport(final AbstractBuild<?, ?> build, final HubJenkinsLogger logger,
			final HubReportGenerationInfo reportGenInfo, final HubServerInfo serverInfo,
			final HubSupportHelper hubSupport, final BomUpToDateAction action) throws Exception {
		final HubReportAction reportAction = new HubReportAction(build);
		final RemoteBomGenerator remoteBomGenerator = new RemoteBomGenerator(reportGenInfo, hubSupport,
				build.getBuiltOn().getChannel());

		reportAction.setReportData(remoteBomGenerator.generateHubReport(logger));
		action.setHasBomBeenUdpated(true);
		build.addAction(reportAction);
	}

	protected ProjectItem ensureProjectExists(final HubIntRestService service, final IntLogger logger,
			final String projectName) throws IOException, URISyntaxException, BDJenkinsHubPluginException {
		ProjectItem project = null;
		try {
			project = service.getProjectByName(projectName);

		} catch (final ProjectDoesNotExistException e) {
			// Project was not found, try to create it
			try {
				final String projectUrl = service.createHubProject(projectName);
				project = service.getProject(projectUrl);
			} catch (final BDRestException e1) {
				if (e1.getResource() != null) {
					logger.error("Status : " + e1.getResource().getStatus().getCode());
					logger.error("Response : " + e1.getResource().getResponse().getEntityAsText());
				}
				throw new BDJenkinsHubPluginException("Problem creating the Project. ", e1);
			}
		} catch (final BDRestException e) {
			if (e.getResource() != null) {
				if (e.getResource() != null) {
					logger.error("Status : " + e.getResource().getStatus().getCode());
					logger.error("Response : " + e.getResource().getResponse().getEntityAsText());
				}
				throw new BDJenkinsHubPluginException("Problem getting the Project. ", e);
			}
		}

		return project;
	}

	/**
	 * Ensures the Version exists. Returns the version URL
	 *
	 * @throws UnexpectedHubResponseException
	 */
	protected ReleaseItem ensureVersionExists(final HubIntRestService service, final IntLogger logger,
			final String projectVersion, final ProjectItem project) throws IOException, URISyntaxException,
					BDJenkinsHubPluginException, UnexpectedHubResponseException {
		ReleaseItem version = null;
		try {
			version = service.getVersion(project, projectVersion);
			if (!version.getPhase().equals(getHubVersionPhase())) {
				logger.warn(
						"The selected Phase does not match the Phase of this Version. If you wish to update the Phase please do so in the Hub UI.");
			}
			if (!version.getDistribution().equals(getHubVersionDist())) {
				logger.warn(
						"The selected Distribution does not match the Distribution of this Version. If you wish to update the Distribution please do so in the Hub UI.");
			}
		} catch (final VersionDoesNotExistException e) {
			try {
				final String versionURL = service.createHubVersion(project, projectVersion, getHubVersionPhase(),
						getHubVersionDist());
				version = service.getProjectVersion(versionURL);
			} catch (final BDRestException e1) {
				if (e1.getResource() != null) {
					logger.error("Status : " + e1.getResource().getStatus().getCode());
					logger.error("Response : " + e1.getResource().getResponse().getEntityAsText());
				}
				throw new BDJenkinsHubPluginException("Problem creating the Version. ", e1);
			}
		} catch (final BDRestException e) {
			throw new BDJenkinsHubPluginException("Could not retrieve or create the specified version.", e);
		}
		return version;
	}

	public void printConfiguration(final AbstractBuild<?, ?> build, final BuildListener listener,
			final HubJenkinsLogger logger, final HubScanJobConfig jobConfig) throws IOException, InterruptedException {
		logger.alwaysLog("Initializing - Hub Jenkins Plugin - " + getDescriptor().getPluginVersion());

		if (StringUtils.isEmpty(build.getBuiltOn().getNodeName())) {
			// Empty node name indicates master
			logger.alwaysLog("-> Running on : master");
		} else {
			logger.alwaysLog("Running on : " + build.getBuiltOn().getNodeName());
		}
		logger.alwaysLog("-> Log Level : " + logger.getLogLevel());
		logger.alwaysLog("-> Using Url : " + getHubServerInfo().getServerUrl());
		logger.alwaysLog("-> Using Username : " + getHubServerInfo().getUsername());
		logger.alwaysLog("-> Using Build Full Name : " + build.getFullDisplayName());
		logger.alwaysLog("-> Using Build Number : " + build.getNumber());
		logger.alwaysLog("-> Using Build Workspace Path : " + build.getWorkspace().getRemote());
		logger.alwaysLog(
				"-> Using Hub Project Name : " + jobConfig.getProjectName() + ", Version : " + jobConfig.getVersion());

		logger.alwaysLog("-> Scanning the following targets  : ");
		for (final String target : jobConfig.getScanTargetPaths()) {
			logger.alwaysLog("-> " + target);
		}

		logger.alwaysLog("-> Generate Hub report : " + jobConfig.isShouldGenerateRiskReport());
		final String formattedTime = String.format("%d minutes", jobConfig.getMaxWaitTimeForBomUpdate());
		logger.alwaysLog("-> Maximum wait time for the BOM Update : " + formattedTime);
	}

	/**
	 * Validates that the target of the scanJob exists, creates a ProcessBuilder
	 * to run the shellscript and passes in the necessarry arguments, sets the
	 * JAVA_HOME of the Process Builder to the one that the User chose, starts
	 * the process and prints out all stderr and stdout to the Console Output.
	 *
	 */
	private void runScan(final HubIntRestService service, final AbstractBuild<?, ?> build,
			final JenkinsScanExecutor scan, final HubJenkinsLogger logger, final String scanExec, final String javaExec,
			final String oneJarPath, final HubScanJobConfig jobConfig) throws IOException, HubConfigurationException,
					InterruptedException, BDJenkinsHubPluginException, HubIntegrationException, URISyntaxException {
		validateScanTargets(logger, jobConfig.getScanTargetPaths(), jobConfig.getWorkingDirectory(),
				build.getBuiltOn().getChannel());
		scan.setLogger(logger);
		addProxySettingsToScanner(logger, scan);

		scan.setScanMemory(jobConfig.getScanMemory());
		scan.setWorkingDirectory(jobConfig.getWorkingDirectory());

		scan.setVerboseRun(isVerbose());
		if (StringUtils.isNotBlank(jobConfig.getProjectName()) && StringUtils.isNotBlank(jobConfig.getVersion())) {

			scan.setProject(jobConfig.getProjectName());
			scan.setVersion(jobConfig.getVersion());
		}
		final com.blackducksoftware.integration.hub.ScanExecutor.Result result = scan.setupAndRunScan(scanExec,
				oneJarPath, javaExec);
		if (result == com.blackducksoftware.integration.hub.ScanExecutor.Result.SUCCESS) {
			setResult(Result.SUCCESS);
		} else {
			setResult(Result.UNSTABLE);
		}
	}

	public void addProxySettingsToScanner(final IntLogger logger, final JenkinsScanExecutor scan)
			throws BDJenkinsHubPluginException, HubIntegrationException, URISyntaxException, MalformedURLException {
		final Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			final ProxyConfiguration proxyConfig = jenkins.proxy;
			if (proxyConfig != null) {

				final URL serverUrl = new URL(getHubServerInfo().getServerUrl());

				final Proxy proxy = ProxyConfiguration.createProxy(serverUrl.getHost(), proxyConfig.name,
						proxyConfig.port, proxyConfig.noProxyHost);

				if (proxy != Proxy.NO_PROXY && proxy.address() != null) {
					final InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
					if (StringUtils.isNotBlank(proxyAddress.getHostName()) && proxyAddress.getPort() != 0) {
						if (StringUtils.isNotBlank(jenkins.proxy.getUserName())
								&& StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
							scan.setProxyHost(proxyAddress.getHostName());
							scan.setProxyPort(proxyAddress.getPort());
							scan.setProxyUsername(jenkins.proxy.getUserName());
							scan.setProxyPassword(jenkins.proxy.getPassword());

						} else {
							scan.setProxyHost(proxyAddress.getHostName());
							scan.setProxyPort(proxyAddress.getPort());
						}
						if (logger != null) {
							logger.debug("Using proxy: '" + proxyAddress.getHostName() + "' at Port: '"
									+ proxyAddress.getPort() + "'");
						}
					}
				}
			}
		}
	}

	public void addProxySettingsToCLIInstaller(final IntLogger logger, final CLIRemoteInstall remoteCLIInstall)
			throws BDJenkinsHubPluginException, HubIntegrationException, URISyntaxException, MalformedURLException {
		final Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			final ProxyConfiguration proxyConfig = jenkins.proxy;
			if (proxyConfig != null) {

				final URL serverUrl = new URL(getHubServerInfo().getServerUrl());

				final Proxy proxy = ProxyConfiguration.createProxy(serverUrl.getHost(), proxyConfig.name,
						proxyConfig.port, proxyConfig.noProxyHost);

				if (proxy != Proxy.NO_PROXY && proxy.address() != null) {
					final InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
					if (StringUtils.isNotBlank(proxyAddress.getHostName()) && proxyAddress.getPort() != 0) {
						if (StringUtils.isNotBlank(jenkins.proxy.getUserName())
								&& StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
							remoteCLIInstall.setProxyHost(proxyAddress.getHostName());
							remoteCLIInstall.setProxyPort(proxyAddress.getPort());
							remoteCLIInstall.setProxyUserName(jenkins.proxy.getUserName());
							remoteCLIInstall.setProxyPassword(jenkins.proxy.getPassword());

						} else {
							remoteCLIInstall.setProxyHost(proxyAddress.getHostName());
							remoteCLIInstall.setProxyPort(proxyAddress.getPort());
						}
						if (logger != null) {
							logger.debug("Using proxy: '" + proxyAddress.getHostName() + "' at Port: '"
									+ proxyAddress.getPort() + "'");
						}
					}
				}
			}
		}
	}

	public String getScanCLI(final HubJenkinsLogger logger, final Node node, final String toolsDirectory,
			final String localHostName, final EnvVars variables) throws IOException, InterruptedException, Exception {

		if (getHubServerInfo() == null) {
			logger.error("Could not find the Hub server information.");
			return null;
		}
		final CLIRemoteInstall remoteCLIInstall = new CLIRemoteInstall(logger, toolsDirectory, localHostName,
				getHubServerInfo().getServerUrl(), getHubServerInfo().getUsername(), getHubServerInfo().getPassword(),
				variables);

		addProxySettingsToCLIInstaller(logger, remoteCLIInstall);

		node.getChannel().call(remoteCLIInstall);

		final GetCLIExists cliExists = new GetCLIExists(logger, toolsDirectory);
		FilePath scanExec = null;
		if (node.getChannel().call(cliExists)) {
			final GetCLI getCLi = new GetCLI(logger, toolsDirectory);
			scanExec = new FilePath(node.getChannel(), node.getChannel().call(getCLi));
			logger.debug("Using this BlackDuck scan CLI at : " + scanExec.getRemote());
		} else {
			throw new HubScanToolMissingException("Could not find the CLI file to execute.");
		}
		return scanExec.getRemote();
	}

	public String getJavaExec(final HubJenkinsLogger logger, final AbstractBuild<?, ?> build,
			final String toolsDirectory) throws IOException, InterruptedException, Exception {
		final GetCLIProvidedJRE getProvidedJre = new GetCLIProvidedJRE(toolsDirectory);
		String jrePath = build.getBuiltOn().getChannel().call(getProvidedJre);

		if (StringUtils.isBlank(jrePath)) {
			final JDK java = determineJava(logger, build);

			FilePath javaExec = new FilePath(build.getBuiltOn().getChannel(), java.getHome());
			javaExec = new FilePath(javaExec, "bin");
			if (build.getBuiltOn().getChannel().call(new GetIsOsWindows())) {
				javaExec = new FilePath(javaExec, "java.exe");
			} else {
				javaExec = new FilePath(javaExec, "java");
			}

			jrePath = javaExec.getRemote();
		}
		return jrePath;
	}

	public String getOneJarFile(final Node node, final String toolsDirectory)
			throws IOException, InterruptedException, Exception {
		final GetOneJarFile getOneJar = new GetOneJarFile(toolsDirectory);
		return node.getChannel().call(getOneJar);
	}

	/**
	 * Sets the Java Home that is to be used for running the Shell script
	 *
	 */
	private JDK determineJava(final HubJenkinsLogger logger, final AbstractBuild<?, ?> build)
			throws IOException, InterruptedException, HubConfigurationException {
		JDK javaHomeTemp = null;

		final EnvVars envVars = build.getEnvironment(logger.getJenkinsListener());
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

	/**
	 * Validates that the Plugin is configured correctly. Checks that the User
	 * has defined an iScan tool, a Hub server URL, a Credential, and that there
	 * are at least one scan Target/Job defined in the Build
	 *
	 */
	public boolean validateGlobalConfiguration() throws HubScanToolMissingException, HubConfigurationException {

		if (getHubServerInfo() == null) {
			throw new HubConfigurationException("Could not find the Hub global configuration.");
		}
		if (!getHubServerInfo().isPluginConfigured()) {
			// If plugin is not Configured, we try to find out what is missing.
			if (StringUtils.isEmpty(getHubServerInfo().getServerUrl())) {
				throw new HubConfigurationException("No Hub URL was provided.");
			}
			if (StringUtils.isEmpty(getHubServerInfo().getCredentialsId())) {
				throw new HubConfigurationException("No credentials could be found to connect to the Hub.");
			}
		}
		// No exceptions were thrown so return true
		return true;
	}

	/**
	 * Validates that all scan targets exist
	 *
	 */
	public boolean validateScanTargets(final IntLogger logger, final List<String> scanTargets,
			final String workingDirectory, final VirtualChannel channel)
					throws IOException, HubConfigurationException, InterruptedException {
		for (final String currTarget : scanTargets) {

			if (currTarget.length() < workingDirectory.length() || !currTarget.startsWith(workingDirectory)) {
				throw new HubConfigurationException("Can not scan targets outside of the workspace.");
			}

			final FilePath currentTargetPath = new FilePath(channel, currTarget);

			if (!currentTargetPath.exists()) {
				throw new IOException("Scan target could not be found : " + currTarget);
			} else {
				logger.debug("Scan target exists at : " + currTarget);
			}
		}
		return true;
	}
        
        //@nrowles
        public void bdPhoneHome(String blackDuckVersion){
            String blackDuckName = "Hub";
            String thirdPartyName = "Jenkins";
            String thirdPartyVersion = Jenkins.getVersion().toString();
            PhoneHomeInfo info = new PhoneHomeInfo(blackDuckName, blackDuckVersion, thirdPartyName, thirdPartyVersion);
            info.phoneHome();
//            PhoneHomeInfo dummyInfo = new PhoneHomeInfo("blackDuckName", "blackDuckVersion", "thirdPartyName", "thirdPartyVersion");
//                
//            dummyInfo.phoneHome();
        }

}
