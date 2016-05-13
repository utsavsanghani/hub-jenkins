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

function customCreateProject(method, withVars, button) {

	validateButton(method, withVars, button);

	var hubProjectName = getFieldByName('_.hubProjectName');
	var hubProjectVersion = getFieldByName('_.hubProjectVersion');

	// wait 1000 ms before running the checks
	// otherwise it happens too quick and shows that the project doesnt
	// exist yet
	setTimeout(function() {
		checker(hubProjectName);
		checker(hubProjectVersion);
	}, 1000);

	if(checkMavenWrapperIsEnabled()){
		var sameAsPostBuildScan = getFieldByName('_.mavenSameAsPostBuildScan');
		if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
			// We found the Build wrapper checkbox and it is checked to use the same configuration as the Cli scan
			// So we run the check on the wrapper fields as well
			
			var hubWrapperProjectName = getFieldByName('_.mavenHubProjectName');
			var hubWrapperProjectVersion = getFieldByName('_.mavenHubProjectVersion');
			
			setTimeout(function() {
				checker(hubWrapperProjectName);
				checker(hubWrapperProjectVersion);
			}, 1000);
		}
	} else if(checkGradleWrapperIsEnabled()){
		var sameAsPostBuildScan = getFieldByName('_.gradleSameAsPostBuildScan');
		if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
			// We found the Build wrapper checkbox and it is checked to use the same configuration as the Cli scan
			// So we run the check on the wrapper fields as well
			
			var hubWrapperProjectName = getFieldByName('_.gradleHubProjectName');
			var hubWrapperProjectVersion = getFieldByName('_.gradleHubProjectVersion');
			
			setTimeout(function() {
				checker(hubWrapperProjectName);
				checker(hubWrapperProjectVersion);
			}, 1000);
		}
	}
}

function checkMavenWrapperIsEnabled(){
	var mavenWrapperCheckBox = getFieldByName('com-blackducksoftware-integration-hub-jenkins-maven-MavenBuildWrapper');
	var mavenReporterCheckBox = getFieldByName('com-blackducksoftware-integration-hub-jenkins-maven-HubMavenReporter');
	
	
	if ((mavenWrapperCheckBox && mavenWrapperCheckBox.checked) || (mavenReporterCheckBox && mavenReporterCheckBox.checked)) {
		return true;
	} else{
		return false;
	}
}

function checkGradleWrapperIsEnabled(){
	
	var gradleWrapperCheckBox = getFieldByName('com-blackducksoftware-integration-hub-jenkins-gradle-GradleBuildWrapper');
	
	if ((gradleWrapperCheckBox && gradleWrapperCheckBox.checked)) {
		return true;
	} else{
		return false;
	}
}

function checkBothWrappersAreEnabled(){
		return checkMavenWrapperIsEnabled() && checkGradleWrapperIsEnabled();
}

function useSameAsBuildWrapper(checkbox, onload) {
	if (checkbox.checked) {
		enableSameAsBuildWrapper(onload);
	} else {
		disableSameAsBuildWrapper(onload);
	}
	
	// When the window loads this will already run so we dont need to trigger it again
	if(!onload){
		if(checkMavenWrapperIsEnabled()){
			var sameAsPostBuildScan = getFieldByName('_.mavenSameAsPostBuildScan');
			if (sameAsPostBuildScan){
				if (sameAsPostBuildScan.checked) {
					// We found the Build wrapper checkbox and it is checked to use the same configuration as the Cli scan
					// So we trigger its enable method as well
					enableSameAsPostBuildScan(false,  '_.mavenHubProjectName','_.mavenHubProjectVersion','_.mavenHubVersionPhase','_.mavenHubVersionDist', 'sameAsMessageAreaMaven', '_.mavenSameAsPostBuildScan');
				} else{
					disableSameAsPostBuildScan(false, 'sameAsMessageAreaMaven', '_.mavenHubProjectName','_.mavenHubProjectVersion','_.mavenHubVersionPhase','_.mavenHubVersionDist');
				}
			}
		} else if(checkGradleWrapperIsEnabled()){
			var sameAsPostBuildScan = getFieldByName('_.gradleSameAsPostBuildScan');
			if (sameAsPostBuildScan){
				if (sameAsPostBuildScan.checked) {
					// We found the Build wrapper checkbox and it is checked to use the same configuration as the Cli scan
					// So we trigger its enable method as well
					enableSameAsPostBuildScan(false, '_.gradleHubProjectName','_.gradleHubProjectVersion','_.gradleHubVersionPhase','_.gradleHubVersionDist', 'sameAsMessageAreaGradle', '_.gradleSameAsPostBuildScan');
				} else{
					disableSameAsPostBuildScan(false, 'sameAsMessageAreaGradle', '_.gradleHubProjectName','_.gradleHubProjectVersion','_.gradleHubVersionPhase','_.gradleHubVersionDist');
				}
			}
		}
	}
}

