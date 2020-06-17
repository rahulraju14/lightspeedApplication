//	File Name:	ProductionDood.java
package com.lightspeedeps.dood;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.RealWorldElement;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * This class maintains the Day-out-of-Days information for all Productions.
 * Since the data is held in a static field, and accumulates as it is needed, it
 * is periodically cleared by a scheduled task to prevent it from becoming
 * excessively large.
 */
public class ProductionDood {
	private static final Log log = LogFactory.getLog(ProductionDood.class);

	/**
	 * This Map holds the ProjectDood for each unit, keyed by unit id, once it
	 * has been created. If some data changes that affects the DooD values, the
	 * projectDood should be marked 'dirty', which will cause it to be refreshed
	 * the next time is is fetched -- see the getProjectDood() method below.
	 */
	private static final Map<Integer,ProjectDood> productionDoodMap =
			new ConcurrentHashMap<Integer,ProjectDood>(
					100, 		// Should be higher than peak # of unique users logging in between "clear()" calls.
								// As of 1/14/2014, it cleared daily, and peak unique users per day is < 30.
					(float)0.75, // Default loading factor is fine
					4);			// Level of concurrent access (# of threads) is likely to be very low.

	public ProductionDood() {
	}

	public static Map<Integer, ProjectDood> getProductionDoodMap() {
		return productionDoodMap;
	}

	/**
	 * Add a new ScriptElement to the DooD table.  This allows us to at least
	 * display default or initial values when a ScriptElement is first created.
	 * @param project The Project the ScriptElement has been added to.
	 * @param se The new ScriptElement object.
	 */
	public static void addElement(Project project, ScriptElement se) {
		ProjectDood pDood = null;
		// get pDood without checking 'dirty' flag
		pDood = productionDoodMap.get(project.getMainUnit().getId());
		if (pDood == null) { // no luck, do "full" get, to force build
			pDood = getProjectDood(project.getMainUnit());
		}
		if (pDood != null) {
			pDood.addElement(se);
			pDood.setDirty(true);
		}
	}

	public static WorkdayType getStatus(Unit unit, Integer scriptElementId, Date date) {
		WorkdayType status = null;
			ElementDood eDood = getElementDood(unit, scriptElementId);
			if (eDood != null) {
				status = eDood.getStatus(date);
			}
		return status;
	}

	public static WorkdayType getStatus(Unit unit, ScriptElement scriptElement, Date date) {
		return getStatus(unit, scriptElement.getId(), date);
	}

	public static ElementDood getElementDood(Project project, Integer scriptElementId) {
		ElementDood eDood = null;
		// The ScriptElement DooD is only used for occurrence-counting, not start/end
		// dates, so we don't need to "merge" all the Unit DooD info -- just use the main one.
		Unit unit = project.getMainUnit();
		if (scriptElementId != null) {
			ProjectDood pDood = getProjectDood(unit);
			if (pDood != null) {
				eDood = pDood.getElementDood(scriptElementId);
			}
		}
		return eDood;
	}

	public static ElementDood getElementDood(Unit unit, Integer scriptElementId) {
		ElementDood eDood = null;
		if (scriptElementId != null) {
			ProjectDood pDood = getProjectDood(unit);
			if (pDood != null) {
				eDood = pDood.getElementDood(scriptElementId);
			}
		}
		return eDood;
	}

