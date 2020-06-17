package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Approver;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Approver entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Approver
 */

public class ApproverDAO extends BaseTypeDAO<Approver> {
	private static final Log log = LogFactory.getLog(ApproverDAO.class);

	// property constants
	public static final String SHARED = "shared";
	public static final String CONTACT = "contact";

	public static ApproverDAO getInstance() {
		return (ApproverDAO)getInstance("ApproverDAO");
	}

	/**
	 * Determine if the given Contact is an Approver of any type.
	 *
	 * @param contact The Contact of interest.
	 * @return True iff the Contact is some type of Approver (either production
	 *         approver, or department approver, or out-of-line (one-time)
	 *         approver.
	 */
	public boolean existsContact(Contact contact) {
		// The contact can only be an approver if it exists as the Approver.contact
		// field for some Approver in the database.
		return existsProperty( CONTACT, contact);
	}

	/**
	 * Find any out-of-line Approver`s that have the given Approver as their
	 * nextApprover value.  This is used by the ApproverHierarchy bean when a
	 * given Approver is about to be removed from the hierarchy.
	 *
	 * @param approver The Approver value to look for in nextApprover fields of
	 *            out-of-line (unshared) Approver`s.
	 * @return A non-null, but possibly empty, List of Approver`s which are
	 *         out-of-line (the share field is false), and whose nextApprover
	 *         value equals the given Approver parameter.
	 */
	@SuppressWarnings("unchecked")
	public List<Approver> findByNextUnshared(Approver approver) {
		String query = "from Approver where shared = false and " +
				" nextApprover = ? ";
		return find(query, approver);
	}

	@Transactional
	public void attachDirty(Approver instance) {
		try {
			if (instance.getId() != null && instance.getNextApproverId() != null &&
					instance.getId().intValue() == instance.getNextApproverId().intValue()) {
				log.debug("id=" + instance.getId() + ", next=" + instance.getNextApproverId());
				throw new IllegalArgumentException("Approver instance will create loop in Approver chain");
			}
			else {
				super.attachDirty(instance);
			}
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/**
	 * @param approverId The id of the Approver object of interest.
	 * @return The database id of the User object associated with the given
	 *         Approver (via it's Contact reference).
	 */
	public Integer findUserId(Integer approverId) {
		if (approverId == null) {
			return null;
		}
		String query = "select c.user.id from Approver a, Contact c" +
				" where a.id = ? and a.contact = c";
		@SuppressWarnings("unchecked")
		List<Integer> ids = find(query, approverId);
		Integer userId = null;
		if (ids != null && ids.size() > 0) {
			userId = ids.get(0);
		}
		return userId;
	}

//	public static ApproverDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ApproverDAO)ctx.getBean("ApproverDAO");
//	}

}