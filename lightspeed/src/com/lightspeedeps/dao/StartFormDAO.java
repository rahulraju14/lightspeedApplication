package com.lightspeedeps.dao;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.*;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.type.Permission;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.login.AuthorizationBean;

/**
 * A data access object (DAO) providing persistence and search support for
 * StartForm entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.StartForm
 */

public class StartFormDAO extends BaseTypeDAO<StartForm> {
	private static final Log log = LogFactory.getLog(StartFormDAO.class);

	// property constants
	public static final String CONTACT = "contact";
	public static final String PROJECT = "project";
	private static final String BATCH = "productionBatch";
	private static final String LOCKED_BY = "lockedBy";
	public static final String PRIOR_FORM_ID = "priorFormId";
//	private static final String FORM_NUMBER = "formNumber";
//	private static final String FORM_TYPE = "formType";
	private static final String SOCIAL_SECURITY = "socialSecurity";
//	private static final String ACCT_CODE = "acctCode";
	public static final String UNION_LOCAL_NUM = "unionLocalNum";
//	private static final String OCCUPATION_CODE = "occupationCode";
//	private static final String JOB_CLASS = "jobClass";
	private static final String DEPT_NAME = "deptName";
//	private static final String SCHEDULE_LTR = "scheduleLtr";
//	private static final String COMPANY_NAME = "companyName";
	private static final String FEDERAL_TAX_ID = "federalTaxId";
//	private static final String ETHNIC_CODE = "ethnicCode";
//	private static final String CITIZEN_STATUS = "citizenStatus";
//	private static final String STATE_OF_RESIDENCE = "stateOfResidence";
//	private static final String MINOR = "minor";
//	private static final String AGENT_REP = "agentRep";
//	private static final String EMPLOYEE_TYPE = "employeeType";
//	private static final String HOURLY_RATE_STUDIO = "hourlyRateStudio";
//	private static final String HOURLY_RATE_STUDIO_HRS = "hourlyRateStudioHrs";
//	private static final String HOURLY_RATE_LOC = "hourlyRateLoc";
//	private static final String HOURLY_RATE_LOC_HRS = "hourlyRateLocHrs";
//	private static final String HOURLY_RATE_ACCT_CODE = "hourlyRateAcctCode";
//	private static final String WEEKLY_RATE_STUDIO = "weeklyRateStudio";
//	private static final String WEEKLY_RATE_STUDIO_HRS = "weeklyRateStudioHrs";
//	private static final String WEEKLY_RATE_LOC = "weeklyRateLoc";
//	private static final String WEEKLY_RATE_LOC_HRS = "weeklyRateLocHrs";
//	private static final String WEEKLY_RATE_ACCT_CODE = "weeklyRateAcctCode";
//	private static final String X15_RATE_OVER_STUDIO = "x15RateOverStudio";
//	private static final String X15_RATE_OVER_STUDIO_HRS = "x15RateOverStudioHrs";
//	private static final String X15_RATE_OVER_LOC = "x15RateOverLoc";
//	private static final String X15_RATE_OVER_LOC_HRS = "x15RateOverLocHrs";
//	private static final String X15_RATE_OVER_ACCT_CODE = "x15RateOverAcctCode";
//	private static final String X20_RATE_OVER_STUDIO = "x20RateOverStudio";
//	private static final String X20_RATE_OVER_STUDIO_HRS = "x20RateOverStudioHrs";
//	private static final String X20_RATE_OVER_LOC = "x20RateOverLoc";
//	private static final String X20_RATE_OVER_LOC_HRS = "x20RateOverLocHrs";
//	private static final String X20_RATE_OVER_ACCT_CODE = "x20RateOverAcctCode";
//	private static final String DAY6_RATE_STUDIO = "day6RateStudio";
//	private static final String DAY6_RATE_STUDIO_HRS = "day6RateStudioHrs";
//	private static final String DAY6_RATE_LOC = "day6RateLoc";
//	private static final String DAY6_RATE_LOC_HRS = "day6RateLocHrs";
//	private static final String DAY6_RATE_ACCT_CODE = "day6RateAcctCode";
//	private static final String DAY7_RATE_STUDIO = "day7RateStudio";
//	private static final String DAY7_RATE_STUDIO_HRS = "day7RateStudioHrs";
//	private static final String DAY7_RATE_LOC = "day7RateLoc";
//	private static final String DAY7_RATE_LOC_HRS = "day7RateLocHrs";
//	private static final String DAY7_RATE_ACCT_CODE = "day7RateAcctCode";
//	private static final String IDLE_DAY6_RATE_LOC = "idleDay6RateLoc";
//	private static final String IDLE_DAY6_RATE_LOC_HRS = "idleDay6RateLocHrs";
//	private static final String IDLE_DAY6_RATE_ACCT_CODE = "idleDay6RateAcctCode";
//	private static final String IDLE_DAY7_RATE_LOC = "idleDay7RateLoc";
//	private static final String IDLE_DAY7_RATE_LOC_HRS = "idleDay7RateLocHrs";
//	private static final String IDLE_DAY7_RATE_ACCT_CODE = "idleDay7RateAcctCode";
//	private static final String BOX_RENTAL_STUDIO = "boxRentalStudio";
//	private static final String BOX_RENTAL_LOC = "boxRentalLoc";
//	private static final String BOX_RENTAL_SEP_CHECK = "boxRentalSepCheck";
//	private static final String BOX_RENTAL_ACCT_CODE = "boxRentalAcctCode";
//	private static final String CAR_ALLOW_STUDIO = "carAllowStudio";
//	private static final String CAR_ALLOW_LOC = "carAllowLoc";
//	private static final String CAR_ALLOW_SEP_CHECK = "carAllowSepCheck";
//	private static final String CAR_ALLOW_ACCT_CODE = "carAllowAcctCode";
//	private static final String MEAL_ALLOW = "mealAllow";
//	private static final String MEAL_ALLOW_SEP_CHECK = "mealAllowSepCheck";
//	private static final String MEAL_ALLOW_ACCT_CODE = "mealAllowAcctCode";
//	private static final String MEAL_PENALTY_SEP_CHECK = "mealPenaltySepCheck";
//	private static final String MEAL_PENALTY_ACCT_CODE = "mealPenaltyAcctCode";
//	private static final String SCREEN_CREDIT_ROLE = "screenCreditRole";
//	private static final String SCREEN_CREDIT_NAME = "screenCreditName";
//	private static final String INDUSTRY_EXP_ROSTER_CONF = "industryExpRosterConf";
//	private static final String EMERGENCY_NAME = "emergencyName";
//	private static final String EMERGENCY_PHONE = "emergencyPhone";
//	private static final String MEDICAL_CONDITIONS = "medicalConditions";


