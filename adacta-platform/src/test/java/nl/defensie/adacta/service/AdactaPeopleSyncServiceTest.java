package nl.defensie.adacta.service;

import org.alfresco.service.cmr.repository.NodeService;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AdactaPeopleSyncServiceTest {

	@Mock
    NodeService nodeServiceMock;
	
	AdactaPeopleSyncService adactaPeopleSyncService;
	
	@Before
	    public void setUp() throws Exception {
	        MockitoAnnotations.initMocks(this);

	        adactaPeopleSyncService = new AdactaPeopleSyncService() {
	        	
	            public NodeService getNodeService() {
	                return nodeServiceMock;
	            }
	        };
	    }
	
	
}
