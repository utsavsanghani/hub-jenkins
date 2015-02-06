package com.blackducksoftware.integration.hub.jenkins.tests;

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

}
