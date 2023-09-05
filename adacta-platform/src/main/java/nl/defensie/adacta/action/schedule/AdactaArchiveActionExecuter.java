package nl.defensie.adacta.action.schedule;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.archive.AdactaArchiveService;
import nl.defensie.adacta.utils.SyncLogger;

public class AdactaArchiveActionExecuter extends ActionExecuterAbstractBase {

	public static final String NAME = "adactaArchive";
	protected Logger LOGGER = Logger.getLogger(this.getClass());
	
	@Autowired
	protected AdactaArchiveService adactaArchiveService;
	
	@Value("${adacta.cron.job.synclogger.path}")
	private String syncloggerLocation;
	
	@Value("${adacta.cron.job.adactaArchive.enabled}")
	private Boolean archiveEnabled;
	
	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (!archiveEnabled) {
			LOGGER.info("Archiving not enabled, jobexecution aborted");
			return;
		}
		SyncLogger.reset();
		SyncLogger.setLoggerPath(syncloggerLocation);
		SyncLogger.info("Start Adacta Archive job");
		
		try {
			adactaArchiveService.startArchiving();
		} catch (Exception e) {
			SyncLogger.error("foutje in archiving ",e);
		}
		
		SyncLogger.info("Done Adacta Archive job");
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		// TODO Auto-generated method stub
		
	}

}
