/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.jenkins.maven;


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
