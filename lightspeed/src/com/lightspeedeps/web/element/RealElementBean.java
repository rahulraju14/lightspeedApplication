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
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.model.table.RowState;

import com.lightspeedeps.dao.AddressDAO;
import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.ImageDAO;
import com.lightspeedeps.dao.PointOfInterestDAO;
import com.lightspeedeps.dao.ProjectMemberDAO;
import com.lightspeedeps.dao.RealLinkDAO;
import com.lightspeedeps.dao.RealWorldElementDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dood.ElementDood;
import com.lightspeedeps.dood.ProjectDood;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.DateRange;
import com.lightspeedeps.model.Image;
import com.lightspeedeps.model.PointOfInterest;
import com.lightspeedeps.model.RealLink;
import com.lightspeedeps.model.RealWorldElement;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.type.Permission;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.contact.ContactViewBean;
import com.lightspeedeps.web.image.ImageAddBean;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.view.ListImageView;

/**
 * Backing bean for the "Production Elements" page (originally called
 * "Real World Elements"). Includes both View and Edit code. This class handles
 * most of the features and functions associated with the "Details", "Map" and
 * "Manage" mini-tabs. The "Images" mini-tab is supported mostly by the
 * {@link ListImageView} superclass. The left-hand list of the page is mostly
 * supported by the {@link com.lightspeedeps.web.view.ListView ListView} superclass.
 */
@ManagedBean
@ViewScoped
public final class RealElementBean extends ListImageView implements Serializable {
	/** */
	private static final long serialVersionUID = - 6534533170648355156L;

	private static final Log LOG = LogFactory.getLog(RealElementBean.class);

	private static final int TAB_DETAIL = 0;
	//private static final int TAB_IMAGES = 1;
	private static final int TAB_MAP = 2; // only for Location element types
	private static final int TAB_MANAGER = 3; // only for Location element types

	private static final String INITIAL_CATEGORY = Constants.CATEGORY_ALL;//ScriptElementType.CHARACTER;

	private static final int ACT_REMOVE_BLACKOUT = 10;
	private static final int ACT_REMOVE_SCRIPT_LINK = 11;
	private static final int ACT_REMOVE_INTEREST = 12;
	private static final int ACT_DELETE_MAP = 13;

	private static final SelectItem SELECT_LOCATION =
			new SelectItem(ScriptElementType.LOCATION.name(), ScriptElementType.LOCATION.getRwLabel());
//	private static final SelectItem SELECT_CAST =
//			new SelectItem(ScriptElementType.CHARACTER.name(), ScriptElementType.CHARACTER.getRwLabel());

	/** True if user is allowed to view non-Location type Real World Elements.  Note
	 * that all users can automatically view Location RWEs. */
	private boolean showNonLocation = false;
	/** True if user is allowed to edit non-Location type Real World Elements,
	 * not including Cast type.  */
	private boolean editNonLocation = false;
	/** True if user is allowed to view Cast RW Elements. */
	private boolean showCast = false;
	/** True if user is allowed to edit (and create) Cast RW Elements. */
	private boolean editCast = false;
	/** True if user is allowed to edit (and create) Location type RW Elements. */
	private boolean editLocation = false;
	/** True if the "Add Link" button should be displayed in Edit mode.  This will be
	 * false if the current element is a Cast element whose corresponding Contact is
	 * NOT a member of the current project.  It is null when it has not been set yet --
	 * it is lazy initialized for Cast types, to avoid the database call needed, as it
	 * is only used in Edit mode. */
	private Boolean showAddLink;

	/**
	 * Only display linked ScriptElements that are in the current project. This controls the list
	 * display on the right-hand side, in the "linked-to/playing" table. The flag is set or unset by
	 * the user via a checkbox above the list.
	 */
	private boolean showProjectOnly = false;

	/**
	 * Only display RealWorldElements (in the main RW list) that are already linked to the current
	 * project. This is set or unset by the user via a checkbox on the page above the list.
	 */
	private boolean showLinkedOnly = false;

	/** The currently selected element category, or "ALL" -- bound to the
	 * drop-down menu above the element list.  This will always be uppercase
	 * and be either an actual ScriptElementType name(), or "ALL". */
	private String category = INITIAL_CATEGORY;

	/** The category selected by the user when adding a new RWE. */
	private String addCategory;

	/** The list of elements currently displayed. */
	private List<RealWorldElement> realElementList;

	/** The drop-down selection list for element type (or "All"). */
	private List<SelectItem> realElementTypeDL;

	/** The drop-down selection list for element type in the Add RWE
	 * dialog.  This list may be restricted based on the user's permissions. */
	private List<SelectItem> allowedNewTypeDL;

	/** The currently viewed Real World Element. */
	private RealWorldElement realElement;

	/** The element name when it was set up for display.  We use this when the
	 * project is saved, to see if it's changed and needs to be check for
	 * duplicating another element's name. */
	private String originalName;

	/** The SelectItem list of contacts for the "responsible party"
	 * drop-down.  Includes a "select contact" entry at the beginning.  */
	private List<SelectItem> contactDL;

	private boolean showElemLink = false;

	/** The database id of the realLink selected for removal by the user
	 * clicking the "X" next to it in the "Linked To/played by" list. */
	private Integer removeLinkId;

	/** The name field of an item being removed; this is only used in situations where the
	 * user has just added a link, which will not yet have an id (until it is saved).
	 */
	private String removeName;

	/** The database id of the blackout dateRange selected for removal by the user
	 * clicking the "X" next to it in the "Blackout Dates" list. */
	private Integer removeBlackoutId;

	/** The database id of the POI selected for removal by the user
	 * clicking the "X" next to it in the "Blackout Dates" list. */
	private Integer removeInterestId;

	/** The id of the RealWorldElement's responsible party. Held separately to simplify management
	 * of the "select contact" drop-down list. */
	private Integer managerId = -1;

	private List<ScriptElement> linkableElements = null;
	private final List<Integer> removedElemLinks = new ArrayList<>();
	private final List<Integer> removedDateRanges = new ArrayList<>();

//	private QuickContactBean quickContact;

	/** If true, a message is generated within the current pop-up dialog box. */
	private boolean msgAdded = false;

	/** If true, display the "Add RWE" pop-up dialog. */
	private boolean showAdd = false;

	/** if true, display the "View Map" pop-up */
	private boolean showViewMap = false;

