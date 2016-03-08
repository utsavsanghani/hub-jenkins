package com.blackducksoftware.integration.hub.jenkins.failure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import com.blackducksoftware.integration.hub.jenkins.mocks.TestBuild;
import com.blackducksoftware.integration.hub.jenkins.mocks.TestProject;
import com.blackducksoftware.integration.hub.jenkins.tests.utils.TestBuildListener;

public class HubFailureConditionStepUnitTest {

    @Rule
    public static JenkinsRule j = new JenkinsRule();

    @WithoutJenkins
    @Test
    public void testConstructor() {
        Boolean failBuildForPolicyViolations = true;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        assertEquals(failBuildForPolicyViolations, failureStep.getFailBuildForPolicyViolations());

        failBuildForPolicyViolations = false;
        failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        assertEquals(failBuildForPolicyViolations, failureStep.getFailBuildForPolicyViolations());
    }

    @WithoutJenkins
    @Test
    public void testGetRequiredMonitorService() {
        Boolean failBuildForPolicyViolations = true;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        assertEquals(BuildStepMonitor.NONE, failureStep.getRequiredMonitorService());
    }

    @Test
    public void testGetDescriptor() {
        Boolean failBuildForPolicyViolations = true;
        HubFailureConditionStep failureStep = new HubFailureConditionStep(failBuildForPolicyViolations);

        assertNotNull(failureStep.getDescriptor());
    }

    @Test
    public void testPerformBuildNotSuccessful() throws Exception {
        Boolean failBuildForPolicyViolations = true;
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
    }
}
