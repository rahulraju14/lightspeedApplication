package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.SpecialRule;

/**
 * A data access object (DAO) providing persistence and search support for
 * SpecialRule entities.
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
 * @see com.lightspeedeps.model.SpecialRule
 */
public class SpecialRuleDAO extends RuleDAO<SpecialRule> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SpecialRuleDAO.class);

	//property constants
//	private static final String PRODUCTION_ID = "productionId";
//	private static final String RULE_KEY = "ruleKey";
//	private static final String LONG_NAME = "longName";
//	private static final String DESCRIPTION = "description";
//	private static final String NOTES = "notes";

	public static SpecialRuleDAO getInstance() {
		return (SpecialRuleDAO)getInstance("SpecialRuleDAO");
	}

}
