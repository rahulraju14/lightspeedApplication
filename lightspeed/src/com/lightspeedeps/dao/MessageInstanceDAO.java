package com.lightspeedeps.dao;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.MessageInstance;
import com.lightspeedeps.type.NotificationMethod;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * MessageInstance entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.MessageInstance
 */

public class MessageInstanceDAO extends BaseTypeDAO<MessageInstance> {
	private static final Log log = LogFactory.getLog(MessageInstanceDAO.class);
	// property constants
//	private static final String SENT_METHOD = "sentMethod";
//	private static final String ACKNOWLEDGED = "acknowledged";

	public static MessageInstanceDAO getInstance() {
		return (MessageInstanceDAO)getInstance("MessageInstanceDAO");
	}

	/**
	 * Find all MessageInstance objects sent using the specified
	 * NotificationMethod and associated with the current Production. Since this
	 * is only used for the "All Messages" display, it excludes "new account"
	 * and "password reset" notifications, as those include a private link which
	 * should not be available to any other user.
	 *
	 * @param sentVia The notification method (e.g., web, email, SMS)
	 * @return A non-null, but possibly empty, List of MessageInstance's.
	 */
	@SuppressWarnings("unchecked")
	public List<MessageInstance> findBySentViaAndProduction(NotificationMethod sentVia) {
		Object values[] = { sentVia, SessionUtils.getProduction() };
		String query = "from MessageInstance m where m.sentMethod = ? " +
				" and m.message.notification.project.production = ? " +
				" and m.message.notification.type not in " +
				"('NEW_ACCOUNT', 'PW_RESET', 'PRODUCTION_CREATED', 'PRODUCTION_UNSUBSCRIBED', " +
				" 'PRODUCTION_RESUBSCRIBED', 'PRODUCTION_UPGRADED') ";
		return find(query, values);
	}

//	@SuppressWarnings("unchecked")
//	public List<MessageInstance> getMessagesByAckContact(Byte ackFlag, Contact contact) {
//			Object values[] = { ackFlag, contact };
//			String query = "from MessageInstance m where m.acknowledged = ? and m.contact = ?";
//			return find(query, values);
//	}

//	@SuppressWarnings("unchecked")
//	public List<MessageInstance> findByContactNotificationType(Contact contact, NotificationType type) {
//		try {
//			Object values[] = { contact, type };
//			String query = "from MessageInstance m where m.contact = ? " +
//					" and m.message.notification.type = ?";
//			List<MessageInstance> results = find(query, values);
//			return results;
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//	}

//	public boolean existsContactNotificationType(Contact contact, NotificationType type) {
//		boolean bRet = false;
//		try {
//			Object values[] = { contact, type };
//			String query = " from MessageInstance m where m.contact = ? " +
//					" and m.message.notification.type = ?";
//			bRet = exists(query, values);
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//		return bRet;
//	}

//	public boolean existsContact(Contact contact) {
//		String query = "select count(id) from MessageInstance where contact = ? ";
//		return findCount(query, contact) > 0;
//	}

//	public static MessageInstanceDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (MessageInstanceDAO) ctx.getBean("MessageInstanceDAO");
//	}

	/**
	 * Find all MessageInstances sent to a particular contact via the specified method (e.g, email).
	 *
	 * @param contact The contact who was the recipient of the messages.
	 * @param method The NotificationMethod used to send the messageInstance.
	 * @return A (possibly empty) List of MessageInstances matching the parameters given. Null is
	 *         never returned.
	 * @throws RuntimeException
	 */
	@SuppressWarnings("unchecked")
	public List<MessageInstance> findByContactNotificationMethod(Contact contact, NotificationMethod method) {
		try {
			Object values[] = { contact, method };
			String query = "from MessageInstance m where m.contact = ? and m.sentMethod = ?";
			List<MessageInstance> results = find(query, values);
			return results;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

	/**
	 * Delete the given MessageInstance, and remove any other related links.
	 * The only association that needs to be removed is in Contact.getMessageInstances().
	 * @param mi
	 */
	@Transactional
	public void remove(MessageInstance mi) {
		mi = refresh(mi);
		Contact c = mi.getContact();
		boolean b = c.getMessageInstances().remove(mi);
		log.debug("removed=" + b);
		delete(mi);
	}

}