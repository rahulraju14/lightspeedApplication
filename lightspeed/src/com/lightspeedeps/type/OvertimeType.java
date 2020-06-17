/*	File Name:	OvertimeType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in the OvertimeRule object to indicate how overtime is
 * calculated.
 */
public enum OvertimeType {
	/** Overtime rate is applied to the hourly wage */
	H		("Hourly"),
	/** Overtime rate is applied to the Daily wage to determine a per-hour
	 * payment. */
	DH		("Daily per Hour"),
	/** Overtime rate is applied to the Daily wage to determine a per-hour
	 * payment, and the number of hours worked is rounded up to the next full
	 * hour. */
	DHR		("Daily per Hour-Rounded"),
	/** Overtime rate is applied to the Weekly wage */
	W		("Weekly"),
	;

	private final String heading;

	OvertimeType(String head) {
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
