package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.File;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.cli.CLIInstaller;

public class CLIRemoteIntall implements Callable<Void, Exception> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final File directoryToInstallTo;

    private final String localHost;

    private final String hubUrl;

    private final String hubUser;

    private final String hubPassword;

    private String proxyHost;

    private Integer proxyPort;

    private String proxyUserName;

    private String proxyPassword;

    private StoredLogger logger;

    public CLIRemoteIntall(File directoryToInstallTo, String localHost, String hubUrl, String hubUser, String hubPassword) {
        this.directoryToInstallTo = directoryToInstallTo;
        this.localHost = localHost;
        this.hubUrl = hubUrl;
        this.hubUser = hubUser;
        this.hubPassword = hubPassword;
        logger = new StoredLogger();
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUserName() {
        return proxyUserName;
    }

    public void setProxyUserName(String proxyUserName) {
        this.proxyUserName = proxyUserName;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public File getDirectoryToInstallTo() {
        return directoryToInstallTo;
    }

    public String getLocalHost() {
        return localHost;
    }

    public String getHubUrl() {
        return hubUrl;
    }

    public String getHubUser() {
        return hubUser;
    }

    public String getHubPassword() {
        return hubPassword;
    }

    public String getOutput() {
        return logger.getOutputString();
    }

    @Override
    public Void call() throws Exception {
        CLIInstaller installer = new CLIInstaller(getDirectoryToInstallTo());

        HubIntRestService service = new HubIntRestService(getHubUrl());
        service.setProxyProperties(getProxyHost(), getProxyPort(), null, getProxyUserName(), getProxyPassword());
        service.setCookies(getHubUser(), getHubPassword());

        installer.performInstallation(logger, service, getLocalHost());

        return null;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(CLIRemoteIntall.class));
    }

}
