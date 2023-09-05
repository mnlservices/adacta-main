function main() {
    var widget = {
        id: "DatatableIndex",
        name: "ADACTA.DatatableIndex",
        options: {
            searchType: "indexPage",
            maxSearchResults: 250
        }
    };
    model.widgets = [widget];
    model.isAdactaUser = isAdactaUser();
}
main();

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