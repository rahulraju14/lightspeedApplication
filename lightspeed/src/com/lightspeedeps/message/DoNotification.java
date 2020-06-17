//	File Name:	DoNotification.java
package com.lightspeedeps.message;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.faces.bean.*;

import org.apache.commons.logging.*;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.StringUtils;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.web.login.PasswordBean;
import com.lightspeedeps.web.validator.*;
import com.ttconline.dto.EmailDTO;

/**
 * A class for handling notifications.
 * <p>
 * DoNotification handles formatting and storing (in the database) of Notifications, Messages and
 * MessageInstances. It has code that is specific to various events (such as a Calendar change), but
 * only so far as knowing which message resources to retrieve to create the appropriate subject and
 * body texts, and, in some cases, which Contacts to generate messages for. It does NOT contain code
 * that is specific to the determination of whether an event occurred that requires notification.
 * This code remains in the class that caused or was "aware of" the event due to a user interaction.
 * For example, code in PrSchEditBean (calendar edit) determines that a particular date changed from
 * a Travel Day to a Shooting Day, and then it invokes a method in DoNotification to generate the
 * appropriate notifications for that event.
 * <p>
 * From various trigger points in the application, methods here are called for specific notification
 * types. Different types have different parameters, so unique entry points are required in many
 * cases.
 * <p>
 * For each notification call, we build a Notification object, a set of Contacts to be notified, and
 * a Message for each type of delivery system (email, Web, SMS, etc.).
 * <p>
 * From the set of Contacts, we create a MessageInstance for each Contact and each Message, if that
 * Contact has the appropriate preference and data (e.g., email address) filled in. (The
 * MessageInstances are stored for tracking acknowledgements, and for display on the "My Message"
 * page.)
 * <p>
 * Each MessageInstance is then "dispatched" to its intended recipient in the executeMessages method.
 */
@ManagedBean
@ViewScoped
public class DoNotification implements Serializable {
	/** */
	private static final long serialVersionUID = - 7171295258724723589L;

	private static final Log log = LogFactory.getLog(DoNotification.class);

	private transient MessageDAO messageDAO;
	private transient NotificationDAO notificationDAO;

	/** A Spring component used to schedule asynchronous tasks, in this case, to send the
	 * email and SMS messages. */
	private TaskExecutor taskExecutor;

	/** The current Production title (for messages) */
//	String productionName;

	/** The current project. */
	private Project project;

	/** The Unit associated with the message(s) being generated; may be null. */
	private Unit unit;

	/** If true, an email notification will be sent regardless of the user's preference
	 * setting.  Used for some timecard notifications. */
	boolean ignoreEmailPref = false;

	/** short Date format: 3-letter month + day number (SMS and/or subject lines) */
	private final Format shortDateFormat = new SimpleDateFormat("MMM d");

	/** time format: h:mm am/pm */
	private final Format timeFormat = new SimpleDateFormat("h:mm a");

	/** long Date format: weekday + month-name + day number (for body text) */
	private final Format bodyDateFormat = new SimpleDateFormat(Constants.LONG_DATE_FORMAT);

	/** Timecard W/E Date format: usually mm/dd/yyyy (or foreign equivalent?) */
	private final Format timecardDateFormat = new SimpleDateFormat(Constants.WEEK_END_DATE_FORMAT);

	/** short Date & Time: 3-letter month, day number, time */
	private final Format updatedDateTimeFormat = new SimpleDateFormat("MMM d, h:mm a");

	/** full Date & Time: 3-letter month, day number, year, time */
	private final Format fullDateTimeFormat = new SimpleDateFormat("MMM d, yyyy, h:mm a");

	private Locale locale = Constants.LOCALE_US;

	public DoNotification() {
		log.debug("");
	}

	public static DoNotification getInstance() {
		return (DoNotification)ServiceFinder.findBean("doNotification");
	}

	/**
	 * Acquire an instance of this class when a FacesContext is not available;
	 * this requires an entry in applicationContext.xml. This is used by
	 * quartz-scheduled ("batch") processes.
	 *
	 * @param ctx The current ApplicationContext reference.
	 * @return Our instance.
	 */
	public static DoNotification getInstance(ApplicationContext ctx) {
		return (DoNotification) ctx.getBean("NotificationBean");
	}

