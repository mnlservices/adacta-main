package nl.defensie.adacta.service;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.i18n.MessageService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.service.ServiceDescriptorRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.UrlUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.MsgLabels;

/**
 * Prints a html report of all documents in a scanbatch.
 * If a single document or a subset of the documents in a batch is processed, the id of the node is
 * temporary stored in a csv file. The name of the file is the same as the scanbatchfile.
 * Only when all documents in a batch are processed the final html report will be produced.
 * The csv file is then deleted.
 * 
 * @author wim.schreurs
 *
 */
public class AdactaReportService {

	protected Logger LOGGER = Logger.getLogger(this.getClass());
	
	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired 
	protected AdactaSearchService adactaSearchService;
	@Autowired
	@Qualifier("FileFolderService")
	protected FileFolderService fileFolderService;
	@Autowired
	@Qualifier("ContentService")
	protected ContentService contentService;
	@Value("${adacta.report.index}")
	protected String template;
	@Autowired
	@Qualifier("MessageService")
	protected MessageService messageService;
	@Autowired
	@Qualifier("AuthenticationService")
	protected AuthenticationService authenticationService;
	@Autowired
	@Qualifier("ServiceRegistry")
	protected ServiceDescriptorRegistry serviceDescriptorRegistry;
	@Autowired
	@Qualifier("TemplateService")
	protected TemplateService templateService;
	@Autowired
	protected AdactaFileFolderService adactaFileFolderService;
	
	/**
	 * Prints the records of a scanbatch.
	 * @param scanbatchname
	 * @param items
	 */
	public void printHtmlIndexReport(String scanbatchname, List<NodeRef> items, String reportUuid){
		if (null == items){
			NodeRef batch = adactaSearchService.getScanBatchCSVFileByName(scanbatchname + ".csv");
			if (batch == null){
				//csv can be null when all docs in a batch are deleted and none cuppled
				return;
			}
			items = readCsv(batch);
			items = sortScanSeq(items);
		}	
		doPrint(items, scanbatchname, reportUuid);
	}
	
	/**
	 * Print the records of an Import action.
	 * Noderefes of import documents are not temporarily stored in a csv file. 
	 * 
	 * @param items
	 */
	public void printHtmlImportReport( List<NodeRef> items, String reportUuid){
			doPrint(items, null, reportUuid);	
	}
	/**
	 * Transform a list of noderefs to a Html report.
	 * @param items
	 */
		private void doPrint(List<NodeRef> items, String scanbatchname, String reportUuid) {
		String html = transformToHtml(items);
		NodeRef root = getReportRoot();
		// create year-month-day structure in root
		NodeRef target = getOrCreateDateFolder(root, new Date());

		HashMap<QName, Serializable> props = getReportProperties(items, scanbatchname);

		NodeRef htmlRef = adactaFileFolderService.createNode(target, ContentModel.TYPE_CONTENT, props,
				MimetypeMap.MIMETYPE_HTML, reportUuid);
		if (null != htmlRef){
			LOGGER.info("created node with uuid "+htmlRef.getId());
		}
		ContentWriter writer = contentService.getWriter(htmlRef, ContentModel.PROP_CONTENT, true);
		writer.setMimetype(MimetypeMap.MIMETYPE_HTML);
		writer.putContent(html);
	}

