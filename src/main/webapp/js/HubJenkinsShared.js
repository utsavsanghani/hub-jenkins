/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
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
	var fieldToBlur = getFieldByName(fieldNameToBlur);
	if(fieldToBlur){
		fieldToBlur.onblur = function() {
			// When the field to blur is changed and then loses focus, this method will run 
			// This will trigger the specified field to update 
			// ONLY IF the checkbox is checked
			
			var checkbox = getFieldByName(checkBoxFieldName);
			if (checkbox && checkbox.checked) {
				var fieldToUpdate = getFieldByName(fieldNameToUpdate);
				fieldToUpdate.value = fieldToBlur.value
				setTimeout(function() {
					checker(fieldToUpdate);
				}, 900);
			}
		};
	}
}

function doubleOnBlur(firstFieldNameToUpdate, secondFieldNameToUpdate, fieldNameToBlur, checkBoxFieldName) {
	//This is a separate method for when there are two fields need to be check when the blur field is changed
	
	var fieldToBlur = getFieldByName(fieldNameToBlur);
	if(fieldToBlur){
		fieldToBlur.onblur = function() {
			// When the field to blur is changed and then loses focus, this method will run 
			// This will trigger the specified field to update 
			// ONLY IF the checkbox is checked
			
			var checkbox = getFieldByName(checkBoxFieldName);
			if (checkbox && checkbox.checked) {
				var firstField = getFieldByName(firstFieldNameToUpdate);
				var secondField = getFieldByName(secondFieldNameToUpdate);
				firstField.value = fieldToBlur.value
				setTimeout(function() {
					checker(firstField);
					checker(secondField);
				}, 900);
			}
		};
	}
}


function getFieldByName(fieldName){
	return document.getElementsByName(fieldName)[0];
}
