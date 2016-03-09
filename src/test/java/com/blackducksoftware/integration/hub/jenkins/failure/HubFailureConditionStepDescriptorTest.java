package com.blackducksoftware.integration.hub.jenkins.failure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import hudson.util.FormValidation;

import org.junit.Test;
import org.mockito.Mockito;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.jenkins.Messages;

public class HubFailureConditionStepDescriptorTest {

    private HubIntRestService getMockedService(String returnVersion) throws Exception {
        HubIntRestService service = Mockito.mock(HubIntRestService.class);
        Mockito.when(service.getHubVersion()).thenReturn(returnVersion);
        return service;
    }

    @Test
    public void testGetDisplayName() {
        HubFailureConditionStepDescriptor descriptor = new HubFailureConditionStepDescriptor();
        assertEquals(Messages.HubFailureCondition_getDisplayName(), descriptor.getDisplayName());
    }

    @Test
    public void testIsApplicablePolicyNotSupportedByHub() throws Exception {
        HubFailureConditionStepDescriptor descriptor = new HubFailureConditionStepDescriptor();
        descriptor = Mockito.spy(descriptor);

        HubIntRestService service = getMockedService("1.0.0");
        HubSupportHelper hubSupport = new HubSupportHelper();
        hubSupport.checkHubSupport(service, null);
        Mockito.when(descriptor.getCheckedHubSupportHelper()).thenReturn(hubSupport);

        assertTrue(!descriptor.isApplicable(null));
    }

    @Test
    public void testIsApplicablePoliciesSupported() throws Exception {
        HubFailureConditionStepDescriptor descriptor = new HubFailureConditionStepDescriptor();
        descriptor = Mockito.spy(descriptor);

        HubIntRestService service = getMockedService("3.0.0");
        HubSupportHelper hubSupport = new HubSupportHelper();
        hubSupport.checkHubSupport(service, null);
        Mockito.when(descriptor.getCheckedHubSupportHelper()).thenReturn(hubSupport);

        assertTrue(descriptor.isApplicable(null));
    }

    @Test
    public void testDoCheckFailBuildForPolicyViolations() throws Exception {
        HubFailureConditionStepDescriptor descriptor = new HubFailureConditionStepDescriptor();

        FormValidation validation = descriptor.doCheckFailBuildForPolicyViolations(false);
        assertEquals(FormValidation.Kind.OK, validation.kind);
    }

    @Test
    public void testDoCheckFailBuildForPolicyViolationsPolicyNotSupported() throws Exception {
        HubFailureConditionStepDescriptor descriptor = new HubFailureConditionStepDescriptor();
        descriptor = Mockito.spy(descriptor);

        HubIntRestService service = getMockedService("1.0.0");
        HubSupportHelper hubSupport = new HubSupportHelper();
        hubSupport.checkHubSupport(service, null);
        Mockito.when(descriptor.getCheckedHubSupportHelper()).thenReturn(hubSupport);

        FormValidation validation = descriptor.doCheckFailBuildForPolicyViolations(true);
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
        assertEquals(Messages.HubFailureCondition_getPoliciesNotSupported(), validation.getMessage());
    }

    @Test
    public void testDoCheckFailBuildForPolicyViolationsPolicySupported() throws Exception {
        HubFailureConditionStepDescriptor descriptor = new HubFailureConditionStepDescriptor();
        descriptor = Mockito.spy(descriptor);

        HubIntRestService service = getMockedService("3.0.0");
        HubSupportHelper hubSupport = new HubSupportHelper();
        hubSupport.checkHubSupport(service, null);
        Mockito.when(descriptor.getCheckedHubSupportHelper()).thenReturn(hubSupport);

        FormValidation validation = descriptor.doCheckFailBuildForPolicyViolations(true);
        assertEquals(FormValidation.Kind.OK, validation.kind);
    }
}
