package com.lightspeedeps.dao;

import com.lightspeedeps.model.NtPremiumRule;

/**
 * A data access object (DAO) providing persistence and search support for
 * NtPremiumRule entities.
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
 * @see com.lightspeedeps.model.NtPremiumRule
 */
public class NtPremiumRuleDAO extends RuleDAO<NtPremiumRule> {
	//private static final Log log = LogFactory.getLog(NtPremiumRuleDAO.class);

	//property constants
//	public static final String NO_NP_LIMIT = "noNpLimit";
//	public static final String CALL_START = "callStart";
//	public static final String NP_RATE_START = "npRateStart";
//	public static final String NP_RATE = "npRate";
//	public static final String NP_RATE_END = "npRateEnd";
//	public static final String LATE_CALL_START = "lateCallStart";
//	public static final String LATE_CALL_END = "lateCallEnd";
//	public static final String LATE_RATE = "lateRate";
//	public static final String LATE_RATE_END = "lateRateEnd";
//	public static final String EARLY_CALL_START = "earlyCallStart";
//	public static final String EARLY_CALL_END = "earlyCallEnd";
//	public static final String EARLY_RATE = "earlyRate";
//	public static final String EARLY_RATE_END = "earlyRateEnd";

	public static NtPremiumRuleDAO getInstance() {
		return (NtPremiumRuleDAO)getInstance("NtPremiumRuleDAO");
	}

}
