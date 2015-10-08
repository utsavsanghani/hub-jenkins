package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.Result;
import hudson.model.AbstractBuild;

import com.blackducksoftware.integration.hub.jenkins.BuildHelper;

public class BuildHelperTest {

    public void testIsSuccess() {
        AbstractBuild build = mock(AbstractBuild.class);

        when(build.getResult()).thenReturn(Result.SUCCESS);

        assertTrue(BuildHelper.isSuccess(build));

    }

    public void testIsSuccessFailure() {
        AbstractBuild build = mock(AbstractBuild.class);

        when(build.getResult()).thenReturn(Result.FAILURE);

        assertTrue(!BuildHelper.isSuccess(build));

    }

    public void testIsOngoing() {
        AbstractBuild build = mock(AbstractBuild.class);

        when(build.getResult()).thenReturn(null);

        assertTrue(BuildHelper.isOngoing(build));

    }

    public void testIsNotOngoing() {
        AbstractBuild build = mock(AbstractBuild.class);

        when(build.getResult()).thenReturn(Result.SUCCESS);

        assertTrue(!BuildHelper.isOngoing(build));

    }
}
