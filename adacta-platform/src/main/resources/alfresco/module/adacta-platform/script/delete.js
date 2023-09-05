/**
 * Script to cleanup all entities created in the initial setup. 
 */

function main() {

    adacta.info("Delete document library...");
    var docLib = adacta.getSiteDocumentLibrary();
    deleteFolders(docLib);

    adacta.info("Delete data lists...");
    var dataLists = adacta.getDataListContainer();
    deleteFolders(dataLists);

    adacta.info("Delete adacta scripts...");
    var dictionaryScripts = adacta.getDictionaryScripts();
    deleteScripts(dictionaryScripts);

    adacta.info("Done!");
}

function deleteScripts(parent) {
    var children = parent.children;

    for (var i in children) {
        var child = children[i];

        if (child.name.indexOf("adacta") > -1) {
            child.remove();
        }
    }
}

function deleteFolders(parent) {
    var children = parent.children;

    for (var i in children) {
        var child = children[i];
        child.remove();
    }
}

main();