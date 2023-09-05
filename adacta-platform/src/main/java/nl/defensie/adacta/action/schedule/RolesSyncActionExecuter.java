package nl.defensie.adacta.action.schedule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.i18n.MessageService;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.service.Adacta2DatabaseService;
import nl.defensie.adacta.service.AdactaLdapRoleSyncService;
import nl.defensie.adacta.service.AdactaMailService;
import nl.defensie.adacta.service.AdactaRoleSyncChecksService;
import nl.defensie.adacta.service.AdactaRoleSyncService;
import nl.defensie.adacta.utils.CdmRoleSyncBean;
import nl.defensie.adacta.utils.RoleAuthoritiesBean;
import nl.defensie.adacta.utils.SyncLogger;

public class RolesSyncActionExecuter extends ActionExecuterAbstractBase {

	private static final String ROLES_KEY = "PSROLES";
	private static final String ROL_BEHEER = "ROL_BEHEER";
	private static final String OTHER_ADACTA_GROUP = "OTHER_ADACTA_GROUP";
	
	@Autowired
	protected AdactaRoleSyncService adactaRoleSyncService;
	@Autowired
	protected AdactaLdapRoleSyncService adactaLdapSyncService;
	@Autowired
	protected Adacta2DatabaseService adacta2DatabaseService;
	@Autowired
	protected AdactaRoleSyncChecksService adactaRoleSyncChecksService;
	@Autowired
	protected AdactaMailService adactaMailService;
	@Autowired
	@Qualifier("MessageService")
	protected MessageService messageService;

	@Value("${adacta.cron.job.synclogger.path}")
	private String syncloggerLocation;

	public static final String NAME = "rolesSync";
	private static final String PPATT01 = "PPATT01";
	private static final String PPBSG01 = "PPBSG01";


	public Set<String> removeFromLdap = new HashSet<>();
	public Set<String> addToLdap = new HashSet<>();
	//format role_authority
	public List<String> sortedRemoveFromLdap = new ArrayList<>();
	public List<String> sortedAddToLdap = new ArrayList<>();

	public Map<String, List<String>> cdmEmplidsByRole = new HashMap<>();
	public Map<String, List<String>> cdmRolesExploded = new HashMap<>();
	public Map<String, List<String>> ldapAuthorities = new HashMap<>();
	public RoleAuthoritiesBean roleAuthoritiesBean;


	@Override
	protected void executeImpl(final Action action, final NodeRef actionedUponNodeRef) {
		SyncLogger.reset();
		SyncLogger.setLoggerPath(syncloggerLocation);
		SyncLogger.info("Start roles sync job");
		getRoleAuthoritiesBean();
		List<CdmRoleSyncBean> cdmRolesList = adacta2DatabaseService.getCdmRoles(roleAuthoritiesBean);
		List<String> cdmRolesAll = adacta2DatabaseService.getAllCdmRoles(roleAuthoritiesBean);
		List<String> uniqueActiveCdmRoles = getUniqueCdmRoles(cdmRolesList);
		List<String> inactiveCdmRoles = getInactiveCdmRoles(uniqueActiveCdmRoles, cdmRolesAll);
		SyncLogger.info("all cdm roles: "+cdmRolesAll.size()+" active: "+uniqueActiveCdmRoles.size()+" inactive: "+inactiveCdmRoles.size());
		SyncLogger.info("cdmRolesList size "+cdmRolesList.size());	
		cdmRolesExploded = getExplodedCdmRoles(cdmRolesList);

		try {
			adactaLdapSyncService.initContext();

			Set<String> activeCdmRoles = cdmRolesExploded.keySet();
			for (String key:activeCdmRoles) {
				List<String> ldapGroups = returnListIfNull(getAdactaLdapSyncService().getLdapAuthorities(key));
				List<String> cdmGroups = cdmRolesExploded.get(key);
				if (!groupsEqual(ldapGroups, cdmGroups)) {
					process(key, ldapGroups, cdmGroups);
				}
			}
			for (String r: inactiveCdmRoles) {
				List<String> ldapGroups = returnListIfNull(getAdactaLdapSyncService().getLdapAuthorities(r));
				if (!ldapGroups.isEmpty()) {
					for (String l:ldapGroups) {
						removeFromLdap.add(r+"_"+l);
					}
				}
			}
			SyncLogger.info("end loop, closing context...");
			adactaLdapSyncService.closeContext();
		} catch (Exception e) {
			SyncLogger.error("exception while processing role entries", e);
		}
		sortCollections();
		doChecks();
		sendEmails();
		SyncLogger.info("Done roles sync job");
	}


