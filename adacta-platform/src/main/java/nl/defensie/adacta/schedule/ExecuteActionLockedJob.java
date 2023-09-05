package nl.defensie.adacta.schedule;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This is copied from the script mechanisms. This job will execute an action configured in the bean definition.
 * 
 * @author Rick de Rooij
 *
 */
public class ExecuteActionLockedJob extends AbstractScheduledLockedJob implements Job {

	private static final String ADACTA_SERVICE_USER = "sa_adacta";
	private static final String PARAM_ACTION_SERVICE = "actionService";
	private static final String PARAM_AUTHENTICATION_COMPONENT = "authenticationComponent";
	private static final String PARAM_DEFAULT = "default";

	private String actionName;
	private String defaultParam;

	@Override
	public void executeJob(JobExecutionContext jobContext) throws JobExecutionException {
		JobDataMap jobData = jobContext.getJobDetail().getJobDataMap();

		// Get the script service from the job map
		Object actionServiceObj = jobData.get(PARAM_ACTION_SERVICE);
		if (actionServiceObj == null || !(actionServiceObj instanceof ActionService)) {
			throw new AlfrescoRuntimeException("ExecuteScriptJob data must contain valid action service");
		}

		// Get the authentication component from the job map
		Object authenticationComponentObj = jobData.get(PARAM_AUTHENTICATION_COMPONENT);
		if (authenticationComponentObj == null || !(authenticationComponentObj instanceof AuthenticationComponent)) {
			throw new AlfrescoRuntimeException("ExecuteScriptJob data must contain valid authentication component");
		}

		// Execute the script as the system user
		//((AuthenticationComponent) authenticationComponentObj).setSystemUserAsCurrentUser();
		((AuthenticationComponent) authenticationComponentObj).setCurrentUser(ADACTA_SERVICE_USER);
		try {
			// Execute the action
			Action action = ((ActionService) actionServiceObj).createAction(actionName);
			action.setParameterValue(PARAM_DEFAULT, defaultParam);
			((ActionService) actionServiceObj).executeAction(action, null);
		} finally {
			((AuthenticationComponent) authenticationComponentObj).clearCurrentSecurityContext();
		}
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public void setDefaultParam(String defaultParam) {
		this.defaultParam = defaultParam;
	}
}
