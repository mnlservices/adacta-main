package nl.defensie.adacta.action.schedule;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaSearchService;
import nl.defensie.adacta.utils.DateUtils;

/**
 * Delete documents that are marked for deletion. Based on the modified date and current date, the document will be deleted or not.
 * Also delete empty scanbatches.
 * 
 * @author Miruna Chirita
 * @author Rick de Rooij
 *
 */
public class DeleteMarkedDocumentsActionExecuter extends ActionExecuterAbstractBase {

	protected Logger LOGGER = Logger.getLogger(this.getClass());

	public static final String NAME = AdactaModel.PREFIX + "DeleteMarkedDocuments";

	public static final String PARAM_DEFAULT = "default";

	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	protected AdactaSearchService adactaSearchService;

	@Value("${adacta.days.to.delete.documents}")
	private Integer daysToDeleteDocuments;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		LOGGER.debug("running job deleting marked documents");
		// Get the documents that are marked for removal
		ResultSet results = adactaSearchService.getDocumentsForDelete();

		// Check result
		if (null == results || results.length() == 0) {
			return;
		}

		// Loop through results and calculate the date difference.
		for (int i = 0; i < results.length(); i++) {
			NodeRef nodeRef = results.getRow(i).getNodeRef();

			Date currentDate = new Date();
			Date modifiedDate = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
			String docCategory = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_CATEGORY);
			boolean medical = (null != docCategory && (docCategory.equals("85") ||docCategory.equals("86") || docCategory.equals("87")));
			Integer daysMarkedForRemoval = (int) DateUtils.getDateDiff(modifiedDate, currentDate, TimeUnit.DAYS);

			if (daysMarkedForRemoval >= daysToDeleteDocuments && !medical) {
				String documentName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
				String employeeNumber = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_NUMBER);
				// Do not delete permanently.
				//nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
				nodeService.deleteNode(nodeRef);
				
				LOGGER.info(String.format("%s %s %s %s", action.getDescription(), nodeRef, documentName, employeeNumber));
			}
		}
		//Also delete all empty scanbatchfolders in the same job
		deleteEmptyScanbatches(action);
	}

	private void deleteEmptyScanbatches(Action action) {
		LOGGER.info("running scheduled job; deleting empty scanbatches");
		List<NodeRef> results = adactaSearchService.getScanBatchFolders();
		for (NodeRef nodeRef:results){
			List<ChildAssociationRef> children = nodeService.getChildAssocs(nodeRef);
			String folderName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
				int nodeCounter = 0;
				Iterator<ChildAssociationRef> i = children.iterator();
				while (i.hasNext()) {
					ChildAssociationRef ref = i.next();
					NodeRef child = ref.getChildRef();
					if (nodeService.getType(child).equals(AdactaModel.TYPE_DOCUMENT) || nodeService.getType(child).equals(ContentModel.TYPE_CONTENT)) {
						nodeCounter++;
					}				
				}
				if (nodeCounter == 0){
					nodeService.deleteNode(nodeRef);
					LOGGER.info("Deleted empty scanbatchfolder "+String.format("%s, %s %s",action.getDescription(), nodeRef, folderName));					
				}else{
					LOGGER.info("Not deleting scanbatchfolder "+folderName+" nr docs remaining: "+nodeCounter);										
				}
		}
		
	}


	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(new ParameterDefinitionImpl(PARAM_DEFAULT, DataTypeDefinition.ANY, false, getParamDisplayLabel(PARAM_DEFAULT)));
	}
}