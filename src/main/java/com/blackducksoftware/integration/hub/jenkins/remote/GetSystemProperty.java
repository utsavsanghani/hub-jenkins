package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.IOException;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

public class GetSystemProperty implements Callable<String, IOException> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final String property;

    public GetSystemProperty(String property) {
        this.property = property;
    }

    @Override
    public String call() throws IOException {
        return System.getProperty(property);
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(GetPathSeparator.class));
    }
}
