package nl.defensie.adacta.evaluator;

import org.alfresco.web.evaluator.BaseEvaluator;
import org.json.simple.JSONObject;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.webscripts.ScriptRemote;
import org.springframework.extensions.webscripts.connector.Response;

public class HasNextSelectedItems extends BaseEvaluator {

	// private static Log LOGGER = LogFactory.getLog(HasNextSelectedItems.class);

	private static final String LINK = "/nl/defensie/adacta/preferences/selected-items/next/";
	private static final String TRUE = "true";

	private ScriptRemote scriptRemote;

	public void setScriptRemote(ScriptRemote scriptRemote) {
		this.scriptRemote = scriptRemote;
	}

	@Override
	public boolean evaluate(JSONObject jsonObject) {

		RequestContext context = (RequestContext) ThreadLocalRequestContext.getRequestContext();
		String userName = context.getUserId();

		JSONObject node = (JSONObject) jsonObject.get("node");
		if (node == null) {
			return false;
		}
		String nodeRef = (String) node.get("nodeRef");

		Response response = scriptRemote.call(LINK + userName + "/" + nodeRef.replace(":/", ""));
		String responseText = response.getText();
		return TRUE.equals(responseText);
	}
}
