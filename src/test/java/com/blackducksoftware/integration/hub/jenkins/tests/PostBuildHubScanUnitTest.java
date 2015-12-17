package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.StreamBuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.slaves.DumbSlave;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubScan;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.ScanInstallation;
import com.blackducksoftware.integration.hub.jenkins.ScanJobs;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.IScanToolMissingException;
import com.blackducksoftware.integration.hub.jenkins.tests.utils.TestLogger;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.google.common.base.Charsets;

public class PostBuildHubScanUnitTest {

    private static final String CLI_VERSION = "2.1.2";

    private static final String TEST_CLI_PATH = "/lib/scan.cli-" + CLI_VERSION + "-standalone.jar";

    private static String VALID_CREDENTIAL = "Valid Credential Id";

    private static String VALID_SERVERURL = "http://integration-hub";

    private static String basePath;

    private static String iScanInstallPath;

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
        basePath = ScanIntegrationTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        basePath = basePath.substring(0, basePath.indexOf("/target"));
        basePath = basePath + "/test-workspace";
        iScanInstallPath = basePath + "/scan.cli-" + CLI_VERSION;

        byteOutput = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(byteOutput);
        listener = new StreamBuildListener(ps, Charsets.UTF_8);
    }

    @After
    public void resetOutputStream() {
        byteOutput.reset();
    }

    // These test the public methods of this class, anything not tested here should be covered in the integration tests

    // validateScanTargets
    @Test
    public void testValidateScanTargetsTwoExistingInWorkspace() throws Exception {
        File createdFolder = folder.newFolder("newfolder");
        File createdFile = folder.newFile("myfilefile.txt");
        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class, Mockito.CALLS_REAL_METHODS);
        File workspace = new File(createdFolder.getCanonicalPath() + "/..");
        VirtualChannel nullChannel = null;
        when(mockpbScan.getWorkingDirectory()).thenReturn(new FilePath(nullChannel, workspace.getCanonicalPath()));
        ArrayList<String> scanTargets = new ArrayList<String>();
        scanTargets.add(createdFolder.getCanonicalPath());
        scanTargets.add(createdFile.getCanonicalPath());

        TestLogger logger = new TestLogger(listener);
        Assert.assertTrue(mockpbScan.validateScanTargets(logger, scanTargets, nullChannel));
        String output = logger.getOutputString();
        Assert.assertTrue(output, output.contains("Scan target exists at : "));
    }

    @Test
    public void testValidateScanTargetsTwoNotExisting() throws Exception {
        exception.expect(IOException.class);
        exception.expectMessage("Scan target could not be found :");
        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class, Mockito.CALLS_REAL_METHODS);
        VirtualChannel nullChannel = null;
        when(mockpbScan.getWorkingDirectory()).thenReturn(new FilePath(nullChannel, "/"));

        ArrayList<String> scanTargets = new ArrayList<String>();
        scanTargets.add("/ASSERT/NOT/EXISTING");
        scanTargets.add("/RE-ASSERT/Not/EXISTING");

        mockpbScan.validateScanTargets(new TestLogger(listener), scanTargets, nullChannel);
    }

    @Test
    public void testValidateScanTargetsExistingOutsideWorkspace() throws Exception {
        exception.expect(HubConfigurationException.class);
        exception.expectMessage("Can not scan targets outside of the workspace.");
        File createdFolder = folder.newFolder("newfolder");
        File testWorkspace = folder.newFolder("workspace");
        File createdFile = folder.newFile("myfilefile.txt");
        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class, Mockito.CALLS_REAL_METHODS);
        VirtualChannel nullChannel = null;
        when(mockpbScan.getWorkingDirectory()).thenReturn(new FilePath(nullChannel, testWorkspace.getCanonicalPath()));

        ArrayList<String> scanTargets = new ArrayList<String>();
        scanTargets.add(createdFolder.getCanonicalPath());
        scanTargets.add(createdFile.getCanonicalPath());

        TestLogger logger = new TestLogger(listener);
        mockpbScan.validateScanTargets(logger, scanTargets, nullChannel);
    }

    // getIScanScript
    @Test
    public void testGetIScanScriptOnMaster() throws Exception {
        // getIscanScript with empty nodeName (indicates master), with valid iScan installation configured and selected,
        // and with the script existing

        AbstractBuild mockBuild = mock(AbstractBuild.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockBuild.getBuiltOn().getNodeName()).thenReturn("");

        DumbSlave slave = j.createSlave();
        slave.setNodeName("");
        Node node = slave;
        when(mockBuild.getBuiltOn()).thenReturn(node);

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);
        ScanInstallation[] iScanInstallations = new ScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        PostBuildHubScan pbScan = new PostBuildHubScan(null, "default", false, null, null, null, null, "4096");

        TestLogger logger = new TestLogger(listener);
        FilePath script = pbScan.getScanCLI(iScanInstallations, logger, mockBuild);
        Assert.assertTrue(script.exists());
        Assert.assertTrue(script.getRemote().equals(iScanInstallPath + TEST_CLI_PATH));
        String output = logger.getOutputString();
        Assert.assertTrue(output, output.contains("Running on : master"));
        Assert.assertTrue(output, output.contains("Using this BlackDuck Scan CLI at : "));
    }

    @Test
    public void testGetIScanScriptOnSlave() throws Exception {
        // getIscanScript with nodeName "Slave machine", with valid iScan installation configured and selected,
        // and with the script existing

        AbstractBuild mockBuild = mock(AbstractBuild.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockBuild.getBuiltOn().getNodeName()).thenReturn("");
        DumbSlave slave = j.createSlave("Slave machine", null);
        slave.setNodeName("testSlave");
        Node node = slave;
        when(mockBuild.getBuiltOn()).thenReturn(node);

        // IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        ScanInstallation mockIScanInstall = mock(ScanInstallation.class);
        when(mockIScanInstall.getName()).thenReturn("default");
        when(mockIScanInstall.getHome()).thenReturn(iScanInstallPath);
        when(mockIScanInstall.forNode(Mockito.any(Node.class), Mockito.any(BuildListener.class))).thenReturn(mockIScanInstall);
        when(mockIScanInstall.getCLI(Mockito.any(VirtualChannel.class))).thenCallRealMethod();
        when(mockIScanInstall.getExists(Mockito.any(VirtualChannel.class), Mockito.any(IntLogger.class))).thenCallRealMethod();
        ScanInstallation[] iScanInstallations = new ScanInstallation[1];
        iScanInstallations[0] = mockIScanInstall;

        // iScan.forNode(build.getBuiltOn(), listener);

        PostBuildHubScan pbScan = new PostBuildHubScan(null, "default", false, null, null, null, null, "4096");

        TestLogger logger = new TestLogger(listener);
        FilePath script = pbScan.getScanCLI(iScanInstallations, logger, mockBuild);
        Assert.assertTrue(script.exists());
        Assert.assertTrue(script.getRemote().equals(iScanInstallPath + TEST_CLI_PATH));
        String output = logger.getOutputString();
        Assert.assertTrue(output, output.contains("Running on : testSlave"));
        Assert.assertTrue(output, output.contains("Using this BlackDuck Scan CLI at : "));
    }

    @Test
    public void testGetIScanScriptNoiScanInstallations() throws Exception {
        // getIscanScript with empty nodeName (indicates master), with no iScan installation configured and one
        // selected, and with the script existing

        exception.expect(HubConfigurationException.class);
        exception.expectMessage("You need to select which BlackDuck Scan installation to use.");

        ScanInstallation[] iScanInstallations = new ScanInstallation[0];

        PostBuildHubScan pbScan = new PostBuildHubScan(null, "default", false, null, null, null, null, "4096");

        TestLogger logger = new TestLogger(listener);
        FilePath script = pbScan.getScanCLI(iScanInstallations, logger, null);
        Assert.assertTrue(script.exists());
        Assert.assertTrue(script.getRemote().equals(iScanInstallPath + TEST_CLI_PATH));
    }

    @Test
    public void testGetIScanScriptDoesntExist() throws Exception {
        // getIscanScript with empty nodeName (indicates master), with valid iScan installation configured and selected,
        // and with the script not existing

        exception.expect(IScanToolMissingException.class);
        exception.expectMessage("Could not find the CLI file to execute at : '");

        AbstractBuild mockBuild = mock(AbstractBuild.class);

        DumbSlave slave = j.createSlave();
        slave.setNodeName("");
        Node node = slave;
        when(mockBuild.getBuiltOn()).thenReturn(node);

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath + "/FAKE/PATH/scan.cli.jar", null);
        ScanInstallation[] iScanInstallations = new ScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        PostBuildHubScan pbScan = new PostBuildHubScan(null, "default", false, null, null, null, null, "4096");

        TestLogger logger = new TestLogger(listener);
        pbScan.getScanCLI(iScanInstallations, logger, mockBuild);
    }

    // validateConfiguration
    @Test
    public void testValidateConfigurationValid() throws Exception {
        // validateConfiguration with correct IScanInstallations and IScanJobs

        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(VALID_CREDENTIAL);
        hubServerInfo.setServerUrl(VALID_SERVERURL);
        descriptor.setHubServerInfo(hubServerInfo);

        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class);
        when(mockpbScan.getDescriptor()).thenReturn(descriptor);
        when(mockpbScan.validateConfiguration(Mockito.any(ScanInstallation[].class), Mockito.any(ScanJobs[].class))).thenCallRealMethod();

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);
        ScanInstallation[] iScanInstallations = new ScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        ScanJobs oneScan = new ScanJobs(basePath);
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        Assert.assertTrue(mockpbScan.validateConfiguration(iScanInstallations, scans));
    }

    @Test
    public void testValidateConfigurationNoiScanInstallations() throws Exception {
        // validateConfiguration with no IScanInstallations and correct IScanJobs

        exception.expect(IScanToolMissingException.class);
        exception.expectMessage("Could not find an Black Duck Scan Installation to use.");

        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class);
        when(mockpbScan.validateConfiguration(Mockito.any(ScanInstallation[].class), Mockito.any(ScanJobs[].class))).thenCallRealMethod();

        ScanInstallation[] iScanInstallations = new ScanInstallation[0];

        ScanJobs oneScan = new ScanJobs(basePath);
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        Assert.assertTrue(mockpbScan.validateConfiguration(iScanInstallations, scans));
    }

    @Test
    public void testValidateConfigurationNulliScanInstallations() throws Exception {
        // validateConfiguration with null IScanInstallations and correct IScanJobs

        exception.expect(IScanToolMissingException.class);
        exception.expectMessage("Could not find an Black Duck Scan Installation to use.");

        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class);
        when(mockpbScan.validateConfiguration(Mockito.any(ScanInstallation[].class), Mockito.any(ScanJobs[].class))).thenCallRealMethod();

        ScanJobs oneScan = new ScanJobs(basePath);
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        Assert.assertTrue(mockpbScan.validateConfiguration(null, scans));
    }

    @Test
    public void testValidateConfigurationNoIScanJobs() throws Exception {
        // validateConfiguration with correct IScanInstallations and no IScanJobs

        exception.expect(HubConfigurationException.class);
        exception.expectMessage("Could not find any targets to scan.");

        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class);
        when(mockpbScan.validateConfiguration(Mockito.any(ScanInstallation[].class), Mockito.any(ScanJobs[].class))).thenCallRealMethod();

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);
        ScanInstallation[] iScanInstallations = new ScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        ScanJobs[] scans = new ScanJobs[0];

        Assert.assertTrue(mockpbScan.validateConfiguration(iScanInstallations, scans));
    }

    @Test
    public void testValidateConfigurationNullIScanJobs() throws Exception {
        // validateConfiguration with correct IScanInstallations and null IScanJobs

        exception.expect(HubConfigurationException.class);
        exception.expectMessage("Could not find any targets to scan.");

        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class);
        when(mockpbScan.validateConfiguration(Mockito.any(ScanInstallation[].class), Mockito.any(ScanJobs[].class))).thenCallRealMethod();

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);
        ScanInstallation[] iScanInstallations = new ScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        Assert.assertTrue(mockpbScan.validateConfiguration(iScanInstallations, null));
    }

    @Test
    public void testValidateConfigurationNoServerURL() throws Exception {
        // validateConfiguration with correct IScanInstallations and IScanJobs

        exception.expect(HubConfigurationException.class);
        exception.expectMessage("No Hub URL was provided.");

        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(VALID_CREDENTIAL);
        hubServerInfo.setServerUrl("");
        descriptor.setHubServerInfo(hubServerInfo);

        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class);
        when(mockpbScan.getDescriptor()).thenReturn(descriptor);
        when(mockpbScan.validateConfiguration(Mockito.any(ScanInstallation[].class), Mockito.any(ScanJobs[].class))).thenCallRealMethod();

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);
        ScanInstallation[] iScanInstallations = new ScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        ScanJobs oneScan = new ScanJobs(basePath);
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        Assert.assertTrue(mockpbScan.validateConfiguration(iScanInstallations, scans));
    }

    @Test
    public void testValidateConfigurationNullServerURL() throws Exception {
        // validateConfiguration with correct IScanInstallations and IScanJobs

        exception.expect(HubConfigurationException.class);
        exception.expectMessage("No Hub URL was provided.");

        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(VALID_CREDENTIAL);
        hubServerInfo.setServerUrl(null);
        descriptor.setHubServerInfo(hubServerInfo);

        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class);
        when(mockpbScan.getDescriptor()).thenReturn(descriptor);
        when(mockpbScan.validateConfiguration(Mockito.any(ScanInstallation[].class), Mockito.any(ScanJobs[].class))).thenCallRealMethod();

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);
        ScanInstallation[] iScanInstallations = new ScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        ScanJobs oneScan = new ScanJobs(basePath);
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        Assert.assertTrue(mockpbScan.validateConfiguration(iScanInstallations, scans));
    }

    @Test
    public void testValidateConfigurationNoCredential() throws Exception {
        // validateConfiguration with correct IScanInstallations and IScanJobs

        exception.expect(HubConfigurationException.class);
        exception.expectMessage("No credentials could be found to connect to the Hub.");

        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId("");
        hubServerInfo.setServerUrl(VALID_SERVERURL);
        descriptor.setHubServerInfo(hubServerInfo);

        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class);
        when(mockpbScan.getDescriptor()).thenReturn(descriptor);
        when(mockpbScan.validateConfiguration(Mockito.any(ScanInstallation[].class), Mockito.any(ScanJobs[].class))).thenCallRealMethod();

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);
        ScanInstallation[] iScanInstallations = new ScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        ScanJobs oneScan = new ScanJobs(basePath);
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        mockpbScan.validateConfiguration(iScanInstallations, scans);
    }

    @Test
    public void testValidateConfigurationNullCredential() throws Exception {
        // validateConfiguration with correct IScanInstallations and IScanJobs

        exception.expect(HubConfigurationException.class);
        exception.expectMessage("No credentials could be found to connect to the Hub.");

        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        HubServerInfo hubServerInfo = new HubServerInfo();
        hubServerInfo.setCredentialsId(null);
        hubServerInfo.setServerUrl(VALID_SERVERURL);
        descriptor.setHubServerInfo(hubServerInfo);

        PostBuildHubScan mockpbScan = mock(PostBuildHubScan.class);
        when(mockpbScan.getDescriptor()).thenReturn(descriptor);
        when(mockpbScan.validateConfiguration(Mockito.any(ScanInstallation[].class), Mockito.any(ScanJobs[].class))).thenCallRealMethod();

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);
        ScanInstallation[] iScanInstallations = new ScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        ScanJobs oneScan = new ScanJobs(basePath);
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        mockpbScan.validateConfiguration(iScanInstallations, scans);
    }

    @Test
    public void testHandleVariableReplacementVariableUndefined() throws Exception {
        exception.expect(BDJenkinsHubPluginException.class);
        exception
                .expectMessage("Variable was not properly replaced. Value : ${JOB_NAME}, Result : ${JOB_NAME}. Make sure the variable has been properly defined.");

        PostBuildHubScan postScan = new PostBuildHubScan(null, null, false, null, null, null, null, null);
        Map<String, String> emptyVariables = new HashMap<String, String>();
        postScan.handleVariableReplacement(emptyVariables, "${JOB_NAME}");
    }

    @Test
    public void testHandleVariableReplacementVariable() throws Exception {
        PostBuildHubScan postScan = new PostBuildHubScan(null, null, false, null, null, null, null, null);
        Map<String, String> emptyVariables = new HashMap<String, String>();
        emptyVariables.put("JOB_NAME", "Test Job");
        assertEquals("Test Job", postScan.handleVariableReplacement(emptyVariables, "${JOB_NAME}"));
    }

    @Test
    public void testHandleVariableReplacementVariableNull() throws Exception {
        PostBuildHubScan postScan = new PostBuildHubScan(null, null, false, null, null, null, null, null);
        Map<String, String> emptyVariables = new HashMap<String, String>();
        assertNull(postScan.handleVariableReplacement(null, null));
    }
}
