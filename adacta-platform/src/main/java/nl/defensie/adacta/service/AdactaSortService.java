package nl.defensie.adacta.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.solr.AlfrescoModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaModel;

public class AdactaSortService {

	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "SortService");

	protected Logger LOGGER = Logger.getLogger(this.getClass());
	
	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;

	public List<NodeRef> doSort(List<NodeRef> nodes, String field, String direction){
		if (null != field && field.contains("docCategory") ) {
			return sortDocCategory(nodes, direction);
		}
		if (null != field && field.contains("docSubject") ) {
			return sortDocSubject(nodes, direction);
		}
		if (null != field && field.contains("docCaseNumber") ) {
			return sortDocCaseNumber(nodes, direction);
		}
		if (null != field && field.contains("docWorkDossier") ) {
			return sortDocWorkDossier(nodes, direction);
		}
		if (null != field && field.contains("docReference") ) {
			return sortDocReference(nodes, direction);
		}
		if (null != field && field.contains("docDate") ) {
			return sortDocDate(nodes, direction);
		}
		if (null != field && field.contains("created")) {
			return sortCreated(nodes, direction);
		}
		return nodes;
	}
	
	public List<NodeRef> sortDocCategory(List<NodeRef> nodes, String direction) {
	       Collections.sort(nodes, new Comparator<NodeRef>()
	        {
	            public int compare(NodeRef n1, NodeRef n2)
	            {
	            	String seq1 = (String) nodeService.getProperty(n1, AdactaModel.PROP_DOC_CATEGORY);
	            	String seq2 = (String) nodeService.getProperty(n2, AdactaModel.PROP_DOC_CATEGORY);
	            	if (direction.equals("desc")) {
	                return seq1.compareTo(seq2);
	            }else {
	                return seq2.compareTo(seq1);	            	
	            }
	            }
	        });
		return nodes;
	}
	public List<NodeRef> sortDocSubject(List<NodeRef> nodes, String direction) {
	       Collections.sort(nodes, new Comparator<NodeRef>()
	        {
	            public int compare(NodeRef n1, NodeRef n2)
	            {
	            	String seq1 = (String) nodeService.getProperty(n1, AdactaModel.PROP_DOC_SUBJECT);
	            	String seq2 = (String) nodeService.getProperty(n2, AdactaModel.PROP_DOC_SUBJECT);
	            	if (direction.equals("desc")) {
	                return seq1.compareTo(seq2);
	            }else {
	                return seq2.compareTo(seq1);	            	
	            }
	            }
	        });
		return nodes;
	}
	public List<NodeRef> sortDocCaseNumber(List<NodeRef> nodes, String direction) {
	       Collections.sort(nodes, new Comparator<NodeRef>()
	        {
	            public int compare(NodeRef n1, NodeRef n2)
	            {
	            	String seq1 = (String) nodeService.getProperty(n1, AdactaModel.PROP_DOC_CASE_NUMBER);
	            	String seq2 = (String) nodeService.getProperty(n2, AdactaModel.PROP_DOC_CASE_NUMBER);
	            	if (direction.equals("desc")) {
	                return seq1.compareTo(seq2);
	            }else {
	                return seq2.compareTo(seq1);	            	
	            }
	            }
	        });
		return nodes;
	}
	public List<NodeRef> sortDocWorkDossier(List<NodeRef> nodes, String direction) {
	       Collections.sort(nodes, new Comparator<NodeRef>()
	        {
	            public int compare(NodeRef n1, NodeRef n2)
	            {
	            	String seq1 = (String) nodeService.getProperty(n1, AdactaModel.PROP_DOC_WORK_DOSSIER);
	            	String seq2 = (String) nodeService.getProperty(n2, AdactaModel.PROP_DOC_WORK_DOSSIER);
	            	if (direction.equals("desc")) {
	                return seq1.compareTo(seq2);
	            }else {
	                return seq2.compareTo(seq1);	            	
	            }
	            }
	        });
		return nodes;
	}
	public List<NodeRef> sortDocReference(List<NodeRef> nodes, String direction) {
	       Collections.sort(nodes, new Comparator<NodeRef>()
	        {
	            public int compare(NodeRef n1, NodeRef n2)
	            {
	            	String seq1 = (String) nodeService.getProperty(n1, AdactaModel.PROP_DOC_REFERENCE);
	            	String seq2 = (String) nodeService.getProperty(n2, AdactaModel.PROP_DOC_REFERENCE);
	            	if (direction.equals("desc")) {
	                return seq1.compareTo(seq2);
	            }else {
	                return seq2.compareTo(seq1);	            	
	            }
	            }
	        });
		return nodes;
	}

	public List<NodeRef> sortDocDate(List<NodeRef> nodes, String direction) {
		Collections.sort(nodes, new Comparator<NodeRef>() {
			public int compare(NodeRef n1, NodeRef n2) {
				Date date1 = (Date) nodeService.getProperty(n1, AdactaModel.PROP_DOC_DATE);
				Date date2 = (Date) nodeService.getProperty(n2, AdactaModel.PROP_DOC_DATE);
				long time1 = date1 == null ? Long.MAX_VALUE : date1.getTime();
				long time2 = date2 == null ? Long.MAX_VALUE : date2.getTime();

				long result = time1 - time2;

				if (direction.equals("desc")) {
					return result > 0 ? 1 : (result < 0 ? -1 : 0);
				} else {
					return result > 0 ? -1 : (result < 0 ? 1 : 0);
				}
			}
		});
		return nodes;
	}
	public List<NodeRef> sortCreated(List<NodeRef> nodes, String direction) {
		Collections.sort(nodes, new Comparator<NodeRef>() {
			public int compare(NodeRef n1, NodeRef n2) {
				Date date1 = (Date) nodeService.getProperty(n1, ContentModel.PROP_CREATED);
				Date date2 = (Date) nodeService.getProperty(n2, ContentModel.PROP_CREATED);
				long time1 = date1 == null ? Long.MAX_VALUE : date1.getTime();
				long time2 = date2 == null ? Long.MAX_VALUE : date2.getTime();

				long result = time1 - time2;

				if (direction.equals("desc")) {
					return result > 0 ? 1 : (result < 0 ? -1 : 0);
				} else {
					return result > 0 ? -1 : (result < 0 ? 1 : 0);
				}
			}
		});
		return nodes;
	}

	public List<NodeRef> selfServiceDefaultSearch(List<NodeRef> nodes) {
		Comparator<NodeRef> docCategoryComparator = new Comparator<NodeRef>() {
			public int compare(NodeRef n1, NodeRef n2) {
				String seq1 = (String) nodeService.getProperty(n1, AdactaModel.PROP_DOC_CATEGORY);
				String seq2 = (String) nodeService.getProperty(n2, AdactaModel.PROP_DOC_CATEGORY);
				return seq1.compareTo(seq2);
			}
		};
		Comparator<NodeRef> docSubjectComparator = new Comparator<NodeRef>() {
			public int compare(NodeRef n1, NodeRef n2) {
				String seq1 = (String) nodeService.getProperty(n1, AdactaModel.PROP_DOC_SUBJECT);
				String seq2 = (String) nodeService.getProperty(n2, AdactaModel.PROP_DOC_SUBJECT);
				return seq1.compareTo(seq2);
			}
		};
		Comparator<NodeRef> docDateComparator = new Comparator<NodeRef>() {
			public int compare(NodeRef n1, NodeRef n2) {
				Date date1 = (Date) nodeService.getProperty(n1, AdactaModel.PROP_DOC_DATE);
				Date date2 = (Date) nodeService.getProperty(n2, AdactaModel.PROP_DOC_DATE);
				long time1 = date1 == null ? Long.MAX_VALUE : date1.getTime();
				long time2 = date2 == null ? Long.MAX_VALUE : date2.getTime();
				long result = time1 - time2;
				return result > 0 ? 1 : (result < 0 ? -1 : 0);
			}
		};
		List<Comparator<NodeRef>> comparators = new ArrayList<Comparator<NodeRef>>();
		comparators.add(docCategoryComparator);
		comparators.add(docSubjectComparator);
		comparators.add(docDateComparator);
		Collections.sort(nodes, ComparatorUtils.chainedComparator(comparators));
		return nodes;

	}
	public List<NodeRef> raadplegenDefaultSearch(List<NodeRef> nodes) {
		Comparator<NodeRef> docCategoryComparator = new Comparator<NodeRef>() {
			public int compare(NodeRef n1, NodeRef n2) {
				String seq1 = (String) nodeService.getProperty(n1, AdactaModel.PROP_DOC_CATEGORY);
				String seq2 = (String) nodeService.getProperty(n2, AdactaModel.PROP_DOC_CATEGORY);
				return seq1.compareTo(seq2);
			}
		};
		Comparator<NodeRef> docSubjectComparator = new Comparator<NodeRef>() {
			public int compare(NodeRef n1, NodeRef n2) {
				String seq1 = (String) nodeService.getProperty(n1, AdactaModel.PROP_DOC_SUBJECT);
				String seq2 = (String) nodeService.getProperty(n2, AdactaModel.PROP_DOC_SUBJECT);
				return seq1.compareTo(seq2);
			}
		};
		Comparator<NodeRef> docDateComparator = new Comparator<NodeRef>() {
			public int compare(NodeRef n1, NodeRef n2) {
				Date date1 = (Date) nodeService.getProperty(n1, ContentModel.PROP_CREATED);
				Date date2 = (Date) nodeService.getProperty(n2, ContentModel.PROP_CREATED);
				long time1 = date1 == null ? Long.MAX_VALUE : date1.getTime();
				long time2 = date2 == null ? Long.MAX_VALUE : date2.getTime();
				long result = time1 - time2;
				return result > 0 ? 1 : (result < 0 ? -1 : 0);
			}
		};
		List<Comparator<NodeRef>> comparators = new ArrayList<Comparator<NodeRef>>();
		comparators.add(docDateComparator);
		comparators.add(docCategoryComparator);
		comparators.add(docSubjectComparator);
		Collections.sort(nodes, ComparatorUtils.chainedComparator(comparators));
		return nodes;

	}
}
