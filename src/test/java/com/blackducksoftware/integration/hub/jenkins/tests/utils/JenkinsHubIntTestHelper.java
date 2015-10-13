package com.blackducksoftware.integration.hub.jenkins.tests.utils;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.Cookie;
import org.restlet.data.Method;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.jenkins.Messages;
import com.blackducksoftware.integration.hub.response.ProjectItem;

public class JenkinsHubIntTestHelper extends HubIntRestService {

    public JenkinsHubIntTestHelper(String baseUrl) {
        super(baseUrl);
    }

    /**
     * Delete HubProject. For test purposes only!
     *
     * @param projectId
     *            String
     * @return boolean true if deleted successfully
     * @throws BDRestException
     */
    public boolean deleteHubProject(String projectId) {
        if (StringUtils.isEmpty(projectId)) {
            return false;
        }
        try {
            Series<Cookie> cookies = getCookies();
            String url = getBaseUrl() + "/api/v1/projects/" + projectId;
            ClientResource resource = new ClientResource(url);

            resource.getRequest().setCookies(cookies);
            resource.setMethod(Method.DELETE);

            resource.delete();
            int responseCode = resource.getResponse().getStatus().getCode();

            if (responseCode != 204) {
                System.out.println(Messages.HubBuildScan_getErrorConnectingTo_0_(responseCode));
            } else {
                return true;
            }
        } catch (ResourceException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public ProjectItem getProjectByName(String projectName) throws IOException, BDRestException, URISyntaxException {
        try {
            return super.getProjectByName(projectName);
        } catch (BDRestException e) {
            e.printStackTrace();
        }
        return new ProjectItem();
    }

}
