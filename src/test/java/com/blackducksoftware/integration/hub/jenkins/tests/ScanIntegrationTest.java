package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.api.VersionComparison;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubScan;
import com.blackducksoftware.integration.hub.jenkins.ScanJobs;
import com.blackducksoftware.integration.hub.jenkins.action.HubReportAction;
import com.blackducksoftware.integration.hub.jenkins.action.HubScanFinishedAction;
import com.blackducksoftware.integration.hub.jenkins.failure.HubFailureConditionStep;
import com.blackducksoftware.integration.hub.jenkins.tests.utils.JenkinsHubIntTestHelper;
import com.blackducksoftware.integration.hub.project.api.ProjectItem;
import com.blackducksoftware.integration.hub.version.api.DistributionEnum;
import com.blackducksoftware.integration.hub.version.api.PhaseEnum;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.ProxyConfiguration;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import jenkins.model.Jenkins;

public class ScanIntegrationTest {

	private static final String PASSWORD_WRONG = "Assert.failurePassword";

	private static final String USERNAME_NON_EXISTING = "Assert.failureUser";

	private static final String PROJECT_NAME_NOT_EXISTING = "Assert Project Does Not Exist";

	private static final String PROJECT_RELEASE_NOT_EXISTING = "Assert Release Does Not Exist";

	private static String basePath;

	private static String testWorkspace;

	private static Properties testProperties;

	private static JenkinsHubIntTestHelper restHelper;

