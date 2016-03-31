package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.jenkins.HubServerInfo;
import com.blackducksoftware.integration.hub.report.api.HubReportGenerationInfo;
import com.blackducksoftware.integration.hub.report.api.HubRiskReportData;
import com.blackducksoftware.integration.hub.report.api.RiskReportGenerator;

public class RemoteBomGenerator implements Callable<HubRiskReportData, Exception> {
	private static final long serialVersionUID = 3459269768733083577L;

	private final HubJenkinsLogger logger;

	private final HubServerInfo serverInfo;

	private final String proxyHost;

	private final int proxyPort;

	private final String proxyUsername;

	private final String proxyPassword;

	private final HubReportGenerationInfo reportGenInfo;

	private final HubSupportHelper hubSupport;


	public RemoteBomGenerator(final HubJenkinsLogger logger,
			final HubServerInfo serverInfo, final String proxyHost, final int proxyPort,
			final String proxyUsername, final String proxyPassword,
			final HubReportGenerationInfo reportGenInfo, final HubSupportHelper hubSupport) {
		this.logger = logger;
		this.serverInfo = serverInfo;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;
		this.reportGenInfo = reportGenInfo;
		this.hubSupport = hubSupport;
	}

	@Override
	public HubRiskReportData call() throws Exception {

		final HubIntRestService service = new HubIntRestService(serverInfo.getServerUrl());


		final HubReportGenerationInfo reportGenInfo = new HubReportGenerationInfo();
		final RiskReportGenerator reportGenerator = new RiskReportGenerator(reportGenInfo, hubSupport);

		return reportGenerator.generateHubReport(logger);
	}

	@Override
	public void checkRoles(final RoleChecker checker) throws SecurityException {
		checker.check(this, new Role(RemoteBomGenerator.class));
	}
}
