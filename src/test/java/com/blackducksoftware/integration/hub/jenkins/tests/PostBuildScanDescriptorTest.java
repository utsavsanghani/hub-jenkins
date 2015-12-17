package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import hudson.model.AutoCompletionCandidates;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.tests.utils.JenkinsHubIntTestHelper;
import com.blackducksoftware.integration.hub.response.ProjectItem;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class PostBuildScanDescriptorTest {
    private static final String PASSWORD_WRONG = "Assert.failurePassword";

    private static final String USERNAME_NON_EXISTING = "Assert.failureUser";

    private static final String PROJECT_NAME_NOT_EXISTING = "Assert Project Does Not Exist";

    private static final String PROJECT_RELEASE_NOT_EXISTING = "Assert Release Does Not Exist";

    private static Properties testProperties;

    private static JenkinsHubIntTestHelper restHelper;

    private static PostBuildScanDescriptor descriptor;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public static JenkinsRule j = new JenkinsRule();

    @BeforeClass
    public static void init() throws Exception {
        testProperties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream("test.properties");
        try {
            testProperties.load(is);
        } catch (IOException e) {
            System.err.println("reading test.properties failed!");
        }
        String user = testProperties.getProperty("TEST_USERNAME");
        String pass = testProperties.getProperty("TEST_PASSWORD");
        String url = testProperties.getProperty("TEST_HUB_SERVER_URL");
        restHelper = new JenkinsHubIntTestHelper(url);
        restHelper.setCookies(user, pass);
        projectCleanup();
    }

    public static void projectCleanup() {
        try {
            ProjectItem project = restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT"));
            if (project != null && project.getId() != null) {
                restHelper.deleteHubProject(project.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UsernamePasswordCredentialsImpl addCredentialToGlobalStore(String username, String password) {
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                username, password);
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return credential;
    }

    @Test
    public void testConnectionNonExistentUrl() {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        FormValidation form = descriptor.doTestConnection("http://ASSERTNONEXISTENTURL", credential.getId());
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getCanNotReachThisServer_0_("http://ASSERTNONEXISTENTURL")));

    }

    @Test
    public void testConnectionNonExistentUser() {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        FormValidation form = descriptor.doTestConnection(testProperties.getProperty("TEST_HUB_SERVER_URL"), "NONEXITENTCREDENTIAL");
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertTrue(form.getMessage().contains("User needs to specify which credentials to use."));

    }

    @Test
    public void testConnectionUnAuthorized() {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(USERNAME_NON_EXISTING,
                PASSWORD_WRONG);
        FormValidation form = descriptor.doTestConnection(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId());
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertTrue(form.getMessage(), form.getMessage().contains("Unauthorized (401)"));

    }

    @Test
    public void testConnection() {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        FormValidation form = descriptor.doTestConnection(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId());
        Assert.assertEquals(FormValidation.Kind.OK, form.kind);
        Assert.assertTrue(form.getMessage(), form.getMessage().contains("Credentials valid for: " + testProperties.getProperty("TEST_HUB_SERVER_URL")));

    }

    @Test
    public void testCreateProjectAndCheckForNameAndRelease() throws Exception, InterruptedException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        try {
            FormValidation form = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
                    testProperties.getProperty("TEST_CREATE_VERSION"),
                    "DEVELOPMENT", "EXTERNAL");

            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());

            // wait before checking for the project and release
            Thread.sleep(6000);
            // Need to wait a second before checking if the project exists or it will not be recognized
            FormValidation form2 = descriptor.doCheckHubProjectName(testProperties.getProperty("TEST_CREATE_PROJECT"), null);
            Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectExistsIn_0_(testProperties.getProperty("TEST_HUB_SERVER_URL")));
            Assert.assertEquals(FormValidation.Kind.OK, form2.kind);

            FormValidation form3 = descriptor.doCheckHubProjectVersion(testProperties.getProperty("TEST_CREATE_VERSION"),
                    testProperties.getProperty("TEST_CREATE_PROJECT"));
            assertTrue(form3.getMessage().contains(Messages.HubBuildScan_getVersionExistsIn_0_("")));
            assertEquals(FormValidation.Kind.OK, form3.kind);
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")).getId());
        }

    }

    @Test
    public void testCreateProjectDuplicateNameDifferentRelease() throws Exception, InterruptedException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);
        try {
            Thread.sleep(6000);
            FormValidation form = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
                    testProperties.getProperty("TEST_CREATE_VERSION"),
                    "DEVELOPMENT", "EXTERNAL");
            Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());
            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Thread.sleep(6000);
            FormValidation form2 = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"), "New Release", "DEVELOPMENT", "EXTERNAL");
            Assert.assertEquals(Messages.HubBuildScan_getVersionCreated(), form2.getMessage());
            Assert.assertEquals(FormValidation.Kind.OK, form2.kind);
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")).getId());
        }
    }

    @Test
    public void testCreateProjectDuplicateRelease() throws Exception, InterruptedException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        try {
            Thread.sleep(6000);
            FormValidation form = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
                    testProperties.getProperty("TEST_CREATE_VERSION"),
                    "DEVELOPMENT", "EXTERNAL");
            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
            Thread.sleep(6000);
            FormValidation form2 = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
                    testProperties.getProperty("TEST_CREATE_VERSION"),
                    "DEVELOPMENT", "EXTERNAL");
            Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
            Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionExist(), form2.getMessage());
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")).getId());
        }
    }

    @Test
    public void testCreateProjectVariables() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        FormValidation form = descriptor.doCreateHubProject("${JOB_NAME}", testProperties.getProperty("TEST_CREATE_VERSION"), "DEVELOPMENT", "EXTERNAL");

        Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
        Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());

        FormValidation form2 = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"), "${BUILD_NUMBER}", "DEVELOPMENT", "EXTERNAL");

        Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
        Assert.assertEquals(Messages.HubBuildScan_getProjectVersionContainsVariable(), form2.getMessage());

        FormValidation form3 = descriptor.doCreateHubProject("${JOB_NAME}", "${BUILD_NUMBER}", "DEVELOPMENT", "EXTERNAL");

        Assert.assertEquals(FormValidation.Kind.WARNING, form3.kind);
        Assert.assertEquals(form3.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());

    }

    @Test
    public void testCheckForProjectNameVariable() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId("FAKE ID");
        hubServerInfo.setServerUrl("FAKE SERVER");
        descriptor.setHubServerInfo(hubServerInfo);

        FormValidation form = descriptor.doCheckHubProjectName("${JOB_NAME}", null);
        Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
        Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());
    }

    @Test
    public void testCheckForProjectNameNotExistent() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        try {
            restHelper.deleteHubProject(restHelper.getProjectByName(PROJECT_NAME_NOT_EXISTING).getId());
        } catch (Exception e) {
            // ignore all exceptions
        }

        FormValidation form = descriptor.doCheckHubProjectName(PROJECT_NAME_NOT_EXISTING, null);
        Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getProjectNonExistingOrTroubleConnecting_()));
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
    }

    @Test
    public void testCheckForProjectNameEmptyWithVersion() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

        FormValidation form = descriptor.doCheckHubProjectName("", "testVersion");
        Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getProvideProjectName()));
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
    }

    @Test
    public void testCheckForProjectVersionEmptyWithName() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

        FormValidation form = descriptor.doCheckHubProjectVersion("", "testName");
        Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getProvideProjectVersion()));
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
    }

    @Test
    public void testCheckForProjectReleaseNotExistent() throws Exception, InterruptedException {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        try {
            FormValidation form = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
                    testProperties.getProperty("TEST_CREATE_VERSION"),
                    "DEVELOPMENT", "EXTERNAL");
            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
            Thread.sleep(6000);
            FormValidation form2 = descriptor.doCheckHubProjectVersion(PROJECT_RELEASE_NOT_EXISTING, testProperties.getProperty("TEST_CREATE_PROJECT"));
            Assert.assertTrue(form2.getMessage(),
                    form2.getMessage().contains(Messages.HubBuildScan_getVersionNonExistingIn_0_(testProperties.getProperty("TEST_CREATE_PROJECT"), "")));
            Assert.assertEquals(FormValidation.Kind.ERROR, form2.kind);
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")).getId());
        }
    }

    @Test
    public void testCheckForReleaseNameVariable() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId("FAKE ID");
        hubServerInfo.setServerUrl("FAKE SERVER");
        descriptor.setHubServerInfo(hubServerInfo);

        FormValidation form = descriptor.doCheckHubProjectVersion("${BUILD_NUMBER}", null);
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProvideProjectName());

        FormValidation form2 = descriptor.doCheckHubProjectVersion("${BUILD_NUMBER}", testProperties.getProperty("TEST_CREATE_PROJECT"));
        Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
        Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectVersionContainsVariable());
    }

    @Test
    public void testCheckForProjectReleaseNotExistentProject() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        try {
            restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")).getId());
        } catch (Exception e) {
            // ignore all exceptions
        }
        FormValidation form = descriptor.doCheckHubProjectVersion(testProperties.getProperty("TEST_CREATE_VERSION"),
                testProperties.getProperty("TEST_CREATE_PROJECT"));
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getProjectNonExistingOrTroubleConnecting_()));
    }

    @Test
    public void testCheckInvalidMemory() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

        FormValidation form = descriptor.doCheckScanMemory("This is not an Integer");
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getInvalidMemoryString()));
    }

    @Test
    public void testCheckNoMemory() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

        FormValidation form = descriptor.doCheckScanMemory("");
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getNeedMemory());
    }

    @Test
    public void testCheckValidMemory() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

        FormValidation form = descriptor.doCheckScanMemory("512");
        Assert.assertEquals(FormValidation.Kind.OK, form.kind);
    }

    @Test
    public void testDoFillHubVersionPhaseItems() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        ListBoxModel list = descriptor.doFillHubVersionPhaseItems();

        assertTrue(list.size() == 5);

    }

    @Test
    public void testDoFillHubVersionDistItems() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        ListBoxModel list = descriptor.doFillHubVersionDistItems();

        assertTrue(list.size() == 3);

    }

    @Test
    public void testDoAutoCompleteHubWrapperProjectNameNotExistent() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        AutoCompletionCandidates matches = descriptor.doAutoCompleteHubProjectName("SHOULDNOTAUTOCOMPLETETOANYTHING");
        assertTrue(matches.getValues().size() == 0);

    }

    @Test
    public void testDoAutoCompleteHubWrapperProjectName() throws Exception {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        descriptor.setHubServerInfo(hubServerInfo);

        AutoCompletionCandidates matches = descriptor.doAutoCompleteHubProjectName("j");
        assertTrue(matches.getValues().size() > 0);
    }

}
