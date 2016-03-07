package com.blackducksoftware.integration.hub.jenkins.failure;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;

@Extension
public class HubFailureConditionStepDescriptor extends BuildStepDescriptor<Publisher> implements Serializable {

    public HubFailureConditionStepDescriptor() {
        super(HubFailureConditionStep.class);
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        // Indicates that this builder can be used with all kinds of project
        // types

        HubServerInfo serverInfo = HubServerInfoSingleton.getInstance().getServerInfo();
        HubSupportHelper hubSupport = new HubSupportHelper();
        HubIntRestService service;
        try {
            service = BuildHelper.getRestService(null, serverInfo.getServerUrl(),
                    serverInfo.getUsername(),
                    serverInfo.getPassword(),
                    serverInfo.getTimeout());

            hubSupport.checkHubSupport(service, null);

        } catch (Exception e) {
            return false;
        }
        if (hubSupport.isPolicyApiSupport()) {
            return true;
        }

        return false;
    }

    @Override
    public String getDisplayName() {
        return Messages.HubFailureCondition_getDisplayName();
    }

    public FormValidation doCheckFailBuildForPolicyViolations(@QueryParameter("failBuildForPolicyViolations") boolean failBuildForPolicyViolations)
            throws IOException, ServletException {
        if (failBuildForPolicyViolations) {

            HubServerInfo serverInfo = HubServerInfoSingleton.getInstance().getServerInfo();
            HubSupportHelper hubSupport = new HubSupportHelper();
            HubIntRestService service;
            try {
                service = BuildHelper.getRestService(null, serverInfo.getServerUrl(),
                        serverInfo.getUsername(),
                        serverInfo.getPassword(),
                        serverInfo.getTimeout());

                hubSupport.checkHubSupport(service, null);

            } catch (Exception e) {
                // Something was not configured correctly. The error will appear in the Hub Scan configuration of the
                // Job or in the Build's Console Output
                return FormValidation.ok();
            }

            if (!hubSupport.isPolicyApiSupport()) {
                return FormValidation.error(Messages.HubFailureCondition_getPoliciesNotSupported());
            }
        }

        return FormValidation.ok();
    }

}
