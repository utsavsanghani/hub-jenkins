package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import hudson.model.StreamBuildListener;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.slaves.DumbSlave;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.ScanJobs;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubScan;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.tests.utils.TestLogger;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.google.common.base.Charsets;

public class PostBuildHubScanUnitTest {

	private static String VALID_CREDENTIAL = "Valid Credential Id";

	private static String VALID_SERVERURL = "http://integration-hub";

	private static String basePath;

	private static Properties testProperties;

	@Rule
	public JenkinsRule j = new JenkinsRule();

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static StreamBuildListener listener;

	private static ByteArrayOutputStream byteOutput;

	@BeforeClass
	public static void init() {
		basePath = PostBuildHubScanUnitTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		basePath = basePath.substring(0, basePath.indexOf("/target"));
		basePath = basePath + "/test-workspace";

		byteOutput = new ByteArrayOutputStream();
		final PrintStream ps = new PrintStream(byteOutput);
		listener = new StreamBuildListener(ps, Charsets.UTF_8);

		testProperties = new Properties();
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final InputStream is = classLoader.getResourceAsStream("test.properties");
		try {
			testProperties.load(is);
		} catch (final IOException e) {
			System.err.println("reading test.properties failed!");
		}
	}

	@Before
	public void resetServerInfo() {
		HubServerInfoSingleton.getInstance().setServerInfo(null);

	}

	@After
	public void resetOutputStream() {
		byteOutput.reset();
	}

	@Test
	public void testConstructor() throws Exception {
		PostBuildHubScan pbScan = new PostBuildHubScan(null, false, null, null, null, null, null, false, null);
		assertNull(pbScan.getScans());
		assertTrue(!pbScan.getSameAsBuildWrapper());
		assertNull(pbScan.getHubProjectName());
		assertNull(pbScan.getHubProjectVersion());
		assertNull(pbScan.getHubVersionPhase());
		assertNull(pbScan.getHubVersionDist());
		assertEquals(pbScan.getDefaultMemory(), pbScan.getScanMemory());
		assertTrue(!pbScan.getShouldGenerateHubReport());
		assertEquals(pbScan.getDefaultReportWaitTime(), pbScan.getReportMaxiumWaitTime());

		final String testString = "testString";
		final ScanJobs oneScan = new ScanJobs("");
		final ScanJobs[] scans = new ScanJobs[1];
		scans[0] = oneScan;
		pbScan = new PostBuildHubScan(scans, true, testString, testString, testString, testString, "Not Number", true, "Not Number");
		assertArrayEquals(pbScan.getScans(), scans);
		assertTrue(pbScan.getSameAsBuildWrapper());
		assertEquals(testString, pbScan.getHubProjectName());
		assertEquals(testString, pbScan.getHubProjectVersion());
		assertEquals(testString, pbScan.getHubVersionPhase());
		assertEquals(testString, pbScan.getHubVersionDist());
		assertEquals(pbScan.getDefaultMemory(), pbScan.getScanMemory());
		assertTrue(pbScan.getShouldGenerateHubReport());
		assertEquals(pbScan.getDefaultReportWaitTime(), pbScan.getReportMaxiumWaitTime());

		pbScan = new PostBuildHubScan(null, false, null, null, null, null, "9001", false, "66");
		assertNull(pbScan.getScans());
		assertTrue(!pbScan.getSameAsBuildWrapper());
		assertNull(pbScan.getHubProjectName());
		assertNull(pbScan.getHubProjectVersion());
		assertNull(pbScan.getHubVersionPhase());
		assertNull(pbScan.getHubVersionDist());
		assertEquals("9001", pbScan.getScanMemory());
		assertTrue(!pbScan.getShouldGenerateHubReport());
		assertEquals("66", pbScan.getReportMaxiumWaitTime());
	}

	// These test the public methods of this class, anything not tested here should be covered in the integration tests

