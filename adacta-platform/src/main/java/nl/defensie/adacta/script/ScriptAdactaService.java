package nl.defensie.adacta.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.model.DataListModel;
import org.alfresco.repo.action.executer.ScriptActionExecuter;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ValueConverter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.CompositeAction;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaAuthorityService;
import nl.defensie.adacta.service.AdactaFileFolderService;
import nl.defensie.adacta.service.AdactaSearchService;
import nl.defensie.adacta.service.AdactaSiteService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;

/**
 * General script functions for Adacta.
 * 
 * @author Rick de Rooij
 *
 */
public class ScriptAdactaService extends BaseScopableProcessorExtension {

	private static final Log LOGGER = LogFactory.getLog(ScriptAdactaService.class);
	private static final SystemOut systemOut = new SystemOut();

	private static final String DUMMY_FILE = "dummy.pdf";

	private static final String REGEX_UNESQUAPED_COMMAS = "(?<!\\\\);";

	@Value("${adacta.module}")
	private String module;
	@Value("${adacta.extended.permission.enabled}")
	private Boolean permissionsEnabled;

	@Autowired
	@Qualifier("ActionService")
	protected ActionService actionService;
	@Autowired
	@Qualifier("RuleService")
	protected RuleService ruleService;
	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	@Qualifier("PermissionService")
	protected PermissionService permissionService;
	@Autowired
	@Qualifier("AuthorityService")
	protected AuthorityService authorityService;
	@Autowired
	protected AdactaAuthorityService adactaAuthorityService;
	@Autowired
	protected AdactaFileFolderService adactaFileFolderService;
	@Autowired
	protected AdactaSearchService adactaSearchService;
	@Autowired
	protected AdactaSiteService adactaSiteService;
	//@Autowired
	//protected AdactaDatabaseService adactaDatabaseService;
	@Autowired
	@Qualifier("ContentService")
	protected ContentService contentService;
	@Autowired
	@Qualifier("PersonService")
	protected PersonService personService;
	@Autowired
	@Qualifier("NamespaceService")
	protected NamespaceService nameSpaceService;
	@Autowired
	@Qualifier("DictionaryService")
	protected DictionaryService dictionaryService;
	@Autowired
	@Qualifier("ServiceRegistry")
	protected ServiceRegistry serviceRegistry;

	/**
	 * Used for the default search (and advanced search). It will exclude the adacta types. Only if the extended permissions are enabled.
	 * 
	 * @param searchTerm
	 * @return String query excluding the adacta types.
	 */
	public String excludeTypes(String searchTerm) {
		if (permissionsEnabled == false) {
			return searchTerm;
		}

		String q = AdactaSearchService.QUERY_EXCLUDE_TYPES;
		if (searchTerm != null) {
			return searchTerm += (" AND " + q);
		} else {
			return q;
		}
	}

	/**
	 * Create dummy files and personnel files based on a CSV.
	 * 
	 * @param rootFolder
	 *            ScriptNode the folder where to start the three-level structure
	 * @param lineNumber
	 *            Integer the line number only used for logging purposes
	 * @param line
	 *            String one line in CSV separated by ;
	 * @param columnQNames
	 *            QName[] list of QNames that are defined on the first line of the CSV
	 * @param createdDossiers
	 *            Map<String,String> map of node names to node references for existing personnel files
	 * @return NodeRef node reference for the newly created personnel file.
	 */
	public NodeRef createDummyFileFolder(ScriptNode rootFolder, final int lineNumber, final String line, final QName[] columnQNames, final Map<String, String> createdDossiers) {
		// Check for header
		Boolean isHeaderLine = isHeaderLine(line, columnQNames);
		if (isHeaderLine) {
			return null;
		}

		Map<QName, Serializable> properties = getProperties(lineNumber, line, columnQNames);
		if (properties.isEmpty()) {
			LOGGER.warn(String.format("Line number %s is empty.", lineNumber));
			return null;
		} else {
			NodeRef target = null;
			FileInfo fileInfo = null;

			// If running method for documents, get the nodeRef of the existing dossier and create documents
			if (createdDossiers != null) {
				String dossierId = (String) properties.get(ContentModel.PROP_NAME);
				target = new NodeRef(createdDossiers.get(dossierId));

				// Create personnel file
				fileInfo = adactaFileFolderService.getPersonnelFile(target);

				// Update status
				String docStatus = (String) properties.get(AdactaModel.PROP_DOC_STATUS);
				if (docStatus != null) {
					properties.put(AdactaModel.PROP_DOC_STATUS, docStatus.trim());
				}

				// Create document
				NodeRef documentRef = adactaFileFolderService.createNode(fileInfo.getNodeRef(), AdactaModel.TYPE_DOCUMENT, properties, MimetypeMap.MIMETYPE_PDF, true);
				String documentName = (String) nodeService.getProperty(documentRef, ContentModel.PROP_NAME);

				// Stream dummy content
				InputStream is = getClass().getClassLoader().getResourceAsStream(String.format("alfresco/module/%s/files/%s", module, DUMMY_FILE));
				if (is == null) {
					LOGGER.error(String.format("Cannot find %s file.", DUMMY_FILE));
				}
				ContentWriter writer = contentService.getWriter(documentRef, ContentModel.PROP_CONTENT, true);
				writer.guessMimetype(documentName);
				writer.putContent(is);
			} else {
				// Create personnel file
				fileInfo = adactaFileFolderService.creatPersonnelFile(rootFolder.getNodeRef(), (String) properties.get(ContentModel.PROP_NAME), ContentModel.TYPE_FOLDER,
						(String) properties.get(AdactaModel.PROP_EMPLOYEE_NAME), (String) properties.get(AdactaModel.PROP_EMPLOYEE_BSN),
						(String) properties.get(AdactaModel.PROP_EMPLOYEE_MRN), (String) properties.get(AdactaModel.PROP_EMPLOYEE_DEPARTMENT));
			}

			return fileInfo.getNodeRef();
		}
	}

