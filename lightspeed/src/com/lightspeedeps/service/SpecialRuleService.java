/**
 * SpecialRuleService.java
 */
package com.lightspeedeps.service;

import java.math.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.payroll.TimecardCalc;

/**
 * Handles processing of the contract Special Rules. These are rules which
 * generally have no parameters stored in the database. The function of each
 * rule is specific to that rule name, and determined simply by the rule name.
 * <p>
 * Special Rules may involve processing the payroll data at one or more points
 * in the HTG process. There are several "standard" points at which this class'
 * prePhase or postPhase methods are called, to allow any Special Rule code to
 * run at that point.
 */
public class SpecialRuleService {
	/** Logger */
	private static final Log LOG = LogFactory.getLog(SpecialRuleService.class);

	/** Travel outside minimum call & before 6am doesn't count. */
	private static final BigDecimal EARLY_TRAVEL_LIMIT = new BigDecimal(6);

	/** Travel outside minimum call & after 6pm doesn't count. */
	private static final BigDecimal LATE_TRAVEL_LIMIT = new BigDecimal(18);

	/** The time-of-day used for the special rule reverting call time to 8:30am */
	private static final BigDecimal REVERT_CALL_TIME = new BigDecimal("8.5");

	/** The hours after which the "pay daily per 5 hours" Starts (currently 12). */
	private static final BigDecimal PAY_DAY_PER_5_START = new BigDecimal(12);

	/** The number of worked hours which gets another day's pay (currently 5),
	 * for the SP_PAY_DAY_PER_5 rule. */
	private static final BigDecimal PAY_DAY_PER_HOURS = new BigDecimal(5);

	/** 'split job group' number assigned for Travel days. Must be greater
	 * than 7 to avoid collision with groups assigned in PayJobService. */
	private static final int TRAVEL_GROUP = 10;

	/** The last set of rule key ids logged to our audit trail. Used to
	 * suppress duplicate log information. */
	private String ruleKeysLogged = "";

	/** If true, we have issued the "special rule is TBD" message already, so it
	 * won't be issued a second time. */
	private boolean tbdFlag = false;

	/** The current HtgData instance. */
	private final	HtgData htg;

	/** Enum defines a value for each of the Special Rules; the enum name must
	 * match the SpecialRule.name field in the SpecialRule table. */
	private enum SpecialRuleType {

		/** Report time for a timecard with a Daily rate, non-exempt, as Flat,
		 * even though Flat is usually only used for exempt workers. Also, if no
		 * hourly rate is specified in the Start Form, compute it from the Daily rate. */
		SP_DAILY_FLAT,

		/** True if "6th day rates" are applied flexibly, that is, only when the
		 * number of days worked is more than 5. False if "6th day rates" ALWAYS
		 * apply to the 6th day of the production week, regardless of the number
		 * of days worked during the week. */
		SP_FLEXIBLE_6TH,

		/** True if the employee's work week is allowed to "float", so that it is
		 * not tied to the Production work week, but dependent only on what days
		 * this employee worked. */
		SP_FLOATING_WEEK,

		/** Do not deduct meal times during Gold hours. */
		SP_GOLD_NO_DED_MEAL,

		/** Must work scheduled day before & after holiday to get paid
		 * for not-worked Holiday (no pay if 1+wk hiatus follows) */
		SP_HOLIDAY_WORK_A, // TODO to be implemented

		/** Special for non-union, check StartForm's */
		SP_NO_6TH_7TH_DAY,

		/** Mark OT pay breakdown lines as "Extended Day" instead of just "n.nx Hours". */
		SP_OT_EXTENDED,

		/** Round worked hours up to 1/2 hour for OT calculation */
		SP_OT_ROUND_HALF_HR,

		/** When a week is mixed Distant & Studio, 5 days pay must be at least
		 *  equal to Studio week rate. */
		SP_PART_WK_GUAR,

		/** After full week, partial last week pays 10% of 1/5 wkly minimum per day not worked */
		SP_PAY_10_PCT_GUAR, // TODO to be implemented

