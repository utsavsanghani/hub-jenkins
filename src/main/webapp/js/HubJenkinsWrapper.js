//this method from Jenkins hudson-behavior.js
var wrapperChecker = function(el) {

	var target = el.targetElement;
	FormChecker.sendRequest(el.targetUrl(), {
		method : 'post',
		onComplete : function(x) {
			target.innerHTML = x.responseText;
			Behaviour.applySubtree(target);
		}
	});
}

function customWrapperCreateProject(method, withVars, button) {

	validateButton(method, withVars, button);

	var hubWrapperProjectName = getFieldByNameWrapper('_.hubWrapperProjectName');
	var hubWrapperProjectVersion = getFieldByNameWrapper('_.hubWrapperProjectVersion');

	// wait 1000 ms before running the checks
	// otherwise it happens too quick and shows that the project doesnt
	// exist yet
	setTimeout(function() {
		wrapperChecker(hubWrapperProjectName);
		wrapperChecker(hubWrapperProjectVersion);
	}, 1000);
	
	var sameAsBuildWrapper = getFieldByNameWrapper('_.sameAsBuildWrapper');
	if (sameAsBuildWrapper && sameAsBuildWrapper.checked) {
		// We found the Cli scan checkbox and it is checked to use the same configuration as the Build wrapper
		// So we run the check on the cli fields as well
		
		var hubProjectName = getFieldByNameWrapper('_.hubProjectName');
		var hubProjectVersion = getFieldByNameWrapper('_.hubProjectVersion');
		
		setTimeout(function() {
			wrapperChecker(hubProjectName);
			wrapperChecker(hubProjectVersion);
		}, 1000);
	}

}

