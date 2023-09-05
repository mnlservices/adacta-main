package nl.defensie.adacta.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.query.PagingRequest;
import org.alfresco.query.PagingResults;
import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorker;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.SyncLogger;

public class AdactaPeopleSyncService {

    private static final String ADACTA_SERVICE_USER = "sa_adacta";
	private static final String NAME_DP_CODE_BATCH_SYNC = "DP_CODE_SYNC_BATCH";
    private static final Log LOG = LogFactory.getLog(AdactaPeopleSyncService.class);
    QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "PeopleSyncService");  
    private int batchSize = 200;
    private int batchThreads = 20;

    @Autowired
    protected Adacta2DatabaseService adacta2DatabaseService;
    @Autowired
    @Qualifier("NodeService")
    protected NodeService nodeService;
    @Autowired
    @Qualifier("PersonService")
    protected PersonService personService;
    @Autowired
    @Qualifier("RuleService")
    protected RuleService ruleService;
    @Autowired
    @Qualifier("TransactionService")
    protected TransactionService transactionService;
   
    protected Map<String, String> changedRowSecClassList;

    /**
     * Sync DP codes on all users in Alfresco.
     * @return tuple of total items updated and time taken in seconds
     */
    public void synchronizeUserDPCodes() {
    	SyncLogger.info("starting sync DP codes");
    	changedRowSecClassList = adacta2DatabaseService.getChangedRowSecClass();
    	SyncLogger.info("found "+changedRowSecClassList.size()+" entries in changedRowSecClassList");
        final int pageSize = 5000;
        final List<Pair<QName, Boolean>> sortProperties = new ArrayList<Pair<QName, Boolean>>();
        sortProperties.add(new Pair<QName, Boolean>(ContentModel.PROP_USERNAME, false));
        sortProperties.add(new Pair<QName, Boolean>(ContentModel.PROP_FIRSTNAME, false));
        sortProperties.add(new Pair<QName, Boolean>(ContentModel.PROP_LASTNAME, false));

        final long start = System.nanoTime();
        PagingRequest pagingRequest = new PagingRequest(0, pageSize);
        int total = 0;
        while (true) {
            final PagingResults<PersonInfo> people = personService.getPeople("*", null, sortProperties, pagingRequest);
            batchSynchronizeUsers(people.getPage());
            if (people.getPage().size() == pageSize) {
                final int oldSkipCount = pagingRequest.getSkipCount();
                pagingRequest = new PagingRequest(oldSkipCount + pageSize, pageSize);

                // Failsafe to prevent infinite loop
                final long duration = System.nanoTime() - start;
                if (TimeUnit.NANOSECONDS.toHours(duration) > 3) {
                    throw new AlfrescoRuntimeException("Timeout of 3 hours exceeded!");
                }
            } else {
                total = pagingRequest.getSkipCount() + people.getPage().size();
                break;
            }
        }
        
        final long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
        SyncLogger.info(String.format("Processed %s users in %s seconds", total, duration));
    }

    private void batchSynchronizeUsers(final List<PersonInfo> users) {
        final Iterator<PersonInfo> i = users.listIterator();
        final BatchProcessWorkProvider<PersonInfo> provider = new BatchProcessWorkProvider<PersonInfo>() {
            @Override
            public int getTotalEstimatedWorkSize() {
                return users.size();
            }

            @Override
            public Collection<PersonInfo> getNextWork() {
                final List<PersonInfo> work = new ArrayList<PersonInfo>(batchSize);
                while (i.hasNext() && work.size() < batchSize) {
                    work.add(i.next());
                }
                return work;
            }
        };

        final BatchProcessor<PersonInfo> processor = new BatchProcessor<PersonInfo>(
                NAME_DP_CODE_BATCH_SYNC, 
                transactionService.getRetryingTransactionHelper(),
                provider,
                batchThreads,
                batchSize, 
                null,
                SyncLog.LOG,
                batchSize);

        final BatchProcessWorker<PersonInfo> worker = new BatchProcessWorker<PersonInfo>() {
            @Override
            public String getIdentifier(final PersonInfo entry) {
                return entry.getUserName();
            }

            @Override
            public void beforeProcess() throws Throwable {
                // Authentication
                String systemUser = ADACTA_SERVICE_USER;
                AuthenticationUtil.setRunAsUser(systemUser);
            }

            @Override 
            public void process(final PersonInfo entry) throws Throwable {
                try {
                    final NodeRef nodeRef = entry.getNodeRef();
                    String emplid = (String) getNodeService().getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_ID);
                    String userDp = (String) getNodeService().getProperty(nodeRef, AdactaModel.PROP_DP_CODE);
                    String dpCode = changedRowSecClassList.get(emplid);
                    if (dpCode != null){
                    	dpCode=dpCode.trim();
                    }else{
                    	//nothing found in list with changes RowSecclass, ignore this entry
                    	return;
                    }
                    if (!StringUtils.isEmpty(dpCode) && !dpCode.equalsIgnoreCase("null") ){
                    	if (!dpCode.equalsIgnoreCase(userDp)){
                    		getNodeService().setProperty(nodeRef, AdactaModel.PROP_DP_CODE, dpCode);                    	
                    		SyncLogger.debug(String.format("Set DPCODE for %s from %s to %s", entry.getUserName(),userDp, dpCode));
                    	}
                    }else{
                    	//remove dpcode
                    	if (!StringUtils.isEmpty(userDp)){
                    		getNodeService().removeProperty(nodeRef, AdactaModel.PROP_DP_CODE);
                    		SyncLogger.debug(String.format("removing DPCODE %s for user %s",userDp, entry.getUserName()));
                    	}
                    }
                } catch (final Throwable t) {
                    SyncLogger.error("Error syncing user " + getIdentifier(entry), t);
                    throw t;
                }
            }

            @Override
            public void afterProcess() throws Throwable {
                // Clear authentication
                AuthenticationUtil.clearCurrentSecurityContext();
            }
        };

        final int invocations = processor.process(worker, true);
        LOG.info("Completed synchronization of " + users.size() + " users using " + invocations + " invocations");
        SyncLogger.info("Completed synchronization of " + users.size() + " users using " + invocations + " invocations");
    }

    /**
     * SyncLogger is not logging - for unknown configuration errors. The Batchprocessor needs however a commons.LOG 
     * Therefore it is maintained for now. Logging inside this class is replaced by the log4j.SyncLogger
     * @return
     */
	public static Log getSyncLogger() {
        return SyncLog.LOG;
    }

    private static class SyncLog {
        private static final Log LOG = LogFactory.getLog(SyncLog.class);
    }

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public Map<String, String> getChangedRowSecClassList() {
		return changedRowSecClassList;
	}

	public void setChangedRowSecClassList(Map<String, String> changedRowSecClassList) {
		this.changedRowSecClassList = changedRowSecClassList;
	}
    
    
}
