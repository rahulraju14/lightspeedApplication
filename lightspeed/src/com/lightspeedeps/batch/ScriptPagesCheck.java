package com.lightspeedeps.batch;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.NotificationDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.ProjectMemberDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.AccessStatus;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.type.NotificationType;
import com.lightspeedeps.type.Permission;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.util.report.ReportUtils;
import com.lightspeedeps.util.report.ScriptReporter;
import com.lightspeedeps.web.login.AuthorizationBean;

/**
 * This class handles sending "advance copies" of script pages to those users who have selected that
 * preference. It is normally run as a Quartz scheduled job once a day. It scans all projects in the
 * production, and for each project, scans all project members for users who have selected to
 * receive script pages in advance. Each user may select how many days in advance they wish to
 * receive the pages.
 * <p>
 * The information sent is based on the project's "current" (active) Script and Stripboard.
 * <p>
 * The method to invoke to start the process is execute().
 * <p>
 * The methods in this class build the text file of the script scenes, and determine which contacts
 * will receive the text. It invokes methods of the DoNotification class to actually issue the
 * notifications (e-mails).
 * <p>
 * The functionality may be tested as a JUnit using ScriptPagesTest.java.
 */
public class ScriptPagesCheck extends SpringBatch {

	private static final Log log = LogFactory.getLog(ScriptPagesCheck.class);

	private ScheduleUtils scheduleUtils;
	private DoNotification notifier;
	private AuthorizationBean authBean;

	// The DAO objects are normally initialized via XML entries.
	// See applicationContextScheduler.xml.

	private transient NotificationDAO notificationDAO;
	private transient ProjectDAO projectDAO;
	private transient ProjectMemberDAO projectMemberDAO;
	private transient SceneDAO sceneDAO;
	private transient StripDAO stripDAO;

	private Date currentDate;	// today's date only, time=12:00am
	private Calendar currentCal;
	private Calendar tomorrow;

	public ScriptPagesCheck() {
		log.debug("");
	}

	@Override
	protected void setUp() {
		log.info("");
		super.setUp();
		try {
			currentCal = Calendar.getInstance();
			CalendarUtils.setStartOfDay(currentCal);
			currentDate = currentCal.getTime(); // date only, time=12:00:00am
			tomorrow = CalendarUtils.getInstance(currentDate);
			tomorrow.add(Calendar.DAY_OF_MONTH, 1);
			notifier = (DoNotification)ServiceFinder.findBean("NotificationBean");

			if (authBean == null) { // test invocation probably
				log.info("self-initialization required"); // only if not run as a "bean"
				authBean = AuthorizationBean.getInstance();
			}
		}
		catch (Exception e) {
			EventUtils.logError(" ScriptPagesCheck initialization error = ", e);
		}
	}

	/**
	 * Called via a scheduled job, such as a Quartz task. (See file
	 * applicationContextScheduler.xml.)
	 * <p>
	 * This method loops through all the Project`s in the Production, and all
	 * the Unit`s within each Project. It calls {@link #sendPagesForProject} for
	 * each Unit found.
	 *
	 * @return "success", unless an exception is caught, in which case "failure"
	 *         is returned.
	 */
	public String execute() {
		String result = Constants.SUCCESS;
		setUp();

		try {
			if (getProjectDAO().checkDb()) {
				// Process all ACTIVE productions in the database...
				List<Production> prods = ProductionDAO.getInstance()
						.findByProperty(ProductionDAO.STATUS, AccessStatus.ACTIVE);
				for (Production prod : prods) {
					doProduction(prod);
				}
			}
		}
		catch (Exception e) {
			result = Constants.FAILURE;
			EventUtils.logError(e);
		}
		finally {
			tearDown();
		}
		return result;
	}

	/**
	 * Do the Advance Script pages processing for all the projects
	 * in the given Production.
	 */
	private void doProduction(Production prod) {
		String projectMsg = "";
		Project proj = null;
		try {
			Set<Project> projectList = prod.getProjects();// projectDAO.findByProduction(prod);
			for (Project project : projectList) {
				proj = project; // for error recording
				log.debug("projectId=" + project.getId());
				if (project.getScript() != null && project.getScript().getSceneText()
						&& project.getStatus() == AccessStatus.ACTIVE) {
					boolean done = getNotificationDAO().existsByTypeProjectDate(
							NotificationType.SCRIPT_PAGES, project, currentDate);
					if (! done) {
						projectMsg = "script=" + project.getScript().toString();
						for (Unit unit : project.getUnits()) {
							sendPagesForProject(project, unit);
						}
					}
				}
				else {
					log.debug("skipping project='" + project.getTitle() +
							"': project inactive, no script, or script has no text");
				}
			}
		}
		catch (Exception e) {
			EventUtils.logEvent(EventType.APP_ERROR, prod, proj, null,
					projectMsg + Constants.NEW_LINE + EventUtils.traceToString(e) );
		}
	}

