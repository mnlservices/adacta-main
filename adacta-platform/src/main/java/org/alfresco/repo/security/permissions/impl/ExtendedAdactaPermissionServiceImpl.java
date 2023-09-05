package org.alfresco.repo.security.permissions.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.collections.CollectionUtils;

import net.sf.acegisecurity.Authentication;
import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaAuthorityService;
import nl.defensie.adacta.util.ReadProperties;

/**
 * Extends the permission service for managing the permissions on folders and documents for Adacta.
 * 
 * @author Rick de Rooij
 *
 */
public class ExtendedAdactaPermissionServiceImpl extends PermissionServiceImpl {

	// private static final Log LOGGER = LogFactory.getLog(ExtendedAdactaPermissionServiceImpl.class);

	private Boolean permissionsEnabled = false;

	private HashMap<String, String> categoryAuthorityMap;
	private List<String> groupsWithDpallPermissionsMap;
	private List<String> catgoriesAccessibleByOwnerMap;

	protected PersonService personService;

	@Override
	public AccessStatus hasPermission(NodeRef nodeRef, PermissionReference permIn) {
		// Check if it is enabled
		if (permissionsEnabled == false) {
			return super.hasPermission(nodeRef, permIn);
		}

		// Make sure we deal with the adacta types
		if (nodeService.exists(nodeRef) && !nodeService.getType(nodeRef).equals(AdactaModel.TYPE_DOCUMENT) && !nodeService.getType(nodeRef).equals(AdactaModel.TYPE_DOSSIER)) {
			return super.hasPermission(nodeRef, permIn);
		}

		// We don't have to deal with write/delete permissions.
		if (permIn.equals(getPermissionReference(WRITE)) || permIn.equals(getPermissionReference(DELETE))) {
			return super.hasPermission(nodeRef, permIn);
		}

		// Get the person reference
		String userName = AuthenticationUtil.getRunAsUser();
		NodeRef personRef = personService.getPerson(userName);

		// Validate document type
		if (nodeService.exists(nodeRef) && nodeService.getType(nodeRef).equals(AdactaModel.TYPE_DOCUMENT)) {
			return hasDocumentPermission(nodeRef, personRef, permIn);
		}

		// Validate personnel file (dossier) type
		if (nodeService.exists(nodeRef) && nodeService.getType(nodeRef).equals(AdactaModel.TYPE_DOSSIER)) {
			return hasPersonnelFilePermission(nodeRef, personRef, permIn);
		}

		// If we are here, use also the default permission mechanism
		return super.hasPermission(nodeRef, permIn);
	}

	/**
	 * Validates the document permissions against the category value (ada:docCategory).
	 * 
	 * @param nodeRef
	 *            NodeRef the node reference
	 * @param permIn
	 *            the permission reference
	 * @return AccessStatus allow or denied access
	 */
	private AccessStatus hasDocumentPermission(NodeRef nodeRef, NodeRef personRef, PermissionReference permIn) {
		// Administrator can view all files
		if (adminRead() == AccessStatus.ALLOWED) {
			return AccessStatus.ALLOWED;
		}

		// We need a status
		String docStatus = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_STATUS);
		if (docStatus == null) {
			return AccessStatus.DENIED;
		}

		// If you query only for documents, we need to check the permission on the parent folder (dossier) also.
		NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
		AccessStatus personnelFileAccessStatus = hasPersonnelFilePermission(parentRef, personRef, permIn);
		if (nodeService.getType(parentRef).equals(AdactaModel.TYPE_DOSSIER) && personnelFileAccessStatus == AccessStatus.DENIED) {
			return AccessStatus.DENIED;
		} else if (nodeService.getType(parentRef).equals(AdactaModel.TYPE_DOSSIER) && personnelFileAccessStatus == AccessStatus.ALLOWED && isOwnerPersonnelFile(nodeRef, personRef)
				&& isAccessibleCategory(nodeRef)) {
			return AccessStatus.ALLOWED;
		}

		// Get the authorisations
		Authentication auth = AuthenticationUtil.getRunAsAuthentication();
		final Set<String> authorisations = getAuthorisations(auth, nodeRef, permIn);

		// If document is closed only the adacta admin can view it
		if (docStatus.contains(AdactaModel.LIST_STATUS_GESLOTEN) && authorisations.contains(AdactaAuthorityService.GROUP_ADACTA_BEHEERDER)) {
			return AccessStatus.ALLOWED;
		}

