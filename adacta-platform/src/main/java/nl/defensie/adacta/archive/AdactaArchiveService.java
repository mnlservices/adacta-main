package nl.defensie.adacta.archive;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaSearchService;
import nl.defensie.adacta.utils.SyncLogger;

public class AdactaArchiveService {


	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "adactaArchiveService");

	protected Logger LOGGER = Logger.getLogger(this.getClass());
	
	@Autowired
	protected AdactaSearchService adactaSearchService;
	@Autowired
	protected FileFolderService fileFolderService;
	@Autowired
	protected NodeService nodeService;
	@Autowired
	protected AdactaArchiveDatabaseService dbService;
	
	
	public void startArchiving() {
		List<String> noderefs = new ArrayList<String>();
		//ambtsberichten
		SyncLogger.info("start to archive ambtsberichten (Personeelsadministratie)");
		dbService.initialNameSpaceQueries();
		noderefs = dbService.getQueryResults("1801", 6);
		closeDocuments(noderefs);
		SyncLogger.info("done archive ambtsberichten (Personeelsadministratie)");
		SyncLogger.info("start to archive ambtsberichten (Management Development)");
		noderefs = dbService.getQueryResults("8204", 6);
		closeDocuments(noderefs);
		SyncLogger.info("done archive ambtsberichten (Management Development)");	
	}
	
	private void deleteDocuments(List<NodeRef> noderefs) {
		SyncLogger.info("starting to delete "+noderefs.size()+" documents ");
		for (NodeRef noderef:noderefs) {
			deleteAsSystem(noderef);
		}
	}

	private void deleteAsSystem(final NodeRef nodeRef) {
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
			public Void doWork() throws Exception {
				fileFolderService.delete(nodeRef);
				return null;
			}
		});
	}
	private void closeDocuments(List<String> noderefs) {
		SyncLogger.info("starting to close "+noderefs.size()+" documents ");
		for (String noderef:noderefs) {
			NodeRef nr = new NodeRef("workspace://SpacesStore/"+noderef); 
			closeAsSystem(nr);
		}
		SyncLogger.info("done closing "+noderefs.size()+" documents ");
	}
	
	private void closeAsSystem(final NodeRef nodeRef) {
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
			public Void doWork() throws Exception {
				nodeService.setProperty(nodeRef, AdactaModel.PROP_DOC_STATUS, AdactaModel.LIST_STATUS_GESLOTEN);
				return null;
			}
		});
	}

}
