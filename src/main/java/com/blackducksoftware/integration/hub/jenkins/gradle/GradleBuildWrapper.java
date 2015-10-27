package com.blackducksoftware.integration.hub.jenkins.gradle;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.plugins.gradle.Gradle;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.blackducksoftware.integration.build.BuildInfo;
import com.blackducksoftware.integration.gradle.BDCustomTask;
import com.blackducksoftware.integration.hub.BuilderType;
import com.blackducksoftware.integration.hub.jenkins.BDBuildWrapper;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCanonicalPath;
import com.blackducksoftware.integration.hub.jenkins.remote.GetSeparator;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;

/**
 * Sample {@link Builder}.
 * <p>
 * When the user configures the project and enables this builder, {@link DescriptorImpl#newInstance(StaplerRequest)} is
 * invoked and a new {@link GradleBuildWrapper} is created. The created instance is persisted to the project
 * configuration XML by using XStream, so this allows you to use instance fields (like {@link #name}) to remember the
 * configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be invoked.
 *
 * @author James Richard
 */
public class GradleBuildWrapper extends BDBuildWrapper {

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public GradleBuildWrapper(String userScopesToInclude, boolean gradleSameAsPostBuildScan, String gradleHubProjectName, String gradleHubVersionPhase,
            String gradleHubVersionDist, String gradleHubProjectVersion) {

        super(userScopesToInclude, gradleSameAsPostBuildScan, gradleHubProjectName,
                gradleHubVersionPhase, gradleHubVersionDist, gradleHubProjectVersion);
    }

    // Need these getters for the UI
    public boolean isGradleSameAsPostBuildScan() {
        return isSameAsPostBuildScan();
    }

    public String getGradleHubProjectName() {
        return getHubWrapperProjectName();
    }

    public String getGradleHubVersionPhase() {
        return getHubWrapperVersionPhase();
    }

    public String getGradleHubVersionDist() {
        return getHubWrapperVersionDist();
    }

    public String getGradleHubProjectVersion() {
        return getHubWrapperProjectVersion();
    }

    @Override
    public GradleBuildWrapperDescriptor getDescriptor() {
        return (GradleBuildWrapperDescriptor) super.getDescriptor();
    }

    @Override
    public List<String> getScopesAsList(IntLogger buildLogger) {
        List<String> scopesToInclude = new ArrayList<String>();
        String[] tokens = null;
        if (!StringUtils.isEmpty(userScopesToInclude)) {
            if (userScopesToInclude.contains(",")) {
                tokens = userScopesToInclude.split(",");
            } else {
                tokens = new String[1];
                tokens[0] = userScopesToInclude;
            }
            for (String scope : tokens) {
                scopesToInclude.add(scope.trim().toUpperCase());
            }
        } else {
            if (buildLogger != null) {
                buildLogger.error("Cannot get Configurations from an empty String");
            }
            return null;
        }

        return scopesToInclude;

    }

