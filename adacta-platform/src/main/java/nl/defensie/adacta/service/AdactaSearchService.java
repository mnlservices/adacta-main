package nl.defensie.adacta.service;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaModel;

/**
 * Service specific for the Adacata searches.
 * 
 * @author Rick de Rooij
 *
 */
public class AdactaSearchService {

	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "SearchService");
	private static final String COMPANY_HOME = "company_home";

	protected Logger LOGGER = Logger.getLogger(this.getClass());

	public static final String QUERY_EXCLUDE_TYPES = " -TYPE:\"ada:document\" AND -TYPE:\"ada:dossier\"";

	public static final String QUERY_USER_EMPLOYEE_ID = "=TYPE:\"cm:person\" AND @ada\\:employeeID:\"%s\"";
	private static final String QUERY_NAME = String.format(
			"PATH:\"/app:company_home/st:sites/cm:%s//*\" AND @cm\\:name:\"%s\" AND (TYPE:\"ada:dossier\" OR TYPE:\"ada:document\" OR ASPECT:\"ada:scanAspect\")",
			ISO9075.encode(AdactaModel.PREFIX), "%s");
	private static final String QUERY_SCAN_CSV = "=TYPE:\"cm:content\" AND =@cm\\:name:\"%s\" AND =ASPECT:\"ada:scanAspect\" ";
	private static final String QUERY_SCAN_FOLDER = "=TYPE:\"cm:folder\" AND =@cm\\:name:\"%s\" AND =ASPECT:\"ada:scanAspect\" ";
	private static final String QUERY_SCAN_FOLDERS = String.format(
			"PATH:\"/app:company_home/st:sites/cm:%s/cm:documentLibrary/cm:Indexeer/*\" AND =TYPE:\"cm:folder\" AND =ASPECT:\"ada:scanAspect\" ",
			ISO9075.encode(AdactaModel.PREFIX), "%s");
	private static final String QUERY_INDEXREPORT2 = "=TYPE:\"cm:content\" AND =@cm\\:name:\"%s\"";
	private static final String QUERY_DOSSIER_BSN = "=TYPE:\"ada:dossier\" AND =@cm\\:name:\"%s\"";
	private static final String QUERY_DOSSIER_MRN = String.format(
			"PATH:\"/app:company_home/st:sites/cm:%s//*\" AND TYPE:\"ada:dossier\" AND @ada\\:employeeMrn:\"%s\"",
			ISO9075.encode(AdactaModel.PREFIX), "%s");

	private static final String QUERY_DOSSIER_EMPLOYEE_NUMBER = String.format(
			"PATH:\"/app:company_home/st:sites/cm:%s//*\" AND TYPE:\"ada:dossier\" AND @ada\\:employeeNumber:\"%s\"",
			ISO9075.encode(AdactaModel.PREFIX), "%s");