		// Get category (Rubriek)
		String docCategory = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_CATEGORY);

		// Use default permissions
		if (docCategory == null) {
			return super.hasPermission(nodeRef, permIn);
		}

		// Validate category with authority and it's only for active documents
		if (authorisations.contains(getCategoryAuthorityMap().get(docCategory)) && docStatus.contains(AdactaModel.LIST_STATUS_ACTIEF)) {
			return AccessStatus.ALLOWED;
		}

		// Sorry, no access :(
		return AccessStatus.DENIED;
	}

	/**
	 * Validates the users dpcode against the list of dpcodes stored on the folder (dossier). If dpcode consist in list, the users is granted access.
	 * 
	 * @param nodeRef
	 *            NodeRef the node reference of dossier
	 * @param permIn
	 *            PermissionReference the permission reference
	 * @return AccessStatus allow or denied access
	 */
	private AccessStatus hasPersonnelFilePermission(NodeRef nodeRef, NodeRef personRef, PermissionReference permIn) {
		// Administrator can view all files
		if (adminRead() == AccessStatus.ALLOWED) {
			return AccessStatus.ALLOWED;
		}

		// User can always see his/her own personnel file
		if (isOwnerPersonnelFile(nodeRef, personRef)) {
			return AccessStatus.ALLOWED;
		}

		// Get dpcode of user
		String dpCode = (String) nodeService.getProperty(personRef, AdactaModel.PROP_DP_CODE);

		// We cannot determine authority without dpcode
		if (dpCode == null) {
			return AccessStatus.DENIED;
		}

		// TODO: This is a temporary solution in which we grant DPALL permissions for DPEXMIVD employees. When DP codes are organized properly (i.e. DPMEDISCH), you can delete this block of code.
		if (dpCode.contains("DPEXMIVD")) {
			Authentication auth = AuthenticationUtil.getRunAsAuthentication();
			final Set<String> authorisations = getAuthorisations(auth, nodeRef, permIn);

			// Check if any group exists in authorization list.
			if (CollectionUtils.containsAny(authorisations, groupsWithDpallPermissionsMap)) {
				dpCode = "DPALL";
			}
		}

		@SuppressWarnings("unchecked")
		ArrayList<String> dpCodes = (ArrayList<String>) nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_DP_CODES);

		// We cannot determine authority without a list of dpcodes
		if (dpCodes == null) {
			return AccessStatus.DENIED;
		}

		// We only allow access if dpcode exists in list of dpcodes
		if (dpCodes.contains(dpCode)) {
			return AccessStatus.ALLOWED;
		}

		// Sorry, no access :(
		return AccessStatus.DENIED;
	}

	/**
	 * Validates if authenticated user is looking at his/her file or personnel file.
	 * 
	 * @param nodeRef
	 *            NodeRef document reference or personell file reference
	 * @param personRef
	 *            NodeRef person reference
	 * @return Boolean true iff employee number on document matches the employee ID of authenticated user.
	 */
	private Boolean isOwnerPersonnelFile(NodeRef nodeRef, NodeRef personRef) {
		// Can this be null?
		if (personRef == null) {
			return false;
		}

		// Employee number is both available on folder and document
		String employeeID = (String) nodeService.getProperty(personRef, AdactaModel.PROP_EMPLOYEE_ID);
		String employeeNumber = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_NUMBER);
		if (employeeID != null && employeeNumber != null && employeeNumber.contains(employeeID)) {
			return true;
		}
		return false;
	}

	/**
	 * Validates if category on document is accessible by user.
	 * 
	 * @param nodeRef
	 *            NodeRef the node reference
	 * @return Boolean true if category matches the list of accessible categories.
	 */
	private Boolean isAccessibleCategory(NodeRef nodeRef) {
		String categoryCode = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_CATEGORY);
		if (categoryCode != null && getCategoriesAccessibleByOwnerMap().contains(categoryCode)) {
			return true;
		}
		return false;
	}

	public List<String> getCategoriesAccessibleByOwnerMap() {
		if (catgoriesAccessibleByOwnerMap == null){
			catgoriesAccessibleByOwnerMap = ReadProperties.getPropsInArray("alfresco/extension/messages/categories-accessible-by-owner");
		}

		return catgoriesAccessibleByOwnerMap;
	}

	public HashMap<String, String> getCategoryAuthorityMap() {
		if (categoryAuthorityMap == null){
			categoryAuthorityMap = ReadProperties.getPropsInHashMap("alfresco/extension/messages/category-authority");
		}
		return categoryAuthorityMap;
	}

	public Boolean getPermissionsEnabled() {
		return permissionsEnabled;
	}

	public void setPermissionsEnabled(Boolean permissionsEnabled) {
		this.permissionsEnabled = permissionsEnabled;
	}

	public PersonService getPersonService() {
		return personService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public List<String> getGroupsWithDpallPermissionsMap() {
		return groupsWithDpallPermissionsMap;
	}

	public void setGroupsWithDpallPermissionsMap(List<String> groupsWithDpallPermissionsMap) {
		this.groupsWithDpallPermissionsMap = groupsWithDpallPermissionsMap;
	}
}