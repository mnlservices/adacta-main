package nl.defensie.adacta.utils;

import nl.defensie.adacta.action.CreateIndexReportActionExecuter;

/**
 * A simple POJO for mapping category and subjects code and labels. This is only used for generating the index report. See {@link CreateIndexReportActionExecuter}.
 * 
 * @author Rick de Rooij
 *
 */
public class MsgLabels {
	private String categoryCode;
	private String categoryLabel;
	private String subjectCode;
	private String subjectLabel;

	public MsgLabels(String categoryCode, String categoryLabel, String subjectCode, String subjectLabel) {
		this.categoryCode = categoryCode;
		this.categoryLabel = categoryLabel;
		this.subjectCode = subjectCode;
		this.subjectLabel = subjectLabel;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	public String getCategoryLabel() {
		return categoryLabel;
	}

	public void setCategoryLabel(String categoryLabel) {
		this.categoryLabel = categoryLabel;
	}

	public String getSubjectCode() {
		return subjectCode;
	}

	public void setSubjectCode(String subjectCode) {
		this.subjectCode = subjectCode;
	}

	public String getSubjectLabel() {
		return subjectLabel;
	}

	public void setSubjectLabel(String subjectLabel) {
		this.subjectLabel = subjectLabel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((categoryCode == null) ? 0 : categoryCode.hashCode());
		result = prime * result + ((categoryLabel == null) ? 0 : categoryLabel.hashCode());
		result = prime * result + ((subjectCode == null) ? 0 : subjectCode.hashCode());
		result = prime * result + ((subjectLabel == null) ? 0 : subjectLabel.hashCode());
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
		MsgLabels other = (MsgLabels) obj;
		if (categoryCode == null) {
			if (other.categoryCode != null)
				return false;
		} else if (!categoryCode.equals(other.categoryCode))
			return false;
		if (categoryLabel == null) {
			if (other.categoryLabel != null)
				return false;
		} else if (!categoryLabel.equals(other.categoryLabel))
			return false;
		if (subjectCode == null) {
			if (other.subjectCode != null)
				return false;
		} else if (!subjectCode.equals(other.subjectCode))
			return false;
		if (subjectLabel == null) {
			if (other.subjectLabel != null)
				return false;
		} else if (!subjectLabel.equals(other.subjectLabel))
			return false;
		return true;
	}
	
}