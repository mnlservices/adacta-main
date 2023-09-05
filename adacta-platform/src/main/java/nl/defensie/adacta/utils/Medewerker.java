package nl.defensie.adacta.utils;

import java.sql.Date;

/**
 * POJO for DIS_P8_MDW table.
 * 
 * @author Rick de Rooij
 *
 */
public class Medewerker {

	protected String STRBSN;
	protected String STREMPID;
	protected String STRMRN;
	protected String STRNAAM;
	protected Date DATGEBOORTE;
	protected Date DATUITDIENST;
	protected String STRROWSECCLASS;
	protected String STRSETID;
	
	public Medewerker() {}

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

	public String getSTRROWSECCLASS() {
		return STRROWSECCLASS;
	}

	public void setSTRROWSECCLASS(String sTRROWSECCLASS) {
		STRROWSECCLASS = sTRROWSECCLASS;
	}

	public String getSTRSETID() {
		return STRSETID;
	}

	public void setSTRSETID(String sTRSETID) {
		STRSETID = sTRSETID;
	}
}
