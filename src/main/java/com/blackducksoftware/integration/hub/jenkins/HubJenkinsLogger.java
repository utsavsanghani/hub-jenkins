package com.blackducksoftware.integration.hub.jenkins;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.blackducksoftware.integration.hub.logging.LogLevel;

import hudson.model.TaskListener;

public class HubJenkinsLogger implements IntLogger {

	private final TaskListener jenkinsLogger;

	private LogLevel level = LogLevel.TRACE;

	public HubJenkinsLogger(final TaskListener jenkinsLogger) {
		this.jenkinsLogger = jenkinsLogger;
	}

	public TaskListener getJenkinsListener() {
		return jenkinsLogger;
	}

	@Override
	public void setLogLevel(final LogLevel level) {
		this.level = level;
	}

	@Override
	public LogLevel getLogLevel() {
		return level;
	}

	@Override
	public void debug(final String txt) {
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
	public void debug(final String txt, final Throwable t) {
		if (LogLevel.isLoggable(level, LogLevel.DEBUG)) {
			if (txt != null) {
				if (jenkinsLogger != null) {
					jenkinsLogger.error(txt);
				} else {
					System.err.println(txt);
				}
			}
			if (t != null) {
				final StringWriter sw = new StringWriter();
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
	public void error(final Throwable e) {
		if (LogLevel.isLoggable(level, LogLevel.ERROR)) {
			if (e != null) {
				final StringWriter sw = new StringWriter();
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
	public void error(final String txt) {
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
	public void error(final String txt, final Throwable e) {
		if (LogLevel.isLoggable(level, LogLevel.ERROR)) {
			if (txt != null) {
				if (jenkinsLogger != null) {
					jenkinsLogger.error(txt);
				} else {
					System.err.println(txt);
				}
			}
			if (e != null) {
				final StringWriter sw = new StringWriter();
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
	public void info(final String txt) {
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
	public void trace(final String txt) {
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
	public void trace(final String txt, final Throwable e) {
		if (LogLevel.isLoggable(level, LogLevel.TRACE)) {
			if (txt != null) {
				if (jenkinsLogger != null) {
					jenkinsLogger.getLogger().println(txt);
				} else {
					System.out.println(txt);
				}
			}
			if (e != null) {
				final StringWriter sw = new StringWriter();
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
	public void warn(final String txt) {
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
