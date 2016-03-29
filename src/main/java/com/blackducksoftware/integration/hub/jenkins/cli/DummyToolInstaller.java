package com.blackducksoftware.integration.hub.jenkins.cli;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.tools.ToolInstallation;

import java.io.IOException;

public class DummyToolInstaller extends ToolInstaller {

    public DummyToolInstaller() {
        super(null);
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        return null;
    }

    @Override
    public ToolInstallerDescriptor<?> getDescriptor() {

        return new DummyToolInstallerDescriptor();
    }

    public FilePath getToolDir(ToolInstallation tool, Node node) {
        FilePath toolsDir = preferredLocation(tool, node);
        // preferredLocation will return {root}/tools/descriptorId/installationName
        // and we want to return {root}/tools
        return toolsDir.getParent().getParent();
    }

    public static class DummyToolInstallerDescriptor extends ToolInstallerDescriptor<DummyToolInstaller> {

        @Override
        public String getDisplayName() {
            return null;
        }

    }

}
