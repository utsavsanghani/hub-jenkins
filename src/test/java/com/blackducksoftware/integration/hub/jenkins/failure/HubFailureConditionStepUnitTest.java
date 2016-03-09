package com.blackducksoftware.integration.hub.jenkins.failure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.ArtifactArchiver;

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
import com.blackducksoftware.integration.hub.jenkins.action.HubScanFinishedAction;
import com.blackducksoftware.integration.hub.jenkins.mocks.TestBuild;
import com.blackducksoftware.integration.hub.jenkins.mocks.TestProject;
import com.blackducksoftware.integration.hub.jenkins.tests.utils.TestBuildListener;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatus;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatusCounts;
import com.blackducksoftware.integration.hub.policy.api.PolicyStatusEnum;
import com.blackducksoftware.integration.hub.response.ProjectItem;
import com.blackducksoftware.integration.hub.response.ReleaseItem;

public class HubFailureConditionStepUnitTest {

    @Rule
    public static JenkinsRule j = new JenkinsRule();

    private HubIntRestService getMockedService(String returnVersion, PolicyStatus status) throws Exception {
        HubIntRestService service = Mockito.mock(HubIntRestService.class);
        Mockito.doReturn(returnVersion).when(service).getHubVersion();
        Mockito.doReturn(status).when(service).getPolicyStatus(Mockito.anyString(), Mockito.anyString());

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
        Boolean failBuildForPolicyViolations = false;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        assertEquals(BuildStepMonitor.NONE, failureStep.getRequiredMonitorService());
    }

    @Test
    public void testGetDescriptor() {
        Boolean failBuildForPolicyViolations = false;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        assertNotNull(failureStep.getDescriptor());
    }

    @Test
    public void testPerformBuildNotSuccessful() throws Exception {
        Boolean failBuildForPolicyViolations = false;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);

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
        Boolean failBuildForPolicyViolations = false;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
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
        Boolean failBuildForPolicyViolations = false;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
        build.setResult(Result.SUCCESS);
        List<Publisher> publishers = new ArrayList<Publisher>();
        ArtifactArchiver publisher = new ArtifactArchiver("");
        publishers.add(publisher);
        project.setPublishersList(publishers);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        TestBuildListener listener = new TestBuildListener(stream);

        failureStep.perform(build, null, listener);

