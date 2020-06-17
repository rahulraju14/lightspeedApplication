package com.lightspeedeps.util.report;

import java.util.*;

import javax.faces.application.FacesMessage;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.dood.ProjectDood;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.UserCallInfo;
import com.lightspeedeps.type.ReportStatus;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.web.login.AuthorizationBean;

/**
 * Static utility methods for manipulating a callsheet, in particular
 * creating a new callsheet for a specified Date and Unit.
 */
public class CallSheetUtils {
	private static final Log log = LogFactory.getLog(CallSheetUtils.class);

	/** The string to use to separate individual status values in the cast list. */
	private static final String separator = " / ";

	/** The string which represents a cast member's status when they do not appear
	 * in an episode, and they are not on Hold. */
	private static final String missing = "--";

	private static Comparator<SceneCall> advanceScenecomparator = new Comparator<SceneCall>() {
		@Override
		public int compare(SceneCall one, SceneCall two) {
			return one.compareDate(two);
		}
	};

	private CallSheetUtils() {
		// prevent instantiation
	}

	/**
	 * Create a new callsheet for the specified date and Unit.
	 * @param date
	 * @param pUnit
	 * @return The new Callsheet, or null if the create process was not successful.
	 */
	public static Callsheet create(Date date, Unit pUnit) {
		Callsheet callsheet = new Callsheet();
		Project project = SessionUtils.getCurrentProject();
		Production prod = project.getProduction();
		boolean crossProject = prod.getType().getCrossProject();
		Callsheet priorCs = CallsheetDAO.getInstance().findPriorProject(date, project, pUnit.getNumber());
		if (priorCs == null) { // no match, see if any other, regardless of unit...
			priorCs = CallsheetDAO.getInstance().findPriorProject(date, project, null);
			if ((priorCs == null) && crossProject) { // no match, see if any other of same unit, regardless of project...
				priorCs = CallsheetDAO.getInstance().findPriorProduction(date, prod, pUnit.getNumber());
				if (priorCs == null) { // no match, see if any other, regardless of unit or project...
					priorCs = CallsheetDAO.getInstance().findPriorProduction(date, prod, null);
				}
			}
		}

		/** Map priority to crew departments listed */
		Map<Integer,DeptCall> deptCalls = new TreeMap<>();

		List<CastCall> castCalls = new ArrayList<>();
		//List<OtherCall> otherCalls = new ArrayList<OtherCall>();
		List<SceneCall> sceneCalls = new ArrayList<>();
		List<SceneCall> advanceSceneCalls = new ArrayList<>();

		// Set fixed values
		callsheet.setDate(date);
		callsheet.setTitle(prod.getTitle()); // default to production title; editable by user
		callsheet.setProject(project);
		callsheet.setEpisodeList("");
		List<Project> projects = new ArrayList<>(1);
		projects.add(project);
		callsheet.setProjects(projects);
		callsheet.setUnitNumber(pUnit.getNumber());
		callsheet.setTimeZone(prod.getTimeZone());
		callsheet.setStatus(ReportStatus.CREATED);
		callsheet.setShootDay(0); // we'll use the highest one we encounter

		// Set values from prior callsheet as "defaults"
		CateringLog cLog = null;
		if (priorCs != null) {
			callsheet.setAdvanceDays(priorCs.getAdvanceDays());
			callsheet.setCallTime( CalendarUtils.sameDateAs(date, priorCs.getCallTime()) );
			callsheet.setShootTime( CalendarUtils.sameDateAs(date, priorCs.getShootTime()) );
			callsheet.setRehearseTime( CalendarUtils.sameDateAs(date, priorCs.getRehearseTime()) );
			callsheet.setExecutives(priorCs.getExecutives());
			copyCommonCallNotes(priorCs, callsheet);
			if (priorCs.getCateringLog() != null) {
				cLog = priorCs.getCateringLog().clone();
			}
		}
		else {
			callsheet.setAdvanceDays(2);
			callsheet.setCallTime( CalendarUtils.setTime(date,9,0) );	// TODO how to set default call times?
			callsheet.setShootTime( CalendarUtils.setTime(date,10,30) );
			//callsheet.setRehearseTime(callsheet.getShootTime());
			callsheet.setExecutives("Producer:");
		}
		if (cLog == null) {
			cLog = new CateringLog();
		}
		callsheet.setCateringLog(cLog);

		if (callsheet.getCallTime() == null) {
			callsheet.setCallTime( CalendarUtils.setTime(date,9,0) );	// TODO how to set default call times?
		}

		Project commercialProject = null;
		if (prod.getType().hasPayrollByProject()) {
			commercialProject = project;
		}
		List<Department> depts = DepartmentDAO.getInstance().findByProductionCompleteCallsheet(prod, commercialProject);

		if (crossProject) {
			callsheet = mergeProjects(prod, projects, callsheet, castCalls,
					sceneCalls, advanceSceneCalls, deptCalls, depts);
		}
		else {
			List<ScriptElement> cast = new ArrayList<>();
			if (! processUnit(callsheet, pUnit, 0, false, castCalls, cast,
					sceneCalls, advanceSceneCalls, deptCalls, depts)) {
				callsheet = null;  // some problem - unit not added to callsheet!
			}
		}
		if (callsheet == null) { // error
			return null;
		}

		// Set collections that have been created
		Collections.sort(advanceSceneCalls, advanceScenecomparator); // order by date & line number
		int ix = 0; // now renumber advance scenes to keep them in sorted order!
		for (SceneCall sc : advanceSceneCalls) {
			sc.setLineNumber(ix++);
		}
		callsheet.setAdvanceSceneCalls(advanceSceneCalls);
		callsheet.setSceneCalls(sceneCalls);
		Collections.sort(castCalls);	// sort cast list by Cast Id
		callsheet.setCastCalls(castCalls);

		List<DeptCall> dcList = new ArrayList<>();
		for (DeptCall dc : deptCalls.values()) {
			dcList.add(dc);
		}
		callsheet.setDeptCalls(dcList);
		//callsheet.setOtherCalls(otherCalls);

		// Set current date & time as last-updated value
		callsheet.setUpdated(new Date());

		log.debug("callsheet: " + callsheet);
		return callsheet;
	}

