package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.File;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.cli.CLIInstaller;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;

public class CLIRemoteInstall implements Callable<Void, Exception> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final HubJenkinsLogger logger;

    private final String directoryToInstallTo;

    private final String localHost;

    private final String hubUrl;

    private final String hubUser;

    private final String hubPassword;

    private String proxyHost;

    private int proxyPort;

    private String proxyUserName;

    private String proxyPassword;

    public CLIRemoteInstall(HubJenkinsLogger logger, String directoryToInstallTo, String localHost, String hubUrl, String hubUser, String hubPassword) {
        this.directoryToInstallTo = directoryToInstallTo;
        this.localHost = localHost;
        this.hubUrl = hubUrl;
        this.hubUser = hubUser;
        this.hubPassword = hubPassword;
        this.logger = logger;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setProxyUserName(String proxyUserName) {
        this.proxyUserName = proxyUserName;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    @Override
    public Void call() throws Exception {
        CLIInstaller installer = new CLIInstaller(new File(directoryToInstallTo));

        HubIntRestService service = new HubIntRestService(hubUrl);
        service.setLogger(logger);
        service.setProxyProperties(proxyHost, proxyPort, null, proxyUserName, proxyPassword);
        service.setCookies(hubUser, hubPassword);

        installer.performInstallation(logger, service, localHost);

        return null;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(CLIRemoteInstall.class));
    }

}
