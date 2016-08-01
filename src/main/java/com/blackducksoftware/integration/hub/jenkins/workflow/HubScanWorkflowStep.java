package com.blackducksoftware.integration.hub.jenkins.workflow;

import java.io.IOException;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.builder.HubScanJobConfigBuilder;
import com.blackducksoftware.integration.hub.builder.ValidationResultEnum;
import com.blackducksoftware.integration.hub.builder.ValidationResults;
import com.blackducksoftware.integration.hub.jenkins.BDCommonDescriptorUtil;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.ScanJobs;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.remote.GetSystemProperty;
import com.blackducksoftware.integration.hub.jenkins.scan.HubCommonScanStep;
import com.blackducksoftware.integration.hub.job.HubScanJobConfig;
import com.blackducksoftware.integration.hub.job.HubScanJobFieldEnum;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Computer;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class HubScanWorkflowStep extends AbstractStepImpl {

	private final ScanJobs[] scans;
	private final String hubProjectName;
	private final String hubProjectVersion;
	private final String hubVersionPhase;
	private final String hubVersionDist;
	private final String scanMemory;
	private final boolean shouldGenerateHubReport;
	private final String bomUpdateMaxiumWaitTime;
	private final boolean dryRun;
	private Boolean verbose;

	@DataBoundConstructor
	public HubScanWorkflowStep(final ScanJobs[] scans, final String hubProjectName, final String hubProjectVersion,
			final String hubVersionPhase, final String hubVersionDist, final String scanMemory,
			final boolean shouldGenerateHubReport, final String bomUpdateMaxiumWaitTime, final boolean dryRun) {
		this.scans = scans;
		this.hubProjectName = hubProjectName;
		this.hubVersionPhase = hubVersionPhase;
		this.hubVersionDist = hubVersionDist;
		this.hubProjectVersion = hubProjectVersion;
		this.scanMemory = scanMemory;
		this.shouldGenerateHubReport = shouldGenerateHubReport;
		this.bomUpdateMaxiumWaitTime = bomUpdateMaxiumWaitTime;
		this.dryRun = dryRun;
	}

	public void setVerbose(final boolean verbose) {
		this.verbose = verbose;
	}

	public boolean isVerbose() {
		if (verbose == null) {
			verbose = true;
		}
		return verbose;
	}

	public ScanJobs[] getScans() {
		return scans;
	}

	public String getHubProjectName() {
		return hubProjectName;
	}

	public String getHubProjectVersion() {
		return hubProjectVersion;
	}

	public String getHubVersionPhase() {
		return hubVersionPhase;
	}

	public String getHubVersionDist() {
		return hubVersionDist;
	}

	public String getScanMemory() {
		return scanMemory;
	}

	public boolean getShouldGenerateHubReport() {
		return shouldGenerateHubReport;
	}

	public String getBomUpdateMaxiumWaitTime() {
		return bomUpdateMaxiumWaitTime;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	@Override
	public HubScanWorkflowStepDescriptor getDescriptor() {
		return (HubScanWorkflowStepDescriptor) super.getDescriptor();
	}

	@Extension(optional = true)
	public static final class HubScanWorkflowStepDescriptor extends AbstractStepDescriptorImpl {

		public HubScanWorkflowStepDescriptor() {
			super(Execution.class);
		}

		@Override
		public String getFunctionName() {
			return "hub_scan";
		}

		@Override
		public String getDisplayName() {
			return Messages.HubBuildScan_getDisplayName();
		}

		/**
		 * @return the hubServerInfo
		 */
		public HubServerInfo getHubServerInfo() {
			return HubServerInfoSingleton.getInstance().getServerInfo();
		}

		public FormValidation doCheckScanMemory(@QueryParameter("scanMemory") final String scanMemory)
				throws IOException, ServletException {
			final ValidationResults<HubScanJobFieldEnum, HubScanJobConfig> results = new ValidationResults<HubScanJobFieldEnum, HubScanJobConfig>();
			final HubScanJobConfigBuilder builder = new HubScanJobConfigBuilder(false);
			builder.setScanMemory(scanMemory);
			builder.validateScanMemory(results);

			if (!results.isSuccess()) {
				if (results.hasWarnings()) {
					return FormValidation.warning(
							results.getResultString(HubScanJobFieldEnum.SCANMEMORY, ValidationResultEnum.WARN));
				} else if (results.hasErrors()) {
					return FormValidation
							.error(results.getResultString(HubScanJobFieldEnum.SCANMEMORY, ValidationResultEnum.ERROR));
				}
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckBomUpdateMaxiumWaitTime(
				@QueryParameter("bomUpdateMaxiumWaitTime") final String bomUpdateMaxiumWaitTime)
						throws IOException, ServletException {
			final ValidationResults<HubScanJobFieldEnum, HubScanJobConfig> results = new ValidationResults<HubScanJobFieldEnum, HubScanJobConfig>();
			final HubScanJobConfigBuilder builder = new HubScanJobConfigBuilder(false);
			builder.setMaxWaitTimeForBomUpdate(bomUpdateMaxiumWaitTime);
			builder.validateMaxWaitTimeForBomUpdate(results);

			if (!results.isSuccess()) {
				if (results.hasWarnings()) {
					return FormValidation.warning(results.getResultString(
							HubScanJobFieldEnum.MAX_WAIT_TIME_FOR_BOM_UPDATE, ValidationResultEnum.WARN));
				} else if (results.hasErrors()) {
					return FormValidation.error(results.getResultString(
							HubScanJobFieldEnum.MAX_WAIT_TIME_FOR_BOM_UPDATE, ValidationResultEnum.ERROR));
				}
			}
			return FormValidation.ok();
		}

		/**
		 * Fills the Credential drop down list in the global config
		 *
		 * @return
		 */
		public ListBoxModel doFillHubCredentialsIdItems() {

			ListBoxModel boxModel = null;
			final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
			boolean changed = false;
			try {
				if (PostBuildScanDescriptor.class.getClassLoader() != originalClassLoader) {
					changed = true;
					Thread.currentThread().setContextClassLoader(PostBuildScanDescriptor.class.getClassLoader());
				}

				// Code copied from
				// https://github.com/jenkinsci/git-plugin/blob/f6d42c4e7edb102d3330af5ca66a7f5809d1a48e/src/main/java/hudson/plugins/git/UserRemoteConfig.java
				final CredentialsMatcher credentialsMatcher = CredentialsMatchers
						.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
				final AbstractProject<?, ?> project = null; // Dont want to
				// limit
				// the search to a
				// particular project
				// for the drop
				// down menu
				boxModel = new StandardListBoxModel().withEmptySelection().withMatching(credentialsMatcher,
						CredentialsProvider.lookupCredentials(StandardCredentials.class, project, ACL.SYSTEM,
								Collections.<DomainRequirement> emptyList()));
			} finally {
				if (changed) {
					Thread.currentThread().setContextClassLoader(originalClassLoader);
				}
			}
			return boxModel;
		}

		/**
		 * Fills the drop down list of possible Version phases
		 *
		 */
		public ListBoxModel doFillHubVersionPhaseItems() {
			return BDCommonDescriptorUtil.doFillHubVersionPhaseItems();
		}

		/**
		 * Fills the drop down list of possible Version distribution types
		 *
		 */
		public ListBoxModel doFillHubVersionDistItems() {
			return BDCommonDescriptorUtil.doFillHubVersionDistItems();
		}

		public AutoCompletionCandidates doAutoCompleteHubProjectName(
				@QueryParameter("value") final String hubProjectName) throws IOException, ServletException {
			return BDCommonDescriptorUtil.doAutoCompleteHubProjectName(getHubServerInfo(), hubProjectName);
		}

		/**
		 * Performs on-the-fly validation of the form field 'hubProjectName'.
		 * Checks to see if there is already a project in the Hub with this
		 * name.
		 *
		 */
		public FormValidation doCheckHubProjectName(@QueryParameter("hubProjectName") final String hubProjectName,
				@QueryParameter("hubProjectVersion") final String hubProjectVersion,
				@QueryParameter("dryRun") final boolean dryRun) throws IOException, ServletException {
			return BDCommonDescriptorUtil.doCheckHubProjectName(getHubServerInfo(), hubProjectName, hubProjectVersion,
					dryRun);
		}

		/**
		 * Performs on-the-fly validation of the form field 'hubProjectVersion'.
		 * Checks to see if there is already a project in the Hub with this
		 * name.
		 *
		 */
		public FormValidation doCheckHubProjectVersion(
				@QueryParameter("hubProjectVersion") final String hubProjectVersion,
				@QueryParameter("hubProjectName") final String hubProjectName,
				@QueryParameter("dryRun") final boolean dryRun) throws IOException, ServletException {
			return BDCommonDescriptorUtil.doCheckHubProjectVersion(getHubServerInfo(), hubProjectVersion,
					hubProjectName, dryRun);
		}

		/**
		 * Creates the Hub project AND/OR version
		 *
		 *
		 */
		public FormValidation doCreateHubProject(@QueryParameter("hubProjectName") final String hubProjectName,
				@QueryParameter("hubProjectVersion") final String hubProjectVersion,
				@QueryParameter("hubVersionPhase") final String hubVersionPhase,
				@QueryParameter("hubVersionDist") final String hubVersionDist) {
			save();
			return BDCommonDescriptorUtil.doCreateHubProject(getHubServerInfo(), hubProjectName, hubProjectVersion,
					hubVersionPhase, hubVersionDist);
		}

	}

	public static final class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {

		private static final long serialVersionUID = 1L;

		@Inject
		private transient HubScanWorkflowStep hubScanStep;
		@StepContextParameter
		private transient Computer computer;
		@StepContextParameter
		transient Launcher launcher;
		@StepContextParameter
		transient TaskListener listener;
		@StepContextParameter
		transient EnvVars envVars;
		@StepContextParameter
		private transient FilePath workspace;
		@StepContextParameter
		private transient Run run;

		@Override
		protected Void run() {
			final HubJenkinsLogger logger = new HubJenkinsLogger(listener);
			try {
				final Node node = computer.getNode();
				final HubCommonScanStep scanStep = new HubCommonScanStep(hubScanStep.getScans(),
						hubScanStep.getHubProjectName(), hubScanStep.getHubProjectVersion(),
						hubScanStep.getHubVersionPhase(), hubScanStep.getHubVersionDist(), hubScanStep.getScanMemory(),
						hubScanStep.getShouldGenerateHubReport(), hubScanStep.getBomUpdateMaxiumWaitTime(),
						hubScanStep.isDryRun(), hubScanStep.isVerbose());

				final JDK jdk = determineJava(logger, node, envVars);
				final FilePath javaHome = new FilePath(node.getChannel(), jdk.getHome());
				scanStep.runScan(run, node, envVars, workspace, logger, launcher, listener,
						run.getFullDisplayName(),
						run.getNumber(), javaHome);

			} catch (final Exception e) {
				logger.error(e);
				run.setResult(Result.UNSTABLE);
			}
			return null;
		}

		/**
		 * Sets the Java Home that is to be used for running the Shell script
		 *
		 */
		private JDK determineJava(final HubJenkinsLogger logger, final Node builtOn, final EnvVars envVars)
				throws IOException, InterruptedException, HubConfigurationException {
			JDK javaHomeTemp = null;

			if (StringUtils.isEmpty(builtOn.getNodeName())) {
				logger.info("Getting Jdk on master  : " + builtOn.getNodeName());
				// Empty node name indicates master
				final String byteCodeVersion = System.getProperty("java.class.version");
				final Double majorVersion = Double.valueOf(byteCodeVersion);
				if (majorVersion >= 51.0) {
					// Java 7 bytecode
					final String javaHome = System.getProperty("java.home");
					javaHomeTemp = new JDK("Java running master agent", javaHome);
				}
			} else {
				logger.info("Getting Jdk on node  : " + builtOn.getNodeName());

				final String byteCodeVersion = builtOn.getChannel().call(new GetSystemProperty("java.class.version"));
				final Double majorVersion = Double.valueOf(byteCodeVersion);
				if (majorVersion >= 51.0) {
					// Java 7 bytecode
					final String javaHome = builtOn.getChannel().call(new GetSystemProperty("java.home"));
					javaHomeTemp = new JDK("Java running slave agent", javaHome);
				}
			}
			if (javaHomeTemp != null && javaHomeTemp.getHome() != null) {
				logger.info("JDK home : " + javaHomeTemp.getHome());
			}

			if (javaHomeTemp == null || StringUtils.isEmpty(javaHomeTemp.getHome())) {
				logger.info("Could not find the specified Java installation, checking the JAVA_HOME variable.");
				if (envVars.get("JAVA_HOME") == null || envVars.get("JAVA_HOME") == "") {
					throw new HubConfigurationException("Need to define a JAVA_HOME or select an installed JDK.");
				}
				// In case the user did not select a java installation, set to
				// the
				// environment variable JAVA_HOME
				javaHomeTemp = new JDK("Default Java", envVars.get("JAVA_HOME"));
			}
			final FilePath javaHome = new FilePath(builtOn.getChannel(), javaHomeTemp.getHome());
			if (!javaHome.exists()) {
				throw new HubConfigurationException(
						"Could not find the specified Java installation at: " + javaHome.getRemote());
			}

			return javaHomeTemp;
		}

	}
}
