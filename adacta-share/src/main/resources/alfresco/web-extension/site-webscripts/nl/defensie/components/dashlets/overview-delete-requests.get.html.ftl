<#assign el=args.htmlid?html>
<@markup id="css" >
   <#-- CSS Dependencies -->
   <@link href="${url.context}/res/components/dashlets/overview-delete-requests.css" group="dashlets"/>
</@>

<@markup id="js">
   <#-- JavaScript Dependencies -->
	<#include "../../../../org/alfresco/components/form/form.js.ftl"/>
   
   <@script src="${url.context}/res/components/form/form-extension.js" group="dashlets"/>     
   <@script src="${url.context}/res/components/dashlets/overview-delete-requests.js" group="dashlets"/>
</@>

<@markup id="widgets">
   <@createWidgets group="dashlets"/>
</@>

<@markup id="html">
   <@uniqueIdDiv>
      <#assign el=args.htmlid?html>
       <div class="dashlet overview-delete-requests">
   	    <div class="title">${msg("dashlet.overview-delete-requests.shortName")}</div>
	    <div class="body">
         <div class="toolbar flat-button">
            <div>
                <span id="${el}-empty-button" class="align-left yui-button yui-push-button refresh-icon">
                    <span class="first-child">
                        <button id="${el}-empty-button" type="button">${msg("label.refreshButton")}</button>
                    </span>
                </span>
            </div>
            <div class="clear"></div>
         </div>
         <div class="content">
            <div id="${el}-datalist" class="datalist"></div>
            <div>
               <div id="${el}-paginator" class="paginator">
                  <span class="yui-button yui-push-button" id="${el}-paginator-less-button">
                     <span class="first-child"><button>${msg("pagination.previousPageLinkLabel")}</button></span>
                  </span>
                  &nbsp;
                  <span class="yui-button yui-push-button" id="${el}-paginator-more-button">
                     <span class="first-child"><button>${msg("pagination.nextPageLinkLabel")}</button></span>
                  </span>
               </div>
            </div>
         </div>
       </div>
     </div>   
   </@>
</@>