		/** Pay 1 day's pay for every 5 hours worked after the first 12 hours. */
		SP_PAY_DAY_PER_5,

		/** Pay 1.5 day's pay for every 5 hours worked after the first 12 hours. */
		SP_PAY_DAY15X_PER_5,

		/** Pay 2 day's pay for every 5 hours worked after the first 12 hours. */
		SP_PAY_DAY2X_PER_5,

		/** Only pay holiday if worked 4 (of 5) or 5 (of 6) previous days & worked one day this week. */
		SP_PAY_HOLIDAY_1, // TODO to be implemented

		/** Force the following day to be a "Worked" day; used for O/C working
		 *  past 1am and 15 hours or more */
		SP_PAY_NXT_WRK,

		/** Weekly, pro-rating; suspend guarantee if start date within week, or prior week is full week. */
		SP_PRO_RATE_FRACTION, // TODO to be implemented

		/** Revert call time to 8:30am if between 8:30a and 4:30p */
		SP_REVERT_CALL_85,

		/** Skip paying a guaranteed week`s pay for OnCall employees. */
		SP_SKIP_OC_GUARANTEE,

		/** Travel time (days) should be split to a separate PayJob instance. LS-4664 */
		SP_SPLIT_TRAVEL,

		/** Special for travel+work; Meal 1 out=travel end; meal 1 in=work start.
		 * Travel within minimum call=work, adds to gold. */
		SP_TRAV_WRK_1,

		/** Use a 5-day span (120 hrs from 1st call time in week) for computing OT. (for NY 817) */
		SP_USE_5_DAY_SPAN,

		/** Special for work+travel; last meal out=wrap; last meal in=travel start.
		 * Travel within minimum call=work, adds to gold. */
		SP_WRK_TRAV_1,
	}

	/** Specifies which part of the HTG process is calling the special rules
	 * service. Many of the special rules only affect one particular phase. In
	 * addition, this is called twice for each phase -- once before that phase's
	 * processing has started, called the 'pre-phase call', and a second time
	 * when that phase's processing is done, called the 'post-phase call'. */
	public static enum HtgPhase {
		/** Meal Penalty evaluation phase is in progress. */
		MPV,
		/**Night Premium calculation phase is in progress. */
		NIGHT_PREMIUM,
		/** Fill Jobs phase (when the PayJob tables are filled in) is in progress. */
		FILL_JOBS,
		/** Pay Breakdown phase (when Pay Breakdown entries are generated) is in progress. */
		PAY_BREAKDOWN
	}

	/** Which phase of HTG processing we have been called for. */
	private HtgPhase phase;

	/** The List of SpecialRule instances that applies to the current process. */
	private final List<SpecialRule> specialRules = new ArrayList<>();

	/** The AuditEvent to use for logging. */
	private AuditEvent event;

	/** The DailyTime that was used to determine the current set of SpecialRules;
	 * this is remembered so we don't need to re-calculate the list for each
	 * phase of a given day's processing. */
	private DailyTime rulesDailyTime;

	/** The current DailyTime being processed. */
	private DailyTime dailyTime;

	/** The DAO for accessing our SpecialRule objects. */
	private final SpecialRuleDAO specialRuleDAO;

	/** Our normal constructor. */
	public SpecialRuleService(HtgData phtg) {
		htg = phtg;
		specialRuleDAO = SpecialRuleDAO.getInstance();
	}

	/**
	 * Process any Special rules applicable to the current timecard, for the
	 * DailyTime being calculated, prior to the specified phase.
	 *
	 * @param dt The DailyTime being calculated/processed.
	 * @param phas The phase of HTG processing about to be performed.
	 * @param evt The AuditEvent for logging HTG information.
	 * @return True if processing should continue for this DailyTime. False
	 *         indicates that the given DailyTime should NOT be processed by the
	 *         given phase -- usually meaning that we will not generate any
	 *         hours or pay for this DailyTime.
	 */
	public boolean prePhase(DailyTime dt, HtgPhase phas, AuditEvent evt) {
		event = evt;
		phase = phas;
		dailyTime = dt;
		LOG.debug(phase.name() + " - " + (dt == null ? "(any day)" :  dt.getWeekDayName()));
		return process(true);
	}

