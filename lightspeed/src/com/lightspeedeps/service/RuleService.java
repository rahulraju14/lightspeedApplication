/**
 * RuleService.java
 */
package com.lightspeedeps.service;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.RuleFilter;
import com.lightspeedeps.object.RuleFilterImpl;
import com.lightspeedeps.type.DayType;
import com.lightspeedeps.type.RuleType;
import com.lightspeedeps.type.WorkZone;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;

/**
 * This service class contains methods related to retrieving, parsing, and
 * applying contract rules and supporting rules. These methods are used
 * extensively in the HTG process.
 * <p>
 * An instance of RuleService is created to process a single timecard. This
 * instance is shared among the various service classes that perform the HTG
 * process.
 * <p>
 * Note that the rule Lists are originally filled (via the database query) in
 * priority and id order, so the last matching rule of a given type is the one
 * that should be applied.
 * <p>
 * The {@link #findTimecardContractRules(AuditEvent)} method is the "starting
 * point" for determining the full set of rules that apply to a given timecard.
 * Most of the other 'find' methods are merely searching for particular rules or
 * subsets within that initial set.
 *
 * @see ContractRule
 * @see RuleFilter
 */
public class RuleService extends BaseService {
	private static final Log log = LogFactory.getLog(RuleService.class);

	/** The HtgData instance has most of the information needed to do the
	 * PayJob processing. */
	private HtgData htg;

	/** The list of rules which match the current timecard and production. This
	 * should include all rules that might apply to any given day during the
	 * week. This list is always kept sorted in ascending priority order -- the
	 * last applicable rule is the most important and (typically) overrides
	 * earlier ones of the same type. */
	private List<ContractRule> contractRules;

	/** The list of rules that have been qualified based on a the DayType of the
	 * day currently being processed. This list is always kept sorted in
	 * ascending priority order -- the last applicable rule is the most important
	 * and (typically) overrides earlier ones of the same type. */
	private List<ContractRule> dailyContractRules;

	/** The StartForm that the timecard being processed was created from. */
	private StartForm startForm;

	/** The service that should be called to process Special Rules
	 * before and after each phase of HTG processing. */
	private SpecialRuleService specialService;

	/** A collection of all the rule names which have been added to the
	 * current audit trail.  This is used so that detailed rule information
	 * is only added once for a given rule. */
	private final Set<String> rulesDetailed = new HashSet<>();


	@Autowired
	private ContractRuleDAO  contractRuleDAO;

	@Autowired
	private CallBackRuleDAO  callBackRuleDAO;

	@Autowired
	private EmploymentDAO  employmentDAO;

	@Autowired
	private GoldenRuleDAO  goldenRuleDAO;

	@Autowired
	private GuaranteeRuleDAO  guaranteeRuleDAO;

	@Autowired
	private HolidayListRuleDAO  holidayListRuleDAO;

	@Autowired
	private HolidayRuleDAO  holidayRuleDAO;

	@Autowired
	private MpvRuleDAO mpvRuleDAO;

	@Autowired
	private NtPremiumRuleDAO  ntPremiumRuleDAO;

	@Autowired
	private OnCallRuleDAO onCallRuleDAO;

	@Autowired
	private OvertimeRuleDAO overtimeRuleDAO;

	@Autowired
	private RestRuleDAO restRuleDAO;

	@Autowired
	private WeeklyRuleDAO weeklyRuleDAO;


	/**
	 * Our normal constructor. (There is no parameterless constructor.)
	 *
	 * @param pHtg The HtgData instance carrying all the necessary HTG
	 *            information.
	 */
	public RuleService(HtgData pHtg) {
		htg = pHtg;
		startForm = htg.weeklyTimecard.getStartForm();
		specialService = new SpecialRuleService(htg);
	}

	/**
	 * A constructor used during timecard creation.
	 * @param wtc The WeeklyTimecard being analyzed.
	 * @param prod The Production to which the timecard belongs.
	 */
	public RuleService(WeeklyTimecard wtc, Production prod) {
		TimecardService tcs = new TimecardService(wtc, prod);
		htg = tcs.getHtg();
		startForm = htg.weeklyTimecard.getStartForm();
	}

