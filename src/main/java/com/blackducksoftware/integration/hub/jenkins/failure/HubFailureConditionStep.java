package com.blackducksoftware.integration.hub.jenkins.failure;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.MissingPolicyStatusException;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubScan;
import com.blackducksoftware.integration.hub.jenkins.action.BomUpToDateAction;
import com.blackducksoftware.integration.hub.jenkins.action.HubScanFinishedAction;
import com.blackducksoftware.integration.hub.jenkins.bom.RemoteHubEventPolling;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;
import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.blackducksoftware.integration.hub.logging.LogLevel;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatus;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatusEnum;
import com.blackducksoftware.integration.hub.report.api.HubReportGenerationInfo;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class HubFailureConditionStep extends Recorder {

	private final Boolean failBuildForPolicyViolations;

	@DataBoundConstructor
	public HubFailureConditionStep(final Boolean failBuildForPolicyViolations) {
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
	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher,
			final BuildListener listener) throws InterruptedException, IOException {
		final HubJenkinsLogger logger = new HubJenkinsLogger(listener);
		logger.setLogLevel(LogLevel.TRACE);

		if (build.getResult() != Result.SUCCESS) {
			logger.error("The Build did not run sucessfully, will not check the Hub Failure Conditions.");
			return true;
		}
		PostBuildHubScan hubScanStep = null;
		final List<Publisher> publishers = build.getProject().getPublishersList();
		// TODO add support for Build wrappers when we start using them
		if (publishers == null || publishers.isEmpty()) {
			// User didn't configure the job with a Hub Scan
			logger.error("Could not find the Hub Scan step for this Build.");
			build.setResult(Result.UNSTABLE);
			return true;
		}

		for (final Publisher publisher : publishers) {
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

		final HubServerInfo serverInfo = HubServerInfoSingleton.getInstance().getServerInfo();
		try {
			final HubIntRestService restService = getHubIntRestService(logger, serverInfo);

			final HubSupportHelper hubSupport = getDescriptor().getCheckedHubSupportHelper();

			final BomUpToDateAction action = build.getAction(BomUpToDateAction.class);
			if (action == null) {
				throw new BDJenkinsHubPluginException(
						"Could not find the BomUpToDateAction in the Hub Failure Conditions. Make sure the Hub scan was run before the Failure Conditions.");
			}
			waitForBomToBeUpdated(build, logger, action, restService, hubSupport);


			if (!hubSupport.isPolicyApiSupport()) {
				logger.error("This version of the Hub does not have support for Policies.");
				build.setResult(Result.UNSTABLE);
				return true;
			} else if (getFailBuildForPolicyViolations()) {
				try {
					// We use this conditional in case there are other failure conditions in the future
					final PolicyStatus policyStatus = restService.getPolicyStatus(action.getPolicyStatusUrl());
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
				} catch (final MissingPolicyStatusException e) {
					logger.warn(e.getMessage());
				}
			}
		} catch (final BDJenkinsHubPluginException e) {
			logger.error(e.getMessage(), e);
			build.setResult(Result.UNSTABLE);
		} catch (final HubIntegrationException e) {
			logger.error(e.getMessage(), e);
			build.setResult(Result.UNSTABLE);
		} catch (final URISyntaxException e) {
			logger.error(e.getMessage(), e);
			build.setResult(Result.UNSTABLE);
		} catch (final BDRestException e) {
			logger.error(e.getMessage(), e);
			build.setResult(Result.UNSTABLE);
		}
		return true;
	}

	public HubIntRestService getHubIntRestService(final HubJenkinsLogger logger, final HubServerInfo serverInfo) throws IOException,
	BDRestException, URISyntaxException, BDJenkinsHubPluginException, HubIntegrationException {
		return BuildHelper.getRestService(logger, serverInfo.getServerUrl(), serverInfo.getUsername(), serverInfo.getPassword(),
				serverInfo.getTimeout());
	}

	public void waitForBomToBeUpdated(final AbstractBuild<?, ?> build, final IntLogger logger,
			final BomUpToDateAction action, final HubIntRestService service, final HubSupportHelper supportHelper)
					throws BDJenkinsHubPluginException, InterruptedException, BDRestException, HubIntegrationException,
					URISyntaxException, IOException {
		if(action.isHasBomBeenUdpated()){
			return;
		}

		final HubReportGenerationInfo reportGenInfo = new HubReportGenerationInfo();
		reportGenInfo.setService(service);
		reportGenInfo.setHostname(action.getLocalHostName());
		reportGenInfo.setScanTargets(action.getScanTargets());

		reportGenInfo.setMaximumWaitTime(action.getMaxWaitTime());

		reportGenInfo.setBeforeScanTime(action.getBeforeScanTime());
		reportGenInfo.setAfterScanTime(action.getAfterScanTime());

		reportGenInfo.setScanStatusDirectory(action.getScanStatusDirectory());

		final RemoteHubEventPolling hubEventPolling = new RemoteHubEventPolling(service, build.getBuiltOn().getChannel());

		if (supportHelper.isCliStatusDirOptionSupport()) {
			hubEventPolling.assertBomUpToDate(reportGenInfo, logger);
		} else {
			hubEventPolling.assertBomUpToDate(reportGenInfo);
		}


	}

}
