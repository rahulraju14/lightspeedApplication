//	File Name:	ScriptReviewBean.java
package com.lightspeedeps.web.script;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.event.ActionEvent;
import javax.annotation.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.SceneListItem;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * A class for backing two Script Import-related pages:
 * 	4.3.1, Script Import step 2, "Cleanup & Review"
 * 	4.3.3, Script Import step 4, "Breakdown & Final Review"
 */
@ManagedBean
@ViewScoped
public class ScriptReviewBean extends SceneListBase implements LocationHolder, PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = 6615002678239377007L;

	private static final Log log = LogFactory.getLog(ScriptReviewBean.class);

	/*package*/ static final int ACT_CANCEL_IMPORT = 10;

	private boolean finalReview = false; // set to true by ScriptFinalReviewBean

	/** True if any row is in edit mode. */
	private boolean editMode = false;
	private SelectItem lastMatchedItem = null;
//	private int currentRowIndex = -1;
//	private int dropTarget = -1;
	private String newLocationName;
	private LocationSelector locationSelector;
	private int screenSize = 20;
	protected boolean busy;	// prevent double-click from double-executing some code

	/**
	 * Holds a list with one item, which is the "drag source" for
	 * inserting new scenes into the script.
	 */
//	private List<SceneListViewerBean> dragSceneList;

	private List<SelectItem> locationList = new ArrayList<SelectItem>();
	/**
	 * Indicates if all higher-numbered scenes should be automatically
	 * renumbered when a new scene is inserted (via drag & drop).
	 */
	private boolean renumberScenes = false;

	protected int showRowNumber = 0;

	protected List<Strip> stripList = null;

	protected List<SceneListItem> sceneViewList = new ArrayList<SceneListItem>();

	protected HashMap<Integer, List<String>> listHash = null;

	protected HashMap<Integer, String> nameHash = null;

	public ScriptReviewBean() {
		log.debug(""+hashCode());
	}

	@Override
	@PostConstruct
	protected void init() {
		log.debug(""+hashCode());
		if (getScript() != null) {	// try to get import script
			checkStripboard(); // note that subclasses may override the method in this class
		}
		super.init();
		setSceneBeanData(project);
//		initDragData(); // create drag&drop source item
		setLocationList(createLocationList());
		locationSelector = (LocationSelector) ServiceFinder.findBean("locationSelector");
		locationSelector.setLocationHolder(this);	// for call-backs
	}

	/**
	 * Called during initialization (post-construction) to take care of
	 * initializing a stripboard so that it matches the script under review.
	 * However, the "import review" screen does not use the stripboard,
	 * so this method is a no-op.  It is overridden in the final review class.
	 */
	protected void checkStripboard() {
		// The "import review" screen does not need to do this.
	}

	@Override
	public Script getScript() {
		if (script == null) {
			setBreakdownMap(null); // can't be valid
			Integer id = getScriptId();
			if (id != null) {
				if (script == null) {
					script = SessionUtils.getCurrentProject().getScript();
					if (script != null) {
						scriptId = script.getId();
					}
				}
			}
		}
		//log.debug("script#="+scriptId);
		return script;
	}

	@Override
	public Integer getScriptId() {
		Integer id = SessionUtils.getInteger(Constants.ATTR_IMPORT_SCRIPT_ID);
		if (id != null) {
			// validate that this id belongs to the current project!
			Script s = (Script)scriptDAO.findById(id);
			if (s != null && ! s.getProject().getId().equals(project.getId())) {
				// wrong project, clear the session attribute
				id = null;
				SessionUtils.put(Constants.ATTR_IMPORT_SCRIPT_ID, null);
			}
		}
		if (id == null) {
			id = getDefaultScriptId();
		}
		if (id != null) {
			setScriptId(id); // this will also set 'script'
		}
		return scriptId;
	}

	protected Integer getDefaultScriptId() {
		Integer id = null;
		if (project != null) {
			Script tscript = project.getScript();
			if (tscript != null) {
				id = tscript.getId();
			}
		}
		return id;
	}

	/**
	 * Delete a scene.  This is only allowed if the scene is already
	 * marked as "omitted".
	 * @return null navigation string
	 */
	@Override
	public String actionDelete() {
		SceneListItem viewer = findSelectedSceneViewer();
		setSceneId(viewer.getSceneId());
		log.debug("delete id="+sceneId);
		if (sceneId != null) {
			Scene scene = sceneDAO.findById(sceneId);
			boolean b = actionDelete(scene);
			if (b) {
				actionEditOff(viewer);
			}
			else {
				log.warn("scene not removed from script; scene:"+scene);
			}
		}
		else {
			MsgUtils.addFacesMessage("SceneList.DeleteFailed", FacesMessage.SEVERITY_ERROR);
		}
		setSceneBeanData(project);
		return null;
	}

	private void actionEditOff(SceneListItem viewer) {
		showRowNumber = -1;
		setEditMode(false);
		if (viewer != null) {
			viewer.setEdit(false);
		}
	}

	/**
	 * Mark a scene as "edit mode"; any existing scene that was
	 * in edit mode is removed from edit mode, and the backing
	 * bean (SceneListViewerBean) for it is recreated to make sure
	 * that it reflects any changes to the Scene object.
	 */
	public String actionEdit() {
		setEditMode(false);
		//saveScript();
		//log.debug("row="+showRowNumber);
		for (SceneListItem viewer : sceneViewList ) {
			if (viewer.getEdit()) {
				Scene scene = viewer.getScene();
				if (scene != null && scene.getId() != null) {
					try { // had problems with null values in scene.dnType! see bug#419.
						sceneDAO.attachDirty(scene);
					}
					catch (Exception e) {
						EventUtils.logError(e);
						try {
							viewer.setScene(sceneDAO.refresh(scene));
						}
						catch (Exception e1) {
							EventUtils.logError(e1);
						}
					}
				}
				SceneListItem newBean = createViewerBean(viewer.getScene());
				newBean.setRowNumber(viewer.getRowNumber());
				newBean.setShowData(viewer.getShowData());
				sceneViewList.set(viewer.getRowNumber(), newBean);
			}
			if (showRowNumber == viewer.getRowNumber()) {
				if (viewer.getEdit()) {
					viewer.setEdit(false);
				}
				else {
					Scene scene = viewer.getScene();
					if (scene != null /*&& ! scene.getDeleted()*/) {
						viewer.setEdit(true);
						setEditMode(true);
						if (scene.getScriptElement() != null) {
							locationSelector.setLocationId(scene.getScriptElement().getId());
						}
						else {
							locationSelector.setLocationId(-1); // "select location" prompt
						}
					}
				}
			}
			else {
				viewer.setEdit(false);
			}
		}
		return null;
	}

	/**
	 * Renumber the breakdown sheets sequentially, matching the scene numbers.
	 * @return null navigation string
	 */
	public String actionRedoBreakdown() {
		log.debug("start");
		project = SessionUtils.getCurrentProject();
		Stripboard stripboard = project.getStripboard();

		stripboardDAO.renumberStrips(stripboard, getScript());
		setBreakdownMap(null); // any existing map is no longer valid
		setSceneBeanData(project);

		String text = "Breakdown sheets were renumbered.";
		FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, text, text);
		FacesContext.getCurrentInstance().addMessage(null, msg);
		log.debug("end");
		return null;
	}

	/**
	 * Called when user hits "Next" button to exit this page and
	 * continue to the comparison page.
	 */
	public void exitReview(ActionEvent evt) {
		log.debug("");
		if (! busy) { // skip if user double-clicked "Next" button
			busy = true;
			try {
				saveScript();	// save any scene changes
				// set parameter for import compare, not draft compare:
				SessionUtils.put(Constants.ATTR_SCRIPT_COMPARE_IMPORT, true);
			}
			catch (RuntimeException e) {
				EventUtils.logError("saveScript failed: ", e);
			}
			finally {
				log.debug("done");
				busy = false;
			}
		}
	}

	/**
	 * Called when user hits "Done" button to exit the manual
	 * entry page and continue to the Script Drafts page.
	 */
	public void exitManualEntry(ActionEvent evt) {
		log.debug("");
		saveScript(); // save any pending edits
		fillStripboard();	// create & populate a stripboard if none exists
		ScriptDraftsBean.navigateToScriptDrafts(); // set headers for script | drafts page
	}

	public String actionCancelImport() {
		PopupBean.getInstance().show(
				this, ACT_CANCEL_IMPORT, "ImportScript.ConfirmCancel.");
		//ListView.addClientResize();
		return null;
	}

	@Override
	public String confirmCancel(int action) {
		return null;
	}

	@Override
	public String confirmOk(int action) {
		cancelImport();
		return "cancel";
	}

	/**
	 * Called when user clicks OK on the "Cancel Import" prompt on Review, Comparison,
	 * Manual Entry, or Final Review pages.
	 */
	public void cancelImport() {
		log.debug("");
		try {
			script = getScript();
			stripList = null;
			sceneViewList = null;
			purgeImport(script);
			setScript(null);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		ScriptDraftsBean.navigateToScriptDrafts(); // set headers for script | drafts page
	}

	@SuppressWarnings("unchecked")
	/*package*/ static void purgeImport(Script script) {
		if (script != null) {
			log.debug("");
			boolean purged = false;
			script = ScriptDAO.getInstance().refresh(script);
			Project project = ProjectDAO.getInstance().refresh(script.getProject());
			if (project.getScript() != null && project.getScript().getId() == script.getId()) {
				ProjectDAO.getInstance().setScript(project, null);
				ProjectDAO.getInstance().merge(project);
				purged = true;
			}
			ScriptDAO.getInstance().delete(script); // Scenes deleted via cascade
			if (purged) {
				ScriptElementDAO.getInstance().deleteAll(project); // remove all ScriptElements, too.
			}
			if (! purged) {
				List<Integer> addedElements = (List<Integer>)SessionUtils.get(Constants.ATTR_IMPORT_ADDED_ELEMENTS);
				if (addedElements != null) {
					SessionUtils.put(Constants.ATTR_IMPORT_ADDED_ELEMENTS, null);
					ScriptElementDAO.getInstance().deleteAll(addedElements);
				}
			}
		}
	}

	/**
	 * @return a non-null List of SelectItems from the Location type ScriptElements in this project.
	 */
	private List<SelectItem> createLocationList() {
		return LocationSelector.createLocationList(scriptElementDAO, project);
	}

	protected void saveScript() {
		log.debug(""+hashCode());
		for (SceneListItem bean : sceneViewList) {
			Scene scene = bean.getScene();
			if (scene != null && scene.getId() != null) {
				sceneDAO.attachDirty(scene);
			}
		}
		/* This used to do "merge(script)", but updating the individual Scene's is what
		 * we really need.  The 'script' object's scene collection may be out of date. */
		sceneDAO.flushAndClear(); // so double-click won't throw Hibernate errors
	}


	public boolean isScriptActive() {
		boolean b = false;
		if (project.getScript() == null) {
			b = true;
		}
		else if (getScript() != null && project.getScript().getId() == getScript().getId()) {
			b = true;
		}
		return b;
	}

	// * * * Accessors & Mutators

	public List<SelectItem> getLocationList() {
		return locationList;
	}
	public void setLocationList(List<SelectItem> locationList) {
		this.locationList = locationList;
	}

	public String getNewLocationName() {
		return newLocationName;
	}
	public void setNewLocationName(String newLocationName) {
		log.debug("loc="+newLocationName);
		this.newLocationName = newLocationName;
	}

	public boolean isFinalReview() {
		return finalReview;
	}
	public void setFinalReview(boolean finalReview) {
		this.finalReview = finalReview;
	}

	public boolean getRenumberScenes() {
		return renumberScenes;
	}
	public void setRenumberScenes(boolean renumberScenes) {
		this.renumberScenes = renumberScenes;
	}

	public SelectItem getLastMatchedItem() {
		return lastMatchedItem;
	}
	public void setLastMatchedItem(SelectItem lastMatchedItem) {
		this.lastMatchedItem = lastMatchedItem;
	}

	public int getScreenSize() {
		return screenSize;
	}
	public void setScreenSize(int screenSize) {
		this.screenSize = screenSize;
	}

	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}
	public boolean getEditMode() {
		return editMode;
	}

	// * * * Methods for LocationHolder interface * * *

	@Override
	public void locationUpdated() {
	}

	@Override
	public void saveChanges() {
	}

	@Override
	public void updateScene(Scene scene) {
		SceneListItem sceneView = null;
		if (showRowNumber < sceneViewList.size()) {
			sceneView = sceneViewList.get(showRowNumber);
			sceneView.setScene(scene);
		}
	}

	@Override
	public Scene getScene() {
		Scene scene = null;
		SceneListItem sceneView = null;
		if (showRowNumber < sceneViewList.size() && showRowNumber >= 0) {
			sceneView = sceneViewList.get(showRowNumber);
			if (sceneView != null) {
				scene = sceneView.getScene();
				//log.debug("row="+showRowNumber+", scene=" + scene.getNumber());
			}
		}
		return scene;
	}

	/**
	 * showRowNumber is set by the f:setPropertyActionListener on the +/-
	 * web page controls, on importreview.jsp and others.
	 */
	public int getShowRowNumber() {
		return showRowNumber;
	}

	public void setShowRowNumber(int showRowNumber) {
		this.showRowNumber = showRowNumber;
	}

	public List<SceneListItem> getSceneViewList() {
		return sceneViewList;
	}

	public void setSceneViewList(List<SceneListItem> sceneViewList) {
		this.sceneViewList = sceneViewList;
	}

	public void setSceneBeanData(Project tproject) {
		log.debug("");
		script = getScript();
		if (script != null) {
			sceneViewList = prepareStripTable();
		}
	}

	/**
	 * This is the action method referenced in importreview.jsp,
	 * called when the user clicks the +/i button to expand or
	 * collapse the display of script elements within the scene.
	 * @return null navigation string
	 */
	public String expandTableListener() {
		log.debug("showrownumber="+showRowNumber);
		int rowNumber = showRowNumber;
		for (SceneListItem viewer : sceneViewList ) {
			int rowNum=viewer.getRowNumber();
			if (rowNumber==rowNum) {
				if (viewer.getShowData()) {
					viewer.setShowData(false);
				}
				else {
					viewer.setShowData(true);
				}
			}
			else {
				viewer.setShowData(false);
			}
		}
		return null;
	}

	protected SceneListItem findSelectedSceneViewer() {
		log.debug("");
		SceneListItem sceneView = null;
		// we no longer support binding= from page
//		if (getSceneTable() != null && getSceneViewList() != null) {
//			log.debug("ix="+showRowNumber);
//			if (showRowNumber >= 0 && showRowNumber < getSceneViewList().size()) {
//				sceneView = getSceneViewList().get(showRowNumber);
//			}
//		}
		return sceneView;
	}

	@Override
	protected Scene findSelectedScene() {
		log.debug("");
		Scene scene = null;
		SceneListItem sceneView = findSelectedSceneViewer();
		if (sceneView != null) {
			scene = sceneView.getScene();
		}
		return scene;
	}

	/**
	 * Creates and populates a List of SceneListViewerBean, which will be
	 * the table displayed by the various scene list pages (manualentry.jsp,
	 * importreview.jsp, etc.)
	 * @return A List of SceneListViewerBean, with an entry for each
	 * Scene in the Script 'script'.
	 */
	private List<SceneListItem> prepareStripTable() {
		log.debug("");
		SceneListItem sceneListViewerBean;
		List<SceneListItem> sceneListViewerList = new ArrayList<SceneListItem>();
		List<Scene> scenes = script.getScenes(); // The list of scenes, in Script order.
		int rowNumberScenes = 0;
		for (Scene sceneObj : scenes ) {
			//log.info("stripObj  scene number::" + sceneList.get(sceneIndex).getNumber());
			//log.info("stripObj  scene row number::" + rowNumberScenes);
			sceneListViewerBean = createViewerBean(sceneObj);
			sceneListViewerBean.setRowNumber(rowNumberScenes);
			sceneListViewerList.add(sceneListViewerBean);
			rowNumberScenes++;
		}
		return sceneListViewerList;
	}

	/**
	 * Create the backing bean for one row (one scene) in a scene list.
	 * Used on Scene List page, and Script Import Review and Final Review pages.
	 * @param scene The scene that the bean represents.
	 * @return The matching SceneListViewerBean with all fields filled
	 * EXCEPT for the row number -- the caller must set this.
	 */
	protected SceneListItem createViewerBean(Scene scene) {
		SceneListItem sceneListViewerBean = new SceneListItem();
		sceneListViewerBean.setScene(scene); // DH to test pass-thru of data updates
		sceneListViewerBean.setSceneId(scene.getId());
		sceneListViewerBean.setSceneAlpha(scene.getSceneAlpha());
		sceneListViewerBean.setOrderNumber(scene.getSequence());

		if (getBreakdownMap() != null) {
			Integer sheetNum = getBreakdownMap().get(scene.getNumber());
			if (sheetNum != null) {
				Strip strip = stripDAO.findById(sheetNum);
				if (strip != null && strip.getSheetNumber() != null) {
					sceneListViewerBean.setSheetNumber(strip.getSheetNumber().toString());
				}
			}
		}
		// Not used
		// sceneListViewerBean.setId(stripObj.getId());
		// sceneListViewerBean.setSynopsis(stripObj.getSynopsis());
		// sceneListViewerBean.setStatus(stripObj.getStatus());
		// sceneListViewerBean.setSceneNumbers(stripObj.getSceneNumbers());
		// sceneListViewerBean.setScenes(null);
		sceneListViewerBean.setSceneNumbers(scene.getNumber());
		sceneListViewerBean.setLength(scene.getLength());
		if (scene.getSet()!=null) {
			sceneListViewerBean.setTitle(scene.getSet().getName());
			sceneListViewerBean.setLocationId(scene.getSet().getId());
		}
		else {
			sceneListViewerBean.setTitle(Constants.NO_SET_NAME);
			sceneListViewerBean.setLocationId(-1);
		}
		sceneListViewerBean.setIntEType(scene.getIeType());
		sceneListViewerBean.setDayNumber(scene.getScriptDay());
		sceneListViewerBean.setDayNType(scene.getDnType());
		sceneListViewerBean.setHeading(scene.getHeading()); // get pre-built scene heading
		// here we get the script_ElementId's
		Set<ScriptElement> sceneScriptElements=scene.getScriptElements();
		Integer columnCount = 0;
//		int ix = 0;
		for (ScriptElementType type : ScriptElementType.values()) {
			if (type.equals(ScriptElementType.N_A)) { // ignore N/A and any following values
				break;
			}
			List<String> list = SceneDAO.getScriptElements(sceneScriptElements, type);
			// setting the column values
			if (list.size() != 0) {
				sceneListViewerBean.setArray(columnCount,list);
				sceneListViewerBean.setColumn(columnCount, type.toString());
				columnCount++;
			}
//			ix++;
		}
		sceneListViewerBean.setColumnCount(columnCount - 1);
		//log.info("columnCount:::"	+ sceneListViewerBean.getColumnCount());
		if (columnCount == 0) {
			sceneListViewerBean.setArray(0, new ArrayList<String>());
			sceneListViewerBean.setColumn(0, Constants.NO_SCRIPT_ELEMENTS);
		}
		return sceneListViewerBean;
	}

}
