package com.blackducksoftware.integration.hub.jenkins.failure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mockito;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubScan;
import com.blackducksoftware.integration.hub.jenkins.action.BomUpToDateAction;
import com.blackducksoftware.integration.hub.jenkins.action.HubScanFinishedAction;
import com.blackducksoftware.integration.hub.jenkins.mocks.TestBuild;
import com.blackducksoftware.integration.hub.jenkins.mocks.TestProject;
import com.blackducksoftware.integration.hub.jenkins.tests.utils.TestBuildListener;
import com.blackducksoftware.integration.hub.policy.api.ComponentVersionStatusCount;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatus;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatusEnum;
import com.blackducksoftware.integration.hub.project.api.ProjectItem;
import com.blackducksoftware.integration.hub.version.api.ReleaseItem;

import hudson.model.Result;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;

public class HubFailureConditionStepUnitTest {

	@Rule
	public static JenkinsRule j = new JenkinsRule();

	private HubIntRestService getMockedService(final String returnVersion, final PolicyStatus status) throws Exception {
		final HubIntRestService service = Mockito.mock(HubIntRestService.class);
		Mockito.doReturn(returnVersion).when(service).getHubVersion();
		Mockito.doReturn(status).when(service).getPolicyStatus(Mockito.anyString());

		return service;
	}

	@WithoutJenkins
	@Test
	public void testConstructor() {
		Boolean failBuildForPolicyViolations = false;
		HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

		assertEquals(failBuildForPolicyViolations, failureStep.getFailBuildForPolicyViolations());

		failBuildForPolicyViolations = true;
		failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

		assertEquals(failBuildForPolicyViolations, failureStep.getFailBuildForPolicyViolations());
	}

	@WithoutJenkins
	@Test
	public void testGetRequiredMonitorService() {
		final Boolean failBuildForPolicyViolations = false;
		final HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

		assertEquals(BuildStepMonitor.NONE, failureStep.getRequiredMonitorService());
	}

	@Test
	public void testGetDescriptor() {
		final Boolean failBuildForPolicyViolations = false;
		final HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

		assertNotNull(failureStep.getDescriptor());
	}

	@Test
	public void testPerformBuildNotSuccessful() throws Exception {
		final Boolean failBuildForPolicyViolations = false;
		final HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

		final TestProject project = new TestProject(j.getInstance(), "Test Project");
		final TestBuild build = new TestBuild(project);

		// Test Build Unstable
		build.setResult(Result.UNSTABLE);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(baos);
		TestBuildListener listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		String output = baos.toString();
		assertTrue(output, output.contains("The Build did not run sucessfully, will not check the Hub Failure Conditions."));

		// Test Build Failure
		build.setResult(Result.FAILURE);

		baos = new ByteArrayOutputStream();
		stream = new PrintStream(baos);
		listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		output = baos.toString();
		assertTrue(output, output.contains("The Build did not run sucessfully, will not check the Hub Failure Conditions."));

		// Test Aborted
		build.setResult(Result.ABORTED);

		baos = new ByteArrayOutputStream();
		stream = new PrintStream(baos);
		listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		output = baos.toString();
		assertTrue(output, output.contains("The Build did not run sucessfully, will not check the Hub Failure Conditions."));

		// Test NotBuilt
		build.setResult(Result.NOT_BUILT);

		baos = new ByteArrayOutputStream();
		stream = new PrintStream(baos);
		listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		output = baos.toString();
		assertTrue(output, output.contains("The Build did not run sucessfully, will not check the Hub Failure Conditions."));
	}

	@Test
	public void testPerformNoPublishers() throws Exception {
		final Boolean failBuildForPolicyViolations = false;
		final HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

		final TestProject project = new TestProject(j.getInstance(), "Test Project");
		final TestBuild build = new TestBuild(project);
		build.setResult(Result.SUCCESS);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(baos);
		TestBuildListener listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		String output = baos.toString();
		assertTrue(output, output.contains("Could not find the Hub Scan step for this Build."));
		assertEquals(Result.UNSTABLE, build.getResult());

		build.setResult(Result.SUCCESS);

		project.setPublishersList(new ArrayList<Publisher>());

		baos = new ByteArrayOutputStream();
		stream = new PrintStream(baos);
		listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		output = baos.toString();
		assertTrue(output, output.contains("Could not find the Hub Scan step for this Build."));
		assertEquals(Result.UNSTABLE, build.getResult());
	}

