package com.blackducksoftware.integration.hub.jenkins.cli;

import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;

public class DummyToolInstallation extends ToolInstallation {

    public DummyToolInstallation() {
        super("Dummy Tool Name", null, null);
    }

    @Override
    public ToolDescriptor<?> getDescriptor() {

        return new DummyToolInstallationDescriptor();
    }

    public static class DummyToolInstallationDescriptor extends ToolDescriptor<DummyToolInstallation> {

        public DummyToolInstallationDescriptor() {
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getId() {
            return "Dummy_Tool_Installation_Descriptor";
        }
    }
}
