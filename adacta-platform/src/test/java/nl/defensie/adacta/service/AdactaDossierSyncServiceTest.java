package nl.defensie.adacta.service;


import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.namespace.QName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.AdactaDossier;

public class AdactaDossierSyncServiceTest {

	private static final String SPACE = " ";

	@Mock
    AdactaSearchService adactaSearchServiceMock;
	
	@Mock
    NodeService nodeServiceMock;

	@Mock
    AdactaFileFolderService adactaFileFolderServiceMock;
	
	AdactaDossierSyncService adactaDossierSyncService;
	
	@Before
	    public void setUp() throws Exception {
	        MockitoAnnotations.initMocks(this);

	        adactaDossierSyncService = new AdactaDossierSyncService() {
	        	
	            public AdactaSearchService getAdactaSearchService() {
	                return adactaSearchServiceMock;
	            }

	            public NodeService getNodeService() {
	                return nodeServiceMock;
	            }
	            
	            public AdactaFileFolderService getAdactaFileFolderService() {
	                return adactaFileFolderServiceMock;
	            }

	        };
	    }
	
	
	/**
	 * Test the change scenario - the dossier exists so the node will be updated. 
	 * Emplid is unchanged so the underlying documents will not be updated. 
	 */
	@Test
	public void synchronizeExistingDossier1Test(){
		AdactaDossier dossier = getTestDossier1();
		NodeRef pDossier = new NodeRef("workspace://SpacesStore/e9d006a9-d0e1-44d5-9327-a7d10ac6a91a");
		String emplid ="000370500";
		ArrayList<String> dpcodes = new ArrayList<>(Arrays.asList(dossier.getDpcodes().split(SPACE)));
	    when(adactaSearchServiceMock.getPersonnelFileByBSN(dossier.getEmplbsn(), QueryConsistency.TRANSACTIONAL)).thenReturn(pDossier);
	    when(nodeServiceMock.getProperty(pDossier, AdactaModel.PROP_EMPLOYEE_DP_CODES)).thenReturn(dpcodes);
	    when(nodeServiceMock.getProperty(pDossier, AdactaModel.PROP_EMPLOYEE_NUMBER)).thenReturn(emplid);
	    
	    adactaDossierSyncService.synchronizeDossier(dossier, true);

	    //verify dossier is updated
	    verify(nodeServiceMock, atLeastOnce()).addAspect(isA(NodeRef.class), isA(QName.class), isA(HashMap.class));
	    //verify documents are not updated
	    verify(nodeServiceMock, never()).getChildAssocs(pDossier);
	}

	/**
	 * Test the change scenario - the dossier exists so the node will be updated. 
	 * Emplid changes so the underlying documents will be updated. 
	 * There are no documents in the dossiers however.
	 */
	@Test
	public void synchronizeExistingDossier2Test(){
		AdactaDossier dossier = getTestDossier1();
		NodeRef pDossier = new NodeRef("workspace://SpacesStore/e9d006a9-d0e1-44d5-9327-a7d10ac6a91a");
		String emplid ="000377555";
		ArrayList<String> dpcodes = new ArrayList<>(Arrays.asList(dossier.getDpcodes().split(SPACE)));
	    when(adactaSearchServiceMock.getPersonnelFileByBSN(dossier.getEmplbsn(), QueryConsistency.TRANSACTIONAL)).thenReturn(pDossier);
	    when(nodeServiceMock.getProperty(pDossier, AdactaModel.PROP_EMPLOYEE_DP_CODES)).thenReturn(dpcodes);
	    when(nodeServiceMock.getProperty(pDossier, AdactaModel.PROP_EMPLOYEE_NUMBER)).thenReturn(emplid);
	    List<ChildAssociationRef> children = new ArrayList<>();
	    when(nodeServiceMock.getChildAssocs(pDossier)).thenReturn(children);
	    
	    adactaDossierSyncService.synchronizeDossier(dossier, true);

	    //verify dossier is updated
	    verify(nodeServiceMock, atLeastOnce()).addAspect(isA(NodeRef.class), isA(QName.class), isA(HashMap.class));
	    //verify documents are not updated
	    verify(nodeServiceMock, atLeastOnce()).getChildAssocs(pDossier);
	}

	private AdactaDossier getTestDossier1() {
		AdactaDossier dossier = new AdactaDossier();
		dossier.setDpcodes("DPALL DPEXMIVD");
		dossier.setEmplbsn("NLD-11111111");
		dossier.setEmpldep("KM");
		dossier.setEmplid("000370500");
		dossier.setEmplmrn("12345678");
		dossier.setEmplname("Tinus Arts");
		dossier.setRowsecclass("DPALL");
		return dossier;
	}
		
		
	}
