function main(query) {
	// Widget instantiation metadata
	var widget = {
		id : "AdactaDocumentBrowser", 
		name : "Alfresco.AdactaDocumentBrowser",
		options : {
			searchTerm : query,
			searchType : "searchPage",
			maxSearchResults: 250,
			isAdactaAdmin : isAdactaAdmin(),
			canDownload : canDownload()
	    },
		searchTerm : query
	};
	model.widgets = [widget];
}

var obj = JSON.parse(page.url.args.query);
var generatedQuery = "PATH:\"/app:company_home/st:sites/cm:adacta/cm:documentLibrary/cm:Dossiers//*\"  AND TYPE:";

if (obj.datatype === "ada:dossier") {
	generatedQuery += "\"ada:dossier\"";
} else if (obj.datatype === "ada:document") {
	generatedQuery += "\"ada:document\"";

	if (obj.prop_ada_docCategory.length > 1) {
		generatedQuery += " AND =@ada\\:docCategory:'" + obj.prop_ada_docCategory + "'";
	}
	if (obj.prop_ada_docSubject.length > 2) {
		generatedQuery += " AND =@ada\\:docSubject:'" + obj.prop_ada_docSubject + "'";
	}
	if (obj["prop_ada_docDate-date-range"].length > 0) {
		generatedQuery += " AND =@ada\\:docDate:" + translateDateRange(obj["prop_ada_docDate-date-range"]);
	}
	if (obj.prop_ada_docReference.length > 0) {
		generatedQuery += " AND =@ada\\:docReference:'" + obj.prop_ada_docReference + "'";
	}
	if (obj.prop_ada_docWorkDossier.length > 0) {
		generatedQuery += " AND =@ada\\:docWorkDossier:'" + obj.prop_ada_docWorkDossier + "'";
	}
	if (obj.prop_ada_docCaseNumber.length > 0) {
		generatedQuery += " AND =@ada\\:docCaseNumber:'" + obj.prop_ada_docCaseNumber + "'";
	}
	if (obj.prop_ada_employeeDepartment.length > 0) {
		generatedQuery += " AND =@ada\\:employeeDepartment:'" + obj.prop_ada_employeeDepartment + "'";
	}
}

if (obj.prop_ada_employeeNumber.length > 0) {
	generatedQuery += " AND =@ada\\:employeeNumber:'" + obj.prop_ada_employeeNumber + "'";
}
if (obj.prop_ada_employeeName.length > 0) {
	generatedQuery += " AND @ada\\:employeeName:'" + obj.prop_ada_employeeName + "'";
}
if (obj.prop_ada_employeeBsn.length > 0) {
	generatedQuery += " AND =@ada\\:employeeBsn:"+'"' + obj.prop_ada_employeeBsn + '"';
}
if (obj.prop_ada_employeeMrn.length > 0) {
	generatedQuery += " AND =@ada\\:employeeMrn:'" + obj.prop_ada_employeeMrn + "'";
}

main(generatedQuery);

function translateDateRange(dateString) {
	var rangeParts = dateString.split("|");
	var queryDateString = "[" + rangeParts[0] + " TO " + rangeParts[1] + "]";
	return queryDateString;
}

function isAdactaAdmin() {
	var isManager = false;
	var json = remote.call("/api/people/" + user.name + "?groups=true");
	
	if (json.status == 200) {
        var result = JSON.parse(json);
        for each (var group in result.groups) {
        	if (group.itemName === "GROUP_ADACTA_BEHEERDER" || group.itemName === "GROUP_ALFRESCO_ADMINISTRATORS") {
        		isManager = true;
        		break;
        	}
        }
    }
    return isManager;
}

/**
* Set user rights - has download permission.
* group LH02P01DISF04 (annoteren) is (ab)used for this function 
* 
* @property canDownload
* @type boolean
*/
function canDownload() {
	var canDownload = false;
	var json = remote.call("/api/people/" + user.name + "?groups=true");
	
	if (json.status == 200) {
        var result = JSON.parse(json);
        for each (var group in result.groups) {
        	if (group.itemName === "GROUP_LH02P01DISF04") {
        		canDownload = true;
        		break;
        	}
        }
    }
    return canDownload;
 }