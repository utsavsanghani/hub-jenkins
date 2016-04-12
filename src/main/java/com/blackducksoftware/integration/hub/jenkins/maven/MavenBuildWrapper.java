/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.jenkins.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.build.BuildInfo;
import com.blackducksoftware.integration.build.extractor.Recorder_3_0_Loader;
import com.blackducksoftware.integration.build.extractor.Recorder_3_1_Loader;
import com.blackducksoftware.integration.hub.BuilderType;
import com.blackducksoftware.integration.hub.exception.BDCIScopeException;
import com.blackducksoftware.integration.hub.exception.BDMavenRetrieverException;
import com.blackducksoftware.integration.hub.jenkins.BDBuildWrapper;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.action.MavenClasspathAction;
import com.blackducksoftware.integration.hub.jenkins.remote.GetPathSeparator;
import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.blackducksoftware.integration.hub.logging.LogLevel;
import com.blackducksoftware.integration.hub.maven.BdMavenConfigurator;
import com.blackducksoftware.integration.hub.maven.Scope;
import com.google.gson.Gson;

import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.MavenUtil;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import hudson.tasks.Maven;
import hudson.tasks.Maven.MavenInstallation;
import hudson.tasks.Maven.ProjectWithMaven;
import jenkins.model.Jenkins;

public class MavenBuildWrapper extends BDBuildWrapper {

	public static final String MAVEN_EXT_CLASS_PATH = "maven.ext.class.path";