	/**
	 * Get Cdm roles that do not have a single entry with an active status.
	 * If Ldap has authorities connected to these roles, they must be removed.
	 * @param uniqueActiveCdmRoles
	 * @param cdmRolesAll
	 * @return
	 */
	private List<String> getInactiveCdmRoles(List<String> uniqueActiveCdmRoles, List<String> cdmRolesAll) {
		List<String> inactiveRoles = new ArrayList<>();
		SyncLogger.info("getInactive roles "+uniqueActiveCdmRoles.get(0)+" "+cdmRolesAll.get(0));
		for (String r: cdmRolesAll) {
			if (!uniqueActiveCdmRoles.contains(r)) {
				inactiveRoles.add(r);
			}
		}
		return inactiveRoles;
	}



	/**
	 * Get a list of unique rol cdm role names (R00123456)
	 * @param cdmRolesList
	 * @return
	 */
	private List<String> getUniqueCdmRoles(List<CdmRoleSyncBean> cdmRolesList) {
		List<String> m = new ArrayList<>();
		for (CdmRoleSyncBean cbean : cdmRolesList) {
			if (!m.contains(cbean.getRole())) {
				m.add(cbean.getRole());
			} 
		}
		return m;
	}
	/**
	 * If Ldap has more authorities than Cdm, they must be removed.
	 * If it has less, they must be added.
	 * @param key
	 * @param ldapGroups
	 * @param cdmGroups
	 */
	private void process(String key, List<String> ldapGroups, List<String> cdmGroups) {
		for (String l: ldapGroups) {
			if (!cdmGroups.contains(l)) {
				//ldapgroup not found in cdm -> remove
				removeFromLdap.add(key+"_"+l);
				SyncLogger.info("remove from ldap "+key+"_"+l+", not found in cdm "+cdmGroups.toString() );
			}
		}
		for (String c: cdmGroups) {
			if (!ldapGroups.contains(c)) {
				//cdm group not in ldap -> add
				if (c.equalsIgnoreCase(PPATT01) && !hasBatchClass(cdmGroups)){
					SyncLogger.info("group "+c + " not added for role "+key+" , no kofax batch class found" );
					continue;
				}
				if (c.equalsIgnoreCase(PPBSG01) && !hasBeheerRol(cdmGroups)){
					SyncLogger.info("group "+c + " not added for role "+key+" , no management role (F05) found" );
					continue;
				}
				addToLdap.add(key+"_"+c);
			}
		}
	}


	/**
	 * Performs a number of checks on the resulting lists.
	 */
	private void doChecks(){
		sortedRemoveFromLdap = adactaRoleSyncChecksService.excludeManagers(sortedRemoveFromLdap, roleAuthoritiesBean);
		sortedRemoveFromLdap = adactaRoleSyncChecksService.removeSingleKofaxAuthorities(sortedRemoveFromLdap);
		sortedAddToLdap = adactaRoleSyncChecksService.removeSingleKofaxAuthorities(sortedAddToLdap);
	}
	
	/**
	 * Sort result lists alphabetically.
	 */
	protected void sortCollections() {
		SyncLogger.info("sorting...");
		sortedAddToLdap = new ArrayList<String>(addToLdap);
		Collections.sort(sortedAddToLdap);
		sortedRemoveFromLdap = new ArrayList<String>(removeFromLdap);
		Collections.sort(sortedRemoveFromLdap);
	}
	
	private boolean hasBatchClass(List<String> cdmList) {
		if (!cdmList.contains("LH02P01DISF06") && !cdmList.contains("LH02P01DISF07")
				&& !cdmList.contains("LH02P01DISF08")) {
			return false;
		}
		return true;
	}
	private boolean hasBeheerRol(List<String> cdmList) {
		if (!cdmList.contains("LH02P01DISF05")) {
			return false;
		}
		return true;
	}
	private String getRole(String rolx) {
		if (null == rolx) {
			return "";
		} else {
			String[] r = rolx.split("_");
			return r[0];
		}
	}
	private String getAuthority(String rolx) {
		if (null == rolx) {
			return "";
		} else {
			String[] r = rolx.split("_");
			return r[1];
		}
	}
	private boolean groupsEqual(List<String> ldapGroups, List<String> cdmGroups) {
		for (String cdm : cdmGroups) {
			if (!ldapGroups.contains(cdm)) {
				return false;
			}
		}
		for (String ldap : ldapGroups) {
			if (!cdmGroups.contains(ldap)) {
				return false;
			}
		}
		return true;
	}
	
	private List<String> returnListIfNull(List<String> l) {
		if (null == l) {
			return new ArrayList<String>();
		}
		return l;
	}

	@Override
	protected void addParameterDefinitions(final List<ParameterDefinition> paramList) {
	}

