<#macro renderItem item>
	<#escape y as jsonUtils.encodeJSONString(y)>
	{
		"id": "${item.nodeRef}",
		"text": "${item.properties["ada:employeeName"]!""} | ${item.properties["ada:employeeBsn"]!""} | ${item.properties["ada:employeeMrn"]!""} | ${item.properties["ada:employeeNumber"]!""} | ${item.properties["ada:employeeDepartment"]!""}"
	}
	</#escape>	
</#macro>

<#if items??>
   [
    <#list items as i>
      <@renderItem i /><#if i_has_next>,</#if>
    </#list>
   ]
</#if>