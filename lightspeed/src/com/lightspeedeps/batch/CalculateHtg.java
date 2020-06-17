package com.lightspeedeps.batch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.StartFormDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.TimecardMessage;
import com.lightspeedeps.service.TimecardService;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardUtils;

/**
 * This class runs the HTG (hours-to-gross) process for multiple WeeklyTimecard objects.
 * <p>
 * Currently it is typically run only from an Admin UI page, allowing a privileged user to run the
 * HTG process on a range of weeks in a single operation.
 * <p>
 * It could be run in "batch" mode via the Quartz scheduler; the scheduling parameters, and property
 * values for class fields, would be set in applicationContextScheduler.xml.
 * <p>
 * The method to invoke to start the process is execute().
 * <p>
 * There is currently no JUnit test for this class.
 */
public class CalculateHtg extends SpringBatch {
	private static final Log log = LogFactory.getLog(CalculateHtg.class);

	/**
	 * Set to true when the process starts to prevent more than one execution at a time.
	 */
	private static boolean executing = false;

	/** A message to be added to the Event log upon completion. */
	private String logMessage;

	/** The Production whose timecards should be processed. */
	private Production production;

	/**
	 * The Project whose timecards should be processed; only non-null for Commercial projects.
	 */
	private Project project;

	/** The number of active Productions found and checked. */
	private int prodCount;

	/** The first W/E date of the timecards to be processed. */
	private Date wkFirstDate;

	/**
	 * The last W/E date of the timecards to be processed. (That is, the processing WILL include
	 * this W/E date.)
	 */
	private Date wkLastDate;

	private WeeklyTimecard weeklyTimecard;

	/** If true, then ... */
	//	private boolean ignorePriorWeek;

	/** The total number of timecards select during this run */
	int totalTimecardsSelected;
	/** The total number of timecards calculated during this run. */
	private int totalTimecards;

	private transient WeeklyTimecardDAO weeklyTimecardDAO;
	private transient StartFormDAO startFormDAO;

	/**
	 * Constructor. Note that this bean is constructed once at application start-up, and re-used
	 * each time it is scheduled to run.
	 */
	public CalculateHtg() {
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
	}

	private void init() {
		try {
			logMessage = "";
			prodCount = 0;
			totalTimecards = 0;
			totalTimecardsSelected = 0;
			//			ignorePriorWeek = false;
		}
		catch (Exception e) {
			EventUtils.logError("initializaion error: ", e);
		}
	}

	/**
	 * Calculate timecards for all productions. (Not currently used.)
	 * <p>
	 * Called via a scheduled job, such as a Quartz task, or via some test facility, such as JUnit.
	 * <p>
	 * See applicationContextScheduler.xml.
	 *
	 * @return "failure" if an exception occurs, otherwise "success".
	 */
	public String execute() {
		log.info("Scheduled timecard HTG Calculation starting");
		List<Integer> results = process(null, false, null, null, null, true);
		int tcCalculated = results.get(1);
		if (tcCalculated < 0) {
			return Constants.FAILURE;
		}
		return Constants.SUCCESS;
	}

	/**
	 * Calculate HTG for one Production or all productions.
	 * <p>
	 * Called via some test facility, such as JUnit, or a control on one of our admin/test pages.
	 * <p>
	 * Note that using this method bypasses the "batch" setup support in the SpringBatch superclass.
	 * This is necessary so that we do not use the "batch" Hibernate context, but use the normal,
	 * existing, Hibernate context.
	 *
	 * @param pProd The Production for which timecards will be calculated; if null, all Production`s
	 *            will be processed.
	 * @param pIgnorePriorWeek ... currently unused
	 * @param weFirstDate The first week-ending date of the timecards to be processed. If null, the
	 *            week-ending date of the current week is used.
	 * @param weLastDate The last week-ending date of the timecards to be processed. If null, the
	 *            weFirstDate is used, i.e., a single week will be processed.
	 * @param msgs
	 * @return -1 if an exception occurs, otherwise the number of timecards calculated.
	 */
	public List<Integer> execute(Production pProd, boolean pIgnorePriorWeek, Date weFirstDate, Date weLastDate, List<TimecardMessage> msgs) {
		User user = SessionUtils.getCurrentUser();
		log.info("HTG calculation started, user=" + (user == null ? "null" : user.getEmailAddress()));

		return process(pProd, pIgnorePriorWeek, weFirstDate, weLastDate, msgs, false);
	}

