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
package com.blackducksoftware.integration.hub.jenkins.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.report.api.AggregateBomViewEntry;
import com.blackducksoftware.integration.hub.report.api.HubRiskReportData;
import com.blackducksoftware.integration.hub.report.api.VersionReport;
import com.blackducksoftware.integration.hub.report.risk.api.RiskCategories;
import com.blackducksoftware.integration.hub.report.risk.api.RiskCounts;
import com.blackducksoftware.integration.hub.report.risk.api.RiskProfile;

public class HubReportActionTest {

	@Test
	public void testSetReportHighRisks() {
		final RiskCounts counts = new RiskCounts(1, 0, 0, 0, 0);
		final RiskCategories categories = new RiskCategories(counts, counts, counts, counts, counts);
		final RiskProfile riskProfile = new RiskProfile(0, categories);
		final AggregateBomViewEntry bomEntry = new AggregateBomViewEntry(null, null, null, null, null, null, null, null,
				null, null, null, null, riskProfile, null);
		final List<AggregateBomViewEntry> aggregateBomViewEntries = new ArrayList<AggregateBomViewEntry>();
		aggregateBomViewEntries.add(bomEntry);
		final HubRiskReportData reportData = new HubRiskReportData();
		final VersionReport report = new VersionReport(null, aggregateBomViewEntries);
		reportData.setReport(report);
		final HubReportAction action = new HubReportAction(null);
		action.setReportData(reportData);
		assertNotNull(action.getBomEntries());
		assertEquals(1, action.getVulnerabilityRiskHighCount());
		assertEquals(0, action.getVulnerabilityRiskMediumCount());
		assertEquals(0, action.getVulnerabilityRiskLowCount());
		assertEquals(0, action.getVulnerabilityRiskNoneCount());

		assertEquals(1, action.getLicenseRiskHighCount());
		assertEquals(0, action.getLicenseRiskMediumCount());
		assertEquals(0, action.getLicenseRiskLowCount());
		assertEquals(0, action.getLicenseRiskNoneCount());

		assertEquals(1, action.getOperationalRiskHighCount());
		assertEquals(0, action.getOperationalRiskMediumCount());
		assertEquals(0, action.getOperationalRiskLowCount());
		assertEquals(0, action.getOperationalRiskNoneCount());
	}

	@Test
	public void testSetReportSecurityHighRisks() {
		final RiskCounts counts = new RiskCounts(1, 1, 1, 1, 1);
		final RiskCategories categories = new RiskCategories(counts, counts, counts, counts, counts);
		final RiskProfile riskProfile = new RiskProfile(0, categories);
		final AggregateBomViewEntry bomEntry = new AggregateBomViewEntry(null, null, null, null, null, null, null, null,
				null, null, null, null, riskProfile, null);
		final List<AggregateBomViewEntry> aggregateBomViewEntries = new ArrayList<AggregateBomViewEntry>();
		aggregateBomViewEntries.add(bomEntry);
		final HubRiskReportData reportData = new HubRiskReportData();
		final VersionReport report = new VersionReport(null, aggregateBomViewEntries);
		reportData.setReport(report);
		final HubReportAction action = new HubReportAction(null);
		action.setReportData(reportData);
		assertNotNull(action.getBomEntries());
		assertEquals(1, action.getVulnerabilityRiskHighCount());
		assertEquals(0, action.getVulnerabilityRiskMediumCount());
		assertEquals(0, action.getVulnerabilityRiskLowCount());
		assertEquals(0, action.getVulnerabilityRiskNoneCount());
	}

	@Test
	public void testSetReportSecurityMediumRisks() {
		final RiskCounts counts = new RiskCounts(0, 1, 1, 1, 1);
		final RiskCategories categories = new RiskCategories(counts, counts, counts, counts, counts);
		final RiskProfile riskProfile = new RiskProfile(0, categories);
		final AggregateBomViewEntry bomEntry = new AggregateBomViewEntry(null, null, null, null, null, null, null, null,
				null, null, null, null, riskProfile, null);
		final List<AggregateBomViewEntry> aggregateBomViewEntries = new ArrayList<AggregateBomViewEntry>();
		aggregateBomViewEntries.add(bomEntry);
		final HubRiskReportData reportData = new HubRiskReportData();
		final VersionReport report = new VersionReport(null, aggregateBomViewEntries);
		reportData.setReport(report);
		final HubReportAction action = new HubReportAction(null);
		action.setReportData(reportData);
		assertNotNull(action.getBomEntries());
		assertEquals(0, action.getVulnerabilityRiskHighCount());
		assertEquals(1, action.getVulnerabilityRiskMediumCount());
		assertEquals(0, action.getVulnerabilityRiskLowCount());
		assertEquals(0, action.getVulnerabilityRiskNoneCount());
	}

