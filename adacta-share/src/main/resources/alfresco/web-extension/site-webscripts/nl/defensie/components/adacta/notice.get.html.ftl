<@markup id="css" >
   <#-- CSS Dependencies -->
   <#include "../../../../org/alfresco/components/form/form.css.ftl"/>
   <@link href="${url.context}/res/components/adacta/notice.css" group="notice"/>
</@>

<@markup id="js">
   <#-- JavaScript Dependencies -->
   <#include "../../../../org/alfresco/components/form/form.js.ftl"/>
   <@script src="${url.context}/res/components/console/consoletool.js" group="notice"/>
</@>

<@markup id="widgets">
   <@createWidgets group="notice"/>
</@>

<@markup id="html">
   <@uniqueIdDiv>
      <!--[if IE]>
      <iframe id="yui-history-iframe" src="${url.context}/res/yui/history/assets/blank.html"></iframe> 
      <![endif]-->
      <input id="yui-history-field" type="hidden" />
      <#assign el=args.htmlid?html>
      <div id="${el}-body" class="adacta-notice" style="padding: 10px;">
      
	  <#if text?? && text != "">   
        ${text}    
      </#if>
      
      </div>
   </@>
</@>