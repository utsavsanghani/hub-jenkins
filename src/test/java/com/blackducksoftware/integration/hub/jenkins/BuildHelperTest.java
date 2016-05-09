/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.jenkins;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.blackducksoftware.integration.hub.jenkins.category.UnitTest;
import com.blackducksoftware.integration.hub.jenkins.helper.BuildHelper;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

@Category(UnitTest.class)
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
