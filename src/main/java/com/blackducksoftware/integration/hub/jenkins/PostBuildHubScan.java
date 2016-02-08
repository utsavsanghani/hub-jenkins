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
import hudson.tools.ToolDescriptor;

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

import jenkins.model.Jenkins;

import org.codehaus.plexus.util.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.restlet.data.Status;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.IScanToolMissingException;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCanonicalPath;
import com.blackducksoftware.integration.hub.jenkins.remote.GetHostName;
import com.blackducksoftware.integration.hub.jenkins.remote.GetHostNameFromNetworkInterfaces;
import com.blackducksoftware.integration.hub.jenkins.remote.GetSeparator;
import com.blackducksoftware.integration.hub.jenkins.remote.GetSystemProperty;
import com.blackducksoftware.integration.hub.jenkins.scan.JenkinsScanExecutor;
import com.blackducksoftware.integration.hub.response.ReleaseItem;
import com.blackducksoftware.integration.hub.response.VersionComparison;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;

public class PostBuildHubScan extends Recorder {

    public static final int DEFAULT_MEMORY = 4096;

    private final ScanJobs[] scans;

    private final String scanName;

    protected final boolean sameAsBuildWrapper;

    private final String hubProjectName;

    private final String hubVersionPhase;

    private final String hubVersionDist;

    private String hubProjectVersion;

    // Old variable, renaming to hubProjectVersion
    // need to keep this around for now for migration purposes
    private String hubProjectRelease;

    private Integer scanMemory;

    private transient FilePath workingDirectory;

    private transient JDK java;

    private transient Result result;

    // private HubIntRestService service = null;

    private Boolean verbose;

