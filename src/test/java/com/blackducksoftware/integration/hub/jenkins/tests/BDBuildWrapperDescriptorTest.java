package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import hudson.maven.MavenModuleSet;
import hudson.model.AutoCompletionCandidates;
import hudson.model.FreeStyleProject;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import com.blackducksoftware.integration.hub.jenkins.BDBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.PluginHelper;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.response.DistributionEnum;
import com.blackducksoftware.integration.hub.response.PhaseEnum;
import com.blackducksoftware.integration.hub.response.ProjectItem;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class BDBuildWrapperDescriptorTest {

    private static final String PASSWORD_WRONG = "Assert.failurePassword";

    private static final String USERNAME_NON_EXISTING = "Assert.failureUser";

    private static final String PROJECT_NAME_NOT_EXISTING = "Assert Project Does Not Exist";

    private static final String PROJECT_RELEASE_NOT_EXISTING = "Assert Release Does Not Exist";

    private static Properties testProperties;

    private static JenkinsHubIntTestHelper restHelper;

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
            ProjectItem project = restHelper.getProjectByName(testProperties.getProperty("TEST_PROJECT"));
            if (project != null && project.getId() != null) {
                restHelper.deleteHubProject(project.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addHubServerInfo(HubServerInfo hubServerInfo) {
        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        descriptor.setHubServerInfo(hubServerInfo);
        j.getInstance().getDescriptorList(Publisher.class).add(descriptor);
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
    public void testCreateProjectAndCheckForNameAndRelease() throws Exception, InterruptedException {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        addHubServerInfo(hubServerInfo);

        try {
            FormValidation form = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_PROJECT"), testProperties.getProperty("TEST_VERSION"),
                    "DEVELOPMENT", "EXTERNAL");

            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());

            // wait 1.5 seconds before checking for the project and release
            Thread.sleep(3000);
            // Need to wait a second before checking if the project exists or it will not be recognized
            FormValidation form2 = descriptor.doCheckHubWrapperProjectName(testProperties.getProperty("TEST_PROJECT"), null);
            Assert.assertEquals(FormValidation.Kind.OK, form2.kind);
            Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectExistsIn_0_(testProperties.getProperty("TEST_HUB_SERVER_URL")));

            FormValidation form3 = descriptor.doCheckHubWrapperProjectVersion(testProperties.getProperty("TEST_VERSION"),
                    testProperties.getProperty("TEST_PROJECT"));
            assertEquals(FormValidation.Kind.OK, form3.kind);
            assertTrue(form3.getMessage().contains(Messages.HubBuildScan_getVersionExistsIn_0_("")));
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_PROJECT")).getId());
        }

    }

    @Test
    public void testCreateProjectDuplicateNameDifferentRelease() throws Exception, InterruptedException {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        addHubServerInfo(hubServerInfo);
        try {
            Thread.sleep(3000);
            FormValidation form = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_PROJECT"), testProperties.getProperty("TEST_VERSION"),
                    "DEVELOPMENT", "EXTERNAL");
            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());
            Thread.sleep(3000);
            FormValidation form2 = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_PROJECT"), "New Release", "DEVELOPMENT", "EXTERNAL");
            Assert.assertEquals(FormValidation.Kind.OK, form2.kind);
            Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_PROJECT")).getId());
        }
    }

    @Test
    public void testCreateProjectDuplicateRelease() throws Exception, InterruptedException {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        addHubServerInfo(hubServerInfo);

        try {
            Thread.sleep(3000);
            FormValidation form = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_PROJECT"), testProperties.getProperty("TEST_VERSION"),
                    "DEVELOPMENT", "EXTERNAL");
            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
            Thread.sleep(3000);
            FormValidation form2 = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_PROJECT"), testProperties.getProperty("TEST_VERSION"),
                    "DEVELOPMENT", "EXTERNAL");
            Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
            Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionExist(), form2.getMessage());
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_PROJECT")).getId());
        }
    }

    @Test
    public void testCreateProjectVariables() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        addHubServerInfo(hubServerInfo);

        FormValidation form = descriptor.doCreateHubWrapperProject("${JOB_NAME}", testProperties.getProperty("TEST_VERSION"), "DEVELOPMENT", "EXTERNAL");

        Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
        Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());

        FormValidation form2 = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_PROJECT"), "${BUILD_NUMBER}", "DEVELOPMENT", "EXTERNAL");

        Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
        Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectCreated() + " :: " + Messages.HubBuildScan_getProjectVersionContainsVariable());

        FormValidation form3 = descriptor.doCreateHubWrapperProject("${JOB_NAME}", "${BUILD_NUMBER}", "DEVELOPMENT", "EXTERNAL");

        Assert.assertEquals(FormValidation.Kind.WARNING, form3.kind);
        Assert.assertEquals(form3.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());

    }

    @Test
    public void testCheckForProjectNameVariable() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();

        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId("FAKE ID");
        hubServerInfo.setServerUrl("FAKE SERVER");
        addHubServerInfo(hubServerInfo);

        FormValidation form = descriptor.doCheckHubWrapperProjectName("${JOB_NAME}", null);
        Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
        Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());
    }

    @Test
    public void testCheckForProjectNameNotExistent() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        addHubServerInfo(hubServerInfo);

        FormValidation form = descriptor.doCheckHubWrapperProjectName(PROJECT_NAME_NOT_EXISTING, null);
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getProjectNonExistingOrTroubleConnecting_()));
    }

    @Test
    public void testCheckForProjectReleaseNotExistent() throws Exception, InterruptedException {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        addHubServerInfo(hubServerInfo);

        try {
            FormValidation form = descriptor.doCreateHubWrapperProject(testProperties.getProperty("TEST_PROJECT"), testProperties.getProperty("TEST_VERSION"),
                    "DEVELOPMENT", "EXTERNAL");
            Assert.assertEquals(FormValidation.Kind.OK, form.kind);
            Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
            Thread.sleep(2000);
            FormValidation form2 = descriptor.doCheckHubWrapperProjectVersion(PROJECT_RELEASE_NOT_EXISTING, testProperties.getProperty("TEST_PROJECT"));
            Assert.assertEquals(FormValidation.Kind.ERROR, form2.kind);
            Assert.assertTrue(form2.getMessage(),
                    form2.getMessage().contains(Messages.HubBuildScan_getVersionNonExistingIn_0_(testProperties.getProperty("TEST_PROJECT"), "")));
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_PROJECT")).getId());
        }
    }

    @Test
    public void testCheckForReleaseNameVariable() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId("FAKE ID");
        hubServerInfo.setServerUrl("FAKE SERVER");
        addHubServerInfo(hubServerInfo);

        FormValidation form = descriptor.doCheckHubWrapperProjectVersion("${BUILD_NUMBER}", null);
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProvideProjectName());

        FormValidation form2 = descriptor.doCheckHubWrapperProjectVersion("${BUILD_NUMBER}", testProperties.getProperty("TEST_PROJECT"));
        Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
        Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectVersionContainsVariable());
    }

    @Test
    public void testCheckForProjectReleaseNotExistentProject() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        addHubServerInfo(hubServerInfo);

        FormValidation form = descriptor
                .doCheckHubWrapperProjectVersion(testProperties.getProperty("TEST_VERSION"), testProperties.getProperty("TEST_PROJECT"));
        Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
        Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getProjectNonExistingOrTroubleConnecting_()));
    }

    @Test
    public void testDoFillHubWrapperVersionPhaseItems() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        ListBoxModel list = descriptor.doFillHubWrapperVersionPhaseItems();

        assertTrue(list.contains(new ListBoxModel.Option(PhaseEnum.ARCHIVED.name(), PhaseEnum.ARCHIVED.name())));
        assertTrue(list.contains(new ListBoxModel.Option(PhaseEnum.DEPRECATED.name(), PhaseEnum.DEPRECATED.name())));
        assertTrue(list.contains(new ListBoxModel.Option(PhaseEnum.DEVELOPMENT.name(), PhaseEnum.DEVELOPMENT.name())));
        assertTrue(list.contains(new ListBoxModel.Option(PhaseEnum.PLANNING.name(), PhaseEnum.PLANNING.name())));
        assertTrue(list.contains(new ListBoxModel.Option(PhaseEnum.RELEASED.name(), PhaseEnum.RELEASED.name())));

        assertTrue(!list.contains(new ListBoxModel.Option(PhaseEnum.UNKNOWNPHASE.name(), PhaseEnum.UNKNOWNPHASE.name())));

    }

    @Test
    public void testDoFillHubWrapperVersionDistItems() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        ListBoxModel list = descriptor.doFillHubWrapperVersionDistItems();

        assertTrue(list.contains(new ListBoxModel.Option(DistributionEnum.EXTERNAL.name(), DistributionEnum.EXTERNAL.name())));
        assertTrue(list.contains(new ListBoxModel.Option(DistributionEnum.INTERNAL.name(), DistributionEnum.INTERNAL.name())));
        assertTrue(list.contains(new ListBoxModel.Option(DistributionEnum.SAAS.name(), DistributionEnum.SAAS.name())));

        assertTrue(!list.contains(new ListBoxModel.Option(DistributionEnum.UNKNOWNDISTRIBUTION.name(), DistributionEnum.UNKNOWNDISTRIBUTION.name())));

    }

    @Test
    public void testDoAutoCompleteHubWrapperProjectNameNotExistent() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        addHubServerInfo(hubServerInfo);

        AutoCompletionCandidates matches = descriptor.doAutoCompleteHubWrapperProjectName(PROJECT_NAME_NOT_EXISTING);
        assertTrue(matches.getValues().size() == 0);

    }

    @Test
    public void testDoAutoCompleteHubWrapperProjectName() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        addHubServerInfo(hubServerInfo);

        AutoCompletionCandidates matches = descriptor.doAutoCompleteHubWrapperProjectName(testProperties.getProperty("TEST_PROJECT"));
        assertTrue(matches.getValues().size() > 0);

    }

    @Test
    public void testGetServerInfoNotConfigured() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();

        assertNull(descriptor.getHubServerInfo());

    }

    @Test
    public void testGetServerInfo() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(credential.getId());
        hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        addHubServerInfo(hubServerInfo);

        HubServerInfo testServerInfo = descriptor.getHubServerInfo();

        assertNotNull(testServerInfo);
        assertEquals(testProperties.getProperty("TEST_USERNAME"), testServerInfo.getUsername());
        assertEquals(testProperties.getProperty("TEST_PASSWORD"), testServerInfo.getPassword());
    }

    @Test
    public void testIsApplicable() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
        FreeStyleProject project = j.createFreeStyleProject("test");
        assertTrue(descriptor.isApplicable(project));
        MavenModuleSet mavenProject = j.createMavenProject();
        assertTrue(!descriptor.isApplicable(project));
    }

    @Test
    public void testGetDisplayName() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();

        assertTrue(StringUtils.isBlank(descriptor.getDisplayName()));
    }

    @WithoutJenkins
    @Test
    public void testGetPluginVersionUnknown() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();

        assertEquals(PluginHelper.UNKNOWN_VERSION, descriptor.getPluginVersion());
    }

    @Test
    public void testGetPluginVersion() throws Exception {
        BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();

        assertTrue(StringUtils.isNotBlank(descriptor.getPluginVersion()));
    }

    // Test checkscopes in the other descriptors

}
