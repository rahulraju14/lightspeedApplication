package com.lightspeedeps.dao;

import com.lightspeedeps.model.ExtraTime;


/**
 * A data access object (DAO) providing persistence and search support for
 * ExtraTime entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.ExtraTime
 */

public class ExtraTimeDAO extends BaseTypeDAO<ExtraTime> {

	// property constants
//	private static final String DESCRIPTION = "description";
//	private static final String COUNT = "count";
//	private static final String RATE = "rate";
//	private static final String ADJUSTMENT = "adjustment";
//	private static final String OT = "ot";
//	private static final String MPV = "mpv";
//	private static final String MILES = "miles";
//	private static final String MISC = "misc";
//	private static final String NOTES = "notes";

	public static ExtraTimeDAO getInstance() {
		return (ExtraTimeDAO)getInstance("ExtraTimeDAO");
	}

//	public static ExtraTimeDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ExtraTimeDAO) ctx.getBean("ExtraTimeDAO");
//	}

}