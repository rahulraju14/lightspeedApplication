package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.PayBreakdownDaily;

/**
 * A data access object (DAO) providing persistence and search support for
 * PayBreakdownDaily entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.PayBreakdownDaily
 */

public class PayBreakdownDailyDAO extends BaseTypeDAO<PayBreakdownDaily> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayBreakdownDailyDAO.class);

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

	public static PayBreakdownDailyDAO getInstance() {
		return (PayBreakdownDailyDAO)getInstance("PayBreakdownDailyDAO");
	}

//	public static PayBreakdownDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (PayBreakdownDailyDAO)ctx.getBean("PayBreakdownDailyDAO");
//	}

}