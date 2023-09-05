(function() {
    YAHOO.Bubbling.fire("registerAction", {
        actionName: "onActionSelectedItemsNext",
        fn: function adacta_onActionSelectedItemsNext(node) {

            var onSuccess = function _onSuccess(response) {

                    var items = response.json.data.selectedItems;

                    var index = 0;
                    for (var i = 0, il = items.length; i < il; i++) {
                        var item = items[i];

                        // Are node refs equal?
                        if (node.nodeRef.indexOf(item.nodeRef) > -1) {

                            // Get the next one
                            if (index == i && index != (items.length - 1)) {
                                index++
                            }

                            // Create link
                            var editDocUrl = Alfresco.util.siteURL("document-details?nodeRef=" + items[index].nodeRef);
                            window.location = window.location.protocol + "//" + window.location.hostname + editDocUrl;
                        }
                        index++;
                    }
                };

            this.modules.actions.genericAction({
                success: {
                    event: {},
                    callback: {
                        fn: onSuccess,
                        scope: this
                    }
                },
                failure: {},
                webscript: {
                    name: "/nl/defensie/adacta/preferences/selected-items/{user}",
                    stem: Alfresco.constants.PROXY_URI,
                    method: Alfresco.util.Ajax.GET,
                    params: {
                        user: Alfresco.constants.USERNAME
                    }
                },
                config: {}
            });
        }
    });
})();