	/** if true, display the "Add Map" pop-up */
	private boolean showAddMap = false;
	/** The backing bean for the Add Image dialog for "Add Map". */
	private ImageAddBean mapImageAddBean;
	/** Holds map image added by user; this is a Set so that it
	 * works with ImageAddBean, even though only a single map image is
	 * supported. */
	private Set<Image> addMapImages;
	/** The database id of the element's map image if it was deleted
	 * by the user.  Hold the value until a Save is issued, when the
	 * actual delete from the database is done. */
	private Integer deletedMapId;

	/** if true, display the "Add Blackout Date" pop-up */
	private boolean showBlackout = false;
	/** Start date for the "Add Blackout" pop-up */
	private Date blackoutStartDate = new Date();
	/** End date for the "Add Blackout" pop-up */
	private Date blackoutEndDate = new Date();
	/** 'Reason' for the "Add Blackout" pop-up */
	private String blackoutReason = "";

	/** If true, display the "add points of interest" dialog. */
	private boolean showInterest = false;
	private List<PointOfInterest> pointsOfInterest = null;

	private Integer addId;
	//private Integer lastId;

	private transient RealWorldElementDAO realWorldElementDAO;
	private transient ScriptElementDAO scriptElementDAO;
	private transient PointOfInterestDAO pointOfInterestDAO;


	/* Constructor */
	public RealElementBean() {
		super(RealWorldElement.SORTKEY_NAME, "RealElement.");
		LOG.debug("Initializing");
		try {
			AuthorizationBean auth = AuthorizationBean.getInstance();
			Contact user = SessionUtils.getCurrentContact();
			showNonLocation = auth.hasPermission(user, user.getProject(), Permission.VIEW_RW_ELEMENTS);
			editNonLocation = auth.hasPermission(user, user.getProject(), Permission.EDIT_RW_ELEMENTS_OTHER);
			showCast = auth.hasPermission(user, user.getProject(), Permission.VIEW_CONTACTS_CAST);
			editCast = auth.hasPermission(user, user.getProject(), Permission.EDIT_CONTACTS_CAST);
			editLocation = auth.hasPermission(user, user.getProject(), Permission.EDIT_RW_ELEMENTS_LOCATIONS);

			if (showNonLocation) {
				realElementTypeDL = new ArrayList<>( EnumList.getRealWorldElementTypeList() );
				if (! showCast) {
					realElementTypeDL.remove(ScriptElementType.CHARACTER.ordinal()); // take out "Cast" entry
				}
				realElementTypeDL.add(0, Constants.GENERIC_ALL_ITEM);
			}
			else {
				realElementTypeDL = new ArrayList<>();
				realElementTypeDL.add(0, SELECT_LOCATION);
			}

			String defaultCat = (showNonLocation ? Constants.CATEGORY_ALL : ScriptElementType.LOCATION.name());
			String cat = SessionUtils.getString(Constants.ATTR_RW_CATEGORY, defaultCat);
			category = cat;
			changeCategory(getCategory(), false);
			checkTab(); // restore last selected mini-tab
			restoreSortOrder();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		LOG.debug("");

		try {
			Integer id = SessionUtils.getInteger(Constants.ATTR_RW_ELEMENT_ID);
			setupSelectedItem(id);
			scrollToRow();
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
		if (realElement != null) {
			realElement.setSelected(false);
			updateItemInList(realElement);
		}
		if (id != null && ! id.equals(NEW_ID)) {
			realElement = getRealWorldElementDAO().findById(id);
			if (realElement == null) { // last viewed item was deleted
				id = null;	// force lookup of new default item to view
			}
		}
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_RW_ELEMENT_ID, null);
			realElement = null;
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			SessionUtils.put(Constants.ATTR_RW_ELEMENT_ID, id);
			setShowAddLink(null); // lazy initialized if needed
			realElement = getRealWorldElementDAO().findById(id);
			if (realElement != null) { // null would be very odd, e.g., another user deleted it
				if ((getSelectedTab() == TAB_MANAGER || getSelectedTab() == TAB_MAP)
						&& getElement().getType() != ScriptElementType.LOCATION) {
					setSelectedTab(TAB_DETAIL);
				}
				if (! getCategory().equals(Constants.CATEGORY_ALL) &&
						! getCategory().equals(realElement.getType().name())) {
					changeCategory(realElement.getType().name(), true);
				}
				if (realElement.getType() != ScriptElementType.CHARACTER) {
					setShowAddLink(true);
				}
			}
		}
		else {
			LOG.debug("new RWE");
			SessionUtils.put(Constants.ATTR_RW_ELEMENT_ID, null); // erase "new" flag
			realElement = null;
			ScriptElementType type = ScriptElementType.valueOf(addCategory);
			if ( ! getCategory().equals(Constants.CATEGORY_ALL) &&
					! getCategory().equals(type.name())) {
				changeCategory(type.name(), false);
				// do changeCategory() while realElement is null to prevent it
				// from trying to pick a new realElement to display!
			}
			realElement = new RealWorldElement();
			realElement.setProduction(SessionUtils.getProduction());
			realElement.setType(type);
//			if (type == ScriptElementType.CHARACTER) {
//				// * Creating a new Character element is no longer allowed. *
//				Contact contact = ContactViewBean.initNew();
//				contact.setDepartment(DepartmentDAO.getInstance().findById(Constants.DEPARTMENT_ID_CAST));
//				realElement.setActor(contact);
//			}
			setShowAddLink(true);
			setSelectedTab(TAB_DETAIL);
		}
		removedElemLinks.clear();
		removedDateRanges.clear();
		pointsOfInterest = null;
		resetImages();
		deletedMapId = null;
		addMapImages = null;
		setRemoveLinkId(null);
		setLinkableElements(null);
		if (realElement != null) {
			originalName = realElement.getName();
			realElement.setSelected(true);
			RowState state = new RowState();
			state.setSelected(true);
			getStateMap().put(realElement, state);
			if (realElement.getAddress() == null) {
				realElement.setAddress(new Address());
			}
			setupManager();
			forceLazyInit();
		}
	}

	private void setupManager() {
		if (realElement.getManager() == null) {
			setManagerId(-1);
		}
		else {
			setManagerId(realElement.getManager().getId());
		}
	}

	/**
	 * Find the id of some RWE to display, usually the first in the current
	 * list.
	 *
	 * @return The database id of a RealWorldElement to display; will return
	 *         null if the list of items is empty.
	 */
	@SuppressWarnings("unchecked")
	private Integer findDefaultId() {
		Integer id = null;
		List<RealWorldElement> list = getItemList();
		if (list.size() > 0) {
			id = list.get(0).getId();
		}
		return id;
	}

