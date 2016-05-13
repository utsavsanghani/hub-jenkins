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
