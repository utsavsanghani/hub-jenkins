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
package com.blackducksoftware.integration.hub.jenkins.tests;

import java.util.Properties;

import com.blackducksoftware.integration.hub.jenkins.tests.utils.JenkinsHubIntTestHelper;

public class BDBuildWrapperDescriptorTest {

    private static final String PASSWORD_WRONG = "Assert.failurePassword";

    private static final String USERNAME_NON_EXISTING = "Assert.failureUser";

    private static final String PROJECT_NAME_NOT_EXISTING = "Assert Project Does Not Exist";

    private static final String PROJECT_RELEASE_NOT_EXISTING = "Assert Release Does Not Exist";

    private static Properties testProperties;

    private static JenkinsHubIntTestHelper restHelper;

    // @Rule
    // public ExpectedException exception = ExpectedException.none();
    //
    // @Rule
    // public static JenkinsRule j = new JenkinsRule();

    // @BeforeClass
    // public static void init() throws Exception {
    // testProperties = new Properties();
    // ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    // InputStream is = classLoader.getResourceAsStream("test.properties");
    // try {
    // testProperties.load(is);
    // } catch (IOException e) {
    // System.err.println("reading test.properties failed!");
    // }
    // String user = testProperties.getProperty("TEST_USERNAME");
    // String pass = testProperties.getProperty("TEST_PASSWORD");
    // String url = testProperties.getProperty("TEST_HUB_SERVER_URL");
    // restHelper = new JenkinsHubIntTestHelper(url);
    // restHelper.setCookies(user, pass);
    // projectCleanup();
    // }
    //
    // public static void projectCleanup() {
    // try {
    // ProjectItem project = restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT"));
    // if (project != null && project.getId() != null) {
    // restHelper.deleteHubProject(project.getId());
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
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
    // while (j.getInstance().getDescriptorList(Publisher.class).size() != 0) {
    // j.getInstance().getDescriptorList(Publisher.class).remove(0);
    // }
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
    // @Test
    // public void testCreateProjectAndCheckForNameAndRelease() throws Exception, InterruptedException {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
    // testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId(credential.getId());
    // hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // addHubServerInfo(hubServerInfo);
    //
    // try {
    // FormValidation form = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
    // testProperties.getProperty("TEST_CREATE_VERSION"),
    // "DEVELOPMENT", "EXTERNAL");
    //
    // Assert.assertEquals(FormValidation.Kind.OK, form.kind);
    // Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());
    //
    // // wait 1.5 seconds before checking for the project and release
    // Thread.sleep(3000);
    // // Need to wait a second before checking if the project exists or it will not be recognized
    // FormValidation form2 = descriptor.doCheckHubWrapperProjectName(testProperties.getProperty("TEST_CREATE_PROJECT"),
    // null);
    // Assert.assertEquals(FormValidation.Kind.OK, form2.kind);
    // Assert.assertEquals(form2.getMessage(),
    // Messages.HubBuildScan_getProjectExistsIn_0_(testProperties.getProperty("TEST_HUB_SERVER_URL")));
    //
    // FormValidation form3 =
    // descriptor.doCheckHubWrapperProjectVersion(testProperties.getProperty("TEST_CREATE_VERSION"),
    // testProperties.getProperty("TEST_CREATE_PROJECT"));
    // assertEquals(FormValidation.Kind.OK, form3.kind);
    // assertTrue(form3.getMessage().contains(Messages.HubBuildScan_getVersionExistsIn_0_("")));
    // } finally {
    // restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")).getId());
    // }
    //
    // }
    //
    // @Test
    // public void testCreateProjectDuplicateNameDifferentRelease() throws Exception, InterruptedException {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
    // testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId(credential.getId());
    // hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // addHubServerInfo(hubServerInfo);
    // try {
    // Thread.sleep(3000);
    // FormValidation form = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
    // testProperties.getProperty("TEST_CREATE_VERSION"),
    // "DEVELOPMENT", "EXTERNAL");
    // Assert.assertEquals(FormValidation.Kind.OK, form.kind);
    // Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());
    // Thread.sleep(3000);
    // FormValidation form2 = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
    // "New Release", "DEVELOPMENT",
    // "EXTERNAL");
    // Assert.assertEquals(FormValidation.Kind.OK, form2.kind);
    // Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());
    // } finally {
    // restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")).getId());
    // }
    // }
    //
    // @Test
    // public void testCreateProjectDuplicateRelease() throws Exception, InterruptedException {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
    // testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId(credential.getId());
    // hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // addHubServerInfo(hubServerInfo);
    //
    // try {
    // Thread.sleep(3000);
    // FormValidation form = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
    // testProperties.getProperty("TEST_CREATE_VERSION"),
    // "DEVELOPMENT", "EXTERNAL");
    // Assert.assertEquals(FormValidation.Kind.OK, form.kind);
    // Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
    // Thread.sleep(3000);
    // FormValidation form2 = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
    // testProperties.getProperty("TEST_CREATE_VERSION"),
    // "DEVELOPMENT", "EXTERNAL");
    // Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
    // Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionExist(), form2.getMessage());
    // } finally {
    // restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")).getId());
    // }
    // }
    //
    // @Test
    // public void testCreateProjectVariables() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
    // testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId(credential.getId());
    // hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // addHubServerInfo(hubServerInfo);
    //
    // FormValidation form = descriptor.doCreateHubWrapperProject("${JOB_NAME}",
    // testProperties.getProperty("TEST_CREATE_VERSION"), "DEVELOPMENT", "EXTERNAL");
    //
    // Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
    // Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());
    //
    // FormValidation form2 = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
    // "${BUILD_NUMBER}", "DEVELOPMENT",
    // "EXTERNAL");
    //
    // Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
    // Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectCreated() + " :: " +
    // Messages.HubBuildScan_getProjectVersionContainsVariable());
    //
    // FormValidation form3 = descriptor.doCreateHubWrapperProject("${JOB_NAME}", "${BUILD_NUMBER}", "DEVELOPMENT",
    // "EXTERNAL");
    //
    // Assert.assertEquals(FormValidation.Kind.WARNING, form3.kind);
    // Assert.assertEquals(form3.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());
    //
    // }
    //
    // @Test
    // public void testCheckForProjectNameVariable() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    //
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId("FAKE ID");
    // hubServerInfo.setServerUrl("FAKE SERVER");
    // addHubServerInfo(hubServerInfo);
    //
    // FormValidation form = descriptor.doCheckHubWrapperProjectName("${JOB_NAME}", null);
    // Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
    // Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());
    // }
    //
    // @Test
    // public void testCheckForProjectNameNotExistent() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
    // testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId(credential.getId());
    // hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // addHubServerInfo(hubServerInfo);
    //
    // FormValidation form = descriptor.doCheckHubWrapperProjectName(PROJECT_NAME_NOT_EXISTING, null);
    // Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
    // Assert.assertTrue(form.getMessage(),
    // form.getMessage().contains(Messages.HubBuildScan_getProjectNonExistingOrTroubleConnecting_()));
    // }
    //
    // @Test
    // public void testCheckForProjectReleaseNotExistent() throws Exception, InterruptedException {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
    // testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId(credential.getId());
    // hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // addHubServerInfo(hubServerInfo);
    //
    // try {
    // FormValidation form = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
    // testProperties.getProperty("TEST_CREATE_VERSION"),
    // "DEVELOPMENT", "EXTERNAL");
    // Assert.assertEquals(FormValidation.Kind.OK, form.kind);
    // Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
    // Thread.sleep(2000);
    // FormValidation form2 = descriptor.doCheckHubWrapperProjectVersion(PROJECT_RELEASE_NOT_EXISTING,
    // testProperties.getProperty("TEST_CREATE_PROJECT"));
    // Assert.assertEquals(FormValidation.Kind.ERROR, form2.kind);
    // Assert.assertTrue(form2.getMessage(),
    // form2.getMessage().contains(Messages.HubBuildScan_getVersionNonExistingIn_0_(testProperties.getProperty("TEST_CREATE_PROJECT"),
    // "")));
    // } finally {
    // restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")).getId());
    // }
    // }
    //
    // @Test
    // public void testCheckForReleaseNameVariable() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId("FAKE ID");
    // hubServerInfo.setServerUrl("FAKE SERVER");
    // addHubServerInfo(hubServerInfo);
    //
    // FormValidation form = descriptor.doCheckHubWrapperProjectVersion("${BUILD_NUMBER}", null);
    // Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
    // Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProvideProjectName());
    //
    // FormValidation form2 = descriptor.doCheckHubWrapperProjectVersion("${BUILD_NUMBER}",
    // testProperties.getProperty("TEST_CREATE_PROJECT"));
    // Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
    // Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectVersionContainsVariable());
    // }
    //
    // @Test
    // public void testCheckForProjectReleaseNotExistentProject() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
    // testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId(credential.getId());
    // hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // addHubServerInfo(hubServerInfo);
    //
    // FormValidation form = descriptor
    // .doCheckHubWrapperProjectVersion(testProperties.getProperty("TEST_CREATE_VERSION"),
    // testProperties.getProperty("TEST_CREATE_PROJECT"));
    // Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
    // Assert.assertTrue(form.getMessage(),
    // form.getMessage().contains(Messages.HubBuildScan_getProjectNonExistingOrTroubleConnecting_()));
    // }
    //
    // @Test
    // public void testDoFillHubWrapperVersionPhaseItems() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // ListBoxModel list = descriptor.doFillHubWrapperVersionPhaseItems();
    // assertTrue(list.size() == 5);
    // }
    //
    // @Test
    // public void testDoFillHubWrapperVersionDistItems() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // ListBoxModel list = descriptor.doFillHubWrapperVersionDistItems();
    // assertTrue(list.size() == 3);
    //
    // }
    //
    // @Test
    // public void testDoAutoCompleteHubWrapperProjectNameNotExistent() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
    // testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId(credential.getId());
    // hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // addHubServerInfo(hubServerInfo);
    //
    // AutoCompletionCandidates matches = descriptor.doAutoCompleteHubWrapperProjectName(PROJECT_NAME_NOT_EXISTING);
    // assertTrue(matches.getValues().size() == 0);
    //
    // }
    //
    // @Test
    // public void testDoAutoCompleteHubWrapperProjectName() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
    // testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId(credential.getId());
    // hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // addHubServerInfo(hubServerInfo);
    //
    // AutoCompletionCandidates matches = descriptor.doAutoCompleteHubWrapperProjectName("j");
    // assertTrue(matches.getValues().size() > 0);
    //
    // }
    //
    // @Test
    // public void testGetServerInfoNotConfigured() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    //
    // assertNull(descriptor.getHubServerInfo());
    //
    // }
    //
    // @Test
    // public void testGetServerInfo() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
    // testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // HubServerInfo hubServerInfo = new HubServerInfo();
    // hubServerInfo.setCredentialsId(credential.getId());
    // hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // addHubServerInfo(hubServerInfo);
    //
    // HubServerInfo testServerInfo = descriptor.getHubServerInfo();
    //
    // assertNotNull(testServerInfo);
    // assertEquals(testProperties.getProperty("TEST_USERNAME"), testServerInfo.getUsername());
    // assertEquals(testProperties.getProperty("TEST_PASSWORD"), testServerInfo.getPassword());
    // }
    //
    // @Test
    // public void testIsApplicable() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    // FreeStyleProject project = j.createFreeStyleProject("test");
    // assertTrue(descriptor.isApplicable(project));
    // MavenModuleSet mavenProject = j.createMavenProject();
    // assertTrue(!descriptor.isApplicable(mavenProject));
    // }
    //
    // @Test
    // public void testGetDisplayName() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    //
    // assertTrue(StringUtils.isBlank(descriptor.getDisplayName()));
    // }
    //
    // @Test
    // public void testGetPluginVersion() throws Exception {
    // BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
    //
    // assertTrue(StringUtils.isNotBlank(descriptor.getPluginVersion()));
    // }

}
