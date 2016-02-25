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

    public int getVulnerabilityRiskHighCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getVulnerabilityRisk().getHIGH() > 0) {
                count += 1;
            }
        }
        return count;
    }

    public int getVulnerabilityRiskMediumCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getVulnerabilityRisk().getMEDIUM() > 0) {
                count += 1;
            }
        }
        return count;
    }

    public int getVulnerabilityRiskLowCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getVulnerabilityRisk().getLOW() > 0) {
                count += 1;
            }
        }
        return count;
    }

    public int getVulnerabilityRiskNoneCount() {
        int vulnerableEntries = 0;
        vulnerableEntries += getVulnerabilityRiskHighCount();
        vulnerableEntries += getVulnerabilityRiskMediumCount();
        vulnerableEntries += getVulnerabilityRiskLowCount();
        int totalCount = getBomEntries().size();

        return totalCount - vulnerableEntries;
    }

    public int getLicenseRiskHighCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getLicenseRisk().getHIGH() > 0) {
                count += 1;
            }
        }
        return count;
    }

    public int getLicenseRiskMediumCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getLicenseRisk().getMEDIUM() > 0) {
                count += 1;
            }
        }
        return count;
    }

    public int getLicenseRiskLowCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getLicenseRisk().getLOW() > 0) {
                count += 1;
            }
        }
        return count;
    }

    public int getLicenseRiskNoneCount() {
        int vulnerableEntries = 0;
        vulnerableEntries += getLicenseRiskHighCount();
        vulnerableEntries += getLicenseRiskMediumCount();
        vulnerableEntries += getLicenseRiskLowCount();
        int totalCount = getBomEntries().size();

        return totalCount - vulnerableEntries;
    }

    public int getOperationalRiskHighCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getOperationalRisk().getHIGH() > 0) {
                count += 1;
            }
        }
        return count;
    }

    public int getOperationalRiskMediumCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getOperationalRisk().getMEDIUM() > 0) {
                count += 1;
            }
        }
        return count;
    }

    public int getOperationalRiskLowCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getOperationalRisk().getLOW() > 0) {
                count += 1;
            }
        }
        return count;
    }

    public int getOperationalRiskNoneCount() {
        int vulnerableEntries = 0;
        vulnerableEntries += getOperationalRiskHighCount();
        vulnerableEntries += getOperationalRiskMediumCount();
        vulnerableEntries += getOperationalRiskLowCount();
        int totalCount = getBomEntries().size();

        return totalCount - vulnerableEntries;
    }

    public double getPercentage(double count) {
        double totalCount = getBomEntries().size();
        double percentage = 0;
        if (totalCount > 0 && count > 0) {
            percentage = (count / totalCount) * 100;
        }
        return percentage;
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
