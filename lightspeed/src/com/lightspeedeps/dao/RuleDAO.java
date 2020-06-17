package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A data access object (DAO) providing persistence and search support for
 * various "Rule" entities. This is a superclass for the DAOs that support
 * the different payroll rule classes.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 */
public class RuleDAO<T> extends BaseTypeDAO<T> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(RuleDAO.class);

	//property constants
	public static final String PRODUCTION_ID = "productionId";
	public static final String RULE_KEY = "ruleKey";
	public static final String DESCRIPTION = "description";

	public List<T> findByProductionId(Object productionId) {
		return findByProperty(PRODUCTION_ID, productionId);
	}

	/**
	 * Find all the rules that match the given rule key.
	 *
	 * @param ruleKey
	 * @return A non-null, but possibly empty, List of rules.
	 */
	public List<T> findByRuleKey(Object ruleKey) {
		return findByProperty(RULE_KEY, ruleKey);
	}

	/**
	 * Find a single rule (if any) matching the given rule key. If more than one
	 * matches, a random one will be returned.
	 *
	 * @param ruleKey
	 * @return A rule with a matching key, or null if no matching rule is found.
	 */
	public T findOneByRuleKey(Object ruleKey) {
		return findOneByProperty(RULE_KEY, ruleKey);
	}

}
