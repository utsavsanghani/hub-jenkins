package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

public class RemoteReadBuildInfo implements Callable<String, IOException> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final File buildInfoFile;

    public RemoteReadBuildInfo(File buildInfoFile) {
        this.buildInfoFile = buildInfoFile;
    }

    @Override
    public String call() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(buildInfoFile));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(RemoteReadBuildInfo.class));
    }
}
