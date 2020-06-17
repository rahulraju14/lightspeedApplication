package com.lightspeedeps.util.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.AuditEventDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.AuditEvent;
import com.lightspeedeps.model.PersistentObject;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.AuditEventType;
import com.lightspeedeps.type.ObjectType;

/**
 * A utility class for methods related to the Event database table
 * and Event objects.
 */
public class AuditUtils {
	private static final Log log = LogFactory.getLog(AuditUtils.class);

	private AuditUtils() {
	}

	/**
	 * Retrieve, as formatted text, the contents of the AuditEvent table
	 * related to a given object.
	 *
	 * @param type The ObjectType being tracked.
	 * @param id An integer used to specify a particular object, typically the
	 *            database 'id' value.
	 * @param showDetail True iff the detail portion of the AuditEvent records
	 *            should be included.
	 * @return The text to be displayed.
	 */
	public static String getTrail(ObjectType type, Integer id, boolean showDetail) {
		String trail = "";
		String userName = "";
		List<AuditEvent> events = AuditEventDAO.getInstance().findByObject(type, id);
		for (AuditEvent event : events) {
			int depth;
			if ((depth = event.getDepth()) > 0) {
				while (depth-- > 0) {
					trail += "> ";
				}
			}
			else {
				User user = UserDAO.getInstance().findOneByProperty(UserDAO.ACCOUNT_NUMBER, event.getUserAccount());
				if (user != null) {
					userName = user.getAnyName() + ": ";
				}
				SimpleDateFormat sdf = new SimpleDateFormat("M/d HH:mm");
				trail = trail + sdf.format(event.getDate()) + ": ";
			}
			trail += userName + event.getType().getLabel() + Constants.NEW_LINE;
			if (event.getSummary() != null) {
				trail += event.getSummary() + Constants.NEW_LINE;
			}
			if (showDetail && event.getDetail() != null) {
				trail += event.getDetail() + Constants.NEW_LINE;
			}
			trail += Constants.NEW_LINE;
			userName = ""; // only show it once
		}
		return trail;
	}

	/**
	 * Create a new AuditEvent related to a particular object. The returned
	 * instance has NOT been saved to the database.
	 *
	 * @param type The event type being logged.
	 * @param parent The parent of this event; may be null.
	 * @param object The object to which this audit event is related.
	 * @return A new (transient) AuditEvent object documenting the event
	 *         specified, tied to the given object, and a child of the specified
	 *         parent (if any). The event will be associated with the current
	 *         Production and currently logged-in User. The summary and detail
	 *         fields will be null.
	 */
	@SuppressWarnings("rawtypes")
	public static AuditEvent startEvent(AuditEventType type, AuditEvent parent, PersistentObject object) {
		return log(type, parent, object, null, null);
	}

	/**
	 * Truncate the summary or detail fields if they are longer than allowed in the database.
	 * @param event The AuditEvent of interest.
	 */
	public static void updateEvent(AuditEvent event) {
		if (event.getSummary() != null && event.getSummary().length() > AuditEvent.MAX_SUMMARY_LENGTH) {
			log.warn("AuditEvent Summary too long, truncated. Data: " + event.getSummary());
			event.setSummary(event.getSummary().substring(0, AuditEvent.MAX_SUMMARY_LENGTH));
		}
		if (event.getDetail() != null && event.getDetail().length() > AuditEvent.MAX_DETAIL_LENGTH) {
			log.warn("AuditEvent Detail too long, truncated. Data: " + event.getDetail());
			event.setDetail(event.getDetail().substring(0, AuditEvent.MAX_DETAIL_LENGTH));
		}
//		AuditEventDAO.getInstance().attachDirty(event);
	}

	/**
	 * Create a new AuditEvent instance, but do not persist it in the database.
	 *
	 * @param type The event type being logged.
	 * @param summary The summary text for the event.
	 * @param detail The detailed text for the event.
	 * @return A new (transient) AuditEvent object documenting the event
	 *         specified. The 'parent' and relevant object will be null. The
	 *         event will be associated with the current Production and
	 *         currently logged-in User.
	 */
	public static AuditEvent log(AuditEventType type, String summary, String detail) {
		return log(type, null, null, summary, detail);
	}

	/**
	 * Create a new AuditEvent instance, but do not persist it in the database.
	 *
	 * @param type The event type being logged.
	 * @param parent The parent of this event; may be null.
	 * @param object The object to which this audit event is related.
	 * @param summary The summary text for the event.
	 * @param detail The detailed text for the event.
	 * @return A new (transient) AuditEvent object documenting the event specified, tied to
	 *         the given object, and a child of the specified parent (if any). The event
	 *         will be associated with the current Production and currently logged-in User.
	 */
	@SuppressWarnings("rawtypes")
	public static AuditEvent log(AuditEventType type, AuditEvent parent,
			PersistentObject object, String summary, String detail) {
		String username = null;
		User user;
		if ((user=SessionUtils.getCurrentUser()) != null) {
			username = user.getAccountNumber();
		}

		ObjectType objType = null;
		Integer objId = null;
		if (object != null) {
			objType = object.getObjectType();
			objId = object.getId();
		}

		return log(type, SessionUtils.getProduction(), parent,
				username, objType, objId, summary, detail);
	}

	/**
	 * Create a new AuditEvent instance, but do not persist it in the database.
	 *
	 * @param type The event type being logged.
	 * @param production The Production this event is related to.
	 * @param parent The parent of this event; may be null.
	 * @param userAccount The user.accountNumber of the person causing this
	 *            event.
	 * @param objType The type of object, using enum ObjectType. This indirectly
	 *            identifies the class of the object related to the event.
	 * @param objId The identifier of the object related to the event, typically
	 *            the unique database id.
	 * @param summary The summary text for the event.
	 * @param detail The detailed text for the event.
	 * @return A new (transient) AuditEvent object documenting the event
	 *         specified, tied to the given object, and a child of the specified
	 *         parent (if any).
	 */
	public static AuditEvent log(AuditEventType type, Production production,
			AuditEvent parent, String userAccount, ObjectType objType, Integer objId,
			String summary, String detail) {
		AuditEvent event = null;
		try {
			log.debug("event=" + type + ", user=" + userAccount + ", objId=" + objId);

			if (summary != null && summary.length() > AuditEvent.MAX_SUMMARY_LENGTH) {
				summary = summary.substring(0, AuditEvent.MAX_SUMMARY_LENGTH);
			}

			if (detail != null && detail.length() > AuditEvent.MAX_DETAIL_LENGTH) {
				detail = detail.substring(0, AuditEvent.MAX_DETAIL_LENGTH);
			}

			short depth = 0;
			if (parent != null) {
				depth = (short)(parent.getDepth() + 1);
			}

			Date date;
			if (parent == null) {
				date = new Date();
			}
			else {
				date = parent.getDate();
			}

			event = new AuditEvent(date, type, production,
					parent, depth, userAccount, objType, objId, summary, detail);
//			 AuditEventDAO.getInstance().save(event);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return event;
	}

}
