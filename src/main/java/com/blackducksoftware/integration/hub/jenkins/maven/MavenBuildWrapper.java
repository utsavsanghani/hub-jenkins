package com.blackducksoftware.integration.hub.jenkins.maven;

import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.MavenUtil;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import hudson.tasks.Maven;
import hudson.tasks.Maven.ProjectWithMaven;
import hudson.tasks.Maven.MavenInstallation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.blackducksoftware.integration.build.BuildInfo;
import com.blackducksoftware.integration.build.extractor.Recorder_3_0_Loader;
import com.blackducksoftware.integration.build.extractor.Recorder_3_1_Loader;
import com.blackducksoftware.integration.hub.BuilderType;
import com.blackducksoftware.integration.hub.exception.BDCIScopeException;
import com.blackducksoftware.integration.hub.exception.BDMavenRetrieverException;
import com.blackducksoftware.integration.hub.jenkins.BDBuildWrapper;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCanonicalPath;
import com.blackducksoftware.integration.hub.jenkins.remote.GetPathSeparator;
import com.blackducksoftware.integration.hub.maven.BdMavenConfigurator;
import com.blackducksoftware.integration.hub.maven.Scope;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;
import com.google.gson.Gson;

/**
 * Sample {@link Builder}.
 * <p>
 * When the user configures the project and enables this builder, {@link DescriptorImpl#newInstance(StaplerRequest)} is
 * invoked and a new {@link MavenBuildWrapper} is created. The created instance is persisted to the project
 * configuration XML by using XStream, so this allows you to use instance fields (like {@link #name}) to remember the
 * configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be invoked.
 *
 * @author James Richard
 */
public class MavenBuildWrapper extends BDBuildWrapper {

    public static final String MAVEN_EXT_CLASS_PATH = "maven.ext.class.path";

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    /**
     *  This is the Maven Build Wrapper of our Plugin. Can't change otherwise we break our backwards compatability
     */
    public MavenBuildWrapper(String userScopesToInclude, boolean mavenSameAsPostBuildScan, String mavenHubProjectName, String mavenHubVersionPhase,
            String mavenHubVersionDist, String mavenHubProjectVersion) {
        super(userScopesToInclude, mavenSameAsPostBuildScan, mavenHubProjectName,
                mavenHubVersionPhase, mavenHubVersionDist, mavenHubProjectVersion);
    }

    // Need these getters for the UI
    public boolean isMavenSameAsPostBuildScan() {
        return isSameAsPostBuildScan();
    }

    public String getMavenHubProjectName() {
        return getHubWrapperProjectName();
    }

    public String getMavenHubVersionPhase() {
        return getHubWrapperVersionPhase();
    }

    public String getMavenHubVersionDist() {
        return getHubWrapperVersionDist();
    }

