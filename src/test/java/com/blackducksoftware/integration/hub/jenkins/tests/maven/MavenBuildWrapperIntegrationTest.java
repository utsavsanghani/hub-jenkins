package com.blackducksoftware.integration.hub.jenkins.tests.maven;

import java.util.Properties;

public class MavenBuildWrapperIntegrationTest {

    private static String basePath;

    private static String testWorkspace;

    private static Properties testProperties;

    // @Rule
    // public static JenkinsRule j = new JenkinsRule();
    //
    // @Rule
    // public ExpectedException exception = ExpectedException.none();
    //
    // public void addHubServerInfo(HubServerInfo hubServerInfo) {
    // resetPublisherDescriptors();
    //
    // PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
    // descriptor.setHubServerInfo(hubServerInfo);
    // j.getInstance().getDescriptorList(Publisher.class).add(descriptor);
    // }
    //
    // public void resetPublisherDescriptors() {
    // while (j.getInstance().getDescriptorList(Publisher.class).size() != 0) {
    // j.getInstance().getDescriptorList(Publisher.class).remove(0);
    // }
    // }
    //
    // public void addMavenBuildWrapperDescriptor() {
    // MavenBuildWrapperDescriptor descriptor = new MavenBuildWrapperDescriptor();
    // j.getInstance().getDescriptorList(BuildWrapper.class).add(descriptor);
    // }
    //
    // public UsernamePasswordCredentialsImpl addCredentialToGlobalStore(String username, String password) {
    // UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null,
    // null,
    // username, password);
    // UserFacingAction store = new UserFacingAction();
    // try {
    // store.getStore().addCredentials(Domain.global(), credential);
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // return credential;
    // }
    //
    // @BeforeClass
    // public static void init() throws Exception {
    // basePath = ScanIntegrationTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    // basePath = basePath.substring(0, basePath.indexOf(File.separator + "target"));
    // basePath = basePath + File.separator + "test-workspace";
    // testWorkspace = basePath + File.separator + "mavenWorkspace";
    //
    // testProperties = new Properties();
    // ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    // InputStream is = classLoader.getResourceAsStream("test.properties");
    // try {
    // testProperties.load(is);
    // } catch (IOException e) {
    // System.err.println("reading test.properties failed!");
    // }
    // // p.load(new FileReader(new File("test.properties")));
    // System.out.println(testProperties.getProperty("TEST_HUB_SERVER_URL"));
    // System.out.println(testProperties.getProperty("TEST_USERNAME"));
    // System.out.println(testProperties.getProperty("TEST_PASSWORD"));
    // }
    //
    // @Test
    // public void testGradleBuilder() throws Exception {
    // Jenkins jenkins = j.jenkins;
    // addMavenBuildWrapperDescriptor();
    //
    // MavenBuildWrapper buildWrapper = new MavenBuildWrapper(null, false, null, null, null, null);
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // Gradle builder = new Gradle("", "", "", testWorkspace, "", "", true, false, true, false);
    // project.getBuildersList().add(builder);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("This Wrapper should be run with a Maven Builder"));
    // assertTrue(buildOutput, buildOutput.contains("Will not run the Hub Maven Build wrapper."));
    // }
    //
    // @Test
    // public void testRunNoBuilder() throws Exception {
    // Jenkins jenkins = j.jenkins;
    // addMavenBuildWrapperDescriptor();
    //
    // MavenBuildWrapper buildWrapper = new MavenBuildWrapper(null, false, null, null, null, null);
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("No Builder found for this job."));
    // assertTrue(buildOutput, buildOutput.contains("Will not run the Hub Maven Build wrapper."));
    // }
    //
    // @Test
    // public void testRunNotConfiguredNoGlobalConfig() throws Exception {
    // Jenkins jenkins = j.jenkins;
    // addMavenBuildWrapperDescriptor();
    //
    // MavenBuildWrapper buildWrapper = new MavenBuildWrapper(null, false, null, null, null, null);
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // MavenSupport.addMavenBuilder(project, null);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("Could not find the Hub global configuration!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project name configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project version configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Maven scopes configured!"));
    // }
    //
    // @Test
    // public void testRunNotConfiguredEmptyGlobalConfig() throws Exception {
    // Jenkins jenkins = j.jenkins;
    // addMavenBuildWrapperDescriptor();
    //
    // addHubServerInfo(new HubServerInfo("", ""));
    //
    // MavenBuildWrapper buildWrapper = new MavenBuildWrapper(null, false, null, null, null, null);
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // MavenSupport.addMavenBuilder(project, null);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, !buildOutput.contains("Could not find the Hub global configuration!"));
    // assertTrue(buildOutput, buildOutput.contains("The Hub server URL is not configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub credentials configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project name configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project version configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Maven scopes configured!"));
    // }
    //
    // @Test
    // public void testRunNotConfiguredEmptyCredentialsGlobalConfig() throws Exception {
    // Jenkins jenkins = j.jenkins;
    // addMavenBuildWrapperDescriptor();
    //
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore("", "");
    // addHubServerInfo(new HubServerInfo("http://server.com", credential.getId()));
    //
    // MavenBuildWrapper buildWrapper = new MavenBuildWrapper(null, false, null, null, null, null);
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // MavenSupport.addMavenBuilder(project, null);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, !buildOutput.contains("Could not find the Hub global configuration!"));
    // assertTrue(buildOutput, !buildOutput.contains("The Hub server URL is not configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub username configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub password configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project name configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Hub project version configured!"));
    // assertTrue(buildOutput, buildOutput.contains("No Maven scopes configured!"));
    // }
    //
    // @Test
    // public void testRunConfiguredNoPom() throws Exception {
    // File buildInfo = new File(testWorkspace, "build-info.json");
    //
    // try {
    // Jenkins jenkins = j.jenkins;
    // addMavenBuildWrapperDescriptor();
    //
    // UsernamePasswordCredentialsImpl credential =
    // addCredentialToGlobalStore(testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // addHubServerInfo(new HubServerInfo(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId()));
    //
    // MavenBuildWrapper buildWrapper = new MavenBuildWrapper("Compile", false,
    // testProperties.getProperty("TEST_PROJECT"),
    // PhaseEnum.DEVELOPMENT.name(),
    // DistributionEnum.INTERNAL.name(), testProperties.getProperty("TEST_VERSION"));
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // MavenSupport.addMavenBuilder(project, null);
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("Please verify you invoked Maven from the correct directory."));
    // assertTrue(buildOutput, buildOutput.contains("Finished: FAILURE"));
    // assertTrue(buildOutput, buildInfo.exists());
    // } finally {
    // if (buildInfo.exists()) {
    // buildInfo.delete();
    // }
    // }
    // }
    //
    // @Test
    // public void testRunConfiguredEmptyPom() throws Exception {
    // File buildInfo = new File(testWorkspace, "build-info.json");
    //
    // try {
    // Jenkins jenkins = j.jenkins;
    // addMavenBuildWrapperDescriptor();
    //
    // UsernamePasswordCredentialsImpl credential =
    // addCredentialToGlobalStore(testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // addHubServerInfo(new HubServerInfo(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId()));
    //
    // MavenBuildWrapper buildWrapper = new MavenBuildWrapper("Compile", false,
    // testProperties.getProperty("TEST_PROJECT"),
    // PhaseEnum.DEVELOPMENT.name(),
    // DistributionEnum.INTERNAL.name(), testProperties.getProperty("TEST_VERSION"));
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // MavenSupport.addMavenBuilder(project, "EmptyPom" + File.separator + "pom.xml");
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("Non-readable POM"));
    // assertTrue(buildOutput, buildOutput.contains("Finished: FAILURE"));
    // assertTrue(buildOutput, buildInfo.exists());
    // } finally {
    // if (buildInfo.exists()) {
    // buildInfo.delete();
    // }
    // }
    // }
    //
    // @Test
    // public void testRunConfiguredEmptyProject() throws Exception {
    // File buildInfo = new File(testWorkspace, "build-info.json");
    //
    // try {
    //
    // Jenkins jenkins = j.jenkins;
    // addMavenBuildWrapperDescriptor();
    //
    // UsernamePasswordCredentialsImpl credential =
    // addCredentialToGlobalStore(testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // addHubServerInfo(new HubServerInfo(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId()));
    //
    // MavenBuildWrapper buildWrapper = new MavenBuildWrapper("Compile", false,
    // testProperties.getProperty("TEST_PROJECT"),
    // PhaseEnum.DEVELOPMENT.name(),
    // DistributionEnum.INTERNAL.name(), testProperties.getProperty("TEST_VERSION"));
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // MavenSupport.addMavenBuilder(project, "EmptyProject" + File.separator + "pom.xml");
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("The build could not read 1 project"));
    // assertTrue(buildOutput, buildOutput.contains("Finished: FAILURE"));
    // assertTrue(buildOutput, buildInfo.exists());
    // } finally {
    // if (buildInfo.exists()) {
    // buildInfo.delete();
    // }
    // }
    // }
    //
    // @Test
    // public void testRunConfiguredNoDependencies() throws Exception {
    // File buildInfo = new File(testWorkspace, "build-info.json");
    //
    // try {
    // Jenkins jenkins = j.jenkins;
    // addMavenBuildWrapperDescriptor();
    //
    // UsernamePasswordCredentialsImpl credential =
    // addCredentialToGlobalStore(testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // addHubServerInfo(new HubServerInfo(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId()));
    //
    // MavenBuildWrapper buildWrapper = new MavenBuildWrapper("Compile", false,
    // testProperties.getProperty("TEST_PROJECT"),
    // PhaseEnum.DEVELOPMENT.name(),
    // DistributionEnum.INTERNAL.name(), testProperties.getProperty("TEST_VERSION"));
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // MavenSupport.addMavenBuilder(project, "NoDependencies" + File.separator + "pom.xml");
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("build-info.json"));
    // assertTrue(buildOutput, buildOutput.contains("Finished: SUCCESS"));
    // assertTrue(buildOutput, buildInfo.exists());
    // } finally {
    // if (buildInfo.exists()) {
    // buildInfo.delete();
    // }
    // }
    // }
    //
    // @Test
    // public void testRunConfiguredValidProject() throws Exception {
    // File buildInfo = new File(testWorkspace, "build-info.json");
    //
    // try {
    // Jenkins jenkins = j.jenkins;
    // addMavenBuildWrapperDescriptor();
    //
    // UsernamePasswordCredentialsImpl credential =
    // addCredentialToGlobalStore(testProperties.getProperty("TEST_USERNAME"),
    // testProperties.getProperty("TEST_PASSWORD"));
    // addHubServerInfo(new HubServerInfo(testProperties.getProperty("TEST_HUB_SERVER_URL"), credential.getId()));
    //
    // MavenBuildWrapper buildWrapper = new MavenBuildWrapper("Compile", false,
    // testProperties.getProperty("TEST_PROJECT"),
    // PhaseEnum.DEVELOPMENT.name(),
    // DistributionEnum.INTERNAL.name(), testProperties.getProperty("TEST_VERSION"));
    //
    // FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "Test_job");
    // project.setCustomWorkspace(testWorkspace);
    //
    // MavenSupport.addMavenBuilder(project, "ValidProject" + File.separator + "pom.xml");
    //
    // project.getBuildWrappersList().add(buildWrapper);
    //
    // FreeStyleBuild build = project.scheduleBuild2(0).get();
    // String buildOutput = IOUtils.toString(build.getLogInputStream(), "UTF-8");
    // assertTrue(buildOutput, buildOutput.contains("build-info.json"));
    // assertTrue(buildOutput, buildOutput.contains("Finished: SUCCESS"));
    // assertTrue(buildOutput, buildInfo.exists());
    // } finally {
    // if (buildInfo.exists()) {
    // buildInfo.delete();
    // }
    // }
    // }
}
