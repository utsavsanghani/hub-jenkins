package com.blackducksoftware.integration.hub.jenkins.maven;

import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.maven.MavenReporterDescriptor;
import hudson.model.AutoCompletionCandidates;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDCIScopeException;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.PluginHelper;
import com.blackducksoftware.integration.hub.jenkins.PostBuildScanDescriptor;
import com.blackducksoftware.integration.hub.jenkins.PostBuildHubScan;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.maven.Scope;
import com.blackducksoftware.integration.hub.response.AutoCompleteItem;
import com.blackducksoftware.integration.hub.response.DistributionEnum;
import com.blackducksoftware.integration.hub.response.PhaseEnum;
import com.blackducksoftware.integration.hub.response.ProjectItem;
import com.blackducksoftware.integration.hub.response.ReleaseItem;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

@Extension(optional = true)
// This indicates to Jenkins that this is an implementation of an extension
// point.
public class HubMavenReporterDescriptor extends MavenReporterDescriptor {

    /**
     * In order to load the persisted global configuration, you have to call
     * load() in the constructor.
     */
    public HubMavenReporterDescriptor() {
        super(HubMavenReporter.class);
        load();
    }

    @Override
    public String getDisplayName() {
        // Intentional return the BuildWrapper constants
        return Messages.HubMavenWrapper_getDisplayName();
    }

    public String getPluginVersion() {
        return PluginHelper.getPluginVersion();
    }

