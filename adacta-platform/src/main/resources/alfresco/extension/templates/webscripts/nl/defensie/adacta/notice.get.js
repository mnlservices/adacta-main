function main() {

    var docLib = adacta.getSiteDocumentLibrary();
    var node = docLib.childByNamePath("notice.html");

    if (node != null) {
        model.contentNode = node;
    } else {
    	status.setCode(status.STATUS_NOT_FOUND, "Document not found.");
    }
}

main();