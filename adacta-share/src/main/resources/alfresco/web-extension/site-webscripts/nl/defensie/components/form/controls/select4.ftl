<#if field.control.params.jsonGetUrl??><#assign jsonGetUrl=field.control.params.jsonGetUrl><#else><#assign jsonGetUrl=""></#if>
<#if field.control.params.minimumInputLength??><#assign minimumInputLength=field.control.params.minimumInputLength><#else><#assign minimumInputLength=0></#if>
<#if field.control.params.inputTooLong??><#assign inputTooLong=field.control.params.inputTooLong><#else><#assign inputTooLong=100></#if>
<#if field.control.params.maximumSelected??><#assign maximumSelected=field.control.params.maximumSelected><#else><#assign maximumSelected=250></#if>
<#if field.control.params.width??><#assign width=field.control.params.width><#else><#assign width="100%"></#if>
<#if field.control.params.includeSite??><#assign includeSite=field.control.params.includeSite><#else><#assign includeSite=""></#if>

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
.select2-blue {
background-color:#ccccff;
} 
.select2-white{
background-color:white;
}
</style>

<script type="text/javascript">//<![CDATA[
console.log($.fn.jquery);
		$(document).on('click','#${fieldHtmlId}-search-input', function() {	
			var suffix="?";
			var emplid=$('#${fieldHtmlId}-adacta-emplid').val();
			if (emplid){
		 		suffix = suffix +"emplid="+emplid.trim();
			}
			var name=$('#${fieldHtmlId}-adacta-naam').val();
			if (name && suffix.length==1){
		  		suffix = suffix+"name="+name.trim();
			}else{
		  		suffix = suffix+"&name="+name.trim();
			}
			var id=$('#${fieldHtmlId}-adacta-id').val();
			if (id && suffix.length==1){
		  		suffix = suffix+"id="+id.trim();
			}else{
		  		suffix = suffix+"&id="+id.trim();
			}
			var mrn=$('#${fieldHtmlId}-adacta-mrn').val();
			if (mrn && suffix.length==1){
		  		suffix = suffix+"mrn="+mrn.trim();
			}else{
		  		suffix = suffix+"&mrn="+mrn.trim();
			}
			//maak eerst selectbox weer leeg
			$("#${fieldHtmlId}-entry").empty();
			//verwijder een eventuele melding
			$("#${fieldHtmlId}-koppel_aan").html("");
			$("#${fieldHtmlId}-niets_ingevuld").html("");			
			if (!id || id==="NLD-"){
				if (!emplid && !name && !mrn){
					$("#${fieldHtmlId}-niets_ingevuld").html("Vul svp minimaal Ã©Ã©n invoerveld in!");
					return;
				}
			}
			$.ajax({	
			        url: '${url.context}/proxy/alfresco/${jsonGetUrl}'+suffix,
			        dataType: "json",
			        type: "GET"
			    }).success(function(data){
					$("#${fieldHtmlId}-entry").empty();
					if (data && data.length==1 && data[0].id=="no results"){
						$("#${fieldHtmlId}").val("");	
						$("#${fieldHtmlId}-koppel_aan").html("Geen resultaten");
						return;
					}
					for(let i = 0; i < data.length; i++){
						$("#${fieldHtmlId}-entry").append('<option class="select2-white" value='+data[i].id+'>'+data[i].text+'</option>');
					}
					$("#${fieldHtmlId}").val(data[0].id);	
					if (data.length>1){
						$("#${fieldHtmlId}-koppel_aan").html("Kies het juiste dossier uit onderstaande lijst.");
					}else{
						$("#${fieldHtmlId}-koppel_aan").html("");
					}
				});
			});
//]]></script>
<div class="form-field">
		<input id="${fieldHtmlId}" name="${field.name}" value="dossier-id" type="hidden"/>
		<br>
		<table>
		<tr><td colspan="2"><div id="${fieldHtmlId}-niets_ingevuld"></div></td></tr>
		<tr><td style="padding-right:5px;">werknemernr.:</td><td><input id="${fieldHtmlId}-adacta-emplid" name="emplid"/></td></tr>
		<tr><td>naam:</td><td><input id="${fieldHtmlId}-adacta-naam" name="naam"/></td></tr>
		<tr><td>id:</td><td><input id="${fieldHtmlId}-adacta-id" name="idd" value="NLD-"/></td></tr>
		<tr><td>mrn:</td><td><input id="${fieldHtmlId}-adacta-mrn" name="mrn" /></td></tr>
		<tr><td></td><td></td></tr>
		<tr><td><input id="${fieldHtmlId}-search-input" type="button" value="Zoek" ></td><td></td></tr>
		</table>
		<br>
		<div id="${fieldHtmlId}-koppel_aan" style="color:green;font-weight:bold"></div>
		<select id="${fieldHtmlId}-entry" name="-" class="select2-blue" style="max-width: ${width}; width: ${width}"  onchange='$("#${fieldHtmlId}").val($(this).val());'>
		</select>
</div>