//	File Name:	SceneListBean.java
package com.lightspeedeps.web.script;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.model.table.RowState;
import org.icefaces.ace.model.table.RowStateMap;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.dao.StripboardDAO;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.Strip;
import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.script.ScriptUtils;
import com.lightspeedeps.util.script.StripUtils;

/**
 * A superclass used by the backing beans for the Script Import Review (ScriptReviewBean), Script
 * Import Final Review (ScriptFinalReviewBean), and Breakdown Page Edit pages. Note that
 * ScriptFinalReviewBean also supports the similar "Scene List" page.
 * <p>
 * This class implements most of the "scene list" edit functions -- add, omit, delete, and un-omit.
 * <p>
 * Each scene (row) on the display is represented by a SceneListViewerBean instance, except on the
 * Breakdown page which simply uses the List of Scenes.
 */
public abstract class SceneListBase {
	private static final Log LOG = LogFactory.getLog(SceneListBase.class);

	protected Integer scriptId = null;
	protected Integer sceneId;
	protected Script script;
	protected Project project;

	//@Autowired
	protected final ScriptDAO scriptDAO = ScriptDAO.getInstance();
	//@Autowired
	protected final StripDAO stripDAO = StripDAO.getInstance();
	//@Autowired
	protected final SceneDAO sceneDAO = SceneDAO.getInstance();
	protected final StripboardDAO stripboardDAO = StripboardDAO.getInstance();
	protected final ScriptElementDAO scriptElementDAO = ScriptElementDAO.getInstance();
	protected final ProjectDAO projectDAO = ProjectDAO.getInstance();

	/** Set to true when we have verified that a Strip exists in the current
	 * Stripboard matching every Scene in the current Script. */
	protected boolean haveAllStrips = false;

	/** Controls whether the "Add scene" popup is displayed or not. */
	private boolean showAddPopup = false;
	private String addSceneNumber = "";
	private boolean addCopyExisting = true;

	/** The value from the 'new scene'/'scene part' radio buttons on the Add Scene popup */
	private String selectAddScene; // "NEW" or "PART"
	private static final String ADD_SCENE_NEW = "NEW";
//	private static final String ADD_SCENE_PART = "PART";

	/** The value from the 'before'/'after' radio buttons on the Add Scene popup */
	private String placeAddScene; // "BEFORE" or "AFTER"
	/*pkg*/ static final String ADD_SCENE_BEFORE = "BEFORE";
	/*pkg*/ static final String ADD_SCENE_AFTER = "AFTER";

	protected List<Scene> sceneList = null;

	/**
	 * A mapping of scene number to Strip.id for each scene in the script, using
	 * the current stripboard and current Script. If an entry is missing for a
	 * particular Scene.number, it means that the corresponding Strip does not
	 * yet exist in the database.  Note that this field is not required for the
	 * operation of any SceneListBean method, and some subclasses do not use or
	 * require it.  BreakdownBean, however, does use it.
	 */
	private Map<String,Integer> breakdownMap = null;

	/** The RowStateMap instance used to manage the clicked row on the ace:dataTable */
	private RowStateMap stateMap = new RowStateMap();

	public SceneListBase() {
		LOG.debug("");
		project = SessionUtils.getCurrentProject();
	}

	@PostConstruct
	protected void init() {
		LOG.debug("");
	}

	/**
	 * @return The currently selected Scene.  Must be implemented by
	 * subclasses.
	 */
	protected abstract Scene findSelectedScene();

	/**
	 * @return The Scene prior to the currently selected Scene, or
	 * null if the current one is the first in the Script. Should be
	 * overridden by subclasses if used.
	 */
	@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
	protected Scene findPriorScene() {
		return null;
	}

	/**
	 * Called to add (insert) a new scene. The user has the option of copying data from the
	 * preceding scene.  The scene number is proposed by the software but may be changed
	 * by the user.
	 * @return null navigation String
	 */
	public String actionSaveAdd() {
		doSaveAdd();
		return null;
	}

