//	File Name:	TimecardSelectionType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in timecard printing and HTG process to indicate what
 * group of timecards has been selected by the user for processing. The selection
 * indicated is usually further qualified by a particular week-ending date,
 * the current Production (or Project, for Commercial productions), and
 * possibly by a particular status.
 */
public enum TimecardSelectionType {
	/** Only the currently selected timecard will be processed. */
	CURRENT		("Current"),
	/** All timecards for the same person as the currently selected one
	 * (and the selected week) will be processed. */
	PERSON		("Person"),
	/** All timecards for persons in the given department will
	 * be processed. */
	DEPT		("Department"),
	/** All timecards in the selected batch will be processed. */
	BATCH		("Batch"),
	/** All timecards not assigned to any batch will be processed. */
	UNBATCHED	("Unbatched"),
	/** All timecards for this production will be processed. */
	ALL			("All"),
	/** All checked timecards will be printed */
	SELECT		("Select"),
	;

	private final String heading;

	private TimecardSelectionType(String head) {
		heading = head;
	}

	/**
	 * @return The "pretty" mixed-case version of the enumerated value. This
	 *         could be enhanced to use the current locale setting for i18n
	 *         purposes.
	 *         <p>
	 *         NOTE: This overrides the default toString(), which returns the
	 *         same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * @return The "pretty" printable version of this enumerated type. It is the
	 *         same as toString, but can be used from jsp pages since it follows
	 *         the bean accessor (getter) naming convention.
	 */
	public String getLabel() {
		return toString();
	}

}
