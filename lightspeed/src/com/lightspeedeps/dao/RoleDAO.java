package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.*;
import com.lightspeedeps.type.Permission;
import com.lightspeedeps.type.RoleSelectType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.login.AuthorizationBean;

/**
 * A data access object (DAO) providing persistence and search support for
 * Production entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Role
 */

public class RoleDAO extends BaseTypeDAO<Role> {
	//@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(RoleDAO.class);

	// property constants
	private static final String NAME = "name";
	public static final String PRODUCTION = "production";
//	private static final String DESCRIPTION = "description";
//	private static final String LIST_PRIORITY = "listPriority";
	public static final String DEPARTMENT = "department";

	// pre-defined role names for looking up users by role
	public static final String DIRECTOR = "Director";
	public static final String PRODUCER = "Producer";
	public static final String FIRST_AD = "1st AD";
	public static final String SECOND_AD = "Key 2nd AD";
	public static final String LOCATION_MANAGER = "Location Manager";
	public static final String UNIT_PM = "UPM";

	/** Database specific value! The lowest Role.id value for a Cast (Talent) role. */
	/*package*/ static final int TALENT_ROLE_ID_MINIMUM = 39;
	/** Database specific value! The highest Role.id value for a Cast (Talent) role. */
	/*package*/ static final int TALENT_ROLE_ID_MAXIMUM = 50;

	/*package*/ static final int STUNT_CAST_ROLE_ID_MINIMUM = 389;
	/*package*/ static final int STUNT_CAST_ROLE_ID_MAXIMUM = 396;

	private static final int MAXIMUM_SYSTEM_ROLE_ID = 9999;

	public static RoleDAO getInstance() {
		return (RoleDAO)getInstance("RoleDAO");
	}

	public Role findByName(String name) {
		Role role = findOneByProperty(NAME, name);
		if (role == null) {
			LOG.warn("Role name '" + name + "' not found. ");
		}
		return role;
	}

	/**
	 * Retrieve list of roles for a specified department based off of the
	 * RoleSelectType (if any) for roles related to payroll-specific criteria.
	 * It returns roles from the system list and from the specified Production's
	 * custom list (if any).
	 *
	 * @param prod - Associated Production.
	 * @param dept - Department to query for Roles
	 * @param selectType - Retrieve roles for this RoleSelectType, e.g.,
	 *            particular EOR or Worker's comp setting. May not be null.
	 * @param orderBy - optional SQL ordering phrase; may be null
	 * @return a non-null (but possibly empty) set of Role`s matching the given
	 *         criteria.
	 */
	@SuppressWarnings("unchecked")
	private List<Role> findByProdDeptSelectType(Production prod, Department dept, RoleSelectType selectType, String orderBy) {
		String queryString = "from Role where " +
				" production.id in ( ? , ? ) and " +
				" department = ? and (roleSelectType = '" + selectType.name() + "' ";

		queryString += ")";

		if(orderBy != null) {
			queryString += " order by " + orderBy;
		}

		Object[] values = {prod.getId(), Constants.SYSTEM_PRODUCTION_ID, dept };
		return find(queryString, values);
	}

	@SuppressWarnings("unchecked")
	public List<Role> findByProdDept(Production prod, Department dept) {
		String queryString = "from Role where " +
				" production.id in ( ? , ? ) and " +
				" department = ? " +
				" order by name ";
		Object[] values = {prod.getId(), Constants.SYSTEM_PRODUCTION_ID, dept};
	return find(queryString, values);
}

//	/**
//	 * Returns a List of all Role objects for "crew" occupations. This is all
//	 * Roles excluding cast (roles in a specific range of role ids) and also
//	 * excluding LS Admin and LS Data Admin roles (RoleGroup's 1 and 2).
//	 * Used by timecard code.
//	 *
//	 * @return Non-null List of crew Roles sorted by Role.name.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<Role> findAllCrewRoles() {
//		String queryString =
//				"from Role role where " + NOT_CAST_STUNT_ROLE +
//				" and role.roleGroup.id > 2 order by role.name";
//		return find(queryString);
//	}

	public int findMaxPriority(Production prod, Department dept) {
		String queryString = "select max(listPriority) from Role where " +
				" production = ? and " +
				" department = ? ";
		Object[] values = {prod, dept};
		return findCount(queryString, values).intValue();
	}

