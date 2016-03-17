package com.blackducksoftware.integration.hub.jenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.ProxyConfiguration;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

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
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import jenkins.model.Jenkins;

import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTime;
import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.jenkins.action.HubScanFinishedAction;
import com.blackducksoftware.integration.hub.jenkins.action.HubReportAction;
import com.blackducksoftware.integration.hub.jenkins.cli.HubScanInstallation;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubScanToolMissingException;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCanonicalPath;
import com.blackducksoftware.integration.hub.jenkins.remote.GetHostName;
import com.blackducksoftware.integration.hub.jenkins.remote.GetHostNameFromNetworkInterfaces;
import com.blackducksoftware.integration.hub.jenkins.remote.GetIsOsWindows;
import com.blackducksoftware.integration.hub.jenkins.remote.GetSystemProperty;
import com.blackducksoftware.integration.hub.jenkins.scan.JenkinsScanExecutor;
import com.blackducksoftware.integration.hub.polling.HubEventPolling;
import com.blackducksoftware.integration.hub.report.api.VersionReport;
import com.blackducksoftware.integration.hub.response.ReleaseItem;
import com.blackducksoftware.integration.hub.response.ReportFormatEnum;
import com.blackducksoftware.integration.hub.response.ReportMetaInformationItem;
import com.blackducksoftware.integration.hub.response.ReportMetaInformationItem.ReportMetaLinkItem;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;

public class PostBuildHubScan extends Recorder {

    public static final int DEFAULT_MEMORY = 4096;

    // Default wait 5 minutes for the report
    public static final long DEFAULT_REPORT_WAIT_TIME = 5;

    private final ScanJobs[] scans;

    protected final boolean sameAsBuildWrapper;

    private final String hubProjectName;

    private final String hubVersionPhase;

    private final String hubVersionDist;

    private String hubProjectVersion;

    // Old variable, renaming to hubProjectVersion
    // need to keep this around for now for migration purposes
    private String hubProjectRelease;

    private final Integer scanMemory;

    protected final boolean shouldGenerateHubReport;

    protected final long reportMaxiumWaitTime;

    private transient FilePath workingDirectory;

    private transient JDK java;

    private transient Result result;

    private Boolean verbose;

    @DataBoundConstructor
    public PostBuildHubScan(ScanJobs[] scans, boolean sameAsBuildWrapper, String hubProjectName, String hubProjectVersion,
            String hubVersionPhase, String hubVersionDist,
            String scanMemory, boolean shouldGenerateHubReport, String reportMaxiumWaitTime) {
        this.scans = scans;
        this.sameAsBuildWrapper = sameAsBuildWrapper;
        if (StringUtils.isNotBlank(hubProjectName)) {
            this.hubProjectName = hubProjectName.trim();
        } else {
            this.hubProjectName = null;
        }
        if (StringUtils.isNotBlank(hubProjectVersion)) {
            this.hubProjectVersion = hubProjectVersion.trim();
        } else {
            this.hubProjectVersion = null;
        }
        this.hubVersionPhase = hubVersionPhase;
        this.hubVersionDist = hubVersionDist;
        Integer memory;
        try {
            memory = Integer.valueOf(scanMemory);
            if (memory == 0) {
                memory = DEFAULT_MEMORY;
            }
        } catch (NumberFormatException e) {
            memory = DEFAULT_MEMORY;
        }

        this.scanMemory = memory;

        if (StringUtils.isBlank(hubProjectName) || StringUtils.isBlank(hubProjectVersion)) {
            // Dont want to generate the report if they have not provided a Project name or version
            this.shouldGenerateHubReport = false;
        } else {
            this.shouldGenerateHubReport = shouldGenerateHubReport;
        }

        Long longValueWaitTime;

        try {
            // maxiumWaitTimeForBomUpdate needs to be a String because the UI stores a string on save
            longValueWaitTime = Long.valueOf(reportMaxiumWaitTime);
            if (longValueWaitTime == 0) {
                longValueWaitTime = DEFAULT_REPORT_WAIT_TIME;
            }
        } catch (NumberFormatException e) {
            // Ignore the exception here, use the default value instead;
            longValueWaitTime = DEFAULT_REPORT_WAIT_TIME;
        }

        this.reportMaxiumWaitTime = longValueWaitTime;

    }

