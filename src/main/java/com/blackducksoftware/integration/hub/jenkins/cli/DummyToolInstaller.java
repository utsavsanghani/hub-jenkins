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
package com.blackducksoftware.integration.hub.jenkins.cli;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.tools.ToolInstallation;

import java.io.IOException;

public class DummyToolInstaller extends ToolInstaller {

    public DummyToolInstaller() {
        super(null);
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        return null;
    }

    @Override
    public ToolInstallerDescriptor<?> getDescriptor() {

        return new DummyToolInstallerDescriptor();
    }

    public FilePath getToolDir(ToolInstallation tool, Node node) {
        FilePath toolsDir = preferredLocation(tool, node);
        // preferredLocation will return {root}/tools/descriptorId/installationName
        // and we want to return {root}/tools
        return toolsDir.getParent().getParent();
    }

    public static class DummyToolInstallerDescriptor extends ToolInstallerDescriptor<DummyToolInstaller> {

        @Override
        public String getDisplayName() {
            return null;
        }

    }

}
