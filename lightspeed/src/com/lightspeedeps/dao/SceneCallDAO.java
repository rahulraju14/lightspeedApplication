package com.lightspeedeps.dao;

import com.lightspeedeps.model.SceneCall;

/**
 * A data access object (DAO) providing persistence and search support for
 * SceneCall entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.SceneCall
 */

public class SceneCallDAO extends BaseTypeDAO<SceneCall> {
	// property constants
//	private static final String DAY_NUMBER = "dayNumber";
//	private static final String LINE_NUMBER = "lineNumber";
//	private static final String HEADING = "heading";
//	private static final String SYNOPSIS = "synopsis";
//	private static final String NUMBER = "number";
//	private static final String CAST_IDS = "castIds";
//	private static final String DAY_NIGHT = "dayNight";
//	private static final String PAGES = "pages";
//	private static final String LOCATION = "location";

	public static SceneCallDAO getInstance() {
		return (SceneCallDAO)getInstance("SceneCallDAO");
	}

//	public static SceneCallDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (SceneCallDAO) ctx.getBean("SceneCallDAO");
//	}

}