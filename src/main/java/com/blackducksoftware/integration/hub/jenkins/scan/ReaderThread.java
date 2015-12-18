package com.blackducksoftware.integration.hub.jenkins.scan;

import hudson.model.AbstractBuild;

import java.io.File;
import java.io.IOException;

import com.blackducksoftware.integration.hub.jenkins.remote.RemoteContinuousFileReader;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;

public class ReaderThread extends Thread {

    private final File cliOutput;

    private final IntLogger logger;

    private final AbstractBuild build;

    private RemoteContinuousFileReader fileReader;

    public ReaderThread(IntLogger logger, File cliOutput, AbstractBuild build) {
        this.logger = logger;
        this.cliOutput = cliOutput;
        this.build = build;
    }

    public String getOutputString() {
        return fileReader.getOutputString();
    }

    public Boolean hasOutput() {
        return fileReader.hasOutput();
    }

    @Override
    public void run() {
        fileReader = new RemoteContinuousFileReader(cliOutput, logger);

        try {
            build.getBuiltOn().getChannel().call(fileReader);
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }

    }
}
