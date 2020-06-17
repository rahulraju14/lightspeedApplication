package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.PayBreakdown;

/**
 * A data access object (DAO) providing persistence and search support for
 * PayBreakdown entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.PayBreakdown
 */

public class PayBreakdownDAO extends BaseTypeDAO<PayBreakdown> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayBreakdownDAO.class);

	// property constants
//	private static final String LINE_NUMBER = "lineNumber";
//	private static final String WEEKLY_ID = "weeklyTimecard.id";
//	private static final String WEEKLY_TIMECARD = "weeklyTimecard";
//	private static final String ACCOUNT_MAJOR = "accountMajor";
//	private static final String ACCOUNT_DTL = "accountDtl";
//	private static final String ACCOUNT_SET = "accountSet";
//	private static final String FREE = "free";
//	private static final String CATEGORY = "category";
//	private static final String HOURS = "hours";
//	private static final String MULTIPLIER = "multiplier";
//	private static final String RATE = "rate";
//	private static final String TOTAL = "total";

	public static PayBreakdownDAO getInstance() {
		return (PayBreakdownDAO)getInstance("PayBreakdownDAO");
	}

//	public int findCountByTimecard(WeeklyTimecard wtc) {
//		String query =  "select count(id) from PayBreakdown where " + WEEKLY_TIMECARD + " = ?";
//		return findCount(query, wtc).intValue();
//	}

	//	public static PayBreakdownDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (PayBreakdownDAO)ctx.getBean("PayBreakdownDAO");
//	}

}