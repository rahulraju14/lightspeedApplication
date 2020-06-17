package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Role;
import com.lightspeedeps.object.BitMask;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Department entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Department
 */

public class DepartmentDAO extends BaseTypeDAO<Department> {
	private static final Log LOG = LogFactory.getLog(DepartmentDAO.class);

	// property constants
//	private static final String NAME = "name";
	public static final String PRODUCTION = "production";
	public static final String PROJECT = "project";
	public static final String TIME_KEEPER = "timeKeeper";
//	private static final String LIST_PRIORITY = "listPriority";

	public static DepartmentDAO getInstance() {
		return (DepartmentDAO)getInstance("DepartmentDAO");
	}

	/**
	 * Find all Department`s that are standard -- included in the application --
	 * and not specific to one Production.
	 *
	 * @return A non-empty List of all standard Department objects.
	 */
	@SuppressWarnings("unchecked")
	public List<Department> findAllStandard() {
		String query = "from Department where " +
				"production.id = " + Constants.SYSTEM_PRODUCTION_ID +
				" order by name ";
		return find(query);
	}

	/**
	 * Find either a custom Department that matches the given standard
	 * Department id, or, if that doesn't exist, then find the matching standard
	 * Department.
	 *
	 * @param prod The Production to which the custom department should belong.
	 * @param project The Project to which the custom department should belong,
	 *            or null if this parameter should be ignored. Only used for
	 *            Commercial productions.
	 * @param deptId The standard Department database id.
	 * @return The custom department for the given Production which is a
	 *         substitute for the standard (System) Department matching the
	 *         given id, or, if that doesn't exist, then the standard Department
	 *         itself. This should not return null if a valid standard
	 *         Department id is given as the parameter.
	 */
	public Department findByProductionStandardId(Production prod, Project project, Integer deptId) {
		Department dept = null;
		String query = "from Department where " +
				" (production.id = ? and standardDeptId = ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getId());
		valueList.add(deptId);

		if (project != null) {
			valueList.add(project);
			query += " and " + PROJECT + " = ? ";
		}
		query += " ) or " +
				" (id = ? and standardDeptId is null) order by id desc ";
		valueList.add(deptId);

		dept = findOne(query, valueList.toArray());
		return dept;
	}

	/**
	 * Find either a custom Department that matches the given standard
	 * Department id, or, if that doesn't exist, then find the matching standard
	 * Department, and return the "showOnDpr" flag.
	 *
	 * @param prod The Production to which the custom department should belong.
	 * @param project The Project to which the custom department should belong,
	 *            or null if this parameter should be ignored. Only used for
	 *            Commercial productions.
	 * @param deptId The standard Department database id.
	 * @return The custom department for the given Production which is a
	 *         substitute for the standard (System) Department matching the
	 *         given id, or, if that doesn't exist, then the standard Department
	 *         itself. This should not return null if a valid standard
	 *         Department id is given as the parameter.
	 */
	public boolean findOnDprByProductionStandardId(Production prod, Project project, Integer deptId) {
		boolean onDpr = false;
		String query = "select showOnDpr from Department where " +
				" (production.id = ? and standardDeptId = ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getId());
		valueList.add(deptId);

		if (project != null) {
			valueList.add(project);
			query += " and " + PROJECT + " = ? ";
		}
		query += " ) or " +
				" (id = ? and standardDeptId is null) order by id desc ";
		valueList.add(deptId);

		Boolean flag = (Boolean)((BaseDAO)this).findOne(query, valueList.toArray());
		if (flag != null) {
			onDpr = flag;
		}
		else { // no custom department related to the given System department
			Department dept = findById(deptId); // so get the System one.
			onDpr = dept.getShowOnDpr();
		}
		return onDpr;
	}

	/**
	 * Find the Department that matches the given Production and department
	 * name, and optional Project. This will not return a standard department
	 * (defined in the SYSTEM Production). This is mostly used for the timecard
	 * system, to find timeKeepers or department approvers.
	 *
	 * @param prod The production of interest
	 * @param project The project of interest; if null, this parameter is
	 *            ignored. Only used for Commercial productions.
	 * @param name The name of the department to match
	 * @return The matching Department, or null if not found.
	 */
	public Department findByProductionProjectDept(Production prod, Project project, String name) {
		String queryString = "from Department where " +
				PRODUCTION + " = ? and  name = ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod);
		valueList.add(name);

		if (project != null) {
			valueList.add(project);
			queryString += " and " + PROJECT + " = ? ";
		}

		return findOne(queryString, valueList.toArray());
	}

