package com.blackducksoftware.integration.hub.jenkins.tests;

import hudson.EnvVars;
import hudson.ProxyConfiguration;
import hudson.model.FreeStyleBuild;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.JDK;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tools.ToolDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import jenkins.model.Jenkins;

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
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.IScanInstallation;
import com.blackducksoftware.integration.hub.jenkins.IScanInstallation.IScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubiScan;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDRestException;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class IntegrationTest {

    private static final String PASSWORD_WRONG = "Assert.failurePassword";

    private static final String USERNAME_NON_EXISTING = "Assert.failureUser";

    private static final String PROJECT_NAME_EXISTING = "Jenkins Hub Integration Test Project";

    private static final String PROJECT_NAME_NOT_EXISTING = "Assert Project Does Not Exist";

    private static final String PROJECT_RELEASE_EXISTING = "First Release";

    private static final String PROJECT_RELEASE_NOT_EXISTING = "Assert Release Does Not Exist";

    private static String basePath;

    private static String iScanInstallPath;

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
        basePath = IntegrationTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
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
        restHelper = new JenkinsHubIntTestHelper();
        restHelper.setBaseUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        restHelper.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        projectCleanup();
    }

    /**
     * Cleans up any project that may be left over from interrupted tests.
     * 
     * @throws BDRestException
     * @throws IOException
     */
    public static void projectCleanup() throws BDRestException, IOException {
        ArrayList<LinkedHashMap<String, Object>> responseList = restHelper.getProjectMatches(PROJECT_NAME_EXISTING);
        ArrayList<String> ids = restHelper.getProjectIdsFromProjectMatches(responseList, PROJECT_NAME_EXISTING);
        if (ids.size() > 0) {
            for (String id : ids) {
                restHelper.deleteHubProject(id);
            }
        }
    }

    @Test
    public void completeRunthroughAndScan() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", null, null, 256);

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);
        Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Running on : master"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan directory:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] directories in the iScan directory"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib directory:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib file:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan CLI at : "));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this Hub Url : '" + testProperties.getProperty("TEST_HUB_SERVER_URL").substring(7, 36) + "'"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
        Assert.assertTrue(buildOutput.contains("Finished in"));
        Assert.assertTrue(buildOutput.contains("with status SUCCESS"));
        Assert.assertTrue(buildOutput.contains("Finished running Black Duck iScans."));
    }

    @Test
    public void completeRunthroughAndScanWithMapping() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs twoScan = new IScanJobs("ch-simple-web/simple-webapp/target");
        IScanJobs threeScan = new IScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        IScanJobs[] scans = new IScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createTestHubProject(PROJECT_NAME_EXISTING);
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            boolean created = restHelper.createTestHubProjectRelease(PROJECT_RELEASE_EXISTING, projectId);
            Assert.assertTrue(created);
            // Give server time to recognize the Release
            Thread.sleep(2000);

            PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING, 256);

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Running on : master"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] directories in the iScan directory"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib file:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan CLI at : "));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this Hub Url : '" + testProperties.getProperty("TEST_HUB_SERVER_URL").substring(7, 36)
                    + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
            Assert.assertTrue(buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput.contains("', you can view the iScan CLI logs at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Release Id:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] The scan target :"));
            Assert.assertTrue(buildOutput.contains("' has Scan Location Id: '"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] These scan Id's were found for the scan targets."));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Linking the scan Id's to the Hub Project: '" + PROJECT_NAME_EXISTING + "', and Release: '"
                    + PROJECT_RELEASE_EXISTING));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Mapping the scan with id: '"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Successfully mapped the scan with id: '"));
            Assert.assertTrue(buildOutput.contains("Finished running Black Duck iScans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingThroughProxy() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs twoScan = new IScanJobs("ch-simple-web/simple-webapp/target");
        IScanJobs threeScan = new IScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        IScanJobs[] scans = new IScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createTestHubProject(PROJECT_NAME_EXISTING);
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            boolean created = restHelper.createTestHubProjectRelease(PROJECT_RELEASE_EXISTING, projectId);
            Assert.assertTrue(created);
            // Give server time to recognize the Release
            Thread.sleep(2000);

            PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING, 256);
            pbScan.setTEST(true);
            jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST"),
                    Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT")));

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Running on : master"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] directories in the iScan directory"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib file:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan CLI at : "));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this Hub Url : '" + testProperties.getProperty("TEST_HUB_SERVER_URL").substring(7, 36)
                    + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
            Assert.assertTrue(buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput.contains("', you can view the iScan CLI logs at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Using proxy: '" + testProperties.getProperty("TEST_PROXY_HOST") + "' at Port: '"
                    + testProperties.getProperty("TEST_PROXY_PORT") + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Release Id:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] The scan target :"));
            Assert.assertTrue(buildOutput.contains("' has Scan Location Id: '"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] These scan Id's were found for the scan targets."));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Linking the scan Id's to the Hub Project: '" + PROJECT_NAME_EXISTING + "', and Release: '"
                    + PROJECT_RELEASE_EXISTING));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Mapping the scan with id: '"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Successfully mapped the scan with id: '"));
            Assert.assertTrue(buildOutput.contains("Finished running Black Duck iScans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingWithProxyIgnored() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs twoScan = new IScanJobs("ch-simple-web/simple-webapp/target");
        IScanJobs threeScan = new IScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        IScanJobs[] scans = new IScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createTestHubProject(PROJECT_NAME_EXISTING);
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            boolean created = restHelper.createTestHubProjectRelease(PROJECT_RELEASE_EXISTING, projectId);
            Assert.assertTrue(created);
            // Give server time to recognize the Release
            Thread.sleep(2000);

            PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING, 256);
            pbScan.setTEST(true);
            URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));
            jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST"),
                    Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT")), null, null, url.getHost());

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Running on : master"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] directories in the iScan directory"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib file:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan CLI at : "));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this Hub Url : '" + testProperties.getProperty("TEST_HUB_SERVER_URL").substring(7, 36)
                    + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
            Assert.assertTrue(buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput.contains("', you can view the iScan CLI logs at :"));

            Assert.assertTrue(buildOutput.contains("[DEBUG] Ignoring proxy: '" + testProperties.getProperty("TEST_PROXY_HOST") + "' at Port: '"
                    + testProperties.getProperty("TEST_PROXY_PORT") + "' for the Host: '" + url.getHost() + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Release Id:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] The scan target :"));
            Assert.assertTrue(buildOutput.contains("' has Scan Location Id: '"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] These scan Id's were found for the scan targets."));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Linking the scan Id's to the Hub Project: '" + PROJECT_NAME_EXISTING + "', and Release: '"
                    + PROJECT_RELEASE_EXISTING));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Mapping the scan with id: '"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Successfully mapped the scan with id: '"));
            Assert.assertTrue(buildOutput.contains("Finished running Black Duck iScans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingToNonExistentProject() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", PROJECT_NAME_NOT_EXISTING, PROJECT_RELEASE_NOT_EXISTING, 256);
        pbScan.setTEST(true);
        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);
        Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Running on : master"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan directory:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] directories in the iScan directory"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib directory:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib file:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan CLI at : "));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this Hub Url : '" + testProperties.getProperty("TEST_HUB_SERVER_URL").substring(7, 36)
                + "'"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
        Assert.assertTrue(buildOutput.contains("Finished in"));
        Assert.assertTrue(buildOutput.contains("with status SUCCESS"));
        Assert.assertTrue(buildOutput.contains("', you can view the iScan CLI logs at :"));
        Assert.assertTrue(buildOutput
                .contains("com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException: The specified Project could not be found."));
        Assert.assertTrue(buildOutput.contains("ERROR: The specified Project could not be found."));
        Assert.assertTrue(buildOutput.contains("Build step 'Black Duck Hub Integration' changed build result to UNSTABLE"));
        Assert.assertTrue(buildOutput.contains("Finished: UNSTABLE"));

    }

    @Test
    public void completeRunthroughAndScanWithMappingToNonExistentRelease() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createTestHubProject(PROJECT_NAME_EXISTING);
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);

            PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", PROJECT_NAME_EXISTING, PROJECT_RELEASE_NOT_EXISTING, 256);
            pbScan.setTEST(true);
            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Running on : master"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] directories in the iScan directory"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib file:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan CLI at : "));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this Hub Url : '" + testProperties.getProperty("TEST_HUB_SERVER_URL").substring(7, 36)
                    + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
            Assert.assertTrue(buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput.contains("', you can view the iScan CLI logs at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Release Id: 'null'"));
            Assert.assertTrue(buildOutput
                    .contains("com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException: The specified Release could not be found in the Project."));
            Assert.assertTrue(buildOutput.contains("ERROR: The specified Release could not be found in the Project."));
            Assert.assertTrue(buildOutput.contains("Build step 'Black Duck Hub Integration' changed build result to UNSTABLE"));
            Assert.assertTrue(buildOutput.contains("Finished: UNSTABLE"));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingScansAlreadyMapped() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs twoScan = new IScanJobs("ch-simple-web/simple-webapp/target");
        IScanJobs threeScan = new IScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        IScanJobs[] scans = new IScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createTestHubProject(PROJECT_NAME_EXISTING);
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            boolean created = restHelper.createTestHubProjectRelease(PROJECT_RELEASE_EXISTING, projectId);
            Assert.assertTrue(created);
            // Give server time to recognize the Release
            Thread.sleep(2000);

            PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING, 256);
            pbScan.setTEST(true);
            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            // First Run scans and maps the scans to the Project Release
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Running on : master"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] directories in the iScan directory"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib file:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan CLI at : "));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this Hub Url : '" + testProperties.getProperty("TEST_HUB_SERVER_URL").substring(7, 36)
                    + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
            Assert.assertTrue(buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput.contains("', you can view the iScan CLI logs at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Release Id:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] The scan target :"));
            Assert.assertTrue(buildOutput.contains("' has Scan Location Id: '"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] These scan Id's were found for the scan targets."));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Linking the scan Id's to the Hub Project: '" + PROJECT_NAME_EXISTING + "', and Release: '"
                    + PROJECT_RELEASE_EXISTING));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Mapping the scan with id: '"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Successfully mapped the scan with id: '"));
            Assert.assertTrue(buildOutput.contains("Finished running Black Duck iScans."));

            // Second run, scans should already be mapped
            build = project.scheduleBuild2(0).get();
            buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Running on : master"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] directories in the iScan directory"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib directory:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib file:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan CLI at : "));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this Hub Url : '" + testProperties.getProperty("TEST_HUB_SERVER_URL").substring(7, 36)
                    + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
            Assert.assertTrue(buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput.contains("', you can view the iScan CLI logs at :"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] Release Id:"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] The scan target :"));
            Assert.assertTrue(buildOutput.contains("' has Scan Location Id: '"));
            Assert.assertTrue(buildOutput.contains("[DEBUG] These scans are already mapped to Project : '" + PROJECT_NAME_EXISTING + "', Release : '"
                    + PROJECT_RELEASE_EXISTING + "'. OR there was an issue getting the Id's for the defined scan targets."));
            Assert.assertTrue(buildOutput.contains("Finished running Black Duck iScans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void scanInvalidPassword() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), PASSWORD_WRONG);
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", null, null, 256);

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);
        Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Running on : master"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan directory:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] directories in the iScan directory"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib directory:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib file:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan CLI at : "));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this Hub Url : '" + testProperties.getProperty("TEST_HUB_SERVER_URL").substring(7, 36) + "'"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
        Assert.assertTrue(buildOutput.contains("ERROR:")); // TODO replace with new iScan error message
        Assert.assertTrue(buildOutput.contains("Finished in"));
        Assert.assertTrue(buildOutput.contains("with status FAILURE"));
        Assert.assertTrue(buildOutput.contains("Finished running Black Duck iScans."));
    }

    @Test
    public void scanInvalidUser() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                USERNAME_NON_EXISTING, testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", null, null, 256);

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);
        Assert.assertTrue(buildOutput.contains("Starting Black Duck iScans..."));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Running on : master"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan directory:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] directories in the iScan directory"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib directory:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] iScan lib file:"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this iScan CLI at : "));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Scan target exists at :"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this Hub Url : '" + testProperties.getProperty("TEST_HUB_SERVER_URL").substring(7, 36) + "'"));
        Assert.assertTrue(buildOutput.contains("[DEBUG] : Using this java installation : "));
        Assert.assertTrue(buildOutput.contains("ERROR:")); // TODO replace with new iScan error message
        Assert.assertTrue(buildOutput.contains("Finished in"));
        Assert.assertTrue(buildOutput.contains("with status FAILURE"));
        Assert.assertTrue(buildOutput.contains("Finished running Black Duck iScans."));
    }

    @Test
    public void setJDKUserSelectedNonExistent() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", null, null, 256);
        pbScan.setTEST(true);

        JDK nonexistentJDK = new JDK("FAKE", "/assert/this/is/fake/path");

        // build.getProject().getJDK(); Will return null if the jdk doesn't exist.

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);
        project.setJDK(nonexistentJDK);
        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");

        System.out.println(buildOutput);
        Assert.assertTrue(buildOutput
                .contains("Could not find the specified Java installation, checking the JAVA_HOME variable."));
    }

    @Test
    public void setJDKEnvSelectedNonExistent() throws IOException, InterruptedException, ExecutionException {

        Jenkins jenkins = j.jenkins;

        IScanInstallation iScanInstall = new IScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        IScanJobs oneScan = new IScanJobs("");
        IScanJobs[] scans = new IScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubiScan pbScan = new PostBuildHubiScan(scans, "default", null, null, 256);
        pbScan.setTEST(true);

        JDK nonexistentJDK = new JDK("FAKE", "/assert/this/is/fake/path");
        // build.getProject().getJDK(); Will return null if the jdk doesn't exist.

        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put("JAVA_HOME", "/assert/this/is/fake/path");
        j.jenkins.getGlobalNodeProperties().add(prop);

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setJDK(nonexistentJDK); // Will set the JDK to null if the JDK doesn't exist.
        project.setCustomWorkspace(testWorkspace);
        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);
        Assert.assertTrue(buildOutput
                .contains("com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException: Could not find the specified Java installation at"));
    }
}
