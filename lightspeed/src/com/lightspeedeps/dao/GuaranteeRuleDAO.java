package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.GuaranteeRule;

/**
 * A data access object (DAO) providing persistence and search support for
 * OvertimeRule entities.
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
 * @see com.lightspeedeps.model.OvertimeRule
 */
public class GuaranteeRuleDAO extends RuleDAO<GuaranteeRule> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(GuaranteeRuleDAO.class);

	//property constants
//	private static final String PRODUCTION_ID = "productionId";
//	private static final String RULE_KEY = "ruleKey";
//	private static final String MINIMUM_CALL = "minimumCall";
//	private static final String STRAIGHT_MULT = "straightMult";
//	private static final String MAX_HOURS = "maxHours";
//	private static final String DESCRIPTION = "description";
//	private static final String NOTES = "notes";

	public static GuaranteeRuleDAO getInstance() {
		return (GuaranteeRuleDAO)getInstance("GuaranteeRuleDAO");
	}

}
