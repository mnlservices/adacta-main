package nl.defensie.adacta.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.i18n.MessageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import nl.defensie.adacta.utils.RoleAuthoritiesBean;

public class AdactaRoleSyncChecksServiceTest {

	@Mock
	MessageService messageServiceMock;

	AdactaRoleSyncChecksService adactaRoleSyncChecksService;

	@Before

	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		adactaRoleSyncChecksService = new AdactaRoleSyncChecksService() {

		};
	}

	@Test
	public void checkManagersTest() {
		List<String> sortedRemoveFromLdap = new ArrayList<>();
		sortedRemoveFromLdap.add("R00576215_LH02P01DISR86");
		sortedRemoveFromLdap.add("R00576215_LH02P01DISR87");
		sortedRemoveFromLdap.add("R00555215_LH02P01DISR01");
		sortedRemoveFromLdap.add("R00555215_LH02P01DISR02");
		RoleAuthoritiesBean rab = new RoleAuthoritiesBean();
		List<String> beheerders = new ArrayList<>();
		beheerders.add("R00576215");
		beheerders.add("R001234576");
		beheerders.add("R00654321");
		rab.setBeheerders(beheerders);
		List<String> returned = adactaRoleSyncChecksService.excludeManagers(sortedRemoveFromLdap, rab);
		assertEquals(returned.size(), 2);
	}

	@Test
	public void checkKofaxAuthorities() {
		List<String> sortedAddToLdap = new ArrayList<>();
		sortedAddToLdap.add("R00576215_PPATT01");
		sortedAddToLdap.add("R00576215_LH02P01DISR87");
		sortedAddToLdap.add("R00555216_PPATT01");
		sortedAddToLdap.add("R00555999_LH02P01DISR02");
		List<String> returned = adactaRoleSyncChecksService.removeSingleKofaxAuthorities(sortedAddToLdap);
		assertEquals(returned.size(), 3);

	}

}