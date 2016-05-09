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
package com.blackducksoftware.integration.hub.jenkins.mock;

import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;

import java.util.List;

public class TestProject extends AbstractProject<TestProject, TestBuild> implements TopLevelItem {

    private List<Publisher> publishers;

    public TestProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    protected Class getBuildClass() {
        return null;
    }

    @Override
    public boolean isFingerprintConfigured() {
        return false;
    }

    public void setPublishersList(List<Publisher> publishers) {
        this.publishers = publishers;
    }

    @Override
    public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
        if (publishers == null) {
            return null;
        }

        DescribableList<Publisher, Descriptor<Publisher>> temp = new DescribableList<Publisher, Descriptor<Publisher>>(null, publishers);

        return temp;
    }

    @Override
    public TopLevelItemDescriptor getDescriptor() {
        return null;
    }

}
