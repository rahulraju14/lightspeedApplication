package com.lightspeedeps.util.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.object.BitMask;
import com.lightspeedeps.type.Permission;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.view.View;

/**
 * A class for managing lists of Department objects. These lists are widely used
 * throughout the application. There are a number of static lists that are
 * initialized just once, and contain only those Departments that are
 * permanently defined. (These "standard" Department objects are identified in
 * the database by having a production_id value of 1, that is, they are
 * associated with the SYSTEM production.) These standard lists are only used
 * internally, as input to the addCustomDepartments() or
 * addCustomDepartmentsDL() methods, which take a list of standard departments
 * and adds any custom departments defined in the current Production, and
 * returns this customized list to the caller.
 */
public class DepartmentUtils {
	//@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(DepartmentUtils.class);

	private static final Map<Integer,BitMask> idMasks = new HashMap<>();

	/** A BitMask that has all department mask bits turned on. */
	private static final BitMask allDeptMask = new BitMask();

	/** A list of all standard (LS-defined) Department objects in the
	 * database, sorted in ascending alphabetical order. */
	private static List<Department> departmentList;

	/** A list of SelectItems (for a drop-down list) which includes ALL
	 * departments in the database, sorted in ascending alphabetical order. Only
	 * users with LS Admin roles get to see this list. The value field for each
	 * entry is the Department's id field. */
	private static List<SelectItem> departmentAdminDL;

	/** The department drop-down list which includes all departments
	 * except "LS Admin" -- this one includes the "Data Admin" department.
	 * Users with Data Admin roles get to see this one. */
	private static List<SelectItem> departmentDataAdminDL;

	/** The department drop-down list which includes all departments EXCEPT
	 * "LS Admin" and "Data Admin".  Users with both Cast and Crew editing
	 * permissions get to see this one. */
	private static List<SelectItem> departmentCastCrewDL;

	/** A list of SelectItems (drop-down list) containing only the Cast
	 * Department. Users with only Cast editing permissions get to see this one.
	 * That is, users who only have edit_cast permission and not edit_crew
	 * permission. The value field for each entry is the Department's id field. */
	private static List<SelectItem> departmentOnlyCastDL;

	/** The department drop-down list which includes all departments EXCEPT
	 * "LS Admin" and "Cast".  This is used in most places, for most users. */
	private static List<SelectItem> departmentCrewDL;

	/** The department drop-down list which includes ONLY Non-production
	 * department ("Vendors").  This is used for a few specific roles who
	 * are allowed to create and edit only non-production contacts. */
	private static List<SelectItem> departmentNonProdDL;

	/**
	 * The following ...Ids maps are used to map a Department Id to a Boolean
	 * True if the particular department includes roles of that type (cast,
	 * crew, etc.)
	 *
	 * These are used by the getDepartmentIds() method, to construct a custom
	 * Map used in the Cast&Crew | Occupations tab, to restrict which detail lines
	 * are displayed in edit (input) mode versus output-only mode.
	 */

	/** The map that includes all department ids EXCEPT "LS Admin", "Data Admin" and "Cast". */
	private static Map<Integer,Boolean> departmentCrewIds;
	/** The map that includes all department ids EXCEPT "LS Admin" and "Data Admin". */
	private static Map<Integer,Boolean> departmentCastCrewIds;
	/** The map that includes only the Cast department id. */
	private static Map<Integer,Boolean> departmentOnlyCastIds;
	/** The map that includes all department ids. */
	private static Map<Integer,Boolean> departmentAdminIds;
	/** The map that includes all department ids except LS Admin. */
	private static Map<Integer,Boolean> departmentDataAdminIds;
	/** The map that includes only the Vendor department id. */
	private static Map<Integer,Boolean> departmentNonProdIds;

	/**
	 * This static initializer will be run by the JVM the first time any of our
	 * static methods are called, or any static field is referenced.
	 */
	static {
		createDepartmentLists();
	}

	/**
	 * A private constructor, since no instances of this class should ever be
	 * created.
	 */
	private DepartmentUtils() {
	}

