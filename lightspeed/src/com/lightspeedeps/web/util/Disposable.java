/**
 * Disposable.java
 */
package com.lightspeedeps.web.util;

/**
 * Interface for managed ViewScoped beans that need to be called when
 * they are destroyed.
 */
public interface Disposable {

	/**
	 * Called to dispose of any resources held by this object.
	 */
	public void dispose();

}
