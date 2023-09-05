/**
 * Create dummy scanbatches.
 */

function main() {

	adacta.info("Create batches...");
	createBatches();
	
	adacta.info("Done!");
}

/**
 * Create scanbatches.
 */
function createBatches() {
	scanBatches.forEach(createBatch);
}

/**
 * Create one batch with documents.
 * @param batch
 */
function createBatch(batch) {
	
	// Get the root folder
	var rootIndex = adacta.getRootIndex();
		
	// Create batch folder
	var folderName = batch.scanEmployee + "_" + batch.scanWaNr + "_" + batch.scanSeqNr;
	var folder = adacta.makeFolders(rootIndex, folderName);
	
    // Set props
	var props = [];
    props["ada:scanEmployee"] = batch.scanEmployee;
    props["ada:scanSeqNr"] = batch.scanSeqNr;
    props["ada:scanWaNr"] = batch.scanWaNr;
    props["ada:scanBatchName"] = folderName;
    
    // Create some dummy files in folder.
    createBatchFiles(folder, 5, props);
}

/**
 * Create dummy batch files. 
 * @param folder
 * @param total
 */
function createBatchFiles(folder, total, props) {
	
    for (var i = 0; i < total; i++) {
        var pdfName = "example_" + Math.floor(Math.random() * 1000000) + ".pdf";
        var doc = adacta.importClasspathFile(null, folder, pdfName);        
        props["ada:docDateCreated"] = new Date();
        props["ada:scanBatchSize"] = total;
        doc.addAspect("ada:scanAspect", props);
        adacta.log("Importing batch file " + pdfName + ".");
    }
}

/**
 * List of batches to create.
 */
var scanBatches = [{
    "scanEmployee": "u000001",
    "scanSeqNr": "1001",
    "scanWaNr": "WA1000001"
},{
    "scanEmployee": "u000002",
    "scanSeqNr": "1002",
    "scanWaNr": "WA1000002"
},{
    "scanEmployee": "u000004",
    "scanSeqNr": "1004",
    "scanWaNr": "WA1000004"
}];

main();