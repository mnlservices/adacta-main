package nl.defensie.adacta.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.TemplateException;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.CdmRoleSyncBean;
import nl.defensie.adacta.utils.RoleAuthoritiesBean;
import nl.defensie.adacta.utils.SyncLogger;

public class AdactaMailService {

	private static final String PATH_TEMPLATES_ARBEIDSPLAATSEN_VERWIJDEREN = "PATH:\"/app:company_home/app:dictionary/app:email_templates/app:notify_email_templates/cm:arbeidsplaatsen_verwijderen.html.ftl\"";

	private static final String PATH_TEMPLATES_ARBEIDSPLAATSEN_TOEVOEGEN = "PATH:\"/app:company_home/app:dictionary/app:email_templates/app:notify_email_templates/cm:arbeidsplaatsen_toevoegen.html.ftl\"";

	private static final String PATH_TEMPLATES_DOSSIERSYNC = "PATH:\"/app:company_home/app:dictionary/app:email_templates/app:notify_email_templates/cm:samenvatting_dossiersync.html.ftl\"";

	private static final String NOREPLY_MINDEF_NL = "noreply@mindef.nl";

	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "AdactaMailService");

	@Value("${adacta.rolesync.mailto}")
	protected String ROLE_SYNC_MAILTO;
	@Value("${adacta.cdmsync.mailto}")
	protected String CDM_SYNC_MAILTO;
	@Value("${adacta.mail.smtp}")
	protected String MAIL_SMTP;

	@Autowired
	@Qualifier("SearchService")
	protected SearchService searchService;

	/**
	 * Public API access
	 */
	private ServiceRegistry serviceRegistry;

	/**
	 * Send a mail to FB with result of dossier synchronization.
	 * 
	 * @param content
	 */
	public void sendMail(ArrayList<String> content) {
		String onderwerp = "Samenvatting Dossier Synchronizatie " + getFormattedDate(new Date());
		String templatePath = PATH_TEMPLATES_DOSSIERSYNC;
		ServiceRegistry sr = getServiceRegistry();
		ActionService actionService = sr.getActionService();
		Action mailAction = actionService.createAction(MailActionExecuter.NAME);
		mailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT, onderwerp);
		mailAction.setParameterValue(MailActionExecuter.PARAM_FROM, NOREPLY_MINDEF_NL);
		mailAction.setParameterValue(MailActionExecuter.PARAM_TEXT, "notificatie");
		Map<String, Serializable> templateArgs = new HashMap<String, Serializable>();
		templateArgs.put("lines", content);
		Map<String, Serializable> templateModel = new HashMap<String, Serializable>();
		templateModel.put("args", (Serializable) templateArgs);
		mailAction.setParameterValue(MailActionExecuter.PARAM_TEMPLATE_MODEL, (Serializable) templateModel);

		NodeRef template = getTemplate(templatePath);
		if (null == template) {
			return;
		}
		mailAction.setParameterValue(MailActionExecuter.PARAM_TEMPLATE, template);
		mailAction.setParameterValue(MailActionExecuter.PARAM_TO, CDM_SYNC_MAILTO);
		actionService.executeAction(mailAction, null);
	}

	/**
	 * Send emails with result of authorization tool (roles synchronization).
	 * Results are in excel email attachment.
	 * 
	 * @param bean
	 * @param roles
	 * @param add   true = add, false = remove
	 */
	public void sendRolesSyncMailWithAttachment(RoleAuthoritiesBean bean, ArrayList<CdmRoleSyncBean> roles,
			boolean add) {
		SyncLogger.info("sending mail with attachment");
		String templatePath = getTemplatePath(add);

		// get mail body
		NodeRef nodeRef = getTemplate(templatePath);
		SyncLogger.debug("retrieved template " + nodeRef.toString());
		Map<String, Serializable> templateModel = new HashMap<String, Serializable>();

		String body = "";
		try {
			body = serviceRegistry.getTemplateService().processTemplate(nodeRef.toString(), templateModel);
		} catch (TemplateException e1) {
			SyncLogger.debug("error while processing template with noderef " + e1.getMessage());
		}

		// get mail subject
		String subject = getMailSubject(add);

		// create mail excel attachment temp file
		File excelTempFile = null;
		try {
			excelTempFile = createExcelTempFile(bean, roles, add);
		} catch (IOException e) {
			SyncLogger.debug("no attachment, will not send email");
		}

		try {
			// Create mail session
			Properties mailServerProperties = new Properties();
			mailServerProperties = System.getProperties();
			mailServerProperties.put("mail.smtp.host", MAIL_SMTP);
			mailServerProperties.put("mail.smtp.port", "25");
			Session session = Session.getDefaultInstance(mailServerProperties, null);
			session.setDebug(false);

			// Define message
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(NOREPLY_MINDEF_NL));
			message.setSubject(subject);

			// Create the message part with body text
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(body);
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			// Create the Attachment part
			messageBodyPart = new MimeBodyPart();
			String documentName = "";// (String)serviceRegistry.getNodeService().getProperty(nodeRef,
										// ContentModel.PROP_NAME);
			if (add) {
				documentName = "SQ NOT IN Active Directory.xls";
			} else {
				documentName = "SQ NOT IN PeopleSoft.xls";
			}
			byte[] documentData = getDocumentContentBytes(excelTempFile, documentName);
			messageBodyPart.setDataHandler(new DataHandler(
					new ByteArrayDataSource(documentData, new MimetypesFileTypeMap().getContentType(documentName))));
			messageBodyPart.setFileName(documentName);
			multipart.addBodyPart(messageBodyPart);

			// Put parts in message
			message.setContent(multipart);

			String[] mailtos = getMailAdresses(ROLE_SYNC_MAILTO);
			for (String mailto : mailtos) {
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailto));
				Transport.send(message);
			}

			boolean deleted = excelTempFile.delete();
			if (!deleted) {
				SyncLogger.info("tempfile could not be deleted");
			}

		} catch (MessagingException me) {
			SyncLogger.debug("Could not send email: " + me.getMessage());
		}
	}

	private byte[] getDocumentContentBytes(File excelTempFile, String documentName) {
		byte[] fileBytes = null;
		try {
			fileBytes = FileUtils.readFileToByteArray(excelTempFile);
		} catch (IOException e) {
			SyncLogger.error("could not read bytes from excel temp file ", e);
		}
		return fileBytes;
	}

	private File createExcelTempFile(RoleAuthoritiesBean roleAuthoritiesbean, ArrayList<CdmRoleSyncBean> roles,
			boolean add) throws IOException {
		File fileToImport = null;
		if (add) {
			fileToImport = new File("/tmp/SQ NOT IN Active Directory.xls");
		} else {
			fileToImport = new File("/tmp/SQ NOT IN PeopleSoft.xls");
		}
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("FirstSheet");

		HSSFRow rowhead = sheet.createRow((short) 0);
		rowhead.createCell(0).setCellValue("Arbeidsplaats");
		rowhead.createCell(1).setCellValue("Authorisatie");

		short counter = 1;
		try {
			for (CdmRoleSyncBean bean : roles) {
				String role = bean.getRole();
				if (StringUtils.isEmpty(role)) {
					continue;
				}
				String aut = bean.getAuthority();
				if (StringUtils.isEmpty(aut)) {
					continue;
				}
				HSSFRow row = sheet.createRow(counter++);
				row.createCell(0).setCellValue(role);
				row.createCell(1).setCellValue(aut);
			}
		} catch (Exception e) {
			SyncLogger.error("error composing excel workbook ", e);
		}
		FileOutputStream fileOut = new FileOutputStream(fileToImport);
		workbook.write(fileOut);
		fileOut.close();
		workbook.close();
		return fileToImport;
	}

	private String[] getMailAdresses(String mailTo) {
		if (StringUtils.isEmpty(mailTo)) {
			SyncLogger.error("no mail adres found for roles sync", null);
		}
		String[] returnMails = new String[1];
		if (StringUtils.isNotEmpty(mailTo) && mailTo.contains(";")) {
			return mailTo.split(";");
		} else if (StringUtils.isNotEmpty(mailTo) && mailTo.contains(",")) {
			return mailTo.split(",");
		} else if (StringUtils.isNotEmpty(mailTo) && mailTo.contains(" ")) {
			return mailTo.split(" ");
		}
		returnMails[0] = mailTo;
		return returnMails;
	}

	private String getMailSubject(boolean add) {
		String onderwerp = "";
		if (add) {
			onderwerp = "Case " + getFormattedDate(new Date()) + " Arbeidsplaatsen koppelen aan Autorisaties";
		} else {
			onderwerp = "Case " + getFormattedDate(new Date()) + " Arbeidsplaatsen verwijderen van Autorisaties";
		}
		return onderwerp;
	}

	private String getTemplatePath(boolean add) {
		String templatePath = "";
		if (add) {
			templatePath = PATH_TEMPLATES_ARBEIDSPLAATSEN_TOEVOEGEN;
		} else {
			templatePath = PATH_TEMPLATES_ARBEIDSPLAATSEN_VERWIJDEREN;
		}
		return templatePath;
	}

	private Serializable getFormattedDate(Date d) {
		String pattern = "yyyyMMdd";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		return simpleDateFormat.format(d);
	}

	private NodeRef getTemplate(String templatePath) {
		ResultSet resultSet = searchService.query(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"),
				SearchService.LANGUAGE_LUCENE, templatePath);
		if (resultSet.length() == 0) {
			SyncLogger.info("Template beheerdossier email " + templatePath + " not found.");
			return null;
		}
		return resultSet.getNodeRef(0);
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
}