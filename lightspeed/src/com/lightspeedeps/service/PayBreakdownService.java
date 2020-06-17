/**
 * PayBreakdownService.java
 */
package com.lightspeedeps.service;

import java.math.*;
import java.text.*;
import java.util.*;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.PayJobHours;
import com.lightspeedeps.service.SpecialRuleService.HtgPhase;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.*;
import com.lightspeedeps.util.payroll.*;

/**
 * Contains methods used to create the entries in a WeeklyTimecard's
 * PayBreakdown table based primarily on the information in the Job table(s),
 * Expense table, and DailyTime entries.
 */
public class PayBreakdownService extends BaseService {
	/** Our logger. */
	private static final Log LOG = LogFactory.getLog(PayBreakdownService.class);

	/** A character used when concatenating more information (typically an account number) onto a
	 * "rule key" string; this can be any character that won't be used in a real Rule name */
	private static final char RULE_SPECIAL_CHAR = '!';

	/** fake rule name used as Map key for Holiday-Paid (HP) days */
	private static final String HP_KEY = RULE_SPECIAL_CHAR + "HP";

	/** fake rule name used as Map key for Holiday-Worked (HW) days */
	private static final String HW_KEY = RULE_SPECIAL_CHAR + "HW";

	/** fake rule name used as Map key for Touring-type days, e.g., Show, Post. */
	private static final String TOUR_KEY = RULE_SPECIAL_CHAR + "TR";

	/** Constant 0.2 (when multiplied, equivalent of dividing by 5). */
	private static final BigDecimal FLAT_POINT2 = new BigDecimal(2).divide(Constants.DECIMAL_10).setScale(2);

	/** Number of hours assumed by FLSA rules to be straight time. */
	private static final BigDecimal FLSA_STRAIGHT_HOURS = new BigDecimal(40);

	/** Hours at this rate or higher are ignored for FLSA calculations. */
	private static final BigDecimal FLSA_IGNORE_RATE = Constants.DECIMAL_TWO;

	/** The index of the next PayBreakdown entry to be added. */
	private byte lineNum;

	/** If true, pay breakdown line items will be split by day, and entered into
	 * the WeeklyTimecard's payDailyLines list. That is, the labor and MPV items
	 * will be generated on a daily basis. If false, the line items are grouped
	 * (e.g., by category and rate) for the entire week, and put into the
	 * payLines list. */
	private boolean split;

	/**
	 * True iff the pay category is Mileage Taxable and the mileage form is not
	 * null and there are MileageLine objects associated with the Mileage instance.
	 * Also the rate on the PayExpense instance must be the same as the Mileage rate
	 * in PayrollPreferences.
	 */
//	private boolean splitMileageTaxable;

	/**
	 * True iff the pay category is Mileage Nontaxable and the mileage form is not
	 * null and there are MileageLine objects associated with the Mileage instance.
	 * Also the rate on the PayExpense instance must be the same as the Mileage rate
	 * in PayrollPreferences.
	 */
//	private boolean splitMileageNontaxable;

	/** The date of the last day of the week worked. Needed for setting the
	 *  the date field for PayBreakdownDaily instances that are Box Rentals,
	 *  Reimbursements or Mileage if it is not being split into individual
	 *  PayBreakdownDaily instances.
	 */
	private Date lastWorkedDate;

	/** The date to be assigned to any line items generated.  This will be updated
	 * as the DailyTime entries are processed, or set to the week-ending date
	 * when not applicable. */
	private Date lineDate;

	/** An accumulator for the amount paid for the day currently being processed
	 * in {@link #generateHourly()}. Used during generation of the "daily split"
	 * breakdown, the {@link #addPayBreakdown(PayBreakdownMapped)} method
	 * increment this value. It is then stored in DailyTime.payAmount before
	 * switching the process to a different date. */
	private BigDecimal dailyPay;

	/** The account value used by {@link #filterByAccount} to select the
	 * PayJob entries currently being selected. */
	private String filterAccount;

	/** The HtgData instance has most of the information needed to do the
	 * payroll/HTG processing. */
	private HtgData htg;

	/** The current audit trail event to use for logging. */
	private AuditEvent event;

	/** The FLSA value calculated for the current timecard.  We save this after a 'normal'
	 * breakdown, and re-use that value in the 'daily split' breakdown.  We do this because
	 * the daily split breakdown has its multipliers all set to 1.0, which prevents proper
	 * FLSA calculation. */
	private BigDecimal flsaAmount;

	/** The holiday rule that was in effect when a Holiday-Paid day was processed and
	 * added to the days being paid. */
	private HolidayRule holidayPaidRule;

	/** The holiday rule that was in effect when a Holiday-Worked day was processed and
	 * added to the days being paid. */
	private HolidayRule holidayWorkedRule;

	/** Used to track hours which will be displayed as separate line items in the Pay
	 * Breakdown.  The key is the PayCategory whose label will be displayed on the line
	 * item. */
	private final Map<PayCategory, PayJobHours[]> specialHoursMap = new HashMap<>();

	/** The only constructor. */
	public PayBreakdownService(HtgData pHtg) {
		htg = pHtg;
	}

	/**
	 * This is the main calling point for calculating the Pay Breakdown line
	 * items based on data in the PayJob tables and the WeeklyTimecard. However,
	 * the bulk of the work is done in either {@link #generateHourly()}, for
	 * non-exempt employees, or {@link #generateOnCall()}, for exempt employees.
	 * <p>
	 * If a 'daily split' pay breakdown is required, this method will generate both
	 * the usual 'merged' breakdown as well as the 'split' breakdown.
	 *
	 * @param parent The AuditEvent in use by our "parent" task.
	 * @return True iff at least one PayBreakdown item is created for the
	 *         timecard.
	 */
	public boolean generatePayBreakdown(AuditEvent parent) {
		event = htg.service.startEvent(AuditEventType.FILL_PAY_BREAKDOWN, parent, htg.weeklyTimecard);
		htg.auditEvent = event;
		BigDecimal preTotal = htg.weeklyTimecard.getGrandTotal();
		htg.weeklyTimecard.setGrandDailyTotal(null); // force refresh in case we don't complete
		htg.ruleService.getSpecialService().resetRuleLogging(); // log Special rules at least once
		flsaAmount = null;

		if (htg.weeklyTimecard.getUnionNumber() != null && htg.weeklyTimecard.getUnionNumber().equals("UDA")) {
			// for UDA - only necessary for testing via TTCO - force employeeRateType:
			htg.weeklyTimecard.setEmployeeRateType(EmployeeRateType.TABLE);
		}

		boolean splitRequired = false;
		lineDate = null;
		if (htg.production.getPayrollPref().getPayrollService() != null) {
			splitRequired = htg.production.getPayrollPref().getPayrollService().getBreakByDay();
		}

		removePayBreakdown(htg);

		if (htg.weeklyTimecard.getPayJobs().size() == 0) {
			WeeklyTimecardDAO.addPayJob(htg.weeklyTimecard);
		}

		// Check for missing Daily rate in PayJob. LS-3024
		if (htg.weeklyTimecard.getDailyRate() != null) {
			// for some unions, e.g., DGA, may have daily rate even if non-exempt!
			for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
				if (pj.getDailyRate() == null) { // happens if user cleared out field
					pj.setDailyRate(htg.weeklyTimecard.getDailyRate()); // use default
				}
			}
		}

		if (htg.processHotCosts ) {
			return generateHotCosts(parent);
		}

		boolean ret;
		boolean tours = htg.production.getType().isTours();
		if (tours) {
			ret = generateTours(event);
			splitRequired = false; // already did split pay
		}
		else {
			if (! checkLayoff()) {
				return false;
			}
			ret = generatePayBreakdownSub();
		}

		if (ret) {
			if (splitRequired) {
				lineDate = htg.weeklyTimecard.getEndDate();  // will get updated as necessary
				lastWorkedDate = lineDate;
				split = true;	// generate daily-split version of breakdown
//				splitMileageNontaxable = false;
//				splitMileageTaxable = false;
				// Go through the DailyTime instances to find the last day that has hours or is
				// marked as worked.
				List<DailyTime>dts = htg.weeklyTimecard.getDailyTimes();
				for(int i = 6; i >= 0; i--) {
					DailyTime dt = dts.get(i);
					if((dt.getHours() != null && dt.getHours().signum() > 0) || dt.getWorked()) {
						lastWorkedDate = dt.getDate();
						break;
					}
				}

				ret = generatePayBreakdownSub();
			}
			if (ret) {
				renumberPayLines(htg.weeklyTimecard);  // just to be safe!
				TimecardUtils.renumberExpenseLines(htg.weeklyTimecard);  // just to be safe!
				// Calc the total field for each Pay Breakdown line item, and the grand totals
				TimecardCalc.calculatePayBreakdown(htg.weeklyTimecard); // totals for regular PB items
				htg.weeklyTimecard.calculatePayBreakdownDaily(); // totals for "daily split" PB items

				if (! htg.ruleService.getSpecialService().postPhase(null, HtgPhase.PAY_BREAKDOWN, event)) {
					LOG.debug("special service post-phase returned false");
				}

				BigDecimal postTotal = (tours ? htg.weeklyTimecard.getGrandDailyTotal() : htg.weeklyTimecard.getGrandTotal());

				String msg = "Gross=" + postTotal;
				event.appendSummaryLine(msg);
				AuditUtils.updateEvent(event);

				TimecardChangeEvent tcChange = new TimecardChangeEvent(SessionUtils.getCurrentUser(), TimedEventType.AUTO_PAY, htg.weeklyTimecard.getId(),
						TimecardFieldType.GRAND_TOTAL, -1, null, preTotal, postTotal);
				TimecardChangeEventDAO.getInstance().save(tcChange);

//				if (preTotal != null && postTotal != null && preTotal.compareTo(postTotal) != 0) {
//					log.debug("****** total changed ******* " + htg.weeklyTimecard);
//				}
				if (! tours) {
					ret = htg.weeklyTimecard.getPayLines().size() > 0;
				}

				htg.auditEvent = parent;
			}
		}