	/**
	 * Determine if the given date is valid for creating a new Call Sheet.  It must
	 * be on or after the project start date, and there must not be an existing Call Sheet already
	 * created for that date.
	 * @param newDate The date to check.
	 * @return True if the date is valid, false if not.
	 */
	public static boolean validateDate(Date newDate, Unit unit) {
		boolean valid = true;
		Project project = unit.getProject();
		log.debug("date: " + newDate);
		// Checking if new date is beyond the schedule date
		if (newDate.before(unit.getProjectSchedule().getStartDate())) {
			// see if some other project might qualify...
			valid = false; // assume none qualify, then check them all
			Production prod = project.getProduction();
			prod = ProductionDAO.getInstance().refresh(prod);
			for (Project proj : prod.getProjects()) {
				if (proj.getStripboard() != null && proj.getScript() != null) {
					Unit pUnit = proj.getUnit(unit.getNumber());
					if (pUnit != null) {
						// (will be null if not all projects contain the particular Unit #)
						if (! pUnit.getProjectSchedule().getStartDate().after(newDate)) {
							// found unit with start-date before or equal to requested report date
							valid = true;
							break;
						}
					}
				}
			}
			if (! valid) {
				MsgUtils.addFacesMessage("Callsheet.DateEarly", FacesMessage.SEVERITY_ERROR);
			}
		}
		else {
			// Checking if call sheet exists for the selected new date
			Callsheet cs = CallsheetDAO.getInstance().findByDateProjectUnit(newDate, project, unit);
			if (cs != null) {
				MsgUtils.addFacesMessage("Callsheet.DateDuplicate", FacesMessage.SEVERITY_ERROR);
				valid = false;
			}
		}
		return valid;
	}

	/**
	 * Get the database id of the user's "current" Callsheet.
	 *
	 * @return The database id of the"current" Callsheet, or null if no such
	 *         Callsheet exists.
	 */
	public static Integer getCurrentCallsheetId() {
		log.debug("");
		UserCallInfo uci = getUserCallInfo();
		return uci.getCurrentCsId();
	}

	/**
	 * Get a current UserCallInfo object. If the UserCallInfo is not in the
	 * session, or has not been evaluated recently, then a newly-evaluated copy
	 * is returned (and also saved in the session).
	 *
	 * @return The current UserCallInfo (never null).
	 */
	public static UserCallInfo getUserCallInfo() {
		log.debug("");
		boolean buildUci = false;
		UserCallInfo uci = (UserCallInfo)SessionUtils.get(Constants.ATTR_USER_CALL_INFO);
		if (uci == null) {
			buildUci = true;
		}
		else {
			Project proj = SessionUtils.getCurrentProject();
			if (! uci.getProjectId().equals(proj.getId())) {
				buildUci = true;
			}
			else {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MINUTE, -Constants.MAX_EVAL_CALLSHEET_MINUTES);
				if (cal.getTime().after(uci.getLastEvaluated())) {
					buildUci = true;
				}
				else {
					Date updated = CallsheetDAO.getInstance().findLastUpdatedByProject(proj);
					if (updated != null && updated.after(uci.getLastEvaluated())) {
						buildUci = true;
					}
				}
			}
		}

		if (buildUci) {
			uci = createUserCallInfo();
			SessionUtils.put(Constants.ATTR_USER_CALL_INFO, uci);
		}

