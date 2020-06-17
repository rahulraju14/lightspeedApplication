//	File Name:	MsgUtils.java
package com.lightspeedeps.util.app;

import java.text.MessageFormat;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * Methods to assist in generating messages using text stored in the
 * application's message ResourceBundle.
 *
 * NOTE: This code is currently optimized by assuming that the entire
 * application uses a single locale.  If this is NOT the case, the currentLocale
 * and messageBundle statics should be removed, and their "get" methods
 * should dynamically fetch the current value on each call.
 */
public class MsgUtils {
	private static final Log log = LogFactory.getLog(MsgUtils.class);

	private final static ResourceBundle messageBundle = // shared across entire application
			ResourceBundle.getBundle("com.lightspeedeps.util.app.messageResources");
	private final static ResourceBundle messageBundleCanadian = // shared across entire application
			ResourceBundle.getBundle("com.lightspeedeps.util.app.messageResources", Constants.LOCALE_FRENCH_CANADA);
	private static Locale currentLocale = null; // shared across entire application

	private MsgUtils() {
	}

	/**
	 * Get a message from the application's message ResourceBundle using
	 * the current Locale.
	 * @param msgid The keyword identifying the message in the message resource file.
	 * @return The message text from the resource file.
	 */
	public static String getMessage(String msgid) {
		return getMessage(msgid, Constants.LOCALE_US);
	}

	public static String getMessage(String msgid, Locale locale) {
		String msg;
		try {
			msg = getMessageBundle(locale).getString(msgid);
		}
		catch (Exception e) {
			EventUtils.logError("Error retrieving message `" + msgid + "`", e);
			msg = "MISSING MESSAGE, id=" + msgid;
		}
		return msg;
	}

	/**
	 * @param locale
	 * @return ResourceBundle messageBundle
	 */
	private static ResourceBundle getMessageBundle(Locale locale) {
		if (locale != null && locale.equals(Constants.LOCALE_FRENCH_CANADA)) {
			return messageBundleCanadian;
		}
		return messageBundle;
	}

	/**
	 * Determine if a message exists in the application's message ResourceBundle
	 * using the current Locale.
	 *
	 * @param msgid The keyword identifying the message in the message resource
	 *            file.
	 * @return True if the message exists, false otherwise.
	 */
	public static boolean existsMessage(String msgid) {
		return existsMessage(msgid, Constants.LOCALE_US);
	}

	public static boolean existsMessage(String msgid, Locale locale) {
		boolean found = false;
		try {
			String msg = getMessageBundle(locale).getString(msgid);
			if (msg != null) {
				found = true;
			}
		}
		catch (MissingResourceException e) {
		}
		catch (Exception e) {
			EventUtils.logError("Error retrieving message `" + msgid + "`", e);
		}
		return found;
	}


	/**
	 * Format a message with substitution parameters after retrieving it
	 * from the application's message ResourceBundle.
	 * The message will not be associated with any specific field on the page.
	 * @param msgid The keyword identifying the message in the message resource file.
	 * @param args The arguments to be substituted into the message where {0},
	 * {1}, etc., appear.
	 * @return The formatted message text.
	 */
	public static String formatMessage(String msgid, Object... args) {
		return formatMessage(msgid, null, args);
	}

	public static String formatMessage(String msgid, Locale locale, Object... args) {
		if (locale == null) {
			if (getCurrentLocale() != null) {
				locale = getCurrentLocale();
			}
			else {
				locale = Constants.LOCALE_US;
			}
		}
		String output = getMessage(msgid, locale);
		MessageFormat formatter;
		formatter = new MessageFormat(output, locale);
		//log.debug("before: " + output);
		output = formatter.format(args);
		//log.debug("after : " + output);
		return output;
	}


