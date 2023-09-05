<@markup id="css" >
   <#-- CSS Dependencies -->
   <#include "../../../../org/alfresco/components/form/form.css.ftl"/>
   <@link href="${url.context}/res/components/adacta/datatable.css" group="import"/>
</@>

<@markup id="js">
   <#-- JavaScript Dependencies -->
   <#include "../../../../org/alfresco/components/form/form.js.ftl"/>
   <@script src="${url.context}/res/components/form/date-range.js" group="import"/>
   <@script src="${url.context}/res/components/form/number-range.js" group="import"/>

   <@script src="${url.context}/res/components/console/consoletool.js" group="import"/>
   <@script src="${url.context}/res/components/adacta/import.js" group="import"/>
</@>

<@markup id="widgets">
   <@createWidgets group="import"/>
</@>




<@markup id="html">
   <@uniqueIdDiv>
      <!--[if IE]>
      <iframe id="yui-history-iframe" src="${url.context}/res/yui/history/assets/blank.html"></iframe> 
      <![endif]-->
      <input id="yui-history-field" type="hidden" />
      <#if isAdactaUser>
      <#assign el=args.htmlid?html>
      <div id="${el}-body" class="adacta-datatable">
      
         <!-- Search panel -->
         <div id="${el}-search" class="hidden">
            <div class="yui-g separator">
               <div class="yui-u first">
                               
                  <div class="header-menu">
                  
                  <div class="import-button">                    
                    <span class="yui-button yui-push-button" id="${el}-import-button">
                       <span class="first-child"><button>${msg("button.importfiles")}</button></span>
                    </span>
                  </div>

	             <div class="select-button-import">
	               <button id="${el}-select-button">${msg("label.select")}</button>
	               <div id="${el}-selectItems-menu" class="yuimenu">
	                  <div class="bd">
	                     <ul>
	                        <li><a class="select-all" href="#">${msg("label.selectAll")}</a></li>
	                        <li><a class="select-invert" href="#">${msg("label.selectInvert")}</a></li>
	                        <li><a class="select-none" href="#">${msg("label.selectNone")}</a></li>
	                     </ul>
	                  </div>
	               </div>
	             </div>
                  
                  <div class="selected-items-button-import">
               		<button id="${el}-selected">${msg("label.selectedItems")}</button>
               		<div id="${el}-selectedItems-menu" class="yuimenu">
                  		<div class="bd">
                     		<ul>
                        		<li><a class="index-item" href="#">${msg("button.index")}</a></li>
                        		<li><a class="delete-item" href="#">${msg("button.delete")}</a></li>
		                    </ul>
                  		</div>
               		</div>
            	 </div>
	             
	             </div>
                  
               </div>
            </div>
            
            <div class="search-main">
               <div id="${el}-search-bar" class="search-bar theme-bg-color-3">${msg("message.noresults")}</div>
               <div class="results" id="${el}-datatable"></div>
            </div>
            <div id="${el}-paginator" class="paginator">&nbsp;</div>         
         </div>            
         
      </div>
      </#if>
   </@>
</@>