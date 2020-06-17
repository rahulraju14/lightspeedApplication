/**
 * File: PopupHolder.java
 */
package com.lightspeedeps.web.popup;

/**
 * The interface which must be implemented by users of the PopupBean.
 */
public interface PopupHolder {

	public String confirmOk(int action);

	public String confirmCancel(int action);

}
