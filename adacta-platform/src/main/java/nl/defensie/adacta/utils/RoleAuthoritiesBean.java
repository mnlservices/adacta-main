package nl.defensie.adacta.utils;

import java.util.List;
import java.util.Map;

public class RoleAuthoritiesBean {

	// key = P-DOSSIER SIB RAADPLEGEN, value is list of adacta auth: LH02P01DISF01,
	// LH02P01DISF02 etc
	private Map<String, List<String>> roleAuthorities;
	private List<String> otherRoles;
	private List<String> beheerders;

	public Map<String, List<String>> getRoleAuthorities() {
		return roleAuthorities;
	}

	public void setRoleAuthorities(Map<String, List<String>> roleAuthorities) {
		this.roleAuthorities = roleAuthorities;
	}

	public List<String> getOtherRoles() {
		return otherRoles;
	}

	public void setOtherRoles(List<String> otherRoles) {
		this.otherRoles = otherRoles;
	}

	public List<String> getBeheerders() {
		return beheerders;
	}

	public void setBeheerders(List<String> beheerders) {
		this.beheerders = beheerders;
	}

}