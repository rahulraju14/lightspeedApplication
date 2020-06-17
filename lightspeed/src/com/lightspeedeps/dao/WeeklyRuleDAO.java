package com.lightspeedeps.dao;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.WeeklyRule;

/**
 * A data access object (DAO) providing persistence and search support for
 * WeeklyRule entities.
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
 * @see com.lightspeedeps.model.WeeklyRule
 */
public class WeeklyRuleDAO extends RuleDAO<WeeklyRule> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(WeeklyRuleDAO.class);

	//property constants
//	private static final String PRODUCTION_ID = "productionId";
//	private static final String RULE_KEY = "ruleKey";
//	private static final String OVERTIME_INCREMENT = "overtimeIncrement";
//	private static final String OVERTIME_WEEKLY_BREAK = "overtimeWeeklyBreak";
//	private static final String CUME_HOURS = "cumeHours";
//	private static final String CUME_DAYS = "cumeDays";
//	private static final String OVERTIME_RATE = "overtimeRate";
//	private static final String DESCRIPTION = "description";
//	private static final String NOTES = "notes";

	public static WeeklyRuleDAO getInstance() {
		return (WeeklyRuleDAO)getInstance("WeeklyRuleDAO");
	}

}