	private void forceLazyInit() {
		LOG.debug(getSelectedTab());
		@SuppressWarnings("unused")
		String str;
		try {
			if (getElement().getAddress() != null) {
				str = getElement().getAddress().getAddrLine1();
			}
			if (getElement().getActor() != null) {
				str = getElement().getActor().getUser().getDisplayName();
			}
			if (realElement.getManager() != null) {
				str = realElement.getManager().getUser().getDisplayName();
			}
			if (realElement.getRealLinks() != null) {
				for (RealLink rl : realElement.getRealLinks()) {
					ScriptElement se = rl.getScriptElement();
					str = se.getProject().getTitle();
				}
			}
			if (realElement.getDateRanges() != null) {
				for (DateRange dr : realElement.getDateRanges()) {
					@SuppressWarnings("unused")
					Date d = dr.getStartDate();
				}
			}
			if (realElement.getPointOfInterests() != null) {
				for (PointOfInterest poi : realElement.getPointOfInterests()) {
					str = poi.getName();
					if (poi.getAddress() != null) {
						str = poi.getAddress().getAddrLine1();
					}
				}
			}
			if (realElement.getMap() != null) {
				realElement.getMap().getContent();
			}
			if (realElement.getImages() != null) {
				for (Image image : realElement.getImages()) {
					str = image.getTitle();
				}
				realElement.getImageResources();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((RealWorldElement)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Action method for Edit button; convert multi-line text fields for
	 * proper display in edit mode.
	 *
	 * @see com.lightspeedeps.web.view.ListView#actionEdit()
	 */
	@Override
	public String actionEdit() {
		LOG.debug("");
		realElement.setParking(StringUtils.editHtml(realElement.getParking()));
		realElement.setPermitData(StringUtils.editHtml(realElement.getPermitData()));
		realElement.setSpecialInstructions(StringUtils.editHtml(realElement.getSpecialInstructions()));
		return super.actionEdit();
	}

	/**
	 *  Save RealWorldElement - action method when user clicks "Save" on edit page.
	 */
	@Override
	public String actionSave() {
		boolean doodDirty = false;
		showAddMap = false;
		try {
			if (realElement.getRate() != null && realElement.getRate().signum() < 0) {
				MsgUtils.addFacesMessage("RealElement.NegativeRate", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(TAB_DETAIL);
				return null;
			}
			if (realElement.getDirections() != null && realElement.getDirections().length() > RealWorldElement.DIRECTIONS_MAX_LENGTH) {
				MsgUtils.addFacesMessage("RealElement.DirectionsTooLong", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(TAB_MAP);
				return null;
			}
			if (realElement.getActor() != null) {
				Contact contact = realElement.getActor();
				boolean b = ContactViewBean.checkName(contact);
				if ( ! b) {
					setSelectedTab(TAB_DETAIL);
					return null;
				}
				realElement.setName(contact.getUser().getLastNameFirstName());
			}
			else if (getElement().getName() == null || getElement().getName().trim().length() == 0) {
				MsgUtils.addFacesMessage("RealElement.BlankName", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(TAB_DETAIL);
				return null;
			}
			else {
				getElement().setName(getElement().getName().trim());
				if ( ( ! getElement().getName().equalsIgnoreCase(originalName) ) &&
						(getRealWorldElementDAO().findByNameType(getElement().getName(), getElement().getType()) != null)) {
					MsgUtils.addFacesMessage("RealElement.DuplicateName", FacesMessage.SEVERITY_ERROR);
					setSelectedTab(TAB_DETAIL);
					return null;
				}
				originalName = getElement().getName();
			}

			// convert multi-line text field line breaks to html <br/>
			realElement.setParking(StringUtils.saveHtml(realElement.getParking()));
			realElement.setPermitData(StringUtils.saveHtml(realElement.getPermitData()));
			realElement.setSpecialInstructions(StringUtils.saveHtml(realElement.getSpecialInstructions()));

			realElement.setDirections(StringUtils.convertBoldItalic(realElement.getDirections()));

			realElement.setProduction(SessionUtils.getProduction());
			if (managerId == -1 ||
					(realElement.getManager() != null && realElement.getManager().getId() == null)) {
				realElement.setManager(null);	// clear out placeholder
			}
			checkSaveAddress();
			setLinkableElements(null);
			for (RealLink rlink : getElement().getRealLinks()) {
				if (rlink.getId() != null && rlink.getId() < 0) { // temporary value
					rlink.setId(null);
					doodDirty = true;
				}
			}
			for (DateRange date : getElement().getDateRanges()) {
				if (date.getId() != null && date.getId() < 0) { // temporary value
					date.setId(null);
				}
			}

			if (addMapImages != null && addMapImages.size() > 0) {
				realElement.setMap(addMapImages.iterator().next()); // saved via cascade
				LOG.debug("map set=" + realElement.getMap());
			}
			addMapImages = null;

			commitImages();
			if (!newEntry) {
				if (removedElemLinks.size() > 0) {
					doodDirty = true;
				}
				else if (getElement().getRealLinks() != null &&
						getElement().getRealLinks().size() > 0) {
					// status values may have changed
					doodDirty = true;
				}
				Set<ScriptElement> checkElements = updateSubItems(); // realLinks, DateRanges, and element
				if (deletedMapId != null) {
					ImageDAO imageDAO = ImageDAO.getInstance();
					Image map = imageDAO.findById(deletedMapId);
					if (map != null) {
						imageDAO.delete(map);
					}
				}
				deletedMapId = null;
				updateItemInList(getElement());
				getElement().setSelected(true);
				forceLazyInit();
				updateScriptElements(checkElements); // update requirement-satisfied flags
			}
			else {
				RealLinkDAO realLinkDAO = RealLinkDAO.getInstance();
				if (realElement.getActor() != null) {
					Contact contact = realElement.getActor();
					contact.setEmailAddress(ContactViewBean.generateEmailAddress(contact));
					realLinkDAO.attachDirty(realElement.getActor()); // saves User via cascade
				}
				realElement = getRealWorldElementDAO().update(realElement, getAddedImages());
				for (RealLink rlink : getElement().getRealLinks()) {
					realLinkDAO.save(rlink);
					doodDirty = true;
				}
				if (realElement.getManager() != null) {
					Contact c = realElement.getManager();
					c = ContactDAO.getInstance().refresh(c);
					realElement.setManager(c);
				}
				updateScriptElements(null); // update requirement-satisfied flags
				RealWorldElement relem = realElement;
				refreshList(); // to show new entry
				if (realElement == null && showLinkedOnly) {
					// just added new element - won't show cause it's not linked, and no other elements in list
					showLinkedOnly = false; // let it show.
					realElement = relem; // try & keep it selected
					refreshList(); // to show new entry
				}
				scrollToRow();	// scroll list to show this one
			}
			if (doodDirty) {
				ProjectDood.markDirty(realElement);
			}
			SessionUtils.put(Constants.ATTR_RW_ELEMENT_ID, realElement.getId());
			pointsOfInterest = null; // get a fresh version if we need it.

			// Force refresh of the imageResources list.
			realElement.setImageResources(null);

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
			if (addMapImages != null && addMapImages.size() > 0) { // user added a map
				showAddMap = true;	// so rollback will do map image
				ImageAddBean.rollback(this); // roll it back (delete from db)
				addMapImages = null;
			}
			showAddMap = false;	// so superclass cancel handles regular images
			super.actionCancel();
			if (getElement() == null || isNewEntry()) {
				realElement = null;
				changeCategory(getCategory(), true); // select 1st item in current list
				//setupSelectedItem(lastId);
			}
			else {
				realElement = getRealWorldElementDAO().refresh(realElement);
				setupSelectedItem(getElement().getId());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
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
			if (getShowBlackout()) {
				res = actionCancelBlackout();
			}
			else if (getShowElemLink()) {
				res = actionCloseScriptLink();
			}
			else if (getShowInterest()) {
				res = actionCloseInterest();
			}
			else if (getShowAdd()) {
				res = actionCancelAdd();
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

	/* Delete And Navigate To Script Element List */
	@Override
	public String actionDeleteOk() {
		try {
			realElement = getRealWorldElementDAO().refresh(realElement); // refresh
			getRealWorldElementDAO().remove(realElement);
			SessionUtils.put(Constants.ATTR_RW_ELEMENT_ID, null);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		realElementList.remove(realElement);
		realElement = null;
		setupSelectedItem(getRowId(getSelectedRow()));
		addClientResizeScroll();
		return null;
	}

	@Override
	public String actionNew() {
		LOG.debug("");
		try {
			if (editMode) {
				PopupBean.getInstance().show(
						null, 0,
						getMessagePrefix()+"AddSaveFirst.Title",
						getMessagePrefix()+"AddSaveFirst.Text",
						"Confirm.OK",
						null); // no cancel button
//				addClientResize();
				return null;
			}
			showAdd = true;
			if (realElement != null) {
				//lastId = realElement.getId();
				setAddCategory(realElement.getType().name());
			}
			else if ( ! getCategory().equals(Constants.CATEGORY_ALL)) {
				setAddCategory(getCategory());
			}
			else {
				setAddCategory(ScriptElementType.PROP.name());
			}
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the Cancel button on the "Add New Production Element"
	 * pop-up dialog.
	 * @return null navigation string
	 */
	public String actionCancelAdd() {
		showAdd = false;
		addClientResizeScroll();
		return null;
	}

	public String actionSetupAdd() {
		LOG.debug("");
		showAdd = false;
		newEntry = true;
		editMode = true;
		try {
			setupSelectedItem(NEW_ID);
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Delete a link to a RealWorldElement (a RealLink). This is an Action method on a
	 * 'delete' icon in the "linked real elements" table on the edit page. (This method
	 * previously just put up the confirmation dialog.)  The 'removeLinkId' value has been set
	 * via an f:setPropertyActionListener tag in the JSP.
	 */
	public String actionRemoveScriptLink() {
/*		try {
			log.debug("id=" + removeLinkId + ", name=" + removeName);
			PopupBean conf = PopupBean.getInstance();
			conf.show(
					this, ACT_REMOVE_SCRIPT_LINK,
					"RealElement.RemoveLink.Title",
					"RealElement.RemoveLink.Ok",
					"Confirm.Cancel");
			String msg = MsgUtils.formatMessage("RealElement.RemoveLink.Text",
					realElement.getName(), removeName);
			conf.setMessage(msg);
			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
*/
		return actionRemoveScriptLinkOk();
	}

	/**
	 * Delete a link to a RealWorldElement (a RealLink). (This was called when
	 * the user confirms deletion, via the confirmOk() method.)
	 */
	public String actionRemoveScriptLinkOk() {
		try {
			LOG.debug("removing: "+removeLinkId);
			if (removeLinkId != null) {
				if (removeLinkId > 0) { // (will be negative if link was just added and not saved)
					removedElemLinks.add(removeLinkId);
				}
				boolean removeR = false; // could remove debug code for production
				// Remove the link from the set; can't just use Set.remove(removeLink) because we need
				// to do a custom comparison.
				Iterator<RealLink> ri = getElement().getRealLinks().iterator();
				while (ri.hasNext()) {
					RealLink r = ri.next();
					if (r.getId() != null && r.getId().equals(removeLinkId)) {
						getElement().getRealLinks().remove(r);
						getElement().setRealLinks(getElement().getRealLinks()); // force refresh of transients
						removeR = true; // for debugging
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

	public String actionOpenScriptLink() {
		showElemLink = true;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method from the "Cancel" button on the element-link
	 * pop-up dialog; just closes the pop-up.
	 * @return null navigation string
	 */
	public String actionCloseScriptLink() {
		LOG.debug("");
		showElemLink = false;
		addClientResizeScroll();
		return null;
	}

	/**
	 * ActionListener method from one of the "Save" buttons on the element-link
	 * pop-up dialog.  Note that we don't actually update the database here,
	 * that happens (via cascades) when the user hits Save on the Edit
	 * page and we do an attachDirty of the active realElement.
	 */
	public String actionAddScriptLink() {
		try {
			LOG.debug(addId);
			if (addId != null) {
				ScriptElement elem = getScriptElementDAO().findById(addId);
				if (elem != null) {
					RealLink rlink = new RealLink(elem, realElement);
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

//	/**
//	 * Action method to open the "Create contact" pop-up dialog for setting the manager field.
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
//	 * is saved successfully, set it as the current element's manager.
//	 */
//	public String actionSaveQuickAdd() {
//		log.debug("");
//		try {
//			if ( quickContact.save() ) {
//				realElement.setManager(quickContact.getContact());
//				contactDL = null; // force update of contact list
//				setupManager();
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
	 * Action method for New Image button.  {@link ListImageView} handles this, but
	 * we have to make sure our Add Map flag is off first.
	 * @see com.lightspeedeps.web.view.ListImageView#actionOpenNewImage()
	 */
	@Override
	public String actionOpenNewImage() {
		showAddMap = false;
		return super.actionOpenNewImage();
	}

	/**
	 * Called from {@link ImageAddBean} when a new image has been uploaded and
	 * is ready to be saved with the current element.
	 */
	@Override
	public void updateImage(Image image, String filename) {
		LOG.debug("");
		try {
			if (showAddMap) { // Map image
				if (image != null) {
					actionDeleteMapOk();
					realElement.setMap(image);
				}
				LOG.debug("image=" + image);
			}
			else { /** standard RW Element image - let {@link ListView} handle it */
				super.updateImage(image, filename);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/** This is a bit tricky, since we're using the {@link ImageAddBean} in two ways --
	 * for normal RWE images, and for the Map image.
	 * See {@link #addedImages}. */
	@Override
	public Set<Image> getAddedImages() {
		if (showAddMap) {
			return addMapImages;
		}
		return addedImages;
	}
	/** See {@link #getAddedImages}. */
	@Override
	public void setAddedImages(Set<Image> images) {
		if (showAddMap) {
			addMapImages = images;
		}
		else {
			addedImages = images;
		}
	}

	/**
	 * Action method for the "View map" image link.
	 * @return null navigation string
	 */
	public String actionOpenViewMap() {
		showViewMap = true;
//		addClientResize();
		return null;
	}

	/**
	 * Action method for the "Add/create map" legend button.  Get the
	 * Image Add bean instance and use it to open the dialog.
	 * @return null navigation string
	 */
	public String actionOpenAddMap() {
		String res = null;
		try {
			showAddMap = true;
			if (mapImageAddBean == null) {
				mapImageAddBean = ImageAddBean.getInstance();
			}
			mapImageAddBean.setAutoCommit(false);
			mapImageAddBean.setForMap(true);
			res = mapImageAddBean.actionOpenNewImage(this, getElementName());
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return res;
	}

	/**
	 * Delete the current Map. This is an Action method called by the Delete
	 * icon in the Manage /Map area. Here we just put up the confirmation
	 * dialog.
	 */
	public String actionDeleteMap() {
		try {
			PopupBean.getInstance().show(
				this, ACT_DELETE_MAP, "Image.Delete.");
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method called (via confirmOk) to delete the current
	 * Map image for the element.
	 * @return null navigation string
	 */
	public String actionDeleteMapOk() {
		try {
			if (addMapImages != null && addMapImages.size() > 0) {
				// current map was just added and not yet saved
				addMapImages = null;
			}
			else { // have a persistent Map Image
				if (deletedMapId == null && realElement.getMap() != null) {
					deletedMapId = realElement.getMap().getId();
					realElement.setMap(null);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	public String actionOpenBlackout() {
		showBlackout = true;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Add a new black-out date range to the element. This is the Action method for the Save button
	 * on the pop-up dialog box. The entry won't be saved (via cascade) until the user clicks Save
	 * on the Edit page.
	 *
	 * @return null navigation string
	 */
	public String actionSaveBlackout() {
		try {
			LOG.debug("start=" + blackoutStartDate + ", end=" + blackoutEndDate + ", reason=" + blackoutReason);
			if (blackoutStartDate.after(blackoutEndDate)) {
				MsgUtils.addFacesMessage("RealElement.DateOrder", FacesMessage.SEVERITY_ERROR);
			}
			else {
				showBlackout = false;
				DateRange dateRange = new DateRange();
				dateRange.setId(0-(int)(Math.random()*10000.0)); // negative value to track unsaved links
				dateRange.setStartDate(blackoutStartDate);
				dateRange.setEndDate(blackoutEndDate);
				dateRange.setDescription(blackoutReason);
				dateRange.setRealWorldElement(realElement);
				realElement.getDateRanges().add(dateRange);
			}
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	public String actionCancelBlackout() {
		LOG.debug("");
		showBlackout = false;
		addClientResizeScroll();
		return null;
	}

	public String actionCancelViewMap() {
		LOG.debug("");
		showViewMap = false;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Delete a blackout date range. This is an Action method on a 'delete' icon in the
	 * "blackout dates" table on the edit page. This method just puts up the confirmation dialog.
	 * The 'removeBlackoutId' value has been set via an f:setPropertyActionListener tag in the JSP.
	 */
	public String actionRemoveBlackout() {
		try {
			LOG.debug(removeBlackoutId);
			PopupBean.getInstance().show(
					this, ACT_REMOVE_BLACKOUT,
					"RealElement.RemoveBlackout.");
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 *  Delete a Black-out Date Range from element.  Note that the database
	 *  won't be updated until the user hits Save and we attach/merge the
	 *  modified realWorldElement.
	 */
	public String actionRemoveBlackoutOk() {
		try {
			if (removeBlackoutId != null) {
				if (removeBlackoutId > 0) { // (will be negative if link was just added and not saved)
					removedDateRanges.add(removeBlackoutId);
				}
				boolean removed = false;
				for (Iterator<DateRange> iter = getElement().getDateRanges().iterator(); iter.hasNext(); ) {
					DateRange date = iter.next();
					if (date.getId() != null && date.getId().equals(removeBlackoutId)) {
						iter.remove();
						removed = true; // for debugging
						break;
					}
				}
				LOG.debug("removed=" + removed);
			}
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Delete a linked POI. This is an Action method on a 'delete' icon in the
	 * "Points of Interest" table on the edit page. (Previously, this method
	 * just put up the confirmation dialog.) The 'removeInterestId' value has
	 * been set via an f:setPropertyActionListener tag in the JSP.
	 */
	public String actionRemoveInterest() {
/*		try {
			PopupBean.getInstance().show(
					this, ACT_REMOVE_INTEREST,
					"RealElement.RemoveInterest.");
			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
*/

		return actionRemoveInterestOk();
	}

	/**
	 * Remove a linked PointOfInterest object from the current element.
	 * @return Empty string (navigation).
	 */
	public String actionRemoveInterestOk() {
		LOG.debug(removeInterestId);
		try {
			PointOfInterest poi = getPointOfInterestDAO().findById(removeInterestId);
			if (poi != null) {
				boolean removed = realElement.getPointOfInterests().remove(poi);
				LOG.debug("remove=" + removed);
			}
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	//  * * * Add Point of Interest pop-up * * *

	public List<PointOfInterest> createPointsOfInterest() {
		LOG.debug("");
		if (realElement == null) {
			return new ArrayList<>();
		}
		List<PointOfInterest> points = null;
		try {
			points = getPointOfInterestDAO().findAllProductionOrdered();
			for (PointOfInterest poi : realElement.getPointOfInterests()) {
				points.remove(poi);
			}
			//Collections.sort(points);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return points;
	}

	/**
	 * Add a new point of interest to the element.  This is the Action
	 * method for the Add buttons on the pop-up Add Interest dialog box.
	 * The added entries won't be saved until the user clicks Save on the Edit
	 * page.
	 */
	public String actionAddInterest() {
		try {
			LOG.debug(addId);
			if (addId != null) {
				PointOfInterest point = getPointOfInterestDAO().findById(addId);
				if (point != null) {
					realElement.getPointOfInterests().add(point);
					Collections.sort(realElement.getPointOfInterests());
					if (realElement.getPointOfInterests().size() >= 4) {
						showInterest = false; // close the pop-up
					}
					else {
						pointsOfInterest.remove(point); // update selection list in pop-up
					}
				}
			}
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * The Action method for the "Add point of interest" button.  Opens
	 * the dialog box where the user may choose points of interest to be
	 * linked to the current RWE.
	 * @return null navigation string
	 */
	public String actionOpenInterest() {
		LOG.debug("");
		try {
			if (getElement().getPointOfInterests().size() >= 4) {
				MsgUtils.addFacesMessage("RealElement.MaxPoiLinked", FacesMessage.SEVERITY_INFO);
			}
			else {
				showInterest = true;
				addClientResizeScroll();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * The Action method for the Close button on the Add Point of Interest
	 * pop-up dialog box; just closes the pop-up.
	 * @return null navigation string
	 */
	public String actionCloseInterest() {
		LOG.debug("");
		showInterest = false;
		addClientResizeScroll();
		return null;
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
				case ACT_REMOVE_BLACKOUT:
					res = actionRemoveBlackoutOk();
					break;
				case ACT_REMOVE_SCRIPT_LINK:
					res = actionRemoveScriptLinkOk();
					break;
				case ACT_REMOVE_INTEREST:
					res = actionRemoveInterestOk();
					break;
				case ACT_DELETE_MAP:
					res = actionDeleteMapOk();
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
	 * The ValueChangeListener for the "Manager" drop-down list.
	 * Note that the first entry has a value of -1 with a label of "select contact"
	 * or something similar.
	 * @param evt
	 */
	public void changeManager(ValueChangeEvent evt) {
		try {
			if (evt.getNewValue() != null) { // got null value once - how?
				Integer id = Integer.parseInt(evt.getNewValue().toString());
				LOG.debug("id=" + id);
				if (id.intValue() == -1) {
					LOG.debug("clearing manager");
					realElement.setManager(null);
				}
				else {
					Contact contact = ContactDAO.getInstance().findById(id);
					if (contact != null) {
						realElement.setManager(contact);
					}
				}
				setupManager();
			}
			else {
				LOG.warn("null event value? - evt=" + evt);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Value Change Listener for check box selecting whether or not to
	 * "Show script elements for current project only" in "linked-to" list.
	 */
	public void changeShowProjectOnly(ValueChangeEvent event) {
		try {
			showProjectOnly = (Boolean)event.getNewValue();
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ValueChangeListener for check box selecting whether or not to
	 * "show real world elements linked to this project only" at top
	 * of left-hand (element) list.
	 * @param event
	 */
	public void changeShowLinkedOnly(ValueChangeEvent event) {
		try {
			showLinkedOnly = (Boolean)event.getNewValue();
			refreshList();
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
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Set a new category (or "All") as the type of item listed. This regenerates the list of
	 * elements listed. If the currently selected item is not in the new list, then select the first
	 * entry of the new list.
	 *
	 * @param type A ScriptElementType value, or "All".
	 */
	private void changeCategory(String type, boolean pickElement) {
		LOG.debug(type);
		if (getElement() != null && ! editMode) {
			getElement().setSelected(false); // we may end up switching
		}
		if ( ! showNonLocation) { // only locations may be viewed
			type = ScriptElementType.LOCATION.name();
		}
		SessionUtils.put(Constants.ATTR_RW_CATEGORY, type);
		category = type;
		if ( ! type.equals(Constants.CATEGORY_ALL)) {
			ScriptElementType t = ScriptElementType.valueOf(type);
			if (showLinkedOnly) {
				realElementList = getRealWorldElementDAO().findByTypeAndProject(t, SessionUtils.getCurrentProject());
			}
			else {
				realElementList = getRealWorldElementDAO().findByProductionAndType(t);
			}
		}
		else {
			if (showCast) {
				if (showLinkedOnly) {
					realElementList = getRealWorldElementDAO().findByProject(SessionUtils.getCurrentProject());
				}
				else {
					realElementList = getRealWorldElementDAO().findByProduction();
				}
			}
			else { // "ALL" selected, but user does not have "view cast" permission
				if (showLinkedOnly) {
					realElementList = getRealWorldElementDAO().findByNotTypeAndProject(ScriptElementType.CHARACTER, SessionUtils.getCurrentProject());
				}
				else {
					realElementList = getRealWorldElementDAO().findByNotType(ScriptElementType.CHARACTER);
				}
			}
		}
		setSelectedRow(-1);
		doSort();	// the new list may have been previously sorted by some other column
		if (pickElement) { // possibly select an element to view
			@SuppressWarnings("unchecked")
			List<RealWorldElement> list = getItemList();
			if (getElement() != null) {
				int ix = list.indexOf(realElement);
				if (ix < 0) {
					LOG.debug(type + ", " + realElement.getType());
					if (list.size() > 0) {
						setupSelectedItem(list.get(0).getId());
					}
					else {
						setupSelectedItem(null);
					}
				}
				else {
					realElement = list.get(ix);
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

	private void checkSaveAddress() {
		Address addr;
		if ( (addr = realElement.getAddress()) != null) {
			if (addr.getAddrLine1() != null && addr.getCity() != null) {
				addr = AddressDAO.getInstance().merge(addr);
				realElement.setAddress(addr);
			}
			else {
				realElement.setAddress(null);
			}
		}
	}

	/**
	 * Process any "removed" links to script elements that occurred during the
	 * edit session, any removed Date Ranges, and any added Images, and all
	 * other changes to the RW Element.
	 */
	private Set<ScriptElement> updateSubItems() {
		Set<ScriptElement> checkElements = getRealWorldElementDAO().updateSubItems(realElement,
				removedElemLinks, removedDateRanges);
		realElement = getRealWorldElementDAO().update(realElement, getAddedImages()); // images & all else.
		if (realElement.getActor() != null) {
			realElement.getActor().setCastMember(realElement);
			getRealWorldElementDAO().attachDirty(realElement.getActor()); // first/last name changes
		}
		return checkElements;
	}

	/**
	 * We need to check the "requirement satisfied" status of any ScriptElement that we are
	 * currently linked to, plus any that were unlinked during the last edit/save cycle.
	 *
	 * @param checkElements The (possibly empty) set of ScriptElements that were removed (un-linked)
	 *            from our Real element.
	 */
	private void updateScriptElements(Set<ScriptElement> checkElements) {
		if (checkElements == null) {
			checkElements = new HashSet<>();
		}
		if (realElement.getRealLinks() != null) {
			for (RealLink rl : realElement.getRealLinks()) {
				checkElements.add(rl.getScriptElement());
			}
		}
		for (ScriptElement se : checkElements) {
			getScriptElementDAO().updateRequirementSatisfied(se);
		}
	}

	private List<ScriptElement> createLinkableElements() {
		LOG.debug("");
		if (realElement == null) {
			return new ArrayList<>();
		}

		List<ScriptElement> elems = null;
		try {
			ScriptElementType type = realElement.getType();
			elems = getScriptElementDAO().findByTypeAndProject(type, SessionUtils.getCurrentProject());
			// remove already-linked elements
			for (RealLink rl : realElement.getRealLinks()) {
				ScriptElement elem = rl.getScriptElement();
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

	@Override
	public void removeImage(Image image ) {
		// Call the super method first then refresh the
		// Image Resources list.
		super.removeImage(image);

		// Force refresh of the image resources list.
		realElement.setImageResources(null);
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
		return ((item=getRowItem(row)) == null ? null : ((RealWorldElement)item).getId());
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected Comparator getComparator() {
		Comparator<RealWorldElement> comparator = new Comparator<RealWorldElement>() {
			@Override
			public int compare(RealWorldElement elem1, RealWorldElement elem2) {
				int ret = NumberUtils.compare(elem1.getType().ordinal(), elem2.getType().ordinal());
				if (ret == 0 && getSortColumnName() != null) {
					if (getSortColumnName().equals(RealWorldElement.SORTKEY_START) ||
							getSortColumnName().equals(RealWorldElement.SORTKEY_END)) {
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
						else if (getSortColumnName().equals(RealWorldElement.SORTKEY_START)) {
							ret = CalendarUtils.compare( d1.getFirstWorkDate(), d2.getFirstWorkDate());
						}
						else { // sort column = "End"
							ret = CalendarUtils.compare( d1.getLastWorkDate(), d2.getLastWorkDate());
						}
						if ( ! isAscending() ) {
							ret = 0 - ret;	// swap 1 and -1 return values
							// Note that we do NOT invert non-equal Type compares
						}
					}
					if (ret == 0) {
						ret = elem1.compareTo(elem2);
						if (! isAscending()) {
							ret = 0 - ret;	// swap 1 and -1 return values
						}
					}
				}
				return ret;
			}
		};
		return comparator;
	}

	/**
	 * @return the scriptElement
	 */
	public RealWorldElement getRealElement() {
		return realElement;
	}
	public void setRealElement(RealWorldElement scriptElement) {
		realElement = scriptElement;
	}
	public RealWorldElement getElement() { // to simplify JSP
		return realElement;
	}

	/** Return the current element's name, for use in the title of the Add Image dialog */
	@Override
	public String getElementName() {
		return getElement().getName();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return realElementList;
	}

	public List<SelectItem> getContactDL() {
		if (contactDL == null) {
			try {
				final ContactDAO contactDAO = ContactDAO.getInstance();
				contactDL = contactDAO.createContactSelectList(contactDAO.findCrew(false), true);
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
		}
		return contactDL;
	}

	public String getType() {
		return (getElement().getType() == null ? "" : getElement().getType().name());
	}
	public void setType(String typeStr) {
		getElement().setType(ScriptElementType.valueOf(typeStr));
	}

	/**  */
	public boolean isLayoutOther() {
		boolean b = true;
		if (realElement != null) {
			if (realElement.getType() == ScriptElementType.CHARACTER ||
					realElement.getType() == ScriptElementType.LOCATION) {
				b = false;
			}
		}
		return b;
	}

	public String getManagerLabel() {
		String str = "Manager";
		if (realElement != null && realElement.getType() == ScriptElementType.LOCATION) {
			str = "Site Rep";
		}
		return str;
	}

	/** See {@link #showAdd}. */
	public boolean getShowAdd() {
		return showAdd;
	}
	/** See {@link #showAdd}. */
	public void setShowAdd(boolean showAdd) {
		this.showAdd = showAdd;
	}

	/** See {@link #addCategory}. */
	public String getAddCategory() {
		return addCategory;
	}
	/** See {@link #addCategory}. */
	public void setAddCategory(String addCategory) {
		this.addCategory = addCategory;
	}

	public boolean getShowInterest() {
		return showInterest;
	}
	public void setShowInterest(boolean showInterest) {
		this.showInterest = showInterest;
	}

	public List<PointOfInterest> getPointsOfInterest() {
		LOG.debug("");
		if (pointsOfInterest == null) {
			pointsOfInterest = createPointsOfInterest();
		}
		return pointsOfInterest;
	}

	/** See {@link #showBlackout}. */
	public boolean getShowBlackout() {
		return showBlackout;
	}
	/** See {@link #showBlackout}. */
	public void setShowBlackout(boolean b) {
		showBlackout = b;
	}

	/** See {@link #showViewMap}. */
	public boolean getOpenViewMap() {
		return showViewMap;
	}
	/** See {@link #showViewMap}. */
	public void setOpenViewMap(boolean b) {
		showViewMap = b;
	}

	// * * * Add Real World Element link pop-up * * *

	/** See {@link #blackoutStartDate}. */
	public Date getBlackoutStartDate() {
		return blackoutStartDate;
	}
	/** See {@link #blackoutStartDate}. */
	public void setBlackoutStartDate(Date blackoutStartDate) {
		this.blackoutStartDate = blackoutStartDate;
	}

	/** See {@link #blackoutEndDate}. */
	public Date getBlackoutEndDate() {
		return blackoutEndDate;
	}
	/** See {@link #blackoutEndDate}. */
	public void setBlackoutEndDate(Date blackoutEndDate) {
		this.blackoutEndDate = blackoutEndDate;
	}

	/** See {@link #blackoutReason}. */
	public String getBlackoutReason() {
		return blackoutReason;
	}
	/** See {@link #blackoutReason}. */
	public void setBlackoutReason(String blackoutReason) {
		this.blackoutReason = blackoutReason;
	}

	public boolean getShowElemLink() {
		return showElemLink;
	}
	public void setShowElemLink(boolean b) {
		showElemLink = b;
	}

	public List<ScriptElement> getLinkableElements() {
		if (linkableElements == null) {
			linkableElements = createLinkableElements();
		}
		return linkableElements;
	}
	public void setLinkableElements(List<ScriptElement> linkableElements) {
		this.linkableElements = linkableElements;
	}

	private List<SelectItem> createEditTypeDL() {
		List<SelectItem> list = new ArrayList<>();
		try {
			list.add(0, Constants.GENERIC_ALL_ITEM);
			if (getElement() != null) {
				list.add(1,new SelectItem(getElement().getType().name(),getElement().getType().getRwLabel()));
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return list;
	}

	public List<SelectItem> getRealElementTypeDL() {
		if (editMode) {
			return createEditTypeDL();
		}
		return realElementTypeDL;
	}
	public void setRealElementTypeDL(List<SelectItem> list) {
		realElementTypeDL = list;
	}

	/** This supplies the drop-down list for the right-hand pane when in
	 * Edit mode -- the list of types that the user may select from to change
	 * the type of a new or existing element. */
	public List<SelectItem> getRealElementTypeEditDL() {
		List<SelectItem> list = null;
		try {
			if (realElement.getType() == ScriptElementType.CHARACTER) {
				return getAllowedNewTypeDL();
			}
			list = new ArrayList<>(getAllowedNewTypeDL());
			if (editCast) { // 'allowed' list will still include Cast
				list.remove(ScriptElementType.CHARACTER.ordinal());	// remove 'Cast'/CHARACTER entry
				// ... because you can't change a non-Cast element into a Cast element.
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return list;
	}

	/** See {@link #allowedNewTypeDL}. */
	public List<SelectItem> getAllowedNewTypeDL() {
		// for now, disallow creating Cast objects; was testing 'editCast'.
		try {
			if (allowedNewTypeDL == null) {
//				if (false /* editCast && editLocation && editNonLocation*/) { // full access rights
//					allowedNewTypeDL = EnumList.getRealWorldElementTypeList();
//				}
				if ( ! editNonLocation) { // only Cast and/or Location
					allowedNewTypeDL = new ArrayList<>();
//					if (false/*editCast*/) {
//						allowedNewTypeDL.add(SELECT_CAST);
//					}
					if (editLocation) {
						allowedNewTypeDL.add(SELECT_LOCATION);
					}
				}
				else { // all except possibly Cast or Location
					allowedNewTypeDL = new ArrayList<>(EnumList.getRealWorldElementTypeList());
					for (Iterator<SelectItem> iter = allowedNewTypeDL.iterator(); iter.hasNext(); ) {
						SelectItem si = iter.next();
						if ( ! editLocation &&
								si.getValue().toString().equals(ScriptElementType.LOCATION.name()) ) {
							iter.remove();
						}
						else if ( /*! editCast &&*/
								si.getValue().toString().equals(ScriptElementType.CHARACTER.name()) ) {
							iter.remove();
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return allowedNewTypeDL;
	}

	public String getCategory() {
		return category;
	}
	/** This is only used by the framework, and we need to IGNORE that, because we
	 * may have changed the category during an earlier phase of the life-cycle. */
	public void setCategory(String category) {
		//this.category = category;
	}

	/** See {@link #removeBlackoutId}. */
	public Integer getRemoveBlackoutId() {
		return removeBlackoutId;
	}
	/** See {@link #removeBlackoutId}. */
	public void setRemoveBlackoutId(Integer id) {
		LOG.debug(id);
		removeBlackoutId = id;
	}

	/** See {@link #removeInterestId}. */
	public Integer getRemoveInterestId() {
		return removeInterestId;
	}
	/** See {@link #removeInterestId}. */
	public void setRemoveInterestId(Integer removeInterestId) {
		this.removeInterestId = removeInterestId;
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
		LOG.debug(id);
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

	/** See {@link #managerId}. */
	public Integer getManagerId() {
		return managerId;
	}
	/** See {@link #managerId}. */
	public void setManagerId(Integer responsiblePartyId) {
		managerId = responsiblePartyId;
	}

	/** See {@link #showAddLink}. */
	public boolean getShowAddLink() {
		if (showAddLink == null) {
			if (realElement.getType() != ScriptElementType.CHARACTER) {
				// Non-Character types always get the "Associate" (add link) button
				setShowAddLink(true);
			}
			else {
				Contact c = realElement.getActor();
				// Cast (Character) types only get it if the actor is a project member in Cast or Stunt role
				setShowAddLink(ProjectMemberDAO.getInstance()
						.existsContactAsCastOrStuntInProject(c, SessionUtils.getCurrentProject()));
			}
		}
		return showAddLink;
	}
	/** See {@link #showAddLink}. */
	public void setShowAddLink(Boolean showAddLink) {
		this.showAddLink = showAddLink;
	}

	/** See {@link #showProjectOnly}. */
	public boolean getShowProjectOnly() {
		return showProjectOnly;
	}
	/** See {@link #showProjectOnly}. */
	public void setShowProjectOnly(boolean b) {
		showProjectOnly = b;
	}

	public List<RealLink> getRealLinks() {
		if (realElement == null) {
			return null;
		}
		if (getShowProjectOnly()) {
			return realElement.getRealLinksCurrent();
		}
		else {
			return realElement.getRealLinkList();
		}
	}

	/** See {@link #showLinkedOnly}. */
	public boolean getShowLinkedOnly() {
		return showLinkedOnly;
	}
	/** See {@link #showLinkedOnly}. */
	public void setShowLinkedOnly(boolean showLinkedOnly) {
		this.showLinkedOnly = showLinkedOnly;
	}

	/** See {@link #msgAdded}. */
	public boolean getMsgAdded() {
		return msgAdded;
	}
	/** See {@link #msgAdded}. */
	public void setMsgAdded(boolean msgSceneAdded) {
		msgAdded = msgSceneAdded;
	}

	private PointOfInterestDAO getPointOfInterestDAO() {
		if (pointOfInterestDAO == null) {
			pointOfInterestDAO = PointOfInterestDAO.getInstance();
		}
		return pointOfInterestDAO;
	}

	private RealWorldElementDAO getRealWorldElementDAO() {
		if (realWorldElementDAO == null) {
			realWorldElementDAO = RealWorldElementDAO.getInstance();
		}
		return realWorldElementDAO;
	}

	private ScriptElementDAO getScriptElementDAO() {
		if (scriptElementDAO == null) {
			scriptElementDAO = ScriptElementDAO.getInstance();
		}
		return scriptElementDAO;
	}

}
