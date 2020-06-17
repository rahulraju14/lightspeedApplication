package com.lightspeedeps.dao;

import com.lightspeedeps.model.CateringLog;

/**
 * A data access object (DAO) providing persistence and search support for
 * CateringLog entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.CateringLog
 */

public class CateringLogDAO extends BaseTypeDAO<CateringLog> {

	// property constants
//	private static final String MEAL = "meal";
//	private static final String VENDOR = "vendor";
//	private static final String MEAL_COUNT = "mealCount";
//	private static final String NOTE = "note";

	public static CateringLogDAO getInstance() {
		return (CateringLogDAO)getInstance("CateringLogDAO");
	}

//	public static CateringLogDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (CateringLogDAO) ctx.getBean("CateringLogDAO");
//	}

}