    @Override
    protected boolean hasScopes(IntLogger logger, String scopes) {
        if (StringUtils.isBlank(scopes)) {
            logger.error("No Gradle configurations configured!");
            return false;
        }
        return true;
    }

    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher,
            final BuildListener listener) throws IOException,
            InterruptedException {
        // no failure to report yet
        HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
        buildLogger.setLogLevel(LogLevel.TRACE); // TODO make the log level configurable
        Gradle gradleBuilder = null;
        if (build.getProject() instanceof FreeStyleProject) {
            // Project should always be a FreeStyleProject, thats why we have the isApplicable() method
            List<Builder> builders = ((FreeStyleProject) build.getProject()).getBuilders();

            if (builders == null || builders.isEmpty()) {
                // User didn't configure the job with a Builder
                buildLogger.error("No Builder found for this job.");
                buildLogger.error("Will not run the Hub Gradle Build wrapper.");
                build.setResult(Result.UNSTABLE);
                return new Environment() {
                }; // Continue with the rest of the Build
            }

            for (Builder builder : builders) {
                if (builder instanceof Gradle) {
                    gradleBuilder = (Gradle) builder;
                }
            }
            if (gradleBuilder == null) {
                // User didn't configure the job with a Gradle Builder
                buildLogger.error("This Wrapper should be run with a Gradle Builder");
                buildLogger.error("Will not run the Hub Gradle Build wrapper.");
                build.setResult(Result.UNSTABLE);
                return new Environment() {
                }; // Continue with the rest of the Build
            }
        } else {
            buildLogger.error("Cannot run the Hub Gradle Build Wrapper for this type of Project.");
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

        final ThreadLocal<String> originalSwitches = new ThreadLocal<String>();
        final ThreadLocal<String> originalTasks = new ThreadLocal<String>();

        if (gradleBuilder != null) {

            originalSwitches.set(gradleBuilder.getSwitches() + "");
            originalTasks.set(gradleBuilder.getTasks() + "");

            // @Override
            // public void buildEnvVars(Map<String, String> env) {
            BDGradleInitScriptWriter writer = new BDGradleInitScriptWriter(build, buildLogger);
            FilePath workspace = build.getWorkspace();
            FilePath initScript;
            String initScriptPath;
            try {
                if (workspace == null) {
                    buildLogger.error("Workspace: null");
                } else {
                    initScript =
                            workspace.createTextTempFile("init-blackduck", "gradle", writer.generateInitScript(),
                                    false);
                    if (initScript != null) {
                        initScriptPath = initScript.getRemote();
                        initScriptPath = initScriptPath.replace('\\', '/');

                        String newSwitches = originalSwitches.get();
                        String newTasks = originalTasks.get();

                        if (!originalSwitches.get().contains("--init-script ") && !originalSwitches.get().contains("init-blackduck")) {
                            newSwitches = newSwitches + " --init-script " + initScriptPath;
                        }
                        if (!originalSwitches.get().contains(" -D" + BDCustomTask.BUILD_ID_PROPERTY)) {
                            newSwitches = newSwitches + " -D" + BDCustomTask.BUILD_ID_PROPERTY + "=" + build.getId();
                        }
                        // if (!originalSwitches.get().contains(" -D" + BDGradlePlugin.DEPENDENCY_REPORT_OUTPUT)) {
                        // FilePath dependencyTreeFile = new FilePath(workspace, "dependencyTree.txt");
                        // newSwitches = newSwitches + " -D" + BDGradlePlugin.DEPENDENCY_REPORT_OUTPUT + "='" +
                        // dependencyTreeFile.getRemote() + "'";
                        // }

                        if (!originalTasks.get().contains("bdCustomTask")) {
                            newTasks = newTasks + " bdCustomTask";
                        }

                        if (!originalTasks.get().contains("bdDependencyTree")) {
                            newTasks = newTasks + " bdDependencyTree";
                        }
                        setField(gradleBuilder, "switches", newSwitches);
                        setField(gradleBuilder, "tasks", newTasks);
                    }
                }
            } catch (Exception e) {
                listener.getLogger().println("Error occurred while writing Gradle Init Script: " + e.getMessage());
                build.setResult(Result.FAILURE);
            }

        }

        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        try {
            if (GradleBuildWrapper.class.getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(GradleBuildWrapper.class.getClassLoader());
            }
            return new Environment() {
                @Override
                public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                    HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
                    Gradle gradleBuilder = null;
                    try {
                        if (build.getProject() instanceof FreeStyleProject) {
                            // Project should always be a FreeStyleProject, thats why we have the isApplicable() method
                            List<Builder> builders = ((FreeStyleProject) build.getProject()).getBuilders();

                            for (Builder builder : builders) {
                                if (builder instanceof Gradle) {
                                    gradleBuilder = (Gradle) builder;
                                }
                            }
                        }
                        if (gradleBuilder != null) {
                            String rootBuildScriptDir = gradleBuilder.getRootBuildScriptDir();

                            if (StringUtils.startsWithIgnoreCase(rootBuildScriptDir, "${WORKSPACE}")
                                    || StringUtils.startsWithIgnoreCase(rootBuildScriptDir, "$WORKSPACE")) {
                                EnvVars variables = build.getEnvironment(listener);
                                rootBuildScriptDir = handleVariableReplacement(variables, rootBuildScriptDir);
                            }

                            String fileSeparator = null;
                            try {
                                VirtualChannel channel = build.getBuiltOn().getChannel();
                                if (channel == null) {
                                    buildLogger.error("Channel build on: null");
                                } else {
                                    fileSeparator = channel.call(new GetSeparator());
                                }
                            } catch (IOException e) {
                                buildLogger.error(e.toString(), e);
                            } catch (InterruptedException e) {
                                buildLogger.error(e.toString(), e);
                            }
                            if (StringUtils.isEmpty(fileSeparator)) {
                                fileSeparator = File.separator;
                            }

                            File workspaceFile = null;
                            if (build.getWorkspace() == null) {
                                // might be using custom workspace
                                workspaceFile = new File(build.getProject().getCustomWorkspace());
                            } else {
                                workspaceFile = new File(build.getWorkspace().getRemote());
                            }

                            // FilePath workspace = build.getWorkspace();
                            // if (workspaceFile == null) {
                            // buildLogger.error("Workspace: null");
                            // build.setResult(Result.UNSTABLE);
                            // return true;
                            // } else {

                            String workingDirectory = "";
                            try {
                                workingDirectory = build.getBuiltOn().getChannel().call(new GetCanonicalPath(workspaceFile));
                            } catch (IOException e) {
                                buildLogger.error("Problem getting the working directory on this node. Error : " + e.getMessage(), e);
                            }

                            if (!StringUtils.startsWithIgnoreCase(rootBuildScriptDir, workingDirectory)) {
                                if (workingDirectory.endsWith(fileSeparator)) {
                                    rootBuildScriptDir = workingDirectory + rootBuildScriptDir;
                                } else {
                                    rootBuildScriptDir = workingDirectory + fileSeparator + rootBuildScriptDir;
                                }
                            }

                            String buildInfoFilePath = null;
                            FilePath buildInfo = null;
                            Node buildOn = build.getBuiltOn();
                            if (buildOn == null) {
                                buildLogger.error("Node build on: null");
                            } else {
                                VirtualChannel channel = buildOn.getChannel();
                                if (channel == null) {
                                    buildLogger.error("Channel build on: null");
                                } else {
                                    // buildInfoFile = new FilePath(channel, workspacePath);
                                    buildInfo = new FilePath(channel, rootBuildScriptDir);
                                    buildInfo = new FilePath(buildInfo, "build");
                                    buildInfo = new FilePath(buildInfo, "BlackDuck");
                                    buildInfo = new FilePath(buildInfo, BuildInfo.OUTPUT_FILE_NAME);

                                    try {
                                        buildInfoFilePath = channel.call(new GetCanonicalPath(new File(buildInfo.getRemote())));
                                    } catch (IOException e) {
                                        buildLogger.error("Problem getting the build info file on this node. Error : " + e.getMessage(), e);
                                    }

                                }
                            }

                            if (buildInfoFilePath != null) {

                                if (buildInfo.exists()) {
                                    // FIXME looks like the wrong file is being read or it is being read incorrectly
                                    return universalTearDown(build, buildLogger, buildInfoFilePath, getDescriptor(), BuilderType.GRADLE);
                                } else {
                                    buildLogger.error("The " + BuildInfo.OUTPUT_FILE_NAME + " file does not exist at : " + buildInfoFilePath
                                            + ", on machine : " + (buildOn == null ? "null" : buildOn.getDisplayName()));
                                    build.setResult(Result.UNSTABLE);
                                    return true;
                                }
                            }
                            // }
                        } else {
                            buildLogger.error("[WARNING] no gradle build step found");
                            build.setResult(Result.UNSTABLE);
                            return true;
                        }
                    } catch (BDJenkinsHubPluginException e) {
                        buildLogger.error(e.getMessage(), e);
                        build.setResult(Result.UNSTABLE);
                        return true;
                    } catch (Exception e) {
                        buildLogger.error(e.getMessage(), e);
                        build.setResult(Result.UNSTABLE);
                        return true;
                    } finally {
                        if (gradleBuilder != null) {
                            synchronized (this) {
                                try {
                                    // restore the original configuration
                                    setField(gradleBuilder, "switches", originalSwitches.get());
                                    setField(gradleBuilder, "tasks", originalTasks.get());
                                } catch (Exception e) {
                                    buildLogger.error(e.getMessage(), e);
                                    build.setResult(Result.UNSTABLE);
                                    return true;
                                }
                            }
                        }
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

    private void setField(Gradle builder, String fieldName, String value) throws IllegalArgumentException, IllegalAccessException, SecurityException,
            NoSuchFieldException {
        Field targetsField = builder.getClass().getDeclaredField(fieldName);
        targetsField.setAccessible(true);
        targetsField.set(builder, value);
    }

}
