package nl.defensie.adacta.action.schedule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.alfresco.repo.i18n.MessageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import nl.defensie.adacta.service.AdactaLdapRoleSyncService;

public class RoleSyncActionExecuterTest {

	@Mock
	MessageService messageServiceMock;
	@Mock
	AdactaLdapRoleSyncService adactaLdapSyncServiceMock;

	RolesSyncActionExecuter rolesSyncActionExecuter;

	@Before

	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		rolesSyncActionExecuter = new RolesSyncActionExecuter() {

			public AdactaLdapRoleSyncService getAdactaLdapSyncService() {
				return adactaLdapSyncServiceMock;
			}

			public MessageService getMessageService() {
				return messageServiceMock;
			}
		};
	}

	@Test
	public void getRoleAuthoritiesBeanTest() {
		when(messageServiceMock.getMessage("PSROLES")).thenReturn("P-DOSSIER RAADPLEGEN,P-DOSSIER SIB RAADPLEGEN");
		when(messageServiceMock.getMessage("P-DOSSIER_RAADPLEGEN")).thenReturn("LH02P01DISR01,LH02P01DISR02");
		when(messageServiceMock.getMessage("P-DOSSIER_SIB_RAADPLEGEN")).thenReturn("LH02P01DISR03,LH02P01DISR04");
		rolesSyncActionExecuter.getRoleAuthoritiesBean();
		assertEquals(rolesSyncActionExecuter.roleAuthoritiesBean.getRoleAuthorities().size(), 2);
	}

}