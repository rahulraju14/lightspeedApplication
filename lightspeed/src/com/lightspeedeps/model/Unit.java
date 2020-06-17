package com.lightspeedeps.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

/**
 * Unit entity. One or more Unit's exist for each Project. They
 * are number sequentially beginning with 1.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "unit")
public class Unit extends PersistentObject<Unit> implements Comparable<Unit>, Cloneable {

	private static final long serialVersionUID = -2746019045261261480L;

	// Fields

	/** The Project that 'contains' this Unit. */
	private Project project;

	/** Each Unit has its own schedule. */
	private ProjectSchedule projectSchedule;

	/** A sequential number assigned to each Unit, beginning with 1. */
	private Integer number = 1;

	/** The Unit's name, which is used on most screens and reports. This
	 * may not be blank or null. */
	private String name;

	/** An optional description of the Unit's purpose or objective. */
	private String description;

	/** True if the unit should be included in the Production Report (DPR). */
	private Boolean active = true;

	private Set<ProjectMember> projectMembers = new HashSet<>(0);

	private Set<UnitStripboard> unitSbs = new HashSet<>(0);

	@Transient
	private boolean checked;

	// Constructors

	/** default constructor */
	public Unit() {
	}

	/** minimal constructor */
	public Unit(Project project, Integer number, String name) {
		this.project = project;
		this.number = number;
		this.name = name;
	}

	/** full constructor */
//	public Unit(Project project, ProjectSchedule projectSchedule, Integer number, String name,
//			String description) {
//		this.project = project;
//		this.projectSchedule = projectSchedule;
//		this.number = number;
//		this.name = name;
//		this.description = description;
//	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_id", nullable = false)
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Schedule_Id")
	public ProjectSchedule getProjectSchedule() {
		return projectSchedule;
	}
	public void setProjectSchedule(ProjectSchedule projectSchedule) {
		this.projectSchedule = projectSchedule;
	}

	@Column(name = "Number", nullable = false)
	public Integer getNumber() {
		return number;
	}
	public void setNumber(Integer number) {
		this.number = number;
	}

	@Column(name = "Name", nullable = false, length = 9)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "Description", length = 100)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	/** See {@link #active}. */
	@Column(name = "Active", nullable = false)
	public Boolean getActive() {
		return active;
	}
	/** See {@link #active}. */
	public void setActive(Boolean active) {
		this.active = active;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "unit")
	public Set<ProjectMember> getProjectMembers() {
		return projectMembers;
	}
	public void setProjectMembers(Set<ProjectMember> projectMembers) {
		this.projectMembers = projectMembers;
	}

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "unit")
	public Set<UnitStripboard> getUnitSbs() {
		return unitSbs;
	}
	public void setUnitSbs(Set<UnitStripboard> sbs) {
		unitSbs = sbs;
	}

	@Transient
	public boolean isMain() {
		return (getNumber() != null && getNumber() == 1);
	}

	/** See {@link #checked}. */
	@Transient
	public boolean getChecked() {
		return checked;
	}
	/** See {@link #checked}. */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getNumber() == null) ? 0 : getNumber().hashCode());
		result = prime * result + ((getProject() == null) ? 0 : getProject().hashCode());
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
		Unit other = null;
		try {
			other = (Unit)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getId() != null && other.getId() != null) {
			return getId().equals(other.getId());
		}
		if (! getProject().equals(other.getProject())) {
			return false;
		}
		return getNumber().equals(other.getNumber());
	}

	@Override
	public int compareTo(Unit other) {
		int ret = 1; // so null units sort first
		if (other != null) {
			ret = getProject().compareTo(other.getProject());
			if (ret == 0) {
				ret = getNumber().compareTo(other.getNumber());
			}
		}
		return ret;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[id=" + (getId()==null ? "null" : id);
		s += ", #" + (getNumber()==null ? "null" : getNumber());
		s += ", name=" + (getName()==null ? "null" : getName());
		s += "]";
		return s;
	}

	public void merge(Unit unit) {
		setActive(unit.active);
		setDescription(unit.description);
		setName(unit.name);
	}

}