	/**
	 * Process any Special rules applicable to the current timecard, for the
	 * DailyTime being calculated, just after the specified phase has finished.
	 *
	 * @param dt The DailyTime being calculated/processed.
	 * @param phas The phase of HTG processing that was just performed.
	 * @param evt The AuditEvent for logging HTG information.
	 * @return True if processing should continue for this DailyTime. Currently
	 *         no Special rules return false from this post-phase check.
	 */
	public boolean postPhase(DailyTime dt, HtgPhase phas, AuditEvent evt) {
		event = evt;
		phase = phas;
		dailyTime = dt;
		LOG.debug(phase.name() + (dt == null ? "" :  dt.getWeekDayName()));
		return process(false);
	}

	private boolean process(boolean pre) {
		boolean ret = false;
		try {
			findRules();
			ret = processRules(pre);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return ret;
	}

	/**
	 * Process the rules in the {@link #specialRules} list. Processing
	 * frequently depends on the current {@link #phase} and whether this is the
	 * pre- or post-phase call.
	 *
	 * @param pre True if this is for the pre-phase call; false for the
	 *            post-phase call.
	 * @return True if the current day should be paid; false if the current day
	 *         should NOT be paid. When false is returned, it is expected that
	 *         no hours will be entered into the Job table for the current day.
	 */
	private boolean processRules(boolean pre) {
		boolean ret = true;
		for (SpecialRule rule : specialRules) {
			SpecialRuleType rt = null;
			try {
				rt = SpecialRuleType.valueOf(rule.getRuleKey());
			}
			catch (Exception e) {
				EventUtils.logError("Unknown Special Rule: " + rule.getRuleKey());
				break;
			}
			String msg = MsgUtils.formatMessage("Timecard.HTG.Unimplemented", rule.getRuleKey());
			switch (rt) {
				// These are the UNIMPLEMENTED Rules:
				case SP_HOLIDAY_WORK_A:
				case SP_PAY_10_PCT_GUAR:
				case SP_PAY_HOLIDAY_1:
					if (pre && phase == HtgPhase.FILL_JOBS) {
						MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, msg);
						LOG.error(msg);
					}
					// TODO Implement special rules
					break;

				// Remaining case entries are in alphabetical order
				case SP_DAILY_FLAT:
					htg.dailyFlat = true;
					break;
				case SP_FLEXIBLE_6TH:
					// Set flag used during work-day number calculation
					htg.flexible6thDay = true;
					break;
				case SP_FLOATING_WEEK:
					if (! htg.isSimpleWeekly) { // 'simple weekly' type ignores floating week. LS-2189
						// Set flag used during work-day number calculation
						htg.floatingWeek = true;
						// NOTE: floating-week IMPLIES flexible-6th-day
						htg.flexible6thDay = true;
					}
					break;
				case SP_GOLD_NO_DED_MEAL:
					// Do not deduct meal times once Gold time is entered
					htg.goldPaysMeals = true;
					break;
				case SP_NO_6TH_7TH_DAY:
					switch(phase) {
						case FILL_JOBS:
							ret = no6th7thDayFillJobs(pre);
							break;
						case MPV:
						case NIGHT_PREMIUM:
						case PAY_BREAKDOWN:
							break;
					}
					break;
				case SP_OT_EXTENDED:
					 // just set flag for PayBreakdownService.
					htg.otExtended = true;
					break;
				case SP_OT_ROUND_HALF_HR:
					// override default rounding: set to 1/2-hr rounding
					htg.otRounding = HourRoundingType.HALF;
					break;
				case SP_PART_WK_GUAR:
					switch(phase) {
						case PAY_BREAKDOWN:
							partWeekGuarPayBreak(pre);
							break;
						case MPV:
						case NIGHT_PREMIUM:
						case FILL_JOBS:
							break;
					}
					break;
				case SP_PAY_DAY_PER_5:
					if (pre) {
						payDaysPer5Hours(BigDecimal.ONE);
					}
					break;
				case SP_PAY_DAY15X_PER_5:
					if (pre) {
						payDaysPer5Hours(Constants.DECIMAL_ONE_FIVE);
					}
					break;
				case SP_PAY_DAY2X_PER_5:
					if (pre) {
						payDaysPer5Hours(Constants.DECIMAL_TWO);
					}
					break;
				case SP_PAY_NXT_WRK:
					if (! pre) { // just set flag for PayJobService.
						htg.nextDayIsWork = true;
					}
					break;
				case SP_PRO_RATE_FRACTION:
					// sets flag for PayBreakdownService.
					if (pre) {
						switch (phase) {
							case PAY_BREAKDOWN:
								// Applies if start date within week, or prior week is full week.
								if (htg.priorTimecard == null) {
									// just started this week, so apply setting
									htg.proRateFraction = true;
								}
								else {
									int n = TimecardCalc.calculateNumberOfWorkDays(htg.priorTimecard);
									if (n >= 5) { // "full week" - at least 5 days worked
										htg.proRateFraction = true;
									}
								}
								break;
							default:
								break;
						}
					}
					break;
				case SP_REVERT_CALL_85:
					switch(phase) {
						case FILL_JOBS:
							BigDecimal call = dailyTime.getCallTime();
							BigDecimal hours = dailyTime.getHours();
							dailyTime.setCallTime(REVERT_CALL_TIME);
							TimecardCalc.calculateDailyHours(dailyTime);
							htg.hours = dailyTime.getHours(); // Use new hours for HTG
							dailyTime.setCallTime(call); // restore call time
							dailyTime.setHours(hours); // restore hours
							break;
						case MPV:			// No action needed
						case NIGHT_PREMIUM:	// No action needed
						case PAY_BREAKDOWN: // No action needed
							break;
					}
					break;
				case SP_SKIP_OC_GUARANTEE:
					// just set flag for PayBreakdownService.
					htg.skipOcGuarantee = true;
					break;
				case SP_SPLIT_TRAVEL: // LS-4664
					if (pre && phase == HtgPhase.FILL_JOBS) {
						splitTravel(); // set up PayJob for travel.
					}
					break;
				case SP_TRAV_WRK_1:
					switch(phase) {
						case FILL_JOBS:
							travWork1FillJobs(pre);
							break;
						case MPV:
						case NIGHT_PREMIUM:
						case PAY_BREAKDOWN:
							break;
					}
					break;
				case SP_USE_5_DAY_SPAN:
					switch(phase) {
						case FILL_JOBS:
							MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, msg);
							LOG.error(msg);
							// TODO
							break;
						case MPV:			// No action needed
						case NIGHT_PREMIUM:	// No action needed
							break;
						case PAY_BREAKDOWN: // TODO ??
							break;
					}
					break;
				case SP_WRK_TRAV_1:
					switch(phase) {
						case FILL_JOBS:
							workTravel1FillJobs(pre);
							break;
						case MPV:
						case NIGHT_PREMIUM:
						case PAY_BREAKDOWN:
							break;
					}
					break;
			}
		}
		return ret;
	}

	/**
	 */
	private void findRules() {
		List<ContractRule> contractRules;

		if (dailyTime != null && dailyTime == rulesDailyTime) {
			// Still processing the same day's data, so no need
			// ... to re-calculate the set of rules to apply.
			return;
		}
		rulesDailyTime = dailyTime; // remember for next time.

		if (dailyTime == null) { // for Pay Breakdown, there is no DailyTime element
			contractRules = htg.ruleService.findContractRulesByType(RuleType.SP, event);
		}
		else {
			contractRules = htg.ruleService.findDailyContractRules(dailyTime, RuleType.SP, event);
		}
		specialRules.clear();
		// keep track of rules retrieved, and ignore duplicates
		Set<String> ruleKeys = new HashSet<>();
		boolean remove;
		for (ContractRule cr : contractRules) {
			remove = false;
			String key = cr.getUseRuleKey().toUpperCase();
			if (key.startsWith("NOT ")) {
				key = key.substring(4);
				remove = true;
			}
			if (! ruleKeys.contains(key) && ! remove) { // haven't had this one before
				ruleKeys.add(key);
				SpecialRule rule = specialRuleDAO.findOneByRuleKey(key);
				if (rule != null) {
					specialRules.add(rule);
				}
				else if (key.startsWith("TBD")) {
					if (! tbdFlag) { // only warn once per instantiation (i.e., per HTG function)
						TimecardService.warn(htg, "GenericMessage", "Special rule invoked is still TBD");
						tbdFlag = true;
					}
				}
				else {
					TimecardService.warn(htg, "GenericMessage", "Special rule invoked is unrecognized: '" + key + "'");
					EventUtils.logError("Unknown Special Rule reference; rule name='" + key + "'");
				}
			}
			else if (remove) {
				ruleKeys.remove(key);
				SpecialRule rule = specialRuleDAO.findOneByRuleKey(key);
				if (rule != null) {
					specialRules.remove(rule);
				}
			}
		}

		// See if rules in effect changed & log them if so...
		StringBuffer keyIds = new StringBuffer(50);
		for (SpecialRule sr : specialRules) {
			keyIds.append(sr.getId()).append(';');
		}
		if (! keyIds.toString().equals(ruleKeysLogged)) {
			if (specialRules.isEmpty()) {
				// Need to show that previous special rule(s) went away.
				event.appendSummary("Special: none");
			}
			else {
				for (SpecialRule sr : specialRules) {
					htg.ruleService.appendRule(sr, event, "");
				}
			}
			ruleKeysLogged = keyIds.toString();
		}
	}

	/**
	 * May be called to ensure that the selected Special Rules are logged on our
	 * next invocation. Otherwise, they are only logged when the selection
	 * changes. For better visibility in the HTG audit trace, this method is
	 * called when Fill Jobs starts and when Pay Breakdown (Auto Pay) starts, so
	 * the log will show the special rules in effect at each major step in the
	 * HTG process, even if it's the same set as at the end of the prior step.
	 */
	public void resetRuleLogging() {
		ruleKeysLogged = "";
	}

	/**
	 * no6th7thDay rule, FillJobs phase. If the StartForm's "pay6th7thDay" flag
	 * is false, then we may skip processing Sunday and/or Saturday, depending
	 * on the number of days worked in the week.
	 *
	 * @param prePhase True if this is the pre-phase call; false if the
	 *            post-phase call.
	 * @return True if the current day should be paid; false if the current day
	 *         should NOT be paid. When false is returned, it is expected that
	 *         no hours will be entered into the Job table for the current day.
	 */
	private boolean no6th7thDayFillJobs(boolean prePhase) {
		if (! prePhase) { // only need to check once, during pre-phase.
			return true;
		}
		if (htg.weeklyTimecard.getStartForm().getRateType() != EmployeeRateType.WEEKLY) {
			// this rule should only be used for Weekly/Exempt employees, not Daily Exempt.
			return true;
		}
		byte dayNum = dailyTime.getDayNum();
		if (htg.weeklyTimecard.getStartForm().getPay6th7thDay() ||
				(dayNum != 1 && dayNum != 7)) {
			return true;
		}
		boolean sunWorked = false;
		int daysWorked = 0;
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (dt.reportedWork()) {
				daysWorked++;
				if (dt.getDayNum() == 1) {
					sunWorked = true;
				}
			}
		}

		boolean ret = true;
		if (daysWorked > 5) {
			if (dayNum == 1) {
				ret = false;
			}
			else if (dayNum == 7 &&
					(daysWorked == 7 || (! sunWorked)) ) {
				ret = false;
			}
		}
		return ret;
	}

	/**
	 * "Partial Week" (mixed Studio & Distant) special rule to force a minimum
	 * payment during the Pay Breakdown phase of processing.
	 *
	 * @param prePhase True if this is the pre-phase call; false if the
	 *            post-phase call.
	 */
	private void partWeekGuarPayBreak(boolean prePhase) {
		if (prePhase) { // Only process post-phase
			return;
		}
		// Determine amount paid for "flat-rate" days
		List<PayBreakdown> payLines = htg.weeklyTimecard.getPayLines();
		BigDecimal paid = BigDecimal.ZERO;
		for (PayBreakdown pb : payLines) {
			if (pb.getCategoryEnum().equals(PayCategory.FLAT_RATE.name())) {
				paid = paid.add(pb.getTotal());
			}
		}

	}

	/**
	 * @param multiplier Multiplier for number of days pay per each five hours
	 *            beyond 12 hours.
	 */
	private void payDaysPer5Hours(BigDecimal multiplier) {
		if (phase == HtgPhase.FILL_JOBS && dailyTime.getHours() != null) {
			// Compute #days = (hours-12) / 5, rounded up to integer
			BigDecimal days = dailyTime.getHours().subtract(PAY_DAY_PER_5_START)
					.divide(PAY_DAY_PER_HOURS, 0, RoundingMode.CEILING);
			htg.otMult = days.multiply(multiplier).negate();
			// PayJobService will treat negative otMult as # of DAYS of OT.
		}
	}

	/**
	 * If any days are Travel Day Type, set up a separate PayJob entry
	 * to pay those days.  LS-4664
	 */
	private void splitTravel() {
		if (dailyTime.getDayType() == DayType.TR &&
				htg.isModelRelease && // For now, limit it to Model Release forms
				dailyTime.getJobSplitGroup() != TRAVEL_GROUP) {
			// first Travel day encountered: set up new PayJob
			WeeklyTimecardDAO.addPayJob(htg.weeklyTimecard);
			byte jobs = (byte)htg.weeklyTimecard.getPayJobsSize();
			for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
				if (dt.getDayType() == DayType.TR) {
					dt.setJobSplitGroup(TRAVEL_GROUP);
					dt.setJobNum1(jobs);
					dt.setSplitByPercent(true);
					dt.setSplit1(Constants.DECIMAL_100);
				}
			}
			BigDecimal rate = null;
			if (htg.modelRelease.getTravelDayRate() && htg.modelRelease.getTravelDayPerDay() != null &&
					htg.modelRelease.getTravelDayPerHours() != null) {
				try {
					rate = htg.modelRelease.getTravelDayPerDay().divide(new BigDecimal(htg.modelRelease.getTravelDayPerHours()), 2, RoundingMode.HALF_UP);
				}
				catch (Exception e) {
					LOG.debug("Model Release travel-rate per day calculation threw exception:", e);
				}
			}
			if (rate != null) {
				PayJob pj = htg.weeklyTimecard.getPayJobs().get(jobs-1);
				pj.setRate(rate);
			}
		}
		if (dailyTime.getDayType() == DayType.TR && htg.isModelRelease) {
			htg.guarHours = new BigDecimal(htg.modelRelease.getTravelDayPerHours());
		}
	}

	/**
	 * Travel+Work, Fill Jobs phase
	 *
	 * @param prePhase True if this is the pre-phase call; false if the
	 *            post-phase call.
	 */
	private void travWork1FillJobs(boolean prePhase) {
		if (! prePhase) {
			return;
		}
		BigDecimal work = calculateNonTravelWork();

		BigDecimal minCall = htg.guarHours;

		BigDecimal travel = TimecardCalc.calculateTravelHours(dailyTime);

		if (travel.compareTo(minCall) > 0) {
			String date = new SimpleDateFormat("M/d").format(dailyTime.getDate());
		// Travel beyond minimum call & before 6am might not count
			if (dailyTime.getCallTime().compareTo(EARLY_TRAVEL_LIMIT) < 0) {
				TimecardService.warn(htg, "Timecard.HTG.TravelEarly", date);
//				BigDecimal diff = EARLY_TRAVEL_LIMIT.subtract(dailyTime.getCallTime());
//				travel = travel.subtract(diff);
//				travel = travel.max(minCall);
			}
			// Travel beyond minimum call & after 6pm might not count
			if (dailyTime.getM1Out() != null && dailyTime.getM1Out().compareTo(LATE_TRAVEL_LIMIT) > 0) {
				TimecardService.warn(htg, "Timecard.HTG.TravelLate", date);
//				BigDecimal diff = dailyTime.getM1Out().subtract(LATE_TRAVEL_LIMIT);
//				travel = travel.subtract(diff);
//				travel = travel.max(minCall);
			}
		}
		htg.hours = travel.add(work);

		// Travel time beyond minimum call does NOT count towards gold
		travel = travel.min(minCall);
		htg.goldEligHours1 = travel.add(work);
		if (htg.goldHoursType == HoursType.E) { // using Elapsed, not Worked, hours
			// So add meal time for gold-eligible hours
			MpvRule mpvRule = htg.ruleService.findMpvRule(event);
			BigDecimal mealLength = TimecardCalc.calculateMeal1Length(dailyTime, mpvRule.getNdmAllowed());
			if (mealLength != null) {
				htg.goldEligHours1 = htg.goldEligHours1.add(mealLength);
			}
		}
	}

	/**
	 * Work+Travel, Fill Jobs phase
	 *
	 * @param prePhase True if this is the pre-phase call; false if the
	 *            post-phase call.
	 */
	private void workTravel1FillJobs(boolean prePhase) {
		if (! prePhase) {
			return;
		}
		BigDecimal work = calculateNonTravelWork();

		BigDecimal minCall = htg.guarHours;

		// how much time is left in minimum call after work?
		BigDecimal minCallLeft = minCall.subtract(work).max(BigDecimal.ZERO);

		BigDecimal travel = TimecardCalc.calculateTravelHours(dailyTime);

		if (travel.compareTo(minCallLeft) > 0) {
			String date = new SimpleDateFormat("M/d").format(dailyTime.getDate());
			BigDecimal travelStart = dailyTime.getM2In();
			if (travelStart == null) {
				travelStart = dailyTime.getM1In();
			}
			// Travel beyond minimum call & before 6am might not count
			if (travelStart.compareTo(EARLY_TRAVEL_LIMIT) < 0) {
				TimecardService.warn(htg, "Timecard.HTG.TravelEarly", date);
//				BigDecimal diff = EARLY_TRAVEL_LIMIT.subtract(travelStart);
//				travel = travel.subtract(diff);
//				travel = travel.max(minCallLeft);
			}
			// Travel beyond minimum call & after 6pm might not count
			if (dailyTime.getWrap() != null && dailyTime.getWrap().compareTo(LATE_TRAVEL_LIMIT) > 0) {
				TimecardService.warn(htg, "Timecard.HTG.TravelLate", date);
//				BigDecimal diff = dailyTime.getWrap().subtract(LATE_TRAVEL_LIMIT);
//				travel = travel.subtract(diff);
//				travel = travel.max(minCallLeft);
			}
		}
		htg.hours = travel.add(work);

		// Travel time beyond minimum call does NOT count towards gold
		travel = travel.min(minCallLeft);
		htg.goldEligHours1 = travel.add(work);
		if (htg.goldHoursType == HoursType.E) { // using Elapsed, not Worked, hours
			// So add meal time for gold-eligible hours
			MpvRule mpvRule = htg.ruleService.findMpvRule(event);
			BigDecimal mealLength = TimecardCalc.calculateMeal1Length(dailyTime, mpvRule.getNdmAllowed());
			if (mealLength != null) {
				htg.goldEligHours1 = htg.goldEligHours1.add(mealLength);
			}
		}
	}

	/**
	 * Calculate the number of work hours for either a Travel/Work or
	 * Work/Travel day.
	 *
	 * @return The number of worked hours (excluding meals).
	 */
	private BigDecimal calculateNonTravelWork() {
		MpvRule mpvRule = htg.ruleService.findMpvRule(event);
		BigDecimal maxMeal = mpvRule.getMaxMealLength();

		BigDecimal work = TimecardCalc.calculateAmHours(dailyTime);
		if (work == null) { // shouldn't happen
			work = BigDecimal.ZERO;
		}
		BigDecimal temp = TimecardCalc.calculatePmHours(dailyTime, maxMeal, mpvRule.getNdmAllowed());
		if (temp != null) {
			work = work.add(temp);
		}
		return work;
	}

}
