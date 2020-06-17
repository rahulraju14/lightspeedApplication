package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.object.BitMask;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * Department entity. Departments provide a way of organizing cast and crew
 * members and their roles. Each Role in the system is associated with a
 * Department; this determines, for example, where in the Callsheet and Dpr
 * reports the person's entry will appear.
 * <p>
 * Each Department is associated with a Production.  Departments associated
 * with the SYSTEM production are shared across all Productions.  Departments
 * associated with a non-SYSTEM production only appear in that specific
 * Production's on-screen lists and printed reports.
 */
@Entity
@Table(name = "department")
public class Department extends PersistentObject<Department> implements Comparable<Department>, Cloneable {
	private static final Log log = LogFactory.getLog(Department.class);

	private static final long serialVersionUID = 3288570308040079765L;

	public static final String SORT_KEY_NAME = "name";
	public static final String SORT_KEY_PRIORITY = "priority";

	public static final BitMask UNIQUE_DEPT_MASK = new BitMask(-1);

	// Fields

	/** The Production this Department is defined for, or null if it is one
	 * of the standard Department`s. */
	private Production production;

	/** The Project this Department is defined for, or null if it is one
	 * of the standard Department`s, or applies to an entire Production. */
	private Project project;

	/** The displayed name of the Department. */
	private String name;

	/** Description */
	private String description;

	/** The bit mask that represents this dept; used in conjunction with
	 * production.deptMask and project.deptMask. */
	@Transient
	private BitMask mask = new BitMask();

	/** True iff this department should be added to the Callsheet automatically. */
	private boolean showOnCallsheet = true;

	/** True iff this department should be added to the Dpr automatically. */
	private boolean showOnDpr = true;

	/** This value controls the order in which departments appear in the
	 * DPR (Production Report).  A value of zero indicates the department
	 * should not be included in the report. */
	private Integer listPriority;

	/**
	 * This is the department.id value of the "standard" department (defined in
	 * the SYSTEM production) of a corresponding department. This is used when
	 * either (a) a time-keeper is defined for a Department; or (b) the user
	 * renames a standard Department. In both cases we construct a 'duplicate'
	 * Department object for the user's Production to track the
	 * Production-specific value(s). This field will be null for a standard
	 * department entry -- one owned by the SYSTEM production.
	 */
	private Integer standardDeptId;

	/** The time-keeper for this Department; null if none is assigned. */
	private Contact timeKeeper;

	//private Set<DeptCall> deptCalls = new HashSet<DeptCall>(0);

	//private Set<Contact> contacts = new HashSet<Contact>(0);
	/** The List of Role`s that are defined to be in this Department.
	 * This information is maintained the the Permission spreadsheet, and
	 * loaded via SQL into the Role table.
	 * See src.sql.prod.permissionsRolesAccess.sql. */
//	private List<Role> roles = new ArrayList<Role>(0);

	/** Used in left-hand lists; true iff this is the selected item in list. */
	@Transient
	private boolean selected;

	/** Used in JSP displays; true iff this active (included in the mask) for the
	 * current Project or Production. */
	@Transient
	private boolean active;

	/** Used to hold the number of approvers in department for timecards. */
	@Transient
	private Integer approverCount;

	/**  Boolean field used to disable Department for an Approver Group if it is associated with another Approver Group, default is false */
	@Transient
	private boolean disabled = false;

	// Constructors

