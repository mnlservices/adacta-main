package nl.defensie.adacta.webscript;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Executes scripts on classpath which can be executed at the admin console Adacta configuration page.
 * 
 * @author Rick de Rooij
 *
 */
public class SetupGet extends DeclarativeWebScript {

	private static final Log LOGGER = LogFactory.getLog(SetupGet.class);

	@Value("${spaces.store}")
	private StoreRef storeRef;
	@Value("${spaces.company_home.childname}")
	private String companyHomePath;
	@Value("${adacta.module}")
	private String module;

	private static final String PARAM_MODE = "mode";

	private static final String PARAM_MODE_INITIAL_SETUP = "initialsetup";
	private static final String PARAM_MODE_MIGRATION = "migration";
	private static final String PARAM_MODE_AUTHORISATION = "authorisation";
	private static final String PARAM_MODE_LOADTEST = "loadtest";
	private static final String PARAM_MODE_CONFIGURATION = "configuration";
	private static final String PARAM_MODE_IMPORT = "import";
	private static final String PARAM_MODE_SCANBATCH = "scanbatch";
	private static final String PARAM_MODE_DELETE = "delete";

	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	@Qualifier("SearchService")
	protected SearchService searchService;
	@Autowired
	@Qualifier("PersonService")
	protected PersonService personService;
	@Autowired
	@Qualifier("ScriptService")
	protected ScriptService scriptService;
	@Autowired
	@Qualifier("NamespaceService")
	protected NamespaceService namespaceService;
	@Autowired
	@Qualifier("AuthenticationService")
	protected AuthenticationService authenticationService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

		// Get the mode
		Map<String, String> urlVars = req.getServiceMatch().getTemplateVars();
		String mode = urlVars.get(PARAM_MODE);

		if (PARAM_MODE_INITIAL_SETUP.equals(mode)) {
			executeScript(String.format("alfresco/module/%s/script/initial-setup.js", module));
		} else if (PARAM_MODE_MIGRATION.equals(mode)) {
			executeScript(String.format("alfresco/module/%s/script/migration-folder.js", module));
		} else if (PARAM_MODE_AUTHORISATION.equals(mode)) {
			executeScript(String.format("alfresco/module/%s/script/migration-auth.js", module));
		}else if (PARAM_MODE_CONFIGURATION.equals(mode)) {
			executeScript(String.format("alfresco/module/%s/script/config.js", module));
		} else if (PARAM_MODE_IMPORT.equals(mode)) {
			executeScript(String.format("alfresco/module/%s/script/import.js", module));
		} else if (PARAM_MODE_SCANBATCH.equals(mode)) {
			executeScript(String.format("alfresco/module/%s/script/scanbatch.js", module));
		} else if (PARAM_MODE_LOADTEST.equals(mode)) {
			executeScript(String.format("alfresco/module/%s/script/migration-loadtest.js", module));
		} else if (PARAM_MODE_DELETE.equals(mode)) {
			executeScript(String.format("alfresco/module/%s/script/delete.js", module));
		} else {
			LOGGER.warn(String.format("No script found matching %s.", mode));
		}

		// Build response model
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("success", true);

		return model;
	}

	@Override
	protected void executeFinallyImpl(WebScriptRequest req, Status status, Cache cache, Map<String, Object> model) {
	}

	/**
	 * Execute script from classpath.
	 * 
	 * @param scriptClasspath
	 */
	private void executeScript(String scriptClasspath) {
		scriptService.executeScript(scriptClasspath, buildDefaultModel());
	}

	/**
	 * Build default script model.
	 * 
	 * @return Map<String, Object> default model
	 */
	private Map<String, Object> buildDefaultModel() {
		String userName = authenticationService.getCurrentUserName();
		NodeRef personRef = personService.getPerson(userName);
		NodeRef homeSpaceRef = (NodeRef) nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);

		// All node references point to company home
		NodeRef companyHomeRef = getCompanyHome();
		NodeRef spaceRef = companyHomeRef;
		NodeRef documentRef = companyHomeRef;
		NodeRef scriptRef = null;

		return scriptService.buildDefaultModel(personRef, getCompanyHome(), homeSpaceRef, scriptRef, documentRef, spaceRef);
	}

	/**
	 * Gets the company home node.
	 * 
	 * @return the company home node reference
	 */
	private NodeRef getCompanyHome() {
		NodeRef companyHomeRef;

		List<NodeRef> refs = searchService.selectNodes(nodeService.getRootNode(storeRef), companyHomePath, null, namespaceService, false);
		if (refs.size() != 1) {
			throw new IllegalStateException("Invalid company home path: " + companyHomePath + " - found: " + refs.size());
		}
		companyHomeRef = refs.get(0);
		return companyHomeRef;
	}
}