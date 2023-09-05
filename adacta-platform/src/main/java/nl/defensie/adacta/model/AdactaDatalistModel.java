package nl.defensie.adacta.model;

import org.alfresco.service.namespace.QName;

/**
 * The mapping of the main Adacta datalist model.
 * 
 * @author Rick de Rooij
 */
public interface AdactaDatalistModel {

	// The model URI
	String URI = "http://www.defensie.nl/adacta/datalist/1.0";

	// Types
	QName TYPE_CATEGORY_ITEM = QName.createQName(URI, "categoryItem");
	QName TYPE_SUBJECT_ITEM = QName.createQName(URI, "subjectItem");
	
	QName PROP_VALUE = QName.createQName(URI, "value");
	QName PROP_DESC = QName.createQName(URI, "description");
}