    @DataBoundConstructor
    public PostBuildHubScan(ScanJobs[] scans, String scanName, boolean sameAsBuildWrapper, String hubProjectName, String hubProjectVersion,
            String hubVersionPhase, String hubVersionDist,
            String scanMemory) {
        this.scans = scans;
        this.scanName = scanName;
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
        Integer memory = 0;
        try {
            memory = Integer.valueOf(scanMemory);
        } catch (NumberFormatException e) {
            // return FormValidation.error(Messages
            // .HubBuildScan_getInvalidMemoryString());
        }

        if (memory == 0) {
            this.scanMemory = DEFAULT_MEMORY;
        } else {
            this.scanMemory = memory;
        }

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

    public Result getResult() {
        return result;
    }

    private void setResult(Result result) {
        this.result = result;
    }

    public String geDefaultMemory() {
        return String.valueOf(DEFAULT_MEMORY);
    }

    public String getScanMemory() {
        if (scanMemory == 0) {
            scanMemory = DEFAULT_MEMORY;
        }
        return String.valueOf(scanMemory);
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

    public String getScanName() {
        return scanName;
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
        return BuildStepMonitor.BUILD;
    }

    @Override
    public PostBuildScanDescriptor getDescriptor() {
        return (PostBuildScanDescriptor) super.getDescriptor();
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
    public boolean perform(AbstractBuild build, Launcher launcher,
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
                ScanInstallation[] iScanTools = null;
                ToolDescriptor<ScanInstallation> iScanDescriptor = (ToolDescriptor<ScanInstallation>) build.getDescriptorByName(ScanInstallation.class
                        .getSimpleName());
                iScanTools = iScanDescriptor.getInstallations();
                // installations?
                if (validateConfiguration(iScanTools, getScans())) {
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
                    setJava(logger, build);
                    EnvVars variables = build.getEnvironment(listener);
                    List<String> scanTargets = new ArrayList<String>();
                    for (ScanJobs scanJob : getScans()) {
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
                    printConfiguration(build, logger, projectName, projectVersion, scanTargets);

                    FilePath scanExec = getScanCLI(iScanTools, logger, build);

                    HubIntRestService service = getRestService(logger);
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
                    Boolean mappingDone = runScan(service, build, launcher, logger, scanExec, scanTargets, projectName, projectVersion);

                    // Only map the scans to a Project Version if the Project name and Project Version have been
                    // configured
                    if (!mappingDone && getResult().equals(Result.SUCCESS) && StringUtils.isNotBlank(projectName) && StringUtils.isNotBlank(projectVersion)) {
                        // Wait 5 seconds for the scans to be recognized in the Hub server
                        logger.info("Waiting a few seconds for the scans to be recognized by the Hub server.");
                        Thread.sleep(5000);

                        doScanMapping(service, localHostName, logger, versionId, scanTargets);
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
                        message = Messages.HubBuildScan_getCanNotReachThisServer_0_(getDescriptor().getHubServerInfo().getServerUrl());
                    } else if (message.toLowerCase().contains("precondition failed")) {
                        message = message + ", Check your configuration.";
                    }
                }
                logger.error(message, e);
                setResult(Result.UNSTABLE);
            }
        } else {
            logger.info("Build was not successful. Will not run Black Duck Scans.");
        }
        logger.info("Finished running Black Duck Scans.");
        build.setResult(getResult());
        return true;
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

    public HubIntRestService getRestService(IntLogger logger) throws BDJenkinsHubPluginException, HubIntegrationException, URISyntaxException,
            MalformedURLException, BDRestException {
        HubIntRestService service = new HubIntRestService(getDescriptor().getHubServerInfo().getServerUrl());
        service.setTimeout(getDescriptor().getHubServerInfo().getTimeout());
        service.setLogger(logger);
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            ProxyConfiguration proxyConfig = jenkins.proxy;
            if (proxyConfig != null) {

                URL serverUrl = new URL(getDescriptor().getHubServerInfo().getServerUrl());

                Proxy proxy = ProxyConfiguration.createProxy(serverUrl.getHost(), proxyConfig.name, proxyConfig.port,
                        proxyConfig.noProxyHost);

                if (proxy.address() != null) {
                    InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
                    if (StringUtils.isNotBlank(proxyAddress.getHostName()) && proxyAddress.getPort() != 0) {
                        if (StringUtils.isNotBlank(jenkins.proxy.getUserName()) && StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
                            service.setProxyProperties(proxyAddress.getHostName(), proxyAddress.getPort(), null, jenkins.proxy.getUserName(),
                                    jenkins.proxy.getPassword());
                        } else {
                            service.setProxyProperties(proxyAddress.getHostName(), proxyAddress.getPort(), null, null, null);
                        }
                        if (logger != null) {
                            logger.debug("Using proxy: '" + proxyAddress.getHostName() + "' at Port: '" + proxyAddress.getPort() + "'");
                        }
                    }
                }
            }
        }
        service.setCookies(getDescriptor().getHubServerInfo().getUsername(),
                getDescriptor().getHubServerInfo().getPassword());
        return service;
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

    public void printConfiguration(AbstractBuild build, IntLogger logger, String projectName, String projectVersion, List<String> scanTargets)
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

        logger.info("-> Using Url : " + getDescriptor().getHubServerInfo().getServerUrl());
        logger.info("-> Using Username : " + getDescriptor().getHubServerInfo().getUsername());
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
    private Boolean runScan(HubIntRestService service, AbstractBuild build, Launcher launcher, HubJenkinsLogger logger, FilePath scanExec,
            List<String> scanTargets,
            String projectName, String versionName)
            throws IOException, HubConfigurationException, InterruptedException, BDJenkinsHubPluginException, HubIntegrationException, URISyntaxException
    {
        validateScanTargets(logger, scanTargets, build.getBuiltOn().getChannel());
        VersionComparison logOptionComparison = null;
        VersionComparison mappingComparison = null;
        VersionComparison parseStatusComparison = null;
        Boolean mappingDone = false;
        try {
            // FIXME not very efficient, if it is 2.3.0 or older then we know the answers for the others
            // we dont need to make all three connections
            parseStatusComparison = service.compareWithHubVersion("2.3.0");

            mappingComparison = service.compareWithHubVersion("2.2.0");

            // The logDir option wasnt added until Hub version 2.0.1
            logOptionComparison = service.compareWithHubVersion("2.0.1");

        } catch (BDRestException e) {
            if (e.getResourceException().getStatus().equals(Status.CLIENT_ERROR_NOT_FOUND)) {
                // The Hub server is version 2.0.0 and the version endpoint does not exist
            } else {
                logger.error(e.getResourceException().getMessage());
            }
        }
        FilePath oneJarPath = null;

        oneJarPath = new FilePath(scanExec.getParent(), "cache");

        oneJarPath = new FilePath(oneJarPath, "scan.cli.impl-standalone.jar");

        JenkinsScanExecutor scan = new JenkinsScanExecutor(getDescriptor().getHubServerInfo().getServerUrl(), getDescriptor().getHubServerInfo().getUsername(),
                getDescriptor().getHubServerInfo().getPassword(), scanTargets, build.getNumber(), build, launcher, logger.getJenkinsListener());
        scan.setLogger(logger);
        addProxySettingsToScanner(logger, scan);

        if (logOptionComparison != null && logOptionComparison.getNumericResult() < 0) {
            // The logDir option wasnt added until Hub version 2.0.1
            // So if the result is that 2.0.1 is less than the actual version, we know that it supports the log option
            scan.setHubSupportLogOption(true);
        } else {
            scan.setHubSupportLogOption(false);
        }
        scan.setScanMemory(scanMemory);
        scan.setWorkingDirectory(getWorkingDirectory().getRemote());

        if (parseStatusComparison != null && parseStatusComparison.getNumericResult() <= 0) {
            // Should check the exit status code instead
            scan.setShouldParseStatus(false);
        } else {
            scan.setShouldParseStatus(true);
        }

        scan.setVerboseRun(isVerbose());
        if (mappingComparison != null && mappingComparison.getNumericResult() <= 0 &&
                StringUtils.isNotBlank(projectName)
                && StringUtils.isNotBlank(versionName)) {
            // FIXME Which version was this fixed in?

            // The project and release options werent working until Hub version 2.2.?
            // So if the result is that 2.2.0 is less than or equal to the actual version, we know that it supports
            // these options
            scan.setCliSupportsMapping(true);
            scan.setProject(projectName);
            scan.setVersion(versionName);
            mappingDone = true;
        } else {
            scan.setCliSupportsMapping(false);
        }

        String separator = null;
        try {
            separator = build.getBuiltOn().getChannel().call(new GetSeparator());
        } catch (IOException e) {
            logger.error("Problem getting the file separator on this node. Error : " + e.getMessage(), e);
            separator = File.separator;
        }

        FilePath javaExec = new FilePath(build.getBuiltOn().getChannel(), getJava().getHome());
        javaExec = new FilePath(javaExec, "bin");
        if (separator.equals("/")) {
            javaExec = new FilePath(javaExec, "java");
        } else {
            javaExec = new FilePath(javaExec, "java.exe");
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

                URL serverUrl = new URL(getDescriptor().getHubServerInfo().getServerUrl());

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
    private void setJava(HubJenkinsLogger logger, AbstractBuild build) throws IOException, InterruptedException,
            HubConfigurationException {
        EnvVars envVars = build.getEnvironment(logger.getJenkinsListener());
        JDK javaHomeTemp = null;
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
     * @param build
     *            AbstractBuild
     *
     * @return File the scan.cli.sh
     * @throws IScanToolMissingException
     * @throws IOException
     * @throws InterruptedException
     * @throws HubConfigurationException
     */
    public FilePath getScanCLI(ScanInstallation[] scanTools, HubJenkinsLogger logger, AbstractBuild build)
            throws IScanToolMissingException, IOException,
            InterruptedException, HubConfigurationException {
        FilePath scanExecutable = null;
        for (ScanInstallation scanInstallation : scanTools) {
            Node node = build.getBuiltOn();
            if (StringUtils.isEmpty(node.getNodeName())) {
                // Empty node name indicates master
                logger.debug("Running on : master");
            } else {
                logger.debug("Running on : " + node.getNodeName());
                scanInstallation = scanInstallation.forNode(node, logger.getJenkinsListener()); // Need to get the Slave
                                                                                                // iScan
            }
            if (scanInstallation.getName().equals(getScanName())) {
                if (scanInstallation.getExists(node.getChannel(), logger)) {
                    scanExecutable = scanInstallation.getCLI(node.getChannel());
                    if (scanExecutable == null) {
                        // Should not get here unless there are no iScan Installations defined
                        // But we check this just in case
                        throw new HubConfigurationException("You need to select which BlackDuck Scan installation to use.");
                    }
                    logger.debug("Using this BlackDuck Scan CLI at : " + scanExecutable.getRemote());
                } else {
                    logger.error("Could not find the CLI file in : " + scanInstallation.getHome());
                    throw new IScanToolMissingException("Could not find the CLI file to execute at : '" + scanInstallation.getHome() + "'");
                }
            }
        }
        if (scanExecutable == null) {
            // Should not get here unless there are no iScan Installations defined
            // But we check this just in case
            throw new HubConfigurationException("You need to select which BlackDuck Scan installation to use.");
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
     * @throws IScanToolMissingException
     * @throws HubConfigurationException
     */
    public boolean validateConfiguration(ScanInstallation[] iScanTools, ScanJobs[] scans) throws IScanToolMissingException, HubConfigurationException {
        if (iScanTools == null || iScanTools.length == 0 || iScanTools[0] == null) {
            throw new IScanToolMissingException("Could not find an Black Duck Scan Installation to use.");
        }
        if (scans == null || scans.length == 0) {
            throw new HubConfigurationException("Could not find any targets to scan.");
        }
        if (!getDescriptor().getHubServerInfo().isPluginConfigured()) {
            // If plugin is not Configured, we try to find out what is missing.
            if (StringUtils.isEmpty(getDescriptor().getHubServerInfo().getServerUrl())) {
                throw new HubConfigurationException("No Hub URL was provided.");
            }
            if (StringUtils.isEmpty(getDescriptor().getHubServerInfo().getCredentialsId())) {
                throw new HubConfigurationException("No credentials could be found to connect to the Hub.");
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
