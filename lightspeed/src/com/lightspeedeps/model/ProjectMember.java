package com.lightspeedeps.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

/**
 * ProjectMember entity. This associates a Contact with a Role in a Unit,
 * and also holds the permissions assigned to that Role.  If the Unit is
 * null, then the Role applies to all Units (in all Projects) in the
 * Production.
 */
@NamedQueries ({
	@NamedQuery(name=ProjectMember.GET_PROJECT_MEMBER_BY_UNIT_IDS, query = "from ProjectMember pm where pm.unit.id in (:unitId)"),
	@NamedQuery(name=ProjectMember.GET_PROJECT_MEMBER_BY_PROJECT, query = "from ProjectMember pm where pm.employment.contact.project =:project"),
	@NamedQuery(name=ProjectMember.GET_PROJECT_MEMBER_BY_EMPLOYMENT_PROJECT, query = "from ProjectMember pm where pm.employment =:employment and pm.unit.project =:project"),
	@NamedQuery(name=ProjectMember.GET_PROJECT_BY_PROJECTMEMBER_UNIT_EMPLOYMENT, query = "select u.project from ProjectMember pm, Unit u, Employment e where e.contact=:contact and pm.employment=e and pm.unit=u"),
	@NamedQuery(name=ProjectMember.GET_PROJECT_MEMBER_BY_EMPLOYMENT_UNIT, query = "from ProjectMember pm where pm.employment =:employment and pm.unit =:unit")
})
@Entity
@Table(name = "project_member")
public class ProjectMember extends PersistentObject<ProjectMember> implements Comparable<ProjectMember>, Cloneable {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ProjectMember.class);

	/** for serialization */
	private static final long serialVersionUID = -1503640950210187033L;

	public static final String GET_PROJECT_MEMBER_BY_PROJECT = "getProjectMemberByProject";
	public static final String GET_PROJECT_MEMBER_BY_UNIT_IDS = "getProjectMemberByUnitIds";
	public static final String GET_PROJECT_BY_PROJECTMEMBER_UNIT_EMPLOYMENT = "getProjectByProjectMemberUnitEmployment";
	public static final String GET_PROJECT_MEMBER_BY_EMPLOYMENT_PROJECT = "getProjectMemberByEmploymentProject";
	public static final String GET_PROJECT_MEMBER_BY_EMPLOYMENT_UNIT = "getProjectMemberByEmploymentUnit";

	/**
	 * The sort key for the ordering desired in the "Positions" (Roles) tab of
	 * the Contact page. This is grouped by Project, with the most recent
	 * project (defined as the largest database id) first. Within a project,
	 * the ordering is by Unit number, then by Role name.
	 */
	public static final String SORTKEY_ROLELIST = "rolelist";

	// Fields

	/** The Employment record associated with this membership.  Many memberships may
	 * be related to a single Employment record. */
	private Employment employment;

	/** The Unit that this Role & Contact is associated with.  If null, then
	 * the Contact has the given Role in all Units for the Production. */
	private Unit unit;

	/** The Role assigned to the Contact in the given Unit (or all Units). */
//	private Role role; // Moved to Employment

//	/** The Contact associated with the Role and Unit. Note that a Contact is
//	 * (by definition) associated with a single Production. */
//	private Contact contact; // change to deprecated delegate method using Employment // DONE

//	/** The Permission`s, specified as a 64-bit mask, assigned to this Contact
//	 * for this Role.  These permissions may have been customized.  If they
//	 * have not been customized, this field will be identical to Role.roleGroup.permissionMask. */
//	private long permissionMask; // Moved to Employment

//	/** The List of StartForm`s that were created by referencing this ProjectMember. */
//	private final List<StartForm> startForms = new ArrayList<StartForm>(0);

//	/** The Set of Permission values matching the permissionMask value. */
//	@Transient
//	private Set<Permission> permissions;

	/** A Transient value - the database id of the Department to which
	 * the Role of this ProjectMember belongs. */
//	@Transient
//	private Integer departmentId; // made this a transient in Employment.

//	@Transient
//	private Integer relatedStartFormId; // made this a transient in Employment.

	/** The effective start date to be assigned to the newly-created StartForm. */
//	@Transient
//	private Date effectiveStartDate; // made this a transient in Employment.

