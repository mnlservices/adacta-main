var adminGroup = 'GROUP_ALFRESCO_ADMINISTRATORS';
var beheerGroup = 'GROUP_ADACTA_BEHEERDER';
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

var uname = args["uaccount"];
model.msg = '';

if (uname && uname.length > 0) {
	var q = '+cm\\:userName:"' + uname + '"';
	var result = search.query({
		language: 'fts-alfresco',
		store: 'workspace://SpacesStore',
		query: q
	});

	model.msg = '';
	if (result.length === 1) {
		var uaccount = result[0].properties['cm:userName'];
                var orga = result[0].properties['cm:organization'];
		var dp = result[0].properties['ada:dpCode'];

		if (!dp || dp.length < 1) {
			model.msg = 'Geen DP-CODE voor ' + uaccount + ', is user ADACTA GBR?';
		} else {
			model.msg = 'DP-CODE voor ' + uaccount + ': \t ' + dp;
		}

                if (orga && orga.length > 0) {
                    model.msg += '<br />' + orga;
                }
	} else if (result.length === 0) {
		model.msg = 'User ' + uname + ' niet gevonden.';
	} else {
		model.msg = 'Kon u-nummer ' + uname + ' niet herleiden tot een enkele user';
	}
}