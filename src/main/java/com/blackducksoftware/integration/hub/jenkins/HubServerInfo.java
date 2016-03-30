package com.blackducksoftware.integration.hub.jenkins;

import hudson.model.AbstractProject;
import hudson.security.ACL;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

public class HubServerInfo implements Serializable {

    private String serverUrl;

    private String hubCredentialsId;

    private UsernamePasswordCredentialsImpl credential;

    private int timeout;

    public HubServerInfo() {
    }

    public HubServerInfo(String serverUrl, String hubCredentialsId, int timeout) {
        this.serverUrl = serverUrl;
        this.hubCredentialsId = hubCredentialsId;
        this.timeout = timeout;
    }

    public static int getDefaultTimeout() {
        return 120;
    }

    public int getTimeout() {
        if (timeout == 0) {
            return getDefaultTimeout();
        }
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getCredentialsId() {
        return hubCredentialsId;
    }

    public void setCredentialsId(String hubCredentialsId) {
        this.hubCredentialsId = hubCredentialsId;
    }

    public boolean isPluginConfigured() {
        return StringUtils.isNotBlank(getServerUrl()) && StringUtils.isNotBlank(getCredentialsId());
    }

    public String getUsername() {
        UsernamePasswordCredentialsImpl creds = getCredential();
        if (creds == null) {
            return null;
        } else {
            return creds.getUsername();
        }
    }

    public String getPassword() {
        UsernamePasswordCredentialsImpl creds = getCredential();
        if (creds == null) {
            return null;
        } else {
            return creds.getPassword().getPlainText();
        }

    }

    public UsernamePasswordCredentialsImpl getCredential() {
        // Only need to look up the credential when you first run a build or if the credential that the user wants to
        // use has changed.
        if (credential == null || !credential.getId().equals(hubCredentialsId)) {
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
        }
        return credential;
    }

    @Override
    public String toString() {
        return "HubServerInfo [serverUrl=" + serverUrl + ", hubCredentialsId=" + hubCredentialsId + ", credential=" + credential + "]";
        // + ", timeout=" + timeout + "]";
    }

}
