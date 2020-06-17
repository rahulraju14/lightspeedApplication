package com.lightspeedeps.dao;

import com.lightspeedeps.model.RoleGroup;

/**
 * A data access object (DAO) providing persistence and search support for
 * RoleGroup entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.RoleGroup
 */

public class RoleGroupDAO extends BaseTypeDAO<RoleGroup> {
	// property constants
//	private static final String NAME = "name";
//	private static final String DESCRIPTION = "description";
//	private static final String FILE_ACCESS = "fileAccess";

	public static RoleGroupDAO getInstance() {
		return (RoleGroupDAO)getInstance("RoleGroupDAO");
	}

//	public static RoleGroupDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (RoleGroupDAO) ctx.getBean("RoleGroupDAO");
//	}

}