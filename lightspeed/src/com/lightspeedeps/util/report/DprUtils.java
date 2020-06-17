package com.lightspeedeps.util.report;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.DeptTime;
import com.lightspeedeps.type.ReportStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.project.ScheduleUtils;

/**
 * Utility methods for the DPR report, particularly creating a new DPR.
 */
public class DprUtils {
	private static final Log log = LogFactory.getLog(DprUtils.class);

	private final CallsheetDAO callsheetDAO = CallsheetDAO.getInstance();

	Project project = SessionUtils.getCurrentProject();

	public DprUtils() {
	}

	public Dpr create(Date date) {
		Dpr dpr = null;
		try {
			dpr = new Dpr();
			Dpr priorDpr = findPriorDpr(date);
			List<Callsheet> csList = callsheetDAO.findByDateAndProject(date, project);
			Callsheet cs = null;
			if (csList.size() > 0) {
				cs = csList.get(0); // this is the Callsheet with the lowest unitNumber
			}

			List<DprScene> dprScenes = new ArrayList<>();

			// Set fixed values
			dpr.setDate(date);
			dpr.setProject(project);
			dpr.setStatus(ReportStatus.CREATED);

			dpr.setStartDate(project.getMainUnit().getProjectSchedule().getStartDate());
			dpr.setEndDate(project.getOriginalEndDate());

			if (project.getStripboard() != null) {
				ScheduleUtils s = new ScheduleUtils(project.getMainUnit());
				dpr.setRevisedEndDate(s.getEndDate());
				dpr.setShootDay(s.findShootingDayNumber(date));
				dpr.setShootDays(project.getStripboard().getShootingDays(project.getMainUnit()));
			}
			else {
				dpr.setRevisedEndDate(dpr.getEndDate());
				dpr.setShootDay(1);
				dpr.setShootDays(1);
			}

			CateringLog priorCl = null;

			// Set values from callsheet
			if (cs != null) {
				dpr.setCrewCall(cs.getCallTime());
				dpr.setShootingCall(cs.getShootTime());
				dpr.setFirstShot(cs.getShootTime());
				dpr.setProducer(cs.getExecutives());
				priorCl = cs.getCateringLog();
				Set<CallNote> notes = cs.getCallNotes();
				for (CallNote cn : notes) {
					switch (cn.getSection()) {
					case 21:
						dpr.setContact1(cn.getBody());
						break;
					case 22:
						dpr.setContact2(cn.getBody());
						break;
					case 23:
						dpr.setContact3(cn.getBody());
						break;
					case 24:
						dpr.setContact4(cn.getBody());
						break;
					default:
					}
				}
			}
			else if (priorDpr != null) {
				dpr.setProducer(priorDpr.getProducer());
				dpr.setContact1(priorDpr.getContact1());
				dpr.setContact2(priorDpr.getContact2());
				dpr.setContact3(priorDpr.getContact3());
				dpr.setContact4(priorDpr.getContact4());
				dpr.setCrewCall(priorDpr.getCrewCall());
				dpr.setShootingCall(priorDpr.getShootingCall());
				if (priorDpr.getCateringLogs() != null && priorDpr.getCateringLogs().size() > 0) {
					priorCl = priorDpr.getCateringLogs().iterator().next();
				}
			}
			else {
				dpr.setProducer(createProducerString(dpr));
				dpr.setContact1(findStaffName(RoleDAO.UNIT_PM, true));
				dpr.setContact2(findStaffName(RoleDAO.FIRST_AD, true));
				dpr.setContact3(findStaffName(RoleDAO.SECOND_AD, true));
			}

			CateringLog cl;
			if (priorCl == null) {
				cl = new CateringLog();
			}
			else {
				cl = priorCl.clone();
			}
			cl.setDpr(dpr);
			Set<CateringLog> cateringLogs = new HashSet<>();
			cateringLogs.add(cl);
			dpr.setCateringLogs(cateringLogs);

			// Set values from prior dpr as "defaults"
			if (priorDpr != null) {
				dpr.setOvertimeUsedPrior(priorDpr.getOvertimeUsedTotal());
				dpr.setOvertimeRemaining(priorDpr.getOvertimeRemaining());
			}
			else {
				dpr.setOvertimeUsedPrior(new BigDecimal(0));
				dpr.setOvertimeRemaining(new BigDecimal(0));
			}

			// create various collections
			dpr.setCastTimeCards(ReportTimeUtils.createCastTimecards(dpr.getDate()));
			for (ReportTime tc : dpr.getCastTimeCards()) {
				tc.setDprCast(dpr);
			}
			dpr.setCrewTimeCards(ReportTimeUtils.createCrewTimeCards(dpr));

			// Set values from random timecard
			ReportTime tc = null;
			if (dpr.getCrewTimeCards().size() > 0) {
				tc = dpr.getCrewTimeCards().iterator().next();
			}
			else if (dpr.getCastTimeCards().size() > 0) {
				tc = dpr.getCastTimeCards().iterator().next();
			}
			if (tc != null) {
				dpr.setFirstMealBegin(tc.getFirstMealStart());
				dpr.setFirstMealEnd(tc.getFirstMealEnd());
				dpr.setSecondMealBegin(tc.getSecondMealStart());
				dpr.setSecondMealEnd(tc.getSecondMealEnd());
			}

			if (csList.size() > 0) {
				createDprScript(dpr, priorDpr, csList);
				createScenes(dpr, dprScenes, csList);
			}
			else {
				createScenes(dpr, dprScenes);
			}

			dpr.setDprScenes(dprScenes);

			createDprDays(dpr, priorDpr);

			createDprExtras(dpr, csList);


			log.debug("Dpr: " + dpr);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			dpr = null; // so we don't return a half-done DPR
		}
		return dpr;
	}

