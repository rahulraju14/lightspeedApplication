package com.lightspeedeps.batch;

import java.util.*;

import javax.faces.bean.*;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.ReportStatusItem;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.*;
import com.lightspeedeps.util.project.*;

/**
 * This class generates notices of reports that are "due" or "overdue", based on the
 * ReportRequirements table entries, the current date, and the status of existing report objects
 * such as DPR, CallSheet, ExhibitG, and TimeSheet. It is normally run in "batch" mode via the
 * Quartz scheduler; the scheduling parameters, and property values for class fields, are set in
 * applicationContextGeneric.xml.
 * <p>
 * The method to invoke to start the process is projectBasedNotifications().
 * <p>
 * The methods in this class determine which notices should be generated, and to whom; it invokes
 * methods of the DoNotification class to actually issue the notifications.
 * <p>
 * The functionality may be tested as a JUnit using {@link com.lightspeedeps.test.report.OverdueReportTest}.
 */
@ManagedBean
@RequestScoped
public class DueOverdueCheck extends SpringBatch {

	private static final Log log = LogFactory.getLog(DueOverdueCheck.class);


	// Return codes from the various methods that determine due/overdue report status.
	private final static int STATUS_OK = 0;
	private final static int STATUS_DUE = 1;
	private final static int STATUS_OVERDUE = 2;

	private List<Project> projectList ;
	private Project project;
	private Unit unit;
	private ScheduleUtils schedule;
	private DoNotification notifier;

	private transient CallsheetDAO callsheetDAO;
	private transient DprDAO dprDAO;
	private transient ExhibitGDAO exhibitGDAO;
	private transient NotificationDAO notificationDAO;
	private transient ProjectDAO projectDAO;

	/** now - date & time */
	private Date currentDateTime;
	/** today's date only, time=12:00am */
	private Date currentDate;

	/**
	 * Constructor.  Note that this bean is constructed once at application start-up, and
	 * re-used each time it is scheduled to run.
	 */
	public DueOverdueCheck() {
		log.debug("");
	}

	@Override
	protected void setUp() {
		log.info("");
		super.setUp();
		init();
	}

	private void init() {
		try {
			unit = null;
			Calendar today = Calendar.getInstance(); // current date/time
			currentDateTime  = today.getTime(); // now - date & time
			CalendarUtils.setStartOfDay(today);		// set to midnight
			currentDate = today.getTime(); // date only, time=12:00:00am
		}
		catch (Exception e) {
			EventUtils.logError(" DueOverdueCheck initializaion error = ", e);
		}
	}

