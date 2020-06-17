//	File Name:	StripDAO.java
package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Note;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.Strip;
import com.lightspeedeps.model.Stripboard;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.model.UnitStripboard;
import com.lightspeedeps.object.StripBoardScene;
import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.type.StripType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.script.SceneNumber;

/**
 * A data access object (DAO) providing persistence and search support for Strip
 * entities. Transaction control of the save(), update() and delete() operations
 * can directly support Spring container-managed transactions or they can be
 * augmented to handle user-managed Spring transactions. Each of these methods
 * provides additional information for how to configure it for the desired type
 * of transaction control.
 *
 * @see com.lightspeedeps.model.Strip
 */

public class StripDAO extends BaseTypeDAO<Strip> {
	private static final Log log = LogFactory.getLog(StripDAO.class);
	// property constants
//	private static final String DTYPE = "dtype";
//	private static final String ORDER_NUMBER = "orderNumber";
//	private static final String TYPE = "type";
//	private static final String STATUS = "status";
//	private static final String COMMENT = "comment";
//	private static final String SHEET_NUMBER = "sheetNumber";
//	private static final String SCENE_NUMBERS = "sceneNumbers";
//	private static final String LENGTH = "length";
//	private static final String SYNOPSIS = "synopsis";
//	private static final String SEQUENCE = "sequence";
//	private static final String ELAPSED_TIME = "elapsedTime";
//	private static final String STRIPBOARD = "stripboard";

	/** standard increment between orderNumber values */
	public static final int ORDER_INCREMENT = 100000;
	public static final int ORDER_MAXIMUM = Integer.MAX_VALUE-1;

	public static StripDAO getInstance() {
		return (StripDAO)getInstance("StripDAO");
	}

//	public static StripDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (StripDAO) ctx.getBean("StripDAO");
//	}

	/***
	 * Find all Strips with the specified StripStatus that are part of the given
	 * Stripboard, sorted by ascending orderNumber.
	 *
	 * @param status
	 * @param stripboard
	 * @return A possibly empty (but not NULL) List of Strip's matching the
	 *         parameters given.
	 */
	@SuppressWarnings("unchecked")
	public List<Strip> findByStatusAndStripboard(StripStatus status, Stripboard stripboard) {
		Object [] values={stripboard,status};
		String queryString = "from Strip s where s.stripboard = ? and s.status = ? order by orderNumber";
		return find(queryString, values);
	}

	/**
	 * Find all Strips associated with the given Unit that are part of the given
	 * Stripboard, sorted by ascending orderNumber.  Note that only SCHEDULED Strips are
	 * included, since non-SCHEDULED strips are not associated with a specific unit.
	 *
	 * @param unit Only Strip's assigned to this Unit will be returned.
	 * @param stripboard The Stripboard containing the Strip's to be searched.
	 * @return A possibly empty (but not NULL) List of Strip's matching the
	 * parameters given.
	 */
	@SuppressWarnings("unchecked")
	public List<Strip> findByUnitAndStripboard(Unit unit, Stripboard stripboard) {
		Object [] values = {stripboard, unit.getId()};
		String queryString = "from Strip s where s.stripboard = ? and s.unitId = ? order by orderNumber";
		return find(queryString, values);
	}

	/**
	 * Returns all scheduled, unscheduled, and omitted breakdown strips for a
	 * given stripboard, in random order.
	 *
	 * @param stripboard The stripboard whose strips are to be returned.
	 * @return A possibly empty, but never null, List of Strip`s of StripType BREAKDOWN.
	 */
	@SuppressWarnings("unchecked")
	public List<Strip> findAllBreakdownStrips(Stripboard stripboard) {
		Object[] values = { stripboard, StripType.BREAKDOWN };
		String queryString = " from Strip strip where stripboard = ? and type = ?";
		return find(queryString, values);
	}

