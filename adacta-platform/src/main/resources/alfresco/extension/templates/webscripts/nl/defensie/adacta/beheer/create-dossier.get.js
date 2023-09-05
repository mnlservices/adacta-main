var adminGroup = 'GROUP_ALFRESCO_ADMINISTRATORS';
var beheerGroup = 'GROUP_CREATE_DOSSIER';
var groups = people.getContainerGroups(person);

var allow = false;
for (var i = 0; i < groups.length; i++) {
  var n = groups[i].properties["cm:authorityName"];
  if (n.indexOf(adminGroup) === 0
       || n.indexOf(beheerGroup) === 0) {
      allow = true;
      break;
   }
}

if (!allow) {
   throw 'User is not allowed to access this tool!';
}

var teller = 0;
var csv = args["csv"];
model.msg = '';
if (csv && csv.length > 0 && csv != 'null') {
	var csvfile = companyhome.childByNamePath("/Data dictionary/Beheer/createDossiers/"+csv);
	var parentFolder = companyhome.childByNamePath("/Data dictionary/Beheer/createDossiers/");
	var t = getDateString();
	var errorFile = parentFolder.createFile("error-create-dossier-"+t+".txt");
	var reportFile = parentFolder.createFile("report-create-dossier-"+t+".txt");
	var errorContent = errorFile.content;
	var reportContent = reportFile.content;

	if (!csvfile){
		model.msg = 'Csv bestand ' + csv +  ' niet gevonden in /Data dictionary/beheer/createDossiers/';	
	}else{	
		var lines = csvfile.content.split('\n');
		lines.forEach(function (line) {
			work(line);
		});
	}
}
if (csvfile && model.msg == ''){
	model.msg = teller+" dossiers aangemaakt";
}
if (errorContent){
	errorFile.content = errorContent;
}
if (reportContent){
	reportFile.content = reportContent;
}
function work(line){
	//check for empty lines
	if (line.indexOf(";") == -1){
		return;
	}
	var ar = line.split(";");
	var query = 'TYPE:"ada:dossier" AND @cm:name:"'+ar[0]+'"';
	var def = 
	  { 
	    query: query, 
	    store: "workspace://SpacesStore", 
	    language: "fts-alfresco"
	  }; 
	var results = search.query(def);
  	if (results && results[0]){
         errorContent += "dossier "+ar[0]+" bestaat al" + '\n';
         return;
    }
    if (recordIsValid(ar)){
   	   	createDossier(ar);
  	   	teller++;
  	}
}
function createDossier(ar){
	var targetFolder = getTargetFolder();  
	var dos = targetFolder.createFolder(ar[0],"ada:dossier");
	dos.properties["cm:name"]=ar[0];
	dos.properties["ada:employeeNumber"]=ar[1];
	dos.properties["ada:employeeName"]=ar[3];
	dos.properties["ada:employeeBsn"]=ar[0];
	dos.properties["ada:employeeMrn"]=ar[2];
	dos.properties["ada:employeeDepartment"]=ar[4];
	dos.properties["ada:employeeDpCodes"]=getDpCodes(ar[5]);
	dos.save();
	reportContent += "dossier "+dos.displayPath+"/"+ar[0]+" is gemaakt" + '\n';
	}
function randomHex() {
	  var possible = "0123456789ABCDEF";
	  return possible.charAt(Math.floor(Math.random() * possible.length));
}
function getTargetFolder(){
	  	var target=null;
	    var dossiers = companyhome.childByNamePath("/Sites/adacta/documentLibrary/Dossiers");
	  	var h1 = randomHex();
		var loc = "/Sites/adacta/documentLibrary/Dossiers/"+h1;
		var target = companyhome.childByNamePath(loc);
	  	if (!target){
	    	target = dossiers.createFolder(h1);
	  	}
	    var h2 = randomHex();
	  	loc = loc +"/"+h2;
		var target1 = companyhome.childByNamePath(loc);
	  	if (!target1){
	    	target1 = target.createFolder(h2);
	  	}
	    var h3 = randomHex();
	  	loc = loc +"/"+h3;
		var target2 = companyhome.childByNamePath(loc);
	  	if (!target2){
	    	target2 = target1.createFolder(h3);
	  	}
return target2;
}
function getDateString(){
	var d = new Date();
	return d.getFullYear()+"-"+(d.getMonth()+1)+"-"+d.getDate()+"-"+d.getHours()+"-"+d.getMinutes()+"-"+d.getSeconds();
}
function getDpCodes(codeString){
	return codeString.split(" ");
}

// veld 0 - bsn
// veld 1 - applid/emplid
// veld 2 - mrn
// veld 3 - naam
// veld 4 - organisatie
// veld 5 - dpCodes
function recordIsValid(ar){
	if (ar.length != 6){
		errorContent += "het aantal velden in de csv is ongelijk aan 6 ("+ar.length+")"   +'\n';
		return false;		
	}
	for (var i=0;i<6;i++){
		ar[i]= (ar[i]+"").trim();
	}
	//bsn
  var bsn = ar[0]+"";
	if ((bsn.length == 0) || (bsn.indexOf("NLD-") == -1)){
      logger.log("ongeldig bsn");
		errorContent += " ongeldig bsn gevonden "+bsn+"\n"; 
		return false;
    }
	//emplid
  var emplid = ar[1]+"";
	if (emplid.length != 11){
		errorContent += " ongeldig emplid/applid gevonden "+emplid+'\n';
		return false;
	}
	//naam
  var naam = ar[3]+"";
	if (naam.length == 0){
		errorContent += " record zonder naam gevonden "+'\n';
		return false;
	}
	//organisatie
  var org = ar[4]+"";
	if (org.length == 0){
		errorContent += " record zonder organisatie gevonden "+'\n';
		return false;
	}
	//dpCode
  var dpCode = ar[5]+"";
	if (dpCode.length == 0){
		errorContent += " record zonder dpCode gevonden "+'\n';
		return false;
	}

	return true;
}