	/**
	 * Find a Department by Production and Name, either a custom one, if it
	 * exists, or the SYSTEM one of the given name.
	 *
	 * @param prod The Production of interest, which may or may not have a
	 *            custom Department with the specified name.
	 * @param project The Project whose Department`s should be included; if
	 *            null, this parameter is ignored. Only used for Commercial
	 *            productions.
	 * @param name The Department.name field of interest.
	 * @return null if no match is found.
	 */
	public Department findByProductionAndNameAny(Production prod, Project project, String name) {
		List<Object> valueList = new ArrayList<>();
		valueList.add(name);
		valueList.add(prod);

		String project1 = "";
		if (project != null) {
			project1 = " and project = ? ";
			valueList.add(project);
		}

		String query = "from Department where name = ? " +
				" and ( (production = ? " + project1 + " ) " +
					" or production.id = " + Constants.SYSTEM_PRODUCTION_ID + ") " +
				" order by id desc ";

		return findOne(query,valueList.toArray());
	}

	/**
	 * Find all the Department`s that are in either the given Production (and
	 * optional Project), or in the SYSTEM Production. SYSTEM Department`s that
	 * are the equivalent of custom ones in the List will be excluded, so the
	 * list will only have distinct entries. Departments with a listPriority of
	 * zero are excluded, as they should not appear in any list (currently only
	 * the Cast department).
	 *
	 * @param prod The Production whose Department`s should be included.
	 * @param project The Project whose Department`s should be included; if
	 *            null, this parameter is ignored. Only used for Commercial
	 *            productions.
	 * @return A non-empty List, since it will include all SYSTEM Department`s
	 *         or their equivalent custom replacements, plus any additional
	 *         custom Department`s defined for the Production given.
	 */
	public List<Department> findByProductionProjectComplete(Production prod, Project project) {

		return findByProductionProjectComplete(prod, project, false, false, "");
	}

	/**
	 * Find all the Department`s that are in either the given Production (and
	 * optional Project), or in the SYSTEM Production. SYSTEM Department`s that
	 * are the equivalent of custom ones in the List will be excluded, so the
	 * list will only have distinct entries. Departments with a listPriority of
	 * zero are excluded, as they should not appear in any list (currently only
	 * the Cast department).
	 *
	 * @param prod The Production whose Department`s should be included.
	 * @param project The Project whose Department`s should be included; if
	 *            null, this parameter is ignored. Only used for Commercial
	 *            productions.
	 * @param onCallSheet If true, restrict the returned list to departments
	 *            that have the showOnCallsheet field set to true.
	 * @param onDpr If true, restrict the returned list to departments that have
	 *            the showOnDpr field set to true.
	 * @param orderBy An SQL "order by" clause to be added to the query to
	 *            control the order of the departments in the returned List.
	 * @return A non-empty List, since it will include all SYSTEM Department`s
	 *         or their equivalent custom replacements, plus any additional
	 *         custom Department`s defined for the Production given.
	 */
	@SuppressWarnings("unchecked")
	private List<Department> findByProductionProjectComplete(Production prod, Project project,
			boolean onCallSheet, boolean onDpr, String orderBy ) {

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getId());

		String callSheet = "";
		String dpr = "";
		String project1 = "";
		String project2 = "";

		if (onCallSheet) {
			callSheet = " and ( showOnCallsheet = true ) ";
		}

		if (onDpr) {
			dpr = " and ( showOnDpr = true )";
		}

		if (project != null) {
			project1 = " and project.id = ? ";
			valueList.add(project.getId());
		}

		valueList.add(Constants.SYSTEM_PRODUCTION_ID);
		valueList.add(prod.getId());

		if (project != null) {
			project2 = " and d2.project.id = ? ";
			valueList.add(project.getId());
		}

