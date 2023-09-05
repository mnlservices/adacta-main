<#include "/org/alfresco/components/form/controls/common/utils.inc.ftl" />

<#macro renderFieldHelp field>
   <#if field.help?? && field.help?length &gt; 0>
      <span class="help-icon">
         <img id="${fieldHtmlId}-help-icon" src="${url.context}/res/components/form/images/help.png" title="${msg("form.field.help")}" tabindex="1"/>
      </span>
      <div class="help-text" id="${fieldHtmlId}-help"><#if field.helpEncodeHtml>${field.help?html}<#else>${stringUtils.stripUnsafeHTML(field.help)}</#if></div>
   </#if>
</#macro>

<#if field.control.params.jsonGetUrl??>
   <#assign jsonGetUrl=field.control.params.jsonGetUrl>
<#else>
   <#assign jsonGetUrl="">
</#if>
<#if field.control.params.jsonGetRoot??>
   <#assign jsonGetRoot=field.control.params.jsonGetRoot>
<#else>
   <#assign jsonGetRoot="items">
</#if>
<#if field.control.params.jsonGetId??>
   <#assign jsonGetId=field.control.params.jsonGetId>
<#else>
   <#assign jsonGetId="id">
</#if>
<#if field.control.params.jsonGetName??>
   <#assign jsonGetName=field.control.params.jsonGetName>
<#else>
   <#assign jsonGetName="name">
</#if>
<#if field.control.params.showList??>
   <#assign showList=field.control.params.showList>
<#else>
   <#assign showList="true">
</#if>
<#if field.control.params.defaultValue??>
  <#assign defaultValue=field.control.params.defaultValue>
</#if>
<#if field.control.params.defaultText??>
  <#assign defaultText=field.control.params.defaultText>
</#if>
<#assign fieldValue=field.value>
<div class="form-field">
	<#if form.mode == "view">
		<div class="viewmode-field">
			<span class="viewmode-label">${field.label?html}:</span>
			<#if fieldValue?string == "">
            	<#assign valueToShow=msg("form.control.novalue")>
			<#else>
            	<#assign valueToShow=fieldValue>
            	<#if field.control.params.options?? && field.control.params.options != "">
            		<#list field.control.params.options?split(optionSeparator) as nameValue>
                  		<#if nameValue?index_of(labelSeparator) == -1>
                     		<#if nameValue == fieldValue?string || (fieldValue?is_number && fieldValue?c == nameValue)>
                        		<#assign valueToShow=nameValue>
                        		<#break>
                     		</#if>
                  		<#else>
                    		<#assign choice=nameValue?split(labelSeparator)>
                    		<#if choice[0] == fieldValue?string || (fieldValue?is_number && fieldValue?c == choice[0])>
                        		<#assign valueToShow=msgValue(choice[1])>
                        		<#break>
                     		</#if>
                  		</#if>
               		</#list>
            	</#if>
         	</#if>
         	<span class="viewmode-value">${msg("${valueToShow?html}")}</span>
		</div>
	<#else>
		<label for="${fieldHtmlId}">${field.label?html}:<#if field.mandatory><span class="mandatory-indicator">${msg("form.required.fields.marker")}</span></#if></label>
		<select id="${fieldHtmlId}" name="${field.name}" tabindex="0"
	    	<#if field.description??>title="${field.description}"</#if>
	       	<#if field.indexTokenisationMode??>class="non-tokenised"</#if>
	       	<#if field.control.params.size??>size="${field.control.params.size}"</#if> 
	       	<#if field.control.params.styleClass??>class="${field.control.params.styleClass}"</#if>
	       	<#if field.control.params.style??>style="${field.control.params.style}"</#if>
	       	<#if field.disabled  && !(field.control.params.forceEditable?? && field.control.params.forceEditable == "true")>disabled="true"</#if>>
		</select>
    	<@renderFieldHelp field=field />
 	</#if>
</div>

<script type="text/javascript">//<![CDATA[
require(dojoConfig,["jquery"], function(jquery){
(function() {
	var element = new YAHOO.util.Element("${fieldHtmlId}"); 
	element.on('contentReady', function() {

	    YAHOO.util.Event.onAvailable("${fieldHtmlId}", function(){
	        new selectAjax("${fieldHtmlId}");
	    });
	
	    function selectAjax(currentValueHtmlId) {
	        this.currentValueHtmlId = currentValueHtmlId;
	        
	        var select = Dom.get(this.currentValueHtmlId);
	
	        this.register = function () {
	            Alfresco.util.Ajax.jsonGet({
	                url: Alfresco.constants.PROXY_URI + "${jsonGetUrl}?current_value=${field.value}<#if showList??>&show_list=${showList}</#if>",
	                successCallback: {
						fn: this.updateOptions,
	                    scope: this
	                },
	                failureCallback: {
	                    fn: function(){},
	                    scope: this
	                }
	            });
	        };
	        
	        this.updateOptions = function (res) {
				
				var i = 0;
				var currentValue;
				var currentValueMsg;
				
				var result = Alfresco.util.parseJSON(res.serverResponse.responseText);
				
				if (result && result.currentValue && result.currentValueMsg) {
					currentValue = result.currentValue;
					currentValueMsg = result.currentValueMsg;
				}
				
				<#if defaultValue?? && defaultText??>
				$('#${fieldHtmlId}').append($('<option>', { value : "${defaultValue}" }).text("${defaultText}")); 
				</#if>
				
				if(currentValue != null && currentValue.length > 0){
	                $('#${fieldHtmlId}').append($('<option>', { value : currentValue }).text(currentValueMsg)); 
	                i++;
				}
	            
	            if (result && result.${jsonGetRoot}.length > 0) {
	                var items = result.${jsonGetRoot};
	                
	                for (var item in items) {
	                	if(items[item].${jsonGetName} != currentValue){
		                    $('#${fieldHtmlId}').append($('<option>', { value : items[item].${jsonGetId} }).text(items[item].${jsonGetName})); 
		                    i++;
	                    }
	                }
	            }
	            
	        };
	        this.register();
	    }    
	 });
  })();
});   
//]]></script>