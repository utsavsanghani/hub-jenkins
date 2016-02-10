package com.blackducksoftware.integration.hub.jenkins;

import java.io.Serializable;

public class HubServerInfoSingleton implements Serializable {

    private final static HubServerInfoSingleton _instance;

    static // static constructor
    {
        // instantiate the singleton at class loading time.
        _instance = new HubServerInfoSingleton();
    }

    private HubServerInfo _info;

    private ScanInstallation _scanInstallation;

    /**
     * Default constructor.
     */
    private HubServerInfoSingleton()
    {
        _info = null;
        _scanInstallation = null;
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
    public ScanInstallation getScanInstallation()
    {
        return _scanInstallation;
    }

    /**
     * Replace the Hub scan installation.
     *
     */
    public void setScanInstallation(ScanInstallation scanInstallation)
    {
        _scanInstallation = scanInstallation;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

}
