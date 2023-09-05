(function() {

    /** Module creation and method registration */
    Alfresco.module.onActionRedirectFormDialog = function() { /** Load needed components */
        Alfresco.module.onActionRedirectFormDialog.superclass.constructor.call(
        this, "Alfresco.module.onActionRedirectFormDialog", this.id, ["button", "container", "connection"]);

        /** Register action */
        YAHOO.Bubbling.fire("registerAction", {
            actionName: "onActionRedirectFormDialog",
            fn: rdDialog.onActionRedirectFormDialog
        });
        return this;
    };

    /** Extending the module. */
    YAHOO.extend(
    Alfresco.module.onActionRedirectFormDialog, Alfresco.component.Base, rdDialog = {

        // Show the custom action dialog and register its
        // callback handler.
        onActionRedirectFormDialog: function(record, owner) {
            var action = this.getAction(record, owner),
                actionId = action.params.itemId;

            // Show the dialog
            Alfresco.util.PopupManager.displayForm({
                title: Alfresco.util.message("actions." + actionId + ".label"),
                properties: {
                    mode: "create",
                    itemKind: "action",
                    itemId: actionId,
                    destination: action.params.destination
                },
                success: {
                    fn: function(callback) {
                        rdDialog.handleCallback(callback, actionId);
                    },
                    scope: rdDialog
                },
                failure: {
                    fn: function(callback) {
                        rdDialog.handleCallback(callback, actionId);
                    },
                    scope: rdDialog
                }
            });
        },

        /**
         * Handles the returned information from the server.
         */
        handleCallback: function(callback, actionId) {
            var titleLabel = "actions." + actionId + ".label";
            var subMessage = Alfresco.util.message("actions." + actionId + ".success");

            if (callback.serverResponse.status === 200) {
                var persistedObject = callback.json.persistedObject;
                // do we have remaining noderefs?
                if (persistedObject.indexOf("result=http") > -1) {

                    // Show message.
                    Alfresco.util.PopupManager.displayMessage({
                        text: Alfresco.util.message("actions." + actionId + ".success"),
                        displayTime: 3
                    });

                    // Redirect to remaning batch
                    var parts = persistedObject.split("result=");
                    var url = parts[1];
                    window.location = url;

                    // Show message.
                    // Alfresco.util.PopupManager.displayPrompt({
                    // title: Alfresco.util.message(titleLabel),
                    // text: subMessage,
                    // buttons: [{
                    // text: Alfresco.util.message("button.ok"),
                    // handler: function() {
                    // // Redirect to remaning batch
                    // var parts = persistedObject.split("result=");
                    // var url = parts[1];
                    // window.location = url;
                    // },
                    // isDefault: true
                    // }]
                    //  });
                } else {
                    YAHOO.Bubbling.fire("metadataRefresh");

                    // Show message.
                    Alfresco.util.PopupManager.displayMessage({
                        text: Alfresco.util.message("actions." + actionId + ".success"),
                        displayTime: 3
                    });

                    // Show message.
                    // Alfresco.util.PopupManager.displayPrompt({
                    // title: Alfresco.util.message(titleLabel),
                    // text: subMessage
                    // });
                }
            } else {
                Alfresco.util.PopupManager.displayMessage({
                    text: Alfresco.util.message("actions." + actionId + ".failure"),
                    displayTime: 3
                });
            }
        }
    });

    /** First trigger */
    new Alfresco.module.onActionRedirectFormDialog();
})();