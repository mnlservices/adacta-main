package nl.defensie.adacta.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.service.ServiceDescriptorRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.UrlUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaFileFolderService;
import nl.defensie.adacta.service.AdactaPreferenceService;
import nl.defensie.adacta.service.AdactaSearchService;

/**
 * Moves single of multiple documents to personnel file. Also generates and report for each move that is executed.
 * 
 * @author Miruna Chirita
 * @author Rick de Rooij
 *
 */
public class MoveToPersonnelFileActionExecuter extends ActionExecuterAbstractBase {


    protected Log LOGGER = LogFactory.getLog(this.getClass());

    public static final String NAME = AdactaModel.PREFIX + "MoveDocumentToPersonnelFile";

    public static final String PARAM_LABEL = "label";
    public static final String PARAM_PERSONNEL_FILE = "personnel-file";
    public static final String PARAM_IS_BATCH = "is-batch";
    public static final String PARAM_REPORT_UUID = "reportUuid";
    
    @Autowired
    protected AdactaSearchService adactaSearchService;
    @Autowired
    @Qualifier("NodeService")
    protected NodeService nodeService;
    @Autowired
    @Qualifier("ActionService")
    protected ActionService actionService;
    @Autowired
    @Qualifier("PermissionService")
    protected PermissionService permissionService;
    @Autowired
    @Qualifier("FileFolderService")
    protected FileFolderService fileFolderService;
    @Autowired
    @Qualifier("ServiceRegistry")
    protected ServiceDescriptorRegistry serviceDescriptorRegistry;

    @Autowired
    protected AdactaFileFolderService adactaFileFolderService;
    @Autowired
    protected AdactaPreferenceService adactaPreferenceService;

    @Override
    protected void executeImpl(Action action, final NodeRef actionedUponNodeRef) {

			NodeRef parentRef = nodeService.getPrimaryParent(actionedUponNodeRef).getParentRef();
			Map<String, Serializable> params = action.getParameterValues();
			NodeRef personnelFileRef = getPersonnelFileNodeRef(params);
			Boolean isBatch = getIsBatch(params);

			List<NodeRef> selectedItems = adactaPreferenceService.getSelectedItemsPreferences();

			List<NodeRef> itemsToMove = determineItemsToMove(actionedUponNodeRef, isBatch, selectedItems);

			// Get employee props
			Map<QName, Serializable> employeeProps = adactaFileFolderService.getEmployeeProps(personnelFileRef);

			// Only move the items that have been indexed... - i.e the necessary metadata where filled in.
			List<NodeRef> indexedItems = filteredSelectedItems(itemsToMove);
			
			if (LOGGER.isDebugEnabled()) {
			    String folderName = (String) nodeService.getProperty(personnelFileRef, ContentModel.PROP_NAME);         
			    LOGGER.debug(String.format("Move '%s' items to personnel file '%s'.", indexedItems.size(), folderName));            
			}
			
			if (indexedItems.size() == 0) {
			    throw new AlfrescoRuntimeException("No items to process. Mandatory properties are not set on document.");
			}

			// Move documents
			moveDocumentsAsSystem(personnelFileRef, indexedItems, employeeProps);

			// Create index report
			String reportUuid = UUID.randomUUID().toString();
			createIndexReport(actionedUponNodeRef, parentRef, isBatch, indexedItems, reportUuid);

			// reset saved list of selected items and set action for next page 
			resetPreferences(action, itemsToMove, parentRef, isBatch, indexedItems, actionedUponNodeRef, reportUuid);
			
    }

	private List<NodeRef> determineItemsToMove(final NodeRef actionedUponNodeRef, Boolean isBatch,
			List<NodeRef> selectedItems) {
		List<NodeRef> itemsToMove = new ArrayList<NodeRef>();
		if (isBatch && !selectedItems.isEmpty()) {
		    itemsToMove.addAll(selectedItems);
		} else {
		    itemsToMove.add(actionedUponNodeRef);
		}
		return itemsToMove;
	}

