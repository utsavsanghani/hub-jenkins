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

		function customCreateProject(method, withVars, button){
	
			validateButton(method, withVars , button);	

			var hubProjectName = document.getElementsByName('_.hubProjectName')[0];
			var hubProjectVersion = document.getElementsByName('_.hubProjectVersion')[0];
	
			// wait 900 ms before running the checks
			// otherwise it happens too quick and shows that the project doesnt
			// exist yet
			setTimeout(function(){checker(hubProjectName);checker(hubProjectVersion);}, 900);
			
		}
		
		function useSameAsBuildWrapper(checkbox, onload){
			if(checkbox.checked){
				enableSameAsBuildWrapper(onload);
			} else{
				disableSameAsBuildWrapper(onload);
			}
		}
		
		function enableSameAsBuildWrapper(onload){
			var sameAsPostBuildScan = document.getElementsByName('_.sameAsPostBuildScan')[0];
			if(sameAsPostBuildScan && sameAsPostBuildScan.checked){
				var sameAsBuildWrapperMessageArea = document.getElementById('sameAsBuildWrapperMessageArea');
				sameAsBuildWrapperMessageArea.className = 'error';
				var newSpan = document.createElement('span');
				newSpan.innerHTML = "The Hub Build Environment is configured to use this configuration!";
				sameAsBuildWrapperMessageArea.appendChild(newSpan);
			} else{
			
			var hubWrapperProjectName = document.getElementsByName('_.hubWrapperProjectName')[0];
			var hubWrapperProjectVersion = document.getElementsByName('_.hubWrapperProjectVersion')[0];
			var hubWrapperVersionPhase = document.getElementsByName('_.hubWrapperVersionPhase')[0];
			var hubWrapperVersionDist = document.getElementsByName('_.hubWrapperVersionDist')[0];
			
			var hubProjectName = document.getElementsByName('_.hubProjectName')[0];
			var hubProjectVersion = document.getElementsByName('_.hubProjectVersion')[0];
			var hubVersionPhase = document.getElementsByName('_.hubVersionPhase')[0];
			var hubVersionDist = document.getElementsByName('_.hubVersionDist')[0];
			
			if((hubWrapperProjectName) && (hubWrapperProjectVersion) && (hubWrapperVersionPhase) && (hubWrapperVersionDist) ) {
				hubProjectName.readOnly = true;
				hubProjectVersion.readOnly = true;
				hubVersionPhase.disabled = true;
				hubVersionDist.disabled = true;
				
				hubProjectName.value = hubWrapperProjectName.value;
				hubProjectVersion.value = hubWrapperProjectVersion.value;
				hubVersionPhase.value = hubWrapperVersionPhase.value;
				hubVersionDist.value = hubWrapperVersionDist.value;
				
				
				if(!(onload)){
					// Only check if not onload
					// These automatically get checked onload
					setTimeout(function(){checker(hubProjectName);checker(hubProjectVersion);}, 100);
				}
			} else{
				var sameAsBuildWrapperMessageArea = document.getElementById('sameAsBuildWrapperMessageArea');
				sameAsBuildWrapperMessageArea.className = 'error';
				var newSpan = document.createElement('span');
				newSpan.innerHTML = "There is no Hub Build Environment configured for this Job!";
				sameAsBuildWrapperMessageArea.appendChild(newSpan);
			}
			}
		}
		
		
		function disableSameAsBuildWrapper(onload){
			var hubProjectName = document.getElementsByName('_.hubProjectName')[0];
			var hubProjectVersion = document.getElementsByName('_.hubProjectVersion')[0];
			var hubVersionPhase = document.getElementsByName('_.hubVersionPhase')[0];
			var hubVersionDist = document.getElementsByName('_.hubVersionDist')[0];
			
			
			hubProjectName.readOnly = false;
			hubProjectVersion.readOnly = false;
			hubVersionPhase.disabled = false;
			hubVersionDist.disabled = false;
			
			var sameAsBuildWrapperMessageArea = document.getElementById('sameAsBuildWrapperMessageArea');
			sameAsBuildWrapperMessageArea.className = '';
			while (sameAsBuildWrapperMessageArea.firstChild) {
				sameAsBuildWrapperMessageArea.removeChild(sameAsBuildWrapperMessageArea.firstChild);
			}
				
		}
		
		
		
		var hubScanOldOnLoad = window.onload;
		
		window.onload = function(){
			hubScanOldOnLoad();
			
			var sameAsBuildWrapper = document.getElementsByName('_.sameAsBuildWrapper')[0];
			useSameAsBuildWrapper(sameAsBuildWrapper, true);
		};