	/**
	 * Calculate HTG for one Production or all productions.
	 *
	 * @param pProd The Production for which timecards will be calculated; if
	 *            null, all Production`s will be processed.
	 * @param pIgnorePriorWeek Currently unused.
	 * @param msgs A List of TimecardMessage that messages should be added to.
	 * @param weFirstDate The first week-ending date of the timecards to be
	 *            processed. If null, the week-ending date of the current week
	 *            is used.
	 * @param weLastDate The last week-ending date of the timecards to be
	 *            processed. If null, the weFirstDate is used, i.e., a single
	 *            week will be processed.
	 * @param batch Must be specified as true if running as a scheduled task,
	 *            e.g., under Quartz control; false if invoked from a web (user)
	 *            session.
	 * @return [0,-1] if an exception occurs, otherwise the number of timecards
	 *         selected and the number calculated.
	 */
	private List<Integer> process(Production pProd, boolean pIgnorePriorWeek, Date weFirstDate, Date weLastDate, List<TimecardMessage> msgs, boolean batch) {
		List<Integer> results = new ArrayList<Integer>();
		// assume an error occurred
		results.add(0);
		results.add(-1);

		production = pProd;

		if (executing) {
			log.warn("Attempt to start timecard calculated while another instance was still running");
			if (!batch) {
				MsgUtils.addFacesMessage("Timecard.Calculate.Running", FacesMessage.SEVERITY_ERROR);
			}
			return results;
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

			//			ignorePriorWeek = pIgnorePriorWeek;

			if (getStartFormDAO().checkDb()) {
				// Determine week-start and week-end dates for the current
				// week, and for next week.
				Calendar calendar = Calendar.getInstance(); // current date/time

				wkFirstDate = weFirstDate;
				if (wkFirstDate == null) {
					wkFirstDate = TimecardUtils.calculateWeekEndDate(calendar.getTime());
				}

				wkLastDate = weLastDate;
				if (wkLastDate == null) {
					wkLastDate = wkFirstDate;
				}

				log.info(">> First W/E date=" + wkFirstDate + ", last W/E date=" + wkLastDate);

				// Determine which productions to process

				List<Integer> prodIds;
				if (production == null) {
					// retrieve all "eligible" productions
					prodIds = getStartFormDAO().findProductionsForTimecardCreation(); // TODO chg for calc HTG
				}
				else { // caller supplied a Production -- only process this one.
					prodIds = new ArrayList<Integer>();
					prodIds.add(pProd.getId());
				}

				/*
				 * For every applicable Production, process its timecards.
				 */

				for (Integer id : prodIds) {
					production = ProductionDAO.getInstance().findById(id);
					// calculate timecards for the current week, if needed
					if (production.getType().hasPayrollByProject()) {
						for (Project proj : production.getProjects()) {
							project = proj;
							doProduction(msgs);
						}
					}
					else {
						project = null;
						doProduction(msgs);
					}

					prodCount++;
				}

				if (logMessage.length() > 0) {
					logMessage = "\n" + "List of 'production : timecard count' follows:" +
							logMessage;
				}
				logMessage = prodCount + " active productions checked; " +
						totalTimecards + " timecards calculated." + logMessage;
				EventUtils.logEvent(EventType.INFO, logMessage);

				results.clear();
				results.add(totalTimecardsSelected);
				results.add(totalTimecards);
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
		return results;
	}

	/**
	 * Given the current active Production, and optional Project, calculate the timecards with the
	 * given W/E date.
	 *
	 * @param msgs
	 */
	private void doProduction(List<TimecardMessage> msgs) {
		/** The number of timecards calculated for the current production. */
		SimpleDateFormat dateFmt = new SimpleDateFormat("M/d/yyyy");
		int processed = 0;

		Date weDate = wkFirstDate;
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(weDate);

		String prodMessage = production.getTitle();
		if (project != null) {
			prodMessage += " / " + project.getTitle();
		}
		prodMessage += ":";

		while (!weDate.after(wkLastDate)) {
			List<WeeklyTimecard> tcs = getWeeklyTimecardDAO().findByWeekEndDate(production, project, weDate, null);
			// Add to the total amount of timecards selected.
			totalTimecardsSelected += tcs.size();
			int timecardCount = 0;
			int incomplete = 0;

			log.info("w/e date=" + weDate + ", timecards=" + tcs.size() + ", prod=" + production.getTitle());

			for (WeeklyTimecard wtc : tcs) {
				if (wtc.getEmpty()) { // could be false positive
					TimecardCalc.calculateWeeklyTotals(wtc);
				}
				if (! wtc.getEmpty()) {
					weeklyTimecard = wtc;
					timecardCount += doTimecard(msgs);
				}
				else {
					incomplete++;
				}
				processed++;
			}

			prodMessage += " " + timecardCount + " for W/E " + dateFmt.format(weDate);
			totalTimecards += timecardCount;
			if (incomplete > 0) {
				prodMessage += " (" + incomplete + " blank)";
			}
			prodMessage += ";";

			cal.add(Calendar.DATE, 7);
			weDate = cal.getTime();
		}

		String indicator = (processed > 0 ? "* " : "- ");
		logMessage += "\n" + indicator + prodMessage;

	}

	/**
	 * For the given timecard, calculate its gross.
	 *
	 * @param msgs A List to hold messages generated during the HTG process.
	 *
	 * @return The number of timecards calculated, either zero or one.
	 */
	private int doTimecard(List<TimecardMessage> msgs) {

		TimecardService.calculateAllHtgAndUpdate(weeklyTimecard, msgs);

		return 1;
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

}