	/**
	 * Find all the Contract Rules that could apply to a given WeeklyTimecard
	 * for any day in that week. The rules returned may include both system-wide
	 * and Production-specific ones. Note that the list will need to be filtered
	 * by additional information for each day of the week.
	 *
	 * @param event The audit trail object.
	 * @return A non-null, but possibly empty, List of rules to be applied to
	 *         the timecard, in ascending priority order.
	 */
	private List<ContractRule> findTimecardContractRules(AuditEvent event) {
		if (contractRules == null) {
			if (htg.production.getPayrollPref().getUse30Htg() && ! htg.nonUnion) { // Use 3.0 "full" HTG processing
				contractRules = getContractRuleDAO().findByStartFormTimecard(htg.production, startForm, htg.weeklyTimecard, true);
				if (contractRules.size() == 0) {
					TimecardService.errorTrace(htg, "Timecard.HTG.NoContractRules", htg.weeklyTimecard.getStartForm().getContractCode());
					// try again using state (non-union) rules
					contractRules = getContractRuleDAO().findByStartFormTimecard(htg.production, startForm, htg.weeklyTimecard, false);
				}
			}
			else { // Use state/federal rules for non-union
				contractRules = getContractRuleDAO().findByStartFormTimecard(htg.production, startForm, htg.weeklyTimecard, false);
			}
			for (Iterator<ContractRule> iter = contractRules.iterator(); iter.hasNext(); ) {
				ContractRule cr = iter.next();
				if (! evaluateExtraFilters(cr, null)) {
					log.debug("removed due to extra filter(s): " + cr);
					iter.remove();
				}
			}
			dumpRules(contractRules);

			if (startForm != null) {
				String state = null;
				Employment emp = startForm.getEmployment();
				if (emp != null) {
					emp = getEmploymentDAO().refresh(emp);  // Fixes LIE on "auto pay" in commercial prod., cross-project
					state = emp.getWageState();
				}
				if (state == null || state.equals(Constants.STATE_WORKED_CODE)) {
					state = "Worked";
				}
				event.appendDetail("SF: State=" + state
						+ ", Occ=" + startForm.getOccupationCode()
						+ ", LsOcc=" + startForm.getLsOccCode()
						+ ", Sched=" + startForm.getContractSchedule());
			}
		}
		log.debug("rules applying to whole week: " + (contractRules == null ? "NONE!" : contractRules.size()));
		return contractRules;
	}

//	/**
//	 * Find all the Contract Rules of a given RuleType that could apply to a
//	 * given WeeklyTimecard for any day in that week. The rules returned may
//	 * include both system-wide and Production-specific ones. Note that the list
//	 * will need to be filtered by additional information for each day of the
//	 * week.
//	 *
//	 * @param weeklyTimecard The WeeklyTimecard of interest.
//	 * @param rType The specific RuleType to return.
//	 * @param event The audit trail object.
//	 * @return A non-null, but possibly empty, List of rules, of type 'rType',
//	 *         to be applied to the timecard.
//	 */
//	public List<ContractRule> findTimecardContractRules(RuleType rType, AuditEvent event) {
//		List<ContractRule> rules = getContractRuleDAO().findByStartFormTimecard(prod, sf, weeklyTimecard, rType);
//		return rules;
//	}

	/**
	 * Set up the rules engine to get rules for a specific day in the current
	 * timecard. This method should be called before any of the "find..."
	 * (specific type) rule methods. This fills the {@link #dailyContractRules}
	 * field.
	 *
	 * @param dt The DailyTime object of the day being analyzed.
	 * @param event The AuditEvent used for logging.
	 */
	public void prepareDailyContractRules(DailyTime dt, AuditEvent event) {
		dailyContractRules = findDailyContractRules(dt, null, event);
	}

