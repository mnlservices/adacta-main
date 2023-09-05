package nl.defensie.adacta.archive;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.JdbcTemplate;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.DateUtils;
import nl.defensie.adacta.utils.SyncLogger;

public class AdactaArchiveDatabaseService implements ApplicationContextAware {

	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "ArchiveDatabaseService");

	private ApplicationContext applicationContext;
	private int dbNamespaceId;
	private int dbProtocolId;
	private int dbDocumentId;
	private int dbDocCategoryId;
	private int dbDocSubjectId;

	// located in alfresco database
	protected String ADACTA_SYNC_DATASOURCE = "alfresco-sync";

	@Value("${adacta.cron.job.adactaArchive.startdate}")
	protected String STARTDATUM_ARCHIVERING;
	
	public String getAlfrescoSyncDataSourceName() {
		return ADACTA_SYNC_DATASOURCE;
	}

	public void initialNameSpaceQueries() {
		SyncLogger.info("executing base queries");
		getDbProtocolId();
		SyncLogger.info("protocolId: " + this.dbProtocolId);
		getDbNamespaceId();
		SyncLogger.info("nameSpaceId: " + this.dbNamespaceId);
		getDbDocumentId();
		SyncLogger.info("documentId: " + this.dbDocumentId);
		getDbDocCategoryId();
		SyncLogger.info("docCategoryId: " + this.dbDocCategoryId);
		getDbDocSubjectId();
		SyncLogger.info("docSubjectId: " + this.dbDocSubjectId);
	}

	/**
	 * Get the data source from bean definition.
	 * 
	 * @param dataSourceName String the bean id
	 * @return JdbcTemplate JDBC connection
	 */
	private JdbcTemplate getDataSource(String dataSourceName) {
		Object dsBean = applicationContext.getBean(dataSourceName);
		if (dsBean instanceof DataSource) {
			JdbcTemplate ds = new JdbcTemplate();
			ds.setDataSource((DataSource) dsBean);
			return ds;
		} else {
			SyncLogger.info("datasource not found");
			throw new AlfrescoRuntimeException(String.format("Datasource '%s' not found.", dataSourceName));
		}
	}

	private void getDbProtocolId() {
		String sql = "select ASTORE.ID from alf_store ASTORE where ASTORE.protocol = 'workspace' and ASTORE.identifier='SpacesStore'";
		this.dbProtocolId = getDataSource(ADACTA_SYNC_DATASOURCE).queryForObject(sql, Integer.class);
	}

	private void getDbNamespaceId() {
		String sql = "select NS.ID from ALF_NAMESPACE NS where NS.URI like '%adacta/model%'";
		this.dbNamespaceId = getDataSource(ADACTA_SYNC_DATASOURCE).queryForObject(sql, Integer.class);
	}

	private void getDbDocumentId() {
		String sql = "select AQ.id from alf_Qname AQ where AQ.ns_id=" + this.dbNamespaceId + " and AQ.local_name like 'document'";
		this.dbDocumentId = getDataSource(ADACTA_SYNC_DATASOURCE).queryForObject(sql, Integer.class);
	}

	private void getDbDocCategoryId() {
		String sql = "select AQ.ID from alf_Qname AQ where AQ.NS_ID=" + this.dbNamespaceId + " and AQ.LOCAL_NAME like 'docCategory'";
		this.dbDocCategoryId = getDataSource(ADACTA_SYNC_DATASOURCE).queryForObject(sql, Integer.class);
	}

	private void getDbDocSubjectId() {
		String sql = "select AQ.ID from alf_Qname AQ where AQ.ns_id=" + this.dbNamespaceId + " and AQ.local_name like 'docSubject'";
		this.dbDocSubjectId = getDataSource(ADACTA_SYNC_DATASOURCE).queryForObject(sql, Integer.class);
	}

	/**
	 * Returns a list of UUID's of document nodes.
	 * @param onderwerpcode
	 * @param bewaartermijnInJaren
	 * @return
	 */
	public List<String> getQueryResults(String onderwerpcode, int bewaartermijnInJaren){
		String sql = getQueryString(onderwerpcode, bewaartermijnInJaren);
		if (null == sql) {
			return new ArrayList<String>();
		}
		SyncLogger.info("generated query: "+sql);
		List<String> results = getDataSource(ADACTA_SYNC_DATASOURCE).queryForList(sql, String.class);
		return results;
	}
	/**
	 * Gebruikt de daterange tussen startdatum (2023-01-01 en "Nu" minus
	 * bewaartermijnInJaren)
	 * 
	 * @param onderwerpCode
	 * @param bewaartermijnInJaren
	 * @return
	 */
	public String getQueryString(String onderwerpCode, int bewaartermijnInJaren) {
		String endDate = DateUtils.getDateMinusYears(bewaartermijnInJaren);
		SyncLogger.info("endDate=" + endDate);
		// endDate before startDate -> abort
		if (!DateUtils.isBefore(STARTDATUM_ARCHIVERING, endDate)) {
			SyncLogger.info("nothing to archive for subject " + onderwerpCode);
			return null;
		}else {
			SyncLogger.info("Archive date reached for " + onderwerpCode);			
		}
		String rubriekCode = onderwerpCode.substring(0, 2);
		StringWriter sw = new StringWriter();
		sw.append("select NODE.UUID from ALF_NODE NODE ");
		sw.append(" LEFT JOIN ALF_NODE_PROPERTIES  NLD ON (NODE.ID = NLD.NODE_ID AND NLD.QNAME_ID = "
				+ this.dbDocCategoryId);
		sw.append(") ");
		sw.append(" LEFT JOIN ALF_NODE_PROPERTIES  SUB ON (NODE.ID = SUB.NODE_ID AND SUB.QNAME_ID = "
				+ this.dbDocSubjectId);
		sw.append(") ");
		sw.append(" Where NODE.TYPE_QNAME_ID = " + this.dbDocumentId);
		sw.append(" AND NLD.STRING_VALUE  = ");
		sw.append("'");
		sw.append(rubriekCode);
		sw.append("' ");
		sw.append(" AND SUB.STRING_VALUE = ");
		sw.append("'");
		sw.append(onderwerpCode);
		sw.append("' ");
		sw.append(" AND NODE.STORE_ID = " + this.dbProtocolId);
		sw.append(" AND SUBSTR(NODE.AUDIT_CREATED,0, 10) >= ");
		sw.append("'");
		sw.append(STARTDATUM_ARCHIVERING);
		sw.append("' ");
		sw.append(" AND SUBSTR(NODE.AUDIT_CREATED,0, 10) < ");
		sw.append("'");
		sw.append(endDate);
		sw.append("'");

		return sw.toString();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
