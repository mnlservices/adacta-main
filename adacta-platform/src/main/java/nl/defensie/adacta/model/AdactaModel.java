package nl.defensie.adacta.model;

import org.alfresco.service.namespace.QName;

/**
 * The mapping of the main Adacta model.
 * 
 * @author Rick de Rooij
 */
public interface AdactaModel {

	// Is used for identifying all services and actions in order to not conflict with other id's. This name is also the site shortname where all files and folders are located.
	String PREFIX = "adacta";

	// The model URI
	String URI = "http://www.defensie.nl/adacta/model/1.0";

	// Types
	QName TYPE_DOSSIER = QName.createQName(URI, "dossier");
	QName TYPE_DOCUMENT = QName.createQName(URI, "document");

	// Aspects
	QName ASPECT_EMPLOYEE = QName.createQName(URI, "employeeAspect");
	QName ASPECT_DOCUMENT = QName.createQName(URI, "documentAspect");
	QName ASPECT_SYNCHRONIZATION = QName.createQName(URI, "synchronizationAspect");
	QName ASPECT_SCAN = QName.createQName(URI, "scanAspect");
	QName ASPECT_ROW_SEC_CLASS = QName.createQName(URI, "rowSecClassAspect");	
	QName ASPECT_ROOT_INDEX = QName.createQName(URI, "rootIndexAspect");
	QName ASPECT_ROOT_IMPORT = QName.createQName(URI, "rootImportAspect");
	QName ASPECT_ROOT_REPORT = QName.createQName(URI, "rootReportAspect");
	QName ASPECT_ROOT_DOSSIERS = QName.createQName(URI, "rootDossiersAspect");

	// Properties
	QName PROP_EMPLOYEE_NUMBER = QName.createQName(URI, "employeeNumber");
	QName PROP_EMPLOYEE_NAME = QName.createQName(URI, "employeeName");
	QName PROP_EMPLOYEE_BSN = QName.createQName(URI, "employeeBsn");
	QName PROP_EMPLOYEE_MRN = QName.createQName(URI, "employeeMrn");
	QName PROP_EMPLOYEE_DEPARTMENT = QName.createQName(URI, "employeeDepartment");
	QName PROP_EMPLOYEE_DP_CODES = QName.createQName(URI, "employeeDpCodes");
	QName PROP_DOC_CATEGORY = QName.createQName(URI, "docCategory");
	QName PROP_DOC_SUBJECT = QName.createQName(URI, "docSubject");
	QName PROP_DOC_DATE = QName.createQName(URI, "docDate");
	QName PROP_DOC_REFERENCE = QName.createQName(URI, "docReference");
	QName PROP_DOC_WORK_DOSSIER = QName.createQName(URI, "docWorkDossier");
	QName PROP_DOC_CASE_NUMBER = QName.createQName(URI, "docCaseNumber");
	QName PROP_DOC_DATE_CREATED = QName.createQName(URI, "docDateCreated");
	QName PROP_DOC_MIG_ID = QName.createQName(URI, "docMigId");
	QName PROP_DOC_MIG_DATE = QName.createQName(URI, "docMigDate");
	QName PROP_DOC_STATUS = QName.createQName(URI, "docStatus");
	QName PROP_SCAN_EMPLOYEE = QName.createQName(URI, "scanEmployee");
	QName PROP_SCAN_SEQ_NR = QName.createQName(URI, "scanSeqNr");
	QName PROP_SCAN_WA_NR = QName.createQName(URI, "scanWaNr");
	QName PROP_SCAN_BATCH_SIZE = QName.createQName(URI, "scanBatchSize");
	QName PROP_SCAN_BATCH_NAME = QName.createQName(URI, "scanBatchName");
	QName PROP_EMPLOYEE_ID = QName.createQName(URI, "employeeID");
	QName PROP_WHEN_CHANGED = QName.createQName(URI, "whenChanged");
	QName PROP_DP_CODE = QName.createQName(URI, "dpCode");
	QName PROP_DOSSIER_REF = QName.createQName(URI, "dossierRef");
	
	String LIST_STATUS_ACTIEF = "Actief";
	String LIST_STATUS_GESLOTEN = "Gesloten";
}