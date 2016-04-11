package com.blackducksoftware.integration.hub.jenkins;

public class HubServerInfoSingleton {

	private final static HubServerInfoSingleton _instance;

	static // static constructor
	{
		// instantiate the singleton at class loading time.
		_instance = new HubServerInfoSingleton();
	}

	private HubServerInfo _info;

	/**
	 * Default constructor.
	 */
	private HubServerInfoSingleton()
	{
		_info = null;
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
	 */
	public void setServerInfo(final HubServerInfo info)
	{
		_info = info;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

}
