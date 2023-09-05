package nl.defensie.adacta.webscript.preferences;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.webscript.AdactaAbstract;

/**
 * Evaluator used by the Share Client.
 * 
 * @see nl.defensie.adacta.evaluator.HasPreviousSelectedItems
 * @see nl.defensie.adacta.evaluator.HasNextSelectedItems
 * @author Rick de Rooij
 *
 */
public class HasPreviousNextSelectedItemsGet extends AdactaAbstract {

	public static final String DIRECTION_NEXT = "next";
	public static final String DIRECTION_PREVIOUS = "prev";

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

		Map<String, String> urlVars = req.getServiceMatch().getTemplateVars();

		NodeRef currentNodeRef = buildNodeRef(urlVars);

		String userName = urlVars.get(PARAM_USER_NAME);
		String direction = urlVars.get(PARAM_DIR);

		String result = "false";

		if (adactaPreferenceService.hasSelectedItems(userName)) {
			if (direction.equalsIgnoreCase(DIRECTION_NEXT) && adactaPreferenceService.hasNextSelectedItem(userName, currentNodeRef)) {
				result = "true";
			} else if (direction.equalsIgnoreCase(DIRECTION_PREVIOUS) && adactaPreferenceService.hasPreviousSelectedItem(userName, currentNodeRef)) {
				result = "true";
			}
		}

		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("value", result);
		return returnMap;
	}

	@Override
	protected void executeFinallyImpl(WebScriptRequest req, Status status, Cache cache, Map<String, Object> model) {
	}
}