		return uci;
	}

	/**
	 * Build (or rebuild) a currently-evaluated UserCallInfo.
	 * @return the new UserCallInfo instance.
	 */
	private static UserCallInfo createUserCallInfo() {
		log.debug("");
		CallsheetDAO callsheetDAO = CallsheetDAO.getInstance();
		List<Callsheet> list;
		Callsheet bestCs = null;
		UserCallInfo uci = new UserCallInfo();
		Contact contact = SessionUtils.getCurrentContact();
		Project proj = contact.getProject();
		if (proj == null) { // may be Contact built via import or web service
			uci.setProjectId(-1);
			return uci;
		}
		uci.setProjectId(proj.getId());

		for (Unit unit : proj.getUnits()) {
			if (getInUnit(contact, unit)) {
				Integer csId = null;
				Date crewcall, usercall;
				Callsheet callsheet = null;
				list = callsheetDAO.findCurrentByProjectAndUnit(proj, unit);
				if (list != null && list.size() > 0) {
					callsheet = list.get(0);
					int n = 1;
					while (n < list.size()) {
						callsheet = pickBetterCallsheet(callsheet, list.get(n), contact);
						n++;
					}
				}
				if (callsheet != null) {
					csId = callsheet.getId();
					crewcall = callsheet.getCallTime();
					usercall = findCallTime(callsheet, contact);
					bestCs = pickBetterCallsheet(bestCs, callsheet, contact);
				}
				else {
					csId = null;
					crewcall = usercall = null;
				}
				uci.put(unit, csId, usercall, crewcall);
			}
		}
		if (bestCs != null) {
			uci.setCurrentCsId(bestCs.getId());
		}
		return uci;
	}

	/**
	 * Select the "better" of two callsheets, based on the given user's call times
	 * within the two callsheets, and the following rules:
	 * <p>
	 * 1. In general, the call sheet (CS) with the call time closest to the
	 * current time will be chosen.
	 * <p>
	 * 2. Use the CS for 4 hours after the call time has elapsed, unless a
	 * future call time takes precedence. (CS_PREFERRED_HOURS_USED)
	 * <p>
	 * 3. A CS is used for 15 minutes after its call time has elapsed, at an
	 * absolute minimum, no matter what. (CS_MINIMUM_MINUTES_USED)
	 * <p>
	 * 4. If a future CS has been published, it should be displayed as soon as
	 * possible, while respecting rules # 2 and 3 above.
	 * <p>
	 * 5. If a future call time occurs less than 9 hours from the current time,
	 * it takes precedence over rule #2. (MAX_HOURS_TO_PREFERRED_CALLSHEET)
	 * <p>
	 * 6. If two call times occur simultaneously, then the call time for the
	 * Unit with lowest Unit # wins. (Unit 1 takes precedence over Unit 2)
	 * <p>
	 * This method is public only so that it can be run from a JUnit test class.
	 * See {@link com.lightspeedeps.test.util.UtilitiesTest#testPickBetterCallsheet()}.
	 */
	public static Callsheet pickBetterCallsheet(Callsheet cs1, Callsheet cs2, Contact contact) {
		Callsheet preferCs = cs1; // prefer one picked already if no other differences
		if (cs1 == null) {	// first one not set yet
			log.debug("cs1 is null");
			preferCs = cs2;		// so use second & exit
		}
		else {
			// Get user call times, or crew-call if no user call time.
			Date user1 = findCallTime(cs1, contact);
			if (user1 == null)
				user1 = cs1.getCallTime();
			Date user2 = findCallTime(cs2, contact);
			if (user2 == null)
				user2 = cs2.getCallTime();
			log.debug("comparing " + user1 + "(" + cs1.getId() + ") vs " + user2+ "(" + cs2.getId() + ")");

			if (user1.getTime() == user2.getTime()) {
				// Equal - rule #6 - use lower unit number & exit
				log.debug("rule 6 (equal)");
				if (cs2.getUnitNumber() < cs1.getUnitNumber()) {
					log.debug("rule 6 (equal, cs2 has lower unit)");
					preferCs = cs2;
				}
			}
			else {
				Calendar now = Calendar.getInstance();
				log.debug(now.getTime());
				Calendar cal1 = (Calendar)now.clone(); // (cheaper than new instance)
				cal1.setTime(user1);					// cal1 = user call time for cs1
				Calendar cal2 = (Calendar)cal1.clone();
				cal2.setTime(user2);					// cal2 = user call time for cs2

				Calendar temp = (Calendar)cal1.clone();
				temp.add(Calendar.MINUTE, Constants.CS_MINIMUM_MINUTES_USED);
				if (now.after(cal1) && now.before(temp)) { // Rule #3
					log.debug("rule 3 (cs1 within minimum minutes)");
					//cs = cs1;						// use cs1 & exit
				}
				else {
					log.debug(temp.getTime());
					temp = (Calendar)cal2.clone();
					temp.add(Calendar.MINUTE, Constants.CS_MINIMUM_MINUTES_USED);
					if (now.after(cal2) && now.before(temp)) { // Rule #3
						log.debug("rule 3 (cs2 within minimum minutes)");
						preferCs = cs2;						// use cs2 & exit
					}
					else {
						temp.setTime(cal1.getTime());
						Calendar preferred = (Calendar)temp.clone();
						Calendar other = (Calendar)temp.clone();
						other.setTime(cal2.getTime());
						Callsheet otherCs = cs2;
						log.debug("default preferred=cs1");
						if (Math.abs(user1.getTime() - now.getTimeInMillis()) > Math.abs(user2.getTime() - now.getTimeInMillis())) {
							// Rule 1 favors cs2, so set it as "default" choice, and continue
							preferCs = cs2;
							preferred.setTime(cal2.getTime());
							otherCs = cs1;
							other.setTime(cal1.getTime());
							log.debug("rule 1 (preferred=cs2 (closer to 'now'))");
						}
						temp.setTime(preferred.getTime());
						temp.add(Calendar.HOUR, Constants.CS_PREFERRED_HOURS_USED);
						if (preferred.before(now) && temp.after(now)) {  // within rule #2 spec (use for 4 hours),
							// use "default" (cs/temp) unless overridden by rule #5
							other.add(Calendar.HOUR, - Constants.CS_WITHIN_HOURS_PREFERRED);
							if (other.before(now)) {
								log.debug("rule 5 (use other: future one within preferred hours)");
								preferCs = otherCs;
							}
							else {
								log.debug("rule 1 (use preferred)");
							}
						}
						else {
							if (preferred.before(now) && other.after(now)) {
								// 'preferred' is in the past & 'other' is in the future, apply rule #4
								preferCs = otherCs;
								log.debug("rule 4 (use other: past vs future)");
							}
							else {
								log.debug("rule 1 (use preferred)");
							}
						}
					}
				}
			}
		}
		log.debug("better cs id=" + preferCs.getId());
		return preferCs;
	}

	/**
	 * Generate callsheet information which contains data merged from multiple
	 * Project`s. We process projects where the appropriate Unit's start date is
	 * prior to the last day of the "Advance days" section.
	 *
	 * @param prod The Production for which the callsheet is being created.
	 * @param projects The List of Project`s which have been included, in the
	 *            given callsheet. Upon entry, this should contain only the
	 *            "main" Project. Upon return, it will contain all the Project`s
	 *            which had scenes shooting on the given callsheet's date.
	 * @param callsheet The callsheet being created.
	 * @param castCalls The list of CastCall items created for units that have
	 *            been added to this callsheet. The list will be empty when
	 *            called.
	 * @param sceneCalls The list of SceneCall items created for all projects
	 *            added to the callsheet. The list will be empty when called.
	 * @param advanceSceneCalls The list of SceneCall items for "advance" scenes
	 *            (to be shot in the future) created for all projects added to
	 *            the callsheet. The list will be empty when called.
	 * @param deptCalls The list of DeptCall items created for all projects
	 *            added to the callsheet. The list will be empty when called.
	 * @param depts
	 * @return The updated Callsheet, or null if no projects are found which
	 *         have a shooting day on the date of the callsheet.
	 */
	private static Callsheet mergeProjects(Production prod, List<Project> projects, Callsheet callsheet, List<CastCall> castCalls,
			List<SceneCall> sceneCalls, List<SceneCall> advanceSceneCalls,
			Map<Integer, DeptCall> deptCalls, List<Department> depts) {

		// may need to get advanced scene calls even if start date is in the future!
		Calendar cal = Calendar.getInstance();
		cal.setTime(callsheet.getDate());
		cal.add(Calendar.DAY_OF_MONTH, callsheet.getAdvanceDays());
		Date advanceDate = cal.getTime();

		Unit pUnit;
		int episodeCount = 0;
		String episodeList = "";
		List<ScriptElement> cast = new ArrayList<>();
		for (Project proj : prod.getProjects()) {
			if (proj.getStripboard() != null && proj.getScript() != null) {
				pUnit = proj.getUnit(callsheet.getUnitNumber());
				if (pUnit != null) {
					// (will be null if not all projects contain the particular Unit #)
					if (! pUnit.getProjectSchedule().getStartDate().after(advanceDate)) {
						if( processUnit(callsheet, pUnit, episodeCount, true, castCalls, cast,
								sceneCalls, advanceSceneCalls, deptCalls, depts) ) {
							// The Unit processed had scenes for this callsheet's date.
							episodeCount++;		// so bump our episode count
							if (! projects.contains(proj)) {
								// Add the episode "id" to the list of episodes
								projects.add(proj);
							}
							episodeList += ", " + proj.getEpisode();
						}
					}
				}
			}
		}
		Callsheet ret = null;
		if (episodeCount > 0) {
			ret = callsheet;
			episodeList = episodeList.substring(2);
			callsheet.setEpisodeList(episodeList);
			callsheet.setSceneCalls(sceneCalls);
			calculateTotalPages(callsheet);
		}
		return ret;
	}

	/**
	 * Add the information from a particular Unit to the callsheet collections
	 * provided. If the callsheet's date is not a shooting date for this Unit,
	 * then none of the information is updated except for, possibly, the
	 * advanceSceneCalls data.
	 *
	 * @param callsheet The Callsheet being created.
	 * @param pUnit The Unit whose information should be added to the callsheet;
	 *            this is used to determine the shooting schedule and the
	 *            relevant Project.
	 * @param episodeCount How many episodes have been added to this callsheet
	 *            so far. This is used for formatting the "status" field
	 *            properly for cast who appear on some, but not all, of the
	 *            episodes on this callsheet.
	 * @param merge True iff we are creating a "merged" callsheet from multiple
	 *            Project`s.
	 * @param castCalls The existing list of CastCall items created for units
	 *            that have already been added to this callsheet. The list will
	 *            be empty when called for the first unit being added.
	 * @param cast The list of ScriptElements representing CHARACTERS that
	 *            appear in the given unit's scenes on this callsheet's date.
	 * @param sceneCalls The existing list of SceneCall items created for units
	 *            that have already been added to this callsheet. The list will
	 *            be empty when called for the first unit being added.
	 * @param advanceSceneCalls The existing list of SceneCall items for
	 *            "advance" scenes created for units that have already been
	 *            added to this callsheet. The list will be empty when called
	 *            for the first unit being added.
	 * @param deptCalls The existing list of DeptCall items (which hold CrewCall
	 *            entries) created for units that have already been added to
	 *            this callsheet. The list will be empty when called for the
	 *            first unit being added.
	 * @param depts
	 * @return True iff the given unit has a shooting day matching the
	 *         callsheet's date.
	 */
	private static boolean processUnit(Callsheet callsheet, Unit pUnit, int episodeCount, boolean merge, List<CastCall> castCalls,
			List<ScriptElement> cast, List<SceneCall> sceneCalls, List<SceneCall> advanceSceneCalls,
			Map<Integer, DeptCall> deptCalls, List<Department> depts) {
		Project proj = pUnit.getProject();
		Stripboard stripboard = proj.getStripboard();
		Script script = proj.getScript();
		if (stripboard == null || script == null) {
			return false;	// can't create a callsheet yet!
		}
		int shootDay;

		ScheduleUtils schedUtils = new ScheduleUtils(pUnit);
		shootDay = schedUtils.findShootingDayNumber(callsheet.getDate());
		if (shootDay <= 0 && ! merge) { // this episode has not started shooting yet
			return false;				// (change this to allow off-day callsheets)
		}
		if (schedUtils.getEndDate().before(callsheet.getDate())) {
			return false; // already finished shooting
		}
		boolean ret = false;
		if (shootDay > 0) {
			ret = true;
			if (shootDay > callsheet.getShootDay()) {
				// use shoot-Day # & number of shooting days from project w/ highest shoot day #
				callsheet.setShootDay(shootDay);
				// Use "shooting days" count from main project:
				if (stripboard != null) {
					callsheet.setShootDays(stripboard.getShootingDays(pUnit));
				}
			}

			int pageLength = createSceneCalls(callsheet, pUnit, sceneCalls, stripboard, script, shootDay, cast);
			callsheet.setPages(Scene.formatPageLength(pageLength));

			// Add "include on all callsheets" ScriptElements to "cast" list
			List<ScriptElement> included = ScriptElementDAO.getInstance().findByProjectForAllCallsheets(proj);
			for (ScriptElement se : included) {
				if (! cast.contains(se)) {
					cast.add(se);
				}
			}

			createCastCalls(callsheet, pUnit, episodeCount, castCalls, cast);
			cast.clear();

			createDeptCalls(callsheet, pUnit, deptCalls, depts);
		}

		createAdvanceSceneCalls(callsheet, pUnit, schedUtils, advanceSceneCalls, stripboard, script, shootDay);

		return ret;
	}

	/**
	 * Copy some CallNote objects from "source" to "dest" -- we only copy
	 * those that are frequently the same from one callsheet to the next.
	 * @param source The source callsheet.
	 * @param dest The destination (target) callsheet.
	 */
	private static void copyCommonCallNotes(Callsheet source, Callsheet dest) {
		if (source.getCallNotes() == null) {
			return;
		}

		CallNote copy;
		Set<CallNote> notes = new HashSet<>();
		dest.setCallNotes(notes);
		for (CallNote cn : source.getCallNotes()) {
			if (cn.getSection() == Constants.CN_SECTION_TOP_MESSAGE1 ||
					cn.getSection() == Constants.CN_SECTION_TOP_MESSAGE2 ||
					(cn.getSection() >= Constants.CN_SECTION_CONTACT_MESSAGE &&
					cn.getSection() <= Constants.CN_SECTION_CONTACT_MESSAGE+3) ) {
				copy = (CallNote) cn.clone();
				copy.setCallsheet(dest);
				notes.add(copy);
			}
		}
	}

	/**
	 * Given a List of ScriptElements that are CHARACTERs, generate a CastCall
	 * object for each one, fill in the appropriate information, and add it to
	 * the castCalls List.
	 *
	 * @param callsheet
	 * @param episodeCount The number of episodes previously added to the
	 *            callsheet. This is used to construct the concatenated list of
	 *            cast Status values, because a cast member may not have
	 *            appeared (or been on Hold) for the first episode added to the
	 *            callsheet, so when this new CastCall is created, a status
	 *            placeholder ("--") must be put in the status list for the
	 *            preceding episodes.
	 * @param castCalls The existing list of CastCall items created for units
	 *            that have already been added to this callsheet. The list will
	 *            be empty when called for the first unit being added.
	 * @param cast The list of ScriptElements representing CHARACTERS that
	 *            appear in the given unit's scenes on this callsheet's date.
	 */
	private static void createCastCalls(Callsheet callsheet, Unit unit, int episodeCount, List<CastCall> castCalls, List<ScriptElement> cast) {
		CastCall castCall;
		Project project = unit.getProject();
		// force update of DooD information in case anything changed
		ProductionDood.updateProjectDood(unit);
		ProjectDood projectDood = ProductionDood.getProjectDood(unit);

		// mark all existing entries as "not updated" (so far) for this project:
		for (CastCall cc : castCalls) {
			cc.setUpdated(false);
		}

		Date date = callsheet.getCallTime();
		RealWorldElementDAO rwElementDAO = RealWorldElementDAO.getInstance();
		// First do all the Characters that are in the scenes being shot...
		for (ScriptElement se : cast) {
			castCall = null;
			Integer id = se.getElementId();
			if (id != null) { // supposed to always be non-null!
				for (CastCall cc : castCalls) {
					if (id.equals(cc.getActorId()) && se.getName().equals(cc.getCharacterName())) {
						castCall = cc;
						// matches existing entry (from prior episode); just need to update status...
						break;
					}
				}
			}
			// get work status using Dood
			String status = projectDood.getElementDood(se).getStatus(date).asWorkStatus();
			if (castCall == null) {
				// New entry - not added from previous episodes (if any)
				boolean include = true;
				RealWorldElement realchar = rwElementDAO.findLinkedRealWorldElement(se);
				if (realchar != null) {
					if (se.getIncludeOnCallsheet() && realchar.getActor() != null) {
						include = false; // if "forced" include, skip if no Unit membership
						empLoop:
						for (Employment emp : realchar.getActor().getEmployments()) {
							for (ProjectMember pm : emp.getProjectMembers()) {
								if (pm.getUnit() != null && pm.getUnit().equals(unit)) {
									include = true;
									if (status.equals(WorkdayType.OFF.asWorkStatus())) {
										status = WorkdayType.WORK.asWorkStatus();
									}
									break empLoop;
								}
							}
						}
					}
				}
				if (include) {
					castCall = new CastCall();
					castCalls.add(castCall);
					if (realchar != null) {
						castCall.setName(realchar.getName());
						castCall.setContactId(realchar.getActor().getId());
					}
					castCall.setCallsheet(callsheet);
					castCall.setActorIdStr(se.getElementIdStr());
					castCall.setActorId(se.getElementId());
					castCall.setCharacterName(se.getName());
					castCall.setMakeup(date);	// TODO set various call times appropriately
					castCall.setPickup(date);
					castCall.setRehearse(callsheet.getShootTime());
					castCall.setOnSet(callsheet.getShootTime());
					for (int i = episodeCount; i > 0; i--) {
						// add dashes for preceding episodes in which this character did not appear
						status = missing + separator + status;
					}
				}
			}
			else {
				status = castCall.getStatus() + separator + status;
			}
			if (castCall != null) {
				castCall.setStatus(status);
				castCall.setUpdated(true);
			}
		}
		// Then add any actors on "Hold" for this date
		List<ScriptElement> allSe = ScriptElementDAO.getInstance()
				.findByTypeAndProject(ScriptElementType.CHARACTER, project);
		for (ScriptElement se : allSe) {
			if (! cast.contains(se)) {
				WorkdayType type = projectDood.getElementDood(se).getStatus(date);
				if (type.equals(WorkdayType.HOLD)) {
					castCall = null;
					Integer id = se.getElementId();
					if (id != null) { // supposed to always be non-null!
						for (CastCall cc : castCalls) {
							if (id.equals(cc.getActorId()) && se.getName().equals(cc.getCharacterName())) {
								castCall = cc;
								break;
							}
						}
					}
					String status = type.asWorkStatus();
					if (castCall == null) {
						castCall = new CastCall();
						castCalls.add(castCall);
						castCall.setCallsheet(callsheet);
						castCall.setActorIdStr(se.getElementIdStr());
						castCall.setActorId(se.getElementId());
						castCall.setCharacterName(se.getName());
						RealWorldElement realchar = rwElementDAO.findLinkedRealWorldElement(se);
						if (realchar != null) {
							castCall.setName(realchar.getName());
							castCall.setContactId(realchar.getActor().getId());
						}
						for (int i = episodeCount; i > 0; i--) {
							status = missing + separator + status;
						}
					}
					else {
						status = castCall.getStatus() + separator + status;
					}
					castCall.setUpdated(true);
					castCall.setStatus(status);
				}
			}
		}

		// if any entry has not been updated for this project, add a
		// trailing "-" to their status field.
		for (CastCall cc : castCalls) {
			if (! cc.getUpdated()) {
				cc.setStatus(cc.getStatus() + separator + missing);
			}
		}
	}

	/**
	 * Create the List of SceneCall objects for the specified shootDay.
	 *
	 * @param callsheet The callsheet with which the SceneCall objects will be
	 *            associated.
	 * @param sceneCalls the List to which the new SceneCall instances will be
	 *            added.
	 * @param stripboard The Stripboard to be used.
	 * @param script The Script to be used.
	 * @param shootDay The shooting day number (origin one).
	 * @param cast A Collection of distinct ScriptElements representing cast
	 *            members; this is filled in during the process of creating the
	 *            SceneCall objects. Duplicates are not added.
	 * @return the total page length of all the Scene`s in the Strip`s for the
	 *         requested shooting day. (Page length is in number of eighths.)
	 */
	private static int createSceneCalls(Callsheet callsheet, Unit unit, List<SceneCall> sceneCalls,
			Stripboard stripboard, Script script, int shootDay, Collection<ScriptElement> cast) {
		List<Strip> strips = StripDAO.getInstance().findByShootDay(stripboard, unit, shootDay);
		int pageLength = 0;
		int line = sceneCalls.size();
		Project proj = unit.getProject();
		SceneCall sceneCall;
		ScriptElement location;
		RealWorldElement rlocation = null;
		Address addr;
		SceneDAO sceneDAO = SceneDAO.getInstance();
		RealWorldElementDAO rwElementDAO = RealWorldElementDAO.getInstance();
		for (Strip strip : strips) {
			List<String> sceneNumbers = strip.getScenes();
			List<Scene> scenes = sceneDAO.getScenes(script, sceneNumbers);
			for (Scene scene : scenes) {
				sceneCall = makeSceneCall(line, scene, cast);
				sceneCall.setCallsheetByCallsheetId(callsheet);
				sceneCall.setEpisode(proj.getEpisode());
				line++;
				pageLength += scene.getLength();
				location = scene.getScriptElement();
				rlocation = null;
				addr = null;
				if (location != null) {
					rlocation = rwElementDAO.findLinkedRealWorldElement(location);
					if (rlocation != null) {
						sceneCall.setLocation(rlocation.getName());
						addr = rlocation.getAddress();
					}
				}
				sceneCalls.add(sceneCall);

				sceneCall = new SceneCall();
				sceneCall.setCallsheetByCallsheetId(callsheet);
				sceneCall.setLineNumber(line++);
				sceneCall.setHeading(scene.getSynopsis());
				if (addr != null) {
					sceneCall.setLocation(addr.getCompleteAddress());
					if (callsheet.getSunrise() == null) {
						Calendar cals[] = LocationUtils.getSunriseSunset(addr, callsheet.getDate(),
								callsheet.getTimeZone());
						if (cals[0] != null) {
							callsheet.setSunrise( cals[0].getTime() );
						}
						if (cals[1] != null) {
							callsheet.setSunset( cals[1].getTime() );
						}
					}
				}
				sceneCalls.add(sceneCall);

				sceneCall = new SceneCall(); // Add a "blank line" for formatting & extra data entry room
				sceneCall.setCallsheetByCallsheetId(callsheet);
				sceneCall.setLineNumber(line++);
				sceneCalls.add(sceneCall);
			}
		}

		return pageLength;
	}

	/**
	 * Create entries for the "Advanced Scene" (schedule) table. The entries are added to the List
	 * given in the 'sceneCalls' parameter.
	 *
	 * @param callsheet
	 * @param unit
	 * @param schedUtils
	 * @param sceneCalls
	 * @param stripboard
	 * @param script
	 * @param shootDay
	 */
	private static void createAdvanceSceneCalls(Callsheet callsheet, Unit unit, ScheduleUtils schedUtils,
			List<SceneCall> sceneCalls, Stripboard stripboard, Script script, int shootDay) {
		// TODO needs work if we include units that haven't started shooting yet (shootday=0)
		int line = sceneCalls.size();
		SceneCall sceneCall;
		ScriptElement location;
		RealWorldElement rlocation;
		SceneDAO sceneDAO = SceneDAO.getInstance();
		StripDAO stripDAO = StripDAO.getInstance();
		RealWorldElementDAO rwElementDAO = RealWorldElementDAO.getInstance();
		Project proj = unit.getProject();

		for (int i = 0; i < callsheet.getAdvanceDays(); i++) {
			shootDay++;
			List<Strip> strips = stripDAO.findByShootDay(stripboard, unit, shootDay);
			Date date = schedUtils.findShootingDay(shootDay);
			for (Strip strip : strips) {
				List<String> sceneNumbers = strip.getScenes();
				List<Scene> scenes = sceneDAO.getScenes(script, sceneNumbers);
				for (Scene scene : scenes) {
					sceneCall = makeSceneCall(line++, scene, null);
					sceneCall.setCallsheetByAdvanceId(callsheet);
					sceneCall.setEpisode(proj.getEpisode());
					sceneCall.setDate(date);
					sceneCall.setDayNumber(shootDay);
					location = scene.getScriptElement();
					if (location != null) {
						rlocation = rwElementDAO.findLinkedRealWorldElement(location);
						if (rlocation != null) {
							sceneCall.setLocation(rlocation.getName());
						}
					}
					sceneCalls.add(sceneCall);
				}
				// TODO include location addresses somewhere
			}
		}
		return;
	}

	/**
	 * Build most of a SceneCall object based on the scene and other related
	 * objects passed. This method is used for creating regular sceneCalls and
	 * Advance SceneCalls in the callsheet.
	 *
	 * @param line
	 * @param scene
	 * @param cast A List of ScriptElements representing cast members; new
	 *            elements will be added to this list as cast members are
	 *            processed for the scene.
	 * @return the new SceneCall instance.
	 */
	private static SceneCall makeSceneCall(int line, Scene scene, Collection<ScriptElement> cast) {
		SceneCall sceneCall = new SceneCall();
		sceneCall.setLineNumber(line);
		sceneCall.setNumber(scene.getNumber());
		sceneCall.setHeading(scene.getHeading());
		String daynight = scene.getDnType().toString().substring(0, 1);
		if (scene.getScriptDay() != null) {
			daynight += "-" + scene.getScriptDay();
			if (daynight.length() > SceneCall.MAX_LENGTH_DAY_NIGHT) {
				daynight = daynight.substring(0, SceneCall.MAX_LENGTH_DAY_NIGHT);
			}
		}
		sceneCall.setDayNight(daynight);
		sceneCall.setPageLength(scene.getLength());
		sceneCall.setPages(scene.getPageLength());
		Set<ScriptElement> elements = scene.getScriptElements();
		// Get an ordered list of elementIds for all the CHARACTER elements in the scene
		String ids = ScriptElementDAO.createElementIdString(elements, true);
		if (ids.length() > 100) {
			ids = ids.substring(0, 100);
		}
		sceneCall.setCastIds(ids);

		if (cast != null) {
			// accumulate a list of distinct CHARACTER elements for today's scenes:
			for (ScriptElement se : elements) {
				if (se.getType() == ScriptElementType.CHARACTER) {
					if (! cast.contains(se)) {
						cast.add(se);
					}
				}
			}
		}
		return sceneCall;
	}

	/**
	 * Create all the Department/crew call entries. The DAO query used,
	 * 'findAllByPriority', only returns departments with a positive
	 * listPriority, which automatically avoids LS Admin and any other
	 * "departments" which are not meant to be included on the call sheet.
	 *
	 * @param c The callsheet being updated/created.
	 * @param deptCalls A Map of DeptCall entities that have been created for
	 *            this call sheet. This method will use existing entries, and
	 *            add new entries where a matching entry not already exist.
	 * @param depts The list of Department`s to be included on the call sheet.
	 *            This will NOT include any Department whose "showOnCallsheet"
	 *            attribute has been set to false by the production staff.
	 */
	private static void createDeptCalls(Callsheet c, Unit unit, Map<Integer, DeptCall> deptCalls, List<Department> depts) {
		DeptCall dc;
		List<CrewCall> crewCalls;
		for (Department dept : depts) {
			dc = deptCalls.get(dept.getListPriority());
			if (dc == null) {
				dc = new DeptCall(c, dept);
				crewCalls = null;
			}
			else {
				crewCalls = dc.getCrewCalls();
			}
			if (crewCalls == null) {
				crewCalls = new ArrayList<>();
			}
			createCrewCalls(c, unit, dc, crewCalls);
			// Include Catering department even if no crew exists;
			// however, it will NOT be included if it has showOnCallsheet=false.
			boolean isCatering = false;
			if ( dept.getId().equals(Constants.DEPARTMENT_ID_CATERING)) {
				isCatering = true;
			}
			else {
				Integer id = dept.getStandardDeptId();
				if (id != null && id.equals(Constants.DEPARTMENT_ID_CATERING)) {
					isCatering = true;
				}
			}
			if (crewCalls.size() > 0 || isCatering) {
				dc.setCrewCalls(crewCalls);
				deptCalls.put(dept.getListPriority(), dc);
			}
		}
		// add the user-editable department entry; it prints last
// rev 2770 - temporarily turn off user-dept entry code
//		dc = createUserDeptCall(c);
//		deptCalls.add(dc);
	}

