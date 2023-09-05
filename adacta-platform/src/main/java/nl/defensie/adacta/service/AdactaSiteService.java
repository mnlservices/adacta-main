package nl.defensie.adacta.service;

import java.util.HashMap;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.model.DataListModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.util.ReadProperties;

/**
 * Service specific for the Adacta site configuration.
 * 
 * @author Rick de Rooij
 *
 */
public class AdactaSiteService {

	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "SiteService");

	protected Logger LOGGER = Logger.getLogger(this.getClass());

	public static final String DATA_LIST_CONTAINER = "dataLists";

	private HashMap<String, String> categoryAuthorityMap;

	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	@Qualifier("SiteService")
	protected SiteService siteService;
	@Autowired
	@Qualifier("FileFolderService")
	protected FileFolderService fileFolderService;
	@Autowired
	@Qualifier("NamespaceService")
	protected NamespaceService nameSpaceService;
	@Autowired
	protected AdactaAuthorityService adactaAuthorityService;

	/**
	 * Gets site information of the Adacta site.
	 * 
	 * @return SiteInfo the site or null if site is not present
	 */
	public SiteInfo getSite() {
		return siteService.getSite(AdactaModel.PREFIX);
	}

	/**
	 * Get the document library of the Adacata site
	 * 
	 * @return NodeRef the root of document library.
	 */
	public NodeRef getSiteDocumentLibrary() {
		SiteInfo site = getSite();
		NodeRef container = siteService.getContainer(site.getShortName(), SiteService.DOCUMENT_LIBRARY);
		if (container == null) {
			container = siteService.createContainer(site.getShortName(), SiteService.DOCUMENT_LIBRARY, ContentModel.TYPE_FOLDER, null);
		}
		return container;
	}

	/**
	 * Get data list container of Adacta site.
	 * 
	 * @return NodeRef data list container.
	 */
	public NodeRef getDataListContainer() {
		String siteShortName = getSite().getShortName();
		return getDataListContainer(siteShortName);
	}

	/**
	 * Get data list container by providing the site short name.
	 * 
	 * @param siteShortName
	 *            String unique shortname
	 * @return NodeRef datalist container
	 */
	public NodeRef getDataListContainer(String siteShortName) {
		NodeRef datalistContainer = siteService.getContainer(siteShortName, DATA_LIST_CONTAINER);
		if (datalistContainer == null) {
			datalistContainer = fileFolderService.create(siteService.getSite(siteShortName).getNodeRef(), DATA_LIST_CONTAINER, ContentModel.TYPE_FOLDER).getNodeRef();
		}
		return datalistContainer;
	}

	/**
	 * Get the root of the data list.
	 * 
	 * @param siteShortName
	 *            the site
	 * @param datalistItemTypeLocalName
	 * @return NodeRef of datalist
	 */
	public NodeRef getDataListRoot(String siteShortName, String datalistItemTypeLocalName) {
		if (siteService.getSite(siteShortName) == null) {
			LOGGER.warn(String.format("Site %s could not be found.", siteShortName));
			return null;
		}
		NodeRef datalistContainer = getDataListContainer(siteShortName);
		for (ChildAssociationRef assoc : nodeService.getChildAssocs(datalistContainer)) {
			NodeRef child = assoc.getChildRef();
			QName itemType = QName.createQName((String) nodeService.getProperty(child, DataListModel.PROP_DATALIST_ITEM_TYPE), nameSpaceService);
			if (itemType.getLocalName().equals(datalistItemTypeLocalName)) {
				return child;
			}
		}
		LOGGER.warn("Could not find any datalisttype: " + datalistItemTypeLocalName.toString());
		return null;
	}

	/**
	 * Determine to add category to list.
	 * 
	 * @param userGroups
	 *            Set<String> list of groups
	 * @param userName
	 *            String user name
	 * @param value
	 *            String category value
	 * @return Boolean true if user is member of category group
	 */
	public Boolean addCategoryToList(Set<String> userGroups, String userName, String value) {
		if (adactaAuthorityService.isUserAdactaAdministrator(userName)) {
			return true;
		}
		if (userGroups.contains(getCategoryAuthorityMap().get(value))) {
			return true;
		}
		return false;
	}

	/**
	 * Determine to add category to list based on group membership.
	 * 
	 * @param userName
	 *            String user name
	 * @param value
	 *            String value of doc category
	 * @return Boolean true is user has group that equals doc category.
	 * @deprecated Because of extremely poor naming. Use {@link #hasCategoryAccess(String, String)} instead.
	 */
	public Boolean addCategoryToList(String userName, String value) {
		return addCategoryToList(adactaAuthorityService.getAuthoritiesForUserAsSystem(userName), userName, value);
	}
	
	/**
	 * <p>Determine if given <b>userName</b> has access to given <b>categoryCode</b>.
	 * <p>A user has access to categories of which 
	 * he is a member of the categories' corresponding access-control user group.</p>
	 * @param userName user to check access of
	 * @param categoryCode category to check access to
	 * @return
	 */
	public boolean hasCategoryAccess(String userName, String categoryCode) {
		return addCategoryToList(adactaAuthorityService.getAuthoritiesForUserAsSystem(userName), userName, categoryCode);
	}

	public HashMap<String, String> getCategoryAuthorityMap() {
		if (categoryAuthorityMap == null){
			categoryAuthorityMap = ReadProperties.getPropsInHashMap("alfresco/extension/messages/category-authority");
		}
		return categoryAuthorityMap;
	}

}