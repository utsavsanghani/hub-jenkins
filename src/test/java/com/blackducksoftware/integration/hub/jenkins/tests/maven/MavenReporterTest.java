/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.jenkins.tests.maven;


public class MavenReporterTest {

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
    // public void addHubMavenReporterDescriptor() {
    // HubMavenReporterDescriptor descriptor = new HubMavenReporterDescriptor();
    // j.getInstance().getDescriptorList(MavenReporter.class).add(descriptor);
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
    // @Test
    // public void testIsPluginEnabled() {
    // addHubMavenReporterDescriptor();
    //
    // HubMavenReporter buildWrapper = new HubMavenReporter(null, false, null, null, null, null);
    // assertFalse(buildWrapper.isPluginEnabled());
    //
    // buildWrapper = new HubMavenReporter("compile", false, null, null, null, null);
    // assertFalse(buildWrapper.isPluginEnabled());
    //
    // buildWrapper = new HubMavenReporter("compile", false, "projectName", null, null, null);
    // assertFalse(buildWrapper.isPluginEnabled());
    //
    // buildWrapper = new HubMavenReporter("compile", false, "projectName", "phase", null, null);
    // assertFalse(buildWrapper.isPluginEnabled());
    //
    // buildWrapper = new HubMavenReporter("compile", false, "projectName", "phase", "dist", null);
    // assertFalse(buildWrapper.isPluginEnabled());
    //
    // buildWrapper = new HubMavenReporter("compile", false, "projectName", "phase", "dist", "version");
    // assertFalse(buildWrapper.isPluginEnabled());
    //
    // addHubServerInfo(new HubServerInfo());
    // buildWrapper = new HubMavenReporter("compile", false, "projectName", "phase", "dist", "version");
    // assertFalse(buildWrapper.isPluginEnabled());
    //
    // addHubServerInfo(new HubServerInfo("", null));
    // buildWrapper = new HubMavenReporter("compile", false, "projectName", "phase", "dist", "version");
    // assertFalse(buildWrapper.isPluginEnabled());
    //
    // addHubServerInfo(new HubServerInfo("testServer", null));
    // buildWrapper = new HubMavenReporter("compile", false, "projectName", "phase", "dist", "version");
    // assertFalse(buildWrapper.isPluginEnabled());
    //
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore("", "");
    // addHubServerInfo(new HubServerInfo("testServer", credential.getId()));
    // buildWrapper = new HubMavenReporter("compile", false, "projectName", "phase", "dist", "version");
    // assertTrue(buildWrapper.isPluginEnabled());
    //
    // }
    //
    // @Test
    // public void testValidateConfigurationNotConfiguredNoGlobalConfiguration() {
    // addHubMavenReporterDescriptor();
    //
    // TestLogger logger = new TestLogger(null);
    //
    // HubMavenReporter buildWrapper = new HubMavenReporter(null, false, null, null, null, null);
    // assertFalse(buildWrapper.validateConfiguration(logger));
    //
    // String output = logger.getOutputString();
    // assertTrue(output, output.contains("Could not find the Hub global configuration!"));
    // assertTrue(output, output.contains("No Hub project name configured!"));
    // assertTrue(output, output.contains("No Hub project version configured!"));
    // assertTrue(output, output.contains("No Maven scopes configured!"));
    // }
    //
    // @Test
    // public void testValidateConfigurationWithGlobalConfigurationEmpty() {
    // addHubMavenReporterDescriptor();
    //
    // TestLogger logger = new TestLogger(null);
    //
    // addHubServerInfo(new HubServerInfo("", ""));
    //
    // HubMavenReporter buildWrapper = new HubMavenReporter(null, false, null, null, null, null);
    // assertFalse(buildWrapper.validateConfiguration(logger));
    //
    // String output = logger.getOutputString();
    // assertTrue(output, !output.contains("Could not find the Hub global configuration!"));
    // assertTrue(output, output.contains("The Hub server URL is not configured!"));
    // assertTrue(output, output.contains("No Hub credentials configured!"));
    // }
    //
    // @Test
    // public void testValidateConfigurationWithGlobalConfiguration() {
    // addHubMavenReporterDescriptor();
    //
    // TestLogger logger = new TestLogger(null);
    //
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore("User", "Password");
    // addHubServerInfo(new HubServerInfo("http://server.com", credential.getId()));
    //
    // HubMavenReporter buildWrapper = new HubMavenReporter(null, false, null, null, null, null);
    // assertFalse(buildWrapper.validateConfiguration(logger));
    //
    // String output = logger.getOutputString();
    // assertTrue(output, !output.contains("Could not find the Hub global configuration!"));
    // assertTrue(output, !output.contains("The Hub server URL is not configured!"));
    // assertTrue(output, !output.contains("No Hub credentials configured!"));
    // }
    //
    // @Test
    // public void testValidateConfiguration() {
    // addHubMavenReporterDescriptor();
    //
    // TestLogger logger = new TestLogger(null);
    //
    // UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore("User", "Password");
    // addHubServerInfo(new HubServerInfo("http://server.com", credential.getId()));
    //
    // HubMavenReporter buildWrapper = new HubMavenReporter("Compile", false, "Project", null, null, "Version");
    // assertTrue(buildWrapper.validateConfiguration(logger));
    //
    // String output = logger.getOutputString();
    // assertTrue(output, StringUtils.isBlank(output));
    // }
    //
    // @Test
    // public void testHandleVariableReplacementUnknownVariable() throws Exception {
    // exception.expect(BDJenkinsHubPluginException.class);
    // exception.expectMessage("Variable was not properly replaced. Value : ${TEST}, Result : ${TEST}. Make sure the variable has been properly defined.");
    //
    // HubMavenReporter buildWrapper = new HubMavenReporter(null, false, null, null, null, null);
    //
    // HashMap<String, String> variables = new HashMap<String, String>();
    //
    // buildWrapper.handleVariableReplacement(variables, "${TEST}");
    // }
    //
    // @Test
    // public void testHandleVariableReplacement() throws Exception {
    // HubMavenReporter buildWrapper = new HubMavenReporter(null, false, null, null, null, null);
    //
    // HashMap<String, String> variables = new HashMap<String, String>();
    // variables.put("TEST", "Value");
    //
    // assertEquals("Value", buildWrapper.handleVariableReplacement(variables, "${TEST}"));
    // assertEquals("Value", buildWrapper.handleVariableReplacement(variables, "$TEST"));
    // }
    //
    // @Test
    // public void testGetScopesAsListInvalidScopes() throws Exception {
    // TestLogger logger = new TestLogger(null);
    // HubMavenReporter buildWrapper = new HubMavenReporter("Invalid", false, null, null, null, null);
    // buildWrapper.getScopesAsList(logger);
    // String output = logger.getOutputString();
    // assertTrue(output, output.contains("The user has provided an unknown scope :"));
    //
    // logger.resetAllOutput();
    //
    // buildWrapper = new HubMavenReporter(null, false, null, null, null, null);
    // buildWrapper.getScopesAsList(logger);
    // output = logger.getOutputString();
    // assertTrue(output, output.contains("Cannot get Scopes from an empty String"));
    // }
    //
    // @Test
    // public void testGetScopesAsList() throws Exception {
    // HubMavenReporter buildWrapper = new HubMavenReporter("Compile, Test", false, null, null, null, null);
    // List<String> scopeList = buildWrapper.getScopesAsList(null);
    //
    // assertTrue(scopeList.contains("COMPILE"));
    // assertTrue(scopeList.contains("TEST"));
    //
    // }

}
