package nl.defensie.adacta.utils;

/**
 * POJO for DIS_P8_GBR table.
 * 
 * @author Rick de Rooij
 *
 */
public class Gebruiker extends Medewerker {

	protected String STRGEBRUIKERID;
	protected String STRNAAM;
	protected String STRROWSECCLASS;
	protected String STRSETID;
	protected String STRAFDELING;

	public String getSTRGEBRUIKERID() {
		return STRGEBRUIKERID;
	}

	public void setSTRGEBRUIKERID(String sTRGEBRUIKERID) {
		STRGEBRUIKERID = sTRGEBRUIKERID;
	}

	public String getSTRNAAM() {
		return STRNAAM;
	}

	public void setSTRNAAM(String sTRNAAM) {
		STRNAAM = sTRNAAM;
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

	public String getSTRAFDELING() {
		return STRAFDELING;
	}

	public void setSTRAFDELING(String sTRAFDELING) {
		STRAFDELING = sTRAFDELING;
	}

}
