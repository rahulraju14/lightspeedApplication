package com.lightspeedeps.dao;

import com.lightspeedeps.model.MpvRule;

/**
 * A data access object (DAO) providing persistence and search support for
 * MpvRule entities.
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
 * @see com.lightspeedeps.model.MpvRule
 */
public class MpvRuleDAO extends RuleDAO<MpvRule> {
	//private static final Log log = LogFactory.getLog(MpvRuleDAO.class);

	//property constants
//	private static final String MPV1_RATE = "mpv1Rate";
//	private static final String MPV2_RATE = "mpv2Rate";
//	private static final String MPV3_RATE = "mpv3Rate";
//	private static final String MPV_OTHER_RATE = "mpvOtherRate";

	public static MpvRuleDAO getInstance() {
		return (MpvRuleDAO)getInstance("MpvRuleDAO");
	}

}
