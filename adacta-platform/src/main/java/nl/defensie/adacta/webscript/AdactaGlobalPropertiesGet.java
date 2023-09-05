package nl.defensie.adacta.webscript;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Simple webscript for passing some global Adacta settings.
 * 
 * @author Rick de Rooij
 *
 */
public class AdactaGlobalPropertiesGet extends AdactaAbstract {

	@Value("${adacta.audit.enabled}")
	private Boolean auditEnabled;
	@Value("${adacta.serverMode}")
	private String serverMode;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("auditEnabled", auditEnabled);
		model.put("serverMode", serverMode);
		return model;
	}
}