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

function mavenWrapperUseSameAsPostBuildScan(checkbox, onload) {
	if (checkbox.checked) {
		enableSameAsPostBuildScan(onload,  '_.mavenHubProjectName','_.mavenHubProjectVersion','_.mavenHubVersionPhase','_.mavenHubVersionDist', 'sameAsMessageAreaMaven', '_.mavenSameAsPostBuildScan');
	} else {
		disableSameAsPostBuildScan(onload, 'sameAsMessageAreaMaven', '_.mavenHubProjectName','_.mavenHubProjectVersion','_.mavenHubVersionPhase','_.mavenHubVersionDist');
	}

	// When the window loads this will already run so we dont need to trigger it again
	if(!onload){
		var sameAsBuildWrapper = getFieldByName('_.sameAsBuildWrapper');
		if (sameAsBuildWrapper){
			if(sameAsBuildWrapper.checked) {
				// We found the Cli scan checkbox and it is checked to use the same configuration as the Build wrapper
				// So we trigger its enable method as well
				enableSameAsBuildWrapper(false);
			} else{
				disableSameAsBuildWrapper(false);
			}
		}
	}
}




var hubMavenWrapperOldOnLoad = window.onload;

window.onload = function() {
	hubMavenWrapperOldOnLoad();

	var sameAsPostBuildScan = getFieldByName('_.mavenSameAsPostBuildScan');
	mavenWrapperUseSameAsPostBuildScan(sameAsPostBuildScan, true);
};


