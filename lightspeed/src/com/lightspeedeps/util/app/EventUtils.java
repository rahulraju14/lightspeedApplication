package com.lightspeedeps.util.app;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.EventDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.type.EventType;

/**
 * A utility class for methods related to the Event database table
 * and Event objects.
 */
public class EventUtils {
	private static final Log log = LogFactory.getLog(EventUtils.class);

	private EventUtils() {
	}

	/**
	 * Add an event to the event log (table) using current system information
	 * for user name, timestamp, production, and project.  Only the event
	 * type needs to be supplied.
	 * @param type  The type of event to log.
	 */
	public static void logEvent(EventType type) {
		try {
			logEvent( type, getProduction(),
					SessionUtils.getCurrentProject(), SessionUtils.getCurrentContact().getEmailAddress(), null);
		}
		catch (Exception e) {
			log.error("exception: ", e);
			breakpointTrap();
		}
	}

	/**
	 * Create an Event and store it, including the current Production, current
	 * Project, and current User information.
	 *
	 * @param type The EventType to assign to the logged Event
	 * @param description A description of the event, which will be put into the
	 *            Event log.
	 */
	public static void logEvent(EventType type, String description) {
		try {
			String username = null;
			User user;
			if ((user=SessionUtils.getCurrentUser()) != null) {
				username = user.getEmailAddress();
			}
			logEvent(type, getProduction(),
					SessionUtils.getCurrentProject(), username, description);
		}
		catch (Exception e) {
			log.error("exception: ", e);
			breakpointTrap();
		}
	}

	/**
	 * Create an Event and store it, including the current Production, the
	 * specified Project, and current User information.
	 *
	 * @param type The EventType to assign to the logged Event
	 * @param description A description of the event, which will be put into the
	 *            Event log.
	 * @param project The Project associated with the event being logged.
	 */
	public static void logEvent(EventType type, String description, Project project) {
		try {
			logEvent(type, getProduction(),
					project, SessionUtils.getCurrentContact().getEmailAddress(), description);
		}
		catch (Exception e) {
			log.error("exception: ", e);
			breakpointTrap();
		}
	}

	/**
	 * Create an Event and store it, including the specified Production, the
	 * specified Project, and the given User information.
	 *
	 * @param type The EventType to assign to the logged Event
	 * @param description A description of the event, which will be put into the
	 *            Event log.
	 * @param production The Production associated with the event being logged.
	 * @param project The Project associated with the event being logged.
	 * @param username The name of the User associated with the event being logged; typically
	 *            the logged-in user.
	 */
	public static void logEvent(EventType type, Production production, Project project,
				String username) {
		logEvent(type, production, project, username, null);
	}

	/**
	 * Create an Event and store it, including the specified Production, the
	 * specified Project, and the given User information.
	 *
	 * @param type The EventType to assign to the logged Event
	 * @param description A description of the event, which will be put into the
	 *            Event log.
	 * @param production The Production associated with the event being logged.
	 * @param project The Project associated with the event being logged.
	 * @param username The name of the User associated with the event being logged; typically
	 *            the logged-in user.
	 * @param description A description of the event, which will be put into the
	 *            Event log.
	 */
	public static void logEvent(EventType type, Production production, Project project,
			String username, String description) {
		try {
			Integer userId = null;
			try {
				userId = SessionUtils.getInteger(Constants.ATTR_CURRENT_USER);
			}
			catch (Exception e) { // ignore exceptions getting user id
			}
			log.info("event=" + type + ", user=" + username + ", user#=" + userId);
			if (description != null && description.length() > Event.MAX_DESC_LENGTH) {
				description = description.substring(0, Event.MAX_DESC_LENGTH);
			}
			Event event = new Event(project, production, type, new Date(), username, description);
			EventDAO.getInstance().save(event);
		}
		catch (Exception e) {
			log.error("exception: ", e);
			breakpointTrap();
		}
	}

	/**
	 * Log the startup of the application. Usually called from
	 * ApplicationScopeBean after it is constructed.
	 *
	 * @param eventDAO The EventDAO bean, as it may not be available via
	 *            findBean
	 * @param version The version string, usually from a context parameter. This
	 *            will be included in the Event log entry.
	 */
	public static void logStartup(EventDAO eventDAO, String version) {
		try {
			long heapSize = Runtime.getRuntime().maxMemory() / (1024*1024);
			Event event = new Event(null, null, EventType.APP_START,
					new Date(), "EventUtils.logStartup",
					"Version=" + version + "; max heap size=" + heapSize + "M");
			eventDAO.save(event);
		}
		catch (Exception e) {
			log.error("exception: ", e);
			breakpointTrap();
		}
	}

	/**
	 * Creates a ERROR debug-log entry, plus an Event log entry of type APP_ERROR,
	 * with the given description.
	 * @param description
	 */
	public static void logError(String description) {
		log.error("(App_error event recorded) " + description);
		logEvent(EventType.APP_ERROR, description);
		breakpointTrap();
	}

