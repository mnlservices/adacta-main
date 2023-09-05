<#macro renderItem item>
	{
	   	"name": "${item.properties["adadl:value"]!}",
	   	"msg": "${item.properties["adadl:description"]!}"
	}
</#macro>
{
   "items": 
   [
   <#if currentValue?length == 0>
	{
	   	"name": " ",
	   	"msg": " "
	},  
   </#if>
   <#escape x as jsonUtils.encodeJSONString(x)>
    <#list items as i>
      <@renderItem i /><#if i_has_next>,</#if>
    </#list>
    </#escape>
   ],
   "currentValue": "${currentValue}",
   "currentValueMsg": "${currentValueMsg!""}"
}