package com.blackducksoftware.integration.hub.jenkins;

import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.util.StringUtils;
import org.restlet.Response;
import org.restlet.resource.ClientResource;

import com.blackducksoftware.integration.hub.jenkins.exceptions.BDRestException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ScanLocationHandler {

    private final BuildListener listener;

    public ScanLocationHandler(BuildListener listener) {
        this.listener = listener;
    }

    /**
     * Will attempt to retrieve the scanLocation with the current hostname and target path. If this cannot be found then
     *
     * @param targetPath
     * @param versionId
     * @return
     * @throws UnknownHostException
     * @throws MalformedURLException
     * @throws InterruptedException
     * @throws BDRestException
     */
    public void getScanLocationIdWithRetry(AbstractBuild build, ClientResource resource, String remoteTargetPath, String versionId,
            Map<String, Boolean> scanLocationIds)
            throws UnknownHostException, MalformedURLException, InterruptedException, BDRestException {

        if (resource == null) {
            throw new IllegalArgumentException("Need to provide a ClientResource in order to get the ScanLocation");
        }
        if (StringUtils.isEmpty(remoteTargetPath)) {
            throw new IllegalArgumentException("Need to provide the targetPath of the ScanLocation.");
        }
        if (StringUtils.isEmpty(versionId)) {
            throw new IllegalArgumentException("Need to provide the versionId to make sure the mapping hasn't alredy been done.");
        }
        boolean matchFound = false;
        long start = System.currentTimeMillis();
        int i = 0;
        while (!matchFound) {
            if (build.getResult().equals(Result.ABORTED)) {
                throw new BDRestException("Can not find the Scan Location, the current Build was aborted. Try again later.");
            }

            i++;

            listener.getLogger().println("Attempt # " + i + " to get the Scan Location for : '" + remoteTargetPath + "'.");

            matchFound = scanLocationRetrieval(resource, remoteTargetPath, versionId, scanLocationIds);
            if (matchFound) {
                break;
            }
            long end = System.currentTimeMillis() - start;
            if (end > 300 * 1000) { // This should check if the loop has been running for 5 minutes. If it has, the
                // exception is thrown.
                throw new BDRestException("Can not find the Scan Location after 5 minutes. Try again later.");
            }
            long minutes = TimeUnit.MILLISECONDS.toMinutes(end);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(end) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(end));

            long milliSeconds = end - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(end));

            String time = String.format("%02dm %02ds %dms", minutes, seconds, milliSeconds);
            listener.getLogger().println("Scan Location retrieval running for : " + time);
            Thread.sleep(5000); // Check every 5 seconds
        }

    }

    private boolean scanLocationRetrieval(ClientResource resource, String remoteTargetPath, String versionId, Map<String, Boolean> scanLocationIds) {
        boolean matchFound = false;
        resource.get();

        int responseCode = resource.getResponse().getStatus().getCode();
        try {
            HashMap<String, Object> responseMap = null;
            if (responseCode == 200 || responseCode == 204 || responseCode == 202) {
                Response resp = resource.getResponse();
                Reader reader = resp.getEntity().getReader();
                BufferedReader bufReader = new BufferedReader(reader);
                StringBuilder sb = new StringBuilder();
                String line = bufReader.readLine();
                while (line != null) {
                    sb.append(line + "\n");
                    line = bufReader.readLine();
                }
                bufReader.close();
                byte[] mapData = sb.toString().getBytes();

                // Create HashMap from the Rest response
                ObjectMapper responseMapper = new ObjectMapper();
                responseMap = responseMapper.readValue(mapData, HashMap.class);
            } else {
                throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
            }

            if (responseMap.containsKey("items") && ((ArrayList<LinkedHashMap<String, Object>>) responseMap.get("items")).size() > 0) {
                ArrayList<LinkedHashMap<String, Object>> scanMatchesList = (ArrayList<LinkedHashMap<String, Object>>) responseMap.get("items");
                // More than one match found
                String path = null;
                if (scanMatchesList.size() > 1) {
                    for (LinkedHashMap<String, Object> scanMatch : scanMatchesList) {

                        path = ((String) scanMatch.get("path")).trim();

                        // Remove trailing slash from both strings
                        if (path.endsWith("/")) {
                            path = path.substring(0, path.length() - 1);
                        }
                        if (remoteTargetPath.endsWith("/")) {
                            remoteTargetPath = remoteTargetPath.substring(0, remoteTargetPath.length() - 1);
                        }

                        listener.getLogger().println("[DEBUG] Comparing target : '" + remoteTargetPath + "' with path : '" + path + "'.");
                        if (remoteTargetPath.equals(path)) {
                            listener.getLogger().println("[DEBUG] MATCHED!");
                            matchFound = true;
                            handleScanLocationMatch(scanLocationIds, scanMatch, remoteTargetPath, versionId);
                            break;
                        }
                    }
                } else if (scanMatchesList.size() == 1) {
                    LinkedHashMap<String, Object> scanMatch = scanMatchesList.get(0);
                    path = (String) scanMatch.get("path");

                    // Remove trailing slash from both strings
                    if (path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }
                    if (remoteTargetPath.endsWith("/")) {
                        remoteTargetPath = remoteTargetPath.substring(0, remoteTargetPath.length() - 1);
                    }

                    listener.getLogger().println("[DEBUG] Comparing target : '" + remoteTargetPath + "' with path : '" + path + "'.");
                    // trim the path, this way there should be no whitespaces to intefere with the comparison
                    if (remoteTargetPath.equals(path.trim())) {
                        listener.getLogger().println("[DEBUG] MATCHED!");
                        matchFound = true;
                        handleScanLocationMatch(scanLocationIds, scanMatch, remoteTargetPath, versionId);
                    }
                }
                // if (scanId != null) {
                // return scanId;
                //
                // } else {
                // if (!alreadyMapped) {
                // // TODO perform retry until the scan location is available
                // listener.getLogger().println(
                // "[ERROR] No Scan Location Id could be found for the scan target : '" + targetPath + "'.");
                //
                // }
                // }
            } else {
                listener.getLogger().println(
                        "[ERROR] No Scan Location Id could be found for the scan target : '" + remoteTargetPath + "'.");
            }
        } catch (IOException e) {
            e.printStackTrace(listener.getLogger());
        } catch (BDRestException e) {
            e.printStackTrace(listener.getLogger());
        }

        return matchFound;
    }

    private void handleScanLocationMatch(Map<String, Boolean> scanLocationIds, LinkedHashMap<String, Object> scanMatch, String targetPath, String versionId) {
        ArrayList<LinkedHashMap<String, Object>> assetReferences = (ArrayList<LinkedHashMap<String, Object>>) scanMatch
                .get("assetReferenceList");
        if (!assetReferences.isEmpty()) {
            for (LinkedHashMap<String, Object> assetReference : assetReferences) {
                LinkedHashMap<String, Object> ownerEntity = (LinkedHashMap<String, Object>) assetReference.get("ownerEntityKey");
                String ownerId = (String) ownerEntity.get("entityId");
                if (ownerId.equals(versionId)) {
                    String scanId = (String) scanMatch.get("id");
                    scanLocationIds.put(scanId, true);
                    listener.getLogger().println(
                            "[DEBUG] The scan target : '"
                                    + targetPath
                                    + "' has Scan Location Id: '"
                                    + scanId
                                    + "'. This is already mapped to the Version with Id: '"
                                    + versionId + "'.");
                    listener.getLogger().println();
                } else {
                    String scanId = (String) scanMatch.get("id");
                    listener.getLogger().println(
                            "[DEBUG] The scan target : '" + targetPath + "' has Scan Location Id: '" + scanId + "'.");
                    scanLocationIds.put(scanId, false);
                }
            }

        } else {
            String scanId = (String) scanMatch.get("id");
            listener.getLogger().println(
                    "[DEBUG] The scan target : '" + targetPath + "' has Scan Location Id: '" + scanId + "'.");
            scanLocationIds.put(scanId, false);
        }

    }

}
