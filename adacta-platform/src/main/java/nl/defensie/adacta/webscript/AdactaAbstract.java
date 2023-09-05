package nl.defensie.adacta.webscript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.i18n.MessageService;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.service.ServiceDescriptorRegistry;
import org.alfresco.repo.template.TemplateNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.SortDefinition;
import org.alfresco.service.cmr.search.SearchParameters.SortDefinition.SortType;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.alfresco.util.ModelUtil;
import org.alfresco.util.UrlUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaAuthorityService;
import nl.defensie.adacta.service.AdactaDataListService;
import nl.defensie.adacta.service.AdactaFileFolderService;
import nl.defensie.adacta.service.AdactaPreferenceService;
import nl.defensie.adacta.service.AdactaSearchService;
import nl.defensie.adacta.service.AdactaSiteService;
import nl.defensie.adacta.service.AdactaSortService;

/**
 * Abstract for all Adacta webscripts functions.
 * 
 * @author Rick de Rooij
 *
 */
public abstract class AdactaAbstract extends DeclarativeWebScript {

	protected static final Log LOGGER = LogFactory.getLog(AdactaAbstract.class);	
	
	public static final String PARAM_START_INDEX = "startIndex"; // page size
	public static final String PARAM_PAGE_SIZE = "pageSize"; // max items
	public static final String PARAM_FILTER = "filter";
	public static final String PARAM_SORT_BY = "sortBy";
	public static final String PARAM_DIR = "dir";
	public static final String PARAM_SEARCH_TYPE = "searchType";
	public static final String PARAM_NODEREF = "nodeRef";
	public static final String PARAM_USER_NAME = "userName";
	public static final String PARAM_QUERY = "query";
	
	// Data lists
	public static final String PARAM_DATALIST_LOCALNAME = "datalistLocalname";
	public static final String PARAM_CURRENT_VALUE = "current_value";

	private static final String DEFAULT_QUERY = String.format("PATH:\"/app:company_home/st:sites/cm:%s//*\" AND (TYPE:\"ada:dossier\" OR TYPE:\"ada:document\")",
			ISO9075.encode(AdactaModel.PREFIX));
	
	private static final String TYPE_DOCUMENT_QUERY = "=TYPE:\"ada:document\"";

	@Autowired
	@Qualifier("AuthorityService")
	protected AuthorityService authorityService;
	@Autowired
	@Qualifier("ActionService")
	protected ActionService actionService;
	@Autowired
	@Qualifier("AuthenticationService")
	protected AuthenticationService authenticationService;
	@Autowired
	@Qualifier("ServiceRegistry")
	protected ServiceRegistry serviceRegistry;
	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	@Qualifier("NamespaceService")
	protected NamespaceService namespaceService;
	@Autowired
	@Qualifier("SearchService")
	protected SearchService searchService;
	@Autowired
	@Qualifier("PersonService")
	protected PersonService personService;
	@Autowired
	@Qualifier("SiteService")
	protected SiteService siteService;
	@Autowired
	@Qualifier("OwnableService")
	protected OwnableService ownableService;
	@Autowired
	@Qualifier("PermissionService")
	protected PermissionService permissionService;
	@Autowired
	protected AdactaSortService adactaSortService;
	@Autowired
	protected AdactaAuthorityService adactaAuthorityService;
	@Autowired
    protected AdactaFileFolderService adactaFileFolderService;
	@Autowired
	protected AdactaSearchService adactaSearchService;
	@Autowired
	protected AdactaPreferenceService adactaPreferenceService;
	@Autowired
	protected AdactaSiteService adactaSiteService;
	@Autowired
	protected AdactaDataListService adactaDataListService;
	@Autowired
	@Qualifier("ServiceRegistry")
	protected ServiceDescriptorRegistry serviceDescriptorRegistry;
	@Autowired
	protected Repository repository;
	@Autowired
	@Qualifier("MessageService")
	protected MessageService messageService;

	/**
	 * This static map is for getting the sorting property
	 */
	static final HashMap<String, QName> sortPropMapping = new HashMap<String, QName>();