	/**
	 * Create users from CSV. The line contains props to map to
	 * 
	 * @param lineNumber
	 * @param line
	 * @param columnQNames
	 */
	public void createUser(final int lineNumber, final String line, final QName[] columnQNames) {
		// Check for header
		Boolean isHeaderLine = isHeaderLine(line, columnQNames);
		if (isHeaderLine) {
			return;
		}

		Map<QName, Serializable> properties = getProperties(lineNumber, line, columnQNames);
		if (properties.isEmpty()) {
			LOGGER.warn(String.format("Line number %s is empty.", lineNumber));
		} else {
			adactaAuthorityService.createUser(properties);
		}
	}

	/**
	 * Create group based on CSV.
	 * 
	 * @param lineNumber
	 *            Integer the line number
	 * @param line
	 *            String the complete line of CSV
	 * @param columnQNames
	 *            QName[] list of qNames to map
	 */
	public void createGroup(final int lineNumber, final String line, final QName[] columnQNames) {
		// Check for header
		Boolean isHeaderLine = isHeaderLine(line, columnQNames);
		if (isHeaderLine) {
			return;
		}

		Map<QName, Serializable> properties = getProperties(lineNumber, line, columnQNames);
		if (properties.isEmpty()) {
			LOGGER.warn(String.format("Line number %s is empty.", lineNumber));
		} else {

			// Get the names
			String authorityName = (String) properties.get(ContentModel.PROP_AUTHORITY_NAME);
			String authorityDisplayName = (String) properties.get(ContentModel.PROP_AUTHORITY_DISPLAY_NAME);

			if (authorityName != null && authorityDisplayName != null) {
				// Create group
				adactaAuthorityService.createGroup(authorityName, authorityDisplayName);
			} else {
				LOGGER.warn(String.format("Group not created on line number %s.", lineNumber));
			}
		}
	}

	/**
	 * Add authority (user or group) to parent group.
	 * 
	 * @param lineNumber
	 *            Integer the line number
	 * @param line
	 *            String the line to process
	 * @param columnNames
	 *            String[] list of column names
	 */
	public void addMembership(final int lineNumber, final String line, final String[] columnNames) {
		// Check for header line
		String columnName = columnNames[0];
		if (line.contains(columnName)) {
			LOGGER.warn(String.format("Skip header line, because it contains '%s'.", columnName));
			return;
		}

		// Get the values from line
		String[] values = line.split(REGEX_UNESQUAPED_COMMAS);
		if (values.length > columnNames.length) {
			LOGGER.error(String.format("Too many values %s (line number), %s (length values), %s (length columns).", lineNumber, values.length, columnNames.length));
		}

		// Get the values
		String childAuthorityName = values[0];
		String parentAuthorityName = values[1];
		Boolean isGroupAuthority = Boolean.valueOf(values[2]);

		// Add membership
		if (isGroupAuthority) {
			adactaAuthorityService.addGroupToGroup(childAuthorityName, parentAuthorityName);
		} else {
			adactaAuthorityService.addUserToGroup(childAuthorityName, parentAuthorityName);
		}
	}

	/**
	 * Get the header QNames of the CSV file. The first line defines the properties, like cm:name etc.
	 * 
	 * @param fileName
	 *            the CSV file
	 * @return QName[] list of Qnames.
	 */
	public QName[] getHeaderQNamesFromCsvFile(String fileName) {
		BufferedReader reader = null;
		InputStream is = getClass().getClassLoader().getResourceAsStream(String.format("alfresco/module/%s/files/%s", module, fileName));

		InputStreamReader streamReader = new InputStreamReader(is);
		reader = new BufferedReader(streamReader);

		try {
			String line = reader.readLine();
			String[] headers = line.split(REGEX_UNESQUAPED_COMMAS);
			QName[] columnQNames = new QName[headers.length];
			for (int i = 0; i < headers.length; i++) {
				columnQNames[i] = QName.resolveToQName(nameSpaceService, headers[i]);
				if (columnQNames[i] == null) {
					LOGGER.error(String.format("Invalid qName: %s.", headers[i]));
				}
			}
			return columnQNames;

		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (Exception ioe) {
				LOGGER.error(ioe);
			}
		}
		return null;
	}

	/**
	 * Get header names from CSV.
	 * 
	 * @param fileName
	 *            file to read
	 * @return String[] list of header names
	 */
	public String[] getHeaderNamesFromCsvFile(String fileName) {
		BufferedReader reader = null;
		InputStream is = getClass().getClassLoader().getResourceAsStream(String.format("alfresco/module/%s/files/%s", module, fileName));

		InputStreamReader streamReader = new InputStreamReader(is);
		reader = new BufferedReader(streamReader);

		try {
			String line = reader.readLine();
			String[] headers = line.split(REGEX_UNESQUAPED_COMMAS);
			return headers;

		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (Exception ioe) {
				LOGGER.error(ioe);
			}
		}
		return null;
	}