function enableSameAsBuildWrapper(onload) {
	if(checkBothWrappersAreEnabled()){
		// Both the maven and gradle wrappers are configured, so we cant determine which one to use with only one check box
		
		enableScanFields();
		addTextToMessageArea("Both of the Hub Build Environment's are configured. Can not determine which configuration to use!");
		return;
	}
	
	var foundError = false;
	if(checkMavenWrapperIsEnabled()){
		var sameAsPostBuildScan = getFieldByName('_.mavenSameAsPostBuildScan');
		if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
			// The Build wrapper and the Cli are both checked to use the other configuration
			// This is obviously an issue so we log an error to the screen
			
			enableScanFields();
			addTextToMessageArea("The Hub Maven Build Environment is configured to use this configuration!");
			foundError = true;
		}
	} 
	if(checkGradleWrapperIsEnabled()){
		var sameAsPostBuildScan = getFieldByName('_.gradleSameAsPostBuildScan');
		if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
			// The Build wrapper and the Cli are both checked to use the other configuration
			// This is obviously an issue so we log an error to the screen
			
			enableScanFields();
			addTextToMessageArea("The Hub Gradle Build Environment is configured to use this configuration!");
			foundError = true;
		}
	}
	if(foundError){
		return;
	}
	var hubWrapperProjectName;
	var hubWrapperProjectVersion;
	var hubWrapperVersionPhase;
	var hubWrapperVersionDist;
	
	if(checkMavenWrapperIsEnabled()){
		 hubWrapperProjectName = getFieldByName('_.mavenHubProjectName');
		 hubWrapperProjectVersion = getFieldByName('_.mavenHubProjectVersion');
		 hubWrapperVersionPhase = getFieldByName('_.mavenHubVersionPhase');
		 hubWrapperVersionDist = getFieldByName('_.mavenHubVersionDist');
	} else if(checkGradleWrapperIsEnabled()){
		 hubWrapperProjectName = getFieldByName('_.gradleHubProjectName');
		 hubWrapperProjectVersion = getFieldByName('_.gradleHubProjectVersion');
		 hubWrapperVersionPhase = getFieldByName('_.gradleHubVersionPhase');
		 hubWrapperVersionDist = getFieldByName('_.gradleHubVersionDist');
	} else{
		//No wrapper is configured so we cant use the same configuration as it
		// so we log the error for the user
		
		enableScanFields();
		addTextToMessageArea("There is no Hub Build Environment configured for this Job!");
		return;
	}

		var hubProjectName = getFieldByName('_.hubProjectName');
		var hubProjectVersion = getFieldByName('_.hubProjectVersion');
		var hubVersionPhase = getFieldByName('_.hubVersionPhase');
		var hubVersionDist = getFieldByName('_.hubVersionDist');

		if(checkMavenWrapperIsEnabled()){
			addOnBlurToWrapperFields('_.mavenHubProjectName', '_.mavenHubProjectVersion', '_.mavenHubVersionPhase', '_.mavenHubVersionDist', '_.sameAsBuildWrapper');
		}  else if(checkGradleWrapperIsEnabled()){
			addOnBlurToWrapperFields('_.gradleHubProjectName', '_.gradleHubProjectVersion', '_.gradleHubVersionPhase', '_.gradleHubVersionDist', '_.sameAsBuildWrapper');
		}
			
			//We disable the scan fields since we want to use the wrapper fields
			disableScanFields();

			//We set the scan fields to the same values as the wrapper fields
			hubProjectName.value = hubWrapperProjectName.value;
			hubProjectVersion.value = hubWrapperProjectVersion.value;
			hubVersionPhase.value = hubWrapperVersionPhase.value;
			hubVersionDist.value = hubWrapperVersionDist.value;

			if (!(onload)) {
				// Only check if not onload
				// These automatically get checked onload
				setTimeout(function() {
					checker(hubProjectName);
					checker(hubProjectVersion);
				}, 1000);
			}
}