	/**
	 * Determine how to spread the department/crew listings across 3 columns.
	 * Used by online DPR display, and to build parameters for Jasper
	 * report.
	 */
	public static void createDeptColumns(Dpr dpr, List<DeptTime> deptTime[]) {
		Map<Integer,DeptTime> allDepartments = new TreeMap<>();
		DeptTime cateringDt = null;
		// First group crew timecards into DeptTime objects, by department.
		// By using a TreeMap, with the listPriority as key, we automatically
		// get the ordering we need for output.
		if (dpr != null && dpr.getCrewTimeCards() != null) {
			for (ReportTime tc : dpr.getCrewTimeCards()) {
				log.debug(tc.getId());
				Department dept = tc.getDepartment();
				Integer priority = dept.getListPriority();
				DeptTime dt = allDepartments.get(priority);
				if (dt == null) {
					dt = new DeptTime(dept);
					allDepartments.put(priority, dt);
					if (dept.getId().equals(Constants.DEPARTMENT_ID_CATERING) ||
							(dept.getStandardDeptId() != null &&
							dept.getStandardDeptId().equals(Constants.DEPARTMENT_ID_CATERING))) {
						cateringDt = dt; // remember the Catering department entry
					}
				}
				dt.getTimeCards().add(tc);
			}
		}
		if (cateringDt == null) { // If not there yet, add it, to show the meal count info.
			Production prod = SessionUtils.getProduction();
			Project project = null;
			if (prod.getType().hasPayrollByProject()) {
				project = SessionUtils.getCurrentProject();
			}
			Department dept = DepartmentDAO.getInstance()
					.findByProductionStandardId(prod, project, Constants.DEPARTMENT_ID_CATERING);
			if (dept.getShowOnDpr()) {
				cateringDt = new DeptTime(dept);
				allDepartments.put(dept.getListPriority(), cateringDt);
			}
		}
		// Now compute total number of lines of output, and sort TimeCard Lists
		int totalLines = 3; // accounts for "craft services" lines added in JSF
		for (DeptTime dt : allDepartments.values()) {
			if (dt.getTimeCards() != null /*&& dt.getTimeCards().size() > 0*/) {
				// add extra "2" for the dept heading and column heading
				totalLines += dt.getTimeCards().size() + 2;
				Collections.sort(dt.getTimeCards(), ReportTime.priorityComparator);
			}
		}
		// Now split the departments into 3 groups, attempting to balance length of columns
		int limit = (int)(0.32 * (totalLines+1)); // 32% for first column
		log.debug("total ccs=" + totalLines + ", limit=" + limit);
		int colnum = 0;
		int lines = 0;
		deptTime[0] = new ArrayList<>();
		deptTime[1] = new ArrayList<>();
		deptTime[2] = new ArrayList<>();
		for (DeptTime dt : allDepartments.values()) {
			if (dt.getTimeCards() != null /*&& dt.getTimeCards().size() > 0*/) {
				deptTime[colnum].add(dt);
				lines += dt.getTimeCards().size() + 2;
				if (lines >= limit) { // enough for one column,
					colnum++;	// ...jump to next
					if (colnum == 1) { // recalc limit for 2nd column
						limit = (int)(0.47 *(totalLines - lines + 1)); // 47% of what's left
					}
					else {
						limit = totalLines; // no limit for last column
					}
					lines = 0;	// ...and reset counter
				}
			}
		}
	}

