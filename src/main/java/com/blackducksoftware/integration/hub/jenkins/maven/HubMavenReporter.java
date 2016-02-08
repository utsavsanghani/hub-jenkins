package com.blackducksoftware.integration.hub.jenkins.maven;

import hudson.Launcher;
import hudson.Util;
import hudson.ProxyConfiguration;
import hudson.maven.MavenBuildProxy;
import hudson.maven.MavenReporter;
import hudson.maven.MojoInfo;
import hudson.maven.MavenBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jenkins.model.Jenkins;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.build.BuildArtifact;
import com.blackducksoftware.integration.build.BuildDependency;
import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDCIScopeException;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.jenkins.BuildHelper;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.maven.Scope;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;

public class HubMavenReporter extends MavenReporter {

    private static final long serialVersionUID = -7476189251746648322L;

    protected final String userScopesToInclude;

    protected final boolean mavenSameAsPostBuildScan;

    private final String mavenHubProjectName;

    private final String mavenHubVersionPhase;

    private final String mavenHubVersionDist;

    private final String mavenHubProjectVersion;

    private transient BuildArtifact buildArtifact;

    private transient Set<BuildDependency> dependencies = new HashSet<BuildDependency>();

    @DataBoundConstructor
    public HubMavenReporter(String userScopesToInclude, boolean mavenSameAsPostBuildScan, String mavenHubProjectName, String mavenHubVersionPhase,
            String mavenHubVersionDist, String mavenHubProjectVersion) {
        if (StringUtils.isNotBlank(userScopesToInclude)) {
            this.userScopesToInclude = userScopesToInclude.trim();
        } else {
            this.userScopesToInclude = null;
        }

        this.mavenSameAsPostBuildScan = mavenSameAsPostBuildScan;

        if (StringUtils.isNotBlank(mavenHubProjectName)) {
            this.mavenHubProjectName = mavenHubProjectName.trim();
        } else {
            this.mavenHubProjectName = null;
        }

        this.mavenHubVersionPhase = mavenHubVersionPhase;
        this.mavenHubVersionDist = mavenHubVersionDist;

        if (StringUtils.isNotBlank(mavenHubProjectVersion)) {
            this.mavenHubProjectVersion = mavenHubProjectVersion.trim();
        } else {
            this.mavenHubProjectVersion = null;
        }
    }

    public boolean isMavenSameAsPostBuildScan() {
        return mavenSameAsPostBuildScan;
    }

    public String getMavenHubProjectName() {
        return mavenHubProjectName;
    }

    public String getMavenHubVersionPhase() {
        return mavenHubVersionPhase;
    }

    public String getMavenHubVersionDist() {
        return mavenHubVersionDist;
    }

    public String getMavenHubProjectVersion() {
        return mavenHubProjectVersion;
    }

