package nl.defensie.adacta.action.schedule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.service.AdactaFileFolderService;
import nl.defensie.adacta.service.AdactaSearchService;

/**
 * Scheduled action that will import WIT (Word Import Tool) documents. It will only process the documents if personnel file can be located.
 * 
 * @author Rick de Rooij
 *
 */
public class ImportWITDocumentsActionExecuter extends ActionExecuterAbstractBase {

	protected Logger LOGGER = Logger.getLogger(this.getClass());

	public static final String NAME = AdactaModel.PREFIX + "ImportWITDocuments";

	public static final String PARAM_DEFAULT = "default";

	/**
	 * This static map for mapping XML properties with node properties
	 */
	static final HashMap<String, QName> XML_MAPPING = new HashMap<String, QName>();

	{
		{
			XML_MAPPING.put("bsn", AdactaModel.PROP_EMPLOYEE_BSN);
			XML_MAPPING.put("MRN", AdactaModel.PROP_EMPLOYEE_MRN);
			XML_MAPPING.put("onderdeel", AdactaModel.PROP_EMPLOYEE_DEPARTMENT);
			XML_MAPPING.put("MRN", AdactaModel.PROP_EMPLOYEE_MRN);
			XML_MAPPING.put("emplID", AdactaModel.PROP_EMPLOYEE_NUMBER);
			XML_MAPPING.put("naam", AdactaModel.PROP_EMPLOYEE_NAME);

			XML_MAPPING.put("categoryCode", AdactaModel.PROP_DOC_CATEGORY);
			XML_MAPPING.put("subjectCode", AdactaModel.PROP_DOC_SUBJECT);
			XML_MAPPING.put("dateNumber", AdactaModel.PROP_DOC_DATE);
			XML_MAPPING.put("caseNumber", AdactaModel.PROP_DOC_CASE_NUMBER);
		}
	};

	@Value("${adacta.import.wit.documents.path}")
	protected String importPath;

	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	@Qualifier("ContentService")
	protected ContentService contentService;
	@Autowired
	protected AdactaSearchService adactaSearchService;
	@Autowired
	protected AdactaFileFolderService adactaFileFolderService;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		LOGGER.info("running job import WIT documents");
		// Get the import directory and stop if not exists.
		File dir = getFileOrDirectory(importPath);
		if (!dir.exists()) {
			LOGGER.warn(String.format("Importing WIT documents failed. Directory '%s' not exists.", importPath));
			return;
		}

		// Iterate files in directory
		try {
			DirectoryStream<Path> stream = Files.newDirectoryStream(dir.toPath());

			Iterator<Path> iterator = stream.iterator();
			while (iterator.hasNext()) {
				Path path = iterator.next();
				File file = path.toFile();

				if (isXml(file.getName()) && processXml(file)) {
					file.delete();
				}
			}
			stream.close();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(new ParameterDefinitionImpl(PARAM_DEFAULT, DataTypeDefinition.ANY, false, getParamDisplayLabel(PARAM_DEFAULT)));
	}

