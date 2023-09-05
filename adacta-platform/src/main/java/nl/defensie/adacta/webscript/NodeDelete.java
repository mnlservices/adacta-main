package nl.defensie.adacta.webscript;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.action.DeleteNodeActionExecuter;

/**
 * Delete a node by using the {@link DeleteNodeActionExecuter}. In order to manage the deletion of (Adacta) nodes on one location.
 * 
 * @author Rick de Rooij
 *
 */
public class NodeDelete extends AdactaAbstract {

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		Map<String, String> urlVars = req.getServiceMatch().getTemplateVars();
		NodeRef nodeRef = buildNodeRef(urlVars);

		Action action = actionService.createAction(DeleteNodeActionExecuter.NAME);
		action.setExecuteAsynchronously(false);
		actionService.executeAction(action, nodeRef);

		model.put("success", true);
		return model;
	}
}