	/**
	 * Returns the appropriate List of SelectItems related to Departments, based
	 * on the given Contact's permissions. The list will also be filtered based
	 * on the current Production's (or Project's) department mask.
	 *
	 * @param contact The Contact whose permissions will determine exactly which
	 *            departments are included in the returned list.
	 * @return A non-null, but possibly empty, List.
	 */
	public static List<SelectItem> getDepartmentDL(Contact contact) {
		return getDepartmentDL(contact, false, true);
	}

	/**
	 * Returns the appropriate List of SelectItems related to Departments, based
	 * on the given Contact's permissions. The list may be filtered based on the
	 * current Production's (or Project's) department mask.
	 *
	 * @param contact The Contact whose permissions will determine exactly which
	 *            departments are included in the returned list.
	 * @param skipAdmin If true, Admin-related departments will be omitted,
	 *            regardless of the Contact's permissions.
	 * @param applyMask If true, the list of Department`s returned will be
	 *            filtered by the current Production's (or Project's) department
	 *            mask.
	 * @return A non-null, but possibly empty, List.
	 */
	public static List<SelectItem> getDepartmentDL(Contact contact, boolean skipAdmin, boolean applyMask) {
		Project project = findCommercialProject();
		return getDepartmentDL(contact, project, skipAdmin, applyMask);
	}

	/**
	 * Returns the appropriate List of SelectItems related to Departments, based
	 * on the given Contact's permissions.
	 *
	 * @param contact The Contact whose permissions will determine exactly which
	 *            departments are included in the returned list.
	 * @param project The Project used to filter custom Departments. This should
	 *            only be non-null for a Commercial production.
	 * @param skipAdmin If true, Admin-related departments will be omitted,
	 *            regardless of the Contact's permissions.
	 * @param applyMask If true, the list of Department`s returned will be
	 *            filtered by the current Production's (or Project's) department
	 *            mask.
	 * @return A non-null, but possibly empty, List.
	 */
	public static List<SelectItem> getDepartmentDL(Contact contact, Project project, boolean skipAdmin, boolean applyMask) {
		List<SelectItem> departmentDL;
		AuthorizationBean authBean = AuthorizationBean.getInstance();
		if (! skipAdmin && authBean.hasPermission(contact, contact.getProject(), Permission.WRITE_ANY)) {
			departmentDL = addCustomDepartmentsDL(departmentAdminDL, project, applyMask);
		}
		else if (! skipAdmin &&
				(authBean.isDataAdmin() || authBean.hasPermission(Permission.EDIT_FINANCE_PERMISSIONS))) {
			departmentDL = getDepartmentDataAdminDL(project, applyMask);
		}
		else if (authBean.hasPermission(contact, contact.getProject(), Permission.EDIT_CONTACTS_CAST)) {
			if (authBean.hasPermission(contact, contact.getProject(), Permission.EDIT_CONTACTS_CREW)) {
				departmentDL = getDepartmentCastCrewDL(project, applyMask);
			}
			else {
				departmentDL = departmentOnlyCastDL; // no mask applied - only Cast entry included
			}
		}
		else if (authBean.hasPermission(contact, contact.getProject(), Permission.EDIT_CONTACTS_CREW)) {
			departmentDL = getDepartmentCrewDL(project, applyMask);
		}
		else {
			departmentDL = departmentNonProdDL; // no mask applied - only Vendor entry included
		}
		return departmentDL;
	}

	/**
	 * Returns the appropriate List of SelectItems related to Departments, based
	 * on the given Contact's permissions, and also includes a leading "(Select department)"
	 * entry. This list is NOT filtered by the Production's (or Project's) department mask!
	 *
	 * @param contact The Contact whose permissions will determine exactly which
	 *            departments are included in the returned list.
	 * @return A non-null List
	 */
	public static List<SelectItem> getDepartmentDL2(Contact contact) {
		Project project = findCommercialProject();
		return getDepartmentDL2(contact, project);
	}

