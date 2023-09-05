package nl.defensie.adacta.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import nl.defensie.adacta.utils.RoleAuthoritiesBean;

public class Adacta2DatabaseServiceTest {

	Adacta2DatabaseService adacta2DatabaseService;
	
	@Before
	    public void setUp() throws Exception {
	        MockitoAnnotations.initMocks(this);

	        adacta2DatabaseService = new Adacta2DatabaseService() {

	        };
	    }
	
	
	@Test
	public void CDMqueryTest(){
		RoleAuthoritiesBean roleAuthoritiesBean = new RoleAuthoritiesBean();
		Map<String, List<String>> roleAuthorities = new HashMap<String, List<String>>();
		List<String> list1 = new ArrayList<>();
		list1.add("LH02P01DISF01");
		list1.add("LH02P01DISR01");
		roleAuthorities.put("P-DOSSIER SIB RAADPLEGEN", list1);
		List<String> list2 = new ArrayList<>();
		list1.add("LH02P01DISF02");
		list1.add("LH02P01DISR02");
		roleAuthorities.put("P-DOSSIER ALGEMEEN RAADPLEGEN", list2);
		roleAuthoritiesBean.setRoleAuthorities(roleAuthorities);
		String result = adacta2DatabaseService.getRoleSyncQuery(roleAuthoritiesBean);
		String test = "SELECT DISTINCT 'R' || sysadm.PS_MVD_ARBEIDSPLTS.Position_nbr AS role, sysadm.PS_MVD_AP_ROLLEN.ROLENAME as authority, sysadm.PS_MVD_PROFIELEN.EMPLID as emplid FROM sysadm.PS_MVD_ARBEIDSPLTS INNER JOIN sysadm.PS_MVD_AP_ROLLEN ON sysadm.PS_MVD_ARBEIDSPLTS.MVD_ARBEIDSPLTS_SK = sysadm.PS_MVD_AP_ROLLEN.MVD_ARBEIDSPLTS_SK INNER JOIN sysadm.PS_MVD_PROFIELEN ON sysadm.PS_MVD_ARBEIDSPLTS.POSITION_NBR = sysadm.PS_MVD_PROFIELEN.POSITION_NBR  WHERE TRIM(sysadm.PS_MVD_AP_ROLLEN.ROLENAME)  IS NOT NULL  AND (UPPER(sysadm.PS_MVD_AP_ROLLEN.ROLENAME) = 'P-DOSSIER ALGEMEEN RAADPLEGEN' OR UPPER(sysadm.PS_MVD_AP_ROLLEN.ROLENAME) = 'P-DOSSIER SIB RAADPLEGEN' )  AND (((sysadm.PS_MVD_PROFIELEN.EFFDT_FROM)<CURRENT_DATE+14) AND ((sysadm.PS_MVD_PROFIELEN.EFFDT_TO)>CURRENT_DATE-60  OR (sysadm.PS_MVD_PROFIELEN.EFFDT_TO) Is Null)  AND ((sysadm.PS_MVD_ARBEIDSPLTS.EFF_STATUS='A')  AND sysadm.PS_MVD_ARBEIDSPLTS.MVD_CURRENT='Y'))";
		assertEquals(result,test);
	}
}