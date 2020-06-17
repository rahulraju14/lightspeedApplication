package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Coupon;

/**
 * A data access object (DAO) providing persistence and search support for
 * Coupon entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Coupon
 */

public class CouponDAO extends BaseTypeDAO<Coupon> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CouponDAO.class);

	// property constants
	public static final String CODE = "code";
//	private static final String DESCRIPTION = "description";
//	private static final String DISCOUNT_TYPE = "discountType";
//	private static final String AMOUNT = "amount";
//	private static final String SKU_PATTERN = "skuPattern";
//	private static final String NUMBER_USED = "numberUsed";
//	private static final String NUMBER_LEFT = "numberLeft";
//	private static final String REDEEMER_ACCT = "redeemerAcct";
//	private static final String PROD_ID = "prodId";

	public static CouponDAO getInstance() {
		return (CouponDAO)getInstance("CouponDAO");
	}

//	public static CouponDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (CouponDAO)ctx.getBean("CouponDAO");
//	}

}