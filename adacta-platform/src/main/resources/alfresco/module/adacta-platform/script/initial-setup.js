/**
 * This script will create the initial structure of the Adacta personnel files. The
 * current environments already have folders and documents imported using the
 * bulk importer.
 */

function main() {

    // Batch settings for all used batches
    var batchSize = 250;
    var threads = 4;

    // CSV file names
    var csvFolders = "folders.csv";
    var csvFiles = "files.csv";
    var csvUsers = "users.csv";
    var csvGroups = "groups.csv";
    var csvMemberships = "memberships.csv";

    adacta.info("Create users...");
    createTestUsers(csvUsers, batchSize, threads);

    adacta.info("Create groups...");
    createTestGroups(csvGroups, batchSize, threads);

    adacta.info("Create dummy files and folders...");
    var dossiersCreated = createDummyFolders(csvFolders, batchSize, threads);
    createDummyFiles(csvFiles, batchSize, threads, dossiersCreated);
    
    adacta.info("Add memberships...");
    addMemberships(csvMemberships, batchSize, threads);
    
    adacta.info("Create DIS_P8 tables...");
    createDatabaseTables("create_disp8_tables.sql");
    
    adacta.info("Done!");
}

/**
 * Execute static SQL statement.
 * @param fileName
 */
function createDatabaseTables(fileName) {	
	var sql = adacta.getFileFromClasspath(fileName);	
	adacta.dbExecute(sql);
}

/**
 * Create dummy files and folders that match the environments of Adacta 2.0.
 * 
 * @param csvFileName
 * @param batchSize
 * @param threads
 */

function createDummyFolders(csvFileName, batchSize, threads) {
    // Get all header information from CSV
    var headerQnames = adacta.getHeaderQNamesFromCsvFile(csvFileName);

    // Create a counter
    var counter = 1;

    // Get the site doclib
    var docLib = adacta.getSiteDocumentLibrary();

    // Make root location in new transaction so that the batch processor knows
    // it exists
    var rootFolder = adacta.makeFolders(docLib, "Dossiers", true);

    var nodes = new java.util.HashMap();

    // Get index of the column with names (cm:name)
    var nameIndex = 0;
    for (var i = 0; i < headerQnames.length; i++) {
    	if (headerQnames[i] === "{http://www.alfresco.org/model/content/1.0}name") {
    		nameIndex = i;
    		break;
    	}
    }
    
    // Execute batch
    batchExecuter.processArray({
        items: adacta.getFileFromClasspath(csvFileName).split("\n"),
        batchSize: batchSize,
        threads: threads,
        onNode: function(line) {
            // Create files and personnel files
            var newNode = adacta.createDummyFileFolder(rootFolder, counter, line, headerQnames, null);
            var name = line.split(";")[nameIndex];
            
            nodes[name] = ("" + newNode).toString();
            
            // Get the next line
            counter++;
        }
    });
    
    return nodes;
}

/**
 * Create dummy files that match the environments of Adacta 2.0.
 * 
 * @param csvFileName
 * @param batchSize
 * @param threads
 */

function createDummyFiles(csvFileName, batchSize, threads, newNodes) {
    // Get all header information from CSV
    var headerQnames = adacta.getHeaderQNamesFromCsvFile(csvFileName);

    // Create a counter
    var counter = 1;

    // Get the site doclib
    var docLib = adacta.getSiteDocumentLibrary();

    // Make root location in new transaction so that the batch processor knows
    // it exists
    var rootFolder = adacta.makeFolders(docLib, "Dossiers", true);
    
    // Execute batch
    batchExecuter.processArray({
        items: adacta.getFileFromClasspath(csvFileName).split("\n"),
        batchSize: batchSize,
        threads: threads,
        onNode: function(line) {
            // Create files and personnel files
            adacta.createDummyFileFolder(rootFolder, counter, line, headerQnames, newNodes);
            
            // Get the next line
            counter++;
        }
    });
}

/**
 * Create test users.
 * @param csvFileName
 * @param batchSize
 * @param threads
 */

function createTestUsers(csvFileName, batchSize, threads) {

    // Get all header information from CSV
    var headerQnames = adacta.getHeaderQNamesFromCsvFile(csvFileName);

    // Create a counter
    var counter = 1;

    // Execute batch
    batchExecuter.processArray({
        items: adacta.getFileFromClasspath(csvFileName).split("\n"),
        batchSize: batchSize,
        threads: threads,
        onNode: function(line) {
            adacta.createUser(counter, line, headerQnames);
            counter++;
        }
    });
}

/**
 * Create test groups.
 * @param csvFileName
 * @param batchSize
 * @param threads
 */

function createTestGroups(csvFileName, batchSize, threads) {

    // Get all header information from CSV
    var headerQnames = adacta.getHeaderQNamesFromCsvFile(csvFileName);

    // Create a counter
    var counter = 1;

    // Execute batch
    batchExecuter.processArray({
        items: adacta.getFileFromClasspath(csvFileName).split("\n"),
        batchSize: batchSize,
        threads: threads,
        onNode: function(line) {
            adacta.createGroup(counter, line, headerQnames);
            counter++;
        }
    });
}

/**
 * Add memberships.
 * 
 * @param csvFileName
 * @param batchSize
 * @param threads
 */

function addMemberships(csvFileName, batchSize, threads) {

    // Get all header information from CSV
    var headerNames = adacta.getHeaderNamesFromCsvFile(csvFileName);

    // Create a counter
    var counter = 1;

    // Execute batch
    batchExecuter.processArray({
        items: adacta.getFileFromClasspath(csvFileName).split("\n"),
        batchSize: batchSize,
        threads: threads,
        onNode: function(line) {
            adacta.addMembership(counter, line, headerNames);
            counter++;
        }
    });
}

main();