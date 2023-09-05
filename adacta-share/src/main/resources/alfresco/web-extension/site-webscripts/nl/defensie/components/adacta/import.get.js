function main() {

    var widget = {
        id: "DatatableImport",
        name: "ADACTA.DatatableImport",
        options: {
            searchType: "importPage",
            maxSearchResults: 250,
            rootFolder: getNodeRef()
        }
    };

    model.widgets = [widget];
    model.isAdactaUser = isAdactaUser();
}

function getNodeRef() {
    var aspect = "ada:rootImportAspect"
    var json = remote.call("/nl/defensie/adacta/search/rootfolder?aspect=" + aspect);
    if (json.status == 200) {
        var obj = JSON.parse(json);
        if (obj != null) {
            return obj.nodeRef;
        }
    }
    return null;
}

function isAdactaUser() {
    var isUser = false;
    var json = remote.call("/api/people/" + user.name + "?groups=true");
    if (json.status == 200) {
        var result = JSON.parse(json);
        for each(var group in result.groups) {
            if (group.itemName === "GROUP_ADACTA_INVOERDER" || group.itemName === "GROUP_ADACTA_BEHEERDER" || group.itemName === "GROUP_ALFRESCO_ADMINISTRATORS") {
                isUser = true;
                break;
            }
        }
    }
    return isUser;
}

main();