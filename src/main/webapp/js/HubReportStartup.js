var pageBody = document.getElementById("page-body");
if(pageBody){
	//If the element was found set the class name 
	pageBody.className += " pageBody";
}

adjustWidth(document.getElementById("highVulnerabilityRiskBar"));
adjustWidth(document.getElementById("mediumVulnerabilityRiskBar"));
adjustWidth(document.getElementById("lowVulnerabilityRiskBar"));
adjustWidth(document.getElementById("noVulnerabilityRiskBar"));
adjustTableOnStartup();
