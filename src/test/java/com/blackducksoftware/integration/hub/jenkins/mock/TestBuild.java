/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