	@Test
	public void testSetReportMediumRisks() {
		final RiskCounts counts = new RiskCounts(0, 1, 0, 0, 0);
		final RiskCategories categories = new RiskCategories(counts, counts, counts, counts, counts);
		final RiskProfile riskProfile = new RiskProfile(0, categories);
		final AggregateBomViewEntry bomEntry = new AggregateBomViewEntry(null, null, null, null, null, null, null, null,
				null, null, null, null, riskProfile, null);
		final List<AggregateBomViewEntry> aggregateBomViewEntries = new ArrayList<AggregateBomViewEntry>();
		aggregateBomViewEntries.add(bomEntry);
		final HubRiskReportData reportData = new HubRiskReportData();
		final VersionReport report = new VersionReport(null, aggregateBomViewEntries);
		reportData.setReport(report);
		final HubReportAction action = new HubReportAction(null);
		action.setReportData(reportData);
		assertNotNull(action.getBomEntries());
		assertEquals(0, action.getVulnerabilityRiskHighCount());
		assertEquals(1, action.getVulnerabilityRiskMediumCount());
		assertEquals(0, action.getVulnerabilityRiskLowCount());
		assertEquals(0, action.getVulnerabilityRiskNoneCount());

		assertEquals(0, action.getLicenseRiskHighCount());
		assertEquals(1, action.getLicenseRiskMediumCount());
		assertEquals(0, action.getLicenseRiskLowCount());
		assertEquals(0, action.getLicenseRiskNoneCount());

		assertEquals(0, action.getOperationalRiskHighCount());
		assertEquals(1, action.getOperationalRiskMediumCount());
		assertEquals(0, action.getOperationalRiskLowCount());
		assertEquals(0, action.getOperationalRiskNoneCount());
	}

	@Test
	public void testSetReportLowRisks() {
		final RiskCounts counts = new RiskCounts(0, 0, 1, 0, 0);
		final RiskCategories categories = new RiskCategories(counts, counts, counts, counts, counts);
		final RiskProfile riskProfile = new RiskProfile(0, categories);
		final AggregateBomViewEntry bomEntry = new AggregateBomViewEntry(null, null, null, null, null, null, null, null,
				null, null, null, null, riskProfile, null);
		final List<AggregateBomViewEntry> aggregateBomViewEntries = new ArrayList<AggregateBomViewEntry>();
		aggregateBomViewEntries.add(bomEntry);
		final HubRiskReportData reportData = new HubRiskReportData();
		final VersionReport report = new VersionReport(null, aggregateBomViewEntries);
		reportData.setReport(report);
		final HubReportAction action = new HubReportAction(null);
		action.setReportData(reportData);
		assertNotNull(action.getBomEntries());
		assertEquals(0, action.getVulnerabilityRiskHighCount());
		assertEquals(0, action.getVulnerabilityRiskMediumCount());
		assertEquals(1, action.getVulnerabilityRiskLowCount());
		assertEquals(0, action.getVulnerabilityRiskNoneCount());

		assertEquals(0, action.getLicenseRiskHighCount());
		assertEquals(0, action.getLicenseRiskMediumCount());
		assertEquals(1, action.getLicenseRiskLowCount());
		assertEquals(0, action.getLicenseRiskNoneCount());

		assertEquals(0, action.getOperationalRiskHighCount());
		assertEquals(0, action.getOperationalRiskMediumCount());
		assertEquals(1, action.getOperationalRiskLowCount());
		assertEquals(0, action.getOperationalRiskNoneCount());
	}