    public void setverbose(boolean verbose) {
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

    private void setResult(Result result) {
        this.result = result;
    }

    public String getDefaultMemory() {
        return String.valueOf(DEFAULT_MEMORY);
    }

    public String getScanMemory() {
        return String.valueOf(scanMemory);
    }

    public String getReportMaxiumWaitTime() {
        // need to return a String for the Ui to display correctly
        if (reportMaxiumWaitTime == 0) {
            return getDefaultReportWaitTime();
        }
        return String.valueOf(reportMaxiumWaitTime);
    }

    public String getDefaultReportWaitTime() {
        return String.valueOf(DEFAULT_REPORT_WAIT_TIME);
    }

    public long getConvertedReportMaxiumWaitTime() {
        // Converts the minutes that the User set to milliseconds
        return reportMaxiumWaitTime * 1000 * 60;
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

    public FilePath getWorkingDirectory() {
        return workingDirectory;
    }

    private void setWorkingDirectory(VirtualChannel channel, String workingDirectory) {
        this.workingDirectory = new FilePath(channel, workingDirectory);
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
     * @param build
     *            AbstractBuild
     * @param launcher
     *            Launcher
     * @param listener
     *            BuildListener
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        HubJenkinsLogger logger = new HubJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.TRACE); // TODO make the log level configurable
        setResult(build.getResult());
        if (BuildHelper.isSuccess(build)) {
            try {
                logger.info("Starting BlackDuck Scans...");

                String localHostName = "";
                try {
                    localHostName = build.getBuiltOn().getChannel().call(new GetHostName());
                } catch (IOException e) {
                    // logger.error("Problem getting the Local Host name : " + e.getMessage(), e);
                    // ignore the error, try to get the host name from the network interfaces
                }
                if (StringUtils.isBlank(localHostName)) {
                    try {
                        localHostName = build.getBuiltOn().getChannel().call(new GetHostNameFromNetworkInterfaces());
                    } catch (IOException e) {
                        logger.error("Problem getting the Local Host name : " + e.getMessage(), e);
                    }
                }
                logger.info("Hub Plugin running on machine : " + localHostName);

                if (validateConfiguration()) {
                    // This set the base of the scan Target, DO NOT remove this or the user will be able to specify any
                    // file even outside of the Jenkins directories
                    File workspace = null;
                    if (build.getWorkspace() == null) {
                        // might be using custom workspace
                        workspace = new File(build.getProject().getCustomWorkspace());
                    } else {
                        workspace = new File(build.getWorkspace().getRemote());
                    }
                    String workingDirectory = "";
                    try {
                        workingDirectory = build.getBuiltOn().getChannel().call(new GetCanonicalPath(workspace));
                    } catch (IOException e) {
                        logger.error("Problem getting the working directory on this node. Error : " + e.getMessage(), e);
                    }
                    logger.info("Node workspace " + workingDirectory);
                    VirtualChannel remotingChannel = build.getBuiltOn().getChannel();
                    setWorkingDirectory(remotingChannel, workingDirectory);
                    EnvVars variables = build.getEnvironment(listener);
                    List<String> scanTargets = new ArrayList<String>();

                    ScanJobs[] scans = getScans();
                    if (scans == null) {
                        // They deleted all the scan targets in the Job config and then saved,
                        // So the ScanJobs[] will be null
                        scans = new ScanJobs[1];
                        ScanJobs scan = new ScanJobs(" ");
                        scans[0] = scan;
                    }

                    for (ScanJobs scanJob : scans) {
                        if (StringUtils.isEmpty(scanJob.getScanTarget())) {

                            scanTargets.add(getWorkingDirectory().getRemote());
                        } else {
                            // trim the target so there are no false whitespaces at the beginning or end of the target
                            // path
                            String target = handleVariableReplacement(variables, scanJob.getScanTarget().trim());
                            // make sure the target provided doesn't already begin with a slash or end in a slash
                            // removes the slash if the target begins or ends with one
                            File targetFile = new File(getWorkingDirectory().getRemote(), target);

                            try {
                                target = build.getBuiltOn().getChannel().call(new GetCanonicalPath(targetFile));
                            } catch (IOException e) {
                                logger.error("Problem getting the real path of the target : " + target + " on this node. Error : " + e.getMessage(), e);
                            }
                            scanTargets.add(target);
                        }
                    }
                    String projectName = null;
                    String projectVersion = null;

                    if (StringUtils.isNotBlank(getHubProjectName()) && StringUtils.isNotBlank(getHubProjectVersion())) {
                        projectName = handleVariableReplacement(variables, getHubProjectName());
                        projectVersion = handleVariableReplacement(variables, getHubProjectVersion());

                    }
                    printConfiguration(build, logger, projectName, projectVersion, scanTargets, getShouldGenerateHubReport(),
                            getConvertedReportMaxiumWaitTime());

                    HubScanInstallation scanInstallation = HubServerInfoSingleton.getInstance().getHubScanInstallation();
                    // Need to get the CLI for this Node, triggers the auto install
                    scanInstallation = scanInstallation.forNode(build.getBuiltOn(), logger.getJenkinsListener());

                    FilePath scanExec = getScanCLI(scanInstallation, logger, build.getBuiltOn());
                    setJava(scanInstallation, logger, build);

                    HubIntRestService service = BuildHelper.getRestService(logger, getHubServerInfo().getServerUrl(),
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
                    HubSupportHelper hubSupport = new HubSupportHelper();
                    hubSupport.checkHubSupport(service, logger);

                    JenkinsScanExecutor scan = new JenkinsScanExecutor(getHubServerInfo(), scanTargets, build.getNumber(), build, launcher,
                            logger.getJenkinsListener());

                    DateTime beforeScanTime = new DateTime();
                    Boolean mappingDone = runScan(service, build, scan, logger, scanExec, scanTargets, projectName, projectVersion, hubSupport);
                    DateTime afterScanTime = new DateTime();

                    // Only map the scans to a Project Version if the Project name and Project Version have been
                    // configured
                    if (!mappingDone && getResult().equals(Result.SUCCESS) && StringUtils.isNotBlank(projectName) && StringUtils.isNotBlank(projectVersion)) {
                        // Wait 5 seconds for the scans to be recognized in the Hub server
                        logger.info("Waiting a few seconds for the scans to be recognized by the Hub server.");
                        Thread.sleep(5000);

                        doScanMapping(service, localHostName, logger, versionId, scanTargets);
                    }

                    if (getResult().equals(Result.SUCCESS) && getShouldGenerateHubReport()) {

                        HubReportGenerationInfo reportGenInfo = new HubReportGenerationInfo();
                        reportGenInfo.setService(service);
                        reportGenInfo.setHostname(localHostName);
                        reportGenInfo.setProjectId(projectId);
                        reportGenInfo.setVersionId(versionId);
                        reportGenInfo.setScanTargets(scanTargets);

                        reportGenInfo.setMaximumWaitTime(getConvertedReportMaxiumWaitTime());

                        reportGenInfo.setBeforeScanTime(beforeScanTime);
                        reportGenInfo.setAfterScanTime(afterScanTime);

                        generateHubReport(build, logger, reportGenInfo, hubSupport, scan.getScanStatusDirectoryPath());
                    }
                }
            } catch (BDJenkinsHubPluginException e) {
                logger.error(e.getMessage(), e);
                setResult(Result.UNSTABLE);
            } catch (Exception e) {
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

    private void generateHubReport(AbstractBuild<?, ?> build, IntLogger logger, HubReportGenerationInfo reportGenInfo, HubSupportHelper hubSupport,
            String scanStatusDirectory)
            throws IOException, BDRestException, URISyntaxException, InterruptedException, BDJenkinsHubPluginException, HubIntegrationException {
        HubReportAction reportAction = new HubReportAction(build);

        // logger.debug("Time before scan : " + reportGenInfo.getBeforeScanTime().toString());
        // logger.debug("Time after scan : " + reportGenInfo.getAfterScanTime().toString());
        logger.debug("Waiting for the bom to be updated with the scan results.");
        HubEventPolling hubPoller = new HubEventPolling(reportGenInfo.getService());

        boolean isBomUpToDate = false;

        if (hubSupport.isCliStatusDirOptionSupport()) {
            if (hubPoller.isBomUpToDate(reportGenInfo.getScanTargets().size(), scanStatusDirectory, reportGenInfo.getMaximumWaitTime(), logger)) {
                isBomUpToDate = true;
            }
        } else {
            if (hubPoller.isBomUpToDate(reportGenInfo.getBeforeScanTime(), reportGenInfo.getAfterScanTime(),
                    reportGenInfo.getHostname(), reportGenInfo.getScanTargets(), reportGenInfo.getMaximumWaitTime())) {
                isBomUpToDate = true;
            }
        }

        if (isBomUpToDate) {
            logger.debug("The bom has been updated, generating the report.");
            String reportUrl = reportGenInfo.getService().generateHubReport(reportGenInfo.getVersionId(), ReportFormatEnum.JSON);

            DateTime timeFinished = null;
            ReportMetaInformationItem reportInfo = null;

            while (timeFinished == null) {
                // Wait until the report is done being generated
                // Retry every 5 seconds
                Thread.sleep(5000);
                reportInfo = reportGenInfo.getService().getReportLinks(reportUrl);

                timeFinished = reportInfo.getTimeFinishedAt();
            }

            List<ReportMetaLinkItem> links = reportInfo.get_meta().getLinks();

            ReportMetaLinkItem contentLink = null;
            for (ReportMetaLinkItem link : links) {
                if (link.getRel().equalsIgnoreCase("content")) {
                    contentLink = link;
                    break;
                }
            }
            if (contentLink == null) {
                throw new BDJenkinsHubPluginException("Could not find content link for the report at : " + reportUrl);
            }

            VersionReport report = reportGenInfo.getService().getReportContent(contentLink.getHref());
            reportAction.setReport(report);
            logger.debug("Finished retrieving the report.");

            reportGenInfo.getService().deleteHubReport(reportGenInfo.getVersionId(), reportGenInfo.getService().getReportIdFromReportUrl(reportUrl));

            build.addAction(reportAction);
        } else {
            // if the bom is not up to date then an exception will be thrown
        }
    }

    private String ensureProjectExists(HubIntRestService service, IntLogger logger, String projectName, String projectVersion) throws IOException,
            URISyntaxException,
            BDJenkinsHubPluginException {
        String projectId = null;
        try {
            projectId = service.getProjectByName(projectName).getId();

        } catch (ProjectDoesNotExistException e) {
            // Project was not found, try to create it
            try {

                projectId = service.createHubProjectAndVersion(projectName, projectVersion, getHubVersionPhase(), getHubVersionDist());
                logger.debug("Project and Version created!");

            } catch (BDRestException e1) {
                if (e1.getResource() != null) {
                    logger.error("Status : " + e1.getResource().getStatus().getCode());
                    logger.error("Response : " + e1.getResource().getResponse().getEntityAsText());
                }
                throw new BDJenkinsHubPluginException("Problem creating the Project. ", e1);
            }
        } catch (BDRestException e) {
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

    private String ensureVersionExists(HubIntRestService service, IntLogger logger, String projectVersion, String projectId) throws IOException,
            URISyntaxException, BDJenkinsHubPluginException {
        String versionId = null;
        try {

            List<ReleaseItem> projectVersions = service.getVersionsForProject(projectId);
            for (ReleaseItem release : projectVersions) {
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
        } catch (BDRestException e) {
            throw new BDJenkinsHubPluginException("Could not retrieve or create the specified version.", e);
        }
        return versionId;
    }

    private void doScanMapping(HubIntRestService service, String hostname, IntLogger logger, String versionId, List<String> scanTargets)
            throws IOException, BDRestException,
            BDJenkinsHubPluginException,
            InterruptedException, HubIntegrationException, URISyntaxException {

        Map<String, Boolean> scanLocationIds = service.getScanLocationIds(hostname, scanTargets, versionId);
        if (scanLocationIds != null && !scanLocationIds.isEmpty()) {
            logger.debug("These scan Id's were found for the scan targets.");
            for (Entry<String, Boolean> scanId : scanLocationIds.entrySet()) {
                logger.debug(scanId.getKey());
            }

            service.mapScansToProjectVersion(scanLocationIds, versionId);
        } else {
            logger.debug("There was an issue getting the Scan Location Id's for the defined scan targets.");
        }

    }

    /**
     *
     * @param variables
     *            Map of variables
     * @param value
     *            String to check for variables
     * @return the new Value with the variables replaced
     * @throws BDJenkinsHubPluginException
     */
    public String handleVariableReplacement(Map<String, String> variables, String value) throws BDJenkinsHubPluginException {
        if (value != null) {

            String newValue = Util.replaceMacro(value, variables);

            if (newValue.contains("$")) {
                throw new BDJenkinsHubPluginException("Variable was not properly replaced. Value : " + value + ", Result : " + newValue
                        + ". Make sure the variable has been properly defined.");
            }
            return newValue;
        } else {
            return null;
        }
    }

    public void printConfiguration(AbstractBuild<?, ?> build, IntLogger logger, String projectName, String projectVersion,
            List<String> scanTargets, boolean shouldGenerateReport, long reportMaxiumWaitTime)
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
                "-> Using Hub Project Name : " + projectName + ", Version : " + projectVersion);

        logger.info(
                "-> Scanning the following targets  : ");
        for (String target : scanTargets) {
            logger.info(
                    "-> " + target);
        }

        logger.info(
                "-> Generate Hub report : " + shouldGenerateReport);
        if (shouldGenerateReport) {
            String formattedTime = String.format("%d minutes", TimeUnit.MILLISECONDS.toMinutes(reportMaxiumWaitTime));
            logger.info(
                    "-> Maximum wait time for the report : " + formattedTime);
        }
    }

    /**
     * Validates that the target of the scanJob exists, creates a ProcessBuilder to run the shellscript and passes in
     * the necessarry arguments, sets the JAVA_HOME of the Process Builder to the one that the User chose, starts the
     * process and prints out all stderr and stdout to the Console Output.
     *
     * @param build
     *            AbstractBuild
     * @param launcher
     *            Launcher
     * @param listener
     *            BuildListener
     * @param scanExec
     *            FilePath
     * @param scanTargets
     *            List<String>
     *
     * @throws IOException
     * @throws HubConfigurationException
     * @throws InterruptedException
     * @throws BDRestException
     * @throws BDJenkinsHubPluginException
     * @throws URISyntaxException
     * @throws HubIntegrationException
     */
    private Boolean runScan(HubIntRestService service, AbstractBuild<?, ?> build, JenkinsScanExecutor scan, HubJenkinsLogger logger, FilePath scanExec,
            List<String> scanTargets,
            String projectName, String versionName, HubSupportHelper hubSupport)
            throws IOException, HubConfigurationException, InterruptedException, BDJenkinsHubPluginException, HubIntegrationException, URISyntaxException
    {
        validateScanTargets(logger, scanTargets, build.getBuiltOn().getChannel());
        Boolean mappingDone = false;

        FilePath oneJarPath = null;

        oneJarPath = new FilePath(scanExec.getParent(), "cache");

        oneJarPath = new FilePath(oneJarPath, "scan.cli.impl-standalone.jar");

        scan.setLogger(logger);
        addProxySettingsToScanner(logger, scan);

        scan.setHubSupportLogOption(hubSupport.isLogOptionSupport());
        scan.setScanMemory(scanMemory);
        scan.setWorkingDirectory(getWorkingDirectory().getRemote());

        scan.setShouldParseStatus(hubSupport.isCliStatusReturnSupport());

        scan.setVerboseRun(isVerbose());
        if (hubSupport.isCliMappingSupport() &&
                StringUtils.isNotBlank(projectName)
                && StringUtils.isNotBlank(versionName)) {

            scan.setCliSupportsMapping(hubSupport.isCliMappingSupport());
            scan.setProject(projectName);
            scan.setVersion(versionName);
            mappingDone = true;
        } else {
            scan.setCliSupportsMapping(false);
        }

        scan.setCliSupportStatusOption(hubSupport.isCliStatusDirOptionSupport());

        FilePath javaExec = new FilePath(build.getBuiltOn().getChannel(), getJava().getHome());
        javaExec = new FilePath(javaExec, "bin");
        if (build.getBuiltOn().getChannel().call(new GetIsOsWindows())) {
            javaExec = new FilePath(javaExec, "java.exe");
        } else {
            javaExec = new FilePath(javaExec, "java");
        }

        com.blackducksoftware.integration.hub.ScanExecutor.Result result = scan.setupAndRunScan(scanExec.getRemote(),
                oneJarPath.getRemote(), javaExec.getRemote());
        if (result == com.blackducksoftware.integration.hub.ScanExecutor.Result.SUCCESS) {
            setResult(Result.SUCCESS);
        } else {
            setResult(Result.UNSTABLE);
        }

        return mappingDone;
    }

    public void addProxySettingsToScanner(IntLogger logger, JenkinsScanExecutor scan) throws BDJenkinsHubPluginException, HubIntegrationException,
            URISyntaxException,
            MalformedURLException {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            ProxyConfiguration proxyConfig = jenkins.proxy;
            if (proxyConfig != null) {

                URL serverUrl = new URL(getHubServerInfo().getServerUrl());

                Proxy proxy = ProxyConfiguration.createProxy(serverUrl.getHost(), proxyConfig.name, proxyConfig.port,
                        proxyConfig.noProxyHost);

                if (proxy.address() != null) {
                    InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
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

    public JDK getJava() {
        return java;
    }

    /**
     * Sets the Java Home that is to be used for running the Shell script
     *
     * @param build
     *            AbstractBuild
     * @param listener
     *            BuildListener
     * @throws IOException
     * @throws InterruptedException
     * @throws HubConfigurationException
     */
    private void setJava(HubScanInstallation hubScanInstallation, HubJenkinsLogger logger, AbstractBuild<?, ?> build) throws IOException, InterruptedException,
            HubConfigurationException {
        JDK javaHomeTemp = null;

        FilePath providedJavaHome = hubScanInstallation.getProvidedJavaHome(build.getBuiltOn().getChannel());
        if (providedJavaHome != null) {
            javaHomeTemp = new JDK("Java packaged with ClI.", providedJavaHome.getRemote());
        } else {
            EnvVars envVars = build.getEnvironment(logger.getJenkinsListener());
            if (StringUtils.isEmpty(build.getBuiltOn().getNodeName())) {
                logger.info("Getting Jdk on master  : " + build.getBuiltOn().getNodeName());
                // Empty node name indicates master

                String byteCodeVersion = System.getProperty("java.class.version");
                Double majorVersion = Double.valueOf(byteCodeVersion);
                if (majorVersion >= 51.0) {
                    // Java 7 bytecode
                    String javaHome = System.getProperty("java.home");
                    javaHomeTemp = new JDK("Java running master agent", javaHome);
                } else {
                    javaHomeTemp = build.getProject().getJDK();
                }
            } else {
                logger.info("Getting Jdk on node  : " + build.getBuiltOn().getNodeName());

                String byteCodeVersion = build.getBuiltOn().getChannel().call(new GetSystemProperty("java.class.version"));
                Double majorVersion = Double.valueOf(byteCodeVersion);
                if (majorVersion >= 51.0) {
                    // Java 7 bytecode
                    String javaHome = build.getBuiltOn().getChannel().call(new GetSystemProperty("java.home"));
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
        }
        FilePath javaHome = new FilePath(build.getBuiltOn().getChannel(), javaHomeTemp.getHome());
        if (!javaHome.exists()) {
            throw new HubConfigurationException("Could not find the specified Java installation at: " +
                    javaHome.getRemote());
        }

        java = javaHomeTemp;
    }

    // private Integer getJavaMajorVersion(String versionString) {
    // String[] versionSplit = versionString.split(".");
    // Integer majorVersion = Integer.valueOf(versionSplit[0]);
    // return majorVersion;
    // }

    /**
     * Looks through the ScanInstallations to find the one that the User chose, then looks for the scan.cli.sh in the
     * bin folder of the directory defined by the Installation.
     * It then checks that the File exists.
     *
     * @param iScanTools
     *            IScanInstallation[] User defined iScan installations
     * @param listener
     *            BuildListener
     * @param node
     *            Node
     *
     * @return File the scan.cli.sh
     * @throws HubScanToolMissingException
     * @throws IOException
     * @throws InterruptedException
     * @throws HubConfigurationException
     */
    public FilePath getScanCLI(HubScanInstallation hubScanInstallation, HubJenkinsLogger logger, Node node)
            throws HubScanToolMissingException, IOException,
            InterruptedException, HubConfigurationException {
        FilePath scanExecutable = null;

        if (hubScanInstallation == null) {
            // Should not get here unless we have not setup the auto-install CLI correctly
            // But we check this just in case
            throw new HubConfigurationException("You need to select which BlackDuck scan installation to use.");
        }
        if (hubScanInstallation.getExists(node.getChannel(), logger)) {
            scanExecutable = hubScanInstallation.getCLI(node.getChannel());
            logger.debug("Using this BlackDuck scan CLI at : " + scanExecutable.getRemote());
        } else {
            logger.error("Could not find the CLI file in : " + hubScanInstallation.getHome());
            throw new HubScanToolMissingException("Could not find the CLI file to execute at : '" + hubScanInstallation.getHome() + "'");
        }
        return scanExecutable;
    }

    /**
     * Validates that the Plugin is configured correctly. Checks that the User has defined an iScan tool, a Hub server
     * URL, a Credential, and that there are at least one scan Target/Job defined in the Build
     *
     * @param iScanTools
     *            IScanInstallation[] User defined iScan installations
     * @param scans
     *            IScanJobs[] the iScan jobs defined in the Job config
     *
     * @return True if everything is configured correctly
     *
     * @throws HubScanToolMissingException
     * @throws HubConfigurationException
     */
    public boolean validateConfiguration() throws HubScanToolMissingException,
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
        if (HubServerInfoSingleton.getInstance().getHubScanInstallation() == null) {
            try {
                PostBuildScanDescriptor.checkHubScanTool(getHubServerInfo().getServerUrl());
            } catch (Exception e) {
                throw new HubScanToolMissingException("Could not find an Black Duck Scan Installation to use.", e);
            }
        }
        // No exceptions were thrown so return true
        return true;
    }

    /**
     * Validates that all scan targets exist
     *
     * @param listener
     *            BuildListener
     * @param channel
     *            VirtualChannel
     * @param scanTargets
     *            List<String>
     *
     * @return
     * @throws IOException
     * @throws HubConfigurationException
     * @throws InterruptedException
     */
    public boolean validateScanTargets(IntLogger logger, List<String> scanTargets, VirtualChannel channel) throws IOException,
            HubConfigurationException,
            InterruptedException {
        for (String currTarget : scanTargets) {

            String workingDir = getWorkingDirectory().getRemote();

            if (currTarget.length() <= workingDir.length()
                    && !workingDir.equals(currTarget) && !currTarget.contains(workingDir)) {
                throw new HubConfigurationException("Can not scan targets outside of the workspace.");
            }

            FilePath currentTargetPath = new FilePath(channel, currTarget);

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