	@Rule
	public JenkinsRule j = new JenkinsRule();

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@BeforeClass
	public static void init() throws Exception {
		basePath = ScanIntegrationTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		basePath = basePath.substring(0, basePath.indexOf(File.separator + "target"));
		basePath = basePath + File.separator + "test-workspace";
		testWorkspace = URLDecoder.decode(basePath + File.separator + "workspace", "UTF-8");

		testProperties = new Properties();
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final InputStream is = classLoader.getResourceAsStream("test.properties");
		try {
			testProperties.load(is);
		} catch (final IOException e) {
			System.err.println("reading test.properties failed!");
		}
		// p.load(new FileReader(new File("test.properties")));
		System.out.println(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		System.out.println(testProperties.getProperty("TEST_USERNAME"));
		System.out.println(testProperties.getProperty("TEST_PASSWORD"));

		restHelper = new JenkinsHubIntTestHelper(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		restHelper.setTimeout(300);
		restHelper.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		projectCleanup();
	}

	@Before
	public void resetServerInfo() {
		HubServerInfoSingleton.getInstance().setServerInfo(null);

	}

	@AfterClass
	public static void tearDown() {
		projectCleanup();
	}

	/**
	 * Cleans up any project that may be left over from interrupted tests.
	 *
	 */
	public static void projectCleanup() {
		try {
			final ProjectItem project = restHelper.getProjectByName(testProperties.getProperty("TEST_PROJECT"));
			if (project != null) {
				restHelper.deleteHubProject(project);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void completeRunthroughAndScan() throws IOException, InterruptedException, ExecutionException {
		final Jenkins jenkins = j.jenkins;

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());
		serverInfo.setTimeout(200);

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs[] scans = new ScanJobs[1];
		scans[0] = oneScan;

		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

		final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, null, null, null, null, "4096", false, "0");

		final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
		project.setCustomWorkspace(testWorkspace);

		project.getPublishersList().add(pbScan);

		final FreeStyleBuild build = project.scheduleBuild2(0).get();
		final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
		System.out.println(buildOutput);

		assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
		assertTrue(buildOutput, buildOutput.contains("Running on : master"));
		assertTrue(buildOutput, buildOutput.contains("BlackDuck scan directory:"));
		assertTrue(buildOutput, buildOutput.contains("directories in the BlackDuck scan directory"));
		assertTrue(buildOutput, buildOutput.contains("BlackDuck scan lib directory:"));
		assertTrue(buildOutput, buildOutput.contains("Using this BlackDuck scan CLI at : "));
		assertTrue(buildOutput, buildOutput.contains("Scan target exists at :"));
		final URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		assertTrue(buildOutput, buildOutput.contains("Using this Hub hostname : '" + url.getHost()));
		assertTrue(buildOutput, buildOutput.contains("Using this java installation : "));
		assertTrue(buildOutput, buildOutput.contains("Finished in"));
		assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
		assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
	}

	@Test
	public void completeRunthroughAndScanWithMapping() throws Exception {
		final Jenkins jenkins = j.jenkins;

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());
		serverInfo.setTimeout(200);

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
		final ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
		final ScanJobs[] scans = new ScanJobs[3];
		scans[0] = oneScan;
		scans[1] = twoScan;
		scans[2] = threeScan;

		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
		String projectUrl = null;
		try {
			projectUrl = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
			// Give server time to recognize the Project
			Thread.sleep(2000);
			final ProjectItem hubProject = restHelper.getProject(projectUrl);

			restHelper.createHubVersion(hubProject,
					testProperties.getProperty("TEST_VERSION"),
					PhaseEnum.DEVELOPMENT.name(),
					DistributionEnum.EXTERNAL.name());
			// Give server time to recognize the Version
			Thread.sleep(2000);

			final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
					testProperties.getProperty("TEST_VERSION"), null,
					null, "4096", false, "0");

			final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
			project.setCustomWorkspace(testWorkspace);

			project.getPublishersList().add(pbScan);

			final FreeStyleBuild build = project.scheduleBuild2(0).get();
			final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains(
					"You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
		} finally {
			restHelper.deleteHubProject(projectUrl);
		}
	}

	@Test
	public void completeRunthroughAndScanWithMappingAndGenerateReport() throws Exception {
		final Jenkins jenkins = j.jenkins;

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());
		serverInfo.setTimeout(200);

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
		final ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
		final ScanJobs fourScan = new ScanJobs("ch-simple-web");
		final ScanJobs fiveScan = new ScanJobs("ch-simple-web/simple-webapp");
		final ScanJobs[] scans = new ScanJobs[5];
		scans[0] = oneScan;
		scans[1] = twoScan;
		scans[2] = threeScan;
		scans[3] = fourScan;
		scans[4] = fiveScan;

		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
		String projectUrl = null;
		try {
			projectUrl = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
			// Give server time to recognize the Project
			Thread.sleep(2000);
			final ProjectItem hubProject = restHelper.getProject(projectUrl);

			restHelper.createHubVersion(hubProject, testProperties.getProperty("TEST_VERSION"),
					PhaseEnum.DEVELOPMENT.name(),
					DistributionEnum.EXTERNAL.name());
			// Give server time to recognize the Version
			Thread.sleep(2000);

			final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
					testProperties.getProperty("TEST_VERSION"), null,
					null, "4096", true, "5");

			final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
			project.setCustomWorkspace(testWorkspace);

			project.getPublishersList().add(pbScan);

			final FreeStyleBuild build = project.scheduleBuild2(0).get();
			final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("-> Generate Hub report : true"));
			assertTrue(buildOutput, buildOutput.contains("-> Maximum wait time for the BOM Update : 5 minutes"));
			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains(
					"You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));
			assertTrue(buildOutput, buildOutput.contains("The bom has been updated, generating the report."));
			assertTrue(buildOutput, buildOutput.contains("Finished retrieving the report."));

			final HubReportAction hubReportAction = build.getAction(HubReportAction.class);

			assertNotNull(hubReportAction);
			assertNotNull(hubReportAction.getReport());
			assertNotNull(hubReportAction.getReport().getAggregateBomViewEntries());
			assertTrue(!hubReportAction.getReport().getAggregateBomViewEntries().isEmpty());
			assertNotNull(hubReportAction.getReport().getAggregateBomViewEntries().get(0).getVulnerabilityRisk());
			assertNotNull(hubReportAction.getReleaseSummary());

			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
		} finally {
			restHelper.deleteHubProject(projectUrl);
		}

	}

