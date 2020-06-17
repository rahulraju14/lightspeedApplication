//	File Name:	ActionType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in the Change model.
 */
public enum ActionType {
	CREATE,
	UPDATE,
	DELETE,
	DOWNLOAD,
	N_A;

	/** Provides a String that can be used in JSP for ace:dataTable
	 * operations such as sorting and filtering. */
	public String getLabel() {
		return name();
	}
}
