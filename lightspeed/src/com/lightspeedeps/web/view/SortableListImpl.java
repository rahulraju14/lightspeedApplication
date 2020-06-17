/**
 * File: SortableListImpl.java
 */
package com.lightspeedeps.web.view;

import java.util.List;

/**
 * This class provides an implementation of SortableList which can be used for
 * managing a list that needs to be sorted for presentation to the user. Note
 * that the getList() method will sort the list if the sort values (sort key or
 * ascending flag) have changed since the last getList() call.
 */
public class SortableListImpl extends SortableList {
	/** */
	private static final long serialVersionUID = 4584473020405915895L;

	private final SortHolder holder;

	@SuppressWarnings("rawtypes")
	private List<Comparable> list;

	/**
	 * Creates a new SortableListImpl to manage the given List. The initial
	 * sorting of the list will use the supplied defaultSortColumn key and
	 * defaultAscending ordering. The given SortHolder will be called back
	 * whenever a sort of the list is required.
	 *
	 * @param hold The SortHolder (typically the object that is instantiating
	 *            this class) that will be called back to perform the actual
	 *            list sort.
	 * @param pList The List to be managed; note that if the List is
	 *            re-allocated, the SortHolder must supply the new List to this
	 *            object by calling the setList() method.
	 * @param defaultSortColumn The column name (sort key) to be used for the
	 *            initial sort.
	 * @param defaultAscending A flag indicating if the initial sort should be
	 *            in ascending (true) or descending (false) order.
	 */
	public SortableListImpl(SortHolder hold,
			@SuppressWarnings("rawtypes") List<Comparable> pList,
			String defaultSortColumn, boolean defaultAscending) {
		super();
		holder = hold;
		list = pList;
		sortColumnName = defaultSortColumn;
		setAscending(defaultAscending);
		oldAscending = !ascending; // force initial sort
	}

	/**
	 * @see com.lightspeedeps.web.view.SortableList#sort()
	 */
	@Override
	protected void sort() {
		holder.sort(list, this);
	}

	/**
	 * @see com.lightspeedeps.web.view.SortableList#isDefaultAscending(java.lang.String)
	 */
	@Override
	public boolean isDefaultAscending(String sortColumn) {
		// delegate the call to our holder's equivalent method:
		return holder.isSortableDefaultAscending(sortColumn);
	}

	/** See {@link #list}. */
	@SuppressWarnings("rawtypes")
	public List getList() {
		sortIfNeeded();
		return list;
	}
	/** See {@link #list}. */
	public void setList(@SuppressWarnings("rawtypes") List<Comparable> list) {
		this.list = list;
	}

	/**
	 * Replace an item in the list with another item that would compare 'equal'
	 * to the existing item. This is typically called when a class updates other
	 * fields in the item that are displayed in the view of the list.
	 *
	 * @param item The new item which will replace the existing item.
	 */
	@SuppressWarnings("rawtypes")
	public void updateItemInList(Comparable item) {
		SortableList.replaceItemInList(list, item);
	}

}