	/**
	 * Find the rules which apply to a specific day in a timecard; optionally
	 * limited to a specific rule type.
	 *
	 * @param dt The DailyTime object whose values will be used to filter the
	 *            rules.
	 * @param rType The specific rule type wanted, or null if all rule types
	 *            should be included.
	 * @param event The AuditEvent used for logging.
	 * @return A non-null, but possibly empty, List of rules matching the given
	 *         parameters, in ascending priority order.
	 */
	public List<ContractRule> findDailyContractRules(DailyTime dt, RuleType rType, AuditEvent event) {

		List<ContractRule> dRules = new ArrayList<>();
		findTimecardContractRules(event); // fills 'contractRules' if necessary

		DayType dayType = dt.getDayType();
		WorkZone zone = dt.getWorkZone();
		if (zone == null || zone == WorkZone.N_A) {
			// If DayType doesn't indicate the area of shooting (distant vs studio), use timecard setting
			zone = dt.getWeeklyTimecard().getDefaultZone();
		}
		String state = null;
		if ( /*dt.getWeeklyTimecard().isNonUnion() &&*/ htg.weeklyTimecard.getUseStateWorked()) {
			// Unions without full HTG rules installed may use state rules!
			state = dt.getState();
			if (state == null) {
				state = dt.getWeeklyTimecard().getStateWorked();
			}
		}

		// Create list of only those rules matching tests against daily values.
		// Note that most tests are "negative" - if test passes, we do NOT include the rule.
		for (ContractRule rule : contractRules) {
			if (rType != null && rule.getRuleType() != rType) {
				continue;
			}
			if (dayType == null && // Empty day type ... only matches completely "generic" rule
					(rule.getDayType() != DayType.N_A ||
					rule.getOnProduction() != DayType.N_A ||
					rule.getWorkZone() != WorkZone.N_A ||
					! rule.getDayNumber().equals(ContractRule.RULE_FIELD_NA) ||
					rule.getExtraFilters() != null)) {
				continue;
			}

			// check Work Zone
			if (rule.getWorkZone() != WorkZone.N_A) {
				// Rule "Work Area" -- compare to current
				if (rule.getWorkZone() != zone) {
					// Not an exact match -- check for conflicts.
					// Check Studio vs Distant.
					if (rule.getWorkZone().isStudio() && zone.isDistant()) {
						continue;
					}
					if (rule.getWorkZone().isDistant() && zone.isStudio()) {
						continue;
					}
					// Check sub-types of Studio.
					if (rule.getWorkZone() == WorkZone.ZN || rule.getWorkZone() == WorkZone.ST ||
							rule.getWorkZone() == WorkZone.BL) {
						// For any of these rule DayTypes, we should have had an exact match;
						// since we didn't, throw out the rule.
						continue;
					}
					if (rule.getWorkZone() == WorkZone.NB &&
							zone == WorkZone.BL) {
						continue;
					}
					if (rule.getWorkZone() == WorkZone.SB &&
							(zone != WorkZone.BL && zone != WorkZone.ST)) {
						continue;
					}
					if (rule.getWorkZone() == WorkZone.ZB &&
							(zone != WorkZone.BL && zone != WorkZone.ZN)) {
						continue;
					}
				}
			}

			/* Check DayType */
			if (rule.getDayType() == DayType.ID && (! dt.getDayType().isIdle())) {
				continue;
			}

			if (rule.getDayType() != DayType.N_A && rule.getDayType() != dt.getDayType()) {
				// not exact dayType match, but maybe it's the right category
				if (	(rule.getDayType() == DayType.ID && (dt.getDayType().isIdle())) ||
						(rule.getDayType() == DayType.TR && (dt.getDayType().isTravel()))
						) {
					// OK - idle = any idle; travel = any travel
				}
				else {
					continue;
				}
			}

			// Check On-production vs Off-production
			if ((rule.getOnProduction() != DayType.N_A) &&
					(rule.getOnProduction() != dayType) &&
					(rule.getOnProduction() != DayType.ON ||
						htg.weeklyTimecard.getOffProduction() || dt.getOffProduction()) &&
					(rule.getOnProduction() != DayType.OP ||
						( dayType != DayType.OF && (! htg.weeklyTimecard.getOffProduction()) && (! dt.getOffProduction())))
				) {
				continue;
			}

			// Check Day Number
			if (! rule.getDayNumber().equals(ContractRule.RULE_FIELD_NA)) {
				if (! rule.matchesDayNumber(dt.getWorkDayNum())) {
					continue;
				}
			}

			// Check "worked state" if applicable (non-union, or union w/o full HTG rules)
			if (state != null && (! rule.getAgreement().equals(ContractRule.RULE_FIELD_NA)) &&
					(rule.getAgreement().length() == 2) && // only apply restriction if agreement is a state code!
					(! rule.getAgreement().equals(state))) {
				continue;
			}

			// Check the rule's effective date range, if specified. LS-2928
			if (rule.getFirstEffectiveDate() != null &&
					dt.getDate().before(rule.getFirstEffectiveDate())) {
				// the rule has not taken effect yet (as of the DailyTime's date)
				continue;
			}
			if (rule.getLastEffectiveDate() != null &&
					dt.getDate().after(rule.getLastEffectiveDate())) {
				// the rule is no longer in effect (for the DailyTime's date)
				continue;
			}

			// Passed all the "daily" tests, include it in results
			if (rule.getExtraFilters() != null) {
				if (! evaluateExtraFilters(rule, dt)) {
					continue;
				}
			}
			dRules.add(rule);
		}
		dumpRules(dRules);
		return dRules;
	}

