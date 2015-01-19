package com.blackducksoftware.integration.hub.jenkins;

import hudson.model.AbstractDescribableImpl;

import org.kohsuke.stapler.DataBoundConstructor;

public class ScanJobs extends AbstractDescribableImpl<ScanJobs> {
    private final String scanTarget;

    @DataBoundConstructor
    public ScanJobs(String scanTarget) {
        this.scanTarget = scanTarget;
    }

    public String getScanTarget() {
        return scanTarget;
    }

    @Override
    public ScanJobsDescriptor getDescriptor() {
        return (ScanJobsDescriptor) super.getDescriptor();
    }

}
