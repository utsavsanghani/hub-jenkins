package com.blackducksoftware.integration.hub.jenkins.tests.gradle;


public class GradleBuildWrapperDescriptorTest {

    // @Rule
    // public static JenkinsRule j = new JenkinsRule();
    //
    // @Test
    // public void testIsApplicable() throws Exception {
    // GradleBuildWrapperDescriptor descriptor = new GradleBuildWrapperDescriptor();
    // FreeStyleProject project = j.createFreeStyleProject("test");
    // assertTrue(descriptor.isApplicable(project));
    // MavenModuleSet mavenProject = j.createMavenProject();
    // assertTrue(!descriptor.isApplicable(mavenProject));
    // }
    //
    // @Test
    // public void testGetDisplayName() throws Exception {
    // GradleBuildWrapperDescriptor descriptor = new GradleBuildWrapperDescriptor();
    // assertEquals(Messages.HubGradleWrapper_getDisplayName(), descriptor.getDisplayName());
    // }
    //
    // @Test
    // public void testDoCheckUserScopesToInclude() throws Exception {
    // GradleBuildWrapperDescriptor descriptor = new GradleBuildWrapperDescriptor();
    // assertEquals(Messages.HubGradleWrapper_getPleaseIncludeAConfiguration(),
    // descriptor.doCheckUserScopesToInclude("").getMessage());
    // assertEquals(FormValidation.Kind.OK, descriptor.doCheckUserScopesToInclude("what").kind);
    // assertEquals(FormValidation.Kind.OK, descriptor.doCheckUserScopesToInclude("compile, runtime").kind);
    // }
}
