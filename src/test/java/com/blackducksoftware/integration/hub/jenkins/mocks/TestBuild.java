package com.blackducksoftware.integration.hub.jenkins.mocks;

import hudson.model.Action;
import hudson.model.Result;
import hudson.model.AbstractBuild;

import java.io.IOException;

import com.blackducksoftware.integration.hub.jenkins.action.BomUpToDateAction;
import com.blackducksoftware.integration.hub.jenkins.action.HubScanFinishedAction;

public class TestBuild extends AbstractBuild<TestProject, TestBuild> {

	private Result result;

	private HubScanFinishedAction scanFinishedAction;

	private BomUpToDateAction bomUpdatedAction;

	public TestBuild(final TestProject project) throws IOException {
		super(project);
	}

	@Override
	public Result getResult() {
		return result;
	}

	@Override
	public void setResult(final Result result) {
		this.result = result;
	}

	public HubScanFinishedAction getHubScanFinishedAction() {
		return scanFinishedAction;
	}

	public void setScanFinishedAction(final HubScanFinishedAction action) {
		this.scanFinishedAction = action;
	}

	public void setBomUpdatedAction(final BomUpToDateAction action) {
		this.bomUpdatedAction = action;
	}

	@Override
	public Action getAction(final Class c) {
		if (c == HubScanFinishedAction.class) {
			return scanFinishedAction;
		}
		if (c == BomUpToDateAction.class) {
			return bomUpdatedAction;
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
