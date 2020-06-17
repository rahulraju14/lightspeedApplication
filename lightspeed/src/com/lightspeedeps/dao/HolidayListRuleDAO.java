package com.lightspeedeps.dao;

import com.lightspeedeps.model.HolidayListRule;

/**
 * A data access object (DAO) providing persistence and search support for
 * HolidayListRule entities.
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
 * @see com.lightspeedeps.model.HolidayListRule
 */
public class HolidayListRuleDAO extends RuleDAO<HolidayListRule> {
	//private static final Log log = LogFactory.getLog(HolidayListRuleDAO.class);

	private static final String DEFAULT_HOLIDAY_LIST_RULE = "HL-STD";

	public static HolidayListRuleDAO getInstance() {
		return (HolidayListRuleDAO)getInstance("HolidayListRuleDAO");
	}

	public HolidayListRule findDefaultRule() {
		return findOneByRuleKey(DEFAULT_HOLIDAY_LIST_RULE);
	}

}
