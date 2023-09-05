package nl.defensie.adacta.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ActionImpl;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.admin.SysAdminParamsImpl;
import org.alfresco.repo.service.ServiceDescriptorRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaAuthorityService;
import nl.defensie.adacta.service.AdactaPreferenceService;
import nl.defensie.adacta.service.AdactaReportService;
import nl.defensie.adacta.service.AdactaSearchService;

public class DeleteNodeActionExecuterTest {

	@Mock
    AuthenticationService authenticationServiceMock;
	@Mock
    AdactaAuthorityService adactaAuthorityServiceMock;
	@Mock
    AdactaReportService adactaReportServiceMock;
	@Mock
	NodeService nodeServiceMock;
	@Mock
	AdactaSearchService searchServiceMock;
	@Mock
	AdactaPreferenceService adactaPreferenceServiceMock;
	@Mock
	FileFolderService fileFolderServiceMock;
	@Mock
	ChildAssociationRef childAssociationRefMock1;
	@Mock
	ChildAssociationRef childAssociationRefMock2;
	@Mock
	ServiceDescriptorRegistry serviceDescriptorRegistryMock;

	DeleteNodeActionExecuter deleteNodeActionExecuter;
	
	@Before

	public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        deleteNodeActionExecuter = new DeleteNodeActionExecuter() {
        	
            public NodeService getNodeService() {
                return nodeServiceMock;
            }
            public AdactaSearchService getAdactaSearchService() {
                return searchServiceMock;
            }
            public AdactaPreferenceService getAdactaPreferenceService() {
                return adactaPreferenceServiceMock;
            }
            public AuthenticationService getAuthenticationService() {
                return authenticationServiceMock;
            }
            public AdactaAuthorityService getAdactaAuthorityService() {
                return adactaAuthorityServiceMock;
            }
            public FileFolderService getFileFolderService() {
                return fileFolderServiceMock;
            }
            public AdactaReportService getAdactaReportService() {
                return adactaReportServiceMock;
            }
            public ServiceDescriptorRegistry getServiceDescriptorRegistry() {
                return serviceDescriptorRegistryMock;
            }
            public String getReportUUID() {
            	return "UUID-1";
            }
        };
    }
	
	/**
	 *  
	 * 
	 */
	@Test
	public void getContextPageAdactaImportTest() {
		NodeRef nodeRef = new NodeRef("workSpace://SpacesStore/actionedUponNoderef");
		NodeRef parentNodeRef = new NodeRef("workSpace://SpacesStore/parentNoderef");
		NodeRef parentParentNodeRef = new NodeRef("workSpace://SpacesStore/parentParentNoderef");
		when(nodeServiceMock.getPrimaryParent(nodeRef)).thenReturn(childAssociationRefMock1);
		when(childAssociationRefMock1.getParentRef()).thenReturn(parentNodeRef);
		when(nodeServiceMock.getPrimaryParent(parentNodeRef)).thenReturn(childAssociationRefMock2);
		when(childAssociationRefMock2.getParentRef()).thenReturn(parentParentNodeRef);
		when(nodeServiceMock.hasAspect(parentParentNodeRef, AdactaModel.ASPECT_ROOT_INDEX)).thenReturn(true);
		String context = deleteNodeActionExecuter.getContextPage(nodeRef);
		assertEquals(context,"adacta-index");
	}
	@Test
	public void getContextPageAdactaIndexTest() {
		NodeRef nodeRef = new NodeRef("workSpace://SpacesStore/actionedUponNoderef");
		NodeRef parentNodeRef = new NodeRef("workSpace://SpacesStore/parentNoderef");
		NodeRef parentParentNodeRef = new NodeRef("workSpace://SpacesStore/parentParentNoderef");
		when(nodeServiceMock.getPrimaryParent(nodeRef)).thenReturn(childAssociationRefMock1);
		when(childAssociationRefMock1.getParentRef()).thenReturn(parentNodeRef);
		when(nodeServiceMock.getPrimaryParent(parentNodeRef)).thenReturn(childAssociationRefMock2);
		when(childAssociationRefMock2.getParentRef()).thenReturn(parentParentNodeRef);
		when(nodeServiceMock.hasAspect(parentNodeRef, AdactaModel.ASPECT_ROOT_IMPORT)).thenReturn(true);
		String context = deleteNodeActionExecuter.getContextPage(nodeRef);
		assertEquals(context,"adacta-import");
	}
	@Test
	public void getContextPageAdactaSearchTest() {
		NodeRef nodeRef = new NodeRef("workSpace://SpacesStore/actionedUponNoderef");
		NodeRef parentNodeRef = new NodeRef("workSpace://SpacesStore/parentNoderef");
		NodeRef parentParentNodeRef = new NodeRef("workSpace://SpacesStore/parentParentNoderef");
		when(nodeServiceMock.getPrimaryParent(nodeRef)).thenReturn(childAssociationRefMock1);
		when(childAssociationRefMock1.getParentRef()).thenReturn(parentNodeRef);
		when(nodeServiceMock.getPrimaryParent(parentNodeRef)).thenReturn(childAssociationRefMock2);
		when(childAssociationRefMock2.getParentRef()).thenReturn(parentParentNodeRef);
		when(nodeServiceMock.hasAspect(parentNodeRef, AdactaModel.ASPECT_ROOT_IMPORT)).thenReturn(false);
		when(nodeServiceMock.hasAspect(parentParentNodeRef, AdactaModel.ASPECT_ROOT_INDEX)).thenReturn(false);
		String context = deleteNodeActionExecuter.getContextPage(nodeRef);
		assertEquals(context,"adacta-search");
	}
	/**
	 * UseCase: origin: import page
	 * Delete is effected,
	 * next page:  staying on adacta-import
	 * no report is shown
	 */
	@Test
	public void executeImplImportPageTest() {
		NodeRef nodeRef = new NodeRef("workSpace://SpacesStore/actionedUponNoderef");
		NodeRef parentNodeRef = new NodeRef("workSpace://SpacesStore/parentNoderef");
		NodeRef parentParentNodeRef = new NodeRef("workSpace://SpacesStore/parentParentNoderef");
		when(nodeServiceMock.getPrimaryParent(nodeRef)).thenReturn(childAssociationRefMock1);
		when(childAssociationRefMock1.getParentRef()).thenReturn(parentNodeRef);
		when(nodeServiceMock.getPrimaryParent(parentNodeRef)).thenReturn(childAssociationRefMock2);
		when(childAssociationRefMock2.getParentRef()).thenReturn(parentParentNodeRef);
		when(nodeServiceMock.hasAspect(parentNodeRef, AdactaModel.ASPECT_ROOT_IMPORT)).thenReturn(true);
		when(authenticationServiceMock.getCurrentUserName()).thenReturn("wim");
		when(adactaAuthorityServiceMock.isUserAdactaAdministrator("wim")).thenReturn(true);
		Mockito.doNothing().when(fileFolderServiceMock).delete(nodeRef);
		String parentFolderName = "NLD-wim";
		when(nodeServiceMock.getProperty(parentNodeRef, ContentModel.PROP_NAME)).thenReturn(parentFolderName);
		//batch not empty
		//when(adactaReportServiceMock.batchEmpty(parentFolderName)).thenReturn(false);
		Action action = new ActionImpl(nodeRef, "id","adactaDeleteNode");
		SysAdminParams p = new SysAdminParamsImpl();
		
		when(serviceDescriptorRegistryMock.getSysAdminParams()).thenReturn(p) ;
		deleteNodeActionExecuter.executeImpl(action, nodeRef);
		String result = (String) action.getParameterValue("result");
		
		assertTrue(result.contains("adacta-import"));
	}
	/**
	 * UseCase: origin: index page, deleting last document in batch
	 * Delete is effected, batch is empty, so print report
	 * next page:  show report
	 * 
	 */
	@Test
	public void executeImplIndexPageBatchEmptyTest() {
		NodeRef nodeRef = new NodeRef("workSpace://SpacesStore/actionedUponNoderef");
		NodeRef parentNodeRef = new NodeRef("workSpace://SpacesStore/parentNoderef");
		NodeRef parentParentNodeRef = new NodeRef("workSpace://SpacesStore/parentParentNoderef");
		NodeRef csvNodeRef = new NodeRef("workSpace://SpacesStore/csvNoderef");
		NodeRef htmlNodeRef = new NodeRef("workSpace://SpacesStore/htmlNoderef");
		when(nodeServiceMock.getPrimaryParent(nodeRef)).thenReturn(childAssociationRefMock1);
		when(childAssociationRefMock1.getParentRef()).thenReturn(parentNodeRef);
		when(nodeServiceMock.getPrimaryParent(parentNodeRef)).thenReturn(childAssociationRefMock2);
		when(childAssociationRefMock2.getParentRef()).thenReturn(parentParentNodeRef);
		when(nodeServiceMock.hasAspect(parentNodeRef, AdactaModel.ASPECT_ROOT_IMPORT)).thenReturn(false);
		when(nodeServiceMock.hasAspect(parentParentNodeRef, AdactaModel.ASPECT_ROOT_INDEX)).thenReturn(true);
		when(authenticationServiceMock.getCurrentUserName()).thenReturn("wim");
		when(adactaAuthorityServiceMock.isUserAdactaAdministrator("wim")).thenReturn(true);
		Mockito.doNothing().when(fileFolderServiceMock).delete(nodeRef);
		String parentFolderName = "NLD-wim";
		when(nodeServiceMock.getProperty(parentNodeRef, ContentModel.PROP_NAME)).thenReturn(parentFolderName);
		//batch not empty
		when(adactaReportServiceMock.batchEmpty(parentFolderName)).thenReturn(true);
		Action action = new ActionImpl(nodeRef, "id","adactaDeleteNode");
		SysAdminParams p = new SysAdminParamsImpl();
		when(serviceDescriptorRegistryMock.getSysAdminParams()).thenReturn(p) ;
		Mockito.doNothing().when(adactaReportServiceMock).printHtmlIndexReport(parentFolderName, null, "UUID-1");
		when(searchServiceMock.getScanBatchCSVFileByName(parentFolderName + ".csv")).thenReturn(csvNodeRef);
		Mockito.doNothing().when(adactaReportServiceMock).deleteCsvFile(csvNodeRef);
		when(searchServiceMock.getIndexReport("UUID-1")).thenReturn(htmlNodeRef);
		when(serviceDescriptorRegistryMock.getSysAdminParams()).thenReturn(p) ;
		
		deleteNodeActionExecuter.executeImpl(action, nodeRef);
		String result = (String) action.getParameterValue("result");

		assertTrue(result.contains(htmlNodeRef.getId()));
	}
	/**
	 * UseCase: origin: index page, deleting document in batch
	 * Batch not empty, we still have documents in scanbatch after delete
	 * No report will be printed
	 * There are also more selected documents in the batch
	 * next page:  show detail page next selected document
	 * 
	 */
	@Test
	public void executeImplIndexPageBatchNotEmpty1Test() {
		NodeRef nodeRef = new NodeRef("workSpace://SpacesStore/actionedUponNoderef");
		NodeRef parentNodeRef = new NodeRef("workSpace://SpacesStore/parentNoderef");
		NodeRef parentParentNodeRef = new NodeRef("workSpace://SpacesStore/parentParentNoderef");
		List<NodeRef> selectedItems = new ArrayList<>();
		selectedItems.add(new NodeRef("workSpace://SpacesStore/actionedUponNoderef"));
		selectedItems.add(new NodeRef("workSpace://SpacesStore/selectedNoderef-2"));
		selectedItems.add(new NodeRef("workSpace://SpacesStore/selectedNoderef-3"));
		when(nodeServiceMock.getPrimaryParent(nodeRef)).thenReturn(childAssociationRefMock1);
		when(childAssociationRefMock1.getParentRef()).thenReturn(parentNodeRef);
		when(nodeServiceMock.getPrimaryParent(parentNodeRef)).thenReturn(childAssociationRefMock2);
		when(childAssociationRefMock2.getParentRef()).thenReturn(parentParentNodeRef);
		when(nodeServiceMock.hasAspect(parentNodeRef, AdactaModel.ASPECT_ROOT_IMPORT)).thenReturn(false);
		when(nodeServiceMock.hasAspect(parentParentNodeRef, AdactaModel.ASPECT_ROOT_INDEX)).thenReturn(true);
		when(authenticationServiceMock.getCurrentUserName()).thenReturn("wim");
		when(adactaAuthorityServiceMock.isUserAdactaAdministrator("wim")).thenReturn(true);
		Mockito.doNothing().when(fileFolderServiceMock).delete(nodeRef);
		String parentFolderName = "NLD-wim";
		when(nodeServiceMock.getProperty(parentNodeRef, ContentModel.PROP_NAME)).thenReturn(parentFolderName);
		//batch not empty
		when(adactaReportServiceMock.batchEmpty(parentFolderName)).thenReturn(false);
		Action action = new ActionImpl(nodeRef, "id","adactaDeleteNode");
		when(adactaPreferenceServiceMock.getSelectedItemsPreferences()).thenReturn(selectedItems);
		SysAdminParams p = new SysAdminParamsImpl();
		when(serviceDescriptorRegistryMock.getSysAdminParams()).thenReturn(p) ;
		
		deleteNodeActionExecuter.executeImpl(action, nodeRef);
		String result = (String) action.getParameterValue("result");

		assertTrue(result.contains("selectedNoderef-2"));
	}
	/**
	 * UseCase: origin: index page, deleting document in batch
	 * Batch not empty, we still have documents in scanbatch after delete
	 * No report will be printed
	 * There is at most one selected document in the batch - this is however the actionedUponNodeRef
	 * next page:  show index page
	 * 
	 */
	@Test
	public void executeImplIndexPageBatchNotEmpty2Test() {
		NodeRef nodeRef = new NodeRef("workSpace://SpacesStore/actionedUponNoderef");
		NodeRef parentNodeRef = new NodeRef("workSpace://SpacesStore/parentNoderef");
		NodeRef parentParentNodeRef = new NodeRef("workSpace://SpacesStore/parentParentNoderef");
		List<NodeRef> selectedItems = new ArrayList<>();
		selectedItems.add(new NodeRef("workSpace://SpacesStore/actionedUponNoderef"));
		//selectedItems.add(new NodeRef("workSpace://SpacesStore/selectedNoderef-2"));
		//selectedItems.add(new NodeRef("workSpace://SpacesStore/selectedNoderef-3"));
		when(nodeServiceMock.getPrimaryParent(nodeRef)).thenReturn(childAssociationRefMock1);
		when(childAssociationRefMock1.getParentRef()).thenReturn(parentNodeRef);
		when(nodeServiceMock.getPrimaryParent(parentNodeRef)).thenReturn(childAssociationRefMock2);
		when(childAssociationRefMock2.getParentRef()).thenReturn(parentParentNodeRef);
		when(nodeServiceMock.hasAspect(parentNodeRef, AdactaModel.ASPECT_ROOT_IMPORT)).thenReturn(false);
		when(nodeServiceMock.hasAspect(parentParentNodeRef, AdactaModel.ASPECT_ROOT_INDEX)).thenReturn(true);
		when(authenticationServiceMock.getCurrentUserName()).thenReturn("wim");
		when(adactaAuthorityServiceMock.isUserAdactaAdministrator("wim")).thenReturn(true);
		Mockito.doNothing().when(fileFolderServiceMock).delete(nodeRef);
		String parentFolderName = "NLD-wim";
		when(nodeServiceMock.getProperty(parentNodeRef, ContentModel.PROP_NAME)).thenReturn(parentFolderName);
		//batch not empty
		when(adactaReportServiceMock.batchEmpty(parentFolderName)).thenReturn(false);
		Action action = new ActionImpl(nodeRef, "id","adactaDeleteNode");
		when(adactaPreferenceServiceMock.getSelectedItemsPreferences()).thenReturn(selectedItems);
		SysAdminParams p = new SysAdminParamsImpl();
		when(serviceDescriptorRegistryMock.getSysAdminParams()).thenReturn(p) ;
		
		deleteNodeActionExecuter.executeImpl(action, nodeRef);
		String result = (String) action.getParameterValue("result");

		assertTrue(result.contains("adacta-index"));
	}
	
}
