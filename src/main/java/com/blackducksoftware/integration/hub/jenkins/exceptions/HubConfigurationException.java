package com.blackducksoftware.integration.hub.jenkins.exceptions;

public class HubConfigurationException extends Exception {

    public HubConfigurationException() {

    }

    public HubConfigurationException(String message)
    {
        super(message);
    }

    public HubConfigurationException(Throwable cause)
    {
        super(cause);
    }

    public HubConfigurationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
