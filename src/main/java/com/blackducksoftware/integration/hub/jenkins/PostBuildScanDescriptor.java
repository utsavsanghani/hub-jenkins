package com.blackducksoftware.integration.hub.jenkins;

import hudson.Extension;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.ProxyConfiguration;
import hudson.model.AutoCompletionCandidates;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;

import java.io.IOException;
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

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.blackducksoftware.integration.hub.jenkins.ScanInstallation.IScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDRestException;
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
 * Descriptor for {@link PostBuildHubScan}. Used as a singleton. The
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

    private static final String FORM_SERVER_URL = "hubServerUrl";

    // private static final String FORM_TIMEOUT = "timeout";

    // private static final long DEFAULT_TIMEOUT = 300;

    private static final String FORM_CREDENTIALSID = "hubCredentialsId";

    private HubServerInfo hubServerInfo;

    /**
     * In order to load the persisted global configuration, you have to call
     * load() in the constructor.
     */
    public PostBuildScanDescriptor() {
        super(PostBuildHubScan.class);
        load();
    }

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

    public String getPluginVersion() {
        Plugin p = Jenkins.getInstance().getPlugin("hub-jenkins");
        PluginWrapper pw = p.getWrapper();
        return pw.getVersion();
    }

    public String getDefaultProjectName() {
        return "${JOB_NAME}";
    }

    public String getDefaultProjectVersion() {
        return "<unnamed>";
    }

    public void setupService(JenkinsHubIntRestService service) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            ProxyConfiguration proxy = jenkins.proxy;
            if (proxy != null) {
                service.setNoProxyHosts(proxy.getNoProxyHostPatterns());
                service.setProxyHost(proxy.name);
                service.setProxyPort(proxy.port);
            }
        }
    }

    public FormValidation doCheckScanMemory(@QueryParameter("scanMemory") String scanMemory)
            throws IOException, ServletException {
        if (scanMemory.length() == 0) {
            return FormValidation.error(Messages
                    .HubBuildScan_getNeedMemory());
        }

        try {
            Integer.valueOf(scanMemory);
        } catch (NumberFormatException e) {
            return FormValidation.error(Messages
                    .HubBuildScan_getInvalidMemoryString());
        }

        return FormValidation.ok();
    }

    /**
     * Fills the Credential drop down list in the global config
     *
     * @return
     */
    public ListBoxModel doFillHubCredentialsIdItems() {

        ListBoxModel boxModel = null;
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        try {
            if (PostBuildScanDescriptor.class.getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(PostBuildScanDescriptor.class.getClassLoader());
            }

            // Code copied from
            // https://github.com/jenkinsci/git-plugin/blob/f6d42c4e7edb102d3330af5ca66a7f5809d1a48e/src/main/java/hudson/plugins/git/UserRemoteConfig.java
            CredentialsMatcher credentialsMatcher = CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
            AbstractProject<?, ?> project = null; // Dont want to limit the search to a particular project for the drop
            // down menu
            boxModel = new StandardListBoxModel().withEmptySelection().withMatching(credentialsMatcher,
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
    public ListBoxModel doFillScanNameItems() {
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        ListBoxModel items = null;
        try {
            items = new ListBoxModel();
            Jenkins jenkins = Jenkins.getInstance();
            IScanDescriptor iScanDescriptor = jenkins.getDescriptorByType(IScanDescriptor.class);

            ScanInstallation[] iScanInstallations = iScanDescriptor.getInstallations();
            for (ScanInstallation iScan : iScanInstallations) {
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
            items.add("PLANNING", "PLANNING");
            items.add("DEVELOPMENT", "DEVELOPMENT");
            items.add("RELEASED", "RELEASED");
            items.add("DEPRECATED", "DEPRECATED");
            items.add("ARCHIVED", "ARCHIVED");

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
            items.add("EXTERNAL", "EXTERNAL");
            items.add("SAAS", "SAAS");
            items.add("INTERNAL", "INTERNAL");

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
     * Performs on-the-fly validation of the form field 'serverUrl'.
     *
     * @param value
     *            This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckServerUrl(@QueryParameter("serverUrl") String serverUrl)
            throws IOException, ServletException {
        if (serverUrl.length() == 0) {
            return FormValidation.error(Messages
                    .HubBuildScan_getPleaseSetServerUrl());
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
            return FormValidation.error(Messages
                    .HubBuildScan_getCanNotReachThisServer_0_(serverUrl));
        } catch (RuntimeException e) {
            return FormValidation.error(Messages
                    .HubBuildScan_getNotAValidUrl());
        }
        return FormValidation.ok();
    }

    public AutoCompletionCandidates doAutoCompleteHubProjectName(@QueryParameter("value") final String hubProjectName) throws IOException,
            ServletException {
        AutoCompletionCandidates potentialMatches = new AutoCompletionCandidates();
        if (!StringUtils.isEmpty(getHubServerUrl()) || !StringUtils.isEmpty(getHubServerInfo().getCredentialsId())) {
            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            boolean changed = false;
            try {

                JenkinsHubIntRestService service = new JenkinsHubIntRestService();
                setupService(service);
                service.setBaseUrl(getHubServerUrl());
                service.setCookies(getHubServerInfo().getUsername(), getHubServerInfo().getPassword());
                ArrayList<LinkedHashMap<String, Object>> responseList = service.getProjectMatches(hubProjectName);

                if (!responseList.isEmpty()) {
                    ArrayList<String> projectNames = new ArrayList<String>();
                    for (LinkedHashMap<String, Object> map : responseList) {
                        if (map.get("value").equals(hubProjectName)) {
                            if (!projectNames.contains(map.get("value"))) {
                                projectNames.add((String) map.get("value"));
                            }
                        } else {
                            // name does not match
                            projectNames.add((String) map.get("value"));
                        }
                    }
                    if (!projectNames.isEmpty()) {
                        for (String projectName : projectNames) {
                            potentialMatches.add(projectName);
                        }
                    }
                }
            } catch (Exception e) {
                // do nothing for exception
            } finally {
                if (changed) {
                    Thread.currentThread().setContextClassLoader(
                            originalClassLoader);
                }
            }

        }
        return potentialMatches;
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
    public FormValidation doCheckHubProjectName(@QueryParameter("hubProjectName") final String hubProjectName,
            @QueryParameter("hubProjectVersion") final String hubProjectVersion) throws IOException, ServletException {
        // Query for the project version so hopefully the check methods run for boths fields
        // when the User changes the Name of the project
        if (hubProjectName.length() > 0) {
            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            boolean changed = false;
            try {
                if (StringUtils.isEmpty(getHubServerUrl())) {
                    return FormValidation.error(Messages.HubBuildScan_getPleaseSetServerUrl());
                }
                if (StringUtils.isEmpty(getHubServerInfo().getCredentialsId())) {
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
                }
                if (hubProjectName.matches("(\\$\\{.*\\}){1,}")) {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectNameContainsVariable());
                }

                String credentialUserName = null;
                String credentialPassword = null;

                UsernamePasswordCredentialsImpl credential = hubServerInfo.getCredential();
                if (credential == null) {
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
                }
                credentialUserName = credential.getUsername();
                credentialPassword = credential.getPassword().getPlainText();
                JenkinsHubIntRestService service = new JenkinsHubIntRestService();
                setupService(service);
                service.setBaseUrl(getHubServerUrl());
                service.setCookies(credentialUserName, credentialPassword);

                String projectId = service.getProjectId(hubProjectName);

                if (projectId != null) {
                    return FormValidation.ok(Messages.HubBuildScan_getProjectExistsIn_0_(getHubServerUrl()));
                } else {
                    return FormValidation.error(Messages.HubBuildScan_getProjectNonExistingIn_0_(getHubServerUrl()));
                }
            } catch (BDRestException e) {
                e.printStackTrace();
                return FormValidation.error(e.getMessage());
            } catch (Exception e) {
                String message;
                if (e.getCause() != null && e.getCause().getCause() != null) {
                    message = e.getCause().getCause().toString();
                } else if (e.getCause() != null) {
                    message = e.getCause().toString();
                } else {
                    message = e.toString();
                }
                if (message.toLowerCase().contains("service unavailable")) {
                    message = Messages.HubBuildScan_getCanNotReachThisServer_0_(getHubServerUrl());
                } else if (message.toLowerCase().contains("precondition failed")) {
                    message = message + ", Check your configuration.";
                }
                return FormValidation.error(message);
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
     * Performs on-the-fly validation of the form field 'hubProjectVersion'. Checks to see if there is already a project
     * in the Hub with this name.
     *
     * @param hubProjectVersion
     *            This parameter receives the value that the user has typed for the Version.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckHubProjectVersion(@QueryParameter("hubProjectVersion") final String hubProjectVersion,
            @QueryParameter("hubProjectName") final String hubProjectName) throws IOException, ServletException {
        if (hubProjectVersion.length() > 0) {

            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            boolean changed = false;
            try {
                if (StringUtils.isEmpty(getHubServerUrl())) {
                    return FormValidation.error(Messages.HubBuildScan_getPleaseSetServerUrl());
                }
                if (StringUtils.isEmpty(getHubServerInfo().getCredentialsId())) {
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
                }
                if (StringUtils.isEmpty(hubProjectName)) {
                    return FormValidation.error(Messages.HubBuildScan_getProvideProjectName());
                }
                if (hubProjectVersion.matches("(\\$\\{.*\\}){1,}")) {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectVersionContainsVariable());
                }
                if (hubProjectName.matches("(\\$\\{.*\\}){1,}")) {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectNameContainsVariable());
                }

                String credentialUserName = null;
                String credentialPassword = null;

                UsernamePasswordCredentialsImpl credential = hubServerInfo.getCredential();
                if (credential == null) {
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
                }
                credentialUserName = credential.getUsername();
                credentialPassword = credential.getPassword().getPlainText();
                JenkinsHubIntRestService service = new JenkinsHubIntRestService();
                setupService(service);
                service.setBaseUrl(getHubServerUrl());
                service.setCookies(credentialUserName, credentialPassword);
                String idToUse = null;
                try {
                    idToUse = service.getProjectId(hubProjectName);
                } catch (BDRestException e) {
                    return FormValidation.error(e.getMessage());
                }

                HashMap<String, Object> responseMap = service.getVersionMatchesForProjectId(idToUse);
                StringBuilder projectVersions = new StringBuilder();
                if (responseMap.containsKey("items")) {
                    ArrayList<LinkedHashMap> versionList = (ArrayList<LinkedHashMap>) responseMap.get("items");
                    for (LinkedHashMap release : versionList) {
                        if (((String) release.get("version")).equals(hubProjectVersion)) {
                            return FormValidation.ok(Messages.HubBuildScan_getVersionExistsIn_0_(idToUse));
                        } else {
                            if (projectVersions.length() > 0) {
                                projectVersions.append(", " + ((String) release.get("version")));
                            } else {
                                projectVersions.append((String) release.get("version"));
                            }
                        }
                    }
                } else {
                    // The Hub Api has changed and we received a JSON response that we did not expect
                    return FormValidation.error(Messages.HubBuildScan_getIncorrectMappingOfServerResponse());
                }
                return FormValidation.error(Messages.HubBuildScan_getVersionNonExistingIn_0_(idToUse, projectVersions.toString()));
            } catch (Exception e) {
                String message;
                if (e.getCause() != null && e.getCause().getCause() != null) {
                    message = e.getCause().getCause().toString();
                } else if (e.getCause() != null) {
                    message = e.getCause().toString();
                } else {
                    message = e.toString();
                }
                if (message.toLowerCase().contains("service unavailable")) {
                    message = Messages.HubBuildScan_getCanNotReachThisServer_0_(getHubServerUrl());
                } else if (message.toLowerCase().contains("precondition failed")) {
                    message = message + ", Check your configuration.";
                }
                return FormValidation.error(message);
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
    public FormValidation doTestConnection(@QueryParameter("hubServerUrl") final String serverUrl,
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
            FormValidation urlCheck = doCheckServerUrl(serverUrl);
            if (urlCheck.kind != Kind.OK) {
                return urlCheck;
            }

            String credentialUserName = null;
            String credentialPassword = null;

            UsernamePasswordCredentialsImpl credential = null;
            AbstractProject<?, ?> project = null;
            List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                    project, ACL.SYSTEM,
                    Collections.<DomainRequirement> emptyList());
            IdMatcher matcher = new IdMatcher(hubCredentialsId);
            for (StandardCredentials c : credentials) {
                if (matcher.matches(c) && c instanceof UsernamePasswordCredentialsImpl) {
                    credential = (UsernamePasswordCredentialsImpl) c;
                }
            }
            if (credential == null) {
                return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
            }
            credentialUserName = credential.getUsername();
            credentialPassword = credential.getPassword().getPlainText();

            JenkinsHubIntRestService service = new JenkinsHubIntRestService();

            setupService(service);
            service.setBaseUrl(serverUrl);

            int responseCode = service.setCookies(credentialUserName, credentialPassword);

            if (responseCode == 200 || responseCode == 204 || responseCode == 202) {
                return FormValidation.ok(Messages.HubBuildScan_getCredentialsValidFor_0_(serverUrl));
            } else if (responseCode == 401) {
                // If User is Not Authorized, 401 error, an exception should be thrown by the ClientResource
                return FormValidation.error(Messages.HubBuildScan_getCredentialsInValidFor_0_(serverUrl));
            } else {
                return FormValidation.error(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
            }
        } catch (Exception e) {
            String message = null;
            if (e instanceof BDJenkinsHubPluginException) {
                message = e.getMessage();
            } else if (e.getCause() != null && e.getCause().getCause() != null) {
                message = e.getCause().getCause().toString();
            } else if (e.getCause() != null) {
                message = e.getCause().toString();
            } else {
                message = e.toString();
            }
            if (message.toLowerCase().contains("service unavailable")) {
                message = Messages.HubBuildScan_getCanNotReachThisServer_0_(serverUrl);
            } else if (message.toLowerCase().contains("precondition failed")) {
                message = message + ", Check your configuration.";
            }
            return FormValidation.error(message);
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
            }
        }

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
    public FormValidation doCreateHubProject(@QueryParameter("hubProjectName") final String hubProjectName,
            @QueryParameter("hubProjectVersion") final String hubProjectVersion, @QueryParameter("hubVersionPhase") final String hubVersionPhase,
            @QueryParameter("hubVersionDist") final String hubVersionDist) {
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        try {

            save();

            if (StringUtils.isEmpty(hubProjectName)) {
                return FormValidation.error(Messages.HubBuildScan_getProvideProjectName());
            }
            if (StringUtils.isEmpty(hubProjectVersion)) {
                return FormValidation.error(Messages.HubBuildScan_getProvideProjectVersion());
            }
            if (hubProjectName.matches("(\\$\\{.*\\}){1,}")) {
                return FormValidation
                        .warning(Messages.HubBuildScan_getProjectNameContainsVariable());
            }

            String credentialUserName = null;
            String credentialPassword = null;

            UsernamePasswordCredentialsImpl credential = hubServerInfo.getCredential();
            if (credential == null) {
                return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
            }
            credentialUserName = credential.getUsername();
            credentialPassword = credential.getPassword().getPlainText();

            JenkinsHubIntRestService service = new JenkinsHubIntRestService();
            setupService(service);
            service.setBaseUrl(getHubServerUrl());
            service.setCookies(credentialUserName, credentialPassword);

            boolean projectExists = false;
            boolean projectCreated = false;

            String projectId = null;
            try {
                projectId = service.getProjectId(hubProjectName);
                if (projectId != null) {
                    projectExists = true;
                }
            } catch (BDRestException e) {
                // Either doesnt exist or cant connect to the server.
                // Assume it doesnt exist for now
            }

            if (projectId == null) {
                try {
                    projectId = service.createHubProject(hubProjectName);
                    if (projectId != null) {
                        projectCreated = true;
                    }
                } catch (BDRestException e) {
                    return FormValidation.error(e.getMessage());
                }
            }

            if (hubProjectVersion.matches("(\\$\\{.*\\}){1,}")) {
                if (projectCreated) {
                    return FormValidation
                            .warning(Messages._HubBuildScan_getProjectCreated() + " :: " + Messages.HubBuildScan_getProjectVersionContainsVariable());
                } else {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectVersionContainsVariable());
                }
            }
            String versionId = null;
            try {
                LinkedHashMap<String, Object> versionMatchesResponse = service.getVersionMatchesForProjectId(projectId);
                versionId = service.getVersionIdFromMatches(versionMatchesResponse, hubProjectVersion, hubVersionPhase, hubVersionDist);
                if (projectExists && versionId != null) {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectAndVersionExist());
                }

                if (versionId == null) {
                    versionId = service.createHubVersion(hubProjectVersion, projectId, hubVersionPhase, hubVersionDist);
                }
            } catch (BDRestException e) {
                if (e.getResource().getResponse().getStatus().getCode() == 412) {
                    return FormValidation.error(Messages.HubBuildScan_getProjectVersionCreationProblem());
                } else if (e.getResource().getResponse().getStatus().getCode() == 401) {
                    // If User is Not Authorized, 401 error, an exception should be thrown by the ClientResource
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsInValidFor_0_(getHubServerUrl()));
                } else {
                    return FormValidation.error(Messages.HubBuildScan_getErrorConnectingTo_0_(e.getResource().getResponse().getStatus().getCode()));
                }
            }
            if (!StringUtils.isEmpty(versionId)) {
                return FormValidation.ok(Messages.HubBuildScan_getProjectAndVersionCreated());
            } else {
                return FormValidation.error(Messages.HubBuildScan_getProjectVersionCreationProblem());
            }

        } catch (Exception e) {
            String message;
            if (e.getCause() != null && e.getCause().getCause() != null) {
                message = e.getCause().getCause().toString();
            } else if (e.getCause() != null) {
                message = e.getCause().toString();
            } else {
                message = e.toString();
            }
            if (message.toLowerCase().contains("service unavailable")) {
                message = Messages.HubBuildScan_getCanNotReachThisServer_0_(getHubServerUrl());
            } else if (message.toLowerCase().contains("precondition failed")) {
                message = message + ", Check your configuration.";
            }
            return FormValidation.error(message);
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
    // if (value.startsWith("/") || value.startsWith("\\")) {
    // return FormValidation.warning("");
    // }
    // if (value.endsWith("/") || value.endsWith("\\")) {
    // return FormValidation.warning("");
    // }
    // return FormValidation.ok();
    // }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws Descriptor.FormException {
        // To persist global configuration information,
        // set that to properties and call save().
        hubServerInfo = new HubServerInfo(formData.getString(FORM_SERVER_URL), formData.getString(FORM_CREDENTIALSID));
        // formData.getLong(FORM_TIMEOUT));
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

    public String getHubServerUrl() {
        return (hubServerInfo == null ? "" : (hubServerInfo
                .getServerUrl() == null ? "" : hubServerInfo
                .getServerUrl()));
    }

    // public long getTimeout() {
    // return hubServerInfo == null ? getDefaultTimeout()
    // : hubServerInfo.getTimeout();
    // }
    //
    // public long getDefaultTimeout() {
    // return DEFAULT_TIMEOUT;
    // }

    public String getHubCredentialsId() {
        return (hubServerInfo == null ? "" : (hubServerInfo.getCredentialsId() == null ? "" : hubServerInfo.getCredentialsId()));
    }
}
