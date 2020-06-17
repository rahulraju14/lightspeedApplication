//	File Name:	MessageHandler.java
package com.lightspeedeps.message;

import java.util.*;
import java.text.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.WaterMark;
import com.lightspeedeps.type.AccessStatus;
import com.lightspeedeps.type.NotificationMethod;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.validator.EmailValidator;

/**
 * A class for preparing email messages to one or more Contacts. This class is
 * mostly concerned with creating the Message and MessageInstance entries in the
 * database. It then schedules an instance of ExecuteMesage to actually email
 * the messages.
 */
@Component("MessageHandler")
@Scope("request")
public class MessageHandler {
	private static final Log log = LogFactory.getLog(MessageHandler.class);

	/** DAO for Message objects; set via XML for JUnit tests. */
	@Autowired
	private transient MessageDAO messageDAO;

	/** A Spring component used to schedule asynchronous tasks, in this case, to send the
	 * email and SMS messages. */
	@Autowired
	private transient TaskExecutor taskExecutor;

	/** The current Production title (for messages) */
	String productionName;

	/** The current project. */
	private Project project;

	public MessageHandler() {
		log.debug("");
	}

	public static MessageHandler getInstance() {
		return (MessageHandler)ServiceFinder.findBean("MessageHandler");
	}

	/**
	 * Email a file to a List of Contact's. A Message object will be added to the
	 * database, plus a MessageInstance for each Contact with a valid email address.
	 * The email subject and body text will be generated using the resource-file messages with prefix "MessageHandler.report".
	 *
	 * @param contacts The (possibly empty) List of Contact to receive the file.
	 *            Each Contact's email address will be validated (for syntax)
	 *            before sending the file to him.
	 * @param reportName The name of the report; this is the 0-th substitution
	 *            parameter passed when formatting the email subject and message
	 *            body text.
	 * @param date The timestamp that will be formatted as a date and passed as
	 *            the 1st substitution parameter when formatting the email
	 *            subject and message body text.
	 * @param time The timestamp that will be formatted as date and time and
	 *            passed as the 2nd substitution parameter when formatting the
	 *            email subject and message body text.
	 * @param file The file to be sent to each recipient.
	 */
	public void sendReport(Collection<Contact> contacts, String reportName, Date date, Date time, String file) {
		sendReport(contacts, "MessageHandler.report", reportName, date, time, "", file, null);
	}

	/**
	 * Email a file to a List of Contact's. A Message object will be added to
	 * the database, plus a MessageInstance for each Contact with a valid email
	 * address. Optionally, a watermark may be applied to each copy of the file
	 * sent. This is only supported for PDF files.
	 *
	 * @param contacts The (possibly empty) List of Contact to receive the file.
	 *            Each Contact's email address will be validated (for syntax)
	 *            before sending the file to him.
	 * @param msgPrefix The prefix of the messageProperties resource to be used
	 *            to create the resource names for the subject and body text.
	 * @param reportName The name of the report; this is the 0-th substitution
	 *            parameter passed when formatting the email subject and message
	 *            body text.
	 * @param date The timestamp that will be formatted as a date and passed as
	 *            the 1st substitution parameter when formatting the email
	 *            subject and message body text.
	 * @param time The timestamp that will be formatted as date and time and
	 *            passed as the 2nd substitution parameter when formatting the
	 *            email subject and message body text.
	 * @param sender The String that will be passed as the 3rd substitution
	 *            parameter for the email subject and message; intended to be
	 *            the name of the sending party.
	 * @param file The fully-qualified name (path) of the file to be attached to
	 *            each email.
	 * @param mark True if the files sent should have individual watermarks
	 *            applied.
	 */
	public void sendReport(Collection<Contact> contacts, String msgPrefix,
			String reportName, Date date, Date time, String sender,
			String file, WaterMark mark) {
		log.debug("report=" + reportName + ", file=" + file);

		final Format shortDateFormat = new SimpleDateFormat("MMM d");
		final Format timeFormat = new SimpleDateFormat("M/d/yy, h:mm a");
		final Format bodyDateFormat = new SimpleDateFormat(Constants.LONG_DATE_FORMAT);

		if ( ! (getProject().getProduction().getStatus() == AccessStatus.ACTIVE)) {
			return;
		}
		if (contacts != null && contacts.size() > 0) {
			String subject = formatMessage( msgPrefix + ".Subject",
					reportName, shortDateFormat.format(date), timeFormat.format(time), sender);
			String body = formatMessage( msgPrefix + ".Msg",
					reportName, bodyDateFormat.format(date), timeFormat.format(time), sender);
			body = DoNotification.finalizeBody(body, getProject());
			sendFileToAll(contacts, subject, body, file, null, mark);
		}
	}

