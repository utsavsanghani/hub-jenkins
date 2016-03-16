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

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.MissingPolicyStatusException;
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
            build.setResult(Result.UNSTABLE);
            return true;
        }

        HubServerInfo serverInfo = HubServerInfoSingleton.getInstance().getServerInfo();
        try {
            HubIntRestService restService = getHubIntRestService(logger, serverInfo);

            String projectId = getProjectId(logger, restService, hubScanStep.getHubProjectName());
            if (StringUtils.isBlank(projectId)) {
                build.setResult(Result.UNSTABLE);
                return true;
            }
            String versionId = getVersionId(logger, restService, projectId, hubScanStep.getHubProjectVersion());
            if (StringUtils.isBlank(versionId)) {
                build.setResult(Result.UNSTABLE);
                return true;
            }

            HubSupportHelper hubSupport = getDescriptor().getCheckedHubSupportHelper();

            if (!hubSupport.isPolicyApiSupport()) {
                logger.error("This version of the Hub does not have support for Policies.");
                build.setResult(Result.UNSTABLE);
                return true;
            } else if (getFailBuildForPolicyViolations()) {
                try {
                    // We use this conditional in case there are other failure conditions in the future
                    PolicyStatus policyStatus = restService.getPolicyStatus(projectId, versionId);
                    if (policyStatus == null) {
                        logger.error("Could not find any information about the Policy status of the bom.");
                        build.setResult(Result.UNSTABLE);
                    }
                    if (policyStatus.getOverallStatusEnum() == PolicyStatusEnum.IN_VIOLATION) {
                        build.setResult(Result.FAILURE);
                    }

                    if (policyStatus.getCountInViolation() == null) {
                        logger.error("Could not find the number of bom entries In Violation of a Policy.");
                    } else {
                        logger.info("Found " + policyStatus.getCountInViolation().getValue() + " bom entries to be In Violation of a defined Policy.");
                    }
                    if (policyStatus.getCountInViolationOverridden() == null) {
                        logger.error("Could not find the number of bom entries In Violation Overridden of a Policy.");
                    } else {
                        logger.info("Found " + policyStatus.getCountInViolationOverridden().getValue()
                                + " bom entries to be In Violation of a defined Policy, but they have been overridden.");
                    }
                    if (policyStatus.getCountNotInViolation() == null) {
                        logger.error("Could not find the number of bom entries Not In Violation of a Policy.");
                    } else {
                        logger.info("Found " + policyStatus.getCountNotInViolation().getValue() + " bom entries to be Not In Violation of a defined Policy.");
                    }
                } catch (MissingPolicyStatusException e) {
                    logger.error(e.getMessage());
                }
            }
        } catch (BDJenkinsHubPluginException e) {
            logger.error(e.getMessage(), e);
            build.setResult(Result.UNSTABLE);
        } catch (HubIntegrationException e) {
            logger.error(e.getMessage(), e);
            build.setResult(Result.UNSTABLE);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(), e);
            build.setResult(Result.UNSTABLE);
        } catch (BDRestException e) {
            logger.error(e.getMessage(), e);
            build.setResult(Result.UNSTABLE);
        }
        return true;
    }

    public String getProjectId(HubJenkinsLogger logger, HubIntRestService restService, String projectName) throws IOException, BDRestException,
            URISyntaxException {
        String projectId = null;
        ProjectItem project = null;
        try {
            project = restService.getProjectByName(projectName);
            if (project == null) {
                logger.error("Could not find the specified Hub Project.");
                return null;
            }
            projectId = project.getId();
            if (StringUtils.isBlank(projectId)) {
                logger.error("Could not find the specified Hub Project.");
                return null;
            }
        } catch (ProjectDoesNotExistException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
        return projectId;
    }

    public String getVersionId(HubJenkinsLogger logger, HubIntRestService restService, String projectId, String versionName) throws IOException,
            BDRestException, URISyntaxException {
        String versionId = null;

        List<ReleaseItem> projectVersions = restService.getVersionsForProject(projectId);
        if (projectVersions != null) {
            for (ReleaseItem release : projectVersions) {
                if (versionName.equals(release.getVersion())) {
                    versionId = release.getId();
                }
            }
        }
        if (StringUtils.isBlank(versionId)) {
            logger.error("Could not find the specified Version for this Hub Project.");
            return null;
        }
        return versionId;
    }

    public HubIntRestService getHubIntRestService(HubJenkinsLogger logger, HubServerInfo serverInfo) throws IOException,
            BDRestException, URISyntaxException, BDJenkinsHubPluginException, HubIntegrationException {
        return BuildHelper.getRestService(logger, serverInfo.getServerUrl(), serverInfo.getUsername(), serverInfo.getPassword(),
                serverInfo.getTimeout());
    }

}
