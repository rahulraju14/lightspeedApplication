/**
 * TimecardMpvService.java
 */
package com.lightspeedeps.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.lightspeedeps.dao.MpvRuleDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.SpecialRuleService.HtgPhase;
import com.lightspeedeps.type.AuditEventType;
import com.lightspeedeps.type.RuleType;
import com.lightspeedeps.util.app.AuditUtils;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardUtils;

/**
 * Methods related to MPV calculations for WeeklyTimecard`s.
 */
public class MpvService extends BaseService {
	private static final Log log = LogFactory.getLog(MpvService.class);

	/** The HtgData instance has most of the information needed to do the
	 * PayJob processing. */
	private HtgData htg;

	@Autowired
	private MpvRuleDAO mpvRuleDAO;

	private AuditEvent event;

	public MpvService(HtgData pHtg) {
		htg = pHtg;
	}

	/**
	 * Calculate the MPV values for the current timecard (passed to our
	 * constructor).  The timecard should have been validated before calling
	 * this method.
	 *
	 * @param parent The parent AuditEvent; this method will create a child
	 *            event for its audit trail. May be null, in which case the MPV
	 *            audit trail will be at the top-most level.
	 */
	public void calculateMpvs(AuditEvent parent) {
		log.debug("");

		// Fields in MpvRule that are not yet used in our calculations:
		// TODO maxNdmAdjust is currently not used
		/*
		 * Note that maxGrace is superseded in calculations by
		 * MpvRule.mayExtendBy, since any "grace" period specified on the
		 * timecard may actually an "extension" requested by the production
		 * staff.
		 */

		try {
			event = htg.service.startEvent(AuditEventType.CALC_MPV, parent, htg.weeklyTimecard);
			htg.auditEvent = event;

			// Update day types for worked days that are currently null or non-worked holiday
			TimecardUtils.fillDayTypes(htg.weeklyTimecard);

			// Clear MPVs
			for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
				dt.setMpv1Payroll(null);
				dt.setMpv2Payroll(null);
				dt.setMpv3Payroll(null);
			}

			// Apply the rules day by day...
			applyMpvRule();

			AuditUtils.updateEvent(event);
//			TimecardCalc.calculateWeeklyTotals(htg.weeklyTimecard);

			htg.auditEvent = parent;
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Apply one or more of a set of ContractRule`s to all the days in the given
	 * timecard. Upon completion, the payroll MPV fields in each day of the
	 * timecard will have been set to the proper value.  The MPV fields should
	 * have been cleared before calling this method.
	 *
	 */
	private void applyMpvRule() {
		String lastRuleKey = "";
		List<MpvRule> mpvRules = new ArrayList<>();
		for (DailyTime dt : htg.weeklyTimecard.getDailyTimes()) {
			if (! htg.ruleService.getSpecialService().prePhase(dt, HtgPhase.MPV, event)) {
				break;
			}
			lastRuleKey = calculateMpvs(dt, lastRuleKey, mpvRules);
			if (lastRuleKey == null) {
				break;
			}
		}
	}

	/**
	 * Calculate the MPV values for a single day.
	 *
	 * @param pHtg The current HtgData instance.
	 * @param dt The DailyTime object for the day being evaluated.
	 */
	public static void calculateMpvs(HtgData pHtg, DailyTime dt) {
		MpvService mpv = new MpvService(pHtg);
		mpv.event = new AuditEvent();
		String lastRuleKey = "";
		List<MpvRule> mpvRules = new ArrayList<>();
		lastRuleKey = mpv.calculateMpvs(dt, lastRuleKey, mpvRules);
	}

	/**
	 * Calculate the MPV values for a single day.
	 *
	 * @param dt The DailyTime object for the day being evaluated.
	 * @param lastRuleKey The last-used MPV rule key; may not be null, but may
	 *            be an empty String.
	 * @param mpvRules The list of MpvRules corresponding to the 'lastRuleKey'.
	 *            These rules will be used unless the code determines that the
	 *            MPV Rule in effect has changed, in which case the old rules
	 *            will be removed from this List and the new rules will be
	 *            added.
	 * @return The last-used MPV rule key.
	 */
	private String calculateMpvs(DailyTime dt, String lastRuleKey, List<MpvRule> mpvRules) {
		if (dt.getDayType() != null) {
			ContractRule dayRule = htg.ruleService.findLastDailyContractRule(dt, RuleType.MP, event);
			if (dayRule != null) {
				if (! dayRule.getUseRuleKey().equals(lastRuleKey)) {
					// Need to load a new MpvRule (or multiple rules in unusual cases).
					lastRuleKey = dayRule.getUseRuleKey();
					mpvRules.clear();
					mpvRules.addAll(getMpvRuleDAO().findByRuleKey(lastRuleKey));
				}
				if (mpvRules.size() > 0) {
					event.appendSummary(dt.getWeekDayName() + "=" + lastRuleKey);
					applyMpvRule(dt, mpvRules, htg);
				}
				else {
					TimecardService.errorTrace(htg, "Timecard.HTG.NoMpvRule", lastRuleKey);
					return null;
				}
			}
			else {
				TimecardService.errorTrace(htg, "Timecard.HTG.NoMpvDayRule", dt.getWeekDayNum());
				return null;
			}
		}
		if (! htg.ruleService.getSpecialService().postPhase(dt, HtgPhase.MPV, event)) {
			return null;
		}
		return lastRuleKey;
	}

	/**
	 * Apply the given MpvRule(s) to one day. This updates the DailyTime's
	 * payroll MPV fields with the calculated values.
	 *
	 * @param dt The DailyTime entry to be analyzed.
	 * @param rules The set of rules to apply. Up to 3 rules may be specified,
	 *            one for each possible meal period violation. If only one rule
	 *            is given, it applies to all meals. If two rules are given, the
	 *            first rule applies to the first meal period, the second rule
	 *            to the remaining meal periods. If three rules are given, each
	 *            rule applies to the appropriate meal period.
	 */
	@SuppressWarnings("null")
	public static void applyMpvRule(DailyTime dt, List<MpvRule> rules, HtgData pHtg) {

		MpvRule rule = rules.get(0);
		MpvRule rule2 = rule;
		MpvRule rule3 = rule;
		if (rules.size() > 1) {
			rule2 = rules.get(1);
			rule3 = rule2;
			if (rules.size() > 2) {
				rule3 = rules.get(2);
			}
		}

		String date = new SimpleDateFormat("M/d").format(dt.getDate());

//		if (rule.getMinTo2ndMeal().signum() > 0) {
//			log.error("Unsupported rule value: MpvRule minTo2ndMeal is non-zero");
//		}

		boolean allowNdm = rule.getNdmAllowed();

		byte totalMpvs = 0; // total number of mpvs calculated for the day

		// Determine elapsed time between meals...
		BigDecimal am = TimecardCalc.calculateAmMealHours(dt, allowNdm); 	// 1st meal period
		BigDecimal pm = TimecardCalc.calculatePmHours(dt, rule.getMaxMealLength(), allowNdm);		// 2nd meal period (if any)
		BigDecimal wrap = TimecardCalc.calculateWrapHours(dt, rule2.getMaxMealLength(), allowNdm);	// 3rd meal period (if any)
		if (pm == null && wrap != null) {
			// timecard has meal 2, but not meal 1 -- shift times
			pm = wrap;
			wrap = null;
		}
		BigDecimal allowedTime;

		if (am != null || pm != null || wrap != null) {
			boolean meal1Dropped = false;
			BigDecimal mealLen = TimecardCalc.calculateMeal1Length(dt, allowNdm);
			if (mealLen != null) {
				if (mealLen.compareTo(rule.getMinMealLength()) < 0) {
					// 1st meal is too short -- does not count as a meal!!
					TimecardService.warn(pHtg, "Timecard.HTG.FirstMealShort", date);
					am = NumberUtils.safeAdd(am, pm, mealLen);
					pm = wrap;	// Shift remaining meals
					wrap = null;
					meal1Dropped = true;
				}
				else if (mealLen.compareTo(rule.getMaxMealLength()) > 0) {
					// 1st meal too long, affects time worked (deduction), handled in Pay Breakdown
					TimecardService.warn(pHtg, "Timecard.HTG.FirstMealLong", date);
				}
			}
			mealLen = TimecardCalc.calculateMeal2Length(dt, allowNdm);
			if (mealLen != null) {
				if (mealLen.compareTo(rule2.getMinMealLength()) < 0) {
					// 2nd meal too short -- does not count as meal!
					pm = NumberUtils.safeAdd(pm, wrap, mealLen);
					wrap = null;
					if (meal1Dropped) { // both meals were too short
						am = am.add(pm); // so all work time gets lumped together
						pm = null;
					}
					TimecardService.warn(pHtg, "Timecard.HTG.SecondMealShort", date);
				}
				else if (mealLen.compareTo(rule2.getMaxMealLength()) > 0) {
					// 2nd meal too long, affects time worked (deduction), handled in Pay Breakdown
					TimecardService.warn(pHtg, "Timecard.HTG.SecondMealLong", date);
				}
			}
			BigDecimal violation = null;

			// check 1st meal period for violation
			if (am != null) {
				allowedTime = rule.getMaxTo1stMeal();
				if (dt.getGrace1() != null && dt.getGrace1().signum() > 0) {
					if (dt.getGrace1().compareTo(rule.getMaxGrace()) > 0) {
						TimecardService.warn(pHtg, "Timecard.HTG.GraceHigh", "1", date);
						allowedTime = allowedTime.add(rule.getMaxGrace());
					}
					else {
						allowedTime = allowedTime.add(dt.getGrace1());
					}
				}
				violation = am.subtract(allowedTime);
				if (violation.signum() > 0) {
					// "normal" violation - too long to 1st meal with or without extensions
					byte mpvs = calculateMpvCount(violation, rule.getIncrement());
					if (mpvs > rule.getMaxMpPerDay()) {
						mpvs = rule.getMaxMpPerDay();
					}
					totalMpvs = mpvs;
					dt.setMpv1Payroll(mpvs);
				}
			}

			boolean extended = dt.getCameraWrap();
			if (pm != null && totalMpvs < rule.getMaxMpPerDay()) {
				violation = null;
				allowedTime = rule2.getMaxTo2ndMeal();
				BigDecimal maxGrace = BigDecimal.ZERO; // grace/extension to be granted
				if (extended && wrap == null) {
					// camera wrap checked, & no 3rd period
					maxGrace = rule2.getMayExtendBy().max(rule2.getMaxGrace());
				}
				if (dt.getGrace2() != null && dt.getGrace2().signum() > 0) {
					maxGrace = maxGrace.max(rule2.getMaxGrace()); // if no camera wrap, use Max Grace
					if (dt.getGrace2().compareTo(maxGrace) > 0) { // illegal grace
						TimecardService.warn(pHtg, "Timecard.HTG.GraceHigh", "2", date);
					}
					else if (! (extended && wrap == null)) {
						// camera wrap didn't apply; and Grace2 <= max allowed grace,
						maxGrace = dt.getGrace2(); // so use specified value (may be smaller)
					}
				}
				allowedTime = allowedTime.add(maxGrace);

				violation = pm.subtract(allowedTime);
				if (violation.signum() > 0 && TimecardCalc.meal2IsNdm(dt, rule2, pHtg.isCommercial)) {
					// recalc pm hours due to NDM
					pm = TimecardCalc.calculatePmHours(dt, rule.getMaxMealLength(), allowNdm);
					violation = pm.subtract(allowedTime);
				}
				if (violation.signum() > 0) {
					// "normal" violation - too long to 2nd meal with or without extensions
					byte mpvs = calculateMpvCount(violation, rule2.getIncrement());
					if ((totalMpvs + mpvs) > rule2.getMaxMpPerDay()) {
						mpvs = (byte)(rule2.getMaxMpPerDay() - totalMpvs);
					}
					if (mpvs > 0) {
						totalMpvs += mpvs;
						dt.setMpv2Payroll(mpvs);
					}
				}
			}

			if (wrap != null && totalMpvs < rule.getMaxMpPerDay()) {
				violation = null;
				if (pm.compareTo(rule.getMinTo2ndMeal()) < 0) {
					// 2nd meal too soon -- will affect time worked (deduction).
					// TODO warning for this issue?
				}
				allowedTime = rule3.getMaxTo2ndMeal();
				if (extended) {
					allowedTime = allowedTime.add(rule3.getMayExtendBy());
				}
				violation = wrap.subtract(allowedTime);
				if (violation.signum() > 0) {
					// "normal" violation - too long to wrap with or without extensions
					byte mpvs = calculateMpvCount(violation, rule3.getIncrement());
					if ((totalMpvs + mpvs) > rule3.getMaxMpPerDay()) {
						mpvs = (byte)(rule3.getMaxMpPerDay() - totalMpvs);
					}
					if (mpvs > 0) {
						dt.setMpv3Payroll(mpvs);
					}
				}
			}
			log.debug("Applied MPV rule; hours={" + am + "," + pm + "," + wrap +
					"}, mpvs={" + dt.getMpv1Payroll() + "," + dt.getMpv2Payroll() + "," + dt.getMpv3Payroll() +
					"}; rule=" + rule.toAudit());
		}
	}

	/**
	 * Calculate the number of MPVs for a given length of time, for a given
	 * increment size.
	 *
	 * @param violation The number of hours the meal was delayed by.
	 * @param increment The MPV increment -- what period (in hours) counts as
	 *            one MPV.
	 * @return The number of MPVs, always an integral value.
	 */
	private static byte calculateMpvCount(BigDecimal violation, BigDecimal increment) {
		byte mpvs = 0;
		while (violation.signum() > 0) {
			mpvs++;
			violation = violation.subtract(increment);
		}
		return mpvs;
	}

	/** See {@link #mpvRuleDAO}. */
	private MpvRuleDAO getMpvRuleDAO() {
		if (mpvRuleDAO == null) {
			mpvRuleDAO = (MpvRuleDAO)MpvRuleDAO.getInstance();
		}
		return mpvRuleDAO;
	}

}
