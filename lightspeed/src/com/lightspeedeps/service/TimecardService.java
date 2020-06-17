/**
 * File: TimecardService.java
 */
package com.lightspeedeps.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.dao.WeeklyTimecardDAO.TimecardRange;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.TimecardMessage;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.payroll.*;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.login.AuthorizationBean;


/**
 * Contains methods with business logic related to timecard and payroll processing,
 * such as filling the Job Tables and generating Pay Breakdown information.
 */
public class TimecardService extends BaseService {
	private static final Log log = LogFactory.getLog(TimecardService.class);

	/** Minimum rate that is considered "golden" time payment; typically 2.0 */
	protected static final BigDecimal MINIMUM_GOLD_RATE = Constants.DECIMAL_TWO;

	/** The current AuditEvent, used for collecting HTG 'trace' information. */
	private AuditEvent event;

	/** The list of AuditEvent`s to be persisted when processing is complete. */
	private Collection<AuditEvent> eventList = new ArrayList<>();

	/** This object holds all the shared HTG data used during calculations. */
	private final HtgData htg = new HtgData();


	/**
	 * The usual constructor for HTG processing. A timecard must be provided;
	 * the Production, StartForm, and other necessary entities will be
	 * determined from the timecard or the session. Note that a new HtgData instance is
	 * created.
	 */
	public TimecardService(WeeklyTimecard wtc) {
		this(wtc, SessionUtils.getNonSystemProduction());
	}

	/**
	 * A constructor used when the Production being processed may not be
	 * the current (logged in) Production.
	 */
	public TimecardService(WeeklyTimecard wtc, Production prod) {
		htg.service = this;
		htg.weeklyTimecard = wtc;
		htg.production = prod;
		if (htg.production == null ||
				(! htg.weeklyTimecard.getProdId().equals(htg.production.getProdId()))) {
			htg.production = ProductionDAO.getInstance().findByProdId(htg.weeklyTimecard.getProdId());
		}

		if (htg.production.getType().hasPayrollByProject()) {
			// use project associated with timecard. LS-3806
			htg.project = htg.weeklyTimecard.getStartForm().getProject();
			htg.preference = htg.project.getPayrollPref();
			htg.isCommercial = true;
			if (htg.weeklyTimecard.getStartForm().getModelRelease() != null) {
				Integer mrId = htg.weeklyTimecard.getStartForm().getModelRelease().getId();
				htg.isModelRelease = true; // to control later processing. LS-4664
				htg.modelRelease = (FormModelRelease)FormService.getInstance().findById(mrId,
						PayrollFormType.MODEL_RELEASE.getApiFindUrl(), FormModelRelease.class);
			}
		}
		else {
			htg.preference = htg.production.getPayrollPref();
			htg.isCommercial = false;
		}

		PayrollService payrollSvc = htg.production.getPayrollPref().getPayrollService();
		if (payrollSvc != null) {
			htg.isTeam = payrollSvc.getTeamPayroll();
		}

		htg.ruleService = new RuleService(htg);

		htg.priorTimecard = findPriorTimecard();
		htg.priorDailyTime = null;
		if (htg.priorTimecard != null) {
			htg.priorDailyTime = htg.priorTimecard.getDailyTimes().get(6); // Saturday
		}

		if (htg.weeklyTimecard.isNonUnion()) {
			htg.nonUnion = true;
			htg.flexible6thDay = true;
			if (htg.isTeam && htg.weeklyTimecard.getAllowWorked()) { // LS-2189
				if (htg.weeklyTimecard.getStartForm().getRateType() == EmployeeRateType.WEEKLY) {
					htg.isSimpleWeekly = true; // remember Team/weekly-exempt/non-union
					htg.priorTimecard = null;  // prior week should be ignored for calculations
				}
			}
		}
		else if (! htg.production.getPayrollPref().getUse30Htg()) {
			htg.flexible6thDay = true;
		}
	}

	/**
	 * Calculate and fill in the number of MPVs on the given timecard.
	 *
	 * @param wtc The timecard whose MPVs are to be determined.
	 */
	public static boolean calculateMpvs(WeeklyTimecard wtc) {
		TimecardService timecardService = new TimecardService(wtc);
		return timecardService.calculateMpvsSub(null);
	}

