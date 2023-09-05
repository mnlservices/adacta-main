package nl.defensie.adacta.webscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.ModelUtil;
import org.alfresco.util.ScriptPagingDetails;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * General search webscript with paging and sorting support. This is used for every search function in Adacta. 
 * 
 * @author Rick de Rooij
 *
 */
public class FileFolderGet extends AdactaAbstract {

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		// Define result set
		List<NodeRef> results = null;

		// Determine search query based on search type.
		String searchType = req.getParameter(PARAM_SEARCH_TYPE);
		if (searchType != null && searchType.equalsIgnoreCase("indexPage")) {
			results = doIndexFolderSearch(req);
		} else if (searchType != null && searchType.equalsIgnoreCase("searchPage")) {
			results = doSearch(req);
		} else if (searchType != null && searchType.equalsIgnoreCase("overviewDeleteRequests")) {
			results = doOverviewDeleteRequestsSearch(req);
		} else if (searchType != null && searchType.equalsIgnoreCase("overviewIndexReports")) {
			results = doOverviewIndexReportsSearch(req);
		} else if (searchType != null && searchType.equalsIgnoreCase("overviewUnprocessedFolderFiles")) {
			results = doOverviewUnprocessedFolderFilesSearch(req);
		} else {
			// Fall on default search
			results = doFileFolderSearch(req);
		}

		// Process search results
		int processStartRow = getSkipCount(req);
		int processEndRow = getSkipCount(req) + getMaxItems(req);
		int totalRecords = results.size();

		processEndRow = Math.min(processEndRow, totalRecords);

		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
		for (int i = processStartRow; i < processEndRow; i++) {
			items.add(buildModel(results.get(i)));
		}

		// Grab the paging parameters
		ScriptPagingDetails paging = new ScriptPagingDetails(getNonNegativeIntParameter(req, PARAM_PAGE_SIZE, getMaxItems(req)),
				getNonNegativeIntParameter(req, PARAM_START_INDEX, 0));

		// Build the model
		model.put("items", items);

		// Because we haven't used ModelUtil.page method, we need to set the total items manually.
		paging.setTotalItems((int) totalRecords);
		model.put("paging", ModelUtil.buildPaging(paging));

		return model;
	}
}