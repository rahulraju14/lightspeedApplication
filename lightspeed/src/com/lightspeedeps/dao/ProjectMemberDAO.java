//	File Name:	ProjectMemberDAO.java
package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;

import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.ProjectMember;
import com.lightspeedeps.model.Role;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.Permission;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * ProjectMember entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.ProjectMember
 */

public class ProjectMemberDAO extends BaseTypeDAO<ProjectMember> {
	private static final Log log = LogFactory.getLog(ProjectMemberDAO.class);
	// property constants
//	private static final String ROLE = "role";
//	private static final String ASSIGNED = "assigned";

	public static ProjectMemberDAO getInstance() {
		return (ProjectMemberDAO)getInstance("ProjectMemberDAO");
	}

//	public List<ProjectMember> findByRole(Role role) {
//		return findByProperty(ROLE, role);
//	}

//	@SuppressWarnings("unchecked")
//	public List<ProjectMember> findByProjectDistinct(Project project) {
//		log.debug("unit=" + project.getId());
//		try {
//			Object[] values = { project };
//			String queryString =
//				"select distinct pm from ProjectMember pm where pm.unit.project = ?";
//			return find(queryString, values);
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//	}

	/**
	 * Find all ProjectMember`s who have at least one Role in the specified Project.
	 * This includes ProjectMember`s with a production-wide role.
	 *
	 * @return A (possibly empty) List of contacts. Never returns null.
	 */
	@SuppressWarnings("unchecked")
	public List<ProjectMember> findByProject(Project project) {
		Production prod = project.getProduction();

		String queryString = "select distinct pm from ProjectMember pm, Unit u " +
				" where pm.unit = u and u.project = ?";
		List<ProjectMember> pm1 = find(queryString, project);

		queryString = "select distinct pm from ProjectMember pm " +
				" where pm.unit IS NULL and pm.employment.contact.production = ?";
		List<ProjectMember> pm2 = find(queryString, prod);

		pm1.addAll(pm2);
		log.debug("count=" + pm1.size());
		return pm1;

		// Old version, ran very slow (2-4 sec! on production db):
//		Object[] values = { project, prod };
//		final String queryString2 = "select distinct pm from ProjectMember pm, Unit u " +
//				" where ( ( pm.unit = u and u.project = ? ) " +
//					" or (pm.unit IS NULL and pm.employment.contact.production = ? ) ) ";
//		return find(queryString2, values);

	}

//	/**
//	 * Find all Contact`s who have at least one Role in the specified Project.
//	 * This includes Contact`s with a production-wide role.
//	 *
//	 * @return A (possibly empty) List of contacts. Never returns null.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<Contact> findByProjectActiveDistinctContact(Project project) {
//		String queryString = "select distinct pm.employment.contact from ProjectMember pm " +
//				" where ( pm.unit.project = ? " +
//				" or (pm.unit IS NULL and pm.employment.contact.production = ? ) ) " +
//			" and (pm.employment.contact.status in " + MemberStatus.sqlActiveList() + ")" ;
//		return find(queryString, project);
//	}

