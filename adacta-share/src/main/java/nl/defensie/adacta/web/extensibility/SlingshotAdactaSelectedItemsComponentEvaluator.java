package nl.defensie.adacta.web.extensibility;

import java.util.Map;

import org.alfresco.web.extensibility.SlingshotEvaluatorUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.config.ConfigService;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.extensibility.impl.DefaultSubComponentEvaluator;

public class SlingshotAdactaSelectedItemsComponentEvaluator extends DefaultSubComponentEvaluator {
	private static Log logger = LogFactory.getLog(SlingshotAdactaSelectedItemsComponentEvaluator.class);

	// Evaluator parameters

	protected SlingshotEvaluatorUtil util = null;
	protected ConfigService configService = null;

	/**
	 * Decides if the node type matchest.
	 * 
	 * @param context
	 * @param params
	 * @return true if we type matches
	 */
	@Override
	public boolean evaluate(RequestContext context, Map<String, String> params) {
		String userName = context.getUserId();

		try {
			JSONObject json = util.jsonGet("/nl/defensie/adacta/preferences/selected-items/" + userName);

			if (json == null) {
				return false;
			}

			JSONObject paging = (JSONObject) json.get("paging");
			Integer totalItems = paging.getInt("totalItems");

			if (totalItems != 0) {
				return true;
			}
		} catch (JSONException e) {
			if (logger.isErrorEnabled()) {
				logger.error("Could not get a the selected items.");
			}
		}
		return false;
	}

	/**
	 * Sets the evaluator util.
	 * 
	 * @param slingshotExtensibilityUtil
	 *            the evaluator util
	 */
	public void setSlingshotEvaluatorUtil(SlingshotEvaluatorUtil slingshotExtensibilityUtil) {
		this.util = slingshotExtensibilityUtil;
	}

	/**
	 * Sets the config service.
	 * 
	 * @param configService
	 *            the new config service
	 */
	public void setConfigService(ConfigService configService) {
		this.configService = configService;
	}
}