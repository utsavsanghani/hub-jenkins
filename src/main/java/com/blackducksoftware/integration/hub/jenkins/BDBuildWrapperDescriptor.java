package com.blackducksoftware.integration.hub.jenkins;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.exception.BDCIScopeException;
import com.blackducksoftware.integration.hub.jenkins.gradle.GradleBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.jenkins.helper.PluginHelper;
import com.blackducksoftware.integration.hub.jenkins.maven.MavenBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.maven.Scope;

import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

// This indicates to Jenkins that this is an implementation of an extension
// point.
public class BDBuildWrapperDescriptor extends BuildWrapperDescriptor implements Serializable {

	/**
	 * In order to load the persisted global configuration, you have to call
	 * load() in the constructor.
	 */
	public BDBuildWrapperDescriptor() {
		super(BDBuildWrapper.class);
		load();
	}

	/**
	 * In order to load the persisted global configuration, you have to call
	 * load() in the constructor.
	 */
	public BDBuildWrapperDescriptor(final Class<? extends BuildWrapper> subClass) {
		super(subClass);
		load();
	}

	public HubServerInfo getHubServerInfo() {
		return HubServerInfoSingleton.getInstance().getServerInfo();
	}

	/**
	 * Fills the drop down list of possible Version phases
	 *
	 * @return
	 */
	public ListBoxModel doFillHubWrapperVersionPhaseItems() {
		return BDCommonDescriptorUtil.doFillHubVersionPhaseItems();
	}

	/**
	 * Fills the drop down list of possible Version distribution types
	 *
	 * @return
	 */
	public ListBoxModel doFillHubWrapperVersionDistItems() {
		return BDCommonDescriptorUtil.doFillHubVersionDistItems();
	}

	public AutoCompletionCandidates doAutoCompleteHubWrapperProjectName(@QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName)
			throws IOException,
			ServletException {
		return BDCommonDescriptorUtil.doAutoCompleteHubProjectName(getHubServerInfo(), hubWrapperProjectName);
	}

	/**
	 * Performs on-the-fly validation of the form field 'hubWrapperProjectName'. Checks to see if there is already a
	 * project in
	 * the Hub with this name.
	 *
	 */
	public FormValidation doCheckHubWrapperProjectName(@QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName,
			@QueryParameter("hubWrapperProjectVersion") final String hubWrapperProjectVersion) throws IOException, ServletException {
		return BDCommonDescriptorUtil.doCheckHubProjectName(getHubServerInfo(), hubWrapperProjectName,
				hubWrapperProjectVersion);
	}

	/**
	 * Performs on-the-fly validation of the form field 'hubWrapperProjectVersion'. Checks to see if there is already a
	 * project
	 * in the Hub with this name.
	 *
	 */
	public FormValidation doCheckHubWrapperProjectVersion(@QueryParameter("hubWrapperProjectVersion") final String hubWrapperProjectVersion,
			@QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName) throws IOException, ServletException {
		return BDCommonDescriptorUtil.doCheckHubProjectVersion(getHubServerInfo(), hubWrapperProjectVersion,
				hubWrapperProjectName);
	}

	/**
	 * Creates the Hub project AND/OR version
	 *
	 *
	 */
	public FormValidation doCreateHubWrapperProject(@QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName,
			@QueryParameter("hubWrapperProjectVersion") final String hubWrapperProjectVersion,
			@QueryParameter("hubWrapperVersionPhase") final String hubWrapperVersionPhase,
			@QueryParameter("hubWrapperVersionDist") final String hubWrapperVersionDist) {

		save();
		return BDCommonDescriptorUtil.doCreateHubProject(getHubServerInfo(), hubWrapperProjectName,
				hubWrapperProjectVersion, hubWrapperVersionPhase, hubWrapperVersionDist);
	}

	/**
	 * Performs on-the-fly validation of the form field 'userScopesToInclude'.
	 *
	 */
	public FormValidation doCheckUserScopesToInclude(@QueryParameter final String value)
			throws IOException, ServletException {
		if (this instanceof MavenBuildWrapperDescriptor) {
			if (value.length() == 0) {
				return FormValidation.error(Messages
						.HubMavenWrapper_getPleaseIncludeAScope());
			}
			try {
				Scope.getScopeListFromString(value);
			} catch (final BDCIScopeException e) {
				final String scope = e.getMessage().substring(e.getMessage().indexOf(":") + 1).trim();
				return FormValidation.error(Messages.HubMavenWrapper_getIncludedInvalidScope_0_(scope));
			}
		} else if (this instanceof GradleBuildWrapperDescriptor) {
			if (value.length() == 0) {
				return FormValidation.error(Messages
						.HubGradleWrapper_getPleaseIncludeAConfiguration());
			}
		}
		return FormValidation.ok();
	}

	@Override
	public boolean isApplicable(final AbstractProject<?, ?> aClass) {
		// Indicates that this builder can be used with all kinds of project
		// types
		return aClass.getClass().isAssignableFrom(FreeStyleProject.class);
		// || aClass.getClass().isAssignableFrom(MavenModuleSet.class);
	}

	@Override
	public String getDisplayName() {
		return "";
	}

	public String getPluginVersion() {
		return PluginHelper.getPluginVersion();
	}

}