	/**
	 * Returns the appropriate List of SelectItems related to Departments, based
	 * on the given Contact's permissions, and also includes a leading "(Select department)"
	 * entry. This list is NOT filtered by the Production's (or Project's) department mask!
	 *
	 * @param contact The Contact whose permissions will determine exactly which
	 *            departments are included in the returned list.
	 * @param project The Project used to filter custom Departments. This should
	 *            only be non-null for a Commercial production.
	 * @return A non-null List
	 */
	public static List<SelectItem> getDepartmentDL2(Contact contact, Project project) {
		List<SelectItem> departmentDL = new ArrayList<>(getDepartmentDL(contact, project, false, false));
		departmentDL.add(0, new SelectItem(new Integer(-1), Constants.SELECT_HEAD_DEPARTMENT));
		return departmentDL;
	}

	/**
	 * Returns the appropriate Map of Department ids, based on the given user's
	 * permissions. The Map returned will contain the department id for all
	 * Departments which contain roles that this user may edit. This is used by
	 * the Contact | Positions tab, to restrict which detail lines are displayed
	 * in edit (input) mode versus output-only mode.
	 *
	 * @param user The User whose permissions will be used to determine the
	 *            contents of the Map returned.
	 * @param project The Project used to filter custom Departments. This should
	 *            only be non-null for a Commercial production.
	 * @return A Map with Department ids as keys. If a Department may be edited
	 *         by the given user, there will be an entry in the map with a value
	 *         of True. Otherwise there will not be an entry in the map.
	 */
//	public static Map<Integer,Boolean> getDepartmentIds(Contact user, Project project) {
//		Map<Integer,Boolean> departmentIds;
//		AuthorizationBean authBean = AuthorizationBean.getInstance();
//		if (authBean.hasPermission(user, user.getProject(), Permission.WRITE_ANY)) {
//			departmentIds = addCustomDepartmentIds(departmentAdminIds, project);
//		}
//		else if (authBean.isDataAdmin() || authBean.hasPermission(Permission.EDIT_FINANCE_PERMISSIONS)) {
//			departmentIds = addCustomDepartmentIds(departmentDataAdminIds, project);
//		}
//		else if (authBean.hasPermission(user, user.getProject(), Permission.EDIT_CONTACTS_CAST)) {
//			if (authBean.hasPermission(user, user.getProject(), Permission.EDIT_CONTACTS_CREW)) {
//				departmentIds = addCustomDepartmentIds(departmentCastCrewIds, project);
//			}
//			else {
//				departmentIds = departmentOnlyCastIds; // no custom depts added; no masking
//			}
//		}
//		else if (authBean.hasPermission(user, user.getProject(), Permission.EDIT_CONTACTS_CREW)) {
//			departmentIds = addCustomDepartmentIds(departmentCrewIds, project);
//		}
//		else {
//			departmentIds = departmentNonProdIds; // no custom depts added; no masking
//		}
//		return departmentIds;
//	}

	/** See {@link #departmentList}. */
	private static List<Department> getStandardDepartmentList() {
		if (departmentList == null) {
			// get sorted list of "standard" departments from database
			departmentList = DepartmentDAO.getInstance().findAllStandard();
		}
		return departmentList;
	}

	/**
	 * Returns a list of all Department objects for the current Production in
	 * the database, sorted in ascending alphabetical order. The list will be
	 * filtered based on the current Production's (or Project's) department
	 * mask.
	 *
	 * @return a non-empty List
	 */
	public static List<Department> getDepartmentList() {
		return addCustomDepartments(getStandardDepartmentList(), true);
	}

	/**
	 * Returns a list of SelectItems containing all the Department objects in
	 * the database except for LS Admin and Cast, sorted in ascending
	 * alphabetical order. The value field for each entry is the Department's id
	 * field. The list will also be filtered based on the current Production's
	 * (or Project's) department mask.
	 *
	 * @return a non-null List
	 */
	public static List<SelectItem> getDepartmentCrewDL() {
		Project project = findCommercialProject();
		return getDepartmentCrewDL(project, true);
	}

