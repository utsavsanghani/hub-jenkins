package com.blackducksoftware.integration.hub.jenkins;

import hudson.model.Result;
import hudson.model.AbstractBuild;

public class BuildHelper {

    public static boolean isSuccess(AbstractBuild<?, ?> build) {
        return build.getResult() == Result.SUCCESS;
    }

    public static boolean isOngoing(AbstractBuild<?, ?> build) {
        return build.getResult() == null;
    }

}
