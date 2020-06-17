package com.lightspeedeps.dao;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.PersistentObject;

/**
 * This class provides method signatures that match a particular DAO's return
 * type (e.g., "Contact" for "ContactDAO") without each individual DAO class
 * having to include those methods themselves. All the DAO classes simply
 * "extend BaseTypeDAO<Contact>", for example. This removes a large amount of
 * nearly-redundant code from all of the DAO classes, simplifying maintenance.
 *
 * @param <T> The class of the object to be returned by the methods provided
 *            here. This will be one of the persistent entity classes in the
 *            {@link com.lightspeedeps.model} package.
 *
 * @see BaseDAO
 */
public class BaseTypeDAO<T> extends BaseDAO {
	private static final Log log = LogFactory.getLog(BaseDAO.class);


	/**
	 * This will get run by the framework for each bean. Here we determine the
	 * entity name (for Hibernate purposes) of the objects managed by this DAO,
	 * by analyzing our generic class parameter. The entity name is kept in a
	 * field and allows the base classes (this class, and BaseDAO) to provide
	 * methods, such as findAll(), that would otherwise have to be coded
	 * separately in each subclass.
	 * <p/>
	 * It would be very bad if this method did not get called, so we make it
	 * final, to prevent any subclass from overriding it.
	 *
	 * @see org.springframework.dao.support.DaoSupport#initDao()
	 */
	@Override
	protected final void initDao() {
		entityClass = (Class<?>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		entityName = entityClass.getName();
		entityName = entityName.substring(entityName.lastIndexOf('.')+1);
	}

	/**
	 * See if the supplied instance is in the Session cache. If not, get a fresh
	 * instance (which will be in the cache) and return it. This situation
	 * arises frequently because of the ICEfaces extended-request scope. The
	 * objects we hold often live across requests, but a new Hibernate session
	 * is created for each request.
	 *
	 * @param instance The object instance to locate; if null, then this method
	 *            returns null -- no error condition is raised.
	 *
	 * @return Either the same instance, or a new one, or null; in either
	 *         non-null case, the instance is definitely in the current
	 *         hibernate session cache.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public T refresh(PersistentObject instance) {
		if (instance == null) {
			return null;
		}
		if (instance.getId() == null) {
			log.warn("Refresh called on transient instance (id=null), class=" + entityName);
			return (T)instance;
		}
		return (T)refresh(instance, instance.getId());
	}

	@SuppressWarnings("unchecked")
	public T findById(Integer id) {
		return (T)super.findByIdBase(id);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findAll() {
		return super.findAll();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findByProperty(String propertyName, Object value) {
		return super.findByProperty(propertyName, value);
	}

	/**
	 * @see com.lightspeedeps.dao.BaseDAO#findOneByProperty(java.lang.String, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T findOneByProperty(String propertyName, Object value) {
		return (T)super.findOneByProperty(propertyName, value);
	}

	/**
	 * @see com.lightspeedeps.dao.BaseDAO#findOne(java.lang.String, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T findOne(String query, Object value) {
		return (T)super.findOne(query, value);
	}

	/**
	 * @see com.lightspeedeps.dao.BaseDAO#findOne(java.lang.String, java.lang.Object[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T findOne(String query, Object[] values) {
		return (T)super.findOne(query, values);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<T> findByNamedQuery(String query, Map<String, Object> values) {
		return super.findByNamedQuery(query, values);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<T> findByNamedQuery(String query) {
		return super.findByNamedQuery(query);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<T> findByNamedQuery(String query, String parameter, List<Integer> integerList) {
		return super.findByNamedQuery(query, parameter, integerList);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<T> findByNamedQuery(String query, Map<String, Object> values1, Map<String, List<Integer>> values2) {
		return super.findByNamedQuery(query, values1, values2);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T findOneByNamedQuery(String query, Map<String, Object> values) {
		return (T)super.findOneByNamedQuery(query, values);
	}

	@SuppressWarnings("unchecked")
	@Transactional
	@Override
	public T merge(Object detachedInstance) {
		return (T)super.merge(detachedInstance);
	}

}