	/**
	 * Returns a List of Strips of type "BREAKDOWN" (so it excludes end-of-day and Banner strips),
	 * ordered by the corresponding scene sequence numbers.
	 * <p>
	 * *NOTE* This does not currently support multi-scene strips!
	 *
	 * @param board The stripboard for the strips desired.
	 * @return List of Strips, which may be empty, but is not null.
	 */
/*	@SuppressWarnings("unchecked")
	public List<Strip> findBreakdownOrderByScene(Stripboard board, Script script) { // fix if multi-scene strips required
		try {
			Object[] values = { board, StripType.BREAKDOWN, script };
			String queryString = "select s from Strip s, Scene scn where s.stripboard = ? and s.type = ?" +
					" and s.sceneNumbers = scn.number and scn.script = ? order by scn.sequence";
			return find(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}
*/
	/**
	 * Update the strip scene numbers
	 *
	 */
/*	public void resequenceStripScenes(Integer id,List<SceneListViewerBean> sceneViewList,String sceneIndex) {
		int indexNumber = Integer.parseInt(sceneIndex) - 1;
		List<Integer> list = new ArrayList<Integer>();
		for (int index = 0; index < sceneViewList.size(); index++) {
			if (index >= indexNumber) {
				SceneListViewerBean stripBean = sceneViewList.get(index);
				if (list.size() == 0 || !list.contains(stripBean.getId())) {
					list.add(stripBean.getId());
					Strip strip = this.findById(stripBean.getId());
					String sceneNumber = strip.getScenesIncremented();
					// update the scene numbers by one
					strip.setSceneNumbers(sceneNumber);
					merge(strip);
				}
			}
		}
	}
*/

	/**
	 * Find the maximum orderNumber of all Strips in the given stripboard with the specified status,
	 * regardless of Strip type.
	 *
	 * @param stripboard The stripboard of interest.
	 * @param status The strip.status value to be selected on.
	 * @return The highest numeric value (maximum) of all the orderNumber fields in the Strips of
	 *         'stripboard' with matching status. Returns 0 if the Stripboard contains no Strips
	 *         with the given status.
	 */
	@SuppressWarnings("unchecked")
	private int findMaxOrderNumber(Stripboard stripboard, StripStatus status) {
		log.debug("status=" + status + ", stripboard id=" + stripboard.getId());
		int maximum = 0;
		final String FIND_MAX_STATUS_STRIPBOARD =
				"select max(s.orderNumber) from Strip s " +
				"where s.status = ? and s.stripboard = ? " +
				" and s.type <> '" + StripType.END_STRIPBOARD.name() + "'";
		Object[] values = { status, stripboard };
		List<Integer> result = null;
		result = find(FIND_MAX_STATUS_STRIPBOARD, values);
		if (result != null && result.size() > 0 && result.get(0) != null) {
			maximum = result.get(0).intValue();
		}
		log.debug("max="+maximum);
		return maximum;
	}

	/**
	 * Find the maximum orderNumber of all Unscheduled Strips in the given stripboard, of any type.
	 *
	 * @param stripboard The stripboard of interest.
	 * @return The highest numeric value (maximum) of all the orderNumber fields in the Strips of
	 *         'stripboard'. Returns 0 if the Stripboard contains no Strips.
	 */
	public int findMaxUnscheduledOrderNumber(Stripboard stripboard) {
		return findMaxOrderNumber(stripboard, StripStatus.UNSCHEDULED);
	}

