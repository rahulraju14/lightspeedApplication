package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.HolidayRule;

/**
 * A data access object (DAO) providing persistence and search support for
 * HolidayRule entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link BaseDAO} or {@link BaseTypeDAO}.
 * <p>
 * In addition, for DAOs of entities that subclass Rule, some common access
 * functions reside in {@link RuleDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.HolidayRule
 */
public class HolidayRuleDAO extends RuleDAO<HolidayRule> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(HolidayRuleDAO.class);

	//property constants
//	private static final String PRODUCTION_ID = "productionId";
//	private static final String RULE_KEY = "ruleKey";
//	private static final String DAILY_RATE = "dailyRate";
//	private static final String GOLD_RATE = "goldRate";
//	private static final String GOLD_HOURS = "goldHours";
//	private static final String ON_CALL_RATE = "onCallRate";
//	private static final String WEEKLY_RATE = "weeklyRate";
//	private static final String OVERLAP_APPLIES = "overlapApplies";
//	private static final String OVERLAP_DAILY_RATE = "overlapDailyRate";
//	private static final String OVERLAP_GOLD_RATE = "overlapGoldRate";
//	private static final String OVERLAP_GOLD_HOURS = "overlapGoldHours";
//	private static final String PAID = "paid";
//	private static final String NOT_WORKED_HOURS = "notWorkedHours";
//	private static final String NOT_WORKED_ON_CALL_RATE = "notWorkedOnCallRate";
//	private static final String NOT_WORKED_WEEKLY_RATE = "notWorkedWeeklyRate";
//	private static final String DESCRIPTION = "description";
//	private static final String NOTES = "notes";

	public static HolidayRuleDAO getInstance() {
		return (HolidayRuleDAO)getInstance("HolidayRuleDAO");
	}

}
