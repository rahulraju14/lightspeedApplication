/* EventLogSummary.java */
package com.lightspeedeps.batch;

import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.EventDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.message.Mailer;
import com.lightspeedeps.model.Event;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;

/**
 * This class looks at the Event log and summarizes recent activity.
 * It is designed to run as a Quartz-scheduled task.
 */
@ManagedBean	// so a test class can access it.
public class EventLogSummary extends SpringBatch {

	private static final Log log = LogFactory.getLog(EventLogSummary.class);

	/** Look at this many hours in Event log prior to the current time; if there were
	 * any Error events during that period, send a notification. */
	private int triggerHours = 1;

	/** Set in applicationContextGeneric.xml. */
	private transient EventDAO eventDAO;

	/** Set in applicationContextGeneric.xml. */
	private transient ProductionDAO productionDAO;

	/** A mail-handler for sending the failed logon count to an admin. Set in
	 * applicationContextGeneric.xml. */
	private Mailer mailer;

	/**
	 * The recipient's email address for the "login failed count" daily email. This is currently set
	 * in web.xml (and passed from there through applicationContextGeneric.xml to our bean.
	 */
	private String recipient;

	public EventLogSummary() {
		log.debug("");
	}

	/**
	 * Called via a scheduled job, such as a Quartz task.
	 * See applicationContextScheduler.xml.
	 * @return empty string
	 */
	public String execute() {
		log.info("");
		String ret = Constants.SUCCESS;
		setUp();	// required for SpringBatch subclasses - initializes context
		try {
			if (getEventDAO().checkDb()) {
				Date startDate, endDate;
				endDate = new Date();
				startDate = new Date(endDate.getTime() - (triggerHours * 60 * 60 * 1000)); // 'n' hours earlier
				long count1 = getEventDAO().findCountTypeInRange(EventType.APP_ERROR, startDate, endDate);

				if (count1 > 0) {
					startDate = new Date(endDate.getTime() - (24 * 60 * 60 * 1000)); // 24 hours earlier
					List<Event> events = getEventDAO().findTypeInRange(EventType.APP_ERROR, startDate, endDate);
					long countDay = events.size();

//					String description = count1 + " recent errors; " + countDay + " in last 24 hours";
//					EventUtils.logEvent(EventType.INFO, null, null, "lightspeed", description);

					if (recipient != null && recipient.length() > 0) {
						// web.xml and context.xml configured to request email notification...
						Production prod = getProductionDAO().findById(Constants.SYSTEM_PRODUCTION_ID);
						String summary = formatEvents(events);
						// Substitution variables in message text:
						// 0 = date & time; 1 = recent count; 2 = 24-hr count; 3 = ProdId; 4 = summary text
						String subject = MsgUtils.formatMessage("notification.event.count.subject",
								endDate, (Long)count1, (Long)countDay, prod.getProdId());
						String body = MsgUtils.formatMessage("notification.event.count.body",
								endDate, (Long)count1, (Long)countDay, prod.getProdId(), summary);
						if (! mailer.sendMail(recipient, subject, body)) {
							ret = Constants.FAILURE;
						}
					}
				}
			}
		}
		catch (Exception ex) {
			ret = Constants.FAILURE;
			EventUtils.logError(ex);
		}
		finally {
			tearDown();	// required for SpringBatch subclasses - clean up.
		}

		return ret;
	}

	/**
	 * @param events
	 * @return A formatted String of all the Event objects given.
	 */
	private String formatEvents(List<Event> events) {
		String str = "";
		for (Event evt : events) {
			str += evt.toString() + Constants.NEW_LINE;
		}
		return str;
	}

	/**See {@link #triggerHours}. */
	public int getTriggerHours() {
		return triggerHours;
	}
	/**See {@link #triggerHours}. */
	public void setTriggerHours(int triggerHours) {
		this.triggerHours = triggerHours;
	}

	/**See {@link #productionDAO}. */
	public ProductionDAO getProductionDAO() {
		if (productionDAO == null) {
			productionDAO = ProductionDAO.getInstance();
		}
		return productionDAO;
	}
	/**See {@link #productionDAO}. */
	public void setProductionDAO(ProductionDAO productionDAO) {
		this.productionDAO = productionDAO;
	}

	public EventDAO getEventDAO() {
		if (eventDAO == null) {
			eventDAO = EventDAO.getInstance();
		}
		return eventDAO;
	}
	public void setEventDAO(EventDAO dao) {
		eventDAO = dao;
	}

	/** See {@link #mailer}. */
	public Mailer getMailer() {
		return mailer;
	}
	/** See {@link #mailer}. */
	public void setMailer(Mailer mailer) {
		this.mailer = mailer;
	}

	/** See {@link #recipient}. */
	public String getRecipient() {
		return recipient;
	}
	/** See {@link #recipient}. */
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

}