	@Test
	public void completeRunthroughAndScanWithMappingAndFailureCondition() throws Exception {
		final Jenkins jenkins = j.jenkins;

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());
		serverInfo.setTimeout(200);

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
		final ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
		final ScanJobs fourScan = new ScanJobs("ch-simple-web");
		final ScanJobs fiveScan = new ScanJobs("ch-simple-web/simple-webapp");
		final ScanJobs[] scans = new ScanJobs[5];
		scans[0] = oneScan;
		scans[1] = twoScan;
		scans[2] = threeScan;
		scans[3] = fourScan;
		scans[4] = fiveScan;

		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
		String projectUrl = null;
		try {
			projectUrl = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
			// Give server time to recognize the Project
			Thread.sleep(2000);
			final ProjectItem hubProject = restHelper.getProject(projectUrl);

			restHelper.createHubVersion(hubProject, testProperties.getProperty("TEST_VERSION"),
					PhaseEnum.DEVELOPMENT.name(),
					DistributionEnum.EXTERNAL.name());
			// Give server time to recognize the Version
			Thread.sleep(2000);

			final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
					testProperties.getProperty("TEST_VERSION"), null,
					null, "4096", false, "0");
			final HubFailureConditionStep failureCond = new HubFailureConditionStep(true);

			final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
			project.setCustomWorkspace(testWorkspace);

			project.getPublishersList().add(pbScan);
			project.getPublishersList().add(failureCond);

