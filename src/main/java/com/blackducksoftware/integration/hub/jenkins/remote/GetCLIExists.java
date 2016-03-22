package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.File;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.cli.CLIInstaller;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;

public class GetCLIExists implements Callable<Boolean, Exception> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final HubJenkinsLogger logger;

    private final String directoryToInstallTo;

    public GetCLIExists(HubJenkinsLogger logger, String directoryToInstallTo) {
        this.logger = logger;
        this.directoryToInstallTo = directoryToInstallTo;
    }

    @Override
    public Boolean call() throws Exception {
        CLIInstaller installer = new CLIInstaller(new File(directoryToInstallTo));
        return installer.getCLIExists(logger);
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(GetCLIExists.class));
    }
}
