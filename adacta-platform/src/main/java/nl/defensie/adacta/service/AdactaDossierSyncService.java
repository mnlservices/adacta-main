package nl.defensie.adacta.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorker;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.util.ThreadSafeCounter;
import nl.defensie.adacta.utils.AdactaDossier;
import nl.defensie.adacta.utils.SyncLogger;

public class AdactaDossierSyncService {

	protected Logger LOGGER = Logger.getLogger(this.getClass());
	
    private static final String ADACTA_SERVICE_USER = "sa_adacta";
	private static final String SPACE = " ";
	private static final String NAME_BATCH_SYNC = "DOSSIER_SYNC_BATCH";
    
    private int batchSize = 200;
    private int batchThreads = 20;

    @Autowired
    protected Adacta2DatabaseService adacta2DatabaseService;
    @Autowired
    protected AdactaPeopleSyncService adactaPeopleSyncService;
    @Autowired
    protected AdactaFileFolderService adactaFileFolderService;
    @Autowired
    protected AdactaSearchService adactaSearchService;
    @Autowired
    @Qualifier("NodeService")
    protected NodeService nodeService;
    @Autowired
    @Qualifier("RuleService")
    protected RuleService ruleService;
    @Autowired
    @Qualifier("TransactionService")
    protected TransactionService transactionService;
	@Autowired
	protected AdactaMailService adactaMailService;

	@Value("${adacta.cron.job.synclogger.path}")
	private String syncloggerLocation;
	
    private ArrayList<String> mailContentList;
    
    
	public void syncDossiers() {
		LOGGER.info("Starting ADACTA sync dossier process ");
		SyncLogger.reset();
		SyncLogger.setLoggerPath(syncloggerLocation);
		SyncLogger.debug("Starting ADACTA sync dossier process ");
        mailContentList = new ArrayList<>();
		try {

			preprocess();

			process();

			postprocess();

			SyncLogger.debug("End ADACTA sync dossier process ");
			//TODO property address mailbox
			adactaMailService.sendMail(mailContentList);

		} catch (Throwable t) {
            SyncLogger.error("Error syncing dossiers ", t);
            mailContentList.clear();
            mailContentList.add("Generieke fout in de dossierynchronisatie. ");
            if (t.toString().contains("Could not get JDBC Connection")){
            	mailContentList.add("Geen verbinding met CDM verkregen ");
            	mailContentList.add("Eventuele nieuwe dossiers zullen in de volgende run aangemaakt worden. ");
            	mailContentList.add("Uw applicatiebeheerder kan deze job op verzoek ook handmatig uitvoeren.");
            }
			adactaMailService.sendMail(mailContentList);            
		}
	}

    /**
     * Truncates the ADACTA sync table and then fills it with CDM data.
     * @return
     */
    private void preprocess() {
        SyncLogger.debug("Start preprocessing ADACTA sync dossier process. ");
		adacta2DatabaseService.truncateAlfrescoSyncTable();
        SyncLogger.debug("Done truncating ADACTA sync table ");
		adacta2DatabaseService.copyCDMData();
        SyncLogger.debug("Done preprocessing ADACTA sync dossier process ");
	}


    /**
     * Determine changed and new records and process them.
     * @return
     */
	private void process() {
        SyncLogger.debug("Start processing ADACTA sync dossier process ");

		//new dossiers
		List<AdactaDossier> blist = adacta2DatabaseService.getNewDossiers();
		SyncLogger.debug("Done getting new dossiers, found  "+blist.size());
		mailContentList.add("Aantal nieuwe dossiers te maken: "+blist.size());
		batchSynchronizeDossiers(blist, false);
		SyncLogger.debug("Done creating new dossiers ");
		//changed dossiers
		List<AdactaDossier> alist = adacta2DatabaseService.getChangedDossiers();
        SyncLogger.debug("Done getting changed dossiers, found  "+alist.size());
        mailContentList.add("Aantal dossiers te wijzigen: "+alist.size());
		batchSynchronizeDossiers(alist, true);
        SyncLogger.debug("Done Updating changed dossiers ");
        SyncLogger.debug("Done processing ADACTA sync dossier process ");

        //dpCodes
		try {
			adactaPeopleSyncService.synchronizeUserDPCodes();
		} catch (Exception e) {
			SyncLogger.error("error while synchronizing user DpCode ",e);
		}
	}

