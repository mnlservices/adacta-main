<#import "/org/alfresco/repository/generic-paged-results.lib.ftl" as genericPaging />
<#escape x as jsonUtils.encodeJSONString(x)>
{
   "data":
   {
      "selectedItems":
      [
         <#list items as item>
         {
            "id": "${item.id}",
            "nodeRef": "${item.nodeRef}",
            "name": "${item.name}",
            "title": "${item.title!""}",
            "description": "${item.description!""}",            
            "creator": "${item.creator}",
            "modifier": "${item.modifier}",
            "modifierFullName": "${item.modifierFullName}",
            "created": "${xmldate(item.created)}",
            "modified": "${xmldate(item.modified)}",
            "nodeType": "${item.nodeType}",
            "isContentType": <#if item.isContentType?is_number>${item.isContentType?c}<#else>${item.isContentType?string}</#if>,        
            "employeeNumber": "<#if item.employeeNumber??>${item.employeeNumber}</#if>",
            "employeeName": "<#if item.employeeName??>${item.employeeName}</#if>",
            "employeeBsn": "<#if item.employeeBsn??>${item.employeeBsn}</#if>",
            "employeeMrn": "<#if item.employeeMrn??>${item.employeeMrn}</#if>",
            "employeeDepartment": "<#if item.employeeDepartment??>${item.employeeDepartment}</#if>",
            "docCategory": "<#if item.docCategory??>${item.docCategory}</#if>",
            "docSubject": "<#if item.docSubject??>${item.docSubject}</#if>",
            "docDate": "<#if item.docDate??>${xmldate(item.docDate)}</#if>",
            "docReference": "<#if item.docReference??>${item.docReference}</#if>",
            "docWorkDossier": "<#if item.docWorkDossier??>${item.docWorkDossier}</#if>",
            "docCaseNumber": "<#if item.docCaseNumber??>${item.docCaseNumber}</#if>",
            "docDateCreated": "<#if item.docDateCreated??>${xmldate(item.docDateCreated)}</#if>",
            "docMigId": "<#if item.docMigId??>${item.docMigId}</#if>",
            "docMigDate": "<#if item.docMigDate??>${xmldate(item.docMigDate)}</#if>",
            "docStatus": "<#if item.docStatus??>${item.docStatus}</#if>",
            "scanEmployee": "<#if item.scanEmployee??>${item.scanEmployee}</#if>",
            "scanSeqNr": "<#if item.scanSeqNr??>${item.scanSeqNr}</#if>",
            "scanWaNr": "<#if item.scanWaNr??>${item.scanWaNr}</#if>",
	        "adactaBrowseUrl": "<#if item.adactaBrowseUrl??>${item.adactaBrowseUrl}</#if>",
	        "adactaDetailUrl": "<#if item.adactaDetailUrl??>${item.adactaDetailUrl}</#if>"           
         }<#if item_has_next>,</#if>
         </#list>
      ]
   }

   <@genericPaging.pagingJSON />
}
</#escape>