/**
 * NightPremiumService.java
 */
package com.lightspeedeps.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.lightspeedeps.model.*;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;

/**
 * Calculates Night Premium data for timecards.
 */
public class NightPremiumService extends BaseService {

	/** The HtgData instance has most of the information needed to do the
	 * PayJob processing. */
	private HtgData htg;

	private NtPremiumRule npRule;

	/** Number of straight-time hours paid at low night-premium rate. */
	private BigDecimal hours10p1;

	/** Number of straight-time hours paid at high night-premium rate. */
	private BigDecimal hours10p2;

	/** Number of overtime hours paid at low night-premium rate. */
	private BigDecimal hours15p1;

	/** Number of overtime hours paid at high night-premium rate. */
	private BigDecimal hours15p2;

	/** This controls whether NP hours that occur during "OT2" time should be
	 * recorded separately from NP hours occurring during "OT" time.  No one currently
	 * needs this feature, and it causes a problem for UDA if enabled, so we'll leave
	 * it false for now. (This allows us to keep the code intact.) */
	private boolean splitNpOT2Hours = false;

	public NightPremiumService(HtgData htg1) {
		htg = htg1;
	}

	/**
	 * Calculate the Night Premium for the specified day. The values calculated
	 * will be placed into the PayJob entry that corresponds to the first job
	 * number for the given date.
	 *
	 * @param dt The DailyTime instance for the day being calculated.
	 * @param rule The NightPremium rule in effect for this day.
	 * @return the last NP rate paid, or 1.0 if no NP paid. This may be used
	 *         when calculating the next day's pay after a forced call.
	 */
	public BigDecimal calculateNightPremium(DailyTime dt, NtPremiumRule rule) {

		/** goldRate: The rate multiplier for "gold" time; this is compared
		 * against the current Night Premium rule's "noNpLimit" when determining
		 * if NP applies to gold time. (Typically the 'noNpLimit' value is 2.0,
		 * and the golden rate will be 2.0 or higher, so NP will not apply to
		 * gold time.) */
		BigDecimal goldRate = htg.goldMult;

		/** return value - the last NP rate paid, or 1.0 if no NP paid. */
		BigDecimal lastNpRate= BigDecimal.ONE;

		npRule = rule;
		if (npRule != null) {
			hours10p1 = null;
			hours10p2 = null;
			hours15p1 = null;
			hours15p2 = null;

			if (npRule.getNoNpLimit().signum() == 0) {
				// No night premium at all (rate limit is 0)
			}
			else {
				if (htg.goldHours != null && goldRate != null) {
					// We have some gold time...
					if (goldRate.compareTo(npRule.getNoNpLimit()) >= 0) {
						// Golden rate is >= NP rate limit (typical): don't pay NP on gold
						if (htg.allGold) {
							// all hours are gold, no NP to calculate
							htg.npEligTime = null;
						}
						else {
							// back up end of NP time from wrap to when gold starts;
							// e.g., if wrap was 11pm, but 1 hr of gold, then NP only pays til 10pm
							htg.npEligTime = htg.npEligTime.subtract(htg.goldHours);
						}
					}
				}
				if (htg.npEligTime != null) {
					if (npRule.getCallStart() == null) {
						// Use case B method (NP based on any work during specific times; local 695)
						calculateNightPremiumB(dt);
					}
					else {
						// Use case A method (typical, NP varies with call & wrap times)
						calculateNightPremiumA(dt);
					}
					if (hours10p1 != null && hours10p1.signum() > 0) {
						lastNpRate = lastNpRate.max(npRule.getNpRate());
					}
					if (hours10p2 != null && hours10p2.signum() > 0) {
						lastNpRate = lastNpRate.max(npRule.getLateRate());
					}
					if (hours15p1 != null && hours15p1.signum() > 0) {
						lastNpRate = lastNpRate.max(npRule.getNpRate().multiply(htg.otMult));
					}
					if (hours15p2 != null && hours15p2.signum() > 0) {
						lastNpRate = lastNpRate.max(npRule.getLateRate().multiply(htg.otMult));
					}
				}
			}
		}
		else {
			TimecardService.warn(htg, "Timecard.HTG.NoNtPremiumRule");
		}
		return lastNpRate;
	}

