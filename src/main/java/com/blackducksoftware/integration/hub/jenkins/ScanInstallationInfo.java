package com.blackducksoftware.integration.hub.jenkins;

import java.io.Serializable;

public class ScanInstallationInfo implements Serializable {

    private String toolLocation;

    public ScanInstallationInfo() {
    }

    public ScanInstallationInfo(String toolLocation) {
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
