package com.lightspeedeps.web.element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.RowState;

//import com.icesoft.faces.component.DisplayEvent;
import com.lightspeedeps.dao.AddressDAO;
import com.lightspeedeps.dao.PointOfInterestDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.Image;
import com.lightspeedeps.model.PointOfInterest;
import com.lightspeedeps.type.PointOfInterestType;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.view.ListImageView;

/**
 * The backing bean for the Point Of Interest List/View/Edit page. Includes both
 * View and Edit code. This class handles most of the features and functions
 * associated with the "Details" mini-tab. The "Images" mini-tab is supported
 * mostly by the {@link ListImageView} superclass. The left-hand list of the page is
 * mostly supported by the {@link com.lightspeedeps.web.view.ListView ListView} superclass.
 */
@ManagedBean
@ViewScoped
public class PointOfInterestBean extends ListImageView implements Serializable {
	/** */
	private static final long serialVersionUID = 6740906149819686432L;

	private static final Log log = LogFactory.getLog(PointOfInterestBean.class);
	private static final int TAB_DETAIL = 0;
	//private static final int TAB_IMAGES = 1;

	private static final PointOfInterestType INITIAL_CATEGORY = PointOfInterestType.OTHER;

	/* Variables*/

	/** The currently selected category, or "All" */
	private String category = INITIAL_CATEGORY.name();

	/** The list of elements currently displayed. */
	private List<PointOfInterest> pointOfInterestList;

	/** The drop-down selection list for type (or "All"). */
	private List<SelectItem> pointOfInterestTypeDL;

	/** The currently viewed item. */
	private PointOfInterest pointOfInterest;

	private transient PointOfInterestDAO pointOfInterestDAO;