	/**
	 * Creates a ERROR debug-log entry, including a stack trace, plus an Event
	 * log entry of type APP_ERROR. The stack trace is also included in the
	 * Event log, along with information on the current Production, current
	 * Project, and currently logged-in User.
	 *
	 * @param e The Exception to be logged.
	 */
	public static void logError(Throwable e) {
		if (e instanceof LoggedException) {
			log.error(e.getLocalizedMessage());
		}
		else {
			log.error("exception: ", e);
			logEvent(EventType.APP_ERROR, traceToString(e) );
		}
		breakpointTrap();
	}

	/**
	 * Creates a ERROR debug-log entry, including a stack trace, plus an Event
	 * log entry of type APP_ERROR. The stack trace is also included in the
	 * Event log. The fields of the passed object are also output using
	 * reflection.
	 *
	 * @param obj The object whose fields should be included in the error output.
	 * @param e The Exception to be logged.
	 */
	public static void logError(Object obj, Throwable e) {
		if (e instanceof LoggedException) {
			log.error(e.getLocalizedMessage());
		}
		else {
			log.error("exception: ", e);

			String dump = "";
			try {
				dump = ReflectionToStringBuilder.toString(obj);
				log.error(dump);
			}
			catch (Exception e1) {
			}
			logEvent(EventType.APP_ERROR, traceToString(e) +
					Constants.NEW_LINE + dump + Constants.NEW_LINE);
		}
		breakpointTrap();
	}

	/**
	 * Creates a ERROR debug-log entry, including a stack trace, plus an Event
	 * log entry of type APP_ERROR, with the given description. The stack trace
	 * is also included in the Event log.
	 *
	 * @param msg The message to be included prior to the stack trace, both in
	 *            the debug log output and the Event log description.
	 * @param e The Exception to be logged.
	 */
	public static void logError(String msg, Throwable e) {
		if (e instanceof LoggedException) {
			log.error(e.getLocalizedMessage());
		}
		else {
			log.error(msg, e);
			logEvent(EventType.APP_ERROR, msg + Constants.NEW_LINE + traceToString(e) );
			breakpointTrap();
		}
	}

	/**
	 * Creates a ERROR debug-log entry, including a stack trace, plus an Event
	 * log entry of type APP_ERROR, with the given description. The stack trace
	 * is also included in the Event log.
	 *
	 * @param pLog The Log object used to output the log entry. By using this
	 *            technique, the log entry will have the caller's class and
	 *            method information, instead of EventUtils'.
	 * @param msg The message to be included prior to the stack trace, both in
	 *            the debug log output and the Event log description.
	 * @param e The Exception to be logged.
	 */
	public static void logError(Log pLog, String msg, Throwable e) {
		if (e instanceof LoggedException) {
			pLog.error(e.getLocalizedMessage());
		}
		else {
			pLog.error(msg, e);
			logEvent(EventType.APP_ERROR, msg + Constants.NEW_LINE + traceToString(e) );
			breakpointTrap();
		}
	}

	private static Production getProduction() {
		Production prod = null;
		try {
			prod = SessionUtils.getProduction();
			if (prod == null) { // see if there's a production for "My Timecard" or similar
				Integer prodId = SessionUtils.getInteger(Constants.ATTR_VIEW_PRODUCTION_ID);
				if (prodId != null) {
					prod = ProductionDAO.getInstance().findById(prodId);
				}
			}
		}
		catch (Exception e) {
			log.error("Error retrieving production", e);
			breakpointTrap();
		}
		return prod;
	}

	/**
	 * Generate a stack trace from an Exception and return it as a String.
	 *
	 * @param e The Exception whose stack trace should be formatted.
	 * @return The formatted stack trace.
	 */
	public static String traceToString(Throwable e) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(os);
			e.printStackTrace(ps);
			ps.close();
			return os.toString();
		}
		catch (Exception e1) {
			log.error("exception: ", e);
			breakpointTrap();
		}
		return "[EXCEPTION]";
	}

	/**
	 * A method called by all error methods, for ease of break-pointing.
	 */
	private static void breakpointTrap() {
		// someplace to put a breakpoint in debug mode;
		// all EventUtil errors eventually call this method.
		return;
	}

//	/**
//	 * Generate a stack trace from the given Exception and output it to the
//	 * debug log, limited to 1000 characters.
//	 *
//	 * @param e The Exception whose stack trace should be formatted.
//	 */
//	public static void shortTrace(Throwable e) {
//		try {
//			String trace = traceToString(e);
//			trace = trace.substring(0, Math.min(1000,trace.length()));
//			log.debug(trace);
//		}
//		catch (Exception e1) {
//		}
//	}
//
//	/**
//	 * Generate a stack trace at the current point of execution and output it to
//	 * the debug log, limited to 1000 characters.
//	 */
//	public static void shortTrace() {
//		shortTrace(new Throwable());
//	}

}
