//	File Name:	FilterType.java
package com.lightspeedeps.type;


/**
 * Enumeration used in {@link com.lightspeedeps.web.approver.FilterBean} class
 * and users of the {@link com.lightspeedeps.web.approver.FilterHolder}
 * interface.
 */
public enum FilterType {
	BATCH	("Batch"),
	DEPT	("Dept"),
	STATUS  ("Status"),
	N_A		("N/A"),	// just delineate Filter-by items (above) from others
	UNION	("Union"),	// in 3.1, this is in use; in 2.9, not in use
	DATE	("Date"),
	NAME	("Name");

	private final String heading;

	FilterType(String head) {
		heading = head;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * Returns the "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return toString();
	}

}
