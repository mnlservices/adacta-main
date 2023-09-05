package nl.defensie.adacta.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.template.TemplateNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import nl.defensie.adacta.model.AdactaDatalistModel;
import nl.defensie.adacta.model.AdactaModel;

public class AdactaDataListService {
	
	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;
	@Autowired
	protected AdactaSiteService adactaSiteService;
	
	/**
	 * Get a list of all items in the given dataList, permission checked.
	 * @return
	 */
	public LinkedHashMap<String, KVDataListItem> getKVDataListItems(final String dataListLocalName) {
		final NodeRef dataList = adactaSiteService.getDataListRoot(AdactaModel.PREFIX, dataListLocalName);
		final List<ChildAssociationRef> dataListItems = nodeService.getChildAssocs(dataList, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL, true);
		final LinkedHashMap<String, KVDataListItem> items = new LinkedHashMap<String, KVDataListItem>();
		for (final ChildAssociationRef ref : dataListItems) {
			final NodeRef nodeRef = ref.getChildRef();
			final String key = (String) nodeService.getProperty(nodeRef, AdactaDatalistModel.PROP_VALUE);
			final String value = (String) nodeService.getProperty(nodeRef, AdactaDatalistModel.PROP_DESC);
			items.put(key, new KVDataListItem(nodeRef, key, value));
		}
		
		return items;
	}
	
	public static List<TemplateNode> toTemplates(final Collection<KVDataListItem> kvDataListItems, final ServiceRegistry serviceRegistry) {
		final List<TemplateNode> templateNodes = new ArrayList<TemplateNode>(kvDataListItems.size());
		for (final KVDataListItem item : kvDataListItems) {
			templateNodes.add(new TemplateNode(item.getNodeRef(), serviceRegistry, null));
		}
		return templateNodes;
	}
	
	public class KVDataListItem {
		private NodeRef nodeRef;
		private String key;
		private String value;
		
		public KVDataListItem(final NodeRef nodeRef, final String key, final String value) {
			this.nodeRef = nodeRef;
			this.key = key;
			this.value = value;
		}
		
		public NodeRef getNodeRef() {
			return this.nodeRef;
		}
		
		public String getKey() {
			return this.key;
		}
		
		String getValue() {
			return this.value;
		}
	}
}
