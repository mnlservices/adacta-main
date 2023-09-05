<#escape x as jsonUtils.encodeJSONString(x)>
{
    "dossierRef": "<#if item.employeeMrn??>${item.dossierRef}</#if>",
    "dpCode": "<#if item.dpCode??>${item.dpCode}</#if>",
    "employeeID": "<#if item.employeeID??>${item.employeeID}</#if>"
}
</#escape>