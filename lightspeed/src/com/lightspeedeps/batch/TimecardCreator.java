package com.lightspeedeps.batch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.StartFormDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardUtils;

/**
 * This class creates new WeeklyTimecard objects, based on the existence of
 * StartForm records for a particular User.
 * <p>
 * It is normally run in "batch" mode via the Quartz scheduler; the scheduling
 * parameters, and property values for class fields, are set in
 * applicationContextScheduler.xml.
 * <p>
 * The method to invoke to start the process is execute().
 * <p>
 * The functionality may be tested as a JUnit using TimecardCreatorTest.java.
 */
public class TimecardCreator extends SpringBatch {
	private static final Log log = LogFactory.getLog(TimecardCreator.class);

	/** Set to true when the process starts to prevent more than one execution
	 * at a time. */
	private static boolean executing = false;

	/** A message to be added to the Event log upon completion. */
	private String logMessage;

	/** The number of active Productions found and checked. */
	private int prodCount;

	/** A mapping from week-end day number (Sunday=1, Saturday=7) to a set
	 * of date values used to determine which timecards are to be created. */
	private Map<Integer, DateParms> dateMap = new HashMap<>();

	/** If true, then create new timecards regardless of the status of the prior week's
	 * timecard.  If false, new timecards are NOT created when the prior week's timecard
	 * exists but is empty (unused). */
	private boolean ignorePriorWeek;

	/** If true, then create a new timecard only if the prior week's timecard exists. No
	 * hours need to have been entered. */
	private boolean onlyIfPriorWeek;

	/** The specific week-ending date for which timecards should be created.  This is only set
	 * when the creation process is run from an Admin utility page.  When the create is run
	 * from a scheduled task, this field is null. */
	private Date weekEndDate;

	/** True iff running in 'batch' mode, that is, via a schedule task, or JUnit test. */
	private boolean batch;

	/** If true, then the "auto-create-timecards" payroll preference will be
	 * ignored. This is set to true when the code is called from an
	 * admin/utility function to create timecards for a specific production. It
	 * is set to false when the scheduled task is being run for all eligible
	 * productions in the system. */
	private boolean ignoreAutoPref;

	/** The total number of timecards created during this run of the creation
	 * process. */
	private int totalTimecards;

	private transient WeeklyTimecardDAO weeklyTimecardDAO;
	private transient StartFormDAO startFormDAO;

	/**
	 * Constructor.  Note that this bean is constructed once at application start-up, and
	 * re-used each time it is scheduled to run.
	 */
	public TimecardCreator() {
		log.debug("");
	}

	/**
	 * @see com.lightspeedeps.batch.SpringBatch#setUp()
	 */
	@Override
	protected void setUp() {
		log.info("");
		super.setUp();
		init();
		ignorePriorWeek = false;
	}

	/**
	 * Initialization; common to both batch and admin usage.
	 */
	private void init() {
		try {
			logMessage = "";
			prodCount = 0;
			totalTimecards = 0;
			dateMap = new HashMap<>();
		}
		catch (Exception e) {
			EventUtils.logError("initializaion error: ", e);
		}
	}

	/**
	 * Create new timecards for all productions. May ignore some users if they
	 * had no timecard for the prior week, or if the prior week's timecard is
	 * empty (unused).
	 * <p>
	 * Called via a scheduled job, such as a Quartz task, or via some test
	 * facility, such as JUnit.
	 * <p>
	 * See applicationContextScheduler.xml.
	 *
	 * @return "failure" if an exception occurs, otherwise "success".
	 */
	public String execute() {
		log.info("Scheduled timecard creation starting");
		ignorePriorWeek = false;
		onlyIfPriorWeek = false;
		weekEndDate = null;
		batch = true;
		int ret = process(null);
		if (ret < 0) {
			return Constants.FAILURE;
		}
		return Constants.SUCCESS;
	}

