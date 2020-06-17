/**
 * File: BaseDAO.java
 */
package com.lightspeedeps.dao;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.springframework.dao.DataAccessException;
//import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.PersistentObject;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * This class provides common methods shared across most of the "DAO" classes.
 * See the comment block at the end of this class's source file for sample
 * methods to use when creating a new DAO subclass.
 * <p>
 * Every subclass of BaseDAO (e.g., UserDAO) must have a findId(Integer) method;
 * that method's return type (e.g., User) must match the Hibernate entity name
 * defined for the subclass, which is typically the same name as the SQL table
 * it supports.
 *
 * @see BaseTypeDAO
 */
public class BaseDAO extends HibernateDaoSupport {
	private static final Log log = LogFactory.getLog(BaseDAO.class);

	/** The name of the 'entity' maintained by this particular instance (which is
	 * some particular subclass of BaseTypeDAO). The entity name is NOT the full
	 * class name, but just the final qualifier of the class name, e.g.,
	 * "Address", not "com.lightspeedeps.model.Address". This value is used in a
	 * number of the Hibernate queries implemented in this class. */
	protected String entityName = null;

	/** The Class of the entity maintained by this particular instance, e.g.,
	 * "com.lightspeedeps.model.Address". Note that the class of each BaseDAO
	 * instance will be a subclass of BaseTypeDAO). */
	protected Class<?> entityClass = null;

	/**
	 * Get an instance of this DAO class. This is the method used throughout LS
	 * code for retrieving a DAO instance, rather than using the ServiceFinder
	 * call. This would allow us to change the underlying DAO implementation
	 * without changing all our instance requestors.
	 *
	 * @param beanName The name of the DAO bean. In LS, this is always the
	 *            capitalized name of the final qualifier of the class name,
	 *            e.g., "ContactDAO". These, in turn, are always the (partial)
	 *            class name of the supported class, e.g., "Contact", with the
	 *            suffix "DAO".
	 * @return An instance of this DAO class.
	 */
	public static Object getInstance(String beanName) {
		return ServiceFinder.findBean(beanName);
	}

	/**
	 * @return same as getSession(), so we only have to suppress deprecation in one place!
	 */
	@SuppressWarnings("deprecation")
	protected Session getHibernateSession() {
		return getSession();
	}

	/**
	 * The method used to retrieve a specific database entity by its unique
	 * identifier, which relies on this base class' knowledge of the entity
	 * class managed by this DAO instance.
	 *
	 * @param id The unique Integer identifier for the entity to be retrieved.
	 * @see #findById(String,Integer)
	 */
	public Object findByIdBase(Integer id) {
		return findById(entityName, id);
	}

