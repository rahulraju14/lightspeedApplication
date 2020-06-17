/**
 * File: SelectableListHolder.java
 */
package com.lightspeedeps.web.view;

/**
 * This interface must be implemented by users of the {@link SelectableList} SelectableList class. It
 * defines callback methods that the SelectableList uses to communicate with
 * the list "holder".
 */
public interface SelectableListHolder extends SortHolder {

	public void selectableRowChanged();

	public void setSelectableSelected(Object item, boolean b);

	public void setupSelectableItem(Integer id);

}