	/**
	 * Calculate and fill in the number of MPVs on the given timecard. This
	 * method will validate the timecard before proceeding.
	 */
	private boolean calculateMpvsSub(AuditEvent parent) {
		boolean ret = true;
		if (parent == null) { // no parent, we need to validate the timecard
			event = startEvent(AuditEventType.CALC_MPV, null, htg.weeklyTimecard);
			htg.auditEvent = event;
			ret = validateDayTypes(event);
			if (! ret) {
				errorTrace(htg, "Timecard.HTG.FillPayJobs.InvalidTimecard");
			}
			parent = event;
		}
		if (ret) {
			MpvService timecardMpvService = new MpvService(htg);
			timecardMpvService.calculateMpvs(parent);
		}
		AuditUtils.updateEvent(parent);
		htg.auditEvent = parent;
		return ret;
	}

	/**
	 * Use the raw hours and the daily split information to (a) calculate MPVs
	 * and (b) fill in the PayJob hourly information. This method will validate
	 * the timecard before proceeding. It also sets
	 * WeeklyTimecard.jobHoursDiffer.
	 *
	 * @param wtc The timecard whose hours are to be broken down.
	 */
	public static void fillPayJobs(WeeklyTimecard wtc) {
		wtc.setStartForm(StartFormDAO.getInstance().refresh(wtc.getStartForm()));
		TimecardService timecardService = new TimecardService(wtc);
		AuditEvent parent = timecardService.startEvent(AuditEventType.FILL_JOBS, null, wtc);
		if (timecardService.calculateMpvsSub(parent)) {
			timecardService.fillPayJobsSub(parent);
		}
		timecardService.flushEvents();
	}

	/**
	 * Use the raw hours and the daily split information to fill in the PayJob
	 * hourly information. This method will validate the timecard if no parent
	 * audit trail is provided. It also sets WeeklyTimecard.jobHoursDiffer.
	 *
	 * @param parent The parent AuditEvent; if this is not null, we assume that
	 *            the calling method (our 'parent') already validated the
	 *            timecard.
	 * @return True iff the "Fill jobs" operation was successful.
	 */
	/*package*/ boolean fillPayJobsSub(AuditEvent parent) {
		boolean ret = true;
		event = startEvent(AuditEventType.FILL_JOBS, parent, htg.weeklyTimecard);
		htg.auditEvent = event;

		// make sure Box Rental expense matches timecard & StartForm data
		TimecardUtils.updateExpenseItems(htg.weeklyTimecard, false, true);

		if (parent == null) { // no parent, we need to validate the timecard
			ret = TimecardCheck.validateUserData(htg.weeklyTimecard);
			if (! ret) {
				TimecardService.errorTrace(htg, "Timecard.HTG.FillPayJobs.InvalidTimecard");
			}
		}
		if (ret) {
			PayJobService pjs = new PayJobService(htg, event);
			ret = pjs.fillPayJobsValid();
		}
		AuditUtils.updateEvent(event);
		htg.auditEvent = parent;
		return ret;
	}

	/**
	 * Generate Pay Breakdown line items for all the Expense/Reimbursement
	 * fields and all the Pay Job entries. This method will validate the
	 * timecard before proceeding.
	 *
	 * @param wtc The timecard to be calculated.
	 *
	 * @return True iff at least one PayBreakdown item is created for the
	 *         timecard.
	 */
	public static boolean autoPay(WeeklyTimecard wtc) {
		log.debug("");
		TimecardService timecardService = new TimecardService(wtc);
		wtc.setStartForm(StartFormDAO.getInstance().refresh(wtc.getStartForm()));
		boolean ret = timecardService.autoPaySub(null);
		timecardService.flushEvents();
		return ret;
	}

