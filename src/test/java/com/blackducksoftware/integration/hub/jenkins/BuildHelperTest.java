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
package com.blackducksoftware.integration.hub.jenkins;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

public class BuildHelperTest {
    @Mock
    private AbstractBuild<FreeStyleProject, FreeStyleBuild> build;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsSuccess() {
        when(build.getResult()).thenReturn(Result.SUCCESS);

        assertTrue(BuildHelper.isSuccess(build));
    }

    @Test
    public void testIsSuccessFailure() {
        when(build.getResult()).thenReturn(Result.FAILURE);

        assertFalse(BuildHelper.isSuccess(build));
    }

    @Test
    public void testIsOngoing() {
        when(build.getResult()).thenReturn(null);

        assertTrue(BuildHelper.isOngoing(build));
    }

    @Test
    public void testIsNotOngoing() {
        when(build.getResult()).thenReturn(Result.SUCCESS);

        assertFalse(BuildHelper.isOngoing(build));
    }

}
