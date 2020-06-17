package com.lightspeedeps.object;

import java.io.Serializable;

/**
 * A class to assist in adding new Role`s to an existing Contact, or adding a new
 * Contact/Role entry to a list, such as in the DPR or Callsheet.
 */
public class ContactRole implements Serializable {
	/** */
	private static final long serialVersionUID = - 2702381992275304241L;

	/** The database id of the Contact being modified. */
	private Integer contactId;

	/** The name of the role selected. */
	private String roleName;

	/** The list (print) priority of the selected role. */
	private short priority;


	public ContactRole() {
	}

	public ContactRole(Integer cId, String rName) {
		contactId = cId;
		roleName = rName;
	}

	public ContactRole(Integer cId, String rName, short pri) {
		contactId = cId;
		roleName = rName;
		priority = pri;
	}

	/** See {@link #contactId}. */
	public Integer getContactId() {
		return contactId;
	}
	/** See {@link #contactId}. */
	public void setContactId(Integer contactId) {
		this.contactId = contactId;
	}

	/** See {@link #roleName}. */
	public String getRoleName() {
		return roleName;
	}
	/** See {@link #roleName}. */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	/** See {@link #priority}. */
	public short getPriority() {
		return priority;
	}
	/** See {@link #priority}. */
	public void setPriority(short priority) {
		this.priority = priority;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contactId == null) ? 0 : contactId.hashCode());
		result = prime * result + ((roleName == null) ? 0 : roleName.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContactRole other = (ContactRole)obj;
		if (contactId == null) {
			if (other.contactId != null)
				return false;
		}
		else if (!contactId.equals(other.contactId))
			return false;
		if (roleName == null) {
			if (other.roleName != null)
				return false;
		}
		else if (!roleName.equals(other.roleName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + contactId;
		s += ", role=" + roleName;
		s += "]";
		return s;
	}

}