	public static StartFormDAO getInstance() {
		return (StartFormDAO)getInstance("StartFormDAO");
	}

	/**
	 * Find all StartForm`s associated with a given Production.
	 *
	 * @param prod The Production of interest.
	 * @return A non-null, but possibly empty, List of StartForm`s.
	 */
	@SuppressWarnings("unchecked")
	public List<StartForm> findByProduction(Production prod) {
		String queryString = "from StartForm sd where " +
				" contact.production = ? ";
		return find(queryString, prod);
	}

//	/**
//	 * Find all Start Forms for the given production, batch and Department that
//	 * have not yet expired. This means their their Effective End data (if any)
//	 * and Work End date (if any) must be greater than the end of the prior
//	 * payroll week. This method includes StartForm`s whose work-start or
//	 * effective-start date is in the future.
//	 *
//	 * @param prod The Production containing the StartForm`s.
//	 * @param project The Project of interest; if null, ignore the StartForm's
//	 *            Project affiliation.
//	 * @param batch The ProductionBatch that the StartForm belongs to. If null,
//	 *            the StartForm must not belong to any batch to be included.
//	 * @param deptName The department name of the StartForm. If null, then the
//	 *            StartForm may be for any Department.
//	 * @return A non-null, but possibly empty, List of StartForm`s matching the
//	 *         criteria given above.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<StartForm> findByBatchDepartmentUnexpired(Production prod, Project project,
//			ProductionBatch batch, String deptName) {
//		Date endDate = TimecardUtils.calculateLastDayOfCurrentWeek();
//		endDate = TimecardUtils.calculatePriorWeekEndDate(endDate);
//		String queryString = "from StartForm sd where " +
//				" contact.production = ? and " +
//				" (effectiveEndDate is null or effectiveEndDate > ?) and " +
//				" (workEndDate is null or workEndDate > ?) and ";
//
//		List<Object> valueList = new ArrayList<Object>();
//		valueList.add(prod);
//		valueList.add(endDate);
//		valueList.add(endDate);
//
//		if (batch == null) {
//			queryString += BATCH + " is null ";
//		}
//		else {
//			valueList.add(batch);
//			queryString += BATCH + " = ? ";
//		}
//
//		if (deptName != null) {
//			valueList.add(deptName);
//			queryString += " and " + DEPT_NAME + " = ? ";
//		}
//
//		if (project != null) {
//			valueList.add(project);
//			queryString += " and " + PROJECT + " = ? ";
//		}
//
//		return find(queryString, valueList.toArray());
//	}

//	/**
//	 * Find all Start Forms for the given production, batch and Department that
//	 * are in effect for the current week. This means their Work Start date and
//	 * Effective Date must be earlier than or equal to the last day of this
//	 * week's payroll week; and their Effective End data (if any) and Work End
//	 * date (if any) must be greater than the end of the prior payroll week.
//	 *
//	 * @param production The Production containing the StartForm`s.
//	 * @param batch The ProductionBatch that the StartForm belongs to. If null,
//	 *            the StartForm must not belong to any batch to be included.
//	 * @param deptName The department name of the StartForm. If null, then the
//	 *            StartForm may be for any Department.
//	 * @return A non-null, but possibly empty, List of StartForm`s matching the
//	 *         criteria given above.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<StartForm> findByBatchDepartmentActive(Production prod, ProductionBatch batch, String deptName) {
//		Date endDate = TimecardUtils.getLastDayOfCurrentWeek();
//		Date priorDate = TimecardUtils.getPriorWeekEndDate(endDate);
//		String queryString = "from StartForm sd where " +
//				" contact.production = ? and " +
//				" effectiveStartDate <= ? and" +
//				" workStartDate <= ? and " +
//				" (effectiveEndDate is null or effectiveEndDate > ?) and " +
//				" (workEndDate is null or workEndDate > ?) and ";
//
//		List<Object> valueList = new ArrayList<Object>();
//		valueList.add(prod);
//		valueList.add(endDate);
//		valueList.add(endDate);
//		valueList.add(priorDate);
//		valueList.add(priorDate);
//
//		if (batch == null) {
//			queryString += BATCH + " is null ";
//		}
//		else {
//			valueList.add(batch);
//			queryString += BATCH + " = ? ";
//		}
//
//		if (deptName != null) {
//			valueList.add(deptName);
//			queryString += " and " + DEPT_NAME + " = ? ";
//		}
//
//		return find(queryString, valueList.toArray());
//	}

