package com.lightspeedeps.dao;

import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.DeptCall;

/**
 * A data access object (DAO) providing persistence and search support for
 * DeptCall entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.DeptCall
 */

public class DeptCallDAO extends BaseTypeDAO<DeptCall> {

	// property constants
//	private static final String DEPT_NAME = "deptName";

	public static DeptCallDAO getInstance() {
		return (DeptCallDAO)getInstance("DeptCallDAO");
	}

	/**
	 * Determine if any DeptCall`s reference the given Department.
	 *
	 * @param dept
	 * @return True iff at least one DeptCall references the specified
	 *         Department., which really means that at least one Callsheet
	 *         references the Department, since all DeptCall instances are
	 *         children of some Callsheet.
	 */
	public boolean referencesDept(Department dept) {
		String query = "select count(id) from DeptCall where department = ?";
		return findCount(query, dept) > 0;
	}

//	public static DeptCallDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (DeptCallDAO) ctx.getBean("DeptCallDAO");
//	}

}