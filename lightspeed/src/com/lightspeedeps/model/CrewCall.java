package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * CrewCall entity. This holds the data for one line (one person) in the crew
 * table on the "back page" of the Call sheet.  A set of CrewCall objects is
 * held be a {@link DeptCall} object.
 */
@Entity
@Table(name = "crew_call", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Dept_Call_Id", "Line_Number" }))
public class CrewCall extends PersistentObject<CrewCall> {
	private static final long serialVersionUID = - 131817726051230137L;

	// Fields

	/** The DeptCall object indicates which department this CrewCall
	 * object belongs to. */
	private DeptCall deptCall;
	/** The (usually sequential) line number of this entry, within its
	 * DeptCall group. */
	private Integer lineNumber;
	/** The count field. This is always(?) 1. */
	private Integer count;
	/** The print priority -- used to order the list */
	private short priority;
	/** The role (occupation) of the person. */
	private String roleName;
	/** The Contact.id of the person. */
	private Integer contactId;
	/** The person's name as "first last" */
	private String name;
	/** The call time for the person; if null, this is displayed as "O/C" (on-call). */
	private Date time;

	// Constructors

	/** default constructor */
	public CrewCall() {
	}

	/** constructor */
	public CrewCall(DeptCall deptCall, Integer lineNumber, Integer count,
			String roleName, String name, Date time) {
		this.deptCall = deptCall;
		this.lineNumber = lineNumber;
		this.count = count;
		this.roleName = roleName;
		this.name = name;
		this.time = time;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Dept_Call_Id", nullable = false)
	public DeptCall getDeptCall() {
		return deptCall;
	}
	public void setDeptCall(DeptCall deptCall) {
		this.deptCall = deptCall;
	}

	@Column(name = "Line_Number", nullable = false)
	public Integer getLineNumber() {
		return lineNumber;
	}
	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Column(name = "Count")
	public Integer getCount() {
		return count;
	}
	public void setCount(Integer count) {
		this.count = count;
	}

	@Column(name = "Role_Name", length = 50)
	public String getRoleName() {
		return roleName;
	}
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	/** See {@link #priority}. */
	@Column(name = "priority")
	public short getPriority() {
		return priority;
	}
	/** See {@link #priority}. */
	public void setPriority(short priority) {
		this.priority = priority;
	}

	/** See {@link #contactId}. */
	@Column(name = "contact_id")
	public Integer getContactId() {
		return contactId;
	}
	/** See {@link #contactId}. */
	public void setContactId(Integer contactId) {
		this.contactId = contactId;
	}

	@Column(name = "Name", length = 50)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Time", length = 0)
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + id;
		s += ", n=" + count;
		s += ", line=" + lineNumber;
		s += ", name=" + name;
		s += ", role=" + roleName;
		s += ", time=" + time;
		s += "]";
		return s;
	}

}