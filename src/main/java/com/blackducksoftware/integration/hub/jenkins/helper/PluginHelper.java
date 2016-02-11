package com.blackducksoftware.integration.hub.jenkins.helper;

import hudson.Plugin;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;

public class PluginHelper {

    public static final String UNKNOWN_VERSION = "<unknown>";

    public static String getPluginVersion() {
        String pluginVersion = UNKNOWN_VERSION;
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            // Jenkins still active
            Plugin p = jenkins.getPlugin("hub-jenkins");
            if (p != null) {
                // plugin found
                PluginWrapper pw = p.getWrapper();
                if (pw != null) {
                    pluginVersion = pw.getVersion();
                }
            }
        }
        return pluginVersion;
    }
}
