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

    public HubSupportHelper getCheckedHubSupportHelper() {
        HubSupportHelper hubSupport = new HubSupportHelper();
        HubServerInfo serverInfo = HubServerInfoSingleton.getInstance().getServerInfo();
        try {
            HubIntRestService service = BuildHelper.getRestService(null, serverInfo.getServerUrl(),
                    serverInfo.getUsername(),
                    serverInfo.getPassword(),
                    serverInfo.getTimeout());
            hubSupport.checkHubSupport(service, null);
        } catch (Exception e) {
            return null;
        }
        return hubSupport;
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        // Indicates that this builder can be used with all kinds of project
        // types

        HubSupportHelper hubSupport = getCheckedHubSupportHelper();
        if (hubSupport != null && hubSupport.isPolicyApiSupport()) {
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

            HubSupportHelper hubSupport = getCheckedHubSupportHelper();

            if (hubSupport != null && !hubSupport.isPolicyApiSupport()) {
                return FormValidation.error(Messages.HubFailureCondition_getPoliciesNotSupported());
            }
        }

        return FormValidation.ok();
    }

}