	/**
	 * Generate Pay Breakdown line items for all the Expense/Reimbursement
	 * fields and all the Pay Job entries. This method will validate the
	 * timecard if no parent audit event is provided.
	 *
	 * @param parent The parent AuditEvent; if this is not null, we assume that
	 *            the calling method (our 'parent') already validated the
	 *            timecard.
	 * @return True iff at least one PayBreakdown item is created for the
	 *         timecard.
	 */
	private boolean autoPaySub(AuditEvent parent) {
		boolean ret = true;
		if (parent == null) { // no parent, we need to validate the timecard
			event = startEvent(AuditEventType.FILL_PAY_BREAKDOWN, null, htg.weeklyTimecard);
			htg.auditEvent = event;
			parent = event;

			ret = validateDayTypes(event);
			if (! ret) {
				errorTrace(htg, "Timecard.HTG.AutoPay.InvalidTimecard");
			}
			if (htg.production.getType().isTours() && htg.weeklyTimecard.getPayJobs().size() == 0) {
				WeeklyTimecardDAO.addPayJob(htg.weeklyTimecard);
			}
			ret = validateJobSplits(event);
			if (! ret) {
				errorTrace(htg, "Timecard.HTG.AutoPay.InvalidJobSplits");
			}
			AuditUtils.updateEvent(event);
		}
		if (ret) {
			PayBreakdownService payBreakdownService = new PayBreakdownService(htg);
			ret = payBreakdownService.generatePayBreakdown(parent);
			if (! ret) {
				parent.appendMsg("Timecard.HTG.AutoPay.NoData.Trace");
			}
		}

		htg.auditEvent = parent;
		return ret;
	}

	/**
	 * Perform the full "suite" of HTG actions on a single timecard -- all the
	 * steps required to evaluate a timecard's gross pay -- WITHOUT committing
	 * the results to the database. This method is called from the Full Timecard
	 * page. The steps performed include:
	 * <ul>
	 * <li>Calculate the number of MPVs
	 * <li>Fill the job tables with the recorded hours, respecting splits, and
	 * calculating OT, gold, invaded hours, etc.
	 * <li>Generate Pay Breakdown entries to match the job tables.
	 * </ul>
	 * It also sets WeeklyTimecard.jobHoursDiffer.
	 *
	 * @param wtc The timecard to be analyzed.
	 */
	public static void calculateAllHtg(WeeklyTimecard wtc, boolean processHotCosts, boolean hotCostsReRate) {
		TimecardService timecardService = new TimecardService(wtc);
		timecardService.getHtg().processHotCosts = processHotCosts;
		timecardService.getHtg().hotCostsReRate = hotCostsReRate;
		timecardService.calculateAllHtg((List<TimecardMessage>)null);
	}

	/**
	 * Perform the full "suite" of HTG actions on a single timecard -- all the
	 * steps required to evaluate a timecard's gross pay -- <b>and update</b>
	 * the database with the results. This method is called from the "Calc HTG"
	 * button on the Approver Dashboard's "Gross Payroll" minitab.
	 * <p>
	 * The steps performed include:
	 * <ul>
	 * <li>Calculate the number of MPVs
	 * <li>Fill the job tables with the recorded hours, respecting splits, and
	 * calculating OT, gold, invaded hours, etc.
	 * <li>Generate Pay Breakdown entries to match the job tables (Auto-pay).
	 * </ul>
	 * It also sets WeeklyTimecard.jobHoursDiffer.
	 *
	 * @param wtc The timecard to be analyzed.
	 * @param msgs The List of messages to which error messages will be added
	 *            during the process.
	 */
	public static void calculateAllHtgAndUpdate(WeeklyTimecard wtc, List<TimecardMessage> msgs) {
		TimecardService timecardService = new TimecardService(wtc);
		timecardService.calculateAllHtg(msgs);
		WeeklyTimecardDAO.getInstance().updateHtg(wtc);
	}