	@Test
	public void testSetReportNoneRisks() {
		final RiskCounts counts = new RiskCounts(0, 0, 0, 0, 0);
		final RiskCategories categories = new RiskCategories(counts, counts, counts, counts, counts);
		final RiskProfile riskProfile = new RiskProfile(0, categories);
		final AggregateBomViewEntry bomEntry = new AggregateBomViewEntry(null, null, null, null, null, null, null, null,
				null, null, null, null, riskProfile, null);
		final List<AggregateBomViewEntry> aggregateBomViewEntries = new ArrayList<AggregateBomViewEntry>();
		aggregateBomViewEntries.add(bomEntry);
		final HubRiskReportData reportData = new HubRiskReportData();
		final VersionReport report = new VersionReport(null, aggregateBomViewEntries);
		reportData.setReport(report);
		final HubReportAction action = new HubReportAction(null);
		action.setReportData(reportData);
		assertNotNull(action.getBomEntries());
		assertEquals(0, action.getVulnerabilityRiskHighCount());
		assertEquals(0, action.getVulnerabilityRiskMediumCount());
		assertEquals(0, action.getVulnerabilityRiskLowCount());
		assertEquals(1, action.getVulnerabilityRiskNoneCount());

		assertEquals(0, action.getLicenseRiskHighCount());
		assertEquals(0, action.getLicenseRiskMediumCount());
		assertEquals(0, action.getLicenseRiskLowCount());
		assertEquals(1, action.getLicenseRiskNoneCount());

		assertEquals(0, action.getOperationalRiskHighCount());
		assertEquals(0, action.getOperationalRiskMediumCount());
		assertEquals(0, action.getOperationalRiskLowCount());
		assertEquals(1, action.getOperationalRiskNoneCount());
	}

	@Test
	public void testGetPercentageWholeNumbers() {
		final RiskCounts counts = new RiskCounts(1, 0, 0, 0, 0);
		final RiskCategories categories = new RiskCategories(counts, counts, counts, counts, counts);
		final RiskProfile riskProfile = new RiskProfile(0, categories);
		final AggregateBomViewEntry bomEntry = new AggregateBomViewEntry(null, null, null, null, null, null, null, null,
				null, null, null, null, riskProfile, null);
		final List<AggregateBomViewEntry> aggregateBomViewEntries = new ArrayList<AggregateBomViewEntry>();
		aggregateBomViewEntries.add(bomEntry);
		final HubRiskReportData reportData = new HubRiskReportData();
		final VersionReport report = new VersionReport(null, aggregateBomViewEntries);
		reportData.setReport(report);
		final HubReportAction action = new HubReportAction(null);
		action.setReportData(reportData);
		assertNotNull(action.getBomEntries());

		assertTrue(0.0 == action.getPercentage(0.0));
		assertTrue(100.0 == action.getPercentage(1.0));
	}

	@Test
	public void testGetPercentage() {
		final RiskCounts counts = new RiskCounts(1, 0, 0, 0, 0);
		final RiskCategories categories = new RiskCategories(counts, counts, counts, counts, counts);
		final RiskProfile riskProfile = new RiskProfile(0, categories);
		final AggregateBomViewEntry bomEntry = new AggregateBomViewEntry(null, null, null, null, null, null, null, null,
				null, null, null, null, riskProfile, null);
		final List<AggregateBomViewEntry> aggregateBomViewEntries = new ArrayList<AggregateBomViewEntry>();
		aggregateBomViewEntries.add(bomEntry);
		aggregateBomViewEntries.add(bomEntry);
		aggregateBomViewEntries.add(bomEntry);
		aggregateBomViewEntries.add(bomEntry);
		final HubRiskReportData reportData = new HubRiskReportData();
		final VersionReport report = new VersionReport(null, aggregateBomViewEntries);
		reportData.setReport(report);
		final HubReportAction action = new HubReportAction(null);
		action.setReportData(reportData);
		assertNotNull(action.getBomEntries());

		assertTrue(0.0 == action.getPercentage(0.0));
		assertTrue(25.0 == action.getPercentage(1.0));
		assertTrue(50.0 == action.getPercentage(2.0));
		assertTrue(75.0 == action.getPercentage(3.0));
		assertTrue(100.0 == action.getPercentage(4.0));

	}

	@Test
	public void testGetIconFileName() {
		final HubReportAction action = new HubReportAction(null);
		assertEquals("/plugin/hub-jenkins/images/blackduck.png", action.getIconFileName());
	}

	@Test
	public void testGetDisplayName() {
		final HubReportAction action = new HubReportAction(null);
		assertEquals(Messages.HubReportAction_getDisplayName(), action.getDisplayName());
	}

	@Test
	public void testGetUrlName() {
		final HubReportAction action = new HubReportAction(null);
		assertEquals("hub_risk_report", action.getUrlName());
	}
}
