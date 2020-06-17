package com.lightspeedeps.dao;

import com.lightspeedeps.model.OtherCall;

/**
 * A data access object (DAO) providing persistence and search support for
 * OtherCall entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.OtherCall
 */

public class OtherCallDAO extends BaseTypeDAO<OtherCall> {
	// property constants
//	public static final String TYPE = "type";
//	public static final String LINE_NUMBER = "lineNumber";
//	public static final String DESCRIPTION = "description";
//	public static final String COUNT = "count";

	public static OtherCallDAO getInstance() {
		return (OtherCallDAO)getInstance("OtherCallDAO");
	}

//	public static OtherCallDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (OtherCallDAO) ctx.getBean("OtherCallDAO");
//	}

}