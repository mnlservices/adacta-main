/**
 * Inbound rule script set on index folder. We are not receiving the Kofax fields with the standard integration. We have to extract the fields from the folder name.  
 */
function main() {
	if (document && document.isSubType("cm:content")) {

		// Set username on folder
		var folder = document.parent;
		var folderName = document.properties["ada:scanBatchName"];
		var folderSize = folder.children.length;

		var userName = "";

		var parts = folderName.split("_");

		if (parts.length === 3) {
			userName = parts[0].toString();
		} else {
			logger.warn("Folder name not correct.")
		}

		if (userName !== null && userName !== "") {
			var batchSize = document.properties["ada:scanBatchSize"];

			if (batchSize === folderSize) {
				var props = [];
				props["ada:scanEmployee"] = userName;
				props["ada:scanWaNr"] = parts[1];
				props["ada:scanSeqNr"] = parts[2];
				folder.addAspect("ada:scanAspect", props);

				// Disabled and replaced: does not meet spec
				//adacta.setDepartmentGroupPermissions(userName, folder);

				// FIXED AFTER GO-LIVE (RFCE-180206020)
				folder.setPermission('Coordinator', userName);
				folder.setOwner(userName);
				grantDepartmentAccess(folder, userName);
				
				// Set date on child documents
				folder.children.forEach(function (child) {
					if (child.typeShort !== 'ada:document') {return;}
					child.properties["ada:docDateCreated"] = child.properties["cm:created"];
					child.save();
				});
				// END RFCE-180206020 FIX
			}
		} else {
			adacta.warn("No username exist on document " + document.name);
		}
	}
}

/**
 * Find the scan group for this batch and provide it with access to the batch.
 */
function grantDepartmentAccess(batchFolder, scanUser) {
	var owner = people.getPerson(scanUser);
	var org = owner.properties["cm:organization"];
	if (!org || org.length < 1) {
		logger.log('Scanner has no organization for ' + child.name);
		return;
	}
	
	var grp = getScanGroup(org);
	
	if (grp !== null) {
		batchFolder.setPermission('Coordinator', grp.properties["cm:authorityName"]);
	}
}

/**
 * Scanbatches should be shared with different levels within the organization depending on which organization they are a part of.
 * CLAS, CLSK, DMO, KMAR: 2nd level
 * DOSCO, CZSK, BS: 3rd level
 * Any batch scanned by someone on a higher level or not included in this 
 * logic will be shared with the lowest level department that the scan user is part of (user's cm:organization).
 */
function getScanGroup(org) {
	var spl = org.split('/');
	var displayName = null;
	var up = org.toUpperCase();
	
	if (up.indexOf('DOSCO') === 0 ||
	up.indexOf('CZSK') === 0 ||
	up.indexOf('BS') === 0) {
		// Derde niveau
		if (spl.length < 3) {
			// Original
            displayName = org;
		} else {
			// 3rd level
			displayName = spl.splice(0, 3).join('/');
		}
	} else if (up.indexOf('CLAS') === 0 ||
	up.indexOf('CLSK') === 0 ||
	up.indexOf('DMO') === 0 ||
	up.indexOf('KMAR') === 0) {
		// Tweede niveau
		if (spl.length < 2) {
			// Original
            displayName = org;
		} else {
			// 2rd level
			displayName = spl.splice(0, 2).join('/');
		}
	} else {
		// Unknown. Use original.
		displayName = org;
	}
	
	displayName += ' (Afdeling-rolgroep)';
	
	// Retrieve group node
	// Allow expansion to support case insensitive matching, filter out unruly matches later
	var q = '=TYPE:"cm:authorityContainer" AND +@cm\\:authorityDisplayName:"' + displayName + '"';
	var results = search.query({
		store: 'workspace://SpacesStore',
		language: 'fts-alfresco',
		query: q
	});
	
	// In case of multiple matches: there can be only one
	for (var i = 0; i < results.length; i++) {
		var result = results[i];
		var dName = result.properties["cm:authorityDisplayName"];
		if (dName.toLowerCase() === displayName.toLowerCase()) {
			return result;
		}
	}
	
	logger.log('No matching group for ' + displayName);
	return null;
}

main();