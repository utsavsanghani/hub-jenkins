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

function gradleWrapperUseSameAsPostBuildScan(checkbox, onload) {
	if (checkbox.checked) {
		enableSameAsPostBuildScan(onload, '_.gradleHubProjectName','_.gradleHubProjectVersion','_.gradleHubVersionPhase','_.gradleHubVersionDist', 'sameAsMessageAreaGradle', '_.gradleSameAsPostBuildScan');
	} else {
		disableSameAsPostBuildScan(onload, 'sameAsMessageAreaGradle', '_.gradleHubProjectName','_.gradleHubProjectVersion','_.gradleHubVersionPhase','_.gradleHubVersionDist');
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




var hubGradleWrapperOldOnLoad = window.onload;

window.onload = function() {
	hubGradleWrapperOldOnLoad();

	var sameAsPostBuildScan = getFieldByName('_.gradleSameAsPostBuildScan');
	gradleWrapperUseSameAsPostBuildScan(sameAsPostBuildScan, true);
};