	/**
	 * Determine if the given role-name exists either in the specified
	 * Production, or in the SYSTEM Production, within the given Department.
	 *
	 * @param roleName The role name of interest; note that this is NOT
	 *            case-sensitive. (The default SQL comparison is not case
	 *            sensitive.)
	 * @param prod The Production to be checked for the role, in addition to the
	 *            SYSTEM Production.
	 * @param dept The Department to be checked.
	 * @return True iff a Role already exists in either the SYSTEM Production,
	 *         or the given Production, in the Department specified, with a name
	 *         equal to the given 'roleName'.
	 */
	public boolean existsRole(String roleName, Production prod, Department dept) {
		String queryString = "select count(id) from Role where " +
				" production.id in ( ? , ? ) and " +
				" department = ? and " +
				" name = ? ";
		Object[] values = {prod.getId(), Constants.SYSTEM_PRODUCTION_ID, dept, roleName};
		return findCount(queryString, values) > 0;
	}

	/**
	 * Find a Role within a specific department, given the department's name and
	 * the Role name. The Role may be either a system-wide (LightSPEED-defined)
	 * role, or a custom Role within the specified Production.
	 *
	 * @param prod The Production to search for custom roles (in addition to
	 *            system-wide roles).
	 * @param deptName The name of the Department in which the Role must be
	 *            defined.
	 * @param roleName The name of the Role to be found.
	 * @return The Role matching the search criteria, or null if none is found.
	 *         Since business logic prevents the same Role name appearing twice
	 *         in the same department, the search should never find more than
	 *         one matching entry.
	 */
	public Role findByProdDeptAndNameCustom(Production prod, String deptName, String roleName) {
		String queryString = "from Role where " +
				" production.id in ( ? , ? ) and " +
				" department.name = ? and " +
				" name = ? ";
		Object[] values = {prod.getId(), Constants.SYSTEM_PRODUCTION_ID, deptName, roleName};
		Role role = null;
		role = findOne(queryString, values);
		return role;
	}

	/**
	 * Build a List of SelectItem's of Role's that are in the specified
	 * department.
	 *
	 * @param deptId The department's id.
	 * @param header True iff a "header" entry like "select role" should be
	 *            included as the first item in the list.
	 * @return A non-null List of SelectItem entries, where the value is the
	 *         Role object, and the label is the name of the Role.
	 */
	public List<SelectItem> createRoleSelectList(Integer deptId, boolean header) {
		List<SelectItem> list = new ArrayList<>();
		if (deptId == null) {
			deptId = Constants.DEFAULT_DEPARTMENT_ID;
		}
		LOG.debug("dept id=" + deptId);
		Department dept = DepartmentDAO.getInstance().findById(deptId);
		if (dept != null) {
			List<Role> roles = findByProdDept(getProduction(), dept);
			if (header) {
				list.add(new SelectItem( null, Constants.SELECT_HEAD_ROLE));
			}
			boolean add;
			for (Role role : roles) {
				add = false;
				if (role.isFinancialAdmin()) {
					AuthorizationBean bean = AuthorizationBean.getInstance();
					if (bean.isAdmin() || bean.hasPermission(Permission.EDIT_FINANCE_PERMISSIONS)) {
						add = true;
					}
				}
				else if (role.isDataAdmin()) {
					AuthorizationBean bean = AuthorizationBean.getInstance();
					if (bean.isAdmin() || bean.isLsCoord() || bean.isDataAdmin()) {
						add = true;
					}
				}
				else {
					add = true;
				}
				if (add) {
					list.add(new SelectItem(role, role.getName()));
				}
			}
		}
		return list;
	}

	/**
	 * Build a List of SelectItem's of Role.id values that are in the specified
	 * department. LS-1994
	 *
	 * @param prod The related production - the current production, or if in "My
	 *            Starts", the selected (viewed) production.
	 * @param deptId The department's id.
	 * @param header True iff a "header" entry like "select role" should be
	 *            included as the first item in the list.
	 * @param selectType Type of talent contract that we are generating roles
	 *            for. May be null.
	 * @param orderBy - an optional SQL ordering phrase; may be null.
	 *
	 * @return A non-null List of SelectItem entries, where the value is the
	 *         Role.id integer value, and the label is the name of the Role.
	 */
	public List<SelectItem> createRoleIdSelectList(Production prod, Integer deptId, boolean header, RoleSelectType selectType, String orderBy) {
		List<SelectItem> list = new ArrayList<>();
		List<Role> roles = findRoleList(prod, deptId, selectType, orderBy);
		if (roles != null) {
			if (header) {
				list.add(new SelectItem(null, Constants.SELECT_HEAD_ROLE));
			}
			for (Role role : roles) {
				list.add(new SelectItem(role.getId(), role.getName()));
			}
		}
		return list;
	}

