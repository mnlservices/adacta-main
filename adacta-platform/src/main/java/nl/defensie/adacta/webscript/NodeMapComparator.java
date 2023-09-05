package nl.defensie.adacta.webscript;

import java.util.Comparator;
import java.util.Date;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.springframework.beans.factory.annotation.Autowired;

import nl.defensie.adacta.model.AdactaModel;

/**
 * A comparison function, which imposes a total ordering on some collection of nodes. Use by  {@link ImportFolderListGet}.
 * 
 * @author Rick de Rooij
 *
 */
public class NodeMapComparator implements Comparator<Map<String, Object>> {

	public NodeNameAscComparator nodeNameAscComparator = new NodeNameAscComparator();
	public NodeTitleAscComparator nodeTitleAscComparator = new NodeTitleAscComparator();
	public NodeCreatedAscComparator nodeCreatedAscComparator = new NodeCreatedAscComparator();
	public NodeModifiedAscComparator nodeModifiedAscComparator = new NodeModifiedAscComparator();

	public NodeDocDateAscComparator nodeDocDateAscComparator = new NodeDocDateAscComparator();
	public NodeDocCategoryAscComparator nodeDocCategoryAscComparator = new NodeDocCategoryAscComparator();
	public NodeDocSubjectAscComparator nodeDocSubjectAscComparator = new NodeDocSubjectAscComparator();

	@Autowired
	protected NodeService nodeService;

	public class NodeCreatedAscComparator implements Comparator<Map<String, Object>> {

		public int compare(Map<String, Object> o1, Map<String, Object> o2) {

			Date date1 = (Date) nodeService.getProperty(new NodeRef((String) o1.get("nodeRef")), ContentModel.PROP_CREATED);
			Date date2 = (Date) nodeService.getProperty(new NodeRef((String) o2.get("nodeRef")), ContentModel.PROP_CREATED);

			long time1 = date1 == null ? Long.MAX_VALUE : date1.getTime();
			long time2 = date2 == null ? Long.MAX_VALUE : date2.getTime();

			long result = time1 - time2;

			return result > 0 ? 1 : (result < 0 ? -1 : 0);
		}
	}

	public class NodeModifiedAscComparator implements Comparator<Map<String, Object>> {

		public int compare(Map<String, Object> o1, Map<String, Object> o2) {

			Date date1 = (Date) nodeService.getProperty(new NodeRef((String) o1.get("nodeRef")), ContentModel.PROP_MODIFIED);
			Date date2 = (Date) nodeService.getProperty(new NodeRef((String) o2.get("nodeRef")), ContentModel.PROP_MODIFIED);

			long time1 = date1 == null ? Long.MAX_VALUE : date1.getTime();
			long time2 = date2 == null ? Long.MAX_VALUE : date2.getTime();

			long result = time1 - time2;

			return result > 0 ? 1 : (result < 0 ? -1 : 0);
		}
	}

	public class NodeNameAscComparator implements Comparator<Map<String, Object>> {

		public int compare(Map<String, Object> o1, Map<String, Object> o2) {

			new NodeRef((String) o1.get("nodeRef"));

			String str1 = (String) nodeService.getProperty(new NodeRef((String) o1.get("nodeRef")), ContentModel.PROP_NAME);
			String str2 = (String) nodeService.getProperty(new NodeRef((String) o2.get("nodeRef")), ContentModel.PROP_NAME);

			if (str1 == null && str2 == null)
				return 0;
			if (str1 == null)
				return -1;
			if (str2 == null)
				return 1;
			return str1.compareTo(str2);
		}
	}

	public class NodeTitleAscComparator implements Comparator<Map<String, Object>> {

		public int compare(Map<String, Object> o1, Map<String, Object> o2) {

			String str1 = (String) nodeService.getProperty(new NodeRef((String) o1.get("nodeRef")), ContentModel.PROP_TITLE);
			String str2 = (String) nodeService.getProperty(new NodeRef((String) o2.get("nodeRef")), ContentModel.PROP_TITLE);

			if (str1 == null && str2 == null)
				return 0;
			if (str1 == null)
				return -1;
			if (str2 == null)
				return 1;
			return str1.compareTo(str2);
		}
	}

	public class NodeDocDateAscComparator implements Comparator<Map<String, Object>> {

		public int compare(Map<String, Object> o1, Map<String, Object> o2) {

			Date date1 = (Date) nodeService.getProperty(new NodeRef((String) o1.get("nodeRef")), AdactaModel.PROP_DOC_DATE);
			Date date2 = (Date) nodeService.getProperty(new NodeRef((String) o2.get("nodeRef")), AdactaModel.PROP_DOC_DATE);

			long time1 = date1 == null ? Long.MAX_VALUE : date1.getTime();
			long time2 = date2 == null ? Long.MAX_VALUE : date2.getTime();

			long result = time1 - time2;

			return result > 0 ? 1 : (result < 0 ? -1 : 0);
		}
	}

	public class NodeDocCategoryAscComparator implements Comparator<Map<String, Object>> {

		public int compare(Map<String, Object> o1, Map<String, Object> o2) {

			new NodeRef((String) o1.get("nodeRef"));

			String str1 = (String) nodeService.getProperty(new NodeRef((String) o1.get("nodeRef")), AdactaModel.PROP_DOC_CATEGORY);
			String str2 = (String) nodeService.getProperty(new NodeRef((String) o2.get("nodeRef")), AdactaModel.PROP_DOC_CATEGORY);

			if (str1 == null && str2 == null)
				return 0;
			if (str1 == null)
				return -1;
			if (str2 == null)
				return 1;
			return str1.compareTo(str2);
		}
	}

	public class NodeDocSubjectAscComparator implements Comparator<Map<String, Object>> {

		public int compare(Map<String, Object> o1, Map<String, Object> o2) {

			new NodeRef((String) o1.get("nodeRef"));

			String str1 = (String) nodeService.getProperty(new NodeRef((String) o1.get("nodeRef")), AdactaModel.PROP_DOC_SUBJECT);
			String str2 = (String) nodeService.getProperty(new NodeRef((String) o2.get("nodeRef")), AdactaModel.PROP_DOC_SUBJECT);

			if (str1 == null && str2 == null)
				return 0;
			if (str1 == null)
				return -1;
			if (str2 == null)
				return 1;
			return str1.compareTo(str2);
		}
	}

	@Override
	public int compare(Map<String, Object> o1, Map<String, Object> o2) {
		return 0;
	}
}