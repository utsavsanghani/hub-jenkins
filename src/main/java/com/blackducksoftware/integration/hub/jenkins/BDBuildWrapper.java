package com.blackducksoftware.integration.hub.jenkins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.ProxyConfiguration;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import jenkins.model.Jenkins;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

import com.blackducksoftware.integration.build.BuildInfo;
import com.blackducksoftware.integration.hub.BuilderType;
import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.maven.Scope;
import com.blackducksoftware.integration.hub.response.ReleaseItem;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;

/**
 * Sample {@link Builder}.
 * <p>
 * When the user configures the project and enables this builder, {@link DescriptorImpl#newInstance(StaplerRequest)} is
 * invoked and a new {@link BDBuildWrapper} is created. The created instance is persisted to the project configuration
 * XML by using XStream, so this allows you to use instance fields (like {@link #name}) to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be invoked.
 *
 * @author James Richard
 */
public abstract class BDBuildWrapper extends BuildWrapper {

    protected final String userScopesToInclude;

    protected final boolean sameAsPostBuildScan;

    private final String hubProjectName;

    private final String hubVersionPhase;

    private final String hubVersionDist;

    private final String hubProjectVersion;

    public BDBuildWrapper(String userScopesToInclude, boolean sameAsPostBuildScan, String hubProjectName, String hubVersionPhase,
            String hubVersionDist, String hubProjectVersion) {
        if (StringUtils.isNotBlank(userScopesToInclude)) {
            this.userScopesToInclude = userScopesToInclude.trim();
        } else {
            this.userScopesToInclude = null;
        }

        this.sameAsPostBuildScan = sameAsPostBuildScan;

        if (StringUtils.isNotBlank(hubProjectName)) {
            this.hubProjectName = hubProjectName.trim();
        } else {
            this.hubProjectName = null;
        }

        this.hubVersionPhase = hubVersionPhase;
        this.hubVersionDist = hubVersionDist;

        if (StringUtils.isNotBlank(hubProjectVersion)) {
            this.hubProjectVersion = hubProjectVersion.trim();
        } else {
            this.hubProjectVersion = null;
        }
    }

    public boolean isSameAsPostBuildScan() {
        return sameAsPostBuildScan;
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

    public String getHubProjectVersion() {
        return hubProjectVersion;
    }

    public String getUserScopesToInclude() {
        return userScopesToInclude;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public BDBuildWrapperDescriptor getDescriptor() {
        return (BDBuildWrapperDescriptor) super.getDescriptor();
    }

    public abstract List<String> getScopesAsList(IntLogger logger);

    @Override
    public abstract Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener) throws IOException, InterruptedException;

    public boolean universalTearDown(AbstractBuild build, IntLogger buildLogger, FilePath buildInfoFile, BDBuildWrapperDescriptor descriptor,
            BuilderType buidler) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException,
            IOException {

        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        try {
            if (BDBuildWrapper.class.getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(BDBuildWrapper.class.getClassLoader());
            }

            // result may be null if the build is successful and ongoing
            if (build == null) {
                buildLogger.error("Build: null. The Hub build wrapper will not run.");
            } else if (BuildHelper.isOngoing(build) || BuildHelper.isSuccess(build)) {
                if (isPluginEnabled() && validateConfiguration(buildLogger)) {
                    // // ** read build-info.json file
                    // BuildInfo buildInfo = readBuildInfo(buildInfoFile, build.getId(), buildLogger);
                    // CodeCenterCIFacade facade = getFacade(descriptor, buildLogger);
                    // // This initialized the report action and defines the Code
                    // // Center application
                    // CodeCenterGovernanceReportAction reportAction = new CodeCenterGovernanceReportAction(build);
                    // build.addAction(reportAction);
                    // CodeCenterApplication application = new CodeCenterApplication();
                    // application.setApplicationName(getCodeCenterApplicationName());
                    // application
                    // .setApplicationVersion(getCodeCenterApplicationVersion());
                    // reportAction.setApplication(application);
                    // GovernanceReportGenerator reportGenerator = new GovernanceReportGenerator(facade, buildInfo,
                    // getCodeCenterApplicationName(),
                    // getCodeCenterApplicationVersion(), isAutoCreateComponentRequests(), isSubmitComponentRequests(),
                    // isAutoSwitchNotInUseRequests(),
                    // getScopesAsList(buildLogger),
                    // build);
                    // Result result = build.getResult();
                    // try {
                    // result = reportGenerator.generateReport(reportAction, buildLogger, buidler);
                    // } catch (BDCIFacadeException e) {
                    // buildLogger.error(e.getMessage(), e);
                    // build.setResult(Result.UNSTABLE);
                    // }
                    // build.setResult(result);
                }
            } else {
                buildLogger.error("The build was not successful. The Code Center plugin will not run.");
            }
            return true;
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
            }
        }
    }