//	@Transient
//	private boolean copyStartData; // made this a transient in Employment.



	// Constructors

	/** default constructor */
	public ProjectMember() {
	}

	/**
	 * The new standard constructor for a ProjectMember.
	 * @param unit
	 * @param emp
	 */
	public ProjectMember(Unit unit, Employment emp) {
		this.unit = unit;
		employment = emp;
	}

	/** See {@link #employment}. */
	@Cascade({CascadeType.SAVE_UPDATE})
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Employment_Id")
	public Employment getEmployment() {
		return employment;
	}
	/** See {@link #employment}. */
	public void setEmployment(Employment employment) {
		this.employment = employment;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Unit_Id")
	public Unit getUnit() {
		return unit;
	}
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	/**
	 * The compare method used for sorting the ProjectMember list in the
	 * Positions (Roles) mini-tab of the Contact View page.
	 *
	 * @param member The ProjectMember to compare to 'this' object; may be null.
	 * @param sortField A literal identifying which field (or combination of
	 *            fields) to sort on. A value of SORTKEY_ROLELIST is specified
	 *            for the Positions table ordering.
	 * @return standard compare values -1/0/1
	 */
	public int compareTo(ProjectMember member, String sortField) {
		int ret = 1;
		if (sortField.equals(SORTKEY_ROLELIST)) {
			if (member != null) {
				ret = compareUnitForNull(member.getUnit());
				if (ret == 0) { // both null or both non-null
					if (getUnit() != null) {
						// sort by project id, descending, to get most recent projects on top
						ret = getUnit().getProject().compareTo(member.getUnit().getProject(), Project.SORTKEY_ID, false);
						if (ret == 0) { // then by Unit Number
							ret = getUnit().getNumber().compareTo(member.getUnit().getNumber());
							if (ret == 0 && getEmployment() != null && member.getEmployment() != null) { // finally by Role
								ret = getEmployment().getRole().compareTo(member.getEmployment().getRole());
							}
						}
					}
					else if (getEmployment() != null) { // both units are null
						ret = getEmployment().getRole().compareTo(member.getEmployment().getRole());
					}
				}
			}
		}
		else {
			ret = compareTo(member);
		}
		return ret;
	}

	/**
	 * Default ProjectMember comparison -- currently unused.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ProjectMember member) {
		int ret = 1;
		if (member != null) {
			ret = compareUnitForNull(member.getUnit());
			if (ret == 0 && getUnit() != null) {
				ret = getUnit().compareTo(member.getUnit());
			}
			if (ret == 0) {
				ret = getEmployment().getContact().compareTo(member.getEmployment().getContact());
				if (ret == 0) {
					ret = getEmployment().getRole().compareTo(member.getEmployment().getRole());
				}
			}
		}
		return ret;
	}

	private int compareUnitForNull(Unit pUnit) {
		int ret = 0;
		if (getUnit() != null) {
			if (pUnit == null) {
				ret = 1;
			}
		}
		else if (pUnit != null) {
			ret = -1;
		}
		return ret;
	}

	@Override
	public ProjectMember clone() {
		ProjectMember pm;
		try {
			pm = (ProjectMember)super.clone();
			pm.id = null;
		}
		catch (CloneNotSupportedException e) {
			//log.error(e);
			return null;
		}
		return pm;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "unit="+ (getUnit()==null ? "null" : getUnit().getId());
		/*s += ", role="+ (getEmployment().getRole()==null ? "null" : getEmployment().getRole().getId());
		s += ", contact="+ (getEmployment().getContact()==null ? "null" : getEmployment().getContact().getId());
		s += ", mask=" + (Permission.toBinaryString(getEmployment().getPermissionMask()));*/
		s += " ]";
		return s;
	}

	/**
	 * Determine if one ProjectMember is a duplicate of another.
	 *
	 * @param pm The ProjectMember to be compared.
	 * @return True iff the given ProjectMember is a duplicate of 'this'
	 *         ProjectMember.
	 */
	public boolean duplicateOf(ProjectMember pm) {
		return getEmployment().getRole().getId().equals(pm.getEmployment().getRole().getId()) &&
				getEmployment().getContact().getId().equals(pm.getEmployment().getContact().getId()) &&
				((getUnit() == null && pm.getUnit() == null) ||
						getUnit().getId().equals(pm.getUnit().getId()));
	}

}