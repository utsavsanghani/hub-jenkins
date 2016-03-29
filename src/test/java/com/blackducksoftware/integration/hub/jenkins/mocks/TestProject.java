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
        if (publishers == null) {
            return null;
        }

        DescribableList<Publisher, Descriptor<Publisher>> temp = new DescribableList<Publisher, Descriptor<Publisher>>(null, publishers);

        return temp;
    }

    @Override
    public TopLevelItemDescriptor getDescriptor() {
        return null;
    }

}
