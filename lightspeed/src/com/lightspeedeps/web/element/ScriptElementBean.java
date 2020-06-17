package com.lightspeedeps.web.element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.RealLinkDAO;
import com.lightspeedeps.dao.RealWorldElementDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dood.ElementDood;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Image;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.RealLink;
import com.lightspeedeps.model.RealWorldElement;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.object.DatePair;
import com.lightspeedeps.object.Item;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.script.ScriptUtils;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.script.BreakdownBean;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.view.ListImageView;

/**
 * Backing bean for the Script Element page.  Includes both View and Edit
 * code.  This class handles most of the features and functions associated
 * with the "Details" and "played by" mini-tabs.  The "Images" mini-tab is
 * supported mostly by the {@link ListImageView} superclass.  The left-hand list of
 * ScriptElements is mostly supported by the {@link com.lightspeedeps.web.view.ListView ListView} superclass.
 */
@ManagedBean
@ViewScoped
public final class ScriptElementBean extends ListImageView implements PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 5961376084993169402L;

	private static final Log LOG = LogFactory.getLog(ScriptElementBean.class);

	private static final int TAB_DETAIL = 0;
	//private static final int TAB_IMAGES = 1;
	//private static final int TAB_PLAYED_BY = 2;

	private static final ScriptElementType INITIAL_CATEGORY = ScriptElementType.CHARACTER;

	private static final int ACT_REMOVE_SCENE = 10;
	private static final int ACT_REMOVE_REAL_LINK = 11;

	/** An unlikely date, to use as the "None" entry in the drop-down list. */
	private static final Date NULL_DATE = Constants.JAN_1_2000;

	/** Include elements that are "orphans" in list -- ones not referenced by any
	 * scene in any script in the current project. */
	private boolean showOrphans = true;

	/** The currently selected element category, or "All" */
	private String category = INITIAL_CATEGORY.name();

	/** The list of elements currently displayed. */
	private List<ScriptElement> scriptElementList;

	/** The drop-down selection list for element type (or "All"). */
	private List<SelectItem> scriptElementTypeDL;

	private transient Project project;

	/** False if this project does not have a default script; used by
	 * JSP to hide or disable some controls. */
	private boolean hasScript = false;

	/** The currently viewed Script Element. */
	private ScriptElement scriptElement;

	/** The element name when it was set up for display.  We use this when the
	 * project is saved, to see if it's changed and needs to be check for
	 * duplicating another element's name. */
	private String originalName;

	/** The element's "elementId", commonly referred to as cast Id, when
	 * it was set up for display.  In Edit mode, we compare this upon saving
	 * to see if it changed, and if so, to validate it. */
	private String originalCastId;

	/** The SelectItem list of contacts for the "responsible party"
	 * drop-down.  Includes a "select contact" entry at the beginning.  */
	private List<SelectItem> contactDL;

	/** If true, display the "add real-world element link" pop-up. */
	private boolean showRWElemLink = false;

	/** If true, display the "add script element link" pop-up. */
	private boolean showChild = false;

	/** If true, display the "retroactively add linked elements" pop-up. */
	private boolean showRetro = false;

	/** A list of Scenes in the current Script which the current ScriptElement
	 * is linked to. */
	private List<Scene> linkedScenes;

	/** If true, display the "Add Scenes" pop-up dialog box. */
	private boolean showAddScene;

	/** The list of Scenes which the user may select to link to the current ScriptElement -- that is,
	 * those scenes which are in the current Script, but do not yet include the current ScriptElement. */
	private transient List<Scene> selectScenes = null;

	/** The cast id of the item to be viewed; set by callsheet.jsp or other
	 * JSPs wishing to "jump" to a script element view based on cast id. */
	private Integer castId;

	/** The database id of the scene selected for removal by the user
	 * clicking the "X" next to it in the "appears in scenes" list. */
	private Integer removeSceneId;

	/** The database id of the realLink selected for removal by the user
	 * clicking the "X" next to it in the "Linked To/played by" list. */
	private Integer removeLinkId;

	/** The name field of an item being removed; this is only used in situations where the
	 * user has just added a link, which will not yet have an id (until it is saved).
	 */
	private String removeName;

	/** The id of the ScriptElement's responsible party. Held separately to simplify management
	 * of the "select contact" drop-down list. */
	private Integer contactId = -1;

	/** List of RealWorldElements that could be linked as "played by" elements
	 * to this ScriptElement.  This list is typically displayed in the
	 * "Add Link to Real World Element" pop-up dialog box. */
	private List<RealWorldElement> linkableElements = null;

	/** List of id`s of RealWorldElement that were deleted from the list of linked
	 * ('played by') elements during the current Edit session. */
	private final List<Integer> removedElemLinks = new ArrayList<>();

	/** List of ScriptElement`s that could be linked as child elements (for use in
	 * breakdown) to this ScriptElement. */
	private List<ScriptElement> eligibleChildren = null;

	/** The current type of ScriptElement`s to show in the 'add child' popup. */
	private ScriptElementType childType = ScriptElementType.PROP;

	/** The current type of ScriptElement`s to show in the 'add child' popup,
	 * as a String -- referenced by drop-down selector in JSP. */
	private String childTypeName = ScriptElementType.PROP.name();

	/** List of id`s of ScriptElement that were deleted from the list of linked
	 *  elements during the current Edit session. */
	private final List<Integer> removedChildren = new ArrayList<>();

	/** List of Item`s reflecting the linked children of this element; used for the
	 * "apply retroactively" facility. */
	private List<Item> childItems;

	/** A List of selections of DatePairs from the hold-dates ranges of
	 * the element, available for selection from the "use for drop"
	 * drop-down list. */
	private List<SelectItem> dropDateDL;

