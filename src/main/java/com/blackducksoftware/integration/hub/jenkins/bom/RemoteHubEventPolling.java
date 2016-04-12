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
package com.blackducksoftware.integration.hub.jenkins.bom;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.logging.IntLogger;
import com.blackducksoftware.integration.hub.polling.HubEventPolling;
import com.blackducksoftware.integration.hub.polling.ScanStatusChecker;
import com.blackducksoftware.integration.hub.report.api.HubReportGenerationInfo;
import com.blackducksoftware.integration.hub.scan.status.ScanStatusToPoll;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RemoteHubEventPolling extends HubEventPolling {

	private final VirtualChannel channel;

	public RemoteHubEventPolling(final HubIntRestService service, final VirtualChannel channel) {
		super(service);
		this.channel = channel;
	}

	public VirtualChannel getChannel() {
		return channel;
	}

	/**
	 * Checks the status's in the scan files and polls their URL's, every 10 seconds,
	 * until they have all have status COMPLETE. We keep trying until we hit the maximum wait time.
	 * If we find a scan history object that has status cancelled or an error type then we throw an exception.
	 */
	@Override
	public void assertBomUpToDate(final HubReportGenerationInfo hubReportGenerationInfo, final IntLogger logger) throws InterruptedException,
	BDRestException, HubIntegrationException, URISyntaxException, IOException {
		if (StringUtils.isBlank(hubReportGenerationInfo.getScanStatusDirectory())) {
			throw new HubIntegrationException("The scan status directory must be a non empty value.");
		}
		final FilePath statusDirectory = new FilePath(getChannel(),hubReportGenerationInfo.getScanStatusDirectory());
		if (!statusDirectory.exists()) {
			throw new HubIntegrationException("The scan status directory does not exist.");
		}
		if (!statusDirectory.isDirectory()) {
			throw new HubIntegrationException("The scan status directory provided is not a directory.");
		}
		final List<FilePath> statusFiles = statusDirectory.list();
		if (statusFiles == null || statusFiles.size() == 0) {
			throw new HubIntegrationException("Can not find the scan status files in the directory provided.");
		}
		int expectedNumScans = 0;
		if (hubReportGenerationInfo.getScanTargets() != null && !hubReportGenerationInfo.getScanTargets().isEmpty()) {
			expectedNumScans = hubReportGenerationInfo.getScanTargets().size();
		}
		if (statusFiles.size() != expectedNumScans) {
			throw new HubIntegrationException("There were " + expectedNumScans + " scans configured and we found " + statusFiles.size() + " status files.");
		}
		logger.info("Checking the directory : " + statusDirectory.getRemote() + " for the scan status's.");
		final CountDownLatch lock = new CountDownLatch(expectedNumScans);
		final List<ScanStatusChecker> scanStatusList = new ArrayList<ScanStatusChecker>();
		for (final FilePath currentStatusFile : statusFiles) {
			final String fileContent = currentStatusFile.readToString();
			final Gson gson = new GsonBuilder().create();
			final ScanStatusToPoll status = gson.fromJson(fileContent, ScanStatusToPoll.class);
			if (status.get_meta() == null || status.getStatus() == null) {
				throw new HubIntegrationException("The scan status file : " + currentStatusFile.getRemote()
						+ " does not contain valid scan status json.");
			}
			final ScanStatusChecker checker = new ScanStatusChecker(getService(), status, lock);
			scanStatusList.add(checker);
		}

		logger.debug("Cleaning up the scan status files at : " + statusDirectory.getRemote());
		// We delete the files in a second loop to ensure we have all the scan status's in memory before we start
		// deleting the files. This way, if there is an exception thrown, the User can go look at the files to see what
		// went wrong.
		for (final FilePath currentStatusFile : statusFiles) {
			currentStatusFile.delete();
		}
		statusDirectory.delete();

		pollScanStatusChecker(lock,hubReportGenerationInfo,scanStatusList);
	}

}
