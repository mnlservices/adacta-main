package nl.defensie.adacta.behaviour;

import java.io.Serializable;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.content.ContentServicePolicies.OnContentReadPolicy;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnSetNodeTypePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.AuditLogger;

/**
 * This behaviour is bind to 'ada:document' type. This actually means that only
 * the audit logger is only enabled for Adacta personnel files documents. Not
 * the default cm:content, in which this was initially done in the original
 * code. See {@link AuditLogger} how information is being processed.
 * 
 * @author Rick de Rooij
 *
 */
public class AuditLoggerBehaviour implements NodeServicePolicies.OnCreateNodePolicy,
		ContentServicePolicies.OnContentReadPolicy, NodeServicePolicies.OnUpdatePropertiesPolicy,
		NodeServicePolicies.BeforeDeleteNodePolicy, NodeServicePolicies.OnSetNodeTypePolicy {

	protected Logger LOGGER = Logger.getLogger(this.getClass());

	@Value("${adacta.audit.enabled}")
	protected Boolean auditEnabled;
	@Value("${adacta.audit.location}")
	protected String auditLocation;
	@Value("${adacta.audit.filename}")
	protected String auditFileName;

	@Autowired
	@Qualifier("nodeService")
	protected NodeService nodeService;
	@Autowired
	@Qualifier("authenticationService")
	protected AuthenticationService authenticationService;
	@Autowired
	private PolicyComponent policyComponent;

	@PostConstruct
	public void initialise() {
		if (auditEnabled) {
			bindBehaviour(OnCreateNodePolicy.QNAME, NotificationFrequency.TRANSACTION_COMMIT,
					AdactaModel.TYPE_DOCUMENT);
			bindBehaviour(OnSetNodeTypePolicy.QNAME, NotificationFrequency.TRANSACTION_COMMIT,
					AdactaModel.TYPE_DOCUMENT);
			bindBehaviour(OnContentReadPolicy.QNAME, NotificationFrequency.FIRST_EVENT, AdactaModel.TYPE_DOCUMENT);
			bindBehaviour(OnUpdatePropertiesPolicy.QNAME, NotificationFrequency.TRANSACTION_COMMIT,
					AdactaModel.TYPE_DOCUMENT);
			bindBehaviour(BeforeDeleteNodePolicy.QNAME, NotificationFrequency.FIRST_EVENT, AdactaModel.TYPE_DOCUMENT);
			AuditLogger.auditLocation = getAuditLogLocation(auditLocation);
			AuditLogger.auditFileName = getAuditLogFilename(auditFileName);
		}
	}

	private String getAuditLogLocation(String loc) {
		if (null == auditLocation || auditLocation.isEmpty() || auditLocation.equals("${adacta.audit.location}")) {
			return "";
		}
		if (!loc.endsWith("/")) {
			loc = loc + "/";
		}

		return loc;
	}

	private String getAuditLogFilename(String name) {
		if (null == auditFileName || auditFileName.isEmpty() || auditFileName.equals("${adacta.audit.filename}")) {
			return "audit.log";
		}

		return name;
	}

	/**
	 * Binds a new behaviour to a name to watch for.
	 * 
	 * @param name       the full name of the behaviour to bind.
	 * @param frequency  the trigger for activation.
	 * @param targetType the item type to watch for changes.
	 */
	private void bindBehaviour(QName name, NotificationFrequency frequency, QName targetType) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Binding behaviour '" + name + "' to '" + targetType + "' at frequency '" + frequency + "'.");
		}
		JavaBehaviour behaviour = new JavaBehaviour(this, name.getLocalName(), frequency);
		this.policyComponent.bindClassBehaviour(name, targetType, behaviour);
	}

	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		AuditLogger.info(nodeService, authenticationService, childAssocRef.getChildRef(), "onCreateNode");
	}

	@Override
	public void onContentRead(NodeRef nodeRef) {
		AuditLogger.info(nodeService, authenticationService, nodeRef, "onContentRead");
	}

	@Override
	public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
		AuditLogger.info(nodeService, authenticationService, nodeRef, "onUpdateProperties");
	}

	@Override
	public void beforeDeleteNode(NodeRef nodeRef) {
		AuditLogger.info(nodeService, authenticationService, nodeRef, "onDeleteNode");
	}

	@Override
	public void onSetNodeType(NodeRef nodeRef, QName oldType, QName newType) {
		AuditLogger.info(nodeService, authenticationService, nodeRef,
				"onCreateAdactaType " + oldType.toPrefixString() + " " + newType.toPrefixString());
	}
}