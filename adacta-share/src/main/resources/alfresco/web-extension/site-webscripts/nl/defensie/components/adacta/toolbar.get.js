function main() {
    model.activePage = (page.url.templateArgs.pageid || "");

    model.links = [];

    var groups = getGroups();

    if (isAdactaUser(groups)) {
        addLink("search-link", "adacta-search", "link.search");
    }

    if (currentUserCanEdit(groups)) {
        addLink("import-link", "adacta-import", "link.import");
        addLink("index-link", "adacta-index", "link.index");
    }
    
    addLink("notice-link", "adacta-notice", "link.notice");
}

function getGroups() {
    var json = remote.call("/api/people/" + user.name + "?groups=true");
    if (json.status == 200) {
        var result = JSON.parse(json);
        return result;
    }
    return null;
}

function addLink(id, href, msgId, msgArgs) {
    model.links.push({
        id: id,
        href: href,
        cssClass: (model.activePage == href) ? "theme-color-4" : null,
        label: msg.get(msgId, msgArgs ? msgArgs : null)
    });
}

function currentUserCanEdit(result) {
    var canEdit = false;

    for each(var group in result.groups) {
        if (group.itemName === "GROUP_ADACTA_INVOERDER" || group.itemName === "GROUP_ADACTA_BEHEERDER" || group.itemName === "GROUP_ALFRESCO_ADMINISTRATORS") {
            canEdit = true;
            break;
        }
    }

    return canEdit;
}

function isAdactaUser(result) {
    var isUser = false;
    for each(var group in result.groups) {
        if (group.itemName === "GROUP_ADACTA_RAADPLEGER" || group.itemName === "GROUP_ADACTA_INVOERDER" || group.itemName === "GROUP_ADACTA_BEHEERDER" || group.itemName === "GROUP_ALFRESCO_ADMINISTRATORS") {
            isUser = true;
            break;
        }
    }
    return isUser;
}

main();