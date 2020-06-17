package com.lightspeedeps.dao;

import com.lightspeedeps.model.ActivityLog;

/**
 * A data access object (DAO) providing persistence and search support for
 * ActivityLog entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.ActivityLog
 */
public class ActivityLogDAO extends BaseTypeDAO<ActivityLog> {

	public static ActivityLogDAO getInstance() {
		return (ActivityLogDAO)getInstance("ActivityLogDAO");
	}

}