	/**
	 * Email a file to a List of Contact's. This takes the subject & body
	 * strings, and creates one Message object plus a MessageInstance object for
	 * each Contact in the given collection that has a valid email address.
	 * Optionally, a watermark may be applied to each copy of the file sent.
	 * This is only supported for PDF files.
	 *
	 * @param contacts A collection of Contact's who are to receive the
	 *            messages.
	 * @param subject The subject line for each email.
	 * @param bodyEmail The message body to be sent via email.
	 * @param fullFileName The fully-qualified name (including path) of the file
	 *            to be attached to each email.
	 * @param displayFileName The name to use for the attached file; if null, a
	 *            filename is extracted from the fullFileName parameter.
	 * @param mark True if the files sent should have individual watermarks
	 *            applied.
	 */
	public void sendFileToAll(Collection<Contact> contacts, String subject,
			String bodyEmail, String fullFileName, String displayFileName, WaterMark mark) {

		if (contacts.size() == 0) {
			log.warn("** No contacts provided, subject=" + subject);
			return;
		}

		// Message for delivery via e-mail
		Message msgEmail = new Message(NotificationMethod.EMAIL, getProdMailSender(), subject, bodyEmail, null);
		if (displayFileName != null) {
			fullFileName += Message.NAME_SEPARATOR + displayFileName;
		}
		msgEmail.setFileName(fullFileName);

		MessageInstance mi = null;
		ContactDAO contactDAO = ContactDAO.getInstance();
		for (Contact contact : contacts) {
			contact = contactDAO.refresh(contact);
			String email = contact.getEmailAddress();
			if (email != null && EmailValidator.isValidEmail(email)) {
				mi = new MessageInstance(msgEmail, contact, new Date(), NotificationMethod.EMAIL,
						Constants.FALSE, null);
				msgEmail.getMessageInstances().add(mi);
			}
		}
		// Save the Message objects; the MessageInstance objects are saved automatically via cascade
		getMessageDAO().save(msgEmail);
		Mailer mailer = Mailer.getInstance();
		getTaskExecutor().execute(new ExecuteMessage(msgEmail, mailer, mark));
	}

	/**
	 * Format a message after adding the current Production name and Project
	 * title as the first two arguments (preceding the supplied arguments).
	 *
	 * @param msgid The message id (String) identifying the message within our
	 *            message resource property file.
	 * @param args The (optional) list of arguments to pass to the format
	 *            routine.
	 * @return The formatted message.
	 */
	private String formatMessage(String msgid, Object... args) {
		return DoNotification.formatMessage(msgid, getProductionName(), project, Constants.LOCALE_US, null, args);
	}

	/**
	 * @return The string to use as the "sender" for generated email.
	 */
	private String getProdMailSender() {
		return DoNotification.getProdMailSender(getProject());
	}

	/** See {@link #productionName}. */
	public String getProductionName() {
		if (productionName == null) {
			productionName = SessionUtils.getProduction().getTitle();
		}
		return productionName;
	}
	/**See {@link #productionName}. */
	public void setProductionName(String productionName) {
		this.productionName = productionName;
	}

	/** See {@link #project}. */
	public Project getProject() {
		if (project == null) {
			project = SessionUtils.getCurrentProject();
		}
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

	/** See {@link #messageDAO}. */
	public MessageDAO getMessageDAO() {
		if (messageDAO == null) {
			messageDAO = MessageDAO.getInstance();
		}
		return messageDAO;
	}
	/** See {@link #messageDAO}. */
	public void setMessageDAO(MessageDAO messageDAO) {
		this.messageDAO = messageDAO;
	}

	/** See {@link #taskExecutor}. */
	public TaskExecutor getTaskExecutor() {
		if (taskExecutor == null) {
			taskExecutor = (TaskExecutor)ServiceFinder.findBean("taskExecutor");
		}
		return taskExecutor;
	}
	/** See {@link #taskExecutor}. */
	public void setTaskExecutor(TaskExecutor executor) {
		taskExecutor = executor;
	}

}