	/**
	 * empty the sync save table, and copy sync table into save table. 
	 */
	private void postprocess() {
        SyncLogger.debug("Start postprocessing ADACTA sync dossier process ");
		adacta2DatabaseService.truncateAlfrescoSyncSaveTable();
        SyncLogger.debug("Done truncating ADACTA sync save table ");
		adacta2DatabaseService.copyAlfrescoSyncTable();
        SyncLogger.debug("Done copying ADACTA sync table to save table ");
        SyncLogger.debug("Done postprocessing ADACTA sync dossier process ");
	}

	/**
	 * 
	 * @param dossiers
	 * @param changed true for changed dossiers, false for new dossiers
	 */
	private void batchSynchronizeDossiers(final List<AdactaDossier> dossiers, boolean changed) {
        final ThreadSafeCounter created = new ThreadSafeCounter();
        final ThreadSafeCounter updated = new ThreadSafeCounter();
        final ThreadSafeCounter failedCreate = new ThreadSafeCounter();
        final ThreadSafeCounter failedUpdate = new ThreadSafeCounter();

        final Iterator<AdactaDossier> i = dossiers.listIterator();
        final BatchProcessWorkProvider<AdactaDossier> dossiersProvider = new BatchProcessWorkProvider<AdactaDossier>() {
            @Override
            public int getTotalEstimatedWorkSize() {
                return dossiers.size();
            }

            @Override
            public Collection<AdactaDossier> getNextWork() {
                final List<AdactaDossier> work = new ArrayList<AdactaDossier>(batchSize);
                while (i.hasNext() && work.size() < batchSize) {
                    work.add(i.next());
                }
                return work;
            }
        };

        final BatchProcessor<AdactaDossier> dossiersProcessor = new BatchProcessor<AdactaDossier>(NAME_BATCH_SYNC,
                transactionService.getRetryingTransactionHelper(), dossiersProvider, batchThreads, batchSize, null,
                SyncLog.LOG, batchSize);

        final BatchProcessWorker<AdactaDossier> worker = new BatchProcessWorker<AdactaDossier>() {
            @Override
            public String getIdentifier(final AdactaDossier entry) {
                return entry.getEmplbsn();
            }

            @Override
            public void beforeProcess() throws Throwable {
                // Disable rules
                ruleService.disableRules();
                // Authentication
                String systemUser = ADACTA_SERVICE_USER;
                AuthenticationUtil.setRunAsUser(systemUser);
            }

            @Override
            public void process(final AdactaDossier entry) throws Throwable {
                try {
                    if (synchronizeDossier(entry, changed)) {
                    	if (changed){updated.increment();}
                    	if (!changed){created.increment();}
                    } else{
                    	if (changed){failedUpdate.increment();}
                    	if (!changed){failedCreate.increment();}                    	
                    }
                } catch (final Throwable t) {
                	if (changed){failedUpdate.increment();}
                	if (!changed){failedCreate.increment();}
                    String bsn = null;
                    if (null != entry){
                    	bsn = entry.getEmplbsn();
                    }
                    SyncLogger.error("Error syncing dossier " + bsn, t);
                    throw t;
                }
            }

            @Override
            public void afterProcess() throws Throwable {
                // Enable rules
                ruleService.enableRules();
                // Clear authentication
                AuthenticationUtil.clearCurrentSecurityContext();
            }
        };

        final int invocations = dossiersProcessor.process(worker, true);
        //LOG.info("Completed synchronization of " + dossiers.size() + " dossiers using " + invocations + " invocations");
        SyncLogger.info(
                "Completed synchronization of " + dossiers.size() + " dossiers using " + invocations + " invocations");
        if (changed){
        	SyncLogger.info(String.format("Updated: %s, Failed: %s",  updated.getValue(), failedUpdate.getValue()));
        	mailContentList.add("Aantal dossiers gewijzigd: "+updated.getValue()+" aantal gefaald: "+failedUpdate.getValue());
        }else{
            SyncLogger.info(String.format("Created: %s, Failed: %s", created.getValue(),failedCreate.getValue()));        	
            mailContentList.add("Aantal dossiers aangemaakt: "+created.getValue()+" aantal gefaald: "+failedCreate.getValue());
        }
    }


