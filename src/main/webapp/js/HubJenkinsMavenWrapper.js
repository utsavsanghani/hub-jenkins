
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