	/**
	 * Perform the full "suite" of HTG actions -- all the steps required to
	 * evaluate a timecard's gross pay -- on multiple timecards. The updated
	 * information is committed to the database. The set of timecards processed
	 * will depend on the 'select', 'allDates' and 'stat' parameters passed. The
	 * actions performed on each timecard includes:
	 * <ul>
	 * <li>Calculate the number of MPVs
	 * <li>Fill the job tables with the recorded hours, respecting splits, and
	 * calculating OT, gold, invaded hours, etc.
	 * <li>Generate Pay Breakdown entries to match the job tables.
	 * </ul>
	 * It also sets WeeklyTimecard.jobHoursDiffer.
	 *
	 * @param wtc The timecard which may supply necessary values in determining
	 *            the set of timecards processed, such as the week-ending date
	 *            or the weekly Batch.
	 * @param contact The Contact running this process, who must have edit
	 *            access to a timecard before it will be selected for HTG
	 *            processing.
	 * @param prod The Production to which all the processed timecards belong.
	 * @param project The Project to which all the processed timecards must
	 *            belong. This should only be non-null for a Commercial
	 *            production.
	 * @param select The main selection parameter; for a user-initiated process,
	 *            this selection closely mirrors the radio-button selections
	 *            available in the "Run HTG" pop-up.
	 * @param allDates True iff the W/E date of the timecards will be ignored,
	 *            i.e., all dates will be processed.
	 * @param wtcDate The date of the timecards to be processed if 'allDates' is
	 *            false.
	 * @param stat The status (e.g., OPEN or SUBMITTED) of timecards to be
	 *            processed; if null, timecards will be processed regardless of
	 *            their status.
	 * @param msgs The List of messages to which error messages will be added
	 *            during the process.
	 * @return A List of two Integers: the number of timecards included by the
	 *         selection criteria, and the number of successfully completed
	 *         timecards.
	 */
	public static List<Integer> calculateMultipleHtg(WeeklyTimecard wtc, Contact contact,
			Production prod, Project project, TimecardSelectionType select, boolean allDates,
			Date wtcDate, ApprovalStatus stat, List<TimecardMessage> msgs, String statusFilter) {

		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		List<WeeklyTimecard> list = new ArrayList<>();
		List<Integer> results = new ArrayList<>();

		switch(select) {
			case CURRENT:
				if (allDates) {
					list = weeklyTimecardDAO.findByUserAccountOccupation(prod, project,
							wtc.getUserAccount(), wtc.getOccupation());
				}
				else {
					if (wtc.getEndDate().equals(wtcDate)) {
						list.add(wtc);
					}
					else { // one person, but different date than selected TC
						list = weeklyTimecardDAO.findByWeekEndDateAccountOccupation(prod, project, wtcDate,
								wtc.getUserAccount(), wtc.getOccupation());
					}
				}
				break;
			case PERSON:
				if (allDates) {
					list = weeklyTimecardDAO.findByUserAccount(prod, project, wtc.getUserAccount(), statusFilter);
				}
				else {
					list = weeklyTimecardDAO.findByWeekEndDateAccount(prod, project, wtcDate, TimecardRange.EXACT,
							wtc.getUserAccount(), statusFilter);
				}
				break;
			case DEPT:
				if (allDates) {
					list = weeklyTimecardDAO.findByUserAccountDepartment(prod, project, null, wtc.getDeptName(), statusFilter);
				}
				else {
					list = weeklyTimecardDAO.findByWeekEndDateAccountDept(prod, project, wtcDate, TimecardRange.EXACT,
							null, wtc.getDeptName(), statusFilter, null);
				}
				break;
			case BATCH:
				// "allDates" is irrelevant -- doing contents of a specific WEEKLY batch
				list = weeklyTimecardDAO.findByUserAccountBatch(prod, project, null, wtc.getWeeklyBatch(), statusFilter);
				break;
			case UNBATCHED:
				// "allDates" is not supported for "unbatched" range
				list = weeklyTimecardDAO.findByWeekEndDateAccountBatchDept(prod, project, wtcDate, TimecardRange.EXACT,
						null, null, null, statusFilter, null);
				break;
			case ALL:
				if (allDates) { // Only LS Admin should be allowed this - all TCs for an entire project/production
					list = weeklyTimecardDAO.findByWeekEndDate(prod, project, null, statusFilter);
				}
				else {
					list = weeklyTimecardDAO.findByWeekEndDate(prod, project, wtcDate, statusFilter);
				}
				break;
			case SELECT:
				// Not yet available in Run HTG UI - shouldn't get here.
				EventUtils.logError("Unsupported Run HTG selection option.");
				break;
		}

		int count = 0;
		boolean superApprover = AuthorizationBean.getInstance().getPseudoApprover();
		for (WeeklyTimecard w : list) {
			ApprovalStatus tcStat = w.getStatus();
			if (tcStat == ApprovalStatus.VOID) {
				continue;
			}
			if (stat == null || tcStat == stat ||
					(stat == ApprovalStatus.SUBMITTED && ! w.getSubmitable())) {
				if ((! w.getSubmitable()) && contact != null && ! superApprover) {
					// TC is submitted, contact must be next approver to have edit access (2.9.5504)
					boolean isApprover = ApproverUtils.isNextApprover(w, contact.getId());
					if (! isApprover) {
						continue;	// skip this timecard
					}
				}

				TimecardService timecardService = new TimecardService(w);
				log.debug("W/E " + w.getEndDate() + " for: " + w.getFirstName() + " " + w.getLastName());
				try {
					boolean success = timecardService.calculateAllHtg(msgs);
					weeklyTimecardDAO.updateHtg(w);

					if(success) {
						count++;
					}
				}
				catch (Exception e) {
					EventUtils.logError(e);
					warn(timecardService.htg, "Timecard.HTG.Error");
				}
			}
		}

		results.add(list.size());
		results.add(count);

		log.debug(results);

		return results;
	}

