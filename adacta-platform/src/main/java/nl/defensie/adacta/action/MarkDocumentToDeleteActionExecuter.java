package nl.defensie.adacta.action;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
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

/**
 * A simple action that marks and unmarks a document for deletion.
 * 
 * @author Miruna Chirita
 * @author Rick de Rooij
 *
 */
public class MarkDocumentToDeleteActionExecuter extends ActionExecuterAbstractBase {

	public static final String NAME = AdactaModel.PREFIX + "MarkDocumentToDelete";

	public static final String PARAM_LABEL= "label";
	public static final String PARAM_CODE = "code";
	protected Logger LOGGER = Logger.getLogger(this.getClass());
	
	@Autowired
	@Qualifier("NodeService")
	private NodeService nodeService;

	@Override
	protected void executeImpl(Action action, final NodeRef actionedUponNodeRef) {
		final String code = (String) action.getParameterValue(PARAM_CODE);
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
	            public Void doWork() throws Exception {
	                
	            	// Get the property
	        		String workDossier = (String) nodeService.getProperty(actionedUponNodeRef, AdactaModel.PROP_DOC_WORK_DOSSIER);
	        		if (workDossier != null && workDossier.trim().length() == 0) {
	        			workDossier = null;
	        		}	        		
	        		
	        		// Mark or unmark
	        		Map<QName, Serializable> props = new HashMap<QName, Serializable>(2);
	        	//	if (!StringUtils.isEmpty(code)){
	        		if (null != code && !code.isEmpty()){
	        			//Mark
	        			props.put(AdactaModel.PROP_DOC_STATUS, AdactaModel.LIST_STATUS_ACTIEF);
	        			props.put(AdactaModel.PROP_DOC_WORK_DOSSIER, code);
	        			nodeService.addAspect(actionedUponNodeRef, AdactaModel.ASPECT_DOCUMENT, props);
	        		} else {
	        			//UnMark
	        			props.put(AdactaModel.PROP_DOC_STATUS, AdactaModel.LIST_STATUS_ACTIEF);
	        			props.put(AdactaModel.PROP_DOC_WORK_DOSSIER, null);
	        			nodeService.addAspect(actionedUponNodeRef, AdactaModel.ASPECT_DOCUMENT, props);
	        		}
	        		return null;
	            }
	        });
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(new ParameterDefinitionImpl(PARAM_CODE, DataTypeDefinition.ANY, false, getParamDisplayLabel(PARAM_CODE)));
		paramList.add(new ParameterDefinitionImpl(PARAM_LABEL, DataTypeDefinition.ANY, false, getParamDisplayLabel(PARAM_LABEL)));
	}
}