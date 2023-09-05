package nl.defensie.adacta.webscript;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Removes or set ownership on folder (scanbatch).
 * 
 * @author Miruna Chirita
 * @author Rick de Rooij
 *
 */
public class AllocateBatchGet extends AdactaAbstract {

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		Map<String, String> urlVars = req.getServiceMatch().getTemplateVars();
		NodeRef scanBatch = buildNodeRef(urlVars);

		String clear = urlVars.get("clear");
		if (clear != null) {
			deallocateOwnerAsSystem(scanBatch);
		} else {
			String currentUser = authenticationService.getCurrentUserName();
			Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
			props.put(ContentModel.PROP_OWNER, currentUser);
			allocateOwnerAsSystem(scanBatch, props);
		}

		model.put("success", true);
		return model;
	}

	/**
	 * Add current user as owner to batch.
	 * 
	 * @param batchNode
	 *            NodeRef the batch folder.
	 * @param props
	 *            <QName, Serializable> map of properties to set on folder.
	 */
	private void allocateOwnerAsSystem(final NodeRef batchNode, final Map<QName, Serializable> props) {
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
			public Void doWork() throws Exception {
				nodeService.addAspect(batchNode, ContentModel.ASPECT_OWNABLE, props);
				return null;
			}
		});
	}

	/**
	 * Remove ownable aspect from batch.
	 * 
	 * @param batchNode
	 */
	private void deallocateOwnerAsSystem(final NodeRef batchNode) {
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
			public Void doWork() throws Exception {
				nodeService.removeAspect(batchNode, ContentModel.ASPECT_OWNABLE);
				return null;
			}
		});
	}
}