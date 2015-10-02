package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.File;
import java.io.IOException;

public class GetSeparator implements Callable<String, IOException> {
    private static final long serialVersionUID = 3459269768733083577L;

    @Override
    public String call() throws IOException {
        return File.separator;
    }
}
