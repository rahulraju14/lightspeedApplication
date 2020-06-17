package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.OnCallRule;

/**
 * A data access object (DAO) providing persistence and search support for
 * OnCallRule entities.
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
 * @see com.lightspeedeps.model.OnCallRule
 */
public class OnCallRuleDAO extends RuleDAO<OnCallRule> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(OnCallRuleDAO.class);
	//property constants
//	private static final String PRODUCTION_ID = "productionId";
//	private static final String RULE_KEY = "ruleKey";
//	private static final String DAYS1TO5_MULT = "days1to5Mult";
//	private static final String DAYS1TO5_BASE = "days1to5Base";
//	private static final String DAY6_MULT = "day6Mult";
//	private static final String DAY6_BASE = "day6Base";
//	private static final String DAY7_MULT = "day7Mult";
//	private static final String DAY7_BASE = "day7Base";
//	private static final String DESCRIPTION = "description";
//	private static final String NOTES = "notes";

	public static OnCallRuleDAO getInstance() {
		return (OnCallRuleDAO)getInstance("OnCallRuleDAO");
	}

}
