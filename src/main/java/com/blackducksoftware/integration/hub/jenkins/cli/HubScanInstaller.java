package com.blackducksoftware.integration.hub.jenkins.cli;

import static hudson.FilePath.TarCompression.GZIP;
import hudson.FilePath;
import hudson.Functions;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import jenkins.MasterToSlaveFileCallable;

import org.apache.commons.io.input.CountingInputStream;

public class HubScanInstaller extends ToolInstaller {

    /**
     * URL of a ZIP file which should be downloaded in case the tool is missing.
     */
    private final String url;

    public HubScanInstaller(String label, String url) {
        super(label);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath dir = preferredLocation(tool, node);
        if (customInstall(dir, new URL(url), log, "Unpacking " + url + " to " + dir + " on " + node.getDisplayName())) {
            dir.act(new SetRemotePermissions());
        }
        return dir;
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

    /**
     * Sets execute permission on all files, since unzip etc. might not do this.
     */
    private class SetRemotePermissions extends MasterToSlaveFileCallable<Void> {
        private static final long serialVersionUID = 1L;

        @Override
        public Void invoke(File d, VirtualChannel channel) throws IOException {
            if (!Functions.isWindows()) {
                process(d);
            }
            return null;
        }

        private void process(File f) {
            if (f.isFile()) {
                f.setExecutable(true, false);
            } else {
                File[] files = f.listFiles();
                if (files != null) {
                    for (File file : files) {
                        process(file);
                    }
                }
            }
        }
    }
}
