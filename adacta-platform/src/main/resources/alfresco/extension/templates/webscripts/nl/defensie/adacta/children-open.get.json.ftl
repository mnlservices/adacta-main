<#escape x as jsonUtils.encodeJSONString(x)>
{
    "total": "<#if total??>${total}</#if>",
    "nodeRefs":
      [
         <#list items as item>
         {
            "nodeRef": "${item.nodeRef}"
         }<#if item_has_next>,</#if>
         </#list>
      ]
}
</#escape>