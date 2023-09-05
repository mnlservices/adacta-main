<#escape x as jsonUtils.encodeJSONString(x)>
<#if response??>
{
	"result": <#if response.result?is_number>${response.result?c}<#else>${response.result?string}</#if>,
	"message":"${response.message!""}"
}
</#if>
</#escape>