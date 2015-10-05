package com.blackducksoftware.integration.hub.jenkins;

import hudson.ProxyConfiguration;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.response.DistributionEnum;
import com.blackducksoftware.integration.hub.response.PhaseEnum;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;

// This indicates to Jenkins that this is an implementation of an extension
// point.
public class BDBuildWrapperDescriptor extends BuildWrapperDescriptor implements Serializable {

    /**
     * In order to load the persisted global configuration, you have to call
     * load() in the constructor.
     */
    public BDBuildWrapperDescriptor() {
        super(BDBuildWrapper.class);
        load();
    }

    /**
     * In order to load the persisted global configuration, you have to call
     * load() in the constructor.
     */
    public BDBuildWrapperDescriptor(Class subClass) {
        super(subClass);
        load();
    }

    protected HubServerInfo getHubServerInfo() {
        PostBuildScanDescriptor descriptor = null;
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            descriptor = (PostBuildScanDescriptor) jenkins.getDescriptor(
                    PostBuildHubScan.class);
        }
        if (descriptor != null) {
            if (descriptor.getHubServerInfo() != null) {
                return descriptor.getHubServerInfo();
            }
        }
        return null;
    }

    /**
     * Fills the drop down list of possible Version phases
     *
     * @return
     */
    public ListBoxModel doFillHubVersionPhaseItems() {
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        ListBoxModel items = new ListBoxModel();
        try {
            // FIXME should get this list from the Hub server, ticket HUB-1610
            for (PhaseEnum phase : PhaseEnum.values()) {
                if (phase != PhaseEnum.UNKNOWNPHASE) {
                    items.add(phase.name(), phase.name());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
            }
        }
        return items;
    }

    /**
     * Fills the drop down list of possible Version distribution types
     *
     * @return
     */
    public ListBoxModel doFillHubVersionDistItems() {
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        ListBoxModel items = new ListBoxModel();
        try {
            // FIXME should get this list from the Hub server, ticket HUB-1610
            for (DistributionEnum distribution : DistributionEnum.values()) {
                if (distribution != DistributionEnum.UNKNOWNDISTRIBUTION) {
                    items.add(distribution.name(), distribution.name());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
            }
        }
        return items;
    }

    public HubIntRestService getRestService(String serverUrl, String username, String password, IntLogger logger) throws BDJenkinsHubPluginException,
            HubIntegrationException, URISyntaxException,
            MalformedURLException {
        HubIntRestService service = new HubIntRestService(serverUrl);
        service.setLogger(logger);
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
        service.setCookies(username,
                password);
        return service;
    }

    /**
     * Performs on-the-fly validation of the form field 'userScopesToInclude'.
     *
     * @param value
     *            This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckUserScopesToInclude(@QueryParameter String value)
            throws IOException, ServletException {
        // if (this instanceof MavenBuildWrapper.DescriptorImpl) {
        // if (value.length() == 0) {
        // return FormValidation.error(Messages
        // .CodeCenterBuildWrapper_getPleaseIncludeAScope());
        // }
        // try {
        // Scope.getScopeListFromString(value);
        // } catch (BDCIScopeException e) {
        // String scope = e.getMessage().substring(e.getMessage().indexOf(":") + 1).trim();
        // return FormValidation.error(Messages.CodeCenterBuildWrapper_getIncludedInvalidScope_0_(scope));
        // }
        // } else if (this instanceof CodeCenterGradleBuildWrapper.DescriptorImpl) {
        // if (value.length() == 0) {
        // return FormValidation.error(Messages
        // .CodeCenterBuildWrapper_getPleaseIncludeAConfiguration());
        // }
        // }
        return FormValidation.ok();
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> aClass) {
        // Indicates that this builder can be used with all kinds of project
        // types
        return aClass.getClass().isAssignableFrom(FreeStyleProject.class);
        // || aClass.getClass().isAssignableFrom(MavenModuleSet.class);
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    public String getPluginVersion() {
        return PluginHelper.getPluginVersion();
    }

}