	/**
	 * Determine if a StartForm exists for the given Contact.
	 *
	 * @param contact The Contact of interest.
	 * @return True iff the given Contact has a StartForm in the database.
	 */
	public boolean existsContact(Contact contact) {
		String query = "from StartForm where " +
				CONTACT + " = ? " ;
		return exists(query, contact);
	}

	/**
	 * Determine if a StartForm exists for the given Contact and Project. This
	 * is only meaningful only for Commercial productions.
	 *
	 * @param contact The Contact of interest.
	 * @param project The Project of interest.
	 * @return True iff the given Contact has a StartForm for the specified
	 *         Project.
	 */
	public boolean existsContactProject(Contact contact, Project project) {
		String query = "from StartForm where " +
				CONTACT + " = ? " +
				" and " + PROJECT + " = ? ";
		Object[] values = { contact, project };
		return exists(query, values);
	}

	public boolean existsTimecardsForEmployment(Employment emp) {
		boolean exists = false;
		List<StartForm> startFormList = findByProperty("employment", emp);
		for (StartForm sf : startFormList) {
			// Check if Timecards exists
			if (WeeklyTimecardDAO.getInstance().existsTimecardsForStartForm(sf.getId())) {
				log.debug("found");
				exists = true;
				break;
			}
		}
		return exists;
	}

