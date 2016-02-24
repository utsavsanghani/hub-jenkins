var filteredClassName = " rowFiltered";

function adjustWidth(object) {
	var percentageSpan = object.getElementsByTagName("SPAN")[0];
	var percent = percentageSpan.innerHTML;
	percentageSpan.style.display = "none";
	object.style.width = percent;

}

function adjustTableOnStartup() {
	var governanceReportTable = document.getElementById('hubBomReport').tBodies[0];
	var odd = true;
	for (var i = 0; i < governanceReportTable.rows.length; i++) {
		adjustTableRow(governanceReportTable.rows[i], odd);
	//	adjustVulnerabilities(governanceReportTable.rows[i]);
		odd = !odd;
	}
}

function adjustTableRow(row, odd) {
	var className = row.className;

	if (odd) {
		if (!className || className.length == 0) {
			className += "oddRow";
		} else {
			if (className.indexOf("evenRow") != -1) {
				className = className.replace("evenRow", "oddRow");
			}
		}
	} else {
		if (!className || className.length == 0) {
			className += "evenRow";
		} else {
			if (className.indexOf("oddRow") != -1) {
				className = className.replace("oddRow", "evenRow");
			}
		}
	}

	row.className = className;
}

function adjustVulnerabilities(row) {
	if (row.cells[7].innerHTML.length == 0 || row.cells[7].firstChild.innerHTML == "Unknown") {
		row.cells[6].innerHTML = '-';
		if (row.cells[6].className.indexOf("vuln-unknown-count") == -1) {
			row.cells[6].className += " vuln-unknown-count";
		}
		row.cells[7].innerHTML = '-';
		if (row.cells[7].className.indexOf("vuln-unknown-count") == -1) {
			row.cells[7].className += " vuln-unknown-count";
		}
		row.cells[8].innerHTML = '-';
		if (row.cells[8].className.indexOf("vuln-unknown-count") == -1) {
			row.cells[8].className += " vuln-unknown-count";
		}
	} else {
		if (row.cells[6].innerHTML > 0) {
			if (row.cells[6].className.indexOf("vuln-high-count") == -1) {
				row.cells[6].className += " vuln-high-count";
			}
		}
		if (row.cells[7].innerHTML > 0) {
			if (row.cells[7].className.indexOf("vuln-med-count") == -1) {
				row.cells[7].className += " vuln-med-count";
			}
		}
		if (row.cells[8].innerHTML > 0) {
			if (row.cells[8].className.indexOf("vuln-low-count") == -1) {
				row.cells[8].className += " vuln-low-count";
			}
		}
	}

}

function filterTableByRisk(risk) {
	if (removeFilter('noneVulnLabel', risk)) {
		return;
	}
	if (removeFilter('highVulnLabel', risk)) {
		return;
	}
	if (removeFilter('mediumVulnLabel', risk)) {
		return;
	}
	if (removeFilter('lowVulnLabel', risk)) {
		return;
	}
	risk.className += " filterSelected";
	filterTable(document.getElementById('governanceReport').tBodies[0], risk, false);
}

function removeFilter(id, currRisk) {
	var vulnLabel = document.getElementById(id);
	if (vulnLabel.className.indexOf("filterSelected") != -1) {
		filterTable(document.getElementById('governanceReport').tBodies[0], null, true);
		document.getElementById(id).className = document.getElementById(id).className
		.replace('filterSelected', '');
		if(id == currRisk.id){
			return true;
		}
	}
}

function filterTable(governanceReportTable, riskToFilter, shouldRemoveFilter) {
	var odd = true;
	for (var i = 0; i < governanceReportTable.rows.length; i++) {
		if (shouldRemoveFilter) {
			removeFilterFromRow(governanceReportTable.rows[i]);
		} else {
			filterRow(governanceReportTable.rows[i], riskToFilter);
		}
		adjustTableRow(governanceReportTable.rows[i], odd);
		odd = !odd;
	}
}

function filterRow(row, riskToFilter) {
	if (riskToFilter.id == "noneVulnLabel") {
		if (row.cells[6].innerHTML != 0 || row.cells[7].innerHTML != 0
				|| row.cells[8].innerHTML != 0) {
			if (row.className.indexOf(filteredClassName) == -1) {
				row.className += filteredClassName;
			}
		} else if(row.cells[6].innerHTML == '-' || row.cells[7].innerHTML == '-'
			|| row.cells[8].innerHTML == '-'){
			if (row.className.indexOf(filteredClassName) == -1) {
				row.className += filteredClassName;
			}
		}
	} else if (riskToFilter.id == "highVulnLabel") {
		filterRowByVulnerability(row, 6);
	}else if (riskToFilter.id == "mediumVulnLabel") {
		filterRowByVulnerability(row, 7);
	} else if (riskToFilter.id == "lowVulnLabel") {
		filterRowByVulnerability(row, 8);
	} else{
		if(row.cells[6].innerHTML != '-' || row.cells[7].innerHTML != '-'
			|| row.cells[8].innerHTML != '-'){
			if (row.className.indexOf(filteredClassName) == -1) {
				row.className += filteredClassName;
			}
		}

	}
}