	/**
	 * Process XML file by getting the file, search for personnel file and write file in personnel file.
	 * 
	 * @param file
	 *            File the XML file
	 * @return true if document is imported in personnel file.
	 */
	private Boolean processXml(File file) {
		Boolean result = false;
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(file);

			Element rootElement = doc.getDocumentElement();
			NodeList childList = rootElement.getChildNodes();

			for (int i = 0; i < childList.getLength(); i++) {
				Node currentNode = childList.item(i);

				switch (currentNode.getNodeType()) {
				case Node.TEXT_NODE:
					break;
				case Node.ELEMENT_NODE:
					// get the file name
					NamedNodeMap nnm = currentNode.getAttributes();
					String importFileName = nnm.getNamedItem("importfile").getNodeValue();

					// create path
					String filePath = importPath + "/" + importFileName;

					// get file
					File fileToImport = getFileOrDirectory(filePath);
					if (!fileToImport.exists()) {
						LOGGER.warn(String.format("File %s not exists.", filePath));
						break;
					}

					// file exists, so get the props
					Map<QName, Serializable> props = getProperties(currentNode);

					// search for personnel file
					String bsn = (String) props.get(AdactaModel.PROP_EMPLOYEE_BSN);
					NodeRef personnelFile = adactaSearchService.getPersonnelFileByBSN(bsn);
					if (personnelFile == null) {
						LOGGER.warn(String.format("Personnel file with ID '%s' not found", bsn));
						break;
					}
					String dosEmplid = (String)nodeService.getProperty(personnelFile, AdactaModel.PROP_EMPLOYEE_NUMBER);
					String docEmplid = (String)props.get(AdactaModel.PROP_EMPLOYEE_NUMBER);

					if (!dosEmplid.equalsIgnoreCase(docEmplid)){
						props.put(AdactaModel.PROP_EMPLOYEE_NUMBER, dosEmplid);
					}
					
					// Create document
					NodeRef documentRef = adactaFileFolderService.createNode(personnelFile, AdactaModel.TYPE_DOCUMENT, props, MimetypeMap.MIMETYPE_PDF, true);

					// write pdf to node
					InputStream is = new FileInputStream(fileToImport);
					ContentWriter writer = contentService.getWriter(documentRef, ContentModel.PROP_CONTENT, true);
					writer.guessMimetype(importFileName);
					writer.putContent(is);
					is.close();

					// delete pdf
					fileToImport.delete();

					result = true;

					break;
				}
			}
		} catch (ParserConfigurationException e) {
			LOGGER.error(e);
		} catch (SAXException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		}
		return result;
	}

	/**
	 * Get all XML properties to map on the node.
	 * 
	 * @param node
	 * @return Map<QName, Serializable> list of properties
	 */
	private Map<QName, Serializable> getProperties(Node node) {

		Map<QName, Serializable> properties = new HashMap<QName, Serializable>();

		NodeList propertyList = node.getChildNodes();
		for (int j = 0; j < propertyList.getLength(); j++) {
			Node property = propertyList.item(j);

			switch (node.getNodeType()) {
			case Node.TEXT_NODE:
				break;
			case Node.ELEMENT_NODE:
				if (property.getNodeName().equalsIgnoreCase("property")) {
					// get the prop name attribute
					NamedNodeMap nnm = property.getAttributes();
					String propName = nnm.getNamedItem("name").getNodeValue();

					// Get the property qName
					QName propQname = getProperty(propName);
					if (propQname != null) {
						// convert date
						if (propQname.equals(AdactaModel.PROP_DOC_DATE)) {
							String strDate = property.getTextContent();
							Date date = (Date) adactaFileFolderService.parseDate(strDate);
							properties.put(propQname, date);
						} else {
							// add to list
							properties.put(propQname, property.getTextContent());
						}
					}
				}
				break;
			}
		}
		// add de WIT kenmerk
		try {
			properties.put(QName.createQName("{http://www.defensie.nl/externalsource/model/1.0}externalApp"), "WIT");
		} catch (Exception e) {
			LOGGER.error("error adding external APP " + e.getMessage());
		}
		return properties;
	}

	/**
	 * Validate if file name is an XML by identifying the extension name.
	 * 
	 * @param fileName
	 * @return
	 */
	private Boolean isXml(String fileName) {
		String ext = getFileExtension(fileName);
		if (ext != null && ext.length() > 0 && ext.equalsIgnoreCase("xml")) {
			return true;
		}
		return false;
	}

	/**
	 * Get the file extension of a file name.
	 * 
	 * @param fullName
	 * @return String the file extension
	 */
	private static String getFileExtension(String fullName) {
		String fileName = new File(fullName).getName();
		int dotIndex = fileName.lastIndexOf('.');
		return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
	}

	/**
	 * Create new file (directory)
	 * 
	 * @param path
	 * @return File the directory
	 */
	private static File getFileOrDirectory(String path) {
		return new File(path);
	}

	protected QName getProperty(String propName) {
		return XML_MAPPING.get(propName);
	}
}