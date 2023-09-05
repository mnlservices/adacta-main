<#assign counter=0 /> 
<#macro dateFormat date>${date?string("dd MMM yyyy HH:mm:ss")}</#macro>
<#macro dateFormat2 date>${date?string("dd MMM yyyy")}</#macro>
<html>
   <head>
      <link rel="stylesheet" type="text/css" href="${shareUrl}/res/css/report.css" />
   </head>
   
   <body bgcolor="#dddddd">
      <table width="100%" cellpadding="20" cellspacing="0" border="0" bgcolor="#dddddd">
         <tr>
            <td width="100%" align="center">
               <table width="70%" cellpadding="0" cellspacing="0" bgcolor="white" style="background-color: white; border: 1px solid #aaaaaa;">
                  <tr>
                     <td width="100%">
                        <table width="100%" cellpadding="0" cellspacing="0" border="0">
                           <tr>
                              <td style="padding: 10px 30px 0px;">
                                 <table width="100%" cellpadding="0" cellspacing="0" border="0">
                                    <tr>
                                       <td>
                                          <table width="100%" cellpadding="0" cellspacing="0" border="0">
                                             <tr>
                                                <td>
                                                   <img src="${shareUrl}/res/themes/defensieTheme/images/app-logo-48.png" alt="" width="64" height="64" border="0" style="padding-right: 20px;" />
                                                </td>
                                                <td>
                                                   <div style="font-size: 22px; padding-bottom: 4px;">Indexeerverslag</div>
                                                   <div style="font-size: 13px;">Overzicht van documenten die zijn geindexeerd en toegevoegd aan een P-Dossier.</div>
                                                </td>
                                                <td><div style="text-align:right"><a href="${shareUrl}/page/context/adacta/adacta-search">raadplegen</a></div>
											         <div style="text-align:right"><a href="${shareUrl}/page/context/adacta/adacta-index">indexeer-scan</a>
											         <div style="text-align:right"><a href="${shareUrl}/page/context/adacta/adacta-import">indexeer-import</a></div></div>	
												</td>
                                             </tr>
                                          </table>
                                          <div style="font-size: 14px; margin: 12px 0px 24px 0px; padding-top: 10px; border-top: 1px solid #aaaaaa;">
                                             <p><strong>Indexeerder</strong>: ${username}</p>
                                             <p><strong>Aangemaakt op</strong>: <@dateFormat created /></p>
											 <p><strong>Aantal documenten</strong>: ${size?c}</p>
                                             <br />
                                             <p><strong>Documenten</strong></p>
											 <table width="100%" style="border: 1px solid #aaaaaa;">
											 <tr>
											 	<td><strong>Nr</strong></td>
											 	<td><strong>Doc.naam</strong></td>
											 	<td><strong>Doc.datum</strong></td>
												<td><strong>Werknemer</strong></td>
												<td><strong>Reg.nummer</strong></td>
												<td><strong>Naam, voornaam</strong></td>
												<td><strong>Onderdeel</strong></td>
												<td><strong>Rubriek</strong></td>
												<td><strong>Onderwerp</strong></td>
												<td><strong>Kenmerk</strong></td>
												<td><strong>Werkdossier</strong></td>
												<td><strong>Casenummer</strong></td>
											 </tr>                                             
											 <#if items??>
											 <#list items as item>
											 <#assign counter=counter+1 />											 
											 <tr>
											 	<td>${counter}</td>
											 	<td><a href="${shareUrl}/page/document-details?nodeRef=workspace://SpacesStore/${item.properties['sys:node-uuid']}">${item.properties["cm:name"]}</a></td><!-- dit moet een link naar het document worden!!!-->
											 	<td><@dateFormat2 item.properties["ada:docDate"]!"" /></td>
											 	<td>${item.properties["ada:employeeNumber"]!""}</td>
											 	<td>${item.properties["ada:employeeMrn"]!""}</td>
											 	<td>${item.properties["ada:employeeName"]!""}</td>
											 	<td>${item.properties["ada:employeeDepartment"]!""}</td>
											 	<td>
												<#if msg?? && item.properties["ada:docCategory"]??>
											 	<#list msg as m>
											 	<#if m.categoryCode?? && m.categoryCode == item.properties["ada:docCategory"]>${m.categoryLabel!""}</#if>											 	
												</#list>											 	
												</#if>
											 	</td>
											 	<td>
												<#if msg?? && item.properties["ada:docSubject"]??>
											 	<#list msg as m>
											 	<#if m.subjectCode?? && m.subjectCode == item.properties["ada:docSubject"]>${m.subjectLabel!""}</#if>											 	
												</#list>											 	
												</#if>											 	
											 	</td>
											 	<td>${item.properties["ada:docReference"]!""}</td>
											 	<td>${item.properties["ada:docWorkDossier"]!""}</td>
											 	<td>${item.properties["ada:docCaseNumber"]!""}</td>								 	
											 </tr>
											 </#list>
											 </#if>
											 </table>
                                          </div>
                                       </td>
                                    </tr>
                                 </table>
                              </td>
                           </tr>
                           <tr>
                              <td>
                                 <div style="border-bottom: 1px solid #aaaaaa;">&nbsp;</div>
                              </td>
                           </tr>
                        </table>
                     </td>
                  </tr>
               </table>
            </td>
         </tr>
      </table>
   </body>
</html>