//	File Name:	ProjectDood.java
package com.lightspeedeps.dood;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.model.DateRange;
import com.lightspeedeps.model.RealLink;
import com.lightspeedeps.model.RealWorldElement;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.model.Strip;
import com.lightspeedeps.model.Stripboard;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.type.RealLinkStatus;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.project.ScheduleUtils;

/**
 * The ProjectDood object holds all the DooD information for one project.
 * This consists of a Map of ElementDood objects, one for each ScriptElement
 * in the project.
 */
public class ProjectDood implements Serializable {
	/** */
	private static final long serialVersionUID = 8928169550033701567L;

	private static final Log log = LogFactory.getLog(ProjectDood.class);

//	private Integer unitId;
	private boolean dirty = false;

	/** If true, DooD refresh should update 'dropToUse' in Script Elements. */
	private boolean updateDrops;

	/**
	 * The projectDood map contains one ElementDood for each Script Element,
	 * keyed by the ScriptElement id.
	 */
	private Map<Integer,ElementDood> projectDood = null;

	/**
	 * The projectDood map contains one ElementDood for each Real World Element,
	 * keyed by the RealWorldElement id.
	 */
	private Map<Integer,ElementDood> projectRwDood = null;

	/**
	 * The normal constructor.
	 */
	public ProjectDood() {
		projectDood = new Hashtable<Integer,ElementDood>();
		projectRwDood = new Hashtable<Integer,ElementDood>();
	}

	/**
	 * This creates the DooD information for all the ScriptElements, by cycling
	 * through all the dates beginning with the project schedule's start date,
	 * and continuing until the number of shooting days (from the current
	 * strip-board) has been accounted for.
	 *
	 * @return True iff the data was successfully created. The process will fail
	 *         and return false if there is no current stripboard or current
	 *         script for the project.
	 */
	public boolean createDood(Unit unit) {
		boolean bRet = true;
		ScriptElementDAO seDAO = ScriptElementDAO.getInstance();
		SceneDAO sceneDAO = SceneDAO.getInstance();
		StripDAO stripDAO = StripDAO.getInstance();
		Script script = unit.getProject().getScript(); // current script for this project
		Stripboard stripboard = unit.getProject().getStripboard(); // current stripboard for this project

//		unitId = unit.getId();

		if (script == null || stripboard == null) {
			return false;
		}

		ScheduleUtils schedBean = new ScheduleUtils(unit);

		// Get all the Script elements for this project
		List<ScriptElement> seList = seDAO.findByProject(unit.getProject());

		// Start with an empty map
		projectDood = new Hashtable<Integer,ElementDood>(2*seList.size());

		// A map to save scene appearances of each element, keyed by element id
		Map<Integer,List<String>> sceneLists = new Hashtable<Integer,List<String>>(2*seList.size());

		ElementDood eDood, crewDood;
		WorkdayType status = null;
		int shootDay;
		Calendar cal = new GregorianCalendar();
		Date date;

		cal.setTime(unit.getProjectSchedule().getStartDate());

		int totalShootingDays = stripboard.getShootingDays(unit);
		/** holds the set of ScriptElement.id values for the elements in use on a single day */
		Set<Integer> seInUse = new HashSet<Integer>();
		log.debug("script=" + script.getId() + ": " + script);
		log.debug("board=" + stripboard.getId() + ": "  + stripboard);
		log.debug("elems=" + seList.size() + ", days=" + totalShootingDays + ", start=" + cal.getTime());

		// Add a fresh ElementDood to our map for every script element.
		for (ScriptElement se : seList) {
			eDood = new ElementDood(se);
			setElementDood(se,eDood);
			sceneLists.put(se.getId(), new ArrayList<String>());
		}
		// A single ElementDood handles all Crew assignments
		crewDood = new ElementDood(Constants.CREW_SCRIPT_ELEMENT_ID);
		setElementDood(Constants.CREW_SCRIPT_ELEMENT_ID, crewDood);

		// Cycle through the calendar until we get enough shooting days
		for (shootDay = 0; shootDay < totalShootingDays; ) {
			date = cal.getTime();
			status = schedBean.findDayType(date);
			log.debug("status("+date+")="+status);
			if (status.equals(WorkdayType.WORK)) {
				shootDay++;
				// have a working day ... we'll need list of Script elements in use.
				// get list of strips for this day
				List<Strip> strips = stripDAO.findByShootDay(stripboard, unit, shootDay);
				// determine Scenes & script elements for those strips
				seInUse.clear();
				for (Strip strip : strips) {
					List<String> sceneNumbers = strip.getScenes();
					List<Scene> scenes = sceneDAO.getScenes(script, sceneNumbers);
					//log.debug("strip=" + strip.getOrderNumber() + ", scn=" + strip.getSceneNumbers());
					for (Scene scene : scenes) {
						//log.debug("scene="+scene.getId());
						// then get Script elements for those scenes
						Set<ScriptElement> elements = scene.getScriptElements();
						for (ScriptElement se : elements) {
							//log.debug("se="+se.getId());
							seInUse.add(se.getId());
							// G&G added check to avoid NullPointException, but NPE would indicate logic
							// error, as every SE in project should have an entry in sceneLists map.
							//if (sceneLists.get(se.getId())!=null) {
							sceneLists.get(se.getId()).add(scene.getNumber());
						}
						// Track set (location) status, too.
						if (scene.getScriptElement() != null) {
							seInUse.add(scene.getScriptElement().getId());
							sceneLists.get(scene.getScriptElement().getId()).add(scene.getNumber()); // set
						}
					}
				}
				log.debug("date=" + date + ", strips=" + strips.size() + ", SE in use=" + seInUse.size());
			}
			// First add the status to our dummy Crew DooD entry
			crewDood.setNextStatus(date, status);

			// Set the status for this date on each script element
			for (ScriptElement se : seList) {
				WorkdayType tStatus = status;
				if (tStatus == WorkdayType.WORK && ! seInUse.contains(se.getId())) {
					// have a working day ... but this Script element is not needed
					tStatus = WorkdayType.NOT_NEEDED;
				}
				// setNextStatus will take care of "START" setting
				getElementDood(se).setNextStatus(date, tStatus);
			}
			cal.add(Calendar.DAY_OF_MONTH, 1); // get next day
		}

		// Do final pass of ScriptElements & their ElementDood's to set number of scenes
		// in which the element appears (and lists of scenes), and to let ElementDood set
		// appropriate "FINISH", "PICKUP", "HOLD" and "DROP" settings
		for (ScriptElement se : seList) {
			ElementDood elemDood = getElementDood(se);
			// Get list of SCRIPT scenes -- scenes using this element, regardless of schedule
			List<Scene> scenes = seDAO.getOccurs(se, script);
			elemDood.setScriptOccurs(scenes.size());
			List<String> sceneList = new ArrayList<String>();
			for (Scene scene : scenes) {
				sceneList.add(scene.getNumber());
			}
			elemDood.setScriptSceneList(sceneList);

			// Save list of SCHEDULED scenes -- scenes scheduled in stripboard that use this element
			List<String> scheduled = sceneLists.get(se.getId());
			elemDood.setScheduledSceneList(scheduled);
			elemDood.setScheduledOccurs(scheduled.size());

			// Let ElementDood finalize dates, status, counts, drop/pickup info, etc.
			elemDood.setLastStatus(updateDrops);
		}
		// ...and do final status update for Crew entry
		crewDood.setLastStatus(false);
		return bRet;
	}

