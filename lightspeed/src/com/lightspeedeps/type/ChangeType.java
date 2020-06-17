//	File Name:	ChangeType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in the {@link com.lightspeedeps.model.Change Change}
 * instances, indicating the major "type" of the change being recorded. This
 * usual indicates the object type being affected. (The {@link ActionType} enum
 * is used to further qualify the change being recorded.)
 */
public enum ChangeType {
	PRODUCTION,
	PROJECT,
	UNIT,
	PAYROLL,
	USER,
	CONTACT,
	SCRIPT,
	SCENE,
	STRIPBOARD,
	SCRIPT_ELEMENT,
	REAL_ELEMENT;

	/** Provides a String that can be used in JSP for ace:dataTable
	 * operations such as sorting and filtering. */
	public String getLabel() {
		return name();
	}
}
