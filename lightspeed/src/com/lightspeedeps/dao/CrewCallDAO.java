package com.lightspeedeps.dao;

import com.lightspeedeps.model.CrewCall;

/**
 * A data access object (DAO) providing persistence and search support for
 * CrewCall entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.CrewCall
 */

public class CrewCallDAO extends BaseTypeDAO<CrewCall> {

	// property constants
//	private static final String LINE_NUMBER = "lineNumber";
//	private static final String COUNT = "count";
//	private static final String ROLE_NAME = "roleName";
//	private static final String NAME = "name";
//	private static final String CALL_TYPE = "callType";

	public static CrewCallDAO getInstance() {
		return (CrewCallDAO)getInstance("CrewCallDAO");
	}

//	public static CrewCallDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (CrewCallDAO) ctx.getBean("CrewCallDAO");
//	}

}