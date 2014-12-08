package com.blackducksoftware.integration.hub.jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

public class ScanJobs {
    private final String scanTarget;

    @DataBoundConstructor
    public ScanJobs(String scanTarget) {
        this.scanTarget = scanTarget;
    }

    public String getScanTarget() {
        return scanTarget;
    }

}
