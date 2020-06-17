package com.lightspeedeps.message;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;

import com.lightspeedeps.model.Message;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * This class provides the low-level E-mail support for all of the lightSpeed application.
 * The SMTP information it needs is specified in web.xml, so that it can be customized
 * for each instance of the application.
 * <p>
 * The "from" (sender's email) value may be set by calling setFrom().  If it is not set prior
 * to calling sendFile() or sendMail(), the default value (also from web.xml) will be used.
 */
public class Mailer extends ApplicationObjectSupport {
	private static final Log log = LogFactory.getLog(Mailer.class);

	private String from = null;

	/**
	 * This parameter is set via the Mailer bean definition in faces-config.xml. It is the Spring
	 * framework implementation. Note that the host, username, and password used by the mailSender
	 * is set in the managed-bean declaration (also in faces-config.xml), and the values reference
	 * initial parameters from web.xml.
	 */
	private JavaMailSenderImpl mailSender;

	/** Default constructor */
	public Mailer() {
	}

	/**
	 * Get an instance of this class. NOTE: this will fail if the current task does not have a
	 * "Faces" context. For example, Quartz-scheduled tasks cannot use this. They should have their
	 * Mailer property set via <property> tags in the application context xml file.
	 *
	 * @return A Mailer instance.
	 */
	public static Mailer getInstance() {
		return (Mailer)ServiceFinder.findBean("appMailer");
	}

	/**
	 *
	 * Send one or more files, via email, to a single recipient, using the
	 * specified subject line and body text, and the "sender" parameter as the
	 * 'from' address.
	 *
	 * @param recipient The email address to which the mail will be sent.
	 * @param subject The text for the subject line.
	 * @param body The body of the email message. It may contain newline
	 *            characters for simple formatting.
	 * @param fileNames A Collection of fully-qualified file names, each of
	 *            which will be used to create an attachment to the message. May
	 *            be null or an empty Collection if no attachments exist. The
	 *            "attachment name" -- the name the recipient will see, and
	 *            which is typically used by a mail program as the default
	 *            file-system name if the attachment is copied to the local
	 *            computer -- may be included following the fully-qualified
	 *            filename, separated by a '?'. E.g.,
	 *            <pre>
	 * &quot;/usr/report/script123_456.pdf?Lost Horizons.pdf;/usr/report/aSecondFileToAttach.txt&quot;
	 * </pre>
	 *
	 *            If the filename does not include a '?', then the portion of
	 *            the fully-qualified filename following the last path separator
	 *            will be used as the "attachment name".
	 * @param sender The email address to use as the 'from' address.
	 * @return True if the send was successful, false otherwise.
	 */
	public boolean sendFile(String recipient, String subject, String body, Collection<String> fileNames, String sender) {
		boolean bRet = false;
		if (recipient == null || recipient.length() < 5) {
			log.warn("invalid recipient; message ignored.");
			return false;
		}
		MimeMessage message = null;
		String info = "recipient: " + recipient + ", subject: " + subject;

		try {
			if (ServiceFinder.getBatchApplicationContext() == null) {
				// Pass the application context to ServiceFinder for use in "findBean". This is only
				// used if we encounter an exception and use EventUtils.
				ServiceFinder.setBatchApplicationContext(getApplicationContext());
			}
			message = mailSender.createMimeMessage();

			// use the true flag to indicate you need a multipart message
			MimeMessageHelper helper = new MimeMessageHelper(message, true);

			helper.setTo(recipient);
			if (sender.indexOf("do_not_reply") < 0) {
				sender = Constants.EMAIL_FROM_PREFIX + sender;
			}
			helper.setFrom(sender);
			helper.setSubject(subject);
			boolean html = (body.indexOf('<') >= 0);
			helper.setText(body, html);

			if (fileNames != null) {
				try {
					info += ", " + fileNames.size() + " files attached.";
					int ix;
					for (String fileName : fileNames) {
						log.info("file=" + fileName);
						String[] names = StringUtils.delimitedListToStringArray(fileName, Message.NAME_SEPARATOR);
						fileName = names[0];
//						fileName = fileName.replace("/usr/local/jakarta/tomcat/webapps/ls", "D:\\Dev\\MyEclipseWorkspace\\.metadata\\.me_tcat\\webapps\\lightspeed29");
//						log.debug("file=" + fileName);
						File file = new File(fileName);
						String displayName = fileName;
						if (names.length > 1) {
							displayName = names[1];
						}
						else if ((ix = fileName.lastIndexOf(File.separatorChar)) > 0) {
							displayName = fileName.substring(ix + 1);
						}
						FileSystemResource res = new FileSystemResource( file );
						helper.addAttachment(displayName, res);
					}
				}
				catch (Exception e) {
					EventUtils.logError("Error attaching files to email (" + info + "):", e);
					// log error, but let message go through.
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("Exception while creating message instance (" + info + "):", e);
			message = null; // no point
		}
		try {
			if (message != null) {
				log.debug("host=" + mailSender.getHost() +
						", port=" + mailSender.getPort() +
						", user=" + mailSender.getUsername() /*+ ", pw=" + mailSender.getPassword()*/);
				if (mailSender.getPort() == 587) { // authorization required (gmail)
					Properties props = mailSender.getJavaMailProperties();
					props.put("mail.smtp.starttls.enable","true");
					props.put("mail.smtp.auth", "true");
				}
				mailSender.send(message);
				log.info("Email sent Ok; " + info);
				bRet = true;
			}
		}
 		catch (MailException e) {
			EventUtils.logError("Error sending email; " + info, e);
		}
 		return bRet;
	}

	/**
	 * Send an email to a single recipient with the specified subject line and
	 * body text.
	 * <p>
	 * The "from" (sender's email) value may be set by calling setFrom(). If it
	 * is not set prior to to calling sendFile() or sendMail(), the default
	 * value (also from web.xml) will be used.
	 *
	 * @param recipient The email address to which the mail will be sent.
	 * @param subject The text for the subject line.
	 * @param body The body of the email message. It may contain newline
	 *            characters for simple formatting.
	 * @return True if the send was successful, false otherwise.
	 */
	public boolean sendMail(String recipient, String subject, String body) {
		return sendFile(recipient, subject, body, null, getFrom());
	}

	/**
	 * Send an email to a single recipient with the specified subject line and
	 * body text, and the specified 'sender' (from) address.
	 * <p>
	 * The "from" (sender's email) value may be set by calling setFrom(). If it
	 * is not set prior to to calling sendFile() or sendMail(), the default
	 * value (also from web.xml) will be used.
	 *
	 * @param recipient The email address to which the mail will be sent.
	 * @param subject The text for the subject line.
	 * @param body The body of the email message. It may contain newline
	 *            characters for simple formatting.
	 * @param sender The email address to use as the 'from' address.
	 * @return True if the send was successful, false otherwise.
	 */
	public boolean sendMail(String recipient, String subject, String body, String sender) {
		return sendFile(recipient, subject, body, null, sender);
	}

	/** Set the MailSender object used for the actual sending process.  This is
	 * normally set via a managed-property entry in faces-config.xml.
	 */
	public void setMailSender(JavaMailSenderImpl mailSender) {
		this.mailSender = mailSender;
	}

	public String getFrom() {
		if (from == null) {
			from = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_SEND_MAIL_FROM);
		}
		return from;
	}
	public void setFrom(String s) {
		from = s;
	}

}