    public String getUserScopesToInclude() {
        return userScopesToInclude;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public HubMavenReporterDescriptor getDescriptor() {
        return (HubMavenReporterDescriptor) super.getDescriptor();
    }

    public List<String> getScopesAsList(IntLogger buildLogger) {
        List<String> scopesToInclude = new ArrayList<String>();
        try {

            scopesToInclude = Scope.getScopeListFromString(userScopesToInclude);
        } catch (BDCIScopeException e) {
            // The invalid scope should have been caught by the on-the-fly validation
            // This should not be reached
            if (buildLogger != null) {
                buildLogger.error(e.getMessage());
            }
            return null;
        }
        return scopesToInclude;
    }

    @Override
    public boolean preBuild(MavenBuildProxy build, MavenProject pom,
            BuildListener listener) throws InterruptedException, IOException {

        HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
        buildLogger.setLogLevel(LogLevel.TRACE); // TODO make the log level configurable
        buildLogger.info("Collecting dependencies info");
        dependencies = new HashSet<BuildDependency>();
        buildArtifact = createBuildArtifact(pom);
        // build.registerAsProjectAction(new Foo());
        return super.preBuild(build, pom, listener);
    }

    /**
     * Mojos perform different dependency resolution, so we add dependencies for
     * each mojo.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public boolean postExecute(MavenBuildProxy build, MavenProject pom,
            MojoInfo mojo, BuildListener listener, Throwable error)
            throws InterruptedException, IOException {

        HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
        buildLogger.setLogLevel(LogLevel.TRACE); // TODO make the log level configurable
        buildLogger.debug("postExecute()");
        recordMavenDependencies(pom.getArtifacts());
        build.registerAsProjectAction(this);
        return super.postExecute(build, pom, mojo, listener, error);
    }

    /**
     * Sends the collected dependencies over to the master and record them.
     */
    @Override
    public boolean postBuild(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException,
            IOException {
        HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
        buildLogger.setLogLevel(LogLevel.TRACE); // TODO make the log level configurable
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        try {
            if (HubMavenReporter.class.getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(HubMavenReporter.class.getClassLoader());
            }
            buildLogger.debug("postBuild()");
            HubBuildCallable callable = new HubBuildCallable(buildLogger, buildArtifact, dependencies);

            build.execute(callable);
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
            }
        }

        return super.postBuild(build, pom, listener);
    }

    /*
     * (non-JSDoc)
     * 
     * @see hudson.maven.MavenReporter#end(hudson.maven.MavenBuild, hudson.Launcher, hudson.model.BuildListener)
     */
    @Override
    public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
        buildLogger.setLogLevel(LogLevel.TRACE); // TODO make the log level configurable
        buildLogger.info("Hub Jenkins Plugin version : " + getDescriptor().getPluginVersion());
        if (build == null) {
            buildLogger.error("Build: null. The Hub Maven Reporter will not run.");
        } else if (BuildHelper.isOngoing(build) || BuildHelper.isSuccess(build)) {
            if (validateConfiguration(buildLogger)) {
                ClassLoader originalClassLoader = Thread.currentThread()
                        .getContextClassLoader();
                boolean changed = false;

                AbstractBuild<?, ?> rootBuild = build.getRootBuild();
                if (rootBuild == null) {
                    // Single module was built
                    rootBuild = build;
                }
                try {
                    if (HubMavenReporter.class.getClassLoader() != originalClassLoader) {
                        changed = true;
                        Thread.currentThread().setContextClassLoader(HubMavenReporter.class.getClassLoader());
                    }

                    // add action
                    // boolean reportComplete = false;
                    // CodeCenterGovernanceReportAction reportAction =
                    // rootBuild.getAction(CodeCenterGovernanceReportAction.class);
                    // if (reportAction == null) {
                    // reportAction = new CodeCenterGovernanceReportAction(rootBuild);
                    // buildLogger.debug("Adding Action " + reportAction);
                    // rootBuild.addAction(reportAction);
                    // } else {
                    // reportComplete = true;
                    // }

                    BuildInfoAction biAction = rootBuild.getAction(BuildInfoAction.class);
                    if (biAction != null) {
                        buildLogger.debug(
                                "BuildInfo :" + biAction.getBuildInfo().getBuildId() + "==" + rootBuild.getId());
                        // We don't want that action anymore

                        // rootBuild.getActions().remove(biAction);

                        // GovernanceReportGenerator reportGenerator = new GovernanceReportGenerator(facade,
                        // biAction.getBuildInfo(),
                        // getCodeCenterApplicationName(), getCodeCenterApplicationVersion(),
                        // isAutoCreateComponentRequests(),
                        // isSubmitComponentRequests(), isAutoSwitchNotInUseRequests(),
                        // getScopesAsList(buildLogger), build);
                        //
                        // Result result = rootBuild.getResult();
                        // try {
                        // result = reportGenerator.generateReport(reportAction, buildLogger, BuilderType.MAVEN);
                        // } catch (BDCIFacadeException e) {
                        // buildLogger.error(e.getMessage(), e);
                        // rootBuild.setResult(Result.UNSTABLE);
                        // }

                        // rootBuild.setResult(result);

                    } else {
                        // if (!reportComplete) {
                        buildLogger.warn("No buildInfoAction found on build: " + rootBuild.getId());
                        // } else {
                        // The report has already been completed.
                        // This method runs after the entire multi-module build is done
                        // This is called for each module
                        // We only need one report
                        // }
                    }
                    // for (hudson.model.Action a : build.getActions()) {
                    // buildLogger.info("Action: " + a.getDisplayName() + " (" + a.getClass().getName() + ")");
                    // if (a instanceof CodeCenterGovernanceReportAction) {
                    // CodeCenterGovernanceReportAction b = (CodeCenterGovernanceReportAction) a;
                    // buildLogger.info("** Application: " + b.getApplication() + "::" + b.getBuild());
                    // }
                    // }
                } catch (Exception e1) {
                    buildLogger.error(e1.getMessage(), e1);
                    rootBuild.setResult(Result.UNSTABLE);
                } finally {
                    if (changed) {
                        Thread.currentThread().setContextClassLoader(
                                originalClassLoader);
                    }
                }
            } else {
                AbstractBuild<?, ?> rootBuild = build.getRootBuild();
                rootBuild.setResult(Result.UNSTABLE);
            }
        } else {
            buildLogger.error("The build was not successful. The Code Center plugin will not run.");
        }
        return super.end(build, launcher, listener);
    }

