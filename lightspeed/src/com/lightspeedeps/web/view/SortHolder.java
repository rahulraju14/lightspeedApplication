/**
 * File: SortHolder.java
 */
package com.lightspeedeps.web.view;

import java.util.List;

/**
 * This interface must be implemented by users of the SortableListImpl class.
 */
public interface SortHolder {

	/**
	 * The method that will be called when SortableListImpl determines that the
	 * underlying list of items needs to be sorted. This is typically the result
	 * of something (such as the JSP framework) calling the SortableListImpl's
	 * getList() method.
	 * <p/>
	 * The current sort key (sortColumnName) and sort direction (ascending or
	 * descending) may be determined by calling the appropriate methods of the
	 * provided SortableList instance.
	 * <p/>
	 * Upon exit, the provided List should have been sorted according to the
	 * current sortColumnName and ascending flag values.
	 *
	 * @param list The list of objects to be sorted. This is the same List
	 *            instance that was provided to the SortableListImpl object when
	 *            it was instantiated, unless the value was changed by a
	 *            setList() call.
	 * @param sortableList The SortableListImpl instance which is calling this
	 *            sort method. From this instance the sort method may get the
	 *            current sortColumnName and ascending (boolean) value.
	 */
	public void sort(@SuppressWarnings("rawtypes") List list, SortableList sortableList);

	/**
	 * Returns the default ordering of the given column.
	 *
	 * @param sortColumn The name of the column about to be sorted.
	 * @return True if the default sort order for the given column is ascending,
	 *         false if it is descending.
	 */
	public boolean isSortableDefaultAscending(String sortColumn);

}