	/**
	 * Format text with substitution parameters.
	 *
	 * @param text The text, which typically will contain substitution markers
	 *            in the form of braces surrounding an integer, e.g., {0}.
	 * @param args The arguments to be substituted into the message where {0},
	 *            {1}, etc., appear.
	 * @return The formatted text.
	 */
	public static String formatText(String text, Object... args) {
		MessageFormat formatter;
		if (getCurrentLocale() != null) {
			formatter = new MessageFormat(text, getCurrentLocale());
		}
		else { // probably a quartz (scheduled) task calling us
			formatter = new MessageFormat(text); // use default locale
		}
		text = formatter.format(args);
		return text;
	}

	/**
	 * Create a complete, secure, URL -- typically for use in emails -- which ends
	 * with the given path.
	 *
	 * @param path The page reference, relative to our context root.
	 * @param useESS If true, use the ESS application URL as the base, not TTCO.
	 * @return A fully-qualified URL, using the secure protocol (if specified in
	 *         the appropriate web.xml parameter).
	 */
	public static String createPath(String path, boolean useESS) {
		// Combine base URL and additional path
		String url = useESS ? ApplicationUtils.getEssBaseUrl() : ApplicationUtils.getSecureBaseUrl();
		if (path != null) {
			url += path;
		}
		return url;
	}

	/**
	 * Determine the appropriate URL for a login link, depending on both the
	 * current domain in use, and also whether the current production is a Team
	 * client. LS-1763
	 *
	 * @param pageRef The page reference, relative to our context root; may be null.
	 * @return The appropriate login path to send to a client.
	 */
	public static String createBrandedPath(String pageRef) {
		return createBrandedPath(pageRef, false); // don't use ESS URL. ESS-1364
	}

