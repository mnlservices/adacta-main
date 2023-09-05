package nl.defensie.adacta.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.AdactaRoleSyncBean;
import nl.defensie.adacta.utils.RoleAuthoritiesBean;
import nl.defensie.adacta.utils.SyncLogger;

public class AdactaRoleSyncService {

	private static final String ADACTA_SERVICE_USER = "sa_adacta";
	private static final String NAME_ROLESYNC_BATCH = "ROLESYNC_BATCH";
	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "AdactaRoleSyncService");
	private int batchSize = 200;
	private int batchThreads = 20;

	ArrayList<AdactaRoleSyncBean> adactaMultipleRolesSyncBeans = new ArrayList<>();
	public RoleAuthoritiesBean roleAuthoritiesBean;

	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	@Qualifier("PersonService")
	protected PersonService personService;
	@Autowired
	@Qualifier("AuthorityService")
	protected AuthorityService authorityService;
	@Autowired
	@Qualifier("TransactionService")
	protected TransactionService transactionService;

	/**
	 * Starts a batch process to loop through all Alfresco users and select those
	 * that have a role (Arbeidsplaats-rolgroep). Returns a bean with a list of
	 * combined role names and employee ID's, separated by an underscore, and a list
	 * of ADACTA Authorities. for example 'R00123456_00000012345'
	 * 
	 * @return adactaRolesXAuthorities
	 */
	public ArrayList<AdactaRoleSyncBean> getAdactaRoles(RoleAuthoritiesBean roleAuthorities) {
		roleAuthoritiesBean = roleAuthorities;
		SyncLogger.info("start getting adacta authorities ");
		final int pageSize = 5000;
		final List<Pair<QName, Boolean>> sortProperties = new ArrayList<Pair<QName, Boolean>>();
		sortProperties.add(new Pair<QName, Boolean>(ContentModel.PROP_USERNAME, false));

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
		// printAdactaRoles(adactaMultipleRolesSyncBeans);
		return adactaMultipleRolesSyncBeans;
	}

	@SuppressWarnings("unused")
	private void printAdactaRoles(ArrayList<AdactaRoleSyncBean> adactaMultipleRolesSyncBeans2) {
		for (AdactaRoleSyncBean arsb : adactaMultipleRolesSyncBeans2) {
			System.out.println(arsb.getEmplid());
			System.out.println(arsb.getRole());
			System.out.println(arsb.getUserName());
			System.out.println(arsb.getGroups().toString());
			System.out.println(arsb.getRoles().toString());
			System.out.println("------------------------------------");
		}

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

		final BatchProcessor<PersonInfo> processor = new BatchProcessor<PersonInfo>(NAME_ROLESYNC_BATCH,
				transactionService.getRetryingTransactionHelper(), provider, batchThreads, batchSize, null, null,
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
				doProcess(entry);
			}

			@Override
			public void afterProcess() throws Throwable {
				// Clear authentication
				AuthenticationUtil.clearCurrentSecurityContext();
			}
		};

		final int invocations = processor.process(worker, true);
		SyncLogger
				.info("Completed synchronization of " + users.size() + " users using " + invocations + " invocations");
	}

	protected void doProcess(final PersonInfo entry) {
		try {
			Set<String> authorities = getAuthorityService().getAuthoritiesForUser(entry.getUserName());
			// SyncLogger.info("handling user "+entry.getUserName());
			NodeRef personNode = entry.getNodeRef();
			String emplid = (String) getNodeService().getProperty(personNode, AdactaModel.PROP_EMPLOYEE_ID);
			ArrayList<String> adactaGroups = new ArrayList<String>();
			ArrayList<String> roles = new ArrayList<String>();
			ArrayList<String> roleXemplid = new ArrayList<String>();
			for (String auth : authorities) {
				String displayName = getAuthorityService().getAuthorityDisplayName(auth);
				String groupName = auth.substring(6, auth.length());
				// bijvb: auth = 'GROUP_R00495459
				// displayname='Adviseur.......(R00495459)(Arbeidsplaats-rolgroep)
				if (displayName.contains("Arbeidsplaats-rolgroep")) {
					roles.add(groupName);
				}
				if (hasAdactaRights(auth)) {
					adactaGroups.add(groupName);
				}
			}
			// add emplids to roles
			for (String r : roles) {
				roleXemplid.add(r + "_" + emplid);
			}

			if (roleXemplid.size() > 0) {
				// multiple roles, not individually coupled to groups - we don't
				// know which role has which group
				AdactaRoleSyncBean amrsb = new AdactaRoleSyncBean();
				amrsb.setUserName(entry.getUserName());
				amrsb.setRoles(roleXemplid);
				amrsb.setEmplid(emplid);
				amrsb.setGroups(adactaGroups);
				adactaMultipleRolesSyncBeans.add(amrsb);
			}
		} catch (final Throwable t) {
			SyncLogger.error("Error syncing user " + entry.getUserName(), t);
			throw t;
		}

	}

	private boolean hasAdactaRights(String auth) {
		if (auth.startsWith("GROUP_LH02P01DIS")) {
			return true;
		}
		for (String non : roleAuthoritiesBean.getOtherRoles()) {
			if (auth.contains(non)) {
				return true;
			}
		}
		return false;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public PersonService getPersonService() {
		return personService;
	}

	public TransactionService getTransactionService() {
		return transactionService;
	}

	public AuthorityService getAuthorityService() {
		return authorityService;
	}

}