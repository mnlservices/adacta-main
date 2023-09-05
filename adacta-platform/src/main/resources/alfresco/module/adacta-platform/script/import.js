/**
 * Import dummy files in the import folder. 
 */

function main() {

    var total = 10;
    var rootImport = adacta.getRootImport();

    for (var i = 0; i < total; i++) {
        var pdfName = "example_" + Math.floor(Math.random() * 1000000) + ".pdf";
        adacta.importClasspathFile(null, rootImport, pdfName);
        adacta.log("Importing file " + pdfName + ".");
    }
}
main();