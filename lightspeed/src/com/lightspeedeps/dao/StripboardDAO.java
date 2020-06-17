//	File Name:	ScriptElementDAO.java
package com.lightspeedeps.dao;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.Strip;
import com.lightspeedeps.model.Stripboard;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.model.UnitStripboard;
import com.lightspeedeps.type.ActionType;
import com.lightspeedeps.type.ChangeType;
import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.type.StripType;
import com.lightspeedeps.util.app.ChangeUtils;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Stripboard entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Stripboard
 */
public class StripboardDAO extends BaseTypeDAO<Stripboard> {
	private static final Log log = LogFactory.getLog(StripboardDAO.class);
	// property constants
//	private static final String REVISION = "revision";
//	private static final String DESCRIPTION = "description";
//	private static final String SHOOTING_DAYS = "shootingDays";

	public static StripboardDAO getInstance() {
		return (StripboardDAO)getInstance("StripboardDAO");
	}

	/***
	 * Determine if any Stripboard`s exist where the given User is currently the
	 * "last saved by" person.
	 *
	 * @param user
	 * @return True iff there is a Stripboard with the given User set as the
	 *         "last saved by" person.
	 */
//	public boolean existsUser(User user) {
//		String query = "select count(id) from Stripboard where user = ? ";
//		return findCount(query, user) > 0;
//	}

	/**
	 * Find the highest revision number of any stripboard in the specified project.
	 * @param project
	 * @return The highest existing revision number.
	 */
	public Integer findMaxRevision(Project project) {
		log.debug("finding Stripboard instance with property: max value: ");
		String queryString = "select max(s.revision) from Stripboard s where s.project = ?";
		return (Integer)find(queryString, project).get(0);
	}

//	public static StripboardDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (StripboardDAO) ctx.getBean("StripboardDAO");
//	}

	/**
	 * Find the highest revision value for all Stripboards in the given Project.
	 * Returns 0 if there are no Stripboards in the Project.
	 */
	@SuppressWarnings("unchecked")
	public int findMaxStripboardRevision(Project project) {
		String strQuery = "select max(s.revision) from Stripboard s where s.project=?";
		Integer iRev = null;
		int revision = 0;
		List<Object> list = null;
		list = find(strQuery, project);
		if (list != null && list.size() > 0 && list.get(0) instanceof Integer) {
			iRev = (Integer)list.get(0);
			if (iRev != null) {
				revision = iRev.intValue();
			}
		}
		return revision;
	}

	/**
	 * Create a new Stripboard, and populate it with Strips matching the Scenes in the supplied
	 * Script. If the project does not have a default Script, the one given will be set as the
	 * default; if there is no default Stripboard, the one created will be made the default.
	 *
	 * @param project The project containing the Script and Stripboard.
	 * @param script The Script containing Scenes to be associated with the new Stripboard.
	 * @param title The title to assign the new Stripboard.
	 */
	@Transactional
	public Project initStripboard(Project project, Script script, String title) {
		ProjectDAO projectDAO = ProjectDAO.getInstance();
		project = projectDAO.refresh(project);
		log.debug("project=" + project);
		Stripboard stripboard = createStripboard(project, title);
		populateStripboard(stripboard, script);
		if (project.getStripboard() == null) { // no default stripboard yet
			project.setStripboard(stripboard);
		}

		if (project.getScript() == null) { // no default script yet
			projectDAO.setScript(project, script);
		}
		projectDAO.attachDirty(project);
		log.debug("stripboard created & populated, project="+project.getId());
		return project;
	}

	//@Transactional -- caller is transactional
	private Stripboard createStripboard(Project project, String description) {
		Stripboard stripboard = new Stripboard();
		int rev = findMaxStripboardRevision(project);
		log.debug("prior max stripboard revision= "+rev);
		stripboard.setRevision(++rev);
		stripboard.setDescription(description);
		stripboard.setProject(project);
		stripboard.setLastSaved(new Date());
		for (Unit unit : project.getUnits()) {
			UnitStripboard usb = new UnitStripboard(unit, stripboard);
			stripboard.getUnitSbs().add(usb);
		}
		save(stripboard);
		return stripboard;
	}

	/**
	 * Fill a stripboard with strips for every scene in the script provided. Each strip (breakdown
	 * sheet) is assigned one scene. The stripboard is assumed to have no Strips to begin with. The
	 * strips are numbered sequentially beginning at 1, and their order number is set equal to the
	 * sheet number.
	 *
	 * @param stripboard The stripboard to contain the new strips.
	 * @param script The script containing the scenes for which strips will be created.
	 */
	//@Transactional - caller is transactional
	private void populateStripboard(Stripboard stripboard, Script script) {
		int breakdownNumber = 1;
		final StripDAO stripDAO = StripDAO.getInstance();
		List<Scene> scenes = script.getScenes();
		for (Scene scene : scenes) {
			// Create a Strip (Breakdown Sheet) to match the Scene
			Strip strip = stripDAO.createStrip(stripboard, breakdownNumber, breakdownNumber, scene);
			// add breakdown sheet to database
			stripDAO.save(strip);
			breakdownNumber++;
		}
		// Insert one end-of-day strip to get schedule primed.
		Strip strip = new Strip(StripDAO.ORDER_INCREMENT, StripType.END_OF_DAY, StripStatus.SCHEDULED,
				stripboard, stripboard.getProject().getMainUnit());
		stripboard.getStrips().add(strip);
		stripboard.getStrips().addAll(createEndStrips(stripboard));
		attachDirty(stripboard);
	}

