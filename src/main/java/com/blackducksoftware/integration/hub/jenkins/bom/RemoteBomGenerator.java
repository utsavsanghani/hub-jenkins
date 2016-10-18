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
package com.blackducksoftware.integration.hub.jenkins.bom;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.api.report.HubReportGenerationInfo;
import com.blackducksoftware.integration.hub.api.report.RiskReportGenerator;
import com.blackducksoftware.integration.hub.polling.HubEventPolling;

import hudson.remoting.VirtualChannel;

public class RemoteBomGenerator extends RiskReportGenerator {

    private final VirtualChannel channel;

    public RemoteBomGenerator(final HubReportGenerationInfo hubReportGenerationInfo,
            final HubSupportHelper supportHelper, final VirtualChannel channel) {
        super(hubReportGenerationInfo, supportHelper);
        this.channel = channel;
    }

    public VirtualChannel getChannel() {
        return channel;
    }

    @Override
    public HubEventPolling getHubEventPolling(final HubIntRestService service) {
        return new RemoteHubEventPolling(service, getChannel());
    }

}
