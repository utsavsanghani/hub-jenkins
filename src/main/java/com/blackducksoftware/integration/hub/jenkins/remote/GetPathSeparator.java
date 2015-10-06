package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

public class GetPathSeparator implements Callable<String, IOException> {
    private static final long serialVersionUID = 3459426768733083577L;

    @Override
    public String call() throws IOException {
        return File.pathSeparator;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(GetPathSeparator.class));
    }
}
