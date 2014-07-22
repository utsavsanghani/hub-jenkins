package com.blackducksoftware.integration.hub.jenkins.tests;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.Cookie;
import org.restlet.data.Method;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;

import com.blackducksoftware.integration.hub.jenkins.JenkinsHubIntRestService;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.jenkins.exceptions.BDRestException;

public class JenkinsHubIntTestHelper extends JenkinsHubIntRestService {

    protected JenkinsHubIntTestHelper() {

    }

    /**
     * Delete HubProject. For test purposes only!
     * 
     * @param projectId
     *            String
     * @return boolean true if deleted successfully
     * @throws BDRestException
     */
    public boolean deleteHubProject(String projectId) throws BDRestException {
        if (StringUtils.isEmpty(projectId)) {
            return false;
        }

        Series<Cookie> cookies = getCookies();
        String url = getBaseUrl() + "/api/v1/projects/" + projectId;
        ClientResource resource = new ClientResource(url);

        resource.getRequest().setCookies(cookies);
        resource.setMethod(Method.DELETE);

        resource.delete();
        int responseCode = resource.getResponse().getStatus().getCode();

        if (responseCode != 204) {
            throw new BDRestException(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
        } else {
            return true;
        }
    }

    /**
     * Create Hub Project. For test purposes only!
     * 
     * @param projectId
     *            String
     * @return String projectId
     * @throws BDRestException
     * @throws IOException
     */
    public String createTestHubProject(String hubProjectName) throws BDRestException, IOException {
        if (StringUtils.isEmpty(hubProjectName)) {
            return null;
        }
        String projectId = null;
        Series<Cookie> cookies = getCookies();
        HashMap<String, Object> responseMap = createHubProject(hubProjectName);
        StringBuilder projectReleases = new StringBuilder();
        if (responseMap.containsKey("id")) {
            projectId = (String) responseMap.get("id");
        } else {
            // The Hub Api has changed and we received a JSON response that we did not expect
            throw new BDRestException(Messages.HubBuildScan_getIncorrectMappingOfServerResponse());
        }
        return projectId;
    }

    /**
     * Create Hub Project Release. For test purposes only!
     * 
     * @param projectId
     *            String
     * @return boolean true if created successfully
     * @throws BDRestException
     * @throws IOException
     */
    public boolean createTestHubProjectRelease(String hubProjectRelease, String projectId) throws IOException, BDRestException {
        if (StringUtils.isEmpty(projectId)) {
            return false;
        }
        if (StringUtils.isEmpty(hubProjectRelease)) {
            return false;
        }
        int responseCode = 0;
        responseCode = createHubRelease(hubProjectRelease, projectId);
        if (responseCode != 201) {
            return false;
        }
        return true;

    }

}
