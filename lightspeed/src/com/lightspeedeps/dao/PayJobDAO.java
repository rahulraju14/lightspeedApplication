package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.PayJob;

/**
 * A data access object (DAO) providing persistence and search support for
 * PayJob entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.PayJob
 */

public class PayJobDAO extends BaseTypeDAO<PayJob> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayJobDAO.class);

	// property constants
//	private static final String JOB_NUMBER = "jobNumber";
//	private static final String LOCATION_CODE = "locationCode";
//	private static final String PROD_EPISODE = "prodEpisode";
//	private static final String OCC_CODE = "occCode";
//	private static final String FREE = "free";
//	private static final String RATE = "rate";
//	private static final String BOX_AMT = "boxAmt";
//	private static final String ACCOUNT_MAJOR = "accountMajor";
//	private static final String ACCOUNT_DTL = "accountDtl";
//	private static final String ACCT_SET = "acctSet";
//	private static final String CUSTOM_MULT = "customMult";


	public static PayJobDAO getInstance() {
		return (PayJobDAO)getInstance("PayJobDAO");
	}

//	public static PayJobDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (PayJobDAO)ctx.getBean("PayJobDAO");
//	}

}
