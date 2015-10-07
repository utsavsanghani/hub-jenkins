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
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;

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
    public GradleBuildWrapper(String userScopesToInclude, boolean sameAsPostBuildScan, String hubWrapperProjectName, String hubWrapperVersionPhase,
            String hubWrapperVersionDist, String hubWrapperProjectVersion) {

        super(userScopesToInclude, sameAsPostBuildScan, hubWrapperProjectName,
                hubWrapperVersionPhase, hubWrapperVersionDist, hubWrapperProjectVersion);
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
    protected void checkScopesValid(List<String> errorMessage, List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            errorMessage.add("you must provide at least one valid scope to include");
        }
    }

    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher,
            final BuildListener listener) throws IOException,
            InterruptedException {
        // no failure to report yet
        HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
        Gradle gradleBuilder = null;
        if (build.getProject() instanceof FreeStyleProject) {
            // Project should always be a FreeStyleProject, thats why we have the isApplicable() method
            List<Builder> builders = ((FreeStyleProject) build.getProject()).getBuilders();

            if (builders == null || builders.isEmpty()) {
                // User didn't configure the job with a Builder
                buildLogger.error("No Builder found for this job.");
                buildLogger.error("Will not run the Code Center plugin.");
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
                buildLogger.error("This Wrapper should only be run with a Gradle Builder");
                buildLogger.error("Will not run the Code Center plugin.");
                build.setResult(Result.UNSTABLE);
                return new Environment() {
                }; // Continue with the rest of the Build
            }
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
                        if (!originalTasks.get().contains("bdCustomTask")) {
                            newTasks = newTasks + " bdCustomTask";
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

                            FilePath workspace = build.getWorkspace();
                            if (workspace == null) {
                                buildLogger.error("Workspace: null");
                                build.setResult(Result.UNSTABLE);
                                return true;
                            } else {
                                if (!StringUtils.startsWithIgnoreCase(rootBuildScriptDir, workspace.getRemote())) {
                                    if (workspace.getRemote().endsWith(File.separator)) {
                                        rootBuildScriptDir = workspace + rootBuildScriptDir;
                                    } else {
                                        rootBuildScriptDir = workspace + File.separator + rootBuildScriptDir;
                                    }
                                }

                                FilePath buildInfoFile = null;
                                Node buildOn = build.getBuiltOn();
                                if (buildOn == null) {
                                    buildLogger.error("Node build on: null");
                                } else {
                                    VirtualChannel channel = buildOn.getChannel();
                                    if (channel == null) {
                                        buildLogger.error("Channel build on: null");
                                    } else {
                                        buildInfoFile = new FilePath(channel, rootBuildScriptDir + File.separator + "build"
                                                + File.separator + "BlackDuck" + File.separator + BuildInfo.OUTPUT_FILE_NAME);
                                    }
                                }
                                if (buildInfoFile != null) {
                                    if (buildInfoFile.exists()) {
                                        return universalTearDown(build, buildLogger, buildInfoFile, getDescriptor(), BuilderType.GRADLE);
                                    } else {
                                        buildLogger.error("The " + BuildInfo.OUTPUT_FILE_NAME + " file does not exist at : " + buildInfoFile.getRemote());
                                    }
                                } else {
                                    buildLogger.error("The " + BuildInfo.OUTPUT_FILE_NAME + " file does not exist on: "
                                            + (buildOn == null ? "null" : buildOn.getDisplayName()));
                                    build.setResult(Result.UNSTABLE);
                                    return true;
                                }
                                return true;
                            }
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
