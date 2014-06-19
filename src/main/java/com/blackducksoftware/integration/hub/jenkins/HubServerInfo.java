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

    private String credentialsId;

    private UsernamePasswordCredentialsImpl credential;

    private long timeout;

    public HubServerInfo() {
    }

    public HubServerInfo(String serverUrl, String credentialsId, long timeout) {
        super();
        this.serverUrl = serverUrl;
        this.credentialsId = credentialsId;
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * @param credentialsId
     *            the credentialsId to set
     */
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public boolean isPluginConfigured() {
        return !(StringUtils.isEmpty(getServerUrl()) || StringUtils.isEmpty(getCredentialsId()));
    }

    public String getUsername() {
        UsernamePasswordCredentialsImpl creds = getCredential();

        return creds.getUsername();
    }

    public String getPassword() {
        UsernamePasswordCredentialsImpl creds = getCredential();

        return creds.getPassword().getPlainText();
    }

    private UsernamePasswordCredentialsImpl getCredential() {
        // Only need to look up the credential when you first run a build or if the credential that the user wants to
        // use has changed.
        if (credential == null || !credential.getId().equals(credentialsId)) {
            AbstractProject<?, ?> project = null;
            List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                    project, ACL.SYSTEM,
                    Collections.<DomainRequirement> emptyList());
            IdMatcher matcher = new IdMatcher(credentialsId);
            for (StandardCredentials c : credentials) {
                if (matcher.matches(c)) {
                    if (c instanceof UsernamePasswordCredentialsImpl) {
                        UsernamePasswordCredentialsImpl credential = (UsernamePasswordCredentialsImpl) c;
                        this.credential = credential;
                    }
                }
            }
        }
        return credential;
    }

    /*
     * (non-JSDoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "HubServerInfo [serverUrl=" + serverUrl + ", credentialsId=" + credentialsId + ", credential=" + credential + ", timeout=" + timeout
                + "]";
    }

}
