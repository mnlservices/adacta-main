package nl.defensie.adacta.webscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.model.AdactaModel;

/**
 * Get total amount of content childs. This is used for the index page in which you can see the total amount of files in each batch.
 * 
 * @author Rick de Rooij
 *
 */
public class ChildrenGet extends AdactaAbstract {

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		Map<String, String> urlVars = req.getServiceMatch().getTemplateVars();
		NodeRef container = buildNodeRef(urlVars);

		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
		List<ChildAssociationRef> children = nodeService.getChildAssocs(container);
		Iterator<ChildAssociationRef> i = children.iterator();
		while (i.hasNext()) {
			ChildAssociationRef ref = i.next();
			NodeRef child = ref.getChildRef();

			if (nodeService.getType(child).equals(ContentModel.TYPE_CONTENT) || nodeService.getType(child).equals(AdactaModel.TYPE_DOCUMENT)) {
				items.add(buildModel(child));
			}
		}

		model.put("total", items.size());
		model.put("items", items);

		return model;
	}
}