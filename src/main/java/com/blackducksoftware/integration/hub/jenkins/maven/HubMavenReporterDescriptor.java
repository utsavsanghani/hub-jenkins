package com.blackducksoftware.integration.hub.jenkins.maven;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.exception.BDCIScopeException;
import com.blackducksoftware.integration.hub.jenkins.BDCommonDescriptorUtil;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.helper.PluginHelper;
import com.blackducksoftware.integration.hub.maven.Scope;
import com.blackducksoftware.integration.hub.version.api.DistributionEnum;
import com.blackducksoftware.integration.hub.version.api.PhaseEnum;

import hudson.maven.MavenReporterDescriptor;
import hudson.model.AutoCompletionCandidates;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

// TODO Uncomment extension to add the maven reporter back in
// @Extension(optional = true)
// This indicates to Jenkins that this is an implementation of an extension
// point.
public class HubMavenReporterDescriptor extends MavenReporterDescriptor {

	/**
	 * In order to load the persisted global configuration, you have to call
	 * load() in the constructor.
	 */
	public HubMavenReporterDescriptor() {
		super(HubMavenReporter.class);
		load();
	}

	@Override
	public String getDisplayName() {
		// Intentional return the BuildWrapper constants
		return Messages.HubMavenWrapper_getDisplayName();
	}

	public String getPluginVersion() {
		return PluginHelper.getPluginVersion();
	}

	public HubServerInfo getHubServerInfo() {
		return HubServerInfoSingleton.getInstance().getServerInfo();
	}

	/**
	 * Fills the drop down list of possible Version phases
	 *
	 * @return
	 */
	public ListBoxModel doFillMavenHubVersionPhaseItems() {
		final ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		final boolean changed = false;
		final ListBoxModel items = new ListBoxModel();
		try {
			// should get this list from the Hub server, ticket HUB-1610
			for (final PhaseEnum phase : PhaseEnum.values()) {
				if (phase != PhaseEnum.UNKNOWNPHASE) {
					items.add(phase.name(), phase.name());
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(
						originalClassLoader);
			}
		}
		return items;
	}

	/**
	 * Fills the drop down list of possible Version distribution types
	 *
	 * @return
	 */
	public ListBoxModel doFillMavenHubVersionDistItems() {
		final ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		final boolean changed = false;
		final ListBoxModel items = new ListBoxModel();
		try {
			// should get this list from the Hub server, ticket HUB-1610
			for (final DistributionEnum distribution : DistributionEnum.values()) {
				if (distribution != DistributionEnum.UNKNOWNDISTRIBUTION) {
					items.add(distribution.name(), distribution.name());
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(
						originalClassLoader);
			}
		}
		return items;
	}

	public AutoCompletionCandidates doAutoCompleteMavenHubProjectName(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName)
			throws IOException, ServletException {
		return BDCommonDescriptorUtil.doAutoCompleteHubProjectName(getHubServerInfo(), mavenHubProjectName);
	}

	/**
	 * Performs on-the-fly validation of the form field 'mavenHubProjectName'.
	 * Checks to see if there is already a project in the Hub with this name.
	 *
	 */
	public FormValidation doCheckMavenHubProjectName(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName,
			@QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion) throws IOException, ServletException {
		return BDCommonDescriptorUtil.doCheckHubProjectName(getHubServerInfo(), mavenHubProjectName, mavenHubProjectVersion);
	}

	/**
	 * Performs on-the-fly validation of the form field 'mavenHubProjectVersion'. Checks to see if there is already a
	 * project
	 * in the Hub with this name.
	 *
	 */
	public FormValidation doCheckMavenHubProjectVersion(@QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion,
			@QueryParameter("mavenHubProjectName") final String mavenHubProjectName) throws IOException, ServletException {
		return BDCommonDescriptorUtil.doCheckHubProjectVersion(getHubServerInfo(), mavenHubProjectVersion,
				mavenHubProjectName);
	}

	/**
	 * Creates the Hub project AND/OR version
	 *
	 *
	 */
	public FormValidation doCreateMavenHubProject(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName,
			@QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion,
			@QueryParameter("mavenHubVersionPhase") final String mavenHubVersionPhase,
			@QueryParameter("mavenHubVersionDist") final String mavenHubVersionDist) {
		return BDCommonDescriptorUtil.doCreateHubProject(getHubServerInfo(), mavenHubProjectName,
				mavenHubProjectVersion, mavenHubVersionPhase, mavenHubVersionDist);
	}

	/**
	 * Performs on-the-fly validation of the form field 'userScopesToInclude'.
	 *
	 */
	public FormValidation doCheckUserScopesToInclude(@QueryParameter final String value)
			throws IOException, ServletException {
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
		return FormValidation.ok();
	}

}