	/**
	 * Returns a list of SelectItems containing all the Department objects in
	 * the database except for LS Admin and Cast, sorted in ascending
	 * alphabetical order. The value field for each entry is the Department's id
	 * field.
	 *
	 * @param project The Project used to filter custom Departments. This should
	 *            only be non-null for a Commercial production.
	 * @param applyMask If true, the list of Department`s returned will be
	 *            filtered by the current Production's (or Project's) department
	 *            mask.
	 * @return a non-null List
	 */
	private static List<SelectItem> getDepartmentCrewDL(Project project, boolean applyMask) {
		return addCustomDepartmentsDL(departmentCrewDL, project, applyMask);
	}

	/**
	 * Returns a list of SelectItems containing all the Department objects in
	 * the database except for LS Admin, sorted in ascending alphabetical order.
	 * This includes both Cast and Crew departments. The value field for each
	 * entry is the Department's id field. The list of Department`s returned
	 * will be filtered by the current Production's (or Project's) department
	 * mask.
	 *
	 * @return a non-null List
	 */
	public static List<SelectItem> getDepartmentCastCrewDL() {
		Project project = findCommercialProject();
		return getDepartmentCastCrewDL(project, true);
	}

	/**
	 * Returns a list of SelectItems containing all the Department objects in
	 * the database except for LS Admin, sorted in ascending alphabetical order.
	 * This includes both Cast and Crew departments. The value field for each
	 * entry is the Department's id field.
	 *
	 * @param project The Project used to filter custom Departments. This should
	 *            only be non-null for a Commercial production.
	 * @param applyMask If true, the list of Department`s returned will be
	 *            filtered by the current Production's (or Project's) department
	 *            mask.
	 * @return a non-null List
	 */
	private static List<SelectItem> getDepartmentCastCrewDL(Project project, boolean applyMask) {
		return addCustomDepartmentsDL(departmentCastCrewDL, project, applyMask);
	}

	/**
	 * See {@link #departmentDataAdminDL}.
	 *
	 * The list of Department`s returned will be filtered by the current
	 * Production's (or Project's) department mask.
	 *
	 * @return a non-null List
	 */
	public static List<SelectItem> getDepartmentDataAdminDL() {
		Project project = findCommercialProject();
		return getDepartmentDataAdminDL(project, true);
	}

	/**
	 * See {@link #departmentDataAdminDL}.
	 *
	 * @param project The Project used to filter custom Departments. This should
	 *            only be non-null for a Commercial production.
	 * @param applyMask If true, the list of Department`s returned will be
	 *            filtered by the current Production's (or Project's) department
	 *            mask.
	 *
	 * @return a non-null List
	 */
	public static List<SelectItem> getDepartmentDataAdminDL(Project project, boolean applyMask) {
		return addCustomDepartmentsDL(departmentDataAdminDL, project, applyMask);
	}

	/**
	 * Initialize all of our static Lists and Maps from the database. Invoked by
	 * our static initializer the first time any code in the application uses
	 * this class.
	 */
	private static void createDepartmentLists() {
		departmentCrewDL = new ArrayList<>();
		departmentOnlyCastDL = new ArrayList<>();
		departmentCastCrewDL = new ArrayList<>();
		departmentAdminDL = new ArrayList<>();
		departmentDataAdminDL = new ArrayList<>();
		departmentNonProdDL = new ArrayList<>();

		departmentCrewIds = new HashMap<>();
		departmentOnlyCastIds = new HashMap<>();
		departmentCastCrewIds = new HashMap<>();
		departmentAdminIds = new HashMap<>();
		departmentDataAdminIds = new HashMap<>();
		departmentNonProdIds = new HashMap<>();

		List<Department> deptList = getStandardDepartmentList();
		if (deptList != null) {
			for (Department dept : deptList) {
				departmentAdminDL.add(new SelectItem( dept.getId(), dept.getName() ));
				departmentAdminIds.put(dept.getId(), true);
				if ( ! dept.getId().equals(Constants.DEPARTMENT_ID_LS_ADMIN)) {
					departmentDataAdminDL.add(new SelectItem( dept.getId(), dept.getName() ));
					departmentDataAdminIds.put(dept.getId(), true);
					if ( ! dept.getId().equals(Constants.DEPARTMENT_ID_DATA_ADMIN)) {
						departmentCastCrewDL.add(new SelectItem( dept.getId(), dept.getName() ));
						departmentCastCrewIds.put(dept.getId(), true);
						if ( dept.getId().equals(Constants.DEPARTMENT_ID_CAST)) {
							departmentOnlyCastDL.add(new SelectItem( dept.getId(), dept.getName() ));
							departmentOnlyCastIds.put(dept.getId(), true);
						}
						else {
							departmentCrewDL.add(new SelectItem( dept.getId(), dept.getName() ));
							departmentCrewIds.put(dept.getId(), true);
							if (dept.getId().equals(Constants.DEPARTMENT_ID_VENDOR)) {
								departmentNonProdDL.add(new SelectItem( dept.getId(), dept.getName() ));
								departmentNonProdIds.put(dept.getId(), true);
							}
						}
					}
				}
				// create mask that includes all standard departments
				allDeptMask.or(dept.getMask());
			}
		}
		departmentList = null; // free the memory
	}