	/**
	 * Returns a List of distinct Contact`s who belong to the given project. The
	 * usual list of projectMembers (using findByProject()) may contain
	 * duplicate Contact`s if the same Contact is assigned multiple Roles within
	 * a project. This list includes Contact`s who have production-wide roles.
	 * <p>
	 * This method is used by notification routines to avoid sending duplicate
	 * notifications to one user.
	 *
	 * @param project The Project of interest.
	 * @return A non-null, but possibly empty, List of Contact`s.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findByProjectDistinctContact(Project project) {
		log.debug("Project Id: " + project.getId());
		Production prod = project.getProduction();
		Object[] values = { project, prod };
		final String queryString =
				"select distinct pm.employment.contact from ProjectMember pm, Unit u " +
				" where ( pm.unit = u and u.project = ? ) " +
				" or ( pm.unit IS NULL and pm.employment.contact.production = ? ) ";
		return find(queryString, values);
	}

	/**
	 * Returns a List of distinct Contact`s who belong to the given Unit. The
	 * usual list of projectMembers (using findByUnit()) may contain duplicate
	 * Contact`s if the same Contact is assigned multiple Roles within a Unit.
	 * This list includes Contact`s who have production-wide roles.
	 * <p>
	 * This method is used by notification routines to avoid sending duplicate
	 * notifications to one user.
	 *
	 * @param unit The Unit of interest.
	 * @return A non-null, but possibly empty, List of Contact`s.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findByUnitDistinctContact(Unit unit) {
		log.debug("unit=" + unit.getId());
		Object[] values = { unit, unit.getProject().getProduction() };
		final String queryString =
				"select distinct pm.employment.contact from ProjectMember pm " +
				" where pm.unit = ? " +
				" or (pm.unit IS NULL and pm.employment.contact.production = ?)";
		return find(queryString, values);
	}

	/**
	 * Find all Contact`s who have at least one Role in the specified Unit.
	 * Since this is only used when cleaning up after deleting a Unit, the list
	 * does NOT include Contact`s with production-wide roles.
	 *
	 * @return A (possibly empty) List of contacts. Never returns null.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findByUnitExactDistinctContact(Unit unit) {
		final String queryString = "select distinct pm.employment.contact from ProjectMember pm "
				+ " where pm.unit = ? ";
		return find(queryString, unit);
	}

//	/**
//	 * Find all Contact`s who have at least one Role in the specified Unit.
//	 * Since this is used to provide a selection list to choose from, it
//	 * includes Contact`s with production-wide roles.
//	 *
//	 * @return A (possibly empty) List of contacts. Never returns null.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<Contact> findByUnitActiveDistinctContact(Unit unit) {
//		String queryString = "select distinct pm.employment.contact from ProjectMember pm " +
//				" where ( pm.unit = ? " +
//				" or (pm.unit IS NULL and pm.employment.contact.production = ?) ) " +
//				" and (pm.employment.contact.status in " + MemberStatus.sqlActiveList() + " )" ;
//		return find(queryString, unit);
//	}

//	/**
//	 * Find all the ProjectMember`s with the specified Role in the Unit
//	 * specified. NOTE: this does NOT support production-wide Role`s such
//	 * as LS Admin and Prod Data Admin.
//	 *
//	 * @param role The Role of interest.
//	 * @param unit The Unit in which the Role should exist.
//	 * @return A non-null, but possibly empty, List of ProjectMember`s.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<ProjectMember> findByRoleAndUnit(Role role, Unit unit) {
//		log.debug("role=" + role.getName() + ", unit=" + unit.getId());
//		final String FIND_BY_ROLE_AND_UNIT =
//				"from ProjectMember pm where " +
//				" pm.employment.role = ? and pm.unit = ? ";
//		return find(FIND_BY_ROLE_AND_UNIT, new Object[]{role,unit});
//	}

//	@SuppressWarnings("unchecked")
//	public List<ProjectMember> findByRoleNameAndProject(String rolename, Unit unit) {
//		final String FIND_BY_ROLE_NAME_AND_UNIT =
//			"from ProjectMember pm where pm.employment.role.name = ? and pm.unit = ?";
//		List<ProjectMember> list = null;
//		log.debug("role " + rolename + ", unit " + unit.getId());
//		try {
//			list = find(FIND_BY_ROLE_NAME_AND_UNIT, new Object[]{rolename,unit});
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//		return list;
//	}

//	public static ProjectMemberDAO getFromApplicationContext(
//			ApplicationContext ctx) {
//		return (ProjectMemberDAO) ctx.getBean("ProjectMemberDAO");
//	}

	/**
	 * Find all ProjectMember's in the current Production who have a Role that
	 * is within the given Department. Used for Permissions page.  This includes
	 * ProjectMember`s that are for production-wide roles.
	 *
	 * @param department
	 * @return A possibly empty List of ProjectMember's, ordered by
	 *         Role.listPriority.
	 */
	@SuppressWarnings("unchecked")
	public List<ProjectMember> findByProductionAndDepartment(Department department) {
		log.debug("Dept=" + department.getId());
		final Object[] values = { SessionUtils.getProduction(), department };
		final String queryString = "from ProjectMember where " +
				" employment.contact.production = ? " +
				" and employment.role.department = ? order by employment.role.listPriority";
		return find(queryString, values);
	}

