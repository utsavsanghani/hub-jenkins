package com.blackducksoftware.integration.hub.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolProperty;
import hudson.tools.ToolInstallation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class IScanInstallation extends ToolInstallation implements NodeSpecific<IScanInstallation>, EnvironmentSpecific<IScanInstallation> {

    @DataBoundConstructor
    public IScanInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    public IScanInstallation forEnvironment(EnvVars environment) {
        return new IScanInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    public IScanInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new IScanInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Override
    public IScanDescriptor getDescriptor() {
        return (IScanDescriptor) super.getDescriptor();
    }

    /**
     * Checks if the executable exists
     * 
     * @param channel
     *            VirtualChannel to find the executable on master or slave
     * 
     * @return true if executable is found, false otherwise
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean getExists(VirtualChannel channel, BuildListener listener) throws IOException, InterruptedException {
        FilePath homeFilePath = new FilePath(channel, getHome());
        // find the lib folder in the iScan directory
        listener.getLogger().println("[DEBUG] BlackDuck Scan directory: " + homeFilePath.getRemote());
        List<FilePath> files = homeFilePath.listDirectories();
        if (files != null) {
            listener.getLogger().println("[DEBUG] directories in the BlackDuck Scan directory: " + files.size());
            if (!files.isEmpty()) {
                FilePath libFolder = null;
                for (FilePath iScanDirectory : files) {
                    if ("lib".equalsIgnoreCase(iScanDirectory.getName())) {
                        libFolder = iScanDirectory;
                    }
                }
                if (libFolder == null) {
                    return false;
                }
                listener.getLogger().println("[DEBUG] BlackDuck Scan lib directory: " + libFolder.getRemote());
                FilePath[] cliFiles = libFolder.list("scan.cli*.jar");
                FilePath iScanScript = null;
                if (cliFiles == null) {
                    return false;
                } else {
                    for (FilePath file : cliFiles) {
                        listener.getLogger().println("[DEBUG] BlackDuck Scan lib file: " + file.getRemote());
                        if (file.getName().contains("scan.cli")) {
                            iScanScript = file;
                        }
                    }
                }
                return iScanScript.exists();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns the executable file of the installation
     * 
     * @param channel
     *            VirtualChannel to find the executable on master or slave
     * 
     * @return FilePath
     * @throws IOException
     * @throws InterruptedException
     */
    public FilePath getCLI(VirtualChannel channel) throws IOException, InterruptedException {
        File homeFile = new File(getHome() + "/lib");
        FilePath homeFilePath = new FilePath(channel, homeFile.getCanonicalPath());
        if (homeFilePath.exists()) {

            FilePath[] files = homeFilePath.list("scan.cli*.jar");
            FilePath iScanScript = null;
            for (FilePath file : files) {
                if (file.getName().contains("scan.cli")) {
                    iScanScript = file;
                }
            }
            return iScanScript;
        } else {
            return null;
        }
    }

    @Extension
    public static final class IScanDescriptor extends ToolDescriptor<IScanInstallation> {

        public IScanDescriptor() {
            setInstallations();
            load();
        }

        @Override
        public String getDisplayName() {
            return "BlackDuck Scan";
        }

        @Override
        public IScanInstallation newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return (IScanInstallation) super.newInstance(req, formData.getJSONObject("IScanInstallation"));
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws Descriptor.FormException {
            save();
            return super.configure(req, formData);
        }
    }
}
