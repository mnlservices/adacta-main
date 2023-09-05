package nl.defensie.adacta.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.alfresco.repo.i18n.MessageService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.RoleAuthoritiesBean;

public class AdactaRoleSyncServiceTest {

	@Mock
	AuthorityService authorityServiceMock;
	@Mock
	NodeService nodeServiceMock;
	@Mock
	MessageService messageServiceMock;

	AdactaRoleSyncService adactaRoleSyncService;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		adactaRoleSyncService = new AdactaRoleSyncService() {

			public AuthorityService getAuthorityService() {
				return authorityServiceMock;
			}

			public NodeService getNodeService() {
				return nodeServiceMock;
			}

			public MessageService getMessageService() {
				return messageServiceMock;
			}
		};
	}

	@Test
	public void OneGroupsAndOneRoleAdacta() throws NamingException {
		NodeRef noderef = new NodeRef("workspace://SpacesStore/62750f54-04bb-4610-acc9-9b085ed7cf1d");
		String emplid = "00000012345";
		String role = "R00123456";
		Set<String> authorities = new HashSet<String>();
		authorities.add("GROUP_" + role);
		String group = "GROUP_LH02P01DISR01";
		authorities.add(group);
		when(authorityServiceMock.getAuthoritiesForUser("u00xxxx")).thenReturn(authorities);
		when(nodeServiceMock.getProperty(noderef, AdactaModel.PROP_EMPLOYEE_ID)).thenReturn(emplid);
		when(authorityServiceMock.getAuthorityDisplayName("GROUP_" + role)).thenReturn("(Arbeidsplaats-rolgroep)");
		when(authorityServiceMock.getAuthorityDisplayName(group)).thenReturn("");
		when(messageServiceMock.getMessage("OTHER_ADACTA_GROUP")).thenReturn("PPATT01");
		String username = "u00xxxx";
		String firstname = "wim";
		String lastname = "schreurs";
		PersonInfo pi = new PersonInfo(noderef, username, firstname, lastname);
		RoleAuthoritiesBean roleAuthoritiesBean = new RoleAuthoritiesBean();
		List<String> otherRoles = new ArrayList<>();
		roleAuthoritiesBean.setOtherRoles(otherRoles);
		adactaRoleSyncService.roleAuthoritiesBean = roleAuthoritiesBean;
		adactaRoleSyncService.doProcess(pi);
		assertEquals(adactaRoleSyncService.adactaMultipleRolesSyncBeans.size(), 1);
		assertEquals(adactaRoleSyncService.adactaMultipleRolesSyncBeans.get(0).getEmplid(), emplid);
		assertEquals(adactaRoleSyncService.adactaMultipleRolesSyncBeans.get(0).getRoles().get(0), role + "_" + emplid);
		assertEquals(adactaRoleSyncService.adactaMultipleRolesSyncBeans.get(0).getGroups().get(0), "LH02P01DISR01");
	}

	@Test
	public void NoGroupsInAdacta() throws NamingException {
		NodeRef noderef = new NodeRef("workspace://SpacesStore/62750f54-04bb-4610-acc9-9b085ed7cf1d");
		String emplid = "00000012345";
		Set<String> authorities = new HashSet<String>();
		authorities.add("GROUP_R00123456");
		when(authorityServiceMock.getAuthoritiesForUser("u00xxxx")).thenReturn(authorities);
		when(nodeServiceMock.getProperty(noderef, AdactaModel.PROP_EMPLOYEE_ID)).thenReturn(emplid);
		when(authorityServiceMock.getAuthorityDisplayName("GROUP_R00123456")).thenReturn("(Arbeidsplaats-rolgroep)");
		when(messageServiceMock.getMessage("OTHER_ADACTA_GROUP")).thenReturn("PPATT01");
		String username = "u00xxxx";
		String firstname = "wim";
		String lastname = "schreurs";
		PersonInfo pi = new PersonInfo(noderef, username, firstname, lastname);
		RoleAuthoritiesBean roleAuthoritiesBean = new RoleAuthoritiesBean();
		List<String> otherRoles = new ArrayList<>();
		roleAuthoritiesBean.setOtherRoles(otherRoles);
		adactaRoleSyncService.roleAuthoritiesBean = roleAuthoritiesBean;
		adactaRoleSyncService.doProcess(pi);
		assertEquals(adactaRoleSyncService.adactaMultipleRolesSyncBeans.size(), 1);
		assertEquals(adactaRoleSyncService.adactaMultipleRolesSyncBeans.get(0).getGroups().size(), 0);
	}

	@Test
	public void NoRoleInAdacta() throws NamingException {
		NodeRef noderef = new NodeRef("workspace://SpacesStore/62750f54-04bb-4610-acc9-9b085ed7cf1d");
		String emplid = "00000012345";
		Set<String> authorities = new HashSet<String>();
		authorities.add("GROUP_LH02P01DISR01");
		when(authorityServiceMock.getAuthoritiesForUser("u00xxxx")).thenReturn(authorities);
		when(nodeServiceMock.getProperty(noderef, AdactaModel.PROP_EMPLOYEE_ID)).thenReturn(emplid);
		when(authorityServiceMock.getAuthorityDisplayName("GROUP_LH02P01DISR01")).thenReturn("");
		when(messageServiceMock.getMessage("OTHER_ADACTA_GROUP")).thenReturn("PPATT01");
		String username = "u00xxxx";
		String firstname = "wim";
		String lastname = "schreurs";
		PersonInfo pi = new PersonInfo(noderef, username, firstname, lastname);
		RoleAuthoritiesBean roleAuthoritiesBean = new RoleAuthoritiesBean();
		List<String> otherRoles = new ArrayList<>();
		roleAuthoritiesBean.setOtherRoles(otherRoles);
		adactaRoleSyncService.roleAuthoritiesBean = roleAuthoritiesBean;
		adactaRoleSyncService.doProcess(pi);
		assertEquals(adactaRoleSyncService.adactaMultipleRolesSyncBeans.size(), 0);
	}

}