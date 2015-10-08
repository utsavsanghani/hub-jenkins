
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




