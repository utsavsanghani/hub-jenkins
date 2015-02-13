package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertNotNull;
import hudson.EnvVars;
import hudson.ProxyConfiguration;
import hudson.model.FreeStyleBuild;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.JDK;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tools.ToolDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import jenkins.model.Jenkins;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.ScanJobs;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubScan;
import com.blackducksoftware.integration.hub.jenkins.ScanInstallation;
import com.blackducksoftware.integration.hub.jenkins.ScanInstallation.IScanDescriptor;
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
        basePath = basePath.substring(0, basePath.indexOf(File.separator + "target"));
        basePath = basePath + File.separator + "test-workspace";
        iScanInstallPath = basePath + File.separator + "scan.cli-1.18.0-SNAPSHOT";
        testWorkspace = basePath + File.separator + "workspace";

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

    @AfterClass
    public static void tearDown() throws BDRestException, IOException {
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

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", null, null, "4096");

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);
        List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
        Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] : Running on : master"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] BlackDuck Scan directory:"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] directories in the BlackDuck Scan directory"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] BlackDuck Scan lib directory:"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] BlackDuck Scan lib file:"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] : Using this BlackDuck Scan CLI at : "));
        Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] : Scan target exists at :"));
        URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] : Using this Hub Url : '" + url.getHost()));
        Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] : Using this java installation : "));
        Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "with status SUCCESS"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));
    }

    @Test
    public void completeRunthroughAndScanWithMapping() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs[] scans = new ScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createHubProject(PROJECT_NAME_EXISTING);
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            String versionId = restHelper.createHubVersion(PROJECT_RELEASE_EXISTING, projectId);
            assertNotNull(versionId);
            // Give server time to recognize the Version
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING, "4096");

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "with status SUCCESS"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "', you can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Version Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Checking for the scan location with Host name:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] The scan target :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "' has Scan Location Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] These scan Id's were found for the scan targets."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Mapping the scan location with id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Successfully mapped the scan with id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingVariableProjectName() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs[] scans = new ScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;
        String projectName = "Jenkins Hub Integration Variable Project Name";
        try {
            PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
            scanDesc.setHubServerInfo(serverInfo);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", "${JOB_NAME}", PROJECT_RELEASE_EXISTING, "4096");

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, projectName);
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "with status SUCCESS"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "', you can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Project Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Version Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Checking for the scan location with Host name:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] The scan target :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "' has Scan Location Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] These scan Id's were found for the scan targets."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Mapping the scan location with id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Successfully mapped the scan with id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectId(projectName));
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingThroughProxy() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs[] scans = new ScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createHubProject(PROJECT_NAME_EXISTING);
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            String versionId = restHelper.createHubVersion(PROJECT_RELEASE_EXISTING, projectId);
            assertNotNull(versionId);
            // Give server time to recognize the Version
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING, "4096");
            pbScan.setTEST(true);
            jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST"),
                    Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT")));

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "with status SUCCESS"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "', you can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Using proxy: '" + testProperties.getProperty("TEST_PROXY_HOST") + "' at Port: '"
                    + testProperties.getProperty("TEST_PROXY_PORT") + "'"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Version Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Checking for the scan location with Host name:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] The scan target :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "' has Scan Location Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] These scan Id's were found for the scan targets."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Mapping the scan location with id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Successfully mapped the scan with id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));

        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingWithProxyIgnored() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs[] scans = new ScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createHubProject(PROJECT_NAME_EXISTING);
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            String versionId = restHelper.createHubVersion(PROJECT_RELEASE_EXISTING, projectId);
            assertNotNull(versionId);
            // Give server time to recognize the Version
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING, "4096");
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
            List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));

            Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "with status SUCCESS"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "', you can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Using proxy: '" + testProperties.getProperty("TEST_PROXY_HOST") + "' at Port: '"
                    + testProperties.getProperty("TEST_PROXY_PORT") + "'"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Ignoring proxy for the Host: '" + url.getHost() + "'"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Version Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Checking for the scan location with Host name:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] The scan target :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "' has Scan Location Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] These scan Id's were found for the scan targets."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Mapping the scan location with id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Successfully mapped the scan with id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));

        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingToNonExistentProject() throws IOException, InterruptedException, ExecutionException, BDRestException {
        try {
            Jenkins jenkins = j.jenkins;

            ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

            IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
            iScanDesc.setInstallations(iScanInstall);

            CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
            UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                    testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
            store.addCredentials(Domain.global(), credential);

            HubServerInfo serverInfo = new HubServerInfo();
            serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
            serverInfo.setCredentialsId(credential.getId());

            ScanJobs oneScan = new ScanJobs("");
            ScanJobs[] scans = new ScanJobs[1];
            scans[0] = oneScan;

            PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
            scanDesc.setHubServerInfo(serverInfo);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", PROJECT_NAME_NOT_EXISTING, PROJECT_RELEASE_NOT_EXISTING, "4096");
            pbScan.setTEST(true);
            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] : Scan target exists at :"));
            URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "with status SUCCESS"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "', you can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Project Id: '"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Version Id: '"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Successfully mapped the scan"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished: SUCCESS"));
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectId(PROJECT_NAME_NOT_EXISTING));
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingToNonExistentVersion() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createHubProject(PROJECT_NAME_EXISTING);
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", PROJECT_NAME_EXISTING, PROJECT_RELEASE_NOT_EXISTING, "4096");
            pbScan.setTEST(true);
            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] : Scan target exists at :"));
            URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "with status SUCCESS"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "', you can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Version Id: '"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Successfully mapped the scan"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished: SUCCESS"));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingScansAlreadyMapped() throws IOException, InterruptedException, ExecutionException, BDRestException {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs[] scans = new ScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createHubProject(PROJECT_NAME_EXISTING);
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            String versionId = restHelper.createHubVersion(PROJECT_RELEASE_EXISTING, projectId);
            assertNotNull(versionId);
            // Give server time to recognize the Version
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", PROJECT_NAME_EXISTING, PROJECT_RELEASE_EXISTING, "4096");
            pbScan.setTEST(true);
            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            // First Run scans and maps the scans to the Project Version
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "with status SUCCESS"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "', you can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Version Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Checking for the scan location with Host name:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] The scan target :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "' has Scan Location Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] These scan Id's were found for the scan targets."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Mapping the scan location with id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Successfully mapped the scan with id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));

            // Second run, scans should already be mapped
            build = project.scheduleBuild2(0).get();
            buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);
            buildOutputList = Arrays.asList(buildOutput.split("\n"));

            Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "with status SUCCESS"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "', you can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Version Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Checking for the scan location with Host name:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] The scan target :"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "' has Scan Location Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Project Id: '" + projectId + "'"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Version Id:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] Checking for the scan location with Host name:"));
            Assert.assertTrue(listContainsSubString(buildOutputList, "[DEBUG] These scan Id's were found for the scan targets."));
            Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void scanInvalidPassword() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), PASSWORD_WRONG);
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", null, null, "4096");

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);
        List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));

        Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
        // TODO replace with new BlackDuck Scan error message
        Assert.assertTrue(listContainsSubString(buildOutputList, "ERROR:"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "with status FAILURE"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));
    }

    @Test
    public void scanInvalidUser() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                USERNAME_NON_EXISTING, testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", null, null, "4096");

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);
        List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));

        Assert.assertTrue(listContainsSubString(buildOutputList, "Starting BlackDuck Scans..."));
        // TODO replace with new BlackDuck Scan error message
        Assert.assertTrue(listContainsSubString(buildOutputList, "ERROR:"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "Finished in"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "with status FAILURE"));
        Assert.assertTrue(listContainsSubString(buildOutputList, "Finished running Black Duck Scans."));
    }

    @Test
    public void setJDKUserSelectedNonExistent() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", null, null, "4096");
        pbScan.setTEST(true);

        JDK nonexistentJDK = new JDK("FAKE", "/assert/this/is/fake/path");

        // build.getProject().getJDK(); // Will return null if the jdk doesn't exist.

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);
        project.setJDK(nonexistentJDK);
        project.getPublishersList().add(pbScan);

        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put("JAVA_HOME", "");
        j.jenkins.getGlobalNodeProperties().add(prop);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");

        System.out.println(buildOutput);
        // FIXME Need to check the Jdk exists before using it
        // List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));
        // Assert.assertTrue(buildOutputList.get(3).contains("Could not find the specified Java installation, checking the JAVA_HOME variable."));
        // Assert.assertTrue(buildOutputList
        // .get(4)
        // .contains(
        // "Need to define a JAVA_HOME or select an installed JDK."));
    }

    @Test
    public void setJDKEnvSelectedNonExistent() throws IOException, InterruptedException, ExecutionException {

        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation("default", iScanInstallPath, null);

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        iScanDesc.setInstallations(iScanInstall);

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
        scanDesc.setHubServerInfo(serverInfo);

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, "default", null, null, "4096");
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
        // FIXME Need to check the Jdk exists before using it
        // List<String> buildOutputList = Arrays.asList(buildOutput.split("\n"));
        // Assert.assertTrue(buildOutputList
        // .get(3)
        // .contains(
        // "Could not find the specified Java installation, checking the JAVA_HOME variable."));
        // Assert.assertTrue(buildOutputList
        // .get(4)
        // .contains(
        // "Could not find the specified Java installation at:"));
    }

    private boolean listContainsSubString(List<String> list, String subString) {
        for (String listString : list) {
            if (listString != null && !listString.isEmpty()) {
                if (listString.contains(subString)) {
                    return true;
                }
            }
        }
        return false;
    }
}
