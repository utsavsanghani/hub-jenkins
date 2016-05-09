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
package com.blackducksoftware.integration.hub.jenkins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.jenkins.category.IntegrationTest;
import com.blackducksoftware.integration.hub.jenkins.utils.HubJenkinsTestIntRestService;
import com.blackducksoftware.integration.hub.project.api.ProjectItem;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.maven.MavenModuleSet;
import hudson.model.AutoCompletionCandidates;
import hudson.model.FreeStyleProject;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

@Category(IntegrationTest.class)
public class BDBuildWrapperDescriptorTest {
	private static final String PROJECT_NAME_NOT_EXISTING = "Assert Project Does Not Exist";
	private static final String PROJECT_RELEASE_NOT_EXISTING = "Assert Release Does Not Exist";
	private static Properties testProperties;
	private static HubJenkinsTestIntRestService restHelper;

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
		restHelper = new HubJenkinsTestIntRestService(url);
		restHelper.setCookies(user, pass);
		projectCleanup();
	}

	public static void projectCleanup() {
		try {
			final ProjectItem project = restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT"));
			restHelper.deleteHubProject(project);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void addHubServerInfo(final HubServerInfo hubServerInfo) {
		resetPublisherDescriptors();

		final PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);
		j.getInstance().getDescriptorList(Publisher.class).add(descriptor);
	}

	public void resetPublisherDescriptors() {
		while (j.getInstance().getDescriptorList(Publisher.class).size() != 0) {
			j.getInstance().getDescriptorList(Publisher.class).remove(0);
		}
	}

	public UsernamePasswordCredentialsImpl addCredentialToGlobalStore(final String username, final String password) {
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
				null, null, username, password);
		final UserFacingAction store = new UserFacingAction();
		try {
			store.getStore().addCredentials(Domain.global(), credential);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return credential;
	}

	@Test
	public void testCreateProjectAndCheckForNameAndRelease() throws Exception, InterruptedException {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		addHubServerInfo(hubServerInfo);

		try {
			final FormValidation form = descriptor.doCreateHubWrapperProject(
					testProperties.getProperty("TEST_CREATE_PROJECT"),
					testProperties.getProperty("TEST_CREATE_VERSION"), "DEVELOPMENT", "EXTERNAL");

			Assert.assertEquals(FormValidation.Kind.OK, form.kind);
			Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());

			// wait 1.5 seconds before checking for the project and release
			Thread.sleep(3000);
			// Need to wait a second before checking if the project exists or it
			// will not be recognized
			final FormValidation form2 = descriptor
					.doCheckHubWrapperProjectName(testProperties.getProperty("TEST_CREATE_PROJECT"), null);
			Assert.assertEquals(FormValidation.Kind.OK, form2.kind);
			Assert.assertEquals(form2.getMessage(),
					Messages.HubBuildScan_getProjectExistsIn_0_(testProperties.getProperty("TEST_HUB_SERVER_URL")));

			final FormValidation form3 = descriptor.doCheckHubWrapperProjectVersion(
					testProperties.getProperty("TEST_CREATE_VERSION"),
					testProperties.getProperty("TEST_CREATE_PROJECT"));
			assertEquals(FormValidation.Kind.OK, form3.kind);
			assertTrue(form3.getMessage().contains(Messages.HubBuildScan_getVersionExistsIn_0_("")));
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")));
		}
	}

	@Test
	public void testCreateProjectDuplicateNameDifferentRelease() throws Exception, InterruptedException {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		addHubServerInfo(hubServerInfo);
		try {
			Thread.sleep(3000);
			final FormValidation form = descriptor.doCreateHubWrapperProject(
					testProperties.getProperty("TEST_CREATE_PROJECT"),
					testProperties.getProperty("TEST_CREATE_VERSION"), "DEVELOPMENT", "EXTERNAL");
			Assert.assertEquals(FormValidation.Kind.OK, form.kind);
			Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectAndVersionCreated());
			Thread.sleep(3000);
			final FormValidation form2 = descriptor.doCreateHubWrapperProject(
					testProperties.getProperty("TEST_CREATE_PROJECT"), "New Release", "DEVELOPMENT", "EXTERNAL");
			Assert.assertEquals(FormValidation.Kind.OK, form2.kind);
			Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getVersionCreated());
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")));
		}
	}

	@Test
	public void testCreateProjectDuplicateRelease() throws Exception, InterruptedException {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		addHubServerInfo(hubServerInfo);

		try {
			Thread.sleep(3000);
			final FormValidation form = descriptor.doCreateHubWrapperProject(
					testProperties.getProperty("TEST_CREATE_PROJECT"),
					testProperties.getProperty("TEST_CREATE_VERSION"), "DEVELOPMENT", "EXTERNAL");
			Assert.assertEquals(FormValidation.Kind.OK, form.kind);
			Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
			Thread.sleep(3000);
			final FormValidation form2 = descriptor.doCreateHubWrapperProject(
					testProperties.getProperty("TEST_CREATE_PROJECT"),
					testProperties.getProperty("TEST_CREATE_VERSION"), "DEVELOPMENT", "EXTERNAL");
			Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
			Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionExist(), form2.getMessage());
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")));
		}
	}

	@Test
	public void testCreateProjectVariables() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		addHubServerInfo(hubServerInfo);

		final FormValidation form = descriptor.doCreateHubWrapperProject("${JOB_NAME}",
				testProperties.getProperty("TEST_CREATE_VERSION"), "DEVELOPMENT", "EXTERNAL");

		Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
		Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());

		final FormValidation form2 = descriptor.doCreateHubWrapperProject(
				testProperties.getProperty("TEST_CREATE_PROJECT"), "${BUILD_NUMBER}", "DEVELOPMENT", "EXTERNAL");

		Assert.assertEquals(FormValidation.Kind.WARNING, form2.kind);
		Assert.assertEquals(form2.getMessage(), Messages.HubBuildScan_getProjectVersionContainsVariable());

		final FormValidation form3 = descriptor.doCreateHubWrapperProject("${JOB_NAME}", "${BUILD_NUMBER}",
				"DEVELOPMENT", "EXTERNAL");

		Assert.assertEquals(FormValidation.Kind.WARNING, form3.kind);
		Assert.assertEquals(form3.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());
	}

	@Test
	public void testCheckForProjectNameVariable() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();

		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId("FAKE ID");
		hubServerInfo.setServerUrl("FAKE SERVER");
		addHubServerInfo(hubServerInfo);

		final FormValidation form = descriptor.doCheckHubWrapperProjectName("${JOB_NAME}", null);
		Assert.assertEquals(FormValidation.Kind.WARNING, form.kind);
		Assert.assertEquals(form.getMessage(), Messages.HubBuildScan_getProjectNameContainsVariable());
	}

	@Test
	public void testCheckForProjectNameNotExistent() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		addHubServerInfo(hubServerInfo);

		final FormValidation form = descriptor.doCheckHubWrapperProjectName(PROJECT_NAME_NOT_EXISTING, null);
		Assert.assertEquals(FormValidation.Kind.ERROR, form.kind);
		Assert.assertEquals(form.getMessage(),
				Messages.HubBuildScan_getProjectNonExistingIn_0_(testProperties.getProperty("TEST_HUB_SERVER_URL")));
	}

	@Test
	public void testCheckForProjectReleaseNotExistent() throws Exception, InterruptedException {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		addHubServerInfo(hubServerInfo);

		try {
			final FormValidation form = descriptor.doCreateHubWrapperProject(
					testProperties.getProperty("TEST_CREATE_PROJECT"),
					testProperties.getProperty("TEST_CREATE_VERSION"), "DEVELOPMENT", "EXTERNAL");
			Assert.assertEquals(FormValidation.Kind.OK, form.kind);
			Assert.assertEquals(Messages.HubBuildScan_getProjectAndVersionCreated(), form.getMessage());
			Thread.sleep(2000);
			final FormValidation form2 = descriptor.doCheckHubWrapperProjectVersion(PROJECT_RELEASE_NOT_EXISTING,
					testProperties.getProperty("TEST_CREATE_PROJECT"));
			Assert.assertEquals(FormValidation.Kind.ERROR, form2.kind);
			Assert.assertTrue(form2.getMessage(), form2.getMessage().contains(Messages
					.HubBuildScan_getVersionNonExistingIn_0_(testProperties.getProperty("TEST_CREATE_PROJECT"), "")));
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(testProperties.getProperty("TEST_CREATE_PROJECT")));
		}
	}

	@Test
	public void testDoFillHubWrapperVersionPhaseItems() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final ListBoxModel list = descriptor.doFillHubWrapperVersionPhaseItems();
		assertTrue(list.size() == 5);
	}

	@Test
	public void testDoFillHubWrapperVersionDistItems() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final ListBoxModel list = descriptor.doFillHubWrapperVersionDistItems();
		assertTrue(list.size() == 4);
	}

	@Test
	public void testDoAutoCompleteHubWrapperProjectNameNotExistent() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		addHubServerInfo(hubServerInfo);

		final AutoCompletionCandidates matches = descriptor
				.doAutoCompleteHubWrapperProjectName(PROJECT_NAME_NOT_EXISTING);
		assertTrue(matches.getValues().size() == 0);
	}

	@Test
	public void testDoAutoCompleteHubWrapperProjectName() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		addHubServerInfo(hubServerInfo);

		final AutoCompletionCandidates matches = descriptor.doAutoCompleteHubWrapperProjectName("j");
		assertTrue(matches.getValues().size() > 0);
	}

	@Test
	public void testGetServerInfo() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore(
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		addHubServerInfo(hubServerInfo);

		final HubServerInfo testServerInfo = descriptor.getHubServerInfo();

		assertNotNull(testServerInfo);
		assertEquals(testProperties.getProperty("TEST_USERNAME"), testServerInfo.getUsername());
		assertEquals(testProperties.getProperty("TEST_PASSWORD"), testServerInfo.getPassword());
	}

	@Test
	public void testIsApplicable() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		final FreeStyleProject project = j.createFreeStyleProject("test");
		assertTrue(descriptor.isApplicable(project));
		final MavenModuleSet mavenProject = j.createMavenProject();
		assertTrue(!descriptor.isApplicable(mavenProject));
	}

	@Test
	public void testGetDisplayName() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		assertTrue(StringUtils.isBlank(descriptor.getDisplayName()));
	}

	@Test
	public void testGetPluginVersion() throws Exception {
		final BDBuildWrapperDescriptor descriptor = new BDBuildWrapperDescriptor();
		assertTrue(StringUtils.isNotBlank(descriptor.getPluginVersion()));
	}

}