	/**
	 * Add a scheduled "end-stripboard" Strip to the given Stripboard, assigned
	 * to the given unit. This creates the new Strip and adds it to the
	 * database.
	 *
	 * @param stripboard
	 * @param unit
	 */
	@Transactional
	public void addScheduledEndStrip(Stripboard stripboard, Unit unit) {
		stripboard.getStrips().add(createScheduledEndStrip(stripboard, unit));
		attachDirty(stripboard);
	}

	/**
	 * Add an unscheduled "end-stripboard" Strip to the given Stripboard. This
	 * creates the new Strip and adds it to the database.
	 *
	 * @param stripboard
	 */
	@Transactional
	public Strip addUnscheduledEndStrip(Stripboard stripboard) {
		Strip strip = new Strip(StripDAO.ORDER_MAXIMUM, StripType.END_STRIPBOARD, StripStatus.UNSCHEDULED, stripboard);
		stripboard.getStrips().add(strip);
		attachDirty(stripboard);
		return strip;
	}

	/**
	 * Create all the "end-stripboard" Strip's required for the given
	 * Stripboard, and return them in a Collection. The database is not updated.
	 *
	 * @param stripboard
	 * @return A Collection of "end-stripboard" Strip's -- at least two. One
	 *         scheduled Strip for each Unit in the Project associated with the
	 *         Stripboard, plus one unscheduled Strip.
	 */
	private Collection<Strip> createEndStrips(Stripboard stripboard) {
		Collection<Strip> strips = new ArrayList<Strip>();
		for (Unit unit : stripboard.getProject().getUnits()) {
			strips.add(createScheduledEndStrip(stripboard, unit));
		}
		strips.add(new Strip(StripDAO.ORDER_MAXIMUM, StripType.END_STRIPBOARD, StripStatus.UNSCHEDULED, stripboard));
		return strips;
	}

	public Strip createScheduledEndStrip(Stripboard stripboard, Unit unit) {
		Strip strip = new Strip(StripDAO.ORDER_MAXIMUM, StripType.END_STRIPBOARD, StripStatus.SCHEDULED, stripboard, unit);
		return strip;
	}

	/**
	 * Renumber all the Strips in a Stripboard, in the same order that
	 * the Scenes that they represent are in within the Script.  Note that this
	 * does not affect the order of the Strips within the Stripboard.
	 * @param stripboard The stripboard whose Strips are to be renumbered.
	 * @param script The script to use for ordering the scenes.
	 */
	@Transactional
	public void renumberStrips(Stripboard stripboard, Script script) {
		int stripNumber = 1;
		boolean updateStripboard = false;
		boolean newStrip;
		final StripDAO stripDAO = StripDAO.getInstance();
		int orderNumber = stripDAO.findMaxUnscheduledOrderNumber(stripboard);

		List<Scene> scenes = script.getScenes(); // The list of scenes, in Script order.

		Set<Strip> strips = stripboard.getStrips(); // The existing Strips

		// A list of the strips that we have renumbered so far:
		Set<Strip> renumbered = new HashSet<Strip>(2*scenes.size());

		// Create a temporary map to lookup Strips by scene number
		Map<String,Strip> stripMap = new HashMap<String,Strip>();
		for (Strip strip: strips) {
			for (String sceneNumber : strip.getScenes()) {
				stripMap.put(sceneNumber, strip);
			}
		}

		// Create a Breakdown sheet (Strip) to match each Scene in turn
		for (Scene scene : scenes) {
			// Find the Strip that lists this scene, and remove it from map, too.
			Strip strip = stripMap.remove(scene.getNumber());
			if (strip == null || ! renumbered.contains(strip)) {
				// Scene has no Strip, or the one that
				// represents it has not been renumbered yet.
				newStrip = false;
				if (strip == null) { // no strip was assigned to this scene yet
					newStrip = true;
					strip = new Strip();
					//strip.setStatus(StripStatus.UNSCHEDULED); // default
					//strip.setType(StripType.BREAKDOWN); // default
					strip.setLength(scene.getLength());
					strip.setSceneNumbers(scene.getNumber());
					orderNumber += StripDAO.ORDER_INCREMENT;
					strip.setOrderNumber(orderNumber);
					strip.setStripboard(stripboard);
					stripboard.getStrips().add(strip);
					updateStripboard = true;
					log.debug("new strip added, order="+orderNumber);
				}
				log.debug("scene="+scene.getNumber()+", stripId="+strip.getId() +
						",old num="+strip.getSheetNumber() + ", new num=" + stripNumber + (newStrip?" (new)":""));
				strip.setSheetNumber(stripNumber);
				stripNumber++;
				renumbered.add(strip);
				// add new breakdown sheet to database
				if (newStrip) {
					stripDAO.save(strip);
				}
				else {
					stripDAO.attachDirty(strip);
				}
			}
		}
		// Now the stripMap contains those strips related to scenes that don't exist
		// in the current script.  We need to renumber them, too, so they don't conflict
		// with numbers we just assigned!  This will be in an arbitrary order.
		for (Strip strip : stripMap.values()) {
			if (! renumbered.contains(strip)) {
				log.debug("ex-scene="+strip.getSceneNumbers()+", stripId="+strip.getId() +
						",old num="+strip.getSheetNumber() + ", new num=" + stripNumber);
				strip.setSheetNumber(stripNumber);
				stripNumber++;
				renumbered.add(strip);
			}
		}
		if (updateStripboard) {
			stripboard = merge(stripboard);
		}
		if (log.isDebugEnabled()) {
			for (Strip st : stripboard.getStrips()) {
				log.debug("id:" + st.getId() + ", sheet#:" + st.getSheetNumber());
			}
		}
	}