	/** Constructor */
	public PointOfInterestBean() {
		super(PointOfInterest.SORTKEY_NAME, "PointOfInterest.");
		log.debug("Init");
		try {
			pointOfInterestTypeDL = new ArrayList<>( EnumList.getPointOfInterestTypeList() );
			pointOfInterestTypeDL.add(0, Constants.GENERIC_ALL_ITEM);

			String cat = SessionUtils.getString(Constants.ATTR_POI_CATEGORY, Constants.CATEGORY_ALL);
			category = cat;
			changeCategory(getCategory(), false);

			Integer id = SessionUtils.getInteger(Constants.ATTR_POINT_OF_INTEREST_ID);
			setupSelectedItem(id);
			scrollToRow();
			checkTab(); // restore last selected mini-tab
			restoreSortOrder();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		if (pointOfInterest != null) {
			pointOfInterest.setSelected(false);
		}
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_POINT_OF_INTEREST_ID, null);
			pointOfInterest = null;
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			pointOfInterest = getPointOfInterestDAO().findById(id);
			if (pointOfInterest == null) {
				id = findDefaultId();
				if (id != null) {
					pointOfInterest = getPointOfInterestDAO().findById(id);
				}
			}
			if ( ! getCategory().equals(Constants.CATEGORY_ALL) &&
					! getCategory().equals(pointOfInterest.getType().name())) {
				changeCategory(pointOfInterest.getType().name(), false);
			}
			SessionUtils.put(Constants.ATTR_POINT_OF_INTEREST_ID, id);
		}
		else {
			log.debug("new POI");
			SessionUtils.put(Constants.ATTR_POINT_OF_INTEREST_ID, null); // erase "new" flag
			pointOfInterest = new PointOfInterest();
			String type = SessionUtils.getString(Constants.ATTR_POI_CATEGORY, PointOfInterestType.OTHER.name());
			if (type.equals(Constants.CATEGORY_ALL)) {
				pointOfInterest.setType(PointOfInterestType.OTHER);
			}
			else {
				pointOfInterest.setType(PointOfInterestType.valueOf(type));
			}
		}
		resetImages();
		if (pointOfInterest != null) {
			pointOfInterest.setSelected(true);
			RowState state = new RowState();
			state.setSelected(true);
			getStateMap().put(pointOfInterest, state);
			if (pointOfInterest.getAddress() == null) {
				pointOfInterest.setAddress(new Address());
			}
			forceLazyInit();
			// Force refresh of image resources list
			pointOfInterest.setImageResources(null);
		}
	}

	@SuppressWarnings("unchecked")
	protected Integer findDefaultId() {
		Integer id = null;
		List<PointOfInterest> list = getItemList();
		if (list.size() > 0) {
			id = list.get(0).getId();
		}
		return id;
	}

	private void forceLazyInit() {
		@SuppressWarnings("unused")
		int i = getElement().getImages().size();

		@SuppressWarnings("unused")
		String str;
		if (getElement().getAddress() != null) {
			str = getElement().getAddress().getAddrLine1();
		}

	}

	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((PointOfInterest)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/** Save PointOfInterest */
	@Override
	public String actionSave() {
		if (getElement().getName() == null || getElement().getName().trim().length() == 0) {
			MsgUtils.addFacesMessage("RealElement.BlankName", FacesMessage.SEVERITY_ERROR);
			setSelectedTab(TAB_DETAIL);
			return null;
		}
		try {
			String poiName = getElement().getName().trim();
			// Make sure we remove and characters that could pose a security risk.
			poiName = ApplicationUtils.fixSecurityRiskForStrings(poiName);
			getElement().setName(poiName);
			commitImages();
			if (!newEntry) {
				pointOfInterest = getPointOfInterestDAO().update(getElement(), getAddedImages());
				updateItemInList(getElement());
				getElement().setSelected(true);
				forceLazyInit();
			}
			else {
				pointOfInterest.setProduction(SessionUtils.getProduction());
				pointOfInterest = getPointOfInterestDAO().update(getElement(), getAddedImages());
				refreshList();
				scrollToRow();
			}
			AddressDAO.getInstance().attachDirty(getPointOfInterest().getAddress());
			SessionUtils.put(Constants.ATTR_POINT_OF_INTEREST_ID, pointOfInterest.getId());
			return super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * The Action method for Cancel button while in Edit mode.
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		super.actionCancel();
		if (getElement() == null || isNewEntry()) {
			pointOfInterest = null;
			changeCategory(getCategory(), true); // select 1st item in current list
			//setupSelectedItem(null);
		}
		else {
			pointOfInterest = getPointOfInterestDAO().refresh(pointOfInterest);
			setupSelectedItem(getElement().getId());
		}
		return null;
	}

	/** Delete selected row from Database And Navigate To List PointOfInterest */
	@Override
	public String actionDeleteOk() {
		try {
			pointOfInterest = getPointOfInterestDAO().findById(pointOfInterest.getId()); // refresh
			getPointOfInterestDAO().remove(pointOfInterest);
			SessionUtils.put(Constants.ATTR_POINT_OF_INTEREST_ID, null);

			pointOfInterestList.remove(pointOfInterest);
			pointOfInterest = null;
			setupSelectedItem(getRowId(getSelectedRow()));
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	public void displayListener(SelectEvent event) {
//		if ( ! event.isVisible()) {
//			return; // tooltip is hidden.. skip it.
//		}
//		Object o = event.getContextValue();
//		log.debug(o);
//		// event's target is the component being hovered, i.e., the panelGroup
//		for (UIComponent child : event.getTarget().getChildren()) {
//			/* if the child's id matches our hard coded "holder" id
//			 * then set the current hover field A to it's value
//			 */
//			if (child.getId().equals("hoverID") == true && child instanceof HtmlOutputText) {
//				HtmlOutputText hot = (HtmlOutputText)child;
//				log.debug(hot.getValue());
//				Integer id = (Integer)hot.getValue();
//				log.debug(id);
//				tooltipPoi = getPointOfInterestDAO().findById(id);
//				return;
//			}
//		}
	}

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
	 * The Value Change Listener for the category (element type) selection
	 * drop-down list on the Details (right-hand) tab.  This is only available
	 * in Edit mode.
	 * @param evt
	 */
	public void changeEditCategory(ValueChangeEvent evt) {
		log.debug("");
		try {
			if (evt.getNewValue() != null) {
				if (! category.equals(Constants.CATEGORY_ALL)) {
					changeCategory( (String)evt.getNewValue(), false );
				}
			}
			else {
				log.warn("Null newValue in category change event: " + evt);
			}
//			addClientResize();
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
				log.warn("Null newValue in category change event: " + evt);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
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
		if (getElement() != null && ! editMode) {
			getElement().setSelected(false); // we may end up switching
		}
		SessionUtils.put(Constants.ATTR_POI_CATEGORY, type);
		category = type;
		if ( ! type.equals(Constants.CATEGORY_ALL)) {
			PointOfInterestType t = PointOfInterestType.valueOf(type);
			pointOfInterestList = getPointOfInterestDAO().findByProductionAndType(t);
		}
		else {
			pointOfInterestList = getPointOfInterestDAO().findByProduction();
		}
		setSelectedRow(-1);
		doSort();	// the new list may have been previously sorted by some other column
		if (pickElement) { // possibly select an element to view
			@SuppressWarnings("unchecked")
			List<PointOfInterest> list = getItemList();
			if (getElement() != null) {
				int ix = list.indexOf(pointOfInterest);
				if (ix < 0) {
					log.debug(type + ", " + pointOfInterest.getType());
					if (list.size() > 0) {
						setupSelectedItem(list.get(0).getId());
					}
					else {
						setupSelectedItem(null); // clear View if nothing in list
					}
				}
				else {
					pointOfInterest = list.get(ix);
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
//		addClientResize();
	}

	@Override
	protected void refreshList() {
		changeCategory(getCategory(), true);
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
		return ((item=getRowItem(row)) == null ? null : ((PointOfInterest)item).getId());
	}

	@Override
	protected Comparator<PointOfInterest> getComparator() {
		Comparator<PointOfInterest> comparator = new Comparator<PointOfInterest>() {
			@Override
			public int compare(PointOfInterest c1, PointOfInterest c2) {
				return c1.compareTo(c2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * @return the pointOfInterest
	 */
	public PointOfInterest getPointOfInterest() {
		return pointOfInterest;
	}
	public void setPointOfInterest(PointOfInterest pointOfInterest) {
		this.pointOfInterest = pointOfInterest;
	}
	public PointOfInterest getElement() {
		return pointOfInterest;
	}

	/** Return the current element's name, for use in the title of the Add Image dialog */
	@Override
	public String getElementName() {
		return getElement().getName();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return pointOfInterestList;
	}

	public String getType() {
		return (getElement().getType() == null ? "" : getElement().getType().name());
	}
	public void setType(String typeStr) {
		getElement().setType(PointOfInterestType.valueOf(typeStr));
	}


	private List<SelectItem> createEditTypeDL() {
		List<SelectItem> list = new ArrayList<>();
		list.add(0, Constants.GENERIC_ALL_ITEM);
		if (getElement() != null) {
			list.add(1,new SelectItem(getElement().getType().name(),getElement().getType().getLabel()));
		}
		return list;
	}

	public List<SelectItem> getPointOfInterestTypeDL() {
		if (editMode) {
			return createEditTypeDL();
		}
		return pointOfInterestTypeDL;
	}

	public String getCategory() {
		return category;
	}
	/** This is only used by the framework, and we need to IGNORE that, because we
	 * may have changed the category during an earlier phase of the life-cycle. */
	public void setCategory(String category) {
		//this.category = category;
	}

	private PointOfInterest tooltipPoi;
	/** See {@link #tooltipPoi}. */
	public PointOfInterest getTooltipPoi() {
		return tooltipPoi;
	}
	/** See {@link #tooltipPoi}. */
	public void setTooltipPoi(PointOfInterest tooltipPoi) {
		this.tooltipPoi = tooltipPoi;
	}

	private PointOfInterestDAO getPointOfInterestDAO() {
		if (pointOfInterestDAO == null) {
			pointOfInterestDAO = PointOfInterestDAO.getInstance();
		}
		return pointOfInterestDAO;
	}

}
