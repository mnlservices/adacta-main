package nl.defensie.adacta.evaluator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.web.evaluator.BaseEvaluator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

public class HasDestroyCode extends BaseEvaluator {
	
	private static Log LOGGER = LogFactory.getLog(HasDestroyCode.class);

	private Set<String> codes = new HashSet<String>();

	public void setCodes(List<String> codes) {
		this.codes.addAll(codes);
	}

	@Override
	public boolean evaluate(JSONObject jsonObject) {
		JSONObject node = (JSONObject) jsonObject.get("node");
		if (node == null)
			return false;
		JSONObject properties = (JSONObject) node.get("properties");
		if (properties == null)
			return false;
		String code = (String) properties.get("ada:docWorkDossier");
		LOGGER.debug("Code " + code);
		return codes.contains(code);
	}
}