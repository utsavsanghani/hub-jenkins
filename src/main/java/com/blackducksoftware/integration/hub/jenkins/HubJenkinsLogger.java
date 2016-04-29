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
package com.blackducksoftware.integration.hub.jenkins;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.blackducksoftware.integration.hub.logging.LogLevel;

import hudson.EnvVars;
import hudson.model.TaskListener;

public class HubJenkinsLogger implements IntLogger, Serializable {

	private static final long serialVersionUID = -685871863395350470L;

	private final TaskListener jenkinsLogger;

	private LogLevel level = LogLevel.INFO;

	public HubJenkinsLogger(final TaskListener jenkinsLogger) {
		this.jenkinsLogger = jenkinsLogger;
	}

	public TaskListener getJenkinsListener() {
		return jenkinsLogger;
	}

	public void setLogLevel(final EnvVars variables) {
		final String logLevel = variables.get("HUB_LOG_LEVEL", "INFO");
		try {
			setLogLevel(LogLevel.valueOf(logLevel.toUpperCase()));
		} catch (final IllegalArgumentException e) {
			setLogLevel(LogLevel.INFO);
		}
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
