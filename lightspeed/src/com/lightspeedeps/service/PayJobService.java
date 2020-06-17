/**
 * File: PayJobService.java
 */
package com.lightspeedeps.service;

import java.math.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.SpecialRuleService.HtgPhase;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.payroll.*;


/**
 * Contains methods with business logic related to timecard processing -- in
 * particular, breaking down the hours recorded on a timecard into the Job
 * Tables.
 * @see PayJob
 * @see PayBreakdownService
 */
public class PayJobService extends BaseService {
	private static final Log log = LogFactory.getLog(PayJobService.class);

	/** Set as custom multiplier value to indicate this PayJob column is a Daily value (not hours) */
	public static final BigDecimal CUSTOM_MULTIPLIER_DAILY = BigDecimal.ONE.negate();
	/** Set as custom multiplier value to indicate this PayJob column is a Daily touring value */
	public static final BigDecimal CUSTOM_MULTIPLIER_TOURS = new BigDecimal(99).negate();

	/** The HtgData instance has most of the information needed to do the
	 * PayJob processing. */
	private final HtgData htg;

	/** */
	private final NightPremiumService nightPremiumService;

	/** The current Audit trail event object. */
	private final AuditEvent event;

	/** True iff this service is being run inside a unit test (e.g., JUnit). */
	private boolean unitTest;

	/** True if the this week starts before Prep ends, and the Start Form has prep rates. */
	private boolean usingPrepRates;

	private CallBackRule callBackRule;

	/*package*/ GoldenRule goldenRule;

	/*package*/ GuaranteeRule guaranteeRule;

	/*package*/ HolidayRule holidayRule;

	private MpvRule mpvRule;

	/*package*/ NtPremiumRule npRule;

	private OnCallRule onCallRule;

	/*package*/ OvertimeRule overtimeRule;

	private RestRule restRule;

	/*package*/ WeeklyRule weeklyRule;

	private String dailyRuleKeys = "";

	/** Map to save rates retrieved from contractRate table for current timecard.
	 * Maps rate-code to amount (rate). LS-2241 */
	private Map<String,BigDecimal> jobRates = new HashMap<>();

	/**
	 * The usual constructor. The current HtgData object must be provided; the
	 * timecard, Production, StartForm, and other necessary entities will be
	 * determined from the HtgData fields.
	 *
	 * @param evt The AuditEvent (audit trail) object.
	 */
	public PayJobService(HtgData pHtg, AuditEvent evt) {
		htg = pHtg;
		if (evt != null) {
			event = evt;
		}
		else {
			event = htg.service.startEvent(AuditEventType.FILL_JOBS, null, htg.weeklyTimecard);
		}
		nightPremiumService = new NightPremiumService(htg);
		htg.auditEvent = event;
	}

	/**
	 * Use the raw hours and the daily split information to fill in the PayJob
	 * hourly information. This method assumes the timecard has already been
	 * validated.
	 *
	 * @return True iff the Fill Job completed normally.
	 */
	public boolean fillPayJobsValid() {
		boolean ret = true;
		htg.ruleService.getSpecialService().resetRuleLogging(); // log Special rules at least once
		boolean tours = htg.production.getType().isTours();

		if ((! unitTest) && (! tours)) {
			// r2.9.5086 calculate default guaranteed hours for both Union and non-union;
			// Because of no rules, we'll need it when guarantee (from rules) turns out to be zero.
			htg.defaultGuarHours = calculateGuarHours(htg.weeklyTimecard.getDailyTimes().get(0));
		}

		// Update day types for worked days that are currently null or non-worked holiday,
		// and set the work zone for Idle days to Distant.
		TimecardUtils.fillDayTypes(htg.weeklyTimecard);

		// clear existing data in daily pay job entries
		for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
			for (PayJobDaily pjd : pj.getPayJobDailies()) {
				pjd.eraseValues();
			}
		}
		if (htg.weeklyTimecard.getPayJobs().size() == 0) {
			// We need at least one PayJob for further processing.
			WeeklyTimecardDAO.addPayJob(htg.weeklyTimecard);
		}

		if (tours) {
			return true; // nothing else needed for Tours timecards
		}

		weeklyRule = htg.ruleService.findWeeklyRule(event);

		htg.lastWrap = BigDecimal.ZERO; // Wrap from Saturday of last week
		htg.lastMult = BigDecimal.ONE;
		htg.nextMinCall = BigDecimal.ZERO;
		htg.priorTcImportant = false;
		jobRates.clear(); // LS-2241

		if (weeklyRule.getOvertimeIncrement().signum() >= 0) {
			// Positive increment is actual increment for rounding
			htg.otRoundingWeek = HourRoundingType.convertFraction(weeklyRule.getOvertimeIncrement());
		}
		else { // Negative increment just means to use the payroll preference
			htg.otRoundingWeek = htg.preference.getHourRoundingType();
		}

		htg.exemptHours = TimecardCheck.calculateExemptHours(htg.weeklyTimecard);

		// Fill in the DailyTime.workDayNum values
		calculateWorkDayNumbers(htg.weeklyTimecard, htg);

		// Automatically split jobs, if necessary, due to Prep/Shoot boundary,
		// Studio/Distant mixed week, or other reason.
		if (htg.weeklyTimecard.getPayJobs().size() < 2 && !htg.hotCostsReRate) {
			// do NOT try and auto-split if there's already a split.
			autoJobSplit();
		}

		DailyTime originalPriorDt = htg.priorDailyTime; // Save last Saturday's DailyTime