    /**
     * <p>
     * Synchronize dossier for the given NLD. Metadata are retrieved from the
     * CDM database.
     * </p>
     * <p>
     * If the dossier doesn't exist already, it is created.
     * </p>
     * 
     * @param nld
     *            NLD-ID for the subject of the dossier
     * @return <code>true</code> if the dossier did not previously exist and was
     *         created
     */
	protected boolean synchronizeDossier(final AdactaDossier dossier, boolean changed) {
		if (dossier == null) {
			throw new IllegalArgumentException(
					"DATA INCONSISTENCY DETECTED - No MDW match for dossier with NLD-ID: " + dossier);
		}
		final Map<QName, Serializable> dossierProps = new HashMap<QName, Serializable>();
		ArrayList<String> dpcodes = null;
		if (dossier.getDpcodes() != null){
			dpcodes = new ArrayList<>(Arrays.asList(dossier.getDpcodes().split(SPACE)));
		}else{
			dpcodes = new ArrayList<String>();
		}
		//Bestaande dossiers
		if (changed) {
			final NodeRef pDossier = getAdactaSearchService().getPersonnelFileByBSN(dossier.getEmplbsn(),
					QueryConsistency.TRANSACTIONAL);
			if (pDossier == null){
				  SyncLogger.info("adacta dossier with id "+dossier.getEmplbsn()+" not found");
				  return false;
			}
			dossierProps.put(AdactaModel.PROP_EMPLOYEE_BSN, dossier.getEmplbsn());
			dossierProps.put(AdactaModel.PROP_EMPLOYEE_DEPARTMENT, dossier.getEmpldep());
			@SuppressWarnings("unchecked")
			ArrayList<String> adactaDpCodes = (ArrayList<String>) getNodeService().getProperty(pDossier,  AdactaModel.PROP_EMPLOYEE_DP_CODES);
			if (dpcodes==null || dpcodes.size()==0){
				//geen dpcodes in CDM
				if (null != adactaDpCodes && adactaDpCodes.size()>0){
					// wel in Adacta, neem dan de Adacta codes maar
					dpcodes = adactaDpCodes;
				}
			}
			dossierProps.put(AdactaModel.PROP_EMPLOYEE_DP_CODES, dpcodes);
			
			dossierProps.put(AdactaModel.PROP_EMPLOYEE_NUMBER, dossier.getEmplid());
			dossierProps.put(AdactaModel.PROP_EMPLOYEE_NAME, dossier.getEmplname());
			dossierProps.put(AdactaModel.PROP_EMPLOYEE_MRN, dossier.getEmplmrn());

			String emplid = (String)getNodeService().getProperty(pDossier, AdactaModel.PROP_EMPLOYEE_NUMBER);
			if (!emplid.equals(dossier.getEmplid())){
				 updateDocuments(pDossier, dossier.getEmplid());
			} 

			logProps(dossierProps);

			getNodeService().addAspect(pDossier, AdactaModel.ASPECT_EMPLOYEE, dossierProps);
			SyncLogger.info("Updated dossier " + dossier.getEmplbsn());
			return true;
		} else {
			//nieuwe dossiers
			//final Map<QName, Serializable> dossierPropsNew = new HashMap<QName, Serializable>();
			final Map<QName, Serializable> dossierPropsNew = getDossierPropertiesNew(dossier, dpcodes);

			NodeRef existingDossier = getAdactaSearchService().getPersonnelFileByBSN(dossier.getEmplbsn());
			if (existingDossier==null){
				final NodeRef nr = getAdactaFileFolderService().creatPersonnelFile(dossierPropsNew).getNodeRef();
				SyncLogger.info(String.format("Created new personnel file for WID %s: %s", dossier.getEmplid(), nr.toString()));
				logProps(dossierPropsNew);
				return true;
			}else{
				SyncLogger.info(String.format("Could not create new personnel file for  %s", dossier.getEmplbsn()));
				LOGGER.info(String.format("Could not create new personnel file for  %s", dossier.getEmplbsn()));
				return false;
			}
		}
	}