	/**
	 * Perform the full "suite" of HTG actions -- all the steps required to
	 * evaluate a timecard's gross pay -- on a given list of timecards. The
	 * updated information is committed to the database. The actions performed
	 * on each timecard may include:
	 * <ul>
	 * <li>Calculate the number of MPVs
	 * <li>Fill the job tables with the recorded hours, respecting splits, and
	 * calculating OT, gold, invaded hours, etc.
	 * <li>Generate Pay Breakdown entries to match the job tables.
	 * </ul>
	 * It also sets WeeklyTimecard.jobHoursDiffer.
	 * <p>
	 * Note that if this is a tours production, only a small part of the normal
	 * HTG processing is performed.
	 *
	 * @param msgs The List of messages to which error messages will be added
	 *            during the process.
	 * @return A List of two Integers: the number of timecards in the provided
	 *         list, and the number of successfully completed timecards.
	 */
	public static List<Integer> calculateMultipleHtg(List<WeeklyTimecard> list, List<TimecardMessage> msgs) {

		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		List<Integer> results = new ArrayList<>();

		int count = 0;
		for (WeeklyTimecard w : list) {
			if (w.getStatus() == ApprovalStatus.VOID) {
				continue;
			}
			TimecardService timecardService = new TimecardService(w);
			log.debug("W/E " + w.getEndDate() + " for: " + w.getFirstName() + " " + w.getLastName());

			try {
				boolean success = timecardService.calculateAllHtg(msgs);
				weeklyTimecardDAO.updateHtg(w);
				if(success) {
					count++;
				}
			}
			catch (Exception e) {
				EventUtils.logError(e);
				warn(timecardService.htg, "Timecard.HTG.Error");
			}

		}

		results.add(list.size());
		results.add(count);

		log.debug(results);

		return results;
	}

