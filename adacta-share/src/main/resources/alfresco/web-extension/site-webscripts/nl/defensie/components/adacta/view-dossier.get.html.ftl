<@markup id="css" >
	<#-- CSS Dependencies -->
	<#include "../../../../org/alfresco/components/form/form.css.ftl"/>
	<@link href="${url.context}/res/components/adacta/datatable.css" group="search"/>
</@>

<@markup id="js">
	<#-- JavaScript Dependencies -->
	<#include "../../../../org/alfresco/components/form/form.js.ftl"/>
	<@script src="${url.context}/res/components/console/consoletool.js" group="search"/>
	<@script src="${url.context}/res/components/adacta/view-dossier.js" group="search"/>
</@>

<@markup id="widgets">
	<@createWidgets group="search"/>
</@>

<@markup id="html">
	<@uniqueIdDiv>
		<!--[if IE]>
		<iframe id="yui-history-iframe" src="${url.context}/res/yui/history/assets/blank.html"></iframe>
		<![endif]-->
	    <input id="yui-history-field" type="hidden" />
    
		<#assign el=args.htmlid?html>
      	<div id="${el}-body" class="adacta-datatable">
      
         	<!-- View Child Documents panel -->
         	<div id="${el}-view" class="hidden">         		
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
         			
         			<span class="yui-button yui-menu-button" style="margin-left:1em" id="${el}-filterCategory">
	                    <span class="first-child">
	                        <button type="button"></button>
	                    </span>
	                </span>
	                <select id="${el}-filterCategory-menu">
	                	<option value="Alle">Alle</option>
	                	 <#list filterCategory![] as filter>
	              			<option value="${filter.name}">${filter.desc}</option>
	           			 </#list>
	                </select>
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

