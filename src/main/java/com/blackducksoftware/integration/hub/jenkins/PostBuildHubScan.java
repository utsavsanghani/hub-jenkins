package com.blackducksoftware.integration.hub.jenkins;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubScanJobConfig;
import com.blackducksoftware.integration.hub.HubScanJobConfigBuilder;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
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
import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.blackducksoftware.integration.hub.logging.LogLevel;
import com.blackducksoftware.integration.hub.report.api.HubReportGenerationInfo;
import com.blackducksoftware.integration.hub.version.api.ReleaseItem;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.Util;
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

	protected final boolean sameAsBuildWrapper;

	private final String hubProjectName;

	private final String hubVersionPhase;

	private final String hubVersionDist;

	private String hubProjectVersion;

	// Old variable, renaming to hubProjectVersion
	// need to keep this around for now for migration purposes
	private String hubProjectRelease;

	private final String scanMemory;

	protected final boolean shouldGenerateHubReport;

	// Hub Jenkins 1.4.1, renaming this variable to bomUpdateMaxiumWaitTime
	// need to keep this around for now for migration purposes
	protected String reportMaxiumWaitTime;

	protected String bomUpdateMaxiumWaitTime;

	private transient Result result;

	private Boolean verbose;

	@DataBoundConstructor
	public PostBuildHubScan(final ScanJobs[] scans, final boolean sameAsBuildWrapper, final String hubProjectName, final String hubProjectVersion,
			final String hubVersionPhase, final String hubVersionDist,
			final String scanMemory, final boolean shouldGenerateHubReport,
			final String bomUpdateMaxiumWaitTime) {
		this.scans = scans;
		this.sameAsBuildWrapper = sameAsBuildWrapper;
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

	public boolean getSameAsBuildWrapper() {
		return sameAsBuildWrapper;
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
	 * Overrides the Recorder perform method. This is the method that gets called by Jenkins to run as a Post Build
	 * Action
	 *
	 */
	@Override
	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher,
			final BuildListener listener) throws InterruptedException, IOException {
		final HubJenkinsLogger logger = new HubJenkinsLogger(listener);
		logger.setLogLevel(LogLevel.TRACE);
		setResult(build.getResult());
		if (BuildHelper.isSuccess(build)) {
			try {
				logger.info("Starting BlackDuck Scans...");

				final String localHostName = getLocalHostName(logger, build);

				if (validateGlobalConfiguration()) {
					final String workingDirectory = getWorkingDirectory(logger, build);

					final EnvVars variables = build.getEnvironment(listener);

					final List<String> scanTargetPaths =getScanTargets(logger, build, variables, workingDirectory);

					String projectName = null;
					String projectVersion = null;

					if (StringUtils.isNotBlank(getHubProjectName()) && StringUtils.isNotBlank(getHubProjectVersion())) {
						projectName = handleVariableReplacement(variables, getHubProjectName());
						projectVersion = handleVariableReplacement(variables, getHubProjectVersion());

					}
					final HubScanJobConfigBuilder hubScanJobConfigBuilder = new HubScanJobConfigBuilder();
					hubScanJobConfigBuilder.setProjectName(projectName);
					hubScanJobConfigBuilder.setVersion(projectVersion);
					hubScanJobConfigBuilder.setPhase(getHubVersionPhase());
					hubScanJobConfigBuilder.setDistribution(getHubVersionDist());
					hubScanJobConfigBuilder.setWorkingDirectory(workingDirectory);
					hubScanJobConfigBuilder.setShouldGenerateRiskReport(getShouldGenerateHubReport());
					hubScanJobConfigBuilder.setMaxWaitTimeForBomUpdate(getBomUpdateMaxiumWaitTime());
					hubScanJobConfigBuilder.setScanMemory(getScanMemory());
					hubScanJobConfigBuilder.addAllScanTargetPaths(scanTargetPaths);
					hubScanJobConfigBuilder.disableScanTargetPathExistenceCheck();

					final HubScanJobConfig jobConfig = hubScanJobConfigBuilder.build(logger);

					printConfiguration(build, logger, jobConfig);

					final DummyToolInstaller dummyInstaller = new DummyToolInstaller();
					final String toolsDirectory = dummyInstaller.getToolDir(new DummyToolInstallation(), build.getBuiltOn()).getRemote();

					final String scanExec = getScanCLI(logger, build.getBuiltOn(), toolsDirectory, localHostName);

					final String jrePath = getJavaExec(logger, build, toolsDirectory);

					final String oneJarPath = getOneJarFile(build.getBuiltOn(), toolsDirectory);

					final HubIntRestService service = BuildHelper.getRestService(logger, getHubServerInfo().getServerUrl(),
							getHubServerInfo().getUsername(),
							getHubServerInfo().getPassword(),
							getHubServerInfo().getTimeout());
					String projectId = null;
					String versionId = null;
					if (StringUtils.isNotBlank(projectName) && StringUtils.isNotBlank(projectVersion)) {
						projectId = ensureProjectExists(service, logger, projectName, projectVersion);
						versionId = ensureVersionExists(service, logger, projectVersion, projectId);

						if (StringUtils.isEmpty(projectId)) {
							throw new BDJenkinsHubPluginException("The specified Project could not be found.");
						}

						logger.debug("Project Id: '" + projectId + "'");

						if (StringUtils.isEmpty(versionId)) {
							throw new BDJenkinsHubPluginException("The specified Version could not be found in the Project.");
						}
						logger.debug("Version Id: '" + versionId + "'");
					}
					final HubSupportHelper hubSupport = new HubSupportHelper();
					hubSupport.checkHubSupport(service, logger);

					final JenkinsScanExecutor scan = new JenkinsScanExecutor(getHubServerInfo(), jobConfig.getScanTargetPaths(), build.getNumber(), hubSupport,
							build, launcher,
							logger.getJenkinsListener());

					final DateTime beforeScanTime = new DateTime();
					runScan(service, build, scan, logger, scanExec, jrePath, oneJarPath, jobConfig);
					final DateTime afterScanTime = new DateTime();


					final BomUpToDateAction bomUpdatedAction = new BomUpToDateAction();
					if (getResult().equals(Result.SUCCESS) && getShouldGenerateHubReport()) {

						final HubReportGenerationInfo reportGenInfo = new HubReportGenerationInfo();
						reportGenInfo.setService(service);
						reportGenInfo.setHostname(localHostName);
						reportGenInfo.setProjectId(projectId);
						reportGenInfo.setVersionId(versionId);
						reportGenInfo.setScanTargets(jobConfig.getScanTargetPaths());

						reportGenInfo.setMaximumWaitTime(jobConfig.getMaxWaitTimeForRiskReportInMilliseconds());

						reportGenInfo.setBeforeScanTime(beforeScanTime);
						reportGenInfo.setAfterScanTime(afterScanTime);

						reportGenInfo.setScanStatusDirectory(scan.getScanStatusDirectoryPath());

						generateHubReport(build, logger, reportGenInfo,getHubServerInfo(), hubSupport, bomUpdatedAction);
					} else{
						bomUpdatedAction.setHasBomBeenUdpated(false);
						bomUpdatedAction.setAfterScanTime(afterScanTime);
						bomUpdatedAction.setBeforeScanTime(beforeScanTime);
						bomUpdatedAction.setLocalHostName(localHostName);
						bomUpdatedAction.setMaxWaitTime(jobConfig.getMaxWaitTimeForRiskReportInMilliseconds());
						bomUpdatedAction.setScanStatusDirectory(scan.getScanStatusDirectoryPath());
						bomUpdatedAction.setScanTargets(jobConfig.getScanTargetPaths());
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
				// Add this action to the Build so we know if the scan ran before the Failure Conditions
				build.addAction(new HubScanFinishedAction());
			}
		} else {
			logger.info("Build was not successful. Will not run Black Duck Scans.");
		}
		logger.info("Finished running Black Duck Scans.");
		build.setResult(getResult());
		return true;
	}

	public String getLocalHostName(final IntLogger logger, final AbstractBuild<?,?> build) throws InterruptedException{
		String localHostName = "";
		try {
			localHostName = build.getBuiltOn().getChannel().call(new GetHostName());
		} catch (final IOException e) {
			// logger.error("Problem getting the Local Host name : " + e.getMessage(), e);
			// ignore the error, try to get the host name from the network interfaces
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

	public String getWorkingDirectory(final IntLogger logger, final AbstractBuild<?,?> build) throws InterruptedException {
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

	public List<String> getScanTargets(final IntLogger logger, final AbstractBuild<?,?> build, final EnvVars variables, final String workingDirectory) throws BDJenkinsHubPluginException, InterruptedException{
		final List<String> scanTargetPaths = new ArrayList<String>();
		final ScanJobs[] scans = getScans();
		if (scans == null || scans.length == 0) {
			scanTargetPaths.add(workingDirectory);
		} else {
			for (final ScanJobs scanJob : scans) {
				if (StringUtils.isEmpty(scanJob.getScanTarget())) {
					scanTargetPaths.add(workingDirectory);
				} else {
					String target = handleVariableReplacement(variables, scanJob.getScanTarget().trim());
					// make sure the target provided doesn't already begin with a slash or end in a slash
					// removes the slash if the target begins or ends with one
					final File targetFile = new File(workingDirectory, target);

					try {
						target = build.getBuiltOn().getChannel().call(new GetCanonicalPath(targetFile));
					} catch (final IOException e) {
						logger.error("Problem getting the real path of the target : " + target + " on this node. Error : " + e.getMessage(), e);
					}
					scanTargetPaths.add(target);
				}
			}
		}
		return scanTargetPaths;
	}

	private void generateHubReport(final AbstractBuild<?, ?> build, final HubJenkinsLogger logger, final HubReportGenerationInfo reportGenInfo,final HubServerInfo serverInfo,
			final HubSupportHelper hubSupport, final BomUpToDateAction action)
					throws Exception {
		final HubReportAction reportAction = new HubReportAction(build);
		final RemoteBomGenerator remoteBomGenerator = new RemoteBomGenerator(reportGenInfo, hubSupport, build.getBuiltOn().getChannel());

		reportAction.setReportData(remoteBomGenerator.generateHubReport(logger));
		action.setHasBomBeenUdpated(true);
		build.addAction(reportAction);
	}

	private String ensureProjectExists(final HubIntRestService service, final IntLogger logger, final String projectName, final String projectVersion)
			throws IOException,
			URISyntaxException,
			BDJenkinsHubPluginException {
		String projectId = null;
		try {
			projectId = service.getProjectByName(projectName).getId();

		} catch (final ProjectDoesNotExistException e) {
			// Project was not found, try to create it
			try {

				projectId = service.createHubProjectAndVersion(projectName, projectVersion, getHubVersionPhase(), getHubVersionDist());
				logger.debug("Project and Version created!");

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

		return projectId;
	}

	private String ensureVersionExists(final HubIntRestService service, final IntLogger logger, final String projectVersion, final String projectId)
			throws IOException,
			URISyntaxException, BDJenkinsHubPluginException {
		String versionId = null;
		try {

			final List<ReleaseItem> projectVersions = service.getVersionsForProject(projectId);
			for (final ReleaseItem release : projectVersions) {
				if (projectVersion.equals(release.getVersion())) {
					versionId = release.getId();
					if (!release.getPhase().equals(getHubVersionPhase())) {
						logger.warn("The selected Phase does not match the Phase of this Version. If you wish to update the Phase please do so in the Hub UI.");
					}
					if (!release.getDistribution().equals(getHubVersionDist())) {
						logger.warn("The selected Distribution does not match the Distribution of this Version. If you wish to update the Distribution please do so in the Hub UI.");
					}
				}
			}
			if (versionId == null) {
				versionId = service.createHubVersion(projectVersion, projectId, getHubVersionPhase(), getHubVersionDist());
				logger.debug("Version created!");
			}
		} catch (final BDRestException e) {
			throw new BDJenkinsHubPluginException("Could not retrieve or create the specified version.", e);
		}
		return versionId;
	}

	public String handleVariableReplacement(final Map<String, String> variables, final String value) throws BDJenkinsHubPluginException {
		if (value != null) {

			final String newValue = Util.replaceMacro(value, variables);

			if (newValue.contains("$")) {
				throw new BDJenkinsHubPluginException("Variable was not properly replaced. Value : " + value + ", Result : " + newValue
						+ ". Make sure the variable has been properly defined.");
			}
			return newValue;
		} else {
			return null;
		}
	}

	public void printConfiguration(final AbstractBuild<?, ?> build, final IntLogger logger, final HubScanJobConfig jobConfig)
			throws IOException,
			InterruptedException {
		logger.info("Initializing - Hub Jenkins Plugin - "
				+ getDescriptor().getPluginVersion());

		if (StringUtils.isEmpty(build.getBuiltOn().getNodeName())) {
			// Empty node name indicates master
			logger.info("-> Running on : master");
		} else {
			logger.debug("Running on : " + build.getBuiltOn().getNodeName());
		}

		logger.info("-> Using Url : " + getHubServerInfo().getServerUrl());
		logger.info("-> Using Username : " + getHubServerInfo().getUsername());
		logger.info(
				"-> Using Build Full Name : " + build.getFullDisplayName());
		logger.info(
				"-> Using Build Number : " + build.getNumber());
		logger.info(
				"-> Using Build Workspace Path : "
						+ build.getWorkspace().getRemote());
		logger.info(
				"-> Using Hub Project Name : " + jobConfig.getProjectName() + ", Version : " + jobConfig.getVersion());

		logger.info(
				"-> Scanning the following targets  : ");
		for (final String target : jobConfig.getScanTargetPaths()) {
			logger.info(
					"-> " + target);
		}

		logger.info(
				"-> Generate Hub report : " + jobConfig.isShouldGenerateRiskReport());
		final String formattedTime = String.format("%d minutes",
				TimeUnit.MILLISECONDS.toMinutes(jobConfig.getMaxWaitTimeForRiskReportInMilliseconds()));
		logger.info("-> Maximum wait time for the BOM Update : " + formattedTime);
	}

	/**
	 * Validates that the target of the scanJob exists, creates a ProcessBuilder to run the shellscript and passes in
	 * the necessarry arguments, sets the JAVA_HOME of the Process Builder to the one that the User chose, starts the
	 * process and prints out all stderr and stdout to the Console Output.
	 *
	 */
	private void runScan(final HubIntRestService service, final AbstractBuild<?, ?> build, final JenkinsScanExecutor scan, final HubJenkinsLogger logger,
			final String scanExec, final String javaExec, final String oneJarPath,
			final HubScanJobConfig jobConfig)
					throws IOException, HubConfigurationException, InterruptedException, BDJenkinsHubPluginException, HubIntegrationException, URISyntaxException
	{
		validateScanTargets(logger, jobConfig.getScanTargetPaths(),jobConfig.getWorkingDirectory(), build.getBuiltOn().getChannel());
		scan.setLogger(logger);
		addProxySettingsToScanner(logger, scan);

		scan.setScanMemory(jobConfig.getScanMemory());
		scan.setWorkingDirectory(jobConfig.getWorkingDirectory());

		scan.setVerboseRun(isVerbose());
		if (StringUtils.isNotBlank(jobConfig.getProjectName())
				&& StringUtils.isNotBlank(jobConfig.getVersion())) {

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

	public void addProxySettingsToScanner(final IntLogger logger, final JenkinsScanExecutor scan) throws BDJenkinsHubPluginException, HubIntegrationException,
	URISyntaxException,
	MalformedURLException {
		final Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			final ProxyConfiguration proxyConfig = jenkins.proxy;
			if (proxyConfig != null) {

				final URL serverUrl = new URL(getHubServerInfo().getServerUrl());

				final Proxy proxy = ProxyConfiguration.createProxy(serverUrl.getHost(), proxyConfig.name, proxyConfig.port,
						proxyConfig.noProxyHost);

				if (proxy != Proxy.NO_PROXY && proxy.address() != null) {
					final InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
					if (StringUtils.isNotBlank(proxyAddress.getHostName()) && proxyAddress.getPort() != 0) {
						if (StringUtils.isNotBlank(jenkins.proxy.getUserName()) && StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
							scan.setProxyHost(proxyAddress.getHostName());
							scan.setProxyPort(proxyAddress.getPort());
							scan.setProxyUsername(jenkins.proxy.getUserName());
							scan.setProxyPassword(jenkins.proxy.getPassword());

						} else {
							scan.setProxyHost(proxyAddress.getHostName());
							scan.setProxyPort(proxyAddress.getPort());
						}
						if (logger != null) {
							logger.debug("Using proxy: '" + proxyAddress.getHostName() + "' at Port: '" + proxyAddress.getPort() + "'");
						}
					}
				}
			}
		}
	}

	public void addProxySettingsToCLIInstaller(final IntLogger logger, final CLIRemoteInstall remoteCLIInstall) throws BDJenkinsHubPluginException,
	HubIntegrationException,
	URISyntaxException,
	MalformedURLException {
		final Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			final ProxyConfiguration proxyConfig = jenkins.proxy;
			if (proxyConfig != null) {

				final URL serverUrl = new URL(getHubServerInfo().getServerUrl());

				final Proxy proxy = ProxyConfiguration.createProxy(serverUrl.getHost(), proxyConfig.name, proxyConfig.port,
						proxyConfig.noProxyHost);

				if (proxy != Proxy.NO_PROXY && proxy.address() != null) {
					final InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
					if (StringUtils.isNotBlank(proxyAddress.getHostName()) && proxyAddress.getPort() != 0) {
						if (StringUtils.isNotBlank(jenkins.proxy.getUserName()) && StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
							remoteCLIInstall.setProxyHost(proxyAddress.getHostName());
							remoteCLIInstall.setProxyPort(proxyAddress.getPort());
							remoteCLIInstall.setProxyUserName(jenkins.proxy.getUserName());
							remoteCLIInstall.setProxyPassword(jenkins.proxy.getPassword());

						} else {
							remoteCLIInstall.setProxyHost(proxyAddress.getHostName());
							remoteCLIInstall.setProxyPort(proxyAddress.getPort());
						}
						if (logger != null) {
							logger.debug("Using proxy: '" + proxyAddress.getHostName() + "' at Port: '" + proxyAddress.getPort() + "'");
						}
					}
				}
			}
		}
	}

	public String getScanCLI(final HubJenkinsLogger logger, final Node node, final String toolsDirectory, final String localHostName) throws IOException,
	InterruptedException, Exception {
		if (getHubServerInfo() == null) {
			logger.error("Could not find the Hub server information.");
			return null;
		}
		final CLIRemoteInstall remoteCLIInstall = new CLIRemoteInstall(logger, toolsDirectory, localHostName, getHubServerInfo().getServerUrl(),
				getHubServerInfo().getUsername(), getHubServerInfo().getPassword());

		addProxySettingsToCLIInstaller(logger, remoteCLIInstall);

		node.getChannel().call(remoteCLIInstall);

		final GetCLIExists cliExists = new GetCLIExists(logger, toolsDirectory);
		FilePath scanExec = null;
		if (node.getChannel().call(cliExists)) {
			final GetCLI getCLi = new GetCLI(toolsDirectory);
			scanExec = new FilePath(node.getChannel(), node.getChannel().call(getCLi));
			logger.debug("Using this BlackDuck scan CLI at : " + scanExec.getRemote());
		} else {
			throw new HubScanToolMissingException("Could not find the CLI file to execute.");
		}
		return scanExec.getRemote();
	}

	public String getJavaExec(final HubJenkinsLogger logger, final AbstractBuild<?, ?> build, final String toolsDirectory) throws IOException,
	InterruptedException, Exception {
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

	public String getOneJarFile(final Node node, final String toolsDirectory) throws IOException, InterruptedException, Exception {
		final GetOneJarFile getOneJar = new GetOneJarFile(toolsDirectory);
		return node.getChannel().call(getOneJar);
	}

	/**
	 * Sets the Java Home that is to be used for running the Shell script
	 *
	 */
	private JDK determineJava(final HubJenkinsLogger logger, final AbstractBuild<?, ?> build) throws IOException, InterruptedException,
	HubConfigurationException {
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

			final String byteCodeVersion = build.getBuiltOn().getChannel().call(new GetSystemProperty("java.class.version"));
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
			// In case the user did not select a java installation, set to the environment variable JAVA_HOME
			javaHomeTemp = new JDK("Default Java", envVars.get("JAVA_HOME"));
		}
		final FilePath javaHome = new FilePath(build.getBuiltOn().getChannel(), javaHomeTemp.getHome());
		if (!javaHome.exists()) {
			throw new HubConfigurationException("Could not find the specified Java installation at: " +
					javaHome.getRemote());
		}

		return javaHomeTemp;
	}

	/**
	 * Validates that the Plugin is configured correctly. Checks that the User has defined an iScan tool, a Hub server
	 * URL, a Credential, and that there are at least one scan Target/Job defined in the Build
	 *
	 */
	public boolean validateGlobalConfiguration() throws HubScanToolMissingException,
	HubConfigurationException {

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
	public boolean validateScanTargets(final IntLogger logger, final List<String> scanTargets, final String workingDirectory, final VirtualChannel channel) throws IOException,
	HubConfigurationException,
	InterruptedException {
		for (final String currTarget : scanTargets) {

			if (currTarget.length() < workingDirectory.length()
					|| !currTarget.startsWith(workingDirectory)) {
				throw new HubConfigurationException("Can not scan targets outside of the workspace.");
			}

			final FilePath currentTargetPath = new FilePath(channel, currTarget);

			if (!currentTargetPath.exists()) {
				throw new IOException("Scan target could not be found : " + currTarget);
			} else {
				logger.debug(
						"Scan target exists at : " + currTarget);
			}
		}
		return true;
	}

}
