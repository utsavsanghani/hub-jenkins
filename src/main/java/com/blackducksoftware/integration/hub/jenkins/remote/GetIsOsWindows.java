package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

public class GetIsOsWindows implements Callable<Boolean, IOException> {
    private static final long serialVersionUID = 3459269768733083577L;

    public GetIsOsWindows() {
    }

    @Override
    public Boolean call() throws IOException {
        return SystemUtils.IS_OS_WINDOWS;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(GetIsOsWindows.class));
    }
}