	public static ElementDood getElementDood(RealWorldElement rwe) {
		ElementDood eDood = null;
		if (rwe.getRealLinks() != null && rwe.getRealLinks().size() > 0) {
			Date first = null,
				last = null;
			ProjectDood pDood;
			ElementDood dood;
			boolean dateFlag = false;
			//String dateMsg = null;
			try {
				for (Unit u : HeaderViewBean.getInstance().getProject().getUnits()) {
					pDood = getProjectDood(u);
					if (pDood != null) {
						dood = pDood.getElementDood(rwe);
						if ((! dateFlag) && dood.getDateFlag()) {
							dateFlag = true;
							// SimpleDateFormat oSdf = new SimpleDateFormat("M/d");
							// dateMsg = "Blackout date conflict occurs on " + oSdf.format(dood.getConflictDate());
						}
						if (first == null ||
								(dood.getFirstWorkDate() != null &&
								first.after(dood.getFirstWorkDate()))) {
							first = dood.getFirstWorkDate();
						}
						if (last == null ||
								(dood.getLastWorkDate() != null &&
								last.before(dood.getLastWorkDate()))) {
							last = dood.getLastWorkDate();
						}
						//log.debug("u=" + u.getId() + ", first=" + first + ", last=" + last);
					}

				}
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
			eDood = new ElementDood(rwe.getId());
			eDood.setFirstWorkDate(first);
			eDood.setLastWorkDate(last);
			eDood.setDateFlag(dateFlag);
			//eDood.setConflictMsg(dateMsg);
		}
		return eDood;
	}

	public static ProjectDood getProjectDood(Unit unit) {
		ProjectDood pDood = productionDoodMap.get(unit.getId());
		if (pDood == null || pDood.isDirty()) {
			pDood = updateProjectDood(unit);
		}
		return pDood;
	}

	public static void setProjectDood(Unit unit, ProjectDood pDood) {
		productionDoodMap.put(unit.getId(), pDood);
	}

	/**
	 * Mark the DooD information dirty for all Unit`s in the given Project.
	 * @param project
	 */
	public static void markProjectDirty(Project project) {
		log.debug("");
		markProjectDirty(project, true);
	}

	/**
	 * Mark the DooD information dirty for all Unit`s in the given Project.
	 * @param project
	 * @param updateDrops
	 */
	public static void markProjectDirty(Project project, boolean updateDrops) {
		for (Unit u : project.getUnits()) {
			markUnitDirty(u, updateDrops);
		}
	}

	/**
	 * Mark a unit's DooD data "dirty" -- that is, ensure that the next
	 * request for it will result in it being freshly created.
	 * <p>
	 * The process checks for the existence of the unit's DooD data. If
	 * the data does not already exist, there's no need to mark anything,
	 * because a call to getProjectDooD() will create it.
	 * @param unit
	 */
	public static void markUnitDirty(Unit unit) {
		log.debug("");
		markUnitDirty(unit, true);
	}

	/**
	 * Mark a unit's DooD data "dirty" -- that is, ensure that the next
	 * request for it will result in it being freshly created.
	 * <p>
	 * The process checks for the existence of the unit's DooD data. If the
	 * data does not already exist, there's no need to mark anything, because a
	 * call to getProjectDooD() will create it.
	 *
	 * @param unit
	 * @param updateDrops If true, update "drop to use" values for Character
	 *            elements to match the longest "hold period". If false, the
	 *            "drop to use" will only be changed if it no longer matches any
	 *            valid hold period. This should be true if we believe the user
	 *            has changed data that will affect the scheduling of one or
	 *            more Character elements, and false otherwise.
	 */
	public static void markUnitDirty(Unit unit, boolean updateDrops) {
		log.debug("updateDrops="+updateDrops);
		ProjectDood pDood = productionDoodMap.get(unit.getId());
		if (pDood != null) {
			pDood.setDirty(true);
			pDood.setUpdateDrops(updateDrops);
		}
	}

	/**
	 * Mark the given RealWorldElement's DooD information invalid
	 * for all Unit`s within the given Project.
	 * @param project
	 * @param element
	 */
	public static void markDirty(Project project, RealWorldElement element) {
		for (Unit u : project.getUnits()) {
			markDirty(u, element);
		}
	}

	/**
	 * Mark the DooD information for a specific RealWorldElement invalid. It
	 * does this by simply removing the related ElementDood object, so that it
	 * will be recreated the next time it is requested. (See
	 * ProjectDood.getElementDood.)
	 *
	 * @param unit The unit for which the DooD information is no longer
	 *            valid.
	 * @param element The element whose information is no longer valid.
	 */
	public static void markDirty(Unit unit, RealWorldElement element) {
		log.debug("elem=" + element.getName());
		ProjectDood pDood = productionDoodMap.get(unit.getId());
		if (pDood != null) {
			pDood.setElementDood(element, null);
		}
	}

	/**
	 * Recalculate the DooD values for the specified unit.
	 * @param unit
	 * @return The non-null, updated ProjectDood
	 */
	public static ProjectDood updateProjectDood(Unit unit) {
		log.debug("update unit, id=" + unit.getId());
		boolean updateDrops = false;
		ProjectDood pDood = productionDoodMap.get(unit.getId());
		if (pDood != null) {
			updateDrops = pDood.getUpdateDrops();
			log.debug("updateDrops="+updateDrops);
		}
		pDood = new ProjectDood();
		pDood.setUpdateDrops(updateDrops);
		unit = UnitDAO.getInstance().refresh(unit); // refresh!
		// Without refresh, we got lazy init errors & others on Script Element List page, when
		// scenes were added, causing the DooD data to be dirty.
		pDood.createDood(unit);
		pDood.setUpdateDrops(false);
		setProjectDood(unit, pDood);
		return pDood;
	}

	public static void clear() {
		productionDoodMap.clear();
	}

	/**
	 * @return The total number of ElementDood objects (for all Productions)
	 * in the DooD map.
	 */
	public static int getElementCount() {
		int count = 0;
		Set<Integer> set = productionDoodMap.keySet();
		for (Integer uid : set) {
			ProjectDood pd = productionDoodMap.get(uid);
			count += pd.getSeMap().size();
			count += pd.getRwMap().size();
		}
		log.debug("total elementDood's=" + count);
		return count;
	}

}
