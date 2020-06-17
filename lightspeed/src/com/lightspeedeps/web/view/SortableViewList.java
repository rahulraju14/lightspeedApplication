/**
 * File: SortableViewList.java
 */
package com.lightspeedeps.web.view;

import java.io.Serializable;

import com.lightspeedeps.util.app.SessionUtils;


/**
 * A simple concrete class for {@link SortableList} (which is abstract).  It is designed
 * to be used by any of the {@link ListView} subclasses.
 */
public class SortableViewList extends SortableList implements Serializable {
	/** */
	private static final long serialVersionUID = 1763982038361847064L;

	/** A reference to the ListView that 'owns' this instance. */
	private final ListView viewBean;

	/**
	 * Construct a SortableViewList with the given default sort column, and which will
	 * call the given bean to execute the sort.
	 *
	 * @param defaultSortColumn The name of the column to be used for sorting
	 *            until it is changed.
	 * @param bean The {@link ListView} that actually contains the list to be
	 *            sorted, and which will be called to execute the sort when
	 *            necessary.
	 */
	public SortableViewList(String defaultSortColumn, ListView bean) {
		super(defaultSortColumn);
		viewBean = bean;
		setAscending(viewBean.isDefaultAscending(defaultSortColumn));
		oldAscending = !ascending; // force initial sort
	}

	/**
	 * @see com.lightspeedeps.web.view.SortableList#isDefaultAscending(java.lang.String)
	 */
	@Override
	public boolean isDefaultAscending(String sortColumn) {
		if (viewBean != null) {
			return viewBean.isDefaultAscending(sortColumn);
		}
		// called from the SortableList constructor!
		return true;
	}

	/**
	 * Save the sort-ordering value (true for ascending, false for descending)
	 * in the user's session. This makes it available to the
	 * ListView.restoreSortOrder() method.
	 *
	 * @see com.lightspeedeps.web.view.SortableList#saveAscending(boolean)
	 */
	@Override
	protected void saveAscending(boolean ascending) {
		SessionUtils.put(ListView.ATTR_SORT_ORDER_PREFIX + viewBean.getSortAttributePrefix(), ascending);
	}

	/**
	 * @see com.lightspeedeps.web.view.SortableList#sort()
	 */
	@Override
	protected void sort() {
		viewBean.sort();
	}

}