	/**
	 * Perform the full "suite" of HTG actions for a single timecard,
	 * <b>WITHOUT</b> updating the database, including:
	 * <ul>
	 * <li>Calculate the number of MPVs
	 * <li>Fill the job tables with the recorded hours, respecting splits, and
	 * calculating OT, gold, invaded hours, etc.
	 * <li>Generate Pay Breakdown entries to match the job tables (Auto-pay).
	 * </ul>
	 * It also sets WeeklyTimecard.jobHoursDiffer.
	 *
	 * @param msgs The List of messages to which error messages will be added
	 *            during the process.
	 * @return - This determines whether we add to the successful calculated count or not.
	 */
	private boolean calculateAllHtg(List<TimecardMessage> msgs) {
		boolean ret = true;
		// Create a "parent" event as an umbrella for the subordinate actions...
		AuditEvent parent = startEvent(AuditEventType.CALC_HTG, null, htg.weeklyTimecard);
		htg.auditEvent = parent;

		htg.messages = msgs;

		ret = TimecardCheck.validateUserData(htg.weeklyTimecard);

		if (ret) { // validated the timecard

			if (! htg.production.getType().isTours()) {
				calculateMpvsSub(parent); // step 1: MPVs
			}

			// Step 2: Fill the PayJob objects (Job tables) from the daily info
			ret = fillPayJobsSub(parent);

			if (ret) {	// Skip Auto-pay if Fill Job step failed.
				if (! autoPaySub(parent)) { // Step 3: Auto Pay fills the Pay Breakdown table
					error(htg, "Timecard.HTG.AutoPay.NoData");
				}
			}
			else {
				// Fill job failed - delete old pay breakdown line items
				PayBreakdownService.removePayBreakdown(htg);
				PayBreakdownService.renumberPayLines(htg.weeklyTimecard);  // just to be safe!
				TimecardCalc.calculatePayBreakdown(htg.weeklyTimecard); // generate totals
			}
			// Check to see if the rate is null or 0 and grand total on the timecard is 0.
			// If so, put up error message. LS-1000.
			BigDecimal rate = htg.weeklyTimecard.getRate();
			BigDecimal grandTotal = htg.weeklyTimecard.getGrandTotal();
			if((rate == null || rate.signum() == 0)
					 && (grandTotal == null || grandTotal.signum() == 0)) {
				error(htg, "Timecard.Calculate.MissingRate");
				ret = false;
			}
		}
		else {
			errorTrace(htg, "Timecard.HTG.AllPay.InvalidTimecard");
		}
		AuditUtils.updateEvent(parent);
		log.info("All HTG done for: " + htg.weeklyTimecard);
		htg.auditEvent = null;
		flushEvents();

		return ret;
	}

	/**
	 * Make sure the given timecard has enough PayJob entries to allow the
	 * specified job number to be used. If there are not enough entries, enough
	 * entries will be added to handle the given job number.
	 *
	 * @param wtc The timecard in question.
	 * @param job The job number (index, origin 0 !) to be used. This is one
	 *            less than the value the displayed and entered on the timecard
	 *            page.
	 */
	public static void checkPayJobsSize(WeeklyTimecard wtc, int job) {
		while (job >= wtc.getPayJobs().size()) {
			WeeklyTimecardDAO.addPayJob(wtc);
		}
	}

	/**
	 * This will update any of the 3 required fields (SSN, Work city, Work
	 * State) on the timecard that are blank with their corresponding values
	 * from the Start Form. (This is typically done when someone tries to Submit
	 * the timecard.)
	 *
	 * @param wtc The timecard to be updated.
	 * @return The value of WeeklyTimecard.getHasRequiredFields() after any
	 *         updates to the timecard have been done.
	 */
	public static boolean updateTimecardRequiredFields(WeeklyTimecard wtc) {
		boolean updated = false;
		StartForm sf = wtc.getStartForm();
		if (StringUtils.isEmpty(wtc.getSocialSecurity())) {
			if ( ! StringUtils.isEmpty(sf.getSocialSecurity())) {
				wtc.setSocialSecurity(sf.getSocialSecurity());
				updated = true;
			}
		}
		if (StringUtils.isEmpty(wtc.getCityWorked())) {
			if ( ! StringUtils.isEmpty(sf.getWorkCity())) {
				wtc.setCityWorked(sf.getWorkCity());
				updated = true;
			}
		}
		if (StringUtils.isEmpty(wtc.getStateWorked())) {
			if ( ! StringUtils.isEmpty(sf.getWorkState())) {
				wtc.setStateWorked(sf.getWorkState());
				updated = true;
			}
		}
		if (StringUtils.isEmpty(wtc.getWorkCountry())) { // LS-2156
			if ( ! StringUtils.isEmpty(sf.getWorkCountry())) {
				wtc.setWorkCountry(sf.getWorkCountry());
				updated = true;
			}
		}
		if (updated) {
			WeeklyTimecardDAO.getInstance().attachDirty(wtc);
		}

		boolean checkPaidAs = FF4JUtils.useFeature(FeatureFlagType.TTCO_ENHANCED_LOAN_OUT);
		return wtc.getHasRequiredFields(checkPaidAs);
	}

