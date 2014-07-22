package com.blackducksoftware.integration.hub.jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

public class IScanJobs {
    private final String scanTarget;

    public String getScanTarget() {
        return scanTarget;
    }

    @DataBoundConstructor
    public IScanJobs(String scanTarget) {
        this.scanTarget = scanTarget;
    }
}