        String output = baos.toString();
        assertTrue(output, output.contains("Could not find the Hub Scan step for this Build."));
        assertEquals(Result.UNSTABLE, build.getResult());
    }

    @Test
    public void testPerformOutOfOrder() throws Exception {
        Boolean failBuildForPolicyViolations = false;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
        build.setResult(Result.SUCCESS);
        List<Publisher> publishers = new ArrayList<Publisher>();
        PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, null, null, null, null,
                false, null);
        publishers.add(hubScanStep);
        project.setPublishersList(publishers);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        TestBuildListener listener = new TestBuildListener(stream);

        failureStep.perform(build, null, listener);

        String output = baos.toString();
        assertTrue(output, output.contains("The scan must be configured to run before the Failure Conditions."));
        assertEquals(Result.UNSTABLE, build.getResult());

    }

    @Test
    public void testPerformNoFailureConditionSet() throws Exception {
        Boolean failBuildForPolicyViolations = false;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
        build.setResult(Result.SUCCESS);
        List<Publisher> publishers = new ArrayList<Publisher>();
        PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, null, null, null, null,
                false, null);
        publishers.add(hubScanStep);
        project.setPublishersList(publishers);
        build.setAction(new HubScanFinishedAction());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        TestBuildListener listener = new TestBuildListener(stream);

        failureStep.perform(build, null, listener);

        String output = baos.toString();
        assertTrue(output, output.contains("The Hub failure condition step has not been configured to do anything."));
        assertEquals(Result.UNSTABLE, build.getResult());
    }

    @Test
    public void testPerformNullProject() throws Exception {
        Boolean failBuildForPolicyViolations = true;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
        failureStep = Mockito.spy(failureStep);
        HubIntRestService service = getMockedService("1.0.0", null);
        Mockito.doReturn(null).when(service).getProjectByName(Mockito.anyString());

        HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

        Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
        build.setResult(Result.SUCCESS);
        List<Publisher> publishers = new ArrayList<Publisher>();
        PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, null, null, null, null,
                false, null);
        publishers.add(hubScanStep);
        project.setPublishersList(publishers);
        build.setAction(new HubScanFinishedAction());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        TestBuildListener listener = new TestBuildListener(stream);

        failureStep.perform(build, null, listener);

        String output = baos.toString();
        assertTrue(output, output.contains("Could not find the specified Hub Project."));
        assertEquals(Result.UNSTABLE, build.getResult());
    }

    @Test
    public void testPerformNoProjectId() throws Exception {
        Boolean failBuildForPolicyViolations = true;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
        failureStep = Mockito.spy(failureStep);
        HubIntRestService service = getMockedService("1.0.0", null);
        ProjectItem projectItem = new ProjectItem();
        projectItem.setId(null);
        Mockito.doReturn(projectItem).when(service).getProjectByName(Mockito.anyString());

        HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

        Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
        build.setResult(Result.SUCCESS);
        List<Publisher> publishers = new ArrayList<Publisher>();
        PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, null, null, null, null,
                false, null);
        publishers.add(hubScanStep);
        project.setPublishersList(publishers);
        build.setAction(new HubScanFinishedAction());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        TestBuildListener listener = new TestBuildListener(stream);

        failureStep.perform(build, null, listener);

        String output = baos.toString();
        assertTrue(output, output.contains("Could not find the specified Hub Project."));
        assertEquals(Result.UNSTABLE, build.getResult());
    }

    @Test
    public void testPerformNullVersions() throws Exception {
        Boolean failBuildForPolicyViolations = true;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
        failureStep = Mockito.spy(failureStep);
        HubIntRestService service = getMockedService("1.0.0", null);
        ProjectItem projectItem = new ProjectItem();
        projectItem.setId("ProjectId");
        Mockito.doReturn(projectItem).when(service).getProjectByName(Mockito.anyString());
        Mockito.doReturn(null).when(service).getVersionsForProject(Mockito.anyString());

        HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

        Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
        build.setResult(Result.SUCCESS);
        List<Publisher> publishers = new ArrayList<Publisher>();
        PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, "VerisonName", null, null, null,
                false, null);
        publishers.add(hubScanStep);
        project.setPublishersList(publishers);
        build.setAction(new HubScanFinishedAction());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        TestBuildListener listener = new TestBuildListener(stream);

        failureStep.perform(build, null, listener);

        String output = baos.toString();
        assertTrue(output, output.contains("Could not find the specified Version for this Hub Project."));
        assertEquals(Result.UNSTABLE, build.getResult());
    }

    @Test
    public void testPerformNoVersionId() throws Exception {
        Boolean failBuildForPolicyViolations = true;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
        failureStep = Mockito.spy(failureStep);
        HubIntRestService service = getMockedService("1.0.0", null);
        ProjectItem projectItem = new ProjectItem();
        projectItem.setId("ProjectId");
        Mockito.doReturn(projectItem).when(service).getProjectByName(Mockito.anyString());
        List<ReleaseItem> projectVersions = new ArrayList<ReleaseItem>();
        ReleaseItem releaseItem = new ReleaseItem();
        releaseItem.setId("VersionId");
        releaseItem.setVersion("FakeVersion");
        projectVersions.add(releaseItem);
        Mockito.doReturn(projectVersions).when(service).getVersionsForProject(Mockito.anyString());

        HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

        Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
        build.setResult(Result.SUCCESS);
        List<Publisher> publishers = new ArrayList<Publisher>();
        PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, "VerisonName", null, null, null,
                false, null);
        publishers.add(hubScanStep);
        project.setPublishersList(publishers);
        build.setAction(new HubScanFinishedAction());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        TestBuildListener listener = new TestBuildListener(stream);

        failureStep.perform(build, null, listener);

        String output = baos.toString();
        assertTrue(output, output.contains("Could not find the specified Version for this Hub Project."));
        assertEquals(Result.UNSTABLE, build.getResult());
    }

    @Test
    public void testPerformPoliciesNotSupported() throws Exception {
        Boolean failBuildForPolicyViolations = true;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
        HubFailureConditionStepDescriptor descriptor = failureStep.getDescriptor();
        failureStep = Mockito.spy(failureStep);
        HubIntRestService service = getMockedService("1.0.0", null);
        ProjectItem projectItem = new ProjectItem();
        projectItem.setId("ProjectId");
        Mockito.doReturn(projectItem).when(service).getProjectByName(Mockito.anyString());
        List<ReleaseItem> projectVersions = new ArrayList<ReleaseItem>();
        ReleaseItem releaseItem = new ReleaseItem();
        releaseItem.setId("VersionId");
        releaseItem.setVersion("VerisonName");
        projectVersions.add(releaseItem);
        Mockito.doReturn(projectVersions).when(service).getVersionsForProject(Mockito.anyString());

        HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

        descriptor = Mockito.spy(descriptor);

        HubSupportHelper hubSupport = new HubSupportHelper();
        hubSupport.checkHubSupport(service, null);

        Mockito.doReturn(hubSupport).when(descriptor).getCheckedHubSupportHelper();
        Mockito.doReturn(descriptor).when(failureStep).getDescriptor();
        Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
        build.setResult(Result.SUCCESS);
        List<Publisher> publishers = new ArrayList<Publisher>();
        PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, "VerisonName", null, null, null,
                false, null);
        publishers.add(hubScanStep);
        project.setPublishersList(publishers);
        build.setAction(new HubScanFinishedAction());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        TestBuildListener listener = new TestBuildListener(stream);

        failureStep.perform(build, null, listener);

        String output = baos.toString();
        assertTrue(output, output.contains("This version of the Hub does not have support for Policies."));
        assertEquals(Result.UNSTABLE, build.getResult());
    }

    @Test
    public void testPerformValidNoViolation() throws Exception {
        Boolean failBuildForPolicyViolations = true;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
        HubFailureConditionStepDescriptor descriptor = failureStep.getDescriptor();
        failureStep = Mockito.spy(failureStep);
        PolicyStatusCounts counts = new PolicyStatusCounts("0", "12", "45");
        PolicyStatus policyStatus = new PolicyStatus(PolicyStatusEnum.NOT_IN_VIOLATION.name(), null, counts, null);
        HubIntRestService service = getMockedService("3.0.0", policyStatus);
        ProjectItem projectItem = new ProjectItem();
        projectItem.setId("ProjectId");
        Mockito.doReturn(projectItem).when(service).getProjectByName(Mockito.anyString());
        List<ReleaseItem> projectVersions = new ArrayList<ReleaseItem>();
        ReleaseItem releaseItem = new ReleaseItem();
        releaseItem.setId("VersionId");
        releaseItem.setVersion("VerisonName");
        projectVersions.add(releaseItem);
        Mockito.doReturn(projectVersions).when(service).getVersionsForProject(Mockito.anyString());
        HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

        descriptor = Mockito.spy(descriptor);

        HubSupportHelper hubSupport = new HubSupportHelper();
        hubSupport.checkHubSupport(service, null);

        Mockito.doReturn(hubSupport).when(descriptor).getCheckedHubSupportHelper();
        Mockito.doReturn(descriptor).when(failureStep).getDescriptor();
        Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
        build.setResult(Result.SUCCESS);
        List<Publisher> publishers = new ArrayList<Publisher>();
        PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, "VerisonName", null, null, null,
                false, null);
        publishers.add(hubScanStep);
        project.setPublishersList(publishers);
        build.setAction(new HubScanFinishedAction());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        TestBuildListener listener = new TestBuildListener(stream);

        failureStep.perform(build, null, listener);

        String output = baos.toString();
        assertTrue(output,
                output.contains("Found " + policyStatus.getStatusCounts().getIN_VIOLATION() + " bom entries to be In Violation of a defined Policy."));
        assertTrue(output, output.contains("Found " + policyStatus.getStatusCounts().getIN_VIOLATION_OVERRIDDEN()
                + " bom entries to be In Violation of a defined Policy, but they have been manually overridden."));
        assertTrue(output,
                output.contains("Found " + policyStatus.getStatusCounts().getNOT_IN_VIOLATION() + " bom entries to be Not In Violation of a defined Policy."));
        assertEquals(Result.SUCCESS, build.getResult());
    }

    @Test
    public void testPerformValidWithViolations() throws Exception {
        Boolean failBuildForPolicyViolations = true;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);
        HubFailureConditionStepDescriptor descriptor = failureStep.getDescriptor();
        failureStep = Mockito.spy(failureStep);
        PolicyStatusCounts counts = new PolicyStatusCounts("3", "12", "45");
        PolicyStatus policyStatus = new PolicyStatus(PolicyStatusEnum.IN_VIOLATION.name(), null, counts, null);
        HubIntRestService service = getMockedService("3.0.0", policyStatus);
        ProjectItem projectItem = new ProjectItem();
        projectItem.setId("ProjectId");
        Mockito.doReturn(projectItem).when(service).getProjectByName(Mockito.anyString());
        List<ReleaseItem> projectVersions = new ArrayList<ReleaseItem>();
        ReleaseItem releaseItem = new ReleaseItem();
        releaseItem.setId("VersionId");
        releaseItem.setVersion("VerisonName");
        projectVersions.add(releaseItem);
        Mockito.doReturn(projectVersions).when(service).getVersionsForProject(Mockito.anyString());

        HubServerInfo serverInfo = new HubServerInfo("Fake Server", "Fake Creds", 499);
        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

        descriptor = Mockito.spy(descriptor);

        HubSupportHelper hubSupport = new HubSupportHelper();
        hubSupport.checkHubSupport(service, null);

        Mockito.doReturn(hubSupport).when(descriptor).getCheckedHubSupportHelper();
        Mockito.doReturn(descriptor).when(failureStep).getDescriptor();
        Mockito.doReturn(service).when(failureStep).getHubIntRestService(Mockito.any(HubJenkinsLogger.class), Mockito.any(HubServerInfo.class));

        TestProject project = new TestProject(j.getInstance(), "Test Project");
        TestBuild build = new TestBuild(project);
        build.setResult(Result.SUCCESS);
        List<Publisher> publishers = new ArrayList<Publisher>();
        PostBuildHubScan hubScanStep = new PostBuildHubScan(null, false, null, "VerisonName", null, null, null,
                false, null);
        publishers.add(hubScanStep);
        project.setPublishersList(publishers);
        build.setAction(new HubScanFinishedAction());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);
        TestBuildListener listener = new TestBuildListener(stream);

        failureStep.perform(build, null, listener);

        String output = baos.toString();
        assertTrue(output,
                output.contains("Found " + policyStatus.getStatusCounts().getIN_VIOLATION() + " bom entries to be In Violation of a defined Policy."));
        assertTrue(output, output.contains("Found " + policyStatus.getStatusCounts().getIN_VIOLATION_OVERRIDDEN()
                + " bom entries to be In Violation of a defined Policy, but they have been manually overridden."));
        assertTrue(output,
                output.contains("Found " + policyStatus.getStatusCounts().getNOT_IN_VIOLATION() + " bom entries to be Not In Violation of a defined Policy."));
        assertEquals(Result.FAILURE, build.getResult());
    }
}
