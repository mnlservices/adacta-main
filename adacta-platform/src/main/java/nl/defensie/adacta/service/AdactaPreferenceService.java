package nl.defensie.adacta.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.service.cmr.preference.PreferenceService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaModel;

/**
 * Service specific for managing specific preferences for Adacta.
 * 
 * @author Rick de Rooij
 *
 */
public class AdactaPreferenceService {

	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "PreferenceService");

	protected Logger LOGGER = Logger.getLogger(this.getClass());

	private static final String SELECTED_ITEMS_PREFERENCE_KEY = "nl.defensie.adacta.selecteditems.";

	@Autowired
	@Qualifier("PreferenceService")
	protected PreferenceService preferenceService;
	@Autowired
	@Qualifier("AuthenticationService")
	protected AuthenticationService authenticationService;

	/**
	 * Clear selected items for current (authenticated) user.
	 */
	public void clearSelectedItemsPreferences() {
		clearSelectedItemsPreferences(authenticationService.getCurrentUserName());
	}

	/**
	 * Clear selected items used in import and index context.
	 * 
	 * @param userName
	 *            String the user name
	 */
	public void clearSelectedItemsPreferences(String userName) {
		preferenceService.clearPreferences(userName, SELECTED_ITEMS_PREFERENCE_KEY);
	}

	/**
	 * Set selected items in preferences of provided user.
	 * 
	 * @param userName
	 *            String user name
	 * @param items
	 *            List<NodeRef> list of node references.
	 */
	public void setSelectedItemsPreferences(String userName, List<NodeRef> items) {
		Map<String, Serializable> preferences = new LinkedHashMap<String, Serializable>();
		//we need to create a map with a single entry: a comma-separated list of noderefs - we need to keep the ordering
		String prefs = "";
		if (items.size() > 0) {
			// First clear list
			clearSelectedItemsPreferences(userName);

			for (NodeRef item : items) {
				prefs = prefs +item.getId();
				prefs = prefs+";";
			}
			//remove last separator
			prefs = prefs.substring(0, prefs.length()-1);
			preferences.put(SELECTED_ITEMS_PREFERENCE_KEY, prefs);
		}

		preferenceService.setPreferences(userName, preferences);
	}

	/**
	 * Set selected items in preferences of current user.
	 * 
	 * @param items
	 *            List<NodeRef> list of node references.
	 */
	public void setSelectedItemsPreferences(List<NodeRef> items) {
		setSelectedItemsPreferences(authenticationService.getCurrentUserName(), items);
	}

	

	/**
	 * Get list of node references of selected items.
	 * 
	 * @param userName
	 * @return List<NodeRef> list of selected items or empty list
	 */
	public List<NodeRef> getSelectedItemsPreferences(String userName) {
		List<NodeRef> list = new ArrayList<NodeRef>();

		Map<String, Serializable> preferences = preferenceService.getPreferences(userName, SELECTED_ITEMS_PREFERENCE_KEY);
		
		if (preferences.size() == 0) {
			return list;
		}
		String nodeRefCsv = "";
		for (Entry<String, Serializable> entry : preferences.entrySet()) {
			Serializable value = entry.getValue();

			if (value instanceof String) {
				nodeRefCsv = (String) value;
			}
			String[] ids = nodeRefCsv.split(";");
			for (String id: ids){
				try {
					NodeRef n = new NodeRef("workspace://SpacesStore/"+id);
					list.add(n);
				} catch (Exception e) {
					LOGGER.error("id workspace://SpacesStore/"+id+" could not be transformed to NodeRef "+e.getMessage());
				}
			}
		}

		return list;
	}

	/**
	 * Get selected items based on current user.
	 * 
	 * @return List<NodeRef> list of node references of empty list
	 */
	public List<NodeRef> getSelectedItemsPreferences() {
		return getSelectedItemsPreferences(authenticationService.getCurrentUserName());
	}

	public Boolean hasSelectedItems(String userName) {
		List<NodeRef> items = getSelectedItemsPreferences();
		if (items.size() == 0) {
			return false;
		}
		return true;
	}

	public Boolean hasNextSelectedItem(String userName, NodeRef currentNodeRef) {
		List<NodeRef> items = getSelectedItemsPreferences(userName);

		int index = 0;
		for (NodeRef item : items) {
			if (item.equals(currentNodeRef)) {
				break;
			}
			index++;
		}
		
		if (index == items.size() - 1) {
			return false;
		}
		return true;
	}

	public Boolean hasPreviousSelectedItem(String userName, NodeRef currentNodeRef) {
		List<NodeRef> items = getSelectedItemsPreferences(userName);

		int index = 0;
		for (NodeRef item : items) {
			if (item.equals(currentNodeRef)) {
				break;
			}
			index++;
		}

		if (index == 0) {
			return false;
		}
		return true;
	}
}