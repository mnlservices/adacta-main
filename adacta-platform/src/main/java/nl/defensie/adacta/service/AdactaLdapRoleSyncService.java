package nl.defensie.adacta.service;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.SyncLogger;

public class AdactaLdapRoleSyncService {

    QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "AdactaLdapRoleSyncService");
    
	private static final String BASE = "ou=User,ou=Role,ou=Groups,dc=mod,dc=nl";
	
	private DirContext ctx;

	@Value("${ldap.authentication.java.naming.provider.url}")
	private String adactaLdapTestUrl;

	@Value("${ldap.synchronization.java.naming.security.principal}")
	private String adactaLdapTestPrincipal;

	@Value("${ldap.synchronization.java.naming.security.credentials}")
	private String adactaLdapTestCredentials;

	public List<String> getLdapAuthorities(String role) throws NamingException {
		List<String> groups = new ArrayList<>();;
		try {
			DirContext ctx = getCtx();
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			final String ldapSearchBase = BASE;
			NamingEnumeration<SearchResult> ne = ctx.search(ldapSearchBase, String.format("cn=%s", role), searchControls);
			while (ne.hasMore()) {
				String group = "";
				SearchResult sr = ne.next();
				if (sr.getAttributes() != null && sr.getAttributes().get("memberOf") != null) {
					NamingEnumeration<?> ne2 = sr.getAttributes().get("memberOf").getAll();
					while (ne2.hasMore()) {
						Object a = (Object) ne2.next();
						if (a instanceof String) {
							String as = (String) a;
							// String=CN=LH02P01DISF08,OU=Data,OU=Groups,DC=mod,DC=nl
							// String=CN=ZZPPBWX01,OU=Data,OU=Groups,DC=mod,DC=nl
							//TODO get from propertyfile
							if (as.contains("LH02P01DIS")) {
								group = as.substring(3, 16);
								groups.add(group);
							}
							if (as.contains("PPATT01")) {
								group = as.substring(3, 10);
								groups.add(group);
							}
							if (as.contains("ZZPPBWX01")) {
								group = as.substring(3, 12);
								groups.add(group);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			SyncLogger.error("error getting ldap role "+role, e);
		}
		//SyncLogger.info("checked ldap, found "+groups.size()+" ldap groups for " + role);
		return groups;
	}

	public void initContext() {
		SyncLogger.info("initializing ldap context");
		DirContext dctx = null;
		try {
			Hashtable<String, Object> env = new Hashtable<String, Object>();
			env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(javax.naming.Context.PROVIDER_URL, adactaLdapTestUrl);
			env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
			env.put(javax.naming.Context.SECURITY_PRINCIPAL, adactaLdapTestPrincipal);
			env.put(javax.naming.Context.SECURITY_CREDENTIALS, adactaLdapTestCredentials);
			dctx = new InitialDirContext(env);
		} catch (Exception e) {
			SyncLogger.error("unable to create directory context ldap", e);
		}
		this.ctx = dctx;
	}

	public void closeContext() {
		if (null != ctx) {
			try {
				ctx.close();
			} catch (Exception e) {
				SyncLogger.error("error closing ldap directory context", e);
			}
		}
	}
	
	public DirContext getCtx() {
		return ctx;
	}

	public void setCtx(DirContext ctx) {
		this.ctx = ctx;
	}


}