		return ret;
	}

	/**
	 * Generate the Pay Breakdown to be used by the Hot Costs feature.
	 *
	 * @param parent The audit event to use for tracking.
	 * @return True iff at least one PayBreakdown item is created for the
	 *         timecard.
	 */
	private boolean generateHotCosts(AuditEvent parent) {

		boolean ret = generatePayBreakdownSub(); // TODO is this (non-split breakdown) required?

//		if (splitRequired) {
		// Hot Costs always requires a daily-split breakdown, since it's looking for a single day's cost!
			lineDate = htg.weeklyTimecard.getEndDate();  // will get updated as necessary
			split = true;
			ret = generatePayBreakdownSub();
//		}

		if (ret) {
			htg.weeklyTimecard.calculatePayBreakdownDaily(); // generate totals

			if (! htg.ruleService.getSpecialService().postPhase(null, HtgPhase.PAY_BREAKDOWN, event)) {
				LOG.debug("special service post-phase returned false");
			}
		}

		return ret;
	}

	/**
	 * Generate the Pay Breakdown for a Tours timecard.
	 *
	 * @param parent The audit event to use for tracking.
	 * @return True iff at least one PayBreakdown item is created for the
	 *         timecard.
	 */
	private boolean generateTours(AuditEvent parent) {
		lineDate = htg.weeklyTimecard.getEndDate();  // will get updated as necessary
		split = true;	// generate daily-split version of breakdown
		boolean ret = false;
		lineNum = 0;
		PayJob pj = new PayJob();
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getPayAmount() != null && dt.getPayAmount().signum() != 0) {
				// employee was paid for the day; generate a 'flat-rate' line item
				lineDate = dt.getDate();
				// Generate effective hourly rate; needed for AS400. LS-3826
				BigDecimal rate = dt.getPayAmount().divide(Constants.DAILY_STRAIGHT_HOURS, 4, RoundingMode.UP);
				addLine(pj, PayCategory.FLAT_RATE, Constants.DAILY_STRAIGHT_HOURS, BigDecimal.ONE, rate, false, null);
				ret = true;
			}
		}

		// Add breakdown items corresponding to Expense table items
		lastWorkedDate = lineDate;  // fix blank dates in Tours (timesheet) payBreakdownDaily expense items.
		for (PayExpense exp : htg.weeklyTimecard.getExpenseLines()) {
			addExpense(exp);
		}

		return ret;
	}

	/**
	 * Generate all the Pay Breakdown line items; labor items are based on data in the PayJob tables
	 * and the WeeklyTimecard, and are generated by either
	 * {@link #generateHourly()}, for non-exempt employees, or
	 * {@link #generateOnCall()}, for exempt employees. Items generated from the Expense/Reimbursement table
	 * are created by {@link #addExpense(PayExpense)}.  Additional 'special case' items are generated
	 * by {@link #generateFlsa()} and .
	 * <p>
	 * This method generates EITHER the 'daily split' pay breakdown, or the
	 * usual 'merged' breakdown, based on the value of the {@link #split} field.
	 * When both regular and split breakdowns are required, this method will be
	 * called twice.
	 *
	 * @return True iff at least one PayBreakdown item is created for the
	 *         timecard.
	 */
	private boolean generatePayBreakdownSub() {
		// Initialize "global" values prior to generation
		lineNum = 0;
		specialHoursMap.clear();
		htg.skipOcGuarantee = false;
		htg.meal1AdjHours = BigDecimal.ZERO;
		htg.meal2AdjHours = BigDecimal.ZERO;

		if (! htg.ruleService.getSpecialService().prePhase(null, HtgPhase.PAY_BREAKDOWN, event)) {
			return false;
		}

		// One of these two methods will do most of the work!
		if (htg.weeklyTimecard.getAllowWorked()) {
			generateOnCall(); // generate breakdown for Exempt (flat-rate) employees
		}
		else {
			generateHourly(); // generate breakdown for non-Exempt (hourly) employees
		}

		// Add breakdown items corresponding to Expense table items
		for (PayExpense exp : htg.weeklyTimecard.getExpenseLines()) {
			addExpense(exp);
		}

		// The following line items, when generated in the Daily Split table, will be
		// given the 'last worked date' from the timecard, rather than the week-ending date.
		lineDate = lastWorkedDate;

		// We compute FLSA at end if any overtime rule in the week has flag on.
		if (flsaAmount != null) { // calculated on prior pass (i.e., normal breakdown)
			if (flsaAmount.signum() > 0) {
				addLine(null, PayCategory.FLSA, BigDecimal.ONE, BigDecimal.ONE, flsaAmount, false, null);
				flsaAmount = null;
			}
		}
		else if (htg.preference.getCalcFlsa()) {
			flsaAmount = BigDecimal.ZERO;
			List<ContractRule> otRules = htg.ruleService.findContractRulesByType(RuleType.OT, event);
			boolean computeFlsa = false;
			for (ContractRule cr : otRules) {
				OvertimeRule ot = OvertimeRuleDAO.getInstance().findOneByRuleKey(cr.getUseRuleKey());
				if (ot != null) {
					if (ot.getApplyFlsa()) {
						computeFlsa = true;
						break;
					}
				}
			}
			if (computeFlsa) {
				// calc FLSA amount and create the line item for it if positive.
				generateFlsa();
			}
		}

		if (htg.nonUnion) {
			// calc & create re-use and commission (LS-2142); includes Model Release (LS-4664)
			generateTalentFees();
		}
		return true;
	}

	/**
	 * Remove all the Pay Breakdown line items, in preparation for recreating
	 * them. They are not deleted from the database yet, but held until the user
	 * does either a Save or Cancel. (In the case of a multiple "Run HTG", they
	 * will be deleted automatically.)
	 *
	 * @param htg The current HtgData instance.
	 */
	protected static void removePayBreakdown(HtgData htg) {
		for (PayBreakdown pb : htg.weeklyTimecard.getPayLines()) {
			// keep track of PayBreakdown instances we'll need to delete from the database
			if (pb.getId() != null) { // (if null id, has not been saved yet, so ignore.)
				htg.weeklyTimecard.getDeletedPayLines().add(pb);
			}
		}
		htg.weeklyTimecard.setPayLines(new ArrayList<PayBreakdown>());

		for (PayBreakdownDaily pb : htg.weeklyTimecard.getPayDailyLines()) {
			// keep track of PayBreakdown instances we'll need to delete from the database
			if (pb.getId() != null) { // (if null id, has not been saved yet, so ignore.)
				htg.weeklyTimecard.getDeletedPayLines().add(pb);
			}
		}
		htg.weeklyTimecard.setPayDailyLines(new ArrayList<PayBreakdownDaily>());
	}

	/**
	 * Generate all the Pay Breakdown line items for hourly employees. This
	 * includes the lines for labor hours, MPV penalties, holiday hours worked
	 * and not worked, and guaranteed hours.
	 */
	private void generateHourly() {

		// Loop through dailyTimes to:
		// 1. Determine if there are hours for any holiday; count "paid" holidays.
		// 2. Clear payAmount field for non-tours days. LS-1347
		DailyTime holidayDt = null;
		int paidHolidayCount = 0;
		boolean holidayInWeek = false; // at least one holiday occurs during the week
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getDayType() == null) {
				dt.setPayAmount(null);
			}
			else {
				if (dt.getDayType().isHoliday()) {
					holidayDt = dt; // save for later
					holidayInWeek = true;
					if (dt.getDayType() == DayType.HP) {
						// Have a paid (not worked) holiday; this cannot be split
						paidHolidayCount++;
					}
				}
				if (split && (! dt.getDayType().isTours())) {
					// for tours types, amount is set by PayJobService;
					// for non-tours types, it will be accumulated here.
					dt.setPayAmount(BigDecimal.ZERO);
				}
			}
		}

		if (htg.dailyFlat) {
			// non-exempt employee paid as daily, may need to calculate hourly rate
			if (htg.defaultGuarHours == null) {
				htg.defaultGuarHours = htg.weeklyTimecard.getGuarHours();
			}
			if (htg.extendedRate == null && htg.defaultGuarHours != null) {
				calculateExtendedRate();
			}
			if (htg.extendedRate != null) {
				for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
					if (pj.getRate() == null) {
						pj.setRate(htg.extendedRate);
					}
				}
			}
		}

		// Figure out the two Night Premium multipliers (high & low), if they will be needed...
		NtPremiumRule npRule = null;
		BigDecimal npRate = null;
		BigDecimal npLateRate = null;
		for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
			if (pj.getHasNpHours()) {
				int dayCount = 0;
				for (PayJobDaily pjd : pj.getPayJobDailies()) {
					if (pjd.getHasNpHours()) {
						DailyTime dt = htg.weeklyTimecard.getDailyTimes().get(dayCount);
						// update htg.ruleService to current DailyTime:
						htg.ruleService.prepareDailyContractRules(dt, event);
						break;
					}
					dayCount++;
				}
				npRule = htg.ruleService.findNpRule(event);
				if (npRule == null) { // this shouldn't happen, I think!
					npRate = NtPremiumRule.PREMIUM_LOW_RATE;	// Use the "Default" rates
					npLateRate = NtPremiumRule.PREMIUM_HIGH_RATE;
				}
				else {
					npRate = npRule.getNpRate();
					npLateRate = npRule.getLateRate();
				}
				break;
			}
		}

		if (holidayInWeek) { // Holiday during week
			// calculate holiday values due to work hours overlapping into a holiday
			calculateHolidayOverlap();
		}

		filterAccount = null;
		int pjNumber = 0;
		for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
			event.appendSummary("Job#" + pj.getJobNumber() + ": ");

			if (split) {
				// We're creating the "daily split" Pay breakdown table...
				// generate hourly items separately for each day in the week
				for (PayJobDaily pjd : pj.getPayJobDailies()) { // loop through daily entries
					lineDate = pjd.getDate(); 	// used by filterByDate
					pj.calculateTotals(filterByDate); // create PayJob totals just for one day
					filterAccount = pjd.getAccountNumber(); // to populate line items' acct number
					DailyTime dt = htg.weeklyTimecard.getDailyTimes().get(pjd.getDayNum()-1);
					PayCategory pc = null;
					if (dt.getDayType() != null) {
						// want line items for Special categories (Holiday, Travel) to be labeled as such:
						pc = dt.getDayType().getCategory();
						if (pc == null) {
							// LS-781 - check if 6th/7th day category/paycode applies.
//							if (pjd.getHours10() == null && dt.getWorkDayNum() >= 6) {
//								pc = hasDailyOTrule(dt);
//							}
						}
						else if (pc == PayCategory.WRAP_PAID) {
							// LS-1263 - pass normal Hours pay-code instead of "Wrap" for Team daily split
							pc = null;
						}
						else if (pc == PayCategory.TRAVEL && htg.isTeam && htg.nonUnion) {
							pc = null; // LS-1785 for Team, show non-union Travel as work.
						}
						else if (htg.weeklyTimecard.getEmployeeRateType() == EmployeeRateType.TABLE) {
							pc = null;
						}
					}
					dailyPay = dt.getPayAmount(); // get daily pay so far
					if (dailyPay == null) { // normally shouldn't happen, but just in case!
						dailyPay = BigDecimal.ZERO;
					}
					generateHourlyJob(pj, pjNumber, holidayDt, paidHolidayCount, npRate,
							npLateRate, pc, filterByDate);
					if (dt.getDayType() == DayType.OV && pjNumber == 0) {
						generateOvernight(pj);
					}
					calculateAndAddMpvLine(pj, filterByDate); // generate MPVs.
					if ((dt.getDayType() != null) && ! dt.getDayType().isTours()) { // LS-1347
						// for non-tours types, save day's accumulated pay
						dt.setPayAmount(dailyPay);
					}
				}
				pj.calculateTotals(); // restore full totals
				filterAccount = null;

			}
			else {
				// Move hours from "normal" hourly totals into the special totals if those
				// hours will be reported on separate line items in the breakdown.
				calculateSpecialHours(pj, pjNumber);
				if (htg.isCommercial) {
					// Possibly split one payJob into multiple lines based on daily account numbers
					Set<String> accts = new HashSet<>();
					for (PayJobDaily pjd : pj.getPayJobDailies()) {
						if (! StringUtils.isEmpty(pjd.getAccountNumber())) {
							accts.add(pjd.getAccountNumber());
						}
						else { // use TC default Shoot account #
							pjd.setAccountNumber(null);
							accts.add(null);
						}
					}
					if (accts.size() == 1 && accts.iterator().next() == null) {
						generateHourlyJob(pj, pjNumber, holidayDt, paidHolidayCount, npRate,
								npLateRate, null, filterByDate);
					}
					else {
						// Generate line items, by account code, for all normal labor hours:
						for (String acct : accts) {
							filterAccount = acct;
							pj.calculateTotals(filterByAccount);
							generateHourlyJob(pj, pjNumber, holidayDt, paidHolidayCount, npRate,
									npLateRate, null, filterByAccount);
						}
						pj.calculateTotals(); // restore full totals
					}
					filterAccount = null;
				}
				else { // TV/Feature - no need to split PayJob by accountNumber
					// Generate line items for all normal labor hours:
					generateHourlyJob(pj, pjNumber, holidayDt, paidHolidayCount, npRate,
							npLateRate, null, null);
				}
				calculateAndAddMpvLine(pj, null); // generate MPVs.
				if (pjNumber == 0) { // generate any Overnight payments
					generateAllOvernight(pj);
				}
				// Now generate line items for the special hours removed above.
				for (PayCategory pc : specialHoursMap.keySet()) {
					generateSpecialHourLines(pj, pc, specialHoursMap.get(pc)[pjNumber]);
				}
			}

			pjNumber++;
		} // End of PayJob loop

		// Possibly generate an additional line if guaranteed hours were not met (cumulative schedules)
		if (! htg.proRateFraction) { // skip if pro-rating allowed
			generateGuar();
		}

		// Restore normal totals that may have been changed during the breakdown process
		TimecardCalc.calculatePayJobHours(htg.weeklyTimecard);
	}

	/**
	 * Generate the breakdown line items for a single PayJob instance. Used for
	 * both Split and non-Split processing.
	 * <ul>
	 * <li>For Split processing, it is called once for each day in the week,
	 * with the appropriate Filter set.</li>
	 * <li>For non-Split processing, it is usually just called once, unless
	 * items need to be grouped by account codes (commercial only).</li>
	 * </ul>
	 *
	 * @param pj The PayJob entry for which breakdown line items should be
	 *            generated.
	 * @param pjNumber The index of the given PayJob within the timecard's
	 *            job-table list. Origin zero.
	 * @param holidayDt The DailyTime entry which should be used to determine
	 *            the Holiday rule in effect, if necessary. Only used if holiday
	 *            hours are present in the given PayJob.
	 * @param paidHolidayCount The number of paid holiday days for this PayJob,
	 *            if any.
	 * @param npRate The lower night-premium rate, if any.
	 * @param npLateRate The higher night-premium rate, if any.
	 * @param pc PayCategory to be used to label the Pay breakdown line items;
	 *            if null, the normal labor hour labels are generated.  This is only used for Split line processing.
	 * @param pjdFilter The daily filter to be used (or null) for filtering by
	 *            date or account number. This is used when generating touring
	 *            rate entries.
	 */
	private void generateHourlyJob(PayJob pj, int pjNumber, DailyTime holidayDt,
			int paidHolidayCount, BigDecimal npRate, BigDecimal npLateRate,
			PayCategory pc, Filter<PayJobDaily> pjdFilter) {
		// Generate labor (hours) entries for the current PayJob

		String label = null;
		if (pc != null) {
			label = pc.getLabel();
		}

		// Straight time...
		BigDecimal hours10 = pj.getTotal10();
		addLine(pj, PayCategory.X10_HOURS, hours10, PayRateType.H, label == null ? null : label + "@1.0x");
		if (npRate != null) {
			// possibly generate night-premium line items for 1.0x hours
			addLine(pj, PayCategory.NIGHT_PREM_10_1X, pj.getTotal10Np1(),
					BigDecimal.ONE, NumberUtils.safeMultiply(pj.getRate(), npRate), false, label);
			addLine(pj, PayCategory.NIGHT_PREM_20_1X, pj.getTotal10Np2(),
					BigDecimal.ONE, NumberUtils.safeMultiply(pj.getRate(), npLateRate), false, label);
		}

		// Overtime ...
		addLine(pj, PayCategory.X15_HOURS, pj.getTotal15(), PayRateType.H,  label == null ? null : label + "@1.5x");
		if (npRate != null) {
			// possibly generate night-premium line items for 1.5x hours
			addLine(pj, PayCategory.NIGHT_PREM_10_15X, pj.getTotal15Np1(),
					NtPremiumRule.PREMIUM_OT_RATE, NumberUtils.safeMultiply(pj.getRate(), npRate), false, label);
			addLine(pj, PayCategory.NIGHT_PREM_20_15X, pj.getTotal15Np2(),
					NtPremiumRule.PREMIUM_OT_RATE, NumberUtils.safeMultiply(pj.getRate(), npLateRate), false, label);
		}

		// Gold or other special rate times...
		addHoursLine(pj, pj.getTotalCust1(), pj.getCustomMult1(), pj.getCustom1Type(), label, pjdFilter);
		addHoursLine(pj, pj.getTotalCust2(), pj.getCustomMult2(), pj.getCustom2Type(), label, pjdFilter);
		addHoursLine(pj, pj.getTotalCust3(), pj.getCustomMult3(), pj.getCustom3Type(), label, pjdFilter);
		addHoursLine(pj, pj.getTotalCust4(), pj.getCustomMult4(), pj.getCustom4Type(), label, pjdFilter);
		addHoursLine(pj, pj.getTotalCust5(), pj.getCustomMult5(), pj.getCustom5Type(), label, pjdFilter);
		addHoursLine(pj, pj.getTotalCust6(), pj.getCustomMult6(), pj.getCustom6Type(), label, pjdFilter);

		if (holidayDt != null) {
			htg.ruleService.prepareDailyContractRules(holidayDt, event);
			HolidayRule holidayRule = htg.ruleService.findHolidayRule(event);
			if (paidHolidayCount > 0) { // HolidayPaid in week - handle CUME case
				WeeklyRule weeklyRule = htg.ruleService.findWeeklyRule(event);
				if (weeklyRule.getCumeHours() != null) { // weekly (cumulative hours) schedule
					htg.ruleService.appendRule(weeklyRule, event, null);
					htg.ruleService.appendRule(holidayRule, event, null);
					// Add line for "not worked" holiday
					BigDecimal fraction = holidayRule.getNotWorkedWeeklyRate().evaluate(
							BigDecimal.ONE);
					BigDecimal rate = htg.weeklyTimecard.getWeeklyRate();
					if (rate == null) {
						rate = calculateWeeklyGuarantee(weeklyRule, pj.getRate());
						if (rate == null) {
							TimecardService.error(htg, "Timecard.HTG.HolidayMissingWeeklyRate");
							rate = BigDecimal.ZERO;
						}
					}
					addLine(pj, PayCategory.HOLIDAY_PAID, fraction,
							new BigDecimal(paidHolidayCount), rate, false, null);
				}
			}
			else if (holidayRule.getHourlyRate() == PayFraction.T1) {
				// Holiday rule calls for table-rate (hourly, from ContractRate table)
				addLine(pj, PayCategory.HOLIDAY_WKD, hours10, PayRateType.T, "Holiday Pay");
				addLine(pj, PayCategory.HOLIDAY_WKD, pj.getTotal15(), PayRateType.T,  "Holiday Pay");
				// TODO additional columns ...
			}
		}
	}

	/**
	 * Generate the Pay Breakdown lines related to one of the "special" hour
	 * groups (holidays, idle or travel). Up to six lines could be added, one
	 * for each possibly hourly multiplier, but that would be very odd.
	 * Typically it will add one or two lines (the second for overtime holiday
	 * time).
	 * <p>
	 * This method is NOT used for the "split" (daily) line entries.
	 *
	 * @param pj The PayJob containing the account codes and rate to use.
	 * @param pc The PayCategory used to generate the breakdown labels.
	 * @param pjHours The PayJobHours containing the worked-hours information.
	 *
	 */
	private void generateSpecialHourLines(PayJob pj, PayCategory pc, PayJobHours pjHours) {
		addLine(pj, pc, pjHours.getHours10(), BigDecimal.ONE, pj.getRate(), true, null);
		addLine(pj, pc, pjHours.getHours15(), Constants.DECIMAL_ONE_FIVE, pj.getRate(), true, null);

		BigDecimal rate;
		rate = (pj.getCustomMult1().signum() >= 0 ? pj.getRate() : pj.getDailyRate());
		addLine(pj, pc, pjHours.getHoursCust1(), pj.getCustomMult1(), rate, true, null);

		rate = (pj.getCustomMult2().signum() >= 0 ? pj.getRate() : pj.getDailyRate());
		addLine(pj, pc, pjHours.getHoursCust2(), pj.getCustomMult2(), rate, true, null);

		rate = (pj.getCustomMult3().signum() >= 0 ? pj.getRate() : pj.getDailyRate());
		addLine(pj, pc, pjHours.getHoursCust3(), pj.getCustomMult3(), rate, true, null);

		rate = (pj.getCustomMult4().signum() >= 0 ? pj.getRate() : pj.getDailyRate());
		addLine(pj, pc, pjHours.getHoursCust4(), pj.getCustomMult4(), rate, true, null);

		rate = (pj.getCustomMult5().signum() >= 0 ? pj.getRate() : pj.getDailyRate());
		addLine(pj, pc, pjHours.getHoursCust5(), pj.getCustomMult5(), rate, true, null);

		rate = (pj.getCustomMult6().signum() >= 0 ? pj.getRate() : pj.getDailyRate());
		addLine(pj, pc, pjHours.getHoursCust6(), pj.getCustomMult6(), rate, true, null);
	}

	/**
	 * Generate one Overnight pay breakdone line item for the sum off all the
	 * Overnight DayTypes in the current timecard.
	 *
	 * @param pj The PayJob (job table entry) to use for account codes.
	 */
	private void generateAllOvernight(PayJob pj) {
		StartForm sf = htg.weeklyTimecard.getStartForm();

		if (sf != null && sf.getOvernight() != null && sf.getOvernight().signum() != 0) {
			int over = 0;
			for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
				if (dt.getDayType() == DayType.OV) {
					over++;
				}
			}
			if (over > 0) {
				addLine(pj, PayCategory.OVERNIGHT, new BigDecimal(over), BigDecimal.ONE, sf.getOvernight(), false, null);
			}
		}
	}

	/**
	 * Generate an "overnight" pay breakdown line item.  Used for hybrid productions, when
	 * the DayType is Overnight.
	 * @param pj The associated PayJob (job table) entry; may be used for account codes.
	 */
	private void generateOvernight(PayJob pj) {
		StartForm sf = htg.weeklyTimecard.getStartForm();
		if (sf != null &&  sf.getOvernight() != null && sf.getOvernight().signum() != 0) {
			addLine(pj, PayCategory.OVERNIGHT, BigDecimal.ONE, BigDecimal.ONE, sf.getOvernight(), false, null);
		}
	}

	/**
	 * Conditionally generates a Pay Breakdown line item if the employee had a
	 * guaranteed number of hours for the week (cumulative schedules), and has
	 * not yet been paid for that many hours; or has a guaranteed number of days
	 * and hasn't been paid for that many.
	 */
	private void generateGuar() {
		WeeklyRule weeklyRule = htg.ruleService.findWeeklyRule(event);

		// Check for weekly cumulative guarantee
		if (weeklyRule != null && weeklyRule.getCumeDays() != null) {
			if (weeklyRule.getCumeHours() != null && weeklyRule.getCumeHours().signum() > 0) {
				generateHourlyGuar(weeklyRule);
			}
			else {
				generateDailyGuar(weeklyRule);
			}
		}
	}

	/**
	 * Conditionally generates a Pay Breakdown line item if the employee had a
	 * guaranteed number of hours for the week (cumulative schedules), and has
	 * not yet been paid for that many hours.
	 */
	private void generateHourlyGuar(WeeklyRule weeklyRule) {
		PayJob job1 = htg.weeklyTimecard.getPayJobs().get(0);
		// Compute equivalent 1.0x hours to meet guarantee = for example, (40 + ((54-40)*1.5)) = 61
		BigDecimal guar = weeklyRule.getOvertimeWeeklyBreak().add(
				weeklyRule.getCumeHours().subtract(weeklyRule.getOvertimeWeeklyBreak()).
				multiply(weeklyRule.getOvertimeRate()));

		// Time paid as 6th or 7th day is not counted towards guarantee...
		// so figure out which days are to be included vs excluded:
		boolean include[] = new boolean[7];
		boolean no6th7th = true; // if no days to exclude, we can use shorter calcs!
		int ix = 0;
		int regularDaysWorked = 0; // worked, or not deserving of guarantee, e.g., HOA.
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getWorkDayNum() > 5) {
				include[ix] = false;
				no6th7th = false;
			}
			else {
				include[ix] = true;
				if (dt.getWorkDayNum() != 0 || dt.getDayType() == DayType.HA) {
					regularDaysWorked++;
				}
			}
			ix++;
		}

		if (no6th7th && regularDaysWorked >= weeklyRule.getCumeDays()) {
			// Do a quick check; this will be satisfied in most cases, and save processing.
			if (job1.getTotal10().add(job1.getTotal15()).compareTo(guar) >= 0) {
				return; // Job #1's 1.0x + 1.5x hours are enough, no need to check further.
			}
		}

		BigDecimal hours10 = BigDecimal.ZERO;
		BigDecimal hours15 = BigDecimal.ZERO;
		// Accumulate all 1.0x and 1.5x hours, including Night Premium hours
		if (no6th7th) { // we can use existing totals by multiplier, i.e.,
			// we don't need to look at individual daily entries.
			for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
				hours10 = hours10.add(pj.getTotal10());
				hours10 = hours10.add(pj.getTotal10Np1());
				hours10 = hours10.add(pj.getTotal10Np2());
				hours15 = hours15.add(pj.getTotal15());
				hours15 = hours15.add(pj.getTotal15Np1());
				hours15 = hours15.add(pj.getTotal15Np2());
			}
		}
		else { // have to go through day-by-day to exclude 6th/7th days
			for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
				int daynum = 0;
				for (PayJobDaily pjd : pj.getPayJobDailies()) {
					if (include[daynum]) {
						hours10 = NumberUtils.safeAdd(hours10, pjd.getHours10(), pjd.getHours10Np1(), pjd.getHours10Np2());
						hours15 = NumberUtils.safeAdd(hours15, pjd.getHours15(), pjd.getHours15Np1(), pjd.getHours15Np2());
					}
					daynum++;
				}
			}
		}

		if (regularDaysWorked < weeklyRule.getCumeDays()) {
			// Not paid for guaranteed number of days; generate guarantee for this.
			// First get a guarantee rule that should fit "normal" days:
			GuaranteeRule rule = null;
			for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
				if (dt.getWorkDayNum() >= 1 && dt.getWorkDayNum() <= 5) {
					htg.ruleService.prepareDailyContractRules(dt, event);
					rule = htg.ruleService.findGuaranteeRule(event);
					if (rule != null) {
						break;
					}
				}
			}
			if (rule != null) { // Got rule, generate hours for un-worked days
				int guarDays = weeklyRule.getCumeDays() - regularDaysWorked;
				BigDecimal extraMinCall = rule.getMinimumCall().multiply(new BigDecimal(guarDays));
				BigDecimal all10 = hours10.add(extraMinCall); // this would be total 1.0x hours paid
				if (all10.compareTo(weeklyRule.getOvertimeWeeklyBreak()) > 0) {
					// oops, daily guar will push us over weekly OT break (typically 40 hrs)
					all10 = all10.subtract(weeklyRule.getOvertimeWeeklyBreak()); // how much we're over;
					extraMinCall = extraMinCall.subtract(all10); // reduce 1.0x guarantee payment by that much.
					// The hours beyond 1.0x (in 'all10') should be paid as OT, because if the person
					// actually worked those hours each day, they'd get pushed into OT at some point.
					all10 = all10.multiply(weeklyRule.getOvertimeRate()); // so convert to effective 1.0x hours at OT multiple
					all10 = all10.add(weeklyRule.getOvertimeWeeklyBreak()); // add back in the break point (e.g., 40)
					guar = guar.max(all10); // 1.0x-equivalent guarantee must at least match this.
				}
				addLine(job1, PayCategory.MEET_GUARANTEE, extraMinCall,
						BigDecimal.ONE, job1.getRate(), false, null);
				hours10 = hours10.add(extraMinCall); // remaining calcs can assume this was paid
			}
		}

		BigDecimal hours = hours10.add(hours15);
		BigDecimal paid10;

		// in some cases, e.g., Cume40 rule, no OT is included in guarantee.
		boolean cumeNoOT = weeklyRule.getOvertimeWeeklyBreak().compareTo(weeklyRule.getCumeHours()) == 0;
		if (cumeNoOT) {
			paid10 = hours; // count 1.5x time as straight; only applies if guarantee has no OT
		}
		else {
			paid10 = hours10.add(hours15.multiply(weeklyRule.getOvertimeRate()));
		}

		if (guar.compareTo(paid10) > 0) { // have not reached guarantee,
			if (weeklyRule.getCumeDays() > 0) { // see if guar should be reduced due to HOA
				BigDecimal dailyGuar =  guar.divide(new BigDecimal(weeklyRule.getCumeDays()),RoundingMode.HALF_UP);
				for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
					if (dt.getDayType() == DayType.HA || dt.getDayType() == DayType.HP) {
						// deduct for HOA, since it shouldn't be paid
						// deduct for HP, as it is paid separately
						guar = guar.subtract(dailyGuar);
					}
				}
			}
		}

		if (guar.compareTo(paid10) > 0) {	// Still have not reached guarantee
			// See if we need to include other hours over 1.5x
			BigDecimal hours20 = BigDecimal.ZERO;
			BigDecimal nonGold, gold;
			for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
				if (dt.getDayType() != null) {
					htg.ruleService.prepareDailyContractRules(dt, event);
					GuaranteeRule rule = htg.ruleService.findGuaranteeRule(event);
					BigDecimal dailyGuar = BigDecimal.ZERO;
					if (rule != null) {
						dailyGuar = rule.getMinimumCall();
					}
					if (dt.getForcedCall() || dailyGuar.signum() > 0) {
						for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
							PayJobDaily pjd = pj.getPayJobDailies().get(dt.getDayNum() - 1);
							nonGold = calculateNonGoldCustomHours(pj, pjd);
							hours15 = hours15.add(nonGold); // include, e.g., 1.65x hours
							gold = calculateGoldHours(pj, pjd);
							if (gold.signum() > 0) { // may be able to use gold towards guarantee...
								if (dt.getForcedCall()) { // include gold hours due to Forced Call.
									hours20 = hours20.add(gold);
								}
								else {
									// if non-gold is < guarantee, use gold hours up to guarantee.
									// this will pick up guaranteed hours on a worked holiday, or
									// hours overlapping from non-holiday to holiday, paid at gold
									nonGold = nonGold.add(pjd.getTotalNonCustomHours());
									if (nonGold.compareTo(dailyGuar) < 0) {
										gold = dailyGuar.subtract(nonGold).min(gold);
										hours20 = hours20.add(gold);
									}
								}
							}
						}
					}
				}
			}
			if (hours20.signum() > 0) {
				// Some gold hours counted -- recompute paid hours...
				hours = hours.add(hours20);
				if (cumeNoOT) {
					paid10 = hours;
				}
				else {
					paid10 = hours10.add(hours20).add(hours15.multiply(weeklyRule.getOvertimeRate()));
				}
			}

			if (guar.compareTo(paid10) > 0) { // ok, still short of guarantee
				BigDecimal add10;
				if (guar.compareTo(weeklyRule.getOvertimeWeeklyBreak()) < 0) {
					// guarantee is less than weekly break(40); only happens if HOAs
					add10 = guar.subtract(paid10);
					addLine(job1, PayCategory.MEET_GUARANTEE, add10,
							BigDecimal.ONE, job1.getRate(), false, null);
				}
				else { // guarantee is greater than (or equal) to the weekly overtime break (e.g., 40)

					guar = guar.subtract(weeklyRule.getOvertimeWeeklyBreak());
					if (hours10.compareTo(weeklyRule.getOvertimeWeeklyBreak()) < 0) {
						// paid straight time was less than overtime break (40); make up that first
						BigDecimal borrow = BigDecimal.ZERO;
						if (guar.signum() > 0) {
							// compute # of 1.5x hours we can NOT count towards 1x shortage
							borrow = guar.divide(weeklyRule.getOvertimeRate(),RoundingMode.HALF_UP).setScale(4,RoundingMode.HALF_UP);
						}
						borrow = hours15.subtract(borrow); // = 1.5x hours we CAN count towards 1x shortage
						add10 = weeklyRule.getOvertimeWeeklyBreak().subtract(hours10);
						if (borrow.signum() > 0) { // Paid more 1.5x hours than needed for guarantee,
							// so 'borrow' them (they will make up part of guaranteed pay); convert to 1.0x hours:
							borrow = borrow.multiply(weeklyRule.getOvertimeRate());
							add10 = add10.subtract(borrow); // and reduce required 1.0x hours for guarantee
						}
						addLine(job1, PayCategory.MEET_GUARANTEE, add10,
								BigDecimal.ONE, job1.getRate(), false, null);
					}
					if (guar.signum() > 0) { // Some guaranteed 1.5x hours; did we meet them yet?
						guar = guar.divide(weeklyRule.getOvertimeRate(),RoundingMode.HALF_UP).setScale(4,RoundingMode.HALF_UP);
						guar = guar.subtract(hours15);
						if (guar.signum() > 0) { // may be zero if TC had too few 1x hrs but enough (or extra) 1.5x hours
							addLine(job1, PayCategory.MEET_GUARANTEE, guar,
									weeklyRule.getOvertimeRate(), job1.getRate(), false, null);
						}
					}
				}
			}
		}
	}

	/**
	 * Calculate the total of all hours in the 'custom' multiplier columns of
	 * the given PayJobDaily for which the multiplier is less than the
	 * "gold minimum", currently 2.0x. The multipliers for each column are found
	 * in the given PayJob.
	 *
	 * @param pj The PayJob (which holds the PayJobDaily), needed to evaluate
	 *            the column multipliers.
	 * @param pjd The PayJobDaily whose hours are to be accumulated.
	 * @return The total hours in the PayJobDaily corresponding to custom
	 *         columns where the current multiplier is less than 2.0x. May
	 *         return zero, but never returns null.
	 */
	private BigDecimal calculateNonGoldCustomHours(PayJob pj, PayJobDaily pjd) {
		BigDecimal hours = BigDecimal.ZERO;
		if (pj.getCustomMult1().compareTo(TimecardService.MINIMUM_GOLD_RATE) < 0 && pjd.getHoursCust1() != null) {
			hours = hours.add(pjd.getHoursCust1());
		}
		if (pj.getCustomMult2().compareTo(TimecardService.MINIMUM_GOLD_RATE) < 0 && pjd.getHoursCust2() != null) {
			hours = hours.add(pjd.getHoursCust2());
		}
		if (pj.getCustomMult3().compareTo(TimecardService.MINIMUM_GOLD_RATE) < 0 && pjd.getHoursCust3() != null) {
			hours = hours.add(pjd.getHoursCust3());
		}
		if (pj.getCustomMult4().compareTo(TimecardService.MINIMUM_GOLD_RATE) < 0 && pjd.getHoursCust4() != null) {
			hours = hours.add(pjd.getHoursCust4());
		}
		if (pj.getCustomMult5().compareTo(TimecardService.MINIMUM_GOLD_RATE) < 0 && pjd.getHoursCust5() != null) {
			hours = hours.add(pjd.getHoursCust5());
		}
		if (pj.getCustomMult6().compareTo(TimecardService.MINIMUM_GOLD_RATE) < 0 && pjd.getHoursCust6() != null) {
			hours = hours.add(pjd.getHoursCust6());
		}
		return hours;
	}

	/**
	 * Calculate the number of "gold" hours paid on a given day and job.
	 *
	 * @param pj The PayJob being examined.
	 * @param pjd The PayJobDaily whose hours are being totalled.
	 * @return The total number of hours in the given PayJobDaily which are in
	 *         columns of the PayJob whose rate is greater than or equal to the
	 *         minimum gold rate (2.0).
	 */
	private BigDecimal calculateGoldHours(PayJob pj, PayJobDaily pjd) {
		BigDecimal gold = BigDecimal.ZERO;
		if (pj.getCustomMult1().compareTo(TimecardService.MINIMUM_GOLD_RATE) >= 0 && pjd.getHoursCust1() != null) {
			gold = gold.add(pjd.getHoursCust1());
		}
		if (pj.getCustomMult2().compareTo(TimecardService.MINIMUM_GOLD_RATE) >= 0 && pjd.getHoursCust2() != null) {
			gold = gold.add(pjd.getHoursCust2());
		}
		if (pj.getCustomMult3().compareTo(TimecardService.MINIMUM_GOLD_RATE) >= 0 && pjd.getHoursCust3() != null) {
			gold = gold.add(pjd.getHoursCust3());
		}
		if (pj.getCustomMult4().compareTo(TimecardService.MINIMUM_GOLD_RATE) >= 0 && pjd.getHoursCust4() != null) {
			gold = gold.add(pjd.getHoursCust4());
		}
		if (pj.getCustomMult5().compareTo(TimecardService.MINIMUM_GOLD_RATE) >= 0 && pjd.getHoursCust5() != null) {
			gold = gold.add(pjd.getHoursCust5());
		}
		if (pj.getCustomMult6().compareTo(TimecardService.MINIMUM_GOLD_RATE) >= 0 && pjd.getHoursCust6() != null) {
			gold = gold.add(pjd.getHoursCust6());
		}
		return gold;
	}

	/**
	 * Conditionally generates a Pay Breakdown line item if the employee had a
	 * guaranteed number of days for the week, and has not yet been paid for
	 * that many days.
	 */
	private void generateDailyGuar(WeeklyRule weeklyRule) {

		int worked = 0; // count how many days they worked
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.reportedWork() && dt.getWorkDayNum() < 6) {
				worked++;
			}
		}

		if (worked < weeklyRule.getCumeDays()) {
			// Need to pay for unworked days.
			int days = weeklyRule.getCumeDays() - worked;
			// Calculate number of hours to pay per day =
			//   "weekly hours" / "cume days" from rule.
			BigDecimal guar = weeklyRule.getOvertimeWeeklyBreak();
			guar.setScale(1);  // one decimal place
			guar =  guar.divide(new BigDecimal(weeklyRule.getCumeDays()),RoundingMode.HALF_UP);
			guar = guar.multiply(new BigDecimal(days));
			if (guar.signum() > 0) { // should be
				PayJob pj = htg.weeklyTimecard.getPayJobs().get(0);
				addLine(pj, PayCategory.MEET_GUARANTEE, guar,
						BigDecimal.ONE, pj.getRate(), false, null);
			}
		}

	}

	/**
	 * Generates the appropriate Pay Breakdown line item(s) for an On-Call
	 * schedule. From one to several lines may be added -- days may be split by
	 * Pay Job due to shoot/prep, studio/distant, or rate/contract change, plus
	 * separate lines are generated (if necessary) for 6th day, 7th day,
	 * holidays worked, and holidays not worked.
	 */
	private void generateOnCall() {

		PayJobService.calculateWorkDayNumbers(htg.weeklyTimecard, htg);

		/**
		 * 'ruleDaysPerJob' is a mapping from rule keys to 'days-per-PayJob'
		 * lists. Each key is either (a) a rule name, for TV/Features, or (b)
		 * <rule name + "!" + accountNumber>, for Commercials. The account
		 * number is taken from the appropriate PayJobDaily.accountNumber field.
		 * <p>
		 * The value corresponding to the rule key is a List of BigDecimal`s,
		 * where the entry with index 'n' corresponds to the number of worked
		 * days to be assigned to PayJob number 'n'. (The 0-th entry in the List
		 * is unused.)
		 */
		Map<String, List<BigDecimal>> ruleDaysPerJob = new HashMap<>();

		// Generate mapping of rules to days-per-job:
		BigDecimal guarantee = fillRuleDaysMap(ruleDaysPerJob);

		if (guarantee.signum() < 0) {
			// negative guar = some error was found and message issued.
			return;
		}

		BigDecimal rate = htg.weeklyTimecard.getRate();
		boolean weekly = true;
		if (htg.weeklyTimecard.getDailyRate() != null && htg.weeklyTimecard.getWeeklyRate() == null) {
			weekly = false;
		}

		// Check for missing rate in PayJob. LS-3024
		for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
			if (weekly) {
				if (pj.getWeeklyRate() == null) { // happens if user cleared out field
					pj.setWeeklyRate(rate); // use default
				}
			}
			else { // daily
				if (pj.getDailyRate() == null) { // happens if user cleared out field
					pj.setDailyRate(rate); // use default
				}
			}
		}

		/** This accumulates the total number of days-worth of pay generated in the lines added below. */
		BigDecimal daysPaid = BigDecimal.ZERO;

		/* We now have all days worked distributed across the jobs and the rules.
		 * Process each rule in turn, applying its multipliers to the number of
		 * days assigned to each PayJob. */

		if (split) { // Doing day-by-day split breakdown
			daysPaid = generateOnCallSplit(); // this includes holidays, too.
			if (daysPaid == null) { // error in process; message was issued.
				return;
			}
		}
		else { // doing normal (summarized) breakdown
			daysPaid = generateOnCallDays1to5(ruleDaysPerJob, rate, weekly);
			// Do Holidays, both paid and unpaid:
			daysPaid = generateOnCallHoliday(ruleDaysPerJob, rate, weekly, daysPaid);
			// Do "touring" days
			daysPaid = generateOnCallTours(daysPaid);
			// Now handle day 6 & 7 payments, if necessary
			StartForm sf = htg.weeklyTimecard.getStartForm();
			if ((! htg.nonUnion) || sf.getPay6th7thDay()
					|| sf.getRateType() == EmployeeRateType.DAILY) {
				// (skip for non-union WeeklyExempt whose "pay 6/7" check box is not checked.)
				BigDecimal daysPaidAll = generateOnCallDays67(rate, weekly, daysPaid);
				if (daysPaidAll == null) {
					// error in process; message issued.
					return;
				}
			}
			// Generate any overtime pay breakdown line items.
			// This only applies to a few unions, such as 399 Location Managers.
			generateOnCallOT();
			PayJob pj = htg.weeklyTimecard.getPayJobs().get(0);
			generateAllOvernight(pj); // Generate Overnight payments. LS-2939
		}

		for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
			// Calculate MPV line for each PayJob (most OnCalls won't have any)
			if ((pj.getTotalMpv1() != null && pj.getTotalMpv1() > 0) ||
					(pj.getTotalMpv2() != null && pj.getTotalMpv2() > 0) ||
					(pj.getTotalMpv3() != null && pj.getTotalMpv3() > 0)) {
				calculateAndAddMpvLine(pj, null);
			}
		}

		// Done with all explicit payments -- see if more required to meet guarantee
		if (! (htg.skipOcGuarantee || htg.proRateFraction || htg.hasLayoff)) {
			filterAccount = null;
			PayJob pj = htg.weeklyTimecard.getPayJobs().get(0); // assume job #1 rate, codes
			if (htg.isSimpleWeekly) { // Generate exactly one week's pay. LS-2189
				if ((! split) && guarantee.signum() > 0) {
					// Generate flat-rate line item for unpaid days
					// (split handled this in generateOnCallSplit())
					guarantee = guarantee.multiply(Constants.DECIMAL_FIVE).subtract(daysPaid);
					if (guarantee.signum() > 0) {
						addLine(pj, PayCategory.FLAT_RATE, Constants.DECIMAL_POINT_TWO, guarantee,
								pj.getWeeklyRate(), false, null);
					}
				}
			}
			else if (isIntegerValue(daysPaid)) {
				guarantee = guarantee.multiply(Constants.DECIMAL_FIVE).subtract(daysPaid);
				if (guarantee.signum() > 0) { // guarantee was not met yet, add pay line item
					addLine(pj, PayCategory.MEET_GUARANTEE, Constants.DECIMAL_POINT_TWO, guarantee,
							pj.getWeeklyRate(), false, null);
				}
			}
			else {
				daysPaid = daysPaid.divide(Constants.DECIMAL_FIVE); // change days to fraction of a week
				guarantee = guarantee.subtract(daysPaid);
				if (guarantee.signum() > 0) { // guarantee was not met yet, add pay line item
					addLine(pj, PayCategory.MEET_GUARANTEE, guarantee, BigDecimal.ONE,
							pj.getWeeklyRate(), false, null);
				}
			}
		}
	}

	/**
	 * Generate the pay breakdown line items for work-day numbers 1 through 5,
	 * based on the information in the ruleDaysPerJob map supplied.
	 *
	 * @param ruleDaysPerJob the mapping of rules to days-per-PayJob Lists.
	 * @param rate The default rate to pay if no weekly or daily rate is in a
	 *            PayJob being processed.
	 * @param weekly True if this is a weekly exempt/on-call employee.
	 * @return the number of days paid so far; should be between 0.0 and 5.0.
	 */
	private BigDecimal generateOnCallDays1to5(Map<String, List<BigDecimal>> ruleDaysPerJob,
			BigDecimal rate, boolean weekly) {
		BigDecimal daysPaid = BigDecimal.ZERO;
		OnCallRule ocRule;
		BigDecimal fraction;
		int ix;
		String ruleName;
		for (String mapKey : ruleDaysPerJob.keySet()) {
			if ((ix = mapKey.indexOf(RULE_SPECIAL_CHAR)) > 0) {
				// must be a Commercial production; account number follows the RULE_SPECIAL_CHAR
				filterAccount = mapKey.substring(ix+1);
				ruleName = mapKey.substring(0, ix);
			}
			else if (ix == 0) {
				continue;	// key was one of our "fake" rules, e.g., for Holiday Paid days
			}
			else { // TV or Feature production
				filterAccount = null;
				ruleName = mapKey;
			}
			ocRule = OnCallRuleDAO.getInstance().findOneByRuleKey(ruleName);
			List<BigDecimal> daysList = ruleDaysPerJob.get(mapKey);
			if (weekly) {
				fraction = ocRule.getDays1to5Base().evaluate(BigDecimal.ONE);
			}
			else {
				fraction = BigDecimal.ONE;
			}

			for (int jobnum = 1; jobnum < daysList.size(); jobnum++) {
				PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobnum-1);
				BigDecimal days = daysList.get(jobnum);
				if (days.signum() > 0) {
					days = days.multiply(ocRule.getDays1to5Mult());
					if (days.signum() > 0) { // can be zero if a no-pay rule applies on "worked" day
						BigDecimal pjRate = pj.getWeeklyRate();
						if (pjRate == null) {
							if (! weekly) {
								pjRate = pj.getDailyRate();
							}
							else {
								pjRate = rate;
							}
						}
						// Break out the Travel to generate their own PayBreakdown item.
//						int travelDays = 0;
//						List<DailyTime> dts = htg.weeklyTimecard.getDailyTimes();
//
//						for(int day = 1; day <= days.intValue(); day++) {
//							DailyTime dt = dts.get(day - 1);
//
//							if(dt.getDayType().isTravel()) {
//								travelDays++;
//							}
//						}

						addLine(pj, PayCategory.FLAT_RATE, fraction, days, pjRate, false, null);
//						if(travelDays > 0) {
//							// Break out the travel days
//							addLine(pj, PayCategory.TRAVEL, fraction, BigDecimal.valueOf(travelDays), pjRate, false, null);
//						}
						daysPaid = daysPaid.add(days);
					}
				}
			}
		}
		return daysPaid;
	}

	/**
	 * Generate the pay breakdown line items for work-day numbers 6 and 7.
	 *
	 * @param rate The default rate to pay if no weekly or daily rate is in a
	 *            PayJob being processed.
	 * @param weekly True if this is a weekly exempt/on-call employee.
	 * @param daysPaid the number of days paid prior to this; should be between
	 *            0.0 and 5.0.
	 * @return the number of days paid including any line items generated here;
	 *         should be between 0.0 and 7.0. Returns null if an applicable
	 *         OnCallRule is not found.
	 */
	private BigDecimal generateOnCallDays67(BigDecimal rate, boolean weekly, BigDecimal daysPaid) {
		int numJobs = htg.weeklyTimecard.getPayJobs().size();
		OnCallRule ocRule;
		BigDecimal fraction;
		int dayCount = 0;
		final String ruleKey = "X"; // dummy rule name to use as Map key
		Map<String, List<BigDecimal>> mapRuleDays = new HashMap<>(1);

		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getDayType() != null && (! dt.getDayType().isHoliday()) &&
					(! dt.getDayType().isTours()) &&
					(dt.getWorkDayNum() > 5)) {
				if (dt.reportedWork() || dt.getDayType().isAlwaysPaid() || dt.getDayType().isIdle()) {
					htg.ruleService.prepareDailyContractRules(dt, event);
					ocRule = htg.ruleService.findOnCallRule(event);
					if (ocRule == null) {
						TimecardService.error(htg, "Timecard.HTG.NoOnCallRule");
						daysPaid = null;
						break;
					}
					PayCategory cat;
					BigDecimal mult;
					mapRuleDays.clear();
					BigDecimal restDays = BigDecimal.ZERO;
					// With LS-2241, all Rest invasion calculation is handled in PayJobService.
//LS-2241			if (dt.getForcedCall()) {
//						RestRule rRule = htg.ruleService.findRestRule(event);
//						if (rRule != null && rRule.getForcedPayMethod().isDaily()) {
//							restDays = rRule.getForcedPayMethod().getMultiplier();
//						}
//					}
					splitOnCallDay(dt, mapRuleDays, ruleKey, restDays, numJobs);
					List<BigDecimal> daySplitPerJob = mapRuleDays.values().iterator().next();// mapRuleDays.get(ruleKey);

					boolean traveling = dt.getDayType().isTravel();

					if (dt.getWorkDayNum() == 6) {
						cat = PayCategory.DAY6;
						if(traveling) {
							cat =  PayCategory.TRAVEL;
						}
						mult =  ocRule.getDay6Mult();
						fraction = ocRule.getDay6Base().evaluate(BigDecimal.ONE);
					}
					else {
						cat = PayCategory.DAY7;
						if(traveling) {
							cat =  PayCategory.TRAVEL;
						}
						mult =  ocRule.getDay7Mult();
						fraction = ocRule.getDay7Base().evaluate(BigDecimal.ONE);
					}
					if (! weekly) {
						fraction = BigDecimal.ONE;
					}
					if (dt.getDayType().isIdle()) {
						cat = PayCategory.IDLE_PAY;
					}
					for (int jobnum = 1; jobnum < daySplitPerJob.size(); jobnum++) {
						PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobnum-1);
						BigDecimal days = daySplitPerJob.get(jobnum);
						if (days.signum() > 0) {
							daysPaid = daysPaid.add(days);
							days = days.multiply(fraction);
							BigDecimal pjRate = pj.getWeeklyRate();
							if (pjRate == null) {
								if (! weekly) {
									pjRate = pj.getDailyRate();
								}
								else {
									pjRate = rate;
								}
							}
							PayJobDaily pjd = pj.getPayJobDailies().get(dayCount);
							filterAccount = pjd.getAccountNumber(); // acct# for PB if commercial
							addLine(pj, cat, days, mult, pjRate, false, null);
						}
					}
				}
			}
			dayCount++; // sequential day number within payroll week (Sun=0)
		}
		return daysPaid;
	}

	/**
	 * Generate the pay breakdown line items for Holidays Paid or Worked, based
	 * on the information in the ruleDaysPerJob map supplied.
	 *
	 * @param ruleDaysPerJob the mapping of rules to days-per-PayJob Lists.
	 * @param rate The default rate to pay if no weekly or daily rate is in a
	 *            PayJob being processed.
	 * @param weekly True if this is a weekly exempt/on-call employee.
	 * @param daysPaid the number of days paid prior to this; should be between
	 *            0.0 and 5.0.
	 * @return the number of days paid including any line items generated here;
	 *         should be between 0.0 and 7.0.
	 */
	private BigDecimal generateOnCallHoliday(Map<String, List<BigDecimal>> ruleDaysPerJob,
			BigDecimal rate, boolean weekly, BigDecimal daysPaid) {
		BigDecimal fraction;
		List<BigDecimal> holidaysPaidPerJob = null;
		List<BigDecimal> holidaysWorkedPerJob = null;
		String holidayPaidAccount = null;
		String holidayWorkedAccount = null;

		int ix;
		for (String mapKey : ruleDaysPerJob.keySet()) {
			if (mapKey.startsWith(HP_KEY)) {
				holidaysPaidPerJob = ruleDaysPerJob.get(mapKey);
				if ((ix = mapKey.indexOf(RULE_SPECIAL_CHAR, 1)) > 0) {
					holidayPaidAccount = mapKey.substring(ix+1);
				}
			}
			else if (mapKey.startsWith(HW_KEY)) {
				holidaysWorkedPerJob = ruleDaysPerJob.get(mapKey);
				if ((ix = mapKey.indexOf(RULE_SPECIAL_CHAR, 1)) > 0) {
					holidayWorkedAccount = mapKey.substring(ix+1);
				}
			}
		}

		if (holidaysPaidPerJob != null || holidaysWorkedPerJob != null) {
			HolidayRule holidayRule = null;
			if (holidaysPaidPerJob != null) {
				holidayRule = holidayPaidRule;
				filterAccount = holidayPaidAccount; // acct# for PB if commercial
				if (weekly) {
					fraction = holidayRule.getNotWorkedOnCallRate().evaluate(BigDecimal.ONE);
				}
				else if (holidayRule.getOnCallRate() == PayFraction.N_A) {
					fraction = BigDecimal.ZERO;
				}
				else {
					fraction = BigDecimal.ONE;
				}
				for (int jobnum = 1; jobnum < holidaysPaidPerJob.size(); jobnum++) {
					PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobnum-1);
					BigDecimal days = holidaysPaidPerJob.get(jobnum);
					if (days.signum() > 0) {
						BigDecimal pjRate = pj.getWeeklyRate();
						if (pjRate == null) {
							if (! weekly) {
								pjRate = pj.getDailyRate();
							}
							else {
								pjRate = rate;
							}
						}
						addLine(pj, PayCategory.HOLIDAY_PAID, fraction, days, pjRate, false, null);
						daysPaid = daysPaid.add(days);
					}
				}
			}

			if (holidaysWorkedPerJob != null) {
				holidayRule = holidayWorkedRule;
				filterAccount = holidayWorkedAccount; // acct# for PB if commercial
				if (weekly || holidayRule.getOnCallRate().isDaily()) {
					fraction = holidayRule.getOnCallRate().evaluate(BigDecimal.ONE);
				}
				else if (holidayRule.getOnCallRate() == PayFraction.N_A) {
					fraction = BigDecimal.ZERO;
				}
				else {
					fraction = BigDecimal.ONE;
				}
				for (int jobnum = 1; jobnum < holidaysWorkedPerJob.size(); jobnum++) {
					PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobnum-1);
					BigDecimal days = holidaysWorkedPerJob.get(jobnum);
					if (days.signum() > 0) {
						// Note: "isAdditional" should be handled by having included the
						// normal "Worked" payment earlier in the process.
//						if (holidayRule.getOnCallRate().isAdditional()) {
//							days = days.add(days);
//						}
						BigDecimal pjRate = pj.getWeeklyRate();
						if (pjRate == null) {
							pjRate = rate;
						}
						addLine(pj, PayCategory.HOLIDAY_WKD, fraction, days, pjRate, false, null);
						if (! holidayRule.getOnCallRate().isAdditional()) {
							// when "additional", we don't want these days to count towards
							// guarantee of paid days.
							daysPaid = daysPaid.add(days);
						}
					}
				}
			}
		}
		return daysPaid;
	}

	/**
	 * Generate the "Pay Breakdown Daily" line items for an on-call (exempt)
	 * employee's timecard. This is only called if the assigned payroll service
	 * requires a daily split export, such as for TEAM.
	 * @return the number of days paid; should be between 0.0 and 7.0. Returns null if an applicable
	 *         OnCallRule is not found.
	 */
	private BigDecimal generateOnCallSplit() {
		BigDecimal daysPaid = BigDecimal.ZERO;
		BigDecimal splitRate = htg.weeklyTimecard.getRate();
		if (splitRate == null) {
			splitRate = BigDecimal.ZERO;
		}

		BigDecimal hoursPerDayOrWeek = HtgData.STANDARD_WEEK_HOURS_UNION;
		if (htg.nonUnion) {
			hoursPerDayOrWeek = HtgData.STANDARD_WEEK_HOURS;
		}

		boolean weekly = true;
		if (htg.weeklyTimecard.getDailyRate() != null && htg.weeklyTimecard.getWeeklyRate() == null) {
			weekly = false; // Daily exempt, not weekly
			if (htg.nonUnion) {
				hoursPerDayOrWeek = Constants.WORKED_HOURS_NON_UNION; // default
				if (htg.weeklyTimecard.getGuarHours() != null && hoursPerDayOrWeek.compareTo(htg.weeklyTimecard.getGuarHours()) > 0) {
					// Guarantee for minors is often less than 8
					hoursPerDayOrWeek = htg.weeklyTimecard.getGuarHours();
				}
			}
			else {
				hoursPerDayOrWeek = Constants.WORKED_HOURS_UNION;
			}
		}
		splitRate = splitRate.divide(hoursPerDayOrWeek, 6, RoundingMode.UP);
		BigDecimal simpleRate = splitRate;	// Save for rate on guaranteed days. LS-3024

		int numJobs = htg.weeklyTimecard.getPayJobs().size();
		OnCallRule ocRule;
		BigDecimal fraction, hours;
		int dayCount = 0; // which day number (0..6) we're processing in the loop
		int hoaCount = 0; // number of "HOA" days in the week; LS-2189
		final String ruleKey = "X"; // dummy rule name to use as Map key
		Map<String, List<BigDecimal>> mapRuleDays = new HashMap<>(1);
		List<DailyTime> unusedDays = new ArrayList<>(); // days not paid; LS-2189

		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			boolean dtPaid = false;
			boolean skipPay = false; // may be set true if skipping pay due to 6th/7th day worked
			if (dt.getDayType() != null /*&& (! dt.getDayType().isHoliday())*/) {

				lineDate = dt.getDate();
				boolean includeWork = true;
				HolidayRule holidayRule = null;
				htg.ruleService.prepareDailyContractRules(dt, event);
				if (dt.getDayType() == DayType.HP || dt.getDayType() == DayType.HW) {
					// For Holiday Paid or Worked, if the rule is N/A, or the
					// rule is for "additional" pay, then continue checking for "Worked" days.
					holidayRule = htg.ruleService.findHolidayRule(event);
					if (dt.getDayType() == DayType.HP) {
						includeWork = false;
					}
					else if (holidayRule != null && holidayRule.getOnCallRate() != PayFraction.N_A &&
							! holidayRule.getOnCallRate().isAdditional()) {
						// A non-additional and not N/A rule, it's pay overrides the Worked pay,
						// so we'll skip the next section.
						includeWork = false;
					}
				}

				if (dt.reportedWork() || dt.getDayType().isAlwaysPaid() ||
						dt.getDayType().isIdle() || dt.getDayType() == DayType.HP) {
					if (! dt.getDayType().isTours()) { // Init non-tours daily pay to 0. LS-1572
						dt.setPayAmount(BigDecimal.ZERO);
						// (For touring day types, pay is set in PayJobService)
					}
					ocRule = htg.ruleService.findOnCallRule(event);
					if (ocRule == null) {
						TimecardService.error(htg, "Timecard.HTG.NoOnCallRule");
						daysPaid = null;
						break;
					}
					PayCategory cat;
					BigDecimal mult = BigDecimal.ONE;
					fraction = ocRule.getDays1to5Base().evaluate(BigDecimal.ONE);
					mapRuleDays.clear();
					BigDecimal restDays = BigDecimal.ZERO;
//LS-2241			if (dt.getForcedCall()) {
//						RestRule rRule = htg.ruleService.findRestRule(event);
//						if (rRule != null && rRule.getForcedPayMethod().isDaily()) {
//							restDays = rRule.getForcedPayMethod().getMultiplier();
//						}
//					}
					splitOnCallDay(dt, mapRuleDays, ruleKey, restDays, numJobs);
					List<BigDecimal> daySplitPerJob = mapRuleDays.values().iterator().next();
					cat = PayCategory.FLAT_RATE;
					if (dt.getWorkDayNum() >= 6) { // 6th or 7th day...
						StartForm sf = htg.weeklyTimecard.getStartForm();
						if (htg.nonUnion && (! sf.getPay6th7thDay())
								&& sf.getRateType() == EmployeeRateType.WEEKLY) {
							skipPay = true; // skip paying 6th/7th days, per StartForm
						}
						else if (dt.getWorkDayNum() == 6) {
//							cat = PayCategory.DAY6;
							mult =  ocRule.getDay6Mult();
							fraction = ocRule.getDay6Base().evaluate(BigDecimal.ONE);
						}
						else if (dt.getWorkDayNum() == 7) {
//							cat = PayCategory.DAY7;
							mult =  ocRule.getDay7Mult();
							fraction = ocRule.getDay7Base().evaluate(BigDecimal.ONE);
						}
					}
					if (! weekly) {
						fraction = BigDecimal.ONE;
					}
					if (dt.getDayType().isIdle()) {
						cat = PayCategory.IDLE_PAY;
					}
					else if (dt.getDayType() == DayType.SP) {
						cat = PayCategory.SICK_PAY; // Set category for dayType "Sick Paid". LS-3041 10/25/19
					}
					if (! skipPay) {
						if ((dt.getDayType() == DayType.HP || dt.getDayType() == DayType.HW) && holidayRule != null) {
							cat = (dt.getDayType() == DayType.HP) ? PayCategory.HOLIDAY_PAID : PayCategory.HOLIDAY_WKD;
							// adjust multiplier based on holiday onCall rule
							if (weekly) {
								fraction = holidayRule.getNotWorkedOnCallRate().evaluate(BigDecimal.ONE);
							}
							else if (holidayRule.getOnCallRate() == PayFraction.N_A) {
								fraction = BigDecimal.ZERO;
							}
						}
						for (int jobnum = 1; jobnum < daySplitPerJob.size(); jobnum++) {
							PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobnum - 1);
							BigDecimal days = daySplitPerJob.get(jobnum);
							if (days.signum() > 0) {
								daysPaid = daysPaid.add(days);
								days = days.multiply(fraction);
								hours = days.multiply(hoursPerDayOrWeek);
								BigDecimal pjRate = calculateOnCallRate(splitRate, hoursPerDayOrWeek, weekly, pj);
								if (jobnum == 1) { // if first Job table,
									simpleRate = pjRate; // save this daily rate for 'guaranteed' days. LS-3024
								}
								PayJobDaily pjd = pj.getPayJobDailies().get(dayCount);
								filterAccount = pjd.getAccountNumber(); // acct# for PB if commercial
								dailyPay = dt.getPayAmount(); // get daily pay so far
								if (dailyPay == null) { // normally shouldn't happen, but just in case!
									dailyPay = BigDecimal.ZERO;
								}
								if (dt.getDayType().isTours()) {
									dtPaid |= generateOnCallTours();
								}
								else if (includeWork) {
									addLine(pj, cat, hours, mult, pjRate, false, null);
									generateOnCallOTJob(pj, pjd, pjRate);
									dtPaid = true;
								}
								if ((dt.getDayType() == DayType.HP || dt.getDayType() == DayType.HW) && holidayRule != null) {
									addLine(pj, cat, hours, mult, pjRate, false, null);
									dtPaid = true;
								}
								if (! dt.getDayType().isTours()) { // LS-1347
									// for non-tours types, save day's accumulated pay
									dt.setPayAmount(dailyPay);
									dtPaid = true;
								}
							}
						} // end for each PayJob
					}
				}
				if (dt.getDayType() == DayType.OV) {
					PayJob pj = htg.weeklyTimecard.getPayJobs().get(0);
					generateOvernight(pj); // Generate Overnight payments. LS-2939
				}
			}
			else { // null day type. LS-1572
				dt.setPayAmount(null);
			}
			if (! dtPaid) { // Track unused days; LS-2189
				if (dt.getDayType() == DayType.HA) {
					hoaCount++;
				}
				else {
					unusedDays.add(dt);
				}
			}
			dayCount++; // sequential day number within payroll week (Sun=0)
		} // end for each DailyTime:dt

		if (htg.isSimpleWeekly && daysPaid != null &&
				(daysPaid.add(new BigDecimal(hoaCount))).compareTo(Constants.DECIMAL_FIVE) < 0) {
			// 'simple weekly', and did not get at least 5 days pay (or HOA), add more. LS-2189
			BigDecimal toPay = Constants.DECIMAL_FIVE.subtract(daysPaid).subtract(new BigDecimal(hoaCount));
			int daysToPay = toPay.intValue();
			// try to use days towards "middle of week"
			if (unusedDays.size() > daysToPay) {
				unusedDays.remove(0); // so skip 1st unused day if we don't need it;
				if (unusedDays.size() > daysToPay) {
					unusedDays.remove(unusedDays.size()-1); // and skip last unused day if not needed.
				}
			}
			PayJob pj = htg.weeklyTimecard.getPayJobs().get(0);
			BigDecimal dailyRate = pj.getWeeklyRate().divide(Constants.DECIMAL_FIVE, 2, RoundingMode.UP);
			while (toPay.signum() > 0) {
				// loop generating a line for each day (or possibly partial day for last one)
				if (toPay.compareTo(BigDecimal.ONE) >= 0) { // at least 1.0 left to fill
					fraction = BigDecimal.ONE;
					toPay = toPay.subtract(BigDecimal.ONE);
				}
				else { // less than a full day left
					fraction = toPay;
					toPay = BigDecimal.ZERO;
				}
				DailyTime dt = unusedDays.get(0);
				if (unusedDays.size() > 1) { // don't remove last, just in case!
					unusedDays.remove(0);
				}
				lineDate = dt.getDate();
				dt.setPayAmount(dailyRate);
				addLine(pj, PayCategory.FLAT_RATE, Constants.WORKED_HOURS_NON_UNION, BigDecimal.ONE, simpleRate, false, null);
			}
		}
		return daysPaid;
	}

	/**
	 * Determine the rate for the given PayJob for an Oncall (exempt) employee.
	 *
	 * @param splitRate Previously calculated flat daily rate for a weekly
	 *            person.
	 * @param hoursPerDayOrWeek divisor to be used to convert daily rate to
	 *            hourly rate
	 * @param weekly True if the current timecard is weekly-exempt.
	 * @param pj The PayJob of interest.
	 * @return The calculated rate to be used in the pay breakdown line items.
	 */
	private BigDecimal calculateOnCallRate(BigDecimal splitRate, BigDecimal hoursPerDayOrWeek, boolean weekly, PayJob pj) {
		BigDecimal pjRate = pj.getWeeklyRate();
		if (pjRate == null) {
			if (weekly) {
				pjRate = splitRate;
			}
			else {
				pjRate = pj.getDailyRate();
				if (pjRate == null) { // May be null value if split job & daily/exempt (NFL)
					pjRate = BigDecimal.ZERO;
				}
				else {
					pjRate = pjRate.divide(hoursPerDayOrWeek, 6, RoundingMode.UP);
				}
			}
		}
		else {
			if (htg.nonUnion) {
				pjRate = pjRate.divide(HtgData.STANDARD_WEEK_HOURS, 4, RoundingMode.UP);
			}
			else {
				pjRate = pjRate.divide(HtgData.STANDARD_WEEK_HOURS_UNION, 4, RoundingMode.UP);
			}
		}
		return pjRate;
	}

	/**
	 * Generate all the touring-type pay items applicable for this timecard;
	 * there may be none. LS-2189
	 *
	 * @return True if one or more touring-type days were found during
	 *         processing.
	 */
	private boolean generateOnCallTours() {
		BigDecimal daysPaid = generateOnCallTours(BigDecimal.ZERO);
		return daysPaid.signum() > 0;
	}

	/**
	 * Generate all the touring-type pay items applicable for this timecard;
	 * there may be none.
	 *
	 * @param daysPaid The current number of days paid prior to this method
	 *            being called.
	 * @return The (possibly) updated value of 'daysPaid', which will have been
	 *         incremented by the number of touring-type days found during
	 *         processing.
	 */
	private BigDecimal generateOnCallTours(BigDecimal daysPaid) {
		int tourDays = 0;
		for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
			tourDays = Math.max(tourDays, addOnCallToursLine(pj, pj.getCustomMult1()));
			tourDays = Math.max(tourDays, addOnCallToursLine(pj, pj.getCustomMult2()));
			tourDays = Math.max(tourDays, addOnCallToursLine(pj, pj.getCustomMult3()));
			tourDays = Math.max(tourDays, addOnCallToursLine(pj, pj.getCustomMult4()));
			tourDays = Math.max(tourDays, addOnCallToursLine(pj, pj.getCustomMult5()));
			tourDays = Math.max(tourDays, addOnCallToursLine(pj, pj.getCustomMult6()));
		}
		daysPaid = daysPaid.add(new BigDecimal(tourDays));
		return daysPaid;
	}

	/**
	 * If the passed multiplier indicates a touring-type column, generate the
	 * appropriate pay items for the PayJob.
	 *
	 * @param pj The PayJob being processed.
	 * @param customMult The custom multiplier for some column in the given
	 *            PayJob.
	 */
	private int addOnCallToursLine(PayJob pj, BigDecimal customMult) {
		int tourDays = 0;
		if (customMult.compareTo(PayJobService.CUSTOM_MULTIPLIER_TOURS) == 0) { // Touring rates; LS-1347
			Filter<PayJobDaily> pjdFilter = null;
			if (split) {
				pjdFilter = filterByDate;
			}
			tourDays = addToursLines(pj, pjdFilter);
		}
		return tourDays;
	}

	/**
	 * For on-call timecards, generate the 'ruleDaysPerJob' mapping from rule
	 * keys to 'days-per-PayJob' lists. Each key is either (a) an OnCallRule
	 * rule name, for TV/Features, or (b) [rule name + "!" + accountNumber], for
	 * Commercials. The account number is taken from the appropriate
	 * PayJobDaily.accountNumber field.
	 * <p>
	 * The value corresponding to the rule key is a List of BigDecimal`s, where
	 * the entry with index 'n' corresponds to the number of worked days to be
	 * assigned to PayJob number 'n'. (The 0-th entry in the List is unused.)
	 *
	 * @param ruleDaysPerJob Mapping of rules to days-per-PayJob Lists. This is
	 *            empty upon call and filled in upon return.
	 * @return Number of guaranteed WEEKS -- some value between 0.0 and 1.0. If
	 *         a negative value is returned, then an error occurred and an
	 *         appropriate message has been issued.
	 */
	private BigDecimal fillRuleDaysMap(Map<String, List<BigDecimal>> ruleDaysPerJob) {
		int numJobs = htg.weeklyTimecard.getPayJobs().size();
		OnCallRule ocRule;
		/** Track guaranteed time; normally weekly employee is guaranteed one week's salary.
		 * This value may be decreased (at least in union cases) by HOA rules, etc. */
		BigDecimal guarantee = BigDecimal.ONE; // assume guarantee of 1.0 week
		if (htg.weeklyTimecard.getStartForm().getRateType() != EmployeeRateType.WEEKLY) {
			guarantee = BigDecimal.ZERO;
		}

		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getDayType() != null) {
				htg.ruleService.prepareDailyContractRules(dt, event);
				boolean includeWork = true;
				HolidayRule holidayRule = null;
				if (dt.getDayType() == DayType.HP || dt.getDayType() == DayType.HW) {
					// For Holiday Paid or Worked, if the rule is N/A, or the
					// rule is for "additional" pay, then continue checking for "Worked" days.
					holidayRule = htg.ruleService.findHolidayRule(event);
					if (holidayRule != null && holidayRule.getOnCallRate() != PayFraction.N_A &&
							! holidayRule.getOnCallRate().isAdditional()) {
						// A non-additional and not N/A rule, it's pay overrides the Worked pay,
						// so we'll skip the next section.
						includeWork = false;
					}
				}
				if (dt.getDayType().isTours()) {
					splitOnCallDay(dt, ruleDaysPerJob, TOUR_KEY, BigDecimal.ZERO, numJobs);
					continue;
				}
				if ((dt.getWorkDayNum() <= 5) && includeWork) {
					ocRule = htg.ruleService.findOnCallRule(event);
					if (ocRule == null) {
						TimecardService.error(htg, "Timecard.HTG.NoOnCallRule");
						ruleDaysPerJob.clear();
						guarantee = Constants.DECIMAL_NEG_1K; // any negative value
						break;
					}
					BigDecimal restDays = BigDecimal.ZERO;
//LS-2241			if (dt.getForcedCall()) {
//						RestRule rRule = htg.ruleService.findRestRule(event);
//						if (rRule != null && rRule.getForcedPayMethod().isDaily()) {
//							restDays = rRule.getForcedPayMethod().getMultiplier();
//						}
//					}
					if (dt.reportedWork() || dt.getDayType().isAlwaysPaid()) {
						String rule = ocRule.getRuleKey();
						splitOnCallDay(dt, ruleDaysPerJob, rule, restDays, numJobs);
					}
					if (ocRule.getDays1to5Mult().signum() == 0) {
						// a no-pay rule (may be due to "HOA") -- decrement the guarantee of 1 full week's pay
						guarantee = guarantee.subtract(ocRule.getDays1to5Base().evaluate(BigDecimal.ONE));
					}
				}
				if (dt.getDayType() == DayType.HP) {
					holidayPaidRule = holidayRule;
					splitOnCallDay(dt, ruleDaysPerJob, HP_KEY, BigDecimal.ZERO, numJobs);
				}
				else if (dt.getDayType() == DayType.HW) {
					holidayWorkedRule = holidayRule;
					splitOnCallDay(dt, ruleDaysPerJob, HW_KEY, BigDecimal.ZERO, numJobs);
				}
			}
		}
		return guarantee;
	}

	/**
	 * Spread a single on-call day across a list of values corresponding to the
	 * PayJob`s to which the day (or fraction thereof) will be assigned.
	 *
	 * @param dt The DailyTime for the day to be split.
	 * @param ruleDaysPerJob (See also description in 'generateOnCall()'.) A
	 *            mapping of rule names (for OnCallRule rules) to a List of
	 *            BigDecimal`s, where the entry with index 'n' corresponds to
	 *            the number of days to be assigned to PayJob number 'n'. If the
	 *            appropriate List is not in the map, a new List is created with
	 *            all entries set to zero before doing the split processing.
	 * @param rule The name of the rule, which will be the key used to access
	 *            the map.
	 * @param extraDays Extra number of days to add to the split process; this
	 *            is usually zero, but may be positive due to a rest-invasion
	 *            payment, e.g., an extra half-day's pay.
	 * @param numJobs The total number of PayJob`s in the current timecard.
	 */
	private void splitOnCallDay(DailyTime dt, Map<String, List<BigDecimal>> ruleDaysPerJob,
			String rule, BigDecimal extraDays, int numJobs) {

		int jobNum = dt.getJobNum1();
		List<BigDecimal> daysPerJob = findDaysPerJobList(dt, ruleDaysPerJob, rule, numJobs, jobNum);

		BigDecimal days = daysPerJob.get(jobNum);
		days = days.add(extraDays);
		if (dt.getSplitStart2() == null) {
			daysPerJob.set(jobNum, days.add(BigDecimal.ONE));
		}
		else {
			days = days.add(dt.getSplitStart2().divide(Constants.DECIMAL_100).setScale(4,RoundingMode.HALF_UP));
			daysPerJob.set(jobNum, days);
			jobNum = dt.getJobNum2();
			daysPerJob = findDaysPerJobList(dt, ruleDaysPerJob, rule, numJobs, jobNum);
			days = daysPerJob.get(jobNum);
			if ( dt.getSplitStart3() != null) { // it's a 3-way split
				days = days.add(dt.getSplitStart3().divide(Constants.DECIMAL_100).setScale(4,RoundingMode.HALF_UP));
				daysPerJob.set(jobNum, days);
				BigDecimal left = Constants.DECIMAL_100.subtract(dt.getSplitStart2().add(dt.getSplitStart3()));
				if (left.signum() > 0) {
					jobNum = dt.getJobNum3();
					daysPerJob = findDaysPerJobList(dt, ruleDaysPerJob, rule, numJobs, jobNum);
					days = daysPerJob.get(jobNum);
					days = days.add(left.divide(Constants.DECIMAL_100).setScale(4,RoundingMode.HALF_UP));
					daysPerJob.set(jobNum, days);
				}
			}
			else {
				BigDecimal left = Constants.DECIMAL_100.subtract(dt.getSplitStart2());
				if (left.signum() > 0) {
					days = days.add(left.divide(Constants.DECIMAL_100).setScale(4,RoundingMode.HALF_UP));
					daysPerJob.set(jobNum, days);
				}
			}
		}
	}

	/**
	 * Find the "daysPerJob" List, from the ruleDaysPerJob Map, that corresponds
	 * to the given DailyTime (date), rule, and job number. If necessary, a new
	 * List will be created and added to the Map.
	 *
	 * @param dt The DailyTime for the day being processed.
	 * @param ruleDaysPerJob The existing Map in use for on-call processing.
	 * @param rule The rule name in effect -- used as the whole or partial key
	 *            in the Map.
	 * @param numJobs The number of PayJob`s for this timecard -- needed if a
	 *            new List is created.
	 * @param jobNum The job number (origin 1) being processed.
	 * @return The List<BigDecimal> of daysPerJob; this will be one of the
	 *         entries in the ruleDaysPerJob map that was supplied -- either a
	 *         previously existing entry, or a newly added entry.
	 */
	private List<BigDecimal> findDaysPerJobList(DailyTime dt,
			Map<String, List<BigDecimal>> ruleDaysPerJob, String rule, int numJobs, int jobNum) {
		PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobNum-1);
		PayJobDaily pjd = pj.getPayJobDailies().get(dt.getDayNum()-1);
		String account = pjd.getAccountNumber();
		rule = (StringUtils.isEmpty(account)) ? rule : rule + RULE_SPECIAL_CHAR + account;
		List<BigDecimal> daysPerJob = findRuleDaysPerJob(ruleDaysPerJob, numJobs, rule);
		return daysPerJob;
	}

	/**
	 * Find and/or create a List of days-per-PayJob within the given Map.
	 *
	 * @param ruleDaysPerJob (See also description in 'generateOnCall()'.) A
	 *            mapping of rule names to List of BigDecimal`s, where the entry
	 *            with index 'n' corresponds to the number of days to be
	 *            assigned to PayJob number 'n'. If the appropriate List is not
	 *            in the map, a new List is created with all entries set to zero
	 *            before doing the split processing.
	 * @param numJobs The number of PayJob entries in the current timecard.
	 * @param rule The name of the rule to be looked up in (or added to) the
	 *            map.
	 * @return The List matching the given rule key. If the list did not already
	 *         exist in the map, a new List has been created, initialized, and
	 *         added to the map.
	 */
	private List<BigDecimal> findRuleDaysPerJob(Map<String, List<BigDecimal>> ruleDaysPerJob,
			int numJobs, String rule) {
		List<BigDecimal> daysPerJob = ruleDaysPerJob.get(rule);
		if (daysPerJob == null) {
			daysPerJob = new ArrayList<>();
			for (int i = 0; i <= numJobs; i++) {
				// (zero-th entry is not used, but initialized)
				daysPerJob.add(BigDecimal.ZERO);
			}
			ruleDaysPerJob.put(rule, daysPerJob); // Add new List to Map.
		}
		return daysPerJob;
	}

	/**
	 * Generate any Pay Breakdown line items for overtime for
	 * OnCall timecards.  This is for the non-split Pay breakdown only.
	 */
	private void generateOnCallOT() {

		filterAccount = null;
		for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
			if (htg.isCommercial) {
				// Possibly split one payJob into multiple lines based on daily account numbers
				Set<String> accts = new HashSet<>();
				for (PayJobDaily pjd : pj.getPayJobDailies()) {
					if (! StringUtils.isEmpty(pjd.getAccountNumber())) {
						accts.add(pjd.getAccountNumber());
					}
					else { // use TC default Shoot account #
						pjd.setAccountNumber(null);
						accts.add(null);
					}
				}
				for (String acct : accts) {
					filterAccount = acct;
					pj.calculateTotals(filterByAccount);
					generateOnCallOTJob(pj);
				}
				pj.calculateTotals(); // restore full totals
			}
			else { // TV/Feature - no need to split PayJob by accountNumber
				generateOnCallOTJob(pj);
			}
		} // End of PayJob loop

	}

	/**
	 * Generate the overtime labor (hours) entries for the specified PayJob for an
	 * OnCall employee, if any.  This is for the non-split Pay breakdown only.
	 *
	 * @param pj The PayJob entry for which the breakdown line items are being
	 *            generated.
	 */
	private void generateOnCallOTJob(PayJob pj) {

		// Generate OT labor (hours) entries for the current PayJob for an OnCall employee
		// Any custom multiplier column could have daily or weekly OT values...
		addOnCallOTLine(pj, pj.getTotalCust1(), pj.getCustomMult1(), pj.getCustom1Type());
		addOnCallOTLine(pj, pj.getTotalCust2(), pj.getCustomMult2(), pj.getCustom2Type());
		addOnCallOTLine(pj, pj.getTotalCust3(), pj.getCustomMult3(), pj.getCustom3Type());
		addOnCallOTLine(pj, pj.getTotalCust4(), pj.getCustomMult4(), pj.getCustom4Type());
		addOnCallOTLine(pj, pj.getTotalCust5(), pj.getCustomMult5(), pj.getCustom5Type());
		addOnCallOTLine(pj, pj.getTotalCust6(), pj.getCustomMult6(), pj.getCustom6Type());

	}

	/**
	 * Generate an overtime labor (hours) entry for the specified PayJob for an
	 * OnCall employee, if either the daily or weekly parameter is true, and the
	 * total is non-zero.
	 *
	 * @param pj The PayJob to be used for rate information.
	 * @param total The number of hours to be put in the quantity field.
	 * @param mult The multiplier field value.
	 * @param payRateType True if the employee's Daily pay rate should be used. LS-2241
	 * @param weekly True if the employee's Weekly pay rate should be used.
	 */
	private void addOnCallOTLine(PayJob pj, BigDecimal total, BigDecimal mult,
			PayRateType payRateType) {

		if (mult.compareTo(PayJobService.CUSTOM_MULTIPLIER_TOURS) == 0) {
			// Called for touring rate column -- ignore; LS-1347
		}
		else if (total.signum() != 0) { // don't bother if quantity is zero
			mult = mult.abs();	// daily & weekly OT multipliers may be stored as negative values
			PayCategory pc = null;
			BigDecimal rate = null;

			if (payRateType == PayRateType.D) {
				pc = PayCategory.DAILY_OT;
				rate = pj.getDailyRate();
				if (rate == null) {
					rate = htg.getWeeklyTimecard().getDailyRate();
				}
			}
			else if (payRateType == PayRateType.W) {
				pc = PayCategory.WEEKLY_OT;
				rate = pj.getWeeklyRate();
				if (rate == null) {
					rate = htg.getWeeklyTimecard().getWeeklyRate();
				}
			}
			else if (payRateType == PayRateType.F) { // LS-2241
				pc = PayCategory.REST_INV; // Kluge! Only fixed-rate usage for now is Rest Invasion.
				addLine(pj, pc, total, BigDecimal.ONE, mult, false, pc.getLabel());
				rate = null; // suppress addLine below.
			}
			else { // Hourly OT (probably Gold) - unusual; e.g., 700 Editors.
				rate = pj.getRate();
				if (rate == null) {
					rate = htg.getWeeklyTimecard().getHourlyRate();
				}
				if (rate != null) {
					addHoursLine(pj, total, mult, PayRateType.H, null, null);
				}
				rate = null;
			}

			if (rate != null) {
				addLine(pj, pc, total, mult, rate, true, null);
			}
		}

	}

	/**
	 * Generate the overtime labor (hours) entries for the specified PayJob and
	 * a single day (PayJobDaily) for an OnCall employee, if any. This method is
	 * only used for the "daily split" breakdown.
	 *
	 * @param pj The PayJob entry for which the breakdown line items are being
	 *            generated.
	 * @param pjd The PayJobDaily entry for which the breakdown line items are
	 *            being generated.
	 * @param pjRate The hourly rate to use.
	 */
	private void generateOnCallOTJob(PayJob pj, PayJobDaily pjd, BigDecimal pjRate) {
		// Generate OT labor (hours) entries for the current PayJob for an OnCall employee
		// Any custom multiplier column could have daily or weekly OT values...
		addOnCallOTLine(pj, pjd.getHoursCust1(), pj.getCustomMult1(), pj.getCustom1Type(), pjRate);
		addOnCallOTLine(pj, pjd.getHoursCust2(), pj.getCustomMult2(), pj.getCustom2Type(), pjRate);
		addOnCallOTLine(pj, pjd.getHoursCust3(), pj.getCustomMult3(), pj.getCustom3Type(), pjRate);
		addOnCallOTLine(pj, pjd.getHoursCust4(), pj.getCustomMult4(), pj.getCustom4Type(), pjRate);
		addOnCallOTLine(pj, pjd.getHoursCust5(), pj.getCustomMult5(), pj.getCustom5Type(), pjRate);
		addOnCallOTLine(pj, pjd.getHoursCust6(), pj.getCustomMult6(), pj.getCustom6Type(), pjRate);
	}

	/**
	 * Generate an overtime labor (hours) entry for the specified PayJob for an
	 * OnCall employee, if either the daily or weekly parameter is true, and the
	 * total is non-zero. This method is only used for the "daily split" (Team)
	 * breakdown.  Since this only applies to union occupations, a fixed value
	 * of 12 hours is used in the generated breakdown items.
	 *
	 * @param pj The PayJob to be used for rate information.
	 * @param total The number of hours or fraction of a day or week.
	 * @param mult The multiplier field value.
	 * @param payRateType Rate type - hourly, daily, weekly, etc. LS-2241
	 * @param rate The hourly rate to be put in the line item.
	 */
	private void addOnCallOTLine(PayJob pj, BigDecimal total, BigDecimal mult,
			PayRateType payRateType, BigDecimal rate) {

		if (total != null && total.signum() != 0) { // don't bother if quantity is zero
			if (payRateType != PayRateType.F) {
				mult = mult.abs();	// daily & weekly OT multipliers may be stored as negative values
				mult = mult.multiply(total);
			}
			PayCategory pc = null;
			if (payRateType == PayRateType.D) {
				pc = PayCategory.DAILY_OT;
				addLine(pj, pc, Constants.DECIMAL_12, mult, rate, true, null);
			}
			else if (payRateType == PayRateType.W) {
				pc = PayCategory.WEEKLY_OT;
				addLine(pj, pc, Constants.DECIMAL_12, mult, rate, true, null);
			}
			else if (payRateType == PayRateType.F) { // LS-2241
				// Fixed payment type: 'mult' is pay rate (amount); 'total' is Qty (e.g., hours)
				pc = PayCategory.REST_INV; // TODO Kluge! Assume Rest Invasion for now - only item using "F" at this time.
				addLine(pj, pc, total, BigDecimal.ONE, mult, false, pc.getAbbreviation());
			}
			else { // Hourly OT (probably Gold) - unusual; e.g., 700 Editors
				if (rate != null) {
					addHoursLine(pj, mult, BigDecimal.ONE, PayRateType.H, null, null);
				}
			}
		}
	}

	/**
	 * Accumulate the total of worked-holiday hours and other (paid or unpaid)
	 * holiday hours, idle hours, and travel hours, from the given PayJob, by
	 * rate. The "normal" totals are reduced by the number of special hours.
	 *
	 * @param pj The PayJob whose totals are to be computed.
	 * @param pjNumber The index of the given PayJob within the timecard's
	 *            job-table list. Origin zero.
	 */
	private void calculateSpecialHours(PayJob pj, int pjNumber) {
		int dayCount = 0; // which day of the week we're doing (0...6)
		// Accumulate the daily totals of special hours within the given payJob
		for (PayJobDaily pjd : pj.getPayJobDailies()) {
			pjd.setIsSpecial(false); // clear flag - assume not special hours
			DailyTime dt = htg.weeklyTimecard.getDailyTimes().get(dayCount);
			if (dt.getDayType() != null) {
				PayCategory pc = dt.getDayType().getCategory();
				if (pc == PayCategory.TRAVEL && htg.isTeam && htg.nonUnion) {
					pc = null; // LS-1785 for Team, show non-union Travel as work.
				}
				if (pc != null) {
					if (pc == PayCategory.TRAVEL &&
							(dt.getDayType() == DayType.TW || dt.getDayType() == DayType.WT)) {
						BigDecimal travHrs = TimecardCalc.calculateTravelHours(dt);
						// Assume the travel is always paid as straight time!
						if (pjd.getHours10() == null) {
							travHrs = BigDecimal.ZERO;
						}
						else {
							travHrs = travHrs.min(pjd.getHours10()); // limit it to 1.0 hours
						}
						PayJobHours pjHours = findSpecialPayJobHours(pc)[pjNumber];
						// add it to the special travel hours
						pjHours.setHours10(NumberUtils.safeAdd(travHrs, pjHours.getHours10()));
					}
					else {
						addHours(pjd, findSpecialPayJobHours(pc)[pjNumber]);
					}
				}
				else if (pjd.getHours10() == null && dt.getWorkDayNum() >= 6) { // LS-781
					pc = hasDailyOTrule(dt);
					if (pc != null) {
						// find or create the accumulator for this category
						PayJobHours pjHours = findSpecialPayJobHours(pc)[pjNumber];
						// add this day's hours to the accumulator for the category
						pjHours.add(pjd);
						pjd.setIsSpecial(true); // so filterByAccount will skip this
					}
				}
			}
			dayCount++;
		}

		// Now reduce the standard totals by the amount of
		// the special hour totals from this PayJob
		for (PayCategory pc : specialHoursMap.keySet()) {
			subtractHours(pj, specialHoursMap.get(pc)[pjNumber]);
		}
	}

	/**
	 * Determine if OT may be paid due to a rule that is specific to the
	 * work-day number in the given DailyTime, when the work-day-number
	 * is either 6 or 7 -- i.e., either 6th or 7th day worked. LS-781
	 *
	 * @param dt The DailyTime of interest.
	 * @return True iff an OT rule applied specifically to the given day
	 *         for a 6th- or 7th-day.
	 */
	private PayCategory hasDailyOTrule(DailyTime dt) { // LS-781
		PayCategory pc = null;
		if (dt.getWorkDayNum() == 6 || dt.getWorkDayNum() == 7) {
			htg.ruleService.prepareDailyContractRules(dt, event);
			boolean hasRule = isEffectiveRule(dt, htg.ruleService.findLastDailyContractRuleByType(RuleType.OT, event));
			if (! hasRule) {
				hasRule = isEffectiveRule(dt, htg.ruleService.findLastDailyContractRuleByType(RuleType.GL, event));
			}
			if (hasRule) {
				if (dt.getWorkDayNum() == 6) {
					pc = PayCategory.DAY6;
				}
				else {
					pc = PayCategory.DAY7;
				}
			}
		}
		return pc;
	}

	/**
	 * Determine if the given rule will generate a payment.
	 * @param dt
	 *
	 * @param rule The rule to be tested or null.
	 * @return True if the rule exists (is not null) and if the rule name does
	 *         NOT contain the text "-NONE". In this fashion, we ignore rules
	 *         that "turn off" payment, such as "OT-NONE" or "GL-NONE".
	 */
	private boolean isEffectiveRule(DailyTime dt, ContractRule rule) {
		boolean hasRule = false;
		if (rule != null) {
			if ((! rule.getUseRuleKey().contains("-NONE")) &&
					rule.matchesDayNumber(dt.getWorkDayNum())) {
				hasRule = true;
			}
		}
		return hasRule;
	}

	/**
	 * Take all the hours in the given PayJobDaily (one day's hours within a
	 * PayJob), and add them to the corresponding columns of the given
	 * PayJobHours instance. This is typically done for Holiday-Worked and Idle
	 * time.
	 *
	 * @param pjd The source PayJobDaily, typically for an existing day in one
	 *            of the timecard's PayJob tables.
	 * @param pjHours The PayJobHours instance to which the hours will be added;
	 *            we maintain separate PayJobHours instances for each category
	 *            of time that needs to be itemized separately in the Pay
	 *            Breakdown table.
	 */
	private void addHours(PayJobDaily pjd, PayJobHours pjHours) {
		pjHours.setHours10(NumberUtils.safeAdd(pjd.getHours10(), pjHours.getHours10()));
		pjHours.setHours15(NumberUtils.safeAdd(pjd.getHours15(), pjHours.getHours15()));
		pjHours.setHoursCust1(NumberUtils.safeAdd(pjd.getHoursCust1(), pjHours.getHoursCust1()));
		pjHours.setHoursCust2(NumberUtils.safeAdd(pjd.getHoursCust2(), pjHours.getHoursCust2()));
		pjHours.setHoursCust3(NumberUtils.safeAdd(pjd.getHoursCust3(), pjHours.getHoursCust3()));
		pjHours.setHoursCust4(NumberUtils.safeAdd(pjd.getHoursCust4(), pjHours.getHoursCust4()));
		pjHours.setHoursCust5(NumberUtils.safeAdd(pjd.getHoursCust5(), pjHours.getHoursCust5()));
		pjHours.setHoursCust6(NumberUtils.safeAdd(pjd.getHoursCust6(), pjHours.getHoursCust6()));
	}

	/**
	 * Take the hours in the given PayJobHours instance, and subtract them from
	 * the given PayJob's totals (column by column). The purpose is to remove
	 * any 'special' hours (which will be listed separately in the Pay Breakdown
	 * table) from the totals in the 'real' PayJob, so that we can generate the
	 * non-special line items in the Pay Breakdown.
	 *
	 * @param pj The PayJob whose column totals will be reduced by the column
	 *            totals of the PayJobHours.
	 * @param pjHours The PayJobHours whose special hours are to be subtracted
	 *            from the PayJob.
	 */
	private void subtractHours(PayJob pj, PayJobHours pjHours) {
		pj.setTotal10(pj.getTotal10().subtract(pjHours.getHours10()));
		pj.setTotal15(pj.getTotal15().subtract(pjHours.getHours15()));
		pj.setTotalCust1(pj.getTotalCust1().subtract(pjHours.getHoursCust1()));
		pj.setTotalCust2(pj.getTotalCust2().subtract(pjHours.getHoursCust2()));
		pj.setTotalCust3(pj.getTotalCust3().subtract(pjHours.getHoursCust3()));
		pj.setTotalCust4(pj.getTotalCust4().subtract(pjHours.getHoursCust4()));
		pj.setTotalCust5(pj.getTotalCust5().subtract(pjHours.getHoursCust5()));
		pj.setTotalCust6(pj.getTotalCust6().subtract(pjHours.getHoursCust6()));

	}

	/**
	 * Accumulate the totals for hours related to a worked (non-holiday) day
	 * extending into a holiday. We call this a "holiday overlap". Those hours
	 * are added to the corresponding fields in {@link #specialHoursMap}, and
	 * subtracted from the "normal" (non-holiday) hour totals in the PayJob.
	 */
	private void calculateHolidayOverlap() {
		int dayCount = 0;
		BigDecimal hours = null;
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if ( dayCount > 0 &&
					(dt.getDayType() == DayType.HP || dt.getDayType() == DayType.HW)) {
				// have a Holiday - see if prior day overlapped
				DailyTime priorDt = htg.weeklyTimecard.getDailyTimes().get(dayCount-1);
				if (priorDt.getHours() != null && priorDt.getHours().signum() > 0) {
					if (priorDt.getWrap() != null &&
							priorDt.getWrap().compareTo(Constants.HOURS_IN_A_DAY) > 0) {
						// prior day was worked, and wrapped after midnight.
						// Determine number of hours worked after midnight (into holiday)
						hours = TimecardCalc.calculateWorkedHours(priorDt, Constants.HOURS_IN_A_DAY, priorDt.getWrap(), htg);
						htg.ruleService.prepareDailyContractRules(priorDt, event);
						GoldenRule goldRule = htg.ruleService.findGoldenRule(event);
						// First see if any hours in the payJobs are at second gold rate, and move them.
						BigDecimal multiplier = goldRule.getMultiplier2();
						if (multiplier != null) {
							hours = adjustHoliday(hours, multiplier, dayCount);
						}
						if (hours.signum() > 0) {
							// still hours left to move, they should be at normal gold rate
							multiplier = goldRule.getMultiplier();
							hours = adjustHoliday(hours, multiplier, dayCount);
						}
					}
				}
			}
			dayCount++;
		}
	}

	/**
	 * Move a given number of hours, which are currently in the PayJob(s) at
	 * 'multiplier' rates, into the equivalent holiday field -- based on the
	 * value of 'multiplier'. The same number of hours will be subtracted from
	 * the corresponding normal hour totals in the PayJob(s). Note that we can't
	 * move any more hours than already exist in the PayJob(s) at the
	 * appropriate multiplier value.
	 * <p>
	 * This is only used in the "holiday overlap" situation.
	 *
	 * @param hours The number of hours to shift from normal time to holiday
	 *            time.
	 * @param multiplier The multiplier at which to look for these hours (e.g.,
	 *            2x or 4x).
	 * @param dayCount The day number (origin zero) of the day whose hours are
	 *            to be moved.
	 * @return The number of hours remaining to be moved from normal time into
	 *         holiday time; that is, the 'hours' value passed as a parameter,
	 *         minus the hours moved.
	 */
	private BigDecimal adjustHoliday(BigDecimal hours, BigDecimal multiplier, int dayCount) {
		int pjNumber = 0;
		for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
			PayJobDaily pjd = pj.getPayJobDailies().get(dayCount-1);
			BigDecimal moved = adjustHolidayPayjob(pj, pjNumber, pjd, multiplier, hours);
			hours = hours.subtract(moved);
			if (hours.signum() <= 0) {
				break;
			}
			pjNumber++;
		}
		return hours;
	}


	/**
	 * Move a given number of hours, which may currently be in the given PayJob
	 * at 'multiplier' rates, into the equivalent holiday field -- based on the
	 * value of 'multiplier'. The same number of hours will be subtracted from
	 * the corresponding normal hour totals in that PayJob. Note that we won't
	 * move any more hours than already exist in the PayJob at the appropriate
	 * multiplier value.
	 * <p>
	 * This is only used in the "holiday overlap" situation.
	 *
	 * @param pj The PayJob to examine, and whose total hours (normal and
	 *            holiday) may be updated.
	 * @param pjNumber The index of the given PayJob within the timecard's
	 *            job-table list. Origin zero.
	 * @param pjd The PayJobDaily to examine for available hours.
	 * @param multiplier The multiplier at which to look for these hours (e.g.,
	 *            2x or 4x).
	 * @param hours The number of hours to shift from normal time to holiday
	 *            time.
	 * @return The number of hours successfully moved from normal time into
	 *         holiday time.
	 */
	private BigDecimal adjustHolidayPayjob(PayJob pj, int pjNumber, PayJobDaily pjd, BigDecimal multiplier, BigDecimal hours) {
		BigDecimal moved = BigDecimal.ZERO;
		PayJobHours worked = findSpecialPayJobHours(PayCategory.HOLIDAY_WKD)[pjNumber];
		if (multiplier.compareTo(BigDecimal.ONE) == 0) { // check 1.0x column
			if (pjd.getHours10() != null) {
				moved = hours.min(pjd.getHours10());
				worked.setHours10(NumberUtils.safeAdd(moved, worked.getHours10()));
			}
		}
		else if (multiplier.compareTo(Constants.DECIMAL_ONE_FIVE) == 0) { // check 1.5x column
			if (pjd.getHours15() != null) {
				moved = hours.min(pjd.getHours15());
				worked.setHours15(NumberUtils.safeAdd(moved, worked.getHours15()));
			}
		}
		else if (multiplier.compareTo(pj.getCustomMult1()) == 0) { // check each "custom" column
			if (pjd.getHoursCust1() != null) {
				moved = hours.min(pjd.getHoursCust1());
				worked.setHoursCust1(NumberUtils.safeAdd(moved, worked.getHoursCust1()));
			}
		}
		else if (multiplier.compareTo(pj.getCustomMult2()) == 0) {
			if (pjd.getHoursCust2() != null) {
				moved = hours.min(pjd.getHoursCust2());
				worked.setHoursCust2(NumberUtils.safeAdd(moved, worked.getHoursCust2()));
			}
		}
		else if (multiplier.compareTo(pj.getCustomMult3()) == 0) {
			if (pjd.getHoursCust3() != null) {
				moved = hours.min(pjd.getHoursCust3());
				worked.setHoursCust3(NumberUtils.safeAdd(moved, worked.getHoursCust3()));
			}
		}
		else if (multiplier.compareTo(pj.getCustomMult4()) == 0) {
			if (pjd.getHoursCust4() != null) {
				moved = hours.min(pjd.getHoursCust4());
				worked.setHoursCust4(NumberUtils.safeAdd(moved, worked.getHoursCust4()));
			}
		}
		else if (multiplier.compareTo(pj.getCustomMult5()) == 0) {
			if (pjd.getHoursCust5() != null) {
				moved = hours.min(pjd.getHoursCust5());
				worked.setHoursCust5(NumberUtils.safeAdd(moved, worked.getHoursCust5()));
			}
		}
		else if (multiplier.compareTo(pj.getCustomMult6()) == 0) {
			if (pjd.getHoursCust6() != null) {
				moved = hours.min(pjd.getHoursCust6());
				worked.setHoursCust6(NumberUtils.safeAdd(moved, worked.getHoursCust6()));
			}
		}
		return moved;
	}

	/**
	 * Conditionally add a Pay Breakdown line item for the total of all Meal
	 * Period Violations. This looks up each of the MPV penalty values and
	 * accumulates them, then adds the breakdown line item, if the total is
	 * greater than zero.
	 *
	 * @param pj The PayJob whose MPV entries will be evaluated.
	 * @param pjdFilter The filter in effect for selecting PayJobDaily's, or
	 *            null if no filtering is in effect.
	 */
	private void calculateAndAddMpvLine(PayJob pj, Filter<PayJobDaily> pjdFilter) {
		try {
			// Do quick test to bypass rest of processing if no MPVs in this PayJob
			Byte totalMpvs = NumberUtils.safeAdd(pj.getTotalMpv1(), pj.getTotalMpv2(), pj.getTotalMpv3());
			if (totalMpvs == null || totalMpvs == 0) {
				return;
			}
			int i = 0;
			BigDecimal penalty = BigDecimal.ZERO;
			List<MpvRule> mpvRules = null;
			String lastRuleKey = "";
//			boolean addDetail = true;
			for (PayJobDaily pjd : pj.getPayJobDailies()) {
				if ((pjd.getMpv1() != null || pjd.getMpv2() != null || pjd.getMpv3() != null) &&
						(pjdFilter == null || pjdFilter.filter(pjd))) {
					DailyTime dt = htg.weeklyTimecard.getDailyTimes().get(i);
					List<ContractRule> dayRules = htg.ruleService.findDailyContractRules(dt, RuleType.MP, event);
					if (dayRules.size() > 0) {
						ContractRule dayRule = dayRules.get(dayRules.size()-1); // use last one
						if (! dayRule.getUseRuleKey().equals(lastRuleKey)) {
							lastRuleKey = dayRule.getUseRuleKey();
							mpvRules = MpvRuleDAO.getInstance().findByRuleKey(lastRuleKey);
//							addDetail = true;
						}
//						else {
//							addDetail = false;
//						}
						if (mpvRules != null && mpvRules.size() > 0) {
							MpvRule rule = mpvRules.get(0);
							event.appendSummary("MPV: " + dt.getWeekDayName() + ": rule=" + rule.getRuleKey());
//							if (addDetail) {
//								event.appendDetail(rule.toAudit()); // skip detail since only for internal use
//							}
							BigDecimal prevailingRate = null;
							BigDecimal straightRate = null;
							if (pjd.getMpv1() != null && pjd.getMpv1() > 0) { // don't bother for zero entries
								// possibly issue warning regarding prevailing rate if split jobs?
								straightRate = pj.getRate();
								prevailingRate = checkPrevailing(rule, pj, pjd, dt, 1);
							}
							penalty = penalty.add(calculateMpvPenalty(rule, pjd.getMpv1(),
									straightRate, prevailingRate, event));
							if (mpvRules.size() > 1) {
								rule = mpvRules.get(1);
							}
							if (pjd.getMpv2() != null && pjd.getMpv2() > 0) { // don't bother for zero entries
								straightRate = pj.getRate();
								prevailingRate = checkPrevailing(rule, pj, pjd, dt, 2);
							}
							penalty = penalty.add(calculateMpvPenalty(rule, pjd.getMpv2(),
									straightRate, prevailingRate, event));
							if (mpvRules.size() > 2) {
								rule = mpvRules.get(2);
							}
							if (pjd.getMpv3() != null && pjd.getMpv3() > 0) { // don't bother for zero entries
								straightRate = pj.getRate();
								prevailingRate = checkPrevailing(rule, pj, pjd, dt, 3);
							}
							penalty = penalty.add(calculateMpvPenalty(rule, pjd.getMpv3(),
									straightRate, prevailingRate, event));
						}
					}
				}
				i++;
			}
			if (penalty.signum() > 0) {
				if (htg.weeklyTimecard.getEmployeeRateType() == EmployeeRateType.TABLE) {
					addLine(pj, PayCategory.MEAL_PENALTY, new BigDecimal(totalMpvs),
							BigDecimal.ONE, penalty, false, null);
				}
				else {
					addLine(pj, PayCategory.MEAL_PENALTY, BigDecimal.ONE,
							BigDecimal.ONE, penalty, false, null);
				}

			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			TimecardService.errorTrace(htg, "Timecard.HTG.Error.Mpv");
		}
	}

	/**
	 * Calculate the dollar meal-violation penalty for one meal time.
	 *
	 * @param rule The MPV rule used to calculate the penalty.
	 * @param mpvs The number of violations (typically 1/2-hour increments) for
	 *            which the penalty is to be computed.
	 * @param straightRate The employee's "straight rate"; some rules use this
	 *            value (or a multiple of it) as a penalty increment.
	 * @param prevailingRate The employee's current hourly prevailing wage; some
	 *            rules use this value as a penalty increment.
	 * @param event The audit trail event for logging.
	 * @return The dollar amount of the penalty for the given violation.
	 */
	private static BigDecimal calculateMpvPenalty(MpvRule rule, Byte mpvs,
			BigDecimal straightRate, BigDecimal prevailingRate, AuditEvent pEvent) {
		if (mpvs == null || mpvs == 0) {
			return BigDecimal.ZERO;
		}
		// MPV rates of zero indicate to use the "prevailing rate" - the employee's rate at time of violation
		BigDecimal penalty = rule.getMpv1Rate();
		if (penalty.signum() == 0) {
			// Zero penalty means use prevailing rate.
			penalty = prevailingRate;
		}
		else if (penalty.signum() < 0) {
			// Negative penalty represents multiplier of straight-rate.
			if (straightRate != null) {
				penalty = penalty.multiply(straightRate).negate();
			}
			else { // Rate not specified - return zero
				penalty = BigDecimal.ZERO;
			}
		}
		String msg = "{" + mpvs + " mpvs=$";
		mpvs--;
		if (mpvs > 0) {
			if (straightRate==null) { // can happen if PayJob has no rate filled in.
				straightRate = BigDecimal.ZERO;
			}
			if (rule.getMpv2Rate().signum() == 0) {
				penalty = penalty.add(prevailingRate);
			}
			else if (rule.getMpv2Rate().signum() < 0) {
				penalty = penalty.add(rule.getMpv2Rate().multiply(straightRate).negate());
			}
			else {
				penalty = penalty.add(rule.getMpv2Rate());
			}
			mpvs--;
			if (mpvs > 0) {
				if (rule.getMpv3Rate().signum() == 0) {
					penalty = penalty.add(prevailingRate);
				}
				else if (rule.getMpv3Rate().signum() < 0) {
					penalty = penalty.add(rule.getMpv3Rate().multiply(straightRate).negate());
				}
				else {
					penalty = penalty.add(rule.getMpv3Rate());
				}
				mpvs--;
				if (mpvs > 0) {
					if (rule.getMpv4Rate().signum() == 0) {
						penalty = penalty.add(prevailingRate);
					}
					else if (rule.getMpv4Rate().signum() < 0) {
						penalty = penalty.add(rule.getMpv4Rate().multiply(straightRate).negate());
					}
					else {
						penalty = penalty.add(rule.getMpv4Rate());
					}
					mpvs--;
					BigDecimal endingRate = rule.getMpvOtherRate();
					if (endingRate.signum() == 0) {
						endingRate = prevailingRate;
					}
					else if (endingRate.signum() < 0) {
						endingRate = endingRate.multiply(straightRate).negate();
					}
					while (mpvs > 0) {
						// for each additional period, add either prevailing or fixed rate:
						penalty = penalty.add(endingRate);
						// "final" rate may be incremented continually:
						endingRate = endingRate.add(rule.getRateIncrement());
						mpvs--;
					}
				}
			}
		}
		pEvent.appendSummary(msg + penalty + "}");
		return penalty;
	}

	/**
	 * Determine if a "prevailing rate" value is required for the given MPV rule,
	 * and, if so, calculate what that rate is.
	 *
	 * @param rule The MpvRule being applied to the timecard.
	 * @param pj The PayJob entry to be used as the source of the pay rate(s).
	 * @param pjd The PayJobDaily entry for the day in question.
	 * @param dt The DailyTime entry for the day in question.
	 * @param mealNum Which meal number (1 or 2) is being evaluated.
	 * @return The prevailing rate at the time of the given meal number, or null
	 *         if no prevailing rate is required by the given MPV rule.
	 */
	private BigDecimal checkPrevailing(MpvRule rule, PayJob pj, PayJobDaily pjd, DailyTime dt, int mealNum) {
		BigDecimal rate = pj.getRate();
		if (rule.getMpv1Rate().signum() == 0 || rule.getMpv2Rate().signum() == 0 ||
				rule.getMpv3Rate().signum() == 0 || rule.getMpv4Rate().signum() == 0 ||
				rule.getMpvOtherRate().signum() == 0) {
			// At least one rate is "prevailing rate"
			BigDecimal elapsedHours = TimecardCalc.calculateAmHours(dt);
			if (mealNum != 1) {
				elapsedHours = elapsedHours.add(TimecardCalc.calculatePmHours(dt,
						rule.getMaxMealLength(), rule.getNdmAllowed()));
				if (mealNum == 3) {
					elapsedHours = elapsedHours.add(TimecardCalc.calculateWrapHours(dt,
							rule.getMaxMealLength(), rule.getNdmAllowed()));
				}
			}
			if (elapsedHours.signum() > 0 && pjd.getHours10() != null) {
				elapsedHours = elapsedHours.subtract(pjd.getHours10());
			}
			if (elapsedHours.signum() > 0 && pjd.getHours15() != null) {
				rate = rate.multiply(Constants.DECIMAL_ONE_FIVE);
				elapsedHours = elapsedHours.subtract(pjd.getHours15());
			}
			if (elapsedHours.signum() > 0 && pjd.getHoursCust1() != null) {
				rate = pj.getRate().multiply(pj.getCustomMult1());
				elapsedHours = elapsedHours.subtract(pjd.getHoursCust1());
			}
			if (elapsedHours.signum() > 0 && pjd.getHoursCust2() != null) {
				rate = pj.getRate().multiply(pj.getCustomMult2());
				elapsedHours = elapsedHours.subtract(pjd.getHoursCust2());
			}
			if (elapsedHours.signum() > 0 && pjd.getHoursCust3() != null) {
				rate = pj.getRate().multiply(pj.getCustomMult3());
				elapsedHours = elapsedHours.subtract(pjd.getHoursCust3());
			}
			if (elapsedHours.signum() > 0 && pjd.getHoursCust4() != null) {
				rate = pj.getRate().multiply(pj.getCustomMult4());
				elapsedHours = elapsedHours.subtract(pjd.getHoursCust4());
			}
			if (elapsedHours.signum() > 0 && pjd.getHoursCust5() != null) {
				rate = pj.getRate().multiply(pj.getCustomMult5());
				elapsedHours = elapsedHours.subtract(pjd.getHoursCust5());
			}
			if (elapsedHours.signum() > 0 && pjd.getHoursCust6() != null) {
				rate = pj.getRate().multiply(pj.getCustomMult6());
				elapsedHours = elapsedHours.subtract(pjd.getHoursCust6());
			}
		}
		return rate;
	}

	/**
	 * Calculate the "extended rate", which is the hourly rate to be
	 * used for "extended day" calculations -- OT for some unions (such
	 * as DGA) where the normal pay is a Daily rate, even though the
	 * employees are not Exempt.
	 */
	private void calculateExtendedRate() {
		if (htg.weeklyTimecard.getHourlyRate() != null) {
			htg.extendedRate = htg.weeklyTimecard.getHourlyRate();
			return;
		}
		OvertimeRule otRule = htg.ruleService.findOvertimeRule(event);
		BigDecimal hours;
		BigDecimal mult;
		if (otRule != null && otRule.getOvertimeDailyBreak().compareTo(Constants.HOURS_IN_A_DAY) < 0) {
			hours = otRule.getOvertimeDailyBreak();
			mult = otRule.getOvertimeRate();
		}
		else {
			// for "OT-NONE" or similar, or no rule, just use defaults
			 hours = Constants.DECIMAL_EIGHT;
			 mult = Constants.DECIMAL_ONE_FIVE;
		}
		hours = htg.defaultGuarHours.subtract(hours).multiply(mult).add(hours);
		// Example: ((12 - 8) * 1.5) + 8 = 14 "pay hours" in a day
		BigDecimal rate = htg.weeklyTimecard.getDailyRate();
		if (rate == null) {
			rate = htg.weeklyTimecard.getWeeklyRate();
			hours = hours.multiply(Constants.DECIMAL_FIVE);
		}
		if (rate != null) {
			rate = rate.divide(hours, 4, RoundingMode.HALF_UP);
			htg.extendedRate = rate;
		}
	}

	/**
	 * Calculate the weekly guaranteed amount for a cumulative schedule.
	 * @param rate
	 * @param weeklyRule
	 *
	 * @return the weekly guaranteed amount for the current timecard.
	 */
	private BigDecimal calculateWeeklyGuarantee(WeeklyRule weeklyRule, BigDecimal rate) {
		if (rate == null) {
			return null;
		}
		BigDecimal guar = rate.multiply(weeklyRule.getOvertimeWeeklyBreak()); // straight time pay
		BigDecimal ot = weeklyRule.getCumeHours().subtract(weeklyRule.getOvertimeWeeklyBreak()); // OT hours
		ot = ot.multiply(weeklyRule.getOvertimeRate()).multiply(rate); // OT pay
		guar = guar.add(ot); // Straight + OT pay
		return guar;
	}

	/**
	 * Calculate the FLSA adjustment, if any, and add it to the Pay Breakdown as
	 * a new line item. Any labor entry with a rate equal to or larger than
	 * FLSA_IGNORE_RATE will not be included in the FLSA calculations. Typically
	 * "gold" time (2x) or better is ignored for FLSA.
	 */
	@SuppressWarnings("rawtypes")
	private void generateFlsa() {
		BigDecimal totalAdds = BigDecimal.ZERO;
		BigDecimal totalHours = BigDecimal.ZERO;
		List payList;
		if (split) {
			payList = htg.weeklyTimecard.getPayDailyLines();
		}
		else {
			payList = htg.weeklyTimecard.getPayLines();
		}
		for (Iterator iter = payList.iterator(); iter.hasNext(); ) {
			PayBreakdownMapped pb = (PayBreakdownMapped)iter.next();
			String cat = pb.getCategory();
			PayCategory pCat = PayCategory.toValue(cat);
			if (pCat != null) {
				BigDecimal total = null;
				boolean hours = pCat.name().indexOf("HOURS") >= 0;
				if (hours) {
					if (pb.getMultiplier().compareTo(FLSA_IGNORE_RATE) < 0) {
						total = NumberUtils.safeMultiply(pb.getQuantity(), BigDecimal.ONE, pb.getRate());
						totalHours = totalHours.add(pb.getQuantity());
					}
				}
				else if (pCat.getIsFlsa()) {
					total = NumberUtils.safeMultiply(pb.getQuantity(), pb.getMultiplier(), pb.getRate());
					if (total != null) {
						totalAdds = totalAdds.add(NumberUtils.scaleTo(total, 0, 4));
					}
				}
			}
		}
		LOG.debug(totalAdds + ", hrs: " + totalHours);
		if (totalAdds.signum() > 0 && totalHours.signum() > 0) {
			BigDecimal otHours = totalHours.subtract(FLSA_STRAIGHT_HOURS);
			if (otHours.signum() > 0) {
				BigDecimal addRate = totalAdds.divide(totalHours, 6, RoundingMode.UP);
				addRate = addRate.divide(Constants.DECIMAL_TWO, 6, RoundingMode.UP);
				BigDecimal extra = addRate.multiply(otHours);
				extra = NumberUtils.scaleTo(extra, 0, 2);
				if (extra.signum() > 0) {
					addLine(null, PayCategory.FLSA, BigDecimal.ONE, BigDecimal.ONE, extra, false, null);
				}
				flsaAmount = extra;
			}
		}
	}

	/**
	 * Creates Pay Breakdown line items for non-union talent timecards that have
	 * a re-use fee and/or agent commission specified. LS-2142
	 */
	@SuppressWarnings("rawtypes")
	private void generateTalentFees() {
		StartForm sf = htg.weeklyTimecard.getStartForm();
		if (sf.getEmpReuse() != null && sf.getEmpReuse().signum() > 0) {
			// Generate re-use fee entry
			addLine(null, PayCategory.REUSE, BigDecimal.ONE, BigDecimal.ONE, sf.getEmpReuse(), false, null);
		}

		if (htg.isModelRelease) { // Add PB lines for Model release Weather & Intimates payments. LS-4664
			for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
				if (dt.getDayType() != null) {
					lineDate = dt.getDate(); // apply pay to appropriate date for splits
					if (dt.getWeatherDay()) {
						addLine(null, PayCategory.FLAT_RATE, BigDecimal.ONE, BigDecimal.ONE, htg.modelRelease.getWeatherPerDay(), false, null);
						dt.setPayAmount(NumberUtils.safeAdd(htg.modelRelease.getWeatherPerDay(), dt.getPayAmount())); // include in daily pay amount
					}
					if (dt.getIntimatesDay()) {
						addLine(null, PayCategory.FLAT_RATE, BigDecimal.ONE, BigDecimal.ONE, htg.modelRelease.getIntimatesPerDay(), false, null);
						dt.setPayAmount(NumberUtils.safeAdd(htg.modelRelease.getIntimatesPerDay(), dt.getPayAmount())); // include in daily pay amount
					}
//					if (dt.getDayType() == DayType.TR) {
//						addLine(null, PayCategory.TRAVEL, BigDecimal.ONE, BigDecimal.ONE, htg.modelRelease.getTravelDayPerDay(), false, null);
//						dt.setPayAmount(NumberUtils.safeAdd(htg.modelRelease.getTravelDayPerDay(), dt.getPayAmount())); // include in daily pay amount
//					}
				}
			}
			lineDate = lastWorkedDate; // restore 'split' date for following line items
		}

		if (sf.getEmpAgentCommisssion() != null && sf.getEmpAgentCommisssion().signum() > 0) {
			// Generate commission entry, based on total of labor payments
			BigDecimal commissionRate = sf.getEmpAgentCommisssion().divide(Constants.DECIMAL_100); // change % to multiplier
			BigDecimal totalBasis = BigDecimal.ZERO;
			List payList;
			if (split) {
				payList = htg.weeklyTimecard.getPayDailyLines();
			}
			else {
				payList = htg.weeklyTimecard.getPayLines();
			}
			for (Iterator iter = payList.iterator(); iter.hasNext(); ) {
				PayBreakdownMapped pb = (PayBreakdownMapped)iter.next();
				PayCategory pCat = pb.getCategoryType();
				if (pCat != null && pCat.paysCommission()) { // commission is paid on this line item
					BigDecimal lineTotal = NumberUtils.safeMultiply(pb.getQuantity(), pb.getMultiplier(), pb.getRate());
					totalBasis = NumberUtils.safeAdd(totalBasis, NumberUtils.scaleTo(lineTotal, 0, 4));
				}
			}
			LOG.debug("basis=" + totalBasis);
			addLine(null, PayCategory.AGENT_FEE_NONTAX, BigDecimal.ONE, commissionRate, totalBasis , false, null);
		}
	}

	/**
	 * Determine if there is a layoff point during the current timecard. This
	 * may be indicated by any one of:
	 * <ul>
	 * <li>The StartForm's Work End date falling during the week;</li>
	 * <li>The StartForm's Effective End date falling during the week;</li>
	 * <li>A DayType of WkLayOf (Work-Layoff) occurring during the week.</li>
	 * </ul>
	 * The HtgData field hasLayoff will be set to true if any of these occur,
	 * otherwise it will be set to false.
	 *
	 * @return False iff there is a work day specified following a WkLayOf
	 *         DayType. In this case an error message will have been posted.
	 */
	private boolean checkLayoff() {
		boolean layoff = false;
		boolean ret = true;
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getDayType() != null) {
				if (dt.getDayType() == DayType.WL) {
					layoff = true;
				}
				else if (layoff) { // had layoff dayType earlier in week
					if (dt.getDayType().isWork()) {
						// Working dayType follows layoff - invalid
						String date = new SimpleDateFormat("M/d").format(dt.getDate());
						TimecardService.error(htg, "Timecard.HTG.WorkDayFollowsLayoff", date);
						ret = false;
						break;
					}
					else if (dt.getDayType().isAlwaysPaid()) {
						// warning: paid dayType follows layoff
						String date = new SimpleDateFormat("M/d").format(dt.getDate());
						TimecardService.warn(htg, "Timecard.HTG.PaidDayFollowsLayoff", date);
					}
				}
			}
		}

		if (! layoff && ! htg.isSimpleWeekly) {
			// LS-1557: do not consider SF end-dates for layoff if 'simple weekly' flag set.
			StartForm sf = htg.weeklyTimecard.getStartForm();
			if (sf.getWorkEndDate() != null || sf.getEffectiveEndDate() != null) {
				Date weDate = htg.weeklyTimecard.getEndDate();
				Date priorWeDate = TimecardUtils.calculatePriorWeekEndDate(weDate);
				if (sf.getWorkEndDate() != null &&
						sf.getWorkEndDate().before(weDate) &&
						sf.getWorkEndDate().after(priorWeDate)) {
					layoff = true;
				}
				else if (sf.getEffectiveEndDate() != null &&
						sf.getEffectiveEndDate().before(weDate) &&
						sf.getEffectiveEndDate().after(priorWeDate)) {
					layoff = true;
				}
			}
		}
		htg.hasLayoff = layoff;
		return ret;
	}

	/**
	 * Conditionally create a new Pay Breakdown line item for labor hours with a
	 * Custom Multiplier value. This method can generate the category text based
	 * on the multiplier value and 'premium' flag setting. The category will be
	 * set to "n.nx Hours" or "n.nx Hours (Prem)", where 'n.n' is the multiplier
	 * given.
	 *
	 * @param pj The PayJob to use as a source for rates and account codes for
	 *            the new pay breakdown line item.
	 * @param totalCust The number of hours to enter in the pay breakdown line.
	 *            If null or zero, no Pay Breakdown line item will be generated.
	 * @param customMult Custom multiplier value.
	 * @param payRateType The rate type for this entry, e.g., Regular, premium, daily, etc.  LS-2241
	 * @param specialLabel The label to use for the line item; if not specified,
	 *            a typical labor hours label (e.g., "1.5x Hours") will be
	 *            generated.
	 * @param pjdFilter The filter to be used if individual PayJobDaily entries
	 *            are processed; this is currently only used if touring rates
	 *            have been specified in this timecard.
	 */
	private void addHoursLine(PayJob pj, BigDecimal totalCust,
			BigDecimal customMult, PayRateType payRateType, String specialLabel, Filter<PayJobDaily> pjdFilter) {
		if (customMult.compareTo(PayJobService.CUSTOM_MULTIPLIER_TOURS) == 0) { // Touring rates; LS-1347
			addToursLines(pj, pjdFilter);
			return;
		}
		if (totalCust == null || totalCust.signum() == 0) {
			return;
		}
		PayCategory pc = null;
		if (customMult != null) {
			if (customMult.compareTo(BigDecimal.ONE) == 0) {
				pc = ((payRateType == PayRateType.P) ? PayCategory.X10_HOURS_PREM : PayCategory.X10_HOURS);
			}
			else if (customMult.signum() < 0) {
				pc = PayCategory.FLAT_RATE;
			}
		}
		if (pc == null) {
			if (specialLabel != null) {
				DecimalFormat fmt = new DecimalFormat("@#0.0#x");
				specialLabel += fmt.format(customMult);
			}
			PayBreakdownMapped pb = addLine(pj, PayCategory.CUSTOM, totalCust, payRateType, specialLabel);
			if (pb != null && customMult != null) {
				if (split && customMult.compareTo(BigDecimal.ONE) != 0) {
					// Daily Split (for Team export) always uses multiplier=1,
					// so multiply the rate by the multiplier...
					BigDecimal rate;
					if (pb.getRate() != null) {
						// rate = rate * multiplier
						rate = pb.getRate().multiply(customMult).setScale(4,RoundingMode.HALF_UP);
					}
					else {
						rate = BigDecimal.ZERO;
					}
					pb.setRate(rate);
					pb.setMultiplier(BigDecimal.ONE);
					// Adjust dailyPay, as actual line total will be changed. LS-1347
					dailyPay = dailyPay.subtract(pb.getTotal()); // delete existing total.
					dailyPay = dailyPay.add(pb.calculateTotal()); // add real total for this entry.
				}
				else { // not Daily Split, leave multiplier separate
					pb.setMultiplier(customMult);
				}
				DecimalFormat fmt = new DecimalFormat("#0.0#");
				String label;
				if (specialLabel != null) {
					label = specialLabel;
				}
				else {
					label = fmt.format(customMult);
					if (htg.otExtended) {
						label = Constants.EXTENDED_DAY_PREFIX + label;
					}
					else {
						label += "x Hours";
						if (payRateType == PayRateType.P) {
							label += " (Prem)";
						}
					}
				}
				pb.setCategory(label);
			}
		}
		else {
			addLine(pj, pc, totalCust, payRateType, specialLabel);
		}
	}

	/**
	 * Conditionally create Pay Breakdown line items for touring days on a 'hybrid' timecard.
	 * The category will be set to "Flat Rate", and the quantity will be one. The
	 * rate is pulled from the DailyTime.payAmount field.
	 *
	 * @param pj The PayJob to use as a source for account codes for the new pay
	 *            breakdown line item(s).
	 * @param pjdFilter The filter to be used for the individual PayJobDaily
	 *            entries processed.
	 */
	private int addToursLines(PayJob pj, Filter<PayJobDaily> pjdFilter) {
		// Handle daily touring rates here; LS-1347
		// Each day's rate may be different, and the rate must be pulled from the
		// DailyTime.payAmount, which is set by the PayJobService processing ("Fill Jobs")
		int ix = 0; // daily index
		int tourDays = 0;
		for (PayJobDaily pjd : pj.getPayJobDailies()) {
			DailyTime dt = htg.weeklyTimecard.getDailyTimes().get(ix);
			if (dt.getDayType() != null && dt.getDayType().isTours() &&
					dt.getPayAmount() != null && dt.getPayAmount().signum() != 0) {
				tourDays++;
				if ((pjdFilter == null || pjdFilter.filter(pjd))) {
					if (split) {
						// Generate effective hourly rate; needed for AS400. LS-4003
						BigDecimal rate = dt.getPayAmount().divide(Constants.DAILY_STRAIGHT_HOURS, 4, RoundingMode.UP);
						addLine(pj, PayCategory.FLAT_RATE, Constants.DAILY_STRAIGHT_HOURS, BigDecimal.ONE, rate, false, null);
					}
					else {
						addLine(pj, PayCategory.FLAT_RATE, BigDecimal.ONE, BigDecimal.ONE, dt.getPayAmount(), false, null);
					}
				}
			}
			ix++;
		}
		return tourDays;
	}

	/**
	 * Conditionally create a new Pay Breakdown line item using account codes
	 * and rate from the supplied PayJob. The quantity/hours must be supplied;
	 * its use, and the determination of the multiplier, is based on the
	 * attributes of the supplied PayCategory. If this quantity is null or zero,
	 * no line item is generated. Used for both Split and non-Split lines.
	 * <p>
	 * For 'special' items such as Travel, it is used for Split, but not non-Split.
	 *
	 * @param pj The PayJob to use as a source for rates and account codes for
	 *            the new pay breakdown line item.
	 * @param pc The PayCategory to assign to the new pay breakdown line.
	 * @param qtyHours The value to put in the Qty/Hours field: usually the
	 *            number of hours, but for FLAT_RATE entries, this is the number
	 *            of days (it still goes in the Qty/hours field). If this is
	 *            null or zero, no line item is generated.
	 * @param payRateType The rate type - regular, premium, daily, etc. LS-2241
	 * @param specialLabel Text to be used as the pay breakdown line label. If
	 *            null, the supplied PayCategory (pc) label is used.
	 * @return The newly-created PayBreakdown instance, if any. (May be null.)
	 */
	private PayBreakdownMapped addLine(PayJob pj, PayCategory pc, BigDecimal qtyHours,
			PayRateType payRateType, String specialLabel) {
		PayBreakdownMapped pb = null;
		if (qtyHours != null && qtyHours.signum() != 0) {
			pb = getNewPayBreakdown(lineDate);
			pb.setQuantity(qtyHours);
			// Get PayCategory fields
			pb.setCategory(specialLabel == null ? pc.getLabel() : specialLabel);
			if (pc.getMultiplier() != null) {
				pb.setMultiplier(pc.getMultiplier());
			}
			else {
				pb.setMultiplier(BigDecimal.ONE);
			}
			// Get PayJob fields
			if (htg.isCommercial) {
				pb.setAccountMajor(filterAccount);
			}
			else { // Set account codes from the Pay Job or SF fields
				AccountCodes ac = findAccountCodes(pc, pj);
				pb.setAccountCodes(ac); // copy acct codes from appropriate source
			}
			pb.setJobNumber(pj.getJobAccountNumber());
			if (payRateType == PayRateType.P) {
				pb.setRate(pj.getPremiumRate());
			}
			else if (pc == PayCategory.FLAT_RATE) {
				pb.setMultiplier(qtyHours); // number of days
				EmployeeRateType type = pj.getWeeklyTimecard().getStartForm().getRateType();
				if (type == EmployeeRateType.WEEKLY) {
					pb.setRate(pj.getWeeklyRate());
					pb.setQuantity(FLAT_POINT2); // Qty = 0.2 (1/5 of weekly = daily)
				}
				else if (type == EmployeeRateType.DAILY) {
					pb.setRate(pj.getDailyRate());
					pb.setQuantity(BigDecimal.ONE);
				}
				else if (type == EmployeeRateType.HOURLY) {
					pb.setRate(pj.getDailyRate());
					pb.setQuantity(qtyHours);
					pb.setMultiplier(BigDecimal.ONE);
				}
			}
			else if (pc != PayCategory.MEAL_PENALTY) {
				pb.setRate(pj.getRate());
			}
			// Add to timecard & bump line number
			addPayBreakdown(pb);
		}
		return pb;
	}

	/**
	 * Conditionally add a new PayBreakdown item using the account codes from
	 * the supplied PayJob, the category text from the given PayCategory, and
	 * the specified quantity (hours), multiplier, and rate. A breakdown line
	 * will NOT be added if the quantity (hours) is null or zero.
	 * <p>
	 * This typically handles 'special' lines such as Travel, for non-split line
	 * creation.
	 *
	 * @param pj The PayJob to use as a source for account codes for the new pay
	 *            breakdown line item; if null, uses the first PayJob.
	 * @param pc The PayCategory to assign to the new pay breakdown line.
	 * @param quantity The value to put in the Qty/Hours field.
	 * @param multiplier The value to put in the "X" field.
	 * @param rate The value to put in the Rate field.
	 * @param multLabel If true, append the multiplier to the category label.
	 *            This is used for specials cases such as Holiday Worked line
	 *            items, by adding the multiplier as text, e.g., "Holiday
	 *            Worked@4.0x".
	 * @param specialLabel Text to be used as the pay breakdown line label. If
	 *            null, the supplied PayCategory (pc) label is used.
	 */
	private void addLine(PayJob pj, PayCategory pc,
			BigDecimal quantity, BigDecimal multiplier, BigDecimal rate, boolean multLabel, String specialLabel) {

		if (quantity != null && quantity.signum() != 0 && multiplier.signum() != 0) {
//			Date itemDate = lineDate;
//			if (pc == PayCategory.FLSA) {
//				itemDate = lastWorkedDate;
//			}
			PayBreakdownMapped pb = getNewPayBreakdown(lineDate);
			pb.setQuantity(quantity);
			pb.setMultiplier(multiplier.abs());
			if (specialLabel != null) {
				pb.setCategory(specialLabel);
			}
			else {
				if (multLabel) {
					DecimalFormat fmt = new DecimalFormat("@#0.0#");
					String label = pc.getLabel();
					label += fmt.format(pb.getMultiplier());
					label += (multiplier.signum() > 0 ? "x" : "D");
					pb.setCategory(label);
				}
				else {
					pb.setCategory(pc.getLabel());
				}
			}

			// Get PayJob fields
			AccountCodes ac = htg.weeklyTimecard.getStartForm().getByPayCategory(pc);
			if (pj == null) { // use default - first PayJob in timecard: (LS-2142)
				pj = htg.weeklyTimecard.getPayJobs().get(0);
			}
			if (htg.isCommercial) {
				if (ac != null) {
					// Set values from the Start Form "Allowances" account codes;
					// but if Allowance code is blank, and this is 'labor' item,
					// use the current 'filterAccount' (i.e., prep/wrap/shoot) code.
					if (ac.getMajor() == null || ac.getMajor().isEmpty()) {
						if (pc.getIsLabor()) {
							pb.setAccountMajor(filterAccount);
						}
					}
					else {
						pb.setAccountMajor(ac.getMajor());
					}
					pb.setAccountFree(ac.getFree());
				}
				else {
					pb.setAccountMajor(filterAccount);
				}
			}
			else { // TV/Feature production
				if (ac != null) {
					// Set values from the Start Form "Allowances" account codes
					pb.setAccountCodes(ac);
				}
				else { // Set account codes from the Pay Job or SF fields
					ac = findAccountCodes(pc, pj); // determine appropriate source
					pb.setAccountCodes(ac); // copy acct codes
				}
			}

			pb.setJobNumber(pj.getJobAccountNumber());
			pb.setRate(rate);

			// Add to timecard
			addPayBreakdown(pb);
		}
	}

	/**
	 * Given a PayExpense line item, generate the matching PayBreakdown entry
	 * and add it to the timecard.  Note that the PayBreakdown is only created
	 * if the PayExpense meets the following criteria: the category, quantity,
	 * and rate must be non-null; the quantity must be > 0; and the rate must
	 * be > 0.
	 *
	 * @param exp The PayExpense, which is the source of data for the new entry.
	 */
	private void addExpense(PayExpense exp) {

		if (exp.getCategory() != null && exp.getQuantity() != null && exp.getRate() != null &&
				exp.getQuantity().signum() != 0 &&
				exp.getRate().signum() != 0 ) {
			PayBreakdownMapped pb = getNewPayBreakdown(htg.weeklyTimecard.getEndDate());

			if (split) {
				// Assign the date of the last day worked number to the paybreakdown for all expense/reimbursement items.
				pb = getNewPayBreakdown(lastWorkedDate);
			}

			pb.setCategory(exp.getCategory());
			pb.setMultiplier(BigDecimal.ONE);

			pb.setAccountLoc(exp.getAccountLoc());
			pb.setAccountMajor(exp.getAccountMajor());
			pb.setAccountDtl(exp.getAccountDtl());
			pb.setAccountSub(exp.getAccountSub());
			pb.setAccountSet(exp.getAccountSet());
			pb.setAccountFree(exp.getAccountFree());
			pb.setAccountFree2(exp.getAccountFree2());
			pb.setJobNumber(htg.weeklyTimecard.getJobNumber());

			pb.setQuantity(exp.getQuantity());
			pb.setRate(exp.getRate());

//			pb.setProdEpisode(prodEpisode);

			addPayBreakdown(pb);
		}
	}

	/**
	 * Find the appropriate set of AccountCodes to use for a breakdown item. The
	 * PayJob's account codes will be used unless this is a labor item, in which
	 * case the Start Form's 'rate table' account codes may be used if the
	 * current multiplier is 1x, 1.5x, or 2x, and the corresponding account
	 * codes in the rate table have a non-empty 'major' value. Similarly for Flat
	 * rate (exempt) entries, either the Weekly or Daily account codes will be
	 * used, based on the EmployeeRateType.
	 *
	 * @param pc The PayCategory for the breakdown line item being generated.
	 * @param pj The PayJob that is the source of the breakdown information.
	 * @return A reference to the AccountCodes to be used for the breakdown line
	 *         item being generated.
	 */
	private AccountCodes findAccountCodes(PayCategory pc, PayJob pj) {
		AccountCodes ac = pj.getAccount();
		if (pc.getIsLabor()) {
			StartForm sf = htg.weeklyTimecard.getStartForm();
			if (sf != null) {
				if (pc == PayCategory.FLAT_RATE) {
					if (sf.getRateType() == EmployeeRateType.DAILY) {
						if (! StringUtils.isEmpty(sf.getProd().getDaily().getMajor())) {
							ac = sf.getProd().getDaily();
						}
					}
					else if (sf.getRateType() == EmployeeRateType.WEEKLY) {
						if (! StringUtils.isEmpty(sf.getProd().getWeekly().getMajor())) {
							ac = sf.getProd().getWeekly();
						}
					}
				}
				else if (pc == PayCategory.DAY6) {
					if (! StringUtils.isEmpty(sf.getProd().getDay6().getMajor())) {
						ac = sf.getProd().getDay6();
					}
				}
				else if (pc == PayCategory.DAY7) {
					if (! StringUtils.isEmpty(sf.getProd().getDay7().getMajor())) {
						ac = sf.getProd().getDay7();
					}
				}
				else if (pc == PayCategory.IDLE_PAY) {
					if (! StringUtils.isEmpty(sf.getProd().getIdleDay6().getMajor())) {
						ac = sf.getProd().getIdleDay6();
					}
				}
				else {
					if (! StringUtils.isEmpty(sf.getProd().getHourly().getMajor())) {
						ac = sf.getProd().getHourly();
					}
					if (pc.getMultiplier() != null) {
						if (pc.getMultiplier().compareTo(Constants.DECIMAL_ONE_FIVE) == 0) {
							if (! StringUtils.isEmpty(sf.getProd().getX15().getMajor())) {
								ac = sf.getProd().getX15();
							}
						}
						else if (pc.getMultiplier().compareTo(Constants.DECIMAL_TWO) == 0) {
							if (! StringUtils.isEmpty(sf.getProd().getX20().getMajor())) {
								ac = sf.getProd().getX20();
							}
						}
					}
				}
			}
		}
		return ac;
	}

	/**
	 * Create a new PayBreakdown or PayBreakdownDaily line item, initializing it
	 * with our current timecard and line number.
	 *
	 * @param date The date to assign, if this is a PayBreakdownDaily instance.
	 * @return New instance of PayBreakdown or PayBreakdownDaily, depending on
	 *         the value of the {@link #split} field.
	 */
	private PayBreakdownMapped getNewPayBreakdown(Date date) {
		PayBreakdownMapped pb;
		if (split) {
			pb = new PayBreakdownDaily(htg.weeklyTimecard, lineNum, date);
		}
		else {
			pb = new PayBreakdown(htg.weeklyTimecard, lineNum);
		}
		return pb;
	}

	/**
	 * Add the given PayBreakdown line item to either the 'traditional' Pay Breakdown
	 * table, or to the 'daily' Pay breakdown table, depending on our {@link #split} setting.
	 * The current line number value, {@link #lineNum}, is also incremented.
	 * @param pb The PayBreakdown line item instance.
	 */
	private void addPayBreakdown(PayBreakdownMapped pb) {
		if (split) {
			PayCategory cat = pb.getCategoryType();
			if (pb.getMultiplier().compareTo(BigDecimal.ONE) != 0) {
				if (cat != PayCategory.MEET_GUARANTEE && cat != PayCategory.FLAT_RATE &&
						(cat != PayCategory.CUSTOM || pb.getCategory().startsWith("Travel")) &&
						cat.getIsLabor()) {
					if (pb.getRate() != null) {
						BigDecimal rate = pb.getRate().multiply(pb.getMultiplier()).setScale(4,RoundingMode.HALF_UP);
						pb.setRate(rate);
						pb.setMultiplier(BigDecimal.ONE);
					}
				}
			}
			if (cat.getIsLabor() && (dailyPay != null)) {
				dailyPay = dailyPay.add(pb.calculateTotal()); // accumulate total for each day. LS-1347
			}
			htg.weeklyTimecard.getPayDailyLines().add(lineNum, (PayBreakdownDaily)pb);
		}
		else {
			htg.weeklyTimecard.getPayLines().add(lineNum, (PayBreakdown)pb);
		}
		lineNum++;
	}

	/**
	 * Find, or create, a PayJobHours array to hold hours corresponding to the
	 * specified PayCategory. The arrays are kept in the
	 * {@link #specialHoursMap}, keyed by the PayCategory. If the requested
	 * entry doesn't already exist, it gets created and added to the map.
	 *
	 * @param pc The PayCategory related to the PayJobHours array.
	 * @return an array of PayJobHours with as many entries as there are
	 *         PayJob`s in the current timecard.
	 */
	private PayJobHours[] findSpecialPayJobHours(PayCategory pc) {
		PayJobHours[] p;
		p = specialHoursMap.get(pc);
		if (p == null) {
			p = createPayJobHours();
			specialHoursMap.put(pc, p);
		}
		return p;
	}

	/**
	 * @return an array of PayJobHours with as many entries as there are
	 *         PayJob`s in the current timecard.
	 */
	private PayJobHours[] createPayJobHours() {
		PayJobHours[] p = new PayJobHours[htg.weeklyTimecard.getPayJobsSize()+1];
		for (int i = 0; i < p.length; i++) {
			p[i] = new PayJobHours();
		}
		return p;
	}

	/**
	 * A Filter instance used when selecting a subset of PayJob entries to be totaled.
	 * This will select all those entries whose accountMajor field matches the
	 * value of {@link #filterAccount}, and which are NOT special hours, such as
	 * travel or holiday.
	 */
	private final Filter<PayJobDaily> filterByAccount = new Filter<PayJobDaily>() {
		@Override
		public boolean filter(PayJobDaily filtered) {
			if (filterAccount != null && filtered.getAccountNumber() == null) {
				return false;
			}
			if (filtered.getIsSpecial()) {
				// marked special; probably 6th/7th day, will be added separately
				return false; // so filter it out when accumulating by account-code
			}
			// See if this PJD corresponds to a day whose hours are "special" (e.g., travel, holiday),
			// and will be totaled via the special-hours handling. If so, do not include in filtered results.
			DailyTime dt = htg.weeklyTimecard.getDailyTimes().get(filtered.getDayNum()-1);
			if (dt.getDayType() != null &&
					dt.getDayType().getCategory() != null) {
				return false; // filter out "special" entries
			}
			if (filterAccount == null) {
				return filtered.getAccountNumber() == null;
			}
			return filtered.getAccountNumber().equals(filterAccount);
		}
	};

	/**
	 * A Filter instance used when selecting a subset of PayJobDaily entries to
	 * be totaled. This will select any entry with a date matching
	 * {@link #lineDate}.
	 */
	private final Filter<PayJobDaily> filterByDate = new Filter<PayJobDaily>() {
		@Override
		public boolean filter(PayJobDaily filtered) {
			if (lineDate != null && ! filtered.getDate().equals(lineDate)) {
				return false;
			}
			return true;
		}
	};

