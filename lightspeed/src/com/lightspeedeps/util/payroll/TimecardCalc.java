package com.lightspeedeps.util.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.*;
import com.lightspeedeps.service.HtgData;
import com.lightspeedeps.type.DayType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * Superclass for the IndivTimecardBean, FullTimecardBean, and
 * MobileTimecardBean classes, which are the backing beans for the "basic",
 * "full", and several mobile timecard pages. This class is designed to
 * encapsulate all the code common to those pages, such as Edit, Save, Submit,
 * Approve, Reject, and data validation.
 */
public class TimecardCalc {
	/** */
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TimecardCalc.class);

	private TimecardCalc() { // prevent instantiation
	}

	/**
	 * Calculate the number of worked hours in a day and set it in the DailyTime
	 * provided.
	 *
	 * @param dt The DailyTime instance to be evaluated and updated.
	 */
	public static void calculateDailyHours(DailyTime dt) {
		dt.setHours(null);
		if (dt.getCallTime() != null && dt.getWorkEnd() != null) {
			BigDecimal hoursVal = dt.getWorkEnd().subtract(dt.getCallTime()).subtract(dt.getOnCallGap());
			if (dt.getM1In() != null && dt.getM1Out() != null) {
				hoursVal = hoursVal.subtract(dt.getM1In().subtract(dt.getM1Out()));
			}
			if (dt.getM2In() != null && dt.getM2Out() != null) {
				hoursVal = hoursVal.subtract(dt.getM2In().subtract(dt.getM2Out()));
			}
			dt.setHours(hoursVal);
		}
		else if (dt.getDayType() != null && dt.getDayType().getHours() != null) {
			BigDecimal hours = dt.getDayType().getHours();
			if (hours.signum() == 0) { // if DayType says '0', use non-union guar hours (null for union)
				hours = TimecardCheck.calculateNonUnionGuarHours(dt.getWeeklyTimecard());
			}
			dt.setHours(hours);
		}
	}

	/**
	 * Calculate the number of worked hours from the beginning of the day (call
	 * time) up to the time given.
	 *
	 * @param dt The DailyTime entry to use for the calculation.
	 * @param end The ending time; the time calculated is from the dt.callTime
	 *            up to the split time value.
	 * @param htg The current HtgData instance.
	 * @return the number of worked hours from the dt.callTime up to the split
	 *         time value.
	 */
	public static BigDecimal calculateWorkedHours(DailyTime dt, BigDecimal end, HtgData htg) {
		return calculateWorkedHours(dt, dt.getCallTime(), end, htg);
	}

	/**
	 * Calculate the number of worked hours between any two given times in the
	 * work day.
	 *
	 * @param dt The DailyTime entry to use for the calculation.
	 * @param start The starting time; the time calculated is from this time up
	 *            to the given ending time.
	 * @param end The ending time.
	 * @param htg The current HtgData instance.
	 * @return the number of worked hours from the start time up to the end
	 *         time. This is the number of elapsed hours from the start to the
	 *         end, minus any meal time(s) that occur between the start and end
	 *         times.
	 */
	public static BigDecimal calculateWorkedHours(DailyTime dt, BigDecimal start, BigDecimal end, HtgData htg) {
		BigDecimal hours = BigDecimal.ZERO;
		if (dt.getCallTime() != null && dt.getWrap() != null) { // have a (reasonably) valid day...
			hours = end.subtract(start); // this is correct if no meals overlap or intervene.
			hours = subtractMealTime(hours, start, end, dt.getM1Out(), dt.getM1In(), htg.meal1AdjHours);
			hours = subtractMealTime(hours, start, end, dt.getM2Out(), dt.getM2In(), htg.meal2AdjHours);
		}
		return hours;
	}

	/**
	 * Given a number of hours in the current day, from 'start' to 'end', reduce
	 * those hours by the amount of time that overlaps the specified meal period
	 * (if any).
	 *
	 * @param hours The number of hours from start to end, less any meal times
	 *            subtracted so far.
	 * @param start The starting time of the period being calculated.
	 * @param end The ending time of the period being calculated.
	 * @param mealStart The starting time of the meal to consider.
	 * @param mealEnd The ending time of the meal to consider.
	 * @param mealAdjHours The number of hours the length of the meal might have
	 *            been adjusted due to 'meal too short', 'meal too long' or
	 *            other rule violations. May not be null.
	 * @return The number of hours given, less the amount of time that the
	 *         start/end times overlap with the specified meal times.
	 */
	private static BigDecimal subtractMealTime(BigDecimal hours, BigDecimal start, BigDecimal end, BigDecimal mealStart, BigDecimal mealEnd, BigDecimal mealAdjHours) {
		if (mealEnd != null && mealStart != null && start.compareTo(mealEnd) < 0) {
			// have valid meal, and 'start' is before meal ends, may need to adjust hours.
			boolean adjust = true; // Probably need to adjust hours.
			if (mealAdjHours.signum() != 0) {
				adjust = false; // probably the whole meal has become non-deductible;
				// that's the most common case, e.g., caused by meal too short, or too soon.
				BigDecimal mealLen = mealEnd.subtract(mealStart);
				if (mealLen.compareTo(mealAdjHours) != 0) { // Hmm, whole meal NOT non-deductible...
					// this is an unusual case, currently only happens if a meal is longer than the max allowed.
					adjust = true;
					// change meal to end sooner, this should make it a valid meal period.
					mealEnd = mealEnd.subtract(mealAdjHours);
				}
			}
			if (adjust) {
				if (start.compareTo(mealStart) <= 0) {
					// 'start' is before meal begins.
					if (end.compareTo(mealStart) > 0) {
						// 'end' is after meal begins, will need to adjust
						if (end.compareTo(mealEnd) >= 0) {
							// 'end' is after meal ends, subtract all of meal 2.
							hours = hours.subtract(mealEnd.subtract(mealStart));
						}
						else {
							// 'end' is in the middle of meal; subtract time from beginning of meal til 'end'
							hours = hours.subtract(end.subtract(mealStart));
						}
					}
				}
				else {
					// 'start' is during meal
					if (end.compareTo(mealEnd) > 0) {
						// 'end' is after meal ends, subtract time from 'start' until end of meal.
						hours = hours.subtract(mealEnd.subtract(start));
					}
					else {
						// 'end' is in the middle of meal, so no work time ('start' and 'end' both within meal)
						hours = BigDecimal.ZERO;
					}
				}
			}
		}
		return hours;
	}

	/**
	 * Calculate the elapsed hours from call time to wrap.
	 *
	 * @param dt The DailyTime whose hours will be used to do the calculation.
	 * @return The number of elapsed hours from call time to wrap.
	 */
	public static BigDecimal calculateElapsedHours(DailyTime dt) {
		BigDecimal hours = BigDecimal.ZERO;
		if (dt.getCallTime() != null && dt.getWrap() != null) {
			hours = dt.getWrap();
			if (hours.compareTo(dt.getCallTime()) < 0) {
				hours = BigDecimal.ZERO;
			}
			else {
				hours = hours.subtract(dt.getCallTime());
			}
		}
		return hours;
	}

	/**
	 * Calculate the number of hours from the start of work (usually call time)
	 * until the first meal time, or until wrap if no meal time is recorded.
	 *
	 * @param dt The DailyTime entry to use for the calculations.
	 * @return The number of hours worked from the start of work until the first
	 *         recorded work "out" time, which may be either m1out, m2out, or
	 *         wrap. Returns null if no call time is recorded, or if none of
	 *         m1out, m2out, or wrap is recorded. If the DayType is Travel/Work,
	 *         the "start of work" will be m1in, if recorded, else m2in.
	 */
	public static BigDecimal calculateAmHours(DailyTime dt) {
		BigDecimal hoursVal = null;
		if (dt.getCallTime() != null) {
			if (dt.getDayType() == DayType.TW) {
				// Travel/Work: call to m1Out is travel time...
				// AM is then first available "in to out" range:
				//   either m1In to m2Out, or m1In to wrap, or m2In to wrap (if no m1In)
				if (dt.getM1In() != null) {
					if (dt.getM2Out() != null) {
						hoursVal = dt.getM2Out().subtract(dt.getM1In());
					}
					else if (dt.getWorkEnd() != null) {
						hoursVal = dt.getWorkEnd().subtract(dt.getM1In()).subtract(dt.getOnCallGap());
					}
				}
				else if (dt.getM2In() != null) {
					if (dt.getWorkEnd() != null) {
						hoursVal = dt.getWorkEnd().subtract(dt.getM2In()).subtract(dt.getOnCallGap());
					}
				}
			}
			else {
				if (dt.getM1Out() != null) {
					hoursVal = dt.getM1Out().subtract(dt.getCallTime());
				}
				else if (dt.getM2Out() != null) {
					hoursVal = dt.getM2Out().subtract(dt.getCallTime());
				}
				else if (dt.getWorkEnd() != null) {
					hoursVal = dt.getWorkEnd().subtract(dt.getCallTime()).subtract(dt.getOnCallGap());
				}
			}
		}
		return hoursVal;
	}

	/**
	 * Calculate the number of hours from the call time or the NDB-end until the
	 * first meal time.
	 *
	 * @param dt The DailyTime entry to use for the calculations.
	 * @param allowNdm True if NDM/NDB values should be respected; if false,
	 *            they are ignored for the purpose of calculating worked hours.
	 * @return The number of hours worked in the "morning", following a
	 *         non-deductible breakfast, if any. This is used for determining
	 *         meal-period violations.
	 */
	public static BigDecimal calculateAmMealHours(DailyTime dt, boolean allowNdm) {
		BigDecimal hoursVal = null;
		if (dt.getCallTime() != null) {
			BigDecimal in = dt.getCallTime();
			if (allowNdm && dt.getNonDeductMealPayroll() && dt.getNdbEnd() != null
					&& dt.getNdbEnd().compareTo(dt.getCallTime()) > 0) {
				in = dt.getNdbEnd();
			}
			if (dt.getM1Out() != null) {
				hoursVal = dt.getM1Out().subtract(in);
			}
			else if (allowNdm && dt.getNonDeductMeal2Payroll() && dt.getNdmStart() != null) {
				hoursVal = dt.getNdmStart().subtract(in);
			}
			else if (dt.getM2Out() != null) {
				hoursVal = dt.getM2Out().subtract(in);
			}
			else if (dt.getWrap() != null) {
				hoursVal = dt.getWrap().subtract(in);
			}
		}
		return hoursVal;
	}

	/**
	 * Calculate the number of elapsed hours from the end of the first meal
	 * until either the beginning of the second meal, or wrap time. If the
	 * DailyTime has a lastManIn value later than the end of the first meal, the
	 * lastManIn value is used instead.
	 *
	 * @param dt The DailyTime entry to be analyzed.
	 * @param maxMealLen The maximum allowable length of the meal. If the meal
	 *            period is longer than this, the work time is considered to
	 *            begin at "maxMealLen" after the beginning of the meal. If this
	 *            is null, the meal length is not checked.
	 * @param allowNdm True if an NDM is allowed, in which case the NDM start
	 *            may be treated as the start of the next meal ("dinner"). If
	 *            this is false, any NDM data in the DailyTime object is
	 *            ignored.
	 * @return the number of hours from the end of the first meal until the next
	 *         stopping point, which will be either the beginning of the second
	 *         meal, or the wrap time; returns null if there was no first meal.
	 *         If a "lastManIn" time was specified, then that time (rather than
	 *         the "meal1In" time) will be used as the time for the end of the
	 *         first meal.
	 * @see #calculateAmHours(DailyTime)
	 * @see #calculateWrapHours(DailyTime,BigDecimal,boolean)
	 */
	public static BigDecimal calculatePmHours(DailyTime dt, BigDecimal maxMealLen, boolean allowNdm) {
		BigDecimal hoursVal = null;
		if (dt.getDayType() == DayType.WT) { // Work/Travel day
			// Only have PM hours on Work/Travel if 2 meal times recorded,
			// as 1st one will be real meal, and 2nd is work end/travel begin.
			if (dt.getM1Out() != null && dt.getM1In() != null && dt.getM2Out() != null) {
				BigDecimal in = dt.getM1In();
				if (maxMealLen != null &&
						in.subtract(dt.getM1Out()).compareTo(maxMealLen) > 0) {
					in = dt.getM1Out().add(maxMealLen);
				}
				hoursVal = dt.getM2Out().subtract(in);
			}
		}
		else if (dt.getDayType() == DayType.TW) { // Travel/Work day
			if (dt.getM1In() != null && dt.getM2In() != null) { // have a meal during work part
				BigDecimal in = dt.getM2In();
				if (maxMealLen != null && dt.getM2Out() != null &&
						in.subtract(dt.getM2Out()).compareTo(maxMealLen) > 0) {
					in = dt.getM2Out().add(maxMealLen);
				}
				hoursVal = dt.getWrap().subtract(in);
			}
		}
		else { // normal work day (no travel)
			BigDecimal in = null;
			BigDecimal out = null;
			if (dt.getM1In() != null) {
				in = dt.getM1In();
				out = dt.getM1Out();
				if (dt.getLastManIn() != null && dt.getLastManIn().compareTo(in) > 0) {
					in = dt.getLastManIn();
				}
			}
			if (in == null && allowNdm && dt.getNonDeductMeal2Payroll() && dt.getNdmEnd() != null) {
				// NDM is first meal
				in = dt.getNdmEnd();
				out = dt.getNdmStart();
			}
			if (in != null) {
				if (maxMealLen != null && out != null &&
						in.subtract(out).compareTo(maxMealLen) > 0) {
					in = out.add(maxMealLen);
				}
				if (dt.getM2Out() != null) {
					hoursVal = dt.getM2Out().subtract(in);
				}
				else if (allowNdm && dt.getNonDeductMeal2Payroll() && dt.getNdmStart() != null) { // NDM is second meal
					hoursVal = dt.getNdmStart().subtract(in);
				}
				else if (dt.getWrap() != null) {
					hoursVal = dt.getWrap().subtract(in);
				}
			}
		}
		return hoursVal;
	}

	/**
	 * Calculate the number of elapsed hours from the end of a second meal until
	 * wrap time.
	 *
	 * @param dt The DailyTime entry to be analyzed.
	 * @param maxMealLen The maximum allowable length of the meal. If the meal
	 *            period is longer than this, the work time is considered to
	 *            begin at "maxMealLen" after the beginning of the meal.
	 * @param allowNdm True if an NDM is allowed, in which case the NDM start
	 *            may be treated as the start of the next meal ("dinner"). If
	 *            this is false, any NDM data in the DailyTime object is
	 *            ignored.
	 * @return the number of hours from the end of the second meal until wrap
	 *         time; returns null if there was no second meal. Returns null for
	 *         any travel-type day, as a second meal cannot be recorded in that
	 *         case.
	 * @see #calculatePmHours(DailyTime,BigDecimal,boolean)
	 */
	public static BigDecimal calculateWrapHours(DailyTime dt, BigDecimal maxMealLen, boolean allowNdm) {
		BigDecimal hoursVal = null;
		BigDecimal in = null;
		BigDecimal out = null;
		if (dt.getM2In() != null && (! dt.getDayType().includesTravel())) {
			in = dt.getM2In();
			out = dt.getM2Out();
		}
		else if (allowNdm && dt.getNonDeductMeal2Payroll() && dt.getNdmEnd() != null) {
			// Use NDM as second meal
			in = dt.getNdmEnd();
			out = dt.getNdmStart();
		}
		if (in != null) {
			if (maxMealLen != null && out != null &&
					in.subtract(out).compareTo(maxMealLen) > 0) {
				in = out.add(maxMealLen);
			}
			if (dt.getWrap() != null) {
				hoursVal = dt.getWrap().subtract(in);
			}
		}
		return hoursVal;
	}

	/**
	 * Calculate the length of the first meal.
	 *
	 * @param dt The DailyTime entry to use for the calculations.
	 * @param allowNdm True if an NDM is allowed, in which case the NDM start
	 *            may be treated as the start of the next meal ("dinner"). If
	 *            this is false, any NDM data in the DailyTime object is
	 *            ignored.
	 * @return The number of hours within the "meal 1" time, except that for
	 *         Travel/Work, it will be the length of the "meal 2" time recorded.
	 *         Returns null if no first meal is recorded.
	 */
	public static BigDecimal calculateMeal1Length(DailyTime dt, boolean allowNdm) {
		BigDecimal hoursVal = null;
		if (dt.getDayType() == DayType.TW) {
			// Travel/Work: from call-time to m1Out is travel time...
			// "meal 1" is then what's recorded as "meal 2"
			if (dt.getM2In() != null && dt.getM2Out() != null) {
				hoursVal = dt.getM2In().subtract(dt.getM2Out());
			}
		}
		else if (dt.getDayType() == DayType.WT) {
			// Work/Travel: travel ends at wrap, might start at m1in or m2in...
			if (dt.getM2In() != null && dt.getM1In() != null) {
				// both meal times recorded, so 1st meal IS a real meal.
				hoursVal = dt.getM1In().subtract(dt.getM1Out());
			}
			// (Otherwise meal 1 is just the end of work & beginning of travel,
			// so there is no actual meal 1, and we return null.
		}
		else {
			if (dt.getM1In() != null && dt.getM1Out() != null) {
				hoursVal = dt.getM1In().subtract(dt.getM1Out());
			}
			else if (allowNdm && dt.getNonDeductMeal2Payroll() &&
					dt.getNdmEnd() != null &&
					dt.getNdmStart() != null) {
				hoursVal = dt.getNdmEnd().subtract(dt.getNdmStart());
			}
		}
		return hoursVal;
	}

	/**
	 * Calculate the length of the second meal.
	 *
	 * @param dt The DailyTime entry to use for the calculations.
	 * @param allowNdm True if an NDM is allowed, in which case the NDM start
	 *            may be treated as the start of the next meal ("dinner"). If
	 *            this is false, any NDM data in the DailyTime object is
	 *            ignored.
	 * @return The number of hours within the "meal 2" time, except that for any
	 *         travel-type day, it will be null, as there is no place to record
	 *         a second meal in that case. Also returns null if there is no m2in
	 *         or m2out times recorded.
	 */
	public static BigDecimal calculateMeal2Length(DailyTime dt, boolean allowNdm) {
		BigDecimal hoursVal = null;
		if (! dt.getDayType().includesTravel()) {
			if (dt.getM2In() != null && dt.getM2Out() != null) {
				hoursVal = dt.getM2In().subtract(dt.getM2Out());
			}
			else if (allowNdm && dt.getNonDeductMeal2Payroll() &&
					dt.getM1In() != null && // there's a Meal 1 entry,
					dt.getNdmEnd() != null && // so if there's an NDM, treat it as meal 2
					dt.getNdmStart() != null) {
				hoursVal = dt.getNdmEnd().subtract(dt.getNdmStart());
			}
		}
		return hoursVal;
	}

	/**
	 * Determine if meal 2 is a non-deductible meal.
	 *
	 * @param dt The DailyTime to be examined.
	 * @param rule The MpvRule currently in effect.
	 * @param isCommercial The timecard being evaluated is from a Commercial
	 *            production.
	 * @return True iff meal 2 meets the qualifications for a non-deductible
	 *         meal.
	 */
	public static boolean meal2IsNdm(DailyTime dt, MpvRule rule, boolean isCommercial) {
		boolean isNdm = false;
		if (dt.getNonDeductMeal2Payroll() &&
				(! dt.getDayType().includesTravel()) &&
				dt.getM1In() != null && // there's a Meal 1 entry,
				dt.getWrap() != null) { // and a wrap ...
			if (ndmMatches(dt, isCommercial)) {
				if (rule.getMaxToWrapForNdm2().signum() > 0) {
					BigDecimal time = dt.getM1In().add(rule.getMaxTo2ndMeal()); // required start of meal 2
					time = time.add(rule.getMaxToWrapForNdm2()); // latest allowed wrap for NDM to be non-deductible
					if (time.compareTo(dt.getWrap()) >= 0) { // wrap is soon enough
						isNdm = true;	// so NDM is ok as non-deductible
					}
				}
				else { // no check necessary for wrap time...
					isNdm = true; // NDM is ok as non-deductible
				}
			}
		}
		return isNdm;
	}

	/**
	 * Determine if the payroll-entered NDM matches the user-entered Meal 2.
	 *
	 * @param dt The DailyTime to be examined.
	 * @param isCommercial The timecard being evaluated is from a Commercial production.
	 * @return True if the employee did not report a meal 2 at all, or the
	 *         reported meal matches the payroll-entered NDM times.
	 */
	public static boolean ndmMatches(DailyTime dt, boolean isCommercial) {
		boolean match = false;
		boolean haveMeal2 = (dt.getM2In() != null && dt.getM2Out() != null);

		if (haveMeal2 && isCommercial) {
			// no place for ndmStart/End to be entered; assume meal 2 is accurate.
			match = true;
		}
		if (dt.getNdmEnd() != null && dt.getNdmStart() != null) {
			if (dt.getM2In() == null && dt.getM2Out() == null) {
				match = true; // no employee-reported meal, so no conflict
			}
			else if (haveMeal2) {
				if (dt.getM2Out().compareTo(dt.getNdmStart()) == 0 &&
						dt.getM2In().compareTo(dt.getNdmEnd()) == 0) {
					match = true; // employee times and NDM times match exactly
				}
				else {
					BigDecimal m2Len = dt.getM2In().subtract(dt.getM2Out());
					BigDecimal ndmLen = dt.getNdmEnd().subtract(dt.getNdmStart());
					if (m2Len.compareTo(ndmLen) >= 0) {
						// employee reported meal is equal to or longer then NDM report
						match = true; // no complaints for getting paid extra
					}
				}
			}
		}
		return match;
	}

	/**
	 * Calculate the number of hours spent traveling (or guaranteed for travel
	 * time) when the DayType is a travel type (including Travel/Work and
	 * Work/Travel).
	 *
	 * @param dt The DailyTime to be evaluated.
	 * @return The number of travel hours.
	 */
	public static BigDecimal calculateTravelHours(DailyTime dt) {
		BigDecimal hoursVal = BigDecimal.ZERO;
		if (dt.getDayType().includesTravel()) {
			if (dt.getDayType() == DayType.T4 || dt.getDayType() == DayType.T7 || dt.getDayType() == DayType.T8) {
				hoursVal = dt.getDayType().getHours();
			}
			else if (dt.getDayType() == DayType.TR || dt.getDayType() == DayType.TP || dt.getDayType() == DayType.TU) {
				hoursVal = calculateElapsedHours(dt);
			}
			else if (dt.getDayType() == DayType.TW) {
				BigDecimal travelEnd = dt.getM1Out();
				if (travelEnd == null) {
					travelEnd = dt.getM2Out();
				}
				if (travelEnd != null && dt.getCallTime() != null) {
					hoursVal = travelEnd.subtract(dt.getCallTime());
				}
			}
			else if (dt.getDayType() == DayType.WT) {
				BigDecimal travelStart = dt.getM2In();
				if (travelStart == null) {
					travelStart = dt.getM1In();
				}
				if (travelStart != null && dt.getWrap() != null) {
					hoursVal = dt.getWrap().subtract(travelStart);
				}
			}
		}
		return hoursVal;
	}

	/**
	 * Calculate the totals that are based on accumulation of the
	 * daily hours and MPVs.
	 */
	public static void calculateWeeklyTotals(WeeklyTimecard wtc) {
		// Calculate Total Hours
		BigDecimal total = null;
		short totalMp1 = 0;
		short totalMp2 = 0;
		short totalMp3 = 0;
		short totalMpUser = 0;
		BigDecimal exemptHours = null;
		boolean allExempt = wtc.getAllowWorked(); // for exempt employee, assume only checked "worked"
		for (DailyTime daily : wtc.getDailyTimes()) {
			if (daily.getWorked()) { // Checked "worked" box (no explicit hours entered)
				if (exemptHours == null) {
					exemptHours = TimecardCheck.calculateExemptHours(wtc);
				}
				total = NumberUtils.safeAdd(total, exemptHours);
			}
			else {
				total = NumberUtils.safeAdd(total, daily.getHours());
				if (daily.getHours() != null) { // explicit hours entered
					allExempt = false;
				}
			}
			totalMp1 += (daily.getMpv1Payroll()==null ? 0 : daily.getMpv1Payroll());
			totalMp2 += (daily.getMpv2Payroll()==null ? 0 : daily.getMpv2Payroll());
			totalMp3 += (daily.getMpv3Payroll()==null ? 0 : daily.getMpv3Payroll());
			if (daily.getState() == null || daily.getState().trim().length() == 0) {
				daily.setState(wtc.getStateWorked());
			}
			if (daily.getMpvUser() != null) {
				String mpv = daily.getMpvUser().trim();
				int iMpv = 0;
				try { // non-numerics are allowed in user MPV fields
					iMpv = Integer.parseInt(mpv);
				}
				catch(NumberFormatException e) {
				}
				if (iMpv > 0 && iMpv <= Constants.MAX_DAILY_MPV) {
					totalMpUser += iMpv;
				}
			}

		}
		wtc.setAllExempt(allExempt);
		wtc.setTotalHours(total);
		wtc.setTotalMpv1(totalMp1);
		wtc.setTotalMpv2(totalMp2);
		wtc.setTotalMpv3(totalMp3);
		wtc.setTotalMpvUser(totalMpUser);
	}


	/**
	 * Calculate the total hours by rate (1.0, 1.5, etc.) within each PayJob, by
	 * summing up the PayJobDaily entries.
	 *
	 * @param wtc The WeeklyTimecard whose PayJob totals should be calculated.
	 */
	public static void calculatePayJobHours(WeeklyTimecard wtc) {
		for (PayJob pj : wtc.getPayJobs()) {
			pj.calculateTotals();
		}
	}


	/**
	 * Calculate the total hours by rate (1.0, 1.5, etc.) within each PayJob, by
	 * summing up the PayJobDaily entries. Then determine if this total matches
	 * the employee's total reported hours.
	 * <p>
	 * This method sets the WeeklyTimecard.jobHoursDiffer boolean to true if any
	 * of these are true: (a) there are no PayJobs created yet; (b) no hours are
	 * entered in the PayJobs yet (the total of all PayJob hours is zero); or
	 * (c) the total of the PayJob hours matches the WeeklyTimecard.totalHours
	 * field.
	 *
	 * @param wtc The WeeklyTimecard whose totals should be calculated.
	 */
	public static void calculateAndCheckJobHours(WeeklyTimecard wtc) {
		calculatePayJobHours(wtc);
		wtc.setJobHoursDiffer(! TimecardCheck.validateJobHours(wtc));
	}

	/**
	 * Calculate all types of totals for use in the Full time card, including
	 * expenses and advances, Job Table totals, Pay Breakdown totals, and grand totals.
	 */
	public static void calculateOtherTotals(WeeklyTimecard wtc) {
		// Calculate and update expense & reimbursement line totals
		wtc.calculateExpenses();
		// Calculate all Pay Breakdown line item totals
		calculatePayBreakdown(wtc);
		// Calculate all Pay Job total items.
		calculatePayJobHours(wtc);
	}

	/**
	 * Calculate the line totals for all the Pay Breakdown line items, and set
	 * the 'grand total' to their sum. Note that labor items line totals are
	 * rounded to 4 decimal places, while non-labor line item totals are rounded
	 * to 2 decimal places. This ensures that when we're using hourly rates that
	 * were "backed into" from a weekly guarantee, we'll end up with the right
	 * labor (weekly) total. (rev 2.9.5386)
	 *
	 * @param wtc The timecard being calculated.
	 */
	public static void calculatePayBreakdown(WeeklyTimecard wtc) {
		// Calculate pay breakdown detail line totals
		// Detail labor line totals are rounded to 4 decimal places.
		// Detail non-labor line totals are rounded to 2 decimal places.
		BigDecimal payTot = BigDecimal.ZERO;

		for (PayBreakdown pb : wtc.getPayLines()) {
			BigDecimal total = pb.calculateTotal();
			if (pb.getCategoryType().includedInTotal()) { // LS-2142
				payTot = payTot.add(total);
			}
		}
		// Grand total -- round to 2 decimal places
		wtc.setGrandTotal(NumberUtils.scaleTo2Places(payTot));
	}

	/**
	 * Calculate and return the number of "work days" in the given timecard. A
	 * work day is any day that has hours reported, or where the "Worked" flag
	 * is true.
	 *
	 * @param wtc The timecard of interest.
	 * @return The number of work days on the timecard, from 0 to 7.
	 */
	public static int calculateNumberOfWorkDays(WeeklyTimecard wtc) {
		return calculateNumberOfWorkDays(wtc, 1);
	}

	/**
	 * Calculate and return the number of "work days" in the given timecard
	 * starting at a specific day within the week. A work day is any day that
	 * has hours reported, or where the "Worked" flag is true.
	 *
	 * @param wtc The timecard of interest.
	 * @param dayNum The day number index (always 1-7) of the first day in the
	 *            timecard to be examined.
	 * @return The number of work days on the timecard, starting from the
	 *         specified day through Saturday, from 0 to 7.
	 */
	public static byte calculateNumberOfWorkDays(WeeklyTimecard wtc, int dayNum) {
		byte days = 0;
		for (DailyTime daily : wtc.getDailyTimes()) {
			if (daily.getDayNum() >= dayNum) {
				if (daily.reportedWork() ||
						(daily.getDayType() != null && daily.getDayType().getHours() != null)) {
					days++;
				}
			}
		}
		return days;
	}

	/**
	 * Calculate a daily rate based on a given hourly rate and the number of
	 * guaranteed hours. Time after 8 hours is assumed to be paid at 1.5x. No
	 * consideration is made for gold time.
	 *
	 * @param hourlyRate The hourly rate to use.
	 * @param guarHours The number of guaranteed hours.
	 * @return null if either input parameter is null, otherwise a computed
	 *         daily rate.
	 */
	public static BigDecimal calculateDailyRate(BigDecimal hourlyRate, BigDecimal guarHours) {
		BigDecimal dayRate = null;
		if (hourlyRate != null && guarHours != null) {
			dayRate = hourlyRate.multiply(Constants.WORKED_HOURS_NON_UNION);
			guarHours = guarHours.subtract(Constants.WORKED_HOURS_NON_UNION);
			if (guarHours.signum() > 0) {
				hourlyRate = hourlyRate.multiply(guarHours).multiply(Constants.DECIMAL_ONE_FIVE);
				dayRate = dayRate.add(hourlyRate);
			}
			dayRate = dayRate.setScale(2, RoundingMode.HALF_UP);
		}
		return dayRate;
	}

}