	/**
	 * Determine the appropriate URL for a login link, depending on both the
	 * current domain in use, and also whether the current production is a Team
	 * client. LS-1763.  Includes option to have the link redirect to the ESS
	 * application. ESS-1364
	 *
	 * @param pageRef The page reference, relative to our context root; may be null.
	 * @param useESS If true, use the ESS application URL as the base, not TTCO. ESS-1364
	 * @return The appropriate login path to send to a client.
	 */
	public static String createBrandedPath(String pageRef, boolean useESS) {
		String path = null;
		if (SessionUtils.isTTCOnline() || useESS) {
			path = MsgUtils.createPath(pageRef, useESS);
		}
		else if (SessionUtils.isTTCProd()) {
			// user connected via lightspeedeps.com URL, but generating email from a Team-branded
			// production, so return a team-centric login URL:
			path = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_SECURE_BASE_URL);
			boolean beta = ApplicationScopeBean.getInstance().getIsBeta();
			path = path.replace("*", (beta ? Constants.TTC_ONLINE_BETA_DOMAIN : Constants.TTC_ONLINE_DOMAIN));
			if (pageRef != null) {
				path += pageRef;
			}
		}
		else {
			path = MsgUtils.createPath(pageRef, useESS);
		}
		return path;
	}

	/**
	 * Add our standard "unexpected error" message to the current Faces context.
	 */
	public static void addGenericErrorMessage() {
		addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
	}

	/**
	 * Add a set of JSF messages from the message ResourceBundle to the current
	 * Faces context. The messages will not be associated with any specific
	 * field on the page.
	 *
	 * @param errorList A List of message ids identifying the message in the
	 *            message resource file.
	 * @param severity The FacesMessage severity level, e.g, Info or Error.
	 */
	public static void addFacesMessage(List<String> errorList, FacesMessage.Severity severity) {
		if (errorList != null) {
			for (String err : errorList) {
				MsgUtils.addFacesMessage(err, severity);
			}
		}
	}

	/**
	 * Add a JSF message from the message ResourceBundle to the current Faces context.
	 * The message will not be associated with any specific field on the page.
	 * @param msgid The keyword identifying the message in the message resource file.
	 * @param severity The FacesMessage severity level, e.g, Info or Error.
	 */
	public static void addFacesMessage(String msgid, FacesMessage.Severity severity) {
		addFacesMessage(null, msgid, severity);
	}

	/**
	 * Add a JSF message from the message ResourceBundle to the current Faces
	 * context for the specific field identified by 'clientid'.
	 *
	 * @param clientid The value of the 'id' parameter of the field within the
	 *            jsp page.
	 * @param msgid The keyword identifying the message in the message resource
	 *            file.
	 * @param severity The FacesMessage severity level, e.g, Info or Error.
	 */
	public static void addFacesMessage(String clientid, String msgid, FacesMessage.Severity severity) {
		String output = getMessage( msgid );
		FacesMessage msg = new FacesMessage(severity, output, "");
		FacesContext.getCurrentInstance().addMessage(clientid, msg);
		setMsgExists(true);
		log.debug("clientid=`"+clientid+"`, msg=`"+msg.getSummary()+"`");
	}

	/**
	 * Format a message with substitution parameters after retrieving it from
	 * the application's message ResourceBundle; then add it to the current
	 * Faces context.
	 *
	 * @param msgid The keyword identifying the message in the message resource
	 *            file.
	 * @param severity The FacesMessage severity level, e.g, Info or Error.
	 * @param args The arguments to be substituted into the message where {0},
	 *            {1}, etc., appear.
	 */
	public static void addFacesMessage(String msgid, FacesMessage.Severity severity, Object... args) {
		addFacesMessage(null, msgid, severity, args);
	}


	/**
	 * Format a message with substitution parameters after retrieving it from
	 * the application's message ResourceBundle; then add it to the current
	 * Faces context, for the specific field identified by 'clientid'.
	 *
	 * @param clientid The value of the 'id' parameter of the field within the
	 *            jsp page.
	 * @param msgid The keyword identifying the message in the message resource
	 *            file.
	 * @param severity The FacesMessage severity level, e.g, Info or Error.
	 * @param args The arguments to be substituted into the message where {0},
	 *            {1}, etc., appear.
	 */
	public static void addFacesMessage(String clientid, String msgid, FacesMessage.Severity severity, Object... args) {
		String output = formatMessage(msgid, args);
		FacesMessage msg = new FacesMessage(severity, output, "");
		FacesContext context = FacesContext.getCurrentInstance();
		if (context != null) {
			context.addMessage(clientid, msg);
			setMsgExists(true);
		}
		else {
			log.warn(output);
		}
		log.debug("clientid=`"+clientid+"`, msg=`"+msg.getSummary()+"`");
	}

	/**
	 * Add a JSF message using the given text to the current Faces context.
	 *
	 * @param text The text of the message to be displayed.
	 * @param severity The FacesMessage severity level, e.g, Info or Error.
	 */
	public static void addFacesMessageText(String text, FacesMessage.Severity severity) {
		FacesMessage msg = new FacesMessage(severity, text, "");
		FacesContext context = FacesContext.getCurrentInstance();
		if (context != null) {
			context.addMessage(null, msg);
			setMsgExists(true);
		}
		else {
			log.warn(text);
		}
		log.debug("msg=`"+msg.getSummary()+"`");
	}

	/**
	 * Add a JSF message using the given text to the current Faces context.
	 *
	 * @param msgs The list of text messages to be displayed.
	 * @param severity The FacesMessage severity level, e.g, Info or Error.
	 */
	public static void addFacesMessageText(List<String> msgs, FacesMessage.Severity severity) {
		FacesMessage msg;
		FacesContext context = FacesContext.getCurrentInstance();

		for(String message : msgs) {
			msg = new FacesMessage(severity, message, "");
			if (context != null) {
				context.addMessage(null, msg);
				setMsgExists(true);
			}
			else {
				log.warn(msg);
			}
			log.debug("msg=`"+msg.getSummary()+"`");
		}
	}

	public static ResourceBundle getMessageBundle() {
		return messageBundle;
	}

	public static Locale getCurrentLocale() {
		if (currentLocale == null && FacesContext.getCurrentInstance() != null) {
			currentLocale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
		}
		return currentLocale;
	}

	/** Delegate method to set HeaderViewBean's msgExists. */
	public static void setMsgExists(boolean msgExists) {
		HeaderViewBean.getInstance().setMsgExists(msgExists);
	}

}