	private NodeRef getReportRoot() {
		NodeRef root = adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER,
				AdactaModel.ASPECT_ROOT_REPORT);
		return root;
	}

	private HashMap<QName, Serializable> getReportProperties(List<NodeRef> items, String scanbatchname) {
		HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
		if (items != null){
		props.put(ContentModel.PROP_DESCRIPTION,
				messageService.getMessage("action.createIndexReport.numberdocs", items.size()));
		}
		return props;
	}

	
	public String transformToHtml(List<NodeRef> items) {
		Map<String, Serializable> templateArgs = new HashMap<String, Serializable>();
			templateArgs.put("username", authenticationService.getCurrentUserName());
			templateArgs.put("shareUrl", UrlUtil.getShareUrl(serviceDescriptorRegistry.getSysAdminParams()));
			templateArgs.put("created", new Date());
			templateArgs.put("size", items.size());
			templateArgs.put("items", (Serializable) items);
			templateArgs.put("msg", (Serializable) getCategorySubjectLabels(items));
			return templateService.processTemplate(template, templateArgs);
	}
	/**
	 * Get all messages labels of category and subject codes. These need to be
	 * displayed in the report.
	 * 
	 * @param items
	 * @return List<MsgLabels> list of codes with labels.
	 */
	private List<MsgLabels> getCategorySubjectLabels(List<NodeRef> items) {
		List<MsgLabels> list = new ArrayList<MsgLabels>();
		for (NodeRef nodeRef : items) {
				String codeCat = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_CATEGORY);
				String codeSub = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_SUBJECT);
				MsgLabels ml = new MsgLabels(codeCat, messageService.getMessage(codeCat), codeSub,
						messageService.getMessage(codeSub));
				if (!list.contains(ml)){
				list.add(new MsgLabels(codeCat, messageService.getMessage(codeCat), codeSub,
					messageService.getMessage(codeSub)));
				}
		}
		return list;
	}
	
	public boolean batchEmpty(String scanbatchname) {
		NodeRef batch = adactaSearchService.getScanBatchFolderByName(scanbatchname);
		Set<QName> doctypes = new TreeSet<>();
		doctypes.add(ContentModel.TYPE_CONTENT);
		doctypes.add(AdactaModel.TYPE_DOCUMENT);
		List<ChildAssociationRef> children = nodeService.getChildAssocs(batch, doctypes);
		if (children == null || children.size()==0){
			return true;
		}
		return false;
	}
	
	public void deleteEmptyBatch(String scanbatchname){
		NodeRef batch = adactaSearchService.getScanBatchFolderByName(scanbatchname);
		deleteEmptyBatch(batch);
	}
	public void deleteEmptyBatch(NodeRef scanbatchNodeRef){
		//TODO add aspect sys:temporary
		deleteAsSystem(scanbatchNodeRef);
	}
	/**
	 * Delete node as system.
	 * 
	 * @param nodeRef
	 */
	private void deleteAsSystem(final NodeRef nodeRef) {
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
			public Void doWork() throws Exception {
				fileFolderService.delete(nodeRef);
				return null;
			}
		});
	}
	/**
	 * Reads the csv file that contains id's of all files in the scanbatch.
	 * Returns a list of noderefs.
	 * If an id cannot be transformed to an existing nodeRef, is is ommitted.
	 * 
	 * @param nodeRef - noderef of teh csv file
	 * @return
	 */
	private List<NodeRef> readCsv(NodeRef csvRef){
		List<NodeRef> items = new ArrayList<>();
		ContentReader reader = contentService.getReader(csvRef, ContentModel.PROP_CONTENT);
		String content = reader.getContentString();
		String[] noderefs = content.split(";");
		for (int i=0;i<noderefs.length;i++){
			if (noderefs[i] != null && noderefs[i].length()>0){
				String refid = noderefs[i];
				NodeRef n = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE+"/"+refid);
				if (nodeService.exists(n)){
					items.add(n);
				}else{
					LOGGER.debug("node does not exist! "+StoreRef.STORE_REF_WORKSPACE_SPACESSTORE+"/"+refid);
				}
				
			}
		}
		return items;
	}
	/**
	 * Returns the NodeRef of a sub folder [dest]/yyyy/mm/dd, where dest is the
	 * specified folder. The parts yyyy, mm and dd are the year, month and day
	 * part of the specified date.
	 *
	 * If one or more of the date folders do not exist, they will be created.
	 * 
	 * @param dest
	 * @param date
	 * @return
	 */
	private NodeRef getOrCreateDateFolder(NodeRef dest, Date date) {
		// If there is no destination root folder, then return null
		if (dest == null)
			return null;

		// If no date is specified, use the current system date.
		if (date == null)
			date = new Date();

		// Get the year, month and day part.
		String year = new SimpleDateFormat("yyyy").format(date);
		String month = new SimpleDateFormat("MM").format(date);
		String day = new SimpleDateFormat("dd").format(date);
		return adactaFileFolderService.makeFolders(dest, year + "/" + month + "/" + day, ContentModel.TYPE_FOLDER);
	}

	public NodeRef findOrCreateReportCsvNode(String scanbatchname) {
		HashMap<QName, Serializable> props =new HashMap<>();
		NodeRef batch = adactaSearchService.getScanBatchCSVFileByName(scanbatchname + ".csv");
		if (batch == null) {
			props.put(ContentModel.PROP_NAME, scanbatchname + ".csv");
			NodeRef n = adactaFileFolderService.createNode(getOrCreateDateFolder(getReportRoot(), new Date()), ContentModel.TYPE_CONTENT, props,
					MimetypeMap.MIMETYPE_TEXT_CSV, false);
			nodeService.addAspect(n, AdactaModel.ASPECT_SCAN, new HashMap<QName, Serializable>());
			return n;
		} else {
			return batch;
		}
	}
	/**
	 * Append a batch of document noderefs to the csv file
	 * @param items
	 * @param csvref
	 * @throws IOException
	 */
	public void appendToCsvFile(List<NodeRef> items, NodeRef csvref) throws IOException{
		for (NodeRef item:items){
			appendToCsvFile(item.getId()+";", csvref);
		}
	}
	
	/**
	 * Appends an id + semicolon to a csv file.
	 * @param logLine - the id + semicolon
	 * @param noderef - the noderef of the csv file
	 * @throws IOException
	 */
	public void appendToCsvFile(String logLine, NodeRef csvref) throws IOException {
		ContentWriter contentWriter = contentService.getWriter(csvref, ContentModel.PROP_CONTENT, true);
		contentWriter.setMimetype("text/csv");
		FileChannel fileChannel = contentWriter.getFileChannel(false);
		ByteBuffer bf = ByteBuffer.wrap(logLine.getBytes());
		fileChannel.position(contentWriter.getSize());
		fileChannel.write(bf);
		fileChannel.force(false);
		fileChannel.close();
	}

	public void deleteCsvFile(NodeRef csvRef) {
		deleteAsSystem(csvRef);
	}
	
	/**
	 * Sorts the nodes on scan sequence number alphabetically.
	 * @param results
	 * @return
	 */
	public List<NodeRef> sortScanSeq(List<NodeRef> results) {
	       Collections.sort(results, new Comparator<NodeRef>()
	        {
	            public int compare(NodeRef n1, NodeRef n2)
	            {
	            	String seq1 = (String) nodeService.getProperty(n1, AdactaModel.PROP_SCAN_SEQ_NR);
	            	String seq2 = (String) nodeService.getProperty(n2, AdactaModel.PROP_SCAN_SEQ_NR);
	                return seq1.compareTo(seq2);
	            }
	        });
		return results;
	}
}
