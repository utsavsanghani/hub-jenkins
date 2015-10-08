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

function singleOnBlur(fieldNameToUpdate, fieldNameToBlur, checkBoxFieldName) {
	var fieldToUpdate = getFieldByName(fieldNameToBlur);
	if(fieldToUpdate){
		fieldToUpdate.onblur = function() {
			// When the field to blur is changed and then loses focus, this method will run 
			// This will trigger the specified field to update 
			// ONLY IF the checkbox is checked
			
			var checkbox = getFieldByName(checkBoxFieldName);
			if (checkbox && checkbox.checked) {
				var fieldToUpdate = getFieldByName(fieldNameToUpdate);
				fieldToUpdate.value = fieldToUpdate.value
				setTimeout(function() {
					wrapperChecker(fieldToUpdate);
				}, 900);
			}
		};
	}
}

function doubleOnBlur(firstFieldNameToUpdate, secondFieldNameToUpdate, fieldNameToBlur, checkBoxFieldName) {
	//This is a separate method for when there are two fields need to be check when the blur field is changed
	
	var fieldToUpdate = getFieldByName(fieldNameToBlur);
	if(fieldToUpdate){
		fieldToUpdate.onblur = function() {
			// When the field to blur is changed and then loses focus, this method will run 
			// This will trigger the specified field to update 
			// ONLY IF the checkbox is checked
			
			var checkbox = getFieldByName(checkBoxFieldName);
			if (checkbox && checkbox.checked) {
				var firstField = getFieldByName(firstFieldNameToUpdate);
				var secondField = getFieldByName(secondFieldNameToUpdate);
				firstField.value = fieldToUpdate.value
				setTimeout(function() {
					wrapperChecker(firstField);
					wrapperChecker(secondField);
				}, 900);
			}
		};
	}
}


function getFieldByName(fieldName){
	return document.getElementsByName(fieldName)[0];
}