	/**
	 * Determine if there is at least one ProjectMember in the current
	 * Production with a Role that is within the given Department. This includes
	 * ProjectMember`s that are for production-wide roles.
	 *
	 * @param department The Department of interest; may be either a custom or a
	 *            pre-defined Department.
	 * @return True iff at least one ProjectMember exists in the current
	 *         Production with a Role associated with the given Department.
	 */
//	public boolean existsInProductionAndDepartment(Department department) {
//		log.debug("Dept=" + department.getId());
//		final Object[] values = { SessionUtils.getProduction(), department };
//		final String queryString = "select count(id) from ProjectMember where " +
//				" contact.production = ? " +
//				" and role.department = ? ";
//		return findCount(queryString, values) > 0;
//	}

	/**
	 * Find all ProjectMember`s with the given Unit, plus ProjectMember`s for
	 * production-wide roles in the given Production.
	 *
	 * @param unit The Unit that the ProjectMember's should belong to.
	 * @param prod The Production for which production-wide ProjectMember
	 *            entries should be included.
	 * @return A non-null, but possibly empty, List of ProjectMember's.
	 */
	@SuppressWarnings("unchecked")
	public List<ProjectMember> findByUnitOrProduction(Unit unit, Production prod) {
		log.debug("Unit Id=" + unit.getId());
		final Object[] values = { unit, prod };
		final String queryString = "from ProjectMember " +
				" where unit = ? " +
				" or (unit is null and employment.contact.production = ? ) ";
		return find(queryString, values);
	}

	/**
	 * Find all ProjectMember`s within the given Unit who have a Role that is
	 * within the given Department. Used for populating the callsheet crew call
	 * section.  This will NOT include ProjectMember`s for production-wide roles.
	 *
	 * @param unit
	 * @param department
	 * @return A non-null, but possibly empty, List of ProjectMember's, ordered
	 *         by Role.listPriority.
	 */
	@SuppressWarnings("unchecked")
	public List<ProjectMember> findByUnitAndDepartment(Unit unit, Department department) {
		log.debug("Unit Id=" + unit.getId() + ", dept=" + department.getId());
		final Object[] values = { unit, department };
		final String queryString = "from ProjectMember where unit = ? " +
				" and employment.role.department = ? order by employment.role.listPriority";
		return find(queryString, values);
	}

	/**
	 * Find all the Contact`s who have any role within the given Department for
	 * the specified Unit. Since this is used to provide a selection list to
	 * choose from, it includes Contact`s with production-wide roles.
	 *
	 * @param dept The Department that must contain the Role the Contact has.
	 * @param unit The Unit in which the Contact must have the Role.
	 * @return A possibly empty list of Contact`s. Will not return null.
	 */
	@SuppressWarnings("unchecked")
	public Collection<Contact> findByUnitAndDepartmentDistinctContact(Unit unit, Department dept) {
		final Object[] values = { dept, unit, unit.getProject().getProduction() };
		final String queryString =
				"select distinct pm.employment.contact from ProjectMember pm " +
				" where  pm.employment.role.department = ? " +
				" and ( pm.unit = ? " +
				" or ( pm.unit IS NULL and pm.employment.contact.production = ? ) ) ";
		return find(queryString, values);
	}

//	@SuppressWarnings("unchecked")
//	public List<ProjectMember> findUserForRole(String roleName , int projectId) {
//		log.debug("finding Project Member instance with Project_Id: " + projectId + " and Role : " + roleName);
//		try {
//			Object[] values = { roleName, projectId };
//			String queryString = "select pm from ProjectMember pm where pm.employment.role.name = ? and pm.unit.project.id =?";
//			return find(queryString, values);
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//	}

