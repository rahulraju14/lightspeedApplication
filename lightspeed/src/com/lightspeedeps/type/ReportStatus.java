package com.lightspeedeps.type;

/**
 * An enumeration used to describe a report object's status; used
 * for Callsheet, DPR, ExhibitG, and TimeSheet.
 */
public enum ReportStatus {
	CREATED 	("CREATED", "PRELIMINARY"),
	UPDATED 	("IN PROGRESS", "PRELIMINARY"),
	SUBMITTED 	("FINAL / SUBMITTED"),
	APPROVED 	("FINAL / APPROVED", "FINAL"),
	PUBLISHED 	("FINAL");

	private final String heading;
	private final String callHeading;

	ReportStatus(String head) {
		heading = head;
		callHeading = null;
	}

	ReportStatus(String head, String pCall) {
		heading = head;
		callHeading = pCall;
	}

	/**
	 * Returns a "pretty" version of the enumerated value, for use in screen
	 * displays or reports.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * Returns the "pretty" printable/display version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 *
	 * @return The value of the enumerated type as it should be displayed on-screen or in reports.
	 */
	public String getLabel() {
		return this.toString();
	}

	public String getCallLabel() {
		if (callHeading != null) {
			return callHeading;
		}
		return heading;
	}

}