	/**
	 * Calculate Night Premium pay for the typical union conditions, where the
	 * rate paid varies based on the day's call time.
	 *
	 * @param dt The DailyTime entry for the day being calculated.
	 */
	private void calculateNightPremiumA(DailyTime dt) {
		// (a) day call, extends into night
		if (dt.getCallTime().compareTo(npRule.getCallStart()) >= 0 && 	// e.g,. call >= 6am
				dt.getCallTime().compareTo(npRule.getLateCallStart()) < 0) { // and call < 8pm
			if (htg.npEligTime.compareTo(npRule.getNpRateStart()) > 0) {	// and (wrap-gold) > 8pm
				// Compute worked time (without meal times) from 8pm to (wrap-gold)
				hours15p1 = TimecardCalc.calculateWorkedHours(dt, npRule.getNpRateStart(), htg.npEligTime, htg);
				if (htg.holidayOverlap != null) {
					hours15p1 = hours15p1.subtract(htg.holidayOverlap);
				}
				// see if all NP time is within overtime hours or not.
				if (hours15p1.compareTo(htg.otHours) > 0) { // No
					hours10p1 = hours15p1.subtract(htg.otHours); // some NP goes to straight time
					htg.strHours = htg.strHours.subtract(hours10p1); // reduce regular straight time hours
					hours15p1 = htg.otHours;	// all overtime becomes NP 10% (first) at 1.5x
					htg.otHours = BigDecimal.ZERO; // no "regular" overtime left
				}
				else { // All NP time is within overtime
					htg.otHours = htg.otHours.subtract(hours15p1); // just subtract it from OT (which may now be zero)
				}
				// put the NP10p1 hours at an offset of strHours into the work day
				splitAcrossJobs(dt, true, true, hours10p1, htg.strHours);
				// put the NP15p1 hours at an off set of (strHours+otHours) into the work day
				splitAcrossJobs(dt, true, false, hours15p1, htg.strHours.add(htg.otHours));
			}
		}
		// (b) Night call
		else if (dt.getCallTime().compareTo(npRule.getLateCallStart()) >= 0 || 	// call >= 8pm
				dt.getCallTime().compareTo(npRule.getLateCallEnd()) < 0) {		// and call < 4am
			// change all straight & OT hours to 20% (second) premium
			hours10p2 = htg.strHours;
			htg.strHours = BigDecimal.ZERO;
			hours15p2 = htg.otHours;
			htg.otHours = BigDecimal.ZERO;
			// put the NP10p2 hours at the beginning of work day (offset zero)
			splitAcrossJobs(dt, false, true, hours10p2, BigDecimal.ZERO);
			// put the NP15p2 hours at an offset of NP10p2 into the work day
			splitAcrossJobs(dt, false, false, hours15p2, hours10p2);
		}
		// (c) Early AM call
		else if (dt.getCallTime().compareTo(npRule.getEarlyCallStart()) >= 0 && // call >= 4am
				dt.getCallTime().compareTo(npRule.getEarlyCallEnd()) < 0) {		// and call < 6am
			// calculate hours eligible for 20% (second) premium
			hours10p2 = npRule.getEarlyRateEnd().subtract(dt.getCallTime());	// e.g., 6am - 4.5 = 1.5hrs
			// see if NP time is within straight time
			if (hours10p2.compareTo(htg.strHours) <= 0) { // yes, typical
				htg.strHours = htg.strHours.subtract(hours10p2);  // straight time not eligible for NP stays straight
			}
			else { // for some reason, most/all of time is already OT?
				hours15p2 = hours10p2.subtract(htg.strHours); // some/all NP 1x pushed into NP 1.5x
				htg.strHours = BigDecimal.ZERO; // no straight time left
				hours10p2 = hours10p2.subtract(hours15p2); // NP 1x reduced by amount pushed to NP 1.5X
				// Does NP in 1.5X exceed amount of 1.5X hours?
				if (hours15p2.compareTo(htg.otHours) <= 0) { // no
					htg.otHours = htg.otHours.subtract(hours15p2); // just subtract it from OT (which may now be zero)
				}
				else {
					hours15p2 = htg.otHours; // all OT becomes NP 20% 1.5X
					// (if there were more NP hours, they must be gold already, so ignore)
					htg.otHours = BigDecimal.ZERO; // no more plain OT hours left.
				}
			}
			// put the NP10p2 hours at the beginning of work day (offset zero)
			splitAcrossJobs(dt, false, true, hours10p2, BigDecimal.ZERO);
			// put the NP15p2 hours at an offset of NP10p2 into the work day
			splitAcrossJobs(dt, false, false, hours15p2, hours10p2);
		}
	}

