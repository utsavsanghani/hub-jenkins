package com.blackducksoftware.integration.hub.jenkins.cli;

import static hudson.FilePath.TarCompression.GZIP;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallation;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;

public class HubScanInstaller extends ToolInstaller {

    /**
     * URL of a ZIP file which should be downloaded in case the tool is missing.
     */

    public HubScanInstaller() {
        // Labels restrict which node the installer applies to, we do not want any restrictions for the auto install so
        // we pass null to super

        super(null);
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath dir = preferredLocation(tool, node);
        HubJenkinsLogger logger = new HubJenkinsLogger(log);

        String cliDownloadUrl = getCLIDownloadUrl(node, logger);
        if (StringUtils.isNotBlank(cliDownloadUrl)) {
            customInstall(dir, new URL(cliDownloadUrl), log, "Unpacking " + cliDownloadUrl + " to " + dir + " on " + node.getDisplayName());
        } else {
            logger.error("Could not find the correct Hub CLI download URL.");
        }

        return dir;
    }

    private String getCLIDownloadUrl(Node node, HubJenkinsLogger logger) throws IOException, InterruptedException {
        try {
            HubServerInfo serverInfo = HubServerInfoSingleton.getInstance().getServerInfo();
            HubSupportHelper hubSupport = new HubSupportHelper();
            HubIntRestService service;
            service = BuildHelper.getRestService(logger, serverInfo.getServerUrl(),
                    serverInfo.getUsername(),
                    serverInfo.getPassword(),
                    serverInfo.getTimeout());

            hubSupport.checkHubSupport(service, logger);
            VirtualChannel channel = node.getChannel();
            if (channel.call(new GetIsOsMac()) && hubSupport.isJreProvidedSupport()) {
                return HubSupportHelper.getOSXCLIWrapperLink(serverInfo.getServerUrl());
            } else if (channel.call(new GetIsOsWindows()) && hubSupport.isJreProvidedSupport()) {
                return HubSupportHelper.getWindowsCLIWrapperLink(serverInfo.getServerUrl());
            } else {
                return HubSupportHelper.getLinuxCLIWrapperLink(serverInfo.getServerUrl());
            }
        } catch (BDJenkinsHubPluginException e) {
            logger.error(e.getMessage(), e);
        } catch (HubIntegrationException e) {
            logger.error(e.getMessage(), e);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(), e);
        } catch (BDRestException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     *
     *
     * @param archive
     * @param listener
     * @param message
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean customInstall(FilePath directory, URL archive, TaskListener listener, String message) throws IOException, InterruptedException {
        try {
            FilePath timestamp = directory.child(".timestamp");
            URLConnection con;
            try {
                con = ProxyConfiguration.open(archive);
                if (timestamp.exists()) {
                    con.setIfModifiedSince(timestamp.lastModified());
                }
                con.connect();
            } catch (IOException x) {
                if (directory.exists()) {
                    // Cannot connect now, so assume whatever was last unpacked is still OK.
                    if (listener != null) {
                        listener.getLogger().println("Skipping installation of " + archive + " to " + directory.getRemote() + ": " + x);
                    }
                    return false;
                } else {
                    throw x;
                }
            }

            if (con instanceof HttpURLConnection
                    && ((HttpURLConnection) con).getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return false;
            }

            long sourceTimestamp = con.getLastModified();

            if (directory.exists()) {
                if (timestamp.exists() && sourceTimestamp == timestamp.lastModified())
                {
                    return false; // already up to date
                }
                directory.deleteContents();
            } else {
                directory.mkdirs();
            }

            if (listener != null) {
                listener.getLogger().println(message);
            }

            // for HTTP downloads, enable automatic retry for added resilience
            InputStream in = archive.getProtocol().startsWith("http") ? ProxyConfiguration.getInputStream(archive) : con.getInputStream();
            CountingInputStream cis = new CountingInputStream(in);

            try {
                if (archive.toExternalForm().endsWith(".zip")) {
                    directory.unzipFrom(cis);
                } else {
                    directory.untarFrom(cis, GZIP);
                }
            } catch (IOException e) {
                throw new IOException(String.format("Failed to unpack %s (%d bytes read of total %d)",
                        archive, cis.getByteCount(), con.getContentLength()), e);
            }
            timestamp.touch(sourceTimestamp);
            return true;
        } catch (IOException e) {
            throw new IOException("Failed to install " + archive + " to " + directory.getRemote(), e);
        }
    }

    class GetIsOsMac implements Callable<Boolean, IOException> {
        private static final long serialVersionUID = 3459269768733083577L;

        public GetIsOsMac() {
        }

        @Override
        public Boolean call() throws IOException {
            return SystemUtils.IS_OS_MAC_OSX;
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
            checker.check(this, new Role(GetIsOsMac.class));
        }
    }

    class GetIsOsWindows implements Callable<Boolean, IOException> {
        private static final long serialVersionUID = 3459269768733083577L;

        public GetIsOsWindows() {
        }

        @Override
        public Boolean call() throws IOException {
            return SystemUtils.IS_OS_WINDOWS;
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
            checker.check(this, new Role(GetIsOsWindows.class));
        }
    }

}
