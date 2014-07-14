package com.blackducksoftware.integration.hub.jenkins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.CookieSetting;
import org.restlet.data.Method;
import org.restlet.representation.EmptyRepresentation;
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

    public HashMap<String, Object> getProjectMatches(String hubProjectName) throws IOException, BDRestException {

        String url = getBaseUrl() + "/api/v1/search/PROJECT?q=" + hubProjectName + "&limit=20";
        ClientResource resource = new ClientResource(url);

        resource.getRequest().setCookies(cookies);
        resource.setMethod(Method.GET);
        resource.get();
        int responseCode = resource.getResponse().getStatus().getCode();

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
        return responseMap;
    }

    public HashMap<String, Object> getReleaseMatchesForProjectId(String projectId) throws IOException, BDRestException {

        Series<Cookie> cookies = getCookies();
        String url = getBaseUrl() + "/api/v1/projects/" + projectId + "/releases?limit=20";
        ClientResource resource = new ClientResource(url);

        resource.getRequest().setCookies(cookies);
        resource.setMethod(Method.GET);
        resource.get();
        int responseCode = resource.getResponse().getStatus().getCode();

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
        return responseMap;
    }

    public HashMap<String, Object> createHubProject(String projectName) throws IOException, BDRestException {

        Series<Cookie> cookies = getCookies();
        String url = getBaseUrl() + "/api/v1/projects";
        ClientResource resource = new ClientResource(url);

        resource.getRequest().setCookies(cookies);
        resource.setMethod(Method.POST);
        resource.setAttribute("name", projectName);
        EmptyRepresentation rep = new EmptyRepresentation();
        resource.post(rep);
        int responseCode = resource.getResponse().getStatus().getCode();

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
        return responseMap;
    }

    public int createHubRelease(String projectRelease, String projectId) throws IOException, BDRestException {
        Series<Cookie> cookies = getCookies();
        String url = getBaseUrl() + "/api/v1/releases";
        ClientResource resource = new ClientResource(url);

        resource.getRequest().setCookies(cookies);
        resource.setMethod(Method.POST);
        resource.getRequestAttributes().put("projectId", projectId);
        resource.getRequestAttributes().put("version", projectRelease);
        resource.post(resource.getRequest());
        int responseCode = resource.getResponse().getStatus().getCode();

        return responseCode;
    }
}