	/**
	 * Add Department`s for the current production's custom departments -- NOT
	 * including those that simply replace a SYSTEM department. For a commercial
	 * production, only the current project will be considered.
	 *
	 * @param list The existing list, to which the custom Department`s will be
	 *            added.
	 * @param applyMask If true, the list of Department`s returned will be
	 *            filtered by the current Production's (or Project's) department
	 *            mask.
	 * @return The updated List.
	 */
	private static List<Department> addCustomDepartments(List<Department> list, boolean applyMask) {
		Project project = findCommercialProject();
		List<Department> added = findByProductionCustom(project);
		BitMask active = SessionUtils.getDeptMask();
		if (added.size() > 0 || ! active.equals(allDeptMask)) {
			if (! applyMask) {
				Production prod = SessionUtils.getNonSystemProduction();
				if (prod.getType().isTours() || prod.getType().isCanadaTalent()) {
					applyMask = true; // Tours or Canada Talent productions filter ALL department lists.
				}
			}
			if (applyMask) {
				for (Department dept : list) {
					BitMask deptMask = dept.getMask();
					if (deptMask.intersects(active)) {
						added.add(dept);
					}
				}
			}
			else {
				added.addAll(list);
			}
			Collections.sort(added);
			return added;
		}
		return list;
	}

	/**
	 * Add SelectItem`s for the current production's custom departments -- NOT
	 * including those that simply replace a SYSTEM department.
	 *
	 * @param list The existing list, to which the new SelectItem`s will be
	 *            added.
	 * @param project The Project used to filter custom Departments. This should
	 *            only be non-null for a Commercial production. If null for a
	 *            Commercial production, custom departments from all projects
	 *            will be included.
	 * @param applyMask If true, the list of Department`s returned will be
	 *            filtered by the current Production's (or Project's) department
	 *            mask.
	 * @return The updated List.
	 */
	private static List<SelectItem> addCustomDepartmentsDL(List<SelectItem> list, Project project, boolean applyMask) {
		List<Department> added = findByProductionCustom(project);
		BitMask active = null;
		if (! applyMask) {
			Production prod = SessionUtils.getNonSystemProduction();
			if (prod.getType().isTours() || prod.getType().isCanadaTalent()) {
				applyMask = true; // Tours or Canada Talent productions filter ALL department lists.
			}
		}
		if (applyMask) {
			if (project == null) {
				if (SessionUtils.getNonSystemProduction() != null) {
					active = SessionUtils.getNonSystemProduction().getDeptMaskB();
				}
				else {
					active = allDeptMask;
				}
			}
			else {
				// since project is not null, must be a Commercial production
				active = project.getDeptMask(); // so use project mask. LS-1994
			}
		}
		if (added.size() > 0 ||
				(applyMask && active != null && ! active.equals(allDeptMask))) {
			List<SelectItem> newList = new ArrayList<>();
			if (applyMask) {
				for (SelectItem si : list) {
					BitMask deptMask = findMaskById((Integer)si.getValue());
					if (deptMask!= null && deptMask.intersects(active)) {
						newList.add(si);
					}
				}
			}
			else {
				newList.addAll(list);
			}
			for (Department dept : added) {
				newList.add(new SelectItem(dept.getId(), dept.getName()));
			}
			Collections.sort(newList, View.getSelectItemComparator());
			return newList;
		}
		return list;
	}

