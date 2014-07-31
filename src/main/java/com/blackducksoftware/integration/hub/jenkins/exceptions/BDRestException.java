package com.blackducksoftware.integration.hub.jenkins.exceptions;

public class BDRestException extends Exception {

    public BDRestException() {

    }

    public BDRestException(String message)
    {
        super(message);
    }

    public BDRestException(Throwable cause)
    {
        super(cause);
    }

    public BDRestException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
