package nl.defensie.adacta.webscript;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Get all Adacta details of some node.
 * 
 * @author Rick de Rooij
 *
 */
public class NodeDetailsGet extends AdactaAbstract {

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		Map<String, String> urlVars = req.getServiceMatch().getTemplateVars();
		NodeRef nodeRef = buildNodeRef(urlVars);

		model.put("item", buildModel(nodeRef));

		return model;
	}
}