package nl.defensie.adacta.webscript.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.ModelUtil;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.webscript.AdactaAbstract;

/**
 * Get the selected items of user. This is used in Share for displaying the progress of items that are being indexed.
 * 
 * @author Rick de Rooij
 *
 */
public class SelectedItemsGet extends AdactaAbstract {

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		Map<String, String> urlVars = req.getServiceMatch().getTemplateVars();
		String userName = urlVars.get(PARAM_USER_NAME);

		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
		List<NodeRef> list = adactaPreferenceService.getSelectedItemsPreferences(userName);

		for (NodeRef item : list) {
			if (nodeService.getType(item).equals(ContentModel.TYPE_CONTENT) || nodeService.getType(item).equals(AdactaModel.TYPE_DOCUMENT)) {
				items.add(buildModel(item));
			}
		}

		// build the model
		int totalItems = items.size();
		int maxItems = 1000;
		int skipCount = 0;

		skipCount = skipCount > items.size() ? 0 : skipCount;

		List<Map<String, Object>> itemsPaged = applyPagination(items, maxItems, skipCount);
		model.put("items", itemsPaged);

		// maxItems or skipCount parameter was provided so we need to include paging into response
		model.put("paging", ModelUtil.buildPaging(totalItems, maxItems, skipCount));

		return model;
	}

	@Override
	protected void executeFinallyImpl(WebScriptRequest req, Status status, Cache cache, Map<String, Object> model) {
	}
}