	private Map<QName, Serializable> getDossierPropertiesNew(AdactaDossier dossier, ArrayList<String> dpcodes) {
		final Map<QName, Serializable> dossierPropsNew = new HashMap<QName, Serializable>();
		String dep = dossier.getEmpldep();
		//nieuw dossier en geen afdeling, dan is het een sollicitant
		if (StringUtils.isEmpty(dep)){
			dep = "SOLLI";
		}
		dossierPropsNew.put(AdactaModel.PROP_EMPLOYEE_BSN, dossier.getEmplbsn());
		dossierPropsNew.put(AdactaModel.PROP_EMPLOYEE_DEPARTMENT, dep);
		if (dpcodes.size()==0 && dep.equalsIgnoreCase("SOLLI")){
			dpcodes.add("DPALL");
			dpcodes.add("DPEXMIVD");
		}

		dossierPropsNew.put(AdactaModel.PROP_EMPLOYEE_DP_CODES, dpcodes);
		dossierPropsNew.put(AdactaModel.PROP_EMPLOYEE_NUMBER, dossier.getEmplid());
		dossierPropsNew.put(AdactaModel.PROP_EMPLOYEE_NAME, dossier.getEmplname());
		dossierPropsNew.put(AdactaModel.PROP_EMPLOYEE_MRN, dossier.getEmplmrn());

		return dossierPropsNew;
	}

	private void logProps(final Map<QName, Serializable> dossierProps) {
		final StringBuilder s = new StringBuilder("Dossier properties");
		for (final Entry<QName, Serializable> entry : dossierProps.entrySet()) {
			s.append("\n\t" + entry.getKey() + ": " + entry.getValue());
		}

		// Log props at once, as we're likely in an environment with many
		// threads running concurrently
		SyncLogger.info(s.toString());
	}

	private void updateDocuments(NodeRef pDossier, String emplid) {
		SyncLogger.info("updating documents");
		List<ChildAssociationRef> children = getNodeService().getChildAssocs(pDossier);
			Iterator<ChildAssociationRef> i = children.iterator();
			while (i.hasNext()) {
				ChildAssociationRef ref = i.next();
				NodeRef child = ref.getChildRef();
				String docWorkDossier = (String)getNodeService().getProperty(child, AdactaModel.PROP_DOC_WORK_DOSSIER) ;
				if (getNodeService().getType(child).equals(AdactaModel.TYPE_DOCUMENT) && !isDeleteCode(docWorkDossier)) {
					getNodeService().setProperty(child, AdactaModel.PROP_EMPLOYEE_NUMBER, emplid);
				}				
			}
	}


    private boolean isDeleteCode(String docWorkDossier) {
		if (StringUtils.isEmpty(docWorkDossier)){
			return false;
		}
		if (docWorkDossier.length()>2){
			return false;
		}
		if (docWorkDossier.equals("V1") || 
				docWorkDossier.equals("V2") || 
				docWorkDossier.equals("V3") || 
				docWorkDossier.equals("V4") || 
				docWorkDossier.equals("V5") || 
				docWorkDossier.equals("V6") || 
				docWorkDossier.equals("V7") || 
				docWorkDossier.equals("V8")){
			return true;
		}
		return false;
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

	public AdactaFileFolderService getAdactaFileFolderService() {
		return adactaFileFolderService;
	}

	public void setAdactaFileFolderService(AdactaFileFolderService adactaFileFolderService) {
		this.adactaFileFolderService = adactaFileFolderService;
	}

	public AdactaSearchService getAdactaSearchService() {
		return adactaSearchService;
	}

	public void setAdactaSearchService(AdactaSearchService adactaSearchService) {
		this.adactaSearchService = adactaSearchService;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
    
}
