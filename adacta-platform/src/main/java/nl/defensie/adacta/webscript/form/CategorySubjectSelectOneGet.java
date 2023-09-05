package nl.defensie.adacta.webscript.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.template.TemplateNode;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.ISO9075;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.model.AdactaDatalistModel;
import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.webscript.AdactaAbstract;

/**
 * Supports the drop down lists for categories and subjects during edit mode (index).
 * 
 * @author Rick de Rooij
 *
 */
public class CategorySubjectSelectOneGet extends AdactaAbstract {

	private static final String PARAM_FILTER = "filter";
	private static final String PARAM_SHOW_LIST = "show_list";
	private static final String PARAM_ID_TYPE = "id_type";
	private static final String PARAM_ID_VALUE = "id_value";

	private static final String BASE_QUERY = String.format("+PATH:\"/app:company_home/st:sites/cm:%s//*\" AND TYPE:\"ada:document\"", ISO9075.encode(AdactaModel.PREFIX));

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

		// Get params
		Map<String, String> urlVars = req.getServiceMatch().getTemplateVars();

		String datalistLocalName = urlVars.get(PARAM_DATALIST_LOCALNAME);
		String filter = urlVars.get(PARAM_FILTER);

		if (datalistLocalName == null || datalistLocalName.equals("")) {
			throw new AlfrescoRuntimeException(String.format("url-parameter %s must be specified!", PARAM_DATALIST_LOCALNAME));
		}

		String idType = req.getParameter(PARAM_ID_TYPE);
		String idValue = req.getParameter(PARAM_ID_VALUE);
		List<String> categoriesInDossier = new ArrayList<String>();
		if (idType != null && idValue != null) {
			// Create query
			String query = BASE_QUERY + " AND @ada\\:" + idType + ":'" + idValue + "'";

			// Build search
			SearchParameters searchParameters = new SearchParameters();
			searchParameters.setQuery(query);
			searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
			searchParameters.setSkipCount(0);
			searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

			List<NodeRef> docs = adactaSearchService.query(searchParameters);

			for (NodeRef doc : docs) {
				String category = (String) nodeService.getProperty(doc, AdactaModel.PROP_DOC_CATEGORY);
				if (!categoriesInDossier.contains(category) && hasDocumentAccess(doc)) {
					categoriesInDossier.add(category);
				}
			}
		}
		
		// Get all data list items in specific site
		List<TemplateNode> items = new ArrayList<TemplateNode>();
		NodeRef dataList = adactaSiteService.getDataListRoot(AdactaModel.PREFIX, datalistLocalName);
		List<ChildAssociationRef> dataListsItems = nodeService.getChildAssocs(dataList, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL, true);
		for (ChildAssociationRef car : dataListsItems) {
			NodeRef dlItem = car.getChildRef();
			if (filter != null) {
				String value = (String) nodeService.getProperty(dlItem, AdactaDatalistModel.PROP_VALUE);
				if (value.startsWith(filter)) {
					items.add(new TemplateNode(dlItem, serviceRegistry, null));
				}
			} else {
				if (idType != null) {
					String value = (String) nodeService.getProperty(dlItem, AdactaDatalistModel.PROP_VALUE);
					if (categoriesInDossier.contains(value) && adactaSiteService.hasCategoryAccess(authenticationService.getCurrentUserName(), value)) {
						items.add(new TemplateNode(dlItem, serviceRegistry, null));
					}
				} else {
					String value = (String) nodeService.getProperty(dlItem, AdactaDatalistModel.PROP_VALUE);
					if (adactaSiteService.hasCategoryAccess(authenticationService.getCurrentUserName(), value)) {
						items.add(new TemplateNode(dlItem, serviceRegistry, null));
					}
				}
			}
		}
		//sort alpha
		items = sortCategoryFilter(items);
		
		Map<String, Object> returnMap = new HashMap<String, Object>();
		if (req.getParameter(PARAM_SHOW_LIST) != null) {
			Boolean showList = Boolean.valueOf(req.getParameter(PARAM_SHOW_LIST));
			if (showList) {
				returnMap.put("items", items);
			} else {
				returnMap.put("items", new ArrayList<TemplateNode>());
			}
		} else {
			returnMap.put("items", items);
		}

		// Add value if provided
		String currentValue = req.getParameter(PARAM_CURRENT_VALUE);
		if (currentValue == null) {
			currentValue = "";
		}
		returnMap.put("currentValue", currentValue);
		returnMap.put("currentValueMsg", messageService.getMessage(currentValue));

		return returnMap;
	}

	@Override
	protected void executeFinallyImpl(WebScriptRequest req, Status status, Cache cache, Map<String, Object> model) {
	}
}