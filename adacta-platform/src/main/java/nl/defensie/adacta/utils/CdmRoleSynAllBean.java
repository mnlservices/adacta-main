package nl.defensie.adacta.utils;

import java.io.Serializable;

/**
 * Represents an item resulting from the cdm roles all query.
 * 
 * @author wim.schreurs
 *
 */
public class CdmRoleSynAllBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//People soft arbeidsplaats-rolgroep, bv R00123456
	private String role;
	
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		CdmRoleSynAllBean other = (CdmRoleSynAllBean) obj;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		return true;
	}

}