package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.File;
import java.io.IOException;

public class GetCanonicalPath implements Callable<String, IOException> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final File file;

    public GetCanonicalPath(File file) {
        this.file = file;
    }

    @Override
    public String call() throws IOException {
        return file.getCanonicalPath();
    }
}