	/**
	 * Find the maximum sheetNumber for all the strips in 'stripboard'.
	 *
	 * @param stripboard The Stripboard of interest.
	 * @return The highest sheetNumber value for all the Strips in the given stripboard. Returns 0
	 *         if there are no Strips in the stripboard.
	 */
	@SuppressWarnings("unchecked")
	private int findMaxSheetNumber(Stripboard stripboard) {
		int maximum = 0;
		final String FIND_MAX_SHEETNUMBER = "select max(s.sheetNumber) from Strip s where s.stripboard = ?";
		List<Integer> result = null;
		log.debug("finding max sheetNumber in stripboard " + stripboard.getId());
		try {
			result = find(FIND_MAX_SHEETNUMBER, stripboard);
			if (result != null && result.size() > 0 && result.get(0) != null) {
				Integer n = result.get(0);
				maximum = n;
			}
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		log.debug("max=" + maximum);
		return maximum;
	}

//	@Transactional
//	public Strip mergeNext(Stripboard stripboard, Integer prevSheetNumber, Integer nextSheetNumber) {
//		log.debug("board="+stripboard.getId()+", prev="+prevSheetNumber+", next="+nextSheetNumber);
//		Strip prev = null;
//		if (prevSheetNumber != null) {
//			prev = findBySheetNumberAndStripboard(prevSheetNumber, stripboard);
//			if (nextSheetNumber != null) {
//				Strip next = findBySheetNumberAndStripboard(nextSheetNumber, stripboard);
//				if (prev != null && next != null) {
//					int elapsed = (prev.getElapsedTime()==null ? 0 : prev.getElapsedTime());
//					elapsed += (next.getElapsedTime()==null ? 0 : next.getElapsedTime());
//					prev.setElapsedTime(elapsed);
//					prev.setLength(prev.getLength()+next.getLength());
//					update(prev);
//					stripboard.getStrips().remove(next);
//					delete(next);
//				}
//			}
//		}
//		return prev;
//	}

//	@SuppressWarnings("unchecked")
//	private Strip findBySheetNumberAndStripboard(Integer number, Stripboard stripboard) {
//		Strip strip = null;
//		try {
//			Object[] values = { number, stripboard };
//			String queryString = "from Strip where sheetNumber = ? and stripboard = ?";
//			log.debug("board=" + stripboard.getId() + ", sheet=" + number);
//			List<Strip> list = (List<Strip>) find(queryString, values);
//			if (list != null && list.size() > 0) {
//				strip = list.get(0);
//			}
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//		return strip;
//	}

	/**
	 * Return a list of Strips that correspond to a particular day of shooting.
	 *
	 * @param stripboard The stripboard to use in getting the list of strips.
	 * @param unit Only Strip's assigned to this Unit will be returned.
	 * @param shootDay The shooting day number, origin 1, for which the list of
	 *            Strips is to be returned.
	 * @return A list of those Strips that are scheduled in the specified
	 *         stripboard for the given shooting day (sequential) number. Never
	 *         returns null, but will return an empty list if no Strips are
	 *         scheduled for the specified day; e.g., if the shootDay is greater
	 *         than the number of shooting days in the stripboard, or if
	 *         shootDay is less than 1.
	 */
	public List<Strip> findByShootDay(Stripboard stripboard, Unit unit, int shootDay) {
		return findByShootDay(stripboard, unit, shootDay, false);
	}

	/**
	 * Return a list of Strips that correspond to a particular day of shooting.
	 *
	 * @param stripboard The stripboard to use in getting the list of strips.
	 * @param unit Only Strip's assigned to this Unit will be returned.
	 * @param shootDay The shooting day number, origin 1, for which the list of
	 *            Strips is to be returned.
	 * @param includeEOD If True, then the End-of-Day strip which ends the
	 *            requested shooting day will be included in the List of Strips.
	 * @return A list of those Strips that are scheduled in the specified
	 *         stripboard for the given shooting day (sequential) number. Never
	 *         returns null, but will return an empty list if no Strips are
	 *         scheduled for the specified day; e.g., if the shootDay is greater
	 *         than the number of shooting days in the stripboard, or if
	 *         shootDay is less than 1.
	 */
	public List<Strip> findByShootDay(Stripboard stripboard, Unit unit, int shootDay, boolean includeEOD) {
		log.debug("get strips for day# "+shootDay);
		List<Strip> result = new ArrayList<Strip>();
		// TODO efficiency - first retrieve just the EndOfDay strips, and use the orderNumber values in the
		// bracketing EOD records to then retrieve just the strips matching the request.
		for (Strip strip : findByUnitAndStripboard(unit, stripboard)) {
			if (strip.getType() == StripType.END_OF_DAY) {
				shootDay--;
				if (shootDay < 1) {
					if (includeEOD) {
						result.add(strip);
					}
					break;
				}
			}
			if (shootDay == 1 && strip.getType() == StripType.BREAKDOWN) {
				result.add(strip);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public int findShootDayNumber(Strip strip) {
		int count = 0;
		String queryString = "select count(*) from Strip" +
				" where stripboard = ? and " +
				"unitId = ? and " +
				"type = '" + StripType.END_OF_DAY + "' and " +
				"orderNumber < ?";
		Object[] values = {strip.getStripboard(), strip.getUnitId(), strip.getOrderNumber()};
		List<Long> list = (List<Long>)find(queryString, values);
		if (list.size() > 0) { // # of EODs preceding the strip
			count = list.get(0).intValue() + 1; // so shooting-day number is result + 1
		}
		return count;
	}

	/**
	 * Find the Strip associated with a specific scene and stripboard.  The Strip returned
	 * may be associated with multiple scenes; if so, one of them has the requested scene number.
	 * @param sceneNumber The specific (single) scene number to be matched.
	 * @param stripboard The stripboard containing the strips to be searched.
	 * @return The matching Strip or null if not found.
	 */
	public Strip findBySceneAndStripboard(String sceneNumber, Stripboard stripboard) {
		Object[] values = { stripboard };
		String queryString = "from Strip where stripboard = ? and (";
		queryString += "sceneNumbers = '" + sceneNumber + "' ";
		queryString += " or sceneNumbers like '%," + sceneNumber + "' ";
		queryString += " or sceneNumbers like '" + sceneNumber + ",%' ";
		queryString += " or sceneNumbers like '%," + sceneNumber + ",%' ";
		queryString += ")";
		Strip strip = findOne(queryString, values);
		return strip;
	}

	/**
	 * Find all strips assigned the given scene number(s) string, and associated with any stripboard
	 * in the given project. This is used for (a) finding and deleting "orphan" strips --
	 * ones associated with a scene number that no longer exists in the project; and (b) finding
	 * strips that need to be updated for a Merge operation (from the Breakdown page).
	 *
	 * @param sceneNumbers The 'sceneNumbers' string to match in the Strip object.
	 * @param project The project whose strips (related via stripboards) are to be searched.
	 * @return a non-null List
	 */
	@SuppressWarnings("unchecked")
	private List<Strip> findBySceneNumberAndProject(String sceneNumbers, Project project) {
		Object[] values = { sceneNumbers, project };
		String queryString = "from Strip s where s.sceneNumbers = ? and s.stripboard.project = ?";
		return find(queryString, values);
	}

	/**
	 * Mark a set of Strips as Omitted. The Strips marked will be those associated with the given
	 * "sceneNumbers" string from all stripboards in the given project.  The updated Strips will
	 * have sequence numbers higher than any existing Omitted Strip, so they will appear at the
	 * end of the Omitted list.
	 *
	 * @param sceneNumbers The 'sceneNumbers' field to match in the Strips. This may be a single
	 *            scene number, or a list of scene numbers separated by commas in the case of merged
	 *            scenes.
	 * @param project The project whose stripboards will be searched for the Strips in question.
	 */
	@Transactional
	public void omitStrips(String sceneNumbers, Project project) {
		log.debug("scenes=" + sceneNumbers + ", project=" + project.getId());
		List<Strip> strips = findBySceneNumberAndProject(sceneNumbers, project);
		for (Strip strip : strips) {
			int maxOrderNumber = findMaxOrderNumber(strip.getStripboard(), StripStatus.OMITTED);
			strip.setOrderNumber(maxOrderNumber + StripDAO.ORDER_INCREMENT);
			strip.setStatus(StripStatus.OMITTED);
			strip.setUnitId(null);
		}
	}

	/**
	 * Mark a set of omitted Strips as Unscheduled. The Strips marked will be those associated with
	 * the given "sceneNumbers" string from all stripboards in the given project. The updated Strips
	 * will have sequence numbers higher than any existing Unscheduled Strip, so they will appear at
	 * the end of the Unscheduled list.
	 *
	 * @param sceneNumbers The 'sceneNumbers' field to match in the Strips. This may be a single
	 *            scene number, or a list of scene numbers separated by commas in the case of merged
	 *            scenes.
	 * @param project The project whose stripboards will be searched for the Strips in question.
	 */
	@Transactional
	public void restoreStrips(String sceneNumbers, Project project) {
		List<Strip> strips = findBySceneNumberAndProject(sceneNumbers, project);
		for (Strip strip : strips) {
			int maxOrderNumber = findMaxOrderNumber(strip.getStripboard(), StripStatus.UNSCHEDULED);
			strip.setOrderNumber(maxOrderNumber + StripDAO.ORDER_INCREMENT);
			strip.setStatus(StripStatus.UNSCHEDULED);
			strip.setUnitId(null); // should be null already, but can't hurt
			attachDirty(strip);
		}
	}

	/**
	 * Re-sequence all the Strip objects in the given stripboard that are either
	 * for the specified Unit, or are Unscheduled or Omitted (which are not
	 * associated with any Unit).
	 */
	@Transactional
	public void resequenceOrderNumbers(Stripboard board, Unit unit) {
		StripboardDAO stripboardDAO = StripboardDAO.getInstance();
		board = stripboardDAO.refresh(board);
		List<Strip> strips = findByUnitAndStripboard(unit, board);
		int order = ORDER_INCREMENT;
		for (Strip s : strips) {
			if (s.getType() == StripType.END_STRIPBOARD) {
				s.setOrderNumber(StripDAO.ORDER_MAXIMUM);
			}
			else {
				s.setOrderNumber(order);
				order += ORDER_INCREMENT;
			}
		}
		log.debug("Scheduled re-seq'd: " + strips.size());

		strips = findByStatusAndStripboard(StripStatus.UNSCHEDULED, board);
		order = ORDER_INCREMENT;
		for (Strip s : strips) {
			if (s.getType() == StripType.END_STRIPBOARD) {
				s.setOrderNumber(StripDAO.ORDER_MAXIMUM);
			}
			else {
				s.setOrderNumber(order);
				order += ORDER_INCREMENT;
			}
		}
		log.debug("UN-Scheduled re-seq'd: " + strips.size());

		strips = findByStatusAndStripboard(StripStatus.OMITTED, board);
		order = ORDER_INCREMENT;
		for (Strip s : strips) {
			s.setOrderNumber(order);
			order += ORDER_INCREMENT;
		}
		log.debug("Omitted re-seq'd: " + strips.size());

		stripboardDAO.attachDirty(board);
	}

	/**
	 * Scan the provided list of StripBoardSceneBean`s and verify that the
	 * related Strip`s have increasing orderNum values. If not, update any
	 * orderNum values necessary to keep the list in ascending order.
	 *
	 * @param list The List to be reviewed.
	 * @return True iff at least one Strip had to have its orderNum value
	 *         updated.
	 */
	@Transactional
	public boolean updateStripOrderNums(List<StripBoardScene> list) {
		boolean updated = false;
		int orderNum = 0;
		Strip strip;
		// just make sure #'s are increasing, don't have to renumber if already in order.
		for (StripBoardScene s : list) {
			if (s.getOrderNumber() <= orderNum && s.getType() != StripType.END_STRIPBOARD) {
				orderNum += (StripDAO.ORDER_INCREMENT/4);
				strip = findById(s.getId());
				strip.setOrderNumber(orderNum);
				s.setOrderNumber(orderNum);
				//attachDirty(strip); No need to attach - will be updated at transaction end.
				updated = true;
			}
			orderNum = s.getOrderNumber();
		}
		return updated;
	}

	/**
	 * Update all the Strip`s in a Stripboard so their status matches the
	 * corresponding Scene in the given Script. Also creates any missing
	 * Strip`s, which can happen if multiple Stripboard`s exist and a Script is
	 * loaded which has more Scene`s than the previous one.
	 *
	 * @param stripboard The Stripboard (containing Strip`s) to compare against
	 *            and update.
	 * @param script The new default Script, which may be null.
	 */
	@Transactional
	public void updateStripboard(Stripboard stripboard, Script script) {
		if (script == null) {
			return;	// nothing to do.
		}
		try {
			log.debug("stripboard=" + stripboard.getId() + ", script=" + script.getId());
			/** Scene`s to be searched for matching Strip. */
			Collection<Scene> scenes = script.getScenes();
			/** The database ids of Scene`s represented by existing Strip`s. */
			Set<Integer> sceneIds = new HashSet<Integer>();
			int maxUnscheduledOrderNumber = findMaxOrderNumber(stripboard, StripStatus.UNSCHEDULED);
			int maxOmittedOrderNumber = findMaxOrderNumber(stripboard, StripStatus.OMITTED);
			for (Strip strip : stripboard.getStrips()) {
				Scene scene = findSceneForStrip(scenes, strip);
				if (scene != null) {
					sceneIds.add(scene.getId()); // track processed Scenes
					boolean updated = false;
					if (scene.getOmitted()) { // scene is omitted,
						if (strip.getStatus() != StripStatus.OMITTED) { // Strip is not -- change it:
							maxOmittedOrderNumber += StripDAO.ORDER_INCREMENT;
							strip.setOrderNumber(maxOmittedOrderNumber);
							strip.setStatus(StripStatus.OMITTED);
							strip.setUnitId(null);
							updated = true;
						}
					}
					else { // scene is NOT omitted
						if (strip.getStatus() == StripStatus.OMITTED) { // but Strip is -- change it:
							maxUnscheduledOrderNumber += StripDAO.ORDER_INCREMENT;
							strip.setOrderNumber(maxUnscheduledOrderNumber);
							strip.setStatus(StripStatus.UNSCHEDULED);
							updated = true;
						}
					}
					if (scene.getLength() != null && ! scene.getLength().equals(strip.getLength())) {
						log.debug(strip.getLength() + " " + scene.getLength());
						strip.setLength(scene.getLength());
						updated = true;
					}
					if (updated) {
						attachDirty(strip);
					}
				}
			}
			// Now check for Scenes that had no matching Strip...
			boolean updated = false;
			for (Scene scn : scenes) {
				if (! sceneIds.contains(scn.getId())) {
					// No Strip matched this Scene ... add a new Strip
					Strip strip = createStrip(stripboard, -1, -1, scn);
					save(strip);
					updated = true;
				}
			}
			if (updated) {
				attachDirty(stripboard); // this will add strips to db also
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Find the Scene within the given Collection of Scene`s that has the number
	 * matching the sceneNumber in the given Strip.
	 * @param scenes
	 * @param strip
	 * @return The matching Scene, or null if no match is found.
	 */
	private Scene findSceneForStrip(Collection<Scene> scenes, Strip strip) {
		String stNum = strip.getSceneNumbers();
		for (Scene s : scenes) {
			if (s.getNumber().equals(stNum)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Update all the strips, in all the stripboards for the given project, to reflect a merge
	 * operation, normally requested from the Breakdown page. The sourceStrip is associated with one
	 * or more scenes, and those scenes will be added to the scene(s) already associated with
	 * 'targetStrip'. Then the 'sourceStrip' will be deleted, as it is no longer required.
	 *
	 * @param sourceStrip The source of the scenes to be added to the target Strip.
	 * @param targetStrip The Strip whose list of scenes will be updated by adding to it the
	 *            scene(s) currently associated with the source Strip.
	 * @param project The project whose stripboards will be updated.
	 */
	@Transactional
	public void mergeStrips(Strip sourceStrip, Strip targetStrip, Project project) {
		log.debug("source=" + sourceStrip.getId() + ", target=" + targetStrip.getId());
		// Create an updated list of scene numbers, in script sequence,
		// which takes a few steps...
		List<String> sceneList = targetStrip.getScenes(); // get existing scene number(s)
		sceneList.addAll(sourceStrip.getScenes()); // add list of merging scene #'s

		// change unordered collection of scene numbers to ORDERED list of scenes
		List<SceneNumber> snList = new ArrayList<SceneNumber>();
		for (String str : sceneList) { // change them into SceneNumber objects for sort
			snList.add(new SceneNumber(str));
		}
		Collections.sort(snList); // now they're in order!
		String nums = StringUtils.getStringFromList(snList, ",");

		int newLength = sourceStrip.getLength() + targetStrip.getLength();
		List<Strip> sources = findBySceneNumberAndProject(sourceStrip.getSceneNumbers(), project);
		for (Strip source : sources) {
			log.debug("source=" + source.getId());
			Strip target = findBySceneAndStripboard(targetStrip.getSceneNumbers(), source.getStripboard());
			if (target != null) { // only null if stripboards out of sync for some reason.
				log.debug("target=" + target.getId());
				target.setSceneNumbers(nums);
				target.setLength(newLength);
				for (Note note : source.getNotes()) {
					Note newnote = (Note)note.clone();
					newnote.setStrip(target);
					target.getNotes().add(newnote);
				}
			}
			delete(source);
		}
	}

	/**
	 * Split a particular scene out of a set of merged scenes, by updating all the Strips in any
	 * stripboard in the specified project to remove that scene from the merged strip(s) and insert
	 * a new strip (in each stripboard) associated with only the split scene.
	 *
	 * @param split The Scene which is being removed (split) off of the merged set.
	 * @param strip An existing Strip that refers to the current merged list.
	 * @return The strip that was added for the split scene, within the project's active (current)
	 *         stripboard.
	 */
	@Transactional
	public Strip split(Scene split, Strip strip, Project project) {

		List<String> list = strip.getScenes(); // get existing List of scene numbers
		list.remove(split.getNumber());	// remove the scene number being split
		String nums = StringUtils.getStringFromList(list, ","); // turn it back into comma-delimited list

		int newLength = strip.getLength() - split.getLength();
		List<Strip> targets = findBySceneNumberAndProject(strip.getSceneNumbers(), project);
		Strip newStrip, saveStrip = null;
		for (Strip target : targets) {
			target.setSceneNumbers(nums);
			target.setLength(newLength);
			newStrip = addStrip(target.getStripboard(), -1, -1, split);
			if (project.getStripboard().equals(target.getStripboard())) {
				saveStrip = newStrip;
				Note newNote;
				for (Note note : strip.getNotes()) {
					newNote = (Note)note.clone();
					newNote.setStrip(saveStrip);
					saveStrip.getNotes().add(newNote);
				}
				attachDirty(saveStrip);
			}
		}
		log.debug("returning strip=" + (saveStrip==null?"null":saveStrip.getId()));
		return saveStrip;
	}

	/**
	 * Given a List of scene numbers (Strings), delete all Strips in the specified Project that
	 * refer to any of those scene numbers. The Strips that match are deleted from the database, and
	 * removed from their association with their stripboard.
	 *
	 * @param sceneNumList A list of Strings, each of which is a scene number.
	 * @param project The project which owns the scripts related to these Scenes.
	 */
	@Transactional
	public void deleteOrphanStrips(List<String> sceneNumList, Project project) {
		log.debug("");
		Collection<Stripboard> boards = new HashSet<Stripboard>();
		for (String sceneNum : sceneNumList) {
			Collection<Strip> strips = findBySceneNumberAndProject(sceneNum, project);
			// TODO needs enhancing to support multiple scenes per strip
			for (Strip strip : strips) {
				strip.getStripboard().getStrips().remove(strip);
				boards.add(strip.getStripboard());
				delete(strip);
			}
		}
		final StripboardDAO stripboardDAO = StripboardDAO.getInstance();
		for (Stripboard board : boards) {
			stripboardDAO.attachDirty(board);
		}
	}

	/**
	 * Given a scene number (String), delete all Strips in the specified
	 * Project that refer to that scene number.  The Strips that match are
	 * deleted from the database, and removed from their association with
	 * their stripboard.
	 * @param sceneNum
	 * @param project
	 */
	@Transactional
	public void deleteOrphanStrips(String sceneNum, Project project) {
		log.debug("");
		List<String> list = new ArrayList<String>(1);
		list.add(sceneNum);
		deleteOrphanStrips(list, project);
	}

	/**
	 * Delete all "breakdown" Strips in the specified Project. The Strips that match are deleted
	 * from the database, and removed from their association with their stripboard. Only Banner and
	 * End-of-Day strips will remain in the project.
	 *
	 * @param project
	 */
	@Transactional
	public void deleteOrphanStrips(Project project) {
		log.debug("");
		Collection<Stripboard> changedBoards = new HashSet<Stripboard>();
		for (Stripboard board : project.getStripboards()) {
			for (Iterator<Strip> iter = board.getStrips().iterator(); iter.hasNext(); ) {
				Strip strip = iter.next();
				if (strip.getType() == StripType.BREAKDOWN) {
					changedBoards.add(strip.getStripboard());
					iter.remove();
					delete(strip);
				}
			}
		}
		StripboardDAO stripboardDAO = StripboardDAO.getInstance();
		for (Stripboard board : changedBoards) {
			stripboardDAO.attachDirty(board);
		}
	}

	/**
	 * A Unit is being removed -- remove the UnitStripboard associated with it, and
	 * update any Strip's associated with the Unit to be unscheduled.
	 * @param project
	 * @param unit
	 */
	@Transactional
	public void removeUnit(Project project, Unit unit) {
		//StripboardDAO stripboardDAO = StripboardDAO.getInstance();
		for (Stripboard sb : project.getStripboards()) {
			List<Strip> list = findByUnitAndStripboard(unit, sb);
			int order = findMaxOrderNumber(sb, StripStatus.UNSCHEDULED);
			for (Strip st : list) {
				if (st.getType() == StripType.BREAKDOWN) {
					order += ORDER_INCREMENT;
					st.setOrderNumber(order); // place Strips at bottom of unscheduled list
					st.setStatus(StripStatus.UNSCHEDULED);
					st.setUnitId(null);
				}
				else {
					sb.getStrips().remove(st);
					delete(st);
				}
			}
			UnitStripboard usb = sb.getUnitSb(unit);
			if (usb != null) { // null only if db is corrupted
				sb.getUnitSbs().remove(usb);
				delete(usb);
			}
			attachDirty(sb);
		}
	}

	/**
	 * Instantiates a new Strip, and initializes the order#, sheet#, length, scene#'s, and synopsis
	 * (if text is available). The strip type is set to "BK" (breakdown). The strip is added to the
	 * stripboard's collection BUT it is not saved to the database. (Use addStrip() to also add it
	 * to the database.) If the breakdown sheet number is negative, a new one is calculated by
	 * finding the highest sheet number in the stripboard and adding 1. Similarly, if the order
	 * number is negative, the highest order number for the existing set of Unscheduled strips is
	 * found, and then incremented by the "standard" amount.
	 *
	 * @param stripboard The Stripboard that this Strip is part of.
	 * @param sheetNumber The breakdown page number for the new strip, or a negative value if the
	 *            method should assign it the next available number.
	 * @param orderNumber The order (sequence) number of the new strip, or a negative value if the
	 *            method should assign it the next available order number within the set of
	 *            Unscheduled strips.
	 * @param scene The (first) scene associated with the new Strip.
	 * @return The newly created Strip.
	 */
	@Transactional
	public Strip createStrip(Stripboard stripboard, int sheetNumber, int orderNumber, Scene scene) {
		if (sheetNumber < 0) {
			// find next available breakdown number
			sheetNumber = findMaxSheetNumber(stripboard);
			sheetNumber++;
		}
		if (orderNumber < 0) {
			orderNumber = findMaxUnscheduledOrderNumber(stripboard);
			orderNumber += StripDAO.ORDER_INCREMENT;
		}
		Strip strip = new Strip(); // default is type=BREAKDOWN and status=UNSCHEDULED
		strip.setOrderNumber(orderNumber);
		strip.setSheetNumber(sheetNumber);
		strip.setLength(scene.getLength());
		strip.setSceneNumbers(scene.getNumber());
//		strip.setSynopsis(SceneDAO.createSynopsis(scene));
		// add strip to stripboard
		stripboard.getStrips().add(strip);
		strip.setStripboard(stripboard);
		return strip;
	}

	/**
	 * Insert one or more new Strips to match a newly-inserted Scene, if needed.
	 * Adds one as an unscheduled Strip to any Stripboard in the current Project
	 * that does not have a matching Strip.
	 *
	 * @param newScene The Scene which the new Strip should specify.
	 * @return The Strip which matches the supplied Scene (contains its scene
	 *         number) in the current Stripboard. This may be a Strip that was
	 *         just created and added to the current Stripboard, or an existing
	 *         Strip (in the current Stripboard) that was found which matched
	 *         the given scene number.
	 */
	@Transactional
	public Strip addStrips(Scene newScene, Project project) {
		Stripboard currentSb = project.getStripboard();
		Strip strip;
		Strip retStrip = null;
		if ( currentSb != null) {
			for (Stripboard sb : project.getStripboards()) {
				sb = StripboardDAO.getInstance().refresh(sb);
				// see if a Strip already exists (maybe created due to earlier Script revision)
				strip = findBySceneAndStripboard(newScene.getNumber(), sb);
				if (strip == null) { // need to create one
					strip = addStrip(sb, -1, -1, newScene);
					// That added it to db & updated the stripboard too.
				}
				else {
					updateStrip(sb, strip, newScene);
				}
				if (currentSb.getId().equals(sb.getId())) {
					retStrip = strip;
				}
			}
		}
		return retStrip;
	}

	/**
	 * Instantiates a new Strip, and initializes the order#, sheet#, length, scene#'s, and synopsis
	 * (if text is available). The strip type is set to "BK" (breakdown). The strip is added to the
	 * stripboard's collection, saved to the database, and the stripboard updated to the database.
	 * If the breakdown sheet number is negative, a new one is calculated by finding the highest
	 * sheet number in the stripboard and adding 1. Similarly, if the order number is negative, the
	 * highest order number for the existing set of Unscheduled strips is found, and then
	 * incremented by the "standard" amount.
	 *
	 * @param stripboard The Stripboard that this Strip is part of.
	 * @param sheetNumber The breakdown page number for the new strip, or a negative value if the
	 *            method should assign it the next available number.
	 * @param orderNumber The order (sequence) number of the new strip, or a negative value if the
	 *            method should assign it the next available order number within the set of
	 *            Unscheduled strips.
	 * @param scene The (first) scene associated with the new Strip.
	 * @return The newly created Strip.
	 */
	//@Transactional
	private Strip addStrip(Stripboard stripboard, int sheetNumber, int orderNumber, Scene scene) {
		Strip strip = createStrip(stripboard, sheetNumber, orderNumber, scene);
		save(strip);
		attachDirty(stripboard);
		return strip;
	}

	/**
	 * Update a Strip's length and synopsis fields to match the given Scene.
	 * Used during an "add scene" operation, when the Strip is found to already
	 * exist; we need to make sure the Strip reflects the contents of the newly
	 * created Scene.
	 *
	 * @param strip The Strip to be updated.
	 * @param scene The Scene that the Strip includes.
	 */
	@Transactional
	public void updateStrip(Stripboard stripboard, Strip strip, Scene scene) {
		strip.setLength(scene.getLength());
//		strip.setSynopsis(SceneDAO.createSynopsis(scene));
		if (strip.getStatus() != StripStatus.UNSCHEDULED) {
			int orderNumber = findMaxUnscheduledOrderNumber(stripboard);
			orderNumber += StripDAO.ORDER_INCREMENT;
			strip.setOrderNumber(orderNumber);
			strip.setStatus(StripStatus.UNSCHEDULED);
			strip.setUnitId(null);
		}
		attachDirty(strip);
	}

}
