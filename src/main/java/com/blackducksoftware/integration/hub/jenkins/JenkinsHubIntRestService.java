package com.blackducksoftware.integration.hub.jenkins;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.codehaus.plexus.util.StringUtils;
import org.restlet.Context;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.CookieSetting;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import com.blackducksoftware.integration.hub.jenkins.exceptions.BDRestException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JenkinsHubIntRestService {
    private Series<Cookie> cookies;

    private String baseUrl;

    private String proxyHost;

    private int proxyPort;

    private List<Pattern> noProxyHosts;

    private BuildListener listener;

    protected JenkinsHubIntRestService() {
    }

    public void setListener(BuildListener listener) {
        this.listener = listener;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setNoProxyHosts(List<Pattern> noProxyHosts) {
        this.noProxyHosts = noProxyHosts;
    }

    public List<Pattern> getNoProxyHosts() {
        return noProxyHosts;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    private ClientResource createClientResource(String url) throws MalformedURLException {
        ClientResource resource = new ClientResource(new Context(), url);
        if (noProxyHosts != null) {
            if (!getMatchingNoProxyHostPatterns(url, noProxyHosts)) {
                if (!StringUtils.isEmpty(proxyHost) && proxyPort != 0) {
                    resource.getContext().getParameters().add("proxyHost", proxyHost);
                    resource.getContext().getParameters().add("proxyPort", Integer.toString(proxyPort));
                }
            } else {
                if (listener != null) {
                    URL currUrl = new URL(url);
                    listener.getLogger().println(
                            "[DEBUG] Ignoring proxy for the Host: '" + currUrl.getHost() + "'");
                }
            }
        }
        return resource;
    }

    /**
     * Checks the list of user defined host names that should be connected to directly and not go through the proxy. If
     * the hostToMatch matches any of these hose names then this method returns true.
     *
     * @param hostToMatch
     *            String the hostName to check if it is in the list of hosts that should not go through the proxy.
     *
     * @return boolean
     */
    protected static boolean getMatchingNoProxyHostPatterns(String hostToMatch, List<Pattern> noProxyHosts) {
        if (noProxyHosts.isEmpty()) {
            // User did not specify any hosts to ignore the proxy
            return false;
        }
        StringBuilder pattern = new StringBuilder();
        for (Pattern p : noProxyHosts) {
            if (pattern.length() > 0) {
                pattern.append('|');
            }
            pattern.append('(');
            pattern.append(p.pattern());
            pattern.append(')');
        }
        Pattern noProxyHostPattern = Pattern.compile(pattern.toString());
        Matcher m = noProxyHostPattern.matcher(hostToMatch);
        boolean match = false;
        while (m.find()) {
            match = true;
        }
        return match;

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
     * @throws MalformedURLException
     */
    public int setCookies(String credentialUserName, String credentialPassword) throws MalformedURLException {
        String url = getBaseUrl() + "/j_spring_security_check?j_username=" + credentialUserName + "&j_password=" + credentialPassword;
        ClientResource resource = createClientResource(url);
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

            cookies = requestCookies;
        }
        // else {
        // cookies already set
        // }

        return resource.getResponse().getStatus().getCode();
    }

    public Series<Cookie> getCookies() {
        return cookies;
    }

    public ArrayList<LinkedHashMap<String, Object>> getProjectMatches(String hubProjectName) throws IOException, BDRestException {

        String url = getBaseUrl() + "/api/v1/autocomplete/PROJECT?text=" + hubProjectName + "&limit=30";
        ClientResource resource = createClientResource(url);

        resource.getRequest().setCookies(getCookies());
        resource.setMethod(Method.GET);
        resource.get();
        int responseCode = resource.getResponse().getStatus().getCode();

        if (responseCode == 200 || responseCode == 204 || responseCode == 202) {
            ArrayList<LinkedHashMap<String, Object>> list = null;
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
            list = responseMapper.readValue(mapData, ArrayList.class);
            // responseMap = responseMapper.readValue(mapData, HashMap.class);
            return list;
        } else {
            throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
        }
    }

    public ArrayList<LinkedHashMap<String, Object>> getProjectById(String projectId) throws IOException, BDRestException {

        String url = getBaseUrl() + "/api/v1/projects/" + projectId;
        ClientResource resource = createClientResource(url);

        resource.getRequest().setCookies(getCookies());
        resource.setMethod(Method.GET);
        resource.get();
        int responseCode = resource.getResponse().getStatus().getCode();

        if (responseCode == 200 || responseCode == 204 || responseCode == 202) {
            ArrayList<LinkedHashMap<String, Object>> list = null;
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
            list = responseMapper.readValue(mapData, ArrayList.class);
            // responseMap = responseMapper.readValue(mapData, HashMap.class);
            return list;
        } else {
            throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
        }
    }

    public String getProjectId(String hubProjectName) throws IOException, BDRestException {

        String url = getBaseUrl() + "/api/v1/projects?name=" + hubProjectName;
        ClientResource resource = createClientResource(url);
        try {
            resource.getRequest().setCookies(getCookies());
            resource.setMethod(Method.GET);
            resource.get();
            int responseCode = resource.getResponse().getStatus().getCode();

            HashMap<String, Object> responseMap = new HashMap<String, Object>();
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

                if (!responseMap.containsKey("id")) {
                    // The Hub Api has changed and we received a JSON response that we did not expect
                    throw new BDRestException(Messages.HubBuildScan_getIncorrectMappingOfServerResponse());
                } else {
                    return (String) responseMap.get("id");
                }

            } else {
                throw new BDRestException(Messages.HubBuildScan_getProjectNonExistingOrTroubleConnecting_());
            }
        } catch (ResourceException e) {
            throw new BDRestException(Messages.HubBuildScan_getProjectNonExistingOrTroubleConnecting_(), e);
        }
    }

    /**
     * Gets the scan Id for each scan target, it searches the list of scans and gets the latest scan id for the scan
     * matching the hostname and path. If the matching scans are already mapped to the Version then that id will not be
     * returned in the list.
     *
     * @param listener
     *            BuildListener
     * @param scanTargets
     *            List<String>
     * @param versionId
     *            String
     *
     * @return Map<Boolean, String> scan Ids
     * @throws UnknownHostException
     * @throws MalformedURLException
     * @throws InterruptedException
     * @throws BDRestException
     */
    public Map<String, Boolean> getScanLocationIds(AbstractBuild build, BuildListener listener, List<String> scanTargets, String versionId)
            throws UnknownHostException,
            MalformedURLException, InterruptedException, BDRestException {
        HashMap<String, Boolean> scanLocationIds = new HashMap<String, Boolean>();
        for (String targetPath : scanTargets) {
            String url = null;
            ClientResource resource = null;
            String localHostName = "";
            try {
                localHostName = build.getBuiltOn().getChannel().call(new GetHostName());
            } catch (IOException e) {
                listener.error("Problem getting the Local Host name : " + e.getMessage());
                e.printStackTrace(listener.getLogger());
            }
            if (localHostName == null || localHostName.length() == 0) {
                return null;
            }
            url = baseUrl + "/api/v1/scanlocations?host=" + localHostName + "&path=" + targetPath;
            listener.getLogger().println(
                    "[DEBUG] Checking for the scan location with Host name: '" + localHostName + "' and Path: '" + targetPath + "'");

            resource = createClientResource(url);

            resource.getRequest().setCookies(getCookies());
            resource.setMethod(Method.GET);

            ScanLocationHandler handler = new ScanLocationHandler(listener);

            handler.getScanLocationIdWithRetry(build, resource, targetPath, versionId, scanLocationIds);

        }
        return scanLocationIds;
    }

    public void mapScansToProjectVersion(BuildListener listener, Map<String, Boolean> scanLocationIds, String versionId) throws BDRestException,
            MalformedURLException {
        if (!scanLocationIds.isEmpty()) {
            for (Entry<String, Boolean> scanId : scanLocationIds.entrySet()) {
                if (!scanId.getValue()) {
                    // This scan location has not yet been mapped to the project/version
                    listener.getLogger().println(
                            "[DEBUG] Mapping the scan location with id: '" + scanId.getKey() + "', to the Version with Id: '" + versionId + "'.");
                    // FIXME Need to change this to /api/v1/asset-references soon
                    String url = getBaseUrl() + "/api/v1/assetreferences";
                    ClientResource resource = createClientResource(url);

                    resource.getRequest().setCookies(getCookies());
                    resource.setMethod(Method.POST);

                    JSONObject obj = new JSONObject();

                    JSONObject ownerEntity = new JSONObject();
                    ownerEntity.put("entityId", versionId);
                    // this is the version location
                    ownerEntity.put("entityType", "RL");

                    JSONObject assetEntity = new JSONObject();
                    assetEntity.put("entityId", scanId.getKey());
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
                                .println(
                                        "[DEBUG] Successfully mapped the scan with id: '" + scanId.getKey() + "', to the Version with Id: '" + versionId + "'.");
                    } else {
                        throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
                    }
                } else {
                    listener.getLogger().println(
                            "[DEBUG] The scan location with id: '" + scanId.getKey() + "', is already mapped to the Version with Id: '" + versionId + "'.");
                }
            }
        }
        else {
            listener.getLogger().println("[DEBUG] Could not find any scan Id's to map to the Version.");
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
        if (!responseList.isEmpty()) {
            for (LinkedHashMap<String, Object> map : responseList) {
                if (map.get("value").equals(projectName)) {
                    projectId.add((String) map.get("uuid"));
                }
                // else {
                // name does not match
                // }
            }
        }
        return projectId;
    }

    public LinkedHashMap<String, Object> getVersionMatchesForProjectId(String projectId) throws IOException, BDRestException {

        String url = getBaseUrl() + "/api/v1/projects/" + projectId + "/releases?limit=30";
        ClientResource resource = createClientResource(url);

        resource.getRequest().setCookies(getCookies());
        resource.setMethod(Method.GET);
        resource.get();
        int responseCode = resource.getResponse().getStatus().getCode();

        LinkedHashMap<String, Object> responseMap = null;
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
            responseMap = responseMapper.readValue(mapData, LinkedHashMap.class);
        } else {
            throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
        }
        return responseMap;
    }

    public String getVersionIdFromMatches(LinkedHashMap<String, Object> responseMap, String releaseVersion) throws IOException, BDRestException {
        String versionId = null;

        if (responseMap.containsKey("items")) {
            ArrayList<LinkedHashMap<String, Object>> versionList = (ArrayList<LinkedHashMap<String, Object>>) responseMap.get("items");
            for (LinkedHashMap<String, Object> release : versionList) {
                if (((String) release.get("version")).equals(releaseVersion)) {
                    versionId = (String) release.get("id");

                }
            }
        }
        return versionId;
    }

    public HashMap<String, Object> createHubProject(String projectName) throws IOException, BDRestException {

        String url = getBaseUrl() + "/api/v1/projects";
        ClientResource resource = createClientResource(url);
        HashMap<String, Object> responseMap = null;
        try {
            resource.getRequest().setCookies(getCookies());
            resource.setMethod(Method.POST);

            JSONObject obj = new JSONObject();
            obj.put("name", projectName);

            StringRepresentation stringRep = new StringRepresentation(obj.toString());
            stringRep.setMediaType(MediaType.APPLICATION_JSON);

            resource.post(stringRep);
            int responseCode = resource.getResponse().getStatus().getCode();

            if (responseCode == 201) {

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
        } catch (ResourceException e) {
            org.restlet.Response r = resource.getResponse();
            System.out.println(e.getStatus().toString());
            System.out.println(r.getEntityAsText());
        }

        return responseMap;
    }

    public int createHubVersion(String projectVersion, String projectId) throws IOException, BDRestException {
        String url = getBaseUrl() + "/api/v1/releases";
        ClientResource resource = createClientResource(url);
        int responseCode;
        try {
            JSONObject obj = new JSONObject();
            obj.put("projectId", projectId);
            obj.put("version", projectVersion);
            obj.put("phase", "DEVELOPMENT");
            obj.put("distribution", "EXTERNAL");

            resource.getRequest().setCookies(getCookies());
            resource.setMethod(Method.POST);
            StringRepresentation stringRep = new StringRepresentation(obj.toString());
            stringRep.setMediaType(MediaType.APPLICATION_JSON);

            resource.post(stringRep);
            responseCode = resource.getResponse().getStatus().getCode();
        } catch (ResourceException e) {
            org.restlet.Response r = resource.getResponse();
            System.out.println(e.getStatus().toString());
            System.out.println(r.getEntityAsText());
            responseCode = resource.getResponse().getStatus().getCode();
        }
        return responseCode;
    }

}
