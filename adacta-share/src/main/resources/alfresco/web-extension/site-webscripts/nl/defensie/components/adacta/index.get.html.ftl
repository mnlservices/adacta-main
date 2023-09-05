<@markup id="css" >
   <#-- CSS Dependencies -->
   <#include "../../../../org/alfresco/components/form/form.css.ftl"/>
   <@link href="${url.context}/res/components/adacta/datatable.css" group="index"/>
</@>

<@markup id="js">
   <#-- JavaScript Dependencies -->
   <#include "../../../../org/alfresco/components/form/form.js.ftl"/>
   <@script src="${url.context}/res/components/form/date-range.js" group="index"/>
   <@script src="${url.context}/res/components/form/number-range.js" group="index"/>

   <@script src="${url.context}/res/components/console/consoletool.js" group="index"/>
   <@script src="${url.context}/res/components/adacta/index.js" group="index"/>
</@>

<@markup id="widgets">
   <@createWidgets group="index"/>
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
                  <div class="search-text"><input type="text" id="${el}-search-text" name="-" value="" maxlength="256"/>
                     <div class="search-button">
                        <span class="yui-button yui-push-button" id="${el}-search-button">
                           <span class="first-child"><button>${msg("button.search")}</button></span>
                        </span>
                     </div>
                  </div>
                  
                 <div class="select-button">
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
                  
                  <div class="selected-items-button">
               		<button id="${el}-selected">${msg("label.selectedItems")}</button>
               		<div id="${el}-selectedItems-menu" class="yuimenu">
                  		<div class="bd">
                     		<ul>
                        		<li><a class="release-item" href="#">${msg("button.release")}</a></li>
                        		<li><a class="delete-item" href="#">${msg("button.delete")}</a></li>
		                    </ul>
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
         
         <!-- View Folder panel -->
         <div id="${el}-viewFolder" class="hidden">
            <div class="yui-g separator">
               <div class="yui-u first">
	               <div class="goback-button">
	                  <span class="yui-button yui-push-button" id="${el}-goback-button">
	                     <span class="first-child"><button>${msg("button.goback")}</button></span>
	                  </span>
	               </div>
               </div>
            </div>
            
            <div id="${el}-view-main" class="view-main separator">
               <!-- Each info section separated by a header-bar div -->
               <div class="header-bar">${msg("label.about")}</div>
               <div class="field-row">
                  <span class="field-label-right">${msg("label.name")}:</span>
                  <span id="${el}-name" class="field-value"></span>
               </div>
               <div class="field-row">
                  <span class="field-label-right">${msg("label.created")}:</span>
                  <span id="${el}-created" class="field-value"></span>
               </div>
               <div class="field-row">
                  <span class="field-label-right">${msg("label.scanBy")}:</span>
                  <span id="${el}-scan-by" class="field-value"></span>
               </div>
               
               <div class="header-bar">${msg("label.childDocuments")}</div>
                 
                  <div class="header-menu">

	              <div class="select-button-child">
	               <button id="${el}-select-button-child">${msg("label.select")}</button>
	               <div id="${el}-selectItems-menu-child" class="yuimenu">
	                  <div class="bd">
	                     <ul>
	                        <li><a class="select-all" href="#">${msg("label.selectAll")}</a></li>
	                        <li><a class="select-invert" href="#">${msg("label.selectInvert")}</a></li>
	                        <li><a class="select-none" href="#">${msg("label.selectNone")}</a></li>
	                     </ul>
	                  </div>
	               </div>
	              </div>
                  
                  <div class="selected-items-button-child">
               		<button id="${el}-selected-child">${msg("label.selectedItems")}</button>
               		<div id="${el}-selectedItems-menu-child" class="yuimenu">
                  		<div class="bd">
                     		<ul>
	                     		<li><a class="index-item" href="#">${msg("button.index")}</a></li>
                        		<li><a class="delete-item" href="#">${msg("button.delete")}</a></li>
		                    </ul>
                  		</div>
               		</div>
            	 </div>
            
	             </div>  
	            
	             <div class="search-main" style="margin:1em">
	               <div id="${el}-search-bar-children" class="search-bar theme-bg-color-3">${msg("message.noresults")}</div>
	               <div class="results" id="${el}-datatable-children"></div>
	             </div>
	             <div id="${el}-paginator-children" class="paginator">&nbsp;</div>
               
            </div>
      
            <div class="yui-g">

            </div>
         </div>                     
         
      </div>
      </#if>
   </@>
</@>