package com.blackducksoftware.integration.hub.jenkins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.build.BuildInfo;
import com.blackducksoftware.integration.hub.BuilderType;
import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.VersionDoesNotExistException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;
import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.blackducksoftware.integration.hub.project.api.ProjectItem;
import com.blackducksoftware.integration.hub.version.api.ReleaseItem;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;

public abstract class BDBuildWrapper extends BuildWrapper {

	protected final String userScopesToInclude;

	protected final boolean sameAsPostBuildScan;

	private final String hubWrapperProjectName;

	private final String hubWrapperVersionPhase;

	private final String hubWrapperVersionDist;

	private final String hubWrapperProjectVersion;

	public BDBuildWrapper(final String userScopesToInclude, final boolean sameAsPostBuildScan, final String hubWrapperProjectName, final String hubWrapperVersionPhase,
			final String hubWrapperVersionDist, final String hubWrapperProjectVersion) {
		if (StringUtils.isNotBlank(userScopesToInclude)) {
			this.userScopesToInclude = userScopesToInclude.trim();
		} else {
			this.userScopesToInclude = null;
		}

		this.sameAsPostBuildScan = sameAsPostBuildScan;

		if (StringUtils.isNotBlank(hubWrapperProjectName)) {
			this.hubWrapperProjectName = hubWrapperProjectName.trim();
		} else {
			this.hubWrapperProjectName = null;
		}

		this.hubWrapperVersionPhase = hubWrapperVersionPhase;
		this.hubWrapperVersionDist = hubWrapperVersionDist;

		if (StringUtils.isNotBlank(hubWrapperProjectVersion)) {
			this.hubWrapperProjectVersion = hubWrapperProjectVersion.trim();
		} else {
			this.hubWrapperProjectVersion = null;
		}
	}

	public boolean isSameAsPostBuildScan() {
		return sameAsPostBuildScan;
	}

	public String getHubWrapperProjectName() {
		return hubWrapperProjectName;
	}

	public String getHubWrapperVersionPhase() {
		return hubWrapperVersionPhase;
	}

	public String getHubWrapperVersionDist() {
		return hubWrapperVersionDist;
	}

	public String getHubWrapperProjectVersion() {
		return hubWrapperProjectVersion;
	}

	public String getUserScopesToInclude() {
		return userScopesToInclude;
	}

	@Override
	public BDBuildWrapperDescriptor getDescriptor() {
		return (BDBuildWrapperDescriptor) super.getDescriptor();
	}

	public HubServerInfo getHubServerInfo() {
		return HubServerInfoSingleton.getInstance().getServerInfo();
	}

	public abstract List<String> getScopesAsList(IntLogger logger);

