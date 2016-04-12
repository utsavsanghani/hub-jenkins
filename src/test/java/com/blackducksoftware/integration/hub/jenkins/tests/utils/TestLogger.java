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

import hudson.model.BuildListener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.logging.LogLevel;

public class TestLogger extends HubJenkinsLogger {

    public TestLogger(BuildListener jenkinsLogger) {
        super(jenkinsLogger);
    }

    private ArrayList<String> outputList = new ArrayList<String>();

    private ArrayList<Throwable> errorList = new ArrayList<Throwable>();

    public ArrayList<String> getOutputList() {
        return outputList;
    }

    public ArrayList<Throwable> getErrorList() {
        return errorList;
    }

    public void resetOutputList() {
        outputList.clear();
    }

    public void resetErrorList() {
        errorList.clear();
    }

    public void resetAllOutput() {
        resetOutputList();
        resetErrorList();
    }

    public String getOutputString() {
        if (outputList != null && !outputList.isEmpty()) {

            StringBuilder sb = new StringBuilder();
            for (String string : outputList) {
                sb.append(string);
                sb.append('\n');

            }
            return sb.toString();
        }
        return "";
    }

    public String getErrorOutputString() {
        if (errorList != null && !errorList.isEmpty()) {

            StringBuilder sb = new StringBuilder();
            for (Throwable e : errorList) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                sb.append(sw.toString());
                sb.append('\n');
            }
            return sb.toString();
        }
        return "";
    }

    @Override
    public void debug(String txt) {
        outputList.add(txt);

    }

    @Override
    public void debug(String txt, Throwable e) {
        outputList.add(txt);
        errorList.add(e);
    }

    @Override
    public void error(Throwable e) {
        errorList.add(e);
    }

    @Override
    public void error(String txt) {
        outputList.add(txt);

    }

    @Override
    public void error(String txt, Throwable e) {
        outputList.add(txt);
        errorList.add(e);
    }

    @Override
    public void info(String txt) {
        outputList.add(txt);

    }

    @Override
    public void trace(String txt) {
        outputList.add(txt);

    }

    @Override
    public void trace(String txt, Throwable e) {
        outputList.add(txt);
        errorList.add(e);
    }

    @Override
    public void warn(String txt) {
        outputList.add(txt);

    }

    @Override
    public void setLogLevel(LogLevel level) {

    }

    @Override
    public LogLevel getLogLevel() {
        return null;

    }

}
