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
package com.blackducksoftware.integration.hub.jenkins.tests.utils;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

import hudson.console.ConsoleNote;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Result;

public class TestBuildListener implements BuildListener {

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
