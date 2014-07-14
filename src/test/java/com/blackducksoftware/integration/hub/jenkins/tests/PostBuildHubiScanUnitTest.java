package com.blackducksoftware.integration.hub.jenkins.tests;

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
import java.util.Arrays;

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
import com.blackducksoftware.integration.hub.jenkins.IScanJobs;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.IScanInstallation;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubiScan;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.IScanToolMissingException;
import com.google.common.base.Charsets;

public class PostBuildHubiScanUnitTest {

    private static final String TEST_CLI_PATH = "/lib/scan.cli-1.14.0-SNAPSHOT-standalone.jar";

    private static String VALID_CREDENTIAL = "Valid Credential Id";

    private static String VALID_SERVERURL = "http://donald:8080";

    private static String basePath;

    private static String iScanInstallPath;

    private static StreamBuildListener listener;

    private static ByteArrayOutputStream byteOutput;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void init() {
        basePath = IntegrationTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        basePath = basePath.substring(0, basePath.indexOf("/target"));
        basePath = basePath + "/test-workspace";
        iScanInstallPath = basePath + "/scan.cli-1.14.0-SNAPSHOT";
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
    public void testValidateScanTargetsTwoExistingInWorkspace() throws IOException, HubConfigurationException, InterruptedException {
        File createdFolder = folder.newFolder("newfolder");
        File createdFile = folder.newFile("myfilefile.txt");
        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class, Mockito.CALLS_REAL_METHODS);
        File workspace = new File(createdFolder.getCanonicalPath() + "/..");
        when(mockpbScan.getWorkingDirectory()).thenReturn(workspace.getCanonicalPath());
        String[] scanTargets = { createdFolder.getCanonicalPath(), createdFile.getCanonicalPath() };
        Assert.assertTrue(mockpbScan.validateScanTargets(listener, null, Arrays.asList(scanTargets)));
        Assert.assertTrue(byteOutput.toString("UTF-8").contains("[DEBUG] : Scan target exists at : "));
    }