	public ElementDood getElementDood(ScriptElement scriptElement) {
		return getElementDood(scriptElement.getId());
	}

	public ElementDood getElementDood(Integer scriptElementId) {
		ElementDood dood = null;
		if (projectDood != null) {
			dood = projectDood.get(scriptElementId);
		}
		return dood;
	}

	public void setElementDood(ScriptElement scriptElement, ElementDood dood) {
		projectDood.put(scriptElement.getId(), dood);
	}

	private void setElementDood(Integer scriptElementId, ElementDood dood) {
		projectDood.put(scriptElementId, dood);
	}

	/**
	 * Add a new ScriptElement to the projectDooD table.
	 * @param se
	 */
	public void addElement(ScriptElement se) {
		setElementDood(se, new ElementDood(se));
	}

	/**
	 * Get the DooD information for a particular RW Element as it relates to
	 * this Unit. If the ElementDood object does not exist AND the RW element is
	 * linked to at least one ScriptElement, then the ElementDood will be
	 * created.
	 * <p>
	 * If the RW Element is linked to multiple ScriptElement`s, then the date
	 * range for the RW Element will be the "union" of the date ranges for all
	 * of the linked ScriptElement`s.
	 *
	 * @param rwe The RealWorldElement whose DooD information is needed.
	 * @return The current ElementDood information.
	 */
	public ElementDood getElementDood(RealWorldElement rwe) {
		ElementDood dood = null;
		try {
			if (projectRwDood != null) {
				dood = projectRwDood.get(rwe.getId());
				if (dood == null && projectDood != null && rwe.getRealLinks() != null
						&& rwe.getRealLinks().size() > 0) {
					log.debug("create RW DooD, elem=" + rwe.getName());
					dood = new ElementDood(rwe.getId());
					Date first = null,
						last = null;
					ElementDood seDood;
					Date firstBlackout = null, lastBlackout = null;
					boolean chkBlackout = rwe.getDateRanges() != null && rwe.getDateRanges().size() > 0;
					if (chkBlackout) { // compute bounding range of blackout dates
						for (DateRange dr : rwe.getDateRanges()) {
							if (firstBlackout == null || firstBlackout.after(dr.getStartDate())) {
								firstBlackout = dr.getStartDate();
							}
							if (lastBlackout == null || lastBlackout.before(dr.getEndDate())) {
								lastBlackout = dr.getEndDate();
							}
						}
					}
					for (RealLink rl : rwe.getRealLinks()) {
						if (rl.getStatus() == RealLinkStatus.SELECTED) {
							ScriptElement se = rl.getScriptElement();
							if (se != null && se.getId() != null) {
								seDood = getElementDood(se.getId());
								if (seDood != null) {
									if (first == null ||
											(seDood.getFirstWorkDate() != null && first.after(seDood.getFirstWorkDate()))) {
										first = seDood.getFirstWorkDate();
									}
									if (last == null ||
											(seDood.getLastWorkDate() != null && last.before(seDood.getLastWorkDate()))) {
										last = seDood.getLastWorkDate();
									}
									if (chkBlackout && (! dood.getDateFlag()) &&
											seDood.getFirstWorkDate() != null &&
											lastBlackout != null && firstBlackout != null &&
											(! lastBlackout.before(seDood.getFirstWorkDate())) &&
											(! firstBlackout.after(seDood.getLastWorkDate())) ) {
										// have some overlap between blackout ranges and work dates
										Date bad = checkDates(rwe.getDateRanges(), seDood);
										if (bad != null) { // found a date conflict
											dood.setDateFlag(true);
											dood.setConflictDate(bad);
										}
									}
								}
							}
						}
					}
					dood.setFirstWorkDate(first);
					dood.setLastWorkDate(last);
					setElementDood(rwe, dood);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return dood;
	}

	/**
	 * Find a date that occurs in the supplied date ranges which is a working
	 * date for an element.
	 *
	 * @param dateRanges A List of DateRange`s representing the "black out"
	 *            dates for a given RealWorldElement.
	 * @param seDood The ElementDood instance for the ScriptElement being
	 *            checked against.
	 * @return A Date on which a conflict occurs between the blackout date
	 *         ranges and the working dates documented in the seDood item; null
	 *         if no conflicting date is found.
	 */
	private Date checkDates(List<DateRange> dateRanges, ElementDood seDood) {
		Date badDate = null;
		try {
			for (DateRange dr : dateRanges) {
				// do quick check on start/end of range (avoid Calendar code)
				if (seDood.getStatus(dr.getStartDate()).isWork()) {
					badDate = dr.getStartDate();
					break;
				}
				if (seDood.getStatus(dr.getEndDate()).isWork()) {
					badDate = dr.getEndDate();
					break;
				}
			}

			if (badDate == null) { // no match yet, try more detailed comparison
				for (DateRange dr : dateRanges) {
					Calendar start = new GregorianCalendar();
					Calendar end = new GregorianCalendar();
					start.setTime(dr.getStartDate());
					start.add(Calendar.DATE, 1); // already checked the starting date
					end.setTime(dr.getEndDate());
					while (start.before(end)) {
						if (seDood.getStatus(start.getTime()).isWork()) {
							badDate = start.getTime();
							break;
						}
						start.add(Calendar.DATE, 1);
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}

		return badDate;
	}

	public void setElementDood(RealWorldElement element, ElementDood dood) {
		if (dood == null) {
			projectRwDood.remove(element.getId());
		}
		else {
			projectRwDood.put(element.getId(), dood);
		}
	}

	/**
	 * Mark the DooD information for a specific RealWorldElement invalid. This
	 * is do ensure that the information will be recreated the next time it is
	 * requested. (See ProjectDood.getElementDood.)
	 *
	 * @param element The element whose information is no longer valid.
	 */
	public static void markDirty(RealWorldElement element) {
		ProductionDood.markDirty(SessionUtils.getCurrentProject(), element);
	}


	public boolean isDirty() {
		return dirty;
	}
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/** See {@link #updateDrops}. */
	public boolean getUpdateDrops() {
		return updateDrops;
	}
	/** See {@link #updateDrops}. */
	public void setUpdateDrops(boolean updateHolds) {
		updateDrops = updateHolds;
	}

	/** See {@link #projectDood}. */
	public Map<Integer,ElementDood> getSeMap() {
		return projectDood;
	}

	/** See {@link #projectRwDood}. */
	public Map<Integer,ElementDood> getRwMap() {
		return projectRwDood;
	}

	/**
	 * Returns a listing of the DooD status for all the ScriptElements
	 * in this project.
	 */
	@Override
	public String toString() {
		String s = "projectDood: \n";
		for (Integer id : projectDood.keySet()) {
			s += getElementDood(id).toString() + "\n";
		}
		return s;
	}

}
