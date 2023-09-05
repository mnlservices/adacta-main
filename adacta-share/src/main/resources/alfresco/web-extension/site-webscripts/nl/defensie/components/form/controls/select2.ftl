<#if field.control.params.jsonGetUrl??><#assign jsonGetUrl=field.control.params.jsonGetUrl><#else><#assign jsonGetUrl=""></#if>
<#if field.control.params.minimumInputLength??><#assign minimumInputLength=field.control.params.minimumInputLength><#else><#assign minimumInputLength=0></#if>
<#if field.control.params.inputTooLong??><#assign inputTooLong=field.control.params.inputTooLong><#else><#assign inputTooLong=100></#if>
<#if field.control.params.maximumSelected??><#assign maximumSelected=field.control.params.maximumSelected><#else><#assign maximumSelected=250></#if>
<#if field.control.params.width??><#assign width=field.control.params.width><#else><#assign width="100%"></#if>
<#if field.control.params.includeSite??><#assign includeSite=field.control.params.includeSite><#else><#assign includeSite=""></#if>

<#-- To make it 4.2 compatible -->
<script type="text/javascript">//<![CDATA[
	dojoConfig.packages.push(
    	{name : 'jquery', location : 'js/jquery', main : 'jquery-1.11.3.min'},
        {name : 'select2', location : 'js/select2', main : 'select2.min'}
	);
//]]></script>

<style>
<#if form.mode == "view">
.select2-container--default.select2-container--disabled .select2-selection--multiple {
    background-color: #fafafa;
    border: 0px;
}
</#if>
.select2-container--default .select2-selection--multiple .select2-selection__choice {
    border-radius: 0px;
}
.select2-container--default .select2-selection--multiple {
    border-radius: 0px;
}
</style>

<script type="text/javascript">//<![CDATA[
require(dojoConfig,["select2"], function(jquery){
	(function() {
		var element = new YAHOO.util.Element("${fieldHtmlId}-entry"); 
		element.on('contentReady', function() {		  		  	
		  	$(".${fieldHtmlId}-select2").select2({
			    language: {
			            errorLoading:function(){
			                return"${msg("select2.noResults")}"
			            },
			            inputTooLong:function(e){
			                return"${msg("select2.inputTooLong", "${inputTooLong}")}";
			            },
			            inputTooShort:function(e){
			                return"${msg("select2.inputTooShort", "${minimumInputLength}")}";
			            },
			            loadingMore:function(){
			                return"${msg("select2.loadingMore")}"
			            },
			            maximumSelected:function(e){
			                return"${msg("select2.maximumSelected", "${maximumSelected}")}";
			            },
			            noResults:function(){
			                return "${msg("select2.noResults")}";
			            },
			            searching:function(){
			                return"${msg("select2.searching")}"
			            }
			    },
				minimumInputLength: ${minimumInputLength},
				maximumSelectionLength: ${maximumSelected},
				<#if jsonGetUrl != "">
			    ajax: {
			        <#if includeSite != "">
			        url: '${url.context}/proxy/alfresco/${jsonGetUrl}/' + Alfresco.constants.SITE,
			        <#else>
			        url: '${url.context}/proxy/alfresco/${jsonGetUrl}',
			        </#if>
			        dataType: "json",
			        type: "GET",
					processResults: function (data) {
					    return {
					        results: $.map(data, function(obj) {
					            return { id: obj.id, text: obj.text };
					        })
					    };
					}        
			    }
			    <#else>
			    tags: true
			    </#if>		    
			})<#if form.mode == "view">.prop("disabled", true)</#if>;
		});
	})();
});
//]]></script>
<div class="form-field">
   <#if form.mode == "view">
      <div class="viewmode-field">
		<span class="viewmode-label">${field.label?html}:</span>
		<#else>	
		<label for="${fieldHtmlId}-entry">${field.label?html}:<#if field.mandatory><span class="mandatory-indicator">${msg("form.required.fields.marker")}</span></#if></label>
		<input id="${fieldHtmlId}" type="hidden" name="${field.name}" value="${field.value?string}" />
		<input type="hidden" id="${fieldHtmlId}_isListProperty" name="${field.name}_isListProperty" value="true" />	
	</#if>	
	<select id="${fieldHtmlId}-entry" name="-" class="${fieldHtmlId}-select2" multiple="multiple" style="max-width: ${width}; width: ${width}" onchange="javascript:Alfresco.util.updateMultiSelectListValue('${fieldHtmlId}-entry', '${fieldHtmlId}', <#if field.mandatory>true<#else>false</#if>);">
	<#if field.value?? && field.value?has_content>
		<#list field.value?split(",") as sValue>
			<option value="${sValue}" selected="selected">${sValue}</option>
		</#list>
	</#if>
	</select>
	<#if form.mode != "view">
		<@formLib.renderFieldHelp field=field />
	</#if>
   	<#if form.mode == "view">
      <div class="viewmode-field">
	</#if>
</div>