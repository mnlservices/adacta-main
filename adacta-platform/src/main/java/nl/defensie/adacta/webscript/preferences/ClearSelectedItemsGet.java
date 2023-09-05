package nl.defensie.adacta.webscript.preferences;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.webscript.AdactaAbstract;

/**
 * Clear the selected items stored in the preferences of user.
 * 
 * @author Rick de Rooij
 *
 */
public class ClearSelectedItemsGet extends AdactaAbstract {

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		adactaPreferenceService.clearSelectedItemsPreferences();

		Map<String, Serializable> responseInfo = new HashMap<String, Serializable>();
		responseInfo.put("result", true);
		responseInfo.put("message", "Selected items are cleared.");

		model.put("response", responseInfo);
		return model;
	}

	@Override
	protected void executeFinallyImpl(WebScriptRequest req, Status status, Cache cache, Map<String, Object> model) {
	}
}