package com.lightspeedeps.web.script;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.combobox.ComboBox;
import org.icefaces.ace.event.SelectEvent;

//import com.icesoft.faces.component.ext.HtmlPanelGroup;
//import com.icesoft.faces.component.ext.HtmlSelectOneListbox;
//import com.icesoft.faces.component.ext.RowSelectorEvent;
//import com.icesoft.faces.component.selectinputtext.SelectInputText;

import com.lightspeedeps.dao.NoteDAO;
import com.lightspeedeps.dao.RealWorldElementDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.model.Note;
import com.lightspeedeps.model.RealWorldElement;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.model.Strip;
import com.lightspeedeps.model.Stripboard;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.object.Item;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.util.script.ScriptUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.util.UserPrefs;
import com.lightspeedeps.web.view.ListView;

/**
 * Backing bean for the Breakdown Page View and Edit screens.
 */
@ManagedBean
@ViewScoped
public class BreakdownBean extends SceneListBase implements LocationHolder, PopupHolder,  Serializable {
	/** */
	private static final long serialVersionUID = - 7117648737877668127L;

	private static final Log LOG = LogFactory.getLog(BreakdownBean.class);

	private static final String ATTR_BREAKDOWN_MINI_TAB = Constants.ATTR_CURRENT_MINI_TAB + "Breakdown";

	private static final String ATTR_SCENE_NUM = Constants.ATTR_PREFIX + "breakdownSceneNumber";
	private static final String ATTR_BREAKDOWN_AUTOSAVE = Constants.ATTR_PREFIX + "breakdownAutosave";

	/** The regular-expression String that matches the HTML ID tag generated for our
	 * auto-complete input fields on the breakdown page.  This value is
	 * dependent on multiple id="" values within the page! */
	private static final  String ID_PATTERN_AUTO_COMPLETE_STR = "bd:etbl(\\d+):(\\d+):in";

	/** The compiled pattern for matching the HTML id values of our input fields. */
	private static final  Pattern ID_PATTERN_AUTO_COMPLETE = Pattern.compile(ID_PATTERN_AUTO_COMPLETE_STR);

	private final static int ACT_DELETE_SCENE = 1; // for confirmation prompts
	private final static int ACT_OMIT_SCENE = 2; // for confirmation prompts
	private final static int ACT_RESTORE_SCENE = 3; // for confirmation prompts
	private final static int ACT_REMOVE_ELEMENT = 4; // for confirmation prompts

	/** The minimum time allowed (in milliseconds) after we finish readying a scene for rendering
	 * before we will accept a valueChangeEvent as valid. */
	private static final long MINIMUM_CHANGE_DELAY = 800;
	/** The minimum time allowed (in milliseconds) between two successive ValueChangeEvent`s; if the
	 * interval is less than this, the event is considered invalid. */
	private static final long MINIMUM_CHANGE_INTERVAL = 25;

	//private static final int TAB_DETAIL = 0;
	//private static final int TAB_NOTES = 1;

	/** The list of Scenes comprising the current script. */
	@SuppressWarnings("hiding")
	private List<Scene> sceneList;

	/** The current stripboard. */
	private Stripboard stripboard;

	/** The currently displayed Strip. */
	private Strip strip;

	/** The first (and possibly only) Scene for the currently displayed Strip. */
	private Scene scene;

	/** The collection of Scenes (including the current one) that are merged onto the
	 * current Strip. */
	private List<Scene> mergedScenes;

	/** True if the currently selected scene is merged with any other scene(s). */
	private boolean sceneMerged;

	/** The shooting day number (as a String) for the current Scene/Strip, if
	 * it is scheduled, else the text to display, such as "Unscheduled". */
	private String shootDayNumber;

	/** The shooting date for this Scene/Strip, if it is scheduled, or null if
	 * it is not. */
	private Date shootDate;

	/** The zero-origin index of the currently displayed strip, within the list of strips. */
	private int listIndex = -1;

	/** String which backs the text-box where the user enters a new note. */
	private String noteText;

	private List<Note> addedNotes;

	/** The list of merged scene numbers to display, or "none" */
	private String mergedSceneString = "";

	/** This flag controls the display of the "Merge Scene" pop-up dialog */
	private boolean showMerge = false;

	/** This flag controls the display of the "Split Scene" pop-up dialog */
	private boolean showSplit = false;

	/** Current sort setting - by Id or alphabetically */
	private String sortStyleValue = SORT_STYLE_ID;
	/** sort value for "by Id" in JSP drop-down selection list */
	private final static String SORT_STYLE_ID = "id";
	/** sort value for "alphabetically" in JSP drop-down selection list */
	//private final static String SORT_STYLE_ALPHA = "alpha"; // not referenced in Java

	/** True iff element sort is by ID; false iff sort is alphabetical. */
	private boolean sortById = true;

	/** True if Autosave is enabled. */
	private boolean autosave = true;

	private String autosaveValue = AUTOSAVE_ON;
	private final static String AUTOSAVE_ON = "ON";
	//private final static String AUTOSAVE_OFF = "OFF";

	/** Set to true when we have un-saved changes to the current Scene. */
	private boolean dataChanged;

	private boolean editMode = false;

	/** The zero-origin index of the currently selected tab (pane). */
	private int selectedTab = 0;

	/** The input field in the Add Element pop-up for creating a new element. */
	private String newElementName;

	/** For LSMOBILE - list of element types present in a scene. */
	private List<ScriptElementType> scriptElementTypeList;

	private RealWorldElement realLocation;

	private static final int MAX_COLUMNS = ScriptElementType.values().length;

	/** An array of column headings for the script element 'boxes'.  This is
	 * indexed by the ordinal value of the corresponding ScriptElementType. */
	private final String[] columns = new String[MAX_COLUMNS];

	/** The index of the ScriptElement table command button being clicked. */
	private int typeIndex;

	/** The scriptElement type index of the last processed "value change" event.  Used
	 * to 'guess' that a tab-key event was invoked with the 'wrong' element index. */
//	private int lastElementTypeIx;

	/** Input fields that back the "auto-complete" input fields within the
	 * element table. */
	private String[] elementInput =  new String[MAX_COLUMNS];

	/** The List of elements to be displayed below the auto-complete field,
	 * as the possible choices.  The array of Lists is indexed by the
	 * ordinal value of the ScriptElementType it corresponds with. */
//	@SuppressWarnings("unchecked")
//	private List<SelectItem>[] autoElementList = new List[MAX_COLUMNS];

	/** The List of elements to be displayed below the auto-complete field,
	 * as the possible choices.  The array of Lists is indexed by the
	 * ordinal value of the ScriptElementType it corresponds with. */
	@SuppressWarnings("unchecked")
	private List<SelectItem>[] elementList = new List[MAX_COLUMNS];

	/**
	 * An array of Lists containing the names of the script elements belonging
	 * to the current Scene, one array (List entry) for each ScriptElementType.
	 * The entries in the arrays are Item's, which simply hold a name and id. By
	 * using an Item instead of an actual ScriptElement, we minimize the memory
	 * footprint and avoid problems with cascading object references.
	 */
	@SuppressWarnings("unchecked")
	private final List<Item>[] elements = new List[MAX_COLUMNS];

	private LocationSelector locationSelector;
//	private StripUtils stripUtils = StripUtils.getInstance();

	/** A list of scene #'s & names for selecting in the Merge Scene pop-up */
	List<SelectItem> mergeChoices;
	/** The 'value' field for the selection listbox of sceneChoices. */
	Integer mergeValue = 0;

	/** A list of scene #'s & names for the Split Scene pop-up */
	List<Scene> splitList;

	/** Used to prevent intermittent extraneous and invalid ValueChangeEvent`s from being allowed
	 * to update scenes.  Typically occurred when quickly switching between several scenes. */
	Date refreshTime = new Date();

	/** The last time we got a ValueChangeEvent.  We'll check for too short an interval between
	 * successive events, and consider that an indication of invalid events. */
	Date lastChangeTime;

