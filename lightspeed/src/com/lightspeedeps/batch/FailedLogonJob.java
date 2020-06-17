package com.lightspeedeps.batch;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.bean.ManagedBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.message.Mailer;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;

/**
 * This class captures the number of failed logon attempts from the prior day and
 * logs it.  It is designed to run as a Quartz-scheduled task.
 */
@ManagedBean	// so test class can access it.
public class FailedLogonJob extends SpringBatch {

	private static final Log log = LogFactory.getLog(FailedLogonJob.class);

	private final Format messageDateFormat = new SimpleDateFormat("h:mm a, EEEE, MMM d, yyyy");

	private long count = 0;

	/** Set in applicationContextGeneric.xml. */
	private transient UserDAO userDAO;

	/** A mail-handler for sending the failed logon count to an admin. Set in
	 * applicationContextGeneric.xml. */
	private Mailer mailer;

	/**
	 * The recipient's email address for the "login failed count" daily email. This is currently set
	 * in web.xml (and passed from there through applicationContextGeneric.xml to our bean.
	 */
	private String recipient;

	public FailedLogonJob() {
		log.debug("");
	}

	/**
	 * Called via a scheduled job, such as a Quartz task.
	 * See applicationContextScheduler.xml.
	 * @return empty string
	 */
	public String execute() {
		log.debug("");
		String ret = Constants.SUCCESS;
		setUp();	// required for SpringBatch subclasses - initializes context
		try {
			if (getUserDAO().checkDb()) {
				count = getUserDAO().findLockedOutCount();
				String dateStr = messageDateFormat.format(new Date());
				String description = "Number of locked-out users at " + dateStr + " is " + count;

				EventUtils.logEvent(EventType.INFO, null, null, "lightspeed", description);

				if (recipient != null && recipient.length() > 0) {
					// web.xml and context.xml configured to request email notification...
					String subject = MsgUtils.formatMessage("notification.login.failedCount.subject", new Date(), (Long)count);
					String body = MsgUtils.formatMessage("notification.login.failedCount.body", new Date(), (Long)count);
					if (! mailer.sendMail(recipient, subject, body)) {
						ret = Constants.FAILURE;
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

	public UserDAO getUserDAO() {
		if (userDAO == null) {
			userDAO = UserDAO.getInstance();
		}
		return userDAO;
	}
	public void setUserDAO(UserDAO userDao) {
		userDAO = userDao;
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
