function main() {

    var dashlet = {
        id: "OverviewIndexReports",
        name: "ADACTA.OverviewIndexReports",
        options: {
        	searchType: "overviewIndexReports",
        	isAdactaAdminOrImporter: isAdactaAdminOrImporter()
        }
    };

    var dashletTitleBarActions = {
        id: "DashletTitleBarActions",
        name: "Alfresco.widget.DashletTitleBarActions",
        useMessages: false,
        options: {
            actions: [{
                cssClass: "help",
                bubbleOnClick: {
                    message: msg.get("dashlet.help")
                },
                tooltip: msg.get("dashlet.help.tooltip")
            }]
        }
    };
    model.widgets = [dashlet, dashletTitleBarActions];
}


function isAdactaAdminOrImporter() {
	var isBeheerder = false;
	var json = remote.call("/api/people/" + user.name + "?groups=true");
	
	if (json.status == 200) {
        var result = JSON.parse(json);
        for each (var group in result.groups) {
        	if (group.itemName === "GROUP_ADACTA_BEHEERDER" || group.itemName === "GROUP_ADACTA_INVOERDER"
        			|| group.itemName === "GROUP_ALFRESCO_ADMINISTRATORS") {
        		isBeheerder = true;
        		break;
        	}
        }
    }
    return isBeheerder;
}

main();