	// validateScanTargets
	@Test
	public void testValidateScanTargetsTwoExistingInWorkspace() throws Exception {
		final File createdFolder = folder.newFolder("newfolder");
		final File createdFile = folder.newFile("myfilefile.txt");
		final PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class, Mockito.CALLS_REAL_METHODS);
		final File workspace = new File(createdFolder.getCanonicalPath() + "/..");
		final VirtualChannel nullChannel = null;
		final ArrayList<String> scanTargets = new ArrayList<String>();
		scanTargets.add(createdFolder.getCanonicalPath());
		scanTargets.add(createdFile.getCanonicalPath());

		final TestLogger logger = new TestLogger(listener);
		assertTrue(mockpbScan.validateScanTargets(logger, scanTargets,workspace.getCanonicalPath(), nullChannel));
		final String output = logger.getOutputString();
		assertTrue(output, output.contains("Scan target exists at : "));
	}

	@Test
	public void testValidateScanTargetsTwoNotExisting() throws Exception {
		exception.expect(IOException.class);
		exception.expectMessage("Scan target could not be found :");
		final PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class, Mockito.CALLS_REAL_METHODS);
		final VirtualChannel nullChannel = null;

		final ArrayList<String> scanTargets = new ArrayList<String>();
		scanTargets.add("/ASSERT/NOT/EXISTING");
		scanTargets.add("/RE-ASSERT/Not/EXISTING");

		mockpbScan.validateScanTargets(new TestLogger(listener), scanTargets,"/", nullChannel);
	}

	@Test
	public void testValidateScanTargetsExistingOutsideWorkspace() throws Exception {
		exception.expect(HubConfigurationException.class);
		exception.expectMessage("Can not scan targets outside of the workspace.");
		final File createdFolder = folder.newFolder("newfolder");
		final File testWorkspace = folder.newFolder("workspace");
		final File createdFile = folder.newFile("myfilefile.txt");
		final PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class, Mockito.CALLS_REAL_METHODS);
		final VirtualChannel nullChannel = null;

		final ArrayList<String> scanTargets = new ArrayList<String>();
		scanTargets.add(createdFolder.getCanonicalPath());
		scanTargets.add(createdFile.getCanonicalPath());

		final TestLogger logger = new TestLogger(listener);
		mockpbScan.validateScanTargets(logger, scanTargets,testWorkspace.getCanonicalPath(), nullChannel);
	}

	@Test
	public void testGetIScanScriptNoServerInfo() throws Exception {
		// getIscanScript with empty nodeName (indicates master), with valid iScan installation configured and selected,
		// and with the script existing
		final DumbSlave slave = j.createOnlineSlave();
		final Node node = j.getInstance().getNode(slave.getNodeName());

		File toolsDir = j.getInstance().getRootDir();
		toolsDir = new File(toolsDir, "tools");

		final PostBuildHubScan pbScan = new PostBuildHubScan(null, false, null, null, null, null, "4096", false, "0");

		final TestLogger logger = new TestLogger(listener);
		assertNull(pbScan.getScanCLI(logger, node, toolsDir.getAbsolutePath(), "TestHost"));
		final String output = logger.getOutputString();
		assertTrue(output, output.contains("Could not find the Hub server information."));
	}

	@Test
	public void testGetIScanScript() throws Exception {
		// getIscanScript with empty nodeName (indicates master), with valid iScan installation configured and selected,
		// and with the script existing
		final DumbSlave slave = j.createOnlineSlave();
		final Node node = j.getInstance().getNode(slave.getNodeName());

		File toolsDir = j.getInstance().getRootDir();
		toolsDir = new File(toolsDir, "tools");
		assertTrue(toolsDir.listFiles() == null);

		final CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
		final UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
				testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
		store.addCredentials(Domain.global(), credential);

		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(credential.getId());
		hubServerInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		final PostBuildHubScan pbScan = new PostBuildHubScan(null, false, null, null, null, null, "4096", false, "0");

		final TestLogger logger = new TestLogger(listener);
		final String scriptPath = pbScan.getScanCLI(logger, node, toolsDir.getAbsolutePath(), "TestHost");
		final File cli = new File(scriptPath);
		assertTrue(cli.exists());
		assertTrue(cli.getAbsolutePath().contains(toolsDir.getAbsolutePath()));
		final String output = logger.getOutputString();
		assertTrue(output, output.contains("Using this BlackDuck scan CLI at : "));
	}

	// validateConfiguration
	@Test
	public void testValidateConfigurationValid() throws Exception {
		// validateConfiguration with correct IHubScanInstallations and IScanJobs

		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(VALID_CREDENTIAL);
		hubServerInfo.setServerUrl(VALID_SERVERURL);
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		final PostBuildHubScan postBuildScan = new PostBuildHubScan(null, false, null, null, null, null, "4096", false, "0");

		assertTrue(postBuildScan.validateGlobalConfiguration());
	}

	@Test
	public void testValidateConfigurationNoServerURL() throws Exception {
		// validateConfiguration with correct IHubScanInstallations and IScanJobs

		exception.expect(HubConfigurationException.class);
		exception.expectMessage("No Hub URL was provided.");

		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(VALID_CREDENTIAL);
		hubServerInfo.setServerUrl("");
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		final PostBuildHubScan postBuildScan = new PostBuildHubScan(null, false, null, null, null, null, "4096", false, "0");

		assertTrue(postBuildScan.validateGlobalConfiguration());
	}

	@Test
	public void testValidateConfigurationNullServerURL() throws Exception {
		// validateConfiguration with correct IHubScanInstallations and IScanJobs

		exception.expect(HubConfigurationException.class);
		exception.expectMessage("No Hub URL was provided.");

		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(VALID_CREDENTIAL);
		hubServerInfo.setServerUrl(null);
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		final PostBuildHubScan postBuildScan = new PostBuildHubScan(null, false, null, null, null, null, "4096", false, "0");

		assertTrue(postBuildScan.validateGlobalConfiguration());
	}

	@Test
	public void testValidateConfigurationNoCredential() throws Exception {
		// validateConfiguration with correct IHubScanInstallations and IScanJobs

		exception.expect(HubConfigurationException.class);
		exception.expectMessage("No credentials could be found to connect to the Hub.");

		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId("");
		hubServerInfo.setServerUrl(VALID_SERVERURL);
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		final PostBuildHubScan postBuildScan = new PostBuildHubScan(null, false, null, null, null, null, "4096", false, "0");

		postBuildScan.validateGlobalConfiguration();
	}

	@Test
	public void testValidateConfigurationNullCredential() throws Exception {
		// validateConfiguration with correct IHubScanInstallations and IScanJobs

		exception.expect(HubConfigurationException.class);
		exception.expectMessage("No credentials could be found to connect to the Hub.");

		final HubServerInfo hubServerInfo = new HubServerInfo();
		hubServerInfo.setCredentialsId(null);
		hubServerInfo.setServerUrl(VALID_SERVERURL);
		HubServerInfoSingleton.getInstance().setServerInfo(hubServerInfo);

		final PostBuildHubScan postBuildScan = new PostBuildHubScan(null, false, null, null, null, null, "4096", false, "0");

		postBuildScan.validateGlobalConfiguration();
	}

	@Test
	public void testHandleVariableReplacementVariableUndefined() throws Exception {
		exception.expect(BDJenkinsHubPluginException.class);
		exception
		.expectMessage("Variable was not properly replaced. Value : ${JOB_NAME}, Result : ${JOB_NAME}. Make sure the variable has been properly defined.");

		final PostBuildHubScan postScan = new PostBuildHubScan(null, false, null, null, null, null, null, false, "0");
		final Map<String, String> emptyVariables = new HashMap<String, String>();
		postScan.handleVariableReplacement(emptyVariables, "${JOB_NAME}");
	}

	@Test
	public void testHandleVariableReplacementVariable() throws Exception {
		final PostBuildHubScan postScan = new PostBuildHubScan(null, false, null, null, null, null, null, false, "0");
		final Map<String, String> emptyVariables = new HashMap<String, String>();
		emptyVariables.put("JOB_NAME", "Test Job");
		assertEquals("Test Job", postScan.handleVariableReplacement(emptyVariables, "${JOB_NAME}"));
	}

	@Test
	public void testHandleVariableReplacementVariableNull() throws Exception {
		final PostBuildHubScan postScan = new PostBuildHubScan(null, false, null, null, null, null, null, false, "0");
		assertNull(postScan.handleVariableReplacement(null, null));
	}
}
