/**
 * HtgData.java
 */
package com.lightspeedeps.service;

import java.math.BigDecimal;
import java.util.List;

import com.lightspeedeps.model.*;
import com.lightspeedeps.object.TimecardMessage;
import com.lightspeedeps.type.*;

/**
 * A class to hold data shared among the various classes responsible
 * for HTG calculations.  This just exists while the calculations
 * are being performed; it is not persisted.
 */
public class HtgData {

	/** Standard/default number of work hours in a non-union work week (5x8). */
	static final BigDecimal STANDARD_WEEK_HOURS =  new BigDecimal(40);

	/** Standard/default number of work hours in a union work week (5x12 or 6x10). */
	static final BigDecimal STANDARD_WEEK_HOURS_UNION =  new BigDecimal(60);

	/** The current instance of the TimecardService. */
	public TimecardService service;

	/** The current RuleService instance for this HTG processing cycle. */
	public RuleService ruleService;

	/** The WeeklyTimecard being evaluated in this HTG processing cycle. */
	/*package*/ WeeklyTimecard weeklyTimecard;

	/** The Production to which the timecard belongs. This is not necessarily
	 * the current (logged-in) Production. */
	/*package*/ Production production;

	/** The Project to which the timecard belongs. This is relevant for Commercial
	 * productions. */
	/*package*/ Project project;

	/** The related FormModelRelease, if any. */
	/*package*/ FormModelRelease modelRelease;

	/** The HTG audit-trace event handler currently in use, or null if none in use. */
	/*package*/ AuditEvent auditEvent;

	/** True iff the timecard is for a Commercial (AICP) production. */
	/*package*/ boolean isCommercial;

	/** If true, HTG process was called for Hot-Cost processing */
	/*package*/ boolean processHotCosts = false;

	/** If true, Hot-Cost process is using a re-rate value, not StartForm data */
	/*package*/ boolean hotCostsReRate = false;

	/** The current PayrollPreference to apply. */
	public PayrollPreference preference;

	/** The timecard from the prior week for the same employee & occupation. */
	public WeeklyTimecard priorTimecard;

	/** True iff the prior week's timecard is important, i.e., it could affect
	 * the calculation for this week's gross. Likely to get set to true if
	 * there's a floating work week, or if the production week spans two payroll
	 * weeks. (Other cases exist, too.) When an HTG calculation is completed, if
	 * this is true, and the prior week's timecard exists but was not
	 * calculated, a warning message is issued to the effect of "the preceding
	 * timecard should be calculated first." */
	/*package*/ boolean priorTcImportant = false;

	/** The DailyTime representing the prior day. When processing Sunday (the
	 * first day of the payroll week), this will be the DailyTime for the last
	 * day of the preceding payroll week, i.e., Saturday. This may be used for
	 * determining this Sunday's status as 6th or 7th day, and also for rest
	 * invasion (call-back/forced-call) calculations. */
	/*package*/ DailyTime priorDailyTime;

	/** True if timecard is for a non-union position. */
	/*package*/ boolean nonUnion = false;

	/** True if timecard is for a Team client production; setting copied from the
	 * currently applicable PayrollService. */
	/*package*/ boolean isTeam = false;

	/** True iff timecard is for a weekly-exempt, non-union Start for a Team payroll client.
	 * In this case, we report 5 days paid at 8 hrs each, for 'daily pay breakdown'; only
	 * exception is for "HOA" DayType.  Floating work week flag is ignored. LS-2189 */
	/*package*/ boolean isSimpleWeekly = false;

	/** True iff the current timecard is based on a FormModelReleasePrint instance (typically
	 * called just a "model release form". This is determined based on the fact that the associated
	 * StartForm is linked to a FormModelReleasePrint.  LS-4664 */
	/*package*/ boolean isModelRelease = false;

	/** Enum indicating if current WeeklyTimecard uses rates from Prep or not. */
	/*package*/ ProdOrPrep prodOrPrep;

	/** The type of rounding to apply to calculated overtime hours -- e.g, 1/10th or 1/4 hour; this field
	 * is set once for the entire week, based on the WeeklyRule OT-increment setting. */
	/*package*/ HourRoundingType otRoundingWeek = HourRoundingType.TENTH;
			// Use as: time = NumberUtils.round(time, htg.otRounding, RoundingMode.HALF_UP);

	/** The type of rounding to apply to calculated overtime hours -- e.g, 1/10th or 1/4 hour; this field
	 * is set daily from {@link #otRoundingWeek}, and then it may be modified by Special Rule processing
	 * before being applied to the current day's calculation. */
	/*package*/ HourRoundingType otRounding = HourRoundingType.TENTH;
			// Use as: time = NumberUtils.round(time, htg.otRounding, RoundingMode.HALF_UP);

