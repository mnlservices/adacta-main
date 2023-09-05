function main(query, category, subject, selfservice) {
	// Widget instantiation metadata
	var widget = {
		id : "AdactaPersonnelFile", 
		name : "Alfresco.AdactaPersonnelFile",
		options : {
			searchTerm : query,
			maxSearchResults: 250,
			category : category,
			subject : subject,
			searchType : "searchPage",
			selfservice : selfservice
	    }
	};
	model.widgets = [widget];
	model.filterCategory = getFilterCategoryList();
}

var bsn = page.url.args.id;
var psid = page.url.args.psid;
var regid = page.url.args.regid;
var category = page.url.args.category;
var subject = page.url.args.subject;
var selfservice = page.url.args.selfservice;
var empid = getEmployeeID();

var generatedQuery = "";

if (selfservice && selfservice=="true") {
    var generatedQuery = '(@ada\\:docCategory:"0?" OR @ada\\:docCategory:"1?") AND !@ada\\:docCategory:"06" AND ';
}
generatedQuery +="PATH:\"/app:company_home/st:sites/cm:adacta/cm:documentLibrary/cm:Dossiers//*\" "
 +" AND TYPE:\"ada:document\" "; 

 if (selfservice && selfservice=="true"){
	 generatedQuery += ' AND NOT @ada\\:docStatus:"Gesloten" ';
 }

 
if (bsn && bsn.length > 0) {
	generatedQuery += " AND @ada\\:employeeBsn:'" + bsn + "'";
}else if (psid && psid.length > 0) {
	generatedQuery += " AND @ada\\:employeeNumber:'" + psid + "'";
}else if (regid && regid.length > 0) {
	generatedQuery += " AND @ada\\:employeeMrn:'" + regid + "'";
}

// Go to self service page if no parameters are given
// empty query if empid is not found
if (bsn === null && psid === null && regid === null) {
	if (empid){
		generatedQuery += " AND @ada\\:employeeNumber:'" + empid + "'";
	}else{
		generatedQuery = "";
	}
}

function getFilterCategoryList() {
	var url = "";
	if (selfservice && selfservice=="true"){
		url ="/nl/defensie/adacta/categories/categoryItem?empid=" + empid;		
	}else{
		url ="/nl/defensie/adacta/categories/categoryItem?bsn="+bsn+"&psid="+psid+"&regid="+regid;
	}
	var json = remote.call(url);
	var cats = (JSON.parse(json)).items;
	if (cats && cats.length>0){
		return cats;
	}else{
		return [];
	}
}

function getEmployeeID() {
	var json = remote.call("/nl/defensie/adacta/user/" + user.name);
    return (JSON.parse(json)).employeeID;
}

main(generatedQuery, category, subject, selfservice);