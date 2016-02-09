package com.blackducksoftware.integration.hub.jenkins.helper;

import hudson.ProxyConfiguration;
import hudson.model.Result;
import hudson.model.AbstractBuild;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;

public class BuildHelper {

    public static boolean isSuccess(AbstractBuild<?, ?> build) {
        return build.getResult() == Result.SUCCESS;
    }

    public static boolean isOngoing(AbstractBuild<?, ?> build) {
        return build.getResult() == null;
    }

    public static HubIntRestService getRestService(String serverUrl, String username, String password, int hubTimeout) throws BDJenkinsHubPluginException,
            HubIntegrationException, URISyntaxException,
            MalformedURLException, BDRestException {

        return getRestService(null, serverUrl, username, password, hubTimeout);
    }

    public static HubIntRestService getRestService(IntLogger logger, String serverUrl, String username, String password, int hubTimeout)
            throws BDJenkinsHubPluginException,
            HubIntegrationException, URISyntaxException,
            MalformedURLException, BDRestException {
        HubIntRestService service = new HubIntRestService(serverUrl);
        service.setTimeout(hubTimeout);
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            ProxyConfiguration proxyConfig = jenkins.proxy;
            if (proxyConfig != null) {

                URL actualUrl = new URL(serverUrl);

                Proxy proxy = ProxyConfiguration.createProxy(actualUrl.getHost(), proxyConfig.name, proxyConfig.port,
                        proxyConfig.noProxyHost);

                if (proxy.address() != null) {
                    InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
                    if (StringUtils.isNotBlank(proxyAddress.getHostName()) && proxyAddress.getPort() != 0) {
                        if (StringUtils.isNotBlank(jenkins.proxy.getUserName()) && StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
                            service.setProxyProperties(proxyAddress.getHostName(), proxyAddress.getPort(), null, jenkins.proxy.getUserName(),
                                    jenkins.proxy.getPassword());
                        } else {
                            service.setProxyProperties(proxyAddress.getHostName(), proxyAddress.getPort(), null, null, null);
                        }
                        if (logger != null) {
                            logger.debug("Using proxy: '" + proxyAddress.getHostName() + "' at Port: '" + proxyAddress.getPort() + "'");
                        }
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            service.setCookies(username,
                    password);
        }

        return service;
    }

}