	/**
	 * Not currently used; see PayBreakdownService for another prevailing-rate determination.
	 * <p>
	 * Determining an employee's prevailing hourly wage for a particular day and
	 * time based on their StartForm data.
	 *
	 * @param weeklyTimecard The timecard of the employee.
	 * @param dt The DailyTime entry for the day under consideration.
	 * @param elapsedHours The number of hours into the day at which point we
	 *            want to determine the hourly wage. This is used to determine,
	 *            for example, if the employee was at an overtime or golden
	 *            rate.
	 * @return The hourly wage for the given day and time. Returns null if the
	 *         value cannot be determined, e.g., due to missing or invalid
	 *         rules.
	 */
	/*@Deprecated
	public static BigDecimal findPrevailingRate(WeeklyTimecard weeklyTimecard, DailyTime dt,
			BigDecimal elapsedHours) {
		Production prod = SessionUtils.getNonSystemProduction();
		BigDecimal rate = null;
		StartForm sf = weeklyTimecard.getStartForm();
		sf = StartFormDAO.getInstance().refresh(sf);
		boolean useStudio;
		if (dt.getWorkZone() == null) {
			useStudio = weeklyTimecard.isStudioRate();
		}
		else {
			useStudio = dt.getWorkZone().isStudio();
		}
		StartRateSet rateSet;
		if (sf.getHasPrepRates() && prod.getPayrollPref().getInPrep(dt.getDate())) {
			rateSet = sf.getPrep();
		}
		else {
			rateSet = sf.getProd();
		}
		RateHoursGroup grp;
		grp = rateSet.getHourly();
		if (dt.getWorkDayNum() == 6 && rateSet.getDay6() != null) {
			grp = rateSet.getDay6();
		}
		else if (dt.getWorkDayNum() == 7 && rateSet.getDay7() != null) {
			grp = rateSet.getDay7();
		}
		rate = grp.getStudio();
		if (rate == null || ((! useStudio) && grp.getLoc() != null)) {
			rate = grp.getLoc();
		}
		// to be done: determine if rate needs OT or golden adjustment
		return rate;
	}*/

	/**
	 * Validate the current timecard.
	 * @param evt The AuditEvent for logging.
	 * @return True iff the current weeklyTimecard being processed passes all
	 *         validations.
	 */
	private boolean validateDayTypes(AuditEvent evt) {
		boolean ret = true;
		ret = TimecardCheck.validateAll(htg.weeklyTimecard);
//		for (DailyTime dt : weeklyTimecard.getDailyTimes()) {
//			if (dt.getDayType() == null) {
//				if (dt.getHours().signum() != 0) {
//					ret = false;
//					break;
//				}
//			}
//		}
		return ret;
	}


