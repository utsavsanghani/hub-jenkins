package com.blackducksoftware.integration.hub.jenkins.mocks;

import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;

import java.util.List;

public class TestProject extends AbstractProject<TestProject, TestBuild> implements TopLevelItem {

    private List<Publisher> publishers;

    public TestProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    protected Class getBuildClass() {
        return null;
    }

    @Override
    public boolean isFingerprintConfigured() {
        return false;
    }

    public void setPublishersList(List<Publisher> publishers) {
        this.publishers = publishers;
    }

    @Override
    public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
        return (DescribableList<Publisher, Descriptor<Publisher>>) publishers;
    }

    @Override
    public TopLevelItemDescriptor getDescriptor() {
        // TODO Auto-generated function stub
        return null;
    }

}
