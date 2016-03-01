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

    private int vulnerabilityRiskHighCount = 0;

    private int vulnerabilityRiskMediumCount = 0;

    private int vulnerabilityRiskLowCount = 0;

    private int vulnerabilityRiskNoneCount = 0;

    private int licenseRiskHighCount = 0;

    private int licenseRiskMediumCount = 0;

    private int licenseRiskLowCount = 0;

    private int licenseRiskNoneCount = 0;

    private int operationalRiskHighCount = 0;

    private int operationalRiskMediumCount = 0;

    private int operationalRiskLowCount = 0;

    private int operationalRiskNoneCount = 0;

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

    public void setVulnerabilityRiskHighCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getVulnerabilityRisk().getHIGH() > 0) {
                count += 1;
            }
        }
        vulnerabilityRiskHighCount = count;
    }

    public void setVulnerabilityRiskMediumCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getVulnerabilityRisk().getMEDIUM() > 0) {
                count += 1;
            }
        }
        vulnerabilityRiskMediumCount = count;
    }

    public void setVulnerabilityRiskLowCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getVulnerabilityRisk().getLOW() > 0) {
                count += 1;
            }
        }
        vulnerabilityRiskLowCount = count;
    }

    public void setVulnerabilityRiskNoneCount() {
        int vulnerableEntries = 0;
        vulnerableEntries += getVulnerabilityRiskHighCount();
        vulnerableEntries += getVulnerabilityRiskMediumCount();
        vulnerableEntries += getVulnerabilityRiskLowCount();
        int totalCount = getBomEntries().size();

        vulnerabilityRiskNoneCount = totalCount - vulnerableEntries;
    }

    public void setLicenseRiskHighCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getLicenseRisk().getHIGH() > 0) {
                count += 1;
            }
        }
        licenseRiskHighCount = count;
    }

    public void setLicenseRiskMediumCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getLicenseRisk().getMEDIUM() > 0) {
                count += 1;
            }
        }
        licenseRiskMediumCount = count;
    }

    public void setLicenseRiskLowCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getLicenseRisk().getLOW() > 0) {
                count += 1;
            }
        }
        licenseRiskLowCount = count;
    }

    public void setLicenseRiskNoneCount() {
        int vulnerableEntries = 0;
        vulnerableEntries += getLicenseRiskHighCount();
        vulnerableEntries += getLicenseRiskMediumCount();
        vulnerableEntries += getLicenseRiskLowCount();
        int totalCount = getBomEntries().size();

        licenseRiskNoneCount = totalCount - vulnerableEntries;
    }

    public void setOperationalRiskHighCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getOperationalRisk().getHIGH() > 0) {
                count += 1;
            }
        }
        operationalRiskHighCount = count;
    }

    public void setOperationalRiskMediumCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getOperationalRisk().getMEDIUM() > 0) {
                count += 1;
            }
        }
        operationalRiskMediumCount = count;
    }

    public void setOperationalRiskLowCount() {
        int count = 0;
        for (AggregateBomViewEntry bomEntry : getBomEntries()) {
            if (bomEntry.getOperationalRisk().getLOW() > 0) {
                count += 1;
            }
        }
        operationalRiskLowCount = count;
    }

    public void setOperationalRiskNoneCount() {
        int vulnerableEntries = 0;
        vulnerableEntries += getOperationalRiskHighCount();
        vulnerableEntries += getOperationalRiskMediumCount();
        vulnerableEntries += getOperationalRiskLowCount();
        int totalCount = getBomEntries().size();

        operationalRiskNoneCount = totalCount - vulnerableEntries;
    }

    // /////////////////////////////////////////////////

    public int getVulnerabilityRiskHighCount() {
        return vulnerabilityRiskHighCount;
    }

    public int getVulnerabilityRiskMediumCount() {
        return vulnerabilityRiskMediumCount;
    }

    public int getVulnerabilityRiskLowCount() {
        return vulnerabilityRiskLowCount;
    }

    public int getVulnerabilityRiskNoneCount() {
        return vulnerabilityRiskNoneCount;
    }

    public int getLicenseRiskHighCount() {
        return licenseRiskHighCount;
    }

    public int getLicenseRiskMediumCount() {
        return licenseRiskMediumCount;
    }

    public int getLicenseRiskLowCount() {
        return licenseRiskLowCount;
    }

    public int getLicenseRiskNoneCount() {
        return licenseRiskNoneCount;
    }

    public int getOperationalRiskHighCount() {
        return operationalRiskHighCount;
    }

    public int getOperationalRiskMediumCount() {
        return operationalRiskMediumCount;
    }

    public int getOperationalRiskLowCount() {
        return operationalRiskLowCount;
    }

    public int getOperationalRiskNoneCount() {
        return operationalRiskNoneCount;
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
        setVulnerabilityRiskHighCount();
        setVulnerabilityRiskMediumCount();
        setVulnerabilityRiskLowCount();
        setVulnerabilityRiskNoneCount();
        setLicenseRiskHighCount();
        setLicenseRiskMediumCount();
        setLicenseRiskLowCount();
        setLicenseRiskNoneCount();
        setOperationalRiskHighCount();
        setOperationalRiskMediumCount();
        setOperationalRiskLowCount();
        setOperationalRiskNoneCount();
    }

    @Override
    public String getIconFileName() {
        return "/plugin/hub-jenkins/images/blackduck.png";
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
