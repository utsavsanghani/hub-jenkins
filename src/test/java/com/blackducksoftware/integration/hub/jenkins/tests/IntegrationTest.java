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
import java.net.URISyntaxException;
import java.net.URL;
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

import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubScan;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.ScanInstallation;
import com.blackducksoftware.integration.hub.jenkins.ScanInstallation.IScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.ScanJobs;
import com.blackducksoftware.integration.hub.response.DistributionEnum;
import com.blackducksoftware.integration.hub.response.PhaseEnum;
import com.blackducksoftware.integration.hub.response.ProjectItem;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class IntegrationTest {

    private static final String DEFAULT_ISCAN = "default";

    private static final String PASSWORD_WRONG = "Assert.failurePassword";

    private static final String USERNAME_NON_EXISTING = "Assert.failureUser";

    private static final String PROJECT_NAME_NOT_EXISTING = "Assert Project Does Not Exist";

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
        iScanInstallPath = basePath + File.separator + "scan.cli-2.1.2";
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
        restHelper = new JenkinsHubIntTestHelper(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        restHelper.setCookies(testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        projectCleanup();
    }

    @AfterClass
    public static void tearDown() throws BDRestException, IOException, URISyntaxException {
        projectCleanup();
    }

    /**
     * Cleans up any project that may be left over from interrupted tests.
     *
     * @throws BDRestException
     * @throws IOException
     * @throws URISyntaxException
     */
    public static void projectCleanup() throws BDRestException, IOException, URISyntaxException {
        try {
            ProjectItem project = restHelper.getProjectByName(testProperties.getProperty("TEST_PROJECT"));
            if (project != null && project.getId() != null) {
                restHelper.deleteHubProject(project.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void completeRunthroughAndScan() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, null, null, null, null, "4096");
        pbScan.setverbose(false);
        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);

        Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
        Assert.assertTrue(buildOutput, buildOutput.contains("Running on : master"));
        Assert.assertTrue(buildOutput, buildOutput.contains("BlackDuck Scan directory:"));
        Assert.assertTrue(buildOutput, buildOutput.contains("directories in the BlackDuck Scan directory"));
        Assert.assertTrue(buildOutput, buildOutput.contains("BlackDuck Scan lib directory:"));
        Assert.assertTrue(buildOutput, buildOutput.contains("BlackDuck Scan lib file:"));
        Assert.assertTrue(buildOutput, buildOutput.contains("Using this BlackDuck Scan CLI at : "));
        Assert.assertTrue(buildOutput, buildOutput.contains("Scan target exists at :"));
        URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        Assert.assertTrue(buildOutput, buildOutput.contains("Using this Hub hostname : '" + url.getHost()));
        Assert.assertTrue(buildOutput, buildOutput.contains("Using this java installation : "));
        Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
        Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
        Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
    }

    @Test
    public void completeRunthroughAndScanWithMapping() throws Exception {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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
            projectId = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            String versionId = restHelper.createHubVersion(testProperties.getProperty("TEST_VERSION"), projectId, PhaseEnum.DEVELOPMENT.name(),
                    DistributionEnum.EXTERNAL.name());
            assertNotNull(versionId);
            // Give server time to recognize the Version
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, testProperties.getProperty("TEST_PROJECT"),
                    testProperties.getProperty("TEST_VERSION"), null,
                    null, "4096");
            pbScan.setverbose(false);
            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("The scan target :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingVariableProjectName() throws Exception {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, "${JOB_NAME}", testProperties.getProperty("TEST_VERSION"),
                    PhaseEnum.DEVELOPMENT.name(),
                    DistributionEnum.EXTERNAL.name(), "4096");
            pbScan.setverbose(false);
            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, projectName);
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Project Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("The scan target :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(projectName).getId());
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingThroughProxy() throws Exception {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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
            projectId = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            String versionId = restHelper.createHubVersion(testProperties.getProperty("TEST_VERSION"), projectId, PhaseEnum.DEVELOPMENT.name(),
                    DistributionEnum.EXTERNAL.name());
            assertNotNull(versionId);
            // Give server time to recognize the Version
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, testProperties.getProperty("TEST_PROJECT"),
                    testProperties.getProperty("TEST_VERSION"), null,
                    null, "4096");
            pbScan.setverbose(false);
            pbScan.setTEST(true);
            jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST_PASSTHROUGH"),
                    Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT_PASSTHROUGH")));

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            Assert.assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyHost="));
            Assert.assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPort="));

            Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Using proxy: '" + testProperties.getProperty("TEST_PROXY_HOST_PASSTHROUGH") + "' at Port: '"
                    + testProperties.getProperty("TEST_PROXY_PORT_PASSTHROUGH") + "'"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("The scan target :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));

        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    // @Test
    // public void completeRunthroughAndScanWithMappingThroughBASICProxy() throws Exception {
    // Jenkins jenkins = j.jenkins;
    //
    // ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);
    //
    // IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
    // iScanDesc.setInstallations(iScanInstall);
    //
    // CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
    // UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null,
    // null,
    // testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
    // store.addCredentials(Domain.global(), credential);
    //
    // HubServerInfo serverInfo = new HubServerInfo();
    // serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // serverInfo.setCredentialsId(credential.getId());
    //
    // ScanJobs oneScan = new ScanJobs("");
    // ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
    // ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
    // ScanJobs[] scans = new ScanJobs[3];
    // scans[0] = oneScan;
    // scans[1] = twoScan;
    // scans[2] = threeScan;
    //
    // PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
    // scanDesc.setHubServerInfo(serverInfo);
    // String projectId = null;
    // try {
    // projectId = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
    // Assert.assertNotNull(projectId);
    // // Give server time to recognize the Project
    // Thread.sleep(2000);
    // String versionId = restHelper.createHubVersion(testProperties.getProperty("TEST_VERSION"), projectId,
    // PhaseEnum.DEVELOPMENT.name(),
    // DistributionEnum.EXTERNAL.name());
    // assertNotNull(versionId);
    // // Give server time to recognize the Version
    // Thread.sleep(2000);
    //
    // PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, testProperties.getProperty("TEST_PROJECT"),
    // testProperties.getProperty("TEST_VERSION"), null,
    // null, "4096");
    // pbScan.setTEST(true);
    // jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST_BASIC"),
    // Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT_BASIC")),
    // testProperties.getProperty("TEST_PROXY_USER_BASIC"),
    // testProperties.getProperty("TEST_PROXY_PASSWORD_BASIC"));
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // project.getPublishersList().add(pbScan);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // System.out.println(buildOutput);
    //
    // Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
    // Assert.assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyHost="));
    // Assert.assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPort="));
    // Assert.assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyUser="));
    // Assert.assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPassword="));
    //
    // Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
    // Assert.assertTrue(buildOutput, buildOutput.contains(
    // "You can view the BlackDuck Scan CLI logs at :"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Using proxy: '" +
    // testProperties.getProperty("TEST_PROXY_HOST_BASIC") + "' at Port: '"
    // + testProperties.getProperty("TEST_PROXY_PORT_BASIC") + "'"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Version Id:"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("The scan target :"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
    //
    // } finally {
    // restHelper.deleteHubProject(projectId);
    // }
    // }
    //
    // @Test
    // public void completeRunthroughAndScanWithMappingThroughDIGESTProxy() throws Exception {
    // Jenkins jenkins = j.jenkins;
    //
    // ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);
    //
    // IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
    // iScanDesc.setInstallations(iScanInstall);
    //
    // CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
    // UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null,
    // null,
    // testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
    // store.addCredentials(Domain.global(), credential);
    //
    // HubServerInfo serverInfo = new HubServerInfo();
    // serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // serverInfo.setCredentialsId(credential.getId());
    //
    // ScanJobs oneScan = new ScanJobs("");
    // ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
    // ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
    // ScanJobs[] scans = new ScanJobs[3];
    // scans[0] = oneScan;
    // scans[1] = twoScan;
    // scans[2] = threeScan;
    //
    // PostBuildScanDescriptor scanDesc = jenkins.getExtensionList(Descriptor.class).get(PostBuildScanDescriptor.class);
    // scanDesc.setHubServerInfo(serverInfo);
    // String projectId = null;
    // try {
    // projectId = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
    // Assert.assertNotNull(projectId);
    // // Give server time to recognize the Project
    // Thread.sleep(2000);
    // String versionId = restHelper.createHubVersion(testProperties.getProperty("TEST_VERSION"), projectId,
    // PhaseEnum.DEVELOPMENT.name(),
    // DistributionEnum.EXTERNAL.name());
    // assertNotNull(versionId);
    // // Give server time to recognize the Version
    // Thread.sleep(2000);
    //
    // PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, testProperties.getProperty("TEST_PROJECT"),
    // testProperties.getProperty("TEST_VERSION"), null,
    // null, "4096");
    // pbScan.setverbose(false);
    // pbScan.setTEST(true);
    // jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST_DIGEST"),
    // Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT_DIGEST")),
    // testProperties.getProperty("TEST_PROXY_USER_DIGEST"),
    // testProperties.getProperty("TEST_PROXY_PASSWORD_DIGEST"));
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // project.getPublishersList().add(pbScan);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // System.out.println(buildOutput);
    //
    // Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
    // Assert.assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyHost="));
    // Assert.assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPort="));
    // Assert.assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyUser="));
    // Assert.assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPassword="));
    //
    // Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
    // Assert.assertTrue(buildOutput, buildOutput.contains(
    // "You can view the BlackDuck Scan CLI logs at :"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Using proxy: '" +
    // testProperties.getProperty("TEST_PROXY_HOST_DIGEST") + "' at Port: '"
    // + testProperties.getProperty("TEST_PROXY_PORT_DIGEST") + "'"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Version Id:"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("The scan target :"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
    // Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
    //
    // } finally {
    // restHelper.deleteHubProject(projectId);
    // }
    // }

    @Test
    public void completeRunthroughAndScanWithMappingWithProxyIgnored() throws Exception {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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
            projectId = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            String versionId = restHelper.createHubVersion(testProperties.getProperty("TEST_VERSION"), projectId, PhaseEnum.DEVELOPMENT.name(),
                    DistributionEnum.EXTERNAL.name());
            assertNotNull(versionId);
            // Give server time to recognize the Version
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, testProperties.getProperty("TEST_PROJECT"),
                    testProperties.getProperty("TEST_VERSION"), null,
                    null, "4096");
            pbScan.setverbose(false);
            pbScan.setTEST(true);
            URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));

            jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST_PASSTHROUGH"),
                    Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT_PASSTHROUGH")), null, null, url.getHost());

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(buildOutput, !buildOutput.contains("Using proxy: '" + testProperties.getProperty("TEST_PROXY_HOST") + "' at Port: '"
                    + testProperties.getProperty("TEST_PROXY_PORT") + "'"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("The scan target :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));

        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingToNonExistentProject() throws Exception {
        try {
            Jenkins jenkins = j.jenkins;

            ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, PROJECT_NAME_NOT_EXISTING, PROJECT_RELEASE_NOT_EXISTING,
                    PhaseEnum.DEVELOPMENT.name(),
                    DistributionEnum.EXTERNAL.name(), "4096");
            pbScan.setverbose(false);
            pbScan.setTEST(true);
            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Scan target exists at :"));

            // URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));

            Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Project Id: '"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Version Id: '"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished: SUCCESS"));
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(PROJECT_NAME_NOT_EXISTING).getId());
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingToNonExistentVersion() throws Exception {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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
            projectId = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, testProperties.getProperty("TEST_PROJECT"), PROJECT_RELEASE_NOT_EXISTING,
                    PhaseEnum.DEVELOPMENT.name(),
                    DistributionEnum.EXTERNAL.name(),
                    "4096");
            pbScan.setverbose(false);
            pbScan.setTEST(true);
            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Scan target exists at :"));

            // URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));

            Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Version Id: '"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished: SUCCESS"));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingScansAlreadyMapped() throws Exception {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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
            projectId = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);
            String versionId = restHelper.createHubVersion(testProperties.getProperty("TEST_VERSION"), projectId, PhaseEnum.DEVELOPMENT.name(),
                    DistributionEnum.EXTERNAL.name());
            assertNotNull(versionId);
            // Give server time to recognize the Version
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, testProperties.getProperty("TEST_PROJECT"),
                    testProperties.getProperty("TEST_VERSION"),
                    PhaseEnum.DEVELOPMENT.name(), DistributionEnum.EXTERNAL.name(),
                    "4096");
            pbScan.setverbose(false);
            pbScan.setTEST(true);
            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            // First Run scans and maps the scans to the Project Version
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("The scan target :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));

            // Second run, scans should already be mapped
            build = project.scheduleBuild2(0).get();
            buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished in"));
            Assert.assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            Assert.assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("The scan target :"));
            Assert.assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
            Assert.assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
            Assert.assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void scanInvalidPassword() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, null, null, null, null, "4096");
        pbScan.setverbose(false);
        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);

        Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
        Assert.assertTrue(buildOutput, buildOutput.contains("Unauthorized (401)"));
    }

    @Test
    public void scanInvalidUser() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, null, null, null, null, "4096");
        pbScan.setverbose(false);
        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);

        Assert.assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
        Assert.assertTrue(buildOutput, buildOutput.contains("Unauthorized (401)"));
    }

    @Test
    public void setJDKUserSelectedNonExistent() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);
        System.err.println("iScanInstall: " + iScanInstall + "(\"" + iScanInstallPath + "\")");

        IScanDescriptor iScanDesc = jenkins.getExtensionList(ToolDescriptor.class).get(IScanDescriptor.class);
        System.err.println("iScanDescriptor: " + iScanDesc);

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

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, null, null, null, null, "4096");
        pbScan.setverbose(false);
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
        //
        // Assert.assertTrue(buildOutputList.get(3).contains("Could not find the specified Java installation, checking the JAVA_HOME variable."));
        // Assert.assertTrue(buildOutputList
        // .get(4)
        // .contains(
        // "Need to define a JAVA_HOME or select an installed JDK."));
    }

    @Test
    public void setJDKEnvSelectedNonExistent() throws IOException, InterruptedException, ExecutionException {

        Jenkins jenkins = j.jenkins;

        ScanInstallation iScanInstall = new ScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);

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

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, DEFAULT_ISCAN, null, null, null, null, "4096");
        pbScan.setverbose(false);
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
        //
        // Assert.assertTrue(buildOutputList
        // .get(3)
        // .contains(
        // "Could not find the specified Java installation, checking the JAVA_HOME variable."));
        // Assert.assertTrue(buildOutputList
        // .get(4)
        // .contains(
        // "Could not find the specified Java installation at:"));
    }

}