    public HubIntRestService getRestService(IntLogger logger) throws BDJenkinsHubPluginException, HubIntegrationException, URISyntaxException,
            MalformedURLException {
        HubIntRestService service = new HubIntRestService(getDescriptor().getHubServerInfo().getServerUrl());
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

    protected BuildInfo readBuildInfo(FilePath filePath, String buildId, IntLogger buildLogger) {
        BuildInfo buildInfo = new BuildInfo();
        // Gets the build-info.json file so we can retrieve
        // the dependencies that were recorded to it
        // This parses the build-info.json file for the
        // dependencies and resolves them to catalog
        // components
        InputStream in = null;
        try {
            buildLogger.info("Reading BuildInfo from " + (filePath.isRemote() ? "@" : "") + filePath.getRemote());
            in = filePath.read();

            buildInfo.parseFileForDependencies(in, buildId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    buildLogger.error(e.getMessage(), e);
                    // eat exception
                }
            }
        }
        return buildInfo;
    }

    private String ensureProjectExists(HubIntRestService service, IntLogger logger, String projectName) throws IOException, URISyntaxException,
            BDJenkinsHubPluginException {
        String projectId = null;
        try {
            projectId = service.getProjectByName(projectName).getId();

        } catch (BDRestException e) {
            if (e.getResource() != null) {
                if (e.getResource().getResponse().getStatus().getCode() == 404) {
                    // Project was not found, try to create it
                    try {

                        projectId = service.createHubProject(projectName);
                        logger.debug("Project created!");

                    } catch (BDRestException e1) {
                        if (e1.getResource() != null) {
                            logger.error("Status : " + e1.getResource().getStatus().getCode());
                            logger.error("Response : " + e1.getResource().getResponse().getEntityAsText());
                        }
                        throw new BDJenkinsHubPluginException("Problem creating the Project. ", e1);
                    }
                } else {
                    if (e.getResource() != null) {
                        logger.error("Status : " + e.getResource().getStatus().getCode());
                        logger.error("Response : " + e.getResource().getResponse().getEntityAsText());
                    }
                    throw new BDJenkinsHubPluginException("Problem getting the Project. ", e);
                }
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

    /**
     * Determine if plugin is enabled
     *
     * @return true if Code Center server info is complete and if
     *         CodeCenterApplication Name and CodeCenterApplicationVersion are
     *         not empty
     */
    protected boolean isPluginEnabled() {
        // Checks to make sure the user provided an application name and version
        // also checks to make sure a server url, username, and password were
        // provided
        BDBuildWrapperDescriptor descriptor = getDescriptor();
        HubServerInfo serverInfo = descriptor.getHubServerInfo();
        boolean isPluginConfigured = serverInfo != null
                && serverInfo.isPluginConfigured();
        boolean isPluginEnabled = StringUtils
                .isNotBlank(getHubProjectName()) &&
                StringUtils.isNotBlank(getHubVersionPhase()) &&
                StringUtils.isNotBlank(getHubVersionDist()) &&
                StringUtils.isNotBlank(getHubProjectVersion()) &&
                StringUtils.isNotBlank(getUserScopesToInclude());

        boolean scopesProvided = true;
        List<String> scopes = getScopesAsList(null);
        if (scopes == null || scopes.isEmpty()) {
            scopesProvided = false;
        }

        return isPluginConfigured && isPluginEnabled && scopesProvided;
    }

    public Boolean validateConfiguration(IntLogger logger) {

        return false;
    }

    protected void checkScopesValid(List<String> errorMessage, List<String> scopes) {
        if (scopes == null || scopes.isEmpty() || scopes.contains(Scope.UNKNOWNSCOPE.toString())) {
            errorMessage.add("you must provide at least one valid scope to include");
        }
    }

}