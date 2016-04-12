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
