package com.blackducksoftware.integration.hub.jenkins.cli;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.DescribableList;

import java.io.IOException;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.remote.GetIsOsMac;
import com.blackducksoftware.integration.hub.jenkins.remote.GetIsOsWindows;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;

public class HubScanInstallation extends ToolInstallation implements NodeSpecific<HubScanInstallation>, EnvironmentSpecific<HubScanInstallation> {

    public static final String AUTO_INSTALL_TOOL_NAME = "Hub Scan Installation";

    private String url;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public HubScanInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    @Override
    public HubScanInstallation forEnvironment(EnvVars environment) {
        return new HubScanInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    @Override
    public HubScanInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new HubScanInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Override
    public HubScanInstallationDescriptor getDescriptor() {
        return (HubScanInstallationDescriptor) super.getDescriptor();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
    public boolean getExists(VirtualChannel channel, IntLogger logger) throws IOException, InterruptedException {
        FilePath cliHomeFilePath = getCliHome(channel);
        if (cliHomeFilePath == null) {
            return false;
        }
        // find the lib folder in the iScan directory
        logger.debug("BlackDuck scan directory: " + cliHomeFilePath.getRemote());
        List<FilePath> files = cliHomeFilePath.listDirectories();
        if (files != null) {
            logger.debug("directories in the BlackDuck scan directory: " + files.size());
            if (!files.isEmpty()) {
                FilePath libFolder = null;
                for (FilePath directory : files) {
                    if ("lib".equalsIgnoreCase(directory.getName())) {
                        libFolder = directory;
                        break;
                    }
                }
                if (libFolder == null) {
                    return false;
                }
                logger.debug("BlackDuck scan lib directory: " + libFolder.getRemote());
                FilePath[] cliFiles = libFolder.list("scan.cli*.jar");
                FilePath hubScanJar = null;
                if (cliFiles == null) {
                    return false;
                } else {
                    for (FilePath file : cliFiles) {
                        logger.debug("BlackDuck scan lib file: " + file.getRemote());
                        if (file.getName().contains("scan.cli")) {
                            hubScanJar = file;
                            break;
                        }
                    }
                }
                if (hubScanJar == null) {
                    return false;
                }
                return hubScanJar.exists();
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
    public FilePath getProvidedJavaHome(VirtualChannel channel, IntLogger logger) throws IOException, InterruptedException {
        FilePath cliHomeFilePath = getCliHome(channel);
        if (cliHomeFilePath == null) {
            return null;
        }
        List<FilePath> files = cliHomeFilePath.listDirectories();
        if (files != null && !files.isEmpty()) {
            FilePath jreFolder = null;
            for (FilePath directory : files) {
                if ("jre".equalsIgnoreCase(directory.getName())) {
                    jreFolder = directory;
                    break;
                }
            }
            if (jreFolder != null) {
                logger.info("Checking CLI provided jre at : " + jreFolder.getRemote());
                FilePath javaExec = null;
                if (channel.call(new GetIsOsMac())) {
                    jreFolder = new FilePath(jreFolder, "Contents");
                    jreFolder = new FilePath(jreFolder, "Home");
                    javaExec = new FilePath(jreFolder, "bin");
                } else {
                    javaExec = new FilePath(jreFolder, "bin");
                }

                if (channel.call(new GetIsOsWindows())) {
                    javaExec = new FilePath(javaExec, "java.exe");
                } else {
                    javaExec = new FilePath(javaExec, "java");
                }
                if (javaExec.exists()) {
                    return jreFolder;
                } else {
                    logger.info("The provided jre could not be found at : " + javaExec.getRemote());
                }
            }
        }
        return null;
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
        FilePath cliHomeFilePath = getCliHome(channel);
        if (cliHomeFilePath == null) {
            return null;
        }
        List<FilePath> files = cliHomeFilePath.listDirectories();
        if (files != null && !files.isEmpty()) {
            FilePath libFolder = null;
            for (FilePath directory : files) {
                if ("lib".equalsIgnoreCase(directory.getName())) {
                    libFolder = directory;
                    break;
                }
            }
            if (libFolder == null) {
                return null;
            }
            FilePath[] cliFiles = libFolder.list("scan.cli*.jar");
            FilePath cliFile = null;
            if (cliFiles == null) {
                return null;
            } else {
                for (FilePath file : cliFiles) {
                    if (file.getName().contains("scan.cli")) {
                        cliFile = file;
                        break;
                    }
                }
            }
            return cliFile;
        } else {
            return null;
        }
    }

    private FilePath getCliHome(VirtualChannel channel) throws IOException, InterruptedException {
        FilePath autoInstallHomeFilePath = new FilePath(channel, getHome());
        if (!autoInstallHomeFilePath.exists() || autoInstallHomeFilePath.list().isEmpty()) {
            return null;
        }
        FilePath cliHomeFilePath = null;

        List<FilePath> installedFiles = autoInstallHomeFilePath.list();

        if (installedFiles != null) {
            if (installedFiles.size() > 1) {
                // The cli is currently packed with an extra directory "scan.cli-windows-X.X.X-SNAPSHOT"
                for (FilePath currentFile : installedFiles) {
                    if (currentFile.getName().toLowerCase().contains("scan.cli") && !currentFile.getName().contains("windows")) {
                        cliHomeFilePath = currentFile;
                    }
                }
            } else if (installedFiles.size() == 1) {
                if (installedFiles.get(0).getName().toLowerCase().contains("scan.cli")) {
                    cliHomeFilePath = installedFiles.get(0);
                }
            }
        }
        if (cliHomeFilePath == null) {
            // This was not an auto-installed CLI, this is most likely a test,
            // or the user overrided the Tool in a node configuration
            cliHomeFilePath = autoInstallHomeFilePath;
        }
        return cliHomeFilePath;
    }

    @Extension
    public static class HubScanInstallationDescriptor extends ToolDescriptor<HubScanInstallation> {

        public HubScanInstallationDescriptor() {
            // nothing to load
        }

        @Override
        public void setInstallations(HubScanInstallation... installations) {
            // We do not allow the User to define CLI installation so we dont need to do anything here

            // We create our installation in the PostBuildScanDescriptor when the configuration is loaded and when the
            // configuration is saved
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            // We dont need to define our installer here unless we wanted it to show up in the UI
            return null;
        }

        @Override
        public DescribableList<ToolProperty<?>, ToolPropertyDescriptor> getDefaultProperties() throws IOException {
            // We return null so the User can't add an Auto-installer to the installation in the UI
            // This prevents the UI from rendering the drop-down list of installers
            return null;
        }

        @Override
        public List<ToolPropertyDescriptor> getPropertyDescriptors() {
            // We return null because this is used for the UI when the User is configuring the tool
            return null;
        }

        @Override
        public HubScanInstallation[] getInstallations() {
            HubScanInstallation scanInstallation = HubServerInfoSingleton.getInstance().getHubScanInstallation();
            HubScanInstallation[] scanInstallations = new HubScanInstallation[1];
            if (scanInstallation != null) {
                // We add the installation that we define so that the UI shows that something has been configured
                scanInstallations[0] = scanInstallation;
            }
            return scanInstallations;
        }

        @Override
        public String getDisplayName() {
            return "BlackDuck Scan";
        }

        @Override
        public HubScanInstallation newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // We return null because we dont allow the User to create new installations from the UI
            return null;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws Descriptor.FormException {
            // nothing to save, we do not persist the installation we created
            // We create our installation in the PostBuildScanDescriptor when the configuration is loaded and when the
            // configuration is saved
            return false;
        }
    }

}
