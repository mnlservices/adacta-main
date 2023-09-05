(function() {
    YAHOO.lang.augmentObject(Alfresco.FormUI.prototype, {
        /**
         * Overwrites the onReady. Added this.formsRuntime.setShowSubmitStateDynamically(true, false); to make sure Ok button is disabled if not all mandatory props are filled in.
         * 
         * @method onReady
         */
        onReady: function FormUI_onReady() {
        	if (this.options.mode !== "view") {
                // make buttons YUI buttons
                if (Dom.get(this.id + "-submit") !== null) {
                    this.buttons.submit = Alfresco.util.createYUIButton(this, "submit", null, {
                        type: "submit",
                        additionalClass: "alf-primary-button"
                    });

                    // force the generated button to have a name of "-" so it gets ignored in
                    // JSON submit. TODO: remove this when JSON submit behaviour is configurable
                    Dom.get(this.id + "-submit-button").name = "-";
                }

                if (Dom.get(this.id + "-reset") !== null) {
                    this.buttons.reset = Alfresco.util.createYUIButton(this, "reset", null, {
                        type: "reset"
                    });

                    // force the generated button to have a name of "-" so it gets ignored in
                    // JSON submit. TODO: remove this when JSON submit behaviour is configurable
                    Dom.get(this.id + "-reset-button").name = "-";
                }

                if (Dom.get(this.id + "-cancel") !== null) {
                    this.buttons.cancel = Alfresco.util.createYUIButton(this, "cancel", null);

                    // force the generated button to have a name of "-" so it gets ignored in
                    // JSON submit. TODO: remove this when JSON submit behaviour is configurable
                    Dom.get(this.id + "-cancel-button").name = "-";
                }

                // fire event to inform any listening components that the form HTML is ready
                YAHOO.Bubbling.fire("formContentReady", this);

                this.formsRuntime = new Alfresco.forms.Form(this.id);
                this.formsRuntime.setSubmitElements(this.buttons.submit);
                this.formsRuntime.setShowSubmitStateDynamically(true, false);

                // setup JSON/AJAX mode if appropriate
                if (this.options.enctype === "application/json") {
                    this.formsRuntime.setAJAXSubmit(true, {
                        successCallback: {
                            fn: this.onJsonPostSuccess,
                            scope: this
                        },
                        failureCallback: {
                            fn: this.onJsonPostFailure,
                            scope: this
                        }
                    });
                    this.formsRuntime.setSubmitAsJSON(true);
                }

                // add field help
                for (var f = 0; f < this.options.fields.length; f++) {
                    var ff = this.options.fields[f],
                        iconEl = Dom.get(this.parentId + "_" + ff.id + "-help-icon");
                    if (iconEl) {
                        Alfresco.util.useAsButton(iconEl, this.toggleHelpText, ff.id, this);
                    }
                }

                // add any field constraints present
                for (var c = 0; c < this.options.fieldConstraints.length; c++) {
                    var fc = this.options.fieldConstraints[c];

                    // Check the number of events for the handler...
                    var events = fc.event.split(",");
                    for (var e = 0; e < events.length; e++) {
                        var trimmedEvent = events[e].replace(" ", "");
                        this.formsRuntime.addValidation(fc.fieldId, fc.handler, fc.params, trimmedEvent, fc.message);
                    }
                }

                // fire event to inform any listening components that the form is about to be initialised
                YAHOO.Bubbling.fire("beforeFormRuntimeInit", {
                    eventGroup: this.eventGroup,
                    component: this,
                    runtime: this.formsRuntime
                });

                this.formsRuntime.init();

                // fire event to inform any listening components that the form has finished initialising
                YAHOO.Bubbling.fire("afterFormRuntimeInit", {
                    eventGroup: this.eventGroup,
                    component: this,
                    runtime: this.formsRuntime
                });
            }
        }
    }, true);
})();