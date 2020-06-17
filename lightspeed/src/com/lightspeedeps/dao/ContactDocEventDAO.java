package com.lightspeedeps.dao;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.ContactDocEvent;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.type.TimedEventType;

public class ContactDocEventDAO extends SignedEventDAO<ContactDocEvent> {

	private static final Log log = LogFactory.getLog(ContactDocEventDAO.class);

	public static ContactDocEventDAO getInstance() {
		return (ContactDocEventDAO)getInstance("ContactDocEventDAO");
	}

	/**
	 * Create a new ContactDocEvent instance of the given type, save it in the
	 * database, and add it to the given ContactDocument's collection of events.
	 * Note that *sometimes* the event is added to the collection as a result of
	 * the 'save' operation, but not always! So the code checks the last event
	 * in the collection, and if it is not the one just saved, adds it.
	 *
	 * @param contactDocument The ContactDocument to which the event is related.
	 * @param type The TimedEventType of the event to be created.
	 * @return The newly-created TimedEventType instance.
	 */
	@Transactional
	public ContactDocEvent createEvent(ContactDocument contactDocument, TimedEventType type) {
		ContactDocEvent event = new ContactDocEvent();
		event.setContactDocument(contactDocument);
		event.setType(type);
		event.setDate(new Date());
		initEvent(event);
		Integer savedEventId = save(event);
		List<ContactDocEvent> events = contactDocument.getContactDocEvents();
		if (events.size() == 0 || ! events.get(events.size()-1).getId().equals(savedEventId)) {
			contactDocument.getContactDocEvents().add(event);
			log.debug(" --------------->  contactDocEvent ADDED  <----------------------");
		}
		else {
			log.debug("contactDocEvent NOT added");
		}
		return event;
	}

}
