Alfresco.forms.validation.updateDocSubject = function(field, args, event, form, silent, message) {	
    // get the form data
    var formdata = form.getFormData();
    var formId = form.formId.replace("-form", "");
    
    // get category  
    var docCategory = YAHOO.util.Dom.get(formId + "_prop_ada_docCategory");
    
    // only listen to doc category event.
    YAHOO.util.Event.addListener(docCategory, "change", function (e) {       
        // the subject url
        var url = Alfresco.constants.PROXY_URI + "nl/defensie/adacta/selectone/subjectItem/";

        // get the value
        if (YAHOO.lang.trim(field.value).length != 0) {
            url += field.value;
        } else {
            url += "choose";
        }
        
    	if (YAHOO.lang.trim(field.value).length != 0 && docCategory != null) {
            setDocSubject(form, url);
        }
    });

    return true;
}

/**
 * Create the select list.
 * @param form
 * @param url
 */

function setDocSubject(form, url) {
    var f = YAHOO.util.Dom.get(form.formId);
    var length = f.elements.length;

    for (var i = 0; i < length; i++) {
        var element = f.elements[i];

        if (element.id.indexOf("docSubject") > -1) {
            element.innerHTML = "";
            
            var result = getValueBasedOnCategory(url);
            for (var k = 0, kk = result.items.length; k < kk; k++) {
                element.options.add(new Option(result.items[k].msg, result.items[k].name));
            }
            
            element.focus();
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
    var i;
    for (i = selectbox.options.length - 1; i >= 0; i--) {
        selectbox.remove(i);
    }
}