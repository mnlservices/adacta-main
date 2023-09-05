if (typeof(ADACTA) == "undefined") ADACTA = {};

/**
 * DatatableIndex tool component.
 *
 * @namespace ADACTA
 * @class ADACTA.DatatableIndex
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
     * @return {ADACTA.DatatableIndex} The new ConsoleUsers instance
     * @constructor
     */
    ADACTA.DatatableIndex = function(htmlId) {
        this.name = "ADACTA.DatatableIndex";
        ADACTA.DatatableIndex.superclass.constructor.call(this, htmlId);

        /* Register this component */
        Alfresco.util.ComponentManager.register(this);

        /* Load YUI Components */
        Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "paginator", "json", "history"], this.onComponentsLoaded, this);

        /* Decoupled event listeners */
        YAHOO.Bubbling.on("viewFolderClick", this.onViewFolderClick, this);

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
                parent.widgets.searchButton = Alfresco.util.createYUIButton(parent, "search-button", parent.onSearchClick);

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

                // register the "enter" event on the search text field
                var searchText = Dom.get(parent.id + "-search-text");

                new YAHOO.util.KeyListener(searchText, {
                    keys: YAHOO.util.KeyListener.KEY.ENTER
                }, {
                    fn: function() {
                        parent.onSearchClick();
                    },
                    scope: parent,
                    correctScope: true
                }, "keydown").enable();
            },

            onShow: function onShow() {
                Dom.get(parent.id + "-search-text").focus();
            },

            onUpdate: function onUpdate() {
                // update the text field - as this event could come from bookmark, navigation or a search button click
                var searchTermElem = Dom.get(parent.id + "-search-text");
                searchTermElem.value = parent.searchTerm;

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
             * Handler for the edit form
             *
             * @method _onEditNode
             * @param row {object} DataTable row representing item to be actioned
             */
            _onEditNode: function(row) {
            	
            	var record = parent.widgets.pagingDataTable.widgets.dataTable.getRecord(row);

                var childrenNodes = record.getData().childrenNodes;

                var resultNodeRefs = [];
                var firstNodeRef = null;

                for (var i = 0; i < childrenNodes.length; i++) {

                    if (i == 0) {
                        firstNodeRef = childrenNodes[i].nodeRef;
                    }
                    resultNodeRefs[i] = childrenNodes[i].nodeRef;
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
                
                Alfresco.util.Ajax.request({
                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/allocatebatch/" + record.getData("nodeRef").replace(":/", ""),
                    method: "GET",
                    successCallback: {
                        fn: function() {
                        	parent.refreshDataTable();
                        },
                        scope: parent
                    },
                    failureMessage: "Error allocating batch."
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

                registerEventHandler("onEditNode", this._onEditNode);
                registerEventHandler("onDeleteNode", this._onDeleteNode);

                /**
                 * Generic HTML-safe custom datacell formatter
                 */
                var renderCellSafeHTML = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(oData);
                    };

                var renderCellName = function renderCellFullName(elCell, oRecord, oColumn, oData) {

                        var viewFolderLink = document.createElement("a");
                        viewFolderLink.innerHTML = $html(oRecord.getData("name"));

                        // fire the 'viewFolderClick' event 
                        YAHOO.util.Event.addListener(viewFolderLink, "click", function(e) {
                            YAHOO.Bubbling.fire('viewFolderClick', {
                                nodeRef: oRecord.getData("nodeRef")
                            });
                        }, null, parent);
                        elCell.appendChild(viewFolderLink);

                        // assign batch on click
                        YAHOO.util.Event.addListener(viewFolderLink, "click", function(e) {
                            Alfresco.util.Ajax.request({
                                url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/allocatebatch/" + oRecord.getData("nodeRef").replace(":/", ""),
                                method: "GET",
                                successCallback: {
                                    fn: function() {
                                        parent.refreshDataTable();
                                    },
                                    scope: parent
                                },
                                failureMessage: "Error allocating batch."
                            });
                        }, null, parent);
                        elCell.appendChild(viewFolderLink);
                    };

                var renderCellSelect = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");

                        var me = this;
                        elCell.innerHTML = '<input id="checkbox-' + oRecord.getId() + '" type="checkbox" value="' + oRecord.getData("nodeRef") + '">';
                        elCell.firstChild.onclick = function() {
                            parent._updateSelectedItemsMenu();
                        };

                    };

                var renderCellCreated = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        var meta = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oRecord.getData("created"))));
                        elCell.innerHTML = meta;
                    };

                var renderCellScanEmployee = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        var meta = $html(oRecord.getData("scanEmployeeFullName"));
                        elCell.innerHTML = meta;
                    };

                var renderCellOwner = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        var meta = $html(oRecord.getData("ownerFullName"));
                        elCell.innerHTML = meta;
                    };

                var renderCellChildren = function renderCell(elCell, oRecord, oColumn, oData) {
                        Alfresco.util.Ajax.request({
                            url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/children/" + oRecord.getData("nodeRef").replace(":/", ""),
                            method: "GET",
                            successCallback: {
                                fn: function(res) {
                                    elCell.innerHTML = res.json["total"];
                                    oRecord.setData("childrenNodes", res.json["nodeRefs"]);
                                },
                                scope: parent
                            },
                            failureMessage: "Error fetching children."
                        });
                    };

                var renderCellActions = function renderCellThumbnail(elCell, oRecord, oColumn, oData) {
                        Dom.setStyle(elCell.parentNode, "vertical-align", "middle");
                        Dom.setStyle(elCell.parentNode, "text-align", "center");

                        var assigned = (oRecord.getData("owner") && oRecord.getData("owner").length > 0);
                        
                        var desc = "";
                        if (oRecord.getData("isContentType")) {
                            desc += '<a class="onDeleteNode" title="' + parent._msg("actions.document.delete") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-delete-16.png" title="' + parent._msg("actions.document.delete") + '"/></a>&nbsp;';
                        } else {
                        	if (assigned) {
                        		desc += '<a class="onEditNode" title="' + parent._msg("label.action.index-batch") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-edit-metadata-visited-16.png" title="' + parent._msg("label.action.index-batch") + '"/></a>&nbsp;';
                        	} else {
                        		desc += '<a class="onEditNode" title="' + parent._msg("label.action.index-batch") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-edit-metadata-16.png" title="' + parent._msg("label.action.index-batch") + '"/></a>&nbsp;';
                        	}
                            desc += '<a class="onDeleteNode" title="' + parent._msg("actions.batch.delete") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/folder-delete-16.png" title="' + parent._msg("actions.batch.delete") + '"/></a>&nbsp;';
                        }
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
                    key: "scanEmployee",
                    label: parent._msg("label.scanBy"),
                    sortable: true,
                    formatter: renderCellScanEmployee
                }, {
                    key: "created",
                    label: parent._msg("label.created"),
                    sortable: true,
                    formatter: renderCellCreated
                }, {
                    key: "owner",
                    label: parent._msg("label.assignedTo"),
                    sortable: true,
                    formatter: renderCellOwner
                }, {
                    key: "children",
                    label: parent._msg("label.numberOfDocuments"),
                    sortable: false,
                    formatter: renderCellChildren
                }];

                var me = this;
                var meParent = parent;

                parent.widgets.pagingDataTable = new Alfresco.util.DataTable({
                    dataTable: {
                        config: {
                            generateRequest: function(oState, oSelf) {
                                var startIndex = (oState.pagination.page - 1) * oState.pagination.rowsPerPage;
                                var sort = encodeURIComponent((oState.sortedBy) ? oState.sortedBy.key : oSelf.getColumnSet().keys[5].getKey());
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
                        url: Alfresco.constants.PROXY_URI + "/nl/defensie/adacta/search/filefolder",
                        pagingResolver: function(currentSkipCount, currentMaxItems, currentSortKey, currentDir) {
                        	if (!currentSortKey){
                        		currentSortKey="created";
                        	}
                        	if (!currentDir){
                        		currentDir="desc";
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
                dataTable.set("MSG_EMPTY", parent._msg("message.empty", "ADACTA.DatatableIndex"));
                dataTable.set("MSG_ERROR", parent._msg("message.error", "ADACTA.DatatableIndex"));
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


        /* View Panel Handler */
        ViewPanelHandler = function ViewPanelHandler_constructor() {
            ViewPanelHandler.superclass.constructor.call(this, "viewFolder");
        };

        YAHOO.extend(ViewPanelHandler, Alfresco.ConsolePanelHandler, {
            onLoad: function onLoad() {
                // Buttons
                parent.widgets.gobackButton = Alfresco.util.createYUIButton(parent, "goback-button", parent.onGoBackClick);

                // Menus
                parent.widgets.actionMenuChild = Alfresco.util.createYUIButton(parent, "selected-child", parent.onActionItemChildClick, {
                    disabled: true,
                    type: "menu",
                    menu: "selectedItems-menu-child"
                });
                parent.widgets.selectMenuChild = Alfresco.util.createYUIButton(parent, "select-button-child", parent.onSelectItemChildClick, {
                    type: "menu",
                    menu: "selectItems-menu-child"
                });
            },

            onBeforeShow: function onBeforeShow() {
                // Hide the main panel area before it is displayed - so we don't show
                // old data to the user before the Update() method paints the results
                Dom.setStyle(parent.id + "-view-main", "visibility", "hidden");
            },

            onShow: function onShow() {
                window.scrollTo(0, 0);
            },

            onUpdate: function onUpdate() {

                var success = function(res) {

                        var fnSetter = function(id, val) {
                                Dom.get(parent.id + id).innerHTML = val ? $html(val) : "";
                            };

                        var folder = YAHOO.lang.JSON.parse(res.serverResponse.responseText);

                        fnSetter("-name", folder.item.name);
                        fnSetter("-created", Alfresco.util.formatDate(Alfresco.util.fromISO8601(folder.item.created)));
                        fnSetter("-scan-by", folder.item.scanEmployeeFullName);

                        // Make main panel area visible
                        Dom.setStyle(parent.id + "-view-main", "visibility", "visible");
                    };


                // make an ajax call to get user details
                Alfresco.util.Ajax.request({
                    url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/nodedetails/" + parent.currentNodeRef.replace(":/", ""),
                    method: Alfresco.util.Ajax.GET,
                    successCallback: {
                        fn: success,
                        scope: parent
                    },
                    failureMessage: parent._msg("message.getuser-failure", $html(parent.currentUserId))
                });

                // Setup the main datatable
                this._setupDataTable();
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
                registerEventHandler("onDeleteNodeChild", this._onDeleteNodeChild);
                /**
                 * Generic HTML-safe custom datacell formatter
                 */
                var renderCellSafeHTML = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(oData);
                    };

                var renderCellSelect = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");

                        var me = this;
                        elCell.innerHTML = '<input id="checkbox-' + oRecord.getId() + '" type="checkbox" value="' + oRecord.getData("nodeRef") + '">';
                        elCell.firstChild.onclick = function() {
                            parent._updateSelectedItemsChildMenu();
                        };
                    };

                var renderCellCategory = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(parent._msg(oRecord.getData("docCategory")));
                    };

                var renderCellSubject = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(parent._msg(oRecord.getData("docSubject")));
                    };

                var renderCellCreated = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        var meta = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oRecord.getData("created"))));
                        elCell.innerHTML = meta;
                    };

                var renderCellDocDate = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        if (oRecord.getData("docDate") != "") {
                            var meta = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oRecord.getData("docDate")), "yyyy-mm-dd"));
                            elCell.innerHTML = meta;
                        }
                    };

                var renderCellName = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        var uri = Alfresco.constants.PAGEID.replace("adacta-index", "") + "document-details?nodeRef=" + oRecord.getData("nodeRef");
                        elCell.innerHTML = '<a href="' + uri + '">' + oRecord.getData("name") + '</a>';
                    };

                var renderCellActions = function renderCellThumbnail(elCell, oRecord, oColumn, oData) {
                        Dom.setStyle(elCell.parentNode, "vertical-align", "middle");
                        Dom.setStyle(elCell.parentNode, "text-align", "right");

                        var desc = "";

                        desc += '<a class="onIndexDocument" title="' + parent._msg("label.index-document") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-edit-metadata-16.png" title="' + parent._msg("label.index-document") + '"/></a>&nbsp;';
                        desc += '<a class="onDeleteNodeChild" title="' + parent._msg("actions.document.delete") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-delete-16.png" title="' + parent._msg("actions.document.delete") + '"/></a>';

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
                    key: "docDate",
                    label: parent._msg("label.docDate"),
                    sortable: true,
                    formatter: renderCellDocDate
                }];

                var me = this;
                var meParent = parent;

                parent.widgets.pagingDataTableChildren = new Alfresco.util.DataTable({
                    dataTable: {
                        config: {
                            generateRequest: function(oState, oSelf) {
                                var startIndex = (oState.pagination.page - 1) * oState.pagination.rowsPerPage;
                                var sort = encodeURIComponent((oState.sortedBy) ? oState.sortedBy.key : oSelf.getColumnSet().keys[0].getKey());
                                var dir = (oState.sortedBy && oState.sortedBy.dir === YAHOO.widget.DataTable.CLASS_DESC) ? "desc" : "asc";

                                //Save current sort for paging
                                meParent.widgets.pagingDataTableChildren.currentSortKey = sort;
                                meParent.widgets.pagingDataTableChildren.currentDir = dir;

                                // Build the request
                                var query = "?sortBy=" + sort + "&dir=" + dir + "&searchType=" + parent.options.searchType + "&nodeRef=" + parent.currentNodeRef;

                                if (parent.searchTerm || parent.searchTerm == "") {
                                    query = query + "&filter=" + encodeURIComponent(parent.searchTerm) + "&startIndex=" + startIndex + "&pageSize=" + oState.pagination.rowsPerPage;
                                }

                                return query;
                            }
                        },
                        container: parent.id + "-datatable-children",
                        columnDefinitions: columnDefinitions
                    },
                    dataSource: {
                        url: Alfresco.constants.PROXY_URI + "/nl/defensie/adacta/search/filefolder",
                        pagingResolver: function(currentSkipCount, currentMaxItems, currentSortKey, currentDir) {
                        	if (!currentSortKey){
                        		currentSortKey="created";
                        	}
                        	if (!currentDir){
                        		currentDir="asc";
                        	}
                            return "startIndex=" + currentSkipCount + "&" + "pageSize=" + currentMaxItems + "&" + me._buildSearchParams(parent.searchTerm).substring(1) + "&sortBy=" + currentSortKey + "&dir=" + currentDir + "&searchType=" + parent.options.searchType + "&nodeRef=" + parent.currentNodeRef;
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
                            containers: [parent.id + "-paginator-children"],
                            rowsPerPage: parent.options.maxSearchResults
                        }
                    }
                });


                //Show total number of search result
                parent.widgets.pagingDataTableChildren.widgets.dataSource.doBeforeParseData = function doBeforeParseData(oRequest, oFullResponse) {
                    me._setResultsMessage("message.results", oFullResponse.paging.totalItems);

                    return oFullResponse;
                };
            },

            /**
             * Handler for the edit page
             *
             * @method _onIndexDocument
             * @param row {object} DataTable row representing item to be actioned
             */
            _onIndexDocument: function(row) {
                var data = parent.widgets.pagingDataTableChildren.widgets.dataTable.getRecord(row).getData();
                var editDocUrl = Alfresco.util.siteURL("document-details?nodeRef=" + data.nodeRef);
                window.location = window.location.protocol + "//" + window.location.hostname + editDocUrl;
            },

            /**
             * Handler for the delete form
             *
             * @method _onDeleteNode
             * @param row {object} DataTable row representing item to be actioned
             */
            _onDeleteNodeChild: function(row) {
                var data = parent.widgets.pagingDataTableChildren.widgets.dataTable.getRecord(row).getData();

                Alfresco.util.PopupManager.displayForm({
                    title: parent._msg("actions.adactaDeleteNode.label"),
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

                            parent.refreshDataTableChildren();
                        },
                        scope: parent
                    },
                    failureMessage: parent._msg("message.delete.failure", data.name)
                });
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
                dataTable.set("MSG_EMPTY", parent._msg("message.empty", "ADACTA.DatatableIndex"));
                dataTable.set("MSG_ERROR", parent._msg("message.error", "ADACTA.DatatableIndex"));
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
                var resultsDiv = Dom.get(parent.id + "-search-bar-children");
                resultsDiv.innerHTML = parent._msg(messageId, arg1, arg2);
            }

        });
        new ViewPanelHandler();

        return this;
    };

    YAHOO.extend(ADACTA.DatatableIndex, Alfresco.ConsoleTool, {
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
            searchType: ""
        },

        /**
         * Current search term, obtained from form input field.
         *
         * @property searchTerm
         * @type string
         */
        searchTerm: "",


        /**
         * Current nodeRef for an action.
         *
         * @property currentNodeRef
         * @type string
         */
        currentNodeRef: "",

        /**
         * Fired by YUI when parent element is available for scripting.
         * Component initialisation, including instantiation of YUI widgets and event listener binding.
         *
         * @method onReady
         */
        onReady: function Datatable_onReady() {
            // Call super-class onReady() method
            ADACTA.DatatableIndex.superclass.onReady.call(this);
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
            }

            if (state.nodeRef && (this.currentPanelId === "viewFolder" || this.currentPanelId === "search")) {
                this.currentNodeRef = state.nodeRef;
                this.updateCurrentPanel();
            }
        },

        /**
         * Search button click event handler
         *
         * @method onSearchClick
         * @param e {object} DomEvent
         * @param args {array} Event parameters (depends on event type)
         */
        onSearchClick: function Datatable_onSearchClick(e, args) {
            var searchTermElem = Dom.get(this.id + "-search-text");
            var searchTerm = YAHOO.lang.trim(searchTermElem.value);

            // inform the user if the search term entered is too small
            if (searchTerm.length < this.options.minSearchTermLength) {
                Alfresco.util.PopupManager.displayMessage({
                    text: this._msg("message.minimum-length", this.options.minSearchTermLength)
                });
                return;
            }

            this.refreshUIState({
                "search": searchTerm
            });
        },

        /**
         * View folder details.
         */
        onViewFolderClick: function Datatable_onViewFolderClick(e, args) {
            var ref = args[1].nodeRef;
            this.refreshUIState({
                "panel": "viewFolder",
                "nodeRef": ref
            });
        },

        /**
         * Go back button click event handler
         *
         * @method onGoBackClick
         * @param e {object} DomEvent
         * @param args {array} Event parameters (depends on event type)
         */
        onGoBackClick: function ConsoleUsers_onGoBackClick(e, args) {
            this.refreshUIState({
                "panel": "search"
            });
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
            if (this.currentNodeRef !== "") {
                stateObj.nodeRef = this.currentNodeRef;
            }
            if (this.searchTerm !== undefined) {
                stateObj.search = this.searchTerm;
            }

            // convert to encoded url history state - overwriting with any supplied values
            var state = "";
            if (obj.panel || stateObj.panel) {
                state += "panel=" + encodeURIComponent(obj.panel ? obj.panel : stateObj.panel);
            }
            if (obj.nodeRef || stateObj.nodeRef) {
                if (state.length !== 0) {
                    state += "&";
                }
                state += "nodeRef=" + encodeURIComponent(obj.nodeRef ? obj.nodeRef : stateObj.nodeRef);
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
            return Alfresco.util.message.call(this, messageId, "ADACTA.DatatableIndex", Array.prototype.slice.call(arguments).slice(1));
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

            case "release-item":            	
                var failed = [],
                    total = 0;
                for (var i = 0; i < items.length; i++) {
                    // make ajax calls to Delete the items
                    Alfresco.util.Ajax.request({                    	
                    	url: Alfresco.constants.PROXY_URI + "nl/defensie/adacta/allocatebatch/" + items[i].getData("nodeRef").replace(":/", "") + "/clear",
                        method: "GET",
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
                                title: me.msg("message.release.report"),
                                text: me.msg("message.release.report-info", (items.length - failed.length), failed.length)
                            });
                            me.refreshDataTable();
                            me._deselectAll();
                        } else {
                            setTimeout(completeFn, 500);
                        }
                    };
                setTimeout(completeFn, 500);

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

        onActionItemChildClick: function Datatable_onActionItemClick(sType, aArgs, p_obj) {
            var items = [],
                dt = this.widgets.pagingDataTableChildren.getDataTable(),
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
                                        me.refreshDataTableChildren();
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
         * Select items menu item event handler
         * @method onSelectItemClick.
         * @param sType, aArgs, p_obj
         */
        onSelectItemChildClick: function Datatable_onSelectItemClick(sType, aArgs, p_obj) {
            switch (aArgs[1]._oAnchor.className.split(" ")[0]) {
            case "select-all":
                this._selectChildAll();
                break;
            case "select-invert":
                this._invertChildSelection();
                break;
            case "select-none":
                this._deselectChildAll();
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
         * Select all items.
         * @method _selectAll
         */
        _selectChildAll: function Datatable_selectAll() {
            var rows = this.widgets.pagingDataTableChildren.getDataTable().getTbodyEl().rows;
            for (var i = 0; i < rows.length; i++) {
                rows[i].cells[0].getElementsByTagName('input')[0].checked = true;
            }
            this._updateSelectedItemsChildMenu();
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
        },

        /**
         * Deselect all items.
         * @method _deselectAll
         */
        _deselectChildAll: function Datatable_deselectAll() {
            var rows = this.widgets.pagingDataTableChildren.getDataTable().getTbodyEl().rows;
            for (var i = 0; i < rows.length; i++) {
                rows[i].cells[0].getElementsByTagName('input')[0].checked = false;
            }
            this._updateSelectedItemsChildMenu();
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
         * Invert selection of items.
         * @method _invertSelection
         */
        _invertChildSelection: function Datatable_invertSelection() {
            var rows = this.widgets.pagingDataTableChildren.getDataTable().getTbodyEl().rows;
            for (var i = 0; i < rows.length; i++) {
                var check = rows[i].cells[0].getElementsByTagName('input')[0];
                check.checked = !check.checked;
            }
            this._updateSelectedItemsChildMenu();
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
         * Update the disabled status of the multi-select action menu based on the state of the item checkboxes
         * @method _updateSelectedItemsMenu
         */
        _updateSelectedItemsChildMenu: function Datatable_updateSelectedItemsMenu() {
            this.widgets.actionMenu.set("disabled", true);
            var rows = this.widgets.pagingDataTableChildren.getDataTable().getTbodyEl().rows;
            for (var i = 0; i < rows.length; i++) {
                if (rows[i].cells[0].getElementsByTagName('input')[0].checked) {
                    this.widgets.actionMenuChild.set("disabled", false);
                    break;
                }
            }
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
         * Refresh the list after an action has occured
         * 
         * @method refreshDataTable
         */
        refreshDataTableChildren: function Datatable_refreshDataTable() {
            this.widgets.pagingDataTableChildren.loadDataTable(null);
        }

    });
})();