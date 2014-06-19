package com.blackducksoftware.integration.hub.jenkins;

import java.io.Serializable;

public class IScanInstallationInfo implements Serializable {

    private String toolLocation;

    public IScanInstallationInfo() {
    }

    public IScanInstallationInfo(String toolLocation) {
        super();
        this.toolLocation = toolLocation;
    }

    public String getToolLocation() {
        return toolLocation;
    }

    public void setToolLocation(String toolLocation) {
        this.toolLocation = toolLocation;
    }

}