	/**
	 * Calculate Night Premium when fixed periods of the work day get specific
	 * rates, regardless of call and wrap times. This is used, for example, by
	 * local 695 and by UDA.
	 *
	 * @param dt The DailyTime entry for the day being calculated.
	 */
	private void calculateNightPremiumB(DailyTime dt) {
		hours10p1 = BigDecimal.ZERO;
		// See if work period overlaps "early NP" period (e.g., 8pm to 1am)
		if (dt.getCallTime().compareTo(npRule.getNpRateEnd()) <= 0 &&
				htg.npEligTime.compareTo(npRule.getNpRateStart()) > 0) {
			// Find start of eligible early NP hours
			BigDecimal npStart = npRule.getNpRateStart().max(dt.getCallTime());
			// Determine offset into work day of the NP start
			BigDecimal offset = TimecardCalc.calculateWorkedHours(dt, npStart, htg);

			// Find end of eligible early NP hours
			hours15p1 = npRule.getNpRateEnd().min(htg.npEligTime);
			// Find number of worked hours (less meals) between start & end = # of NP hours
			hours15p1 = TimecardCalc.calculateWorkedHours(dt, npStart, hours15p1, htg);

			BigDecimal npOT2hoursOverlap = null;
			BigDecimal otHoursBeforeNp = BigDecimal.ZERO;

			if (htg.strHours.compareTo(offset) >= 0) {
				// NP time starts during (or exactly at end of) straight hours
				hours10p1 = htg.strHours.subtract(offset);	// available straight time to convert to NP
				if (hours10p1.compareTo(hours15p1) > 0) {  // more than we need
					hours10p1 = hours15p1;
					hours15p1 = null;
				}
				else { // use all those hours at 10p1, and the rest we need at 15p1:
					hours15p1 = hours15p1.subtract(hours10p1);
					npOT2hoursOverlap = updateOtHours(otHoursBeforeNp);
				}
				htg.strHours = htg.strHours.subtract(hours10p1); // reduce regular straight time hours
			}
			else {
				// NP starts during OT or OT2 time.
				// calc hours worked from call time to start of NP:
				otHoursBeforeNp = TimecardCalc.calculateWorkedHours(dt, dt.getCallTime(), npStart, htg);
				// then subtract straight time to get # of OT hours prior to start of NP:
				otHoursBeforeNp = otHoursBeforeNp.subtract(htg.strHours);
				npOT2hoursOverlap = updateOtHours(otHoursBeforeNp);
			}
			if (htg.otHours != null && htg.otHours.signum() < 0) {
				// can happen due to "holiday hours"
				hours15p1 = hours15p1.add(htg.otHours); // decrement NP hours
				htg.otHours = BigDecimal.ZERO;
			}
			// put the NP10p1 hours at the proper offset into the work day
			splitAcrossJobs(dt, true, true, hours10p1, offset);
			// put the NP15p1 hours at an off set of (offset+hours10p1) into the work day
			splitAcrossJobs(dt, true, false, hours15p1, NumberUtils.safeAdd(offset,hours10p1));
			// put the NP10p1 hours at the beginning of work day (offset zero)
			if (splitNpOT2Hours && npOT2hoursOverlap != null) {
				splitAcrossJobs(dt, false, false, npOT2hoursOverlap, NumberUtils.safeAdd(offset,hours15p1));
			}
		}

		// See if work period overlaps "late NP" period (e.g., 1am to 6am)
		BigDecimal lateStart = npRule.getLateCallStart();
		BigDecimal lateEnd = npRule.getLateCallEnd();
		if (dt.getCallTime().compareTo(lateEnd) > 0) {
			// No NP overlap; try shifting late range by 24 hours (e.g., 25.0 to 30.0)
			lateStart = lateStart.add(Constants.HOURS_IN_A_DAY);
			lateEnd = lateEnd.add(Constants.HOURS_IN_A_DAY);
		}
		if (dt.getCallTime().compareTo(lateEnd) <= 0 &&
				htg.npEligTime.compareTo(lateStart) > 0) {
			// Find start and end of eligible early NP hours
			BigDecimal start = lateStart.max(dt.getCallTime());
			// how many hours into work day the NP eligible time starts
			BigDecimal offset = TimecardCalc.calculateWorkedHours(dt, start, htg);
			offset = offset.subtract(hours10p1); // account for early-NP using up straight time
			hours10p1 = null;

			// Find end of eligible early NP hours
			hours15p1 = lateEnd.min(htg.npEligTime);
			// Find number of worked hours (less meals) between start & end
			hours15p1 = TimecardCalc.calculateWorkedHours(dt, start, hours15p1, htg);

			if (htg.strHours.compareTo(offset) >= 0) {
				// NP time starts during straight hours
				hours10p1 = htg.strHours.subtract(offset);	// available straight time to convert to NP
				if (hours10p1.compareTo(hours15p1) > 0) {  // more than we need
					hours10p1 = hours15p1;
					hours15p1 = null;
				}
				else { // use all those hours at 10p1, and the rest we need at 15p1:
					hours15p1 = hours15p1.subtract(hours10p1);
					htg.otHours = htg.otHours.subtract(hours15p1);
				}
				htg.strHours = htg.strHours.subtract(hours10p1); // reduce regular straight time hours
			}
			else {
				// NP starts during OT
				htg.otHours = htg.otHours.subtract(hours15p1);
			}
			// put the NP10p1 hours at the beginning of work day (offset zero)
			splitAcrossJobs(dt, false, true, hours10p1, TimecardCalc.calculateWorkedHours(dt, start, htg));
			// put the NP15p1 hours at an offset of NP10p1 into the work day
			splitAcrossJobs(dt, false, false, hours15p1, NumberUtils.safeAdd(offset,hours10p1));
		}
	}

