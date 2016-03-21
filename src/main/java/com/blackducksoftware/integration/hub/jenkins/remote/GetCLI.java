package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.File;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.cli.CLIInstaller;

public class GetCLI implements Callable<String, Exception> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final File directoryToInstallTo;

    public GetCLI(File directoryToInstallTo) {
        this.directoryToInstallTo = directoryToInstallTo;
    }

    public File getDirectoryToInstallTo() {
        return directoryToInstallTo;
    }

    @Override
    public String call() throws Exception {
        CLIInstaller installer = new CLIInstaller(getDirectoryToInstallTo());
        File file = installer.getCLI();
        if (file != null) {
            return file.getCanonicalPath();
        }
        return null;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(GetCLI.class));
    }
}
