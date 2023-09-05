<@markup id="css" >
	<#-- CSS Dependencies -->
	<@link href="${url.context}/res/components/adacta/datatable.css" group="import"/>
</@>

<@markup id="js">
	<#-- JavaScript Dependencies -->
	<@script src="${url.context}/res/components/console/consoletool.js" group="console"/>
	<@script src="${url.context}/res/components/adacta/search-results.js" group="console"/>
</@>

<@markup id="widgets">
	<@createWidgets group="console"/>
</@>

<@markup id="html">
	<@uniqueIdDiv>
		<!--[if IE]>
		<iframe id="yui-history-iframe" src="${url.context}/res/yui/history/assets/blank.html"></iframe>
		<![endif]-->
	    <input id="yui-history-field" type="hidden" />
    
		<#assign el=args.htmlid?html>
      	<div id="${el}-body" class="adacta-datatable">
      	
      		<!-- Search panel -->
      		<div id="${el}-search" class="hidden">
	      		<div id="${el}-search-main" class="search-main separator">
	      		
	      			<div id="${el}-document-components">
		      			<div class="header-bar">${msg("label.about")}</div>
	         			<div class="field-row">
	         				<span class="field-label-right-long">${msg("label.dossier-info-name")}:</span>
	         				<span id="${el}-search-name" class="field-value-long"></span>
	         			</div>
	         			<div class="field-row">
	         				<span class="field-label-right-long">${msg("label.employeeNumber")}:</span>
	         				<span id="${el}-search-number" class="field-value-long"></span>
	         			</div>
	         			
	         			<div id="${el}-search-header-bar" class="header-bar">${msg("label.childDocuments")}</div>
		      		
		      			<div class="unread-button" style="margin:1em">
		 					<span class="yui-button yui-push-button" id="${el}-all-unread-button-search">
		 						<span class="first-child">
		 							<button title="${msg("button.setunreadtitle")}">${msg("button.setunreadvalue")}</button>
		 						</span>
		 					</span>
		 				</div>
	     			</div>
	     			
	      			<div class="search-main" style="margin:1em">
	      				<div id="${el}-search-bar" class="search-bar theme-bg-color-3">${msg("message.noresults")}</div>
	      				<div class="results" id="${el}-datatable"></div>
	      			</div>
	      			<div id="${el}-paginator" class="paginator">&nbsp;</div>
	      		</div>
      		</div>
      
         	<!-- View Child Documents panel -->
         	<div id="${el}-view" class="hidden">
         		<div class="yui-g separator">
         			<div class="yui-u first">
         				<div class="goback-button">
         					<span class="yui-button yui-push-button" id="${el}-goback-button-top">
         						<span class="first-child"><button>${msg("button.searchback")}</button></span>
         					</span>
         				</div>
         			</div>
         		</div>
         		
         		<div id="${el}-view-main" class="view-main separator">
         			<div class="header-bar">${msg("label.about")}</div>
         			<div class="field-row">
         				<span class="field-label-right-long">${msg("label.dossier-info-name")}:</span>
         				<span id="${el}-name" class="field-value-long"></span>
         			</div>
         			<div class="field-row">
         				<span class="field-label-right-long">${msg("label.employeeNumber")}:</span>
         				<span id="${el}-number" class="field-value-long"></span>
         			</div>
         			
         			<div id="${el}-header-bar" class="header-bar">${msg("label.childDocuments")}</div>
         			
     				<div class="unread-button" style="margin:1em">
						<span class="yui-button yui-push-button deselect-all" id="${el}-deselect-all-button">
     						<span class="first-child">
     							<button title="${msg("button.deselectalltitle")}">${msg("button.deselectallvalue")}</button>
     						</span>
     					</span>
     					<span class="yui-button yui-push-button" id="${el}-all-unread-button">
     						<span class="first-child">
     							<button title="${msg("button.setunreadtitle")}">${msg("button.setunreadvalue")}</button>
     						</span>
     					</span>
						<span class="yui-button yui-push-button open-in-tabs" id="${el}-open-in-tabs-button">
     						<span class="first-child">
     							<button title="${msg("button.open-in-tabs.title")}">${msg("button.open-in-tabs.label")}</button>
     						</span>
     					</span>
     				</div>

         			<div class="search-main" style="margin:1em">
         				<div id="${el}-search-bar-children" class="search-bar theme-bg-color-3">${msg("message.noresults")}</div>
         				<div class="results" id="${el}-view-node-children"></div>
         			</div>
         			<div id="${el}-paginator-children" class="paginator">&nbsp;</div>
         		</div>
         	</div>
         </div>
	</@>
</@>

