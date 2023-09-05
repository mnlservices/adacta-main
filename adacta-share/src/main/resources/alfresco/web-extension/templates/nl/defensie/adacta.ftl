<#include "/org/alfresco/include/alfresco-template.ftl" />
<@templateHeader />

<@templateBody>
   <@markup id="alf-hd">
   <div id="alf-hd">
      <@region scope="global" id="share-header" chromeless="true"/>
   </div>
   </@>
   <@markup id="bd">
   <div id="bd">
      <@region id="toolbar" scope="template" />
      <@region id="main-content" scope="template" />
   </div>
   
   <@region id="archive-and-download" scope="template"/>
   
   <@region id="html-upload" scope="template" />
   <@region id="flash-upload" scope="template" />
   <@region id="file-upload" scope="template" />
   <@region id="dnd-upload" scope="template"/> 
   
   </@>
</@>

<@templateFooter>
   <@markup id="alf-ft">
   <div id="alf-ft">
      <@region id="footer" scope="global" />
   </div>
   </@>
</@>