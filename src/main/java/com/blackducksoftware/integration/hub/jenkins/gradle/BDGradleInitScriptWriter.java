package com.blackducksoftware.integration.hub.jenkins.gradle;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.remoting.Which;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.blackducksoftware.integration.build.BuildInfo;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.google.common.base.Charsets;
import com.google.gson.Gson;

/**
 * Class to generate a Gradle initialization script
 *
 */
public class BDGradleInitScriptWriter {
    private final AbstractBuild<?, ?> build;

    private transient IntLogger buildLogger;

    /**
     * The gradle initialization script constructor.
     *
     * @param build
     */
    public BDGradleInitScriptWriter(AbstractBuild<?, ?> build, IntLogger buildLogger) {
        this.build = build;
        this.buildLogger = buildLogger;
    }

    /**
     * Generate the init script from a template
     *
     * @return The generated script.
     */
    public String generateInitScript() throws URISyntaxException, IOException, InterruptedException {
        StringBuilder initScript = new StringBuilder();
        InputStream templateStream = getClass().getResourceAsStream("/bdInitScript.gradle");
        String templateAsString = IOUtils.toString(templateStream, Charsets.UTF_8.name());

        Node buildOn = build.getBuiltOn();
        if (buildOn == null) {
            buildLogger.error("Node build on: null");
        } else {
            FilePath remoteDependencyDir = new FilePath(buildOn.getRootPath(), "cache");
            remoteDependencyDir = new FilePath(remoteDependencyDir, "hub-jenkins");
            removeSnapshots(remoteDependencyDir);

            File gradleExtractorJar = Which.jarFile(getClass().getResource("/bdInitScript.gradle"));
            File buildInfoJar = Which.jarFile(BuildInfo.class);
            File gsonJar = Which.jarFile(Gson.class);

            copyDependenciesToRemote(remoteDependencyDir, gradleExtractorJar);
            copyDependenciesToRemote(remoteDependencyDir, buildInfoJar);
            copyDependenciesToRemote(remoteDependencyDir, gsonJar);

            String absoluteDependencyDirPath = remoteDependencyDir.getRemote();
            absoluteDependencyDirPath = absoluteDependencyDirPath.replace("\\", "/");
            String str = templateAsString.replace("${pluginLibDir}", absoluteDependencyDirPath);
            initScript.append(str);
        }
        return initScript.toString();
    }

    public void copyDependenciesToRemote(FilePath remoteDir, File localDependencyFile) throws IOException, InterruptedException {

        if (!remoteDir.exists()) {
            remoteDir.mkdirs();
        }

        FilePath remoteDependencyFilePath = new FilePath(remoteDir, localDependencyFile.getName());
        if (!remoteDependencyFilePath.exists()) {
            FilePath localDependencyFilePath = new FilePath(localDependencyFile);
            localDependencyFilePath.copyTo(remoteDependencyFilePath);
        } else {
            if (remoteDependencyFilePath.getName().contains("SNAPSHOT")) {
                // Update Snapshot versions
                remoteDependencyFilePath.delete();
                FilePath localDependencyFilePath = new FilePath(localDependencyFile);
                localDependencyFilePath.copyTo(remoteDependencyFilePath);
            }
        }

    }

    /**
     * Force update of snap shots
     */
    private void removeSnapshots(FilePath remoteDir) throws IOException, InterruptedException {
        if (remoteDir == null) {
            return;
        }
        List<FilePath> remoteFiles = remoteDir.list();
        if (remoteFiles == null || remoteFiles.size() == 0) {
            return;
        }
        for (FilePath file : remoteDir.list()) {
            // We need to do this because we released plugins which had snapshot dependencies and some of them had
            // timestamps rather than SNAPSHOT in the name
            // So now we force SNAPSHOT updates by removing them before we do anything else
            if (file.getName().contains("2014") || file.getName().contains("2015") || file.getName().contains("SNAPSHOT")) {
                file.delete();
            }
        }
    }

}
