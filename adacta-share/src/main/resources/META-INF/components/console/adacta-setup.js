if (typeof(ADACTA) == "undefined") ADACTA = {};

(function() {
    /**
     * YUI Library aliases
     */
    var Dom = YAHOO.util.Dom,
        Event = YAHOO.util.Event;

    /**
     * Alfresco Slingshot aliases
     */
    var $html = Alfresco.util.encodeHTML;

    /**
     * Constructor.
     * 
     * @param {String}
     *            htmlId The HTML id of the parent element
     * @return {ADACTA.Setup} The new component instance
     * @constructor
     */
    ADACTA.Setup = function ADACTA_constructor(htmlId) {
        return ADACTA.Setup.superclass.constructor.call(this, "ADACTA.Setup", htmlId);
    };

    YAHOO.extend(ADACTA.Setup, Alfresco.component.Base, {
        /**
         * Fired by YUI when parent element is available for scripting
         * 
         * @method onReady
         */
        onReady: function ADACTA_onReady() {
            var me = this;

            this.widgets.feedbackMessage = null;

            // setup link events
            Event.on(this.id + "-initial-link", "click", this.onInitialSetup, null, this);
            Event.on(this.id + "-migration-link", "click", this.onMigration, null, this);
            Event.on(this.id + "-authorisation-link", "click", this.onAuthorisation, null, this);
            Event.on(this.id + "-loadtest-link", "click", this.onLoadTest, null, this);
            Event.on(this.id + "-configuration-link", "click", this.onConfig, null, this);
            Event.on(this.id + "-import-link", "click", this.onImport, null, this);
            Event.on(this.id + "-scanbatch-link", "click", this.onScanBatch, null, this);
            Event.on(this.id + "-delete-link", "click", this.onDelete, null, this);
        },

        onInitialSetup: function ADACTA_onLoadConfig(e, args) {
            Event.stopEvent(e);

            if (this.widgets.feedbackMessage === null) {
                this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage({
                    text: this.msg("message.creating"),
                    spanClass: "wait",
                    displayTime: 0
                });

                // call repo-tier to perform test data import
                Alfresco.util.Ajax.request({
                    method: Alfresco.util.Ajax.GET,
                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/setup/initialsetup",
                    successCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.done"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    },
                    failureCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.failure"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    }
                });
            }
        },

        onMigration: function ADACTA_onMigration(e, args) {
            Event.stopEvent(e);

            if (this.widgets.feedbackMessage === null) {
                this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage({
                    text: this.msg("message.creating"),
                    spanClass: "wait",
                    displayTime: 0
                });

                // call repo-tier to perform test data import
                Alfresco.util.Ajax.request({
                    method: Alfresco.util.Ajax.GET,
                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/setup/migration",
                    successCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.done"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    },
                    failureCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.failure"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    }
                });
            }
        },
        
        onAuthorisation: function ADACTA_onAuthorisation(e, args) {
            Event.stopEvent(e);

            if (this.widgets.feedbackMessage === null) {
                this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage({
                    text: this.msg("message.creating"),
                    spanClass: "wait",
                    displayTime: 0
                });

                // call repo-tier to perform test data import
                Alfresco.util.Ajax.request({
                    method: Alfresco.util.Ajax.GET,
                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/setup/authorisation",
                    successCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.done"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    },
                    failureCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.failure"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    }
                });
            }
        },        
        
        onLoadTest: function ADACTA_onMigrationTest(e, args) {
            Event.stopEvent(e);

            if (this.widgets.feedbackMessage === null) {
                this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage({
                    text: this.msg("message.creating"),
                    spanClass: "wait",
                    displayTime: 0
                });

                // call repo-tier to perform test data import
                Alfresco.util.Ajax.request({
                    method: Alfresco.util.Ajax.GET,
                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/setup/loadtest",
                    successCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.done"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    },
                    failureCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.failure"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    }
                });
            }
        },

        onConfig: function ADACTA_onConfigTest(e, args) {
            Event.stopEvent(e);

            if (this.widgets.feedbackMessage === null) {
                this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage({
                    text: this.msg("message.creating"),
                    spanClass: "wait",
                    displayTime: 0
                });

                // call repo-tier to perform test data import
                Alfresco.util.Ajax.request({
                    method: Alfresco.util.Ajax.GET,
                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/setup/configuration",
                    successCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.done"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    },
                    failureCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.failure"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    }
                });
            }
        },
        
        onImport: function ADACTA_onImportTest(e, args) {
            Event.stopEvent(e);

            if (this.widgets.feedbackMessage === null) {
                this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage({
                    text: this.msg("message.creating"),
                    spanClass: "wait",
                    displayTime: 0
                });

                // call repo-tier to perform test data import
                Alfresco.util.Ajax.request({
                    method: Alfresco.util.Ajax.GET,
                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/setup/import",
                    successCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.done"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    },
                    failureCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.failure"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    }
                });
            }
        },
        
        onScanBatch: function ADACTA_onScanBatchTest(e, args) {
            Event.stopEvent(e);

            if (this.widgets.feedbackMessage === null) {
                this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage({
                    text: this.msg("message.creating"),
                    spanClass: "wait",
                    displayTime: 0
                });

                // call repo-tier to perform test data import
                Alfresco.util.Ajax.request({
                    method: Alfresco.util.Ajax.GET,
                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/setup/scanbatch",
                    successCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.done"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    },
                    failureCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.failure"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    }
                });
            }
        },

        onDeleteConfirm: function ADACTA_onDeleteAllData() {
            if (this.widgets.feedbackMessage === null) {
                this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage({
                    text: this.msg("message.creating"),
                    spanClass: "wait",
                    displayTime: 0
                });

                // call repo-tier to perform config data import
                Alfresco.util.Ajax.request({
                    method: Alfresco.util.Ajax.GET,
                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/setup/delete",
                    successCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.done"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    },
                    failureCallback: {
                        fn: function() {
                            this.widgets.feedbackMessage.destroy();
                            Alfresco.util.PopupManager.displayMessage({
                                text: this.msg("message.load.failure"),
                                displayTime: 3
                            });

                            // reset feedback message - to allow another action
                            // if required
                            this.widgets.feedbackMessage = null;
                        },
                        scope: this
                    }
                });
            }
        },

        onDelete: function ADACTA_onDeleteAllData(e, args) {
            //Event.stopEvent(e);
            var me = this;
            // Show message.
            Alfresco.util.PopupManager.displayPrompt({
                title: this.msg("message.delete.title"),
                text: this.msg("message.delete.message"),
                buttons: [{
                    text: Alfresco.util.message("button.ok"),
                    handler: function ConsoleGroups__createGroup_confirmOk() {
                        // Hide Prompt
                        this.destroy();
                        me.onDeleteConfirm(me);
                    },
                    isDefault: true
                }, {
                    text: this.msg("button.cancel"),
                    handler: function ConsoleReplicationJobs_onDeleteJob_onCancel() {
                        this.destroy();
                    },
                    isDefault: true
                }]
            });
        }
    });
})();