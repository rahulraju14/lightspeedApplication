/**
 * File: ExecuteMessage.java
 */
package com.lightspeedeps.message;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import com.lightspeedeps.model.Message;
import com.lightspeedeps.model.MessageInstance;
import com.lightspeedeps.model.User;
import com.lightspeedeps.object.WaterMark;
import com.lightspeedeps.type.NotificationMethod;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.file.PdfUtils;
import com.lightspeedeps.util.report.ReportUtils;
import com.lightspeedeps.web.validator.EmailValidator;
import com.lightspeedeps.web.validator.PhoneNumberValidator;

/**
 * Execute the message instances contained in a Message object, including
 * e-mail and SMS execution.
 */
/* package-private */ class ExecuteMessage implements Runnable {
	private static final Log log = LogFactory.getLog(ExecuteMessage.class);

	private Message message;
	private Collection<Message> messages;
	private Mailer mailer;
	private WaterMark mark;
	private User user;

	public ExecuteMessage() {
	}

	/**
	 * Construct an instance to process a Message object via the supplied
	 * Mailer, with an optional watermark applied to any attached file(s).
	 *
	 * @param msg The Message to process.
	 * @param mail The mail handler.
	 * @param pMark An optional WaterMark to apply to each of the attached
	 *            files; if null, no watermark is applied.
	 */
	public ExecuteMessage(Message msg, Mailer mail, WaterMark pMark) {
		message= msg;
		mailer = mail;
		mark = pMark;
	}

	/**
	 * Construct an instance to process a set of Message objects via the
	 * supplied Mailer, with an optional watermark applied to any attached
	 * file(s).
	 *
	 * @param msgs The Collection of Message`s to process.
	 * @param mail The mail handler.
	 * @param pMark An optional WaterMark to apply to each of the attached files
	 *            (if any); if null, no watermark is applied.
	 */
	public ExecuteMessage(Collection<Message> msgs, Mailer mail, WaterMark pMark) {
		messages = msgs;
		mailer = mail;
		mark = pMark;
	}

	/**
	 * Construct an instance to process a set of Message objects via the
	 * supplied Mailer.
	 *
	 * @param msgs The Collection of Message`s to process.
	 * @param mail The mail handler.
	 */
	public ExecuteMessage(Collection<Message> msgs, Mailer mail) {
		messages = msgs;
		mailer = mail;
	}

	/**
	 * Take the message or messages which we were instantiated with, and
	 * process them by either sending them via e-mail or via SMS (text messaging).
	 * For LS purposes, this is usually called from a TaskExecutor instance.
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			if (message != null) {
				execute(message);
			}
			if (messages != null) {
				for (Message msg : messages) {
					execute(msg);
				}
			}
		}
		catch (Exception e) {
			log.error("Run failed - Exception: ",e);
		}
	}

	/**
	 * Execute (process) a single Message object, which may include multiple
	 * MessageInstance objects. Each messageInstance is handled, according to
	 * its 'sentMethod' property, by e-mailing or text-messaging it.
	 * <p>
	 * Instance fields used that should be set upon entry:<br>
	 * Field 'mailer': The e-mail support bean used for sending e-mails. <br>
	 * Field 'mark': The watermark to apply to any attached file(s); if null, no
	 * watermark is applied.
	 *
	 * @param message The message whose MessageInstances will be processed.
	 */
	private void execute(Message message) {
		log.debug("Executing msg: " + message.getSubject());
		for (MessageInstance mi : message.getMessageInstances()) {
			//log.debug(" instance for user " + mi.getUser());
			try {
				user =  mi.getUser();
				if (mi.getSentMethod() == NotificationMethod.EMAIL) {
					boolean res = false;
					int retryCount = 1;
					log.debug("maxCount = " + Constants.MAIL_RETRY_COUNT);
					while ((! res) && Constants.MAIL_RETRY_COUNT >= retryCount) {
						log.debug("Retry Count = " + retryCount);
						res = sendEmail(message, mi);
						++retryCount;
						log.debug("res = " + res);
					}
				}
				else if (mi.getSentMethod() == NotificationMethod.TEXT_MESSAGE && user != null) {
					sendText(message, mi);
				}
				else {
					log.debug("instance ignored: not email or textmsg; type=" + mi.getSentMethod());
				}
			}
			catch (Exception e) {
				log.error("Exception: ", e);
			}
		}
	}

	/**
	 * Send the email specified by the given MessageInstance. If the instance
	 * includes a watermark specification, then, for each file associated with
	 * the message, a copy of the file is made and the watermark applied, and
	 * the watermarked file(s) are sent with the message instead of the original
	 * file(s).
	 *
	 * @param message The Message object containing the subject, body, and
	 *            (optional) list of files to be attached.
	 * @param mi The MessageInstance that describes the recipient.
	 */
	private boolean sendEmail(Message message, MessageInstance mi) {
		String email, emailName = "", emailString;
		boolean ret = false;
		if (mi.getContact() == null) {
			if (user != null) {
				email = user.getEmailAddress();
				emailName = user.getFirstNameLastName();
			}
			else {
				email = mi.getEmailAddress();
			}
		}
		else {
			email = mi.getContact().getEmailAddress();
			emailName = mi.getContact().getDisplayName();
		}
		emailString = email;
		if (! StringUtils.isEmpty(emailName) && (! emailName.contains("\""))) {
			emailString = '"' + emailName + "\" <" + email + ">";
		}
		log.debug("email=" + emailString);
		if (email != null && EmailValidator.isValidEmail(email)) {
			if (message.getFileName() != null && message.getFileName().trim().length() > 0) {
				Set<String> fileNames = StringUtils.commaDelimitedListToSet(message.getFileName());
				if (mark != null) { // need to watermark each file with this Contact's name
					ret = sendWithWatermark(message, emailString, fileNames);
				}
				else {
					ret = mailer.sendFile(emailString, message.getSubject(), message.getBody(), fileNames, message.getSender());
				}
			}
			else {
				ret = mailer.sendMail(emailString, message.getSubject(), message.getBody(), message.getSender());
			}
		}
		return ret;
	}

	/**
	 * Create watermarked copies of any attachments and send the email with those copies.
	 * @param message The Message being sent.
	 * @param emailString The email address, optionally "enhanced".
	 * @param fileNames The collection of fully-qualified filenames of the attachments.
	 */
	private boolean sendWithWatermark(Message message, String emailString, Set<String> fileNames) {
		boolean ret = false;
		mark = ReportUtils.updateWaterMark(mark, user);
		Set<String> markedFiles = new HashSet<String>();

		// Create a water-marked copy of each attachment
		for (String fileName : fileNames ) {
			String[] names = StringUtils.delimitedListToStringArray(fileName, Message.NAME_SEPARATOR);
			fileName = names[0];
			int ix = fileName.lastIndexOf('.');
			String newFileName = fileName.substring(0, ix) + "_" + user.getId() + "W.pdf";
			PdfUtils.addWatermark(fileName, newFileName, mark, false);
			if (names.length > 1 &&
					names[1] != null) {
				newFileName += Message.NAME_SEPARATOR + names[1];
			}
			markedFiles.add(newFileName);
		}
		// Generate the email using the water-marked files
		ret = mailer.sendFile(emailString, message.getSubject(), message.getBody(), markedFiles, message.getSender());

		// Delete the temporary (water-marked) files
		for (String fileName : markedFiles ) {
			File f = new File(fileName);
			if (f.exists()) {
				if ( ! f.delete()) {
					log.error("Report file delete failed for '" + f.getAbsolutePath() + "'");
					EventUtils.logError("Report file delete failed for '" + f.getAbsolutePath() + "'");
				}
				else {
					log.debug("Report file deleted OK: '" + f.getAbsolutePath() + "'");
				}
			}
		}
		return ret;
	}

	/**
	 * Send a Text message (SMS), consisting of the text in the given Message
	 * instance, to the recipient identified by the MessageInstance.
	 *
	 * @param message The message to be sent.
	 * @param mi The recipient of the message.
	 */
	private void sendText(Message message, MessageInstance mi) {
		String phone;
		if (mi.getContact() == null || mi.getContact().getCellPhone() == null) {
			phone = user.getCellPhone();
		}
		else {
			phone = mi.getContact().getCellPhone();
		}
		log.debug("phone=" + phone);
		if (phone != null && PhoneNumberValidator.isValid(phone)) {
			int rc = SmsUtils.sendMsg(message.getBody(), phone);
			if (rc != 0) {
				log.error("SMS send failed, rc=" + rc + ", phone=" + phone +
						", \nmsg=`" + message.getBody() + "`");
			}
		}
	}

}
