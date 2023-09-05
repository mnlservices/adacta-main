package nl.defensie.adacta.action;

import java.util.List;
import java.util.UUID;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.service.ServiceDescriptorRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.util.UrlUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaAuthorityService;
import nl.defensie.adacta.service.AdactaPreferenceService;
import nl.defensie.adacta.service.AdactaReportService;
import nl.defensie.adacta.service.AdactaSearchService;

/**
 * A simple action that delete a node (document or folder).
 * 
 * @author Rick de Rooij
 *
 */
public class DeleteNodeActionExecuter extends ActionExecuterAbstractBase {

	public static final String NAME = AdactaModel.PREFIX + "DeleteNode";

	public static final String PARAM_LABEL = "label";
	public static final String PARAM_COMMENT = "comment";
	protected Log LOGGER = LogFactory.getLog(DeleteNodeActionExecuter.class);	
	@Autowired
	@Qualifier("FileFolderService")
	protected FileFolderService fileFolderService;
	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;

	@Autowired
	@Qualifier("AuthenticationService")
	protected AuthenticationService authenticationService;
	@Autowired
	protected AdactaReportService adactaReportService;
	@Autowired
	protected AdactaSearchService adactaSearchService;
	@Autowired
	protected AdactaAuthorityService adactaAuthorityService;
	@Autowired
	@Qualifier("ServiceRegistry")
	protected ServiceDescriptorRegistry serviceDescriptorRegistry;
    @Autowired
    protected AdactaPreferenceService adactaPreferenceService;


	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {

		String userName = getAuthenticationService().getCurrentUserName();
		String context = getContextPage(actionedUponNodeRef);
		String parentFoldername = getParentFolderName(actionedUponNodeRef);
		
		if (getAdactaAuthorityService().isUserAdactaInvoerderOrBeheerder(userName)) {
			deleteAsSystem(actionedUponNodeRef);
		} else {
			getFileFolderService().delete(actionedUponNodeRef);
		}
		String reportUuid = getReportUUID();
		// we are on index page and deleting last document in the batch: show report
		if (context.equalsIgnoreCase("adacta-index") && getAdactaReportService().batchEmpty(parentFoldername)) {
			getAdactaReportService().printHtmlIndexReport(parentFoldername, null, reportUuid);
			NodeRef csvRef = getAdactaSearchService().getScanBatchCSVFileByName(parentFoldername + ".csv");
			if (csvRef != null) {
				getAdactaReportService().deleteCsvFile(csvRef);
			}
			String url = getReportUrl(reportUuid);
			action.setParameterValue(PARAM_RESULT, url);
			return;
		}
		// set a default context for the next page to show 
		action.setParameterValue(PARAM_RESULT, getUrlForContext(context));
		
		// or got the next selected item if appropriate
		if (context.equalsIgnoreCase("adacta-index") && !getAdactaReportService().batchEmpty(parentFoldername)) {
			List<NodeRef> selectedItems = getAdactaPreferenceService().getSelectedItemsPreferences();
			// go to next selected item
			if (selectedItems.size()>1) {
				action.setParameterValue(PARAM_RESULT, getDetailUrl(selectedItems.get(1)));//get(0)==the deleted (actual) noderef
			}
		}
	}


	private String getReportUrl(String reportUuid) {
		NodeRef htmlRef = getAdactaSearchService().getIndexReport(reportUuid);
		if (htmlRef == null) {
			// this can happen if all documents in a batch were deleted
			return getUrlForContext("adacta-search");
		}
		String id = htmlRef.getId();
		return UrlUtil.getShareUrl(getServiceDescriptorRegistry().getSysAdminParams())
				+ "/proxy/alfresco/slingshot/node/content/workspace/SpacesStore/" + id;
	}
    private String getDetailUrl(NodeRef nodeRef) {
    	 return UrlUtil.getShareUrl(getServiceDescriptorRegistry().getSysAdminParams()) + "/page/context/adacta/document-details?nodeRef=" + nodeRef.getStoreRef() + "/"
                + nodeRef.getId();
    }
	private String getUrlForContext(String context) {
		return UrlUtil.getShareUrl(getServiceDescriptorRegistry().getSysAdminParams()) + "/page/context/adacta/" + context;
	}

	private String getParentFolderName(NodeRef nodeRef) {
		return (String) getNodeService().getProperty(getParentNodeRef(nodeRef), ContentModel.PROP_NAME);
	}

	private NodeRef getParentNodeRef(NodeRef nodeRef) {
		return getNodeService().getPrimaryParent(nodeRef).getParentRef();
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(new ParameterDefinitionImpl(PARAM_LABEL, DataTypeDefinition.ANY, false, getParamDisplayLabel(PARAM_LABEL)));
	}

	/**
	 * Get context based on the location of the document. This will be used to redirect the page to the correct location when user is on detail page deleting a document.
	 * 
	 * @param nodeRef
	 *            NodeRef the node reference
	 * @return String page name
	 */
	protected String getContextPage(NodeRef nodeRef) {
		String result = "adacta-search";
		NodeRef parentRef = getParentNodeRef(nodeRef);
		NodeRef parentParentRef = getParentNodeRef(parentRef);
		if (getNodeService().hasAspect(parentRef, AdactaModel.ASPECT_ROOT_IMPORT)) {
			result = "adacta-import";
		} else if (getNodeService().hasAspect(parentParentRef, AdactaModel.ASPECT_ROOT_INDEX)) {
			result = "adacta-index";
		}
		return result;
	}

	/**
	 * Delete node as system.
	 * 
	 * @param nodeRef
	 */
	private void deleteAsSystem(final NodeRef nodeRef) {
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
			public Void doWork() throws Exception {
				getFileFolderService().delete(nodeRef);
				return null;
			}
		});
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public AdactaAuthorityService getAdactaAuthorityService() {
		return adactaAuthorityService;
	}

	public void setAdactaAuthorityService(AdactaAuthorityService adactaAuthorityService) {
		this.adactaAuthorityService = adactaAuthorityService;
	}

	public FileFolderService getFileFolderService() {
		return fileFolderService;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}

	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public AdactaReportService getAdactaReportService() {
		return adactaReportService;
	}

	public void setAdactaReportService(AdactaReportService adactaReportService) {
		this.adactaReportService = adactaReportService;
	}

	public ServiceDescriptorRegistry getServiceDescriptorRegistry() {
		return serviceDescriptorRegistry;
	}

	public void setServiceDescriptorRegistry(ServiceDescriptorRegistry serviceDescriptorRegistry) {
		this.serviceDescriptorRegistry = serviceDescriptorRegistry;
	}

	public AdactaSearchService getAdactaSearchService() {
		return adactaSearchService;
	}

	public void setAdactaSearchService(AdactaSearchService adactaSearchService) {
		this.adactaSearchService = adactaSearchService;
	}

	public AdactaPreferenceService getAdactaPreferenceService() {
		return adactaPreferenceService;
	}

	public void setAdactaPreferenceService(AdactaPreferenceService adactaPreferenceService) {
		this.adactaPreferenceService = adactaPreferenceService;
	}
	protected String getReportUUID() {
		return UUID.randomUUID().toString();
	}
	

}