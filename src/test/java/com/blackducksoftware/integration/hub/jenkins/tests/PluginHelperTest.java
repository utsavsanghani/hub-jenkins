package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import com.blackducksoftware.integration.hub.jenkins.PluginHelper;

public class PluginHelperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @WithoutJenkins
    @Test
    public void testGetPluginVersionUnknown() {
        assertEquals(PluginHelper.UNKNOWN_VERSION, PluginHelper.getPluginVersion());
    }

    @Test
    public void testGetPluginVersion() {
        assertNotNull(PluginHelper.getPluginVersion(), PluginHelper.getPluginVersion());
    }
}
