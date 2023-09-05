package nl.defensie.adacta.service;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileFolderUtil;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaModel;

/**
 * Service for managing folders and files.
 * 
 * @author Rick de Rooij
 *
 */
public class AdactaFileFolderService {

	private static final String ADACTA_SERVICE_USER = "sa_adacta";

	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "FileFolderService");

	protected Logger LOGGER = Logger.getLogger(this.getClass());

	private static final String HEXADECIMAL_NUMBERS = "0123456789ABCDEF";
	private static Random random = new Random();

	/** A map of supported mimetypes and corresponding extensions. */
	private static final Map<String, String> EXTENSIONS;

	static {
		// Creating the map of mimetypes <-> extensions
		Map<String, String> map = new HashMap<String, String>();
		map.put(MimetypeMap.MIMETYPE_PDF, "pdf");
		map.put(MimetypeMap.MIMETYPE_IMAGE_GIF, "gif");
		map.put(MimetypeMap.MIMETYPE_IMAGE_JPEG, "jpeg");
		map.put(MimetypeMap.MIMETYPE_IMAGE_PNG, "png");
		map.put(MimetypeMap.MIMETYPE_BINARY, "bin");
		map.put(MimetypeMap.MIMETYPE_TEXT_PLAIN, "txt");
		map.put(MimetypeMap.MIMETYPE_XML, "xml");
		map.put(MimetypeMap.MIMETYPE_TEXT_CSV, "csv");
		map.put(MimetypeMap.MIMETYPE_JSON, "json");
		map.put(MimetypeMap.MIMETYPE_HTML, "html");
		EXTENSIONS = Collections.unmodifiableMap(map);
	}

	/** List of destroy codes. */
	private static final List<String> DESTROY_CODES = new ArrayList<String>();

	{
		{
			DESTROY_CODES.add("V1");
			DESTROY_CODES.add("V2");
			DESTROY_CODES.add("V3");
			DESTROY_CODES.add("V4");
			DESTROY_CODES.add("V5");
			DESTROY_CODES.add("V6");
			DESTROY_CODES.add("V7");
			DESTROY_CODES.add("V8");
		}
	}

	private static final List<DateFormat> DATE_FORMATS;

	static {
		DATE_FORMATS = new ArrayList<DateFormat>();
		DATE_FORMATS.add(new SimpleDateFormat("yyyy-MM-dd"));
		DATE_FORMATS.add(new SimpleDateFormat("yyyyMMdd"));
		DATE_FORMATS.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
		DATE_FORMATS.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));
		DATE_FORMATS.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
	}

	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	@Qualifier("FileFolderService")
	protected FileFolderService fileFolderService;
	@Autowired
	@Qualifier("TransactionService")
	protected TransactionService transactionService;
	@Autowired
	protected AdactaSearchService adactaSearchService;

	/**
	 * Validate if node has a known destroy code.
	 * 
	 * @param nodeRef
	 *            NodeRef the node reference
	 * @return Boolean true if code is V1,V2..V8.
	 */
	public Boolean hasDestroyCode(NodeRef nodeRef) {
		String destoyCode = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_WORK_DOSSIER);
		for (String s : DESTROY_CODES) {
			if (s.equalsIgnoreCase(destoyCode)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Create personnel file folder based on three level structure
	 * 
	 * @param rootFolder
	 *            NodeRef the root folder to start from
	 * @param folderName
	 *            String the personnel file name
	 * @param folderType
	 *            QName the folder type
	 * @param employeeName
	 *            String name
	 * @param employeeBsn
	 *            String service number
	 * @param employeeMrn
	 *            String registration number
	 * @param employeeDepartment
	 *            String department
	 * @return FileInfo of created personnel file
	 */
	public FileInfo creatPersonnelFile(NodeRef rootFolder, String folderName, QName folderType, String employeeName, String employeeBsn, String employeeMrn,
			String employeeDepartment) {

		// Make three level path structure
		String folderPath = selectRandomHexNumber() + "/" + selectRandomHexNumber() + "/" + selectRandomHexNumber();

		NodeRef target = makeFolders(rootFolder, folderPath, ContentModel.TYPE_FOLDER);
		NodeRef folder = fileFolderService.create(target, folderName, folderType).getNodeRef();

		FileInfo fileInfo = fileFolderService.getFileInfo(folder);

		// Apply all props
		Map<QName, Serializable> employeeProps = new HashMap<QName, Serializable>(4);
		employeeProps.put(AdactaModel.PROP_EMPLOYEE_NAME, employeeName);
		employeeProps.put(AdactaModel.PROP_EMPLOYEE_BSN, employeeBsn);
		employeeProps.put(AdactaModel.PROP_EMPLOYEE_MRN, employeeMrn);
		employeeProps.put(AdactaModel.PROP_EMPLOYEE_DEPARTMENT, employeeDepartment);
		nodeService.addAspect(fileInfo.getNodeRef(), AdactaModel.ASPECT_EMPLOYEE, employeeProps);

		return fileInfo;
	}

	/**
	 * Create personnel file (ada:dossier) in three level path structure.
	 * 
	 * @param rootFolder
	 *            NodeRef the root node to start creating the personnel file
	 * @param properties
	 *            Map<QName, Serializable> the employee properties including DP codes
	 * @return FileInfo the created personnel file
	 */
	public FileInfo creatPersonnelFile(NodeRef rootFolder, Map<QName, Serializable> properties) {
		// Make random three level path structure
		String folderPath = selectRandomHexNumber() + "/" + selectRandomHexNumber() + "/" + selectRandomHexNumber();

		String folderName = (String) properties.get(AdactaModel.PROP_EMPLOYEE_BSN);
		if (folderName == null) {
			throw new AlfrescoRuntimeException("Cannot create folder. No employee BSN (NLD-XXX) value available.");
		}

		NodeRef target = makeFolders(rootFolder, folderPath, ContentModel.TYPE_FOLDER);
		NodeRef folder = fileFolderService.create(target, folderName.trim(), AdactaModel.TYPE_DOSSIER).getNodeRef();

		FileInfo fileInfo = fileFolderService.getFileInfo(folder);

		// Apply all props
		Map<QName, Serializable> employeeProps = new HashMap<QName, Serializable>(6);
		employeeProps.put(AdactaModel.PROP_EMPLOYEE_NAME, properties.get(AdactaModel.PROP_EMPLOYEE_NAME));
		employeeProps.put(AdactaModel.PROP_EMPLOYEE_BSN, properties.get(AdactaModel.PROP_EMPLOYEE_BSN));
		employeeProps.put(AdactaModel.PROP_EMPLOYEE_MRN, properties.get(AdactaModel.PROP_EMPLOYEE_MRN));
		employeeProps.put(AdactaModel.PROP_EMPLOYEE_NUMBER, properties.get(AdactaModel.PROP_EMPLOYEE_NUMBER));
		employeeProps.put(AdactaModel.PROP_EMPLOYEE_DEPARTMENT, properties.get(AdactaModel.PROP_EMPLOYEE_DEPARTMENT));
		employeeProps.put(AdactaModel.PROP_EMPLOYEE_DP_CODES, properties.get(AdactaModel.PROP_EMPLOYEE_DP_CODES));
		nodeService.addAspect(fileInfo.getNodeRef(), AdactaModel.ASPECT_EMPLOYEE, employeeProps);

		return fileInfo;
	}

	/**
	 * Create personnel file (ada:dossier) in folder where dossier root aspect is set.
	 * 
	 * @param properties
	 *            Map<QName, Serializable> list of employee props.
	 * @return FileInfo the created personnel file
	 */
	public FileInfo creatPersonnelFile(Map<QName, Serializable> properties) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Create folder '%s' for '%s' with ID '%s'.", (String) properties.get(AdactaModel.PROP_EMPLOYEE_BSN),
					(String) properties.get(AdactaModel.PROP_EMPLOYEE_NAME), (String) properties.get(AdactaModel.PROP_EMPLOYEE_NUMBER)));
		}
		return creatPersonnelFile(adactaSearchService.getDossiersRoot(), properties);
	}

	/**
	 * Get personnel file folder.
	 * 
	 * @param folder
	 *            NodeRef node reference of the existing personnel file
	 * @return FileInfo of existing personnel file
	 */
	public FileInfo getPersonnelFile(NodeRef folder) {

		return fileFolderService.getFileInfo(folder);
	}

	/**
	 * Change type from cm:folder to ada:dossier and set employee metadata.
	 * 
	 * @param folderRef
	 *            the node to be updated
	 * @param type
	 *            QName the type to be set on the node
	 */
	public void createDossierFromFolder(NodeRef folderRef, QName type) {
		nodeService.setType(folderRef, type);

		Map<QName, Serializable> fileProperties = null;
		Map<QName, Serializable> folderProperties = nodeService.getProperties(folderRef);

		List<FileInfo> childrenFileInfoList = fileFolderService.listFiles(folderRef);

		if (childrenFileInfoList.size() > 0) {
			fileProperties = childrenFileInfoList.get(0).getProperties();

			folderProperties.put(AdactaModel.PROP_EMPLOYEE_NUMBER, fileProperties.get(AdactaModel.PROP_EMPLOYEE_NUMBER));
			folderProperties.put(AdactaModel.PROP_EMPLOYEE_NAME, fileProperties.get(AdactaModel.PROP_EMPLOYEE_NAME));
			folderProperties.put(AdactaModel.PROP_EMPLOYEE_BSN, fileProperties.get(AdactaModel.PROP_EMPLOYEE_BSN));
			folderProperties.put(AdactaModel.PROP_EMPLOYEE_MRN, fileProperties.get(AdactaModel.PROP_EMPLOYEE_MRN));
			folderProperties.put(AdactaModel.PROP_EMPLOYEE_DEPARTMENT, fileProperties.get(AdactaModel.PROP_EMPLOYEE_DEPARTMENT));
			nodeService.addAspect(folderRef, AdactaModel.ASPECT_EMPLOYEE, folderProperties);
		}
	}

	/**
	 * Get all employee props of personnel file (dossier).
	 * 
	 * @param personnelFile
	 *            NodeRef the personnel file
	 * @return
	 */
	public Map<QName, Serializable> getEmployeeProps(NodeRef personnelFile) {
		Map<QName, Serializable> props = new HashMap<QName, Serializable>();

		if (nodeService.getType(personnelFile).equals(AdactaModel.TYPE_DOSSIER)) {
			String employeeNumber = (String) nodeService.getProperty(personnelFile, AdactaModel.PROP_EMPLOYEE_NUMBER);
			String employeeName = (String) nodeService.getProperty(personnelFile, AdactaModel.PROP_EMPLOYEE_NAME);
			String employeeBsn = (String) nodeService.getProperty(personnelFile, AdactaModel.PROP_EMPLOYEE_BSN);
			String employeeMrn = (String) nodeService.getProperty(personnelFile, AdactaModel.PROP_EMPLOYEE_MRN);
			String employeeDepartment = (String) nodeService.getProperty(personnelFile, AdactaModel.PROP_EMPLOYEE_DEPARTMENT);

			props.put(AdactaModel.PROP_EMPLOYEE_NUMBER, employeeNumber);
			props.put(AdactaModel.PROP_EMPLOYEE_NAME, employeeName);
			props.put(AdactaModel.PROP_EMPLOYEE_BSN, employeeBsn);
			props.put(AdactaModel.PROP_EMPLOYEE_MRN, employeeMrn);
			props.put(AdactaModel.PROP_EMPLOYEE_DEPARTMENT, employeeDepartment);
			return props;
		} else {
			LOGGER.warn(String.format("%s is not a personnel file. ", personnelFile));
			return null;
		}
	}

	/**
     * Return the distinct categoryCodes for all existing documents in a personnel file folder. A dossier is expected 
     * to exist for the given empId and the empId should be provided.
	 * @param selfservice 
     * @param empId
     * @return
     */
    public LinkedList<String> getDossierCategoryList(final NodeRef dossier, boolean selfservice) {
        final LinkedList<String> categoryCodes = new LinkedList<String>();
        for (final FileInfo doc : fileFolderService.listFiles(dossier)) {
            final String code = (String) doc.getProperties().get(AdactaModel.PROP_DOC_CATEGORY);
            String docStatus = (String) doc.getProperties().get(AdactaModel.PROP_DOC_STATUS);
            boolean docVerwijderd = !StringUtils.isEmpty(docStatus) && docStatus.equalsIgnoreCase(AdactaModel.LIST_STATUS_GESLOTEN);
            if (StringUtils.isNotBlank(code) && !categoryCodes.contains(code) && !docVerwijderd && !codeExcluded(code, selfservice)) {
                categoryCodes.add(code);
            }
        }

        return categoryCodes;
    }

    /**
     * If coming from the selfservice page, some categoryCodes are excluded from the categoryFilter:
     * code 06
     * codes > 19
     * 
     * @param code
     * @return
     */
	private boolean codeExcluded(String code, boolean selfservice) {
		if (!selfservice){
			return false;
		}
		if ((code.startsWith("0") || code.startsWith("1")) && !code.equals("06")){
			return false;
		}
		return true;
	}

	/**
	 * Get all documents of batch.
	 * 
	 * @param container
	 *            NodeRef the (scan) folder
	 * @return List<NodeRef> list of nodes
	 */
	public List<NodeRef> getDocumentsOfBatch(NodeRef container) {
		List<NodeRef> items = new ArrayList<NodeRef>();
		List<ChildAssociationRef> children = nodeService.getChildAssocs(container);
		Iterator<ChildAssociationRef> i = children.iterator();
		while (i.hasNext()) {
			ChildAssociationRef ref = i.next();
			NodeRef child = ref.getChildRef();

			if (nodeService.getType(child).equals(AdactaModel.TYPE_DOCUMENT) || nodeService.getType(child).equals(ContentModel.TYPE_CONTENT)) {
				items.add(child);
			}
		}
		return items;
	}

	/**
	 * Create node with the possibility to set the name as uuid and if provided the mimetype extension.
	 * 
	 * @param parentRef
	 *            the parent node, e.g. folder
	 * @param type
	 *            QName the type of the node
	 * @param properties
	 *            Map<QName, Serializable> list of properties
	 * @param mimetype
	 *            String mimetype
	 * @param nameIsUuid
	 *            if true the name will be the uuid
	 * @return NodeRef the new created node reference.
	 */
	public NodeRef createNode(NodeRef parentRef, QName type, Map<QName, Serializable> properties, String mimetype, Boolean nameIsUuid) {
		String uuid = UUID.randomUUID().toString();
		String nodeName = uuid;

		// Get extension if provided
		String extension = null;
		if (mimetype != null) {
			extension = EXTENSIONS.get(mimetype);
		}

		if (extension != null) {
			nodeName = uuid + "." + extension;
		}

		properties.put(ContentModel.PROP_NODE_UUID, uuid);
		if (nameIsUuid) {
			properties.put(ContentModel.PROP_NAME, nodeName);
		}
		return nodeService.createNode(parentRef, ContentModel.ASSOC_CONTAINS, QName.createQName(nodeName), type, properties).getChildRef();
	}
	
		/**
		 * Creates a node with the given uuid, and with nodeName as uuid.
		 * @param parentRef
		 * @param type
		 * @param properties
		 * @param mimetype
		 * @param uuid
		 * @return
		 */
		
		public NodeRef createNode(NodeRef parentRef, QName type, Map<QName, Serializable> properties, String mimetype, String uuid) {
			// Get extension if provided
			String extension = null;
			if (mimetype != null) {
				extension = EXTENSIONS.get(mimetype);
			}
			String nodeName= "";		
			if (extension != null) {
				nodeName = uuid + "." + extension;
			}
			properties.put(ContentModel.PROP_NODE_UUID, uuid);
			properties.put(ContentModel.PROP_NAME, nodeName);
			return nodeService.createNode(parentRef, ContentModel.ASSOC_CONTAINS, QName.createQName(nodeName), type, properties).getChildRef();
		}

	/**
	 * Random hex number.
	 * 
	 * @return char random hex number
	 */
	private static char selectRandomHexNumber() {
		return selectRandomChar(HEXADECIMAL_NUMBERS);
	}

	/**
	 * Select random character from provided string.
	 * 
	 * @param s
	 *            String
	 * @return char random character of string
	 */
	private static char selectRandomChar(String s) {
		return s.charAt(random.nextInt(s.length()));
	}

	/**
	 * Make folders as system user based on relative path structure, such as "a/b/c".
	 * 
	 * @param targetRootRef
	 *            the node reference where to start.
	 * @param relativePath
	 *            a string path
	 * @param folderType
	 *            QName the folder type, such as cm:folder.
	 * @return NodeRef the latest folder node reference of provided path.
	 */
	public NodeRef makeFolders(final NodeRef targetRootRef, final String relativePath, final QName folderType) {
		NodeRef returnRef = AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
			public NodeRef doWork() throws Exception {
				List<String> pathElements = Arrays.asList(relativePath.split("/"));
				FileInfo folders = FileFolderUtil.makeFolders(fileFolderService, targetRootRef, pathElements, folderType);
				return folders.getNodeRef();
			}
		}, ADACTA_SERVICE_USER);
		return returnRef;
	}

	/**
	 * Create folders in a new transaction. It will execute {@link makeFolders}.
	 * 
	 * @param targetRootRef
	 *            the node reference where to start.
	 * @param relativePath
	 *            a string path
	 * @param folderType
	 * @param newTransaction
	 *            true it will create folders in new transaction QName the folder type, such as cm:folder.
	 * @return NodeRef the latest folder node reference of provided path.
	 */
	public NodeRef makeFolders(final NodeRef targetRootRef, final String relativePath, final QName folderType, Boolean newTransaction) {
		if (newTransaction) {
			RetryingTransactionHelper transactionHelper = new RetryingTransactionHelper();
			transactionHelper.setMaxRetries(0);
			transactionHelper.setTransactionService(transactionService);
			return transactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>() {
				@Override
				public NodeRef execute() throws Throwable {
					return makeFolders(targetRootRef, relativePath, folderType);
				}
			}, false, true);
		} else {
			return makeFolders(targetRootRef, relativePath, folderType);
		}
	}

	/**
	 * Parse string value to date.
	 * 
	 * @param providedValue
	 * @return Serializable date value
	 * @throws AlfrescoRuntimeException
	 */
	public Serializable parseDate(final String providedValue) throws AlfrescoRuntimeException {
		for (DateFormat format : DATE_FORMATS) {
			try {
				Serializable value = format.parse(providedValue);
				return value;
			} catch (Exception pe) {
				/* do nothing */ }
		}
		LOGGER.error(String.format("Fail to parse date %s.", providedValue));
		return null;
	}

	/**
	 * Validate if all mandatory properties are not null. The properties are category, subject and document date.
	 * 
	 * @param nodeRef
	 *            NodeRef the node reference
	 * @return true if all mandatory properties are not null.
	 */
	public Boolean hasMandantoryProps(NodeRef nodeRef) {
		String category = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_CATEGORY);
		if (category == null) {
			return false;
		}
		String subject = (String) nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_SUBJECT);
		if (subject == null) {
			return false;
		}
		Date date = (Date) nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_DATE);
		if (date == null) {
			return false;
		}
		if (category.trim().length() == 0) {
			return false;
		}
		if (subject.trim().length() == 0) {
			return false;
		}

		return true;
	}
	

	/**
	 * Renames the provided filename. 
	 * If filename has a dash followed by a number just before the dot that marks the extension, then this number is raised by 1.
	 * If filename has no dash, than "-1" is added to the filename (before the extension).
	 * file-1.txt -> file-2.txt
	 *  
	 * @param filename
	 * @return renamed filename
	 */
	public String renameDuplicateFileName(String filename) {
		int dot = filename.lastIndexOf(".");
		if (dot < 0) {
			return renameNoDot(filename);
		}
		String prefix = filename.substring(0, dot);
		int len = filename.length();
		String postfix = filename.substring(dot + 1, len);

		int dash = filename.lastIndexOf("-");
		if (dash < 0) {
			return prefix + "-1" + "." + postfix;
		} else {
			String index = prefix.substring(dash + 1, prefix.length());
			int indexInt;
			try {
				indexInt = Integer.parseInt(index);
			} catch (NumberFormatException e) {
				// not a index behind the last dash, then simple add -1 
				return prefix+"-1"+"."+postfix;
			}
			indexInt++;
			String prefixDash = prefix.substring(0, dash);
			return prefixDash + "-" + indexInt + "." + postfix;
		}
	}

	private String renameNoDot(String filename) {
			int dash = filename.lastIndexOf("-");
			if (dash < 0) {
				return filename + "-1";
			} else {
				String index = filename.substring(dash + 1, filename.length());
				int indexInt;
				try {
					indexInt = Integer.parseInt(index);
				} catch (NumberFormatException e) {
					// not a index behind the last dash, then simple add -1 
					return filename+"-1";
				}
				String prefixDash = filename.substring(0, dash);
				return prefixDash + "-" + indexInt++;
			}
	}

	
}