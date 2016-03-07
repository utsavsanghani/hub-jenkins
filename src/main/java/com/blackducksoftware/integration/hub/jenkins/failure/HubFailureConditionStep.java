package com.blackducksoftware.integration.hub.jenkins.failure;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubScan;
import com.blackducksoftware.integration.hub.jenkins.action.HubScanFinishedAction;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatus;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatusEnum;
import com.blackducksoftware.integration.hub.response.ProjectItem;
import com.blackducksoftware.integration.hub.response.ReleaseItem;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;

public class HubFailureConditionStep extends Recorder {

    // TODO test this class and its descriptor

    private final Boolean failBuildForPolicyViolations;

    @DataBoundConstructor
    public HubFailureConditionStep(Boolean failBuildForPolicyViolations) {
        this.failBuildForPolicyViolations = failBuildForPolicyViolations;
    }

    public Boolean getFailBuildForPolicyViolations() {
        return failBuildForPolicyViolations;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public HubFailureConditionStepDescriptor getDescriptor() {
        return (HubFailureConditionStepDescriptor) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        HubJenkinsLogger logger = new HubJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.TRACE); // TODO make the log level configurable

        if (build.getResult() != Result.SUCCESS) {
            logger.error("The Build did not run sucessfully, will not check the Hub Failure Conditions.");
            return true;
        }
        PostBuildHubScan hubScanStep = null;
        List<Publisher> publishers = build.getProject().getPublishersList();
        // TODO add support for Build wrappers when we start using them
        if (publishers == null || publishers.isEmpty()) {
            // User didn't configure the job with a Hub Scan
            logger.error("Could not find the Hub Scan step for this Build.");
            build.setResult(Result.UNSTABLE);
            return true;
        }

        for (Publisher publisher : publishers) {
            if (publisher instanceof PostBuildHubScan) {
                hubScanStep = (PostBuildHubScan) publisher;
            }
        }
        if (hubScanStep == null) {
            // User didn't configure the job with a Hub Scan
            logger.error("Could not find the Hub Scan step for this Build.");
            build.setResult(Result.UNSTABLE);
            return true;
        } else {
            if (build.getAction(HubScanFinishedAction.class) == null) {
                logger.error("The scan must be configured to run before the Failure Conditions.");
                build.setResult(Result.UNSTABLE);
                return true;
            }
        }

        if (!getFailBuildForPolicyViolations()) {
            logger.error("The Hub failure condition step has not been configured to do anything.");
            return true;
        }

        HubServerInfo serverInfo = HubServerInfoSingleton.getInstance().getServerInfo();
        try {
            HubIntRestService restService = BuildHelper.getRestService(logger, serverInfo.getServerUrl(), serverInfo.getUsername(), serverInfo.getPassword(),
                    serverInfo.getTimeout());

            ProjectItem project;
            try {
                project = restService.getProjectByName(hubScanStep.getHubProjectName());
                if (project == null) {
                    logger.error("Could not find the specified Hub Project.");
                    return true;
                }
            } catch (ProjectDoesNotExistException e) {
                logger.error(e.getMessage(), e);
                return true;
            }

            String versionId = null;

            List<ReleaseItem> projectVersions = restService.getVersionsForProject(project.getId());
            for (ReleaseItem release : projectVersions) {
                if (hubScanStep.getHubProjectVersion().equals(release.getVersion())) {
                    versionId = release.getId();
                }
            }
            if (versionId == null) {
                logger.error("Could not find the specified Version for this Hub Project.");
                return true;
            }

            HubSupportHelper hubSupport = new HubSupportHelper();
            HubIntRestService service;
            service = BuildHelper.getRestService(logger, serverInfo.getServerUrl(),
                    serverInfo.getUsername(),
                    serverInfo.getPassword(),
                    serverInfo.getTimeout());

            hubSupport.checkHubSupport(service, logger);

            if (!hubSupport.isPolicyApiSupport()) {
                logger.error("This version of the Hub does not have support for Policies.");
                build.setResult(Result.UNSTABLE);
                return true;
            } else if (getFailBuildForPolicyViolations()) {
                // We use this conditional in case there are other failure conditions in the future
                PolicyStatus policyStatus = restService.getPolicyStatus(project.getId(), versionId);
                if (policyStatus.getOverallStatusEnum() == PolicyStatusEnum.IN_VIOLATION) {
                    build.setResult(Result.FAILURE);
                }
                logger.info("Found " + policyStatus.getStatusCounts().getIN_VIOLATION() + " bom entries to be In Violation of a defined Policy.");
                logger.info("Found " + policyStatus.getStatusCounts().getIN_VIOLATION_OVERRIDDEN()
                        + " bom entries to be In Violation of a defined Policy, but they have been manually overridden.");
                logger.info("Found " + policyStatus.getStatusCounts().getNOT_IN_VIOLATION() + " bom entries to be Not In Violation of a defined Policy.");

            }
        } catch (BDJenkinsHubPluginException e) {
            logger.error(e.getMessage(), e);
        } catch (HubIntegrationException e) {
            logger.error(e.getMessage(), e);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(), e);
        } catch (BDRestException e) {
            logger.error(e.getMessage(), e);
        }
        return true;
    }
}
