package com.blackducksoftware.integration.hub.jenkins.utils;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public class StoredPrintStream extends PrintStream {

	private final ArrayList<String> outputList = new ArrayList<String>();

	public StoredPrintStream() throws FileNotFoundException {
		super("test.log");
	}

	@Override
	public void println(final String x) {
		outputList.add(x);
	}

	public ArrayList<String> getOutputList() {
		return outputList;
	}

	public String getOutputString() {
		return StringUtils.join(outputList, ' ');
	}
}