	/**
	 * The hours paid using NP rates should not be paid for as OT or OT2, so
	 * here we decrement the otHours and ot2Hours fields for this day, based on
	 * how many of our night-premium hours fall into those parts of the day.
	 * On entry, {@link #hours15p1} is the number of NP hours to be paid at OT rates,
	 * and {@link #hours10p1} is the number of HP hours to be paid at straight rate.
	 *
	 * @return The number of hours overlapping between OT2 time and NP time.
	 *         This will only be used if {@link #splitNpOT2Hours} is true.
	 */
	private BigDecimal updateOtHours(BigDecimal otHoursBeforeNp) {
		BigDecimal npOT2hoursOverlap = null;
		if (htg.ot2Hours == null || htg.ot2Hours.signum() <= 0 || hours15p1.compareTo(htg.otHours) <= 0) {
			// No OT2 hours (just OT); or NP OT hours is <= OT hours.
			// Easy case: just decrement OT by number of NP OT hours.
			// (If OT2 hours exist, they will not overlap NP OT hours.)
			htg.otHours = htg.otHours.subtract(hours15p1);
		}
		else {
			// Complicated, as OT or OT2 hours could be occurring after
			// the END of night-premium hours.
			if (otHoursBeforeNp.compareTo(htg.otHours) >= 0) {
				// all OT1 is before NP starts, so just adjust OT2
				if (hours15p1.compareTo(htg.ot2Hours) >= 0) { // NP extends to end of, or past, OT2
					npOT2hoursOverlap = htg.ot2Hours; 	// ...so all OT2 hours overlap
					htg.ot2Hours = BigDecimal.ZERO;		// ...and no OT2 to be paid.
				}
				else { // OT2 extends past end of NP
					npOT2hoursOverlap = hours15p1; 	// OT2 overlap = NP hours
					htg.ot2Hours = htg.ot2Hours.subtract(hours15p1); // decrement OT2 hours by NP hours
				}
			}
			else {
				// some (or all) of OT1 is in NP, and OT2 hours exist and may start within NP
				BigDecimal otHoursinNp = htg.otHours.subtract(otHoursBeforeNp);
				htg.otHours =  htg.otHours.subtract(otHoursinNp.min(hours15p1));
				if (otHoursinNp.compareTo(hours15p1) >= 0) {
					// OT extends to end of, or past end of, NP hours; so OT2 can be ignored:
					htg.otHours = htg.otHours.subtract(hours15p1); // just take NP hours out of OT1
				}
				else {
					// OT ends within NP: some or all of OT2 hours are within NP
					npOT2hoursOverlap = hours15p1.subtract(otHoursinNp);
					if (npOT2hoursOverlap.compareTo(htg.ot2Hours) >= 0) { // OT2 hours are all within NP
						htg.ot2Hours = BigDecimal.ZERO; // so don't pay any OT2
					}
					else { // decrement OT2 hours by the number of hours that occur within NP
						htg.ot2Hours = htg.ot2Hours.subtract(npOT2hoursOverlap);
					}
				}
			}
			if (splitNpOT2Hours && npOT2hoursOverlap != null) {
				hours15p1 = hours15p1.subtract(npOT2hoursOverlap);
			}
		}
		return npOT2hoursOverlap;
	}

