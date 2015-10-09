package com.blackducksoftware.integration.hub.jenkins.tests.gradle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import jenkins.model.Jenkins;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.gradle.GradleBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.jenkins.gradle.GradleBuildWrapper;
import com.blackducksoftware.integration.hub.jenkins.tests.TestLogger;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class GradleBuildWrapperTest {

    @Rule
    public static JenkinsRule j = new JenkinsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    public void addHubServerInfo(HubServerInfo hubServerInfo) {
        resetPublisherDescriptors();

        PostBuildScanDescriptor descriptor = new PostBuildScanDescriptor();
        descriptor.setHubServerInfo(hubServerInfo);
        j.getInstance().getDescriptorList(Publisher.class).add(descriptor);
    }

    public void resetPublisherDescriptors() {
        while (Jenkins.getInstance().getDescriptorList(Publisher.class).size() != 0) {
            Jenkins.getInstance().getDescriptorList(Publisher.class).remove(0);
        }
    }

    public void addGradleBuildWrapperDescriptor() {
        GradleBuildWrapperDescriptor descriptor = new GradleBuildWrapperDescriptor();
        j.getInstance().getDescriptorList(BuildWrapper.class).add(descriptor);
    }

    public UsernamePasswordCredentialsImpl addCredentialToGlobalStore(String username, String password) {
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null,
                username, password);
        UserFacingAction store = new UserFacingAction();
        try {
            store.getStore().addCredentials(Domain.global(), credential);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return credential;
    }

    public GradleBuildWrapper getGradleBuildWrapper(String userScopesToInclude, boolean sameAsPostBuildScan, String hubWrapperProjectName,
            String hubWrapperVersionPhase,
            String hubWrapperVersionDist, String hubWrapperProjectVersion) {
        GradleBuildWrapper buildWrapper = new GradleBuildWrapper(userScopesToInclude, sameAsPostBuildScan, hubWrapperProjectName, hubWrapperVersionPhase,
                hubWrapperVersionDist, hubWrapperProjectVersion);
        return buildWrapper;
    }

    @Test
    public void testIsPluginEnabled() {
        addGradleBuildWrapperDescriptor();

        GradleBuildWrapper buildWrapper = getGradleBuildWrapper(null, false, null, null, null, null);
        assertFalse(buildWrapper.isPluginEnabled());

        buildWrapper = getGradleBuildWrapper("compile", false, null, null, null, null);
        assertFalse(buildWrapper.isPluginEnabled());

        buildWrapper = getGradleBuildWrapper("compile", false, "projectName", null, null, null);
        assertFalse(buildWrapper.isPluginEnabled());

        buildWrapper = getGradleBuildWrapper("compile", false, "projectName", "phase", null, null);
        assertFalse(buildWrapper.isPluginEnabled());

        buildWrapper = getGradleBuildWrapper("compile", false, "projectName", "phase", "dist", null);
        assertFalse(buildWrapper.isPluginEnabled());

        buildWrapper = getGradleBuildWrapper("compile", false, "projectName", "phase", "dist", "version");
        assertFalse(buildWrapper.isPluginEnabled());

        addHubServerInfo(new HubServerInfo());
        buildWrapper = getGradleBuildWrapper("compile", false, "projectName", "phase", "dist", "version");
        assertFalse(buildWrapper.isPluginEnabled());

        addHubServerInfo(new HubServerInfo("", null));
        buildWrapper = getGradleBuildWrapper("compile", false, "projectName", "phase", "dist", "version");
        assertFalse(buildWrapper.isPluginEnabled());

        addHubServerInfo(new HubServerInfo("testServer", null));
        buildWrapper = getGradleBuildWrapper("compile", false, "projectName", "phase", "dist", "version");
        assertFalse(buildWrapper.isPluginEnabled());

        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore("", "");
        addHubServerInfo(new HubServerInfo("testServer", credential.getId()));
        buildWrapper = getGradleBuildWrapper("compile", false, "projectName", "phase", "dist", "version");
        assertTrue(buildWrapper.isPluginEnabled());

    }

    @Test
    public void testValidateConfigurationNotConfiguredNoGlobalConfiguration() {
        addGradleBuildWrapperDescriptor();

        TestLogger logger = new TestLogger(null);

        GradleBuildWrapper buildWrapper = getGradleBuildWrapper(null, false, null, null, null, null);
        assertFalse(buildWrapper.validateConfiguration(logger));

        String output = logger.getOutputString();
        assertTrue(output, output.contains("Could not find the Hub global configuration!"));
        assertTrue(output, output.contains("No Hub project name configured!"));
        assertTrue(output, output.contains("No Hub project version configured!"));
        assertTrue(output, output.contains("No Gradle configurations configured!"));
    }

    @Test
    public void testValidateConfigurationWithGlobalConfigurationEmpty() {
        addGradleBuildWrapperDescriptor();

        TestLogger logger = new TestLogger(null);

        addHubServerInfo(new HubServerInfo("", ""));

        GradleBuildWrapper buildWrapper = getGradleBuildWrapper(null, false, null, null, null, null);
        assertFalse(buildWrapper.validateConfiguration(logger));

        String output = logger.getOutputString();
        assertTrue(output, !output.contains("Could not find the Hub global configuration!"));
        assertTrue(output, output.contains("The Hub server URL is not configured!"));
        assertTrue(output, output.contains("No Hub credentials configured!"));
    }

    @Test
    public void testValidateConfigurationWithGlobalConfiguration() {
        addGradleBuildWrapperDescriptor();

        TestLogger logger = new TestLogger(null);

        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore("User", "Password");
        addHubServerInfo(new HubServerInfo("http://server.com", credential.getId()));

        GradleBuildWrapper buildWrapper = getGradleBuildWrapper(null, false, null, null, null, null);
        assertFalse(buildWrapper.validateConfiguration(logger));

        String output = logger.getOutputString();
        assertTrue(output, !output.contains("Could not find the Hub global configuration!"));
        assertTrue(output, !output.contains("The Hub server URL is not configured!"));
        assertTrue(output, !output.contains("No Hub credentials configured!"));
    }

    @Test
    public void testValidateConfiguration() {
        addGradleBuildWrapperDescriptor();

        TestLogger logger = new TestLogger(null);

        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore("User", "Password");
        addHubServerInfo(new HubServerInfo("http://server.com", credential.getId()));

        GradleBuildWrapper buildWrapper = getGradleBuildWrapper("Compile", false, "Project", null, null, "Version");
        assertTrue(buildWrapper.validateConfiguration(logger));

        String output = logger.getOutputString();
        assertTrue(output, StringUtils.isBlank(output));
    }

    @Test
    public void testHandleVariableReplacementUnknownVariable() throws Exception {
        exception.expect(BDJenkinsHubPluginException.class);
        exception.expectMessage("Variable was not properly replaced. Value : ${TEST}, Result : ${TEST}. Make sure the variable has been properly defined.");

        addGradleBuildWrapperDescriptor();

        GradleBuildWrapper buildWrapper = getGradleBuildWrapper(null, false, null, null, null, null);

        HashMap<String, String> variables = new HashMap<String, String>();

        buildWrapper.handleVariableReplacement(variables, "${TEST}");
    }

    @Test
    public void testHandleVariableReplacement() throws Exception {
        addGradleBuildWrapperDescriptor();

        GradleBuildWrapper buildWrapper = getGradleBuildWrapper(null, false, null, null, null, null);

        HashMap<String, String> variables = new HashMap<String, String>();
        variables.put("TEST", "Value");

        assertEquals("Value", buildWrapper.handleVariableReplacement(variables, "${TEST}"));
        assertEquals("Value", buildWrapper.handleVariableReplacement(variables, "$TEST"));
    }

    @Test
    public void testGetScopesAsListInvalidScopes() throws Exception {
        TestLogger logger = new TestLogger(null);
        GradleBuildWrapper buildWrapper = getGradleBuildWrapper(null, false, null, null, null, null);
        buildWrapper.getScopesAsList(logger);
        String output = logger.getOutputString();
        assertTrue(output, output.contains("Cannot get Configurations from an empty String"));
    }

    @Test
    public void testGetScopesAsList() throws Exception {
        GradleBuildWrapper buildWrapper = getGradleBuildWrapper("Compile, Test, Fake", false, null, null, null, null);
        List<String> scopeList = buildWrapper.getScopesAsList(null);

        assertTrue(scopeList.contains("COMPILE"));
        assertTrue(scopeList.contains("TEST"));
        assertTrue(scopeList.contains("FAKE"));

    }
}
