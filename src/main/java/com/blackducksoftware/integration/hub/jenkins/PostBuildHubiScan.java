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

import com.blackducksoftware.integration.hub.jenkins.exceptions.HubConfigurationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.IScanToolMissingException;

public class PostBuildHubiScan extends Recorder {

    private IScanJobs[] scans;

    private String iScanName;

    private String workingDirectory;

    private String javaHome;

    @DataBoundConstructor
    public PostBuildHubiScan(IScanJobs[] scans, String iScanName) {
        this.scans = scans;
        this.iScanName = iScanName;
    }

    public IScanJobs[] getScans() {
        return scans;
    }

    public String getiScanName() {
        return iScanName;
    }

    public void setiScanName(String iScanName) {
        this.iScanName = iScanName;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    // http://javadoc.jenkins-ci.org/hudson/tasks/Recorder.html
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public PostBuildScanDescriptor getDescriptor() {
        return (PostBuildScanDescriptor) super.getDescriptor();
    }

    /**
     * Overrides the Recorder perform method. This is the method that gets called by Jenkins to run as a Post Build
     * Action
     * 
     * @param build
     *            AbstractBuild
     * @param launcher
     *            Launcher
     * @param listener
     *            BuildListener
     * 
     * @throws IOException
     * @throws InterruptedException
     */
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
                if (validateConfiguration(iScanTools, getScans())) {
                    // This set the base of the scan Target, DO NOT remove this or the user will be able to specify any
                    // file
                    // even outside of the Jenkins directories
                    setWorkingDirectory(build.getWorkspace().getRemote() + "/"); // This should work on master and
                                                                                 // slaves

                    setJavaHome(build, listener);

                    File iScanScript = getIScanScript(iScanTools, listener);

                    for (IScanJobs scanJob : getScans()) {
                        runScan(listener, build, scanJob, iScanScript);
                    }
                }

            } catch (IScanToolMissingException e) {
                build.setResult(Result.UNSTABLE);
                // have to rethrow exception as IOException or InterruptedException
                throw new IOException(e);
            } catch (HubConfigurationException e) {
                build.setResult(Result.UNSTABLE);
                // have to rethrow exception as IOException or InterruptedException
                throw new IOException(e);
            }
        } else {
            listener.getLogger().println("Build was not successful. Will not run Black Duck iScans.");
        }

        return true;
    }

    /**
     * Validates that the target of the scanJob exists, creates a ProcessBuilder to run the shellscript and passes in
     * the necessarry arguments, sets the JAVA_HOME of the Process Builder to the one that the User chose, starts the
     * process and prints out all stderr and stdout to the Console Output.
     * 
     * @param listener
     *            BuildListener
     * @param build
     *            AbstractBuild
     * @param scanJob
     *            IScanJobs
     * @param iScanScript
     *            File
     * 
     * @throws IOException
     */
    public void runScan(BuildListener listener, AbstractBuild build, IScanJobs scanJob, File iScanScript) throws IOException {
        // This starts the filepath with the workspace, so only targets in the workspace should be
        // accessible
        File target = new File(getWorkingDirectory() + scanJob.getScanTarget());
        if (!target.exists()) {
            build.setResult(Result.UNSTABLE);
            throw new IOException("Scan target could not be found : " + scanJob.getScanTarget());
        } else {
            listener.getLogger().println("[DEBUG] : Target does exist : " + target.getCanonicalPath());
        }
        // Use a substring of the host url because the http:// is not currently needed in the definition.
        ProcessBuilder pb = new ProcessBuilder(iScanScript.getCanonicalPath(),
                "--host", getDescriptor().getServerUrl().substring(7)
                , "--username", getDescriptor().getHubServerInfo().getUsername()
                , "--password", getDescriptor().getHubServerInfo().getPassword()
                , target.getCanonicalPath());
        // target is in quotations in case there is a space in the path

        listener.getLogger().println("[DEBUG] : Using this java installation : " + getJavaHome());
        pb.environment().put("JAVA_HOME", getJavaHome());

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

    public String getJavaHome() {
        return javaHome;
    }

    /**
     * Sets the Java Home that is to be used for running the Shell script
     * 
     * @param build
     * @param listener
     * @throws IOException
     * @throws InterruptedException
     */
    public void setJavaHome(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
        EnvVars envVars = new EnvVars();
        envVars = build.getEnvironment(listener);
        String javaHomeTemp = null;
        javaHomeTemp = build.getProject().getJDK().getHome();
        if (StringUtils.isEmpty(javaHomeTemp)) {
            // In case the user did not select a java installation, set to the environment variable JAVA_HOME
            javaHomeTemp = envVars.get("JAVA_HOME");
        }
        javaHome = javaHomeTemp;
    }

    /**
     * Looks through the iScanInstallations to find the one that the User chose, then looks for the scan.cli.sh in the
     * bin folder of the directory defined by the Installation.
     * It then checks that the File exists.
     * 
     * @param iScanTools
     *            IScanInstallation[] User defined iScan installations
     * @param listener
     *            BuildListener
     * @return File the scan.cli.sh
     * @throws IScanToolMissingException
     * @throws IOException
     */
    public File getIScanScript(IScanInstallation[] iScanTools, BuildListener listener) throws IScanToolMissingException, IOException {
        File iScanScript = null;
        for (IScanInstallation iScan : iScanTools) {
            if (iScan.getName().equals(getiScanName())) {
                iScanScript = new File(iScan.getHome() + "/bin/scan.cli.sh");
            }
        }
        if (!iScanScript.exists()) {
            listener.getLogger().println("[ERROR] : Script doesn't exist : " + iScanScript.getCanonicalPath());
            throw new IScanToolMissingException("Could not find the script file to execute.");
        } else {
            listener.getLogger().println("[DEBUG] : Script does exist : " + iScanScript.getCanonicalPath());
        }
        return iScanScript;
    }

    /**
     * Validates that the Plugin is configured correctly. Checks that the User has defined an iScan tool, a Hub server
     * URL, a Credential, and that there are at least one scan Target/Job defined in the Build
     * 
     * @param iScanTools
     *            IScanInstallation[] User defined iScan installations
     * @param scans
     *            IScanJobs[] the iScan jobs defined in the Job config
     * 
     * @return True if everything is configured correctly
     * 
     * @throws IScanToolMissingException
     * @throws HubConfigurationException
     */
    public boolean validateConfiguration(IScanInstallation[] iScanTools, IScanJobs[] scans) throws IScanToolMissingException, HubConfigurationException {
        if (iScanTools[0] == null) {
            throw new IScanToolMissingException("Could not find an iScan Installation to use.");
        }
        if (scans == null || scans.length == 0) {
            throw new HubConfigurationException("Could not find any targets to scan.");
        }
        if (!getDescriptor().getHubServerInfo().isPluginConfigured()) {
            // If plugin is not Configured, we try to find out what is missing.
            if (StringUtils.isEmpty(getDescriptor().getHubServerInfo().getServerUrl())) {
                throw new HubConfigurationException("Could not find any targets to scan.");
            }
            if (StringUtils.isEmpty(getDescriptor().getHubServerInfo().getCredentialsId())) {
                throw new HubConfigurationException("Could not find any targets to scan.");
            }
        }
        // No exceptions were thrown so return true
        return true;
    }
}
