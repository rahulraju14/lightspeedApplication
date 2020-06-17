package com.lightspeedeps.dao;

import com.lightspeedeps.model.UnitStripboard;

/**
 * A data access object (DAO) providing persistence and search support for
 * UnitStripboard entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.UnitStripboard
 */

public class UnitStripboardDAO extends BaseTypeDAO<UnitStripboard> {

	// property constants
//	private static final String SHOOTING_DAYS = "shootingDays";

	public static UnitStripboardDAO getInstance() {
		return (UnitStripboardDAO)getInstance("UnitStripboardDAO");
	}

// 	public static UnitStripboardDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (UnitStripboardDAO)ctx.getBean("UnitStripboardDAO");
//	}

}