	/**
	 *
	 * @param proj
	 * @param dept
	 * @return A non-null, but possibly empty, List of ProjectMember`s that are
	 * in the specified Project, and whose Role is in the specified Department.
	 */
	@SuppressWarnings("unchecked")
	public List<ProjectMember> findByProjectAndDepartment(Project proj, Department dept) {
		final Object[] values = {dept, proj.getProduction(), proj};
		final String queryString =
				"select distinct pm from ProjectMember pm, Unit u " +
				" where pm.employment.role.department = ? " +
				" and ( (pm.unit is null and pm.employment.contact.production = ?) " +
				"   or (pm.unit = u and u.project = ?) )";
		return find(queryString, values);
	}

	/**
	 * Find all ProjectMember entries for the specified User and Project. This
	 * will include production-wide ProjectMembers (those with null Unit).
	 *
	 * @param contact The Contact whose entries are to be found.
	 * @param project The Project that the ProjectMember entries should relate
	 *            to.
	 * @return A non-null, but possibly empty, List of matching ProjectMember`s
	 */
	@SuppressWarnings("unchecked")
	public List<ProjectMember> findByContactAndProject(Contact contact, Project project) {
		if (contact == null || project == null) {
			return new ArrayList<ProjectMember>();
		}
		log.debug("Project: " + project.getId() + " and contact: " + contact.getId());
		final Object[] values = { contact, project };
		final String queryString = "select distinct pm from ProjectMember pm, Unit u " +
				" where pm.employment.contact = ? " +
				" and pm.employment.contact.production.status != 'DELETED' " +
				" and ( (pm.unit is null) or (pm.unit = u and u.project = ?) )";
		return find(queryString, values);
	}

	/**
	 * Find any ProjectMember instance for the given Contact and the given
	 * roleId. Note that there might be more than one that matches the given
	 * criteria; the instance with the lowest database id value will be
	 * returned.
	 *
	 * @param contact The Contact of interest.
	 * @param roleId The Role.id value of the Role of interest.
	 * @return A ProjectMember matching the given parameters, or null if none
	 *         matches.
	 */
	public ProjectMember findByContactAndRoleId(Contact contact, Integer roleId) {
		final Object[] values = { contact, roleId };
		final String queryString = "from ProjectMember pm " +
				" where pm.employment.contact = ? " +
				" and pm.employment.role.id = ? " +
				" order by id ";
		return findOne(queryString, values);
	}

	/**
	 * Determine if a ProjectMember object exists for the specified Contact,
	 * Role, and Unit. The Unit may be null, in which case the entry is for a
	 * production-wide role.
	 *
	 * @param contact The non-null Contact of interest.
	 * @param role The non-null Role of interest.
	 * @param unit The Unit of interest; may be null.
	 * @return True iff a ProjectMember entry is found with the matching
	 *         Contact, Role, and Unit values.
	 */
	public boolean existsContactRoleAndUnit(Contact contact, Role role, Unit unit) {
		if (role == null || contact == null) {
			return false;
		}
		log.debug("unit: " + (unit==null ? "null" : unit.getId()) +
				", role: " + role.getId() + ", contact: " + contact.getId());
		Object[] values;
		String queryString = "select count(id) from ProjectMember " +
				" where employment.contact = ? and employment.role = ? and ";
		if (unit != null) {
			values = new Object[]{ contact, role, unit };
			queryString += " unit = ? ";
		}
		else {
			values = new Object[]{ contact, role };
			queryString += " unit IS NULL ";
		}
		return findCount(queryString, values) > 0;
	}