	/**
	 * Get the document library of the Adacta site.
	 * 
	 * @return ScriptNode the doc lib script node reference
	 */
	public ScriptNode getSiteDocumentLibrary() {
		return new ScriptNode(adactaSiteService.getSiteDocumentLibrary(), serviceRegistry, getScope());
	}

	/**
	 * Get the datalist container of the Adacta site.
	 * 
	 * @return ScriptNode the data list root folder.
	 */
	public ScriptNode getDataListContainer() {
		return new ScriptNode(adactaSiteService.getDataListContainer(), serviceRegistry, getScope());
	}

	/**
	 * Make folders based on relative path like 'a/b/c'.
	 * 
	 * @param scriptNode
	 * @param relativePath
	 * @return ScriptNode the latest folder of path
	 */
	public ScriptNode makeFolders(ScriptNode scriptNode, String relativePath) {
		return makeFolders(scriptNode, relativePath, false);
	}

	/**
	 * Get the folder with root aspect index.
	 * 
	 * @return ScriptNode
	 */
	public ScriptNode getRootIndex() {
		return new ScriptNode(adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_INDEX), serviceRegistry, getScope());
	}

	/**
	 * Get the folder with root aspect import.
	 * 
	 * @return ScriptNode
	 */
	public ScriptNode getRootImport() {
		return new ScriptNode(adactaSearchService.getNodeWithAspect(ContentModel.TYPE_FOLDER, AdactaModel.ASPECT_ROOT_IMPORT), serviceRegistry, getScope());
	}

	/**
	 * Get the scripts folder.
	 * 
	 * @return
	 */
	public ScriptNode getDictionaryScripts() {
		NodeRef scripts = adactaSearchService.getFirstNodeAtXPath("/app:company_home/app:dictionary/app:scripts");
		return new ScriptNode(scripts, serviceRegistry, getScope());
	}

	/**
	 * Make folders based on relative path like 'a/b/c'. Can be in a new transaction.
	 * 
	 * @param scriptNode
	 * @param relativePath
	 * @param newTransaction
	 * @return ScriptNode the latest folder of path
	 */
	public ScriptNode makeFolders(final ScriptNode scriptNode, final String relativePath, Boolean newTransaction) {
		NodeRef folder = null;
		if (newTransaction) {
			folder = adactaFileFolderService.makeFolders(scriptNode.getNodeRef(), relativePath, ContentModel.TYPE_FOLDER, true);
			return new ScriptNode(folder, serviceRegistry, getScope());
		} else {
			folder = adactaFileFolderService.makeFolders(scriptNode.getNodeRef(), relativePath, ContentModel.TYPE_FOLDER);
			return new ScriptNode(folder, serviceRegistry, getScope());
		}
	}

	/**
	 * Migration action to transform a personnel file into a dossier
	 * 
	 * @param folder
	 *            ScriptNode of the folder to be transformed into a dossier
	 */
	public void changeFolderTypeAndMetadata(ScriptNode folder) {
		// Create personnel file
		adactaFileFolderService.createDossierFromFolder(folder.getNodeRef(), AdactaModel.TYPE_DOSSIER);
	}

	/**
	 * Set input stream into a provided node reference.
	 * 
	 * @param classpath
	 * @param scriptNode
	 * @param nodeName
	 */
	public void putFileInputStreamInNode(ScriptNode scriptNode, String fileName) {
		InputStream is = getClass().getClassLoader().getResourceAsStream(String.format("alfresco/module/%s/files/%s", module, fileName));
		if (is == null) {
			throw new AlfrescoRuntimeException("File could not be found.");
		}
		ContentWriter writer = contentService.getWriter(scriptNode.getNodeRef(), ContentModel.PROP_CONTENT, true);
		writer.guessMimetype(fileName);
		writer.putContent(is);
	}

	/**
	 * Import script.
	 * 
	 * @param parent
	 * @param scriptName
	 * @param nodeName
	 * @return
	 */
	public ScriptNode importClasspathScript(ScriptNode parent, String scriptName, String nodeName) {
		return importClasspathFile(String.format("alfresco/module/%s/script/%s", module, scriptName), parent, nodeName);
	}

	/**
	 * Import file from folder files.
	 * 
	 * @param parent
	 *            the parent node
	 * @param nodeName
	 *            the file name to import
	 * @return ScriptNode the new created node.
	 */
	public ScriptNode importClasspathFile(ScriptNode parent, String nodeName) {
		String classpath = String.format("alfresco/module/%s/files/%s", module, nodeName);
		return importClasspathFile(classpath, parent, nodeName);
	}

	/**
	 * Import file based on classpath.
	 * 
	 * @param classpath
	 *            the path where file located
	 * @param parent
	 *            parent node
	 * @param nodeName
	 *            is optional
	 * @return ScriptNode to update new created node
	 */
	public ScriptNode importClasspathFile(String classpath, ScriptNode parent, String nodeName) {
		NodeRef result = null;

		if (classpath == null) {
			classpath = String.format("alfresco/module/%s/files/%s", module, DUMMY_FILE);
		}

		// Check if we have a node name
		if (nodeName == null) {
			int index = classpath.lastIndexOf('/');
			String lastPart = classpath.substring(index + 1);
			nodeName = lastPart;
		}

		if (nodeService.exists(parent.getNodeRef())) {

			Map<QName, Serializable> rowProps = new HashMap<QName, Serializable>();
			rowProps.put(ContentModel.PROP_NAME, nodeName);
			rowProps.put(AdactaModel.PROP_DOC_WORK_DOSSIER, null);
			result = adactaFileFolderService.createNode(parent.getNodeRef(), ContentModel.TYPE_CONTENT, rowProps, null, false);

			InputStream is = getClass().getClassLoader().getResourceAsStream(classpath);
			if (is == null) {
				throw new AlfrescoRuntimeException("File could not be found on the classpath: " + classpath);
			}
			ContentWriter writer = contentService.getWriter(result, ContentModel.PROP_CONTENT, true);
			writer.guessMimetype(nodeName);
			writer.putContent(is);
		}
		return new ScriptNode(result, serviceRegistry, getScope());
	}

	/**
	 * Get file from classpath located in the 'files' folder of module.
	 * 
	 * @param fileName
	 * @return String the content of the file
	 */
	public String getFileFromClasspath(String fileName) {
		return getContentFromClasspath(String.format("alfresco/module/%s/files/%s", module, fileName));
	}

	/**
	 * Get content as string from classpath.
	 * 
	 * @param classpath
	 * @return String the content
	 */
	public String getContentFromClasspath(String classpath) {
		String isString = null;
		InputStream is = getClass().getClassLoader().getResourceAsStream(classpath);
		isString = getStringFromInputStream(is);
		return isString;
	}

	/**
	 * Import CSV file into a data list.
	 * 
	 * @param csvFile
	 *            ScriptNode
	 * @param dataList
	 */
	public void importCsvToDataList(ScriptNode csvFileScriptNode, ScriptNode dataListScriptNode) {

		NodeRef csvFile = csvFileScriptNode.getNodeRef();
		NodeRef dataList = dataListScriptNode.getNodeRef();

		BufferedReader reader = null;
		try {
			ContentReader contentReader = contentService.getReader(csvFile, ContentModel.PROP_CONTENT);
			InputStream contentStream = contentReader.getContentInputStream();
			InputStreamReader streamReader = new InputStreamReader(contentStream);
			reader = new BufferedReader(streamReader);

			String line = reader.readLine();
			QName[] columnQNames = getHeaderNames(line);

			line = reader.readLine();
			int lineNumber = 2;
			while (line != null) {
				Map<QName, Serializable> properties = getProperties(lineNumber, line, columnQNames);
				if (properties.isEmpty()) {
					LOGGER.warn(lineNumber);
				} else {
					String dataListItemProperty = (String) nodeService.getProperty(dataList, DataListModel.PROP_DATALIST_ITEM_TYPE);
					QName type = QName.createQName(dataListItemProperty, nameSpaceService);
					nodeService.createNode(dataList, ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS, type, properties);
				}
				line = reader.readLine();
				lineNumber++;
			}
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (Exception ioe) {
				LOGGER.error(ioe);
			}
		}
	}

	/**
	 * Get the column QNames.
	 * 
	 * @param line
	 *            the header line .
	 * @return the found QNames.
	 * @throws AlfrescoRuntimeException
	 * 
	 */
	private QName[] getHeaderNames(final String line) throws AlfrescoRuntimeException {
		if (line == null || line.isEmpty()) {
			LOGGER.error("No Header in file.");
		}
		String[] headers = line.split(REGEX_UNESQUAPED_COMMAS);
		QName[] columnQNames = new QName[headers.length];
		for (int i = 0; i < headers.length; i++) {
			columnQNames[i] = QName.resolveToQName(nameSpaceService, headers[i]);
			if (columnQNames[i] == null) {
				LOGGER.error(String.format("Invalid qName: %s.", headers[i]));
			}
		}
		return columnQNames;
	}

	/**
	 * Transform input stream to String.
	 * 
	 * @param is
	 *            InputStream the stream
	 * @return String the content as string.
	 */
	private static String getStringFromInputStream(InputStream is) {
		StringBuilder inputStringBuilder = new StringBuilder();
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line = bufferedReader.readLine();

			while (line != null) {
				inputStringBuilder.append(line);
				inputStringBuilder.append('\n');
				line = bufferedReader.readLine();
			}
			return inputStringBuilder.toString();
		} catch (IOException e) {
			LOGGER.error(e);
		}
		return inputStringBuilder.toString();
	}

	/**
	 * Create a properties map based on the list of QNames.
	 * 
	 * @param lineNumber
	 *            Integer the line of CSV
	 * @param line
	 *            String all the values separated by ;
	 * @param columnQNames
	 *            QName[] list of QNames.
	 * @return Map<QName, Serializable> properties map
	 * @throws AlfrescoRuntimeException
	 */
	private Map<QName, Serializable> getProperties(final int lineNumber, final String line, final QName[] columnQNames) throws AlfrescoRuntimeException {

		String[] values = line.split(REGEX_UNESQUAPED_COMMAS);
		if (values.length > columnQNames.length) {
			LOGGER.error(String.format("Too many values %s (line number), %s (length values), %s (length columns).", lineNumber, values.length, columnQNames.length));
		}

		Map<QName, Serializable> rowProps = new HashMap<QName, Serializable>();
		for (int i = 0; i < values.length; i++) {
			String readValue = values[i];
			QName propertyName = columnQNames[i];
			if (propertyName == null) {
				LOGGER.error(String.format("No property set %s, %s, %s.", readValue, i, lineNumber));
			}
			try {
				Serializable propertyValue = getValue(propertyName, readValue);
				if (propertyValue != null) {
					rowProps.put(propertyName, propertyValue);
				}
			} catch (AlfrescoRuntimeException e) {
				LOGGER.error(String.format("Unable to parse %s, %s, %s.", readValue, i, lineNumber));
			}
		}
		return rowProps;
	}

	/**
	 * Transform value to primitive data types.
	 * 
	 * @param propertyName
	 *            QName the property
	 * @param providedValue
	 *            String provided value in CSV
	 * @return Serializable the value converted to the data type.
	 */
	private Serializable getValue(final QName propertyName, final String providedValue) {
		if (providedValue == null || "".equals(providedValue) || "-".equals(providedValue)) {
			return null;
		}
		String typeName = getType(propertyName);

		boolean isInteger = typeName.equals(Integer.class.getName());
		boolean isLong = typeName.equals(Long.class.getName());
		boolean isDate = typeName.equals(Date.class.getName());
		boolean isBoolean = typeName.equals(Boolean.class.getName());

		Serializable value = providedValue;
		try {
			if (isInteger) {
				value = Integer.parseInt(providedValue);
			} else if (isLong) {
				value = Long.parseLong(providedValue);
			} else if (isDate) {
				value = adactaFileFolderService.parseDate(providedValue);
			} else if (isBoolean) {
				value = Boolean.parseBoolean(providedValue);
			}

		} catch (Exception e) {
			LOGGER.error(String.format("Unable to parse as %s, %s, %s.", value, typeName, propertyName));
		}
		return value;
	}

	/**
	 * Get the property matching the data model.
	 * 
	 * @param property
	 * @return String type name
	 * @throws AlfrescoRuntimeException
	 */
	private String getType(QName property) throws AlfrescoRuntimeException {
		String typeName = null;
		if (dictionaryService.getProperty(property) != null) {
			PropertyDefinition propertyDefinition = dictionaryService.getProperty(property);
			DataTypeDefinition typeDefinition = propertyDefinition.getDataType();
			typeName = typeDefinition.getJavaClassName();
		} else {
			LOGGER.error(String.format("Property not found %s.", property));
		}
		return typeName;
	}

	/**
	 * Validates if line is the header.
	 * 
	 * @param line
	 *            line to execute
	 * @param columnQNames
	 *            list of qNames
	 * @return true if prefixed qnames are in the provided line.
	 */
	private Boolean isHeaderLine(String line, QName[] columnQNames) {
		// Get the first
		QName qName = columnQNames[0];
		QName prefixedName = qName.getPrefixedQName(nameSpaceService);
		if (line.contains(prefixedName.toPrefixString())) {
			LOGGER.warn(String.format("Skip header line, because it contains '%s'.", prefixedName.toPrefixString()));
			return true;
		}
		return false;
	}

	/**
	 * Create script action rule.
	 * 
	 * @param nodeRef
	 *            the folder reference
	 * @param scriptRef
	 *            the script reference
	 * @param ruleType
	 *            inbound, update or outbound
	 * @param title
	 *            a title
	 * @param description
	 *            a rule description
	 * @param executeAsynchronously
	 *            async or not
	 * @param applyToChildren
	 *            apply to sub folders or not
	 */
	public void createScriptActionRule(ScriptNode scriptNode, ScriptNode jscriptNode, String ruleType, String title, String description, boolean executeAsynchronously,
			boolean applyToChildren) {

		List<ScriptNode> scriptNodeList = new ArrayList<ScriptNode>();
		scriptNodeList.add(jscriptNode);

		List<String> ruleTypes = new ArrayList<String>();
		ruleTypes.add(ruleType);

		createScriptActionRuleWithCondition(scriptNode, scriptNodeList, ruleTypes, new ArrayList<String>(), new ArrayList<String>(), title, description, executeAsynchronously,
				applyToChildren);
	}

	/**
	 * Execute query against JDBC data source.
	 * 
	 * @param dataSourceName
	 *            String the bean id
	 * @param sql
	 *            String SQL query
	 * @param params
	 *            Object additional params used in query
	 * @return Map<String, Object>[] list of items.
	 */
	//public Map<String, Object>[] dbQuery(String dataSourceName, String sql, Object... params) {
	//	return adactaDatabaseService.query(dataSourceName, sql, params);
	//}

	/**
	 * Execute static SQL statement.
	 * 
	 * @param sql
	 */
