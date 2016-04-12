package com.blackducksoftware.integration.hub.jenkins.action;

import java.util.List;

import org.joda.time.DateTime;

import hudson.model.Action;

public class BomUpToDateAction implements Action {

	private boolean hasBomBeenUdpated;

	private String localHostName;

	private List<String> scanTargets;

	private long maxWaitTime;

	private DateTime beforeScanTime;

	private DateTime afterScanTime;

	private String scanStatusDirectory;

	private String policyStatusUrl;

	public boolean isHasBomBeenUdpated() {
		return hasBomBeenUdpated;
	}

	public void setHasBomBeenUdpated(final boolean hasBomBeenUdpated) {
		this.hasBomBeenUdpated = hasBomBeenUdpated;
	}

	public String getLocalHostName() {
		return localHostName;
	}

	public void setLocalHostName(final String localHostName) {
		this.localHostName = localHostName;
	}

	public List<String> getScanTargets() {
		return scanTargets;
	}

	public void setScanTargets(final List<String> scanTargets) {
		this.scanTargets = scanTargets;
	}

	public long getMaxWaitTime() {
		return maxWaitTime;
	}

	public void setMaxWaitTime(final long maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}

	public DateTime getBeforeScanTime() {
		return beforeScanTime;
	}

	public void setBeforeScanTime(final DateTime beforeScanTime) {
		this.beforeScanTime = beforeScanTime;
	}

	public DateTime getAfterScanTime() {
		return afterScanTime;
	}

	public void setAfterScanTime(final DateTime afterScanTime) {
		this.afterScanTime = afterScanTime;
	}

	public String getScanStatusDirectory() {
		return scanStatusDirectory;
	}

	public void setScanStatusDirectory(final String scanStatusDirectory) {
		this.scanStatusDirectory = scanStatusDirectory;
	}

	public String getPolicyStatusUrl() {
		return policyStatusUrl;
	}

	public void setPolicyStatusUrl(final String policyStatusUrl) {
		this.policyStatusUrl = policyStatusUrl;
	}

	@Override
	public String getIconFileName() {
		return null;
	}

	@Override
	public String getDisplayName() {
		return "Temp Action to verify we have already waited for the Bom to finish updating";
	}

	@Override
	public String getUrlName() {
		return null;
	}

}