	/**
	 * Find the ids for all Unit`s of those Project`s in which the given Contact
	 * has the specified Permission. Note that if the Contact has the Permission
	 * in one Unit of a Project, then the ids of all Unit`s in that Project will
	 * be included in the result. If the Contact has a production-wide Role,
	 * then the ids of all Unit`s in the Production will be returned.
	 *
	 * @param contact The Contact of interest.
	 * @param permission The Permission the Contact must have for a Unit to
	 *            qualify.
	 * @return A non-null, but possibly empty, List of distinct Integer`s, where
	 *         each Integer is the database id (Unit.id) of a Unit that is
	 *         part of a Project in which the given Contact has the
	 *         specified Permission.
	 */
	@SuppressWarnings("unchecked")
	public List<Integer> findByContactPermissionDistinctUnits(Contact contact, Permission permission) {
		log.debug("contact=" + contact.getId() + ", perm=" + permission);
		List<Integer> list;
		String queryString;
		if (hasProductionRole(contact)) {
			Production prod = contact.getProduction();
			queryString = "select distinct u.id " +
					" from Unit u " +
					" where u.project.production = ? ";
			list = find(queryString, prod);
		}
		else {
			queryString = "select distinct u.id " +
					" from Unit u " +
					" where u.project.id in " +
					" ( select distinct pm.unit.project.id " +
						" from ProjectMember pm " +
						" where pm.employment.contact.id = :contactid " +
						" and (mod(pm.permissionMask,( :mask * 2 )) / :mask ) >= 1 ) ";
				// Note: Hibernate does not support the bit-wise AND (&) operator,
				// so we do the equivalent by using a combination of mod & division.
			Query query = getHibernateSession().createQuery(queryString)
					.setLong("mask", permission.getMask())
					.setInteger("contactid", contact.getId());
			list = query.list();
		}

		log.debug("cnt=" + list.size());
		return list;
	}

	/**
	 * Find all ProjectMember objects that belong to the given Production. This
	 * will include ones that are production-wide, i.e., have a null Unit.
	 *
	 * @param production The Production of interest.
	 * @return A non-null List; it should also be non-empty, as every Production
	 *         should contain at least one ProjectMember entry (for an LS
	 *         Admin).
	 */
	@SuppressWarnings("unchecked")
	public List<ProjectMember> findByProduction(Production production) {
		final String queryString =
				"from ProjectMember pm where pm.employment.contact.production = ?";
		return find(queryString, production);
	}

	/**
	 * Find all ProjectMember objects that belong to the given Production and
	 * which include the specified Permission in their mask.
	 *
	 * @param prod The Production of interest.
	 * @param permission The Permission the ProjectMember must have to be
	 *            included in the result.
	 * @return A non-null, but possibly empty, List of ProjectMember`s.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findByProductionPermissionDistinctContact(Production prod, Permission permission) {
		log.debug("perm=" + permission);

		String queryString = "select distinct pm.employment.contact " +
				" from ProjectMember pm " +
				" where pm.employment.contact.production = :prod and " +
				" (mod(pm.employment.permissionMask,( :mask * 2 )) / :mask ) >= 1 ";
				//"  (rg.permissionMask & :mask) != 0 ";
		// Note: Hibernate does not support the bit-wise AND (&) operator,
		// so we do the equivalent by using a combination of mod & division.

		Query query = getHibernateSession().createQuery(queryString)
				.setLong("mask", permission.getMask())
				.setEntity("prod", prod);
		List<Contact> list = query.list();
		log.debug("cnt=" + list.size());

		queryString = "select c from Contact c " +
				" where c.production = :prod and " +
				" (mod(c.permissionMask,( :mask * 2 )) / :mask ) >= 1 ";
		query = getHibernateSession().createQuery(queryString)
				.setLong("mask", permission.getMask())
				.setEntity("prod", prod);

		List<Contact> list2 = query.list();
		log.debug("cnt 2=" + list2.size());
		if (list.size() == 0) {
			return list2;
		}
		for (Contact c : list2) {
			if (! list.contains(c)) {
				list.add(c);
			}
		}
		return list;
	}

	/**
	 * Determine if the given Contact is a projectMember of the given project. This
	 * includes the case where a Contact has a production-wide role.
	 *
	 * @param contact The Contact we're looking for.
	 * @param project The Project we want the Contact to be a member of.
	 * @return True iff the contact is a projectMember
	 *         within the specified project or across all projects; false otherwise.
	 */
	public boolean existsContactInProject(Contact contact, Project project) {
		log.debug("is User: " + contact.getId() + " a member of Project: " + project.getId());
		final Object[] values = { contact, project };
		final String queryString = "select count(pm.id) from ProjectMember pm, Unit u " +
				" where pm.employment.contact = ? " +
				" and ( (pm.unit is null) or (pm.unit = u and u.project = ?) )";
		return (findCount(queryString, values) > 0);
	}