	private void sendEmails() {
		SyncLogger.info("sending mails - not in adacta: " + sortedAddToLdap.size() + " not in cdm: " + sortedRemoveFromLdap.size());
		if (sortedAddToLdap.isEmpty()) {
			SyncLogger.info("not In Adacta is empty ");
		} else {
			for (String nia : sortedAddToLdap) {
			SyncLogger.info("add to ldap: " + nia);
			}
		}
		
		if (sortedRemoveFromLdap.isEmpty()) {
			SyncLogger.info("remove from Ldap is empty ");
		} else {
			for (String nic : sortedRemoveFromLdap) {
				SyncLogger.info("remove from ldap: " + nic);
			}
		}
		//String to Bean && send email
		ArrayList<CdmRoleSyncBean> rlist = new ArrayList<>();
		for (String sortedRemove :sortedRemoveFromLdap){
			CdmRoleSyncBean crsb = new CdmRoleSyncBean();
			crsb.setRole(getRole(sortedRemove));
			crsb.setAuthority(getAuthority(sortedRemove));
			rlist.add(crsb);
		}
		adactaMailService.sendRolesSyncMailWithAttachment(roleAuthoritiesBean, rlist, false);

		ArrayList<CdmRoleSyncBean> alist = new ArrayList<>();
		for (String sortedAdd :sortedAddToLdap){
			CdmRoleSyncBean crsb = new CdmRoleSyncBean();
			crsb.setRole(getRole(sortedAdd));
			crsb.setAuthority(getAuthority(sortedAdd));
			alist.add(crsb);
		}
		adactaMailService.sendRolesSyncMailWithAttachment(roleAuthoritiesBean, alist, true);		
	}

	/**
	 * Translates PeopleSoft roles to Alfresco Adacta authorities. Each
	 * PeopleSoft role corresponds to multiple Adacta authorities, hence the
	 * name 'exploded'.
	 * 
	 * @param cdmRolesList
	 * @return
	 */
	public Map<String, List<String>> getExplodedCdmRoles(List<CdmRoleSyncBean> cdmRolesList) {
		Map<String, List<String>> cdmRoles = new HashMap<>();
		for (CdmRoleSyncBean b : cdmRolesList) {
			Set<String> groups = new HashSet<>();
			if (roleAuthoritiesBean.getRoleAuthorities().containsKey(b.getAuthority().toUpperCase())) {
				List<String> auths = roleAuthoritiesBean.getRoleAuthorities().get(b.getAuthority().toUpperCase());
				for (String gr : auths) {
					groups.add(gr);
				}
			}
			// een rolgroep-emplid combinatie kan meerdere keren voorkomen,
			// omdat 1 rolgroep gekoppeld kan zijn aan 2 peoplesoft rollen
			String key = b.getRole();
			if (cdmRoles.containsKey(key)) {
				// check if groups to add already exist
				List<String> auth = cdmRoles.get(key);
				for (String g : groups) {
					if (!auth.contains(g)) {
						auth.add(g);
					}
				}
				cdmRoles.replace(key, auth);
			} else {
				cdmRoles.put(key, new ArrayList<String>(groups));
			}
		}
		return cdmRoles;
	}
	
	protected void getRoleAuthoritiesBean() {
		roleAuthoritiesBean = new RoleAuthoritiesBean();
		String rolesKey = ROLES_KEY;
		String l = getMessageService().getMessage(rolesKey);
		SyncLogger.info("rolesKey="+l);
		String[] la =  l.split(",");
		HashMap<String, List<String>> hm = new HashMap<String, List<String>>();
		for (String aut:la) {
			String autnew = aut.replaceAll(" ", "_");
			String v = getMessageService().getMessage(autnew.toUpperCase());
			String[] va = v.split(",");
			SyncLogger.info("putting key "+aut+" "+(Arrays.asList(va)).toString());
			hm.put(aut, Arrays.asList(va));
		}
		SyncLogger.info("hm size "+hm.size());
		roleAuthoritiesBean.setRoleAuthorities(hm);
		String o = getMessageService().getMessage(OTHER_ADACTA_GROUP);
		if (o!=null) {
			String[] vo = o.split(",");
			roleAuthoritiesBean.setOtherRoles(Arrays.asList(vo));
		}else {
			roleAuthoritiesBean.setOtherRoles(new ArrayList<String>());
		}
		String b = getMessageService().getMessage(ROL_BEHEER);	
		if (b!=null) {
			String[] vb = b.split(",");
			roleAuthoritiesBean.setBeheerders(Arrays.asList(vb));
		}else {
			roleAuthoritiesBean.setBeheerders(new ArrayList<String>());			
		}
	}

	public AdactaLdapRoleSyncService getAdactaLdapSyncService() {
		return adactaLdapSyncService;
	}

	public void setAdactaLdapSyncService(AdactaLdapRoleSyncService adactaLdapSyncService) {
		this.adactaLdapSyncService = adactaLdapSyncService;
	}


	public Map<String, List<String>> getCdmEmplidsByRole() {
		return cdmEmplidsByRole;
	}

	public void setCdmEmplidsByRole(Map<String, List<String>> cdmEmplidsByRole) {
		this.cdmEmplidsByRole = cdmEmplidsByRole;
	}

	public Map<String, List<String>> getCdmRolesExploded() {
		return cdmRolesExploded;
	}


	public MessageService getMessageService() {
		return messageService;
	}


	public void setMessageService(MessageService messageService) {
		this.messageService = messageService;
	}

}