package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.ApprovalAnchor;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;

/**
 * A data access object (DAO) providing persistence and search support for
 * ApprovalAnchor entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.ApprovalAnchor
 */

public class ApprovalAnchorDAO extends BaseTypeDAO<ApprovalAnchor> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ApprovalAnchorDAO.class);

	// property constants
	public static final String PRODUCTION = "production";
	private static final String PROJECT = "project";
	private static final String DEPARTMENT = "department";
	private static final String FIRST_APPROVER = "approver";

	public static ApprovalAnchorDAO getInstance() {
		return (ApprovalAnchorDAO)getInstance("ApprovalAnchorDAO");
	}

	/**
	 * Find the ApprovalAnchor for a Department-approver list for the specified
	 * Production, and optionally, Project.
	 *
	 * @param prod The Production to be searched for the approver chain.
	 * @param project The Project to be searched for the approver chain; this
	 *            should be null except when searching for a project-specific
	 *            Department approver chain in a Commercial Production.
	 * @param dept The Department to which the approver chain applies.
	 * @return The matching ApprovalAnchor, or null if no matching entry was found.
	 */
	public ApprovalAnchor findByProductionProjectDept(Production prod, Project project, Department dept) {
		String queryString = "from ApprovalAnchor where " +
				PRODUCTION + " = ? and " +
				DEPARTMENT + " = ? ";

		List<Object> valueList = new ArrayList<Object>();
		valueList.add(prod);
		valueList.add(dept);

		if (project != null) {
			valueList.add(project);
			queryString += " and " + PROJECT + " = ? ";
		}

		return findOne(queryString, valueList.toArray());
	}

	/**
	 * Find the ApprovalAnchor for a Department-approver list for the specified
	 * Production, and optionally, Project.
	 *
	 * @param prod The Production to be searched for the approver chain.
	 * @param project The Project to be searched for the approver chain; this
	 *            should be null except when searching for a project-specific
	 *            Department approver chain in a Commercial Production.
	 * @param dept The Department to which the approver chain applies.
	 * @return The matching ApprovalAnchor, or null if no matching entry was found.
	 */
	public boolean existsApproverForProductionProjectDept(Production prod, Project project, Department dept) {
		String queryString = "from ApprovalAnchor where " +
				PRODUCTION + " = ? and " +
				DEPARTMENT + " = ? and " +
				FIRST_APPROVER + " is not null ";

		List<Object> valueList = new ArrayList<Object>();
		valueList.add(prod);
		valueList.add(dept);

		if (project != null) {
			valueList.add(project);
			queryString += " and " + PROJECT + " = ? ";
		}

		return exists(queryString, valueList.toArray());
	}

	/**
	 * Find all ApprovalAnchor`s for the given Production (and optional Project)
	 * that are anchors for any Department.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, this parameter is
	 *            ignored.
	 * @return A non-null, but possibly empty, List of ApprovalAnchor`s. Each
	 *         one is related to the Production given, and is the anchor for the
	 *         approval list of some Department in the Production.
	 */
	@SuppressWarnings("unchecked")
	public List<ApprovalAnchor> findByProductionProjectDepartmental(Production prod, Project project) {
		String queryString = "from ApprovalAnchor where " +
				PRODUCTION + " = ? and " +
				DEPARTMENT + " is not null ";

		List<Object> valueList = new ArrayList<Object>();
		valueList.add(prod);

		if (project != null) {
			valueList.add(project);
			queryString += " and " + PROJECT + " = ? ";
		}

		return find(queryString, valueList.toArray());
	}

	/**
	 * Determine if any ApprovalAnchor`s reference the given Department.
	 *
	 * @param dept
	 * @return True iff at least one ApprovalAnchor references the specified
	 *         Department.
	 */
	public boolean referencesDept(Department dept) {
		String query = "from ApprovalAnchor where department = ?";
		return exists(query, dept);
	}

//	/**
//	 * Find all ApprovalAnchor`s for the given Production that match any of the
//	 * given Department`s.
//	 *
//	 * @param prod The Production of interest.
//	 * @param depts A Collection of Department`s.
//	 * @return A non-null, but possibly empty, List of ApprovalAnchor`s. Each
//	 *         one is related to the Production given, and is the anchor for the
//	 *         approval list of one of the Department`s provided in the
//	 *         collection.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<ApprovalAnchor> findByProductionDepartments(Production prod, Collection<Department> depts) {
//		String ids = "";
//		for (Department dept : depts) {
//			ids += "," + dept.getId();
//		}
//		ids = ids.substring(1);
//		String queryString = "from ApprovalAnchor where " +
//				PRODUCTION + " = ? and " +
//				"department.id in ( " + ids + ") ";
//		return find(queryString, prod);
//	}

//	public static ApprovalAnchorDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ApprovalAnchorDAO)ctx.getBean("ApprovalAnchorDAO");
//	}

}