	private String createProducerString(Dpr dpr) {
		String text = "";
		String name = findStaffName(RoleDAO.PRODUCER, false);
		if (name != null) {
			text += RoleDAO.PRODUCER + ": " + name + Constants.NEW_LINE;
		}
		name = findStaffName(RoleDAO.DIRECTOR, false);
		if (name != null) {
			text += RoleDAO.DIRECTOR + ": " + name + Constants.NEW_LINE;
		}
		return text;
	}

	/**
	 * Create the DprDays objects for this Dpr. These describe the number of
	 * days assigned to various categories, e.g., shooting, rehearsal, travel,
	 * etc.
	 *
	 * @param dpr The Dpr being created.
	 * @param priorDpr The most recent previous DPR, or null if none is
	 *            available.
	 */
	private void createDprDays(Dpr dpr, Dpr priorDpr) {
		if (priorDpr != null) {
			DprDays days = (DprDays)priorDpr.getDprDaysScheduled().clone();
			dpr.setDprDaysScheduled(days);
			days = (DprDays)priorDpr.getDprDaysToDate().clone();
			dpr.setDprDaysToDate(days);
		}
		else {
			dpr.setDprDaysScheduled(new DprDays());
			dpr.setDprDaysToDate(new DprDays());
		}
		BigDecimal[] daysScheduled = dpr.getDprDaysScheduled().getUnits();
		BigDecimal[] daysToDate = dpr.getDprDaysToDate().getUnits();
		for (Unit u : project.getUnits()) {
			// Set active units' value = 0, and inactive = null --
			// this will control the display of the units on the DPR View page.
			if (u.getActive()) {
				if (daysScheduled[u.getNumber()] == null) {
					daysScheduled[u.getNumber()] = BigDecimal.ZERO; // DprDays.ZERO_DAYS;
				}
				if (daysToDate[u.getNumber()] == null) {
					daysToDate[u.getNumber()] = BigDecimal.ZERO; // DprDays.ZERO_DAYS;
				}
			}
			else {
				daysScheduled[u.getNumber()] = null;
				daysToDate[u.getNumber()] = null;
			}
		}
		dpr.getDprDaysScheduled().setUnits(daysScheduled);
		dpr.getDprDaysToDate().setUnits(daysToDate);
	}

	/**
	 * For each project/episode included on the DPR, initialize the information on script
	 * progress -- scenes, pages, and minutes shot today, to-date, etc.
	 * @param dpr
	 * @param priorDpr
	 * @param csList
	 */
	private void createDprScript(Dpr dpr, Dpr priorDpr, List<Callsheet> csList) {
		List<Project> projects = new ArrayList<>();

		projects.add(dpr.getProject());
		createEpisode(dpr, dpr.getProject());

		for (Callsheet cs : csList) {
			for (Project p : cs.getProjects()) {
				if (! projects.contains(p)) {
					projects.add(p); // keep track so we only process each project once
					createEpisode(dpr, p);
				}
			}
		}
	}

	/**
	 * Create list of Extras information from Callsheet entries.
	 *
	 * @param dpr The DPR being created.
	 * @param csList The List of Callsheet`s for the same date as the DPR.
	 */
	private void createDprExtras(Dpr dpr, List<Callsheet> csList) {
		List<ExtraTime> exTimes = new ArrayList<>();
		dpr.setExtraTimes(exTimes);
		ExtraTime ext;
		int line = 0;
		for (Callsheet cs : csList) {
			for (OtherCall oc : cs.getOtherCalls()) {
				ext = new ExtraTime();
				ext.setDpr(dpr);
				ext.setCount(oc.getCount());
				ext.setDescription(oc.getDescription());
				ext.setLineNumber(line++);
				ext.setCallTime(CalendarUtils.convertTimeToDecimal(oc.getTime(), false));
				exTimes.add(ext);
			}
		}
	}


