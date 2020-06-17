package com.lightspeedeps.dao;

import com.lightspeedeps.model.RealLink;

/**
 * A data access object (DAO) providing persistence and search support for
 * RealLink entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.RealLink
 */

public class RealLinkDAO extends BaseTypeDAO<RealLink> {
	// property constants
//	private static final String STATUS = "status";

	public static RealLinkDAO getInstance() {
		return (RealLinkDAO)getInstance("RealLinkDAO");
	}

//	public static RealLinkDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (RealLinkDAO) ctx.getBean("RealLinkDAO");
//	}

}