	/** default constructor */
	public Department() {
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id")
	public Production getProduction() {
		return production;
	}
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #project}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

	@Column(name = "Name", nullable = false, length = 50)
	public String getName() {
		//log.debug("id="+id);
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #description}. */
	@Column(name = "Description", length = 1000)
	public String getDescription() {
		return description;
	}
	/** See {@link #description}. */
	public void setDescription(String description) {
		this.description = description;
	}

	/** See {@link #mask}. */
	@Transient
	public BitMask getMask() {
		getMaskDb(); // ensure Hibernate has retrieved value
		return mask;
	}
	/** See {@link #mask}. */
	public void setMask(BitMask mask) {
		this.mask = mask;
	}

	/** See {@link #mask}. */
	@Column(name = "Mask")
	public long getMaskDb() {
		return mask.getLong();
	}
	/** See {@link #mask}. */
	public void setMaskDb(long msk) {
		mask.set(msk);
	}

	/** See {@link #showOnCallsheet}. */
	@Column(name = "Show_On_Callsheet", nullable = false)
	public boolean getShowOnCallsheet() {
		return showOnCallsheet;
	}
	/** See {@link #showOnCallsheet}. */
	public void setShowOnCallsheet(boolean showOnCallsheet) {
		this.showOnCallsheet = showOnCallsheet;
	}

	/** See {@link #showOnDpr}. */
	@Column(name = "Show_On_Dpr", nullable = false)
	public boolean getShowOnDpr() {
		return showOnDpr;
	}
	/** See {@link #showOnDpr}. */
	public void setShowOnDpr(boolean showOnDpr) {
		this.showOnDpr = showOnDpr;
	}

	/** See {@link #listPriority}. */
	@Column(name = "List_Priority", nullable = false)
	public Integer getListPriority() {
		return listPriority;
	}
	/** See {@link #listPriority}. */
	public void setListPriority(Integer listPriority) {
		this.listPriority = listPriority;
	}

/*	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "department")
	public Set<DeptCall> getDeptCalls() {
		return this.deptCalls;
	}
	public void setDeptCalls(Set<DeptCall> deptCalls) {
		this.deptCalls = deptCalls;
	}
*/

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "department")
//	public Set<Contact> getContacts() {
//		return this.contacts;
//	}
//	public void setContacts(Set<Contact> contacts) {
//		this.contacts = contacts;
//	}

	/** See {@link #standardDeptId}. */
	@Column(name = "Std_Dept_Id")
	public Integer getStandardDeptId() {
		return standardDeptId;
	}
	/** See {@link #standardDeptId}. */
	public void setStandardDeptId(Integer standardDeptId) {
		this.standardDeptId = standardDeptId;
	}

	/** See {@link #timeKeeper}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Id")
	public Contact getTimeKeeper() {
		return timeKeeper;
	}
	/** See {@link #timeKeeper}. */
	public void setTimeKeeper(Contact timeKeeper) {
		this.timeKeeper = timeKeeper;
	}

	/**
	 * @return true iff this Department is a custom department, that is, not a
	 *         LightSPEED-defined department. Custom departments are associated
	 *         with a specific Production, not with the SYSTEM Production.
	 */
	@Transient
	public boolean isCustom() {
		// Test if this is associated with the SYSTEM Production.
		return (getProduction().getId() != Constants.SYSTEM_PRODUCTION_ID);
	}

	/**
	 * @return True iff this Department is not a system department, and is not a
	 *         "proxy" for a system department. For example, if a user assigns a
	 *         time-keeper to the "Grip" department, that creates a proxy for
	 *         the system "Grip" department; such a proxy will return false for
	 *         isUnique(), but true for isCustom(). However, if they add a new
	 *         Department like "My Own Dept", that will return true for both
	 *         isUnique() and isCustom().
	 */
	@Transient
	public boolean isUnique() {
		/*
		 * Any newly created (that is, unique) department has its 'mask' set to -1;
		 * this provides a quick test for uniqueness.
		 */
		return (getMask().equals(UNIQUE_DEPT_MASK));
		/*
		 * This is the way that COULD be used to test for uniqueness, if we weren't
		 * able to use the maskDb value for some reason:
		 * 		return (getStandardDeptId() == null && isCustom());
		 * (Since a "proxy" will have a non-null standardDeptId value.)
		 */
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "department")
//	@OrderBy("name")
//	public List<Role> getRoles() {
//		return this.roles;
//	}
//	public void setRoles(List<Role> roles) {
//		this.roles = roles;
//	}

	public int compareTo(Department d2, String sortColumnName, boolean ascending) {
		int ret = compareTo(d2, sortColumnName);
		if ( ! ascending ) {
			ret = 0 - ret;	// swap 1 and -1 return values
		}
		return ret;
	}

	public int compareTo(Department d2, String sortColumnName) {
		int ret = 0;
		if (sortColumnName.equals(SORT_KEY_PRIORITY)) {
			ret = NumberUtils.compare(getListPriority(), d2.getListPriority());
		}
		if (ret == 0) {
			ret = compareTo(d2);
		}
		return ret;
	}

	/**
	 * The default comparison for Department, which uses the
	 * department name.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Department dept) {
		int ret = 1;
		if (dept != null) {
			return getName().compareToIgnoreCase(dept.getName());
		}
		return ret;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	/**
	 * Compares Department objects based on their database id and/or name.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Department other = null;
		try {
			other = (Department)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		}
		else if (getId().equals(other.getId()))
			return true;
		return getName().equals(other.getName());
	}

	/** See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean b) {
		selected = b;
	}

	/** See {@link #active}. */
	@Transient
	public boolean getActive() {
		return active;
	}
	/** See {@link #active}. */
	public void setActive(boolean active) {
		this.active = active;
	}

	/** See {@link #approverCount}. */
	@Transient
	public Integer getApproverCount() {
		return approverCount;
	}
	/** See {@link #approverCount}. */
	public void setApproverCount(Integer approverCount) {
		this.approverCount = approverCount;
	}

	@Transient
	/** See {@link #disabled}. */
	public boolean getDisabled() {
		return disabled;
	}
	/** See {@link #disabled}. */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	@Override
	public Department clone() {
		Department newdept;
		try {
			newdept = (Department)super.clone();
			newdept.id = null;
			newdept.production = null;
			newdept.project = null;
			newdept.timeKeeper = null;
			newdept.selected = false;
		}
		catch (CloneNotSupportedException e) {
			log.error("Department clone error: ", e);
			return null;
		}
		return newdept;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "name=" + (getName()==null ? "null" : getName());
		s += "]";
		return s;
	}

}