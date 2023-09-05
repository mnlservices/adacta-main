package nl.defensie.adacta.webscript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ModelUtil;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.model.AdactaModel;

/**
 * Supporting the import page for importing documents to be indexed. This mechanism uses child association instead of a search query. Because we need to provided instant result whan user uploads a
 * document.
 * 
 * @author Rick de Rooij
 *
 */
public class ImportFolderListGet extends AdactaAbstract {

	private NodeMapComparator nodeMapComparator;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		NodeRef container = adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_IMPORT);

		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
		List<ChildAssociationRef> children = nodeService.getChildAssocs(container);
		Iterator<ChildAssociationRef> i = children.iterator();
		while (i.hasNext()) {
			ChildAssociationRef ref = i.next();
			NodeRef child = ref.getChildRef();

			if (nodeService.getType(child).equals(ContentModel.TYPE_CONTENT) || nodeService.getType(child).equals(AdactaModel.TYPE_DOCUMENT)) {
				// Only show documents to their owner
				final String currentUser = authenticationService.getCurrentUserName();
				final String owner = ownableService.getOwner(child);
				
				if (currentUser != null && currentUser.equalsIgnoreCase(owner)) {
					items.add(buildModel(child));
				}
			}
		}

		// sort the nodes.
		sortNodes(items, getSortColumn(req), getDirection(req));

		// build the model
		int totalItems = items.size();
		int maxItems = getMaxItems(req);
		int skipCount = getSkipCount(req);

		skipCount = skipCount > items.size() ? 0 : skipCount;

		List<Map<String, Object>> itemsPaged = applyPagination(items, maxItems, skipCount);
		model.put("items", itemsPaged);

		// maxItems or skipCount parameter was provided so we need to include paging into response
		model.put("paging", ModelUtil.buildPaging(totalItems, maxItems, skipCount));

		return model;

	}

	/**
	 * Sorting the list of nodes.
	 * 
	 * @param nodes
	 * @param sortColumn
	 * @param sortDirection
	 */
	private void sortNodes(List<Map<String, Object>> nodes, QName sortColumn, String sortDirection) {

		if (sortColumn == null || sortDirection == null) {
			// initial sorting if no column is selected
			Collections.sort(nodes, nodeMapComparator.nodeCreatedAscComparator);
		} else {
			if (sortColumn.equals(ContentModel.PROP_CREATED)) {
				if (sortDirection.equals("asc")) {
					Collections.sort(nodes, nodeMapComparator.nodeCreatedAscComparator);
				} else {
					Collections.sort(nodes, Collections.reverseOrder(nodeMapComparator.nodeCreatedAscComparator));
				}
			} else if (sortColumn.equals(ContentModel.PROP_TITLE)) {
				if (sortDirection.equals("asc")) {
					Collections.sort(nodes, nodeMapComparator.nodeTitleAscComparator);
				} else {
					Collections.sort(nodes, Collections.reverseOrder(nodeMapComparator.nodeTitleAscComparator));
				}
			} else if (sortColumn.equals(ContentModel.PROP_NAME)) {

				if (sortDirection.equals("asc")) {
					Collections.sort(nodes, nodeMapComparator.nodeNameAscComparator);
				} else {
					Collections.sort(nodes, Collections.reverseOrder(nodeMapComparator.nodeNameAscComparator));
				}
			} else if (sortColumn.equals(AdactaModel.PROP_DOC_CATEGORY)) {

				if (sortDirection.equals("asc")) {
					Collections.sort(nodes, nodeMapComparator.nodeDocCategoryAscComparator);
				} else {
					Collections.sort(nodes, Collections.reverseOrder(nodeMapComparator.nodeDocCategoryAscComparator));
				}
			} else if (sortColumn.equals(AdactaModel.PROP_DOC_SUBJECT)) {

				if (sortDirection.equals("asc")) {
					Collections.sort(nodes, nodeMapComparator.nodeDocSubjectAscComparator);
				} else {
					Collections.sort(nodes, Collections.reverseOrder(nodeMapComparator.nodeDocSubjectAscComparator));
				}
			} else if (sortColumn.equals(AdactaModel.PROP_DOC_DATE)) {

				if (sortDirection.equals("asc")) {
					Collections.sort(nodes, nodeMapComparator.nodeDocDateAscComparator);
				} else {
					Collections.sort(nodes, Collections.reverseOrder(nodeMapComparator.nodeDocDateAscComparator));
				}
			}
		}
	}

	public NodeMapComparator getNodeMapComparator() {
		return nodeMapComparator;
	}

	public void setNodeMapComparator(NodeMapComparator nodeMapComparator) {
		this.nodeMapComparator = nodeMapComparator;
	}
}