	/**
	 * Find the custom departments for the current Production and, optionally,
	 * the given project; then add their id's to the supplied map.
	 *
	 * @param map A mapping of Integer to Boolean, where the Integers are
	 *            Department.id values. The boolean will always be true.
	 * @param project The Project used to filter custom Departments. This should
	 *            only be non-null for a Commercial production. If null for a
	 *            Commercial production, custom departments from all projects
	 *            will be included.
	 * @return An updated Map with Department ids as keys. If a custom
	 *         Department was found, there will be an entry in the map with a
	 *         value of True.
	 */
//	private static Map<Integer,Boolean> addCustomDepartmentIds(Map<Integer,Boolean> map, Project project) {
//		List<Department> added = findByProductionCustom(project);
//		if (added.size() > 0) {
//			Map<Integer,Boolean> newMap = new HashMap<Integer,Boolean>(map);
//			for (Department dept : added) {
//				newMap.put(dept.getId(), true);
//			}
//			return newMap;
//		}
//		return map;
//	}

	/**
	 * Find the custom Department`s for the current Production and/or the given
	 * Project.
	 *
	 * @param project The Project used to filter custom Departments. This should
	 *            only be non-null for a Commercial production. If null for a
	 *            Commercial production, custom departments from all projects
	 *            will be included.
	 * @return a non-null, but possibly empty, List
	 */
	private static List<Department> findByProductionCustom(Project project) {
		Production prod = SessionUtils.getProduction();
		return DepartmentDAO.getInstance().findByProductionCustom(prod, project);
	}

	private static BitMask findMaskById(Integer id) {
		BitMask mask = idMasks.get(id);
		if (mask == null) {
			Department dept = DepartmentDAO.getInstance().findById(id);
			if (dept != null) {
				mask = dept.getMask();
				idMasks.put(id, mask);
			}
		}
		return mask;
	}

	/**
	 * Update the department mask of the current Production or Project with the
	 * given BitMask. For a Commercial production, the current Project is
	 * updated with the given mask, then the masks of all the projects in the
	 * current production are OR'd together, and the production's mask is set to
	 * that value.
	 *
	 * @param activeMask The department BitMask to be assigned.
	 */
	public static void updateMask(BitMask activeMask) {
		Production prod = SessionUtils.getNonSystemProduction();
		BitMask prodMask;
		if (prod .getType().isAicp()) {
			prodMask = new BitMask();
			Project proj = SessionUtils.getCurrentProject();
			if (proj != null) {
				log.debug("Proj. mask=" + proj.getDeptMask() + ", new=" + activeMask);
				proj.setDeptMask(activeMask);
				ProjectDAO.getInstance().attachDirty(proj);
				for (Project p : prod.getProjects()) {
					prodMask.or(p.getDeptMask());
				}
			}
		}
		else {
			prodMask = activeMask;
		}
		log.debug("Prod. mask=" + prod.getDeptMaskB() + ", new=" + prodMask);
		prod.setDeptMaskB(prodMask);
		ProductionDAO.getInstance().attachDirty(prod);
	}

	/**
	 * @return The Project to be used in determining the list of Department
	 *         values. For a TV/Feature production, this is null. For a
	 *         Commercial production, it is the current Project.
	 */
	private static Project findCommercialProject() {
		Production prod = SessionUtils.getNonSystemProduction();
		Project project = null;
		if (prod != null && prod.getType().hasPayrollByProject()) {
			project = SessionUtils.getCurrentProject();
		}
		return project;
	}

}
