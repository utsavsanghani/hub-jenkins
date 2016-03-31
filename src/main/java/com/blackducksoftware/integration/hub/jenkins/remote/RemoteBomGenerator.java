package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.report.api.HubReportGenerationInfo;
import com.blackducksoftware.integration.hub.report.api.HubRiskReportData;
import com.blackducksoftware.integration.hub.report.api.RiskReportGenerator;

public class RemoteBomGenerator implements Callable<HubRiskReportData, Exception> {
	private static final long serialVersionUID = 3459269768733083577L;

	private final HubJenkinsLogger logger;

	private final String hubUrl;

	private final String userName;

	private final String password;

	private final HubReportGenerationInfo reportGenInfo;

	private final HubSupportHelper hubSupport;

	private String proxyHost;

	private int proxyPort;

	private String proxyUsername;

	private String proxyPassword;


	public RemoteBomGenerator(final HubJenkinsLogger logger, final HubReportGenerationInfo reportGenInfo,final String hubUrl,final String userName,final String password,
			final HubSupportHelper hubSupport) {
		this.logger = logger;
		this.reportGenInfo=reportGenInfo;
		this.hubUrl = hubUrl;
		this.userName = userName;
		this.password = password;
		this.hubSupport = hubSupport;
	}

	public void setProxyHost(final String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public void setProxyPort(final int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public void setProxyUsername(final String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	public void setProxyPassword(final String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	@Override
	public HubRiskReportData call() throws Exception {

		final HubIntRestService service = new HubIntRestService(hubUrl);
		if(StringUtils.isNotBlank(proxyHost)){
			if(StringUtils.isNotBlank(proxyUsername) && StringUtils.isNotBlank(proxyPassword)){
				service.setProxyProperties(proxyHost, proxyPort, null, proxyUsername, proxyPassword);
			} else{
				service.setProxyProperties(proxyHost, proxyPort, null, proxyUsername, proxyPassword);
			}
		}
		service.setCookies(userName,password);
		reportGenInfo.setService(service);
		final RiskReportGenerator reportGenerator = new RiskReportGenerator(reportGenInfo, hubSupport);

		return reportGenerator.generateHubReport(logger);
	}

	@Override
	public void checkRoles(final RoleChecker checker) throws SecurityException {
		checker.check(this, new Role(RemoteBomGenerator.class));
	}
}
