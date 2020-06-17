package com.lightspeedeps.dao;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Event;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.EventType;

/**
 * A data access object (DAO) providing persistence and search support for
 * PayRate entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Event
 */

public class EventDAO extends BaseTypeDAO<Event> {
	private static final Log log = LogFactory.getLog(EventDAO.class);

	// property constants
//	private static final String TYPE = "type";
	private static final String PRODUCTION = "production";
//	private static final String USERNAME = "username";
//	private static final String DESCRIPTION = "description";

	public static EventDAO getInstance() {
		return (EventDAO)getInstance("EventDAO");
	}

	public List<Event> findByProduction(Production prod) {
		return findByProperty(PRODUCTION, prod);
	}

	/**
	 * Used by Event List page to find all Events matching the
	 * user's criteria.
	 * @param query
	 * @param values
	 * @return Non-null List of Event
	 */
	@SuppressWarnings("unchecked")
	public List<Event> findByQuery(String query, List<Object> values) {
		log.debug("finding Events with query: '" + query + "'" );
		if (log.isDebugEnabled()) {
			for (Object o: values) {
				log.debug("value: "+o);
			}
		}
		String queryString = "from Event e where " + query;
		return find(queryString, values.toArray());
	}

	/**
	 * Find the count of Events of a particular type in the database logged
	 * within a given time frame.
	 *
	 * @param type The EventType of interest.
	 * @param begin The starting date/time (inclusive) of the Events to be
	 *            counted.
	 * @param end The ending date/time (inclusive) of the Events to be counted.
	 * @return The count of Events of the specified EventType whose timestamp is
	 *         within the range of the begin and ending timestamp parameters
	 *         (may be zero).
	 */
	public long findCountTypeInRange(EventType type, Date begin, Date end) {

		String queryString = "select count(id) from Event ev "
				+ " where type = ? and "
				+ " Start_time >= ? and "
				+ " Start_time <= ? ";

		Object[] values = {type, begin, end};

		return findCount(queryString, values);
	}

	/**
	 * Find the Events of a particular type in the database logged within a
	 * given time frame and return them in descending id order.
	 *
	 * @param type The EventType of interest.
	 * @param begin The starting date/time (inclusive) of the Events to be
	 *            retrieved.
	 * @param end The ending date/time (inclusive) of the Events to be
	 *            retrieved.
	 * @return A non-null (but possibly empty) List of Event`s whose timestamp
	 *         is within the range of the begin and ending timestamp parameters.
	 */
	@SuppressWarnings("unchecked")
	public List<Event> findTypeInRange(EventType type, Date begin, Date end) {

		String queryString = "from Event ev "
				+ " where type = ? and "
				+ " Start_time >= ? and "
				+ " Start_time <= ? "
				+ " order by id desc ";

		Object[] values = {type, begin, end};

		return find(queryString, values);
	}

	/**
	 * Find when the given user last logged in successfully to the application.
	 *
	 * @return The Date of the last successful login record for the given user,
	 *         or null if no login record is found.
	 */
	public Date findLastLoginByUser(User user) {
		Date date = null;

		String queryString = "from Event ev "
				+ " where type = 'LOGIN_OK' and "
				+ " userName = ? "
				+ " order by id desc ";

		Object[] values = {user.getEmailAddress()};

		Event evt = findOne(queryString, values);
		if (evt != null) {
			date = evt.getStartTime();
		}

		return date;
	}

	/**
	 * EventDAO has its own version of 'save' which does NOT call EventUtils if
	 * an error occurs -- since that tends to cause an infinite recursion of
	 * errors if we cannot write to the Event table.
	 *
	 * @see com.lightspeedeps.dao.BaseDAO#save(java.lang.Object)
	 */
	@Override
	@Transactional
	public Integer save(Object transientInstance) {
		try {
			getHibernateTemplate().save(transientInstance);
			log.debug("save successful for Event");
		}
		catch (RuntimeException re) {
			log.error("exception: " + re.getLocalizedMessage());
			throw re;
		}
		return null;
	}

//	public static EventDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (EventDAO) ctx.getBean("EventDAO");
//	}

}