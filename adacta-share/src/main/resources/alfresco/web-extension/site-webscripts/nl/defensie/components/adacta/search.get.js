/**
 * Adacta Search component GET method
 */

function main() {
    // fetch the request params required by the advanced search component template
    var siteId = (page.url.templateArgs["site"] != null) ? page.url.templateArgs["site"] : "";

    // get the search forms from the config
    var formsElements = config.scoped["AdactaSearch"]["adacta-search"].getChildren("forms");
    var searchForms = [];

    for (var x = 0, forms; x < formsElements.size(); x++) {
        forms = formsElements.get(x).childrenMap["form"];

        for (var i = forms.size() - 1, form, formId, label, desc; i >= 0; i--) {
            form = forms.get(i);

            // get optional attributes and resolve label/description text
            formId = form.attributes["id"];

            label = form.attributes["label"];
            if (label == null) {
                label = form.attributes["labelId"];

                if (label != null) {
                    label = msg.get(label);
                }
            }

            desc = form.attributes["description"];
            if (desc == null) {
                desc = form.attributes["descriptionId"];

                if (desc != null) {
                    desc = msg.get(desc);
                }
            }

            // create the model object to represent the form definition
            searchForms.push({
                id: formId ? formId : "search",
                type: form.value,
                label: label ? label : form.value,
                description: desc ? desc : ""
            });
        }
    }

    // config override can force repository search on/off
    model.searchScope = siteId || "all_sites";
    model.siteId = siteId;
    model.searchForms = searchForms;
    model.searchPath = "context/adacta/adacta-search-results?query={query}";

    // Widget instantiation metadata...
    var adactaSearch = {
        id: "AdactaSearch",
        name: "Alfresco.AdactaSearch",
        options: {
            siteId: model.siteId,
            savedQuery: (page.url.args.sq != null) ? page.url.args.sq : "",
            searchScope: model.searchScope,
            searchForms: model.searchForms,
            searchPath: model.searchPath
        }
    };
    model.widgets = [adactaSearch];
    model.isAdactaUser = isAdactaUser();
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

main();