	/**
	 * Fill in one or more PayJobDaily entries (the line in a Job table for a
	 * specific day) by splitting the given number of hours across whatever job
	 * splits are specified in the DailyTime instance. This fills in a single
	 * night premium "cell" (such as the hours15Np1 field) in each of the
	 * PayJobDaily instances indicated by the DailyTime data.
	 *
	 * @param dt The DailyTime entry, used to determine which PayJob(s) to
	 *            update, and which day's entry in the PayJob table to update.
	 * @param fillNp1 True if the "NPxx1" fields should be filled, false for the
	 *            "NPxx2" fields.
	 * @param isMult10 True if the "NP10x" fields should be filled in, false for
	 *            the "NP15x" fields.
	 * @param hours The total number of hours that should be distributed across
	 *            1-3 jobs.
	 * @param offset The time during the day when these hours occur, represented
	 *            as the number of work hours after the call time. This is only
	 *            used when a job-split is done by time of day, rather than
	 *            percentage.
	 */
	private void splitAcrossJobs(DailyTime dt, boolean fillNp1, boolean isMult10,
			BigDecimal hours, BigDecimal offset) {
		if (hours == null || hours.signum() <= 0) {
			return;
		}

		int job = dt.getJobNum1() - 1; // first job specified for this day, changed to zero origin
		TimecardService.checkPayJobsSize(htg.weeklyTimecard, job);

		int daynum = dt.getDayNum() - 1; // weekday number for this day, changed to zero origin.

		if (dt.getSplitStart2() == null) { // no splits, all time goes into first job
			updateOneField(job, daynum, fillNp1, isMult10, hours);
		}
		else if (dt.getSplitByPercent()) { // split values are percentages, not times
			fillJobsByPercent(dt, fillNp1, isMult10, hours);
		}
		else { // split by time of day, not percentage
			BigDecimal nHours;
			// see if any hours fit into the first split ...
			nHours = fillOneSplit(job, daynum, fillNp1, isMult10,
					BigDecimal.ZERO, dt.getSplit1(), offset, hours);
			hours = hours.subtract(nHours);  // calculate hours left to distribute
			if (hours.signum() > 0) { // still have some
				offset = offset.add(nHours); // move the offset forward by the number of hours already distributed
				// see if any hours fit into the second split ...
				nHours = fillOneSplit(dt.getJobNum2()-1, dt.getDayNum()-1, fillNp1, isMult10,
						dt.getSplit1(), dt.getSplit2(), offset, hours);
				hours = hours.subtract(nHours);
				if (hours.signum() > 0 && dt.getSplit3() != null) { // third job split & have time left...
					offset = offset.add(nHours); // move the offset forward by the number of hours already distributed
					// try and add remaining hours to the third job
					hours = fillOneSplit(dt.getJobNum3()-1, dt.getDayNum()-1, fillNp1, isMult10,
							dt.getSplit1().add(dt.getSplit2()), dt.getSplit3(), offset, hours);
				}
			}
		}
	}

	/**
	 * Optionally fill in a single PayJobDaily entry with up to the number of
	 * hours specified. This method determines if the given job, with the
	 * specified split starting point and split length, overlaps any of the
	 * hours given, based on the "offset" of those hours.
	 * <p>
	 * If there is no overlap, nothing is changed, and zero is returned. If
	 * there is an overlap, then the PayJobDaily entry will be set to the length
	 * (in hours) of the overlap, and that value will also be returned.
	 * <p>
	 * This method is only used when a job split has been specified by the time
	 * of day, rather than as a percentage.
	 *
	 * @param job The job number, origin zero, to be updated.
	 * @param daynum The weekday number, used to locate the PayJobDaily to be
	 *            updated.
	 * @param fillNp1 True if the "NPxx1" fields should be filled, false for the
	 *            "NPxx2" fields.
	 * @param isMult10 True if the "NP10x" fields should be filled in, false for
	 *            the "NP15x" fields.
	 * @param splitStart number of work hours into the day when this job starts.
	 * @param splitLength number of work hours occupied by this job.
	 * @param offset where to "place" the hours -- the number of work hours into
	 *            the day at which the given hours occur.
	 * @param hours The maximum number of hours to place into the job entry.
	 * @return The number of hours placed into the PayJobDaily entry. This is
	 *         always less than or equal to the smaller of 'hours' and
	 *         'splitLength'.
	 */
	private BigDecimal fillOneSplit(int job, int daynum, boolean fillNp1,
			boolean isMult10, BigDecimal splitStart, BigDecimal splitLength, BigDecimal offset, BigDecimal hours) {
		BigDecimal nHours = BigDecimal.ZERO;
		BigDecimal splitEnd = splitStart.add(splitLength);
		BigDecimal hoursEnd = offset.add(hours); //x
		if (hoursEnd.compareTo(splitStart) > 0 && offset.compareTo(splitEnd) < 0 ) {
			// there's some overlap between the job-split time and when the hours are to be placed
			BigDecimal start = offset.max(splitStart);
			BigDecimal end = splitEnd.min(hoursEnd);
			nHours = end.subtract(start);
			updateOneField(job, daynum, fillNp1, isMult10, nHours);
		}
		return nHours;
	}

