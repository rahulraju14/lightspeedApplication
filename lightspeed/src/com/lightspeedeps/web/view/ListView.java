/**
 * File ListView.java
 */
package com.lightspeedeps.web.view;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIData;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.RowState;
import org.icefaces.ace.model.table.RowStateMap;

import com.lightspeedeps.model.PersistentObject;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.user.UserPrefBean;

/**
 * A class which provides some common page-backing functionality for pages with
 * combinations of List on the left and a tabbed detail view on the right. It
 * has a {@link SortableViewList} that holds the list information, and provides
 * methods for list sorting and row selection. It includes basic functions for
 * adding ({@link #actionNew()}) and deleting {@link #actionDelete()} list
 * entries.
 * <p>
 * Note that sorting of the list is usually done automatically as a result of
 * the JSF code invoking the {@link ListView#getSortedItemList()} method,
 * when it refers to bean.sortedItemList in a dataTable or similar JSF tag.
 */
public abstract class ListView extends View implements Serializable {
	/** */
	private static final long serialVersionUID = 3222389192297757345L;

	private static Log log = LogFactory.getLog(ListView.class);

	public static final int ACT_DELETE_ITEM = 1;
	protected static final int ACT_SETUP_DEFAULT = 2;
	protected static final Integer NEW_ID = -1;

	protected static final String ATTR_SORT_NAME_PREFIX = Constants.ATTR_PREFIX + "sortKey.";
	protected static final String ATTR_SORT_ORDER_PREFIX = Constants.ATTR_PREFIX + "sortAsc.";

	private static final String SCROLL_MAIN_JS = "scrollMainToPos();";
	private static final String SHOW_MAIN_ROW_JS = "showMainRow";

	/** Our {@link SortableList} holder.  We provide delegation methods for the
	 * methods on this object that are required by our subclasses. */
	private final SortableViewList viewList;

	/** The index of the currently selected row. */
	private int selectedRow = -1;

	/** The height (in pixels) of each row in the main list.  This is used
	 * by scrollToRow, when a particular row needs to be brought to the
	 * center (vertically) of the display. */
	protected short rowHeight = 26;

	/** When True, we are editing a NEW object (contact, element, etc), not an existing one. */
	protected boolean newEntry = false;

	private boolean sortInProgress = false;

	/** A prefix string used when generating sort-related attribute names, for example,
	 * the attribute for saving the current sort key.  By default, this is set
	 * to the same value as the View's messagePrefix. */
	private String sortAttributePrefix;

	/** Set the "selected" flag on the specified item to either
	 * true or false.  Used to manage highlighting of the items
	 * in the list. */
	abstract protected void setSelected(Object item, boolean b);

	/** Return the list of items being viewed and managed. */
	@SuppressWarnings("rawtypes")
	public abstract List<Comparable> getItemList();

	/** The RowStateMap instance used to manage the clicked row on the data table */
	private RowStateMap stateMap = new RowStateMap();

	/**
	 * Return a Comparator suitable for comparing two of the items in the list.
	 */
	@SuppressWarnings("rawtypes")
	protected abstract Comparator getComparator();

	/**
	 * Our only constructor. The default sort column name and the message prefix
	 * string must both be supplied.
	 *
	 * @param defaultSortColumn The default sort-key, which determines which
	 *            column in our main list is used to sort the items for their
	 *            initial display. The available sort-key values are usually
	 *            defined as static constants in the model class that
	 *            corresponds to the items in the list.
	 * @param prefix The message id prefix, used by several superclass methods
	 *            that provide standard functions (such as Delete), often with
	 *            dialog boxes. The prefix is used to create a full message id
	 *            which will be looked up in our messageResources.properties
	 *            file. By convention, the supplied prefix should end with a
	 *            period. The string usual reflects the primary type of item or
	 *            function of the page being backed, e.g., "Project." or
	 *            "Contact.".
	 */
	public ListView(String defaultSortColumn, String prefix) {
		super(prefix);
		setSortAttributePrefix(prefix);
		viewList = new SortableViewList(defaultSortColumn, this);
	}

