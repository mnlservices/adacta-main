<@markup id="css"></@>

<@markup id="js"></@>

<@markup id="widgets">
   <#if document??>
     <@createWidgets group="document-details"/>
     <@inlineScript group="document-details">
        YAHOO.util.Event.onContentReady("${args.htmlid?js_string}-heading", function() {
           Alfresco.util.createTwister("${args.htmlid?js_string}-heading", "SelectedItems");
        });
     </@>
   </#if>
</@>

<@markup id="html">
   <@uniqueIdDiv>
      <#if document?? && number?? && totalItems??>
         <#assign el=args.htmlid?html>
         <div id="${el}-body" class="document-links document-details-panel">
            <h2 id="${el}-heading" class="thin dark">${msg("header")}</h2>
            <div class="panel-body">
               <div class="info">${msg("message.progress.selectedItems", number?c, totalItems?c)}</div>
            </div>   
         </div>
      </#if>
   </@>
</@>