	/**
	 * Find the last rule of the given type for the current timecard for the
	 * specified day. {@link #prepareDailyContractRules(DailyTime, AuditEvent)}
	 * should have been called prior to calling this method, to set up for a
	 * specific day.
	 *
	 * @param dt The DailyTime for the day of the week to match.
	 * @param rType The RuleType to be found and returned.
	 * @param event The audit trail instance.
	 * @return a rule of the specified type for the given day of the current
	 *         timecard, or null if none found.
	 */
	public ContractRule findLastDailyContractRule(DailyTime dt, RuleType rType, AuditEvent event) {
		List<ContractRule> rules = findDailyContractRules(dt, rType, event);
		ContractRule rule = null;
		if (rules.size() > 0) {
			rule = rules.get(rules.size()-1);
		}
		return rule;
	}

	/**
	 * Find the last Call-Back rule for the current timecard for the current
	 * day. {@link #prepareDailyContractRules(DailyTime, AuditEvent)} should
	 * have been called prior to calling this method, to set up for a specific
	 * day.
	 *
	 * @param event The audit trail instance.
	 * @return a call-back rule for the current day of the current timecard, or
	 *         null if none found.
	 */
	public CallBackRule findCallBackRule(AuditEvent event) {
		CallBackRule rule = null;
		ContractRule cr = findLastDailyContractRuleByType(RuleType.CB, event);
		if (cr != null) {
			rule = getCallBackRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find the last golden rule for the current timecard for the current day.
	 * {@link #prepareDailyContractRules(DailyTime, AuditEvent)} should have
	 * been called prior to calling this method, to set up for a specific day.
	 *
	 * @param event The audit trail instance.
	 * @return a golden rule for the current day of the current timecard, or
	 *         null if none found.
	 */
	public GoldenRule findGoldenRule(AuditEvent event) {
		GoldenRule rule = null;
		ContractRule cr = findLastDailyContractRuleByType(RuleType.GL, event);
		if (cr != null) {
			rule = getGoldenRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find the last Guarantee rule for the current timecard for the current
	 * day. {@link #prepareDailyContractRules(DailyTime, AuditEvent)} should
	 * have been called prior to calling this method, to set up for a specific
	 * day.
	 *
	 * @param event The audit trail instance.
	 * @return a Guarantee rule for the current day of the current timecard, or
	 *         null if none found.
	 */
	public GuaranteeRule findGuaranteeRule(AuditEvent event) {
		GuaranteeRule rule = null;
		ContractRule cr = findLastDailyContractRuleByType(RuleType.GT, event);
		if (cr != null) {
			rule = getGuaranteeRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find the last defined HolidayList rule for the current timecard, for any
	 * time during the week.
	 *
	 * @param event The AuditEvent to use for logging.
	 * @return A HolidayListRule matching the current timecard, or null if a
	 *         match was not found.
	 */
	public HolidayListRule findHolidayListRule(AuditEvent event) {
		HolidayListRule rule = null;
		ContractRule cr = findLastContractRuleByType(RuleType.HL, event);
		if (cr != null) {
			rule = getHolidayListRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find the last Holiday rule for the current timecard for the current day.
	 * {@link #prepareDailyContractRules(DailyTime, AuditEvent)} should have
	 * been called prior to calling this method, to set up for a specific day.
	 *
	 * @param event The audit trail instance.
	 * @return a Holiday rule for the current day of the current timecard, or
	 *         null if none found.
	 */
	public HolidayRule findHolidayRule(AuditEvent event) {
		HolidayRule rule = null;
		ContractRule cr = findLastDailyContractRuleByType(RuleType.HO, event);
		if (cr != null) {
			rule = getHolidayRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find the last meal-period-violation rule for the current timecard for the
	 * current day. {@link #prepareDailyContractRules(DailyTime, AuditEvent)}
	 * should have been called prior to calling this method, to set up for a
	 * specific day.
	 *
	 * @param event The audit trail instance.
	 * @return an MPV rule for the current day of the current timecard, or null
	 *         if none found.
	 */
	public MpvRule findMpvRule(AuditEvent event) {
		MpvRule rule = null;
		ContractRule cr = findLastDailyContractRuleByType(RuleType.MP, event);
		if (cr != null) {
			rule = getMpvRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find the last night-premium rule for the current timecard for the current
	 * day. {@link #prepareDailyContractRules(DailyTime, AuditEvent)} should
	 * have been called prior to calling this method, to set up for a specific
	 * day.
	 *
	 * @param event The audit trail instance.
	 * @return a night-premium rule the current day of for the current timecard,
	 *         or null if none found.
	 */
	public NtPremiumRule findNpRule(AuditEvent event) {
		NtPremiumRule rule = null;
		ContractRule cr = findLastDailyContractRuleByType(RuleType.NP, event);
		if (cr != null) {
			rule = getNtPremiumRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find the last OnCall rule for the current timecard for the current day.
	 * {@link #prepareDailyContractRules(DailyTime, AuditEvent)} should have
	 * been called prior to calling this method, to set up for a specific day.
	 *
	 * @param event The audit trail instance.
	 * @return an On-Call rule for the current day of the current timecard, or
	 *         null if none found.
	 */
	public OnCallRule findOnCallRule(AuditEvent event) {
		OnCallRule rule = null;
		ContractRule cr = findLastDailyContractRuleByType(RuleType.OC, event);
		if (cr != null) {
			rule = getOnCallRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find the last Overtime rule for the current timecard for the current day.
	 * {@link #prepareDailyContractRules(DailyTime, AuditEvent)} should have
	 * been called prior to calling this method, to set up for a specific day.
	 *
	 * @param event The audit trail instance.
	 * @return an Overtime rule for the current day of the current timecard, or
	 *         null if none found.
	 */
	public OvertimeRule findOvertimeRule(AuditEvent event) {
		OvertimeRule rule = null;
		ContractRule cr = findLastDailyContractRuleByType(RuleType.OT, event);
		if (cr != null) {
			rule = getOvertimeRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find the last Rest Invasion rule for the current timecard for the current
	 * day. {@link #prepareDailyContractRules(DailyTime, AuditEvent)} should
	 * have been called prior to calling this method, to set up for a specific
	 * day.
	 *
	 * @param event The audit trail instance.
	 * @return a Rest Invasion rule for the current day of the current timecard,
	 *         or null if none found.
	 */
	public RestRule findRestRule(AuditEvent event) {
		RestRule rule = null;
		ContractRule cr = findLastDailyContractRuleByType(RuleType.RS, event);
		if (cr != null) {
			rule = getRestRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find the contract code of the last Rest Invasion rule for the current
	 * timecard for the current day.
	 * {@link #prepareDailyContractRules(DailyTime, AuditEvent)} should have
	 * been called prior to calling this method, to set up for a specific day.
	 *
	 * @param event The audit trail instance.
	 * @return The contract code for the Rest Invasion rule for the current day
	 *         of the current timecard, or null if none found.
	 */
	public String findRestRuleContract(AuditEvent event) {
		String contract = null;
		ContractRule cr = findLastDailyContractRuleByType(RuleType.RS, event);
		if (cr != null) {
			contract = cr.getAgreement();
		}
		return contract;
	}

	/**
	 * Find the last Weekly(base) rule for the current timecard. Note that this
	 * is not based on a DailyTime entry -- it should be constant for the whole
	 * week!
	 *
	 * @param event The audit trail instance.
	 * @return a Weekly (Basic) rule for the current timecard, or null if none
	 *         found.
	 */
	public WeeklyRule findWeeklyRule(AuditEvent event) {
		WeeklyRule rule = null;
		ContractRule cr = findLastContractRuleByType(RuleType.BA, event);
		if (cr != null) {
			rule = getWeeklyRuleDAO().findOneByRuleKey(cr.getUseRuleKey());
			if (rule == null) {
				logMissingRule(cr, event);
			}
		}
		return rule;
	}

	/**
	 * Find all the rules for the current timecard of a specific rule type.
	 *
	 * @param rType The ruleType desired; if null, an empty set of rules is
	 *            returned.
	 * @param event The Audit trail instance.
	 * @return A list (possibly empty) of all the rules of the requested type
	 *         that apply to the current timecard.
	 */
	public List<ContractRule> findContractRulesByType(RuleType rType, AuditEvent event) {
		List<ContractRule> dRules = new ArrayList<>();
		if (rType != null) {
			// Create list of only those rules matching the requested rule type
			for (ContractRule rule : findTimecardContractRules(event)) {
				if (rule.getRuleType() == rType) {
					if (rule.getExtraFilters() != null) {
						if (! evaluateExtraFilters(rule, null)) {
							continue;
						}
					}
					dRules.add(rule);
				}
			}
			dumpRules(dRules);
		}
		return dRules;
	}

	/**
	 * Find the last rule of a specific rule type for the current timecard, for
	 * any day of the week. Note that the rule Lists are originally filled in
	 * priority and id order, so the last matching rule of a given type is the
	 * one that should be applied.
	 *
	 * @param rType The ruleType desired; if null, null is returned.
	 * @param event The Audit trail instance.
	 * @return A rule of the requested type that applies to the current
	 *         timecard, or null if none is found.
	 */
	private ContractRule findLastContractRuleByType(RuleType rType, AuditEvent event) {
		ContractRule rule = null;
		if (rType != null) {
			// Find the last rule matching the requested rule type
			WorkZone zone = htg.weeklyTimecard.getDefaultZone();
			for (ContractRule cr : findTimecardContractRules(event)) {
				if (cr.getRuleType() == rType) {
					// Check Studio vs Distant
					if ((cr.getWorkZone() == WorkZone.N_A) ||
							(cr.getWorkZone() == zone)) {
						rule = cr;
						// don't break - we want the LAST one!
					}
				}
			}
		}
		return rule;
	}

	/**
	 * Find the last rule of a specific rule type for the current timecard and
	 * current day. {@link #prepareDailyContractRules(DailyTime, AuditEvent)}
	 * should have been called prior to this method being used.
	 *
	 * @param rType The ruleType desired; if null, null is returned.
	 * @param event The Audit trail instance.
	 * @return A rule of the requested type that applies to the current timecard,
	 *         or null if none is found.
	 */
	/* package */ ContractRule findLastDailyContractRuleByType(RuleType rType, AuditEvent event) {
		ContractRule rule = null;
		if (rType != null && dailyContractRules != null) {
			// Find the last rule matching the requested rule type
			for (ContractRule cr : dailyContractRules) {
				if (cr.getRuleType() == rType) {
					rule = cr;
					// don't break - we want the LAST one!
				}
			}
			//dumpRules(dRules);
		}
		return rule;
	}

	/**
	 * Evaluate any extra filters ("additional restrictions") that may exist for
	 * the given rule, against data from the supplied DailyTime, and possibly
	 * data from the Production or WeeklyTimecard used to instantiate this
	 * instance.
	 *
	 * @param rule The rule to be evaluated.
	 * @param dt The DailyTime which may supply data for particular
	 *            restrictions. If null, those restrictions will be ignored
	 *            (treated as true).
	 * @return True if all the filters/restrictions specified in the
	 *         ContractRule either evaluate to true, or cannot be evaluated
	 *         because a null DailyTime was supplied.
	 */
	private boolean evaluateExtraFilters(ContractRule rule, DailyTime dt) {
		if (rule.getExtraFilters() == null || rule.getExtraFilters().length() == 0) {
			return true;
		}
		if (rule.getRuleFilters() == null) {
			createRuleFilters(rule, htg);
		}

		boolean bRet = rule.getAndFilters(); // assume True for AND, False for OR

		for (RuleFilter rf : rule.getRuleFilters()) {
			if (rf != null && rf instanceof RuleFilterImpl) {
				bRet = ((RuleFilterImpl)rf).evaluate(htg.production, dt, htg);
				if (rule.getAndFilters()) {
					if (! bRet) { // for AND operation, exit on first False result
						break;
					}
				}
				else if (bRet) { // for OR operation, exit on first True result
					break;
				}
			}
		}
		return bRet;
	}
	/**
	 * Parse the 'extraFilters' field in a ContractRule and create the equivalent List of
	 * RuleFilter`s and add them to the rule.
	 * @param rule
	 */
	private static void createRuleFilters(ContractRule rule, HtgData pHtg) {
		List<RuleFilter> filters = new ArrayList<>();
		String[] parts;
		boolean andFilters = true;
		if (rule.getExtraFilters().indexOf('+') > 0) {
			parts = rule.getExtraFilters().split("\\+");
			andFilters = false;
		}
		else {
			parts = rule.getExtraFilters().split(";");
		}
		RuleFilter rf = null;

		for (String part : parts) {
			rf = RuleFilterImpl.createFilter(part);
			if (rf == null) {
				String msg = MsgUtils.formatMessage("Timecard.HTG.ParsingError", part, rule.getUseRuleKey(), rule.getId());
				EventUtils.logError(msg);
				TimecardService.errorTrace(pHtg, "Timecard.HTG.ParsingError", part, rule.getUseRuleKey(), rule.getId());
			}
			else {
				filters.add(rf);
			}
		}
		rule.setRuleFilters(filters);
		rule.setAndFilters(andFilters);
	}

	/**
	 * Filter a list of rules and return only those matching the given RuleType.
	 *
	 * @param rules An existing List of ContractRule`s.
	 * @param rtype The type of interest.
	 * @return A new, non-null, but possibly empty, List of ContractRule`s
	 *         consisting of all the rules in the given List whose type matches
	 *         the given type.
	 */
	@SuppressWarnings("unused")
	private List<ContractRule> filterRules(List<ContractRule> rules, RuleType rtype) {
		List<ContractRule> fRules = new ArrayList<>();

		for (ContractRule rule : rules) {
			if ( rule.getRuleType() == rtype) {
				fRules.add(rule);
			}
		}
		dumpRules(fRules);
		return fRules;
	}

	/**
	 * Add a rule description to the audit trail. This also tracks which rules
	 * have been added, so that the detail information is only added once.
	 *
	 * @param rule The rule to add.
	 * @param event The AuditEvent to which the information is added.
	 * @param nullText The text to add to the audit trail in the event that the
	 *            'rule' parameter is null.
	 */
	public void appendRule(Rule rule, AuditEvent event, String nullText) {
		if (rule == null) {
			event.appendSummary(nullText);
		}
		else {
			event.appendSummary(/*rule.getType().getBrief() +*/ " " + rule.getRuleKey());
			if (! rulesDetailed.contains(rule.getRuleKey())) {
				// only output detailed view of a rule once
				rulesDetailed.add(rule.getRuleKey());
//				event.appendDetail(rule.toAudit()); // skip detail since only for internal use
			}
		}
	}

	/**
	 * For debugging: output a list of rules to the log file.
	 * @param rules The List of rules to be "dumped" to the debug log.
	 */
	public static void dumpRules(List<ContractRule> rules) {

		if (rules == null) {
			return;
		}
		// Condensed output:
		StringBuffer buff = new StringBuffer(500);
		for (ContractRule rule : rules) {
			buff.append(rule.getId()).append(':').append(rule.getUseRuleKey()).append("; ");
		}
		log.debug(buff.toString());
		// Long form output:
//		for (ContractRule rule : rules) {
//			log.debug(rule.getId() + ": " + rule.getRuleType() + "=" + rule.getUseRuleKey());
//		}
//		log.debug("");
	}

	/**
	 * Make appropriate log/event entries when we don't find a supporting rule
	 * that was referenced in a ContractRule.
	 *
	 * @param cr
	 *            The failing ContractRule.
	 * @param event
	 *            The current audit event trail.
	 */
	private void logMissingRule(ContractRule cr, AuditEvent event) {
		EventUtils.logError("Missing supporting rule, key=" + cr.getUseRuleKey() + "; TC:" + htg.weeklyTimecard);
		event.appendDetail("Supporting rule not found: " + cr.getUseRuleKey());
	}

	/**
	 * Used by test routines to invoke our filter-parsing code.
	 *
	 * @param filter The string of text representing one or more filter
	 *            expressions, such as "CL>8; WR<20.5".
	 *
	 * @return The List of RuleFilter objects that correspond to the supplied
	 *         filter string.
	 */
	public static List<RuleFilter> testFilterParsing(String filter) {
		ContractRule rule = new ContractRule();
		rule.setExtraFilters(filter);
		createRuleFilters(rule, null);
		return rule.getRuleFilters();
	}

	/**See {@link #contractRules}. */
	public List<ContractRule> getContractRules() {
		return contractRules;
	}
	/**See {@link #contractRules}. */
	public void setContractRules(List<ContractRule> contractRules) {
		this.contractRules = contractRules;
	}

	/**See {@link #specialService}. */
	public SpecialRuleService getSpecialService() {
		return specialService;
	}
	/**See {@link #specialService}. */
	public void setSpecialService(SpecialRuleService specialService) {
		this.specialService = specialService;
	}

	/** See {@link #contractRuleDAO}. */
	private ContractRuleDAO getContractRuleDAO() {
		if (contractRuleDAO == null) {
			contractRuleDAO = (ContractRuleDAO)ContractRuleDAO.getInstance();
		}
		return contractRuleDAO;
	}

	/** See {@link #callBackRuleDAO}. */
	private CallBackRuleDAO getCallBackRuleDAO() {
		if (callBackRuleDAO == null) {
			callBackRuleDAO = (CallBackRuleDAO)CallBackRuleDAO.getInstance();
		}
		return callBackRuleDAO;
	}

	/** See {@link #employmentDAO}. */
	private EmploymentDAO getEmploymentDAO() {
		if (employmentDAO == null) {
			employmentDAO = (EmploymentDAO)EmploymentDAO.getInstance();
		}
		return employmentDAO;
	}

	/** See {@link #goldenRuleDAO}. */
	private GoldenRuleDAO getGoldenRuleDAO() {
		if (goldenRuleDAO == null) {
			goldenRuleDAO = (GoldenRuleDAO)GoldenRuleDAO.getInstance();
		}
		return goldenRuleDAO;
	}

	/** See {@link #guaranteeRuleDAO}. */
	private GuaranteeRuleDAO getGuaranteeRuleDAO() {
		if (guaranteeRuleDAO == null) {
			guaranteeRuleDAO = (GuaranteeRuleDAO)GuaranteeRuleDAO.getInstance();
		}
		return guaranteeRuleDAO;
	}

	/** See {@link #holidayListRuleDAO}. */
	private HolidayListRuleDAO getHolidayListRuleDAO() {
		if (holidayListRuleDAO == null) {
			holidayListRuleDAO = (HolidayListRuleDAO)HolidayListRuleDAO.getInstance();
		}
		return holidayListRuleDAO;
	}

	/** See {@link #holidayRuleDAO}. */
	private HolidayRuleDAO getHolidayRuleDAO() {
		if (holidayRuleDAO == null) {
			holidayRuleDAO = HolidayRuleDAO.getInstance();
		}
		return holidayRuleDAO;
	}

	/** See {@link #mpvRuleDAO}. */
	private MpvRuleDAO getMpvRuleDAO() {
		if (mpvRuleDAO == null) {
			mpvRuleDAO = (MpvRuleDAO)MpvRuleDAO.getInstance();
		}
		return mpvRuleDAO;
	}

	/** See {@link #ntPremiumRuleDAO}. */
	private NtPremiumRuleDAO getNtPremiumRuleDAO() {
		if (ntPremiumRuleDAO == null) {
			ntPremiumRuleDAO = (NtPremiumRuleDAO)NtPremiumRuleDAO.getInstance();
		}
		return ntPremiumRuleDAO;
	}

	/** See {@link #onCallRuleDAO}. */
	private OnCallRuleDAO getOnCallRuleDAO() {
		if (onCallRuleDAO == null) {
			onCallRuleDAO = (OnCallRuleDAO)OnCallRuleDAO.getInstance();
		}
		return onCallRuleDAO;
	}

	/** See {@link #overtimeRuleDAO}. */
	private OvertimeRuleDAO getOvertimeRuleDAO() {
		if (overtimeRuleDAO == null) {
			overtimeRuleDAO = (OvertimeRuleDAO)OvertimeRuleDAO.getInstance();
		}
		return overtimeRuleDAO;
	}

	/** See {@link #restRuleDAO}. */
	private RestRuleDAO getRestRuleDAO() {
		if (restRuleDAO == null) {
			restRuleDAO = (RestRuleDAO)RestRuleDAO.getInstance();
		}
		return restRuleDAO;
	}

	/** See {@link #weeklyRuleDAO}. */
	private WeeklyRuleDAO getWeeklyRuleDAO() {
		if (weeklyRuleDAO == null) {
			weeklyRuleDAO = (WeeklyRuleDAO)WeeklyRuleDAO.getInstance();
		}
		return weeklyRuleDAO;
	}

}
