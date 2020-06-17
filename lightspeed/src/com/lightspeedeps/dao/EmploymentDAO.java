package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.ProductionBatch;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Role;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.type.ApprovableStatusFilter;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.util.app.SessionUtils;

public class EmploymentDAO extends BaseTypeDAO<Employment>{
	private static final Log log = LogFactory.getLog(EmploymentDAO.class);

	private static final String PROJECT = "project";
	private static final String BATCH = "productionBatch";
//	private static final String UNION_LOCAL_NUM = "unionLocalNum";
//	private static final String DEPT_NAME = "deptName";

	public static EmploymentDAO getInstance() {
		return (EmploymentDAO)getInstance("EmploymentDAO");
	}

	/**
	 * Find all Employment`s within a Production, optionally filtered by other
	 * criteria, including batch name, department name, and user account number.
	 *
	 * @param prod The Production of interest.
	 * @param startForm The related StartForm, if any. This may be used for
	 *            testing some of the filtering values, such as union number.
	 * @param project The Project of interest; if null, ignore the Employment's
	 *            Project affiliation.
	 * @param anyBatch If true, then the ProductionBatch to which the Employment
	 *            is assigned is irrelevant. If false, the 'batch' parameter
	 *            will be used.
	 * @param batch The ProductionBatch of interest. If null, and 'anyBatch' is
	 *            false, the Employment must be "un-batched" (not assigned to
	 *            any batch) to be selected.
	 * @param deptName The name of the Department of interest.
	 * @param userAcct The user account number (User.accountNumber) of interest.
	 * @param status The status of the StartForm as complete or incomplete. If
	 *            null or "ALL" is specified, no filtering is done.
	 * @param unionLocalNum
	 * @return Non-null, but possibly empty, List of Employment`s matching the
	 *         above description.
	 */
	@SuppressWarnings("unchecked")
	public List<Employment> findByBatchDepartmentUserAcct(Production prod,
			Project project, boolean anyBatch, ProductionBatch batch, String deptName,
			String userAcct, ApprovableStatusFilter status, String unionLocalNum) {

		List<Object> valueList = new ArrayList<>();
		String query;
		if (unionLocalNum != null || (status != null && status == ApprovableStatusFilter.COMPLETE)) {
			// we will use StartForm in 'where' clauses
			query = "select emp from Employment emp, StartForm sf where sf.employment = emp and ";
		}
		else {
			query = "select emp from Employment emp where ";
		}

		query += " emp.contact.production = ? ";
		valueList.add(prod);

		if (project != null) {
			query += " and emp." + PROJECT + " = ? ";
			valueList.add(project);
		}

		if (deptName != null) {
			query += " and emp.role.department.name = ? ";
			valueList.add(deptName);
		}

		if (userAcct != null) {
			query += " and emp.contact.user.accountNumber = ? ";
			valueList.add(userAcct);
		}

		if (! anyBatch) {
			if (batch == null) {
				query += " and emp." + BATCH + " is null ";
			}
			else {
				query += " and emp." + BATCH + " = ? ";
				valueList.add(batch);
			}
		}

		if (unionLocalNum != null) {
			query += " and sf." + StartFormDAO.UNION_LOCAL_NUM + " = ? ";
			valueList.add(unionLocalNum);
		}

		if (status != null && status == ApprovableStatusFilter.COMPLETE) {
			query += " and sf.socialSecurity is not null "; // partial filtering...
			// SSN is the most likely field to be incomplete!
		}

		List<Employment> list = find(query, valueList.toArray());

		if (status != null && status != ApprovableStatusFilter.ALL) {
			// For status filtering, we need to use transient method on StartForm
			boolean complete;
			for (Iterator<Employment> it = list.iterator(); it.hasNext(); ) {
				Employment emp = it.next();
				complete = false;
				for (StartForm sf : emp.getStartForms()) {
					if (sf != null && sf.getHasRequiredFields()) {
						// consider "employment complete" if any one of its SF's is complete
						complete = true;
						break;
					}
				}
				if ((complete && status == ApprovableStatusFilter.INCOMPLETE) ||
						((!complete) && status == ApprovableStatusFilter.COMPLETE)) {
					// throw out the ones that don't match the requested status
					it.remove();
				}
			}
		}

		return list;
	}

	/**
	 * Instantiate a Employment comparator, used for sorting the list of
	 * Positions (Roles) on the Positions tab.
	 */
	public static Comparator<Employment> getEmpComparator() {
		Comparator<Employment> comparator = new Comparator<Employment>() {
			@Override
			public int compare(Employment one, Employment two) {
				return one.compareTo(two, Employment.SORTKEY_OCCUPATION);
			}
		};
		return comparator;
	}

