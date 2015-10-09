package com.blackducksoftware.integration.hub.jenkins.maven;

import hudson.FilePath;
import hudson.maven.MavenBuildProxy.BuildCallable;
import hudson.maven.MavenBuild;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;

import com.blackducksoftware.integration.build.BuildArtifact;
import com.blackducksoftware.integration.build.BuildDependency;
import com.blackducksoftware.integration.build.BuildInfo;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;

public class HubBuildCallable implements BuildCallable<Void, IOException> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final HubJenkinsLogger buildLogger;

    private final BuildArtifact bArtifact;

    private final Set<BuildDependency> buildDependencies;

    public HubBuildCallable(HubJenkinsLogger buildLogger, BuildArtifact bArtifact, Set<BuildDependency> buildDependencies) {
        this.buildLogger = buildLogger;
        this.bArtifact = bArtifact;
        this.buildDependencies = buildDependencies;
    }

    @Override
    public Void call(MavenBuild build) throws IOException, InterruptedException {
        buildLogger.debug("reportGenerated().asynch-call()");
        AbstractBuild<?, ?> rootBuild = build.getRootBuild();
        if (rootBuild == null) {
            // Single module was built
            rootBuild = build;
            buildLogger.debug("buildId: " + build.getId());
        } else {
            buildLogger.debug("buildId: " + build.getId() + " -- parent: " + build.getRootBuild().getId());
        }

        // if (build.getId().equals(rootBuild.getId())) {
        BuildInfoAction biAction = rootBuild.getAction(BuildInfoAction.class);
        BuildInfo buildInfo = null;
        if (biAction == null) {
            biAction = new BuildInfoAction();
            rootBuild.addAction(biAction);
            buildInfo = new BuildInfo();
            FilePath workspace = rootBuild.getWorkspace();
            if (workspace == null) {
                buildLogger.info("Workspace: null" + " @" + InetAddress.getLocalHost().getHostName());
            } else {
                buildLogger.info("Workspace: " + workspace.getRemote() + " @" + InetAddress.getLocalHost().getHostName());
            }
            biAction.setBuildInfo(buildInfo);
        } else {
            buildInfo = biAction.getBuildInfo();
        }
        buildInfo.setBuildId(rootBuild.getId());
        buildInfo.setDependencies(buildDependencies);
        buildInfo.addArtifact(bArtifact);
        return null;
    }
}