	/**
	 * Determine if the given Contact is a projectMember of the given project with a
	 * role in either the Cast or Stunt departments.
	 *
	 * @param contact The Contact we're looking for.
	 * @param project The Project we want the Contact to be a member of.
	 * @return True iff the contact is a projectMember
	 *         within the specified project or across all projects; false otherwise.
	 */
	public boolean existsContactAsCastOrStuntInProject(Contact contact, Project project) {
		log.debug("is User: " + contact.getId() + " a Cast member of Project: " + project.getId());
		final Object[] values = { contact, project };
		final String queryString = "select count(pm.id) from ProjectMember pm " +
				" where pm.employment.contact = ? " +
				" and pm.unit.project = ? " +
				" and pm.employment.role.department.id in ( " +
				Constants.DEPARTMENT_ID_CAST + "," + Constants.DEPARTMENT_ID_STUNTS + ")";
		return (findCount(queryString, values) > 0);
	}

	/**
	 * Determine if the given User is a projectMember of the given project; this
	 * includes being a projectMember of the production with a production-wide
	 * role (such as Prod Data Admin).
	 *
	 * @param user The User we're looking for.
	 * @param project The Project we want the User to be a member of.
	 * @return True iff the User's Contact is a projectMember within the
	 *         specified project or the project's Production; false otherwise.
	 */
	public boolean existsUserInProject(User user, Project project) {
		boolean bRet = true;
		// first check for production-wide roles
		String queryString = "select count(pm.id) from ProjectMember pm " +
				" where pm.employment.contact.user = ? " +
				" and pm.unit IS NULL and pm.employment.contact.production = ? ";
		final Object[] values = { user, project.getProduction() };
		if ( findCount(queryString, values) == 0) {
			// no production wide roles; try project-specific roles:
			final Object[] values2 = { user, project };
			queryString = "select count(pm.id) from ProjectMember pm, Unit u " +
					" where pm.employment.contact.user = ? " +
					" and ( pm.unit = u and u.project = ? ) ";
			bRet = (findCount(queryString, values2) > 0);
		}

		return bRet;
	}

	/**
	 * See if any other Contact, besides the one given, has the role of Prod
	 * Data Admin in the given Production.
	 *
	 * @param contact The Contact to ignore when looking for Prod Data Admin
	 *            roles.
	 * @param production The Production to search for Contacts with Prod Data
	 *            Admin role.
	 * @return True iff at least one other Contact (besides the one given) has
	 *         the role of Prod Data Admin within any project or unit of the
	 *         given Production. The Contact must be in ACCEPTED status.
	 */
	public boolean existsOtherProdAdmin(Contact contact, Production production) {
		final Object[] values = { contact, production };
		final String queryString = "select count(pm.id) from ProjectMember pm " +
				" where employment.role.id = " + Constants.ROLE_ID_DATA_ADMIN +
				" and employment.contact <> ? " +
				" and employment.contact.status = 'ACCEPTED' " +
				" and employment.contact.production = ? ";
		return (findCount(queryString, values) > 0);
	}

	/**
	 * See if any other Contact, besides the one given, has the role of
	 * Financial Data Admin in the given Production.
	 *
	 * @param contact The Contact to ignore when looking for Financial Data
	 *            Admin roles.
	 * @param production The Production to search for Contacts with Financial
	 *            Data Admin role.
	 * @return True iff at least one other Contact (besides the one given) has
	 *         the role of Financial Data Admin within any project or unit of
	 *         the given Production. The Contact must be in ACCEPTED status.
	 */
	public boolean existsOtherFinancialAdmin(Contact contact, Production production) {
		final Object[] values = { contact, production };
		final String queryString = "select count(pm.id) from ProjectMember pm " +
				" where employment.role.id = " + Constants.ROLE_ID_FINANCIAL_ADMIN +
				" and employment.contact <> ? " +
				" and employment.contact.status = 'ACCEPTED' " +
				" and employment.contact.production = ? ";
		return (findCount(queryString, values) > 0);
	}