    private BuildArtifact createBuildArtifact(MavenProject pom) {
        BuildArtifact artifact = new BuildArtifact();
        artifact.setType("org.apache.maven");
        artifact.setGroup(pom.getGroupId());
        artifact.setArtifact(pom.getArtifactId());
        artifact.setVersion(pom.getVersion());
        artifact.setId(pom.getId());
        return artifact;
    }

    private void recordMavenDependencies(Set<Artifact> artifacts) {
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if (artifact.isResolved() && artifact.getFile() != null) {
                    BuildDependency mavenDependency = getDependencyFromArtifact(artifact);
                    dependencies.add(mavenDependency);
                }
            }
        }
    }

    /**
     * @param artifact
     * @return
     */
    private BuildDependency getDependencyFromArtifact(Artifact artifact) {
        BuildDependency dependency = new BuildDependency();
        dependency.setGroup(artifact.getGroupId());
        dependency.setArtifact(artifact.getArtifactId());
        dependency.setVersion(artifact.getVersion());
        dependency.setClassifier(artifact.getClassifier());
        ArrayList<String> scopes = new ArrayList<String>();
        scopes.add(artifact.getScope());
        dependency.setScope(scopes);
        dependency.setExtension(artifact.getType());
        return dependency;
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
     * Determine if plugin is enabled
     *
     * @return true if Code Center server info is complete and if
     *         CodeCenterApplication Name and CodeCenterApplicationVersion are
     *         not empty
     */
    public boolean isPluginEnabled() {
        // Checks to make sure the user provided an application name and version
        // also checks to make sure a server url, username, and password were
        // provided
        HubMavenReporterDescriptor descriptor = getDescriptor();
        HubServerInfo serverInfo = descriptor.getHubServerInfo();
        boolean isPluginConfigured = serverInfo != null
                && serverInfo.isPluginConfigured();
        boolean isPluginEnabled = StringUtils
                .isNotBlank(getMavenHubProjectName()) &&
                StringUtils.isNotBlank(getMavenHubVersionPhase()) &&
                StringUtils.isNotBlank(getMavenHubVersionDist()) &&
                StringUtils.isNotBlank(getMavenHubProjectVersion()) &&
                StringUtils.isNotBlank(getUserScopesToInclude());

        boolean scopesProvided = true;
        List<String> scopes = getScopesAsList(null);
        if (scopes == null || scopes.isEmpty()) {
            scopesProvided = false;
        }

        return isPluginConfigured && isPluginEnabled && scopesProvided;
    }

    /**
     * Determine if plugin is configured correctly
     *
     * @return true if Code Center server info is complete and if
     *         CodeCenterApplication Name and CodeCenterApplicationVersion are
     *         not empty
     */
    public Boolean validateConfiguration(IntLogger logger) {
        // Checks to make sure the user provided an application name and version
        // also checks to make sure a server url, username, and password were
        // provided
        HubMavenReporterDescriptor descriptor = getDescriptor();
        HubServerInfo serverInfo = descriptor.getHubServerInfo();

        boolean isPluginConfigured = true;
        if (serverInfo == null) {
            isPluginConfigured = false;
            logger.error("Could not find the Hub global configuration!");
        } else {
            if (StringUtils.isBlank(serverInfo.getServerUrl())) {
                isPluginConfigured = false;
                logger.error("The Hub server URL is not configured!");
            }
            if (StringUtils.isBlank(serverInfo.getCredentialsId())) {
                isPluginConfigured = false;
                logger.error("No Hub credentials configured!");
            } else {
                if (StringUtils.isBlank(serverInfo.getUsername())) {
                    isPluginConfigured = false;
                    logger.error("No Hub username configured!");
                }
                if (StringUtils.isBlank(serverInfo.getPassword())) {
                    isPluginConfigured = false;
                    logger.error("No Hub password configured!");
                }
            }
        }
        if (StringUtils.isBlank(getMavenHubProjectName())) {
            isPluginConfigured = false;
            logger.error("No Hub project name configured!");
        }
        // if (StringUtils.isBlank(getHubVersionPhase())) {
        // isPluginConfigured = false;
        // logger.error("");
        // }
        // if (StringUtils.isBlank(getHubVersionDist())) {
        // isPluginConfigured = false;
        // logger.error("");
        // }
        if (StringUtils.isBlank(getMavenHubProjectVersion())) {
            isPluginConfigured = false;
            logger.error("No Hub project version configured!");
        }
        if (hasScopes(logger, getUserScopesToInclude())) {
            List<String> scopes = getScopesAsList(logger);
            if (scopes == null || scopes.isEmpty()) {
                isPluginConfigured = false;
            }
        }

        return isPluginConfigured;
    }

    protected boolean hasScopes(IntLogger logger, String scopes) {
        if (StringUtils.isBlank(scopes)) {
            logger.error("No Maven scopes configured!");
            return false;
        }
        return true;
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

}
