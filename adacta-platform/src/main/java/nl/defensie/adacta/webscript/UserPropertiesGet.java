package nl.defensie.adacta.webscript;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.model.AdactaModel;

/**
 * Get user object properties (including custom properties) based on provided username.
 * 
 * @author Miruna Chirita
 *
 */
public class UserPropertiesGet extends AdactaAbstract {

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		Map<String, String> urlVars = req.getServiceMatch().getTemplateVars();
		String userName = urlVars.get(PARAM_USER_NAME);
		
		NodeRef authorityRef = authorityService.getAuthorityNodeRef(userName);
		
		Map<String, Object> item = new HashMap<String, Object>();

		// Custom props
		item.put("dossierRef", nodeService.getProperty(authorityRef, AdactaModel.PROP_DOSSIER_REF));
		item.put("dpCode", nodeService.getProperty(authorityRef, AdactaModel.PROP_DP_CODE));
		item.put("employeeID", nodeService.getProperty(authorityRef, AdactaModel.PROP_EMPLOYEE_ID));
		
		model.put("item", item);

		return model;
	}
}