package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import hudson.ProxyConfiguration;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.ScanJobs;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubScan;
import com.blackducksoftware.integration.hub.jenkins.action.HubReportAction;
import com.blackducksoftware.integration.hub.jenkins.cli.HubScanInstallation;
import com.blackducksoftware.integration.hub.jenkins.tests.utils.JenkinsHubIntTestHelper;
import com.blackducksoftware.integration.hub.response.DistributionEnum;
import com.blackducksoftware.integration.hub.response.PhaseEnum;
import com.blackducksoftware.integration.hub.response.ProjectItem;
import com.blackducksoftware.integration.hub.response.VersionComparison;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class ScanIntegrationTest {

    private static final String CLI_VERSION = "2.1.2";

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
        basePath = ScanIntegrationTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        basePath = basePath.substring(0, basePath.indexOf(File.separator + "target"));
        basePath = basePath + File.separator + "test-workspace";
        iScanInstallPath = basePath + File.separator + "scan.cli-" + CLI_VERSION;
        System.err.println("*************** " + iScanInstallPath);
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

        HubScanInstallation iScanInstall = new HubScanInstallation(HubScanInstallation.AUTO_INSTALL_TOOL_NAME, iScanInstallPath, null);

        HubServerInfoSingleton.getInstance().setHubScanInstallation(iScanInstall);

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

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());
        serverInfo.setTimeout(200);

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, null, null, null, null, "4096", false, "0");

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);

        assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
        assertTrue(buildOutput, buildOutput.contains("Running on : master"));
        assertTrue(buildOutput, buildOutput.contains("BlackDuck Scan directory:"));
        assertTrue(buildOutput, buildOutput.contains("directories in the BlackDuck Scan directory"));
        assertTrue(buildOutput, buildOutput.contains("BlackDuck Scan lib directory:"));
        assertTrue(buildOutput, buildOutput.contains("BlackDuck Scan lib file:"));
        assertTrue(buildOutput, buildOutput.contains("Using this BlackDuck Scan CLI at : "));
        assertTrue(buildOutput, buildOutput.contains("Scan target exists at :"));
        URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        assertTrue(buildOutput, buildOutput.contains("Using this Hub hostname : '" + url.getHost()));
        assertTrue(buildOutput, buildOutput.contains("Using this java installation : "));
        assertTrue(buildOutput, buildOutput.contains("Finished in"));
        assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
        assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
    }

    @Test
    public void completeRunthroughAndScanWithCLIAutoInstall() throws IOException, InterruptedException, ExecutionException {
        HubScanInstallation orgHubInstall = HubServerInfoSingleton.getInstance().getHubScanInstallation();
        try {
            Jenkins jenkins = j.jenkins;

            CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
            UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                    testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
            store.addCredentials(Domain.global(), credential);

            HubServerInfo serverInfo = new HubServerInfo();
            serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
            serverInfo.setCredentialsId(credential.getId());
            serverInfo.setTimeout(200);

            ScanJobs oneScan = new ScanJobs("");
            ScanJobs[] scans = new ScanJobs[1];
            scans[0] = oneScan;

            HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

            PostBuildScanDescriptor.checkHubScanTool(serverInfo.getServerUrl());

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, null, null, null, null, "4096", false, "0");

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            assertTrue(buildOutput, buildOutput.contains("Running on : master"));
            assertTrue(buildOutput, buildOutput.contains("BlackDuck Scan directory:"));
            assertTrue(buildOutput, buildOutput.contains("directories in the BlackDuck Scan directory"));
            assertTrue(buildOutput, buildOutput.contains("BlackDuck Scan lib directory:"));
            assertTrue(buildOutput, buildOutput.contains("BlackDuck Scan lib file:"));
            assertTrue(buildOutput, buildOutput.contains("Using this BlackDuck Scan CLI at : "));
            assertTrue(buildOutput, buildOutput.contains("Scan target exists at :"));
            URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));
            assertTrue(buildOutput, buildOutput.contains("Using this Hub hostname : '" + url.getHost()));
            assertTrue(buildOutput, buildOutput.contains("Using this java installation : "));
            assertTrue(buildOutput, buildOutput.contains("Finished in"));
            assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
        } finally {
            HubServerInfoSingleton.getInstance().setHubScanInstallation(orgHubInstall);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMapping() throws Exception {
        Jenkins jenkins = j.jenkins;

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());
        serverInfo.setTimeout(200);

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs[] scans = new ScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
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

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
                    testProperties.getProperty("TEST_VERSION"), null,
                    null, "4096", false, "0");

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            assertTrue(buildOutput, buildOutput.contains("Finished in"));
            assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            if (isHubOlderThanThisVersion("2.3.2")) {
                // Only to be asserted if run against hub <2.3.2, because the plugin does the
                // project/version/codelocation

                assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
                assertTrue(buildOutput, buildOutput.contains("The scan target :"));
                assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
                assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
                assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
                assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            }
            assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingAndGenerateReport() throws Exception {
        // TODO
        Jenkins jenkins = j.jenkins;

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());
        serverInfo.setTimeout(200);

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs fourScan = new ScanJobs("ch-simple-web");
        ScanJobs fiveScan = new ScanJobs("ch-simple-web/simple-webapp");
        ScanJobs[] scans = new ScanJobs[5];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;
        scans[3] = fourScan;
        scans[4] = fiveScan;

        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
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

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
                    testProperties.getProperty("TEST_VERSION"), null,
                    null, "4096", true, "5");

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            assertTrue(buildOutput, buildOutput.contains("-> Generate Hub report : true"));
            assertTrue(buildOutput, buildOutput.contains("-> Maximum wait time for the report : 5 minutes"));
            assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            assertTrue(buildOutput, buildOutput.contains("Finished in"));
            assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            assertTrue(buildOutput, buildOutput.contains("The bom has been updated, generating the report."));
            assertTrue(buildOutput, buildOutput.contains("Finished retrieving the report."));

            HubReportAction hubReportAction = build.getAction(HubReportAction.class);

            assertNotNull(hubReportAction);
            assertNotNull(hubReportAction.getReport());
            assertNotNull(hubReportAction.getReleaseSummary());

            if (isHubOlderThanThisVersion("2.3.2")) {
                // Only to be asserted if run against hub <2.3.2, because the plugin does the
                // project/version/codelocation

                assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
                assertTrue(buildOutput, buildOutput.contains("The scan target :"));
                assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
                assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
                assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
                assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            }
            assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }

    }

    @Test
    public void completeRunthroughAndScanWithMappingVariableProjectName() throws Exception {
        Jenkins jenkins = j.jenkins;

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());
        serverInfo.setTimeout(200);

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs[] scans = new ScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;
        String projectName = "Jenkins Hub Integration Variable Project Name";
        try {
            HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, "${JOB_NAME}", testProperties.getProperty("TEST_VERSION"),
                    PhaseEnum.DEVELOPMENT.name(),
                    DistributionEnum.EXTERNAL.name(), "4096", false, "0");

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, projectName);
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            assertTrue(buildOutput, buildOutput.contains("Finished in"));
            assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            assertTrue(buildOutput, buildOutput.contains("Project Id:"));
            assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            if (isHubOlderThanThisVersion("2.3.2")) {
                // Only to be asserted if run against hub <2.3.2, because the plugin does the
                // project/version/codelocation
                assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
                assertTrue(buildOutput, buildOutput.contains("The scan target :"));
                assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
                assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
                assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
                assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            }
            assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(projectName).getId());
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingThroughProxy() throws Exception {
        Jenkins jenkins = j.jenkins;

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());
        serverInfo.setTimeout(200);

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs[] scans = new ScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
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

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
                    testProperties.getProperty("TEST_VERSION"), null,
                    null, "4096", false, "0");

            jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST_PASSTHROUGH"),
                    Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT_PASSTHROUGH")));

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();

            File logFile = build.getLogFile();
            System.out.println("Log File : " + logFile.getAbsolutePath() + "!!!!!!!!!!");

            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyHost="));
            assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPort="));

            assertTrue(buildOutput, buildOutput.contains("Finished in"));
            assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            assertTrue(buildOutput, buildOutput.contains("Using proxy: '" + testProperties.getProperty("TEST_PROXY_HOST_PASSTHROUGH") + "' at Port: '"
                    + testProperties.getProperty("TEST_PROXY_PORT_PASSTHROUGH") + "'"));
            assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            if (isHubOlderThanThisVersion("2.3.2")) {
                // Only to be asserted if run against hub <2.3.2, because the plugin does the
                // project/version/codelocation
                assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
                assertTrue(buildOutput, buildOutput.contains("The scan target :"));
                assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
                assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
                assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
                assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            }
            assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));

        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    // @Test
    // public void completeRunthroughAndScanWithMappingThroughBASICProxy() throws Exception {
    // Jenkins jenkins = j.jenkins;
    //
    // HubScanInstallation iScanInstall = new HubScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);
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
    // assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
    // assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyHost="));
    // assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPort="));
    // assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyUser="));
    // assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPassword="));
    //
    // assertTrue(buildOutput, buildOutput.contains("Finished in"));
    // assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
    // assertTrue(buildOutput, buildOutput.contains(
    // "You can view the BlackDuck Scan CLI logs at :"));
    // assertTrue(buildOutput, buildOutput.contains("Using proxy: '" +
    // testProperties.getProperty("TEST_PROXY_HOST_BASIC") + "' at Port: '"
    // + testProperties.getProperty("TEST_PROXY_PORT_BASIC") + "'"));
    // assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
    // assertTrue(buildOutput, buildOutput.contains("Version Id:"));
    /*
     * Only to be asserted if run against hub <2.3.1
     *
     * // assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
     * // assertTrue(buildOutput, buildOutput.contains("The scan target :"));
     * // assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
     * // assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
     * // assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
     * // assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
     */
    // assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
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
    // HubScanInstallation iScanInstall = new HubScanInstallation(DEFAULT_ISCAN, iScanInstallPath, null);
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
    //
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
    // assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
    // assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyHost="));
    // assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPort="));
    // assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyUser="));
    // assertTrue(buildOutput, buildOutput.contains("-Dhttp.proxyPassword="));
    //
    // assertTrue(buildOutput, buildOutput.contains("Finished in"));
    // assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
    // assertTrue(buildOutput, buildOutput.contains(
    // "You can view the BlackDuck Scan CLI logs at :"));
    // assertTrue(buildOutput, buildOutput.contains("Using proxy: '" +
    // testProperties.getProperty("TEST_PROXY_HOST_DIGEST") + "' at Port: '"
    // + testProperties.getProperty("TEST_PROXY_PORT_DIGEST") + "'"));
    // assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
    // assertTrue(buildOutput, buildOutput.contains("Version Id:"));
    // if (isHubOlderThanThisVersion("2.3.2")) {
    // Only to be asserted if run against hub <2.3.2, because the plugin does the project/version/codelocation
    // assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
    // assertTrue(buildOutput, buildOutput.contains("The scan target :"));
    // assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
    // assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
    // assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
    // assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
    // }
    // assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
    //
    // } finally {
    // restHelper.deleteHubProject(projectId);
    // }
    // }

    @Test
    public void completeRunthroughAndScanWithMappingWithProxyIgnored() throws Exception {
        Jenkins jenkins = j.jenkins;

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());
        serverInfo.setTimeout(200);

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs[] scans = new ScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;
        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
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

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
                    testProperties.getProperty("TEST_VERSION"), null,
                    null, "4096", false, "0");

            URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));

            jenkins.proxy = new ProxyConfiguration(testProperties.getProperty("TEST_PROXY_HOST_PASSTHROUGH"),
                    Integer.valueOf(testProperties.getProperty("TEST_PROXY_PORT_PASSTHROUGH")), null, null, url.getHost());

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            assertTrue(buildOutput, buildOutput.contains("Finished in"));
            assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            assertTrue(buildOutput, !buildOutput.contains("Using proxy: '" + testProperties.getProperty("TEST_PROXY_HOST") + "' at Port: '"
                    + testProperties.getProperty("TEST_PROXY_PORT") + "'"));
            assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            if (isHubOlderThanThisVersion("2.3.2")) {
                // Only to be asserted if run against hub <2.3.2, because the plugin does the
                // project/version/codelocation
                assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
                assertTrue(buildOutput, buildOutput.contains("The scan target :"));
                assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
                assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
                assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
                assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            }
            assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));

        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    private boolean isHubOlderThanThisVersion(String version) throws IOException, BDRestException, URISyntaxException {
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
            Jenkins jenkins = j.jenkins;

            CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
            UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                    testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
            store.addCredentials(Domain.global(), credential);

            HubServerInfo serverInfo = new HubServerInfo();
            serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
            serverInfo.setCredentialsId(credential.getId());
            serverInfo.setTimeout(200);

            ScanJobs oneScan = new ScanJobs("");
            ScanJobs[] scans = new ScanJobs[1];
            scans[0] = oneScan;

            HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, PROJECT_NAME_NOT_EXISTING, PROJECT_RELEASE_NOT_EXISTING,
                    PhaseEnum.DEVELOPMENT.name(),
                    DistributionEnum.EXTERNAL.name(), "4096", false, "0");

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            assertTrue(buildOutput, buildOutput.contains("Scan target exists at :"));

            // URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));

            assertTrue(buildOutput, buildOutput.contains("Finished in"));
            assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            assertTrue(buildOutput, buildOutput.contains("Project Id: '"));
            assertTrue(buildOutput, buildOutput.contains("Version Id: '"));
            if (isHubOlderThanThisVersion("2.3.2")) {
                // Only to be asserted if run against hub <2.3.2, because the plugin does the
                // project/version/codelocation
                assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan"));
            }
            assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
            assertTrue(buildOutput, buildOutput.contains("Finished: SUCCESS"));
        } finally {
            restHelper.deleteHubProject(restHelper.getProjectByName(PROJECT_NAME_NOT_EXISTING).getId());
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingToNonExistentVersion() throws Exception {
        Jenkins jenkins = j.jenkins;

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());
        serverInfo.setTimeout(200);

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs[] scans = new ScanJobs[1];
        scans[0] = oneScan;

        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
        String projectId = null;
        try {
            projectId = restHelper.createHubProject(testProperties.getProperty("TEST_PROJECT"));
            Assert.assertNotNull(projectId);
            // Give server time to recognize the Project
            Thread.sleep(2000);

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
                    PROJECT_RELEASE_NOT_EXISTING,
                    PhaseEnum.DEVELOPMENT.name(),
                    DistributionEnum.EXTERNAL.name(),
                    "4096", false, "0");

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
            project.setCustomWorkspace(testWorkspace);

            project.getPublishersList().add(pbScan);

            FreeStyleBuild build = project.scheduleBuild2(0).get();
            String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            assertTrue(buildOutput, buildOutput.contains("Scan target exists at :"));

            // URL url = new URL(testProperties.getProperty("TEST_HUB_SERVER_URL"));

            assertTrue(buildOutput, buildOutput.contains("Finished in"));
            assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            assertTrue(buildOutput, buildOutput.contains("Version Id: '"));
            if (isHubOlderThanThisVersion("2.3.2")) {
                // Only to be asserted if run against hub <2.3.2, because the plugin does the
                // project/version/codelocation
                assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan"));
            }
            assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
            assertTrue(buildOutput, buildOutput.contains("Finished: SUCCESS"));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void completeRunthroughAndScanWithMappingScansAlreadyMapped() throws Exception {
        Jenkins jenkins = j.jenkins;

        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                testProperties.getProperty("TEST_USERNAME"), testProperties.getProperty("TEST_PASSWORD"));
        store.addCredentials(Domain.global(), credential);

        HubServerInfo serverInfo = new HubServerInfo();
        serverInfo.setServerUrl(testProperties.getProperty("TEST_HUB_SERVER_URL"));
        serverInfo.setCredentialsId(credential.getId());
        serverInfo.setTimeout(200);

        ScanJobs oneScan = new ScanJobs("");
        ScanJobs twoScan = new ScanJobs("ch-simple-web/simple-webapp/target");
        ScanJobs threeScan = new ScanJobs("ch-simple-web/simple-webapp/target/simple-webapp.war");
        ScanJobs[] scans = new ScanJobs[3];
        scans[0] = oneScan;
        scans[1] = twoScan;
        scans[2] = threeScan;

        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);
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

            PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, testProperties.getProperty("TEST_PROJECT"),
                    testProperties.getProperty("TEST_VERSION"),
                    PhaseEnum.DEVELOPMENT.name(), DistributionEnum.EXTERNAL.name(),
                    "4096", false, "0");

            FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
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
                    "You can view the BlackDuck Scan CLI logs at :"));
            assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            if (isHubOlderThanThisVersion("2.3.2")) {
                // Only to be asserted if run against hub <2.3.2, because the plugin does the
                // project/version/codelocation
                assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
                assertTrue(buildOutput, buildOutput.contains("The scan target :"));
                assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
                assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
                assertTrue(buildOutput, buildOutput.contains("Mapping the scan location with id:"));
                assertTrue(buildOutput, buildOutput.contains("Successfully mapped the scan with id:"));
            }
            assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));

            // Second run, scans should already be mapped
            build = project.scheduleBuild2(0).get();
            buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
            System.out.println(buildOutput);

            assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
            assertTrue(buildOutput, buildOutput.contains("Finished in"));
            assertTrue(buildOutput, buildOutput.contains("with status SUCCESS"));
            assertTrue(buildOutput, buildOutput.contains(
                    "You can view the BlackDuck Scan CLI logs at :"));
            assertTrue(buildOutput, buildOutput.contains("Project Id: '" + projectId + "'"));
            assertTrue(buildOutput, buildOutput.contains("Version Id:"));
            if (isHubOlderThanThisVersion("2.3.2")) {
                // Only to be asserted if run against hub <2.3.2, because the plugin does the
                // project/version/codelocation
                assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
                assertTrue(buildOutput, buildOutput.contains("The scan target :"));
                assertTrue(buildOutput, buildOutput.contains("' has Scan Location Id:"));
                assertTrue(buildOutput, buildOutput.contains("Checking for the scan location with Host name:"));
                assertTrue(buildOutput, buildOutput.contains("These scan Id's were found for the scan targets."));
            }
            assertTrue(buildOutput, buildOutput.contains("Finished running Black Duck Scans."));
        } finally {
            restHelper.deleteHubProject(projectId);
        }
    }

    @Test
    public void scanInvalidPassword() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

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

        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, null, null, null, null, "4096", false, "0");

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);

        assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
        assertTrue(buildOutput, buildOutput.contains("Unauthorized (401)"));
    }

    @Test
    public void scanInvalidUser() throws IOException, InterruptedException, ExecutionException {
        Jenkins jenkins = j.jenkins;

        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
        project.setCustomWorkspace(testWorkspace);

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

        HubServerInfoSingleton.getInstance().setServerInfo(serverInfo);

        PostBuildHubScan pbScan = new PostBuildHubScan(scans, false, null, null, null, null, "4096", false, "0");

        project.getPublishersList().add(pbScan);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
        System.out.println(buildOutput);

        assertTrue(buildOutput, buildOutput.contains("Starting BlackDuck Scans..."));
        assertTrue(buildOutput, buildOutput.contains("Unauthorized (401)"));
    }

}