	/**
	 * The Action method of the "Edit" button.
	 *
	 * @return null navigation string (since our View and Edit modes normally
	 *         share the same page).
	 */
	@Override
	public String actionEdit() {
		log.debug("");
		newEntry = false;
		super.actionEdit();
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method for the "Add" button. This is typically overridden by our
	 * subclasses, but we provide some common functionality here.
	 *
	 * @return null navigation string
	 */
	public String actionNew() {
		log.debug("");
		if (editMode) {
			PopupBean.getInstance().show(
					null, 0,
					getMessagePrefix()+"AddSaveFirst.Title",
					getMessagePrefix()+"AddSaveFirst.Text",
					"Confirm.OK",
					null); // no cancel button
//			addClientResize();
			return null;
		}
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
	 * Standard activities for all subclasses when a Save completes.
	 * @return null navigation string
	 */
	@Override
	public String actionSave() {
		super.actionSave();
		newEntry = false;
		sort();	// user may have updated the object's sort value -- name or other.
		return null;
	}

	/**
	 * The Action method for Cancel button while in Edit mode. This method
	 * adds the scrolling Javascript call, intended to keep a selected item
	 * in a ListView's "main list" visible to the user.
	 *
	 * @return navigation string from superclass (typically null).
	 */
	@Override
	public String actionCancel() {
		String res = super.actionCancel();
		addClientResizeScroll();
		return res;
	}

	/**
	 * Action method for the Delete button, which usually deletes the current
	 * 'item' from the list. Here we just put up the confirmation dialog.
	 *
	 * @return null navigation string
	 */
	public String actionDelete() {
		PopupBean.getInstance().show(
				this, ACT_DELETE_ITEM,
				getMessagePrefix()+"Delete.");
		addClientResizeScroll();
		return null;
	}

	/**
	 * An action method which is sometimes invoked by JavaScript "clicking" an
	 * invisible "escape" button when the user presses the Escape key.
	 *
	 * @return null navigation string
	 */
	public String actionEscape() {
		actionEscapeKey();
		return null;
	}

	/**
	 * Handle the default processing for the Escape key being pressed. First we
	 * try and close any open dialog box; if that fails, and the user is in Edit
	 * mode, we simulate hitting the Cancel button.
	 *
	 * @return true if the popup close returns true; or if the popup return
	 *         false and we were in Edit mode. (So a false return indicates the
	 *         popup close returned false and we were NOT in Edit mode.)
	 */
	public boolean actionEscapeKey() {
		boolean ret = PopupBean.actionEscape();
		if (! ret && editMode) {
			actionCancel();
			ret = true;
		}
		return ret;
	}

	/**
	 * RowSelector selectionListener method: invoked when user selects a row in the
	 * List+View page. It is IMPERATIVE that the RowSelector tag include the 'immediate="false"'
	 * attribute for proper operation.  Otherwise, in Edit mode the input fields do not get
	 * refreshed with data from the newly selected object.
	 *
	 * @param evt The selection event, which includes the row number. We don't use this information;
	 *            instead we expect the contact id to be available as a component attribute.
	 */
	@SuppressWarnings("rawtypes")
	public void rowSelected(SelectEvent evt) {
		Integer id = (Integer)evt.getComponent().getAttributes().get("currentId");
		UIData ud = (UIData)evt.getComponent();
		int row = ud.getRowIndex();
		log.debug("row=" + row + ", id=" + id);

		if (row < 0) {
			PersistentObject object;
			object = (PersistentObject) evt.getObject();
			log.debug("Object id selected = "+object.getId());
			if (object != null) {
				row = getItemList().indexOf(object);
				id = object.getId();
				log.debug("row=" + row + ", id=" + id);
			}
		}

		if (getSelectedRow() >= 0 && getSelectedRow() < getItemList().size()) {
			Object item = getItemList().get(getSelectedRow());
			if (item != null) {
				setSelected(item, false);
			}
		}
		if (getSelectedRow() != row) {
			selectedRowChanged();
			if (editMode) {
				actionCancel();
			}
		}
		setSelectedRow(row);
		setupSelectedItem(id);
	}

	@SuppressWarnings("rawtypes")
	public void listenRowClicked(SelectEvent evt) {
		log.debug("<>");
		PersistentObject object;
		object =  (PersistentObject) evt.getObject();
		log.debug("Object id selected = "+object.getId());

		if (getSelectedRow() >= 0 && getSelectedRow() < getItemList().size()) {
			Object item = getItemList().get(getSelectedRow());
			if (item != null) {
				setSelected(item, false);
			}
		}
		if (getSelectedRow() != object.getId()) {
			selectedRowChanged();
			if (editMode) {
				actionCancel();
			}
		}
		setSelectedRow(object.getId());
		setupSelectedItem(object.getId());
	}

	/**
	 * Find the index of the last selected row in the component identified by
	 * the given event. The component should be a list-type object such as
	 * ace:dataTable.
	 *
	 * @param event The SelectEvent provided by the framework.
	 * @return The selected row index found in the request parameters, or -1 if
	 *         the parameter is not found.
	 */
	public static int findSelectedRowParam(SelectEvent event) {
		int selectedIndex = -1;
		try {
			if (event != null) {
				UIData ud = (UIData)event.getComponent();
				FacesContext facesContext = FacesContext.getCurrentInstance();
				Map<String, String> requestMap =
						facesContext.getExternalContext().getRequestParameterMap();
				if (requestMap != null) {
					String baseId = ud.getClientId();
					baseId += Constants.ICEFACES_SELECTED_ROW;
					String selectedIndexStr = requestMap.get(baseId);
					if (selectedIndexStr != null) {
						selectedIndex = Integer.parseInt(selectedIndexStr);
					}
				}
			}
		}
		catch (Exception e) {
			log.error("error: ", e);
		}
		return selectedIndex;
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
		getStateMap().put(entry, state);
		setSelectedRow(entry);
	}

	/**
	 * Prepare to display the item identified by 'id'.
	 * @param id The database id of the item which has been
	 * selected to view.
	 */
	protected void setupSelectedItem(Integer id) {
		// implemented by subclasses if necessary
	}

	/**
	 * Called by our rowSelected() method if the user has clicked on a
	 * row different than the currently selected one.
	 */
	protected void selectedRowChanged() {
		// implemented by subclasses if necessary
	}

	/**
	 * Append a JavaScript call to "scrollMainToPos()" routine.
	 * "scrollMainToPos()" ensures that the left-hand "main" list is positioned
	 * so that the currently selected item is visible.
	 */
	public static void addClientResizeScroll() {
		// The resize() is now added automatically on every render-response by
		// the PageAuthenticatePhaseListener.
		addJavascript(/*RESIZE_JS +*/ SCROLL_MAIN_JS);
	}

	protected void showItemMissing() {
		setSelectedRow(-1);
		PopupBean.getInstance().show(
				this, ACT_SETUP_DEFAULT,
				"List.ItemMissing.Title",
				"List.ItemMissing.Text",
				"Confirm.OK",
				null); // no cancel button
//		addClientResize();
	}

	/**
	 * Typically implemented by subclasses to rebuild whatever list is displayed
	 * as that class' "main list".
	 */
	protected void refreshList() {
	}

	/**
	 * Returns the list to be displayed by the JSP. The list returned will always be in the sort
	 * order controlled by the user clicking on the available sort column headers. The initial sort
	 * is determined by the column name passed to our constructor.
	 */
	@SuppressWarnings("rawtypes")
	public List getSortedItemList() {
		if (getItemList() == null) {
			return null;
		}
		int n = getItemList().size(); // note: may change 'selectedRow' value!
		if (getSelectedRow() >= 0 && getSelectedRow() < n) {
			log.debug("");
			/* TODO DH: Due to multiple calls on this method (getSortedItemList()) from somewhere,
			 setSelected method is called many times. Because of this, the two issues 
			 occurs even if press enter after filling the amount field. */			
			setSelected(getItemList().get(getSelectedRow()), true);
		}
		viewList.sortIfNeeded();
		return getItemList();
	}

	/**
	 * Replace an item in the list with another item that would compare 'equal'
	 * to the existing item. This is typically called when a class updates other
	 * fields in the item that are displayed in the view of the list.
	 *
	 * @param item The new item which will replace the existing item.
	 */
	@SuppressWarnings("rawtypes")
	protected void updateItemInList(Comparable item) {
		SortableList.replaceItemInList(getItemList(), item);
	}

	/**
	 * Set the list's selectedRow field to 'row' (origin 0) after adjusting it
	 * if necessary to fall within the bounds of the list.
	 *
	 * @param row The row number (origin zero) of the item to be found.
	 * @return the item that resides in the given row (origin 0) of the
	 *         currently displayed list; returns null only if the list is empty.
	 */
	protected Object getRowItem(int row) {
		int size = getItemList().size();
		if (size == 0) {
			return null;
		}
		if (row < 0) {
			row = 0;
		}
		if (row >= size) {
			row = size - 1;
		}
		setSelectedRow(row);
		//id = list.get(row).getId();
		return getItemList().get(row);
	}

	/**
	 * If no item is currently selected, find the supplied object in the main
	 * (left-hand) list, and make its row the selected row.
	 */
	protected int setSelectedRow(Object element) {
		int ix = getSelectedRow();
		if (ix < 0 && element != null && getItemList() != null) {
			ix = getItemList().indexOf(element);
			setSelectedRow(ix);
		}
		log.debug(ix);
		return ix;
	}

	/**
	 * If no item is currently selected, find the supplied object in the main
	 * (left-hand) list, and make its row the selected row. In any case, send a
	 * JavaScript command to scroll the list so that the selected row is
	 * visible.
	 */
	protected void scrollToRow(Object element) {
		int ix = setSelectedRow(element);
		if (ix >= 0) {
			scrollToMainRow(ix);
		}
	}

	/**
	 * Send a JavaScript command to scroll the main (left-side) list so that the
	 * 'ix'th row is visible.
	 *
	 * @param ix The index (origin zero) of the row to be scrolled to a visible
	 *            position.
	 */
	protected void scrollToMainRow(int ix) {
		addJavascript(SHOW_MAIN_ROW_JS + "(" + ix + "," + rowHeight + ");");
	}

	/**
	 * Sort our list.  This is called back from the ViewList object.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void sort() {
		if (sortInProgress) {
			return;
		}
		log.debug("sort starting, by="+viewList.getSortColumnName());
		sortInProgress = true;
		try {
			Object selectedItem = null;
			if (getSelectedRow() >= 0) {
				selectedItem = getRowItem(getSelectedRow());
				setSelectedRow(-1);
			}
			List<Comparable> list = getItemList();
			Collections.sort(list, getComparator());
			if (selectedItem != null) {
				int ix = list.indexOf(selectedItem);
				if (ix >= 0) {
					log.debug("new selected row=" + ix);
					setSelectedRow(ix);
					scrollToMainRow(ix);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		log.debug("sort complete");
		sortInProgress = false;
	}

	public void sortIfNeeded() {
		viewList.sortIfNeeded();
	}

	public void doSort() {
		viewList.doSort();
	}

	/**
	 * Determines the default ascending/descending order for the given column name.
	 *
	 * @param sortColumn Name of the column for which the default order is requested.
	 * @return whether sortColumn's default order is ascending or descending.
	 */
	public boolean isDefaultAscending(String sortColumn) {
		return true;	// all columns default to ascending
	}

	/**
	 * @return True if the current sort order for the list is ascending.
	 */
	public boolean isAscending() {
		return viewList.isAscending();
	}
	/**
	 * Set the list's current sort order to ascending (true) or
	 * descending (false).
	 */
	public void setAscending(boolean b) {
		SessionUtils.put(ATTR_SORT_ORDER_PREFIX + getSortAttributePrefix(), b);
		// If the current sort column is the default, it may not be stored in
		// the session yet.  Put it there so restoreSortOrder() can find it.
		SessionUtils.put(ATTR_SORT_NAME_PREFIX + getSortAttributePrefix(), getSortColumnName());
		viewList.setAscending(b);
	}

	/**
	 * @return The name of the column that is currently used as the sort key for
	 *         our list. This interacts with ice:dataTable tags in the JSP via
	 *         the sortColumn="#{beanName.sortColumnName}" attribute.
	 */
	public String getSortColumnName() {
		return viewList.getSortColumnName();
	}

	/**
	 * Set the name of the column that is to be used as the sort key for our
	 * list. This method is called when someone clicks on a sort-enabled column
	 * header, as a reult of the sortColumn="#{beanName.sortColumnName}"
	 * attribute in an ice:dataTable tag. The value passed comes from the <
	 * ice:commandSortHeader columnName="name" ... > value.
	 */
	public void setSortColumnName(String name) {
		SessionUtils.put(ATTR_SORT_NAME_PREFIX + getSortAttributePrefix(), name);
		viewList.setSortColumnName(name);
	}

	public void restoreSortOrder() {
		String col = SessionUtils.getString(ATTR_SORT_NAME_PREFIX + getSortAttributePrefix());
		if (col == null) {
			// sort column not saved in session -- see if saved in User Preferences.
			col = UserPrefBean.getInstance().getString(ATTR_SORT_NAME_PREFIX + getSortAttributePrefix(), null);
		}
		if (col != null ) {
			setSortColumnName(col);
			boolean asc = isAscending();
			if (col != null) {
				asc = isDefaultAscending(col);
			}
			asc = SessionUtils.getBoolean(ATTR_SORT_ORDER_PREFIX + getSortAttributePrefix(), asc);
			setAscending(asc);
			doSort();
		}
	}

	/**See {@link #sortAttributePrefix}. */
	public String getSortAttributePrefix() {
		return sortAttributePrefix;
	}
	/**See {@link #sortAttributePrefix}. */
	public void setSortAttributePrefix(String sortAttributePrefix) {
		this.sortAttributePrefix = sortAttributePrefix;
	}

	/**
	 * Cause the next call to doSort() or sortIfNeeded() to definitely do
	 * a sort operation.
	 */
	public void forceSort() {
		viewList.forceSort();
	}

	/**
	 * Called when user clicks "Ok" (or equivalent) on a standard confirmation dialog.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_DELETE_ITEM:
				res = actionDeleteOk();
				break;
			case ACT_SETUP_DEFAULT:
				refreshList();
				break;
		}
		addClientResizeScroll();
		return res;
	}

	/**
	 *
	 * @see com.lightspeedeps.web.view.View#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		// Frequently ICEfaces scrolls our list back to the top while a pop-up
		// is displayed.  If the pop-up (e.g., for Delete) is cancelled, restore
		// the list to its prior scroll position.
		addClientResizeScroll();
		return super.confirmCancel(action);
	}

	/**
	 * The method called when the user clicks OK in the delete confirmation
	 * dialog. This implementation does nothing. Subclasses should override this
	 * method if they support the Delete operation.
	 *
	 * @return null navigation string
	 */
	protected String actionDeleteOk() {
		// Subclasses should override if they support Delete
		return null;
	}

	/**
	 * @return the number of items in our item list.  Useful in JSP, which
	 * cannot query the size method directly.
	 */
	public int getItemListSize() {
		return getItemList().size();
	}

	/** See {@link #newEntry}. */
	public boolean isNewEntry() {
		return newEntry;
	}
	/** See {@link #newEntry}. */
	public void setNewEntry(boolean b) {
		newEntry = b;
	}

	/** See {@link #selectedRow}. */
	public int getSelectedRow() {
		return selectedRow;
	}
	/** See {@link #selectedRow}. */
	public void setSelectedRow(int selectedRow) {
		this.selectedRow = selectedRow;
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
