package com.blackducksoftware.integration.hub.jenkins;

import java.io.Serializable;

import com.blackducksoftware.integration.hub.jenkins.cli.HubScanInstallation;

public class HubServerInfoSingleton implements Serializable {

    private final static HubServerInfoSingleton _instance;

    static // static constructor
    {
        // instantiate the singleton at class loading time.
        _instance = new HubServerInfoSingleton();
    }

    private HubServerInfo _info;

    private HubScanInstallation _hubScanInstallation;

    /**
     * Default constructor.
     */
    private HubServerInfoSingleton()
    {
        _info = null;
        _hubScanInstallation = null;
    }

    /**
     * Retrieve the singleton instance.
     *
     * @return The object instance that encapsulates the object with the server
     *         information.
     */
    public static HubServerInfoSingleton getInstance()
    {
        return _instance;
    }

    /**
     * Retrieve the Hub server information object.
     *
     * @return The object containing the server information.
     */
    public HubServerInfo getServerInfo()
    {
        return _info;
    }

    /**
     * Replace the Hub server information object.
     *
     * @param info
     *            The object containing the server information.
     */
    public void setServerInfo(HubServerInfo info)
    {
        _info = info;
    }

    /**
     * Retrieve the Hub scan installation.
     *
     */
    public HubScanInstallation getHubScanInstallation()
    {
        return _hubScanInstallation;
    }

    /**
     * Replace the Hub scan installation.
     *
     */
    public void setHubScanInstallation(HubScanInstallation hubScanInstallation)
    {
        _hubScanInstallation = hubScanInstallation;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

}
