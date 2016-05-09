/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.jenkins.mock;

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