	/**
	 * Generate all the Notifications (which will become e-mails) for the
	 * specified Project and Unit. It finds all the User`s belonging to the Unit
	 * who have opted to have advance script pages sent, and creates the
	 * formatted output for each person based on the number of days in advance
	 * requested by them, and the scenes scheduled to be shot on that (future) date.
	 *
	 * @param project The Project for which notifications should be generated.
	 * @param unit The Unit for which notifications should be generated -- this
	 *            determines the User`s whose preferences will be inspected,
	 *            based on their membership in the Unit.
	 */
	private void sendPagesForProject(Project project, Unit unit) {
		scheduleUtils = new ScheduleUtils(unit);
		if (! currentDate.before(scheduleUtils.getEndDate())) {
			return; // shooting already completed (or completes today)
		}

		// Loop through all the project's contacts, and organize them based on
		// the number of days advance notice they want.
		Map<Integer,List<Contact>> map = new HashMap<Integer,List<Contact>>();
		List<Contact> contacts;
		List<Contact> projectUserList = getProjectMemberDAO().findByUnitDistinctContact(unit);
		for (Contact user : projectUserList) {
			//Contact c = user.getContact();
			int days;
			if ( (user.getSendAdvanceScript() != null) && (days=user.getSendAdvanceScript()) > 0) {
				// user wants scripts sent; see if they have permission
				if (authBean.hasPermission(user, project, Permission.VIEW_SCRIPT_REVISIONS)) {
					contacts = map.get(days);
					if (contacts == null) { // no collection yet for this 'advance days' value
						contacts = new ArrayList<Contact>();
						map.put(days, contacts);
					}
					contacts.add(user);
				}
			}
		}
		Calendar targetCal;
		for (Integer days : map.keySet()) {
			// Loop thru each existing "advance days" settings (1+, not necessarily sequential),
			// see if that many days ahead is a shooting day, and if so, generate a printout
			// of the script for the scenes shooting on that day.
			targetCal = CalendarUtils.getInstance(currentDate);
			targetCal.add(Calendar.DAY_OF_MONTH, days);
			Date shootDate = targetCal.getTime();
			int shootDay = scheduleUtils.findShootingDayNumber(shootDate);
			if (shootDay != 0) { // work day
				List<Scene> scenes = new ArrayList<Scene>();
				String sceneNumberList = findScenesForDay(project, shootDay, scenes);
				if (sceneNumberList != null) {
					String fileName = generateFile(project, unit, scenes);
					notifier.scriptPages(project, map.get(days), shootDate,
							days, sceneNumberList, fileName);
				}
			}
		}
	}

	/**
	 * Determine all the scenes on a particular shooting day, and return a list
	 * of the scene numbers, plus a collection of the Scene objects.
	 *
	 * @param project The project containing the script.
	 * @param shootDay The shooting day number, which determines the scenes to
	 *            be formatted.
	 * @param scenes The List to which we will add each scene to be printed.
	 * @return A comma-delimited list of the scene numbers which have been
	 *         formatted.
	 */
	private String findScenesForDay(Project project, int shootDay, List<Scene> scenes) {
		Stripboard stripboard = project.getStripboard();
		Script script = project.getScript();
		List<Strip> strips = getStripDAO().findByShootDay(stripboard, project.getMainUnit(), shootDay);
		String sceneNumberList = "";
		for (Strip strip : strips) {
			List<String> sceneNumbers = strip.getScenes();
			for (String number : sceneNumbers) {
				Scene scene = getSceneDAO().findByScriptAndNumber(script, number);
				if (scene != null) {
					scenes.add(scene);
					sceneNumberList += ", " + number;
				}
			}
		}
		if (sceneNumberList.length() > 2) { // it should be, unless we have a shooting day with no scenes!
			sceneNumberList = sceneNumberList.substring(2);
		}
		else {
			sceneNumberList = null;
		}
		return sceneNumberList;
	}

	/**
	 * Create a PDF containing a print of the specified list of scenes.
	 *
	 * @param proj The Project whose current script has the scenes are to be
	 *            printed
	 * @param unit The unit involved; the unit name is passed to the report
	 *            generator, although this is not currently displayed on script
	 *            printouts.
	 * @param scenes The list of Scenes to print
	 * @return The fully-qualified filename of the PDF file created.
	 */
	private String generateFile(Project proj, Unit unit, List<Scene> scenes) {
		Script script = proj.getScript();
		ScriptReporter reporter = new ScriptReporter(script, scenes,
						0, Integer.MAX_VALUE); // set page range to avoid restrictions
		reporter.setFromSceneSeq(0);
		reporter.setToSceneSeq(Integer.MAX_VALUE);
		reporter.setDayRange(true);
//		reporter.setSidesStyle(true);
		String fileName = ReportUtils.generateAdvanceScript(reporter, proj, unit);
		return fileName;
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

	/** See {@link #projectMemberDAO}. */
	public ProjectMemberDAO getProjectMemberDAO() {
		if (projectMemberDAO == null) {
			projectMemberDAO = ProjectMemberDAO.getInstance();
		}
		return projectMemberDAO;
	}
	/** See {@link #projectMemberDAO}. */
	public void setProjectMemberDAO(ProjectMemberDAO projectMemberDAO) {
		this.projectMemberDAO = projectMemberDAO;
	}

	/** See {@link #sceneDAO}. */
	public SceneDAO getSceneDAO() {
		if (sceneDAO == null) {
			sceneDAO = SceneDAO.getInstance();
		}
		return sceneDAO;
	}
	/** See {@link #sceneDAO}. */
	public void setSceneDAO(SceneDAO sceneDAO) {
		this.sceneDAO = sceneDAO;
	}

	/** See {@link #stripDAO}. */
	public StripDAO getStripDAO() {
		if (stripDAO == null) {
			stripDAO = StripDAO.getInstance();
		}
		return stripDAO;
	}
	/** See {@link #stripDAO}. */
	public void setStripDAO(StripDAO stripDAO) {
		this.stripDAO = stripDAO;
	}

	/** See {@link #authBean}. */
	public AuthorizationBean getAuthBean() {
		return authBean;
	}
	/** See {@link #authBean}. */
	public void setAuthBean(AuthorizationBean authBean) {
		this.authBean = authBean;
	}

}
