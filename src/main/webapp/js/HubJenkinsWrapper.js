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

	// wait 900 ms before running the checks
	// otherwise it happens too quick and shows that the project doesnt
	// exist yet
	setTimeout(function() {
		wrapperChecker(hubWrapperProjectName);
		wrapperChecker(hubWrapperProjectVersion);
	}, 900);

}

function useSameAsPostBuildScan(checkbox, onload) {
	if (checkbox.checked) {
		enableSameAsPostBuildScan(onload);
	} else {
		disableSameAsPostBuildScan(onload);
	}
	if(!onload){	
		var sameAsBuildWrapper = getFieldByNameScan('_.sameAsBuildWrapper');
		if (sameAsBuildWrapper){
			if(sameAsBuildWrapper.checked) {
				enableSameAsBuildWrapper(false);
			} else{
				disableSameAsBuildWrapper(false);
			}
		}
	}
}

function enableSameAsPostBuildScan(onload) {
	addOnBlurToScanFields();

	var sameAsBuildWrapper = getFieldByNameWrapper('_.sameAsBuildWrapper');
	if (sameAsBuildWrapper && sameAsBuildWrapper.checked) {
		var sameAsPostBuildScanMessageArea = document.getElementById('sameAsPostBuildScanMessageArea');
		sameAsPostBuildScanMessageArea.className = 'error';
		var newWrapperSpan = document.createElement('span');
		newWrapperSpan.innerHTML = "The Post-build Action Hub Integration is configured to use this configuration!";
		sameAsPostBuildScanMessageArea.appendChild(newWrapperSpan);

	} else {
		var hubProjectName = getFieldByNameWrapper('_.hubProjectName');
		var hubProjectVersion = getFieldByNameWrapper('_.hubProjectVersion');
		var hubVersionPhase = getFieldByNameWrapper('_.hubVersionPhase');
		var hubVersionDist = getFieldByNameWrapper('_.hubVersionDist');

		var hubWrapperProjectName = getFieldByNameWrapper('_.hubWrapperProjectName');
		var hubWrapperProjectVersion = getFieldByNameWrapper('_.hubWrapperProjectVersion');
		var hubWrapperVersionPhase = getFieldByNameWrapper('_.hubWrapperVersionPhase');
		var hubWrapperVersionDist = getFieldByNameWrapper('_.hubWrapperVersionDist');

		if ((hubProjectName) && (hubProjectVersion) && (hubVersionPhase)
				&& (hubVersionDist)) {
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
				}, 100);
			}
		} else {
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
	enableWrapperFields();

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

	hubWrapperProjectName.readOnly = false;
	hubWrapperProjectVersion.readOnly = false;
	hubWrapperVersionPhase.disabled = false;
	hubWrapperVersionDist.disabled = false;

}

function disableWrapperFields() {
	var hubWrapperProjectName = getFieldByNameWrapper('_.hubWrapperProjectName');
	var hubWrapperProjectVersion = getFieldByNameWrapper('_.hubWrapperProjectVersion');
	var hubWrapperVersionPhase = getFieldByNameWrapper('_.hubWrapperVersionPhase');
	var hubWrapperVersionDist = getFieldByNameWrapper('_.hubWrapperVersionDist');

	hubWrapperProjectName.readOnly = true;
	hubWrapperProjectVersion.readOnly = true;
	hubWrapperVersionPhase.disabled = true;
	hubWrapperVersionDist.disabled = true;
}



var hubWrapperOldOnLoad = window.onload;

window.onload = function() {
	hubWrapperOldOnLoad();

	var sameAsPostBuildScan = getFieldByNameWrapper('_.sameAsPostBuildScan');
	useSameAsPostBuildScan(sameAsPostBuildScan, true);
};

function addOnBlurToScanFields() {
	wrapperOnBlur('_.hubWrapperProjectName', '_.hubProjectName');
	wrapperOnBlur('_.hubWrapperProjectVersion', '_.hubProjectVersion');
	wrapperOnBlur('_.hubWrapperVersionPhase', '_.hubVersionPhase');
	wrapperOnBlur('_.hubWrapperVersionDist', '_.hubVersionDist');

}

function wrapperOnBlur(wrapperFieldName, scanFieldName) {
	var scanField = getFieldByNameWrapper(scanFieldName);
	if(scanField){
		scanField.onblur = function() {
			
			var sameAsPostBuildScan = getFieldByNameWrapper('_.sameAsPostBuildScan');
			if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
				var wrapperField = getFieldByNameWrapper(wrapperFieldName);
				wrapperField.value = scanField.value
				setTimeout(function() {
					wrapperChecker(wrapperField);
				}, 100);
			}
		};
	}
}


function getFieldByNameWrapper(fieldName){
	return document.getElementsByName(fieldName)[0];
}