	{
		{
			sortPropMapping.put("name", ContentModel.PROP_NAME);
			sortPropMapping.put("title", ContentModel.PROP_TITLE);
			sortPropMapping.put("description", ContentModel.PROP_DESCRIPTION);
			sortPropMapping.put("creator", ContentModel.PROP_CREATOR);
			sortPropMapping.put("modifier", ContentModel.PROP_MODIFIER);
			sortPropMapping.put("modified", ContentModel.PROP_MODIFIED);
			sortPropMapping.put("created", ContentModel.PROP_CREATED);
			sortPropMapping.put("owner", ContentModel.PROP_OWNER);

			sortPropMapping.put("employeeNumber", AdactaModel.PROP_EMPLOYEE_NUMBER);
			sortPropMapping.put("employeeName", AdactaModel.PROP_EMPLOYEE_NAME);
			sortPropMapping.put("employeeBsn", AdactaModel.PROP_EMPLOYEE_BSN);
			sortPropMapping.put("employeeMrn", AdactaModel.PROP_EMPLOYEE_MRN);
			sortPropMapping.put("employeeDepartment", AdactaModel.PROP_EMPLOYEE_DEPARTMENT);

			sortPropMapping.put("docCategory", AdactaModel.PROP_DOC_CATEGORY);
			sortPropMapping.put("docSubject", AdactaModel.PROP_DOC_SUBJECT);
			sortPropMapping.put("docDate", AdactaModel.PROP_DOC_DATE);
			sortPropMapping.put("docReference", AdactaModel.PROP_DOC_REFERENCE);
			sortPropMapping.put("docWorkDossier", AdactaModel.PROP_DOC_WORK_DOSSIER);
			sortPropMapping.put("docCaseNumber", AdactaModel.PROP_DOC_CASE_NUMBER);
			sortPropMapping.put("docDateCreated", AdactaModel.PROP_DOC_DATE_CREATED);
			sortPropMapping.put("docMigId", AdactaModel.PROP_DOC_MIG_ID);
			sortPropMapping.put("docMigDate", AdactaModel.PROP_DOC_MIG_DATE);
			sortPropMapping.put("docStatus", AdactaModel.PROP_DOC_STATUS);

			sortPropMapping.put("scanEmployee", AdactaModel.PROP_SCAN_EMPLOYEE);
			sortPropMapping.put("scanSeqNr", AdactaModel.PROP_SCAN_SEQ_NR);
			sortPropMapping.put("scanWaNr", AdactaModel.PROP_SCAN_WA_NR);
		}
	};

	protected QName getSortProperty(String propName) {
		return sortPropMapping.get(propName);
	}

	/**
	 * Do search for file or folders and search in all possible props.
	 * 
	 * @param req
	 * @return List<NodeRef> the results of query
	 */
	protected List<NodeRef> doFileFolderSearch(WebScriptRequest req) {
		// get the filter
		String filter = req.getParameter(PARAM_FILTER);
		if (filter == null) {
			filter = "";
		}

		// create query
		String query = DEFAULT_QUERY;
		query += buildQuery(filter);

		// Build search
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		LOGGER.debug("doFileFolder query="+query);
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.setSkipCount(0);
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

		String sort = getSort(req);
		if (sort != null) {
			searchParameters.addSort(sort, getDir(req));
		}
		return adactaSearchService.query(searchParameters);
	}

	/**
	 * Get all batches in the index folder.
	 * 
	 * @param req
	 * @return List<NodeRef> the results of the query.
	 */
	protected List<NodeRef> doIndexFolderSearch(WebScriptRequest req) {
		// Get the filter
		String filter = req.getParameter(PARAM_FILTER);
		if (filter == null) {
			filter = "";
		}

		// Create query
		String query = DEFAULT_QUERY;

		// Check if provide node ref is available
		String paramNodeRef = req.getParameter(PARAM_NODEREF);
		if (paramNodeRef != null) {
			NodeRef nodeRef = new NodeRef(paramNodeRef);
			//gebruik hier geen PATH query - laat na koppelen items zien die niet meer in de folder zitten
			return getScanBatchChildren(nodeRef);
			//query = "+PATH:\"" + adactaSearchService.getQnamePath(nodeRef) + "//*\" AND (TYPE:\"cm:content\" || TYPE:\"ada:document\")";
		} else {
			NodeRef nodeRef = adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_INDEX);
			query = "+PATH:\"" + adactaSearchService.getQnamePath(nodeRef) + "//*\" +TYPE:\"cm:folder\" -TYPE:\"cm:systemfolder\" +ASPECT:\"ada:scanAspect\" ";

			// Append additional search params
			query += buildQuery(filter);
		}