	/** If true, the hours for this day should be paid all at straight time, and should
	 * not count towards any weekly OT limit.  Typically used for travel hours or similar
	 * situations. */
	/*package*/ boolean onlyStraight = false;

	/** True iff a "layoff event" occurs during the current weeklyTimecard's period. This may be due
	 * to a "Work-Layoff" DayType, or due to the Payroll Start's Effective End or Work End dates.  Note that
	 * for a "simple weekly" employee (non-union, weekly-exempt, Team), the PS end dates will NOT cause
	 * a layoff to be set true, therefore guaranteeing a full week's pay regardless of the
	 * number of days worked. */
	/*package*/ boolean hasLayoff = false;


	// * * * MULTIPLIERS * * *

	/** The multiplier for straight time. */
	/*package*/ BigDecimal strMult;

	/** The multiplier for overtime. */
	/*package*/ BigDecimal otMult;

	/** The multiplier for a second (premium) overtime period. This is
	 * typically for employees with "Day Rate" (Commercial) Start Forms. */
	/*package*/ BigDecimal ot2Mult;

	/** The multiplier for gold time. */
	/*package*/ BigDecimal goldMult;

	/** The multiplier for holiday hours. */
	/*package*/ BigDecimal holidayMult;

	/** The multiplier for holiday gold hours. */
	/*package*/ BigDecimal holidayGoldMult;


	// * * * HOURS * * *

	/** Hours to be paid (worked or guaranteed) */
	/*package*/ BigDecimal hours = null;

	/** Number of hours to pay at straight time. */
	/*package*/ BigDecimal strHours;

	/** Number of hours to pay at OT rate. */
	/*package*/ BigDecimal otHours;

	/** Number of hours to pay at the second (premium) OT rate. This is
	 * typically for employees with "Day Rate" (Commercial) Start Forms.*/
	/*package*/ BigDecimal ot2Hours;

	/** Number of hours to pay at gold rate. */
	/*package*/ BigDecimal goldHours;

	/** Number of hours to pay at the holiday rate.  Used when a work-day starts
	 * on one day and extends into a holiday. */
	/*package*/ BigDecimal holidayHours;

	/** Number of hours to pay at the holiday-gold rate.  Used when a work-day starts
	 * on one day and extends into a holiday, and part or all of the hours on the
	 * holiday fall into gold time. */
	/*package*/ BigDecimal holidayGoldHours;

	/** Number of hours that a work day overlapped into a holiday day (i.e.,
	 * past midnight). */
	/*package*/ BigDecimal holidayOverlap;

	/** Cumulative straight time for the week; updated as each day is broken. */
	/*package*/ BigDecimal weekStrHours = BigDecimal.ZERO;

	/** The number of hours added back to the worked hours because the first meal period
	 * was too early, too short, or too long. */
	public BigDecimal meal1AdjHours;

	/** The number of hours added back to the worked hours because the second meal period
	 * was too early, too short, or too long. */
	public BigDecimal meal2AdjHours;

	/** The time of day up until we effectively paid (in some cases).  Usually the same as wrap,
	 * but will be later if "guaranteed hours after meal" added pay hours. This is used
	 * to adjust pay if "gold pays meals" is in effect. */
	public BigDecimal paidWrap;

	/** Time of day up to which night premium may need to be paid (either wrap time,
	 * or when gold time starts. */
	/*package*/ BigDecimal npEligTime;

	/** The wrap time from the previous day; used in checking for call-backs and
	 * forced calls (rest invasion). */
	/*package*/ BigDecimal lastWrap = BigDecimal.ZERO;

	/** The earliest allowable call time on a day, based on the previous day's
	 * wrap time and the minimum rest period.  Used for calculating rest
	 * invasion. */
	/*package*/ BigDecimal nextMinCall = BigDecimal.ZERO;

	/** The number of additional hours of pay the person should get due to being
	 * called back too soon. For some unions, this comes into effect if the rest
	 * period is less than 4 hours, in which case all the intervening time is
	 * counted as work; in this case, this field will be the number of hours in
	 * that intervening time. */
	/*package*/ BigDecimal callbackHours = null;

	/** Guaranteed hours per day for non-union, non-exempt personnel for the day
	 * being processed.  This is assumed to be non-null during processing. */
	/*package*/ BigDecimal guarHours = BigDecimal.ZERO;

	/** Guaranteed hours per day for non-union, non-exempt personnel based on the
	 * timecard's "studio/location" setting.  When a day's DayType doesn't match the
	 * timecard setting, we need to get the guaranteed hours from the StartForm. */
	/*package*/ BigDecimal defaultGuarHours = null;

