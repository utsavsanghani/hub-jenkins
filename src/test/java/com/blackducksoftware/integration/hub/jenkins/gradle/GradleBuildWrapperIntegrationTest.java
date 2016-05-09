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
package com.blackducksoftware.integration.hub.jenkins.gradle;

import java.util.Properties;

public class GradleBuildWrapperIntegrationTest {

    private static String basePath;

    private static String testWorkspace;

    private static Properties testProperties;

    // @Rule
    // public static JenkinsRule j = new JenkinsRule();
    //
    // @Rule
    // public ExpectedException exception = ExpectedException.none();
    //
    // public void addHubServerInfo(HubServerInfo hubServerInfo) {
    // resetPublisherDescriptors();
    //
    // PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
    // descriptor.setHubServerInfo(hubServerInfo);
    // j.getInstance().getDescriptorList(Publisher.class).add(descriptor);
    // }
    //
    // public void resetPublisherDescriptors() {
    // while (Jenkins.getInstance().getDescriptorList(Publisher.class).size() != 0) {
    // Jenkins.getInstance().getDescriptorList(Publisher.class).remove(0);
    // }
    // }
    //
    // public void addGradleBuildWrapperDescriptor() {
    // GradleBuildWrapperDescriptor descriptor = new GradleBuildWrapperDescriptor();
    // j.getInstance().getDescriptorList(BuildWrapper.class).add(descriptor);
    // }
    //
    // public UsernamePasswordCredentialsImpl addCredentialToGlobalStore(String username, String password) {
    // UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null,
    // null,
    // username, password);
    // UserFacingAction store = new UserFacingAction();
    // try {
    // store.getStore().addCredentials(Domain.global(), credential);
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // return credential;
    // }
    //
    // @BeforeClass
    // public static void init() throws Exception {
    // basePath = ScanIntegrationTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    // basePath = basePath.substring(0, basePath.indexOf(File.separator + "target"));
    // basePath = basePath + File.separator + "test-workspace";
    // testWorkspace = basePath + File.separator + "gradleWorkspace";
    //
    // testProperties = new Properties();
    // ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    // InputStream is = classLoader.getResourceAsStream("test.properties");
    // try {
    // testProperties.load(is);
    // } catch (IOException e) {
    // System.err.println("reading test.properties failed!");
    // }
    // // p.load(new FileReader(new File("test.properties")));
    // System.out.println(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // System.out.println(testProperties.getProperty("TEST_USERNAME"));
    // System.out.println(testProperties.getProperty("TEST_PASSWORD"));
    // }
    //
    // @Test
    // public void testMavenProject() throws Exception {
    // Jenkins jenkins = j.jenkins;
    //
    // GradleBuildWrapper buildWrapper = new GradleBuildWrapper(null, false, null, null, null, null);
    //
    // Maven.MavenInstallation mvn = MavenSupport.getMavenInstallation();
    //
    // MavenModuleSet project = jenkins.createProject(MavenModuleSet.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // project.getDescriptor().getMavenDescriptor().setInstallations(mvn);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // MavenModuleSetBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput,
    // buildOutput.contains("Cannot run the Hub Gradle Build Wrapper for this type of Project."));
    //
    // }
    //
    // @Test
    // public void testRunNoBuilder() throws Exception {
    // Jenkins jenkins = j.jenkins;
    // addGradleBuildWrapperDescriptor();
    //
    // GradleBuildWrapper buildWrapper = new GradleBuildWrapper(null, false, null, null, null, null);
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("No Builder found for this job."));
    // assertTrue(buildOutput, buildOutput.contains("Will not run the Hub Gradle Build wrapper."));
    // }
    //
    // @Test
    // public void testRunMavenBuilder() throws Exception {
    // Jenkins jenkins = j.jenkins;
    // addGradleBuildWrapperDescriptor();
    //
    // GradleBuildWrapper buildWrapper = new GradleBuildWrapper(null, false, null, null, null, null);
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // MavenSupport.addMavenBuilder(project, null);
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("This Wrapper should be run with a Gradle Builder"));
    // assertTrue(buildOutput, buildOutput.contains("Will not run the Hub Gradle Build wrapper."));
    //
    // }
    //
    // @Test
    // public void testRunNotConfiguredNoGlobalConfig() throws Exception {
    // Jenkins jenkins = j.jenkins;
    // addGradleBuildWrapperDescriptor();
    //
    // GradleBuildWrapper buildWrapper = new GradleBuildWrapper(null, false, null, null, null, null);
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // Gradle builder = new Gradle("", "", "", testWorkspace, "", "", true, false, true, false);
    // project.getBuildersList().add(builder);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("Could not find the Hub global configuration!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project name configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project version configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Gradle configurations configured!"));
    // }
    //
    // @Test
    // public void testRunNotConfiguredEmptyGlobalConfig() throws Exception {
    // Jenkins jenkins = j.jenkins;
    // addGradleBuildWrapperDescriptor();
    //
    // addHubServerInfo(new HubServerInfo("", ""));
    //
    // GradleBuildWrapper buildWrapper = new GradleBuildWrapper(null, false, null, null, null, null);
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // Gradle builder = new Gradle("", "", "", testWorkspace, "", "", true, false, true, false);
    // project.getBuildersList().add(builder);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, !buildOutput.contains("Could not find the Hub global configuration!"));
    // assertTrue(buildOutput, buildOutput.contains("The Hub server URL is not configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub credentials configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project name configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project version configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Gradle configurations configured!"));
    // }
    //
    // @Test
    // public void testRunNotConfiguredEmptyCredentialsGlobalConfig() throws Exception {
    // Jenkins jenkins = j.jenkins;
    // addGradleBuildWrapperDescriptor();
    //
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore("", "");
    // addHubServerInfo(new HubServerInfo("http://server.com", credential.getId()));
    //
    // GradleBuildWrapper buildWrapper = new GradleBuildWrapper(null, false, null, null, null, null);
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // Gradle builder = new Gradle("", "", "", testWorkspace + File.separator + "GradleNoDependencies", "", "", true,
    // false, true, false);
    // project.getBuildersList().add(builder);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, !buildOutput.contains("Could not find the Hub global configuration!"));
    // assertTrue(buildOutput, !buildOutput.contains("The Hub server URL is not configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub username configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub password configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project name configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project version configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Gradle configurations configured!"));
    // }
    //
    // @Test
    // public void testRunConfiguredNoDependencies() throws Exception {
    // File buildInfo = new File(testWorkspace, "GradleNoDependencies");
    // buildInfo = new File(buildInfo, "build");
    // buildInfo = new File(buildInfo, "BlackDuck");
    // buildInfo = new File(buildInfo, "build-info.json");
    //
    // try {
    // Jenkins jenkins = j.jenkins;
    // addGradleBuildWrapperDescriptor();
    //
    // UsernamePasswordCredentialsImpl credential =
    // addCredentialToGlobalStore(testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // addHubServerInfo(new HubServerInfo(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId()));
    //
    // GradleBuildWrapper buildWrapper = new GradleBuildWrapper("Compile", false,
    // testProperties.getProperty("TEST_PROJECT"),
    // PhaseEnum.DEVELOPMENT.name(),
    // DistributionEnum.INTERNAL.name(), testProperties.getProperty("TEST_VERSION"));
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // Gradle builder = new Gradle("", "", "", testWorkspace + File.separator + "GradleNoDependencies", "", "", true,
    // false, true, false);
    // project.getBuildersList().add(builder);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("Will create build-info.json"));
    // assertTrue(buildOutput, buildOutput.contains("Finished: SUCCESS"));
    // assertTrue(buildOutput, buildInfo.exists());
    // } finally {
    // if (buildInfo.exists()) {
    // buildInfo.delete();
    // }
    // }
    // }
    //
    // @Test
    // public void testRunConfigured() throws Exception {
    // File buildInfo = new File(testWorkspace, "ValidGradleProject");
    // buildInfo = new File(buildInfo, "build");
    // buildInfo = new File(buildInfo, "BlackDuck");
    // buildInfo = new File(buildInfo, "build-info.json");
    //
    // try {
    // Jenkins jenkins = j.jenkins;
    // addGradleBuildWrapperDescriptor();
    //
    // UsernamePasswordCredentialsImpl credential =
    // addCredentialToGlobalStore(testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // addHubServerInfo(new HubServerInfo(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId()));
    //
    // GradleBuildWrapper buildWrapper = new GradleBuildWrapper("Compile", false,
    // testProperties.getProperty("TEST_PROJECT"),
    // PhaseEnum.DEVELOPMENT.name(),
    // DistributionEnum.INTERNAL.name(), testProperties.getProperty("TEST_VERSION"));
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // Gradle builder = new Gradle("", "", "", testWorkspace + File.separator + "ValidGradleProject", "", "", true,
    // false, true, false);
    // project.getBuildersList().add(builder);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("Will create build-info.json"));
    // assertTrue(buildOutput, buildOutput.contains("Finished: SUCCESS"));
    // assertTrue(buildInfo.exists());
    // } finally {
    // if (buildInfo.exists()) {
    // buildInfo.delete();
    // }
    // }
    // }

}