	/**
	 * Called via a scheduled job, such as a Quartz task.
	 * See applicationContextScheduler.xml.
	 * @return Success
	 */
	public String projectBasedNotifications() {
		// This task should never be scheduled in an offline environment.  So we set
		// the 'offline' status to false, to prevent ApplicationScopeBean getting
		// an error trying to look up the value when we don't have a Faces Context.
		ApplicationUtils.setOffline(false);

		setUp();
		notifier = (DoNotification)ServiceFinder.findBean("NotificationBean");

		try {
			if (getProjectDAO().checkDb()) {
				// Find productions that are active and have report requirements
				List<Production> prods = ProductionDAO.getInstance().findForReportDue();
				for (Production prod : prods) {
					doProduction(prod);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		finally {
			tearDown();
		}
		return Constants.SUCCESS;
	}

	/**
	 * Run the due/overdue check for all the projects within one Production.
	 */
	private void doProduction(Production prod) {
		try {
			projectList = getProjectDAO().findByProduction(prod);
			unit = null; // force refresh of unit information

			for (Project projectItr : projectList) {
				setProject(projectItr);
				//log.debug("project Id=" + projectItr.getId() + ", name: " + projectItr.getTitle());
				if (project.getNotifying() && hasActive(project)) {
					Callsheet sourceCs = null;
					for (ReportRequirement requirement : project.getReportRequirements()) {
						if (requirement.getType() == ReportType.CALL_SHEET) {
							checkUnit(requirement);
							int rc = firstCallsheetStatus(requirement, true, null);
							if (rc == STATUS_OK) {
								sourceCs = nextCallsheetStatus(requirement, true, null);
							}
						}
					}
					// next call handles all report requirements except call sheet:
					findReportDueStatusOthers(sourceCs, project.getReportRequirements(), true);
				}
				else {
					//log.debug("project '" + project.getTitle() + "': notifications are off");
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Set our {@link #unit} variable to a fresh copy of the Unit that matches
	 * the given ReportRequirement.
	 *
	 * @param requirement The ReportRequirement to be processed.
	 */
	private void checkUnit(ReportRequirement requirement) {
		if (unit == null || ! unit.getNumber().equals(requirement.getUnitNumber())) {
			unit = null;
			schedule = null; // force schedule refresh since unit has changed
			for (Unit u : project.getUnits()) {
				if (u.getNumber().equals(requirement.getUnitNumber())) {
					unit = u;
					log.debug("setting unit to " + unit.getNumber());
					break;
				}
			}
		}
		if (unit == null) { // shouldn't happen, but just in case...
			unit = project.getMainUnit();
		}
		unit = UnitDAO.getInstance().refresh(unit);
	}

	/**
	 * Get the due/overdue status of reports for online display, e.g., on the
	 * Home page.
	 *
	 * @param pProject The project of interest.
	 * @param firstDate The earliest date to show any report due/overdue.
	 * @param reportRequirementList The reportRequirements list to use.
	 * @return A non-null, but possibly empty, list of report status objects;
	 *         entries are only added to this List if they are due or overdue.
	 *         Reports in "OK" status are not added.
	 */
	public List<ReportStatusItem> findReportDueStatus(Project pProject, Date firstDate,
			List<ReportRequirement> reportRequirementList) {
		List<ReportStatusItem> csReports = new ArrayList<>();
		Callsheet sourceCs = null;
		project = pProject;
		if (! hasActive(project)) {
			return csReports;
		}
		init();

		// We call the callsheet routines even if the call sheet isn't in the given
		// 'reportRequirementList', because we need the "source" (active) call sheet
		// for the other requirements.
		for (ReportRequirement requirement : project.getReportRequirements()) {
			if (requirement.getType() == ReportType.CALL_SHEET) {
				checkUnit(requirement);
				int rc = firstCallsheetStatus(requirement, false, csReports);
				if (rc == STATUS_OK) {
					sourceCs = nextCallsheetStatus(requirement, false, csReports);
				}
			}
		}
		// See if the caller wants call sheet requirements...
		boolean callsheetRequired = false;
		for (ReportRequirement requirement : reportRequirementList) {
			if (requirement.getType() == ReportType.CALL_SHEET) {
				callsheetRequired = true;
			}
		}
		if (! callsheetRequired) { // not wanted/needed
			csReports.clear();		// so erase any Report status entries.
		}
		// next call handles all report requirements except call sheet:
		List<ReportStatusItem> reports = findReportDueStatusOthers(sourceCs, reportRequirementList, false);
		reports.addAll(csReports);

		findEarlierReportsDue(firstDate, reportRequirementList, reports);

		return reports;
	}

	/**
	 *  Do due/overdue check for all reports except callsheet.
	 *  <p>
	 *  This is driven by entries in the given List of ReportRequirements, which specifies the
	 *  report type, the frequency of the report, the first due date for the report, and
	 *  who should be notified when the report is due or overdue.
	 */
	private List<ReportStatusItem> findReportDueStatusOthers(Callsheet sourceCs,
			List<ReportRequirement> reportRequirementList, boolean notify) {
		boolean done;
		ReportType reportType;
		int status;
		List<ReportStatusItem> reports = null;
		if (! notify) {
			reports = new ArrayList<>();
		}

		/** the date & time of the crew-call from the current callsheet. */
		Date crewCall = null;
		Calendar csDate = new GregorianCalendar();
		if (sourceCs != null) {
			crewCall = sourceCs.getCallTime();
		}
		else {
			if (unit == null || unit.getNumber() != 1) {
				unit = project.getMainUnit(); // for end-date and "day off" checks when no callsheet exists
				schedule = null; // force schedule refresh since unit has changed
			}
			if (getSchedule().getEndDate().before(currentDate)) {
				return reports;
			}
			else if (getSchedule().isDayOff(currentDate)) {
				// no "current" call sheet, & today is not a shoot day, skip this
				return reports;
			}
		}
		if (crewCall == null) { // a shoot day, but no callsheet or no time in the callsheet
			Calendar c = CalendarUtils.getInstance(currentDate);
			c.add(Calendar.HOUR_OF_DAY, 8); // 8am today
			crewCall = c.getTime();
		}
		csDate.setTime(setDate(crewCall)); // date of crewcall, with time=12:00am

		log.debug("# requirements: " + reportRequirementList.size());
		for (ReportRequirement reportRequirement : reportRequirementList) {
			log.debug("req=" + reportRequirement);
			reportType = reportRequirement.getType();
			status = STATUS_OK;
			checkUnit(reportRequirement);
			if (isReportDay(reportRequirement, sourceCs)) {

				switch (reportType) {
					case DPR:
						done = getDprDAO().existsByStatusDateAndProject(ReportStatus.SUBMITTED, csDate.getTime(), project);
						if ( ! done) {
							done = getDprDAO().existsByStatusDateAndProject(ReportStatus.APPROVED, csDate.getTime(), project);
						}
						if ( ! done) {
							log.debug("----  DPR due/overdue check ------");
							status = calculateDueOverdue(reportRequirement, Constants.DPR_DUE_HOURS_AFTER_CALLTIME,
									Constants.DPR_OVERDUE_HOURS_AFTER_CALLTIME, crewCall, csDate, notify);
						}
						break;

					case APPROVE_DPR:
//						// currently unused
//						done = getDprDAO().existsByStatusDateAndProject(ReportStatus.APPROVED, csDate.getTime(), project);
//						if ( ! done) {
//							log.debug("----  approve DPR due/overdue check ------");
//							status = calculateDueOverdue(reportRequirement, Constants.DPR_DUE_HOURS_AFTER_CALLTIME,
//									Constants.DPR_OVERDUE_HOURS_AFTER_CALLTIME, crewCall, csDate, notify);
//						}
						break;

//					case EXHIBIT_G:
//						done = getExhibitGDAO().existsByStatusDateAndProject(ReportStatus.PUBLISHED, csDate.getTime(), project);
//						if ( ! done) {
//							log.debug("----  exhibit-g due/overdue check -----");
//							status = calculateDueOverdue(reportRequirement, Constants.EXHIBITG_DUE_HOURS_AFTER_CALLTIME,
//									Constants.EXHIBITG_OVERDUE_HOURS_AFTER_CALLTIME, crewCall, csDate, notify);
//						}
//						break;

//					case TIME_SHEET:
//						done = timeSheetDAO.existsByStatusDateAndProject(ReportStatus.SUBMITTED, csDate.getTime(), project);
//						if ( ! done) {
//							done = timeSheetDAO.existsByStatusDateAndProject(ReportStatus.APPROVED, csDate.getTime(), project);
//						}
//						if (!done) {
//							log.debug("----  timesheet due/overdue check -----");
//							status = calculateDueOverdue(reportRequirement, Constants.TS_DUE_HOURS_AFTER_CALLTIME,
//									Constants.TS_OVERDUE_HOURS_AFTER_CALLTIME, crewCall, csDate, notify);
//						}
//						break;

//					case APPROVE_TIMESHEET:
//						// currently unused
//						done = timeSheetDAO.existsByStatusDateAndProject(ReportStatus.APPROVED, csDate.getTime(), project);
//						if (!done) {
//							log.debug("----  approve timesheet due/overdue check -----");
//							status = calculateDueOverdue(reportRequirement, Constants.TS_DUE_HOURS_AFTER_CALLTIME,
//									Constants.TS_OVERDUE_HOURS_AFTER_CALLTIME, crewCall, csDate, notify);
//						}
//						break;

					case CALL_SHEET:
						// Do nothing - handled elsewhere
						break;

					default:
						String msg = "Unhandled report type: " + reportType + ", project=" + project.toString();
						log.error(msg);
						EventUtils.logEvent(EventType.DATA_ERROR, SessionUtils.getProduction(),
								project, "DueOverdueCheck", msg);
						break;
				} // End of Switch
				if ( (! notify && status != STATUS_OK) && reports != null) {
					ReportStatusItem rb =
						new ReportStatusItem(reportRequirement.getType(), csDate.getTime(), status==STATUS_OVERDUE);
					reports.add(rb);
				}
			}
		} // End of ReportRequirement for-loop
		return reports;
	}

	/**
	 * See if the first callsheet is due. Special checks apply to the first one based on the project
	 * start date.
	 * @return int STATUS_OK if the callsheet is due & has been published, or if none is due;
	 * <br/>STATUS_DUE if the first call sheet has NOT been published and is due;
	 * <br/>STATUS_OVERDUE if the first call sheet has NOT been published and is overdue;
	 */
	private int firstCallsheetStatus(ReportRequirement requirement, boolean notify,
			List<ReportStatusItem> reports) {
		//log.debug("");
		int status = STATUS_OK;
		Date projectStart = unit.getProjectSchedule().getStartDate();
		if (currentDate.after(projectStart)) {
			log.debug("too late");
			return status; // now is later than (12:00am of) the project start date, this check is unnecessary.
		}
		if (getCallsheetDAO().existsByStatusDateAndProject(ReportStatus.PUBLISHED, projectStart, project)) {
			log.debug("published");
			return status; // the callsheet for the project start date has already been published.
		}

		Calendar startDate = new GregorianCalendar();
		startDate.setTime(setDate(projectStart));

		Calendar firstCsDateStart = new GregorianCalendar();
		firstCsDateStart.setTime(startDate.getTime());
		firstCsDateStart.add(Calendar.HOUR_OF_DAY, -1*Constants.CS_DUE_HOURS_BEFORE_PROJECT_START);

		Calendar firstCsDateEnd = new GregorianCalendar();
		firstCsDateEnd.setTime(startDate.getTime());
		firstCsDateEnd.add(Calendar.HOUR_OF_DAY, -1*Constants.CS_OVERDUE_HOURS_BEFORE_PROJECT_START);

		log.debug("due=" + firstCsDateStart.getTime() +
				", overdue=" + firstCsDateEnd.getTime());
		if (currentDateTime.after(firstCsDateEnd.getTime())) {
			log.debug("overdue");
			status = STATUS_OVERDUE;
			// Report is overdue - Did we issue notification yet?
			if (notify) {
				boolean done = getNotificationDAO().existsByTypeProjectDateReportType(
						NotificationType.REPORT_OVERDUE, project, projectStart, ReportType.CALL_SHEET);
				if ( ! done) {
					List<Contact> list = ReportRequirementsUtils.getContactList(requirement);
					notifier.overdueNotification(project, list, startDate.getTime(), ReportType.CALL_SHEET);
				}
			}
		}
		else if ( currentDateTime.after(firstCsDateStart.getTime()) ) {
			log.debug("due");
			status = STATUS_DUE;
			// Report due (but not overdue) -- Did we issue notification yet?
			if (notify) {
				boolean done = getNotificationDAO().existsByTypeProjectDateReportType(
						NotificationType.REPORT_DUE, project, projectStart, ReportType.CALL_SHEET);
				if ( ! done) {
					List<Contact> list = ReportRequirementsUtils.getContactList(requirement);
					notifier.dueNotification(project, list, startDate.getTime(), ReportType.CALL_SHEET);
				}
			}
		}
		if ( reports != null && status != STATUS_OK) {
			ReportStatusItem rb =
				new ReportStatusItem(requirement.getType(), startDate.getTime(), status==STATUS_OVERDUE);
			reports.add(rb);
		}
		log.debug("end, status=" + status);
		return status;
	}

	/**
	 * Determine if next call sheet is due or overdue.  First we have to figure out the
	 * date of the "target callsheet" -- which one we're checking for.  The "source callsheet"
	 * is the one whose call-time we'll use to calculate the due/overdue time for the next one.
	 *<pre>
	 *<br/> If today is a shooting day
	 *<br/> 	if today's Callsheet is published,
	 *<br/> 		target-Callsheet is for next shoot date (source is today's)
	 *<br/> 	else
	 *<br/> 		target-Callsheet is today's Callsheet (source is prior Callsheet)
	 *<br/> else
	 *<br/> 	target-Callsheet is for next shoot day (source is prior Callsheet)
	 *</pre>
	 */
	private Callsheet nextCallsheetStatus(ReportRequirement requirement,
			boolean notify, List<ReportStatusItem> reports) {
		Callsheet sourceCs = null;
		Calendar targetCal = null;
		boolean done = false;
		if (!getSchedule().isDayOff(currentDate)) { // A shoot day
			done = getCallsheetDAO().existsByStatusDateAndProject(ReportStatus.PUBLISHED, currentDate, project);
			if (done) { // it's published already, we'll check for the next one.
				//log.debug("a");
				sourceCs = getCallsheetDAO().findByDateProjectUnit(currentDate, project, unit);
				targetCal = getSchedule().findNextWorkDate(currentDate);
				done = getCallsheetDAO().existsByStatusDateAndProject(ReportStatus.PUBLISHED, targetCal.getTime(), project);
			}
			else { // not published, figure out if today's is due/overdue
				//log.debug("b");
				Calendar priorDate = getSchedule().findPreviousWorkDate(CalendarUtils.getInstance(currentDate));
				if (priorDate != null) {
					sourceCs = getCallsheetDAO().findByDateProjectUnit(priorDate.getTime(), project, unit);
				}
				targetCal = CalendarUtils.getInstance(currentDate);
			}
		}
		else { // today's not a work day; we'll check for callsheet for next work day, and use
			// prior work day's call time to calculate out due/overdue times.
			//log.debug("c");
			Calendar priorDate = getSchedule().findPreviousWorkDate(CalendarUtils.getInstance(currentDate));
			if (priorDate != null) {
				sourceCs = getCallsheetDAO().findByDateProjectUnit(priorDate.getTime(), project, unit);
			}
			targetCal = getSchedule().findNextWorkDate(currentDate);
			done = getCallsheetDAO().existsByStatusDateAndProject(ReportStatus.PUBLISHED, targetCal.getTime(), project);
		}
		log.debug("done=" + done + ", source=" +
				(sourceCs==null? "null" : (sourceCs.getCallTime())) +
				", target=" + (targetCal.getTime()));
		if (!done && sourceCs != null && sourceCs.getCallTime() != null) {
			Date priorCalltime = sourceCs.getCallTime();
			log.debug("prior callsheet call time: " + priorCalltime);
			int status = calculateDueOverdue(requirement,
					Constants.CS_DUE_HOURS_AFTER_CALLTIME, Constants.CS_OVERDUE_HOURS_AFTER_CALLTIME,
					priorCalltime, targetCal, notify);
			if ( reports != null && status != STATUS_OK) {
				ReportStatusItem rb =
					new ReportStatusItem(requirement.getType(), targetCal.getTime(), status==STATUS_OVERDUE);
				reports.add(rb);
			}
			log.debug("status=" + status);
		}
		//log.debug("exit");
		return sourceCs;
	}

	/**
	 * See if a particular report is due or overdue, based on the current
	 * date/time and the dueTIme and overdueTime values provided. It does not
	 * check for existence of the reports. (That is done by the callers.)
	 *
	 * @param requirement The ReportRequirement that we are trying to satisfy.
	 * @param dueHours The number of hours after the 'calltime' at which point
	 *            this report is "DUE".
	 * @param overdueHours The number of hours after the 'calltime' at which
	 *            point this report is "OVERDUE".
	 * @param calltime For Callsheet`s, the crew-call time of the prior (last
	 *            work-day's Callsheet. For other reports, it is the crew-call
	 *            time of the current callsheet.
	 * @param callsheetDate For Callsheets, the 'target date' of the
	 *            (due/overdue) callsheet. For other reports, the date of the
	 *            callsheet associated with the report, usually the "current"
	 *            callsheet. In either case, the time value should be 12:00am.
	 * @return STATUS_OK, STATUS_DUE, or STATUS_OVERDUE
	 */
	private int calculateDueOverdue(ReportRequirement requirement,
			int dueHours, int overdueHours, Date calltime, Calendar callsheetDate, boolean notify) {

		Calendar dueTime = CalendarUtils.getInstance(calltime);
		Calendar overdueTime = CalendarUtils.getInstance(calltime);

		dueTime.add(Calendar.HOUR_OF_DAY, dueHours);
		overdueTime.add(Calendar.HOUR_OF_DAY, overdueHours);

		return calculateDueOverdue(requirement, calltime, dueTime, overdueTime, callsheetDate, notify);
	}

	/**
	 * See if a particular report is due or overdue, based on the current
	 * date/time and the dueTime and overdueTime values provided. It does not
	 * check for existence of the reports. (That is done by the callers.)
	 *
	 * @param requirement The ReportRequirement which identifies the type of
	 *            report to be checked/notified for, and whose contact
	 *            information will be used to generate the contact list.
	 * @param crewCall The date from this crewCall time will be included in the
	 *            notification sent.
	 * @param dueTime The date & time the report is due.
	 * @param overdueTime The date & time at which the report is overdue.
	 * @param csCal For Callsheets, the 'target date' of the (due/overdue)
	 *            callsheet. For other reports, the date of the callsheet
	 *            associated with the report, usually the "current" callsheet.
	 *            In either case, the time value should be 12:00am.
	 * @param notify
	 * @return STATUS_OK, STATUS_DUE, or STATUS_OVERDUE
	 */
	private int calculateDueOverdue(ReportRequirement requirement, Date crewCall,
			Calendar dueTime, Calendar overdueTime, Calendar csCal, boolean notify) {
		CalendarUtils.setStartOfDay(csCal);
		Date csDate = csCal.getTime();
		int rc = STATUS_OK;

		int diff = (int) ((currentDateTime.getTime() - overdueTime.getTime().getTime()) / (1000 * 60 * 60));
		log.debug("cs=" + (csDate) + ", crewCall=" + (crewCall) +
				", due=" + (dueTime.getTime()) + ", overdue=" + (overdueTime.getTime()) +
				", diff=" + diff + ", now=" + (currentDateTime) + ", this=" + this);

		ReportType reportType = requirement.getType();
		try {
			if ((currentDateTime.equals(dueTime.getTime())) || (currentDateTime.after(dueTime.getTime()))) {
				Date date = setDate(crewCall);
				if (reportType == ReportType.CALL_SHEET) {
					date = csDate;
				}
				if (currentDateTime.after(overdueTime.getTime())) {
					rc = STATUS_OVERDUE;
					//log.debug("OverDue: " + reportType);
					int hours = Constants.REPORT_OVERDUE_FREQUENCY_HOURS;
					// have we issued an overdue notice within the last "n" hours?
					if (notify) {
						boolean done = getNotificationDAO().existsByTypeProjectDateReportTypeWithinHours(
								NotificationType.REPORT_OVERDUE, project, date, reportType, hours);
						if ( ! done) {
							List<Contact> contactList = ReportRequirementsUtils.getContactList(requirement);
							notifier.overdueNotification(project, contactList, date, reportType);
						}
						else {
							log.debug("already notified for " + reportType + " OVER due.");
						}
					}
				}
				else {
					rc = STATUS_DUE;
					//log.debug("Due: " + reportType);
					if (notify) {
						boolean done = getNotificationDAO().existsByTypeProjectDateReportType(
								NotificationType.REPORT_DUE, project, date, reportType);
						if ( ! done) { // no notification sent yet
							List<Contact> contactList = ReportRequirementsUtils.getContactList(requirement);
							notifier.dueNotification(project, contactList, date, reportType);
						}
						else {
							log.debug("already notified for " + reportType + " due.");
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return rc;
	}

	private void findEarlierReportsDue(Date firstDate,
			List<ReportRequirement> requirementList, List<ReportStatusItem> reports) {
		if (firstDate.after(currentDate)) {
			return;
		}

		DateIterator diter = new DateIterator(firstDate, currentDate);
		log.debug("date range: " + firstDate + " to " + currentDate);
		for (Date date = diter.next(); diter.hasNext(); date = diter.next()) {
			Callsheet callsheet;
//			ExhibitG exhibitG;
			for (ReportRequirement requirement : requirementList) {
				checkUnit(requirement);
				boolean dayOff = getSchedule().isDayOff(date);
				log.debug(date + ", off=" + dayOff);
				boolean found = false;
				if (requirement.getFrequency() == ReportFrequency.WORKDAYS && ! dayOff) {
					switch (requirement.getType()) {
						case DPR:
						case APPROVE_DPR:
							Dpr dpr = getDprDAO().findByDateAndProject(date, project);
							found = (dpr != null);
							if (found && (requirement.getType() == ReportType.APPROVE_DPR) &&
									dpr != null && (dpr.getStatus() != ReportStatus.APPROVED)) {
								found = false;
							}
							break;
//						case TIME_SHEET:
//						case APPROVE_TIMESHEET:
//							TimeSheet timesheet = timeSheetDAO.findByDateAndProject(date, project);
//							found = (timesheet != null);
//							if (found && (requirement.getType() == ReportType.APPROVE_TIMESHEET) &&
//									(timesheet.getStatus() != ReportStatus.APPROVED)) {
//								found = false;
//							}
//							break;
						case CALL_SHEET:
							callsheet = getCallsheetDAO().findByDateProjectUnit(date, project, unit);
							found = (callsheet != null);
							if (found && callsheet != null) {
								found = (callsheet.getStatus() == ReportStatus.PUBLISHED);
							}
							break;
//						case EXHIBIT_G:
//							exhibitG = getExhibitGDAO().findByDateAndProject(date, project);
//							found = (exhibitG != null);
//							if (found) {
//								found = (exhibitG.getStatus() == ReportStatus.PUBLISHED);
//							}
						default:
							break;
					}
					if ( ! found) {
						ReportStatusItem rb =
							new ReportStatusItem(requirement.getType(), date, true);
						if ( ! reports.contains(rb)) {
							reports.add(rb);
						}
					}
				}
			}
		}
	}

	/**
	 * Is a scheduled report due, given the supplied ReportRequirement? For weekly and every-workday
	 * reports, if a "source" callsheet is supplied, its date is used in the determination,
	 * otherwise today's date is used. For one-time reports, today's date is always used.
	 *
	 * @param reportRequirement The reportRequirement in question.
	 * @return True if the specified report should be issued on the relevant day - either today or
	 *         the callsheet's date.
	 */
	private boolean isReportDay(ReportRequirement reportRequirement, Callsheet sourceCs) {
		ReportFrequency reportFrequency = reportRequirement.getFrequency();
		Date sourceDate = currentDate;
		if (sourceCs != null) {
			sourceDate = sourceCs.getDate();
		}

		if (reportFrequency == ReportFrequency.WORKDAYS && ! getSchedule().isDayOff(sourceDate)) {
			return true;
		}
		// Restore this code if WEEKLY or ONCE report frequency is required
//		int workDayNum = CalendarUtils.getDayNumber(reportRequirement.getFirstDate());
//		if (reportFrequency == ReportFrequency.WEEKLY && workDayNum == CalendarUtils.getDayNumber(sourceDate)) {
//			return true;
//		}
//		if (reportFrequency == ReportFrequency.ONCE && currentDate.equals(reportRequirement.getFirstDate())) {
//			return true;
//		}
		return false;
	}

	/**
	 * @param proj Project of interest
	 * @return True if any ReportRequirement for the project is "active".
	 */
	private boolean hasActive(Project proj) {
		for (ReportRequirement requirement : proj.getReportRequirements()) {
			if (requirement.isActive()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Set a Date's time-of-day to 12:00:00am.
	 *
	 * @param date
	 * @return the same date as the supplied value, but with the time-of-day
	 *         zeroed (set to 12:00:00am).
	 */
	private Date setDate(final Date date) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		CalendarUtils.setStartOfDay(cal);
		return cal.getTime();
	}

	/** See {@link #schedule}. */
	public ScheduleUtils getSchedule() {
		if (schedule == null) {
			schedule = new ScheduleUtils(unit);
		}
		return schedule;
	}

	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	/** See {@link #callsheetDAO}. */
	public CallsheetDAO getCallsheetDAO() {
		if (callsheetDAO == null) {
			callsheetDAO = CallsheetDAO.getInstance();
		}
		return callsheetDAO;
	}
	/** See {@link #callsheetDAO}. */
	public void setCallsheetDAO(CallsheetDAO callsheetDAO) {
		this.callsheetDAO = callsheetDAO;
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

	/** See {@link #projectDAO}. */
	public ProjectDAO getProjectDAO() {
		if (projectDAO == null) {
			projectDAO = ProjectDAO.getInstance();
		}
		return projectDAO;
	}
	/** See {@link #projectDAO}. */
	public void setProjectDAO(ProjectDAO projectDao) {
		projectDAO = projectDao;
	}

	/** See {@link #dprDAO}. */
	public DprDAO getDprDAO() {
		if (dprDAO == null) {
			dprDAO = DprDAO.getInstance();
		}
		return dprDAO;
	}
	/** See {@link #dprDAO}. */
	public void setDprDAO(DprDAO dprDAO) {
		this.dprDAO = dprDAO;
	}

	/** See {@link #exhibitGDAO}. */
	public ExhibitGDAO getExhibitGDAO() {
		if (exhibitGDAO == null) {
			exhibitGDAO = ExhibitGDAO.getInstance();
		}
		return exhibitGDAO;
	}
	/** See {@link #exhibitGDAO}. */
	public void setExhibitGDAO(ExhibitGDAO exhibitGDAO) {
		this.exhibitGDAO = exhibitGDAO;
	}

}
