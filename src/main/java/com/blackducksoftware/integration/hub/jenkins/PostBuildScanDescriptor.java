package com.blackducksoftware.integration.hub.jenkins;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.blackducksoftware.integration.hub.jenkins.IScanInstallation.IScanDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

/**
 * Descriptor for {@link CodeCenterBuildWrapper}. Used as a singleton. The
 * class is marked as public so that it can be accessed from views.
 * 
 * <p>
 * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt> for the actual HTML fragment for
 * the configuration screen.
 */
@Extension
// This indicates to Jenkins that this is an implementation of an extension
// point.
public class PostBuildScanDescriptor extends BuildStepDescriptor<Publisher> implements Serializable {

    private static final String FORM_SERVER_URL = "serverUrl";

    private static final String FORM_TIMEOUT = "timeout";

    private static final long DEFAULT_TIMEOUT = 300;

    private static final String FORM_CREDENTIALSID = "hubCredentialsId";

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
    public void setHubServerInfo(HubServerInfo hubServerInfo) {
        this.hubServerInfo = hubServerInfo;
    }

    /**
     * In order to load the persisted global configuration, you have to call
     * load() in the constructor.
     */
    public PostBuildScanDescriptor() {
        super(PostBuildHubiScan.class);
        load();
    }

    /**
     * Fills the Credential drop down list in the global config
     * 
     * @return
     */
    public ListBoxModel doFillHubCredentialsIdItems() {
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        ListBoxModel boxModel = null;
        try {

            // Code copied from
            // https://github.com/jenkinsci/git-plugin/blob/f6d42c4e7edb102d3330af5ca66a7f5809d1a48e/src/main/java/hudson/plugins/git/UserRemoteConfig.java
            CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
            AbstractProject<?, ?> project = null; // Dont want to limit the search to a particular project for the drop
            // down menu
            boxModel = new StandardListBoxModel().withEmptySelection().withMatching(CREDENTIALS_MATCHER,
                    CredentialsProvider.lookupCredentials(StandardCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement> emptyList()));

        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
            }
        }

        return boxModel;
    }

    /**
     * Fills the iScan drop down list in the job config
     * 
     * @return
     */
    public ListBoxModel doFillIScanNameItems() {
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        ListBoxModel items = null;
        try {

            items = new ListBoxModel();
            Jenkins jenkins = Jenkins.getInstance();
            IScanDescriptor iScanDescriptor = jenkins.getDescriptorByType(IScanDescriptor.class);

            IScanInstallation[] iScanInstallations = iScanDescriptor.getInstallations();
            for (IScanInstallation iScan : iScanInstallations) {
                items.add(iScan.getName());
            }

        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
            }
        }
        return items;
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

    /**
     * Performs on-the-fly validation of the form field 'serverUrl'.
     * 
     * @param value
     *            This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckHubProjectName(@QueryParameter("hubProjectName") final String value) throws IOException, ServletException {
        if (value.length() > 0) {
            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            boolean changed = false;
            try {
                AbstractProject<?, ?> nullProject = null;
                List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                        nullProject, ACL.SYSTEM,
                        Collections.<DomainRequirement> emptyList());
                IdMatcher matcher = new IdMatcher(getHubServerInfo().getCredentialsId());
                String credentialUserName = null;
                String credentialPassword = null;
                for (StandardCredentials c : credentials) {
                    if (matcher.matches(c)) {
                        if (c instanceof UsernamePasswordCredentialsImpl) {
                            UsernamePasswordCredentialsImpl credential = (UsernamePasswordCredentialsImpl) c;
                            credentialUserName = credential.getUsername().trim();
                            credentialPassword = credential.getPassword().getPlainText().trim();
                        }
                    }
                }
                // TODO Check if any projects exist with the name provided

                // URL url = new URL(getServerUrl() + "/api/v1/projects");
                // HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                // // String userpass = credentialUserName + ":" + credentialPassword;
                // // String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
                // // connection.setRequestProperty("Authorization", basicAuth);
                // Permission perm = connection.getPermission();
                // // Object projectList = connection.getContent();
                //
                // connection.disconnect();
                //
                // return FormValidation.error(perm.getClass().getSimpleName());

            } catch (Exception e) {
                if (e.getCause() != null && e.getCause().getCause() != null) {
                    e.printStackTrace();
                    return FormValidation.error(e.getCause().getCause().toString());
                } else if (e.getCause() != null) {
                    return FormValidation.error(e.getCause().toString());
                } else {
                    return FormValidation.error(e.toString());
                }

            } finally {
                if (changed) {
                    Thread.currentThread().setContextClassLoader(
                            originalClassLoader);
                }
            }
        }
        return FormValidation.ok();
    }

    /**
     * Validates that the URL, Username, and Password are correct for connecting to the Hub Server.
     * 
     * 
     * @param serverUrl
     *            String
     * @param hubCredentialsId
     *            String
     * @return FormValidation
     * @throws ServletException
     */
    public FormValidation doTestConnection(@QueryParameter("serverUrl") final String serverUrl,
            @QueryParameter("hubCredentialsId") final String hubCredentialsId) {
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        try {
            AbstractProject<?, ?> nullProject = null;
            List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                    nullProject, ACL.SYSTEM,
                    Collections.<DomainRequirement> emptyList());
            IdMatcher matcher = new IdMatcher(hubCredentialsId);
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

            URL url = new URL(serverUrl + "/j_spring_security_check?j_username=" + credentialUserName + "&j_password=" + credentialPassword);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");

            OutputStream os = connection.getOutputStream();
            os.flush();

            int response = connection.getResponseCode();
            connection.disconnect();
            if (response == 200 || response == 204 || response == 202) {
                return FormValidation.ok(Messages.HubBuildScan_getCredentialsValidFor_0_(serverUrl));
            } else if (response == 401) {
                return FormValidation.error(Messages.HubBuildScan_getCredentialsInValidFor_0_(serverUrl));
            } else {
                return FormValidation.error(Messages.HubBuildScan_getErrorConnectingTo_0_(response));
            }

        } catch (Exception e) {
            if (e.getCause() != null && e.getCause().getCause() != null) {
                e.printStackTrace();
                return FormValidation.error(e.getCause().getCause().toString());
            } else if (e.getCause() != null) {
                return FormValidation.error(e.getCause().toString());
            } else {
                return FormValidation.error(e.toString());
            }

        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
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

    // TODO Do we need this validation? The default is the workspace of the job, which is always valid since the
    // workspace gets created when the build starts
    // /**
    // * Performs on-the-fly validation of the scans targets
    // *
    // * @param value
    // * This parameter receives the value that the user has typed.
    // * @return
    // * Indicates the outcome of the validation. This is sent to the browser.
    // */
    // public FormValidation doCheckScanTarget(@QueryParameter String value)
    // throws IOException, ServletException {
    // if (value.length() == 0) {
    // return FormValidation.error("Please set a target to scan");
    // }
    // return FormValidation.ok();
    // }

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

    // public String getIScanToolLocation() {
    // return (iScanInfo == null ? "" : (iScanInfo
    // .getToolLocation() == null ? "" : iScanInfo
    // .getToolLocation()));
    // }

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