	/**
	 * Determine if there are any StartForm`s for the given Employment which do
	 * NOT have a VOID status.
	 *
	 * @param emp The Employment of interest.
	 * @return True iff at least one StartForm with a non-VOID status exists
	 *         which is related to the given Employment.
	 *//*
	public boolean existsNonVoidByEmployment(Employment emp) {
		return findCountByNamedQuery(StartForm.GET_COUNT_START_FORM_NON_VOID_BY_EMPLOYMENT, map("employment", emp)) > 0;
	}
*/
	/**
	 * Find all the StartForm`s associated with the specified Project. This is
	 * meaningful only for Commercial productions.
	 *
	 * @param project The Project of interest.
	 * @return a non-null, but possibly empty, List of StartForm`s associated
	 *         with the given Project.
	 */
	@SuppressWarnings("unchecked")
	public List<StartForm> findByProject(Project project) {
		String queryString = "from StartForm sd where " +
				" project = ? ";

		return find(queryString, project);
	}

	/**
	 * Finds all StartForm`s that match the given Contact.
	 *
	 * @param contact The Contact of interest.
	 * @return Non-null, but possibly empty, List of StartForm`s matching the
	 *         above description.
	 */
	public List<StartForm> findByContact(Contact contact) {
		return findByProperty(CONTACT, contact);
	}

	/**
	 * Finds a StartForm that matches the given Contact and position (occupation).
	 * Note that if multiple StartForm`s match the query, the one with the latest
	 * workStartDate or effectiveStartDate will be returned.
	 *
	 * @param contact The Contact of interest.
	 * @param position The position (occupation or job class) of interest.
	 * @return A matching StartForm, or null if no match is found.
	 */
	public StartForm findByContactPosition(Contact contact, String position) {
		Object[] values = { contact, position };
		String queryString = "from StartForm sd where contact = ? " +
				" and jobClass = ? " +
				" order by workStartDate DESC, effectiveStartDate DESC ";
		return findOne(queryString, values);
	}

//	/**
//	 * Determine how many StartForm objects exist for the given Contact, where
//	 * the StartForm's workStartDate and workEndDate would make a timecard valid
//	 * if it had a week-ending date of 'weekEndDate'.
//	 *
//	 * @param contact The Contact of interest.
//	 * @param priorWeekEndDate The week-ending date 7 days earlier than
//	 *            'weekEndDate'.
//	 * @param weekEndDate The week-ending date that would appear on a timecard.
//	 * @return A non-negative value. The value is the number of StartForm's found
//	 *         for the given Contact, where the workStartDate is
//	 *         less-than-or-equal to the weekEndDate, and the workEndDate is
//	 *         greater-than the priorWeekEndDate. That is, a timecard whose
//	 *         week-end date was 'weekEndDate' would be valid based on the
//	 *         StartForm dates.
//	 */
//	public int countActiveByContactDates(Contact contact, Date priorWeekEndDate, Date weekEndDate) {
//		String queryString = "select count(id) from StartForm sd where " +
//				" contact = ? and " +
//				" workStartDate <= ? and " +
//				" (workEndDate is null or workEndDate > ?) ";
//		Object[] values = { contact, weekEndDate, priorWeekEndDate };
//		return findCount(queryString, values).intValue();
//	}

	/**
	 * Find all the StartForm`s for the specified Contact and (optional)
	 * Project.
	 *
	 * @param contact The Contact of interest.
	 * @param project The Project of interest; if null, ignore the StartForm's
	 *            Project affiliation.
	 * @return A non-null, but possibly empty, list of matching StartForm`s.
	 */
	@SuppressWarnings("unchecked")
	public List<StartForm> findByContactProject(Contact contact, Project project, boolean allowOnboard, String orderBy) {
		String queryString = "from StartForm sd where " +
				" sd.contact = ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(contact);

		if (allowOnboard) {
			queryString = "select sd from StartForm sd, ContactDocument cd where sd.id = cd.relatedFormId and cd.status <> 'VOID'" +
				" and sd.contact = ? ";
		}
		if (project != null) {
			valueList.add(project);
			queryString += " and sd.project = ? ";
		}

		if(orderBy != null) {
			queryString += " order by " + orderBy;
		}

		return find(queryString, valueList.toArray());
	}

