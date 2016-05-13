/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
