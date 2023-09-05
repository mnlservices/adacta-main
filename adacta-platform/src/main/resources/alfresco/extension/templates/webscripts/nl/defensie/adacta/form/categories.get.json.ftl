<#macro renderItem item>
	{
	   	"name": "${item.properties["adadl:value"]!}",
	   	"desc": "${item.properties["adadl:description"]!}"
	}
</#macro>
{
   "items": 
   [
    <#escape x as jsonUtils.encodeJSONString(x)>
    <#list items as i>
      <@renderItem i /><#if i_has_next>,</#if>
    </#list>
    </#escape>
   ]
}