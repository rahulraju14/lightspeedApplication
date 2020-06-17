/*	File Name:	AuditEventType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in the AuditEvent object to indicate the type
 * of event that occurred.
 */
public enum AuditEventType {
	// ******* DO NOT CHANGE THE ORDER OF THIS ENUMERATION ******
	// ******* ORDINAL VALUES ARE STORED IN THE DATABASE ********

	/** Calculate all HTG values for a WeeklyTimecard */
	CALC_HTG			("All HTG:"),

	/** Calculate the MPV values for a WeeklyTimecard */
	CALC_MPV			("Calculate MPVs."),

	/** Breakdown a WeeklyTimecard into one or more PayJob tables. */
	FILL_JOBS			("Fill Job tables."),

	/** Fill a WeeklyTimecard's PayBreakdown entries based on the
	 * data in the timecard's PayJob table(s). */
	FILL_PAY_BREAKDOWN	("Auto-Pay."),

	/** Validate a timecard prior to one or more HTG processing steps. */
	VALIDATE_TIMECARD ("Validate timecard."),

	// ******* DO NOT CHANGE THE ORDER OF THIS ENUMERATION ******
	// ******* ORDINAL VALUES ARE STORED IN THE DATABASE ********
	;

	private final String heading;

	AuditEventType(String head) {
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
