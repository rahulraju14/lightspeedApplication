package com.lightspeedeps.dao;

import com.lightspeedeps.model.DprScene;


/**
 * A data access object (DAO) providing persistence and search support for
 * DprScene entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.DprScene
 */

public class DprSceneDAO extends BaseTypeDAO<DprScene> {
	// property constants
//	private static final String SCENE_NUMBER = "sceneNumber";
//	private static final String TITLE = "title";
//	private static final String LOCATION = "location";
//	private static final String MILEAGE = "mileage";

	public static DprSceneDAO getInstance() {
		return (DprSceneDAO)getInstance("DprSceneDAO");
	}

//	public static DprSceneDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (DprSceneDAO) ctx.getBean("DprSceneDAO");
//	}

}