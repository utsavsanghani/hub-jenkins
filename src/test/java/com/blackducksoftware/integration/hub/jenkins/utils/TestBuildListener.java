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
package com.blackducksoftware.integration.hub.jenkins.utils;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

import hudson.console.ConsoleNote;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Result;

public class TestBuildListener implements BuildListener {
	private static final long serialVersionUID = 6298337589492113754L;

	private PrintStream stream = null;

	public TestBuildListener(final PrintStream stream) {
		this.stream = stream;
	}

	@Override
	public PrintWriter error(final String txt) {
		if (txt != null) {
			stream.println(txt);
		}
		return null;
	}

	@Override
	public PrintStream getLogger() {
		return stream;
	}

	@Override
	public void annotate(final ConsoleNote ann) throws IOException {
	}

	@Override
	public void hyperlink(final String url, final String text) throws IOException {
	}

	@Override
	public PrintWriter error(final String format, final Object... args) {
		stream.println(String.format(format, args));
		return null;
	}

	@Override
	public PrintWriter fatalError(final String msg) {
		if (msg != null) {
			stream.println(msg);
		}
		return null;
	}

	@Override
	public PrintWriter fatalError(final String format, final Object... args) {
		stream.println(String.format(format, args));
		return null;
	}

	@Override
	public void started(final List<Cause> causes) {
	}

	@Override
	public void finished(final Result result) {
	}

}
