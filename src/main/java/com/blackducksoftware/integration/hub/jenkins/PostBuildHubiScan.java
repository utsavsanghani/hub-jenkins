package com.blackducksoftware.integration.hub.jenkins;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import hudson.tools.ToolDescriptor;

import java.io.File;
import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.hub.jenkins.exceptions.IScanToolMissingException;

public class PostBuildHubiScan extends Recorder {

    private IScanJobs[] scans;

    @DataBoundConstructor
    public PostBuildHubiScan(IScanJobs[] scans) {
        this.scans = scans;
    }

    public IScanJobs[] getScans() {
        return scans;
    }

    // http://javadoc.jenkins-ci.org/hudson/tasks/Recorder.html
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public PostBuildScanDescriptor getDescriptor() {
        return (PostBuildScanDescriptor) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        String buildLog = build.getLog();
        Result result = build.getResult();
        if (result.equals(Result.SUCCESS)) {
            try {
                listener.getLogger().println("Starting Black Duck iScans...");

                IScanInstallation[] iScanTools = null;

                ToolDescriptor<IScanInstallation> iScanDescriptor = (ToolDescriptor<IScanInstallation>) build.getDescriptorByName(IScanInstallation.class
                        .getSimpleName());

                iScanTools = iScanDescriptor.getInstallations();
                // TODO do this in the configuration of the job, have the User choose which one to use
                // Use the first installation for now

                if (iScanTools[0] == null) {
                    throw new IScanToolMissingException("Could not find an iScan Installation to use.");
                }
                IScanInstallation iScan = iScanTools[0];
                File iScanScript = new File(iScan.getHome() + "/bin/scan.cli.sh");

                if (!iScanScript.exists()) {
                    listener.getLogger().println("[ERROR] : " + iScanScript.getCanonicalPath());
                    throw new IScanToolMissingException("Could not find the script file to execute.");
                }

                for (IScanJobs scanJob : scans) {
                    File target = new File(scanJob.getScanTarget());
                    if (!target.exists()) {
                        throw new IOException("scan target could not be found : " + scanJob.getScanTarget());
                    }
                }

            } catch (IScanToolMissingException e) {
                // have to rethrow exception as IOException or InterruptedException
                throw new IOException(e);
            }
        } else {
            listener.getLogger().println("Build was not successful. Will not run Black Duck iScans.");
        }

        return true;

    }
}
