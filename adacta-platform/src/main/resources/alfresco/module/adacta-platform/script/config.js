/**
 * Configuration script to run all additional settings to make the functionality
 * work.
 */

function main() {

    adacta.info("Create data lists...");
    createImportDataList("datalist_categories.csv", "Categories", "Rubrieken", "adadl:categoryItem");
    createImportDataList("datalist_subjects.csv", "Subjects", "Onderwerpen", "adadl:subjectItem");

    adacta.info("Create root folders in document library...");
    var docLib = adacta.getSiteDocumentLibrary();
    var importRoot = adacta.makeFolders(docLib, "Importeer", true);
    var indexRoot = adacta.makeFolders(docLib, "Indexeer", true);
    var reportRoot = adacta.makeFolders(docLib, "Rapporten", true);

    importRoot.addAspect("ada:rootImportAspect");
    indexRoot.addAspect("ada:rootIndexAspect");
    reportRoot.addAspect("ada:rootReportAspect");

    var dossiersRoot = docLib.childByNamePath("Dossiers");
    if (dossiersRoot != null) {
        dossiersRoot.addAspect("ada:rootDossiersAspect");
    } else {
        throw "No folder exists with name 'Dossiers'.";
    }

    adacta.info("Create script rules...");
    createScriptRules(importRoot, indexRoot);

    adacta.info("Create Adacta groups and memberships...");
    createGroupsAndMemberships();

    adacta.info("Set permissions on folders...");

    // Adacta admins what to have limited permissions to personnel files.
    dossiersRoot.setPermission("Collaborator", "GROUP_ADACTA_BEHEERDER");
    dossiersRoot.setPermission("Collaborator", "GROUP_ADACTA_INVOERDER");

    importRoot.setInheritsPermissions(false);
    importRoot.setPermission("Collaborator", "GROUP_ADACTA_INVOERDER");
    importRoot.setPermission("Coordinator", "GROUP_ADACTA_BEHEERDER");

    indexRoot.setInheritsPermissions(false);
    indexRoot.setPermission("Collaborator", "GROUP_ADACTA_INVOERDER");
    indexRoot.setPermission("Coordinator", "GROUP_ADACTA_BEHEERDER");

    reportRoot.setInheritsPermissions(false);
    reportRoot.setPermission("Collaborator", "GROUP_ADACTA_INVOERDER");
    reportRoot.setPermission("Coordinator", "GROUP_ADACTA_BEHEERDER");

    // Disabled overwriting notice...
    var htmlNotice = createNotice();
    if (htmlNotice != null) {
        htmlNotice.setPermission("Collaborator", "GROUP_ADACTA_BEHEERDER");
    }
    adacta.info("Done!");
}

/**
 * Create notice for notice page.
 */

function createNotice() {
    var noticeName = "notice.html";
    var docLib = adacta.getSiteDocumentLibrary();

    var html = docLib.childByNamePath(noticeName);
    if (html == null) {
        adacta.info("Create adacta notice...");
        return adacta.importClasspathFile(docLib, noticeName);
    }

    return null;
}

/**
 * Create groups and add memberships.
 */

function createGroupsAndMemberships() {

    var raadpleger = "GROUP_ADACTA_RAADPLEGER";
    var invoerder = "GROUP_ADACTA_INVOERDER";
    var beheerder = "GROUP_ADACTA_BEHEERDER";

    adacta.createGroup(raadpleger);
    adacta.createGroup(invoerder);
    adacta.createGroup(beheerder);

    adacta.addGroupToGroup("LH02P01DISF03", raadpleger);
    adacta.addGroupToGroup("LH02P01DISF01", invoerder);
    adacta.addGroupToGroup("LH02P01DISF02", invoerder);
    adacta.addGroupToGroup("LH02P01DISF06", invoerder);
    adacta.addGroupToGroup("LH02P01DISF07", invoerder);
    adacta.addGroupToGroup("LH02P01DISF08", invoerder);
    adacta.addGroupToGroup("LH02P01DISF05", beheerder);
}

/**
 * Create rules on folders
 */

function createScriptRules(importRoot, indexRoot) {

    // Remove all rules
    adacta.removeAllRules(importRoot);
    adacta.removeAllRules(indexRoot);

    // Get scripts root
    var dictionaryScripts = adacta.getDictionaryScripts();

    // Create scripts
    var scriptRuleType = createScript("_adacta_rule_set_type.js");
    var scriptMimetypeBlocker = createScript("_adacta_mimetype_blocker.js");
    var scriptOwnerScan = createScript("_adacta_set_owner_scan.js");
    var scriptDepartmentGroup = createScript("_adacta_set_department_group.js");

    // Create rules on import folder
    adacta.createScriptActionRule(importRoot, scriptRuleType, "inbound", "Set type on documents", "Set ada:document type on documents.", false, true);
    adacta.createScriptActionRule(importRoot, scriptMimetypeBlocker, "inbound", "Mimetype blocker", "Accept only documents with mimetype application/pdf.", false, true);
    adacta.createScriptActionRule(importRoot, scriptDepartmentGroup, "inbound", "Set group permissions.", "Set group permissions.", false, true);

    // Create rules on index folder
    adacta.createScriptActionRule(indexRoot, scriptRuleType, "inbound", "Set type on documents", "Set ada:document type on documents.", false, true);
    adacta.createScriptActionRule(indexRoot, scriptOwnerScan, "inbound", "Set ownership", "Set ownership on documents and scanfolder.", false, true);
}

/**
 * Create script in data dictionary scripts folder
 * 
 * @param scriptName
 *            String name
 * @returns ScriptNode the created script.
 */

function createScript(scriptName) {
    var dictionaryScripts = adacta.getDictionaryScripts();
    var scriptNode = dictionaryScripts.childByNamePath(scriptName.replace("_", ""));
    if (scriptNode != null) {
        scriptNode.remove();
    }
    return adacta.importClasspathScript(dictionaryScripts, scriptName, scriptName.replace("_", ""));
}

/**
 * Create data list based on CSV file
 * 
 * @param csvFileName
 * @param dataListName
 * @param dataListTitle
 * @param dataListItemType
 */

function createImportDataList(csvFileName, dataListName, dataListTitle, dataListItemType) {
    // Get context
    var docLib = adacta.getSiteDocumentLibrary();
    var dataListContainer = adacta.getDataListContainer();

    // Create data list
    var dataList = createDataList(dataListContainer, dataListName, dataListTitle, dataListItemType);

    // Create CSV from classpath
    var csv = docLib.createNode(csvFileName, "cm:content");
    adacta.putFileInputStreamInNode(csv, csvFileName);

    // Import CSV
    adacta.importCsvToDataList(csv, dataList);

    // Remove file
    csv.addAspect("sys:temporary");
    csv.remove();
}

/**
 * Create data list with specific type.
 * 
 * @param container
 *            the data list container of a site
 * @param name
 *            of data list. It can be null.
 * @param title
 *            data list title
 * @param dataListItemType
 *            the type of data list
 */

function createDataList(container, name, title, dataListItemType) {

    var datalist = container.childByNamePath(name);
    if (datalist != null) {
        datalist.remove();
    }

    var datalistItem = container.createNode(name, "dl:dataList");
    datalistItem.properties["dl:dataListItemType"] = dataListItemType;
    datalistItem.properties["cm:name"] = name;
    datalistItem.save();

    var props = [];
    props["cm:title"] = title;
    datalistItem.addAspect("cm:titled", props);

    return datalistItem;
}

main();