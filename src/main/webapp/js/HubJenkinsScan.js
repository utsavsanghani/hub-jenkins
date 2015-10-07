//this method from Jenkins hudson-behavior.js
var scanChecker = function(el) {

	var target = el.targetElement;
	FormChecker.sendRequest(el.targetUrl(), {
		method : 'post',
		onComplete : function(x) {
			target.innerHTML = x.responseText;
			Behaviour.applySubtree(target);
		}
	});
}

function customCreateProject(method, withVars, button) {

	validateButton(method, withVars, button);

	var hubProjectName = getFieldByNameScan('_.hubProjectName');
	var hubProjectVersion = getFieldByNameScan('_.hubProjectVersion');

	// wait 1000 ms before running the checks
	// otherwise it happens too quick and shows that the project doesnt
	// exist yet
	setTimeout(function() {
		scanChecker(hubProjectName);
		scanChecker(hubProjectVersion);
	}, 1000);

	var sameAsPostBuildScan = getFieldByNameScan('_.sameAsPostBuildScan');
	if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
		// We found the Build wrapper checkbox and it is checked to use the same configuration as the Cli scan
		// So we run the check on the wrapper fields as well
		
		var hubWrapperProjectName = getFieldByNameScan('_.hubWrapperProjectName');
		var hubWrapperProjectVersion = getFieldByNameScan('_.hubWrapperProjectVersion');
		
		setTimeout(function() {
			wrapperChecker(hubWrapperProjectName);
			wrapperChecker(hubWrapperProjectVersion);
		}, 1000);
	}
}

function useSameAsBuildWrapper(checkbox, onload) {
	if (checkbox.checked) {
		enableSameAsBuildWrapper(onload);
	} else {
		disableSameAsBuildWrapper(onload);
	}
	
	// When the window loads this will already run so we dont need to trigger it again
	if(!onload){
		var sameAsPostBuildScan = getFieldByNameWrapper('_.sameAsPostBuildScan');
		if (sameAsPostBuildScan){
			if (sameAsPostBuildScan.checked) {
				// We found the Build wrapper checkbox and it is checked to use the same configuration as the Cli scan
				// So we trigger its enable method as well
				enableSameAsPostBuildScan(false);
			} else{
				disableSameAsPostBuildScan(false);
			}
		}
	}
}

function enableSameAsBuildWrapper(onload) {
	var sameAsPostBuildScan = getFieldByNameScan('_.sameAsPostBuildScan');
	if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
		// The Build wrapper and the Cli are both checked to use the other configuration
		// This is obviously an issue so we log an error to the screen
		
		var sameAsBuildWrapperMessageArea = document.getElementById('sameAsBuildWrapperMessageArea');
		sameAsBuildWrapperMessageArea.className = 'error';
		var newScanSpan = document.createElement('span');
		newScanSpan.innerHTML = "The Hub Build Environment is configured to use this configuration!";
		sameAsBuildWrapperMessageArea.appendChild(newScanSpan);
		disableScanFields();
	} else {
		addOnBlurToWrapperFields();

		var hubWrapperProjectName = getFieldByNameScan('_.hubWrapperProjectName');
		var hubWrapperProjectVersion = getFieldByNameScan('_.hubWrapperProjectVersion');
		var hubWrapperVersionPhase = getFieldByNameScan('_.hubWrapperVersionPhase');
		var hubWrapperVersionDist = getFieldByNameScan('_.hubWrapperVersionDist');

		var hubProjectName = getFieldByNameScan('_.hubProjectName');
		var hubProjectVersion = getFieldByNameScan('_.hubProjectVersion');
		var hubVersionPhase = getFieldByNameScan('_.hubVersionPhase');
		var hubVersionDist = getFieldByNameScan('_.hubVersionDist');

		// Only run this if the wrapper has been configured
		if ((hubWrapperProjectName) && (hubWrapperProjectVersion)
				&& (hubWrapperVersionPhase) && (hubWrapperVersionDist)) {
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
					scanChecker(hubProjectName);
					scanChecker(hubProjectVersion);
				}, 1000);
			}
		} else {
			//The wrapper is not configured so we cant use the same configuration as it
			// so we log the error for the user
			
			var sameAsBuildWrapperMessageArea = document
					.getElementById('sameAsBuildWrapperMessageArea');
			sameAsBuildWrapperMessageArea.className = 'error';
			var newSpan = document.createElement('span');
			newSpan.innerHTML = "There is no Hub Build Environment configured for this Job!";
			sameAsBuildWrapperMessageArea.appendChild(newSpan);
		}
	}
}

