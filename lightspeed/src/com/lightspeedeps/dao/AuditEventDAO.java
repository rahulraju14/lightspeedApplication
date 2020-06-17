package com.lightspeedeps.dao;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.AuditEvent;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.type.ObjectType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * AuditEvent entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.AuditEvent
 */
public class AuditEventDAO extends BaseTypeDAO<AuditEvent> {
	private static final Log log = LogFactory.getLog(AuditEventDAO.class);

	//property constants
	public static final String TYPE = "type";
	public static final String DEPTH = "depth";
	public static final String USER_ACCOUNT = "userAccount";
	public static final String RELATED_OBJECT_TYPE = "relatedObjectType";
	public static final String RELATED_OBJECT_ID = "relatedObjectId";
	public static final String SUMMARY = "summary";
	public static final String DETAIL = "detail";

	public static AuditEventDAO getInstance() {
		return (AuditEventDAO)getInstance("AuditEventDAO");
	}

	@SuppressWarnings("unchecked")
	public List<AuditEvent> findByObject(ObjectType type, Integer relatedObjectId) {
		Production prod = SessionUtils.getProduction();
		Object[] values = {prod, type, relatedObjectId};
		String queryString = "from AuditEvent where production = ? " +
				" and relatedObjectType = ? " +
				" and relatedObjectId = ?" +
				" order by date desc id asc";
		return find(queryString, values);
	}

	/**
	 * Delete all of the "audit trail" objects related to the given object.
	 *
	 * @param type The object type.
	 * @param id The id of the related object.
	 */
	@Transactional
	public void deleteTrail(ObjectType type, Integer id) {
		List<AuditEvent> events = findByObject(type, id);
		for (AuditEvent event : events) {
			delete(event);
		}
	}

	/**
	 * Save all the (transient) instances in the supplied list.
	 *
	 * @param buffer A Collection of AuditEvent`s to be persisted.
	 */
	@Transactional
	public void save(Collection<AuditEvent> buffer) {
		log.debug("");
		if (buffer != null) {
			try {
				AuditEvent lastEvt = null;
				for (AuditEvent evt : buffer) {
					getHibernateTemplate().save(evt); // Quick save bypasses debug
					lastEvt = evt;
				}
				log.debug(buffer.size() + " objects saved; last id=" + (lastEvt == null ? "null" : lastEvt.getId()));
			}
			catch (RuntimeException re) {
				EventUtils.logError(re);
				throw re;
			}
		}
	}

}