	/** Set to true if we have invalid ValueChangeEvent`s detected.  This prevents the next autosave
	 * from updating the database. */
	boolean skipSave = false;

	/* Constructor */
	public BreakdownBean() {
		LOG.debug("");
	}

	@Override
	@PostConstruct
	protected void init() {
		LOG.debug("");
		super.init();
		try {
			autosave = UserPrefs.getBoolean(ATTR_BREAKDOWN_AUTOSAVE, true);
			for (int i = 0; i < elements.length; i++) {
				elements[i] = new ArrayList<>();
//				autoElementList[i] = new ArrayList<>();
				elementList[i] = new ArrayList<>();
				elementInput[i] = "";
			}
			createColumnHeadings(columns);
			script = project.getScript();
			if ( ! SessionUtils.isMobileApp()) {
				locationSelector = (LocationSelector)ServiceFinder.findBean("locationSelector");
				locationSelector.setLocationHolder(this); // for call-backs
				locationSelector.setAutosave(false);
			}
			if (script != null) {
				fillStripboard();
			}
			stripboard = project.getStripboard();
			//editMode = SessionUtils.getBoolean(ATTR_BK_EDIT, false);
			if (stripboard != null && script != null) {
				createSceneList();
				LOG.debug("stripboard id=" + stripboard.getId() + ", # scenes=" + sceneList.size());
				String sceneNum = SessionUtils.getString(ATTR_SCENE_NUM);
				findListIndexFromSceneNum(sceneNum);
				if (listIndex >= sceneList.size()) {
					listIndex = 0;
				}
				if (sceneList.size() > listIndex) {
					scene = sceneList.get(listIndex);
					setupFromScene(true);
				}
				Integer tab = SessionUtils.getInteger(ATTR_BREAKDOWN_MINI_TAB);
				if (tab != null) {
					setSelectedTab(tab);
				}
			}
			else { // not a normal occurrence!
				sceneList = new ArrayList<>();
				mergedScenes = new ArrayList<>();
				listIndex = 0;
				strip = new Strip();
				scene = new Scene();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Update our scene list from the Script's list of Scene`s, and update
	 * ScriptPageBean's internal values. Also sets all 'selected' flags to false
	 * (used for highlighting the currently selected Scene in the left-hand
	 * list).
	 */
	private void createSceneList() {
		sceneList = script.getScenes();
		for (Scene s : sceneList) {
			s.setSelected(false);
		}
		// invoke ScriptPageBean to refresh its list of scenes:
		ScriptPageBean.getInstance().setupScriptScene(script, 0);
	}

	private void findListIndexFromSceneNum(String sceneNum) {
		if (sceneNum != null) {
			for (listIndex = 0; listIndex < sceneList.size(); listIndex++) {
				if (sceneList.get(listIndex).getNumber().equals(sceneNum)) {
					break;
				}
			}
		}
		if (listIndex < 0 || listIndex >= sceneList.size()) {
			listIndex = 0;
		}
	}

	/**
	 * Generates the Set of the 'family' of the given element, which is the
	 * element itself plus all its distinct descendants except for any LOCATION
	 * elements.
	 *
	 * @param se The ScriptElement whose descendants are to be found.
	 * @return A possibly empty, but non-null, Set of ScriptElements, consisting
	 *         of the element itself, plus the children of this element, and
	 *         those items' children, etc., with only a single instance of each
	 *         one in the Set. All elements of type LOCATION are removed *after*
	 *         the Set is initially generated, so that children of LOCATION
	 *         elements are included.
	 */
	public static Set<ScriptElement> createFamily(ScriptElement se) {
		Set<ScriptElement> group = new HashSet<>();
		createFamily(group, se, ScriptElementDAO.getInstance());
		for (Iterator<ScriptElement> iter = group.iterator(); iter.hasNext(); ) {
			ScriptElement elem = iter.next();
			if (elem.getType() == ScriptElementType.LOCATION) {
				iter.remove();
			}
		}
		return group;
	}

	/**
	 * Recursively accumulate all the elements that are linked to
	 * the given element, placing them all in 'group'
	 * @param group
	 * @param se
	 */
	private static void createFamily(Set<ScriptElement> group, ScriptElement se, ScriptElementDAO seDAO) {
		boolean added = group.add(se);
		if (added && (se.getChildElements() != null)) {
			// wasn't in our set before, so add its children
			for (ScriptElement child : se.getChildElements()) {
				child = seDAO.refresh(child);
				createFamily(group, child, seDAO);
			}
		}
	}

	/**
	 * Called when a different strip is to be displayed. It discards any changes (to Strip and
	 * Scene) if in edit mode, then calls setupFromScene() to handle the rest.
	 *
	 * @param index - The zero-origin index within the stripList indicating the Strip to be made
	 *            current. If the requested index is past the end, then the last scene is selected.
	 *            If it is negative, the first (0'th) scene is selected.
	 */
	private void setupFromScene(int index) {
		if (editMode && ! autosave) {
			cancelChanges();
		}
		setupFromScene(index, false);
	}

	private void setupFromScene(int index, boolean doSave) {
		if (index < 0) {
			index = 0;
		}
		if (index >= sceneList.size()) {
			index = sceneList.size() - 1;
		}
		if (index >= 0) {
			if (index != listIndex) { // changing page
				if (listIndex >= 0 && listIndex < sceneList.size()) {
					sceneList.get(listIndex).setSelected(false);
				}
//				if (doSave) { // old code from when we auto-saved changes
//					saveScene();
//				}
				listIndex = index;
				scene = sceneList.get(listIndex);
				setupFromScene(false);
				if (editMode) {
					refreshTime = new Date();
					lastChangeTime = null;
				}
			}
			scene.setSelected(true);
		}
	}

	/**
	 * Called when a different scene (and possibly Strip) is to be displayed. It initializes all the
	 * fields referenced by the jsp page to match the newly-selected scene. The instance fields
	 * "scene" and "listIndex" must be set before calling this method.
	 */
	private void setupFromScene(boolean doScroll) {
		if (scene != null) {
			project = projectDAO.refresh(project);
			scene = sceneDAO.refresh(scene); // refresh; got lazy init errors w/o this.
			sceneList.set(listIndex, scene); // replace in list
			Integer id = getBreakdownMap().get(scene.getNumber());
			if (id == null) {
				strip = null;
			}
			else {
				strip = stripDAO.findById(id);
			}
			if (strip == null) {
				// possibly user switched scripts or stripboards: we're missing a Strip in
				// the current stripboard for this scene -- so just create one.
				strip = insertStrip(scene);
			}
			scene.setSelected(true);
			setupLocation();
			createMergedScenes(); // update "merged scenes" display string & collection
			createElementArrays(mergedScenes, elements);
			SessionUtils.put(ATTR_SCENE_NUM, scene.getNumber());
			createScriptElementTypeList();
			createShootingDayInfo();
			forceLazyInit();
			if (doScroll) {
				addJavaScript();
			}
			// Invoke Javascript function to scroll Script window to the top
			ListView.addJavascript("scrollScriptTop();");
//			if (editMode) { // hide all '+' icons when switching scenes
//				ListView.addJavascript("bdHidePlusAll();");
//			}
			//mergeChoices = createMergeChoices();
			// Update the script display area
			ScriptPageBean.getInstance().displayScene(listIndex);
			selectRowState(scene);
		}
	}

	private void setupLocation() {
		if (scene.getScriptElement() != null) {
			if (locationSelector != null) { // (null for LSMOBILE)
				locationSelector.setLocationId(scene.getScriptElement().getId());
			}
			realLocation = RealWorldElementDAO.getInstance().findLinkedRealWorldElement(scene.getScriptElement());
		}
		else {
			if (locationSelector != null) { // (null for LSMOBILE)
				locationSelector.setLocationId( -1); // "select location" prompt
			}
		}
	}

	/**
	 * These fields are referenced on some tab (such as Notes), and are not
	 * initialized when the Scene/Strip object is still in the session that obtained
	 * it.  We need to force Hibernate to load them while the object is
	 * still in the original session.  This applies to fields marked as Fetch=LAZY.
	 */
	private void forceLazyInit() {
		LOG.debug("");
		@SuppressWarnings("unused")
		String s;
		// Initialize for Notes tab
		for (Note note : strip.getNotes()) {
			s = note.getUser().getDisplayName();
		}
	}

	private void createShootingDayInfo() {
		if (strip != null) {
			if (strip.getStatus() == StripStatus.UNSCHEDULED) {
				shootDayNumber = "Unscheduled";
				shootDate = null;
			}
			else if (strip.getStatus() == StripStatus.OMITTED) {
				shootDayNumber = "Omitted";
				shootDate = null;
			}
			else {
				int dayNum = stripDAO.findShootDayNumber(strip);
				if (dayNum > 0) {
					shootDayNumber = dayNum + "";
					Unit u = UnitDAO.getInstance().findById(strip.getUnitId());
					ScheduleUtils su = new ScheduleUtils(u);
					shootDate = su.findShootingDay(dayNum);
				}
			}
		}
	}

	/**
	 * Set the "mergedSceneString" string value (for JSP display) to list any scenes that are
	 * 'merged' with the current Scene on the same Strip; and update the 'mergedScenes' Collection
	 * to reflect the set of scenes for the current strip (which may be only the single current
	 * scene).
	 */
	private void createMergedScenes() {
		setMergedSceneString("(none)"); // default
		setSceneMerged(false);
		if (strip.getHasMultipleScenes()) {
			List<String> list = strip.getScenes(); // get existing List of scene numbers
			mergedScenes = sceneDAO.findByNumbersAndScript(list, script);
			if (mergedScenes.size() > 1) { // could be 1 if some scene numbers aren't in current script
				list.remove(scene.getNumber()); // remove the current scene number
				// turn it back into comma-delimited list
				setMergedSceneString(StringUtils.getStringFromList(list));
				setSceneMerged(true);
			}
		}
		else {
			mergedScenes = new ArrayList<>();
			mergedScenes.add(scene);
		}
	}

	/**
	 * Initialize the list of ScriptElementTypes to be displayed. Used for
	 * LSMOBILE application. The List includes all ScriptElementType's except
	 * for LOCATION.
	 */
	private void createScriptElementTypeList() {
		scriptElementTypeList = new ArrayList<>();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null && elements[i].size() > 0) {
				ScriptElementType t = ScriptElementType.values()[i];
				scriptElementTypeList.add(t);
			}
		}
	}

	/**
	 * Called to open Breakdown View positioned to a scene selected in Calendar or
	 * Stripboard View.
	 */
	public void openSheet(ActionEvent evt) {
		HeaderViewBean.setMenu(HeaderViewBean.SUB_MENU_BREAKDOWN_IX);
		try {
			String sceneNo = evt.getComponent().getAttributes().get("sceneNo").toString();
			SessionUtils.put(ATTR_SCENE_NUM, sceneNo);
		}
		catch (Exception e) {
			EventUtils.logError("openSheet failed Exception: ", e);
		}
	}

	/**
	 * For the desktop or mobile breakdown sheet page, scroll to the
	 * previous strip in the list.
	 * @param evt - ActionEvent, not used.
	 */
	public void actionPrevious(ActionEvent evt) {
		LOG.debug("");
		try {
			checkAutoSave();
			setupFromScene(listIndex-1);
			addJavaScript();	// center the selected scene in left-hand list
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * For the desktop or mobile breakdown sheet page, scroll to the next strip in the list.
	 * @param evt - ActionEvent, not used.
	 */
	public void actionNext(ActionEvent evt) {
		LOG.debug("");
		try {
			checkAutoSave();
			setupFromScene(listIndex+1);
			addJavaScript();	// center the selected scene in left-hand list
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Called when user clicks a link to jump to the stripboard view of the
	 * current scene. JSP statements have already setup the parameters for
	 * stripboard view; we just need to handle any autosave requirement before
	 * continuing.
	 *
	 * @return The navigation string for the Stripboard View page.
	 */
	public String actionJumpStripboardView() {
		checkAutoSave();
		return HeaderViewBean.SCHEDULE_MENU_STRIPBOARD_VIEW;
	}

	/**
	 * Called due to ice:rowSelector 'selectionListener' attribute, when a user clicks on a row in
	 * the list of strips. *NOTE* It is critical that the rowSelector specifies "immediate='false'",
	 * so that this method is called at the appropriate phase in the JSF life-cycle!
	 */
	public void listenRowSelect(SelectEvent evt) {
		int row = ListView.findSelectedRowParam(evt);
		LOG.debug("row=" + row);
		checkAutoSave();
		setupFromScene(row);
	}

	/**
	 * Called when user clicks the Autosave checkbox.
	 * @param evt The event passed from the framework.
	 */
	public void listenAutosave(ValueChangeEvent evt) {
		LOG.debug(evt);
		try {
			if (! evt.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
				// simpler to schedule event for later - after "setXxxx()" are called from framework
				evt.setPhaseId(PhaseId.INVOKE_APPLICATION);
				evt.queue();
				return;
			}
			String auto = (String)evt.getNewValue();
			if (auto != null) {
				autosave = auto.equalsIgnoreCase(AUTOSAVE_ON);
				UserPrefs.put(ATTR_BREAKDOWN_AUTOSAVE, autosave);
				if (autosave) {
					saveScene();
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Called when user changes almost any data item on the breakdown page, so
	 * that we can note that a change has occurred.
	 *
	 * @param evt The event passed from the framework.
	 */
	public void listenDataChange(ValueChangeEvent evt) {
		LOG.debug("");
		try {
			if (evt.getNewValue() != null) {
				Object newval = evt.getNewValue();
				boolean changing = false;
				if ((! (newval instanceof String)) || (evt.getOldValue() != null)) {
					changing = true;
				}
				else { // old value is null, new value is a String...
					String newstr = (String)newval;
					if (newstr.trim().length() != 0) { // non-empty String
						changing = true;
					}
					// if old value is null, and new value is empty string, do NOT set 'data changed'
				}
				if (changing) {
					LOG.debug("old=" + evt.getOldValue() + ", new=" + newval +
							", diff=" + ((new Date()).getTime() - refreshTime.getTime()) );
					if (! validateDataChangeTime()) {
						return;
					}
					dataChanged();
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * ActionListener method for the drop-down selection list within each
	 * element type list. (Not the auto-complete field.)
	 *
	 * @param evt The event created by the framework. The source component's
	 *            HTML id is parsed to determine which of the (20) drop-down
	 *            lists was clicked.
	 */
	public void listenElementSelected(ValueChangeEvent evt) {
		if (! evt.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			evt.setPhaseId(PhaseId.INVOKE_APPLICATION);
			evt.queue();
			return;
		}
		try {
			LOG.debug(evt);
			if (evt.getNewValue() != null &&
					evt.getSource() != null && evt.getSource() instanceof ComboBox) {
				ComboBox sit = (ComboBox)evt.getSource();
				String id = sit.getClientId(FacesContext.getCurrentInstance());
				LOG.debug(id);
				// We parse the HTML element id to find the column (=type) it applies to.
				// The id style is "bd:elad<column#>:<row#>:in"
				Matcher m = ID_PATTERN_AUTO_COMPLETE.matcher(id);
				if (m.matches()) {
					int iType = -1;
					try {
						iType = Integer.parseInt(m.group(1));
						// group(1) is the "column" number, i.e., the ordinal of
						// the related ScriptElementType.
					}
					catch (Exception e) {}
					if (iType >= 0 && iType < elementInput.length) { // Should be!
						String name = (String)evt.getNewValue();
						SelectItem found = findItem(elementList[iType], name);
						if (found != null) {
							ScriptElement se = scriptElementDAO.findById((Integer)found.getValue());
							if (se != null) {
								addElement(se);
								createElementList(se.getType());
							}
						}
						else {
							// newly entered item
							addElement(iType, name);
						}
					}
					elementInput[iType] = "";
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Called when the user changes the element Sort option drop-down (sort by ID or name).
	 * @param evt The event passed from the framework.
	 */
	public void listenSortStyle(ValueChangeEvent evt) {
		LOG.debug(""+evt.getPhaseId());
		try {
			String sort = (String)evt.getNewValue();
			if (sort != null) {
				sortById = sort.equalsIgnoreCase(SORT_STYLE_ID);
				//UserPrefs.put(ATTR_BREAKDOWN_, sortById);
				// rebuild element lists with proper sort keys, and sort them.
				createElementArrays(mergedScenes, elements);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Action method for the "Add" button, which opens the "Add scene" popup.
	 * This overrides the superclass method so we can send the enhanced JavaScript
	 * for positioning the left-hand list.
	 * @return null navigation string
	 */
	@Override
	public String actionOpenAddPopup() {
		LOG.debug("");
		doOpenAddPopup();
		addJavaScript();
		return null;
	}

	/**
	 * The Action method called when the user clicks the Save button on the "Add Scene" popup, to
	 * add (insert) a new scene. The user has the option of copying data from the preceding scene.
	 * The scene number is proposed by the software but may be changed by the user.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionSaveAdd() {
		try {
			checkAutoSave();	// save any pending changes first
			boolean b = doSaveAdd();
			if (b) {	// successful add
				createSceneList(); // update our scene list
				if (getPlaceAddScene().equals("BEFORE")) {
					int ix = listIndex;
					listIndex++; // force refresh
					setupFromScene(ix, false); // do setup, but don't save current Scene
				}
				else {
					setupFromScene(listIndex+1, false); // do setup, but don't save current Scene
				}
			}
			addJavaScript();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Mark the given scene as "omitted". It is not actually removed from the database, and its
	 * Strip (if any) remains unchanged. This is an Action method called by the Omit button on one
	 * of the various Scene List pages. Here we just put up the confirmation dialog.
	 */
	public String actionOmit() {
		PopupBean.getInstance().show(this, ACT_OMIT_SCENE, "Breakdown.Omit.");
		return null;
	}

	/**
	 * Mark an "omitted" scene as "normal". This is an Action method called by the Restore button on
	 * one of the various Scene List pages. Here we just put up the confirmation dialog.
	 */
	public String actionRestore() {
		PopupBean.getInstance().show(this, ACT_RESTORE_SCENE, "Breakdown.Restore.");
		return null;
	}

	/**
	 * Delete a scene. This is only allowed if the scene is already marked as "omitted". This is an
	 * Action method called by the Delete button on one of the various Scene List pages.  Here we
	 * just put up the confirmation dialog.
	 */
	@Override
	public String actionDelete() {
		PopupBean.getInstance().show(this, ACT_DELETE_SCENE, "Breakdown.Delete.");
		return null;
	}

	/**
	 * Delete a scene. This is only allowed if the scene is already marked as "omitted". This method
	 * is called from confirmOk(), as a result of the user clicking OK on the confirmation dialog.
	 */
	public String actionDeleteOk() {
		String res = null;
		try {
			res = super.actionDelete();
			createSceneList();
			int showix = listIndex;	// show whatever scene is now in the n'th (same) entry.
			listIndex = -1;		// force 'setup' to update selected scene info
			setupFromScene(showix, false); // false: don't save even though we're in edit mode
			checkAutoSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return res;
	}

	/**
	 * @return The currently selected Scene.
	 */
	@Override
	protected Scene findSelectedScene() {
		LOG.debug("");
		return scene;
	}

	/**
	 * @return The Scene prior to the currently selected Scene, or
	 * null if the current one is the first in the Script.
	 */
	@Override
	protected Scene findPriorScene() {
		Scene scn = null;
		int index = listIndex - 1;
		if (index >= 0 && index < sceneList.size()) {
			scn = sceneList.get(index);
		}
		LOG.debug("scene=" + (scn == null ? "null" : scn.getNumber()));
		return scn;
	}

	/**
	 * Insert a new Strip to match a newly-inserted Scene. Adds it as an unscheduled Strip to the
	 * current stripboard.
	 *
	 * @param newScene The Scene which the new Strip should specify.
	 */
	@Override
	protected Strip insertStrip(Scene newScene) {
		Strip strip1 = doInsertStrip(newScene);
		return strip1;
	}

	/**
	 * This method will refresh the arrays used to display the Script Element table.
	 * It is called after split or merge operations, or after Cancel.
	 */
	private void refreshScenesAndElementTable() {
		scene = sceneDAO.findById(scene.getId()); // refresh
		sceneList.set(listIndex, scene); // replace in list
		for (int i = 0; i < mergedScenes.size(); i++) {
			Scene s = mergedScenes.get(i);
			s = sceneDAO.findById(s.getId()); // freshen
			mergedScenes.set(i, s);
		}
		createElementArrays(mergedScenes, elements);
	}

	/**
	 * action method called when the user clicks the "Add Note" button.
	 */
	public String actionAddNote() {
		if (noteText == null) {
			return null;
		}
		try {
			noteText = noteText.trim();
			if (noteText.length() > 0) {
				Calendar cal = Calendar.getInstance();
				Note note = new Note();
				note.setStrip(strip);
				note.setText(noteText);
				note.setTime(cal.getTime());
				note.setUser(SessionUtils.getCurrentUser());
				strip.getNotes().add(0,note); // insert at top of list
				//strip.setNotes(strip.getNotes()); // will force refresh of List
				if (editMode) {
					if (addedNotes == null) {
						addedNotes = new ArrayList<>();
					}
					addedNotes.add(note);
				}
				else { // user has add-note privilege, but not edit; save note immediately
					NoteDAO.getInstance().save(note);
					//stripDAO.attachDirty(strip);
				}
				noteText = "";
				dataChanged();
				addJavaScript();
			}
		}
		catch (Exception e) {
			EventUtils.logError("actionAddNote failed: ", e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	Integer removeElementId;
	/** See {@link #removeElementId}. */
	public Integer getRemoveElementId() {
		return removeElementId;
	}
	/** See {@link #removeElementId}. */
	public void setRemoveElementId(Integer id) {
		removeElementId = id;
	}

	/**
	 * ActionListener for the delete icons on individual scriptElements associated
	 * with the current scene. (No longer used.)
	 */
	public void actionRemoveElement(ActionEvent evt) {
		//PopupBean.getInstance().show(this, ACT_REMOVE_ELEMENT, "Breakdown.RemoveElement.");
	}

	/**
	 * Method called to remove an individual script element from the current
	 * scene. Was called from the confirmation dialog Ok method; now called
	 * directly (bypassing confirmation).
	 *
	 */
	public String actionRemoveElementOk() {
		try {
			Integer id = getRemoveElementId();
			if (id != null) {
				ScriptElement scriptElement = scriptElementDAO.findById(id);
				if (scriptElement != null) {
					int rem = 0;
					removeFromElementArray(scriptElement);
					for (int i = 0; i < mergedScenes.size(); i++) {
						Scene s = mergedScenes.get(i);
						boolean b = s.getScriptElements().remove(scriptElement);
						if (b) {
							rem++;
						}
					}
					LOG.debug("removed=" + rem + " t=" + scriptElement.getType() + " se=" + scriptElement);
					// Mark DooD out of date (occurrence counts need updating even if unscheduled)
					project = projectDAO.refresh(project);
					ProductionDood.markProjectDirty(project, (scriptElement.getType()==ScriptElementType.CHARACTER));
					dataChanged();
					addJavaScript();
				}
				else {
					LOG.warn("removal element not found, id=" + id);
				}
			}
			else {
				LOG.warn("removal element null");
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Add a ScriptElement to the appropriate entry in the array of elements used to display the
	 * elements that are included in the scene.
	 *
	 * @param se The ScriptElement to add.
	 *
	 * @return True iff the appropriate element array was changed, that is, it did not
	 * already contain the given ScriptElement.
	 */
	private boolean addToElementArray(ScriptElement se) {
		boolean added = false;
		if (se.getType() != ScriptElementType.N_A) { // shouldn't happen, but just in case!
			int n = se.getType().ordinal();
			Item item = makeItem(se);
			if (! elements[n].contains(item)) {
				added = elements[n].add(makeItem(se));
			}
			Collections.sort(elements[n]);
		}
		return added;
	}

	/**
	 * Remove the specified ScriptElement from the appropriate column in the array of Items used to
	 * display the elements.
	 *
	 * @param se The ScriptElement to be removed.
	 */
	private void removeFromElementArray(ScriptElement se) {
		int n = se.getType().ordinal();
		List<Item> list = elements[n];
		for (Item it : list) {
			if (it.getId() != null && it.getId().equals(se.getId())) {
				list.remove(it);
				LOG.debug(it);
				break;
			}
		}
		createElementList(se.getType());
	}

	/**
	 * Called when user clicks the "Edit" button on the View page.
	 * @return null navigation string
	 */
	public String actionEdit() {
		setEditMode(true);
		checkStrip();		// double-check strip is non-null
		createElementLists(); // Lists of scriptElements for drop-downs
//		ListView.addJavascript("bdHidePlusAll();"); // gray out all '+' buttons
		addJavaScript();
		refreshTime = new Date();
		lastChangeTime = null;
		return null;
	}

	/**
	 * Action method for the Save button.  This saves any pending changes,
	 * and returns to View mode.  This is also used for the "Save & Exit"
	 * button (visible when auto-save is on).
	 * @return null navigation string
	 */
	public String actionSave() {
		try {
			saveScene();
			setEditMode(false);
			// Remove the blank element used for auto-complete
			for (int i = 0; i < elements.length; i++) {
				int n = elements[i].size();
				if (elements[i].get(n-1).getName() == null) { // should be!
					elements[i].remove(n-1);
				}
			}
			addJavaScript();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the Save-Now button. This saves any pending changes,
	 * but leaves the page in Edit mode.
	 * @return null navigation string
	 */
	public String actionSaveNow() {
		try {
			saveScene();
			addJavaScript();
			refreshTime = new Date();
			lastChangeTime = null;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * See if autosave is on, and if so, save the current data.
	 */
	private void checkAutoSave() {
		if (autosave && dataChanged && scene != null) {
			if (skipSave) {
				LOG.info("** Autosave suppressed due to invalid ValueChangeEvent(s) **");
			}
			else {
				saveScene();
			}
		}
		skipSave = false;
	}

	private void saveScene() {
		LOG.debug("#=" + scene.getNumber());
		try {
			if (scene.getPageNumber() < 1) {
				MsgUtils.addFacesMessage("Breakdown.ScriptPageInvalid", FacesMessage.SEVERITY_ERROR);
			}
			// save any changes
			scene = sceneDAO.saveScene(script, scene, strip, addedNotes, mergedScenes);
			sceneList.set(listIndex, scene); // replace in list
			for (int i=0; i < mergedScenes.size(); i++) {
				if (mergedScenes.get(i).equals(scene)) {
					mergedScenes.set(i, scene);	// replace with (possibly different) object
				}
			}
			scene.setSelected(true);
			//scene.setDirty(false);
			addedNotes = null;
			dataChanged = false;
			setupLocation();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Determine if enough time has passed since we rendered the last scene,
	 * and/or enough time in between change events, to allow a ValueChangeEvent
	 * to be accepted.
	 *
	 * @return True iff enough time has passed, and the event is therefore
	 *         considered valid.
	 */
	private boolean validateDataChangeTime() {
		Date now = new Date();
		long diff = now.getTime() - refreshTime.getTime();
		if (diff < MINIMUM_CHANGE_DELAY) {
			LOG.info(">>>>>>>>>>>>>>>>>>>>>>>> CHANGE INVALID (too soon) >>>>>>>>>>>>>>>>>> diff=" + diff);
			skipSave = true;
			return false;
		}
		if (lastChangeTime != null) {
			diff = now.getTime() - lastChangeTime.getTime();
			if (diff < MINIMUM_CHANGE_INTERVAL) {
				LOG.info(">>>>>>>>>>>>>>>>>>>>>>>> CHANGE INVALID (too close) >>>>>>>>>>>>> diff=" + diff);
				skipSave = true;
				return false;
			}
		}
		lastChangeTime = new Date();
		return true;
	}

	/**
	 * Called when something changes the data in the scene.
	 * Just sets the dataChanged flag for now.
	 */
	private void dataChanged() {
		dataChanged = true;
	}

	/**
	 * Action method called when user clicks the "Cancel" button on the Edit page.
	 * @return null navigation string
	 */
	public String actionCancel() {
		try {
			setEditMode(false);
			cancelChanges();
			// Because Cancel has immediate=true attribute, we need to clear the edit
			// fields of held information, so they will refresh from the backing bean(s)
			// when the user goes into Edit on a different Scene.
			BreakdownRequestBean bean = (BreakdownRequestBean)ServiceFinder.findBean("breakdownRequestBean");
			if (bean != null) {
				HtmlPanelGroup panel = bean.getEditPanel();
				if (panel != null) {
					ListView.clearEditFields(panel);
				}
			}
			checkStrip();		// double-check strip is non-null
			forceLazyInit();	// we were getting lazy init errors after Cancel & clicking Note tab
			addJavaScript();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * User did Cancel - need to restore our objects to same state as database.
	 */
	private void cancelChanges() {
		addedNotes = null;
		if (strip != null && strip.getId() != null) {
			strip = stripDAO.findById(strip.getId());
		}
		dataChanged = false;
		setupLocation();
		refreshScenesAndElementTable();
		scene.setSelected(true);
	}

	/**
	 * Create an array of column headings matching the ScriptElementType enumeration.
	 * @param headings
	 */
	private void createColumnHeadings(String[] headings) {
		int columnCount = 0;
		for (ScriptElementType type : ScriptElementType.values()) {
			if (type.equals(ScriptElementType.N_A)) { // ignore N/A and any following values
				break;
			}
			headings[columnCount] = type.toString();
			columnCount++;
		}
	}

	private void createElementLists() {
		for (ScriptElementType type : ScriptElementType.values()) {
			if (type.equals(ScriptElementType.N_A)) { // ignore N/A and any following values
				break;
			}
			createElementList(type);
		}
		// Add a blank item to each element type list for the auto-complete entry
		Item blankItem = new Item(null, null);
		blankItem.setKey("~~~~"); // so it sorts last in the list
		for (int i = 0; i < elements.length; i++) {
			elements[i].add(blankItem); // used by JSP to trigger display of "+" auto-complete entry
			elementInput[i] = ""; // erase any input value from prior scene
		}
	}

	private void createElementList(ScriptElementType type) {
		int iType = type.ordinal();
		List<ScriptElement> elemList = scriptElementDAO.findByTypeAndProject(type, project);
		elementList[iType].clear();
//		boolean itemAdded = false;
		if (elemList.size() > 0) {
			for (ScriptElement se : elemList) {
				boolean found = false;
				for (Item it : elements[iType]) { // throw out ones already in the scene
					if (it.getId() != null && it.getId().equals(se.getId())) {
						found = true;
						break;
					}
				}
				if (! found) {
					elementList[iType].add(new SelectItem(se.getId(),se.getName()));
//					itemAdded = true;
				}
			}
//			if (itemAdded) {
//				elementList[iType].add(0, new SelectItem(null,"Add element"));
//			}
		}
	}

	/**
	 * Get the list of script elements for a Collection of scenes, and fill an array of Lists with
	 * the Items containing the ids and names of the script elements, one array entry (List) for each
	 * ScriptElementType.
	 * <p>
	 * The array may include empty Lists, where no element of a particular type existed -- all the
	 * ScriptElementType values will be represented in the array of Lists.
	 *
	 * @param scenes The Collection of scenes from which the ScriptElements should be retrieved; the
	 *            set of ScriptElement's will be the union of all the script elements from all the
	 *            scenes.
	 * @param elems An (output) array of List<Item>; each entry in the array will be a List of
	 *            Items that describe the ScriptElements of a particular element type.
	 */
	private void createElementArrays(Collection<Scene> scenes, List<Item>[] elems) {
		for (int i = 0; i < elems.length; i++) {
			elems[i].clear();
		}
		Item it;
		for (Scene scn : scenes) {
			for (ScriptElement se : scn.getScriptElements()) {
				if (se.getType() != ScriptElementType.N_A) {
					int n = se.getType().ordinal();
					it = makeItem(se);
					if ( ! elems[n].contains(it)) {
						elems[n].add(it);
					}
				}
			}
		}
		if (editMode) {
			createElementLists(); // Lists of scriptElements for drop-downs
		}
		// Sort the lists
		for (int i = 0; i < elems.length; i++) {
			Collections.sort(elems[i]);
		}
	}

	private Item makeItem(ScriptElement se) {
		Item it;
		if (se.getElementId() != null) {
			it = new Item(se.getId(), se.getElementIdStr() + ' ' + se.getName());
			if (sortById) {
				String s = String.format("%09d", se.getElementId());
				it.setKey(s + se.getName());
			}
			else {
				it.setKey(se.getName());
			}
			//log.debug(it.getKey());
		}
		else {
			it = new Item(se.getId(), se.getName());
		}
		return it;
	}

	/**
	 * Action method for the "+" control adjacent to each of the auto-complete element
	 * entry fields. The {@link #typeIndex} is set by a f:setPropertyActionListener in
	 * the JSP. Note that JavaScript code on the page can also "click" the
	 * control as a result of the Enter key being pressed while focus is in one
	 * of the auto-complete input fields.
	 *
	 * @return null navigation string
	 */
	public String actionAddElement() {
		return addElement(true);
	}

	/**
	 * Action method for handling tabbing out of an auto-complete element entry
	 * field. The {@link #typeIndex} is set by a f:setPropertyActionListener in the
	 * JSP. Note that JavaScript code on the page "clicks" a hidden control to
	 * invoke this method.
	 *
	 * @return null navigation string
	 */
	public String actionTabElement() {
		return addElement(false);
	}

/**
	 * Handle adding an element from the current text in an auto-complete element
	 * entry fields. The 'typeIndex' is set by a f:setPropertyActionListener in
	 * the JSP.
	 *
	 * @return null navigation string
	 */
	private String addElement(boolean focusThis) {
		int n = getTypeIndex(); // Indicates which button was clicked
		// n will match the ordinal value of the ScriptElementType of the
		// elements in the column where the "+" was clicked.
		if (n >= 0 && n < elementInput.length) {
			String elemName = elementInput[n].trim().toUpperCase();

//			if ((! focusThis) && (elemName.length() == 0) && (n == (lastElementTypeIx-1))) {
//				// The tab-key event in javascript usually indicates the 'activeElement' is
//				// the tabbed-to field, and the javascript backs up to the prior column to
//				// report the event. However, once in a while, the activeElement is the tabbed-from
//				// field, and so the reported element index is 1 low.  This 'if' block is meant
//				// to catch that case, and fix it by using an index value 1 higher than reported,
//				// which is the same as 'lastElementTypeIx'.
//				n++;
//				elemName = elementInput[n].trim().toUpperCase();
//				//log.debug("fix up, elemName=" + elemName + "<<<<<<<<<<<<<<<<<<");
//			}
//			lastElementTypeIx = -1; // prevent using the above fix-up inadvertently

			String focus = "bdFocus(" + n + "," + (elements[n].size()-1) + ");" ;
			LOG.debug(focus);
			if (elemName.length() > 0) {
				addElement(n, elemName);
				// Call our macro to put the focus back on either this, or the next, auto-complete entry.
				if ((! focusThis) && (n < elementInput.length-1)) {
					n++;
				}
				ListView.addJavascript(focus);
			}
			else {
				//log.debug("empty element ************");
				if (focusThis) {
					ListView.addJavascript(focus);
				}
			}
		}
		return null;
	}

	private void addElement(int typeIx, String elemName) {
		LOG.debug(elemName);
		if (elemName.length() > 0) {
			if (elemName.length() > Constants.MAX_ELEMENT_NAME_LENGTH) {
				MsgUtils.addFacesMessage("Breakdown.ElementNameTooLong",
						FacesMessage.SEVERITY_ERROR, Constants.MAX_ELEMENT_NAME_LENGTH);
				return;
			}
			elemName = elemName.toUpperCase();
			elementInput[typeIx] = ""; // Clear the input field
			ScriptElementType type = ScriptElementType.values()[typeIx];
			ScriptElement se = null;
			if (NumberUtils.isDigits(elemName)) {
				int elemNum = NumberUtils.toInt(elemName, -1);
				se = scriptElementDAO.findById(elemNum);
			}
			else {
				se = scriptElementDAO.findByNameTypeProject(elemName, type, project);
			}
			if (se == null) {
				se = createScriptElement(elemName, type);
			}
			else {
				if (addElement(se)) {
					createElementList(type); // to remove newly added item from drop-down selection list
				}
				else { // duplicate entry, not added
					elementInput[typeIx] = elemName; // set input to trimmed & uppercase version, to be clear
				}
			}
//			autoElementList[n].clear(); // Erase the list of auto-complete choices
		}
	}

	/**
		 * Add the given ScriptElement, and its linked "children", to the current
		 * Scene.
		 *
		 * @param scriptElement The ScriptElement to add. Note that the elements are
		 *            added to the current Scene AND to any other scenes that are
		 *            merged with the current Scene.
		 */
		private boolean addElement(ScriptElement scriptElement) {
			LOG.debug("add existing: " + scriptElement.toString());
			scriptElement = scriptElementDAO.refresh(scriptElement);
			Set<ScriptElement> linkedElems = createFamily(scriptElement);
			boolean added = false;
			for (ScriptElement se : linkedElems) {
				for (int i = 0; i < mergedScenes.size(); i++) {
					Scene s = mergedScenes.get(i);
					s.getScriptElements().add(se);
				}
				added |= addToElementArray(se);
			}
			if (! added) {
	//			MsgUtils.addFacesMessage("Breakdown.ElementInScene", FacesMessage.SEVERITY_ERROR);
			}
			dataChanged();
			// Mark DooD out of date (occur counts change even if unscheduled)
			project = projectDAO.refresh(project);
			ProductionDood.markProjectDirty(project,(scriptElement.getType()==ScriptElementType.CHARACTER));
			return added;
		}

	private ScriptElement createScriptElement(String elementName, ScriptElementType type) {
		ScriptElement se = null;
		if (elementName != null && elementName.trim().length() > 0 ) {
			project = projectDAO.refresh(project);
			se = new ScriptElement();
			se.setName(elementName.toUpperCase().trim());
			se.setType(type);
			if (scriptElementDAO.existsNameTypeProject(se.getName(), se.getType(), project)) {
				return null;
			}
			LOG.debug("created=" + se.getName());
			ProductionDood.markProjectDirty(project,(se.getType()==ScriptElementType.CHARACTER));
			ScriptUtils.setDefaultValues(se, project);
			scriptElementDAO.update(se, null);
			for (Scene s : mergedScenes) {
				s.getScriptElements().add(se);
				//s.setDirty(true);
			}
			addToElementArray(se);
			dataChanged();
		}
		else {
			se = null;
		}
		return se;
	}

	/**
	 * @param items
	 * @param label
	 * @return
	 */
	private SelectItem findItem(List<SelectItem> items, String label) {
		SelectItem found = null;
		int val = NumberUtils.toInt(label, -1);
		for (SelectItem item : items) {
			if (item.getLabel().equalsIgnoreCase(label)) {
				found = item;
				break;
			}
			else if (val > 0 && (Integer)item.getValue() == val) {
				found = item;
				break;
			}
		}
		return found;
	}

	/**
	 * Called to validate 'strip' field as non-null.  This method is called from
	 * various points where we do NOT expect 'strip' to be null.  There was a long-standing
	 * bug in Merge, activated when a user double-clicked the Merge button, that
	 * left 'strip' null, and resulted in subsequent errors.  We now check for the
	 * null 'strip' value in key places, in case a similar bug is still hiding! Rev 3133.
	 */
	private void checkStrip() {
		if (strip == null) {
			EventUtils.logError("Missing (null) strip detected - creating replacement.");
			strip = insertStrip(scene);
		}
	}

	/**
	 * Append the 'standard' JavaScript requests for the Breakdown page.  This
	 * includes the usual resize() call, plus a call to keep the left-hand list
	 * scrolled to the appropriate location.
	 */
	private void addJavaScript() {
//		ListView.addClientResize();
		if (listIndex >= 0) {
			ListView.addJavascript("showStrip(" + listIndex + ");");
		}
	}

	/**
	 * Should the "Next" button be enabled?
	 * @return True iff the Next button should be enabled.
	 */
	public boolean getNextEnabled() {
		return (sceneList != null && listIndex < sceneList.size()-1);
	}

	public int getListIndex() {
		return listIndex;
	}
	public void setListIndex(int listIndex) {
		this.listIndex = listIndex;
	}

	/** See {@link #sceneList}. */
	public List<Scene> getSceneList() {
		return sceneList;
	}

	/** See {@link #sceneList}. */
	public void setSceneList(List<Scene> sceneList) {
		this.sceneList = sceneList;
	}

	public Strip getStrip() {
		return strip;
	}

	public void setStrip(Strip strip) {
		this.strip = strip;
	}

	/**
	 * The super-class version of this does nothing as it does not use
	 * the map;  our class does, so here we ensure that it exists and is
	 * up to date. See {@link SceneListBase#breakdownMap}.
	 */
	@Override
	public Map<String, Integer> getBreakdownMap() {
		return checkBreakdownMap();
	}

	@Override
	public Scene getScene() {
		return scene;
	}

	public void setScene(Scene scene) {
		this.scene = scene;
	}

	public String getNoteText() {
		return noteText;
	}

	public void setNoteText(String noteText) {
		this.noteText = noteText;
	}

	/** See {@link #sortStyleValue}. */
	public String getSortStyleValue() {
		return sortStyleValue;
	}

	/** See {@link #sortStyleValue}. */
	public void setSortStyleValue(String sortStyleValue) {
		this.sortStyleValue = sortStyleValue;
	}

	public List<ScriptElementType> getScriptElementTypeList() {
		return scriptElementTypeList;
	}
	public void setScriptElementTypeList(List<ScriptElementType> typeList) {
		scriptElementTypeList = typeList;
	}

	/** See {@link #dataChanged}. */
	public boolean getDataChanged() {
		return dataChanged;
	}
	/** See {@link #dataChanged}. */
	public void setDataChanged(boolean dataChanged) {
		this.dataChanged = dataChanged;
	}

	/** See {@link #autosave}. */
	public boolean getAutosave() {
		return autosave;
	}
	/** See {@link #autosave}. */
	public void setAutosave(boolean autosave) {
		this.autosave = autosave;
	}

	/** See {@link #autosaveValue}. */
	public String getAutosaveValue() {
		return autosaveValue;
	}
	/** See {@link #autosaveValue}. */
	public void setAutosaveValue(String autosaveValue) {
		this.autosaveValue = autosaveValue;
	}

	/** See {@link #typeIndex}. */
	public int getTypeIndex() {
		return typeIndex;
	}
	/** See {@link #typeIndex}. */
	public void setTypeIndex(int typeIndex) {
		this.typeIndex = typeIndex;
	}

	/** See {@link #elementInput}. */
	public String[] getElementInput() {
		return elementInput;
	}
	/** See {@link #elementInput}. */
	public void setElementInput(String[] elementInput) {
		this.elementInput = elementInput;
	}

	/** See {@link #autoElementList}. */
//	public List<SelectItem>[] getAutoElementList() {
//		return autoElementList;
//	}
//	/** See {@link #autoElementList}. */
//	public void setAutoElementList(List<SelectItem>[] elementList) {
//		autoElementList = elementList;
//	}

	/** See {@link #elementList}. */
	public List<SelectItem>[] getElementList() {
		return elementList;
	}
	/** See {@link #elementList}. */
	public void setElementList(List<SelectItem>[] elementList) {
		this.elementList = elementList;
	}

	/** See {@link #newElementName}. */
	public String getNewElementName() {
		return newElementName;
	}
	/** See {@link #newElementName}. */
	public void setNewElementName(String newElementName) {
		this.newElementName = newElementName;
	}

	/**
	 * Returns the RealWorldElement location that has been
	 * linked to this strip's script element location.
	 */
	public RealWorldElement getRealLocation() {
		return realLocation;
	}
	public void setRealLocation(RealWorldElement realLocation) {
		this.realLocation = realLocation;
	}

	/**
	 * Returns the length of the strip, in pages, formatted in
	 * the standard LightSPEED style.
	 */
	public String getPageLength() {
		if (strip == null) {
			return "";
		}
		int len = (strip.getLength()==null ? 0 : strip.getLength().intValue());
		return Scene.formatPageLength(len);
	}

	/** See {@link #shootDayNumber}. */
	public String getShootDayNumber() {
		return shootDayNumber;
	}
	/** See {@link #shootDayNumber}. */
	public void setShootDayNumber(String shootDayNumber) {
		this.shootDayNumber = shootDayNumber;
	}

	/** See {@link #shootDate}. */
	public Date getShootDate() {
		return shootDate;
	}
	/** See {@link #shootDate}. */
	public void setShootDate(Date shootDate) {
		this.shootDate = shootDate;
	}

	public List<ScriptElement> getSceneScriptElements() {
		return getScene().getScriptElementList();
	}

	// * * * Merge popup dialog methods * * *

	/**
	 * Open the Merge pop-up dialog box.
	 * @return null navigation string
	 */
	public String actionOpenMerge() {
		checkStrip();		// double-check strip is non-null
		showMerge = true;
		mergeChoices = null; // force refresh of list
		mergeValue = 0;
//		ListView.addClientResize();
		return null;
	}

	/**
	 * Close the Merge pop-up without doing anything.
	 * @return null navigation string
	 */
	public String actionCloseMerge() {
		showMerge = false;
		mergeChoices = null; // free resources & force refresh of list next time
//		ListView.addClientResize();
		return null;
	}

	/**
	 * The Action method of the Merge button on the Merge pop-up dialog
	 * box.  The 'mergeValue' -- the value of the selected item in the
	 * pop-up's scene list -- has already been set by the framework.
	 * The value is the id of the scene to be merged. (The createChoices
	 * method builds the SelectItems that way.)
	 * <p>
	 * Note that if the selected scene is part of a merged strip, we will
	 * merge all of those scenes with the current strip's scene or scenes.
	 */
	public String actionMerge() {
		LOG.debug("val=" + mergeValue);
		if (!showMerge) { // user probably double-clicked
			return null;
		}
		if (mergeValue == null) { // no entry selected
			MsgUtils.addFacesMessage("Breakdown.SelectMerge", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		try {
			showMerge = false;
			Scene merge = sceneDAO.findById(mergeValue);

			Strip oldStrip = stripDAO.findBySceneAndStripboard(merge.getNumber(),
					SessionUtils.getCurrentProject().getStripboard());
			if (oldStrip == null) {
				// only happens if concurrent change to stripboard, or bug.
				insertStrip(merge);
				MsgUtils.addFacesMessage("Breakdown.MergeFailed", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			stripDAO.mergeStrips(oldStrip, strip, project);
			strip = stripDAO.refresh(strip);

			setBreakdownMap(null);	// no longer valid.
			createMergedScenes(); // update "merged scenes" display string
			refreshScenesAndElementTable();	// update element table to include all merged scenes
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		mergeChoices = null; // free resources & force refresh of list next time
//		ListView.addClientResize();
		return null;
	}

	/**
	 * User clicked on an entry in the "scene choice" list.
	 * (probably don't need actionListener.)
	 */
/*	public void actionSceneChoiceSelect(ValueChangeEvent evt) {
		mergeIndex = (Integer)evt.getNewValue();
		log.debug("ix=" + mergeIndex + ", list=" + sceneList.size());
	}
*/
	/** See {@link #mergeChoices}. */
	public List<SelectItem> getMergeChoices() {
		if (mergeChoices == null) {
			mergeChoices = createMergeChoices();
		}
		return mergeChoices;
	}
	/** See {@link #mergeChoices}. */
	public void setMergeChoices(List<SelectItem> sceneChoices) {
		mergeChoices = sceneChoices;
	}

	/** See {@link #mergeValue}. */
	public Integer getMergeValue() {
		return mergeValue;
	}
	/** See {@link #mergeValue}. */
	public void setMergeValue(Integer mergeIndex) {
		mergeValue = mergeIndex;
	}

	/** See {@link #showMerge}. */
	public boolean getShowMerge() {
		return showMerge;
	}
	/** See {@link #showMerge}. */
	public void setShowMerge(boolean showMerge) {
		this.showMerge = showMerge;
	}

	/** See {@link #sceneMerged}.  */
	public boolean getSceneMerged() {
		return sceneMerged;
	}
	/** See {@link #sceneMerged}. */
	public void setSceneMerged(boolean sceneMerged) {
		this.sceneMerged = sceneMerged;
	}

	// * * * Split popup dialog methods * * *

	/** See {@link #mergedSceneString}. */
	public String getMergedSceneString() {
		return mergedSceneString;
	}
	/** See {@link #mergedSceneString}. */
	public void setMergedSceneString(String mergedScenes) {
		mergedSceneString = mergedScenes;
	}

	public String actionOpenSplit() {
		checkStrip();		// double-check strip is non-null
		if (strip.getHasMultipleScenes()) {
			showSplit = true;
			splitList = null; // ensure splitList is refreshed
		}
		else {
			String message = "This scene does not have any merged scenes to split.";
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_WARN, message);
		}
		return null;
	}

	public String actionCloseSplit() {
		showSplit = false;
		return null;
	}

	/**
	 * The Action method of the Split button on the Split pop-up dialog.
	 * We will remove the current Scene from the set of merged scenes to which
	 * it currently belongs.
	 * @return null navigation string
	 */
	public String actionSplit() {
		if (! showSplit) { // user may have double-clicked
			return null;
		}
		showSplit = false;
		try {
			if (editMode) {
				scene = sceneDAO.merge(scene);
				strip = stripDAO.merge(strip);
			}
			else {
				scene = sceneDAO.refresh(scene);	// refresh our Scene object
				strip = stripDAO.refresh(strip);
			}
			splitScene(scene, strip);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		addJavaScript();
		return null;
	}

	@Override
	protected Strip splitScene(Scene split, Strip splitStrip) {
		strip = super.splitScene(split, splitStrip); // updates the Strip and stripboard
		createMergedScenes(); // update "merged scenes" display string and collection
		refreshScenesAndElementTable();	// update element table to include current merged scenes
		return strip;
	}

	/** See {@link #splitList}. */
	public List<Scene> getSplitList() {
		if (splitList == null) {
			splitList = createSplitList();
		}
		return splitList;
	}
	/** See {@link #splitList}. */
	public void setSplitList(List<Scene> splitList) {
		this.splitList = splitList;
	}

	/** See {@link #showSplit}. */
	public boolean getShowSplit() {
		return showSplit;
	}
	/** See {@link #showSplit}. */
	public void setShowSplit(boolean showSplit) {
		this.showSplit = showSplit;
	}

	/** Create a list of SelectItems for the selection list in
	 * the "Merge" dialog box. This should be all Scenes in the
	 * current script that are not omitted and have not already
	 * been merged with the current Scene. */
	private List<SelectItem> createMergeChoices() {
		List<SelectItem> dir = new ArrayList<>();
		List<String> merged = strip.getScenes();
		for (Scene s : sceneList) {
			if ( ! s.getOmitted() &&
					! merged.contains(s.getNumber())) {
				dir.add(new SelectItem(s.getId(), makeSelectLabel(s)));
			}
		}
		LOG.debug("choices=" + dir.size());
		return dir;
	}

	private List<Scene> createSplitList() {
		return createExcludeList(sceneDAO.findByNumbersAndScript(strip.getScenes(), script));
	}

	private List<Scene> createExcludeList(Collection<Scene> scenes) {
		LOG.debug("");
		List<Scene> exList = new ArrayList<>();
		for (Scene s : scenes) {
			if ( ! s.getSequence().equals(scene.getSequence())) {
				exList.add(s);
			}
		}
		LOG.debug("list=" + exList.size());
		return exList;
	}

	private static String makeSelectLabel(Scene scn) {
		String str = scn.getNumber() + ": " + scn.getHeading();
		return str;
	}

	public String[] getColumns() {
		return columns;
	}

	public List<Item>[] getElements() {
		return elements;
	}

	public LocationSelector getLocationSelector() {
		return locationSelector;
	}
	public void setLocationSelector(LocationSelector locationSelector) {
		this.locationSelector = locationSelector;
	}

	public boolean getEditMode() {
		return editMode;
	}
	public void setEditMode(boolean b) {
		editMode = b;
//		if (b) {
//			SessionUtils.put(ATTR_BK_EDIT, Boolean.TRUE);
//		}
//		else {
//			SessionUtils.put(ATTR_BK_EDIT, null);
//		}
	}

	public boolean getViewMode() {
		return ! editMode;
	}
	public void setViewMode(boolean b) {
		setEditMode(!b);
	}

	// * * * TABs * * *

	/** See {@link #selectedTab}. */
	public int getSelectedTab() {
		return selectedTab;
	}
	/** See {@link #selectedTab}. */
	public void setSelectedTab(int n) {
		LOG.debug(n);
		if (selectedTab != n) {
			addJavaScript();
		}
		selectedTab = n;
		SessionUtils.put(ATTR_BREAKDOWN_MINI_TAB, selectedTab);
		setupTabs();
	}

	private void setupTabs() {
	}

	// * * * Methods for LocationHolder interface * * *

	@Override
	public void locationUpdated() {
		LOG.debug("");
		if (validateDataChangeTime()) {
			dataChanged();
		}
		else {
			LOG.debug("(location change ignored)");
		}
	}

	@Override
	public void saveChanges() {
		LOG.debug("");
	}

	@Override
	public void updateScene(Scene scn) {
		LOG.debug("");
		setScene(scn);
		ProductionDood.markProjectDirty(project, false);
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		addJavaScript();
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		try {
			switch(action) {
				case ACT_DELETE_SCENE:
					res = actionDeleteOk();
					break;
				case ACT_OMIT_SCENE:
					res = actionOmitOk();
					createShootingDayInfo();
					break;
				case ACT_RESTORE_SCENE:
					res = actionRestoreOk();
					createShootingDayInfo();
					break;
				case ACT_REMOVE_ELEMENT:
					res = actionRemoveElementOk();
					break;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return res;
	}

	/**
	 * Called when our bean goes "out of scope", e.g., the user navigates
	 * away from the Breakdown page.
	 */
	@PreDestroy
	public void dispose() {
		LOG.debug("");
		checkAutoSave();
	}

}
