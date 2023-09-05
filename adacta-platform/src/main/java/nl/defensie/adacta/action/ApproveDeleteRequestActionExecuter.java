package nl.defensie.adacta.action;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaFileFolderService;

/**
 * Action that will add a C to the destroy code. 
 * 
 * @author Rick de Rooij
 *
 */
public class ApproveDeleteRequestActionExecuter extends ActionExecuterAbstractBase {

	protected Logger LOGGER = Logger.getLogger(this.getClass());

	public static final String NAME = AdactaModel.PREFIX + "ApproveDeleteRequest";

	public static final String PARAM_LABEL = "label";

	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	protected AdactaFileFolderService adactaFileFolderService;

	@Override
	protected void executeImpl(Action action, final NodeRef actionedUponNodeRef) {

		String workDossier = (String) nodeService.getProperty(actionedUponNodeRef, AdactaModel.PROP_DOC_WORK_DOSSIER);
		String requester = (String) nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_MODIFIER);
		
		// Do we have a value
		if (workDossier == null) {
			throw new AlfrescoRuntimeException("Cannot approve delete request, because property is null.");
		}

		// Is the value a code?
		if (adactaFileFolderService.hasDestroyCode(actionedUponNodeRef) == false) {
			throw new AlfrescoRuntimeException("Value is not a destoy code V1.. V8.");
		}

		// Now we can make the destroy code final
		String finalDestroyCode = workDossier + "C";

		Map<QName, Serializable> props = new HashMap<QName, Serializable>(2);
		props.put(AdactaModel.PROP_DOC_STATUS, AdactaModel.LIST_STATUS_GESLOTEN);
		props.put(AdactaModel.PROP_DOC_WORK_DOSSIER, finalDestroyCode);
		nodeService.addAspect(actionedUponNodeRef, AdactaModel.ASPECT_DOCUMENT, props);

		String documentName = (String) nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_NAME);
		String approver = (String) nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_MODIFIER);
		LOGGER.info(String.format("%s %s %s %s %s", action, actionedUponNodeRef, documentName, requester, approver));
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(new ParameterDefinitionImpl(PARAM_LABEL, DataTypeDefinition.ANY, false, getParamDisplayLabel(PARAM_LABEL)));
	}
}