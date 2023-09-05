/**
 *  Replace control value jjjjmmdd with jjjj-mm-dd. This is because users do only enter numbers.
 */
Alfresco.forms.validation.updateDocDate = function(field, args, event, form, silent, message) {
    // get the form data
    var formdata = form.getFormData();
    var formId = form.formId.replace("-form", "");

    // get the date field
    var cntlDocDate = YAHOO.util.Dom.get(formId + "_prop_ada_docDate-cntrl-date");

    YAHOO.util.Event.addListener(cntlDocDate, "keyup", function (e) {
	    // we expect jjjjmmdd
	    if (cntlDocDate != null) {
	
	        var val = cntlDocDate.value;
	        if (val.length == 8) {
	            var cntrl = val.replace(/(\d{4})(\d{2})(\d{2})/, "$1-$2-$3");
	            cntlDocDate.value = cntrl;
	        }
	    }
    });
    return true;
}