	/**
	 * Called when the "normal week days off" setting has changed within the
	 * next 7 days. The project (for logging the Notification) is assumed to be
	 * the current project.
	 *
	 * @param pUnit The unit whose calendar was updated. This will be used to
	 *            find the list of Contacts to be notified, and this unit's name
	 *            may be used within the message text.
	 * @param date The date whose status has changed.
	 * @param oldOffDay True iff the date given used to be an "off" day.
	 * @param newOffDay True iff the date given is now an "off" day.
	 */
	public void calendarDayOffChanged(Unit pUnit, Date date, boolean oldOffDay, boolean newOffDay) {
		if (!isNotifying()) {
			return;
		}
		try {
			//String oldType = (oldOffDay ? "WEEKLY OFF" : "SHOOTING");
			String newType = (newOffDay ? "Day Off" : "Shooting Day");
			String url = createUnitPath(Constants.CALENDAR_PATH, pUnit);
			Collection<Contact> contacts = new ArrayList<>();
			List<Contact> users = ProjectMemberDAO.getInstance().findByUnitDistinctContact(pUnit);
			for (Contact user : users) {
				if (user != null &&
						user.getNotifyForCalendarChanges()) {
					contacts.add(user);
				}
			}
			if (contacts.size() > 0) {
				sendToAll(NotificationType.DAY_TYPE_CHANGED, "notification.scheduler", contacts,
						null,
						shortDateFormat.format(date),
						bodyDateFormat.format(date),
						url, newType);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Called when the WorkdayType of a date within the next week has changed.
	 * Notify all project members of the schedule change. The project (for
	 * logging the Notification) is assumed to be the current project.
	 *
	 * @param pUnit The unit whose calendar was updated. This will be used to
	 *            find the list of Contacts to be notified, and this unit's name
	 *            may be used within the message text.
	 * @param date The date whose day type has changed.
	 * @param oldType The old day type.
	 * @param newType The new day type.
	 */
	public void calendarDayTypeChanged(Unit pUnit, Date date, String oldType, String newType) {
		log.debug("");
		if (!isNotifying()) {
			return;
		}
		try {
			Collection<Contact> contacts = new ArrayList<>();
			List<Contact> users = ProjectMemberDAO.getInstance().findByUnitDistinctContact(pUnit);
			for (Contact user : users) {
				if (user != null &&
						user.getNotifyForCalendarChanges()) {
					contacts.add(user);
				}
			}
			log.debug("count=" + contacts.size());
			if (contacts.size() > 0) {
				String url = createUnitPath(Constants.CALENDAR_PATH, pUnit);
				sendToAll(NotificationType.DAY_TYPE_CHANGED, "notification.scheduler", contacts,
						null,
						shortDateFormat.format(date),
						bodyDateFormat.format(date),
						url, newType);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * send callsheet call-time change alert email/sms for Cast or crew members
	 *
	 * @param contact The contact to whom the alert will be sent; may be null.
	 * @param pUnit The unit whose name may be used within the message text.
	 * @param csDate The date of the call sheet
	 * @param callTimeCal The new Date call time for the Contact. If null,
	 *            the user was just removed from the call sheet.
	 * @param added True if this user was just added to the call sheet.
	 */
	public void crewCalltimeChanged(Contact contact, Unit pUnit, Date csDate,
			Date callTimeCal, boolean added) {
		log.debug("");
		if ( ! isNotifying()) {
			return;
		}
		try {
			log.debug("");
			if (contact != null && contact.getNotifyForAlerts()) {
				setUnit(pUnit);
				List<Contact> contactList = makeContactList(contact);
				String url = createUnitPath(Constants.CALLSHEET_PATH, pUnit);
				log.debug("");
				if (callTimeCal == null) { // member dropped from callsheet
					sendToAll(NotificationType.CALL_TIME_REMOVED, "notification.calltimeRemoved", contactList,
							null,
							shortDateFormat.format(csDate),
							bodyDateFormat.format(csDate), url);
				}
				else if (added) {
					sendToAll(NotificationType.CALL_TIME_ADDED, "notification.crewCalltimeAdded", contactList,
							null,
							shortDateFormat.format(csDate),
							bodyDateFormat.format(csDate),
							timeFormat.format(callTimeCal),
							url);
				}
				else {
					sendToAll(NotificationType.CALL_TIME_CHANGED, "notification.crewCalltimeChanged", contactList,
							null,
							shortDateFormat.format(csDate),
							bodyDateFormat.format(csDate),
							timeFormat.format(callTimeCal),
							url);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * send callsheet call-time change alert email/sms for Cast members
	 *
	 * @param contact The contact to whom the alert will be sent; may be null.
	 * @param pUnit The unit whose name may be used within the message text.
	 * @param csDate The date of the call sheet
	 * @param pkupTime The new pickup time for the Contact. If null, the user
	 *            was just removed from the call sheet.
	 * @param mkupTime The new 'report to makeup' time for the Contact. If null,
	 *            the user was just removed from the call sheet.
	 * @param onsetTime The new 'on-set' time for the Contact. If null, the user
	 *            was just removed from the call sheet.
	 * @param added True if this user was just added to the call sheet.
	 */
	public void castCalltimeChanged(Contact contact, Unit pUnit, Date csDate,
			Date pkupTime, Date mkupTime, Date onsetTime, boolean added) {
		log.debug("");
		if ( ! isNotifying()) {
			return;
		}
		try {
			if (contact != null && contact.getNotifyForAlerts()) {
				setUnit(pUnit);
				List<Contact> contactList = makeContactList(contact);
				String url = createUnitPath(Constants.CALLSHEET_PATH, pUnit);
				String pkup = (pkupTime == null ? "N/A" : timeFormat.format(pkupTime));
				String mkup = (mkupTime == null ? "N/A" : timeFormat.format(mkupTime));
				String onset = (onsetTime == null ? "N/A" : timeFormat.format(onsetTime));
				if (pkupTime == null) { // member dropped from callsheet
					this.sendToAll(NotificationType.CALL_TIME_REMOVED, "notification.calltimeRemoved", contactList,
							null,
							shortDateFormat.format(csDate),
							bodyDateFormat.format(csDate), url);
				}
				else if (added) {
					sendToAll(NotificationType.CALL_TIME_ADDED, "notification.castCalltimeAdded", contactList,
							null,
							shortDateFormat.format(csDate),
							bodyDateFormat.format(csDate),
							onset, url, pkup, mkup);
				}
				else {
					sendToAll(NotificationType.CALL_TIME_CHANGED, "notification.castCalltimeChanged", contactList,
							null,
							shortDateFormat.format(csDate),
							bodyDateFormat.format(csDate),
							onset, url, pkup, mkup);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Sends a notification to cast members when a Callsheet has been published.
	 *
	 * @param contactList The Collection of Contacts to receive this message.
	 * @param pUnit The unit whose name may be used within the message text.
	 * @param times A String consisting of three times concatenated, separated
	 *            by blanks. (The times are expressed as long values.) The first
	 *            time is the pickup time, followed by the make-up time,
	 *            followed by the on-set time. At least one of the three times
	 *            must be positive; a zero time will be inserted into the
	 *            message as "N/A".
	 */
	public void castCalltimePublished(Collection<Contact> contactList, Unit pUnit, String times) {
		log.debug("");
		if (!isNotifying()) {
			return;
		}
		if (contactList != null && contactList.size() > 0) {
			setUnit(pUnit);
			String url = createUnitPath(Constants.CALLSHEET_PATH, pUnit);
			String time[] = times.split(" ");
			long pickup = Long.parseLong(time[0]);
			long makeup = Long.parseLong(time[1]);
			long onset = Long.parseLong(time[2]);
			Date pickupTime = new Date(pickup);
			Date makeupTime = new Date(makeup);
			Date onsetTime = new Date(onset);
			Date useDate = (onset != 0 ? onsetTime : makeup != 0 ? makeupTime : pickupTime);
			sendToAll(NotificationType.CALL_TIME_PUBLISHED, "notification.calltimePublished.Cast", contactList,
					null,
					(onset==0 ? "N/A" : timeFormat.format(onsetTime)),
					shortDateFormat.format(useDate),
					bodyDateFormat.format(useDate),
					url,
					(pickup==0 ? "N/A" : timeFormat.format(pickupTime)),
					(makeup==0 ? "N/A" : timeFormat.format(makeupTime)) );
		}
	}

	/**
	 * Sends a notification to crew members when a Callsheet has been published.
	 *
	 * @param contactList The Collection of Contacts to receive this message.
	 * @param pUnit The unit whose name may be used within the message text.
	 * @param callTime The crew call time to be included in the message text.
	 */
	public void crewCalltimePublished(Collection<Contact> contactList, Unit pUnit, Date callTime) {
		log.debug("");
		if (!isNotifying()) {
			return;
		}
		if (contactList != null && contactList.size() > 0) {
			setUnit(pUnit);
			String url = createUnitPath(Constants.CALLSHEET_PATH, pUnit);
			sendToAll(NotificationType.CALL_TIME_PUBLISHED, "notification.calltimePublished.Crew", contactList,
					null,
					timeFormat.format(callTime),
					shortDateFormat.format(callTime),
					bodyDateFormat.format(callTime),
					url);
		}
	}

	/**
	 * Sends a notification to a list of Contact's regarding a change of
	 * shooting location when a Callsheet has been saved.
	 *
	 * @param contactList The Collection of Contacts to receive this message.
	 * @param pUnit The unit whose name may be used within the message text.
	 * @param date The shooting date - to be included in message
	 * @param newLocationIds The Collection of database ids for the Locations
	 *            (RealWorldElement) that exist in the updated version of the
	 *            Callsheet. This will be used to create a textual list of names
	 *            and addresses to be included in the message body.
	 */
	public void callsheetLocationChanged(Collection<Contact> contactList, Unit pUnit, Date date,
			Collection<Integer> newLocationIds) {
		log.debug("contacts=" + contactList.size() + ", new locs=" + newLocationIds.size());
		if (!isNotifying()) {
			return;
		}
		setUnit(pUnit);
		String url = createUnitPath(Constants.CALLSHEET_PATH, pUnit);
		String locations = createLocationList(newLocationIds);
		sendToAll(NotificationType.LOCATION_CHANGED, "notification.callsheetLocation", contactList,
				null,
				shortDateFormat.format(date),
				bodyDateFormat.format(date), url,
				locations);
	}

	/**
	 * Send the "call sheet locations published" message.
	 *
	 * @param contacts The Collection of Contacts to receive the message.
	 * @param pUnit The Unit associated with the Callsheet being published. The
	 *            unit name may be included in message text.
	 * @param date The date of the Callsheet (which day it applies to).
	 * @param fileNames A Collection of filenames of the files to be attached to
	 *            each message. These files are normally PDFs of location
	 *            direction pages, one for each location referenced on the
	 *            Callsheet.
	 * @param locationIds A List of Integer values, each of which is the
	 *            database id of a RealWorldElement representing a set location
	 *            referenced on the Callsheet. This will be used to create a
	 *            textual list of names and addresses to be included in the
	 *            message body.
	 */
	public void callsheetLocationPublished(Collection<Contact> contacts, Unit pUnit, Date date,
			Collection<String> fileNames, List<Integer> locationIds) {
		if (!isActive()) {
			return;
		}
		if (contacts != null && contacts.size() > 0) {
			setUnit(pUnit);
			String locations = createLocationList(locationIds);
			Notification notification = new Notification(NotificationType.DIRECTIONS, new Date(),
					getProject(), date, ReportType.CALL_SHEET);
			sendFileToContacts(notification, "notification.locDirReport", fileNames, contacts,
					true, null,
					shortDateFormat.format(date),
					bodyDateFormat.format(date),
					locations);
		}
	}

	/**
	 * Send the "call sheet scenes changed" message.
	 *
	 * @param contactList The list of Contacts to receive the message.
	 * @param pUnit The Unit associated with the Callsheet being published. The
	 *            unit name may be included in message text.
	 * @param date The date of the Callsheet (which day it applies to).
	 * @param oldScenes The text indicating the previous list of scenes on the
	 *            Callsheet, which may be used in the message text.
	 * @param newScenes The text indicating the new list of scenes on the
	 *            Callsheet, which may be used in the message text.
	 */
	public void callsheetScenesChanged(Collection<Contact> contactList, Unit pUnit, Date date,
			String oldScenes, String newScenes) {
		log.debug("");
		if (!isNotifying()) {
			return;
		}
		log.debug("");
		setUnit(pUnit);
		String url = createUnitPath(Constants.CALLSHEET_PATH, pUnit);
		sendToAll(NotificationType.SCENE_CHANGED, "notification.callsheetScene", contactList,
				null,
				shortDateFormat.format(date),
				bodyDateFormat.format(date),
				url, newScenes);
	}

	/**
	 * Send the "call sheet published" message, and attach the PDF of the
	 * Callsheet to the email messages.
	 *
	 * @param contactList The list of Contacts to receive the message.
	 * @param pUnit The Unit associated with the Callsheet being published. The
	 *            unit name may be included in message text.
	 * @param reportDate The date of the Callsheet (which day it applies to).
	 * @param updateTime The timestamp on the Callsheet -- the last time it was
	 *            updated.
	 * @param file The name of the PDF containing the Callsheet.
	 */
	public void callsheetPublished(Collection<Contact> contactList, Unit pUnit, Date reportDate,
			Date updateTime, String file) {
		if (!isActive()) {
			return;
		}
		if (contactList != null && contactList.size() > 0) {
			setUnit(pUnit);
			Notification notification = new Notification(NotificationType.REPORT_PUBLISHED, new Date(),
					getProject(), reportDate, ReportType.CALL_SHEET);
			List<String> fileNames = new ArrayList<>(1);
			fileNames.add(file);
			sendFileToContacts(notification, "notification.callsheetReport", fileNames,
					contactList, true, null,
					shortDateFormat.format(reportDate),
					bodyDateFormat.format(reportDate),
					updatedDateTimeFormat.format(updateTime) );
		}
	}

	/**
	 * Send the "call sheet deleted" message.
	 *
	 * @param contactList The list of Contacts to receive the message.
	 * @param pUnit The Unit associated with the Callsheet being published. The
	 *            unit name may be included in message text.
	 * @param reportDate The date of the Callsheet (which day it applies to).
	 */
	public void callsheetDeleted(Collection<Contact> contactList, Unit pUnit, Date reportDate) {
		if (!isActive()) {
			return;
		}
		if (contactList != null && contactList.size() > 0) {
			setUnit(pUnit);
			Notification notification = new Notification(NotificationType.CALL_TIME_REMOVED, new Date(),
					getProject(), reportDate, ReportType.CALL_SHEET);
			sendToAll(notification, "notification.callsheetDeleted", contactList,
					null,
					shortDateFormat.format(reportDate), // {3} in message
					bodyDateFormat.format(reportDate) ); // {4} in message
		}
	}

	/**
	 * Unused - 8/14/2012 rev 3313
	 * @param contacts
	 * @param reportDate
	 * @param updated
	 * @param file
	 */
	@SuppressWarnings("unused")
	private void dprPublished(Collection<Contact> contacts, Date reportDate, Date updated, String file) {
/*		if (!isActive()) {
			return;
		}
		if (contacts != null && contacts.size() > 0) {
			setUnit(null); // not used for DPR
			Notification notification = new Notification(NotificationType.REPORT_PUBLISHED, new Date(),
					getProject(), reportDate, ReportType.DPR);
			List<String> fileNames = new ArrayList<String>(1);
			fileNames.add(file);
			sendFileToAll(notification, "notification.dprReport", fileNames, contacts,
					null,
					shortDateFormat.format(reportDate),
					bodyDateFormat.format(reportDate),
					updatedDateTimeFormat.format(updated) );
		}
*/	}

	public void dprSubmitted(Collection<Contact> contactList, Date date) {
		if (!isNotifying()) {
			return;
		}
		if (contactList != null && contactList.size() > 0) {
			Unit u = SessionUtils.getCurrentProject().getMainUnit();
			String url = createUnitPath(Constants.DPR_PATH, u);
			Notification notification = new Notification(NotificationType.REPORT_SUBMITTED, new Date(),
					getProject(), date, ReportType.DPR);
			sendToAll(notification, "notification.dprSubmitted", contactList,
					null,
					shortDateFormat.format(date),
					bodyDateFormat.format(date),
					url);
		}
	}

//	public void timesheetSubmitted(Collection<Contact> contactList, Date date) {
//		if (!isNotifying()) {
//			return;
//		}
//		if (contactList != null && contactList.size() > 0) {
//			Unit u = SessionUtils.getCurrentProject().getMainUnit();
//			String url = buildUnitPath(Constants.TIMESHEET_PATH, u);
//			Notification notification = new Notification(NotificationType.REPORT_SUBMITTED, new Date(),
//					getProject(), date, ReportType.TIME_SHEET);
//			sendToAll(notification, "notification.timesheetSubmitted", contactList,
//					null,
//					shortDateFormat.format(date),
//					bodyDateFormat.format(date),
//					url);
//		}
//	}

	/**
	 * Send a (partial) script listing to a group of contacts.
	 *
	 * @param contacts The contacts to receive the data.
	 * @param date The date on which the given script text will be shot.
	 * @param file The fully-qualified filename of the script print file to be
	 *            attached to each email.
	 */
	public void scriptPages(Project pProject, Collection<Contact> contacts, Date date,
			Integer days, String sceneNumberList, String file) {
		if (contacts != null && contacts.size() > 0) {
			Calendar today = Calendar.getInstance();
			CalendarUtils.setStartOfDay(today);
			setProject(pProject); // will be used to determine email sender; #787

			String subject = formatMessage("notification.scriptPages.Subject", pProject);
			// Format the message body that precedes the script text
			String bodyMsg = formatMessage("notification.scriptPages.Msg", pProject,
					days, bodyDateFormat.format(date), sceneNumberList );
			bodyMsg += MsgUtils.getMessage("notification.scriptPages.Suffix"); // add trailing msg

			List<String> fileNames = new ArrayList<>(1);
			fileNames.add(file);

			Notification notification = new Notification(NotificationType.SCRIPT_PAGES, new Date(), pProject,
					today.getTime(), null);
			WaterMark wm = null;
			if (pProject.getProduction().getWatermark() == WatermarkPreference.REQUIRED) {
				wm = new WaterMark(); // Watermark distributed scripts if required by prod. #787
			}
			sendFileToAll(notification, contacts, true, null, subject, bodyMsg, fileNames, wm);
		}
	}

	/**
	 * Unused - 8/14/2012 rev 3313
	 * Send a stripboard report to a group of contacts.
	 * @param contacts Contacts to receive the report file.
	 * @param file The fully-qualified filename of the file to be attached to each email.
	 * @param pUnit The Unit related to this stripboard report - used for message inserts.
	 */
	@SuppressWarnings("unused")
	private void stripboardUpdated(Collection<Contact> contacts, String file, Unit pUnit) {
/*		if (!isActive()) {
			return;
		}
		if (contacts != null && contacts.size() > 0) {
			setUnit(unit);
			List<String> fileNames = new ArrayList<String>(1);
			fileNames.add(file);
			Notification notification = new Notification(NotificationType.STRIPBOARD_CHANGED, new Date(), getProject());
			sendFileToAll(notification, "notification.stripBrdReport", fileNames, contacts, null );
		}
*/	}

	public void dueNotification(Project pProject, Collection<Contact> contactList,
			Date startDate, ReportType reportType) {
		if (!isNotifying(pProject)) {
			return;
		}
		Notification notification = new Notification(NotificationType.REPORT_DUE, new Date(),
				pProject, startDate, reportType);
		sendToAll(notification,
				"notification." + reportType.getMessageName() + "Due",
				contactList,
				pProject,
				shortDateFormat.format(startDate),
				bodyDateFormat.format(startDate) );
	}

	public void overdueNotification(Project pProject, Collection<Contact> contactList,
			Date startDate, ReportType reportType) {
		log.debug("");
		if (!isNotifying(pProject)) {
			return;
		}
		//log.debug("");
		Notification notification = new Notification(NotificationType.REPORT_OVERDUE, new Date(),
				pProject, startDate, reportType);
		sendToAll(notification,
				"notification." + reportType.getMessageName() + "Overdue",
				contactList,
				pProject,
				shortDateFormat.format(startDate),
				bodyDateFormat.format(startDate) );
	}

	/**
	 * Send a message to the administrator when a username gets locked out due to an excessive
	 * number of failed login attempts (invalid passwords).
	 * @param user The User that has just been locked out.
	 */
	public void lockoutMessage(User user) {
		String email = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_EMAIL_AUDIT);
		Contact admin = ContactDAO.getInstance().findOneByProperty(ContactDAO.EMAIL_ADDRESS, email);
		if (admin != null) {
			sendToAll(NotificationType.USER_LOCK_OUT, "notification.lockout",
					makeContactList(admin), null,
					user.getEmailAddress(), user.getLastNameFirstName());
		}
		else {
			EventUtils.logError("Missing admin email for 'locked out' message.");
		}
	}


	/**
	 * Send notification to LS audit trail that a new Production has been created.
	 * @param prod
	 */
	public void productionCreated(Production prod, User user) {
		log.debug("");
		try {
			setProject(prod.getDefaultProject());
			String email = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_EMAIL_AUDIT);
			Contact admin = ContactDAO.getInstance().findOneByProperty(ContactDAO.EMAIL_ADDRESS, email);
			if (admin != null) {
				sendToAll(NotificationType.PRODUCTION_CREATED, "notification.production.created",
						makeContactList(admin), getProject(),
						prod.getProdId(), prod.getOwningAccount(),
						(prod.getNextBillDate()==null ? "NOT SET" : bodyDateFormat.format(prod.getNextBillDate())),
						user.getAccountNumber(), user.getDisplayName(), user.getEmailAddress(),
						prod.getTransactionId(), prod.getSku());
			}
			else {
				EventUtils.logError("Missing admin email for CREATE Production.");
			}

			// Create email to send to user, along with Quick Start guide.
			Notification notification = new Notification(NotificationType.PRODUCTION_CREATED, new Date(), getProject());

			Collection<String> fileNames = new ArrayList<>();
			String path = SessionUtils.getRealPath(Constants.EXPORT_FOLDER) +
					File.separator + Constants.QUICK_START_GUIDE_FILENAME;
			fileNames.add(path);

			sendToUser(notification, user, "notification.production.userCreated", null, fileNames,
					user.getFirstNameLastName(), prod.getTitle());
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Send notification to LS audit trail that a re-subscribe has taken place.
	 * @param prod
	 */
	public void productionResubscribed(Production prod, User user) {
		log.debug("");
		try {
			setProject(prod.getDefaultProject());
			String email = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_EMAIL_AUDIT);
			Contact admin = ContactDAO.getInstance().findOneByProperty(ContactDAO.EMAIL_ADDRESS, email);
			if (admin != null) {
				sendToAll(NotificationType.PRODUCTION_RESUBSCRIBED, "notification.production.resubscribed",
						makeContactList(admin), getProject(),
						prod.getProdId(), prod.getOwningAccount(),
						(prod.getNextBillDate()==null ? "NOT SET" : bodyDateFormat.format(prod.getNextBillDate())),
						user.getAccountNumber(), user.getDisplayName(), user.getEmailAddress(),
						prod.getTransactionId());
			}
			else {
				EventUtils.logError("Missing admin email for RESUBSCRIBED Production.");
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Send notification to LS audit trail that an un-subscribe has taken place.
	 *
	 * @param prod
	 */
	public void productionUnsubscribed(Production prod) {
		log.debug("");
		try {
			setProject(prod.getDefaultProject());
			String email = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_EMAIL_AUDIT);
			Contact admin = ContactDAO.getInstance().findOneByProperty(ContactDAO.EMAIL_ADDRESS, email);
			if (admin != null) {
				User user = SessionUtils.getCurrentUser();
				sendToAll(NotificationType.PRODUCTION_UNSUBSCRIBED, "notification.production.unsubscribed",
						makeContactList(admin), getProject(),
						prod.getProdId(), prod.getOwningAccount(),
						(prod.getNextBillDate()==null ? "NOT SET" : bodyDateFormat.format(prod.getNextBillDate())),
						user.getAccountNumber(), user.getDisplayName(), user.getEmailAddress());
			}
			else {
				EventUtils.logError("Missing admin email for UNSUBSCRIBED Production.");
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Send notification to LS audit trail that a re-subscribe has taken place.
	 * @param prod
	 */
	public void productionUpgraded(Production prod, User user) {
		log.debug("");
		try {
			setProject(prod.getDefaultProject());
			String email = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_EMAIL_AUDIT);
			Contact admin = ContactDAO.getInstance().findOneByProperty(ContactDAO.EMAIL_ADDRESS, email);
			if (admin != null) {
				sendToAll(NotificationType.PRODUCTION_UPGRADED, "notification.production.upgrade",
						makeContactList(admin), getProject(),
						prod.getProdId(), prod.getOwningAccount(),
						(prod.getNextBillDate()==null ? "NOT SET" : bodyDateFormat.format(prod.getNextBillDate())),
						user.getAccountNumber(), user.getDisplayName(), user.getEmailAddress(),
						prod.getTransactionId(), prod.getSku());
			}
			else {
				EventUtils.logError("Missing admin email for UPGRADED Production.");
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Send the notification(s) appropriate for when a new Contact has been
	 * created for an existing User and invited to join a Production. That is,
	 * an "Add" was done on the Cast & Crew page, and the user's account
	 * (specified by the email address) already existed.
	 *
	 * @param contact The newly created Contact.
	 */
	public void inviteContact(Contact contact) {
		log.debug("");
		User user = contact.getUser();
		User inviter = SessionUtils.getCurrentUser();
		String inviterName = inviter.getDisplayName();
		String inviterEmail = inviter.getEmailAddress();

		if ((SessionUtils.isTTCProd() || SessionUtils.isTTCOnline())
				&& FF4JUtils.useFeature(FeatureFlagType.TTCO_ESS_EMAIL_INVITATIONS)) {
			// Send email via micro-service. ESS-1471
			sendEmail(contact, EmailType.INVITE_CONTACT,
					MsgUtils.createBrandedPath(Constants.MY_ACCT_PATH), // login URL
					inviterName, inviterEmail);
		}
		else {
			String companyName = Constants.LS_COMPANY_NAME;
			sendToAll(NotificationType.NEW_CONTACT, "notification.inviteContact", makeContactList(contact),
					contact.getProject(),
					MsgUtils.createBrandedPath(Constants.MY_ACCT_PATH), // message arguments #3 and beyond
					user.getDisplayName(), user.getEmailAddress(), // #4, #5
					inviterName, inviterEmail,						// #6, #7
					companyName ); 									// #8
		}
	}

	/**
	 * Send the notification(s) appropriate for when a new User Account has been
	 * created and the User/Contact has been invited to join a Production. This
	 * invitation includes a link (which expires) allowing the user to reset
	 * their password, since the initial account is set up with a random
	 * password that is not displayed anywhere. The title of the current Production
	 * will be included in the message, and the name of the current logged-in user
	 * as the inviting person.
	 *
	 * @param contact The contact whose email address will be the recipient of
	 *            the message.
	 * @return The link sent in the email, which will take the user to the New
	 *         Registration page.
	 */
	public String inviteNewAccountFirst(Contact contact) {
		return inviteNewAccount(contact, "notification.inviteAccount", EmailType.INVITE_NEW_ACCOUNT, true);
	}

	/**
	 * Send the notification(s) appropriate for when a new User Account has been
	 * created and the User/Contact has been invited to join a Production. This
	 * invitation includes a link (which expires) allowing the user to reset
	 * their password, since the initial account is set up with a random
	 * password that is not displayed anywhere. This version sends a message
	 * that does NOT include the name of the inviting production -- it is used
	 * when the original link has expired, and the user is requesting a new
	 * link, but we don't "know" which production invited them.
	 *
	 * @param contact The contact whose email address will be the recipient of
	 *            the message.
	 */
	public void inviteNewAccountAgain(Contact contact) {
		inviteNewAccount(contact, "notification.reInviteAccount", EmailType.INVITE_NEW_ACCOUNT, false);
	}

	/**
	 * Send the notification(s) appropriate for when a new User Account has been
	 * created and the User/Contact has been invited to join a Production. This
	 * invitation includes a link (which expires) allowing the user to reset
	 * their password, since the initial account is set up with a random
	 * password that is not displayed anywhere.
	 *
	 * @param contact The contact whose email address will be the recipient of
	 *            the message.
	 * @param msgId The resource id (prefix) of the message (subject and body)
	 *            to be sent.
	 * @param type The EmailType, identifying the email template to be sent;
	 *            used for emails sent via the email micro-service API.
	 * @param showInviter True iff the name of the current user should be passed
	 *            as an argument to the message formatting code.
	 * @return The link sent in the email, which will take the user to the New
	 *         Registration page.
	 */
	private String inviteNewAccount(Contact contact, String msgId, EmailType type, boolean showInviter) {
		log.debug("");
		boolean isTTC = (SessionUtils.isTTCProd() || SessionUtils.isTTCOnline());
		User user = contact.getUser(); // The User being invited.
		String url = PasswordBean.createNewUserURI(user.getId(), Constants.TIME_EMAIL_LINK_VALID, isTTC);
		log.debug("new account url: " + url);
		String inviterName = "", inviterEmail = "";

		if (showInviter) {
			User inviter = SessionUtils.getCurrentUser();
			inviterName = inviter.getDisplayName();
			inviterEmail = inviter.getEmailAddress();
		}
		Project ctProject = contact.getProject();

		if (ctProject == null) {
			ctProject = contact.getProduction().getDefaultProject();
		}

		if (isTTC && FF4JUtils.useFeature(FeatureFlagType.TTCO_ESS_EMAIL_INVITATIONS)) {
			// Send email via micro-service. ESS-1471
			sendEmail(contact, type, url, inviterName, inviterEmail);
		}
		else {
			List<Contact> list = makeContactList(contact);
			String supportEmail = Constants.LS_SUPPORT_EMAIL;
			String supportPhoneExt = Constants.LS_SUPPORT_PHONE + ", " + Constants.LS_MARKETING_EXT;
			String companyName = Constants.LS_COMPANY_NAME;
			sendToAll(NotificationType.NEW_ACCOUNT, msgId, list,
					ctProject, 					// a default Project
					url, user.getDisplayName(), // message arguments #3 and beyond
					contact.getEmailAddress(),	// #5
					inviterName, inviterEmail,  // #6, #7
					supportEmail, supportPhoneExt,	// #8, #9
					companyName);  // #10
		}
		return url;
	}

	/**
	 * Send an email via the email micro-service API. ESS-1471
	 *
	 * @param contact The Contact who is the email recipient.
	 * @param type The type of email (determines the email template used).
	 * @param url The URL (if any) to be embedded in the email body, e.g., a
	 *            new-account setup link.
	 * @param inviterName The display name of the inviter (sender); may be used
	 *            in the email body.
	 * @param inviterEmail The email address of the inviter (sender); may be
	 *            used in the email body.
	 */
	public void sendEmail(Contact contact, EmailType type, String url,
			String inviterName, String inviterEmail) {
		User user = contact.getUser(); // The User being invited.
		EmailDTO email = new EmailDTO(type.getTemplate());
		email.setExistingUser(type==EmailType.INVITE_CONTACT);
		email.setActionUrl(url);
		email.setDisplayFrench(user.getShowCanada() ? "Y" : "N");
		email.setSource("TTCO");
		email.setProductionName(contact.getProduction().getTitle());
		email.setToAddress(user.getEmailAddress());
		email.setInviterEmail(inviterEmail);
		email.setInviterName(inviterName);
		ApiUtils.sendEmail(email);
	}

	/**
	 * Send the notification(s) appropriate for when a new User Account has been created
	 * by the user themselves (self-registration).  An email is sent to the new user.
	 * @param user The User just created.
	 */
	public void newAccount(User user) {
		log.debug("");
		Notification notification = new Notification(NotificationType.NEW_ACCOUNT, new Date(), null);
		sendToUser(notification, user, "notification.registerAccount", null, null,
				MsgUtils.createBrandedPath(null));
	}

	/**
	 * Send an email notification to the LS audit trail that a new User account was created.
	 *
	 * @param user The newly-created User.
	 * @param selfCreated True iff this is a notification for a self-created user account.
	 */
	public void userCreated(User user, boolean selfCreated) {
		log.debug("");
		String email = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_EMAIL_AUDIT);
		Contact admin = ContactDAO.getInstance().findOneByProperty(ContactDAO.EMAIL_ADDRESS, email);
		if (admin != null) {
			if (selfCreated) {
				sendToAll(NotificationType.NEW_ACCOUNT, "notification.accountSelfCreated", makeContactList(admin),
						null,
						user.getAccountNumber(), user.getLastNameFirstName(),
						user.getEmailAddress(),
						fullDateTimeFormat.format(user.getCreated()),
						SessionUtils.get(Constants.ATTR_IP_ADDR),
						(user.getReferredBy()==null ? "(none)" : user.getReferredBy()) );
			}
			else {
				User current = SessionUtils.getCurrentUser();
				sendToAll(NotificationType.NEW_ACCOUNT, "notification.accountCreated", makeContactList(admin),
						null,
						user.getAccountNumber(), user.getLastNameFirstName(),
						user.getEmailAddress(),
						current.getAccountNumber(), current.getLastNameFirstName(),
						current.getEmailAddress(),
						fullDateTimeFormat.format(user.getCreated()),
						SessionUtils.get(Constants.ATTR_IP_ADDR));
			}
		}
	}

	/**
	 * Send an email notification to the LS audit trail that a User account was
	 * deleted.
	 *
	 * @param user The User being deleted.
	 * @param selfDeleted True if this is a notification that a user is deleting
	 *            their own account (from the My Account page). False if the
	 *            User account is being deleted from the Prod Admin / Users
	 *            page.
	 */
	public void userDeleted(User user, boolean selfDeleted) {
		log.debug("");
		String email = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_EMAIL_AUDIT);
		Contact admin = ContactDAO.getInstance().findOneByProperty(ContactDAO.EMAIL_ADDRESS, email);
		if (admin != null) {
			if (selfDeleted) {
				sendToAll(NotificationType.ACCOUNT_DELETED, "notification.accountSelfDeleted", makeContactList(admin),
						null,
						user.getAccountNumber(), user.getLastNameFirstName(),
						user.getEmailAddress(),
						fullDateTimeFormat.format(user.getCreated()),
						SessionUtils.get(Constants.ATTR_IP_ADDR),
						(user.getReferredBy()==null ? "(none)" : user.getReferredBy()) );
			}
			else {
				User current = SessionUtils.getCurrentUser();
				sendToAll(NotificationType.ACCOUNT_DELETED, "notification.accountDeleted", makeContactList(admin),
						null,
						user.getAccountNumber(), user.getLastNameFirstName(),
						user.getEmailAddress(),
						current.getAccountNumber(), current.getLastNameFirstName(),
						current.getEmailAddress(),
						fullDateTimeFormat.format(user.getCreated()),
						SessionUtils.get(Constants.ATTR_IP_ADDR));
			}
		}
	}

	/**
	 * Send the notification(s) appropriate for when an invited User accepted an
	 * invitation to join a production.
	 * @param toContact
	 */
	public void invitationAccepted(Contact toContact, User invitedUser, Production production ) {
		log.debug(""+production.getTitle());
		Project proj = production.getDefaultProject();
		proj = ProjectDAO.getInstance().refresh(proj);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		sendToAll(NotificationType.NEW_CONTACT_ACCEPT, "notification.invitationAccepted",
				makeContactList(toContact), proj, invitedUser.getDisplayName());
	}

	/**
	 * Send the notification(s) appropriate for when an invited User declined an
	 * invitation to join a production.
	 * @param toContact
	 */
	public void invitationDeclined(Contact toContact, User invitedUser, Production production) {
		log.debug("");
		Project proj = production.getDefaultProject();
		proj = ProjectDAO.getInstance().refresh(proj);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		sendToAll(NotificationType.NEW_CONTACT_DECLINE, "notification.invitationDeclined",
				makeContactList(toContact), proj, invitedUser.getDisplayName());
	}

	/**
	 * Sends the message for resetting a user's password -- this is requested by
	 * the user from the "reset password" page, typically when the user has
	 * forgotten their password.
	 *
	 * @param user The User who requested the message and to whom it will
	 *            be sent.
	 * @param url The special URL which links to the "reset return" page, with
	 *            an encrypted key that includes the Contact info and a
	 *            timestamp.
	 */
	public void passwordResetMessage(User user, String url) {
		log.debug("user #" + user.getId());
		Notification notification = new Notification(NotificationType.PW_RESET, new Date(), null);
		sendToUser(notification, user, "notification.resetPassword", null, null, url);
	}

	/**
	 * Send one or more batches of timecards.
	 *
	 * @param emailAddress The recipient's email address.
	 * @param fileNames An optional List of file names, representing files to be
	 *            attached to the email. If 'null', no files are attached.
	 * @param proj The relevant Project, whose name may be used in the message.
	 * @param batchCount The number of batches being sent, to be included in the
	 *            email body.
	 * @param batchMessage The list of batch names, to be included in the email
	 *            body.
	 * @param timecardCount The total number of timecards being sent, to be
	 *            included in the email body.
	 */
	public void sendTimecardBatch(String emailAddress, Collection<String> fileNames,
			Project proj, int batchCount, String batchMessage, int timecardCount, WeeklyBatch weeklyBatch) {
		Date date = new Date();
		Collection<String> emails = new ArrayList<>();
		emails.add(emailAddress);
		Notification notification = new Notification(NotificationType.TIMECARD_BATCH_SENT, date,
				getProject(), date, ReportType.TIMECARD);

		if (SessionUtils.getProduction().getBatchTransferExtraField() && weeklyBatch != null) {
			// include additional "custom" fields in email message
			sendFileToEmails(notification, "notification.timecardBatchExtraFields", fileNames, emails,
					proj, batchCount, batchMessage, timecardCount, "B#" + weeklyBatch.getId(),
					weeklyBatch.getJobName(), weeklyBatch.getJobCode(), weeklyBatch.getGlAccountCode(),
					weeklyBatch.getCostCenter(), weeklyBatch.getComments());
		}
		else { // standard batch email message
			sendFileToEmails(notification, "notification.timecardBatch", fileNames, emails,
					proj, batchCount, batchMessage, timecardCount, "B#" + weeklyBatch.getId());
		}

	}

	/**
	 * Send a timesheet -- used for Tours productions.
	 *
	 * @param emailAddress The recipient's email address.
	 * @param fileNames An optional List of file names, representing files to be
	 *            attached to the email. If 'null', no files are attached.
	 * @param proj The relevant Project, whose name may be used in the message.
	 * @param timecardCount The total number of line items in the timesheet, to be
	 *            included in the email body.
	 */
	public void sendTimesheet(String emailAddress, Collection<String> fileNames,
			Project proj, int timecardCount) {
		Date date = new Date();
		Collection<String> emails = new ArrayList<>();
		emails.add(emailAddress);
		User user = SessionUtils.getCurrentUser();
		Notification notification = new Notification(NotificationType.TIMESHEET_SENT, date,
				getProject(), date, ReportType.TIMECARD);
		sendFileToEmails(notification, "notification.timesheetSent", fileNames, emails,
				proj, timecardCount, user.getFirstNameLastName());
	}

	/**
	 * Method to transfer documents.
	 *
	 * @param emailList The list of recipients' email addresses.
	 * @param fileNames An optional List of file names, representing files to be
	 *            attached to the email. If 'null', no files are attached.
	 * @param proj The relevant Project, whose name may be used in the message.
	 * @param contactCount number of people whose documents are included, to be
	 *            included in the email body.
	 * @param docCount The total number of documents being sent, to be included
	 *            in the email body.
	 * @param fileNameText The textual list of filenames, to be included in the
	 *            email body.
	 */
	public void sendDocuments(Collection<String> emailList, Collection<String> fileNames,
			Project proj, int contactCount, int docCount, String fileNameText) {
		Date date = new Date();
		Notification notification = new Notification(NotificationType.DOCUMENTS_SENT, date,
				getProject(), date, ReportType.START_DOCUMENT);
		sendFileToEmails(notification, "notification.sendDocuments", fileNames, emailList,
				proj, contactCount, docCount, fileNameText);
	}

	/**
	 * Method to send Intent form.
	 *
	 * @param emailList The list of recipients' email addresses.
	 * @param fileNames An optional List of file names, representing files to be
	 *            attached to the email. If 'null', no files are attached.
	 * @param proj The relevant Project, whose name may be used in the message.
	 */
	public void sendIntentForm(Collection<String> emailList, Collection<String> fileNames,
			Project proj) {
		Date date = new Date();
		Notification notification = new Notification(NotificationType.DOCUMENTS_SENT, date,
				getProject(), date, ReportType.START_DOCUMENT);
		sendFileToEmails(notification, "notification.sendIntent", fileNames, emailList, proj);
	}

	/**
	 * Method to send files to an individual employee.
	 *
	 * @param emailAddress The recipient's email address.
	 * @param proj The relevant Project, whose name may be used in the message.
	 * @param fileName
	 * @param unionName
	 */
	public void sendIndividualEmpDocuments(String emailAddress, Project proj, String fileName, String unionName) {
		Date date = new Date();
		Collection<String> emails = new ArrayList<>();
		emails.add(emailAddress);
		Collection<String> fileNames = new ArrayList<>();
		fileNames.add(fileName);
		Notification notification = new Notification(NotificationType.DOCUMENTS_SENT, date,
				getProject(), date, ReportType.START_DOCUMENT);
		sendFileToEmails(notification, "notification.sendDocuments.Individual", fileNames, emails, proj, unionName);
	}

	/**
	 * Send document to individual union office. LS-2197
	 *
	 * @param emailAddress The recipient's email address.
	 * @param proj The relevant Project, whose name may be used in the message.
	 * @param filename
	 * @param unionName
	 */
	public void sendIndividualOfficeDocuments(Collection<String> emailAddress, Project proj,
			List<String> fileNames, String unionName) {
		Date date = new Date();
		Notification notification = new Notification(NotificationType.DOCUMENTS_SENT, date,
				getProject(), date, ReportType.START_DOCUMENT);
		sendFileToEmails(notification, "notification.sendDocuments.Individual.Office", fileNames, emailAddress, proj, unionName);
	}

	/**
	 * Method to send project data notification.
	 *
	 * @param emailAddress The recipient's email address.
	 * @param proj The relevant Project, whose name may be used in the message.
	 * @param projectText Additional text for email - currently project/job code
	 *            for commercial productions.
	 */
	public void sendProject(String emailAddress, Project proj, String projectText) {
		Date date = new Date();
		Collection<String> emails = new ArrayList<>();
		emails.add(emailAddress);
		Notification notification = new Notification(NotificationType.PROJECT_SENT, date,
				getProject(), date, ReportType.PROJECT);
		sendFileToEmails(notification, "notification.sendProject", null, emails,
				proj, projectText);
	}

	/**
	 * Method to send project data notification.
	 *
	 * @param emailAddress The recipient's email address.
	 * @param proj The relevant Project, whose name may be used in the message.
	 * @param contact Contact record associated.
	 */
	public void sendAllocation(String emailAddress, Project proj, List<String> fileNames, Contact contact) {
		Date date = new Date();
		Collection<String> emails = new ArrayList<>();
		emails.add(emailAddress);
		Notification notification = new Notification(NotificationType.ALLOCATION_SENT, date,
				getProject(), date, null);
		sendFileToEmails(notification, "notification.allocation", fileNames, emails,
				proj, contact.getDisplayName());
	}

	/**
	 * Sends the appropriate message when a WeeklyTimecard has been pulled.
	 *
	 * @param firstName The (timecard's) employee's first name.
	 * @param lastName The (timecard's) employee's last name.
	 * @param weDate The week-ending date of the timecard.
	 * @param puller The Contact representing the person who did the Pull
	 *            action, typically the current Contact.
	 * @param oldApproverName The display name (first last) of the approver from
	 *            whose queue the timecard was removed. This will be used in the
	 *            notification text.
	 * @param approvers The List of Contact`s to receive the message, which is
	 *            the person from whose queue it was removed, and all following
	 *            approvers, up to, but not including, the approver doing the
	 *            Pull.
	 */
	public void timecardPulled(String firstName, String lastName, Date weDate, Contact puller,
			String oldApproverName, List<Contact> approvers) {
		log.debug("empl=" +  firstName + " " + lastName + ", date=" + weDate);
		if (!isNotifying()) {
			return;
		}
		Project proj = SessionUtils.getCurrentProject();
		Notification notification = new Notification(NotificationType.TIMECARD_PULLED, new Date(), proj);
		String msgDate = timecardDateFormat.format(weDate);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		sendToAll(notification, "notification.timecardPulled",
				approvers, proj, lastName + ", " + firstName, firstName + " " + lastName, msgDate,
				puller.getUser().getFirstNameLastName(), oldApproverName);
	}

	/**
	 * Sends the appropriate message to an approver when a WeeklyTimecard is
	 * ready for approval.
	 *
	 * @param employee The employee whose WeeklyTimecard has been added to the
	 *            approver's queue.
	 * @param weDate The week-ending date of the timecard.
	 * @param approver The Contact to whose queue the timecard has been added.
	 */
	public void timecardReady(Contact employee, Date weDate, Contact approver, Project proj) {
		log.debug("empl=" +  employee + ", date=" + weDate);

		if (! approver.getNotifyForApproval()) {
			return;
		}

		if (proj == null) {
			proj = findAndSetProject(employee);
		}

		if (!isNotifying()) {
			return;
		}
		List<Contact> contacts = makeContactList(approver);

		Notification notification = new Notification(NotificationType.TIMECARD_READY, new Date(), proj);
		String msgDate = timecardDateFormat.format(weDate);
		sendToAll(notification, "notification.timecardReady",
				contacts, proj, employee.getUser().getLastNameFirstName(),
				employee.getUser().getFirstNameLastName(),
				msgDate);
	}

	/**
	 * Sends the appropriate message to an approver when a I9 Document is ready
	 * for approval.
	 *
	 * @param employee The employee whose contactDocument has been added to the
	 *            approver's queue.
	 * @param weDate The date of submission of the document.
	 * @param approverList The collection of Contact`s to whose queue the
	 *            document has been added. (May be more than one if the approval
	 *            path uses an approver Pool.
	 */
	/*public void formI9Submitted(Contact employee, Date weDate, Collection<Contact> approverList) {
		log.debug("empl=" +  employee + ", date=" + weDate + "approver=" +  approverList.size());

		if (! approver.getNotifyForApproval()) { // code was returning after log 1232 due to this code.
			log.debug("..");
			return;
		}

		Project proj = findAndSetProject(employee);

		if (!isNotifying()) {
			log.debug("..");
			return;
		}
		//List<Contact> contacts = makeContactList(approver);
		Notification notification = new Notification(NotificationType.FORM_I9_SUBMITTED, new Date(), proj);
		String msgDate = timecardDateFormat.format(weDate);
		sendToAll(notification, "notification.formI9Submitted",
				approverList, proj, employee.getUser().getLastNameFirstName(),
				employee.getUser().getFirstNameLastName(),
				msgDate);
	}*/

	/**
	 * Sends a message to an approvers with the status of the I9 documents that
	 * have not been completed.
	 *
	 * @param contactId The contact.id value of the Approver receiving the
	 *            message.
	 * @param messageList The list of I9Message objects to be turned into text
	 *            lines in the email.
	 */
	public void formI9Waiting(Integer contactId, List<I9Message> messageList) {
		Contact contact = ContactDAO.getInstance().findById(contactId);
		List<Contact> contacts = makeContactList(contact);

		// Note: production and project notification settings are checked in FormI9Check, not here!

		String i9Message = "";
		String projectTitle = "";
		for (I9Message msg : messageList) {

			// Check if production is commercial
			if (msg.getProjectName() != null && ! msg.getProjectName().isEmpty()) {
				// Check if msg belongs to the project same as previous msg
				if (! msg.getProjectName().equals(projectTitle)) {
					// To give Space before new project's messages and to avoid this space for first project.
					if (! projectTitle.isEmpty()) {
						i9Message = i9Message + "\n";
					}
					projectTitle = msg.getProjectName();
					i9Message = i9Message + "\n" + "Project:  " + projectTitle;
				}
			}
			String name = msg.getLastName() + "," + msg.getFirstName();
			/*for (int i = name.length(); i<=20; i++) {
				name = name + " ";
			}*/
			//name = org.apache.commons.lang.StringUtils.rightPad(name, 20);
			String occupation = msg.getOccupation();
			/*for (int i = occupation.length(); i<=30; i++) {
				occupation = occupation + " ";
			}*/
			//occupation = org.apache.commons.lang.StringUtils.rightPad(occupation, 30);
			name = String.format("%1$2s", name);
			name = String.format("%1$-20s", name).replace(" ", "  ");;
			occupation = String.format("%1$-40s", occupation).replace(" ", "  ");;

			if (msg.getDaysLeft() < 0) {
				i9Message = i9Message + "\n" + name + occupation + " " + "OVERDUE";
			}
			else {
				i9Message = i9Message + "\n"  + name + occupation + " " +  msg.getDaysLeft();
			}
		}
		i9Message = "<pre>" + i9Message + "</pre>";
		log.debug("contact=" + contact + ", I9 text=" + i9Message);
		Project proj = findAndSetProject(contact);

		Date date = new Date();
		Notification notification = new Notification(NotificationType.FORM_I9_WAITING, date, proj);
		String msgDate = timecardDateFormat.format(date);
		sendToAll(notification, "notification.formI9Waiting", contacts, proj, msgDate, i9Message);
	}

	/**
	 * Sends the appropriate message to an approver when a contactDocument is
	 * ready for approval.
	 *
	 * @param employee The employee whose contactDocument has been added to the
	 *            approver's queue.
	 * @param submittedDate The date of submission of the document.
	 * @param approver The Contact to whose queue the document has been added.
	 */
	/*public void formI9Expired(Contact employee, Date submittedDate, Contact approver) {
		log.debug("empl=" +  employee + ", date=" + submittedDate + "approver=" + approver);

		if (! approver.getNotifyForApproval()) {
			log.debug("..");
			return;
		}

		Project proj = findAndSetProject(employee);

		if (!isNotifying()) {
			log.debug("..");
			return;
		}
		List<Contact> contacts = makeContactList(approver);

		Calendar c = Calendar.getInstance();
		c.setTime(submittedDate);
		c.add(Calendar.DATE, 3);
		log.debug(">..........>>>>>>>>>>...date of expiration: " + c.getTime());

		Notification notification = new Notification(NotificationType.FORM_I9_EXPIRED, new Date(), proj);
		String msgDate = timecardDateFormat.format(submittedDate);
		sendToAll(notification, "notification.formI9Expired",
				contacts, proj, employee.getUser().getLastNameFirstName(),
				employee.getUser().getFirstNameLastName(),
				msgDate, c.getTime());
	}*/

	/**
	 * Sends the appropriate message to an approver when a contactDocument is
	 * distributed.
	 *
	 * @param employee The employee to whom the contactDocuments are distributed.
	 * @param Sender The Contact who has distributed the documents.
	 * @param numOfDocs The The number of documents that are distributed.
	 * @param distributedDocumentList String to hold the list of document names.
	 */
	public void documentDistributed(Contact employee, Contact sender, int numOfDocs, List<String> distributedDocumentList)  {
		log.debug("empl=" +  employee + ", numOfDocs=" + numOfDocs + "sender=" +  sender);
		/*if (! approver.getNotifyForApproval()) {
			log.debug("..");
			return;
		}*/
		Project proj = findAndSetProject(sender);

		if (!isNotifying()) {
			log.debug("..");
			return;
		}
		List<Contact> contacts = makeContactList(employee);
		String documentNames = "";
		for (String docName : distributedDocumentList) {
			documentNames = documentNames + " - " + docName + "<br/>";
		}

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		Notification notification = new Notification(NotificationType.DOCUMENT_DISTRIBUTED, new Date(), proj);
		sendToAll(notification, "notification.documentsSentToYou",
				contacts, proj, employee.getUser().getLastNameFirstName(),
				employee.getUser().getFirstNameLastName(),
				MsgUtils.createBrandedPath(null), numOfDocs, documentNames);
	}

	/**
	 * Sends the appropriate message when a WeeklyTimecard has been recalled.
	 *
	 * @param proj The project associated with the timecard being recalled.
	 * @param firstName The (timecard's) employee's first name.
	 * @param lastName The (timecard's) employee's last name.
	 * @param weDate The week-ending date of the timecard.
	 * @param recaller The Contact representing the person who did the Recall
	 *            action, typically the current Contact.
	 * @param approvers The List of Contact`s to receive the message, which is
	 *            everyone who has approved the timecard, after the person who
	 *            did the recall, and also the current approver (before it was
	 *            recalled).
	 */
	public void timecardRecalled(Project proj, String firstName, String lastName, Date weDate, Contact recaller,
			Collection<Contact> approvers) {
		log.debug("empl=" +  firstName + " " + lastName + ", date=" + weDate);
		setProject(proj);
		if (!isNotifying()) {
			return;
		}
		Notification notification = new Notification(NotificationType.TIMECARD_RECALLED, new Date(), proj);
		String msgDate = timecardDateFormat.format(weDate);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		sendToAll(notification, "notification.timecardRecalled",
				approvers, proj, lastName + ", " + firstName, firstName + " " + lastName, msgDate,
				recaller.getUser().getFirstNameLastName());
	}

	/**
	 * Sends the appropriate message(s) when a WeeklyTimecard has been rejected.
	 *
	 * @param firstName The (timecard's) employee's first name.
	 * @param lastName The (timecard's) employee's last name.
	 * @param weDate The week-ending date of the timecard.
	 * @param rejector The Contact of the person doing the Reject operation.
	 * @param receiver The Contact to whom the timecard was sent back -- the
	 *            person who must either resubmit or reapprove the rejected
	 *            timecard.
	 * @param approvers The List of Contact`s representing approvers who should
	 *            be notified of the rejection action.
	 * @param comment The reason text entered by the person doing the reject;
	 *            may be null.
	 */
	public void timecardRejected(String firstName, String lastName, Date weDate, Contact rejector, Contact receiver,
			Collection<Contact> approvers, String comment) {
		log.debug("empl=" + firstName + " " + lastName + ", date=" + weDate);
		if (!isNotifying()) {
			return;
		}
		Project proj = SessionUtils.getCurrentProject();
		Notification notification = new Notification(NotificationType.TIMECARD_REJECTED, new Date(), proj);
		String msgDate = timecardDateFormat.format(weDate);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		if (approvers.size() > 0) {
			sendToAll(notification, "notification.timecardRejected",
					approvers, proj, lastName + ", " + firstName, firstName + " " + lastName,
					msgDate, rejector.getUser().getFirstNameLastName(), comment);
		}

		sendToUser(notification, receiver.getUser(), "notification.timecardRejectedToYou",
				proj, null, lastName + ", " + firstName, firstName + " " + lastName,
				msgDate, rejector.getUser().getFirstNameLastName(), comment);
	}

	/**
	 * Sends the appropriate message when a WeeklyTimecard has been submitted.
	 *
	 * @param employee The employee whose WeeklyTimecard has been submitted. If
	 *            null, then the submitter is also the employee.
	 * @param proj The project associated with the timecard for Commercial productions.
	 * @param weDate The week-ending date of the timecard.
	 * @param submitter The person submitting the timecard, which may be the
	 *            employee or another Contact.
	 */
	public void timecardSubmitted(Contact employee, Project proj, Date weDate, Contact submitter, String reportFile) {
		log.debug("empl=" +  employee + ", date=" + weDate);
		List<Contact> contacts = makeContactList(submitter);
		if (employee != null && ! submitter.equals(employee)) {
			contacts.add(employee);
		}
		if (employee == null) { // means submitter is the employee
			employee = submitter; // set so we can pull name from one spot.
		}

		if (proj == null) { // probably TV/Feature
			proj = findAndSetProject(employee);
		}
		setProject(proj); // will be used to determine email sender; rev 2.2.4876.

		Notification notification = new Notification(NotificationType.TIMECARD_SUBMITTED, new Date(), proj);
		String msgDate = timecardDateFormat.format(weDate);
		List<String> fileNames = new ArrayList<>(1);
		fileNames.add(reportFile);
		User user = employee.getUser();

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		// Note "false" parameter prevents sending timecard PDF to assistant!
		sendFileToContacts(notification, "notification.timecardSubmitted", fileNames, contacts,
				false, proj, user.getLastNameFirstName(), user.getFirstNameLastName(), msgDate);

	}

	/**
	 * Sends the appropriate message when a Timesheet has been submitted.
	 *
	 * @param proj The project associated with the timesheet for Commercial
	 *            productions.
	 * @param weDate The week-ending date of the timesheet.
	 * @param submitter The person submitting the timesheet.
	 * @param reportFile The fully-qualified filename of a PDF to be included as
	 *            an attachment with the notification email.
	 */
	public void timesheetSubmitted(Project proj, Date weDate, Contact submitter, String reportFile) {
		log.debug("empl=" +  submitter + ", date=" + weDate);
		List<Contact> contacts = makeContactList(submitter);

		if (proj == null) { // probably TV/Feature
			proj = findAndSetProject(submitter);
		}
		setProject(proj); // will be used to determine email sender; rev 2.2.4876.

		Notification notification = new Notification(NotificationType.TIMESHEET_SUBMITTED, new Date(), proj);
		String msgDate = timecardDateFormat.format(weDate);
		List<String> fileNames = new ArrayList<>(1);
		fileNames.add(reportFile);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		// Note "false" parameter prevents sending timesheet PDF to assistant!
		sendFileToContacts(notification, "notification.timesheetSubmitted", fileNames, contacts,
				false, proj, msgDate);

	}

	/**
	 * Sends the appropriate message when a WeeklyTimecard has been voided.
	 *
	 * @param firstName The (timecard's) employee's first name.
	 * @param lastName The (timecard's) employee's last name.
	 * @param weDate The week-ending date of the timecard.
	 * @param recaller The Contact representing the person who did the Void
	 *            action, typically the current Contact.
	 * @param approvers The List of Contact`s to receive the message.
	 */
	public void timecardVoided(String firstName, String lastName, Date weDate, Contact recaller,
			Collection<Contact> approvers, String reason) {
		log.debug("empl=" +  firstName + " " + lastName + ", date=" + weDate);
		if (!isNotifying()) {
			return;
		}
		Project proj = SessionUtils.getCurrentProject();
		Notification notification = new Notification(NotificationType.TIMECARD_VOIDED, new Date(), proj);
		String msgDate = timecardDateFormat.format(weDate);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		sendToAll(notification, "notification.timecardVoided",
				approvers, proj, lastName + ", " + firstName, firstName + " " + lastName, msgDate,
				recaller.getUser().getFirstNameLastName(), reason);
	}

	/**
	 * Sends the appropriate message when a contactDocument has been voided.
	 *
	 * @param firstName The (document's) employee's first name.
	 * @param lastName The (document's) employee's last name.
	 * @param doer The Contact representing the person who did the Void
	 *            action, typically the current Contact.
	 * @param approvers The List of Contact`s to receive the message.
	 */
	public void documentVoided(String firstName, String lastName, Contact doer,
			Collection<Contact> approvers, String reason, String documentType) {
		log.debug("empl=" +  firstName + " " + lastName);
		if (!isNotifying()) {
			return;
		}
		Project proj = SessionUtils.getCurrentProject();
		Notification notification = new Notification(NotificationType.DOCUMENT_VOIDED, new Date(), proj);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		/*if (documentType == null) {
			documentType = "Non-standard";
		}*/
		sendToAll(notification, "notification.documentVoided",
				approvers, proj, lastName + ", " + firstName, firstName + " " + lastName, documentType,
				doer.getUser().getFirstNameLastName(), reason);
	}

	/**
	 * Create a fully-qualified, secure URL pointing to the given relative page
	 * reference, including the given Unit's database id as a URL parameter.
	 *
	 * @param pageRef The page reference, relative to our context root.
	 * @param pUnit The Unit to which the user should be directed when they use
	 *            this URL.
	 * @return The fully-qualified URL.
	 */
	private static String createUnitPath(String pageRef, Unit pUnit) {
		String url = MsgUtils.createBrandedPath(pageRef);
		url = url + "?" + Constants.UNIT_ID_URL_KEY + "=" + pUnit.getId().toString();
		return url;
	}

	/**
	 * Format a message after adding three additional message parameters
	 * preceding the arguments passed in 'args':
	 * <ul>
	 * <li>0 = the current Production name
	 * <li>1 = the supplied Project's title, or the current project's title
	 * <li>2 = the current Unit name, or the default main unit name
	 * </ul>
	 *
	 * @param msgid The message id (String) identifying the message within our
	 *            message resource property file.
	 * @param pProject The project used to supply the project title argument, and
	 *            to determine if the project has multiple Unit`s.  If null, the
	 *            session's current Project is used.
	 * @param args The (optional) list of arguments to pass to the format
	 *            routine.
	 * @return The formatted message, or null if the given message id does
	 *            not exist.  This is sometimes the case for SMS messages.
	 */
	private String formatMessage(String msgid, Project pProject, Object... args) {
		return formatMessage(msgid, getProductionName(pProject), pProject, locale, getUnit(), args);
	}

	/**
	 * Format a message with the given optional message parameters (production,
	 * project, and unit) preceding the arguments passed in 'args'.
	 *
	 * @param msgid The message id (String) identifying the message within our
	 *            message resource property file. A suffix of ".Unit" and/or
	 *            ".Prod" or ".Comm" may be added to this message id. The
	 *            ".Unit" suffix is added if the 'pProject' value is not null
	 *            and that project has more than one unit, and such a message
	 *            exists. The ".Prod" suffix is added for Feature productions
	 *            (non-episodic) if such a message exists. The ".Comm" suffix is
	 *            added for Commercial productions if such a message exists.
	 * @param pProject The project used to supply the project title argument,
	 *            and to determine if the project has multiple Unit`s, and if
	 *            the associated production is episodic. If null, the session's
	 *            current Project is used.
	 * @param locale for default locale(US)
	 * @param args The (optional) list of arguments to pass to the format
	 *            routine following the production name, project name, and unit
	 *            name.
	 * @return The formatted message, or null if the given message id does not
	 *         exist. This is sometimes the case for SMS messages.
	 */
	public static String formatMessage(String msgid, String prodName, Project pProject, Locale locale, Unit unit,
			Object... args) {
		log.debug(msgid);
		if (pProject == null) {
			pProject = SessionUtils.getCurrentProject();
		}
		if (getProduction(pProject) != null) {
			locale = getProduction(pProject).getLocale();
		}
		if (pProject != null && pProject.getHasUnits()) {
			// Unit-qualification of messages is optional - skip if qualified message is not found
			if (MsgUtils.existsMessage(msgid + ".Unit", locale)) {
				msgid += ".Unit";
			}
		}
		if (! getTypeEpisodic(pProject) && MsgUtils.existsMessage(msgid + ".Prod", locale)) {
			msgid += ".Prod";
		}
		if (getTypeCommercial(pProject) && MsgUtils.existsMessage(msgid + ".Comm", locale)) {
			msgid += ".Comm";
		}

		String msg = null;
		if (MsgUtils.existsMessage(msgid, locale)) {
			List<Object> list = new ArrayList<>(Arrays.asList(args));
			list.add(0, prodName);
			list.add(1, (pProject == null ? "" : pProject.getTitle()));
			if (unit != null) {
				list.add(2, unit.getName());
			}
			else {
				list.add(2, Constants.DEFAULT_UNIT_NAME);
			}
			// 0=production; 1=project; 2=unit; others as passed to us
			msg = MsgUtils.formatMessage(msgid, locale, list.toArray());
		}
		return msg;
	}


	/**
	 * Generates the subject and body of a message based on a message id prefix
	 * and optional argument; then sends the message to the specified list of
	 * Contacts. A Notification object will be constructed (by a later routine).
	 *
	 * @param type The NotificationType which should be used to construct a
	 *            Notification object, which will be associated with the
	 *            message.
	 * @param msgIdRoot The prefix of the message id. The messages will be
	 *            obtained from our message resource property file, using this
	 *            prefix plus the suffixes ".Subject", ".Msg", and ".TextMsg",
	 *            and formatted using the supplied arguments, if any.
	 * @param contacts The list of Contacts to receive the message.
	 * @param pProject The project used for a project title and unit
	 *            information. If null, the session's current Project is used.
	 * @param args The (optional) list of arguments passed to the message
	 *            formatting routine to be substituted into the message text.
	 */
	private void sendToAll(NotificationType type, String msgIdRoot, Collection<Contact> contacts,
			Project pProject, Object... args) {
		setProject(pProject);
		Notification notification = new Notification(type, new Date(), getProject());
		sendToAll(notification, msgIdRoot, contacts, pProject, args);
	}

	/**
	 * Generates the subject and body of a message based on a message id prefix
	 * and optional argument; then sends the message to the specified list of
	 * Contacts. The message will be associated with the supplied Notification
	 * object.
	 *
	 * @param note The Notification object to be associated with the message.
	 * @param msgIdRoot The prefix of the message id. The messages will be
	 *            obtained from our message resource property file, using this
	 *            prefix plus the suffixes ".Subject", ".Msg", and ".TextMsg",
	 *            and formatted using the supplied arguments, if any.
	 * @param contacts The list of Contacts to receive the message.
	 * @param pProject The project used for a project title and unit information.
	 *            If null, the session's current Project is used.
	 * @param args The (optional) list of arguments passed to the message
	 *            formatting routine to be substituted into the message text.
	 *            The first of these will be parameter {3} in the message text, as
	 *            {0}-{2} are always production name, project name, and unit name.
	 */
	private void sendToAll(Notification note, String msgIdRoot, Collection<Contact> contacts, Project pProject,
			Object... args) {
		String subject = formatMessage(msgIdRoot + ".Subject", pProject, args);
		String body = formatMessage(msgIdRoot + ".Msg", pProject, args);
		String textMsg = formatMessage(msgIdRoot + ".TextMsg", pProject, args);
		sendToAll(note, contacts, pProject, subject, body, body, textMsg);
	}

	/**
	 * This takes the subject & body strings, and builds three message objects (one for each type),
	 * and creates MessageInstance objects for all the Contacts in the given collection. We also
	 * check the notification preferences for each user, and only generate instances where
	 * appropriate, and if the necessary data (email address or phone number) has been supplied.
	 * This eliminates creating instances which can never be sent anyway.
	 *
	 * @param type The NotificationType to be set in the Notification object which will track all
	 *            the created messages.
	 * @param contacts A collection of Contacts who are to receive the messages. Their individual
	 *            message preference settings will be checked, and appropriate version(s) of the
	 *            message created.
	 * @param subject Subject line; same text is used for all three message styles, although the
	 *            TextMessage sender may ignore the subject.
	 * @param bodyWeb The body of the message to be stored in the application for viewing on the
	 *            Message Center page.
	 * @param bodyEmail The body to be sent via email.
	 * @param bodyTextMsg The body to be sent via text message (SMS); if null, no text messages
	 * 			  are sent.
	 */
//	private void sendToAll( NotificationType type, Collection<Contact> contacts, String subject,
//			String bodyWeb, String bodyEmail, String bodyTextMsg) {
//		log.debug("# of contacts=" + contacts.size() + ", subject=" + subject + "\n msg=" + bodyWeb);
//
//		Notification notification = new Notification(type, new Date(), getProject());
//		sendToAll(notification, contacts, subject, bodyWeb, bodyEmail, bodyTextMsg);
//	}

	/**
	 * This takes the subject & body strings, and builds three message objects (one for each type),
	 * and creates MessageInstance objects for all the Contacts in the given collection. We also
	 * check the notification preferences for each user, and only generate instances where
	 * appropriate, and if the necessary data (email address or phone number) has been supplied.
	 * This eliminates creating instances which can never be sent anyway.
	 *
	 * @param notification The Notification object which will track all the created messages; this
	 *            will be saved in the database, and should be a transient (un-saved) instance.
	 * @param contacts A collection of Contacts who are to receive the messages. Their individual
	 *            message preference settings will be checked, and appropriate version(s) of the
	 *            message created.
	 * @param pProject The project whose id will be appended to the email's "sender"
	 *            field, following the current Production's "email prefix" value.
	 * @param subject Subject line; same text is used for all three message styles, although the
	 *            TextMessage sender may ignore the subject.
	 * @param bodyWeb The body of the message to be stored in the application for viewing on the
	 *            Message Center page.
	 * @param bodyEmail The body to be sent via email.
	 * @param bodyTextMsg The body to be sent via text message (SMS); if null, no text messages
	 * 			  are sent.
	 */
	private void sendToAll( Notification notification, Collection<Contact> contacts, Project pProject,
			String subject, String bodyWeb, String bodyEmail, String bodyTextMsg ) {

		if (contacts.size() == 0) {
			log.warn("** ?? No contacts provided, Notification="+notification.getType() + ", subject=" + subject);
			return;
		}

		if (bodyTextMsg != null) { // check if SMS is enabled
			Production prod = getProduction(pProject);
			if (prod != null && ! prod.getSmsEnabled()) {
				log.debug("SMS disabled, msg suppressed: " + bodyTextMsg);
				bodyTextMsg = null;
			}
		}

		try {
			getNotificationDAO().save(notification);
			bodyEmail = finalizeBody(bodyEmail, pProject); // add header/trailer, etc.

			// Create a Message object for each type of notification, since bodies
			// and senders are different.
			Set<Message> messages = new HashSet<>();

			// Message for delivery via MessageCenter page
			bodyWeb = finalizeWeb(bodyWeb); // add signature, etc.
			Message msgWeb = new Message(NotificationMethod.WEB, "System Alert", subject, bodyWeb, notification);

			// Message for delivery via e-mail
			Message msgEmail = new Message(NotificationMethod.EMAIL, getProdMailSender(pProject), subject, bodyEmail,
					notification);

			// Message for delivery via SMS
			Message msgTextmsg = null;
			if (bodyTextMsg != null) {
				msgTextmsg = new Message(NotificationMethod.TEXT_MESSAGE, "SMS", subject, bodyTextMsg, notification);
			}

			MessageInstance mi = null;
			for (Contact contact : contacts) {
				// Every contact in list gets a web message
				mi = new MessageInstance(msgWeb, contact, new Date(),
						NotificationMethod.WEB, Constants.FALSE, null);
				msgWeb.getMessageInstances().add(mi);

				if (ignoreEmailPref || contact.getNotifyByEmail()) {
					// if they normally want an email, or it's a password-reset request
					String email = contact.getEmailAddress();
					if (email != null && EmailValidator.isValidEmail(email)) {
						mi = new MessageInstance(msgEmail, contact, new Date(),
								NotificationMethod.EMAIL, Constants.FALSE, null);
						msgEmail.getMessageInstances().add(mi);
					}
				}
				if (contact.getNotifyByTextMsg() && msgTextmsg != null) {
					// if they want a text message...
					String phone = contact.getCellPhone();
					if (phone != null && PhoneNumberValidator.isValid(phone)) {
						mi = new MessageInstance(msgTextmsg, contact, new Date(), NotificationMethod.TEXT_MESSAGE,
								Constants.FALSE, null);
						msgTextmsg.getMessageInstances().add(mi);
					}
				}
				if ( (contact.getNotifyByAsstEmail() || contact.getNotifyByAsstTextMsg()) &&
						contact.getAssistant() != null) {
					// assistant notifications
					Contact asst = contact.getAssistant();
					if (contact.getNotifyByAsstEmail()) { // send email to assistant
						String email = asst.getEmailAddress();
						if (email != null && EmailValidator.isValidEmail(email)) {
							mi = new MessageInstance(msgEmail, asst, new Date(),
									NotificationMethod.EMAIL, Constants.FALSE, null);
							msgEmail.getMessageInstances().add(mi);
						}
					}
					if (contact.getNotifyByAsstTextMsg() && msgTextmsg != null) { // send text msg to assistant ...
						String phone = asst.getCellPhone();
						if (phone != null && PhoneNumberValidator.isValid(phone)) {
							mi = new MessageInstance(msgTextmsg, asst, new Date(), NotificationMethod.TEXT_MESSAGE,
									Constants.FALSE, null);
							msgTextmsg.getMessageInstances().add(mi);
						}
					}
				}
			}
			// Save the Message objects; the MessageInstance objects are saved automatically via cascade
			getMessageDAO().save(msgWeb);
			messages.add(msgWeb);

			if (msgEmail.getMessageInstances().size() > 0) {
				getMessageDAO().save(msgEmail);
				messages.add(msgEmail);
			}
			else {
				msgEmail = null;
			}

			if (msgTextmsg != null && msgTextmsg.getMessageInstances().size() > 0) {
				getMessageDAO().save(msgTextmsg);
				messages.add(msgTextmsg);
			}
			else {
				msgTextmsg = null;
			}

			notification.setMessages(messages);
			getNotificationDAO().attachDirty(notification);

			executeMessages(messages); // do actually send of email & SMS
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Send an email to a single User, and do not send any equivalent text
	 * message (SMS). Note that the email is always sent, regardless of the
	 * user's preference setting.
	 *
	 * @param notification The notification object, which will be saved in the
	 *            database.
	 * @param user The User who is to receive the notification.
	 * @param msgIdRoot The root of the message id; the usual suffixes will be
	 *            added to find the actual message text (.subject, .msg, etc.)
	 * @param pProject The relevant Project, whose name may be used in the
	 *            message.
	 * @param fileNames An optional List of file names, representing files to be
	 *            attached to the email. If 'null', no files are attached.
	 * @param args Any additional arguments to pass to the message formatting
	 *            routines.
	 */
	private void sendToUser( Notification notification, User user,
			String msgIdRoot, Project pProject, Collection<String> fileNames, Object... args ) {
		String subject;
		String bodyEmail;

		if (user != null && user.getShowCanada()) {
			locale = Constants.LOCALE_FRENCH_CANADA;
		}
		else {
			locale = Constants.LOCALE_US;
		}

		subject = formatMessage(msgIdRoot + ".Subject", pProject, args);
		bodyEmail = formatMessage(msgIdRoot + ".Msg", pProject, args);
//		String bodyTextMsg = formatMessage(msgIdRoot + ".TextMsg", pProject, args);

		if (user == null) {
			log.warn("** ?? No user provided, Notification="+notification.getType() + ", subject=" + subject);
			return;
		}
		try {
			getNotificationDAO().save(notification);

			bodyEmail = finalizeBody(bodyEmail, pProject); // add header/trailer, etc.

			// Create a Message object for each type of notification, since bodies
			// and senders are different.
			Set<Message> messages = new HashSet<>();

			// Message for delivery via e-mail
			Message msgEmail = new Message(NotificationMethod.EMAIL, getProdMailSender(), subject, bodyEmail,
					notification);
			if (fileNames != null) {
				msgEmail.setFileName(StringUtils.collectionToCommaDelimitedString(fileNames));
			}

			// Message for delivery via SMS
//			Message msgTextmsg = null;
//			if (bodyTextMsg != null) {
//				msgTextmsg = new Message(NotificationMethod.TEXT_MESSAGE, "SMS", subject, bodyTextMsg, notification);
//			}

			MessageInstance mi = null;
			String email = user.getEmailAddress();
			if (email != null && EmailValidator.isValidEmail(email)) {
				mi = new MessageInstance(msgEmail, user, null, new Date(),
						NotificationMethod.EMAIL);
				msgEmail.getMessageInstances().add(mi);
			}

			if (msgEmail.getMessageInstances().size() > 0) {
				getMessageDAO().save(msgEmail);
				messages.add(msgEmail);
			}
			else {
				msgEmail = null;
			}

//			if (msgTextmsg != null && msgTextmsg.getMessageInstances().size() > 0) {
//				getMessageDAO().save(msgTextmsg);
//				messages.add(msgTextmsg);
//			}
//			else {
//				msgTextmsg = null;
//			}

			notification.setMessages(messages);
			getNotificationDAO().attachDirty(notification);

			executeMessages(messages); // do actually send of email & SMS
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Send a message and optional accompanying files to a list of Contacts.
	 * Messages are sent if the user has the preference set to accept email
	 * notifications, or if {@link #ignoreEmailPref} is true.
	 *
	 * @param notification The notification object, which will be saved in the
	 *            database.
	 * @param msgIdRoot The root of the message id; the usual suffixes will be
	 *            added to find the actual message text (.subject, .msg, etc.)
	 * @param fileNames An optional List of file names, representing files to be
	 *            attached to the email. If 'null', no files are attached.
	 * @param contacts A list of Contacts to receive the message.
	 * @param includeAssistant If true, each Contact will be checked to see if
	 *            the contact.notifyByAsstEmail flag is true, and if there is a
	 *            contact.assistant assigned, in which case a duplicate email
	 *            will be sent to the assistant (if that contact has a valid
	 *            email address).
	 * @param pProject The relevant Project, whose name may be used in the
	 *            message.
	 * @param args Any additional arguments to pass to the message formatting
	 *            routines.
	 */
	private void sendFileToContacts(Notification notification, String msgIdRoot, Collection<String> fileNames,
			Collection<Contact> contacts, boolean includeAssistant, Project pProject,
			Object... args) {
		String subject = formatMessage(msgIdRoot + ".Subject", pProject, args);
		String body = formatMessage(msgIdRoot + ".Msg", pProject, args);
		sendFileToAll(notification, contacts, includeAssistant, null, subject, body, fileNames, null);
	}

	/**
	 * Send a message and optional accompanying files to a list of email
	 * addresses. The email addresses do not need to be associated with a
	 * LightSpeed user account.
	 *
	 * @param notification The notification object, which will be saved in the
	 *            database.
	 * @param msgIdRoot The root of the message id; the usual suffixes will be
	 *            added to find the actual message text (.subject, .msg, etc.)
	 * @param fileNames An optional List of file names, representing files to be
	 *            attached to the email. If 'null', no files are attached.
	 * @param addresses A list of email addresses (as Strings) to which the
	 *            message (and optional files) should be sent.
	 * @param pProject The relevant Project, whose name may be used in the
	 *            message.
	 * @param args Any additional arguments to pass to the message formatting
	 *            routines.
	 */
	private void sendFileToEmails(Notification notification, String msgIdRoot,
			Collection<String> fileNames, Collection<String> addresses, Project pProject,
			Object... args) {
		String subject = formatMessage(msgIdRoot + ".Subject", pProject, args);
		String body = formatMessage(msgIdRoot + ".Msg", pProject, args);
		sendFileToAll(notification, null, false, addresses, subject, body, fileNames, null);
	}

	/**
	 * Send a message and optional accompanying files to a list of Contacts.
	 * Messages are sent if the user has the preference set to accept email
	 * notifications, or if {@link #ignoreEmailPref} is true.
	 *
	 * @param notification The notification object, which will be saved in the
	 *            database.
	 * @param contacts An optional list of Contacts to receive the message (and
	 *            optional files); may be null.
	 * @param includeAssistant If true, each Contact will be checked to see if
	 *            the contact.notifyByAsstEmail flag is true, and if there is a
	 *            contact.assistant assigned, in which case a duplicate email
	 *            will be sent to the assistant (if that contact has a valid
	 *            email address).
	 * @param addresses An optional list of email addresses (as Strings) to
	 *            which the message (and optional files) should be sent; may be
	 *            null.
	 * @param subject The subject line of the email.
	 * @param bodyEmail The body text of the email.
	 * @param fileNames An optional List of file names, representing files to be
	 *            attached to the email. If 'null', no files are attached.
	 * @param wm An optional Watermark object. If this is not null, an
	 *            individual watermark will be added to each of the files
	 *            distributed. That is, when the files are sent, each file will
	 *            be marked with the name or email address of the recipient of
	 *            the file.
	 */
	private void sendFileToAll(Notification notification, Collection<Contact> contacts,
			boolean includeAssistant, Collection<String> addresses, String subject,
			String bodyEmail, Collection<String> fileNames, WaterMark wm) {

		if (contacts != null && contacts.size() == 0) {
			log.warn("** ?? No contacts provided, Notification="+notification.getType() + ", subject=" + subject);
			return;
		}
		else if (addresses != null && addresses.size() == 0) {
			log.warn("** ?? No addresses provided, Notification="+notification.getType() + ", subject=" + subject);
			return;
		}
		try {
			getNotificationDAO().save(notification);
			bodyEmail = finalizeBody(bodyEmail, getProject()); // add header/trailer, etc.

			// Create a Message object for each type of notification, since bodies
			// and senders are different.
			Set<Message> messages = new HashSet<>();

			// Message for delivery via e-mail
			Message msgEmail = new Message(NotificationMethod.EMAIL, getProdMailSender(), subject, bodyEmail, notification);
			msgEmail.setFileName(StringUtils.collectionToCommaDelimitedString(fileNames));

			if (msgEmail.getFileName().length() >= 20000) {
				String files = msgEmail.getFileName().substring(0, 19990) + "...";
				msgEmail.setFileName(files);
			}

			if (contacts != null) {
				createMsgInstances(msgEmail, contacts, includeAssistant);
			}
			if (addresses != null) {
				createEmailMsgInstances(msgEmail, addresses);
			}

			if (msgEmail.getMessageInstances().size() > 0) {
				// Save the Message object; the MessageInstance objects are saved automatically via cascade
				getMessageDAO().save(msgEmail);
				messages.add(msgEmail);
			}
			else {
				msgEmail = null;
			}

			notification.setMessages(messages);
			getNotificationDAO().attachDirty(notification);

			executeMessages(messages, wm); // do actually send of email & SMS
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Generate the MessageInstance instances corresponding to the supplied list
	 * of Contacts. Upon return, the msgEmail object will have been updated to
	 * include the necessary MessageInstance objects.
	 *
	 * @param msgEmail The Message object to which the MessageInstance entries
	 *            should be added.
	 * @param contacts The (non-null, but possibly empty) list of Contacts to
	 *            receive the message.
	 * @param includeAssistant If true, each Contact will be checked to see if
	 *            the contact.notifyByAsstEmail flag is true, and if there is a
	 *            contact.assistant assigned, in which case a duplicate email
	 *            will be sent to the assistant (if that contact has a valid
	 *            email address).
	 */
	public void createMsgInstances(Message msgEmail, Collection<Contact> contacts,
			boolean includeAssistant) {
		MessageInstance mi;
		for (Contact contact : contacts) {
			if (ignoreEmailPref || contact.getNotifyByEmail()) {
				// if they want an email...
				String email = contact.getEmailAddress();
				if (email != null && EmailValidator.isValidEmail(email)) {
					mi = new MessageInstance(msgEmail, contact, new Date(),
							NotificationMethod.EMAIL, Constants.FALSE, null);
					msgEmail.getMessageInstances().add(mi);
				}
			}
			if ( includeAssistant && contact.getNotifyByAsstEmail() &&
					contact.getAssistant() != null) {
				// assistant notifications
				Contact asst = contact.getAssistant();
				String email = asst.getEmailAddress();
				if (email != null && EmailValidator.isValidEmail(email)) {
					mi = new MessageInstance(msgEmail, asst, new Date(),
							NotificationMethod.EMAIL, Constants.FALSE, null);
					msgEmail.getMessageInstances().add(mi);
				}
			}
		}
	}

	/**
	 * Generate the MessageInstance instances corresponding to the supplied list
	 * of addresses. Upon return, the msgEmail object will have been updated to
	 * include the necessary MessageInstance objects.
	 *
	 * @param msgEmail The Message object to which the MessageInstance entries
	 *            should be added.
	 * @param addresses A (non-null, but possibly empty) list of email addresses
	 *            (as Strings) to which the message (and optional files) should
	 *            be sent.
	 */
	private void createEmailMsgInstances(Message msgEmail, Collection<String> addresses) {
		MessageInstance mi;
		for (String email : addresses) {
			if (email != null && EmailValidator.isValidEmail(email)) {
				mi = new MessageInstance(msgEmail, email, new Date(),
						NotificationMethod.EMAIL);
				msgEmail.getMessageInstances().add(mi);
			}
		}
	}

	/**
	 * Add default header and trailer to email body text, and insert any common
	 * sections, such as a signature, indicated by specific text in brackets,
	 * e.g., '[SIG1]' for the default signature and logo.
	 *
	 * @param bodyEmail
	 * @return The updated email body text.
	 */
	/*package*/ static String finalizeBody(String bodyEmail, Project proj) {

		if (bodyEmail.contains("[")) {
			Production prod = getProduction(proj);

			// Standard signature(s)
			int ix = bodyEmail.indexOf("[SIG1]");
			String companyName = Constants.LS_COMPANY_NAME;
			String homeSiteUrl = Constants.LS_HOME_URL;

			if (SessionUtils.isTTCProd() || SessionUtils.isTTCOnline()) {
				companyName = Constants.TTC_ONLINE_COMPANY_NAME;
				homeSiteUrl = Constants.TEAM_HOME_URL;
			}
			if (ix >= 0) {
				bodyEmail = bodyEmail.substring(0, ix) +
						MsgUtils.formatMessage("notification.common.signature1", companyName, homeSiteUrl, homeSiteUrl) +
						bodyEmail.substring(ix+6);
			}
			else if ((ix = bodyEmail.indexOf("[SIG2]")) >= 0) {
				bodyEmail =
						bodyEmail.substring(0, ix) +
								MsgUtils.formatMessage("notification.common.signature2",
										companyName, homeSiteUrl, homeSiteUrl) +
								bodyEmail.substring(ix + 6);
			}
			else if ((ix = bodyEmail.indexOf("[SIGU]")) >= 0) {
				User user = SessionUtils.getCurrentUser();
				String sig;
				if (user != null) {
					sig = MsgUtils.formatMessage("notification.common.signatureUser",
							user.getFirstNameLastName(), user.getEmailAddress());
				}
				else {
					sig = MsgUtils.formatMessage("notification.common.signatureUser",
							"unknown", "(no email)");
				}
				bodyEmail = bodyEmail.substring(0, ix) + sig +
						bodyEmail.substring(ix+6);
			}

			// Custom production text #1
			if ((ix = bodyEmail.indexOf("[PRD1]")) >= 0) {
				String text = prod.getCustomText1();
				bodyEmail = bodyEmail.substring(0, ix) +
						(text == null ? "" : text) + bodyEmail.substring(ix+6);
			}
//			// Custom production text #2
			// LS-4219: don't use PRD2 for emails; used on My Starts page instead.
//			if ((ix = bodyEmail.indexOf("[PRD2]")) >= 0) {
//				String text = prod.getCustomText2();
//				bodyEmail = bodyEmail.substring(0, ix) +
//						(text == null ? "" : text) + bodyEmail.substring(ix+6);
//			}

			// Standard header(s)
			if ((ix = bodyEmail.indexOf("[HDR1]")) >= 0) {
				bodyEmail = bodyEmail.substring(0, ix) +
						MsgUtils.getMessage("notification.common.header1") +
						bodyEmail.substring(ix+6);
			}
			if ((ix = bodyEmail.indexOf("[HDR1CA]")) >= 0) {
				bodyEmail =
						bodyEmail.substring(0, ix) +
								MsgUtils.getMessage("notification.common.header1",
										Constants.LOCALE_FRENCH_CANADA) +
								bodyEmail.substring(ix + 8);
			}
			if ((ix = bodyEmail.indexOf("[SIG1CA]")) >= 0) {
				bodyEmail = bodyEmail.substring(0, ix) +
						MsgUtils.formatMessage("notification.common.signature1", Constants.LOCALE_FRENCH_CANADA,
								companyName, homeSiteUrl, homeSiteUrl) +
						bodyEmail.substring(ix + 8);
			}
			if ((ix = bodyEmail.indexOf("[SIG2CA]")) >= 0) {
				bodyEmail = bodyEmail.substring(0, ix) + MsgUtils.formatMessage(
						"notification.common.signature2", Constants.LOCALE_FRENCH_CANADA,
						companyName, homeSiteUrl, homeSiteUrl) + bodyEmail.substring(ix + 8);
			}
			if ((ix = bodyEmail.indexOf("[SIGUCA]")) >= 0) {
				bodyEmail = bodyEmail.substring(0, ix) + MsgUtils.formatMessage(
						"notification.common.signatureUser", Constants.LOCALE_FRENCH_CANADA,
						companyName, homeSiteUrl, homeSiteUrl) + bodyEmail.substring(ix + 8);
			}
		}
		bodyEmail = Constants.EMAIL_STD_HEADER + bodyEmail + Constants.EMAIL_STD_TRAILER;

		if (bodyEmail.length() > Message.MAX_MESSAGE_BODY_LENGTH) {
			String emsg = "Email body too long (" + bodyEmail.length() + "); truncated.";
			EventUtils.logEvent(EventType.DATA_ERROR, emsg);
			log.error(emsg);
			bodyEmail = bodyEmail.substring(0, Message.MAX_MESSAGE_BODY_LENGTH-20) +
					"... [truncated]";
		}
		return bodyEmail;
	}

	/**
	 * Finalize the WEB notification body of a message. Inserts any common
	 * sections, such as a signature, indicated by specific text in brackets,
	 * e.g., '[SIG1]' for the default signature.
	 *
	 * @param body The body of the message to be displayed in the online version.
	 * @return The updated web body text.
	 */
	private String finalizeWeb(String body) {

		if (body.contains("[")) {
			int ix = body.indexOf("[SIG1]");
			if (ix >= 0) {
				body = body.substring(0, ix) +
						MsgUtils.getMessage("notification.common.signature1.web") +
						body.substring(ix+6);
			}
			else if ((ix = body.indexOf("[SIG2]")) >= 0) {
				body = body.substring(0, ix) +
						MsgUtils.getMessage("notification.common.signature2.web") +
						body.substring(ix+6);
			}
			else if ((ix = body.indexOf("[SIGU]")) >= 0) {
				User user = SessionUtils.getCurrentUser();
				String sig = MsgUtils.formatMessage("notification.common.signatureUser.web",
						user.getFirstNameLastName(), user.getEmailAddress());
				body = body.substring(0, ix) + sig +
						body.substring(ix+6);
			}
			if ((ix = body.indexOf("[HDR1]")) >= 0) {
				body = body.substring(0, ix) +
						MsgUtils.getMessage("notification.common.header1.web") +
						body.substring(ix+6);
			}
		}
		return body;
	}

	/**
	 * Actually schedule the messages to be sent!!
	 */
	public void executeMessages(Set<Message> messages) {
		executeMessages(messages, null);
	}

	/**
	 * Actually schedule the messages to be sent!!
	 * The given WaterMark will be applied if it is not null and the
	 * messages have any attached PDF files.
	 */
	private void executeMessages(Set<Message> messages, WaterMark wm) {
		if (messages.size() == 0) {
			// nothing to do. May happen if no valid email addresses found.
			return;
		}
		// refresh all User instances to prevent LazyInitializationExceptions in ExecuteMessage
		// (watermark code got a LazyInitExc - rev 2795/#787; refresh moved rev 2909)
		UserDAO userDAO = UserDAO.getInstance();
		for (Message m : messages) {
			for (MessageInstance mi : m.getMessageInstances()) {
				mi.setUser(userDAO.refresh(mi.getUser()));
			}
		}
		Mailer mailer = Mailer.getInstance();
		getTaskExecutor().execute(new ExecuteMessage(messages, mailer, wm));
	}

	/**
	 * Create a String showing the names and addresses of one or more Locations.
	 *
	 * @param locationIds A Collection of database ids associated with the
	 *            RealWorldElements which are the locations whose information is
	 *            to be returned.
	 * @return A String consisting of the names and addresses of the supplied locations,
	 *            formatted using HTML tags.
	 */
	private String createLocationList(Collection<Integer> locationIds) {
		String locations = "";
		RealWorldElement location;
		RealWorldElementDAO rwDAO = RealWorldElementDAO.getInstance();
		for (Integer id : locationIds) {
			location = rwDAO.findById(id);
			if (location != null) {
				locations += location.getName()
					+ (location.getAddress()==null ? "" : "<br/>" + location.getAddress().getCompleteAddress())
					+ "<p/>";
			}
		}
		return locations;
	}

	/**
	 * Create a List containing the one Contact given. If the given Contact is
	 * null, it is NOT added to the List, and an empty List is returned.
	 */
	private List<Contact> makeContactList(Contact contact) {
		List<Contact> contactList = new ArrayList<>(1);
		if (contact != null) {
			contactList.add(contact);
		}
		return contactList;
	}

	/**
	 * Sends the appropriate message to an approver when a Contact Document is
	 * ready for approval.
	 *
	 * @param employee The employee whose Contact Document has been added to the
	 *            approver's queue.
	 * @param approver The Contact to whose queue the contact document has been added.
	 */
	/*public void documentReady(Contact employee, Contact approver) {
		log.debug("empl=" +  employee);

		if (! approver.getNotifyForApproval()) {
			return;
		}

		Project proj = findAndSetProject(employee);

		if (!isNotifying()) {
			return;
		}
		List<Contact> contacts = makeContactList(approver);

		Notification notification = new Notification(NotificationType.DOCUMENT_READY, new Date(), proj);
		sendToAll(notification, "notification.documentReady",
				contacts, proj, employee.getUser().getLastNameFirstName(),
				employee.getUser().getFirstNameLastName());
	}*/

	/**
	 * Sends the appropriate message(s) when a Contact Document has been rejected.
	 *
	 * @param firstName The (contact document's) contact's first name.
	 * @param lastName The (contact document's) contact's last name.
	 * @param rejector The Contact of the person doing the Reject operation.
	 * @param receiver The Contact to whom the timecard was sent back -- the
	 *            person who must either resubmit or reapprove the rejected
	 *            timecard.
	 * @param approvers The List of Contact`s representing approvers who should
	 *            be notified of the rejection action.
	 * @param comment The reason text entered by the person doing the reject;
	 *            may be null.
	 * @param docName The Name of the rejected document/form.
	 */
	public void documentRejected(String firstName, String lastName, Contact rejector, Contact receiver,
			Collection<Contact> approvers, String comment, String docName) {
		log.debug("empl=" + firstName + " " + lastName);
		if (!isNotifying()) {
			return;
		}
		Project proj = SessionUtils.getCurrentProject();
		Notification notification = new Notification(NotificationType.DOCUMENT_REJECTED, new Date(), proj);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		if (approvers.size() > 0) {
			sendToAll(notification, "notification.documentRejected",
					approvers, proj, lastName + ", " + firstName, firstName + " " + lastName,
					rejector.getUser().getFirstNameLastName(), comment, docName, receiver.getUser().getFirstNameLastName());
		}

		/*sendToUser(notification, receiver.getUser(), "notification.documentRejectedToYou",
				proj, null, lastName + ", " + firstName, firstName + " " + lastName,
				rejector.getUser().getFirstNameLastName(), comment);*/
	}

	/**
	 * Sends the appropriate message when a Document has been recalled.
	 *
	 * @param proj The project associated with the document being recalled.
	 * @param firstName The (document's) employee's first name.
	 * @param lastName The (document's) employee's last name.
	 * @param documentType The type of the document.
	 * @param recaller The Contact representing the person who did the Recall
	 *            action, typically the current Contact.
	 * @param approvers The List of Contact`s to receive the message, which is
	 *            everyone who has approved the document, after the person who
	 *            did the recall, and also the current approver (before it was
	 *            recalled).
	 */
	public void documentRecalled(Project proj, String firstName, String lastName, String documentType, Contact recaller,
			Collection<Contact> approvers) {
		log.debug("empl=" +  firstName + " " + lastName + ", document type=" + documentType);
		setProject(proj);
		if (!isNotifying()) {
			return;
		}
		Notification notification = new Notification(NotificationType.DOCUMENT_RECALLED, new Date(), proj);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		sendToAll(notification, "notification.documentRecalled",
				approvers, proj, lastName + ", " + firstName, firstName + " " + lastName, documentType,
				recaller.getUser().getFirstNameLastName());
	}

	/**
	 * Sends the appropriate message when a Document has been pulled.
	 *
	 * @param firstName The (document's) employee's first name.
	 * @param lastName The (document's) employee's last name.
	 * @param documentType The type of the document.
	 * @param puller The Contact representing the person who did the Pull
	 *            action, typically the current Contact.
	 * @param oldApproverName The display name (first last) of the approver from
	 *            whose queue the document was removed. This will be used in the
	 *            notification text.
	 * @param approvers The List of Contact`s to receive the message, which is
	 *            the person from whose queue it was removed, and all following
	 *            approvers, up to, but not including, the approver doing the
	 *            Pull.
	 */
	public void documentPulled(String firstName, String lastName, String documentType, Contact puller,
			String oldApproverName, List<Contact> approvers) {
		log.debug("empl=" +  firstName + " " + lastName + ", documentType=" + documentType);
		if (!isNotifying()) {
			return;
		}
		Project proj = SessionUtils.getCurrentProject();
		Notification notification = new Notification(NotificationType.DOCUMENT_PULLED, new Date(), proj);

		ignoreEmailPref = true; // always send - user may not "opt out" of this.

		sendToAll(notification, "notification.documentPulled",
				approvers, proj, lastName + ", " + firstName, firstName + " " + lastName, documentType,
				puller.getUser().getFirstNameLastName(), oldApproverName);
	}

	/**
	 * @return
	 * true iff notifications should be sent out for the current Project.
	 */
	private boolean isNotifying() {
		return (isNotifying(getProject()));
	}

	/**
	 * @param pProject
	 * @return
	 * true iff notifications should be sent out for the given Project.
	 */
	private static boolean isNotifying(Project pProject) {
		return (isActive(pProject) && pProject.getNotifying() &&
				pProject.getProduction().getNotify());
	}

	/**
	 * @return
	 * true iff the current project & current production are in ACTIVE status.
	 */
	private boolean isActive() {
		return isActive(getProject());
	}

	/**
	 * @param pProject
	 * @return
	 * true iff the given project & its production are in ACTIVE status,
	 * and this is an online (not Offline) system.
	 */
	private static boolean isActive(Project pProject) {
		return (pProject.getProduction().getStatus() == AccessStatus.ACTIVE &&
				pProject.getStatus() == AccessStatus.ACTIVE &&
				! ApplicationUtils.isOffline());
	}

	/**
	 * @return The string to use as the "sender" for generated email.
	 */
	private String getProdMailSender() {
		return getProdMailSender(getProject());
	}

	/**
	 * @return The string to use as the "sender" for generated email.
	 */
	/* package */ static String getProdMailSender(Project project) {
		String s;
		Production prod;
		if (project != null) {
			prod = project.getProduction();
		}
		else {
			prod = SessionUtils.getProduction();
		}
		if (prod != null) {
			if (prod.getEmailSender() != null && prod.getEmailSender().trim().length() > 0) {
				s = prod.getEmailSender();
			}
			else {
				s = Constants.DEFAULT_EMAIL_SENDER; // Bug#727
//				s = prod.getProdId();
			}
//			if (prod.getType().getEpisodic() && project != null) {
//				s += "." + project.getCode();
//			}
		}
		else {
			s = "system";
		}
		s += "@" + SessionUtils.getString(Constants.ATTR_EMAIL_SENDING_DOMAIN, Constants.TTC_ONLINE_DOMAIN);
		return s;
	}

	/**
	 * @param contact The contact instance to use to find a relevant Project if
	 *            there is no "current project".
	 * @return A Project to use for message information; this will be the
	 *         "current project" if there is one, or the default Project for the
	 *         Production associated with the given Contact.
	 */
	private Project findAndSetProject(Contact contact) {
		Project proj = SessionUtils.getCurrentProject();
		if (proj == null || proj.getProduction().isSystemProduction()) {
			proj = contact.getProduction().getDefaultProject();
		}
		setProject(proj); // will be used to determine email sender; rev 2.2.4876.
		return proj;
	}

	/** See {@link #project}.  If {@link #project} is null,
	 * sets it to the current session's project. */
	public Project getProject() {
		if (project == null) {
			project = SessionUtils.getCurrentProject();
		}
		else {
			project = ProjectDAO.getInstance().refresh(project);
		}
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

//	/** See {@link #unit}. */
//	public Unit getUnit() {
//		if (unit == null) {
//			unit = getProject().getUnit();
//		}
//		return unit;
//	}
//	/** See {@link #unit}. */
//	public void setUnit(Unit unit) {
//		this.unit = unit;
//	}

	/** See {@link #unit}. */
	public Unit getUnit() {
		return unit;
	}
	/** See {@link #unit}. */
	public void setUnit(Unit u) {
		unit = u;
	}

	/**
	 * Returns the Production associated with the current notification.
	 *
	 * @param proj The Project associated with the current notification, or null
	 *            if not known.
	 * @return the Production for the current notification.
	 */
	private static Production getProduction(Project proj) {
		Production prod = null;
		if (proj != null) {
			prod = proj.getProduction();
		}
		else {
			prod = SessionUtils.getProduction();
		}
		return prod;
	}

	/**
	 * Returns the name of the Production associated with the current
	 * notification.
	 *
	 * @param proj The Project associated with the current notification, or null
	 *            if not known.
	 * @return the name of the Production for the current notification.
	 */
	private static String getProductionName(Project proj) {
		Production prod = getProduction(proj);
		if (prod == null) {
			return null;
		}
		return prod.getTitle();
	}

	/**
	 * Determine if the project being handled is part of an episodic Production
	 * (can have multiple "projects").
	 *
	 * @param pProject The Project associated with the current notification, or
	 *            null if not known.
	 * @return True iff the project given belongs to an episodic Production.
	 */
	private static boolean getTypeEpisodic(Project pProject) {
		try {
			if (pProject != null && pProject.getProduction().getType().getEpisodic()) {
				return true;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return false;
	}

	/**
	 * Determine if the project being handled is part of a Commercial Production.
	 *
	 * @param pProject The Project associated with the current notification, or
	 *            null if not known.
	 * @return True iff the project given belongs to a Commercial Production.
	 */
	private static boolean getTypeCommercial(Project pProject) {
		try {
			if (pProject != null && pProject.getProduction().getType().isAicp()) {
				return true;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return false;
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

	/** See {@link #notificationDAO}. */
	public NotificationDAO getNotificationDAO() {
		if (notificationDAO == null) {
			notificationDAO = NotificationDAO.getInstance();
		}
		return notificationDAO;
	}
	/** See {@link #notificationDAO}. */
	public void setNotificationDAO(NotificationDAO notificationDAO) {
		this.notificationDAO = notificationDAO;
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

	/**
	 * Holds the I9Messages for an approver, to send them to the approver in a single mail.
	 * It holds the information for one line in the output table
	 */
	public static class I9Message implements Serializable {
		/** */
		private static final long serialVersionUID = 806713733079908947L;

		/** First name of owner of the I9 document. */
		private String firstName;
		/** Last name of owner of the I9 document. */
		private String lastName;
		/** Occupation of owner of the I9 document. */
		private String occupation;
		/** The number of days left to approve the I9 document. */
		private Integer daysLeft;
		/** The name of project to which the current I9 Document belongs, for commercial Production. */
		private String projectName;

//		private static final String SORTKEY_PROJECT_NAME = "projectName";

		public I9Message(String firstName, String lastName, String occupation, Integer daysLeft, String projectName) {
			super();
			this.firstName = firstName;
			this.lastName = lastName;
			this.occupation = occupation;
			this.daysLeft = daysLeft;
			this.projectName = projectName;
		}

		/** See {@link #firstName}. */
		public String getFirstName() {
			return firstName;
		}
		/** See {@link #firstName}. */
		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		/** See {@link #lastName}. */
		public String getLastName() {
			return lastName;
		}
		/** See {@link #lastName}. */
		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		/** See {@link #occupation}. */
		public String getOccupation() {
			return occupation;
		}
		/** See {@link #occupation}. */
		public void setOccupation(String occupation) {
			this.occupation = occupation;
		}

		/** See {@link #daysLeft}. */
		public Integer getDaysLeft() {
			return daysLeft;
		}
		/** See {@link #daysLeft}. */
		public void setDaysLeft(Integer daysLeft) {
			this.daysLeft = daysLeft;
		}

		/** See {@link #projectName}. */
		public String getProjectName() {
			return projectName;
		}
		/** See {@link #projectName}. */
		public void setProjectName(String projectName) {
			this.projectName = projectName;
		}

		public int compareTo(I9Message other) {
			int ret = 0;
			if (other == null) {
				return 1;
			}
			if (ret == 0) {
				ret = compareProject(other);
				if (ret == 0) {
					ret = compareI9Message(other);
				}
			}
			return ret;
		}

		/** Method to compare I9Messages.*/
		public int compareI9Message(I9Message other) {
			int ret = 0;
			if (other == null) {
				return 1;
			}
			if (ret == 0) {
				ret = getDaysLeft().compareTo(other.getDaysLeft());
			}
			if (ret == 0) {
				ret = getLastName().compareTo(other.getLastName());
			}
			if (ret == 0) {
				ret = getFirstName().compareTo(other.getFirstName());
			}
			return ret;
		}

		/** Method to compare projects of the I9Messages.*/
		private int compareProject(I9Message other) {
			int ret = 0;
			ret = getProjectName().compareToIgnoreCase(other.getProjectName());
			return ret;
		}

		/**
		 * Instantiate a I9Message comparator, used for sorting the list of
		 * I9Messages for the email body.
		 */
		public static Comparator<I9Message> getI9Comparator() {
			Comparator<I9Message> comparator = new Comparator<I9Message>() {
				@Override
				public int compare(I9Message one, I9Message two) {
					return one.compareTo(two);
				}
			};
			return comparator;
		}
	}

}
