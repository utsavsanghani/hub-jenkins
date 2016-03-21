package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.File;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.cli.CLIInstaller;

public class GetCLIExists implements Callable<Boolean, Exception> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final File directoryToInstallTo;

    private StoredLogger logger;

    public GetCLIExists(File directoryToInstallTo) {
        this.directoryToInstallTo = directoryToInstallTo;
        logger = new StoredLogger();
    }

    public File getDirectoryToInstallTo() {
        return directoryToInstallTo;
    }

    public String getOutput() {
        return logger.getOutputString();
    }

    @Override
    public Boolean call() throws Exception {
        CLIInstaller installer = new CLIInstaller(getDirectoryToInstallTo());
        return installer.getCLIExists(logger);
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(GetCLIExists.class));
    }
}