//	/**
//	 * A Filter instance used when selecting a subset of PayJobDaily entries to
//	 * be totaled. This will select only entries with 'special' hours, such as travel or
//	 * holiday.
//	 */
//	private final Filter<PayJobDaily> filterSpecial = new Filter<PayJobDaily>() {
//		@Override
//		public boolean filter(PayJobDaily filtered) {
//			// See if this PJD corresponds to a day whose hours are "special" (e.g., travel, holiday),
//			// and will be totaled via the special-hours handling. If so, include in filtered results.
//			DailyTime dt = htg.weeklyTimecard.getDailyTimes().get(filtered.getDayNum()-1);
//			if (dt.getDayType() != null &&
//					dt.getDayType().getCategory() != null) {
//				return true; // filter out "special" entries
//			}
//			return false;
//		}
//	};

	/**
	 * Renumber the 'lineNumber' fields of the existing PayBreakdown entries.
	 *
	 * @param weeklyTimecard The timecard whose lines are to be renumbered.
	 */
	public static void renumberPayLines(WeeklyTimecard weeklyTimecard) {
		byte i = 0;
		for (PayBreakdown pb : weeklyTimecard.getPayLines()) {
			pb.setLineNumber(i++);
		}
		i = 0;
		for (PayBreakdownDaily pb : weeklyTimecard.getPayDailyLines()) {
			pb.setLineNumber(i++);
		}
	}

	/**
	 * Calculate the total dollar amount of a pair of MPV counts based on the
	 * specified set of rules. Used for unit testing, e.g., by JUnit test
	 * classes.
	 *
	 * @param mpvRules
	 * @param mpv1
	 * @param mpv2
	 * @param mpv3
	 * @param straightRate The employee's "straight rate"; some rules use this
	 *            value (or a multiple of it) as a penalty increment.
	 * @param prevailingRate The employee's "prevailing rate", which may be used
	 *            in determining the total dollar value of the meal penalties if
	 *            the specified rule uses that method.
	 * @return The total dollar value of the MPVs for the given DailyTime
	 *         settings.
	 */
	public static BigDecimal calculateMpvPenaltyTest(List<MpvRule> mpvRules,
			Byte mpv1, Byte mpv2, Byte mpv3, BigDecimal straightRate, BigDecimal prevailingRate) {
		AuditEvent evt = new AuditEvent();
		MpvRule rule = mpvRules.get(0);

		// Get penalty for first meal period
		BigDecimal penalty = calculateMpvPenalty(rule, mpv1, straightRate, prevailingRate, evt);

		if (mpvRules.size() > 1) { // MPV has separate Meal-2 rule
			rule = mpvRules.get(1);
		}
		// Add penalty for second meal period
		penalty = penalty.add(calculateMpvPenalty(rule, mpv2, straightRate, prevailingRate, evt));

		if (mpvRules.size() > 2) { // MPV has separate Meal-3 rule
			rule = mpvRules.get(2);
		}
		// Add penalty for third meal period
		penalty = penalty.add(calculateMpvPenalty(rule, mpv3, straightRate, prevailingRate, evt));

		return penalty;
	}

	/**
	 * Determine if a BigDecimal value is an integer (whole number), i.e., has
	 * no fractional part.
	 *
	 * @param bd The BigDecimal in question.
	 * @return True iff the value is an integer.
	 */
	private boolean isIntegerValue(BigDecimal bd) {
		return bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0;
		// Note that the last test, using stripTrailingZeros(), should be sufficient,
		// except that it is known to fail for zero, so the first test has been added,
		// and also it is slow, so the second test (which should be quite fast) is
		// done, as it will be true in many cases.
	}

}
