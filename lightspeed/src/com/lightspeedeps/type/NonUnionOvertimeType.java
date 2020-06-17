/*	File Name:	NonUnionOvertimeType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in the Production object for the default overtime rule
 * to use for Non-Union employees.
 */
public enum NonUnionOvertimeType {
	/** California: OT (1.5x) after 8 hrs; 2x after 12 hrs;
	 * over 6 hrs gets 1 MPV, paid at 1 hour of straight time (regardless
	 * of length of violation). */
	CA		("Calif. OT 8/4/2x + MPV"),
	/** Daily overtime of 1.5x after 8 hours. No Gold or MPVs. */
	D8		("Daily OT 8+"),
	/** Weekly overtime of 1.5x after 40 hours, no daily OT.
	 * No Gold or MPVs. */
	W40		("Weekly OT 40+"),
	/** not specified */
	N_A		("N/A"),
	;

	private final String heading;

	NonUnionOvertimeType(String head) {
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
