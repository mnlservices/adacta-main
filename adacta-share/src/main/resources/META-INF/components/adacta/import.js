if (typeof(ADACTA) == "undefined") ADACTA = {};

/**
 * DatatableImport tool component.
 *
 * @namespace ADACTA
 * @class ADACTA.DatatableImport
 */
(function() {
    /**
     * YUI Library aliases
     */
    var Dom = YAHOO.util.Dom,
        Event = YAHOO.util.Event,
        Element = YAHOO.util.Element;

    /**
     * Alfresco Slingshot aliases
     */
    var $html = Alfresco.util.encodeHTML;

    /**
     * ConsoleUsers constructor.
     *
     * @param {String} htmlId The HTML id of the parent element
     * @return {ADACTA.DatatableImport} The new ConsoleUsers instance
     * @constructor
     */
    ADACTA.DatatableImport = function(htmlId) {
        this.name = "ADACTA.DatatableImport";
        ADACTA.DatatableImport.superclass.constructor.call(this, htmlId);

        /* Register this component */
        Alfresco.util.ComponentManager.register(this);

        /* Load YUI Components */
        Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "paginator", "json", "history"], this.onComponentsLoaded, this);

        /* Define panel handlers */
        var parent = this;

        /* Search Panel Handler */
        SearchPanelHandler = function SearchPanelHandler_constructor() {
            SearchPanelHandler.superclass.constructor.call(this, "search");
        };

        YAHOO.extend(SearchPanelHandler, Alfresco.ConsolePanelHandler, {

            /**
             * Keeps track if this panel is searching or not
             *
             * @property isSearching
             * @type Boolean
             */
            isSearching: false,

            /**
             * PANEL LIFECYCLE CALLBACKS
             */

            /**
             * Called by the ConsolePanelHandler when this panel shall be loaded
             *
             * @method onLoad
             */
            onLoad: function onLoad() {
                // Buttons
                parent.widgets.importButton = Alfresco.util.createYUIButton(parent, "import-button", parent.onImportFiles);

                // Menus
                parent.widgets.actionMenu = Alfresco.util.createYUIButton(parent, "selected", parent.onActionItemClick, {
                    disabled: true,
                    type: "menu",
                    menu: "selectedItems-menu"
                });
                parent.widgets.selectMenu = Alfresco.util.createYUIButton(parent, "select-button", parent.onSelectItemClick, {
                    type: "menu",
                    menu: "selectItems-menu"
                });

                // Setup the main datatable
                this._setupDataTable();

                // Clear selected items
                parent._clearSelectedItems();
            },

            onShow: function onShow() {
                // Dom.get(parent.id + "-search-text").focus();
            },

            onUpdate: function onUpdate() {
                // update the text field - as this event could come from bookmark, navigation or a search button click
                // var searchTermElem = Dom.get(parent.id + "-search-text");
                searchTermElem.value = null;

                // check search length again as we may have got here via history navigation
                if (!this.isSearching && parent.searchTerm !== undefined && parent.searchTerm.length >= parent.options.minSearchTermLength) {
                    this.isSearching = true;

                    var me = this;

                    // Reset the custom error messages
                    me._setDefaultDataTableErrors(parent.widgets.pagingDataTable.widgets.dataTable);

                    // Don't display any message
                    parent.widgets.pagingDataTable.widgets.dataTable.set("MSG_EMPTY", parent._msg("message.searching"));

                    // Empty results table
                    var startShowIndex = parent.widgets.pagingDataTable.widgets.dataTable.getRecordSet().getLength() - parent.widgets.pagingDataTable.currentMaxItems;
                    parent.widgets.pagingDataTable.widgets.dataTable.deleteRows(startShowIndex, parent.widgets.pagingDataTable.widgets.dataTable.getRecordSet().getLength());

                    //Clear sorting in paginator
                    parent.widgets.pagingDataTable.currentSortKey = null;
                    parent.widgets.pagingDataTable.currentDir = null;

                    var successHandler = function ConsoleUsers__ps_successHandler(sRequest, oResponse, oPayload) {
                            me._enableSearchUI();
                            me._setDefaultDataTableErrors(parent.widgets.pagingDataTable.widgets.dataTable);
                            parent.widgets.pagingDataTable.widgets.dataTable.onDataReturnInitializeTable.call(parent.widgets.pagingDataTable.widgets.dataTable, sRequest, oResponse, oPayload);
                        };

                    var failureHandler = function ConsoleUsers__ps_failureHandler(sRequest, oResponse) {
                            me._enableSearchUI();
                            if (oResponse.status == 401) {
                                // Our session has likely timed-out, so refresh to offer the login page
                                window.location.reload();
                            } else {
                                try {
                                    var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                                    parent.widgets.pagingDataTable.widgets.dataTable.set("MSG_ERROR", response.message);
                                    parent.widgets.pagingDataTable.widgets.dataTable.showTableMessage(Alfresco.util.encodeHTML(response.message), YAHOO.widget.DataTable.CLASS_ERROR);
                                    me._setResultsMessage("message.noresults");
                                } catch (e) {
                                    me._setDefaultDataTableErrors(parent.widgets.pagingDataTable.widgets.dataTable);
                                }
                            }
                        };

                    // Send the query to the server
                    parent.widgets.pagingDataTable.widgets.dataSource.sendRequest(me._buildSearchParams(parent.searchTerm) + "&startIndex=0&pageSize=" + parent.options.maxSearchResults + "&searchType=" + parent.options.searchType, {
                        success: successHandler,
                        failure: failureHandler,
                        scope: parent,
                        argument: {}
                    });
                    me._setResultsMessage("message.searchingFor", $html(parent.searchTerm));

                    // Disable search button and display a wait feedback message if the users hasn't been found yet
                    parent.widgets.searchButton.set("disabled", true);
                    YAHOO.lang.later(2000, me, function() {
                        if (me.isSearching) {
                            if (!me.widgets.feedbackMessage) {
                                me.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage({
                                    text: Alfresco.util.message("message.searching", parent.name),
                                    spanClass: "wait",
                                    displayTime: 0
                                });
                            } else if (!me.widgets.feedbackMessage.cfg.getProperty("visible")) {
                                me.widgets.feedbackMessage.show();
                            }
                        }
                    }, []);
                }
            },

            /**
             * Handler for the edit page
             *
             * @method _onIndexDocument
             * @param row {object} DataTable row representing item to be actioned
             */
            _onIndexDocument: function(row) {
                var data = parent.widgets.pagingDataTable.widgets.dataTable.getRecord(row).getData();
                var editDocUrl = Alfresco.util.siteURL("document-details?nodeRef=" + data.nodeRef);
                window.location = window.location.protocol + "//" + window.location.hostname + editDocUrl;
            },

            /**
             * Handler for the edit form
             *
             * @method _onEditNode
             * @param row {object} DataTable row representing item to be actioned
             */
            _onEditNode: function(row) {
                var data = parent.widgets.pagingDataTable.widgets.dataTable.getRecord(row).getData();

                Alfresco.util.PopupManager.displayForm({
                    title: parent._msg("actions.document.edit-metadata"),
                    properties: {
                        mode: "edit",
                        itemKind: "node",
                        itemId: data.nodeRef
                    },
                    success: {
                        fn: function(response) {
                            Alfresco.util.PopupManager.displayMessage({
                                text: parent._msg("message.details.success")
                            });

                            parent.refreshDataTable();
                        },
                        scope: parent
                    },
                    failureMessage: parent._msg("message.details.failure")
                });
            },

            /**
             * Handler for the delete form
             *
             * @method _onDeleteNode
             * @param row {object} DataTable row representing item to be actioned
             */
            _onDeleteNode: function(row) {
                var data = parent.widgets.pagingDataTable.widgets.dataTable.getRecord(row).getData();

                Alfresco.util.PopupManager.displayForm({
                    title: data.name,
                    properties: {
                        mode: "create",
                        itemKind: "action",
                        itemId: "adactaDeleteNode",
                        destination: data.nodeRef
                    },
                    success: {
                        fn: function(response) {
                            Alfresco.util.PopupManager.displayMessage({
                                text: parent._msg("message.delete.success", data.name),
                                displayTime: 4
                            });

                            parent.refreshDataTable();
                        },
                        scope: parent
                    },
                    failureMessage: parent._msg("message.delete.failure", data.name)
                });
            },

            /**
             * Enable search button, hide the pending wait message and set the panel as not searching.
             *
             * @method _enableSearchUI
             * @private
             */
            _enableSearchUI: function _enableSearchUI() {
                // Enable search button and close the wait feedback message if present
                if (this.widgets.feedbackMessage && this.widgets.feedbackMessage.cfg.getProperty("visible")) {
                    this.widgets.feedbackMessage.hide();
                }
                parent.widgets.searchButton.set("disabled", false);
                this.isSearching = false;
            },

            /**
             * Setup the YUI DataTable with custom renderers.
             *
             * @method _setupDataTable
             * @private
             */
            _setupDataTable: function _setupDataTable() {

                /**
                 * Hook action events
                 */
                var registerEventHandler = function(cssClass, fnHandler) {
                        var fnEventHandler = function(layer, args) {
                                var owner = YAHOO.Bubbling.getOwnerByTagName(args[1].anchor, "div");
                                if (owner !== null) {
                                    fnHandler.call(me, args[1].target.offsetParent, owner);
                                }
                                return true;
                            };
                        YAHOO.Bubbling.addDefaultAction(cssClass, fnEventHandler);
                    };


                registerEventHandler("onIndexDocument", this._onIndexDocument);
                //registerEventHandler("onEditNode", this._onEditNode);
                registerEventHandler("onDeleteNode", this._onDeleteNode);

                /**
                 * Generic HTML-safe custom datacell formatter
                 */
                var renderCellSafeHTML = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(oData);
                    };

                var renderCellCategory = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(parent._msg(oRecord.getData("docCategory")));
                    };

                var renderCellSubject = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(parent._msg(oRecord.getData("docSubject")));
                    };

                var renderCellName = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        var uri = Alfresco.constants.PAGEID.replace("adacta-import", "") + "document-details?nodeRef=" + oRecord.getData("nodeRef");
                        elCell.innerHTML = '<a href="' + uri + '">' + oRecord.getData("name") + '</a>';
                    };

                var renderCellCreated = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        var meta = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oRecord.getData("created"))));
                        elCell.innerHTML = meta;
                    };

                var renderCellCreatorFullName = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        var meta = $html((oRecord.getData("creatorFullName")));
                        elCell.innerHTML = meta;
                    };

                var renderCellDocDate = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        if (oRecord.getData("docDate") != "") {
                            var meta = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oRecord.getData("docDate")), "yyyy-mm-dd"));
                            elCell.innerHTML = meta;
                        }
                    };

                var renderCellSelect = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");

                        var me = this;
                        elCell.innerHTML = '<input id="checkbox-' + oRecord.getId() + '" type="checkbox" value="' + oRecord.getData("nodeRef") + '">';
                        elCell.firstChild.onclick = function() {
                            parent._updateSelectedItemsMenu();
                        };
                    };

                var renderCellActions = function renderCellThumbnail(elCell, oRecord, oColumn, oData) {
                        Dom.setStyle(elCell.parentNode, "vertical-align", "middle");
                        Dom.setStyle(elCell.parentNode, "text-align", "right");

                        var desc = "";
                        // desc += '<a class="onEditNode" title="' + parent._msg("actions.document.edit-metadata") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-edit-metadata-16.png" title="' + parent._msg("actions.document.edit-metadata") + '"/></a>&nbsp;';
                        desc += '<a class="onIndexDocument" title="' + parent._msg("label.index-document") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-edit-metadata-16.png" title="' + parent._msg("label.index-document") + '"/></a>&nbsp;';
                        desc += '<a class="onDeleteNode" title="' + parent._msg("actions.document.delete") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-delete-16.png" title="' + parent._msg("actions.document.delete") + '"/></a>&nbsp;';
                        elCell.innerHTML = desc;
                    };

                // DataTable column defintions
                var columnDefinitions = [{
                    key: "select",
                    label: parent._msg("label.checkbox"),
                    sortable: false,
                    formatter: renderCellSelect,
                    width: 10
                }, {
                    key: "actions",
                    label: parent._msg("label.actions"),
                    sortable: false,
                    formatter: renderCellActions,
                    width: 40
                }, {
                    key: "name",
                    label: parent._msg("label.name"),
                    sortable: true,
                    formatter: renderCellName
                }, {
                    key: "docCategory",
                    label: parent._msg("label.docCategory"),
                    sortable: true,
                    formatter: renderCellCategory
                }, {
                    key: "docSubject",
                    label: parent._msg("label.docSubject"),
                    sortable: true,
                    formatter: renderCellSubject
                }, {
                    key: "created",
                    label: parent._msg("label.created"),
                    sortable: true,
                    formatter: renderCellCreated
                }, {
                    key: "creator",
                    label: parent._msg("label.creator"),
                    sortable: true,
                    formatter: renderCellCreatorFullName
                }, {
                    key: "docDate",
                    label: parent._msg("label.docDate"),
                    sortable: true,
                    formatter: renderCellDocDate
                }];

                var me = this;
                var meParent = parent;

                parent.widgets.pagingDataTable = new Alfresco.util.DataTable({
                    dataTable: {
                        config: {
                            generateRequest: function(oState, oSelf) {
                                var startIndex = (oState.pagination.page - 1) * oState.pagination.rowsPerPage;
                                var sort = encodeURIComponent((oState.sortedBy) ? oState.sortedBy.key : oSelf.getColumnSet().keys[0].getKey());
                                var dir = (oState.sortedBy && oState.sortedBy.dir === YAHOO.widget.DataTable.CLASS_DESC) ? "desc" : "asc";

                                //Save current sort for paging
                                meParent.widgets.pagingDataTable.currentSortKey = sort;
                                meParent.widgets.pagingDataTable.currentDir = dir;

                                // Build the request
                                var query = "?sortBy=" + sort + "&dir=" + dir + "&searchType=" + parent.options.searchType;

                                if (parent.searchTerm || parent.searchTerm == "") {
                                    query = query + "&filter=" + encodeURIComponent(parent.searchTerm) + "&startIndex=" + startIndex + "&pageSize=" + oState.pagination.rowsPerPage;
                                }

                                return query;
                            }
                        },
                        container: parent.id + "-datatable",
                        columnDefinitions: columnDefinitions
                    },
                    dataSource: {
                        url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/search/importfolderlist",
                        pagingResolver: function(currentSkipCount, currentMaxItems, currentSortKey, currentDir) {
                        	if (!currentSortKey){
                        		currentSortKey="created";
                        	}
                        	if (!currentDir){
                        		currentDir="asc";
                        	}
                            return "startIndex=" + currentSkipCount + "&" + "pageSize=" + currentMaxItems + "&" + me._buildSearchParams(parent.searchTerm).substring(1) + "&sortBy=" + currentSortKey + "&dir=" + currentDir + "&searchType=" + parent.options.searchType;
                        },
                        defaultFilter: {
                            filterId: "all"
                        },
                        config: {
                            responseType: YAHOO.util.DataSource.TYPE_JSON,
                            responseSchema: {
                                resultsList: "data.adactaNodes"
                            }
                        }
                    },
                    paginator: {
                        history: false,
                        hide: false,
                        config: {
                            containers: [parent.id + "-paginator"],
                            rowsPerPage: parent.options.maxSearchResults
                        }
                    }
                });

                //Show total number of search result
                parent.widgets.pagingDataTable.widgets.dataSource.doBeforeParseData = function doBeforeParseData(oRequest, oFullResponse) {
                    me._setResultsMessage("message.results", oFullResponse.paging.totalItems);

                    return oFullResponse;
                };
            },

            /**
             * Resets the YUI DataTable errors to our custom messages
             * NOTE: Scope could be YAHOO.widget.DataTable, so can't use "this"
             *
             * @method _setDefaultDataTableErrors
             * @param dataTable {object} Instance of the DataTable
             * @private
             */
            _setDefaultDataTableErrors: function _setDefaultDataTableErrors(dataTable) {
                var msg = Alfresco.util.message;
                dataTable.set("MSG_EMPTY", parent._msg("message.empty", "ADACTA.DatatableImport"));
                dataTable.set("MSG_ERROR", parent._msg("message.error", "ADACTA.DatatableImport"));
            },

            /**
             * Build URI parameters for List JSON data webscript
             *
             * @method _buildSearchParams
             * @param searchTerm {string} User search term
             * @private
             */
            _buildSearchParams: function _buildSearchParams(searchTerm) {
                return "?filter=" + encodeURIComponent(searchTerm);
            },

            /**
             * Set the message in the Results Bar area
             *
             * @method _setResultsMessage
             * @param messageId {string} The messageId to display
             * @private
             */
            _setResultsMessage: function _setResultsMessage(messageId, arg1, arg2) {
                var resultsDiv = Dom.get(parent.id + "-search-bar");
                resultsDiv.innerHTML = parent._msg(messageId, arg1, arg2);
            }
        });
        new SearchPanelHandler();

        return this;
    };

    YAHOO.extend(ADACTA.DatatableImport, Alfresco.ConsoleTool, {
        /**
         * Object container for initialization options
         *
         * @property options
         * @type object
         */
        options: {
            /**
             * Number of characters required for a search.
             *
             * @property minSearchTermLength
             * @type int
             * @default 1
             */
            minSearchTermLength: 1,

            /**
             * Maximum number of items to display in the results list
             *
             * @property maxSearchResults
             * @type int
             * @default 100
             */
            maxSearchResults: 100,

            /**
             * Set search type for backend search query.
             */
            searchType: "",

            /**
             * Root folder node reference
             *
             * @property minSearchTermLength
             * @type nodeRef
             * 
             */
            rootFolder: ""
        },

        /**
         * Current search term, obtained from form input field.
         *
         * @property searchTerm
         * @type string
         */
        searchTerm: "",


        /**
         * Fired by YUI when parent element is available for scripting.
         * Component initialisation, including instantiation of YUI widgets and event listener binding.
         *
         * @method onReady
         */
        onReady: function Datatable_onReady() {
            // Call super-class onReady() method
            ADACTA.DatatableImport.superclass.onReady.call(this);
        },

        /**
         * History manager state change event handler (override base class)
         *
         * @method onStateChanged
         * @param e {object} DomEvent
         * @param args {array} Event parameters (depends on event type)
         */
        onStateChanged: function Datatable_onStateChanged(e, args) {
            var state = this.decodeHistoryState(args[1].state);

            // test if panel has actually changed?
            if (state.panel) {
                this.showPanel(state.panel);
            }

            if (state.search !== undefined && this.currentPanelId === "search") {
                // keep track of the last search performed
                var searchTerm = state.search;
                this.searchTerm = searchTerm;

                this.updateCurrentPanel();
            } else {

                this.updateCurrentPanel();
            }
        },

        /**
         * File Upload complete event handler
         * 
         * @method onFileUploadComplete
         * @param complete
         *            {object} Object literal containing details of successful and failed uploads
         */
        onFileUploadComplete: function UP_onFileUploadComplete(complete) {
            this.widgets.alfrescoDataTable.loadDataTable();
        },

        /**
         * Encode state object into a packed string for use as url history value.
         * Override base class.
         *
         * @method encodeHistoryState
         * @param obj {object} state object
         * @private
         */
        encodeHistoryState: function CDatatable_encodeHistoryState(obj) {
            // wrap up current state values
            var stateObj = {};
            if (this.currentPanelId !== "") {
                stateObj.panel = this.currentPanelId;
            }
            if (this.currentUserId !== "") {
                stateObj.userid = this.currentUserId;
            }
            if (this.searchTerm !== undefined) {
                stateObj.search = this.searchTerm;
            }

            // convert to encoded url history state - overwriting with any supplied values
            var state = "";
            if (obj.panel || stateObj.panel) {
                state += "panel=" + encodeURIComponent(obj.panel ? obj.panel : stateObj.panel);
            }
            if (obj.userid || stateObj.userid) {
                if (state.length !== 0) {
                    state += "&";
                }
                state += "userid=" + encodeURIComponent(obj.userid ? obj.userid : stateObj.userid);
            }
            if (obj.search !== undefined || stateObj.search !== undefined) {
                if (state.length !== 0) {
                    state += "&";
                }
                state += "search=" + encodeURIComponent(obj.search !== undefined ? obj.search : stateObj.search);
            }
            return state;
        },

        /**
         * Gets a custom message
         *
         * @method _msg
         * @param messageId {string} The messageId to retrieve
         * @return {string} The custom message
         * @private
         */
        _msg: function Datatable_msg(messageId) {
            return Alfresco.util.message.call(this, messageId, "ADACTA.DatatableImport", Array.prototype.slice.call(arguments).slice(1));
        },

        /**
         * Selected items action menu event handler
         * @method onActionItemClick.
         * @param sType, aArgs, p_obj
         */
        onActionItemClick: function Datatable_onActionItemClick(sType, aArgs, p_obj) {
            var items = [],
                dt = this.widgets.pagingDataTable.getDataTable(),
                rows = dt.getTbodyEl().rows;
            for (var i = 0; i < rows.length; i++) {
                if (rows[i].cells[0].getElementsByTagName('input')[0].checked) {
                    var data = dt.getRecord(i);
                    if (data) {
                        items.push(data);
                    }
                }
            }

            var me = this;
            switch (aArgs[1]._oAnchor.className.split(" ")[0]) {

            case "index-item":
            	
                var resultNodeRefs = [];
                var firstNodeRef = null;

                for (var i = 0; i < items.length; i++) {

                    if (i == 0) {
                        firstNodeRef = items[i].getData("nodeRef");
                    }
                    resultNodeRefs[i] = items[i].getData("nodeRef");
                }

                var onSuccess = function _onSuccess(response) {
                        var docDetails = Alfresco.util.siteURL("document-details?nodeRef=" + firstNodeRef);
                        window.location = window.location.protocol + "//" + window.location.hostname + docDetails;
                    };

                var onFailure = function _onFailure(response) {};

                var url = Alfresco.constants.PROXY_URI + "nl/defensie/adacta/preferences/selected-items";

                Alfresco.util.Ajax.jsonRequest({
                    url: url,
                    method: "POST",
                    dataObj: {
                        items: resultNodeRefs
                    },
                    successCallback: {
                        fn: onSuccess,
                        scope: this
                    },
                    failureCallback: {
                        fn: onFailure,
                        scope: this
                    }
                });

                break;

            case "delete-item":
                // confirm this brutal operation with the user
                Alfresco.util.PopupManager.displayPrompt({
                    title: me.msg("button.delete"),
                    text: me.msg("message.delete.confirm"),
                    buttons: [{
                        text: me.msg("button.ok"),
                        handler: function() {
                            this.destroy();
                            var failed = [],
                                total = 0;
                            for (var i = 0; i < items.length; i++) {
                                // make ajax calls to Delete the items
                                Alfresco.util.Ajax.request({
                                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/node/" + items[i].getData("nodeRef").replace(":/", ""),
                                    method: "DELETE",
                                    failureCallback: {
                                        fn: function() {
                                            failed.push(items[i].getData("name"));
                                            total++;
                                        },
                                        obj: items[i],
                                        scope: me
                                    },
                                    successCallback: {
                                        fn: function() {
                                            total++;
                                        },
                                        obj: items[i],
                                        scope: me
                                    }
                                });
                            }
                            var completeFn = function() {
                                    if (total === items.length) {
                                        Alfresco.util.PopupManager.displayPrompt({
                                            title: me.msg("message.delete.report"),
                                            text: me.msg("message.delete.report-info", (items.length - failed.length), failed.length)
                                        });
                                        me.refreshDataTable();
                                        me._deselectAll();
                                    } else {
                                        setTimeout(completeFn, 500);
                                    }
                                };
                            setTimeout(completeFn, 500);


                        }
                    }, {
                        text: me.msg("button.cancel"),
                        handler: function() {
                            this.destroy();
                        },
                        isDefault: true
                    }]
                });
                break;
            }
        },

        /**
         * Select items menu item event handler
         * @method onSelectItemClick.
         * @param sType, aArgs, p_obj
         */
        onSelectItemClick: function Datatable_onSelectItemClick(sType, aArgs, p_obj) {
            switch (aArgs[1]._oAnchor.className.split(" ")[0]) {
            case "select-all":
                this._selectAll();
                break;
            case "select-invert":
                this._invertSelection();
                break;
            case "select-none":
                this._deselectAll();
                break;
            }
        },

        /**
         * Select all items.
         * @method _selectAll
         */
        _selectAll: function Datatable_selectAll() {
            var rows = this.widgets.pagingDataTable.getDataTable().getTbodyEl().rows;
            for (var i = 0; i < rows.length; i++) {
                rows[i].cells[0].getElementsByTagName('input')[0].checked = true;
            }
            this._updateSelectedItemsMenu();
        },

        /**
         * Deselect all items.
         * @method _deselectAll
         */
        _deselectAll: function Datatable_deselectAll() {
            var rows = this.widgets.pagingDataTable.getDataTable().getTbodyEl().rows;
            for (var i = 0; i < rows.length; i++) {
                rows[i].cells[0].getElementsByTagName('input')[0].checked = false;
            }
            this._updateSelectedItemsMenu();
            this._clearSelectedItems();
        },

        /**
         * Invert selection of items.
         * @method _invertSelection
         */
        _invertSelection: function Datatable_invertSelection() {
            var rows = this.widgets.pagingDataTable.getDataTable().getTbodyEl().rows;
            for (var i = 0; i < rows.length; i++) {
                var check = rows[i].cells[0].getElementsByTagName('input')[0];
                check.checked = !check.checked;
            }
            this._updateSelectedItemsMenu();
        },

        /**
         * Update the disabled status of the multi-select action menu based on the state of the item checkboxes
         * @method _updateSelectedItemsMenu
         */
        _updateSelectedItemsMenu: function Datatable_updateSelectedItemsMenu() {
            this.widgets.actionMenu.set("disabled", true);
            var rows = this.widgets.pagingDataTable.getDataTable().getTbodyEl().rows;
            for (var i = 0; i < rows.length; i++) {
                if (rows[i].cells[0].getElementsByTagName('input')[0].checked) {
                    this.widgets.actionMenu.set("disabled", false);
                    break;
                }
            }
        },

        /**
         * Clear selected items.
         * @method _clearSelectedItems
         */
        _clearSelectedItems: function Datatable_clearSelectedItems() {
            Alfresco.util.Ajax.request({
                url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/preferences/selected-items/clear",
                method: "GET",
                failureCallback: {},
                successCallback: {}
            });
        },

        /**
         * Refresh the list after an action has occured
         * 
         * @method refreshDataTable
         */
        refreshDataTable: function Datatable_refreshDataTable() {
            this.widgets.pagingDataTable.loadDataTable(null);
        },

        /**
         * Upload dialog
         */
        onImportFiles: function Datatable_onImportFiles(e, p_obj) {
            this.fileUpload = Alfresco.getFileUploadInstance();

            var uploadConfig = {
                destination: this.options.rootFolder,
                flashUploadURL: "api/upload",
                htmlUploadURL: "api/upload",
                mode: this.fileUpload.MODE_MULTI_UPLOAD,
                onFileUploadComplete: {
                    fn: this.onFileUploadComplete,
                    scope: this
                }
            };

            this.fileUpload.show(uploadConfig);
            Event.preventDefault(e);
        },

        onFileUploadComplete: function Datatable_onFileUploadComplete(complete) {
            this.widgets.pagingDataTable.loadDataTable(null);
        }
    });
})();