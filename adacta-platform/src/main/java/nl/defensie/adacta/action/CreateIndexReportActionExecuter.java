package nl.defensie.adacta.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaReportService;

/**
 * Create index report of single document or batch of documents. If it is a
 * batch then all documents in folder need to be moved to personnel file.
 * 
 * All Scan documents in a batch must be represented in a single html report file, even if they are individually moved to a personnel file.
 * Noderefs of scanbatch documents are therefore temporarily stored in a csv file. When all documents in a batch are processed, the report 
 * is generated and the csv deleted. 
 * 
 * @authors Rick de Rooij, Wim Schreurs
 *
 */
public class CreateIndexReportActionExecuter extends ActionExecuterAbstractBase {

	private static final String SCANBATCHNAME = "scanbatchname";

	private static final String IMPORTEER = "Importeer";

	protected Log LOGGER = LogFactory.getLog(CreateIndexReportActionExecuter.class);

	public static final String NAME = AdactaModel.PREFIX + "CreateIndexReport";

	public static final String PARAM_IS_BATCH = "is-batch";
	public static final String PARAM_DOCUMENTS = "documents";
	public static final String PARAM_REPORT_UUID = "reportUuid";
	
	@Value("${adacta.report.index}")
	protected String template;

	@Autowired
	protected AdactaReportService adactaReportService;

	@Override
	protected void executeImpl(Action action, final NodeRef actionedUponNodeRef) {

		List<NodeRef> items = new ArrayList<NodeRef>();

		String scanbatchname = (String) action.getParameterValue(SCANBATCHNAME);
		String reportUuid = (String)action.getParameterValue(PARAM_REPORT_UUID);
		Boolean isBatch = (Boolean) action.getParameterValue(PARAM_IS_BATCH);
		if (action.getParameterValue(PARAM_DOCUMENTS) instanceof List) {
			@SuppressWarnings("unchecked")
			List<NodeRef> providedItems = (List<NodeRef>) action.getParameterValue(PARAM_DOCUMENTS);
			items.addAll(providedItems);
		}
		if (scanbatchname.equalsIgnoreCase(IMPORTEER)) {
			adactaReportService.printHtmlImportReport(items, reportUuid);
			return;
		}
		NodeRef csvRef = adactaReportService.findOrCreateReportCsvNode(scanbatchname);

		if (isBatch) {
			printBatch(items, scanbatchname, csvRef, reportUuid);
		} else {
			printSingleNode(actionedUponNodeRef, scanbatchname, csvRef, reportUuid);
		}
	}

	private void printSingleNode(final NodeRef actionedUponNodeRef, String scanbatchname, NodeRef csvRef, String reportUuid) {
		try {
			adactaReportService.appendToCsvFile(actionedUponNodeRef.getId() + ";", csvRef);
			if (adactaReportService.batchEmpty(scanbatchname)) {
				adactaReportService.printHtmlIndexReport(scanbatchname, null, reportUuid);
				adactaReportService.deleteCsvFile(csvRef);
			}
		} catch (Exception e) {
			LOGGER.error("error while writing last scan batch report line " + e.getMessage());
		}
	}


	private void printBatch(List<NodeRef> items, String scanbatchname, NodeRef csvRef, String reportUuid) {
		try {
			if (adactaReportService.batchEmpty(scanbatchname)){
				adactaReportService.appendToCsvFile(items, csvRef);
				adactaReportService.printHtmlIndexReport(scanbatchname, null, reportUuid);
				adactaReportService.deleteCsvFile(csvRef);
			}else{
				try {
					adactaReportService.appendToCsvFile(items, csvRef);
				} catch (IOException e) {
					LOGGER.error("error appending batch to csv file "+e.getMessage());
				}
			}
		} catch (IOException e) {
			LOGGER.error("error while printing batch "+e.getMessage());
		}
	}


	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(new ParameterDefinitionImpl(PARAM_IS_BATCH, DataTypeDefinition.BOOLEAN, false,
				getParamDisplayLabel(PARAM_IS_BATCH)));
		paramList.add(new ParameterDefinitionImpl(PARAM_DOCUMENTS, DataTypeDefinition.ANY, false,
				getParamDisplayLabel(PARAM_DOCUMENTS)));
		paramList.add(new ParameterDefinitionImpl(PARAM_REPORT_UUID, DataTypeDefinition.ANY, false,
				getParamDisplayLabel(PARAM_REPORT_UUID)));		
	}



}