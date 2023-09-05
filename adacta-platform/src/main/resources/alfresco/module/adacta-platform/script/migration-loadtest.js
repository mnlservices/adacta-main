/**
 * This script will create additional folders, files and users on top
 * of the initial load. The purpose is to test the performance of the 
 * configuration and migration scripts at high load.
 * 
 * The logic in the initial setup script is applied, but the dummy metadata
 * is generated inside the script instead of being loaded from files. This
 * allows for flexibility regarding the total number of entries.
 */

function main() {

    // Batch settings for all used batches
    var batchSize = 250;
    var threads = 4;

    var startIndex = 6;
    var targetSize = 10000;
    var docsPerDossier = 1;
    
    var createUsersAndMemberships = false;
    
    // CSV file names
    var csvUsers = "users.csv";
    var csvMemberships = "memberships.csv";
    var csvFolders = "folders.csv";
    var csvFiles = "files.csv";

    if (createUsersAndMemberships) {
    	adacta.info("Create users...");
        createTestUsers(csvUsers, batchSize, threads, startIndex, targetSize);

        adacta.info("Add memberships...");
        addMemberships(csvMemberships, batchSize, threads, startIndex, targetSize);
    }

    adacta.info("Create dummy files and folders...");
    var dossiersCreated = createDummyFolders(csvFolders, batchSize, threads, startIndex, targetSize);
    createDummyFiles(csvFiles, batchSize, threads, dossiersCreated, startIndex, targetSize, docsPerDossier);
    
    adacta.info("Done!");
}

/**
 * Create dummy files and folders that match the environments of Adacta 2.0.
 * 
 * @param csvFileName
 * @param batchSize
 * @param threads
 */

function createDummyFolders(csvFileName, batchSize, threads, startIndex, targetSize) {
    // Get all header information from CSV
    var headerQnames = adacta.getHeaderQNamesFromCsvFile(csvFileName);

    // Create a counter
    var counter = 1;

    // Get the site Dossiers folder node
    var docLib = adacta.getSiteDocumentLibrary();
    var rootFolder = docLib.childByNamePath("Dossiers");

    var nodes = new java.util.HashMap();

    // Get index of the column with names (cm:name)
    var nameIndex = 0;
    for (var i = 0; i < headerQnames.length; i++) {
    	if (headerQnames[i] === "{http://www.alfresco.org/model/content/1.0}name") {
    		nameIndex = i;
    		break;
    	}
    }
    
    var lines = [];
    
    for (var i = startIndex; i <= targetSize; i++) {
    	var numericId = ("0000000" + i).substr(-8);
    	var line = "NLD-1" + numericId + ";" + ("0000000000" + i).substr(-11) + ";Herplaatsingsadviseur, Hans;NLD-1" + 
    		numericId + ";6" + numericId + ";CO";
    	lines.push(line);
    }
    
    // Execute batch
    batchExecuter.processArray({
        items: lines,
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

function createDummyFiles(csvFileName, batchSize, threads, newNodes, startIndex, targetSize, docsPerDossier) {
    // Get all header information from CSV
    var headerQnames = adacta.getHeaderQNamesFromCsvFile(csvFileName);

    // Create a counter
    var counter = 1;

    // Get the site Dossiers folder node
    var docLib = adacta.getSiteDocumentLibrary();
    var rootFolder = docLib.childByNamePath("Dossiers");
    
    var subjectsList = ["0201","0301","0302","0401","0402","0403","1101","1301","1401","1402","1403","1404","0501","0601","0701","0702"];
    var datesList = ["1998-06-01;;;;2008-03-18;;2013-07-15","2002-08-11;;;;2010-02-21;;2015-07-25","2004-06-24;;;;2010-05-14;;2015-02-15","2009-01-25;;;;2012-09-18;;2014-07-03","2002-09-01;;;;2015-03-18;;2016-04-02"];
    
    // Generate randomness for the dates and the categories + subjects
    // The minimum and the maximum values are inclusive
	var minSub = 0, minDates = 0;
	var maxSub = 10, maxDates = 4;
    
    var lines = [];
    
    for (var i = startIndex; i <= targetSize; i++) {
    	for (var j = 0; j < docsPerDossier; j++) {
    		var randomSubIndex = Math.floor(Math.random() * (maxSub - minSub + 1)) + minSub;
        	var subject = subjectsList[randomSubIndex];
        	var catgory = subject.substring(0,2);
        	
        	var randomDatesIndex = Math.floor(Math.random() * (maxDates - minDates + 1)) + minDates;
        	var datesString = datesList[randomDatesIndex];
        	
        	var numericId = ("0000000" + i).substr(-8);
        	var line = "NLD-1" + numericId + ";" + ("0000000000" + i).substr(-11) + ";Herplaatsingsadviseur, Hans;NLD-1" + 
        		numericId + ";6" + numericId + ";CO;" + catgory + ";" + subject + ";" + datesString + ";Actief";
        	lines.push(line);
    	}
    }
    
    // Execute batch
    batchExecuter.processArray({
        items: lines,
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

function createTestUsers(csvFileName, batchSize, threads, startIndex, targetSize) {

    // Get all header information from CSV
    var headerQnames = adacta.getHeaderQNamesFromCsvFile(csvFileName);

    // Create a counter
    var counter = 1;
    
    var lines = [];
    
    for (var i = startIndex; i <= targetSize; i++) {
    	var line = "u" + ("00000" + i).substr(-6) + ";support@contezza.nl;Hans;Herplaatsingsadviseur;" + ("0000000000" + i).substr(-11);
    	lines.push(line);
    }
    
    // Execute batch
    batchExecuter.processArray({
        items: lines,
        batchSize: batchSize,
        threads: threads,
        onNode: function(line) {
            adacta.createUser(counter, line, headerQnames);
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

function addMemberships(csvFileName, batchSize, threads, startIndex, targetSize) {

    // Get all header information from CSV
    var headerNames = adacta.getHeaderNamesFromCsvFile(csvFileName);

    // Create a counter
    var counter = 1;
    
    var lines = [];
    
    for (var i = startIndex; i <= targetSize; i++) {
    	var line = "u" + ("00000" + i).substr(-6) + ";R00563357;false;";
    	lines.push(line);
    }
    
    // Execute batch
    batchExecuter.processArray({
        items: lines,
        batchSize: batchSize,
        threads: threads,
        onNode: function(line) {
            adacta.addMembership(counter, line, headerNames);
            counter++;
        }
    });
}

main();