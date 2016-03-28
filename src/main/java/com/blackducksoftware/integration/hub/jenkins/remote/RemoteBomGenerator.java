package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;
import com.blackducksoftware.integration.hub.report.api.HubReportGenerationInfo;
import com.blackducksoftware.integration.hub.report.api.HubRiskReportData;
import com.blackducksoftware.integration.hub.report.api.RiskReportGenerator;

public class RemoteBomGenerator implements Callable<HubRiskReportData, Exception> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final HubJenkinsLogger logger;

    private final HubReportGenerationInfo reportGenInfo;

    private final HubSupportHelper hubSupport;

    public RemoteBomGenerator(HubJenkinsLogger logger, HubReportGenerationInfo reportGenInfo,
            HubSupportHelper hubSupport) {
        this.logger = logger;
        this.reportGenInfo = reportGenInfo;
        this.hubSupport = hubSupport;
    }

    @Override
    public HubRiskReportData call() throws Exception {
        RiskReportGenerator reportGenerator = new RiskReportGenerator(reportGenInfo, hubSupport);

        return reportGenerator.generateHubReport(logger);
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(RemoteBomGenerator.class));
    }
}
