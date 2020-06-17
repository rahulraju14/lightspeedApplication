package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.util.common.NumberUtils;

/**
 * DeptCall entity. There is a set of these for each callsheet, one for each
 * Department listed in the "crew call" section of the report.  This contains
 * a List of CrewCall objects, each entry holding the call time for one contact.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "dept_call", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Callsheet_Id", "Department_Id" }))
public class DeptCall extends PersistentObject<DeptCall> implements Comparable<DeptCall> {
	private static final long serialVersionUID = -8562987656287874563L;

	// Fields
	private Department department;
	private Callsheet callsheet;
	private String deptName;

	/** Determines the order of listing on the call sheet, lowest value
	 * listed first. */
	private int priority = 0;

	private List<CrewCall> crewCalls = new ArrayList<CrewCall>(0);

	// Constructors

	/** default constructor */
	public DeptCall() {
	}

	public DeptCall(Callsheet sheet, Department dept) {
		callsheet = sheet;
		department = dept;
		deptName = department.getName();
		// Note: Department.listPriority may be negative for Department's not
		// included in call sheet by default.
		priority = Math.abs(department.getListPriority());
	}

	/**
	 * Constructor for "user-defined" department entry on the callsheet. Note
	 * that it has a department name, but not an actual Department object
	 * reference.
	 *
	 * @param sheet The associated callsheet.
	 * @param name The name to be displayed at the top of the department's
	 *            detail lines.
	 * @param prty The priority of this entry in the overall list of departments
	 *            for the callsheet. (Lower values print first.) Typically, the
	 *            user-defined department is printed last.
	 */
	public DeptCall(Callsheet sheet, String name, int prty) {
		callsheet = sheet;
		deptName = name;
		priority = prty;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Department_Id")
	public Department getDepartment() {
		return this.department;
	}
	public void setDepartment(Department department) {
		this.department = department;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Callsheet_Id", nullable = false)
	public Callsheet getCallsheet() {
		return this.callsheet;
	}
	public void setCallsheet(Callsheet callsheet) {
		this.callsheet = callsheet;
	}

	@Column(name = "Dept_Name", nullable = false, length = 50)
	public String getDeptName() {
		return this.deptName;
	}
	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}

	/** See {@link #priority}. */
	@Column(name = "Priority", nullable = false)
	public int getPriority() {
		return priority;
	}
	/** See {@link #priority}. */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "deptCall")
	@OrderBy("lineNumber")
	public List<CrewCall> getCrewCalls() {
		return this.crewCalls;
	}
	public void setCrewCalls(List<CrewCall> crewCalls) {
		this.crewCalls = crewCalls;
	}

	@Override
	public int compareTo(DeptCall other) {
		if (other == null) {
			return 1;
		}
		return NumberUtils.compare(priority, other.priority);
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + id;
		s += ", csId=" + getCallsheet().getId();
		s += ", deptId=" + (getDepartment()==null ? "null" : getDepartment().getId());
		s += ", deptName=" + getDeptName();
		s += ", priority=" + priority;
		if (getCrewCalls() != null) {
			s += ", crew calls:\n";
			for (CrewCall cc : getCrewCalls()) {
				s += cc.toString() + "\n";
			}
		}
		s += "]";
		return s;
	}

}