	/**
	 * Determine if the given Contact is an admin of its Production -- either LS
	 * Admin, or Prod Data Admin.
	 *
	 * @param contact The Contact of interest.
	 * @return True iff the Contact has at least one Role within its
	 *         Production which is either LS Admin or Prod Data Admin. The role
	 *         may be in any Project and Unit of that Production.
	 */
	public boolean isContactProductionAdmin(Contact contact) {
		if (contact == null) {
			return false;
		}
		log.debug("contact: " + contact.getId());
		final String queryString = "select count(id) from ProjectMember pm" +
				" where pm.employment.contact = ? " +
				" and pm.employment.role.id in (" + Constants.ROLE_ID_DATA_ADMIN + "," +
				Constants.ROLE_ID_LS_ADMIN + ") ";
		return (findCount(queryString, contact) > 0);
	}

	/**
	 * Determine if a Contact has a role that is production-wide, i.e., not
	 * specific to one Unit.
	 *
	 * @param contact The Contact of interest.
	 * @return True iff the given Contact has a production-wide role, e.g., LS
	 *         Admin.
	 */
	public boolean hasProductionRole(Contact contact) {
		if (contact == null) {
			return false;
		}
		log.debug("contact: " + contact.getId());
		final String queryString = "from ProjectMember " +
				" where employment.contact = ? and unit IS NULL ";
		return exists(queryString, contact);
	}

//	/**
//	 * Find the number of ProjectMember`s assigned to the given Project.
//	 */
//	@SuppressWarnings("unchecked")
//	public int findMemberCount(Project project) {
//		log.debug("project: " + project.getId());
//		try {
//			Object[] values = { project };
//			String queryString = "select count(*) from ProjectMember where unit.project = ? ";
//			List<Long> list = find(queryString, values);
//			return list.get(0).intValue();
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//	}

	/**
	 * Find the number of ProjectMember`s assigned to the given Unit. Note that this
	 * includes ProjectMembers that are production-wide (which have a null Unit).
	 */
	public int findMemberCount(Unit unit) {
		final String queryString = "select count(id) from ProjectMember " +
				" where unit IS NULL or unit = ? ";
		return findCount(queryString, unit).intValue();
	}

	/**
	 * Create a new ProjectMember object with the given information, and add it
	 * to the database.
	 * @param contact
	 * @param project
	 * @param roleId
	 * @return A non-null List containing the single ProjectMember created, or empty
	 * if it could not be created.
	 */
	public List<ProjectMember> createProjectMember(Contact contact, Project project, int roleId) {
		log.debug("");
		List<ProjectMember> list = new ArrayList<ProjectMember>();
		try {
			if (project != null && contact != null) {
				Unit unit = project.getMainUnit();
				Role role = RoleDAO.getInstance().findById(roleId);
				if (unit != null && role != null) {
					Employment employment = new Employment(role, role.getName(), contact, role.getRoleGroup().getPermissionMask());
					ProjectMember pm = new ProjectMember(unit, employment);
					save(pm);
					list.add(pm);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return list;
	}

	/**
	 * Instantiate a ProjectMember comparator, used for sorting the list of
	 * Positions (Roles) on the Positions tab.
	 */
	public static Comparator<ProjectMember> getPmComparator() {
		Comparator<ProjectMember> comparator = new Comparator<ProjectMember>() {
			@Override
			public int compare(ProjectMember one, ProjectMember two) {
				return one.compareTo(two, ProjectMember.SORTKEY_ROLELIST);
			}
		};
		return comparator;
	}

}
