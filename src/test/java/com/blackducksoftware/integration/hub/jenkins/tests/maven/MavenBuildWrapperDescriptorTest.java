package com.blackducksoftware.integration.hub.jenkins.tests.maven;


public class MavenBuildWrapperDescriptorTest {

    // @Rule
    // public static JenkinsRule j = new JenkinsRule();
    //
    // @Test
    // public void testIsApplicable() throws Exception {
    // MavenBuildWrapperDescriptor descriptor = new MavenBuildWrapperDescriptor();
    // FreeStyleProject project = j.createFreeStyleProject("test");
    // assertTrue(descriptor.isApplicable(project));
    // MavenModuleSet mavenProject = j.createMavenProject();
    // assertTrue(!descriptor.isApplicable(mavenProject));
    // }
    //
    // @Test
    // public void testGetDisplayName() throws Exception {
    // MavenBuildWrapperDescriptor descriptor = new MavenBuildWrapperDescriptor();
    // assertEquals(Messages.HubMavenWrapper_getDisplayName(), descriptor.getDisplayName());
    // }
    //
    // @Test
    // public void testDoCheckUserScopesToInclude() throws Exception {
    // MavenBuildWrapperDescriptor descriptor = new MavenBuildWrapperDescriptor();
    // assertEquals(Messages.HubMavenWrapper_getPleaseIncludeAScope(),
    // descriptor.doCheckUserScopesToInclude("").getMessage());
    // assertEquals(Messages.HubMavenWrapper_getIncludedInvalidScope_0_("what"),
    // descriptor.doCheckUserScopesToInclude("what").getMessage());
    // assertEquals(FormValidation.Kind.OK, descriptor.doCheckUserScopesToInclude("compile, test").kind);
    // }
}
