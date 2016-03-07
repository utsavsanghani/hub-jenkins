package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.IOException;
import java.net.InetAddress;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

public class GetHostName implements Callable<String, IOException> {
    private static final long serialVersionUID = 3459269768733083577L;

    @Override
    public String call() throws IOException {
        return InetAddress.getLocalHost().getHostName();
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(GetHostName.class));
    }
}