	public List<StartForm> findByContactProject(Contact contact, Project project, boolean allowOnboard) {
		return findByContactProject(contact, project, allowOnboard, null);
	}

	/**
	 * Find all the StartForm`s for the specified Contact and (optional)
	 * Project.
	 *
	 * @param contact The Contact of interest.
	 * @param project The Project of interest; if null, ignore the StartForm's
	 *            Project affiliation.
	 * @return A non-null, but possibly empty, list of matching StartForm`s.
	 */
	@SuppressWarnings("unchecked")
	public List<StartForm> findByContactProjectPermitted(Contact contact, Project project) {
		String queryString = "from StartForm sd where " +
				" contact = ? " +
				" order by project.id ";

		List<StartForm> list = find(queryString, contact);
		Contact viewer = null;

		if (project != null) { // filter results based on permissions
			AuthorizationBean authBean = AuthorizationBean.getInstance();
			Project lastProject = project;
			boolean permitted = true; // SFs for 'project' are known to be accessible
			for (Iterator<StartForm> iter = list.iterator(); iter.hasNext(); ) {
				StartForm sf = iter.next();
				if (! sf.getProject().equals(lastProject)) {
					lastProject = sf.getProject();
					if (viewer == null) {
						viewer = SessionUtils.getCurrentContact();
					}
					permitted = authBean.hasPermission(viewer, lastProject, Permission.VIEW_TIMECARD_HTG);
					permitted = permitted || authBean.hasPermission(viewer, lastProject, Permission.EDIT_START_FORMS);
				}
				if (! permitted) {
					iter.remove();
				}
			}
		}
		return list;
	}

	/**
	 * Find the StartForm that has the most recent workStartDate or
	 * effectiveStartDate, for the given Contact -- regardless of
	 * position/occupation.
	 *
	 * @param contact The Contact of interest.
	 * @return null if no StartForm exists for the Contact.
	 */
	public StartForm findLatestForContact(Contact contact) {
		String queryString = "from StartForm sd where " +
				" contact = ? order by workStartDate DESC, effectiveStartDate DESC, sequence DESC ";
		Object[] values = { contact };
		return findOne(queryString, values);
	}

	/**
	 * Find the StartForm that has the most recent workStartDate or
	 * effectiveStartDate, for the given Employment.
	 *
	 * @param contact The Contact of interest.
	 * @return null if no StartForm exists for the Contact.
	 */
	public StartForm findLatestForEmployment(Employment emp) {
		String queryString = "from StartForm sd where " +
				" employment = ? order by workStartDate DESC, effectiveStartDate DESC, sequence DESC ";
		Object[] values = { emp };
		return findOne(queryString, values);
	}

	/**
	 * find StartForm's that belong to the specified Production, and that are
	 * "active" within the range of dates given, which means they should have:<br>
	 * effectiveStartDate <= wkEndDate & effectiveEndDate null or >= wkStartDate <br>
	 * & workStartDate <= wkEndDate & workEndDate >= wkStartDate, <br>
	 * sorted by user and effectiveStartDate, descending.
	 *
	 * @param prod The production containing the StartForm's.
	 * @param wkStartDate The starting date of the week (i.e., Sunday) for which
	 *            active StartForm's should be found.
	 * @param wkEndDate The ending date of the week (i.e., Saturday) for which
	 *            active StartForm's should be found.
	 * @return Non-null, but possibly empty, List of StartForm`s matching the
	 *         above description.
	 */
	@SuppressWarnings("unchecked")
	public List<StartForm> findByActiveDate(Production prod, Date wkStartDate, Date wkEndDate) {
		String queryString = "from StartForm sd where " +
				" contact.production = ? and " +
				" (effectiveStartDate is null or effectiveStartDate <= ?) and" +
				" (effectiveEndDate is null or effectiveEndDate >= ?) and " +
				" workStartDate <= ? and " +
				" (workEndDate is null or workEndDate >= ?) " +
				" order by workStartDate DESC, effectiveStartDate DESC ";
		Object[] values = { prod, wkEndDate, wkStartDate, wkEndDate, wkStartDate };
		return find(queryString, values);
	}

