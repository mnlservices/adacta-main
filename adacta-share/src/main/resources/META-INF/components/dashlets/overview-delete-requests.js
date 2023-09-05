if (typeof ADACTA === undefined || !ADACTA) {
    var ADACTA = {};
}(function() {
    /**
     * YUI Library aliases
     */
    var Dom = YAHOO.util.Dom,
        Event = YAHOO.util.Event;

    /**
     * Alfresco Slingshot aliases
     */
    var $html = Alfresco.util.encodeHTML,
        $combine = Alfresco.util.combinePaths,
        $encodeHTML = Alfresco.util.encodeHTML;

    /**
     * Overview delete requests constructor.
     * 
     * @param {String} htmlId The HTML id of the parent element
     * @return {ADACTA.OverviewDeleteRequests} The new ManagePersonnelFiles instance
     * @constructor
     */
    ADACTA.OverviewDeleteRequests = function(htmlId) {
        ADACTA.OverviewDeleteRequests.superclass.constructor.call(this, "ADACTA.OverviewDeleteRequests", htmlId, ["button", "container", "datasource", "datatable", "paginator"]);
        this.searchText = "";
        return this;
    }

    YAHOO.extend(ADACTA.OverviewDeleteRequests, Alfresco.component.Base, {

        options: {
            /**
             * The component id.
             *
             * @property componentId
             * @type string
             */
            componentId: "",

            /**
             * Maximum number of items to display in the results list
             *
             * @property maxSearchResults
             * @type int
             * @default 10
             */
            maxSearchResults: 250,

            /**
             * The search type.
             *
             * @property componentId
             * @type string
             */
            searchType: "overviewDeleteRequests",

            /**
             * Is user functional administrator. Only then user can use dashlet.
             *
             * @property componentId
             * @type string
             */
            isAdactaAdmin: false

        },

        searchText: null,
        pageSize: 250,
        skipCount: 0,

        /**
         * Fired by YUI when parent element is available for scripting.
         * Component initialisation, including instantiation of YUI widgets and event listener binding.
         *
         * @method onReady
         */
        onReady: function _onReady() {
            // Reference to self used by inline functions
            var me = this;

            if (me.options.isAdactaAdmin) {

                // Buttons and menus            
                this.widgets.empty = Alfresco.util.createYUIButton(this, "empty-button", this.onClear);
                this.widgets.pageLess = Alfresco.util.createYUIButton(this, "paginator-less-button", this.onPageLess);
                this.widgets.pageMore = Alfresco.util.createYUIButton(this, "paginator-more-button", this.onPageMore);

                // Configure datatable
                var url = Alfresco.constants.PROXY_URI + "nl/defensie/adacta/search/filefolder";
                this.widgets.dataTable = new Alfresco.util.DataTable({
                    dataTable: {
                        config: {
                            generateRequest: function(oState, oSelf) {

                                var startIndex = (oState.pagination.page - 1) * oState.pagination.rowsPerPage;
                                var sort = encodeURIComponent((oState.sortedBy) ? oState.sortedBy.key : oSelf.getColumnSet().keys[0].getKey());
                                var dir = (oState.sortedBy && oState.sortedBy.dir === YAHOO.widget.DataTable.CLASS_DESC) ? "desc" : "asc";

                                //Save current sort for paging
                                me.widgets.dataTable.currentSortKey = sort;
                                me.widgets.dataTable.currentDir = dir;

                                // Build the request
                                return "?sortBy=" + sort + "&dir=" + dir + "&startIndex=" + startIndex + "&pageSize=" + oState.pagination.rowsPerPage + "&searchType=" + me.options.searchType;
                            }
                        },
                        container: this.id + "-datalist",
                        columnDefinitions: [{
                            key: "indicator",
                            label: this.msg("label.checkbox"),
                            sortable: false,
                            formatter: this.bind(this.renderCellIndicator),
                            width: 16
                        }, {
                            key: "thumbnail",
                            label: this.msg("label.checkbox"),
                            sortable: false,
                            formatter: this.bind(this.renderCellIcon),
                            width: 16
                        }, {
                            key: "modifier",
                            label: this.msg("label.name"),
                            sortable: true,
                            formatter: this.bind(this.renderCellSafeHTML),
                            minWidth: 150
                        }, {
                            key: "docCategory",
                            label: this.msg("label.docCategory"),
                            sortable: true,
                            formatter: this.bind(this.renderCellCategory),
                            minWidth: 250
                        }, {
                            key: "docSubject",
                            label: this.msg("label.docSubject"),
                            sortable: true,
                            formatter: this.bind(this.renderCellSubject),
                            minWidth: 150
                        }, {
                            key: "docWorkDossier",
                            label: this.msg("label.docWorkDossier"),
                            sortable: true,
                            formatter: this.bind(this.renderCellSafeHTML),
                            minWidth: 100
                        }, {
                            key: "modified",
                            label: this.msg("label.modified"),
                            sortable: true,
                            formatter: this.bind(this.renderCellModified),
                            minWidth: 250
                        }, {
                            key: "docDate",
                            label: this.msg("label.docDate"),
                            sortable: true,
                            formatter: this.bind(this.renderCellDocDate),
                            minWidth: 150
                        }, {
                            key: "actions",
                            label: this.msg("label.actions"),
                            sortable: false,
                            formatter: this.bind(this.renderCellActions),
                            width: 40
                        }]
                    },
                    dataSource: {
                        url: url,
                        initialParameters: "&filter=" + encodeURIComponent(this.searchText) + "&startIndex=" + this.skipCount + "&pageSize=" + (this.pageSize + 1) + "&searchType=" + this.options.searchType,
                        config: {
                            responseSchema: {
                                resultsList: "data.adactaNodes"
                            },
                            doBeforeParseData: function _doBeforeParseData(oRequest, oResponse) {
                                // process the paging meta data to correctly set paginator button enabled state
                                me.widgets.pageLess.set("disabled", ((me.skipCount = oResponse.paging.skipCount) === 0));
                                if (oResponse.paging.totalItems > (me.pageSize + oResponse.paging.skipCount)) {
                                    // remove the last item as it's only for us to evaluate the "more" button state
                                    oResponse.data.adactaNodes.pop();
                                    me.widgets.pageMore.set("disabled", false);
                                } else {
                                    me.widgets.pageMore.set("disabled", true);
                                }
                                return oResponse;
                            }
                        }
                    },
                    paginator: {
                        history: false,
                        hide: true,
                        config: {
                            containers: [parent.id + "-paginator"],
                            rowsPerPage: this.options.maxSearchResults
                        }
                    }
                });

                // Remove some restrictions on datatables imposed by Alfresco css
                YAHOO.util.Dom.replaceClass(this.id + '-datalist', 'alfresco-datatable', 'odr-datatable');

                /**
                 * Hook action events
                 */
                var registerEventHandler = function(cssClass, fnHandler) {
                        var fnEventHandler = function MS_oR_fnEventHandler(layer, args) {
                                var owner = YAHOO.Bubbling.getOwnerByTagName(args[1].anchor, "div");
                                if (owner !== null) {
                                    fnHandler.call(me, args[1].target.offsetParent, owner);
                                }
                                return true;
                            };
                        YAHOO.Bubbling.addDefaultAction(cssClass, fnEventHandler);
                    };
                registerEventHandler("onApproveDeleteRequest", this.onApproveDeleteRequest);
                registerEventHandler("onUnmarkDocumentForDelete", this.onUnmarkDocumentForDelete);
            }
        },

        /**
         * DataTable Cell Renderers
         */

        renderCellCategory: function renderCellModified(elCell, oRecord, oColumn, oData) {
            elCell.innerHTML = $html(this.msg(oRecord.getData("docCategory")));
        },

        renderCellSubject: function renderCellModified(elCell, oRecord, oColumn, oData) {
            elCell.innerHTML = $html(this.msg(oRecord.getData("docSubject")));
        },

        renderCellModified: function renderCellModified(elCell, oRecord, oColumn, oData) {
            var meta = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oRecord.getData("modified"), false)));
            elCell.innerHTML = meta;
        },

        renderCellDocDate: function _renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
            if (oRecord.getData("docDate") != "") {
                var meta = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oRecord.getData("docDate")), "yyyy-mm-dd"));
                elCell.innerHTML = meta;
            }
        },

        /**
         * File/Folder icon custom datacell formatter
         *
         * @method MPF_renderCellIcon
         */
        renderCellIcon: function MPF_renderCellIcon(elCell, oRecord, oColumn, oData) {
            Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");
            var uri = oRecord.getData("adactaDetailUrl");
            elCell.innerHTML = '<span class="icon32"><a href="' + uri + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/images/filetypes/' + Alfresco.util.getFileIcon(oRecord.getData("name"), "cm:content", 16) + '" alt="' + $html(name) + '" /></a></span>';
        },

        /**
         * 
         */
        renderCellIndicator: function _renderCellIndicator(elCell, oRecord, oColumn, oData) {
            Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");

            var color = "orange-16.png";
            if (oRecord.getData("docWorkDossier").indexOf("C") > -1) {
                color = "red-16.png";
            }

            elCell.innerHTML = '<span class="icon32"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/indicators/' + color + '" /></span>';
        },

        /**
         * Description metadata custom datacell formatter
         * 
         * @method MPF_renderCellDescription
         */
        renderCellName: function _renderCellName(elCell, oRecord, oColumn, oData) {
            var uri = oRecord.getData("adactaDetailUrl");
            elCell.innerHTML = '<a href="' + uri + '">' + oRecord.getData("name") + '</a>';
        },

        renderCellSafeHTML: function _renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
            elCell.innerHTML = $html(oData);
        },

        /**
         * Actions custom datacell formatter
         *
         * @method MPF_renderCellActions
         */
        renderCellActions: function MPF_renderCellActions(elCell, oRecord, oColumn, oData) {
            Dom.setStyle(elCell.parentNode, "vertical-align", "middle");
            Dom.setStyle(elCell.parentNode, "text-align", "right");

            var desc = "";
            desc += '<a class="onApproveDeleteRequest" title="' + this.msg("actions.adactaApproveDeleteRequest.label") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/approve-16.png" title="' + this.msg("actions.adactaApproveDeleteRequest.label") + '"/></a>&nbsp;';
            desc += '<a class="onUnmarkDocumentForDelete" title="' + this.msg("actions.adactaUnMarkDocumentToDelete.label") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/cancel-16.png" title="' + this.msg("actions.adactaUnMarkDocumentToDelete.label") + '"/></a>&nbsp;';
            elCell.innerHTML = desc;
        },

        onApproveDeleteRequest: function(row) {
            var data = this.widgets.dataTable.getRecord(row).getData();
            Alfresco.util.PopupManager.displayForm({
                title: this.msg("actions.adactaApproveDeleteRequest.label"),
                properties: {
                    mode: "create",
                    itemKind: "action",
                    itemId: "adactaApproveDeleteRequest",
                    destination: data.nodeRef
                },
                success: {
                    fn: function(response) {
                        Alfresco.util.PopupManager.displayMessage({
                            text: this.msg("actions.adactaApproveDeleteRequest.success"),
                            displayTime: 4
                        });

                        this.refreshDataTable();
                    },
                    scope: this
                },
                failureMessage: this.msg("actions.adactaApproveDeleteRequest.failure")
            });
        },

        onUnmarkDocumentForDelete: function(row) {
            var data = this.widgets.dataTable.getRecord(row).getData();
            Alfresco.util.PopupManager.displayForm({
                title: this.msg("actions.adactaUnMarkDocumentToDelete.label"),
                properties: {
                    mode: "create",
                    itemKind: "action",
                    itemId: "adactaUnMarkDocumentToDelete",
                    destination: data.nodeRef
                },
                success: {
                    fn: function(response) {
                        Alfresco.util.PopupManager.displayMessage({
                            text: this.msg("actions.adactaUnMarkDocumentToDelete.success"),
                            displayTime: 4
                        });

                        this.refreshDataTable();
                    },
                    scope: this
                },
                failureMessage: this.msg("actions.adactaUnMarkDocumentToDelete.failure")
            });
        },

        /**
         * onClear button click handler
         * 
         * @method onClear
         * @param e {object} DomEvent
         * @param p_obj {object} Object passed back from addListener method
         */
        onClear: function MPF_onClear(e, p_obj) {
            this.refreshDataTable();
        },

        /**
         * onPageLess button click handler
         * 
         * @method onPageLess
         * @param e {object} DomEvent
         * @param p_obj {object} Object passed back from addListener method
         */
        onPageLess: function MPF_onPageLess(e, p_obj) {
            if (this.skipCount > 0) {
                this.skipCount -= this.pageSize;
            }
            this.refreshDataTable();
        },

        /**
         * onPageMore button click handler
         * 
         * @method onPageMore
         * @param e {object} DomEvent
         * @param p_obj {object} Object passed back from addListener method
         */
        onPageMore: function MPF_onPageMore(e, p_obj) {
            this.skipCount += this.pageSize;
            this.refreshDataTable();
        },

        _onActionSuccess: function MC__onActionSuccess(response, obj) {
            Alfresco.util.PopupManager.displayMessage({
                text: this.msg("message.action.success", obj.name)
            });

            this.refreshDataTable();
        },

        /**
         * Refresh the list after an action has occured
         * 
         * @method refreshDataTable
         */
        refreshDataTable: function MPF_refreshDataTable() {
            // we alway ask for an extra item to see if there are more for the next page
            var params = "&pageSize=" + (this.pageSize + 1) + "&startIndex=" + this.skipCount + "&searchType=" + this.options.searchType;
            if (this.searchText.length !== 0) {
                var search = this.searchText;
                if (search.match("\\*") != "*") {
                    search += "*";
                }
                params += "&nf=" + encodeURIComponent(search);
            }
            this.widgets.dataTable.loadDataTable(params);
        }
    });
})();