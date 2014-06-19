package com.blackducksoftware.integration.hub.jenkins.exceptions;

public class IScanToolMissingException extends Exception {

    public IScanToolMissingException() {

    }

    public IScanToolMissingException(String message)
    {
        super(message);
    }

    public IScanToolMissingException(Throwable cause)
    {
        super(cause);
    }

    public IScanToolMissingException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
