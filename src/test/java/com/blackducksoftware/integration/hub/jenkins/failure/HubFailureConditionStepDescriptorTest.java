/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
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