	/**
	 * Find a specific database entity by its unique identifier and "class". In
	 * this application, all our database objects have a unique identifier which
	 * is always the 'id' field, that is generally maintained by Hibernate.
	 *
	 * @param type The pseudo-class of the object to be retrieved. The true
	 *            class name is created by prefixing this value with
	 *            "com.lightspeedeps.model.".
	 * @param id The unique Integer identifier for the entity to be retrieved.
	 * @return The persisted object, as retrieved from either the database or
	 *         from Hibernate cache. Note that not all the fields (columns) of
	 *         an object are necessarily retrieved from the database at this
	 *         time, since Hibernate uses "lazy initialization" to a significant
	 *         extent.
	 */
	public Object findById(String type, Integer id) {
		log.debug("get " + type + " with id: " + id);
		try {
			Object instance = getHibernateTemplate().get(
					"com.lightspeedeps.model." + type, id);
			//debugSession();
			return instance;
		}
		catch (DataAccessException e1) {
			// Skip EventUtil to avoid recursion in case db is not accessible
			log.error("Exception: ", e1);
			throw e1;
		}
		catch (RuntimeException e) {
			String msg = "exception fetching " + type + " (id=" + id + "): " + e.getLocalizedMessage();
			// Use low-level logEvent to avoid recursion when EventUtils does getCurrentProject()
			EventUtils.logEvent(EventType.APP_ERROR, null, null, null,
					msg + Constants.NEW_LINE + EventUtils.traceToString(e));
			log.error(msg, e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Remove a Hibernate entity from the session cache. After this it is
	 * considered a "transient" (i.e., non-persisted) instance. This can be used
	 * to force Hibernate to re-read an object from the database, or to prevent
	 * a changed object from having its changes "automatically" applied to the
	 * database.
	 *
	 * @param persistentInstance The currently persisted entity to be removed
	 *            from the cache.
	 */
	@SuppressWarnings("rawtypes")
	@Transactional
	public void evict(Object persistentInstance) {
		if (persistentInstance != null) {
			try {
				getHibernateTemplate().evict(persistentInstance);
				Integer id = null;
				if (persistentInstance instanceof PersistentObject) {
					id = ((PersistentObject)persistentInstance).getId();
				}
				log.info("evict successful for " + persistentInstance.getClass().getName() + ", id=" + id);
			}
			catch (RuntimeException re) {
				EventUtils.logError(re);
				throw new LoggedException(re);
			}
		}
	}

	/**
	 * See if the supplied instance is in the Session cache. If not, get a fresh instance (which
	 * will be in the cache) and return it. This situation arises frequently because of the ICEfaces
	 * extended-request scope. The objects we hold often live across requests, but a new Hibernate
	 * session is created for each request.
	 *
	 * @param instance The object instance to locate.
	 * @param claz The POJO class of the instance; note that we can't use "instance.getClass()",
	 *            because sometimes the instance is a cglibEnhanced class!
	 * @param id The database id to use to fetch a new instance, if the supplied object is not in
	 *            the session cache.
	 * @return Either the same instance, or a new one; in either case, the instance is definitely in
	 *         the current hibernate session cache.
	 */
//	@SuppressWarnings("unchecked")
//	protected Object refresh(Object instance, Class claz, Integer id) {
//		if ( ! getHibernateTemplate().contains(instance)) {
//			instance = getHibernateTemplate().get(claz, id);
//		}
//		return instance;
//	}

	/**
	 * See if the supplied instance is in the Session cache. If not, get a fresh instance (which
	 * will be in the cache) and return it. This situation arises frequently because of the ICEfaces
	 * extended-request scope. The objects we hold often live across requests, but a new Hibernate
	 * session is created for each request.
	 *
	 * @param instance The object instance to locate.
	 * @param id The database id to use to fetch a new instance, if the supplied object is not in
	 *            the session cache.
	 * @return Either the same instance, or a new one; in either case, the instance is definitely in
	 *         the current hibernate session cache.
	 */
	protected Object refresh(Object instance, Integer id) {
		if ( ! getHibernateTemplate().contains(instance)) {
			instance = getHibernateTemplate().get(entityClass, id);
			//log.debug("loading instance, id=" + id);
		}
		return instance;
	}

	@SuppressWarnings("rawtypes")
	public List findByProperty(String propertyName, Object value) {
		try {
			if (propertyName.indexOf(".") > 0) {
				String queryString = "from " + entityName + " as model where model." + propertyName + "= ?";
				return find(queryString, value);
			}
			else {
				Criteria criteria = getHibernateSession().createCriteria(entityClass);
				criteria.add(Restrictions.eq(propertyName, value));
				criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
				List list = criteria.list();
				if (log.isDebugEnabled()) {
					log.debug("Count=" + list.size() + ", Qry: from " + entityName + " where " +
							propertyName + "=" + toString(value));
				}
				return list;
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
	}

	/**
	 * Find a single instance of an entity where a particular property has the
	 * given value. This is typically used where business logic requires that
	 * there only be a single such instance.
	 *
	 * @param propertyName The Hibernate property name of the field.
	 * @param value The value to match.
	 * @return The first entity found where the specified property has the given
	 *         value; or null if no such entity exists.
	 */
	@SuppressWarnings("rawtypes")
	public Object findOneByProperty(String propertyName, Object value) {
		try {
			Criteria criteria = getHibernateSession().createCriteria(entityClass);
			criteria.add(Restrictions.eq(propertyName, value));
			criteria.setMaxResults(1);
			List list = criteria.list();
			if (log.isDebugEnabled()) {
				log.debug("Count=" + list.size() + ", Qry: from " + entityName + " where " +
						propertyName + "=" + toString(value));
			}
			if ( ! list.isEmpty()) {
				return list.get(0);
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
		return null;
	}

	/**
	 * Find a single instance of an entity satisfying the supplied query. This
	 * is typically used where business logic requires that there only be a
	 * single such instance.  If more than one exists, the first one in the
	 * result list is returned.  Therefore, the caller could affect which one
	 * is returned by including an "order by" clause in the query.
	 *
	 * @param query The HQL query to perform.
	 * @param value The value to substitute into the query string.
	 * @return The first entity found satisfying the query; or null if no such
	 *         entity exists.
	 */
	@SuppressWarnings("rawtypes")
	public Object findOne(String query, Object value) {
		Object obj = null;
		getHibernateTemplate().setMaxResults(1);
		List list = find(query, value);
		getHibernateTemplate().setMaxResults(0);
		if (list.size() > 0) {
			obj = list.get(0);
		}
		//debugSession();
		return obj;
	}

	/**
	 * Execute a query specifying a limited number of returned values.
	 *
	 * @param query The HQL query to run.
	 * @param value The value string for the query.
	 * @param limit The maximum number of entries to return.
	 * @return A non-null (but possibly empty) List of results matching the
	 *         query.
	 */
	@SuppressWarnings("rawtypes")
	public List findLimited(String query, Object value, int limit) {
		getHibernateTemplate().setMaxResults(limit);
		List list = getHibernateTemplate().find(query, value);
		if (log.isDebugEnabled()) {
			log.debug("Count=" + (list==null ? 0 : list.size()) + ", Qry: " + query + ", value=" + toString(value));
		}
		//debugSession();
		getHibernateTemplate().setMaxResults(0);
		//debugSession();
		return list;
	}

	/**
	 * Find a single instance of an entity satisfying the supplied query. This
	 * is typically used where business logic requires that there only be a
	 * single such instance.  If more than one exists, the first one in the
	 * result list is returned.  Therefore, the caller could affect which one
	 * is returned by including an "order by" clause in the query.
	 *
	 * @param query The HQL query to perform.
	 * @param values The values to substitute into the query string.
	 * @return The first entity found satisfying the query; or null if no such
	 *         entity exists.
	 */
	@SuppressWarnings("rawtypes")
	public Object findOne(String query, Object[] values) {
		Object obj = null;
		getHibernateTemplate().setMaxResults(1);
		List list = find(query, values);
		getHibernateTemplate().setMaxResults(0);
		if (list.size() > 0) {
			obj = list.get(0);
		}
		//debugSession();
		return obj;
	}

	/**
	 * Issue an HQL query that does not require any substitution values.
	 *
	 * @param query The HQL query string.
	 * @return A non-null (but possibly empty) List of results.
	 */
	@SuppressWarnings("rawtypes")
	public List find(String query) {
		//log.debug("query: " + query);
		try {
			List list = getHibernateTemplate().find(query);
			log.debug("Count=" + (list==null ? 0 : list.size()) + ", Qry: " + query);
			//debugSession();
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/**
	 * Issue an HQL query that requires one or more substitution values.
	 *
	 * @param query The HQL query string; should contain a "?" for each of the
	 *            values passed in the 'values' parameter.
	 * @param value The values to be substituted in the query, in positional
	 *            order (matching the order of "?"s in the query.
	 * @return A non-null (but possibly empty) List of results.
	 */
	@SuppressWarnings("rawtypes")
	public List find(String query, Object[] values) {
		//log.debug("query: " + query);
		try {
			List list = getHibernateTemplate().find(query, values);
			if (log.isDebugEnabled()) {
				String msg = "[";
				if (values != null) {
					for (Object obj : values) {
						if (msg.length() == 1) {
							msg += toString(obj);
						}
						else {
							msg += ", " + toString(obj);
						}
					}
					msg += "]";
				}
				else {
					msg = "null";
				}
				log.debug("Count=" + (list==null ? 0 : list.size()) + ", Qry: " + query + ", values=" + msg);
			}
			//debugSession();
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/**
	 * Issue an HQL query that requires a single substitution value.
	 *
	 * @param query The HQL query string; should contain a single "?" for the
	 *            replacement value.
	 * @param value The value to be substituted in the query.
	 * @return A non-null (but possibly empty) List of results.
	 */
	@SuppressWarnings("rawtypes")
	public List find(String query, Object value) {
		//log.debug("query: " + query);
		try {
			List list = getHibernateTemplate().find(query, value);
			if (log.isDebugEnabled()) {
				log.debug("Count=" + (list==null ? 0 : list.size()) + ", Qry: " + query + ", value=" + toString(value));
			}
			//debugSession();
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}


	/**
	 * Return the count returned by the given query, with the given substitution
	 * value.
	 *
	 * @param query The hibernate query, which must return a count. The query
	 *            should typically be something like
	 *            "select count(id) from ... where ...".
	 * @param value The Object value to substitute into the query in place of a
	 *            "?". May be null if the query does not have any substitution
	 *            parameters.
	 * @return A Long value, 0 or greater, which is the count returned by the
	 *         query.
	 */
	@SuppressWarnings("unchecked")
	public Long findCount(String query, Object value) {
		Long count = 0L;
		try {
			List<Long> list = find(query, value);
			if (list.size() > 0) {
				Object entry = list.get(0);
				if (entry == null) {
					// leave it at 0
				}
				else if (entry instanceof Long) { // standard for count(x)
					count = (Long)entry;
				}
				else if (entry instanceof Integer) {
					// useful for max(x), min(x), etc. where x is Integer
					count = new Long((Integer)entry);
				}
				else { // curious - will probably throw an error
					count = list.get(0);
				}
			}
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		log.debug("count=" + count);
		//debugSession();
		return count;
	}

	/**
	 * Return the count returned by the given query, with the given substitution
	 * values.
	 *
	 * @param query The hibernate query, which must return a count. The query
	 *            should typically be something like
	 *            "select count(id) from ... where ...".
	 * @param values The array of Object values to substitute into the query, in
	 *            the proper order (matching the order of "?"s in the query).
	 * @return A Long value, 0 or greater, which is the count returned by the
	 *         query.
	 */
	@SuppressWarnings("unchecked")
	public Long findCount(String query, Object[] values) {
		Long count = 0L;
		try {
			List<Long> list = find(query, values);
			if (list.size() > 0) {
				Object entry = list.get(0);
				if (entry == null) {
					// leave it at 0
				}
				else if (entry instanceof Long) { // standard for count(x)
					count = (Long)entry;
				}
				else if (entry instanceof Integer) {
					// useful for max(x), min(x), etc. where x is Integer
					count = new Long((Integer)entry);
				}
				else { // curious - will probably throw an error
					count = list.get(0);
				}
			}
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		log.debug("count=" + count);
		//debugSession();
		return count;
	}

	/**
	 * Determine if at least one entity exists in the database with the given
	 * property value.
	 *
	 * @param propertyName The name of the property to be searched for.
	 * @param value The value of the property to match on.
	 * @return True iff at least one object in the database table has the value
	 *         given for the specified property. (The table searched depends on
	 *         the subclass invoking this method.)
	 */
	public boolean existsProperty(String propertyName, Object value) {
		try {
			Criteria criteria = getHibernateSession().createCriteria(entityClass);
			criteria.add(Restrictions.eq(propertyName, value));
			Projection pj = Projections.id(); // return only id field
			criteria.setProjection(pj);
			criteria.setMaxResults(1);
			boolean b = (criteria.list().size()) > 0;
			if (log.isDebugEnabled()) {
				log.debug(b + ": from " + entityName + " where " + propertyName + "=" + toString(value));
			}
			return b;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
	}

	/**
	 * Determine if at least one entity exists in the database that satisfies
	 * the given query.
	 *
	 * @param queryString The HQL query to issue; this should begin with "from
	 *            < table > ..." -- it should NOT include the leading "Select < x > "
	 *            phrase.
	 * @param value The value (if any) to be substituted into the query.
	 * @return True iff at least one row satisfies the query and value given.
	 */
	@SuppressWarnings("rawtypes")
	public boolean exists(String queryString, Object value) {
		//debugSession();
		boolean b = false;
		try {
			// We can select a constant, as we don't need any data from the row:
			queryString = "select 1 " + queryString;
			getHibernateTemplate().setMaxResults(1); // no need to retrieve more than 1 row
			List list = getHibernateTemplate().find(queryString, value);
			getHibernateTemplate().setMaxResults(0); // reset for other callers!
			b = list.size() > 0;
			if (log.isDebugEnabled()) {
				log.debug(b + ": " + queryString + ", value=" + toString(value));
			}
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		return b;
	}


	/**
	 * Determine if at least one entity exists in the database that satisfies
	 * the given query.
	 *
	 * @param queryString The HQL query to issue; this should begin with "from
	 *            < table > ..." -- it should NOT include the leading "Select < x > "
	 *            phrase.
	 * @param values The values to be substituted into the query.
	 * @return True iff at least one row satisfies the query and values given.
	 */
	@SuppressWarnings("rawtypes")
	public boolean exists(String queryString, Object[] values) {
		//debugSession();
		boolean b = false;
		try {
			// We can select a constant, as we don't need any data from the row:
			queryString = "select 1 " + queryString;
			getHibernateTemplate().setMaxResults(1); // no need to retrieve more than 1 row
			List list = getHibernateTemplate().find(queryString, values);
			getHibernateTemplate().setMaxResults(0); // reset for other callers!
			b = list.size() > 0;
			if (log.isDebugEnabled()) {
				String msg = "[";
				if (values != null) {
					for (Object obj : values) {
						if (msg.length() == 1) {
							msg += toString(obj);
						}
						else {
							msg += ", " + toString(obj);
						}
					}
					msg += "]";
				}
				else {
					msg = "null";
				}
				log.debug(b + ": " + queryString + ", values=" + msg);
			}
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		return b;
	}

	@SuppressWarnings("rawtypes")
	public List findAll() {
		try {
			Criteria criteria = getHibernateSession().createCriteria(entityClass);
			List list = criteria.list();
			log.debug("Count=" + list.size() + ", Qry: (all) " + entityName);
			return list;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
	}


	@SuppressWarnings("rawtypes")
	@Transactional
	public Integer save(Object transientInstance) {
		String className = "";
		try {
			className = transientInstance.getClass().getName();
			getHibernateTemplate().save(transientInstance);
			Integer id = null;
			if (transientInstance instanceof PersistentObject) {
				id = ((PersistentObject)transientInstance).getId();
			}
			log.info("save successful for " + className + ", id=" + id);
			//debugSession();
			return id;
		}
		// Some 'sample' code to do more analysis of an error; tested briefly.
//		catch (DataAccessException de) {
//			log.error("save **FAILED** for class=" + className);
//			Throwable priorCause = de;
//			Throwable cause = de.getCause();
//			while (cause != null && priorCause != cause) {
//				log.info(cause.getClass());
//				if (cause instanceof SQLException) {
//					log.info(cause.getMessage());
//					log.info((((SQLException)cause).getErrorCode()));
//				}
//				priorCause = cause;
//				cause = cause.getCause();
//			}
//			EventUtils.logError(de);
//			throw new LoggedException(de);
//		}
		catch (RuntimeException re) {
			log.error("save **FAILED** for class=" + className);
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	@SuppressWarnings("rawtypes")
	@Transactional
	public void delete(Object persistentInstance) {
		String className = "";
		Integer id = null;
		try {
			className = persistentInstance.getClass().getName();
			if (persistentInstance instanceof PersistentObject) {
				id = ((PersistentObject)persistentInstance).getId();
			}
			getHibernateTemplate().delete(persistentInstance);
			log.info("delete successful for " + className + ", id=" + id);
			//debugSession();
		}
		catch (RuntimeException re) {
			log.error("delete **FAILED** for " + className + ", id=" + id);
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	@SuppressWarnings("rawtypes")
	@Transactional
	public void attachDirty(Object instance) {
		try {
			getHibernateTemplate().saveOrUpdate(instance);
			Integer id = null;
			if (instance instanceof PersistentObject) {
				id = ((PersistentObject)instance).getId();
			}
			log.info("attach successful for " + instance.getClass().getName() + ", id=" + id);
			//debugSession();
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	@SuppressWarnings("rawtypes")
	@Transactional
	public void attachClean(Object instance) {
		try {
			getHibernateTemplate().lock(instance, LockMode.NONE);
			Integer id = null;
			if (instance instanceof PersistentObject) {
				id = ((PersistentObject)instance).getId();
			}
			log.info("attach successful for " + instance.getClass().getName() + ", id=" + id);
			//debugSession();
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	@SuppressWarnings("rawtypes")
	//@Transactional - all our callers need to be Transactional
	public Object merge(Object detachedInstance) {
		try {
			Object result = getHibernateTemplate().merge(detachedInstance);
			Integer id = null;
			if (detachedInstance instanceof PersistentObject) {
				id = ((PersistentObject)detachedInstance).getId();
			}
			log.info("merge successful for " + detachedInstance.getClass().getName() + ", id=" + id);
			//debugSession();
			return result;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/**
	 * Flush all pending saves, updates and deletes to the database.
	 * NB: Callers should already be Transactional!
	 */
	protected void flush() {
		log.debug("flushing...");
		getHibernateTemplate().flush();
		log.debug("hibernate session flushed");
		//debugSession();
	}

	/**
	 * Flush all pending saves, updates and deletes to the database, then
	 * clear the Session of all objects.
	 */
	@Transactional
	public void flushAndClear() {
		log.debug("flushing...");
		getHibernateTemplate().flush();
		log.debug("hibernate session flushed; clearing...");
		getHibernateTemplate().clear();
		log.debug("hibernate session cleared.");
		//debugSession();
	}

	public boolean checkDb() {
		long count = -1;
		try {
//			if (Math.random() < 0.5) { // for testing
//				System.out.println("throwing fake error!!");
//				throw new DataAccessResourceFailureException("test exception handling");
//			}
			String queryString = "select count(id) from Product";
			count = (Long)getHibernateTemplate().find(queryString).get(0);
		}
		catch (DataAccessException d) {
			log.error("****** DATABASE CHECK FAILED *******");
			log.error("DataAccessException: ", d);
		}
		catch (Exception e) {
			log.error("****** DATABASE CHECK FAILED *******");
			log.error("Exception: ", e);
		}
		log.debug("count=" + count);
		return (count >= 0);
	}

	/**
	 * Issue a native SQL query.
	 * @param queryString
	 * @return The result of the query
	 */
	public Object sqlQuery(String queryString, boolean hasResult) {
		try {
			Query query = getHibernateSession().createSQLQuery(queryString);
			Object result = null;
			if (hasResult) {
				result = query.list();
			}
			else {
				query.executeUpdate();
			}
			//debugSession();
			return result;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Run a native SQL update (non-query) statement containing two variables to
	 * be inserted in the query. LS-2028
	 *
	 * @param queryString The native SQL query string to be executed.
	 * @param id The Integer field to be inserted as the ":id" value.
	 * @param field The String field to be inserted as the ":field" value.
	 */
	public void sqlUpdate(String queryString, Integer id, String field) {
		try {
			Query query = getHibernateSession().createSQLQuery(queryString)
					.setInteger("id", id)
					.setString("field", field);
			query.executeUpdate();
			//debugSession();
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return;
	}

	/**
	 * Run a native SQL update (non-query) statement containing three variables to
	 * be inserted in the query. LS-2028
	 *
	 * @param queryString The native SQL query string to be executed.
	 * @param id The Integer field to be inserted as the ":id" value.
	 * @param field1 The String field to be inserted as the ":field1" value.
	 * @param field2 The String field to be inserted as the ":field2" value.
	 */
	public void sqlUpdate(String queryString, Integer id, String field1, String field2) {
		try {
			Query query = getHibernateSession().createSQLQuery(queryString)
					.setInteger("id", id)
					.setString("field1", field1)
					.setString("field2", field2);
			query.executeUpdate();
			//debugSession();
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return;
	}

	/**
	 * Update an individual field, using an HQL (not SQL) query string.
	 * @param queryString The HQL query (update) to run.
	 * @param id The data to use as the ":id" value in the query.
	 * @param field The data to use as the ":field" value in the query.
	 */
	@Transactional
	public void update(String queryString, Integer id, String field) {
		try {
			Query query = getHibernateSession().createQuery(queryString)
					.setInteger("id", id)
					.setString("field", field);
			query.executeUpdate();
			//debugSession();
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return;
	}

	/** Method fires a Named Query on to the database with the specified parameters
	 *  passed as a map of string and object value
	 * @param query Named Query
	 * @param values map<string object>
	 * @return result list
	 */
	@SuppressWarnings("rawtypes")
	public List findByNamedQuery(String query, Map<String, Object> values) {
		try {
			Query que = buildQuery(query, values);
			List list = que.list();
			log.debug("From "+entityName +", Query "+query +", size returned "+list.size());
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/** Method fires a plain Named query on to the database without any parameters
	 * @param query Named query
	 * @return result list
	 */
	@SuppressWarnings("rawtypes")
	public List findByNamedQuery(String query) {
		try {
			Query que = getHibernateSession().getNamedQuery(query);
			List list = que.list();
			log.debug("From "+entityName +", Query "+query +", size returned "+list.size());
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/** Method fires a Named Query on to the database with the list of integer id's
	 * @param query Named Query
	 * @param parameter parameter in the where clause
	 * @param integerList list of id's of the parameter key
	 * @return result list
	 */
	@SuppressWarnings("rawtypes")
	public List findByNamedQuery(String query,String parameter,List<Integer> integerList) {
		try {
			Query que = getHibernateSession().getNamedQuery(query);
			que.setParameterList(parameter, integerList);
			List list = que.list();
			log.debug("From "+entityName +", Query "+query+" Parameter" +parameter + ", Size of Id list "+integerList.size() +", size returned "+list.size());
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/** Method fires a Named Query on to the database with two maps,
	 * a map of string and object value and a map of string and list of integer id's.
	 * @param query Named Query
	 * @param values1 map<String, Object>
	 * @param values2 map<String, List<Integer>>
	 * @return result list
	 */
	@SuppressWarnings("rawtypes")
	public List findByNamedQuery(String query, Map<String, Object> values1, Map<String, List<Integer>> values2) {
		try {
			Query que = buildQuery(query, values1);
			for (String key : values2.keySet()) {
				que.setParameterList(key, values2.get(key));
			}
			List list = que.list();
			log.debug("From "+entityName +", Query "+query +", size returned "+list.size());
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/** Method fires a Named Query on to the database with the parameters,
	 * a map of string and object value and a list of integer id's.
	 * @param query Named Query
	 * @param values map<string object>
	 * @param parameter parameter in the where clause
	 * @param integerList list of id's of the parameter key
	 * @return result list
	 */
	@SuppressWarnings("rawtypes")
	public List findByNamedQuery(String query, Map<String, Object> values, String parameter, List<Integer> integerList) {
		try {
			List list;
			if (integerList.size() == 0) {
				list = new ArrayList();
			}
			else {
				Query que = buildQuery(query, values);
				que.setParameterList(parameter, integerList);
				list = que.list();
			}
			log.debug("From "+entityName +", Query "+query+" Parameter" +parameter + ", Size of Id list "+integerList.size() +", size returned "+list.size());
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/** Method fires a Named Query on to the database with the specified parameters
	 *  passed as a map of string and object value
	 * @param query Named Query
	 * @param values map<string object>
	 * @return count of the result list
	 */
	public Long findCountByNamedQuery(String query, Map<String, Object> values) {
		Long count = 0L;
		try {
			Query que = buildQuery(query, values);
			@SuppressWarnings("rawtypes")
			List list = que.list();
			if (list.size() > 0) {
				Object entry = list.get(0);
				if (entry instanceof Long) { // standard for count(x)
					count = (Long)entry;
				}
			}
			log.debug("From "+entityName +", Query "+query +", count returned "+count);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		return count;
	}

	/** Method fires a Named Query on to the database with the specified parameters
	 *  passed as a map of string and object value
	 * @param query Named Query
	 * @return count of the result list
	 */
	@SuppressWarnings("rawtypes")
	public Long findCountByNamedQuery(String query) {
		Long count = 0L;
		try {
			Query que = getHibernateSession().getNamedQuery(query);
			List list = que.list();
			if (list.size() > 0) {
				Object entry = list.get(0);
				if (entry instanceof Long) { // standard for count(x)
					count = (Long)entry;
				}
			}
			log.debug("From "+entityName +", Query "+query +", count returned "+count);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		log.debug("count=" + count);
		return count;
	}

	/**
	 * Execute a Named Query on the database with the specified parameters
	 * passed as a map of String and Object values.
	 *
	 * @param query The name of a NamedQuery which is expected to return a
	 *            boolean value.
	 * @param values Map<String, Object> of parameters for the query.
	 * @return The result of the query, either true or false. Will NOT return
	 *         null. If, for some reason, the query returns an empty result set,
	 *         or a value that is not a Boolean, then false is returned.
	 */
	public boolean findBooleanByNamedQuery(String query, Map<String, Object> values) {
		Boolean result = false;
		try {
			Query que = buildQuery(query, values);
			@SuppressWarnings("rawtypes")
			List list = que.list();
			if (list.size() > 0) {
				Object entry = list.get(0);
				if (entry instanceof Boolean) { // expected for this method
					result = (Boolean)entry;
				}
			}
			log.debug("From " + entityName + ", Query " + query + ", returned=" + result);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		return result.booleanValue();
	}

	/** Method fires a Named Query on to the database with the specified parameters
	 *  passed as a map of string and object value.
	 * @param query Named Query
	 * @param values map<string object>
	 * @return List<Integer>, List of Integer id's from the result.
	 */
	@SuppressWarnings("unchecked")
	public List<Integer> findIntegerListByNamedQuery(String query, Map<String, Object> values) {
		try {
			Query que = buildQuery(query, values);
			List<Integer> list = que.list();
			log.debug("From "+entityName +", Query "+query +", size returned "+list.size());
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/** Method fires a Named Query on to the database with the specified parameters
	 *  passed as a map of string and object value
	 * @param query Named Query
	 * @param values map<string object>
	 * @return result single object
	 */
	public Object findOneByNamedQuery(String query, Map<String, Object> values) {
		try {
			Query que = buildQuery(query, values);
			que.setMaxResults(1);
			@SuppressWarnings("rawtypes")
			List list = que.list();
			log.debug("From " + entityName + ", Query " + query + ", size returned " + list.size());
			if (! list.isEmpty()) {
				return list.get(0);
			}
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		return null;
	}

	/**
	 * Create a Query object from the named query string, and a map of
	 * parameters to be added to the Query.
	 *
	 * @param query The name of the NamedQuery.
	 * @param values A map of <String, Object> to be added to the query as
	 *            parameters. Each key (String) is a parameter name, and
	 *            the mapped Object is the parameter value.
	 * @return The completed Query.
	 */
	private Query buildQuery(String query, Map<String, Object> values) {
		Query que = getHibernateSession().getNamedQuery(query);
		log.debug("parameter pairs passed: " + values.size());
		for (String key : values.keySet()) {
			que.setParameter(key, values.get(key));
		}
		return que;
	}

	/**
	 * Create a Map<String, Object> for use in named-query processing, and add
	 * an entry formed from the parameters given.
	 *
	 * @param label The parameter name used in the query; this will be the key in
	 *            the Map entry created.
	 * @param value The object to be referenced in the query; this will be the
	 *            value in the Map <key,value> pair.
	 * @return A new instance of a Map containing the single entry generated
	 *         from the parameters.
	 */
	protected static Map<String, Object> map(String label, Object value) {
		Map<String, Object> valueMap = new HashMap<>();
		valueMap.put(label, value);
		return valueMap;
	}

	public void updateByProcedure(int docChainId) {
		try {
			Query query = getHibernateSession().createSQLQuery("CALL deleteOrUpdateDocument(:docChainId)").addEntity(entityClass).setParameter("docChainId", docChainId);
			query.executeUpdate();
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Get an Object's "toString" value, while ignoring any exception it might
	 * throw in the process.
	 *
	 * @param value The object of interest
	 * @return Either the Object's toString() value, or "null" if it is null, or
	 *         "ERROR" if an exception is thrown by the toString() method.
	 */
	private String toString(Object value) {
		String msg;
		if (value != null) {
			try { // prevent debug code from causing error!
				msg = value.toString();
			}
			catch (Exception e) {
				msg = "ERROR";
			}
		}
		else {
			msg = "null";
		}
		return msg;
	}

	@SuppressWarnings("unused")
	private void debugSession() {
		try {
			Session session = SessionFactoryUtils.getSession(getHibernateTemplate().getSessionFactory(), false);
			log.debug("session: " + session.hashCode());
		}
		catch (Exception e) {
			log.debug("session not available: " + e.getLocalizedMessage());
		}
	}

/*
	Sample code for inclusion in DAO classes

	public static _type_DAO getInstance() {
		return (_type_DAO)getInstance("_type_DAO");
	}

	public _type_ refresh(_type_ instance) {
		return (_type_)super.refresh(instance, _type_.class, instance.getId());
	}

*/

}
