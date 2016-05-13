/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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




