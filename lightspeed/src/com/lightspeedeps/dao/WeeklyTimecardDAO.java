package com.lightspeedeps.dao;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.port.Importer;
import com.lightspeedeps.service.TimecardService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.*;
import com.lightspeedeps.web.approver.ApprovePromptBean;
import com.lightspeedeps.web.approver.ApproverUtils;

/**
 * A data access object (DAO) providing persistence and search support for WeeklyTimecard entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in one of the
 * superclasses {@link com.lightspeedeps.dao.BaseDAO} or {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they should be accessed.
 * <p>
 * Some notes on methods:
 * <ul>
 * <li>Methods that return a List<TimecardEntry> (instead of the usual List <WeeklyTimeCard>) have
 * names ending in "TCE".
 * <li>Since almost all of the findBy... methods begin with a Production and Project parameter,
 * those words are not included in the method names; e.g., we have findByUserAccount() instead of
 * findByProductionProjectUserAccount().
 * <li>In most cases a null parameter is ignored, e.g., for week-ending date. However, methods that
 * take a "batch name" parameter treat null to mean that the timecard is NOT in a batch.
 * </ul>
 *
 * @see com.lightspeedeps.model.WeeklyTimecard
 */
public class WeeklyTimecardDAO extends ApprovableDAO<WeeklyTimecard> {
	// @SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(WeeklyTimecardDAO.class);

	// property constants
	private static final String USER_ACCOUNT = "userAccount";
	private static final String END_DATE = "endDate";
	private static final String STATUS = "status";
	private static final String ADJUSTED = "adjusted";
	public static final String START_FORM = "startForm";
	private static final String WEEKLY_BATCH = "weeklyBatch";
	private static final String APPROVER_ID = "approverId";
	private static final String LOCKED_BY = "lockedBy";
	// private static final String LAST_NAME = "lastName";
	// private static final String FIRST_NAME = "firstName";
	// private static final String USER_ID = "userId";
	// private static final String LOAN_OUT_CORP = "loanOutCorp";
	private static final String PROD_ID = "prodId";
	// private static final String PROD_NAME = "prodName";
	// private static final String PROD_CO = "prodCo";
	private static final String OCCUPATION = "occupation";
	private static final String DEPT_NAME = "deptName";
	private static final String DEPT_ID = "departmentId";
	// private static final String RATE = "rate";
	// private static final String GUAR_HOURS = "guarHours";
	// private static final String STATE_WORKED = "stateWorked";
	// private static final String FED_CORP_ID = "fedCorpId";
	// private static final String STATE_CORP_ID = "stateCorpId";
	// private static final String ACCOUNT_NUM = "accountNum";
	// private static final String CUSTOM_MULT = "customMult";
	// private static final String TOTAL10 = "total10";
	// private static final String TOTAL15 = "total15";
	// private static final String TOTAL20 = "total20";
	// private static final String TOTAL_CUST = "totalCust";
	// private static final String TOTAL_MPV = "totalMpv";
	// private static final String RATE10 = "rate10";
	// private static final String RATE15 = "rate15";
	// private static final String RATE20 = "rate20";
	// private static final String RATEXX = "ratexx";
	// private static final String RATE_MPV = "rateMpv";
	// private static final String NUM_ADJ = "numAdj";
	// private static final String RATE_ADJ = "rateAdj";
	// private static final String TIME_TABLE_TOTAL = "timeTableTotal";
	// private static final String COMMENTS = "comments";
	// private static final String BOX_INFO_FILE = "boxInfoFile";
	// private static final String BOX_SEP_CHECK = "boxSepCheck";
	// private static final String BOX_AMT = "boxAmt";
	// private static final String BOX_ACCT = "boxAcct";
	// private static final String PERDIEM_TX = "perdiemTx";
	// private static final String PERDIEM_NTX = "perdiemNtx";
	// private static final String PERDIEM_ACCT = "perdiemAcct";
	// private static final String PERDIEM_DAYS = "perdiemDays";
	// private static final String LODGE_TX = "lodgeTx";
	// private static final String LODGE_NTX = "lodgeNtx";
	// private static final String LODGE_ACCT = "lodgeAcct";
	// private static final String LODGE_DAYS = "lodgeDays";
	// private static final String PERS_MILES_NUM = "persMilesNum";
	// private static final String PERS_MILES_RATE = "persMilesRate";
	// private static final String PERS_MILES_TOTAL = "persMilesTotal";
	// private static final String PERS_MILES_ACCT = "persMilesAcct";
	// private static final String BIZ_MILES_NUM = "bizMilesNum";
	// private static final String BIZ_MILES_RATE = "bizMilesRate";
	// private static final String BIZ_MILES_TOTAL = "bizMilesTotal";
	// private static final String BIZ_MILES_ACCT = "bizMilesAcct";
	// private static final String CAR_ALLOW_AMT = "carAllowAmt";
	// private static final String CAR_ALLOW_ACCT = "carAllowAcct";
	// private static final String OTHER_EXPL = "otherExpl";
	// private static final String OTHER_AMT = "otherAmt";
	// private static final String OTHER_ACCT = "otherAcct";
	// private static final String ADV1_EXPL = "adv1Expl";
	// private static final String ADV1_AMT = "adv1Amt";
	// private static final String ADV1_ACCT = "adv1Acct";
	// private static final String ADV2_EXPL = "adv2Expl";
	// private static final String ADV2_AMT = "adv2Amt";
	// private static final String ADV2_ACCT = "adv2Acct";
	// private static final String ADJ_GTOTAL = "adjGtotal";
	// private static final String ADV_GTOTAL = "advGtotal";
	// private static final String GRAND_TOTAL = "grandTotal";

	/**
	 * Used to control which timecards should be included, when searching
	 * relative to a given date:
	 * <ul>
	 * <li>'EXACT' if the timecards must match the given date;
	 * <li>'PRIOR' if they can be any date earlier than that given;
	 * <li>'WEEK' if they can be the same date or any date in the prior 6 days
	 * (that week);
	 * <li>'ANY' if the date is irrelevant (the 'week-ending-date' argument
	 * passed to the method should be null)
	 * </ul>
	 */
	public enum TimecardRange {
		/** search for timecards that exactly match a given date */
		EXACT,
		/** search for timecards that are prior to a given date */
		PRIOR,
		/** search for timecards that have the same date or any date in the prior
		 * 6 days (that week) */
		WEEK,
		/** search for timecards with ANY date (ignore the date argument) */
		ANY };

	public static WeeklyTimecardDAO getInstance() {
		return (WeeklyTimecardDAO) getInstance("WeeklyTimecardDAO");
	}

	/**
	 * Find all the timecards for a given User within a specific Production. The list may be
	 * optionally filtered by timecard status. Note that this list may include timecards for
	 * multiple occupations (that is, related to different StartForm`s).
	 *
	 * @param prod The Production of interest; if null, an error Event is logged, and an empty list
	 *            is returned.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param userAccount The LS account number (i.e., User.accountNumber) of interest. If null,
	 *            this parameter is ignored, and all timecards for the Production are returned.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard`s matching the given criteria.
	 */
	public List<WeeklyTimecard> findByUserAccount(Production prod, Project project, String userAccount, String statusFilter) {

		return findByWeekEndDateAccount(prod, project, null, TimecardRange.ANY, userAccount, statusFilter);
	}

	/**
	 * Find all the timecards for a given User within a specific Production. The list may be
	 * optionally filtered by timecard status. Note that this list may include timecards for
	 * multiple occupations (that is, related to different StartForm`s). The timecards are returned
	 * wrapped in TimecardEntry objects.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param userAccount The LS account number (i.e., User.accountNumber) of interest.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of TimecardEntry`s containing WeeklyTimecard`s
	 *         matching the given criteria.
	 */
	public List<TimecardEntry> findByUserAccountTCE(Production prod, Project project, String userAccount, String statusFilter) {

		return createTCElist(findByUserAccount(prod, project, userAccount, statusFilter));
	}

	/**
	 * Find all the timecards for a given User within a specific Production, and either within a
	 * specific batch, or un-batched. The list may be optionally filtered by timecard status.
	 * <p>
	 * Note that this list may include timecards for multiple occupations (that is, related to
	 * different StartForm`s).
	 *
	 * @param prod The Production of interest (must not be null).
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param userAccount The LS account number (i.e., User.accountNumber) of interest. If null, the
	 *            field is ignored and no filtering is done by user account number.
	 * @param batch The WeeklyBatch of interest; if null, then only un-batched timecards are
	 *            included.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard`s matching the given criteria.
	 */
	public List<WeeklyTimecard> findByUserAccountBatch(Production prod, Project project,
			String userAccount, WeeklyBatch batch, String statusFilter) {

		return findByUserAccountBatchDepartment(prod, project, userAccount, batch, null, statusFilter, null);
	}

	/**
	 * Find all the timecards for a given User within a specific Production, and either within a
	 * specific batch, or un-batched. The list may be optionally filtered by timecard status.
	 * <p>
	 * Note that this list may include timecards for multiple occupations (that is, related to
	 * different StartForm`s).
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param userAccount The LS account number (i.e., User.accountNumber) of interest.
	 * @param batch The WeeklyBatch of interest; if null, then only un-batched timecards are
	 *            included.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A List of TimecardEntry representing all the matching WeeklyTimecards.
	 */
	public List<TimecardEntry> findByUserAccountBatchTCE(Production prod, Project project, String userAccount,
			WeeklyBatch batch, String statusFilter) {

		return createTCElist(findByUserAccountBatch(prod, project, userAccount, batch, statusFilter));
	}

	/**
	 * Find any WeeklyTimecard that matches the given parameters of Production, userAccount, and
	 * department name.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param userAcct The User's LS account number -- to match weeklyTimecard.userAccount. If null,
	 *            the field is ignored and no filtering is done by user account number.
	 * @param deptName The department name of interest.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard's.
	 */
	public List<WeeklyTimecard> findByUserAccountDepartment(Production prod,
			Project project, String userAcct, String deptName, String statusFilter) {

		return findByWeekEndDateAccountDept(prod, project, null, TimecardRange.ANY, userAcct, deptName, statusFilter, null);
	}

	/**
	 * Find any WeeklyTimecard that matches the given parameters of Production, userAccount, and
	 * department name.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param userAcct The User's LS account number -- to match weeklyTimecard.userAccount. If null,
	 *            the field is ignored and no filtering is done by user account number.
	 * @param deptName The department name of interest.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard's wrapped in TimecardEntry
	 *         objects.
	 */
	public List<TimecardEntry> findByUserAccountDepartmentTCE(Production prod,
			Project project, String userAcct, String deptName, String statusFilter) {

		return createTCElist(findByUserAccountDepartment(prod, project, userAcct, deptName, statusFilter));
	}

