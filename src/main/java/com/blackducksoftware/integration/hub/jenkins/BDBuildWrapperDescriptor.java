package com.blackducksoftware.integration.hub.jenkins;

import hudson.ProxyConfiguration;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Describable;
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
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDCIScopeException;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDJenkinsHubPluginException;
import com.blackducksoftware.integration.hub.jenkins.maven.MavenBuildWrapperDescriptor;
import com.blackducksoftware.integration.hub.maven.Scope;
import com.blackducksoftware.integration.hub.response.AutoCompleteItem;
import com.blackducksoftware.integration.hub.response.DistributionEnum;
import com.blackducksoftware.integration.hub.response.PhaseEnum;
import com.blackducksoftware.integration.hub.response.ProjectItem;
import com.blackducksoftware.integration.hub.response.ReleaseItem;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

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
    public ListBoxModel doFillHubWrapperVersionPhaseItems() {
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
    public ListBoxModel doFillHubWrapperVersionDistItems() {
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

    public AutoCompletionCandidates doAutoCompleteHubWrapperProjectName(@QueryParameter("value") final String hubWrapperProjectName) throws IOException,
            ServletException {
        AutoCompletionCandidates potentialMatches = new AutoCompletionCandidates();
        if (StringUtils.isNotBlank(getHubServerInfo().getServerUrl()) || StringUtils.isNotBlank(getHubServerInfo().getCredentialsId())) {
            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();
            boolean changed = false;
            try {

                HubIntRestService service = getRestService(getHubServerInfo().getServerUrl(), getHubServerInfo().getUsername(), getHubServerInfo()
                        .getPassword());

                List<AutoCompleteItem> suggestions = service.getProjectMatches(hubWrapperProjectName);

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
     * Performs on-the-fly validation of the form field 'hubWrapperProjectName'. Checks to see if there is already a
     * project in
     * the Hub with this name.
     *
     * @param hubWrapperProjectName
     *            This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckHubWrapperProjectName(@QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName,
            @QueryParameter("hubWrapperProjectVersion") final String hubWrapperProjectVersion) throws IOException, ServletException {
        // Query for the project version so hopefully the check methods run for boths fields
        // when the User changes the Name of the project
        if (hubWrapperProjectName.length() > 0) {
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
                if (hubWrapperProjectName.matches("(\\$\\{.*\\}){1,}")) {
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

                ProjectItem project = service.getProjectByName(hubWrapperProjectName);

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
     * Performs on-the-fly validation of the form field 'hubWrapperProjectVersion'. Checks to see if there is already a
     * project
     * in the Hub with this name.
     *
     * @param hubWrapperProjectVersion
     *            This parameter receives the value that the user has typed for the Version.
     * @return Indicates the outcome of the validation. This is sent to the
     *         browser.
     */
    public FormValidation doCheckHubWrapperProjectVersion(@QueryParameter("hubWrapperProjectVersion") final String hubWrapperProjectVersion,
            @QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName) throws IOException, ServletException {
        if (hubWrapperProjectVersion.length() > 0) {

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
                if (StringUtils.isBlank(hubWrapperProjectName)) {
                    return FormValidation.error(Messages.HubBuildScan_getProvideProjectName());
                }
                if (hubWrapperProjectVersion.matches("(\\$\\{.*\\}){1,}")) {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectVersionContainsVariable());
                }
                if (hubWrapperProjectName.matches("(\\$\\{.*\\}){1,}")) {
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
                    project = service.getProjectByName(hubWrapperProjectName);
                } catch (BDRestException e) {
                    return FormValidation.error(e, e.getMessage());
                }
                List<ReleaseItem> releases = service.getVersionsForProject(project.getId());

                StringBuilder projectVersions = new StringBuilder();
                for (ReleaseItem release : releases) {
                    if (release.getVersion().equals(hubWrapperProjectVersion)) {
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
    public FormValidation doCreateHubWrapperProject(@QueryParameter("hubWrapperProjectName") final String hubWrapperProjectName,
            @QueryParameter("hubWrapperProjectVersion") final String hubWrapperProjectVersion,
            @QueryParameter("hubWrapperVersionPhase") final String hubWrapperVersionPhase,
            @QueryParameter("hubWrapperVersionDist") final String hubWrapperVersionDist) {
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        try {

            save();

            if (StringUtils.isBlank(hubWrapperProjectName)) {
                return FormValidation.error(Messages.HubBuildScan_getProvideProjectName());
            }
            if (StringUtils.isBlank(hubWrapperProjectVersion)) {
                return FormValidation.error(Messages.HubBuildScan_getProvideProjectVersion());
            }
            if (hubWrapperProjectName.matches("(\\$\\{.*\\}){1,}")) {
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
                project = service.getProjectByName(hubWrapperProjectName);
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
                    projectId = service.createHubProject(hubWrapperProjectName);
                    if (projectId != null) {
                        projectCreated = true;
                    }
                } catch (BDRestException e) {
                    return FormValidation.error(e.getMessage());
                }
            } else {
                projectId = project.getId();
            }

            if (hubWrapperProjectVersion.matches("(\\$\\{.*\\}){1,}")) {
                if (projectCreated) {
                    return FormValidation
                            .warning(Messages._HubBuildScan_getProjectCreated() + " :: " + Messages.HubBuildScan_getProjectVersionContainsVariable());
                } else {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectVersionContainsVariable());
                }
            }
            if (StringUtils.isBlank(hubWrapperVersionPhase)) {
                return FormValidation.error(Messages.HubBuildScan_getProvideVersionPhase());
            }
            if (StringUtils.isBlank(hubWrapperVersionDist)) {
                return FormValidation.error(Messages.HubBuildScan_getProvideVersionDist());
            }
            String versionId = null;
            try {
                List<ReleaseItem> releases = service.getVersionsForProject(projectId);
                for (ReleaseItem release : releases) {
                    if (release.getVersion().equals(hubWrapperProjectVersion)) {
                        versionId = release.getId();
                    }

                }
                if (projectExists && versionId != null) {
                    return FormValidation
                            .warning(Messages.HubBuildScan_getProjectAndVersionExist());
                }

                if (versionId == null) {
                    versionId = service.createHubVersion(hubWrapperProjectVersion, projectId, hubWrapperVersionPhase, hubWrapperVersionDist);
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
        if (this instanceof MavenBuildWrapperDescriptor) {
            if (value.length() == 0) {
                return FormValidation.error(Messages
                        .CodeCenterBuildWrapper_getPleaseIncludeAScope());
            }
            try {
                Scope.getScopeListFromString(value);
            } catch (BDCIScopeException e) {
                String scope = e.getMessage().substring(e.getMessage().indexOf(":") + 1).trim();
                return FormValidation.error(Messages.CodeCenterBuildWrapper_getIncludedInvalidScope_0_(scope));
            }
        }
        // else if (this instanceof CodeCenterGradleBuildWrapper.DescriptorImpl) {
        // if (value.length() == 0) {
        // return FormValidation.error(Messages
        // .CodeCenterBuildWrapper_getPleaseIncludeAConfiguration());
        // }
        // }
        return FormValidation.ok();
    }

    public FormValidation doCheckSameAsPostBuildScan(@QueryParameter("sameAsPostBuildScan") final Boolean sameAsPostBuildScan,
            @AncestorInPath AbstractProject project) throws IOException, ServletException {

        if (sameAsPostBuildScan) {
            Describable hubScan = project.getPublishersList().get(PostBuildHubScan.class);
            if (hubScan == null) {
                return FormValidation.error(Messages.HubWrapper_getCLIScanNotConfigured());
            }
        }

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
