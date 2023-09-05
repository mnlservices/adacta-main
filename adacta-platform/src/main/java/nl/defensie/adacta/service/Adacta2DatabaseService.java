package nl.defensie.adacta.service;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import nl.defensie.adacta.model.AdactaModel;
import nl.defensie.adacta.utils.AdactaDossier;
import nl.defensie.adacta.utils.CdmRoleSynAllBean;
import nl.defensie.adacta.utils.CdmRoleSyncBean;
import nl.defensie.adacta.utils.RoleAuthoritiesBean;
import nl.defensie.adacta.utils.SyncLogger;

/**
 * 
 * 
 * @author Wim Schreurs
 *
 */
public class Adacta2DatabaseService implements ApplicationContextAware {

	private static final String SPACE = " ";

	private ApplicationContext applicationContext;

	QName NAME = QName.createQName(NamespaceService.ALFRESCO_URI, AdactaModel.PREFIX + "2DatabaseService");

	protected Logger LOGGER = Logger.getLogger(this.getClass());

	@Value("${db.username}")
	protected String AlfrescoTablespaceName;


	protected final String INSERT_INTO_ADACTA_QRY_PREFIX="INSERT ALL";
	protected final String INSERT_INTO_ADACTA_QRY_BODY="INTO ADACTA_SYNC  (emplbsn,emplid,emplmrn,emplname,empldep,dpcodes,rowsecclass) VALUES ";
	protected final String INSERT_INTO_ADACTA_QRY_POSTFIX="SELECT 1 FROM DUAL";
	protected final String NEW_DOSSIERS_QRY="SELECT A.emplbsn, A.emplid, A.emplmrn, A.emplname, A.empldep, A.dpcodes from Adacta_Sync A left join Adacta_sync_save B on A.emplbsn  = B.emplbsn where B.emplbsn IS NULL";
	
	protected final String ADACTA_SYNC_TABLE="ADACTA_SYNC";

	protected final String ADACTA_SYNC_SAVE_TABLE="ADACTA_SYNC_SAVE";

	protected List<AdactaDossier> newDossiers;

	// located in alfresco database
	protected String ADACTA_SYNC_DATASOURCE = "alfresco-sync";
	// located in CDM database
	protected String CDM_SYNC_DATASOURCE = "cdm-sync";

