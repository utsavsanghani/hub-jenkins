package com.blackducksoftware.integration.hub.jenkins.workflow;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.ScanJobs;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.remote.GetSystemProperty;
import com.blackducksoftware.integration.hub.jenkins.scan.BDCommonDescriptorUtil;
import com.blackducksoftware.integration.hub.jenkins.scan.BDCommonScanStep;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Computer;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
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
            return BDCommonDescriptorUtil.doCheckScanMemory(scanMemory);
        }

        public FormValidation doCheckBomUpdateMaxiumWaitTime(
                @QueryParameter("bomUpdateMaxiumWaitTime") final String bomUpdateMaxiumWaitTime)
                throws IOException, ServletException {
            return BDCommonDescriptorUtil.doCheckBomUpdateMaxiumWaitTime(bomUpdateMaxiumWaitTime);
        }

        /**
         * Fills the Credential drop down list in the global config
         *
         * @return
         */
        public ListBoxModel doFillHubCredentialsIdItems() {
            return BDCommonDescriptorUtil.doFillCredentialsIdItems();
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
                final BDCommonScanStep scanStep = new BDCommonScanStep(hubScanStep.getScans(),
                        hubScanStep.getHubProjectName(), hubScanStep.getHubProjectVersion(),
                        hubScanStep.getHubVersionPhase(), hubScanStep.getHubVersionDist(), hubScanStep.getScanMemory(),
                        hubScanStep.getShouldGenerateHubReport(), hubScanStep.getBomUpdateMaxiumWaitTime(),
                        hubScanStep.isDryRun(), hubScanStep.isVerbose());

                final JDK jdk = determineJava(logger, node, envVars);
                final FilePath javaHome = new FilePath(node.getChannel(), jdk.getHome());
                scanStep.runScan(run, node, envVars, workspace, logger, launcher, listener,
                        run.getFullDisplayName(),
                        String.valueOf(run.getNumber()), javaHome);

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