	/**
	 * Build a List of SelectItem's of Role's that are in the specified
	 * department.
	 *
	 * @param deptId The department's id.
	 * @param header True iff a "header" entry like "select role" should be
	 *            included as the first item in the list.
	 * @param selectType Type of talent contract that we are generating roles
	 *            for.  May be null.
	 * @param orderBy - an optional SQL ordering phrase; may be null.
	 *
	 * @return A non-null List of SelectItem entries, where the value is the
	 *         Role object, and the label is the name of the Role.
	 */
	public List<SelectItem> createRoleSelectList(Integer deptId, boolean header, RoleSelectType selectType, String orderBy) {
		List<SelectItem> list = new ArrayList<>();
		List<Role> roles = findRoleList(getProduction(), deptId, selectType, orderBy);
		if (roles != null) {
			if (header) {
				list.add(new SelectItem(null, Constants.SELECT_HEAD_ROLE));
			}
			for (Role role : roles) {
				list.add(new SelectItem(role, role.getName()));
			}
		}
		return list;
	}

	/**
	 * Get a list of Role's matching the given parameters.
	 * @param prod The current or viewed Production. LS-1994
	 * @param deptId The Department.id value of the desired department.
	 * @param selectType Restriction selection parameter.
	 * @param orderBy Optional order-by SQL clause; may be null.
	 * @return A List of Role objects, or null if the department id is invalid.
	 */
	private List<Role> findRoleList(Production prod, Integer deptId, RoleSelectType selectType, String orderBy) {
		List<Role> roles = null;
		if (deptId == null) {
			deptId = Constants.DEFAULT_DEPARTMENT_ID;
		}
		//LOG.debug("dept id=" + deptId);
		Department dept = DepartmentDAO.getInstance().findById(deptId);
		if (dept != null) {
			if (selectType != null) {
				roles = findByProdDeptSelectType(prod, dept, selectType, orderBy);
			}
			else {
				roles = findByProdDept(prod, dept);
			}
		}
		return roles;
	}

	/**
	 * Find the Contact who has been assigned the Role with the given name in
	 * the given Unit, or as a production-wide Role.
	 *
	 * @param unit The Unit to search.
	 * @param roleName The role name to search for; see RoleDAO for predefined
	 *            static role names.
	 * @return The Contact assigned to the given role in the given project;
	 *         returns null if no such Contact exists.
	 */
	public static Contact findContactByRole(Unit unit, String roleName) {
		Contact contact = null;
		Role role = RoleDAO.getInstance().findByName(roleName);
		if (role != null) {
			List<Employment> mbrs = EmploymentDAO.getInstance().findByRoleAndUnit(role, unit);
			if (mbrs.size() > 0) {
				contact = mbrs.get(0).getContact();
			}
		}
		else {
			EventUtils.logError("findContactByRole called with unknown Role name=" + roleName);
		}
		return contact;
	}

	// The following is code ported from early US Talent version (3.2) but not currently used.
	// It was used as the basis of the createRoleSelectList() method above.
//	/**
//	 * Build a List of SelectItem's of Role's that are in the specified
//	 * department.
//	 *
//	 * @param deptId The department's id.
//	 * @param header True iff a "header" entry like "select role" should be
//	 *            included as the first item in the list.
//	 * @param productionType
//	 * @param castContractType Type of talent contract that we are generating roles for.
//	 * @param includeRoleOther Whether to include the 'Other' role in the Talent role list
//	 * @param orderBy
//	 *
//	 * @return A non-null List of SelectItem entries, where the value is the
//	 *         Role object, and the label is the name of the Role.
//	 */
//	public List<SelectItem> createRoleSelectList(Integer deptId, boolean header, ProductionType productionType, CastContractType castContractType, boolean includeRoleOther, String orderBy) {
//		List<SelectItem> list = new ArrayList<>();
//		if (deptId == null) {
//			deptId = Constants.DEFAULT_DEPARTMENT_ID;
//		}
//		log.debug("dept id=" + deptId);
//		Department dept = DepartmentDAO.getInstance().findById(deptId);
//		if (dept != null) {
//			List<Role> roles = null;
//			if(productionType != null) {
//				if(productionType == ProductionType.AGENCY_COMMERCIAL) {
//					roles = findByProdCommercialCastDept(getProduction(), dept, castContractType, includeRoleOther, orderBy);
//				}
//			}
//			else {
//				roles = findByProdDept(getProduction(), dept);
//			}
//			if (header) {
//				list.add(new SelectItem( null, Constants.SELECT_HEAD_ROLE));
//			}
//
//			if(roles != null) {
//				boolean add;
//				for (Role role : roles) {
//					add = false;
//					if (role.isFinancialAdmin()) {
//						AuthorizationBean bean = AuthorizationBean.getInstance();
//						if (bean.isAdmin() || bean.hasPermission(Permission.EDIT_FINANCE_PERMISSIONS)) {
//							add = true;
//						}
//					}
//					else if (role.isDataAdmin()) {
//						AuthorizationBean bean = AuthorizationBean.getInstance();
//						if (bean.isAdmin() || bean.isDataAdmin()) {
//							add = true;
//						}
//					}
//					else {
//						add = true;
//					}
//					if (add) {
//						list.add(new SelectItem(role, role.getName()));
//					}
//				}
//			}
//		}
//		return list;
//	}