//	public void dbExecute(String sql) {
//		adactaDatabaseService.execute(adactaDatabaseService.getDefaultDataSourceName(), sql);
//	}

	/**
	 * Get employee from database.
	 * 
	 * @param employeeNumber
	 *            String employee number
	 * @return DISP8Owner[] list of items of employee
	 */
//	public DISP8Owner[] dbEmployee(String dataSourceName, String employeeNumber) {
//		List<DISP8Owner> items = adactaDatabaseService.getEmployee(dataSourceName, employeeNumber);
//		return items.toArray(new DISP8Owner[items.size()]);
//	}

	/**
	 * Get employees by last change date.
	 * 
	 * @param s
	 *            String date
	 * @return DISP8Owner[] list of items of employee
	 */
	//public DISP8Owner[] dbEmployeesByLastChangeDate(String dataSourceName, String s) {
	//	List<DISP8Owner> items = adactaDatabaseService.getEmployeesByLastChangeDate(dataSourceName, s);
	//	return items.toArray(new DISP8Owner[items.size()]);
	//}

	/**
	 * Search using the Adacta search service that validates the permission of the nodes when executing a query.
	 * 
	 * @param search
	 *            Object search parameters
	 * @return Scriptable script nodes.
	 */
	public Scriptable query(Object search) {
		Object[] results = null;

		if (search instanceof Serializable) {
			Serializable obj = new ValueConverter().convertValueForRepo((Serializable) search);
			if (obj instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<Serializable, Serializable> def = (Map<Serializable, Serializable>) obj;

				// test for mandatory values
				String query = (String) def.get("query");
				if (query == null || query.length() == 0) {
					throw new AlfrescoRuntimeException("Failed to search: Missing mandatory 'query' value.");
				}

				// collect optional values
				String store = (String) def.get("store");
				String language = (String) def.get("language");
				@SuppressWarnings("unchecked")
				List<Map<Serializable, Serializable>> sort = (List<Map<Serializable, Serializable>>) def.get("sort");
				@SuppressWarnings("unchecked")
				Map<Serializable, Serializable> page = (Map<Serializable, Serializable>) def.get("page");
				String namespace = (String) def.get("namespace");
				String defaultField = (String) def.get("defaultField");

				// extract supplied values

				// sorting columns
				SortColumn[] sortColumns = null;
				if (sort != null) {
					sortColumns = new SortColumn[sort.size()];
					int index = 0;
					for (Map<Serializable, Serializable> column : sort) {
						String strCol = (String) column.get("column");
						if (strCol == null || strCol.length() == 0) {
							throw new AlfrescoRuntimeException("Failed to search: Missing mandatory 'sort: column' value.");
						}
						Boolean boolAsc = (Boolean) column.get("ascending");
						boolean ascending = (boolAsc != null ? boolAsc.booleanValue() : false);
						sortColumns[index++] = new SortColumn(strCol, ascending);
					}
				}

				// paging settings
				int maxResults = -1;
				int skipResults = 0;
				if (page != null) {
					if (page.get("maxItems") != null) {
						Object maxItems = page.get("maxItems");
						if (maxItems instanceof Number) {
							maxResults = ((Number) maxItems).intValue();
						} else if (maxItems instanceof String) {
							// try and convert to int (which it what it should be!)
							maxResults = Integer.parseInt((String) maxItems);
						}
					}
					if (page.get("skipCount") != null) {
						Object skipCount = page.get("skipCount");
						if (skipCount instanceof Number) {
							skipResults = ((Number) page.get("skipCount")).intValue();
						} else if (skipCount instanceof String) {
							skipResults = Integer.parseInt((String) skipCount);
						}
					}
				}

				// query templates
				Map<String, String> queryTemplates = null;
				@SuppressWarnings("unchecked")
				List<Map<Serializable, Serializable>> templates = (List<Map<Serializable, Serializable>>) def.get("templates");
				if (templates != null) {
					queryTemplates = new HashMap<String, String>(templates.size(), 1.0f);

					for (Map<Serializable, Serializable> template : templates) {
						String field = (String) template.get("field");
						if (field == null || field.length() == 0) {
							throw new AlfrescoRuntimeException("Failed to search: Missing mandatory 'template: field' value.");
						}
						String t = (String) template.get("template");
						if (t == null || t.length() == 0) {
							throw new AlfrescoRuntimeException("Failed to search: Missing mandatory 'template: template' value.");
						}
						queryTemplates.put(field, t);
					}
				}

				SearchParameters sp = new SearchParameters();
				sp.addStore(store != null ? new StoreRef(store) : new StoreRef("workspace://SpacesStore"));
				sp.setLanguage(language != null ? language : SearchService.LANGUAGE_LUCENE);
				sp.setQuery(query);
				if (defaultField != null) {
					sp.setDefaultFieldName(defaultField);
				}
				if (namespace != null) {
					sp.setNamespace(namespace);
				}
				if (maxResults > 0) {
					sp.setLimit(maxResults);
					sp.setLimitBy(LimitBy.FINAL_SIZE);
				}
				if (skipResults > 0) {
					sp.setSkipCount(skipResults);
				}
				if (sort != null) {
					for (SortColumn sd : sortColumns) {
						sp.addSort(sd.column, sd.asc);
					}
				}
				if (queryTemplates != null) {
					for (String field : queryTemplates.keySet()) {
						sp.addQueryTemplate(field, queryTemplates.get(field));
					}
				}

				// execute search based on search definition
				List<NodeRef> nodeRefs = adactaSearchService.query(sp);
				Object[] objs = new Object[nodeRefs.size()];
				for (int i = 0; i < nodeRefs.size(); i++) {
					objs[i] = new ScriptNode(nodeRefs.get(i), serviceRegistry);
				}

				results = objs;
			}
		}

		if (results == null) {
			results = new Object[0];
		}

		return Context.getCurrentContext().newArray(getScope(), results);
	}

	/**
	 * Create script action rule.
	 * 
	 * @param nodeRef
	 *            the folder reference
	 * @param scriptRef
	 *            the list of script references
	 * @param ruleTypes
	 *            list with possible values: inbound, update or outbound
	 * @param isOfTypeValues
	 *            list with node types to be checked as condition
	 * @param hasAspectValues
	 *            list with aspects to be checked as condition
	 * @param title
	 *            a rule title
	 * @param description
	 *            a rule description
	 * @param executeAsynchronously
	 *            async or not
	 * @param applyToChildren
	 *            apply to sub folders or not
	 */
	public void createScriptActionRuleWithCondition(ScriptNode scriptNode, List<ScriptNode> jscriptNodes, List<String> ruleTypes, List<String> conditionTypes,
			List<String> conditionAspects, String title, String description, boolean executeAsynchronously, boolean applyToChildren) {

		// Setup rule
		Rule rule = new Rule();
		rule.setRuleTypes(ruleTypes);
		rule.setTitle(title);
		rule.setDescription(description);
		rule.applyToChildren(applyToChildren);
		rule.setRuleDisabled(false);
		rule.setExecuteAsynchronously(executeAsynchronously);

		CompositeAction compositeAction = actionService.createCompositeAction();

		if (conditionTypes.size() == 0 && conditionAspects.size() == 0) {
			ActionCondition condition = actionService.createActionCondition("no-condition");
			condition.setParameterValues(new HashMap<String, Serializable>());
			condition.setInvertCondition(false);
			compositeAction.addActionCondition(condition);
		} else {
			for (String type : conditionTypes) {
				HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
				QName typeQName = QName.createQName(type);
				parameters.put("type", typeQName);

				ActionCondition condition = actionService.createActionCondition("is-subtype");
				condition.setParameterValues(parameters);
				compositeAction.addActionCondition(condition);
			}

			for (String aspect : conditionAspects) {
				HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
				QName aspectQName = QName.createQName(aspect);
				parameters.put("aspect", aspectQName);

				ActionCondition condition = actionService.createActionCondition("has-aspect");
				condition.setParameterValues(parameters);
				compositeAction.addActionCondition(condition);
			}
		}

		// Create the script action
		for (ScriptNode jscriptNode : jscriptNodes) {
			Action action = actionService.createAction(ScriptActionExecuter.NAME);
			action.setParameterValue(ScriptActionExecuter.PARAM_SCRIPTREF, jscriptNode.getNodeRef());
			compositeAction.addAction(action);
		}

		rule.setAction(compositeAction);
		ruleService.saveRule(scriptNode.getNodeRef(), rule);

		if (LOGGER.isInfoEnabled()) {
			String nodeName = (String) nodeService.getProperty(scriptNode.getNodeRef(), ContentModel.PROP_NAME);
			String scriptName = (String) nodeService.getProperty(scriptNode.getNodeRef(), ContentModel.PROP_NAME);
			LOGGER.info(String.format("Apply script '%s' to folder '%s'. ", scriptName, nodeName));
		}
	}

	/**
	 * Create new group.
	 * 
	 * @param groupName
	 *            String group name include GROUP_ prefix.
	 */
	public void createGroup(String groupName) {
		adactaAuthorityService.createGroup(groupName);
	}

	public void removeAllRules(ScriptNode scriptNode) {
		ruleService.removeAllRules(scriptNode.getNodeRef());
	}

	/**
	 * Add existing group to the existing parent group.
	 * 
	 * @param childGroupName
	 *            the name of the sub group.
	 * @param parentGroupName
	 *            the name of the parent group.
	 */
	public void addGroupToGroup(String childGroupName, String parentGroupName) {
		adactaAuthorityService.addGroupToGroup(childGroupName, parentGroupName);
	}

	/**
	 * Set group permissions on documents to be indexed (import files or scan folders).
	 * 
	 * @param userName
	 *            the name of the owner user.
	 * @param node
	 *            the import file or scan folder.
	 */
	public void setDepartmentGroupPermissions(String userName, ScriptNode node) {
		List<String> groups = adactaAuthorityService.getDepartmentGroups(userName);
		permissionService.setInheritParentPermissions(node.getNodeRef(), false);

		for (String group : groups) {
			permissionService.setPermission(node.getNodeRef(), group, PermissionService.COORDINATOR, true);
		}

		// Also set permissions for everyone in the "GROUP_ADACTA_BEHEERDER" group
		permissionService.setPermission(node.getNodeRef(), "GROUP_ADACTA_BEHEERDER", PermissionService.COORDINATOR, true);
	}

	/**
	 * Get authorization information for easy debugging the Adacta authorization mechanims. This can be used in javascript console.
	 * 
	 * @param userName
	 *            String user name or node ref
	 * @param nodeName
	 *            String node name or node ref
	 * @return String auth info
	 */
	public String getAuthorizationInfo(String userName, String nodeName) {

		StringBuilder sb = new StringBuilder();

		sb.append("### USER INFO ###");
		NodeRef personRef = null;
		if (NodeRef.isNodeRef(userName)) {
			personRef = new NodeRef(userName);
		} else {
			personRef = personService.getPerson(userName);
		}

		if (personRef != null) {
			sb.append(String.format("\nNAME: %s",
					nodeService.getProperty(personRef, ContentModel.PROP_FIRSTNAME) + " " + nodeService.getProperty(personRef, ContentModel.PROP_LASTNAME)));
			sb.append(String.format("\nEMPLOYEE ID: %s", nodeService.getProperty(personRef, AdactaModel.PROP_EMPLOYEE_ID)));
			sb.append(String.format("\nDP CODE: %s", nodeService.getProperty(personRef, AdactaModel.PROP_DP_CODE)));

			sb.append("\n");
			sb.append("### USER GROUPS ###");
			Set<String> authorities = adactaAuthorityService.getAuthoritiesForUserAsSystem(personService.getPerson(personRef).getUserName());
			for (String auth : authorities) {
				String displayName = authorityService.getAuthorityDisplayName(auth);
				sb.append("\n" + auth + " (" + displayName + ")");
			}
		} else {
			sb.append(String.format("\nUSER %s NOT FOUND.", userName));
		}

		NodeRef nodeRef = null;
		if (NodeRef.isNodeRef(nodeName)) {
			nodeRef = new NodeRef(nodeName);
		} else {
			nodeRef = adactaSearchService.getDossierOrDocumentByName(nodeName);
		}

		if (nodeRef != null) {
			sb.append("\n");
			if (nodeService.getType(nodeRef).equals(AdactaModel.TYPE_DOCUMENT)) {
				sb.append("### DOCUMENT INFO ###");
				NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
				sb.append(String.format("\nPARENT FOLDER NAME: %s", nodeService.getProperty(parentRef, ContentModel.PROP_NAME)));
				sb.append(String.format("\nRUBRIEK: %s", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_CATEGORY)));
				sb.append(String.format("\nONDERWERP: %s", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_SUBJECT)));
				sb.append(String.format("\nSTATUS: %s", nodeService.getProperty(nodeRef, AdactaModel.PROP_DOC_STATUS)));
			} else if (nodeService.getType(nodeRef).equals(AdactaModel.TYPE_DOSSIER)) {
				sb.append("### DOSSIER INFO ###");
				sb.append(String.format("\nEMPLOYEE NAME: %s", nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_NAME)));
				sb.append(String.format("\nEMPLOYEE NUMBER: %s", nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_NUMBER)));
				@SuppressWarnings("unchecked")
				ArrayList<String> dpCodes = (ArrayList<String>) nodeService.getProperty(nodeRef, AdactaModel.PROP_EMPLOYEE_DP_CODES);
				if (dpCodes != null) {
					sb.append(String.format("\nDP CODES: %s", StringUtils.join(dpCodes, ",")));
				} else {
					sb.append("\nDP CODES: NONE");
				}
			} else if (nodeService.getType(nodeRef).equals(ContentModel.TYPE_FOLDER)) {
				sb.append("### SCAN BATCH PERMISSIONS ###");
				Set<AccessPermission> accessPermissions = permissionService.getAllSetPermissions(nodeRef);
				for (AccessPermission accessPermission : accessPermissions) {
					String auth = accessPermission.getAuthority();
					String displayName = authorityService.getAuthorityDisplayName(auth);
					sb.append("\n" + accessPermission.getPermission() + " " + auth + " (" + displayName + ") " + accessPermission.getAccessStatus().toString());
				}
				sb.append("\n### SCAN BATCH PROPERTIES ###");
				sb.append(String.format("\nEMPLOYEE: %s", nodeService.getProperty(nodeRef, AdactaModel.PROP_SCAN_EMPLOYEE)));
				sb.append(String.format("\nSEQUENCE NUMBER: %s", nodeService.getProperty(nodeRef, AdactaModel.PROP_SCAN_SEQ_NR)));
				sb.append(String.format("\nWA NUMBER: %s", nodeService.getProperty(nodeRef, AdactaModel.PROP_SCAN_WA_NR)));
				sb.append(String.format("\nBATCH NAME: %s", nodeService.getProperty(nodeRef, AdactaModel.PROP_SCAN_BATCH_NAME)));
				sb.append(String.format("\nBATCH SIZE: %s", nodeService.getProperty(nodeRef, AdactaModel.PROP_SCAN_BATCH_SIZE)));
			}
		} else {
			sb.append(String.format("\nNODE %s NOT FOUND.", nodeName));
		}

		return sb.toString();
	}

	/**
	 * Custom root logger for Adacta.
	 * 
	 */
	public void log(String str) {
		debug(str);
	}

	public void trace(String str) {
		LOGGER.trace(str);
	}

	public void debug(String str) {
		LOGGER.debug(str);
	}

	public void info(String str) {
		LOGGER.info(str);
	}

	public void warn(String str) {
		LOGGER.warn(str);
	}

	public SystemOut getSystem() {
		return systemOut;
	}

	public static class SystemOut {
		public void out(Object str) {
			System.out.println(str);
		}
	}

	/**
	 * Search sort column
	 */
	public class SortColumn {
		/**
		 * Constructor
		 * 
		 * @param column
		 *            column to sort on
		 * @param asc
		 *            sort direction
		 */
		public SortColumn(String column, boolean asc) {
			this.column = column;
			this.asc = asc;
		}

		public String column;
		public boolean asc;
	}
	/**
	 * Used in overriden upload webscript. Returns true if fileName occurs in parentFolder, else false.
	 * @param parentFolderNodeRef
	 * @param fileName
	 * @return
	 */
	public boolean fileExistsAsSystem(final ScriptNode parentFolderNodeRef, final String fileName) {
		return AuthenticationUtil.runAsSystem(new RunAsWork<Boolean>() {
			public Boolean doWork() throws Exception {
				NodeRef nodeRef = parentFolderNodeRef.getNodeRef();
				NodeRef nr = nodeService.getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, fileName);
				if (nr==null) {
					return false;
				}else {
					return true;
				}
			}
			
		});
	}
}