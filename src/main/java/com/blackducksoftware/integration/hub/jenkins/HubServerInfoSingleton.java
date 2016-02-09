package com.blackducksoftware.integration.hub.jenkins;

import com.blackducksoftware.integration.hub.jenkins.cli.HubScanInstallation;

/**
 * A singleton class that contains the configured Hub Server
 * Configuration.
 *
 * @author jrichard
 *
 */
public class HubServerInfoSingleton implements Cloneable {

    private final static HubServerInfoSingleton instance;

    static // static constructor
    {
        // instantiate the singleton at class loading time.
        instance = new HubServerInfoSingleton();
    }

    private HubServerInfo info;

    private HubScanInstallation hubScanInstallation;

    /**
     * Default constructor.
     */
    private HubServerInfoSingleton()
    {
        info = null;
        hubScanInstallation = null;
    }

    /**
     * Retrieve the singleton instance.
     *
     * @return The object instance that encapsulates the object with the server
     *         information.
     */
    public static HubServerInfoSingleton getInstance()
    {
        return instance;
    }

    /**
     * Retrieve the Hub server information object.
     *
     * @return The object containing the server information.
     */
    public HubServerInfo getServerInfo()
    {
        return info;
    }

    /**
     * Retrieve the Hub scan installation
     *
     */
    public HubScanInstallation getHubScanInstallation()
    {
        return hubScanInstallation;
    }

    /**
     * Replace the Hub server information object.
     *
     * @param info
     *            The object containing the server information.
     */
    public void setServerInfo(HubServerInfo info)
    {
        this.info = info;
    }

    /**
     * Replace the Hub scan installation
     */
    public void setHubScanInstallation(HubScanInstallation hubScanInstallation)
    {
        this.hubScanInstallation = hubScanInstallation;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