	/**
	 * Find any WeeklyTimecard that matches the given parameters of Production, userAccount, weekly
	 * batch, and department name.
	 *
	 * @param prod The Production of interest (must not be null).
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param userAcct The User's LS account number -- to match weeklyTimecard.userAccount. If null,
	 *            the field is ignored and no filtering is done by user account number.
	 * @param batch The WeeklyBatch of interest; if null, then only include unbatched timecards.
	 * @param deptName The department name of interest; if null, the timecard's department value is
	 *            ignored.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @param order An optional list of fields to be used for ordering the result. It should be in
	 *            SQL format, without the leading " order by " text.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard's.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyTimecard> findByUserAccountBatchDepartment(Production prod,
			Project project, String userAcct, WeeklyBatch batch, String deptName, String statusFilter, String order) {
		String query = "from WeeklyTimecard wtc where " +
				START_FORM  + " is not null and " +
				PROD_ID + " = ? and " +
				WEEKLY_BATCH + (batch == null ? " is null " : " = ? ");

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		if (batch != null) {
			valueList.add(batch);
		}

		if (deptName != null) {
			valueList.add(deptName);
			query += DEPT_NAME + " = ? and ";
		}

		if (statusFilter != null) {
			query += " and " + STATUS + " in (" + statusFilter + ") ";
		}

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		if (userAcct != null) {
			valueList.add(userAcct);
			query += " and " + USER_ACCOUNT + " = ? ";
		}

		if (order != null) {
			query += " order by " + order;
		}

		return find(query, valueList.toArray());
	}

	/**
	 * Find any WeeklyTimecard that matches the given parameters of Production, userAccount, weekly
	 * batch, and department name.
	 *
	 * @param prod The Production of interest (must not be null).
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param userAcct The User's LS account number -- to match weeklyTimecard.userAccount. If null,
	 *            the field is ignored and no filtering is done by user account number.
	 * @param batch The WeeklyBatch of interest; if null, then only include un-batched timecards.
	 * @param deptName The department name of interest (must not be null).
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard's.
	 */
	public List<TimecardEntry> findByUserAccountBatchDepartmentTCE(Production prod,
			Project project, String userAcct, WeeklyBatch batch, String deptName, String statusFilter) {

		return createTCElist(findByUserAccountBatchDepartment(prod, project, userAcct, batch, deptName, statusFilter, null));
	}

	/**
	 * Find any WeeklyTimecard that matches the given parameters of Production, userAccount, and
	 * occupation.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param userAccount The User's LS account number -- to match weeklyTimecard.userAccount.
	 * @param occupation The role or occupation -- to match weeklyTimecard.occupation.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard's.
	 */
	public List<WeeklyTimecard> findByUserAccountOccupation(Production prod,
			Project project, String userAccount, String occupation) {

		return findByWeekEndDateAccountOccupation(prod, project, null, userAccount, occupation);
	}

	/**
	 * Find all the WeeklyTimecard.endDate values for all timecards for the
	 * given Production, Project, user account, and occupation.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial
	 *            productions.
	 * @param userAcct The User's LS account number -- to match
	 *            weeklyTimecard.userAccount.
	 * @param occupation The role or occupation -- to match
	 *            weeklyTimecard.occupation.
	 * @return A non-null, but possibly empty, List of Date values.
	 */
	@SuppressWarnings("unchecked")
	public List<Date> findDatesForUserAccountOccupation(Production prod, Project project,
			String userAcct, String occupation) {
		String query = "select " + END_DATE + " from WeeklyTimecard wtc where " +
				PROD_ID + "= ? and " +
				STATUS + " <> 'VOID' ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		if (occupation != null) {
			valueList.add(occupation);
			query += " and " + OCCUPATION + " = ? ";
		}

		if (userAcct == null) {
			// if no account specified, we need to restrict to "real" timecards,
			// to avoid retrieving the Global Preferences timecards.
			query += " and " + START_FORM + " is not null ";
		}
		else {
			valueList.add(userAcct);
			query += " and " + USER_ACCOUNT + " = ? ";
		}

