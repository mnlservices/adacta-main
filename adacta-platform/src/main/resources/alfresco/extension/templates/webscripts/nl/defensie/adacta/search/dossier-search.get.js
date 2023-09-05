function main() {

    var results = new Array();
	var name = args["name"];
    var emplid = args["emplid"];
    var id = args["id"];
    var mrn = args["mrn"];

 	var baseQuery ="TYPE:\"ada:dossier\" AND PATH:\"/app:company_home/st:sites/cm:adacta/cm:documentLibrary/cm:Dossiers//*\"";
	var extraQuery ="";
	if (name){
		extraQuery = extraQuery + " AND @ada\\:employeeName:*'" + name +"' "; 
	}
	if (emplid){
		if (emplid.length !== 0 && emplid.length<11 ){
			if (emplid.indexOf("A")>-1){
				// A-type emplids are 8 long
			}else{
				while (emplid.length<11){
					emplid = '0'+emplid;
				}
			}
		}
		extraQuery = extraQuery + " AND @ada\\:employeeNumber:*'" + emplid +"' "; 
	}
	if (mrn){
		extraQuery = extraQuery + " AND @ada\\:employeeMrn:*'" + mrn +"' "; 
	}
	if (id){
		extraQuery = extraQuery + " AND @ada\\:employeeBsn:'" + id +"'"; 
	}
	var query = baseQuery+extraQuery;

    results = adacta.query({
        query: query,
        sort: [{
            column: "cm:name",
            ascending: true
        }]
    });

    if (results.length > 0) {
        model.items = results;
    }
    if (results.length == 0) {
		var properties =  {"ada:employeeName":"","ada:employeeMrn":"","ada:employeeBsn":"","ada:employeeNumber":""};
		results=[{"nodeRef":"no results","properties":properties}];
    	model.items = results;
    }
}
main();