function useSameAsPostBuildScan(checkbox, onload) {
	if (checkbox.checked) {
		enableSameAsPostBuildScan(onload);
	} else {
		disableSameAsPostBuildScan(onload);
	}

	// When the window loads this will already run so we dont need to trigger it again
	if(!onload){
		var sameAsBuildWrapper = getFieldByNameScan('_.sameAsBuildWrapper');
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

function enableSameAsPostBuildScan(onload) {

	var sameAsBuildWrapper = getFieldByNameWrapper('_.sameAsBuildWrapper');
	if (sameAsBuildWrapper && sameAsBuildWrapper.checked) {
		// The Build wrapper and the Cli are both checked to use the other configuration
		// This is obviously an issue so we log an error to the screen
		
		var sameAsPostBuildScanMessageArea = document.getElementById('sameAsPostBuildScanMessageArea');
		sameAsPostBuildScanMessageArea.className = 'error';
		var newWrapperSpan = document.createElement('span');
		newWrapperSpan.innerHTML = "The Post-build Action Hub Integration is configured to use this configuration!";
		sameAsPostBuildScanMessageArea.appendChild(newWrapperSpan);
		disableWrapperFields();
		
	} else {
		var hubProjectName = getFieldByNameWrapper('_.hubProjectName');
		var hubProjectVersion = getFieldByNameWrapper('_.hubProjectVersion');
		var hubVersionPhase = getFieldByNameWrapper('_.hubVersionPhase');
		var hubVersionDist = getFieldByNameWrapper('_.hubVersionDist');

		var hubWrapperProjectName = getFieldByNameWrapper('_.hubWrapperProjectName');
		var hubWrapperProjectVersion = getFieldByNameWrapper('_.hubWrapperProjectVersion');
		var hubWrapperVersionPhase = getFieldByNameWrapper('_.hubWrapperVersionPhase');
		var hubWrapperVersionDist = getFieldByNameWrapper('_.hubWrapperVersionDist');

		// Only run this if the scan has been configured
		if ((hubProjectName) && (hubProjectVersion) && (hubVersionPhase)
				&& (hubVersionDist)) {
			addOnBlurToScanFields();
				
			//We disable the wrapper fields since we want to use the scan fields
			disableWrapperFields();

			hubWrapperProjectName.value = hubProjectName.value;
			hubWrapperProjectVersion.value = hubProjectVersion.value;
			hubWrapperVersionPhase.value = hubVersionPhase.value;
			hubWrapperVersionDist.value = hubVersionDist.value;

			if (!(onload)) {
				// Only check if not onload
				// These automatically get checked onload
				setTimeout(function() {
					wrapperChecker(hubWrapperProjectName);
					wrapperChecker(hubWrapperProjectVersion);
				}, 1000);
			}
		} else {
			//The scan is not configured so we cant use the same configuration as it
			// so we log the error for the user
			
			var sameAsPostBuildScanMessageArea = document
					.getElementById('sameAsPostBuildScanMessageArea');
			sameAsPostBuildScanMessageArea.className = 'error';
			var newSpan = document.createElement('span');
			newSpan.innerHTML = "The Post-build Action 'Black Duck Hub Integration' is not configured for this Job!";
			sameAsPostBuildScanMessageArea.appendChild(newSpan);
		}
	}
}

function disableSameAsPostBuildScan(onload) {
	//We enable the wrapper fields since we no longer want to use the scan fields
	enableWrapperFields();

	
	// We remove the appropriate error messages from the UI
	var sameAsPostBuildScanMessageArea = document.getElementById('sameAsPostBuildScanMessageArea');
	sameAsPostBuildScanMessageArea.className = '';
	while (sameAsPostBuildScanMessageArea.firstChild) {
		sameAsPostBuildScanMessageArea
				.removeChild(sameAsPostBuildScanMessageArea.firstChild);
	}

	var sameAsBuildWrapperMessageArea = document.getElementById('sameAsBuildWrapperMessageArea');
	if (sameAsBuildWrapperMessageArea) {

		sameAsBuildWrapperMessageArea.className = '';
		while (sameAsBuildWrapperMessageArea.firstChild) {
			sameAsBuildWrapperMessageArea
					.removeChild(sameAsBuildWrapperMessageArea.firstChild);
		}
	}

}

function enableWrapperFields() {
	var hubWrapperProjectName = getFieldByNameWrapper('_.hubWrapperProjectName');
	var hubWrapperProjectVersion = getFieldByNameWrapper('_.hubWrapperProjectVersion');
	var hubWrapperVersionPhase = getFieldByNameWrapper('_.hubWrapperVersionPhase');
	var hubWrapperVersionDist = getFieldByNameWrapper('_.hubWrapperVersionDist');

	// Make sure the fields are no longer read only or disabled
	hubWrapperProjectName.readOnly = false;
	hubWrapperProjectVersion.readOnly = false;
	hubWrapperVersionPhase.disabled = false;
	hubWrapperVersionDist.disabled = false;
	
	//Remove the readonly css class we added to the fields
	hubWrapperProjectName.className = hubWrapperProjectName.className.replace(/ bdReadOnly/g,"");
	hubWrapperProjectVersion.className = hubWrapperProjectVersion.className.replace(/ bdReadOnly/g,"");
	hubWrapperVersionPhase.className = hubWrapperVersionPhase.className.replace(/ bdReadOnly/g,"");
	hubWrapperVersionDist.className = hubWrapperVersionDist.className.replace(/ bdReadOnly/g,"");

}

function disableWrapperFields() {
	var hubWrapperProjectName = getFieldByNameWrapper('_.hubWrapperProjectName');
	var hubWrapperProjectVersion = getFieldByNameWrapper('_.hubWrapperProjectVersion');
	var hubWrapperVersionPhase = getFieldByNameWrapper('_.hubWrapperVersionPhase');
	var hubWrapperVersionDist = getFieldByNameWrapper('_.hubWrapperVersionDist');

	// Make sure the fields are read only or disabled
	hubWrapperProjectName.readOnly = true;
	hubWrapperProjectVersion.readOnly = true;
	hubWrapperVersionPhase.disabled = true;
	hubWrapperVersionDist.disabled = true;
	
	//Add the readonly css class to the fields	
	hubWrapperProjectName.className = hubWrapperProjectName.className + ' bdReadOnly';
	hubWrapperProjectVersion.className = hubWrapperProjectVersion.className + ' bdReadOnly';
	hubWrapperVersionPhase.className = hubWrapperVersionPhase.className + ' bdReadOnly';
	hubWrapperVersionDist.className = hubWrapperVersionDist.className + ' bdReadOnly';
}



var hubWrapperOldOnLoad = window.onload;

window.onload = function() {
	hubWrapperOldOnLoad();

	var sameAsPostBuildScan = getFieldByNameWrapper('_.sameAsPostBuildScan');
	useSameAsPostBuildScan(sameAsPostBuildScan, true);
};

function addOnBlurToScanFields() {
	projectWrapperOnBlur('_.hubWrapperProjectName','_.hubWrapperProjectVersion', '_.hubProjectName');
	projectWrapperOnBlur('_.hubWrapperProjectVersion','_.hubWrapperProjectName', '_.hubProjectVersion');
	wrapperOnBlur('_.hubWrapperVersionPhase', '_.hubVersionPhase');
	wrapperOnBlur('_.hubWrapperVersionDist', '_.hubVersionDist');

}

function wrapperOnBlur(wrapperFieldName, scanFieldName) {
	var scanField = getFieldByNameWrapper(scanFieldName);
	if(scanField){
		scanField.onblur = function() {
			// When the scan field is changed and then loses focus, this method will run 
			// This will trigger the specified wrapper field to update 
			// ONLY IF the wrapper is set to use the same config as the scan
			
			var sameAsPostBuildScan = getFieldByNameWrapper('_.sameAsPostBuildScan');
			if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
				var wrapperField = getFieldByNameWrapper(wrapperFieldName);
				wrapperField.value = scanField.value
				setTimeout(function() {
					wrapperChecker(wrapperField);
				}, 900);
			}
		};
	}
}

function projectWrapperOnBlur(wrapperFirstFieldName, wrapperSecondFieldName, scanFieldName) {
	//This is a separate method for when the project name or version is updated to trigger
	// the validation of both of the appropriate fields
	
	var scanField = getFieldByNameWrapper(scanFieldName);
	if(scanField){
		scanField.onblur = function() {
			// When the scan field is changed and then loses focus, this method will run 
			// This will trigger the specified wrapper field to update 
			// ONLY IF the wrapper is set to use the same config as the scan
			
			var sameAsPostBuildScan = getFieldByNameWrapper('_.sameAsPostBuildScan');
			if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
				var wrapperFirstField = getFieldByNameWrapper(wrapperFirstFieldName);
				var wrapperSecondField = getFieldByNameWrapper(wrapperSecondFieldName);
				wrapperFirstField.value = scanField.value
				setTimeout(function() {
					wrapperChecker(wrapperFirstField);
					wrapperChecker(wrapperSecondField);
				}, 900);
			}
		};
	}
}


function getFieldByNameWrapper(fieldName){
	return document.getElementsByName(fieldName)[0];
}
