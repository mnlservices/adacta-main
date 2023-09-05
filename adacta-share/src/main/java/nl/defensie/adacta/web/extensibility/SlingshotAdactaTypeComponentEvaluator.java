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
import org.springframework.extensions.surf.util.URLEncoder;

public class SlingshotAdactaTypeComponentEvaluator extends DefaultSubComponentEvaluator {
    private static Log logger = LogFactory.getLog(SlingshotAdactaTypeComponentEvaluator.class);

    // Evaluator parameters
    public static final String PARAMS_TYPE = "type";
    public static final String NODEREF = "nodeRef";

    public static final String FOLDER_TYPE = "ada:dossier";
    public static final String DOCUMENT_TYPE = "ada:document";

    private Boolean evaluateOnlyFolder = false;

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
        String nodeRef = (String) context.getParameters().get(NODEREF);

        if (nodeRef == null) {
            return false;
        }

        try {
            JSONObject node = util.jsonGet("/api/metadata?shortQNames=true&nodeRef=" + URLEncoder.encode(nodeRef));
            if (node != null) {
                String type = node.getString(PARAMS_TYPE);
                if (type != null && type.equalsIgnoreCase(FOLDER_TYPE)) {
                    return true;
                } else if (type != null && type.equalsIgnoreCase(DOCUMENT_TYPE) && !evaluateOnlyFolder) {
                    return true;
                }
            }
        } catch (final JSONException e) {
            logger.error("Failed to retrieve node type of " + nodeRef, e);
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

    public void setEvaluateOnlyFolder(Boolean evaluateOnlyFolder) {
        this.evaluateOnlyFolder = evaluateOnlyFolder;
    }
}