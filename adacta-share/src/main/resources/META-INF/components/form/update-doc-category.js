Alfresco.forms.validation.updateDocCategory = function(field, args, event, form, silent, message) {
	// Get the form data
	var formdata = form.getFormData();
	var formId = form.formId.replace("-form", "");
	
	// Get input value
	// message = _prop_ada_employeeNumber / _prop_ada_employeeBsn / _prop_ada_employeeMrn
	var input = YAHOO.util.Dom.get(formId + message);
	
	// Only listen to doc category event.
	YAHOO.util.Event.addListener(input, "blur", function (e) {
		// The category url, based on dossier identifier
		var inputType = message.substring(message.lastIndexOf("_") + 1);
		var url = Alfresco.constants.PROXY_URI + "nl/defensie/adacta/selectone/categoryItem?id_type=" + inputType + "&id_value=" + input.value;
		var enteredValue = YAHOO.lang.trim(field.value);
		// add leading zero's
		if (inputType === 'employeeNumber' && enteredValue.length >= 1 && enteredValue.length != 11){
			while(enteredValue.length <11){
				enteredValue="0"+enteredValue;
			}
			url = Alfresco.constants.PROXY_URI + "nl/defensie/adacta/selectone/categoryItem?id_type=" + inputType + "&id_value="+enteredValue;
		}
		if (enteredValue.length >= 1 && enteredValue.indexOf("*") === -1) {
			setDocCategory(form, url, false);
		}
	});
	
	return true;
}

/**
 * Create the select list.
 * @param form
 * @param url
 */

function setDocCategory(form, url, addEmpty) {
	var f = YAHOO.util.Dom.get(form.formId);
	var length = f.elements.length;
	
	for (var i = 0; i < length; i++) {
		var element = f.elements[i];
		
		if (element.id.indexOf("docCategory") > -1) {
			element.innerHTML = "";
			
			if (addEmpty) {
				element.options.add(new Option("", ""));
			}
			
			var result = getValueBasedOnCategory(url);
			for (var k = 0, kk = result.items.length; k < kk; k++) {
				element.options.add(new Option(result.items[k].msg, result.items[k].name));
			}
		}
	}
}

/**
 * Get all subject values corresponding to category value. 
 * @param url
 * @returns JSONObject 
 */

function getValueBasedOnCategory(url) {
	var result = null;
	
	var request = new XMLHttpRequest();
	request.onreadystatechange = function() {
		if (request.readyState === 4) {
			if (request.status === 200) {
				var jsonObj = JSON.parse(this.response);
				result = jsonObj;
			}
		}
	};
	request.open("GET", url, false);
	request.send(null);
	
	return result;
}

/**
 * Remove all options of select form element.
 * @param selectbox
 */

function removeOptions(selectbox) {
	for (var i = selectbox.options.length - 1; i >= 0; i--) {
		selectbox.remove(i);
	}
}