    public HubServerInfo getHubServerInfo() {
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
    public ListBoxModel doFillMavenHubVersionPhaseItems() {
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
    public ListBoxModel doFillMavenHubVersionDistItems() {
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

    public HubIntRestService getRestService(String serverUrl, String username, String password) throws BDJenkinsHubPluginException,
            HubIntegrationException, URISyntaxException,
            MalformedURLException {
        HubIntRestService service = new HubIntRestService(serverUrl);
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
                    }
                }
            }
        }
        service.setCookies(username,
                password);
        return service;
    }

    public AutoCompletionCandidates doAutoCompleteMavenHubProjectName(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName)
            throws IOException,
            ServletException {
        AutoCompletionCandidates potentialMatches = new AutoCompletionCandidates();
        if (StringUtils.isNotBlank(getHubServerInfo().getServerUrl()) || StringUtils.isNotBlank(getHubServerInfo().getCredentialsId())) {
            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            boolean changed = false;
            try {
                if (mavenHubProjectName.contains("$")) {
                    return potentialMatches;
                }

                HubIntRestService service = getRestService(getHubServerInfo().getServerUrl(), getHubServerInfo().getUsername(), getHubServerInfo()
                        .getPassword());

                List<AutoCompleteItem> suggestions = service.getProjectMatches(mavenHubProjectName);

                if (!suggestions.isEmpty()) {
                    for (AutoCompleteItem projectSuggestion : suggestions) {
                        potentialMatches.add(projectSuggestion.getValue());
                    }
                }
            } catch (Exception e) {
                // do nothing for exception, there is nowhere in the UI to display this error
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
     * Performs on-the-fly validation of the form field 'mavenHubProjectName'. Checks to see if there is already a
     * project in
     * the Hub with this name.
     *
     * @param mavenHubProjectName
     *            This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckMavenHubProjectName(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName,
            @QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion) throws IOException, ServletException {
        // Query for the project version so hopefully the check methods run for boths fields
        // when the User changes the Name of the project
        if (mavenHubProjectName.length() > 0) {
            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            boolean changed = false;
            try {
                if (StringUtils.isBlank(getHubServerInfo().getServerUrl())) {
                    return FormValidation.error(Messages.HubBuildScan_getPleaseSetServerUrl());
                }
                if (StringUtils.isBlank(getHubServerInfo().getCredentialsId())) {
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
                }
                if (mavenHubProjectName.contains("$")) {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectNameContainsVariable());
                }

                String credentialUserName = null;
                String credentialPassword = null;

                UsernamePasswordCredentialsImpl credential = getHubServerInfo().getCredential();
                if (credential == null) {
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
                }
                credentialUserName = credential.getUsername();
                credentialPassword = credential.getPassword().getPlainText();

                HubIntRestService service = getRestService(getHubServerInfo().getServerUrl(), credentialUserName, credentialPassword);

                ProjectItem project = service.getProjectByName(mavenHubProjectName);

                if (project != null && StringUtils.isNotBlank(project.getId())) {
                    return FormValidation.ok(Messages.HubBuildScan_getProjectExistsIn_0_(getHubServerInfo().getServerUrl()));
                } else {
                    return FormValidation.error(Messages.HubBuildScan_getProjectNonExistingIn_0_(getHubServerInfo().getServerUrl()));
                }
            } catch (BDRestException e) {
                e.printStackTrace();
                return FormValidation.error(e, e.getMessage());
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
                    message = Messages.HubBuildScan_getCanNotReachThisServer_0_(getHubServerInfo().getServerUrl());
                } else if (message.toLowerCase().contains("precondition failed")) {
                    message = message + ", Check your configuration.";
                }
                return FormValidation.error(e, message);
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
     * Performs on-the-fly validation of the form field 'mavenHubProjectVersion'. Checks to see if there is already a
     * project
     * in the Hub with this name.
     *
     * @param mavenHubProjectVersion
     *            This parameter receives the value that the user has typed for the Version.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckMavenHubProjectVersion(@QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion,
            @QueryParameter("mavenHubProjectName") final String mavenHubProjectName) throws IOException, ServletException {
        if (mavenHubProjectVersion.length() > 0) {

            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            boolean changed = false;
            try {
                if (StringUtils.isBlank(getHubServerInfo().getServerUrl())) {
                    return FormValidation.error(Messages.HubBuildScan_getPleaseSetServerUrl());
                }
                if (StringUtils.isBlank(getHubServerInfo().getCredentialsId())) {
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
                }
                if (StringUtils.isBlank(mavenHubProjectName)) {
                    return FormValidation.error(Messages.HubBuildScan_getProvideProjectName());
                }
                if (mavenHubProjectVersion.contains("$")) {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectVersionContainsVariable());
                }
                if (mavenHubProjectName.contains("$")) {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectNameContainsVariable());
                }

                String credentialUserName = null;
                String credentialPassword = null;

                UsernamePasswordCredentialsImpl credential = getHubServerInfo().getCredential();
                if (credential == null) {
                    return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
                }
                credentialUserName = credential.getUsername();
                credentialPassword = credential.getPassword().getPlainText();

                HubIntRestService service = getRestService(getHubServerInfo().getServerUrl(), credentialUserName, credentialPassword);

                ProjectItem project = null;
                try {
                    project = service.getProjectByName(mavenHubProjectName);
                } catch (BDRestException e) {
                    return FormValidation.error(e, e.getMessage());
                }
                List<ReleaseItem> releases = service.getVersionsForProject(project.getId());

                StringBuilder projectVersions = new StringBuilder();
                for (ReleaseItem release : releases) {
                    if (release.getVersion().equals(mavenHubProjectVersion)) {
                        return FormValidation.ok(Messages.HubBuildScan_getVersionExistsIn_0_(project.getName()));
                    } else {
                        if (projectVersions.length() > 0) {
                            projectVersions.append(", " + release.getVersion());
                        } else {
                            projectVersions.append(release.getVersion());
                        }
                    }
                }

                return FormValidation.error(Messages.HubBuildScan_getVersionNonExistingIn_0_(project.getName(), projectVersions.toString()));
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
                    message = Messages.HubBuildScan_getCanNotReachThisServer_0_(getHubServerInfo().getServerUrl());
                } else if (message.toLowerCase().contains("precondition failed")) {
                    message = message + ", Check your configuration.";
                }
                return FormValidation.error(e, message);
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
     * Creates the Hub project AND/OR version
     *
     *
     * @param serverUrl
     *            String
     * @param hubCredentialsId
     *            String
     * @return FormValidation
     * @throws ServletException
     */
    public FormValidation doCreateMavenHubProject(@QueryParameter("mavenHubProjectName") final String mavenHubProjectName,
            @QueryParameter("mavenHubProjectVersion") final String mavenHubProjectVersion,
            @QueryParameter("mavenHubVersionPhase") final String mavenHubVersionPhase,
            @QueryParameter("mavenHubVersionDist") final String mavenHubVersionDist) {
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        try {

            save();

            if (StringUtils.isBlank(mavenHubProjectName)) {
                return FormValidation.error(Messages.HubBuildScan_getProvideProjectName());
            }
            if (StringUtils.isBlank(mavenHubProjectVersion)) {
                return FormValidation.error(Messages.HubBuildScan_getProvideProjectVersion());
            }
            if (mavenHubProjectName.contains("$")) {
                return FormValidation
                        .warning(Messages.HubBuildScan_getProjectNameContainsVariable());
            }

            String credentialUserName = null;
            String credentialPassword = null;

            UsernamePasswordCredentialsImpl credential = getHubServerInfo().getCredential();
            if (credential == null) {
                return FormValidation.error(Messages.HubBuildScan_getCredentialsNotFound());
            }
            credentialUserName = credential.getUsername();
            credentialPassword = credential.getPassword().getPlainText();

            HubIntRestService service = getRestService(getHubServerInfo().getServerUrl(), credentialUserName, credentialPassword);

            boolean projectExists = false;
            boolean projectCreated = false;

            ProjectItem project = null;
            try {
                project = service.getProjectByName(mavenHubProjectName);
                if (project != null && project.getId() != null && project.getName() != null) {
                    projectExists = true;
                }

            } catch (BDRestException e) {
                // Either doesnt exist or cant connect to the server.
                // Assume it doesnt exist for now
            }

            String projectId = null;
            if (!projectExists) {
                try {
                    projectId = service.createHubProject(mavenHubProjectName);
                    if (projectId != null) {
                        projectCreated = true;
                    }
                } catch (BDRestException e) {
                    return FormValidation.error(e.getMessage());
                }
            } else {
                projectId = project.getId();
            }

            if (mavenHubProjectVersion.contains("$")) {
                if (projectCreated) {
                    return FormValidation
                            .warning(Messages._HubBuildScan_getProjectCreated() + " :: " + Messages.HubBuildScan_getProjectVersionContainsVariable());
                } else {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectVersionContainsVariable());
                }
            }
            if (StringUtils.isBlank(mavenHubVersionPhase)) {
                return FormValidation.error(Messages.HubBuildScan_getProvideVersionPhase());
            }
            if (StringUtils.isBlank(mavenHubVersionDist)) {
                return FormValidation.error(Messages.HubBuildScan_getProvideVersionDist());
            }
            String versionId = null;
            try {
                List<ReleaseItem> releases = service.getVersionsForProject(projectId);
                for (ReleaseItem release : releases) {
                    if (release.getVersion().equals(mavenHubProjectVersion)) {
                        versionId = release.getId();
                    }

                }
                if (projectExists && versionId != null) {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectAndVersionExist());
                }

                if (versionId == null) {
                    versionId = service.createHubVersion(mavenHubProjectVersion, projectId, mavenHubVersionPhase, mavenHubVersionDist);
                }
            } catch (BDRestException e) {
                if (e.getResource().getResponse().getStatus().getCode() == 412) {
                    return FormValidation.error(e, Messages.HubBuildScan_getProjectVersionCreationProblem());
                } else if (e.getResource().getResponse().getStatus().getCode() == 401) {
                    // If User is Not Authorized, 401 error, an exception should be thrown by the ClientResource
                    return FormValidation.error(e, Messages.HubBuildScan_getCredentialsInValidFor_0_(getHubServerInfo().getServerUrl()));
                } else {
                    return FormValidation.error(e, Messages.HubBuildScan_getErrorConnectingTo_0_(e.getResource().getResponse().getStatus().getCode()));
                }
            }
            if (StringUtils.isNotBlank(versionId)) {
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
                message = Messages.HubBuildScan_getCanNotReachThisServer_0_(getHubServerInfo().getServerUrl());
            } else if (message.toLowerCase().contains("precondition failed")) {
                message = message + ", Check your configuration.";
            }
            return FormValidation.error(e, message);
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
            }
        }

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
        if (value.length() == 0) {
            return FormValidation.error(Messages
                    .HubMavenWrapper_getPleaseIncludeAScope());
        }
        try {
            Scope.getScopeListFromString(value);
        } catch (BDCIScopeException e) {
            String scope = e.getMessage().substring(e.getMessage().indexOf(":") + 1).trim();
            return FormValidation.error(Messages.HubMavenWrapper_getIncludedInvalidScope_0_(scope));
        }
        return FormValidation.ok();
    }

}
