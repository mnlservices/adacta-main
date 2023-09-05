package nl.defensie.adacta.utils;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import nl.defensie.adacta.model.AdactaModel;

/**
 * This code is similar to the code that was developed by OPENSatisfaction in which they provided an alternative for the standard Alfresco audit services.
 * 
 * @author Rick de Rooij
 *
 */
public class AuditLogger {

	public static String auditLocation = null;
	public static String auditFileName = "audit.log";
	
	private static Logger auditlogger = null;

	public static void info(NodeService nodeService, AuthenticationService authenticationService, NodeRef nodeRef, String action) {
		logger();

		if (nodeService != null && nodeService.exists(nodeRef)) {

			String authenticatedUser = authenticationService.getCurrentUserName();
			if (authenticatedUser == null) {
				authenticatedUser = "System";
			}

			String documentName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
			String employeeBsn = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_BSN);
			if (employeeBsn == null) {
				employeeBsn = "(empty)";
			}

			auditlogger.info(String.format("%s %s %s %s %s", authenticatedUser, nodeRef, action, employeeBsn, documentName));
		}
	}

	/**
	 * Create the custom audit logger.
	 * 
	 * @return Logger the logger
	 */
	private static Logger logger() {
		if (auditlogger == null) {
			auditlogger = Logger.getLogger(auditLocation+auditFileName);
			auditlogger.setLevel(Level.INFO);
			auditlogger.setAdditivity(false);
			try {
				PatternLayout layout = new PatternLayout();
				String conversionPattern = "[%p] %d %c %M - %m%n";
				layout.setConversionPattern(conversionPattern);
				DailyRollingFileAppender appender = new DailyRollingFileAppender(layout, auditLocation+auditFileName, "'.'yyyy-MM-dd");
				auditlogger.removeAllAppenders();
				auditlogger.addAppender(appender);
			} catch (Exception e) {
				auditlogger.error(e);
			}
		}
		return auditlogger;
	}
}