	@DataBoundConstructor
	public MavenBuildWrapper(final String userScopesToInclude, final boolean mavenSameAsPostBuildScan, final String mavenHubProjectName, final String mavenHubVersionPhase,
			final String mavenHubVersionDist, final String mavenHubProjectVersion) {
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

	@Override
	public MavenBuildWrapperDescriptor getDescriptor() {
		return (MavenBuildWrapperDescriptor) super.getDescriptor();
	}

	@Override
	public List<String> getScopesAsList(final IntLogger buildLogger) {
		List<String> scopesToInclude = new ArrayList<String>();
		try {
			scopesToInclude = Scope.getScopeListFromString(userScopesToInclude);
		} catch (final BDCIScopeException e) {
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
	public Environment setUp(final AbstractBuild build, final Launcher launcher,
			final BuildListener listener) throws IOException,
	InterruptedException {
		final HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
		buildLogger.setLogLevel(LogLevel.TRACE);
		Maven mavenBuilder = null;
		if (build.getProject() instanceof FreeStyleProject) {
			// Project should always be a FreeStyleProject, thats why we have the isApplicable() method
			final List<Builder> builders = ((FreeStyleProject) build.getProject()).getBuilders();

			if (builders == null || builders.isEmpty()) {
				// User didn't configure the job with a Builder
				buildLogger.error("No Builder found for this job.");
				buildLogger.error("Will not run the Hub Maven Build wrapper.");
				build.setResult(Result.UNSTABLE);
				return new Environment() {
				}; // Continue with the rest of the Build
			}

			for (final Builder builder : builders) {
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

		final ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		boolean changed = false;
		try {
			if (MavenBuildWrapper.class.getClassLoader() != originalClassLoader) {
				changed = true;
				Thread.currentThread().setContextClassLoader(MavenBuildWrapper.class.getClassLoader());
			}
			return new Environment() {

				@Override
				public boolean tearDown(final AbstractBuild build, final BuildListener listener) throws IOException, InterruptedException {
					final HubJenkinsLogger buildLogger = new HubJenkinsLogger(listener);
					try {
						final FilePath buildInfoFile = new FilePath(build.getWorkspace(),
								BuildInfo.OUTPUT_FILE_NAME);
						if (buildInfoFile.exists()) {

							return universalTearDown(build, buildLogger, buildInfoFile, getDescriptor(), BuilderType.MAVEN);
						} else {
							buildLogger.error("The " + BuildInfo.OUTPUT_FILE_NAME + " file does not exist at : " + buildInfoFile.getRemote());
							build.setResult(Result.UNSTABLE);
							return true;
						}
					} catch (final Exception e) {
						buildLogger.error(e.getMessage(), e);
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

	private void setupAndAddMavenClasspathAction(final AbstractBuild<?, ?> build, final HubJenkinsLogger buildLogger) {
		try {
			final MavenClasspathAction mavenClasspathAction = new MavenClasspathAction();

			final BdMavenConfigurator mavenConfig = new BdMavenConfigurator(buildLogger);

			final String mavenVersion = getMavenVersion(build, mavenConfig, buildLogger);
			final StringBuilder mavenExtClasspath = new StringBuilder();
			File dependencyRecorderJar = null;
			File buildInfo = null;
			File slf4jJar = null;
			File log4jJar = null;
			File slf4jJdkBindingJar = null;
			File gsonJar = null;

			final boolean maven3orLater = MavenUtil.maven3orLater(mavenVersion);
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
						final String buildId = build.getId();
						mavenClasspathAction.setBuildId(buildId);

						final FilePath workspace = build.getWorkspace();
						if (workspace != null) {
							final String workingDirectory = workspace.getRemote();
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
			} catch (final IllegalArgumentException e) {
				buildLogger.error("Failed to retrieve Maven information! "
						+ e.toString(), e);
				build.setResult(Result.UNSTABLE);
			} catch (final IOException e) {
				buildLogger.error("Failed to retrieve Maven information! "
						+ e.toString(), e);
				build.setResult(Result.UNSTABLE);
			} catch (final BDMavenRetrieverException e) {
				buildLogger.error("Failed to retrieve Maven information! "
						+ e.toString(), e);
				build.setResult(Result.UNSTABLE);
			}

			if (supportedVersion) {
				// transport the necessary jars to slave
				final Node buildOn = build.getBuiltOn();
				if (buildOn == null) {
					buildLogger.error("Node build on: null");
				} else {
					FilePath remoteRootPath = new FilePath(buildOn.getRootPath(), "cache");
					remoteRootPath = new FilePath(remoteRootPath, "hub-jenkins");
					removeSnapshots(remoteRootPath);

					String pathSeparator = null;
					try {
						final VirtualChannel channel = buildOn.getChannel();
						if (channel == null) {
							buildLogger.error("Channel build on: null");
						} else {
							pathSeparator = channel.call(new GetPathSeparator());
						}
					} catch (final IOException e) {
						buildLogger.error(e.toString(), e);
					} catch (final InterruptedException e) {
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

		} catch (final Exception e) {
			buildLogger.error(e);
		}
		return;
	}

	/**
	 * Force update of snap shots
	 */
	private void removeSnapshots(final FilePath remoteDir) throws IOException, InterruptedException {
		if (remoteDir == null) {
			return;
		}
		final List<FilePath> remoteFiles = remoteDir.list();
		if (remoteFiles == null || remoteFiles.size() == 0) {
			return;
		}
		for (final FilePath file : remoteDir.list()) {
			// We force SNAPSHOT updates by removing them before we do anything
			// else
			if (file.getName().contains("2014") || file.getName().contains("2015") || file.getName().contains("SNAPSHOT")) {
				file.delete();
			}
		}
	}

	@Override
	public void makeBuildVariables(final AbstractBuild build,
			final Map<String, String> variables) {

		if (isPluginEnabled()) {
			final MavenClasspathAction classpathAction = build.getAction(MavenClasspathAction.class);
			if (classpathAction != null) {

				if (classpathAction.getIsRecorder30()) {
					variables.put(Recorder_3_0_Loader.PROPERTY_BUILD_ID, classpathAction.getBuildId());
					variables.put(Recorder_3_0_Loader.PROPERTY_WORKING_DIRECTORY, classpathAction.getWorkingDirectory());

				} else if (classpathAction.getIsRecorder31()) {
					variables.put(Recorder_3_1_Loader.PROPERTY_BUILD_ID, classpathAction.getBuildId());
					variables.put(Recorder_3_1_Loader.PROPERTY_WORKING_DIRECTORY, classpathAction.getWorkingDirectory());
				}

				final String currentClasspath = variables.get(MAVEN_EXT_CLASS_PATH);
				final String mavenExtClasspath = classpathAction.getMavenClasspathExtension();
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

	private void appendClasspath(final AbstractBuild<?, ?> build, final FilePath remoteRoot, final StringBuilder classpath,
			final String location, final IntLogger buildLogger, final String pathSeparator) {
		if (!StringUtils.isEmpty(location)) {
			final File locationFile = new File(location);
			final FilePath remoteFile = new FilePath(remoteRoot,
					locationFile.getName());
			try {
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
			} catch (final IOException e) {
				buildLogger.error(e.toString(), e);
				return;
			} catch (final InterruptedException e) {
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
		}
	}

	private String getMavenVersion(final AbstractBuild<?, ?> build, final BdMavenConfigurator mavenConfig, final HubJenkinsLogger buildLogger) {
		MavenInstallation mavenInstallation = null;
		final AbstractProject<?, ?> project = build.getProject();
		String mavenVersion = "unknown";
		if (project instanceof ProjectWithMaven) {
			try {
				mavenInstallation = ((ProjectWithMaven) project).inferMavenInstallation();
				MavenInstallation mavenInstallationOnNode = null;
				if (mavenInstallation != null && Jenkins.getInstance() != null) {
					mavenInstallationOnNode = mavenInstallation.forNode(Jenkins.getInstance(), buildLogger.getJenkinsListener());
					try {
						if (mavenInstallationOnNode != null) {
							final File mavenHomeDir = mavenInstallationOnNode.getHomeDir();
							if (mavenHomeDir != null) {
								mavenVersion = mavenConfig.getMavenVersion(mavenInstallationOnNode.getHomeDir().getCanonicalPath());
							} else {
								buildLogger.warn("mavenHomeDir = null");
							}
						} else {
							buildLogger.warn("mavenInstallationonNode = null");
						}
					} catch (final BDMavenRetrieverException e) {
						buildLogger.error(e.toString(), e);
					} catch (final IOException e) {
						buildLogger.error(e.toString(), e);
					}
				}
			} catch (final IOException e) {
				buildLogger.error(e.toString(), e);
			} catch (final InterruptedException e) {
				buildLogger.error(e.toString(), e);
			}
		}

		return mavenVersion;
	}
}
