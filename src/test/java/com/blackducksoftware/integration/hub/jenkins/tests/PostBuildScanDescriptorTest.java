package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.tests.utils.JenkinsHubIntTestHelper;
import com.blackducksoftware.integration.hub.project.api.ProjectItem;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.AutoCompletionCandidates;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import junit.framework.Assert;

public class PostBuildScanDescriptorTest {
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
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final InputStream is = classLoader.getResourceAsStream("test.properties");
		try {
			testProperties.load(is);
		} catch (final IOException e) {
			System.err.println("reading test.properties failed!");
		}
		final String user = testProperties.getProperty("TEST_USERNAME");
		final String pass = testProperties.getProperty("TEST_PASSWORD");
		final String url = testProperties.getProperty("TEST_HUB_SERVER_URL");

		restHelper = new JenkinsHubIntTestHelper(url);
		restHelper.setCookies(user, pass);
		projectCleanup();
	}

	@Before
	public void resetServerInfo() {
		HubServerInfoSingleton.getInstance().setServerInfo(null);

	}

	public static void projectCleanup() {
		try {
			final ProjectItem project = restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT"));
			if (project != null) {
				restHelper.deleteHubProject(project);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public UsernamePasswordCredentialsImpl addCredentialToGlobalStore(final String username, final String password) {
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				username, password);
		final UserFacingAction store = new UserFacingAction();
		try {
			store.getStore().addCredentials(Domain.global(), credential);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return credential;
	}

	@Test
	public void testConnectionNonExistentUrl() {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final FormValidation form = descriptor.doTestConnection("http://ASSERTNONEXISTENTURL", credential.getId(), "120");
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getCanNotReachThisServer_0_("http://ASSERTNONEXISTENTURL")));

	}

	@Test
	public void testConnectionNonExistentUser() {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final FormValidation form = descriptor.doTestConnection(testProperties.getProperty("TEST_HUB_SERVER_URL"), "NONEXITENTCREDENTIAL", "120");
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertTrue(form.getMessage().contains("User needs to specify which credentials to use."));

	}

	@Test
	public void testConnectionUnAuthorized() {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(USERNAME_NON_EXISTING,
				PASSWORD_WRONG);
		final FormValidation form = descriptor.doTestConnection(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId(), "120");
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertTrue(form.getMessage(), form.getMessage().contains("Unauthorized (401)"));

	}

	@Test
	public void testConnection() {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final FormValidation form = descriptor.doTestConnection(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId(), "120");
		Assert.assertEquals(FormValidation.Kind.OK, form.kind);
		Assert.assertTrue(form.getMessage(), form.getMessage().contains("Credentials valid for: " + testProperties.getProperty("TEST_HUB_SERVER_URL")));

	}

	@Test
	public void testConnectionZeroTimeout() {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final FormValidation form = descriptor.doTestConnection(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId(), "0");
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertTrue(form.getMessage(), form.getMessage().contains("Can not set the timeout to zero"));

	}

	@Test
	public void testCreateProjectAndCheckForNameAndRelease() throws Exception, InterruptedException {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		try {
			final FormValidation form = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
					testProperties.getProperty("TEST_CREATE_VERSION"),
					"DEVELOPMENT", "EXTERNAL");

			Assert.assertEquals(FormValidation.Kind.OK, form.kind);
			Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());

			// wait before checking for the project and release
			Thread.sleep(6000);
			// Need to wait a second before checking if the project exists or it will not be recognized
			final FormValidation form2 = descriptor.doCheckHubProjectName(testProperties.getProperty("TEST_CREATE_PROJECT"), null);
			Assert.assertEquals(
					Messages.HubBuildScan_getProjectExistsIn_0_(testProperties.getProperty("TEST_HUB_SERVER_URL")),
					form2.getMessage());
			Assert.assertEquals(FormValidation.Kind.OK, form2.kind);

			final FormValidation form3 = descriptor.doCheckHubProjectVersion(testProperties.getProperty("TEST_CREATE_VERSION"),
					testProperties.getProperty("TEST_CREATE_PROJECT"));
			assertTrue(form3.getMessage().contains(Messages.HubBuildScan_getVersionExistsIn_0_("")));
			assertEquals(FormValidation.Kind.OK, form3.kind);
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")));
		}

	}

	@Test
	public void testCreateProjectDuplicateNameDifferentRelease() throws Exception, InterruptedException {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);
		try {
			Thread.sleep(6000);
			final FormValidation form = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
					testProperties.getProperty("TEST_CREATE_VERSION"),
					"DEVELOPMENT", "EXTERNAL");
			Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());
			Assert.assertEquals(FormValidation.Kind.OK, form.kind);
			Thread.sleep(6000);
			final FormValidation form2 = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"), "New Release", "DEVELOPMENT", "EXTERNAL");
			Assert.assertEquals(Messages.HubBuildScan_getVersionCreated(), form2.getMessage());
			Assert.assertEquals(FormValidation.Kind.OK, form2.kind);
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")));
		}
	}

	@Test
	public void testCreateProjectDuplicateRelease() throws Exception, InterruptedException {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		try {
			Thread.sleep(6000);
			final FormValidation form = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
					testProperties.getProperty("TEST_CREATE_VERSION"),
					"DEVELOPMENT", "EXTERNAL");
			Assert.assertEquals(FormValidation.Kind.OK, form.kind);
			Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
			Thread.sleep(6000);
			final FormValidation form2 = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
					testProperties.getProperty("TEST_CREATE_VERSION"),
					"DEVELOPMENT", "EXTERNAL");
			Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
			Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionExist(), form2.getMessage());
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")));
		}
	}

	@Test
	public void testCreateProjectVariables() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		final FormValidation form = descriptor.doCreateHubProject("${JOB_NAME}", testProperties.getProperty("TEST_CREATE_VERSION"), "DEVELOPMENT", "EXTERNAL");

		Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
		Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());

		final FormValidation form2 = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"), "${BUILD_NUMBER}", "DEVELOPMENT", "EXTERNAL");

		Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
		Assert.assertEquals(Messages.HubBuildScan_getProjectVersionContainsVariable(), form2.getMessage());

		final FormValidation form3 = descriptor.doCreateHubProject("${JOB_NAME}", "${BUILD_NUMBER}", "DEVELOPMENT", "EXTERNAL");

		Assert.assertEquals(FormValidation.Kind.WARNING, form3.kind);
		Assert.assertEquals(form3.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());

	}

	@Test
	public void testCheckForProjectNameVariable() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId("FAKE ID");
		hubServerInfo.setServerUrl("FAKE SERVER");
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		final FormValidation form = descriptor.doCheckHubProjectName("${JOB_NAME}", null);
		Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
		Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());
	}

	@Test
	public void testCheckForProjectNameNotExistent() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		try {
			restHelper.deleteHubProject(restHelper.getProjectByName(PROJECT_NAME_NOT_EXISTING));
		} catch (final Exception e) {
			// ignore all exceptions
		}

		final FormValidation form = descriptor.doCheckHubProjectName(PROJECT_NAME_NOT_EXISTING, null);
		Assert.assertTrue(form.getMessage(),
				form.getMessage().contains(Messages.HubBuildScan_getProjectNonExistingIn_0_(testProperties.getProperty("TEST_HUB_SERVER_URL"))));
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
	}

	@Test
	public void testCheckForProjectNameEmptyWithVersion() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

		final FormValidation form = descriptor.doCheckHubProjectName("", "testVersion");
		Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getProvideProjectName()));
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
	}

	@Test
	public void testCheckForProjectVersionEmptyWithName() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

		final FormValidation form = descriptor.doCheckHubProjectVersion("", "testName");
		Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getProvideProjectVersion()));
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
	}

	@Test
	public void testCheckForProjectReleaseNotExistent() throws Exception, InterruptedException {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		try {
			restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")));
		} catch (final Exception e) {
		}

		try {
			final FormValidation form = descriptor.doCreateHubProject(testProperties.getProperty("TEST_CREATE_PROJECT"),
					testProperties.getProperty("TEST_CREATE_VERSION"),
					"DEVELOPMENT", "EXTERNAL");
			Assert.assertEquals(FormValidation.Kind.OK, form.kind);
			Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
			Thread.sleep(6000);
			final FormValidation form2 = descriptor.doCheckHubProjectVersion(PROJECT_RELEASE_NOT_EXISTING, testProperties.getProperty("TEST_CREATE_PROJECT"));
			Assert.assertTrue(form2.getMessage(),
					form2.getMessage().contains(Messages.HubBuildScan_getVersionNonExistingIn_0_(testProperties.getProperty("TEST_CREATE_PROJECT"), "")));
			Assert.assertEquals(FormValidation.Kind.ERROR, form2.kind);
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")));
		}
	}

	@Test
	public void testCheckForReleaseNameVariable() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId("FAKE ID");
		hubServerInfo.setServerUrl("FAKE SERVER");
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		final FormValidation form = descriptor.doCheckHubProjectVersion("${BUILD_NUMBER}", null);
		Assert.assertEquals(FormValidation.Kind.OK, form.kind);

		final FormValidation form2 = descriptor.doCheckHubProjectVersion("${BUILD_NUMBER}", testProperties.getProperty("TEST_CREATE_PROJECT"));
		Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
		Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectVersionContainsVariable());
	}

	@Test
	public void testCheckForProjectReleaseNotExistentProject() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		try {
			restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")));
		} catch (final Exception e) {
			// ignore all exceptions
		}
		final FormValidation form = descriptor.doCheckHubProjectVersion(testProperties.getProperty("TEST_CREATE_VERSION"),
				testProperties.getProperty("TEST_CREATE_PROJECT"));
		// This is OK because we expect the Name field to catch this error
		Assert.assertEquals(FormValidation.Kind.OK, form.kind);
	}

	@Test
	public void testCheckInvalidMemory() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

		final FormValidation form = descriptor.doCheckScanMemory("This is not an Integer");
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertTrue(form.getMessage(), form.getMessage().contains(Messages.HubBuildScan_getInvalidMemoryString()));
	}

	@Test
	public void testCheckNoMemory() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

		final FormValidation form = descriptor.doCheckScanMemory("");
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getNeedMemory());
	}

	@Test
	public void testCheckValidMemory() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

		final FormValidation form = descriptor.doCheckScanMemory("512");
		Assert.assertEquals(FormValidation.Kind.OK, form.kind);
	}

	@Test
	public void testDoFillHubVersionPhaseItems() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final ListBoxModel list = descriptor.doFillHubVersionPhaseItems();

		assertTrue(list.size() == 5);

	}

	@Test
	public void testDoFillHubVersionDistItems() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final ListBoxModel list = descriptor.doFillHubVersionDistItems();

		assertTrue(list.size() == 4);

	}

	@Test
	public void testDoAutoCompleteHubWrapperProjectNameNotExistent() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		final AutoCompletionCandidates matches = descriptor.doAutoCompleteHubProjectName("SHOULDNOTAUTOCOMPLETETOANYTHING");
		assertTrue(matches.getValues().size() == 0);

	}

	@Test
	public void testDoAutoCompleteHubWrapperProjectName() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"),
				testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);
		final String projectAutoCompleteName = "AutoCompleteName";
		try {
			descriptor.doCreateHubProject(projectAutoCompleteName,
					testProperties.getProperty("TEST_CREATE_VERSION"),
					"DEVELOPMENT", "EXTERNAL");

			// Need to sleep 1 second, otherwise the project not be available when we try the auto complete
			Thread.sleep(2000l);

			final AutoCompletionCandidates matches = descriptor.doAutoCompleteHubProjectName(projectAutoCompleteName);
			assertTrue(matches.getValues().size() > 0);

		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(projectAutoCompleteName));
		}

	}

	@Test
	public void testDoCheckReportMaxiumWaitTime() throws Exception {
		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();

		FormValidation form = descriptor.doCheckBomUpdateMaxiumWaitTime(null);
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getBomUpdateWaitTimeEmpty());

		form = descriptor.doCheckBomUpdateMaxiumWaitTime("");
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getBomUpdateWaitTimeEmpty());

		form = descriptor.doCheckBomUpdateMaxiumWaitTime("This is not an Integer");
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertTrue(form.getMessage(),
				form.getMessage().contains(Messages.HubBuildScan_getBomUpdateWaitTimeInvalid()));

		form = descriptor.doCheckBomUpdateMaxiumWaitTime("0");
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getBomUpdateWaitTimeGreaterThanZero());

		form = descriptor.doCheckBomUpdateMaxiumWaitTime("1");
		Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
		Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getBomUpdateWaitTimeShort());

		form = descriptor.doCheckBomUpdateMaxiumWaitTime("5");
		Assert.assertEquals(FormValidation.Kind.OK, form.kind);
	}

}
