package com.lightspeedeps.web.view;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The SortableList class is a utility class used by most of the UI beans to
 * manage their primary list -- the list presented on the left-hand side of many
 * of the LS pages (e.g., the list of ScriptElements for the Script Element
 * page). This is done through the use of the SortableViewList implementation
 * class. For other lists, the SortableListImpl class may be used.
 */
public abstract class SortableList implements Serializable {
	/** */
	private static final long serialVersionUID = - 4671866328502737906L;

	private static Log log = LogFactory.getLog(SortableList.class);

	/** The current sort key used for sorting the list of items. This value
	 * is typically used by the sort method to control the order of items
	 * in the list. */
	protected String sortColumnName;
	/** True iff the list should be sorted in ascending order. */
	protected boolean ascending;

	/** The last-used sort key; if this value is different than {@link #sortColumnName}
	 * then we need to re-sort the list. */
	protected String oldSortColumnName;
	/** The last-used setting of the {@link #ascending} flag; if this value
	 * is different than {@link #ascending}, then we need to re-sort the list. */
	protected boolean oldAscending;

	/**
	 * Construct a new SortableList with the specified String as the initial
	 * sort key.
	 *
	 * @param defaultSortColumn The String which represents the column or field
	 *            which will be used to sort the list initially (until
	 *            setSortColumnName() is called to change this value).
	 */
	protected SortableList(String defaultSortColumn) {
		sortColumnName = defaultSortColumn;
		ascending = isDefaultAscending(defaultSortColumn);
		oldSortColumnName = sortColumnName;
		// make sure sortColumnName on first render
		oldAscending = !ascending;
	}

	/**
	 * Construct a new SortableList without an initial sort key (column name)
	 * specified.
	 */
	protected SortableList() {
		sortColumnName = "";
		oldSortColumnName = "";
		ascending = true;
		// make sure we'll perform a sort on the first render:
		oldAscending = false;
	}

	/**
	 * Sort the list. This method must be supplied by the implementation
	 * class.
	 */
	protected abstract void sort();

	/**
	 * Call sort() if the sort column or sort direction has changed; and
	 * save the new sort values.  This is the sort method that is typically
	 * called, which avoids unnecessary sorting.
	 */
	public void sortIfNeeded() {
		if (! oldSortColumnName.equals(sortColumnName) || oldAscending != ascending) {
			if (! oldSortColumnName.equals(sortColumnName)) {
				setAscending(isDefaultAscending(sortColumnName));
				saveAscending(isAscending()); // save the new value, if desired.
			}
			doSort();
		}
	}

	/**
	 * This method allows subclasses an opportunity to save the (possibly
	 * changed) value of the sort-ascending flag.
	 *
	 * @param ascending This list's current sort-ordering value.
	 */
	protected void saveAscending(boolean ascending) {
		// Default is to do nothing.
	}

	/**
	 * Call the abstract sort() method & save the new sort values
	 * (column name and sort order).
	 */
	public void doSort() {
		oldSortColumnName = sortColumnName;
		oldAscending = ascending;
		sort();
	}

	/**
	 * Cause the next call to doSort() or sortIfNeeded() to definitely do
	 * a sort operation.
	 */
	public void forceSort() {
		oldAscending = ! ascending;
	}

	/**
	 * Is the default sort direction for the given column "ascending" ?
	 */
	public abstract boolean isDefaultAscending(String sortColumn);

	/** See {@link #sortColumnName}. */
	public String getSortColumnName() {
		return sortColumnName;
	}

	/**
	 * Sets the sortColumnName value, which identifies the current
	 * "sort key".  The list will not be sorted immediately, but on
	 * the next call to sort() or doSort().
	 *
	 * @param sortColumnName column to sortColumnName
	 */
	public void setSortColumnName(String sortColumnName) {
		oldSortColumnName = this.sortColumnName;
		this.sortColumnName = sortColumnName;
	}

	/** See {@link #ascending}. */
	public boolean isAscending() {
		return ascending;
	}
	/** See {@link #ascending}. */
	public void setAscending(boolean ascending) {
		oldAscending = this.ascending;
		this.ascending = ascending;
	}

	/**
	 * Replace an item in the list with another item that would compare 'equal'
	 * to the existing item. This is typically called when a class updates other
	 * fields in the item that are displayed in the view of the list.
	 *
	 * @param list The List containing the item to be replaced.
	 * @param item The new item which will replace the existing item.
	 */
	@SuppressWarnings("rawtypes")
	protected static void replaceItemInList(List<Comparable> list, Comparable item) {
		int ix = list.indexOf(item);
		if (ix < 0) {
			//ix = getSelectedRow();
			log.debug("item not found in list: " + item);
		}
		else {
			log.debug("row: " + ix);
			list.set(ix, item);
		}
	}

}
