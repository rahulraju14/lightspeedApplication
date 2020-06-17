package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.CastCall;

/**
 * A data access object (DAO) providing persistence and search support for
 * CastCall entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.CastCall
 */

public class CastCallDAO extends BaseTypeDAO<CastCall> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CastCallDAO.class);
	// property constants
//	private static final String ACTOR_ID = "actorId";
//	private static final String CHARACTER_NAME = "characterName";
//	private static final String NAME = "name";
//	private static final String STATUS = "status";
//	private static final String NOTE = "note";

	public static CastCallDAO getInstance() {
		return (CastCallDAO)getInstance("CastCallDAO");
	}

//	public static CastCallDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (CastCallDAO) ctx.getBean("CastCallDAO");
//	}

}