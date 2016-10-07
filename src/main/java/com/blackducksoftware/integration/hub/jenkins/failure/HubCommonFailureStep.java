package com.blackducksoftware.integration.hub.jenkins.failure;

import java.io.IOException;
import java.net.URISyntaxException;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusEnum;
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusItem;
import com.blackducksoftware.integration.hub.api.report.HubReportGenerationInfo;
import com.blackducksoftware.integration.hub.capabilities.HubCapabilitiesEnum;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.MissingUUIDException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.action.BomUpToDateAction;
import com.blackducksoftware.integration.hub.jenkins.action.HubVariableContributor;
import com.blackducksoftware.integration.hub.jenkins.bom.RemoteHubEventPolling;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.util.CIEnvironmentVariables;

import hudson.EnvVars;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

public class HubCommonFailureStep {

	private final Boolean failBuildForPolicyViolations;

	public HubCommonFailureStep(final Boolean failBuildForPolicyViolations) {
		this.failBuildForPolicyViolations = failBuildForPolicyViolations;
	}

	public Boolean getFailBuildForPolicyViolations() {
		return failBuildForPolicyViolations;
	}

	public boolean checkFailureConditions(final Run run, final Node builtOn, final EnvVars envVars,
			final HubJenkinsLogger logger, final TaskListener listener, final BomUpToDateAction bomUpToDateAction)
			throws InterruptedException, IOException {

		final CIEnvironmentVariables variables = new CIEnvironmentVariables();
		variables.putAll(envVars);
		logger.setLogLevel(variables);

		if (!getFailBuildForPolicyViolations()) {
			logger.error("The Hub failure condition step has not been configured to do anything.");
			run.setResult(Result.UNSTABLE);
			return true;
		}

		final HubServerInfo serverInfo = HubServerInfoSingleton.getInstance().getServerInfo();
		try {
			final HubIntRestService restService = getHubIntRestService(logger, serverInfo);

			final HubSupportHelper hubSupport = getCheckedHubSupportHelper();

			waitForBomToBeUpdated(builtOn, logger, bomUpToDateAction, restService, hubSupport);

			if (!hubSupport.hasCapability(HubCapabilitiesEnum.POLICY_API)) {
				logger.error("This version of the Hub does not have support for Policies.");
				run.setResult(Result.UNSTABLE);
				return true;
			} else if (getFailBuildForPolicyViolations()) {
				if (bomUpToDateAction.getPolicyStatusUrl() == null) {
					logger.error(
							"Can not check policy violations, could not find the policy status URL for this Version.");
					run.setResult(Result.UNSTABLE);
					return true;
				}
				// We use this conditional in case there are other failure
				// conditions in the future
				final PolicyStatusItem policyStatus = restService
						.getPolicyStatus(bomUpToDateAction.getPolicyStatusUrl());
				if (policyStatus == null) {
					logger.error("Could not find any information about the Policy status of the bom.");
					run.setResult(Result.UNSTABLE);
				}
				if (policyStatus.getOverallStatus() == PolicyStatusEnum.IN_VIOLATION) {
					run.setResult(Result.FAILURE);
				}

				final HubVariableContributor variableContributor = new HubVariableContributor();

				if (policyStatus.getCountInViolation() == null) {
					logger.error("Could not find the number of bom entries In Violation of a Policy.");
				} else {
					logger.info("Found " + policyStatus.getCountInViolation().getValue()
							+ " bom entries to be In Violation of a defined Policy.");
					variableContributor.setBomEntriesInViolation(policyStatus.getCountInViolation().getValue());
				}
				if (policyStatus.getCountInViolationOverridden() == null) {
					logger.error("Could not find the number of bom entries In Violation Overridden of a Policy.");
				} else {
					logger.info("Found " + policyStatus.getCountInViolationOverridden().getValue()
							+ " bom entries to be In Violation of a defined Policy, but they have been overridden.");
					variableContributor.setViolationsOverriden(policyStatus.getCountInViolationOverridden().getValue());
				}
				if (policyStatus.getCountNotInViolation() == null) {
					logger.error("Could not find the number of bom entries Not In Violation of a Policy.");
				} else {
					logger.info("Found " + policyStatus.getCountNotInViolation().getValue()
							+ " bom entries to be Not In Violation of a defined Policy.");
					variableContributor.setBomEntriesNotInViolation(policyStatus.getCountNotInViolation().getValue());
				}
				run.addAction(variableContributor);
			}
		} catch (final BDJenkinsHubPluginException e) {
			logger.error(e.getMessage(), e);
			run.setResult(Result.UNSTABLE);
		} catch (final HubIntegrationException e) {
			logger.error(e.getMessage(), e);
			run.setResult(Result.UNSTABLE);
		} catch (final URISyntaxException e) {
			logger.error(e.getMessage(), e);
			run.setResult(Result.UNSTABLE);
		} catch (final BDRestException e) {
			logger.error(e.getMessage(), e);
			run.setResult(Result.UNSTABLE);
		} catch (final ProjectDoesNotExistException e) {
			logger.error(e.getMessage(), e);
			run.setResult(Result.UNSTABLE);
		} catch (final MissingUUIDException e) {
			logger.error(e.getMessage(), e);
			run.setResult(Result.UNSTABLE);
		} catch (final UnexpectedHubResponseException e) {
			logger.error(e.getMessage(), e);
			run.setResult(Result.UNSTABLE);
		}
		return true;
	}

	public HubSupportHelper getCheckedHubSupportHelper() {
		final HubSupportHelper hubSupport = new HubSupportHelper();
		final HubServerInfo serverInfo = HubServerInfoSingleton.getInstance().getServerInfo();
		try {
			final HubIntRestService service = BuildHelper.getRestService(null, serverInfo.getServerUrl(),
					serverInfo.getUsername(), serverInfo.getPassword(), serverInfo.getTimeout());
			hubSupport.checkHubSupport(service, null);
		} catch (final Exception e) {
			return null;
		}
		return hubSupport;
	}

	public HubIntRestService getHubIntRestService(final HubJenkinsLogger logger, final HubServerInfo serverInfo)
			throws IOException, BDRestException, URISyntaxException, BDJenkinsHubPluginException,
			HubIntegrationException {
		return BuildHelper.getRestService(logger, serverInfo.getServerUrl(), serverInfo.getUsername(),
				serverInfo.getPassword(), serverInfo.getTimeout());
	}

	public void waitForBomToBeUpdated(final Node builtOn, final IntLogger logger, final BomUpToDateAction action,
			final HubIntRestService service, final HubSupportHelper supportHelper) throws BDJenkinsHubPluginException,
			InterruptedException, BDRestException, HubIntegrationException, URISyntaxException, IOException,
			ProjectDoesNotExistException, MissingUUIDException, UnexpectedHubResponseException {
		if (action.isHasBomBeenUdpated()) {
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

		final RemoteHubEventPolling hubEventPolling = new RemoteHubEventPolling(service, builtOn.getChannel());

		if (supportHelper.hasCapability(HubCapabilitiesEnum.CLI_STATUS_DIRECTORY_OPTION)) {
			hubEventPolling.assertBomUpToDate(reportGenInfo, logger);
		} else {
			hubEventPolling.assertBomUpToDate(reportGenInfo);
		}

	}
}