	@Test
	public void testPerformNoHubScanStep() throws Exception {
		final Boolean failBuildForPolicyViolations = false;
		final HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

		final TestProject project = new TestProject(j.getInstance(), "Test Project");
		final TestBuild build = new TestBuild(project);
		build.setResult(Result.SUCCESS);
		final List<Publisher> publishers = new ArrayList<Publisher>();
		final ArtifactArchiver publisher = new ArtifactArchiver("");
		publishers.add(publisher);
		project.setPublishersList(publishers);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream stream = new PrintStream(baos);
		final TestBuildListener listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		final String output = baos.toString();
		assertTrue(output, output.contains("Could not find the Hub Scan step for this Build."));
		assertEquals(Result.UNSTABLE, build.getResult());
	}

	@Test
	public void testPerformOutOfOrder() throws Exception {
		final Boolean failBuildForPolicyViolations = false;
		final HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

		final TestProject project = new TestProject(j.getInstance(), "Test Project");
		final TestBuild build = new TestBuild(project);
		build.setResult(Result.SUCCESS);
		final List<Publisher> publishers = new ArrayList<Publisher>();
		final PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, null, null, null, null,
				false, null);
		publishers.add(hubScanStep);
		project.setPublishersList(publishers);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream stream = new PrintStream(baos);
		final TestBuildListener listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		final String output = baos.toString();
		assertTrue(output, output.contains("The scan must be configured to run before the Failure Conditions."));
		assertEquals(Result.UNSTABLE, build.getResult());

	}

	@Test
	public void testPerformNoFailureConditionSet() throws Exception {
		final Boolean failBuildForPolicyViolations = false;
		final HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

		final TestProject project = new TestProject(j.getInstance(), "Test Project");
		final TestBuild build = new TestBuild(project);
		build.setResult(Result.SUCCESS);
		final List<Publisher> publishers = new ArrayList<Publisher>();
		final PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, null, null, null, null,
				false, null);
		publishers.add(hubScanStep);
		project.setPublishersList(publishers);
		build.setScanFinishedAction(new HubScanFinishedAction());
		final BomUpToDateAction bomUpdatedAction = new BomUpToDateAction();
		bomUpdatedAction.setHasBomBeenUdpated(true);
		build.setBomUpdatedAction(bomUpdatedAction);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream stream = new PrintStream(baos);
		final TestBuildListener listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		final String output = baos.toString();
		assertTrue(output, output.contains("The Hub failure condition step has not been configured to do anything."));
		assertEquals(Result.UNSTABLE, build.getResult());
	}


	@Test
	public void testPerformPoliciesNotSupported() throws Exception {
		final Boolean failBuildForPolicyViolations = true;
		HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
		HubFailureConditionStepDescriptor descriptor = failureStep.getDescriptor();
		failureStep = Mockito.spy(failureStep);
		final HubIntRestService service = getMockedService("1.0.0", null);
		final ProjectItem projectItem = new ProjectItem(null, null, null);
		Mockito.doReturn(projectItem).when(service).getProjectByName(Mockito.anyString());
		final ReleaseItem releaseItem = new ReleaseItem(null, null, null, null, null);
		Mockito.doReturn(releaseItem).when(service).getVersion(Mockito.any(ProjectItem.class), Mockito.anyString());

		final HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

		final HubSupportHelper hubSupport = new HubSupportHelper();
		hubSupport.checkHubSupport(service, null);

		descriptor = Mockito.spy(descriptor);

		Mockito.doReturn(hubSupport).when(descriptor).getCheckedHubSupportHelper();
		Mockito.doReturn(descriptor).when(failureStep).getDescriptor();
		Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

		final TestProject project = new TestProject(j.getInstance(), "Test Project");
		final TestBuild build = new TestBuild(project);
		build.setResult(Result.SUCCESS);
		final List<Publisher> publishers = new ArrayList<Publisher>();
		final PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, "VerisonName", null, null, null,
				false, null);
		publishers.add(hubScanStep);
		project.setPublishersList(publishers);
		build.setScanFinishedAction(new HubScanFinishedAction());
		final BomUpToDateAction bomUpdatedAction = new BomUpToDateAction();
		bomUpdatedAction.setHasBomBeenUdpated(true);
		build.setBomUpdatedAction(bomUpdatedAction);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream stream = new PrintStream(baos);
		final TestBuildListener listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		final String output = baos.toString();
		assertTrue(output, output.contains("This version of the Hub does not have support for Policies."));
		assertEquals(Result.UNSTABLE, build.getResult());
	}

	@Test
	public void testPerformValidUnknownCounts() throws Exception {
		final Boolean failBuildForPolicyViolations = true;
		HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
		HubFailureConditionStepDescriptor descriptor = failureStep.getDescriptor();
		failureStep = Mockito.spy(failureStep);
		final ComponentVersionStatusCount countsUnknown = new ComponentVersionStatusCount(PolicyStatusEnum.UNKNOWN.name(), 0);
		final List<ComponentVersionStatusCount> counts = new ArrayList<ComponentVersionStatusCount>();
		counts.add(countsUnknown);

		final PolicyStatus policyStatus = new PolicyStatus(PolicyStatusEnum.NOT_IN_VIOLATION.name(), null, counts, null);
		final HubIntRestService service = getMockedService("3.0.0", policyStatus);
		final ProjectItem projectItem = new ProjectItem(null, null, null);
		Mockito.doReturn(projectItem).when(service).getProjectByName(Mockito.anyString());
		final ReleaseItem releaseItem = new ReleaseItem(null, null, null, null, null);
		Mockito.doReturn(releaseItem).when(service).getVersion(Mockito.any(ProjectItem.class), Mockito.anyString());

		final HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

		final HubSupportHelper hubSupport = new HubSupportHelper();
		hubSupport.checkHubSupport(service, null);

		descriptor = Mockito.spy(descriptor);

		Mockito.doReturn(hubSupport).when(descriptor).getCheckedHubSupportHelper();
		Mockito.doReturn(descriptor).when(failureStep).getDescriptor();
		Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

		final TestProject project = new TestProject(j.getInstance(), "Test Project");
		final TestBuild build = new TestBuild(project);
		build.setResult(Result.SUCCESS);
		final List<Publisher> publishers = new ArrayList<Publisher>();
		final PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, "VerisonName", null, null, null,
				false, null);
		publishers.add(hubScanStep);
		project.setPublishersList(publishers);
		build.setScanFinishedAction(new HubScanFinishedAction());
		final BomUpToDateAction bomUpdatedAction = new BomUpToDateAction();
		bomUpdatedAction.setHasBomBeenUdpated(true);
		build.setBomUpdatedAction(bomUpdatedAction);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream stream = new PrintStream(baos);
		final TestBuildListener listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		final String output = baos.toString();
		assertTrue(output,
				output.contains("Could not find the number of bom entries In Violation of a Policy."));
		assertTrue(output, output.contains("Could not find the number of bom entries In Violation Overridden of a Policy."));
		assertTrue(output,
				output.contains("Could not find the number of bom entries Not In Violation of a Policy."));
		assertEquals(Result.SUCCESS, build.getResult());
	}

	@Test
	public void testPerformValidNoViolation() throws Exception {
		final Boolean failBuildForPolicyViolations = true;
		HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
		HubFailureConditionStepDescriptor descriptor = failureStep.getDescriptor();
		failureStep = Mockito.spy(failureStep);
		final ComponentVersionStatusCount countsInViolation = new ComponentVersionStatusCount(PolicyStatusEnum.IN_VIOLATION.name(), 0);
		final ComponentVersionStatusCount countsNotInViolation = new ComponentVersionStatusCount(PolicyStatusEnum.IN_VIOLATION_OVERRIDDEN.name(), 12);
		final ComponentVersionStatusCount countsInViolationOverridden = new ComponentVersionStatusCount(PolicyStatusEnum.NOT_IN_VIOLATION.name(), 45);
		final List<ComponentVersionStatusCount> counts = new ArrayList<ComponentVersionStatusCount>();
		counts.add(countsInViolationOverridden);
		counts.add(countsInViolation);
		counts.add(countsNotInViolation);

		final PolicyStatus policyStatus = new PolicyStatus(PolicyStatusEnum.NOT_IN_VIOLATION.name(), null, counts, null);
		final HubIntRestService service = getMockedService("3.0.0", policyStatus);
		final ProjectItem projectItem = new ProjectItem(null, null, null);
		Mockito.doReturn(projectItem).when(service).getProjectByName(Mockito.anyString());
		final ReleaseItem releaseItem = new ReleaseItem(null, null, null, null, null);
		Mockito.doReturn(releaseItem).when(service).getVersion(Mockito.any(ProjectItem.class), Mockito.anyString());
		final HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

		final HubSupportHelper hubSupport = new HubSupportHelper();
		hubSupport.checkHubSupport(service, null);

		descriptor = Mockito.spy(descriptor);

		Mockito.doReturn(hubSupport).when(descriptor).getCheckedHubSupportHelper();
		Mockito.doReturn(descriptor).when(failureStep).getDescriptor();
		Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

		final TestProject project = new TestProject(j.getInstance(), "Test Project");
		final TestBuild build = new TestBuild(project);
		build.setResult(Result.SUCCESS);
		final List<Publisher> publishers = new ArrayList<Publisher>();
		final PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, "VerisonName", null, null, null,
				false, null);
		publishers.add(hubScanStep);
		project.setPublishersList(publishers);
		build.setScanFinishedAction(new HubScanFinishedAction());
		final BomUpToDateAction bomUpdatedAction = new BomUpToDateAction();
		bomUpdatedAction.setHasBomBeenUdpated(true);
		build.setBomUpdatedAction(bomUpdatedAction);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream stream = new PrintStream(baos);
		final TestBuildListener listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		final String output = baos.toString();
		assertTrue(output,
				output.contains("Found " + policyStatus.getCountInViolation().getValue() + " bom entries to be In Violation of a defined Policy."));
		assertTrue(output, output.contains("Found " + policyStatus.getCountInViolationOverridden().getValue()
				+ " bom entries to be In Violation of a defined Policy, but they have been overridden."));
		assertTrue(output,
				output.contains("Found " + policyStatus.getCountNotInViolation().getValue() + " bom entries to be Not In Violation of a defined Policy."));
		assertEquals(Result.SUCCESS, build.getResult());
	}

	@Test
	public void testPerformValidWithViolations() throws Exception {
		final Boolean failBuildForPolicyViolations = true;
		HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
		HubFailureConditionStepDescriptor descriptor = failureStep.getDescriptor();
		failureStep = Mockito.spy(failureStep);

		final ComponentVersionStatusCount countsInViolation = new ComponentVersionStatusCount(PolicyStatusEnum.IN_VIOLATION.name(), 3);
		final ComponentVersionStatusCount countsNotInViolation = new ComponentVersionStatusCount(PolicyStatusEnum.IN_VIOLATION_OVERRIDDEN.name(), 12);
		final ComponentVersionStatusCount countsInViolationOverridden = new ComponentVersionStatusCount(PolicyStatusEnum.NOT_IN_VIOLATION.name(), 45);
		final List<ComponentVersionStatusCount> counts = new ArrayList<ComponentVersionStatusCount>();
		counts.add(countsInViolationOverridden);
		counts.add(countsInViolation);
		counts.add(countsNotInViolation);
		final PolicyStatus policyStatus = new PolicyStatus(PolicyStatusEnum.IN_VIOLATION.name(), null, counts, null);
		final HubIntRestService service = getMockedService("3.0.0", policyStatus);
		final ProjectItem projectItem = new ProjectItem(null, null, null);
		Mockito.doReturn(projectItem).when(service).getProjectByName(Mockito.anyString());
		final ReleaseItem releaseItem = new ReleaseItem(null, null, null, null, null);
		Mockito.doReturn(releaseItem).when(service).getVersion(Mockito.any(ProjectItem.class), Mockito.anyString());

		final HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

		final HubSupportHelper hubSupport = new HubSupportHelper();
		hubSupport.checkHubSupport(service, null);

		descriptor = Mockito.spy(descriptor);

		Mockito.doReturn(hubSupport).when(descriptor).getCheckedHubSupportHelper();
		Mockito.doReturn(descriptor).when(failureStep).getDescriptor();
		Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

		final TestProject project = new TestProject(j.getInstance(), "Test Project");
		final TestBuild build = new TestBuild(project);
		build.setResult(Result.SUCCESS);
		final List<Publisher> publishers = new ArrayList<Publisher>();
		final PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, "VerisonName", null, null, null,
				false, null);
		publishers.add(hubScanStep);
		project.setPublishersList(publishers);
		build.setScanFinishedAction(new HubScanFinishedAction());
		final BomUpToDateAction bomUpdatedAction = new BomUpToDateAction();
		bomUpdatedAction.setHasBomBeenUdpated(true);
		build.setBomUpdatedAction(bomUpdatedAction);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream stream = new PrintStream(baos);
		final TestBuildListener listener = new TestBuildListener(stream);

		failureStep.perform(build, null, listener);

		final String output = baos.toString();
		assertTrue(output,
				output.contains("Found " + policyStatus.getCountInViolation().getValue() + " bom entries to be In Violation of a defined Policy."));
		assertTrue(output, output.contains("Found " + policyStatus.getCountInViolationOverridden().getValue()
				+ " bom entries to be In Violation of a defined Policy, but they have been overridden."));
		assertTrue(output,
				output.contains("Found " + policyStatus.getCountNotInViolation().getValue() + " bom entries to be Not In Violation of a defined Policy."));
		assertEquals(Result.FAILURE, build.getResult());
	}
}