function disableSameAsBuildWrapper(onload) {
	//We enable the scan fields since we no longer want to use the wrapper fields
	enableScanFields();
	
	
	// We remove the appropriate error messages from the UI
	var sameAsPostBuildScanMessageArea = document.getElementById('sameAsPostBuildScanMessageArea');
	if (sameAsPostBuildScanMessageArea) {
		sameAsPostBuildScanMessageArea.className = '';
		while (sameAsPostBuildScanMessageArea.firstChild) {
			sameAsPostBuildScanMessageArea.removeChild(sameAsPostBuildScanMessageArea.firstChild);
		}
	}

	var sameAsBuildWrapperMessageArea = document.getElementById('sameAsBuildWrapperMessageArea');
	sameAsBuildWrapperMessageArea.className = '';
	while (sameAsBuildWrapperMessageArea.firstChild) {
		sameAsBuildWrapperMessageArea.removeChild(sameAsBuildWrapperMessageArea.firstChild);
	}
}

function enableScanFields() {
	var hubProjectName = getFieldByNameScan('_.hubProjectName');
	var hubProjectVersion = getFieldByNameScan('_.hubProjectVersion');
	var hubVersionPhase = getFieldByNameScan('_.hubVersionPhase');
	var hubVersionDist = getFieldByNameScan('_.hubVersionDist');

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
	var hubProjectName = getFieldByNameScan('_.hubProjectName');
	var hubProjectVersion = getFieldByNameScan('_.hubProjectVersion');
	var hubVersionPhase = getFieldByNameScan('_.hubVersionPhase');
	var hubVersionDist = getFieldByNameScan('_.hubVersionDist');

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

	var sameAsBuildWrapper = getFieldByNameScan('_.sameAsBuildWrapper');
	useSameAsBuildWrapper(sameAsBuildWrapper, true);
};

function addOnBlurToWrapperFields() {
	projectScanOnBlur('_.hubProjectName', '_.hubProjectVersion', '_.hubWrapperProjectName');
	projectScanOnBlur('_.hubProjectVersion', '_.hubProjectName', '_.hubWrapperProjectVersion');
	scanOnBlur('_.hubVersionPhase', '_.hubWrapperVersionPhase');
	scanOnBlur('_.hubVersionDist', '_.hubWrapperVersionDist');

}

function scanOnBlur(scanFieldName, wrapperFieldName) {
	var wrapperField = getFieldByNameScan(wrapperFieldName);
	if(wrapperField){
		wrapperField.onblur = function() {
			// When the wrapper field is changed and then loses focus, this method will run 
			// This will trigger the specified scan field to update 
			// ONLY IF the scan is set to use the same config as the wrapper
			var sameAsBuildWrapper = getFieldByNameScan('_.sameAsBuildWrapper');
			if (sameAsBuildWrapper && sameAsBuildWrapper.checked) {
				var scanField = getFieldByNameScan(scanFieldName);
				scanField.value = wrapperField.value
				setTimeout(function() {
					scanChecker(scanField);
				}, 900);
			}
		};
	}
}

function projectScanOnBlur(scanFirstFieldName, scanSecondFieldName, scanFieldName) {
	//This is a separate method for when the project name or version is updated to trigger
	// the validation of both of the appropriate fields
	
	var scanField = getFieldByNameWrapper(scanFieldName);
	if(scanField){
		scanField.onblur = function() {
			// When the wrapper field is changed and then loses focus, this method will run 
			// This will trigger the specified scan field to update 
			// ONLY IF the scan is set to use the same config as the wrapper
			var sameAsPostBuildScan = getFieldByNameWrapper('_.sameAsBuildWrapper');
			if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
				var scanFirstField = getFieldByNameWrapper(scanFirstFieldName);
				var scanSecondField = getFieldByNameWrapper(scanSecondFieldName);
				scanFirstField.value = scanField.value
				setTimeout(function() {
					wrapperChecker(scanFirstField);
					wrapperChecker(scanSecondField);
				}, 900);
			}
		};
	}
}

function getFieldByNameScan(fieldName){
	return document.getElementsByName(fieldName)[0];
}