		String query = "select d1 from Department d1 where " +
				" listPriority <> 0 " +
				callSheet +
				dpr +
				" and ( (production.id = ? " + project1 + " ) or " +
				" (production.id = ? and id not in " +
				"	(select standardDeptId from Department d2 where " +
				"    d2.standardDeptId is not null and d2.production.id = ? " + project2 + " )" +
				"  ) ) " + orderBy;

		return find(query, valueList.toArray());
	}

	/**
	 * Find all the Department`s that are in either the given Production (and
	 * optional Project), or in the SYSTEM Production, and return them as a
	 * SelectItem list. SYSTEM Department`s that are the equivalent of custom
	 * ones in the List will be excluded, so the list will only have distinct
	 * entries. Departments with a listPriority of zero are excluded, as they
	 * should not appear in any list (currently only the Cast department). This
	 * list is also filtered based on the provided department mask.
	 *
	 * @param prod The Production whose Department`s should be included.
	 * @param project The Project whose Department`s should be included; if
	 *            null, this parameter is ignored. Only used for Commercial
	 *            productions.
	 * @param deptMask A BitMask indicating which departments should be
	 *            included.
	 * @return A non-empty List, since it will include all SYSTEM Department`s
	 *         or their equivalent custom replacements, plus any additional
	 *         custom Department`s defined for the Production given.
	 */
	public List<SelectItem> findByProductionCompleteSelect(Production prod, Project project, BitMask deptMask) {
		List<Department> depts = findByProductionProjectComplete(prod, project, false, false, " order by name ");
		List<SelectItem> list = new  ArrayList<>();
		for (Department dept : depts) {
			if (deptMask.intersects(dept.getMask())) {
				list.add(new SelectItem(dept.getId(), dept.getName()));
			}
		}
		return list;
	}

	/**
	 * Find all the Department`s that are in either the given Production (and
	 * optional Project), or in the SYSTEM Production. SYSTEM Department`s that
	 * are the equivalent of custom ones in the List will be excluded, so the
	 * list will only have distinct entries. Departments must have the
	 * 'showOnCallsheet' flag set true to be included.
	 *
	 * @param prod The Production whose Department`s should be included.
	 * @param project The Project whose Department`s should be included; if
	 *            null, this parameter is ignored. Only used for Commercial
	 *            productions.
	 * @return A non-empty List, since it will include all SYSTEM Department`s
	 *         or their equivalent custom replacements, plus any additional
	 *         custom Department`s defined for the Production given. The List
	 *         will be in 'listPriority' order.
	 */
	public List<Department> findByProductionCompleteCallsheet(Production prod, Project project) {

		return findByProductionProjectComplete(prod, project, true, false, " order by listPriority ");
	}

	/**
	 * Find all the Department`s that are in either the given Production (and
	 * optional Project), or in the SYSTEM Production. SYSTEM Department`s that
	 * are the equivalent of custom ones in the List will be excluded, so the
	 * list will only have distinct entries. Departments must have the
	 * 'showOnDpr' flag set true to be included.
	 *
	 * @param prod The Production whose Department`s should be included.
	 * @param project The Project whose Department`s should be included; if
	 *            null, this parameter is ignored. Only used for Commercial
	 *            productions.
	 * @return A non-empty List, since it will include all SYSTEM Department`s
	 *         or their equivalent custom replacements, plus any additional
	 *         custom Department`s defined for the Production given.
	 */
	public List<Department> findByProductionCompleteDpr(Production prod, Project project) {
		return findByProductionProjectComplete(prod, project, false, true, "");
	}

	/**
	 * Find the Department`s that are custom to this Production. This excludes
	 * ones that are simply substitutes for standard departments, created for
	 * the purpose of managing approvals or re-ordering.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project whose Department`s should be included; if
	 *            null, this parameter is ignored. Only used for Commercial
	 *            productions.
	 * @return A non-null, but possibly empty, List of Departments.
	 */
	@SuppressWarnings("unchecked")
	public List<Department> findByProductionCustom(Production prod, Project project) {
		String query = "from Department where production = ? " +
				" and standardDeptId is null ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod);

		if (project != null) {
			query += " and project = ? ";
			valueList.add(project);
		}

		return find(query, valueList.toArray());
	}

	/**
	 * Find a list of department.id values, where the id's are from our standard
	 * Department entries, but the match of Contact to Department.timeKeeper is
	 * in a custom department object. In particular, this is used by the
	 * Timecard code to find the standard department ids that match custom
	 * departments where the time-keeper is set to a specific Contact.
	 *
	 * @param contact The Contact to match (as a time-keeper) in the custom
	 *            Department.
	 * @param project The Project to match (for Commercial productions), or null
	 *            if the project should be ignored (that is, any project will
	 *            match). For TV/Feature productions this should always be null.
	 * @return A non-null, but possibly empty, list of Integer`s, where each one
	 *         is the Department.id value of a standard Department object.
	 */
	@SuppressWarnings("unchecked")
	public List<Integer> findStdIdsByContact(Contact contact, Project project) {

		// Get department ids for standard departments
		String query = "select dept1.id from Department dept1, Department dept2 where " +
				" dept2." + TIME_KEEPER + " = ? " +
				" and dept1.name = dept2.name " +
				" and dept1.production.id = " + Constants.SYSTEM_PRODUCTION_ID;

		List<Object> valueList = new ArrayList<>();
		valueList.add(contact);

		if (project != null) {
			query += " and dept2.project = ? ";
			valueList.add(project);
		}

		List<Integer> list = find(query, valueList.toArray());

		// Add ids from custom departments
		query = "select dept.id from Department dept where " +
				"dept." + TIME_KEEPER + " = ? " +
				" and dept.standardDeptId is null ";
		if (project != null) {
			query += " and dept.project = ? ";
		}
		List<Integer> list2 = find(query, valueList.toArray());
		list.addAll(list2);

		return list;
	}

	/**
	 * Find a list of Department`s, where the Department`s are our standard
	 * Department entries, but the search is based on a "custom" field -- that
	 * is, one which is only set in customized Department objects and not in
	 * standard Department objects. In particular, this is used by the Timecard
	 * code to find the standard department ids that match custom departments
	 * where the time-keeper is set to a specific Contact.
	 *
	 * @param contact The Contact which must match the Department time-keeper
	 *            values.
	 * @param project The Project the departments should belong to -- applies
	 *            only to Commercial productions.
	 * @return A non-null, but possibly empty, list of standard Department
	 *         objects.
	 */
	@SuppressWarnings("unchecked")
	public List<Department> findStdDeptsByContact(Contact contact, Project project) {
		String query = "select dept1 from Department dept1, Department dept2 where " +
				"dept2." + TIME_KEEPER + " = ? " +
				" and dept1.name = dept2.name " +
				" and dept1.production.id = " + Constants.SYSTEM_PRODUCTION_ID;

		List<Object> valueList = new ArrayList<>();
		valueList.add(contact);

		if (project != null) {
			query += " and dept2.project = ? ";
			valueList.add(project);
		}

		return find(query, valueList.toArray());
	}

	/**
	 * Find the maximum current priority of all Department`s in the given
	 * Production. This will include both standard and custom departments.
	 *
	 * @param prod The Production of interest.
	 * @return The maximum list priority.
	 */
	public int findMaxPriority(Production prod) {
		String queryString = "select max(listPriority) from Department where " +
				" production = ? ";
		return findCount(queryString, prod).intValue();
	}

	/**
	 * Determine the combined mask for all the departments represented within
	 * the given List of ProjectMember`s. Note that "unique" departments --
	 * created by a user -- are not included, as they have mask values of -1
	 * (all bits turned on).
	 *
	 * @param emps The list of ProjectMembers to be used.
	 * @return A single BitMask which is the logical "OR" of all the
	 *         Department.mask values for all the departments which contain a
	 *         Role included in any of the given ProjectMember`s.
	 */
	@SuppressWarnings("unchecked")
	public BitMask findMaskForRoles(List<Employment> emps) {
		String empIds = "0";
		for (Employment emp : emps) {
			// build a list of the ProjectMember.id values
			empIds += "," + emp.getId();
		}
		String query = "select d.maskDb from Department d, Employment emp, Role r " +
				" where " +
				" emp.id in (" + empIds + ") " +
				" and emp.role = r and " +
				" r.department = d and " +
				" d.maskDb <> -1 ";
		List<Long> masks = find(query);
		BitMask mask = new BitMask();
		for (Long lng : masks) {
			mask.or(new BitMask(lng));
		}
		return mask;
	}

	/**
	 * Determine if the given Contact is a time-keeper in any department.
	 *
	 * @param contact The Contact of interest.
	 * @param project The Project of interest. If not null, the contact must be
	 *            a time-keeper in some department for the given Project. If
	 *            null, the contact must be a time-keeper in some department for
	 *            any Project within its Production.
	 * @return True iff the specified contact is a time-keeper in the specified
	 *         project (if not null), or in any project (if null).
	 */
	public boolean existsTimekeeper(Contact contact, Project project) {
		String query = "from Department dept where " +
				TIME_KEEPER + " = ? ";

		// Note that there's no need to add the production to the query, since
		// a given Contact can only be a time-keeper within the Production to
		// the Contact belongs.

		List<Object> valueList = new ArrayList<>();
		valueList.add(contact);

		if (project != null) {
			query += " and project = ? ";
			valueList.add(project);
		}

		return exists(query, valueList.toArray());
	}

	/**
	 * Check to see if a Contact has a role in every Department for which they
	 * are a time-keeper (if any). If there's a Department where they are a
	 * time-keeper but do NOT have a role, then remove them as the time-keeper
	 * of the Department.
	 *
	 * @param contact The Contact to be checked.
	 */
	@Transactional
	public void updateTimeKeeper(Contact contact) {
		try {
			String query = "from Department where timeKeeper = ?";
			@SuppressWarnings("unchecked")
			List<Department> tkDepts = find(query, contact);
			if (tkDepts.size() > 0) {
				// Contact is a time-keeper for one or more departments; note that these
				// will always be "custom" department entries, not system ones -- we never
				// set a time-keeper id in a system Department instance.
				Set<Integer> roleDepts = new HashSet<>();
				// Create set of Department.id values for all departments in which he has a role.
				if (contact.getEmployments() != null) {
					for (Employment emp : contact.getEmployments()) {
						roleDepts.add(emp.getDepartmentId());
					}
				}
				for (Department dept : tkDepts) {
					// Now see if each time-keeping department's id, or the equivalent standard
					// department id, is in the list of departments generated from their roles.
					boolean found;
					if (dept.getStandardDeptId() != null) { // clone of standard Department,
						// the Role will have the standard Department's id...
						found = roleDepts.contains(dept.getStandardDeptId());
					}
					else { // a true custom Department, which will have custom roles,
						// and the Role will contain the actual Department.id value.
						found = roleDepts.contains(dept.getId());
					}
					if (! found) {
						// Contact is time-keeper for 'dept', but has no role in it.
						dept.setTimeKeeper(null);
						attachDirty(dept);
						LOG.debug("contact id#" + contact.getId() + " removed as time-keeper of dept #" + dept.getId());
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

//	/**
//	 * Build a List of SelectItem's of Role's that are in the specified
//	 * department. The value field in each SelectItem will be the Role.id value
//	 * (not the Role object itself).
//	 *
//	 * @param dept The Department whose Role`s will be placed in the List.
//	 * @return A non-null List of SelectItem entries, where the value is the
//	 *         Role.id, and the label is the name of the Role.
//	 */
//	public List<SelectItem> createRoleSelectIdList(Department dept) {
//		List<SelectItem> list;
//		if (dept != null) {
//			list = createRoleSelectList(dept.getId(), false);
//			for (SelectItem item : list) { // replace Role's with Role.id
//				item.setValue(((Role)item.getValue()).getId());
//			}
//		}
//		else {
//			list = new ArrayList<>();
//		}
//		return list;
//	}

	/**
	 * Deletes a custom Department from the database, and any associated
	 * (custom) Role`s.
	 *
	 * @param department
	 */
	@Transactional
	public void remove(Department department) {
		List<Role> roles = RoleDAO.getInstance().findByProperty(RoleDAO.DEPARTMENT, department);
		if (roles != null) {
			for (Role role : roles) {
				delete(role);
			}
		}
		delete(department);
	}

//	public static DepartmentDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (DepartmentDAO) ctx.getBean("DepartmentDAO");
//	}

}
