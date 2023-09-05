var conn = remote.connect("alfresco");
var sitesMenu = widgetUtils.findObject(model.jsonModel, "id", "HEADER_SITES_MENU");
if (!canCreateSite()) {
    sitesMenu.config.showCreateSite = false;
}
if (siteExists(conn)) {
    var menuBar = widgetUtils.findObject(model.jsonModel, "id", "HEADER_APP_MENU_BAR");

    if (isAdactaUser()) {
        if (menuBar != null) {
            menuBar.config.widgets.push({
                name: "alfresco/menus/AlfMenuBarItem",
                config: {
                    label: msg.get("label.menu.adacta-console"),
                    targetUrl: "context/adacta/adacta-search"
                }
            });
        }
    } else {
        if (menuBar != null) {
            menuBar.config.widgets.push({
                name: "alfresco/menus/AlfMenuBarItem",
                config: {
                    label: msg.get("label.menu.adacta-console"),
                    targetUrl: "context/adacta/adacta-notice"
                }
            });
        }
    }
}

function siteExists(conn) {
    var res = conn.get("/api/sites/adacta");
    if (res.status == 404) {
        // site does not exist yet
        return false;
    } else if (res.status == 200) {
        // site already exists
        return true;
    }
}

function isAdactaUser() {
    var isUser = false;
    var json = remote.call("/api/people/" + user.name + "?groups=true");
    if (json.status == 200) {
        var result = JSON.parse(json);
        for each(var group in result.groups) {
            if (group.itemName === "GROUP_ADACTA_RAADPLEGER" || group.itemName === "GROUP_ADACTA_INVOERDER" || group.itemName === "GROUP_ADACTA_BEHEERDER" || group.itemName === "GROUP_ALFRESCO_ADMINISTRATORS") {
                isUser = true;
                break;
            }
        }
    }
    return isUser;
}
function canCreateSite() {
    var canCreate = false;
    var json = remote.call("/api/people/" + user.name + "?groups=true");
    if (json.status == 200) {
        var result = JSON.parse(json);
        for each(var group in result.groups) {
            if (group.itemName === "GROUP_SITE_CREATORS"  || group.itemName === "GROUP_ALFRESCO_ADMINISTRATORS") {
                canCreate = true;
                break;
            }
        }
    }
    return canCreate;
}
