package com.lightspeedeps.model;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import com.lightspeedeps.dao.RoleDAO;
import com.lightspeedeps.type.CastContractType;
import com.lightspeedeps.type.RoleSelectType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;

/**
 * Role entity.  This defines a position or job within the Lightspeed system.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "role")
public class Role extends PersistentObject<Role> implements Cloneable, Comparable<Role> {
	private static final Log log = LogFactory.getLog(Role.class);

	private static final long serialVersionUID = -601881398366467904L;

	// Fields
	private RoleGroup roleGroup;
	private String name;
//	private String description;

	/** Determines the order of listing the role on the call sheet, within its department.
	 * If the value is less than 1, people with this role are not automatically included
	 * on the call sheet. */
	private Integer listPriority;

	/** The Production for which this Role is defined.  A Role linked to the SYSTEM
	 * Production is visible in all productions.  A Role linked to any other Production
	 * is only visible in that Production. */
	private Production production;

	/** The department to which this role is assigned; determines where the person with this
	 * role is listed on the call sheet. */
	private Department department;

	/** The Lightspeed-assigned Occupation Code for a specific occupation. These
	 * codes are for non-union occupations. */
	private String lsOccCode;

	/** To determine which type of union cast contract for this role. Is null for all
	 * departments other than Union Cast departments.
	 */
	private RoleSelectType roleSelectType;

	/** To determine which type of union cast contract for this role. Is null for all
	 * departments other than Union Cast departments.
	 */
	private CastContractType castContractType;

	@Transient
	private Boolean cast;

	@Transient
	private Boolean stunt;

	// Constructors

	/** default constructor */
	public Role() {
	}

	/**
	 * Constructor used for fake "new role" object.
	 * @param pId
	 */
	public Role(Integer pId) {
		id = pId;
		name = ""; // so equals() doesn't get an exception
	}

	/**
	 * Constructor used when copying a custom Role to a new Production.
	 *
	 * @param prod The Production to which the Role belongs.
	 * @param pName The name of the Role.
	 * @param group The RoleGroup to which the Role belongs.
	 * @param dept The Department to which the Role belongs, which may be either
	 *            a custom or system-defined one.
	 * @param priority The list (print) priority of this Role.
	 */
	public Role(Production prod, String pName, RoleGroup group,
			Department dept, Integer priority) {
		production = prod;
		name = pName;
		roleGroup = group;
		department = dept;
		listPriority = priority;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Role_Group_Id", nullable = false)
	public RoleGroup getRoleGroup() {
		return roleGroup;
	}
	public void setRoleGroup(RoleGroup roleGroup) {
		this.roleGroup = roleGroup;
	}

	@Column(name = "Name", nullable = false, length = 50)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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

	/** See {@link #production}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id", nullable = false)
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/**
	 * See {@link #department}. It has the Hibernate CascadeType.SAVE_UPDATE
	 * property so that when a (new) Role is saved which refers to a new
	 * (custom) Department, it will save that Department as well.
	 * <p>
	 * Note that using the javax CascadeType.PERSIST did NOT work -- it did not
	 * cascade the save operation (from ProjectDAO.createNewProject).
	 */
	@Cascade({CascadeType.SAVE_UPDATE})
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Department_Id", nullable = false)
	public Department getDepartment() {
		return department;
	}
	/** See {@link #department}. */
	public void setDepartment(Department department) {
		this.department = department;
	}

	/**See {@link #lsOccCode}. */
	@Column(name = "Ls_Occ_Code", length = 10)
	public String getLsOccCode() {
		return lsOccCode;
	}
	/**See {@link #lsOccCode}. */
	public void setLsOccCode(String lSoccCode) {
		lsOccCode = lSoccCode;
	}

	/** See {@link #roleSelectType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Role_Select_Type", length = 30)
	public RoleSelectType getRoleSelectType() {
		return roleSelectType;
	}
	/** See {@link #roleSelectType}. */
	public void setRoleSelectType(RoleSelectType roleSelectType) {
		this.roleSelectType = roleSelectType;
	}

	/** See {@link #castContractType}. */
	@Enumerated(EnumType.STRING)
	@Column(name="Cast_Contract_Type", length = 30)
	public CastContractType getCastContractType() {
		return castContractType;
	}
	/** See {@link #castContractType}. */
	public void setCastContractType(CastContractType castContractType) {
		this.castContractType = castContractType;
	}

	/** @return True iff this Role is a cast role, which is detected by the role id falling within a
	 * specific range of numeric values. */
	@Transient
	public boolean isCast() {
		if (cast == null) {
			boolean b = false;
			if (getId() != null) {
				b = RoleDAO.isCast(this);
			}
			else {
				EventUtils.logError("Role.getId() returned null!");
			}
			cast = b;
		}
		return cast;
	}

	/** @return True iff this Role is a cast role, which is detected by the role id falling within a
	 * specific range of numeric values. */
	@Transient
	public boolean isStunt() {
		if (stunt == null) {
			boolean b = false;
			if (getId() != null) {
				b = RoleDAO.isStunt(this);
			}
			else {
				EventUtils.logError("Role.getId() returned null!");
			}
			stunt = b;
		}
		return stunt;
	}

	/** @return True iff this Role is a cast role, which is detected by the role id falling within a
	 * specific range of numeric values. */
	@Transient
	public boolean isCastOrStunt() {
		return isCast() || isStunt();
	}

	/**
	 * @return True iff this Role is a "crew" role, excluding Stunts. This is any role
	 * that is (a) not cast or stunt, (b) not LS Admin, and (c) not External
	 * (vendor).
	 */
	@Transient
	public boolean isCrewNotStunt() {
		return !isCastOrStunt() && !isAdmin() && !getName().toLowerCase().contains("external");
	}

	/** @return True if the role is "crew" for the purposes of timecards.  This excludes the Prod Data Admin
	 * role, which is included in the standard 'isCrew' test. */
	@Transient
	public boolean isCrewTc() {
		return isCrewNotStunt() && !isDataAdmin() && !isFinancialAdmin();
	}

	/** @return True iff this Role is an "admin" role, which includes LS Admin and 'VIP' roles;
	 * this test is generally used for hiding administrative users from displayed lists. */
	@Transient
	public boolean isAdmin() {
		return (isLsAdmin() || isViewAdmin() || isLsCoord() || isLsVIP());
	}

	/** @return true iff this Role is the LS eps Administrator role. */
	@Transient
	public boolean isLsAdmin() {
		return (getId().intValue() == Constants.ROLE_ID_LS_ADMIN);
	}

	/** @return true iff this Role is the LS Coordinator role. */
	@Transient
	public boolean isLsCoord() {
		return (getId().intValue() == Constants.ROLE_ID_LS_COORDINATOR);
	}

	/** @return true iff this Role is the LS eps Administrator View role. */
	@Transient
	public boolean isViewAdmin() {
		return (getId().intValue() == Constants.ROLE_ID_LS_ADMIN_VIEW);
	}

	/** @return true iff this Role is the LS eps Administrator role. */
	@Transient
	public boolean isLsVIP() {
		return (getId().intValue() == Constants.ROLE_ID_LS_VIP);
	}

	/** @return true iff this Role is the Prod Data Administrator role. */
	@Transient
	public boolean isDataAdmin() {
		return (getId().intValue() == Constants.ROLE_ID_DATA_ADMIN);
	}

	/** @return true iff this Role is the Prod Financial Administrator role. */
	@Transient
	public boolean isFinancialAdmin() {
		return (getId().intValue() == Constants.ROLE_ID_FINANCIAL_ADMIN);
	}

	/** @return true iff this Role is a production-wide role, that is, it is
	 * not specific to a single Unit or Project. */
	@Transient
	public boolean isProductionWide() {
		return (isLsAdmin() || isDataAdmin() || isFinancialAdmin() || isViewAdmin()) || isLsCoord(); // LS-2475
	}

	/** @return true iff this Role is a custom role, that is, not a
	 * LightSPEED-defined Role.  Custom roles are associated with a
	 * custom RoleGroup, and are not associated with the SYSTEM Production. */
	@Transient
	public boolean isCustom() {
		// A custom Role will not belong to the SYSTEM Production.
		return (getProduction().getId() != Constants.SYSTEM_PRODUCTION_ID);
	}

	/**
	 * The default comparison of Roles, which compares their role name.
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Role role) {
		int ret = 1;
		if (role != null) {
			ret = getName().compareTo(role.getName());
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
		result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
		return result;
	}

	/**
	 * @see com.lightspeedeps.model.PersistentObject#equalsData(com.lightspeedeps.model.PersistentObject)
	 */
	@Override
	protected boolean equalsData(Role other) {
		return getName().equals(other.getName());
	}

	/**
	 * @return A copy of this object, but only including the primitive data
	 *         items; all embedded object references are null in the returned
	 *         copy.
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Role clone() {
		Role newRole;
		try {
			newRole = (Role)super.clone();
			newRole.id = null;
			newRole.production = null;
			newRole.department = null;
		}
		catch (CloneNotSupportedException e) {
			log.error("Role clone error: ", e);
			return null;
		}
		return newRole;
	}

	/**
	 * @see com.lightspeedeps.model.PersistentObject#toString()
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