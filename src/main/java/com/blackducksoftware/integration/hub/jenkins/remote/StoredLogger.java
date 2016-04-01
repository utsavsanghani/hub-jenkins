package com.blackducksoftware.integration.hub.jenkins.remote;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.blackducksoftware.integration.hub.logging.LogLevel;

public class StoredLogger implements IntLogger {

    private ArrayList<String> outputList = new ArrayList<String>();

    public ArrayList<String> getOutputList() {
        return outputList;
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

    @Override
    public void debug(String txt) {
        outputList.add(txt);

    }

    @Override
    public void debug(String txt, Throwable t) {
        outputList.add(txt);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            outputList.add(sw.toString());
        }
    }

    @Override
    public void error(Throwable t) {
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            outputList.add(sw.toString());
        }
    }

    @Override
    public void error(String txt) {
        outputList.add(txt);

    }

    @Override
    public void error(String txt, Throwable t) {
        outputList.add(txt);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            outputList.add(sw.toString());
        }
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
    public void trace(String txt, Throwable t) {
        outputList.add(txt);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            outputList.add(sw.toString());
        }
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
