(function() {

    /** Module creation and method registration */
    Alfresco.module.onActionDeleteDocumentDialog = function() { /** Load needed components */
        Alfresco.module.onActionDeleteDocumentDialog.superclass.constructor.call(
        this, "Alfresco.module.onActionDeleteDocumentDialog", this.id, ["button", "container", "connection"]);

        /** Register action */
        YAHOO.Bubbling.fire("registerAction", {
            actionName: "onActionDeleteDocumentDialog",
            fn: ddDialog.onActionDeleteDocumentDialog
        });
        return this;
    };

    /** Extending the module. */
    YAHOO.extend(
    Alfresco.module.onActionDeleteDocumentDialog, Alfresco.component.Base, ddDialog = {

        // Show the custom action dialog and register its
        // callback handler.
        onActionDeleteDocumentDialog: function(record, owner) {
            var action = this.getAction(record, owner);

            var displayName = record.displayName;

            // Show the dialog
            Alfresco.util.PopupManager.displayForm({
                title: record.displayName,
                properties: {
                    mode: "create",
                    itemKind: "action",
                    itemId: "adactaDeleteNode",
                    destination: record.nodeRef
                },
                success: {
                    fn: function(callback) {
                        ddDialog.handleCallback(callback, displayName);
                    },
                    scope: ddDialog
                },
                failure: {
                    fn: function(callback) {
                        ddDialog.handleCallback(callback, displayName);
                    },
                    scope: ddDialog
                }
            });
        },

        /**
         * Handles the returned information from the server.
         */
        handleCallback: function(callback, displayName) {
            if (callback.serverResponse.status === 200) {
                Alfresco.util.PopupManager.displayMessage({
                    text: this.msg("message.delete.success", displayName),
                    displayTime: 3
                });
                
                var persistedObject = callback.json.persistedObject;
                var parts = persistedObject.split("result=");
               var context = parts[1];
                if (context == null) {
                	// default page
                	context = "adacta-search";
                	window.location = Alfresco.util.siteURL(context);
                }else{
	                   // Redirect to remaining batch
                    window.location = context;
				}   
            } else {
                Alfresco.util.PopupManager.displayMessage({
                    text: this.msg("message.delete.failure", displayName),
                    displayTime: 3
                });
            }
        },

        msg: function Base_msg(messageId) {
            return Alfresco.util.message.call(this, messageId, this.name, Array.prototype.slice.call(arguments).slice(1));
        }
    });

    /** First trigger */
    new Alfresco.module.onActionDeleteDocumentDialog();
})();