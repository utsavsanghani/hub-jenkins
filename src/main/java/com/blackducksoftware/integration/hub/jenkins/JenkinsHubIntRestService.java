package com.blackducksoftware.integration.hub.jenkins;

import hudson.model.BuildListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import net.sf.json.JSONObject;

import org.joda.time.DateTime;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.CookieSetting;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

import com.blackducksoftware.integration.hub.jenkins.exceptions.BDRestException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JenkinsHubIntRestService {
    private Series<Cookie> cookies;

    private String baseUrl;

    protected JenkinsHubIntRestService() {

    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Gets the cookie for the Authorized connection to the Hub server. Returns the response code from the connection.
     * 
     * @param serverUrl
     *            String the Url for the Hub server
     * @param credentialUserName
     *            String the Username for the Hub server
     * @param credentialPassword
     *            String the Password for the Hub server
     * 
     * @return int Status code
     */
    public int setCookies(String credentialUserName, String credentialPassword) {
        Series<Cookie> cookies = getCookies();

        String url = getBaseUrl() + "/j_spring_security_check?j_username=" + credentialUserName + "&j_password=" + credentialPassword;
        ClientResource resource = new ClientResource(url);
        resource.setMethod(Method.POST);

        EmptyRepresentation rep = new EmptyRepresentation();

        resource.post(rep);
        if (cookies == null) {
            Series<CookieSetting> cookieSettings = resource.getResponse().getCookieSettings();
            Series<Cookie> requestCookies = resource.getRequest().getCookies();
            for (CookieSetting ck : cookieSettings) {
                Cookie cookie = new Cookie();
                cookie.setName(ck.getName());
                cookie.setDomain(ck.getDomain());
                cookie.setPath(ck.getPath());
                cookie.setValue(ck.getValue());
                cookie.setVersion(ck.getVersion());
                requestCookies.add(cookie);
            }

            this.cookies = requestCookies;
        } else {
            // cookies already set
        }

        return resource.getResponse().getStatus().getCode();
    }

    public Series<Cookie> getCookies() {
        return cookies;
    }

    public ArrayList<LinkedHashMap<String, Object>> getProjectMatches(String hubProjectName) throws IOException, BDRestException {

        String url = getBaseUrl() + "/api/v1/autocomplete/PROJECT?text=" + hubProjectName + "&limit=20";
        ClientResource resource = new ClientResource(url);

        resource.getRequest().setCookies(cookies);
        resource.setMethod(Method.GET);
        resource.get();
        int responseCode = resource.getResponse().getStatus().getCode();

        ArrayList<LinkedHashMap<String, Object>> list = new ArrayList<LinkedHashMap<String, Object>>();
        if (responseCode == 200 || responseCode == 204 || responseCode == 202) {
            Response resp = resource.getResponse();
            Reader reader = resp.getEntity().getReader();
            BufferedReader bufReader = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufReader.readLine()) != null) {
                sb.append(line + "\n");
            }
            byte[] mapData = sb.toString().getBytes();
            // Create HashMap from the Rest response
            ObjectMapper responseMapper = new ObjectMapper();
            list = responseMapper.readValue(mapData, ArrayList.class);
            // responseMap = responseMapper.readValue(mapData, HashMap.class);
        } else {
            throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
        }
        return list;
    }

    // TODO
    // public String getProjectId(String hubProjectName) throws IOException, BDRestException {
    //
    // String url = getBaseUrl() + "/api/v1/projects/name/" + hubProjectName + "?projectName=" + hubProjectName;
    // ClientResource resource = new ClientResource(url);
    //
    // resource.getRequest().setCookies(cookies);
    // resource.setMethod(Method.GET);
    // resource.get();
    // int responseCode = resource.getResponse().getStatus().getCode();
    //
    // HashMap<String, Object> responseMap = new HashMap<String, Object>();
    // if (responseCode == 200 || responseCode == 204 || responseCode == 202) {
    // Response resp = resource.getResponse();
    // Reader reader = resp.getEntity().getReader();
    // BufferedReader bufReader = new BufferedReader(reader);
    // StringBuilder sb = new StringBuilder();
    // String line;
    // while ((line = bufReader.readLine()) != null) {
    // sb.append(line + "\n");
    // }
    // byte[] mapData = sb.toString().getBytes();
    // // Create HashMap from the Rest response
    // ObjectMapper responseMapper = new ObjectMapper();
    // responseMap = responseMapper.readValue(mapData, HashMap.class);
    // } else {
    // throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
    // }
    // // TODO parse the response and Return the project Id
    // return null;
    // }

    /**
     * Gets the scan Id for each scan target, it searches the list of scans and gets the latest scan id for the scan
     * matching the hostname and path. If the matching scans are already mapped to the Release then that id will not be
     * returned in the list.
     * 
     * @param listener
     *            BuildListener
     * @param scanTargets
     *            List<String>
     * @param releaseId
     *            String
     * 
     * @return List<String> scan Ids
     * @throws UnknownHostException
     */
    public List<String> getScanLocationIds(BuildListener listener, List<String> scanTargets, String releaseId) throws UnknownHostException {
        Series<Cookie> cookies = getCookies();
        String localhostname = InetAddress.getLocalHost().getHostName();
        String url = null;
        ClientResource resource = null;
        List<String> scanIds = new ArrayList<String>();
        for (String targetPath : scanTargets) {
            url = getBaseUrl() + "/api/v1/scanlocations?host=" + localhostname + "&path=" + targetPath;
            resource = new ClientResource(url);

            resource.getRequest().setCookies(cookies);
            resource.setMethod(Method.GET);
            resource.get();

            int responseCode = resource.getResponse().getStatus().getCode();
            try {
                HashMap<String, Object> responseMap = new HashMap<String, Object>();
                if (responseCode == 200 || responseCode == 204 || responseCode == 202) {

                    Response resp = resource.getResponse();
                    Reader reader = resp.getEntity().getReader();
                    BufferedReader bufReader = new BufferedReader(reader);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufReader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
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
                    String scanId = null;
                    String path = null;
                    boolean alreadyMapped = false;
                    if (scanMatchesList.size() > 1) {
                        LinkedHashMap<String, Object> latestScan = null;
                        DateTime lastestScanTime = null;
                        for (LinkedHashMap<String, Object> scanMatch : scanMatchesList) {
                            path = (String) scanMatch.get("path");
                            if (targetPath.equals(path)) {
                                ArrayList<LinkedHashMap<String, Object>> assetReferences = (ArrayList<LinkedHashMap<String, Object>>) scanMatch
                                        .get("assetReferenceList");
                                if (assetReferences.size() > 0) {
                                    for (LinkedHashMap<String, Object> assetReference : assetReferences) {
                                        LinkedHashMap<String, Object> ownerEntity = (LinkedHashMap<String, Object>) assetReference.get("ownerEntityKey");
                                        String ownerId = (String) ownerEntity.get("entityId");
                                        if (ownerId.equals(releaseId)) {
                                            alreadyMapped = true;
                                            listener.getLogger().println(
                                                    "[DEBUG] The scan target : '"
                                                            + targetPath
                                                            + "' has Scan Location Id: '"
                                                            + (String) scanMatch.get("id")
                                                            + "'. This is already mapped to the Release with Id: '"
                                                            + releaseId + "'.");
                                            listener.getLogger().println();
                                        }
                                    }
                                    if (!alreadyMapped) {
                                        scanId = (String) scanMatch.get("id");
                                    }
                                } else {
                                    scanId = (String) scanMatch.get("id");
                                }
                            }
                        }
                    } else if (scanMatchesList.size() == 1) {
                        LinkedHashMap<String, Object> scanMatch = scanMatchesList.get(0);
                        path = (String) scanMatch.get("path");
                        if (targetPath.equals(path)) {
                            ArrayList<LinkedHashMap<String, Object>> assetReferences = (ArrayList<LinkedHashMap<String, Object>>) scanMatch
                                    .get("assetReferenceList");
                            if (assetReferences.size() > 0) {
                                for (LinkedHashMap<String, Object> assetReference : assetReferences) {
                                    LinkedHashMap<String, Object> ownerEntity = (LinkedHashMap<String, Object>) assetReference.get("ownerEntityKey");
                                    String ownerId = (String) ownerEntity.get("entityId");
                                    if (ownerId.equals(releaseId)) {
                                        alreadyMapped = true;
                                        listener.getLogger().println(
                                                "[DEBUG] The scan target : '"
                                                        + targetPath
                                                        + "' has Scan Location Id: '"
                                                        + (String) scanMatch.get("id")
                                                        + "'. This is already mapped to the Release with Id: '"
                                                        + releaseId + "'.");
                                        listener.getLogger().println();
                                    }
                                }
                                if (!alreadyMapped) {
                                    scanId = (String) scanMatch.get("id");
                                }
                            } else {
                                scanId = (String) scanMatch.get("id");
                            }
                        }
                    }
                    if (scanId != null) {
                        if (scanIds.contains(scanId)) {
                            listener.getLogger()
                                    .println(
                                            "[DEBUG] The scan target : '"
                                                    + targetPath
                                                    + "' has Scan Location Id: '"
                                                    + scanId
                                                    + "'. BUT this Id has already been added to the list. Either this is a duplicate target or the correct scan could not be found.");
                            listener.getLogger().println();
                        } else {
                            listener.getLogger().println(
                                    "[DEBUG] The scan target : '" + targetPath + "' has Scan Location Id: '" + scanId + "'.");
                            scanIds.add(scanId);
                        }

                    } else {
                        if (!alreadyMapped) {
                            listener.getLogger().println(
                                    "[ERROR] No Scan Location Id could be found for the scan target : '" + targetPath + "'.");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(listener.getLogger());
            } catch (BDRestException e) {
                e.printStackTrace(listener.getLogger());
            }
        }
        return scanIds;
    }

    public void mapScansToProjectRelease(BuildListener listener, List<String> scanIds, String releaseId) throws BDRestException {
        if (scanIds.size() > 0) {
            for (String scanId : scanIds) {
                listener.getLogger().println("[DEBUG] Mapping the scan with id: '" + scanId + "', to the Release with Id: '" + releaseId + "'.");
                String url = getBaseUrl() + "/api/v1/assetreferences";
                ClientResource resource = new ClientResource(url);

                resource.getRequest().setCookies(cookies);
                resource.setMethod(Method.POST);

                JSONObject obj = new JSONObject();

                JSONObject ownerEntity = new JSONObject();
                ownerEntity.put("entityId", releaseId);
                // this is the release location
                ownerEntity.put("entityType", "RL");

                JSONObject assetEntity = new JSONObject();
                assetEntity.put("entityId", scanId);
                // this is the code location
                assetEntity.put("entityType", "CL");

                obj.put("ownerEntityKey", ownerEntity);
                obj.put("assetEntityKey", assetEntity);

                StringRepresentation stringRep = new StringRepresentation(obj.toString());
                stringRep.setMediaType(MediaType.APPLICATION_JSON);
                resource.post(stringRep);
                int responseCode = resource.getResponse().getStatus().getCode();

                // HashMap<String, Object> responseMap = new HashMap<String, Object>();
                if (responseCode == 201) {
                    // Successful mapping
                    listener.getLogger()
                            .println("[DEBUG] Successfully mapped the scan with id: '" + scanId + "', to the Release with Id: '" + releaseId + "'.");
                } else {
                    throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
                }
            }
        }
        else {
            listener.getLogger().println("[DEBUG] Could not find any scan Id's to map to the Release.");
        }
        // return responseMap;

    }

    /**
     * Gets the project Ids for every project with a name that matches exactly to the one specified.
     * 
     * @param responseList
     *            ArrayList<LinkedHashMap<String, Object>>
     * @param projectName
     *            String
     * @return ArrayList<String> the project Ids
     * @throws IOException
     * @throws BDRestException
     */
    public ArrayList<String> getProjectIdsFromProjectMatches(ArrayList<LinkedHashMap<String, Object>> responseList, String projectName) throws IOException,
            BDRestException {
        ArrayList<String> projectId = new ArrayList<String>();
        if (responseList.size() > 0) {
            for (LinkedHashMap<String, Object> map : responseList) {
                if (map.get("value").equals(projectName)) {
                    projectId.add((String) map.get("uuid"));
                } else {
                    // name does not match
                }
            }
        }
        return projectId;
    }

    public LinkedHashMap<String, Object> getReleaseMatchesForProjectId(String projectId) throws IOException, BDRestException {

        Series<Cookie> cookies = getCookies();
        String url = getBaseUrl() + "/api/v1/projects/" + projectId + "/releases?limit=20";
        ClientResource resource = new ClientResource(url);

        resource.getRequest().setCookies(cookies);
        resource.setMethod(Method.GET);
        resource.get();
        int responseCode = resource.getResponse().getStatus().getCode();

        LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
        if (responseCode == 200 || responseCode == 204 || responseCode == 202) {

            Response resp = resource.getResponse();
            Reader reader = resp.getEntity().getReader();
            BufferedReader bufReader = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufReader.readLine()) != null) {
                sb.append(line + "\n");
            }
            byte[] mapData = sb.toString().getBytes();

            // Create HashMap from the Rest response
            ObjectMapper responseMapper = new ObjectMapper();
            responseMap = responseMapper.readValue(mapData, LinkedHashMap.class);
        } else {
            throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
        }
        return responseMap;
    }

    public String getReleaseIdFromReleaseMatches(LinkedHashMap<String, Object> responseMap, String releaseVersion) throws IOException, BDRestException {
        String releaseId = null;

        if (responseMap.containsKey("items")) {
            ArrayList<LinkedHashMap<String, Object>> releaseList = (ArrayList<LinkedHashMap<String, Object>>) responseMap.get("items");
            for (LinkedHashMap<String, Object> release : releaseList) {
                if (((String) release.get("version")).equals(releaseVersion)) {
                    releaseId = (String) release.get("id");

                }
            }
        }
        return releaseId;
    }

    public HashMap<String, Object> createHubProject(String projectName) throws IOException, BDRestException {

        Series<Cookie> cookies = getCookies();
        String url = getBaseUrl() + "/api/v1/projects";
        ClientResource resource = new ClientResource(url);

        resource.getRequest().setCookies(cookies);
        resource.setMethod(Method.POST);

        JSONObject obj = new JSONObject();
        obj.put("name", projectName);

        StringRepresentation stringRep = new StringRepresentation(obj.toString());
        stringRep.setMediaType(MediaType.APPLICATION_JSON);

        resource.post(stringRep);
        int responseCode = resource.getResponse().getStatus().getCode();

        HashMap<String, Object> responseMap = new HashMap<String, Object>();
        if (responseCode == 201) {

            Response resp = resource.getResponse();
            Reader reader = resp.getEntity().getReader();
            BufferedReader bufReader = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufReader.readLine()) != null) {
                sb.append(line + "\n");
            }
            byte[] mapData = sb.toString().getBytes();

            // Create HashMap from the Rest response
            ObjectMapper responseMapper = new ObjectMapper();
            responseMap = responseMapper.readValue(mapData, HashMap.class);
        } else {
            throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
        }
        return responseMap;
    }

    public int createHubRelease(String projectRelease, String projectId) throws IOException, BDRestException {
        Series<Cookie> cookies = getCookies();
        String url = getBaseUrl() + "/api/v1/releases";
        ClientResource resource = new ClientResource(url);

        JSONObject obj = new JSONObject();
        obj.put("projectId", projectId);
        obj.put("version", projectRelease);

        resource.getRequest().setCookies(cookies);
        resource.setMethod(Method.POST);
        StringRepresentation stringRep = new StringRepresentation(obj.toString());
        stringRep.setMediaType(MediaType.APPLICATION_JSON);

        resource.post(stringRep);
        int responseCode = resource.getResponse().getStatus().getCode();

        return responseCode;
    }
}
