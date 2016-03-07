package com.blackducksoftware.integration.hub.jenkins.failure;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;

import java.io.Serializable;

import com.blackducksoftware.integration.hub.jenkins.Messages;

@Extension
public class HubFailureConditionStepDescriptor extends BuildStepDescriptor<Publisher> implements Serializable {

    public HubFailureConditionStepDescriptor() {
        super(HubFailureConditionStep.class);
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        // Indicates that this builder can be used with all kinds of project
        // types
        return true;
    }

    @Override
    public String getDisplayName() {
        return Messages.HubFailureCondition_getDisplayName();
    }

}
