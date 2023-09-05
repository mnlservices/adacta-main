package nl.defensie.adacta.service;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.RoleAuthoritiesBean;

//bij toevoegen: extra checks uit te voeren: 
//PPATT01 niet toevoegen als niet tegelijk F06,F07 of F08 (Batchklassen)
//ZZPPBWX01 : alleen DCHR 
//PPBSG01 : alleen F05
//bij verwijderen: excluden adacta beheerders

/**
 * Performs a number of checks on the outcome of the roles sync job.
 * 
 * @author wim.schreurs
 *
 */
public class AdactaRoleSyncChecksService {

	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "AdactaRoleSyncChecksService");
	public RoleAuthoritiesBean roleAuthoritiesBean;

	/**
	 * Rollen van ADACTA beheerders mogen niet verwijderd worden - ADACTA deelt deze
	 * rol zelf uit.
	 * 
	 * @param sortedRemoveFromLdap
	 * @param roleAuthorities      - represents the configuration file
	 * @return
	 */
	public List<String> excludeManagers(List<String> sortedRemoveFromLdap, RoleAuthoritiesBean roleAuthorities) {
		roleAuthoritiesBean = roleAuthorities;
		List<String> cleanedList = new ArrayList<>();
		List<String> managers = roleAuthorities.getBeheerders();
		for (String removeRole : sortedRemoveFromLdap) {
			boolean manager_found = false;
			for (String manager : managers) {
				if (removeRole.contains(manager)) {
					manager_found = true;
				}
			}
			if (!manager_found) {
				cleanedList.add(removeRole);
			}
		}
		return cleanedList;
	}

	/**
	 * Voeg PPATT01 (kofax) alleen toe als er ook nog andere authorisaties worden
	 * toegevoegd.
	 * 
	 * @param sortedAddToLdap - format role_authority
	 * @return
	 */
	public List<String> removeSingleKofaxAuthorities(List<String> sortedAddToLdap) {
		List<String> cleanedList = new ArrayList<>();
		for (String e : sortedAddToLdap) {
			String role = getRole(e);
			if (e.endsWith("PPATT01") && (countEntriesForRole(role, sortedAddToLdap) == 1)) {
				// do not add
			} else {
				cleanedList.add(e);
			}

		}
		return cleanedList;
	}

	private int countEntriesForRole(String role, List<String> sortedAddToLdap) {
		int counter = 0;
		for (String e : sortedAddToLdap) {
			if (e.contains(role)) {
				counter++;
			}
		}
		return counter;
	}

	private String getRole(String rolx) {
		if (null == rolx) {
			return "";
		} else {
			String[] r = rolx.split("_");
			return r[0];
		}
	}

}