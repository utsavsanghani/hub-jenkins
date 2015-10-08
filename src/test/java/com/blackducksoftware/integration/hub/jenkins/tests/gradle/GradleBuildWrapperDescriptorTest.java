package com.blackducksoftware.integration.hub.jenkins.tests.gradle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import hudson.maven.MavenModuleSet;
import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.gradle.GradleBuildWrapperDescriptor;

public class GradleBuildWrapperDescriptorTest {

    @Rule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void testIsApplicable() throws Exception {
        GradleBuildWrapperDescriptor descriptor = new GradleBuildWrapperDescriptor();
        FreeStyleProject project = j.createFreeStyleProject("test");
        assertTrue(descriptor.isApplicable(project));
        MavenModuleSet mavenProject = j.createMavenProject();
        assertTrue(!descriptor.isApplicable(project));
    }

    @Test
    public void testGetDisplayName() throws Exception {
        GradleBuildWrapperDescriptor descriptor = new GradleBuildWrapperDescriptor();
        assertEquals(Messages.HubMavenWrapper_getDisplayName(), descriptor.getDisplayName());
    }

    @Test
    public void testDoCheckUserScopesToInclude() throws Exception {
        GradleBuildWrapperDescriptor descriptor = new GradleBuildWrapperDescriptor();
        assertEquals(Messages.HubGradleWrapper_getPleaseIncludeAConfiguration(), descriptor.doCheckUserScopesToInclude("").getMessage());
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckUserScopesToInclude("what").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckUserScopesToInclude("compile, runtime").kind);
    }
}