			final FreeStyleBuild build = project.scheduleBuild2(0).get();
			final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("-> Generate Hub report : false"));
			assertTrue(buildOutput, buildOutput.contains("-> Maximum wait time for the BOM Update : 5 minutes"));
			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains(
					"You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));

			assertNull(build.getAction(HubReportAction.class));
			assertNotNull(build.getAction(HubScanFinishedAction.class));

			if (isHubOlderThanThisVersion("3.0.0")) {
				// server does not support policies
				assertTrue(buildOutput, buildOutput.contains("This version of the Hub does not have support for Policies."));
			} else {
				assertTrue(buildOutput, buildOutput.contains("bom entries to be In Violation of a defined Policy."));
				assertTrue(buildOutput, buildOutput.contains("bom entries to be In Violation of a defined Policy, but they have been overridden."));
				assertTrue(buildOutput, buildOutput.contains("bom entries to be Not In Violation of a defined Policy."));
			}
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
		} finally {
			restHelper.deleteHubProject(projectUrl);
		}

	}

	@Test
	public void completeRunthroughAndScanAndFailureConditionWithVariables() throws Exception {
		final Jenkins jenkins = j.jenkins;

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
				null, null, testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());
		serverInfo.setTimeout(200);

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
		final ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
		final ScanJobs fourScan = new ScanJobs("ch-simple-web");
		final ScanJobs fiveScan = new ScanJobs("ch-simple-web/simple-webapp");
		final ScanJobs[] scans = new ScanJobs[5];
		scans[0] = oneScan;
		scans[1] = twoScan;
		scans[2] = threeScan;
		scans[3] = fourScan;
		scans[4] = fiveScan;

		final String projectName = "Jenkins Hub Integration Variable Project Name";
		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
		try {
			final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false,
					"${JOB_NAME}", "${BUILD_NUMBER}", PhaseEnum.DEVELOPMENT.name(), DistributionEnum.EXTERNAL.name(),
					"4096", false, "5");
			final HubFailureConditionStep failureCond = new HubFailureConditionStep(true);

			final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, projectName);
			project.setCustomWorkspace(testWorkspace);

			project.getPublishersList().add(pbScan);
			project.getPublishersList().add(failureCond);

			final FreeStyleBuild build = project.scheduleBuild2(0).get();
			final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("-> Generate Hub report : false"));
			assertTrue(buildOutput, buildOutput.contains("-> Maximum wait time for the BOM Update : 5 minutes"));
			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains("You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));

			assertNull(build.getAction(HubReportAction.class));
			assertNotNull(build.getAction(HubScanFinishedAction.class));

			if (isHubOlderThanThisVersion("3.0.0")) {
				// server does not support policies
				assertTrue(buildOutput,
						buildOutput.contains("This version of the Hub does not have support for Policies."));
			} else {
				assertTrue(buildOutput, buildOutput.contains("bom entries to be In Violation of a defined Policy."));
				assertTrue(buildOutput, buildOutput.contains(
						"bom entries to be In Violation of a defined Policy, but they have been overridden."));
				assertTrue(buildOutput,
						buildOutput.contains("bom entries to be Not In Violation of a defined Policy."));
			}
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(projectName));
		}
	}

	@Test
	public void completeRunthroughAndScanWithMappingVariableProjectName() throws Exception {
		final Jenkins jenkins = j.jenkins;

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());
		serverInfo.setTimeout(200);

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
		final ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
		final ScanJobs[] scans = new ScanJobs[3];
		scans[0] = oneScan;
		scans[1] = twoScan;
		scans[2] = threeScan;
		final String projectName = "Jenkins Hub Integration Variable Project Name";
		try {
			HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

			final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, "${JOB_NAME}", testProperties.getProperty("TEST_VERSION"),
					PhaseEnum.DEVELOPMENT.name(),
					DistributionEnum.EXTERNAL.name(), "4096", false, "0");

			final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, projectName);
			project.setCustomWorkspace(testWorkspace);

			project.getPublishersList().add(pbScan);

			final FreeStyleBuild build = project.scheduleBuild2(0).get();
			final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains(
					"You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(projectName));
		}
	}

	@Test
	public void completeRunthroughAndScanWithMappingThroughProxy() throws Exception {
		final Jenkins jenkins = j.jenkins;

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());
		serverInfo.setTimeout(200);

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
		final ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
		final ScanJobs[] scans = new ScanJobs[3];
		scans[0] = oneScan;
		scans[1] = twoScan;
		scans[2] = threeScan;

		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
		String projectUrl = null;
		try {
			projectUrl = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
			// Give server time to recognize the Project
			Thread.sleep(2000);
			final ProjectItem hubProject = restHelper.getProject(projectUrl);

			restHelper.createHubVersion(hubProject, testProperties.getProperty("TEST_VERSION"),
					PhaseEnum.DEVELOPMENT.name(),
					DistributionEnum.EXTERNAL.name());
			// Give server time to recognize the Version
			Thread.sleep(2000);

			final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
					testProperties.getProperty("TEST_VERSION"), null,
					null, "4096", false, "0");

			jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST_PASSTHROUGH"),
					Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT_PASSTHROUGH")));

			final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
			project.setCustomWorkspace(testWorkspace);

			project.getPublishersList().add(pbScan);

			final FreeStyleBuild build = project.scheduleBuild2(0).get();

			final File logFile = build.getLogFile();
			System.out.println("Log File : " + logFile.getAbsolutePath() + "!!!!!!!!!!");

			final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyHost="));
			assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPort="));

			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains(
					"You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, buildOutput.contains("Using proxy: '" + testProperties.getProperty("TEST_PROXY_HOST_PASSTHROUGH") + "' at Port: '"
					+ testProperties.getProperty("TEST_PROXY_PORT_PASSTHROUGH") + "'"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));

		} finally {
			restHelper.deleteHubProject(projectUrl);
		}
	}


	@Test
	public void completeRunthroughAndScanWithMappingWithProxyIgnored() throws Exception {
		final Jenkins jenkins = j.jenkins;

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());
		serverInfo.setTimeout(200);

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
		final ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
		final ScanJobs[] scans = new ScanJobs[3];
		scans[0] = oneScan;
		scans[1] = twoScan;
		scans[2] = threeScan;
		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
		String projectUrl = null;
		try {
			projectUrl = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
			// Give server time to recognize the Project
			Thread.sleep(2000);
			final ProjectItem hubProject = restHelper.getProject(projectUrl);

			restHelper.createHubVersion(hubProject, testProperties.getProperty("TEST_VERSION"),
					PhaseEnum.DEVELOPMENT.name(),
					DistributionEnum.EXTERNAL.name());
			// Give server time to recognize the Version
			Thread.sleep(2000);

			final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
					testProperties.getProperty("TEST_VERSION"), null,
					null, "4096", false, "0");

			final URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));

			jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST_PASSTHROUGH"),
					Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT_PASSTHROUGH")), null, null, url.getHost());

			final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
			project.setCustomWorkspace(testWorkspace);

			project.getPublishersList().add(pbScan);

			final FreeStyleBuild build = project.scheduleBuild2(0).get();
			final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains(
					"You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, !buildOutput.contains("Using proxy: '" + testProperties.getProperty("TEST_PROXY_HOST") + "' at Port: '"
					+ testProperties.getProperty("TEST_PROXY_PORT") + "'"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));

		} finally {
			restHelper.deleteHubProject(projectUrl);
		}
	}

	private boolean isHubOlderThanThisVersion(final String version) throws IOException, BDRestException, URISyntaxException {
		VersionComparison compare = restHelper.compareWithHubVersion(version + "-SNAPSHOT");
		if (Integer.valueOf(0) == compare.getNumericResult()) {
			// same version, the Hub is not older than this
			return false;
		}
		compare = restHelper.compareWithHubVersion(version);
		if (Integer.valueOf(1) == compare.getNumericResult()) {
			// The version you have provided is newer than this Hub instance
			// The actual version of the Hub is less than the one specified
			return true;
		}
		return false;
	}

	@Test
	public void completeRunthroughAndScanWithMappingToNonExistentProject() throws Exception {
		try {
			final Jenkins jenkins = j.jenkins;

			final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
			final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
					testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
			store.addCredentials(Domain.global(), credential);

			final HubServerInfo serverInfo = new HubServerInfo();
			serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
			serverInfo.setCredentialsId(credential.getId());
			serverInfo.setTimeout(200);

			final ScanJobs oneScan = new ScanJobs("");
			final ScanJobs[] scans = new ScanJobs[1];
			scans[0] = oneScan;

			HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

			final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, PROJECT_NAME_NOT_EXISTING, PROJECT_RELEASE_NOT_EXISTING,
					PhaseEnum.DEVELOPMENT.name(),
					DistributionEnum.EXTERNAL.name(), "4096", false, "0");

			final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
			project.setCustomWorkspace(testWorkspace);

			project.getPublishersList().add(pbScan);

			final FreeStyleBuild build = project.scheduleBuild2(0).get();
			final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("Scan target exists at :"));

			// URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));

			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains(
					"You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
			assertTrue(buildOutput, buildOutput.contains("Finished: SUCCESS"));
		} finally {
			restHelper.deleteHubProject(restHelper.getProjectByName(PROJECT_NAME_NOT_EXISTING));
		}
	}

	@Test
	public void completeRunthroughAndScanWithMappingToNonExistentVersion() throws Exception {
		final Jenkins jenkins = j.jenkins;

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());
		serverInfo.setTimeout(200);

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs[] scans = new ScanJobs[1];
		scans[0] = oneScan;

		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
		String projectUrl = null;
		try {
			projectUrl = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
			// Give server time to recognize the Project
			Thread.sleep(2000);

			final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
					PROJECT_RELEASE_NOT_EXISTING,
					PhaseEnum.DEVELOPMENT.name(),
					DistributionEnum.EXTERNAL.name(),
					"4096", false, "0");

			final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
			project.setCustomWorkspace(testWorkspace);

			project.getPublishersList().add(pbScan);

			final FreeStyleBuild build = project.scheduleBuild2(0).get();
			final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("Scan target exists at :"));

			// URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));

			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains(
					"You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
			assertTrue(buildOutput, buildOutput.contains("Finished: SUCCESS"));
		} finally {
			restHelper.deleteHubProject(projectUrl);
		}
	}

	@Test
	public void completeRunthroughAndScanWithMappingScansAlreadyMapped() throws Exception {
		final Jenkins jenkins = j.jenkins;

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());
		serverInfo.setTimeout(200);

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
		final ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
		final ScanJobs[] scans = new ScanJobs[3];
		scans[0] = oneScan;
		scans[1] = twoScan;
		scans[2] = threeScan;

		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
		String projectUrl = null;
		try {
			projectUrl = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
			// Give server time to recognize the Project
			Thread.sleep(2000);
			final ProjectItem hubProject = restHelper.getProject(projectUrl);

			restHelper.createHubVersion(hubProject, testProperties.getProperty("TEST_VERSION"),
					PhaseEnum.DEVELOPMENT.name(),
					DistributionEnum.EXTERNAL.name());
			// Give server time to recognize the Version
			Thread.sleep(2000);

			final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
					testProperties.getProperty("TEST_VERSION"),
					PhaseEnum.DEVELOPMENT.name(), DistributionEnum.EXTERNAL.name(),
					"4096", false, "0");

			final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
			project.setCustomWorkspace(testWorkspace);

			project.getPublishersList().add(pbScan);

			// First Run scans and maps the scans to the Project Version
			FreeStyleBuild build = project.scheduleBuild2(0).get();
			String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains(
					"You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));

			// Second run, scans should already be mapped
			build = project.scheduleBuild2(0).get();
			buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
			System.out.println(buildOutput);

			assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
			assertTrue(buildOutput, buildOutput.contains("Finished in"));
			assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
			assertTrue(buildOutput, buildOutput.contains(
					"You can view the BlackDuck scan CLI logs at :"));
			assertTrue(buildOutput, buildOutput.contains("Found Project : "));
			assertTrue(buildOutput, buildOutput.contains("Found Version : "));
			assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
		} finally {
			restHelper.deleteHubProject(projectUrl);
		}
	}

	@Test
	public void scanInvalidPassword() throws IOException, InterruptedException, ExecutionException {
		final Jenkins jenkins = j.jenkins;

		final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
		project.setCustomWorkspace(testWorkspace);

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), PASSWORD_WRONG);
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs[] scans = new ScanJobs[1];
		scans[0] = oneScan;

		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

		final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, null, null, null, null, "4096", false, "0");

		project.getPublishersList().add(pbScan);

		final FreeStyleBuild build = project.scheduleBuild2(0).get();
		final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
		System.out.println(buildOutput);

		assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
		assertTrue(buildOutput, buildOutput.contains("Unauthorized (401)"));
	}

	@Test
	public void scanInvalidUser() throws IOException, InterruptedException, ExecutionException {
		final Jenkins jenkins = j.jenkins;

		final FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
		project.setCustomWorkspace(testWorkspace);

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				USERNAME_NON_EXISTING, testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo serverInfo = new HubServerInfo();
		serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		serverInfo.setCredentialsId(credential.getId());

		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs[] scans = new ScanJobs[1];
		scans[0] = oneScan;

		HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

		final PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, null, null, null, null, "4096", false, "0");

		project.getPublishersList().add(pbScan);

		final FreeStyleBuild build = project.scheduleBuild2(0).get();
		final String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
		System.out.println(buildOutput);

		assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
		assertTrue(buildOutput, buildOutput.contains("Unauthorized (401)"));
	}

}
