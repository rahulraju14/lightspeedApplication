package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Change;

/**
 * A data access object (DAO) providing persistence and search support for
 * Change entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Change
 */

public class ChangeDAO extends BaseTypeDAO<Change> {
	private static final Log log = LogFactory.getLog(ChangeDAO.class);

	// property constants
//	private static final String TYPE = "type";
//	private static final String DESCRIPTION = "description";

	public static ChangeDAO getInstance() {
		return (ChangeDAO)getInstance("ChangeDAO");
	}


	/**
	 * Used by Event List page to find all Events matching the
	 * user's criteria.
	 * @param query
	 * @param values
	 * @return Non-null List of Event
	 */
	@SuppressWarnings("unchecked")
	public List<Change> findByQuery(String query, List<Object> values) {
		log.debug("finding Events with query: '" + query + "'" );
		String queryString = "from Change c where " + query;
		return find(queryString, values.toArray());
	}

//	public static ChangeDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ChangeDAO) ctx.getBean("ChangeDAO");
//	}

}