	/** Eligible pay hours used to compute gold for the "1st gold break" in the
	 * Golden rules - this may be elapsed or worked hours. Note that some Golden
	 * rules use worked hours for the first break, but use elapsed hours for the
	 * 2nd or 3rd "gold break point." */
	/*package*/ BigDecimal goldEligHours1 = null;

	/** Hours used to compute gold for the "2nd gold break" in the Golden rules
	 * - may be elapsed or worked hours. */
	/*package*/ BigDecimal goldEligHours2 = null;

	/** Hours used to compute gold for the "3rd gold break" in the Golden rules
	 * - may be elapsed or worked hours. */
	/*package*/ BigDecimal goldEligHours3 = null;

	/** How many more hours are needed to reach gold break point. */
	/*package*/ BigDecimal remainToGold = null;

	/** All hours to be paid gold, usually due to forced call following gold hours. */
	/*package*/ boolean allGold = false;

	/** True iff prior day ended with gold hours paid. */
	/*package*/ boolean lastGold = false;

	/** This indicates whether the hours used for determining the gold break-point
	 * are worked or elapsed. */
	/*package*/ HoursType goldHoursType;

	/** How many hours of rest time were "invaded" -- not given as off time. */
	/*package*/ BigDecimal invadedRest = null;

	/** Regarding a "long rest" rule for periods between production work weeks
	 * (i.e., after a 5th or 6th day worked) -- don't check for this again, as
	 * we've already processed a work day after the start of the production week
	 * where the rest invasion could have occurred. It can't happen a second
	 * time in this week. */
	/*package*/  boolean skipLongRest = false;

	/** The multiplier used for the last hours on the prior day, e.g., 1.5x or 2.0x.
	 * Used in some cases for the rate to be paid for invaded hours. */
	/*package*/ BigDecimal lastMult = null;

	/** The number of hours (per day) that an exempt employee will be assigned when
	 * they have just checked the "worked" box and not entered any hours. */
	/*package*/ BigDecimal exemptHours = null;

	/** Hourly Pay rate ($/hour) for daily non-exempt employee calculated when no
	 * hourly rate is supplied in the Start Form. */
	/*package*/ BigDecimal extendedRate = null;

	/*package*/ List<TimecardMessage> messages;


	//    * * *     SPECIAL RULE FLAGS    * * *

	/** Report time for a timecard with a Daily rate, non-exempt, as Flat. Also, if
	 * no hourly rate is specified in the Start Form, compute it from the Daily rate.
	 * This flag is set by a SpecialRule. */
	/*package*/  boolean dailyFlat;

	/** If true, payment for a "6th day" is flexible, based on the number of days
	 * actually worked -- "6th day rate" is only paid if at least 5 previous days
	 * in the production week were worked.  If false, it is NOT flexible - the 6th
	 * day of the production week always pays the "6th day rate", even if the user
	 * has worked less than 5 days previously. By default, this is true for non-union
	 * timecards, or if the production is not using contract-assigned rules. */
	/*package*/  boolean flexible6thDay;

	/** If true, the employee's work week is allowed to "float", so that it is
	 * not tied to the Production work week, but dependent only on what days
	 * this employee worked. Normally set via a Special Rule.  Note that if 'floatingWeek'
	 * is true, 'flexible6thDay' must also be true. */
	/*package*/  boolean floatingWeek;

	/** Gold time includes meal times; set via a Special rule. Used for Videotape agreement.
	 * This flag is set by a SpecialRule.*/
	/*package*/ boolean goldPaysMeals;

	/** Flag set by SPECIAL rule to force the following day to be paid
	 * as a worked day. */
	/*package*/ boolean nextDayIsWork;

	/** Mark OT pay breakdown lines as "Extended Day" instead of just "n.nx Hours". This flag
	 * is set by a SpecialRule. */
	/*package*/  boolean otExtended;

	/** When true, a timecard with a weekly guarantee may be pro-rated for the days worked
	 * at 1/5W per day.  This flag is set by a SpecialRule. */
	/*package*/  boolean proRateFraction;

	/** Flag set by SPECIAL rule to skip an On-Call guarantee. */
	/*package*/ boolean skipOcGuarantee;


	/** See {@link #weeklyTimecard}. */
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}

	/** See {@link #lastWrap}. */
	public BigDecimal getLastWrap() {
		return lastWrap;
	}

	/** See {@link #flexible6thDay}. Setter used for JUnit tests. */
	public void setFlexible6thDay(boolean flexible6thDay) {
		this.flexible6thDay = flexible6thDay;
	}

	/** See {@link #floatingWeek}. Setter used for JUnit tests. */
	public void setFloatingWeek(boolean floatingWeek) {
		this.floatingWeek = floatingWeek;
	}

}
