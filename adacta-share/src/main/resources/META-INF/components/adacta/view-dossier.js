/**
 * AdactaPersonnelFile tool component.
 * 
 * @namespace Alfresco
 * @class Alfresco.AdactaPersonnelFile
 * @author Miruna Chirita
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
     * AdactaPersonnelFile constructor.
     * 
     * @param {String} htmlId The HTML id of the parent element
     * @return {Alfresco.AdactaPersonnelFile} The new AdactaPersonnelFile instance
     * @constructor
     */
    Alfresco.AdactaPersonnelFile = function(htmlId) {
        this.name = "Alfresco.AdactaPersonnelFile";
        Alfresco.AdactaPersonnelFile.superclass.constructor.call(this, htmlId);

        /* Register this component */
        Alfresco.util.ComponentManager.register(this);

        /* Load YUI Components */
        Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "paginator", "json", "history"], this.onComponentsLoaded, this);

        /* Define panel handlers */
        var parent = this;

        // NOTE: the panel registered first is considered the "default" view and is displayed first

        /* View Panel Handler */
        ViewPanelHandler = function AdactaPersonnelFile_ViewPanelHandler_constructor() {
            ViewPanelHandler.superclass.constructor.call(this, "view");
        };

        YAHOO.extend(ViewPanelHandler, Alfresco.ConsolePanelHandler, {
            onLoad: function AdactaPersonnelFile_ViewPanelHandler_onLoad() {
                Alfresco.util.createYUIButton(parent, "open-in-tabs-button", parent.onOpenSelectedClick);
                Alfresco.util.createYUIButton(parent, "all-unread-button", parent.onSetAllAsUnreadClick);
                Alfresco.util.createYUIButton(parent, "deselect-all-button", parent.onDeselectAllDocumentsClick);
            },

            onShow: function AdactaPersonnelFile_ViewPanelHandler_onShow() {
            },

            onUpdate: function AdactaPersonnelFile_ViewPanelHandler_onUpdate() {
            	// Hide category filter if category is already given through url parameters
            	if (parent.options.category && parent.options.category.length > 0) {
            		Dom.setStyle(parent.id + "-filterCategory", "display", "none");
            	}
                
            	this.onDataLoad();
            },

            /**
             * Node data loaded successfully. Sets up YUI DataTable instances and other UI elements.
             * 
             * @method onDataLoad
             */
            onDataLoad: function AdactaPersonnelFile_ViewPanelHandler_onDataLoad() {
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

                    
                 var renderCellCheckbox = function renderCellCheckbox (elCell, oRecord, oColumn, oData) {
                        var checkboxLink = document.createElement("input");
                        Dom.setAttribute(checkboxLink, "type", "checkbox");
                        //onthoud wat aangevinkt is
                        if (window.sessionStorage.savedCheckboxVD && window.sessionStorage.savedCheckboxVD.indexOf("" + oRecord.getData("adactaDetailUrl")) > -1) {
                        	Dom.setAttribute(checkboxLink, "checked", true);
                       	} else {
                       		Dom.setAttribute(checkboxLink, "checked", false);
                       	}

                        var clickHandler = function(e) {     
                        	if (checkboxLink.checked){
                        		if (window.sessionStorage.savedCheckboxVD) {
                        			window.sessionStorage.savedCheckboxVD += ";" + oRecord.getData("adactaDetailUrl");
                        		} else {
                        			window.sessionStorage.savedCheckboxVD = oRecord.getData("adactaDetailUrl");
                        		}
                        	}else{
                        		//checkbox is un checked, remove from saved state
                        		if (window.sessionStorage.savedCheckboxVD){
                        			var s = window.sessionStorage.savedCheckboxVD.split(";");
                        			var i = s.indexOf(oRecord.getData("adactaDetailUrl"));
                        			s.splice(i,1);
                        			window.sessionStorage.savedCheckboxVD = s.join(";");
                        		}
                        	}
                        };
                        //or change event?
                        YAHOO.util.Event.addListener(checkboxLink, "click", clickHandler, null, parent);

                    	 elCell.appendChild(checkboxLink);
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

                        if (window.sessionStorage.savedNodesVD && window.sessionStorage.savedNodesVD.indexOf("" + oRecord.getData("nodeRef")) > -1) {
                            viewNodeLink.innerHTML = '<span class="icon16"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-view-content-visited-16.png" /></span>';
                        } else {
                            viewNodeLink.innerHTML = '<span class="icon16"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-view-content-16.png" /></span>';
                        }

                        var clickHandler = function(e) {
                            Dom.setAttribute(viewNodeLink.childNodes[0].childNodes[0], "src", Alfresco.constants.URL_RESCONTEXT + "components/documentlibrary/actions/document-view-content-visited-16.png");
                            if (window.sessionStorage.savedNodesVD) {
                                window.sessionStorage.savedNodesVD += ";" + oRecord.getData("nodeRef");
                            } else {
                                window.sessionStorage.savedNodesVD = oRecord.getData("nodeRef");
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
                    formatter: renderCellCheckbox,
                    sortable: false,
                    className: 'align-center'
                }, {
                    key: "name",
                    label: parent.msg("label.document.show"),
                    sortable: false,
                    width: 32,
                    formatter: renderNodeLink
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
                                var query = "?sortBy=" + sort + "&dir=" + dir + "&searchType=" + parent.options.searchType
                                	+ "&query=" + me.createQuery() + "&startIndex=" + startIndex 
                                	+ "&pageSize=" + oState.pagination.rowsPerPage+ "&selfservice=" + parent.options.selfservice;

                                return query;
                            }
                        },
                        container: parent.id + "-view-node-children",
                        columnDefinitions: columnDefinitions
                    },
                    dataSource: {
                        url: Alfresco.constants.PROXY_URI + "/nl/defensie/adacta/search/filefolder",
                        pagingResolver: function(currentSkipCount, currentMaxItems, currentSortKey, currentDir) {
                            return "startIndex=" + currentSkipCount + "&" + "pageSize=" + currentMaxItems 
                            	+ "&query=" + me.createQuery() + "&sortBy=" + currentSortKey 
                            	+ "&dir=" + currentDir + "&searchType=" + parent.options.searchType+ "&selfservice=" + parent.options.selfservice;
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
                
                // Show total number of search results & name of dossier owner
                parent.widgets.pagingDataTable.widgets.dataSource.doBeforeParseData = function doBeforeParseData(oRequest, oFullResponse) {
                    // Set label for number of results
                    var headerDiv = Dom.get(parent.id + "-search-bar-children");
                    headerDiv.innerHTML = parent._msg("message.children.results", oFullResponse.paging.totalItems);

                    // Set name and employee number of dossier owner in the header bar
                    if (oFullResponse.data.adactaNodes.length > 0) {
                    	setEmplnaam(oFullResponse.data.adactaNodes[0].employeeNumber, oFullResponse.data.adactaNodes[0].employeeName);
                    }

                    var numberDiv = Dom.get(parent.id + "-number");
                    if (oFullResponse.data.adactaNodes.length > 0) {
                    	numberDiv.innerHTML = parent._msg(oFullResponse.data.adactaNodes[0].employeeNumber);
                    }
                    
                    return oFullResponse;
                };
                setEmplnaam : function setEmplnaam(emplid, nameOld){      
                	//zoek dossier met emplid, haal er de naam af
                	// als niet gevonden, geef oude naam terug (= naam op eerste document)
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
                			  nameDiv.innerHTML = parent._msg(nameOld);                			                  			  
                		  }
                	  };
                	  var onFailure = function _onFailure(response) {
                		  nameDiv.innerHTML = nameOld;
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
                };

                // Make main panel area visible
                Dom.setStyle(parent.id + "-view-main", "visibility", "visible");

                parent.widgets.pagingDataTable.loadDataTable();
            },
            
            /**
             * Create query from search term and category (parent parameters).
             * 
             * @method createQuery
             */
            createQuery: function AdactaPersonnelFile_ViewPanelHandler_createQuery() {
            	var query = parent.searchTerm;
            	if (parent.category && parent.category.length > 0 && "" + parent.category !== "Alle") {
            		query += " AND @ada\\:docCategory:'" + parent.category + "'";
            	}
            	if (parent.subject && parent.subject.length > 0 ) {
            		query += " AND @ada\\:docSubject:'" + parent.subject + "'";
            	}
            	return encodeURIComponent(query);
            }
        });
        new ViewPanelHandler();
        
        return this;
    };
    
    YAHOO.extend(Alfresco.AdactaPersonnelFile, Alfresco.ConsoleTool, {
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
             * Current search term, obtained from form input field.
             * 
             * @property searchTerm
             * @type string
             */
            category: "",

            /**
             * Current search term, obtained from form input field.
             * 
             * @property searchTerm
             * @type string
             */
            subject: "",

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
            userIsManager: false,
            /**
             * Call originates from selfservice page.
             * 
             * @property selfservice
             * @type boolean
             */
            selfservice: false
        },

        /**
         * Current search term, obtained from form input field.
         * 
         * @property searchTerm
         * @type string
         */
        searchTerm: "",

        /**
         * Current search term, obtained from form input field.
         * 
         * @property searchTerm
         * @type string
         */
        category: "",
        subject: "",
        /**
         * Fired by YUILoaderHelper when required component script files have
         * been loaded into the browser.
         * 
         * @method onComponentsLoaded
         */
        onComponentsLoaded: function AdactaPersonnelFile_onComponentsLoaded() {
            Event.onContentReady(this.id, this.onReady, this, true);
        },

        /**
         * Fired by YUI when parent element is available for scripting.
         * Component initialisation, including instantiation of YUI widgets and
         * event listener binding.
         * 
         * @method onReady
         */
        onReady: function AdactaPersonnelFile_onReady() {
            // Call super-class onReady() method
            Alfresco.AdactaPersonnelFile.superclass.onReady.call(this);
            
        	var filterCategory = "Alle";
        	
            this.widgets.filterCategoryMenuButton = Alfresco.util.createYUIButton(this, "filterCategory", this.onFilterCategorySelected, {
				type: "menu",
				menu: "filterCategory-menu",
				lazyloadmenu: false
			});
            
            this.widgets.filterCategoryMenuButton.set("label", filterCategory + " " + Alfresco.constants.MENU_ARROW_SYMBOL);
			this.widgets.filterCategoryMenuButton.value = "Alle";
        },
        
        onFilterCategorySelected: function AdactaPersonnelFile_onFilterCategorySelected(p_sType, p_aArgs) {
			var menuItem = p_aArgs[1];

			if (menuItem) {
				this.widgets.filterCategoryMenuButton.set("label", menuItem.cfg.getProperty("text") + " " + Alfresco.constants.MENU_ARROW_SYMBOL);
				this.widgets.filterCategoryMenuButton.value = menuItem.value;

				this.category = menuItem.value;
				
				// Empty results table, then reload
                this.widgets.pagingDataTable.widgets.dataTable.deleteRows(0, this.widgets.pagingDataTable.widgets.dataTable.getRecordSet().getLength());
				this.widgets.pagingDataTable.loadDataTable();
			}
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
        onStateChanged: function AdactaPersonnelFile_onStateChanged(e, args) {
            var state = this.decodeHistoryState(args[1].state);

            // Test if panel has actually changed?
            if (state.panel) {
                this.showPanel(state.panel);
            }
            
            if ((state.searchTerm === undefined || state.searchTerm === "") && this.currentPanelId === "view") {
            	state.searchTerm = YAHOO.lang.trim(this.options.searchTerm);
                this.searchTerm = state.searchTerm;
            }
            
            if ((state.category === undefined || state.category === "") && this.currentPanelId === "view") {
            	state.category = YAHOO.lang.trim(this.options.category);
                this.category = state.category;
            }

            if ((state.subject === undefined || state.subject === "") && this.currentPanelId === "view") {
            	state.subject = YAHOO.lang.trim(this.options.subject);
            	if (state.subject){
            		var sub = state.subject;
            		if (sub.length === 1){
            			sub = "0"+sub;
            			this.subject = this.category+sub;
            		}
            		if (sub.length === 2){
            			this.subject = this.category+sub;
            		}
            		if (sub.length === 4){
            			this.subject = sub;
            		}
            	}
            }

            if ((state.selfservice === undefined || state.selfservice === "") && this.currentPanelId === "view") {
            	state.selfservice = YAHOO.lang.trim(this.options.selfservice);
                this.selfservice = state.selfservice;
            }

            if (state.searchTerm && (this.currentPanelId === "view")) {
                this.updateCurrentPanel();
            }
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
        encodeHistoryState: function AdactaPersonnelFile_encodeHistoryState(obj) {
            // Wrap up current state values
            var stateObj = {};
            if (this.currentPanelId !== "") {
                stateObj.panel = this.currentPanelId;
            }
            if (this.searchTerm !== "") {
                stateObj.searchTerm = this.searchTerm;
            }

            // Convert to encoded url history state - overwriting with any
            // supplied values
            var state = "";
            if (obj.panel || stateObj.panel) {
                state += "panel=" + encodeURIComponent(obj.panel ? obj.panel : stateObj.panel);
            }
            if (obj.searchTerm || stateObj.searchTerm) {
                if (state.length !== 0) {
                    state += "&";
                }
                state += "searchTerm=" + encodeURIComponent(obj.searchTerm ? obj.searchTerm : stateObj.searchTerm);
            }
            if (obj.category || stateObj.category) {
                if (state.length !== 0) {
                    state += "&";
                }
                state += "category=" + encodeURIComponent(obj.category ? obj.category : stateObj.category);
            }
            if (obj.subject || stateObj.subject) {
                if (state.length !== 0) {
                    state += "&";
                }
                state += "subject=" + encodeURIComponent(obj.subject ? obj.subject : stateObj.subject);
            }
            if (obj.selfservice || stateObj.selfservice) {
                if (state.length !== 0) {
                    state += "&";
                }
                state += "selfservice=" + encodeURIComponent(obj.selfservice ? obj.selfservice : stateObj.selfservice);
            }
            return state;
        },
        /**
         * Open all checked documents
         */
        onOpenSelectedClick: function AdactaPersonnelFile_onOpenSelectedClick(e, args) {
            var me = this;

            try {
            	var urls = window.sessionStorage.savedCheckboxVD;
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
                        if (window.sessionStorage.savedNodesVD) {
                            window.sessionStorage.savedNodesVD += ";" + me.getNodeRefFromUrl(url);
                       } else {
                            window.sessionStorage.savedNodesVD = me.getNodeRefFromUrl(url);
                        }
                    	this.widgets.pagingDataTable.loadDataTable();
                        window.open(rows[i]);
                    }
                }
            } catch (error) {
                console.log("Error when handling onOpenSelectedClick: " + error);
            }
        },
        getNodeRefFromUrl: function AdactaPersonnelFile_getNodeRefFromUrl(url){
        	var iss = url.indexOf("=")+1;
        	var noderef = url.substring(iss, url.length); 
        	console.log("returning noderef "+noderef);
        	return noderef;
        },
        /**
         * Set documents as unread click event handler - search panel
         * 
         * @method onSetAllAsUnreadClick
         * @param e
         *            {object} DomEvent
         * @param args
         *            {array} Event parameters (depends on event type)
         */
        onSetAllAsUnreadClick: function AdactaPersonnelFile_onSetAllAsUnreadClick(e, args) {
            window.sessionStorage.removeItem("savedNodesVD");
            this.widgets.pagingDataTable.loadDataTable();
        },
        /**
         * Deselect all documents click handle
         *
         * @method onDeselectAlDocumentsClick
         *
         * @param e {object} DomEvent
         * @param args {array} Event parameters (depends on event type)
         */
        onDeselectAllDocumentsClick: function AdactaPersonnelFile_onDeselectAllDocumentsClick(e, args) {
            window.sessionStorage.removeItem("savedCheckboxVD");
            this.widgets.pagingDataTable.loadDataTable();            
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
        _msg: function AdactaPersonnelFile__msg(messageId) {
            return Alfresco.util.message.call(this, messageId, "Alfresco.AdactaPersonnelFile", Array.prototype.slice.call(arguments).slice(1));
        }
    });
})();