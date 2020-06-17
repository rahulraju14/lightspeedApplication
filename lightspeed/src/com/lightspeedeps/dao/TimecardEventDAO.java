package com.lightspeedeps.dao;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.TimecardEvent;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.type.TimedEventType;
import com.lightspeedeps.type.TimecardSubmitType;

/**
 * A data access object (DAO) providing persistence and search support for
 * TimecardEvent entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.TimecardEvent
 */
public class TimecardEventDAO extends SignedEventDAO<TimecardEvent> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TimecardEventDAO.class);

	// property constants
	public static final String USER_ACCOUNT = "userAccount";
	public static final String LAST_NAME = "lastName";
	public static final String FIRST_NAME = "firstName";
	public static final String DAY_NUM = "dayNum";
	public static final String FIELD = "field";


	public static TimecardEventDAO getInstance() {
		return (TimecardEventDAO)getInstance("TimecardEventDAO");
	}

	/**
	 * Create and save a new TimecardEvent of the specified type, and associate
	 * it with the given timecard. On exit, the event has been stored in
	 * the database, and the WeeklyTimecard's list of TimecardEvent's has been
	 * updated with the new event.
	 *
	 * @param wtc The WeeklyTimecard that this event is related to.
	 * @param type The type of Event, such as Approver or Reject.
	 * @param field The value for the event's 'field' field, which indicates the
	 *            source of the 'newValue' field.
	 * @param newValue The value to be stored in the event's newValue field.
	 */
	public void createEvent(WeeklyTimecard wtc, TimedEventType type, short field, Integer newValue) {
		TimecardEvent ev = buildEvent(wtc);
		ev.setField(field);
		if (newValue != null) {
			ev.setNewId(newValue);
		}
		addEvent(ev, wtc, type);
	}

	/**
	 * Create and save a new TimecardEvent of the specified type, and associate
	 * it with the given timecard. On exit, the event has been stored in the
	 * database, and the WeeklyTimecard's list of TimecardEvent's has been
	 * updated with the new event.
	 *
	 * @param wtc The WeeklyTimecard that this event is related to.
	 * @param type The type of Event, such as Approver or Reject.
	 * @param dayNumber The value to be stored in the event's dayNum field.
	 * @param field The value for the event's 'field' field, which indicates the
	 *            source of the 'oldValue' and 'newValue' fields.
	 * @param oldValue The value to be stored in the event's oldValue field.
	 * @param newValue The value to be stored in the event's newValue field.
	 */
	public void createEvent(WeeklyTimecard wtc, TimedEventType type, byte dayNumber,
			short field, Integer oldValue, Integer newValue) {
		TimecardEvent ev = buildEvent(wtc);
		ev.setDayNum(dayNumber);
		ev.setField(field);
		if (oldValue != null) {
			ev.setOldId(oldValue);
		}
		if (newValue != null) {
			ev.setNewId(newValue);
		}
		addEvent(ev, wtc, type);
	}

	/**
	 * Create and save a new "SUBMIT" TimecardEvent with the specified
	 * submitType, and associate it with the given WeeklyTimecard. On exit, the
	 * event has been stored in the database, and the WeeklyTimecard's list of
	 * TimecardEvent's has been updated with the new event.
	 *
	 * @param wtc The WeeklyTimecard that this event is related to.
	 * @param submitType The TimecardSubmitType, which identifies a particular
	 *            kind of SUBMIT Event.
	 * @param newValue The User.id value to be stored in the event's newValue field.
	 */
	public void createEvent(WeeklyTimecard wtc, TimecardSubmitType submitType, Integer newValue) {
		TimecardEvent ev = buildEvent(wtc);
		ev.setSubmitType(submitType);
		if (newValue != null) {
			ev.setField(TimecardEvent.FIELD_USER);
			ev.setNewId(newValue);
		}
		addEvent(ev, wtc, TimedEventType.SUBMIT);
	}

	/**
	 * Build a basic TimecardEvent, except for setting the type of event.
	 *
	 * @param wtc The WeeklyTimecard the event will be attached to.
	 * @return A new TimecardEvent, with user information filled in and a UUID
	 *         assigned.
	 */
	private TimecardEvent buildEvent(WeeklyTimecard wtc) {
		TimecardEvent ev = new TimecardEvent(wtc, new Date());
		initEvent(ev);
		return ev;
	}

	/**
	 * Add the event to the database, and add it to the collection of events for
	 * the given timecard.
	 *
	 * @param ev The event to be added.
	 * @param wtc The WeeklyTimecard associated with this event.
	 * @param type The TimecardEventType that applies to this event.
	 */
	private void addEvent(TimecardEvent ev, WeeklyTimecard wtc, TimedEventType type) {
		ev.setType(type);
		save(ev);
		wtc.getTimecardEvents().add(ev);
		attachDirty(wtc);
	}

//	public static TimecardEventDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (TimecardEventDAO)ctx.getBean("TimecardEventDAO");
//	}

}