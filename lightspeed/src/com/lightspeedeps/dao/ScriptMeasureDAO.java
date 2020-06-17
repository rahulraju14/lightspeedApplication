package com.lightspeedeps.dao;

import com.lightspeedeps.model.ScriptMeasure;

/**
 * A data access object (DAO) providing persistence and search support for
 * ScriptMeasure entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.ScriptMeasure
 */

public class ScriptMeasureDAO extends BaseTypeDAO<ScriptMeasure> {
	// property constants
//	private static final String SCENES = "scenes";
//	private static final String PAGES = "pages";
//	private static final String MINUTES = "minutes";
//	private static final String SETUPS = "setups";

	public static ScriptMeasureDAO getInstance() {
		return (ScriptMeasureDAO)getInstance("ScriptMeasureDAO");
	}

//	public static ScriptMeasureDAO getFromApplicationContext(
//			ApplicationContext ctx) {
//		return (ScriptMeasureDAO) ctx.getBean("ScriptMeasureDAO");
//	}

}