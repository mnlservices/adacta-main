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
     * Overview index reports constructor.
     * 
     * @param {String} htmlId The HTML id of the parent element
     * @return {ADACTA.OverviewIndexReports} The new ManagePersonnelFiles instance
     * @constructor
     */
    ADACTA.OverviewIndexReports = function(htmlId) {
        ADACTA.OverviewIndexReports.superclass.constructor.call(this, "ADACTA.OverviewIndexReports", htmlId, ["button", "container", "datasource", "datatable", "paginator"]);
        this.searchText = "";
        return this;
    }

    YAHOO.extend(ADACTA.OverviewIndexReports, Alfresco.component.Base, {

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
            searchType: "",

            /**
             * Is user functional administrator or importer. Only then user can use dashlet.
             *
             * @property componentId
             * @type string
             */
            isAdactaAdminOrImporter: false

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

            if (me.options.isAdactaAdminOrImporter) {

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
                                var sort = encodeURIComponent((oState.sortedBy) ? oState.sortedBy.key : oSelf.getColumnSet().keys[3].getKey());
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
                            key: "thumbnail",
                            label: this.msg("label.checkbox"),
                            sortable: false,
                            formatter: this.bind(this.renderCellIcon),
                            width: 16
                        }, {
                            key: "description",
                            label: this.msg("label.description"),
                            sortable: true,
                            formatter: this.bind(this.renderCellDescription),
                            minWidth: 400
                        }, {
                            key: "creator",
                            label: this.msg("label.creator"),
                            sortable: true,
                            formatter: this.bind(this.renderCellCreatorFullName),
                            minWidth: 250
                        }, {
                            key: "created",
                            label: this.msg("label.created"),
                            sortable: true,
                            formatter: this.bind(this.renderCellCreated),
                            minWidth: 250
                        }, {
                            key: "modified",
                            label: this.msg("label.modified"),
                            sortable: true,
                            formatter: this.bind(this.renderCellModified),
                            minWidth: 250
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
                YAHOO.util.Dom.replaceClass(this.id + '-datalist', 'alfresco-datatable', 'ada-datatable');

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
                registerEventHandler("onEditReportNode", this._onEditNode);
                registerEventHandler("onDeleteReportNode", this._onDeleteNode);
            }
        },

        /**
         * DataTable Cell Renderers
         */

        renderCellDescription: function _renderCellName(elCell, oRecord, oColumn, oData) {
            var uri = Alfresco.constants.PROXY_URI + "slingshot/node/content/" + oRecord.getData("nodeRef").replace(":/","");             
            elCell.innerHTML = '<a href="' + uri + '" target="_blank">' + oRecord.getData("description") + '</a>';
        },
                
        renderCellCreatorFullName: function renderCellModified(elCell, oRecord, oColumn, oData) {
            elCell.innerHTML = $html(oRecord.getData("creatorFullName"));
        },

        renderCellSafeHTML: function renderCellModified(elCell, oRecord, oColumn, oData) {
            elCell.innerHTML = $html(oData);
        },

        renderCellCreated: function renderCellModified(elCell, oRecord, oColumn, oData) {
            var meta = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oRecord.getData("created"), false)));
            elCell.innerHTML = meta+"       ";
        },

        renderCellModified: function renderCellModified(elCell, oRecord, oColumn, oData) {
            var meta = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oRecord.getData("modified"), false)));
            elCell.innerHTML = meta;
        },

        renderCellDocDate: function _renderCellSafeHTML(elCell, oRecord, oColumn, oData) {
            if (oRecord.getData("docDate") != "") {
                var meta = $html(Alfresco.util.formatDate(Alfresco.util.fromISO8601(oRecord.getData("docDate"))));
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
            desc += '<a class="onEditReportNode" title="' + this.msg("actions.document.edit-metadata") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-edit-metadata-16.png" title="' + this.msg("actions.document.edit-metadata") + '"/></a>&nbsp;';
            desc += '<a class="onDeleteReportNode" title="' + this.msg("actions.document.delete") + '"><img src="' + Alfresco.constants.URL_RESCONTEXT + 'components/documentlibrary/actions/document-delete-16.png" title="' + this.msg("actions.document.delete") + '"/></a>&nbsp;';

            elCell.innerHTML = desc;
        },

        /**
         * Handler for the edit form
         *
         * @method _onEditNode
         * @param row {object} DataTable row representing item to be actioned
         */
        _onEditNode: function(row) {
            var data = this.widgets.dataTable.getRecord(row).getData();

            Alfresco.util.PopupManager.displayForm({
                title: this.msg("actions.document.edit-metadata"),
                properties: {
                    mode: "edit",
                    itemKind: "node",
                    itemId: data.nodeRef
                },
                success: {
                    fn: function(response) {
                        Alfresco.util.PopupManager.displayMessage({
                            text: this.msg("message.details.success")
                        });

                        this.refreshDataTable();
                    },
                    scope: parent
                },
                failureMessage: this.msg("message.details.failure")
            });
        },

        /**
         * Handler for the delete form
         *
         * @method _onDeleteNode
         * @param row {object} DataTable row representing item to be actioned
         */
        _onDeleteNode: function(row) {
            var data = this.widgets.dataTable.getRecord(row).getData();

            Alfresco.util.PopupManager.displayForm({
                title: this.msg("actions.adactaDeleteNode.label"),
                properties: {
                    mode: "create",
                    itemKind: "action",
                    itemId: "adactaDeleteNode",
                    destination: data.nodeRef
                },
                success: {
                    fn: function(response) {
                        Alfresco.util.PopupManager.displayMessage({
                            text: this.msg("message.delete.success", data.name),
                            displayTime: 4
                        });

                        this.refreshDataTable();
                    },
                    scope: parent
                },
                failureMessage: this.msg("message.delete.failure", data.name)
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