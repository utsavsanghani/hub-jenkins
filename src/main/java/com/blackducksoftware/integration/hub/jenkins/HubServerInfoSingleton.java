/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
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