	/**
	 * Determine if a given Role is a Cast (talent) role. This is based on a
	 * hard-coded range of Role id values for non-custom roles (avoiding
	 * database access), or membership in the Cast department.
	 *
	 * @param role The Role in question.
	 * @return True iff the Role is within the range of id values assigned to
	 *         Cast roles, or if a custom Role is in the Cast Department. See
	 *         the values {@link #TALENT_ROLE_ID_MINIMUM} and
	 *         {@link #TALENT_ROLE_ID_MAXIMUM}.
	 */
	public static boolean isCast(Role role) {
		int id = role.getId();
		boolean b = (id >= TALENT_ROLE_ID_MINIMUM && id <= TALENT_ROLE_ID_MAXIMUM);
		if ((! b) && role.getId() > MAXIMUM_SYSTEM_ROLE_ID ) {
			try {
				Integer departmentId = role.getDepartment().getId();
				b = departmentId.equals(Constants.DEPARTMENT_ID_CAST);
			}
			catch (Exception e) {
				EventUtils.logError("Unable to fetch department id for Role", e);
			}

		}
		return b;
	}

	/**
	 * Determine if a given Role is a Stunt role. This is based on hard-coded
	 * ranges of Role id values for non-custom roles (avoiding database access),
	 * or membership in the Stunt department for custom roles.
	 *
	 * @param role The Role in question.
	 * @return True iff the Role is within the range of id values assigned to
	 *         Stunt roles, or if the Role is assigned to the Stunts department.
	 *         See the values {@link #STUNT_CAST_ROLE_ID_MINIMUM} and
	 *         {@link #STUNT_CAST_ROLE_ID_MAXIMUM}.
	 */
	public static boolean isStunt(Role role) {
		int id = role.getId();
		boolean b = (id >= STUNT_CAST_ROLE_ID_MINIMUM && id <= STUNT_CAST_ROLE_ID_MAXIMUM);
		if ((! b) && role.getId() > MAXIMUM_SYSTEM_ROLE_ID ) {
			try {
				Integer departmentId = role.getDepartment().getId();
				b = departmentId.equals(Constants.DEPARTMENT_ID_STUNTS);
			}
			catch (Exception e) {
				EventUtils.logError("Unable to fetch department id for Role", e);
			}
		}
		return b;
	}

	/**
	 * Create and save a new custom Role in the given department, within the
	 * current Production.
	 *
	 * @param roleName The name to give the new Role.
	 * @param department The Department the Role is to be associated with.
	 * @return The new Role instance, which has been persisted in the database.
	 */
	public Role addRole(String roleName, Department department) {
		Role newRole = new Role();
		newRole.setProduction(getProduction());
		newRole.setName(roleName);
		RoleGroup roleGroup = RoleGroupDAO.getInstance().findById(Constants.ROLE_GROUP_ID_CUSTOM_ROLES);
		newRole.setRoleGroup(roleGroup);
		newRole.setDepartment(department);
		int priority = findMaxPriority(getProduction(), department);
		newRole.setListPriority(priority + 1);
		save(newRole);
		return newRole;
	}

	public Production getProduction() {
		Production production = SessionUtils.getCurrentOrViewedProduction();
		return production;
	}
//	/**
//	 * Determine if a give Role is a Crew (non-talent) role. This is based on a
//	 * hard-coded range of *CAST* Role id values! (This avoids database access.)
//	 *
//	 * @param role The Role in question.
//	 * @return True iff the Role is NOT a Cast value.  See {@link #isCast(Role)}.
//	 */
//	public static boolean isCrew(Role role) {
//		return !isCast(role);
//	}

//	public static RoleDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (RoleDAO) ctx.getBean("RoleDAO");
//	}

}
