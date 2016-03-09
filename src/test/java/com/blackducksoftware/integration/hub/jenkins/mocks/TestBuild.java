package com.blackducksoftware.integration.hub.jenkins.mocks;

import hudson.model.Action;
import hudson.model.Result;
import hudson.model.AbstractBuild;

import java.io.IOException;

import com.blackducksoftware.integration.hub.jenkins.action.HubScanFinishedAction;

public class TestBuild extends AbstractBuild<TestProject, TestBuild> {

    private Result result;

    private HubScanFinishedAction action;

    public TestBuild(TestProject project) throws IOException {
        super(project);
    }

    @Override
    public Result getResult() {
        return result;
    }

    @Override
    public void setResult(Result result) {
        this.result = result;
    }

    public HubScanFinishedAction getAction() {
        return action;
    }

    public void setAction(HubScanFinishedAction action) {
        this.action = action;
    }

    @Override
    public Action getAction(Class c) {
        if (c == HubScanFinishedAction.class) {
            return action;
        }
        return null;
    }

    @Override
    public TestProject getParent() {
        return project;
    }

    @Override
    public void run() {

    }

}
