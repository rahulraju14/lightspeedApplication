package com.lightspeedeps.dao;

import com.lightspeedeps.model.DateRange;

/**
 * A data access object (DAO) providing persistence and search support for
 * DateRange entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.DateRange
 */

public class DateRangeDAO extends BaseTypeDAO<DateRange> {

	// property constants
//	private static final String DESCRIPTION = "description";

	public static DateRangeDAO getInstance() {
		return (DateRangeDAO)getInstance("DateRangeDAO");
	}

//	public static DateRangeDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (DateRangeDAO) ctx.getBean("DateRangeDAO");
//	}

}