	/**
	 * Create the list of scenes to be shown on the DPR. The list is derived
	 * based on the contents of the provided list of callsheets.
	 *
	 * @param dpr The DPR being created.
	 * @param dprScenes A List of DprScene`s to which this method will add the
	 *            newly-created DprScene objects describing the scenes shot
	 *            during this day.
	 * @param csList The List of Callsheet`s matching the date of the DPR. If
	 *            this list is empty, then the scene list will be created based
	 *            on the script and stripboard information in the Production.
	 */
	private static void createScenes(Dpr dpr, List<DprScene> dprScenes, List<Callsheet> csList) {

		DprScene dprScene;
		int lineNum = 0;
		// We need to create a list of Scene numbers by project/episode during this
		// process. Note that one callsheet may have scenes from multiple episodes.
		Map<String,String> episodeScenes = new HashMap<>();

		// There may be multiple Callsheet`s if the project has multiple Unit`s.
		for (Callsheet callsheet : csList) {
			String sceneNumbers = "";
			for (SceneCall scl : callsheet.getSceneCalls()) {
				if (scl.getNumber() != null && scl.getNumber().trim().length() > 0) {
					dprScene = new DprScene();
					dprScene.setDpr(dpr);
					dprScene.setLineNumber(lineNum++);
					dprScene.setSceneNumber(scl.getNumber());
					dprScene.setEpisode(scl.getEpisode());
					dprScene.setTitle(scl.getHeading());
					String locStr = scl.getLocation();
					if (RealWorldElementDAO.getLocationId(scl) != null) {
						RealWorldElement loc = RealWorldElementDAO.getInstance().findById(scl.getLocationId());
						if (loc != null) {
							locStr = loc.getName();
							Address addr = loc.getAddress();
							if (addr != null) {
								locStr += ", " + addr.getTwoLineAddress();
							}
						}
					}
					dprScene.setLocation(locStr);
					dprScenes.add(dprScene);
					// update list of scene numbers for this episode
					sceneNumbers = episodeScenes.get(dprScene.getEpisode());
					if (sceneNumbers == null) {
						sceneNumbers = scl.getNumber();
					}
					else {
						sceneNumbers += ", " + scl.getNumber();
					}
					episodeScenes.put(dprScene.getEpisode(), sceneNumbers);
				}
			}
		}
		for (Entry<String, String> entry : episodeScenes.entrySet()) {
			DprEpisode episode = findEpisode(dpr, entry.getKey());
			if (episode != null) {
				episode.setCompletedScenes(entry.getValue());
			}
		}

	}

	/**
	 * Create the list of scenes for the DPR from the script and stripboard
	 * information of all projects that have shooting days on the day of the
	 * given DPR. This is similar to the code used to generate a callsheet's
	 * list of scenes.
	 *
	 * @param dpr The DPR being created.
	 * @param dprScenes A List of DprScene`s to which this method will add the
	 *            newly-created DprScene objects describing the scenes shot
	 *            during this day.
	 */
	private static void createScenes(Dpr dpr, List<DprScene> dprScenes) {
		Production prod = dpr.getProject().getProduction();
		if (prod.getType().getCrossProject()) {
			for (Project proj : prod.getProjects()) {
				createScenesFromProject(dpr, dprScenes, proj);
			}
		}
		else {
			createScenesFromProject(dpr, dprScenes, dpr.getProject());
		}
	}

	/**
	 * Create the list of scenes for the DPR from the script and stripboard
	 * information of the given project, if it has a shooting day on the day of
	 * the given DPR.
	 *
	 * @param dpr The DPR being created.
	 * @param dprScenes The List of DprScene`s to be filled in with scene
	 *            information.
	 * @param proj The Project to be used for the source of scene information.
	 * @return A String containing the scene numbers separated by commas.
	 */
	private static String createScenesFromProject(Dpr dpr, List<DprScene> dprScenes, Project proj) {

		StripDAO stripDAO = StripDAO.getInstance();
		SceneDAO sceneDAO = SceneDAO.getInstance();
		RealWorldElementDAO rwElementDAO = RealWorldElementDAO.getInstance();
		DprScene dprScene = null;
		String sceneNumbers = "";
		int lineNum = dprScenes.size();
		Script script = proj.getScript();
		Stripboard stripboard = proj.getStripboard();
		if (stripboard != null && script != null) {
			for (Unit unit : proj.getUnits()) {
				if (! unit.getProjectSchedule().getStartDate().after(dpr.getDate())) {
					ScheduleUtils schedUtils = new ScheduleUtils(unit);
					int shootDay = schedUtils.findShootingDayNumber(dpr.getDate());
					if (shootDay > 0) {
						List<Strip> strips = stripDAO.findByShootDay(stripboard, unit, shootDay);
						for (Strip strip : strips) {
							List<String> sceneNumberList = strip.getScenes();
							List<Scene> scenes = sceneDAO.getScenes(script, sceneNumberList);
							for (Scene sc : scenes) {
								if (sc.getNumber() != null && sc.getNumber().trim().length() > 0) {
									dprScene = new DprScene();
									dprScene.setDpr(dpr);
									dprScene.setLineNumber(lineNum++);
									dprScene.setSceneNumber(sc.getNumber());
									dprScene.setEpisode(proj.getEpisode());
									sceneNumbers += ", " + sc.getNumber();
									dprScene.setTitle(sc.getHeading());
									String locStr = "";
									ScriptElement location = sc.getScriptElement();
									if (location != null) {
										RealWorldElement loc = rwElementDAO.findLinkedRealWorldElement(location);
										if (loc != null) {
											locStr = loc.getName();
											Address addr = loc.getAddress();
											if (addr != null) {
												locStr += ", " + addr.getTwoLineAddress();
											}
										}
									}
									dprScene.setLocation(locStr);
									dprScenes.add(dprScene);
								}
							}
						}
					}
				}
			}
			if (dprScene != null) { // at least one scene from this project was added to DPR
				DprEpisode episode = createEpisode(dpr, proj);
				episode.setCompletedScenes(sceneNumbers.substring(2));
			}
		}
		return sceneNumbers;
	}

