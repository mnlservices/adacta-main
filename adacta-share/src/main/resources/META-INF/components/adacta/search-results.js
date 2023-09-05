/**
 * AdactaDocumentBrowser tool component.
 * 
 * @namespace Alfresco
 * @class Alfresco.AdactaDocumentBrowser
 * @author Miruna Chirita
 */

/**
 * Gets employeeName from dossier and places it in the header. If an error occurs,
 * then place param 'docEmplName'  (the employeeName found on the first document) in the header.
 */
setNameFromDossier : function setNameFromDossier(emplid, docEmplName, parent){       	
     var nameDiv = Dom.get(parent.id + "-name");
     var url = Alfresco.constants.PROXY_URI + "/nl/defensie/adacta/search/dossier-search?emplid="+emplid;
     var onSuccess = function _onSuccess(response) {
    	 var json = response.serverResponse.responseText;
    	 var text;
    	 var naam;
    	 if (json){
    		 text = (JSON.parse(json))[0].text;
    	 }
    	 if (text){
    		 var pipe = text.indexOf("|")
    		 naam = text.substr(0, pipe);
    	 }
    	 if (naam){
    		 nameDiv.innerHTML = parent._msg(naam);                			  
    	 }else{
    		 nameDiv.innerHTML = parent._msg(docEmplName);                			                  			  
    	 }
     };
     var onFailure = function _onFailure(response) {
    	 nameDiv.innerHTML = docEmplName;
     };
     Alfresco.util.Ajax.request({
    	 url: url,
    	 method: Alfresco.util.Ajax.GET,
    	 successCallback: {
    		 fn: onSuccess,
    		 scope: this
    	 },
    	 failureCallback: {
    		 fn: onFailure,
    		 scope: this
    	 }
     });
}


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
     * AdactaDocumentBrowser constructor.
     * 
     * @param {String} htmlId The HTML id of the parent element
     * @return {Alfresco.AdactaDocumentBrowser} The new AdactaDocumentBrowser instance
     * @constructor
     */
    Alfresco.AdactaDocumentBrowser = function(htmlId) {
        this.name = "Alfresco.AdactaDocumentBrowser";
        Alfresco.AdactaDocumentBrowser.superclass.constructor.call(this, htmlId);

        /* Register this component */
        Alfresco.util.ComponentManager.register(this);

        /* Load YUI Components */
        Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "paginator", "json", "history"], this.onComponentsLoaded, this);

        /* Decoupled event listeners */
        YAHOO.Bubbling.on("viewNodeClick", this.onViewNodeClick, this);

        /* Define panel handlers */
        var parent = this;

        // NOTE: the panel registered first is considered the "default" view and is displayed first
        /* Search Panel Handler */
        SearchPanelHandler = function AdactaDocumentBrowser_SearchPanelHandler_constructor() {
            SearchPanelHandler.superclass.constructor.call(this, "search");
        };

        YAHOO.extend(SearchPanelHandler, Alfresco.ConsolePanelHandler, {
            /**
             * INSTANCE VARIABLES
             */

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
             * Called by the ConsolePanelHandler when this panel shall
             * be loaded
             * 
             * @method onLoad
             */
            onLoad: function AdactaDocumentBrowser_SearchPanelHandler_onLoad() {
                Alfresco.util.createYUIButton(parent, "all-unread-button-search", parent.onSetAllAsUnreadSearchClick);
                
                if (parent.options.searchTerm.indexOf("TYPE:\"ada:dossier") > -1) {
                	//Dom.setStyle(parent.id + "-all-unread-button-search", "display", "none");
                	Dom.setStyle(parent.id + "-document-components", "display", "none");
                }
                
                // Setup the main datatable
                this._setupDataTable();
                parent.widgets.pagingDataTable.loadDataTable();
            },

            onShow: function AdactaDocumentBrowserr_SearchPanelHandler_onShow() {

            },

            onUpdate: function AdactaDocumentBrowser_SearchPanelHandler_onUpdate() {
                // Check search length again as we may have got here via history navigation
                if (!this.isSearching && parent.searchTerm !== undefined && parent.searchTerm.length >= parent.options.minSearchTermLength) {
                    this.isSearching = true;

                    var me = this;

                    // Reset the custom error messages
                    me._setDefaultDataTableErrors(parent.widgets.pagingDataTable.widgets.dataTable);

                    // Don't display any message
                    parent.widgets.pagingDataTable.widgets.dataTable.set("MSG_EMPTY", parent._msg("message.datatable.empty"));

                    // Empty results table
                    var startShowIndex = parent.widgets.pagingDataTable.widgets.dataTable.getRecordSet().getLength() - parent.widgets.pagingDataTable.currentMaxItems;
                    parent.widgets.pagingDataTable.widgets.dataTable.deleteRows(startShowIndex, parent.widgets.pagingDataTable.widgets.dataTable.getRecordSet().getLength());

                    // Clear sorting in paginator
                    parent.widgets.pagingDataTable.currentSortKey = null;
                    parent.widgets.pagingDataTable.currentDir = null;

                    var successHandler = function AdactaDocumentBrowser_SearchPanelHandler_onUpdate_successHandler(sRequest, oResponse, oPayload) {
                            me._enableSearchUI();
                            me._setDefaultDataTableErrors(parent.widgets.pagingDataTable.widgets.dataTable);
                            parent.widgets.pagingDataTable.widgets.dataTable.onDataReturnInitializeTable.call(
                            parent.widgets.pagingDataTable.widgets.dataTable, sRequest, oResponse, oPayload);
                            var data = parent.widgets.pagingDataTable.widgets.dataTable.getRecord(row).getData();
                        };

                    var failureHandler = function AdactaDocumentBrowser_SearchPanelHandler_onUpdate_failureHandler(sRequest, oResponse) {
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
                }
            },

            /**
             * Handler for the download dossier action
             * 
             * @method _onDownloadDossier
             * @param row {object} DataTable row representing item to be actioned
             */
            _onDownloadDossier: function(row) {
                var data = parent.widgets.pagingDataTable.widgets.dataTable.getRecord(row).getData();
         	    var url = Alfresco.constants.PROXY_URI + "/nl/defensie/adacta/children-open/workspace/SpacesStore/"+data.id;
                var onSuccess = function _onSuccess(response) {
                	var nodesArray = [];		  
                	var result = JSON.parse(response.serverResponse.responseText);
                    for (var k = 0, kk = result.nodeRefs.length; k < kk; k++) {
                    	var nr  = result.nodeRefs[k].nodeRef;
                    	nodesArray.push({"nodeRef":nr});
                    }              		
                    var downloadDialog = Alfresco.getArchiveAndDownloadInstance(),
                    config = {
                    	nodesToArchive: nodesArray,
                        archiveName: data.name
                    };
                downloadDialog.show(config);
                };
                var onFailure = function _onFailure(response) {
                };
                Alfresco.util.Ajax.request({
                	url: url,
                	method: Alfresco.util.Ajax.GET,
                	successCallback: {
                		fn: onSuccess,
                		scope: this
                	},
                	failureCallback: {
                		fn: onFailure,
                		scope: this
                	}
                });
            },

            /**
             * Enable search button, hide the pending wait message and set the panel as not searching.
             * 
             * @method _enableSearchUI
             * @private
             */
            _enableSearchUI: function AdactaDocumentBrowser_SearchPanelHandler_enableSearchUI() {
                // Close the wait feedback message if present
                if (this.widgets.feedbackMessage && this.widgets.feedbackMessage.cfg.getProperty("visible")) {
                    this.widgets.feedbackMessage.hide();
                }
                this.isSearching = false;
            },

            /**
             * Setup the YUI DataTable with custom renderers.
             * 
             * @method _setupDataTable
             * @private
             */
            _setupDataTable: function AdactaDocumentBrowser_SearchPanelHandler_setupDataTable() {
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

                registerEventHandler("onDownloadDossier", this._onDownloadDossier);

                /**
                 * DataTable Cell Renderers
                 * 
                 * Each cell has a custom renderer defined as a custom function. 
                 * See YUI documentation for details. These MUST be inline in order to have access to the parent
                 * instance (via the "parent" variable).
                 */

                /**
                 * Generic HTML-safe custom datacell formatter
                 */
                var renderCellSafeHTML = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(oData);
                    };

                /**
                 * Date custom datacell formatter
                 */
                var renderCellDate = function renderCellDate(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oData), "yyyy-mm-dd"));
                    };

                /**
                 * Datetime custom datacell formatter
                 */
                var renderCellDatetime = function renderCellDatetime(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oData), "yyyy-mm-dd hh:MM:ss"));
                    };

                /**
                 * View id and link datacell formatter
                 * 
                 * @method renderLink
                 */
                var renderLink = function renderLink(elCell, oRecord, oColumn, oData) {
                    // Create view node link
                    var viewNodeLink = document.createElement("a");
                    Dom.setAttribute(viewNodeLink, "href", "#");
                    viewNodeLink.innerHTML = $html(oData);

                    // Fire the 'viewNodeLink' event when the selected node in the list has changed
                    YAHOO.util.Event.addListener(viewNodeLink, "click", function(e) {
                        YAHOO.util.Event.preventDefault(e);
                        YAHOO.Bubbling.fire('viewNodeClick', {
                            nodeRef: oRecord.getData("nodeRef")
                        });
                    }, null, parent);
                    elCell.appendChild(viewNodeLink);
                };

                /**
                 * View icon and link datacell formatter
                 * 
                 * @method renderDocViewLink
                 */
                var renderDocViewLink = function renderDocViewLink(elCell, oRecord, oColumn, oData) {
                    // Create view node link
                    var viewNodeLink = document.createElement("a");
                    Dom.setAttribute(viewNodeLink, "href", oRecord.getData("adactaDetailUrl"));

                    if (window.sessionStorage.savedNodes && window.sessionStorage.savedNodes.indexOf("" + oRecord.getData("nodeRef")) > -1) {
                        viewNodeLink.innerHTML = '<span class="icon16"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-view-content-visited-16.png" /></span>';
                    } else {
                        viewNodeLink.innerHTML = '<span class="icon16"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-view-content-16.png" /></span>';
                    }

                    // Fire the 'viewNodeLink' event when the selected node in the list has changed
                    YAHOO.util.Event.addListener(viewNodeLink, "click", function(e) {
                        Dom.setAttribute(viewNodeLink.childNodes[0].childNodes[0], "src", Alfresco.constants.URL_RESCONTEXT + "components/documentlibrary/actions/document-view-content-visited-16.png");
                        if (window.sessionStorage.savedNodes) {
                            window.sessionStorage.savedNodes += ";" + oRecord.getData("nodeRef");
                        } else {
                            window.sessionStorage.savedNodes = oRecord.getData("nodeRef");
                        }
                    }, null, parent);
                    elCell.appendChild(viewNodeLink);
                };

                var renderCellCategory = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(parent._msg(oRecord.getData("docCategory")));
                    };

                var renderCellSubject = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(parent._msg(oRecord.getData("docSubject")));
                    };

                var renderCellActions = function renderCellActions(elCell, oRecord, oColumn, oData) {
                    Dom.setStyle(elCell.parentNode, "vertical-align", "middle");
                    Dom.setStyle(elCell.parentNode, "text-align", "right");

                    var desc = "";
                    desc += '<a class="onDownloadDossier" title="' + parent._msg("label.index-document") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-download-16.png" title="' + parent._msg("label.download-dossier") + '"/></a>&nbsp;';
                    elCell.innerHTML = desc;
                };

                // DataTable column defintions
                var columnDefinitions = [];

                if (parent.options.searchTerm.indexOf("TYPE:\"ada:dossier") > -1) {

                    // Only add the actions column (download dossier) if user is manager
                    if (parent.options.isAdactaAdmin || parent.options.canDownload) {
                        columnDefinitions.push({
                            key: "actions",
                            label: parent._msg("label.actions"),
                            sortable: false,
                            formatter: renderCellActions,
                            width: 40
                        });
                    }

                    columnDefinitions.push({
                        key: "name",
                        label: parent._msg("label.employeeBsn"),
                        sortable: true,
                        formatter: renderLink
                    }, {
                        key: "employeeName",
                        label: parent._msg("label.employeeName"),
                        sortable: true,
                        formatter: renderCellSafeHTML
                    }, {
                        key: "employeeMrn",
                        label: parent._msg("label.employeeMrn"),
                        sortable: true,
                        formatter: renderCellSafeHTML
                    }, {
                        key: "employeeNumber",
                        label: parent._msg("label.employeeNumber"),
                        sortable: true,
                        formatter: renderCellSafeHTML
                    });
                } else {
                    columnDefinitions.push({
                        key: "name",
                        label: parent._msg("label.document.show"),
                        sortable: false,
                        width: 32,
                        formatter: renderDocViewLink
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
                        key: "docCaseNumber",
                        label: parent._msg("label.docCaseNumber"),
                        sortable: true,
                        formatter: renderCellSafeHTML
                    }, {
                        key: "docWorkDossier",
                        label: parent._msg("label.docWorkDossier"),
                        sortable: true,
                        formatter: renderCellSafeHTML
                    }, {
                        key: "docDate",
                        label: parent._msg("label.docDate"),
                        sortable: true,
                        formatter: renderCellDate
                    }, {
                        key: "docReference",
                        label: parent._msg("label.docReference"),
                        sortable: true,
                        formatter: renderCellSafeHTML
                    }, {
                        key: "created",
                        label: parent._msg("label.document.created"),
                        sortable: true,
                        formatter: renderCellDatetime
                    });
                }

                var me = this;
                var meParent = parent;

                // Paging DataTable definition
                parent.widgets.pagingDataTable = new Alfresco.util.DataTable({
                    dataTable: {
                        config: {
                            generateRequest: function(oState, oSelf) {
                                var startIndex = (oState.pagination.page - 1) * oState.pagination.rowsPerPage;
                                var sort = encodeURIComponent((oState.sortedBy) ? oState.sortedBy.key : oSelf.getColumnSet().keys[0].getKey());
                                var dir = (oState.sortedBy && oState.sortedBy.dir === YAHOO.widget.DataTable.CLASS_DESC) ? "desc" : "asc";

                                // Save current sort for paging
                                meParent.widgets.pagingDataTable.currentSortKey = sort;
                                meParent.widgets.pagingDataTable.currentDir = dir;

                                // Build the request
                                var query = "?sortBy=" + sort + "&dir=" + dir + "&searchType=" + parent.options.searchType;

                                if (parent.searchTerm || parent.searchTerm == "") {
                                    query = query + "&query=" + encodeURIComponent(parent.searchTerm) + "&startIndex=" + startIndex + "&pageSize=" + oState.pagination.rowsPerPage;
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
                            return "startIndex=" + currentSkipCount + "&" + "pageSize=" + currentMaxItems + "&" + me._buildSearchParams(parent.options.searchTerm).substring(1) + "&sortBy=" + currentSortKey + "&dir=" + currentDir + "&searchType=" + parent.options.searchType;
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

                // Show total number of search result
                parent.widgets.pagingDataTable.widgets.dataSource.doBeforeParseData = function doBeforeParseData(oRequest, oFullResponse) {
                	var nameDiv = Dom.get(parent.id + "-name");
                	var numberDiv = Dom.get(parent.id + "-number");
                    if (oFullResponse.data.adactaNodes && oFullResponse.data.adactaNodes.length > 0) {
                    	me._setResultsMessage("message.results", oFullResponse.paging.totalItems);  
                    	setNameFromDossier(oFullResponse.data.adactaNodes[0].employeeNumber,oFullResponse.data.adactaNodes[0].employeeName, parent)
                    	numberDiv.innerHTML = parent._msg(oFullResponse.data.adactaNodes[0].employeeNumber);
                    }else{
                    	me._setResultsMessage("message.results", 0); 
                    	nameDiv.innerHTML = "";
                        numberDiv.innerHTML = "";
                    }
                    return oFullResponse;
                };                
            },

            /**
             * Resets the YUI DataTable errors to our custom messages
             * NOTE: Scope could be YAHOO.widget.DataTable, so can't use
             * "this"
             * 
             * @method _setDefaultDataTableErrors
             * @param dataTable
             *            {object} Instance of the DataTable
             * @private
             */
            _setDefaultDataTableErrors: function AdactaDocumentBrowser_SearchPanelHandler_setDefaultDataTableErrors(dataTable) {
                dataTable.set("MSG_EMPTY", parent._msg("message.datatable.empty"));
                dataTable.set("MSG_ERROR", parent._msg("message.datatable.error"));
            },

            /**
             * Build URI parameters for People List JSON data webscript
             * 
             * @method _buildSearchParams
             * @param searchTerm
             *            {string} User search term
             * @private
             */
            _buildSearchParams: function AdactaDocumentBrowser_SearchPanelHandler_buildSearchParams(searchTerm) {
                return "?query=" + encodeURIComponent(searchTerm);
            },

            /**
             * Set the message in the Results Bar area
             * 
             * @method _setResultsMessage
             * @param messageId
             *            {string} The messageId to display
             * @private
             */
            _setResultsMessage: function AdactaDocumentBrowser_SearchPanelHandler_setResultsMessage(messageId) {
                var resultsDiv = Dom.get(parent.id + "-search-bar");
                resultsDiv.innerHTML = parent._msg.apply(this, arguments);
            }
        });
        new SearchPanelHandler();

        /* View Panel Handler */
        ViewPanelHandler = function AdactaDocumentBrowser_ViewPanelHandler_constructor() {
            ViewPanelHandler.superclass.constructor.call(this, "view");
        };

        YAHOO.extend(ViewPanelHandler, Alfresco.ConsolePanelHandler, {
            onLoad: function AdactaDocumentBrowser_ViewPanelHandler_onLoad() {
                // Buttons
                Alfresco.util.createYUIButton(parent, "goback-button-top", parent.onGoBackClick);
                Alfresco.util.createYUIButton(parent, "all-unread-button", parent.onSetAllAsUnreadClick);
                Alfresco.util.createYUIButton(parent, "open-in-tabs-button", parent.onOpenSelectedClick);
                Alfresco.util.createYUIButton(parent, "deselect-all-button", parent.onDeselectAllDocumentsClick);
            },

            onBeforeShow: function AdactaDocumentBrowser_ViewPanelHandler_onBeforeShow() {
                // Hide the main panel area before it is displayed - so we don't
                // show old data to the user before the Update() method paints the results
                Dom.setStyle(parent.id + "-view-main", "visibility", "hidden");
            },

            onShow: function AdactaDocumentBrowser_ViewPanelHandler_onShow() {

            },

            onUpdate: function AdactaDocumentBrowser_ViewPanelHandler_onUpdate() {
                this.onDataLoad();
            },

            /**
             * Node data loaded successfully. Sets up YUI DataTable instances and other UI elements.
             * 
             * @method onDataLoad
             */
            onDataLoad: function AdactaDocumentBrowser_ViewPanelHandler_onDataLoad() {
                /**
                 * DataTable Cell Renderers
                 * 
                 * Each cell has a custom renderer defined as a custom function.
                 * See YUI documentation for details. These MUST be inline in
                 * order to have access to the parent instance (via the "parent" variable).
                 */

                /**
                 * Generic HTML-safe custom datacell formatter
                 */
                var renderCellSafeHTML = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(oData);
                    };

                var renderCellCheckbox = function renderCellCheckbox (elCell, oRecord, oColumn, oData) {
                    var checkboxLink = document.createElement("input");
                    Dom.setAttribute(checkboxLink, "type", "checkbox");
                    //onthoud wat aangevinkt is
                    if (window.sessionStorage.savedCheckbox && window.sessionStorage.savedCheckbox.indexOf("" + oRecord.getData("adactaDetailUrl")) > -1) {
                    	Dom.setAttribute(checkboxLink, "checked", true);
                   	} else {
                   		Dom.setAttribute(checkboxLink, "checked", false);
                   	}

                    var clickHandler = function(e) {     
                    	if (checkboxLink.checked){
                    		if (window.sessionStorage.savedCheckbox) {
                    			window.sessionStorage.savedCheckbox += ";" + oRecord.getData("adactaDetailUrl");
                    		} else {
                    			window.sessionStorage.savedCheckbox = oRecord.getData("adactaDetailUrl");
                    		}
                    	}else{
                    		//checkbox is un checked, remove from saved state
                    		if (window.sessionStorage.savedCheckbox){
                    			var s = window.sessionStorage.savedCheckbox.split(";");
                    			var i = s.indexOf(oRecord.getData("adactaDetailUrl"));
                    			s.splice(i,1);
                    			window.sessionStorage.savedCheckbox = s.join(";");
                    		}
                    	}
                    };
                    //or change event?
                    YAHOO.util.Event.addListener(checkboxLink, "click", clickHandler, null, parent);

                	 elCell.appendChild(checkboxLink);
                };

                /**
                 * Date custom datacell formatter
                 */
                var renderCellDate = function renderCellDate(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oData), "yyyy-mm-dd"));
                    };

                /**
                 * Datetime custom datacell formatter
                 */
                var renderCellDatetime = function renderCellDatetime(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oData), "yyyy-mm-dd hh:MM:ss"));
                    };

                var renderCellCategory = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(parent._msg(oRecord.getData("docCategory")));
                    };

                var renderCellSubject = function renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
                        elCell.innerHTML = $html(parent._msg(oRecord.getData("docSubject")));
                    };

                /**
                 * Node view link datacell formatter
                 * 
                 * @method renderNodeLink
                 */
                var renderNodeLink = function renderNodeLink(elCell, oRecord, oColumn, oData) {
                    // Create view node link
                    var viewNodeLink = document.createElement("a");
                    Dom.setAttribute(viewNodeLink, "href", oRecord.getData("adactaDetailUrl"));
                    Dom.setAttribute(viewNodeLink, "target", "_blank");

                    if (window.sessionStorage.savedNodes && window.sessionStorage.savedNodes.indexOf("" + oRecord.getData("nodeRef")) > -1) {
                        viewNodeLink.innerHTML = '<span class="icon16"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-view-content-visited-16.png" /></span>';
                    } else {
                        viewNodeLink.innerHTML = '<span class="icon16"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-view-content-16.png" /></span>';
                    }

                    var clickHandler = function(e) {
                        Dom.setAttribute(viewNodeLink.childNodes[0].childNodes[0], "src", Alfresco.constants.URL_RESCONTEXT + "components/documentlibrary/actions/document-view-content-visited-16.png");
                        if (window.sessionStorage.savedNodes) {
                            window.sessionStorage.savedNodes += ";" + oRecord.getData("nodeRef");
                        } else {
                            window.sessionStorage.savedNodes = oRecord.getData("nodeRef");
                        }
                    };

                    var currentOverlay = null;

                    var mouseOverHandler = function(e) {
                        try {
                            currentOverlay = Alfresco.util.createYUIOverlay(
                                "#bd", {
                                    context: [viewNodeLink, "tl", "bl"],
                                    zIndex: 2,
                                    visible: true
                                }, {
                                    render: true,
                                });

                            currentOverlay.setBody("<div class='adacta-doc-previewer'>" +
                                "<div><img height='200px' alt='preview' src='" + Alfresco.constants.PROXY_URI + "/api/node/workspace/SpacesStore/" +
                                oRecord.getData("id") + "/content/thumbnails/doclib?c=force'" + " /></div></div>");
                        } catch (e) {
                            console.log(e);
                        }
                    };

                    var mouseOutHandler = function(e) {
                        if (currentOverlay) {
                            currentOverlay.destroy();
                            currentOverlay = null;
                        }
                    };

                    // Fire the 'viewNodeLink' event when the selected node in the list has changed
                    YAHOO.util.Event.addListener(viewNodeLink, "click", clickHandler, null, parent);
                    // Show Overlay when hovering over link
                    YAHOO.util.Event.addListener(viewNodeLink, "mouseover", mouseOverHandler, null, parent);
                    YAHOO.util.Event.addListener(viewNodeLink, "mouseout", mouseOutHandler, null, parent);

                    elCell.appendChild(viewNodeLink);
                };

                var columnDefinitions = [{
                    key: "checked",
                    allowHTML:  true,
                    label: "",
                    //formatter: YAHOO.widget.DataTable.formatCheckbox,
                    formatter: renderCellCheckbox,
                    sortable: false,
                    className: 'align-center'
                }, {
                    key: "name",
                    label: parent.msg("label.document.show"),
                    sortable: false,
                    width: 32,
                    formatter: renderNodeLink,
                    className: 'align-center'
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
                    key: "docCaseNumber",
                    label: parent._msg("label.docCaseNumber"),
                    sortable: true,
                    formatter: renderCellSafeHTML
                }, {
                    key: "docWorkDossier",
                    label: parent._msg("label.docWorkDossier"),
                    sortable: true,
                    formatter: renderCellSafeHTML
                }, {
                    key: "docDate",
                    label: parent._msg("label.docDate"),
                    sortable: true,
                    formatter: renderCellDate
                }, {
                    key: "docReference",
                    label: parent._msg("label.docReference"),
                    sortable: true,
                    formatter: renderCellSafeHTML
                }, {
                    key: "created",
                    label: parent._msg("label.document.created"),
                    sortable: true,
                    formatter: renderCellDatetime
                }];

                var me = this;
                var meParent = parent;

                // Paging DataTable definition for view pane (children view)
                parent.widgets.childrenDT = new Alfresco.util.DataTable({
                    dataTable: {
                        config: {
                            MSG_SORTASC: parent.msg("column.sort-ascending"),
                            MSG_SORTDESC: parent.msg("column.sort-descending"),
                            generateRequest: function(oState, oSelf) {
                                var startIndex = (oState.pagination.page - 1) * oState.pagination.rowsPerPage;
                                var sort = encodeURIComponent((oState.sortedBy) ? oState.sortedBy.key : oSelf.getColumnSet().keys[0].getKey());
                                var dir = (oState.sortedBy && oState.sortedBy.dir === YAHOO.widget.DataTable.CLASS_DESC) ? "desc" : "asc";

                                // Save current sort for paging
                                meParent.widgets.pagingDataTable.currentSortKey = sort;
                                meParent.widgets.pagingDataTable.currentDir = dir;
                                // Build the request
                                var query = "?sortBy=" + sort + "&dir=" + dir + "&searchType=" + parent.options.searchType;

                                if (parent.currentNodeRef) {
                                    query = query + "&nodeRef=" + parent.currentNodeRef + "&startIndex=" + startIndex + "&pageSize=" + oState.pagination.rowsPerPage;
                                }

                                return query;
                            }
                        },
                        container: parent.id + "-view-node-children",
                        columnDefinitions: columnDefinitions
                    },
                    dataSource: {
                        url: Alfresco.constants.PROXY_URI + "/nl/defensie/adacta/search/filefolder",
                        pagingResolver: function(currentSkipCount, currentMaxItems, currentSortKey, currentDir) {
                        	var savedSort = window.sessionStorage.savedSort;
                        	if (savedSort){
                        		var ss = JSON.parse(savedSort);
                        		sort = ss.oColumn.key;
                        		dir =  ss.sSortDir;
                        		if (dir && dir.indexOf("asc")>0){
                        			dir ="asc";
                        		}else{
                        			dir="desc";
                        		}
                        		currentSortKey = sort;
                        		currentDir = dir;
                        	}
                            return "startIndex=" + currentSkipCount + "&" + "pageSize=" + currentMaxItems + "&nodeRef=" + parent.currentNodeRef + "&sortBy=" + currentSortKey + "&dir=" + currentDir + "&searchType=" + parent.options.searchType;
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
                

                parent.widgets.childrenDT.widgets.dataTable.doBeforeSortColumn = function DataGrid_doBeforeSortColumn(oColumn, sSortDir) {
                    var sort = {
                        oColumn: oColumn,
                        sSortDir: sSortDir
                    };
                    window.sessionStorage.savedSort = JSON.stringify(sort);
                    return true;
                };

                // Show total number of search results & name of dossier owner
                parent.widgets.childrenDT.widgets.dataSource.doBeforeParseData = function doBeforeParseData(oRequest, oFullResponse) {
                    // Set label for number of results
                    var headerDiv = Dom.get(parent.id + "-search-bar-children");
                    var numberDiv = Dom.get(parent.id + "-number");
                    var nameDiv = Dom.get(parent.id + "-name");
                    if (oFullResponse.data.adactaNodes && oFullResponse.data.adactaNodes.length > 0) {
                    	headerDiv.innerHTML = parent._msg("message.children.results", oFullResponse.paging.totalItems);
                    	//get Name of employee from Dossier, not from documents
                        setNameFromDossier(oFullResponse.data.adactaNodes[0].employeeNumber,oFullResponse.data.adactaNodes[0].employeeName, parent)
                    	numberDiv.innerHTML = parent._msg(oFullResponse.data.adactaNodes[0].employeeNumber);
                    }else{
                    	 headerDiv.innerHTML = parent._msg("message.children.results", 0);
                    	 nameDiv.innerHTML = "";
                    	 numberDiv.innerHTML = "";
                    }
                    
                    return oFullResponse;
                };

                parent.widgets.childrenDT.widgets.dataTable.on("checkboxClickEvent", function(oArgs) {
                    try {
                        var checked = oArgs.target.checked;
                        if (checked) {
                            parent.widgets.childrenDT.getDataTable().selectRow(parent.widgets.childrenDT.getRecord(oArgs.target));
                        } else {
                            parent.widgets.childrenDT.getDataTable().unselectRow(parent.widgets.childrenDT.getRecord(oArgs.target));
                        }
                    } catch (error) {
                        console.log("Error while handling checkboxClickEvent: " + error);
                    }
                });

                // Make main panel area visible
                Dom.setStyle(parent.id + "-view-main", "visibility", "visible");

                parent.widgets.childrenDT.loadDataTable();
            }
        });
        new ViewPanelHandler();

        return this;
    };

    YAHOO.extend(Alfresco.AdactaDocumentBrowser, Alfresco.ConsoleTool, {
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
             * Current search term, obtained from form input field.
             * 
             * @property searchTerm
             * @type string
             */
            searchTerm: "",

            /**
             * Set search type for backend search query.
             * 
             * @property searchType
             * @type string
             */
            searchType: "",

            /**
             * Set user rights - manager or not.
             * 
             * @property userIsManager
             * @type boolean
             */
            isAdactaAdmin: false,
            /**
             * Set user rights - has download permission.
             * 
             * @property canDownload
             * @type boolean
             */
			canDownload: false
        },

        /**
         * Current node ref if viewing a node.
         * 
         * @property currentNodeRef
         * @type string
         */
        currentNodeRef: "",

        /**
         * Current search term, obtained from form input field.
         * 
         * @property searchTerm
         * @type string
         */
        searchTerm: "",

        /**
         * Fired by YUILoaderHelper when required component script files have
         * been loaded into the browser.
         * 
         * @method onComponentsLoaded
         */
        onComponentsLoaded: function AdactaDocumentBrowser_onComponentsLoaded() {
            Event.onContentReady(this.id, this.onReady, this, true);
        },

        /**
         * Fired by YUI when parent element is available for scripting.
         * Component initialisation, including instantiation of YUI widgets and
         * event listener binding.
         * 
         * @method onReady
         */
        onReady: function AdactaDocumentBrowser_onReady() {
            // Call super-class onReady() method
            Alfresco.AdactaDocumentBrowser.superclass.onReady.call(this);
        },

        /**
         * YUI WIDGET EVENT HANDLERS Handlers for standard events fired from YUI
         * widgets, e.g. "click"
         */

        /**
         * History manager state change event handler (override base class)
         * 
         * @method onStateChanged
         * @param e {object} DomEvent
         * @param args {array} Event parameters (depends on event type)
         */
        onStateChanged: function AdactaDocumentBrowser_onStateChanged(e, args) {
            var state = this.decodeHistoryState(args[1].state);

            // Test if panel has actually changed?
            if (state.panel) {
                this.showPanel(state.panel);
            }

            if ((state.search === undefined || state.search === "") && this.currentPanelId === "search") {
                state.search = YAHOO.lang.trim(this.options.searchTerm);
                this.searchTerm = state.search;
            }
            
            if (state.nodeRef && (this.currentPanelId === "view")) {
                this.currentNodeRef = state.nodeRef;
                this.updateCurrentPanel();
            }
        },

        /**
         * View Node event handler
         * 
         * @method onViewNodeClick
         * @param e
         *            {object} DomEvent
         * @param args
         *            {array} Event parameters (depends on event type)
         */
        onViewNodeClick: function AdactaDocumentBrowser_onViewNodeClick(e, args) {
            var nodeRef = args[1].nodeRef;
            this.refreshUIState({
                "panel": "view",
                "nodeRef": nodeRef
            });
        },

        /**
         * Go back button click event handler
         * 
         * @method onGoBackClick
         * @param e
         *            {object} DomEvent
         * @param args
         *            {array} Event parameters (depends on event type)
         */
        onGoBackClick: function AdactaDocumentBrowser_onGoBackClick(e, args) {
            this.refreshUIState({
                "panel": "search"
            });
        },

        /**
         * Deselect all documents click handle
         *
         * @method onDeselectAlDocumentsClick
         *
         * @param e {object} DomEvent
         * @param args {array} Event parameters (depends on event type)
         */
        onDeselectAllDocumentsClick: function AdactaDocumentBrowser_onDeselectAllDocumentsClick(e, args) {
            window.sessionStorage.removeItem("savedCheckbox");
            this.widgets.childrenDT.loadDataTable();            
        },
        
        /**
         * Set documents as unread click event handler - view panel
         * 
         * @method onSetAllAsUnreadClick
         * @param e
         *            {object} DomEvent
         * @param args
         *            {array} Event parameters (depends on event type)
         */
        onSetAllAsUnreadClick: function AdactaDocumentBrowser_onSetAllAsUnreadClick(e, args) {
            window.sessionStorage.removeItem("savedNodes");
            this.widgets.childrenDT.loadDataTable();
        },

        /**
         * Open all checked documents
         */
        onOpenSelectedClick: function AdactaDocumentBrowser_onOpenSelectedClick(e, args) {
            var me = this;
            var datatable = me.widgets.childrenDT.getDataTable();

            try {
            	var urls = window.sessionStorage.savedCheckbox;
            	var rows = [];
            	if (urls){
            		rows = urls.split(";");
            	}
                //var rows = datatable.getSelectedRows();
                if (!rows || rows.length < 1) {
                    Alfresco.util.PopupManager.displayMessage({
                        text: me._msg("label.opendocuments-noselection")
                    });
                } else {
                    for (var i = 0; i < rows.length; i++) {
                    	var url = rows[i];
                        if (window.sessionStorage.savedNodes) {
                            window.sessionStorage.savedNodes += ";" + me.getNodeRefFromUrl(url);
                        } else {
                            window.sessionStorage.savedNodes = me.getNodeRefFromUrl(url);
                        }
                        this.widgets.childrenDT.loadDataTable();    
                        window.open(rows[i]);
                    }
                }
            } catch (error) {
                console.log("Error when handling onOpenSelectedClick: " + error);
            }
        },
        getNodeRefFromUrl: function AdactaDocumentBrowser_getNodeRefFromUrl(url){
        	var iss = url.indexOf("=")+1;
        	var noderef = url.substring(iss, url.length); 
        	return noderef;
        },
        /**
         * Set documents as unread click event handler - search panel
         * 
         * @method onSetAllAsUnreadSearchClick
         * @param e
         *            {object} DomEvent
         * @param args
         *            {array} Event parameters (depends on event type)
         */
        onSetAllAsUnreadSearchClick: function AdactaDocumentBrowser_onSetAllAsUnreadSearchClick(e, args) {
            window.sessionStorage.removeItem("savedNodes");
            this.widgets.pagingDataTable.loadDataTable();
        },
        
        /**
         * Encode state object into a packed string for use as url history
         * value. Override base class.
         * 
         * @method encodeHistoryState
         * @param obj
         *            {object} state object
         * @private
         */
        encodeHistoryState: function AdactaDocumentBrowser_encodeHistoryState(obj) {
            // Wrap up current state values
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

            // Convert to encoded url history state - overwriting with any
            // supplied values
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
         * @param messageId
         *            {string} The messageId to retrieve
         * @return {string} The custom message
         * @private
         */
        _msg: function AdactaDocumentBrowser__msg(messageId) {
            return Alfresco.util.message.call(this, messageId, "Alfresco.AdactaDocumentBrowser", Array.prototype.slice.call(arguments).slice(1));
        }
    });
})();