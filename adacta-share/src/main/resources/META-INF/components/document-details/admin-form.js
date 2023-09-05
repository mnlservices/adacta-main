(function() {
    /**
     * YUI Library aliases
     */
    var Dom = YAHOO.util.Dom,
        Event = YAHOO.util.Event,
        formatDate = Alfresco.util.formatDate,
        fromISO8601 = Alfresco.util.fromISO8601;

    /**
     * DocumentName constructor.
     *
     * @param {String} htmlId The HTML id of the parent element
     * @return {Alfresco.AdminForm} The new DocumentName instance
     * @constructor
     */
    Alfresco.AdminForm = function AdminForm_constructor(htmlId) {
        Alfresco.AdminForm.superclass.constructor.call(this, "Alfresco.AdminForm", htmlId);

        // Decoupled event listeners
        YAHOO.Bubbling.on("metadataRefresh", this.doRefresh, this);

        return this;
    };

    YAHOO.extend(Alfresco.AdminForm, Alfresco.component.Base, {

        /**
         * Object container for initialization options
         *
         * @property options
         * @type object
         */
        options: {
            /**
             * The nodeRefs to load the form for.
             *
             * @property nodeRef
             * @type string
             * @required
             */
            nodeRef: null,

            /**
             * The current site (if any)
             *
             * @property site
             * @type string
             */
            site: null,

            /**
             * The form id for the form to use.
             *
             * @property destination
             * @type string
             */
            formId: null
        },

        /**
         * Fired by YUI when parent element is available for scripting.
         * Template initialisation, including instantiation of YUI widgets and event listener binding.
         *
         * @method onReady
         */
        onReady: function AdminForm_onReady() {
            // Load the form
            Alfresco.util.Ajax.request({
                url: Alfresco.constants.URL_SERVICECONTEXT + "components/form",
                dataObj: {
                    htmlid: this.id + "-formContainer",
                    itemKind: "node",
                    itemId: this.options.nodeRef,
                    formId: this.options.formId,
                    mode: "view"
                },
                successCallback: {
                    fn: this.onFormLoaded,
                    scope: this
                },
                failureMessage: this.msg("message.failure"),
                scope: this,
                execScripts: true
            });


        },

        /**
         * Called when a workflow form has been loaded.
         * Will insert the form in the Dom.
         *
         * @method onFormLoaded
         * @param response {Object}
         */
        onFormLoaded: function DocumentMetadata_onFormLoaded(response) {
            var formEl = Dom.get(this.id + "-formContainer"),
                me = this;
            formEl.innerHTML = response.serverResponse.responseText;
            Dom.getElementsByClassName("viewmode-value-date", "span", formEl, function() {
                var showTime = Dom.getAttribute(this, "data-show-time"),
                    fieldValue = Dom.getAttribute(this, "data-date-iso8601"),
                    dateFormat = (showTime == 'false') ? me.msg("date-format.defaultDateOnly") : me.msg("date-format.default"),
                    // MNT-9693 - Pass the ignoreTime flag
                    ignoreTime = showTime == 'false',
                    theDate = fromISO8601(fieldValue, ignoreTime);

                this.innerHTML = formatDate(theDate, dateFormat);
            });
        },

        /**
         * Refresh component in response to metadataRefresh event
         *
         * @method doRefresh
         */
        doRefresh: function AdminForm_doRefresh() {
            //alert("Test");
            YAHOO.Bubbling.unsubscribe("metadataRefresh", this.doRefresh, this);
            this.refresh('components/document-details/admin-form?nodeRef={nodeRef}' + (this.options.siteId ? '&site={siteId}' : '') + (this.options.formId ? '&formId={formId}' : ''));
        }
    });
})();