/**
 * SelectableList.java
 */
package com.lightspeedeps.web.view;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.RowState;
import org.icefaces.ace.model.table.RowStateMap;

import com.lightspeedeps.model.PersistentObject;

/**
 * This class is used to manage lists of items that are both sortable
 * and selectable. That is, one item at a time in the list may be in
 * a "selected" state.
 */
public class SelectableList extends SortableListImpl {
	protected static Log log = LogFactory.getLog(SelectableList.class);

	private static final long serialVersionUID = - 6598398975899733917L;

	/** The object which is using this SelectableList. This reference may be
	 * used for callbacks. */
	private final SelectableListHolder holder;

	/** The index of the currently selected row. */
	private int selectedRow = -1;

	/** The RowStateMap instance used to manage the UI status of the clicked row in the list */
	private RowStateMap stateMap = new RowStateMap();

	/**
	 * @param hold
	 * @param pList
	 * @param defaultSortColumn
	 * @param defaultAscending
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SelectableList(SelectableListHolder hold, List pList, String defaultSortColumn, boolean defaultAscending) {
		super(hold, pList, defaultSortColumn, defaultAscending);
		holder = hold;
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

		int row = ListView.findSelectedRowParam(evt);
		log.debug("row=" + row + ", id=" + id);

		if (getSelectedRow() >= 0 && getSelectedRow() < getItemList().size()) {
			Object item = getItemList().get(getSelectedRow());
			if (item != null) {
				setSelected(item, false);
			}
		}
		if (getSelectedRow() != row) {
			selectedRowChanged();
		}
		setSelectedRow(row);
		if (id == null && row >= 0 && row < getItemList().size()) {
			Object item = getItemList().get(row);
			if (item instanceof PersistentObject) {
				id = ((PersistentObject)item).getId();
			}
		}
		setupSelectedItem(id);
		// Use the following if it becomes necessary to skip input-validation, invoke-application, etc.
		// See http://jira.icefaces.org/browse/ICE-2751 for more discussion.
		//FacesContext.getCurrentInstance().renderResponse(); // Skip directly to Render-response phase
	}

	/**
	 * Called by our rowSelected() method if the user has clicked on a
	 * row different than the currently selected one.
	 */
	protected void selectedRowChanged() {
		if (holder != null) {
			holder.selectableRowChanged();
		}
	}

	/**
	 * Prepare to display the item identified by 'id'.
	 * @param id The database id of the item which has been
	 * selected to view.
	 */
	protected void setupSelectedItem(Integer id) {
		if (holder != null) {
			holder.setupSelectableItem(id);
		}
	}

	/** Set the "selected" flag on the specified item to either
	 * true or false.  Used to manage highlighting of the items
	 * in the list. */
	protected void setSelected(Object item, boolean b) {
		if (holder != null) {
			holder.setSelectableSelected(item, b);
		}
	}

	/**
	 * Select a row in the current list by creating an ace RowState object and
	 * adding to the {@link #stateMap}.
	 *
	 * @param entry The object (list item) to be selected.
	 */
	public void selectRowState(Object entry) {
		RowState state = new RowState();
		state.setSelected(true);
		getStateMap().put(entry, state);
		//setSelected(entry, true);
	}

	/** Return the list of items being viewed and managed. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Comparable> getItemList() {
		return getList();
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
