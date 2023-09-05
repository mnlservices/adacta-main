package nl.defensie.adacta.webscript.preferences;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.webscript.AdactaAbstract;

/**
 * Add selected items to the preferences of user.
 * 
 * @author Rick de Rooij
 *
 */
public class SelectedItemsPost extends AdactaAbstract {

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		JSONObject jsonRequest = (JSONObject) req.parseContent();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("JSON to process: " + jsonRequest);
		}

		Map<String, Object> model = new HashMap<String, Object>();

		try {
			List<NodeRef> items = getNodeRefList(jsonRequest);
			adactaPreferenceService.setSelectedItemsPreferences(items);

			Map<String, Serializable> responseInfo = new HashMap<String, Serializable>();
			responseInfo.put("result", true);
			responseInfo.put("message", "Items are added to user preferences.");

			model.put("response", responseInfo);
			return model;

		} catch (JSONException e) {
			LOGGER.error(e);
		}
		return model;
	}

	@Override
	protected void executeFinallyImpl(WebScriptRequest req, Status status, Cache cache, Map<String, Object> model) {
	}

	/**
	 * Get node reference list from JSON object.
	 * 
	 * @param jsonRequest
	 * @return List<NodeRef> list of nodes.
	 * @throws JSONException
	 */
	private List<NodeRef> getNodeRefList(JSONObject jsonRequest) throws JSONException {
		List<NodeRef> list = new ArrayList<NodeRef>();
		Object obj = getJsonObject(jsonRequest, "items");
		if (obj instanceof JSONArray) {
			JSONArray jsonArr = (JSONArray) obj;
			for (int i = 0; i < jsonArr.length(); i++) {
				Object jsonObject = jsonArr.get(i);
				if (jsonObject instanceof String) {
					NodeRef nodeRef = new NodeRef((String) jsonObject);
					list.add(nodeRef);
				}
			}
		}
		return list;
	}
}