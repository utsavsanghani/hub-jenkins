package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.IOException;
import java.net.InetAddress;

public class GetHostName implements Callable<String, IOException> {
    private static final long serialVersionUID = 3459269768733083577L;

    public String call() throws IOException {
        return InetAddress.getLocalHost().getHostName();
    }
}
