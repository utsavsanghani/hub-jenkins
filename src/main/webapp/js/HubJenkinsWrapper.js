//this method from Jenkins hudson-behavior.js
		var checker = function(el) {
		
        	var target = el.targetElement;
        	FormChecker.sendRequest(el.targetUrl(), {
            	method : 'post',
            	onComplete : function(x) {
            	    target.innerHTML = x.responseText;
            	   Behaviour.applySubtree(target);
           	 	}
        	});
    	}

		function customWrapperCreateProject(method, withVars, button){
	
			validateButton(method, withVars , button);	

			var hubWrapperProjectName = document.getElementsByName('_.hubWrapperProjectName')[0];
			var hubWrapperProjectVersion = document.getElementsByName('_.hubWrapperProjectVersion')[0];
	
			// wait 900 ms before running the checks
			// otherwise it happens too quick and shows that the project doesnt
			// exist yet
			setTimeout(function(){checker(hubWrapperProjectName);checker(hubWrapperProjectVersion);}, 900);
			
		}
		
		function useSameAsPostBuildScan(checkbox, onload){
			if(checkbox.checked){
				enableSameAsPostBuildScan(onload);
			} else{
				disableSameAsPostBuildScan(onload);
			}
		}
		
		function enableSameAsPostBuildScan(onload){
			var hubProjectName = document.getElementsByName('_.hubProjectName')[0];
			var hubProjectVersion = document.getElementsByName('_.hubProjectVersion')[0];
			var hubVersionPhase = document.getElementsByName('_.hubVersionPhase')[0];
			var hubVersionDist = document.getElementsByName('_.hubVersionDist')[0];
			
			var hubWrapperProjectName = document.getElementsByName('_.hubWrapperProjectName')[0];
			var hubWrapperProjectVersion = document.getElementsByName('_.hubWrapperProjectVersion')[0];
			var hubWrapperVersionPhase = document.getElementsByName('_.hubWrapperVersionPhase')[0];
			var hubWrapperVersionDist = document.getElementsByName('_.hubWrapperVersionDist')[0];
			
			
			if((hubProjectName) && (hubProjectVersion) && (hubVersionPhase) && (hubVersionDist) ) {
				hubWrapperProjectName.readOnly = true;
				hubWrapperProjectVersion.readOnly = true;
				hubWrapperVersionPhase.disabled = true;
				hubWrapperVersionDist.disabled = true;
				
				hubWrapperProjectName.value = hubProjectName.value;
				hubWrapperProjectVersion.value = hubProjectVersion.value;
				hubWrapperVersionPhase.value = hubVersionPhase.value;
				hubWrapperVersionDist.value = hubVersionDist.value;
				
				if(!(onload)){
					// Only check if not onload
					// These automatically get checked onload
					setTimeout(function(){checker(hubWrapperProjectName);checker(hubWrapperProjectVersion);}, 100);
				}
			} else{
				var sameAsPostBuildScanMessageArea = document.getElementById('sameAsPostBuildScanMessageArea');
				sameAsPostBuildScanMessageArea.className = 'error';
				var newSpan = document.createElement('span');
				newSpan.innerHTML = "The Post-build Action 'Black Duck Hub Integration' is not configured for this Job!";
				sameAsPostBuildScanMessageArea.appendChild(newSpan);
			}
		}
		
		
		function disableSameAsPostBuildScan(onload){
			var hubWrapperProjectName = document.getElementsByName('_.hubWrapperProjectName')[0];
			var hubWrapperProjectVersion = document.getElementsByName('_.hubWrapperProjectVersion')[0];
			var hubWrapperVersionPhase = document.getElementsByName('_.hubWrapperVersionPhase')[0];
			var hubWrapperVersionDist = document.getElementsByName('_.hubWrapperVersionDist')[0];

			hubWrapperProjectName.readOnly = false;
			hubWrapperProjectVersion.readOnly = false;
			hubWrapperVersionPhase.disabled = false;
			hubWrapperVersionDist.disabled = false;
			
			var sameAsPostBuildScanMessageArea = document.getElementById('sameAsPostBuildScanMessageArea');
			sameAsPostBuildScanMessageArea.className = '';
			while (sameAsPostBuildScanMessageArea.firstChild) {
				sameAsPostBuildScanMessageArea.removeChild(sameAsPostBuildScanMessageArea.firstChild);
			}
				
		}
		
		
		
		var oldOnLoad = window.onload;
		
		window.onload = function(){
			oldOnLoad();
			
			var sameAsPostBuildScan = document.getElementsByName('_.sameAsPostBuildScan')[0];
			useSameAsPostBuildScan(sameAsPostBuildScan, true);
		};