		// Build job information for Paid Holidays first; this prevents the
		// hours from being pushed into weekly OT for Cumulative schedules.
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			dt.setPaidHours(null);
			if (dt.getDayType() != null && dt.getDayType() == DayType.HP) {
				if (! fillPayJobDaily(dt)) {
					ret = false;
					break;
				}
			}
			htg.priorDailyTime = dt; // keep prior day available for next day's processing
		}
		htg.priorDailyTime = originalPriorDt; // restore initial value (last Saturday's DailyTime)

		// See if we need straight time hours from last week...
		if (htg.priorTimecard != null &&
				((htg.preference.getFirstWorkDayNum() != 1) || htg.floatingWeek) &&
				(weeklyRule.getOvertimeWeeklyBreak().compareTo(Constants.HOURS_IN_A_WEEK) < 0)) {
			// There's a weekly OT break, and
			// (either producer's work week does not start on Sunday OR floating work week is in effect), so...
			// we need to know how many straight-time hours were worked last week.
			htg.weekStrHours = calculateStraightTime(htg.priorTimecard);
			htg.priorTcImportant = true; // last week's TC may affect this week's gross.
			if (htg.weekStrHours.compareTo(weeklyRule.getOvertimeWeeklyBreak()) > 0) {
				htg.weekStrHours = weeklyRule.getOvertimeWeeklyBreak();
			}
		}

		// Build job information one day at a time, for non-Holiday days
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			// Reset total straight-time hours for the week on first 'work' day
			if (htg.floatingWeek) {
				if (dt.getWorkDayNum() == 1) {
					htg.weekStrHours = BigDecimal.ZERO;
				}
			}
			else {
				// Not using 'floating week', so first work day is based on Preference setting
				if (dt.getDayNum() == htg.preference.getFirstWorkDayNum()) {
					htg.weekStrHours = BigDecimal.ZERO;
				}
			}
			if (dt.getDayType() == null || dt.getDayType() != DayType.HP) { // Skip HP, already done
				if (! fillPayJobDaily(dt)) {
					ret = false;
					break;
				}
			}
			else {
				htg.nextMinCall = BigDecimal.ZERO;
			}
			htg.priorDailyTime = dt; // keep prior day available for next day's processing
		}

		// Move MPVs from DailyTime entries into Job Tables
		int i = 0;
		if (htg.weeklyTimecard.getPayJobs().size() > 0) { // at least one PayJob exists...
			PayJob pj;
			for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
				if (dt.getMpv1Payroll() != null) {
					pj = findPayJobForAmMpv(dt);
					pj.getPayJobDailies().get(i).setMpv1(dt.getMpv1Payroll());
				}
				if (dt.getMpv2Payroll() != null) {
					pj = findPayJobForPmMpv(dt);
					pj.getPayJobDailies().get(i).setMpv2(dt.getMpv2Payroll());
				}
				if (dt.getMpv3Payroll() != null) {
					pj = findPayJobForWrapMpv(dt);
					pj.getPayJobDailies().get(i).setMpv3(dt.getMpv3Payroll());
				}
				if (dt.getAccountMajor() != null) {
					if (htg.isCommercial) {
					// Copy daily account number to the matching day entry in all PayJob`s
						for (PayJob pjx : htg.weeklyTimecard.getPayJobs()) {
							pjx.getPayJobDailies().get(i).setAccountNumber(dt.getAccountMajor().trim());
						}
					}
				}
				i++; // increment index for payJobDailies to match DailyTime entry
			}
		}

		updateDailyPaidHours();

		if (htg.priorTcImportant && htg.priorTimecard != null &&
				htg.priorTimecard.getPayLines().size() == 0) {
			// warn that prior timecard should be calculated first.
			TimecardService.warn(htg, "Timecard.HTG.CalcPriorTimecardFirst");
		}

		// If we end up supporting Box rental amount in PayJob table, do something here.

		AuditUtils.updateEvent(event);

		TimecardCalc.calculateWeeklyTotals(htg.weeklyTimecard);
		TimecardCalc.calculateAndCheckJobHours(htg.weeklyTimecard);

		return ret;
	}

	/**
	 * Fill the Job Table(s) with the data from a single day.
	 *
	 * @param dt DailyTime instance for the day being calculated.
	 * @return True iff the job breakdown was successful.
	 */
	public boolean fillPayJobDaily(DailyTime dt) {

		htg.hours = null;
		htg.invadedRest = null;
		htg.goldEligHours1 = null;
		htg.goldEligHours2 = null;
		htg.goldEligHours3 = null;
		htg.holidayHours = null;
		htg.holidayGoldHours = null;
		htg.allGold = false;
		htg.goldPaysMeals = false;
		htg.dailyFlat = false;
		htg.meal1AdjHours = BigDecimal.ZERO;
		htg.meal2AdjHours = BigDecimal.ZERO;
		dt.setForcedCall(false);
		dt.setPayAmount(null);

		// Note that the "pre-phase" special rule call is done later...

		// Handle Special rule SP_PAY_NXT_WRK -- prior day
		// marked "next day" (the one we're processing) as worked
		DayType dayType = dt.getDayType();
		if (htg.nextDayIsWork) { // set by Special rule during yesterday's post-phase
			if (dayType == null) {
				dayType = DayType.GR;
			}
			else if (dayType.isHoliday()) {
				dayType = DayType.HW;
			}
			else if (! dayType.isWork()) {
				dayType = DayType.GR;
			}
		}
		else if ((dt.getWorkDayNum() == 0 || dayType == null) &&
				dayType != DayType.HP) {
			htg.nextMinCall = BigDecimal.ZERO;
			return true;	// not a paid day
		}
		if (dayType == null) {
			// this will never be true, but the check here convinces the
			// compiler, so later "may be null" warnings are eliminated
			return true;
		}

		if (dt.getWorkZone() == null) { // no work zone set,
			dt.setWorkZone(findStudioLoc(dt)); // use default from timecard
		}

		event.appendSummaryLine(dt.getWeekDayName() + "-"+ dt.getWorkDayNum());

		if ((! unitTest) || overtimeRule == null) {
			// update ruleService to current DailyTime:
			htg.ruleService.prepareDailyContractRules(dt, event);

			if (! findDailyRules(dt)) { // get today's rules & add to audit trail
				// one or more required rules were missing
				return false;
			}
		}

		if (dayType == DayType.HP && !holidayRule.getPaid()) {
			htg.nextMinCall = BigDecimal.ZERO;
			return true;	// no pay for "Paid Holiday" if rule says not to!
		}

		htg.paidWrap = dt.getWrap(); // may be adjust by MPV rules

		// Determine guaranteed hours (minimum call) from Start or from rule...
		htg.defaultGuarHours = calculateGuarHours(dt); // re-calc for each day; prep/prod & studio/distant may change it.
		BigDecimal sfGuarHours = htg.defaultGuarHours; // Start Form value
		sfGuarHours = sfGuarHours.min(guaranteeRule.getMaxHours()); // limit SF to rule's maximum
		htg.guarHours = guaranteeRule.getMinimumCall();
		if (htg.guarHours.signum() < 0) {
			// rule minimum < 0 means "use Start Form guarantee"
			htg.guarHours = sfGuarHours;
		}
		else if (htg.guarHours.signum() > 0) {
			// rule > 0, use larger of rule and StartForm
			if (dayType.isWorkTime() || (dayType == DayType.GR)) { // ... but only for "work" or "Guarantee" dayTypes
				htg.guarHours = htg.guarHours.max(sfGuarHours);
			}
		}
		// Note that if rule guarantee == 0, StartForm won't override it.

		// Are we using elapsed or worked for golden? Make this determination
		// prior to SpecialRule pre-phase call!
		// All rules have explicit Elapsed or Work setting:
		htg.goldHoursType = goldenRule.getWorkOrElapsed();

		htg.otHours = BigDecimal.ZERO; // may be changed by some special rules
		htg.strMult = BigDecimal.ONE;
		htg.otMult = BigDecimal.ONE;
		htg.ot2Mult = BigDecimal.ONE;
		htg.goldMult = BigDecimal.ONE;

		// set daily rounding to weekly value; may be changed by Special rules.
		htg.otRounding = htg.otRoundingWeek;

		// This placement of the pre-phase special rule call works for Travel/Work and Work/Travel
		if (! htg.ruleService.getSpecialService().prePhase(dt, HtgPhase.FILL_JOBS, event)) {
			htg.nextMinCall = BigDecimal.ZERO;
			return true;
		}

		// CHECK INTER-WEEK REST VIOLATION INFO
		if (htg.priorDailyTime != null && dt.getDayNum() == 1 &&
				htg.priorDailyTime.reportedWork() &&
				htg.priorDailyTime.getWrap() != null &&
				(htg.priorDailyTime.getDayType() == null ||
					htg.priorDailyTime.getDayType() == DayType.TW || ! htg.priorDailyTime.getDayType().includesTravel())) {
			// Processing Sunday, and prior Saturday was worked...
			// Note that the test above EXcludes Work/Travel and pure travel days.
			htg.lastWrap = htg.priorDailyTime.getWrap();
			if (restRule != null) {
				// Calculate earliest allowed call based on last Saturday's wrap
				htg.nextMinCall = htg.lastWrap.add(restRule.getMinimum());
			}
			// assume that the highest rate on Saturday was the last (prevailing) rate.
			htg.lastMult = findHighSaturdayRate(htg.priorDailyTime);
			if (htg.lastWrap.compareTo(Constants.HOURS_IN_A_DAY) > 0) {
				if (dt.getCallTime() != null &&
						htg.lastWrap.subtract(Constants.HOURS_IN_A_DAY).compareTo(dt.getCallTime()) > 0) {
					// Saturday's call time is on Sunday morning & later than Sunday's call time!
					TimecardService.error(htg, "Timecard.FillJob.CallTooEarly");
				}
			}
		}

		// ADJUST HOURS for special DAY TYPES
		BigDecimal dayTypeHours = null;
		if (dayType == DayType.HP) {
			dayTypeHours = holidayRule.getNotWorkedHours();
			if (weeklyRule.getCumeHours() != null) {
				// CUME (weekly, not on-call) schedule
//					dayTypeHours = holidayRule.getNotWorkedWeeklyRate().evaluate(weeklyRule.getCumeHours());
				// Does CUME schedule always get minimum call for Holiday Not Worked?
				dayTypeHours = htg.guarHours;
			}
		}
//		else if (dayType.isTours()) {
//			// Should not need this if we can use Rules instead. LS-1347
//			htg.hours = htg.defaultGuarHours;
//			dayTypeHours = htg.hours;
//		}
		else {
			dayTypeHours = dayType.getHours();
		}

		// Set initial worked hours
		if (htg.hours == null) { // was not set by SpecialRules processing
			if (dt.getWorked() || htg.nextDayIsWork) {
				htg.hours = BigDecimal.ZERO; // we'll fill in the amount later
			}
			else {
				htg.hours = dt.getHours(); // may be null if no hours reported (day not worked)
			}
		}

		// APPLY REST INVASION RULE...
		// This may increase payable hours; do this before we check minimum-call/guarantees
		htg.invadedRest = null;
		htg.callbackHours = null;
		BigDecimal minCallBack = BigDecimal.ZERO;
		if (restRule != null && // rule is only null for some JUnit tests
				restRule.getMinimum().signum() > 0 && // ignore if minimum rest = 0 (e.g., non-union)
				(! dt.getWorked()) &&	// skip for on-call (person checked "Worked")
				(! dayType.isTravel()) && // no rest invasion when work is followed by travel
				(dayType != DayType.TW) && // no rest invasion -- work followed by travel/work
				dt.getCallTime() != null && htg.hours != null) { // ensure some hours entered

			minCallBack = processRestRule(dt);

			htg.nextMinCall = dt.getWrap().add(restRule.getMinimum());
		}
		else { // day not worked, or checked "Worked"; no minimum call time for tomorrow
			htg.nextMinCall = BigDecimal.ZERO;
		}

		if (dayType.isTravel() && (dayType != DayType.TW)) {
			htg.lastWrap = BigDecimal.ZERO; // ignore wrap time if travel day other than Travel/Work
		}
		else if (dt.getWrap() == null) {
			htg.lastWrap = BigDecimal.ZERO;
		}
		else {
			htg.lastWrap = dt.getWrap();
		}

		// Figure out how many hours will be paid -- either worked, or guaranteed

		if (mpvRule != null && dt.getDayType() != null) {
			// May need to adjust hours if meal breaks were too long or too soon,
			// or not enough work hours after last meal.

			List<MpvRule> mpvRules = MpvRuleDAO.getInstance().findByRuleKey(mpvRule.getRuleKey());
			MpvRule rule = mpvRules.get(0);
			MpvRule rule2 = rule;
			//MpvRule rule3 = rule; // Could be rule3, but not used here
			if (mpvRules.size() > 1) {
				rule2 = mpvRules.get(1);
				//rule3 = rule2;
				//if (mpvRules.size() > 2) {
				//	rule3 = mpvRules.get(2);
				//}
			}
			boolean allowNdm = rule.getNdmAllowed();

			boolean mealNdm = false; // assume meal 1 is NOT a non-deductible meal
			BigDecimal mealLen = TimecardCalc.calculateMeal1Length(dt, allowNdm);
			if (mealLen != null) {
				BigDecimal amHours = TimecardCalc.calculateAmHours(dt);
				if (amHours != null && amHours.compareTo(rule.getMinTo1stMeal()) < 0) {
					// 1st meal too soon, which makes it non-deductible, so add it back to hours worked
					htg.hours = htg.hours.add(mealLen);
					htg.meal1AdjHours = mealLen;
					String date = new SimpleDateFormat("M/d").format(dt.getDate());
					if (rule.getMinTo1stMeal().compareTo(Constants.HOURS_IN_A_DAY) < 0) { // LS-2241
						// msg if "min to 1st meal" < 24 hours -- otherwise it's a "Pay meal" rule
						TimecardService.warn(htg, "Timecard.HTG.FirstMealTooSoon", date);
					}
					mealNdm = true;
				}
				if (( ! mealNdm) && mealLen.compareTo(rule.getMinMealLength()) < 0) {
					// 1st meal is too short -- does not count as a meal! Msg issued in MpvService
					htg.hours = htg.hours.add(mealLen);
					htg.meal1AdjHours = mealLen;
				}
				else if (( ! mealNdm) && mealLen.compareTo(rule.getMaxMealLength()) > 0) {
					// first meal exceeded max; add amount over max to hours worked; msg issued in MpvService
					htg.hours = htg.hours.add(mealLen).subtract(rule.getMaxMealLength());
					htg.meal1AdjHours = mealLen.subtract(rule.getMaxMealLength());
				}
			}
			mealNdm = false; // assume meal 2 is NOT a non-deductible meal.
			mealLen = TimecardCalc.calculateMeal2Length(dt, allowNdm);
			if (mealLen != null) {
				String date = new SimpleDateFormat("M/d").format(dt.getDate());
				BigDecimal pmHours = TimecardCalc.calculatePmHours(dt, rule2.getMaxMealLength(), allowNdm);
				if (pmHours != null && pmHours.compareTo(rule2.getMinTo2ndMeal()) < 0) {
					// 2nd meal too soon, which makes it non-deductible, so add it back to hours worked
					htg.hours = htg.hours.add(mealLen);
					htg.meal2AdjHours = mealLen;
					if (rule.getMinTo2ndMeal().compareTo(Constants.HOURS_IN_A_DAY) < 0) { // LS-2241
						// msg if "min to 2nd meal" < 24 hours -- otherwise it's a "Pay meal" rule
						TimecardService.warn(htg, "Timecard.HTG.SecondMealTooSoon", date);
					}
					mealNdm = true;
				}
				else if (rule2.getMinimumWorkToDeductMeal().compareTo(htg.hours) > 0) {
					// not enough hours worked to deduct second meal
					mealNdm = true;
					htg.hours = htg.hours.add(mealLen);
					htg.meal2AdjHours = mealLen;
					TimecardService.warn(htg, "Timecard.HTG.SecondMealNotAllowed", date);
				}
				else if (htg.isCommercial && dt.getNonDeductMeal2Payroll()) {
					// Commercial - no ndmStart or ndmEnd fields; assume check-mark is correct
					mealNdm = true;
					htg.hours = htg.hours.add(mealLen);
					htg.meal2AdjHours = mealLen;
				}
				else {
					mealNdm = TimecardCalc.meal2IsNdm(dt, rule2, htg.isCommercial);
				}
				if (( ! mealNdm) && mealLen.compareTo(rule2.getMinMealLength()) < 0) {
					// 2nd meal is too short -- does not count as a meal! msg issued in MpvService
					htg.hours = htg.hours.add(mealLen);
					htg.meal2AdjHours = mealLen;
				}
				else if (( ! mealNdm) && mealLen.compareTo(rule2.getMaxMealLength()) > 0) {
					// second meal exceeded max; add amount over max to hours worked; msg issued in MpvService
					htg.hours = htg.hours.add(mealLen).subtract(rule2.getMaxMealLength());
					htg.meal2AdjHours = mealLen.subtract(rule2.getMaxMealLength());
				}
			}
			if (rule2.getGuarHoursAfterMeal2().signum() > 0 && ! htg.production.getPayrollPref().getIgnoreGuarHrsAfterMeal2()) {
				BigDecimal wrapHours = TimecardCalc.calculateWrapHours(dt,
						rule2.getMaxMealLength(), allowNdm);
				if (wrapHours != null) { // if "wrap hours" is not null, there were 2 meals reported.
					// Check for minimum guaranteed hours after 2nd meal
					if (wrapHours.compareTo(rule2.getGuarHoursAfterMeal2()) < 0) {
						BigDecimal diff = rule2.getGuarHoursAfterMeal2().subtract(wrapHours);
						htg.paidWrap = htg.paidWrap.add(diff);
						BigDecimal am = TimecardCalc.calculateAmHours(dt);
						BigDecimal pm = TimecardCalc.calculatePmHours(dt,
								rule2.getMaxMealLength(), allowNdm);
						htg.hours = am.add(pm).add(rule2.getGuarHoursAfterMeal2());
					}
				}
			}
		}

		if (htg.weeklyTimecard.getMayUseOnCall() && htg.preference.getShowOnCallFields() && dt.getDayType() != null
				&& htg.hours != null && htg.guarHours.signum() > 0 && htg.hours.compareTo(htg.guarHours) < 0) {
			// Special for selected productions -- add "on-call" hours to reach non-union guarantee
			fillOnCallGuarantee(dt);
		}

		BigDecimal minCall = htg.guarHours.max(minCallBack);
		if (dayTypeHours != null && dayTypeHours.signum() == 0) {
			// let "minimum call" from GT rule override "DayType" defined hours ONLY IF zero
			dayTypeHours = dayTypeHours.max(minCall);
		}

		// CHECK DAY-TYPE MINIMUM HOURS
		if (htg.hours == null && dayTypeHours != null) {
			htg.hours = dayTypeHours; // nothing reported, use day type hours, e.g., TRAVEL-4, or SF hours
		}
		else if (dayTypeHours != null // else hours entered on TC, and some guarantee (from rules, SF, or DayTYpe)
				&& dt.getDayType().getWorkDay()) { // and it's a worked day (i.e., not PTO, etc)
			// LS-1242, for PTO, Sick, etc., if hours entered use them and NOT guarantee.
			htg.hours = htg.hours.max(dayTypeHours); // use higher of reported & day-type guarantee
		}

		// Enforce minimum call, if applicable
		htg.onlyStraight = false;
		if ((guaranteeRule.getOnlyStraight() &&
				guaranteeRule.getMinimumCall().compareTo(minCall) == 0) ||
				(! dt.getDayType().getWorkDay())) { // LS-1242 PTO etc. always pays as straight time
			if ((htg.isTeam && dt.getDayType().isTravel() && htg.nonUnion)) {
				// LS-1785 for Team, non-union: Travel also counts as work time and may cause OT (e.g., for > 40 hours)
			}
			else {
				htg.onlyStraight = true;
			}
		}
		if (dt.getWorked()) { // Exempt -- probably OnCall schedule
			htg.hours = minCall; // 'minCall' already includes guarantee from rule or StartForm
			if (htg.hours.signum() == 0) {
				// 2.9.5086 Employee checked Worked, but we never found hours.
				htg.hours = htg.exemptHours; // use default "Exempt" calculated hours
			}
		}
		else if ((dt.getCallTime() != null && dt.getHours() != null) ||
				dt.getDayType() == DayType.TR) {
			if (htg.hours == null) { // probably Travel day w/o hours entered
				htg.hours = BigDecimal.ZERO;
			}
			// enforce minimum call/guarantee if time reported, but not if, e.g., Idle4 or Trav7, etc.
			// do use guar min call for plain "Travel" dayType
			if (dt.getDayType().getWorkDay()) {  // Only enforce minCall if actual work day (not PTO, sick, etc). LS-1242
				htg.hours = htg.hours.max(minCall); // 'minCall' already includes guarantee from rule or StartForm
			}
			else if (htg.isModelRelease && dt.getDayType() == DayType.TR) { // LS-4664
				htg.hours = htg.hours.max(minCall); // allow Travel to have guaranteed hours for Model Release
			}
			// enforce MAXIMUM payable hours, e.g., for travel day
			htg.hours = htg.hours.min(guaranteeRule.getMaxHours());
		}
		else { // no hours reported and "worked" is not checked
			if (dayType.isIdle() || dayType.isTours()) {
				htg.hours = minCall; // 'minCall' already includes guarantee from rule or StartForm
			}
			else if (htg.nextDayIsWork) {
				htg.hours = minCall; // 'minCall' already includes guarantee from rule or StartForm
			}
		}
		htg.nextDayIsWork = false;

		// ANYTHING TO PAY TODAY?
		if (htg.hours == null || htg.hours.signum() == 0) { // no work or guarantee - skip it
			return true;
		}

		if (htg.invadedRest != null) {
			// Limit paying invaded rest time to the number of pay hours
			htg.invadedRest = htg.invadedRest.min(htg.hours);
		}

		log.debug("Breaking: orig hrs=" + dt.getHours() + ", pay hrs=" + htg.hours +
				", invaded=" + htg.invadedRest);

		// Have number of hours to pay - now split into straight / OT / Gold ...
		boolean ret = splitDay(dt);

		// Give SPECIAL rules a chance to run after Fill Jobs phase...
		ret &= htg.ruleService.getSpecialService().postPhase(dt, HtgPhase.FILL_JOBS, event);

		return ret;
	}

	/**
	 * Do all processing for a possible Rest-period invasion. This may update a
	 * number of fields in our HtgData instance, such as hours, lastGold, and
	 * lastMult.
	 *
	 * @param dt The DailyTime being processed.
	 * @return The minimum call-back period, if any. May be zero, but not null.
	 */
	private BigDecimal processRestRule(DailyTime dt) {
		boolean invaded = false;
		BigDecimal minCallBack = BigDecimal.ZERO;

		BigDecimal newCall = dt.getCallTime().add(Constants.HOURS_IN_A_DAY); // change to "yesterday's time" for checking rest
		if (newCall.compareTo(htg.nextMinCall) < 0) {
			invaded = true; // typical, next-day invasion
		}
		if (( ! htg.skipLongRest) && (restRule.getMinimumAfter5Days().signum() > 0 ||
				restRule.getMinimumAfter6Days().signum() > 0) &&
				dt.getWorkDayNum() < 4) { // invasion only possible in 1st 3 days; not applicable if "6th" or "7th" day.

			// Check for long rest period in between production weeks.

			if (htg.priorDailyTime != null && htg.priorDailyTime.reportedWork() &&
					htg.priorDailyTime.getWorkDayNum() > 0 &&
					(htg.priorDailyTime.getWorkDayNum() < dt.getWorkDayNum() ||
							(htg.priorDailyTime.getDayNum()+1)==dt.getDayNum())) {
				// today can't apply, since worked yesterday & it was earlier day in this production week.
				// (That is, yesterday might have invaded the long rest, but not today.)
				htg.skipLongRest = true; // don't test for "long" period invasion again this week
			}
			else {
				int lastWorkDay = calculatePriorLastWorkDayNum();
				// ...returns the last "workDayNum" of the prior production week.
				// Also set htg.lastWrap to time relative to start of Sunday a week ago.
				if (lastWorkDay > 0) {
					BigDecimal hoursSinceLastSun = Constants.HOURS_IN_A_DAY
							.multiply(new BigDecimal(6 + dt.getDayNum()));
					// convert today's call time into hour relative to a week ago Sunday.
					newCall = dt.getCallTime().add(hoursSinceLastSun);
					if (newCall.compareTo(htg.lastWrap) > 0) {
						// (if newCall < lastWrap, today is earlier in the week than the prod. week boundary; skip it.)
						if (lastWorkDay <= 5 && restRule.getMinimumAfter5Days().signum() > 0) {
							htg.nextMinCall = htg.lastWrap.add(restRule.getMinimumAfter5Days());
						}
						else if (lastWorkDay > 5 && restRule.getMinimumAfter6Days().signum() > 0) {
							htg.nextMinCall = htg.lastWrap.add(restRule.getMinimumAfter6Days());
						}
						else {
							htg.nextMinCall = BigDecimal.ZERO;
						}
						if (newCall.compareTo(htg.nextMinCall) < 0) {
							// there was a Rest invasion!
							invaded = true;
							htg.skipLongRest = true; // don't test for "long" period invasion again this week
						}
					}
				}
			}
		}
		if (invaded) {
			// there was a Rest invasion!
			dt.setForcedCall(true);
			htg.invadedRest = htg.nextMinCall.subtract(newCall);
			if (newCall.subtract(htg.lastWrap).compareTo(restRule.getLessIsWork()) < 0) {
				// Less than n (4) hours rest, all time counts as work time
				htg.callbackHours = newCall.subtract(htg.lastWrap);
				htg.hours = htg.hours.add(htg.callbackHours);
				if (htg.lastGold) { // prior period ended in golden time,
					htg.allGold = true; // so pay all continuing hours golden
				}
			}
			else {
				if (htg.priorDailyTime != null && htg.priorDailyTime.getWorkDayNum() == 7 &&
						htg.lastWrap.compareTo(Constants.HOURS_IN_A_DAY) > 0 ) {
					// prior day was 7th, and wrapped after midnight
					if (restRule.getGold7MidnightRate().signum() > 0) {
						// Rule has (probably) lower multiple to pay instead of 7th day gold
						htg.lastMult = htg.lastMult.min(restRule.getGold7MidnightRate());
					}
				}
				if (restRule.getForcedPayMethod() == ForcedPayMethod.PR && htg.lastGold) {
					// using "prevailing rate" for invaded time, and prior day ended golden
					htg.allGold = true;
				}
				else {
					if (callBackRule.getMinimumHours().signum() > 0) {
						minCallBack = callBackRule.getMinimumHours();
					}
					else if (callBackRule.getMinimumPercent().signum() > 0) {
						minCallBack = callBackRule.getMinimumPercent().multiply(htg.guarHours);
					}
					htg.lastMult = htg.lastMult.max(callBackRule.getRate());
				}
			}
		}
		return minCallBack;
	}

	/**
	 * Calculate the "workDayNumber" of the last day worked in the prior
	 * production week. It's really only important whether it was less than 6,
	 * or greater than or equal to 6. That is, whether the prior production week
	 * should be considered a "6-day week" or a "5-day week". Note that it
	 * doesn't really matter how many days were worked, but only whether the
	 * production week's normal days off were worked or not.
	 * <p>
	 * The method also sets htg.lastWrap to the last work day's wrap time,
	 * relative to the start of Sunday (12am) a week ago.
	 *
	 * @return the "workDayNumber" of the last day worked in the prior
	 *         production week
	 */
	private int calculatePriorLastWorkDayNum() {
		int lastDay = 0;
		BigDecimal wrap = null;
		DailyTime dt = null;

		int weekdayNum = htg.preference.getFirstWorkDayNum(); // week day number (1-7) of first production day
		weekdayNum--; // first prod. day of this week, origin 0
		int wrapAdjust = 7;

		List<DailyTime> dailys = htg.weeklyTimecard.getDailyTimes();

		for (int prodDay = 7; prodDay > 1; prodDay--) {
			weekdayNum--;
			if (weekdayNum < 0) {
				if (htg.priorTimecard != null) {
					htg.priorTcImportant = true; // last week's TC may affect this week's gross.
					dailys = htg.priorTimecard.getDailyTimes();
					weekdayNum += 7;
					wrapAdjust = 0;
				}
				else {
					dt = null;
					break;
				}
			}
			dt = dailys.get(weekdayNum);
			if (dt.reportedWork()) {
				lastDay = prodDay;
				break;
			}
		}

		wrap = BigDecimal.ZERO;
		if (dt != null) {
			// save most recent wrap prior to today
			wrap = dt.getWrap();
			if (wrap == null) { // no hours entered; may be "Worked" checked?
				wrap = Constants.HOURS_IN_A_DAY;
			}
			// convert to hours since last Sunday
			wrap = wrap.add(Constants.HOURS_IN_A_DAY.multiply(new BigDecimal(weekdayNum + wrapAdjust)));
		}
		htg.lastWrap = wrap;
		return lastDay;
	}

	/**
	 * Break the day's hours into straight, OT & Gold, and distribute among the
	 * job splits (if any). Upon entry, the {@link HtgData#hours} field has the
	 * total number of hours to be paid for the day.
	 *
	 * @param dt The DailyTime instance for the day being calculated.
	 * @return True if the job was successfully split; false means there was
	 *         some invalid data in the split information.
	 */
	private boolean splitDay(DailyTime dt) {
		int job = dt.getJobNum1() - 1; // PayJob (job table) number - changed to zero origin

		/* Check for issues which make a split invalid. */
		if (dt.getSplitStart2() != null || dt.getSplitStart3() != null) {
			String date = new SimpleDateFormat("M/d").format(dt.getDate());
			if (dt.getDayType() == DayType.HP  && ! dt.getSplitByPercent()) {
				TimecardService.errorTrace(htg, "Timecard.FillJob.SplitPaidHoliday", date);
				return false;
			}
			else if (htg.weeklyTimecard.getAllowWorked() && ! dt.getSplitByPercent()) {
				TimecardService.errorTrace(htg, "Timecard.FillJob.SplitOnCallHours", date);
				return false;
			}
		}

		fillAcctFields(job, dt); // initialize PayJob account code fields

		int workDayNum = dt.getWorkDayNum(); // production work day number
		int daynum = dt.getDayNum()-1; // day number (within timecard), origin zero

		// CHECK FOR WORK SPILLING OVER INTO A HOLIDAY
		htg.holidayOverlap = null;
		if (dt.getDayNum() < 7 && dt.getWrap() != null && dt.getWrap().compareTo(Constants.HOURS_IN_A_DAY) > 0 &&
				holidayRule.getOverlapApplies()) {
			// see if tomorrow is a paid or worked holiday
			DayType nextDayType = htg.weeklyTimecard.getDailyTimes().get(dt.getDayNum()).getDayType();
			if (nextDayType != null &&
					(nextDayType == DayType.HP || nextDayType == DayType.HW)) {
				// need to pay tomorrow's hours (hours past "24.0") at holiday rate
				htg.holidayOverlap = TimecardCalc.calculateWorkedHours(dt, Constants.HOURS_IN_A_DAY, dt.getWrap(), htg);
			}
		}

		StartForm sf = htg.weeklyTimecard.getStartForm();
		StartRateSet rateSet;
		if (sf != null) {
			if (sf.getHasPrepRates() && htg.production.getPayrollPref().getInPrep(dt.getDate())) {
				rateSet = sf.getPrep();
			}
			else {
				rateSet = sf.getProd();
			}
		}
		else {
			rateSet = new StartRateSet();
		}

		// Set default hours to reach gold break-point
		BigDecimal breakGold = goldenRule.getBreak1();
		BigDecimal flatDays = null; // number of flat "days pay"

		if (htg.otMult.signum() < 0) { // SpecialRule set otMult to number of DAYS of OT
			flatDays = htg.otMult; // save it here.
		}

		// DETERMINE MULTIPLIERS for Straight, OT, premium OT (AICP), and Gold
		htg.goldMult = goldenRule.getMultiplier(); // default Gold rate
		if (! htg.onlyStraight) {
			if (rateSet.getOt2Multiplier() != null && rateSet.getOt2AfterHours() != null &&
					rateSet.getOt2Multiplier().compareTo(TimecardService.MINIMUM_GOLD_RATE) >= 0) {
				// Second Start Form break/multiplier is for gold
				breakGold = breakGold.min(rateSet.getOt2AfterHours()); // smaller of rule's gold break & OT2 break
				htg.goldMult = htg.goldMult.max(rateSet.getOt2Multiplier()); // larger of rule's multiplier & OT2 mult.
			}
			else if (! NumberUtils.isEmpty(rateSet.getOt3AfterHours()) && ! NumberUtils.isEmpty(rateSet.getOt3Multiplier())) {
				// Third Start Form break/multiplier is for gold
				if (breakGold.compareTo(rateSet.getOt3AfterHours()) >= 0) { // SF breaks sooner than gold rule; use it. // LS-2412 '>=' not '>'
					breakGold = rateSet.getOt3AfterHours(); 	// smaller of rule's gold break & OT3 break
					htg.goldMult = htg.goldMult.max(rateSet.getOt3Multiplier()); // larger of rule's multiplier & OT3 mult; (ot3 probably higher)
				}
			}
			if (dt.getDayType() == DayType.HP && weeklyRule.getCumeHours() != null) {
				htg.strMult = BigDecimal.ONE;
			}
			else if (onCallRule != null) { // ON-CALL Schedules
				htg.strMult = onCallRule.getDays1to5Mult();
				htg.otMult = htg.strMult;
				if (workDayNum == 6) {
					htg.strMult = onCallRule.getDay6Mult();
				}
				else if (workDayNum == 7) {
					htg.strMult = onCallRule.getDay7Mult();
				}
				if (overtimeRule.getOvertimeType() != OvertimeType.H) {
					// Hourly overtime rules are ignored for OnCall schedules
					htg.otMult = overtimeRule.getOvertimeRate();
				}
			}
			else {
				// not "HolidayPaid" or OnCall - use Overtime rule
				// (as of 2.9.5590 - Holiday Worked also uses OT rule)
				htg.otMult = overtimeRule.getOvertimeRate();
				if (rateSet.getOt1Multiplier() != null) { // have Day Rate multiplier
					htg.otMult = htg.otMult.max(rateSet.getOt1Multiplier());
				}
				htg.ot2Mult = overtimeRule.getOvertimeRate2();
				if (rateSet.getOt2Multiplier() != null) {
					htg.ot2Mult = htg.ot2Mult.max(rateSet.getOt2Multiplier());
				}
				if (overtimeRule.getOvertimeType() != OvertimeType.H) {
					// Daily or Weekly multipliers - negate so PayBreakdown service will recognize it.
					htg.otMult = htg.otMult.negate();
					htg.ot2Mult = htg.ot2Mult.negate();
					if (flatDays == null) { // no daily OT yet
						flatDays = BigDecimal.ZERO; // indicates we're tracking DAYS of OT, not hours.
					}
					else { // Have Daily OT passed from SpecialRule
						htg.strHours = htg.defaultGuarHours; // don't want later code to add a day for normal time
					}
				}
				else if (flatDays != null) { // Have Daily OT passed from SpecialRule
					htg.otMult = flatDays; // put it back in otMult -- looks like "normal" Daily rule
					flatDays = BigDecimal.ZERO; // indicates we're tracking DAYS of OT, not hours.
					htg.strHours = htg.defaultGuarHours;
				}
			}
		}
		/** Last rate multiplier in effect for today, it will become "last rate" if needed for tomorrow's calculation.
		 * Default value is current straight-time rate (typically 1.0, may be higher if 6th/7th day). */
		BigDecimal newLastRate = htg.strMult;

		if (htg.remainToGold != null && htg.invadedRest != null) {
			if (restRule.getLessIsWork().signum() > 0) {
				// if rule uses the "less than x is work" parameter, then we
				// assume that time after invaded rest is added to prior day's time
				// to determine Gold break. This is usual in LA/IATSE and Videotape,
				// not usual in Commercial contracts.
				breakGold = breakGold.min(htg.remainToGold); // use smaller of existing (goldRule?) break & remainToGold
			}
		}
		htg.remainToGold = null;
		if (breakGold.signum() == 0) {
			// Gold starts "after 0 hours", i.e., all time is gold; typical for holidays & some 7th days.
			htg.allGold = true;
		}

		htg.strHours = BigDecimal.ZERO;
		htg.otHours = BigDecimal.ZERO;
		htg.ot2Hours = null;
		htg.goldHours = null;
		htg.npEligTime = dt.getWrap(); // night premium ending time

		calculateGoldEligibleHours(dt); // Determine number of hours eligible for each Gold level

		if (htg.onlyStraight) { // hours are to be paid solely as straight time
			htg.strHours = htg.hours;
		}
		else if (! dt.getWorked()) {
			/** Determine number of hours to pay as Golden */
			htg.lastGold = false;
			htg.strHours = htg.hours;
			if (htg.allGold) {
				htg.goldHours = htg.goldEligHours1;
				// Note - goldPaysMeals was already handled for this case.
				htg.strHours = BigDecimal.ZERO;
				newLastRate = htg.goldMult;
				//newLastRate = htg.lastMult; // rev 6521; don't bump up to new gold multiplier?
				htg.lastGold = true;
			}
			else if (htg.goldEligHours1.compareTo(breakGold) > 0) {
				htg.goldHours = htg.goldEligHours1.subtract(breakGold);
				if (htg.goldHoursType.isAllElapsed() && htg.goldPaysMeals) {
					// goldPaysMeals affected goldEligHours; make sure worked time is adjusted properly...
					// ... in relation to meals & goldBreak point. (rev 6520)
					BigDecimal endGoldElig = dt.getCallTime().add(htg.goldEligHours1); // end of gold eligible (time of day)
					BigDecimal endGoldBreak = dt.getCallTime().add(breakGold); // time of day when gold breaks
					// How many WORK hours (not elapsed) fall between goldBreak & end of gold eligible?
					BigDecimal goldWorkHours = TimecardCalc.calculateWorkedHours(dt, endGoldBreak, endGoldElig, htg);
					goldWorkHours = goldWorkHours.min(htg.goldHours); // It should not be more than goldHours.
					htg.strHours = htg.strHours.subtract(goldWorkHours).max(BigDecimal.ZERO); // reduce straight time by this much
				}
				else {
					// simpler case, just reduce straight time by paid gold hours, no need to adjust for meals
					htg.strHours = htg.strHours.subtract(htg.goldHours).max(BigDecimal.ZERO);
				}
				if (htg.goldPaysMeals && htg.goldHoursType.getBreak1() != HoursType.E) {
					// need to adjust gold hours if meal occurred during gold and not using Elapsed
					calculateGoldPaysMeal(dt);
				}
				newLastRate = htg.goldMult;
				htg.lastGold = true;
			}
			else {
				// person did not reach gold time yet; remember how many more
				// hours are needed to reach gold, in case tomorrow has invaded rest.
				htg.remainToGold = breakGold.subtract(htg.goldEligHours1);
			}
			if (htg.goldHours != null) {
				htg.goldHours = htg.goldHours.max(goldenRule.getMinHours());
			}

			// DETERMINE OVERTIME HOURS - check daily overtime first
			BigDecimal otBreak = overtimeRule.getOvertimeDailyBreak();
			if (rateSet.getOt1AfterHours() != null) {
				otBreak = otBreak.min(rateSet.getOt1AfterHours());
			}
			if (htg.strHours.compareTo(otBreak) > 0) {
				htg.otHours = htg.strHours.subtract(otBreak);
				htg.strHours = otBreak;
				newLastRate = newLastRate.max(htg.otMult);
				if (overtimeRule.getOvertimeType() == OvertimeType.DHR) {
					// round OT time up to next hour
					htg.otHours = htg.otHours.setScale(0, RoundingMode.UP);
				}
				if (flatDays != null) { // OT is in days
					if (flatDays.signum() == 0) { // No SpecialRule days
						flatDays = htg.otMult.abs(); // Multiplier is number of days, e.g., 0.5 or 1.0
						htg.otHours = htg.otHours.subtract(htg.defaultGuarHours);
					}
					else { // SpecialRule days, and maybe OT rule days - combine them
						flatDays = flatDays.abs().add(htg.otMult.abs()); // Multiplier is number of days, e.g., 0.5 or 1.0
						htg.otHours = BigDecimal.ZERO; // don't want any additional OT
					}
				}
			}

			// Check for second tier of overtime, from rules or "OT Rate table" in StartForm
			BigDecimal otBreak2 = overtimeRule.getOvertimeDailyBreak2();
			if (rateSet.getOt2AfterHours() != null) {
				if (rateSet.getOt2Multiplier() != null && rateSet.getOt2Multiplier().compareTo(TimecardService.MINIMUM_GOLD_RATE) < 0) {
					// only use OT2 break if it's NOT golden. If golden (>=2.0), it was handled earlier.
					otBreak2 = otBreak2.min(rateSet.getOt2AfterHours());
				}
			}
			BigDecimal ot1 = otBreak2.subtract(otBreak);
			if (htg.otHours.compareTo(ot1) > 0) {
				htg.ot2Hours = htg.otHours.subtract(ot1);
				htg.otHours = ot1;
				if (flatDays != null) {
					flatDays = flatDays.add(htg.ot2Mult.abs());
				}
				newLastRate = newLastRate.max(htg.ot2Mult);
			}

			// check daily overtime based on time of day (not hours worked)
			if (overtimeRule.getOvertimeBreakTime().signum() > 0 && dt.getCallTime() != null) {
				// Overtime starts at a certain time of day (typically used for Friday midnight rule)
				BigDecimal timeBrk = TimecardCalc.calculateWorkedHours(dt, overtimeRule.getOvertimeBreakTime(), htg);
				if (htg.strHours.compareTo(timeBrk) > 0) {
					htg.otHours = htg.otHours.add(htg.strHours.subtract(timeBrk));
					htg.strHours = timeBrk;
				}
			}

			// Check for weekly overtime - when week's straight time exceeds a limit
			if (htg.strHours.add(htg.weekStrHours).compareTo(weeklyRule.getOvertimeWeeklyBreak()) > 0) {
				BigDecimal extra = htg.strHours.add(htg.weekStrHours).subtract(weeklyRule.getOvertimeWeeklyBreak());
				htg.otHours = htg.otHours.add(extra);
				htg.strHours = htg.strHours.subtract(extra);
				htg.otMult = weeklyRule.getOvertimeRate(); // use weekly OT rate
			}

			if (htg.holidayOverlap != null) {
				BigDecimal overlap = htg.holidayOverlap;
				// End of the work-day overlapped into a holiday
				htg.holidayMult = holidayRule.getOverlapDailyRate();
				htg.holidayGoldMult = holidayRule.getOverlapGoldRate();
				if (htg.goldHours != null && htg.goldHours.signum() > 0) {
					if (overlap.compareTo(htg.goldHours) < 0) {
						// The hours within the holiday are less than the gold hours
						htg.holidayGoldHours = overlap;
						htg.goldHours = htg.goldHours.subtract(overlap);
						overlap = BigDecimal.ZERO; // none left over
					}
					else {
						// Gold less than holiday hours -- make it holiday gold
						htg.holidayGoldHours = htg.goldHours;
						// Calculate how many hours still to be paid in holiday time
						overlap = overlap.subtract(htg.goldHours);
						htg.goldHours = null;
					}
				}
				if (overlap.signum() > 0) {
					// holiday hours left (either original, or after subtracting gold)
					htg.holidayHours = overlap;	// they're all payable at holiday rate
					if (htg.goldPaysMeals) {
						BigDecimal mealLen;
						if (dt.getM2Out() != null && dt.getM2Out().compareTo(Constants.HOURS_IN_A_DAY) > 0) {
							mealLen = TimecardCalc.calculateMeal2Length(dt, false);
							htg.holidayHours = htg.holidayHours.add(mealLen);
						}
						if (dt.getM1Out() != null && dt.getM1Out().compareTo(Constants.HOURS_IN_A_DAY) > 0) {
							mealLen = TimecardCalc.calculateMeal1Length(dt, false);
							htg.holidayHours = htg.holidayHours.add(mealLen);
						}
					}
					if (overlap.compareTo(htg.otHours) < 0) {
						// not all of OT is in holiday - calc remaining OT hours (from original day)
						htg.otHours = htg.otHours.subtract(overlap);
					}
					else {
						// All of OT is within holiday day.
						// How much holiday time is currently assigned to straight time?
						overlap = overlap.subtract(htg.otHours); // Answer: this much.
						// Decrease straight time by remaining amount of holiday hours
						htg.strHours = htg.strHours.subtract(overlap);
						htg.otHours = BigDecimal.ZERO; // no OT left
					}
				}
			}
		}
		else {
			htg.strHours = htg.exemptHours;
		}

		if ((! guaranteeRule.getNotWeekly()) &&
				(dt.getDayType().getWorkDay() ||
					(htg.isTeam && htg.nonUnion && dt.getDayType().isTravel()))) { // LS-1785
			// Guarantee rule says hours do count towards weekly limit (! notWeekly);
			// and hours must be for "worked day", i.e., hours for PTO, Sick, etc. are not included. LS-1242
			// except for Team: non-union Travel also counts as work time LS-1785
			htg.weekStrHours = htg.weekStrHours.add(htg.strHours);
		}

		if (! htg.onlyStraight) {
			if (restRule != null && htg.invadedRest != null && htg.lastMult.compareTo(htg.otMult)==0) {
				// Rest invasion and prevailing rate is OT rate. If some or all time is paid at prevailing rate,
				// move the straight hours into OT hours, so Night Premium calculations will apply
				// as 1.5x NP instead of 1.0x NP, if night premium is applicable.
				if (restRule.getForcedPayMethod() == ForcedPayMethod.PR) { // all at prevailing
					htg.otHours = htg.otHours.add(htg.strHours);
					htg.strHours = BigDecimal.ZERO;
					htg.invadedRest = null;
				}
				else if (restRule.getForcedPayMethod() == ForcedPayMethod.PRI) { // invaded at prevailing
					if (htg.strHours.compareTo(htg.invadedRest) >= 0) {
						// invaded time was all within straight time (typical), move invaded hours to OT
						htg.otHours = htg.otHours.add(htg.invadedRest);
						htg.strHours = htg.strHours.subtract(htg.invadedRest); // and remove from straight
					}
					else { // invaded time greater than straight time! Make all straight time OT.
						htg.otHours = htg.otHours.add(htg.strHours);
						htg.strHours = BigDecimal.ZERO;
					}
					htg.invadedRest = null;
				}
			}
		}

		/* Calculate split percentages or hours for 2 or 3 splits; values are
		 * stored in transient DailyTime fields for use during Night Premium
		 * calculations and others.  */
		if (! calculateSplits(dt)) {
			return false;
		}

		if (! htg.onlyStraight) {
			// Night premium - calculate night premium hours, which may also
			// affect (reduce) straight and OT hours.
			if (! htg.ruleService.getSpecialService().prePhase(dt, HtgPhase.NIGHT_PREMIUM, event)) {
				return false;
			}
			BigDecimal lastNpRate = nightPremiumService.calculateNightPremium(dt, npRule);
			newLastRate = newLastRate.max(lastNpRate);
			if (! htg.ruleService.getSpecialService().postPhase(dt, HtgPhase.NIGHT_PREMIUM, event)) {
				return false;
			}
		}

		if (restRule != null) { // only null when called from some JUnit tests
			if (htg.invadedRest != null) {
				payInvadedRest(job, daynum);
			}
		}

		BigDecimal flatCustomMultiplier = CUSTOM_MULTIPLIER_DAILY; // if used, indicates Daily rate column
		if (htg.dailyFlat) { // Set by Special Rule
			// Replace 'normal' (guaranteed) hours with a flat 1-day's pay.
			BigDecimal hours = Constants.DECIMAL_EIGHT;
			if (overtimeRule != null) {
				hours = overtimeRule.getOvertimeDailyBreak();
			}
			if (htg.strHours.signum() > 0 || htg.otHours.signum() > 0) {
				// If any hours reported, add one day to flat-days total;
				// adjust straight & OT, as employee may have OT beyond guarantee.
				htg.strHours = htg.strHours.add(htg.otHours);
				if (htg.strHours.compareTo(hours) <= 0) {
					htg.otHours = BigDecimal.ZERO;
				}
				else {
					htg.otHours = htg.strHours.subtract(htg.defaultGuarHours).max(BigDecimal.ZERO);
				}
				htg.strHours = BigDecimal.ZERO;
				flatDays = NumberUtils.safeAdd(flatDays, BigDecimal.ONE);
			}
			if (dt.getDayType().isTours() && sf != null) { // get touring rates; LS-1347
				BigDecimal rate = sf.getToursRate(dt.getDayType());
				dt.setPayAmount(rate);
				flatCustomMultiplier = CUSTOM_MULTIPLIER_TOURS; // if used, indicates Touring rate column
			}
		}

		// Now take all the straight time, overtime, gold, holiday, and holiday gold
		// hours and place them in available columns in the appropriate PayJob`s.

		htg.otHours = NumberUtils.round(htg.otHours, htg.otRounding, RoundingMode.HALF_UP);
		htg.ot2Hours = NumberUtils.round(htg.ot2Hours, htg.otRounding, RoundingMode.HALF_UP);
		htg.goldHours = NumberUtils.round(htg.goldHours, htg.otRounding, RoundingMode.HALF_UP);

		boolean ret = true;

		if (dt.getSplitStart2() == null) { // no splits, all time goes into first job
			if (flatDays != null) {
				// Add flat days to Job table; negative multiplier will be recognized later as daily/tours.
				fillPayJobTime(job, daynum, flatDays, flatCustomMultiplier);
			}
			// put in straight, OT, and gold time
			if (htg.goldHours == null ||
					(goldenRule.getBreak2() == null && NumberUtils.isEmpty(rateSet.getOt3AfterHours()))) {
				// typical - no "2nd level gold" or employee has no gold
				if (htg.ot2Hours == null) { // no premium OT hours
					// MOST COMMON CASE - This takes care of all the hours -- straight, OT, and gold
					ret = fillPayJobTime(job, daynum, htg.strHours,
							htg.strMult, htg.otHours, htg.otMult, htg.goldHours, htg.goldMult);
				}
				else {
					// This takes care of straight, OT, and premium OT
					ret = fillPayJobTime(job, daynum, htg.strHours,
							htg.strMult, htg.otHours, htg.otMult, htg.ot2Hours, htg.ot2Mult);
					// ...and this does the gold.
					ret &= fillPayJobTime(job, daynum, htg.goldHours, htg.goldMult);
				}
			}
			else { // There's a possible 2nd-level gold, and the employee has gold hours
				// get straight, OT & premium OT taken care of first
				ret = fillPayJobTime(job, daynum, htg.strHours,
						htg.strMult, htg.otHours, htg.otMult, htg.ot2Hours, htg.ot2Mult);
				// how many hours (max) should be paid at first gold rate?
				// Diff between eligible 1 & 2 hours adjusts for case when one is Work & the other Elapsed
				BigDecimal goldPart;
				if (htg.allGold) {
					goldPart = goldenRule.getBreak1().min(htg.goldHours);
					ret &= fillPayJobTime(job, daynum, goldPart, htg.goldMult);
					// calculate remaining hours of gold to pay
					htg.goldHours = htg.goldHours.subtract(goldPart);
				}
				BigDecimal break2 = BigDecimal.ZERO;
				BigDecimal mult2 = goldenRule.getMultiplier2();
				if (mult2 == null) {
					mult2 = goldenRule.getMultiplier();
				}
				if (goldenRule.getBreak2() != null) {
					break2 = goldenRule.getBreak2().subtract(goldenRule.getBreak1());
				}
				if (! NumberUtils.isEmpty(rateSet.getOt2AfterHours()) && ! NumberUtils.isEmpty(rateSet.getOt3AfterHours())) {
					BigDecimal underGoldHours = htg.strHours.add(htg.otHours); // hours paid at less-than-gold rate
					break2 = rateSet.getOt3AfterHours().subtract(underGoldHours);
					// previously used break2 = (Ot3AfterHours - Ot2AfterHours), but this didn't work in
					// the case of a 7th day in CA, when gold started after 8 instead of after 12.
					mult2 = rateSet.getOt3Multiplier();
				}
				goldPart = htg.goldEligHours2.subtract(htg.goldEligHours1); // non-zero if gold 1 is worked & gold 2 is elapsed
				goldPart = break2.subtract(goldPart);
				if (htg.goldHours.compareTo(goldPart) <= 0) { // All gold fits in 1st gold rate
					ret &= fillPayJobTime(job, daynum, htg.goldHours, htg.goldMult);
				}
				else {
					// some gold extends past 1st gold rate; pay first hours at normal gold rate
					ret &= fillPayJobTime(job, daynum, goldPart, htg.goldMult);
					htg.goldHours = htg.goldHours.subtract(goldPart); // calculate remaining hours of gold to pay
					if (goldenRule.getBreak3() == null) { // no 3rd level gold
						// pay remaining hours at 2nd level gold
						ret = fillPayJobTime(job, daynum, htg.goldHours, mult2);
						newLastRate = newLastRate.max(mult2);
					}
					else { // 3rd level defined
						// how many hours (max) are paid at second gold rate?
						// Diff between eligible 2 & 3 hours adjusts for case when one is Work & the other Elapsed
						goldPart = htg.goldEligHours3.subtract(htg.goldEligHours2);
						goldPart = goldenRule.getBreak3().subtract(goldenRule.getBreak2()).subtract(goldPart);
						if (htg.goldHours.compareTo(goldPart) <= 0) { // Remaining gold fits in 2nd gold rate, so pay it
							ret &= fillPayJobTime(job, daynum, htg.goldHours, goldenRule.getMultiplier2());
							newLastRate = newLastRate.max(goldenRule.getMultiplier2()); // LS-4154
						}
						else { // remaining gold extends into 3rd level; pay "goldPart" at 2nd level rate
							ret &= fillPayJobTime(job, daynum, goldPart, goldenRule.getMultiplier2());
							htg.goldHours = htg.goldHours.subtract(goldPart); // calc remaining hours of gold
							// and pay remaining at 3rd gold rate
							ret &= fillPayJobTime(job, daynum, htg.goldHours, goldenRule.getMultiplier3());
							newLastRate = newLastRate.max(goldenRule.getMultiplier3());
						}
					}
				}
			}
			// put in holiday and holiday-Gold time, if any
			ret &= fillPayJobTime(job, daynum, htg.holidayHours, holidayRule.getOverlapDailyRate(),
					htg.holidayGoldHours, holidayRule.getOverlapGoldRate(), null, null);
		}
		else if (dt.getSplitByPercent()) { // split values are percentages, not times
			// Split each type of hours - straight, overtime, and gold...
			ret = splitPercent(dt, htg.strHours, htg.strMult);
			ret &= splitPercent(dt, htg.otHours, htg.otMult);
			ret &= splitPercent(dt, htg.ot2Hours, htg.ot2Mult);
			ret &= splitPercent(dt, htg.goldHours, htg.goldMult);
			// Split holiday hours (regular and gold)
			ret &= splitPercent(dt, htg.holidayHours, holidayRule.getOverlapDailyRate());
			ret &= splitPercent(dt, htg.holidayGoldHours, holidayRule.getOverlapGoldRate());
		}
		else { // split by time of day, not percentage
			ret = spreadTime(job, daynum, dt.getSplit1());
			ret &= spreadTime(dt.getJobNum2() - 1, daynum, dt.getSplit2());
			if (dt.getSplit3() != null) { // third job has time, so fill it
				ret &= spreadTime(dt.getJobNum3()-1, daynum, dt.getSplit3());
			}
		}
		htg.lastMult = newLastRate; // save for the next day's calculation
		return ret;
	}

	/**
	 * Determine the number of hours eligible for each Gold level.
	 *
	 * @param dt The DailyTime being processed.
	 */
	private void calculateGoldEligibleHours(DailyTime dt) {
		// Determine number of hours eligible for each Gold level
		if (htg.goldEligHours1 == null) { // (Not previously set by SpecialRule processing)
			if (htg.onlyStraight) {
				// Only paying straight time -- clear gold-eligible values
				htg.goldEligHours1 = BigDecimal.ZERO;
				htg.goldEligHours2 = BigDecimal.ZERO;
				htg.goldEligHours3 = BigDecimal.ZERO;
				htg.goldPaysMeals = false; // irrelevant
			}
			else {
				// Hours used to compute Gold may be Worked or Elapsed
				htg.goldEligHours1 = htg.hours; // assume worked (or guarantee)
				htg.goldEligHours2 = htg.hours; // ... for all gold breaks
				htg.goldEligHours3 = htg.hours; // ...
				BigDecimal minElapsedHours = TimecardCalc.calculateElapsedHours(dt);
				if (htg.callbackHours != null) {
					// Rest invasion; typically less than 4 hours break counts as work time
					minElapsedHours = minElapsedHours.add(htg.callbackHours);
				}
				// htg.hours may be larger than calculated elapsed if there was a
				// guarantee; use at least that value.
				minElapsedHours = minElapsedHours.max(htg.guarHours);

				if (htg.goldHoursType.getBreak1() == HoursType.E) {
					htg.goldEligHours1 = minElapsedHours;
				}
				if (htg.allGold && htg.goldPaysMeals) {
					// All hours are going to Gold, and Gold should pay meals,
					// ... so set eligible hours to elapsed hours and we're done.
					htg.goldEligHours1 = minElapsedHours;
					htg.goldEligHours2 = minElapsedHours;
					htg.goldEligHours3 = minElapsedHours;
				}
				else {
					if (htg.goldHoursType.getBreak2() == HoursType.E) {
						htg.goldEligHours2 = minElapsedHours;
					}
					if (htg.goldHoursType.getBreak3() == HoursType.E) {
						htg.goldEligHours3 = minElapsedHours;
					}
				}
			}
		}
		else {
			if (htg.goldEligHours2 == null) {
				// Special rule might set elig1, but not elig2 or elig3.  Rev 3.1.7702.
				htg.goldEligHours2 = htg.goldEligHours1;
				htg.goldEligHours3 = htg.goldEligHours1;
			}
		}
	}

	/**
	 * Adjust the number of gold hours to be paid, because "gold pays meals" is
	 * true, and gold has been calculated based on worked hours, so there may be
	 * meal periods that have to be added.
	 *
	 * @param dt The DailyTime being processed.
	 */
	private void calculateGoldPaysMeal(DailyTime dt) {
		if ((dt.getM1In() != null || dt.getM2In() != null) && htg.paidWrap != null) {
			// (Skip all this if no meals were reported or we have no wrap time.)
			BigDecimal goldStart = htg.paidWrap.subtract(htg.goldHours);
			BigDecimal mealLen;
			if (dt.getM2In() != null && dt.getM2Out() != null && dt.getM2In().compareTo(goldStart) > 0) {
				// Gold started before the end of meal 2
				mealLen = dt.getM2In().subtract(dt.getM2Out());
				htg.goldHours = htg.goldHours.add(mealLen); // add meal 2 to gold
				goldStart = goldStart.subtract(mealLen); // adjust effective start of gold time
			}
			if (dt.getM1In() != null && dt.getM1Out() != null && dt.getM1In().compareTo(goldStart) > 0) {
				// Gold started before the end of meal 1
				mealLen = dt.getM1In().subtract(dt.getM1Out());
				htg.goldHours = htg.goldHours.add(mealLen); // add meal 1 to gold
			}
		}
	}

	/**
	 * Add hours to job table for invaded rest periods.
	 * @param job The PayJob number being updated, origin zero.
	 * @param daynum The day number within the timecard (origin zero) which
	 *            determines the the PayJobDaily entry to be updated.
	 */
	private void payInvadedRest(int job, int daynum) {
		ForcedPayMethod payMethod = restRule.getForcedPayMethod();
		BigDecimal invadedHours = htg.invadedRest;
		switch (payMethod) {
			case PRI:
				// pays prevailing rate for INVADED hours
				fillPayJobTime(job, daynum, invadedHours, htg.lastMult);
				deductHours(invadedHours); // deduct invaded hours from time to be paid
				break;
			case PR: // Pay prevailing rate for ALL hours worked
				payAllHours(job, daynum,  htg.lastMult);
				break;
			case X1PI:
				// pays *additional* 1x base rate for INVADED hours
				fillPayJobTime(job, daynum, invadedHours, BigDecimal.ONE);
				break;

			case X15PI:  // pays *additional* ('P'=plus) 1.5x base rate for INVADED hours
			case X225PI: // pays *additional* ('P'=plus) 2.25x base rate for INVADED hours
			case X3PI:   // pays *additional* ('P'=plus) 3.0x base rate for INVADED hours
				fillPayJobTime(job, daynum, invadedHours, payMethod.getMultiplier());
				break;

			case R15I8R225I12R3X: // pays 1.5x for INVADED hours up to 8 hours, then 2.25x to 12, then 3X
				/* Pay nX for 'a' hours ('n' and 'a' from enum values), then 2nd
				 * rate for hours up to 12 (hard-coded) then 3x (hard-coded) for
				 * any hours over 12. Only two forced-pay methods use the extra
				 * values (second break point of 12 and third rate of 3x), so
				 * they're hard-coded here instead of adding them to all the enums. */
				payRestHours(job, daynum, payMethod, invadedHours, Constants.DECIMAL_12, Constants.DECIMAL_THREE);
				break;

			case R158R22512R3X:  // // pays 1.5x for WORKED hours up to 8 hours, then 2.25x to 12, then 3X
				payRestHours(job, daynum, payMethod, htg.hours, Constants.DECIMAL_12, Constants.DECIMAL_THREE);
				break;

			case X3PI5: // pays +3.0x per 1/2 hour for Invaded hours
				// round invaded hours up to next 1/2-hour increment
				invadedHours = NumberUtils.round(invadedHours, HourRoundingType.HALF, RoundingMode.HALF_UP);
				invadedHours = invadedHours.multiply(Constants.DECIMAL_TWO); // double hours, since paying 3x per 1/2 hour
				fillPayJobTime(job, daynum, invadedHours, payMethod.getMultiplier());
				break;

			case FXI5: // pays fixed amount per hour for Invaded hours, in 1/2-hour increments. LS-2241
				// Round invaded hours up to next 1/2-hour increment:
				invadedHours = NumberUtils.round(invadedHours, HourRoundingType.HALF, RoundingMode.HALF_UP);
				String contract = htg.ruleService.findRestRuleContract(event);  // extract contract code from contract rule in effect.
				BigDecimal rate = findJobRate(contract, "REST", "DEF"); // TODO Hard-coded "REST" for now, as FXI5 only used for Rest Invasion.
				// double time entry needed; if added as 1x, ignored by pay breakdown (due to exempt/OC nature of 399L)
				fillPayJobTime(job, daynum, invadedHours, rate, PayRateType.F);
				break;

			case X1P:
				// pays *additional* 1x base rate for ALL hours worked
				fillPayJobTime(job, daynum, htg.hours, BigDecimal.ONE);
				break;
			case X15P: 	// pays *additional* 1.5x base rate for ALL hours worked
			case X225P: // pays *additional* 2.25x base rate for ALL hours worked
			case X3P: 	// pays *additional* 3.0x base rate for ALL hours worked
				fillPayJobTime(job, daynum, htg.hours, payMethod.getMultiplier());
				break;
			case PRX1PI:
				// pays prevailing rate for INVADED hours plus 1X for invaded hours
				fillPayJobTime(job, daynum, invadedHours, htg.lastMult);
				// AND pays *additional* 1x base rate for INVADED hours
				fillPayJobTime(job, daynum, invadedHours, BigDecimal.ONE);
				deductHours(invadedHours); // deduct invaded hours from time to be paid
				break;
			case PRX1P:
				// Pay prevailing rate for ALL hours worked
				payAllHours(job, daynum,  htg.lastMult);
				// AND pays *additional* 1x base rate for ALL hours worked
				fillPayJobTime(job, daynum, htg.hours, BigDecimal.ONE);
				break;
			case R15I: // pays 1.5x
			case R20I: // pays 2.0x
			case R225I:// pays 2.25x
			case R25I: // pays 2.5x
			case R30I: // pays 3.0x
			case R33I: // pays 3.3x
				// pays given multiplier rate for INVADED hours
				fillPayJobTime(job, daynum, invadedHours, payMethod.getMultiplier());
				deductHours(invadedHours); // deduct invaded hours from time to be paid
				break;
			case X2I3PR:
				// pays +nx for 'a' hours, then 2nd rate which is multiplier times Prevailing rate.
				// Pays additional using first multiplier rate for INVADED hours up to "initialHours",
				// then second multiplier times the prevailing rate for any remaining INVADED hours.
				if (invadedHours.compareTo(payMethod.getInitialHours()) <= 0) {
					fillPayJobTime(job, daynum, invadedHours, payMethod.getMultiplier());
				}
				else {
					fillPayJobTime(job, daynum, payMethod.getInitialHours(), payMethod.getMultiplier());
					BigDecimal remainder = invadedHours.subtract(payMethod.getInitialHours());
					BigDecimal mult = htg.lastMult.multiply(payMethod.getMultiplier2()).setScale(2, RoundingMode.HALF_UP);
					fillPayJobTime(job, daynum, remainder, mult);
				}
				break;
			case R20: // pays 2.0x
			case R225:// pays 2.25x
			case R25: // pays 2.5x
			case R30: // pays 3.0x
			case R33: // pays 3.3x
			case R495: //pays 4.95x
			case R66: // pays 6.6x
				// Pay given multiplier for ALL hours worked
				payAllHours(job, daynum, payMethod.getMultiplier());
				break;
			case X05DP: // Pay additional 0.5x of the Daily rate
			case X10DP: // Pay additional 1.0x of the Daily rate
//LS-2241		if (onCallRule == null) {
				// Handle "daily" rest invasion calculation here (not in PayBreakdownService, as before). LS-2241
				fillPayJobTime(job, daynum, payMethod.getMultiplier(), BigDecimal.ONE.negate(), PayRateType.D);
				PayJob pj = htg.weeklyTimecard.getPayJobs().get(job);
				if (pj.getDailyRate() == null) {
					pj.setDailyRate(calculateDailyRate(pj));
				}
//LS-2241		}
				break;
			case X10D5P:  // Pay additional 1 day's pay for each 5 hour period worked
			case X10D6P:  // Pay additional 1 day's pay for each 6 hour period worked
				// Get actual worked hours, regardless of minimum call...
				BigDecimal hrs = htg.weeklyTimecard.getDailyTimes().get(daynum).getHours();
				BigDecimal days = hrs.divide(payMethod.getInitialHours(),0,RoundingMode.CEILING);
				// Note that 'multiplier' must be negative to be recognized as a Daily, not hourly, value.
				fillPayJobTime(job, daynum, days, payMethod.getMultiplier());
				break;
		}
	}

	/**
	 * @param job - the job (table) number (origin 0) being updated.
	 * @param daynum - the day number (origin 0) being updated.
	 * @param payMethod - the ForcedPayMethod in effect.
	 * @param hoursToPay - hours to be paid, usually either invaded hour or all
	 *            worked hours.
	 * @param break2 - second break point (in hours); must be larger than
	 *            payMethod.getInitialHours() (i.e., break point #1).
	 * @param rate3 - rate (multiplier) paid after 'break2' hours
	 */
	private void payRestHours(int job, int daynum, ForcedPayMethod payMethod, BigDecimal hoursToPay,
			BigDecimal break2, BigDecimal rate3) {
		if (hoursToPay.compareTo(payMethod.getInitialHours()) <= 0) { // fewer than 'break1' (e.g., 8) hours
			fillPayJobTime(job, daynum, hoursToPay, payMethod.getMultiplier());
		}
		else {
			fillPayJobTime(job, daynum, payMethod.getInitialHours(), payMethod.getMultiplier());
			if (hoursToPay.compareTo(break2) <= 0) { // less than or equal to 'break2' (e.g., 12) hours
				BigDecimal remainder = hoursToPay.subtract(payMethod.getInitialHours());
				fillPayJobTime(job, daynum, remainder, payMethod.getMultiplier2());
			}
			else {
				BigDecimal remainder = break2.subtract(payMethod.getInitialHours());
				fillPayJobTime(job, daynum, remainder, payMethod.getMultiplier2()); // hours for break1 to break2
				remainder = hoursToPay.subtract(break2);
				fillPayJobTime(job, daynum, remainder, rate3); // everything over "break2" hours
			}
		}
		deductHours(hoursToPay); // deduct hours paid here from time to be paid 'normally'
	}

	/**
	 * Pay all worked hours at the given rate, unless we reach a rate (e.g.,
	 * gold) that is higher than the given rate -- those hours we won't pay,
	 * we'll let them get processed later at the higher rate.
	 *
	 * @param job The PayJob number being updated, origin zero.
	 * @param daynum The day number within the timecard (origin zero) which
	 *            determines the the PayJobDaily entry to be updated.
	 * @param rate The rate at which to pay the hours.
	 */
	private void payAllHours(int job, int daynum, BigDecimal rate) {
		fillPayJobTime(job, daynum, htg.strHours, rate);
		htg.strHours = BigDecimal.ZERO; // taken care off, don't add it again.
		if (rate.compareTo(htg.otMult) > 0) {
			// prevailing rate is greater than OT rate, pay at prevailing
			fillPayJobTime(job, daynum, htg.otHours, rate);
			htg.otHours = BigDecimal.ZERO; // already paid
			if (rate.compareTo(htg.ot2Mult) > 0) {
				// prevailing rate is greater than gold rate, pay gold hours at prevailing
				fillPayJobTime(job, daynum, htg.ot2Hours, rate);
				htg.ot2Hours = BigDecimal.ZERO; // already paid
				if (rate.compareTo(htg.goldMult) > 0) {
					// prevailing rate is greater than gold rate, pay gold hours at prevailing
					fillPayJobTime(job, daynum, htg.goldHours, rate);
					htg.goldHours = BigDecimal.ZERO; // already paid
				}
			}
		}
	}

	/**
	 * Deduct the given number of hours from the straight, OT,
	 * and Gold hours ready to be paid.
	 * @param invadedRest
	 */
	private void deductHours(BigDecimal invadedRest) {
		if (htg.strHours.compareTo(invadedRest) >= 0) {
			htg.strHours = htg.strHours.subtract(invadedRest);
		}
		else {
			invadedRest = invadedRest.subtract(htg.strHours);
			htg.strHours = BigDecimal.ZERO;
			if (htg.otHours.compareTo(invadedRest) >= 0) {
				htg.otHours = htg.otHours.subtract(invadedRest);
			}
			else {
				invadedRest = invadedRest.subtract(htg.otHours);
				htg.otHours = BigDecimal.ZERO;
				if (htg.ot2Hours != null) {
					if (htg.ot2Hours.compareTo(invadedRest) >= 0) {
						htg.ot2Hours = htg.ot2Hours.subtract(invadedRest);
						invadedRest = BigDecimal.ZERO;
					}
					else {
						invadedRest = invadedRest.subtract(htg.ot2Hours);
						htg.ot2Hours = BigDecimal.ZERO;
					}
				}
				if (htg.goldHours != null) {
					if (htg.goldHours.compareTo(invadedRest) >= 0) {
						htg.goldHours = htg.goldHours.subtract(invadedRest);
					}
//					else {
//						invadedRest = invadedRest.subtract(htg.goldHours);
//					}
				}
			}
		}
	}

	/**
	 * Calculate and set the transient DailyTime split fields to either number
	 * of hours or percentages. For percentages, the user's split values are
	 * moved to the transient fields, except that the last split is calculated
	 * to guarantee the total is 100%. If split by time of day, the number of
	 * hours for each part of a split day is calculated, rounded and stored.
	 *
	 * @param dt The DailyTime entry for the day being split.
	 */
	private boolean calculateSplits(DailyTime dt) {
		BigDecimal split = dt.getSplitStart2();
		String date = new SimpleDateFormat("M/d").format(dt.getDate());
		if (split == null) { // no splits; disregard "by percent" setting
		}
		else if (dt.getSplitByPercent()) { // split values are percentages, not times
			dt.setSplit1(split);
			if ( dt.getSplitStart3() != null) { // it's a 3-way split
				dt.setSplit2(dt.getSplitStart3());
				dt.setSplit3(Constants.DECIMAL_100.subtract(split.add(dt.getSplitStart3())));
				if (dt.getSplit3().signum() < 0) {
					TimecardService.errorTrace(htg, "Timecard.FillJob.SplitPercentInvalid", date);
					return false;
				}
			}
			else {
				dt.setSplit2(Constants.DECIMAL_100.subtract(split));
				dt.setSplit3(null);
			}
		}
		else { // split by time of day, not percentage
			if (dt.getWorked()) {
				// no longer allowed (we shouldn't get here).
			}
			else { // split value entered is a time of day...
				split = TimecardCalc.calculateWorkedHours(dt, split, htg); // get hours for first job
				split = NumberUtils.round(split, htg.otRounding, RoundingMode.HALF_UP);
			}
			if (split.signum() < 0 || split.compareTo(htg.hours) > 0) {
				TimecardService.error(htg, "Timecard.FillJob.SplitOutside", date);
				return false;
			}
			dt.setSplit1(split); // save hours for first job

			BigDecimal splitLast = null; // used for 3rd job in a day, if specified
			if ( dt.getSplitStart3() != null) { // 3-way split
				BigDecimal split2;
				if (dt.getWorked()) { // split value entered IS the number of hours
					split2 = dt.getSplitStart3();
				}
				else { // split value entered is a time of day...
					split2 = TimecardCalc.calculateWorkedHours(dt, dt.getSplitStart3(), htg); // calc hours from call time to start-3
					split2 = NumberUtils.round(split2, htg.otRounding, RoundingMode.HALF_UP);
				}
				if (split2.signum() < 0 || split2.compareTo(htg.hours) > 0) {
					TimecardService.error(htg, "Timecard.FillJob.SplitOutside", date);
					return false;
				}
				splitLast = htg.hours.subtract(split2); // hours for last job
				split = split2.subtract(split); // hours for 2nd job
				if (split.signum() < 0 || split.compareTo(htg.hours) > 0) {
					TimecardService.error(htg, "Timecard.FillJob.SplitInvalid", date);
					return false;
				}
			}
			else {
				split = htg.hours.subtract(split); // hours for 2nd (last) job
			}
			dt.setSplit2(split);
			dt.setSplit3(splitLast);
		}
		return true;
	}

	/**
	 * Split up a number of hours into two or three jobs, based on percentages.
	 *
	 * @param dt The DailyTime for the day being split. The fields used from
	 *            this include
	 *            <ul>
	 *            <li>the weekday number being split -- used to access the
	 *            proper PayJobDaily entry.
	 *            <li>The job numbers (origin 0) of the PayJob`s to receive the
	 *            first, second, and third portions of time.
	 *            <li>split1 The percentage (from 0-99) of the time to be
	 *            assigned to the first job.
	 *            <li>split2 The percentage (from 0-99) of the time to be
	 *            assigned to the second job.
	 *            <li>split3 The percentage (from 0-99) of the time to be
	 *            assigned to the third job, or null if the time is only split
	 *            two ways.
	 *            </ul>
	 * @param hours The total number of hours to be split.
	 * @param mult The multiplier associated with these hours; this will
	 *            determine which "column" of the PayJobDaily the hours are
	 *            placed in (or added to).
	 */
	private boolean splitPercent(DailyTime dt, BigDecimal hours, BigDecimal mult) {
		boolean ret = true;
		int daynum = dt.getDayNum() - 1; // zero origin
		if (hours != null) {
			BigDecimal partHours;
			partHours = hours.multiply(dt.getSplit1()).divide(Constants.DECIMAL_100);
			partHours = NumberUtils.round(partHours, htg.otRounding, RoundingMode.HALF_UP);

			ret = fillPayJobTime(dt.getJobNum1()-1, daynum, partHours, mult);

			partHours = hours.subtract(partHours); // hours left after first job's portion.
			if (dt.getSplit3() != null) {
				// 3-way split
				BigDecimal partHours2 = hours.multiply(dt.getSplit2()).divide(Constants.DECIMAL_100);
				partHours2 = NumberUtils.round(partHours2, htg.otRounding, RoundingMode.HALF_UP);

				ret = fillPayJobTime(dt.getJobNum2()-1, daynum, partHours2, mult);

				partHours = partHours.subtract(partHours2); // hours left after first & second jobs

				ret = fillPayJobTime(dt.getJobNum3()-1, daynum, partHours, mult);
			}
			else {
				// 2-way split - 2nd job gets remainder from first
				ret = fillPayJobTime(dt.getJobNum2()-1, daynum, partHours, mult);
			}
		}
		return ret;
	}

	/**
	 * Spread up to a maximum number of hours across up to five columns
	 * (multipliers) within a single PayJobDaily entry. The distribution is
	 * based on the values in the current HtgData instance for the straight
	 * time, overtime, gold, holiday, and holiday-gold hours to be paid. These
	 * values will be decremented by the number of hours added to the
	 * PayJobDaily fields. Upon exit, the HtgData fields will contain the
	 * remaining number of hours (if any) to be distributed into other PayJob`s.
	 * The multipliers used are the HtgData strMult, otMult, goldMult,
	 * holidayMult, and holidayGoldMult fields.
	 *
	 * @param job The job number (index, origin 0 !) of the PayJob to be
	 *            updated. This is one less than the value the displayed and
	 *            entered on the timecard page.
	 * @param daynum The day number within the timecard (origin zero) which
	 *            determines the the PayJobDaily entry to be updated.
	 * @param hours The maximum number of hours to be added to the PayJobDaily
	 *            entry; these hours may be distributed across some combination
	 *            of straight time, overtime, gold, holiday, and holiday-gold,
	 *            but will not exceed the total of those five HtgData fields.
	 * @return False only if we cannot find a column in which to place one of
	 *         the hours. This only happens if, within one week, hours need to
	 *         be paid at more than 6 different "gold" rates (that is, rates
	 *         other than 1.0 and 1.5).
	 */
	private boolean spreadTime(int job, int daynum, BigDecimal hours) {
		if (hours == null) {
			return true;
		}
		boolean ret = true;
		BigDecimal split;
		if (hours.signum() > 0 && htg.strHours != null) {
			split = hours.min(htg.strHours);
			ret = fillPayJobTime(job, daynum, split, htg.strMult);
			hours = hours.subtract(split);
			htg.strHours = htg.strHours.subtract(split);
		}
		if (hours.signum() > 0 && htg.otHours != null) {
			split = hours.min(htg.otHours);
			ret = fillPayJobTime(job, daynum, split, htg.otMult);
			hours = hours.subtract(split);
			htg.otHours = htg.otHours.subtract(split);
		}
		if (hours.signum() > 0 && htg.ot2Hours != null) {
			split = hours.min(htg.ot2Hours);
			ret = fillPayJobTime(job, daynum, split, htg.ot2Mult);
			hours = hours.subtract(split);
			htg.ot2Hours = htg.ot2Hours.subtract(split);
		}
		if (hours.signum() > 0 && htg.goldHours != null) {
			split = hours.min(htg.goldHours);
			ret = fillPayJobTime(job, daynum, split, htg.goldMult);
			hours = hours.subtract(split);
			htg.goldHours = htg.goldHours.subtract(split);
		}
		if (hours.signum() > 0 && htg.holidayHours != null) {
			split = hours.min(htg.holidayHours);
			ret = fillPayJobTime(job, daynum, split, htg.holidayMult);
			hours = hours.subtract(split);
			htg.holidayHours = htg.holidayHours.subtract(split);
		}
		if (hours.signum() > 0 && htg.holidayGoldHours != null) {
			split = hours.min(htg.holidayGoldHours);
			ret = fillPayJobTime(job, daynum, split, htg.holidayGoldMult);
			hours = hours.subtract(split);
			htg.holidayGoldHours = htg.holidayGoldHours.subtract(split);
		}
		return ret;
	}

	/**
	 * Takes one to three hourly amounts with three corresponding multipliers,
	 * and adds them to the appropriate payJobDaily entries, based on the
	 * multipliers given. The three hours would typically be for straight time,
	 * OT, and gold.
	 *
	 * @param job The job number (index, origin 0 !) of the PayJob to be
	 *            updated. This is one less than the value the displayed and
	 *            entered on the timecard page.
	 * @param daynum The day number within the timecard (origin zero) which determines the
	 *            PayJobDaily instance to use as the source of the hours.
	 * @param hours1 The first hourly amount to add to the job table; if null or
	 *            zero, this entry is ignored.
	 * @param mult1 The rate at which 'hours1' will be added.
	 * @param hoursOt The second hourly amount to add to the job table; if null
	 *            or zero, this entry is ignored.
	 * @param mult2 The rate at which 'hoursOt' will be added.
	 * @param hoursGold The third hourly amount to add to the job table; if null
	 *            or zero, this entry is ignored.
	 * @param mult3 The rate at which 'hoursGold' will be added.
	 * @return False only if we cannot find a column in which to place one of
	 *         the hours. This only happens if, within one week, hours need to
	 *         be paid at more than 6 different "gold" rates (that is, rates
	 *         other than 1.0 and 1.5).
	 */
	private boolean fillPayJobTime(int job, int daynum,
			BigDecimal hours1, BigDecimal mult1, BigDecimal hoursOt, BigDecimal mult2,
			BigDecimal hoursGold, BigDecimal mult3) {
		boolean ret = true;
		checkPayJobsSize(job);
		PayJob pj = htg.weeklyTimecard.getPayJobs().get(job);
		PayJobDaily pd = pj.getPayJobDailies().get(daynum);

		if (hours1 != null && hours1.signum() != 0 && mult1.signum() != 0) {
			ret &= addHours(pj, pd, hours1, mult1, (mult1.signum() >= 0 ? OvertimeType.H : OvertimeType.DH));
		}
		if (hoursOt != null && hoursOt.signum() != 0) {
			ret &= addHours(pj, pd, hoursOt, mult2, overtimeRule.getOvertimeType());
		}
		if (hoursGold != null && hoursGold.signum() != 0) {
			ret &= addHours(pj, pd, hoursGold, mult3, OvertimeType.H);
		}
		return ret;
	}

	/**
	 * Takes an hourly amount with corresponding multiplier, and adds it to the
	 * appropriate payJobDaily entry, based on the multiplier given. This is a
	 * shortened form of
	 * {@link #fillPayJobTime(int, int, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal)}
	 *
	 * @param job The job number (index, origin 0 !) of the PayJob to be
	 *            updated. This is one less than the value the displayed and
	 *            entered on the timecard page.
	 * @param daynum The day number within the timecard (origin zero) which determines the
	 *            PayJobDaily instance to use as the source of the hours.
	 * @param hours1 The first hourly amount to add to the job table; if null or
	 *            zero, this entry is ignored.
	 * @param mult1 The multiplier rate at which 'hours1' will be added.
	 * @return False only if we cannot find a column in which to place one of
	 *         the hours. This only happens if, within one week, hours need to
	 *         be paid at more than 6 different "gold" rates (that is, rates
	 *         other than 1.0 and 1.5).
	 */
	private boolean fillPayJobTime(int job, int daynum,
			BigDecimal hours1, BigDecimal mult1) {
		return fillPayJobTime(job, daynum, hours1, mult1, null, null, null, null);
	}

	/**
	 * Takes an hourly amount with corresponding multiplier, and adds it to the
	 * appropriate payJobDaily entry, based on the multiplier and rate type
	 * given. NOTE: this method is designed specifically for the "fixed" rate
	 * type.  LS-2241
	 *
	 * @param job The job number (index, origin 0 !) of the PayJob to be
	 *            updated. This is one less than the value the displayed and
	 *            entered on the timecard page.
	 * @param daynum The day number within the timecard (origin zero) which
	 *            determines the PayJobDaily instance to use as the source of
	 *            the hours.
	 * @param hours1 The first hourly amount to add to the job table; if null or
	 *            zero, this entry is ignored.
	 * @param mult1 The multiplier rate at which 'hours1' will be added.
	 * @param type The type of rate - daily, weekly, fixed.
	 * @return False only if we cannot find a column in which to place one of
	 *         the hours. This only happens if, within one week, hours need to
	 *         be paid at more than 6 different "gold" rates (that is, rates
	 *         other than 1.0 and 1.5).
	 */
	private boolean fillPayJobTime(int job, int daynum,
			BigDecimal hours1, BigDecimal mult1, PayRateType type) {
		boolean ret = true;
		checkPayJobsSize(job);
		PayJob pj = htg.weeklyTimecard.getPayJobs().get(job);
		PayJobDaily pd = pj.getPayJobDailies().get(daynum);

		if (hours1 != null && hours1.signum() != 0 && mult1.signum() != 0) {
			ret &= addHours(pj, pd, hours1, mult1, type);
		}
		return ret;
	}

	/**
	 * Locates the appropriate column in the given PayJobDaily, and adds the
	 * given number of hours to that column. If no column already exists with a
	 * matching multiplier, an unused column will be selected and the multiplier
	 * assigned.
	 *
	 * @param pj The PayJob being updated.
	 * @param pd The PayJobDaily being updated.
	 * @param hours The number of hours to add to the appropriate entry.
	 * @param mult The rate (multiplier) assigned to these hours; it is this
	 *            rate that determines which column of the PayJobDaily the hours
	 *            are added to.
	 * @param overtimeType The type (rate basis) of overtime - Hourly, Daily, or
	 *            Weekly.
	 * @return False only if we cannot find a column in which to place the
	 *         hours.
	 */
	private boolean addHours(PayJob pj, PayJobDaily pd, BigDecimal hours, BigDecimal mult, OvertimeType overtimeType) {

		PayRateType rateType = PayRateType.H; // default is Hourly LS-2241
		if (overtimeType != OvertimeType.H) {
			if (mult.compareTo(CUSTOM_MULTIPLIER_TOURS) != 0) {
				// Daily and Weekly overtime rates are put into the PayJob as negative values, so they
				// can be detected by Auto-Pay (PayBreakdownService).
				if (mult.compareTo(BigDecimal.ONE) != 0) {
					// column value is converted to multiplier (fraction) of 1 day or 1 week.
					hours = hours.multiply(mult).abs(); // ensure positive "hours" LS-3054
					mult = BigDecimal.ONE;
				}
				mult = mult.negate();
			}
			rateType = PayRateType.D;
			if (overtimeType == OvertimeType.W) {
				rateType = PayRateType.W;
			}
		}
		// Note: GoldenRule multipliers for Daily rates are already stored as negative in
		// the rules, so we don't need to negate them here.

		if (mult.compareTo(BigDecimal.ONE) == 0) {
			// daily entry may have hours if split includes same job more than once (e.g., 3 way: job 1, job 2, job 1)
			hours = NumberUtils.safeAdd(hours, pd.getHours10());
			pd.setHours10(hours);
		}
		else if (mult.compareTo(Constants.DECIMAL_ONE_FIVE) == 0) {
			hours = NumberUtils.safeAdd(hours, pd.getHours15());
			pd.setHours15(hours);
		}
		else if (mult.compareTo(pj.getCustomMult1()) == 0) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust1());
			pd.setHoursCust1(hours);
		}
		else if (mult.compareTo(pj.getCustomMult2()) == 0) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust2());
			pd.setHoursCust2(hours);
		}
		else if (mult.compareTo(pj.getCustomMult3()) == 0) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust3());
			pd.setHoursCust3(hours);
		}
		else if (mult.compareTo(pj.getCustomMult4()) == 0) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust4());
			pd.setHoursCust4(hours);
		}
		else if (mult.compareTo(pj.getCustomMult5()) == 0) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust5());
			pd.setHoursCust5(hours);
		}
		else if (mult.compareTo(pj.getCustomMult6()) == 0) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust6());
			pd.setHoursCust6(hours);
		}
		else { // doesn't match any existing multipliers
			pj.calculateTotals(); // sum up job hours so we can find unused one
			boolean found = false;
			if (pj.getCustomMult6().signum() != 0 &&
					mult.compareTo(pj.getCustomMult6()) > 0 && pj.getTotalCust6().signum() == 0) {
				pj.setCustomMult6(mult);
				pj.setCustom6Type(rateType);
				found = true;
			}
			else if (pj.getCustomMult5().signum() != 0 &&
					mult.compareTo(pj.getCustomMult5()) > 0 && pj.getTotalCust5().signum() == 0) {
				pj.setCustomMult5(mult);
				pj.setCustom5Type(rateType);
				found = true;
			}
			else if (mult.compareTo(pj.getCustomMult4()) > 0 && pj.getTotalCust4().signum() == 0) {
				pj.setCustomMult4(mult);
				pj.setCustom4Type(rateType);
				found = true;
			}
			else if (mult.compareTo(pj.getCustomMult3()) > 0 && pj.getTotalCust3().signum() == 0) {
				pj.setCustomMult3(mult);
				pj.setCustom3Type(rateType);
				found = true;
			}
			else if (mult.compareTo(pj.getCustomMult2()) > 0 && pj.getTotalCust2().signum() == 0) {
				pj.setCustomMult2(mult);
				pj.setCustom2Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust1().signum() == 0) {
				pj.setCustomMult1(mult);
				pj.setCustom1Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust2().signum() == 0) {
				pj.setCustomMult2(mult);
				pj.setCustom2Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust3().signum() == 0) {
				pj.setCustomMult3(mult);
				pj.setCustom3Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust4().signum() == 0) {
				pj.setCustomMult4(mult);
				pj.setCustom4Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust5().signum() == 0) {
				pj.setCustomMult5(mult);
				pj.setCustom5Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust6().signum() == 0) {
				pj.setCustomMult6(mult);
				pj.setCustom6Type(rateType);
				found = true;
			}
			if (found) { // recursive call - this time 'mult' will match an entry
				addHours(pj, pd, hours, mult, OvertimeType.H);
			}
			else {
				TimecardService.errorTrace(htg, "Timecard.FillJob.TooManyRates");
				return false;
			}
		}
		return true;
	}

	/**
	 * Locates the appropriate column, including check for the PayRateType and
	 * rate ('amount'), in the given PayJobDaily, and adds the given number of
	 * hours to that column. If no column already exists with a matching
	 * multiplier, an unused column will be selected and the multiplier
	 * assigned. LS-2241
	 * <p>
	 * NOTE: this method is currently only used for the "fixed" rate type, and
	 * was written with that in mind!
	 *
	 * @param pj The PayJob being updated.
	 * @param pd The PayJobDaily being updated.
	 * @param hours The number of hours to add to the appropriate entry.
	 * @param amount The rate (amount!, not multiplier) assigned to these hours;
	 *            it is this rate that determines which column of the
	 *            PayJobDaily the hours are added to.
	 * @param rateType The PayRateType (rate basis) of the column. LS-2241
	 * @return False only if we cannot find a column in which to place the
	 *         hours.
	 */
	private boolean addHours(PayJob pj, PayJobDaily pd, BigDecimal hours, BigDecimal amount, PayRateType rateType) {

		if (amount.compareTo(pj.getCustomMult1()) == 0 && rateType == pj.getCustom1Type()) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust1());
			pd.setHoursCust1(hours);
		}
		else if (amount.compareTo(pj.getCustomMult2()) == 0 && rateType == pj.getCustom2Type()) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust2());
			pd.setHoursCust2(hours);
		}
		else if (amount.compareTo(pj.getCustomMult3()) == 0 && rateType == pj.getCustom3Type()) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust3());
			pd.setHoursCust3(hours);
		}
		else if (amount.compareTo(pj.getCustomMult4()) == 0 && rateType == pj.getCustom4Type()) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust4());
			pd.setHoursCust4(hours);
		}
		else if (amount.compareTo(pj.getCustomMult5()) == 0 && rateType == pj.getCustom5Type()) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust5());
			pd.setHoursCust5(hours);
		}
		else if (amount.compareTo(pj.getCustomMult6()) == 0 && rateType == pj.getCustom6Type()) {
			hours = NumberUtils.safeAdd(hours, pd.getHoursCust6());
			pd.setHoursCust6(hours);
		}
		else { // doesn't match any existing multipliers
			pj.calculateTotals(); // sum up job hours so we can find unused one
			boolean found = false;
			if (pj.getCustomMult6().signum() != 0 &&
					amount.compareTo(pj.getCustomMult6()) > 0 && pj.getTotalCust6().signum() == 0) {
				pj.setCustomMult6(amount);
				pj.setCustom6Type(rateType);
				found = true;
			}
			else if (pj.getCustomMult5().signum() != 0 &&
					amount.compareTo(pj.getCustomMult5()) > 0 && pj.getTotalCust5().signum() == 0) {
				pj.setCustomMult5(amount);
				pj.setCustom5Type(rateType);
				found = true;
			}
			else if (amount.compareTo(pj.getCustomMult4()) > 0 && pj.getTotalCust4().signum() == 0) {
				pj.setCustomMult4(amount);
				pj.setCustom4Type(rateType);
				found = true;
			}
			else if (amount.compareTo(pj.getCustomMult3()) > 0 && pj.getTotalCust3().signum() == 0) {
				pj.setCustomMult3(amount);
				pj.setCustom3Type(rateType);
				found = true;
			}
			else if (amount.compareTo(pj.getCustomMult2()) > 0 && pj.getTotalCust2().signum() == 0) {
				pj.setCustomMult2(amount);
				pj.setCustom2Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust1().signum() == 0) {
				pj.setCustomMult1(amount);
				pj.setCustom1Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust2().signum() == 0) {
				pj.setCustomMult2(amount);
				pj.setCustom2Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust3().signum() == 0) {
				pj.setCustomMult3(amount);
				pj.setCustom3Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust4().signum() == 0) {
				pj.setCustomMult4(amount);
				pj.setCustom4Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust5().signum() == 0) {
				pj.setCustomMult5(amount);
				pj.setCustom5Type(rateType);
				found = true;
			}
			else if (pj.getTotalCust6().signum() == 0) {
				pj.setCustomMult6(amount);
				pj.setCustom6Type(rateType);
				found = true;
			}
			if (found) { // recursive call - this time 'mult' will match an entry
				addHours(pj, pd, hours, amount, rateType);
			}
			else {
				TimecardService.errorTrace(htg, "Timecard.FillJob.TooManyRates");
				return false;
			}
		}
		return true;
	}

	/**
	 * Fill in the PayJob's Location code, Prod/Episode#, and Account-Set values
	 * from the given DailyTime entry, if they have not already been set.
	 *
	 * @param job The job number (index, origin 0 !) of the PayJob to be
	 *            updated. This is one less than the value the displayed and
	 *            entered on the timecard page.
	 * @param dt The DailyTime entry to use as the source for the account codes.
	 */
	private void fillAcctFields(int job, DailyTime dt) {
		checkPayJobsSize(job);
		if (job < htg.weeklyTimecard.getPayJobs().size()) {
			PayJob pj = htg.weeklyTimecard.getPayJobs().get(job);
			if (pj.getAccountLoc() == null || pj.getAccountLoc().length() == 0) {
				pj.setAccountLoc(dt.getAccountLoc());
			}
			if (pj.getAccountMajor() == null || pj.getAccountMajor().length() == 0) {
				pj.setAccountMajor(dt.getAccountMajor());
			}
			if (dt.getAccountSet() != null && (dt.getAccountSet().trim().length() > 0) &&
					(pj.getAccountSet() == null || pj.getAccountSet().equals(htg.weeklyTimecard.getAccountSet()))) {
				pj.setAccountSet(dt.getAccountSet());
			}
		}
	}

	/**
	 * Set the On-Call Start and End fields to add enough hours to meet the
	 * guarantee. The times are only set if they are currently null.
	 *
	 * @param dt The DailyTime entry to be updated.
	 */
	private void fillOnCallGuarantee(DailyTime dt) {
		if (dt.getDayType().getWorkDay() && ! dt.getDayType().isIdle()) {
			BigDecimal hours = htg.guarHours.subtract(htg.hours);
			boolean updated = false;
			if (dt.getOnCallStart() == null) {
				dt.setOnCallStart(dt.getWrap());
				updated = true;
			}
			if (dt.getOnCallEnd() == null) {
				hours = hours.add(dt.getOnCallStart());
				dt.setOnCallEnd(hours);
				updated = true;
			}
			if (updated) { // need to update "hours" column of DailyTime
				TimecardCalc.calculateDailyHours(dt);
			}
		}
	}

	/**
	 * Calculate and set the number of paid hours for each day in the week
	 * (DailyTime.paidHours), based on the contents of the PayJob(s).
	 */
	private void updateDailyPaidHours() {
		int ix = 0;
		BigDecimal weeklyHours = null;
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getDayType() != null) { // skip un-worked days
				for (PayJob pj : htg.weeklyTimecard.getPayJobs()) {
					PayJobDaily pjd = pj.getPayJobDailies().get(ix);
					dt.setPaidHours(NumberUtils.safeAdd(dt.getPaidHours(), pjd.getTotalHours()));
				}
			}
			ix++;
			weeklyHours = NumberUtils.safeAdd(weeklyHours, dt.getPaidHours());
		}
		htg.weeklyTimecard.setTotalPaidHours(weeklyHours);
	}

	/**
	 * Split the current PayJob based on either rate changes or episode number
	 * change (for TV/Feature) into multiple PayJob's, so that each resulting
	 * PayJob has a single rate and a single episode account code.
	 */
	private void autoJobSplit() {
		// First do splits based on rates
		StartForm nextSf = autoRateSplit();
		updateJobRates(nextSf); // make sure Rates in Pay Jobs match group

		if (! htg.production.getType().isAicp()) { // for non-commercial,
			// ... do additional splits, if any, for episode codes
			autoEpisodeSplit();
		}
	}

	/**
	 * If there is any job number assigned to two (or more) days where those
	 * days have different rates (e.g., Studio vs Distant), this method will
	 * split those jobs, by assigning new job numbers where necessary.
	 * <p>
	 * Other reasons for a rate split are (1) Prep vs Shooting and (2) a
	 * 'Change' StartForm with an effective date that falls on Monday through
	 * Saturday of the current week.
	 *
	 * @return The StartForm which has rates for the second part of the week.
	 *         This will be null if a single StartForm applies to the entire
	 *         week.
	 */
	private StartForm autoRateSplit() {
		/**
		 * We need to figure out if we have to split any jobs due to a change
		 * from Prep rates to Shooting rates occurring, or if some days are
		 * Distant and some Studio, and the rates are different, or if a second
		 * StartForm takes effect during the week. A complication is that we, or
		 * the user, may have already split the jobs to take care of these
		 * situations.
		 * <p>
		 * We can use a "job mask" to represent which job numbers are used on a
		 * particular day. Each '1' bit in the mask represents a job number in
		 * use for that day. Then we can test if two different days share any
		 * job number by ANDing the masks for the two days.
		 * <p>
		 * We partition the days into eight distinct (numbered) "groups":
		 *
		 * <pre>
		 *
		 * Rate A: Shoot+Studio(0), Shoot+Distant(1), Prep+Studio(2), Prep+Distant(3).
		 * Rate B: Shoot+Studio(4), Shoot+Distant(5), Prep+Studio(6), Prep+Distant(7).
		 *
		 * </pre>
		 * "Rate A" simply means it's the rate from the StartForm which created
		 * the timecard. "Rate B" means the rate from a "Change" StartForm that
		 * takes effect during the week. We'll generate a job mask for each
		 * "group", which is simply the sum (OR) of the masks for the days that
		 * belong to that group.
		 */

		// Only worry about Prep/Shoot distinction if employee has prep rates, and
		// the Prep-end-date fell during the current week.

		/** The last day of Prep (from payroll preferences); this will be null if
		 * the production has not set it, and we set prepEnd to null here if we
		 * can ignore the Prep/Shoot distinction for any other reason. */
		Date prepEnd = null;
		StartForm sf = htg.weeklyTimecard.getStartForm();

		// only need to check prep-end date if the employee has Prep rates.
		if (sf.getHasPrepRates()) {
			prepEnd = htg.preference.getPrepEndDate();
			// See if there's a Prep/Shooting date boundary in this week.
			if (prepEnd != null && prepEnd.before(htg.weeklyTimecard.getEndDate())) {
				// last day of Prep comes before last date on timecard (week-end date)
				Date start = htg.weeklyTimecard.getDailyTimes().get(0).getDate(); // first date in timecard
				if (start.after(prepEnd)) { // Prep ended before this week's start,
					prepEnd = null;	// so treat as if Prep doesn't exist
				}
			}
			else {	// No Prep, or it ends after this week ends, so...
				if (prepEnd != null) {	// entire week uses prep rates
					usingPrepRates = true;
				}
				prepEnd = null;	// no need for job split, so treat as if no Prep exists.
			}
		}

		/** The last date on which Sunday's rate applies. If the rate is the same for
		 * the whole week (typical), than we leave rateAEnd as null.  If the rate changes,
		 * for example, due to a contractual increase, then rateAEnd is the last day
		 * prior to the new rate ("Rate B") taking effect. */
		Date rateAEnd = null; // Assume no rate change during the week
		if (sf.getWorkEndDate() != null && sf.getWorkEndDate().before(htg.weeklyTimecard.getEndDate())) {
			rateAEnd = sf.getWorkEndDate();
		}
		if (sf.getEffectiveEndDate() != null && sf.getEffectiveEndDate().before(htg.weeklyTimecard.getEndDate())) {
			if (rateAEnd == null || sf.getEffectiveEndDate().before(rateAEnd)) {
				rateAEnd = sf.getEffectiveEndDate();
			}
		}
		StartForm nextSf = null;
		if (rateAEnd != null) {
			nextSf = StartFormDAO.getInstance().findOneByProperty(StartFormDAO.PRIOR_FORM_ID, sf.getId());
			if (nextSf == null) { // No "next" start,
				rateAEnd = null;  // ...so do NOT split based on effective end date.
			}
		}

		// Determine if Studio and Distant rates are different. If they are the same,
		// or if only one of them exists, then we will not split based on Studio/Distant zone.
		boolean splitZone = splitOnStudioDistant(sf);

		// Generate the job masks for each of the eight groups

		/** In each int mask, each '1' bit indicates a job number in use. The
		 * low-order bit (job "0") is not used. The bit turned on for job number
		 * 'n' is the one with value (2^n). There is a mask (an entry in the array)
		 * for each day that was worked. */
		int[] masks = {0,0,0,0,0,0,0,0};

		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getWorkDayNum() == 0 || dt.getDayType() == null) {
				continue; // not a work day
			}
			int ix = findGroupFromDailyTime(dt, prepEnd, sf.getHasPrepRates(), rateAEnd, splitZone);
			dt.setJobSplitGroup(ix);
			dt.setJobMask(createJobMask(dt));
			masks[ix] |= dt.getJobMask();
		}

		// Need to check overlap between all possible pairs of the 8 groups
		int overlap = 0;
		for (int i=0; i < masks.length-1; i++) {
			for (int j=i+1; j < masks.length; j++) {
				overlap |= (masks[i] & masks[j]);
			}
		}
		log.debug("overlap=" + overlap + ", masks="
				+ masks[0] + masks[1] + masks[2] + masks[3] + masks[4] + masks[5] + masks[6] + masks[7]);
		if (overlap == 0) {
			return nextSf; // no split!
		}

		// If we get here, we need to split one or more jobs

		// What is max job# in use? (we'll use next higher numbers as needed)
		int allMasks = 0;
		for (int i = 0; i < masks.length; i++ ) {
			allMasks |= masks[i]; // OR all the masks together
		}
		allMasks >>= 2;	// skip unused (0) bit and job (bit) 1 (which will always be "on")
		byte maxSrcJob = 1; // Some job number, 1 or higher, must be in use.
		while (allMasks > 0) {
			allMasks = allMasks >> 1;
			maxSrcJob++;
		}

		byte targetJob = maxSrcJob;
		Map<Integer, Byte> groupToJob = new HashMap<>();
		// For each job number that had a conflict (included in 'overlap'), split it
		for (byte srcJob = 1; srcJob <= maxSrcJob; srcJob++) {
			int srcMask = (1 << srcJob);
			if ((overlap & srcMask) > 0) {
				log.debug("split " + srcJob + " into " + targetJob);
				groupToJob.clear();
				// Find "group" of first day in week using this srcJob,
				// then find a different group using it and change the job.
				int firstIx = -1;
				for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
					if (firstIx < 0) { // still looking for first usage
						if ((srcMask & dt.getJobMask()) != 0) {
							firstIx = dt.getJobSplitGroup();
						}
					}
					else {
						if (firstIx != dt.getJobSplitGroup() && (srcMask & dt.getJobMask()) != 0) {
							// this day also uses the source job, and it's a different group,
							// so we need to assign it a different job number.
							int ix = dt.getJobSplitGroup();
							// see if this group already has a replacement job# assigned
							Byte useJob = groupToJob.get(ix);
							if (useJob == null) {
								// no new job # assigned yet for this group
								targetJob++;
								useJob = targetJob;
								groupToJob.put(ix, useJob);
							}
							replaceJob(dt, srcJob, useJob);
						}
					}
				}
				if (firstIx < 0) {
					log.error("job split error - unmatched job mask? - logic error");
					return nextSf;
				}
			}
		}
		log.debug(targetJob);
		return nextSf;
	}

	/**
	 * If there is any job number assigned to two (or more) days where those
	 * days have different episode account codes, this method will
	 * split those jobs and assign new job numbers where necessary.
	 */
	private void autoEpisodeSplit() {
		PayJob pj = htg.weeklyTimecard.getPayJobs().get(0);
		if (! StringUtils.isEmpty(pj.getAccountMajor()) && ! pj.getAccountMajor().equals(htg.weeklyTimecard.getAccountMajor())) {
			// first Job's account number has been changed already; skip auto-episode split.
			return;
		}

		Map<String,Byte> episodeMap = new HashMap<>();
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getWorkDayNum() == 0 || dt.getDayType() == null) {
				continue; // not a work day
			}
			autoEpisodeSplitDailyJob(dt, dt.getJobNum1(), episodeMap);
			if (dt.getSplitStart2() != null) {
				autoEpisodeSplitDailyJob(dt, dt.getJobNum2(), episodeMap);
			}
			if (dt.getSplitStart3() != null) {
				autoEpisodeSplitDailyJob(dt, dt.getJobNum3(), episodeMap);
			}
		}
	}

	/**
	 * If the given DailyTime has a different episode account code than the
	 * PayJob indicated by the 'jobNumber' parameter, then a new PayJob will be
	 * allocated, and assigned the given episode code. The DailyTime's job split
	 * information will be updated to reflect the new job number.
	 * <p>
	 * A Map is maintained for future reference, using the episode and original
	 * job number as the key, with the map value being the new job number
	 * assigned.
	 *
	 * @param dt The DailyTime whose episode code will be examined, and whose
	 *            split-job table may be updated. If the episode code is blank,
	 *            then the episode value from the current timecard will be used.
	 * @param jobNumber The job number (origin 1) of the Job table (PayJob)
	 *            whose episode code should match that of the given DailyTime.
	 */
	private void autoEpisodeSplitDailyJob(DailyTime dt, byte jobNumber, Map<String,Byte> episodeMap) {
		String episode = dt.getAccountMajor();
		if (StringUtils.isEmpty(episode)) {
			episode = htg.weeklyTimecard.getAccountMajor();
		}
		if (! StringUtils.isEmpty(episode)) {
			byte targetJob = (byte)htg.weeklyTimecard.getPayJobs().size();
			PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobNumber-1);
			if (StringUtils.isEmpty(pj.getAccountMajor())) {
				pj.setAccountMajor(episode);
			}
			else if (! pj.getAccountMajor().equals(episode)) {
				String key = episode + '\t' + jobNumber;
				Byte useJob = episodeMap.get(key); // do we already have a PayJob for this combination?
				if (useJob == null) { // no...
					useJob = ++targetJob; // allocate new PayJob for this episode
					episodeMap.put(key, useJob); // remember substitutions
					checkPayJobsSize(useJob-1);	// creates the new PayJob if necessary
					PayJob pjNew = htg.weeklyTimecard.getPayJobs().get(useJob-1);
					pjNew.setAccountMajor(episode);
				}
				replaceJob(dt, jobNumber, useJob);
			}
		}
	}

	/**
	 * Classify a day for the job-split code.
	 *
	 * @param dt The DailyTime representing the day to be evaluated.
	 * @param prepEnd The last day of Prep shooting, or null if Prep does not
	 *            exist (or is irrelevant).
	 * @param hasPrepRates True iff the Start Form corresponding to the timecard
	 *            being calculated has the "Has Prep rates" check-box checked.
	 * @param splitZone If true, the group index will be vary depending on if
	 *            the DailyTime is a Distant or Studio zone. If false, the group
	 *            index will be set as if the DailyTime is a Studio zone,
	 *            regardless of its actual setting.
	 * @return an index from 0 to 7 representing the "group" that the given
	 *         DailyTime object falls into:
	 *         <ul>
	 *         <li>0: Shoot+Studio (Rate A)
	 *         <li>1: Shoot+Distant (Rate A)
	 *         <li>2: Prep+Studio (Rate A)
	 *         <li>3: Prep+Distant (Rate A)
	 *         <li>4: Shoot+Studio (Rate B)
	 *         <li>5: Shoot+Distant (Rate B)
	 *         <li>6: Prep+Studio (Rate B)
	 *         <li>7: Prep+Distant (Rate B)
	 *         </ul>
	 *         Note that if 'splitZone' is false, only even values (Studio) are
	 *         returned.
	 */
	private int findGroupFromDailyTime(DailyTime dt, Date prepEnd, boolean hasPrepRates, Date rateAEnd, boolean splitZone) {
		int ix = 0;
		if (splitZone && findStudioLoc(dt).isDistant()) {
			ix = 1;
		}
		if ((prepEnd != null) && (! dt.getDate().after(prepEnd))) {
			ix += 2; // ix becomes 2 or 3
		}
		else if (hasPrepRates && (dt.getDayType() == DayType.PR || dt.getDayType() == DayType.WD || dt.getPhase() == ProductionPhase.P)) {
			// For DayType=Prep, or phase=Prep, split only if the Start includes Prep Rates
			ix += 2; // ix becomes 2 or 3
		}
		if (rateAEnd != null) {
			if (dt.getDate().after(rateAEnd)) {
				ix += 4; // ix becomes 4...7
			}
		}
		return ix;
	}

	/**
	 * Determine if a day's DayType should be considered Studio or Distant,
	 * based on the DayType and also based on the current timecard's
	 * Studio/Distant setting, if necessary.
	 *
	 * @param dt The DailyTime to be tested.
	 * @return The WorkZone applicable to the given DailyTime entry.
	 */
	private WorkZone findStudioLoc(DailyTime dt) {
		WorkZone wz = dt.getWorkZone();
		if (wz == WorkZone.N_A || wz == null) {
			wz = htg.weeklyTimecard.getDefaultZone();
		}
		return wz;
	}

	/**
	 * Create a mask of bits reflecting the job numbers that are referenced by
	 * the given DailyTime instance.
	 *
	 * @param dt The DailyTime to be evaluated.
	 * @return An int in which each '1' bit indicates a job number in use. The
	 *         low-order bit (job "0") is not used. The bit turned on for job
	 *         number 'n' is the one with value (2^n).
	 */
	private int createJobMask(DailyTime dt) {
		int mask = 0;
		if (dt.getJobNum1() != 0) {
			mask = 1 << dt.getJobNum1();
		}
		if (dt.getJobNum2() != 0 && dt.getSplitStart2() != null) {
			mask |= 1 << dt.getJobNum2();
		}
		if (dt.getJobNum3() != 0 && dt.getSplitStart3() != null) {
			mask |= 1 << dt.getJobNum3();
		}
		return mask;
	}

	/**
	 * Determine if studio and distant rates are both specified and different in
	 * the given Start Form.
	 *
	 * @param sf The Start Form to check.
	 * @return True iff both Studio and Distant (Location) rates are specified
	 *         and are not equal. Prep rates are checked if "prep" is in effect
	 *         during the week covered by the current timecard. Both Prep and
	 *         Prod rates will be checked if the Prep period ends during the
	 *         week.
	 */
	private boolean splitOnStudioDistant(StartForm sf) {
		boolean ret = false;
		ProdOrPrep type = getProdOrPrep();
		switch (type) {
			case PROD:
				ret = splitOnStudioDistantRate(sf, sf.getProd()); // Is split necessary due to Prod rates?
				break;
			case PREP:
				ret = splitOnStudioDistantRate(sf, sf.getPrep()); // Is split necessary due to Prep rates?
				break;
			case MIXED:
			case SPLIT:
				ret = splitOnStudioDistantRate(sf, sf.getProd()); // Is split necessary due to Prod rates?
				ret |= splitOnStudioDistantRate(sf, sf.getPrep()); // or prep rates?
				break;
		}
		return ret;
	}

	/**
	 * Determine if the current weeklyTimecard only uses Prod rates, or only
	 * Prep rates, or a mix of the two.
	 *
	 * @param sf The Start Form to check.
	 * @return The appropriate ProdOrPrep enum value; for either PREP or MIXED
	 *         to be returned, the StartForm must have the "Uses prep rates" box
	 *         checked. The timecard may be determined to use Prep rates based
	 *         on the Prep End Date (on Payroll Preferences tab), or the
	 *         occurrence of a DayType of Prep, or a phase of Prep (for
	 *         Commercial timecards).
	 */
	private ProdOrPrep calculateProdOrPrep(StartForm sf) {
		ProdOrPrep ret = ProdOrPrep.PROD;
		Date prepEnd;
		if (sf.getHasPrepRates()) {
			if ((prepEnd = htg.preference.getPrepEndDate()) != null) {
				// See if there's a Prep/Shooting date boundary in this week.
				if (prepEnd.before(htg.weeklyTimecard.getEndDate())) {
					// last day of Prep comes before last date on timecard (week-end date); either all Prod or mixed.
					Date start = htg.weeklyTimecard.getDailyTimes().get(0).getDate(); // first date in timecard
					if (! start.after(prepEnd)) { // Prep ended during this week (e.g., Sun thru Fri)
						ret = ProdOrPrep.SPLIT;
					}
					else { // Prep ended before start of week, all time is Prod rates
						// 'ret' set to ProdOrPrep.PROD above
					}
				}
				else {	// it ends after this week ends, so whole week is Prep
					ret = ProdOrPrep.PREP;
				}
			}
			if (ret == ProdOrPrep.PROD) { // see if Prep specified via DayType or phase (Commercial)
				for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
					if (DayType.PR.equals(dt.getDayType()) || DayType.WD.equals(dt.getDayType()) ||
							ProductionPhase.P.equals(dt.getPhase())) {
						ret = ProdOrPrep.MIXED;
						break;
					}
				}
			}
		}
		return ret;
	}

	private boolean splitOnStudioDistantRate(StartForm sf, StartRateSet rate) {
		// Get the right group of rates: hourly/daily/weekly
		RateHoursGroup group;
		switch (sf.getRateType()) {
			case DAILY:
				group = rate.getDaily();
				break;
			default:
			case HOURLY:
				group = rate.getHourly();
				break;
			case WEEKLY:
				group = rate.getWeekly();
				break;
		}
		boolean ret = false;
		if (group.getStudio() != null && group.getLoc() != null &&
				(group.getStudio().compareTo(group.getLoc()) != 0)) {
			// both studio & distant rates exist, and not the same
			ret = true; // so we should split based on zone
		}
		return ret;
	}

	/**
	 * Get the appropriate StartRateSet from the given StartForm based on
	 * whether Prep rates exist, and if the timecard falls in the Prep period.
	 *
	 * @param sf The StartForm of interest.
	 * @param dt The DailyTime of interest; this may have an effect if the
	 *            PayrollPreference indicates a Prep End date that falls mid-week.
	 * @return Either the production or prep StartRateSet.
	 */
	private StartRateSet findProdPrepRate(StartForm sf, DailyTime dt) {
		StartRateSet rate = sf.getProd(); // Assume we're using production (not prep) rates.
		ProdOrPrep type = getProdOrPrep();
		rate = sf.getProd();
		switch (type) {
			case PROD:
				break;
			case PREP:
				rate = sf.getPrep();
				break;
			case SPLIT: // Prep end date falls during the week
				if (! htg.preference.getPrepEndDate().before(dt.getDate())) {
					rate = sf.getPrep();
				}
				// fall through to check dayType and phase...
			case MIXED:
				if (DayType.PR.equals(dt.getDayType()) || DayType.WD.equals(dt.getDayType()) ||  ProductionPhase.P.equals(dt.getPhase())) {
					rate = sf.getPrep();
				}
				break;
		}
		return rate;
	}

	/**
	 * Iterate through the DailyTime entries and update any PayJob entries that
	 * are referred to by the existing daily job-split entries to make sure they
	 * have the correct rates for that day's "group", e.g., distant vs studio.
	 *
	 * @param nextSf The StartForm for any "Rate B" entries, that is, jobs
	 *            assigned to dates beyond the effective date of the timecard's
	 *            assigned StartForm.
	 */
	private void updateJobRates(StartForm nextSf) {
		WeeklyTimecard emptyTc = new WeeklyTimecard();
		emptyTc.setEndDate(htg.weeklyTimecard.getEndDate());
		StartForm sf = htg.weeklyTimecard.getStartForm();
		sf = StartFormDAO.getInstance().refresh(sf);

		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.getWorkDayNum() == 0 || dt.getDayType() == null) {
				continue; // not a work day
			}
			int ix = dt.getJobSplitGroup();

			if (dt.getJobNum1() != 0) {
				updateJobRate(ix, dt.getJobNum1(), emptyTc, sf, nextSf);
			}
			if (dt.getJobNum2() != 0 && dt.getSplitStart2() != null) {
				updateJobRate(ix, dt.getJobNum2(), emptyTc, sf, nextSf);
			}
			if (dt.getJobNum3() != 0 && dt.getSplitStart3() != null) {
				updateJobRate(ix, dt.getJobNum3(), emptyTc, sf, nextSf);
			}
		}

	}

	/**
	 * Update a single PayJob entry to make sure it's rates match the specified
	 * group.
	 *
	 * @param ix The group index (0-7) that indicates which set of rates to use
	 *            (e.g., distant vs studio).
	 * @param jobNum The job number (origin 1) of the PayJob to be created or
	 *            updated.
	 * @param emptyTc An empty WeeklyTimecard that can be used for a fillRates
	 *            call; it just needs to have the week-ending date set.
	 * @param sf The related StartForm to use as the source for the rates.
	 * @param nextSf The StartForm for any "Rate B" entries, that is, jobs
	 *            assigned to dates beyond the effective date of the timecard's
	 *            assigned StartForm.
	 */
	private void updateJobRate(int ix, byte jobNum, WeeklyTimecard emptyTc, StartForm sf, StartForm nextSf) {

		if (ix > 3 && nextSf != null) {
			// ix 4-7 are "Rate B" (e.g., after contractual or other rate increase)
			sf = nextSf;
		}

		boolean prep = (ix == 2 || ix == 3 || ix == 6 || ix == 7); // True if we split based on Prep rates
		if (! prep) {
			// maybe we didn't split on Prep, because prep was in effect for whole week
			prep = usingPrepRates;
		}

		WorkZone zone = WorkZone.SL;
		if (ix == 1 || ix == 3 || ix == 5 || ix == 7) { // Distant job groups
			zone = WorkZone.DL;
		}
		else if (htg.weeklyTimecard.getRateType().equals(StartForm.USE_LOCATION_RATE)) {
			// maybe we didn't split on Distant, because rates were identical
			zone = WorkZone.DL;
		}

		// Get the appropriate rates from the selected StartForm
		TimecardUtils.fillRates(emptyTc, sf, htg.production, zone, prep);

		// Find/create the PayJob to be updated
		jobNum--;
		checkPayJobsSize(jobNum);	// creates the new PayJob if necessary
		PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobNum);

		// Update rates
		pj.setRate(emptyTc.getHourlyRate());
		pj.setDailyRate(emptyTc.getDailyRate());
		pj.setWeeklyRate(emptyTc.getWeeklyRate());

	}

	/**
	 * Update a single day's splits, replacing any use of 'srcJob' with
	 * 'targetJob', for any of job's 1, 2 or 3 used in the split table.
	 *
	 * @param dt The DailyTime instance whose job-split entries are to be
	 *            updated.
	 * @param srcJob The job number which, if found, will be replaced.
	 * @param targetJob The replacement job number.
	 */
	private void replaceJob(DailyTime dt, byte srcJob, byte targetJob) {
		if (dt.getJobNum1() == srcJob) {
			dt.setJobNum1(targetJob);
		}
		if (dt.getJobNum2() == srcJob && dt.getSplitStart2() != null) {
			dt.setJobNum2(targetJob);
		}
		if (dt.getJobNum3() == srcJob && dt.getSplitStart3() != null) {
			dt.setJobNum3(targetJob);
		}
	}

	/**
	 * Determine which PayJob table should get any "AM" MPVs.
	 * <ul>
	 * <li>If there's only one job referenced for the day, that one is returned
	 * (duh).
	 * <li>Otherwise, the job that "contains" the first meal's out-time (start)
	 * is returned.
	 * </ul>
	 *
	 * @param dt The DailyTime for the day being calculated.
	 * @return One of the PayJob entries in the given DailyTime, as described
	 *         above.
	 */
	private PayJob findPayJobForAmMpv(DailyTime dt) {
		int jobNum = 1;
		if (htg.weeklyTimecard.getPayJobs().size() > 1) {
			BigDecimal split = dt.getSplitStart2();
			if (split == null) {
				jobNum = dt.getJobNum1(); // MPVs go to first (only) job listed.
			}
			else {
				if (dt.getSplitByPercent()) {
					split = dt.getHours().multiply(split).divide(Constants.DECIMAL_100);
					split = split.add(dt.getCallTime());
				}
				if (dt.getM1Out() == null || split.compareTo(dt.getM1Out()) >= 0) {
					// split time is after start of first meal,
					jobNum = dt.getJobNum1(); // so MPVs go to first job listed.
				}
				else {
					jobNum = dt.getJobNum2(); // assume it goes to second job entry
					if (dt.getSplitStart3() != null) {
						if (dt.getSplitByPercent()) {
							split = split.add(dt.getHours().multiply(dt.getSplitStart3()).divide(Constants.DECIMAL_100));
						}
						else {
							split = dt.getSplitStart3();
						}
						if (split.compareTo(dt.getM1Out()) <= 0) {
							// split time between 2nd & 3rd jobs is before 1st meal,
							jobNum = dt.getJobNum3(); // assign MPVs to 3rd job.
						}
					}
				}
			}
		}
		PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobNum-1);
		return pj;
	}

	/**
	 * Determine which PayJob table should get any "PM" MPVs.
	 * <ul>
	 * <li>If there's only one job referenced for the day, that one is returned
	 * (duh).
	 * <li>If there are two jobs referenced, the later one is returned.
	 * <li>If there are three jobs referenced, and there is no second meal, the
	 * third one is returned.
	 * <li>If there are three jobs and a second meal, then the job that
	 * "contains" the second meal's out-time (start) is returned.
	 * </ul>
	 *
	 * @param dt The DailyTime for the day being calculated.
	 * @return One of the PayJob entries in the given DailyTime, as described
	 *         above.
	 */
	private PayJob findPayJobForPmMpv(DailyTime dt) {
		int jobNum = 1;
		if (htg.weeklyTimecard.getPayJobs().size() > 1) {
			if (dt.getSplitStart2() == null) { // no splits
				jobNum = dt.getJobNum1(); // so MPVs go to first (only) job listed.
			}
			else if (dt.getSplitStart3() == null) { // only split once
				jobNum = dt.getJobNum2(); // so MPVs go to second (last) job listed.
			}
			else if (dt.getM2Out() == null) { // 3 jobs & no second meal,
				jobNum = dt.getJobNum3(); // so MPVs go to third (last) job listed.
			}
			else { // 3 jobs and 2 meals, so compare job starts to meal 2 time
				BigDecimal split = dt.getSplitStart2();
				if (dt.getSplitByPercent()) {
					split = dt.getHours().multiply(split).divide(Constants.DECIMAL_100);
					split = split.add(dt.getCallTime());
				}
				if (split.compareTo(dt.getM2Out()) >= 0) {
					// 2nd job starts after second meal,
					jobNum = dt.getJobNum1(); // so MPVs go to first job listed
				}
				else {
					// Determine time when 3rd job "starts"
					if (dt.getSplitByPercent()) {
						// take 1st split time and add hours in second job (converted from percentage)
						split = split.add(dt.getHours().multiply(dt.getSplitStart3()).divide(Constants.DECIMAL_100));
					}
					else {
						split = dt.getSplitStart3();
					}
					if (split.compareTo(dt.getM2Out()) >= 0) {
						// 3rd job starts after second meal,
						jobNum = dt.getJobNum2(); // so MPVs go to second job listed
					}
					else { // MPVs go to third job listed
						jobNum = dt.getJobNum3();
					}
				}
			}
		}
		PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobNum-1);
		return pj;
	}

	/**
	 * Determine which PayJob table should get any "wrap" MPVs -- penalties due
	 * to too long between the end of a second meal and the wrap time.
	 * <ul>
	 * <li>If there's only one job referenced for the day, that one is returned
	 * (duh).
	 * <li>If there are two jobs referenced, the later one is returned.
	 * <li>If there are three jobs referenced the third one is returned.
	 * </ul>
	 *
	 * @param dt The DailyTime for the day being calculated.
	 * @return One of the PayJob entries in the given DailyTime, as described
	 *         above.
	 */
	private PayJob findPayJobForWrapMpv(DailyTime dt) {
		int jobNum = 1;
		if (htg.weeklyTimecard.getPayJobs().size() > 1) {
			if (dt.getSplitStart2() == null) { // no splits
				jobNum = dt.getJobNum1(); // so MPVs go to first (only) job listed.
			}
			else if (dt.getSplitStart3() == null) { // only split once
				jobNum = dt.getJobNum2(); // so MPVs go to second (last) job listed.
			}
			else { // 3 jobs
				jobNum = dt.getJobNum3(); // so MPVs go to third (last) job listed.
			}
		}
		PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobNum-1);
		return pj;
	}

	/**
	 * Find all the different rules that apply to the timecard for the day being
	 * analyzed, and add audit-trail entries for them.
	 *
	 * @param dt The DailyTime entity for the day being analyzed.
	 * @return false if one or more required rules were not found.
	 */
	private boolean findDailyRules(DailyTime dt) {
		boolean ret = true;

		callBackRule = htg.ruleService.findCallBackRule(event);
		goldenRule = htg.ruleService.findGoldenRule(event);
		guaranteeRule = htg.ruleService.findGuaranteeRule(event);
		holidayRule = htg.ruleService.findHolidayRule(event);
		mpvRule = htg.ruleService.findMpvRule(event);
		npRule = htg.ruleService.findNpRule(event);
		onCallRule = htg.ruleService.findOnCallRule(event);
		overtimeRule = htg.ruleService.findOvertimeRule(event);
		restRule = htg.ruleService.findRestRule(event);

		StringBuilder keys = new StringBuilder();
		keys.append(weeklyRule == null ? "null" : weeklyRule.getRuleKey()).
				append(callBackRule == null ? "null" : callBackRule.getRuleKey()).
				append(goldenRule == null ? "null" : goldenRule.getRuleKey()).
				append(guaranteeRule == null ? "null" : guaranteeRule.getRuleKey()).
				append(holidayRule == null ? "null" : holidayRule.getRuleKey()).
				append(mpvRule == null ? "null" : mpvRule.getRuleKey()).
				append(npRule == null ? "null" : npRule.getRuleKey()).
				append(onCallRule == null ? "null" : onCallRule.getRuleKey()).
				append(overtimeRule == null ? "null" : overtimeRule.getRuleKey()).
				append(restRule == null ? "null" : restRule.getRuleKey());

		if (! dailyRuleKeys.equals(keys.toString())) {
			htg.ruleService.appendRule(weeklyRule, event, "MISSING BASIC WEEKLY RULE");
			htg.ruleService.appendRule(callBackRule, event, "MISSING CALL-BACK RULE");
			htg.ruleService.appendRule(goldenRule, event, "MISSING GOLDEN RULE");
			htg.ruleService.appendRule(guaranteeRule, event, "MISSING GUARANTEE RULE");
			htg.ruleService.appendRule(holidayRule, event, "MISSING HOLIDAY RULE");
			htg.ruleService.appendRule(mpvRule, event, "MISSING MPV RULE");
			htg.ruleService.appendRule(npRule, event, "MISSING NIGHT PREMIUM RULE");
			htg.ruleService.appendRule(onCallRule, event, "On-call: N/A");
			htg.ruleService.appendRule(overtimeRule, event, "MISSING OVERTIME RULE");
			htg.ruleService.appendRule(restRule, event, "MISSING REST INVASION RULE");
			dailyRuleKeys = keys.toString();
		}
		else {
			event.appendSummary("ditto");
		}

		if (weeklyRule == null || callBackRule == null || goldenRule == null ||
				guaranteeRule == null || holidayRule == null || mpvRule == null || npRule == null ||
				overtimeRule == null || restRule == null ) {
			String missing = "";
			missing += weeklyRule == null ? ", Weekly Rule" : "";
			missing += callBackRule == null ? ", CallBack Rule" : "";
			missing += goldenRule == null ? ", Golden Rule" : "";
			missing += guaranteeRule == null ? ", Guarantee Rule" : "";
			missing += holidayRule == null ? ", Holiday Rule" : "";
			missing += mpvRule == null ? ", Mpv Rule" : "";
			missing += npRule == null ? ", Night Premium Rule" : "";
			missing += overtimeRule == null ? ", Overtime Rule" : "";
			missing += restRule == null ? ", RestRule" : "";
			missing += " ";
			event.appendSummaryLine(Constants.NEW_LINE + "ERR: Missing rule(s): " + missing.substring(2));
			event.appendSummaryLine("No rule assigned, or unknown rule key");
			TimecardService.error(htg, "Timecard.FillJob.MissingRule", missing.substring(1));
			//log.warn(Constants.NEW_LINE + "ERROR: Missing rule(s):" + missing.substring(1) + "; TC:" + htg.weeklyTimecard);
			ret = false;
		}

		if (onCallRule == null && dt.getWorked()) {
			TimecardService.errorTrace(htg, "Timecard.FillJob.MissingRule", "On-Call Rule");
			ret = false;
		}

		return ret;
	}

	/**
	 * Make sure the current timecard has enough PayJob entries to allow the
	 * specified job number to be used. If there are not enough entries, enough
	 * entries will be added to handle the given job number.  Any new PayJobs
	 * created will have their account codes filled in from the timecard.
	 *
	 * @param job The job number (index, origin 0 !) to be used. This is one
	 *            less than the value the displayed and entered on the timecard
	 *            page.
	 */
	private void checkPayJobsSize(int job) {
		TimecardService.checkPayJobsSize(htg.weeklyTimecard, job);
	}

	/**
	 * Calculate the number of guaranteed hours for a non-union employee.
	 *
	 * @param zone The WorkZone of the day whose guaranteed hours should be
	 *            returned.
	 * @return The number of guaranteed hours (for a day) for the given DayType.
	 *         The dayType will determine if we use the Studio or Distant
	 *         guaranteed hours from the StartForm. Returns zero if no valid
	 *         guarantee can be determined. (Will never return null.)
	 */
	private BigDecimal calculateGuarHours(DailyTime dt) {
		BigDecimal hours = null;
		WorkZone zone = dt.getWorkZone();
		if (zone == null) {
			// Currently this shouldn't happen -- we're passing a default instead of null
			return BigDecimal.ZERO;
		}

		StartForm sf = htg.weeklyTimecard.getStartForm();

		// I think we don't need this - can use rules instead!
//		if (dt.getDayType() != null && dt.getDayType().isTours()) {
//			hours = Constants.WORKED_HOURS_NON_UNION; // LS-1347
//			return hours;
//		}

		StartRateSet rate = findProdPrepRate(sf, dt);
		hours = sf.getGuarHours(rate, ! zone.isDistant());
		if (hours != null && hours.compareTo(Constants.HOURS_IN_A_DAY) >= 0) {
			if (htg.nonUnion
					// 2.9.5086 allow exempt/union to calc default daily guarantee;
					// we'll need it if guarantee rule in effect has no hours specified.
					|| htg.weeklyTimecard.getAllowWorked()) {
				// Probably a weekly guarantee -- divide by 5 for daily guarantee
				hours = hours.divide(Constants.DECIMAL_FIVE);
			}
			else {
				// Note: cumulative weekly guarantee not supported for union with GT-NONE rule
				hours = null;
			}
		}
		if (hours == null) {
			hours = BigDecimal.ZERO;
		}
		log.debug("hrs=" + hours);
		return hours;
	}

	/**
	 * Calculate the total number of straight-time hours (hours at 1.0x) listed
	 * in the Job tables (PayJob) for the given timecard that occur from the
	 * beginning of the producer's work week until the end of the timecard (the
	 * end of the calendar week).
	 *
	 * @param wtc The timecard whose hours are being summed.
	 * @return The straight time hours within the given timecard for the last
	 *         producer's work week.
	 */
	private BigDecimal calculateStraightTime(WeeklyTimecard wtc) {
		int first = htg.preference.getFirstWorkDayNum() - 1; // want zero origin
		if (htg.floatingWeek) {
			for (DailyTime dt : wtc.getDailyTimes()) {
				if (dt.getWorkDayNum() == 1) {
					first = dt.getDayNum() - 1;
				}
			}
		}
		BigDecimal total = BigDecimal.ZERO;
		for (PayJob pj : wtc.getPayJobs()) {
			for (int i=first; i < 7; i++) {
				PayJobDaily pjd = pj.getPayJobDailies().get(i);
				if (pjd.getHours10() != null) {
					total = total.add(pjd.getHours10());
				}
			}
		}
		return total;
	}

	/**
	 * Calculate the daily guaranteed pay for an hourly person, based on the
	 * given PayJob's hourly rate, the current Guarantee rule, and the current
	 * Overtime rule.
	 *
	 * @param pj The PayJob from which to get the hourly rate of pay.
	 * @return The daily pay for the guaranteed number of hours.
	 */
	private BigDecimal calculateDailyRate(PayJob pj) {
		BigDecimal rate = null;
		if (guaranteeRule != null && overtimeRule != null && pj.getRate() != null) {
			BigDecimal hrs = overtimeRule.getOvertimeDailyBreak();
			BigDecimal ot = guaranteeRule.getMinimumCall().subtract(hrs);
			rate = pj.getRate().multiply(hrs);
			rate = rate.add(pj.getRate().multiply(ot).multiply(overtimeRule.getOvertimeRate()));
			rate = rate.setScale(2,RoundingMode.UP);
		}
		return rate;
	}

	/**
	 * Find the highest rate multiplier paid on the given Saturday.
	 *
	 * @param saturdayDt The DailyTime instance for the Saturday to be
	 *            inspected.
	 * @return The highest multiplier for which any time was paid on the given
	 *         Saturday, based on the information in the PayJob`s for that
	 *         timecard.
	 */
	private BigDecimal findHighSaturdayRate(DailyTime saturdayDt) {
		BigDecimal hiRate = BigDecimal.ONE; // Assume it's at least 1.0x!
		List<PayJob> pjs = saturdayDt.getWeeklyTimecard().getPayJobs();
		if (pjs != null && pjs.size() > 0) {
			for (PayJob pj : pjs) {
				PayJobDaily pjd = pj.getPayJobDailies().get(6); // Saturday's entry
				if (pjd.getHours15() != null && pjd.getHours15().signum() > 0) {
					hiRate = hiRate.max(Constants.DECIMAL_ONE_FIVE);
				}
				if (pjd.getHoursCust1() != null && pjd.getHoursCust1().signum() > 0) {
					hiRate = hiRate.max(pj.getCustomMult1());
				}
				if (pjd.getHoursCust2() != null && pjd.getHoursCust2().signum() > 0) {
					hiRate = hiRate.max(pj.getCustomMult2());
				}
				if (pjd.getHoursCust3() != null && pjd.getHoursCust3().signum() > 0) {
					hiRate = hiRate.max(pj.getCustomMult3());
				}
				if (pjd.getHoursCust4() != null && pjd.getHoursCust4().signum() > 0) {
					hiRate = hiRate.max(pj.getCustomMult4());
				}
				if (pjd.getHoursCust5() != null && pjd.getHoursCust5().signum() > 0) {
					hiRate = hiRate.max(pj.getCustomMult5());
				}
				if (pjd.getHoursCust6() != null && pjd.getHoursCust6().signum() > 0) {
					hiRate = hiRate.max(pj.getCustomMult6());
				}
				if (pjd.getHours15Np1() != null && pjd.getHours15Np1().signum() > 0) {
					hiRate = hiRate.max(Constants.DECIMAL_ONE_FIVE);
				}
				if (pjd.getHours15Np2() != null && pjd.getHours15Np2().signum() > 0) {
					hiRate = hiRate.max(Constants.DECIMAL_ONE_FIVE);
				}
			}
		}
		return hiRate;
	}

	/**
	 * Find the rate (amount) to be paid for a particular contract and rate
	 * code. This information is retrieved from the ContractRate table. LS-2241
	 *
	 * @param contractCode The contract code for the rate to be retrieved.
	 * @param rateCode The rate code for the rate to be retrieved.
	 * @return The rate found, or null if not found.
	 */
	private BigDecimal findJobRate(String contractCode, String rateCode, String category) {
		BigDecimal rate = jobRates.get(rateCode);
		if (rate == null) {
			rate = ContractRateDAO.getInstance().findRate(contractCode, rateCode, category);
			jobRates.put(rateCode, rate);
		}
		return rate;
	}

	/**
	 * Calculate and set the DailyTime.workDayNum fields for all days in the
	 * current timecard.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param htg The current HTG data instance.
	 */
	public static void calculateWorkDayNumbers(WeeklyTimecard wtc, HtgData htg) {
		if (htg.floatingWeek) {
			calculateFloatingWorkDayNumbers(wtc, htg.priorTimecard);
			htg.priorTcImportant = true; // last week's TC may affect this week's gross.
		}
		else {
			calculateNormalWorkDayNumbers(wtc, htg);
		}
	}

	/**
	 * Calculate and set the DailyTime.workDayNum fields for all days in the
	 * current timecard for an employee whose work week is tied to the Production
	 * work week.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param htg The current HTG data instance.
	 */
	private static void calculateNormalWorkDayNumbers(WeeklyTimecard wtc, HtgData htg) {

		int dayNum = htg.preference.getFirstWorkDayNum(); // day number/index (1-7) of first production day within the timecard
		byte worked7th = 6; // assume 7th production day will be 6th work day; may change.
		dayNum--;  // make it origin 0
		byte workDayNum = 0; // last used work-day number
		List<DailyTime> dTimes = wtc.getDailyTimes();

		for (int prodDay = 1; prodDay <= 7; prodDay++) {
			// 'prodDay' = sequential day number within the production week
			DailyTime dt = dTimes.get(dayNum);
			if (dayNum == 0 && prodDay != 1) { // examining first day in timecard, and prod week doesn't start on this day.
				worked7th = 6; // wrapped around from Sat to Sun; ensure 7th day defaults to 6th until overridden.
				// see how many days worked in prior payroll week that were part of this (overlapping) production week
				if (htg.priorTimecard != null) {
					htg.priorTcImportant = true; // last week's TC may affect this week's gross.
					workDayNum = TimecardCalc.calculateNumberOfWorkDays(htg.priorTimecard , htg.preference.getFirstWorkDayNum());
					if (workDayNum >= 6) {
						worked7th = 7;
					}
					else if (! htg.flexible6thDay) {
						// Not "flexible 6th", so even if worked < 6 days,
						// last day in timecard may have been paid as 6th...
						DailyTime last = htg.priorTimecard.getDailyTimes().get(6);
						if (last.getWorkDayNum() == 6) { // Yes, last day of prior week was paid as a 6th day,
							worked7th = 7; // so this (possibly 7th) day must be paid as 7th day.
						}
					}
					else if (workDayNum < 5 && htg.priorTimecard.getAllowWorked()
							&& htg.priorTimecard.getEmployeeRateType() == EmployeeRateType.WEEKLY
							&& htg.preference.getFirstWorkDayNum() < 4) {
						// LS-1735: assume prior week paid 5 days (using guarantee) even if
						// 5 days were not worked. Avoids treating day in current week as a
						// payable work day (i.e., days 1-5) when it should not be.
						workDayNum = 5;
					}
				}
				else {
					workDayNum = (byte)Math.min(workDayNum, 5);
				}
			}
			boolean worked = true;
			if (! dt.reportedWork()) {
				// no hours & "Worked" not checked -- probably not worked at all
				if (dt.getDayType() == null ||
						(dt.getDayType().getHours() == null && ! dt.getDayType().isIdle() && dt.getDayType() != DayType.OV) ||
						(dt.getDayType().isWorkTime() && wtc.getAllowWorked())) {
					// Not worked - either empty DayType,
					// or DayType (other than Idle) has NO guarantee, e.g., Holiday Not Paid
					// or "Worked" day type & exempt & "worked" not checked -- not really worked!
					worked = false;
				}
			}
			if (dt.getDayType() != null && dt.getDayType().isTravel() && (! htg.nonUnion)) {
				// Union, travel day does not count towards 6th/7th days.
				dt.setWorkDayNum((byte)Math.max(workDayNum, 1)); // but don't set it to zero
			}
			else if (worked) {
				workDayNum++;
				if (prodDay == 7 && workDayNum > 5) { // 7th day of production week & worked over 5 days
					if (workDayNum == 7 || (dt.getDayType() != null && dt.getDayType().isIdle())) {
						// an idle on 7th day of work week is always an "idle 7th" (not 6th)
						worked7th = 7;
					}
					dt.setWorkDayNum(worked7th); // may be 6th or 7th depending on earlier checks.
				}
				else if (prodDay == 7 && ! htg.flexible6thDay) {
					// For union, 7th day in prod week is paid AT LEAST as "6th day"
					// Code in "if block" above will have handled case where it pays as "7th day";
					// or "worked7th" will have been bumped to 7 if prior day was paid as 6th.
					dt.setWorkDayNum(worked7th);
				}
				else if (prodDay == 6 && ! htg.flexible6thDay) {
					// If not "flexible 6th", 6th day in prod week is paid AT LEAST as "6th day"
					dt.setWorkDayNum((byte)6);
				}
				else {
					dt.setWorkDayNum(workDayNum);
				}
			}
			else {
				dt.setWorkDayNum((byte)0);
			}
			log.debug("prodDay="+prodDay + ", day=" + dt.getWeekDayName() + ", workD#=" + dt.getWorkDayNum());
			dayNum++;
			if (dayNum == 7) {
				dayNum = 0;
			}
		}
	}

	/**
	 * Calculate and set the DailyTime.workDayNum fields for all days in the
	 * current timecard when the employee is using a "floating" work week -- one
	 * not tied to the Production's work week, and which may change from week
	 * to week.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param priorTc The WeeklyTimecard of the prior week.
	 */
	private static void calculateFloatingWorkDayNumbers(WeeklyTimecard wtc, WeeklyTimecard priorTc) {
		byte workDayNum = 0; // last used work-day number

		byte weekDayNum = 0; // Day number assigned to the last elapsed day, that is, the "calendar" day-number within...
			// ... the 7-day rolling work week. Zero means we're ready to start a new 7-day period when the
			// ... next worked day is encountered.

		if (priorTc != null) {
			if (priorTc.getEndingDayNum() > 7) {
				// prior timecard not calculated,
				// ... so calculate it, assuming no week prior to that.
				calculateFloatingWorkDayNumbers(priorTc, null);
			}
			weekDayNum = priorTc.getEndingDayNum();
			if (weekDayNum > 7) { // not calculated (shouldn't happen)
				weekDayNum = 0;
			}
			// Note: if weekDayNum==0, workDayNum must be zero (waiting to start new week).
			if (weekDayNum > 0) {
				workDayNum = priorTc.getLastWorkDayNum();
				if (workDayNum > 7) { // not calculated (shouldn't happen)
					workDayNum = 0;
				}
			}
		}
		List<DailyTime> dTimes = wtc.getDailyTimes();

		byte lastWorkDayNum = 0;
		// LS_2944
		boolean resetWorkDay = false;
		boolean daysOff[] = new boolean[7];

		for (int dayNum = 0; dayNum < 7; dayNum++) {
			DailyTime dt = dTimes.get(dayNum);
			boolean worked = true;
			if (! dt.reportedWork()) {
				// no hours & "Worked" not checked -- probably not worked at all
				if (dt.getDayType() == null ||
						(dt.getDayType().getHours() == null
							&& ! dt.getDayType().isIdle() && ! dt.getDayType().isTours() && dt.getDayType() != DayType.OV) ||
						(dt.getDayType().isWorkTime() && wtc.getAllowWorked())) {
					// Not worked - either empty DayType,
					// or DayType (other than Idle) has NO guarantee, e.g., Holiday Not Paid
					// or "Worked" day type & exempt & "worked" not checked -- not really worked!
					worked = false;
					//LS-2944 checking if leave is taken store value as 1 else 0
					daysOff[dayNum] = true;
					if (dayNum == 0 && priorTc != null) {
						DailyTime pdt = priorTc.getDailyTimes().get(6);
						if (! pdt.reportedWork() && // LS-3533 correct test for end-of-last-week issue
								(pdt.getDayType() == null ||
								(pdt.getDayType().getHours() == null &&
										! pdt.getDayType().isIdle() &&
										! pdt.getDayType().isTours() &&
										pdt.getDayType() != DayType.OV) ||
								(pdt.getDayType().isWorkTime() && priorTc.getAllowWorked()))) {
							// Prior week check if no hours & "Worked" not checked -- probably not worked at all
							// Not worked - either empty DayType,
							// or DayType (other than Idle) has NO guarantee, e.g., Holiday Not Paid
							// or "Worked" day type & exempt & "worked" not checked -- not really worked!
							resetWorkDay = true;
						}
					}
					else if (dayNum != 0 && daysOff[dayNum - 1]) { // for current weekly timecard only
						resetWorkDay = true;
					}

				}
			}
			else {
				daysOff[dayNum] = false;
			}

			if (worked || weekDayNum != 0) {
				weekDayNum++; // another day, worked or not, has passed in the rolling week.
				if (weekDayNum > 7 || resetWorkDay) { // reached end of rolling week.
					weekDayNum = 0; // next rolling week starts on next worked day
					workDayNum = 0; // reset count of worked-days within the rolling week
					resetWorkDay = false;
				}
			}
			if (worked) {
				workDayNum++;

				// Note that workDayNum can never exceed 7, as it never exceeds
				// ... weekDayNum, and when that goes over 7 (above), workDayNum is reset.
				if (weekDayNum == 0) {
					weekDayNum = 1;
				}
				dt.setWorkDayNum(workDayNum);
				lastWorkDayNum = workDayNum;
			}
			else {
				dt.setWorkDayNum((byte)0);
			}
			log.debug("workday=" + workDayNum + ", wkDayNum=" + weekDayNum + ", workD#=" +
					dt.getWorkDayNum());
		}
		wtc.setEndingDayNum(weekDayNum);
		wtc.setLastWorkDayNum(lastWorkDayNum);
	}


	public static BigDecimal calculatePayHours(List<PayJob> payJobs, int dayNum) {
		BigDecimal payHours = Constants.DECIMAL_ZERO;
		BigDecimal hours;
		MathContext mc = new MathContext(4, RoundingMode.HALF_UP);

		if(payJobs == null || payJobs.isEmpty()) {
			return payHours;

		}
		for(PayJob payJob : payJobs) {
			PayJobDaily payJobDaily = payJob.getPayJobDailies().get(dayNum);

			// 1x
			hours = payJobDaily.getHours10();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(new BigDecimal(1.0)));
			}
			// 1.5x
			hours = payJobDaily.getHours15();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(new BigDecimal(1.5)));
			}
			// Custom Multipliers
			// Custom 1
			hours = payJobDaily.getHoursCust1();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(payJob.getCustomMult1()));
			}
			// Custom 2
			hours = payJobDaily.getHoursCust2();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(payJob.getCustomMult2()));
			}
			// Custom 3
			hours = payJobDaily.getHoursCust3();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(payJob.getCustomMult3()));
			}
			// Custom 4
			hours = payJobDaily.getHoursCust4();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(payJob.getCustomMult4()));
			}
			// Custom 5
			hours = payJobDaily.getHoursCust5();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(payJob.getCustomMult5()));
			}
			// Custom 6
			hours = payJobDaily.getHoursCust6();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(payJob.getCustomMult6()));
			}

			// Night Premium Mutlipliers
			// NP 10-1
			hours = payJobDaily.getHours10Np1();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(Constants.HOURS_10_NP1));
			}
			// NP 10-1
			hours = payJobDaily.getHours10Np1();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(Constants.HOURS_10_NP2));
			}
			// NP 15-1
			hours = payJobDaily.getHours10Np1();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(Constants.HOURS_15_NP1));
			}
			// NP 15-2
			hours = payJobDaily.getHours10Np1();
			if(hours != null) {
				payHours = payHours.add(hours.multiply(Constants.HOURS_15_NP2));
			}
		}
		// Round up the total pay hours.
		return payHours.round(mc);
	}

	/**
	 * @return The HtgData prodOrPrep value; if null, it will calculate it first.
	 */
	private ProdOrPrep getProdOrPrep() {
		if (htg.prodOrPrep == null) {
			htg.prodOrPrep = calculateProdOrPrep(htg.weeklyTimecard.getStartForm());
		}
		return htg.prodOrPrep;
	}
	/** See {@link #unitTest}. */
	public boolean getUnitTest() {
		return unitTest;
	}
	/** See {@link #unitTest}. */
	public void setUnitTest(boolean unitTest) {
		this.unitTest = unitTest;
	}

	/** See {@link #goldenRule}. */
	public void setGoldenRule(GoldenRule goldenRule) {
		this.goldenRule = goldenRule;
	}

	/** See {@link #guaranteeRule} */
	public void setGuaranteeRule(GuaranteeRule rule) {
		guaranteeRule = rule;
	}

	/** See {@link #holidayRule}. */
	public void setHolidayRule(HolidayRule holidayRule) {
		this.holidayRule = holidayRule;
	}

	/** See {@link #npRule}. */
	public void setNpRule(NtPremiumRule npRule) {
		this.npRule = npRule;
	}

	/** See {@link #overtimeRule}. */
	public void setOvertimeRule(OvertimeRule overtimeRule) {
		this.overtimeRule = overtimeRule;
	}

	/** See {@link #weeklyRule}. */
	public void setWeeklyRule(WeeklyRule weeklyRule) {
		this.weeklyRule = weeklyRule;
	}

}