	/**
	 * Create a DprEpisode instance for the given Project, and add it to the
	 * given DPR. This method creates the ScriptMeasure objects owned by the
	 * DprEpisode.
	 *
	 * @param dpr The Dpr to which the DprEpisode will be added.
	 * @param proj The Project that the DprEpisode corresponds to.
	 * @return The newly-created DprEpisode.
	 */
	private static DprEpisode createEpisode(Dpr dpr, Project proj) {
		DprEpisode episode = new DprEpisode();
		episode.setDpr(dpr);
		episode.setProject(proj);
		dpr.getDprEpisodes().add(episode);
		DprEpisode priorEpisode = DprEpisodeDAO.getInstance().findPrior(dpr.getDate(), proj);
		ScriptMeasure smScript = null;
		ScriptMeasure smShot = null;
		if (priorEpisode != null) {
			smScript = priorEpisode.getScriptMeasureRevisedTotal();
			smScript = smScript.clone();
			smShot = DprEpisode.getScriptMeasureShotToDate(priorEpisode);
			episode.setSoundUsedPrior(priorEpisode.getSoundUsedTotal());
		}
		else {
			smScript = new ScriptMeasure();
			if (proj.getScript() != null) {
				smScript.setScenes(proj.getScript().getScenes().size());
				smScript.setPages(proj.getScript().getLastPage() * 8);
			}
			smShot = new ScriptMeasure();
		}
		episode.setScriptMeasurePriorTotal(smScript); // prior = yesterdays total, if available
		episode.setScriptMeasureShotPrior(smShot);

		smScript = smScript.clone();
		episode.setScriptMeasureRevisedTotal(smScript); // revised total = prior total, until user changes it.

		episode.setScriptMeasureShot(new ScriptMeasure());
		episode.setScriptMeasureShotToDate(new ScriptMeasure());
		return episode;
	}

	/**
	 * Find a particular DprEpisode within the list of them held in the
	 * given Dpr.
	 *
	 * @param dpr
	 * @param match
	 * @return The DprEpisode whose "episode" value matches the given string.
	 */
	private static DprEpisode findEpisode(Dpr dpr, String match) {
		DprEpisode episode = null;
		for (DprEpisode ep : dpr.getDprEpisodes()) {
			if (ep.getProject().getEpisode().equals(match)) {
				episode = ep;
				break;
			}
		}
		return episode;
	}

	/**
	 * Find the DPR for the most recent date preceding the given date, which
	 * matches the current project.
	 *
	 * @param date
	 * @return The preceding DPR, or null if none exists.
	 */
	private Dpr findPriorDpr(Date date) {
		Dpr dpr = null;
		final DprDAO dprDAO = DprDAO.getInstance();
		dpr = dprDAO.findPrior(date, project);
		return dpr;
	}

	/**
	 * Find the Contact name that matches a particular role name in the
	 * current project.
	 * @param roleName
	 * @return The contact's "DisplayName", if found; otherwise, if
	 * 'makeDefault' is false, returns null; if 'makeDefault' is true,
	 * returns the roleName surrounded by square brackets.
	 */
	private String findStaffName(String roleName, boolean makeDefault) {
		String name = null;
		if (makeDefault) {
			name = "[" + roleName + "]";
		}
		Contact c = RoleDAO.findContactByRole(project.getMainUnit(), roleName);
		if (c != null) {
			name = c.getDisplayName();
		}
		return name;
	}

}