	/**
	 * Find the StartForm`s that belong to the specified Project, and that are
	 * "active" within the range of dates given, which means they should have:<br>
	 * effectiveStartDate <= wkEndDate & effectiveEndDate null or >= wkStartDate <br>
	 * & workStartDate <= wkEndDate & workEndDate >= wkStartDate, <br>
	 * sorted by user and effectiveStartDate, descending. Note that this only
	 * works for Commercial productions, where Starts are associated with a
	 * Project instead of a Production.
	 *
	 * @param project The Project containing the StartForm`s.
	 * @param wkStartDate The starting date of the week (i.e., Sunday) for which
	 *            active StartForm's should be found.
	 * @param wkEndDate The ending date of the week (i.e., Saturday) for which
	 *            active StartForm's should be found.
	 * @return Non-null, but possibly empty, List of StartForm`s matching the
	 *         above description.
	 */
	@SuppressWarnings("unchecked")
	public List<StartForm> findByActiveDateProject(Project project, Date wkStartDate, Date wkEndDate) {
		String queryString = "from StartForm sd where " +
				" project = ? and " +
				" (effectiveStartDate is null or effectiveStartDate <= ?) and" +
				" (effectiveEndDate is null or effectiveEndDate >= ?) and " +
				" workStartDate <= ? and " +
				" (workEndDate is null or workEndDate >= ?) " +
				" order by workStartDate DESC, effectiveStartDate DESC ";
		Object[] values = { project, wkEndDate, wkStartDate, wkEndDate, wkStartDate };
		return find(queryString, values);
	}

	/**
	 * Find the Production.id values of Production`s that are eligible for
	 * automatic timecard creation. Such productions must be in Active status,
	 * have the "automatic creation" flag set to true, and have StartForm`s with
	 * non-null social security numbers. The check for SSNs eliminates a lot of
	 * productions that we would otherwise retrieve then ignore when we did not
	 * find any "complete" StartForm`s.
	 * <p>
	 * This method is used by the batch timecard creation process.
	 * <p>
	 * Note that the "automatic creation" flag is ignored for Commercial
	 * productions. The batch code needs to check preferences at the project
	 * level for those.
	 *
	 * @return A non-null, but possibly empty, List of distinct Production.id
	 *         values of all the "Active" Production`s which contain at least
	 *         one StartForm. The List is sorted in ascending title order.
	 */
	@SuppressWarnings("unchecked")
	public List<Integer> findProductionsForTimecardCreation() {
		String queryString = "select distinct p.id from StartForm sd, Production p, PayrollPreference pref where " +
				" sd.contact.production = p and " +
				" sd.socialSecurity is not null and " +
				" p.status = 'ACTIVE' and " +
				" ((p.payrollPref = pref and pref.autoCreateTimecards = 1) or " +
				" (p.type = 'TV_COMMERCIALS')) " +
				" order by p.title ";
		/* Equivalent raw SQL:
		   select distinct p.id from start_form, contact, production p, Payroll_Preference pref where
 		    start_form.contact_id = contact.id and contact.production_id = p.id and
 		    ((p.payroll_preference_id = pref.id and pref.auto_Create_Timecards = 1) or
 		      (p.type = 'TV_COMMERCIALS')) and
 		    p.status = 'active' order by p.title; /*
		 */
		return find(queryString);
	}

	/**
	 * Find StartForms for a given Contact and jobClass (occupation) that are
	 * active within the date range given.
	 *
	 * @param wkStartDate The starting date of the week (i.e., Sunday) for which
	 *            active StartForm's should be found.
	 * @param wkEndDate The ending date of the week (i.e., Saturday) for which
	 *            active StartForm's should be found.
	 * @param contact The Contact whose StartForm`s we are looking for.
	 * @param jobClass The occupation that must match the StartForm.jobClass
	 *            value.
	 * @return Non-null, but possibly empty, List of StartForm`s matching the
	 *         above description.
	 */
	@SuppressWarnings("unchecked")
	public List<StartForm> findByActiveDateContactJob(Date wkStartDate, Date wkEndDate,
			Contact contact, String jobClass) {
		String queryString = "from StartForm sd where " +
				" contact = ? and " +
				" jobClass = ? and " +
				" (effectiveStartDate is null or effectiveStartDate <= ?) and" +
				" (effectiveEndDate is null or effectiveEndDate >= ?) and " +
				" workStartDate <= ? and " +
				" (workEndDate is null or workEndDate >= ?) " +
				" order by effectiveStartDate, workStartDate ";
		Object[] values = { contact, jobClass, wkEndDate, wkStartDate, wkEndDate, wkStartDate };
		return find(queryString, values);
	}

