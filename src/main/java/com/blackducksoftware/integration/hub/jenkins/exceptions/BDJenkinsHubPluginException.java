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
package com.blackducksoftware.integration.hub.jenkins.exceptions;

public class BDJenkinsHubPluginException extends Exception {

    public BDJenkinsHubPluginException() {

    }

    public BDJenkinsHubPluginException(String message)
    {
        super(message);
    }

    public BDJenkinsHubPluginException(Throwable cause)
    {
        super(cause);
    }

    public BDJenkinsHubPluginException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