		// Build search
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.setSkipCount(0);
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

		String sort = getSort(req);
		if (sort != null) {
			searchParameters.addSort(sort, getDir(req));
		}
		List<NodeRef> nodes =  adactaSearchService.query(searchParameters);
		//nodes = adactaSortService.doSort(nodes, sort, getDirection(req));
		return nodes;
	}

	private List<NodeRef> getScanBatchChildren(NodeRef nodeRef) {
		List<NodeRef> items = new ArrayList<>();
		List<ChildAssociationRef> children = nodeService.getChildAssocs(nodeRef);
		Iterator<ChildAssociationRef> i = children.iterator();
		while (i.hasNext()) {
			ChildAssociationRef ref = i.next();
			NodeRef child = ref.getChildRef();

			if (nodeService.getType(child).equals(ContentModel.TYPE_CONTENT) || nodeService.getType(child).equals(AdactaModel.TYPE_DOCUMENT)) {
				items.add(child);
			}
		}
		return items;
	}

	/**
	 * Get dossiers or documents in the dossiers folder, based on input query or parent noderef.
	 * 
	 * @param req
	 * @return List<NodeRef> the results of the query.
	 */
	protected List<NodeRef> doSearch(WebScriptRequest req) {
		// Create query
		String query = DEFAULT_QUERY;
		boolean documentSearch = false;
		// Check if provided noderef is available
		String paramNodeRef = req.getParameter(PARAM_NODEREF);
		NodeRef dossierNodeRef = null;
		if (paramNodeRef != null) {
			dossierNodeRef = new NodeRef(paramNodeRef);
			query = "PATH:\"" + adactaSearchService.getQnamePath(dossierNodeRef) + "//*\" AND TYPE:\"ada:document\" ";
		} else {
			query = req.getParameter(PARAM_QUERY);
		}
		
		if (query.contains("ada:document")){
			documentSearch=true;
		}
		// Build search
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		LOGGER.debug("doSearch query="+query);
		searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
		searchParameters.setSkipCount(0);
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

		boolean selfservice = Boolean.parseBoolean(req.getParameter("selfservice"));
		String sort = getSort(req);
		List<NodeRef> nodes  = adactaSearchService.query(searchParameters);

		if (sort != null) {
			nodes = adactaSortService.doSort(nodes, sort, getDirection(req));
		} else {
			if (selfservice){
				nodes = adactaSortService.selfServiceDefaultSearch(nodes);
			}else{
				nodes = adactaSortService.raadplegenDefaultSearch(nodes);
			}
		}

		if (documentSearch){
			nodes = checkPermissions(nodes, selfservice);
		}
		return nodes;
	}

	/**
	 * When searching for documents in 'searchPage' mode, we have to make sure that we only show active documents
	 * for which currentUser is authorized through category permissions in his userprofile.
	 * Selfservice users however can see their own documents regardless of category access in the userprofile.
	 * Also, documents marked for removal must only be shown to managers.
	 *
	 * @param docNodes
	 * @return
	 */
	private List<NodeRef> checkPermissions(List<NodeRef> docNodes, boolean selfservice) {
		List<NodeRef> newList = new ArrayList<>();
		for (NodeRef docnodeRef : docNodes) {
			String docCategory = (String) nodeService.getProperty(docnodeRef, AdactaModel.PROP_DOC_CATEGORY);
			if (StringUtils.isEmpty(docCategory)) {
				continue;
			}
			if (selfservice) {
				if (hasDocumentAccess(docnodeRef)) {
					newList.add(docnodeRef);
				}
			} else {
				if (adactaSiteService.hasCategoryAccess(authenticationService.getCurrentUserName(), docCategory)
						&& hasDocumentAccess(docnodeRef)) {
					newList.add(docnodeRef);
				}
			}
		}
		return newList;
	}
	/**
	 * Returns true if document status is Active or document status is Closed and user is an administrator. 
	 * @param docnodeRef
	 * @return
	 */
	protected boolean hasDocumentAccess(NodeRef docnodeRef){
		boolean userIsAdactaAdmin = adactaAuthorityService.isUserAdactaAdministrator(authenticationService.getCurrentUserName());
		//admin always has access
		if (userIsAdactaAdmin){
			return true;
		}
		String docStatus = (String) nodeService.getProperty(docnodeRef, AdactaModel.PROP_DOC_STATUS);
		if (StringUtils.isEmpty(docStatus)){
			return true;
		}
		if (docStatus.equalsIgnoreCase(AdactaModel.LIST_STATUS_GESLOTEN)){
			return false;
		}else{
			return true;
		}
	}
	/**
	 * Get dossiers or documents in the dossiers folder, based on input query or parent noderef.
	 * 
	 * @param req
	 * @return List<NodeRef> the results of the query.
	 */
	protected List<NodeRef> doOverviewDeleteRequestsSearch(WebScriptRequest req) {
		// Create query
		String query = TYPE_DOCUMENT_QUERY;
		query += " AND (=@ada\\:docWorkDossier:\"V1\""
				+ " OR =@ada\\:docWorkDossier:\"V2\""
				+ " OR =@ada\\:docWorkDossier:\"V3\""
				+ " OR =@ada\\:docWorkDossier:\"V4\""
				+ " OR =@ada\\:docWorkDossier:\"V5\""
				+ " OR =@ada\\:docWorkDossier:\"V6\""
				+ " OR =@ada\\:docWorkDossier:\"V7\""
				+ " OR =@ada\\:docWorkDossier:\"V8\""
				+ ")";

		// Build search
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
		searchParameters.setSkipCount(0);
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

		String sort = getSort(req);
		if (sort != null) {
			searchParameters.addSort(sort, getDir(req));
		}
		return adactaSearchService.query(searchParameters);
	}

	/**
	 * Get dossiers or documents in the dossiers folder, based on input query or parent noderef.
	 * 
	 * @param req
	 * @return List<NodeRef> the results of the query.
	 */
	protected List<NodeRef> doOverviewIndexReportsSearch(WebScriptRequest req) {

		// Create query
		String query = DEFAULT_QUERY;

		NodeRef nodeRef = adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_REPORT);
		if (nodeRef != null) {
			query = "+PATH:\"" + adactaSearchService.getQnamePath(nodeRef) + "//*\" +TYPE:\"cm:content\" ";
		}

		// If user is not adacta admin, include user name
		String userName = authenticationService.getCurrentUserName();
		if (!adactaAuthorityService.isUserAdactaAdministrator(userName)) {
			query += "+@cm\\:creator:\"" + userName + "\"";
		}

		// Build search
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.setSkipCount(0);
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

		String sort = getSort(req);
		if (sort != null) {
			searchParameters.addSort(sort, getDir(req));
		} else {
			searchParameters.addSort(ContentModel.PROP_MODIFIED.toString(), false);
		}
		//exclude non html files
		List<NodeRef> s = adactaSearchService.query(searchParameters);
		List<NodeRef> finalList = new ArrayList<>();
		for (NodeRef ref : s){
			String name = (String) nodeService.getProperty(ref, ContentModel.PROP_NAME);
			if (name.endsWith(".html")){
				finalList.add(ref);
			}
		}
		return finalList;
	}

	/**
	 * Get the list of unprocessed batches and import files.
	 * 
	 * @param req
	 * @return List<NodeRef> the results of the query.
	 */
	protected List<NodeRef> doOverviewUnprocessedFolderFilesSearch(WebScriptRequest req) {
		// Create query
		String query = DEFAULT_QUERY;

		NodeRef importRootRef = adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_IMPORT);
		NodeRef indexRootRef = adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_INDEX);

		if (importRootRef != null && indexRootRef != null) {
			query = "-TYPE:\"cm:systemfolder\" AND ((+PATH:\"" + adactaSearchService.getQnamePath(importRootRef) + "//*\" +TYPE:\"cm:content\")";
			query += " OR (+PATH:\"" + adactaSearchService.getQnamePath(indexRootRef) + "//*\" +TYPE:\"cm:folder\"))";
		}

		// Build search
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.setSkipCount(0);
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

		String sort = getSort(req);
		if (sort != null) {
			searchParameters.addSort(sort, getDir(req));
		} else {
			searchParameters.addSort(ContentModel.PROP_MODIFIED.toString(), false);
		}
		return adactaSearchService.query(searchParameters);
	}

	/**
	 * Appy filter to all existing properties.
	 * 
	 * @param filter
	 * @return String query filter
	 */
	private String buildQuery(String filter) {
		StringBuilder sb = new StringBuilder();
		sb.append(" AND (");
		sb.append("@ada\\:employeeNumber:\"" + filter + "*\" OR ");
		sb.append("@ada\\:employeeName:\"" + filter + "*\" OR ");
		sb.append("@ada\\:employeeBsn:\"" + filter + "*\" OR ");
		sb.append("@ada\\:employeeMrn:\"" + filter + "*\" OR ");
		sb.append("@ada\\:employeeDepartment:\"" + filter + "*\" OR ");

		sb.append("@ada\\:docReference:\"" + filter + "*\" OR ");
		sb.append("@ada\\:docWorkDossier:\"" + filter + "*\" OR ");
		sb.append("@ada\\:docCaseNumber:\"" + filter + "*\" OR ");
		sb.append("@ada\\:docStatus:\"" + filter + "*\" OR ");

		sb.append("@ada\\:scanEmployee:\"" + filter + "*\" OR ");
		sb.append("@ada\\:scanSeqNr:\"" + filter + "*\" OR ");
		sb.append("@ada\\:scanWaNr:\"" + filter + "*\" OR ");

		sb.append("@cm\\:name:\"" + filter + "*\" OR ");
		sb.append("@cm\\:title:\"" + filter + "*\" OR ");
		sb.append("@cm\\:description:\"" + filter + "*\" ");
		sb.append(") ");
		return sb.toString();
	}

	/**
	 * Get the direction for the sort.
	 * 
	 * @param req
	 * @return Boolean true if direction is asc, else false
	 */
	private Boolean getDir(WebScriptRequest req) {
		String dir = req.getParameter(PARAM_DIR);
		if (dir != null && dir.equalsIgnoreCase("asc")) {
			return true;
		}
		return false;
	}

	/**
	 * Get the direction of the sort.
	 * 
	 * @param req
	 * @return String asc or desc.
	 */
	protected String getDirection(WebScriptRequest req) {
		return req.getParameter(PARAM_DIR);
	}

	/**
	 * Get the sort property.
	 * 
	 * @param req
	 * @return String the sort property.
	 */
	protected String getSort(WebScriptRequest req) {
		String sortProp = req.getParameter(PARAM_SORT_BY);
		if (sortProp != null && sortProp.compareTo("null") != 0 && getSortProperty(sortProp) != null) {
			return getSortProperty(sortProp).toString();
		}
		return null;
	}

	protected QName getSortColumn(WebScriptRequest req) {
		String sortProp = req.getParameter(PARAM_SORT_BY);
		if (sortProp != null) {
			return getSortProperty(sortProp);
		}
		return null;
	}

	/**
	 * Build the model for all Adacta related folder and document properties.
	 * 
	 * @param nodeRef
	 * @return
	 */
	protected Map<String, Object> buildModel(NodeRef nodeRef) {
		Map<String, Object> map = new HashMap<String, Object>();

		// Default props
		map.put("id", nodeRef.getId());
		map.put("nodeRef", nodeRef.toString());
		map.put("name", nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
		map.put("title", nodeService.getProperty(nodeRef, ContentModel.PROP_TITLE));
		map.put("description", nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION));
		map.put("creator", nodeService.getProperty(nodeRef, ContentModel.PROP_CREATOR));
		map.put("creatorFullName", getFullName(nodeRef, ContentModel.PROP_CREATOR));
		map.put("modifier", nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIER));
		map.put("modifierFullName", getFullName(nodeRef, ContentModel.PROP_MODIFIER));
		map.put("created", nodeService.getProperty(nodeRef, ContentModel.PROP_CREATED));
		map.put("modified", nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED));

		QName type = nodeService.getType(nodeRef);
		map.put("nodeType", type.toPrefixString(serviceRegistry.getNamespaceService()));
		map.put("isContentType", type.equals(AdactaModel.TYPE_DOCUMENT));

		map.put("adactaBrowseUrl", getBrowseUrl(nodeRef, null));
		map.put("adactaDetailUrl", getDetailUrl(nodeRef, null));

		if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_OWNABLE)) {
			map.put("owner", nodeService.getProperty(nodeRef, ContentModel.PROP_OWNER));
			map.put("ownerFullName", getFullName(nodeRef, ContentModel.PROP_OWNER));
		}

		// Adacta props
		map.put("employeeNumber", nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_NUMBER));
		map.put("employeeName", nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_NAME));
		map.put("employeeBsn", nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_BSN));
		map.put("employeeMrn", nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_MRN));
		map.put("employeeDepartment", nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_DEPARTMENT));

		map.put("docCategory", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_CATEGORY));
		map.put("docSubject", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_SUBJECT));
		map.put("docDate", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_DATE));
		map.put("docReference", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_REFERENCE));
		map.put("docWorkDossier", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_WORK_DOSSIER));
		map.put("docCaseNumber", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_CASE_NUMBER));
		map.put("docDateCreated", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_DATE_CREATED));
		map.put("docMigId", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_MIG_ID));
		map.put("docMigDate", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_MIG_DATE));
		map.put("docStatus", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_STATUS));

		map.put("scanEmployee", nodeService.getProperty(nodeRef, AdactaModel.PROP_SCAN_EMPLOYEE));
		map.put("scanEmployeeFullName", getFullName(nodeRef, AdactaModel.PROP_SCAN_EMPLOYEE));
		map.put("scanSeqNr", nodeService.getProperty(nodeRef, AdactaModel.PROP_SCAN_SEQ_NR));
		map.put("scanWaNr", nodeService.getProperty(nodeRef, AdactaModel.PROP_SCAN_WA_NR));

		return map;
	}

	/**
	 * Get full name of provided property modifier or creator.
	 * 
	 * @param nodeRef
	 * @param qName
	 * @return
	 */
	private String getFullName(NodeRef nodeRef, QName qName) {
		String userName = (String) nodeService.getProperty(nodeRef, qName);
		if (userName != null && !userName.equalsIgnoreCase("System") && personService.personExists(userName)) {
			NodeRef personRef = personService.getPerson(userName);
			PersonInfo personInfo = personService.getPerson(personRef);
			return personInfo.getFirstName() + " " + personInfo.getLastName();
		}
		return userName;
	}

	/**
	 * Retrieves the named parameter as an integer, if the parameter is not present the default value is returned.
	 * 
	 * @param req
	 *            The WebScript request
	 * @param paramName
	 *            The name of parameter to look for.
	 * @param defaultValue
	 *            The default value that should be returned if parameter is not present in request or is negative.
	 * @return The request parameter or default value
	 * @throws WebScriptException
	 *             if the named parameter cannot be converted to int (HTTP rsp 400).
	 */
	protected int getNonNegativeIntParameter(WebScriptRequest req, String paramName, int defaultValue) {
		final String paramString = req.getParameter(paramName);
		final int result;
		if (paramString != null) {
			try {
				final int paramInt = Integer.valueOf(paramString);
				if (paramInt < 0) {
					result = defaultValue;
				} else {
					result = paramInt;
				}
			} catch (NumberFormatException e) {
				throw new WebScriptException(400, e.getMessage());
			}
		} else {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * Get skip count.
	 * 
	 * @param req
	 * @return Integer the skip count
	 */
	protected int getSkipCount(WebScriptRequest req) {
		String start = req.getParameter(PARAM_START_INDEX);
		int iStart = 0;
		if (start == null || start.length() == 0) {
			return iStart;
		}
		if (start != null && start.length() > 0) {
			try {
				iStart = Integer.parseInt(start.trim());
			} catch (NumberFormatException ex) {
				LOGGER.warn(String.format("numberFormatException: %s", start));
			}
		}
		return iStart;
	}

	/**
	 * Get max items from the request.
	 * 
	 * @param req
	 * @return
	 */
	protected int getMaxItems(WebScriptRequest req) {
		String start = req.getParameter(PARAM_PAGE_SIZE);
		int iStart = 0;
		if (start == null || start.length() == 0) {
			return iStart;
		}
		if (start != null && start.length() > 0) {
			try {
				iStart = Integer.parseInt(start.trim());
			} catch (NumberFormatException ex) {
				LOGGER.warn(String.format("numberFormatException: %s", start));
			}
		}
		return iStart;
	}

	/**
	 * Get the browse URL of a node reference
	 * 
	 * @param nodeRef
	 * @return String the complete URL including hostname
	 */
	protected String getBrowseUrl(NodeRef nodeRef, SiteInfo siteInfo) {
		String url = UrlUtil.getShareUrl(serviceDescriptorRegistry.getSysAdminParams()) + "/page/context/adacta/repository#filter=path|" + getDisplayPath(nodeRef, false);
		if (siteInfo != null) {
			url = UrlUtil.getShareUrl(serviceDescriptorRegistry.getSysAdminParams()) + "/page/site/" + siteInfo.getShortName() + "/documentlibrary#filter=path|"
					+ getDisplayPath(nodeRef, true);
		}

		// Check if it is folder, the add name to path
		if (nodeService.getType(nodeRef).equals(ContentModel.TYPE_FOLDER) || nodeService.getType(nodeRef).equals(AdactaModel.TYPE_DOSSIER)) {
			String folderName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
			url = url + "/" + folderName;
			url = url.replace("path|//", "path|/");
		}
		return url;
	}

	/**
	 * Get the document detail url.
	 * 
	 * @param nodeRef
	 * @return String URL
	 */
	protected String getDetailUrl(NodeRef nodeRef, SiteInfo siteInfo) {
		String type = "document";
		if (nodeService.getType(nodeRef).equals(ContentModel.TYPE_FOLDER) || nodeService.getType(nodeRef).equals(AdactaModel.TYPE_DOSSIER)) {
			type = "folder";
		}

		if (siteInfo != null) {
			return UrlUtil.getShareUrl(serviceDescriptorRegistry.getSysAdminParams()) + "/page/site/" + siteInfo.getShortName() + "/" + type + "-details?nodeRef="
					+ nodeRef.getStoreRef() + "/" + nodeRef.getId();
		}
		return UrlUtil.getShareUrl(serviceDescriptorRegistry.getSysAdminParams()) + "/page/context/adacta/" + type + "-details?nodeRef=" + nodeRef.getStoreRef() + "/"
				+ nodeRef.getId();
	}

	/**
	 * Get the display path for in the URL.
	 * 
	 * @param nodeRef
	 * @param inSite
	 * @return String display path
	 */
	private String getDisplayPath(NodeRef nodeRef, Boolean inSite) {
		String path = "/";
		String newPath = null;
		String displayPath = nodeService.getPath(nodeRef).toDisplayPath(nodeService, permissionService);
		if (inSite) {
			String[] parts = displayPath.split("/documentLibrary");
			if (parts.length == 1) {
				return path;
			}
			return parts[1];
		}

		// Build new path based on displayPath
		String[] parts = displayPath.split("/");
		for (String part : parts) {
			if (part == parts[0])
				continue;
			if (part == parts[1])
				continue;
			if (newPath == null) {
				newPath = path + part;
			} else {
				newPath += path + part;
			}
		}
		return newPath;
	}

	/**
	 * Build noderef based on store type and is
	 * 
	 * @param templateArgs
	 * @return NodeRef node reference
	 */
	protected NodeRef buildNodeRef(Map<String, String> templateArgs) {
		String storeType = templateArgs.get("store_type");
		String storeId = templateArgs.get("store_id");
		String nodeId = templateArgs.get("id");
		String nodePath = storeType + "/" + storeId + "/" + nodeId;
		return repository.findNodeRef("node", nodePath.split("/"));
	}

	/**
	 * Get JSON object from key.
	 * 
	 * @param jsonRequest
	 *            JSONObject json
	 * @param value
	 *            String the key
	 * @return Object
	 */
	protected Object getJsonObject(JSONObject jsonRequest, String value) {
		if (jsonRequest.has(value)) {
			try {
				return jsonRequest.get(value);
			} catch (JSONException e) {
				LOGGER.error(e);
			}
		}
		return null;
	}

	/**
	 * Make the pagination for given list of objects
	 * 
	 * @param results
	 *            the initial list of objects for pagination
	 * @param maxItems
	 *            maximum count of elements that should be included in paging result
	 * @param skipCount
	 *            the count of elements that should be skipped
	 * @return List of paginated results
	 */
	protected List<Map<String, Object>> applyPagination(List<Map<String, Object>> results, int maxItems, int skipCount) {
		// Do the paging
		return ModelUtil.page(results, maxItems, skipCount);
	}
	
	/**
	 * Sorts the category filter alphabetically.
	 * @param results
	 * @return
	 */
	public List<TemplateNode> sortCategoryFilter(List<TemplateNode> results) {
	       Collections.sort(results, new Comparator<TemplateNode>()
	        {
	            public int compare(TemplateNode tn1, TemplateNode tn2)
	            {
	            	String cat1 = (String) tn1.getProperties().get("adadl:description");
	            	String cat2 = (String) tn2.getProperties().get("adadl:description");
	                return cat1.compareTo(cat2);
	            }
	        });
		return results;
	}

}