//	private QuickContactBean quickContact;
	private boolean scenesChanged = false;

	/** If true, a message is generated within the current pop-up dialog box. */
	//private boolean msgAdded = false;

	private Integer addId;

	private transient ScriptElementDAO scriptElementDAO;
	private transient RealLinkDAO realLinkDAO;
	private transient SceneDAO sceneDAO;


	/* Constructor */
	public ScriptElementBean() {
		super(ScriptElement.SORTKEY_ID, "ScriptElement.");
		LOG.debug("");
		try {
			hasScript = (getProject().getScript() != null);

			scriptElementTypeDL = new ArrayList<>( EnumList.getScriptElementTypeList() );
			scriptElementTypeDL.add(0, Constants.GENERIC_ALL_ITEM);

			String cat = SessionUtils.getString(Constants.ATTR_SE_CATEGORY, Constants.CATEGORY_ALL);
			category = cat;
			changeCategory(getCategory(), false);
			checkTab(); // restore last selected mini-tab
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}

		try {
			Integer id = SessionUtils.getInteger(Constants.ATTR_SE_ELEMENT_ID);
			setupSelectedItem(id);
			scrollToRow();
			restoreSortOrder();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Determine which element we are supposed to view. If the id given is null or invalid, we try
	 * to display the "default" element.
	 *
	 * @param id
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		LOG.debug("id=" + id);
		if (scriptElement != null) {
			scriptElement.setSelected(false);
			updateItemInList(scriptElement);
		}
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_SE_ELEMENT_ID, null);
			scriptElement = null;
			editMode = false;
			newEntry = false;
			setSelectedRow(-1);
		}
		else if ( ! id.equals(NEW_ID)) {
			scriptElement = getScriptElementDAO().findById(id);
			if (scriptElement == null) {
				SessionUtils.put(Constants.ATTR_SE_ELEMENT_ID, null);
				showItemMissing();
				return;
			}
			if (! scriptElement.getProject().equals(getProject())) {
				SessionUtils.put(Constants.ATTR_SE_ELEMENT_ID, null);
				scriptElement = null;
				refreshList(); // this will do a new setupSelectedItem() call
				return;
//				id = findDefaultId();
//				if (id != null) {
//					scriptElement = getScriptElementDAO().findById(id);
//				}
			}
			if ( scriptElement != null && ! getCategory().equals(Constants.CATEGORY_ALL) &&
					! getCategory().equals(scriptElement.getType().name())) {
				changeCategory(scriptElement.getType().name(), false);
			}
			SessionUtils.put(Constants.ATTR_SE_ELEMENT_ID, id);
		}
		else { /** User clicked "Add" button ... get here via {@link ListView#actionNew()} */
			LOG.debug("new SE");
			SessionUtils.put(Constants.ATTR_SE_ELEMENT_ID, null); // erase "new" flag
			ScriptElementType newType = ScriptElementType.CHARACTER;
			if (scriptElement != null) {
				newType = scriptElement.getType();
			}
			else if (! category.equals(Constants.CATEGORY_ALL)) {
				newType = ScriptElementType.valueOf(category);
			}
			scriptElement = new ScriptElement();
			scriptElement.setType(newType);
			ScriptUtils.setDefaultValues(scriptElement, getProject());
			if (scriptElement.getType() == ScriptElementType.CHARACTER) {
				scriptElement.setElementIds(
						getScriptElementDAO().findNextElementId(getProject(), ScriptElementType.CHARACTER));
			}
		}
		removedElemLinks.clear();
		removedChildren.clear();
		resetImages();
		setRemoveLinkId(null);
		setLinkableElements(null);
		setEligibleChildren(null);
		setRemoveSceneId(null);
		setSelectScenes(null);
		setLinkedScenes(null);
		dropDateDL = null;
		if (scriptElement != null) {
			originalName = scriptElement.getName();
			originalCastId = scriptElement.getElementIdStr();
			scriptElement.setSelected(true);
			getStateMap().clear();
			selectRowState(scriptElement);
			if (scriptElement.getDropToUse() == null) {
				scriptElement.setDropToUse(NULL_DATE);
			}
			scriptElement.setOkToDelete(true);
			if (scriptElement.getType() == ScriptElementType.LOCATION
					&& scriptElement.getId() != null) { // skip check if new element (just did Add) (rev 2680)
				if (scriptElement.getElementDood() != null &&
						scriptElement.getElementDood().getScriptOccurs() > 0) {
					scriptElement.setOkToDelete(false);
				}
				else {
					scriptElement.setOkToDelete(
							getScriptElementDAO().isLocationOrphanByProject(getProject(), scriptElement));
				}
			}
			updateItemInList(scriptElement);
			setupContact();
			forceLazyInit();
		}
	}

	private void setupContact() {
		if (scriptElement.getContact() == null) {
			setContactId(-1);
		}
		else {
			setContactId(scriptElement.getContact().getId());
			@SuppressWarnings("unused")
			String s = scriptElement.getContact().getUser().getDisplayName(); // force init
		}
	}

	@SuppressWarnings("unchecked")
	private Integer findDefaultId() {
		Integer id = null;
		List<ScriptElement> list = getItemList();
		if (list.size() > 0) {
			id = list.get(0).getId();
		}
		return id;
	}

	/**
	 * This method references a number of fields in the current ScriptElement
	 * and related objects to ensure they are initialized (via Hibernate) while
	 * the scriptElement is still in scope.
	 */
	private void forceLazyInit() {
		@SuppressWarnings("unused")
		int i = getElement().getImages().size();
//		i = getElement().getRealLinkList().size();
		for (RealLink rl : getElement().getRealLinkList()) { // fix for LIE on Script Element | Played by mini-tab
			rl.getRealWorldElement().getName();
		}

		@SuppressWarnings("unused")
		String str;
		if (scriptElement.getContact() != null) {
			str = scriptElement.getContact().getUser().getDisplayName();
		}
		getLinkedScenes(); // force init of linked-scenes list
		if (scriptElement.getType() == ScriptElementType.LOCATION) {
			project = SessionUtils.getCurrentProject(); // refresh
			if (getProject().getScript() != null) {
				for (Scene s : getProject().getScript().getScenes()) {
					str = s.getSetName();
				}
			}
		}
		if (scriptElement.getChildElements() != null) {
			if (editMode) { // might need to find all descendants for add-scene
				BreakdownBean.createFamily(scriptElement);
			}
			else { // for View mode, only immediate children will be accessed
				for (ScriptElement se : scriptElement.getChildElements()) {
					se.getName();
				}
			}
		}
		i = scriptElement.getScenes().size();
		i = scriptElement.getScenesForLocation().size();
	}

	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((ScriptElement)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Action method for links from the "linked script elements" list to a
	 * child/linked element.
	 * @return null navigation string
	 */
	public String actionJumpToChild() {
		Integer id = removeLinkId;
		setSelectedRow(-1); // without this the prior item still shows selected!
		setupSelectedItem(id);
		scrollToRow();
		return null;
	}

	/**
	 * Action method to jump to script element view using a cast id value,
	 * from a different page.
	 * @return
	 */
/*	public String viewByCastId() {
		try {
			if (castId != null) {
				scriptElement = getScriptElementDAO().findByElementidAndProject(castId, getProject());
				if (scriptElement != null) {
					SessionUtils.put(Constants.ATTR_SE_ELEMENT_ID, scriptElement.getId());
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return HeaderViewBean.ELEMENTS_MENU_SCRIPT_ELEMENTS;
	}
*/

	/**
	 * Action method for the main Edit button. To avoid some LazyInitializationException
	 * errors, we refresh the current ScriptElement, and call {@link #forceLazyInit()}.
	 * @see com.lightspeedeps.web.view.ListView#actionEdit()
	 */
	@Override
	public String actionEdit() {
		LOG.debug("");
		super.actionEdit();
		scriptElement = getScriptElementDAO().refresh(scriptElement);
		// in edit mode we do extra steps to prevent LazyInitializationException`s
		forceLazyInit();
		return null;
	}

	/**
	 *  Save ScriptElement - action method when user clicks "Save" on edit page.
	 */
	@Override
	public String actionSave() {
		LOG.debug("");
		try {
			if (getElement().getName() == null || getElement().getName().trim().length() == 0) {
				MsgUtils.addFacesMessage("RealElement.BlankName", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(TAB_DETAIL);
				return null;
			}
			project = SessionUtils.getCurrentProject(); // refresh
			scriptElement.setName(scriptElement.getName().trim().toUpperCase());
			if (( ! getElement().getName().equalsIgnoreCase(originalName) ) &&
					getScriptElementDAO().existsNameTypeProject(getElement().getName(), getElement().getType(), getProject())) {
				MsgUtils.addFacesMessage("RealElement.DuplicateName", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(TAB_DETAIL);
				return null;
			}
//			if (scriptElement.getType() != ScriptElementType.CHARACTER &&
//					scriptElement.getType() != ScriptElementType.EXTRA) {
//				scriptElement.setElementIds(null);
//			}
			if (StringUtils.compare(getElement().getElementIdStr(), originalCastId) != 0) {
				if (getElement().getElementId() != null) {
					if (! ScriptElement.isValidIdString(getElement().getElementIdStr())) {
						MsgUtils.addFacesMessage("ScriptElement.InvalidCastId", FacesMessage.SEVERITY_ERROR);
						setSelectedTab(TAB_DETAIL);
						return null;
					}
					if (getElement().getElementIdStr().equalsIgnoreCase(originalCastId)) {
						// don't do duplicate check in this case -- user is just changing case of id string,
						// and duplicate check will fail (same "elementId" is generated for both)
					}
					else if (getScriptElementDAO().existsElementIdProjectType(
							getElement().getElementId(), getProject(), getElement().getType())) {
						MsgUtils.addFacesMessage("ScriptElement.DuplicateCastId", FacesMessage.SEVERITY_ERROR,
								getElement().getType().toString());
						setSelectedTab(TAB_DETAIL);
						return null;
					}
				}
			}

			originalName = getElement().getName();
			originalCastId = getElement().getElementIdStr();
			if (contactId == null || contactId == -1 ||
					(scriptElement.getContact() != null && scriptElement.getContact().getId() == null)) {
				scriptElement.setContact(null);	// clear out placeholder
			}
			if (scriptElement.getDropToUse() != null && ! scriptElement.getDropToUse().after(NULL_DATE)) {
				scriptElement.setDropToUse(null);
			}
			setLinkableElements(null);
			for (RealLink rlink : getElement().getRealLinks()) {
				if (rlink.getId() != null && rlink.getId() < 0) { // temporary value
					rlink.setId(null);
					ProductionDood.markDirty(getProject(), rlink.getRealWorldElement()); // DooD values out of date
				}
			}
			commitImages();
			if (!newEntry) {
				updateRealLinks();
				boolean refresh = false;
				if (scenesChanged) {
					refresh = updateScenes();
				}
				scriptElement = getScriptElementDAO().update(scriptElement, getAddedImages());
				getScriptElementDAO().updateRequirementSatisfied(scriptElement);
				if (refresh) {	// scene changes may have affected list contents,
					refreshList();	// so update the list
				}
				else { // just update current item
					updateItemInList(getElement());
				}
				getElement().setSelected(true);
				forceLazyInit();
				dropDateDL = null; // in case daysToHold changed
			}
			else { // first save - new element
				scriptElement.setProject(getProject());
				scriptElement = getScriptElementDAO().update(scriptElement, getAddedImages());
				updateChildren();
				for (RealLink rlink : getElement().getRealLinks()) {
					getRealLinkDAO().save(rlink);
				}
				ProductionDood.addElement(getProject(), scriptElement);
				refreshList(); // to show new entry; may leave scriptElement == null!
				scrollToRow();
			}
			if (scriptElement != null) { // might be 'invisible' if it's now an orphan
				SessionUtils.put(Constants.ATTR_SE_ELEMENT_ID, scriptElement.getId());
			}

			// Force refresh of the image resources list.
			scriptElement.setImageResources(null);

			return super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SaveFailed", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * The Action method for Cancel button while in Edit mode.
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		LOG.debug("");
		try {
			super.actionCancel();
			if (getElement() == null || isNewEntry()) {
				scriptElement = null; // flush new entry
				changeCategory(getCategory(), true); // select 1st item in current list
				//setupSelectedItem(null);
			}
			else {
				scriptElement = getScriptElementDAO().refresh(scriptElement);
				setupSelectedItem(getElement().getId());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * The Action method for ESCape key. Close either of our pop-up dialogs if they're open; if not,
	 * pass the escape call to our superclass.
	 *
	 * @return Navigation string, which is typically an empty String.
	 */
	@Override
	public String actionEscape() {
		LOG.debug("");
		String res = null;
		try {
			if (getShowAddScene()) {
				res = actionCloseAddScene();
			}
			else if (getShowRWElemLink()) {
				res = actionCloseRWLink();
			}
			else if (getShowChild()) {
				res = actionCloseChild();
			}
			else {
				res = super.actionEscape();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return res;
	}

	/**
	 * This method processes the request to delete the current ScriptElement,
	 * after the user has clicked OK in the confirmation dialog.
	 *
	 * @see com.lightspeedeps.web.view.ListView#actionDeleteOk()
	 */
	@Override
	public String actionDeleteOk() {
		try {
			scriptElement = getScriptElementDAO().refresh(scriptElement);
			getScriptElementDAO().remove(scriptElement);
			SessionUtils.put(Constants.ATTR_SE_ELEMENT_ID, null);
			scriptElementList.remove(scriptElement);
			scriptElement = null;
			setupSelectedItem(getRowId(getSelectedRow()));
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the "Add Scene" button on the edit screen; just opens
	 * up the pop-up dialog box.  The list of scenes inside the dialog box
	 * will be built "on demand" when {@link #getSelectScenes()} is called.
	 *
	 * @return null navigation string
	 */
	public String actionOpenAddScene() {
		showAddScene = true;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method for the Close button in the "Add Scene" pop-up dialog box.
	 * It just closes the dialog box.
	 *
	 * @return null navigation string
	 */
	public String actionCloseAddScene() {
		showAddScene = false;
		addClientResizeScroll();
		return null;
	}

	/**
	 * ActionListener method for the "add" buttons on the "add scenes" pop-up dialog.
	 *
	 * @param evt The event, which must contain an attribute called "sceneId" containing the
	 *            database id of the Scene to be associated with the current ScriptElement.
	 */
	public void actionAddScene(ActionEvent evt) {
		try {
			Integer addSceneId = (Integer)evt.getComponent().getAttributes().get("sceneId");
			LOG.debug(addSceneId);
			if (newEntry) {
				if (scriptElement.getScenes() == null) {
					scriptElement.setScenes(new HashSet<Scene>());
				}
			}

			Scene scene = getSceneDAO().findById(addSceneId);
			if (scene != null) {
				if (scriptElement.getType() != ScriptElementType.LOCATION) {
					scriptElement.getScenes().add(scene);
					scriptElement.setScenes(scriptElement.getScenes()); // forces sceneList refresh
				}
				else {
					scene.setScriptElement(scriptElement);
				}
				linkedScenes.add(scene); // Update the list displayed on page
				Collections.sort(linkedScenes);
				getSelectScenes().remove(scene); // update selection list in pop-up
			}
			//msgAdded = true;
			scenesChanged = true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Delete a link to a Scene. This is an Action method on a 'delete' icon in
	 * the "appears in scenes" table on the edit page. (Previously this method
	 * just put up the confirmation dialog.) The 'removeSceneId' was set via an
	 * f:setPropertyActionListener tag in the JSP.
	 */
	public String actionRemoveScene() {
/*		try {
			log.debug(removeSceneId);
			String msgIdPrefix = "ScriptElement.RemoveScene.";
			if (scriptElement.getScenes().size() < 2) {
				msgIdPrefix = "ScriptElement.RemoveSceneOrphan.";
			}
			PopupBean.getInstance().show(this, ACT_REMOVE_SCENE, msgIdPrefix);
			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
*/
		return actionRemoveSceneOk();
	}

	/**
	 * Remove a scene from the current ScriptElement's list of scenes. (This was called via
	 * the confirmOk() method, after the user confirms the delete request.)
	 * The 'removeSceneId' was set via an f:setPropertyActionListener tag in the JSP.
	 */
	public String actionRemoveSceneOk() {
		try {
			int removed = 0;
			for (Scene r : scriptElement.getScenes()) {
				if (removeSceneId.equals(r.getId())) {
					scriptElement.getScenes().remove(r);
					removed++; // for debugging
					break;
				}
			}
			LOG.debug(removed);
			scriptElement.setScenes(scriptElement.getScenes()); // forces sceneList refresh
			scenesChanged = true;
			setLinkedScenes(null); // refresh list of linked scenes
			setSelectScenes(null); // refresh list of scenes available for linking
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Delete a link to a RealWorldElement (a RealLink). This is an Action
	 * method on a 'delete' icon in the "linked real elements" table on the edit
	 * page. (This method previously just put up the confirmation dialog. Now it
	 * immediately proceeds to do the removal.) The 'removeLinkId' value has
	 * been set via an f:setPropertyActionListener tag in the JSP.
	 */
	public String actionRemoveRealLink() {
/*		try {
			log.debug("id=" + removeLinkId + ", name=" + removeName);
			PopupBean conf = PopupBean.getInstance();
			conf.show(
					this, ACT_REMOVE_REAL_LINK,
					"ScriptElement.RemoveLink.Title",
					"ScriptElement.RemoveLink.Ok",
					"Confirm.Cancel");
			String msg = MsgUtils.formatMessage("ScriptElement.RemoveLink.Text",
					scriptElement.getName(), removeName);
			conf.setMessage(msg);
			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
*/
		return actionRemoveRealLinkOk(); // bypass prompt
	}

	/**
	 * Delete a link to a RealWorldElement (a RealLink). This was called when the
	 * user confirms deletion, via the confirmOk() method; now it is called
	 * directly, bypassing the confirmation.
	 */
	public String actionRemoveRealLinkOk() {
		try {
			if (removeLinkId != null) {
				if (removeLinkId > 0) { // (will be negative if link was just added and not saved)
					removedElemLinks.add(removeLinkId);
				}
				boolean removeR = false; // debug code - may be removed for production
				// Remove the link from the set; can't just use Set.remove(removeLink) because we need
				// to do a custom comparison.
				Iterator<RealLink> ri = getElement().getRealLinks().iterator();
				while (ri.hasNext()) {
					RealLink r = ri.next();
					if (r.getId() != null && r.getId().equals(removeLinkId)) {
						getElement().getRealLinks().remove(r);
						getElement().setRealLinks(getElement().getRealLinks()); // force refresh of transients
						removeR = true; // only for debugging
						break;
					}
				}
				setLinkableElements(null); // refresh list if user opens "add link" dialog
				LOG.debug("remove=" + removeR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		setRemoveLinkId(null);
//		addClientResize();
		return null;
	}

	public String actionOpenRWLink() {
		showRWElemLink = true;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method from the "Cancel" button on the element-link
	 * pop-up dialog; just closes the pop-up.
	 * @return null navigation string
	 */
	public String actionCloseRWLink() {
		LOG.debug("");
		showRWElemLink = false;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method from one of the "Save" button on the element-link
	 * pop-up dialog.  Note that we don't actually update the database here,
	 * that happens (via cascades) when the user hits Save on the Edit
	 * page and we do an attachDirty of the active scriptElement.
	 * @return empty string
	 */
	public String actionAddRWLink() {
		try {
			LOG.debug(addId);
			if (addId != null) {
				RealWorldElement elem = RealWorldElementDAO.getInstance().findById(addId);
				if (elem != null) {
					RealLink rlink = new RealLink(scriptElement, elem);
					rlink.setId(0-elem.getId()); // negative value to track unsaved links
					elem.getRealLinks().add(rlink);
					getElement().getRealLinks().add(rlink);
					getElement().setRealLinks(getElement().getRealLinks()); // force refresh of transients
					linkableElements.remove(elem); // update selection list in pop-up
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the "Add" button for linked script elements.
	 * Opens the "Add child/linked element" pop-up.
	 * @return Empty string.
	 */
	public String actionOpenChild() {
		showChild = true;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method for the "Close" button on the element-link pop-up dialog;
	 * just closes the pop-up.
	 *
	 * @return Empty string.
	 */
	public String actionCloseChild() {
		LOG.debug("");
		showChild = false;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method from one of the "Save" button on the element-link
	 * pop-up dialog.  Note that we don't actually update the database here,
	 * that happens (via cascades) when the user hits Save on the Edit
	 * page and we do an attachDirty of the active scriptElement.
	 * @return empty string
	 */
	public String actionAddChild() {
		try {
			LOG.debug(addId);
			if (addId != null) {
				ScriptElement elem = getScriptElementDAO().findById(addId);
				if (elem != null) {
					if (getElement().getChildElements() == null) {
						getElement().setChildElements(new HashSet<ScriptElement>());
					}
					getElement().getChildElements().add(elem);
					eligibleChildren.remove(elem); // update selection list in pop-up
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Delete a link to another ScriptElement (a child link). This is an Action
	 * method on a 'delete' icon in the "linked elements" table on the edit
	 * page. The 'removeLinkId' value has been set via an
	 * f:setPropertyActionListener tag in the JSP.
	 */
	public String actionRemoveChild() {
		try {
			if (removeLinkId != null) {
				if (removeLinkId > 0) { // (will be negative if link was just added and not saved)
					removedChildren.add(removeLinkId);
				}
				boolean removeR = false; // debug code - may be removed for production
				// Remove the link from the set; can't just use Set.remove(removeLink) because we need
				// to do a custom comparison.
				Iterator<ScriptElement> iter = getElement().getChildElements().iterator();
				while (iter.hasNext()) {
					ScriptElement se = iter.next();
					if (se.getId() != null && se.getId().equals(removeLinkId)) {
						iter.remove();
						removeR = true; // only for debugging
						break;
					}
				}
				setEligibleChildren(null); // refresh list if user opens "add child" dialog
				LOG.debug("remove=" + removeR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		setRemoveLinkId(null);
//		addClientResize();
		return null;
	}

	/**
	 * This is the valueChangeListener method of the script element type dropdown list within the
	 * "Add Child" pop-up dialog.
	 *
	 * @param evt The event whose "newValue" is the 'value' field in the SelectItem of the row
	 *            selected by the user.  This should be one of the ScriptElementType names.
	 */
	public void changeChildType(ValueChangeEvent evt) {
		try {
			if (evt.getNewValue() != null) {
				String type = evt.getNewValue().toString();
				childType = ScriptElementType.valueOf(type);
				LOG.debug("selectedCategory Type  is " + childType);
				eligibleChildren = createEligibleChildren(childType);
			}
		}
		catch (Exception e) {
			EventUtils.logError("select category exception: ", e);
		}
	}

	/**
	 * Action method for the "Retro" button, to retroactively add linked script elements.
	 * Opens the "apply retroactively" pop-up.
	 * @return Empty string.
	 */
	public String actionOpenRetro() {
		childItems = createChildItems();
		if (childItems.size() > 0) {
			showRetro = true;
			addClientResizeScroll();
		}
		return null;
	}

	/**
	 * Action method for the "Cancel" button on the retroactive
	 * pop-up dialog; just closes the pop-up.
	 * @return Empty string.
	 */
	public String actionCancelRetro() {
		LOG.debug("");
		showRetro = false;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method for the "make retroactive" button on the retroactive-add
	 * element-link pop-up dialog. This actually adds all the selected items to
	 * the set of Scene`s in which the current element appears.
	 *
	 * @return empty string
	 */
	public String actionApplyRetro() {
		try {
			showRetro = false;
			boolean updated = false;
			if (childItems != null) {
				// Collect set of elements that user wants added retroactively:
				Set<ScriptElement> added = new HashSet<>();
				for (Item item : childItems) {
					if (item.getSelected()) {
						ScriptElement elem = getScriptElementDAO().findById(item.getId());
						if (elem != null) {
							added.add(elem);
						}
					}
				}
				LOG.debug(added.size());
				if (added.size() > 0) {
					// Now apply to all the scenes containing the "parent" element
					Set<Scene> scenes;
					if (scriptElement.getType() == ScriptElementType.LOCATION) {
						scenes = scriptElement.getScenesForLocation();
					}
					else {
						scenes = scriptElement.getScenes();
					}
					if (scenes != null && scenes.size() > 0) {
						for (Scene scene : scenes) {
							scene = getSceneDAO().refresh(scene);
							scene.getScriptElements().addAll(added);
							getSceneDAO().attachDirty(scene);
						}
						ProductionDood.markProjectDirty(getProject(), (scriptElement.getType() == ScriptElementType.CHARACTER));
						updated = true;
					}
				}
			}
			if (updated && ! getShowOrphans()) {
				// orphans are hidden, and maybe some elements are no longer orphans
				refreshList(); // so rebuild our list
			}
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

//	/**
//	 * Action method to open the "Create contact" pop-up dialog for setting the responsible party field.
//	 */
//	public String actionOpenQuickAdd() {
//		try {
//			quickContact = (QuickContactBean) ServiceFinder.findBean("quickContactBean");
//			quickContact.setVisible(true);
////			addClientResize();
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
//		return null;
//	}

//	/**
//	 * Action method for the Save button on the "create contact" pop-up dialog. If the new Contact
//	 * is saved successfully, set it as the current element's responsible party.
//	 */
//	public String actionSaveQuickAdd() {
//		log.debug("");
//		try {
//			if ( quickContact.save() ) {
//				scriptElement.setContact(quickContact.getContact());
//				contactDL = null; // force update of contact list
//				setupContact();
//			}
//			addClientResizeScroll();
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
//		return null;
//	}

	/**
	 * @return The list of images owned by the current element.  This is used
	 * by the ImagePaginatorBean and ImageAddBean when adding and removing
	 * elements.
	 */
	@Override
	public List<Image> getImageList() {
		return getElement().getImages();
	}

	/**
	 * Called when user clicks "Ok" (or equivalent) on a standard confirmation dialog.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		try {
			switch(action) {
				case ACT_REMOVE_SCENE:
					res = actionRemoveSceneOk();
					break;
				case ACT_REMOVE_REAL_LINK:
					res = actionRemoveRealLinkOk();
					break;
				default:
					res = super.confirmOk(action);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return res;
	}

	/**
	 * The ValueChangeListener for the "responsible party" drop-down list.
	 * Note that the first entry has a value of -1 with a label of "select contact"
	 * or something similar.
	 * @param evt
	 */
	public void changeContact(ValueChangeEvent evt) {
		try {
			if (evt.getNewValue() != null) { // got null value once - how?
				Integer id = Integer.parseInt(evt.getNewValue().toString());
				LOG.debug("id=" + id);
				if (id.intValue() == -1) {
					LOG.debug("clearing contact");
					scriptElement.setContact(null);
				}
				else {
					Contact contact = ContactDAO.getInstance().findById(id);
					if (contact != null) {
						scriptElement.setContact(contact);
					}
				}
				setupContact();
			}
			else {
				LOG.warn("null newValue in change event: " + evt);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * The Value Change Listener for the category (element type) selection
	 * drop-down list on the element list (left-hand side) display.
	 * @param evt
	 */
	public void selectedCategory(ValueChangeEvent evt) {
		try {
			if (evt.getNewValue() != null) {
				changeCategory( (String)evt.getNewValue(), ! editMode);
			}
			else {
				LOG.warn("Null newValue in category change event: " + evt);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * The Value Change Listener for the category (element type) selection
	 * drop-down list on the Details (right-hand) tab.  This is only available
	 * in Edit mode.
	 * @param evt
	 */
	public void changeEditCategory(ValueChangeEvent evt) {
		LOG.debug("");
		try {
			if (evt.getNewValue() != null) {
				String cat = (String)evt.getNewValue();
				if (! category.equals(Constants.CATEGORY_ALL)) {
					changeCategory( cat, false );
				}
				if (newEntry) {
					ScriptElementType t = ScriptElementType.valueOf(cat);
					if (t == ScriptElementType.CHARACTER) {
						if (scriptElement.getElementId() == null) {
							scriptElement.setElementIds(
									getScriptElementDAO().findNextElementId(getProject(), ScriptElementType.CHARACTER));
						}
					}
					else {
						scriptElement.setElementIds(null);
					}
					// This didn't work; boolean is reset by checkbox during processValues phase:
					// ScriptUtils.setDefaultRWERequired(scriptElement);
				}
			}
			else {
				LOG.warn("Null newValue in category change event: " + evt);
			}
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Value Change Listener for check box selecting whether to
	 * show "all elements" or "only orphan elements".
	 */
	public void changeShowOrphans(ValueChangeEvent event) {
		try {
			showOrphans = (Boolean)event.getNewValue();
			refreshList();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Value Change Listener for the check box that allows drop/pickup
	 * to be used for this item.
	 */
	public void changeDropPickup(ValueChangeEvent event) {
		try {
			LOG.debug("new value=" + event.getNewValue());
			if ((Boolean)event.getNewValue()) {
				updateDropPickup();
			}
			else { // if unchecked, don't need to rebuild DooD values;
				dropDateDL = null;	// just refresh drop-date list
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Value Change Listener for the minimum hold days setting.
	 */
	public void changeHoldDays(ValueChangeEvent event) {
		LOG.debug("new value=" + event.getNewValue());
		updateDropPickup();
	}

	/**
	 * Drop/hold data changed requiring us to update the
	 * DooD values and the list of drop dates available.
	 */
	private void updateDropPickup() {
		try {
			ProductionDood.markProjectDirty(getProject(), false);
			dropDateDL = null;	// refresh drop-date list
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	private List<SelectItem> createDropDateList() {
		List<SelectItem> dropDates = new ArrayList<>();
		dropDates.add(new SelectItem(NULL_DATE, "None"));
		if (scriptElement.getDropPickupAllowed()) {
			ElementDood edood = scriptElement.getElementDood();
			if (edood != null && edood.getHoldDates() != null) {
				for (DatePair dp : edood.getHoldDates()) {
					if (scriptElement.getDaysHeldBeforeDrop() != null && dp.getSpan() >= scriptElement.getDaysHeldBeforeDrop()) {
						dropDates.add(new SelectItem(dp.getStartDate(), dp.getSelectLabel()));
					}
				}
			}
		}
		return dropDates;
	}

	/**
	 * Set a new category (or "All") as the type of item listed. This regenerates the list of
	 * elements listed. If the currently selected item is not in the new list, then select the first
	 * entry of the new list.
	 *
	 * @param type A ScriptElementType value, or "All".
	 */
	private void changeCategory(String type, boolean pickElement) {
		if (getElement() != null && ! editMode) {
			getElement().setSelected(false); // we may end up switching
		}
		SessionUtils.put(Constants.ATTR_SE_CATEGORY, type);
		category = type;
		if ( ! type.equals(Constants.CATEGORY_ALL)) {
			ScriptElementType t = ScriptElementType.valueOf(type);
			if (showOrphans) {
				scriptElementList = getScriptElementDAO().findByTypeAndProject(t, getProject());
			}
			else {
				scriptElementList = getScriptElementDAO().findNonOrphansByTypeAndProject(t, getProject());
			}
		}
		else {
			if (showOrphans) {
				scriptElementList = getScriptElementDAO().findByProject(getProject());
			}
			else {
				scriptElementList = getScriptElementDAO().findNonOrphansByProject(getProject());
			}
		}
		setSelectedRow(-1);
		doSort();	// the new list may have been previously sorted by some other column
		if (pickElement) { // possibly select an element to view
			@SuppressWarnings("unchecked")
			List<ScriptElement> list = getItemList();
			if (getElement() != null) {
				int ix = list.indexOf(scriptElement);
				if (ix < 0) {
					LOG.debug(type + ", " + scriptElement.getType());
					if (list.size() > 0) {
						setupSelectedItem(list.get(0).getId());
					}
					else {
						setupSelectedItem(null); // clear View if nothing in list
					}
				}
				else {
					scriptElement = list.get(ix);
					getElement().setSelected(true);
					forceLazyInit(); // refresh referenced data
				}
			}
			else {
				// no current element & not doing "Add"; if list has entries, view the first
				if (list.size() > 0) {
					setupSelectedItem(list.get(0).getId());
				}
			}
		}
		addClientResizeScroll();
	}

	@Override
	protected void refreshList() {
		changeCategory(getCategory(), true);
	}

	/**
	 * Process any "removed" links to script elements that occurred during the
	 * edit session.  It's a bit complicated because of the many-to-many relation
	 * between RWE's and SE's maintained by the RealLink objects.
	 */
	private void updateRealLinks() {
		try {
			LOG.debug(removedElemLinks.size());
			if (removedElemLinks.size() > 0) {
				ScriptElement tempElement = getScriptElementDAO().findById(scriptElement.getId());
				for (Integer linkId : removedElemLinks) {
					RealLink rl = getRealLinkDAO().findById(linkId); // fresh copy
					boolean remove1 = tempElement.getRealLinks().remove(rl);
					RealWorldElement realElem = rl.getRealWorldElement();
					boolean remove2 = realElem.getRealLinks().remove(rl);
					if (remove1 && remove2) {
						getRealLinkDAO().delete(rl);
						LOG.debug("remove successful");
						ProductionDood.markDirty(getProject(), realElem); // DooD values need updating
					}
					else {
						EventUtils.logError("ScriptElementBean: remove-rw=" + remove1 + ", remove-se=" + remove2);
						LOG.debug("rl=" + rl + ", rw-set:" + tempElement.getRealLinks() + ", se-set:"
								+ realElem.getRealLinks());
					}
				}
				tempElement.setRealLinks(null); // avoids "NonUniqueObjectException" on realLink's
				getScriptElementDAO().evict(tempElement);
				removedElemLinks.clear();  // just in case
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	@Override
	public void removeImage(Image image ) {
		// Call the super method first then refresh the
		// Image Resources list.
		super.removeImage(image);

		// Force refresh of the image resources list.
		scriptElement.setImageResources(null);
	}

	/**
	 * Update the links between the current ScriptElement (being saved) and
	 * Scene`s in which it should be included. Also adds child elements if this
	 * element was added to new Scene`s in the latest edit cycle.
	 * @return True if "refreshList()" should be called after the element has
	 * been updated in the database.
	 */
	private boolean updateScenes() {
		boolean refresh = false;
		boolean dirty = false;
		boolean updated = false;
		Set<Scene> newscenes = new HashSet<>();
		if (scriptElement.getType() != ScriptElementType.LOCATION) {
			List<String> selectedScenes = getSelectedScenes(); // make it current
			ScriptElement tempElement = null;
//			if ( ! newEntry) {
				tempElement = getScriptElementDAO().findById(scriptElement.getId());
				// check scenes in existing set
				for (Scene scene : tempElement.getScenes()) {
					String idstr = scene.getId().toString();
					if ( selectedScenes.contains(idstr)) { // existing link
						newscenes.add(scene);
						selectedScenes.remove(idstr); // we don't need to add this link
					}
					else { // user deleted this scene from list, update the Scene
						scene.getScriptElements().remove(tempElement);
						getSceneDAO().attachDirty(scene);
						dirty = true;
						updated = true;
					}
				}
//			}
			// 'selectedScenes' now has the ids of those Scenes that were added
			// to the list of linked Scenes during the current Edit cycle.

			// get Set of child elements linked to current one
			Set<ScriptElement> linkedElems = BreakdownBean.createFamily(scriptElement);
			linkedElems.remove(scriptElement);

			// handle added scenes
			for (String str : selectedScenes) {
				dirty = true; // at least 1 added
				Integer id = Integer.parseInt(str);
				Scene scene = getSceneDAO().findById(id);
				if (scene != null) {
					newscenes.add(scene);
					for (ScriptElement se : linkedElems) {
						se.getScenes().add(scene);
						se.setScenes(se.getScenes()); // forces sceneList refresh
						updated = true;
					}
					scene.getScriptElements().addAll(linkedElems);
//					if ( ! newEntry) {
						scene.getScriptElements().add(tempElement);
//					}
					getSceneDAO().attachDirty(scene);
				}
			}
//			if (tempElement != null) {
				getScriptElementDAO().evict(tempElement);
//			}
		}
		else { // type = LOCATION (Set)
			for (Scene s : linkedScenes) {
				Scene scene = getSceneDAO().findById(s.getId());
				if (! scriptElement.equals(scene.getSet())) {
					LOG.debug("updating Set for scene id#" + scene.getId());
					scene.setScriptElement(scriptElement);
					getSceneDAO().attachDirty(scene);
					dirty = true;
				}
			}
		}

		if (dirty) {
			if (scriptElement.getType() != ScriptElementType.LOCATION) {
				scriptElement.setScenes(newscenes);
			}
			ProductionDood.markProjectDirty(getProject(), (scriptElement.getType() == ScriptElementType.CHARACTER));
			if (updated && ! getShowOrphans()) {
				// we're not showing orphans, and this element may now be an orphan,
				// or child elements may have become non-orphaned
				refresh = true;
			}
		}
		return refresh;
	}


	/**
	 * Add the current ScriptElement's child elements to its Scenes if this
	 * element was added to any Scene`s. This method is only called for new
	 * elements, i.e., the first time a ScriptElement is saved.
	 */
	private void updateChildren() {
		if (scriptElement.getChildElements() == null ||
				scriptElement.getChildElements().size() == 0 ||
				getLinkedScenes().size() == 0) {
			return;
		}

		boolean dirty = false;
		boolean updateDrops = false;
		boolean added = false;
		// get Set of child elements linked to current one
		Set<ScriptElement> linkedElems = BreakdownBean.createFamily(scriptElement);
		linkedElems.remove(scriptElement); // current one's been done already

		// scan linked scenes
		if (linkedElems.size() > 0) {
			for (Scene scene : getLinkedScenes()) {
				if (scene != null) {
					scene = getSceneDAO().refresh(scene);
					dirty = true;
					for (ScriptElement se : linkedElems) {
						if (se.getScenes().size() == 0) {
							added = true;
						}
						if (se.getType() == ScriptElementType.CHARACTER) {
							updateDrops = true;
						}
						se.getScenes().add(scene);
						se.setScenes(se.getScenes()); // forces sceneList refresh
					}
					scene.getScriptElements().addAll(linkedElems);
					getSceneDAO().attachDirty(scene);
				}
			}
		}
		if (dirty) {
			ProductionDood.markProjectDirty(getProject(), updateDrops);
			if (added && ! getShowOrphans()) {
				// we're hiding orphans, and some element may no longer be an orphan
				refreshList();	// so update the list
			}
		}
	}

	/**
	 * Create a list of Scenes from which the user may select one or
	 * more to link to the current ScriptElement.  It includes all the
	 * Scenes in the current Script except for those Scenes already
	 * linked to the scriptElement.
	 * @return the non-null List described above.
	 */
	private List<Scene> createSelectScenes() {
		List<Scene> list = new ArrayList<>();
		project = SessionUtils.getCurrentProject();
		if (getProject().getScript() != null) {
			list.addAll(getProject().getScript().getScenes());
			for (Scene s : getLinkedScenes()) {
				list.remove(s);
			}
			@SuppressWarnings("unused")
			String str;
			for (Scene s : list) {
				str = s.getHeading(); // force lazy initialization
			}
		}
		return list;
	}

	/**
	 * Create a list of Scene`s in the current Script to which the current
	 * ScriptElement is linked.
	 *
	 * @return A possibly empty (but not null) List of Scene`s.
	 */
	private List<Scene> createLinkedScenes() {
		List<Scene> list = new ArrayList<>();
		@SuppressWarnings("unused")
		String str;
		project = SessionUtils.getCurrentProject();
		if (scriptElement != null && getProject().getScript() != null) {
			if (scriptElement.getType() == ScriptElementType.LOCATION) {
				for (Scene s : getProject().getScript().getScenes()) {
					if (scriptElement.equals(s.getScriptElement())) {
						s = getSceneDAO().refresh(s);
						list.add(s);
						str = s.getHeading(); // force lazy initialization
					}
				}
			}
			else {
				int scriptId = getProject().getScript().getId();
				for (Scene s : scriptElement.getScenes()) {
					s = getSceneDAO().refresh(s);
					if (s.getScript().getId() == scriptId) {
						list.add(s);
						str = s.getHeading(); // force lazy initialization
					}
				}
			}
		}
		Collections.sort(list);
		return list;
	}

	/**
	 * Creates the available list of items for the "Add link to RWE" dialog box.
	 *
	 * @see #linkableElements
	 * @return A non-null but possibly empty list of RealWorldElement`s that are
	 *         suitable matches for the currently displayed ScriptElement.
	 */
	private List<RealWorldElement> createLinkableElements() {
		LOG.debug("");
		if (scriptElement == null) {
			return new ArrayList<>();
		}
		List<RealWorldElement> elems = null;
		try {
			ScriptElementType type = scriptElement.getType();
			if (type == ScriptElementType.CHARACTER) {
				elems = RealWorldElementDAO.getInstance().findCastByProject(SessionUtils.getCurrentProject());
			}
			else {
				elems = RealWorldElementDAO.getInstance().findByProductionAndTypeOrdered(type);
			}
			// remove already-linked elements
			for (RealLink rl : scriptElement.getRealLinks()) {
				RealWorldElement elem = rl.getRealWorldElement();
				if (elems.contains(elem)) {
					elems.remove(elem);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return elems;
	}

	/**
	 * Create a list of ScriptElement`s that are eligible to be added as child
	 * elements of the current item. This is all non-LOCATION elements in the
	 * project, except for those that are already linked as children of this
	 * item.
	 *
	 * @param type The ScriptElementType of elements to be displayed in the
	 *            list.
	 * @return The List of possible child elements.
	 */
	private List<ScriptElement> createEligibleChildren(ScriptElementType type) {
		LOG.debug("");
		if (scriptElement == null) {
			return new ArrayList<>();
		}
		List<ScriptElement> elems = null;
		try {
			elems = getScriptElementDAO().findByTypeAndProject(type, SessionUtils.getCurrentProject());
			// remove already-linked elements
			if (scriptElement.getChildElements() != null) {
				elems.removeAll(scriptElement.getChildElements());
			}
			elems.remove(scriptElement);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return elems;
	}

	private List<Item> createChildItems() {
		List<Item> items = new ArrayList<>();
		if (scriptElement.getChildElements() != null) {
			Set<ScriptElement> childElems = BreakdownBean.createFamily(scriptElement);
			childElems.remove(scriptElement); // don't show item itself
			for (ScriptElement child : childElems) {
				Item item = new Item(child.getId(), child.getName());
				items.add(item);
			}
			Collections.sort(items);
		}
		return items;
	}

	/**
	 * If no item is currently selected, find the current element in
	 * the main (left-hand) list, select it, and send a JavaScript
	 * command to scroll the list so that it is visible.
	 */
	private void scrollToRow() {
		scrollToRow(getElement());
	}

	/**
	 * Return the id of the item that resides in the n'th row of the
	 * currently displayed list.
	 * @param row
	 * @return Returns null only if the list is empty.
	 */
	protected Integer getRowId(int row) {
		Object item;
		return ((item=getRowItem(row)) == null ? null : ((ScriptElement)item).getId());
	}

	@Override
	protected Comparator<ScriptElement> getComparator() {
		Comparator<ScriptElement> comparator = new Comparator<ScriptElement>() {
			@Override
			public int compare(ScriptElement elem1, ScriptElement elem2) {
				int ret = NumberUtils.compare(elem1.getType().ordinal(), elem2.getType().ordinal());
				if (ret == 0 && getSortColumnName() != null) {
					if (getSortColumnName().equals("Occurs") || getSortColumnName().equals("Total")) {
						ElementDood d1 = elem1.getElementDood();
						ElementDood d2 = elem2.getElementDood();
						if (d1 == null || d2 == null) {
							if (d1 == null) {
								if (d2 != null) {
									ret = -1;
								}
							}
							else {
								ret = 1;
							}
						}
						else if (getSortColumnName().equals("Occurs")) {
							ret = NumberUtils.compare( d1.getScriptOccurs(), d2.getScriptOccurs());
						}
						else if (getSortColumnName().equals("Total")) {
							ret = NumberUtils.compare( d1.getTotalDays(), d2.getTotalDays());
						}
						if ( ! isAscending() ) {
							ret = 0 - ret;	// swap 1 and -1 return values
							// Note that we do NOT invert non-equal Type compares
						}
					}
					if (ret == 0) {
						ret = elem1.compareTo(elem2, getSortColumnName(), isAscending());
					}
				}
				return ret;
			}
		};
		return comparator;
	}

	/**
	 * @return the currently selected/displayed scriptElement
	 */
	public ScriptElement getScriptElement() {
		return scriptElement;
	}
	public void setScriptElement(ScriptElement scriptElement) {
		this.scriptElement = scriptElement;
	}
	/**
	 * @return the currently selected/displayed scriptElement
	 */
	public ScriptElement getElement() { // to simplify JSP
		return scriptElement;
	}

	/** Return the current element's name, for use in the title of the Add Image dialog */
	@Override
	public String getElementName() {
		return getElement().getName();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return scriptElementList;
	}

	/** See {@link #hasScript}. */
	public boolean getHasScript() {
		return hasScript;
	}
	/** See {@link #hasScript}. */
	public void setHasScript(boolean noScript) {
		hasScript = noScript;
	}

	public List<SelectItem> getContactDL() {
		if (contactDL == null) {
			ContactDAO contactDAO = ContactDAO.getInstance();
			contactDL = contactDAO.createContactSelectList(contactDAO.findCrew(false), true);
		}
		return contactDL;
	}

	/**See {@link #project}. */
	private Project getProject() {
		if (project == null) {
			project = SessionUtils.getCurrentProject();
		}
		return project;
	}

	public String getType() {
		return (getElement().getType() == null ? "" : getElement().getType().name());
	}
	public void setType(String typeStr) {
		getElement().setType(ScriptElementType.valueOf(typeStr));
	}

	/** Returns the list of linked scenes; creates the list first,
	 * if it doesn't already exist.
	 * @see #linkedScenes */
	public List<Scene> getLinkedScenes() {
		if (linkedScenes == null) {
			linkedScenes = createLinkedScenes();
		}
		return linkedScenes;
	}
	/** See {@link #linkedScenes}. */
	public void setLinkedScenes(List<Scene> linkedScenes) {
		this.linkedScenes = linkedScenes;
	}


	// * * * Select scenes pop-up * * *

	/** Returns the list of scenes available for selection in the "Add Scenes" dialog box.
	 * It creates the list first if it doesn't already exist.
	 * @see #selectScenes */
	public List<Scene> getSelectScenes() {
		if (selectScenes == null) {
			selectScenes = createSelectScenes();
		}
		return selectScenes;
	}
	/** See {@link #selectScenes}. */
	public void setSelectScenes(List<Scene> selectScenes) {
		this.selectScenes = selectScenes;
	}

	public List<String> getSelectedScenes() {
		List<String> selectedScenes = new ArrayList<>();
		for (Scene scene : scriptElement.getSceneList()) {
			selectedScenes.add(scene.getId().toString());
		}
		return selectedScenes;
	}

	/** See {@link #showAddScene}. */
	public boolean getShowAddScene() {
		return showAddScene;
	}
	/** See {@link #showAddScene}. */
	public void setShowAddScene(boolean b) {
		showAddScene = b;
	}

	// * * * Add Real World Element link pop-up * * *

	/** See {@link #showRWElemLink}. */
	public boolean getShowRWElemLink() {
		return showRWElemLink;
	}
	/** See {@link #showRWElemLink}. */
	public void setShowRWElemLink(boolean b) {
		showRWElemLink = b;
	}

	/** Returns the available list of items for the "Add link to RWE"
	 * dialog box.  It will create the list if it does not already exist.
	 * @see #linkableElements */
	public List<RealWorldElement> getLinkableElements() {
		if (linkableElements == null) {
			linkableElements = createLinkableElements();
		}
		return linkableElements;
	}
	/** See {@link #linkableElements}. */
	public void setLinkableElements(List<RealWorldElement> linkableElements) {
		this.linkableElements = linkableElements;
	}

	// * * * Add Child Element link pop-up * * *

	/** See {@link #showChild}. */
	public boolean getShowChild() {
		return showChild;
	}
	/** See {@link #showChild}. */
	public void setShowChild(boolean b) {
		showChild = b;
	}

	/** See {@link #eligibleChildren}. */
	public List<ScriptElement> getEligibleChildren() {
		if (eligibleChildren == null) {
			childType = ScriptElementType.valueOf(childTypeName);
			eligibleChildren = createEligibleChildren(childType);
		}
		return eligibleChildren;
	}
	/** See {@link #eligibleChildren}. */
	public void setEligibleChildren(List<ScriptElement> eligibleChildren) {
		this.eligibleChildren = eligibleChildren;
	}

	/** See {@link #childTypeName}. */
	public String getChildTypeName() {
		return childTypeName;
	}
	/** See {@link #childTypeName}. */
	public void setChildTypeName(String childTypeName) {
		this.childTypeName = childTypeName;
	}

	// * * * Retroactively Add Child Elements pop-up * * *

	/** See {@link #showRetro}. */
	public boolean getShowRetro() {
		return showRetro;
	}
	/** See {@link #showRetro}. */
	public void setShowRetro(boolean showRetro) {
		this.showRetro = showRetro;
	}

	/** See {@link #childItems}. */
	public List<Item> getChildItems() {
		return childItems;
	}
	/** See {@link #childItems}. */
	public void setChildItems(List<Item> childItems) {
		this.childItems = childItems;
	}

	/** See {@link #castId}. */
	public Integer getCastId() {
		return castId;
	}
	/** See {@link #castId}. */
	public void setCastId(Integer castId) {
		this.castId = castId;
	}

	private List<SelectItem> createEditTypeDL() {
		List<SelectItem> list = new ArrayList<>();
		list.add(0, Constants.GENERIC_ALL_ITEM);
		if (getElement() != null) {
			list.add(1,new SelectItem(getElement().getType().name(),getElement().getType().getLabel()));
		}
		return list;
	}

	public List<SelectItem> getScriptElementTypeDL() {
		if (editMode) {
			return createEditTypeDL();
		}
		return scriptElementTypeDL;
	}
	public void setScriptElementTypeDL(List<SelectItem> scriptElementTypeDL) {
		this.scriptElementTypeDL = scriptElementTypeDL;
		LOG.debug(""+scriptElementTypeDL.size());
	}

	public List<SelectItem> getDropDateDL() {
		if (dropDateDL == null) {
			dropDateDL = createDropDateList();
		}
		return dropDateDL;
	}

	public String getCategory() {
		return category;
	}
	/** This is only used by the framework, and we need to IGNORE that, because we
	 * may have changed the category during an earlier phase of the life-cycle. */
	public void setCategory(String category) {
		//this.category = category;
	}

	/** See {@link #removeSceneId}. */
	public Integer getRemoveSceneId() {
		return removeSceneId;
	}
	/** See {@link #removeSceneId}. */
	public void setRemoveSceneId(Integer id) {
		//log.debug(id);
		removeSceneId = id;
	}

	public Integer getAddId() {
		return addId;
	}
	public void setAddId(Integer addedInterest) {
		addId = addedInterest;
	}

	/** See {@link #removeLinkId}. */
	public Integer getRemoveLinkId() {
		return removeLinkId;
	}
	/** See {@link #removeLinkId}. */
	public void setRemoveLinkId(Integer id) {
		//log.debug(id);
		removeLinkId = id;
	}

	/** See {@link #removeName}. */
	public void setRemoveName(String removeName) {
		this.removeName = removeName;
	}
	/** See {@link #removeName}. */
	public String getRemoveName() {
		return removeName;
	}

	/** See {@link #contactId}. */
	public Integer getContactId() {
		return contactId;
	}
	/** See {@link #contactId}. */
	public void setContactId(Integer responsiblePartyId) {
		// Drop-down should not set this to null, but we had Event error where
		// contactId was null.  So don't let it get set to null!
		if (responsiblePartyId != null) {
			contactId = responsiblePartyId;
		}
	}

	/** See {@link #showOrphans}. */
	public boolean getShowOrphans() {
		return showOrphans;
	}
	/** See {@link #showOrphans}. */
	public void setShowOrphans(boolean showOrphansOnly) {
		showOrphans = showOrphansOnly;
	}

//	/** See {@link #msgAdded}. */
//	public boolean isMsgAdded() {
//		return msgAdded;
//	}
//	/** See {@link #msgAdded}. */
//	public void setMsgAdded(boolean msgSceneAdded) {
//		this.msgAdded = msgSceneAdded;
//	}

	private RealLinkDAO getRealLinkDAO() {
		if (realLinkDAO == null) {
			realLinkDAO = RealLinkDAO.getInstance();
		}
		return realLinkDAO;
	}

	private SceneDAO getSceneDAO() {
		if (sceneDAO == null) {
			sceneDAO = SceneDAO.getInstance();
		}
		return sceneDAO;
	}

	private ScriptElementDAO getScriptElementDAO() {
		if (scriptElementDAO == null) {
			scriptElementDAO = ScriptElementDAO.getInstance();
		}
		return scriptElementDAO;
	}

}
