package com.blackducksoftware.integration.hub.jenkins.bom;

import hudson.remoting.VirtualChannel;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.polling.HubEventPolling;
import com.blackducksoftware.integration.hub.report.api.HubReportGenerationInfo;
import com.blackducksoftware.integration.hub.report.api.RiskReportGenerator;

public class RemoteBomGenerator extends RiskReportGenerator {

	private final VirtualChannel channel;

	public RemoteBomGenerator(final HubReportGenerationInfo hubReportGenerationInfo,
			final HubSupportHelper supportHelper,final VirtualChannel channel) {
		super(hubReportGenerationInfo, supportHelper);
		this.channel = channel;
	}

	public VirtualChannel getChannel() {
		return channel;
	}

	@Override
	public HubEventPolling getHubEventPolling(final HubIntRestService service){
		return new RemoteHubEventPolling(service, getChannel());
	}


}