	public String getAlfrescoSyncDataSourceName() {
		return ADACTA_SYNC_DATASOURCE;
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
			throw new AlfrescoRuntimeException(String.format("Datasource '%s' not found.", dataSourceName));
		}
	}

	/**
	 * Execute SQL query on provided data source.
	 * 
	 * @param dataSourceName String the bean id of data source
	 * @param sql            String the SQL statement
	 * @param params         Object additional parameters for query
	 * @return Map<String, Object>[] map of table values.
	 */
	public Map<String, Object>[] query(String dataSourceName, String sql, Object... params) {
		List<Map<String, Object>> result = getDataSource(dataSourceName).queryForList(sql, params);
		@SuppressWarnings("unchecked")
		Map<String, Object>[] arr = new Map[result.size()];
		for (int i = 0; i < result.size(); i++) {
			arr[i] = result.get(i);
		}
		return arr;
	}

	/**
	 * Execute static SQL statement. Purpose is to create tables.
	 * 
	 * @param dataSourceName String the bean id of data source
	 * @param sql            String SQL statement
	 */
	public void execute(String sql) {
		getDataSource(ADACTA_SYNC_DATASOURCE).execute(sql);
	}

	/**
	 * copies data from the CDM Sync table into the Alfresco Sync table
	 */
	public void copyCDMData() {
		// read data from CDM source
		Object[] params = new Object[0];
		StringWriter sw1 = new StringWriter();
		sw1.append("SELECT SYSADM.PS_MVD_INT_ADACTA.MVD_NAT_ID_SOFI, ");
		sw1.append("SYSADM.PS_MVD_INT_ADACTA.MVD_EMPLID_100, ");
		sw1.append("SYSADM.PS_MVD_INT_ADACTA.MVD_NAT_ID_MRN, ");
		sw1.append("SYSADM.PS_MVD_INT_ADACTA.NAME, ");
		sw1.append("SYSADM.PS_MVD_INT_ADACTA.LABOR_AGREEMENT, ");
		sw1.append("SYSADM.PS_MVD_INT_ADACTA.ROWSECCLASS, ");
		sw1.append("SYSADM.PS_MVD_INT_ADACTA.DESCRLONG ");
		sw1.append("FROM SYSADM.PS_MVD_INT_ADACTA");
		final String READ_FROM_CDM_QRY = sw1.toString();
		
		List<Map<String, Object>> result = getDataSource(CDM_SYNC_DATASOURCE).queryForList(READ_FROM_CDM_QRY, params);
		if (result.size() == 0) {
			throw new RuntimeException("error copying data from CDM sync table to Alfresco, aborting sync process ");
		}

		// insert into ADACTA Target
		int counter = 0;
		StringWriter sw = new StringWriter();
		for (Map<String, Object> p : result) {
			sw.append(getValuesFromRow(p));
			if (counter < 1000) {
				counter++;
			} else {
				getDataSource(ADACTA_SYNC_DATASOURCE)
						.execute(INSERT_INTO_ADACTA_QRY_PREFIX + sw.toString() + INSERT_INTO_ADACTA_QRY_POSTFIX);
				sw = new StringWriter();
				counter = 0;
			}
		}
		if (sw != null && sw.toString().length() > 0) {
			getDataSource(ADACTA_SYNC_DATASOURCE)
					.execute(INSERT_INTO_ADACTA_QRY_PREFIX + sw.toString() + INSERT_INTO_ADACTA_QRY_POSTFIX);
		}
	}

	private String getValuesFromRow(Map<String, Object> p) {
		StringWriter sw = new StringWriter();
		sw.append(SPACE);
		sw.append(INSERT_INTO_ADACTA_QRY_BODY);
		sw.append("(");
		sw.append("'");
		sw.append((String) p.get("mvd_nat_id_sofi"));
		sw.append("','");
		sw.append((String) p.get("mvd_emplid_100"));
		sw.append("','");
		sw.append((String) p.get("mvd_nat_id_mrn"));
		sw.append("','");
		String name = (String) p.get("name");
		if (name.contains("'")) {
			name = name.replaceAll("'", "''");
		}
		sw.append(name);
		sw.append("','");
		sw.append((String) p.get("labor_agreement"));
		sw.append("','");
		sw.append((String) p.get("descrlong"));
		sw.append("','");
		sw.append((String) p.get("rowsecclass"));
		sw.append("'");
		sw.append(") ");
		return sw.toString();
	}

	/**
	 * Get the dossiers that must be created. Abort if one of the synchronization
	 * tables is empty.
	 * 
	 * @return
	 */
	public List<AdactaDossier> getNewDossiers() {
		List<AdactaDossier> dlist = new ArrayList<>();
		boolean check1 = checkTableNotEmpty(ADACTA_SYNC_TABLE);
		boolean check2 = checkTableNotEmpty(ADACTA_SYNC_SAVE_TABLE);
		if (check1 && check2) {
			dlist = getDossiers(NEW_DOSSIERS_QRY);
			newDossiers = dlist;
		} else {
			throw new AlfrescoRuntimeException(
					"one or both of the alfresco synctables are empty! aborting synchronization ");
		}
		return dlist;
	}

	private boolean checkTableNotEmpty(String table) {
		String sql = "select count(*) from " + table;
		Integer aantal = getDataSource(ADACTA_SYNC_DATASOURCE).queryForObject(sql, Integer.class);
		if (null != aantal && aantal > 0) {
			return true;
		} else {
			return false;
		}
	}

	public List<AdactaDossier> getChangedDossiers() {
		final String CHANGED_DOSSIERS_QRY = getChangedDossiersQry();
		return getDossiers(CHANGED_DOSSIERS_QRY);
	}

	private String getChangedDossiersQry() {
		return "SELECT A.emplbsn, A.emplid, A.emplmrn, A.emplname, A.empldep, A.dpcodes FROM ADACTA_SYNC  A left join ADACTA_SYNC_SAVE  B on a.emplbsn = b.emplbsn where"
				+ " (a.emplid <> b.emplid or a.emplmrn <> b.emplmrn or a.emplname <> b.emplname or a.empldep <> b.empldep or a.dpcodes <> b.dpcodes)";
	}

	public Map<String, String> getChangedRowSecClass() {
		Map<String, String> m = new HashMap<>();
		m.putAll(addNewDossiersWithRowsecclass());
		final String CHANGED_ROWSECCLASS_QRY = getRowSecClassQry();
		List<AdactaDossier> dossiers = getDossiers(CHANGED_ROWSECCLASS_QRY);
		for (AdactaDossier ad : dossiers) {
			if (StringUtils.isNotEmpty(ad.getEmplid())) {
				String[] e = ad.getEmplid().split(SPACE);
				for (int i = 0; i < e.length; i++) {
					// ignore double entries for the moment
					if (!m.containsKey(e[i])) {
						m.put(e[i], ad.getRowsecclass());
					}
				}
			}
		}
		return m;
	}

	private String getRowSecClassQry() {
		return "SELECT A.emplbsn, A.emplid, A.emplmrn, A.emplname, A.empldep, A.dpcodes, A.rowsecclass FROM ADACTA_SYNC  A "
				+ "left join ADACTA_SYNC_SAVE  B on a.emplbsn = b.emplbsn where a.rowsecclass <> b.rowsecclass";
	}

	/**
	 * A newly created dossier can have a rowsecclass. In this case We must also
	 * update the userprofile.
	 * 
	 * @return a map with key emplid and value rowsecclass.
	 */
	private Map<String, String> addNewDossiersWithRowsecclass() {
		Map<String, String> m = new HashMap<>();
		for (AdactaDossier ad : newDossiers) {
			if (StringUtils.isNotEmpty(ad.getRowsecclass()) && StringUtils.isNotEmpty(ad.getEmplid())) {
				String[] e = ad.getEmplid().split(SPACE);
				for (int i = 0; i < e.length; i++) {
					m.put(e[i], ad.getRowsecclass());
				}
			}
		}
		return m;
	}

	public List<AdactaDossier> getDossiers(String sql) {
		List<AdactaDossier> dossiers = getDataSource(ADACTA_SYNC_DATASOURCE).query(sql, BeanPropertyRowMapper.newInstance(AdactaDossier.class));
		return dossiers;
	}

	public List<CdmRoleSyncBean> getCdmRoles(RoleAuthoritiesBean roleAuthoritiesBean) {
		String sql = getRoleSyncQuery(roleAuthoritiesBean);
		SyncLogger.info(sql);
		List<CdmRoleSyncBean> beans = getDataSource(CDM_SYNC_DATASOURCE).query(sql, BeanPropertyRowMapper.newInstance(CdmRoleSyncBean.class));
		return beans;
	}

	public List<String> getAllCdmRoles(RoleAuthoritiesBean roleAuthoritiesBean) {
		String sql = getRoleSyncQueryAll(roleAuthoritiesBean);
		SyncLogger.info(sql);
		List<CdmRoleSynAllBean> roles = getDataSource(CDM_SYNC_DATASOURCE).query(sql, BeanPropertyRowMapper.newInstance(CdmRoleSynAllBean.class));
		List<String> rs = new ArrayList<>();
		for (CdmRoleSynAllBean b : roles) {
			rs.add(b.getRole());
		}
		return rs;
	}

	/**
	 * Gets a query to retrieve peoplesoft authorizations from CDM. Reads a javabean
	 * (that represents a property file) to include all relevant peoplesoft
	 * rolenames.
	 * 
	 * @param roleAuthoritiesBean (reflects propertyfile
	 *                            peoplesoft-roles-authorities.properties)
	 * @return
	 */
	protected String getRoleSyncQuery(RoleAuthoritiesBean roleAuthoritiesBean) {
		Map<String, List<String>> ra = roleAuthoritiesBean.getRoleAuthorities();
		Set<String> keys = ra.keySet();
		SyncLogger.info("size of keys: " + keys.size());
		StringWriter sw = new StringWriter();
		sw.append("SELECT DISTINCT 'R' || sysadm.PS_MVD_ARBEIDSPLTS.Position_nbr AS role, ");
		sw.append("sysadm.PS_MVD_AP_ROLLEN.ROLENAME as authority, ");
		sw.append("sysadm.PS_MVD_PROFIELEN.EMPLID as emplid ");
		sw.append("FROM sysadm.PS_MVD_ARBEIDSPLTS ");
		sw.append(
				"INNER JOIN sysadm.PS_MVD_AP_ROLLEN ON sysadm.PS_MVD_ARBEIDSPLTS.MVD_ARBEIDSPLTS_SK = sysadm.PS_MVD_AP_ROLLEN.MVD_ARBEIDSPLTS_SK ");
		sw.append(
				"INNER JOIN sysadm.PS_MVD_PROFIELEN ON sysadm.PS_MVD_ARBEIDSPLTS.POSITION_NBR = sysadm.PS_MVD_PROFIELEN.POSITION_NBR ");
		sw.append(" WHERE TRIM(sysadm.PS_MVD_AP_ROLLEN.ROLENAME)  IS NOT NULL ");
		boolean isFirst = true;
		for (String key : keys) {
			if (isFirst) {
				sw.append(" AND (UPPER(sysadm.PS_MVD_AP_ROLLEN.ROLENAME) = '" + key + "'");
				isFirst = false;
			} else {
				sw.append(" OR UPPER(sysadm.PS_MVD_AP_ROLLEN.ROLENAME) = '" + key + "'");
			}
		}
		sw.append(" )  AND (((sysadm.PS_MVD_PROFIELEN.EFFDT_FROM)<CURRENT_DATE+14)");
		sw.append(" AND ((sysadm.PS_MVD_PROFIELEN.EFFDT_TO)>CURRENT_DATE-60 ");
		sw.append(" OR (sysadm.PS_MVD_PROFIELEN.EFFDT_TO) Is Null) ");
		sw.append(" AND ((sysadm.PS_MVD_ARBEIDSPLTS.EFF_STATUS='A') ");
		sw.append(" AND sysadm.PS_MVD_ARBEIDSPLTS.MVD_CURRENT='Y'))");

		return sw.toString();
	}

	protected String getRoleSyncQueryAll(RoleAuthoritiesBean roleAuthoritiesBean) {
		Map<String, List<String>> ra = roleAuthoritiesBean.getRoleAuthorities();
		Set<String> keys = ra.keySet();
		SyncLogger.info("size of keys: " + keys.size());
		StringWriter sw = new StringWriter();
		sw.append("SELECT DISTINCT 'R' || sysadm.PS_MVD_ARBEIDSPLTS.Position_nbr AS role ");
		sw.append("FROM sysadm.PS_MVD_ARBEIDSPLTS ");
		sw.append(
				"INNER JOIN sysadm.PS_MVD_AP_ROLLEN ON sysadm.PS_MVD_ARBEIDSPLTS.MVD_ARBEIDSPLTS_SK = sysadm.PS_MVD_AP_ROLLEN.MVD_ARBEIDSPLTS_SK ");
		sw.append(
				"INNER JOIN sysadm.PS_MVD_PROFIELEN ON sysadm.PS_MVD_ARBEIDSPLTS.POSITION_NBR = sysadm.PS_MVD_PROFIELEN.POSITION_NBR ");
		sw.append(" WHERE TRIM(sysadm.PS_MVD_AP_ROLLEN.ROLENAME)  IS NOT NULL ");
		boolean isFirst = true;
		for (String key : keys) {
			if (isFirst) {
				sw.append(" AND (UPPER(sysadm.PS_MVD_AP_ROLLEN.ROLENAME) = '" + key + "'");
				isFirst = false;
			} else {
				sw.append(" OR UPPER(sysadm.PS_MVD_AP_ROLLEN.ROLENAME) = '" + key + "'");
			}
		}
		sw.append(" )  ");

		return sw.toString();
	}

	public void truncateAlfrescoSyncTable() {
		String sql = "TRUNCATE TABLE " + ADACTA_SYNC_TABLE;
		getDataSource(ADACTA_SYNC_DATASOURCE).execute(sql);
	}

	public void truncateAlfrescoSyncSaveTable() {
		String sql = "TRUNCATE TABLE " + ADACTA_SYNC_SAVE_TABLE;
		getDataSource(ADACTA_SYNC_DATASOURCE).execute(sql);
	}

	public void copyAlfrescoSyncTable() {
		final String COPY_TO_SAVE_QRY = getCopyToSaveQry();
		getDataSource(ADACTA_SYNC_DATASOURCE).execute(COPY_TO_SAVE_QRY);
	}

	private String getCopyToSaveQry() {
		return "INSERT INTO ADACTA_SYNC_SAVE( emplbsn, emplid, emplmrn, emplname,empldep, dpcodes, rowsecclass ) "
				+ " SELECT emplbsn, emplid, emplmrn, emplname,empldep, dpcodes, rowsecclass FROM ADACTA_SYNC";
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}