package nl.defensie.adacta.webscript.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.template.TemplateNode;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.StringUtils;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.service.AdactaDataListService.KVDataListItem;
import nl.defensie.adacta.webscript.AdactaAbstract;
/**
 * Get a list of category codes. This is used in the external link page for displaying the drop-down category filter. If empid is provided, 
 * the returned list is limited to categories present in the corresponding dossier.
 * 
 * @author Mark Tielemans, Wim Schreurs
 *
 */
public class CategoryFilterGet extends AdactaAbstract {

	//empid: employeeNumber of logged in user
    private static final String PARAM_EMP_ID = "empid";
    private static final String PARAM_BSN = "bsn";
    private static final String PARAM_MRN = "regid";
    // psid: employeeNumber passed as parameter to the webscript
    private static final String PARAM_PSID = "psid";

    /**
     * Restricts category list to the categories that are present in the Dossier.
     */
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        final Map<String, String> urlVars = req.getServiceMatch().getTemplateVars();
        final String empId = req.getParameter(PARAM_EMP_ID);
        final String psId = req.getParameter(PARAM_PSID);
        final String bsn = req.getParameter(PARAM_BSN);
        final String mrnId = req.getParameter(PARAM_MRN);
        final String dataListLocalName = urlVars.get(PARAM_DATALIST_LOCALNAME);
        // call originated from the selfservice page
        boolean selfservice = false;
        if (!StringUtils.isEmpty(empId)){
        	selfservice=true;
        }
        // Datalist name is mandatory
        if (dataListLocalName == null || dataListLocalName.equals("")) {
            throw new AlfrescoRuntimeException(String.format("url-parameter %s must be specified!", PARAM_DATALIST_LOCALNAME));
        }

        final String currentUser = authenticationService.getCurrentUserName();
        final LinkedHashMap<String, KVDataListItem> dataListContents = adactaDataListService.getKVDataListItems(dataListLocalName);
        final List<TemplateNode> results = new ArrayList<TemplateNode>();

        if (StringUtils.isNotBlank(empId) || StringUtils.isNotBlank(psId) || StringUtils.isNotBlank(bsn) || StringUtils.isNotBlank(mrnId)) {
            handleCategoriesBasedOnDossier(empId, psId, bsn, mrnId, selfservice, currentUser, dataListContents,
					results);
        } else {
            handleCategoriesWithoutDossier(currentUser, dataListContents, results, selfservice);
        }

        final Map<String, Object> model = new HashMap<String, Object>();
        model.put("items",  sortCategoryFilter(results));
        return model;
    }

	private void handleCategoriesWithoutDossier(final String currentUser,
			final LinkedHashMap<String, KVDataListItem> dataListContents, final List<TemplateNode> results,
			boolean selfservice) {
		for (final Entry<String, KVDataListItem> entry : dataListContents.entrySet()) {
			if (selfservice) {
				results.add(new TemplateNode(entry.getValue().getNodeRef(), serviceRegistry, null));
			} else {
				if (adactaSiteService.hasCategoryAccess(currentUser, entry.getKey())) {
					results.add(new TemplateNode(entry.getValue().getNodeRef(), serviceRegistry, null));
				}
			}
		}
	}

	private void handleCategoriesBasedOnDossier(final String empId, final String psId, final String bsn,
			final String mrnId, boolean selfservice, final String currentUser,
			final LinkedHashMap<String, KVDataListItem> dataListContents, final List<TemplateNode> results) {
		
		final NodeRef dossier = findDossier(psId, bsn, mrnId, empId);
		if (dossier == null) {
			throw new AlfrescoRuntimeException("Dossier not found for emplID=" + empId + " psId=" + psId + " mrnId="
					+ mrnId + " bsn=" + bsn + " dossier: " + dossier);
		}
		final LinkedList<String> dossierCategoryList = adactaFileFolderService.getDossierCategoryList(dossier,
				selfservice);

		for (final String categoryCode : dossierCategoryList) {
			if (selfservice) {
				final KVDataListItem item = dataListContents.get(categoryCode);
				results.add(new TemplateNode(item.getNodeRef(), serviceRegistry, null));
			} else {
				if (adactaSiteService.hasCategoryAccess(currentUser, categoryCode)) {
					final KVDataListItem item = dataListContents.get(categoryCode);
					results.add(new TemplateNode(item.getNodeRef(), serviceRegistry, null));
				}
			}
		}
	}

	private NodeRef findDossier(final String psId, final String bsn, final String mrn, final String empId) {
		if (!StringUtils.isEmpty(empId) && !empId.equalsIgnoreCase("null")){
			return adactaSearchService.getPersonnelFileByEmployeeNumber(empId);
		}	
		if (!StringUtils.isEmpty(psId) && !psId.equalsIgnoreCase("null")){
			return adactaSearchService.getPersonnelFileByEmployeeNumber(psId);
		}
		if (!StringUtils.isEmpty(bsn) && !bsn.equalsIgnoreCase("null")){
			return adactaSearchService.getPersonnelFileByBSN(bsn);			
		}
		if (!StringUtils.isEmpty(mrn) && !mrn.equalsIgnoreCase("null")){
			return adactaSearchService.getPersonnelFileByMRN(mrn);			
		}
		 
		return null;
	}
}