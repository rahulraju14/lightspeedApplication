package com.lightspeedeps.dao;

import com.lightspeedeps.model.DprDays;

/**
 * A data access object (DAO) providing persistence and search support for
 * DprDays entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.DprDays
 */

public class DprDaysDAO extends BaseTypeDAO<DprDays> {

	// property constants
//	private static final String FIRST_UNIT = "firstUnit";
//	private static final String SECOND_UNIT = "secondUnit";
//	private static final String REHEARSE = "rehearse";
//	private static final String TEST = "test";
//	private static final String WAS = "was";
//	private static final String RESHOOT = "reshoot";

	public static DprDaysDAO getInstance() {
		return (DprDaysDAO)getInstance("DprDaysDAO");
	}

//	public static DprDaysDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (DprDaysDAO) ctx.getBean("DprDaysDAO");
//	}

}