package com.lightspeedeps.dao;

import com.lightspeedeps.model.PayExpense;

/**
 * A data access object (DAO) providing persistence and search support for
 * PayExpense entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.PayExpense
 */
public class PayExpenseDAO extends BaseTypeDAO<PayExpense> {
//	private static final Log log = LogFactory.getLog(PayExpenseDAO.class);

	// property constants
//	public static final String LINE_NUMBER = "lineNumber";
//	public static final String PROD_EPISODE = "prodEpisode";
//	public static final String ACCOUNT_MAJOR = "accountMajor";
//	public static final String ACCOUNT_DTL = "accountDtl";
//	public static final String ACCOUNT_SET = "accountSet";
//	public static final String CATEGORY = "category";
//	public static final String CUSTOM_NAME = "customName";
//	public static final String QUANTITY = "quantity";
//	public static final String RATE = "rate";
//	public static final String TOTAL = "total";


	public static PayExpenseDAO getInstance() {
		return (PayExpenseDAO)getInstance("PayExpenseDAO");
	}

//	public static PayExpenseDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (PayExpenseDAO)ctx.getBean("PayExpenseDAO");
//	}

}
