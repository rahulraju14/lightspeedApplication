// File Name : ExemptPayType.java
package com.lightspeedeps.type;

/**
 * Enumeration used in {@link com.lightspeedeps.model.FormWTPA} class,
 * related to the Exempt pay type (Hourly, salary etc).
 */
public enum ExemptPayType {

	H	("hourly", 0),
	S	("salary", 1),
	D	("Day", 2),
	P	("piece", 3),
	O	("other basis", 4)
	// Note that the string values match the NY WTPA form values; if the text is to be
	// changed for other purposes, it will be necessary to add a field that matches these.
	;

	private final String heading;
	
	/** Index of the enum used by the radio buttons on the form. */
	private final int index;

	ExemptPayType(String head, int index) {
		heading = head;
		this.index = index;
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
	
	/**See {@link #index}. */
	public int getIndex() {
		return index;
	}
}
