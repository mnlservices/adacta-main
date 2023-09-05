<@markup id="css" >
   <#-- CSS Dependencies -->
   <@link href="${url.context}/res/components/console/adacta-setup.css" group="adacta-setup"/>
</@>

<@markup id="js">
   <#-- JavaScript Dependencies -->
   <@script src="${url.context}/res/components/console/consoletool.js" group="adacta-setup"/>
   <@script src="${url.context}/res/components/console/adacta-setup.js" group="adacta-setup"/>
</@>

<@markup id="widgets">
   <@createWidgets group="adacta-setup"/>
</@>

<@markup id="html">
   <@uniqueIdDiv>
      <#assign el=args.htmlid?html>
      <div id="${el}-body" class="adacta-setup">
      	 <div class="title">${msg("page.adactaConsole.description")}</div>
         <div class="row info">${msg("label.welcome")}</div>
		 <div class="title">${msg("label.options")}</div>
		 
 	     <#if !foundsite>
	     <div class="row">${msg("label.create-site")}</div>	      
	     </#if>

	     <#if user.isAdmin>	     
	     <#if serverMode == "TEST">	
	     <div id="${el}-initial" class="label" <#if !foundsite>style="display:none"</#if>><a id="${el}-initial-link" href="#">${msg("label.load-initial")}</a></div>
	     <div class="description" <#if !foundsite>style="display:none"</#if>>${msg("label.load-initial.description")}<a id="${el}-loadtest-link" href="#">${msg("label.load-test.description")}</a></div>
	     </#if>	
	    
   	     <div id="${el}-configuration" class="label" <#if !foundsite>style="display:none"</#if>><a id="${el}-configuration-link" href="#">${msg("label.load-configuration")}</a></div>
	     <div class="description" <#if !foundsite>style="display:none"</#if>>${msg("label.load-configuration.description")}</div>
	     
	     <div id="${el}-migration" class="label" <#if !foundsite>style="display:none"</#if>><a id="${el}-migration-link" href="#">${msg("label.load-migration")}</a></div>
	     <div class="description" <#if !foundsite>style="display:none"</#if>>${msg("label.load-migration.description")}</div>
	    
	     <div id="${el}-authorisation" class="label" <#if !foundsite>style="display:none"</#if>><a id="${el}-authorisation-link" href="#">${msg("label.load-authorisation")}</a></div>
	     <div class="description" <#if !foundsite>style="display:none"</#if>>${msg("label.load-authorisation.description")}</div>	    
	     
	     <#if serverMode == "TEST">	
	     <div id="${el}-import" class="label" <#if !foundsite>style="display:none"</#if>><a id="${el}-import-link" href="#">${msg("label.load-import")}</a></div>
	     <div class="description" <#if !foundsite>style="display:none"</#if>>${msg("label.load-import.description")}</div>
	     
	     <div id="${el}-scanbatch" class="label" <#if !foundsite>style="display:none"</#if>><a id="${el}-scanbatch-link" href="#">${msg("label.load-scanbatch")}</a></div>
	     <div class="description" <#if !foundsite>style="display:none"</#if>>${msg("label.load-scanbatch.description")}</div>
	     
	     <div id="${el}-delete" class="label" <#if !foundsite>style="display:none"</#if>><a id="${el}-delete-link" href="#">${msg("label.load-delete")}</a></div>
	     <div class="description" <#if !foundsite>style="display:none"</#if>>${msg("label.load-delete.description")}</div>
	     </#if>     		     			      			        
         </#if>	 
      </div>
   </@>
</@>