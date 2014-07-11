package com.blackducksoftware.integration.hub.jenkins;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.codehaus.plexus.util.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.CookieSetting;
import org.restlet.data.Method;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

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
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final String FORM_PORT = "hubPort";

    private HubServerInfo hubServerInfo;

    private String projectId;

    private Series<Cookie> cookies;

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

    public String getProjectId() {
        if (projectId != null) {
            return projectId;
        } else {
            // TODO When we get this api
            // String url = getServerUrl() + "/api/v1/projects/name/" + hubProjectName;
            // ClientResource resource = new ClientResource(url);
            // resource.getRequest().setCookies(cookies);
            // resource.setMethod(Method.GET);
            // resource.get();
            // int responseCode = resource.getResponse().getStatus().getCode();
            //
            // if (responseCode == 200 || responseCode == 204 || responseCode == 202) {
            //
            // Response resp = resource.getResponse();
            // get project Id from the response
            // }

            return projectId;
        }

    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
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
    public FormValidation doCheckServerUrl(@QueryParameter("serverUrl") String serverUrl, @QueryParameter("hubPort") String hubPort)
            throws IOException, ServletException {
        if (serverUrl.length() == 0) {
            return FormValidation.error(Messages
                    .HubBuildScan_getPleaseSetServerUrl());
        }
        if (hubPort.length() != 0) {
            serverUrl = serverUrl + ":" + hubPort;
        }
        URL url;
        try {
            url = new URL(serverUrl);
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
     * Performs on-the-fly validation of the form field 'hubProjectName'. Checks to see if there is already a project in
     * the Hub with this name.
     * 
     * @param hubProjectName
     *            This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckHubProjectName(@QueryParameter("hubProjectName") final String hubProjectName) throws IOException, ServletException {
        if (hubProjectName.length() > 0) {
            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            boolean changed = false;
            try {
                if (StringUtils.isEmpty(getServerUrl())) {
                    return FormValidation.error(Messages.HubBuildScan_getPleaseSetServerUrl());
                }
                if (StringUtils.isEmpty(getHubServerInfo().getCredentialsId())) {
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
                }
                String credentialUserName = null;
                String credentialPassword = null;

                UsernamePasswordCredentialsImpl credential = getCredentials(getHubServerInfo().getCredentialsId());
                credentialUserName = credential.getUsername();
                credentialPassword = credential.getPassword().getPlainText();
                setCookies(getServerUrl(), credentialUserName, credentialPassword);
                Series<Cookie> cookies = getCookies();
                setProjectId(null);
                // TODO This api call is currently unsupported, still WIP. THIS SHOULD be used INSTEAD OF the search
                // String url = getServerUrl() + "/api/v1/projects/name/" + hubProjectName;

                String url = getServerUrl() + "/api/v1/search/PROJECT?q=" + hubProjectName + "&limit=20";
                ClientResource resource = new ClientResource(url);

                resource.getRequest().setCookies(cookies);
                resource.setMethod(Method.GET);
                resource.get();
                int responseCode = resource.getResponse().getStatus().getCode();

                if (responseCode == 200 || responseCode == 204 || responseCode == 202) {
                    Response resp = resource.getResponse();
                    Reader reader = resp.getEntity().getReader();
                    BufferedReader bufReader = new BufferedReader(reader);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufReader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    byte[] mapData = sb.toString().getBytes();

                    HashMap<String, Object> responseMap = new HashMap<String, Object>();
                    // Create HashMap from the Rest response
                    ObjectMapper responseMapper = new ObjectMapper();
                    responseMap = responseMapper.readValue(mapData, HashMap.class);
                    if (responseMap.containsKey("hits") && ((ArrayList<LinkedHashMap>) responseMap.get("hits")).size() > 0) {
                        ArrayList<LinkedHashMap> projectPotentialMatches = (ArrayList<LinkedHashMap>) responseMap.get("hits");
                        StringBuilder projectMatches = new StringBuilder();
                        // More than one match found
                        if (projectPotentialMatches.size() > 1) {
                            for (LinkedHashMap project : projectPotentialMatches) {
                                LinkedHashMap projectFields = (LinkedHashMap) project.get("fields");

                                // All of the fields are ArrayLists with the value at the first position
                                if (projectMatches.length() > 0) {
                                    projectMatches.append(", " + (String) ((ArrayList) projectFields.get("name")).get(0));
                                } else {
                                    projectMatches.append((String) ((ArrayList) projectFields.get("name")).get(0));
                                }
                            }
                            // Found matches to the project name, print server Url and all the matches for this name
                            // that were found
                            return FormValidation.error(Messages.HubBuildScan_getProjectNonExistingWithMatches_0_(getServerUrl(), projectMatches.toString()));

                        } else if (projectPotentialMatches.size() == 1) {
                            // Single match was found
                            LinkedHashMap projectFields = (LinkedHashMap) projectPotentialMatches.get(0).get("fields");
                            if (((String) ((ArrayList) projectFields.get("name")).get(0)).equals(hubProjectName)) {
                                // All of the fields are ArrayLists with the value at the first position
                                setProjectId((String) ((ArrayList) projectFields.get("uuid")).get(0));
                                return FormValidation.ok(Messages.HubBuildScan_getProjectExistsIn_0_(getServerUrl()));
                            } else {
                                projectMatches.append((String) ((ArrayList) projectFields.get("name")).get(0));
                                return FormValidation
                                        .error(Messages.HubBuildScan_getProjectNonExistingWithMatches_0_(getServerUrl(), projectMatches.toString()));
                            }
                        }
                    } else {
                        return FormValidation.error(Messages.HubBuildScan_getProjectNonExistingIn_0_(getServerUrl()));
                    }
                } else {
                    return FormValidation.error(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
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
        return FormValidation.ok();
    }

    /**
     * Performs on-the-fly validation of the form field 'hubProjectRelease'. Checks to see if there is already a project
     * in the Hub with this name.
     * 
     * @param value
     *            This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckHubProjectRelease(@QueryParameter("hubProjectRelease") final String hubProjectRelease) throws IOException, ServletException {
        if (hubProjectRelease.length() > 0) {

            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            boolean changed = false;
            try {
                if (StringUtils.isEmpty(getServerUrl())) {
                    return FormValidation.error(Messages.HubBuildScan_getPleaseSetServerUrl());
                }
                if (StringUtils.isEmpty(getHubServerInfo().getCredentialsId())) {
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
                }

                String credentialUserName = null;
                String credentialPassword = null;

                UsernamePasswordCredentialsImpl credential = getCredentials(getHubServerInfo().getCredentialsId());
                credentialUserName = credential.getUsername();
                credentialPassword = credential.getPassword().getPlainText();

                setCookies(getServerUrl(), credentialUserName, credentialPassword);

                Series<Cookie> cookies = getCookies();
                String url = getServerUrl() + "/api/v1/projects/" + getProjectId() + "/releases?limit=20";
                ClientResource resource = new ClientResource(url);

                resource.getRequest().setCookies(cookies);
                resource.setMethod(Method.GET);
                resource.get();
                int responseCode = resource.getResponse().getStatus().getCode();
                StringBuilder projectReleases = new StringBuilder();
                if (responseCode == 200 || responseCode == 204 || responseCode == 202) {

                    Response resp = resource.getResponse();
                    Reader reader = resp.getEntity().getReader();
                    BufferedReader bufReader = new BufferedReader(reader);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufReader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    byte[] mapData = sb.toString().getBytes();

                    HashMap<String, Object> responseMap = new HashMap<String, Object>();
                    // Create HashMap from the Rest response
                    ObjectMapper responseMapper = new ObjectMapper();
                    responseMap = responseMapper.readValue(mapData, HashMap.class);
                    if (responseMap.containsKey("items")) {
                        ArrayList<LinkedHashMap> releaseList = (ArrayList<LinkedHashMap>) responseMap.get("items");
                        for (LinkedHashMap release : releaseList) {
                            if (((String) release.get("version")).equals(hubProjectRelease)) {
                                return FormValidation.ok(Messages.HubBuildScan_getReleaseExistsIn_0_(getProjectId()));
                            } else {
                                if (projectReleases.length() > 0) {
                                    projectReleases.append(", " + ((String) release.get("version")));
                                } else {
                                    projectReleases.append((String) release.get("version"));
                                }
                            }
                        }
                    } else {
                        // The Hub Api has changed and we received a JSON response that we did not expect
                        return FormValidation.error(Messages.HubBuildScan_getIncorrectMappingOfServerResponse());
                    }
                } else {
                    return FormValidation.error(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
                }
                return FormValidation.error(Messages.HubBuildScan_getReleaseNonExistingIn_0_(getProjectId(), projectReleases.toString()));
            } catch (Exception e) {
                if (e.getCause() != null && e.getCause().getCause() != null) {
                    if (e.getCause().getCause().toString().contains("(404) - Not Found")) {
                        e.printStackTrace();
                        return FormValidation.error(e.getCause().getCause().toString() + ", Need to provide an existing Hub Project.");
                    }
                    e.printStackTrace();
                    return FormValidation.error(e.getCause().getCause().toString());
                } else if (e.getCause() != null) {
                    if (e.getCause().toString().contains("(404) - Not Found")) {
                        return FormValidation.error(e.getCause().toString() + ", Need to provide an existing Hub Project.");
                    }
                    return FormValidation.error(e.getCause().toString());
                } else {
                    if (e.toString().contains("(404) - Not Found")) {
                        return FormValidation.error(e.toString() + ", Need to provide an existing Hub Project.");
                    }
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
            if (StringUtils.isEmpty(serverUrl)) {
                return FormValidation.error(Messages.HubBuildScan_getPleaseSetServerUrl());
            }
            if (StringUtils.isEmpty(hubCredentialsId)) {
                return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
            }

            String credentialUserName = null;
            String credentialPassword = null;

            UsernamePasswordCredentialsImpl credential = getCredentials(hubCredentialsId);
            credentialUserName = credential.getUsername();
            credentialPassword = credential.getPassword().getPlainText();

            String url = serverUrl + "/j_spring_security_check?j_username=" + credentialUserName + "&j_password=" + credentialPassword;
            ClientResource resource = new ClientResource(url);
            resource.setMethod(Method.POST);

            EmptyRepresentation rep = new EmptyRepresentation();

            resource.post(rep);

            int responseCode = resource.getResponse().getStatus().getCode();

            if (responseCode == 200 || responseCode == 204 || responseCode == 202) {
                return FormValidation.ok(Messages.HubBuildScan_getCredentialsValidFor_0_(serverUrl));
            } else if (responseCode == 401) {
                // If User is Not Authorized, 401 error, an exception should be thrown by the ClientResource
                return FormValidation.error(Messages.HubBuildScan_getCredentialsInValidFor_0_(serverUrl));
            } else {
                return FormValidation.error(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
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

    /**
     * Gets the cookie for the Authorized connection to the Hub server.
     * 
     * @param serverUrl
     *            String the Url for the Hub server
     * @param credentialUserName
     *            String the Username for the Hub server
     * @param credentialPassword
     *            String the Password for the Hub server
     */
    private void setCookies(String serverUrl, String credentialUserName, String credentialPassword) {
        String url = serverUrl + "/j_spring_security_check?j_username=" + credentialUserName + "&j_password=" + credentialPassword;
        ClientResource resource = new ClientResource(url);
        resource.setMethod(Method.POST);

        EmptyRepresentation rep = new EmptyRepresentation();

        resource.post(rep);
        Series<CookieSetting> cookieSettings = resource.getResponse().getCookieSettings();
        Series<Cookie> requestCookies = resource.getRequest().getCookies();
        for (CookieSetting ck : cookieSettings) {
            Cookie cookie = new Cookie();
            cookie.setName(ck.getName());
            cookie.setDomain(ck.getDomain());
            cookie.setPath(ck.getPath());
            cookie.setValue(ck.getValue());
            cookie.setVersion(ck.getVersion());
            requestCookies.add(cookie);
        }

        cookies = requestCookies;
    }

    public Series<Cookie> getCookies() {
        return cookies;
    }

    /**
     * Retrieves the Username and Password that matches the credentialsId that you have provided.
     * 
     * @param hubCredentialsId
     *            String the Credential Id that you want to use to find the matching Username and Password for
     * @return UsernamePasswordCredentialsImpl or NULL if the Credentials could not be found, there are no credentials
     *         stored, or the credentials that were
     *         chosen are not a Username and Password
     */
    private UsernamePasswordCredentialsImpl getCredentials(String hubCredentialsId) {
        AbstractProject<?, ?> nullProject = null;
        List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                nullProject, ACL.SYSTEM,
                Collections.<DomainRequirement> emptyList());
        IdMatcher matcher = new IdMatcher(hubCredentialsId);
        for (StandardCredentials c : credentials) {
            if (matcher.matches(c)) {
                if (c instanceof UsernamePasswordCredentialsImpl) {
                    UsernamePasswordCredentialsImpl credential = (UsernamePasswordCredentialsImpl) c;
                    return credential;
                }
            }
        }
        // Could not find the matching credentials
        return null;
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
                formData.getLong(FORM_TIMEOUT), formData.getString(FORM_PORT));
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

    public String getHubPort() {
        return (hubServerInfo == null ? "" : (hubServerInfo
                .getHubPort() == null ? "" : hubServerInfo
                .getHubPort()));
    }

    public long getDefaultTimeout() {
        return DEFAULT_TIMEOUT;
    }

    public String getHubCredentialsId() {
        return (hubServerInfo == null ? "" : (hubServerInfo.getCredentialsId() == null ? "" : hubServerInfo.getCredentialsId()));
    }
}