    @Test
    public void testValidateScanTargetsTwoNotExisting() throws IOException, HubConfigurationException, InterruptedException {
        exception.expect(IOException.class);
        exception.expectMessage("Scan target could not be found :");
        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class, Mockito.CALLS_REAL_METHODS);
        when(mockpbScan.getWorkingDirectory()).thenReturn("/");
        String[] scanTargets = { "/ASSERT/NOT/EXISTING", "/RE-ASSERT/Not/EXISTING" };
        mockpbScan.validateScanTargets(listener, null, Arrays.asList(scanTargets));
    }

    @Test
    public void testValidateScanTargetsExistingOutsideWorkspace() throws IOException, HubConfigurationException, InterruptedException {
        exception.expect(HubConfigurationException.class);
        exception.expectMessage("Can not scan targets outside of the workspace.");
        File createdFolder = folder.newFolder("newfolder");
        File testWorkspace = folder.newFolder("workspace");
        File createdFile = folder.newFile("myfilefile.txt");
        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class, Mockito.CALLS_REAL_METHODS);
        when(mockpbScan.getWorkingDirectory()).thenReturn(testWorkspace.getCanonicalPath());
        String[] scanTargets = { createdFolder.getCanonicalPath(), createdFile.getCanonicalPath() };
        mockpbScan.validateScanTargets(listener, null, Arrays.asList(scanTargets));
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

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        PostBuildHubiScan pbScan = new PostBuildHubiScan(null, "default", null, null);

        FilePath script = pbScan.getIScanCLI(iScanInstallations, listener, mockBuild);
        Assert.assertTrue(script.exists());
        Assert.assertTrue(script.getRemote().equals(iScanInstallPath + TEST_CLI_PATH));
        Assert.assertTrue(byteOutput.toString("UTF-8").contains("[DEBUG] : master"));
        Assert.assertTrue(byteOutput.toString("UTF-8").contains("[DEBUG] : Using this iScan CLI at : "));
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
        IScanInstallation mockIScanInstall = mock(IScanInstallation.class);
        when(mockIScanInstall.getName()).thenReturn("default");
        when(mockIScanInstall.getHome()).thenReturn(iScanInstallPath);
        when(mockIScanInstall.forNode(Mockito.any(Node.class), Mockito.any(BuildListener.class))).thenReturn(mockIScanInstall);
        when(mockIScanInstall.getCLI(Mockito.any(VirtualChannel.class))).thenCallRealMethod();
        when(mockIScanInstall.getExists(Mockito.any(VirtualChannel.class), Mockito.any(BuildListener.class))).thenCallRealMethod();
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = mockIScanInstall;

        // iScan.forNode(build.getBuiltOn(), listener);

        PostBuildHubiScan pbScan = new PostBuildHubiScan(null, "default", null, null);

        FilePath script = pbScan.getIScanCLI(iScanInstallations, listener, mockBuild);
        Assert.assertTrue(script.exists());
        Assert.assertTrue(script.getRemote().equals(iScanInstallPath + TEST_CLI_PATH));
        Assert.assertTrue(byteOutput.toString("UTF-8").contains("[DEBUG] : testSlave"));
        Assert.assertTrue(byteOutput.toString("UTF-8").contains("[DEBUG] : Using this iScan CLI at : "));
    }

    @Test
    public void testGetIScanScriptNoiScanInstallations() throws Exception {
        // getIscanScript with empty nodeName (indicates master), with no iScan installation configured and one
        // selected, and with the script existing

        exception.expect(HubConfigurationException.class);
        exception.expectMessage("You need to select which iScan installation to use.");

        IScanInstallation[] iScanInstallations = new IScanInstallation[0];

        PostBuildHubiScan pbScan = new PostBuildHubiScan(null, "default", null, null);

        FilePath script = pbScan.getIScanCLI(iScanInstallations, listener, null);
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

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath + "/FAKE/PATH/scan.cli.jar", null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        PostBuildHubiScan pbScan = new PostBuildHubiScan(null, "default", null, null);
        pbScan.getIScanCLI(iScanInstallations, listener, mockBuild);
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

        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class);
        when(mockpbScan.getDescriptor()).thenReturn(descriptor);
        when(mockpbScan.validateConfiguration(Mockito.any(IScanInstallation[].class), Mockito.any(IScanJobs[].class))).thenCallRealMethod();

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        IScanJobs oneScan = new IScanJobs(basePath);
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        Assert.assertTrue(mockpbScan.validateConfiguration(iScanInstallations, scans));
    }

    @Test
    public void testValidateConfigurationNoiScanInstallations() throws Exception {
        // validateConfiguration with no IScanInstallations and correct IScanJobs

        exception.expect(IScanToolMissingException.class);
        exception.expectMessage("Could not find an iScan Installation to use.");

        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class);
        when(mockpbScan.validateConfiguration(Mockito.any(IScanInstallation[].class), Mockito.any(IScanJobs[].class))).thenCallRealMethod();

        IScanInstallation[] iScanInstallations = new IScanInstallation[0];

        IScanJobs oneScan = new IScanJobs(basePath);
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        Assert.assertTrue(mockpbScan.validateConfiguration(iScanInstallations, scans));
    }

    @Test
    public void testValidateConfigurationNulliScanInstallations() throws Exception {
        // validateConfiguration with null IScanInstallations and correct IScanJobs

        exception.expect(IScanToolMissingException.class);
        exception.expectMessage("Could not find an iScan Installation to use.");

        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class);
        when(mockpbScan.validateConfiguration(Mockito.any(IScanInstallation[].class), Mockito.any(IScanJobs[].class))).thenCallRealMethod();

        IScanJobs oneScan = new IScanJobs(basePath);
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        Assert.assertTrue(mockpbScan.validateConfiguration(null, scans));
    }

    @Test
    public void testValidateConfigurationNoIScanJobs() throws Exception {
        // validateConfiguration with correct IScanInstallations and no IScanJobs

        exception.expect(HubConfigurationException.class);
        exception.expectMessage("Could not find any targets to scan.");

        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class);
        when(mockpbScan.validateConfiguration(Mockito.any(IScanInstallation[].class), Mockito.any(IScanJobs[].class))).thenCallRealMethod();

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        IScanJobs[] scans = new IScanJobs[0];

        Assert.assertTrue(mockpbScan.validateConfiguration(iScanInstallations, scans));
    }

    @Test
    public void testValidateConfigurationNullIScanJobs() throws Exception {
        // validateConfiguration with correct IScanInstallations and null IScanJobs

        exception.expect(HubConfigurationException.class);
        exception.expectMessage("Could not find any targets to scan.");

        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class);
        when(mockpbScan.validateConfiguration(Mockito.any(IScanInstallation[].class), Mockito.any(IScanJobs[].class))).thenCallRealMethod();

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
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

        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class);
        when(mockpbScan.getDescriptor()).thenReturn(descriptor);
        when(mockpbScan.validateConfiguration(Mockito.any(IScanInstallation[].class), Mockito.any(IScanJobs[].class))).thenCallRealMethod();

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        IScanJobs oneScan = new IScanJobs(basePath);
        IScanJobs[] scans = new IScanJobs[1];
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

        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class);
        when(mockpbScan.getDescriptor()).thenReturn(descriptor);
        when(mockpbScan.validateConfiguration(Mockito.any(IScanInstallation[].class), Mockito.any(IScanJobs[].class))).thenCallRealMethod();

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        IScanJobs oneScan = new IScanJobs(basePath);
        IScanJobs[] scans = new IScanJobs[1];
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

        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class);
        when(mockpbScan.getDescriptor()).thenReturn(descriptor);
        when(mockpbScan.validateConfiguration(Mockito.any(IScanInstallation[].class), Mockito.any(IScanJobs[].class))).thenCallRealMethod();

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        IScanJobs oneScan = new IScanJobs(basePath);
        IScanJobs[] scans = new IScanJobs[1];
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

        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class);
        when(mockpbScan.getDescriptor()).thenReturn(descriptor);
        when(mockpbScan.validateConfiguration(Mockito.any(IScanInstallation[].class), Mockito.any(IScanJobs[].class))).thenCallRealMethod();

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        IScanJobs oneScan = new IScanJobs(basePath);
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        mockpbScan.validateConfiguration(iScanInstallations, scans);
    }
}