	protected boolean doSaveAdd() {
		Scene scene;
		Scene copyScene = findSelectedScene();
		if (getPlaceAddScene().equals(ADD_SCENE_BEFORE)) {
			scene = findPriorScene();
		}
		else {
			scene = copyScene;
		}
		boolean b = actionSaveAdd(scene, copyScene);
		if ( ! b) {
			LOG.warn("actionSaveAdd failed");
		}
		return b;
	}

	/**
	 * Called to add (insert) a new scene following the given Scene. The user
	 * has the option of copying data from the preceding scene. The scene number
	 * is proposed by the software but may be changed by the user.
	 *
	 * @param scene The Scene preceding the point at which the new scene will be
	 *            inserted. If null, the new scene is inserted as the first
	 *            scene in the Script.
	 * @param copyScene The Scene which will be used as the source of related
	 *            ScriptElements, if isAddCopyExisting() is true.
	 * @return True iff a new Scene was successfully added.
	 */
	public boolean actionSaveAdd(Scene scene, Scene copyScene) {
		boolean b = false;
		String newSceneNum = getAddSceneNumber().trim();
		String msgId = ScriptUtils.validateSceneNumber(script, newSceneNum);
		if (msgId != null) { // duplicate or invalid style
			MsgUtils.addFacesMessage(msgId, FacesMessage.SEVERITY_ERROR);
		}
		else {
			actionCancelAdd();
			if (scene != null && scene.getId() != null) {
				scene = sceneDAO.findById(scene.getId());
			}
			LOG.debug("OLD scene="+scene);
			Scene newscene = ScriptUtils.makeInsertCopy(script, scene, true, newSceneNum, copyScene);
			if (isAddCopyExisting() && copyScene != null) {
				newscene.setScriptElements(new HashSet<>(copyScene.getScriptElements()));
				project = projectDAO.refresh(project);
				ProductionDood.markProjectDirty(project, false); // mark DooD out of date
			}
			addScene(newscene); // Add scene to our current script
			LOG.debug("NEW scene="+newscene);
			// Insert a matching strip, if needed
			insertStrip(newscene);
			b = true;
		}
		return b;
	}