	/**
	 * Based on the current contents of the 'breakdownMap', create any missing
	 * Strip`s, and add their entries to the breakdownMap.
	 *
	 * @param project The new Strip`s will be added to the current Stripboard
	 *            for this Project.
	 * @param script The list of Scene`s in this Script will be the ones used to
	 *            check against the breakdownMap for missing Strip`s.
	 * @param breakdownMap A mapping of Scene.number to Strip.id for all the
	 *            Scene`s in the given Script. If an entry does not exist for
	 *            one of the Scene`s, a new Strip will be created and added to
	 *            the database, and its id will be added to this map.
	 * @return The refreshed Project object, after the current Stripboard object
	 *         has been updated, if any Strips were added.
	 */
	@Transactional
	public Project fillStripboard(Project project, Script script, Map<String, Integer> breakdownMap) {
		boolean updated = false;
		StripDAO stripDAO = StripDAO.getInstance();
		project = ProjectDAO.getInstance().refresh(project); // refresh
		script = ScriptDAO.getInstance().refresh(script);
		int sheetNumber = -1; // 'createStrip' will use max+1
		int orderNumber = -1; // 'createStrip' will use max(unscheduled)+1
		Stripboard stripboard = project.getStripboard();
		Collection<Scene> scenes = script.getScenes();
		for (Scene scene : scenes) {
			if (breakdownMap.get(scene.getNumber()) == null) {
				Strip strip = stripDAO.createStrip(stripboard, sheetNumber, orderNumber, scene);
				stripDAO.save(strip);
				orderNumber = strip.getOrderNumber() + 1;
				sheetNumber = strip.getSheetNumber();
				breakdownMap.put(scene.getNumber(), strip.getId());
				sheetNumber++;
				updated = true;
				log.debug("Added strip="+strip.toString());
			}
		}
		if (updated) {
			stripboard = merge(stripboard); // this will add strips to db also
			project = ProjectDAO.getInstance().refresh(project); // to reflect new stripboard
		}
		return project;
	}

	/**
	 * Delete a stripboard and remove it as the project's "current" stripboard
	 * if necessary.  A Change record is also written to record the delete.
	 * @param stripboard
	 */
	@Transactional
	public void remove(Stripboard stripboard) {
		if (stripboard != null) {
			ProjectDAO projectDAO = ProjectDAO.getInstance();
			Project project = SessionUtils.getCurrentProject();
			ChangeUtils.logChange(ChangeType.STRIPBOARD, ActionType.DELETE,
					project, (Stripboard)null, "delete from list");
			project.getStripboards().remove(stripboard);
			if (project.getStripboard().getId() == stripboard.getId()) {
				project.setStripboard(null);
			}
			delete(stripboard);
			projectDAO.attachDirty(project);
		}

	}

	/**
	 * Replace all the Strips associated with the given stripboard.
	 *
	 * @param stripboard The stripboard whose Strip collection is to be
	 *            replaced.
	 * @param strips The new set of Strips for the stripboard.
	 * @param deletedStrips The set of Strips that should be deleted from the
	 *            database -- this should include all Strips currently
	 *            associated with the stripboard which are not in the 'strips'
	 *            collection.
	 */
	@Transactional
	public void replaceStrips(Stripboard stripboard, Collection<Strip> strips, Collection<Strip> deletedStrips) {
		try {
			stripboard.getStrips().clear();
			stripboard.getStrips().addAll(strips);
			for (Strip s : deletedStrips) {
				delete(s);
			}
			for (Strip s : strips) {
				if (s.getId() == null) {
					save(s);
				}
				else {
					attachDirty(s);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

}
