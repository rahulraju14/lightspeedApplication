/**
 * File: ControlHolder.java
 */
package com.lightspeedeps.object;

/**
 * An interface used between our bean code and the TriStateCheckBoxExt
 * class, for callbacks.  This could be used for other display widgets
 * that need to call back an "owner" upon some event.
 */
public interface ControlHolder {

	public void clicked(TriStateCheckboxExt checkBox, Object id);

}
