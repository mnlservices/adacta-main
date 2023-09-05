<#escape x as jsonUtils.encodeJSONString(x)>
{
    "auditEnabled": ${auditEnabled?c},
    "serverMode": "${serverMode?string}"
}
</#escape>