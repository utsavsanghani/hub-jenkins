package com.blackducksoftware.integration.hub.jenkins.exceptions;

public class BDJenkinsHubPluginException extends Exception {

    public BDJenkinsHubPluginException() {

    }

    public BDJenkinsHubPluginException(String message)
    {
        super(message);
    }

    public BDJenkinsHubPluginException(Throwable cause)
    {
        super(cause);
    }

    public BDJenkinsHubPluginException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
