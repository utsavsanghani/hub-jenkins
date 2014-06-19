package com.blackducksoftware.integration.hub.jenkins;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

public class PostBuildHubiScan extends Recorder {

    private IScanJobs[] scans;

    @DataBoundConstructor
    public PostBuildHubiScan(IScanJobs[] scans) {
        this.scans = scans;
    }

    public IScanJobs[] getScans() {
        return scans;
    }

    // http://javadoc.jenkins-ci.org/hudson/tasks/Recorder.html
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link CodeCenterBuildWrapper}. Used as a singleton. The
     * class is marked as public so that it can be accessed from views.
     * 
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt> for the actual HTML fragment
     * for the configuration screen.
     */
    @Extension
    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {

        private static final String FORM_SERVER_URL = "serverUrl";

        private static final String FORM_TIMEOUT = "timeout";

        private static final long DEFAULT_TIMEOUT = 300;

        private static final String FORM_CREDENTIALSID = "credentialsId";

        private HubServerInfo hubServerInfo;

        /**
         * @return the hubServerInfo
         */
        public HubServerInfo getHubServerInfo() {
            return hubServerInfo;
        }

        /**
         * @param hubServerInfo
         *            the hubServerInfo to set
         */
        public void setCodeCenterServerInfo(HubServerInfo hubServerInfo) {
            this.hubServerInfo = hubServerInfo;
        }

        /**
         * In order to load the persisted global configuration, you have to call
         * load() in the constructor.
         */
        public DescriptorImpl() {
            super(PostBuildHubiScan.class);
            load();
        }

        public ListBoxModel doFillCredentialsIdItems() {
            // Code copied from
            // https://github.com/jenkinsci/git-plugin/blob/f6d42c4e7edb102d3330af5ca66a7f5809d1a48e/src/main/java/hudson/plugins/git/UserRemoteConfig.java
            CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
            AbstractProject<?, ?> project = null; // Dont want to limit the search to a particular project for the drop
                                                  // down menu
            return new StandardListBoxModel().withEmptySelection().withMatching(CREDENTIALS_MATCHER,
                    CredentialsProvider.lookupCredentials(StandardCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement> emptyList()));
        }

        /**
         * Performs on-the-fly validation of the form field 'serverUrl'.
         * 
         * @param value
         *            This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the
         *         browser.
         */
        public FormValidation doCheckServerUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error(Messages
                        .HubBuildScan_getPleaseSetServerUrl());
            }
            URL url;
            try {
                url = new URL(value);
                try {
                    url.toURI();
                } catch (URISyntaxException e) {
                    return FormValidation.error(Messages
                            .HubBuildScan_getNotAValidUrl());
                }
            } catch (MalformedURLException e) {
                return FormValidation.error(Messages
                        .HubBuildScan_getNotAValidUrl());
            }
            try {
                URLConnection connection = url.openConnection();
                connection.getContent();
            } catch (IOException ioe) {
                return FormValidation.warning(Messages
                        .HubBuildScan_getCanNotReachThisServer());
            } catch (RuntimeException e) {
                return FormValidation.error(Messages
                        .HubBuildScan_getNotAValidUrl());
            }
            return FormValidation.ok();
        }

        public FormValidation doTestConnection(@QueryParameter("serverUrl") final String serverUrl,
                @QueryParameter("credentialsId") final String credentialsId)
                throws ServletException {
            try {
                AbstractProject<?, ?> nullProject = null;
                List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                        nullProject, ACL.SYSTEM,
                        Collections.<DomainRequirement> emptyList());
                IdMatcher matcher = new IdMatcher(credentialsId);
                String credentialUserName = null;
                String credentialPassword = null;
                for (StandardCredentials c : credentials) {
                    if (matcher.matches(c)) {
                        if (c instanceof UsernamePasswordCredentialsImpl) {
                            UsernamePasswordCredentialsImpl credential = (UsernamePasswordCredentialsImpl) c;
                            credentialUserName = credential.getUsername();
                            credentialPassword = credential.getPassword().getPlainText();
                        }
                    }
                }
                // CodeCenterFacade ccFacade = new CodeCenterFacade(serverUrl, credentialUserName, credentialPassword);
                // Jenkins jenkins = Jenkins.getInstance();
                // if (jenkins != null && jenkins.proxy != null) {
                // if (!getMatchingNoProxyHostPatterns(serverUrl, jenkins)) {
                // if (!StringUtils.isEmpty(jenkins.proxy.name) && jenkins.proxy.port != 0) {
                // CodeCenterServerProxyV6_5_0_IntegrationX ccServerProxy = ccFacade.getServerProxy();
                // ccServerProxy.setProxyServer(jenkins.proxy.name, jenkins.proxy.port, ProxyServerType.HTTP, true);
                // }
                // }
                // }
                // ccFacade.validate();
                return FormValidation.ok(Messages.HubBuildScan_getCredentialsValidFor_0_(serverUrl));
            } catch (Exception e) {
                if (e.getCause() != null && e.getCause().getCause() != null) {
                    e.printStackTrace();
                    return FormValidation.error(e.getCause().getCause().toString());
                } else if (e.getCause() != null) {
                    return FormValidation.error(e.getCause().toString());
                } else {
                    return FormValidation.error(e.toString());
                }

            }

        }

        @Override
        public boolean isApplicable(Class aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
            // || aClass.getClass().isAssignableFrom(MavenModuleSet.class);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return Messages.HubBuildScan_getDisplayName();
        }

        /**
         * Performs on-the-fly validation of the scans targets
         * 
         * @param value
         *            This parameter receives the value that the user has typed.
         * @return
         *         Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckScanTarget(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a target to scan");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws Descriptor.FormException {
            // To persist global configuration information,
            // set that to properties and call save().

            hubServerInfo = new HubServerInfo(formData.getString(FORM_SERVER_URL), formData.getString(FORM_CREDENTIALSID),
                    formData.getLong(FORM_TIMEOUT));

            // ^Can also use req.bindJSON(this, formData);
            // (easier when there are many fields; need set* methods for this,
            // like setUseFrench)
            save();
            return super.configure(req, formData);
        }

        public String getServerUrl() {
            return (hubServerInfo == null ? "" : (hubServerInfo
                    .getServerUrl() == null ? "" : hubServerInfo
                    .getServerUrl()));
        }

        public long getTimeout() {
            return hubServerInfo == null ? getDefaultTimeout()
                    : hubServerInfo.getTimeout();
        }

        public long getDefaultTimeout() {
            return DEFAULT_TIMEOUT;
        }

        public String getCredentialsId() {
            return (hubServerInfo == null ? "" : (hubServerInfo.getCredentialsId() == null ? "" : hubServerInfo.getCredentialsId()));
        }
    }
}
