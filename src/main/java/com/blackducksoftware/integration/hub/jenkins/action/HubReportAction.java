package com.blackducksoftware.integration.hub.jenkins.action;

import hudson.model.Action;
import hudson.model.AbstractBuild;

import java.util.List;

import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.report.api.AggregateBomViewEntry;
import com.blackducksoftware.integration.hub.report.api.DetailedReleaseSummary;
import com.blackducksoftware.integration.hub.report.api.VersionReport;

public class HubReportAction implements Action {

    private final AbstractBuild<?, ?> build;

    private VersionReport report;

    public HubReportAction(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    public VersionReport getReport() {
        return report;
    }

    public DetailedReleaseSummary getReleaseSummary() {
        if (report == null) {
            return null;
        }
        return report.getDetailedReleaseSummary();
    }

    public List<AggregateBomViewEntry> getBomEntries() {
        if (report == null) {
            return null;
        }
        return report.getAggregateBomViewEntries();
    }

    public void setReport(VersionReport report) {
        this.report = report;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/protex-jenkins/images/blackduck.png";
    }

    @Override
    public String getDisplayName() {
        return Messages.HubReportAction_getDisplayName();
    }

    @Override
    public String getUrlName() {
        // FIXME suggestions for better Url?
        return "hub_governance_report";
    }

}
