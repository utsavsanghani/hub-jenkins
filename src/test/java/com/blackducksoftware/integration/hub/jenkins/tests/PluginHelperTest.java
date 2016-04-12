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
package com.blackducksoftware.integration.hub.jenkins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import com.blackducksoftware.integration.hub.jenkins.helper.PluginHelper;

public class PluginHelperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @WithoutJenkins
    @Test
    public void testGetPluginVersionUnknown() {
        assertEquals(PluginHelper.UNKNOWN_VERSION, PluginHelper.getPluginVersion());
    }

    @Test
    public void testGetPluginVersion() {
        assertNotNull(PluginHelper.getPluginVersion(), PluginHelper.getPluginVersion());
    }
}
