package com.blackducksoftware.integration.hub.jenkins;

import hudson.model.BuildListener;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;

public class HubJenkinsLogger implements IntLogger, Serializable {

    private final BuildListener jenkinsLogger;

    private LogLevel level = LogLevel.INFO; // default is INFO

    public HubJenkinsLogger(BuildListener jenkinsLogger) {
        this.jenkinsLogger = jenkinsLogger;
    }

    public BuildListener getJenkinsListener() {
        return jenkinsLogger;
    }

    @Override
    public void setLogLevel(LogLevel level) {
        this.level = level;
    }

    @Override
    public LogLevel getLogLevel() {
        return level;
    }

    @Override
    public void debug(String txt) {
        if (LogLevel.isLoggable(level, LogLevel.DEBUG)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println(txt);
                } else {
                    System.out.println(txt);
                }
            }
        }
    }

    @Override
    public void debug(String txt, Throwable t) {
        if (LogLevel.isLoggable(level, LogLevel.DEBUG)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.error(txt);
                } else {
                    System.err.println(txt);
                }
            }
            if (t != null) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                if (jenkinsLogger != null) {
                    jenkinsLogger.error(sw.toString());
                } else {
                    System.err.println(sw.toString());
                }
            }
        }
    }

    @Override
    public void error(Throwable e) {
        if (LogLevel.isLoggable(level, LogLevel.ERROR)) {
            if (e != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                if (jenkinsLogger != null) {
                    jenkinsLogger.error(sw.toString());
                } else {
                    System.err.println(sw.toString());
                }
            }
        }
    }

    @Override
    public void error(String txt) {
        if (LogLevel.isLoggable(level, LogLevel.ERROR)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.error(txt);
                } else {
                    System.err.println(txt);
                }

            }
        }
    }

    @Override
    public void error(String txt, Throwable e) {
        if (LogLevel.isLoggable(level, LogLevel.ERROR)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.error(txt);
                } else {
                    System.err.println(txt);
                }
            }
            if (e != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                if (jenkinsLogger != null) {
                    jenkinsLogger.error(sw.toString());
                } else {
                    System.err.println(sw.toString());
                }
            }
        }
    }

    @Override
    public void info(String txt) {
        if (LogLevel.isLoggable(level, LogLevel.INFO)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println(txt);
                } else {
                    System.out.println(txt);
                }
            }
        }
    }

    @Override
    public void trace(String txt) {
        if (LogLevel.isLoggable(level, LogLevel.TRACE)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println(txt);
                } else {
                    System.out.println(txt);
                }
            }
        }
    }

    @Override
    public void trace(String txt, Throwable e) {
        if (LogLevel.isLoggable(level, LogLevel.TRACE)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println(txt);
                } else {
                    System.out.println(txt);
                }
            }
            if (e != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println(sw.toString());
                } else {
                    System.out.println(sw.toString());
                }
            }
        }
    }

    @Override
    public void warn(String txt) {
        if (LogLevel.isLoggable(level, LogLevel.WARN)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println(txt);
                } else {
                    System.out.println(txt);
                }
            }
        }
    }

}