	/**
	 * @param role
	 * @param unit
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Employment> findByRoleAndUnit(Role role, Unit unit) {
		log.debug("role=" + role.getName() + ", unit=" + unit.getId());
		final String FIND_BY_ROLE_AND_UNIT =
				"select emp from Employment emp, ProjectMember pm where " +
				" emp.role = ? and pm.unit = ? and pm.employment = emp";
		return find(FIND_BY_ROLE_AND_UNIT, new Object[]{role,unit});
	}

	/**
	 *
	 * @param contact
	 * @param project
	 * @param role
	 * @return
	 */
	public Employment findByContactProjectRole(Contact contact, Project project, Role role) {
		log.debug("role=" + role.getName() + ", project=" + project + ", contact=" + contact);

		List<Object>objects = new ArrayList<>();

		objects.add(contact);
		objects.add(role);

		String query =
				"select emp from Employment emp where emp.contact = ?" +
				" and emp.role = ?";

		if(project != null) {
			query += " and emp.project = ?";
			objects.add(project);
		}

		return findOne(query, objects.toArray());
	}

	/**
	 * Determine if any Employments exist in a specific Department for the
	 * current Production.
	 *
	 * @param department The Department of interest.
	 * @return True if at least one Employment instance exists associated with
	 *         the current Production and the specified Department.
	 */
	public boolean existsInProductionAndDepartment(Department department) {
		final Object[] values = { SessionUtils.getProduction(), department };
		final String queryString = "from Employment where " +
				" contact.production = ? " +
				" and role.department = ? ";
		return exists(queryString, values);
	}

	/** Method used to fetch the employment records for Transfer tab.
	 * @param sent Transfer status selected in the filter.
	 * @param status Approval status selected in the filter.
	 * @param showAll if true then fetch records for all the projects.
	 * @param selectedFormTypeList
	 * @return List of object array fetched from the database.
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> fetchTransferRecords(Boolean sent, ApprovalStatus status, boolean showAll, List<PayrollFormType> selectedFormTypeList) {
		String queryString = "select e.id, concat(u.lastName,',',u.firstName) as empName, e.occupation, d.name, cd.formType, cd.id, " +
				"  cd.status, cd.timeSent, cd.lastUpdated from Employment e, ContactDocument cd, Role r, Department d, Contact c, User u " +
				"  where cd.employment.id = e.id and e.role.id = r.id and r.department.id = d.id and " +
				"  e.contact.id = c.id and c.user.id = u.id and cd.employment is not null and cd.formType != 'OTHER' " ;

		if (selectedFormTypeList != null && selectedFormTypeList.isEmpty()) {
			// special case - no qualifying types, so return empty list
			return new ArrayList<>();
		}
		if (sent != null) {
			if (sent) {
				queryString += " and cd.timeSent is not null ";
			}
			else if (! sent) {
				queryString += " and cd.timeSent is null ";
			}
		}
		if (status != null) {
			if (status == ApprovalStatus.APPROVED) {
				queryString += " and cd.status in ('APPROVED','LOCKED') ";
			}
			else if (status == ApprovalStatus.SUBMITTED) {
//				queryString += " and cd.approverId is not null ";
				queryString += " and (cd.approverId is not null or cd.status='SUBMITTED_NO_APPROVERS') ";
//				queryString += " and cd.status in ('SUBMITTED','SUBMITTED_NO_APPROVERS','RESUBMITTED','REJECTED','RECALLED') ";
			}
			else if (status == ApprovalStatus.OPEN) {
				queryString += " and cd.approverId is null "; // get docs Rejected to employee or recalled by employee
				queryString += " and cd.status in ('OPEN','REJECTED','RECALLED') ";
			}
			else if (status == ApprovalStatus.PENDING) {
				queryString += " and cd.status ='PENDING' ";
			}
		}
		if (SessionUtils.getNonSystemProduction() != null){
			if (SessionUtils.getNonSystemProduction().getType().isAicp() && (! showAll)) {
				queryString += " and cd.project.id = " + SessionUtils.getCurrentOrViewedProject().getId();
			}
			else {
				queryString += " and c.production.id = " + SessionUtils.getNonSystemProduction().getId();
			}
		}
		if (selectedFormTypeList != null) {
			queryString += " and cd.formType in (:selectedFormTypeList)";
		}
		queryString += " order by cd.employment.id, cd.formType";

		log.debug("QUERY = " + queryString);
		List<Object[]> list;
		if (selectedFormTypeList != null) {
			list = (List<Object[]>) getHibernateTemplate().findByNamedParam(queryString, "selectedFormTypeList", selectedFormTypeList);
		}
		else {
			list = find(queryString);
		}
		log.debug("count=" + (list == null ? "NULL" : list.size()));

		return list;
	}

}
