// File Name : PayRateType.java
package com.lightspeedeps.type;

/**
 * Enumeration used in {@link com.lightspeedeps.model.PayJob} class, to indicate
 * the meaning of the custom values in each column of the Job table, i.e.,
 * whether the values are number of hours, days, weeks, etc. Created in LS-2241.
 */
public enum PayRateType {

	/** Regular (hourly) rate */
	H	("Hourly", ""),
	/** Premium (hourly) - will use "premium rate" from PayJob if filled in. */
	P	("Premium", "p"),
	/** Daily (exempt) */
	D	("Daily", "Dy"),
	/** Weekly (exempt) */
	W	("Weekly", "Wk"),
	/** Fixed rate; multiplier will be rate ($) */
	F	("Fixed", "R"),
	/** UDA rates, read from ContractRate table **/
	T	("Table","T")
	;

	/** The 'standard' heading (pretty name) for field */
	private final String heading;
	/** Short text to be displayed in column header on Job Table. */
	private final String columnText;

	PayRateType(String head, String colText) {
		heading = head;
		columnText = colText;
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

	/** See {@link #columnText}. */
	public String getColumnText() {
		return columnText;
	}

//	/**See {@link #index}. */
//	public int getIndex() {
//		return index;
//	}

}
