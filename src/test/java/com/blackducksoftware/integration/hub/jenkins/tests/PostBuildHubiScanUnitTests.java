package com.blackducksoftware.integration.hub.jenkins.tests;

import hudson.model.StreamBuildListener;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.blackducksoftware.integration.hub.jenkins.PostBuildHubiScan;
import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.google.common.base.Charsets;

public class PostBuildHubiScanUnitTests {

    private static String basePath;

    private static StreamBuildListener listener;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void init() {
        basePath = IntegrationTests.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        basePath = basePath.substring(0, basePath.indexOf("/target"));
        basePath = basePath + "/test-workspace";
        listener = new StreamBuildListener(System.out, Charsets.UTF_8);
    }

    @Test
    public void testValidateScanTargetsTwoExistingInWorkspace() throws IOException, HubConfigurationException, InterruptedException {
        File createdFolder = folder.newFolder("newfolder");
        File createdFile = folder.newFile("myfilefile.txt");
        PostBuildHubiScan pbScan = new PostBuildHubiScan(null, null);
        File workspace = new File(createdFolder.getCanonicalPath() + "/..");
        pbScan.setWorkingDirectory(workspace.getCanonicalPath());
        String[] scanTargets = { createdFolder.getCanonicalPath(), createdFile.getCanonicalPath() };
        pbScan.validateScanTargets(listener, null, Arrays.asList(scanTargets));
    }

    @Test
    public void testValidateScanTargetsTwoNotExisting() throws IOException, HubConfigurationException, InterruptedException {
        exception.expect(IOException.class);
        exception.expectMessage("Scan target could not be found :");
        PostBuildHubiScan pbScan = new PostBuildHubiScan(null, null);
        pbScan.setWorkingDirectory("/");
        String[] scanTargets = { "/ASSERT/NOT/EXISTING", "/RE-ASSERT/Not/EXISTING" };
        pbScan.validateScanTargets(listener, null, Arrays.asList(scanTargets));
    }

    @Test
    public void testValidateScanTargetsExistingOutsideWorkspace() throws IOException, HubConfigurationException, InterruptedException {
        exception.expect(HubConfigurationException.class);
        exception.expectMessage("Can not scan targets outside of the workspace.");
        File createdFolder = folder.newFolder("newfolder");
        File createdFile = folder.newFile("myfilefile.txt");
        PostBuildHubiScan pbScan = new PostBuildHubiScan(null, null);
        pbScan.setWorkingDirectory("/tmp");
        String[] scanTargets = { createdFolder.getCanonicalPath(), createdFile.getCanonicalPath() };
        pbScan.validateScanTargets(listener, null, Arrays.asList(scanTargets));
    }

    // getIScanScript
}