		return find(query, valueList.toArray());
	}

	/**
	 * Generate the list of all TimecardEntry's in the given Production. Note that for Commercial
	 * productions, timecards from all Projects will be included.
	 * <p>
	 * This is currently only used for test functions.
	 *
	 * @param prod The Production that the time cards apply to.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard that match.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyTimecard> findByProduction(Production prod) {
		String query = "from WeeklyTimecard where " +
				START_FORM  + " is not null and " +
				PROD_ID + " = ? ";
		Object[] values = { prod.getProdId() };

		return find(query, values);
	}

	/**
	 * Find the list of WeeklyTimecards associated with the parameters passed in.
	 * Null values are skipped when building the query.
	 *
	 * @param production - current production
	 * @param project - if this is a commercial the project will not be null. Is null for other production types
	 * @param weekEndDate - week end date for the timecards being returned
	 * @param departmentId - department to which the timecards belong to.
	 * @param orderBy - order in which to display the timecards.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyTimecard> findByWeekEndDept(Production production, Project project, Date weekEndDate, Integer departmentId, String orderBy) {
		List<Object> values = new ArrayList<>();
		String query = " from WeeklyTimecard wtc where prodId = ? ";

		values.add(production.getProdId());

		if(project != null) {
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
			values.add(project);
		}

		if(weekEndDate != null) {
			query += "and endDate = ? ";
			values.add(weekEndDate);
		}

		if(departmentId != null) {
			query += "and departmentId = ? ";
			values.add(departmentId);
		}

		if(orderBy != null) {
			query += " order by ?";
			values.add(orderBy);
		}

		return find(query, values.toArray());
	}

	/**
	 * Generate the list of TimecardEntry that match the given week-ending date in the given
	 * Production.
	 *
	 * @param prod The Production that the time cards apply to.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate
	 * @return A non-null, but possibly empty, List of TimecardEntry's that match 'weDate'.
	 */
	public List<WeeklyTimecard> findByWeekEndDate(Production prod, Project project, Date weDate, String statusFilter) {

		return findByWeekEndDateAccount(prod, project, weDate, TimecardRange.EXACT, null, statusFilter);
	}

	/**
	 * Find the weeklyTimecards that match the given week-end date and
	 * department name, and generate TimecardEntry's from the list.
	 *
	 * @param prod The Production that the time cards apply to.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial
	 *            productions.
	 * @param weDate The week-end date to match
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param deptName The department name to match
	 * @return A non-null, but possibly empty, list of TimecardEntry's, each one
	 *         representing a WeeklyTimecard that matched the given criteria.
	 */
	public List<TimecardEntry> findByWeekEndDateDeptTCE(Production prod, Project project, Date weDate, TimecardRange range, String deptName) {

		return findByWeekEndDateAccountDeptTCE(prod, project, weDate, range, null, deptName, null);
	}

	/**
	 * Find the weeklyTimecards that match the given department name, and generate TimecardEntry's
	 * from the list.
	 *
	 * @param prod The Production that the time cards apply to.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param deptName The department name to match
	 * @return A non-null, but possibly empty, list of TimecardEntry's, each one representing a
	 *         WeeklyTimecard that matched the given criteria.
	 */
	public List<TimecardEntry> findByDepartmentTCE(Production prod, Project project, String deptName) {

		return findByUserAccountDepartmentTCE(prod, project, null, deptName, null);
	}

	/**
	 * Determine if any timecards are waiting for approval by a specific Approver. This is used by
	 * the Approval Hierarchy page to check if a person being moved or removed as an approver has
	 * timecards waiting for their approval.
	 *
	 * @param approver The Approver of interest.
	 * @return True iff the given Approver is the next approver for at least one WeeklyTimecard.
	 */
	public boolean existsForApprover(Approver approver) {
		String query = "select count(wtc.id) from WeeklyTimecard wtc where " +
				" wtc.approverId = ? ";
		return findCount(query, approver.getId()) > 0;
	}

	/**
	 * Find all the timecards that are waiting for approval by a specific Approver. This is used by
	 * the Approval Hierarchy page to check if a person being moved or removed as an approver has
	 * timecards waiting for their approval.
	 *
	 * @param approver The Approver of interest.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard`s with the corresponding
	 *         WeeklyTimecard`s whose 'next approver' refers to the give 'approver' value.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyTimecard> findByApprover(Approver approver) {
		String query = "select wtc from WeeklyTimecard wtc where " +
				" wtc.approverId = ? ";
		return find(query, approver.getId());
	}

	/**
	 * Find all the timecards that are waiting for approval by a specific Contact, within a
	 * particular Production and with a given week-ending date. This is used by the Approval
	 * Dashboard to find timecards waiting for "out of line" approvers.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date of interest.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param contact The Contact of interest.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of TimecardEntry`s with the corresponding
	 *         WeeklyTimecard`s that match the search criteria.
	 */
	public List<TimecardEntry> findByWeekEndDateContactTCE(Production prod, Project project, Date weDate,
			TimecardRange range, Contact contact, String statusFilter) {

		return findByWeekEndDateContactDeptAccountTCE(prod, project, weDate, range, statusFilter, contact, null, null);
	}

	/**
	 * Find all the timecards that are waiting for approval by a specific Contact, within a
	 * particular Production and with a given week-ending date and (optional) batch. This is used by
	 * the Approval Dashboard to find timecards waiting for "out of line" approvers.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date of interest.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param batch The WeeklyBatch that should contain the timecard(s), or null if the timecards
	 *            are not in any batch.
	 * @param contact The Contact of interest.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of TimecardEntry`s with the corresponding
	 *         WeeklyTimecard`s that match the search criteria.
	 */
	@SuppressWarnings("unchecked")
	public List<TimecardEntry> findByWeekEndDateContactBatchTCE(Production prod, Project project, Date weDate,
			TimecardRange range, WeeklyBatch batch, Contact contact, String statusFilter) {
		String query = "select wtc from WeeklyTimecard wtc, Approver app where " +
				" app.shared = 0 and " +
				" wtc.approverId = app.id and " +
				" wtc.prodId = ? and " +
				" app.contact = ? and " +
				" wtc." + WEEKLY_BATCH + (batch == null ? " is null " : " = ? ");

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());
		valueList.add(contact);

		if (batch != null) {
			valueList.add(batch);
		}

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		if (statusFilter != null) {
			query += " and wtc." + STATUS + " in (" + statusFilter + ") ";
		}

		if (weDate != null) {
			query += " and " + createDateClause(valueList, weDate, range);
		}

		return createTCElist(find(query, valueList.toArray()));
	}

	/**
	 * Find all the timecards that are waiting for approval by a specific Contact, within a
	 * particular Production and Department, and with a given week-ending date. This is used by the
	 * Approval Dashboard to find timecards waiting for "out of line" approvers.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date of interest.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param contact The Contact of interest.
	 * @param deptName The name of the Department of interest.
	 * @param userAcct Either null or a user account number (User.accountNumber). If null, this
	 *            parameter is ignored. If not null, only those timecards that match this account
	 *            number will be returned.
	 * @return A non-null, but possibly empty, List of TimecardEntry`s with the corresponding
	 *         WeeklyTimecard`s that match the search criteria.
	 */
	@SuppressWarnings("unchecked")
	public List<TimecardEntry> findByWeekEndDateContactDeptAccountTCE(Production prod, Project project,
			Date weDate, TimecardRange range, String statusFilter,
			Contact contact, String deptName, String userAcct) {

		String query = "select wtc from WeeklyTimecard wtc, Approver app where " +
				" wtc.approverId = app.id and " +
				" wtc.prodId = ? and " +
				" app.contact = ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());
		valueList.add(contact);

		if (weDate != null) {
			query += " and " + createDateClause(valueList, weDate, range);
		}

		if (deptName != null) {
			valueList.add(deptName);
			query += " and wtc.deptName = ? ";
		}

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		if (userAcct != null) {
			valueList.add(userAcct);
			query += " and wtc.userAccount = ? ";
		}

		if (statusFilter != null) {
			query += " and wtc." + STATUS + " in (" + statusFilter + ") ";
		}

		return createTCElist(find(query, valueList.toArray()));
	}

	/**
	 * Find all the timecards for the given user with the specified week-ending date, for ANY
	 * production.
	 *
	 * @param weDate The week-ending date of interest.
	 * @param userAcct The user's account number.
	 *
	 * @return A non-null, but possibly empty, List of WeeklyTimecard`s with the specified
	 *         User.accountNumber and week-ending date which do not have a VOID status.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyTimecard> findByWeekEndDateAccount(Date weDate, String userAcct) {
		String query = "from WeeklyTimecard wtc where " +
				STATUS + " <> 'VOID' ";

		List<Object> valueList = new ArrayList<>();

		if (weDate != null) {
			valueList.add(weDate);
			query += " and " + END_DATE + " = ?";
		}

		if (userAcct == null) {
			// if no account specified, we need to restrict to "real" timecards,
			// to avoid retrieving the Global Preferences timecards.
			query += " and " + START_FORM + " is not null ";
		}
		else {
			valueList.add(userAcct);
			query += " and " + USER_ACCOUNT + " = ? ";
		}

		return find(query, valueList.toArray());
	}

	/**
	 * Find any WeeklyTimecard that matches the given parameters of Production, week-ending date,
	 * userAccount, and optional status. Note that this may include the Global Preference timecards.
	 * userAccount may be null, in which case all accounts are returned.
	 * <p>
	 * When a non-null userAccount is specified, the list is sorted in descending order by ADJUSTED
	 * flag, then descending by id. If there are no adjusted timecards, there should only be a
	 * single entry in the returned list. If there's more than one timecard in the list, all except
	 * one should have the Adjusted flag on. In this case, the first entry in the list will be the
	 * most recently created Adjusted timecard.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date -- to match weeklyTimecard.endDate, or null if any date
	 *            matches.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param userAcct The User's LS account number -- to match weeklyTimecard.userAccount. If null,
	 *            the field is ignored and no filtering is done by user account number.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard's.
	 */
	public List<WeeklyTimecard> findByWeekEndDateAccount(Production prod, Project project, Date weDate,
			TimecardRange range, String userAcct, String statusFilter) {

		return findByWeekEndDateAccountDept(prod, project, weDate, range, userAcct, null, statusFilter, null);
	}

	/**
	 * Determine the number of unsubmitted timecards matching the given
	 * parameters of Production, Project, W/E date, Department name, and status
	 * filter. Unsubmitted or approved timecards (i.e., with a null approverId)
	 * are NOT counted.
	 *
	 * @param prod The Production containing the timecards.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial
	 *            productions.
	 * @param weDate The week-ending date of interest. If 'range' is false, then
	 *            the timecards counted must match this W/E date. If 'range' is
	 *            true, the timecards' date must be earlier than this date.
	 * @param deptName The name of the Department in the timecard; if null,
	 *            ignore the timecard's department.
	 * @param statusFilter A comma-separated list of WeeklyStatus values; the
	 *            count of timecards will only include those with status values
	 *            in this list.
	 * @return The number of timecards that match the parameters given; may be
	 *         zero.
	 */
	public Integer findCountByWeekEndDateDeptSubmitted(Production prod, Project project, Date weDate,
			String deptName, String statusFilter) {

		String query = "select count(id) from WeeklyTimecard wtc where " +
				PROD_ID + " = ? and " +
				APPROVER_ID + " is not null and " + // ignore TCs rejected to employee
				START_FORM + " is not null "; // avoid retrieving the Global Preferences timecards

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		valueList.add(weDate);
		query += " and " + END_DATE + " < ?";

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		if (deptName != null) {
			valueList.add(deptName);
			query += " and " + DEPT_NAME + " = ? ";
		}

		if (statusFilter != null) {
			query += " and " + STATUS + " in (" + statusFilter + ") ";
		}

		return findCount(query, valueList.toArray()).intValue();
	}

	/**
	 * Find any WeeklyTimecard that matches the given parameters of Production, week-ending date,
	 * userAccount, and optional status.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date -- to match weeklyTimecard.endDate.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param userAccount The User's LS account number -- to match weeklyTimecard.userAccount.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard's.
	 */
	public List<TimecardEntry> findByWeekEndDateAccountTCE(Production prod, Project project, Date weDate, TimecardRange range, String userAccount, String statusFilter) {

		return createTCElist(findByWeekEndDateAccount(prod, project, weDate, range, userAccount, statusFilter));
	}

	/**
	 * Find the weeklyTimecards that match the given week-end date, account number, and batch, and
	 * generate TimecardEntry's from the list.
	 *
	 * @param prod The Production that the time cards apply to.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-end date to match, or null if any date matches.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param userAcct The accountNumber field to match, or null if any user account matches.
	 * @param batch The WeeklyBatch of interest; if null, the timecard must be un-batched.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, list of TimecardEntry's, each one representing a
	 *         WeeklyTimecard that matched the given criteria.
	 */
	private List<WeeklyTimecard> findByWeekEndDateAccountBatch(Production prod, Project project, Date weDate,
			TimecardRange range, String userAcct, WeeklyBatch batch, String statusFilter) {

		return findByWeekEndDateAccountBatchDept(prod, project, weDate, range, userAcct, batch, null, statusFilter, null);
	}

	/**
	 * Find the weeklyTimecards that match the given week-end date, account number, and batch, and
	 * generate TimecardEntry's from the list.
	 *
	 * @param prod The Production that the time cards apply to.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-end date to match, or null if any date matches.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param userAcct The accountNumber field to match, or null if any user account matches.
	 * @param batch The WeeklyBatch of interest; if null, the timecard must be un-batched.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, list of TimecardEntry's, each one representing a
	 *         WeeklyTimecard that matched the given criteria.
	 */
	public List<TimecardEntry> findByWeekEndDateAccountBatchTCE(Production prod, Project project, Date weDate,
			TimecardRange range, String userAcct, WeeklyBatch batch, String statusFilter) {

		return createTCElist(findByWeekEndDateAccountBatch(prod, project, weDate, range, userAcct, batch, statusFilter));
	}

	/**
	 * Find all WeeklyTimecard`s that matches the given parameters of Production, week-ending date,
	 * userAccount (if not null), batch, and optional status.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date -- to match weeklyTimecard.endDate; or null if any date
	 *            matches.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param userAcct The User's LS account number -- to match weeklyTimecard.userAccount. If null,
	 *            the field is ignored and no filtering is done by user account number.
	 * @param batch The WeeklyBatch that the selected timecards should belong to. If null, then the
	 *            timecard should be un-batched.
	 * @param deptName The department name to match, or null if any department matches.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @param order An optional list of fields to be used for ordering the result. It should be in
	 *            SQL format, without the leading " order by " text.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard's.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyTimecard> findByWeekEndDateAccountBatchDept(Production prod, Project project, Date weDate,
			TimecardRange range, String userAcct, WeeklyBatch batch, String deptName, String statusFilter, String order) {

		String query = "from WeeklyTimecard wtc where " +
				START_FORM  + " is not null and " +
				PROD_ID + " = ? and " +
				WEEKLY_BATCH + (batch == null ? " is null " : " = ? ");

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		if (batch != null) {
			valueList.add(batch);
		}

		if (weDate != null) {
			query += " and " + createDateClause(valueList, weDate, range);
		}

		if (deptName != null) {
			valueList.add(deptName);
			query += " and wtc.deptName = ? ";
		}

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		if (userAcct != null) {
			valueList.add(userAcct);
			query += " and wtc." + USER_ACCOUNT + " = ? ";
		}

		if (statusFilter != null) {
			query += " and wtc." + STATUS + " in (" + statusFilter + ") ";
		}

		if (order != null) {
			query += " order by " + order;
		}

		return find(query, valueList.toArray());
	}

	/**
	 * Find all WeeklyTimecard`s that matches the given parameters of Production, week-ending date,
	 * userAccount (if not null), batch, and optional status.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date -- to match weeklyTimecard.endDate.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param userAcct The User's LS account number -- to match weeklyTimecard.userAccount. If null,
	 *            the field is ignored and no filtering is done by user account number.
	 * @param batch The WeeklyBatch that the selected timecards should belong to. If null, then the
	 *            timecard should be un-batched.
	 * @param deptName The department name to match, or null if any department matches.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard's.
	 */
	public List<TimecardEntry> findByWeekEndDateAccountBatchDeptTCE(Production prod, Project project, Date weDate,
			TimecardRange range, String userAcct, WeeklyBatch batch, String deptName, String statusFilter) {

		return createTCElist(
				findByWeekEndDateAccountBatchDept(prod, project, weDate, range, userAcct, batch, deptName, statusFilter, null));
	}

	/**
	 * Find the weeklyTimecards that match the given week-end date, user account number, and
	 * department name, and generate TimecardEntry's from the list.
	 *
	 * @param prod The Production that the time cards apply to; if null, ignore the timecard's
	 *            Production -- timecards from any production are allowed.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-end date to match; if null, any date is allowed.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @param userAcct The accountNumber field to match, or null if any user account matches.
	 * @param deptName The department name to match, or null if any department matches.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @param order An optional list of fields to be used for ordering the result. It should be in
	 *            SQL format, without the leading " order by " text.
	 * @return A non-null, but possibly empty, list of TimecardEntry's, each one representing a
	 *         WeeklyTimecard that matched the given criteria.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyTimecard> findByWeekEndDateAccountDept(Production prod, Project project, Date weDate,
			TimecardRange range, String userAcct, String deptName, String statusFilter, String order) {
		String query = "from WeeklyTimecard wtc where ";

		List<Object> valueList = new ArrayList<>();

		String join = ""; // joining string; becomes " and " once we add first
							// clause.

		if (prod == null) {
			// abnormal; prevent retrieval across all productions
			return new ArrayList<>();
		}

		query += PROD_ID + " = ?  ";
		valueList.add(prod.getProdId());
		join = " and ";

		if (weDate != null) {
			query += join + createDateClause(valueList, weDate, range);
			join = " and ";
		}

		if (statusFilter != null) {
			query += join + STATUS + " in (" + statusFilter + ") ";
			join = " and ";
		}

		if (project != null) {
			valueList.add(project);
			query += join + " wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
			join = " and ";
		}

		if (deptName != null) {
			valueList.add(deptName);
			query += join + DEPT_NAME + " = ? ";
			join = " and ";
		}

		if (userAcct == null) {
			// if no account specified, we need to restrict to "real" timecards,
			// to avoid retrieving the Global Preferences timecards.
			query += join + START_FORM + " is not null ";
			if (order != null) {
				query += " order by " + order;
			}
		}
		else {
			valueList.add(userAcct);
			query += join + USER_ACCOUNT + " = ? ";
			if (order != null) {
				query += " order by " + order;
			}
			else {
				query += " order by " + END_DATE + " desc, " + ADJUSTED + " desc, id desc ";
			}
		}

		return find(query, valueList.toArray());
	}

	/**
	 * Find the weeklyTimecards that match the given week-end date, user account number, and
	 * department name, and generate TimecardEntry's from the list.
	 *
	 * @param prod The Production that the time cards apply to; if null, ignore the timecard's
	 *            Production -- timecards from any production are allowed.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-end date to match; if null, any date is allowed.
	 * @param prior True iff all timecards range to the given date should be included.
	 * @param userAcct The accountNumber field to match, or null if any user account matches.
	 * @param deptName The department name to match, or null if any department matches.
	 * @param statusFilter The list of WeeklyStatus values to filter on, or null if status is not a
	 *            filter criteria.
	 * @param order An optional list of fields to be used for ordering the result. It should be in
	 *            SQL format, without the leading " order by " text.
	 * @return A non-null, but possibly empty, list of TimecardEntry's, each one representing a
	 *         WeeklyTimecard that matched the given criteria.
	 */
	public WeeklyTimecard findByWeekEndDateAccountDeptStartForm(Production prod, Project project, Date weDate,
			boolean prior, String userAcct, Integer deptId, StartForm startForm, String statusFilter) {
		String query = "from WeeklyTimecard wtc where ";

		List<Object> valueList = new ArrayList<>();

		String join = " and "; // joining string; becomes " and " once we add first
							// clause.
		query += PROD_ID + " = ?  ";
		valueList.add(prod.getProdId());

		query += join + START_FORM;
		if(startForm != null) {
			valueList.add(startForm);
			query += " = ? ";
		}
		else {
			query += " is null ";
		}

		if (weDate != null) {
			valueList.add(weDate);
			if (prior) {
				query += join + END_DATE + " < ?";
			}
			else {
				query += join + END_DATE + " = ?";
			}
			join = " and ";
		}

		if (statusFilter != null) {
			query += join + STATUS + " in (" + statusFilter + ") ";
			join = " and ";
		}

		if (project != null) {
			valueList.add(project);
			query += join + " wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
			join = " and ";
		}

		if (deptId != null) {
			valueList.add(deptId);
			query += join + DEPT_ID + " = ? ";
			join = " and ";
		}

		valueList.add(userAcct);
		query += join + USER_ACCOUNT + " = ? ";

		return findOne(query, valueList.toArray());
	}

	public List<TimecardEntry> findByWeekEndDateAccountDeptTCE(Production prod, Project project, Date weDate,
			TimecardRange range, String userAcct, String deptName, String statusFilter) {

		return createTCElist(findByWeekEndDateAccountDept(prod, project, weDate, range, userAcct, deptName, statusFilter, null));
	}

	/**
	 * Find any WeeklyTimecard that matches the given parameters of Production, week-ending date,
	 * userAccount, and jobClass (occupation). This is used by the TimecardCreator class to
	 * determine if a timecard already exists that matches a particular StartForm for a given week;
	 * and by the HTG code to find the prior week's timecard for checking Saturday-to-Sunday
	 * conditions.
	 * <p>
	 * The list is sorted in descending order by date, then ADJUSTED flag, then descending by id. If
	 * there are no adjusted timecards, there should only be a single entry in the returned list.
	 * <p>
	 * If there's more than one timecard in the list, all except one should have the Adjusted flag
	 * on. In this case, the first entry in the list will be the most recently created Adjusted
	 * timecard.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date -- to match weeklyTimecard.endDate.
	 * @param userAcct The User's LS account number -- to match weeklyTimecard.userAccount.
	 * @param occupation The role or occupation -- to match weeklyTimecard.occupation.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard's.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyTimecard> findByWeekEndDateAccountOccupation(Production prod, Project project, Date weDate,
			String userAcct, String occupation) {

		String query = "from WeeklyTimecard wtc where " +
				PROD_ID + " = ?  ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		if (weDate != null) {
			valueList.add(weDate);
			query += " and " + END_DATE + " = ?";
		}

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		if (occupation != null) {
			valueList.add(occupation);
			query += " and " + OCCUPATION + " = ? ";
		}

		if (userAcct == null) {
			// if no account specified, we need to restrict to "real" timecards,
			// to avoid retrieving the Global Preferences timecards.
			query += " and " + START_FORM + " is not null ";
		}
		else {
			valueList.add(userAcct);
			query += " and " + USER_ACCOUNT + " = ? ";
		}

		query += " order by " + END_DATE + " desc, " + ADJUSTED + " desc, id desc ";

		return find(query, valueList.toArray());
	}

	/**
	 * Find the timecards for a given StartForm with a given week-ending date.
	 *
	 * @param date The week-ending date of the timecards to be found.
	 * @param sf The StartForm associated with the timecards to be found.
	 * @return A non-null, but possibly empty, List of WeeklyTimecard`s matching the given
	 *         parameters.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyTimecard> findByWeekEndDateStartForm(Date date, StartForm sf) {

		String query = "from WeeklyTimecard wtc where " +
				END_DATE + " = ? and " +
				START_FORM + " = ? ";

		Object[] values = { date, sf };

		return find(query, values);
	}

	/**
	 * Find all the WeeklyTimecard objects from the given production with the given week-ending date
	 * that are NOT in any batch.
	 *
	 * @param prod The Production whose timecards are to be retrieved.
	 * @param weDate The week-ending date of the timecards to be retrieved.
	 * @param range Controls which timecards should be included relative to the
	 *            given date; 'EXACT' if the timecards must match the given
	 *            date; 'PRIOR' if they can be any date earlier than that given;
	 *            'WEEK' if they can be the same date or any date in the prior 6
	 *            days (that week); 'ANY' if the date is irrelevant ('weDate'
	 *            should be null).
	 * @return A non-null, but possibly empty, List of WeeklyBatch instances matching the above
	 *         description.
	 */
	public List<WeeklyTimecard> findByWeekEndDateUnbatched(Production prod, Project project, Date weDate, TimecardRange range) {

		return findByWeekEndDateAccountBatch(prod, project, weDate, range, null, null, null);
	}

	/**
	 * Find all the WeeklyTimecard objects from the given production that are NOT in any batch.
	 *
	 * @param prod The Production whose timecards are to be retrieved.
	 * @return A non-null, but possibly empty, List of WeeklyBatch instances matching the above
	 *         description.
	 */
	public List<WeeklyTimecard> findByProductionUnbatched(Production prod, Project project) {

		return findByUserAccountBatch(prod, project, null, null, null);
	}

	/**
	 * Find all the timecards that are waiting for approval by a specific Contact, within a
	 * particular Production and Department. This is used by the Approval Dashboard to find
	 * timecards waiting for "out of line" approvers.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param contact The Contact of interest.
	 * @param deptName The name of the Department of interest.
	 * @param userAcct Either null or a user account number (User.accountNumber). If null, this
	 *            parameter is ignored. If not null, only those timecards that match this account
	 *            number will be returned.
	 * @return A non-null, but possibly empty, List of TimecardEntry`s with the corresponding
	 *         WeeklyTimecard`s that match the search criteria.
	 */
	public List<TimecardEntry> findByContactDeptAccountTCE(Production prod, Project project, Contact contact, String deptName, String userAcct) {

		return findByWeekEndDateContactDeptAccountTCE(prod, project, null, TimecardRange.ANY, null,
				contact, deptName, userAcct);
	}

	/**
	 * Find the WeeklyTimecard used for storing Global Update parameters, and which matches the
	 * given parameters of Production, week-ending date, and userAccount.
	 *
	 * @param prod The Production of interest.
	 * @param weDate The week-ending date -- to match weeklyTimecard.endDate.
	 * @param userAcct The timecard "userAccount" field. For Global update records, this string
	 *            includes the production id, and, for Commercial productions, the project id.
	 * @return The matching WeeklyTimecard, or null if no match was found.
	 */
	public WeeklyTimecard findGlobalByWeekEndDateAccount(Production prod, Date weDate,
			String userAcct) {

		String query = "from WeeklyTimecard wtc where " +
				START_FORM  + " is null and " +
				PROD_ID + " = ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		valueList.add(weDate);
		query += " and wtc." + END_DATE + " = ?";

		valueList.add(userAcct);
		query += " and wtc." + USER_ACCOUNT + " = ? ";

		return findOne(query, valueList.toArray());
	}

	/**
	 * Creates the appropriate SQL clause for date restriction on a timecard
	 * query. Note that the returned clause uses 'wtc' as a reference to the
	 * WeeklyTimecard table.
	 *
	 * @param valueList The list of values (parameters) to be passed on the
	 *            query execution.
	 * @param weDate The date value being tested.
	 * @param range The range qualifier.
	 * @return The additional clause(s) for restricting the search based on the
	 *         date and 'range' parameters.
	 */
	private String createDateClause(List<Object> valueList, Date weDate, TimecardRange range) {
		String query;
		valueList.add(weDate);
		if (range == TimecardRange.PRIOR) {
			query = " wtc." + END_DATE + " < ?";
		}
		else if (range == TimecardRange.EXACT) {
			query = " wtc." + END_DATE + " = ?";
		}
		else if (range == TimecardRange.WEEK) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(weDate);	// last date in week
			cal.add(Calendar.DAY_OF_WEEK, -6); // = first date in week
			Date date = cal.getTime();
			valueList.add(date);
			query = " wtc." + END_DATE + " <= ? and  wtc." + END_DATE + " >= ? ";
		}
		else {
			query = "";
		}
		return query;
	}

	/**
	 * Take a List of WeeklyTimecard and turn it into a List of TimecardEntry.
	 *
	 * @param wtcs The WeeklyTimecard`s to be encapsulated.
	 * @return A non-null List containing one TimecardEntry instance for each instance of
	 *         WeeklyTimecard in wtcs.
	 */
	private List<TimecardEntry> createTCElist(List<WeeklyTimecard> wtcs) {
		List<TimecardEntry> list = new ArrayList<>();
		for (WeeklyTimecard wtc : wtcs) {
			list.add(new TimecardEntry(wtc));
		}
		return list;
	}

	// /**
	// * Determine if any weeklyTimecards that match the given week-end date and
	// * department name exist.
	// *
	// * @param prod The Production that the time cards apply to.
	// * @param weDate The week-end date to match
	// * @param deptName The department name to match
	// * @return True iff at least one WeeklyTimecard matched the given
	// criteria.
	// */
	// public boolean existsWeekEndDate(Production prod, Date weDate, String
	// deptName) {
	// String query = "select count(id) from WeeklyTimecard where " +
	// PROD_ID + " = ? and " +
	// END_DATE + " = ? and " +
	// DEPT_NAME + " = ? ";
	// Object[] values = {prod.getProdId(), weDate, deptName};
	// return findCount(query, values) > 0;
	// }

	/**
	 * Determine if the given User has any timecards within a specific Production. Note that this
	 * check is across all StartForm`s and Project`s. It is currently used to determine if a Contact
	 * may be removed from a Production.
	 *
	 * @param prod The Production of interest; if null, an error Event is logged, and false is
	 *            returned.
	 * @param userAccount The LS account number (i.e., User.accountNumber) of interest.
	 * @return True iff the give user has at least one timecard in the specified Production.
	 */
	public boolean existsUserAccount(Production prod, String userAccount) {
		if (prod == null) {
			EventUtils.logError("Production reference null.", new Throwable());
			return false;
		}
		String query = "from WeeklyTimecard where " +
				PROD_ID + " = ? " +
				" and " + USER_ACCOUNT + " = ? ";
		Object[] values = { prod.getProdId(), userAccount };
		return exists(query, values);
	}

	/**
	 * Determine if any un-batched timecards exist for the given Production and project.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @return True iff at least one timecard exists matching the given criteria and which is not
	 *         assigned to a WeeklyBatch.
	 */
	public boolean existsUnbatched(Production prod, Project project) {
		String query = "from WeeklyTimecard wtc where " +
				PROD_ID + "= ? and " +
				WEEKLY_BATCH + " is null and " +
				START_FORM  + " is not null and " + // avoid "global preference" TCs
		STATUS + " <> 'VOID' ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		return exists(query, valueList.toArray());
	}

	/**
	 * Determine if any timecards exist for the given Production, project, and week-ending date.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date of interest; if null, then any date is acceptable.
	 * @return True iff at least one timecard exists matching the given criteria.
	 */
	public boolean existsWeekEndDate(Production prod, Project project, Date weDate) {
		return existsWeekEndDateAccountOccupation(prod, project, weDate, null, null);
	}

	/**
	 * Determine if any batched timecards exist for the given Production, project, and week-ending
	 * date.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date of interest; if null, then any date is acceptable.
	 * @return True iff at least one timecard exists matching the given criteria which is also
	 *         assigned to a WeeklyBatch.
	 */
	public boolean existsWeekEndDateBatched(Production prod, Project project, Date weDate) {
		String query = "from WeeklyTimecard wtc where " +
				PROD_ID + "= ? and " +
				WEEKLY_BATCH + " is not null and " +
				START_FORM  + " is not null and " + // avoid "global preference" TCs
		STATUS + " <> 'VOID' ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		if (weDate != null) {
			valueList.add(weDate);
			query += " and " + END_DATE + " = ?";
		}

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		return exists(query, valueList.toArray());
	}

	/**
	 * Determine if any un-batched timecards exist for the given Production, project, and
	 * week-ending date.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending date of interest.
	 * @return True iff at least one timecard exists matching the given criteria and which is not
	 *         assigned to a WeeklyBatch.
	 */
	public boolean existsWeekEndDateUnbatched(Production prod, Project project, Date weDate) {
		String query = "from WeeklyTimecard wtc where " +
				PROD_ID + "= ? and " +
				WEEKLY_BATCH + " is null and " +
				START_FORM  + " is not null and " + // avoid "global preference" TCs
		STATUS + " <> 'VOID' ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		valueList.add(weDate);
		query += " and " + END_DATE + " = ?";

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		return exists(query, valueList.toArray());
	}

	/**
	 * Determine if the given user has a timecard with the specified week-ending date in any
	 * production.  Used by Mobile timecards. This will check a range of the preceding 6 days,
	 * to accommodate possible floating (non-Saturday) week-end-day projects.
	 *
	 * @param weDate The week-ending date of interest.
	 * @param userAcct The user's account number.
	 * @return True if the user has at least one timecard with the given week-ending date that is
	 *         not VOID.
	 */
	public boolean existsWeekEndDateAccount(Date weDate, String userAcct) {
		String query = "from WeeklyTimecard wtc where " +
				STATUS + " <> 'VOID' ";

		List<Object> valueList = new ArrayList<>();

		if (weDate != null) {
			query += " and " + createDateClause(valueList, weDate, TimecardRange.WEEK);
		}

		if (userAcct == null) {
			// if no account specified, we need to restrict to "real" timecards,
			// to avoid retrieving the Global Preferences timecards.
			query += " and " + START_FORM + " is not null ";
		}
		else {
			valueList.add(userAcct);
			query += " and " + USER_ACCOUNT + " = ? ";
		}

		return exists(query, valueList.toArray());
	}

	/**
	 * Determine if any specific WeeklyTimecard`s exist, for the given User in the specified
	 * Production, with the given week-ending date and occupation.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The week-ending Date of the record to be searched for; if null, then any date
	 *            is acceptable.
	 * @param userAcct The user account number of interest; if null, then a timecard for any user is
	 *            acceptable.
	 * @param occupation The occupation, typically matching a Role name; if null, then any
	 *            occupation (including no occupation) is acceptable.
	 * @return True iff the given user already has one or more WeeklyTimecard`s in the database for the given
	 *         week-ending date and occupation.
	 */
	public boolean existsWeekEndDateAccountOccupation(Production prod, Project project, Date weDate,
			String userAcct, String occupation) {
		String query = "from WeeklyTimecard wtc where " +
				PROD_ID + "= ? and " +
				STATUS + " <> 'VOID' ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		if (weDate != null) {
			valueList.add(weDate);
			query += " and " + END_DATE + " = ?";
		}

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		if (occupation != null) {
			valueList.add(occupation);
			query += " and " + OCCUPATION + " = ? ";
		}

		if (userAcct == null) {
			// if no account specified, we need to restrict to "real" timecards,
			// to avoid retrieving the Global Preferences timecards.
			query += " and " + START_FORM + " is not null ";
		}
		else {
			valueList.add(userAcct);
			query += " and " + USER_ACCOUNT + " = ? ";
		}

		return exists(query, valueList.toArray());
	}

	/**
	 * Determine if any timecards exist within the given Project.
	 *
	 * @param project the Project of interest.
	 * @return true iff at least one timecard exists whose associated StartForm is linked to the
	 *         given Project.
	 */
	public boolean existsTimecardsForProject(Project project) {
		String query = "from WeeklyTimecard where " +
				" startForm.project = ? ";
		return exists(query, project);
	}

	/**
	 * Determine if any timecards exist for the given Production and (optionally) project.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @return True iff at least one timecard exists matching the given criteria and which is not
	 *         assigned to a WeeklyBatch.
	 */
	public boolean existsForProduction(Production prod, Project project) {
		String query = "from WeeklyTimecard wtc where " +
				PROD_ID + "= ? and " +
				START_FORM  + " is not null "; // avoid "global preference" TCs

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());

		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		return exists(query, valueList.toArray());
	}

	/**
	 * Determine if any WeeklyTimecard exists that is associated with the given StartForm.id value.
	 *
	 * @param startFormId The database id of the StartForm of interest.
	 * @return True iff at least one WeeklyTimecard in the database has the given startFormId value.
	 */
	public boolean existsTimecardsForStartForm(Integer startFormId) {
		String query = "from WeeklyTimecard where " +
				"startForm.id = ? ";
		Object[] values = { startFormId };
		return exists(query, values);
	}

	/**
	 * Determine if any timecards exist for the given date, associated with the
	 * specified StartForm.
	 *
	 * @param date The date of interest.
	 * @param sf The StartForm of interest.
	 * @return True iff at least one WeeklyTimecard in the database has matching
	 *         values.
	 */
	public boolean existsWeekEndDateStartForm(Date date, StartForm sf) {
		String query = "from WeeklyTimecard wtc where " +
				END_DATE + " = ? and " +
				START_FORM + " = ? ";

		Object[] values = { date, sf };
		return exists(query, values);
	}

	/**
	 * Determine if a timecard exists for the week preceding the given timecard, for the same
	 * person, with the same occupation, and which has at least some hours recorded.
	 *
	 * @param wtc The WeeklyTimecard of interest.
	 * @return True iff this person's timecard from the preceding week has a positive total for the
	 *         week's hours.
	 */
	public boolean existsPriorNonZeroTimecard(WeeklyTimecard wtc) {

		String prodId = wtc.getProdId();
		Date weDate = TimecardUtils.calculatePriorWeekEndDate(wtc.getEndDate());
		String userAcct = wtc.getUserAccount();
		String occupation = wtc.getOccupation();

		String query = "select id from WeeklyTimecard wtc where " +
				PROD_ID + " = ?  " +
				" and " + END_DATE + " = ?" +
				" and " + OCCUPATION + " = ? " +
				" and " + USER_ACCOUNT + " = ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prodId);
		valueList.add(weDate);
		valueList.add(occupation);
		valueList.add(userAcct);

		Project project = wtc.getStartForm().getProject();
		if (project != null) {
			valueList.add(project);
			query += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}
		query += " order by " + ADJUSTED + " desc, id desc ";

		boolean bRet = false;
		@SuppressWarnings("unchecked")
		List<Integer> ids = find(query, valueList.toArray());
		if (ids.size() > 0) {
			Integer id = ids.get(0);
			WeeklyTimecard tc = findById(id);
			TimecardCalc.calculateWeeklyTotals(tc);
			if (tc.getTotalHours().signum() > 0) {
				bRet = true;
			}
		}

		return bRet;
	}

	/**
	 * Find the average number of daily hours from all the DailyTime entries for the given
	 * Production, Project, and week-ending date.
	 *
	 * @param prod The Production containing the timecards whose DailyTime values are to be used in
	 *            the calculation.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The desired week-end date.
	 * @return A List of 7 entries giving the average number of hours for the respective day of the
	 *         week, starting with Sunday.
	 */
	public List<Double> findAverageHours(Production prod, Project project, Date weDate) {
		return findAverageHoursDept(prod, project, weDate, null);
	}

	/**
	 * Find the average number of daily hours from all the DailyTime entries for the week-end date
	 * given and (optionally) from the given department.
	 *
	 * @param prod The Production containing the timecards whose DailyTime values are to be used in
	 *            the calculation.
	 * @param project The Project of interest; if null, ignore the timecard's Project affiliation;
	 *            should be non-null only for Commercial productions.
	 * @param weDate The desired week-end date.
	 * @param deptName The desired department.
	 * @return A List of 7 entries giving the average number of hours for the respective day of the
	 *         week, starting with Sunday.
	 */
	public List<Double> findAverageHoursDept(Production prod, Project project, Date weDate, String deptName) {
		String query = "select avg(d.hours) from DailyTime d, WeeklyTimecard w " +
				" where d.weeklyTimecard = w and " +
				" w.startForm is not null and " +
				" w.prodId = ? and " +
				" w.endDate = ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());
		valueList.add(weDate);

		if (deptName != null) {
			valueList.add(deptName);
			query += " and w.deptName = ? ";
		}

		if (project != null) {
			valueList.add(project);
			query += " and w." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}

		query += " group by d.dayNum " +
				" order by d.dayNum";

		@SuppressWarnings("unchecked")
		List<Double> list = find(query, valueList.toArray());

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) == null) {
				list.set(i, new Double(0));
			}
		}
		while (list.size() < 7) {
			list.add(new Double(0));
		}
		return list;
	}

	/**
	 * Find the earliest date of any existing timecard within the given Production.
	 *
	 * @param prod The Production of interest.
	 * @return The earliest week-ending date of all the timecards that are in the specified
	 *         Production; returns null if there are no timecards in the Production.
	 */
	@SuppressWarnings("unchecked")
	public Date findFirstDateByProduction(Production prod) {
		Date date = null;
		String query = "select min(endDate) from WeeklyTimecard wtc where " +
				START_FORM  + " is not null and " +
				PROD_ID + " = ? ";

		List<Date> dates = find(query, prod.getProdId());
		if (dates != null && dates.size() > 0) {
			date = dates.get(0);
		}

		return date;
	}

	/**
	 * Find the earliest date of any existing timecard within the given Project.
	 *
	 * @param project The Project to be checked for timecards; may not be null.
	 * @return The earliest week-ending date of all the timecards that are in the specified Project;
	 *         returns null if there are no timecards in the Project.
	 */
	@SuppressWarnings("unchecked")
	public Date findFirstDateByProject(Project project) {
		Date date = null;
		String query = "select min(endDate) from WeeklyTimecard wtc where " +
				" wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";

		List<Date> dates = find(query, project);
		if (dates != null && dates.size() > 0) {
			date = dates.get(0);
		}

		return date;
	}

	/**
	 * Determine if any WeeklyTimecard`s reference the given Department.
	 *
	 * @param dept
	 * @return True iff at least one WeeklyTimecard references the specified Department.
	 */
	public boolean referencesDept(Department dept) {
		String query = "from WeeklyTimecard where department = ?";
		return exists(query, dept);
	}

	/**
	 * Find the Timecards between specified date range.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest, null for non-commercial projects.
	 * @param startDate
	 * @param endDate
	 * @return List of Timecards.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyTimecard> findTimecardsBetweenDates(Production prod, Project project, Date startDate, Date endDate) {
		String queryString = "from WeeklyTimecard wtc where " +
				PROD_ID + " = ? and " +
				" wtc." + END_DATE + " between ? and ? ";
		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getProdId());
		valueList.add(startDate);
		valueList.add(endDate);
		if (project != null) {
			valueList.add(project);
			queryString += " and wtc." + START_FORM + "." + StartFormDAO.PROJECT + " = ? ";
		}
		return find(queryString, valueList.toArray());
	}

	/**
	 * Handle Submit processing for a WeeklyTimecard. This includes determining the next-approver
	 * field, adding the Submit event (signature) to the timecard, sending appropriate
	 * notification(s), and updating the timecard in the database.
	 *
	 * @param wtc The timecard being submitted.
	 * @param approve True iff this is called via the "submit & approve" dialog box.
	 * @param subType The "submit type" which indicates, for example, if the timecard was submitted
	 *            by other than the employee, and if so, why (e.g., a paper timecard was submitted).
	 * @return A (possibly refreshed) reference to the WeeklyTimecard object, or null if the submit
	 *         process failed.
	 */
	@Transactional
	public WeeklyTimecard submit(WeeklyTimecard wtc, boolean approve, TimecardSubmitType subType) {
		try {
			Production prod = TimecardUtils.findProduction(wtc);
			Project project = TimecardUtils.findProject(prod, wtc);
			User submitUser = SessionUtils.getCurrentUser();
			Contact submitter = ContactDAO.getInstance().findByUserProduction(submitUser, prod);
			boolean selfSubmit = submitUser.getAccountNumber().equals(wtc.getUserAccount());
			boolean routeToProd = false;
			if (selfSubmit) {
				routeToProd = ApproverUtils.isProdOrProjectApprover(submitter, project);
			}
			Integer appId = null;
			if (wtc.getUnderContract() || routeToProd) {
				if (project != null) { // use project approver chain
					if (project.getApprover() != null) {
						appId = project.getApprover().getId();
					}
				}
				if (appId == null && prod.getApprover() != null) {
					appId = prod.getApprover().getId();
				}
			}
			else {
				appId = ApproverUtils.findFirstApproverId(prod, project, wtc.getDepartment());
			}
			log.info("submit, wtc=" + wtc.getId() + ", approver Id=" + appId +
					", self=" + selfSubmit + ", routeToProd=" + routeToProd);
			if (appId != null) {
				// update null and holiday day types as necessary
				TimecardUtils.fillDayTypes(wtc);
				// set Payroll's NDM flags to user's NDM flags, and save all
				// changes
				for (DailyTime dt : wtc.getDailyTimes()) {
					dt.setNonDeductMealPayroll(dt.getNonDeductMeal());
					dt.setNonDeductMeal2Payroll(dt.getNonDeductMeal2());
					attachDirty(dt);
				}
				// Create and save SUBMIT event
				Integer toUserId = ApproverDAO.getInstance().findUserId(appId);
				if (subType == null) {
					TimecardEventDAO.getInstance().createEvent(wtc, TimedEventType.SUBMIT,
							TimecardEvent.FIELD_USER, toUserId);
				}
				else {
					TimecardEventDAO.getInstance().createEvent(wtc, subType, toUserId);
				}
				// and update timecard...
				if (wtc.getStatus() == ApprovalStatus.OPEN) {
					wtc.setStatus(ApprovalStatus.SUBMITTED);
				}
				else {
					wtc.setStatus(ApprovalStatus.RESUBMITTED);
				}
				if (wtc.getPayJobs().size() == 0) { // add a Job table
					addPayJob(wtc);
				}
				wtc.setApproverId(appId);
				wtc.setMarkedForApproval(false);
				if (approve) { // combination Submit & Approve
					// No need to verify user is dept/project approver (done by caller)
					Integer nextId = ApprovePromptBean.getInstance().getApproverContactId();
					wtc = refresh(wtc);
					approveSub(wtc, nextId, false); // this saves the timecard
				}
				else {
					attachDirty(wtc);
				}
				Contact employee = null;
				if (!selfSubmit) {
					// submitter is different then employee on timecard...
					employee = ContactDAO.getInstance()
							.findByAccountNumAndProduction(wtc.getUserAccount(), prod);
				}
				ReportStyle reptStyle = ReportStyle.TC_SIMPLE;
				if (prod.getPayrollPref().getUseModelRelease()) {
					reptStyle = ReportStyle.TC_MODEL;
				}
				else if (prod.getType().isAicp()) {
					reptStyle = ReportStyle.TC_AICP;
				}
				String reportFile = TimecardPrintUtils.printTimecard(wtc, reptStyle, true/*show breakdown*/, false/*not for transfer*/);
				DoNotification.getInstance()
					.timecardSubmitted(employee, project, wtc.getEndDate(), submitter, reportFile);
				updateBatchStatus(wtc);
				if (selfSubmit) { // self-submitted
					employee = submitter;
				}

				// Check if this production has been set to calculate timecards on submission. This value
				// is set on the Prod Admin / Productions / Payroll page
				Production production = ProductionDAO.getInstance().refresh(wtc.getProduction());
				if (production.getPayrollPref().getCalcTimecardsOnSubmit()) {
					calcAndSaveTimecard(wtc);
				}

				notifyReady(wtc, employee, project); // send "ready for approval"
											// notification
			}
			else {
				wtc = null; // give error indication to caller
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			wtc = null; // give error indication to caller
		}
		return wtc;
	}

	/**
	 * Perform the "Calc HTG" function on the current timecard, and persist
	 * it in the database.
	 */
	private void calcAndSaveTimecard(WeeklyTimecard weeklyTimecard) {
		TimecardService.calculateAllHtg(weeklyTimecard, false, false); // this does not persist changes
		removeDeletedPaylines(weeklyTimecard);
		attachDirty(weeklyTimecard); // Update to database
	}

	/**
	 * Handle Submit processing for a WeeklyTimecard of Tours Production. This includes
	 *  adding the Approve event (signature) to the timecard, sending appropriate
	 * notification(s), and updating the timecard in the database.
	 *
	 * @param wtc The timecard being submitted and approved.
	 * @return A (possibly refreshed) reference to the WeeklyTimecard object, or null if the submit
	 *         process failed.
	 */
	@Transactional
	public WeeklyTimecard submitToursTimecard(WeeklyTimecard wtc) {
		try {
			//Production prod = TimecardUtils.findProduction(wtc);
			//Project project = TimecardUtils.findProject(prod, wtc);
			//User submitUser = SessionUtils.getCurrentUser();
			//Contact submitter = ContactDAO.getInstance().findByUserProduction(submitUser, prod);
			//boolean selfSubmit = submitUser.getAccountNumber().equals(wtc.getUserAccount());
			log.info("submit, wtc=" + wtc.getId());
			// update null and holiday day types as necessary
			TimecardUtils.fillDayTypes(wtc);

			/* NDM is not applicable to Tours timecards
			for (DailyTime dt : wtc.getDailyTimes()) {
				dt.setNonDeductMealPayroll(dt.getNonDeductMeal());
				dt.setNonDeductMeal2Payroll(dt.getNonDeductMeal2());
				attachDirty(dt);
			}
			*/
			// Create and save APPROVE event - Tours timecards go direct to Approved status
			TimecardEventDAO.getInstance().createEvent(wtc, TimedEventType.APPROVE, TimecardEvent.FIELD_USER, null);
			// and update timecard...
			if (wtc.getSubmitable()) {
				wtc.setStatus(ApprovalStatus.APPROVED);
			}
			if (wtc.getPayJobs().size() == 0) { // add a Job table
				addPayJob(wtc);
			}
			wtc.setApproverId(null);
			wtc.setMarkedForApproval(false);
//			wtc.setUpdated(new Date());
			attachDirty(wtc);
			/* We are not planning on sending timecard PDFs to Tours employees
			Contact employee = null;
			if (! selfSubmit) {
				// submitter is different then employee on timecard...
				employee = ContactDAO.getInstance().findByAccountNumAndProduction(wtc.getUserAccount(), prod);
			}
			ReportStyle reptStyle = ReportStyle.TC_SIMPLE;
			if (prod.getType().isAicp()) {
				reptStyle = ReportStyle.TC_AICP;
			}
			String reportFile = TimecardPrintUtils.printTimecard(wtc, reptStyle, true);
			DoNotification.getInstance().timecardSubmitted(employee, project, wtc.getEndDate(), submitter, reportFile);
			*/

			//updateBatchStatus(wtc); // Timesheet code will update batch

			//if (selfSubmit) { // self-submitted
			//	employee = submitter;
			//}
			//notifyReady(wtc, employee, project); // send "ready for approval" notification
		}
		catch (Exception e) {
			EventUtils.logError(e);
			wtc = null; // give error indication to caller
		}
		return wtc;
	}

	/**
	 * Mark the given WeeklyTimecard approved, and set the Approver.id value of the next approver in
	 * the chain. This will be null if the WeeklyTimecard has received its final approval. An
	 * approval event is added to the history of the WeeklyTimecard. The updated timecard is saved.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param approverId If not null, the Contact.id value of the person who should be the next
	 *            approver for this WeeklyTimecard. If null, this method determines the next
	 *            approver based on the current approval chain for the given WeeklyTimecard.
	 * @return A (possibly refreshed) reference to the WeeklyTimecard object.
	 */
	@Transactional
	public WeeklyTimecard approve(WeeklyTimecard wtc, Integer approverId) {
		return (WeeklyTimecard)super.approve(wtc, approverId, false);
		}

	/**
	 * Mark the given WeeklyTimecard approved, and set the Approver.id value of the next approver in
	 * the chain. This will be null if the WeeklyTimecard has received its final approval. An
	 * approval event is added to the history of the WeeklyTimecard. The updated timecard is saved.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param approverId If not null, the Contact.id value of the person who should be the next
	 *            approver for this WeeklyTimecard. If null, this method determines the next
	 *            approver based on the current approval chain for the given WeeklyTimecard.
	 */
/*	private void approveSub(WeeklyTimecard wtc, Integer approverId) {
		byte outOfLine = 0;
		Contact contact = null; // Contact of new approver
		// Determine next approver
		Approver nextApprover = TimecardUtils.findNextApprover(wtc, null);
		if (approverId == null) { // use normal chain
			if (nextApprover != null) { // not at end of chain
				approverId = nextApprover.getId(); // this is Approver.id of
													// next one
				contact = nextApprover.getContact();
			}
		}
		else if (nextApprover != null && nextApprover.getContact() != null &&
				nextApprover.getContact().getId().equals(approverId)) {
			approverId = nextApprover.getId(); // use next one in chain
			contact = nextApprover.getContact();
		}
		else { // out-of-line approval: create new Approver instance
			outOfLine = TimecardEvent.DAY_OUT_OF_LINE; // remember for event
			contact = ContactDAO.getInstance().findById(approverId);
			Approver app = new Approver(contact, nextApprover, false);
			save(app);
			approverId = app.getId();
		}

		// NOW approverId is an Approver.id value!
		if (approverId == null) { // no more approvers in the chain ... has
									// final approval!
			wtc.setStatus(ApprovalStatus.APPROVED);
		}
		else if (wtc.getStatus() == ApprovalStatus.REJECTED ||
				wtc.getStatus() == ApprovalStatus.RECALLED) {
			wtc.setStatus(ApprovalStatus.RESUBMITTED);
		}
		Integer priorApproverId = wtc.getApproverId();
		wtc.setApproverId(approverId);

		// Create event
		createEvent(wtc, contact, priorApproverId, outOfLine);
		attachDirty(wtc);

		// If the prior Approver was "out of line" (un-shared), then delete it.
		if (priorApproverId != null) {
			Approver app = ApproverDAO.getInstance().findById(priorApproverId);
			if (app != null && !app.getShared()) {
				log.debug("deleting non-shared Approver, id=" + app.getId());
				delete(app);
			}
		}

		if (approverId != null) {
			notifyReady(wtc);
		}
		updateBatchStatus(wtc);
		log.info("TC approved, id=" + wtc.getId() + ", next appr=" + approverId);
	}
*/

	/**
	 * Create an APPROVE event for the given timecard. This is called from the
	 * ApprovableDAO superclass.
	 *
	 * @param wtc The timecard being approved.
	 * @param contact The Contact of the new approver.
	 * @param priorApproverId The Approver.id of the previous approver.
	 * @param outOfLine An indicator if the was an "out of line" approval; that
	 *            is, the new approver is not in the normal approval chain.
	 */
	@Override
	protected void createEvent(Approvable wtc, Contact contact, Integer priorApproverId,
			byte outOfLine) {
		Integer priorUserId = ApproverDAO.getInstance().findUserId(priorApproverId);
		Integer toUserId = null;
		if (contact != null) {
			toUserId = contact.getUserId();
		}
		TimecardEventDAO.getInstance().createEvent((WeeklyTimecard)wtc, TimedEventType.APPROVE,
				outOfLine, TimecardEvent.FIELD_USER, priorUserId, toUserId);

		((WeeklyTimecard)wtc).setMarkedForApproval(false);
	}

	/**
	 * Notify the new approver that a timecard is ready for their approval. This
	 * is called from the ApprovableDAO superclass.
	 *
	 * @param doc The timecard ready for approval.
	 */
	@Override
	protected void notifyReady(Approvable doc) {
		WeeklyTimecard wtc = (WeeklyTimecard)doc;
			Production prod = TimecardUtils.findProduction(wtc);
			Contact employee = ContactDAO.getInstance()
					.findByAccountNumAndProduction(wtc.getUserAccount(), prod);
			notifyReady(wtc, employee, TimecardUtils.findProject(prod, wtc));
		}

	/**
	 * Update the status of the given timecard and its batch, if it is in a
	 * batch. This is called from the ApprovableDAO superclass.
	 *
	 * @param wtc The timecard being updated.
	 */
	@Override
	protected void updateBatchStatus(Approvable doc) {
		WeeklyTimecard wtc = (WeeklyTimecard)doc;
		WeeklyBatchUtils.updateBatchStatus(wtc);
	}

	/**
	 * Reject a single WeeklyTimecard. A Reject event is added to the timecard's history; the
	 * timecard is unlocked, and its status is changed to REJECTED; the timecard's "next approver"
	 * is changed to refer to the Contact specified; and notifications are sent to the recipient of
	 * the rejected timecard and any approvers who will need to re-approve the timecard.
	 *
	 * @param wtc The WeeklyTimecard to be rejected.
	 * @param rejector The Contact of the person doing the Reject operation.
	 * @param sendToId The Contact.id value of the Contact who should be the "next approver" for the
	 *            timecard. If null, the timecard is set to have no "next approver", which means
	 *            that it is in the employee's possession, waiting to be (re)submitted.
	 * @param approverId The Approver.id value of the Approver which will point to the
	 *            "next approver" for the timecard. May be null.
	 * @param comment The reason for the rejection. May be null. If null or blank, no change is made
	 *            to the timecard's Comment field.
	 * @return A refreshed reference for the WeeklyTimecard, or null if an Exception is thrown
	 *         during processing.
	 */
	@Transactional
	public WeeklyTimecard reject(WeeklyTimecard wtc, Contact rejector, Integer sendToId,
			Integer approverId, String comment) {
		try {
			wtc = refresh(wtc);
			// Add the comment text to the timecard's Comment field.
			TimecardUtils.addComment(wtc, comment);
			// and update timecard...
			wtc.setStatus(ApprovalStatus.REJECTED);
			wtc.setMarkedForApproval(false);
			// clear the timecard's lock...
			log.debug("unlocking #" + wtc.getId() + "; locked by=" + wtc.getLockedBy());
			wtc.setLockedBy(null);

			Integer oldApproverId = wtc.getApproverId();
			// Change the approver, and update the timecard in the database...
			Contact sendTo = changeApprover(wtc, sendToId, approverId, TimedEventType.REJECT);

			// Send notification to recipient & other approvers;
			// 'sendTo' is null if going to employee - buildReject understands
			// this!
			Collection<Contact> approvers = TimecardUtils.createRejectNotify(wtc, sendTo, oldApproverId, null);
			if (sendTo == null) { // target is employee; get Contact value for
									// notification
				Production prod = TimecardUtils.findProduction(wtc);
				sendTo = ContactDAO.getInstance().findByAccountNumAndProduction(wtc.getUserAccount(), prod);
			}
			DoNotification no = DoNotification.getInstance();
			no.timecardRejected(wtc.getFirstName(), wtc.getLastName(), wtc.getEndDate(),
					rejector, sendTo, approvers, comment);
			updateBatchStatus(wtc);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			wtc = null;
		}

		return wtc;
	}

	/**
	 * Set the payroll status of a timecard to "LOCKED". This is the final change to a timecard.
	 * Note that the timecard must be in APPROVED status to be eligible for locking.
	 *
	 * @param wtc The timecard to be locked.
	 * @return The updated timecard, or null if there was a failure.
	 */
	@Transactional
	public WeeklyTimecard lockStatus(WeeklyTimecard wtc) {
		try {
			log.debug(wtc.getFirstName() + " " + wtc.getLastName() + ", " + wtc.getEndDate());
			wtc = refresh(wtc);
			wtc.setMarkedForApproval(false);
			if (wtc.getStatus() == ApprovalStatus.APPROVED) { // double check
																// status!
				wtc.setStatus(ApprovalStatus.LOCKED);
				// Create event & update timecard in db:
				TimecardEventDAO.getInstance().createEvent(wtc, TimedEventType.LOCK, (short) 0, null);
			}
			updateBatchStatus(wtc);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			wtc = null;
		}
		return wtc;
	}

	/**
	 * Recall a single WeeklyTimecard. A Recall event is added to the timecard's history; the
	 * timecard's status is changed to RECALLED; the timecard's "next approver" is changed to refer
	 * to the specified Contact; and notifications are sent to any approvers who will need to
	 * re-approve the timecard (not including the person doing the recall).
	 *
	 * @param wtc The WeeklyTimecard to be recalled.
	 * @param recallerId The Contact.id value of the Contact who is doing the recall, and who should
	 *            be made the "next approver" for the timecard.
	 * @return A refreshed reference for the WeeklyTimecard, or null if there is an Exception during
	 *         processing.
	 */
	@Transactional
	public WeeklyTimecard recall(WeeklyTimecard wtc, Integer recallerId) {
		try {
			wtc = refresh(wtc);
			// Update timecard...
			wtc.setStatus(ApprovalStatus.RECALLED);
			wtc.setMarkedForApproval(false);

			Integer oldApproverId = wtc.getApproverId();
			Contact recaller = changeApprover(wtc, recallerId, null, TimedEventType.RECALL);
			// Send notification to other approvers
			List<Contact> approvers = new ArrayList<>();;
			if (recaller == null && oldApproverId != null) { // employee did "recall"
				Production prod = TimecardUtils.findProduction(wtc);
				recaller = ContactDAO.getInstance().findByAccountNumAndProduction(wtc.getUserAccount(), prod);
				approvers = new ArrayList<>(); // only one approver to notify -
												// the current one:
				Approver oldApprover = ApproverDAO.getInstance().findById(oldApproverId);
				approvers.add(oldApprover.getContact());
			}
			else if (oldApproverId != null) {
				approvers = TimecardUtils.createRecallNotify(wtc, recaller, oldApproverId, null);
			}
			if (approvers.size() > 0) {
				Project proj = SessionUtils.getCurrentProject();
				if (proj == null) {
					Production prod = TimecardUtils.findProduction(wtc);
					proj = TimecardUtils.findProject(prod, wtc); // project if
																	// Commercial
					if (proj == null) {
						proj = prod.getDefaultProject(); // project if
															// TV/Feature
					}
				}
				DoNotification no = DoNotification.getInstance();
				no.timecardRecalled(proj, wtc.getFirstName(), wtc.getLastName(), wtc.getEndDate(), recaller, approvers);
			}
			updateBatchStatus(wtc);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			wtc = null;
		}
		return wtc;
	}

	/**
	 * Pull a single WeeklyTimecard -- that is, move it UP in the approval queue, bypassing the
	 * current approver, and (possibly) others in the chain up to the "puller". A Pull event is
	 * added to the timecard's history; the timecard's status remains unchanged; the timecard's
	 * "next approver" is changed to refer to the specified Contact; and notifications are sent to
	 * those approvers who were bypassed.
	 *
	 * @param wtc The WeeklyTimecard to be recalled.
	 * @param pullerId The Contact.id value of the Contact who is doing the pull, and who should be
	 *            made the "next approver" for the timecard.
	 * @return A refreshed reference for the WeeklyTimecard, or null if there is an Exception during
	 *         processing.
	 */
	@Transactional
	public WeeklyTimecard pull(WeeklyTimecard wtc, Integer pullerId) {
		try {
			wtc = refresh(wtc);
			// Save existing approver id
			Integer oldApproverId = wtc.getApproverId();
			Integer pullerApproverId = null;
			Project project = null;

			// Find the Approver entry for the given Contact (pullerId).
			// we want the "latest" one, so check the production approver chain
			// first.
			Production prod = TimecardUtils.findProduction(wtc);
			Approver app = searchChainContact(prod.getApprover(), pullerId);
			if (app == null) {
				// not a production approver, check project approver chain (if
				// any)
				project = TimecardUtils.findProject(prod, wtc);
				if (project != null) {
					app = searchChainContact(project.getApprover(), pullerId);
				}
			}
			if (app != null) {
				pullerApproverId = app.getId();
			}
			// If approver not found yet, changeApprover will find the dept
			// approver anyway,
			// no need for us to search for it.

			Contact puller = changeApprover(wtc, pullerId, pullerApproverId, TimedEventType.PULL);
			// Send notification to other approvers
			if (puller == null) { // shouldn't happen (employee can't do "pull")
				puller = ContactDAO.getInstance().findByAccountNumAndProduction(wtc.getUserAccount(), prod);
			}
			Project tcProj = TimecardUtils.findProject(prod, wtc);
			List<Contact> approvers = ApproverUtils.createPullNotify(puller, tcProj, oldApproverId, null);
			if (approvers.size() > 0) {
				app = ApproverDAO.getInstance().findById(oldApproverId);
				String approverName = app.getContact().getUser().getFirstNameLastName();
				DoNotification no = DoNotification.getInstance();
				no.timecardPulled(wtc.getFirstName(), wtc.getLastName(), wtc.getEndDate(),
						puller, approverName, approvers);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			wtc = null;
		}
		return wtc;
	}

	/**
	 * Void a single WeeklyTimecard. A Void event is added to the timecard's history; the timecard's
	 * status is changed to VOID; the timecard's "next approver" is set to null; and notifications
	 * are sent to everyone on the approval chain for the timecard, and the employee.
	 *
	 * @param wtc The WeeklyTimecard to be voided.
	 * @param comment The reason for the void. May be null. If null or blank, no change is made to
	 *            the timecard's Comment field.
	 * @return A refreshed reference for the WeeklyTimecard, or null if an Exception is thrown
	 *         during processing.
	 */
	@Transactional
	public WeeklyTimecard voidStatus(WeeklyTimecard wtc, String comment) {
		try {
			wtc = refresh(wtc);
			// Add the comment text to the timecard's Comment field.
			TimecardUtils.addComment(wtc, comment);
			// Create event
			TimecardEventDAO.getInstance().createEvent(wtc, TimedEventType.VOID, (short) 0, null);
			// and update timecard...
			wtc.setStatus(ApprovalStatus.VOID);
			wtc.setUpdated(new Date());
			wtc.setMarkedForApproval(false);

			wtc.setApproverId(null);
			wtc.setApproverContactId(null);

			// Send notification to approvers & employee.
			// The 'sendTo' (2nd param) and 'oldApproverId' (3rd param) are null
			// so list includes all approvers.
			Collection<Contact> approvers = TimecardUtils.createRejectNotify(wtc, null, null, null);
			Production prod = TimecardUtils.findProduction(wtc);
			// Get contact for employee
			Contact sendTo = ContactDAO.getInstance().findByAccountNumAndProduction(wtc.getUserAccount(), prod);
			approvers.add(sendTo); // add employee to notification list
			Contact doer = SessionUtils.getCurrentContact(); // Get contact for
																// person doing
																// the Void
			DoNotification no = DoNotification.getInstance();
			no.timecardVoided(wtc.getFirstName(), wtc.getLastName(), wtc.getEndDate(), doer, approvers, comment);

			updateBatchStatus(wtc);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			wtc = null;
		}
		return wtc;
	}

	/**
	 * Change the current approver for the given timecard to reference the Contact identified by
	 * 'sendToId', a Contact.id value.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param sendToId The Contact.id value of the person to be made the approver for the timecard.
	 *            If this is null, the timecard is being returned to the employee.
	 * @param approverId The Approver.id value of the Approver object that references the Contact
	 *            matching sendToId. If null, use the first (lowest) approver in the hierarchy that
	 *            matches the given sendToId. This parameter is used by the Reject and Pull
	 *            functions so that if the same Contact appears multiple times in the approval
	 *            hierarchy (e.g., as both a departmental and production approver), the rejector can
	 *            select the appropriate one in the list and we will return the timecard to the
	 *            appropriate point in the approval chain, not just to the first occurrence of the
	 *            given Contact.
	 * @param eventType The type of TimecardEvent to create for this action.
	 * @return The Contact object corresponding to the sendToId parameter.
	 */
	private Contact changeApprover(WeeklyTimecard wtc, Integer sendToId, Integer approverId,
			TimedEventType eventType) {
		Contact sendTo = null;
		byte outOfLine = 0;
		Integer priorApproverId = wtc.getApproverId(); // save existing
														// Approver.id
		if (sendToId == null) { // return to employee
			wtc.setApproverId(null);
		}
		else {
			Production prod = TimecardUtils.findProduction(wtc);
			Project project = TimecardUtils.findProject(prod, wtc);
			Approver firstApp = ApproverUtils.findFirstApprover(prod, project, wtc.getDepartment(), null);
			Approver app = firstApp;
			if (approverId != null) {
				// first search the approval chain using the approverId value
				app = searchChain(app, approverId);
				if (app == null && project != null) {
					app = searchChain(project.getApprover(), approverId);
				}
				if (app == null) {
					app = searchChain(prod.getApprover(), approverId);
				}
			}
			if (app == null || approverId == null) {
				// No approverId given, or not found by approverId; look by
				// contact id.
				// ApproverId will be null if the target is an out-of-line
				// approver; if not null,
				// the above search should only fail if someone modified the
				// approval hierarchy while the Reject
				// was in progress (after user got Reject pop-up, but before
				// they clicked Ok).
				app = searchChainContact(firstApp, sendToId);
				if (app == null && project != null) {
					app = searchChainContact(project.getApprover(), sendToId);
				}
				if (app == null) {
					app = searchChainContact(prod.getApprover(), sendToId);
				}
			}
			if (app == null) { // prior out-of-line approver; create a new
								// Approver object
				sendTo = ContactDAO.getInstance().findById(sendToId);
				app = new Approver(sendTo, firstApp, false);
				save(app);
				outOfLine = TimecardEvent.DAY_OUT_OF_LINE; // set flag in event
			}
			else {
				sendTo = app.getContact();
			}
			wtc.setApproverId(app.getId());
		}

		// Create event
		Integer priorUserId = ApproverDAO.getInstance().findUserId(priorApproverId);
		Integer toUserId = null;
		if (sendTo == null) { // returning TC to employee
			toUserId = UserDAO.getInstance().findOneByProperty(UserDAO.ACCOUNT_NUMBER, wtc.getUserAccount()).getId();
		}
		else {
			toUserId = sendTo.getUserId();
		}
		TimecardEventDAO.getInstance().createEvent(wtc, eventType,
				outOfLine, TimecardEvent.FIELD_USER, priorUserId, toUserId);

		wtc.setMarkedForApproval(false);
		attachDirty(wtc);
		// If the prior Approver was "out of line" (un-shared), then delete it.
		if (priorApproverId != null) {
			Approver app = ApproverDAO.getInstance().findById(priorApproverId);
			if (app != null && !app.getShared()) {
				log.debug("deleting non-shared Approver, id=" + app.getId());
				delete(app);
			}
		}
		return sendTo;
	}

	/**
	 * Issue a notification (if appropriate) to the next approver of the timecard.
	 *
	 * @param wtc The timecard whose approver should be notified.
	 * @param employee The employee whose timecard we are processing.
	 */
	private void notifyReady(WeeklyTimecard wtc, Contact employee, Project project) {
		Integer appId = wtc.getApproverId();
		if (appId != null) {
			Approver app = ApproverDAO.getInstance().findById(appId);
			if (app != null && app.getContact() != null) {
				Contact c = app.getContact();
				if (!c.equals(SessionUtils.getCurrentContact())) {
					DoNotification.getInstance().timecardReady(employee, wtc.getEndDate(), c, project);
				}
			}
		}

	}

	// /**
	// * Add a new PayBreakdown detail line to the given WeeklyTimecard.
	// * @param wtc The WeeklyTimecard to be updated.
	// */
	// @Transactional
	// public void addPayBreakdown(WeeklyTimecard wtc) {
	// byte lineNum = 0;
	// for (PayBreakdown pb : wtc.getPayLines()) {
	// if (pb.getLineNumber() > lineNum) lineNum = pb.getLineNumber();
	// }
	// lineNum++;
	// PayBreakdown pb = new PayBreakdown(wtc, lineNum);
	// pb.setAccountMajor(wtc.getAccountMajor());
	// pb.setAccountDtl(wtc.getAccountDtl());
	// pb.setRate(BigDecimal.ONE);
	//
	// wtc.getPayLines().add(pb);
	// wtc.setUpdated(new Date());
	// attachDirty(wtc);
	// }

	/**
	 * Add a new PayJob entry to the WeeklyTimecard. New ones are always added to the end of the
	 * list, and assigned the next higher "job number". The updated timecard is saved, along with
	 * the new PayJob.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 */
	@Transactional
	public void addAndSavePayJob(WeeklyTimecard wtc) {
		addPayJob(wtc);
		// the following attach will also save the new PayJob and related
		// objects
		wtc.setUpdated(new Date());
		attachDirty(wtc);
	}

	/**
	 * Add a new PayJob entry to the WeeklyTimecard without saving it. New ones are always added to
	 * the end of the list, and assigned the next higher "job number". The PayJob's account codes
	 * have been set to match the timecard.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 */
	public static void addPayJob(WeeklyTimecard wtc) {
		byte jobNum = (byte) wtc.getPayJobs().size();
		jobNum++;
		PayJob pj = TimecardUtils.createPayJob(wtc, jobNum);
		wtc.getPayJobs().add(pj);
		log.debug("added payjob #" + jobNum);
	}

	/**
	 * Clear all the data out of a PayJob entry in the given WeeklyTimecard.
	 *
	 * @param wtc The WeeklyTimecard whose PayJob is to be cleared.
	 * @param jobIndex The zero-origin index of the PayJob to be cleared. This will be one less than
	 *            the displayed "job number".
	 */
	@Transactional
	public void clearPayJob(WeeklyTimecard wtc, int jobIndex) {
		byte jobNum = (byte) (jobIndex + 1);
		// delete the old entry
		deletePayJob(wtc, jobIndex);
		// create a new blank PayJob
		PayJob pj = TimecardUtils.createPayJob(wtc, jobNum);
		// replace the deleted entry with the new one
		wtc.getPayJobs().add(jobIndex, pj);
		wtc.setUpdated(new Date());
		// the following attach will also save the new PayJob and related
		// objects
		attachDirty(wtc);
	}

	/**
	 * Delete a specific PayJob from the list of PayJob`s in a WeeklyTimecard.
	 *
	 * @param wtc The WeeklyTimecard of interest.
	 * @param jobIndex The index (origin 0) of the PayJob to be deleted within the timecard's payJob
	 *            list.
	 */
	@Transactional
	public void deletePayJob(WeeklyTimecard wtc, int jobIndex) {
		PayJob pj = wtc.getPayJobs().remove(jobIndex);
		if (pj != null) {
			delete(pj);
			wtc.setUpdated(new Date());
			attachDirty(wtc);
		}
	}

	/**
	 * Update a timecard after HTG completion, which means we have to delete any old pay-breakdown
	 * items also.
	 *
	 * @param wtc The timecard to be updated in the database.
	 */
	@Transactional
	public void updateHtg(WeeklyTimecard wtc) {
		removeDeletedPaylines(wtc);
		attachDirty(wtc);
	}

	/**
	 * Delete from the database those PayBreakdown and PayBreakdownDaily
	 * instances that have been marked for deletion.
	 *
	 * @param wtc The timecard whose "deleted" pay lines should actually be
	 *            deleted from the database.
	 */
	public void removeDeletedPaylines(WeeklyTimecard wtc) {
		for (PayBreakdownMapped pb : wtc.getDeletedPayLines()) {
			delete(pb);
		}
		wtc.getDeletedPayLines().clear();
	}

	/**
	 * Delete the given timecard from the database along with all of its related information, such
	 * as Mileage, BoxRental, DailyTime, PayJob, PayExpense, PayBreakdown, etc.
	 *
	 * @param wtc The timecard to be deleted.
	 */
	@Transactional
	public void remove(WeeklyTimecard wtc) {

		// delete items that don't delete via cascade...
		// AuditEvents
		AuditEventDAO.getInstance().deleteTrail(ObjectType.WT, wtc.getId());

		// Delete timecard from database; Hibernate cascade will delete related
		// Mileage, BoxRental, DailyTime, Image, PayJob, PayExpense,
		// PayBreakdown and TimecardEvent entities, and their sub-entities
		// (like MileageLine).
		delete(wtc);

	}

	/**
	 * Unlock all WeeklyTimecards that are currently locked. Called during application startup, to
	 * clear any locks leftover from a prior execution, in case of a "hard" crash in which our
	 * normal methods did not clear the locks during shutdown (via TimecardBean.dispose()).
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	public void unlockAllLocked() {
		try {
			String query = "from WeeklyTimecard where " + LOCKED_BY + " is not null";
			List<WeeklyTimecard> locked = find(query);
			for (WeeklyTimecard wtc : locked) {
				unlock(wtc, null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Import a collection of WeeklyTimecard`s from a file.
	 *
	 * @param filename The fully-qualified file name to be imported.
	 * @return A non-null (but possibly empty) Collection of the WeeklyTimecard's imported from the
	 *         file.
	 */
	@Transactional
	public Collection<WeeklyTimecard> imports(String filename) {
		Collection<WeeklyTimecard> cards = new ArrayList<>();
		try {
			Production prod = SessionUtils.getNonSystemProduction();
			Importer imp = new Importer(filename);
			Integer id;
			boolean ok = true;
			try {
				while (ok) {
					id = imp.getInt();
					if (id != null) {
						WeeklyTimecard wtc = findById(id);
						if (wtc == null) {
							wtc = new WeeklyTimecard();
							wtc.setProdId(prod.getProdId());
							wtc.setImportStatus(ImportStatus.ADDED);
						}
						else {
							wtc.setImportStatus(ImportStatus.UPDATED);
						}
						try {
							wtc.imports(imp);
						}
						catch (Exception e) {
							ok = false;
							EventUtils.logError(e);
						}
						if (wtc.getImportStatus() == ImportStatus.ADDED) {
							save(wtc);
						}
						else if (wtc.getImportStatus() == ImportStatus.UPDATED) {
							attachDirty(wtc);
						}
						cards.add(wtc);
						ok = imp.next(); // discard any unused data in the
											// record.
					}
				}
			}
			catch (EOFException eof) {
				log.debug("EOF on import file");
			}
			catch (NumberFormatException nbr) {
				EventUtils.logError(nbr);
			}
			catch (IOException io) {
				EventUtils.logError(io);
			}
			imp.close();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return cards;
	}

	// /**
	// * Convert encrypted fields from v2.1 to v2.2. (rev 4439)
	// */
	// @Transactional
	// public void convertTimecards() {
	// int converted = 0;
	// int already = 0;
	//
	// List<WeeklyTimecard> wtcs = findAll();
	// for (WeeklyTimecard wtc : wtcs) {
	// //log.debug(wtc.getId());
	// if (wtc.getSocialSecurity() == null) {
	// if (wtc.getOldSocialSecurity() != null) {
	// wtc.setSocialSecurity(wtc.getOldSocialSecurity());
	// //attachDirty(wtc);
	// converted++;
	// }
	// }
	// else {
	// log.debug("#" + wtc.getId() + " SSN already converted");
	// already++;
	// }
	// }
	//
	// log.info("" + wtcs.size() + " timecards input for conversion from 2.1 to
	// 2.2.");
	// log.info("" + already + " already had new SSN" );
	// log.info("" + converted + " WeeklyTimecards converted");
	// }

	// public static WeeklyTimecardDAO
	// getFromApplicationContext(ApplicationContext ctx) {
	// return (WeeklyTimecardDAO)ctx.getBean("WeeklyTimecardDAO");
	// }

}