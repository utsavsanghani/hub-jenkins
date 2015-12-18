package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.jenkins.scan.JenkinsScanExecutor;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;

public class RemoteContinuousFileReader implements Callable<Void, IOException> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final File fileToRead;

    private final IntLogger logger;

    private StringBuilder outputBuilder = new StringBuilder();

    public RemoteContinuousFileReader(File fileToRead, IntLogger logger) {
        this.fileToRead = fileToRead;
        this.logger = logger;
    }

    public String getOutputString() {
        return outputBuilder.toString();
    }

    public Boolean hasOutput() {
        return outputBuilder.length() > 0;
    }

    @Override
    public Void call() throws IOException {
        BufferedReader buffReader = null;
        try {
            buffReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToRead)));
            String line = null;
            while (true) {
                line = buffReader.readLine();
                if (line == null) {
                    Thread.sleep(JenkinsScanExecutor.THREAD_SLEEP);
                } else {
                    // Only interested in the error logs and the warning logs
                    if (line.contains("Exception")) {
                        outputBuilder.append(line + System.getProperty("line.separator"));
                        logger.error(line);
                    } else if (line.contains("ERROR:")) {
                        outputBuilder.append(line + System.getProperty("line.separator"));
                        logger.error(line);
                    } else if (line.contains("WARN:")) {
                        outputBuilder.append(line + System.getProperty("line.separator"));
                        logger.warn(line);
                    } else if (line.contains("INFO:")) {
                        outputBuilder.append(line + System.getProperty("line.separator"));
                        logger.info(line);
                    } else if (line.contains("Finished in")) {
                        outputBuilder.append(line + System.getProperty("line.separator"));
                        logger.info(line);
                    }
                }
            }
        } catch (IOException ioe) {
            logger.error("Could not read the CLI output file.", ioe);
        } catch (InterruptedException e) {
            // Thread was interrupted
        } finally {
            try {
                buffReader.close();
            } catch (IOException ioe1) {
                // Leave It
            }
        }
        return null;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(RemoteContinuousFileReader.class));
    }
}
