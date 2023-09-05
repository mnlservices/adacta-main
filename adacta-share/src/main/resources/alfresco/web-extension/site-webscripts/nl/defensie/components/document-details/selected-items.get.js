<import resource="classpath:/alfresco/templates/org/alfresco/import/alfresco-util.js">

function main() {
    var nodeRef = AlfrescoUtil.param('nodeRef');
    AlfrescoUtil.param('site', null);
    var documentDetails = AlfrescoUtil.getNodeDetails(model.nodeRef, model.site);
    if (documentDetails) {
        model.document = documentDetails.item;
    }
    getProgressSelectedItems(nodeRef);
}

function getProgressSelectedItems(currentNodeRef) {

    var json = getSelectedItems();
    if (json != null) {

    	model.totalItems = json.paging.totalItems;

        var selecteditems = json.data.selectedItems;
        for (var i = 0, il = selecteditems.length; i < il; i++) {
            var item = selecteditems[i];

            if (item.nodeRef.indexOf(currentNodeRef) > -1) {

                model.number = i + 1;
                break;
            }
        }
    }
}

function getSelectedItems() {
    var json = remote.call("/nl/defensie/adacta/preferences/selected-items/" + user.name);
    if (json.status == 200) {
        return JSON.parse(json);
    }
    return null;
}

main();