    /**
     * Resets the saved list of selected items in the preferences section.
     * Determines which page to show next.
     * @param action
     * @param itemsToMove
     * @param parentRef
     * @param isBatch
     * @param indexedItems
     */
	private void resetPreferences(Action action, List<NodeRef> itemsToMove, NodeRef parentRef, Boolean isBatch,
			List<NodeRef> indexedItems, NodeRef actionedUponNodeRef, String reportUuid) {
		String parentName = (String)nodeService.getProperty(parentRef, ContentModel.PROP_NAME);
		// Clear items if selected list is equal with indexed list.
		if (indexedItems.size() == itemsToMove.size() && isBatch) {
		    adactaPreferenceService.clearSelectedItemsPreferences();
		    // if it is not a batch, but just selected items
		} else if (indexedItems.size() == itemsToMove.size() && !isBatch) {
		    List<NodeRef> si = adactaPreferenceService.getSelectedItemsPreferences();
		    // do we have selected items?
		    if (si.size() != 0) {
		        // remove indexed items from list
		        si.removeAll(indexedItems);
		        //set new selected items
		        adactaPreferenceService.setSelectedItemsPreferences(si);
		        // do we have still some left?
		        if (si.size() != 0) {
		            action.setParameterValue(PARAM_RESULT, getDetailUrl(si.get(0)));
		        }else{
		        	if (parentName.equalsIgnoreCase("Importeer")){
		        		action.setParameterValue(PARAM_RESULT, getAdactaUrl("adacta-import"));		        		
		        	}else{
		        		action.setParameterValue(PARAM_RESULT, getAdactaUrl("adacta-index"));
		        	}
		        }
		    }
		    // set new selected items if indexed items are not equal to total selected items..
		} else {
		    if (indexedItems.size() > 0) {
		        itemsToMove.removeAll(indexedItems);
		    }
		    adactaPreferenceService.setSelectedItemsPreferences(itemsToMove);

		    // Return the first noderef of remaining list
		    if (itemsToMove.size() != 0) {
		        action.setParameterValue(PARAM_RESULT, getDetailUrl(itemsToMove.get(0)));
		    }
		}
		// move to search page when batchfolder empty or no more selected documents 
		if (adactaFileFolderService.getDocumentsOfBatch(parentRef).size() == 0 || isBatch) {
        	if (parentName.equalsIgnoreCase("Importeer")){
        		action.setParameterValue(PARAM_RESULT, getAdactaUrl("adacta-import"));		        		
        	}else{
        		action.setParameterValue(PARAM_RESULT, getAdactaUrl("adacta-index"));
        	}
		}
				//move to report when finished
				if (adactaFileFolderService.getDocumentsOfBatch(parentRef).size() == 0){
					String url = getReportUrl(reportUuid);
					if (null != url){
						action.setParameterValue(PARAM_RESULT, url);
					}
				}
		QName type = nodeService.getType(parentRef);
		//herindexatie - blijf op document!
		if (type.compareTo(AdactaModel.TYPE_DOSSIER)==0){
	        action.setParameterValue(PARAM_RESULT, getDetailUrl(actionedUponNodeRef));
		}
	}

	/**
	 * Create index report, skip however if scanbatchRef appears to be a dossier. This is the situation where a
	 * document is re-indexed (moved from one dossier to another)
	 * 
	 * @param actionedUponNodeRef
	 * @param scanbatchRef
	 * @param isBatch
	 * @param indexedItems
	 */
	private void createIndexReport(final NodeRef actionedUponNodeRef, NodeRef scanbatchRef, Boolean isBatch,
			List<NodeRef> indexedItems, String uuid) {
		QName type = nodeService.getType(scanbatchRef);
		if (type.compareTo(AdactaModel.TYPE_DOSSIER)==0){
			return;
		}
		Action indexReport = actionService.createAction(CreateIndexReportActionExecuter.NAME);
		indexReport.setParameterValue(CreateIndexReportActionExecuter.PARAM_IS_BATCH, isBatch);
		indexReport.setParameterValue(CreateIndexReportActionExecuter.PARAM_DOCUMENTS, (Serializable) indexedItems);
		indexReport.setParameterValue(CreateIndexReportActionExecuter.PARAM_REPORT_UUID, uuid);
		indexReport.setParameterValue("scanbatchname", (String) nodeService.getProperty(scanbatchRef, ContentModel.PROP_NAME));
		indexReport.setExecuteAsynchronously(false);
		actionService.executeAction(indexReport, actionedUponNodeRef);
	}

	private Boolean getIsBatch(Map<String, Serializable> params) {
		Boolean isBatch = false;
		if (params.get(PARAM_IS_BATCH) != null) {
		    isBatch = (Boolean) params.get(PARAM_IS_BATCH);
		}
		return isBatch;
	}

