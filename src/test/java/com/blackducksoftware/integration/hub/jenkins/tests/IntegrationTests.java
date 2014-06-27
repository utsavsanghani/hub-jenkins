package com.blackducksoftware.integration.hub.jenkins.tests;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.NodeProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.IScanJobs;
import com.blackducksoftware.integration.hub.jenkins.IScanInstallation;
import com.blackducksoftware.integration.hub.jenkins.IScanInstallation.IScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubiScan;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class IntegrationTests {

    private static final String PASSWORD_WRONG = "Assert.failurePassword";

    private static final String USERNAME_NON_EXISTING = "Assert.failureUser";

    private static String basePath;

    private static String iScanInstallPath;

    private static String testWorkspace;

    private static Properties testProperties;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void init() throws Exception {
        basePath = IntegrationTests.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        basePath = basePath.substring(0, basePath.indexOf("/target"));
        basePath = basePath + "/test-workspace";
        iScanInstallPath = basePath + "/scan.cli-1.14.0-SNAPSHOT";
        testWorkspace = basePath + "/workspace";

        testProperties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream("test.properties");
        try {
            testProperties.load(is);
        } catch (IOException e) {
            System.err.println("reading test.properties failed!");
        }
        // p.load(new FileReader(new File("test.properties")));
        System.out.println(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        System.out.println(testProperties.getProperty("TEST_USERNAME"));
        System.out.println(testProperties.getProperty("TEST_PASSWORD"));
    }

    @Test
    public void completeRunthroughAndScan() throws IOException, InterruptedException, ExecutionException {

        FreeStyleProject project = j.jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);
        IScanInstallation[] iScanInstallations = new IScanInstallation[1];
        iScanInstallations[0] = iScanInstall;

        IScanDescriptor iScanDesc = new IScanDescriptor();
        iScanDesc.setInstallations(iScanInstallations);

        j.jenkins.getDescriptorList(NodeProperty.class).add(iScanDesc);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs(testWorkspace);
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default");
        pbScan.getDescriptor().setHubServerInfo(serverInfo);

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");

        Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : master"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan script at : "));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
        Assert.assertTrue(buildOutput.contains("Finished in"));
        Assert.assertTrue(buildOutput.contains("with status SUCCESS"));
        Assert.assertTrue(buildOutput.contains("Finished running Black Duck iScans."));
    }
}
