package nl.defensie.adacta.utils;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;

/**
 * POJO for join queries for all tables ADACTA_DOSSIERS, DIS_P8_GBR and DIS_P8_MDW.
 * 
 * @author Rick de Rooij
 *
 */
public class DISP8Owner {

	private String STRBSN;
	private String STREMPID;
	private String STRMRN;
	private String STRNAAM;
	private Date DATGEBOORTE;
	private Date DATUITDIENST;
	private String STRSETID;
	private String STRAFDELING;
	private String DP_CODE;
	private String DP_CODES;

	public DISP8Owner(Map<String, Object> map) {
		this.STRBSN = (String) map.get("STRBSN");
		this.STREMPID = (String) map.get("STREMPID");
		this.STRMRN = (String) map.get("STRMRN");
		this.STRNAAM = (String) map.get("STRNAAM");
		if (map.get("DATGEBOORTE") instanceof Date) {
			this.DATGEBOORTE = (Date) map.get("DATGEBOORTE");
		} else if (map.get("DATGEBOORTE") instanceof Timestamp) {
			Timestamp stamp = (Timestamp) map.get("DATGEBOORTE");
			this.DATGEBOORTE = new Date(stamp.getTime());
		}
		if (map.get("DATUITDIENST") instanceof Date) {
			this.DATUITDIENST = (Date) map.get("DATUITDIENST");
		} else if (map.get("DATUITDIENST") instanceof Timestamp) {
			Timestamp stamp = (Timestamp) map.get("DATUITDIENST");
			this.DATUITDIENST = new Date(stamp.getTime());
		}
		this.STRSETID = (String) map.get("STRSETID");
		this.STRAFDELING = (String) map.get("STRAFDELING");
		this.DP_CODE = (String) map.get("DP_CODE");
		this.DP_CODES = (String) map.get("DP_CODES");
	}

	public String getSTRBSN() {
		return STRBSN;
	}

	public void setSTRBSN(String sTRBSN) {
		STRBSN = sTRBSN;
	}

	public String getSTREMPID() {
		return STREMPID;
	}

	public void setSTREMPID(String sTREMPID) {
		STREMPID = sTREMPID;
	}

	public String getSTRMRN() {
		return STRMRN;
	}

	public void setSTRMRN(String sTRMRN) {
		STRMRN = sTRMRN;
	}

	public String getSTRNAAM() {
		return STRNAAM;
	}

	public void setSTRNAAM(String sTRNAAM) {
		STRNAAM = sTRNAAM;
	}

	public Date getDATGEBOORTE() {
		return DATGEBOORTE;
	}

	public void setDATGEBOORTE(Date dATGEBOORTE) {
		DATGEBOORTE = dATGEBOORTE;
	}

	public Date getDATUITDIENST() {
		return DATUITDIENST;
	}

	public void setDATUITDIENST(Date dATUITDIENST) {
		DATUITDIENST = dATUITDIENST;
	}

	public String getSTRSETID() {
		return STRSETID;
	}

	public void setSTRSETID(String sTRSETID) {
		STRSETID = sTRSETID;
	}

	public String getSTRAFDELING() {
		return STRAFDELING;
	}

	public void setSTRAFDELING(String sTRAFDELING) {
		STRAFDELING = sTRAFDELING;
	}

	public String getDP_CODE() {
		return DP_CODE;
	}

	public void setDP_CODE(String dP_CODE) {
		DP_CODE = dP_CODE;
	}

	public String getDP_CODES() {
		return DP_CODES;
	}

	public void setDP_CODES(String dP_CODES) {
		DP_CODES = dP_CODES;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DISP8Owner) {
			DISP8Owner temp = (DISP8Owner) obj;
			if (this.STREMPID.equals(temp.STREMPID) && this.STRBSN.equals(temp.STRBSN))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.STREMPID.hashCode() + this.STRBSN.hashCode();
	}
}