	private NodeRef getPersonnelFileNodeRef(Map<String, Serializable> params) {
		NodeRef personnelFileRef = null;
		if (params.get(PARAM_PERSONNEL_FILE) instanceof NodeRef) {
		    personnelFileRef = (NodeRef) params.get(PARAM_PERSONNEL_FILE);
		} else if (params.get(PARAM_PERSONNEL_FILE) instanceof String) {
		    String pf = (String) params.get(PARAM_PERSONNEL_FILE);
		    if (NodeRef.isNodeRef(pf)) {
		        personnelFileRef = new NodeRef(pf);
		    } else {
		        throw new AlfrescoRuntimeException("Personnel file is unknown.");
		    }
		}
		return personnelFileRef;
	}

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        paramList.add(new ParameterDefinitionImpl(PARAM_LABEL, DataTypeDefinition.ANY, false, getParamDisplayLabel(PARAM_LABEL)));
        paramList.add(new ParameterDefinitionImpl(PARAM_PERSONNEL_FILE, DataTypeDefinition.ANY, true, getParamDisplayLabel(PARAM_PERSONNEL_FILE)));
        paramList.add(new ParameterDefinitionImpl(PARAM_IS_BATCH, DataTypeDefinition.BOOLEAN, false, getParamDisplayLabel(PARAM_IS_BATCH)));
    }

	/**
	 * Move document to personnel file and apply employee props on document.
	 * 
	 * @param personnelFile
	 *            NodeRef the target folder.
	 * @param items
	 *            List<NodeRef> list of documents to move
	 * @param employeeProps
	 *            <QName, Serializable> map of properties to set on each document.
	 */
	private void moveDocumentsAsSystem(final NodeRef personnelFile, final List<NodeRef> items, final Map<QName, Serializable> employeeProps) {
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
			public Void doWork() throws Exception {
				for (NodeRef item : items) {
					String fileName = (String)nodeService.getProperty(item, ContentModel.PROP_NAME);
					String scanSeqNumber = extractScanSeqNr(fileName);
					nodeService.setProperty(item, AdactaModel.PROP_SCAN_SEQ_NR, scanSeqNumber);
					while (nameExistsInTarget(personnelFile, fileName)){
						fileName=adactaFileFolderService.renameDuplicateFileName(fileName);
					}
					fileFolderService.rename(item, fileName);
					FileInfo fileInfo = fileFolderService.move(item, personnelFile, null);
					nodeService.addAspect(fileInfo.getNodeRef(), AdactaModel.ASPECT_EMPLOYEE, employeeProps);
					Object docDateCreated = nodeService.getProperty(item, AdactaModel.PROP_DOC_DATE_CREATED);
					if (null == docDateCreated){
						nodeService.setProperty(item, AdactaModel.PROP_DOC_DATE_CREATED, new Date());						
					}

					// Clear permissions if inheritance is disabled
					if (permissionService.getInheritParentPermissions(fileInfo.getNodeRef()) == false) {
						permissionService.setInheritParentPermissions(fileInfo.getNodeRef(), true);
						permissionService.clearPermission(fileInfo.getNodeRef(), PermissionService.ALL_AUTHORITIES);
					}
				}
				return null;
			}

			/**
			 * Gets number between last underscore and file-extension.
			 * This number is then iset in the appropriate field.
			 * We be used later to sort scanned items in indexreport.
			 *  
			 * @param fileName
			 * @return
			 */
			private String extractScanSeqNr(String fileName) {
				int dot = fileName.lastIndexOf(".");
				int underscore = 0;
				String prefix ="";
				if (dot < 0){
					underscore = fileName.lastIndexOf("_");
					return fileName.substring(underscore + 1, fileName.length());
				}
				prefix = fileName.substring(0, dot);
				underscore = prefix.lastIndexOf("_");
				return prefix.substring(underscore + 1, prefix.length());
			}

			private boolean nameExistsInTarget(NodeRef personnelFile, String candidateName) {
				List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(personnelFile);
				for (ChildAssociationRef child:childRefs){
					String name = (String) nodeService.getProperty(child.getChildRef(), ContentModel.PROP_NAME);
					if (candidateName.equalsIgnoreCase(name)){
						return true;
					}
						
				}
				return false;
			}
		});
	}

    /**
     * Validate if document is indexed. Check for mandatory props.
     * 
     * @param items
     * @return List<NodeRef> items that have been indexed (mandatory props are set.)
     */
    private List<NodeRef> filteredSelectedItems(List<NodeRef> items) {
        List<NodeRef> list = new ArrayList<NodeRef>();
        for (NodeRef nodeRef : items) {
            if (adactaFileFolderService.hasMandantoryProps(nodeRef)) {
                list.add(nodeRef);
            }
        }

        return list;
    }

    /**
     * Build document details URL.
     * 
     * @param nodeRef
     * @return String absolute URL.
     */
    private String getDetailUrl(NodeRef nodeRef) {
        return UrlUtil.getShareUrl(serviceDescriptorRegistry.getSysAdminParams()) + "/page/context/adacta/document-details?nodeRef=" + nodeRef.getStoreRef() + "/"
                + nodeRef.getId();
    }
	private String getAdactaUrl(String page) {
	    return UrlUtil.getShareUrl(serviceDescriptorRegistry.getSysAdminParams()) + "/page/context/adacta/"+page;
	}
	private String getReportUrl(String reportUuid) {
		NodeRef htmlRef = adactaSearchService.getIndexReport(reportUuid);
		if (htmlRef==null){
			return null;
		}
		String id = htmlRef.getId();
	    return UrlUtil.getShareUrl(serviceDescriptorRegistry.getSysAdminParams()) + "/proxy/alfresco/slingshot/node/content/workspace/SpacesStore/"+id;
	}
}