	/**
	 * Create new timecards for one Production or all productions. May ignore
	 * some users if they had no timecard for the prior week, or if the prior
	 * week's timecard is empty (unused), based on the parameter passed.
	 * <p>
	 * Called via some test facility, such as JUnit, or a control on one of our
	 * admin/test pages.
	 * <p>
	 * Note that using this method bypasses the "batch" setup support in the
	 * SpringBatch superclass. This is necessary so that we do not use the
	 * "batch" Hibernate context, but use the normal, existing, Hibernate
	 * context.
	 *
	 * @param pProd The Production for which timecards will be created; if null,
	 *            all Production`s will be processed.
	 * @param pIgnorePriorWeekWorked If True, do NOT check the prior week for
	 *            work recorded to determine if timecards should be created, but
	 *            instead always create a timecard for a valid StartForm.
	 * @param pOnlyIfPriorWeek Only create a timecard if the prior week's
	 *            timecard exists. (This does not require the prior week to have
	 *            any hours entered.)
	 * @param weekEndDate The week-ending date of the timecards to be created.
	 *            If null, the week-ending date of the current week is used.
	 *
	 * @return -1 if an exception occurs, otherwise the number of timecards
	 *         created.
	 */
	public int execute(Production pProd, boolean pIgnorePriorWeekWorked, boolean pOnlyIfPriorWeek, Date pWeekEndDate) {
		User user = SessionUtils.getCurrentUser();
		log.info("Timecard creation started, user=" + (user==null ? "null" : user.getEmailAddress()));
		ignorePriorWeek = pIgnorePriorWeekWorked;
		weekEndDate = pWeekEndDate;
		onlyIfPriorWeek = pOnlyIfPriorWeek;
		batch = false;
		return process(pProd);
	}

