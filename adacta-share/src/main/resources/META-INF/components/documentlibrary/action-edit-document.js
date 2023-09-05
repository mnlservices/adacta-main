(function() {
    YAHOO.Bubbling.fire("registerAction", {
        actionName: "onActionEditDocument",
        fn: function adacta_onActionEditDocument(node) {

            var editDocUrl = Alfresco.util.siteURL("edit-document?nodeRef=" + node.nodeRef);
            window.location = window.location.protocol + "//" + window.location.hostname + editDocUrl;
        }
    });
})();