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
package com.blackducksoftware.integration.hub.jenkins.gradle;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.build.BuildInfo;
import com.blackducksoftware.integration.gradle.BDGradleUtil;
import com.blackducksoftware.integration.hub.BuilderType;
import com.blackducksoftware.integration.hub.jenkins.BDBuildWrapper;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;
import com.blackducksoftware.integration.hub.jenkins.remote.GetCanonicalPath;
import com.blackducksoftware.integration.hub.jenkins.remote.GetSeparator;
import com.blackducksoftware.integration.hub.logging.IntLogger;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.plugins.gradle.Gradle;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;

public class GradleBuildWrapper extends BDBuildWrapper {

	@DataBoundConstructor
	public GradleBuildWrapper(final String userScopesToInclude, final boolean gradleSameAsPostBuildScan,
			final String gradleHubProjectName, final String gradleHubVersionPhase, final String gradleHubVersionDist,
			final String gradleHubProjectVersion) {
		super(userScopesToInclude, gradleSameAsPostBuildScan, gradleHubProjectName, gradleHubVersionPhase,
				gradleHubVersionDist, gradleHubProjectVersion);
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
	public List<String> getScopesAsList(final IntLogger buildLogger) {
		final List<String> scopesToInclude = new ArrayList<String>();
		String[] tokens = null;
		if (!StringUtils.isEmpty(userScopesToInclude)) {
			if (userScopesToInclude.contains(",")) {
				tokens = userScopesToInclude.split(",");
			} else {
				tokens = new String[1];
				tokens[0] = userScopesToInclude;
			}
			for (final String scope : tokens) {
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
	protected boolean hasScopes(final IntLogger logger, final String scopes) {
		if (StringUtils.isBlank(scopes)) {
			logger.error("No Gradle configurations configured!");
			return false;
		}
		return true;
	}

	@Override
	public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener)
			throws IOException, InterruptedException {
		// no failure to report yet
		final HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);

		final EnvVars variables = build.getEnvironment(listener);
		buildLogger.setLogLevel(variables);

		Gradle gradleBuilder = null;
		if (build.getProject() instanceof FreeStyleProject) {
			// Project should always be a FreeStyleProject, thats why we have
			// the isApplicable() method
			final List<Builder> builders = ((FreeStyleProject) build.getProject()).getBuilders();

			if (builders == null || builders.isEmpty()) {
				// User didn't configure the job with a Builder
				buildLogger.error("No Builder found for this job.");
				buildLogger.error("Will not run the Hub Gradle Build wrapper.");
				build.setResult(Result.UNSTABLE);
				return new Environment() {
				}; // Continue with the rest of the Build
			}

			for (final Builder builder : builders) {
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

			final BDGradleInitScriptWriter writer = new BDGradleInitScriptWriter(build, buildLogger);
			final FilePath workspace = build.getWorkspace();
			FilePath initScript;
			String initScriptPath;
			try {
				if (workspace == null) {
					buildLogger.error("Workspace: null");
				} else {
					initScript = workspace.createTextTempFile("init-blackduck", "gradle", writer.generateInitScript(),
							false);
					if (initScript != null) {
						initScriptPath = initScript.getRemote();
						initScriptPath = initScriptPath.replace('\\', '/');

						String newSwitches = originalSwitches.get();
						String newTasks = originalTasks.get();

						if (!originalSwitches.get().contains("--init-script ")
								&& !originalSwitches.get().contains("init-blackduck")) {
							newSwitches = newSwitches + " --init-script " + initScriptPath;
						}
						if (!originalSwitches.get().contains(" -D" + BDGradleUtil.BUILD_ID_PROPERTY)) {
							newSwitches = newSwitches + " -D" + BDGradleUtil.BUILD_ID_PROPERTY + "=" + build.getId();
						}
						if (!originalSwitches.get().contains(" -D" + BDGradleUtil.INCLUDED_CONFIGURATIONS_PROPERTY)) {
							String configurations = getUserScopesToInclude();
							configurations = configurations.replaceAll(" ", "");

							newSwitches = newSwitches + " -D" + BDGradleUtil.INCLUDED_CONFIGURATIONS_PROPERTY + "="
									+ configurations;
						}

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
			} catch (final Exception e) {
				listener.getLogger().println("Error occurred while writing Gradle Init Script: " + e.getMessage());
				build.setResult(Result.FAILURE);
			}

		}

		final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		boolean changed = false;
		try {
			if (GradleBuildWrapper.class.getClassLoader() != originalClassLoader) {
				changed = true;
				Thread.currentThread().setContextClassLoader(GradleBuildWrapper.class.getClassLoader());
			}
			return new Environment() {
				@Override
				public boolean tearDown(final AbstractBuild build, final BuildListener listener)
						throws IOException, InterruptedException {
					final HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);

					final EnvVars variables = build.getEnvironment(listener);
					buildLogger.setLogLevel(variables);

					Gradle gradleBuilder = null;
					try {
						if (build.getProject() instanceof FreeStyleProject) {
							// Project should always be a FreeStyleProject,
							// thats why we have the isApplicable() method
							final List<Builder> builders = ((FreeStyleProject) build.getProject()).getBuilders();

							for (final Builder builder : builders) {
								if (builder instanceof Gradle) {
									gradleBuilder = (Gradle) builder;
								}
							}
						}
						if (gradleBuilder != null) {
							String rootBuildScriptDir = gradleBuilder.getRootBuildScriptDir();

							if (StringUtils.startsWithIgnoreCase(rootBuildScriptDir, "${WORKSPACE}")
									|| StringUtils.startsWithIgnoreCase(rootBuildScriptDir, "$WORKSPACE")) {
								rootBuildScriptDir = BuildHelper.handleVariableReplacement(variables,
										rootBuildScriptDir);
							}

							String fileSeparator = null;
							try {
								final VirtualChannel channel = build.getBuiltOn().getChannel();
								if (channel == null) {
									buildLogger.error("Channel build on: null");
								} else {
									fileSeparator = channel.call(new GetSeparator());
								}
							} catch (final IOException e) {
								buildLogger.error(e.toString(), e);
							} catch (final InterruptedException e) {
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

							String workingDirectory = "";
							try {
								workingDirectory = build.getBuiltOn().getChannel()
										.call(new GetCanonicalPath(workspaceFile));
							} catch (final IOException e) {
								buildLogger.error(
										"Problem getting the working directory on this node. Error : " + e.getMessage(),
										e);
							}

							if (!StringUtils.startsWithIgnoreCase(rootBuildScriptDir, workingDirectory)) {
								if (workingDirectory.endsWith(fileSeparator)) {
									rootBuildScriptDir = workingDirectory + rootBuildScriptDir;
								} else {
									rootBuildScriptDir = workingDirectory + fileSeparator + rootBuildScriptDir;
								}
							}

							FilePath buildInfo = null;
							final Node buildOn = build.getBuiltOn();
							if (buildOn == null) {
								buildLogger.error("Node build on: null");
							} else {
								final VirtualChannel channel = buildOn.getChannel();
								if (channel == null) {
									buildLogger.error("Channel build on: null");
								} else {
									// buildInfoFile = new FilePath(channel,
									// workspacePath);
									buildInfo = new FilePath(channel, rootBuildScriptDir);
									buildInfo = new FilePath(buildInfo, "build");
									buildInfo = new FilePath(buildInfo, "BlackDuck");
									buildInfo = new FilePath(buildInfo, BuildInfo.OUTPUT_FILE_NAME);

								}
							}

							if (buildInfo != null) {

								if (buildInfo.exists()) {
									return universalTearDown(build, buildLogger, buildInfo, getDescriptor(),
											BuilderType.GRADLE);
								} else {
									buildLogger.error("The " + BuildInfo.OUTPUT_FILE_NAME + " file does not exist at : "
											+ buildInfo.getRemote() + ", on machine : "
											+ (buildOn == null ? "null" : buildOn.getDisplayName()));
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
					} catch (final BDJenkinsHubPluginException e) {
						buildLogger.error(e.getMessage(), e);
						build.setResult(Result.UNSTABLE);
						return true;
					} catch (final Exception e) {
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
								} catch (final Exception e) {
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
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}
	}

	private void setField(final Gradle builder, final String fieldName, final String value)
			throws IllegalArgumentException, IllegalAccessException, SecurityException, NoSuchFieldException {
		final Field targetsField = builder.getClass().getDeclaredField(fieldName);
		targetsField.setAccessible(true);
		targetsField.set(builder, value);
	}

}
