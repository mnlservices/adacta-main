package nl.defensie.adacta.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.model.AdactaModel;

/**
 * Service for user and group (authorities) management.
 * 
 * @author Rick de Rooij
 *
 */
public class AdactaAuthorityService {

	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "AuthorityService");

	private static final String PREFIX_GROUP = "GROUP_";
	private static final String GROUP_ADACTA_INVOERDER = "GROUP_ADACTA_INVOERDER";
	public static final String GROUP_ADACTA_BEHEERDER = "GROUP_ADACTA_BEHEERDER";
	private static final String GROUP_ALFRESCO_ADMINISTRATORS = "GROUP_ALFRESCO_ADMINISTRATORS";

	protected Logger LOGGER = Logger.getLogger(this.getClass());

	@Value("${adacta.test.user.password}")
	private String userPassword;

	@Autowired
	@Qualifier("AuthorityService")
	protected AuthorityService authorityService;
	@Autowired
	@Qualifier("AuthenticationService")
	protected MutableAuthenticationService authenticationService;
	@Autowired
	@Qualifier("PersonService")
	protected PersonService personService;

	/**
	 * Create user based on map. The password is configured in global props file.
	 * 
	 * @param personMap
	 * @return NodeRef created user.
	 */
	public NodeRef createUser(Map<QName, Serializable> personMap) {
		String userName = (String) personMap.get(ContentModel.PROP_USERNAME);
		if (userName == null) {
			LOGGER.warn("Cannot create user. No user name provided in person map.");
			return null;
		}
		try {
			// Only create user if userName not exists
			if (!personService.personExists(userName)) {
				NodeRef person = personService.createPerson(personMap);

				authenticationService.createAuthentication(userName, userPassword.toCharArray());
				authenticationService.setAuthenticationEnabled(userName, true);

				return person;
			}
		} catch (Exception e) {
			LOGGER.error("Could not create user '" + userName + "'.", e);
		}
		return null;
	}

	/**
	 * Create root group with default zones. The display name is without GROUP_ prefix.
	 * 
	 * @param groupName
	 *            String name include GROUP_ prefix.
	 */
	public void createGroup(final String groupName) {
		createGroup(groupName, groupNameExcludePrefix(groupName));
	}

	/**
	 * Create root group with default zones.
	 * 
	 * @param groupname
	 */
	public void createGroup(final String groupName, final String groupDisplayName) {
		createGroup(groupName, groupDisplayName, authorityService.getDefaultZones());
	}

	/**
	 * Create root group with provided zones.
	 * 
	 * @param groupname
	 * @param zones
	 */
	public void createGroup(final String groupName, final String groupDisplayName, final Set<String> zones) {
		boolean groupExists = groupExists(groupName);
		if (!groupExists) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Create authority '%s  with display name '%s'.", groupNameExcludePrefix(groupName), groupNameExcludePrefix(groupDisplayName)));
			}
			authorityService.createAuthority(AuthorityType.GROUP, groupNameExcludePrefix(groupName), groupNameExcludePrefix(groupDisplayName), zones);
		}
	}

	/**
	 * Add existing group to the existing parent group.
	 * 
	 * @param childGroupName
	 *            the name of the sub group.
	 * @param parentGroupName
	 *            the name of the parent group.
	 * @return true if it has been added or already existed as a sub group.
	 */
	public boolean addGroupToGroup(final String childGroupName, final String parentGroupName) {
		boolean result = false;
		try {
			String parentGroup = groupNameIncludePrefix(parentGroupName);
			String childGroup = groupNameIncludePrefix(childGroupName);

			if (!groupHasSubgroup(parentGroup, childGroup)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Add child group '%s' to parent group '%s'.", childGroup, parentGroup));
				}
				authorityService.addAuthority(parentGroup, childGroup);
			}
			return true;

		} catch (Exception e) {
			LOGGER.error("Unable to add group '" + childGroupName + "' to group '" + parentGroupName + "'.", e);
		}
		return result;
	}

	/**
	 * Add an existing user to an existing group. If the username already is a member of the group, no change is persisted (and true is returned)
	 * 
	 * @param userName
	 *            String user name of the existing user
	 * @param parentGroupName
	 *            Group name of the existing group
	 * @return true if the user is assigned (or was assigned to the group already).
	 */
	public boolean addUserToGroup(final String userName, final String parentGroupName) {
		boolean result = false;
		try {
			if (personService.personExists(userName) && groupExists(parentGroupName) && groupHasUser(groupNameIncludePrefix(parentGroupName), userName) == false) {
				authorityService.addAuthority(groupNameIncludePrefix(parentGroupName), userName);
			}
			return true;

		} catch (Exception e) {
			LOGGER.error("Unable to assigne user to group.", e);
		}
		return result;
	}

	/**
	 * Checks if user is member of provided group.
	 * 
	 * @param parentGroupName
	 * @param userName
	 * @return Boolean true if user is member
	 */
	private boolean groupHasUser(final String parentGroupName, final String userName) {
		boolean result = false;
		try {
			final String longUserName = authorityService.getName(AuthorityType.USER, userName);
			if (groupExists(parentGroupName) && authorityService.authorityExists(longUserName)) {
				Set<String> authorities = authorityService.getContainedAuthorities(AuthorityType.USER, parentGroupName, true);
				Iterator<String> groupIterator = authorities.iterator();
				while (groupIterator.hasNext()) {
					String nextGroup = groupIterator.next();
					if (nextGroup.equals(longUserName)) {
						result = true;
						break;
					}
				}
				return result;
			}
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return result;
	}

	/**
	 * Checks whether the parent group has a sub group with the given name.
	 * 
	 * @param parentGroup
	 *            the name of the parent group.
	 * @param childGroup
	 *            the name of the sub group.
	 * @return true if its really is a sub group of the parent group.
	 */
	private boolean groupHasSubgroup(final String parentGroup, final String childGroup) {
		boolean result = false;
		try {
			if (groupExists(parentGroup) && groupExists(childGroup)) {
				Set<String> authorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, parentGroup, true);
				Iterator<String> groupIterator = authorities.iterator();
				while (groupIterator.hasNext()) {
					String nextGroup = groupIterator.next();
					if (nextGroup.equals(groupNameIncludePrefix(childGroup))) {
						result = true;
						break;
					}
				}
				return result;
			}
		} catch (Exception e) {
			LOGGER.error("Unable to determine whether group '" + parentGroup + "' has a child group called '" + childGroup + "'.", e);
		}
		return result;
	}

	/**
	 * Get the short name of an group without GROUP_ prefix.
	 * 
	 * @param groupName
	 * @return String group name without the GROUP_ prefix.
	 */
	private String groupNameExcludePrefix(final String groupName) {
		String newGroupName = groupName;
		if (newGroupName.startsWith(PREFIX_GROUP)) {
			newGroupName = newGroupName.substring(PREFIX_GROUP.length(), newGroupName.length());
		}
		return newGroupName;
	}

	/**
	 * Append group name with GROUP_
	 * 
	 * @param groupName
	 * @return String group name.
	 */
	private String groupNameIncludePrefix(final String groupName) {
		String newGroupName = groupName;
		if (!newGroupName.startsWith(PREFIX_GROUP)) {
			newGroupName = PREFIX_GROUP + newGroupName;
		}
		return newGroupName;
	}

	/**
	 * Checks if group exists based on group name.
	 * 
	 * @param groupName
	 * @return Boolean true if group exists.
	 */
	private boolean groupExists(final String groupName) {
		return authorityService.authorityExists(groupNameIncludePrefix(groupName));
	}

	/**
	 * Validated if user is member of group ADACTA_BEHEERDER or ALFRESCO_ADMINISTRATORS group.
	 * 
	 * @param userName
	 * @return true if user is member of one of the groups.
	 */
	public Boolean isUserAdactaAdministrator(final String userName) {
		return AuthenticationUtil.runAsSystem(new RunAsWork<Boolean>() {
			public Boolean doWork() throws Exception {
				Set<String> userGroups = authorityService.getAuthoritiesForUser(userName);
				return userGroups.contains(GROUP_ADACTA_BEHEERDER) || userGroups.contains(GROUP_ALFRESCO_ADMINISTRATORS);
			}
		});
	}

	/**
	 * 
	 * @param userName
	 * @return
	 */
	public Boolean isUserAdactaInvoerderOrBeheerder(final String userName) {
		return AuthenticationUtil.runAsSystem(new RunAsWork<Boolean>() {
			public Boolean doWork() throws Exception {
				Set<String> userGroups = authorityService.getAuthoritiesForUser(userName);
				return userGroups.contains(GROUP_ADACTA_BEHEERDER) || userGroups.contains(GROUP_ADACTA_INVOERDER);
			}
		});
	}

	/**
	 * Get authorities of user as system.
	 * 
	 * @param userName
	 *            String the user name
	 * @return Set<String> list of groups.
	 */
	public Set<String> getAuthoritiesForUserAsSystem(final String userName) {
		return AuthenticationUtil.runAsSystem(new RunAsWork<Set<String>>() {
			public Set<String> doWork() throws Exception {
				return authorityService.getAuthoritiesForUser(userName);
			}
		});
	}

	/**
	 * Get department groups which contain "Afdeling-rolgroep" in display name 
	 * 	(only groups 2nd level up from arbeidsplaats rolgroep).
	 * 
	 * @param userName
	 *            String the user name
	 * @return List<String> list of groups.
	 */
	public List<String> getDepartmentGroups(final String userName) {
		List<String> result = new ArrayList<String>();
		Set<String> firstLevelGroups = authorityService.getContainingAuthorities(AuthorityType.GROUP, userName, true);

		for (String firstLevelGroup : firstLevelGroups) {
			if (authorityService.getAuthorityDisplayName(firstLevelGroup).contains("Arbeidsplaats-rolgroep")) {
				Set<String> secondLevelGroups = authorityService.getContainingAuthorities(AuthorityType.GROUP, firstLevelGroup, true);

				for (String secondLevelGroup : secondLevelGroups) {
					if (authorityService.getAuthorityDisplayName(secondLevelGroup).contains("Afdeling-rolgroep")) {
						Set<String> thirdLevelGroups = authorityService.getContainingAuthorities(AuthorityType.GROUP, secondLevelGroup, true);
						
						for (String thirdLevelGroup : thirdLevelGroups) {
							if (authorityService.getAuthorityDisplayName(thirdLevelGroup).contains("Afdeling-rolgroep")) {
								result.add(thirdLevelGroup);
							}
						}
					}
				}
			}
		}

		return result;
	}
}