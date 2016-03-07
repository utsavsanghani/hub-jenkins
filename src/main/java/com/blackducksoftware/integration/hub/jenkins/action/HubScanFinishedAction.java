package com.blackducksoftware.integration.hub.jenkins.action;

import hudson.model.Action;

public class HubScanFinishedAction implements Action {

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Temp Action to verify the Hub scan ran before the failure conditions";
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