	/**
	 * Determine if any StartForm`s reference the given Department.
	 *
	 * @param dept The Department of interest.
	 * @return True iff at least one StartForm references the specified
	 *         Department.
	 */
	public boolean referencesDept(Department dept) {
		String query = "select count(id) from StartForm where department = ?";
		return findCount(query, dept) > 0;
	}

	// LS-3611 removed unused methods findByBatch() and findByBatchDepartmentUserAcct()

	/**
	 * @param contact The Contact of interest.
	 * @return The highest sequence number field for all the StartForm`s for the
	 *         given Contact. Returns 0 if there are no such StartForms.
	 */
	@SuppressWarnings("unchecked")
	public int findMaxSequence(Contact contact) {
		String queryString = "select max(sequence) from StartForm where " + CONTACT + " = ? ";
		int sequence = 0;
		List<Object> list = find(queryString, contact);
		if (list != null && list.size() > 0 && list.get(0) instanceof Integer) {
			Integer iSeq = (Integer)list.get(0);
			if (iSeq != null) {
				sequence = iSeq.intValue();
			}
		}
		return sequence;
	}

	/**
	 * Find just the social security number from a StartForm given the id. This
	 * is used by our Jasper report "scriptlet" code.
	 *
	 * @param id The database id of the StartForm of interest.
	 * @return The unformatted 9-digit SSN, or null if not found.
	 */
	@SuppressWarnings("unchecked")
	public String findSsnById(Integer id) {
		String ssn = null;
		String query = "select " + SOCIAL_SECURITY + " from StartForm " +
				" where id = ? ";
		List<String> strs = find(query, id);
		if (strs.size() > 0) {
			ssn = strs.get(0);
		}
		return ssn;
	}

	/**
	 * Find just the federal tax id field from a StartForm given the id. This
	 * is used by our Jasper report "scriptlet" code.
	 *
	 * @param id The database id of the StartForm of interest.
	 * @return The unformatted 9-digit FEIN (federal tax id), or null if not found.
	 */
	@SuppressWarnings("unchecked")
	public String findFedTaxIdById(Integer id) {
		String fedTaxId = null;
		String query = "select " + FEDERAL_TAX_ID + " from StartForm " +
				" where id = ? ";
		List<String> strs = find(query, id);
		if (strs.size() > 0) {
			fedTaxId = strs.get(0);
		}
		return fedTaxId;
	}

	/**
	 * Lock the specified StartForm (for editing) so no other User
	 * can edit it, and return True if successful.
	 *
	 * @param startForm The StartForm to be locked.
	 * @param user The User who will "hold" the lock and is allowed to edit the StartForm.
	 * @return True if the User has successfully been given the lock, which
	 *         means that no other User currently had the lock and we
	 *         successfully updated the database with the lock information, or
	 *         the same User already had the lock.
	 */
	@Transactional
	public boolean lock(StartForm startForm, User user) {
		boolean ret = false;
		try {
			if (startForm.getLockedBy() == null) {
				startForm.setLockedBy(user.getId());
				attachDirty(startForm);
				ret = true;
			}
			else if (startForm.getLockedBy().equals(user.getId())) {
				ret = true;
			}
			log.debug("locked #" + startForm.getId() + "=" + ret + ", by="+ user.getId());
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
		}
		return ret;
	}