	/**
	 * Validate that the job numbers in the daily Job split entries do not
	 * exceed the number of Job tables (PayJob entries). This can fail if the
	 * user deletes a Job table after having done a Fill Jobs operation.
	 *
	 * @param evt The current AuditEvent log
	 * @return True iff all job split values fall within the size of the current
	 *         PayJob List.
	 */
	private boolean validateJobSplits(AuditEvent evt) {
		boolean ret = true;

		byte numJobs = (byte)htg.weeklyTimecard.getPayJobs().size();
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getJobNum1() > numJobs ||
					dt.getJobNum2() > numJobs ||
					dt.getJobNum3() > numJobs) {
				ret = false;
				break;
			}
		}
		return ret;
	}

	/**
	 * Find the timecard for the prior week.
	 *
	 * @return The WeeklyTimecard for the same user and occupation as the
	 *         current weeklyTimecard, but for the previous week.
	 */
	private WeeklyTimecard findPriorTimecard() {
		WeeklyTimecard wtc = null;
		if (htg.production != null ) { // (production may be null for JUnit tests)
			Date priorDate = TimecardUtils.calculatePriorWeekEndDate(htg.weeklyTimecard.getEndDate());
			List<WeeklyTimecard> wtcs = WeeklyTimecardDAO.getInstance().findByWeekEndDateAccountOccupation(
					htg.production, htg.project, priorDate, htg.weeklyTimecard.getUserAccount(), htg.weeklyTimecard.getOccupation());
			if (wtcs.size() == 0) {
				// none found -- try it without occupation, in case employee's job name changed
				wtcs = WeeklyTimecardDAO.getInstance().findByWeekEndDateAccount(
						htg.production, htg.project, priorDate, TimecardRange.EXACT, htg.weeklyTimecard.getUserAccount(), null);
			}
			if (wtcs.size() > 0) {
				wtc = wtcs.get(0);
			}
		}
		return wtc;
	}

	/**
	 * Handle a warning message. This may be displayed to the user immediately,
	 * or added to our 'message queue' when multiple timecards are being
	 * calculated. The current timecard's employee name and week-ending date will be added
	 * to the end of the message.
	 *
	 * @param msgId The resource id (name) of the message to be issued.
	 */
	public static void warn(HtgData htg, String msgId, Object... args) {
		String output = formatMessage(htg, msgId, args );
		if (htg.messages == null) {
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_WARN, output);
		}
	}

	/**
	 * Handle an error message. This may be displayed to the user immediately,
	 * or added to our 'message queue' when multiple timecards are being
	 * calculated. The current timecard's employee name and week-ending date will be added
	 * to the end of the message.
	 *
	 * @param msgId The resource id (name) of the message to be issued.
	 * @param args One or more arguments supplying substitution text for the
	 *            message.
	 */
	public static void error(HtgData htg, String msgId, Object... args) {
		String output = formatMessage(htg, msgId, args );
		if (htg.messages == null) {
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, output);
		}
	}

	/**
	 * Handle a 'traced' error message. This may be displayed to the user
	 * immediately, or added to our 'message queue' when multiple timecards are
	 * being calculated. The current timecard's employee name and week-ending
	 * date will be added to the end of the message.
	 * <p>
	 * A matching "trace" message will also be added to the HTG audit trace, if
	 * available. The msgid used will be different -- the suffix '.Trace' will
	 * be added before formatting. This allows it to be a more compact message
	 * than would be displayed to the user.
	 *
	 * @param msgId The resource id (name) of the message to be issued.
	 * @param args One or more arguments supplying substitution text for the
	 *            message.
	 */
	public static void errorTrace(HtgData htg, String msgId, Object... args) {
		try {
			String output = formatMessage(htg, msgId, args );
			if (htg.messages == null) {
				MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, output);
			}
			if (htg.auditEvent != null) {
				htg.auditEvent.appendMsg(msgId + ".Trace", args);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			// No faces message: avoid compounding error messages
		}
	}

	/**
	 * Generate a warning or error message by formatting the supplied msgId,
	 * then adding the current timecard's employee name and week-ending date.
	 *
	 * @param htg The HtgData instance for the current processing cycle.
	 * @param msgId The message id of the message text within our resource
	 *            bundle.
	 * @param args Optional positional arguments to be substituted into the
	 *            message text.
	 * @return Formatted output message.
	 */
	private static String formatMessage(HtgData htg, String msgId, Object... args) {
		String output = MsgUtils.formatMessage( msgId, args );
		if (htg != null && htg.weeklyTimecard != null) {
			String weDate = new SimpleDateFormat("M/d").format(htg.weeklyTimecard.getEndDate());
			output += " [" + htg.weeklyTimecard.getLastNameFirstName() + "; W/E " + weDate + ']';
		}
		if (htg != null && htg.messages != null) {
			htg.messages.add(new TimecardMessage(output));
		}
		return output;
	}

	/**
	 * Create an AuditEvent, and cache it to be saved later.
	 *
	 * @param type The type of AuditEvent to create.
	 * @param parent The (optional) parent of the event being created.
	 * @param wtc The related WeeklyTimecard.
	 * @return The newly created event.
	 */
	/* package */ AuditEvent startEvent(AuditEventType type, AuditEvent parent, WeeklyTimecard wtc) {
		AuditEvent evt = AuditUtils.startEvent(type, parent, wtc);
		eventList.add(evt);
		return evt;
	}

	/**
	 * Push all pending AuditEvent instances to the database.
	 */
	private void flushEvents() {
		if(!htg.processHotCosts) {
			AuditEventDAO.getInstance().save(eventList);
		}
		eventList.clear();
	}

	/** See {@link #htg}. */
	public HtgData getHtg() {
		return htg;
	}

}
