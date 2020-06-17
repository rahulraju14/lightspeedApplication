package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.GoldenRule;

/**
 * A data access object (DAO) providing persistence and search support for
 * GoldenRule entities.
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
 * @see com.lightspeedeps.model.GoldenRule
 */
public class GoldenRuleDAO extends RuleDAO<GoldenRule> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(GoldenRuleDAO.class);

	//property constants
//	private static final String PRODUCTION_ID = "productionId";
//	private static final String RULE_KEY = "ruleKey";
//	private static final String WORK_OR_ELAPSED = "workOrElapsed";
//	private static final String BREAK_ = "break_";
//	private static final String MULTIPLIER = "multiplier";
//	private static final String BREAK2 = "break2";
//	private static final String MULTIPLIER2 = "multiplier2";
//	private static final String BREAK3 = "break3";
//	private static final String MULTIPLIER3 = "multiplier3";
//	private static final String DESCRIPTION = "description";
//	private static final String NOTES = "notes";

	public static GoldenRuleDAO getInstance() {
		return (GoldenRuleDAO)getInstance("GoldenRuleDAO");
	}

}
