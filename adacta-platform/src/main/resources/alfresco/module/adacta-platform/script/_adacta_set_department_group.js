/**
 * Inbound rule script set on import root folder (Adacta site). 
 */

if (document.parent.hasAspect("ada:rootImportAspect")) {
	adacta.setDepartmentGroupPermissions(person.properties.userName, document);
}