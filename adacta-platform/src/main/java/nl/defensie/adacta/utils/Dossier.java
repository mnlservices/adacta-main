package nl.defensie.adacta.utils;

import java.sql.Date;

/**
 * POJO for ADACTA_DOSSIERS table.
 * 
 * @author Rick de Rooij
 *
 */

public class Dossier {

	private String STRBSN;
	private String STREMPID;
	private String STRMRN;
	private String STRNAAM;
	private Date DATUITDIENST;
	private String STRSETID;
	private Date LASTCHANGEDATE;

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

	public Date getLASTCHANGEDATE() {
		return LASTCHANGEDATE;
	}

	public void setLASTCHANGEDATE(Date lASTCHANGEDATE) {
		LASTCHANGEDATE = lASTCHANGEDATE;
	}
}