function disableSameAsBuildWrapper(onload) {
	//We enable the scan fields since we no longer want to use the wrapper fields
	enableScanFields();
	
	if(checkMavenWrapperIsEnabled()){
		// We remove the appropriate error messages from the UI
		var sameAsPostBuildScanMessageArea = document.getElementById('sameAsMessageAreaMaven');
		if (sameAsPostBuildScanMessageArea) {
			sameAsPostBuildScanMessageArea.className = '';
			while (sameAsPostBuildScanMessageArea.firstChild) {
				sameAsPostBuildScanMessageArea.removeChild(sameAsPostBuildScanMessageArea.firstChild);
			}
		}
	}
	
	if(checkGradleWrapperIsEnabled()){
		// We remove the appropriate error messages from the UI
		var sameAsPostBuildScanMessageArea = document.getElementById('sameAsMessageAreaGradle');
		if (sameAsPostBuildScanMessageArea) {
			sameAsPostBuildScanMessageArea.className = '';
			while (sameAsPostBuildScanMessageArea.firstChild) {
				sameAsPostBuildScanMessageArea.removeChild(sameAsPostBuildScanMessageArea.firstChild);
			}
		}
	}

	var sameAsBuildWrapperMessageArea = document.getElementById('sameAsBuildWrapperMessageArea');
	sameAsBuildWrapperMessageArea.className = '';
	while (sameAsBuildWrapperMessageArea.firstChild) {
		sameAsBuildWrapperMessageArea.removeChild(sameAsBuildWrapperMessageArea.firstChild);
	}
}

function enableScanFields() {
	var hubProjectName = getFieldByName('_.hubProjectName');
	var hubProjectVersion = getFieldByName('_.hubProjectVersion');
	var hubVersionPhase = getFieldByName('_.hubVersionPhase');
	var hubVersionDist = getFieldByName('_.hubVersionDist');

	// Make sure the fields are no longer read only or disabled
	hubProjectName.readOnly = false;
	hubProjectVersion.readOnly = false;
	hubVersionPhase.disabled = false;
	hubVersionDist.disabled = false;
	
	//Remove the readonly css class we added to the fields
	hubProjectName.className = hubProjectName.className.replace(/ bdReadOnly/g,"");
	hubProjectVersion.className = hubProjectVersion.className.replace(/ bdReadOnly/g,"");
	hubVersionPhase.className = hubVersionPhase.className.replace(/ bdReadOnly/g,"");
	hubVersionDist.className = hubVersionDist.className.replace(/ bdReadOnly/g,"");


}

function disableScanFields() {
	var hubProjectName = getFieldByName('_.hubProjectName');
	var hubProjectVersion = getFieldByName('_.hubProjectVersion');
	var hubVersionPhase = getFieldByName('_.hubVersionPhase');
	var hubVersionDist = getFieldByName('_.hubVersionDist');

	// Make sure the fields are read only or disabled
	hubProjectName.readOnly = true;
	hubProjectVersion.readOnly = true;
	hubVersionPhase.disabled = true;
	hubVersionDist.disabled = true;
	
	//Add the readonly css class to the fields	
	hubProjectName.className = hubProjectName.className + ' bdReadOnly';
	hubProjectVersion.className = hubProjectVersion.className + ' bdReadOnly';
	hubVersionPhase.className = hubVersionPhase.className + ' bdReadOnly';
	hubVersionDist.className = hubVersionDist.className + ' bdReadOnly';
}

var hubScanOldOnLoad = window.onload;

window.onload = function() {
	hubScanOldOnLoad();

	var sameAsBuildWrapper = getFieldByName('_.sameAsBuildWrapper');
	if(sameAsBuildWrapper){
		useSameAsBuildWrapper(sameAsBuildWrapper, true);
	}
	
};

function addOnBlurToWrapperFields(wrapperProjectFieldName, wrapperVersionFieldName, wrapperPhaseFieldName, wrapperDistFieldName, scanCheckBoxFieldName) {
		doubleOnBlur('_.hubProjectName', '_.hubProjectVersion', wrapperProjectFieldName, scanCheckBoxFieldName);
		doubleOnBlur('_.hubProjectVersion', '_.hubProjectName', wrapperVersionFieldName, scanCheckBoxFieldName);
		singleOnBlur('_.hubVersionPhase', wrapperPhaseFieldName, scanCheckBoxFieldName);
		singleOnBlur('_.hubVersionDist', wrapperDistFieldName, scanCheckBoxFieldName);
}

function addTextToMessageArea(txt){
	var sameAsBuildWrapperMessageArea = document.getElementById('sameAsBuildWrapperMessageArea');
	if(sameAsBuildWrapperMessageArea.className.indexOf('error') == -1){
		sameAsBuildWrapperMessageArea.className = 'error';
	}
	if((sameAsBuildWrapperMessageArea.firstChild)){
		if(sameAsBuildWrapperMessageArea.firstChild.innerHtml == txt){
			return;
		} else{
			sameAsBuildWrapperMessageArea.firstChild.innerHtml = sameAsBuildWrapperMessageArea.firstChild.innerHtml + " " + txt;
			return;
		}
	}
	var newScanSpan = document.createElement('span');
	newScanSpan.innerHTML = txt;
	sameAsBuildWrapperMessageArea.appendChild(newScanSpan);
	return;
}
