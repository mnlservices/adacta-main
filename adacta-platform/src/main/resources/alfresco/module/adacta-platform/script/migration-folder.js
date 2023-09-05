/**
 * Script for transforming all personnel file folders to ada:dossier type. 
 */

function main() {
	// Batch settings
    var batchSize = 250;
    var threads = 4;
    
	updateMigratedDossiers(batchSize, threads);
}

main();

/**
 * Create dossiers from initially migrated folders
 */

function updateMigratedDossiers(batchSize, threads) {
	var docLib = adacta.getSiteDocumentLibrary();
	var firstLevelNode = docLib.childByNamePath("Dossiers");
    
    batchExecuter.processFolderRecursively({
        root: firstLevelNode,
        batchSize: batchSize,
        threads: threads,
        onNode: function(node) {
        	// Check if the path contains 9 slashes - which means the level is correct (4 levels deep inside the root folder)
        	// e.g. /app:company_home/st:sites/cm:adacta/cm:documentLibrary/cm:Dossiers/cm:0/cm:a/cm:b/NLD-00000
        	var count = ((node.qnamePath).match(/\//g)).length;
            if (count === 9) {
            	adacta.changeFolderTypeAndMetadata(node);
            }
        }
    });
}