	/**
	 * Create new timecards for one Production or all productions. May ignore
	 * some users if they had no timecard for the prior week, or if the prior
	 * week's timecard is empty (unused), based on the parameter passed.
	 *
	 * @param pProd The Production for which timecards will be created; if null,
	 *            all Production`s will be processed.
	 * @return -1 if an exception occurs, otherwise the number of timecards
	 *         created.
	 */
	private int process(Production pProd) {
		int ret = -1; // assume an error occurred

		if (executing) {
			log.warn("Attempt to start timecard creation while another instance was still running");
			if (! batch) {
				MsgUtils.addFacesMessage("Timecard.Autocreate.Running", FacesMessage.SEVERITY_ERROR);
			}
			return ret;
		}

		executing = true;

		try {
			// This task should never be scheduled in an offline environment.  So we set
			// the 'offline' status to false (not null), to prevent ApplicationScopeBean getting
			// an error trying to determine the value when we don't have a Faces Context.
			ApplicationUtils.setOffline(false);

			if (batch) { // running as scheduled task (typically under Quartz)
				setUp();
			}
			else { // NOT running as scheduled task; probably from an Admin page facility.
				// Avoid SpringBatch setup, so we use the existing Hibernate context.
				init();
			}

			if (getStartFormDAO().checkDb()) {
				// Determine week-start and week-end dates for the current
				// week, and for next week.
				Calendar calendar = Calendar.getInstance(); // current date/time
				int weekDay = calendar.get(Calendar.DAY_OF_WEEK); // remember today's day of the week

				// Determine which productions to process

				List<Integer> prodIds;
				if (pProd == null) {
					// retrieve all "eligible" productions
					prodIds = StartFormDAO.getInstance().findProductionsForTimecardCreation();
					ignoreAutoPref = false;
				}
				else { // caller supplied a Production -- only process this one.
					prodIds = new ArrayList<>();
					prodIds.add(pProd.getId());
					ignoreAutoPref = true; // generate timecards regardless of this production's preference setting.
				}

				// For each requested production, call doProductionTwoWeeks() to generate
				// this week's and, possibly, next week's timecards.

				for (Integer id : prodIds) {
					Production prod = ProductionDAO.getInstance().findById(id);
					doProductionTwoWeeks(prod, weekDay);
					prodCount++;
				}

				if (logMessage.length() > 0) {
					logMessage = "\n" + "List of 'production : timecard count' follows:" +
							logMessage;
				}
				logMessage = prodCount + " active productions checked; " +
						totalTimecards + " timecards created." + logMessage;
				EventUtils.logEvent(EventType.INFO, logMessage);
				ret = totalTimecards;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		finally {
			executing = false;
			if (batch) {
				// tear-down only required if SpringBatch setup() was run.
				tearDown();
			}
		}
		return ret;
	}

	/**
	 * For the given Production (and for each Project in the production if
	 * Commercial), invoke the
	 * {@link #doProduction(Production, Project, Date, Date, Date)} method for
	 * this week, and (optionally) for next week if today is within the bounds
	 * of the production's preference for "create timecards <n> days in
	 * advance".
	 *
	 * @param prod The production being processed.
	 * @param weekDay The week-day number for today.
	 */
	private void doProductionTwoWeeks(Production prod, int weekDay) {
		DateParms dp = null; // suppresses compile-time 'errors' in later code
		// create timecards for the current week, if needed
		if (prod.getType().hasPayrollByProject()) {
			for (Project project : prod.getProjects()) {
				if (ignoreAutoPref || project.getPayrollPref().getAutoCreateTimecards()) {
					dp = getDateParms(prod, project);
					doProduction(prod, project, dp.thisWkStartDate, dp.thisWkEndDate, dp.checkWkEndDate);
				}
			}
		}
		else {
			dp = getDateParms(prod, null);
			// For TV/Feature, the autoCreateTimecards flag was checked by the SQL query.
			doProduction(prod, null, dp.thisWkStartDate, dp.thisWkEndDate, dp.checkWkEndDate);
		}

		if (weekEndDate == null) {
			// Figure out when this production wants next week's timecards created.
			// (weekEndDate is not null if we were called for a specific week, so skip this part.)
			if (prod.getType().hasPayrollByProject()) {
				for (Project project : prod.getProjects()) {
					if (ignoreAutoPref || project.getPayrollPref().getAutoCreateTimecards()) {
						dp = getDateParms(prod, project);
						int boundary = project.getPayrollPref().getWeekEndDay() + 1;
						boundary -= project.getPayrollPref().getCreateTimecardsAdvance(); // use Production setup preference
						if (weekDay >= boundary) {
							// create timecards for next week, since it's soon enough.
							doProduction(prod, project, dp.nextWkStartDate, dp.nextWkEndDate,
									dp.checkWkEndDate);
						}
					}
				}
			}
			else {
				int boundary = prod.getPayrollPref().getWeekEndDay() + 1;
				boundary -= prod.getPayrollPref().getCreateTimecardsAdvance(); // use Production setup preference
				if (weekDay >= boundary && dp != null) {
					// create timecards for next week, since it's soon enough.
					doProduction(prod, null, dp.nextWkStartDate, dp.nextWkEndDate,
							dp.checkWkEndDate);
				}
			}
		}
	}

	/**
	 * Given an active Production, and optional Project, review all the
	 * StartForm`s that have an effective date which includes a date within the
	 * given date range, and create timecards corresponding to those StartForms
	 * if they do not already exist.
	 *
	 * @param prod The production of interest.
	 * @param project The Project of interest. If null, it is ignored and the
	 *            production should be a TV or Feature. If non-null, the
	 *            production must be a Commercial production, and only those
	 *            StartForm`s associated with this Project will be processed.
	 * @param wkStartDate The start of the payroll week (wkEndDate minus 6).
	 * @param wkEndDate The payroll week-ending date.
	 * @param checkWkEndDate The week-ending date of the timecard to check for
	 *            emptiness, when deciding whether to create a new timecard.
	 */
	private void doProduction(Production prod, Project project, Date wkStartDate, Date wkEndDate,
			Date checkWkEndDate) {
		/** The number of timecards created for the current production. */
		int timecardCount = 0;
		int incomplete = 0;
		boolean createIncomplete;

		/* Find StartForm's that are in effect during the week delimited by the
		 * given week-start and week-end dates, in the given Production. */
		List<StartForm> docs;
		if (project == null) { // TV/Feature
			createIncomplete = prod.getPayrollPref().getCreateIncompleteTimecards();
			docs = getStartFormDAO().findByActiveDate(prod, wkStartDate, wkEndDate);
		}
		else { // Commercial
			createIncomplete = project.getPayrollPref().getCreateIncompleteTimecards();
			docs = getStartFormDAO().findByActiveDateProject(project, wkStartDate, wkEndDate);
		}
		log.debug("w/e date=" + wkEndDate + ", docs=" + docs.size() + ", prod=" + prod.getTitle());

		ContactDocumentDAO contactDocumentDAO = ContactDocumentDAO.getInstance();
		for (StartForm doc : docs) {
			log.debug("start form = " + doc.getId());
			boolean isVoid = false;
			if (prod.getAllowOnboarding()) {
				ContactDocument cd = contactDocumentDAO.findOneByProperty("relatedFormId", doc.getId());
				if (cd != null && cd.getStatus() == ApprovalStatus.VOID) {
					log.info("****** VOID StartForm ******* " + doc.getId() +" contact document: " + cd.getId());
					isVoid = true;
				}
			}
			if (doc.getEmployment() == null) {
				// was not converted successfully from 3.0 to 3.1 - ignore
				log.info("****** Invalid StartForm (no ER) ******* " + doc.getId());
				isVoid = true;
			}
			if (! isVoid) {
				if (doc.getHasRequiredFields() ||
						(createIncomplete && doc.getAllowTimecardCreate())) {
					if (doc.getContact().getActive()) {
						timecardCount += doStartForm(prod, wkEndDate, checkWkEndDate, doc);
					}
				}
				else {
					incomplete++;
				}
			}
		}
		if (timecardCount > 0 || incomplete > 0) {
			String indicator = (timecardCount > 0 ? "* " : "- ");
			String date = new SimpleDateFormat("M/d/yyyy").format(wkEndDate);
			logMessage += "\n" + indicator + prod.getTitle();
			if (project != null) {
				logMessage += " / " + project.getTitle();
			}
			logMessage += ": " + timecardCount + " for W/E " + date;
			totalTimecards += timecardCount;
			if (incomplete > 0) {
				logMessage += " (" + incomplete + " incomplete Start Forms)";
			}
		}
	}

	/**
	 * For the given StartForm, is there a timecard matching it (date & job
	 * class/occupation)? If so, take no action; if not, create the timecard.
	 * The caller should already have verified that the StartForm is in effect
	 * sometime during the payroll week specified by the week-ending date.
	 *
	 * @param prod The Production being processed.
	 * @param weDate The week-ending date of the timecard to be generated.
	 * @param checkWkEndDate The week-ending date of the timecard to check for
	 *            emptiness, when deciding whether to create a new timecard.
	 * @param sf The StartForm for the timecard to be generated.
	 *
	 * @return The number of timecards created, either zero or one.
	 */
	private int doStartForm(Production prod, Date weDate, Date checkWkEndDate, StartForm sf) {
		boolean create = false; // default to NOT create the new one
		int reason = 0;	// reason for not creating one -- for debugging
		User user = sf.getContact().getUser();
		boolean existing = getWeeklyTimecardDAO().existsWeekEndDateStartForm(weDate, sf);

		if (! existing) {
			Project project = sf.getProject(); // Commercial: project; TV or Feature: null
			String userAcct = user.getAccountNumber();
			existing = getWeeklyTimecardDAO().existsWeekEndDateAccountOccupation(prod, project, weDate,
					userAcct, sf.getJobClass());
		}

		if (! existing) { // no timecard yet.
			if (ignorePriorWeek) {
				reason = 4;
				// don't look at prior timecards, just create one as long as this is a Crew member (not cast/stunt)
				create = ContactDAO.isCrewNotStuntMember(sf.getContact());
			}
			else {
				// check prior week's card; if it exists & has no data, skip creating a new one.
				List<WeeklyTimecard> cards = getWeeklyTimecardDAO().findByWeekEndDateStartForm(checkWkEndDate, sf);
				if (cards.size() > 0) { // have one or more timecards from last week.
					reason = 1;
					if (onlyIfPriorWeek) {
						create = true;
					}
					else {
						// Set "create" flag only if at least one timecard is NOT empty
						for (WeeklyTimecard wtc : cards) {
							TimecardCalc.calculateWeeklyTotals(wtc);
							if (! wtc.getEmpty()) {
								create = true; // not empty - go ahead and create the new one
								break; // no need to test the rest.
							}
						}
					}
				}
				else {
					reason = 2;
					// no timecard from last week. If this is first week for the StartForm,
					// then create the timecard, otherwise do not, because we skipped last week's
					// creation for a reason!
					if (sf.getEffectiveOrStartDate().after(checkWkEndDate) ||
							sf.getWorkStartDate().after(checkWkEndDate)) {
						reason = 3;
						// first week this StartForm has been in effect
						// We only create initial TCs for CREW roles at this time...
						if (ContactDAO.isCrewNotStuntMember(sf.getContact())) {
							create = true; // so we need to create the timecard
						}
					}
//					else {
//						// StartForm has been in effect prior to this week; but if person
//						// has never had a timecard for this Start, give them one. rev 2.2.4858.
//						if (! getWeeklyTimecardDAO().existsTimecardsForStartForm(sf.getId())) {
//							reason = 5;
//							create = ContactDAO.isCrewNotStuntMember(sf.getContact());
//						}
//					}
				}
			}
			if (create) {
				log.debug("create timecard, user=" + user.getFirstNameLastName() + ", job=" + sf.getJobClass());
				TimecardUtils.createTimecard(user, prod, weDate, sf, false);
			}
			else {
				log.debug("skipped timecard: prior week not used; user=" + user.getFirstNameLastName() +
						", job=" + sf.getJobClass() + ", SF id=" + sf.getId() + ", reason=" + reason);
			}
		}
		return (create ? 1 : 0);
	}

	/**
	 * Find or create a set of date parameters which are consistent with the
	 * given Production and/or Project's start-of-week day preference.
	 * @param prod
	 * @param proj
	 * @return
	 */
	private DateParms getDateParms(Production prod, Project proj) {
		int wkEndDay = TimecardUtils.findWeekEndDay(prod, proj);
		DateParms dp = dateMap.get(wkEndDay);
		if (dp == null) {
			dp = new DateParms();
			dp.calculate(prod, proj);
			dateMap.put(wkEndDay, dp);
		}
		return dp;
	}


	// * * * accessors & mutators * * *

	/** See {@link #startFormDAO}. */
	public StartFormDAO getStartFormDAO() {
		if (startFormDAO == null) {
			startFormDAO = StartFormDAO.getInstance();
		}
		return startFormDAO;
	}
	/** See {@link #startFormDAO}. */
	public void setStartFormDAO(StartFormDAO startFormDAO) {
		this.startFormDAO = startFormDAO;
	}

	/** See {@link #weeklyTimecardDAO}. */
	public WeeklyTimecardDAO getWeeklyTimecardDAO() {
		if (weeklyTimecardDAO == null) {
			weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		}
		return weeklyTimecardDAO;
	}
	/** See {@link #weeklyTimecardDAO}. */
	public void setWeeklyTimecardDAO(WeeklyTimecardDAO weeklyTimecardDAO) {
		this.weeklyTimecardDAO = weeklyTimecardDAO;
	}


	/**
	 * This data object holds the dates related to the generation of timecards.
	 * The dates will be dependent on the current date, and on a particular
	 * Production's or Project's payroll start-of-week day preference.
	 */
	public class DateParms {
		/** Date of the first payroll day of the current week. */
		Date thisWkStartDate;

		/** Week-ending Date of the current payroll week. */
		Date thisWkEndDate;

		/** Week-ending Date of the prior payroll week. Used to locate the
		 * prior timecard when checking for existence, hours, etc. */
		Date checkWkEndDate;

		/** Date of the first payroll day of the following week. */
		Date nextWkStartDate;

		/** Week-ending Date of the following payroll week. */
		Date nextWkEndDate;

		public void calculate(Production prod, Project proj) {
			Calendar calendar = Calendar.getInstance(); // current date/time

			thisWkEndDate = weekEndDate;
			if (thisWkEndDate == null) {
				thisWkEndDate = TimecardUtils.calculateWeekEndDate(calendar.getTime(), prod, proj);
			}
			checkWkEndDate = TimecardUtils.calculatePriorWeekEndDate(thisWkEndDate);

			calendar.setTime(thisWkEndDate);
			calendar.add(Calendar.DAY_OF_MONTH, 1); // start of next week
			nextWkStartDate = calendar.getTime();
			calendar.add(Calendar.DAY_OF_MONTH, 6); // end of next week
			nextWkEndDate = calendar.getTime();

			log.info("this w/e date=" + thisWkEndDate + ", next w/e date=" + nextWkEndDate);

			calendar.setTime(thisWkEndDate);
			calendar.add(Calendar.DAY_OF_MONTH, -6); // start of this week
			thisWkStartDate = calendar.getTime();
		}
	}

}
