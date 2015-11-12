package com.blackducksoftware.integration.hub.jenkins.tests.gradle;


public class BDGradleInitScriptWriterTest {

    private static String testWorkspace;

    private static String cacheDirectory;

    // @BeforeClass
    // public static void init() throws Exception {
    // testWorkspace = ScanIntegrationTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    // testWorkspace = testWorkspace.substring(0, testWorkspace.indexOf(File.separator + "target"));
    // testWorkspace = testWorkspace + File.separator + "test-workspace";
    // testWorkspace = testWorkspace + File.separator + "scriptWriter";
    // File workspace = new File(testWorkspace);
    // if (!workspace.exists()) {
    // workspace.mkdirs();
    // }
    // cacheDirectory = testWorkspace + File.separator + "cache";
    // }
    //
    // private void recursivelyDeleteFile(File target) {
    // if (target.exists()) {
    // if (target.isDirectory()) {
    // if (target.listFiles().length > 0) {
    // for (File fileToDelete : target.listFiles()) {
    // recursivelyDeleteFile(fileToDelete);
    // }
    // }
    // }
    // target.delete();
    // }
    // }
    //
    // @Test
    // public void testGenerateInitScript() throws Exception {
    // Node node = mock(Node.class);
    // when(node.getRootPath()).thenReturn(new FilePath(new File(testWorkspace)));
    //
    // AbstractBuild build = mock(AbstractBuild.class);
    //
    // when(build.getBuiltOn()).thenReturn(node);
    //
    // TestLogger logger = new TestLogger(null);
    //
    // try {
    // BDGradleInitScriptWriter scriptWriter = new BDGradleInitScriptWriter(build, logger);
    // scriptWriter.generateInitScript();
    //
    // File cache = new File(testWorkspace, "cache");
    // assertTrue(cache.exists());
    // File pluginCache = new File(cache, "hub-jenkins");
    // assertTrue(pluginCache.exists());
    //
    // assertTrue(pluginCache.listFiles().length == 3);
    // } finally {
    // File cacheDir = new File(cacheDirectory);
    // recursivelyDeleteFile(cacheDir);
    // }
    // }
}
