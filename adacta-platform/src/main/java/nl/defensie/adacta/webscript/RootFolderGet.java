package nl.defensie.adacta.webscript;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import nl.defensie.adacta.model.AdactaModel;

/**
 * Get the node reference based on provided root aspect.
 * 
 * @author Rick de Rooij
 *
 */
public class RootFolderGet extends AdactaAbstract {

	public static final String PARAM_ASPECT = "aspect";

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		String aspect = req.getParameter(PARAM_ASPECT);
		NodeRef nodeRef = null;

		if (aspect.equalsIgnoreCase("ada:rootImportAspect")) {
			nodeRef = adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_IMPORT);
		} else if (aspect.equalsIgnoreCase("ada:rootIndexAspect")) {
			nodeRef = adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_INDEX);
		} else if (aspect.equalsIgnoreCase("rootReportAspect")) {
			nodeRef = adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_REPORT);
		} else if (aspect.equalsIgnoreCase("ada:rootDossiersAspect")) {
			nodeRef = adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_DOSSIERS);
		}
		model.put("item", nodeRef);

		return model;
	}
}