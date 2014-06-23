package com.blackducksoftware.integration.hub.jenkins;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import hudson.tools.ToolDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import org.codehaus.plexus.util.StringUtils;
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

        Result result = build.getResult();
        if (result.equals(Result.SUCCESS)) {
            try {
                listener.getLogger().println("Starting Black Duck iScans...");

                IScanInstallation[] iScanTools = null;

                ToolDescriptor<IScanInstallation> iScanDescriptor = (ToolDescriptor<IScanInstallation>) build.getDescriptorByName(IScanInstallation.class
                        .getSimpleName());

                iScanTools = iScanDescriptor.getInstallations();
                // FIXME do this in the configuration of the job, have the User choose which one to use
                // Use the first installation for now

                if (iScanTools[0] == null) {
                    throw new IScanToolMissingException("Could not find an iScan Installation to use.");
                }
                IScanInstallation iScan = iScanTools[0];
                File iScanScript = new File(iScan.getHome() + "/bin/scan.cli.sh");

                if (!iScanScript.exists()) {
                    listener.getLogger().println("[ERROR] : Script doesn't exist : " + iScanScript.getCanonicalPath());
                    throw new IScanToolMissingException("Could not find the script file to execute.");
                } else {
                    listener.getLogger().println("[DEBUG] : Script does exist : " + iScanScript.getCanonicalPath());
                }

                EnvVars envVars = new EnvVars();
                envVars = build.getEnvironment(listener);
                String javaHome = null;
                javaHome = build.getProject().getJDK().getHome();
                if (StringUtils.isEmpty(javaHome)) {
                    // In case the user did not select a java installation, set to the environment variable JAVA_HOME
                    javaHome = envVars.get("JAVA_HOME");
                }

                for (IScanJobs scanJob : scans) {
                    File target = new File(scanJob.getScanTarget());
                    if (!target.exists()) {
                        throw new IOException("Scan target could not be found : " + scanJob.getScanTarget());
                    } else {
                        listener.getLogger().println("[DEBUG] : Target does exist : " + target.getCanonicalPath());
                    }

                    ProcessBuilder pb = new ProcessBuilder(iScanScript.getCanonicalPath(),
                            "--host", "\"" + getDescriptor().getServerUrl().substring(7) + "\""
                            , "--username", "\"" + getDescriptor().getHubServerInfo().getUsername() + "\""
                            , "--password", "\"" + getDescriptor().getHubServerInfo().getPassword() + "\""
                            , target.getCanonicalPath());

                    listener.getLogger().println("[DEBUG] : THIS IS THE JAVA HOME TO BE USED : " + javaHome);
                    pb.environment().put("JAVA_HOME", javaHome);

                    for (String cmd : pb.command()) {
                        listener.getLogger().println(cmd);
                    }

                    // This is picking up the wrong java installation for some reason
                    Process p = pb.start();
                    String line;

                    BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    while ((line = error.readLine()) != null) {
                        listener.getLogger().println(line);
                    }
                    error.close();

                    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while ((line = input.readLine()) != null) {
                        listener.getLogger().println(line);
                    }

                    input.close();

                    OutputStream outputStream = p.getOutputStream();
                    PrintStream printStream = new PrintStream(outputStream);
                    printStream.println();
                    printStream.flush();
                    printStream.close();
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
