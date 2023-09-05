package nl.defensie.adacta.utils;

import java.util.ArrayList;

/**
 * Each bean represents (authority)groups and roles of a single user, derived from Alfresco user store. 
 * A user can have multiple roles (Arbeidsplaats-rolgroep) and a number of associated groups. 
 * We do not know at this point which role corresponds to which groups.
 * 
 * @author wim.schreurs
 *
 */
public class AdactaRoleSyncBean {

	private String emplid;
	
	//bv u01xxxx
	private String userName;
	
	//bv R0012345
	private String role;
	
	//groups bevat authorizatiegroepen, bv LH02P01DISR01
	private ArrayList<String> groups;
	
	//roles bevat rol_emplid, bv R0012345_00000543211
	private ArrayList<String> roles;

	
	
	public String getEmplid() {
		return emplid;
	}
	public void setEmplid(String emplid) {
		this.emplid = emplid;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public ArrayList<String> getGroups() {
		return groups;
	}
	public void setGroups(ArrayList<String> groups) {
		this.groups = groups;
	}
	public ArrayList<String> getRoles() {
		return roles;
	}
	public void setRoles(ArrayList<String> roles) {
		this.roles = roles;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groups == null) ? 0 : groups.hashCode());
		result = prime * result + ((roles == null) ? 0 : roles.hashCode());
		result = prime * result + ((userName == null) ? 0 : userName.hashCode());
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
		AdactaRoleSyncBean other = (AdactaRoleSyncBean) obj;
		if (groups == null) {
			if (other.groups != null)
				return false;
		} else if (!groups.equals(other.groups))
			return false;
		if (roles == null) {
			if (other.roles != null)
				return false;
		} else if (!roles.equals(other.roles))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}
	
}
