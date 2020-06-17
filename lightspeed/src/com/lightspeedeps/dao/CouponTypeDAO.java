package com.lightspeedeps.dao;

import com.lightspeedeps.model.CouponType;

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

public class CouponTypeDAO extends BaseTypeDAO<CouponType> {

	// property constants
//	private static final String DESCRIPTION = "description";
//	private static final String MESSAGE = "message";
//	private static final String DISCOUNT_TYPE = "discountType";
//	private static final String AMOUNT = "amount";
//	private static final String SKU_PATTERN = "skuPattern";
//	private static final String NUMBER_USED = "maximumUses";

	public static CouponTypeDAO getInstance() {
		return (CouponTypeDAO)getInstance("CouponTypeDAO");
	}

//	public static CouponTypeDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (CouponTypeDAO)ctx.getBean("CouponDAO");
//	}

}