    public String getMavenHubProjectVersion() {
        return getHubWrapperProjectVersion();
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public MavenBuildWrapperDescriptor getDescriptor() {
        return (MavenBuildWrapperDescriptor) super.getDescriptor();
    }

    @Override
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
    public Environment setUp(final AbstractBuild build, Launcher launcher,
            final BuildListener listener) throws IOException,
            InterruptedException {
        // no failure to report yet
        HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
        buildLogger.setLogLevel(LogLevel.TRACE); // TODO make the log level configurable
        Maven mavenBuilder = null;
        if (build.getProject() instanceof FreeStyleProject) {
            // Project should always be a FreeStyleProject, thats why we have the isApplicable() method
            List<Builder> builders = ((FreeStyleProject) build.getProject()).getBuilders();

            if (builders == null || builders.isEmpty()) {
                // User didn't configure the job with a Builder
                buildLogger.error("No Builder found for this job.");
                buildLogger.error("Will not run the Hub Maven Build wrapper.");
                build.setResult(Result.UNSTABLE);
                return new Environment() {
                }; // Continue with the rest of the Build
            }

            for (Builder builder : builders) {
                if (builder instanceof Maven) {
                    mavenBuilder = (Maven) builder;
                }
            }
            if (mavenBuilder == null) {
                // User didn't configure the job with a Maven Builder
                buildLogger.error("This Wrapper should be run with a Maven Builder");
                buildLogger.error("Will not run the Hub Maven Build wrapper.");
                build.setResult(Result.UNSTABLE);
                return new Environment() {
                }; // Continue with the rest of the Build
            }
        } else {
            buildLogger.error("Cannot run the Hub Maven Build Wrapper for this type of Project.");
            build.setResult(Result.UNSTABLE);
            return new Environment() {
            }; // Continue with the rest of the Build
        }

        if (validateConfiguration(buildLogger)) {
            buildLogger.info("Build Recorder enabled");
            buildLogger.info("Hub Jenkins Plugin version : " + getDescriptor().getPluginVersion());

        } else {
            build.setResult(Result.UNSTABLE);
            return new Environment() {
            }; // Continue with the rest of the Build
        }

        setupAndAddMavenClasspathAction(build, buildLogger);

        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        try {
            if (MavenBuildWrapper.class.getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(MavenBuildWrapper.class.getClassLoader());
            }
            return new Environment() {

                @Override
                public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                    HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
                    try {
                        FilePath buildInfoFile = new FilePath(build.getWorkspace(),
                                BuildInfo.OUTPUT_FILE_NAME);
                        if (buildInfoFile.exists()) {
                            String buildInfoFilePath = null;
                            try {
                                buildInfoFilePath = build.getBuiltOn().getChannel().call(new GetCanonicalPath(new File(buildInfoFile.getRemote())));
                            } catch (IOException e) {
                                buildLogger.error("Problem getting the build info file on this node. Error : " + e.getMessage(), e);
                            }

                            return universalTearDown(build, buildLogger, buildInfoFilePath, getDescriptor(), BuilderType.MAVEN);
                        } else {
                            buildLogger.error("The " + BuildInfo.OUTPUT_FILE_NAME + " file does not exist at : " + buildInfoFile.getRemote());
                            build.setResult(Result.UNSTABLE);
                            return true;
                        }
                    } catch (Exception e) {
                        buildLogger.error(e.getMessage(), e);
                    } finally {

                        List<Action> old = new ArrayList<Action>();
                        List<Action> current = build.getActions();
                        for (Action curr : current) {
                            if (curr instanceof MavenClasspathAction) {
                                old.add(curr);
                            }
                        }
                        current.removeAll(old);
                    }
                    return true;
                }
            };
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
            }
        }
    }

    private void setupAndAddMavenClasspathAction(AbstractBuild build, HubJenkinsLogger buildLogger) {
        try {
            MavenClasspathAction mavenClasspathAction = new MavenClasspathAction();

            BdMavenConfigurator mavenConfig = new BdMavenConfigurator(buildLogger);

            String mavenVersion = getMavenVersion(build, mavenConfig, buildLogger);
            StringBuilder mavenExtClasspath = new StringBuilder();
            File dependencyRecorderJar = null;
            File buildInfo = null;
            File slf4jJar = null;
            File log4jJar = null;
            File slf4jJdkBindingJar = null;
            File gsonJar = null;

            boolean maven3orLater = MavenUtil.maven3orLater(mavenVersion);
            boolean supportedVersion = false;
            try {
                if (maven3orLater) {
                    if (mavenVersion.contains("3.0")) {
                        supportedVersion = true;
                        dependencyRecorderJar = mavenConfig.jarFile(Recorder_3_0_Loader.class);
                        slf4jJar = mavenConfig.jarFile(org.slf4j.helpers.FormattingTuple.class);
                        slf4jJdkBindingJar = mavenConfig.jarFile(org.slf4j.impl.JDK14LoggerAdapter.class);
                        mavenClasspathAction.setIsRecorder30(true);
                    } else if (mavenConfig.isMaven31OrLater(mavenVersion)) {
                        supportedVersion = true;
                        dependencyRecorderJar = mavenConfig.jarFile(Recorder_3_1_Loader.class);
                        mavenClasspathAction.setIsRecorder31(true);
                    }
                    if (supportedVersion) {
                        String buildId = build.getId();
                        mavenClasspathAction.setBuildId(buildId);

                        FilePath workspace = build.getWorkspace();
                        if (workspace != null) {
                            String workingDirectory = workspace.getRemote();
                            mavenClasspathAction.setWorkingDirectory(workingDirectory);

                            buildInfo = mavenConfig.jarFile(BuildInfo.class);
                            log4jJar = mavenConfig.jarFile(Logger.class);
                            gsonJar = mavenConfig.jarFile(Gson.class);
                        } else {
                            buildLogger.error("workspace: null");
                            build.setResult(Result.UNSTABLE);
                        }
                    } else {
                        buildLogger.error("Unsupported version of Maven. Maven version: " + mavenVersion);
                        build.setResult(Result.UNSTABLE);
                    }
                } else {
                    buildLogger.error("Unsupported version of Maven. Maven version: " + mavenVersion);
                    build.setResult(Result.UNSTABLE);
                }
            } catch (IllegalArgumentException e) {
                buildLogger.error("Failed to retrieve Maven information! "
                        + e.toString(), e);
                build.setResult(Result.UNSTABLE);
            } catch (IOException e) {
                buildLogger.error("Failed to retrieve Maven information! "
                        + e.toString(), e);
                build.setResult(Result.UNSTABLE);
            } catch (BDMavenRetrieverException e) {
                buildLogger.error("Failed to retrieve Maven information! "
                        + e.toString(), e);
                build.setResult(Result.UNSTABLE);
            }

            if (supportedVersion) {
                // transport the necessary jars to slave
                Node buildOn = build.getBuiltOn();
                if (buildOn == null) {
                    buildLogger.error("Node build on: null");
                } else {
                    FilePath remoteRootPath = new FilePath(buildOn.getRootPath(), "cache");
                    remoteRootPath = new FilePath(remoteRootPath, "hub-jenkins");
                    removeSnapshots(remoteRootPath);

                    String pathSeparator = null;
                    try {
                        VirtualChannel channel = buildOn.getChannel();
                        if (channel == null) {
                            buildLogger.error("Channel build on: null");
                        } else {
                            pathSeparator = channel.call(new GetPathSeparator());
                        }
                    } catch (IOException e) {
                        buildLogger.error(e.toString(), e);
                    } catch (InterruptedException e) {
                        buildLogger.error(e.toString(), e);
                    }
                    if (StringUtils.isEmpty(pathSeparator)) {
                        pathSeparator = File.pathSeparator;
                    }
                    mavenClasspathAction.setSeparator(pathSeparator);

                    if (dependencyRecorderJar != null) {
                        appendClasspath(build, remoteRootPath, mavenExtClasspath,
                                dependencyRecorderJar.getAbsolutePath(), buildLogger, pathSeparator);
                        if (buildInfo != null) {
                            appendClasspath(build, remoteRootPath, mavenExtClasspath,
                                    buildInfo.getAbsolutePath(), buildLogger, pathSeparator);
                        }
                        if (gsonJar != null) {
                            appendClasspath(build, remoteRootPath, mavenExtClasspath,
                                    gsonJar.getAbsolutePath(), buildLogger, pathSeparator);
                        }
                        if (slf4jJar != null) {
                            appendClasspath(build, remoteRootPath, mavenExtClasspath,
                                    slf4jJar.getAbsolutePath(), buildLogger, pathSeparator);
                        }
                        if (slf4jJdkBindingJar != null) {
                            appendClasspath(build, remoteRootPath, mavenExtClasspath,
                                    slf4jJdkBindingJar.getAbsolutePath(), buildLogger, pathSeparator);
                        }
                        if (log4jJar != null) {
                            appendClasspath(build, remoteRootPath, mavenExtClasspath,
                                    log4jJar.getAbsolutePath(), buildLogger, pathSeparator);
                        }
                        mavenClasspathAction.setMavenClasspathExtension(mavenExtClasspath.toString());
                        buildLogger.debug("Hub Build Wrapper 'maven.ext.class.path' = "
                                + mavenExtClasspath.toString());
                        build.addAction(mavenClasspathAction);
                        return;
                    } else {
                        buildLogger.error("Dependency recorder Jar not found. Maven version: " + mavenVersion);
                    }
                }
            }

        } catch (Exception e) {
            buildLogger.error(e);
        }
        return;
    }

    /**
     * Force update of snap shots
     */
    private void removeSnapshots(FilePath remoteDir) throws IOException, InterruptedException {
        if (remoteDir == null) {
            return;
        }
        List<FilePath> remoteFiles = remoteDir.list();
        if (remoteFiles == null || remoteFiles.size() == 0) {
            return;
        }
        for (FilePath file : remoteDir.list()) {
            // We need to do this because we released plugins which had snapshot dependencies and some of them had
            // timestamps rather than SNAPSHOT in the name
            // So now we force SNAPSHOT updates by removing them before we do anything else
            if (file.getName().contains("2014") || file.getName().contains("2015") || file.getName().contains("SNAPSHOT")) {
                file.delete();
            }
        }
    }

    @Override
    public void makeBuildVariables(AbstractBuild build,
            Map<String, String> variables) {

        if (isPluginEnabled()) {
            MavenClasspathAction classpathAction = build.getAction(MavenClasspathAction.class);
            if (classpathAction != null) {

                if (classpathAction.getIsRecorder30()) {
                    variables.put(Recorder_3_0_Loader.PROPERTY_BUILD_ID, classpathAction.getBuildId());
                    variables.put(Recorder_3_0_Loader.PROPERTY_WORKING_DIRECTORY, classpathAction.getWorkingDirectory());

                } else if (classpathAction.getIsRecorder31()) {
                    variables.put(Recorder_3_1_Loader.PROPERTY_BUILD_ID, classpathAction.getBuildId());
                    variables.put(Recorder_3_1_Loader.PROPERTY_WORKING_DIRECTORY, classpathAction.getWorkingDirectory());
                }

                String currentClasspath = variables.get(MAVEN_EXT_CLASS_PATH);
                String mavenExtClasspath = classpathAction.getMavenClasspathExtension();
                if (!StringUtils.isBlank(currentClasspath)) {
                    if (!currentClasspath.contains(mavenExtClasspath)) {
                        variables.put(MAVEN_EXT_CLASS_PATH, mavenExtClasspath + classpathAction.getSeparator() + currentClasspath);
                    }
                } else {
                    variables.put(MAVEN_EXT_CLASS_PATH, mavenExtClasspath);
                }

            }
        }

    }

    private void appendClasspath(AbstractBuild<?, ?> build, FilePath remoteRoot, StringBuilder classpath,
            String location, IntLogger buildLogger, String pathSeparator) {
        if (!StringUtils.isEmpty(location)) {
            File locationFile = new File(location);
            FilePath remoteFile = new FilePath(remoteRoot,
                    locationFile.getName());
            try {
                // logger.println("remoteFile: '" + remoteFile.getRemote() +
                // "' is remote = " + remoteFile.isRemote());
                if (remoteFile.isRemote()) {
                    if (!remoteFile.exists()) {
                        remoteFile.copyFrom(new FilePath(locationFile));
                    } else {
                        if (remoteFile.getName().contains("SNAPSHOT")) {
                            // refresh snapshot versions
                            remoteFile.delete();
                            remoteFile.copyFrom(new FilePath(locationFile));
                        }
                    }
                }
            } catch (IOException e) {
                buildLogger.error(e.toString(), e);
                return;
            } catch (InterruptedException e) {
                buildLogger.error(e.toString(), e);
                return;
            }

            if (classpath.length() > 0) {
                classpath.append(pathSeparator);
            }

            if (remoteFile.isRemote()) {
                classpath.append(remoteFile.getRemote());
            } else {
                classpath.append(location);
            }
            // } else {
            // logger.println("..appendClasspath(): file = '" + location + "'");
        }
    }

    private String getMavenVersion(AbstractBuild<?, ?> build, BdMavenConfigurator mavenConfig, HubJenkinsLogger buildLogger) {
        MavenInstallation mavenInstallation = null;
        AbstractProject<?, ?> project = build.getProject();
        String mavenVersion = "unknown";
        if (project instanceof ProjectWithMaven) {
            try {
                mavenInstallation = ((ProjectWithMaven) project).inferMavenInstallation();
                MavenInstallation mavenInstallationOnNode = null;
                if (mavenInstallation != null && Jenkins.getInstance() != null) {
                    mavenInstallationOnNode = mavenInstallation.forNode(Jenkins.getInstance(), buildLogger.getJenkinsListener());
                    try {
                        if (mavenInstallationOnNode != null) {
                            File mavenHomeDir = mavenInstallationOnNode.getHomeDir();
                            if (mavenHomeDir != null) {
                                mavenVersion = mavenConfig.getMavenVersion(mavenInstallationOnNode.getHomeDir().getCanonicalPath());
                            } else {
                                buildLogger.warn("mavenHomeDir = null");
                            }
                        } else {
                            buildLogger.warn("mavenInstallationonNode = null");
                        }
                    } catch (BDMavenRetrieverException e) {
                        buildLogger.error(e.toString(), e);
                    } catch (IOException e) {
                        buildLogger.error(e.toString(), e);
                    }
                }
            } catch (IOException e) {
                buildLogger.error(e.toString(), e);
            } catch (InterruptedException e) {
                buildLogger.error(e.toString(), e);
            }
        }

        return mavenVersion;
    }
}