//	protected static DeptCall createUserDeptCall(Callsheet cs) {
//		DeptCall dc = new DeptCall(cs, "", Integer.MAX_VALUE);
//		List<CrewCall> crewCalls = new ArrayList<CrewCall>(2);
//		crewCalls.add(new CrewCall(dc, 0, null, "", "", null));
//		crewCalls.add(new CrewCall(dc, 1, null, "", "", null));
//		dc.setCrewCalls(crewCalls);
//		return dc;
//	}

	/**
	 * Create the crew call entries for a specific department.  All the call times
	 * are set to the current call sheet's CallTime. The entries are ordered by
	 * the Role.listPriority value.
	 * @param c
	 * @param dc
	 * @param crewCalls
	 */
	private static void createCrewCalls(Callsheet c, Unit unit, DeptCall dc, List<CrewCall> crewCalls) {
		int line = crewCalls.size();
		Department pmDept = dc.getDepartment();
		if (dc.getDepartment().getStandardDeptId() != null) {
			// For standard departments, projectMembers always refer to the SYSTEM version of the Department
			pmDept = DepartmentDAO.getInstance().findById(dc.getDepartment().getStandardDeptId());
		}

		List<ProjectMember> members = ProjectMemberDAO.getInstance().findByUnitAndDepartment(unit, pmDept);
		log.debug(dc.getDepartment().getName() + ": crew=" + members.size());

		boolean match;
		CrewCall cc;
		for (ProjectMember mbr : members) {
			if (mbr.getEmployment().getRole().getListPriority() > 0) {
				Contact contact = mbr.getEmployment().getContact();
				match = false;
				for (CrewCall cx : crewCalls) {
					if (cx.getContactId().equals(contact.getId()) &&
							cx.getRoleName().equals(mbr.getEmployment().getRole().getName())) {
						match = true;
						break;
					}
				}
				if (! match) {
					cc = new CrewCall(dc, line++, 1, mbr.getEmployment().getRole().getName(),
							contact.getUser().getAnyName(), c.getCallTime());
					cc.setContactId(contact.getId());
					cc.setPriority(mbr.getEmployment().getRole().getListPriority().shortValue());
					crewCalls.add(cc);
				}
			}
		}
	}

	/**
	 * Find the CURRENT user's call time from the given callsheet.
	 *
	 * @param callsheet
	 * @param contact The currently logged-in Contact; if null, then null is
	 *            returned.
	 * @return Call time, or null if not found
	 */
	private static Date findCallTime(Callsheet callsheet, Contact contact) {
		Date date = null;
		if (contact != null) {
			log.debug("contact="+contact.getId());
			AuthorizationBean auth = AuthorizationBean.getInstance();
			if (auth.isCastOrStunt()) {
				if (contact.getCastMember() != null) {
					String castName = contact.getCastMember().getName();
					for (CastCall cc : callsheet.getCastCalls()) {
						if (castName.equals(cc.getName())) {
							date = cc.getPickup(); // TODO is this the right call time for cast?
							log.debug("cast time match=" + date);
							break;
						}
					}
				}
			}
			if (auth.isCrewNotStunt()) {
				String crewName = contact.getUser().getAnyName();
				Integer id = null;
				id = contact.getId();
				if (date == null && crewName != null) {
					for (DeptCall dc : callsheet.getDeptCalls()) {
						for (CrewCall cc : dc.getCrewCalls()) {
							if ((cc.getContactId() != null && cc.getContactId().equals(id)) ||
									crewName.equalsIgnoreCase(cc.getName())) {
								date = cc.getTime();
								log.debug("crew time match=" + date);
								break;
							}
						}
					}
				}
			}
		}
		return date;
	}

	/**
	 * Calculate the total number of pages listed in the SceneCall records, and
	 * update the callsheet's total page count.
	 *
	 * @param cs The callsheet to update.
	 */
	public static void calculateTotalPages(Callsheet cs) {
		int pageLen = 0;
		for (SceneCall sc : cs.getSceneCalls()) {
			if (sc.getPages() != null && sc.getPages().length() > 0) {
				pageLen += Scene.convertPageLength(sc.getPages());
			}
		}
		cs.setPages(Scene.formatPageLength(pageLen));
	}

	/**
	 * Determine how to spread the department/crew listings across 3 columns.
	 * Used by online call sheet display, and to build parameters for Jasper
	 * report.
	 */
	public static void createDeptColumns(Callsheet callsheet, List<DeptCall> deptCalls[]) {
		int totalCcs = 0; // compute total number of lines of output
		for (DeptCall dc : callsheet.getDeptCalls()) {
			if (dc.getCrewCalls() != null && dc.getCrewCalls().size() > 0) {
				// add extra "1" for the dept heading
				totalCcs += dc.getCrewCalls().size() + 1;
			}
		}
		int limit = (int)(0.32 * (totalCcs+1)); // 32% for first column
		log.debug("total ccs=" + totalCcs + ", limit=" + limit);
		int colnum = 0;
		int ccs = 0;
		deptCalls[0] = new ArrayList<>();
		deptCalls[1] = new ArrayList<>();
		deptCalls[2] = new ArrayList<>();
		for (DeptCall dc : callsheet.getDeptCalls()) {
			if (dc.getCrewCalls() != null /* && dc.getCrewCalls().size() > 0*/) {
				deptCalls[colnum].add(dc);
				ccs += dc.getCrewCalls().size() + 1;
				if (ccs >= limit) { // enough for one column,
					colnum++;	// ...jump to next
					if (colnum == 1) { // recalc limit for 2nd column
						limit = (int)(0.47 *(totalCcs - ccs + 1)); // 47% of what's left
					}
					else {
						limit = totalCcs; // no limit for last column
					}
					ccs = 0;	// ...and reset counter
				}
			}
		}
	}

	/**
	 * Determine if this User has a role (projectMember) in the
	 * specified Unit.
	 * @param unit
	 * @return True if one of the User's roles is in the given
	 * Unit; false otherwise.
	 */
	@Transient
	public static boolean getInUnit(Contact ct, Unit unit) {
		for (Employment emp : ct.getEmployments()) {
			List<ProjectMember> pmList = ProjectMemberDAO.getInstance().findByProperty("employment", emp);
			for (ProjectMember pm : pmList) {
				if (pm.getUnit() == null || pm.getUnit().getId().equals(unit.getId())) {
					return true;
				}
			}
		}
		return false;
	}

}
