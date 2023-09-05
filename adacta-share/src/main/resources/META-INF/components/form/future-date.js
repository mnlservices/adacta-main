Alfresco.forms.validation.futureDate = function(field, args, event, form, silent, message) {
	
	// get the form data
    var formdata = form.getFormData();
    var formId = form.formId.replace("-form", "");

    // get the date field
    var cntlDocDate = YAHOO.util.Dom.get(formId + "_prop_ada_docDate-cntrl-date");
    
    if (isFutureDate(cntlDocDate.value)) {
        return true;
    } else {
        return false;
    }
}

function isFutureDate(idate) {
    if (idate == null) {
        return true;
    }
    
    var today = addDays(new Date(), 7).getTime();

    var date = idate.split("T");
    var dateParts = date[0].split("-");

    var newDate = new Date(dateParts[0], dateParts[1] - 1, dateParts[2]).getTime();
    return (newDate - today) < 0 ? true : false;
}

function addDays(date, days) {
    var result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
}