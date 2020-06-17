/**
 * FilterHolder.java
 */
package com.lightspeedeps.web.approver;

import java.util.List;

import javax.faces.model.SelectItem;

import com.lightspeedeps.type.FilterType;

/**
 * This is the interface that must be implemented by classes using the FilterBean
 * class.  It defines callbacks that the FilterBean class calls in its "registered
 * holders".
 */
public interface FilterHolder {

	/**
	 * Callback to notify the holder that the selected mini-tab is changing.
	 * @param priorTab The previously selected mini-tab index.
	 * @param currentTab The newly-selected mini-tab index (origin zero).
	 */
	public void changeTab(int priorTab, int currentTab);

	/**
	 * Callback to create a list of SelectItem`s for a particular drop-down
	 * list. The FilterType is used to determine which drop-down list is being
	 * populated.
	 *
	 * @param type
	 */
	public List<SelectItem> createList(FilterType type);

	/**
	 * Callback to notify the holder that a filter has been removed. This is
	 * usually because the user has selected a different filter type.  For example,
	 * if the user had been filtering by Department, but then selects to "Filter
	 * By Batch", there will be a call to dropFilter with a type of Department.
	 *
	 * @param type The FilterType that should no longer be used for filtering.
	 */
	public void dropFilter(FilterType type);

	/**
	 * Callback for a listenerValueChange event. Note that the callback is never
	 * called with a null value.
	 *
	 * @param type This indicates the type of dropdown that generated the change
	 *            event, e.g., Department or Date.
	 * @param value The new value selected by the user.
	 */
	public void listenChange(FilterType type, Object value);

}