	/**
	 * Split the given number of hours across one to three jobs, based on the
	 * DailyTime's split percentages, then put those calculated values into the
	 * appropriate NP fields based on the fillNp1 and isMult10 parameters.
	 *
	 * @param dt The DailyTime for the day being evaluated.
	 * @param fillNp1 True if the "NPxx1" fields should be filled, false for the
	 *            "NPxx2" fields.
	 * @param isMult10 True if the "NP10x" fields should be filled in, false for
	 *            the "NP15x" fields.
	 * @param hours The number of hours to be divided up.
	 */
	private void fillJobsByPercent(DailyTime dt, boolean fillNp1, boolean isMult10, BigDecimal hours) {
		int daynum = dt.getDayNum()-1;
		if (hours != null) {
			BigDecimal partHours;
			partHours = hours.multiply(dt.getSplit1()).divide(Constants.DECIMAL_100).setScale(1,RoundingMode.HALF_UP);
			updateOneField(dt.getJobNum1()-1, daynum, fillNp1, isMult10, partHours);

			partHours = hours.subtract(partHours); // hours left after first job's portion.
			if (dt.getSplit3() != null) {
				// 3-way split
				BigDecimal partHours2 = hours.multiply(dt.getSplit2()).divide(Constants.DECIMAL_100).setScale(1,RoundingMode.HALF_UP);
				updateOneField(dt.getJobNum2()-1, daynum, fillNp1, isMult10, partHours2);

				partHours = partHours.subtract(partHours2); // hours left after first & second jobs
				updateOneField(dt.getJobNum3()-1, daynum, fillNp1, isMult10, partHours);
			}
			else {
				// 2-way split - 2nd job gets remainder from first
				updateOneField(dt.getJobNum2()-1, daynum, fillNp1, isMult10, partHours);
			}
		}
	}

	/**
	 * Update a single night-premium field by adding the 'hours' given to the
	 * existing value in the field, if any. The field updated is determined by
	 * the parameters passed.
	 *
	 * @param job The job number, origin zero.
	 * @param daynum The weekday number, origin zero.
	 * @param fillNp1 True if the "NPxx1" fields should be filled, false for the
	 *            "NPxx2" fields.
	 * @param isMult10 True if the "NP10x" fields should be filled in, false for
	 *            the "NP15x" fields.
	 * @param hours The value to be added to the field. If this is null or zero,
	 *            the PayJobDaily field is not updated.
	 */
	private void updateOneField(int job, int daynum, boolean fillNp1, boolean isMult10, BigDecimal hours) {
		PayJob pj = htg.weeklyTimecard.getPayJobs().get(job);
		PayJobDaily pd = pj.getPayJobDailies().get(daynum);
		if (hours != null && hours.signum() != 0) {
			if (fillNp1) { // fill the "NPxx1" fields (typically 10% NP)
				if (isMult10) {
					pd.setHours10Np1(NumberUtils.safeAdd(hours, pd.getHours10Np1()));
				}
				else {
					pd.setHours15Np1(NumberUtils.safeAdd(hours, pd.getHours15Np1()));
				}
			}
			else { // fill the "NPxx2" fields -- typically 20% NP
				if (isMult10) {
					pd.setHours10Np2(NumberUtils.safeAdd(hours, pd.getHours10Np2()));
				}
				else {
					pd.setHours15Np2(NumberUtils.safeAdd(hours, pd.getHours15Np2()));
				}
			}
		}
	}

}
