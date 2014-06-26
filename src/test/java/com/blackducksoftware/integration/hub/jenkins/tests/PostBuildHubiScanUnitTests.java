package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.FilePath;
import hudson.model.StreamBuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.slaves.DumbSlave;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import com.blackducksoftware.integration.hub.jenkins.IScanInstallation;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubiScan;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.google.common.base.Charsets;

public class PostBuildHubiScanUnitTests {

    private static String basePath;

    private static String iScanInstallPath;

    private static StreamBuildListener listener;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void init() {
        basePath = IntegrationTests.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        basePath = basePath.substring(0, basePath.indexOf("/target"));
        basePath = basePath + "/test-workspace";
        iScanInstallPath = basePath + "/scan.cli-1.14.0-SNAPSHOT";
        listener = new StreamBuildListener(System.out, Charsets.UTF_8);
    }

    @Test
    public void testValidateScanTargetsTwoExistingInWorkspace() throws IOException, HubConfigurationException, InterruptedException {
        File createdFolder = folder.newFolder("newfolder");
        File createdFile = folder.newFile("myfilefile.txt");
        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class, Mockito.CALLS_REAL_METHODS);
        File workspace = new File(createdFolder.getCanonicalPath() + "/..");
        when(mockpbScan.getWorkingDirectory()).thenReturn(workspace.getCanonicalPath());
        String[] scanTargets = { createdFolder.getCanonicalPath(), createdFile.getCanonicalPath() };
        Assert.assertTrue(mockpbScan.validateScanTargets(listener, null, Arrays.asList(scanTargets)));
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
        File createdFile = folder.newFile("myfilefile.txt");
        PostBuildHubiScan mockpbScan = mock(PostBuildHubiScan.class, Mockito.CALLS_REAL_METHODS);
        when(mockpbScan.getWorkingDirectory()).thenReturn("/tmp");
        String[] scanTargets = { createdFolder.getCanonicalPath(), createdFile.getCanonicalPath() };
        mockpbScan.validateScanTargets(listener, null, Arrays.asList(scanTargets));
    }

    // getIScanScript
    @Test
    public void testGetIScanScriptConfiguredCorrectly() throws Exception {
        // getIscanScript with empty nodeName (indicates master), with valid iScan installation configured and selected,
        // and with the script existing

        AbstractBuild mockBuild = mock(AbstractBuild.class, Mockito.RETURNS_DEEP_STUBS);
        when(mockBuild.getBuiltOn().getNodeName()).thenReturn("");
        DumbSlave slave = j.createSlave("", null);
        Node node = slave;
        when(mockBuild.getBuiltOn()).thenReturn(node);

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        PostBuildHubiScan pbScan = new PostBuildHubiScan(null, "default");

        FilePath script = pbScan.getIScanScript(iScanInstallations, listener, mockBuild);
        Assert.assertTrue(script.exists());
        Assert.assertTrue(script.getRemote().equals(iScanInstallPath + "/bin/scan.cli.sh"));
    }

    // @Test
    // public void testGetIScanScriptOnSlave() throws Exception {
    // // getIscanScript with empty nodeName (indicates master), with valid iScan installation configured and selected,
    // // and with the script existing
    //
    // AbstractBuild mockBuild = mock(AbstractBuild.class, Mockito.RETURNS_DEEP_STUBS);
    // when(mockBuild.getBuiltOn().getNodeName()).thenReturn("");
    // DumbSlave slave = j.createSlave("", null);
    // Node node = slave;
    // when(mockBuild.getBuiltOn()).thenReturn(node);
    //
    // IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
    // IScanInstallation[] iScanInstallations = new IScanInstallation[1];
    // iScanInstallations[0] = iScanInstall;
    //
    // PostBuildHubiScan pbScan = new PostBuildHubiScan(null, "default");
    //
    // FilePath script = pbScan.getIScanScript(iScanInstallations, listener, mockBuild);
    // Assert.assertTrue(script.exists());
    // Assert.assertTrue(script.getRemote().equals(iScanInstallPath + "/bin/scan.cli.sh"));
    // }
    //
    // @Test
    // public void testGetIScanScriptOnSlave() throws Exception {
    // // getIscanScript with empty nodeName (indicates master), with valid iScan installation configured and selected,
    // // and with the script existing
    //
    // AbstractBuild mockBuild = mock(AbstractBuild.class, Mockito.RETURNS_DEEP_STUBS);
    // when(mockBuild.getBuiltOn().getNodeName()).thenReturn("");
    // DumbSlave slave = j.createSlave("", null);
    // Node node = slave;
    // when(mockBuild.getBuiltOn()).thenReturn(node);
    //
    // IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
    // IScanInstallation[] iScanInstallations = new IScanInstallation[1];
    // iScanInstallations[0] = iScanInstall;
    //
    // PostBuildHubiScan pbScan = new PostBuildHubiScan(null, "default");
    //
    // FilePath script = pbScan.getIScanScript(iScanInstallations, listener, mockBuild);
    // Assert.assertTrue(script.exists());
    // Assert.assertTrue(script.getRemote().equals(iScanInstallPath + "/bin/scan.cli.sh"));
    // }
}