//	private static final String QUERY_DOCUMENTS_FOR_DELETE = String.format(
//			"PATH:\"/app:company_home/st:sites/cm:%s//*\" AND TYPE:\"ada:document\" AND ASPECT:\"ada:documentAspect\" AND @ada:docStatus:\"Gesloten\"",
//			ISO9075.encode(AdactaModel.PREFIX));

	private NodeRef dossierRoot = null;

	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	@Qualifier("SearchService")
	protected SearchService searchService;
	@Autowired
	@Qualifier("PermissionService")
	protected PermissionService permissionService;
	@Autowired
	@Qualifier("NamespaceService")
	protected NamespaceService namespaceService;
	@Autowired
	@Qualifier("PersonService")
	protected PersonService personService;

	/**
	 * Executes the search query and apply permission check before returning the
	 * results.
	 * 
	 * @param searchParameters SearchParameters the parameters for the search
	 * @param permission       String provided value can be Read, Write or Delete
	 * @return List<NodeRef> list of node refers with read access.
	 */
	public List<NodeRef> query(SearchParameters searchParameters, String permission) {
		// Execute search
		ResultSet resultSet = searchService.query(searchParameters);
		// Return empty list if there are no results
		if (resultSet.length() == 0) {
			return new ArrayList<NodeRef>();
		}
		// Define list to return
		List<NodeRef> nodeRefsAllowed = new ArrayList<NodeRef>();
		// Go through list and check for read permission
		List<NodeRef> nodeRefs = resultSet.getNodeRefs();
		for (NodeRef nodeRef : nodeRefs) {
			if (permissionService.hasPermission(nodeRef, permission) == AccessStatus.ALLOWED) {
				nodeRefsAllowed.add(nodeRef);
			}
		}
		return nodeRefsAllowed;
	}

	/**
	 * Get dossiers root where all personnel files are stored.
	 * 
	 * @return NodeRef the dossier root node reference.
	 */
	public NodeRef getDossiersRoot() {
		if (dossierRoot == null) {
			try {
				dossierRoot = getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_DOSSIERS);
			} catch (Exception e) {
				LOGGER.debug("dossiers root not found, SOLR may be down, will try save retrieve");
				dossierRoot = getDossiersRootSafe();
			}
		}
		return dossierRoot;
	}

	/**
	 * Get the root node of ADACTA dossiers without using queries. This will return
	 * a result even if SOLR is down.
	 * 
	 * @return
	 */
	private NodeRef getDossiersRootSafe() {
		NodeRef sites = null;
		NodeRef adacta = null;
		NodeRef companyHome = null;
		NodeRef documentLibrary = null;
		NodeRef rootNode = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		if (null != rootNode) {
			List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(rootNode);
			for (ChildAssociationRef c : childRefs) {
				if (c.getQName().toPrefixString().equalsIgnoreCase(COMPANY_HOME)) {
					companyHome = c.getChildRef();
				}
			}
		}
		if (null != companyHome) {
			sites = nodeService.getChildByName(companyHome, ContentModel.ASSOC_CONTAINS, "Sites");
		}
		if (null != sites) {
			adacta = nodeService.getChildByName(sites, ContentModel.ASSOC_CONTAINS, "adacta");
		}
		if (null != adacta) {
			documentLibrary = nodeService.getChildByName(adacta, ContentModel.ASSOC_CONTAINS, "documentLibrary");
		}
		if (null != documentLibrary) {
			return nodeService.getChildByName(documentLibrary, ContentModel.ASSOC_CONTAINS, "Dossiers");
		} else {
			return null;
		}
	}

	/**
	 * Executes the search query and apply read permission check before returning
	 * the results.
	 * 
	 * @param searchParameters SearchParameters the parameters for the search
	 * @return List<NodeRef> list of node refers with read access.
	 */
	public List<NodeRef> query(SearchParameters searchParameters) {
		return query(searchParameters, PermissionService.READ);
	}

	/**
	 * Get documents that have a C value next to the destroy code.
	 * 
	 * @param query
	 * @return ResultSet the query result.
	 */
	public ResultSet getDocumentsForDelete() {
		StringWriter sw = new StringWriter();
		sw.append("TYPE:");
		sw.append('"');
		sw.append("ada:document");
		sw.append('"');
		sw.append(" AND ");
		sw.append("=@ada:docStatus:");
		sw.append('"');
		sw.append("Gesloten");
		sw.append('"');
		return performSearch(sw.toString(), SearchService.LANGUAGE_FTS_ALFRESCO);
	}

	/**
	 * Get node reference based on type and aspect. Most used for root aspects.
	 * 
	 * @param type   QName the node type
	 * @param aspect QName the aspect
	 * @return NodeRef the node reference
	 */
	public NodeRef getNodeWithAspect(QName type, QName aspect) {
		String query = "TYPE:\"" + type.toPrefixString(namespaceService) + "\" AND ASPECT:\""
				+ aspect.toPrefixString(namespaceService) + "\" ";
		ResultSet resultSet = performSearch(query);
		if (resultSet.length() == 0) {
			return null;
		}
		return resultSet.getNodeRef(0);
	}

	public NodeRef getScanBatchFolderByName(String name) {
		String query = String.format(QUERY_SCAN_FOLDER, name);
		ResultSet resultSet = performSearch(query, SearchService.LANGUAGE_FTS_ALFRESCO, QueryConsistency.TRANSACTIONAL);
		if (resultSet.length() == 0) {
			return null;
		}
		return resultSet.getNodeRef(0);
	}

	public List<NodeRef> getScanBatchFolders() {
		List<NodeRef> scanfolders = new ArrayList<>();
		String query = QUERY_SCAN_FOLDERS;
		ResultSet resultSet = performSearch(query, SearchService.LANGUAGE_FTS_ALFRESCO,
				QueryConsistency.TRANSACTIONAL_IF_POSSIBLE);
		if (resultSet.length() == 0) {
			return scanfolders;
		} else {
			return resultSet.getNodeRefs();
		}
	}

	public NodeRef getIndexReport(String reportUuid) {
		String query = String.format(QUERY_INDEXREPORT2, reportUuid + ".html");
		ResultSet resultSet = performSearch(query, SearchService.LANGUAGE_FTS_ALFRESCO, QueryConsistency.TRANSACTIONAL);
		if (resultSet.length() == 0) {
			return null;
		} else {
			return resultSet.getNodeRef(0);
		}
	}

	public NodeRef getScanBatchCSVFileByName(String name) {
		String query = String.format(QUERY_SCAN_CSV, name);
		ResultSet resultSet = performSearch(query, SearchService.LANGUAGE_FTS_ALFRESCO, QueryConsistency.TRANSACTIONAL);
		if (resultSet.length() == 0) {
			return null;
		}
		return resultSet.getNodeRef(0);
	}

	/**
	 * Performs search based on query string. The search is based on lucene.
	 * 
	 * @param query
	 * @return resultSet of provided query string.
	 */
	public ResultSet performSearch(String query) {
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		return searchService.query(searchParameters);
	}

	public ResultSet performSearch(String query, String language) {
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		searchParameters.setLanguage(language);
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		return searchService.query(searchParameters);
	}

	public ResultSet performSearch(String query, String language, QueryConsistency qc) {
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		searchParameters.setQueryConsistency(qc);
		searchParameters.setLanguage(language);
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		return searchService.query(searchParameters);
	}
	//TODO add sort
	public ResultSet performSearch(String query, String language, QueryConsistency qc, QName sortby) {
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		searchParameters.setQueryConsistency(qc);
		searchParameters.setLanguage(language);
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		return searchService.query(searchParameters);
	}

	/**
	 * Get personnel file based on BSN.
	 * 
	 * @param bsn
	 * @return NodeRef the node reference or null
	 */
	public NodeRef getPersonnelFileByBSN(String bsn) {
		return getPersonnelFileByBSN(bsn, QueryConsistency.DEFAULT);
	}

	/**
	 * Get personnel file based on BSN, optionally transactionally consistent.
	 * 
	 * @param bsn
	 * @return NodeRef the node reference or null
	 */
	public NodeRef getPersonnelFileByBSN(String bsn, final QueryConsistency queryConsistency) {
		if (bsn.length() == 9) {
			bsn = "NLD-" + bsn;
		}

		final String query = String.format(QUERY_DOSSIER_BSN, bsn);
		LOGGER.debug("getPersonnelFileByBSN query=" + query);

		final SearchParameters sp = new SearchParameters();
		sp.setQuery(query);
		sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
		sp.setQueryConsistency(queryConsistency);
		sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		final ResultSet resultSet = searchService.query(sp);

		NodeRef result = null;
		if (resultSet != null && resultSet.length() > 0) {
			result = resultSet.getNodeRef(0);
		}
		return result;
	}

	/**
	 * Get personnel file based on BSN.
	 * 
	 * @param bsn
	 * @return NodeRef the node reference or null
	 */
	public NodeRef getPersonnelFileByMRN(String mrn) {
		NodeRef result = null;
		String query = String.format(QUERY_DOSSIER_MRN, mrn);

		ResultSet resultSet = performSearch(query);
		if (resultSet != null && resultSet.length() > 0) {
			result = resultSet.getNodeRef(0);
		}
		return result;
	}

	/**
	 * Get dossier or document by name.
	 * 
	 * @param name String file for folder name
	 * @return NodeRef the node reference.
	 */
	public NodeRef getDossierOrDocumentByName(String name) {
		NodeRef result = null;
		String query = String.format(QUERY_NAME, name);

		ResultSet resultSet = performSearch(query);
		if (resultSet != null && resultSet.length() > 0) {
			result = resultSet.getNodeRef(0);
		}
		return result;
	}

	/**
	 * Get personnel file based on employee number.
	 * 
	 * @param String employee number
	 * @return NodeRef the node reference or null
	 */
	public NodeRef getPersonnelFileByEmployeeNumber(String employeeNumber) {
		NodeRef result = null;
		String query = String.format(QUERY_DOSSIER_EMPLOYEE_NUMBER, employeeNumber);

		ResultSet resultSet = performSearch(query);
		if (resultSet != null && resultSet.length() > 0) {
			result = resultSet.getNodeRef(0);
		}
		return result;
	}

	/**
	 * Get person based on employeeID (employeeNumber). The employeeID is set on
	 * each user by the default LDAP synchronization.
	 * 
	 * @param employeeNumber String the id
	 * @return PersonInfo the person in Alfresco
	 */
	public PersonInfo getPerson(String employeeNumber) {
		PersonInfo result = null;

		String query = String.format(QUERY_USER_EMPLOYEE_ID, employeeNumber);
		ResultSet resultSet = performSearch(query, SearchService.LANGUAGE_LUCENE);

		// We expect only one result
		if (resultSet != null && resultSet.length() > 0) {
			NodeRef personRef = resultSet.getNodeRef(0);
			result = personService.getPerson(personRef);
		}
		return result;
	}

	/**
	 * Get QName path of given node.
	 * 
	 * @param nodeRef
	 * @return String path for search
	 */
	public String getQnamePath(final NodeRef nodeRef) {
		return AuthenticationUtil.runAsSystem(new RunAsWork<String>() {
			public String doWork() throws Exception {
				Path path = nodeService.getPath(nodeRef);
				return path.toPrefixString(namespaceService);
			}
		});
	}

	/**
	 * Gets the first node that can be found on the specified path.
	 * 
	 * @param xPath the qualified name path, starting from the root of the
	 *              spacesStore (like '/app:company_home/app:...').
	 * @return the first node that was found. null if the specified path does not
	 *         map to a node.
	 */
	public NodeRef getFirstNodeAtXPath(String xPath) {
		List<NodeRef> nodes = getNodesAtXPath(xPath);
		if (!nodes.isEmpty()) {
			return nodes.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Gets all nodes that can be found using the specified path.
	 * 
	 * @param xPath the qualified name path, starting from the root of the
	 *              spacesStore (like '/app:company_home/app:...').
	 * @return a list of nodes that were found.
	 */
	public List<NodeRef> getNodesAtXPath(String xPath) {
		ResultSet searchResults = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
				SearchService.LANGUAGE_XPATH, xPath);
		List<NodeRef> nodes = searchResults.getNodeRefs();
		return nodes;
	}
}