	/**
	 * Unlock a StartForm so any User (with appropriate permission) can now
	 * edit it. No error occurs if the StartForm was already unlocked.
	 *
	 * @param startForm The StartForm to be unlocked.
	 * @param userId The database id of the User that is trying to unlock the
	 *            StartForm. If the StartForm is locked by a different user, the
	 *            unlock request is ignored.
	 * @return True if the StartForm was updated in the database; false indicates
	 *         the StartForm was not updated, because either (a) it was not
	 *         locked, or (b) it was locked by someone other than the given
	 *         userId.
	 */
	@Transactional
	public boolean unlock(StartForm startForm, Integer userId) {
		boolean bRet = false;
		try {
			log.debug("unlocking #" + startForm.getId() + "; locked by=" + startForm.getLockedBy());
			if (startForm.getLockedBy() != null) {
				if (userId == null || userId.equals(startForm.getLockedBy())) {
					startForm.setLockedBy(null);
					try {
						attachDirty(startForm);
					}
					catch (Exception e) {
						log.error(e.getMessage(),e);
					}
					log.debug(startForm);
					bRet = true;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return bRet;
	}

	/**
	 * Unlock all StartForms that are currently locked. Called during application
	 * startup, to clear any locks leftover from a prior execution, in case of a
	 * "hard" crash in which our normal methods did not clear the locks during
	 * shutdown (e.g., via StartFormBean.dispose()).
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	public void unlockAllLocked() {
		try {
			String query = "from StartForm where " + LOCKED_BY + " is not null";
			List<StartForm> locked = find(query);
			for (StartForm startForm : locked) {
				unlock(startForm, null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Create and save a new ContactDocument connected to the given StartForm.
	 * This method is only called when we find a StartForm without an associated
	 * ContactDocument, in an On-boarding production, which shouldn't really
	 * happen!
	 *
	 * @param sf The StartForm that needs the ContactDocument.
	 * @param prod The current Production.
	 * @return The newly created ContactDocument instance.
	 */
	public ContactDocument createContactDocument(StartForm sf, Production prod) {
		ContactDocument cd = null;
		if (prod != null && prod.getAllowOnboarding()) {
			Document payrollDocument = DocumentDAO.findPayrollDocument(prod);
			DocumentChain payrollChain = DocumentChainDAO.getInstance().findById(payrollDocument.getDocChainId());
			// We'll mark these as "PENDING" Approval status for now.
			cd = createContactDocument(sf, payrollDocument, payrollChain, ApprovalStatus.PENDING);
		}
		return cd;
	}

	/** Method to create ContactDocument for a StartForm.
	 * @param sf StartForm for which the ContactDocument will be created.
	 * @param sfDocument Document(Payroll Start) for StartForm in the Production.
	 * @return The newly created ContactDocument instance.
	 */
	@Transactional
	public ContactDocument createContactDocument(StartForm sf, Document sfDocument, DocumentChain sfChain, ApprovalStatus status) {
		boolean aicp = false;
		if (sf.getProject() != null) {
			aicp = true;
		}
		ContactDocument cd = ContactDocumentDAO.create(sf.getContact(), sfDocument, null, aicp, sf.getEmployment(), status, sf.getProject());
		cd.setRelatedFormId(sf.getId());
		cd.setFormType(PayrollFormType.START);
		cd.setDocumentChain(sfChain);
		save(cd);
		if (sf.getEffectiveStartDate() == null) {
			sf.setEffectiveStartDate(sf.getWorkStartOrHireDate()); // guaranteed to be not null
			attachDirty(sf);
		}
		log.debug("StartForm : " + sf.getId() + " " + "New Contact Document : " + cd.getId());
		return cd;
	}

//	/**
//	 * Convert encrypted fields from v2.1 to v2.2. (rev 4439)
//	 */
//	@Transactional
//	public void convertStartForms() {
//		int converted = 0;
//		int already = 0;
//
//		List<StartForm> forms = findAll();
//		for (StartForm sform : forms) {
//			//log.debug(wtc.getId());
//			if (sform.getSocialSecurity() == null) {
//				if (sform.getOldSocialSecurity() != null) {
//					sform.setSocialSecurity(sform.getOldSocialSecurity());
//					converted++;
//				}
//			}
//			else {
//				log.debug("#" + sform.getId() + " SSN already converted");
//				already++;
//			}
//		}
//
//		log.info("" + forms.size() + " StartForms input for conversion from 2.1 to 2.2.");
//		log.info("" + already + " already had new startForm SSN" );
//		log.info("" + converted + " StartForms converted");
//	}

//	public static StartFormDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (StartFormDAO)ctx.getBean("StartFormDAO");
//	}

}