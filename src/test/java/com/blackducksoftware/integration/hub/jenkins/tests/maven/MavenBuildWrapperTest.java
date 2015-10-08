package com.blackducksoftware.integration.hub.jenkins.tests.maven;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Publisher;

import java.io.IOException;

import jenkins.model.Jenkins;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.maven.MavenBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.jenkins.maven.MavenBuildWrapper;
import com.blackducksoftware.integration.hub.jenkins.tests.TestLogger;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class MavenBuildWrapperTest {

    // test is plugin enabled
    // test validateConfiguration

    @Rule
    public static JenkinsRule j = new JenkinsRule();

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

    public void addMavenBuildWrapperDescriptor() {
        MavenBuildWrapperDescriptor descriptor = new MavenBuildWrapperDescriptor();
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

    public MavenBuildWrapper getMavenBuildWrapper(String userScopesToInclude, boolean sameAsPostBuildScan, String hubWrapperProjectName,
            String hubWrapperVersionPhase,
            String hubWrapperVersionDist, String hubWrapperProjectVersion) {
        MavenBuildWrapper buildWrapper = new MavenBuildWrapper(userScopesToInclude, sameAsPostBuildScan, hubWrapperProjectName, hubWrapperVersionPhase,
                hubWrapperVersionDist, hubWrapperProjectVersion);
        return buildWrapper;
    }

    @Test
    public void testIsPluginEnabled() {
        addMavenBuildWrapperDescriptor();

        MavenBuildWrapper buildWrapper = getMavenBuildWrapper(null, false, null, null, null, null);
        assertFalse(buildWrapper.isPluginEnabled());

        buildWrapper = getMavenBuildWrapper("compile", false, null, null, null, null);
        assertFalse(buildWrapper.isPluginEnabled());

        buildWrapper = getMavenBuildWrapper("compile", false, "projectName", null, null, null);
        assertFalse(buildWrapper.isPluginEnabled());

        buildWrapper = getMavenBuildWrapper("compile", false, "projectName", "phase", null, null);
        assertFalse(buildWrapper.isPluginEnabled());

        buildWrapper = getMavenBuildWrapper("compile", false, "projectName", "phase", "dist", null);
        assertFalse(buildWrapper.isPluginEnabled());

        buildWrapper = getMavenBuildWrapper("compile", false, "projectName", "phase", "dist", "version");
        assertFalse(buildWrapper.isPluginEnabled());

        addHubServerInfo(new HubServerInfo());
        buildWrapper = getMavenBuildWrapper("compile", false, "projectName", "phase", "dist", "version");
        assertFalse(buildWrapper.isPluginEnabled());

        addHubServerInfo(new HubServerInfo("", null));
        buildWrapper = getMavenBuildWrapper("compile", false, "projectName", "phase", "dist", "version");
        assertFalse(buildWrapper.isPluginEnabled());

        addHubServerInfo(new HubServerInfo("testServer", null));
        buildWrapper = getMavenBuildWrapper("compile", false, "projectName", "phase", "dist", "version");
        assertFalse(buildWrapper.isPluginEnabled());

        UsernamePasswordCredentialsImpl credential = addCredentialToGlobalStore("", "");
        addHubServerInfo(new HubServerInfo("testServer", credential.getId()));
        buildWrapper = getMavenBuildWrapper("compile", false, "projectName", "phase", "dist", "version");
        assertTrue(buildWrapper.isPluginEnabled());

    }

    @Test
    public void testValidateConfigurationNotConfiguredNoGlobalConfiguration() {
        addMavenBuildWrapperDescriptor();

        TestLogger logger = new TestLogger(null);

        MavenBuildWrapper buildWrapper = getMavenBuildWrapper(null, false, null, null, null, null);
        assertFalse(buildWrapper.validateConfiguration(logger));

        String output = logger.getErrorOutputString();
        assertTrue(output, output.contains("Could not find the Hub global configuration!"));
        assertTrue(output, output.contains("No Hub project name configured!"));
        assertTrue(output, output.contains("No Hub project version configured!"));
        assertTrue(output, output.contains("No Maven scopes configured!"));
    }

}