	/**
	 * Delete a scene. This is only allowed if the scene is already marked as
	 * "omitted". This is an Action method called by the Delete button on one of
	 * the various Scene List pages.
	 *
	 * @return null navigation string
	 */
	public String actionDelete() {
		Scene scene = findSelectedScene();
		if (scene != null) {
			scene = sceneDAO.findById(scene.getId()); // refresh
			actionDelete(scene);
		}
		else {
			MsgUtils.addFacesMessage("SceneList.DeleteFailed", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Delete a scene.  This is only allowed if the scene is already
	 * marked as "omitted".
	 */
	protected boolean actionDelete(Scene scene) {
		boolean b = false;
		if (scene != null && scene.getOmitted()) {
			script = scriptDAO.removeScene(script, scene);
			b = true;
			removeOrphanStrip(scene);
		}
		if ( ! b) {
			EventUtils.logError("scene not removed from script; scene: " + scene);
		}
		return b;
	}

	/**
	 * Mark the currently selected scene as "omitted". It is not actually
	 * removed from the database, and its Strip (if any) remains unchanged. This
	 * method is called from BreakdownBean.confirmOk(), as a result of the user
	 * clicking OK on the confirmation dialog.
	 *
	 * @return null navigation string
	 */
	protected String actionOmitOk() {
		//String res = Constants.SUCCESS;
		Scene scene = findSelectedScene();
		if (scene != null) {
			actionOmit(scene);
		}
		else { // this shouldn't happen!
			MsgUtils.addFacesMessage("SceneList.OmitFailed", FacesMessage.SEVERITY_ERROR);
			//res = Constants.FAILURE;
		}
//		ListView.addClientResize();
		return null;
	}

	/**
	 * Mark the given scene as "omitted" (but it is not removed from the
	 * database). All Strips referring to this scene are also marked OMITTED. If
	 * it is merged with other scenes, it is first split off from those, and
	 * then omitted by itself.
	 */
	protected boolean actionOmit(Scene scene) {
		boolean b = false;
		if (scene != null && ! scene.getOmitted()) {
			project = projectDAO.refresh(project);
			scene.setOmitted(true);
			scene = sceneDAO.merge(scene);
			script = scriptDAO.findById(script.getId());
			Strip strip = stripDAO.findBySceneAndStripboard(scene.getNumber(), project.getStripboard());
			if (strip != null && strip.getHasMultipleScenes()) { // merged scenes
				strip = splitScene(scene, strip); // get this scene split off by itself
			}
			if (strip != null) {
				if (strip.getStatus() == StripStatus.SCHEDULED) {
					 // Omitting a scheduled strip will make the DooD out of date
					ProductionDood.markProjectDirty(project);
				}
				stripDAO.omitStrips(scene.getNumber(), project);
			}
			b = true;
		}
		if ( ! b) {
			LOG.warn("scene status not changed; scene:" + scene);
		}
		return b;
	}

	/**
	 * Mark an "omitted" scene as "normal". This is an Action method called by
	 * the Un-Omit button on one of the various Scene List pages. This method is
	 * called from BreakdownBean.confirmOk(), as a result of the user clicking
	 * OK on the confirmation dialog.
	 *
	 * @return null navigation string
	 */
	protected String actionRestoreOk() {
		Scene scene = findSelectedScene();
		if (scene != null) {
			actionUnomit(scene);
		}
		else { // shouldn't happen!
			MsgUtils.addFacesMessage("SceneList.UnomitFailed", FacesMessage.SEVERITY_ERROR);
		}
//		ListView.addClientResize();
		return null;
	}

	/**
	 * Mark an "omitted" scene as "normal"; change any Strips that refer to it
	 * to Unscheduled (from Omitted).
	 */
	protected boolean actionUnomit(Scene scene) {
		boolean b = false;
		if (scene != null && scene.getOmitted()) {
			scene.setOmitted(false);
			scene = sceneDAO.merge(scene);
			stripDAO.restoreStrips(scene.getNumber(), project);
			b = true;
		}
		if ( ! b) {
			LOG.warn("omitted scene status unchanged; scene:" + scene);
		}
		return b;
	}

	/**
	 * If the Strip for the specified Scene is now an orphan -- that is, the scene
	 * number does not exist in any other script -- then delete the Strip and remove
	 * it from its stripboard.
	 * @param scene
	 */
	protected void removeOrphanStrip(Scene scene) {
		String sceneNum = scene.getNumber();
		if (! sceneDAO.existsSceneInProject(sceneNum, project) ) {
			stripDAO.deleteOrphanStrips(sceneNum, project);
		}
	}

	/**
	 * Split a scene out of a Strip where it is merged with other scenes. While this action is
	 * usually handled on the breakdown page, it may be initiated automatically on other scene-list
	 * pages if the user does an "Omit" of a scene that is merged with others.
	 *
	 * @param split The scene to be removed from 'strip'.
	 * @param strip The Strip containing the 'split' scene plus at least one other scene.
	 */
	protected Strip splitScene(Scene split, Strip strip) {
		Strip newStrip = stripDAO.split(split, strip, SessionUtils.getCurrentProject());
		// That added it to db & updated the stripboard(s), too.
		setBreakdownMap(null); // no longer valid
		return newStrip;
	}


	/**
	 * Adds a scene to our current script, and guarantees that the scene
	 * is in the right order in the getScenes() List.
	 * @param newScene scene to be inserted, based on its sequence field.
	 */
	protected void addScene(Scene newScene) {
		boolean done = false;
		sceneDAO.save(newScene);
		LOG.info("new scene Id::"+newScene.getId());
		if (!done) {
			script = scriptDAO.findById(script.getId());
			script.getScenes().add(newScene);
			if (newScene.getPageNumber() > script.getLastPage()) {
				script.setLastPage(newScene.getPageNumber());
			}
		}
		script = scriptDAO.merge(script);
		setBreakdownMap(null); // any existing map is no longer valid
	}

	/**
	 * MAY insert a new Strip to match a newly-inserted Scene. This is a no-op in this class, but is
	 * overridden by other classes (such as ScriptFinalReviewBean) to implement this facility.
	 *
	 * @param newScene The Scene which the new Strip should specify.
	 */
	@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
	protected Strip insertStrip(Scene newScene) {
		return null;
	}

	/**
	 * Insert new Strip(s) to match a newly-inserted Scene. Adds it as an
	 * unscheduled Strip to all existing stripboards in the current Project.
	 *
	 * @param newScene The Scene which the new Strip should specify.
	 * @return The Strip that was added for the current stripboard.
	 */
	protected Strip doInsertStrip(Scene newScene) {
		project = projectDAO.refresh(project); // freshen
		Strip strip = stripDAO.addStrips(newScene, project);
		if (getBreakdownMap() != null) {
			getBreakdownMap().put(newScene.getNumber(), strip.getId());
		}
		return strip;
	}

	/**
	 * Action method for the "Add" button, which opens the "Add scene" popup.
	 * @return null navigation String
	 */
	public String actionOpenAddPopup() {
		LOG.debug("");
		doOpenAddPopup();
//		ListView.addClientResize();
		return null;
	}


	/**
	 * Open the "Add scene" popup.
	 */
	protected void doOpenAddPopup() {
		Scene scene  = findSelectedScene();
		if (scene == null) {
			String message = "Please select a scene first";
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_WARN, message);
		}
		else {
			selectAddScene = ADD_SCENE_NEW;
			placeAddScene = ADD_SCENE_AFTER;
			setAddCopyExisting(true);
			showAddPopup = true;
			setAddSceneNumber(sceneDAO.createNextSceneNumber(script, scene));
		}
	}

	/**
	 * Action method for the "cancel" button on the "Add Scene"
	 * pop-up dialog.
	 * @return null navigation string
	 */
	public String actionCancelAdd() {
		LOG.debug("");
		showAddPopup = false;
//		ListView.addClientResize();
		return null;
	}

	/**
	 * Select a row in the current list by creating an ace RowState object and
	 * adding to the {@link #stateMap}.
	 *
	 * @param entry The object (list item) to be selected.
	 */
	protected void selectRowState(Object entry) {
		RowState state = new RowState();
		state.setSelected(true);
		getStateMap().clear();
		getStateMap().put(entry, state);
		//setSelectedRow(entry);
	}

/*	public void listenSelectAdd(ValueChangeEvent evt) {
		String btn = (String)evt.getNewValue();
		log.debug("btn=" + btn);
	}
*/
	/** See {@link #showAddPopup}. */
	public boolean isShowAddPopup() {
		return showAddPopup;
	}
	/** See {@link #showAddPopup}. */
	public void setShowAddPopup(boolean showAddPopup) {
		this.showAddPopup = showAddPopup;
	}

	/** See {@link #addSceneNumber}. */
	public String getAddSceneNumber() {
		return addSceneNumber;
	}
	/** See {@link #addSceneNumber}. */
	public void setAddSceneNumber(String addSceneNumber) {
		this.addSceneNumber = addSceneNumber;
	}

	/** See {@link #addCopyExisting}. */
	public boolean isAddCopyExisting() {
		return addCopyExisting;
	}
	/** See {@link #addCopyExisting}. */
	public void setAddCopyExisting(boolean addCopyExisting) {
		this.addCopyExisting = addCopyExisting;
	}

	/** See {@link #selectAddScene}. */
	public String getSelectAddScene() {
		return selectAddScene;
	}
	/** See {@link #selectAddScene}. */
	public void setSelectAddScene(String selectAddScene) {
		this.selectAddScene = selectAddScene;
	}

	/** See {@link #placeAddScene}. */
	public String getPlaceAddScene() {
		return placeAddScene;
	}
	/** See {@link #placeAddScene}. */
	public void setPlaceAddScene(String placeAddScene) {
		this.placeAddScene = placeAddScene;
	}

	public Integer getScriptId() {
		return scriptId;
	}
	/**
	 * Setting the scriptId also sets the script.
	 */
	public void setScriptId(Integer id) {
		scriptId = id;
		setBreakdownMap(null); // no longer valid
		script = scriptDAO.findById(id);
		//SessionUtils.put(Constants.ATTR_IMPORT_SCRIPT_ID, id);
	}

	/**
	 * If no script has been set, this will acquire the current
	 * project's script & return that, if it exists.
	 */
	public Script getScript() {
		if (script == null) {
			script = SessionUtils.getCurrentProject().getScript();
			if (script != null) {
				scriptId = script.getId();
			}
			else {
				if (scriptId != null) {
					script = scriptDAO.findById(scriptId);
				}
			}
			LOG.debug("script#="+scriptId);
		}
		return script;
	}
	public void setScript(Script script) {
		this.script = script;
		setBreakdownMap(null); // no longer valid
	}

	public Integer getSceneId() {
		return sceneId;
	}
	public void setSceneId(Integer sceneId) {
		this.sceneId = sceneId;
	}

	protected Project initStripboard() {
		project = stripboardDAO.initStripboard(project, getScript(), Constants.DEFAULT_STRIPBOARD_NAME);
		return project;
	}

	/**
	 * Create a stripboard if none exists, or add strips to an existing stripboard
	 * for any scenes in the current script that do not have strips yet.
	 * This is done on entry to the "final review" page, or when exiting
	 * manual entry mode (when manual entry is complete). The current 'breakdownMap' field
	 * is used to determine if any new Strip`s must be created.
	 * See {@link #breakdownMap}.
	 */
	protected void fillStripboard() {
		// Scenes added by this revision won't have existing strips in the current stripboard.
		if (project.getStripboard() == null) {	// none yet -- presumably first script import
			project = initStripboard();
		}
		if (getBreakdownMap() == null) {
			return;	// should have been initialized
		}
		if (haveAllStrips) {
			return;	// nothing to do
		}

		if (getScript() != null) {
			// The breakdownMap field has an entry for every scene; if it is null,
			// then we're missing a strip for that scene!
			project = stripboardDAO.fillStripboard(project, script, getBreakdownMap());
			haveAllStrips = true;
		}
	}

	/** See {@link #breakdownMap}. */
	@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
	public Map<String, Integer> getBreakdownMap() {
		// "review" screen does not require or display breakdown strip numbers
		return null; /** Some subclasses, such as {@link BreakdownBean}, will override this. */
	}

	/** See {@link #breakdownMap}. */
	@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
	public void setBreakdownMap(Map<String, Integer> map) {
		breakdownMap = map; // may be overridden by subclasses
	}

	/** If the breakdownMap does not exist, create it; then return it.
	 *  See {@link #breakdownMap}. */
	protected Map<String, Integer> checkBreakdownMap() {
		if (breakdownMap == null) {
			initBreakdownMap();
		}
		return breakdownMap; // may be overridden by subclasses
	}

	/**
	 * Create a mapping of Scene.number to Strip.id for each
	 * Scene in the Script, using the current stripboard.
	 * See {@link #breakdownMap}.
	 */
	protected void initBreakdownMap() {
		breakdownMap = StripUtils.createBreakdownMap(project);
		haveAllStrips = false; // may have missing Strips
	}

	/**
	 * Non-static get() method so jsp's can retrieve the colorClassMap, which
	 * maps a scene's "colorKey" to the proper CSS class for the corresponding
	 * strip's color.
	 *
	 * @return the color class map
	 */
	public Map<String, String> getColorClassMap() {
		return StripUtils.getColorClassMap();
	}

	/** See {@link #stateMap}. */
	public RowStateMap getStateMap() {
		return stateMap;
	}
	/** See {@link #stateMap}. */
	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

}
