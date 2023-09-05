<@standalone>
<@markup id="css" />

<@markup id="js">
   <#-- JavaScript Dependencies -->
   <#include "/org/alfresco/components/form/form.js.ftl"/>
   <@script src="${url.context}/res/components/document-details/admin-form.js" group="document-details"/>
</@>

<@markup id="widgets">
	<@createWidgets group="document-details"/>
  	<@inlineScript group="document-details">
       	YAHOO.util.Event.onContentReady("${args.htmlid?js_string}-heading", function() {
           	Alfresco.util.createTwister("${args.htmlid?js_string}-heading", "AdactaAdminForm");
        });
    </@>
</@>

<@markup id="html">
   	<@uniqueIdDiv>
		<#assign el=args.htmlid?html>
        <div id="${el}-body" class="document-links document-details-panel">
            <h2 id="${el}-heading" class="thin dark">
               ${msg("header")}
            </h2>
            <div id="${el}-formContainer"></div>
     	</div>
   	</@>
</@>
</@>