function filterRowByVulnerability(row, cellNumber){
	if (row.cells[cellNumber].innerHTML <= 0) {
		if (row.className.indexOf(filteredClassName) == -1) {
			row.className += filteredClassName;
		}
	} else if(row.cells[6].innerHTML == '-' || row.cells[7].innerHTML == '-'
		|| row.cells[8].innerHTML == '-'){
		if (row.className.indexOf(filteredClassName) == -1) {
			row.className += filteredClassName;
		}
	}
}



function removeFilterFromRow(row) {
	if (row.className.indexOf(filteredClassName) != -1) {
		row.className = row.className.replace(filteredClassName, "");
	}
}

function exportReportAsPDF(title, subject, pdfName) {

	var margins = {
			top: 20,
			bottom: 10,
			left: 10,
			right: 10
	};


	var doc = new jsPDF('p', 'mm', 'letter');

	if(title){
		margins.top = margins.top + 5;
		doc.setFontSize(22);
		doc.text(30, 15, title);
		doc.setFontSize(16);
	}

	doc.setProperties({
		title: title,
		subject: subject,
		author: 'Black Duck Software',
		creator: 'blackduck-vulnerability-report'
	});

	// All units are in the set measurement for the document
	// This can be changed to "pt" (points), "mm" (Default), "cm", "in"
	doc.addHTML($('#reportToExport')[0]
	, margins.left // x coord
	, margins.top // y coord 
	, { 'pagesplit': false, dim : { w: 196, h: 280} }//'margin_bottom': margins.bottom, 'margin_right': margins.right}
	, function() { 
		doc.save(pdfName);
	}
	);

}

function exportReportAsCSV(csvName) {
	var governanceReportTable = $("#governanceReport")[0];
	if (governanceReportTable.tHead == null){
		governanceReportTable.tHead = governanceReportTable.getElementsByTagName('thead')[0];
	}

	var csvLines = "";
	csvLines = createCsvLineFromRow(governanceReportTable.tHead.rows[0]) + "\r\n"; // Add the Header row

	var governanceReportTableBody = governanceReportTable.tBodies[0];
	var odd = true;
	for (var i = 0; i < governanceReportTableBody.rows.length; i++) {
		csvLines = csvLines + createCsvLineFromRow(governanceReportTableBody.rows[i]) + "\r\n";
	}



//	window.open('data:text/csv;charset=utf-8,filename='+csvName+',' + encodeURIComponent(csvLines), '_self');
//	var download = document.createElement('a');
//	download.setAttribute('href', 'data:text/csv;charset=utf-8,' + encodeURIComponent(csvLines));
//	// download.setAttribute('href', 'data:text/csv;charset=utf-8,' + csvLines);
//	download.setAttribute('download', csvName);
//	download.style.display = 'none';
//	document.body.appendChild(download);
//	download.click();
//	document.body.removeChild(download);


	/////////////////////////// Internet Explorer
	// See http://stackoverflow.com/questions/18755750/saving-text-in-a-local-file-in-internet-explorer-10
	var ie = navigator.userAgent.match(/MSIE\s([\d.]+)/),
	ie11 = navigator.userAgent.match(/Trident\/7.0/) && navigator.userAgent.match(/rv:11/),
	ieVer=(ie ? ie[1] : (ie11 ? 11 : -1));


	if (ie || ie11) {
		// Internet explorer
		var blob = new Blob([csvLines], {type: "text/csv"});
		window.navigator.msSaveBlob(blob, csvName);
	/////////////////////////// Internet Explorer End
		
	} else {
		// Set up the link
		var link = document.createElement("a");
		link.setAttribute("target","_blank");
		if(Blob !== undefined) {
			// Blob support http://caniuse.com/#feat=bloburls
			var blob = new Blob([csvLines], {type: "text/csv"});
			link.setAttribute("href", URL.createObjectURL(blob));
		} else {
			link.setAttribute("href","data:text/csv," + encodeURIComponent(csvLines));
		}
		link.setAttribute("download",csvName);
		document.body.appendChild(link);
		link.click();
		document.body.removeChild(link);
	}

}

function createCsvLineFromRow(row){
	var lineElements = row.cells;
	var csvLine = "";

	for (var i = 0; i < lineElements.length; i++) {
		if(lineElements[i].childNodes[0].className != null){
			//This is the license column
			csvLine = csvLine + '"' + lineElements[i].childNodes[0].innerHTML.toString() + '"';
		} else{
			csvLine = csvLine + '"' + lineElements[i].innerHTML.toString() + '"';
		}

		if(i != lineElements.length - 1){
			csvLine = csvLine + ",";
		}
	}
	return csvLine;
}
