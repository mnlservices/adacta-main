package nl.defensie.adacta.utils;

import java.io.Serializable;

/**
 * Represents an item resulting from the cdm roles query.
 * 
 * @author wim.schreurs
 *
 */
public class CdmRoleSyncBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//People soft arbeidsplaats-rolgroep, bv R00123456
	private String role;
	//groeps naam voor document rechten, FB DOSSIERBEHEER
	private String authority;
	//emplid, werknemernummer
	private String emplid;
	
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public String getAuthority() {
		return authority;
	}
	public void setAuthority(String authority) {
		this.authority = authority;
	}
	public String getEmplid() {
		return emplid;
	}
	public void setEmplid(String emplid) {
		this.emplid = emplid;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authority == null) ? 0 : authority.hashCode());
		result = prime * result + ((emplid == null) ? 0 : emplid.hashCode());
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CdmRoleSyncBean other = (CdmRoleSyncBean) obj;
		if (authority == null) {
			if (other.authority != null)
				return false;
		} else if (!authority.equals(other.authority))
			return false;
		if (emplid == null) {
			if (other.emplid != null)
				return false;
		} else if (!emplid.equals(other.emplid))
			return false;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		return true;
	}
	
	
	}