	@Override
	public abstract Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener) throws IOException, InterruptedException;

	public boolean universalTearDown(final AbstractBuild<?, ?> build, final IntLogger buildLogger, final FilePath buildInfoFilePath, final BDBuildWrapperDescriptor descriptor,
			final BuilderType buidler) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException,
	IOException {

		final ClassLoader originalClassLoader = Thread.currentThread()
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
				if (validateConfiguration(buildLogger)) {
					try {
						// read build-info.json file
						final BuildInfo buildInfo = readBuildInfo(build, buildInfoFilePath, build.getId(), buildLogger);

						buildLogger.info("# of Dependencies : " + buildInfo.getDependencies().size());

					} catch (final BDJenkinsHubPluginException e) {
						buildLogger.error(e);
						build.setResult(Result.UNSTABLE);
					}
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

	protected BuildInfo readBuildInfo(final AbstractBuild<?, ?> build, final FilePath buildInfoFilePath, final String buildId, final IntLogger buildLogger)
			throws BDJenkinsHubPluginException {
		final BuildInfo buildInfo = new BuildInfo();
		// Gets the build-info.json file so we can retrieve
		// the dependencies that were recorded to it
		// This parses the build-info.json file for the
		// dependencies and resolves them to catalog
		// components
		InputStream in = null;
		try {
			buildLogger.info("Reading BuildInfo from " + buildInfoFilePath);
			in = buildInfoFilePath.read();

			buildInfo.parseFileForDependencies(in, buildId);
		} catch (final IOException e) {
			throw new BDJenkinsHubPluginException(e);
		} catch (final Exception e) {
			throw new BDJenkinsHubPluginException(e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (final IOException e) {
					buildLogger.error(e.getMessage(), e);
					// eat exception
				}
			}
		}
		return buildInfo;
	}

	protected ProjectItem ensureProjectExists(final HubIntRestService service, final IntLogger logger,
			final String projectName) throws IOException,
	URISyntaxException,
	BDJenkinsHubPluginException {
		ProjectItem project = null;
		try {
			project = service.getProjectByName(projectName);

		} catch (final ProjectDoesNotExistException e) {
			// Project was not found, try to create it
			try {
				final String projectUrl = service.createHubProject(projectName);
				project = service.getProject(projectUrl);
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

		return project;
	}

	/**
	 * Ensures the Version exists. Returns the version URL
	 */
	protected ReleaseItem ensureVersionExists(final HubIntRestService service, final IntLogger logger,
			final String projectVersion, final ProjectItem project)
			throws IOException, URISyntaxException, BDJenkinsHubPluginException {
		ReleaseItem version = null;
		try {
			version = service.getVersion(project, projectVersion);
			if (!version.getPhase().equals(getHubWrapperVersionPhase())) {
				logger.warn(
						"The selected Phase does not match the Phase of this Version. If you wish to update the Phase please do so in the Hub UI.");
			}
			if (!version.getDistribution().equals(getHubWrapperVersionDist())) {
				logger.warn(
						"The selected Distribution does not match the Distribution of this Version. If you wish to update the Distribution please do so in the Hub UI.");
			}
		} catch (final VersionDoesNotExistException e) {
			try {
				final String versionURL = service.createHubVersion(project, projectVersion, getHubWrapperVersionPhase(),
						getHubWrapperVersionDist());
				version = service.getProjectVersion(versionURL);
			} catch (final BDRestException e1) {
				if (e1.getResource() != null) {
					logger.error("Status : " + e1.getResource().getStatus().getCode());
					logger.error("Response : " + e1.getResource().getResponse().getEntityAsText());
				}
				throw new BDJenkinsHubPluginException("Problem creating the Version. ", e1);
			}
		} catch (final BDRestException e) {
			throw new BDJenkinsHubPluginException("Could not retrieve or create the specified version.", e);
		}
		return version;
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

	/**
	 * Determine if plugin is enabled
	 *
	 */
	public boolean isPluginEnabled() {
		// Checks to make sure the user provided an application name and version
		// also checks to make sure a server url, username, and password were
		// provided
		final boolean isPluginConfigured = getHubServerInfo() != null
				&& getHubServerInfo().isPluginConfigured();
		final boolean isPluginEnabled = StringUtils
				.isNotBlank(getHubWrapperProjectName()) &&
				StringUtils.isNotBlank(getHubWrapperVersionPhase()) &&
				StringUtils.isNotBlank(getHubWrapperVersionDist()) &&
				StringUtils.isNotBlank(getHubWrapperProjectVersion()) &&
				StringUtils.isNotBlank(getUserScopesToInclude());

		boolean scopesProvided = true;
		final List<String> scopes = getScopesAsList(null);
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
	public Boolean validateConfiguration(final IntLogger logger) {
		// Checks to make sure the user provided an application name and version
		// also checks to make sure a server url, username, and password were
		// provided

		boolean isPluginConfigured = true;
		if (getHubServerInfo() == null) {
			isPluginConfigured = false;
			logger.error("Could not find the Hub global configuration!");
		} else {
			if (StringUtils.isBlank(getHubServerInfo().getServerUrl())) {
				isPluginConfigured = false;
				logger.error("The Hub server URL is not configured!");
			}
			if (StringUtils.isBlank(getHubServerInfo().getCredentialsId())) {
				isPluginConfigured = false;
				logger.error("No Hub credentials configured!");
			} else {
				if (StringUtils.isBlank(getHubServerInfo().getUsername())) {
					isPluginConfigured = false;
					logger.error("No Hub username configured!");
				}
				if (StringUtils.isBlank(getHubServerInfo().getPassword())) {
					isPluginConfigured = false;
					logger.error("No Hub password configured!");
				}
			}
		}
		if (StringUtils.isBlank(getHubWrapperProjectName())) {
			isPluginConfigured = false;
			logger.error("No Hub project name configured!");
		}
		if (StringUtils.isBlank(getHubWrapperProjectVersion())) {
			isPluginConfigured = false;
			logger.error("No Hub project version configured!");
		}
		if (hasScopes(logger, getUserScopesToInclude())) {
			final List<String> scopes = getScopesAsList(logger);
			if (scopes == null || scopes.isEmpty()) {
				isPluginConfigured = false;
			}
		}

		return isPluginConfigured;
	}

	protected boolean hasScopes(final IntLogger logger, final String scopes) {
		if (StringUtils.isBlank(scopes)) {
			logger.error("No Maven scopes configured!");
			return false;
		}
		return true;
	}

}
