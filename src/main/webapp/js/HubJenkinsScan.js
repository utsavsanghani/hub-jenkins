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

	// wait 900 ms before running the checks
	// otherwise it happens too quick and shows that the project doesnt
	// exist yet
	setTimeout(function() {
		scanChecker(hubProjectName);
		scanChecker(hubProjectVersion);
	}, 900);

}

function useSameAsBuildWrapper(checkbox, onload) {
	if (checkbox.checked) {
		enableSameAsBuildWrapper(onload);
	} else {
		disableSameAsBuildWrapper(onload);
	}
	
	if(!onload){
		var sameAsPostBuildScan = getFieldByNameWrapper('_.sameAsPostBuildScan');
		if (sameAsPostBuildScan){
			if (sameAsPostBuildScan.checked) {
				enableSameAsPostBuildScan(false);
			} else{
				disableSameAsPostBuildScan(false);
			}
		}
	}
}

function enableSameAsBuildWrapper(onload) {
	addOnBlurToWrapperFields();

	var sameAsPostBuildScan = getFieldByNameScan('_.sameAsPostBuildScan');
	if (sameAsPostBuildScan && sameAsPostBuildScan.checked) {
		var sameAsBuildWrapperMessageArea = document.getElementById('sameAsBuildWrapperMessageArea');
		sameAsBuildWrapperMessageArea.className = 'error';
		var newScanSpan = document.createElement('span');
		newScanSpan.innerHTML = "The Hub Build Environment is configured to use this configuration!";
		sameAsBuildWrapperMessageArea.appendChild(newScanSpan);

	} else {

		var hubWrapperProjectName = getFieldByNameScan('_.hubWrapperProjectName');
		var hubWrapperProjectVersion = getFieldByNameScan('_.hubWrapperProjectVersion');
		var hubWrapperVersionPhase = getFieldByNameScan('_.hubWrapperVersionPhase');
		var hubWrapperVersionDist = getFieldByNameScan('_.hubWrapperVersionDist');

		var hubProjectName = getFieldByNameScan('_.hubProjectName');
		var hubProjectVersion = getFieldByNameScan('_.hubProjectVersion');
		var hubVersionPhase = getFieldByNameScan('_.hubVersionPhase');
		var hubVersionDist = getFieldByNameScan('_.hubVersionDist');

		if ((hubWrapperProjectName) && (hubWrapperProjectVersion)
				&& (hubWrapperVersionPhase) && (hubWrapperVersionDist)) {
			disableScanFields();

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
				}, 100);
			}
		} else {
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
	enableScanFields();
	
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

	hubProjectName.readOnly = false;
	hubProjectVersion.readOnly = false;
	hubVersionPhase.disabled = false;
	hubVersionDist.disabled = false;

}

function disableScanFields() {
	var hubProjectName = getFieldByNameScan('_.hubProjectName');
	var hubProjectVersion = getFieldByNameScan('_.hubProjectVersion');
	var hubVersionPhase = getFieldByNameScan('_.hubVersionPhase');
	var hubVersionDist = getFieldByNameScan('_.hubVersionDist');

	hubProjectName.readOnly = true;
	hubProjectVersion.readOnly = true;
	hubVersionPhase.disabled = true;
	hubVersionDist.disabled = true;
}

var hubScanOldOnLoad = window.onload;

window.onload = function() {
	hubScanOldOnLoad();

	var sameAsBuildWrapper = getFieldByNameScan('_.sameAsBuildWrapper');
	useSameAsBuildWrapper(sameAsBuildWrapper, true);
};

function addOnBlurToWrapperFields() {
	scanOnBlur('_.hubProjectName', '_.hubWrapperProjectName');
	scanOnBlur('_.hubProjectVersion', '_.hubWrapperProjectVersion');
	scanOnBlur('_.hubVersionPhase', '_.hubWrapperVersionPhase');
	scanOnBlur('_.hubVersionDist', '_.hubWrapperVersionDist');

}

function scanOnBlur(scanFieldName, wrapperFieldName) {
	var wrapperField = getFieldByNameScan(wrapperFieldName);
	if(wrapperField){
		wrapperField.onblur = function() {
			
			var sameAsBuildWrapper = getFieldByNameScan('_.sameAsBuildWrapper');
			if (sameAsBuildWrapper && sameAsBuildWrapper.checked) {
				var scanField = getFieldByNameScan(scanFieldName);
				scanField.value = wrapperField.value
				setTimeout(function() {
					scanChecker(scanField);
				}, 100);
			}
		};
	}
}

function getFieldByNameScan(fieldName){
	return document.getElementsByName(fieldName)[0];
}
