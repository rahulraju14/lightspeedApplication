package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.CallBackRule;

/**
 * A data access object (DAO) providing persistence and search support for
 * CallBackRule entities.
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
 * @see com.lightspeedeps.model.CallBackRule
 */
public class CallBackRuleDAO extends RuleDAO<CallBackRule> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CallBackRuleDAO.class);

	//property constants
//	private static final String PRODUCTION_ID = "productionId";
//	private static final String RULE_KEY = "ruleKey";
//	private static final String MINIMUM_HOURS = "minimumHours";
//	private static final String MINIMUM_PERCENT = "minimumPercent";
//	private static final String RATE = "rate";
//	private static final String DESCRIPTION = "description";
//	private static final String NOTES = "notes";

	public static CallBackRuleDAO getInstance() {
		return (CallBackRuleDAO)getInstance("CallBackRuleDAO");
	}

}
