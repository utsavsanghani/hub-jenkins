package com.blackducksoftware.integration.hub.jenkins.exceptions;

public class HubScanToolMissingException extends Exception {

    public HubScanToolMissingException() {

    }

    public HubScanToolMissingException(String message)
    {
        super(message);
    }

    public HubScanToolMissingException(Throwable cause)
    {
        super(cause);
    }

    public HubScanToolMissingException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
